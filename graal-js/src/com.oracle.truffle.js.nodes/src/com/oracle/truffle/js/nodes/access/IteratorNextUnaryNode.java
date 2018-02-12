/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * IteratorNext(iterator) unary expression.
 */
public class IteratorNextUnaryNode extends JavaScriptNode {
    @Child private PropertyGetNode getNextNode;
    @Child private JSFunctionCallNode methodCallNode;
    @Child private IsObjectNode isObjectNode;
    @Child private JavaScriptNode iteratorNode;

    protected IteratorNextUnaryNode(JSContext context, JavaScriptNode iteratorNode) {
        this.iteratorNode = iteratorNode;
        this.getNextNode = PropertyGetNode.create(JSRuntime.NEXT, false, context);
        this.methodCallNode = JSFunctionCallNode.createCall();
        this.isObjectNode = IsObjectNode.create();
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode iteratorNode) {
        return new IteratorNextUnaryNode(context, iteratorNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object iterator = iteratorNode.execute(frame);
        return execute(iterator);
    }

    public Object execute(Object iterator) {
        Object next = getNextNode.getValue(iterator);
        Object nextResult = methodCallNode.executeCall(JSArguments.createZeroArg(iterator, next));
        if (!isObjectNode.executeBoolean(nextResult)) {
            throw iteratorResultNotObject(nextResult);
        }
        return nextResult;
    }

    @TruffleBoundary
    private JSException iteratorResultNotObject(Object value) {
        return Errors.createTypeError("Iterator result " + JSRuntime.safeToString(value) + " is not an object", this);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(getNextNode.getContext(), cloneUninitialized(iteratorNode));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == TruffleObject.class;
    }
}
