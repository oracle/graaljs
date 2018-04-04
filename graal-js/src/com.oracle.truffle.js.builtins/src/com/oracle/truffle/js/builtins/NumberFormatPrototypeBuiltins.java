/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.NumberFormatPrototypeBuiltinsFactory.JSNumberFormatResolvedOptionsNodeGen;
import com.oracle.truffle.js.builtins.NumberFormatPrototypeBuiltinsFactory.JSNumberFormatFormatToPartsNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSNumberFormat;

public final class NumberFormatPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<NumberFormatPrototypeBuiltins.NumberFormatPrototype> {

    protected NumberFormatPrototypeBuiltins() {
        super(JSNumberFormat.PROTOTYPE_NAME, NumberFormatPrototype.class);
    }

    public enum NumberFormatPrototype implements BuiltinEnum<NumberFormatPrototype> {

        resolvedOptions(0),
        formatToParts(1);

        private final int length;

        NumberFormatPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, NumberFormatPrototype builtinEnum) {
        switch (builtinEnum) {
            case resolvedOptions:
                return JSNumberFormatResolvedOptionsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case formatToParts:
                return JSNumberFormatFormatToPartsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSNumberFormatResolvedOptionsNode extends JSBuiltinNode {

        public JSNumberFormatResolvedOptionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doResolvedOptions(DynamicObject numberFormat) {
            return JSNumberFormat.resolvedOptions(getContext(), numberFormat);
        }

        @Specialization(guards = "!isDynamicObject(bummer)")
        public void doResolvedOptions(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorNumberFormatExpected();
        }
    }

    public abstract static class JSNumberFormatFormatToPartsNode extends JSBuiltinNode {

        public JSNumberFormatFormatToPartsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isDynamicObject(numberFormat)"})
        public Object doFormatToParts(DynamicObject numberFormat, Object value) {
            return JSNumberFormat.formatToParts(getContext(), numberFormat, value);
        }

        @Specialization(guards = "!isDynamicObject(bummer)")
        @SuppressWarnings("unused")
        public void throwTypeError(Object bummer, Object value) {
            throw Errors.createTypeErrorNumberFormatExpected();
        }
    }
}
