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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtoi;

import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeAddSubNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeCalendarGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeRoundNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToPlainMonthDayNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToPlainTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToPlainYearMonthNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeToZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeUntilSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeWithCalendarNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeWithNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeWithPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeWithPlainTimeNodeGen;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.DifferencePlainDateTimeWithRoundingNode;
import com.oracle.truffle.js.nodes.temporal.GetDifferenceSettingsNode;
import com.oracle.truffle.js.nodes.temporal.GetRoundingIncrementOptionNode;
import com.oracle.truffle.js.nodes.temporal.GetTemporalUnitNode;
import com.oracle.truffle.js.nodes.temporal.IsPartialTemporalObjectNode;
import com.oracle.truffle.js.nodes.temporal.TemporalAddDateTimeNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarDateFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.TemporalMonthDayFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalYearMonthFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.ToFractionalSecondDigitsNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarIdentifierNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarSlotValueNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDateNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDateTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneIdentifierNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDayObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonthObject;
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
            return EnumSet.of(calendarId, era, eraYear, hour, minute, second, millisecond, microsecond, nanosecond, year, month, monthCode, day, dayOfYear, dayOfWeek, weekOfYear, yearOfWeek,
                            daysInWeek, daysInMonth, daysInYear, monthsInYear, inLeapYear).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainDateTimePrototype builtinEnum) {
        switch (builtinEnum) {
            case calendarId:
                return JSTemporalPlainDateTimeCalendarGetterNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
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
            case hour:
            case minute:
            case second:
            case millisecond:
            case microsecond:
            case nanosecond:
                return JSTemporalPlainDateTimeGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));
            case add:
                return JSTemporalPlainDateTimeAddSubNodeGen.create(context, builtin, TemporalUtil.ADD, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case subtract:
                return JSTemporalPlainDateTimeAddSubNodeGen.create(context, builtin, TemporalUtil.SUBTRACT, args().withThis().fixedArgs(2).createArgumentNodes(context));
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
                return JSTemporalPlainDateTimeUntilSinceNodeGen.create(context, builtin, TemporalUtil.UNTIL,
                                args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalPlainDateTimeUntilSinceNodeGen.create(context, builtin, TemporalUtil.SINCE,
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
                return UnsupportedValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainDateTimeCalendarGetterNode extends JSBuiltinNode {

        public JSTemporalPlainDateTimeCalendarGetterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString calendarId(JSTemporalPlainDateTimeObject dateTime,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
            return toCalendarIdentifier.executeString(dateTime.getCalendar());
        }

        @Specialization(guards = "!isJSTemporalPlainDateTime(dateTime)")
        protected static TruffleString invalidReceiver(@SuppressWarnings("unused") Object dateTime) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeGetterNode extends JSBuiltinNode {

        public final TemporalPlainDateTimePrototype property;

        public JSTemporalPlainDateTimeGetterNode(JSContext context, JSBuiltin builtin, TemporalPlainDateTimePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization
        protected Object dateTimeGetter(JSTemporalPlainDateTimeObject temporalDT) {
            switch (property) {
                case era:
                    return Undefined.instance;
                case eraYear:
                    return Undefined.instance;
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
                    return TemporalUtil.toISODayOfWeek(temporalDT.getYear(), temporalDT.getMonth(), temporalDT.getDay());
                case dayOfYear:
                    return TemporalUtil.toISODayOfYear(temporalDT.getYear(), temporalDT.getMonth(), temporalDT.getDay());
                case monthCode:
                    return TemporalUtil.buildISOMonthCode(temporalDT.getMonth());
                case weekOfYear:
                    return TemporalUtil.weekOfToISOWeekOfYear(temporalDT.getYear(), temporalDT.getMonth(), temporalDT.getDay());
                case yearOfWeek:
                    return TemporalUtil.yearOfToISOWeekOfYear(temporalDT.getYear(), temporalDT.getMonth(), temporalDT.getDay());
                case daysInWeek:
                    return 7;
                case daysInMonth:
                    return TemporalUtil.isoDaysInMonth(temporalDT.getYear(), temporalDT.getMonth());
                case daysInYear:
                    return TemporalUtil.isoDaysInYear(temporalDT.getYear());
                case monthsInYear:
                    return 12;
                case inLeapYear:
                    return JSDate.isLeapYear(temporalDT.getYear());

            }
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeAddSubNode extends JSTemporalBuiltinOperation {

        private final int sign;

        protected JSTemporalPlainDateTimeAddSubNode(JSContext context, JSBuiltin builtin, int sign) {
            super(context, builtin);
            this.sign = sign;
        }

        @Specialization
        final JSTemporalPlainDateTimeObject addDurationToOrSubtractDurationFromPlainDateTime(
                        JSTemporalPlainDateTimeObject dateTime, Object temporalDurationLike, Object options,
                        @Cached ToTemporalDurationNode toTemporalDurationNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached TemporalAddDateTimeNode addDateTime,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSTemporalDurationObject duration = toTemporalDurationNode.execute(temporalDurationLike);
            TemporalUtil.rejectDurationSign(
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
            JSDynamicObject resolvedOptions = getOptionsObject(options, this, errorBranch, optionUndefined);
            TemporalUtil.Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);

            TruffleString calendar = dateTime.getCalendar();

            BigInt norm = TemporalUtil.normalizeTimeDuration(sign * duration.getHours(), sign * duration.getMinutes(), sign * duration.getSeconds(),
                            sign * duration.getMilliseconds(), sign * duration.getMicroseconds(), sign * duration.getNanoseconds());
            JSTemporalDateTimeRecord result = addDateTime.execute(
                            dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(),
                            dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
                            dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(),
                            calendar,
                            sign * duration.getYears(), sign * duration.getMonths(), sign * duration.getWeeks(), sign * duration.getDays(), norm,
                            overflow);

            return JSTemporalPlainDateTime.create(getContext(), getRealm(),
                            result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(),
                            result.getNanosecond(), dateTime.getCalendar(), this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSTemporalPlainDateTimeObject invalidReceiver(Object thisObj, Object temporalDurationLike, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeUntilSinceNode extends JSTemporalBuiltinOperation {

        private final int sign;

        protected JSTemporalPlainDateTimeUntilSinceNode(JSContext context, JSBuiltin builtin, int sign) {
            super(context, builtin);
            this.sign = sign;
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected JSTemporalDurationObject differenceTemporalPlainDateTime(JSTemporalPlainDateTimeObject dateTime, Object otherObj, Object options,
                        @Bind Node node,
                        @Cached ToTemporalDateTimeNode toTemporalDateTime,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier,
                        @Cached DifferencePlainDateTimeWithRoundingNode differencePlainDateTimeWithRounding,
                        @Cached GetDifferenceSettingsNode getDifferenceSettings,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSTemporalPlainDateTimeObject other = toTemporalDateTime.execute(otherObj, Undefined.instance);
            if (!TemporalUtil.calendarEquals(dateTime.getCalendar(), other.getCalendar(), toCalendarIdentifier)) {
                errorBranch.enter(node);
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }

            JSContext ctx = getContext();
            JSRealm realm = getRealm();
            JSDynamicObject resolvedOptions = getOptionsObject(options, node, errorBranch, optionUndefined);
            var settings = getDifferenceSettings.execute(sign, resolvedOptions, TemporalUtil.unitMappingDateTimeOrAuto, TemporalUtil.unitMappingDateTime, Unit.NANOSECOND, Unit.DAY);

            if (dateTime.getYear() == other.getYear() && dateTime.getMonth() == other.getMonth() && dateTime.getDay() == other.getDay() && dateTime.getHour() == other.getHour() &&
                            dateTime.getMinute() == other.getMinute() && dateTime.getSecond() == other.getSecond() && dateTime.getMillisecond() == other.getMillisecond() &&
                            dateTime.getMicrosecond() == other.getMicrosecond() && dateTime.getNanosecond() == other.getNanosecond()) {
                return JSTemporalDuration.createTemporalDuration(ctx, realm,
                                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
            }

            JSTemporalPlainDateObject plainDate = JSTemporalPlainDate.create(ctx, realm,
                            dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getCalendar(), node, errorBranch);
            var resultRecord = differencePlainDateTimeWithRounding.execute(plainDate,
                            dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(),
                            other.getYear(), other.getMonth(), other.getDay(),
                            other.getHour(), other.getMinute(), other.getSecond(), other.getMillisecond(), other.getMicrosecond(), other.getNanosecond(),
                            other.getCalendar(), settings.largestUnit(), settings.roundingIncrement(), settings.smallestUnit(), settings.roundingMode());
            JSTemporalDurationRecord result = resultRecord.duration();

            return JSTemporalDuration.createTemporalDuration(ctx, realm,
                            sign * result.getYears(), sign * result.getMonths(), sign * result.getWeeks(), sign * result.getDays(),
                            sign * result.getHours(), sign * result.getMinutes(), sign * result.getSeconds(),
                            sign * result.getMilliseconds(), sign * result.getMicroseconds(), sign * result.getNanoseconds(),
                            node, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSTemporalDurationObject invalidReceiver(Object thisObj, Object otherObj, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeGetISOFields extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        final JSObject getISOFields(JSTemporalPlainDateTimeObject dt) {
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

        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSObject invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeToString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(JSTemporalPlainDateTimeObject dt, Object optionsParam,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached ToFractionalSecondDigitsNode toFractionalSecondDigitsNode,
                        @Cached GetTemporalUnitNode getSmallestUnit,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSDynamicObject options = getOptionsObject(optionsParam, this, errorBranch, optionUndefined);
            ShowCalendar showCalendar = TemporalUtil.toShowCalendarOption(options, getOptionNode, equalNode);
            int digits = toFractionalSecondDigitsNode.execute(options);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode, getOptionNode);

            Unit smallestUnit = getSmallestUnit.execute(options, TemporalConstants.SMALLEST_UNIT, TemporalUtil.unitMappingTime, Unit.EMPTY);
            if (smallestUnit == Unit.HOUR) {
                errorBranch.enter(this);
                throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
            }

            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecisionRecord(smallestUnit, digits);

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

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static TruffleString invalidReceiver(Object thisObj, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        static TruffleString toLocaleString(JSTemporalPlainDateTimeObject dt) {
            return JSTemporalPlainDateTime.temporalDateTimeToString(dt.getYear(), dt.getMonth(), dt.getDay(),
                            dt.getHour(), dt.getMinute(), dt.getSecond(),
                            dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(),
                            dt.getCalendar(), AUTO, ShowCalendar.AUTO);
        }

        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    @ImportStatic(TemporalConstants.class)
    public abstract static class JSTemporalPlainDateTimeWith extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        final JSTemporalPlainDateTimeObject with(JSTemporalPlainDateTimeObject dateTime, Object temporalDateTimeLike, Object options,
                        @Bind Node node,
                        @Cached IsPartialTemporalObjectNode isPartialTemporalObjectNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached("create(getContext(), HOUR)") CreateDataPropertyNode createHourDataPropertyNode,
                        @Cached("create(getContext(), MINUTE)") CreateDataPropertyNode createMinuteDataPropertyNode,
                        @Cached("create(getContext(), SECOND)") CreateDataPropertyNode createSecondDataPropertyNode,
                        @Cached("create(getContext(), MILLISECOND)") CreateDataPropertyNode createMillisecondDataPropertyNode,
                        @Cached("create(getContext(), MICROSECOND)") CreateDataPropertyNode createMicrosecondDataPropertyNode,
                        @Cached("create(getContext(), NANOSECOND)") CreateDataPropertyNode createNanosecondDataPropertyNode,
                        @Cached TemporalCalendarDateFromFieldsNode dateFromFieldsNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            if (!isPartialTemporalObjectNode.execute(temporalDateTimeLike)) {
                errorBranch.enter(node);
                throw TemporalErrors.createTypeErrorPartialTemporalObjectExpected();
            }

            TruffleString calendar = dateTime.getCalendar();

            List<TruffleString> fieldNames = Boundaries.listEditableCopy(TemporalUtil.listDMMCY);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), dateTime, fieldNames, TemporalUtil.listEmpty);

            createHourDataPropertyNode.executeVoid(fields, dateTime.getHour());
            createMinuteDataPropertyNode.executeVoid(fields, dateTime.getMinute());
            createSecondDataPropertyNode.executeVoid(fields, dateTime.getSecond());
            createMillisecondDataPropertyNode.executeVoid(fields, dateTime.getMillisecond());
            createMicrosecondDataPropertyNode.executeVoid(fields, dateTime.getMicrosecond());
            createNanosecondDataPropertyNode.executeVoid(fields, dateTime.getNanosecond());

            addFieldNames(fieldNames);

            JSObject partialDateTime = TemporalUtil.prepareTemporalFields(getContext(), temporalDateTimeLike, fieldNames, null);
            fields = TemporalUtil.calendarMergeFields(getContext(), calendar, fields, partialDateTime);
            fields = TemporalUtil.prepareTemporalFields(getContext(), fields, fieldNames, TemporalUtil.listEmpty);
            Object resolvedOptions = getOptionsObject(options, node, errorBranch, optionUndefined);
            TemporalUtil.Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            JSTemporalDateTimeRecord result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, overflow, dateFromFieldsNode);
            assert TemporalUtil.isValidISODate(result.getYear(), result.getMonth(), result.getDay());
            assert TemporalUtil.isValidTime(result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
            return JSTemporalPlainDateTime.create(getContext(), getRealm(),
                            result.getYear(), result.getMonth(), result.getDay(),
                            result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), calendar,
                            node, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSTemporalPlainDateTimeObject invalidReceiver(Object thisObj, Object temporalDateTimeLike, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }

        @TruffleBoundary
        private static void addFieldNames(List<TruffleString> fieldNames) {
            fieldNames.add(TemporalConstants.HOUR);
            fieldNames.add(TemporalConstants.MICROSECOND);
            fieldNames.add(TemporalConstants.MILLISECOND);
            fieldNames.add(TemporalConstants.MINUTE);
            fieldNames.add(TemporalConstants.NANOSECOND);
            fieldNames.add(TemporalConstants.SECOND);
        }

    }

    public abstract static class JSTemporalPlainDateTimeEquals extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean equalsOtherObj(JSTemporalPlainDateTimeObject thisDateTime, JSTemporalPlainDateTimeObject otherDateTime,
                        @Shared @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
            return equalsIntl(thisDateTime, otherDateTime, toCalendarIdentifier);
        }

        @Specialization(guards = "!isJSTemporalPlainDateTime(other)")
        protected boolean equalsGeneric(JSTemporalPlainDateTimeObject thisDateTime, Object other,
                        @Cached ToTemporalDateTimeNode toTemporalDateTime,
                        @Shared @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
            JSTemporalPlainDateTimeObject otherDateTime = toTemporalDateTime.execute(other, Undefined.instance);
            return equalsIntl(thisDateTime, otherDateTime, toCalendarIdentifier);
        }

        private static boolean equalsIntl(JSTemporalPlainDateTimeObject one, JSTemporalPlainDateTimeObject two, ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
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
                return TemporalUtil.calendarEquals(one.getCalendar(), two.getCalendar(), toCalendarIdentifier);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static boolean invalidReceiver(Object thisObj, Object otherObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeRoundNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeRoundNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        final JSTemporalPlainDateTimeObject round(JSTemporalPlainDateTimeObject dt, Object roundToParam,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached GetTemporalUnitNode getSmallestUnit,
                        @Cached GetRoundingIncrementOptionNode getRoundingIncrementOption,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            if (roundToParam == Undefined.instance) {
                throw TemporalErrors.createTypeErrorOptionsUndefined();
            }
            JSDynamicObject roundTo;
            if (Strings.isTString(roundToParam)) {
                roundTo = JSOrdinary.createWithNullPrototype(getContext());
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
            if (smallestUnit == Unit.NANOSECOND && roundingIncrement == 1) {
                return JSTemporalPlainDateTime.create(getContext(), getRealm(),
                                dt.getYear(), dt.getMonth(), dt.getDay(),
                                dt.getHour(), dt.getMinute(), dt.getSecond(),
                                dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(), dt.getCalendar(), this, errorBranch);
            }
            JSTemporalDurationRecord result = TemporalUtil.roundISODateTime(dt.getYear(), dt.getMonth(), dt.getDay(), dt.getHour(), dt.getMinute(), dt.getSecond(),
                            dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(), roundingIncrement, smallestUnit, roundingMode, null);
            return JSTemporalPlainDateTime.create(getContext(), getRealm(),
                            dtoi(result.getYears()), dtoi(result.getMonths()), dtoi(result.getDays()),
                            dtoi(result.getHours()), dtoi(result.getMinutes()), dtoi(result.getSeconds()),
                            dtoi(result.getMilliseconds()), dtoi(result.getMicroseconds()), dtoi(result.getNanoseconds()), dt.getCalendar(), this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSTemporalPlainDateTimeObject invalidReceiver(Object thisObj, Object roundToParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeToPlainTimeNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToPlainTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        final JSTemporalPlainTimeObject toPlainTime(JSTemporalPlainDateTimeObject dt,
                        @Cached InlinedBranchProfile errorBranch) {
            return JSTemporalPlainTime.create(getContext(), getRealm(),
                            dt.getHour(), dt.getMinute(), dt.getSecond(), dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(), this, errorBranch);
        }

        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSTemporalPlainTimeObject invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeToPlainDateNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToPlainDateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        final JSTemporalPlainDateObject toPlainDate(JSTemporalPlainDateTimeObject dt,
                        @Cached InlinedBranchProfile errorBranch) {
            return JSTemporalPlainDate.create(getContext(), getRealm(),
                            dt.getYear(), dt.getMonth(), dt.getDay(), dt.getCalendar(), this, errorBranch);
        }

        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSTemporalPlainDateObject invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeToZonedDateTimeNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToZonedDateTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        final JSTemporalZonedDateTimeObject toZonedDateTime(JSTemporalPlainDateTimeObject dateTime, Object temporalTimeZoneLike, Object optionsParam,
                        @Cached ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifier,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            TruffleString timeZone = toTimeZoneIdentifier.execute(temporalTimeZoneLike);
            JSDynamicObject options = getOptionsObject(optionsParam, this, errorBranch, optionUndefined);
            Disambiguation disambiguation = TemporalUtil.toTemporalDisambiguation(options, getOptionNode, equalNode);

            BigInt epochNs = TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), getRealm(), timeZone, dateTime, disambiguation);
            return JSTemporalZonedDateTime.create(getContext(), getRealm(), epochNs, timeZone, dateTime.getCalendar());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSTemporalZonedDateTimeObject invalidReceiver(Object thisObj, Object temporalTimeZoneLike, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeToPlainYearMonthNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToPlainYearMonthNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        final JSTemporalPlainYearMonthObject toPlainYearMonth(JSTemporalPlainDateTimeObject dateTime,
                        @Cached TemporalYearMonthFromFieldsNode yearMonthFromFieldsNode) {
            TruffleString calendar = dateTime.getCalendar();
            List<TruffleString> fieldNames = TemporalUtil.listMCY;
            JSObject fields = TemporalUtil.prepareTemporalFields(getContext(), dateTime, fieldNames, TemporalUtil.listEmpty);
            return yearMonthFromFieldsNode.execute(calendar, fields, TemporalUtil.Overflow.CONSTRAIN);
        }

        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSTemporalPlainYearMonthObject invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeToPlainMonthDayNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeToPlainMonthDayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        final JSTemporalPlainMonthDayObject toPlainMonthDay(JSTemporalPlainDateTimeObject dateTime,
                        @Cached TemporalMonthDayFromFieldsNode monthDayFromFieldsNode) {
            TruffleString calendar = dateTime.getCalendar();
            List<TruffleString> fieldNames = TemporalUtil.listDMC;
            JSObject fields = TemporalUtil.prepareTemporalFields(getContext(), dateTime, fieldNames, TemporalUtil.listEmpty);
            return monthDayFromFieldsNode.execute(calendar, fields, TemporalUtil.Overflow.CONSTRAIN);
        }

        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSTemporalPlainMonthDayObject invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeWithPlainTimeNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeWithPlainTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        final JSTemporalPlainDateTimeObject withPlainTime(JSTemporalPlainDateTimeObject temporalDateTime, Object plainTimeLike,
                        @Cached ToTemporalTimeNode toTemporalTime,
                        @Cached InlinedBranchProfile errorBranch) {
            if (plainTimeLike == Undefined.instance) {
                return JSTemporalPlainDateTime.create(getContext(), getRealm(),
                                temporalDateTime.getYear(), temporalDateTime.getMonth(), temporalDateTime.getDay(), 0, 0, 0, 0, 0, 0,
                                temporalDateTime.getCalendar(), this, errorBranch);
            }
            JSTemporalPlainTimeObject plainTime = toTemporalTime.execute(plainTimeLike, Undefined.instance);
            return JSTemporalPlainDateTime.create(getContext(), getRealm(),
                            temporalDateTime.getYear(), temporalDateTime.getMonth(), temporalDateTime.getDay(), plainTime.getHour(), plainTime.getMinute(),
                            plainTime.getSecond(), plainTime.getMillisecond(), plainTime.getMicrosecond(), plainTime.getNanosecond(), temporalDateTime.getCalendar(), this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSTemporalPlainDateTimeObject invalidReceiver(Object thisObj, Object plainTimeLike) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeWithPlainDateNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeWithPlainDateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        final JSTemporalPlainDateTimeObject withPlainDate(JSTemporalPlainDateTimeObject temporalDateTime, Object plainDateLike,
                        @Cached ToTemporalDateNode toTemporalDate,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier,
                        @Cached InlinedBranchProfile errorBranch) {
            JSTemporalPlainDateObject plainDate = toTemporalDate.execute(plainDateLike);
            TruffleString calendar = TemporalUtil.consolidateCalendars(temporalDateTime.getCalendar(), plainDate.getCalendar(), toCalendarIdentifier);
            return JSTemporalPlainDateTime.create(getContext(), getRealm(),
                            plainDate.getYear(), plainDate.getMonth(), plainDate.getDay(), temporalDateTime.getHour(), temporalDateTime.getMinute(),
                            temporalDateTime.getSecond(), temporalDateTime.getMillisecond(), temporalDateTime.getMicrosecond(), temporalDateTime.getNanosecond(), calendar, this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSTemporalPlainDateTimeObject invalidReceiver(Object thisObj, Object plainDateLike) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainDateTimeWithCalendarNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateTimeWithCalendarNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        final JSTemporalPlainDateTimeObject withCalendar(JSTemporalPlainDateTimeObject temporalDateTime, Object calendarLike,
                        @Cached ToTemporalCalendarSlotValueNode toCalendarSlotValue,
                        @Cached InlinedBranchProfile errorBranch) {
            TruffleString calendar = toCalendarSlotValue.execute(calendarLike);
            return JSTemporalPlainDateTime.create(getContext(), getRealm(),
                            temporalDateTime.getYear(), temporalDateTime.getMonth(), temporalDateTime.getDay(),
                            temporalDateTime.getHour(), temporalDateTime.getMinute(), temporalDateTime.getSecond(),
                            temporalDateTime.getMillisecond(), temporalDateTime.getMicrosecond(), temporalDateTime.getNanosecond(), calendar, this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDateTime(thisObj)")
        static JSTemporalPlainDateTimeObject invalidReceiver(Object thisObj, Object calendarParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateTimeExpected();
        }
    }

}
