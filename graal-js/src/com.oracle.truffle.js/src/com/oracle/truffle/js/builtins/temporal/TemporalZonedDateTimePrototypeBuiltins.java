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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.OFFSET;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.PREFER;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtoi;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtol;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltins.JSTemporalBuiltinOperation;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeAddNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeRoundNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeStartOfDayNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeSubtractNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToInstantNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToPlainDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToPlainMonthDayNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToPlainTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToPlainYearMonthNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeUntilNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeValueOfNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeWithCalendarNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeWithNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeWithPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeWithPlainTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeWithTimeZoneNodeGen;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.cast.JSNumberToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarDateFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarGetterNode;
import com.oracle.truffle.js.nodes.temporal.TemporalDurationAddNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.TemporalMonthDayFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalRoundDurationNode;
import com.oracle.truffle.js.nodes.temporal.TemporalYearMonthFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.ToLimitedTemporalDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDateNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalZonedDateTimeNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Disambiguation;
import com.oracle.truffle.js.runtime.util.TemporalUtil.MatchBehaviour;
import com.oracle.truffle.js.runtime.util.TemporalUtil.OffsetBehaviour;
import com.oracle.truffle.js.runtime.util.TemporalUtil.OffsetOption;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Overflow;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.ShowCalendar;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

