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
import com.oracle.truffle.js.runtime.JSTruffleOptions;

@NodeInfo(shortName = "^")
public abstract class JSBitwiseXorNode extends JSBinaryNode {

    protected JSBitwiseXorNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        Truncatable.truncate(left);
        if (JSTruffleOptions.UseSuperOperations && right instanceof JSConstantIntegerNode) {
            int rightValue = ((JSConstantIntegerNode) right).executeInt(null);
            return JSBitwiseXorConstantNode.create(left, rightValue);
        }
        Truncatable.truncate(right);
        return JSBitwiseXorNodeGen.create(left, right);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == int.class;
    }

    public abstract int executeInt(Object a, Object b);

    @Specialization
    protected int doInteger(int a, int b) {
        return a ^ b;
    }

    @Specialization(replaces = "doInteger")
    protected int doGeneric(Object a, Object b,
                    @Cached("create()") JSToInt32Node leftInt32,
                    @Cached("create()") JSToInt32Node rightInt32) {
        return doInteger(leftInt32.executeInt(a), rightInt32.executeInt(b));
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSBitwiseXorNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
