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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArraySpeciesConstructorNode;
import com.oracle.truffle.js.builtins.PromisePrototypeBuiltinsFactory.CatchNodeGen;
import com.oracle.truffle.js.builtins.PromisePrototypeBuiltinsFactory.FinallyNodeGen;
import com.oracle.truffle.js.builtins.PromisePrototypeBuiltinsFactory.ThenNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessFunctionNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.control.ThrowNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseThenNode;
import com.oracle.truffle.js.nodes.promise.PromiseResolveNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains functions of the %PromisePrototype% object.
 */
public final class PromisePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<PromisePrototypeBuiltins.PromisePrototype> {
    protected PromisePrototypeBuiltins() {
        super(JSPromise.PROTOTYPE_NAME, PromisePrototype.class);
    }

    public enum PromisePrototype implements BuiltinEnum<PromisePrototype> {
        then(2),
        catch_(1),
        finally_(1);

        private final int length;

        PromisePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, PromisePrototype builtinEnum) {
        switch (builtinEnum) {
            case then:
                return ThenNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case catch_:
                return CatchNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case finally_:
                return FinallyNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic({JSPromise.class})
    public abstract static class PromiseMethodNode extends JSBuiltinNode {
        @Child private ArraySpeciesConstructorNode speciesConstructorNode;

        protected PromiseMethodNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.speciesConstructorNode = ArraySpeciesConstructorNode.create(context, false);
        }

        protected final DynamicObject speciesConstructor(DynamicObject promise) {
            return speciesConstructorNode.speciesConstructor(promise, getContext().getRealm().getPromiseConstructor());
        }
    }

    public abstract static class ThenNode extends PromiseMethodNode {
        @Child private NewPromiseCapabilityNode newPromiseCapability;
        @Child private PerformPromiseThenNode performPromiseThen;

        protected ThenNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.newPromiseCapability = NewPromiseCapabilityNode.create(context);
            this.performPromiseThen = PerformPromiseThenNode.create(context);
        }

        @Specialization(guards = "isJSPromise(promise)")
        protected DynamicObject doPromise(DynamicObject promise, Object onFulfilled, Object onRejected) {
            DynamicObject constructor = speciesConstructor(promise);
            getContext().notifyPromiseHook(-1 /* parent info */, promise);
            PromiseCapabilityRecord resultCapability = newPromiseCapability.execute(constructor);
            return performPromiseThen.execute(promise, onFulfilled, onRejected, resultCapability);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSPromise(thisObj)")
        protected DynamicObject doNotPromise(Object thisObj, Object onFulfilled, Object onRejected) {
            throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
        }
    }

    public abstract static class CatchNode extends JSBuiltinNode {
        @Child private PropertyGetNode getThen;
        @Child private JSFunctionCallNode callThen;

        protected CatchNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getThen = PropertyGetNode.create(JSPromise.THEN, false, context);
            this.callThen = JSFunctionCallNode.createCall();
        }

        @Specialization
        protected Object doObject(Object promise, Object onRejected) {
            return callThen.executeCall(JSArguments.create(promise, getThen.getValue(promise), Undefined.instance, onRejected));
        }
    }

    public abstract static class FinallyNode extends PromiseMethodNode {
        @Child private IsCallableNode isCallable = IsCallableNode.create();

        @Child private PropertyGetNode getThen;
        @Child private JSFunctionCallNode callThen;

        @Child private PropertySetNode setConstructor;
        @Child private PropertySetNode setOnFinally;

        static final HiddenKey VALUE_KEY = new HiddenKey("Value");

        protected FinallyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getThen = PropertyGetNode.create(JSPromise.THEN, false, context);
            this.callThen = JSFunctionCallNode.createCall();
        }

