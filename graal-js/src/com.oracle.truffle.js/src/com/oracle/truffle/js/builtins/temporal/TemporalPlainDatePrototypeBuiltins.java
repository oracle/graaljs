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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
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
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
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
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZoneObject;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDate;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalTime;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

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
        toPlainYearMonth(1),
        toPlainMonthDay(1),
        getISOFields(0),
        add(1),
        subtract(1),
        with(2),
        withCalendar(1),
        until(2),
        since(2),
        equals(1),
        toPlainDateTime(1),
        toZonedDateTime(1),
        toString(1),
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

        public final TemporalPlainDatePrototype property;

        public JSTemporalPlainDateGetterNode(JSContext context, JSBuiltin builtin, TemporalPlainDatePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization(guards = "isJSTemporalDate(thisObj)")
        protected Object dateGetter(Object thisObj) {
            TemporalDate temporalDT = (TemporalDate) thisObj;
            switch (property) {
                case calendar:
                    return temporalDT.getCalendar();
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

        @Specialization(guards = "!isJSTemporalDate(thisObj)")
        protected static int error(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalDateExpected();
        }
    }

    // 4.3.10
    public abstract static class JSTemporalPlainDateAdd extends JSBuiltinNode {

        protected JSTemporalPlainDateAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject add(DynamicObject thisObj, Object temporalDurationLike, Object optParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) thisObj;
            JSTemporalDurationRecord duration = TemporalUtil.toLimitedTemporalDuration(temporalDurationLike,
                            Collections.emptySet(), isObject, toString);
            DynamicObject options = TemporalUtil.getOptionsObject(getContext(), optParam);
            JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDuration(getContext(), namesNode,
                            duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), DAY);
            DynamicObject balancedDuration = JSTemporalDuration.createTemporalDuration(getContext(), duration.getYears(), duration.getMonths(), duration.getWeeks(),
                            balanceResult.getDays(), 0, 0, 0, 0, 0, 0);
            return TemporalUtil.calendarDateAdd(date.getCalendar(), thisObj, balancedDuration, options, Undefined.instance);
        }
    }

    public abstract static class JSTemporalPlainDateSubtract extends JSBuiltinNode {

        protected JSTemporalPlainDateSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject subtract(DynamicObject thisObj, Object temporalDurationLike, Object optParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) thisObj;
            JSTemporalDurationRecord duration = TemporalUtil.toLimitedTemporalDuration(temporalDurationLike,
                            Collections.emptySet(), isObject, toString);
            DynamicObject options = TemporalUtil.getOptionsObject(getContext(), optParam);
            JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDuration(getContext(), namesNode,
                            duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), DAY);
            DynamicObject balancedDuration = JSTemporalDuration.createTemporalDuration(getContext(), -duration.getYears(), -duration.getMonths(), -duration.getWeeks(),
                            -balanceResult.getDays(), 0, 0, 0, 0, 0, 0);
            return TemporalUtil.calendarDateAdd(date.getCalendar(), thisObj, balancedDuration, options);
        }
    }

    public abstract static class JSTemporalPlainDateWith extends JSBuiltinNode {

        protected JSTemporalPlainDateWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject with(Object thisObj, Object temporalDateLike, Object optParam,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode nameNode) {
            TemporalDate temporalDate = TemporalUtil.requireTemporalDate(thisObj);
            if (!JSRuntime.isObject(temporalDateLike)) {
                throw Errors.createTypeError("Object expected");
            }
            TemporalUtil.rejectTemporalCalendarType((DynamicObject) temporalDateLike);
            Object calendarProperty = JSObject.get((DynamicObject) temporalDateLike, CALENDAR);
            if (calendarProperty != Undefined.instance) {
                throw TemporalErrors.createTypeErrorUnexpectedCalendar();
            }
            Object timeZoneProperty = JSObject.get((DynamicObject) temporalDateLike, TIME_ZONE);
            if (timeZoneProperty != Undefined.instance) {
                throw TemporalErrors.createTypeErrorUnexpectedTimeZone();
            }
            DynamicObject calendar = temporalDate.getCalendar();
            Set<String> fieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.ARR_DMMCY);
            DynamicObject partialDate = TemporalUtil.preparePartialTemporalFields(getContext(), (DynamicObject) temporalDateLike, fieldNames);
            DynamicObject options = TemporalUtil.getOptionsObject(getContext(), optParam);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), (DynamicObject) temporalDate, fieldNames, TemporalUtil.setEmpty);
            fields = (DynamicObject) TemporalUtil.calendarMergeFields(getContext(), nameNode, calendar, fields, partialDate);
            fields = TemporalUtil.prepareTemporalFields(getContext(), fields, fieldNames, TemporalUtil.setEmpty);
            return TemporalUtil.dateFromFields(calendar, fields, options);
        }
    }

    public abstract static class JSTemporalPlainDateWithCalendar extends JSBuiltinNode {

        protected JSTemporalPlainDateWithCalendar(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject withCalendar(Object thisObj, Object calendarParam) {
            TemporalDate td = TemporalUtil.requireTemporalDate(thisObj);
            DynamicObject calendar = TemporalUtil.toTemporalCalendar(getContext(), calendarParam);
            return TemporalUtil.createTemporalDate(getContext(), td.getYear(), td.getMonth(), td.getDay(), calendar);
        }
    }

    public abstract static class JSTemporalPlainDateSince extends JSBuiltinNode {

        protected JSTemporalPlainDateSince(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject since(Object thisObj, Object otherObj, Object optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            TemporalDate temporalDate = TemporalUtil.requireTemporalDate(thisObj);
            JSTemporalPlainDateObject other = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(otherObj, Undefined.instance, getContext(), isObject, toBoolean, toString);
            if (!TemporalUtil.calendarEquals(temporalDate.getCalendar(), other.getCalendar())) {
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }

            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext(), isObject);
            Set<String> disallowedUnits = TemporalUtil.setTime;
            String smallestUnit = TemporalUtil.toSmallestTemporalUnit(options, disallowedUnits, DAY, toBoolean, toString);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(options, disallowedUnits, AUTO, DAY, toBoolean, toString);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, TRUNC, toBoolean, toString);
            roundingMode = TemporalUtil.negateTemporalRoundingMode(roundingMode);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, null, false, isObject, toNumber);
            JSTemporalDurationObject result = (JSTemporalDurationObject) TemporalUtil.calendarDateUntil(temporalDate.getCalendar(), other, (DynamicObject) temporalDate, options, Undefined.instance);

            if (DAYS.equals(smallestUnit) && (roundingIncrement == 1)) {
                return JSTemporalDuration.createTemporalDuration(getContext(), result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), 0, 0, 0, 0, 0, 0);
            }
            DynamicObject relativeTo = JSTemporalPlainDateTime.create(getContext(), temporalDate.getYear(), temporalDate.getMonth(), temporalDate.getDay(), 0, 0, 0, 0, 0,
                            0, temporalDate.getCalendar());
            JSTemporalDurationRecord result2 = TemporalUtil.roundDuration(getContext(), namesNode, -result.getYears(), -result.getMonths(), -result.getWeeks(), -result.getDays(), 0, 0, 0, 0, 0, 0,
                            (long) roundingIncrement, smallestUnit, roundingMode, relativeTo);

            return JSTemporalDuration.createTemporalDuration(getContext(), -result2.getYears(), -result2.getMonths(), -result2.getWeeks(), -result2.getDays(), 0, 0, 0, 0, 0, 0);
        }
    }

    public abstract static class JSTemporalPlainDateUntil extends JSBuiltinNode {

        protected JSTemporalPlainDateUntil(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject until(Object thisObj, Object otherObj, Object optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            TemporalDate temporalDate = TemporalUtil.requireTemporalDate(thisObj);
            JSTemporalPlainDateObject other = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(otherObj, Undefined.instance, getContext(), isObject, toBoolean, toString);
            if (!TemporalUtil.calendarEquals(temporalDate.getCalendar(), other.getCalendar())) {
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext(), isObject);
            Set<String> disallowedUnits = TemporalUtil.setTime;
            String smallestUnit = TemporalUtil.toSmallestTemporalUnit(options, disallowedUnits, DAY, toBoolean, toString);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(options, disallowedUnits, AUTO, DAY, toBoolean, toString);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, TRUNC, toBoolean, toString);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, null, false, isObject, toNumber);
            JSTemporalDurationObject result = (JSTemporalDurationObject) TemporalUtil.calendarDateUntil(temporalDate.getCalendar(), (DynamicObject) thisObj, (DynamicObject) other, options);

            if (!DAY.equals(smallestUnit) || (roundingIncrement != 1)) {
                DynamicObject relativeTo = JSTemporalPlainDateTime.create(getContext(), temporalDate.getYear(), temporalDate.getMonth(), temporalDate.getDay(), 0, 0, 0, 0,
                                0, 0, temporalDate.getCalendar());
                JSTemporalDurationRecord result2 = TemporalUtil.roundDuration(getContext(), namesNode, result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), 0, 0, 0, 0, 0, 0,
                                (long) roundingIncrement, smallestUnit, roundingMode, relativeTo);
                return JSTemporalDuration.createTemporalDuration(getContext(), result2.getYears(), result2.getMonths(), result2.getWeeks(), result2.getDays(), 0, 0, 0, 0, 0, 0);
            }

            return JSTemporalDuration.createTemporalDuration(getContext(), result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), 0, 0, 0, 0, 0, 0);
        }
    }

    public abstract static class JSTemporalPlainDateGetISOFields extends JSBuiltinNode {

        protected JSTemporalPlainDateGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject getISOFields(Object thisObj) {
            TemporalDate dt = TemporalUtil.requireTemporalDate(thisObj);
            DynamicObject obj = JSOrdinary.create(getContext());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, CALENDAR, dt.getCalendar());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoDay", dt.getDay());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoMonth", dt.getMonth());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoYear", dt.getYear());
            return obj;
        }
    }

    public abstract static class JSTemporalPlainDateToString extends JSBuiltinNode {

        protected JSTemporalPlainDateToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(Object thisObj, Object optionsParam,
                        @Cached("create()") IsObjectNode isObject) {
            TemporalDate date = TemporalUtil.requireTemporalDate(thisObj);
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext(), isObject);
            String showCalendar = TemporalUtil.toShowCalendarOption(options);
            return JSTemporalPlainDate.temporalDateToString(date, showCalendar);
        }
    }

    public abstract static class JSTemporalPlainDateToLocaleString extends JSBuiltinNode {

        protected JSTemporalPlainDateToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toLocaleString(Object thisObj) {
            TemporalDate date = TemporalUtil.requireTemporalDate(thisObj);
            return JSTemporalPlainDate.temporalDateToString(date, AUTO);
        }
    }

    public abstract static class JSTemporalPlainDateValueOf extends JSBuiltinNode {

        protected JSTemporalPlainDateValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }

    public abstract static class JSTemporalPlainDateToPlainDateTime extends JSBuiltinNode {

        protected JSTemporalPlainDateToPlainDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toPlainDateTime(Object thisObj, Object temporalTimeObj,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString) {
            TemporalDate date = TemporalUtil.requireTemporalDate(thisObj);
            if (temporalTimeObj == Undefined.instance) {
                return JSTemporalPlainDateTime.create(getContext(), date.getYear(), date.getMonth(), date.getDay(), 0, 0, 0, 0, 0, 0, date.getCalendar());
            }
            TemporalTime time = (TemporalTime) JSTemporalPlainTime.toTemporalTime(temporalTimeObj, null, getContext(), isObject, toString);

            return TemporalUtil.createTemporalDateTime(getContext(), date.getYear(), date.getMonth(), date.getDay(),
                            time.getHour(), time.getMinute(), time.getSecond(), time.getMillisecond(), time.getMicrosecond(),
                            time.getNanosecond(), date.getCalendar());
        }
    }

    public abstract static class JSTemporalPlainDateToPlainYearMonth extends JSBuiltinNode {

        protected JSTemporalPlainDateToPlainYearMonth(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toPlainYearMonth(Object thisObj) {
            TemporalDate date = TemporalUtil.requireTemporalDate(thisObj);
            DynamicObject calendar = date.getCalendar();
            Set<String> fieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.ARR_MCY);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), (DynamicObject) date, fieldNames, TemporalUtil.setEmpty);
            return TemporalUtil.yearMonthFromFields(calendar, fields, Undefined.instance);
        }
    }

    public abstract static class JSTemporalPlainDateToPlainMonthDay extends JSBuiltinNode {

        protected JSTemporalPlainDateToPlainMonthDay(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toPlainMonthDay(Object thisObj) {
            TemporalDate date = TemporalUtil.requireTemporalDate(thisObj);
            DynamicObject calendar = date.getCalendar();
            Set<String> fieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.ARR_DMC);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), (DynamicObject) date, fieldNames, TemporalUtil.setEmpty);
            return TemporalUtil.monthDayFromFields(calendar, fields, Undefined.instance);
        }
    }

    public abstract static class JSTemporalPlainDateEquals extends JSBuiltinNode {

        protected JSTemporalPlainDateEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public boolean equals(Object thisObj, Object otherParam) {
            TemporalDate temporalDate = TemporalUtil.requireTemporalDate(thisObj);
            JSTemporalPlainDateObject other = (JSTemporalPlainDateObject) TemporalUtil.toTemporalDate(getContext(), otherParam, null);
            if (temporalDate.getYear() != other.getYear()) {
                return false;
            }
            if (temporalDate.getMonth() != other.getMonth()) {
                return false;
            }
            if (temporalDate.getDay() != other.getDay()) {
                return false;
            }
            return TemporalUtil.calendarEquals(temporalDate.getCalendar(), other.getCalendar());
        }
    }

    public abstract static class JSTemporalPlainDateToZonedDateTimeNode extends JSBuiltinNode {

        protected JSTemporalPlainDateToZonedDateTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toZonedDateTime(Object thisObj, Object item,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString) {
            TemporalDate td = TemporalUtil.requireTemporalDate(thisObj);

            JSTemporalTimeZoneObject timeZone;
            Object temporalTime;
            JSTemporalPlainDateTimeObject temporalDateTime;
            if (JSRuntime.isObject(item)) {
                DynamicObject itemObj = (DynamicObject) item;
                Object timeZoneLike = JSObject.get(itemObj, TIME_ZONE);
                if (timeZoneLike == Undefined.instance) {
                    timeZone = (JSTemporalTimeZoneObject) TemporalUtil.toTemporalTimeZone(getContext(), item);
                    temporalTime = Undefined.instance;
                } else {
                    timeZone = (JSTemporalTimeZoneObject) TemporalUtil.toTemporalTimeZone(getContext(), timeZoneLike);
                    temporalTime = JSObject.get(itemObj, "plainTime");
                }
            } else {
                timeZone = (JSTemporalTimeZoneObject) TemporalUtil.toTemporalTimeZone(getContext(), item);
                temporalTime = Undefined.instance;
            }
            if (temporalTime == Undefined.instance) {
                temporalDateTime = TemporalUtil.createTemporalDateTime(getContext(), td.getYear(), td.getMonth(), td.getDay(), 0, 0, 0, 0, 0, 0,
                                td.getCalendar());
            } else {
                JSTemporalPlainTimeObject tt = (JSTemporalPlainTimeObject) JSTemporalPlainTime.toTemporalTime(temporalTime, null, getContext(), isObject, toString);
                temporalDateTime = TemporalUtil.createTemporalDateTime(getContext(), td.getYear(), td.getMonth(), td.getDay(),
                                tt.getHour(), tt.getMinute(), tt.getSecond(), tt.getMillisecond(), tt.getMicrosecond(),
                                tt.getNanosecond(), td.getCalendar());
            }
            JSTemporalInstantObject instant = (JSTemporalInstantObject) TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, temporalDateTime, TemporalConstants.COMPATIBLE);
            return TemporalUtil.createTemporalZonedDateTime(getContext(), instant.getNanoseconds(), timeZone, td.getCalendar());
        }
    }

}
