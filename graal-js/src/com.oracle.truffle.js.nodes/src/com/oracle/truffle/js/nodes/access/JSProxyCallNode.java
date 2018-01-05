/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;

@NodeInfo(cost = NodeCost.NONE)
@ImportStatic({JSProxy.class, JSArguments.class})
public abstract class JSProxyCallNode extends JavaScriptBaseNode {

    private final JSContext context;
    @Child private GetMethodNode trapGetter;
    @Child private JSFunctionCallNode callNode;
    @Child private JSFunctionCallNode callTrapNode;
    protected final boolean isNew;
    protected final boolean isNewTarget;
    private final ConditionProfile callableProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile pxTrapFunProfile = ConditionProfile.createBinaryProfile();

    protected JSProxyCallNode(JSContext context, boolean isNew, boolean isNewTarget) {
        this.callNode = isNewTarget ? JSFunctionCallNode.createNewTarget() : isNew ? JSFunctionCallNode.createNew() : JSFunctionCallNode.createCall();
        this.callTrapNode = isNewTarget || isNew ? JSFunctionCallNode.createCall() : callNode;
        this.trapGetter = GetMethodNode.create(context, null, isNewTarget || isNew ? JSProxy.CONSTRUCT : JSProxy.APPLY);
        this.context = context;
        this.isNew = isNew;
        this.isNewTarget = isNewTarget;
    }

    public abstract Object execute(Object[] arguments);

    public static JSProxyCallNode create(JSContext context, boolean isNew, boolean isNewTarget) {
        return JSProxyCallNodeGen.create(context, isNew, isNewTarget);
    }

    /**
     * Implements the [[Call]] internal method ("apply" trap) for Proxy.
     */
    @Specialization(guards = {"!isNew", "!isNewTarget"})
    protected Object doCall(Object[] arguments) {
        Object thisObj = JSArguments.getThisObject(arguments);
        Object function = JSArguments.getFunctionObject(arguments);
        assert JSProxy.isProxy(function);
        DynamicObject proxy = (DynamicObject) function;

        if (!callableProfile.profile(JSRuntime.isCallableProxy(proxy))) {
            throw Errors.createTypeErrorNotAFunction(function, this);
        } else {
            DynamicObject pxHandler = JSProxy.getHandlerChecked(proxy);
            TruffleObject pxTarget = JSProxy.getTarget(proxy);
            DynamicObject pxTrapFun = (DynamicObject) trapGetter.executeWithTarget(pxHandler);
            Object[] proxyArguments = JSArguments.extractUserArguments(arguments);
            if (pxTrapFunProfile.profile(pxTrapFun == Undefined.instance)) {
                return callNode.executeCall(JSArguments.create(thisObj, pxTarget, proxyArguments));
            }
            Object[] trapArgs = new Object[]{pxTarget, thisObj, JSArray.createConstant(context, proxyArguments)};
            return callTrapNode.executeCall(JSArguments.create(pxHandler, pxTrapFun, trapArgs));
        }
    }

    /**
     * Implements the [[Construct]] internal method ("construct" trap) for Proxy.
     */
    @Specialization(guards = {"isNew || isNewTarget"})
    protected Object doConstruct(Object[] arguments) {
        Object function = JSArguments.getFunctionObject(arguments);
        assert JSProxy.isProxy(function);
        DynamicObject proxy = (DynamicObject) function;

        if (!callableProfile.profile(JSRuntime.isCallableProxy(proxy))) {
            throw Errors.createTypeErrorNotAFunction(function, this);
        } else {
            DynamicObject pxHandler = JSProxy.getHandlerChecked(proxy);
            TruffleObject pxTarget = JSProxy.getTarget(proxy);
            DynamicObject pxTrapFun = (DynamicObject) trapGetter.executeWithTarget(pxHandler);
            Object newTarget = isNewTarget ? JSArguments.getNewTarget(arguments) : proxy;
            Object[] constructorArguments = JSArguments.extractUserArguments(arguments, isNewTarget ? 1 : 0);
            if (pxTrapFunProfile.profile(pxTrapFun == Undefined.instance)) {
                if (!JSObject.isJSObject(pxTarget)) {
                    return JSInteropNodeUtil.construct(pxTarget, constructorArguments);
                }
                return callNode.executeCall(isNewTarget ? JSArguments.createWithNewTarget(JSFunction.CONSTRUCT, pxTarget, newTarget, constructorArguments)
                                : JSArguments.create(JSFunction.CONSTRUCT, pxTarget, constructorArguments));
            }
            Object[] trapArgs = new Object[]{pxTarget, JSArray.createConstant(context, constructorArguments), newTarget};
            Object result = callTrapNode.executeCall(JSArguments.create(pxHandler, pxTrapFun, trapArgs));
            if (!JSRuntime.isObject(result)) {
                throw Errors.createTypeErrorObjectExpected();
            }
            return result;
        }
    }
}
