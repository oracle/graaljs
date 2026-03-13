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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.DisposableStackPrototypeBuiltinsFactory.DisposableStackAdoptNodeGen;
import com.oracle.truffle.js.builtins.DisposableStackPrototypeBuiltinsFactory.DisposableStackDeferNodeGen;
import com.oracle.truffle.js.builtins.DisposableStackPrototypeBuiltinsFactory.DisposableStackDisposeNodeGen;
import com.oracle.truffle.js.builtins.DisposableStackPrototypeBuiltinsFactory.DisposableStackDisposedNodeGen;
import com.oracle.truffle.js.builtins.DisposableStackPrototypeBuiltinsFactory.DisposableStackMoveNodeGen;
import com.oracle.truffle.js.builtins.DisposableStackPrototypeBuiltinsFactory.DisposableStackUseNodeGen;
import com.oracle.truffle.js.nodes.control.AddDisposableResourceNode;
import com.oracle.truffle.js.nodes.control.DisposeResourcesNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSDisposableStack;
import com.oracle.truffle.js.runtime.builtins.JSDisposableStackObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DisposeCapability;

public final class DisposableStackPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<DisposableStackPrototypeBuiltins.DisposableStackPrototype> {
    public static final JSBuiltinsContainer BUILTINS = new DisposableStackPrototypeBuiltins();

    protected DisposableStackPrototypeBuiltins() {
        super(JSDisposableStack.PROTOTYPE_NAME, DisposableStackPrototype.class);
    }

    public enum DisposableStackPrototype implements BuiltinEnum<DisposableStackPrototype> {
        use(1),
        adopt(2),
        defer(1),
        move(0),
        dispose(0),
        disposed(0);

        private final int length;

        DisposableStackPrototype(int length) {
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
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, DisposableStackPrototype builtinEnum) {
        switch (builtinEnum) {
            case use:
                return DisposableStackUseNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case adopt:
                return DisposableStackAdoptNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case defer:
                return DisposableStackDeferNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case move:
                return DisposableStackMoveNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case dispose:
                return DisposableStackDisposeNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case disposed:
                return DisposableStackDisposedNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    abstract static class DisposableStackOperation extends JSBuiltinNode {

        DisposableStackOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected final JSDisposableStackObject requireDisposableStack(Object thisObj, InlinedBranchProfile errorProfile) {
            if (thisObj instanceof JSDisposableStackObject stack) {
                return stack;
            }
            errorProfile.enter(this);
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }

        protected final void ensureNotDisposed(JSDisposableStackObject stack, InlinedBranchProfile errorProfile) {
            if (stack.isDisposed()) {
                errorProfile.enter(this);
                throw Errors.createReferenceError("DisposableStack is already disposed", this);
            }
        }
    }

    public abstract static class DisposableStackUseNode extends DisposableStackOperation {
        @Child private AddDisposableResourceNode addDisposableResourceNode;

        DisposableStackUseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.addDisposableResourceNode = AddDisposableResourceNode.create(getContext(), false);
        }

        @Specialization
        protected Object use(Object thisObj, Object value,
                        @Cached InlinedBranchProfile errorProfile) {
            JSDisposableStackObject stack = requireDisposableStack(thisObj, errorProfile);
            ensureNotDisposed(stack, errorProfile);
            addDisposableResourceNode.execute(stack.getDisposeCapability(), value);
            return value;
        }
    }

    public abstract static class DisposableStackAdoptNode extends DisposableStackOperation {
        DisposableStackAdoptNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object adopt(Object thisObj, Object value, Object onDispose,
                        @Cached IsCallableNode isCallableNode,
                        @Cached InlinedBranchProfile errorProfile) {
            JSDisposableStackObject stack = requireDisposableStack(thisObj, errorProfile);
            ensureNotDisposed(stack, errorProfile);
            if (!isCallableNode.executeBoolean(onDispose)) {
                errorProfile.enter(this);
                throw Errors.createTypeErrorNotAFunction(onDispose, this);
            }
            AddDisposableResourceNode.addCallback(stack.getDisposeCapability(), onDispose, value, false);
            return value;
        }
    }

    public abstract static class DisposableStackDeferNode extends DisposableStackOperation {
        DisposableStackDeferNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object defer(Object thisObj, Object onDispose,
                        @Cached IsCallableNode isCallableNode,
                        @Cached InlinedBranchProfile errorProfile) {
            JSDisposableStackObject stack = requireDisposableStack(thisObj, errorProfile);
            ensureNotDisposed(stack, errorProfile);
            if (!isCallableNode.executeBoolean(onDispose)) {
                errorProfile.enter(this);
                throw Errors.createTypeErrorNotAFunction(onDispose, this);
            }
            AddDisposableResourceNode.addCallback(stack.getDisposeCapability(), onDispose, null, false);
            return Undefined.instance;
        }
    }

    public abstract static class DisposableStackMoveNode extends DisposableStackOperation {
        DisposableStackMoveNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object move(Object thisObj,
                        @Cached InlinedBranchProfile errorProfile) {
            JSDisposableStackObject stack = requireDisposableStack(thisObj, errorProfile);
            ensureNotDisposed(stack, errorProfile);
            DisposeCapability capability = stack.takeDisposeCapability();
            return JSDisposableStack.create(getContext(), getRealm(), getRealm().getDisposableStackPrototype(), capability);
        }
    }

    public abstract static class DisposableStackDisposeNode extends DisposableStackOperation {
        @Child private DisposeResourcesNode disposeResourcesNode;

        DisposableStackDisposeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.disposeResourcesNode = DisposeResourcesNode.create();
        }

        @Specialization
        protected Object dispose(Object thisObj,
                        @Cached InlinedBranchProfile errorProfile) {
            JSDisposableStackObject stack = requireDisposableStack(thisObj, errorProfile);
            if (!stack.isDisposed()) {
                stack.setDisposed(true);
                disposeResourcesNode.execute(stack.getDisposeCapability(), DisposeCapability.NO_ERROR);
            }
            return Undefined.instance;
        }
    }

    public abstract static class DisposableStackDisposedNode extends DisposableStackOperation {
        DisposableStackDisposedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean disposed(Object thisObj,
                        @Cached InlinedBranchProfile errorProfile) {
            return requireDisposableStack(thisObj, errorProfile).isDisposed();
        }
    }
}
