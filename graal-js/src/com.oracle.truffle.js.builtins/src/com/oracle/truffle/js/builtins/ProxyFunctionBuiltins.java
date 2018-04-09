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
