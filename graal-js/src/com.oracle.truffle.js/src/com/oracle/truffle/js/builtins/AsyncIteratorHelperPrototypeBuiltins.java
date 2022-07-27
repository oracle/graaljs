/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.AsyncHandlerRootNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class AsyncIteratorHelperPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncIteratorHelperPrototypeBuiltins.HelperIteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new AsyncIteratorHelperPrototypeBuiltins();

    public static final TruffleString PROTOTYPE_NAME = Strings.constant("AsyncIteratorHelper.prototype");

    public static final TruffleString CLASS_NAME = Strings.constant("AsyncIteratorHelper");

    public static final TruffleString TO_STRING_TAG = Strings.constant("Async Iterator Helper");

    private static final HiddenKey TARGET_ID = new HiddenKey("target");
    private static final HiddenKey IMPL_ID = new HiddenKey("impl");
    private static final HiddenKey NEXT_PROMISE_ID = new HiddenKey("promise");

    protected AsyncIteratorHelperPrototypeBuiltins() {
        super(JSArray.ITERATOR_PROTOTYPE_NAME, AsyncIteratorHelperPrototypeBuiltins.HelperIteratorPrototype.class);
    }

    public enum HelperIteratorPrototype implements BuiltinEnum<AsyncIteratorHelperPrototypeBuiltins.HelperIteratorPrototype> {
        next(1),
        return_(0);

        private final int length;

        HelperIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, AsyncIteratorHelperPrototypeBuiltins.HelperIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return AsyncIteratorHelperPrototypeBuiltinsFactory.AsyncIteratorHelperNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case return_:
                return AsyncIteratorHelperPrototypeBuiltinsFactory.IteratorHelperReturnNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    protected static class CreateAsyncIteratorHelperNode extends JavaScriptBaseNode {
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
        @Child private PropertySetNode setTargetNode;
        @Child private PropertySetNode setImplNode;
        @Child private PropertySetNode setPromiseNode;

        @Child private JSFunctionCallNode callNode;
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;

        private final JSContext context;

        public CreateAsyncIteratorHelperNode(JSContext context) {
            this.context = context;

            createObjectNode = CreateObjectNode.createOrdinaryWithPrototype(context);
            setTargetNode = PropertySetNode.createSetHidden(TARGET_ID, context);
            setImplNode = PropertySetNode.createSetHidden(IMPL_ID, context);
            setPromiseNode = PropertySetNode.createSetHidden(NEXT_PROMISE_ID, context);

            callNode = JSFunctionCallNode.createCall();
            newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
        }

        public JSDynamicObject execute(IteratorRecord target, JSFunctionObject next) {
            JSDynamicObject iterator = createObjectNode.execute(JSRealm.get(this).getAsyncIteratorHelperPrototype());
            setTargetNode.setValue(iterator, target);
            setImplNode.setValue(iterator, next);

            PromiseCapabilityRecord capabilityRecord = newPromiseCapabilityNode.executeDefault();
            callNode.executeCall(JSArguments.createOneArg(capabilityRecord.getPromise(), capabilityRecord.getResolve(), Undefined.instance));
            setPromiseNode.setValue(iterator, capabilityRecord.getPromise());
            return iterator;
        }

        public static CreateAsyncIteratorHelperNode create(JSContext context) {
            return new CreateAsyncIteratorHelperNode(context);
        }
    }

    protected abstract static class GetTargetNode extends JavaScriptBaseNode {
        @Child private PropertyGetNode getTargetNode;

        public GetTargetNode(JSContext context) {
            getTargetNode = PropertyGetNode.createGetHidden(TARGET_ID, context);
        }

        public abstract IteratorRecord execute(JSDynamicObject generator);

        @Specialization
        protected IteratorRecord get(JSDynamicObject generator) {
            if (!generator.hasProperty(TARGET_ID)) {
                throw Errors.createTypeErrorIncompatibleReceiver(generator);
            }

            return (IteratorRecord) getTargetNode.getValue(generator);
        }

        public static GetTargetNode create(JSContext context) {
            return AsyncIteratorHelperPrototypeBuiltinsFactory.GetTargetNodeGen.create(context);
        }
    }

    protected abstract static class GetNextNode extends JavaScriptBaseNode {
        @Child private PropertyGetNode getTargetNode;

        public GetNextNode(JSContext context) {
            getTargetNode = PropertyGetNode.createGetHidden(IMPL_ID, context);
        }

        public abstract JSFunctionObject execute(JSDynamicObject generator);

        @Specialization
        protected JSFunctionObject get(JSDynamicObject generator) {
            if (!generator.hasProperty(TARGET_ID)) {
                throw Errors.createTypeErrorIncompatibleReceiver(generator);
            }

            return (JSFunctionObject) getTargetNode.getValue(generator);
        }

        public static GetNextNode create(JSContext context) {
            return AsyncIteratorHelperPrototypeBuiltinsFactory.GetNextNodeGen.create(context);
        }
    }

    public abstract static class IteratorHelperReturnNode extends JSBuiltinNode {
        @Child private GetTargetNode getTargetNode;
        @Child private AsyncIteratorClose asyncIteratorClose;

        protected IteratorHelperReturnNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getTargetNode = GetTargetNode.create(context);
            asyncIteratorClose = AsyncIteratorClose.create(context);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        public Object close(VirtualFrame frame, JSObject thisObj) {
            IteratorRecord iterated = getTargetNode.execute(thisObj);

            return asyncIteratorClose.execute(frame, iterated);
        }
    }

    protected static class AsyncIteratorClose extends JavaScriptBaseNode {
        @Child public GetMethodNode getReturnNode;
        @Child private PropertyGetNode getContructorNode;
        @Child public JSFunctionCallNode callNode;
        @Child public NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child public CreateIterResultObjectNode createIterResultObjectNode;
        @Child public PerformPromiseThenNode performPromiseThenNode;
        @Child public IsObjectNode isObjectNode;
        @Child public PropertySetNode setPromiseNode;

        private static final HiddenKey PROMISE_KEY = new HiddenKey("promiseKey");

        private final JSContext context;

        public AsyncIteratorClose(JSContext context) {
            this.context = context;

            getReturnNode = GetMethodNode.create(context, Strings.RETURN);
            getContructorNode = PropertyGetNode.create(JSObject.CONSTRUCTOR, context);
            callNode = JSFunctionCallNode.createCall();
            newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            performPromiseThenNode = PerformPromiseThenNode.create(context);
            isObjectNode = IsObjectNode.create();
            setPromiseNode = PropertySetNode.createSetHidden(PROMISE_KEY, context);
        }

        public static AsyncIteratorClose create(JSContext context) {
            return new AsyncIteratorClose(context);
        }

        public final Object execute(VirtualFrame frame, IteratorRecord iterated) {
            Object innerResult = getReturnNode.executeWithTarget(iterated.getIterator());
            if (innerResult == Undefined.instance) {
                PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
                callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), createIterResultObjectNode.execute(frame, Undefined.instance, true)));
                return promiseCapability.getPromise();
            }

            if (JSPromise.isJSPromise(innerResult) && getContructorNode.getValueOrDefault(innerResult, Undefined.instance) == getRealm().getPromiseConstructor()) {
                return performPromiseThenNode.execute((JSDynamicObject) innerResult, createCloseFunction(context, (JSDynamicObject) innerResult), Undefined.instance,
                                newPromiseCapabilityNode.executeDefault());
            }

            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
            if (isObjectNode.executeBoolean(innerResult)) {
                callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), createIterResultObjectNode.execute(frame, innerResult, true)));
            } else {
                callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(),
                                createIterResultObjectNode.execute(frame, Errors.createTypeErrorIterResultNotAnObject(innerResult, this), true)));
            }
            return promiseCapability.getPromise();
        }

        protected JSFunctionObject createCloseFunction(JSContext context, JSDynamicObject promise) {
            JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.AsyncIteratorClose, AsyncIteratorClose::createCloseFunctionImpl);
            JSFunctionObject function = JSFunction.create(getRealm(), functionData);
            setPromiseNode.setValue(function, promise);
            return function;
        }

        private static JSFunctionData createCloseFunctionImpl(JSContext context) {
            class AsyncIteratorCloseRootNode extends JavaScriptRootNode implements AsyncHandlerRootNode {
                @Child private JavaScriptNode valueNode;
                @Child public NewPromiseCapabilityNode newPromiseCapabilityNode;
                @Child public JSFunctionCallNode callNode;
                @Child public IsObjectNode isObjectNode;
                @Child public CreateIterResultObjectNode createIterResultObjectNode;

                AsyncIteratorCloseRootNode(JSContext context) {
                    valueNode = AccessIndexedArgumentNode.create(0);
                    newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
                    callNode = JSFunctionCallNode.createCall();
                    isObjectNode = IsObjectNode.create();
                    createIterResultObjectNode = CreateIterResultObjectNode.create(context);
                }

                @Override
                public Object execute(VirtualFrame frame) {
                    Object value = valueNode.execute(frame);
                    PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
                    if (isObjectNode.executeBoolean(value)) {
                        callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), createIterResultObjectNode.execute(frame, value, true)));
                    } else {
                        callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(),
                                        createIterResultObjectNode.execute(frame, Errors.createTypeErrorIterResultNotAnObject(value, this), true)));
                    }
                    return promiseCapability.getPromise();
                }

                @Override
                public AsyncStackTraceInfo getAsyncStackTraceInfo(JSFunctionObject handlerFunction) {
                    assert JSFunction.isJSFunction(handlerFunction) && ((RootCallTarget) JSFunction.getFunctionData(handlerFunction).getCallTarget()).getRootNode() == this;
                    JSDynamicObject promise = (JSDynamicObject) JSObjectUtil.getHiddenProperty(handlerFunction, PROMISE_KEY);
                    return new AsyncStackTraceInfo(promise, null);
                }
            }
            return JSFunctionData.createCallOnly(context, new AsyncIteratorCloseRootNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

    }

    public abstract static class AsyncIteratorHelperNextNode extends JSBuiltinNode {
        @Child private GetNextNode getNextNode;
        @Child public NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child public PerformPromiseThenNode performPromiseThenNode;
        @Child public PropertySetNode setIteratorPromiseNode;
        @Child public PropertyGetNode getPromiseNode;
        @Child public PropertySetNode setPromiseNode;

        protected AsyncIteratorHelperNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getNextNode = GetNextNode.create(context);
            newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            performPromiseThenNode = PerformPromiseThenNode.create(context);
            setIteratorPromiseNode = PropertySetNode.createSetHidden(AsyncIteratorPrototypeBuiltins.PROMISE_ID, context);
            getPromiseNode = PropertyGetNode.createGetHidden(NEXT_PROMISE_ID, context);
            setPromiseNode = PropertySetNode.createSetHidden(NEXT_PROMISE_ID, context);
        }

        @Specialization(guards = {"isJSObject(thisObj)"})
        protected Object next(JSObject thisObj) {
            JSDynamicObject promise = (JSDynamicObject) getPromiseNode.getValue(thisObj);

            Object impl = getNextNode.execute(thisObj);
            setIteratorPromiseNode.setValue(impl, promise);
            JSDynamicObject result = performPromiseThenNode.execute(promise, impl, Undefined.instance, newPromiseCapabilityNode.executeDefault());
            setPromiseNode.setValue(thisObj, result);
            return result;
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected Object incompatible(Object thisObj) {
            throw Errors.createTypeErrorGeneratorObjectExpected();
        }
    }
}
