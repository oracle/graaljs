/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.wasm;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyInstance;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Implementation of ToJSValue() operation. See
 * <a href="https://www.w3.org/TR/wasm-js-api/#tojsvalue">Wasm JS-API Spec</a>
 */
@ImportStatic(JSConfig.class)
@GenerateUncached
public abstract class ToJSValueNode extends JavaScriptBaseNode {

    protected ToJSValueNode() {
    }

    public abstract Object execute(Object value);

    @Specialization
    static int i32(int value) {
        return value;
    }

    @Specialization
    static BigInt i64(long value) {
        return BigInt.valueOf(value);
    }

    @Specialization
    static double f32(float value) {
        return value;
    }

    @Specialization
    static double f64(double value) {
        return value;
    }

    @Fallback
    final Object convert(Object value,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary isFuncLib,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary funcTypeLib,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary asTStringLib,
                    @Cached TruffleString.SwitchEncodingNode switchEncoding) {
        JSRealm realm = getRealm();
        final Object refNull = realm.getWasmRefNull();
        if (value == refNull) {
            return Null.instance;
        } else {
            Object isFuncFn = realm.getWASMIsFunc();
            try {
                Object isFunc = isFuncLib.execute(isFuncFn, value);
                if (isFunc instanceof Boolean && (boolean) isFunc) {
                    Object funcTypeFn = realm.getWASMFuncType();
                    TruffleString funcType = Strings.interopAsTruffleString(funcTypeLib.execute(funcTypeFn, value), asTStringLib, switchEncoding);
                    return JSWebAssemblyInstance.exportFunction(realm.getContext(), realm, value, funcType);
                } else {
                    return value;
                }
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
        }
    }
}
