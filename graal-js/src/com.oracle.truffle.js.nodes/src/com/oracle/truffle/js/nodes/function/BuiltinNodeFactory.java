/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import java.util.Objects;

import com.oracle.truffle.js.runtime.JSContext;

@FunctionalInterface
public interface BuiltinNodeFactory {
    default JSBuiltinNode createNode(JSContext context, JSBuiltin builtin) {
        return (JSBuiltinNode) Objects.requireNonNull(createObject(context, builtin));
    }

    /**
     * Variant with erased return type to prevent the class returned by the lambda from being loaded
     * by the bytecode verifier.
     *
     * @see #createNode(JSContext, JSBuiltin)
     */
    Object createObject(JSContext context, JSBuiltin builtin);
}
