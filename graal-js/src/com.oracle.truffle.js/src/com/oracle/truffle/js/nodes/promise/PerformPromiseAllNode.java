/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

public class PerformPromiseAllNode extends PerformPromiseCombinatorNode {

    protected static final class BoxedInt {
        int value;

        BoxedInt() {
        }

        BoxedInt(int value) {
            this.value = value;
        }
    }

    protected static final class ResolveElementArgs {
        boolean alreadyCalled;
        final int index;
        final SimpleArrayList<Object> values;
        final PromiseCapabilityRecord capability;
        final BoxedInt remainingElements;

        ResolveElementArgs(int index, SimpleArrayList<Object> values, PromiseCapabilityRecord capability, BoxedInt remainingElements) {
            this.alreadyCalled = false;
            this.index = index;
            this.values = values;
            this.capability = capability;
            this.remainingElements = remainingElements;
        }
    }

    protected static final HiddenKey RESOLVE_ELEMENT_ARGS_KEY = new HiddenKey("ResolveElementArgs");

    @Child protected JSFunctionCallNode callResolve;
    @Child protected PropertyGetNode getThen;
    @Child protected JSFunctionCallNode callThen;
    @Child protected PropertySetNode setArgs;
    private final BranchProfile growProfile = BranchProfile.create();

    protected PerformPromiseAllNode(JSContext context) {
        super(context);
        this.callResolve = JSFunctionCallNode.createCall();
        this.getThen = PropertyGetNode.create(JSPromise.THEN, false, context);
        this.callThen = JSFunctionCallNode.createCall();
        this.setArgs = PropertySetNode.createSetHidden(RESOLVE_ELEMENT_ARGS_KEY, context);
    }

    public static PerformPromiseAllNode create(JSContext context) {
        return new PerformPromiseAllNode(context);
    }

    @Override
    public DynamicObject execute(IteratorRecord iteratorRecord, DynamicObject constructor, PromiseCapabilityRecord resultCapability) {
        assert JSRuntime.isConstructor(constructor);
        SimpleArrayList<Object> values = new SimpleArrayList<>(10);
        BoxedInt remainingElementsCount = new BoxedInt(1);
        Object promiseResolve = getPromiseResolve(constructor);
        for (int index = 0;; index++) {
            Object next = iteratorStepOrSetDone(iteratorRecord);
            if (next == Boolean.FALSE) {
                iteratorRecord.setDone(true);
                remainingElementsCount.value--;
                if (remainingElementsCount.value == 0) {
                    DynamicObject valuesArray = JSArray.createConstantObjectArray(context, values.toArray());
                    callResolve.executeCall(JSArguments.createOneArg(Undefined.instance, resultCapability.getResolve(), valuesArray));
                }
                return resultCapability.getPromise();
            }
            Object nextValue = iteratorValueOrSetDone(iteratorRecord, next);
            values.add(Undefined.instance, growProfile);
            Object nextPromise = callResolve.executeCall(JSArguments.createOneArg(constructor, promiseResolve, nextValue));
            DynamicObject resolveElement = createResolveElementFunction(index, values, resultCapability, remainingElementsCount);
            Object rejectElement = createRejectElementFunction(index, values, resultCapability, remainingElementsCount);
            remainingElementsCount.value++;
            callThen.executeCall(JSArguments.create(nextPromise, getThen.getValue(nextPromise), resolveElement, rejectElement));
        }
    }

    protected DynamicObject createResolveElementFunction(int index, SimpleArrayList<Object> values, PromiseCapabilityRecord resultCapability, BoxedInt remainingElementsCount) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseAllResolveElement, (c) -> createResolveElementFunctionImpl(c));
        DynamicObject function = JSFunction.create(context.getRealm(), functionData);
        setArgs.setValue(function, new ResolveElementArgs(index, values, resultCapability, remainingElementsCount));
        return function;
    }

    @SuppressWarnings("unused")
    protected Object createRejectElementFunction(int index, SimpleArrayList<Object> values, PromiseCapabilityRecord resultCapability, BoxedInt remainingElementsCount) {
        return resultCapability.getReject();
    }

    private static JSFunctionData createResolveElementFunctionImpl(JSContext context) {
        class PromiseAllResolveElementRootNode extends JavaScriptRootNode {
            @Child private JavaScriptNode valueNode = AccessIndexedArgumentNode.create(0);
            @Child private PropertyGetNode getArgs = PropertyGetNode.createGetHidden(RESOLVE_ELEMENT_ARGS_KEY, context);
            @Child private JSFunctionCallNode callResolve = JSFunctionCallNode.createCall();

            @Override
            public Object execute(VirtualFrame frame) {
                DynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                ResolveElementArgs args = (ResolveElementArgs) getArgs.getValue(functionObject);
                if (args.alreadyCalled) {
                    return Undefined.instance;
                }
                args.alreadyCalled = true;
                Object value = valueNode.execute(frame);
                args.values.set(args.index, value);
                args.remainingElements.value--;
                if (args.remainingElements.value == 0) {
                    DynamicObject valuesArray = JSArray.createConstantObjectArray(context, args.values.toArray());
                    return callResolve.executeCall(JSArguments.createOneArg(Undefined.instance, args.capability.getResolve(), valuesArray));
                }
                return Undefined.instance;
            }
        }
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new PromiseAllResolveElementRootNode());
        return JSFunctionData.createCallOnly(context, callTarget, 1, "");
    }
}
