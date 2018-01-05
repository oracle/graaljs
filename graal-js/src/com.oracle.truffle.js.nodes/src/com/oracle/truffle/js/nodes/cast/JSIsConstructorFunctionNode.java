/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

/**
 * This node can be used to create a callable 'constructor' function for self-hosted internal
 * JavaScript builtins.
 */
@NodeChild(value = "function")
public abstract class JSIsConstructorFunctionNode extends JavaScriptNode {

    public static JSIsConstructorFunctionNode create(JavaScriptNode argument) {
        return JSIsConstructorFunctionNodeGen.create(argument);
    }

    /**
     * Checks whether this {@link JSFunction} can be called as constructor using 'new'.
     *
     * @param function The function to be checked.
     * @return True, if the function is a constructor.
     */
    @Specialization(guards = "isJSFunction(function)")
    protected boolean doFunction(DynamicObject function) {
        return JSFunction.isConstructor(function);
    }

    @Specialization(guards = "!isJSFunction(function)")
    protected boolean doNonFunction(@SuppressWarnings("unused") Object function) {
        return false;
    }

    abstract JavaScriptNode getFunction();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSIsConstructorFunctionNodeGen.create(cloneUninitialized(getFunction()));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }
}
