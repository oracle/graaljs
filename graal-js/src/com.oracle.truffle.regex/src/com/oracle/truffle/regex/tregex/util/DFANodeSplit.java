/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.util;

import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.regex.tregex.TRegexOptions;
import com.oracle.truffle.regex.tregex.nodes.BackwardDFAStateNode;
import com.oracle.truffle.regex.tregex.nodes.DFAStateNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Implementation of a node splitting algorithm presented by Sebastian Unger and Frank Mueller in
 * "Handling Irreducible Loops: Optimized Node Splitting vs. DJ-Graphs" (2001) and "Transforming
 * Irreducible Regions of Control Flow into Reducible Regions by Optimized Node Splitting" (1998),
 * with the dominance algorithm by Keith D. Cooper, Timothy J. Harvey, and Ken Kennedy presented in
 * "A Simple, Fast Dominance Algorithm"
 */
public final class DFANodeSplit {

    public static class DFANodeSplitBailoutException extends SlowPathException {

        private static final long serialVersionUID = 29374928364982L;
    }

    private static class GraphNode implements Comparable<GraphNode> {

        private final DFAStateNode dfaNode;

        Set<GraphNode> backEdges = new TreeSet<>();
        Set<GraphNode> preds = new TreeSet<>();
        Set<GraphNode> succs = new TreeSet<>();
        Set<GraphNode> succsDom = new TreeSet<>();
        GraphNode idom;
        GraphNode header;
        GraphNode copy;
        int postOrderIndex;
        int level;
        int weight;
        boolean active;
        boolean done;
        boolean traversed;

        GraphNode() {
            dfaNode = null;
        }

        GraphNode(DFAStateNode dfaNode) {
            this(dfaNode, dfaNode.getId());
        }

        GraphNode(DFAStateNode dfaNode, short dfaNodeId) {
            this.dfaNode = dfaNode.createNodeSplitCopy(dfaNodeId);
        }

        void createCopy(short dfaNodeId) {
            GraphNode cpy = new GraphNode(dfaNode, dfaNodeId);
            cpy.backEdges = new TreeSet<>(backEdges);
            cpy.preds = new TreeSet<>(preds);
            cpy.succs = new TreeSet<>(succs);
            cpy.succsDom = new TreeSet<>(succsDom);
            cpy.header = header;
            cpy.level = level;
            cpy.weight = weight;
            cpy.active = active;
            cpy.done = done;
            this.copy = cpy;
        }

        @Override
        public int compareTo(GraphNode o) {
            return getId() - o.getId();
        }

        int getId() {
            return dfaNode.getId();
        }

        void markBackEdge(GraphNode node) {
            backEdges.add(node);
        }

        boolean isBackEdge(GraphNode node) {
            return backEdges.contains(node);
        }

        void clearBackEdges() {
            backEdges.clear();
        }

        int nodeWeight() {
            return dfaNode.getSuccessors().length;
        }

        void setWeightAndHeaders(GraphNode headerNode, Set<GraphNode> scc) {
            weight = nodeWeight();
            for (GraphNode child : succsDom) {
                if (scc.contains(child)) {
                    child.setWeightAndHeaders(headerNode, scc);
                    weight += child.weight;
                }
            }
            header = headerNode;
        }

        void replaceSuccessor(GraphNode suc) {
            short[] dfaSuccessors = dfaNode.getSuccessors();
            for (int i = 0; i < dfaSuccessors.length; i++) {
                if (dfaSuccessors[i] == suc.dfaNode.getId()) {
                    dfaSuccessors[i] = suc.copy.dfaNode.getId();
                    break;
                }
            }
            succs.remove(suc);
            succs.add(suc.copy);
        }

        void markUndone() {
            this.done = false;
            this.active = false;
        }
    }

    private static class EntryNode extends GraphNode {

        @Override
        int getId() {
            return -1;
        }

        @Override
        int nodeWeight() {
            return 0;
        }

        @Override
        void createCopy(short dfaNodeId) {
            throw new UnsupportedOperationException();
        }

        @Override
        void replaceSuccessor(GraphNode suc) {
            throw new UnsupportedOperationException();
        }
    }

    private final GraphNode start;
    private final ArrayList<GraphNode> nodes;
    private short nextId;

    private int nextPostOrderIndex;
    private GraphNode[] postOrder;
    private int[] doms;

    private DFANodeSplit(short[] anchoredEntries, short[] unAnchoredEntries, DFAStateNode[] dfa) {
        nodes = new ArrayList<>(dfa.length);
        for (DFAStateNode n : dfa) {
            assert n.getId() == nodes.size();
            nodes.add(new GraphNode(n));
        }
        nextId = (short) nodes.size();
        for (GraphNode graphNode : nodes) {
            for (int successor : graphNode.dfaNode.getSuccessors()) {
                graphNode.succs.add(nodes.get(successor));
                nodes.get(successor).preds.add(graphNode);
            }
        }
        start = new EntryNode();
        for (short i : anchoredEntries) {
            if (i != -1) {
                start.succs.add(nodes.get(i));
                nodes.get(i).preds.add(start);
            }
        }
        for (short i : unAnchoredEntries) {
            if (i != -1) {
                start.succs.add(nodes.get(i));
                nodes.get(i).preds.add(start);
            }
        }
    }

