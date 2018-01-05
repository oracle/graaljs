/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.CollatorPrototypeBuiltinsFactory.JSCollatorResolvedOptionsNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSCollator;

public final class CollatorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<CollatorPrototypeBuiltins.CollatorPrototype> {

    protected CollatorPrototypeBuiltins() {
        super(JSCollator.PROTOTYPE_NAME, CollatorPrototype.class);
    }

    public enum CollatorPrototype implements BuiltinEnum<CollatorPrototype> {

        resolvedOptions(0);

        private final int length;

        CollatorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, CollatorPrototype builtinEnum) {
        switch (builtinEnum) {
            case resolvedOptions:
                return JSCollatorResolvedOptionsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSCollatorResolvedOptionsNode extends JSBuiltinNode {

        public JSCollatorResolvedOptionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doResolvedOptions(DynamicObject collator) {
            return JSCollator.resolvedOptions(getContext(), collator);
        }

        @Specialization(guards = "!isDynamicObject(bummer)")
        public void doResolvedOptions(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeError("Collator object expected.");
        }
    }
}
