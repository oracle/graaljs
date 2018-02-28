/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IteratorCompleteNode;
import com.oracle.truffle.js.nodes.access.IteratorNextNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.WriteNode;
import com.oracle.truffle.js.nodes.control.ReturnNode.FrameReturnNode;
import com.oracle.truffle.js.nodes.control.YieldNode.ExceptionYieldResultNode;
import com.oracle.truffle.js.nodes.control.YieldNode.YieldResultNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class AsyncGeneratorYieldNode extends AwaitNode {
    @Child protected ReturnNode returnNode;
    @Child private YieldResultNode generatorYieldNode;

    protected AsyncGeneratorYieldNode(JSContext context, JavaScriptNode expression, JSReadFrameSlotNode readAsyncContextNode, JSReadFrameSlotNode readYieldResultNode, ReturnNode returnNode) {
        super(context, expression, readAsyncContextNode, readYieldResultNode);
        this.returnNode = returnNode;
        this.generatorYieldNode = new ExceptionYieldResultNode();
    }

    public static AsyncGeneratorYieldNode createYield(JSContext context, JavaScriptNode expression, JSReadFrameSlotNode readAsyncContextNode, JSReadFrameSlotNode readAsyncResultNode,
                    ReturnNode returnNode) {
        return new AsyncGeneratorYieldNode(context, expression, readAsyncContextNode, readAsyncResultNode, returnNode);
    }

    public static AsyncGeneratorYieldNode createYieldStar(JSContext context, JavaScriptNode expression, JSReadFrameSlotNode readAsyncContextNode, JSReadFrameSlotNode readAsyncResultNode,
                    ReturnNode returnNode, JavaScriptNode readTemp, WriteNode writeTemp) {
        return new AsyncGeneratorYieldStarNode(context, expression, readAsyncContextNode, readAsyncResultNode, returnNode, readTemp, writeTemp);
    }

    @Override
    public Object resume(VirtualFrame frame) {
        int state = getStateAsInt(frame);
        // 0 .. execute expression and await
        // 1 .. resume await and yield
        // 2 .. resume yield (and await if return)
        // 3 .. resume await and return
        final int awaitValue = 1;
        final int suspendedYield = 2;
        final int awaitResumptionValue = 3;

        if (state <= awaitValue) {
            Object awaited;
            if (state == 0) {
                Object value = expression.execute(frame);
                setState(frame, awaitValue);
                awaited = suspendAwait(frame, value);
            } else {
                assert state == awaitValue;
                awaited = resumeAwait(frame);
            }
            setState(frame, suspendedYield);
            return suspendYield(frame, awaited);
        } else {
            assert state >= suspendedYield;
            setState(frame, 0);
            Completion completion = resumeYield(frame);
            if (completion.isNormal()) {
                return completion.getValue();
            } else if (completion.isThrow()) {
                throw UserScriptException.create(completion.getValue(), this);
            } else {
                assert completion.isReturn();
                // Let awaited be Await(resumptionValue.[[Value]]).
                Object awaited;
                if (state == suspendedYield) {
                    setState(frame, awaitResumptionValue);
                    awaited = suspendAwait(frame, completion.getValue());
                } else {
                    assert state == awaitResumptionValue;
                    // If awaited.[[Type]] is throw return Completion(awaited).
                    awaited = resumeAwait(frame);
                }
                // Assert: awaited.[[Type]] is normal.
                return returnValue(frame, awaited);
            }
        }
    }

    protected final Object suspendYield(VirtualFrame frame, Object awaited) {
        return generatorYieldNode.generatorYield(frame, awaited);
    }

    protected final Completion resumeYield(VirtualFrame frame) {
        return (Completion) readAsyncResultNode.execute(frame);
    }

    protected final Object returnValue(VirtualFrame frame, Object value) {
        assert getStateAsInt(frame) == 0;
        if (returnNode instanceof FrameReturnNode) {
            ((WriteNode) returnNode.expression).executeWrite(frame, value);
        }
        throw new ReturnException(value);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return createYield(context, cloneUninitialized(expression), cloneUninitialized(readAsyncContextNode), cloneUninitialized(readAsyncResultNode), cloneUninitialized(returnNode));
    }
}

