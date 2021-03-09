package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarToStringNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.JSTemporalCalendarObject;

public class TemporalCalendarPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalCalendarPrototypeBuiltins.TemporalCalendarPrototype> {

    public static final TemporalCalendarPrototypeBuiltins INSTANCE = new TemporalCalendarPrototypeBuiltins();

    protected TemporalCalendarPrototypeBuiltins() {
        super(JSTemporalCalendar.PROTOTYPE_NAME, TemporalCalendarPrototype.class);
    }

    public enum TemporalCalendarPrototype implements BuiltinEnum<TemporalCalendarPrototype> {
        toString(0),
        toJSON(0);

        private final int length;

        TemporalCalendarPrototype(int length) {
            this.length = length;
        }


        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalCalendarPrototype builtinEnum) {
        switch (builtinEnum) {
            case toString:
            case toJSON:
                return JSTemporalCalendarToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    // 12.4.23
    public abstract static class JSTemporalCalendarToString extends JSBuiltinNode {

        protected JSTemporalCalendarToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toString(DynamicObject thisObj) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            return calendar.getId();
        }
    }
}
