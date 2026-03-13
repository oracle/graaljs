/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.AsyncDisposableStackPrototypeBuiltinsFactory.AsyncDisposableStackAdoptNodeGen;
import com.oracle.truffle.js.builtins.AsyncDisposableStackPrototypeBuiltinsFactory.AsyncDisposableStackDeferNodeGen;
import com.oracle.truffle.js.builtins.AsyncDisposableStackPrototypeBuiltinsFactory.AsyncDisposableStackDisposeAsyncNodeGen;
import com.oracle.truffle.js.builtins.AsyncDisposableStackPrototypeBuiltinsFactory.AsyncDisposableStackDisposedNodeGen;
import com.oracle.truffle.js.builtins.AsyncDisposableStackPrototypeBuiltinsFactory.AsyncDisposableStackMoveNodeGen;
import com.oracle.truffle.js.builtins.AsyncDisposableStackPrototypeBuiltinsFactory.AsyncDisposableStackUseNodeGen;
import com.oracle.truffle.js.nodes.control.AddDisposableResourceNode;
import com.oracle.truffle.js.nodes.control.AsyncDisposeResourcesNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSAsyncDisposableStack;
import com.oracle.truffle.js.runtime.builtins.JSAsyncDisposableStackObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DisposeCapability;

public final class AsyncDisposableStackPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncDisposableStackPrototypeBuiltins.AsyncDisposableStackPrototype> {
    public static final JSBuiltinsContainer BUILTINS = new AsyncDisposableStackPrototypeBuiltins();

    protected AsyncDisposableStackPrototypeBuiltins() {
        super(JSAsyncDisposableStack.PROTOTYPE_NAME, AsyncDisposableStackPrototype.class);
    }

    public enum AsyncDisposableStackPrototype implements BuiltinEnum<AsyncDisposableStackPrototype> {
        use(1),
        adopt(2),
        defer(1),
        move(0),
        disposeAsync(0),
        disposed(0);

        private final int length;

        AsyncDisposableStackPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return this == disposed;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, AsyncDisposableStackPrototype builtinEnum) {
        switch (builtinEnum) {
            case use:
                return AsyncDisposableStackUseNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case adopt:
                return AsyncDisposableStackAdoptNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case defer:
                return AsyncDisposableStackDeferNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case move:
                return AsyncDisposableStackMoveNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case disposeAsync:
                return AsyncDisposableStackDisposeAsyncNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case disposed:
                return AsyncDisposableStackDisposedNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    abstract static class AsyncDisposableStackOperation extends JSBuiltinNode {

        AsyncDisposableStackOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected final JSAsyncDisposableStackObject requireAsyncDisposableStack(Object thisObj, InlinedBranchProfile errorProfile) {
            if (thisObj instanceof JSAsyncDisposableStackObject stack) {
                return stack;
            }
            errorProfile.enter(this);
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }

        protected final void ensureNotDisposed(JSAsyncDisposableStackObject stack, InlinedBranchProfile errorProfile) {
            if (stack.isDisposed()) {
                errorProfile.enter(this);
                throw Errors.createReferenceError("AsyncDisposableStack is already disposed", this);
            }
        }

    }

    public abstract static class AsyncDisposableStackUseNode extends AsyncDisposableStackOperation {
        @Child private AddDisposableResourceNode addDisposableResourceNode;

        AsyncDisposableStackUseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.addDisposableResourceNode = AddDisposableResourceNode.create(getContext(), true);
        }

        @Specialization
        protected Object use(Object thisObj, Object value,
                        @Cached InlinedBranchProfile errorProfile) {
            JSAsyncDisposableStackObject stack = requireAsyncDisposableStack(thisObj, errorProfile);
            ensureNotDisposed(stack, errorProfile);
            addDisposableResourceNode.execute(stack.getDisposeCapability(), value);
            return value;
        }
    }

    public abstract static class AsyncDisposableStackAdoptNode extends AsyncDisposableStackOperation {
        AsyncDisposableStackAdoptNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object adopt(Object thisObj, Object value, Object onDisposeAsync,
                        @Cached IsCallableNode isCallableNode,
                        @Cached InlinedBranchProfile errorProfile) {
            JSAsyncDisposableStackObject stack = requireAsyncDisposableStack(thisObj, errorProfile);
            ensureNotDisposed(stack, errorProfile);
            if (!isCallableNode.executeBoolean(onDisposeAsync)) {
                errorProfile.enter(this);
                throw Errors.createTypeErrorNotAFunction(onDisposeAsync, this);
            }
            AddDisposableResourceNode.addCallback(stack.getDisposeCapability(), onDisposeAsync, value, true);
            return value;
        }
    }

    public abstract static class AsyncDisposableStackDeferNode extends AsyncDisposableStackOperation {
        AsyncDisposableStackDeferNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object defer(Object thisObj, Object onDisposeAsync,
                        @Cached IsCallableNode isCallableNode,
                        @Cached InlinedBranchProfile errorProfile) {
            JSAsyncDisposableStackObject stack = requireAsyncDisposableStack(thisObj, errorProfile);
            ensureNotDisposed(stack, errorProfile);
            if (!isCallableNode.executeBoolean(onDisposeAsync)) {
                errorProfile.enter(this);
                throw Errors.createTypeErrorNotAFunction(onDisposeAsync, this);
            }
            AddDisposableResourceNode.addCallback(stack.getDisposeCapability(), onDisposeAsync, null, true);
            return Undefined.instance;
        }
    }

    public abstract static class AsyncDisposableStackMoveNode extends AsyncDisposableStackOperation {
        AsyncDisposableStackMoveNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object move(Object thisObj,
                        @Cached InlinedBranchProfile errorProfile) {
            JSAsyncDisposableStackObject stack = requireAsyncDisposableStack(thisObj, errorProfile);
            ensureNotDisposed(stack, errorProfile);
            DisposeCapability capability = stack.takeDisposeCapability();
            return JSAsyncDisposableStack.create(getContext(), getRealm(), getRealm().getAsyncDisposableStackPrototype(), capability);
        }
    }

    public abstract static class AsyncDisposableStackDisposeAsyncNode extends AsyncDisposableStackOperation {
        @Child private AsyncDisposeResourcesNode asyncDisposeResourcesNode;
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child private JSFunctionCallNode callNode;
        @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;

        AsyncDisposableStackDisposeAsyncNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.asyncDisposeResourcesNode = AsyncDisposeResourcesNode.create(context);
            this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
            this.callNode = JSFunctionCallNode.createCall();
        }

        @Specialization
        protected Object disposeAsync(Object thisObj,
                        @Cached InlinedBranchProfile errorProfile) {
            PromiseCapabilityRecord promiseCapability = newPromiseCapability();
            try {
                JSAsyncDisposableStackObject stack = requireAsyncDisposableStack(thisObj, errorProfile);
                if (stack.isDisposed()) {
                    callResolve(promiseCapability, Undefined.instance);
                } else {
                    stack.setDisposed(true);
                    asyncDisposeResourcesNode.execute(stack.getDisposeCapability(), DisposeCapability.NO_ERROR, promiseCapability);
                }
            } catch (AbstractTruffleException ex) {
                callReject(promiseCapability, getErrorObjectNode().execute(ex));
            }
            return promiseCapability.getPromise();
        }

        private PromiseCapabilityRecord newPromiseCapability() {
            return newPromiseCapabilityNode.executeDefault();
        }

        private void callResolve(PromiseCapabilityRecord promiseCapability, Object value) {
            callNode.executeCall(JSArguments.createOneArg(promiseCapability.getPromise(), promiseCapability.getResolve(), value));
        }

        private void callReject(PromiseCapabilityRecord promiseCapability, Object error) {
            callNode.executeCall(JSArguments.createOneArg(promiseCapability.getPromise(), promiseCapability.getReject(), error));
        }

        private TryCatchNode.GetErrorObjectNode getErrorObjectNode() {
            if (getErrorObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create());
            }
            return getErrorObjectNode;
        }
    }

    public abstract static class AsyncDisposableStackDisposedNode extends AsyncDisposableStackOperation {
        AsyncDisposableStackDisposedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object disposed(Object thisObj,
                        @Cached InlinedBranchProfile errorProfile) {
            return requireAsyncDisposableStack(thisObj, errorProfile).isDisposed();
        }
    }
}
