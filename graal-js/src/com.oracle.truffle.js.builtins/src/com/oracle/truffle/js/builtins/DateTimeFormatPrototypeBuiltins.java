/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.DateTimeFormatPrototypeBuiltinsFactory.JSDateTimeFormatResolvedOptionsNodeGen;
import com.oracle.truffle.js.builtins.DateTimeFormatPrototypeBuiltinsFactory.JSDateTimeFormatFormatToPartsNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSDateTimeFormat;

public final class DateTimeFormatPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<DateTimeFormatPrototypeBuiltins.DateTimeFormatPrototype> {

    protected DateTimeFormatPrototypeBuiltins() {
        super(JSDateTimeFormat.PROTOTYPE_NAME, DateTimeFormatPrototype.class);
    }

    public enum DateTimeFormatPrototype implements BuiltinEnum<DateTimeFormatPrototype> {

        resolvedOptions(0),
        formatToParts(1);

        private final int length;

        DateTimeFormatPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, DateTimeFormatPrototype builtinEnum) {
        switch (builtinEnum) {
            case resolvedOptions:
                return JSDateTimeFormatResolvedOptionsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case formatToParts:
                return JSDateTimeFormatFormatToPartsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSDateTimeFormatResolvedOptionsNode extends JSBuiltinNode {

        public JSDateTimeFormatResolvedOptionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doResolvedOptions(DynamicObject dateTimeFormat) {
            return JSDateTimeFormat.resolvedOptions(getContext(), dateTimeFormat);
        }

        @Specialization(guards = "!isDynamicObject(bummer)")
        public void doResolvedOptions(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorDateTimeFormatExpected();
        }
    }

    public abstract static class JSDateTimeFormatFormatToPartsNode extends JSBuiltinNode {

        public JSDateTimeFormatFormatToPartsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isDynamicObject(dateTimeFormat)"})
        public Object doFormatToParts(DynamicObject dateTimeFormat, Object value) {
            return JSDateTimeFormat.formatToParts(getContext(), dateTimeFormat, value);
        }

        @Specialization(guards = "!isDynamicObject(bummer)")
        @SuppressWarnings("unused")
        public void throwTypeError(Object bummer, Object value) {
            throw Errors.createTypeErrorDateTimeFormatExpected();
        }
    }
}
