/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * ES6 7.4.2 IteratorNext(iterator, value).
 */
public class IteratorNextNode extends JavaScriptBaseNode {
    @Child private PropertyGetNode getNextNode;
    @Child private JSFunctionCallNode methodCallNode;
    @Child private IsObjectNode isObjectNode;

    protected IteratorNextNode(JSContext context) {
        this.getNextNode = PropertyGetNode.create(JSRuntime.NEXT, false, context);
        this.methodCallNode = JSFunctionCallNode.createCall();
        this.isObjectNode = IsObjectNode.create();
    }

    public static IteratorNextNode create(JSContext context) {
        return new IteratorNextNode(context);
    }

    public DynamicObject execute(DynamicObject iterator, Object value) {
        Object next = getNextNode.getValue(iterator);
        Object result = methodCallNode.executeCall(JSArguments.createOneArg(iterator, next, value));
        if (!isObjectNode.executeBoolean(result)) {
            throw iteratorResultNotObject(result);
        }
        return (DynamicObject) result;
    }

    public DynamicObject execute(DynamicObject iterator) {
        Object next = getNextNode.getValue(iterator);
        Object result = methodCallNode.executeCall(JSArguments.createZeroArg(iterator, next));
        if (!isObjectNode.executeBoolean(result)) {
            throw iteratorResultNotObject(result);
        }
        return (DynamicObject) result;
    }

    @TruffleBoundary
    private JSException iteratorResultNotObject(Object value) {
        return Errors.createTypeError("Iterator result " + JSRuntime.safeToString(value) + " is not an object", this);
    }
}
