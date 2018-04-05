/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
