/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class IsIdenticalUndefinedNode extends JSUnaryNode {

    protected IsIdenticalUndefinedNode(JavaScriptNode operand) {
        super(operand);
    }

    @Specialization
    protected boolean doDynamicObject(Object a) {
        return a == Undefined.instance;
    }

    public static IsIdenticalUndefinedNode create(JavaScriptNode operand) {
        return IsIdenticalUndefinedNodeGen.create(operand);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(getOperand()));
    }
}
