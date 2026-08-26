/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.promise;

import java.util.List;

import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.builtins.helper.ListGetNode;
import com.oracle.truffle.js.builtins.helper.ListGetNodeGen;
import com.oracle.truffle.js.builtins.helper.ListSizeNode;
import com.oracle.truffle.js.builtins.helper.ListSizeNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.JSGetOwnPropertyNode;
import com.oracle.truffle.js.nodes.access.OwnPropertyKeysNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseCombinatorNode.BoxedInt;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/** Implements PerformPromiseAllKeyed for Promise.allKeyed and Promise.allSettledKeyed. */
public final class PerformPromiseAllKeyedNode extends JavaScriptBaseNode {

    private static final HiddenKey RESOLVE_ELEMENT_ARGS_KEY = new HiddenKey("PromiseAllKeyedResolveElementArgs");

    private static final class ResolveElementArgs {
        final int index;
        final PromiseCapabilityRecord capability;
        final SimpleArrayList<Object> keys;
        final SimpleArrayList<Object> values;
        final BoxedInt remainingElements;
        boolean alreadyCalled;

        ResolveElementArgs(int index, PromiseCapabilityRecord capability, SimpleArrayList<Object> keys, SimpleArrayList<Object> values, BoxedInt remainingElements) {
            this.index = index;
            this.capability = capability;
            this.keys = keys;
            this.values = values;
            this.remainingElements = remainingElements;
        }
    }

    private final JSContext context;
    private final boolean allSettled;
    @Child private OwnPropertyKeysNode ownPropertyKeysNode;
    @Child private ListSizeNode listSizeNode;
    @Child private ListGetNode listGetNode;
    @Child private JSGetOwnPropertyNode getOwnPropertyNode;
    @Child private ReadElementNode readElementNode;
    @Child private JSFunctionCallNode callPromiseResolveNode;
    @Child private PropertyGetNode getThenNode;
    @Child private JSFunctionCallNode callThenNode;
    @Child private JSFunctionCallNode callResultResolveNode;
    @Child private PropertySetNode setArgsNode;

    private PerformPromiseAllKeyedNode(JSContext context, boolean allSettled) {
        this.context = context;
        this.allSettled = allSettled;
        this.ownPropertyKeysNode = OwnPropertyKeysNode.create();
        this.listSizeNode = ListSizeNodeGen.create();
        this.listGetNode = ListGetNodeGen.create();
        this.getOwnPropertyNode = JSGetOwnPropertyNode.create(false, true, false, false, false);
        this.readElementNode = ReadElementNode.create(context);
        this.callPromiseResolveNode = JSFunctionCallNode.createCall();
        this.getThenNode = PropertyGetNode.create(Strings.THEN, false, context);
        this.callThenNode = JSFunctionCallNode.createCall();
        this.callResultResolveNode = JSFunctionCallNode.createCall();
        this.setArgsNode = PropertySetNode.createSetHidden(RESOLVE_ELEMENT_ARGS_KEY, context);
    }

    @NeverDefault
    public static PerformPromiseAllKeyedNode create(JSContext context, boolean allSettled) {
        return new PerformPromiseAllKeyedNode(context, allSettled);
    }

    public JSDynamicObject execute(Object promises, Object constructor, PromiseCapabilityRecord resultCapability, Object promiseResolve) {
        assert JSRuntime.isConstructor(constructor);
        assert JSRuntime.isCallable(promiseResolve);

        List<Object> allKeys = ownPropertyKeysNode.execute(promises);
        int allKeysSize = listSizeNode.execute(allKeys);
        SimpleArrayList<Object> keys = new SimpleArrayList<>(allKeysSize);
        SimpleArrayList<Object> values = new SimpleArrayList<>(allKeysSize);
        BoxedInt remainingElementsCount = new BoxedInt(1);

        for (int i = 0; i < allKeysSize; i++) {
            Object key = listGetNode.execute(allKeys, i);
            PropertyDescriptor propertyDescriptor = getOwnPropertyNode.execute(promises, key);
            if (propertyDescriptor == null || !propertyDescriptor.getEnumerable()) {
                continue;
            }

            Object propertyValue = readElementNode.executeWithTargetAndIndex(promises, key);
            int index = keys.size();
            keys.addUnchecked(key);
            values.addUnchecked(Undefined.instance);

            Object nextPromise = callPromiseResolveNode.executeCall(JSArguments.createOneArg(constructor, promiseResolve, propertyValue));
            ResolveElementArgs args = new ResolveElementArgs(index, resultCapability, keys, values, remainingElementsCount);
            JSFunctionObject resolveElement = createResolveElementFunction(args);
            Object rejectElement = allSettled ? createRejectElementFunction(args) : resultCapability.getReject();
            remainingElementsCount.value++;
            callThenNode.executeCall(JSArguments.create(nextPromise, getThenNode.getValue(nextPromise), resolveElement, rejectElement));
        }

        remainingElementsCount.value--;
        if (remainingElementsCount.value == 0) {
            resolveResult(keys, values, resultCapability);
        }
        return resultCapability.getPromise();
    }

