/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.AwaitNode;
import com.oracle.truffle.js.nodes.control.YieldException;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Utility node implementing ES8 ForIn/OfBodyEvaluation handling of async iterators.
 *
 * See ES8 7.2.2.9 and 4.1 Await.
 */
public class AsyncIteratorStepNode extends AwaitNode {

    private final JSContext context;

    @Child private IteratorNextNode iteratorNextNode;
    @Child private IteratorCompleteNode iteratorCompleteNode;

    protected AsyncIteratorStepNode(JSContext context, JavaScriptNode iterator, JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode) {
        super(context, iterator, asyncContextNode, asyncResultNode);
        this.context = context;
        this.iteratorNextNode = IteratorNextNode.create(context);
        this.iteratorCompleteNode = IteratorCompleteNode.create(context);
    }

    public static AwaitNode create(JSContext context, JavaScriptNode iterator, JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode) {
        return new AsyncIteratorStepNode(context, iterator, asyncContextNode, asyncResultNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        DynamicObject iterator = (DynamicObject) expression.execute(frame);
        Object nextResult = iteratorNextNode.execute(iterator, Undefined.instance);
        // suspend at Await nextResult
        Object[] initialState = (Object[]) readAsyncContextNode.execute(frame);
        CallTarget target = (CallTarget) initialState[0];
        DynamicObject currentCapability = (DynamicObject) initialState[1];
        awaitTrampolineCall.executeCall(JSArguments.create(Undefined.instance, setAsyncContextNode.getContext().getAsyncFunctionAwait(),
                        nextResult, new AsyncAwaitExecutionContext(target, currentCapability)));
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
            Object result = readAsyncResultNode.execute(frame);
            if (result instanceof Rejected) {
                throw UserScriptException.create(((Rejected) result).reason, this);
            }
            if (!JSObject.isJSObject(result)) {
                throw Errors.createTypeError("Iterator Result not an object");
            }
            Object done = iteratorCompleteNode.execute((DynamicObject) result);
            if (done instanceof Boolean && ((Boolean) done) == Boolean.TRUE) {
                return false;
            }
            return result;
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new AsyncIteratorStepNode(context, cloneUninitialized(expression), cloneUninitialized(readAsyncContextNode), cloneUninitialized(readAsyncResultNode));
    }

}
