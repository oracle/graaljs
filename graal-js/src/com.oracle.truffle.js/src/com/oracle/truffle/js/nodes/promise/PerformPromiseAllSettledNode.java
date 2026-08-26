/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.TruffleStackTraceElement;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

public abstract class PerformPromiseAllSettledNode extends PerformPromiseAllNode {

    protected PerformPromiseAllSettledNode(JSContext context) {
        super(context);
    }

    public static PerformPromiseAllSettledNode create(JSContext context) {
        return PerformPromiseAllSettledNodeGen.create(context);
    }

    @Specialization
    @Override
    protected JSDynamicObject promiseAll(IteratorRecord iteratorRecord, JSDynamicObject constructor, PromiseCapabilityRecord resultCapability, Object promiseResolve,
                    @Cached InlinedBranchProfile growProfile) {
        return super.promiseAll(iteratorRecord, constructor, resultCapability, promiseResolve, growProfile);
    }

    @Override
    protected JSFunctionObject createResolveElementFunction(int index, SimpleArrayList<Object> values, PromiseCapabilityRecord resultCapability, BoxedInt remainingElementsCount) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseAllSettledResolveElement, (c) -> createResolveElementFunctionImpl(c));
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setArgs.setValue(function, new ResolveElementArgs(index, values, resultCapability, remainingElementsCount));
        return function;
    }

    @Override
    protected Object createRejectElementFunction(int index, SimpleArrayList<Object> values, PromiseCapabilityRecord resultCapability, BoxedInt remainingElementsCount) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseAllSettledRejectElement, (c) -> createRejectElementFunctionImpl(c));
        JSFunctionObject function = JSFunction.create(getRealm(), functionData);
        setArgs.setValue(function, new ResolveElementArgs(index, values, resultCapability, remainingElementsCount));
        return function;
    }

    private static JSFunctionData createResolveElementFunctionImpl(JSContext context) {
        return JSFunctionData.createCallOnly(context, new PromiseAllSettledElementRootNode(context, true).getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    private static JSFunctionData createRejectElementFunctionImpl(JSContext context) {
        return JSFunctionData.createCallOnly(context, new PromiseAllSettledElementRootNode(context, false).getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    private static final class PromiseAllSettledElementRootNode extends JavaScriptRootNode implements AsyncHandlerRootNode {
        private final JSContext context;
        private final boolean fulfilled;
        @Child private JavaScriptNode valueNode;
        @Child private PropertyGetNode getArgsNode;
        @Child private JSFunctionCallNode callResolveNode;
        @Child private CreateObjectNode createObjectNode;
        @Child private CreateDataPropertyNode createStatusNode;
        @Child private CreateDataPropertyNode createResultNode;

        PromiseAllSettledElementRootNode(JSContext context, boolean fulfilled) {
            super(context.getLanguage());
            this.context = context;
            this.fulfilled = fulfilled;
            this.valueNode = AccessIndexedArgumentNode.create(0);
            this.getArgsNode = PropertyGetNode.createGetHidden(RESOLVE_ELEMENT_ARGS_KEY, context);
            this.callResolveNode = JSFunctionCallNode.createCall();
            this.createObjectNode = CreateObjectNode.create(context);
            this.createStatusNode = CreateDataPropertyNode.create(context, Strings.STATUS);
            this.createResultNode = CreateDataPropertyNode.create(context, fulfilled ? Strings.VALUE : Strings.REASON);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            JSFunctionObject functionObject = JSFrameUtil.getFunctionObject(frame);
            ResolveElementArgs args = (ResolveElementArgs) getArgsNode.getValue(functionObject);
            if (args.alreadyCalled) {
                return Undefined.instance;
            }
            args.alreadyCalled = true;
            Object value = valueNode.execute(frame);

            JSRealm realm = getRealm();
            JSObject result = createObjectNode.execute(realm);
            createStatusNode.executeVoid(result, fulfilled ? Strings.FULFILLED : Strings.REJECTED);
            createResultNode.executeVoid(result, value);

            args.values.set(args.index, result);
            args.remainingElements.value--;
            if (args.remainingElements.value == 0) {
                var valuesArray = JSArray.createConstantObjectArray(context, realm, args.values.toArray());
                return callResolveNode.executeCall(JSArguments.createOneArg(Undefined.instance, args.capability.getResolve(), valuesArray));
            }
            return Undefined.instance;
        }

        @Override
        public AsyncStackTraceInfo getAsyncStackTraceInfo(JSFunctionObject handlerFunction) {
            ResolveElementArgs args = (ResolveElementArgs) JSObjectUtil.getHiddenProperty(handlerFunction, RESOLVE_ELEMENT_ARGS_KEY);
            JSRealm realm = JSFunction.getRealm(handlerFunction);
            TruffleStackTraceElement stackTraceElement = createPromiseAllStackTraceElement(args.index, realm, realm.getPromiseAllSettledFunctionObject());
            return new AsyncStackTraceInfo(args.capability.getPromise(), stackTraceElement);
        }
    }
}
