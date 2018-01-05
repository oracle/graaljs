/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * ES6 7.4.6 IteratorClose(iterator, completion).
 *
 * The completion part must be handled in caller.
 */
public class IteratorCloseNode extends JavaScriptNode {
    @Child private GetMethodNode getReturnNode;
    @Child private JSFunctionCallNode methodCallNode;
    @Child private IsObjectNode isObjectNode;
    @Child private JavaScriptNode iteratorNode;

    protected IteratorCloseNode(JSContext context, JavaScriptNode iteratorNode) {
        this.getReturnNode = GetMethodNode.create(context, null, "return");
        this.methodCallNode = JSFunctionCallNode.createCall();
        this.isObjectNode = IsObjectNode.create();
        this.iteratorNode = iteratorNode;
    }

    public static IteratorCloseNode create(JSContext context) {
        return new IteratorCloseNode(context, null);
    }

    public static IteratorCloseNode create(JSContext context, JavaScriptNode iteratorNode) {
        return new IteratorCloseNode(context, iteratorNode);
    }

    public final void executeVoid(DynamicObject iterator) {
        Object returnMethod = getReturnNode.executeWithTarget(iterator);
        if (returnMethod != Undefined.instance) {
            Object innerResult = methodCallNode.executeCall(JSArguments.createZeroArg(iterator, returnMethod));
            if (!isObjectNode.executeBoolean(innerResult)) {
                throw Errors.createNotAnObjectError(this);
            }
        }
    }

    public final Object execute(DynamicObject iterator, Object value) {
        executeVoid(iterator);
        return value;
    }

    public final void executeAbrupt(DynamicObject iterator) {
        Object returnMethod = getReturnNode.executeWithTarget(iterator);
        if (returnMethod != Undefined.instance) {
            try {
                methodCallNode.executeCall(JSArguments.createZeroArg(iterator, returnMethod));
            } catch (Exception e) {
                // re-throw outer exception, see 7.4.6 IteratorClose
            }
        }
    }

    public final <T extends Throwable> T executeRethrow(DynamicObject iterator, T exception) throws T {
        executeAbrupt(iterator);
        throw exception;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid((DynamicObject) iteratorNode.execute(frame));
        return Undefined.instance;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new IteratorCloseNode(getReturnNode.getContext(), cloneUninitialized(iteratorNode));
    }
}
