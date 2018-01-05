/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.runtime.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.result.TraceFinderResult;

public abstract class TraceFinderGetResultNode extends Node {

    public abstract int[] execute(TraceFinderResult receiver);

    @Specialization(guards = {"!receiver.isResultCalculated()"})
    int[] doTraceFinderCalc(TraceFinderResult receiver,
                    @Cached("create()") CalcResultNode calcResult) {
        receiver.setResultCalculated();
        final int preCalcIndex = (int) calcResult.execute(receiver.getTraceFinderCallTarget(),
                        new Object[]{receiver.getInput(), receiver.getEnd() - 1, receiver.getFromIndex()});
        receiver.getPreCalculatedResults()[preCalcIndex].applyRelativeToEnd(receiver.getIndices(), receiver.getEnd());
        return receiver.getIndices();
    }

    @Specialization(guards = {"receiver.isResultCalculated()"})
    int[] doTraceFinder(TraceFinderResult receiver) {
        return receiver.getIndices();
    }

    public static TraceFinderGetResultNode create() {
        return TraceFinderGetResultNodeGen.create();
    }
}
