/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.js.nodes.*;
import com.oracle.truffle.js.runtime.*;
import com.oracle.truffle.js.runtime.builtins.*;
import com.oracle.truffle.js.runtime.objects.*;

/**
 * This node can be used to add a {@link JSFunction} to the queue of pending tasks in a given
 * {@link JSContext}.
 */
@NodeChild(value = "function")
public abstract class JSEnqueueJobNode extends JavaScriptNode {

    private final JSContext context;

    public JSEnqueueJobNode(JSContext context) {
        this.context = context;
    }

    public static JSEnqueueJobNode create(JSContext context, JavaScriptNode argument) {
        return JSEnqueueJobNodeGen.create(context, argument);
    }

    @TruffleBoundary
    @Specialization(guards = {"isJSFunction(function)"})
    protected Object doOther(Object function) {
        context.promiseEnqueueJob((DynamicObject) function);
        return Undefined.instance;
    }

    abstract JavaScriptNode getFunction();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSEnqueueJobNodeGen.create(context, cloneUninitialized(getFunction()));
    }
}