public class TemporalZonedDateTimePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalZonedDateTimePrototypeBuiltins.TemporalZonedDateTimePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalZonedDateTimePrototypeBuiltins();

    protected TemporalZonedDateTimePrototypeBuiltins() {
        super(JSTemporalZonedDateTime.PROTOTYPE_NAME, TemporalZonedDateTimePrototype.class);
    }

    public enum TemporalZonedDateTimePrototype implements BuiltinEnum<TemporalZonedDateTimePrototype> {
        // getters
        calendar(0),
        timeZone(0),
        year(0),
        month(0),
        monthCode(0),
        day(0),
        hour(0),
        minute(0),
        second(0),
        millisecond(0),
        microsecond(0),
        nanosecond(0),
        epochSeconds(0),
        epochMilliseconds(0),
        epochMicroseconds(0),
        epochNanoseconds(0),
        dayOfWeek(0),
        dayOfYear(0),
        weekOfYear(0),
        hoursInDay(0),
        daysInWeek(0),
        daysInMonth(0),
        daysInYear(0),
        monthsInYear(0),
        inLeapYear(0),
        offsetNanoseconds(0),
        offset(0),

        // methods
        with(1),
        withPlainTime(0),
        withPlainDate(1),
        withTimeZone(1),
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
        startOfDay(0),
        toInstant(0),
        toPlainDate(0),
        toPlainTime(0),
        toPlainDateTime(0),
        toPlainYearMonth(0),
        toPlainMonthDay(0),
        getISOFields(0);

        private final int length;

        TemporalZonedDateTimePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.range(calendar, offset).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalZonedDateTimePrototype builtinEnum) {
        switch (builtinEnum) {
            case calendar:
            case timeZone:
            case year:
            case month:
            case monthCode:
            case day:
            case hour:
            case minute:
            case second:
            case millisecond:
            case microsecond:
            case nanosecond:
            case epochSeconds:
            case epochMilliseconds:
            case epochMicroseconds:
            case epochNanoseconds:
            case dayOfWeek:
            case dayOfYear:
            case weekOfYear:
            case hoursInDay:
            case daysInWeek:
            case daysInMonth:
            case daysInYear:
            case monthsInYear:
            case inLeapYear:
            case offsetNanoseconds:
            case offset:
                return JSTemporalZonedDateTimeGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

            case toString:
                return JSTemporalZonedDateTimeToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
                return JSTemporalZonedDateTimeToLocaleStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toJSON:
                return JSTemporalZonedDateTimeToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSTemporalZonedDateTimeValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case with:
                return JSTemporalZonedDateTimeWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case withPlainTime:
                return JSTemporalZonedDateTimeWithPlainTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case withPlainDate:
                return JSTemporalZonedDateTimeWithPlainDateNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case withTimeZone:
                return JSTemporalZonedDateTimeWithTimeZoneNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case withCalendar:
                return JSTemporalZonedDateTimeWithCalendarNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case add:
                return JSTemporalZonedDateTimeAddNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case subtract:
                return JSTemporalZonedDateTimeSubtractNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case until:
                return JSTemporalZonedDateTimeUntilNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalZonedDateTimeSinceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case round:
                return JSTemporalZonedDateTimeRoundNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case equals:
                return JSTemporalZonedDateTimeEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case startOfDay:
                return JSTemporalZonedDateTimeStartOfDayNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toInstant:
                return JSTemporalZonedDateTimeToInstantNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toPlainDate:
                return JSTemporalZonedDateTimeToPlainDateNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toPlainTime:
                return JSTemporalZonedDateTimeToPlainTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toPlainDateTime:
                return JSTemporalZonedDateTimeToPlainDateTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toPlainYearMonth:
                return JSTemporalZonedDateTimeToPlainYearMonthNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toPlainMonthDay:
                return JSTemporalZonedDateTimeToPlainMonthDayNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case getISOFields:
                return JSTemporalZonedDateTimeGetISOFieldsNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalZonedDateTimeGetterNode extends JSBuiltinNode {

        public final TemporalZonedDateTimePrototype property;
        @Child private TemporalCalendarGetterNode calendarGetterNode;

        public JSTemporalZonedDateTimeGetterNode(JSContext context, JSBuiltin builtin, TemporalZonedDateTimePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization(guards = "isJSTemporalZonedDateTime(thisObj)")
        protected Object zonedDateTimeGetter(Object thisObj) {
            JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) thisObj;
            switch (property) {
                case calendar:
                    return zdt.getCalendar();
                case timeZone:
                    return zdt.getTimeZone();
                case year:
                case month:
                case monthCode:
                case day:
                case hour:
                case minute:
                case second:
                case millisecond:
                case microsecond:
                case nanosecond:
                case dayOfWeek:
                case dayOfYear:
                case weekOfYear:
                case daysInWeek:
                case daysInMonth:
                case daysInYear:
                case monthsInYear:
                case inLeapYear:
                    return getterCalendarDetails(zdt);

                case hoursInDay:
                case epochSeconds:
                case epochMilliseconds:
                case epochMicroseconds:
                case epochNanoseconds:
                case offsetNanoseconds:
                case offset:
                    return zonedDateTimeGetterIntl(zdt);
            }
            throw Errors.shouldNotReachHere();
        }

        @TruffleBoundary
        protected Object zonedDateTimeGetterIntl(JSTemporalZonedDateTimeObject zdt) {
            switch (property) {
                case hoursInDay:
                    return getterHoursInDay(zdt);
                case epochSeconds:
                    return getterEpoch(zdt, BigInt.valueOf(1_000_000_000L)).bigIntegerValue().longValue();
                case epochMilliseconds:
                    return getterEpoch(zdt, BigInt.valueOf(1_000_000L)).bigIntegerValue().longValue();
                case epochMicroseconds:
                    return getterEpoch(zdt, BigInt.valueOf(1000L));
                case epochNanoseconds:
                    return getterEpoch(zdt, BigInt.ONE);
                case offsetNanoseconds:
                    return getterOffsetNanoseconds(zdt);
                case offset:
                    return getterOffset(zdt);
            }
            throw Errors.shouldNotReachHere();
        }

        private Object getterOffset(JSTemporalZonedDateTimeObject zdt) {
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), getRealm(), zdt.getNanoseconds());
            return TemporalUtil.builtinTimeZoneGetOffsetStringFor(zdt.getTimeZone(), instant);
        }

        private Object getterOffsetNanoseconds(JSTemporalZonedDateTimeObject zdt) {
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), getRealm(), zdt.getNanoseconds());
            return TemporalUtil.getOffsetNanosecondsFor(zdt.getTimeZone(), instant);
        }

        private Object getterHoursInDay(JSTemporalZonedDateTimeObject zdt) {
            JSDynamicObject timeZone = zdt.getTimeZone();
            JSRealm realm = getRealm();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), realm, zdt.getNanoseconds());
            JSDynamicObject isoCalendar = TemporalUtil.getISO8601Calendar(getContext(), realm);
            JSTemporalPlainDateTimeObject temporalDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, isoCalendar);
            int year = temporalDateTime.getYear();
            int month = temporalDateTime.getMonth();
            int day = temporalDateTime.getDay();
            JSTemporalPlainDateTimeObject today = JSTemporalPlainDateTime.create(getContext(), year, month, day, 0, 0, 0, 0, 0, 0, isoCalendar);
            JSTemporalDateTimeRecord tomorrowFields = TemporalUtil.addISODate(year, month, day, 0, 0, 0, 1, Overflow.REJECT);
            JSTemporalPlainDateTimeObject tomorrow = JSTemporalPlainDateTime.create(getContext(), tomorrowFields.getYear(), tomorrowFields.getMonth(), tomorrowFields.getDay(), 0, 0, 0, 0, 0, 0,
                            isoCalendar);
            JSTemporalInstantObject todayInstant = TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, today, Disambiguation.COMPATIBLE);
            JSTemporalInstantObject tomorrowInstant = TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, tomorrow, Disambiguation.COMPATIBLE);
            BigInt diffNs = tomorrowInstant.getNanoseconds().subtract(todayInstant.getNanoseconds());
            return diffNs.divide(BigInt.valueOf(36_000_000_000_000L));
        }

        private static BigInt getterEpoch(JSTemporalZonedDateTimeObject zdt, BigInt factor) {
            BigInt ns = zdt.getNanoseconds();
            BigInt s = ns.divide(factor); // roundTowardsZero is a no-op
            return s;
        }

        private Object getterCalendarDetails(JSTemporalZonedDateTimeObject zdt) {
            JSDynamicObject timeZone = zdt.getTimeZone();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), getRealm(), zdt.getNanoseconds());
            JSDynamicObject calendar = zdt.getCalendar();
            JSTemporalPlainDateTimeObject tdt = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, calendar);
            if (calendarGetterNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                calendarGetterNode = insert(TemporalCalendarGetterNode.create(getContext()));
            }
            switch (property) {
                case year:
                    return TemporalUtil.calendarYear(calendarGetterNode, calendar, tdt);
                case month:
                    return TemporalUtil.calendarMonth(calendarGetterNode, calendar, tdt);
                case monthCode:
                    return TemporalUtil.calendarMonthCode(calendarGetterNode, calendar, tdt);
                case day:
                    return TemporalUtil.calendarDay(calendarGetterNode, calendar, tdt);
                case hour:
                    return tdt.getHour();
                case minute:
                    return tdt.getMinute();
                case second:
                    return tdt.getSecond();
                case millisecond:
                    return tdt.getMillisecond();
                case microsecond:
                    return tdt.getMicrosecond();
                case nanosecond:
                    return tdt.getNanosecond();
                case dayOfWeek:
                    return TemporalUtil.calendarDayOfWeek(calendarGetterNode, calendar, tdt);
                case dayOfYear:
                    return TemporalUtil.calendarDayOfYear(calendarGetterNode, calendar, tdt);
                case weekOfYear:
                    return TemporalUtil.calendarWeekOfYear(calendarGetterNode, calendar, tdt);
                case daysInWeek:
                    return TemporalUtil.calendarWeekOfYear(calendarGetterNode, calendar, tdt);
                case daysInMonth:
                    return TemporalUtil.calendarDaysInMonth(calendarGetterNode, calendar, tdt);
                case daysInYear:
                    return TemporalUtil.calendarDaysInYear(calendarGetterNode, calendar, tdt);
                case monthsInYear:
                    return TemporalUtil.calendarMonthsInYear(calendarGetterNode, calendar, tdt);
                case inLeapYear:
                    return TemporalUtil.calendarInLeapYear(calendarGetterNode, calendar, tdt);
            }
            CompilerDirectives.transferToInterpreter();
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        protected static int error(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeToString extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(Object thisObj, Object optionsParam,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode) {
            JSDynamicObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            JSDynamicObject options = getOptionsObject(optionsParam);
            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecision(options, toStringNode, getOptionNode(), equalNode);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TemporalConstants.TRUNC, equalNode);
            ShowCalendar showCalendar = TemporalUtil.toShowCalendarOption(options, getOptionNode(), equalNode);
            TruffleString showTimeZone = TemporalUtil.toShowTimeZoneNameOption(options, getOptionNode());
            TruffleString showOffset = TemporalUtil.toShowOffsetOption(options, getOptionNode());
            return TemporalUtil.temporalZonedDateTimeToString(getContext(), getRealm(), zonedDateTime, precision.getPrecision(), showCalendar, showTimeZone, showOffset, precision.getIncrement(),
                            precision.getUnit(), roundingMode);
        }
    }

    public abstract static class JSTemporalZonedDateTimeToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public TruffleString toLocaleString(Object thisObj) {
            JSDynamicObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            return TemporalUtil.temporalZonedDateTimeToString(getContext(), getRealm(), zonedDateTime, AUTO, ShowCalendar.AUTO, AUTO, AUTO);
        }
    }

    public abstract static class JSTemporalZonedDateTimeValueOf extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }

    public abstract static class JSTemporalZonedDateTimeWith extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object with(Object thisObj, Object temporalZonedDateTimeLike, Object optionsParam,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext())") TemporalCalendarFieldsNode calendarFieldsNode,
                        @Cached("create(getContext())") TemporalCalendarDateFromFieldsNode dateFromFieldsNode) {
            JSTemporalZonedDateTimeObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            if (!isObject(temporalZonedDateTimeLike)) {
                errorBranch.enter();
                throw Errors.createTypeError("object expected");
            }
            JSDynamicObject temporalZDTLike = TemporalUtil.toJSDynamicObject(temporalZonedDateTimeLike, errorBranch);
            TemporalUtil.rejectTemporalCalendarType(temporalZDTLike, errorBranch);
            Object calendarProperty = JSObject.get(temporalZDTLike, CALENDAR);
            if (calendarProperty != Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorUnexpectedCalendar();
            }
            Object timeZoneProperty = JSObject.get(temporalZDTLike, TIME_ZONE);
            if (timeZoneProperty != Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorUnexpectedTimeZone();
            }
            JSDynamicObject calendar = zonedDateTime.getCalendar();
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listDHMMMMMNSY);
            Boundaries.listAdd(fieldNames, OFFSET);
            JSDynamicObject partialZonedDateTime = TemporalUtil.preparePartialTemporalFields(getContext(), temporalZDTLike, fieldNames);
            JSDynamicObject options = getOptionsObject(optionsParam);
            Disambiguation disambiguation = TemporalUtil.toTemporalDisambiguation(options, getOptionNode(), equalNode);
            OffsetOption offset = TemporalUtil.toTemporalOffset(options, PREFER, getOptionNode(), equalNode);
            JSDynamicObject timeZone = zonedDateTime.getTimeZone();
            Boundaries.listAdd(fieldNames, TIME_ZONE);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), zonedDateTime, fieldNames, TemporalUtil.listTimeZoneOffset);
            fields = TemporalUtil.calendarMergeFields(getContext(), namesNode, errorBranch, calendar, fields, partialZonedDateTime);
            fields = TemporalUtil.prepareTemporalFields(getContext(), fields, fieldNames, TemporalUtil.listTimeZone);
            Object offsetString = JSObject.get(fields, OFFSET);
            JSTemporalDateTimeRecord dateTimeResult = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, options, getOptionNode, dateFromFieldsNode);
            long offsetNanoseconds = TemporalUtil.parseTimeZoneOffsetString((TruffleString) offsetString);
            JSRealm realm = getRealm();
            BigInt epochNanoseconds = TemporalUtil.interpretISODateTimeOffset(getContext(), realm, dateTimeResult.getYear(), dateTimeResult.getMonth(), dateTimeResult.getDay(),
                            dateTimeResult.getHour(), dateTimeResult.getMinute(), dateTimeResult.getSecond(),
                            dateTimeResult.getMillisecond(), dateTimeResult.getMicrosecond(), dateTimeResult.getNanosecond(), OffsetBehaviour.OPTION,
                            offsetNanoseconds, timeZone, disambiguation, offset, MatchBehaviour.MATCH_EXACTLY);
            return JSTemporalZonedDateTime.create(getContext(), realm, epochNanoseconds, timeZone, calendar);
        }
    }

    public abstract static class JSTemporalZonedDateTimeWithPlainTime extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeWithPlainTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject withPlainTime(Object thisObj, Object plainTimeLike,
                        @Cached("create(getContext())") ToTemporalTimeNode toTemporalTime) {
            JSTemporalZonedDateTimeObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            JSTemporalPlainTimeObject plainTime = null;
            if (plainTimeLike == Undefined.instance) {
                plainTime = JSTemporalPlainTime.create(getContext(), 0, 0, 0, 0, 0, 0, errorBranch);
            } else {
                plainTime = (JSTemporalPlainTimeObject) toTemporalTime.executeDynamicObject(plainTimeLike, null);
            }
            JSDynamicObject timeZone = zonedDateTime.getTimeZone();
            JSRealm realm = getRealm();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), realm, zonedDateTime.getNanoseconds());
            JSDynamicObject calendar = zonedDateTime.getCalendar();
            JSTemporalPlainDateTimeObject plainDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, calendar);
            JSTemporalPlainDateTimeObject resultPlainDateTime = JSTemporalPlainDateTime.create(getContext(), plainDateTime.getYear(), plainDateTime.getMonth(), plainDateTime.getDay(),
                            plainTime.getHour(), plainTime.getMinute(), plainTime.getSecond(), plainTime.getMillisecond(), plainTime.getMicrosecond(), plainTime.getNanosecond(), calendar,
                            errorBranch);
            instant = TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, resultPlainDateTime, Disambiguation.COMPATIBLE);
            return JSTemporalZonedDateTime.create(getContext(), realm, instant.getNanoseconds(), timeZone, calendar);
        }
    }

    public abstract static class JSTemporalZonedDateTimeWithPlainDate extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeWithPlainDate(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject withPlainDate(Object thisObj, Object plainDateLike,
                        @Cached("create(getContext())") ToTemporalDateNode toTemporalDate,
                        @Cached JSToStringNode toStringNode) {
            JSTemporalZonedDateTimeObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            JSTemporalPlainDateObject plainDate = toTemporalDate.executeDynamicObject(plainDateLike, Undefined.instance);
            JSDynamicObject timeZone = zonedDateTime.getTimeZone();
            JSRealm realm = getRealm();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), realm, zonedDateTime.getNanoseconds());
            JSTemporalPlainDateTimeObject plainDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, zonedDateTime.getCalendar());
            JSDynamicObject calendar = TemporalUtil.consolidateCalendars(zonedDateTime.getCalendar(), plainDate.getCalendar(), toStringNode);
            JSTemporalPlainDateTimeObject resultPlainDateTime = JSTemporalPlainDateTime.create(getContext(), plainDate.getYear(), plainDate.getMonth(), plainDate.getDay(),
                            plainDateTime.getHour(), plainDateTime.getMinute(), plainDateTime.getSecond(), plainDateTime.getMillisecond(), plainDateTime.getMicrosecond(),
                            plainDateTime.getNanosecond(), calendar, errorBranch);
            instant = TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, resultPlainDateTime, Disambiguation.COMPATIBLE);
            return JSTemporalZonedDateTime.create(getContext(), realm, instant.getNanoseconds(), timeZone, calendar);
        }
    }

    public abstract static class JSTemporalZonedDateTimeWithTimeZone extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeWithTimeZone(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject withTimeZone(Object thisObj, Object timeZoneLike,
                        @Cached("create(getContext())") ToTemporalTimeZoneNode toTemporalTimeZone) {
            JSTemporalZonedDateTimeObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            JSDynamicObject timeZone = toTemporalTimeZone.executeDynamicObject(timeZoneLike);
            return JSTemporalZonedDateTime.create(getContext(), getRealm(), zonedDateTime.getNanoseconds(), timeZone, zonedDateTime.getCalendar());
        }
    }

    public abstract static class JSTemporalZonedDateTimeWithCalendar extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeWithCalendar(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject withCalendar(Object thisObj, Object calendarLike,
                        @Cached("create(getContext())") ToTemporalCalendarNode toTemporalCalendar) {
            JSTemporalZonedDateTimeObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            JSDynamicObject calendar = toTemporalCalendar.executeDynamicObject(calendarLike);
            return JSTemporalZonedDateTime.create(getContext(), getRealm(), zonedDateTime.getNanoseconds(), zonedDateTime.getTimeZone(), calendar);
        }
    }

    public abstract static class ZonedDateTimeOperation extends JSTemporalBuiltinOperation {
        public ZonedDateTimeOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected JSTemporalZonedDateTimeObject addDurationToOrSubtractDurationFromZonedDateTime(int sign, JSTemporalZonedDateTimeObject zonedDateTime, Object temporalDurationLike,
                        Object optionsParam, JSNumberToBigIntNode toBigInt, ToLimitedTemporalDurationNode toLimitedTemporalDurationNode) {
            JSTemporalDurationRecord duration = toLimitedTemporalDurationNode.executeDynamicObject(temporalDurationLike, TemporalUtil.listEmpty);
            JSDynamicObject options = getOptionsObject(optionsParam);
            JSDynamicObject timeZone = zonedDateTime.getTimeZone();
            JSDynamicObject calendar = zonedDateTime.getCalendar();
            BigInt epochNanoseconds = TemporalUtil.addZonedDateTime(getContext(), zonedDateTime.getNanoseconds(), timeZone, calendar, sign * dtol(duration.getYears()),
                            sign * dtol(duration.getMonths()),
                            sign * dtol(duration.getWeeks()), sign * dtol(duration.getDays()), sign * dtol(duration.getHours()), sign * dtol(duration.getMinutes()), sign * dtol(duration.getSeconds()),
                            sign * dtol(duration.getMilliseconds()),
                            sign * dtol(duration.getMicroseconds()), toBigInt.executeBigInt(sign * duration.getNanoseconds()).bigIntegerValue(), options);
            return JSTemporalZonedDateTime.create(getContext(), getRealm(), epochNanoseconds, timeZone, calendar);
        }

        protected JSTemporalDurationObject differenceTemporalZonedDateTime(int sign, JSTemporalZonedDateTimeObject zonedDateTime, Object otherParam, Object optionsParam, JSToNumberNode toNumber,
                        EnumerableOwnPropertyNamesNode namesNode, ToTemporalZonedDateTimeNode toTemporalZonedDateTime, JSToStringNode toStringNode, TruffleString.EqualNode equalNode,
                        TemporalDurationAddNode durationAddNode, TemporalRoundDurationNode roundDurationNode) {
            JSTemporalZonedDateTimeObject other = (JSTemporalZonedDateTimeObject) toTemporalZonedDateTime.executeDynamicObject(otherParam, Undefined.instance);
            if (!TemporalUtil.calendarEquals(zonedDateTime.getCalendar(), other.getCalendar(), toStringNode)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }
            JSDynamicObject options = getOptionsObject(optionsParam);
            Unit smallestUnit = toSmallestTemporalUnit(options, TemporalUtil.listEmpty, NANOSECOND, equalNode);
            Unit defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(Unit.HOUR, smallestUnit);
            Unit largestUnit = toLargestTemporalUnit(options, TemporalUtil.listEmpty, AUTO, defaultLargestUnit, equalNode);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode);
            if (sign == TemporalUtil.SINCE) {
                roundingMode = TemporalUtil.negateTemporalRoundingMode(roundingMode);
            }
            Double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, maximum, false, isObjectNode, toNumber);
            if (!(Unit.YEAR == largestUnit || Unit.MONTH == largestUnit || Unit.WEEK == largestUnit || Unit.DAY == largestUnit)) {
                long differenceNs = TemporalUtil.bitol(TemporalUtil.differenceInstant(zonedDateTime.getNanoseconds(), other.getNanoseconds(), roundingIncrement, smallestUnit, roundingMode));
                JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDuration(getContext(), namesNode, 0, 0, 0, 0, 0, 0, differenceNs, largestUnit);
                return JSTemporalDuration.createTemporalDuration(getContext(), 0, 0, 0, 0, sign * balanceResult.getHours(), sign * balanceResult.getMinutes(), sign * balanceResult.getSeconds(),
                                sign * balanceResult.getMilliseconds(), sign * balanceResult.getMicroseconds(), sign * balanceResult.getNanoseconds(), errorBranch);
            }
            if (!TemporalUtil.timeZoneEquals(zonedDateTime.getTimeZone(), other.getTimeZone(), toStringNode)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorIdenticalTimeZoneExpected();
            }
            JSDynamicObject untilOptions = TemporalUtil.mergeLargestUnitOption(getContext(), namesNode, options, largestUnit);
            JSTemporalDurationRecord difference = TemporalUtil.differenceZonedDateTime(getContext(), namesNode, zonedDateTime.getNanoseconds(), other.getNanoseconds(), zonedDateTime.getTimeZone(),
                            zonedDateTime.getCalendar(), largestUnit, untilOptions);
            JSTemporalDurationRecord roundResult = roundDurationNode.execute(difference.getYears(), difference.getMonths(), difference.getWeeks(),
                            difference.getDays(),
                            difference.getHours(), difference.getMinutes(), difference.getSeconds(), difference.getMilliseconds(), difference.getMicroseconds(), difference.getNanoseconds(),
                            (long) roundingIncrement, smallestUnit, roundingMode, zonedDateTime);
            JSTemporalDurationRecord result = TemporalUtil.adjustRoundedDurationDays(getContext(), namesNode, durationAddNode, roundResult.getYears(), roundResult.getMonths(), roundResult.getWeeks(),
                            roundResult.getDays(), roundResult.getHours(), roundResult.getMinutes(), roundResult.getSeconds(), roundResult.getMilliseconds(), roundResult.getMicroseconds(),
                            roundResult.getNanoseconds(), (long) roundingIncrement, smallestUnit, roundingMode, zonedDateTime);
            return JSTemporalDuration.createTemporalDuration(getContext(), sign * result.getYears(), sign * result.getMonths(), sign * result.getWeeks(), sign * result.getDays(),
                            sign * result.getHours(), sign * result.getMinutes(),
                            sign * result.getSeconds(), sign * result.getMilliseconds(), sign * result.getMicroseconds(), sign * result.getNanoseconds(), errorBranch);
        }
    }

    public abstract static class JSTemporalZonedDateTimeAdd extends ZonedDateTimeOperation {

        protected JSTemporalZonedDateTimeAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject add(Object thisObj, Object temporalDurationLike, Object optionsParam,
                        @Cached JSNumberToBigIntNode toBigInt,
                        @Cached("create()") ToLimitedTemporalDurationNode toLimitedTemporalDurationNode) {
            JSTemporalZonedDateTimeObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            return addDurationToOrSubtractDurationFromZonedDateTime(1, zonedDateTime, temporalDurationLike, optionsParam, toBigInt, toLimitedTemporalDurationNode);
        }
    }

    public abstract static class JSTemporalZonedDateTimeSubtract extends ZonedDateTimeOperation {

        protected JSTemporalZonedDateTimeSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject subtract(Object thisObj, Object temporalDurationLike, Object optionsParam,
                        @Cached JSNumberToBigIntNode toBigInt,
                        @Cached("create()") ToLimitedTemporalDurationNode toLimitedTemporalDurationNode) {
            JSTemporalZonedDateTimeObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            return addDurationToOrSubtractDurationFromZonedDateTime(-1, zonedDateTime, temporalDurationLike, optionsParam, toBigInt, toLimitedTemporalDurationNode);
        }
    }

    public abstract static class JSTemporalZonedDateTimeUntil extends ZonedDateTimeOperation {

        protected JSTemporalZonedDateTimeUntil(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object until(Object thisObj, Object otherParam, Object optionsParam,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached("create(getContext())") ToTemporalZonedDateTimeNode toTemporalZonedDateTime,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext())") TemporalDurationAddNode durationAddNode,
                        @Cached("create(getContext())") TemporalRoundDurationNode roundDurationNode) {
            JSTemporalZonedDateTimeObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            return differenceTemporalZonedDateTime(TemporalUtil.UNTIL, zonedDateTime, otherParam, optionsParam, toNumber, namesNode, toTemporalZonedDateTime, toStringNode, equalNode, durationAddNode,
                            roundDurationNode);
        }
    }

    public abstract static class JSTemporalZonedDateTimeSince extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeSince(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object since(Object thisObj, Object otherParam, Object optionsParam,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached("create(getContext())") ToTemporalZonedDateTimeNode toTemporalZonedDateTime,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext())") TemporalDurationAddNode durationAddNode,
                        @Cached("create(getContext())") TemporalRoundDurationNode roundDurationNode) {
            JSTemporalZonedDateTimeObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            JSTemporalZonedDateTimeObject other = (JSTemporalZonedDateTimeObject) toTemporalZonedDateTime.executeDynamicObject(otherParam, Undefined.instance);
            if (!TemporalUtil.calendarEquals(zonedDateTime.getCalendar(), other.getCalendar(), toStringNode)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }
            JSDynamicObject options = getOptionsObject(optionsParam);
            Unit smallestUnit = toSmallestTemporalUnit(options, TemporalUtil.listEmpty, NANOSECOND, equalNode);
            Unit defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(Unit.HOUR, smallestUnit);
            Unit largestUnit = toLargestTemporalUnit(options, TemporalUtil.listEmpty, AUTO, defaultLargestUnit, equalNode);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode);
            roundingMode = TemporalUtil.negateTemporalRoundingMode(roundingMode);
            Double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, maximum, false, isObjectNode, toNumber);
            if (!(Unit.YEAR == largestUnit || Unit.MONTH == largestUnit || Unit.WEEK == largestUnit || Unit.DAY == largestUnit)) {
                BigInteger differenceNs = TemporalUtil.differenceInstant(zonedDateTime.getNanoseconds(), other.getNanoseconds(), roundingIncrement, smallestUnit, roundingMode);
                JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDuration(getContext(), namesNode, 0, 0, 0, 0, 0, 0, differenceNs, largestUnit, Undefined.instance);
                return JSTemporalDuration.createTemporalDuration(getContext(), 0, 0, 0, 0, -balanceResult.getHours(), -balanceResult.getMinutes(), -balanceResult.getSeconds(),
                                -balanceResult.getMilliseconds(), -balanceResult.getMicroseconds(), -balanceResult.getNanoseconds(), errorBranch);
            }
            if (!TemporalUtil.timeZoneEquals(zonedDateTime.getTimeZone(), other.getTimeZone(), toStringNode)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorIdenticalTimeZoneExpected();
            }
            JSDynamicObject untilOptions = TemporalUtil.mergeLargestUnitOption(getContext(), namesNode, options, largestUnit);
            JSTemporalDurationRecord difference = TemporalUtil.differenceZonedDateTime(getContext(), namesNode, zonedDateTime.getNanoseconds(), other.getNanoseconds(), zonedDateTime.getTimeZone(),
                            zonedDateTime.getCalendar(), largestUnit, untilOptions);
            JSTemporalDurationRecord roundResult = roundDurationNode.execute(difference.getYears(), difference.getMonths(), difference.getWeeks(), difference.getDays(),
                            difference.getHours(), difference.getMinutes(), difference.getSeconds(), difference.getMilliseconds(), difference.getMicroseconds(), difference.getNanoseconds(),
                            (long) roundingIncrement, smallestUnit, roundingMode, zonedDateTime);
            JSTemporalDurationRecord result = TemporalUtil.adjustRoundedDurationDays(getContext(), namesNode, durationAddNode, roundResult.getYears(), roundResult.getMonths(), roundResult.getWeeks(),
                            roundResult.getDays(), roundResult.getHours(), roundResult.getMinutes(), roundResult.getSeconds(), roundResult.getMilliseconds(), roundResult.getMicroseconds(),
                            roundResult.getNanoseconds(), (long) roundingIncrement, smallestUnit, roundingMode, zonedDateTime);
            return JSTemporalDuration.createTemporalDuration(getContext(), -result.getYears(), -result.getMonths(), -result.getWeeks(), -result.getDays(), -result.getHours(), -result.getMinutes(),
                            -result.getSeconds(), -result.getMilliseconds(), -result.getMicroseconds(), -result.getNanoseconds(), errorBranch);
        }
    }

    public abstract static class JSTemporalZonedDateTimeRound extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject round(Object thisObj, Object roundToParam,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached TruffleString.EqualNode equalNode) {
            JSTemporalZonedDateTimeObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            if (roundToParam == Undefined.instance) {
                errorBranch.enter();
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
                throw TemporalErrors.createRangeErrorSmallestUnitExpected();
            }
            RoundingMode roundingMode = toTemporalRoundingMode(roundTo, HALF_EXPAND, equalNode);
            double roundingIncrement = TemporalUtil.toTemporalDateTimeRoundingIncrement(roundTo, smallestUnit, isObjectNode, toNumber);
            JSDynamicObject timeZone = zonedDateTime.getTimeZone();
            JSRealm realm = getRealm();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), realm, zonedDateTime.getNanoseconds());
            JSDynamicObject calendar = zonedDateTime.getCalendar();
            JSTemporalPlainDateTimeObject tdt = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, calendar);
            JSDynamicObject isoCalendar = TemporalUtil.getISO8601Calendar(getContext(), realm, errorBranch);
            JSTemporalPlainDateTimeObject dtStart = JSTemporalPlainDateTime.create(getContext(), tdt.getYear(), tdt.getMonth(), tdt.getDay(), 0, 0, 0, 0, 0, 0, isoCalendar, errorBranch);
            JSTemporalInstantObject instantStart = TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, dtStart, Disambiguation.COMPATIBLE);
            BigInt startNs = instantStart.getNanoseconds();
            BigInt endNs = TemporalUtil.addZonedDateTime(getContext(), startNs, timeZone, zonedDateTime.getCalendar(), 0, 0, 0, 1, 0, 0, 0, 0, 0, 0);
            BigInt dayLengthNs = endNs.subtract(startNs);
            if (dayLengthNs.compareValueTo(0) == 0) {
                errorBranch.enter();
                throw Errors.createRangeError("day length of zero now allowed");
            }
            JSTemporalDurationRecord roundResult = TemporalUtil.roundISODateTime(tdt.getYear(), tdt.getMonth(), tdt.getDay(), tdt.getHour(), tdt.getMinute(), tdt.getSecond(), tdt.getMillisecond(),
                            tdt.getMicrosecond(), tdt.getNanosecond(), roundingIncrement, smallestUnit, roundingMode, TemporalUtil.bigIntToLong(dayLengthNs));
            long offsetNanoseconds = TemporalUtil.getOffsetNanosecondsFor(timeZone, instant);
            BigInt epochNanoseconds = TemporalUtil.interpretISODateTimeOffset(getContext(), realm, dtoi(roundResult.getYears()), dtoi(roundResult.getMonths()), dtoi(roundResult.getDays()),
                            dtoi(roundResult.getHours()), dtoi(roundResult.getMinutes()), dtoi(roundResult.getSeconds()), dtoi(roundResult.getMilliseconds()), dtoi(roundResult.getMicroseconds()),
                            dtoi(roundResult.getNanoseconds()), OffsetBehaviour.OPTION, offsetNanoseconds, timeZone, Disambiguation.COMPATIBLE, OffsetOption.PREFER, MatchBehaviour.MATCH_EXACTLY);
            return JSTemporalZonedDateTime.create(getContext(), realm, epochNanoseconds, timeZone, calendar);
        }
    }

    public abstract static class JSTemporalZonedDateTimeEquals extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object equals(Object thisObj, Object otherParam,
                        @Cached("create(getContext())") ToTemporalZonedDateTimeNode toTemporalZonedDateTime,
                        @Cached JSToStringNode toStringNode) {
            JSTemporalZonedDateTimeObject zdt = requireTemporalZonedDateTime(thisObj);
            JSTemporalZonedDateTimeObject other = (JSTemporalZonedDateTimeObject) toTemporalZonedDateTime.executeDynamicObject(otherParam, Undefined.instance);
            if (!zdt.getNanoseconds().equals(other.getNanoseconds())) {
                return false;
            }
            if (!TemporalUtil.timeZoneEquals(zdt.getTimeZone(), other.getTimeZone(), toStringNode)) {
                return false;
            }
            return TemporalUtil.calendarEquals(zdt.getCalendar(), other.getCalendar(), toStringNode);
        }
    }

    public abstract static class JSTemporalZonedDateTimeStartOfDay extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeStartOfDay(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object startOfDay(Object thisObj) {
            JSTemporalZonedDateTimeObject zdt = requireTemporalZonedDateTime(thisObj);
            JSDynamicObject timeZone = zdt.getTimeZone();
            JSDynamicObject calendar = zdt.getCalendar();
            JSRealm realm = getRealm();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), realm, zdt.getNanoseconds());
            JSTemporalPlainDateTimeObject dt = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, calendar);
            JSTemporalPlainDateTimeObject startDateTime = JSTemporalPlainDateTime.create(getContext(), dt.getYear(), dt.getMonth(), dt.getDay(), 0, 0, 0, 0, 0, 0, calendar, errorBranch);
            JSTemporalInstantObject startInstant = TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, startDateTime, Disambiguation.COMPATIBLE);
            return JSTemporalZonedDateTime.create(getContext(), realm, startInstant.getNanoseconds(), timeZone, calendar);
        }
    }

    public abstract static class JSTemporalZonedDateTimeToInstant extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToInstant(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object toInstant(Object thisObj) {
            JSTemporalZonedDateTimeObject zonedDateTime = requireTemporalZonedDateTime(thisObj);
            return JSTemporalInstant.create(getContext(), getRealm(), zonedDateTime.getNanoseconds());
        }
    }

    public abstract static class JSTemporalZonedDateTimeToPlainDate extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToPlainDate(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object toPlainDate(Object thisObj) {
            JSTemporalZonedDateTimeObject zdt = requireTemporalZonedDateTime(thisObj);
            JSDynamicObject timeZone = zdt.getTimeZone();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), getRealm(), zdt.getNanoseconds());
            JSDynamicObject calendar = zdt.getCalendar();
            JSTemporalPlainDateTimeObject dt = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, calendar);
            return JSTemporalPlainDate.create(getContext(), dt.getYear(), dt.getMonth(), dt.getDay(), calendar, errorBranch);
        }
    }

    public abstract static class JSTemporalZonedDateTimeToPlainTime extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToPlainTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object toPlainTime(Object thisObj) {
            JSTemporalZonedDateTimeObject zdt = requireTemporalZonedDateTime(thisObj);
            JSDynamicObject timeZone = zdt.getTimeZone();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), getRealm(), zdt.getNanoseconds());
            JSDynamicObject calendar = zdt.getCalendar();
            JSTemporalPlainDateTimeObject dt = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, calendar);
            return JSTemporalPlainTime.create(getContext(), dt.getHour(), dt.getMinute(), dt.getSecond(), dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(), errorBranch);
        }
    }

    public abstract static class JSTemporalZonedDateTimeToPlainDateTime extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToPlainDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object toPlainDateTime(Object thisObj) {
            JSTemporalZonedDateTimeObject zdt = requireTemporalZonedDateTime(thisObj);
            JSDynamicObject timeZone = zdt.getTimeZone();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), getRealm(), zdt.getNanoseconds());
            JSDynamicObject calendar = zdt.getCalendar();
            return TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, calendar);
        }
    }

    public abstract static class JSTemporalZonedDateTimeToPlainYearMonth extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToPlainYearMonth(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object toPlainYearMonth(Object thisObj,
                        @Cached("create(getContext())") TemporalYearMonthFromFieldsNode yearMonthFromFieldsNode,
                        @Cached("create(getContext())") TemporalCalendarFieldsNode calendarFieldsNode) {
            JSTemporalZonedDateTimeObject zdt = requireTemporalZonedDateTime(thisObj);
            JSDynamicObject timeZone = zdt.getTimeZone();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), getRealm(), zdt.getNanoseconds());
            JSDynamicObject calendar = zdt.getCalendar();
            JSTemporalPlainDateTimeObject temporalDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, calendar);
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listMCY);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), temporalDateTime, fieldNames, TemporalUtil.listEmpty);
            return yearMonthFromFieldsNode.execute(calendar, fields, Undefined.instance);
        }
    }

    public abstract static class JSTemporalZonedDateTimeToPlainMonthDay extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToPlainMonthDay(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object toPlainMonthDay(Object thisObj,
                        @Cached("create(getContext())") TemporalMonthDayFromFieldsNode monthDayFromFieldsNode,
                        @Cached("create(getContext())") TemporalCalendarFieldsNode calendarFieldsNode) {
            JSTemporalZonedDateTimeObject zdt = requireTemporalZonedDateTime(thisObj);
            JSDynamicObject timeZone = zdt.getTimeZone();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), getRealm(), zdt.getNanoseconds());
            JSDynamicObject calendar = zdt.getCalendar();
            JSTemporalPlainDateTimeObject temporalDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, calendar);
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listDMC);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), temporalDateTime, fieldNames, TemporalUtil.listEmpty);
            return monthDayFromFieldsNode.execute(calendar, fields, Undefined.instance);
        }
    }

    public abstract static class JSTemporalZonedDateTimeGetISOFields extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object getISOFields(Object thisObj) {
            JSTemporalZonedDateTimeObject zdt = requireTemporalZonedDateTime(thisObj);
            JSRealm realm = getRealm();
            JSObject obj = JSOrdinary.create(getContext(), realm);
            JSDynamicObject timeZone = zdt.getTimeZone();
            JSDynamicObject instant = JSTemporalInstant.create(getContext(), realm, zdt.getNanoseconds());
            JSDynamicObject calendar = zdt.getCalendar();
            JSTemporalPlainDateTimeObject dt = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, calendar);
            TruffleString offset = TemporalUtil.builtinTimeZoneGetOffsetStringFor(timeZone, instant);

            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, CALENDAR, calendar);
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_DAY, dt.getDay());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_HOUR, dt.getHour());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_MICROSECOND, dt.getMicrosecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_MILLISECOND, dt.getMillisecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_MINUTE, dt.getMinute());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_MONTH, dt.getMonth());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_NANOSECOND, dt.getNanosecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_SECOND, dt.getSecond());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_YEAR, dt.getYear());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.OFFSET, offset);
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.TIME_ZONE, timeZone);
            return obj;
        }
    }

}
