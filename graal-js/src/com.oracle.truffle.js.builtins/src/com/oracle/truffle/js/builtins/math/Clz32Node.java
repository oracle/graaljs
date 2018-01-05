/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class Clz32Node extends MathOperation {
    public Clz32Node(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization
    protected static int clz32(int a) {
        return Integer.numberOfLeadingZeros(a);
    }

    @Specialization
    protected int clz32(Object a,
                    @Cached("create()") JSToUInt32Node toUInt32Node) {
        return clz32((int) toUInt32Node.executeLong(a));
    }
}
