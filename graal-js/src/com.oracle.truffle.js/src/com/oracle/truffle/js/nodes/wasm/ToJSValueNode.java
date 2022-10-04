/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
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
public abstract class ToJSValueNode extends JavaScriptBaseNode {
    @Child InteropLibrary isFuncLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
    @Child InteropLibrary funcTypeLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);

    protected ToJSValueNode() {
    }

    public static ToJSValueNode create() {
        return ToJSValueNodeGen.create();
    }

    public static ToJSValueNode getUncached() {
        return ToJSValueNode.Uncached.INSTANCE;
    }

    public abstract Object execute(Object value);

    @Specialization
    public Object convert(Object value) {
        if (value instanceof Integer) {
            return value;
        } else if (value instanceof Long) {
            return BigInt.valueOf((long) value);
        } else if (value instanceof Float) {
            return (double) (float) value;
        } else if (value instanceof Double) {
            return value;
        } else {
            JSRealm realm = getRealm();
            final Object refNull = realm.getWasmRefNull();
            if (value == refNull) {
                return Null.instance;
            } else {
                Object isFuncFn = realm.getWASMIsFunc();
                try {
                    if ((Boolean) isFuncLib.execute(isFuncFn, value)) {
                        Object funcTypeFn = realm.getWASMFuncType();
                        TruffleString funcType = asTString(funcTypeLib.execute(funcTypeFn, value));
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

    static class Uncached extends ToJSValueNode {
        static final Uncached INSTANCE = new Uncached();

        Uncached() {
        }

        @Override
        public Object execute(Object value) {
            if (value instanceof Integer) {
                return value;
            } else if (value instanceof Long) {
                return BigInt.valueOf((long) value);
            } else if (value instanceof Float) {
                return (double) (float) value;
            } else if (value instanceof Double) {
                return value;
            } else {
                JSRealm realm = getRealm();
                final Object refNull = realm.getWasmRefNull();
                if (value == refNull) {
                    return Null.instance;
                } else {
                    Object isFuncFn = realm.getWASMIsFunc();
                    try {
                        if ((Boolean) InteropLibrary.getUncached().execute(isFuncFn, value)) {
                            Object funcTypeFn = realm.getWASMFuncType();
                            TruffleString funcType = ToJSValueNode.asTString(InteropLibrary.getUncached().execute(funcTypeFn, value));
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
    }

    private static TruffleString asTString(Object string) throws UnsupportedMessageException {
        if (string instanceof String) {
            return Strings.fromJavaString((String) string);
        }
        return InteropLibrary.getUncached(string).asTruffleString(string);
    }
}
