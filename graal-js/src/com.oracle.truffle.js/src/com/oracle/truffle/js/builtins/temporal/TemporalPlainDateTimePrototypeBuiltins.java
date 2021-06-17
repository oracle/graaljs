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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeAddNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeRoundNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeSubtractNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToPlainTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeUntilNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeValueOfNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeWithNodeGen;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZoneObject;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDateTime;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalPlainDateTimePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainDateTimePrototypeBuiltins.TemporalPlainDateTimePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainDateTimePrototypeBuiltins();

    protected TemporalPlainDateTimePrototypeBuiltins() {
        super(JSTemporalPlainDateTime.PROTOTYPE_NAME, TemporalPlainDateTimePrototype.class);
    }

    public enum TemporalPlainDateTimePrototype implements BuiltinEnum<TemporalPlainDateTimePrototype> {
        // getters
        calendar(0),
        year(0),
        month(0),
        monthCode(0),
        day(0),
        dayOfYear(0),
        dayOfWeek(0),
        weekOfYear(0),
        daysInWeek(0),
        daysInMonth(0),
        daysInYear(0),
        monthsInYear(0),
        inLeapYear(0),
        hour(0),
        minute(0),
        second(0),
        millisecond(0),
        microsecond(0),
        nanosecond(0),

        // methods
        with(2),
        // withPlainTime(1), //TODO temporal
        // withPlainDate(1),
        // withCalendar(1),
        add(1),
        subtract(1),
        until(2),
        since(2),
        round(1),
        equals(1),
        toString(1),
        toLocaleString(0),
        toJSON(0),
        valueOf(0),
        toPlainDate(0),
        // toPlainYearMonth(0),//TODO temporal
        // toPlainMonthDay(0),
        toPlainTime(0),
        toZonedDateTime(1),
        getISOFields(0);

        private final int length;

        TemporalPlainDateTimePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(calendar, hour, minute, second, millisecond, microsecond, nanosecond, year, month, monthCode, day, dayOfYear, dayOfWeek, weekOfYear, daysInWeek, daysInMonth, daysInYear,
                            monthsInYear, inLeapYear).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainDateTimePrototype builtinEnum) {
        switch (builtinEnum) {
            case calendar:
            case year:
            case month:
            case monthCode:
            case day:
            case dayOfYear:
            case dayOfWeek:
            case weekOfYear:
            case daysInWeek:
            case daysInMonth:
            case daysInYear:
            case monthsInYear:
            case inLeapYear:
            case hour:
            case minute:
            case second:
            case millisecond:
            case microsecond:
            case nanosecond:
                return JSTemporalPlainDateTimeGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));
            case add:
                return JSTemporalPlainDateTimeAddNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case subtract:
                return JSTemporalPlainDateTimeSubtractNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case with:
                return JSTemporalPlainDateTimeWithNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(2).createArgumentNodes(context));
            case until:
                return JSTemporalPlainDateTimeUntilNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalPlainDateTimeSinceNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(2).createArgumentNodes(context));
            case round:
                return JSTemporalPlainDateTimeRoundNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(1).createArgumentNodes(context));
            case equals:
                return JSTemporalPlainDateTimeEqualsNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainTime:
                return JSTemporalPlainDateTimeToPlainTimeNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toPlainDate:
                return JSTemporalPlainDateTimeToPlainDateNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toZonedDateTime:
                return JSTemporalPlainDateTimeToZonedDateTimeNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(2).createArgumentNodes(context));
            case getISOFields:
                return JSTemporalPlainDateTimeGetISOFieldsNodeGen.create(context, builtin,
                                args().withThis().createArgumentNodes(context));
            case toString:
                return JSTemporalPlainDateTimeToStringNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalPlainDateTimeToLocaleStringNodeGen.create(context, builtin,
                                args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSTemporalPlainDateTimeValueOfNodeGen.create(context, builtin,
                                args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainDateTimeGetterNode extends JSBuiltinNode {

        public final TemporalPlainDateTimePrototype property;

        public JSTemporalPlainDateTimeGetterNode(JSContext context, JSBuiltin builtin, TemporalPlainDateTimePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization(guards = "isJSTemporalDateTime(thisObj)")
        protected Object dateTimeGetter(Object thisObj) {
            TemporalDateTime temporalDT = (TemporalDateTime) thisObj;
            switch (property) {
                case calendar:
                    return temporalDT.getCalendar();
                case hour:
                    return temporalDT.getHour();
                case minute:
                    return temporalDT.getMinute();
                case second:
                    return temporalDT.getSecond();
                case millisecond:
                    return temporalDT.getMillisecond();
                case microsecond:
                    return temporalDT.getMicrosecond();
                case nanosecond:
                    return temporalDT.getNanosecond();
                case year:
                    return temporalDT.getYear();
                case month:
                    return temporalDT.getMonth();
                case day:
                    return temporalDT.getDay();
                case dayOfWeek:
                    return TemporalUtil.dayOfWeek(temporalDT.getCalendar(), (DynamicObject) temporalDT);
                case dayOfYear:
                    return TemporalUtil.dayOfYear(temporalDT.getCalendar(), (DynamicObject) temporalDT);
                case monthCode:
                    return JSTemporalCalendar.calendarMonthCode(temporalDT.getCalendar(), (DynamicObject) temporalDT);
                case weekOfYear:
                    return JSTemporalCalendar.calendarWeekOfYear(temporalDT.getCalendar(), (DynamicObject) temporalDT);
                case daysInWeek:
                    return JSTemporalCalendar.calendarDaysInWeek(temporalDT.getCalendar(), (DynamicObject) temporalDT);
                case daysInMonth:
                    return JSTemporalCalendar.calendarDaysInMonth(temporalDT.getCalendar(), (DynamicObject) temporalDT);
                case daysInYear:
                    return JSTemporalCalendar.calendarDaysInYear(temporalDT.getCalendar(), (DynamicObject) temporalDT);
                case monthsInYear:
                    return JSTemporalCalendar.calendarMonthsInYear(temporalDT.getCalendar(), (DynamicObject) temporalDT);
                case inLeapYear:
                    return JSTemporalCalendar.calendarInLeapYear(temporalDT.getCalendar(), (DynamicObject) temporalDT);

            }
            CompilerDirectives.transferToInterpreter();
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalDateTime(thisObj)")
        protected static int error(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalDateTimeExpected();
        }
    }

    // 4.3.10
    public abstract static class JSTemporalPlainDateTimeAdd extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject add(Object thisObj, Object temporalDurationLike, Object optParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString) {
            TemporalDateTime dateTime = TemporalUtil.requireTemporalDateTime(thisObj);
            JSTemporalDurationRecord duration = TemporalUtil.toLimitedTemporalDuration(temporalDurationLike,
                            Collections.emptySet(), isObject, toString);
            TemporalUtil.rejectDurationSign(
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
            DynamicObject options = TemporalUtil.getOptionsObject(getContext(), optParam);
            JSTemporalDateTimeRecord result = TemporalUtil.addDateTime(getContext(),
                            dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(),
                            dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
                            dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(),
                            dateTime.getCalendar(),
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(),
                            options);

            return JSTemporalPlainDateTime.create(getContext(),
                            result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(),
                            result.getNanosecond(),
                            dateTime.getCalendar());
        }
    }

    public abstract static class JSTemporalPlainDateTimeSinceNode extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeSinceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject since(Object thisObj, Object otherObj, Object optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            TemporalDateTime dateTime = TemporalUtil.requireTemporalDateTime(thisObj);
            JSTemporalPlainDateTimeObject other = TemporalUtil.toTemporalDateTime(getContext(), otherObj, Undefined.instance);
            if (!TemporalUtil.calendarEquals(dateTime.getCalendar(), other.getCalendar())) {
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }

            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext(), isObject);

            String smallestUnit = TemporalUtil.toSmallestTemporalUnit(options, TemporalUtil.setEmpty, NANOSECOND, toBoolean, toString);
            String defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(DAY, smallestUnit);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(options, TemporalUtil.setEmpty, AUTO, defaultLargestUnit, toBoolean, toString);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);

            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, TRUNC, toBoolean, toString);
            roundingMode = TemporalUtil.negateTemporalRoundingMode(roundingMode);
            Double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, maximum, false, isObject, toNumber);

            JSTemporalDurationRecord diff = TemporalUtil.differenceISODateTime(getContext(), namesNode, other.getYear(), other.getMonth(), other.getDay(), other.getHour(),
                            other.getMinute(), other.getSecond(), other.getMillisecond(),
                            other.getMicrosecond(), other.getNanosecond(), dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(),
                            dateTime.getSecond(), dateTime.getMillisecond(),
                            dateTime.getMicrosecond(), dateTime.getNanosecond(), dateTime.getCalendar(), largestUnit, options);
            JSTemporalDurationRecord roundResult = TemporalUtil.roundDuration(getContext(), namesNode, -diff.getYears(), -diff.getMonths(), -diff.getWeeks(), -diff.getDays(), -diff.getHours(),
                            -diff.getMinutes(), -diff.getSeconds(), -diff.getMilliseconds(), -diff.getMicroseconds(), -diff.getNanoseconds(),
                            (long) roundingIncrement, smallestUnit, roundingMode, (DynamicObject) dateTime);
            JSTemporalDurationRecord result = TemporalUtil.balanceDuration(getContext(), namesNode, -roundResult.getDays(), -roundResult.getHours(), -roundResult.getMinutes(),
                            -roundResult.getSeconds(), -roundResult.getMilliseconds(), -roundResult.getMicroseconds(), -roundResult.getNanoseconds(), largestUnit, Undefined.instance);
            return JSTemporalDuration.createTemporalDuration(getContext(), -roundResult.getYears(), -roundResult.getMonths(), -roundResult.getWeeks(),
                            result.getDays(), result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds());
        }
    }

    public abstract static class JSTemporalPlainDateTimeUntilNode extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeUntilNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject until(Object thisObj, Object otherObj, Object optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            TemporalDateTime dateTime = TemporalUtil.requireTemporalDateTime(thisObj);
            JSTemporalPlainDateTimeObject other = TemporalUtil.toTemporalDateTime(getContext(), otherObj, Undefined.instance);
            if (!TemporalUtil.calendarEquals(dateTime.getCalendar(), other.getCalendar())) {
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }

            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext(), isObject);

            String smallestUnit = TemporalUtil.toSmallestTemporalUnit(options, TemporalUtil.setEmpty, NANOSECOND, toBoolean, toString);
            String defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(DAY, smallestUnit);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(options, TemporalUtil.setEmpty, AUTO, defaultLargestUnit, toBoolean, toString);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);

            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, TRUNC, toBoolean, toString);
            Double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, maximum, false, isObject, toNumber);

            JSTemporalDurationRecord diff = TemporalUtil.differenceISODateTime(getContext(), namesNode, other.getYear(), other.getMonth(), other.getDay(), other.getHour(),
                            other.getMinute(), other.getSecond(), other.getMillisecond(),
                            other.getMicrosecond(), other.getNanosecond(), dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(),
                            dateTime.getSecond(), dateTime.getMillisecond(),
                            dateTime.getMicrosecond(), dateTime.getNanosecond(), dateTime.getCalendar(), largestUnit, options);
            JSTemporalDurationRecord roundResult = TemporalUtil.roundDuration(getContext(), namesNode, diff.getYears(), diff.getMonths(), diff.getWeeks(), diff.getDays(), diff.getHours(),
                            diff.getMinutes(), diff.getSeconds(), diff.getMilliseconds(), diff.getMicroseconds(), diff.getNanoseconds(),
                            (long) roundingIncrement, smallestUnit, roundingMode, (DynamicObject) dateTime);
            JSTemporalDurationRecord result = TemporalUtil.balanceDuration(getContext(), namesNode, roundResult.getDays(), roundResult.getHours(), roundResult.getMinutes(), roundResult.getSeconds(),
                            roundResult.getMilliseconds(), roundResult.getMicroseconds(), roundResult.getNanoseconds(), largestUnit, Undefined.instance);

            return JSTemporalDuration.createTemporalDuration(getContext(), roundResult.getYears(), roundResult.getMonths(), roundResult.getWeeks(),
                            result.getDays(), result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds());
        }
    }

    public abstract static class JSTemporalPlainDateTimeGetISOFields extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject getISOFields(Object thisObj) {
            TemporalDateTime dt = TemporalUtil.requireTemporalDateTime(thisObj);
            DynamicObject obj = JSOrdinary.create(getContext());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, CALENDAR, dt.getCalendar());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoDay", dt.getDay());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoHour", dt.getHour());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoMonth", dt.getMonth());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoMicrosecond", dt.getMicrosecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoMillisecond", dt.getMillisecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoMinute", dt.getMinute());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoNanosecond", dt.getNanosecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoSecond", dt.getSecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoYear", dt.getYear());
            return obj;
        }
    }

    // 4.3.11
    public abstract static class JSTemporalPlainDateTimeSubtractNode extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeSubtractNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject subtract(Object thisObj, Object temporalDurationLike, Object optParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString) {
            TemporalDateTime dateTime = TemporalUtil.requireTemporalDateTime(thisObj);
            JSTemporalDurationRecord duration = TemporalUtil.toLimitedTemporalDuration(temporalDurationLike,
                            Collections.emptySet(), isObject, toString);
            TemporalUtil.rejectDurationSign(
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
            DynamicObject options = TemporalUtil.getOptionsObject(getContext(), optParam);
            JSTemporalDateTimeRecord result = TemporalUtil.addDateTime(getContext(),
                            dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(),
                            dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
                            dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(),
                            dateTime.getCalendar(),
                            -duration.getYears(), -duration.getMonths(), -duration.getWeeks(), -duration.getDays(),
                            -duration.getHours(), -duration.getMinutes(), -duration.getSeconds(),
                            -duration.getMilliseconds(), -duration.getMicroseconds(), -duration.getNanoseconds(),
                            options);

            return JSTemporalPlainDateTime.create(getContext(),
                            result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(),
                            result.getNanosecond(),
                            dateTime.getCalendar());
        }
    }

    public abstract static class JSTemporalPlainDateTimeToString extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(Object thisObj, Object optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            TemporalDateTime dt = TemporalUtil.requireTemporalDateTime(thisObj);
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext(), isObject);
            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecision(options, toBoolean, toString);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, TRUNC, toBoolean, toString);
            String showCalendar = TemporalUtil.toShowCalendarOption(options);
            JSTemporalDurationRecord result = TemporalUtil.roundISODateTime(
                            dt.getYear(), dt.getMonth(), dt.getDay(),
                            dt.getHour(), dt.getMinute(), dt.getSecond(),
                            dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(),
                            precision.getIncrement(), precision.getUnit(), roundingMode,
                            null);
            return JSTemporalPlainDateTime.temporalDateTimeToString(
                            result.getYears(), result.getMonths(), result.getDays(),
                            result.getHours(), result.getMinutes(), result.getSeconds(),
                            result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            dt.getCalendar(), precision.getPrecision(), showCalendar);
        }
    }

    public abstract static class JSTemporalPlainDateTimeToLocaleString extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toLocaleString(Object thisObj) {
            TemporalDateTime dt = TemporalUtil.requireTemporalDateTime(thisObj);
            return JSTemporalPlainDateTime.temporalDateTimeToString(dt.getYear(), dt.getMonth(), dt.getDay(),
                            dt.getHour(), dt.getMinute(), dt.getSecond(),
                            dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(),
                            dt.getCalendar(), AUTO, AUTO);
        }
    }

    public abstract static class JSTemporalPlainDateTimeValueOf extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") DynamicObject thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }

    public abstract static class JSTemporalPlainDateTimeWith extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization
        public DynamicObject with(Object thisObj, Object temporalDateTimeLike, Object optParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            TemporalDateTime dateTime = TemporalUtil.requireTemporalDateTime(thisObj);
            if (!JSRuntime.isObject(temporalDateTimeLike)) {
                throw TemporalErrors.createTypeErrorTemporalDateTimeExpected();
            }
            DynamicObject temporalDTObj = (DynamicObject) temporalDateTimeLike;
            Object calendarProperty = JSObject.get(temporalDTObj, CALENDAR);
            if (calendarProperty != Undefined.instance) {
                throw TemporalErrors.createTypeErrorUnexpectedCalendar();
            }
            Object timeZoneProperty = JSObject.get(temporalDTObj, TIME_ZONE);
            if (timeZoneProperty != Undefined.instance) {
                throw TemporalErrors.createTypeErrorUnexpectedTimeZone();
            }
            DynamicObject calendar = dateTime.getCalendar();
            Set<String> fieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.setYMMCDHMSMMN);
            DynamicObject partialDateTime = TemporalUtil.preparePartialTemporalFields(getContext(), temporalDTObj, fieldNames);
            DynamicObject options = TemporalUtil.getOptionsObject(optParam, getContext(), isObject);
            Object fields = TemporalUtil.prepareTemporalFields(getContext(), (DynamicObject) dateTime, fieldNames, TemporalUtil.setEmpty);
            fields = TemporalUtil.calendarMergeFields(getContext(), namesNode, calendar, (DynamicObject) fields, partialDateTime);
            fields = TemporalUtil.prepareTemporalFields(getContext(), (DynamicObject) fields, fieldNames, TemporalUtil.setEmpty);
            JSTemporalDateTimeRecord result = TemporalUtil.interpretTemporalDateTimeFields(calendar, (DynamicObject) fields, options);
            assert TemporalUtil.isValidISODate(result.getYear(), result.getMonth(), result.getDay());
            assert TemporalUtil.isValidTime(result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
            return TemporalUtil.createTemporalDateTime(getContext(), result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(),
                            result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), calendar);
        }
    }

    public abstract static class JSTemporalPlainDateTimeEquals extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSTemporalDateTime(otherObj)")
        protected static boolean equalsOtherObj(Object thisObj, DynamicObject otherObj) {
            TemporalDateTime temporalDT = TemporalUtil.requireTemporalDateTime(thisObj);
            return equalsIntl(temporalDT, (TemporalDateTime) otherObj);
        }

        @Specialization(guards = "!isJSTemporalDateTime(other)")
        protected boolean equalsGeneric(Object thisObj, Object other) {
            TemporalDateTime one = TemporalUtil.requireTemporalDateTime(thisObj);
            TemporalDateTime two = TemporalUtil.toTemporalDateTime(getContext(), other, null);
            return equalsIntl(one, two);
        }

        private static boolean equalsIntl(TemporalDateTime one, TemporalDateTime two) {
            int result = TemporalUtil.compareISODateTime(
                            one.getYear(), one.getMonth(), one.getDay(),
                            one.getHour(), one.getMinute(), one.getSecond(),
                            one.getMillisecond(), one.getMicrosecond(), one.getNanosecond(),
                            two.getYear(), two.getMonth(), two.getDay(),
                            two.getHour(), two.getMinute(), two.getSecond(),
                            two.getMillisecond(), two.getMicrosecond(), two.getNanosecond());
            if (result != 0) {
                return false;
            } else {
                return TemporalUtil.calendarEquals(one.getCalendar(), two.getCalendar());
            }
        }
    }

    public abstract static class JSTemporalPlainDateTimeRoundNode extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeRoundNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject round(Object thisObj, Object optParam,
                        @Cached("create()") JSToBooleanNode toBooleanNode,
                        @Cached("create()") JSToStringNode toStringNode,
                        @Cached("create()") JSToNumberNode toNumberNode,
                        @Cached("create()") IsObjectNode isObjectNode) {
            TemporalDateTime dt = TemporalUtil.requireTemporalDateTime(thisObj);

            if (optParam == Undefined.instance) {
                throw TemporalErrors.createTypeErrorOptionsUndefined();
            }
            DynamicObject options = TemporalUtil.getOptionsObject(getContext(), optParam);
            String smallestUnit = TemporalUtil.toSmallestTemporalUnit(options, TemporalUtil.setYMW, null, toBooleanNode, toStringNode);
            if (TemporalUtil.isNullish(smallestUnit)) {
                throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
            }
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, HALF_EXPAND, toBooleanNode, toStringNode);
            double roundingIncrement = TemporalUtil.toTemporalDateTimeRoundingIncrement(options, smallestUnit, isObjectNode, toNumberNode);
            JSTemporalDurationRecord result = TemporalUtil.roundISODateTime(dt.getYear(), dt.getMonth(), dt.getDay(), dt.getHour(), dt.getMinute(), dt.getSecond(),
                            dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(), roundingIncrement, smallestUnit, roundingMode, null);
            return JSTemporalPlainDateTime.create(getContext(), result.getYears(), result.getMonths(), result.getDays(),
                            result.getHours(), result.getMinutes(), result.getSeconds(),
                            result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(), dt.getCalendar());
        }
    }

    public abstract static class JSTemporalPlainDateTimeToPlainTimeNode extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeToPlainTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toPlainTime(Object thisObj) {
            TemporalDateTime dt = TemporalUtil.requireTemporalDateTime(thisObj);
            return TemporalUtil.createTemporalTime(getContext(), dt.getHour(), dt.getMinute(), dt.getSecond(), dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond());
        }
    }

    public abstract static class JSTemporalPlainDateTimeToPlainDateNode extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeToPlainDateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toPlainDate(Object thisObj) {
            TemporalDateTime dt = TemporalUtil.requireTemporalDateTime(thisObj);
            return TemporalUtil.createTemporalDate(getContext(), dt.getYear(), dt.getMonth(), dt.getDay(), dt.getCalendar());
        }
    }

    public abstract static class JSTemporalPlainDateTimeToZonedDateTimeNode extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeToZonedDateTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toZonedDateTime(Object thisObj, Object temporalTimeZoneLike, Object optionsParam) {
            TemporalDateTime dateTime = TemporalUtil.requireTemporalDateTime(thisObj);
            JSTemporalTimeZoneObject timeZone = (JSTemporalTimeZoneObject) TemporalUtil.toTemporalTimeZone(getContext(), temporalTimeZoneLike);
            DynamicObject options = TemporalUtil.getOptionsObject(getContext(), optionsParam);
            String disambiguation = TemporalUtil.toTemporalDisambiguation(options);
            JSTemporalInstantObject instant = TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, (JSTemporalPlainDateTimeObject) dateTime, disambiguation);
            return TemporalUtil.createTemporalZonedDateTime(getContext(), instant.getNanoseconds(), timeZone, dateTime.getCalendar());
        }
    }

}
