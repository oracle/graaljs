/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.function.ConstructorRootNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;

/**
 * This node can be used to create a callable 'constructor' function for self-hosted internal
 * JavaScript builtins.
 */
@NodeChildren({@NodeChild(value = "function")})
public abstract class JSToConstructorFunctionNode extends JavaScriptNode {

    private final JSContext context;

    protected JSToConstructorFunctionNode(JSContext context) {
        this.context = context;
    }

    public static JSToConstructorFunctionNode create(JSContext context, JavaScriptNode argument) {
        return JSToConstructorFunctionNodeGen.create(context, argument);
    }

    /**
     * Create a new {@link JSFunction} that can be called as constructor using 'new'.
     *
     * @param function The function to be converted to constructor.
     * @return A new function instance.
     */
    @TruffleBoundary
    @Specialization(guards = {"isJSFunction(function)"})
    protected Object doOther(Object function) {
        DynamicObject toConvertFun = (DynamicObject) function;
        CallTarget callTarget = JSFunction.getCallTarget(toConvertFun);
        int length = JSFunction.getLength(toConvertFun);
        String name = JSFunction.getName(toConvertFun);
        CallTarget newCallTarget = Truffle.getRuntime().createCallTarget(
                        NodeFactory.getInstance(context).createConstructorRequiresNewRoot(context, ((RootCallTarget) callTarget).getRootNode().getSourceSection()));
        JSFunctionData newFunctionData = JSFunctionData.create(context, newCallTarget, null, null, length, name,
                        true, false, JSFunction.isStrict(toConvertFun), false, JSFunction.needsParentFrame(toConvertFun), false, false, false, true, false, false);
        CallTarget newConstructTarget = Truffle.getRuntime().createCallTarget(ConstructorRootNode.create(newFunctionData, callTarget, false));
        newFunctionData.setConstructTarget(newConstructTarget);
        // Make the CallTarget extensible from ECMA6 classes
        CallTarget newTargetCallTarget = Truffle.getRuntime().createCallTarget(
                        ConstructorRootNode.create(newFunctionData, Truffle.getRuntime().createCallTarget(NodeFactory.getDefaultInstance().createDropNewTarget(callTarget)), true));
        newFunctionData.setConstructNewTarget(newTargetCallTarget);
        return JSFunction.create(JSFunction.getRealm(toConvertFun), newFunctionData, JSFunction.getEnclosingFrame(toConvertFun));
    }

    abstract JavaScriptNode getFunction();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(context, cloneUninitialized(getFunction()));
    }
}
