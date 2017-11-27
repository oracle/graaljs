/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.control.AwaitNode.AsyncAwaitExecutionContext;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * ES8 5.2 AsyncIteratorClose(iterator, completion).
 */
public class IteratorAsyncCloseWrapperNode extends JavaScriptNode implements ResumableNode {

    private final JSContext context;

    @Child private JSReadFrameSlotNode readAsyncResultNode;
    @Child private JSReadFrameSlotNode readAsyncContextNode;
    @Child private JSFunctionCallNode awaitTrampolineCall;

    @Child private JavaScriptNode loopNode;
    @Child private GetMethodNode getReturnNode;
    @Child private JSFunctionCallNode methodCallNode;
    @Child private IsObjectNode isObjectNode;
    @Child private JavaScriptNode iterator;
    @Child private PropertyGetNode getValue;

    protected IteratorAsyncCloseWrapperNode(JSContext context, JavaScriptNode loopNode, JavaScriptNode iterator, JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode) {
        this.context = context;
        this.loopNode = loopNode;
        this.iterator = iterator;

        this.getReturnNode = GetMethodNode.create(context, null, "return");

        this.methodCallNode = JSFunctionCallNode.createCall();
        this.isObjectNode = IsObjectNode.create();
        this.getValue = PropertyGetNode.create("value", false, context);

        this.readAsyncResultNode = asyncResultNode;
        this.readAsyncContextNode = asyncContextNode;
        this.awaitTrampolineCall = JSFunctionCallNode.create(false);

    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode loopNode, JavaScriptNode iterator, JSReadFrameSlotNode asyncContextNode,
                    JSReadFrameSlotNode asyncResultNode) {
        return new IteratorAsyncCloseWrapperNode(context, loopNode, iterator, asyncContextNode, asyncResultNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object completion = loopNode.execute(frame);
        // The for-await-of loop returned here.
        // 5.2 AsyncIteratorClose (iterator, completion)
        Object returnMethod = getReturnNode.executeWithTarget(frame, iterator.execute(frame));

        if (returnMethod == Undefined.instance) {
            return completion;
        }

        Object innerResult = methodCallNode.executeCall(JSArguments.create(iterator.execute(frame), returnMethod, Undefined.instance));

        Object[] initialState = (Object[]) readAsyncContextNode.execute(frame);
        CallTarget target = (CallTarget) initialState[0];
        DynamicObject currentCapability = (DynamicObject) initialState[1];
        awaitTrampolineCall.executeCall(JSArguments.create(Undefined.instance, context.getAsyncFunctionAwait(),
                        innerResult, new AsyncAwaitExecutionContext(target, currentCapability)));
        setState(frame, 1);
        // We throw YieldException so that we can rely on the generator's recovery mechanism. The
        // exception value is ignored.
        throw new YieldException(Undefined.instance);
    }

    @Override
    public Object resume(VirtualFrame frame) {
        int index = getStateAsInt(frame);
        if (index == 0) {
            return execute(frame);
        } else {
            setState(frame, 0);
            // We have been restored at this point. The frame contains the resumption state.
            Object innerResult = readAsyncResultNode.execute(frame);
            // TODO what if rejected?
            if (!JSObject.isJSObject(innerResult)) {
                throw Errors.createTypeError("not an object");
            }
            return innerResult;
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new IteratorAsyncCloseWrapperNode(context, cloneUninitialized(loopNode), cloneUninitialized(iterator), cloneUninitialized(readAsyncContextNode),
                        cloneUninitialized(readAsyncResultNode));
    }
}
