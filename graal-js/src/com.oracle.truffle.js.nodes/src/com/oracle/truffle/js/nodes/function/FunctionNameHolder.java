/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

public interface FunctionNameHolder {
    String getFunctionName();

    void setFunctionName(String name);

    default boolean isAnonymous() {
        return getFunctionName().isEmpty();
    }

    interface Delegate extends FunctionNameHolder {
        FunctionNameHolder getFunctionNameHolder();

        @Override
        default String getFunctionName() {
            return getFunctionNameHolder().getFunctionName();
        }

        @Override
        default void setFunctionName(String name) {
            getFunctionNameHolder().setFunctionName(name);
        }

        @Override
        default boolean isAnonymous() {
            return getFunctionNameHolder().isAnonymous();
        }
    }
}
