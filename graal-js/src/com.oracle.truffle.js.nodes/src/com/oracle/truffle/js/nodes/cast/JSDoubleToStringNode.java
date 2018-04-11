/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
