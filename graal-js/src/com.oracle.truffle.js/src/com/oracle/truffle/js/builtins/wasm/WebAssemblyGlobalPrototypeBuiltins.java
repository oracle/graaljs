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
package com.oracle.truffle.js.builtins.wasm;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyGlobalPrototypeBuiltinsFactory.WebAssemblyGlobalGetValueNodeGen;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyGlobalPrototypeBuiltinsFactory.WebAssemblyGlobalSetValueNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.wasm.ToJSValueNode;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyGlobal;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyGlobalObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyValueTypes;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class WebAssemblyGlobalPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WebAssemblyGlobalPrototypeBuiltins.WebAssemblyGlobalPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new WebAssemblyGlobalPrototypeBuiltins();

    protected WebAssemblyGlobalPrototypeBuiltins() {
        super(JSWebAssemblyGlobal.PROTOTYPE_NAME, WebAssemblyGlobalPrototype.class);
    }

    public enum WebAssemblyGlobalPrototype implements BuiltinEnum<WebAssemblyGlobalPrototype> {
        valueOf(0),

        value(0) {
            @Override
            public boolean isGetter() {
                return true;
            }
        },
        set_value(1) {
            @Override
            public Object getKey() {
                return value.getKey();
            }

            @Override
            public boolean isSetter() {
                return true;
            }
        };

        private final int length;

        WebAssemblyGlobalPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isEnumerable() {
            return true;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, WebAssemblyGlobalPrototype builtinEnum) {
        switch (builtinEnum) {
            case valueOf:
            case value:
                return WebAssemblyGlobalGetValueNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case set_value:
                return WebAssemblyGlobalSetValueNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic(JSConfig.class)
    public abstract static class WebAssemblyGlobalGetValueNode extends JSBuiltinNode {

        protected WebAssemblyGlobalGetValueNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object getValue(JSWebAssemblyGlobalObject object,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached ToJSValueNode toJSValueNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary globalReadLib) {
            if (JSWebAssemblyValueTypes.isV128(object.getValueType())) {
                errorBranch.enter(this);
                throw Errors.createTypeError(getBuiltin().getFullName() + ": cannot read value type v128", this);
            }
            Object wasmGlobal = object.getWASMGlobal();
            Object globalRead = getRealm().getWASMGlobalRead();
            try {
                return toJSValueNode.execute(globalReadLib.execute(globalRead, wasmGlobal));
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
        }

        @TruffleBoundary
        @Fallback
        protected Object doIncompatibleReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError(getBuiltin().getFullName() + ": Receiver is not a WebAssembly.Global", this);
        }
    }

    @ImportStatic(JSConfig.class)
    public abstract static class WebAssemblyGlobalSetValueNode extends JSBuiltinNode {

        protected WebAssemblyGlobalSetValueNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object setValue(JSWebAssemblyGlobalObject global, Object[] args,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached ToWebAssemblyValueNode toWebAssemblyValueNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary globalWriteLib) {
            if (!global.isMutable()) {
                errorBranch.enter(this);
                throw Errors.createTypeError("set WebAssembly.Global.value: Can't set the value of an immutable global");
            }
            if (JSWebAssemblyValueTypes.isV128(global.getValueType())) {
                errorBranch.enter(this);
                throw Errors.createTypeError("set WebAssembly.Global.value: cannot write value type v128", this);
            }
            Object wasmGlobal = global.getWASMGlobal();
            try {
                Object value;
                if (args.length == 0) {
                    errorBranch.enter(this);
                    throw Errors.createTypeError("set WebAssembly.Global.value: Argument 0 is required");
                } else {
                    value = args[0];
                }
                Object webAssemblyValue = toWebAssemblyValueNode.execute(value, global.getValueType());
                Object globalWrite = getRealm().getWASMGlobalWrite();
                globalWriteLib.execute(globalWrite, wasmGlobal, webAssemblyValue);
                return Undefined.instance;
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            } catch (AbstractTruffleException ex) {
                errorBranch.enter(this);
                throw Errors.createTypeError(ex, this);
            }
        }

        @TruffleBoundary
        @Fallback
        protected Object doIncompatibleReceiver(@SuppressWarnings("unused") Object thisObj, @SuppressWarnings("unused") Object args) {
            throw Errors.createTypeError("set WebAssembly.Global.value: Receiver is not a WebAssembly.Global", this);
        }
    }
}
