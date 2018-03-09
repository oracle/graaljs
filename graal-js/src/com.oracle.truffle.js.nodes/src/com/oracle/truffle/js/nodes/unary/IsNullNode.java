/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.objects.Null;

public abstract class IsNullNode extends JSUnaryNode {

    protected IsNullNode(JavaScriptNode operand) {
        super(operand);
    }

    @Specialization
    protected static boolean doCached(Object operand) {
        return operand == Null.instance;
    }

    public static IsNullNode create(JavaScriptNode operand) {
        return IsNullNodeGen.create(operand);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return IsNullNode.create(cloneUninitialized(getOperand()));
    }
}
