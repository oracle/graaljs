/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode.JSToNumberWrapperNode;

@NodeInfo(shortName = "+")
@NodeChild(value = "operand", type = JavaScriptNode.class)
public abstract class JSUnaryPlusNode extends JSToNumberWrapperNode {
    public static JSUnaryPlusNode create(JavaScriptNode operand) {
        return JSUnaryPlusNodeGen.create(operand);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSUnaryPlusNodeGen.create(cloneUninitialized(getOperand()));
    }

    @Override
    public String expressionToString() {
        return "(" + "+" + getOperand().expressionToString() + ")";
    }
}
