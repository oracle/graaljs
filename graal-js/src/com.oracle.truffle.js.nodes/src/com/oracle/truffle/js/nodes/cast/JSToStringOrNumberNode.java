/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * This node is intended to be used only by comparison operators.
 */
public abstract class JSToStringOrNumberNode extends JavaScriptBaseNode {

    public abstract Object execute(Object operand);

    public static JSToStringOrNumberNode create() {
        return JSToStringOrNumberNodeGen.create();
    }

    @Specialization
    protected int doInteger(int value) {
        return value;
    }

    @Specialization
    protected LargeInteger doLargeInteger(LargeInteger value) {
        return value;
    }

    @Specialization
    protected int doBoolean(boolean value) {
        return doBooleanStatic(value);
    }

    private static int doBooleanStatic(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    @Specialization
    protected double doDouble(double value) {
        return value;
    }

    @Specialization
    protected String doString(String value) {
        return value;
    }

    @Specialization(guards = "isJSObject(value)")
    protected double doJSObject(DynamicObject value,
                    @Cached("create()") JSToDoubleNode toDoubleNode) {
        return toDoubleNode.executeDouble(value);
    }

    @Specialization(guards = "isJSNull(value)")
    protected int doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization
    protected Object doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value");
    }

    @Specialization(guards = "isUndefined(value)")
    protected double doUndefined(@SuppressWarnings("unused") Object value) {
        return Double.NaN;
    }

    @Specialization(guards = "isJavaNumber(value)")
    protected double doNumber(Object value) {
        return JSRuntime.doubleValue((Number) value);
    }
}
