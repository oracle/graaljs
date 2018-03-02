/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

/**
 * 11.7.3 The Unsigned Right Shift Operator (>>>).
 */
@NodeInfo(shortName = ">>>")
public abstract class JSUnsignedRightShiftNode extends JSBinaryNode {

    @Child private JSToUInt32Node toUInt32Node;

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        Truncatable.truncate(left);
        Truncatable.truncate(right);
        if (JSTruffleOptions.UseSuperOperations && right instanceof JSConstantIntegerNode) {
            return JSUnsignedRightShiftConstantNode.create(left, right);
        }
        return JSUnsignedRightShiftNodeGen.create(left, right);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == BinaryExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    protected static boolean rvalZero(int b) {
        return (b & 0x1f) == 0;
    }

    @Specialization(guards = {"rvalZero(b)", "a >= 0"})
    protected int doIntegerFast(int a, @SuppressWarnings("unused") int b) {
        return a;
    }

    @Specialization(guards = "a >= 0")
    protected int doInteger(int a, int b) {
        return a >>> b;
    }

    @Specialization(guards = "!rvalZero(b)")
    protected int doIntegerNegative(int a, int b) {
        return a >>> b;
    }

    @Specialization(guards = "rvalZero(b)")
    protected double doDoubleZero(double a, @SuppressWarnings("unused") int b) {
        return toUInt32(a);
    }

    @Specialization(guards = "!rvalZero(b)")
    protected int doDouble(double a, int b) {
        return (int) (toUInt32(a) >>> (b & 0x1F));
    }

    @Specialization(guards = "!isHandled(lval, rval)")
    protected Object doGeneric(Object lval, Object rval,
                    @Cached("create()") JSToUInt32Node rvalToUint32Node,
                    @Cached("createBinaryProfile()") ConditionProfile returnType) {
        long lnum = toUInt32(lval);
        int shiftCount = (int) (rvalToUint32Node.executeLong(rval) & 0x1F);
        if (returnType.profile(lnum >= Integer.MAX_VALUE || lnum <= Integer.MIN_VALUE)) {
            return (double) (lnum >>> shiftCount);
        }
        return (int) (lnum >>> shiftCount);
    }

    private long toUInt32(Object target) {
        if (toUInt32Node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toUInt32Node = insert(JSToUInt32Node.create());
        }
        return toUInt32Node.executeLong(target);
    }

    protected static boolean isHandled(Object lval, Object rval) {
        return (lval instanceof Integer || lval instanceof Double) && rval instanceof Integer;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Number.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSUnsignedRightShiftNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
