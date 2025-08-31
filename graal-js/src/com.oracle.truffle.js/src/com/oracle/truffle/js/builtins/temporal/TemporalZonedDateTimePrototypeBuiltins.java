/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.OFFSET;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.PREFER;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtoi;

import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeAddSubNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeCalendarGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeGetTimeZoneTransitionNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeRoundNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeStartOfDayNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeTimeZoneIdGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToInstantNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToJSONNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToPlainDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToPlainTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeUntilSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeWithCalendarNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeWithNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeWithPlainTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeWithTimeZoneNodeGen;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.GetOptionsObjectNode;
import com.oracle.truffle.js.nodes.intl.GetStringOptionNode;
import com.oracle.truffle.js.nodes.intl.InitializeDateTimeFormatNode;
import com.oracle.truffle.js.nodes.temporal.DifferenceZonedDateTimeWithRoundingNode;
import com.oracle.truffle.js.nodes.temporal.GetDifferenceSettingsNode;
import com.oracle.truffle.js.nodes.temporal.GetRoundingIncrementOptionNode;
import com.oracle.truffle.js.nodes.temporal.GetTemporalUnitNode;
import com.oracle.truffle.js.nodes.temporal.IsPartialTemporalObjectNode;
import com.oracle.truffle.js.nodes.temporal.TemporalAddZonedDateTimeNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarDateFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.ToFractionalSecondDigitsNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarIdentifierNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneIdentifierNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalZonedDateTimeNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormatObject;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeDurationRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
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
import org.graalvm.shadowed.com.ibm.icu.util.Calendar;