class AsyncGeneratorYieldStarNode extends AsyncGeneratorYieldNode {
    @Child private JavaScriptNode readTemp;
    @Child private WriteNode writeTemp;

    @Child private GetIteratorNode getIteratorNode;
    @Child private IteratorNextNode iteratorNextNode;
    @Child private IteratorCompleteNode iteratorCompleteNode;
    @Child private IteratorValueNode iteratorValueNode;
    @Child private GetMethodNode getThrowMethodNode;
    @Child private GetMethodNode getReturnMethodNode;
    @Child private JSFunctionCallNode callThrowNode;
    @Child private JSFunctionCallNode callReturnNode;

    protected AsyncGeneratorYieldStarNode(JSContext context, JavaScriptNode expression, JSReadFrameSlotNode readAsyncContextNode, JSReadFrameSlotNode readYieldResultNode,
                    ReturnNode returnNode, JavaScriptNode readTemp, WriteNode writeTemp) {
        super(context, expression, readAsyncContextNode, readYieldResultNode, returnNode);
        this.readTemp = readTemp;
        this.writeTemp = writeTemp;

        this.getIteratorNode = GetIteratorNode.createAsync(context, null);
        this.iteratorNextNode = IteratorNextNode.create(context);
        this.iteratorCompleteNode = IteratorCompleteNode.create(context);
        this.iteratorValueNode = IteratorValueNode.create(context, null);
        this.getThrowMethodNode = GetMethodNode.create(context, null, "throw");
        this.getReturnMethodNode = GetMethodNode.create(context, null, "return");
        this.callThrowNode = JSFunctionCallNode.createCall();
        this.callReturnNode = JSFunctionCallNode.createCall();
    }

