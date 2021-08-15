/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.promise.AsyncRootNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class AsyncFunctionBodyNode extends JavaScriptNode {

    @NodeInfo(cost = NodeCost.NONE, language = "JavaScript", description = "The root node of async functions in JavaScript.")
    public static final class AsyncFunctionRootNode extends JavaScriptRootNode implements AsyncRootNode {

        private static final int ASYNC_FRAME_ARG_INDEX = 0;

        private final JSContext context;
        private final String functionName;
        @Child private JavaScriptNode functionBody;
        @Child private JSReadFrameSlotNode readAsyncContext;
        @Child private JSWriteFrameSlotNode writeAsyncResult;
        @Child private JSFunctionCallNode callResolveNode;
        @Child private JSFunctionCallNode callRejectNode;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
        @Child private InteropLibrary exceptions;

        AsyncFunctionRootNode(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode asyncResult, JSReadFrameSlotNode readAsyncContext, SourceSection functionSourceSection,
                        String functionName) {
            super(context.getLanguage(), functionSourceSection, null);
            this.context = context;
            this.functionBody = body;
            this.readAsyncContext = readAsyncContext;
            this.writeAsyncResult = asyncResult;
            this.callResolveNode = JSFunctionCallNode.createCall();
            this.functionName = functionName;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            VirtualFrame asyncFrame = JSFrameUtil.castMaterializedFrame(args[ASYNC_FRAME_ARG_INDEX]);
            PromiseCapabilityRecord promiseCapability = (PromiseCapabilityRecord) args[1];
            Completion resumptionValue = (Completion) args[2];
            writeAsyncResult.executeWrite(asyncFrame, resumptionValue);

            final JSRealm currentRealm = getRealm();
            final JSRealm realm;
            final boolean enterContext;
            if (context.neverCreatedChildRealms()) {
                // fast path: if there are no child realms we are guaranteedly in the right realm
                assert currentRealm == JSFunction.getRealm(JSFrameUtil.getFunctionObject(asyncFrame));
                realm = currentRealm;
                enterContext = false;
            } else {
                // must enter function context if realm != currentRealm
                realm = JSFunction.getRealm(JSFrameUtil.getFunctionObject(asyncFrame));
                enterContext = realm != currentRealm;
            }
            Object prev = null;
            TruffleContext childContext = null;

            if (enterContext) {
                childContext = realm.getTruffleContext();
                prev = childContext.enter(this);
            }

            try {
                Object result = functionBody.execute(asyncFrame);
                promiseCapabilityResolve(callResolveNode, promiseCapability, result);
            } catch (YieldException e) {
                assert e.isAwait();
                // no-op: we called await, so we will resume later.
            } catch (Throwable e) {
                if (shouldCatch(e)) {
                    promiseCapabilityReject(callRejectNode, promiseCapability, getErrorObjectNode.execute(e));
                } else {
                    throw e;
                }
            } finally {
                if (enterContext) {
                    childContext.leave(this, prev);
                }
            }
            // The result is undefined for normal completion.
            return Undefined.instance;
        }

        private boolean shouldCatch(Throwable exception) {
            if (getErrorObjectNode == null || callRejectNode == null || exceptions == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
                callRejectNode = insert(JSFunctionCallNode.createCall());
                exceptions = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
            }
            return TryCatchNode.shouldCatch(exception, exceptions);
        }

        @Override
        public boolean isResumption() {
            return true;
        }

        @Override
        public String getName() {
            if (functionName != null && !functionName.isEmpty()) {
                return functionName;
            }
            return ":async";
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public DynamicObject getAsyncFunctionPromise(Frame asyncFrame) {
            Object[] initialState = (Object[]) readAsyncContext.execute((VirtualFrame) asyncFrame);
            RootCallTarget resumeTarget = (RootCallTarget) initialState[AsyncRootNode.CALL_TARGET_INDEX];
            assert resumeTarget.getRootNode() == this;
            Object promiseCapability = initialState[AsyncRootNode.GENERATOR_OBJECT_OR_PROMISE_CAPABILITY_INDEX];
            return ((PromiseCapabilityRecord) promiseCapability).getPromise();
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
            Object frameArg = frame.getArguments()[ASYNC_FRAME_ARG_INDEX];
            if (frameArg instanceof MaterializedFrame) {
                asyncFrame = (MaterializedFrame) frameArg;
            } else {
                asyncFrame = (VirtualFrame) ScopeFrameNode.getNonBlockScopeParentFrame(frame);
            }

            return getSavedStackTrace(asyncFrame);
        }
    }

    private final JSContext context;
    @Child private JavaScriptNode functionBody;
    @Child private JSReadFrameSlotNode readAsyncContext;
    @Child private JSWriteFrameSlotNode writeAsyncContext;
    @Child private JSWriteFrameSlotNode writeAsyncResult;
    @Child private NewPromiseCapabilityNode newPromiseCapability;

    @CompilationFinal private volatile CallTarget resumptionTarget;
    @Child private volatile DirectCallNode asyncCallNode;

    public AsyncFunctionBodyNode(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode writeAsyncContext, JSReadFrameSlotNode readAsyncContext, JSWriteFrameSlotNode writeAsyncResult) {
        this.context = context;
        this.functionBody = body;
        this.readAsyncContext = readAsyncContext;
        this.writeAsyncContext = writeAsyncContext;
        this.writeAsyncResult = writeAsyncResult;
        this.newPromiseCapability = NewPromiseCapabilityNode.create(context);
        transferSourceSection(body, this);
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode writeAsyncContext, JSReadFrameSlotNode readAsyncContext, JSWriteFrameSlotNode writeAsyncResult) {
        return new AsyncFunctionBodyNode(context, body, writeAsyncContext, readAsyncContext, writeAsyncResult);
    }

    private JSContext getContext() {
        return context;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == JSTags.ControlFlowRootTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("type", JSTags.ControlFlowRootTag.Type.AsyncFunction.name());
    }

    private void initializeAsyncCallTarget() {
        CompilerAsserts.neverPartOfCompilation();
        atomic(() -> {
            if (asyncCallTargetInitializationRequired()) {
                RootNode rootNode = getRootNode();
                AsyncFunctionRootNode asyncRootNode = new AsyncFunctionRootNode(getContext(), functionBody, writeAsyncResult, readAsyncContext, rootNode.getSourceSection(), rootNode.getName());
                this.resumptionTarget = Truffle.getRuntime().createCallTarget(asyncRootNode);
                DirectCallNode callNode = DirectCallNode.create(resumptionTarget);
                this.asyncCallNode = insert(callNode);
                // these children have been transferred to the async root node and are now disowned
                this.functionBody = null;
                this.writeAsyncResult = null;
                this.readAsyncContext = null;
            }
        });
    }

    private boolean asyncCallTargetInitializationRequired() {
        return resumptionTarget == null || asyncCallNode == null;
    }

    private void ensureAsyncCallTargetInitialized() {
        if (asyncCallTargetInitializationRequired()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            initializeAsyncCallTarget();
        }
    }

    private void asyncFunctionStart(VirtualFrame frame, PromiseCapabilityRecord promiseCapability) {
        MaterializedFrame materializedFrame = frame.materialize();
        writeAsyncContext.executeWrite(frame, AsyncRootNode.createAsyncContext(resumptionTarget, promiseCapability, materializedFrame));
        Completion unusedInitialResult = null;
        asyncCallNode.call(materializedFrame, promiseCapability, unusedInitialResult);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        PromiseCapabilityRecord promiseCapability = newPromiseCapability.executeDefault();

        ensureAsyncCallTargetInitialized();
        asyncFunctionStart(frame, promiseCapability);

        return promiseCapability.getPromise();
    }

    private static void promiseCapabilityResolve(JSFunctionCallNode promiseCallNode, PromiseCapabilityRecord promiseCapability, Object result) {
        promiseCallNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), result));
    }

    private static void promiseCapabilityReject(JSFunctionCallNode promiseCallNode, PromiseCapabilityRecord promiseCapability, Object result) {
        promiseCallNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), result));
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return atomic(() -> {
            if (resumptionTarget == null) {
                return create(getContext(),
                                cloneUninitialized(functionBody, materializedTags),
                                cloneUninitialized(writeAsyncContext, materializedTags),
                                cloneUninitialized(readAsyncContext, materializedTags),
                                cloneUninitialized(writeAsyncResult, materializedTags));
            } else {
                AsyncFunctionRootNode asyncFunctionRoot = (AsyncFunctionRootNode) ((RootCallTarget) resumptionTarget).getRootNode();
                return create(getContext(),
                                cloneUninitialized(asyncFunctionRoot.functionBody, materializedTags),
                                cloneUninitialized(writeAsyncContext, materializedTags),
                                cloneUninitialized(asyncFunctionRoot.readAsyncContext, materializedTags),
                                cloneUninitialized(asyncFunctionRoot.writeAsyncResult, materializedTags));
            }
        });
    }

}
