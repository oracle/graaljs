/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodesplitter;

import com.oracle.truffle.regex.tregex.automaton.StateIndex;

import java.util.ArrayList;

/**
 * An abstract graph wrapper used by {@link DFANodeSplit}.
 */
class Graph implements StateIndex<GraphNode> {

    private GraphNode start;
    private final ArrayList<GraphNode> nodes;

    Graph(int initialCapacity) {
        this.nodes = new ArrayList<>(initialCapacity);
    }

    public GraphNode getStart() {
        return start;
    }

    public void setStart(GraphNode start) {
        this.start = start;
    }

    public ArrayList<GraphNode> getNodes() {
        return nodes;
    }

    public GraphNode getNode(int id) {
        return nodes.get(id);
    }

    public void addGraphNode(GraphNode graphNode) {
        assert graphNode.getId() == nodes.size();
        nodes.add(graphNode);
        assert graphNode == nodes.get(graphNode.getId());
    }

    public int size() {
        return nodes.size();
    }

    @Override
    public int getNumberOfStates() {
        return size();
    }

    @Override
    public GraphNode getState(int id) {
        return getNode(id);
    }
}
