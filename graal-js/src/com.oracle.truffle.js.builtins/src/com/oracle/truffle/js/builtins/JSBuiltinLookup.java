/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.builtins.Builtin;
import com.oracle.truffle.js.runtime.builtins.JSFunctionLookup;

public abstract class JSBuiltinLookup implements JSFunctionLookup {

    private final Map<String, JSBuiltinsContainer> containers = new HashMap<>();

    public final void defineBuiltins(JSBuiltinsContainer container) {
        assert container.getName() != null;
        defineBuiltins(container.getName(), container);
    }

    public final void defineBuiltins(String containerName, JSBuiltinsContainer container) {
        JSBuiltinsContainer currentContainer = containers.get(containerName);
        if (currentContainer == null) {
            containers.put(containerName, container);
        } else {
            currentContainer.putAll(container);
        }
    }

    @Override
    public Builtin lookupBuiltinFunction(String containerName, String methodName) {
        return lookupBuiltin(containerName, methodName);
    }

    @Override
    public void iterateBuiltinFunctions(String containerName, Consumer<Builtin> consumer) {
        JSBuiltinsContainer container = containers.get(containerName);
        if (container != null) {
            container.forEachBuiltin(consumer);
        }
    }

    private JSBuiltin lookupBuiltin(String containerName, String name) {
        JSBuiltinsContainer builtins = containers.get(containerName);
        if (builtins == null) {
            return null;
        }
        return builtins.lookupByName(name);
    }
}
