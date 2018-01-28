/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * ES6 7.4.2 IteratorNext(iterator, value).
 */
public abstract class IteratorNextNode extends JavaScriptBaseNode {
    @Child private PropertyNode getNextNode;
    @Child private JSFunctionCallNode methodCallNode;
    @Child private IsObjectNode isObjectNode;

    protected IteratorNextNode(JSContext context) {
        NodeFactory factory = NodeFactory.getInstance(context);
        this.getNextNode = factory.createProperty(context, null, JSRuntime.NEXT);
        this.methodCallNode = JSFunctionCallNode.createCall();
        this.isObjectNode = IsObjectNode.create();
    }

    public static IteratorNextNode create(JSContext context) {
        return IteratorNextNodeGen.create(context);
    }

    @Specialization
    protected DynamicObject doIteratorNext(DynamicObject iterator, Object value) {
        Object next = getNextNode.executeWithTarget(iterator);
        Object result = methodCallNode.executeCall(JSArguments.createOneArg(iterator, next, value));
        if (!isObjectNode.executeBoolean(result)) {
            throw iteratorResultNotObject(result);
        }
        return (DynamicObject) result;
    }

    @TruffleBoundary
    private JSException iteratorResultNotObject(Object value) {
        return Errors.createTypeError("Iterator result " + JSRuntime.safeToString(value) + " is not an object", this);
    }

    public abstract DynamicObject execute(DynamicObject iterator, Object value);
}
