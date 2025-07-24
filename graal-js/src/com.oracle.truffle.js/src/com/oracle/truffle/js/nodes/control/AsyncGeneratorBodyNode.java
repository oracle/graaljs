/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.function.AbstractFunctionRootNode;
import com.oracle.truffle.js.nodes.function.FunctionBodyNode;
import com.oracle.truffle.js.nodes.function.SpecializedNewObjectNode;
import com.oracle.truffle.js.nodes.promise.AsyncRootNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSAsyncGenerator;
import com.oracle.truffle.js.runtime.builtins.JSAsyncGeneratorObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunction.AsyncGeneratorState;
import com.oracle.truffle.js.runtime.objects.AsyncGeneratorRequest;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class AsyncGeneratorBodyNode extends JavaScriptNode {

    private static final class AsyncGeneratorRootNode extends AbstractFunctionRootNode implements AsyncRootNode {

        @Child private JavaScriptNode functionBody;
        @Child private JSWriteFrameSlotNode writeYieldValue;
        @Child private JSReadFrameSlotNode readYieldResult;
        @Child private JSReadFrameSlotNode readAsyncContext;
        @Child private AsyncGeneratorResolveNode asyncGeneratorResolveNode;
        @Child private AsyncGeneratorRejectNode asyncGeneratorRejectNode;
        @Child private AsyncGeneratorResumeNextNode asyncGeneratorResumeNextNode;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
        private final JSContext context;
        private final TruffleString functionName;

        AsyncGeneratorRootNode(JSContext context, JavaScriptNode functionBody, JSWriteFrameSlotNode writeYieldValueNode, JSReadFrameSlotNode readYieldResultNode, JSReadFrameSlotNode readAsyncContext,
                        SourceSection functionSourceSection, TruffleString functionName, ScriptOrModule activeScriptOrModule) {
            super(context.getLanguage(), functionSourceSection, null, activeScriptOrModule);
            this.readAsyncContext = readAsyncContext;
            this.functionName = functionName;
            this.functionBody = new FunctionBodyNode(functionBody);
            this.writeYieldValue = writeYieldValueNode;
            this.readYieldResult = readYieldResultNode;
            this.context = context;
            this.asyncGeneratorResolveNode = AsyncGeneratorResolveNode.create(context);
            this.asyncGeneratorResumeNextNode = AsyncGeneratorResumeNextNode.createTailCall(context);
        }

        @Override
        protected Object executeInRealm(VirtualFrame frame) {
            Object[] arguments = frame.getArguments();
            VirtualFrame generatorFrame = JSArguments.getResumeExecutionContext(arguments);
            JSAsyncGeneratorObject generatorObject = (JSAsyncGeneratorObject) JSArguments.getResumeGeneratorOrPromiseCapability(arguments);
            Completion completion = JSArguments.getResumeCompletion(arguments);

            final JSRealm currentRealm = getRealm();
            final JSRealm realm;
            final boolean enterContext;
            if (context.neverCreatedChildRealms()) {
                // fast path: if there are no child realms we are guaranteedly in the right realm
                assert currentRealm == JSFunction.getRealm(JSFrameUtil.getFunctionObject(generatorFrame));
                realm = currentRealm;
                enterContext = false;
            } else {
                // must enter function context if realm != currentRealm
                realm = JSFunction.getRealm(JSFrameUtil.getFunctionObject(generatorFrame));
                enterContext = realm != currentRealm;
            }
            Object prev = null;
            TruffleContext childContext = null;

            if (enterContext) {
                childContext = realm.getTruffleContext();
                prev = childContext.enter(this);
            }

            try {
                for (;;) {
                    // State must be Executing when called from AsyncGeneratorResumeNext.
                    // State can be Executing or SuspendedYield when resuming from Await.
                    assert generatorObject.getAsyncGeneratorState() == AsyncGeneratorState.Executing ||
                                    generatorObject.getAsyncGeneratorState() == AsyncGeneratorState.SuspendedYield;
                    writeYieldValue.executeWrite(generatorFrame, completion);

                    try {
                        Object result = functionBody.execute(generatorFrame);
                        generatorObject.setAsyncGeneratorState(AsyncGeneratorState.Completed);
                        asyncGeneratorResolveNode.performResolve(generatorObject, result, true);
                    } catch (YieldException e) {
                        if (e.isYield()) {
                            generatorObject.setAsyncGeneratorState(AsyncGeneratorState.SuspendedYield);
                            asyncGeneratorResolveNode.performResolve(generatorObject, e.getResult(), false);
                        } else {
                            assert e.isAwait();
                            return Undefined.instance;
                        }
                    } catch (AbstractTruffleException e) {
                        asyncGeneratorReject(generatorObject, e);
                    }
                    // AsyncGeneratorResolve/AsyncGeneratorReject => AsyncGeneratorResumeNext
                    Object nextCompletion = asyncGeneratorResumeNextNode.execute(generatorObject);
                    if (nextCompletion instanceof Completion) {
                        completion = (Completion) nextCompletion;
                        continue; // tail call from AsyncGeneratorResumeNext
                    } else {
                        return Undefined.instance;
                    }
                }
            } finally {
                if (enterContext) {
                    childContext.leave(this, prev);
                }
            }
        }

        private void asyncGeneratorReject(JSAsyncGeneratorObject generatorObject, AbstractTruffleException ex) {
            if (getErrorObjectNode == null || asyncGeneratorRejectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
                asyncGeneratorRejectNode = insert(AsyncGeneratorRejectNode.create());
            }
            generatorObject.setAsyncGeneratorState(AsyncGeneratorState.Completed);
            Object reason = getErrorObjectNode.execute(ex);
            asyncGeneratorRejectNode.performReject(generatorObject, reason);
        }

        @Override
        public boolean isResumption() {
            return true;
        }

        @Override
        public String getName() {
            if (functionName != null && !functionName.isEmpty()) {
                return Strings.toJavaString(functionName);
            }
            return ":asyncgenerator";
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public JSDynamicObject getAsyncFunctionPromise(Frame asyncFrame, Object generatorObject) {
            Object[] initialState = (Object[]) readAsyncContext.execute((VirtualFrame) asyncFrame);
            assert ((RootCallTarget) initialState[AsyncRootNode.CALL_TARGET_INDEX]).getRootNode() == this;
            assert initialState[AsyncRootNode.GENERATOR_OBJECT_OR_PROMISE_CAPABILITY_INDEX] == generatorObject;
            Object queue;
            if (generatorObject instanceof JSAsyncGeneratorObject asyncGeneratorObject &&
                            (queue = asyncGeneratorObject.getAsyncGeneratorQueue()) instanceof ArrayDeque<?> && ((ArrayDeque<?>) queue).size() == 1) {
                AsyncGeneratorRequest request = (AsyncGeneratorRequest) ((ArrayDeque<?>) queue).peekFirst();
                return request.getPromiseCapability().getPromise();
            } else {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        public List<TruffleStackTraceElement> getSavedStackTrace(Frame asyncFrame) {
            Object[] initialState = (Object[]) readAsyncContext.execute((VirtualFrame) asyncFrame);
            return (List<TruffleStackTraceElement>) initialState[AsyncRootNode.STACK_TRACE_INDEX];
        }

        @Override
        protected List<TruffleStackTraceElement> findAsynchronousFrames(Frame frame) {
            if (!context.isOptionAsyncStackTraces() || context.getLanguage().getAsyncStackDepth() == 0) {
                return null;
            }

            VirtualFrame asyncFrame;
            if (frame.getFrameDescriptor() == getFrameDescriptor()) {
                asyncFrame = JSArguments.getResumeExecutionContext(frame.getArguments());
            } else {
                asyncFrame = (VirtualFrame) ScopeFrameNode.getNonBlockScopeParentFrame(frame);
            }

            return getSavedStackTrace(asyncFrame);
        }
    }

    @Child private SpecializedNewObjectNode createAsyncGeneratorObject;

    private final AsyncGeneratorRootNode resumptionRootNode;
    private final JSContext context;

    @Child private JSWriteFrameSlotNode writeAsyncContext;

    public AsyncGeneratorBodyNode(JSContext context, JSWriteFrameSlotNode writeAsyncContext, AsyncGeneratorRootNode resumptionRootNode) {
        this.createAsyncGeneratorObject = SpecializedNewObjectNode.create(context, false, true, true, true, JSAsyncGenerator.INSTANCE);
        this.context = context;
        this.writeAsyncContext = writeAsyncContext;
        this.resumptionRootNode = resumptionRootNode;
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode writeYieldValueNode, JSReadFrameSlotNode readYieldResultNode,
                    JSWriteFrameSlotNode writeAsyncContext, JSReadFrameSlotNode readAsyncContext,
                    SourceSection functionSourceSection, TruffleString functionName, ScriptOrModule activeScriptOrModule) {
        var resumptionRootNode = new AsyncGeneratorRootNode(context, body, writeYieldValueNode, readYieldResultNode, readAsyncContext, functionSourceSection, functionName, activeScriptOrModule);
        return new AsyncGeneratorBodyNode(context, writeAsyncContext, resumptionRootNode);
    }

    private void asyncGeneratorStart(VirtualFrame frame, JSAsyncGeneratorObject generatorObject) {
        MaterializedFrame materializedFrame = frame.materialize();
        CallTarget resumeTarget = resumptionRootNode.getCallTarget();
        generatorObject.setAsyncGeneratorState(AsyncGeneratorState.SuspendedStart);
        generatorObject.setAsyncGeneratorContext(materializedFrame);
        generatorObject.setAsyncGeneratorTarget(resumeTarget);
        generatorObject.setAsyncGeneratorQueue(new ArrayDeque<>(4));
        writeAsyncContext.executeWrite(frame, AsyncRootNode.createAsyncContext(resumeTarget, generatorObject, materializedFrame));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        JSAsyncGeneratorObject generatorObject = (JSAsyncGeneratorObject) createAsyncGeneratorObject.execute(frame, JSFrameUtil.getFunctionObject(frame));

        asyncGeneratorStart(frame, generatorObject);

        return generatorObject;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new AsyncGeneratorBodyNode(context,
                        cloneUninitialized(writeAsyncContext, materializedTags),
                        resumptionRootNode);
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (!materializedTags.isEmpty()) {
            // ensure resumption call target is visible to instrumentation.
            resumptionRootNode.getCallTarget();
        }
        return this;
    }
}
