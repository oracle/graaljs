/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.BooleanPrototypeBuiltinsFactory.JSBooleanToStringNodeGen;
import com.oracle.truffle.js.builtins.BooleanPrototypeBuiltinsFactory.JSBooleanValueOfNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;

/**
 * Contains builtins for {@linkplain JSBoolean}.prototype.
 */
public final class BooleanPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<BooleanPrototypeBuiltins.BooleanPrototype> {
    protected BooleanPrototypeBuiltins() {
        super(JSBoolean.PROTOTYPE_NAME, BooleanPrototype.class);
    }

    public enum BooleanPrototype implements BuiltinEnum<BooleanPrototype> {
        toString(0),
        valueOf(0);

        private final int length;

        BooleanPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, BooleanPrototype builtinEnum) {
        switch (builtinEnum) {
            case toString:
                return JSBooleanToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSBooleanValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSBooleanToStringNode extends JSBuiltinNode {

        public JSBooleanToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"!isJSBoolean(thisObj)", "!isBoolean(thisObj)"})
        protected String toString(@SuppressWarnings("unused") Object thisObj) {
            throw JSBoolean.noBooleanError();
        }

        @Specialization(guards = "isJSBoolean(thisObj)")
        protected String toString(DynamicObject thisObj) {
            return String.valueOf(JSBoolean.valueOf(thisObj));
        }

        @Specialization(guards = "isBoolean(thisObj)")
        protected String toStringPrimitive(Object thisObj) {
            return JSRuntime.booleanToString((boolean) thisObj);
        }
    }

    public abstract static class JSBooleanValueOfNode extends JSBuiltinNode {

        public JSBooleanValueOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"!isJSBoolean(thisObj)", "!isBoolean(thisObj)"})
        protected boolean valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw JSBoolean.noBooleanError();
        }

        @Specialization(guards = "isJSBoolean(thisObj)")
        protected boolean valueOf(DynamicObject thisObj) {
            return JSBoolean.valueOf(thisObj);
        }

        @Specialization(guards = "isBoolean(thisObj)")
        protected boolean valueOfPrimitive(Object thisObj) {
            return (boolean) thisObj;
        }
    }
}
