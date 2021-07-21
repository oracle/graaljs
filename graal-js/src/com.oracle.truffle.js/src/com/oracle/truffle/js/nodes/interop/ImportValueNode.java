/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.interop.InteropFunction;

/**
 * This node prepares the import of a value from Interop. It transforms values allowed in Truffle,
 * but not supported in Graal.js (e.g. {@link Long}).
 *
 * @see JSRuntime#importValue(Object)
 */
@GenerateUncached
public abstract class ImportValueNode extends JavaScriptBaseNode {
    public abstract Object executeWithTarget(Object target);

    public static ImportValueNode create() {
        return ImportValueNodeGen.create();
    }

    public static ImportValueNode getUncached() {
        return ImportValueNodeGen.getUncached();
    }

    @Specialization
    static int fromInt(int value) {
        return value;
    }

    @Specialization
    static String fromString(String value) {
        return value;
    }

    @Specialization
    static boolean fromBoolean(boolean value) {
        return value;
    }

    @Specialization
    static BigInt fromBigInt(BigInt value) {
        return value;
    }

    @Specialization(guards = "isLongRepresentableAsInt32(value)")
    static int fromLongToInt(long value) {
        return (int) value;
    }

    @Specialization(guards = "!isLongRepresentableAsInt32(value)")
    static long fromLong(long value) {
        return value;
    }

    @Specialization
    static double fromDouble(double value) {
        return value;
    }

    @Specialization
    static int fromNumber(byte value) {
        return value;
    }

    @Specialization
    static int fromNumber(short value) {
        return value;
    }

    @Specialization
    static double fromNumber(float value) {
        return value;
    }

    @Specialization
    static String fromChar(char value) {
        return String.valueOf(value);
    }

    @Specialization
    static Object fromDynamicObject(DynamicObject value) {
        return value;
    }

    @Specialization
    static Object fromInteropFunction(InteropFunction value) {
        return value.getFunction();
    }

    @Specialization
    static Object fromJSException(GraalJSException value) {
        return value.getErrorObjectEager();
    }

    @Specialization(guards = {"!isSpecial(value)"})
    static Object fromTruffleObject(TruffleObject value) {
        return value;
    }

    static boolean isSpecial(Object value) {
        return value instanceof InteropFunction || value instanceof GraalJSException;
    }

    @Fallback
    static Object fallbackCase(Object value) {
        throw Errors.createTypeErrorUnsupportedInteropType(value);
    }
}