        @Specialization(guards = "isJSObject(promise)")
        protected Object doObject(DynamicObject promise, Object onFinally) {
            DynamicObject constructor = speciesConstructor(promise);
            assert JSRuntime.isConstructor(constructor);
            Object thenFinally;
            Object catchFinally;
            if (!isCallable.executeBoolean(onFinally)) {
                thenFinally = onFinally;
                catchFinally = onFinally;
            } else {
                thenFinally = createFinallyFunction(constructor, onFinally, true);
                catchFinally = createFinallyFunction(constructor, onFinally, false);
            }
            return callThen.executeCall(JSArguments.create(promise, getThen.getValue(promise), thenFinally, catchFinally));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSObject(thisObj)")
        protected DynamicObject doNotObject(Object thisObj, Object onFinally) {
            throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
        }

        private DynamicObject createFinallyFunction(DynamicObject constructor, Object onFinally, boolean thenFinally) {
            if (setConstructor == null || setOnFinally == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.setConstructor = insert(PropertySetNode.createSetHidden(JSPromise.PROMISE_FINALLY_CONSTRUCTOR, getContext()));
                this.setOnFinally = insert(PropertySetNode.createSetHidden(JSPromise.PROMISE_ON_FINALLY, getContext()));
            }
            JSFunctionData functionData;
            if (thenFinally) {
                functionData = getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseThenFinally, (c) -> createPromiseFinallyFunction(c, true));
            } else {
                functionData = getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseCatchFinally, (c) -> createPromiseFinallyFunction(c, false));
            }
            DynamicObject function = JSFunction.create(getContext().getRealm(), functionData);
            setConstructor.setValue(function, constructor);
            setOnFinally.setValue(function, onFinally);
            return function;
        }

        private static JSFunctionData createPromiseFinallyFunction(JSContext context, boolean thenFinally) {
            class PromiseFinallyRootNode extends JavaScriptRootNode {
                @Child private JavaScriptNode valueNode = AccessIndexedArgumentNode.create(0);
                @Child private PropertyGetNode getConstructor = PropertyGetNode.createGetHidden(JSPromise.PROMISE_FINALLY_CONSTRUCTOR, context);
                @Child private PropertyGetNode getOnFinally = PropertyGetNode.createGetHidden(JSPromise.PROMISE_ON_FINALLY, context);
                @Child private PromiseResolveNode promiseResolve = PromiseResolveNode.create(context);
                @Child private JSFunctionCallNode callFinally = JSFunctionCallNode.createCall();
                @Child private PropertyGetNode getThen = PropertyGetNode.create(JSPromise.THEN, false, context);
                @Child private JSFunctionCallNode callThen = JSFunctionCallNode.createCall();
                @Child private PropertySetNode setValue = PropertySetNode.createSetHidden(VALUE_KEY, context);

                @Override
                public Object execute(VirtualFrame frame) {
                    DynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                    DynamicObject onFinally = (DynamicObject) getOnFinally.getValue(functionObject);
                    assert JSRuntime.isCallable(onFinally);
                    Object result = callFinally.executeCall(JSArguments.createZeroArg(Undefined.instance, onFinally));
                    DynamicObject constructor = (DynamicObject) getConstructor.getValue(functionObject);
                    assert JSRuntime.isConstructor(constructor);
                    DynamicObject promise = promiseResolve.execute(constructor, result);
                    Object value = valueNode.execute(frame);
                    Object thunk = createHandlerFunction(value);
                    return callThen.executeCall(JSArguments.create(promise, getThen.getValue(promise), thunk));
                }

                private Object createHandlerFunction(Object value) {
                    JSFunctionData functionData;
                    if (thenFinally) {
                        functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseValueThunk, (c) -> createValueThunk(c));
                    } else {
                        functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseThrower, (c) -> createThrower(c));
                    }
                    DynamicObject function = JSFunction.create(context.getRealm(), functionData);
                    setValue.setValue(function, value);
                    return function;
                }
            }
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new PromiseFinallyRootNode());
            return JSFunctionData.createCallOnly(context, callTarget, 1, "");
        }

        static JSFunctionData createThrower(JSContext context) {
            return createThunkImpl(context, ThrowNode.create(PropertyNode.createProperty(context, AccessFunctionNode.create(), VALUE_KEY)));
        }

        static JSFunctionData createValueThunk(JSContext context) {
            return createThunkImpl(context, PropertyNode.createProperty(context, AccessFunctionNode.create(), VALUE_KEY));
        }

        private static JSFunctionData createThunkImpl(JSContext context, JavaScriptNode expression) {
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode() {
                @Child private JavaScriptNode body = expression;

                @Override
                public Object execute(VirtualFrame frame) {
                    return body.execute(frame);
                }
            });
            return JSFunctionData.createCallOnly(context, callTarget, 0, "");
        }
    }
}
