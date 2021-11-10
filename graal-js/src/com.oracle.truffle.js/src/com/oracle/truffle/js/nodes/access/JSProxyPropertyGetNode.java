/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSUncheckedProxyHandlerObject;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

@NodeInfo(cost = NodeCost.NONE)
public abstract class JSProxyPropertyGetNode extends JavaScriptBaseNode {

    @Child protected GetMethodNode trapGet;
    @Child private JSFunctionCallNode callNode;
    @Child private JSGetOwnPropertyNode getOwnPropertyNode;
    @Child private JSIdenticalNode sameValueNode;
    private final BranchProfile errorBranch = BranchProfile.create();

    protected JSProxyPropertyGetNode(JSContext context) {
        this.callNode = JSFunctionCallNode.createCall();
        this.trapGet = GetMethodNode.create(context, JSProxy.GET);
    }

    public static JSProxyPropertyGetNode create(JSContext context) {
        return JSProxyPropertyGetNodeGen.create(context);
    }

    public abstract Object executeWithReceiver(Object proxy, Object receiver, Object key);

    @Specialization
    protected Object doGeneric(DynamicObject proxy, Object receiver, Object key,
                    @Cached JSToPropertyKeyNode toPropertyKeyNode,
                    @Cached("createBinaryProfile()") ConditionProfile hasTrap,
                    @Cached JSClassProfile targetClassProfile) {
        assert JSProxy.isJSProxy(proxy);
        assert !(key instanceof HiddenKey);
        Object propertyKey = toPropertyKeyNode.execute(key);
        DynamicObject handler = JSProxy.getHandlerChecked(proxy, errorBranch);
        Object target = JSProxy.getTarget(proxy);
        Object trapFun = trapGet.executeWithTarget(handler);
        if (hasTrap.profile(trapFun == Undefined.instance)) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                return JSObject.getOrDefault((DynamicObject) target, propertyKey, receiver, Undefined.instance, targetClassProfile, this);
            } else {
                return JSInteropUtil.readMemberOrDefault(target, propertyKey, Undefined.instance);
            }
        }
        Object trapResult = callNode.executeCall(JSArguments.create(handler, trapFun, target, propertyKey, receiver));
        if (!(handler instanceof JSUncheckedProxyHandlerObject)) {
            checkInvariants(propertyKey, target, trapResult);
        }
        return trapResult;
    }

    private void checkInvariants(Object propertyKey, Object proxyTarget, Object trapResult) {
        assert JSRuntime.isPropertyKey(propertyKey);
        if (!JSDynamicObject.isJSDynamicObject(proxyTarget)) {
            return; // best effort, cannot check for foreign objects
        }
        PropertyDescriptor targetDesc = getOwnProperty((DynamicObject) proxyTarget, propertyKey);
        if (targetDesc != null) {
            if (targetDesc.isDataDescriptor() && !targetDesc.getConfigurable() && !targetDesc.getWritable()) {
                Object targetValue = targetDesc.getValue();
                if (!isSameValue(trapResult, targetValue)) {
                    errorBranch.enter();
                    throw Errors.createTypeErrorProxyGetInvariantViolated(propertyKey, targetValue, trapResult);
                }
            }
            if (targetDesc.isAccessorDescriptor() && !targetDesc.getConfigurable() && targetDesc.getGet() == Undefined.instance) {
                if (trapResult != Undefined.instance) {
                    errorBranch.enter();
                    throw Errors.createTypeError("Trap result must be undefined since the proxy target has a corresponding non-configurable own accessor property with undefined getter");
                }
            }
        }
    }

    private boolean isSameValue(Object trapResult, Object value) {
        if (sameValueNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            sameValueNode = insert(JSIdenticalNode.createSameValue());
        }
        return sameValueNode.executeBoolean(trapResult, value);
    }

    private PropertyDescriptor getOwnProperty(DynamicObject target, Object propertyKey) {
        if (getOwnPropertyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getOwnPropertyNode = insert(JSGetOwnPropertyNode.create());
        }
        return getOwnPropertyNode.execute(target, propertyKey);
    }
}
