/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.runtime.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

public abstract class CalcResultNode extends Node {
    public abstract Object execute(CallTarget receiver, Object[] args);

    @Specialization(guards = {"target == cachedTarget"})
    Object executeDirect(@SuppressWarnings("unused") CallTarget target, Object[] args,
                    @Cached("target") @SuppressWarnings("unused") CallTarget cachedTarget,
                    @Cached("create(cachedTarget)") DirectCallNode callNode) {
        return callNode.call(args);
    }

    @Specialization(replaces = "executeDirect")
    Object executeIndirect(CallTarget target, Object[] args,
                    @Cached("create()") IndirectCallNode callNode) {
        return callNode.call(target, args);
    }

    public static CalcResultNode create() {
        return CalcResultNodeGen.create();
    }
}
