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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.AsyncContextPrototypeBuiltinsFactory.AsyncContextGetNodeGen;
import com.oracle.truffle.js.builtins.AsyncContextPrototypeBuiltinsFactory.AsyncContextNameNodeGen;
import com.oracle.truffle.js.builtins.AsyncContextPrototypeBuiltinsFactory.AsyncContextRunNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSAsyncContext;
import com.oracle.truffle.js.runtime.builtins.JSAsyncContextObject;
import com.oracle.truffle.js.runtime.builtins.JSShadowRealm;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains built-in functions of the {@code %AsyncContext.prototype%}.
 */
public final class AsyncContextPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncContextPrototypeBuiltins.AsyncContextPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new AsyncContextPrototypeBuiltins();

    protected AsyncContextPrototypeBuiltins() {
        super(JSShadowRealm.PROTOTYPE_NAME, AsyncContextPrototype.class);
    }

    public enum AsyncContextPrototype implements BuiltinEnum<AsyncContextPrototype> {
        run(2),
        get(0),
        name(0) {
            @Override
            public boolean isGetter() {
                return true;
            }
        };

        private final int length;

        AsyncContextPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, AsyncContextPrototype builtinEnum) {
        switch (builtinEnum) {
            case run:
                return AsyncContextRunNodeGen.create(context, builtin, args().withThis().fixedArgs(2).varArgs().createArgumentNodes(context));
            case get:
                return AsyncContextGetNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case name:
                return AsyncContextNameNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic(JSAsyncContext.class)
    public abstract static class AsyncContextRunNode extends JSBuiltinNode {

        public AsyncContextRunNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object run(JSAsyncContextObject thisObj, Object value, Object func, Object[] args,
                        @Cached("createCall()") JSFunctionCallNode callNode) {
            JSAgent agent = getRealm().getAgent();
            var previousContextMapping = agent.getAsyncContextMapping();
            var asyncContextMapping = previousContextMapping.withMapping(thisObj.getAsyncContextKey(), value);
            agent.asyncContextSwap(asyncContextMapping);
            try {
                return callNode.executeCall(JSArguments.create(Undefined.instance, func, args));
            } finally {
                agent.asyncContextSwap(previousContextMapping);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSAsyncContext(thisObj)")
        protected Object invalidReceiver(Object thisObj, Object value, Object func, Object[] args) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }

    @ImportStatic(JSAsyncContext.class)
    public abstract static class AsyncContextGetNode extends JSBuiltinNode {

        public AsyncContextGetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object get(JSAsyncContextObject thisObj) {
            return getRealm().getAgent().getAsyncContextMapping().getOrDefault(thisObj.getAsyncContextKey(), thisObj.getAsyncContextDefaultValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSAsyncContext(thisObj)")
        protected Object invalidReceiver(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }

    @ImportStatic(JSAsyncContext.class)
    public abstract static class AsyncContextNameNode extends JSBuiltinNode {

        public AsyncContextNameNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object name(JSAsyncContextObject thisObj) {
            return thisObj.getAsyncContextKey().getDescription();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSAsyncContext(thisObj)")
        protected Object invalidReceiver(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }
}