    public static DFAStateNode[] createReducibleGraph(short[] anchoredEntries, short[] unAnchoredEntries, DFAStateNode[] nodes) throws DFANodeSplitBailoutException {
        return new DFANodeSplit(anchoredEntries, unAnchoredEntries, nodes).process();
    }

    private DFAStateNode[] process() throws DFANodeSplitBailoutException {
        buildPostOrder();
        buildDominatorTree();
        setLevel(start, 1);
        markUndone();
        searchBackEdges(start);
        markUndone();
        splitLoops(start, new TreeSet<>());
        DFAStateNode[] ret = new DFAStateNode[nodes.size()];
        for (GraphNode node : nodes) {
            ret[node.dfaNode.getId()] = node.dfaNode;
        }
        return ret;
    }

    private boolean graphIsConsistent() {
        for (GraphNode p : start.preds) {
            if (!p.succs.contains(start)) {
                return false;
            }
        }
        for (GraphNode s : start.succs) {
            if (!s.preds.contains(start)) {
                return false;
            }
        }
        for (GraphNode n : nodes) {
            for (GraphNode p : n.preds) {
                if (!p.succs.contains(n)) {
                    return false;
                }
            }
            for (GraphNode s : n.succs) {
                if (!s.preds.contains(n)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void buildPostOrder() {
        assert graphIsConsistent();
        start.traversed = false;
        for (GraphNode n : nodes) {
            n.traversed = false;
        }
        nextPostOrderIndex = 0;
        postOrder = new GraphNode[nodes.size() + 1];
        traversePostOrder(start);
        assert allNodesTraversed();
    }

    private void traversePostOrder(GraphNode cur) {
        cur.traversed = true;
        for (GraphNode n : cur.succs) {
            if (!n.traversed) {
                traversePostOrder(n);
            }
        }
        cur.postOrderIndex = nextPostOrderIndex++;
        postOrder[cur.postOrderIndex] = cur;
    }

    private boolean allNodesTraversed() {
        if (!start.traversed) {
            return false;
        }
        for (GraphNode n : nodes) {
            if (!n.traversed) {
                return false;
            }
        }
        return true;
    }

    private void buildDominatorTree() {
        doms = new int[nodes.size() + 1];
        Arrays.fill(doms, -1);
        doms[start.postOrderIndex] = start.postOrderIndex;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = postOrder.length - 1; i >= 0; i--) {
                GraphNode b = postOrder[i];
                if (b == start) {
                    continue;
                }
                // find a predecessor that was already processed
                GraphNode selectedPredecessor = null;
                for (GraphNode p : b.preds) {
                    if (p.postOrderIndex > i) {
                        selectedPredecessor = p;
                        break;
                    }
                }
                if (selectedPredecessor == null) {
                    throw new IllegalStateException();
                }
                int newIDom = selectedPredecessor.postOrderIndex;
                Set<GraphNode> predsWithoutIDom = new TreeSet<>(b.preds);
                predsWithoutIDom.remove(selectedPredecessor);
                for (GraphNode p : predsWithoutIDom) {
                    if (doms[p.postOrderIndex] != -1) {
                        newIDom = intersect(p.postOrderIndex, newIDom);
                    }
                }
                if (doms[b.postOrderIndex] != newIDom) {
                    doms[b.postOrderIndex] = newIDom;
                    changed = true;
                }
            }
        }
        start.succsDom.clear();
        for (GraphNode n : nodes) {
            n.succsDom.clear();
        }
        for (int i = 0; i < doms.length; i++) {
            GraphNode dominator = postOrder[doms[i]];
            GraphNode successor = postOrder[i];
            if (dominator != successor) {
                dominator.succsDom.add(successor);
            }
        }
    }

    private int intersect(int b1, int b2) {
        int finger1 = b1;
        int finger2 = b2;
        while (finger1 != finger2) {
            while (finger1 < finger2) {
                finger1 = doms[finger1];
            }
            while (finger2 < finger1) {
                finger2 = doms[finger2];
            }
        }
        return finger1;
    }

    private GraphNode idom(GraphNode n) {
        return postOrder[doms[n.postOrderIndex]];
    }

    private boolean dom(GraphNode a, GraphNode b) {
        int dom = doms[b.postOrderIndex];
        while (true) {
            if (a.postOrderIndex == dom) {
                return true;
            }
            if (dom == doms[dom]) {
                return false;
            }
            dom = doms[dom];
        }
    }

    private boolean splitLoops(GraphNode topNode, Set<GraphNode> set) throws DFANodeSplitBailoutException {
        boolean crossEdge = false;
        for (GraphNode child : new TreeSet<>(topNode.succsDom)) {
            if (set.isEmpty() || set.contains(child)) {
                if (splitLoops(child, set)) {
                    crossEdge = true;
                }
            }
        }
        if (crossEdge) {
            handleIrChildren(topNode, set);
        }
        for (GraphNode pred : topNode.preds) {
            if (pred.isBackEdge(topNode) && !dom(topNode, pred)) {
                return true;
            }
        }
        return false;
    }

    private void handleIrChildren(GraphNode topNode, Set<GraphNode> set) throws DFANodeSplitBailoutException {
        ArrayDeque<GraphNode> dfsList = new ArrayDeque<>();
        ArrayList<Set<GraphNode>> sccList = new ArrayList<>();
        for (GraphNode child : topNode.succsDom) {
            if (!child.done && (set.isEmpty() || set.contains(child))) {
                scc1(dfsList, child, set, topNode.level);
            }
        }
        for (GraphNode n : dfsList) {
            if (n.done) {
                Set<GraphNode> scc = new TreeSet<>();
                scc2(scc, n, topNode.level);
                sccList.add(scc);
            }
        }
        for (Set<GraphNode> scc : sccList) {
            if (scc.size() > 1) {
                handleScc(topNode, scc);
            }
        }
    }

    private void scc1(ArrayDeque<GraphNode> dfsList, GraphNode curNode, Set<GraphNode> set, int level) {
        curNode.done = true;
        for (GraphNode child : curNode.succs) {
            if (!child.done && child.level > level && (set.isEmpty() || set.contains(child))) {
                scc1(dfsList, child, set, level);
            }
        }
        dfsList.push(curNode);
    }

    private void scc2(Set<GraphNode> scc, GraphNode curNode, int level) {
        curNode.done = false;
        for (GraphNode pred : curNode.preds) {
            if (pred.done && pred.level > level) {
                scc2(scc, pred, level);
            }
        }
        scc.add(curNode);
    }

    private void handleScc(GraphNode graphNode, Set<GraphNode> scc) throws DFANodeSplitBailoutException {
        Set<GraphNode> msed = new TreeSet<>();
        for (GraphNode n : scc) {
            if (n.level == graphNode.level + 1) {
                n.setWeightAndHeaders(n, scc);
                msed.add(n);
            }
        }
        if (msed.size() <= 1) {
            return;
        }
        splitSCC(chooseNode(msed), scc);

        buildPostOrder();
        buildDominatorTree();
        setLevel(start, 1);
        markUndone();
        searchBackEdges(start);
        markUndone();
        for (GraphNode tmp : findTopNodes(scc)) {
            splitLoops(tmp, scc);
        }
    }

    private void splitSCC(GraphNode headerNode, Set<GraphNode> scc) throws DFANodeSplitBailoutException {
        for (GraphNode n : scc) {
            if (n.header != headerNode) {
                if (nextId == TRegexOptions.TRegexMaxDFASizeAfterNodeSplitting) {
                    throw new DFANodeSplitBailoutException();
                }
                n.createCopy(nextId++);
                assert n.copy.dfaNode.getId() == nodes.size();
                nodes.add(n.copy);
            }
        }
        for (GraphNode cur : scc) {
            if (cur.header != headerNode) {
                for (GraphNode suc : cur.succs) {
                    if (suc.copy == null) {
                        suc.preds.add(cur.copy);
                    } else {
                        cur.copy.replaceSuccessor(suc);
                        suc.copy.preds.remove(cur);
                        suc.copy.preds.add(cur.copy);
                    }
                }
                for (GraphNode pred : cur.preds) {
                    if (pred.copy == null) {
                        if (scc.contains(pred)) {
                            pred.replaceSuccessor(cur);
                        } else {
                            cur.copy.preds.remove(pred);
                        }
                    }
                }
                cur.preds.removeIf(pred -> pred.copy == null && scc.contains(pred));
            }
        }
        for (GraphNode g : new TreeSet<>(scc)) {
            if (g.header != headerNode) {
                scc.add(g.copy);
                g.copy = null;
            }
        }
    }

    private Set<GraphNode> findTopNodes(Set<GraphNode> scc) {
        Set<GraphNode> tops = new TreeSet<>();
        for (GraphNode tmp : scc) {
            GraphNode top = idom(tmp);
            while (scc.contains(top)) {
                top = idom(top);
            }
            if (!tops.contains(top)) {
                tops.add(top);
            }
        }
        return tops;
    }

    private static GraphNode chooseNode(Set<GraphNode> msed) {
        int maxWeight = 0;
        GraphNode maxNode = null;
        for (GraphNode n : msed) {
            if (n.weight > maxWeight) {
                maxWeight = n.weight;
                maxNode = n;
            }
        }
        return maxNode;
    }

    private void searchBackEdges(GraphNode cnode) {
        cnode.done = true;
        cnode.active = true;
        cnode.clearBackEdges();
        for (GraphNode child : cnode.succs) {
            if (child.active) {
                cnode.markBackEdge(child);
            } else if (!child.done) {
                searchBackEdges(child);
            }
        }
        cnode.active = false;
    }

    private void setLevel(GraphNode curNode, int level) {
        curNode.level = level;
        for (GraphNode child : curNode.succsDom) {
            setLevel(child, level + 1);
        }
    }

    private void markUndone() {
        start.markUndone();
        nodes.forEach(GraphNode::markUndone);
    }
}
