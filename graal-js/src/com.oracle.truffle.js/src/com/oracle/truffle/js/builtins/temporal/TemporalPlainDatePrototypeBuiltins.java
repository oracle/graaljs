/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;

import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateAddNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateSubtractNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToPlainDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToPlainMonthDayNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToPlainYearMonthNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateUntilNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateValueOfNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateWithCalendarNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateWithNodeGen;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
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
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDayObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonthObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZoneObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalTime;
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

public class TemporalPlainDatePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainDatePrototypeBuiltins.TemporalPlainDatePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainDatePrototypeBuiltins();

    protected TemporalPlainDatePrototypeBuiltins() {
        super(JSTemporalPlainDate.PROTOTYPE_NAME, TemporalPlainDatePrototype.class);
    }

    public enum TemporalPlainDatePrototype implements BuiltinEnum<TemporalPlainDatePrototype> {
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

        // methods
        toPlainYearMonth(0),
        toPlainMonthDay(0),
        getISOFields(0),
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
            return EnumSet.of(calendar, year, month, monthCode, day, dayOfYear, dayOfWeek, weekOfYear, daysInWeek, daysInMonth, daysInYear,
                            monthsInYear, inLeapYear).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainDatePrototype builtinEnum) {
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
                return JSTemporalPlainDateGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

            case add:
                return JSTemporalPlainDateAddNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case subtract:
                return JSTemporalPlainDateSubtractNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case with:
                return JSTemporalPlainDateWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case withCalendar:
                return JSTemporalPlainDateWithCalendarNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case until:
                return JSTemporalPlainDateUntilNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalPlainDateSinceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
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
            case getISOFields:
                return JSTemporalPlainDateGetISOFieldsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toString:
                return JSTemporalPlainDateToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalPlainDateToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSTemporalPlainDateValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainDateGetterNode extends JSBuiltinNode {

        protected final TemporalPlainDatePrototype property;

        protected JSTemporalPlainDateGetterNode(JSContext context, JSBuiltin builtin, TemporalPlainDatePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization
        protected final Object dateGetter(JSTemporalPlainDateObject temporalDT,
                        @Cached("create(getContext())") TemporalCalendarGetterNode calendarGetterNode) {
            switch (property) {
                case calendar:
                    return temporalDT.getCalendar();
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
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    // 4.3.10
    public abstract static class JSTemporalPlainDateAdd extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSTemporalPlainDateObject add(JSTemporalPlainDateObject date, Object temporalDurationLike, Object optParam,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached ToLimitedTemporalDurationNode toLimitedTemporalDurationNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSTemporalDurationRecord duration = toLimitedTemporalDurationNode.execute(temporalDurationLike, TemporalUtil.listEmpty);
            JSDynamicObject options = getOptionsObject(optParam, this, errorBranch, optionUndefined);
            JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDuration(getContext(), namesNode,
                            duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), Unit.DAY);
            JSTemporalDurationObject balancedDuration = JSTemporalDuration.createTemporalDuration(getContext(), duration.getYears(), duration.getMonths(), duration.getWeeks(),
                            balanceResult.getDays(), 0, 0, 0, 0, 0, 0, this, errorBranch);
            return TemporalUtil.calendarDateAdd(date.getCalendar(), date, balancedDuration, options, Undefined.instance);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalPlainDateObject invalidReceiver(Object thisObj, Object temporalDurationLike, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateSubtract extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSTemporalPlainDateObject subtract(JSTemporalPlainDateObject date, Object temporalDurationLike, Object optParam,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached ToLimitedTemporalDurationNode toLimitedTemporalDurationNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSTemporalDurationRecord duration = toLimitedTemporalDurationNode.execute(temporalDurationLike, TemporalUtil.listEmpty);
            JSDynamicObject options = getOptionsObject(optParam, this, errorBranch, optionUndefined);
            JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDuration(getContext(), namesNode,
                            duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), Unit.DAY);
            JSTemporalDurationObject balancedDuration = JSTemporalDuration.createTemporalDuration(getContext(), -duration.getYears(), -duration.getMonths(), -duration.getWeeks(),
                            -balanceResult.getDays(), 0, 0, 0, 0, 0, 0, this, errorBranch);
            return TemporalUtil.calendarDateAdd(date.getCalendar(), date, balancedDuration, options);
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
        protected final JSTemporalPlainDateObject with(JSTemporalPlainDateObject temporalDate, Object temporalDateLike, Object optParam,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached("create(getContext())") TemporalCalendarFieldsNode calendarFieldsNode,
                        @Cached("create(getContext())") TemporalCalendarDateFromFieldsNode dateFromFieldsNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            if (!isObject(temporalDateLike)) {
                errorBranch.enter(this);
                throw Errors.createTypeError("Object expected");
            }
            JSDynamicObject temporalDateLikeObject = TemporalUtil.toJSDynamicObject(temporalDateLike, this, errorBranch);
            TemporalUtil.rejectTemporalCalendarType(temporalDateLikeObject, this, errorBranch);
            Object calendarProperty = JSObject.get(temporalDateLikeObject, CALENDAR);
            if (calendarProperty != Undefined.instance) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorUnexpectedCalendar();
            }
            Object timeZoneProperty = JSObject.get(temporalDateLikeObject, TIME_ZONE);
            if (timeZoneProperty != Undefined.instance) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorUnexpectedTimeZone();
            }
            JSDynamicObject calendar = temporalDate.getCalendar();
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listDMMCY);
            JSDynamicObject partialDate = TemporalUtil.preparePartialTemporalFields(getContext(), temporalDateLikeObject, fieldNames);
            JSDynamicObject options = getOptionsObject(optParam, this, errorBranch, optionUndefined);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), temporalDate, fieldNames, TemporalUtil.listEmpty);
            fields = TemporalUtil.calendarMergeFields(getContext(), calendar, fields,
                            partialDate, namesNode, this, errorBranch);
            fields = TemporalUtil.prepareTemporalFields(getContext(), fields, fieldNames, TemporalUtil.listEmpty);
            return dateFromFieldsNode.execute(calendar, fields, options);
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
        protected final JSTemporalPlainDateObject withCalendar(JSTemporalPlainDateObject date, Object calendarParam,
                        @Cached("create(getContext())") ToTemporalCalendarNode toTemporalCalendar,
                        @Cached InlinedBranchProfile errorBranch) {
            JSDynamicObject calendar = toTemporalCalendar.execute(calendarParam);
            return JSTemporalPlainDate.create(getContext(), date.getYear(), date.getMonth(), date.getDay(), calendar, this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalPlainDateObject invalidReceiver(Object thisObj, Object calendarParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class PlainDateOperation extends JSTemporalBuiltinOperation {
        public PlainDateOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected JSTemporalDurationObject differenceTemporalPlainDate(int sign, JSTemporalPlainDateObject temporalDate, Object otherObj, Object optionsParam, JSToNumberNode toNumber,
                        EnumerableOwnPropertyNamesNode namesNode, ToTemporalDateNode toTemporalDate, JSToStringNode toStringNode, TruffleString.EqualNode equalNode,
                        TemporalRoundDurationNode roundDurationNode, TemporalGetOptionNode getOptionNode,
                        Node node, InlinedBranchProfile errorBranch, InlinedConditionProfile optionUndefined) {
            JSTemporalPlainDateObject other = toTemporalDate.execute(otherObj, Undefined.instance);
            if (!TemporalUtil.calendarEquals(temporalDate.getCalendar(), other.getCalendar(), toStringNode)) {
                errorBranch.enter(node);
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }

            JSDynamicObject options = getOptionsObject(optionsParam, node, errorBranch, optionUndefined);
            List<TruffleString> disallowedUnits = TemporalUtil.listTime;
            Unit smallestUnit = toSmallestTemporalUnit(options, disallowedUnits, DAY, equalNode, getOptionNode, node, errorBranch);
            Unit defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(Unit.DAY, smallestUnit);
            Unit largestUnit = toLargestTemporalUnit(options, disallowedUnits, AUTO, defaultLargestUnit, equalNode, getOptionNode, node, errorBranch);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode, getOptionNode);
            if (sign == TemporalUtil.SINCE) {
                roundingMode = TemporalUtil.negateTemporalRoundingMode(roundingMode);
            }
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, null, false, isObjectNode, toNumber);
            JSDynamicObject untilOptions = TemporalUtil.mergeLargestUnitOption(getContext(), namesNode, options, largestUnit);
            JSTemporalDurationObject result = TemporalUtil.calendarDateUntil(temporalDate.getCalendar(), temporalDate, other, untilOptions, Undefined.instance);

            if (smallestUnit != Unit.DAY || (roundingIncrement != 1)) {
                JSTemporalDurationRecord result2 = roundDurationNode.execute(result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), 0, 0, 0, 0,
                                0, 0, (long) roundingIncrement, smallestUnit, roundingMode, temporalDate);
                return JSTemporalDuration.createTemporalDuration(getContext(), sign * result2.getYears(), sign * result2.getMonths(), sign * result2.getWeeks(), sign * result2.getDays(), 0, 0, 0, 0,
                                0, 0, node, errorBranch);
            }
            return JSTemporalDuration.createTemporalDuration(getContext(), sign * result.getYears(), sign * result.getMonths(), sign * result.getWeeks(), sign * result.getDays(), 0, 0, 0, 0, 0, 0,
                            node, errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateSince extends PlainDateOperation {
        protected JSTemporalPlainDateSince(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSTemporalDurationObject since(JSTemporalPlainDateObject temporalDate, Object otherObj, Object optionsParam,
                        @Cached JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached("create(getContext())") ToTemporalDateNode toTemporalDate,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext())") TemporalRoundDurationNode roundDurationNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            return differenceTemporalPlainDate(TemporalUtil.SINCE, temporalDate, otherObj, optionsParam,
                            toNumber, namesNode, toTemporalDate, toStringNode, equalNode, roundDurationNode, getOptionNode, this, errorBranch, optionUndefined);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalDurationObject invalidReceiver(Object thisObj, Object otherObj, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateUntil extends PlainDateOperation {
        protected JSTemporalPlainDateUntil(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSTemporalDurationObject until(JSTemporalPlainDateObject temporalDate, Object otherObj, Object optionsParam,
                        @Cached JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached("create(getContext())") ToTemporalDateNode toTemporalDate,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext())") TemporalRoundDurationNode roundDurationNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            return differenceTemporalPlainDate(TemporalUtil.UNTIL, temporalDate, otherObj, optionsParam,
                            toNumber, namesNode, toTemporalDate, toStringNode, equalNode, roundDurationNode, getOptionNode, this, errorBranch, optionUndefined);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalDurationObject invalidReceiver(Object thisObj, Object otherObj, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

    public abstract static class JSTemporalPlainDateGetISOFields extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSObject getISOFields(JSTemporalPlainDateObject dt) {
            JSObject obj = JSOrdinary.create(getContext(), getRealm());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, CALENDAR, dt.getCalendar());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_DAY, dt.getDay());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_MONTH, dt.getMonth());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_YEAR, dt.getYear());
            return obj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSObject invalidReceiver(Object thisObj) {
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

    public abstract static class JSTemporalPlainDateValueOf extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static Object valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }

    public abstract static class JSTemporalPlainDateToPlainDateTime extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToPlainDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final JSTemporalPlainDateTimeObject toPlainDateTime(JSTemporalPlainDateObject date, Object temporalTimeObj,
                        @Cached("create(getContext())") ToTemporalTimeNode toTemporalTime,
                        @Cached InlinedBranchProfile errorBranch) {
            if (temporalTimeObj == Undefined.instance) {
                return JSTemporalPlainDateTime.create(getContext(), date.getYear(), date.getMonth(), date.getDay(), 0, 0, 0, 0, 0, 0, date.getCalendar(), this, errorBranch);
            }
            TemporalTime time = toTemporalTime.execute(temporalTimeObj, null);
            return JSTemporalPlainDateTime.create(getContext(), date.getYear(), date.getMonth(), date.getDay(),
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
        protected final JSTemporalPlainYearMonthObject toPlainYearMonth(JSTemporalPlainDateObject date,
                        @Cached("create(getContext())") TemporalYearMonthFromFieldsNode yearMonthFromFieldsNode,
                        @Cached("create(getContext())") TemporalCalendarFieldsNode calendarFieldsNode) {
            JSDynamicObject calendar = date.getCalendar();
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listMCY);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), date, fieldNames, TemporalUtil.listEmpty);
            return yearMonthFromFieldsNode.execute(calendar, fields, Undefined.instance);
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
        protected final JSTemporalPlainMonthDayObject toPlainMonthDay(JSTemporalPlainDateObject date,
                        @Cached("create(getContext())") TemporalMonthDayFromFieldsNode monthDayFromFieldsNode,
                        @Cached("create(getContext())") TemporalCalendarFieldsNode calendarFieldsNode) {
            JSDynamicObject calendar = date.getCalendar();
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listDMC);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), date, fieldNames, TemporalUtil.listEmpty);
            return monthDayFromFieldsNode.execute(calendar, fields, Undefined.instance);
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
                        @Cached("create(getContext())") ToTemporalDateNode toTemporalDate,
                        @Cached JSToStringNode toStringNode) {
            JSTemporalPlainDateObject other = toTemporalDate.execute(otherParam, Undefined.instance);
            if (temporalDate.getYear() != other.getYear()) {
                return false;
            }
            if (temporalDate.getMonth() != other.getMonth()) {
                return false;
            }
            if (temporalDate.getDay() != other.getDay()) {
                return false;
            }
            return TemporalUtil.calendarEquals(temporalDate.getCalendar(), other.getCalendar(), toStringNode);
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
                        @Cached("create(getContext())") ToTemporalTimeNode toTemporalTime,
                        @Cached("create(getContext())") ToTemporalTimeZoneNode toTemporalTimeZone,
                        @Cached InlinedBranchProfile errorBranch) {
            JSTemporalTimeZoneObject timeZone;
            Object temporalTime;
            JSTemporalPlainDateTimeObject temporalDateTime;
            if (isObject(item)) {
                JSDynamicObject itemObj = TemporalUtil.toJSDynamicObject(item, this, errorBranch);
                Object timeZoneLike = JSObject.get(itemObj, TIME_ZONE);
                if (timeZoneIsUndefined.profile(this, timeZoneLike == Undefined.instance)) {
                    timeZone = (JSTemporalTimeZoneObject) toTemporalTimeZone.execute(item);
                    temporalTime = Undefined.instance;
                } else {
                    timeZone = (JSTemporalTimeZoneObject) toTemporalTimeZone.execute(timeZoneLike);
                    temporalTime = JSObject.get(itemObj, TemporalConstants.PLAIN_TIME);
                }
            } else {
                timeZone = (JSTemporalTimeZoneObject) toTemporalTimeZone.execute(item);
                temporalTime = Undefined.instance;
            }
            if (timeIsUndefined.profile(this, temporalTime == Undefined.instance)) {
                temporalDateTime = JSTemporalPlainDateTime.create(getContext(), td.getYear(), td.getMonth(), td.getDay(), 0, 0, 0, 0, 0, 0,
                                td.getCalendar(), this, errorBranch);
            } else {
                JSTemporalPlainTimeObject tt = toTemporalTime.execute(temporalTime, null);
                temporalDateTime = JSTemporalPlainDateTime.create(getContext(), td.getYear(), td.getMonth(), td.getDay(),
                                tt.getHour(), tt.getMinute(), tt.getSecond(), tt.getMillisecond(), tt.getMicrosecond(),
                                tt.getNanosecond(), td.getCalendar(), this, errorBranch);
            }
            JSTemporalInstantObject instant = TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, temporalDateTime, Disambiguation.COMPATIBLE);
            return JSTemporalZonedDateTime.create(getContext(), getRealm(), instant.getNanoseconds(), timeZone, td.getCalendar());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainDate(thisObj)")
        protected static JSTemporalZonedDateTimeObject invalidReceiver(Object thisObj, Object item) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
    }

}
