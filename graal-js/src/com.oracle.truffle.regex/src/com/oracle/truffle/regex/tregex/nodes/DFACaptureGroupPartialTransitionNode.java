/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.tregex.nfa.GroupBoundaries;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.Arrays;

public final class DFACaptureGroupPartialTransitionNode extends Node {

    public static final int FINAL_STATE_RESULT_INDEX = 0;

    public static final byte[] EMPTY_ARRAY_COPIES = {};
    public static final byte[][] EMPTY_INDEX_UPDATES = {};
    public static final byte[][] EMPTY_INDEX_CLEARS = {};

    @CompilerDirectives.CompilationFinal(dimensions = 1) private final byte[] newOrder;
    @CompilerDirectives.CompilationFinal(dimensions = 1) private final byte[] arrayCopies;
    @CompilerDirectives.CompilationFinal(dimensions = 2) private final byte[][] indexUpdates;
    @CompilerDirectives.CompilationFinal(dimensions = 2) private final byte[][] indexClears;

    private DFACaptureGroupPartialTransitionNode(byte[] newOrder, byte[] arrayCopies, byte[][] indexUpdates, byte[][] indexClears) {
        this.newOrder = newOrder;
        this.arrayCopies = arrayCopies;
        this.indexUpdates = indexUpdates;
        this.indexClears = indexClears;
    }

    public static DFACaptureGroupPartialTransitionNode create(byte[] newOrder, byte[] arrayCopies, byte[][] indexUpdates, byte[][] indexClears) {
        return new DFACaptureGroupPartialTransitionNode(newOrder, arrayCopies, indexUpdates, indexClears);
    }

    public boolean doesReorderResults() {
        return newOrder != null;
    }

    public void apply(DFACaptureGroupTrackingData d, final int currentIndex) {
        CompilerAsserts.partialEvaluationConstant(this);
        if (newOrder != null) {
            System.arraycopy(d.currentResultOrder, 0, d.swap, 0, newOrder.length);
            applyNewOrder(d.currentResultOrder, d.swap);
        }
        applyArrayCopy(d.results, d.currentResultOrder);
        applyIndexUpdate(d.results, d.currentResultOrder, currentIndex);
        applyIndexClear(d.results, d.currentResultOrder);
    }

    @ExplodeLoop
    private void applyNewOrder(int[] currentResultOrder, int[] swap) {
        for (int i = 0; i < newOrder.length; i++) {
            currentResultOrder[i] = swap[Byte.toUnsignedInt(newOrder[i])];
        }
    }

    @ExplodeLoop
    private void applyArrayCopy(int[][] results, int[] currentResultOrder) {
        for (int i = 0; i < arrayCopies.length; i += 2) {
            final int source = Byte.toUnsignedInt(arrayCopies[i]);
            final int target = Byte.toUnsignedInt(arrayCopies[i + 1]);
            System.arraycopy(results[currentResultOrder[source]], 0, results[currentResultOrder[target]], 0, results[currentResultOrder[source]].length);
        }
    }

    @ExplodeLoop
    private void applyIndexUpdate(int[][] results, int[] currentResultOrder, int currentIndex) {
        for (byte[] indexUpdate : indexUpdates) {
            assert indexUpdate.length > 1;
            final int targetArray = Byte.toUnsignedInt(indexUpdate[0]);
            for (int i = 1; i < indexUpdate.length; i++) {
                final int targetIndex = Byte.toUnsignedInt(indexUpdate[i]);
                results[currentResultOrder[targetArray]][targetIndex] = currentIndex;
            }
        }
    }

    @ExplodeLoop
    private void applyIndexClear(int[][] results, int[] currentResultOrder) {
        for (byte[] indexClear : indexClears) {
            assert indexClear.length > 1;
            final int targetArray = Byte.toUnsignedInt(indexClear[0]);
            for (int i = 1; i < indexClear.length; i++) {
                final int targetIndex = Byte.toUnsignedInt(indexClear[i]);
                results[currentResultOrder[targetArray]][targetIndex] = 0;
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof DFACaptureGroupPartialTransitionNode)) {
            return false;
        }
        DFACaptureGroupPartialTransitionNode o = (DFACaptureGroupPartialTransitionNode) obj;
        return Arrays.equals(newOrder, o.newOrder) && Arrays.equals(arrayCopies, o.arrayCopies) &&
                        Arrays.deepEquals(indexUpdates, o.indexUpdates) && Arrays.deepEquals(indexClears, o.indexClears);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = Arrays.hashCode(newOrder);
        result = prime * result + Arrays.hashCode(arrayCopies);
        result = prime * result + Arrays.deepHashCode(indexUpdates);
        result = prime * result + Arrays.deepHashCode(indexClears);
        return result;
    }

    public DebugUtil.Table toTable() {
        return toTable("DfaCGTransition");
    }

    public DebugUtil.Table toTable(String name) {
        DebugUtil.Table table = new DebugUtil.Table(name);
        table.append(new DebugUtil.Value("newOrder", Arrays.toString(newOrder)));
        for (int i = 0; i < arrayCopies.length; i += 2) {
            final int source = Byte.toUnsignedInt(arrayCopies[i]);
            final int target = Byte.toUnsignedInt(arrayCopies[i + 1]);
            table.append(new DebugUtil.Table("ArrayCopy",
                            new DebugUtil.Value("source", source),
                            new DebugUtil.Value("target", target)));
        }
        for (byte[] indexUpdate : indexUpdates) {
            table.append(indexManipulationToTable("IndexUpdate", indexUpdate));
        }
        for (byte[] indexClear : indexClears) {
            table.append(indexManipulationToTable("IndexClear", indexClear));
        }
        return table;
    }

    private static DebugUtil.Table indexManipulationToTable(String name, byte[] values) {
        return new DebugUtil.Table(name,
                        new DebugUtil.Value("target", Byte.toUnsignedInt(values[0])),
                        new DebugUtil.Value("groupStarts", GroupBoundaries.gbArrayGroupEntriesToString(values, 1)),
                        new DebugUtil.Value("groupEnds", GroupBoundaries.gbArrayGroupExitsToString(values, 1)));
    }
}
