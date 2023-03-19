/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.trufflenode.node;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

@ImportStatic({JSRuntime.class})
abstract class FlattenNode extends JavaScriptBaseNode {
    protected abstract Object execute(Object value);

    @Specialization
    protected static String doString(@SuppressWarnings("unused") String value) {
        throw CompilerDirectives.shouldNotReachHere();
    }

    @Specialization
    protected static TruffleString doTruffleString(TruffleString value,
                    @Cached TruffleString.MaterializeNode materializeNode) {
        return Strings.flatten(materializeNode, value);
    }

    @Specialization
    protected static Object doSymbol(Symbol value) {
        return value;
    }

    @Specialization
    protected static Object doBigInt(BigInt value) {
        return value;
    }

    @Specialization
    protected static boolean doBoolean(boolean value) {
        return value;
    }

    @Specialization
    protected static int doInt(int value) {
        return value;
    }

    @Specialization
    protected static SafeInteger doSafeInteger(SafeInteger value) {
        return value;
    }

    @Specialization
    protected static long doLong(long value) {
        return value;
    }

    @Specialization
    protected static double doDouble(double value) {
        return value;
    }

    @Specialization
    protected static Object doJSObject(JSDynamicObject value) {
        return value;
    }

    @Specialization(guards = "isForeignObject(value)", limit = "3")
    protected static Object doForeignObject(TruffleObject value,
                    @CachedLibrary("value") InteropLibrary interop,
                    @Cached TruffleString.SwitchEncodingNode switchEncoding) {
        if (interop.isString(value)) {
            // jobject reference in the native wrapper (GraalString)
            // must be jstring (to allow the usage of various String-specific
            // JNI functions) => return the unboxed string
            return Strings.interopAsTruffleString(value, interop, switchEncoding);
        }
        return value;
    }

    @Specialization(guards = "!isTruffleObject(value)")
    protected static Object doOther(Object value) {
        return value;
    }

}
