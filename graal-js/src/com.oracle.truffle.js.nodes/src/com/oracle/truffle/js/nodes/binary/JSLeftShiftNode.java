/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

@NodeInfo(shortName = "<<")
public abstract class JSLeftShiftNode extends JSBinaryIntegerShiftNode {

    protected JSLeftShiftNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        Truncatable.truncate(left);
        Truncatable.truncate(right);
        if (JSTruffleOptions.UseSuperOperations && right instanceof JSConstantIntegerNode) {
            return JSLeftShiftConstantNode.create(left, right);
        }
        return JSLeftShiftNodeGen.create(left, right);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == BinaryExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    public abstract int executeInt(int a, Object b);

    @Specialization
    protected int doInteger(int a, int b) {
        return a << b;
    }

    @Specialization(guards = "!largerThan2e32(b)")
    protected int doIntegerDouble(int a, double b) {
        return a << (int) ((long) b);
    }

    @Specialization(replaces = {"doInteger", "doIntegerDouble"})
    protected int doGeneric(Object a, Object b,
                    @Cached("create()") JSLeftShiftNode leftShift,
                    @Cached("create()") JSToInt32Node leftInt32,
                    @Cached("create()") JSToUInt32Node rightUInt32) {
        return leftShift.executeInt(leftInt32.executeInt(a), rightUInt32.execute(b));
    }

    public static JSLeftShiftNode create() {
        return JSLeftShiftNodeGen.create(null, null);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == int.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSLeftShiftNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
