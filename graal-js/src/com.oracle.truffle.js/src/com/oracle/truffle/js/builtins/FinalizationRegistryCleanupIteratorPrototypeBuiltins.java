/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.FinalizationRegistryCleanupIteratorPrototypeBuiltinsFactory.CleanupNextNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.FinalizationRecord;
import com.oracle.truffle.js.runtime.builtins.JSFinalizationRegistry;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * The %FinalizationRegistryCleanupIteratorPrototype% Object.
 */
public final class FinalizationRegistryCleanupIteratorPrototypeBuiltins
                extends JSBuiltinsContainer.SwitchEnum<FinalizationRegistryCleanupIteratorPrototypeBuiltins.FinalizationRegistryCleanupIteratorPrototype> {
    public static final JSBuiltinsContainer BUILTINS = new FinalizationRegistryCleanupIteratorPrototypeBuiltins();

    protected FinalizationRegistryCleanupIteratorPrototypeBuiltins() {
        super(JSFinalizationRegistry.CLEANUP_ITERATOR_PROTOTYPE_NAME, FinalizationRegistryCleanupIteratorPrototype.class);
    }

    public enum FinalizationRegistryCleanupIteratorPrototype implements BuiltinEnum<FinalizationRegistryCleanupIteratorPrototype> {
        next(0);

        private final int length;

        FinalizationRegistryCleanupIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, FinalizationRegistryCleanupIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return CleanupNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class CleanupNextNode extends JSBuiltinNode {
        @Child private PropertySetNode setValueNode;
        @Child private PropertySetNode setDoneNode;
        @Child private PropertyGetNode getFinalizationRegistryNode;
        private final BranchProfile errorBranch;

        public CleanupNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.setValueNode = PropertySetNode.create(JSRuntime.VALUE, false, context, false);
            this.setDoneNode = PropertySetNode.create(JSRuntime.DONE, false, context, false);
            this.getFinalizationRegistryNode = PropertyGetNode.createGetHidden(JSRuntime.FINALIZATION_GROUP_CLEANUP_ITERATOR_ID, context);
            this.errorBranch = BranchProfile.create();
        }

        @Specialization
        public DynamicObject execute(Object target) {
            Object finalizationRegistry = getFinalizationRegistryNode.getValue(target);
            if (finalizationRegistry == Undefined.instance || !JSFinalizationRegistry.isJSFinalizationRegistry(finalizationRegistry)) {
                errorBranch.enter();
                throw Errors.createTypeError("FinalizationRegistry Cleanup iterator required");
            }
            FinalizationRecord record = JSFinalizationRegistry.removeCellEmptyTarget((DynamicObject) finalizationRegistry);
            if (record != null) {
                return createIterResultObject(record.getHeldValue(), false);
            }
            return createIterResultObject(Undefined.instance, true);
        }

        private DynamicObject createIterResultObject(Object value, boolean done) {
            DynamicObject iterResultObject = JSUserObject.create(getContext());
            setValueNode.setValue(iterResultObject, value);
            setDoneNode.setValueBoolean(iterResultObject, done);
            return iterResultObject;
        }
    }
}
