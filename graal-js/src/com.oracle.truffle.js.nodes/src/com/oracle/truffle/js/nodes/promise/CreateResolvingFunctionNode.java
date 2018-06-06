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
package com.oracle.truffle.js.nodes.promise;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.PromiseHook;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;

public class CreateResolvingFunctionNode extends JavaScriptBaseNode {

    static final class AlreadyResolved {
        boolean value;
    }

    static final HiddenKey ALREADY_RESOLVED_KEY = new HiddenKey("AlreadyResolved");
    static final HiddenKey PROMISE_KEY = new HiddenKey("Promise");
    static final HiddenKey THENABLE_KEY = new HiddenKey("thenable");
    static final HiddenKey THEN_KEY = new HiddenKey("then");

    private final JSContext context;
    @Child private PropertySetNode setAlreadyResolved;
    @Child private PropertySetNode setPromise;

    protected CreateResolvingFunctionNode(JSContext context) {
        this.context = context;
        this.setAlreadyResolved = PropertySetNode.createSetHidden(ALREADY_RESOLVED_KEY, context);
        this.setPromise = PropertySetNode.createSetHidden(PROMISE_KEY, context);
    }

    public static CreateResolvingFunctionNode create(JSContext context) {
        return new CreateResolvingFunctionNode(context);
    }

    public Pair<DynamicObject, DynamicObject> execute(DynamicObject promise) {
        AlreadyResolved alreadyResolved = new AlreadyResolved();
        DynamicObject resolve = createPromiseResolveFunction(promise, alreadyResolved);
        DynamicObject reject = createPromiseRejectFunction(promise, alreadyResolved);
        return new Pair<>(resolve, reject);
    }

