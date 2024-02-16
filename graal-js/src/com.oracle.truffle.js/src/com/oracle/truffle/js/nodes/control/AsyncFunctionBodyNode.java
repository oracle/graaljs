/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.function.AbstractFunctionRootNode;
import com.oracle.truffle.js.nodes.function.FunctionBodyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.promise.AsyncRootNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class AsyncFunctionBodyNode extends JavaScriptNode {

    public static final class AsyncFunctionRootNode extends AbstractFunctionRootNode implements AsyncRootNode {

        private final JSContext context;
        private final TruffleString functionName;
        @Child private JavaScriptNode functionBody;
        @Child private JSReadFrameSlotNode readAsyncContext;
        @Child private JSWriteFrameSlotNode writeAsyncResult;
        @Child private JSFunctionCallNode callResolveNode;
        @Child private JSFunctionCallNode callRejectNode;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

        AsyncFunctionRootNode(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode asyncResult, JSReadFrameSlotNode readAsyncContext,
                        SourceSection functionSourceSection, TruffleString functionName, ScriptOrModule activeScriptOrModule) {
            super(context.getLanguage(), functionSourceSection, null, activeScriptOrModule);
            this.context = context;
            this.functionBody = new FunctionBodyNode(body);
            this.readAsyncContext = readAsyncContext;
            this.writeAsyncResult = asyncResult;
            this.callResolveNode = JSFunctionCallNode.createCall();
            this.functionName = functionName;
            JavaScriptNode.transferSourceSection(body, functionBody);
        }

        @Override
        protected Object executeInRealm(VirtualFrame frame) {
            Object[] arguments = frame.getArguments();
            VirtualFrame asyncFrame = JSArguments.getResumeExecutionContext(arguments);
            PromiseCapabilityRecord promiseCapability = (PromiseCapabilityRecord) JSArguments.getResumeGeneratorOrPromiseCapability(arguments);
            Completion resumptionValue = JSArguments.getResumeCompletion(arguments);
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
                promiseCapabilityResolve(promiseCapability, result);
            } catch (YieldException e) {
                assert e.isAwait();
                // no-op: we called await, so we will resume later.
            } catch (AbstractTruffleException e) {
                promiseCapabilityReject(promiseCapability, e);
            } finally {
                if (enterContext) {
                    childContext.leave(this, prev);
                }
            }
            // The result is undefined for normal completion.
            return Undefined.instance;
        }

        private void promiseCapabilityResolve(PromiseCapabilityRecord promiseCapability, Object result) {
            callResolveNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), result));
        }

        private void promiseCapabilityReject(PromiseCapabilityRecord promiseCapability, AbstractTruffleException e) {
            if (getErrorObjectNode == null || callRejectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
                callRejectNode = insert(JSFunctionCallNode.createCall());
            }
            Object error = getErrorObjectNode.execute(e);
            callRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), error));
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
            return ":async";
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public JSDynamicObject getAsyncFunctionPromise(Frame asyncFrame, Object promiseCapability) {
            Object[] initialState = (Object[]) readAsyncContext.execute((VirtualFrame) asyncFrame);
            assert ((RootCallTarget) initialState[AsyncRootNode.CALL_TARGET_INDEX]).getRootNode() == this;
            assert promiseCapability == initialState[AsyncRootNode.GENERATOR_OBJECT_OR_PROMISE_CAPABILITY_INDEX];
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
            if (frame.getFrameDescriptor() == getFrameDescriptor()) {
                asyncFrame = JSArguments.getResumeExecutionContext(frame.getArguments());
            } else {
                asyncFrame = (VirtualFrame) ScopeFrameNode.getNonBlockScopeParentFrame(frame);
            }

            return getSavedStackTrace(asyncFrame);
        }
    }

    private final JSContext context;
    @Child private JSWriteFrameSlotNode writeAsyncContext;
    @Child private NewPromiseCapabilityNode newPromiseCapability;
    private final AsyncFunctionRootNode resumptionRootNode;
    @Child private volatile DirectCallNode asyncCallNode;

    public AsyncFunctionBodyNode(JSContext context, JSWriteFrameSlotNode writeAsyncContext, AsyncFunctionRootNode resumptionRootNode) {
        this.context = context;
        this.writeAsyncContext = writeAsyncContext;
        this.resumptionRootNode = resumptionRootNode;
        this.newPromiseCapability = NewPromiseCapabilityNode.create(context);
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode writeAsyncContext, JSReadFrameSlotNode readAsyncContext, JSWriteFrameSlotNode writeAsyncResult,
                    SourceSection functionSourceSection, TruffleString functionName, ScriptOrModule activeScriptOrModule) {
        AsyncFunctionRootNode resumptionRootNode = new AsyncFunctionRootNode(context, body, writeAsyncResult, readAsyncContext, functionSourceSection, functionName, activeScriptOrModule);
        return new AsyncFunctionBodyNode(context, writeAsyncContext, resumptionRootNode);
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
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.asyncCallNode = insert(DirectCallNode.create(resumptionRootNode.getCallTarget()));
    }

    private void ensureAsyncCallTargetInitialized() {
        if (asyncCallNode == null) {
            initializeAsyncCallTarget();
        }
    }

    private void asyncFunctionStart(VirtualFrame frame, PromiseCapabilityRecord promiseCapability) {
        MaterializedFrame materializedFrame = frame.materialize();
        ensureAsyncCallTargetInitialized();
        writeAsyncContext.executeWrite(frame, AsyncRootNode.createAsyncContext(asyncCallNode.getCallTarget(), promiseCapability, materializedFrame));
        Object unusedInitialResult = null;
        asyncCallNode.call(JSArguments.createResumeArguments(materializedFrame, promiseCapability, Completion.Type.Normal, unusedInitialResult));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        PromiseCapabilityRecord promiseCapability = newPromiseCapability.executeDefault();

        asyncFunctionStart(frame, promiseCapability);

        return promiseCapability.getPromise();
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new AsyncFunctionBodyNode(getContext(),
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
