/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyBuiltins;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSWebAssembly {

    public static final String CLASS_NAME = "WebAssembly";

    public static final HiddenKey FUNCTION_ADDRESS = new HiddenKey("FunctionAddress");

    private JSWebAssembly() {
    }

    public static DynamicObject create(JSRealm realm) {
        DynamicObject webAssembly = JSOrdinary.createInit(realm);
        JSObjectUtil.putToStringTag(webAssembly, CLASS_NAME);
        JSObjectUtil.putFunctionsFromContainer(realm, webAssembly, WebAssemblyBuiltins.BUILTINS);
        return webAssembly;
    }

    public static boolean isExportedFunction(Object function) {
        return JSDynamicObject.isJSDynamicObject(function) && JSObjectUtil.hasHiddenProperty((JSDynamicObject) function, FUNCTION_ADDRESS);
    }

    public static Object getExportedFunction(DynamicObject function) {
        assert isExportedFunction(function);
        return JSObjectUtil.getHiddenProperty(function, JSWebAssembly.FUNCTION_ADDRESS);
    }

    public static Object getEmbedderData(JSRealm realm, Object wasmEntity) {
        Object embedderDataGetter = realm.getWASMEmbedderDataGet();
        try {
            return InteropLibrary.getUncached(embedderDataGetter).execute(embedderDataGetter, wasmEntity);
        } catch (InteropException iex) {
            throw CompilerDirectives.shouldNotReachHere(iex);
        }
    }

    public static void setEmbedderData(JSRealm realm, Object wasmEntity, Object data) {
        Object embedderDataSetter = realm.getWASMEmbedderDataSet();
        try {
            InteropLibrary.getUncached(embedderDataSetter).execute(embedderDataSetter, wasmEntity, data);
        } catch (InteropException iex) {
            throw CompilerDirectives.shouldNotReachHere(iex);
        }
    }

}
