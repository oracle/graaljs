/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
        if (DebugUtil.DEBUG_STEP_EXECUTION) {
            System.out.println("applying " + this);
        }
        CompilerAsserts.partialEvaluationConstant(this);
        if (newOrder != null) {
            System.arraycopy(d.currentResultOrder, 0, d.swap, 0, newOrder.length);
            applyNewOrder(d.currentResultOrder, d.swap);
        }
        applyArrayCopy(d.results, d.currentResultOrder);
        applyIndexUpdate(d.results, d.currentResultOrder, currentIndex);
        applyIndexClear(d.results, d.currentResultOrder);
    }

    public void applyFinalStateTransition(DFACaptureGroupTrackingData d, boolean searching, final int currentIndex) {
        if (DebugUtil.DEBUG_STEP_EXECUTION) {
            System.out.println("applying final state transition " + this);
        }
        CompilerAsserts.partialEvaluationConstant(this);
        if (!searching) {
            apply(d, currentIndex);
            return;
        }
        CompilerAsserts.partialEvaluationConstant(this);
        final int source;
        if (newOrder == null) {
            source = 0;
        } else {
            source = Byte.toUnsignedInt(newOrder[0]);
        }
        System.arraycopy(d.results[d.currentResultOrder[source]], 0, d.currentResult, 0, d.currentResult.length);
        assert arrayCopies.length == 0;
        assert indexUpdates.length <= 1;
        assert indexClears.length <= 1;
        if (indexUpdates.length == 1) {
            assert indexUpdates[0][0] == 0;
            applyFinalStateTransitionIndexUpdates(d, currentIndex);
        }
        if (indexClears.length == 1) {
            assert indexClears[0][0] == 0;
            applyFinalStateTransitionIndexClears(d);
        }
    }

    @ExplodeLoop
    private void applyFinalStateTransitionIndexUpdates(DFACaptureGroupTrackingData d, int currentIndex) {
        for (int i = 1; i < indexUpdates[0].length; i++) {
            final int targetIndex = Byte.toUnsignedInt(indexUpdates[0][i]);
            d.currentResult[targetIndex] = currentIndex;
        }
    }

    @ExplodeLoop
    private void applyFinalStateTransitionIndexClears(DFACaptureGroupTrackingData d) {
        for (int i = 1; i < indexClears[0].length; i++) {
            final int targetIndex = Byte.toUnsignedInt(indexClears[0][i]);
            d.currentResult[targetIndex] = 0;
        }
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DfaCGTransition");
        if (newOrder != null && newOrder.length > 0) {
            sb.append(System.lineSeparator()).append("newOrder: ").append(Arrays.toString(newOrder));
        }
        if (arrayCopies.length > 0) {
            sb.append(System.lineSeparator()).append("arrayCopies: ");
            for (int i = 0; i < arrayCopies.length; i += 2) {
                final int source = Byte.toUnsignedInt(arrayCopies[i]);
                final int target = Byte.toUnsignedInt(arrayCopies[i + 1]);
                sb.append(System.lineSeparator()).append("    ").append(source).append(" -> ").append(target);
            }
        }
        indexManipulationsToString(sb, indexUpdates, "indexUpdates");
        indexManipulationsToString(sb, indexClears, "indexClears");
        return sb.toString();
    }

    private static void indexManipulationsToString(StringBuilder sb, byte[][] indexManipulations, String name) {
        if (indexManipulations.length > 0) {
            sb.append(System.lineSeparator()).append(name).append(": ");
            for (byte[] indexManipulation : indexManipulations) {
                final int targetArray = Byte.toUnsignedInt(indexManipulation[0]);
                sb.append(System.lineSeparator()).append("    ").append(targetArray).append(" <- [");
                for (int i = 1; i < indexManipulation.length; i++) {
                    if (i > 1) {
                        sb.append(", ");
                    }
                    sb.append(Byte.toUnsignedInt(indexManipulation[i]));
                }
                sb.append("]");
            }
        }
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