    private DynamicObject createPromiseResolveFunction(DynamicObject promise, AlreadyResolved alreadyResolved) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseResolveFunction, (c) -> createPromiseResolveFunctionImpl(c));
        DynamicObject function = JSFunction.create(context.getRealm(), functionData);
        setPromise.setValue(function, promise);
        setAlreadyResolved.setValue(function, alreadyResolved);
        return function;
    }

    private static JSFunctionData createPromiseResolveFunctionImpl(JSContext context) {
        class PromiseResolveRootNode extends JavaScriptRootNode {
            @Child private JavaScriptNode resolutionNode = AccessIndexedArgumentNode.create(0);
            @Child private PropertyGetNode getPromise = PropertyGetNode.createGetHidden(PROMISE_KEY, context);
            @Child private PropertyGetNode getAlreadyResolved = PropertyGetNode.createGetHidden(ALREADY_RESOLVED_KEY, context);
            @Child private PropertyGetNode getThen = PropertyGetNode.create(JSPromise.THEN, false, context);
            @Child private IsCallableNode isCallable = IsCallableNode.create();
            @Child private IsObjectNode isObjectNode = IsObjectNode.create();
            @Child private FulfillPromiseNode fulfillPromise = FulfillPromiseNode.create(context);
            @Child private RejectPromiseNode rejectPromise;
            @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

            // PromiseResolveThenableJob
            @Child private PropertySetNode setPromise = PropertySetNode.createSetHidden(PROMISE_KEY, context);
            @Child private PropertySetNode setThenable = PropertySetNode.createSetHidden(THENABLE_KEY, context);
            @Child private PropertySetNode setThen = PropertySetNode.createSetHidden(THEN_KEY, context);

            @Override
            public Object execute(VirtualFrame frame) {
                DynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                AlreadyResolved alreadyResolved = (AlreadyResolved) getAlreadyResolved.getValue(functionObject);
                if (alreadyResolved.value) {
                    return Undefined.instance;
                }
                alreadyResolved.value = true;
                DynamicObject promise = (DynamicObject) getPromise.getValue(functionObject);
                context.notifyPromiseHook(PromiseHook.TYPE_RESOLVE, promise);

                Object resolution = resolutionNode.execute(frame);
                if (resolution == promise) {
                    enterErrorBranch();
                    return rejectPromise(promise, Errors.createTypeError("self resolution!"));
                }
                if (!isObjectNode.executeBoolean(resolution)) {
                    return fulfillPromise.execute(promise, resolution);
                }
                Object then;
                try {
                    then = getThen.getValue(resolution);
                } catch (Throwable ex) {
                    enterErrorBranch();
                    if (TryCatchNode.shouldCatch(ex)) {
                        return rejectPromise(promise, ex);
                    } else {
                        throw ex;
                    }
                }
                if (!isCallable.executeBoolean(then)) {
                    return fulfillPromise.execute(promise, resolution);
                }
                DynamicObject job = promiseResolveThenableJob(promise, resolution, then);
                context.promiseEnqueueJob(job);
                return Undefined.instance;
            }

            private Object rejectPromise(DynamicObject promise, Throwable exception) {
                Object error = getErrorObjectNode.execute(exception);
                return rejectPromise.execute(promise, error);
            }

            private void enterErrorBranch() {
                if (rejectPromise == null || getErrorObjectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    rejectPromise = insert(RejectPromiseNode.create(context));
                    getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
                }
            }

            private DynamicObject promiseResolveThenableJob(DynamicObject promise, Object thenable, Object then) {
                JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseResolveThenableJob, (c) -> createPromiseResolveThenableJobImpl(c));
                DynamicObject function = JSFunction.create(context.getRealm(), functionData);
                setPromise.setValue(function, promise);
                setThenable.setValue(function, thenable);
                setThen.setValue(function, then);
                return function;
            }
        }
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new PromiseResolveRootNode());
        return JSFunctionData.createCallOnly(context, callTarget, 1, "");
    }

    private static JSFunctionData createPromiseResolveThenableJobImpl(JSContext context) {
        class PromiseResolveThenableJob extends JavaScriptRootNode {
            @Child private PropertyGetNode getPromiseToResolve = PropertyGetNode.createGetHidden(PROMISE_KEY, context);
            @Child private PropertyGetNode getThenable = PropertyGetNode.createGetHidden(THENABLE_KEY, context);
            @Child private PropertyGetNode getThen = PropertyGetNode.createGetHidden(THEN_KEY, context);
            @Child private PromiseResolveThenableNode promiseResolveThenable = PromiseResolveThenableNode.create(context);

            @Override
            public Object execute(VirtualFrame frame) {
                DynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                DynamicObject promiseToResolve = (DynamicObject) getPromiseToResolve.getValue(functionObject);
                Object thenable = getThenable.getValue(functionObject);
                Object then = getThen.getValue(functionObject);
                return promiseResolveThenable.execute(promiseToResolve, thenable, then);
            }
        }
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new PromiseResolveThenableJob());
        return JSFunctionData.createCallOnly(context, callTarget, 0, "");
    }

    private DynamicObject createPromiseRejectFunction(DynamicObject promise, AlreadyResolved alreadyResolved) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseRejectFunction, (c) -> createPromiseRejectFunctionImpl(c));
        DynamicObject function = JSFunction.create(context.getRealm(), functionData);
        setPromise.setValue(function, promise);
        setAlreadyResolved.setValue(function, alreadyResolved);
        return function;
    }

    private static JSFunctionData createPromiseRejectFunctionImpl(JSContext context) {
        class PromiseRejectRootNode extends JavaScriptRootNode {
            @Child private JavaScriptNode reasonNode = AccessIndexedArgumentNode.create(0);
            @Child private PropertyGetNode getPromise = PropertyGetNode.createGetHidden(PROMISE_KEY, context);
            @Child private PropertyGetNode getAlreadyResolved = PropertyGetNode.createGetHidden(ALREADY_RESOLVED_KEY, context);
            @Child private RejectPromiseNode rejectPromise = RejectPromiseNode.create(context);

            @Override
            public Object execute(VirtualFrame frame) {
                DynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                AlreadyResolved alreadyResolved = (AlreadyResolved) getAlreadyResolved.getValue(functionObject);
                if (alreadyResolved.value) {
                    return Undefined.instance;
                }
                alreadyResolved.value = true;
                DynamicObject promise = (DynamicObject) getPromise.getValue(functionObject);

                Object reason = reasonNode.execute(frame);
                return rejectPromise.execute(promise, reason);
            }
        }
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new PromiseRejectRootNode());
        return JSFunctionData.createCallOnly(context, callTarget, 1, "");
    }
}
