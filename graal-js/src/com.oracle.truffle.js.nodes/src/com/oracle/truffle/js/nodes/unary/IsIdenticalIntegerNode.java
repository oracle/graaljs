/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSRuntime;

public abstract class IsIdenticalIntegerNode extends JSUnaryNode {

    private final int integer;

    protected IsIdenticalIntegerNode(JavaScriptNode operand, int integer) {
        super(operand);
        this.integer = integer;
    }

    @Specialization
    protected boolean doInt(int a) {
        return a == integer;
    }

    @Specialization
    protected boolean doDouble(double a) {
        return a == integer;
    }

    // long etc could come via Interop
    @Specialization(guards = "isJavaNumber(a)")
    protected boolean doJavaNumber(Object a) {
        double doubleValue = JSRuntime.toDouble(a);
        return doubleValue == integer;
    }

    @Fallback
    protected boolean doOther(@SuppressWarnings("unused") Object other) {
        return false;
    }

    public static IsIdenticalIntegerNode create(int integer, JavaScriptNode operand) {
        return IsIdenticalIntegerNodeGen.create(operand, integer);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(integer, cloneUninitialized(getOperand()));
    }
}
