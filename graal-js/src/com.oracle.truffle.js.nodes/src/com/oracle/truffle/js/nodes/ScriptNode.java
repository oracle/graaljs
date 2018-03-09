/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;

public final class ScriptNode {

    private final JSContext context;
    private final JSFunctionData functionData;
    private final RootCallTarget callTarget;

    private ScriptNode(JSContext context, JSFunctionData functionData, RootCallTarget callTarget) {
        this.context = context;
        this.functionData = functionData;
        this.callTarget = callTarget;
    }

    public static ScriptNode fromFunctionRoot(JSContext context, FunctionRootNode root) {
        return new ScriptNode(context, root.getFunctionData(), Truffle.getRuntime().createCallTarget(root));
    }

    public static ScriptNode fromFunctionData(JSContext context, JSFunctionData functionData) {
        return new ScriptNode(context, functionData, (RootCallTarget) functionData.getCallTarget());
    }

    public Object run(JSRealm realm) {
        return runWithThisObject(realm, realm.getGlobalObject());
    }

    public Object[] argumentsToRun(JSRealm realm) {
        return argumentsToRunWithThisObject(realm, realm.getGlobalObject());
    }

    public Object runWithThisObject(JSRealm realm, Object thisObj) {
        return run(argumentsToRunWithThisObject(realm, thisObj));
    }

    public Object[] argumentsToRunWithThisObject(JSRealm realm, Object thisObj) {
        return JSArguments.createZeroArg(thisObj, JSFunction.create(realm, functionData));
    }

    public Object run(Object[] args) {
        return callTarget.call(args);
    }

    public JSContext getContext() {
        return context;
    }

    public RootNode getRootNode() {
        return callTarget.getRootNode();
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }

    public JSFunctionData getFunctionData() {
        return functionData;
    }
}
