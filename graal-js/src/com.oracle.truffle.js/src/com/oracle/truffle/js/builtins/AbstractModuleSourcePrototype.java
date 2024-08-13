/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModule;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModuleObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains built-in methods of AsyncGenerator.prototype.
 */
public enum AbstractModuleSourcePrototype implements BuiltinEnum<AbstractModuleSourcePrototype> {

    _toStringTag(0) {
        @Override
        public Object getKey() {
            return Symbol.SYMBOL_TO_STRING_TAG;
        }

        @Override
        public boolean isGetter() {
            return true;
        }
    };

    public static final TruffleString ABSTRACT_MODULE_SOURCE_PROTOTYPE = Strings.constant("%AbstractModuleSource%.prototype");
    public static final JSBuiltinsContainer BUILTINS = JSBuiltinsContainer.fromEnum(ABSTRACT_MODULE_SOURCE_PROTOTYPE, AbstractModuleSourcePrototype.class);

    private final int length;

    AbstractModuleSourcePrototype(int length) {
        this.length = length;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
        return switch (this) {
            case _toStringTag -> AbstractModuleSourcePrototypeFactory.ToStringTagNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        };
    }

    public abstract static class ToStringTagNode extends JSBuiltinNode {

        public ToStringTagNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static Object doWasmModule(JSWebAssemblyModuleObject thisObj) {
            return JSWebAssemblyModule.WEB_ASSEMBLY_MODULE;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static Object doOther(Object thisObj) {
            return Undefined.instance;
        }
    }
}
