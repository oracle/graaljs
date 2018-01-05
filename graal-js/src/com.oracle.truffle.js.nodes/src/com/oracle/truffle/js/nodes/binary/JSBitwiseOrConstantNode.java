/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import java.util.Objects;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;

@NodeInfo(shortName = "|")
@NodeField(name = "rightValue", type = int.class)
public abstract class JSBitwiseOrConstantNode extends JSUnaryNode {

    public static JavaScriptNode create(JavaScriptNode left, int rightValue) {
        return JSBitwiseOrConstantNodeGen.create(left, rightValue);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == int.class;
    }

    public abstract int executeInt(Object a);

    public abstract int getRightValue();

    @Specialization
    protected int doInteger(int a) {
        return a | getRightValue();
    }

    @Specialization(replaces = "doInteger")
    protected int doGeneric(Object a,
                    @Cached("create()") JSToInt32Node leftInt32) {
        return doInteger(leftInt32.executeInt(a));
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSBitwiseOrConstantNodeGen.create(cloneUninitialized(getOperand()), getRightValue());
    }

    @Override
    public String expressionToString() {
        if (getOperand() != null) {
            return "(" + Objects.toString(getOperand().expressionToString(), INTERMEDIATE_VALUE) + " | " + getRightValue() + ")";
        }
        return null;
    }
}
