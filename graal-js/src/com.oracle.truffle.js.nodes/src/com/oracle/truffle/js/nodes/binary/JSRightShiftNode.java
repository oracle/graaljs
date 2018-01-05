/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

/**
 * 11.7.2 The Signed Right Shift Operator ( >> ).
 */
@NodeInfo(shortName = ">>")
public abstract class JSRightShiftNode extends JSBinaryIntegerShiftNode {

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        Truncatable.truncate(left);
        Truncatable.truncate(right);
        if (JSTruffleOptions.UseSuperOperations && right instanceof JSConstantIntegerNode) {
            return JSRightShiftConstantNode.create(left, right);
        }
        return JSRightShiftNodeGen.create(left, right);
    }

    public abstract int executeInt(int a, Object b);

    @Specialization
    protected int doInteger(int a, int b) {
        return a >> b;
    }

    @Specialization(guards = "!largerThan2e32(b)")
    protected int doDouble(int a, double b) {
        return a >> (int) ((long) b);
    }

    @Specialization(replaces = {"doInteger", "doDouble"})
    protected int doGeneric(Object a, Object b,
                    @Cached("create()") JSRightShiftNode rightShift,
                    @Cached("create()") JSToInt32Node leftInt32,
                    @Cached("create()") JSToUInt32Node rightUInt32) {
        return rightShift.executeInt(leftInt32.executeInt(a), rightUInt32.execute(b));
    }

    public static JSRightShiftNode create() {
        return JSRightShiftNodeGen.create(null, null);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == int.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSRightShiftNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