    @Override
    public Object resume(VirtualFrame frame) {
        int state = getStateAsInt(frame);
        final int loopBegin = 1;
        final int normalAwaitInnerResult = 12;
        final int normalAsyncGeneratorYieldInnerResult = 13;
        final int normalAsyncGeneratorYieldInnerResultSuspendedYield = 14;
        final int normalAsyncGeneratorYieldInnerResultReturn = 15;
        final int throwMethodAwaitInnerResult = 22;
        final int throwAsyncGeneratorYieldInnerResult = 23;
        final int throwAsyncGeneratorYieldInnerResultSuspendedYield = 24;
        final int throwAsyncGeneratorYieldInnerResultReturn = 25;
        final int throwAwaitReturnResult = 26;
        final int returnAwaitInnerReturnResult = 32;
        final int returnAsyncGeneratorYieldInnerReturnResult = 33;
        final int returnAsyncGeneratorYieldInnerReturnResultSuspendedYield = 34;
        final int returnAsyncGeneratorYieldInnerReturnResultReturn = 35;
        final int returnAwaitReceivedValue = 36;

        DynamicObject iterator;
        if (state == 0) {
            iterator = getIteratorNode.execute(expression.execute(frame));
            writeTemp.executeWrite(frame, iterator);
            state = loopBegin;
        } else {
            iterator = (DynamicObject) readTemp.execute(frame);
        }

        Completion received = Completion.forNormal(Undefined.instance);
        Object awaited;
        for (;;) {
            switch (state) {
                case loopBegin: {
                    if (received.isNormal()) {
                        DynamicObject innerResult = iteratorNextNode.execute(iterator, received.getValue());
                        awaited = awaitWithNext(frame, innerResult, normalAwaitInnerResult);
                    } else if (received.isThrow()) {
                        Object throwMethod = getThrowMethodNode.executeWithTarget(iterator);
                        if (throwMethod != Undefined.instance) {
                            Object innerResult = callThrowMethod(throwMethod, iterator, received.getValue());
                            awaited = awaitWithNext(frame, innerResult, throwMethodAwaitInnerResult);
                            /*
                             * NOTE: Exceptions from the inner iterator throw method are propagated.
                             * Normal completions from an inner throw method are processed similarly
                             * to an inner next.
                             */
                        } else {
                            /*
                             * NOTE: If iterator does not have a throw method, this throw is going
                             * to terminate the yield* loop. But first we need to give iterator a
                             * chance to clean up.
                             */
                            // AsyncIteratorClose
                            Object returnMethod = getReturnMethodNode.executeWithTarget(iterator);
                            error: if (returnMethod != Undefined.instance) {
                                Object returnResult;
                                try {
                                    returnResult = callReturnNode.executeCall(JSArguments.createZeroArg(iterator, returnMethod));
                                } catch (GraalJSException e) {
                                    // swallow inner error
                                    break error;
                                }
                                awaited = awaitWithNext(frame, returnResult, throwAwaitReturnResult);
                            }
                            throw Errors.createTypeError("yield* protocol violation: iterator does not have a throw method");
                        }
                    } else {
                        assert received.isReturn();
                        Object returnMethod = getReturnMethodNode.executeWithTarget(iterator);
                        if (returnMethod != Undefined.instance) {
                            Object innerReturnResult = callReturnMethod(returnMethod, iterator, received.getValue());
                            awaitWithNext(frame, innerReturnResult, returnAwaitInnerReturnResult);
                        } else {
                            awaited = awaitWithNext(frame, received.getValue(), returnAwaitReceivedValue);
                        }
                    }
                    break;
                }

                // type is normal
                case normalAwaitInnerResult: {
                    awaited = resumeAwait(frame);
                    DynamicObject innerResult = checkcastIterResult(awaited);
                    boolean done = iteratorCompleteNode.execute(innerResult);
                    if (done) {
                        reset(frame);
                        return iteratorValueNode.execute(innerResult);
                    }
                    Object iteratorValue = iteratorValueNode.execute(innerResult);
                    awaited = awaitWithNext(frame, iteratorValue, normalAsyncGeneratorYieldInnerResult);
                    break;
                }
                case normalAsyncGeneratorYieldInnerResult: {
                    awaited = resumeAwait(frame);
                    yieldWithNext(frame, awaited, normalAsyncGeneratorYieldInnerResultSuspendedYield);
                    break;
                }
                case normalAsyncGeneratorYieldInnerResultSuspendedYield: {
                    Completion resumptionValue = resumeYield(frame);
                    if (!resumptionValue.isReturn()) {
                        received = resumptionValue;
                        state = loopBegin; // repeat
                        break;
                    } else {
                        assert resumptionValue.isReturn();
                        awaited = awaitWithNext(frame, resumptionValue.getValue(), normalAsyncGeneratorYieldInnerResultReturn);
                    }
                    break;
                }
                case normalAsyncGeneratorYieldInnerResultReturn: {
                    Completion returnValue = resumeYield(frame);
                    if (returnValue.isNormal()) {
                        received = Completion.forReturn(returnValue.getValue());
                    } else {
                        assert returnValue.isThrow();
                        received = returnValue;
                    }
                    state = loopBegin; // repeat
                    break;
                }

                // type is throw
                case throwMethodAwaitInnerResult: {
                    awaited = resumeAwait(frame);
                    DynamicObject innerResult = checkcastIterResult(awaited);
                    boolean done = iteratorCompleteNode.execute(innerResult);
                    if (done) {
                        reset(frame);
                        return iteratorValueNode.execute(innerResult);
                    }
                    Object iteratorValue = iteratorValueNode.execute(innerResult);
                    awaited = awaitWithNext(frame, iteratorValue, throwAsyncGeneratorYieldInnerResult);
                    break;
                }
                case throwAsyncGeneratorYieldInnerResult: {
                    awaited = resumeAwait(frame);
                    yieldWithNext(frame, awaited, throwAsyncGeneratorYieldInnerResultSuspendedYield);
                    break;
                }
                case throwAsyncGeneratorYieldInnerResultSuspendedYield: {
                    Completion resumptionValue = resumeYield(frame);
                    if (!resumptionValue.isReturn()) {
                        received = resumptionValue;
                        state = loopBegin; // repeat
                        break;
                    } else {
                        assert resumptionValue.isReturn();
                        awaited = awaitWithNext(frame, resumptionValue.getValue(), throwAsyncGeneratorYieldInnerResultReturn);
                    }
                    break;
                }
                case throwAsyncGeneratorYieldInnerResultReturn: {
                    Completion returnValue = resumeYield(frame);
                    if (returnValue.isNormal()) {
                        received = Completion.forReturn(returnValue.getValue());
                    } else {
                        assert returnValue.isThrow();
                        received = returnValue;
                    }
                    state = loopBegin; // repeat
                    break;
                }
                case throwAwaitReturnResult: {
                    // AsyncIteratorClose: handle Await(innerResult) throw completion.
                    awaited = resumeAwait(frame);
                    throw Errors.createTypeError("yield* protocol violation: iterator does not have a throw method");
                }

                // type is return
                case returnAwaitInnerReturnResult: {
                    awaited = resumeAwait(frame);
                    DynamicObject innerReturnResult = checkcastIterResult(awaited);
                    boolean done = iteratorCompleteNode.execute(innerReturnResult);
                    if (done) {
                        reset(frame);
                        return returnValue(frame, iteratorValueNode.execute(innerReturnResult));
                    }
                    Object iteratorValue = iteratorValueNode.execute(innerReturnResult);
                    awaited = awaitWithNext(frame, iteratorValue, returnAsyncGeneratorYieldInnerReturnResult);
                    break;
                }
                case returnAsyncGeneratorYieldInnerReturnResult: {
                    awaited = resumeAwait(frame);
                    yieldWithNext(frame, awaited, returnAsyncGeneratorYieldInnerReturnResultSuspendedYield);
                    break;
                }
                case returnAsyncGeneratorYieldInnerReturnResultSuspendedYield: {
                    Completion resumptionValue = resumeYield(frame);
                    if (!resumptionValue.isReturn()) {
                        received = resumptionValue;
                        state = loopBegin; // repeat
                    } else {
                        assert resumptionValue.isReturn();
                        awaited = awaitWithNext(frame, resumptionValue.getValue(), returnAsyncGeneratorYieldInnerReturnResultReturn);
                    }
                    break;
                }
                case returnAsyncGeneratorYieldInnerReturnResultReturn: {
                    Completion returnValue = resumeYield(frame);
                    if (returnValue.isNormal()) {
                        received = Completion.forReturn(returnValue.getValue());
                    } else {
                        assert returnValue.isThrow();
                        received = returnValue;
                    }
                    state = loopBegin; // repeat
                    break;
                }
                case returnAwaitReceivedValue: {
                    awaited = resumeAwait(frame);
                    reset(frame);
                    return returnValue(frame, awaited);
                }
                default:
                    throw Errors.shouldNotReachHere();
            }
            // Either suspend and resume into another state or repeat from the beginning.
            assert state == loopBegin;
        }
    }

