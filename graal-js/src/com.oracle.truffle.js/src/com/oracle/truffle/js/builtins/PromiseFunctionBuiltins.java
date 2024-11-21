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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.builtins.PromiseFunctionBuiltinsFactory.PromiseCombinatorNodeGen;
import com.oracle.truffle.js.builtins.PromiseFunctionBuiltinsFactory.PromiseTryNodeGen;
import com.oracle.truffle.js.builtins.PromiseFunctionBuiltinsFactory.RejectNodeGen;
import com.oracle.truffle.js.builtins.PromiseFunctionBuiltinsFactory.ResolveNodeGen;
import com.oracle.truffle.js.builtins.PromiseFunctionBuiltinsFactory.WithResolversNodeGen;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseAllNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseAllSettledNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseAnyNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseCombinatorNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseRaceNode;
import com.oracle.truffle.js.nodes.promise.PromiseResolveNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains functions of the %Promise% constructor function object.
 */
public final class PromiseFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<PromiseFunctionBuiltins.PromiseFunction> {

    public static final JSBuiltinsContainer BUILTINS = new PromiseFunctionBuiltins();

    protected PromiseFunctionBuiltins() {
        super(JSPromise.CLASS_NAME, PromiseFunction.class);
    }

    public enum PromiseFunction implements BuiltinEnum<PromiseFunction> {
        all(1),
        race(1),
        reject(1),
        resolve(1),

        allSettled(1),
        any(1),

        withResolvers(0),

        try_(1);

        private final int length;

        PromiseFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            return switch (this) {
                case any -> JSConfig.ECMAScript2021;
                case allSettled -> JSConfig.ECMAScript2020;
                case withResolvers -> JSConfig.ECMAScript2024;
                case try_ -> JSConfig.ECMAScript2025;
                default -> JSConfig.ECMAScript2015;
            };
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, PromiseFunction builtinEnum) {
        switch (builtinEnum) {
            case all:
                return PromiseCombinatorNodeGen.create(context, builtin, PerformPromiseAllNode.create(context), args().withThis().fixedArgs(1).createArgumentNodes(context));
            case allSettled:
                return PromiseCombinatorNodeGen.create(context, builtin, PerformPromiseAllSettledNode.create(context), args().withThis().fixedArgs(1).createArgumentNodes(context));
            case any:
                return PromiseCombinatorNodeGen.create(context, builtin, PerformPromiseAnyNode.create(context), args().withThis().fixedArgs(1).createArgumentNodes(context));
            case race:
                return PromiseCombinatorNodeGen.create(context, builtin, PerformPromiseRaceNode.create(context), args().withThis().fixedArgs(1).createArgumentNodes(context));
            case reject:
                return RejectNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case resolve:
                return ResolveNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case withResolvers:
                return WithResolversNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case try_:
                return PromiseTryNodeGen.create(context, builtin, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class PromiseCombinatorNode extends JSBuiltinNode {
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child private PropertyGetNode getResolve;
        @Child private IsCallableNode isCallable;
        @Child private PerformPromiseCombinatorNode performPromiseOpNode;
        @Child private JSFunctionCallNode callRejectNode;
        @Child private IteratorCloseNode iteratorCloseNode;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

        protected PromiseCombinatorNode(JSContext context, JSBuiltin builtin, PerformPromiseCombinatorNode performPromiseOp) {
            super(context, builtin);
            this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            this.getResolve = PropertyGetNode.create(JSPromise.RESOLVE, context);
            this.isCallable = IsCallableNode.create();
            this.performPromiseOpNode = performPromiseOp;
        }

        @Specialization
        protected Object doObject(JSObject constructor, Object iterable,
                        @Cached(inline = true) GetIteratorNode getIteratorNode) {
            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.execute(constructor);
            Object promiseResolve;
            IteratorRecord iteratorRecord;
            try {
                promiseResolve = getPromiseResolve(constructor);
                iteratorRecord = getIteratorNode.execute(this, iterable);
            } catch (AbstractTruffleException ex) {
                return rejectPromise(ex, promiseCapability);
            }
            try {
                return performPromiseOpNode.execute(iteratorRecord, constructor, promiseCapability, promiseResolve);
            } catch (AbstractTruffleException ex) {
                if (!iteratorRecord.isDone()) {
                    iteratorClose(iteratorRecord);
                }
                return rejectPromise(ex, promiseCapability);
            }
        }

        private Object getPromiseResolve(JSDynamicObject constructor) {
            assert JSRuntime.isConstructor(constructor);
            Object promiseResolve = getResolve.getValue(constructor);
            if (!isCallable.executeBoolean(promiseResolve)) {
                throw Errors.createTypeErrorNotAFunction(promiseResolve);
            }
            return promiseResolve;
        }

        protected JSDynamicObject rejectPromise(AbstractTruffleException ex, PromiseCapabilityRecord promiseCapability) {
            if (callRejectNode == null || getErrorObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callRejectNode = insert(JSFunctionCallNode.createCall());
                getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(getContext()));
            }
            Object error = getErrorObjectNode.execute(ex);
            callRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), error));
            return promiseCapability.getPromise();
        }

