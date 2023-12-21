/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.js.runtime.builtins.wasm;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Represents the value types used in WebAssembly and provides some methods to check their string
 * representations.
 */
public final class JSWebAssemblyValueTypes {
    public static final TruffleString I32 = Strings.I_32;
    public static final TruffleString I64 = Strings.I_64;
    public static final TruffleString F32 = Strings.F_32;
    public static final TruffleString F64 = Strings.F_64;
    public static final TruffleString V128 = Strings.V_128;
    public static final TruffleString ANYFUNC = Strings.ANYFUNC;
    public static final TruffleString EXTERNREF = Strings.EXTERNREF;

    public static boolean isI32(TruffleString type) {
        return Strings.equals(I32, type);
    }

    public static boolean isI64(TruffleString type) {
        return Strings.equals(I64, type);
    }

    public static boolean isF32(TruffleString type) {
        return Strings.equals(F32, type);
    }

    public static boolean isF64(TruffleString type) {
        return Strings.equals(F64, type);
    }

    public static boolean isV128(TruffleString type) {
        return Strings.equals(V128, type);
    }

    public static boolean isAnyfunc(TruffleString type) {
        return Strings.equals(ANYFUNC, type);
    }

    public static boolean isExternref(TruffleString type) {
        return Strings.equals(EXTERNREF, type);
    }

    public static boolean isValueType(TruffleString type) {
        return isI32(type) || isI64(type) || isF32(type) || isF64(type) || isV128(type) || isAnyfunc(type) || isExternref(type);
    }

    public static boolean isReferenceType(TruffleString type) {
        return isAnyfunc(type) || isExternref(type);
    }

    public static Object getDefaultValue(JSRealm realm, TruffleString type) {
        if (isI32(type)) {
            return 0;
        } else if (isI64(type)) {
            return 0L;
        } else if (isF32(type)) {
            return 0f;
        } else if (isF64(type)) {
            return 0d;
        } else if (isAnyfunc(type)) {
            return realm.getWasmRefNull();
        } else if (isExternref(type)) {
            return Undefined.instance;
        } else {
            throw Errors.shouldNotReachHere();
        }
    }
}
