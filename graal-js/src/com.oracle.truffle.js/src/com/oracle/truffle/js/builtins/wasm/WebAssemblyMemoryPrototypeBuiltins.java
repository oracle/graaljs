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
package com.oracle.truffle.js.builtins.wasm;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyMemoryPrototypeBuiltinsFactory.WebAssemblyMemoryGrowNodeGen;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyIndexOrSizeNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemory;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemoryObject;

public class WebAssemblyMemoryPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WebAssemblyMemoryPrototypeBuiltins.WebAssemblyMemoryPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new WebAssemblyMemoryPrototypeBuiltins();

    protected WebAssemblyMemoryPrototypeBuiltins() {
        super(JSWebAssemblyMemory.PROTOTYPE_NAME, WebAssemblyMemoryPrototype.class);
    }

    public enum WebAssemblyMemoryPrototype implements BuiltinEnum<WebAssemblyMemoryPrototype> {
        grow(1);

        private final int length;

        WebAssemblyMemoryPrototype(int length) {
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
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, WebAssemblyMemoryPrototype builtinEnum) {
        switch (builtinEnum) {
            case grow:
                return WebAssemblyMemoryGrowNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class WebAssemblyMemoryGrowNode extends JSBuiltinNode {
        @Child ToWebAssemblyIndexOrSizeNode toDeltaNode;
        private final BranchProfile errorBranch = BranchProfile.create();
        @Child InteropLibrary memGrowLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);

        public WebAssemblyMemoryGrowNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.toDeltaNode = ToWebAssemblyIndexOrSizeNode.create("WebAssembly.Memory.grow(): Argument 0");
        }

        @Specialization
        protected Object grow(Object thiz, Object delta) {
            if (!JSWebAssemblyMemory.isJSWebAssemblyMemory(thiz)) {
                errorBranch.enter();
                throw Errors.createTypeError("WebAssembly.Memory.grow(): Receiver is not a WebAssembly.Memory");
            }
            JSWebAssemblyMemoryObject memory = (JSWebAssemblyMemoryObject) thiz;
            JSRealm realm = getRealm();
            int deltaInt = toDeltaNode.executeInt(delta);
            Object wasmMemory = memory.getWASMMemory();
            try {
                Object growFn = realm.getWASMMemGrow();
                return memGrowLib.execute(growFn, wasmMemory, deltaInt);
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            } catch (Throwable throwable) {
                errorBranch.enter();
                if (TryCatchNode.shouldCatch(throwable)) {
                    throw Errors.createRangeError(throwable, this);
                } else {
                    throw throwable;
                }
            }
        }

    }

}
