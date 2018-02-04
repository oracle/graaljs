/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.access.IsPrimitiveNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.function.JSNewNode.SpecializedNewObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class ConstructorRootNode extends JavaScriptRootNode {
    private final JSFunctionData functionData;
    private final CallTarget callTarget;

    @Child private DirectCallNode callNode;
    @Child private JSTargetableNode newObjectNode;
    @Child private IsPrimitiveNode isPrimitiveNode;
    private final ConditionProfile isObject = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isNotUndefined = ConditionProfile.createBinaryProfile();
    private final boolean newTarget;

    protected ConstructorRootNode(JSFunctionData functionData, CallTarget callTarget, boolean newTarget) {
        super(functionData.getContext().getLanguage(), ((RootCallTarget) callTarget).getRootNode().getSourceSection(), null);
        this.functionData = functionData;
        this.callTarget = callTarget;
        this.newTarget = newTarget;
    }

    public static ConstructorRootNode create(JSFunctionData functionData, CallTarget callTarget, boolean newTarget) {
        return new ConstructorRootNode(functionData, callTarget, newTarget);
    }

    private Object allocateThisObject(VirtualFrame frame, Object[] arguments) {
        Object functionObject = newTarget ? arguments[2] : arguments[1];
        Object thisObject = newObjectNode.executeWithTarget(frame, functionObject);
        arguments[0] = thisObject;
        return thisObject;
    }

    private Object filterConstructorResult(Object thisObject, Object result) {
        // IsObject is replaced with a !IsPrimitive check for JavaInterop,
        // so that non-primitive Java types can be returned from the constructor, too.
        if (isObject.profile(!isPrimitiveNode.executeBoolean(result))) {
            return result;
        }
        // If [[ConstructorKind]] == "base" or result is undefined return this, otherwise throw
        if (getFunctionData().isDerived() && isNotUndefined.profile(result != Undefined.instance)) {
            throw Errors.createTypeError("constructor result not as expected").setRealm(functionData.getContext().getRealm());
        }
        assert thisObject != JSFunction.CONSTRUCT;
        return thisObject;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (callNode == null || newObjectNode == null || isPrimitiveNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            initialize();
        }
        Object[] arguments = frame.getArguments();
        Object thisObject = allocateThisObject(frame, arguments);
        Object result = callNode.call(arguments);
        return filterConstructorResult(thisObject, result);
    }

    private void initialize() {
        this.callNode = insert(Truffle.getRuntime().createDirectCallNode(callTarget));
        this.newObjectNode = insert(SpecializedNewObjectNode.create(functionData.getContext(), functionData.isBuiltin(), functionData.isConstructor(), functionData.isGenerator(), null));
        this.isPrimitiveNode = insert(IsPrimitiveNode.create());
    }

    private JSFunctionData getFunctionData() {
        return functionData;
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }

    @Override
    protected boolean isCloneUninitializedSupported() {
        return true;
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    protected JavaScriptRootNode cloneUninitialized() {
        return new ConstructorRootNode(functionData, callTarget, newTarget);
    }

    @Override
    @TruffleBoundary
    public String toString() {
        String callTargetName = ((RootCallTarget) callTarget).getRootNode().toString();
        return JSTruffleOptions.DetailedCallTargetNames ? JSRuntime.stringConcat("[Construct]", callTargetName) : callTargetName;
    }
}
