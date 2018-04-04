/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.PluralRulesPrototypeBuiltinsFactory.JSPluralRulesResolvedOptionsNodeGen;
import com.oracle.truffle.js.builtins.PluralRulesPrototypeBuiltinsFactory.JSPluralRulesSelectNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSPluralRules;

public final class PluralRulesPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<PluralRulesPrototypeBuiltins.PluralRulesPrototype> {

    protected PluralRulesPrototypeBuiltins() {
        super(JSPluralRules.PROTOTYPE_NAME, PluralRulesPrototype.class);
    }

    public enum PluralRulesPrototype implements BuiltinEnum<PluralRulesPrototype> {

        resolvedOptions(0),
        select(1);

        private final int length;

        PluralRulesPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, PluralRulesPrototype builtinEnum) {
        switch (builtinEnum) {
            case resolvedOptions:
                return JSPluralRulesResolvedOptionsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case select:
                return JSPluralRulesSelectNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSPluralRulesResolvedOptionsNode extends JSBuiltinNode {

        public JSPluralRulesResolvedOptionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doResolvedOptions(DynamicObject pluralRules) {
            return JSPluralRules.resolvedOptions(getContext(), pluralRules);
        }

        @Specialization(guards = "!isDynamicObject(bummer)")
        public void doResolvedOptions(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorPluralRulesExpected();
        }
    }

    public abstract static class JSPluralRulesSelectNode extends JSBuiltinNode {

        public JSPluralRulesSelectNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isDynamicObject(pluralRules)"})
        public Object doSelect(DynamicObject pluralRules, Object value) {
            return JSPluralRules.select(pluralRules, value);
        }

        @Specialization(guards = "!isDynamicObject(bummer)")
        @SuppressWarnings("unused")
        public void throwTypeError(Object bummer, Object value) {
            throw Errors.createTypeErrorPluralRulesExpected();
        }
    }
}
