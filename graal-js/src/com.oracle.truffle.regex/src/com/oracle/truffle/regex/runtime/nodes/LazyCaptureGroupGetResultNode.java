/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.runtime.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.result.LazyCaptureGroupsResult;

public abstract class LazyCaptureGroupGetResultNode extends Node {

    public abstract int[] execute(LazyCaptureGroupsResult receiver);

    @Specialization(guards = {"receiver.getResult() == null", "receiver.getFindStartCallTarget() == null"})
    int[] doLazyCaptureGroupsCalc(LazyCaptureGroupsResult receiver,
                    @Cached("create()") CalcResultNode calcResult) {
        receiver.setResult((int[]) calcResult.execute(receiver.getCaptureGroupCallTarget(),
                        new Object[]{receiver.getInput(), receiver.getFromIndex(), receiver.getFromIndex(), receiver.getEnd()}));
        return receiver.getResult();
    }

    @Specialization(guards = {"receiver.getResult() == null", "receiver.getFindStartCallTarget() != null"})
    int[] doLazyCaptureGroupsCalcWithFindStart(LazyCaptureGroupsResult receiver,
                    @Cached("create()") CalcResultNode calcStart,
                    @Cached("create()") CalcResultNode calcResult) {
        final int start = (int) calcStart.execute(receiver.getFindStartCallTarget(),
                        new Object[]{receiver.getInput(), receiver.getEnd() - 1, receiver.getFromIndex()}) + 1;
        receiver.setResult((int[]) calcResult.execute(receiver.getCaptureGroupCallTarget(),
                        new Object[]{receiver.getInput(), receiver.getFromIndex(), start, receiver.getEnd()}));
        return receiver.getResult();
    }

    @Specialization(guards = {"receiver.getResult() != null"})
    int[] doLazyCaptureGroups(LazyCaptureGroupsResult receiver) {
        return receiver.getResult();
    }

    public static LazyCaptureGroupGetResultNode create() {
        return LazyCaptureGroupGetResultNodeGen.create();
    }
}
