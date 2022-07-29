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

import java.util.EnumSet;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorNextNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSIterator;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSWrapForAsyncIteratorObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class WrapForAsyncIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WrapForAsyncIteratorPrototypeBuiltins.WrapForWrapForAsyncIterator> {
    public static final JSBuiltinsContainer BUILTINS = new WrapForAsyncIteratorPrototypeBuiltins();

    protected WrapForAsyncIteratorPrototypeBuiltins() {
        super(JSIterator.CLASS_NAME, WrapForWrapForAsyncIterator.class); // TODO: async
    }

    public enum WrapForWrapForAsyncIterator implements BuiltinEnum<WrapForWrapForAsyncIterator> {
        next(1),
        return_(1);

        private final int length;

        WrapForWrapForAsyncIterator(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (EnumSet.of(next, return_).contains(this)) {
                return JSConfig.StagingECMAScriptVersion;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, WrapForWrapForAsyncIterator builtinEnum) {
        switch (builtinEnum) {
            case next:
                return WrapForAsyncIteratorPrototypeBuiltinsFactory.WrapForAsyncIteratorNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case return_:
                return WrapForAsyncIteratorPrototypeBuiltinsFactory.WrapForAsyncIteratorReturnNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }

        assert false : "Unreachable! Missing entries in switch?";
        return null;
    }

    public abstract static class WrapForAsyncIteratorNextNode extends JSBuiltinNode {
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child private IteratorNextNode iteratorNextNode;
        @Child private PropertyGetNode getContructorNode;
        @Child private JSFunctionCallNode callNode;

        public WrapForAsyncIteratorNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            iteratorNextNode = IteratorNextNode.create();
            getContructorNode = PropertyGetNode.create(JSObject.CONSTRUCTOR, context);
            callNode = JSFunctionCallNode.createCall();
        }

        @Specialization
        protected JSDynamicObject next(JSWrapForAsyncIteratorObject thisObj) {

            try {
                Object result = iteratorNextNode.execute(thisObj.getIterated());
                if (JSPromise.isJSPromise(result) && getContructorNode.getValueOrDefault(result, Undefined.instance) == getRealm().getPromiseConstructor()) {
                    return (JSDynamicObject) result;
                }

                PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
                callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), result));
                return promiseCapability.getPromise();
            } catch (AbstractTruffleException ex) {
                PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
                callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), ex));
                return promiseCapability.getPromise();
            }
        }

        @Specialization
        protected JSDynamicObject incompatible(Object thisObj) {
            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
            callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), Errors.createTypeErrorIncompatibleReceiver(thisObj)));
            return promiseCapability.getPromise();
        }
    }

    public abstract static class WrapForAsyncIteratorReturnNode extends JSBuiltinNode {
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child private JSFunctionCallNode callNode;
        @Child private AsyncIteratorCloseNode iteratorCloseNode;
        @Child private CreateIterResultObjectNode createIterResultObjectNode;
        @Child private PerformPromiseThenNode performPromiseThenNode;
        @Child private IsObjectNode isObjectNode;
        @Child private AsyncIteratorCloseResultCheckNode asyncIteratorCloseResultCheckNode;

        public WrapForAsyncIteratorReturnNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            performPromiseThenNode = PerformPromiseThenNode.create(context);
            newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            callNode = JSFunctionCallNode.createCall();
            iteratorCloseNode = AsyncIteratorCloseNode.create(context);
            createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            isObjectNode = IsObjectNode.create();
            asyncIteratorCloseResultCheckNode = AsyncIteratorCloseResultCheckNode.create(context);

        }

        @Specialization
        protected Object performReturn(VirtualFrame frame, JSWrapForAsyncIteratorObject thisObj) {
            try {
                Object innerResult = iteratorCloseNode.execute(thisObj.getIterated().getIterator());
                if (JSPromise.isJSPromise(innerResult)) {
                    JSFunctionData functionData = getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseReturnWrapper,
                                    WrapForAsyncIteratorReturnNode::createIteratorCloseResultCheckImpl);
                    return performPromiseThenNode.execute((JSDynamicObject) innerResult, JSFunction.create(getRealm(), functionData), Undefined.instance, newPromiseCapabilityNode.executeDefault());
                }

                return asyncIteratorCloseResultCheckNode.execute(frame, innerResult);
            } catch (AbstractTruffleException e) {
                PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
                callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), e));
                return promiseCapability.getPromise();
            }
        }

        @Specialization
        protected JSDynamicObject incompatible(Object thisObj) {
            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.execute(getRealm().getPromiseConstructor());
            callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), Errors.createTypeErrorIncompatibleReceiver(thisObj)));
            return promiseCapability.getPromise();
        }

        private static JSFunctionData createIteratorCloseResultCheckImpl(JSContext context) {
            return JSFunctionData.createCallOnly(context, new AsyncIteratorCloseResultCheckNode(context).getCallTarget(), 1, Strings.EMPTY_STRING);
        }

        protected static class AsyncIteratorCloseNode extends JavaScriptBaseNode {
            @Child private GetMethodNode getReturnNode;
            @Child private JSFunctionCallNode methodCallNode;
            @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
            @Child private JSFunctionCallNode callNode;

            protected AsyncIteratorCloseNode(JSContext context) {
                this.getReturnNode = GetMethodNode.create(context, Strings.RETURN);
                this.methodCallNode = JSFunctionCallNode.createCall();
                this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
                this.callNode = JSFunctionCallNode.createCall();
            }

            public static AsyncIteratorCloseNode create(JSContext context) {
                return new AsyncIteratorCloseNode(context);
            }

            public final Object execute(JSDynamicObject iterator) {
                Object returnMethod = getReturnNode.executeWithTarget(iterator);
                if (returnMethod == Undefined.instance) {
                    PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.execute(getRealm().getPromiseConstructor());
                    callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), Undefined.instance));
                    return promiseCapability.getPromise();
                }

                return methodCallNode.executeCall(JSArguments.createZeroArg(iterator, returnMethod));
            }
        }

        protected static class AsyncIteratorCloseResultCheckNode extends JavaScriptRootNode {
            @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
            @Child private IsObjectNode isObjectNode;
            @Child private JSFunctionCallNode callNode;
            @Child private CreateIterResultObjectNode createIterResultObjectNode;

            protected AsyncIteratorCloseResultCheckNode(JSContext context) {
                newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
                isObjectNode = IsObjectNode.create();
                callNode = JSFunctionCallNode.createCall();
                createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            }

            protected static AsyncIteratorCloseResultCheckNode create(JSContext context) {
                return new AsyncIteratorCloseResultCheckNode(context);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object innerResult = JSRuntime.getArgOrUndefined(JSFrameUtil.getArgumentsArray(frame), 0);
                return execute(frame, innerResult);
            }

            public final Object execute(VirtualFrame frame, Object innerResult) {
                PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.execute(getRealm().getPromiseConstructor());
                if (!isObjectNode.executeBoolean(innerResult)) {
                    callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), Errors.createTypeErrorIterResultNotAnObject(innerResult, this)));
                } else {
                    Object result = this.createIterResultObjectNode.execute(frame, innerResult, true);
                    callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), result));
                }
                return promiseCapability.getPromise();
            }
        }
    }
}
