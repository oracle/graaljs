/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import java.util.function.Consumer;

public interface JSFunctionLookup {

    /**
     * Looks up a registered function implementation for a container and a method name.
     *
     * @return {@null} if no function was found.
     */
    Builtin lookupBuiltinFunction(String containerName, String methodName);

    /**
     * Iterates registered function implementations for a built-in class.
     */
    void iterateBuiltinFunctions(String containerName, Consumer<Builtin> consumer);
}
