/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.util;

import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.regex.tregex.TRegexOptions;
import com.oracle.truffle.regex.tregex.automaton.IndexedState;
import com.oracle.truffle.regex.tregex.automaton.StateIndex;
import com.oracle.truffle.regex.tregex.automaton.StateSet;
import com.oracle.truffle.regex.tregex.automaton.StateSetBackingSortedArray;
import com.oracle.truffle.regex.tregex.buffer.ShortArrayBuffer;
import com.oracle.truffle.regex.tregex.nodes.DFAAbstractStateNode;
import com.oracle.truffle.regex.tregex.nodes.DFAInitialStateNode;
import com.oracle.truffle.regex.util.CompilationFinalBitSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * Implementation of a node splitting algorithm presented by Sebastian Unger and Frank Mueller in
 * "Handling Irreducible Loops: Optimized Node Splitting vs. DJ-Graphs" (2001) and "Transforming
 * Irreducible Regions of Control Flow into Reducible Regions by Optimized Node Splitting" (1998),
 * with the dominance algorithm by Keith D. Cooper, Timothy J. Harvey, and Ken Kennedy presented in
 * "A Simple, Fast Dominance Algorithm"
 */
public final class DFANodeSplit implements StateIndex<DFANodeSplit.GraphNode> {

    public static class DFANodeSplitBailoutException extends SlowPathException {

        private static final long serialVersionUID = 29374928364982L;
    }

    public static class GraphNode implements Comparable<GraphNode>, IndexedState {

        private static final short[] NO_DOM_CHILDREN = new short[0];

        private DFAAbstractStateNode dfaNode;
        boolean dfaNodeCopied = false;
        private final short[] successorSet;

        StateSet<GraphNode> predecessorSet;

        StateSet<GraphNode> backEdges;

        private short[] domChildren;
        private int nDomChildren;

        GraphNode header;
        int postOrderIndex;
        int level;
        int weight;
        boolean active;
        boolean done;
        boolean traversed;

        GraphNode copy;

        GraphNode(DFANodeSplit graph, DFAAbstractStateNode dfaNode, short[] successorSet) {
            this.dfaNode = dfaNode;
            this.successorSet = successorSet;
            predecessorSet = new StateSet<>(graph, new StateSetBackingSortedArray());
            backEdges = new StateSet<>(graph);
            domChildren = NO_DOM_CHILDREN;
        }

        GraphNode(GraphNode cpy, short dfaNodeId) {
            this.dfaNode = cpy.dfaNode.createNodeSplitCopy(dfaNodeId);
            this.dfaNodeCopied = true;
            this.successorSet = cpy.successorSet;
            this.predecessorSet = cpy.predecessorSet.copy();
            this.backEdges = cpy.backEdges.copy();
            this.domChildren = cpy.domChildren == NO_DOM_CHILDREN ? NO_DOM_CHILDREN : Arrays.copyOf(cpy.domChildren, cpy.domChildren.length);
            this.header = cpy.header;
            this.postOrderIndex = cpy.postOrderIndex;
            this.level = cpy.level;
            this.weight = cpy.weight;
            this.active = cpy.active;
            this.done = cpy.done;
            this.traversed = cpy.traversed;
        }

        void createCopy(DFANodeSplit graph, short dfaNodeId) {
            if (getId() == 0) {
                assert dfaNode instanceof DFAInitialStateNode;
                throw new UnsupportedOperationException();
            }
            this.copy = new GraphNode(this, dfaNodeId);
            graph.addGraphNode(copy);
        }

        @Override
        public int compareTo(GraphNode o) {
            return getId() - o.getId();
        }

        @Override
        public short getId() {
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

        void setWeightAndHeaders(StateIndex<GraphNode> index, GraphNode headerNode, Set<GraphNode> scc) {
            weight = nodeWeight();
            for (GraphNode child : domChildren(index)) {
                if (scc.contains(child)) {
                    child.setWeightAndHeaders(index, headerNode, scc);
                    weight += child.weight;
                }
            }
            header = headerNode;
        }

        void replaceSuccessor(GraphNode suc) {
            if (!dfaNodeCopied) {
                dfaNode = dfaNode.createNodeSplitCopy(dfaNode.getId());
                dfaNodeCopied = true;
            }
            short[] dfaSuccessors = dfaNode.getSuccessors();
            for (int i = 0; i < dfaSuccessors.length; i++) {
                if (dfaSuccessors[i] == suc.dfaNode.getId()) {
                    dfaSuccessors[i] = suc.copy.dfaNode.getId();
                }
            }
        }

        boolean hasPredecessor(GraphNode pre) {
            return predecessorSet.contains(pre);
        }

        void addPredecessorUnsorted(GraphNode pre) {
            predecessorSet.addBatch(pre);
        }

        void sortPredecessors() {
            predecessorSet.addBatchFinish();
        }

        void addPredecessor(GraphNode pre) {
            predecessorSet.add(pre);
        }

        void replacePredecessor(GraphNode pre) {
            predecessorSet.replace(pre, pre.copy);
        }

        void removePredecessor(GraphNode pre) {
            predecessorSet.remove(pre);
        }

        void addDomChild(GraphNode child) {
            if (nDomChildren == domChildren.length) {
                if (domChildren == NO_DOM_CHILDREN) {
                    domChildren = new short[10];
                } else {
                    domChildren = Arrays.copyOf(domChildren, domChildren.length * 2);
                }
            }
            domChildren[nDomChildren++] = child.getId();
        }

        void clearDomChildren() {
            nDomChildren = 0;
        }

        void markUndone() {
            this.done = false;
            this.active = false;
        }

        Iterable<GraphNode> successors(StateIndex<GraphNode> index) {
            return () -> new Iterator<GraphNode>() {

                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < successorSet.length;
                }

                @Override
                public GraphNode next() {
                    return index.getState(dfaNode.getSuccessors()[successorSet[i++]]);
                }
            };
        }

