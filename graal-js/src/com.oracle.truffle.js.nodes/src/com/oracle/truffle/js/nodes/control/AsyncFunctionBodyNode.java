/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class AsyncFunctionBodyNode extends JavaScriptNode {

    @NodeInfo(cost = NodeCost.NONE, language = "JavaScript", description = "The root node of async functions in JavaScript.")
    private static final class AsyncFunctionRootNode extends JavaScriptRealmBoundaryRootNode {

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
        protected Object executeAndSetRealm(VirtualFrame frame) {
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

        @Override
        protected JSRealm getRealm() {
            return getPromiseReject.getContext().getRealm();
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
