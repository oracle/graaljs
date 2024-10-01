/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssembly;
import com.oracle.truffle.js.runtime.builtins.wasm.WebAssemblyValueType;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Implementation of ToWebAssemblyValue() operation. See
 * <a href="https://www.w3.org/TR/wasm-js-api/#towebassemblyvalue">Wasm JS-API Spec</a>
 */
@ImportStatic(WebAssemblyValueType.class)
@GenerateUncached
public abstract class ToWebAssemblyValueNode extends JavaScriptBaseNode {

    protected ToWebAssemblyValueNode() {
    }

    public abstract Object execute(Object value, WebAssemblyValueType type);

    @Specialization(guards = "type == i32")
    static int i32(Object value, @SuppressWarnings("unused") WebAssemblyValueType type,
                    @Cached JSToInt32Node toInt32Node) {
        return toInt32Node.executeInt(value);
    }

    @Specialization(guards = "type == i64")
    static long i64(Object value, @SuppressWarnings("unused") WebAssemblyValueType type,
                    @Cached JSToBigIntNode toBigIntNode) {
        return toBigIntNode.executeBigInteger(value).longValue();
    }

    @Specialization(guards = "type == f32")
    static float f32(Object value, @SuppressWarnings("unused") WebAssemblyValueType type,
                    @Cached @Shared("toNumber") JSToNumberNode toNumberNode) {
        Number numberValue = toNumberNode.executeNumber(value);
        double doubleValue = JSRuntime.toDouble(numberValue);
        return (float) doubleValue;
    }

    @Specialization(guards = "type == f64")
    static double f64(Object value, @SuppressWarnings("unused") WebAssemblyValueType type,
                    @Cached @Shared("toNumber") JSToNumberNode toNumberNode) {
        Number numberValue = toNumberNode.executeNumber(value);
        return JSRuntime.toDouble(numberValue);
    }

    @Specialization(guards = "type == anyfunc")
    final Object anyfunc(Object value, @SuppressWarnings("unused") WebAssemblyValueType type,
                    @Cached InlinedBranchProfile errorBranch) {
        if (value == Null.instance) {
            return getRealm().getWasmRefNull();
        } else {
            if (JSWebAssembly.isExportedFunction(value)) {
                return JSWebAssembly.getExportedFunction((JSDynamicObject) value);
            }
            errorBranch.enter(this);
            throw notAnExportedFunctionError();
        }
    }

    @TruffleBoundary
    private static JSException notAnExportedFunctionError() {
        throw Errors.createTypeError("value is not an exported function");
    }

    @Specialization(guards = "type == externref")
    final Object externref(Object value, @SuppressWarnings("unused") WebAssemblyValueType type) {
        if (value == Null.instance) {
            return getRealm().getWasmRefNull();
        } else {
            return value;
        }
    }

    @Fallback
    @TruffleBoundary
    final Object fallback(@SuppressWarnings("unused") Object value, WebAssemblyValueType type) {
        throw Errors.createTypeError("Unknown type: " + type, this);
    }
}
