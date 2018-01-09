/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class AwaitNode extends JavaScriptNode implements ResumableNode {

    @Child protected JavaScriptNode expression;
    @Child protected JSReadFrameSlotNode readAsyncResultNode;
    @Child protected JSReadFrameSlotNode readAsyncContextNode;
    @Child protected JSFunctionCallNode awaitTrampolineCall;
    @Child protected PropertySetNode setAsyncContextNode;

    protected AwaitNode(JSContext context, JavaScriptNode expression, JSReadFrameSlotNode readAsyncContextNode, JSReadFrameSlotNode readAsyncResultNode) {
        this.expression = expression;
        this.readAsyncResultNode = readAsyncResultNode;
        this.readAsyncContextNode = readAsyncContextNode;
        this.awaitTrampolineCall = JSFunctionCallNode.create(false);
        this.setAsyncContextNode = PropertySetNode.create("context", false, context, false);
    }

    public static AwaitNode create(JSContext context, JavaScriptNode expression, JSReadFrameSlotNode readAsyncContextNode, JSReadFrameSlotNode readAsyncResultNode) {
        return new AwaitNode(context, expression, readAsyncContextNode, readAsyncResultNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = expression.execute(frame);
        Object[] initialState = (Object[]) readAsyncContextNode.execute(frame);
        CallTarget target = (CallTarget) initialState[0];
        DynamicObject currentCapability = (DynamicObject) initialState[1];
        awaitTrampolineCall.executeCall(JSArguments.create(Undefined.instance, setAsyncContextNode.getContext().getAsyncFunctionAwait(),
                        value, new AsyncAwaitExecutionContext(target, currentCapability)));
        setState(frame, 1);
        // We throw YieldException so that we can rely on the generator's recovery mechanism. The
        // exception value is ignored.
        throw new YieldException(Undefined.instance);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        JavaScriptNode expressionCopy = cloneUninitialized(expression);
        JSReadFrameSlotNode asyncResultCopy = cloneUninitialized(readAsyncResultNode);
        JSReadFrameSlotNode asyncContextCopy = cloneUninitialized(readAsyncContextNode);
        return create(setAsyncContextNode.getContext(), expressionCopy, asyncContextCopy, asyncResultCopy);
    }

    @Override
    public Object resume(VirtualFrame frame) {
        int index = getStateAsInt(frame);
        if (index == 0) {
            Object value = expression.execute(frame);
            Object[] initialState = (Object[]) readAsyncContextNode.execute(frame);
            CallTarget target = (CallTarget) initialState[0];
            DynamicObject currentCapability = (DynamicObject) initialState[1];
            awaitTrampolineCall.executeCall(JSArguments.create(Undefined.instance, setAsyncContextNode.getContext().getAsyncFunctionAwait(),
                            value, new AsyncAwaitExecutionContext(target, currentCapability)));
            setState(frame, 1);
            throw new YieldException(Undefined.instance);
        } else {
            setState(frame, 0);
            // We have been restored at this point. The frame contains the resumption state.
            Object result = readAsyncResultNode.execute(frame);
            if (result instanceof Rejected) {
                throw UserScriptException.create(((Rejected) result).reason, this);
            }
            return result;
        }
    }

    public static class AsyncAwaitExecutionContext {

        public final CallTarget target;
        public final DynamicObject capability;

        public AsyncAwaitExecutionContext(CallTarget target, DynamicObject currentCapability) {
            this.target = target;
            this.capability = currentCapability;
        }

    }

    public static final class Rejected {
        public final Object reason;

        public Rejected(Object reason) {
            this.reason = reason;
        }
    }
}
