/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
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
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

public abstract class JSProxyPropertySetNode extends JavaScriptBaseNode {
    private final JSContext context;
    private final boolean isStrict;
    private final boolean defineProperty;
    private final byte attributes;

    @Child private JSFunctionCallNode call;
    @Child private JSToBooleanNode toBoolean;
    @Child protected GetMethodNode trapGet;
    @Child private FromPropertyDescriptorNode fromPropertyDescriptorNode;

    protected JSProxyPropertySetNode(JSContext context, boolean isStrict, boolean defineProperty, int attributes) {
        this.context = context;
        this.isStrict = isStrict;
        this.defineProperty = defineProperty;
        this.attributes = (byte) attributes;
        this.call = JSFunctionCallNode.createCall();
        this.trapGet = GetMethodNode.create(context, trapName(defineProperty));
        this.toBoolean = JSToBooleanNode.create();
    }

    public abstract boolean executeWithReceiverAndValue(Object proxy, Object receiver, Object value, Object key);

    public abstract boolean executeWithReceiverAndValueInt(Object proxy, Object receiver, int value, Object key);

    @NeverDefault
    public static JSProxyPropertySetNode create(JSContext context, boolean isStrict, boolean defineProperty, int attributes) {
        return JSProxyPropertySetNodeGen.create(context, isStrict, defineProperty, attributes);
    }

    @Specialization
    protected boolean doGeneric(JSDynamicObject proxy, Object receiver, Object value, Object key,
                    @Cached JSToPropertyKeyNode toPropertyKey,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached InlinedConditionProfile hasTrap,
                    @Cached JSClassProfile targetClassProfile) {
        assert JSProxy.isJSProxy(proxy);
        assert !(key instanceof HiddenKey);
        Object propertyKey = toPropertyKey.execute(key);
        if (JSRuntime.isPrivateSymbol(propertyKey)) {
            errorBranch.enter(this);
            if (isStrict) {
                throw Errors.createTypeErrorPrivateSymbolInProxy(this);
            } else {
                return false;
            }
        }
        JSDynamicObject handler = JSProxy.getHandler(proxy);
        if (handler == Null.instance) {
            errorBranch.enter(this);
            throw Errors.createTypeErrorProxyRevoked(trapName(defineProperty), this);
        }
        Object target = JSProxy.getTarget(proxy);
        Object trapFun = trapGet.executeWithTarget(handler);
        if (hasTrap.profile(this, trapFun == Undefined.instance)) {
            if (target instanceof JSDynamicObject targetObj) {
                if (defineProperty) {
                    PropertyDescriptor desc = PropertyDescriptor.createData(value, attributes);
                    return JSObject.defineOwnProperty(targetObj, propertyKey, desc, isStrict);
                } else {
                    return JSObject.setWithReceiver(targetObj, propertyKey, value, receiver, isStrict, targetClassProfile, this);
                }
            } else {
                return JSInteropUtil.set(context, target, propertyKey, value, isStrict);
            }
        }
        Object[] trapArgs;
        PropertyDescriptor desc = null;
        if (defineProperty) {
            desc = PropertyDescriptor.createData(value, attributes);
            trapArgs = JSArguments.create(handler, trapFun, target, propertyKey, fromPropertyDescriptor(desc));
        } else {
            trapArgs = JSArguments.create(handler, trapFun, target, propertyKey, value, receiver);
        }
        Object trapResult = call.executeCall(trapArgs);
        boolean booleanTrapResult = toBoolean.executeBoolean(trapResult);
        if (!booleanTrapResult) {
            errorBranch.enter(this);
            if (isStrict) {
                throw Errors.createTypeErrorTrapReturnedFalsish(trapName(booleanTrapResult), propertyKey);
            } else {
                return false;
            }
        }
        if (handler instanceof JSUncheckedProxyHandlerObject) {
            return true;
        }
        if (defineProperty) {
            return JSProxy.checkProxyDefinePropertyTrapInvariants(proxy, propertyKey, desc);
        } else {
            return JSProxy.checkProxySetTrapInvariants(proxy, propertyKey, value);
        }
    }

    private static TruffleString trapName(boolean defineProperty) {
        return defineProperty ? JSProxy.DEFINE_PROPERTY : JSProxy.SET;
    }

    private Object fromPropertyDescriptor(PropertyDescriptor desc) {
        if (fromPropertyDescriptorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            fromPropertyDescriptorNode = insert(FromPropertyDescriptorNodeGen.create());
        }
        return fromPropertyDescriptorNode.execute(desc, context);
    }
}