    private Object awaitWithNext(VirtualFrame frame, Object value, int nextState) {
        setState(frame, nextState);
        return suspendAwait(frame, value);
    }

    private Object yieldWithNext(VirtualFrame frame, Object value, int nextState) {
        setState(frame, nextState);
        return suspendYield(frame, value);
    }

    private void reset(VirtualFrame frame) {
        setState(frame, 0);
        writeTemp.executeWrite(frame, Undefined.instance);
    }

    private Object callThrowMethod(Object throwMethod, DynamicObject iterator, Object received) {
        return callThrowNode.executeCall(JSArguments.createOneArg(iterator, throwMethod, received));
    }

    private Object callReturnMethod(Object returnMethod, DynamicObject iterator, Object received) {
        return callReturnNode.executeCall(JSArguments.createOneArg(iterator, returnMethod, received));
    }

    private static DynamicObject checkcastIterResult(Object iterResult) {
        if (!JSRuntime.isObject(iterResult)) {
            throw Errors.createTypeError("Iterator Result not an object");
        }
        return (DynamicObject) iterResult;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return createYieldStar(context, cloneUninitialized(expression), cloneUninitialized(readAsyncContextNode), cloneUninitialized(readAsyncResultNode), cloneUninitialized(returnNode),
                        cloneUninitialized(readTemp), (WriteNode) cloneUninitialized((JavaScriptNode) writeTemp));
    }
}
