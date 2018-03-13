/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNode.JSStringToNumberWithTrimNode;
import com.oracle.truffle.js.nodes.interop.JSUnboxOrGetNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * This implements ECMA 9.3 ToNumber, but always converting the result to a double value.
 *
 */
public abstract class JSToDoubleNode extends JavaScriptBaseNode {

    @Child private JSToDoubleNode toDoubleNode;

    public abstract Object execute(Object value);

    public final double executeDouble(Object value) {
        return (double) execute(value);
    }

    public static JSToDoubleNode create() {
        return JSToDoubleNodeGen.create();
    }

    @Specialization
    protected static double doInteger(int value) {
        return value;
    }

    @Specialization
    protected static double doBoolean(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    @Specialization
    protected static double doDouble(double value) {
        return value;
    }

    @Specialization(guards = "isJSNull(value)")
    protected static double doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization(guards = "isUndefined(value)")
    protected static double doUndefined(@SuppressWarnings("unused") Object value) {
        return Double.NaN;
    }

    @Specialization
    protected static double doStringDouble(String value,
                    @Cached("create()") JSStringToNumberWithTrimNode stringToNumberNode) {
        return stringToNumberNode.executeString(value);
    }

    @Specialization(guards = "isJSObject(value)")
    protected double doJSObject(DynamicObject value,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitiveNode) {
        return getToDoubleNode().executeDouble(toPrimitiveNode.execute(value));
    }

    @Specialization
    protected final Number doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value", this);
    }

    @Specialization(guards = "isForeignObject(object)")
    protected double doCrossLanguageToDouble(TruffleObject object,
                    @Cached("create()") JSUnboxOrGetNode interopUnboxNode) {
        return getToDoubleNode().executeDouble(interopUnboxNode.executeWithTarget(object));
    }

    private JSToDoubleNode getToDoubleNode() {
        if (toDoubleNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toDoubleNode = insert(JSToDoubleNode.create());
        }
        return toDoubleNode;
    }

    @Specialization(guards = "isJavaNumber(value)")
    protected static double doJavaNumber(Object value) {
        return JSRuntime.doubleValue((Number) value);
    }

    @Specialization(guards = "isJavaObject(value)")
    protected static double doJavaObject(@SuppressWarnings("unused") Object value) {
        return Double.NaN;
    }

}
