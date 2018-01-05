/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.util.JSReflectUtils;

@NodeInfo(cost = NodeCost.NONE)
public abstract class JSProxyPropertyGetNode extends JavaScriptBaseNode {

    @Child protected GetMethodNode trapGet;
    @Child private JSFunctionCallNode callNode;
    @Child private JSToPropertyKeyNode toPropertyKeyNode;

    protected JSProxyPropertyGetNode(JSContext context) {
        this.callNode = JSFunctionCallNode.createCall();
        this.trapGet = GetMethodNode.create(context, null, JSProxy.GET);
    }

    public static JSProxyPropertyGetNode create(JSContext context) {
        return JSProxyPropertyGetNodeGen.create(context);
    }

    public abstract Object executeWithReceiver(Object proxy, boolean floatingCondition, Object key);

    public abstract Object executeWithReceiverInt(Object proxy, boolean floatingCondition, int key);

    @Specialization
    protected Object doGeneric(DynamicObject proxy, boolean floatingCondition, Object key,
                    @Cached("createBinaryProfile()") ConditionProfile walkProto,
                    @Cached("createBinaryProfile()") ConditionProfile hasTrap) {
        assert !(key instanceof HiddenKey);
        Object propertyKey = toPropertyKey(key);
        DynamicObject px = proxy;
        if (walkProto.profile(!JSProxy.isProxy(proxy))) {
            while (!JSProxy.isProxy(px)) {
                px = JSObject.getPrototype(px);
            }
        }
        DynamicObject handler = JSProxy.getHandler(px, floatingCondition);
        TruffleObject target = JSProxy.getTarget(px, floatingCondition);
        Object trapFun = trapGet.executeWithTarget(handler);
        if (hasTrap.profile(trapFun == Undefined.instance)) {
            if (JSObject.isJSObject(target)) {
                return JSReflectUtils.performOrdinaryGet((DynamicObject) target, propertyKey, proxy);
            } else {
                return JSInteropNodeUtil.read(target, propertyKey);
            }
        }
        Object trapResult = callNode.executeCall(JSArguments.create(handler, trapFun, target, propertyKey, proxy));
        JSProxy.checkProxyGetTrapInvariants(target, propertyKey, trapResult);
        return trapResult;
    }

    private Object toPropertyKey(Object key) {
        if (toPropertyKeyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toPropertyKeyNode = insert(JSToPropertyKeyNode.create());
        }
        return toPropertyKeyNode.execute(key);
    }
}
