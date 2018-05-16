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
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.truffleinterop.InteropBoundFunction;

/**
 * This node prepares the import of a value from Interop. It transforms values allowed in Truffle,
 * but not supported in Graal.js (e.g. {@link Long}).
 *
 * @see JSRuntime#importValue(Object)
 */
public abstract class JSForeignToJSTypeNode extends JavaScriptBaseNode {
    public abstract Object executeWithTarget(Object target);

    public static JSForeignToJSTypeNode create() {
        return JSForeignToJSTypeNodeGen.create();
    }

    @Specialization
    public int fromInt(int value) {
        return value;
    }

    @Specialization
    public double fromDouble(double value) {
        return value;
    }

    @Specialization
    public String fromString(String value) {
        return value;
    }

    @Specialization
    public BigInt fromBigInt(BigInt value) {
        return value;
    }

    @Specialization
    public Object fromNumber(Number value,
                    @Cached("createBinaryProfile()") ConditionProfile longCase,
                    @Cached("createBinaryProfile()") ConditionProfile byteCase,
                    @Cached("createBinaryProfile()") ConditionProfile shortCase) {
        if (value instanceof Double || value instanceof Integer) {
            return value;
        } else if (byteCase.profile(value instanceof Byte)) {
            return ((Byte) value).intValue();
        } else if (shortCase.profile(value instanceof Short)) {
            return ((Short) value).intValue();
        } else if (longCase.profile(value instanceof Long)) {
            long lValue = value.longValue();
            if (JSRuntime.longIsRepresentableAsInt(lValue)) {
                return (int) lValue;
            } else if (JSRuntime.MIN_SAFE_INTEGER_LONG <= lValue && lValue <= JSRuntime.MAX_SAFE_INTEGER_LONG) {
                return LargeInteger.valueOf(lValue);
            }
            return (double) lValue;
        }
        return JSRuntime.doubleValueVirtual(value);
    }

    @Specialization
    public Object fromBoolean(boolean value) {
        return value;
    }

    @Specialization
    public Object fromChar(char value) {
        return String.valueOf(value);
    }

    @Specialization(guards = "isJavaNull(value)")
    public Object isNull(@SuppressWarnings("unused") Object value) {
        return Null.instance;
    }

    @Specialization
    public Object fromTruffleJavaObject(TruffleObject value) {
        if (value instanceof InteropBoundFunction) {
            return ((InteropBoundFunction) value).getFunction();
        } else {
            TruffleLanguage.Env env = AbstractJavaScriptLanguage.getCurrentEnv();
            if (env.isHostObject(value)) {
                Object object = env.asHostObject(value);
                if (object == null) {
                    return Null.instance;
                }
                if (JSTruffleOptions.NashornJavaInterop) {
                    if (object instanceof Class) {
                        return JavaClass.forClass((Class<?>) object);
                    } else {
                        return object;
                    }
                }
            }
        }
        return value;
    }

    @TruffleBoundary
    @Fallback
    public Object fallbackCase(Object value) {
        throw Errors.createTypeError("type " + value.getClass().getSimpleName() + " not supported in JavaScript");
    }
}
