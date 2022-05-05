/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.AsyncRootNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class TopLevelAwaitModuleBodyNode extends JavaScriptNode {

    @NodeInfo(cost = NodeCost.NONE, language = "JavaScript")
    public static final class TopLevelAwaitModuleRootNode extends JavaScriptRootNode {

        private final JSContext context;
        private final String functionName;

        @Child private JavaScriptNode functionBody;
        @Child private JSFunctionCallNode callResolveNode;
        @Child private JSFunctionCallNode callRejectNode;
        @Child private JSWriteFrameSlotNode writeAsyncResult;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

        TopLevelAwaitModuleRootNode(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode asyncResult, SourceSection functionSourceSection, String functionName) {
            super(context.getLanguage(), functionSourceSection, null);
            this.context = context;
            this.functionBody = body;
            this.callResolveNode = JSFunctionCallNode.createCall();
            this.functionName = functionName;
            this.writeAsyncResult = asyncResult;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] arguments = frame.getArguments();
            VirtualFrame asyncFrame = JSArguments.getResumeExecutionContext(arguments);
            PromiseCapabilityRecord promiseCapability = (PromiseCapabilityRecord) JSArguments.getResumeGeneratorOrPromiseCapability(arguments);
            Completion resumptionValue = JSArguments.getResumeCompletion(arguments);
            writeAsyncResult.executeWrite(asyncFrame, resumptionValue);

            JSModuleRecord moduleRecord = (JSModuleRecord) JSArguments.getUserArgument(asyncFrame.getArguments(), 0);
            try {
                Object returnValue = functionBody.execute(asyncFrame);

                assert promiseCapability != null;
                promiseCapabilityResolve(promiseCapability, returnValue);
            } catch (YieldException e) {
                assert promiseCapability == null ? e.isYield() : e.isAwait();
                if (e.isYield()) {
                    moduleRecord.setEnvironment(JSFrameUtil.castMaterializedFrame(asyncFrame));
                } else {
                    assert e.isAwait();
                    // no-op: we called await, so we will resume later.
                }
            } catch (AbstractTruffleException e) {
                if (promiseCapability != null) {
                    promiseCapabilityReject(promiseCapability, e);
                } else {
                    throw e;
                }
            }
            // The result is undefined for normal completion.
            return Undefined.instance;
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
            return ":top-level-await-module";
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
            Object result = getErrorObjectNode.execute(e);
            callRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), result));
        }
    }

    private final JSContext context;

    @CompilationFinal private volatile CallTarget resumptionTarget;

    @Child private JavaScriptNode moduleBodyNode;
    @Child private JSWriteFrameSlotNode writeAsyncResult;
    @Child private volatile DirectCallNode asyncCallNode;
    @Child private JSWriteFrameSlotNode writeAsyncContextNode;

    private TopLevelAwaitModuleBodyNode(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode asyncResult, JSWriteFrameSlotNode writeAsyncContextNode) {
        this.context = context;
        this.moduleBodyNode = body;
        this.writeAsyncContextNode = writeAsyncContextNode;
        this.writeAsyncResult = asyncResult;
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode asyncContext, JSWriteFrameSlotNode writeAsyncContextNode) {
        return new TopLevelAwaitModuleBodyNode(context, body, asyncContext, writeAsyncContextNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] arguments = frame.getArguments();
        JSModuleRecord moduleRecord = (JSModuleRecord) JSArguments.getUserArgument(arguments, 0);
        MaterializedFrame moduleFrame = moduleRecord.getEnvironment() != null ? JSFrameUtil.castMaterializedFrame(moduleRecord.getEnvironment()) : frame.materialize();
        PromiseCapabilityRecord promiseCapability = (JSArguments.getUserArgumentCount(arguments) >= 2 ? (PromiseCapabilityRecord) JSArguments.getUserArgument(arguments, 1) : null);
        ensureAsyncCallTargetInitialized();
        if (promiseCapability != null) {
            writeAsyncContextNode.executeWrite(moduleFrame, AsyncRootNode.createAsyncContext(resumptionTarget, promiseCapability, moduleFrame));
        }
        Object unusedInitialResult = null;
        asyncCallNode.call(JSArguments.createResumeArguments(moduleFrame, promiseCapability, Completion.Type.Normal, unusedInitialResult));
        if (promiseCapability == null) {
            // no capability provided: we are initializing the module.
            return Undefined.instance;
        } else {
            // capability was provided: we are executing the module as an async function.
            return promiseCapability.getPromise();
        }
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

    private void initializeAsyncCallTarget() {
        CompilerAsserts.neverPartOfCompilation();
        atomic(() -> {
            if (asyncCallTargetInitializationRequired()) {
                TopLevelAwaitModuleRootNode asyncRootNode = new TopLevelAwaitModuleRootNode(context, moduleBodyNode, writeAsyncResult, getRootNode().getSourceSection(), "");
                this.resumptionTarget = asyncRootNode.getCallTarget();
                this.asyncCallNode = insert(DirectCallNode.create(resumptionTarget));
                // these children have been transferred to the async root node and are now disowned
                this.moduleBodyNode = null;
                this.writeAsyncResult = null;
            }
        });
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(context, cloneUninitialized(moduleBodyNode, materializedTags), cloneUninitialized(writeAsyncResult, materializedTags),
                        cloneUninitialized(writeAsyncContextNode, materializedTags));
    }

}
