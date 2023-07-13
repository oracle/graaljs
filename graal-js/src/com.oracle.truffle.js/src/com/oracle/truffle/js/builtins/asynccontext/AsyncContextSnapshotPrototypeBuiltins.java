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
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.asynccontext.AsyncContextSnapshotPrototypeBuiltinsFactory.RunNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.asynccontext.JSAsyncContextSnapshot;
import com.oracle.truffle.js.runtime.builtins.asynccontext.JSAsyncContextSnapshotObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains built-in functions of the {@code %AsyncContext.Snapshot.prototype%}.
 */
public final class AsyncContextSnapshotPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncContextSnapshotPrototypeBuiltins.AsyncContextSnapshotPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new AsyncContextSnapshotPrototypeBuiltins();

    protected AsyncContextSnapshotPrototypeBuiltins() {
        super(JSAsyncContextSnapshot.PROTOTYPE_NAME, AsyncContextSnapshotPrototype.class);
    }

    public enum AsyncContextSnapshotPrototype implements BuiltinEnum<AsyncContextSnapshotPrototype> {
        run(1);

        private final int length;

        AsyncContextSnapshotPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, AsyncContextSnapshotPrototype builtinEnum) {
        switch (builtinEnum) {
            case run:
                return RunNodeGen.create(context, builtin, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic(JSAsyncContextSnapshot.class)
    public abstract static class RunNode extends JSBuiltinNode {

        public RunNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doCallable(JSAsyncContextSnapshotObject asyncSnapshotObj, Object func, Object[] args,
                        @Cached("createCall()") JSFunctionCallNode callFunc) {
            JSAgent agent = getRealm().getAgent();
            var snapshot = asyncSnapshotObj.getAsyncSnapshotMapping();
            var previousContextMapping = agent.asyncContextSwap(snapshot);
            try {
                return callFunc.executeCall(JSArguments.create(Undefined.instance, func, args));
            } finally {
                agent.asyncContextSwap(previousContextMapping);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSAsyncContextSnapshot(thisObj)")
        protected Object invalidReceiver(Object thisObj, Object func, Object[] args) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }
}
