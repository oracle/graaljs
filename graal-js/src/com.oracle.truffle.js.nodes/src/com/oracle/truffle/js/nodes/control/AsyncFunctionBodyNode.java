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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class AsyncFunctionBodyNode extends JavaScriptNode {

    @NodeInfo(cost = NodeCost.NONE, language = "JavaScript", description = "The root node of async functions in JavaScript.")
    private static final class AsyncFunctionRootNode extends JavaScriptRootNode {

        @Child private JavaScriptNode functionBody;
        @Child private JSWriteFrameSlotNode writeAsyncResult;
        @Child private PropertyGetNode getPromiseResolve;
        @Child private PropertyGetNode getPromiseReject;
        @Child private JSFunctionCallNode executePromiseMethod;

        AsyncFunctionRootNode(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode asyncResult, SourceSection functionSourceSection) {
            super(context.getLanguage(), functionSourceSection, null);
            this.functionBody = body;
            this.writeAsyncResult = asyncResult;
            this.getPromiseResolve = PropertyGetNode.create("resolve", false, context);
            this.getPromiseReject = PropertyGetNode.create("reject", false, context);
            this.executePromiseMethod = JSFunctionCallNode.createCall();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            VirtualFrame asyncFrame = JSFrameUtil.castMaterializedFrame(frame.getArguments()[0]);
            DynamicObject promiseCapability = (DynamicObject) frame.getArguments()[1];
            Completion resumptionValue = (Completion) frame.getArguments()[2];
            writeAsyncResult.executeWrite(asyncFrame, resumptionValue);
            try {
                Object result = functionBody.execute(asyncFrame);
                promiseCapabilityResolve(executePromiseMethod, getPromiseResolve, promiseCapability, result);
            } catch (YieldException e) {
                assert e.isAwait();
                // no-op: we called await, so we will resume later.
            } catch (GraalJSException error) {
                Object result = error.getErrorObjectEager(getPromiseReject.getContext());
                promiseCapabilityReject(executePromiseMethod, getPromiseReject, promiseCapability, result);
            }
            // The result is undefined for normal completion.
            return Undefined.instance;
        }
    }

    @Child private JavaScriptNode parameterInit;
    @Child private JavaScriptNode functionBody;
    @Child private JSWriteFrameSlotNode writeAsyncContext;
    @Child private JSWriteFrameSlotNode writeAsyncResult;
    @Child private PropertyGetNode getPromise;
    @Child private PropertyGetNode getPromiseReject;
    @Child private JSFunctionCallNode createPromiseCapability;
    @Child private JSFunctionCallNode executePromiseMethod;
    @Child private PropertySetNode setAsyncContext;

    @CompilationFinal CallTarget resumptionTarget;
    @CompilationFinal DirectCallNode asyncCallNode;

    public AsyncFunctionBodyNode(JSContext context, JavaScriptNode parameterInit, JavaScriptNode body, JSWriteFrameSlotNode asyncContext, JSWriteFrameSlotNode asyncResult) {
        this.functionBody = body;
        this.parameterInit = parameterInit;
        this.writeAsyncContext = asyncContext;
        this.writeAsyncResult = asyncResult;
        this.getPromise = PropertyGetNode.create("promise", false, context);
        this.getPromiseReject = PropertyGetNode.create("reject", false, context);
        this.createPromiseCapability = JSFunctionCallNode.createCall();
        this.executePromiseMethod = JSFunctionCallNode.createCall();
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode parameterInit, JavaScriptNode body, JSWriteFrameSlotNode asyncVar, JSWriteFrameSlotNode asyncResult) {
        return new AsyncFunctionBodyNode(context, parameterInit, body, asyncVar, asyncResult);
    }

    private JSContext getContext() {
        return getPromise.getContext();
    }

    private void initializeAsyncCallTarget() {
        CompilerAsserts.neverPartOfCompilation();
        atomic(() -> {
            AsyncFunctionRootNode asyncRootNode = new AsyncFunctionRootNode(getContext(), functionBody, writeAsyncResult, getRootNode().getSourceSection());
            this.resumptionTarget = Truffle.getRuntime().createCallTarget(asyncRootNode);
            this.asyncCallNode = insert(DirectCallNode.create(resumptionTarget));
            // these children have been transferred to the async root node and are now disowned
            this.functionBody = null;
            this.writeAsyncResult = null;
        });
    }

    private void ensureAsyncCallTargetInitialized() {
        if (resumptionTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            initializeAsyncCallTarget();
        }
    }

    private void asyncFunctionStart(VirtualFrame frame, DynamicObject promiseCapability) {
        writeAsyncContext.executeWrite(frame, new Object[]{resumptionTarget, promiseCapability, frame.materialize()});
        Completion unusedInitialResult = null;
        asyncCallNode.call(new Object[]{frame.materialize(), promiseCapability, unusedInitialResult});
    }

    @Override
    public Object execute(VirtualFrame frame) {
        DynamicObject promiseCapability = createPromiseCapability();

        if (parameterInit != null) {
            try {
                parameterInit.execute(frame);
            } catch (GraalJSException error) {
                JSContext context = getPromiseReject.getContext();
                promiseCapabilityReject(executePromiseMethod, getPromiseReject, promiseCapability, error.getErrorObjectEager(context));

                return getPromise.getValue(promiseCapability);
            }
        }

        ensureAsyncCallTargetInitialized();
        asyncFunctionStart(frame, promiseCapability);

        return getPromise.getValue(promiseCapability);
    }

    private static void promiseCapabilityResolve(JSFunctionCallNode promiseCallNode, PropertyGetNode resolveNode, DynamicObject promiseCapability, Object result) {
        DynamicObject resolve = (DynamicObject) resolveNode.getValue(promiseCapability);
        promiseCallNode.executeCall(JSArguments.create(Undefined.instance, resolve, new Object[]{result}));
    }

    private static void promiseCapabilityReject(JSFunctionCallNode promiseCallNode, PropertyGetNode rejectNode, DynamicObject promiseCapability, Object result) {
        DynamicObject reject = (DynamicObject) rejectNode.getValue(promiseCapability);
        promiseCallNode.executeCall(JSArguments.create(Undefined.instance, reject, new Object[]{result}));
    }

    private DynamicObject createPromiseCapability() {
        return (DynamicObject) createPromiseCapability.executeCall(JSArguments.createZeroArg(Undefined.instance, getContext().getRealm().getAsyncFunctionPromiseCapabilityConstructor()));
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        if (resumptionTarget == null) {
            return create(getContext(), cloneUninitialized(parameterInit), cloneUninitialized(functionBody), cloneUninitialized(writeAsyncContext), cloneUninitialized(writeAsyncResult));
        } else {
            AsyncFunctionRootNode asyncFunctionRoot = (AsyncFunctionRootNode) ((RootCallTarget) resumptionTarget).getRootNode();
            return create(getContext(), cloneUninitialized(parameterInit), cloneUninitialized(asyncFunctionRoot.functionBody), cloneUninitialized(writeAsyncContext),
                            cloneUninitialized(asyncFunctionRoot.writeAsyncResult));
        }
    }

}
