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
import com.oracle.truffle.regex.tregex.buffer.ShortArrayBuffer;
import com.oracle.truffle.regex.tregex.nodes.DFAStateNode;
import com.oracle.truffle.regex.util.CompilationFinalBitSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

        private DFAStateNode dfaNode;
        boolean dfaNodeCopied = false;
        private final short[] successorSet;

        private short[] predecessorSet;
        private int nPredecessors = 0;

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

        GraphNode(DFANodeSplit graph) {
            this(graph, null, null);
        }

        GraphNode(DFANodeSplit graph, DFAStateNode dfaNode, short[] successorSet) {
            this.dfaNode = dfaNode;
            this.successorSet = successorSet;
            predecessorSet = new short[10];
            backEdges = new StateSet<>(graph);
            domChildren = NO_DOM_CHILDREN;
        }

        GraphNode(GraphNode cpy, short dfaNodeId) {
            this.dfaNode = cpy.dfaNode.createNodeSplitCopy(dfaNodeId);
            this.dfaNodeCopied = true;
            this.successorSet = cpy.successorSet;

            this.predecessorSet = Arrays.copyOf(cpy.predecessorSet, cpy.predecessorSet.length);
            this.nPredecessors = cpy.nPredecessors;

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

        void createCopy(DFANodeSplit graph, short dfaNodeId, List<GraphNode> nodes) {
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

        int findPredecessor(GraphNode pre) {
            return Arrays.binarySearch(predecessorSet, 0, nPredecessors, pre.getId());
        }

        boolean hasPredecessor(GraphNode pre) {
            return findPredecessor(pre) >= 0;
        }

        private void checkGrowPredecessors() {
            if (predecessorSet.length == nPredecessors) {
                predecessorSet = Arrays.copyOf(predecessorSet, predecessorSet.length * 2);
            }
        }

        void addPredecessorUnsorted(GraphNode pre) {
            checkGrowPredecessors();
            predecessorSet[nPredecessors++] = pre.getId();
        }

        void sortPredecessors() {
            Arrays.sort(predecessorSet, 0, nPredecessors);
        }

        void addPredecessor(GraphNode pre) {
            checkGrowPredecessors();
            int searchResult = findPredecessor(pre);
            assert searchResult < 0;
            int insertionPoint = (searchResult + 1) * (-1);
            System.arraycopy(predecessorSet, insertionPoint, predecessorSet, insertionPoint + 1, nPredecessors - insertionPoint);
            nPredecessors++;
            predecessorSet[insertionPoint] = pre.getId();
        }

        void replacePredecessor(GraphNode pre) {
            int searchResult = findPredecessor(pre.copy);
            assert searchResult < 0;
            int insertionPoint = (searchResult + 1) * (-1);
            int deletionPoint = findPredecessor(pre);
            assert deletionPoint >= 0;
            if (insertionPoint < deletionPoint) {
                System.arraycopy(predecessorSet, insertionPoint, predecessorSet, insertionPoint + 1, deletionPoint - insertionPoint);
            } else if (insertionPoint > deletionPoint) {
                insertionPoint--;
                System.arraycopy(predecessorSet, deletionPoint + 1, predecessorSet, deletionPoint, insertionPoint - deletionPoint);
            }
            predecessorSet[insertionPoint] = pre.copy.getId();
        }

        void removePredecessor(GraphNode pre) {
            removePredecessor(findPredecessor(pre));
        }

        private void removePredecessor(int i) {
            assert i >= 0;
            System.arraycopy(predecessorSet, i + 1, predecessorSet, i, nPredecessors - (i + 1));
            nPredecessors--;
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

        Iterable<GraphNode> predecessors(StateIndex<GraphNode> index) {
            return () -> new Iterator<GraphNode>() {

                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < nPredecessors;
                }

                @Override
                public GraphNode next() {
                    return index.getState(predecessorSet[i++]);
                }

                @Override
                public void remove() {
                    removePredecessor(--i);
                }
            };
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

    private static class EntryNode extends GraphNode {

        private short[] entries;

        private EntryNode(DFANodeSplit graph) {
            super(graph);
        }

        public void setEntries(short[] entries) {
            this.entries = entries;
        }

        @Override
        public short getId() {
            return Short.MAX_VALUE;
        }

        @Override
        int nodeWeight() {
            return 0;
        }

        @Override
        void createCopy(DFANodeSplit graph, short dfaNodeId, List<GraphNode> nodes) {
            throw new UnsupportedOperationException();
        }

        @Override
        void replaceSuccessor(GraphNode suc) {
            throw new UnsupportedOperationException();
        }

        @Override
        Iterable<GraphNode> successors(StateIndex<GraphNode> index) {
            return () -> new Iterator<GraphNode>() {

                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < entries.length;
                }

                @Override
                public GraphNode next() {
                    return index.getState(entries[i++]);
                }
            };
        }
    }

    private final EntryNode start;
    private final ArrayList<GraphNode> nodes;
    private short nextId;

    private int nextPostOrderIndex;
    private GraphNode[] postOrder;
    private int[] doms;

    private DFANodeSplit(short[] anchoredEntries, short[] unAnchoredEntries, DFAStateNode[] dfa) {
        nodes = new ArrayList<>(dfa.length);
        CompilationFinalBitSet successorBitSet = new CompilationFinalBitSet(dfa.length);
        ShortArrayBuffer successorBuffer = new ShortArrayBuffer();
        for (DFAStateNode n : dfa) {
            for (int i = 0; i < n.getSuccessors().length; i++) {
                if (!successorBitSet.get(n.getSuccessors()[i])) {
                    successorBuffer.add((short) i);
                }
                successorBitSet.set(n.getSuccessors()[i]);
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
        start = new EntryNode(this);
        for (short[] arr : new short[][] {anchoredEntries, unAnchoredEntries}) {
            for (short i : arr) {
                if (i != -1) {
                    if (!successorBitSet.get(i)) {
                        successorBuffer.add(i);
                    }
                    successorBitSet.set(i);
                }
            }
        }
        start.setEntries(successorBuffer.toArray());
        for (GraphNode successor : start.successors(this)) {
            successor.addPredecessorUnsorted(start);
        }
        for (GraphNode n : nodes) {
            n.sortPredecessors();
        }
    }

    private void addGraphNode(GraphNode graphNode) {
        assert graphNode.getId() == nodes.size();
        nodes.add(graphNode);
        assert graphNode == nodes.get(graphNode.getId());
    }

    public static DFAStateNode[] createReducibleGraph(short[] anchoredEntries, short[] unAnchoredEntries, DFAStateNode[] nodes) throws DFANodeSplitBailoutException {
        return new DFANodeSplit(anchoredEntries, unAnchoredEntries, nodes).process();
    }

    @Override
    public int getNumberOfStates() {
        return nodes.size() + 1;
    }

    @Override
    public GraphNode getState(int id) {
        if (id == Short.MAX_VALUE) {
            return start;
        }
        return nodes.get(id);
    }

    private DFAStateNode[] process() throws DFANodeSplitBailoutException {
        createDomTree();
        searchBackEdges(start);
        markUndone();
        splitLoops(start, new TreeSet<>());
        DFAStateNode[] ret = new DFAStateNode[nodes.size()];
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
        for (GraphNode s : start.successors(this)) {
            if (!s.hasPredecessor(start)) {
                return false;
            }
        }
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
        for (GraphNode n : cur.successors(this)) {
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
                for (GraphNode p : b.predecessors(this)) {
                    if (p.postOrderIndex > i) {
                        selectedPredecessor = p;
                        break;
                    }
                }
                if (selectedPredecessor == null) {
                    throw new IllegalStateException();
                }
                int newIDom = selectedPredecessor.postOrderIndex;
                for (GraphNode p : b.predecessors(this)) {
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
        start.clearDomChildren();
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
        for (GraphNode pred : topNode.predecessors(this)) {
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
        for (GraphNode child : curNode.successors(this)) {
            if (!child.done && child.level > level && (set.isEmpty() || set.contains(child))) {
                scc1(dfsList, child, set, level);
            }
        }
        dfsList.push(curNode);
    }

    private void scc2(Set<GraphNode> scc, GraphNode curNode, int level) {
        curNode.done = false;
        for (GraphNode pred : curNode.predecessors(this)) {
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
                n.createCopy(this, nextId++, nodes);
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
                Iterator<GraphNode> curPredecessors = cur.predecessors(this).iterator();
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
        start.markUndone();
        for (GraphNode node : nodes) {
            node.markUndone();
        }
    }
}
