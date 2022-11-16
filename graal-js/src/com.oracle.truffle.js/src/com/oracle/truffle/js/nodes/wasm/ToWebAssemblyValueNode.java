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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssembly;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyValueTypes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Implementation of ToWebAssemblyValue() operation. See
 * <a href="https://www.w3.org/TR/wasm-js-api/#towebassemblyvalue">Wasm JS-API Spec</a>
 */
public abstract class ToWebAssemblyValueNode extends JavaScriptBaseNode {

    private final BranchProfile errorBranch = BranchProfile.create();
    @Child JSToInt32Node toInt32Node;
    @Child JSToNumberNode toNumberNode;
    @Child JSToBigIntNode toBigIntNode;

    protected ToWebAssemblyValueNode() {
        this.toNumberNode = JSToNumberNode.create();
        this.toInt32Node = JSToInt32Node.create();
        this.toBigIntNode = JSToBigIntNode.create();
    }

    public static ToWebAssemblyValueNode create() {
        return ToWebAssemblyValueNodeGen.create();
    }

    public static ToWebAssemblyValueNode getUncached() {
        return ToWebAssemblyValueNode.Uncached.INSTANCE;
    }

    public abstract Object execute(Object value, TruffleString type);

    @Specialization
    protected Object convert(Object value, TruffleString type) {
        assert getLanguage().getJSContext().getContextOptions().isWasmBigInt() || !JSWebAssemblyValueTypes.isI64(type);
        if (JSWebAssemblyValueTypes.isI32(type)) {
            return toInt32Node.executeInt(value);
        } else if (JSWebAssemblyValueTypes.isI64(type)) {
            return toBigIntNode.executeBigInteger(value).longValue();
        } else if (JSWebAssemblyValueTypes.isF32(type)) {
            Number numberValue = toNumberNode.executeNumber(value);
            double doubleValue = JSRuntime.toDouble(numberValue);
            return (float) doubleValue;
        } else if (JSWebAssemblyValueTypes.isF64(type)) {
            Number numberValue = toNumberNode.executeNumber(value);
            return JSRuntime.toDouble(numberValue);
        } else if (JSWebAssemblyValueTypes.isReferenceType(type)) {
            if (value == Null.instance) {
                return getRealm().getWasmRefNull();
            } else if (JSWebAssemblyValueTypes.isAnyfunc(type)) {
                if (JSWebAssembly.isExportedFunction(value)) {
                    return JSWebAssembly.getExportedFunction((JSDynamicObject) value);
                }
                errorBranch.enter();
                notAnExportedFunctionError();
            } else if (JSWebAssemblyValueTypes.isExtenref(type)) {
                return value;
            }
        }
        throw Errors.shouldNotReachHere();
    }

    @CompilerDirectives.TruffleBoundary
    private static void notAnExportedFunctionError() {
        throw Errors.createTypeError("value is not an exported function");
    }

    static class Uncached extends ToWebAssemblyValueNode {
        static final Uncached INSTANCE = new Uncached();

        Uncached() {
        }

        @Override
        public Object execute(Object value, TruffleString type) {
            assert getLanguage().getJSContext().getContextOptions().isWasmBigInt() || !JSWebAssemblyValueTypes.isI64(type);
            if (JSWebAssemblyValueTypes.isI32(type)) {
                return JSRuntime.toInt32(value);
            } else if (JSWebAssemblyValueTypes.isI64(type)) {
                return JSRuntime.toBigInt(value).longValue();
            } else if (JSWebAssemblyValueTypes.isF32(type)) {
                double doubleValue = JSRuntime.toDouble(value);
                return (float) doubleValue;
            } else if (JSWebAssemblyValueTypes.isF64(type)) {
                return JSRuntime.toDouble(value);
            } else if (JSWebAssemblyValueTypes.isReferenceType(type)) {
                if (value == Null.instance) {
                    return getRealm().getWasmRefNull();
                } else if (JSWebAssemblyValueTypes.isAnyfunc(type)) {
                    if (JSWebAssembly.isExportedFunction(value)) {
                        return JSWebAssembly.getExportedFunction((JSDynamicObject) value);
                    }
                    ToWebAssemblyValueNode.notAnExportedFunctionError();
                } else if (JSWebAssemblyValueTypes.isExtenref(type)) {
                    return value;
                }
            }
            throw Errors.shouldNotReachHere();
        }
    }
}
