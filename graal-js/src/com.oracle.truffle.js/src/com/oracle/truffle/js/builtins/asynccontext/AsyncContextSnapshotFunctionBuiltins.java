/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.asynccontext;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.FunctionPrototypeBuiltins.CopyFunctionNameAndLengthNode;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.asynccontext.AsyncContextSnapshotFunctionBuiltinsFactory.WrapNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
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
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.asynccontext.JSAsyncContextSnapshot;
import com.oracle.truffle.js.runtime.objects.AsyncContext;

/**
 * Contains functions of the {@code %AsyncContext.Snapshot%} constructor.
 */
public final class AsyncContextSnapshotFunctionBuiltins extends JSBuiltinsContainer.Lambda {

    private static final HiddenKey ASYNC_CONTEXT_MAPPING = new HiddenKey("AsyncContextSnapshotWrapMapping");
    private static final HiddenKey TARGET_FUNCTION = new HiddenKey("AsyncContextSnapshotWrapTarget");
    private static final TruffleString WRAP = Strings.constant("wrap");
    private static final TruffleString WRAPPED_SPC = Strings.constant("wrapped ");

    public static final JSBuiltinsContainer BUILTINS = new AsyncContextSnapshotFunctionBuiltins();

    private AsyncContextSnapshotFunctionBuiltins() {
        super(JSAsyncContextSnapshot.CLASS_NAME);
        defineFunction(WRAP, 1, (context, builtin) -> WrapNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context)));
    }

    public abstract static class WrapNode extends JSBuiltinNode {
        @Child private PropertySetNode setAsyncContextMapping;
        @Child private PropertySetNode setTargetFunction;

        public WrapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.setAsyncContextMapping = PropertySetNode.createSetHidden(ASYNC_CONTEXT_MAPPING, context);
            this.setTargetFunction = PropertySetNode.createSetHidden(TARGET_FUNCTION, context);
        }

        @Specialization
        protected final JSFunctionObject wrap(Object fn,
                        @Cached IsCallableNode isCallable,
                        @Cached CopyFunctionNameAndLengthNode copyNameAndLength,
                        @Cached InlinedBranchProfile errorProfile) {
            if (!isCallable.executeBoolean(fn)) {
                errorProfile.enter(this);
                throw Errors.createTypeErrorNotAFunction(fn, this);
            }
            JSContext context = getContext();
            JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.AsyncContextSnapshotWrap,
                            AsyncContextSnapshotFunctionBuiltins::createWrappedFunctionImpl);
            JSFunctionObject wrapped = JSFunction.create(getRealm(), functionData);
            setAsyncContextMapping.setValue(wrapped, getRealm().getAgent().getAsyncContextMapping());
            setTargetFunction.setValue(wrapped, fn);
            copyNameAndLength.execute(wrapped, fn, WRAPPED_SPC, 0);
            return wrapped;
        }
    }

    private static JSFunctionData createWrappedFunctionImpl(JSContext context) {
        class WrappedFunctionRootNode extends JavaScriptRootNode {
            @Child private PropertyGetNode getAsyncContextMapping = PropertyGetNode.createGetHidden(ASYNC_CONTEXT_MAPPING, context);
            @Child private PropertyGetNode getTargetFunction = PropertyGetNode.createGetHidden(TARGET_FUNCTION, context);
            @Child private JSFunctionCallNode callTarget = JSFunctionCallNode.createCall();

            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                JSFunctionObject functionObject = (JSFunctionObject) JSArguments.getFunctionObject(arguments);
                AsyncContext asyncContextMapping = (AsyncContext) getAsyncContextMapping.getValue(functionObject);
                Object targetFunction = getTargetFunction.getValue(functionObject);
                JSAgent agent = JSRealm.get(this).getAgent();
                AsyncContext previousContextMapping = agent.asyncContextSwap(asyncContextMapping);
                try {
                    int argumentCount = JSArguments.getUserArgumentCount(arguments);
                    Object[] targetArguments = JSArguments.createInitial(JSArguments.getThisObject(arguments), targetFunction, argumentCount);
                    for (int i = 0; i < argumentCount; i++) {
                        JSArguments.setUserArgument(targetArguments, i, JSArguments.getUserArgument(arguments, i));
                    }
                    return callTarget.executeCall(targetArguments);
                } finally {
                    agent.asyncContextSwap(previousContextMapping);
                }
            }
        }
        return JSFunctionData.createCallOnly(context, new WrappedFunctionRootNode().getCallTarget(), 0, Strings.EMPTY_STRING);
    }
}