public class TemporalZonedDateTimePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalZonedDateTimePrototypeBuiltins.TemporalZonedDateTimePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalZonedDateTimePrototypeBuiltins();

    protected TemporalZonedDateTimePrototypeBuiltins() {
        super(JSTemporalZonedDateTime.PROTOTYPE_NAME, TemporalZonedDateTimePrototype.class);
    }

    public enum TemporalZonedDateTimePrototype implements BuiltinEnum<TemporalZonedDateTimePrototype> {
        // getters
        calendarId(0),
        timeZoneId(0),
        era(0),
        eraYear(0),
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
        epochMilliseconds(0),
        epochNanoseconds(0),
        dayOfWeek(0),
        dayOfYear(0),
        weekOfYear(0),
        yearOfWeek(0),
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
        getTimeZoneTransition(1),
        toInstant(0),
        toPlainDate(0),
        toPlainTime(0),
        toPlainDateTime(0);

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
            return EnumSet.range(calendarId, offset).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalZonedDateTimePrototype builtinEnum) {
        switch (builtinEnum) {
            case calendarId:
                return JSTemporalZonedDateTimeCalendarGetterNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case timeZoneId:
                return JSTemporalZonedDateTimeTimeZoneIdGetterNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case era:
            case eraYear:
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
            case epochMilliseconds:
            case epochNanoseconds:
            case dayOfWeek:
            case dayOfYear:
            case weekOfYear:
            case yearOfWeek:
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
                return context.isOptionIntl402()
                                ? JSTemporalZonedDateTimeToLocaleStringNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context))
                                : JSTemporalZonedDateTimeToJSONNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toJSON:
                return JSTemporalZonedDateTimeToJSONNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return UnsupportedValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case with:
                return JSTemporalZonedDateTimeWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case withPlainTime:
                return JSTemporalZonedDateTimeWithPlainTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case withTimeZone:
                return JSTemporalZonedDateTimeWithTimeZoneNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case withCalendar:
                return JSTemporalZonedDateTimeWithCalendarNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case add:
                return JSTemporalZonedDateTimeAddSubNodeGen.create(context, builtin, TemporalUtil.ADD, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case subtract:
                return JSTemporalZonedDateTimeAddSubNodeGen.create(context, builtin, TemporalUtil.SUBTRACT, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case until:
                return JSTemporalZonedDateTimeUntilSinceNodeGen.create(context, builtin, TemporalUtil.UNTIL, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalZonedDateTimeUntilSinceNodeGen.create(context, builtin, TemporalUtil.SINCE, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case round:
                return JSTemporalZonedDateTimeRoundNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case equals:
                return JSTemporalZonedDateTimeEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case startOfDay:
                return JSTemporalZonedDateTimeStartOfDayNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case getTimeZoneTransition:
                return JSTemporalZonedDateTimeGetTimeZoneTransitionNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toInstant:
                return JSTemporalZonedDateTimeToInstantNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toPlainDate:
                return JSTemporalZonedDateTimeToPlainDateNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toPlainTime:
                return JSTemporalZonedDateTimeToPlainTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toPlainDateTime:
                return JSTemporalZonedDateTimeToPlainDateTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalZonedDateTimeCalendarGetterNode extends JSBuiltinNode {

        protected JSTemporalZonedDateTimeCalendarGetterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString calendarId(JSTemporalZonedDateTimeObject zonedDateTime,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
            return toCalendarIdentifier.executeString(zonedDateTime.getCalendar());
        }

        @Specialization(guards = "!isJSTemporalZonedDateTime(zonedDateTime)")
        static TruffleString invalidReceiver(@SuppressWarnings("unused") Object zonedDateTime) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeTimeZoneIdGetterNode extends JSBuiltinNode {

        protected JSTemporalZonedDateTimeTimeZoneIdGetterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString timeZoneId(JSTemporalZonedDateTimeObject zonedDateTime,
                        @Cached ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifier) {
            return toTimeZoneIdentifier.execute(zonedDateTime.getTimeZone());
        }

        @Specialization(guards = "!isJSTemporalZonedDateTime(zonedDateTime)")
        static TruffleString invalidReceiver(@SuppressWarnings("unused") Object zonedDateTime) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeGetterNode extends JSBuiltinNode {

        protected final TemporalZonedDateTimePrototype property;

        protected JSTemporalZonedDateTimeGetterNode(JSContext context, JSBuiltin builtin, TemporalZonedDateTimePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization
        protected Object zonedDateTimeGetter(JSTemporalZonedDateTimeObject zdt,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached InlinedConditionProfile isoCalendarProfile) {
            switch (property) {
                case era:
                case eraYear:
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
                case yearOfWeek:
                case daysInWeek:
                case daysInMonth:
                case daysInYear:
                case monthsInYear:
                case inLeapYear:
                    return getterCalendarDetails(zdt, equalNode, isoCalendarProfile);

                case hoursInDay:
                    return getterHoursInDay(zdt);
                case epochMilliseconds:
                    return TemporalUtil.nanosToMillis(zdt.getNanoseconds());
                case epochNanoseconds:
                    return zdt.getNanoseconds();
                case offsetNanoseconds:
                    return getterOffsetNanoseconds(zdt);
                case offset:
                    return getterOffset(zdt);
            }
            throw Errors.shouldNotReachHere();
        }

        private Object getterOffset(JSTemporalZonedDateTimeObject zdt) {
            JSContext context = getContext();
            JSRealm realm = getRealm();
            JSTemporalInstantObject instant = JSTemporalInstant.create(context, realm, zdt.getNanoseconds());
            TruffleString timeZone = zdt.getTimeZone();
            return TemporalUtil.builtinTimeZoneGetOffsetStringFor(context, timeZone, instant);
        }

        private double getterOffsetNanoseconds(JSTemporalZonedDateTimeObject zdt) {
            return TemporalUtil.getOffsetNanosecondsFor(getContext(), zdt.getTimeZone(), zdt.getNanoseconds());
        }

        private Object getterHoursInDay(JSTemporalZonedDateTimeObject zdt) {
            JSContext context = getContext();
            TruffleString timeZone = zdt.getTimeZone();
            JSTemporalDateTimeRecord today = TemporalUtil.getISODateTimeFor(context, timeZone, zdt.getNanoseconds());
            ISODateRecord tomorrow = TemporalUtil.balanceISODate(today.getYear(), today.getMonth(), today.getDay() + 1);
            BigInt todayNs = TemporalUtil.getStartOfDay(context, timeZone, today.getYear(), today.getMonth(), today.getDay());
            BigInt tomorrowNs = TemporalUtil.getStartOfDay(context, timeZone, tomorrow.year(), tomorrow.month(), tomorrow.day());
            BigInt diffNs = tomorrowNs.subtract(todayNs);
            return diffNs.doubleValue() / TemporalUtil.BI_NS_PER_HOUR.doubleValue();
        }

        private Object getterCalendarDetails(JSTemporalZonedDateTimeObject zdt,
                        TruffleString.EqualNode equalNode,
                        InlinedConditionProfile isoCalendarProfile) {
            TruffleString timeZone = zdt.getTimeZone();
            JSRealm realm = getRealm();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), realm, zdt.getNanoseconds());
            TruffleString calendar = zdt.getCalendar();
            JSTemporalPlainDateTimeObject tdt = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), realm, timeZone, instant, calendar);
            boolean isoCalendar = isoCalendarProfile.profile(this, Strings.equals(equalNode, TemporalConstants.ISO8601, calendar));
            Calendar cal = null;
            if (!isoCalendar) {
                cal = IntlUtil.getCalendar(calendar, tdt.getYear(), tdt.getMonth(), tdt.getDay());
            }
            switch (property) {
                case era:
                    return isoCalendar ? Undefined.instance : IntlUtil.getEra(cal);
                case eraYear:
                    return isoCalendar ? Undefined.instance : IntlUtil.getEraYear(cal);
                case year:
                    return isoCalendar ? tdt.getYear() : IntlUtil.getCalendarField(cal, Calendar.EXTENDED_YEAR);
                case month:
                    return isoCalendar ? tdt.getMonth() : (IntlUtil.getCalendarField(cal, Calendar.ORDINAL_MONTH) + 1);
                case monthCode:
                    return isoCalendar ? TemporalUtil.buildISOMonthCode(tdt.getMonth()) : Strings.fromJavaString(IntlUtil.getTemporalMonthCode(cal));
                case day:
                    return isoCalendar ? tdt.getDay() : IntlUtil.getCalendarField(cal, Calendar.DAY_OF_MONTH);
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
                    return isoCalendar ? TemporalUtil.toISODayOfWeek(tdt.getYear(), tdt.getMonth(), tdt.getDay()) : IntlUtil.getDayOfWeek(cal);
                case dayOfYear:
                    return isoCalendar ? TemporalUtil.toISODayOfYear(tdt.getYear(), tdt.getMonth(), tdt.getDay()) : IntlUtil.getCalendarField(cal, Calendar.DAY_OF_YEAR);
                case weekOfYear:
                    return isoCalendar ? TemporalUtil.weekOfToISOWeekOfYear(tdt.getYear(), tdt.getMonth(), tdt.getDay()) : Undefined.instance;
                case yearOfWeek:
                    return isoCalendar ? TemporalUtil.yearOfToISOWeekOfYear(tdt.getYear(), tdt.getMonth(), tdt.getDay()) : Undefined.instance;
                case daysInWeek:
                    return isoCalendar ? 7 : IntlUtil.getCalendarFieldMax(cal, Calendar.DAY_OF_WEEK);
                case daysInMonth:
                    return isoCalendar ? TemporalUtil.isoDaysInMonth(tdt.getYear(), tdt.getMonth()) : IntlUtil.getCalendarFieldMax(cal, Calendar.DAY_OF_MONTH);
                case daysInYear:
                    return isoCalendar ? TemporalUtil.isoDaysInYear(tdt.getYear()) : IntlUtil.getCalendarFieldMax(cal, Calendar.DAY_OF_YEAR);
                case monthsInYear:
                    return isoCalendar ? 12 : (IntlUtil.getCalendarFieldMax(cal, Calendar.ORDINAL_MONTH) + 1);
                case inLeapYear:
                    return isoCalendar ? JSDate.isLeapYear(tdt.getYear()) : IntlUtil.isLeapYear(cal);
            }
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeToString extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(JSTemporalZonedDateTimeObject zonedDateTime, Object optionsParam,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached ToFractionalSecondDigitsNode toFractionalSecondDigitsNode,
                        @Cached GetTemporalUnitNode getSmallestUnit,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSDynamicObject options = getOptionsObject(optionsParam, this, errorBranch, optionUndefined);
            ShowCalendar showCalendar = TemporalUtil.toShowCalendarOption(options, getOptionNode, equalNode);
            int digits = toFractionalSecondDigitsNode.execute(options);
            TruffleString showOffset = TemporalUtil.toShowOffsetOption(options, getOptionNode);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TemporalConstants.TRUNC, equalNode, getOptionNode);

            Unit smallestUnit = getSmallestUnit.execute(options, TemporalConstants.SMALLEST_UNIT, TemporalUtil.unitMappingTime, Unit.EMPTY);
            if (smallestUnit == Unit.HOUR) {
                errorBranch.enter(this);
                throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
            }

            TruffleString showTimeZone = TemporalUtil.toShowTimeZoneNameOption(options, getOptionNode);
            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecisionRecord(smallestUnit, digits);

            return TemporalUtil.temporalZonedDateTimeToString(getContext(), getRealm(), zonedDateTime, precision.getPrecision(), showCalendar, showTimeZone, showOffset, precision.getIncrement(),
                            precision.getUnit(), roundingMode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(Object thisObj, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeToJSON extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToJSON(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toJSON(JSTemporalZonedDateTimeObject zonedDateTime) {
            return TemporalUtil.temporalZonedDateTimeToString(getContext(), getRealm(), zonedDateTime, AUTO, ShowCalendar.AUTO, AUTO, AUTO);
        }

        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public TruffleString toLocaleString(JSTemporalZonedDateTimeObject zonedDateTime, Object locales, Object options,
                        @Cached("createAnyAll(getContext())") InitializeDateTimeFormatNode initDateTimeFormatNode) {
            JSContext context = getContext();
            JSRealm realm = getRealm();
            JSDateTimeFormatObject dateTimeFormat = JSDateTimeFormat.create(context, realm);
            initDateTimeFormatNode.executeInit(dateTimeFormat, locales, options, zonedDateTime.getTimeZone());
            TruffleString calendar = zonedDateTime.getCalendar();
            if (!TemporalConstants.ISO8601.equals(calendar) && !calendar.equals(dateTimeFormat.getInternalState().getCalendar())) {
                throw Errors.createRangeError("Calendar mismatch");
            }
            return JSDateTimeFormat.format(dateTimeFormat, JSTemporalInstant.create(context, realm, zonedDateTime.getNanoseconds()));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(Object thisObj, Object locales, Object options) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    @ImportStatic(TemporalConstants.class)
    public abstract static class JSTemporalZonedDateTimeWith extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected Object with(JSTemporalZonedDateTimeObject zonedDateTime, Object temporalZonedDateTimeLike, Object options,
                        @Bind Node node,
                        @Cached IsPartialTemporalObjectNode isPartialTemporalObjectNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext(), HOUR)") CreateDataPropertyNode createHourDataPropertyNode,
                        @Cached("create(getContext(), MINUTE)") CreateDataPropertyNode createMinuteDataPropertyNode,
                        @Cached("create(getContext(), SECOND)") CreateDataPropertyNode createSecondDataPropertyNode,
                        @Cached("create(getContext(), MILLISECOND)") CreateDataPropertyNode createMillisecondDataPropertyNode,
                        @Cached("create(getContext(), MICROSECOND)") CreateDataPropertyNode createMicrosecondDataPropertyNode,
                        @Cached("create(getContext(), NANOSECOND)") CreateDataPropertyNode createNanosecondDataPropertyNode,
                        @Cached("create(getContext(), OFFSET)") CreateDataPropertyNode createOffsetDataPropertyNode,
                        @Cached TemporalCalendarDateFromFieldsNode dateFromFieldsNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            if (!isPartialTemporalObjectNode.execute(temporalZonedDateTimeLike)) {
                errorBranch.enter(node);
                throw TemporalErrors.createTypeErrorPartialTemporalObjectExpected();
            }

            JSContext ctx = getContext();
            JSRealm realm = getRealm();

            BigInt epochNs = zonedDateTime.getNanoseconds();
            TruffleString timeZone = zonedDateTime.getTimeZone();
            TruffleString calendar = zonedDateTime.getCalendar();
            long offsetNanoseconds = TemporalUtil.getOffsetNanosecondsFor(ctx, timeZone, epochNs);

            JSTemporalInstantObject instant = JSTemporalInstant.create(ctx, realm, zonedDateTime.getNanoseconds());
            JSTemporalPlainDateTimeObject isoDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, realm, instant, calendar, offsetNanoseconds);

            JSDynamicObject fields = TemporalUtil.isoDateToFields(ctx, calendar, isoDateTime.isoDate(), TemporalUtil.FieldsType.DATE);
            createHourDataPropertyNode.executeVoid(fields, isoDateTime.getHour());
            createMinuteDataPropertyNode.executeVoid(fields, isoDateTime.getMinute());
            createSecondDataPropertyNode.executeVoid(fields, isoDateTime.getSecond());
            createMillisecondDataPropertyNode.executeVoid(fields, isoDateTime.getMillisecond());
            createMicrosecondDataPropertyNode.executeVoid(fields, isoDateTime.getMicrosecond());
            createNanosecondDataPropertyNode.executeVoid(fields, isoDateTime.getNanosecond());
            createOffsetDataPropertyNode.executeVoid(fields, TemporalUtil.formatISOTimeZoneOffsetString(offsetNanoseconds));

            JSDynamicObject partialZonedDateTime = TemporalUtil.prepareCalendarFields(ctx, calendar, temporalZonedDateTimeLike, TemporalUtil.listDMMCY, TemporalUtil.listTimeUnitsOffset, null);
            fields = TemporalUtil.calendarMergeFields(ctx, calendar, fields, partialZonedDateTime);

            JSDynamicObject resolvedOptions = getOptionsObject(options, node, errorBranch, optionUndefined);
            Disambiguation disambiguation = TemporalUtil.toTemporalDisambiguation(resolvedOptions, getOptionNode, equalNode);
            OffsetOption offset = TemporalUtil.toTemporalOffset(resolvedOptions, PREFER, getOptionNode, equalNode);
            Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            JSTemporalDateTimeRecord dateTimeResult = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, overflow, dateFromFieldsNode);
            Object offsetString = JSObject.get(fields, OFFSET);
            long newOffsetNanoseconds = TemporalUtil.parseTimeZoneOffsetString((TruffleString) offsetString);

            BigInt epochNanoseconds = TemporalUtil.interpretISODateTimeOffset(ctx, realm, dateTimeResult.getYear(), dateTimeResult.getMonth(), dateTimeResult.getDay(),
                            dateTimeResult.getHour(), dateTimeResult.getMinute(), dateTimeResult.getSecond(),
                            dateTimeResult.getMillisecond(), dateTimeResult.getMicrosecond(), dateTimeResult.getNanosecond(), OffsetBehaviour.OPTION,
                            newOffsetNanoseconds, timeZone, disambiguation, offset, MatchBehaviour.MATCH_EXACTLY);
            return JSTemporalZonedDateTime.create(ctx, realm, epochNanoseconds, timeZone, calendar);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(Object thisObj, Object temporalZonedDateTimeLike, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }

    }

    public abstract static class JSTemporalZonedDateTimeWithPlainTime extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeWithPlainTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject withPlainTime(JSTemporalZonedDateTimeObject zonedDateTime, Object plainTimeLike,
                        @Cached ToTemporalTimeNode toTemporalTime,
                        @Cached InlinedBranchProfile errorBranch) {
            JSRealm realm = getRealm();
            TruffleString timeZone = zonedDateTime.getTimeZone();
            TruffleString calendar = zonedDateTime.getCalendar();
            JSTemporalDateTimeRecord isoDateTime = TemporalUtil.getISODateTimeFor(getContext(), timeZone, zonedDateTime.getNanoseconds());
            BigInt epochNs;
            if (plainTimeLike == Undefined.instance) {
                epochNs = TemporalUtil.getStartOfDay(getContext(), timeZone, isoDateTime.getYear(), isoDateTime.getMonth(), isoDateTime.getDay());
            } else {
                JSTemporalPlainTimeObject plainTime = toTemporalTime.execute(plainTimeLike, Undefined.instance);
                JSTemporalPlainDateTimeObject resultISODateTime = JSTemporalPlainDateTime.create(getContext(), realm, isoDateTime.getYear(), isoDateTime.getMonth(), isoDateTime.getDay(),
                                plainTime.getHour(), plainTime.getMinute(), plainTime.getSecond(), plainTime.getMillisecond(), plainTime.getMicrosecond(), plainTime.getNanosecond(), calendar,
                                this, errorBranch);
                epochNs = TemporalUtil.getEpochNanosecondsFor(getContext(), realm, timeZone, resultISODateTime, Disambiguation.COMPATIBLE);
            }
            return JSTemporalZonedDateTime.create(getContext(), realm, epochNs, timeZone, calendar);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(Object thisObj, Object plainTimeLike) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeWithTimeZone extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeWithTimeZone(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject withTimeZone(JSTemporalZonedDateTimeObject zonedDateTime, Object timeZoneLike,
                        @Cached ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifier) {
            TruffleString timeZone = toTimeZoneIdentifier.execute(timeZoneLike);
            return JSTemporalZonedDateTime.create(getContext(), getRealm(), zonedDateTime.getNanoseconds(), timeZone, zonedDateTime.getCalendar());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(Object thisObj, Object timeZoneLike) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeWithCalendar extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeWithCalendar(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject withCalendar(JSTemporalZonedDateTimeObject zonedDateTime, Object calendarLike,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
            TruffleString calendar = toCalendarIdentifier.executeString(calendarLike);
            return JSTemporalZonedDateTime.create(getContext(), getRealm(), zonedDateTime.getNanoseconds(), zonedDateTime.getTimeZone(), calendar);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(Object thisObj, Object calendarLike) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeAddSubNode extends JSTemporalBuiltinOperation {

        private final int sign;

        protected JSTemporalZonedDateTimeAddSubNode(JSContext context, JSBuiltin builtin, int sign) {
            super(context, builtin);
            this.sign = sign;
        }

        @Specialization
        protected JSTemporalZonedDateTimeObject addDurationToOrSubtractDurationFromZonedDateTime(
                        JSTemporalZonedDateTimeObject zonedDateTime, Object temporalDurationLike, Object optionsParam,
                        @Cached ToTemporalDurationNode toTemporalDurationNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined,
                        @Cached TemporalAddZonedDateTimeNode addZonedDateTimeNode) {
            JSTemporalDurationObject duration = toTemporalDurationNode.execute(temporalDurationLike);
            JSDynamicObject options = getOptionsObject(optionsParam, this, errorBranch, optionUndefined);
            TruffleString timeZone = zonedDateTime.getTimeZone();
            TruffleString calendar = zonedDateTime.getCalendar();
            JSRealm realm = getRealm();
            BigInt norm = TemporalUtil.normalizeTimeDuration(
                            sign * duration.getHours(), sign * duration.getMinutes(), sign * duration.getSeconds(),
                            sign * duration.getMilliseconds(), sign * duration.getMicroseconds(), sign * duration.getNanoseconds());
            BigInt epochNanoseconds = addZonedDateTimeNode.execute(zonedDateTime.getNanoseconds(), timeZone, calendar,
                            sign * duration.getYears(), sign * duration.getMonths(), sign * duration.getWeeks(), sign * duration.getDays(),
                            norm, null, options);
            return JSTemporalZonedDateTime.create(getContext(), realm, epochNanoseconds, timeZone, calendar);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(Object thisObj, Object temporalDurationLike, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeUntilSinceNode extends JSTemporalBuiltinOperation {

        private final int sign;

        protected JSTemporalZonedDateTimeUntilSinceNode(JSContext context, JSBuiltin builtin, int sign) {
            super(context, builtin);
            this.sign = sign;
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected Object differenceTemporalZonedDateTime(JSTemporalZonedDateTimeObject zonedDateTime, Object otherParam, Object options,
                        @Bind Node node,
                        @Cached ToTemporalZonedDateTimeNode toTemporalZonedDateTime,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier,
                        @Cached ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifier,
                        @Cached GetDifferenceSettingsNode getDifferenceSettings,
                        @Cached DifferenceZonedDateTimeWithRoundingNode differenceZonedDateTimeWithRounding,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSTemporalZonedDateTimeObject other = toTemporalZonedDateTime.execute(otherParam, Undefined.instance);
            TruffleString calendar = zonedDateTime.getCalendar();
            if (!TemporalUtil.calendarEquals(calendar, other.getCalendar(), toCalendarIdentifier)) {
                errorBranch.enter(node);
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }
            JSDynamicObject resolvedOptions = getOptionsObject(options, node, errorBranch, optionUndefined);
            var settings = getDifferenceSettings.execute(sign, resolvedOptions, TemporalUtil.unitMappingDateTimeOrAuto, TemporalUtil.unitMappingDateTime, Unit.NANOSECOND, Unit.HOUR);
            Unit largestUnit = settings.largestUnit();
            JSRealm realm = getRealm();
            if (!(Unit.YEAR == largestUnit || Unit.MONTH == largestUnit || Unit.WEEK == largestUnit || Unit.DAY == largestUnit)) {
                var diffRecord = TemporalUtil.differenceInstant(zonedDateTime.getNanoseconds(), other.getNanoseconds(),
                                settings.roundingIncrement(), settings.smallestUnit(), settings.roundingMode());
                BigInt norm = diffRecord.normalizedTimeDuration();
                TimeDurationRecord result = TemporalUtil.balanceTimeDuration(norm, largestUnit);
                return JSTemporalDuration.createTemporalDuration(getContext(), realm, 0, 0, 0, 0,
                                sign * result.hours(), sign * result.minutes(), sign * result.seconds(),
                                sign * result.milliseconds(), sign * result.microseconds(), sign * result.nanoseconds(), node, errorBranch);
            }
            TruffleString timeZone = zonedDateTime.getTimeZone();
            if (!TemporalUtil.timeZoneEquals(timeZone, other.getTimeZone(), toTimeZoneIdentifier)) {
                errorBranch.enter(node);
                throw TemporalErrors.createRangeErrorIdenticalTimeZoneExpected();
            }
            if (zonedDateTime.getNanoseconds().compareTo(other.getNanoseconds()) == 0) {
                return JSTemporalDuration.createTemporalDuration(getContext(), realm, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
            }

            var instant = JSTemporalInstant.create(getContext(), realm, zonedDateTime.getNanoseconds());
            var precalculatedPlainDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), realm, timeZone, instant, calendar);
            var resultRecord = differenceZonedDateTimeWithRounding.execute(zonedDateTime.getNanoseconds(), other.getNanoseconds(), calendar, timeZone,
                            precalculatedPlainDateTime, largestUnit, settings.roundingIncrement(), settings.smallestUnit(), settings.roundingMode());
            JSTemporalDurationRecord result = resultRecord.duration();

            return JSTemporalDuration.createTemporalDuration(getContext(), realm,
                            sign * result.getYears(), sign * result.getMonths(), sign * result.getWeeks(), sign * result.getDays(),
                            sign * result.getHours(), sign * result.getMinutes(), sign * result.getSeconds(),
                            sign * result.getMilliseconds(), sign * result.getMicroseconds(), sign * result.getNanoseconds(), node, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(Object thisObj, Object otherParam, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeRound extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject round(JSTemporalZonedDateTimeObject zonedDateTime, Object roundToParam,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached GetTemporalUnitNode getSmallestUnit,
                        @Cached GetRoundingIncrementOptionNode getRoundingIncrementOption,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            if (roundToParam == Undefined.instance) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorOptionsUndefined();
            }
            JSContext context = getContext();
            JSDynamicObject roundTo;
            if (Strings.isTString(roundToParam)) {
                roundTo = JSOrdinary.createWithNullPrototype(context);
                JSRuntime.createDataPropertyOrThrow(roundTo, TemporalConstants.SMALLEST_UNIT, roundToParam);
            } else {
                roundTo = getOptionsObject(roundToParam, this, errorBranch, optionUndefined);
            }
            int roundingIncrement = getRoundingIncrementOption.execute(roundTo);
            RoundingMode roundingMode = toTemporalRoundingMode(roundTo, HALF_EXPAND, equalNode, getOptionNode);
            Unit smallestUnit = getSmallestUnit.execute(roundTo, TemporalConstants.SMALLEST_UNIT, TemporalUtil.unitMappingTimeOrDay, Unit.REQUIRED);
            int maximum;
            boolean inclusive;
            if (Unit.DAY == smallestUnit) {
                maximum = 1;
                inclusive = true;
            } else {
                maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
                inclusive = false;
            }
            TemporalUtil.validateTemporalRoundingIncrement(roundingIncrement, maximum, inclusive, this, errorBranch);
            JSRealm realm = getRealm();
            BigInt thisNs = zonedDateTime.getNanoseconds();
            TruffleString timeZone = zonedDateTime.getTimeZone();
            TruffleString calendar = zonedDateTime.getCalendar();
            if (smallestUnit == Unit.NANOSECOND && roundingIncrement == 1) {
                return JSTemporalZonedDateTime.create(context, realm, thisNs, timeZone, calendar);
            }
            JSTemporalDateTimeRecord isoDateTime = TemporalUtil.getISODateTimeFor(context, timeZone, thisNs);
            BigInt epochNanoseconds;
            if (smallestUnit == Unit.DAY) {
                ISODateRecord dateEnd = TemporalUtil.balanceISODate(isoDateTime.getYear(), isoDateTime.getMonth(), isoDateTime.getDay() + 1);
                BigInt startNs = TemporalUtil.getStartOfDay(context, timeZone, isoDateTime.getYear(), isoDateTime.getMonth(), isoDateTime.getDay());
                assert thisNs.compareTo(startNs) >= 0;
                BigInt endNs = TemporalUtil.getStartOfDay(context, timeZone, dateEnd.year(), dateEnd.month(), dateEnd.day());
                assert thisNs.compareTo(endNs) < 0;
                BigInt dayLengthNs = endNs.subtract(startNs);
                BigInt dayProgressNs = thisNs.subtract(startNs);
                BigInt roundedDayNs = TemporalUtil.roundNormalizedTimeDurationToIncrement(dayProgressNs, dayLengthNs, roundingMode);
                epochNanoseconds = roundedDayNs.add(startNs);
            } else {
                JSTemporalDurationRecord roundResult = TemporalUtil.roundISODateTime(isoDateTime.getYear(), isoDateTime.getMonth(), isoDateTime.getDay(), isoDateTime.getHour(),
                                isoDateTime.getMinute(), isoDateTime.getSecond(), isoDateTime.getMillisecond(),
                                isoDateTime.getMicrosecond(), isoDateTime.getNanosecond(), roundingIncrement, smallestUnit, roundingMode);
                long offsetNanoseconds = TemporalUtil.getOffsetNanosecondsFor(context, timeZone, thisNs);
                epochNanoseconds = TemporalUtil.interpretISODateTimeOffset(context, realm, dtoi(roundResult.getYears()), dtoi(roundResult.getMonths()), dtoi(roundResult.getDays()),
                                dtoi(roundResult.getHours()), dtoi(roundResult.getMinutes()), dtoi(roundResult.getSeconds()), dtoi(roundResult.getMilliseconds()), dtoi(roundResult.getMicroseconds()),
                                dtoi(roundResult.getNanoseconds()), OffsetBehaviour.OPTION, offsetNanoseconds, timeZone, Disambiguation.COMPATIBLE, OffsetOption.PREFER, MatchBehaviour.MATCH_EXACTLY);
            }
            return JSTemporalZonedDateTime.create(context, realm, epochNanoseconds, timeZone, calendar);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(Object thisObj, Object roundToParam) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeEquals extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean equals(JSTemporalZonedDateTimeObject thisZonedDateTime, Object otherParam,
                        @Cached ToTemporalZonedDateTimeNode toTemporalZonedDateTime,
                        @Cached ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifier,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
            JSTemporalZonedDateTimeObject otherZonedDateTime = toTemporalZonedDateTime.execute(otherParam, Undefined.instance);
            if (!thisZonedDateTime.getNanoseconds().equals(otherZonedDateTime.getNanoseconds())) {
                return false;
            }
            if (!TemporalUtil.timeZoneEquals(thisZonedDateTime.getTimeZone(), otherZonedDateTime.getTimeZone(), toTimeZoneIdentifier)) {
                return false;
            }
            return TemporalUtil.calendarEquals(thisZonedDateTime.getCalendar(), otherZonedDateTime.getCalendar(), toCalendarIdentifier);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static boolean invalidReceiver(Object thisObj, Object otherParam) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeStartOfDay extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeStartOfDay(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object startOfDay(JSTemporalZonedDateTimeObject zonedDateTime) {
            JSContext context = getContext();
            JSRealm realm = getRealm();
            TruffleString timeZone = zonedDateTime.getTimeZone();
            TruffleString calendar = zonedDateTime.getCalendar();
            JSTemporalDateTimeRecord isoDateTime = TemporalUtil.getISODateTimeFor(context, timeZone, zonedDateTime.getNanoseconds());
            BigInt epochNanoseconds = TemporalUtil.getStartOfDay(context, timeZone, isoDateTime.getYear(), isoDateTime.getMonth(), isoDateTime.getDay());
            return JSTemporalZonedDateTime.create(context, realm, epochNanoseconds, timeZone, calendar);
        }

        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    @ImportStatic(IntlUtil.class)
    public abstract static class JSTemporalZonedDateTimeGetTimeZoneTransition extends JSTemporalBuiltinOperation {
        private static final List<String> DIRECTION_OPTION_VALUES = List.of(IntlUtil.NEXT, IntlUtil.PREVIOUS);

        protected JSTemporalZonedDateTimeGetTimeZoneTransition(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object getTimeZoneTransition(JSTemporalZonedDateTimeObject zonedDateTime, Object param,
                        @Bind Node node,
                        @Cached("create(getContext(), KEY_DIRECTION)") CreateDataPropertyNode createDirectionProperty,
                        @Cached("create(getContext())") GetOptionsObjectNode getOptionsObject,
                        @Cached("createDirectionGetter()") GetStringOptionNode getDirection,
                        @Cached InlinedBranchProfile errorBranch) {
            JSContext context = getContext();
            Object directionParam = param;
            TruffleString timeZone = zonedDateTime.getTimeZone();
            if (directionParam == Undefined.instance) {
                errorBranch.enter(node);
                throw Errors.createTypeError("Direction not specified");
            }
            if (directionParam instanceof TruffleString paramString) {
                directionParam = JSOrdinary.createWithNullPrototype(context);
                createDirectionProperty.executeVoid(directionParam, paramString);
            } else {
                directionParam = getOptionsObject.execute(directionParam);
            }
            String direction = getDirection.executeValue(directionParam);
            if (direction == null) { // direction is required
                errorBranch.enter(node);
                throw Errors.createRangeError("Invalid direction");
            }
            if (TemporalUtil.canParseAsTimeZoneNumericUTCOffset(timeZone)) {
                return Null.instance;
            }
            BigInt transition;
            if (IntlUtil.NEXT.equals(direction)) {
                transition = TemporalUtil.getNamedTimeZoneNextTransition(context, timeZone, zonedDateTime.getNanoseconds());
            } else {
                assert IntlUtil.PREVIOUS.equals(direction);
                transition = TemporalUtil.getNamedTimeZonePreviousTransition(context, timeZone, zonedDateTime.getNanoseconds());
            }
            if (transition == null) {
                return Null.instance;
            }
            return JSTemporalZonedDateTime.create(context, getRealm(), transition, timeZone, zonedDateTime.getCalendar());
        }

        @NeverDefault
        protected GetStringOptionNode createDirectionGetter() {
            return GetStringOptionNode.create(getContext(), IntlUtil.KEY_DIRECTION, DIRECTION_OPTION_VALUES, null);
        }

        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        @SuppressWarnings("unused")
        static Object invalidReceiver(Object thisObj, Object directionParam) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeToInstant extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToInstant(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object toInstant(JSTemporalZonedDateTimeObject zonedDateTime) {
            return JSTemporalInstant.create(getContext(), getRealm(), zonedDateTime.getNanoseconds());
        }

        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeToPlainDate extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToPlainDate(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object toPlainDate(JSTemporalZonedDateTimeObject zdt,
                        @Cached InlinedBranchProfile errorBranch) {
            TruffleString timeZone = zdt.getTimeZone();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), getRealm(), zdt.getNanoseconds());
            TruffleString calendar = zdt.getCalendar();
            JSTemporalPlainDateTimeObject dt = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), getRealm(), timeZone, instant, calendar);
            return JSTemporalPlainDate.create(getContext(), getRealm(), dt.getYear(), dt.getMonth(), dt.getDay(), calendar, this, errorBranch);
        }

        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeToPlainTime extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToPlainTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object toPlainTime(JSTemporalZonedDateTimeObject zdt,
                        @Cached InlinedBranchProfile errorBranch) {
            TruffleString timeZone = zdt.getTimeZone();
            JSRealm realm = getRealm();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), realm, zdt.getNanoseconds());
            TruffleString calendar = zdt.getCalendar();
            JSTemporalPlainDateTimeObject dt = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), realm, timeZone, instant, calendar);
            return JSTemporalPlainTime.create(getContext(), realm, dt.getHour(), dt.getMinute(), dt.getSecond(), dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(), this, errorBranch);
        }

        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeToPlainDateTime extends JSTemporalBuiltinOperation {

        protected JSTemporalZonedDateTimeToPlainDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object toPlainDateTime(JSTemporalZonedDateTimeObject zdt) {
            TruffleString timeZone = zdt.getTimeZone();
            JSRealm realm = getRealm();
            JSTemporalInstantObject instant = JSTemporalInstant.create(getContext(), realm, zdt.getNanoseconds());
            TruffleString calendar = zdt.getCalendar();
            return TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), realm, timeZone, instant, calendar);
        }

        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

}
