/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.simd;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.simd.SIMDBuiltinsFactory.CallSIMDTypeNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSSIMD;

public final class SIMDBuiltins extends JSBuiltinsContainer.SwitchEnum<SIMDBuiltins.SIMDConstructor> {
    public SIMDBuiltins() {
        super(JSSIMD.SIMD_OBJECT_NAME, SIMDConstructor.class);
    }

    public enum SIMDConstructor implements BuiltinEnum<SIMDConstructor> {
        Float32x4(0),
        Int32x4(0),
        Int16x8(0),
        Int8x16(0),
        Uint32x4(0),
        Uint16x8(0),
        Uint8x16(0),
        Bool32x4(0),
        Bool16x8(0),
        Bool8x16(0),
        SIMDTypes(0);

        private final int length;

        SIMDConstructor(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, SIMDConstructor builtinEnum) {
        switch (builtinEnum) {
            case SIMDTypes:
                return CallSIMDTypeNode.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case Float32x4:
            case Int32x4:
            case Int16x8:
            case Int8x16:
            case Uint32x4:
            case Uint16x8:
            case Uint8x16:
            case Bool32x4:
            case Bool16x8:
            case Bool8x16:
                return JSConstructSIMDTypeNode.create(context, builtin, args().varArgs().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class CallSIMDTypeNode extends JSBuiltinNode {
        public CallSIMDTypeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        public static CallSIMDTypeNode create(JSContext context, JSBuiltin builtin, JavaScriptNode[] createArgumentNodes) {
            return CallSIMDTypeNodeGen.create(context, builtin, createArgumentNodes);
        }

        @Specialization
        protected Object callSIMDType(@SuppressWarnings("unused") Object... args) {
            throw Errors.createTypeError("wrong");
        }
    }
}
