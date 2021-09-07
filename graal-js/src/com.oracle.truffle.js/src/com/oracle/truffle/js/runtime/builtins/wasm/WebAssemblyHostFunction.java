/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.wasm.ToJSValueNode;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * WebAssembly host function (imported JS function).
 */
@ExportLibrary(InteropLibrary.class)
public class WebAssemblyHostFunction implements TruffleObject {
    private final Object fn;
    private final String returnType;
    private final boolean returnTypeIsI64;
    private final boolean anyArgTypeIsI64;

    public WebAssemblyHostFunction(Object fn, String typeInfo) {
        assert JSRuntime.isCallable(fn);

        this.fn = fn;

        int idxOpen = typeInfo.indexOf('(');
        int idxClose = typeInfo.indexOf(')');
        String argTypes = typeInfo.substring(idxOpen + 1, idxClose);

        this.returnType = typeInfo.substring(idxClose + 1);
        this.returnTypeIsI64 = JSWebAssemblyValueTypes.isI64(returnType);
        this.anyArgTypeIsI64 = argTypes.contains(JSWebAssemblyValueTypes.I64);
    }

    @ExportMessage
    public static final boolean isExecutable(@SuppressWarnings("unused") WebAssemblyHostFunction receiver) {
        return true;
    }

    @ExportMessage
    public final Object execute(Object[] args,
                    @Cached ToWebAssemblyValueNode toWebAssemblyValueNode,
                    @Cached ToJSValueNode toJSValueNode,
                    @Cached(value = "createCall()", uncached = "getUncachedCall()") JSFunctionCallNode callNode,
                    @Cached BranchProfile errorBranch,
                    @CachedLibrary("this") InteropLibrary self) {
        if (!JavaScriptLanguage.get(self).getJSContext().getContextOptions().isWasmBigInt() && (returnTypeIsI64 || anyArgTypeIsI64)) {
            errorBranch.enter();
            throw Errors.createTypeError("wasm function signature contains illegal type");
        }

        Object[] jsArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            jsArgs[i] = toJSValueNode.execute(args[i]);
        }

        Object result = callNode.executeCall(JSArguments.create(Undefined.instance, fn, jsArgs));

        if (returnType.isEmpty()) {
            return Undefined.instance;
        } else {
            return toWebAssemblyValueNode.execute(result, returnType);
        }
    }

}
