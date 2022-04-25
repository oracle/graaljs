/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtoi;

import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltins.JSTemporalBuiltinOperation;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeAddNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeRoundNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeSubtractNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToPlainMonthDayNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToPlainTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToPlainYearMonthNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeUntilNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeValueOfNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeWithCalendarNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeWithNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeWithPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeWithPlainTimeNodeGen;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarDateFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarGetterNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.TemporalMonthDayFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalRoundDurationNode;
import com.oracle.truffle.js.nodes.temporal.TemporalYearMonthFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.ToLimitedTemporalDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDateNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDateTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZoneObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Disambiguation;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.ShowCalendar;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

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
        with(1),
        withPlainTime(0),
        withPlainDate(1),
        withCalendar(1),
        add(1),
        subtract(1),
        until(1),
        since(1),
        round(1),
        equals(1),
        toString(0),
        toLocaleString(0),
        toJSON(0),
        valueOf(0),
        toPlainDate(0),
        toPlainYearMonth(0),
        toPlainMonthDay(0),
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
            case withPlainTime:
                return JSTemporalPlainDateTimeWithPlainTimeNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(1).createArgumentNodes(context));
            case withPlainDate:
                return JSTemporalPlainDateTimeWithPlainDateNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(1).createArgumentNodes(context));
            case withCalendar:
                return JSTemporalPlainDateTimeWithCalendarNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(1).createArgumentNodes(context));
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
            case toPlainYearMonth:
                return JSTemporalPlainDateTimeToPlainYearMonthNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toPlainMonthDay:
                return JSTemporalPlainDateTimeToPlainMonthDayNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(0).createArgumentNodes(context));
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
        protected Object dateTimeGetter(Object thisObj,
                        @Cached("create(getContext())") TemporalCalendarGetterNode calendarGetterNode) {
            JSTemporalPlainDateTimeObject temporalDT = (JSTemporalPlainDateTimeObject) thisObj;
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
                    return TemporalUtil.calendarYear(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case month:
                    return TemporalUtil.calendarMonth(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case day:
                    return TemporalUtil.calendarDay(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case dayOfWeek:
                    return TemporalUtil.calendarDayOfWeek(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case dayOfYear:
                    return TemporalUtil.calendarDayOfYear(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case monthCode:
                    return TemporalUtil.calendarMonthCode(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case weekOfYear:
                    return TemporalUtil.calendarWeekOfYear(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case daysInWeek:
                    return TemporalUtil.calendarDaysInWeek(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case daysInMonth:
                    return TemporalUtil.calendarDaysInMonth(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case daysInYear:
                    return TemporalUtil.calendarDaysInYear(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case monthsInYear:
                    return TemporalUtil.calendarMonthsInYear(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case inLeapYear:
                    return TemporalUtil.calendarInLeapYear(calendarGetterNode, temporalDT.getCalendar(), temporalDT);

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
    public abstract static class JSTemporalPlainDateTimeAdd extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject add(Object thisObj, Object temporalDurationLike, Object optParam,
                        @Cached("create()") ToLimitedTemporalDurationNode toLimitedTemporalDurationNode) {
            JSTemporalPlainDateTimeObject dateTime = requireTemporalDateTime(thisObj);
            JSTemporalDurationRecord duration = toLimitedTemporalDurationNode.executeDynamicObject(temporalDurationLike, TemporalUtil.listEmpty);
            TemporalUtil.rejectDurationSign(
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
            JSDynamicObject options = getOptionsObject(optParam);
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
                            result.getNanosecond(), dateTime.getCalendar(), errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateTimeSinceNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeSinceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject since(Object thisObj, Object otherObj, Object optionsParam,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached("create(getContext())") ToTemporalDateTimeNode toTemporalDateTime,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext())") TemporalRoundDurationNode roundDurationNode) {
            JSTemporalPlainDateTimeObject dateTime = requireTemporalDateTime(thisObj);
            JSTemporalPlainDateTimeObject other = (JSTemporalPlainDateTimeObject) toTemporalDateTime.executeDynamicObject(otherObj, Undefined.instance);
            if (!TemporalUtil.calendarEquals(dateTime.getCalendar(), other.getCalendar(), toStringNode)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }

            JSDynamicObject options = getOptionsObject(optionsParam);

            Unit smallestUnit = toSmallestTemporalUnit(options, TemporalUtil.listEmpty, NANOSECOND, equalNode);
            Unit defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(Unit.DAY, smallestUnit);
            Unit largestUnit = toLargestTemporalUnit(options, TemporalUtil.listEmpty, AUTO, defaultLargestUnit, equalNode);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);

            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode);
            roundingMode = TemporalUtil.negateTemporalRoundingMode(roundingMode);
            Double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, maximum, false, isObjectNode, toNumber);

            JSTemporalDurationRecord diff = TemporalUtil.differenceISODateTime(getContext(), namesNode, dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(),
                            dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(),
                            other.getYear(), other.getMonth(), other.getDay(), other.getHour(), other.getMinute(),
                            other.getSecond(), other.getMillisecond(), other.getMicrosecond(), other.getNanosecond(),
                            dateTime.getCalendar(), largestUnit, options);
            JSDynamicObject relativeTo = JSTemporalPlainDate.create(getContext(), dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getCalendar(), errorBranch);
            JSTemporalDurationRecord roundResult = roundDurationNode.execute(diff.getYears(), diff.getMonths(), diff.getWeeks(), diff.getDays(), diff.getHours(),
                            diff.getMinutes(), diff.getSeconds(), diff.getMilliseconds(), diff.getMicroseconds(), diff.getNanoseconds(),
                            (long) roundingIncrement, smallestUnit, roundingMode, relativeTo);
            JSTemporalDurationRecord result = TemporalUtil.balanceDuration(getContext(), namesNode, roundResult.getDays(), roundResult.getHours(), roundResult.getMinutes(),
                            roundResult.getSeconds(), roundResult.getMilliseconds(), roundResult.getMicroseconds(), roundResult.getNanoseconds(), largestUnit);
            return JSTemporalDuration.createTemporalDuration(getContext(), -roundResult.getYears(), -roundResult.getMonths(), -roundResult.getWeeks(),
                            -result.getDays(), -result.getHours(), -result.getMinutes(), -result.getSeconds(), -result.getMilliseconds(), -result.getMicroseconds(), -result.getNanoseconds(),
                            errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateTimeUntilNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeUntilNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject until(Object thisObj, Object otherObj, Object optionsParam,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached("create(getContext())") ToTemporalDateTimeNode toTemporalDateTime,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext())") TemporalRoundDurationNode roundDurationNode) {
            JSTemporalPlainDateTimeObject dateTime = requireTemporalDateTime(thisObj);
            JSTemporalPlainDateTimeObject other = (JSTemporalPlainDateTimeObject) toTemporalDateTime.executeDynamicObject(otherObj, Undefined.instance);
            if (!TemporalUtil.calendarEquals(dateTime.getCalendar(), other.getCalendar(), toStringNode)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }

            JSDynamicObject options = getOptionsObject(optionsParam);

            Unit smallestUnit = toSmallestTemporalUnit(options, TemporalUtil.listEmpty, NANOSECOND, equalNode);
            Unit defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(Unit.DAY, smallestUnit);
            Unit largestUnit = toLargestTemporalUnit(options, TemporalUtil.listEmpty, AUTO, defaultLargestUnit, equalNode);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);

            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode);
            Double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, maximum, false, isObjectNode, toNumber);

            JSTemporalDurationRecord diff = TemporalUtil.differenceISODateTime(getContext(), namesNode, dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(),
                            dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(), other.getYear(), other.getMonth(),
                            other.getDay(), other.getHour(), other.getMinute(), other.getSecond(), other.getMillisecond(), other.getMicrosecond(), other.getNanosecond(), dateTime.getCalendar(),
                            largestUnit, options);
            JSDynamicObject relativeTo = JSTemporalPlainDate.create(getContext(), dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getCalendar(), errorBranch);
            JSTemporalDurationRecord roundResult = roundDurationNode.execute(diff.getYears(), diff.getMonths(), diff.getWeeks(), diff.getDays(), diff.getHours(),
                            diff.getMinutes(), diff.getSeconds(), diff.getMilliseconds(), diff.getMicroseconds(), diff.getNanoseconds(),
                            (long) roundingIncrement, smallestUnit, roundingMode, relativeTo);
            JSTemporalDurationRecord result = TemporalUtil.balanceDuration(getContext(), namesNode, roundResult.getDays(), roundResult.getHours(), roundResult.getMinutes(), roundResult.getSeconds(),
                            roundResult.getMilliseconds(), roundResult.getMicroseconds(), roundResult.getNanoseconds(), largestUnit);

            return JSTemporalDuration.createTemporalDuration(getContext(), roundResult.getYears(), roundResult.getMonths(), roundResult.getWeeks(),
                            result.getDays(), result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(), errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateTimeGetISOFields extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject getISOFields(Object thisObj) {
            JSTemporalPlainDateTimeObject dt = requireTemporalDateTime(thisObj);
            JSObject obj = JSOrdinary.create(getContext(), getRealm());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, CALENDAR, dt.getCalendar());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_DAY, dt.getDay());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_HOUR, dt.getHour());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_MICROSECOND, dt.getMicrosecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_MILLISECOND, dt.getMillisecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_MINUTE, dt.getMinute());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_MONTH, dt.getMonth());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_NANOSECOND, dt.getNanosecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_SECOND, dt.getSecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_YEAR, dt.getYear());
            return obj;
        }
    }

    // 4.3.11
    public abstract static class JSTemporalPlainDateTimeSubtractNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeSubtractNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject subtract(Object thisObj, Object temporalDurationLike, Object optParam,
                        @Cached("create()") ToLimitedTemporalDurationNode toLimitedTemporalDurationNode) {
            JSTemporalPlainDateTimeObject dateTime = requireTemporalDateTime(thisObj);
            JSTemporalDurationRecord duration = toLimitedTemporalDurationNode.executeDynamicObject(temporalDurationLike, TemporalUtil.listEmpty);
            TemporalUtil.rejectDurationSign(
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
            JSDynamicObject options = getOptionsObject(optParam);
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
                            result.getNanosecond(), dateTime.getCalendar(), errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateTimeToString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(Object thisObj, Object optionsParam,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode) {
            JSTemporalPlainDateTimeObject dt = requireTemporalDateTime(thisObj);
            JSDynamicObject options = getOptionsObject(optionsParam);
            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecision(options, toStringNode, getOptionNode(), equalNode);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode);
            ShowCalendar showCalendar = TemporalUtil.toShowCalendarOption(options, getOptionNode(), equalNode);
            JSTemporalDurationRecord result = TemporalUtil.roundISODateTime(
                            dt.getYear(), dt.getMonth(), dt.getDay(),
                            dt.getHour(), dt.getMinute(), dt.getSecond(),
                            dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(),
                            precision.getIncrement(), precision.getUnit(), roundingMode,
                            null);
            return JSTemporalPlainDateTime.temporalDateTimeToString(
                            dtoi(result.getYears()), dtoi(result.getMonths()), dtoi(result.getDays()),
                            dtoi(result.getHours()), dtoi(result.getMinutes()), dtoi(result.getSeconds()),
                            dtoi(result.getMilliseconds()), dtoi(result.getMicroseconds()), dtoi(result.getNanoseconds()),
                            dt.getCalendar(), precision.getPrecision(), showCalendar);
        }
    }

    public abstract static class JSTemporalPlainDateTimeToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public TruffleString toLocaleString(Object thisObj) {
            JSTemporalPlainDateTimeObject dt = requireTemporalDateTime(thisObj);
            return JSTemporalPlainDateTime.temporalDateTimeToString(dt.getYear(), dt.getMonth(), dt.getDay(),
                            dt.getHour(), dt.getMinute(), dt.getSecond(),
                            dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(),
                            dt.getCalendar(), AUTO, ShowCalendar.AUTO);
        }
    }

    public abstract static class JSTemporalPlainDateTimeValueOf extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }

    public abstract static class JSTemporalPlainDateTimeWith extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization
        public JSDynamicObject with(Object thisObj, Object temporalDateTimeLike, Object optParam,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached("create(getContext())") TemporalCalendarFieldsNode calendarFieldsNode,
                        @Cached("create(getContext())") TemporalCalendarDateFromFieldsNode dateFromFieldsNode) {
            JSTemporalPlainDateTimeObject dateTime = requireTemporalDateTime(thisObj);
            if (!isObject(temporalDateTimeLike)) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalDateTimeExpected();
            }
            JSDynamicObject temporalDTObj = (JSDynamicObject) temporalDateTimeLike;
            Object calendarProperty = JSObject.get(temporalDTObj, CALENDAR);
            if (calendarProperty != Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorUnexpectedCalendar();
            }
            Object timeZoneProperty = JSObject.get(temporalDTObj, TIME_ZONE);
            if (timeZoneProperty != Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorUnexpectedTimeZone();
            }
            JSDynamicObject calendar = dateTime.getCalendar();
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listDHMMMMMNSY);
            JSDynamicObject partialDateTime = TemporalUtil.preparePartialTemporalFields(getContext(), temporalDTObj, fieldNames);
            JSDynamicObject options = getOptionsObject(optParam);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), dateTime, fieldNames, TemporalUtil.listEmpty);
            fields = TemporalUtil.calendarMergeFields(getContext(), namesNode, errorBranch, calendar, fields, partialDateTime);
            fields = TemporalUtil.prepareTemporalFields(getContext(), fields, fieldNames, TemporalUtil.listEmpty);
            JSTemporalDateTimeRecord result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, options, getOptionNode, dateFromFieldsNode);
            assert TemporalUtil.isValidISODate(result.getYear(), result.getMonth(), result.getDay());
            assert TemporalUtil.isValidTime(result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
            return JSTemporalPlainDateTime.create(getContext(), result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(),
                            result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), calendar, errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateTimeEquals extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSTemporalDateTime(otherObj)")
        protected boolean equalsOtherObj(Object thisObj, JSDynamicObject otherObj,
                        @Shared("toString") @Cached JSToStringNode toStringNode) {
            JSTemporalPlainDateTimeObject temporalDT = requireTemporalDateTime(thisObj);
            return equalsIntl(temporalDT, (JSTemporalPlainDateTimeObject) otherObj, toStringNode);
        }

        @Specialization(guards = "!isJSTemporalDateTime(other)")
        protected boolean equalsGeneric(Object thisObj, Object other,
                        @Cached("create(getContext())") ToTemporalDateTimeNode toTemporalDateTime,
                        @Shared("toString") @Cached JSToStringNode toStringNode) {
            JSTemporalPlainDateTimeObject one = requireTemporalDateTime(thisObj);
            JSTemporalPlainDateTimeObject two = (JSTemporalPlainDateTimeObject) toTemporalDateTime.executeDynamicObject(other, Undefined.instance);
            return equalsIntl(one, two, toStringNode);
        }

        private static boolean equalsIntl(JSTemporalPlainDateTimeObject one, JSTemporalPlainDateTimeObject two, JSToStringNode toStringNode) {
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
                return TemporalUtil.calendarEquals(one.getCalendar(), two.getCalendar(), toStringNode);
            }
        }
    }

    public abstract static class JSTemporalPlainDateTimeRoundNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeRoundNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject round(Object thisObj, Object roundToParam,
                        @Cached("create()") JSToNumberNode toNumberNode,
                        @Cached TruffleString.EqualNode equalNode) {
            JSTemporalPlainDateTimeObject dt = requireTemporalDateTime(thisObj);
            if (roundToParam == Undefined.instance) {
                throw TemporalErrors.createTypeErrorOptionsUndefined();
            }
            JSDynamicObject roundTo;
            if (Strings.isTString(roundToParam)) {
                roundTo = JSOrdinary.createWithNullPrototype(getContext());
                JSRuntime.createDataPropertyOrThrow(roundTo, TemporalConstants.SMALLEST_UNIT, JSRuntime.toStringIsString(roundToParam));
            } else {
                roundTo = getOptionsObject(roundToParam);
            }
            Unit smallestUnit = toSmallestTemporalUnit(roundTo, TemporalUtil.listYMW, null, equalNode);
            if (smallestUnit == Unit.EMPTY) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
            }
            RoundingMode roundingMode = toTemporalRoundingMode(roundTo, HALF_EXPAND, equalNode);
            double roundingIncrement = TemporalUtil.toTemporalDateTimeRoundingIncrement(roundTo, smallestUnit, isObjectNode, toNumberNode);
            JSTemporalDurationRecord result = TemporalUtil.roundISODateTime(dt.getYear(), dt.getMonth(), dt.getDay(), dt.getHour(), dt.getMinute(), dt.getSecond(),
                            dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(), roundingIncrement, smallestUnit, roundingMode, null);
            return JSTemporalPlainDateTime.create(getContext(), dtoi(result.getYears()), dtoi(result.getMonths()), dtoi(result.getDays()),
                            dtoi(result.getHours()), dtoi(result.getMinutes()), dtoi(result.getSeconds()),
                            dtoi(result.getMilliseconds()), dtoi(result.getMicroseconds()), dtoi(result.getNanoseconds()), dt.getCalendar(), errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateTimeToPlainTimeNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToPlainTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject toPlainTime(Object thisObj) {
            JSTemporalPlainDateTimeObject dt = requireTemporalDateTime(thisObj);
            return JSTemporalPlainTime.create(getContext(), dt.getHour(), dt.getMinute(), dt.getSecond(), dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(), errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateTimeToPlainDateNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToPlainDateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject toPlainDate(Object thisObj) {
            JSTemporalPlainDateTimeObject dt = requireTemporalDateTime(thisObj);
            return JSTemporalPlainDate.create(getContext(), dt.getYear(), dt.getMonth(), dt.getDay(), dt.getCalendar(), errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateTimeToZonedDateTimeNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToZonedDateTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject toZonedDateTime(Object thisObj, Object temporalTimeZoneLike, Object optionsParam,
                        @Cached("create(getContext())") ToTemporalTimeZoneNode toTemporalTimeZone,
                        @Cached TruffleString.EqualNode equalNode) {
            JSTemporalPlainDateTimeObject dateTime = requireTemporalDateTime(thisObj);
            JSTemporalTimeZoneObject timeZone = (JSTemporalTimeZoneObject) toTemporalTimeZone.executeDynamicObject(temporalTimeZoneLike);
            JSDynamicObject options = getOptionsObject(optionsParam);
            Disambiguation disambiguation = TemporalUtil.toTemporalDisambiguation(options, getOptionNode(), equalNode);
            JSTemporalInstantObject instant = TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, dateTime, disambiguation);
            return JSTemporalZonedDateTime.create(getContext(), getRealm(), instant.getNanoseconds(), timeZone, dateTime.getCalendar());
        }
    }

    public abstract static class JSTemporalPlainDateTimeToPlainYearMonthNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToPlainYearMonthNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject toPlainYearMonth(Object thisObj,
                        @Cached("create(getContext())") TemporalYearMonthFromFieldsNode yearMonthFromFieldsNode,
                        @Cached("create(getContext())") TemporalCalendarFieldsNode calendarFieldsNode) {
            JSTemporalPlainDateTimeObject dateTime = requireTemporalDateTime(thisObj);
            JSDynamicObject calendar = dateTime.getCalendar();
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listMCY);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), dateTime, fieldNames, TemporalUtil.listEmpty);
            return yearMonthFromFieldsNode.execute(calendar, fields, Undefined.instance);
        }
    }

    public abstract static class JSTemporalPlainDateTimeToPlainMonthDayNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToPlainMonthDayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject toPlainMonthDay(Object thisObj,
                        @Cached("create(getContext())") TemporalMonthDayFromFieldsNode monthDayFromFieldsNode,
                        @Cached("create(getContext())") TemporalCalendarFieldsNode calendarFieldsNode) {
            JSTemporalPlainDateTimeObject dateTime = requireTemporalDateTime(thisObj);
            JSDynamicObject calendar = dateTime.getCalendar();
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listDMC);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), dateTime, fieldNames, TemporalUtil.listEmpty);
            return monthDayFromFieldsNode.execute(calendar, fields, Undefined.instance);
        }
    }

    public abstract static class JSTemporalPlainDateTimeWithPlainTimeNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeWithPlainTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject withPlainTime(Object thisObj, Object plainTimeLike,
                        @Cached("create(getContext())") ToTemporalTimeNode toTemporalTime) {
            JSTemporalPlainDateTimeObject temporalDateTime = requireTemporalDateTime(thisObj);
            if (plainTimeLike == Undefined.instance) {
                return JSTemporalPlainDateTime.create(getContext(), temporalDateTime.getYear(), temporalDateTime.getMonth(), temporalDateTime.getDay(), 0, 0, 0, 0, 0, 0,
                                temporalDateTime.getCalendar(), errorBranch);
            }
            JSTemporalPlainTimeObject plainTime = (JSTemporalPlainTimeObject) toTemporalTime.executeDynamicObject(plainTimeLike, null);
            return JSTemporalPlainDateTime.create(getContext(), temporalDateTime.getYear(), temporalDateTime.getMonth(), temporalDateTime.getDay(), plainTime.getHour(), plainTime.getMinute(),
                            plainTime.getSecond(), plainTime.getMillisecond(), plainTime.getMicrosecond(), plainTime.getNanosecond(), temporalDateTime.getCalendar(), errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateTimeWithPlainDateNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeWithPlainDateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject withPlainDate(Object thisObj, Object plainDateLike,
                        @Cached("create(getContext())") ToTemporalDateNode toTemporalDate,
                        @Cached JSToStringNode toStringNode) {
            JSTemporalPlainDateTimeObject temporalDateTime = requireTemporalDateTime(thisObj);
            JSTemporalPlainDateObject plainDate = toTemporalDate.executeDynamicObject(plainDateLike, Undefined.instance);
            JSDynamicObject calendar = TemporalUtil.consolidateCalendars(temporalDateTime.getCalendar(), plainDate.getCalendar(), toStringNode);
            return JSTemporalPlainDateTime.create(getContext(), plainDate.getYear(), plainDate.getMonth(), plainDate.getDay(), temporalDateTime.getHour(), temporalDateTime.getMinute(),
                            temporalDateTime.getSecond(), temporalDateTime.getMillisecond(), temporalDateTime.getMicrosecond(), temporalDateTime.getNanosecond(), calendar, errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateTimeWithCalendarNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeWithCalendarNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject withCalendar(Object thisObj, Object calendarParam,
                        @Cached("create(getContext())") ToTemporalCalendarNode toTemporalCalendar) {
            JSTemporalPlainDateTimeObject temporalDateTime = requireTemporalDateTime(thisObj);
            JSDynamicObject calendar = toTemporalCalendar.executeDynamicObject(calendarParam);
            return JSTemporalPlainDateTime.create(getContext(), temporalDateTime.getYear(), temporalDateTime.getMonth(), temporalDateTime.getDay(), temporalDateTime.getHour(),
                            temporalDateTime.getMinute(), temporalDateTime.getSecond(), temporalDateTime.getMillisecond(), temporalDateTime.getMicrosecond(), temporalDateTime.getNanosecond(),
                            calendar, errorBranch);
        }
    }

}
