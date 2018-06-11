/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayDeque;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessFunctionNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.function.JSNewNode.SpecializedNewObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunction.AsyncGeneratorState;
import com.oracle.truffle.js.runtime.objects.AsyncGeneratorRequest;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class AsyncGeneratorBodyNode extends JavaScriptNode {

    @NodeInfo(cost = NodeCost.NONE, language = "JavaScript", description = "The root node of async generator functions in JavaScript.")
    private static final class AsyncGeneratorRootNode extends JavaScriptRootNode {
        @Child private PropertyGetNode getGeneratorState;
        @Child private PropertySetNode setGeneratorState;
        @Child private JavaScriptNode functionBody;
        @Child private JSWriteFrameSlotNode writeYieldValue;
        @Child private JSReadFrameSlotNode readYieldResult;
        @Child private AsyncGeneratorResolveNode asyncGeneratorResolveNode;
        @Child private AsyncGeneratorRejectNode asyncGeneratorRejectNode;
        @Child private AsyncGeneratorResumeNextNode asyncGeneratorResumeNextNode;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
        private final JSContext context;

        AsyncGeneratorRootNode(JSContext context, JavaScriptNode functionBody, JSWriteFrameSlotNode writeYieldValueNode, JSReadFrameSlotNode readYieldResultNode, SourceSection functionSourceSection) {
            super(context.getLanguage(), functionSourceSection, null);
            this.getGeneratorState = PropertyGetNode.createGetHidden(JSFunction.ASYNC_GENERATOR_STATE_ID, context);
            this.setGeneratorState = PropertySetNode.createSetHidden(JSFunction.ASYNC_GENERATOR_STATE_ID, context);
            this.functionBody = functionBody;
            this.writeYieldValue = writeYieldValueNode;
            this.readYieldResult = readYieldResultNode;
            this.context = context;
            this.asyncGeneratorResolveNode = AsyncGeneratorResolveNode.create(context);
            this.asyncGeneratorResumeNextNode = AsyncGeneratorResumeNextNode.createTailCall(context);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] arguments = frame.getArguments();
            VirtualFrame generatorFrame = JSFrameUtil.castMaterializedFrame(arguments[0]);
            DynamicObject generatorObject = (DynamicObject) arguments[1];
            Completion completion = (Completion) arguments[2];

            for (;;) {
                AsyncGeneratorState state = (AsyncGeneratorState) getGeneratorState.getValue(generatorObject);

                // State must be Executing when called from AsyncGeneratorResumeNext.
                // State can be Executing or SuspendedYield when resuming from Await.
                assert state == AsyncGeneratorState.Executing || state == AsyncGeneratorState.SuspendedYield : state;
                writeYieldValue.executeWrite(generatorFrame, completion);

                try {
                    Object result = functionBody.execute(generatorFrame);
                    setGeneratorState.setValue(generatorObject, state = AsyncGeneratorState.Completed);
                    asyncGeneratorResolveNode.performResolve(frame, generatorObject, result, true);
                } catch (YieldException e) {
                    if (e.isYield()) {
                        setGeneratorState.setValue(generatorObject, state = AsyncGeneratorState.SuspendedYield);
                        asyncGeneratorResolveNode.performResolve(frame, generatorObject, e.getResult(), false);
                    } else {
                        assert e.isAwait();
                        return Undefined.instance;
                    }
                } catch (Throwable e) {
                    if (shouldCatch(e)) {
                        setGeneratorState.setValue(generatorObject, state = AsyncGeneratorState.Completed);
                        Object reason = getErrorObjectNode.execute(e);
                        asyncGeneratorRejectNode.performReject(generatorFrame, generatorObject, reason);
                    } else {
                        throw e;
                    }
                }
                // AsyncGeneratorResolve/AsyncGeneratorReject => AsyncGeneratorResumeNext
                Object nextCompletion = asyncGeneratorResumeNextNode.execute(generatorFrame, generatorObject);
                if (nextCompletion instanceof Completion) {
                    completion = (Completion) nextCompletion;
                    continue; // tail call from AsyncGeneratorResumeNext
                } else {
                    return Undefined.instance;
                }
            }
        }

        private boolean shouldCatch(Throwable exception) {
            if (getErrorObjectNode == null || asyncGeneratorRejectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
                asyncGeneratorRejectNode = insert(AsyncGeneratorRejectNode.create(context));
            }
            return TryCatchNode.shouldCatch(exception);
        }

        @Override
        public boolean isResumption() {
            return true;
        }
    }

    @Child private JavaScriptNode createAsyncGeneratorObject;
    @Child private PropertySetNode setGeneratorState;
    @Child private PropertySetNode setGeneratorContext;
    @Child private PropertySetNode setGeneratorTarget;
    @Child private PropertySetNode setGeneratorQueue;

    @Child private PropertyGetNode getPromise;
    @Child private PropertyGetNode getPromiseReject;
    @Child private JSFunctionCallNode createPromiseCapability;
    @Child private JSFunctionCallNode executePromiseMethod;
    @Child private PropertySetNode setAsyncContext;

    @CompilationFinal RootCallTarget resumeTarget;
    private final JSContext context;

    @Child private JavaScriptNode functionBody;
    @Child private JSWriteFrameSlotNode writeYieldValueNode;
    @Child private JSReadFrameSlotNode readYieldResultNode;
    @Child private JSWriteFrameSlotNode writeAsyncContext;

    public AsyncGeneratorBodyNode(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode writeYieldValueNode, JSReadFrameSlotNode readYieldResultNode, JSWriteFrameSlotNode writeAsyncContext) {
        this.writeAsyncContext = writeAsyncContext;
        JavaScriptNode functionObject = AccessFunctionNode.create();
        this.createAsyncGeneratorObject = SpecializedNewObjectNode.create(context, false, true, true, functionObject);

        this.setGeneratorState = PropertySetNode.createSetHidden(JSFunction.ASYNC_GENERATOR_STATE_ID, context);
        this.setGeneratorContext = PropertySetNode.createSetHidden(JSFunction.ASYNC_GENERATOR_CONTEXT_ID, context);
        this.setGeneratorTarget = PropertySetNode.createSetHidden(JSFunction.ASYNC_GENERATOR_TARGET_ID, context);
        this.setGeneratorQueue = PropertySetNode.createSetHidden(JSFunction.ASYNC_GENERATOR_QUEUE_ID, context);

        this.context = context;

        // these children are adopted here only temporarily; they will be transferred later
        this.functionBody = body;
        this.writeYieldValueNode = writeYieldValueNode;
        this.readYieldResultNode = readYieldResultNode;
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode writeYieldValueNode, JSReadFrameSlotNode readYieldResultNode,
                    JSWriteFrameSlotNode writeAsyncContext) {
        return new AsyncGeneratorBodyNode(context, body, writeYieldValueNode, readYieldResultNode, writeAsyncContext);
    }

    private void initializeCallTarget() {
        CompilerAsserts.neverPartOfCompilation();
        atomic(() -> {
            AsyncGeneratorRootNode asyncGeneratorRootNode = new AsyncGeneratorRootNode(context, functionBody, writeYieldValueNode, readYieldResultNode, getRootNode().getSourceSection());
            this.resumeTarget = Truffle.getRuntime().createCallTarget(asyncGeneratorRootNode);
            // these children have been transferred to the generator root node and are now disowned
            this.functionBody = null;
            this.writeYieldValueNode = null;
            this.readYieldResultNode = null;
        });
    }

    private void ensureCallTargetInitialized() {
        if (resumeTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            initializeCallTarget();
        }
    }

    private void asyncGeneratorStart(VirtualFrame frame, DynamicObject generatorObject) {
        MaterializedFrame materializedFrame = frame.materialize();
        setGeneratorState.setValue(generatorObject, AsyncGeneratorState.SuspendedStart);
        setGeneratorContext.setValue(generatorObject, materializedFrame);
        setGeneratorTarget.setValue(generatorObject, resumeTarget);
        setGeneratorQueue.setValue(generatorObject, new ArrayDeque<AsyncGeneratorRequest>(4));
        writeAsyncContext.executeWrite(frame, new Object[]{resumeTarget, generatorObject, materializedFrame});
    }

    @Override
    public Object execute(VirtualFrame frame) {
        ensureCallTargetInitialized();

        DynamicObject generatorObject;
        try {
            generatorObject = createAsyncGeneratorObject.executeDynamicObject(frame);
        } catch (UnexpectedResultException e) {
            throw Errors.shouldNotReachHere();
        }

        asyncGeneratorStart(frame, generatorObject);

        return generatorObject;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        if (resumeTarget == null) {
            return create(context, cloneUninitialized(functionBody), cloneUninitialized(writeYieldValueNode), cloneUninitialized(readYieldResultNode), cloneUninitialized(writeAsyncContext));
        } else {
            AsyncGeneratorRootNode generatorRoot = (AsyncGeneratorRootNode) resumeTarget.getRootNode();
            return create(context, cloneUninitialized(generatorRoot.functionBody), cloneUninitialized(generatorRoot.writeYieldValue), cloneUninitialized(generatorRoot.readYieldResult),
                            cloneUninitialized(writeAsyncContext));
        }
    }
}