        private void iteratorClose(IteratorRecord iteratorRecord) {
            if (iteratorCloseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
            }
            iteratorCloseNode.executeAbrupt(iteratorRecord.getIterator());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSObject(thisObj)")
        protected JSDynamicObject doNotObject(Object thisObj, Object iterable) {
            throw Errors.createTypeError("Cannot create promise from this type");
        }
    }

    public abstract static class RejectNode extends JSBuiltinNode {
        @Child private NewPromiseCapabilityNode newPromiseCapability;
        @Child private JSFunctionCallNode callReject;

        protected RejectNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.newPromiseCapability = NewPromiseCapabilityNode.create(context);
            this.callReject = JSFunctionCallNode.createCall();
        }

        @Specialization
        protected JSDynamicObject doObject(JSObject constructor, Object reason) {
            PromiseCapabilityRecord promiseCapability = newPromiseCapability.execute(constructor);
            callReject.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), reason));
            return promiseCapability.getPromise();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSObject(thisObj)")
        protected JSDynamicObject doNotObject(Object thisObj, Object iterable) {
            throw Errors.createTypeError("Cannot reject promise from this type");
        }
    }

    public abstract static class ResolveNode extends JSBuiltinNode {
        @Child private PromiseResolveNode promiseResolve;

        protected ResolveNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.promiseResolve = PromiseResolveNode.create(context);
        }

        @Specialization
        protected JSDynamicObject doObject(JSObject constructor, Object value) {
            return promiseResolve.execute(constructor, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSObject(thisObj)")
        protected JSDynamicObject doNotObject(Object thisObj, Object iterable) {
            throw Errors.createTypeError("Cannot resolve promise from this type");
        }
    }

    public abstract static class WithResolversNode extends JSBuiltinNode {
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child private CreateObjectNode createObjectNode;
        @Child private CreateDataPropertyNode definePromisePropertyNode;
        @Child private CreateDataPropertyNode defineResolvePropertyNode;
        @Child private CreateDataPropertyNode defineRejectPropertyNode;

        protected WithResolversNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            this.createObjectNode = CreateObjectNode.create(context);
            this.definePromisePropertyNode = CreateDataPropertyNode.create(context, Strings.PROMISE);
            this.defineResolvePropertyNode = CreateDataPropertyNode.create(context, Strings.RESOLVE);
            this.defineRejectPropertyNode = CreateDataPropertyNode.create(context, Strings.REJECT);
        }

        @Specialization
        protected JSObject withResolvers(VirtualFrame frame, Object thiz) {
            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.execute(thiz);
            JSObject obj = createObjectNode.execute(frame);
            definePromisePropertyNode.executeVoid(obj, promiseCapability.getPromise());
            defineResolvePropertyNode.executeVoid(obj, promiseCapability.getResolve());
            defineRejectPropertyNode.executeVoid(obj, promiseCapability.getReject());
            return obj;
        }

    }

    public abstract static class PromiseTryNode extends JSBuiltinNode {
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child private JSFunctionCallNode callCallbackFnNode;
        @Child private JSFunctionCallNode callResolveNode;
        @Child private JSFunctionCallNode callRejectNode;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

        protected PromiseTryNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            this.callCallbackFnNode = JSFunctionCallNode.createCall();
            this.callResolveNode = JSFunctionCallNode.createCall();
        }

        @Specialization
        protected final Object doObject(JSObject constructor, Object callbackfn, Object[] args) {
            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.execute(constructor);
            try {
                Object status = callCallbackFnNode.executeCall(JSArguments.create(Undefined.instance, callbackfn, args));
                callResolveNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), status));
            } catch (AbstractTruffleException ex) {
                rejectPromise(ex, promiseCapability);
            }
            return promiseCapability.getPromise();
        }

        private void rejectPromise(AbstractTruffleException ex, PromiseCapabilityRecord promiseCapability) {
            if (callRejectNode == null || getErrorObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callRejectNode = insert(JSFunctionCallNode.createCall());
                getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(getContext()));
            }
            Object error = getErrorObjectNode.execute(ex);
            callRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), error));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSObject(thisObj)")
        protected static Object doNotObject(Object thisObj, Object callbackfn, Object[] args) {
            throw Errors.createTypeError("Cannot create promise from this type");
        }
    }
}
