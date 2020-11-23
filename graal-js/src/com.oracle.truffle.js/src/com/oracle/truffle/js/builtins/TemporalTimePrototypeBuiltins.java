package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.builtins.TemporalTimePrototypeBuiltinsFactory.JSTemporalTimeEqualsNodeGen;
import com.oracle.truffle.js.builtins.TemporalTimePrototypeBuiltinsFactory.JSTemporalTimeToStringNodeGen;
import com.oracle.truffle.js.builtins.TemporalTimePrototypeBuiltinsFactory.JSTemporalTimeValueOfNodeGen;
import com.oracle.truffle.js.builtins.TemporalTimePrototypeBuiltinsFactory.JSTemporalTimeWithNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalTime;
import com.oracle.truffle.js.runtime.builtins.JSTemporalTimeObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalTimePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalTimePrototypeBuiltins.TemporalTimePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalTimePrototypeBuiltins();

    protected TemporalTimePrototypeBuiltins() {
        super(JSTemporalTime.PROTOTYPE_NAME, TemporalTimePrototype.class);
    }

    public enum TemporalTimePrototype implements BuiltinEnum<TemporalTimePrototype> {
        with(2),
        equals(1),
        toString(0),
        toJSON(0),
        valueOf(0);

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
            case with:
                return JSTemporalTimeWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case equals:
                return JSTemporalTimeEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toString:
            case toJSON:
                return JSTemporalTimeToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSTemporalTimeValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    // 4.3.12
    public abstract static class JSTemporalTimeWith extends JSBuiltinNode {

        protected JSTemporalTimeWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected JSTemporalTimeObject with(DynamicObject thisObj, DynamicObject temporalTimeLike, DynamicObject options,
                                            @Cached("create()") IsObjectNode isObject,
                                            @CachedLibrary("thisObj") DynamicObjectLibrary dol,
                                            @Cached("create()") JSToIntegerAsIntNode toInt,
                                            @Cached("create()") JSToBooleanNode toBoolean,
                                            @Cached("create()") JSToStringNode toString,
                                            @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
            if(!isObject.executeBoolean(temporalTimeLike)) {
                throw Errors.createTypeError("Temporal.Time like object expected.");
            }
            // TODO: Get calendar property.
            // TODO: Check calendar property is not undefined.
            // TODO: Get time zone property.
            // TODO: Check time zone property is not undefined.
            // TODO: Get calendar value.
            DynamicObject partialTime = JSTemporalTime.toPartialTime(temporalTimeLike, getContext().getRealm(),
                    isObject, dol, toInt);
            DynamicObject normalizedOptions = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(),
                    isObject);
            String overflow = TemporalUtil.toTemporalOverflow(normalizedOptions, dol, isObject, toBoolean, toString);
            int hour, minute, second, millisecond, microsecond, nanosecond;
            Object tempValue = dol.getOrDefault(partialTime, JSTemporalTime.HOUR, null);
            if (tempValue != null) {
                hour = toInt.executeInt(tempValue);
            } else {
                hour = temporalTime.getHours();
            }
            tempValue = dol.getOrDefault(partialTime, JSTemporalTime.MINUTE, null);
            if(tempValue != null) {
                minute = toInt.executeInt(tempValue);
            } else {
                minute = temporalTime.getMinutes();
            }
            tempValue = dol.getOrDefault(partialTime, JSTemporalTime.SECOND, null);
            if(tempValue != null) {
                second = toInt.executeInt(tempValue);
            } else {
                second = temporalTime.getSeconds();
            }
            tempValue = dol.getOrDefault(partialTime, JSTemporalTime.MILLISECOND, null);
            if(tempValue != null) {
                millisecond = toInt.executeInt(tempValue);
            } else {
                millisecond = temporalTime.getMilliseconds();
            }
            tempValue = dol.getOrDefault(partialTime, JSTemporalTime.MICROSECOND, null);
            if(tempValue != null) {
                microsecond = toInt.executeInt(tempValue);
            } else {
                microsecond = temporalTime.getMicroseconds();
            }
            tempValue = dol.getOrDefault(partialTime, JSTemporalTime.NANOSECOND, null);
            if(tempValue != null) {
                nanosecond = toInt.executeInt(tempValue);
            } else {
                nanosecond = temporalTime.getNanoseconds();
            }
            DynamicObject result = JSTemporalTime.regulateTime(hour, minute, second, millisecond, microsecond,
                    nanosecond, overflow, getContext().getRealm());
            return JSTemporalTime.createTemporalTimeFromInstance(temporalTime,
                    toInt.executeInt(dol.getOrDefault(result, JSTemporalTime.HOUR, 0)),
                    toInt.executeInt(dol.getOrDefault(result, JSTemporalTime.MINUTE, 0)),
                    toInt.executeInt(dol.getOrDefault(result, JSTemporalTime.SECOND, 0)),
                    toInt.executeInt(dol.getOrDefault(result, JSTemporalTime.MILLISECOND, 0)),
                    toInt.executeInt(dol.getOrDefault(result, JSTemporalTime.MICROSECOND, 0)),
                    toInt.executeInt(dol.getOrDefault(result, JSTemporalTime.NANOSECOND, 0)),
                    getContext().getRealm(), callNode
            );
        }
    }

    // 4.3.16
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

    // 4.3.21
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

    // 4.3.24
    public abstract static class JSTemporalTimeValueOf extends JSBuiltinNode {

        protected JSTemporalTimeValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(DynamicObject thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }
}