        Iterable<GraphNode> predecessors() {
            return predecessorSet;
        }

        Iterable<GraphNode> domChildren(StateIndex<GraphNode> index) {
            return () -> new Iterator<GraphNode>() {

                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < nDomChildren;
                }

                @Override
                public GraphNode next() {
                    return index.getState(domChildren[i++]);
                }
            };
        }
    }

    private final GraphNode start;
    private final ArrayList<GraphNode> nodes;
    private short nextId;

    private int nextPostOrderIndex;
    private GraphNode[] postOrder;
    private int[] doms;

    private DFANodeSplit(DFAAbstractStateNode[] dfa) {
        nodes = new ArrayList<>(dfa.length);
        CompilationFinalBitSet successorBitSet = new CompilationFinalBitSet(dfa.length);
        ShortArrayBuffer successorBuffer = new ShortArrayBuffer();
        for (DFAAbstractStateNode n : dfa) {
            for (int i = 0; i < n.getSuccessors().length; i++) {
                if (n.getSuccessors()[i] == -1) {
                    assert n instanceof DFAInitialStateNode;
                } else {
                    if (!successorBitSet.get(n.getSuccessors()[i])) {
                        successorBuffer.add((short) i);
                    }
                    successorBitSet.set(n.getSuccessors()[i]);
                }
            }
            GraphNode graphNode = new GraphNode(this, n, successorBuffer.toArray());
            successorBitSet.clear();
            successorBuffer.clear();
            addGraphNode(graphNode);
        }
        nextId = (short) nodes.size();
        for (GraphNode graphNode : nodes) {
            for (GraphNode successor : graphNode.successors(this)) {
                successor.addPredecessorUnsorted(graphNode);
            }
        }
        for (GraphNode n : nodes) {
            n.sortPredecessors();
        }
        start = nodes.get(0);
    }

    private void addGraphNode(GraphNode graphNode) {
        assert graphNode.getId() == nodes.size();
        nodes.add(graphNode);
        assert graphNode == nodes.get(graphNode.getId());
    }

    public static DFAAbstractStateNode[] createReducibleGraph(DFAAbstractStateNode[] nodes) throws DFANodeSplitBailoutException {
        return new DFANodeSplit(nodes).process();
    }

    @Override
    public int getNumberOfStates() {
        return nodes.size();
    }

    @Override
    public GraphNode getState(int id) {
        return nodes.get(id);
    }

    private DFAAbstractStateNode[] process() throws DFANodeSplitBailoutException {
        createDomTree();
        searchBackEdges(start);
        markUndone();
        splitLoops(start, new StateSet<>(this, new StateSetBackingSortedArray()));
        DFAAbstractStateNode[] ret = new DFAAbstractStateNode[nodes.size()];
        for (GraphNode node : nodes) {
            ret[node.dfaNode.getId()] = node.dfaNode;
        }
        return ret;
    }

    private void createDomTree() {
        buildPostOrder();
        buildDominatorTree();
        setLevel(start, 1);
        markUndone();
    }

    private boolean graphIsConsistent() {
        for (GraphNode n : nodes) {
            for (GraphNode s : n.successors(this)) {
                if (!s.hasPredecessor(n)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void buildPostOrder() {
        assert graphIsConsistent();
        for (GraphNode n : nodes) {
            n.traversed = false;
        }
        nextPostOrderIndex = 0;
        postOrder = new GraphNode[nodes.size()];
        traversePostOrder(start);
        assert allNodesTraversed();
    }

    private void traversePostOrder(GraphNode cur) {
        cur.traversed = true;
        for (GraphNode n : cur.successors(this)) {
            if (!n.traversed) {
                traversePostOrder(n);
            }
        }
        cur.postOrderIndex = nextPostOrderIndex++;
        postOrder[cur.postOrderIndex] = cur;
    }

    private boolean allNodesTraversed() {
        for (GraphNode n : nodes) {
            if (!n.traversed) {
                return false;
            }
        }
        return true;
    }

    private void buildDominatorTree() {
        doms = new int[nodes.size()];
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
                for (GraphNode p : b.predecessors()) {
                    if (p.postOrderIndex > i) {
                        selectedPredecessor = p;
                        break;
                    }
                }
                if (selectedPredecessor == null) {
                    throw new IllegalStateException();
                }
                int newIDom = selectedPredecessor.postOrderIndex;
                for (GraphNode p : b.predecessors()) {
                    if (p == selectedPredecessor) {
                        continue;
                    }
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
        for (GraphNode n : nodes) {
            n.clearDomChildren();
        }
        for (int i = 0; i < doms.length; i++) {
            GraphNode dominator = postOrder[doms[i]];
            GraphNode child = postOrder[i];
            if (dominator != child) {
                dominator.addDomChild(child);
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
        for (GraphNode child : topNode.domChildren(this)) {
            if (set.isEmpty() || set.contains(child)) {
                if (splitLoops(child, set)) {
                    crossEdge = true;
                }
            }
        }
        if (crossEdge) {
            handleIrChildren(topNode, set);
        }
        for (GraphNode pred : topNode.predecessors()) {
            if (pred.isBackEdge(topNode) && !dom(topNode, pred)) {
                return true;
            }
        }
        return false;
    }

    private void handleIrChildren(GraphNode topNode, Set<GraphNode> set) throws DFANodeSplitBailoutException {
        ArrayDeque<GraphNode> dfsList = new ArrayDeque<>();
        ArrayList<Set<GraphNode>> sccList = new ArrayList<>();
        for (GraphNode child : topNode.domChildren(this)) {
            if (!child.done && (set.isEmpty() || set.contains(child))) {
                scc1(dfsList, child, set, topNode.level);
            }
        }
        for (GraphNode n : dfsList) {
            if (n.done) {
                Set<GraphNode> scc = new StateSet<>(this, new StateSetBackingSortedArray());
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
        for (GraphNode child : curNode.successors(this)) {
            if (!child.done && child.level > level && (set.isEmpty() || set.contains(child))) {
                scc1(dfsList, child, set, level);
            }
        }
        dfsList.push(curNode);
    }

    private void scc2(Set<GraphNode> scc, GraphNode curNode, int level) {
        curNode.done = false;
        for (GraphNode pred : curNode.predecessors()) {
            if (pred.done && pred.level > level) {
                scc2(scc, pred, level);
            }
        }
        scc.add(curNode);
    }

    private void handleScc(GraphNode graphNode, Set<GraphNode> scc) throws DFANodeSplitBailoutException {
        Set<GraphNode> msed = new StateSet<>(this, new StateSetBackingSortedArray());
        for (GraphNode n : scc) {
            if (n.level == graphNode.level + 1) {
                n.setWeightAndHeaders(this, n, scc);
                msed.add(n);
            }
        }
        if (msed.size() <= 1) {
            return;
        }
        splitSCC(chooseNode(msed), scc);

        createDomTree();
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
                n.createCopy(this, nextId++);
            }
        }
        for (GraphNode cur : scc) {
            if (cur.header != headerNode) {
                for (GraphNode suc : cur.successors(this)) {
                    if (suc.copy == null) {
                        suc.addPredecessor(cur.copy);
                    } else {
                        cur.copy.replaceSuccessor(suc);
                        suc.copy.replacePredecessor(cur);
                    }
                }
                Iterator<GraphNode> curPredecessors = cur.predecessors().iterator();
                while (curPredecessors.hasNext()) {
                    GraphNode pred = curPredecessors.next();
                    if (pred.copy == null) {
                        if (scc.contains(pred)) {
                            pred.replaceSuccessor(cur);
                            curPredecessors.remove();
                        } else {
                            cur.copy.removePredecessor(pred);
                        }
                    }
                }
            }
        }
        for (GraphNode g : new ArrayList<>(scc)) {
            if (g.header != headerNode) {
                scc.add(g.copy);
                g.copy = null;
            }
        }
    }

    private Set<GraphNode> findTopNodes(Set<GraphNode> scc) {
        Set<GraphNode> tops = new StateSet<>(this, new StateSetBackingSortedArray());
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
        for (GraphNode child : cnode.successors(this)) {
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
        for (GraphNode child : curNode.domChildren(this)) {
            setLevel(child, level + 1);
        }
    }

    private void markUndone() {
        for (GraphNode node : nodes) {
            node.markUndone();
        }
    }
}
