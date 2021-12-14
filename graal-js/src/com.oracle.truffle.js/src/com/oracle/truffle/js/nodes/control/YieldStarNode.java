/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.nodes.control;

import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorCompleteNode;
import com.oracle.truffle.js.nodes.access.IteratorNextNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class YieldStarNode extends AbstractYieldNode implements ResumableNode.WithObjectState {
    @Child private GetIteratorNode getIteratorNode;
    @Child private IteratorNextNode iteratorNextNode;
    @Child private IteratorCompleteNode iteratorCompleteNode;
    @Child private IteratorValueNode iteratorValueNode;
    @Child private GetMethodNode getThrowMethodNode;
    @Child private GetMethodNode getReturnMethodNode;
    @Child private JSFunctionCallNode callThrowNode;
    @Child private JSFunctionCallNode callReturnNode;
    @Child private IteratorCloseNode iteratorCloseNode;
    private final BranchProfile errorBranch = BranchProfile.create();

    protected YieldStarNode(JSContext context, int stateSlot, JavaScriptNode expression, JavaScriptNode yieldValue, ReturnNode returnNode, YieldResultNode yieldResultNode) {
        super(context, stateSlot, expression, yieldValue, returnNode, yieldResultNode);
        this.getIteratorNode = GetIteratorNode.create(context);
        this.iteratorNextNode = IteratorNextNode.create();
        this.iteratorCompleteNode = IteratorCompleteNode.create(context);
        this.iteratorValueNode = IteratorValueNode.create(context, null);
        this.getThrowMethodNode = GetMethodNode.create(context, "throw");
        this.getReturnMethodNode = GetMethodNode.create(context, "return");
        this.callThrowNode = JSFunctionCallNode.createCall();
        this.callReturnNode = JSFunctionCallNode.createCall();
        this.iteratorCloseNode = IteratorCloseNode.create(context);
    }

    private Object executeBegin(VirtualFrame frame) {
        IteratorRecord iteratorRecord = getIteratorNode.execute(expression.execute(frame));
        Object received = Undefined.instance;
        Object innerResult = iteratorNextNode.execute(iteratorRecord, received);
        if (iteratorCompleteNode.execute(innerResult)) {
            return iteratorValueNode.execute(innerResult);
        }
        return saveStateAndYield(frame, iteratorRecord, innerResult);
    }

    private Object saveStateAndYield(VirtualFrame frame, IteratorRecord iteratorRecord, Object innerResult) {
        setState(frame, stateSlot, iteratorRecord);
        return generatorYield(frame, innerResult);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object state = getState(frame, stateSlot);
        if (state == Undefined.instance) {
            return executeBegin(frame);
        } else {
            resetState(frame, stateSlot);
            IteratorRecord iteratorRecord = (IteratorRecord) state;
            Object received = yieldValue.execute(frame);
            if (!(received instanceof Completion)) {
                Object innerResult = iteratorNextNode.execute(iteratorRecord, received);
                if (iteratorCompleteNode.execute(innerResult)) {
                    return iteratorValueNode.execute(innerResult);
                }
                return saveStateAndYield(frame, iteratorRecord, innerResult);
            } else {
                Completion completion = (Completion) received;
                received = completion.getValue();
                if (returnOrExceptionProfile.profile(completion.isThrow())) {
                    return resumeThrow(frame, iteratorRecord, received);
                } else {
                    assert completion.isReturn();
                    return resumeReturn(frame, iteratorRecord, received);
                }
            }
        }
    }

    private Object resumeReturn(VirtualFrame frame, IteratorRecord iteratorRecord, Object received) {
        DynamicObject iterator = iteratorRecord.getIterator();
        Object returnMethod = getReturnMethodNode.executeWithTarget(iterator);
        if (returnMethod == Undefined.instance) {
            return returnValue(frame, received);
        } else {
            DynamicObject innerReturnResult = callReturnMethod(iterator, received, returnMethod);
            if (iteratorCompleteNode.execute(innerReturnResult)) {
                return returnValue(frame, iteratorValueNode.execute(innerReturnResult));
            }
            return saveStateAndYield(frame, iteratorRecord, innerReturnResult);
        }
    }

    private Object resumeThrow(VirtualFrame frame, IteratorRecord iteratorRecord, Object received) {
        DynamicObject iterator = iteratorRecord.getIterator();
        Object throwMethod = getThrowMethodNode.executeWithTarget(iterator);
        if (throwMethod != Undefined.instance) {
            DynamicObject innerResult = callThrowMethod(iterator, received, throwMethod);
            if (iteratorCompleteNode.execute(innerResult)) {
                return iteratorValueNode.execute(innerResult);
            }
            return saveStateAndYield(frame, iteratorRecord, innerResult);
        } else {
            errorBranch.enter();
            iteratorCloseNode.executeVoid(iterator);
            throw Errors.createTypeErrorYieldStarThrowMethodMissing(this);
        }
    }

    private DynamicObject callThrowMethod(DynamicObject iterator, Object received, Object throwMethod) {
        Object innerResult = callThrowNode.executeCall(JSArguments.createOneArg(iterator, throwMethod, received));
        if (!JSRuntime.isObject(innerResult)) {
            errorBranch.enter();
            throw Errors.createTypeErrorIterResultNotAnObject(innerResult, this);
        }
        return (DynamicObject) innerResult;
    }

    private DynamicObject callReturnMethod(DynamicObject iterator, Object received, Object returnMethod) {
        Object innerResult = callReturnNode.executeCall(JSArguments.createOneArg(iterator, returnMethod, received));
        if (!JSRuntime.isObject(innerResult)) {
            errorBranch.enter();
            throw Errors.createTypeErrorIterResultNotAnObject(innerResult, this);
        }
        return (DynamicObject) innerResult;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new YieldStarNode(context, stateSlot, cloneUninitialized(expression, materializedTags),
                        cloneUninitialized(yieldValue, materializedTags), cloneUninitialized(returnNode, materializedTags), generatorYieldNode.cloneUninitialized());
    }
}
