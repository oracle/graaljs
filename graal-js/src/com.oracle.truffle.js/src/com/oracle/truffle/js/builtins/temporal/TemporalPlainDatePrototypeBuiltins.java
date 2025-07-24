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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;

import java.util.EnumSet;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateAddSubNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateCalendarGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToPlainDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToPlainMonthDayNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToPlainYearMonthNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateUntilSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateWithCalendarNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateWithNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.GetDifferenceSettingsNode;
import com.oracle.truffle.js.nodes.temporal.IsPartialTemporalObjectNode;
import com.oracle.truffle.js.nodes.temporal.RoundRelativeDurationNode;
import com.oracle.truffle.js.nodes.temporal.TemporalAddDateNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarDateFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalDifferenceDateNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.TemporalMonthDayFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalYearMonthFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarIdentifierNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDateNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneIdentifierNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDayObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonthObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.NormalizedDurationRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Disambiguation;
import com.oracle.truffle.js.runtime.util.TemporalUtil.ShowCalendar;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;
import org.graalvm.shadowed.com.ibm.icu.util.Calendar;

public class TemporalPlainDatePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainDatePrototypeBuiltins.TemporalPlainDatePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainDatePrototypeBuiltins();

    protected TemporalPlainDatePrototypeBuiltins() {
        super(JSTemporalPlainDate.PROTOTYPE_NAME, TemporalPlainDatePrototype.class);
    }

    public enum TemporalPlainDatePrototype implements BuiltinEnum<TemporalPlainDatePrototype> {
        // getters
        calendarId(0),
        era(0),
        eraYear(0),
        year(0),
        month(0),
        monthCode(0),
        day(0),
        dayOfYear(0),
        dayOfWeek(0),
        weekOfYear(0),
        yearOfWeek(0),
        daysInWeek(0),
        daysInMonth(0),
        daysInYear(0),
        monthsInYear(0),
        inLeapYear(0),

        // methods
        toPlainYearMonth(0),
        toPlainMonthDay(0),
        add(1),
        subtract(1),
        with(1),
        withCalendar(1),
        until(1),
        since(1),
        equals(1),
        toPlainDateTime(0),
        toZonedDateTime(1),
        toString(0),
        toLocaleString(0),
        toJSON(0),
        valueOf(0);

        private final int length;

        TemporalPlainDatePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(calendarId, era, eraYear, year, month, monthCode, day, dayOfYear, dayOfWeek, weekOfYear, yearOfWeek, daysInWeek, daysInMonth, daysInYear,
                            monthsInYear, inLeapYear).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainDatePrototype builtinEnum) {
        switch (builtinEnum) {
            case calendarId:
                return JSTemporalPlainDateCalendarGetterNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case era:
            case eraYear:
            case year:
            case month:
            case monthCode:
            case day:
            case dayOfYear:
            case dayOfWeek:
            case weekOfYear:
            case yearOfWeek:
            case daysInWeek:
            case daysInMonth:
            case daysInYear:
            case monthsInYear:
            case inLeapYear:
                return JSTemporalPlainDateGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

            case add:
                return JSTemporalPlainDateAddSubNodeGen.create(context, builtin, TemporalUtil.ADD, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case subtract:
                return JSTemporalPlainDateAddSubNodeGen.create(context, builtin, TemporalUtil.SUBTRACT, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case with:
                return JSTemporalPlainDateWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case withCalendar:
                return JSTemporalPlainDateWithCalendarNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case until:
                return JSTemporalPlainDateUntilSinceNodeGen.create(context, builtin, TemporalUtil.UNTIL, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalPlainDateUntilSinceNodeGen.create(context, builtin, TemporalUtil.SINCE, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case equals:
                return JSTemporalPlainDateEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainDateTime:
                return JSTemporalPlainDateToPlainDateTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainYearMonth:
                return JSTemporalPlainDateToPlainYearMonthNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainMonthDay:
                return JSTemporalPlainDateToPlainMonthDayNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toZonedDateTime:
                return JSTemporalPlainDateToZonedDateTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toString:
                return JSTemporalPlainDateToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalPlainDateToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return UnsupportedValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainDateCalendarGetterNode extends JSBuiltinNode {

        protected JSTemporalPlainDateCalendarGetterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static TruffleString calendarId(JSTemporalPlainDateObject temporalDate,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
            return toCalendarIdentifier.executeString(temporalDate.getCalendar());
        }

        @Specialization(guards = "!isJSTemporalPlainDate(temporalDate)")
        protected static TruffleString invalidReceiver(@SuppressWarnings("unused") Object temporalDate) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateGetterNode extends JSBuiltinNode {

        protected final TemporalPlainDatePrototype property;

        protected JSTemporalPlainDateGetterNode(JSContext context, JSBuiltin builtin, TemporalPlainDatePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization
        protected final Object dateGetter(JSTemporalPlainDateObject temporalDT,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached InlinedConditionProfile isoCalendarProfile) {
            TruffleString calendar = temporalDT.getCalendar();
            boolean isoCalendar = isoCalendarProfile.profile(this, Strings.equals(equalNode, TemporalConstants.ISO8601, calendar));
            Calendar cal = null;
            if (!isoCalendar) {
                cal = IntlUtil.getCalendar(calendar, temporalDT.getYear(), temporalDT.getMonth(), temporalDT.getDay());
            }
            switch (property) {
                case era:
                    return isoCalendar ? Undefined.instance : IntlUtil.getEra(cal);
                case eraYear:
                    return isoCalendar ? Undefined.instance : IntlUtil.getEraYear(cal);
                case year:
                    return isoCalendar ? temporalDT.getYear() : IntlUtil.getCalendarField(cal, Calendar.EXTENDED_YEAR);
                case month:
                    return isoCalendar ? temporalDT.getMonth() : (IntlUtil.getCalendarField(cal, Calendar.ORDINAL_MONTH) + 1);
                case day:
                    return isoCalendar ? temporalDT.getDay() : IntlUtil.getCalendarField(cal, Calendar.DAY_OF_MONTH);
                case dayOfWeek:
                    return isoCalendar ? TemporalUtil.toISODayOfWeek(temporalDT.getYear(), temporalDT.getMonth(), temporalDT.getDay()) : IntlUtil.getCalendarField(cal, Calendar.DAY_OF_WEEK);
                case dayOfYear:
                    return isoCalendar ? TemporalUtil.toISODayOfYear(temporalDT.getYear(), temporalDT.getMonth(), temporalDT.getDay()) : IntlUtil.getCalendarField(cal, Calendar.DAY_OF_YEAR);
                case monthCode:
                    return isoCalendar ? TemporalUtil.buildISOMonthCode(temporalDT.getMonth()) : Strings.fromJavaString(IntlUtil.getTemporalMonthCode(cal));
                case weekOfYear:
                    return isoCalendar ? TemporalUtil.weekOfToISOWeekOfYear(temporalDT.getYear(), temporalDT.getMonth(), temporalDT.getDay()) : Undefined.instance;
                case yearOfWeek:
                    return isoCalendar ? TemporalUtil.yearOfToISOWeekOfYear(temporalDT.getYear(), temporalDT.getMonth(), temporalDT.getDay()) : Undefined.instance;
                case daysInWeek:
                    return isoCalendar ? 7 : IntlUtil.getCalendarFieldMax(cal, Calendar.DAY_OF_WEEK);
                case daysInMonth:
                    return isoCalendar ? TemporalUtil.isoDaysInMonth(temporalDT.getYear(), temporalDT.getMonth()) : IntlUtil.getCalendarFieldMax(cal, Calendar.DAY_OF_MONTH);
                case daysInYear:
                    return isoCalendar ? TemporalUtil.isoDaysInYear(temporalDT.getYear()) : IntlUtil.getCalendarFieldMax(cal, Calendar.DAY_OF_YEAR);
                case monthsInYear:
                    return isoCalendar ? 12 : (IntlUtil.getCalendarFieldMax(cal, Calendar.ORDINAL_MONTH) + 1);
                case inLeapYear:
                    return isoCalendar ? JSDate.isLeapYear(temporalDT.getYear()) : IntlUtil.isLeapYear(cal);
            }
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    // 4.3.10
    public abstract static class JSTemporalPlainDateAddSubNode extends JSTemporalBuiltinOperation {

        private final int sign;

        protected JSTemporalPlainDateAddSubNode(JSContext context, JSBuiltin builtin, int sign) {
            super(context, builtin);
            this.sign = sign;
        }

        @Specialization
        protected final JSTemporalPlainDateObject addDate(JSTemporalPlainDateObject date, Object temporalDurationLike, Object options,
                        @Cached ToTemporalDurationNode toTemporalDurationNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached TemporalAddDateNode addDateNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            TruffleString calendar = date.getCalendar();
            JSTemporalDurationObject duration = toTemporalDurationNode.execute(temporalDurationLike);
            JSRealm realm = getRealm();

            if (sign < 0) {
                duration = JSTemporalDuration.createNegatedTemporalDuration(getContext(), realm, duration);
            }

            Object resolvedOptions = getOptionsObject(options, this, errorBranch, optionUndefined);
            TemporalUtil.Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            return addDateNode.execute(calendar, date, duration, overflow);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalPlainDateObject invalidReceiver(Object thisObj, Object temporalDurationLike, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateWith extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSTemporalPlainDateObject with(JSTemporalPlainDateObject temporalDate, Object temporalDateLike, Object options,
                        @Cached IsPartialTemporalObjectNode isPartialTemporalObjectNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached TemporalCalendarDateFromFieldsNode dateFromFieldsNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            if (!isPartialTemporalObjectNode.execute(temporalDateLike)) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorPartialTemporalObjectExpected();
            }

            JSContext ctx = getContext();
            TruffleString calendar = temporalDate.getCalendar();
            JSDynamicObject fields = TemporalUtil.isoDateToFields(ctx, calendar, temporalDate.isoDate(), TemporalUtil.FieldsType.DATE);
            JSDynamicObject partialDate = TemporalUtil.prepareCalendarFields(ctx, calendar, temporalDateLike, TemporalUtil.listDMMCY, TemporalUtil.listEmpty, null);
            fields = TemporalUtil.calendarMergeFields(ctx, calendar, fields, partialDate);
            JSDynamicObject resolvedOptions = getOptionsObject(options, this, errorBranch, optionUndefined);
            TemporalUtil.Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            return dateFromFieldsNode.execute(calendar, fields, overflow);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalPlainDateObject invalidReceiver(Object thisObj, Object temporalDateLike, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateWithCalendar extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateWithCalendar(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSTemporalPlainDateObject withCalendar(JSTemporalPlainDateObject date, Object calendarLike,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier,
                        @Cached InlinedBranchProfile errorBranch) {
            TruffleString calendar = toCalendarIdentifier.executeString(calendarLike);
            return JSTemporalPlainDate.create(getContext(), getRealm(), date.getYear(), date.getMonth(), date.getDay(), calendar, this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalPlainDateObject invalidReceiver(Object thisObj, Object calendarParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateUntilSinceNode extends JSTemporalBuiltinOperation {

        private final int sign;

        protected JSTemporalPlainDateUntilSinceNode(JSContext context, JSBuiltin builtin, int sign) {
            super(context, builtin);
            this.sign = sign;
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected JSTemporalDurationObject differenceTemporalPlainDate(JSTemporalPlainDateObject temporalDate, Object otherObj, Object options,
                        @Bind Node node,
                        @Cached TemporalDifferenceDateNode differenceDate,
                        @Cached ToTemporalDateNode toTemporalDate,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier,
                        @Cached RoundRelativeDurationNode roundRelativeDuration,
                        @Cached GetDifferenceSettingsNode getDifferenceSettings,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSTemporalPlainDateObject other = toTemporalDate.execute(otherObj);
            if (!TemporalUtil.calendarEquals(temporalDate.getCalendar(), other.getCalendar(), toCalendarIdentifier)) {
                errorBranch.enter(node);
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }

            JSDynamicObject resolvedOptions = getOptionsObject(options, node, errorBranch, optionUndefined);
            var settings = getDifferenceSettings.execute(sign, resolvedOptions, TemporalUtil.unitMappingDateOrAuto, TemporalUtil.unitMappingDate, Unit.DAY, Unit.DAY);

            TruffleString calendar = temporalDate.getCalendar();
            JSTemporalDurationObject result = differenceDate.execute(calendar, temporalDate, other, settings.largestUnit());
            NormalizedDurationRecord duration = TemporalUtil.createNormalizedDurationRecord(
                            result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), TemporalUtil.zeroTimeDuration());

            boolean roundingGranularityIsNoop = settings.smallestUnit() == Unit.DAY && (settings.roundingIncrement() == 1);
            if (!roundingGranularityIsNoop) {
                BigInt destEpochNs = TemporalUtil.getUTCEpochNanoseconds(other.getYear(), other.getMonth(), other.getDay(), 0, 0, 0, 0, 0, 0);
                var dateTime = new ISODateTimeRecord(temporalDate.getYear(), temporalDate.getMonth(), temporalDate.getDay(), 0, 0, 0, 0, 0, 0);
                var roundedDuration = roundRelativeDuration.execute(duration, destEpochNs, dateTime, calendar, null,
                                settings.largestUnit(), settings.roundingIncrement(), settings.smallestUnit(), settings.roundingMode()).duration();
                duration = new NormalizedDurationRecord(roundedDuration.getYears(), roundedDuration.getMonths(), roundedDuration.getWeeks(), roundedDuration.getDays(), BigInt.ZERO);
            }
            return JSTemporalDuration.createTemporalDuration(getContext(), getRealm(),
                            sign * duration.years(), sign * duration.months(), sign * duration.weeks(), sign * duration.days(),
                            0, 0, 0, 0, 0, 0, node, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalDurationObject invalidReceiver(Object thisObj, Object otherObj, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateToString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(JSTemporalPlainDateObject date, Object optionsParam,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSDynamicObject options = getOptionsObject(optionsParam, this, errorBranch, optionUndefined);
            ShowCalendar showCalendar = TemporalUtil.toShowCalendarOption(options, getOptionNode, equalNode);
            return JSTemporalPlainDate.temporalDateToString(date, showCalendar);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static TruffleString invalidReceiver(Object thisObj, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static TruffleString toLocaleString(JSTemporalPlainDateObject date) {
            return JSTemporalPlainDate.temporalDateToString(date, ShowCalendar.AUTO);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static TruffleString invalidReceiver(Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateToPlainDateTime extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToPlainDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSTemporalPlainDateTimeObject toPlainDateTime(JSTemporalPlainDateObject date, Object temporalTimeObj,
                        @Cached ToTemporalTimeNode toTemporalTime,
                        @Cached InlinedBranchProfile errorBranch) {
            if (temporalTimeObj == Undefined.instance) {
                return JSTemporalPlainDateTime.create(getContext(), getRealm(),
                                date.getYear(), date.getMonth(), date.getDay(),
                                0, 0, 0, 0, 0, 0, date.getCalendar(), this, errorBranch);
            }
            JSTemporalPlainTimeObject time = toTemporalTime.execute(temporalTimeObj, Undefined.instance);
            return JSTemporalPlainDateTime.create(getContext(), getRealm(),
                            date.getYear(), date.getMonth(), date.getDay(),
                            time.getHour(), time.getMinute(), time.getSecond(), time.getMillisecond(), time.getMicrosecond(),
                            time.getNanosecond(), date.getCalendar(), this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalPlainDateTimeObject invalidReceiver(Object thisObj, Object temporalTimeObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateToPlainYearMonth extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToPlainYearMonth(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSTemporalPlainYearMonthObject toPlainYearMonth(JSTemporalPlainDateObject temporalDate,
                        @Cached TemporalYearMonthFromFieldsNode yearMonthFromFieldsNode) {
            TruffleString calendar = temporalDate.getCalendar();
            JSDynamicObject fields = TemporalUtil.isoDateToFields(getContext(), calendar, temporalDate.isoDate(), TemporalUtil.FieldsType.DATE);
            return yearMonthFromFieldsNode.execute(calendar, fields, TemporalUtil.Overflow.CONSTRAIN);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalPlainYearMonthObject invalidReceiver(Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateToPlainMonthDay extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToPlainMonthDay(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSTemporalPlainMonthDayObject toPlainMonthDay(JSTemporalPlainDateObject temporalDate,
                        @Cached TemporalMonthDayFromFieldsNode monthDayFromFieldsNode) {
            TruffleString calendar = temporalDate.getCalendar();
            JSDynamicObject fields = TemporalUtil.isoDateToFields(getContext(), calendar, temporalDate.isoDate(), TemporalUtil.FieldsType.DATE);
            return monthDayFromFieldsNode.execute(calendar, fields, TemporalUtil.Overflow.CONSTRAIN);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalPlainMonthDayObject invalidReceiver(Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateEquals extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static boolean equals(JSTemporalPlainDateObject temporalDate, Object otherParam,
                        @Cached ToTemporalDateNode toTemporalDate,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
            JSTemporalPlainDateObject other = toTemporalDate.execute(otherParam);
            if (temporalDate.getYear() != other.getYear()) {
                return false;
            }
            if (temporalDate.getMonth() != other.getMonth()) {
                return false;
            }
            if (temporalDate.getDay() != other.getDay()) {
                return false;
            }
            return TemporalUtil.calendarEquals(temporalDate.getCalendar(), other.getCalendar(), toCalendarIdentifier);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static boolean invalidReceiver(Object thisObj, Object otherParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateToZonedDateTimeNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToZonedDateTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSTemporalZonedDateTimeObject toZonedDateTime(JSTemporalPlainDateObject td, Object item,
                        @Cached InlinedConditionProfile timeZoneIsUndefined,
                        @Cached InlinedConditionProfile timeIsUndefined,
                        @Cached ToTemporalTimeNode toTemporalTime,
                        @Cached ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifierNode,
                        @Cached InlinedBranchProfile errorBranch) {
            TruffleString timeZone;
            Object temporalTime;
            JSTemporalPlainDateTimeObject temporalDateTime;
            if (isObject(item)) {
                Object timeZoneLike = JSRuntime.get(item, TIME_ZONE);
                if (timeZoneIsUndefined.profile(this, timeZoneLike == Undefined.instance)) {
                    timeZone = toTimeZoneIdentifierNode.execute(item);
                    temporalTime = Undefined.instance;
                } else {
                    timeZone = toTimeZoneIdentifierNode.execute(timeZoneLike);
                    temporalTime = JSRuntime.get(item, TemporalConstants.PLAIN_TIME);
                }
            } else {
                timeZone = toTimeZoneIdentifierNode.execute(item);
                temporalTime = Undefined.instance;
            }

            JSRealm realm = getRealm();
            if (timeIsUndefined.profile(this, temporalTime == Undefined.instance)) {
                temporalDateTime = JSTemporalPlainDateTime.create(getContext(), realm,
                                td.getYear(), td.getMonth(), td.getDay(),
                                0, 0, 0, 0, 0, 0, td.getCalendar(),
                                this, errorBranch);
            } else {
                JSTemporalPlainTimeObject tt = toTemporalTime.execute(temporalTime, Undefined.instance);
                temporalDateTime = JSTemporalPlainDateTime.create(getContext(), realm,
                                td.getYear(), td.getMonth(), td.getDay(),
                                tt.getHour(), tt.getMinute(), tt.getSecond(), tt.getMillisecond(), tt.getMicrosecond(), tt.getNanosecond(), td.getCalendar(),
                                this, errorBranch);
            }

            BigInt epochNs = TemporalUtil.getEpochNanosecondsFor(getContext(), realm, timeZone, temporalDateTime, Disambiguation.COMPATIBLE);
            return JSTemporalZonedDateTime.create(getContext(), realm, epochNs, timeZone, td.getCalendar());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalZonedDateTimeObject invalidReceiver(Object thisObj, Object item) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

}
