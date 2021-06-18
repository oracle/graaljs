/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.builtins.temporal;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.AUTO;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.PLAIN_DATE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.REJECT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;

import java.util.Collections;
import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltins.JSTemporalBuiltinOperation;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeAddNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeRoundNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeSubtractNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToPlainDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeUntilNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeValueOfNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeWithNodeGen;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalTime;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalPlainTimePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainTimePrototypeBuiltins.TemporalPlainTimePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainTimePrototypeBuiltins();

    protected TemporalPlainTimePrototypeBuiltins() {
        super(JSTemporalPlainTime.PROTOTYPE_NAME, TemporalPlainTimePrototype.class);
    }

    public enum TemporalPlainTimePrototype implements BuiltinEnum<TemporalPlainTimePrototype> {
        // getters
        calendar(0),
        hour(0),
        minute(0),
        second(0),
        millisecond(0),
        microsecond(0),
        nanosecond(0),

        // methods
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

        TemporalPlainTimePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(calendar, hour, minute, second, millisecond, microsecond, nanosecond).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainTimePrototype builtinEnum) {
        switch (builtinEnum) {
            case calendar:
            case hour:
            case minute:
            case second:
            case millisecond:
            case microsecond:
            case nanosecond:
                return JSTemporalPlainTimeGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));
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

    public abstract static class JSTemporalPlainTimeGetterNode extends JSBuiltinNode {

        public final TemporalPlainTimePrototype property;

        public JSTemporalPlainTimeGetterNode(JSContext context, JSBuiltin builtin, TemporalPlainTimePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization(guards = "isJSTemporalTime(thisObj)")
        protected Object timeGetter(Object thisObj) {
            TemporalTime temporalTime = (TemporalTime) thisObj;
            switch (property) {
                case calendar:
                    return temporalTime.getCalendar();
                case hour:
                    return temporalTime.getHour();
                case minute:
                    return temporalTime.getMinute();
                case second:
                    return temporalTime.getSecond();
                case millisecond:
                    return temporalTime.getMillisecond();
                case microsecond:
                    return temporalTime.getMicrosecond();
                case nanosecond:
                    return temporalTime.getNanosecond();
            }
            CompilerDirectives.transferToInterpreter();
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalTime(thisObj)")
        protected static int error(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalDateTimeExpected();
        }
    }

    // 4.3.10
    public abstract static class JSTemporalPlainTimeAdd extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject add(Object thisObj, Object temporalDurationLike,
                        @Cached("create()") JSToStringNode toString) {
            TemporalTime temporalTime = requireTemporalTime(thisObj);
            JSTemporalDurationRecord duration = TemporalUtil.toLimitedTemporalDuration(temporalDurationLike,
                            Collections.emptySet(), isObjectNode, toString);
            TemporalUtil.rejectDurationSign(
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
            JSTemporalDurationRecord result = TemporalUtil.addTime(
                            temporalTime.getHour(), temporalTime.getMinute(), temporalTime.getSecond(),
                            temporalTime.getMillisecond(), temporalTime.getMicrosecond(), temporalTime.getNanosecond(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
            JSTemporalDurationRecord result2 = TemporalUtil.regulateTime(
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            REJECT);
            return JSTemporalPlainTime.create(getContext(),
                            result2.getHours(), result2.getMinutes(), result2.getSeconds(), result2.getMilliseconds(), result2.getMicroseconds(), result2.getNanoseconds());
        }
    }

    // 4.3.11
    public abstract static class JSTemporalPlainTimeSubtract extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject subtract(Object thisObj, Object temporalDurationLike,
                        @Cached("create()") JSToStringNode toString) {
            TemporalTime temporalTime = requireTemporalTime(thisObj);
            JSTemporalDurationRecord duration = TemporalUtil.toLimitedTemporalDuration(temporalDurationLike,
                            Collections.emptySet(), isObjectNode, toString);
            TemporalUtil.rejectDurationSign(
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
            JSTemporalDurationRecord result = TemporalUtil.addTime(
                            temporalTime.getHour(), temporalTime.getMinute(), temporalTime.getSecond(),
                            temporalTime.getMillisecond(), temporalTime.getMicrosecond(), temporalTime.getNanosecond(),
                            -duration.getHours(), -duration.getMinutes(), -duration.getSeconds(),
                            -duration.getMilliseconds(), -duration.getMicroseconds(), -duration.getNanoseconds());
            JSTemporalDurationRecord result2 = TemporalUtil.regulateTime(
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            REJECT);
            return JSTemporalPlainTime.create(getContext(),
                            result2.getHours(), result2.getMinutes(), result2.getSeconds(), result2.getMilliseconds(), result2.getMicroseconds(), result2.getNanoseconds());
        }
    }

    // 4.3.12
    public abstract static class JSTemporalPlainTimeWith extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject with(Object thisObj, Object temporalTimeLike, Object options,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            TemporalTime temporalTime = requireTemporalTime(thisObj);
            if (!isObject(temporalTimeLike)) {
                errorBranch.enter();
                throw Errors.createTypeError("Temporal.Time like object expected.");
            }
            DynamicObject timeLikeObj = (DynamicObject) temporalTimeLike;
            TemporalUtil.rejectTemporalCalendarType(timeLikeObj, errorBranch);
            Object calendarProperty = JSObject.get(timeLikeObj, TemporalConstants.CALENDAR);
            if (calendarProperty != Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorUnexpectedCalendar();
            }
            Object timeZoneProperty = JSObject.get(timeLikeObj, TemporalConstants.TIME_ZONE);
            if (timeZoneProperty != Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorUnexpectedTimeZone();
            }
            DynamicObject partialTime = JSTemporalPlainTime.toPartialTime(timeLikeObj, isObjectNode, toInt, getContext());
            DynamicObject normalizedOptions = getOptionsObject(options);
            String overflow = TemporalUtil.toTemporalOverflow(normalizedOptions, toBoolean, toString);
            long hour;
            long minute;
            long second;
            long millisecond;
            long microsecond;
            long nanosecond;
            Object tempValue = JSObject.get(partialTime, HOUR);
            if (tempValue != Undefined.instance) {
                hour = toInt.executeLong(tempValue);
            } else {
                hour = temporalTime.getHour();
            }
            tempValue = JSObject.get(partialTime, MINUTE);
            if (tempValue != Undefined.instance) {
                minute = toInt.executeLong(tempValue);
            } else {
                minute = temporalTime.getMinute();
            }
            tempValue = JSObject.get(partialTime, SECOND);
            if (tempValue != Undefined.instance) {
                second = toInt.executeLong(tempValue);
            } else {
                second = temporalTime.getSecond();
            }
            tempValue = JSObject.get(partialTime, MILLISECOND);
            if (tempValue != Undefined.instance) {
                millisecond = toInt.executeLong(tempValue);
            } else {
                millisecond = temporalTime.getMillisecond();
            }
            tempValue = JSObject.get(partialTime, MICROSECOND);
            if (tempValue != Undefined.instance) {
                microsecond = toInt.executeLong(tempValue);
            } else {
                microsecond = temporalTime.getMicrosecond();
            }
            tempValue = JSObject.get(partialTime, NANOSECOND);
            if (tempValue != Undefined.instance) {
                nanosecond = toInt.executeLong(tempValue);
            } else {
                nanosecond = temporalTime.getNanosecond();
            }
            JSTemporalDurationRecord result = TemporalUtil.regulateTime(hour, minute, second, millisecond, microsecond,
                            nanosecond, overflow);
            return JSTemporalPlainTime.create(getContext(),
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds());
        }
    }

    // 4.3.13
    public abstract static class JSTemporalPlainTimeUntil extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeUntil(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject until(Object thisObj, Object otherObj, Object optionsParam,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            TemporalTime temporalTime = requireTemporalTime(thisObj);
            JSTemporalPlainTimeObject other = JSTemporalPlainTime.toTemporalTime(otherObj, null, getContext(), isObjectNode, toString);
            DynamicObject options = getOptionsObject(optionsParam);
            String smallestUnit = TemporalUtil.toSmallestTemporalUnit(options, TemporalUtil.setYMWD, NANOSECOND, toBoolean, toString);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(options, TemporalUtil.setYMWD, AUTO, HOUR, toBoolean, toString);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, "trunc", toBoolean, toString);
            Double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            long roundingIncrement = (long) TemporalUtil.toTemporalRoundingIncrement(options, maximum, false, isObjectNode, toNumber);
            JSTemporalDurationRecord result = TemporalUtil.differenceTime(
                            temporalTime.getHour(), temporalTime.getMinute(), temporalTime.getSecond(), temporalTime.getMillisecond(), temporalTime.getMicrosecond(),
                            temporalTime.getNanosecond(),
                            other.getHour(), other.getMinute(), other.getSecond(), other.getMillisecond(), other.getMicrosecond(), other.getNanosecond());
            JSTemporalDurationRecord result2 = TemporalUtil.roundDuration(getContext(), namesNode,
                            0, 0, 0, 0,
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            roundingIncrement, smallestUnit, roundingMode, Undefined.instance);
            JSTemporalDurationRecord result3 = TemporalUtil.balanceDuration(getContext(), namesNode,
                            0,
                            result2.getHours(), result2.getMinutes(), result2.getSeconds(), result2.getMilliseconds(), result2.getMicroseconds(), result2.getNanoseconds(),
                            largestUnit, Undefined.instance);
            return JSTemporalDuration.createTemporalDuration(getContext(),
                            0, 0, 0, 0,
                            result3.getHours(), result3.getMinutes(), result3.getSeconds(), result3.getMilliseconds(), result3.getMicroseconds(), result3.getNanoseconds());
        }
    }

    // 4.3.14
    public abstract static class JSTemporalPlainTimeSince extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeSince(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject since(Object thisObj, Object otherObj, Object optionsParam,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            TemporalTime temporalTime = requireTemporalTime(thisObj);
            JSTemporalPlainTimeObject other = JSTemporalPlainTime.toTemporalTime(otherObj, null, getContext(), isObjectNode, toString);
            DynamicObject options = getOptionsObject(optionsParam);
            String smallestUnit = TemporalUtil.toSmallestTemporalUnit(options, TemporalUtil.setYMWD, NANOSECOND, toBoolean, toString);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(options, TemporalUtil.setYMWD, AUTO, HOUR, toBoolean, toString);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, "trunc", toBoolean, toString);
            Double max = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double maximum = max == null ? Double.POSITIVE_INFINITY : max.doubleValue();
            long roundingIncrement = (long) TemporalUtil.toTemporalRoundingIncrement(options, maximum, false, isObjectNode, toNumber);
            JSTemporalDurationRecord result = TemporalUtil.differenceTime(
                            temporalTime.getHour(), temporalTime.getMinute(), temporalTime.getSecond(), temporalTime.getMillisecond(), temporalTime.getMicrosecond(),
                            temporalTime.getNanosecond(),
                            other.getHour(), other.getMinute(), other.getSecond(), other.getMillisecond(), other.getMicrosecond(), other.getNanosecond());
            JSTemporalDurationRecord result2 = TemporalUtil.roundDuration(getContext(), namesNode,
                            0, 0, 0, 0,
                            -result.getHours(), -result.getMinutes(), -result.getSeconds(), -result.getMilliseconds(), -result.getMicroseconds(), -result.getNanoseconds(),
                            roundingIncrement, smallestUnit, roundingMode, Undefined.instance);
            JSTemporalDurationRecord result3 = TemporalUtil.balanceDuration(getContext(), namesNode,
                            0,
                            -result2.getHours(), -result2.getMinutes(), -result2.getSeconds(), -result2.getMilliseconds(), -result2.getMicroseconds(), -result2.getNanoseconds(),
                            largestUnit);
            return JSTemporalDuration.createTemporalDuration(getContext(),
                            0, 0, 0, 0,
                            result3.getHours(), result3.getMinutes(), result3.getSeconds(), result3.getMilliseconds(), result3.getMicroseconds(), result3.getNanoseconds());
        }
    }

    // 4.3.15
    public abstract static class JSTemporalPlainTimeRound extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject round(Object thisObj, Object options,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToNumberNode toNumber) {
            TemporalTime temporalTime = requireTemporalTime(thisObj);
            if (options == Undefined.instance) {
                errorBranch.enter();
                throw Errors.createTypeError("Options should not be null.");
            }
            DynamicObject normalizedOptions = getOptionsObject(options);
            String smallestUnit = TemporalUtil.toSmallestTemporalUnit(normalizedOptions, TemporalUtil.setYMWD, null, toBoolean, toString);
            if (TemporalUtil.isNullish(smallestUnit)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorSmallestUnitExpected();
            }
            String roundingMode = TemporalUtil.toTemporalRoundingMode(normalizedOptions, HALF_EXPAND, toBoolean, toString);
            int maximum;
            if (smallestUnit.equals(HOUR)) {
                maximum = 24;
            } else if (smallestUnit.equals(MINUTE) || smallestUnit.equals(SECOND)) {
                maximum = 60;
            } else {
                maximum = 1000;
            }
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(normalizedOptions, (double) maximum,
                            false, isObjectNode, toNumber);
            JSTemporalDurationRecord result = TemporalUtil.roundTime(temporalTime.getHour(), temporalTime.getMinute(),
                            temporalTime.getSecond(), temporalTime.getMillisecond(), temporalTime.getMicrosecond(),
                            temporalTime.getNanosecond(), roundingIncrement, smallestUnit, roundingMode, null);
            return JSTemporalPlainTime.create(getContext(),
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds());
        }
    }

    // 4.3.16
    public abstract static class JSTemporalPlainTimeEquals extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSTemporalTime(otherObj)")
        protected boolean equalsOtherObj(Object thisObj, DynamicObject otherObj) {
            TemporalTime temporalTime = requireTemporalTime(thisObj);
            return equalsIntl(temporalTime, (TemporalTime) otherObj);
        }

        @Specialization(guards = "!isJSTemporalTime(other)")
        protected boolean equalsGeneric(Object thisObj, Object other,
                        @Cached("create()") JSToStringNode toString) {
            TemporalTime temporalTime = requireTemporalTime(thisObj);
            TemporalTime otherTime = JSTemporalPlainTime.toTemporalTime(other, null, getContext(), isObjectNode, toString);
            return equalsIntl(temporalTime, otherTime);
        }

        private static boolean equalsIntl(TemporalTime thisTime, TemporalTime otherTime) {
            if (thisTime.getHour() != otherTime.getHour()) {
                return false;
            }
            if (thisTime.getMinute() != otherTime.getMinute()) {
                return false;
            }
            if (thisTime.getSecond() != otherTime.getSecond()) {
                return false;
            }
            if (thisTime.getMillisecond() != otherTime.getMillisecond()) {
                return false;
            }
            if (thisTime.getMicrosecond() != otherTime.getMicrosecond()) {
                return false;
            }
            if (thisTime.getNanosecond() != otherTime.getNanosecond()) {
                return false;
            }
            return true;
        }
    }

    // 4.3.17
    public abstract static class JSTemporalPlainTimeToPlainDateTime extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeToPlainDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toPlainDateTime(Object thisObj, Object temporalDateObj,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            TemporalTime time = requireTemporalTime(thisObj);
            DynamicObject temporalDate = JSTemporalPlainDate.toTemporalDate(temporalDateObj, Undefined.instance,
                            getContext(), isObjectNode, toBoolean, toString);
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) temporalDate;

            return TemporalUtil.createTemporalDateTime(getContext(), date.getYear(), date.getMonth(), date.getDay(),
                            time.getHour(), time.getMinute(), time.getSecond(), time.getMillisecond(), time.getMicrosecond(),
                            time.getNanosecond(), date.getCalendar());
        }
    }

    // 4.3.18
    public abstract static class JSTemporalPlainTimeToZonedDateTime extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeToZonedDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toZonedDateTime(Object thisObj, Object itemParam,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            DynamicObject item = (DynamicObject) itemParam;
            TemporalTime time = requireTemporalTime(thisObj);
            Object temporalDateLike = JSObject.get(item, PLAIN_DATE);
            if (temporalDateLike == Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalDateExpected();
            }
            JSTemporalPlainDateObject date = JSTemporalPlainDate.toTemporalDate(temporalDateLike, Undefined.instance, getContext(), isObjectNode, toBoolean, toString);
            Object temporalTimeZoneLike = JSObject.get(item, TemporalConstants.TIME_ZONE);
            if (temporalTimeZoneLike == Undefined.instance || temporalTimeZoneLike == null) {
                errorBranch.enter();
                throw Errors.createTypeError("TimeZone expected");
            }
            DynamicObject timeZone = TemporalUtil.toTemporalTimeZone(getContext(), temporalTimeZoneLike);

            JSTemporalPlainDateTimeObject temporalDateTime = TemporalUtil.createTemporalDateTime(getContext(), date.getYear(), date.getMonth(), date.getDay(),
                            time.getHour(), time.getMinute(), time.getSecond(), time.getMillisecond(), time.getMicrosecond(),
                            time.getNanosecond(), date.getCalendar());
            JSTemporalInstantObject instant = TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, temporalDateTime, TemporalConstants.COMPATIBLE);
            return TemporalUtil.createTemporalZonedDateTime(getContext(), instant.getNanoseconds(), timeZone, date.getCalendar());
        }
    }

    // 4.3.19
    public abstract static class JSTemporalPlainTimeGetISOFields extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject getISOFields(Object thisObj) {
            TemporalTime time = requireTemporalTime(thisObj);
            DynamicObject fields = JSOrdinary.create(getContext());
            TemporalUtil.createDataPropertyOrThrow(getContext(), fields, TemporalConstants.CALENDAR, time.getCalendar());
            TemporalUtil.createDataPropertyOrThrow(getContext(), fields, "isoHour", time.getHour());
            TemporalUtil.createDataPropertyOrThrow(getContext(), fields, "isoMinute", time.getMinute());
            TemporalUtil.createDataPropertyOrThrow(getContext(), fields, "isoSecond", time.getSecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), fields, "isoMillisecond", time.getMillisecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), fields, "isoMicrosecond", time.getMicrosecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), fields, "isoNanosecond", time.getNanosecond());
            return fields;
        }
    }

    // 4.3.20
    public abstract static class JSTemporalPlainTimeToString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(Object thisObj, Object optionsParam,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean) {
            TemporalTime time = requireTemporalTime(thisObj);
            DynamicObject options = getOptionsObject(optionsParam);
            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecision(options, toBoolean, toString);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, TemporalConstants.TRUNC, toBoolean, toString);
            JSTemporalDurationRecord roundResult = TemporalUtil.roundTime(
                            time.getHour(), time.getMinute(), time.getSecond(),
                            time.getMillisecond(), time.getMicrosecond(), time.getNanosecond(),
                            precision.getIncrement(), precision.getUnit(), roundingMode,
                            null);
            return JSTemporalPlainTime.temporalTimeToString(
                            roundResult.getHours(), roundResult.getMinutes(), roundResult.getSeconds(), roundResult.getMilliseconds(), roundResult.getMicroseconds(), roundResult.getNanoseconds(),
                            precision.getPrecision());
        }
    }

    // 4.3.21
    // 4.3.22
    public abstract static class JSTemporalPlainTimeToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toLocaleString(Object thisObj) {
            TemporalTime time = requireTemporalTime(thisObj);
            return JSTemporalPlainTime.temporalTimeToString(
                            time.getHour(), time.getMinute(), time.getSecond(),
                            time.getMillisecond(), time.getMicrosecond(), time.getNanosecond(),
                            TemporalConstants.AUTO);
        }
    }

    // 4.3.23
    public abstract static class JSTemporalPlainTimeValueOf extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") DynamicObject thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }
}