    private JSFunctionObject createResolveElementFunction(ResolveElementArgs args) {
        JSContext.BuiltinFunctionKey key = allSettled
                        ? JSContext.BuiltinFunctionKey.PromiseAllSettledKeyedResolveElement
                        : JSContext.BuiltinFunctionKey.PromiseAllKeyedResolveElement;
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(key, allSettled
                        ? PerformPromiseAllKeyedNode::createAllSettledResolveElementFunction
                        : PerformPromiseAllKeyedNode::createAllResolveElementFunction);
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setArgsNode.setValue(function, args);
        return function;
    }

    private JSFunctionObject createRejectElementFunction(ResolveElementArgs args) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseAllSettledKeyedRejectElement,
                        PerformPromiseAllKeyedNode::createAllSettledRejectElementFunction);
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setArgsNode.setValue(function, args);
        return function;
    }

    private void resolveResult(SimpleArrayList<Object> keys, SimpleArrayList<Object> values, PromiseCapabilityRecord capability) {
        JSObject result = createResultObject(context, keys, values);
        callResultResolveNode.executeCall(JSArguments.createOneArg(Undefined.instance, capability.getResolve(), result));
    }

    private static JSObject createResultObject(JSContext context, SimpleArrayList<Object> keys, SimpleArrayList<Object> values) {
        JSObject result = JSOrdinary.createWithNullPrototype(context);
        for (int i = 0; i < keys.size(); i++) {
            JSRuntime.createDataPropertyOrThrow(result, keys.get(i), values.get(i));
        }
        return result;
    }

    private static Object settleElement(ResolveElementArgs args, Object value, JSContext context, JSFunctionCallNode callResolveNode) {
        if (args.alreadyCalled) {
            return Undefined.instance;
        }
        args.alreadyCalled = true;
        args.values.set(args.index, value);
        args.remainingElements.value--;
        if (args.remainingElements.value == 0) {
            JSObject result = createResultObject(context, args.keys, args.values);
            return callResolveNode.executeCall(JSArguments.createOneArg(Undefined.instance, args.capability.getResolve(), result));
        }
        return Undefined.instance;
    }

    private static JSFunctionData createAllResolveElementFunction(JSContext context) {
        class PromiseAllKeyedResolveElementRootNode extends JavaScriptRootNode implements AsyncHandlerRootNode {
            @Child private JavaScriptNode valueNode = AccessIndexedArgumentNode.create(0);
            @Child private PropertyGetNode getArgsNode = PropertyGetNode.createGetHidden(RESOLVE_ELEMENT_ARGS_KEY, context);
            @Child private JSFunctionCallNode callResolveNode = JSFunctionCallNode.createCall();

            @Override
            public Object execute(VirtualFrame frame) {
                JSFunctionObject function = JSFrameUtil.getFunctionObject(frame);
                ResolveElementArgs args = (ResolveElementArgs) getArgsNode.getValue(function);
                return settleElement(args, valueNode.execute(frame), context, callResolveNode);
            }

            @Override
            public AsyncStackTraceInfo getAsyncStackTraceInfo(JSFunctionObject handlerFunction) {
                ResolveElementArgs args = (ResolveElementArgs) JSObjectUtil.getHiddenProperty(handlerFunction, RESOLVE_ELEMENT_ARGS_KEY);
                JSRealm realm = JSFunction.getRealm(handlerFunction);
                TruffleStackTraceElement stackTraceElement = PerformPromiseAllNode.createPromiseAllStackTraceElement(args.index, realm, realm.getPromiseAllKeyedFunctionObject());
                return new AsyncStackTraceInfo(args.capability.getPromise(), stackTraceElement);
            }
        }
        return JSFunctionData.createCallOnly(context, new PromiseAllKeyedResolveElementRootNode().getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    private static JSFunctionData createAllSettledResolveElementFunction(JSContext context) {
        class PromiseAllSettledKeyedResolveElementRootNode extends JavaScriptRootNode implements AsyncHandlerRootNode {
            @Child private JavaScriptNode valueNode = AccessIndexedArgumentNode.create(0);
            @Child private PropertyGetNode getArgsNode = PropertyGetNode.createGetHidden(RESOLVE_ELEMENT_ARGS_KEY, context);
            @Child private JSFunctionCallNode callResolveNode = JSFunctionCallNode.createCall();
            @Child private CreateObjectNode createObjectNode = CreateObjectNode.create(context);
            @Child private CreateDataPropertyNode createStatusNode = CreateDataPropertyNode.create(context, Strings.STATUS);
            @Child private CreateDataPropertyNode createValueNode = CreateDataPropertyNode.create(context, Strings.VALUE);

            @Override
            public Object execute(VirtualFrame frame) {
                JSFunctionObject function = JSFrameUtil.getFunctionObject(frame);
                ResolveElementArgs args = (ResolveElementArgs) getArgsNode.getValue(function);
                if (args.alreadyCalled) {
                    return Undefined.instance;
                }
                JSObject settledResult = createObjectNode.execute(getRealm());
                createStatusNode.executeVoid(settledResult, Strings.FULFILLED);
                createValueNode.executeVoid(settledResult, valueNode.execute(frame));
                return settleElement(args, settledResult, context, callResolveNode);
            }

            @Override
            public AsyncStackTraceInfo getAsyncStackTraceInfo(JSFunctionObject handlerFunction) {
                ResolveElementArgs args = (ResolveElementArgs) JSObjectUtil.getHiddenProperty(handlerFunction, RESOLVE_ELEMENT_ARGS_KEY);
                JSRealm realm = JSFunction.getRealm(handlerFunction);
                TruffleStackTraceElement stackTraceElement = PerformPromiseAllNode.createPromiseAllStackTraceElement(args.index, realm, realm.getPromiseAllSettledKeyedFunctionObject());
                return new AsyncStackTraceInfo(args.capability.getPromise(), stackTraceElement);
            }
        }
        return JSFunctionData.createCallOnly(context, new PromiseAllSettledKeyedResolveElementRootNode().getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    private static JSFunctionData createAllSettledRejectElementFunction(JSContext context) {
        class PromiseAllSettledKeyedRejectElementRootNode extends JavaScriptRootNode implements AsyncHandlerRootNode {
            @Child private JavaScriptNode reasonNode = AccessIndexedArgumentNode.create(0);
            @Child private PropertyGetNode getArgsNode = PropertyGetNode.createGetHidden(RESOLVE_ELEMENT_ARGS_KEY, context);
            @Child private JSFunctionCallNode callResolveNode = JSFunctionCallNode.createCall();
            @Child private CreateObjectNode createObjectNode = CreateObjectNode.create(context);
            @Child private CreateDataPropertyNode createStatusNode = CreateDataPropertyNode.create(context, Strings.STATUS);
            @Child private CreateDataPropertyNode createReasonNode = CreateDataPropertyNode.create(context, Strings.REASON);

            @Override
            public Object execute(VirtualFrame frame) {
                JSFunctionObject function = JSFrameUtil.getFunctionObject(frame);
                ResolveElementArgs args = (ResolveElementArgs) getArgsNode.getValue(function);
                if (args.alreadyCalled) {
                    return Undefined.instance;
                }
                JSObject settledResult = createObjectNode.execute(getRealm());
                createStatusNode.executeVoid(settledResult, Strings.REJECTED);
                createReasonNode.executeVoid(settledResult, reasonNode.execute(frame));
                return settleElement(args, settledResult, context, callResolveNode);
            }

            @Override
            public AsyncStackTraceInfo getAsyncStackTraceInfo(JSFunctionObject handlerFunction) {
                ResolveElementArgs args = (ResolveElementArgs) JSObjectUtil.getHiddenProperty(handlerFunction, RESOLVE_ELEMENT_ARGS_KEY);
                JSRealm realm = JSFunction.getRealm(handlerFunction);
                TruffleStackTraceElement stackTraceElement = PerformPromiseAllNode.createPromiseAllStackTraceElement(args.index, realm, realm.getPromiseAllSettledKeyedFunctionObject());
                return new AsyncStackTraceInfo(args.capability.getPromise(), stackTraceElement);
            }
        }
        return JSFunctionData.createCallOnly(context, new PromiseAllSettledKeyedRejectElementRootNode().getCallTarget(), 1, Strings.EMPTY_STRING);
    }
}
