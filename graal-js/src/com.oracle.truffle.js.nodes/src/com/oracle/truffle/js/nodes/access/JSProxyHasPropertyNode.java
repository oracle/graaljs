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

    private void checkTrapResult(boolean accessible, boolean trapResult) {
        if (!accessible && !trapResult) {
            throw Errors.createTypeError("Proxy can't successfully access a non-writable, non-configurable property", this);
        }
    }

    @Specialization
    protected boolean doGeneric(DynamicObject proxy, Object key,
                    @Cached("createBinaryProfile()") ConditionProfile trapFunProfile) {
        assert JSProxy.isProxy(proxy);
        Object propertyKey = toPropertyKeyNode.execute(key);
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
