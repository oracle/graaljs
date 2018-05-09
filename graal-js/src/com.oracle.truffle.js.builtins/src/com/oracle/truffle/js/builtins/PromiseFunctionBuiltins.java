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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.PromiseFunctionBuiltinsFactory.PromiseAllOrRaceNodeGen;
import com.oracle.truffle.js.builtins.PromiseFunctionBuiltinsFactory.RejectNodeGen;
import com.oracle.truffle.js.builtins.PromiseFunctionBuiltinsFactory.ResolveNodeGen;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseAllNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseAllOrRaceNode;
import com.oracle.truffle.js.nodes.promise.PerformPromiseRaceNode;
import com.oracle.truffle.js.nodes.promise.PromiseResolveNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains functions of the %Promise% constructor function object.
 */
public final class PromiseFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<PromiseFunctionBuiltins.PromiseFunction> {
    protected PromiseFunctionBuiltins() {
        super(JSPromise.CLASS_NAME, PromiseFunction.class);
    }

    public enum PromiseFunction implements BuiltinEnum<PromiseFunction> {
        all(1),
        race(1),
        reject(1),
        resolve(1);

        private final int length;

        PromiseFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, PromiseFunction builtinEnum) {
        switch (builtinEnum) {
            case all:
                return PromiseAllOrRaceNodeGen.create(context, builtin, PerformPromiseAllNode.create(context), args().withThis().fixedArgs(1).createArgumentNodes(context));
            case race:
                return PromiseAllOrRaceNodeGen.create(context, builtin, PerformPromiseRaceNode.create(context), args().withThis().fixedArgs(1).createArgumentNodes(context));
            case reject:
                return RejectNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case resolve:
                return ResolveNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class PromiseAllOrRaceNode extends JSBuiltinNode {
        @Child private NewPromiseCapabilityNode newPromiseCapability;
        @Child private GetIteratorNode getIterator;
        @Child private PerformPromiseAllOrRaceNode performPromiseOp;
        @Child private JSFunctionCallNode callReject;
        @Child private IteratorCloseNode iteratorClose;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

        protected PromiseAllOrRaceNode(JSContext context, JSBuiltin builtin, PerformPromiseAllOrRaceNode performPromiseOp) {
            super(context, builtin);
            this.newPromiseCapability = NewPromiseCapabilityNode.create(context);
            this.getIterator = GetIteratorNode.create(context);
            this.performPromiseOp = performPromiseOp;
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected DynamicObject doObject(DynamicObject thisObj, Object iterable) {
            DynamicObject constructor = thisObj;
            PromiseCapabilityRecord promiseCapability = newPromiseCapability.execute(constructor);
            IteratorRecord iteratorRecord;
            try {
                DynamicObject iterator = getIterator.execute(iterable);
                iteratorRecord = IteratorRecord.create(iterator, false);
            } catch (Throwable ex) {
                if (shouldCatch(ex)) {
                    return rejectPromise(getErrorObjectNode.execute(ex), promiseCapability);
                } else {
                    throw ex;
                }
            }
            try {
                return performPromiseOp.execute(iteratorRecord, constructor, promiseCapability);
            } catch (Throwable ex) {
                if (shouldCatch(ex)) {
                    if (!iteratorRecord.isDone()) {
                        iteratorClose(iteratorRecord);
                    }
                    return rejectPromise(getErrorObjectNode.execute(ex), promiseCapability);
                } else {
                    throw ex;
                }
            }
        }

        protected DynamicObject rejectPromise(Object value, PromiseCapabilityRecord promiseCapability) {
            if (callReject == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callReject = insert(JSFunctionCallNode.createCall());
            }
            callReject.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), value));
            return promiseCapability.getPromise();
        }

        private void iteratorClose(IteratorRecord iteratorRecord) {
            if (iteratorClose == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorClose = insert(IteratorCloseNode.create(getContext()));
            }
            iteratorClose.executeAbrupt(iteratorRecord.getIterator());
        }

        private boolean shouldCatch(Throwable exception) {
            if (getErrorObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(getContext()));
            }
            return TryCatchNode.shouldCatch(exception);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSObject(thisObj)")
        protected DynamicObject doNotObject(Object thisObj, Object iterable) {
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

        @Specialization(guards = "isJSObject(constructor)")
        protected DynamicObject doObject(DynamicObject constructor, Object reason) {
            PromiseCapabilityRecord promiseCapability = newPromiseCapability.execute(constructor);
            callReject.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), reason));
            return promiseCapability.getPromise();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSObject(thisObj)")
        protected DynamicObject doNotObject(Object thisObj, Object iterable) {
            throw Errors.createTypeError("Cannot reject promise from this type");
        }
    }

    public abstract static class ResolveNode extends JSBuiltinNode {
        @Child private PromiseResolveNode promiseResolve;

        protected ResolveNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.promiseResolve = PromiseResolveNode.create(context);
        }

        @Specialization(guards = "isJSObject(constructor)")
        protected DynamicObject doObject(DynamicObject constructor, Object value) {
            return promiseResolve.execute(constructor, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSObject(thisObj)")
        protected DynamicObject doNotObject(Object thisObj, Object iterable) {
            throw Errors.createTypeError("Cannot resolve promise from this type");
        }
    }
}
