/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;

@NodeInfo(cost = NodeCost.NONE)
@ImportStatic({JSProxy.class})
public abstract class JSProxyHasPropertyNode extends JavaScriptBaseNode {

    @Child protected GetMethodNode trapGetter;
    @Child private JSFunctionCallNode callNode;
    @Child private JSToBooleanNode toBooleanNode;
    @Child private JSToPropertyKeyNode toPropertyKeyNode;

    public JSProxyHasPropertyNode(JSContext context) {
        this.callNode = JSFunctionCallNode.createCall();
        this.trapGetter = GetMethodNode.create(context, null, JSProxy.HAS);
        this.toPropertyKeyNode = JSToPropertyKeyNode.create();
        this.toBooleanNode = JSToBooleanNode.create();
    }

    public static JSProxyHasPropertyNode create(JSContext context) {
        return JSProxyHasPropertyNodeGen.create(context);
    }

    public abstract boolean executeWithTargetAndKeyBoolean(Object shared, Object key);

    private static void checkTrapResult(boolean accessible, boolean trapResult) {
        if (!accessible && !trapResult) {
            throw Errors.createTypeError("Proxy can't successfully access a non-writable, non-configurable property");
        }
    }

    @Specialization
    protected boolean doGeneric(DynamicObject obj, Object key,
                    @Cached("createBinaryProfile()") ConditionProfile trapFunProfile,
                    @Cached("createBinaryProfile()") ConditionProfile walkProto) {
        Object propertyKey = toPropertyKeyNode.execute(key);
        DynamicObject proxy = obj;
        if (walkProto.profile(!JSProxy.isProxy(proxy))) {
            // the proxy is only one of the prototypes
            while (!JSProxy.isProxy(proxy)) {
                proxy = JSObject.getPrototype(proxy);
            }
        }
        TruffleObject target = JSProxy.getTarget(proxy);
        DynamicObject handler = JSProxy.getHandler(proxy);
        DynamicObject trapFun = (DynamicObject) trapGetter.executeWithTarget(handler);
        if (trapFunProfile.profile(trapFun == Undefined.instance)) {
            if (JSObject.isJSObject(target)) {
                return JSObject.hasProperty((DynamicObject) target, propertyKey);
            } else {
                return JSInteropNodeUtil.hasProperty(target, propertyKey);
            }
        } else {
            Object callResult = callNode.executeCall(JSArguments.create(handler, trapFun, target, propertyKey));
            boolean trapResult = toBooleanNode.executeBoolean(callResult);
            boolean accessible = JSProxy.checkPropertyIsSettable(target, propertyKey);
            checkTrapResult(accessible, trapResult);
            return trapResult;
        }
    }
}
