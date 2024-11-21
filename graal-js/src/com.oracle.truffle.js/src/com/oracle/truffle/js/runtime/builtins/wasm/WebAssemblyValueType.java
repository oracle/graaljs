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

import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Represents the value types used in WebAssembly.
 * 
 * See org.graalvm.wasm.api.ValueType.
 */
public enum WebAssemblyValueType {
    i32(false),
    i64(false),
    f32(false),
    f64(false),
    v128(false),
    anyfunc(true),
    externref(true);

    private final boolean reference;

    WebAssemblyValueType(boolean reference) {
        this.reference = reference;
    }

    public boolean isReference() {
        return reference;
    }

    public Object getDefaultValue(JSRealm realm) {
        return switch (this) {
            case i32 -> 0;
            case i64 -> 0L;
            case f32 -> 0f;
            case f64 -> 0d;
            case anyfunc -> realm.getWasmRefNull();
            case externref -> Undefined.instance;
            default -> throw Errors.shouldNotReachHereUnexpectedValue(this);
        };
    }

    public static WebAssemblyValueType lookupType(String type) {
        return switch (type) {
            case "i32" -> i32;
            case "i64" -> i64;
            case "f32" -> f32;
            case "f64" -> f64;
            case "v128" -> v128;
            case "anyfunc" -> anyfunc;
            case "externref" -> externref;
            default -> null;
        };
    }
}
