/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.ConstructorBuiltins.ConstructJSProxyNode;
import com.oracle.truffle.js.builtins.ConstructorBuiltinsFactory.ConstructJSProxyNodeGen;
import com.oracle.truffle.js.builtins.ProxyFunctionBuiltinsFactory.RevocableNodeGen;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains built-in functions of the Proxy Constructor.
 */
public final class ProxyFunctionBuiltins extends JSBuiltinsContainer.Lambda {
    public ProxyFunctionBuiltins() {
        super(JSProxy.CLASS_NAME);
        defineFunction("revocable", 2, (context, builtin) -> RevocableNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context)));
    }

    public abstract static class RevocableNode extends JSBuiltinNode {
        @Child private ConstructJSProxyNode proxyCreateNode;
        @Child private PropertySetNode setRevocableProxySlotNode;
        @Child private CreateObjectNode createObjectNode;
        @Child private CreateDataPropertyNode createProxyPropertyNode;
        @Child private CreateDataPropertyNode createRevokePropertyNode;

        public RevocableNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.proxyCreateNode = ConstructJSProxyNodeGen.create(context, builtin, false, null);
            this.setRevocableProxySlotNode = PropertySetNode.create(JSProxy.REVOCABLE_PROXY, false, context, false);
            this.createObjectNode = CreateObjectNode.create(context);
            this.createProxyPropertyNode = CreateDataPropertyNode.create(context, "proxy");
            this.createRevokePropertyNode = CreateDataPropertyNode.create(context, "revoke");
        }

        @Specialization
        protected Object doDefault(VirtualFrame frame, Object target, Object handler) {
            DynamicObject proxy = proxyCreateNode.execute(Undefined.instance, target, handler);

            JSFunctionData revokerFunctionData = getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.ProxyRevokerFunction, c -> createProxyRevokerFunctionImpl(c));
            DynamicObject revoker = JSFunction.create(getContext().getRealm(), revokerFunctionData);
            setRevocableProxySlotNode.setValue(revoker, proxy);

            DynamicObject result = createObjectNode.execute(frame);
            createProxyPropertyNode.executeVoid(result, proxy);
            createRevokePropertyNode.executeVoid(result, revoker);
            return result;
        }

        private static JSFunctionData createProxyRevokerFunctionImpl(JSContext context) {
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode() {
                @Child private PropertyGetNode getRevocableProxyNode = PropertyGetNode.create(JSProxy.REVOCABLE_PROXY, false, context);
                @Child private PropertySetNode setRevocableProxyNode = PropertySetNode.create(JSProxy.REVOCABLE_PROXY, false, context, false);

                @Override
                public Object execute(VirtualFrame frame) {
                    DynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                    DynamicObject proxy = (DynamicObject) getRevocableProxyNode.getValue(functionObject);
                    if (proxy == Null.instance) {
                        return Undefined.instance;
                    }
                    setRevocableProxyNode.setValue(functionObject, Null.instance);
                    JSProxy.revoke(proxy);
                    return Undefined.instance;
                }
            });
            return JSFunctionData.createCallOnly(context, callTarget, 0, ""); // anonymous
        }
    }
}
