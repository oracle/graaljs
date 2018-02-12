/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * ES8 5.2 AsyncIteratorClose(iterator, completion).
 */
public class AsyncIteratorCloseWrapperNode extends AwaitNode {

    private final JSContext context;
    @Child private JavaScriptNode loopNode;
    @Child private GetMethodNode getReturnNode;
    @Child private JSFunctionCallNode methodCallNode;
    @Child private IsObjectNode isObjectNode;
    @Child private JavaScriptNode iteratorNode;
    @Child private JavaScriptNode doneNode;

    protected AsyncIteratorCloseWrapperNode(JSContext context, JavaScriptNode loopNode, JavaScriptNode iteratorNode, JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode,
                    JavaScriptNode doneNode) {
        super(context, null, asyncContextNode, asyncResultNode);
        this.context = context;
        this.loopNode = loopNode;
        this.iteratorNode = iteratorNode;
        this.doneNode = doneNode;

        this.getReturnNode = GetMethodNode.create(context, null, "return");

        this.methodCallNode = JSFunctionCallNode.createCall();
        this.isObjectNode = IsObjectNode.create();

        this.readAsyncResultNode = asyncResultNode;
        this.readAsyncContextNode = asyncContextNode;
        this.awaitTrampolineCall = JSFunctionCallNode.createCall();

    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode loopNode, JavaScriptNode iterator, JSReadFrameSlotNode asyncContextNode,
                    JSReadFrameSlotNode asyncResultNode, JavaScriptNode doneNode) {
        return new AsyncIteratorCloseWrapperNode(context, loopNode, iterator, asyncContextNode, asyncResultNode, doneNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result;
        try {
            result = loopNode.execute(frame);
        } catch (YieldException e) {
            throw e;
        } catch (Exception e) {
            Object iterator = iteratorNode.execute(frame);
            Object returnMethod = getReturnNode.executeWithTarget(frame, iterator);
            if (returnMethod != Undefined.instance) {
                try {
                    methodCallNode.executeCall(JSArguments.createZeroArg(iterator, returnMethod));
                } catch (Exception ex) {
                    // re-throw outer exception
                }
            }
            throw e;
        }
        if (StatementNode.executeConditionAsBoolean(frame, doneNode)) {
            return result;
        } else {
            Object iterator = iteratorNode.execute(frame);
            Object returnMethod = getReturnNode.executeWithTarget(frame, iterator);
            if (returnMethod == Undefined.instance) {
                return result;
            }
            Object innerResult = methodCallNode.executeCall(JSArguments.createZeroArg(iterator, returnMethod));
            setState(frame, 1);
            return suspendAwait(frame, innerResult);
        }
    }

    @Override
    public Object resume(VirtualFrame frame) {
        int index = getStateAsInt(frame);
        if (index == 0) {
            return execute(frame);
        } else {
            setState(frame, 0);
            Object innerResult = resumeAwait(frame);
            if (!JSObject.isJSObject(innerResult)) {
                throw Errors.createTypeError("not an object");
            }
            return innerResult;
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new AsyncIteratorCloseWrapperNode(context, cloneUninitialized(loopNode), cloneUninitialized(iteratorNode), cloneUninitialized(readAsyncContextNode),
                        cloneUninitialized(readAsyncResultNode), cloneUninitialized(doneNode));
    }
}
