/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * Basically ECMAScript ToInteger, but incorrect for values outside the int32 range. Used by
 * built-in functions that do not care about values outside this range.
 */
public abstract class JSToIntegerNode extends JavaScriptBaseNode {

    @Child private JSToNumberNode toNumberNode;

    public static JSToIntegerNode create() {
        return JSToIntegerNodeGen.create();
    }

    public abstract int executeInt(Object operand);

    @Specialization
    protected static int doInteger(int value) {
        return value;
    }

    @Specialization
    protected static int doBoolean(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    protected static boolean inInt32Range(double value) {
        return value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE;
    }

    @Specialization(guards = "inInt32Range(value)")
    protected static int doDoubleInt32Range(double value) {
        return (int) ((long) value & 0xFFFFFFFFL);
    }

    @Specialization(guards = "!inInt32Range(value)")
    protected static int doDouble(double value) {
        if (Double.isNaN(value)) {
            return 0;
        } else if (value > 0) {
            return Integer.MAX_VALUE;
        } else {
            return Integer.MIN_VALUE;
        }
    }

    @Specialization(guards = "isUndefined(value)")
    protected static int doUndefined(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization(guards = "isJSNull(value)")
    protected static int doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization
    protected final int doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value", this);
    }

    @Specialization
    protected int doString(String value,
                    @Cached("create()") JSToIntegerNode nestedToIntegerNode,
                    @Cached("create()") JSStringToNumberWithTrimNode stringToNumberNode) {
        return nestedToIntegerNode.executeInt(stringToNumberNode.executeString(value));
    }

    @Specialization(guards = "isJSObject(value)")
    protected int doJSObject(DynamicObject value) {
        return JSRuntime.toInt32(getToNumberNode().executeNumber(value));
    }

    @Specialization(guards = "isForeignObject(object)")
    protected int doCrossLanguage(TruffleObject object,
                    @Cached("create()") JSUnboxOrGetNode unboxOrGetNode) {
        Object unboxedForeign = unboxOrGetNode.executeWithTarget(object);
        return JSRuntime.toInt32(getToNumberNode().executeNumber(unboxedForeign));
    }

    private JSToNumberNode getToNumberNode() {
        if (toNumberNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toNumberNode = insert(JSToNumberNode.create());
        }
        return toNumberNode;
    }
}
