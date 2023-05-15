/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.builtins.AsyncContextFunctionBuiltinsFactory.AsyncContextWrapNodeGen;
import com.oracle.truffle.js.builtins.FunctionPrototypeBuiltins.CopyFunctionNameAndLengthNode;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSShadowRealm;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Contains built-in functions of the {@code %AsyncContext%} constructor.
 */
public final class AsyncContextFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncContextFunctionBuiltins.AsyncContextFunction> {

    public static final JSBuiltinsContainer BUILTINS = new AsyncContextFunctionBuiltins();

    protected AsyncContextFunctionBuiltins() {
        super(JSShadowRealm.PROTOTYPE_NAME, AsyncContextFunction.class);
    }

    public enum AsyncContextFunction implements BuiltinEnum<AsyncContextFunction> {
        wrap(1);

        private final int length;

        AsyncContextFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, AsyncContextFunction builtinEnum) {
        switch (builtinEnum) {
            case wrap:
                return AsyncContextWrapNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class AsyncContextWrapNode extends JSBuiltinNode {

        public AsyncContextWrapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isCallable.executeBoolean(target)", limit = "1")
        protected Object doCallable(Object target,
                        @Cached @SuppressWarnings("unused") IsCallableNode isCallable,
                        @Cached GetPrototypeNode getPrototypeOf,
                        @Cached("create(getContext())") CopyFunctionNameAndLengthNode copyNameAndLengthNode) {
            JSRealm realm = getRealm();
            var snapshot = realm.getAgent().getAsyncContextMapping();
            assert JSRuntime.isCallable(target);
            // AsyncContextWrappedFunctionCreate(target, snapshot).
            JSDynamicObject proto = getPrototypeOf.execute(target);
            JSFunctionData asyncContextWrappedFunctionCall = getContext().getOrCreateBuiltinFunctionData(
                            BuiltinFunctionKey.AsyncContextWrappedFunctionCall, AsyncContextFunctionBuiltins::createWrappedFunctionImpl);
            var wrapped = JSFunction.createAsyncContextWrapped(getContext(), realm, asyncContextWrappedFunctionCall, target, snapshot);
            if (proto != realm.getFunctionPrototype()) {
                JSObject.setPrototype(wrapped, proto);
            }
            copyNameAndLengthNode.execute(wrapped, target, Strings.WRAPPED_SPC, 0);
            return wrapped;
        }

        @Fallback
        protected Object doNotCallable(Object target) {
            throw Errors.createTypeErrorNotAFunction(target);
        }
    }

    private static JSFunctionData createWrappedFunctionImpl(JSContext context) {
        final class WrappedFunctionRootNode extends JavaScriptRootNode {
            @Child private JSFunctionCallNode callWrappedTargetFunction = JSFunctionCallNode.createCall();

            protected WrappedFunctionRootNode(JavaScriptLanguage lang) {
                super(lang, null, null);
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object[] args = frame.getArguments();
                Object thisArgument = JSArguments.getThisObject(args);
                var thisFunction = (JSFunctionObject.AsyncContextWrapped) JSArguments.getFunctionObject(args);
                var target = thisFunction.getWrappedTargetFunction();
                var snapshot = thisFunction.getAsyncContextSnapshot();
                assert JSRuntime.isCallable(target) : target;
                JSAgent agent = getRealm().getAgent();
                var previousContextMapping = agent.asyncContextSwap(snapshot);
                try {
                    return callWrappedTargetFunction.executeCall(JSArguments.create(thisArgument, target, JSArguments.extractUserArguments(args)));
                } finally {
                    agent.asyncContextSwap(previousContextMapping);
                }
            }
        }
        return JSFunctionData.createCallOnly(context, new WrappedFunctionRootNode(context.getLanguage()).getCallTarget(), 0, Strings.EMPTY_STRING);
    }
}
