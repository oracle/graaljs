/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNode.JSStringToNumberWithTrimNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * Basically ECMAScript ToInteger, but incorrect for very large values, which we don't care about in
 * array length or index conversion. Returns long.
 */
@ImportStatic(Double.class)
public abstract class JSToIntegerSpecialNode extends JavaScriptBaseNode {

    public static JSToIntegerSpecialNode create() {
        return JSToIntegerSpecialNodeGen.create();
    }

    public abstract long executeLong(Object operand);

    @Specialization
    protected static long doInteger(int value) {
        return value;
    }

    @Specialization
    protected static long doBoolean(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    @Specialization(guards = "!isInfinite(value)")
    protected static long doDouble(double value) {
        return (long) value;
    }

    @Specialization(guards = "isInfinite(value)")
    protected static long doDoubleInfinite(double value) {
        return value > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
    }

    @Specialization(guards = "isUndefined(value)")
    protected static long doUndefined(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization(guards = "isJSNull(value)")
    protected static long doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization
    protected static long doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value");
    }

    @Specialization
    protected long doString(String value,
                    @Cached("create()") JSToIntegerSpecialNode nestedToIntegerNode,
                    @Cached("create()") JSStringToNumberWithTrimNode stringToNumberNode) {
        return nestedToIntegerNode.executeLong(stringToNumberNode.executeString(value));
    }

    @Specialization(guards = "isJSObject(value)")
    protected long doJSObject(DynamicObject value,
                    @Cached("create()") JSToNumberNode toNumberNode) {
        return JSRuntime.toInteger(toNumberNode.executeNumber(value));
    }
}
