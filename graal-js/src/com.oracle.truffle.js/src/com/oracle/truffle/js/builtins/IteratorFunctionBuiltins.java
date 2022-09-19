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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.IteratorFunctionBuiltinsFactory.JSIteratorFromNodeGen;
import com.oracle.truffle.js.nodes.access.GetIteratorDirectNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.binary.InstanceofNode.OrdinaryHasInstanceNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSIterator;
import com.oracle.truffle.js.runtime.builtins.JSWrapForIterator;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSIterator} function (constructor).
 */
public final class IteratorFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorFunctionBuiltins.IteratorFunction> {

    public static final JSBuiltinsContainer BUILTINS = new IteratorFunctionBuiltins();

    IteratorFunctionBuiltins() {
        super(JSIterator.CLASS_NAME, IteratorFunction.class);
    }

    public enum IteratorFunction implements BuiltinEnum<IteratorFunction> {
        from(1);

        private final int length;

        IteratorFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, IteratorFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return JSIteratorFromNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }

        return null;
    }

    public abstract static class JSIteratorFromNode extends JSBuiltinNode {
        @Child private GetMethodNode getIteratorMethodNode;
        @Child private GetIteratorNode getIteratorNode;
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private OrdinaryHasInstanceNode ordinaryHasInstanceNode;
        @Child private IsCallableNode isCallableNode;

        private final ConditionProfile usingIteratorProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile errorProfile = BranchProfile.create();

        public JSIteratorFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getIteratorMethodNode = GetMethodNode.create(context, Symbol.SYMBOL_ITERATOR);
            this.getIteratorNode = GetIteratorNode.create(getContext(), null);
            this.ordinaryHasInstanceNode = OrdinaryHasInstanceNode.create(getContext());
            this.getIteratorDirectNode = GetIteratorDirectNode.create(getContext());
            this.isCallableNode = IsCallableNode.create();
        }


        @Specialization
        protected JSDynamicObject iteratorFrom(Object arg) {
            IteratorRecord iteratorRecord;

            Object usingIterator = getIteratorMethodNode.executeWithTarget(arg);
            if (usingIteratorProfile.profile(usingIterator != Undefined.instance)) {
                iteratorRecord = getIteratorNode.execute(arg);
                if (!isCallableNode.executeBoolean(iteratorRecord.getNextMethod())) {
                    errorProfile.enter();
                    throw Errors.createTypeErrorCallableExpected();
                }

                boolean hasInstance = ordinaryHasInstanceNode.executeBoolean(iteratorRecord.getIterator(), getRealm().getIteratorConstructor());
                if (hasInstance) {
                    return iteratorRecord.getIterator();
                }
            } else {
                iteratorRecord = getIteratorDirectNode.execute(arg);
            }

            return JSWrapForIterator.create(getContext(), getRealm(), iteratorRecord);
        }


    }
}
