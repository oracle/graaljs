package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.TemporalTimePrototypeBuiltinsFactory.JSTemporalTimeEqualsNodeGen;
import com.oracle.truffle.js.builtins.TemporalTimePrototypeBuiltinsFactory.JSTemporalTimeToStringNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalTime;
import com.oracle.truffle.js.runtime.builtins.JSTemporalTimeObject;

public class TemporalTimePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalTimePrototypeBuiltins.TemporalTimePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalTimePrototypeBuiltins();

    protected TemporalTimePrototypeBuiltins() {
        super(JSTemporalTime.PROTOTYPE_NAME, TemporalTimePrototype.class);
    }

    public enum TemporalTimePrototype implements BuiltinEnum<TemporalTimePrototype> {
        equals(1),
        toString(0),
        toJSON(0);

        private final int length;

        TemporalTimePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalTimePrototype builtinEnum) {
        switch (builtinEnum) {
            case equals:
                return JSTemporalTimeEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toString:
            case toJSON:
                return JSTemporalTimeToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalTimeEquals extends JSBuiltinNode {

        protected JSTemporalTimeEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSTemporalTime(otherObj)")
        protected static boolean equals(DynamicObject thisObj, DynamicObject otherObj) {
            JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
            JSTemporalTimeObject other = (JSTemporalTimeObject) otherObj;
            if (temporalTime.getHours() != other.getHours()) {
                return false;
            }
            if (temporalTime.getMinutes() != other.getMinutes()) {
                return false;
            }
            if (temporalTime.getSeconds() != other.getSeconds()) {
                return false;
            }
            if (temporalTime.getMilliseconds() != other.getMilliseconds()) {
                return false;
            }
            if (temporalTime.getMicroseconds() != other.getMicroseconds()) {
                return false;
            }
            if (temporalTime.getNanoseconds() != other.getNanoseconds()) {
                return false;
            }
            return true;
        }

        @Specialization(guards = {"!isJSTemporalTime(otherObj) || isJavaPrimitive(otherObj)"})
        protected static boolean otherNotTemporalTime(DynamicObject thisObj, DynamicObject otherObj) {
            return false;
        }
    }

    public abstract static class JSTemporalTimeToStringNode extends JSBuiltinNode {

        protected JSTemporalTimeToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(DynamicObject thisObj) {
            JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
            return temporalTimeToString(temporalTime);
        }

        private String temporalTimeToString(JSTemporalTimeObject temporalTime) {
            String hour = String.format("%1$2d", temporalTime.getHours()).replace(" ", "0");
            String minute = String.format("%1$2d", temporalTime.getMinutes()).replace(" ", "0");
            String seconds = formatSecondsStringPart(temporalTime);
            return String.format("%s:%s%s", hour, minute, seconds);
        }

        private String formatSecondsStringPart(JSTemporalTimeObject temporalTime) {
            if (temporalTime.getSeconds() + temporalTime.getMilliseconds() + temporalTime.getMicroseconds()
                    + temporalTime.getNanoseconds() == 0) {
                return "";
            }
            String nanos = "", micros = "", millis = "";
            if(temporalTime.getNanoseconds() != 0) {
                nanos = String.format("%1$3d", temporalTime.getNanoseconds()).replace(" ", "0");
                micros = "000";
                millis = "000";
            }
            if(temporalTime.getMicroseconds() != 0) {
                micros = String.format("%1$3d", temporalTime.getMicroseconds()).replace(" ", "0");
                millis = "000";
            }
            if (temporalTime.getMilliseconds() != 0) {
                millis = String.format("%1$3d", temporalTime.getMilliseconds()).replace(" ", "0");
            }
            String decimal = String.format("%s%s%s", millis, micros, nanos);
            String result = String.format("%1$2d", temporalTime.getSeconds()).replace(" ", "0");
            if(!decimal.isEmpty()) {
                result = String.format("%s.%s", result, decimal);
            }
            return String.format(":%s", result);
        }
    }
}
