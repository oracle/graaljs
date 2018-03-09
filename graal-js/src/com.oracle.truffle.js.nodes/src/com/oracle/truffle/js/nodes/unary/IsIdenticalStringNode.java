/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.objects.JSLazyString;

public abstract class IsIdenticalStringNode extends JSUnaryNode {

    private final String string;

    protected IsIdenticalStringNode(String string, JavaScriptNode operand) {
        super(operand);
        this.string = string;
    }

    @Specialization
    protected boolean doString(JSLazyString other) {
        return string.equals(other.toString());
    }

    @Specialization
    protected boolean doString(String other) {
        return string.equals(other);
    }

    @Fallback
    protected boolean doOther(@SuppressWarnings("unused") Object other) {
        return false;
    }

    public static IsIdenticalStringNode create(String string, JavaScriptNode operand) {
        return IsIdenticalStringNodeGen.create(string, operand);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(string, cloneUninitialized(getOperand()));
    }
}
