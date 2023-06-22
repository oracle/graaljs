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
package com.oracle.truffle.js.builtins.asynccontext;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.ConstructorBuiltins;
import com.oracle.truffle.js.builtins.ConstructorBuiltins.ConstructWithNewTargetNode;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.asynccontext.AsyncContextNamespaceBuiltinsFactory.ConstructAsyncContextSnapshotNodeGen;
import com.oracle.truffle.js.builtins.asynccontext.AsyncContextNamespaceBuiltinsFactory.ConstructAsyncContextVariableNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.asynccontext.JSAsyncContext;
import com.oracle.truffle.js.runtime.builtins.asynccontext.JSAsyncContextSnapshot;
import com.oracle.truffle.js.runtime.builtins.asynccontext.JSAsyncContextVariable;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains built-in functions of the {@code %AsyncContext%} constructor.
 */
public final class AsyncContextNamespaceBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncContextNamespaceBuiltins.AsyncContextNamespace> {

    public static final JSBuiltinsContainer BUILTINS = new AsyncContextNamespaceBuiltins();

    protected AsyncContextNamespaceBuiltins() {
        super(JSAsyncContext.NAMESPACE_NAME, AsyncContextNamespace.class);
    }

    public enum AsyncContextNamespace implements BuiltinEnum<AsyncContextNamespace> {
        Snapshot(0, JSAsyncContextSnapshot.CLASS_NAME),
        Variable(1, JSAsyncContextVariable.CLASS_NAME);

        private final int length;
        private final TruffleString key;

        AsyncContextNamespace(int length, TruffleString key) {
            this.length = length;
            this.key = key;
        }

        @Override
        public Object getKey() {
            return key;
        }

        @Override
        public TruffleString getName() {
            return key;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isConstructor() {
            return true;
        }

        @Override
        public boolean isNewTargetConstructor() {
            return true;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, AsyncContextNamespace builtinEnum) {
        switch (builtinEnum) {
            case Snapshot:
                if (construct) {
                    return newTarget ? ConstructAsyncContextSnapshotNodeGen.create(context, builtin, true, args().newTarget().createArgumentNodes(context))
                                    : ConstructAsyncContextSnapshotNodeGen.create(context, builtin, false, args().function().createArgumentNodes(context));
                } else {
                    return ConstructorBuiltins.createCallRequiresNew(context, builtin);
                }
            case Variable:
                if (construct) {
                    return newTarget ? ConstructAsyncContextVariableNodeGen.create(context, builtin, true, args().newTarget().fixedArgs(1).createArgumentNodes(context))
                                    : ConstructAsyncContextVariableNodeGen.create(context, builtin, false, args().function().fixedArgs(1).createArgumentNodes(context));
                } else {
                    return ConstructorBuiltins.createCallRequiresNew(context, builtin);
                }
        }
        return null;
    }

    public abstract static class ConstructAsyncContextSnapshotNode extends ConstructWithNewTargetNode {

        public ConstructAsyncContextSnapshotNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected final JSObject construct(JSDynamicObject newTarget) {
            JSRealm realm = getRealm();
            return swapPrototype(JSAsyncContextSnapshot.create(getContext(), realm, realm.getAgent().getAsyncContextMapping()), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return getRealm().getAsyncContextSnapshotPrototype();
        }
    }

    @ImportStatic(Strings.class)
    public abstract static class ConstructAsyncContextVariableNode extends ConstructWithNewTargetNode {

        public ConstructAsyncContextVariableNode(JSContext context, JSBuiltin builtin, boolean isNewTargetCase) {
            super(context, builtin, isNewTargetCase);
        }

        @Specialization
        protected final JSObject construct(JSDynamicObject newTarget, Object options,
                        @Cached IsObjectNode isObject,
                        @Cached JSToStringNode toString,
                        @Cached("create(NAME, getContext())") PropertyGetNode getName,
                        @Cached("create(DEFAULT_VALUE, getContext())") PropertyGetNode getDefaultValue) {
            JSRealm realm = getRealm();
            TruffleString nameStr;
            Object defaultValue;
            if (isObject.executeBoolean(options)) {
                Object name = getName.getValue(options);
                nameStr = toString.executeString(name);
                defaultValue = getDefaultValue.getValue(options);
            } else {
                nameStr = Strings.EMPTY_STRING;
                defaultValue = Undefined.instance;
            }
            Symbol asyncContextKey = Symbol.create(nameStr);
            return swapPrototype(JSAsyncContextVariable.create(getContext(), realm, asyncContextKey, defaultValue), newTarget);
        }

        @Override
        protected JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
            return getRealm().getAsyncContextVariablePrototype();
        }
    }
}
