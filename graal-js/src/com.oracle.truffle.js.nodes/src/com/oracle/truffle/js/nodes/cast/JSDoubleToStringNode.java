/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * This implements 9.8.1 ToString Applied to the Number Type.
 *
 */
public abstract class JSDoubleToStringNode extends JavaScriptBaseNode {

    public static JSDoubleToStringNode create() {
        return JSDoubleToStringNodeGen.create();
    }

    public abstract String executeString(Object operand);

    @Specialization
    protected static String doInt(int i) {
        return Boundaries.stringValueOf(i);
    }

    @Specialization
    protected static String doLong(long i) {
        return Boundaries.stringValueOf(i);
    }

    @Specialization
    protected static String doDouble(double d,
                    @Cached("createBinaryProfile()") ConditionProfile isInt,
                    @Cached("createBinaryProfile()") ConditionProfile isNaN,
                    @Cached("createBinaryProfile()") ConditionProfile isPositiveInfinity,
                    @Cached("createBinaryProfile()") ConditionProfile isNegativeInfinity,
                    @Cached("createBinaryProfile()") ConditionProfile isZero) {
        if (isZero.profile(d == 0)) {
            return "0";
        } else if (isInt.profile(JSRuntime.doubleIsRepresentableAsInt(d, true))) {
            return doInt((int) d);
        } else if (isNaN.profile(Double.isNaN(d))) {
            return JSRuntime.NAN_STRING;
        } else if (isPositiveInfinity.profile(d == Double.POSITIVE_INFINITY)) {
            return JSRuntime.INFINITY_STRING;
        } else if (isNegativeInfinity.profile(d == Double.NEGATIVE_INFINITY)) {
            return JSRuntime.NEGATIVE_INFINITY_STRING;
        } else {
            return JSRuntime.formatDtoA(d);
        }
    }
}
