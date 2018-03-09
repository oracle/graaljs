/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptNode;

public abstract class IsIdenticalBooleanNode extends JSUnaryNode {

    private final boolean bool;

    protected IsIdenticalBooleanNode(JavaScriptNode operand, boolean bool) {
        super(operand);
        this.bool = bool;
    }

    @Specialization
    protected boolean doBoolean(boolean a) {
        return a == bool;
    }

    @Fallback
    protected boolean doOther(@SuppressWarnings("unused") Object other) {
        return false;
    }

    public static IsIdenticalBooleanNode create(boolean bool, JavaScriptNode operand) {
        return IsIdenticalBooleanNodeGen.create(operand, bool);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return IsIdenticalBooleanNode.create(bool, cloneUninitialized(getOperand()));
    }
}
