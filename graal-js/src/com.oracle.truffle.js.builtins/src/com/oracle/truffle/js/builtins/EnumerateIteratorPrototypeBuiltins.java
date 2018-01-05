/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import java.util.Iterator;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.builtins.EnumerateIteratorPrototypeBuiltinsFactory.EnumerateNextNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Prototype of [[Enumerate]]().
 */
public final class EnumerateIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<EnumerateIteratorPrototypeBuiltins.EnumerateIteratorPrototype> {
    protected EnumerateIteratorPrototypeBuiltins() {
        super(JSFunction.ENUMERATE_ITERATOR_PROTOTYPE_NAME, EnumerateIteratorPrototype.class);
    }

    public enum EnumerateIteratorPrototype implements BuiltinEnum<EnumerateIteratorPrototype> {
        next(0);

        private final int length;

        EnumerateIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, EnumerateIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return EnumerateNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class EnumerateNextNode extends JSBuiltinNode {
        @Child private PropertySetNode setValueNode;
        @Child private PropertySetNode setDoneNode;
        @Child private PropertyGetNode getIteratorNode;
        private final BranchProfile errorBranch;
        private final ValueProfile iteratorProfile;

        public EnumerateNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.setValueNode = PropertySetNode.create("value", false, context, false);
            this.setDoneNode = PropertySetNode.create("done", false, context, false);
            this.getIteratorNode = PropertyGetNode.create(JSRuntime.ENUMERATE_ITERATOR_ID, false, context);
            this.errorBranch = BranchProfile.create();
            this.iteratorProfile = ValueProfile.createClassProfile();
        }

        @Specialization
        public DynamicObject execute(Object target) {
            Object iteratorValue = getIteratorNode.getValue(target);
            if (iteratorValue == Undefined.instance) {
                errorBranch.enter();
                throw Errors.createTypeError("Enumerate iterator required");
            }
            Iterator<?> iterator = (Iterator<?>) iteratorProfile.profile(iteratorValue);
            if (Boundaries.iteratorHasNext(iterator)) {
                return createIterResultObject(Boundaries.iteratorNext(iterator), false);
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
