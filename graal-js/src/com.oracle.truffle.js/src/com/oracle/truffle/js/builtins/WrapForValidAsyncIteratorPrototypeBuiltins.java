/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IteratorNextNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidAsyncIterator;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidAsyncIteratorObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class WrapForValidAsyncIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WrapForValidAsyncIteratorPrototypeBuiltins.WrapForWrapForAsyncIterator> {
    public static final JSBuiltinsContainer BUILTINS = new WrapForValidAsyncIteratorPrototypeBuiltins();

    public static final TruffleString PROTOTYPE_NAME = Strings.constant("%WrapForValidAsyncIteratorPrototype%");

    protected WrapForValidAsyncIteratorPrototypeBuiltins() {
        super(PROTOTYPE_NAME, WrapForWrapForAsyncIterator.class);
    }

    public enum WrapForWrapForAsyncIterator implements BuiltinEnum<WrapForWrapForAsyncIterator> {
        next(0),
        return_(0);

        private final int length;

        WrapForWrapForAsyncIterator(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, WrapForWrapForAsyncIterator builtinEnum) {
        switch (builtinEnum) {
            case next:
                return WrapForValidAsyncIteratorPrototypeBuiltinsFactory.WrapForAsyncIteratorNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case return_:
                return WrapForValidAsyncIteratorPrototypeBuiltinsFactory.WrapForAsyncIteratorReturnNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic({JSWrapForValidAsyncIterator.class})
    public abstract static class WrapForAsyncIteratorNextNode extends JSBuiltinNode {
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child private IteratorNextNode iteratorNextNode;
        @Child private PropertyGetNode getConstructorNode;
        @Child private JSFunctionCallNode callNode;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

        public WrapForAsyncIteratorNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            iteratorNextNode = IteratorNextNode.create();
            getConstructorNode = PropertyGetNode.create(JSObject.CONSTRUCTOR, context);
            callNode = JSFunctionCallNode.createCall();
        }

        @Specialization
        protected JSDynamicObject next(JSWrapForValidAsyncIteratorObject thisObj) {
            try {
                Object result = iteratorNextNode.execute(thisObj.getIterated());
                if (JSPromise.isJSPromise(result) && getConstructorNode.getValue(result) == getRealm().getPromiseConstructor()) {
                    return (JSDynamicObject) result;
                }

                PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
                callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), result));
                return promiseCapability.getPromise();
            } catch (AbstractTruffleException ex) {
                if (getErrorObjectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(getContext()));
                }
                Object error = getErrorObjectNode.execute(ex);
                PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
                callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), error));
                return promiseCapability.getPromise();
            }
        }

        @Specialization(guards = "!isWrapForAsyncIterator(thisObj)")
        protected JSDynamicObject incompatible(Object thisObj) {
            if (getErrorObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(getContext()));
            }
            Object error = getErrorObjectNode.execute(Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getName(), thisObj));
            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
            callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), error));
            return promiseCapability.getPromise();
        }
    }

    @ImportStatic({JSWrapForValidAsyncIterator.class})
    public abstract static class WrapForAsyncIteratorReturnNode extends JSBuiltinNode {
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child private JSFunctionCallNode callNode;
        @Child private CreateIterResultObjectNode createIterResultObjectNode;
        @Child private GetMethodNode getReturnNode;
        @Child private JSFunctionCallNode returnMethodCallNode;
        @Child private PropertyGetNode getConstructorNode;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

        public WrapForAsyncIteratorReturnNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            this.callNode = JSFunctionCallNode.createCall();
            this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            this.getReturnNode = GetMethodNode.create(context, Strings.RETURN);
            this.returnMethodCallNode = JSFunctionCallNode.createCall();
            this.getConstructorNode = PropertyGetNode.create(JSObject.CONSTRUCTOR, context);

        }

        @Specialization
        protected JSDynamicObject performReturn(JSWrapForValidAsyncIteratorObject thisObj) {
            JSRealm realm = getRealm();
            try {
                Object returnMethod = getReturnNode.executeWithTarget(thisObj.getIterated().getIterator());
                if (returnMethod == Undefined.instance) {
                    PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.execute(realm.getPromiseConstructor());
                    callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), createIterResultObjectNode.execute(Undefined.instance, true)));
                    return promiseCapability.getPromise();
                } else {
                    Object result = returnMethodCallNode.executeCall(JSArguments.createZeroArg(thisObj.getIterated().getIterator(), returnMethod));
                    if (JSPromise.isJSPromise(result)) {
                        Object otherConstructor = getConstructorNode.getValue(result);
                        if (otherConstructor == realm.getPromiseConstructor()) {
                            return (JSDynamicObject) result;
                        }
                    }
                    PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.execute(realm.getPromiseConstructor());
                    callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getResolve(), result));
                    return promiseCapability.getPromise();
                }
            } catch (AbstractTruffleException ex) {
                if (getErrorObjectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(getContext()));
                }
                Object error = getErrorObjectNode.execute(ex);
                PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.execute(realm.getPromiseConstructor());
                callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), error));
                return promiseCapability.getPromise();
            }
        }

        @Specialization(guards = "!isWrapForAsyncIterator(thisObj)")
        protected JSDynamicObject incompatible(Object thisObj) {
            if (getErrorObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(getContext()));
            }
            Object error = getErrorObjectNode.execute(Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getName(), thisObj));
            PromiseCapabilityRecord promiseCapability = newPromiseCapabilityNode.executeDefault();
            callNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), error));
            return promiseCapability.getPromise();
        }
    }
}
