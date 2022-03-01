/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;

/**
 * This implements 9.8.1 ToString Applied to the Number Type.
 *
 */
public abstract class JSDoubleToStringNode extends JavaScriptBaseNode {

    public static JSDoubleToStringNode create() {
        return JSDoubleToStringNodeGen.create();
    }

    public abstract TruffleString executeString(Object operand);

    @Specialization
    protected static TruffleString doInt(int i,
                    @Cached @Shared("fromLongNode") TruffleString.FromLongNode fromLongNode) {
        return Strings.fromLong(fromLongNode, i);
    }

    @Specialization
    protected static TruffleString doLong(long i,
                    @Cached @Shared("fromLongNode") TruffleString.FromLongNode fromLongNode) {
        return Strings.fromLong(fromLongNode, i);
    }

    @Specialization
    protected static TruffleString doDouble(double d,
                    @Cached @Shared("fromLongNode") TruffleString.FromLongNode fromLongNode,
                    @Cached ConditionProfile isInt,
                    @Cached ConditionProfile isNaN,
                    @Cached ConditionProfile isPositiveInfinity,
                    @Cached ConditionProfile isNegativeInfinity,
                    @Cached ConditionProfile isZero,
                    @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
        if (isZero.profile(d == 0)) {
            return Strings.ZERO;
        } else if (isInt.profile(JSRuntime.doubleIsRepresentableAsInt(d, true))) {
            return doInt((int) d, fromLongNode);
        } else if (isNaN.profile(Double.isNaN(d))) {
            return Strings.NAN;
        } else if (isPositiveInfinity.profile(d == Double.POSITIVE_INFINITY)) {
            return Strings.INFINITY;
        } else if (isNegativeInfinity.profile(d == Double.NEGATIVE_INFINITY)) {
            return Strings.NEGATIVE_INFINITY;
        } else {
            return Strings.fromJavaString(fromJavaStringNode, JSRuntime.formatDtoA(d));
        }
    }
}
