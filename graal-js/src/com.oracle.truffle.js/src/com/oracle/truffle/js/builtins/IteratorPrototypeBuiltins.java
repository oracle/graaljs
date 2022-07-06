/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * Contains builtins for {@linkplain JSArray}.prototype.
 */
public final class IteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorPrototypeBuiltins.IteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new IteratorPrototypeBuiltins();

    protected IteratorPrototypeBuiltins() {
        super(JSArray.PROTOTYPE_NAME, IteratorPrototype.class);
    }

    public enum IteratorPrototype implements BuiltinEnum<IteratorPrototype> {
        toArray(0);

        private final int length;

        IteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, IteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case toArray:
                return IteratorPrototypeBuiltinsFactory.IteratorToArrayNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class IteratorToArrayNode extends JSBuiltinNode {
        @Child private IteratorFunctionBuiltins.GetIteratorDirectNode getIteratorDirectNode;
        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode iteratorValueNode;
        private final BranchProfile growProfile = BranchProfile.create();

        public IteratorToArrayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getIteratorDirectNode = IteratorFunctionBuiltins.GetIteratorDirectNode.create(context);
            iteratorStepNode = IteratorStepNode.create(context);
            iteratorValueNode = IteratorValueNode.create(context);
        }

        @Specialization
        protected JSDynamicObject toArray(Object thisObj) {
            IteratorRecord iterated = getIteratorDirectNode.execute(thisObj);
            SimpleArrayList<Object> items = new SimpleArrayList<>();

            while (true) {
                Object next = iteratorStepNode.execute(iterated);
                if (next == (Boolean) false) {
                    return JSArray.createConstant(getContext(), getRealm(), items.toArray());
                }

                Object value = iteratorValueNode.execute(next);
                items.add(value, growProfile);
            }
        }
    }
}
