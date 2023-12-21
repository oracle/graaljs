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

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IterableToListNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.wasm.ToJSValueNode;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.interop.InteropArray;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * WebAssembly host function (imported JS function).
 */
@ExportLibrary(InteropLibrary.class)
public class WebAssemblyHostFunction implements TruffleObject {
    private final Object fn;
    private final TruffleString[] resultTypes;
    private final boolean anyReturnTypeIsI64;
    private final boolean anyArgTypeIsI64;
    private final boolean anyReturnTypeIsV128;
    private final boolean anyArgTypeIsV128;

    public WebAssemblyHostFunction(JSContext context, Object fn, TruffleString typeInfo) {
        assert JSRuntime.isCallable(fn);

        this.fn = fn;

        int idxOpen = Strings.indexOf(typeInfo, '(');
        int idxClose = Strings.indexOf(typeInfo, ')');

        TruffleString returnTypes = Strings.lazySubstring(typeInfo, idxClose + 1);
        this.resultTypes = !Strings.isEmpty(returnTypes) ? Strings.split(context, returnTypes, Strings.SPACE) : new TruffleString[0];
        this.anyReturnTypeIsI64 = Strings.indexOf(typeInfo, JSWebAssemblyValueTypes.I64, idxClose + 1) >= 0;
        this.anyArgTypeIsI64 = Strings.indexOf(typeInfo, JSWebAssemblyValueTypes.I64, idxOpen + 1, idxClose) >= 0;
        this.anyReturnTypeIsV128 = Strings.indexOf(typeInfo, JSWebAssemblyValueTypes.V128, idxClose + 1) >= 0;
        this.anyArgTypeIsV128 = Strings.indexOf(typeInfo, JSWebAssemblyValueTypes.V128, idxOpen + 1, idxClose) >= 0;
    }

    @ExportMessage
    public static final boolean isExecutable(@SuppressWarnings("unused") WebAssemblyHostFunction receiver) {
        return true;
    }

    @ExportMessage
    public final Object execute(Object[] args,
                    @Bind("$node") Node node,
                    @Cached ToWebAssemblyValueNode toWebAssemblyValueNode,
                    @Cached ToJSValueNode toJSValueNode,
                    @Cached(value = "createCall()", uncached = "getUncachedCall()") JSFunctionCallNode callNode,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached(inline = true) GetIteratorNode getIteratorNode,
                    @Cached IterableToListNode iterableToListNode,
                    @CachedLibrary("this") InteropLibrary self) {
        JSContext context = JavaScriptLanguage.get(self).getJSContext();
        if ((!context.getLanguageOptions().wasmBigInt() && (anyReturnTypeIsI64 || anyArgTypeIsI64)) || anyReturnTypeIsV128 || anyArgTypeIsV128) {
            errorBranch.enter(node);
            throw Errors.createTypeError("wasm function signature contains illegal type");
        }
        Object[] jsArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            jsArgs[i] = toJSValueNode.execute(args[i]);
        }

        Object result = callNode.executeCall(JSArguments.create(Undefined.instance, fn, jsArgs));

        if (resultTypes.length == 0) {
            return Undefined.instance;
        } else if (resultTypes.length == 1) {
            return toWebAssemblyValueNode.execute(result, resultTypes[0]);
        } else {
            IteratorRecord iterator = getIteratorNode.execute(node, result);
            SimpleArrayList<Object> values = iterableToListNode.execute(iterator);

            if (resultTypes.length != values.size()) {
                errorBranch.enter(node);
                throw Errors.createTypeError("invalid result array arity");
            }
            Object[] wasmValues = new Object[values.size()];
            for (int i = 0; i < values.size(); i++) {
                wasmValues[i] = toWebAssemblyValueNode.execute(values.get(i), resultTypes[i]);
            }
            return InteropArray.create(wasmValues);
        }
    }
}
