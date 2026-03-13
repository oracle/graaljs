/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.wasm.ToJSValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyExceptionObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyMemory;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyTagObject;
import com.oracle.truffle.js.runtime.builtins.wasm.WebAssemblyType;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class WebAssemblyExceptionPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WebAssemblyExceptionPrototypeBuiltins.WebAssemblyExceptionPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new WebAssemblyExceptionPrototypeBuiltins();

    protected WebAssemblyExceptionPrototypeBuiltins() {
        super(JSWebAssemblyMemory.PROTOTYPE_NAME, WebAssemblyExceptionPrototype.class);
    }

    public enum WebAssemblyExceptionPrototype implements BuiltinEnum<WebAssemblyExceptionPrototype> {
        is(1),
        getArg(2),
        stack(0);

        private final int length;

        WebAssemblyExceptionPrototype(int length) {
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

        @Override
        public boolean isGetter() {
            return this == stack;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, WebAssemblyExceptionPrototype builtinEnum) {
        return switch (builtinEnum) {
            case is -> WebAssemblyExceptionPrototypeBuiltinsFactory.WebAssemblyExceptionIsNodeGen.create(context, builtin,
                            args().withThis().fixedArgs(1).createArgumentNodes(context));
            case getArg -> WebAssemblyExceptionPrototypeBuiltinsFactory.WebAssemblyExceptionGetArgNodeGen.create(context, builtin,
                            args().withThis().fixedArgs(2).createArgumentNodes(context));
            case stack -> WebAssemblyExceptionPrototypeBuiltinsFactory.WebAssemblyExceptionStackNodeGen.create(context, builtin,
                            args().withThis().createArgumentNodes(context));
        };
    }

    public abstract static class WebAssemblyExceptionIsNode extends JSBuiltinNode {

        public WebAssemblyExceptionIsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        static Object doException(JSWebAssemblyExceptionObject exn, JSWebAssemblyTagObject tag) {
            return exn.type() == tag;
        }

        @TruffleBoundary
        @Fallback
        final Object doIncompatibleReceiver(Object thisObj, Object tag) {
            if (!(thisObj instanceof JSWebAssemblyExceptionObject)) {
                throw Errors.createTypeError("WebAssembly.Exception.is(): Receiver is not a WebAssembly.Exception", this);
            } else {
                assert !(tag instanceof JSWebAssemblyTagObject);
                throw Errors.createTypeError("WebAssembly.Exception.is(): Argument 0 must be a WebAssembly.Tag", this);
            }
        }
    }

    public abstract static class WebAssemblyExceptionGetArgNode extends JSBuiltinNode {

        public WebAssemblyExceptionGetArgNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        final Object doException(JSWebAssemblyExceptionObject exn, JSWebAssemblyTagObject tag, Object index,
                        @Cached JSToIndexNode toIndexNode,
                        @Cached ToJSValueNode toJSValueNode,
                        @Cached InlinedBranchProfile errorBranch) {
            if (tag != exn.type()) {
                errorBranch.enter(this);
                throw Errors.createTypeError("WebAssembly.Exception.getArg(): First argument does not match the exception tag", this);
            }
            if (index == Undefined.instance) {
                errorBranch.enter(this);
                throw Errors.createTypeError("WebAssembly.Exception.getArg(): Index must be convertible to a valid number", this);
            }
            long indexAsLong = toIndexNode.executeLong(index);
            if (Long.compareUnsigned(indexAsLong, exn.payload().length) >= 0) {
                errorBranch.enter(this);
                throw Errors.createRangeError("WebAssembly.Exception.getArg(): Index out of range", this);
            }
            int i = (int) indexAsLong;
            WebAssemblyType type = exn.type().parameterTypes()[i];
            if (type == WebAssemblyType.v128) {
                errorBranch.enter(this);
                throw Errors.createTypeError("WebAssembly.Exception.getArg(): Invalid type v128", this);
            } else if (type == WebAssemblyType.exnref) {
                errorBranch.enter(this);
                throw Errors.createTypeError("WebAssembly.Exception.getArg(): Invalid type exnref", this);
            }
            return toJSValueNode.execute(exn.payload()[i]);
        }

        @TruffleBoundary
        @Fallback
        final Object doIncompatibleReceiver(Object thisObj, Object tag, @SuppressWarnings("unused") Object index) {
            if (!(thisObj instanceof JSWebAssemblyExceptionObject)) {
                throw Errors.createTypeError("WebAssembly.Exception.getArg(): Receiver is not a WebAssembly.Exception", this);
            } else {
                assert !(tag instanceof JSWebAssemblyTagObject);
                throw Errors.createTypeError("WebAssembly.Exception.getArg(): Argument 0 must be a WebAssembly.Tag", this);
            }
        }
    }

    public abstract static class WebAssemblyExceptionStackNode extends JSBuiltinNode {

        public WebAssemblyExceptionStackNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        static Object doException(JSWebAssemblyExceptionObject exn) {
            return JSRuntime.nullToUndefined(exn.getStack());
        }

        @TruffleBoundary
        @Fallback
        final Object doIncompatibleReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("WebAssembly.Exception.stack: Receiver is not a WebAssembly.Exception", this);
        }
    }
}
