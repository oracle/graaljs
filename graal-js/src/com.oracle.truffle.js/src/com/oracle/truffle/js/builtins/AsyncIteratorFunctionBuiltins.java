/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.access.GetIteratorDirectNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.binary.InstanceofNode.OrdinaryHasInstanceNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSAsyncIterator;
import com.oracle.truffle.js.runtime.builtins.JSWrapForAsyncIterator;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class AsyncIteratorFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncIteratorFunctionBuiltins.AsyncIteratorFunction> {

    public static final JSBuiltinsContainer BUILTINS = new AsyncIteratorFunctionBuiltins();

    protected AsyncIteratorFunctionBuiltins() {
        super(JSAsyncIterator.CLASS_NAME, AsyncIteratorFunction.class);
    }

    public enum AsyncIteratorFunction implements BuiltinEnum<AsyncIteratorFunction> {
        from(1);

        private final int length;

        AsyncIteratorFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (EnumSet.of(from).contains(this)) {
                return JSConfig.StagingECMAScriptVersion;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, AsyncIteratorFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return AsyncIteratorFunctionBuiltinsFactory.JSAsyncIteratorFromNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }

        return null;
    }

    public abstract static class JSAsyncIteratorFromNode extends JSBuiltinNode {
        @Child private GetMethodNode getAsyncIteratorMethodNode;
        @Child private GetMethodNode getIteratorMethodNode;
        @Child private GetIteratorNode getIteratorNode;

        @Child private OrdinaryHasInstanceNode ordinaryHasInstanceNode;
        @Child private GetIteratorDirectNode getIteratorDirectNode;

        public JSAsyncIteratorFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getAsyncIteratorMethodNode = GetMethodNode.create(context, Symbol.SYMBOL_ASYNC_ITERATOR);
            this.getIteratorMethodNode = GetMethodNode.create(context, Symbol.SYMBOL_ITERATOR);
        }

        @Specialization
        protected JSDynamicObject asyncIteratorFrom(Object arg) {
            IteratorRecord iteratorRecord = null;

            Object usingIterator = getAsyncIteratorMethodNode.executeWithTarget(arg);
            if (usingIterator != Undefined.instance || getIteratorMethodNode.executeWithTarget(arg) != Undefined.instance) {
                if (getIteratorNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getIteratorNode = insert(GetIteratorNode.createAsync(getContext(), null));
                }

                iteratorRecord = getIteratorNode.execute(arg);

                if (ordinaryHasInstanceNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    ordinaryHasInstanceNode = insert(OrdinaryHasInstanceNode.create(getContext()));
                }

                boolean hasInstance = ordinaryHasInstanceNode.executeBoolean(iteratorRecord.getIterator(), getRealm().getAsyncIteratorConstructor());
                if (hasInstance) {
                    return iteratorRecord.getIterator();
                }
            } else {
                if (getIteratorDirectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getIteratorDirectNode = insert(GetIteratorDirectNode.create(getContext()));
                }

                iteratorRecord = getIteratorDirectNode.execute(arg);
            }

            return JSWrapForAsyncIterator.create(getContext(), getRealm(), iteratorRecord);
        }
    }
}
