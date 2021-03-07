package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeAddNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeEqualsNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeRoundNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeSinceNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeSubtractNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToPlainDateTimeNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToStringNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeUntilNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeValueOfNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeWithNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.JSTemporalTimeObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

import java.util.Collections;

public class TemporalPlainTimePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainTimePrototypeBuiltins.TemporalTimePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainTimePrototypeBuiltins();

    protected TemporalPlainTimePrototypeBuiltins() {
        super(JSTemporalPlainTime.PROTOTYPE_NAME, TemporalTimePrototype.class);
    }

    public enum TemporalTimePrototype implements BuiltinEnum<TemporalTimePrototype> {
        add(1),
        subtract(1),
        with(2),
        until(2),
        since(2),
        round(1),
        equals(1),
        toPlainDateTime(1),
        toZonedDateTime(1),
        getISOFields(0),
        toString(1),
        toLocaleString(0),
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
            case add:
                return JSTemporalPlainTimeAddNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case subtract:
                return JSTemporalPlainTimeSubtractNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case with:
                return JSTemporalPlainTimeWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case until:
                return JSTemporalPlainTimeUntilNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalPlainTimeSinceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case round:
                return JSTemporalPlainTimeRoundNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case equals:
                return JSTemporalPlainTimeEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainDateTime:
                return JSTemporalPlainTimeToPlainDateTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toZonedDateTime:
                return JSTemporalPlainTimeToZonedDateTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case getISOFields:
                return JSTemporalPlainTimeGetISOFieldsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toString:
                return JSTemporalPlainTimeToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalPlainTimeToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSTemporalPlainTimeValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    // 4.3.10
    public abstract static class JSTemporalPlainTimeAdd extends JSBuiltinNode {

        protected JSTemporalPlainTimeAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public DynamicObject add(DynamicObject thisObj, DynamicObject temporalDurationLike,
                                 @Cached("create()") IsObjectNode isObject,
                                 @Cached("create()") JSToStringNode toString,
                                 @Cached("create()") JSToIntegerAsLongNode toInt,
                                 @Cached("createNew()") JSFunctionCallNode callNode,
                                 @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            try {
                JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
                DynamicObject duration = JSTemporalDuration.toLimitedTemporalDuration(temporalDurationLike,
                        Collections.emptySet(), getContext().getRealm(), isObject, toString, toInt, dol);
                JSTemporalDuration.rejectDurationSign(
                        dol.getLongOrDefault(duration, JSTemporalDuration.YEARS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.MONTHS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.WEEKS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.DAYS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.NANOSECONDS, 0)
                );
                DynamicObject result = JSTemporalPlainTime.addTime(
                        temporalTime.getHours(), temporalTime.getMinutes(), temporalTime.getSeconds(),
                        temporalTime.getMilliseconds(), temporalTime.getMicroseconds(), temporalTime.getNanoseconds(),
                        dol.getLongOrDefault(duration, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.NANOSECONDS, 0),
                        getContext().getRealm()
                );
                result = JSTemporalPlainTime.regulateTime(
                        dol.getLongOrDefault(result, JSTemporalPlainTime.HOUR, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MINUTE, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.SECOND, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MILLISECOND, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MICROSECOND, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.NANOSECOND, 0),
                        "reject", getContext().getRealm()
                );
                return JSTemporalPlainTime.createTemporalTimeFromInstance(temporalTime,
                        dol.getLongOrDefault(result, JSTemporalPlainTime.HOUR, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MINUTE, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.SECOND, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MILLISECOND, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MICROSECOND, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.NANOSECOND, 0),
                        getContext().getRealm(), callNode
                );
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 4.3.11
    public abstract static class JSTemporalPlainTimeSubtract extends JSBuiltinNode {

        protected JSTemporalPlainTimeSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public DynamicObject subtract(DynamicObject thisObj, DynamicObject temporalDurationLike,
                                 @Cached("create()") IsObjectNode isObject,
                                 @Cached("create()") JSToStringNode toString,
                                 @Cached("create()") JSToIntegerAsLongNode toInt,
                                 @Cached("createNew()") JSFunctionCallNode callNode,
                                 @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            try {
                JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
                DynamicObject duration = JSTemporalDuration.toLimitedTemporalDuration(temporalDurationLike,
                        Collections.emptySet(), getContext().getRealm(), isObject, toString, toInt, dol);
                JSTemporalDuration.rejectDurationSign(
                        dol.getLongOrDefault(duration, JSTemporalDuration.YEARS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.MONTHS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.WEEKS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.DAYS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(duration, JSTemporalDuration.NANOSECONDS, 0)
                );
                DynamicObject result = JSTemporalPlainTime.addTime(
                        temporalTime.getHours(), temporalTime.getMinutes(), temporalTime.getSeconds(),
                        temporalTime.getMilliseconds(), temporalTime.getMicroseconds(), temporalTime.getNanoseconds(),
                        -dol.getLongOrDefault(duration, JSTemporalDuration.HOURS, 0),
                        -dol.getLongOrDefault(duration, JSTemporalDuration.MINUTES, 0),
                        -dol.getLongOrDefault(duration, JSTemporalDuration.SECONDS, 0),
                        -dol.getLongOrDefault(duration, JSTemporalDuration.MILLISECONDS, 0),
                        -dol.getLongOrDefault(duration, JSTemporalDuration.MICROSECONDS, 0),
                        -dol.getLongOrDefault(duration, JSTemporalDuration.NANOSECONDS, 0),
                        getContext().getRealm()
                );
                result = JSTemporalPlainTime.regulateTime(
                        dol.getLongOrDefault(result, JSTemporalPlainTime.HOUR, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MINUTE, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.SECOND, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MILLISECOND, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MICROSECOND, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.NANOSECOND, 0),
                        "reject", getContext().getRealm()
                );
                return JSTemporalPlainTime.createTemporalTimeFromInstance(temporalTime,
                        dol.getLongOrDefault(result, JSTemporalPlainTime.HOUR, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MINUTE, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.SECOND, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MILLISECOND, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MICROSECOND, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.NANOSECOND, 0),
                        getContext().getRealm(), callNode
                );
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 4.3.12
    public abstract static class JSTemporalPlainTimeWith extends JSBuiltinNode {

        protected JSTemporalPlainTimeWith(JSContext context, JSBuiltin builtin) {
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
            DynamicObject partialTime = JSTemporalPlainTime.toPartialTime(temporalTimeLike, getContext().getRealm(),
                    isObject, dol, toInt);
            DynamicObject normalizedOptions = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(),
                    isObject);
            String overflow = TemporalUtil.toTemporalOverflow(normalizedOptions, dol, isObject, toBoolean, toString);
            int hour, minute, second, millisecond, microsecond, nanosecond;
            Object tempValue = dol.getOrDefault(partialTime, JSTemporalPlainTime.HOUR, null);
            if (tempValue != null) {
                hour = toInt.executeInt(tempValue);
            } else {
                hour = temporalTime.getHours();
            }
            tempValue = dol.getOrDefault(partialTime, JSTemporalPlainTime.MINUTE, null);
            if(tempValue != null) {
                minute = toInt.executeInt(tempValue);
            } else {
                minute = temporalTime.getMinutes();
            }
            tempValue = dol.getOrDefault(partialTime, JSTemporalPlainTime.SECOND, null);
            if(tempValue != null) {
                second = toInt.executeInt(tempValue);
            } else {
                second = temporalTime.getSeconds();
            }
            tempValue = dol.getOrDefault(partialTime, JSTemporalPlainTime.MILLISECOND, null);
            if(tempValue != null) {
                millisecond = toInt.executeInt(tempValue);
            } else {
                millisecond = temporalTime.getMilliseconds();
            }
            tempValue = dol.getOrDefault(partialTime, JSTemporalPlainTime.MICROSECOND, null);
            if(tempValue != null) {
                microsecond = toInt.executeInt(tempValue);
            } else {
                microsecond = temporalTime.getMicroseconds();
            }
            tempValue = dol.getOrDefault(partialTime, JSTemporalPlainTime.NANOSECOND, null);
            if(tempValue != null) {
                nanosecond = toInt.executeInt(tempValue);
            } else {
                nanosecond = temporalTime.getNanoseconds();
            }
            DynamicObject result = JSTemporalPlainTime.regulateTime(hour, minute, second, millisecond, microsecond,
                    nanosecond, overflow, getContext().getRealm());
            return JSTemporalPlainTime.createTemporalTimeFromInstance(temporalTime,
                    toInt.executeInt(dol.getOrDefault(result, JSTemporalPlainTime.HOUR, 0)),
                    toInt.executeInt(dol.getOrDefault(result, JSTemporalPlainTime.MINUTE, 0)),
                    toInt.executeInt(dol.getOrDefault(result, JSTemporalPlainTime.SECOND, 0)),
                    toInt.executeInt(dol.getOrDefault(result, JSTemporalPlainTime.MILLISECOND, 0)),
                    toInt.executeInt(dol.getOrDefault(result, JSTemporalPlainTime.MICROSECOND, 0)),
                    toInt.executeInt(dol.getOrDefault(result, JSTemporalPlainTime.NANOSECOND, 0)),
                    getContext().getRealm(), callNode
            );
        }
    }

    // 4.3.13
    public abstract static class JSTemporalPlainTimeUntil extends JSBuiltinNode {

        protected JSTemporalPlainTimeUntil(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public DynamicObject until(DynamicObject thisObj, DynamicObject otherObj, DynamicObject options,
                                   @Cached("create()") IsObjectNode isObject,
                                   @Cached("create()") JSToIntegerAsIntNode toInt,
                                   @Cached("create()") JSToStringNode toString,
                                   @Cached("create()") JSToBooleanNode toBoolean,
                                   @Cached("create()") JSToNumberNode toNumber,
                                   @Cached("create()") IsConstructorNode isConstructor,
                                   @Cached("createNew()") JSFunctionCallNode callNode,
                                   @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            try {
                JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
                JSTemporalTimeObject other = (JSTemporalTimeObject) JSTemporalPlainTime.toTemporalTime(otherObj,
                        null, null, getContext().getRealm(), isObject, dol, toInt, toString,
                        isConstructor, callNode);
                options = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                String smallestUnit = TemporalUtil.toSmallestTemporalDurationUnit(options, JSTemporalDuration.NANOSECONDS,
                        TemporalUtil.toSet(JSTemporalDuration.YEARS, JSTemporalDuration.MONTHS, JSTemporalDuration.WEEKS, JSTemporalDuration.DAYS),
                        dol, isObject, toBoolean, toString);
                String largestUnit = TemporalUtil.toLargestTemporalUnit(options,
                        TemporalUtil.toSet(JSTemporalDuration.YEARS, JSTemporalDuration.MONTHS, JSTemporalDuration.WEEKS, JSTemporalDuration.DAYS),
                        JSTemporalDuration.HOURS, dol, isObject, toBoolean, toString);
                TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
                String roundingMode = TemporalUtil.toTemporalRoundingMode(options, "trunc", dol, isObject, toBoolean, toString);
                long maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
                long roundingIncrement = (long) TemporalUtil.toTemporalRoundingIncrement(options, (double) maximum, false, dol, isObject, toNumber);
                DynamicObject result = JSTemporalPlainTime.differenceTime(
                        temporalTime.getHours(), temporalTime.getMinutes(), temporalTime.getSeconds(), temporalTime.getMilliseconds(), temporalTime.getMicroseconds(), temporalTime.getNanoseconds(),
                        other.getHours(), other.getMinutes(), other.getSeconds(), other.getMilliseconds(), other.getMicroseconds(), other.getNanoseconds(),
                        getContext().getRealm(), dol
                );
                result = JSTemporalDuration.roundDuration(
                        0, 0, 0, 0,
                        dol.getLongOrDefault(result, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.NANOSECONDS, 0),
                        roundingIncrement, smallestUnit, roundingMode, null, dol, getContext().getRealm()
                );
                result = JSTemporalDuration.balanceDuration(
                        0,
                        dol.getLongOrDefault(result, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.NANOSECONDS, 0),
                        largestUnit, null, getContext().getRealm()
                );
                return JSTemporalDuration.createTemporalDuration(
                        0, 0, 0, 0,
                        dol.getLongOrDefault(result, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.NANOSECONDS, 0),
                        getContext().getRealm()
                );
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 4.3.14
    public abstract static class JSTemporalPlainTimeSince extends JSBuiltinNode {

        protected JSTemporalPlainTimeSince(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public DynamicObject until(DynamicObject thisObj, DynamicObject otherObj, DynamicObject options,
                                   @Cached("create()") IsObjectNode isObject,
                                   @Cached("create()") JSToIntegerAsIntNode toInt,
                                   @Cached("create()") JSToStringNode toString,
                                   @Cached("create()") JSToBooleanNode toBoolean,
                                   @Cached("create()") JSToNumberNode toNumber,
                                   @Cached("create()") IsConstructorNode isConstructor,
                                   @Cached("createNew()") JSFunctionCallNode callNode,
                                   @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            try {
                JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
                JSTemporalTimeObject other = (JSTemporalTimeObject) JSTemporalPlainTime.toTemporalTime(otherObj,
                        null, null, getContext().getRealm(), isObject, dol, toInt, toString,
                        isConstructor, callNode);
                options = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                String smallestUnit = TemporalUtil.toSmallestTemporalDurationUnit(options, JSTemporalDuration.NANOSECONDS,
                        TemporalUtil.toSet(JSTemporalDuration.YEARS, JSTemporalDuration.MONTHS, JSTemporalDuration.WEEKS, JSTemporalDuration.DAYS),
                        dol, isObject, toBoolean, toString);
                String largestUnit = TemporalUtil.toLargestTemporalUnit(options,
                        TemporalUtil.toSet(JSTemporalDuration.YEARS, JSTemporalDuration.MONTHS, JSTemporalDuration.WEEKS, JSTemporalDuration.DAYS),
                        JSTemporalDuration.HOURS, dol, isObject, toBoolean, toString);
                TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
                String roundingMode = TemporalUtil.toTemporalRoundingMode(options, "trunc", dol, isObject, toBoolean, toString);
                long maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
                long roundingIncrement = (long) TemporalUtil.toTemporalRoundingIncrement(options, (double) maximum, false, dol, isObject, toNumber);
                DynamicObject result = JSTemporalPlainTime.differenceTime(
                        temporalTime.getHours(), temporalTime.getMinutes(), temporalTime.getSeconds(), temporalTime.getMilliseconds(), temporalTime.getMicroseconds(), temporalTime.getNanoseconds(),
                        other.getHours(), other.getMinutes(), other.getSeconds(), other.getMilliseconds(), other.getMicroseconds(), other.getNanoseconds(),
                        getContext().getRealm(), dol
                );
                result = JSTemporalDuration.roundDuration(
                        0, 0, 0, 0,
                        -dol.getLongOrDefault(result, JSTemporalDuration.HOURS, 0),
                        -dol.getLongOrDefault(result, JSTemporalDuration.MINUTES, 0),
                        -dol.getLongOrDefault(result, JSTemporalDuration.SECONDS, 0),
                        -dol.getLongOrDefault(result, JSTemporalDuration.MILLISECONDS, 0),
                        -dol.getLongOrDefault(result, JSTemporalDuration.MICROSECONDS, 0),
                        -dol.getLongOrDefault(result, JSTemporalDuration.NANOSECONDS, 0),
                        roundingIncrement, smallestUnit, roundingMode, null, dol, getContext().getRealm()
                );
                result = JSTemporalDuration.balanceDuration(
                        0,
                        -dol.getLongOrDefault(result, JSTemporalDuration.HOURS, 0),
                        -dol.getLongOrDefault(result, JSTemporalDuration.MINUTES, 0),
                        -dol.getLongOrDefault(result, JSTemporalDuration.SECONDS, 0),
                        -dol.getLongOrDefault(result, JSTemporalDuration.MILLISECONDS, 0),
                        -dol.getLongOrDefault(result, JSTemporalDuration.MICROSECONDS, 0),
                        -dol.getLongOrDefault(result, JSTemporalDuration.NANOSECONDS, 0),
                        largestUnit, null, getContext().getRealm()
                );
                return JSTemporalDuration.createTemporalDuration(
                        0, 0, 0, 0,
                        dol.getLongOrDefault(result, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.NANOSECONDS, 0),
                        getContext().getRealm()
                );
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    // 4.3.15
    public abstract static class JSTemporalPlainTimeRound extends JSBuiltinNode {

        protected JSTemporalPlainTimeRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected JSTemporalTimeObject round(DynamicObject thisObj, DynamicObject options,
                                             @Cached("create()") IsObjectNode isObject,
                                             @CachedLibrary("thisObj") DynamicObjectLibrary dol,
                                             @Cached("create()") JSToBooleanNode toBoolean,
                                             @Cached("create()") JSToStringNode toString,
                                             @Cached("create()") JSToNumberNode toNumber,
                                             @Cached("createNew()") JSFunctionCallNode callNode) {
            try {
                JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
                if (options == null) {
                    throw Errors.createTypeError("Options should not be null.");
                }
                DynamicObject normalizedOptions = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                String smallestUnit = TemporalUtil.toSmallestTemporalUnit(normalizedOptions, Collections.singleton("day"),
                        dol, isObject, toBoolean, toString);
                String roundingMode = TemporalUtil.toTemporalRoundingMode(options, "nearest", dol, isObject, toBoolean, toString);
                int maximum;
                if (smallestUnit.equals("hour")) {
                    maximum = 24;
                } else if (smallestUnit.equals("minute") || smallestUnit.equals("second")) {
                    maximum = 60;
                } else {
                    maximum = 1000;
                }
                double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(normalizedOptions, (double) maximum,
                        false, dol, isObject, toNumber);
                DynamicObject result = JSTemporalPlainTime.roundTime(temporalTime.getHours(), temporalTime.getMinutes(),
                        temporalTime.getSeconds(), temporalTime.getMilliseconds(), temporalTime.getMicroseconds(),
                        temporalTime.getNanoseconds(), roundingIncrement, smallestUnit, roundingMode, null, getContext().getRealm());
                return JSTemporalPlainTime.createTemporalTimeFromInstance(temporalTime,
                        dol.getLongOrDefault(result, JSTemporalPlainTime.HOUR, 0L),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MINUTE, 0L),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.SECOND, 0L),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MILLISECOND, 0L),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.MICROSECOND, 0L),
                        dol.getLongOrDefault(result, JSTemporalPlainTime.NANOSECOND, 0L),
                        getContext().getRealm(), callNode
                );
            } catch (UnexpectedResultException e) {
                throw Errors.createTypeError("The result object of roundTime has not a long in it.");
            }
        }
    }

    // 4.3.16
    public abstract static class JSTemporalPlainTimeEquals extends JSBuiltinNode {

        protected JSTemporalPlainTimeEquals(JSContext context, JSBuiltin builtin) {
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

    // 4.3.17
    public abstract static class JSTemporalPlainTimeToPlainDateTime extends JSBuiltinNode {

        protected JSTemporalPlainTimeToPlainDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public DynamicObject toPlainDateTime(DynamicObject thisObj, DynamicObject temporalDateObj,
                                             @Cached("create()") IsObjectNode isObject,
                                             @Cached("create()") JSToBooleanNode toBoolean,
                                             @Cached("create()") JSToStringNode toString,
                                             @Cached("create()") IsConstructorNode isConstructor,
                                             @Cached("createNew()") JSFunctionCallNode callNode,
                                             @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
            JSTemporalPlainDateObject temporalDate = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(temporalDateObj, null,
                    null, getContext().getRealm(), isObject, dol, toBoolean, toString, isConstructor, callNode);
            return null;    // TODO: Call JSTemporalPlainDateTime.createTemporalDateTime()
        }
    }

    // 4.3.18
    public abstract static class JSTemporalPlainTimeToZonedDateTime extends JSBuiltinNode {

        protected JSTemporalPlainTimeToZonedDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public DynamicObject toZonedDateTime(DynamicObject thisObj, DynamicObject item,
                                             @Cached("create()") IsObjectNode isObject,
                                             @Cached("create()") IsConstructorNode isConstructor,
                                             @Cached("create()") JSToBooleanNode toBoolean,
                                             @Cached("create()") JSToStringNode toString,
                                             @Cached("createNew()") JSFunctionCallNode callNode,
                                             @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
            DynamicObject temporalDateLike = (DynamicObject) dol.getOrDefault(item, "plainDate", null);
            if (temporalDateLike == null) {
                throw Errors.createTypeError("Plain date is not present.");
            }
            JSTemporalPlainDateObject temporalDate = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(temporalDateLike, null,
                    null, getContext().getRealm(), isObject, dol, toBoolean, toString, isConstructor, callNode);
            DynamicObject temporalTimeZoneLike = (DynamicObject) dol.getOrDefault(item, "timeZone", null);
            DynamicObject timeZone = null;          // TODO: Call JSTemporalTimeZone.toTemporalTimeZone()
            DynamicObject temporalDateTime = null;  // TODO: Call JSTemporalPlainDateTime.createTemporalDateTime()
            DynamicObject instant = null;           // TODO: Call JSTemporalTimeZone.builtinTimeZoneGetInstantFor()
            return null;                            // TODO: Call JSTemporalZonedDateTime.createTemporalZonedDateTime()
        }
    }

    // 4.3.19
    public abstract static class JSTemporalPlainTimeGetISOFields extends JSBuiltinNode {

        protected JSTemporalPlainTimeGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject getISOFields(DynamicObject thisObj) {
            JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
            DynamicObject fields = JSObjectUtil.createOrdinaryPrototypeObject(getContext().getRealm());
            // TODO: Add calendar
            JSObjectUtil.putDataProperty(getContext(), fields, "isoHour", temporalTime.getHours());
            JSObjectUtil.putDataProperty(getContext(), fields, "isoMinute", temporalTime.getMinutes());
            JSObjectUtil.putDataProperty(getContext(), fields, "isoSecond", temporalTime.getSeconds());
            JSObjectUtil.putDataProperty(getContext(), fields, "isoMillisecond", temporalTime.getMilliseconds());
            JSObjectUtil.putDataProperty(getContext(), fields, "isoMicrosecond", temporalTime.getMicroseconds());
            JSObjectUtil.putDataProperty(getContext(), fields, "isoNanosecond", temporalTime.getNanoseconds());
            return fields;
        }
    }

    // 4.3.20
    public abstract static class JSTemporalPlainTimeToString extends JSBuiltinNode {

        protected JSTemporalPlainTimeToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected String toString(DynamicObject thisObj, DynamicObject options,
                                  @Cached("create()") IsObjectNode isObject,
                                  @Cached("create()") JSToStringNode toString,
                                  @Cached("create()") JSToNumberNode toNumber,
                                  @Cached("create()") JSToBooleanNode toBoolean,
                                  @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            try {
                JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
                options = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                DynamicObject precision = TemporalUtil.toSecondsStringPrecision(options, dol, isObject, toBoolean, toString, toNumber, getContext().getRealm());
                String roundingMode = TemporalUtil.toTemporalRoundingMode(options, "trunc", dol, isObject, toBoolean, toString);
                DynamicObject roundResult = JSTemporalPlainTime.roundTime(
                        temporalTime.getHours(), temporalTime.getMinutes(), temporalTime.getSeconds(),
                        temporalTime.getMilliseconds(), temporalTime.getMicroseconds(), temporalTime.getNanoseconds(),
                        dol.getIntOrDefault(precision, "increment", 0),
                        (String) dol.getOrDefault(precision, "unit", ""), roundingMode,
                        null, getContext().getRealm()
                );
                return JSTemporalPlainTime.temporalTimeToString(
                        dol.getLongOrDefault(roundResult, JSTemporalPlainTime.HOUR, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalPlainTime.MINUTE, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalPlainTime.SECOND, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalPlainTime.MILLISECOND, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalPlainTime.MICROSECOND, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalPlainTime.NANOSECOND, 0),
                        dol.getOrDefault(precision, "precision", null)
                );
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 4.3.21
    // 4.3.22
    public abstract static class JSTemporalPlainTimeToLocaleString extends JSBuiltinNode {

        protected JSTemporalPlainTimeToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toLocaleString(DynamicObject thisObj) {
            JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) thisObj;
            return JSTemporalPlainTime.temporalTimeToString(
                    temporalTime.getHours(), temporalTime.getMinutes(), temporalTime.getSeconds(),
                    temporalTime.getMilliseconds(), temporalTime.getMicroseconds(), temporalTime.getNanoseconds(),
                    "auto"
            );
        }
    }

    // 4.3.23
    public abstract static class JSTemporalPlainTimeValueOf extends JSBuiltinNode {

        protected JSTemporalPlainTimeValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(DynamicObject thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }
}
