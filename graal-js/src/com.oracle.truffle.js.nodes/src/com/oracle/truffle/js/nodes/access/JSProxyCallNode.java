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
                throw Errors.createTypeErrorNotAnObject(result, this);
            }
            return result;
        }
    }
}
