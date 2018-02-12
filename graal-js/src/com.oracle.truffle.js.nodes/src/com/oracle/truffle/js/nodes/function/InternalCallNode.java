/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;

/**
 * Simple call helper node for internal calls, caching on the call target.
 */
public abstract class InternalCallNode extends JavaScriptBaseNode {
    static final int LIMIT = 3;

    protected InternalCallNode() {
    }

    public static InternalCallNode create() {
        return InternalCallNodeGen.create();
    }

    public abstract Object execute(CallTarget callTarget, Object[] arguments);

    @SuppressWarnings("unused")
    @Specialization(guards = "callTarget == cachedCallTarget", limit = "LIMIT")
    protected static Object directCall(CallTarget callTarget, Object[] arguments,
                    @Cached("callTarget") CallTarget cachedCallTarget,
                    @Cached("create(cachedCallTarget)") DirectCallNode directCallNode) {
        return directCallNode.call(arguments);
    }

    @Specialization
    protected static Object indirectCall(CallTarget callTarget, Object[] arguments,
                    @Cached("create()") IndirectCallNode indirectCallNode) {
        return indirectCallNode.call(callTarget, arguments);
    }
}
