/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.simd;

import java.util.NoSuchElementException;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.JSBasicSimdOperation;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSSIMD;
import com.oracle.truffle.js.runtime.builtins.SIMDType;

public abstract class JSConstructSIMDTypeNode extends JSBasicSimdOperation {

    protected JSConstructSIMDTypeNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin, findSIMDTypeFactory(builtin.getName()).createSimdType());
    }

    public static Object create(JSContext context, JSBuiltin builtin, JavaScriptNode[] createArgumentNodes) {
        return JSConstructSIMDTypeNodeGen.create(context, builtin, createArgumentNodes);
    }

    private static SIMDType.SIMDTypeFactory<? extends SIMDType> findSIMDTypeFactory(String name) {
        for (SIMDType.SIMDTypeFactory<?> simdTypeFactory : SIMDType.FACTORIES) {
            if (simdTypeFactory.getName().equals(name)) {
                return simdTypeFactory;
            }
        }
        throw new NoSuchElementException(name);
    }

    // Implements 5.2.1 SIMDConstructor( ...values )
    @Specialization
    @ExplodeLoop
    protected Object doConstructSIMDInt32(Object[] args) {
        DynamicObject t = JSSIMD.createSIMD(getContext(), simdContext);
        for (int i = 0; i < Math.min(args.length, numberOfElements); i++) {
            Object elem = args[i];
            setLane(t, i, cast(i, elem));
        }

        for (int i = args.length; i < numberOfElements; i++) {
            setLane(t, i, cast(i, 0));
        }

        return t;
    }
}
