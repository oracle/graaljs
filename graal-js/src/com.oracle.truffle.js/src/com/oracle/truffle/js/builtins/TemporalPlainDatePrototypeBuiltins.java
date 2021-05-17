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
package com.oracle.truffle.js.builtins;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.AUTO;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTES;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.getLong;

import java.util.Collections;
import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateAddNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateSinceNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToPlainDateTimeNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToStringNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateUntilNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateValueOfNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimePluralRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDate;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalTime;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalPlainDatePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainDatePrototypeBuiltins.TemporalPlainDatePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainDatePrototypeBuiltins();

    protected TemporalPlainDatePrototypeBuiltins() {
        super(JSTemporalPlainDate.PROTOTYPE_NAME, TemporalPlainDatePrototype.class);
    }

    public enum TemporalPlainDatePrototype implements BuiltinEnum<TemporalPlainDatePrototype> {
        // toPlainYearMonth(1),
        // toPlainMonthDay(1),
        getISOFields(0),
        add(1),
        // subtract(1);
        // with(2),
        // withCalendar(1),
        until(2),
        since(2),
        // equals(1),
        toPlainDateTime(1),
        // toZonedDateTime(1),
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
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainDatePrototype builtinEnum) {
        switch (builtinEnum) {
            case add:
                return JSTemporalPlainDateAddNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            // case subtract:
            // return JSTemporalPlainDateSubtractNodeGen.create(context, builtin,
            // args().withThis().fixedArgs(2).createArgumentNodes(context));
// case with:
// return JSTemporalPlainDateWithNodeGen.create(context, builtin,
// args().withThis().fixedArgs(2).createArgumentNodes(context));
            case until:
                return JSTemporalPlainDateUntilNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalPlainDateSinceNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(2).createArgumentNodes(context));
// case round:
// return JSTemporalPlainDateRoundNodeGen.create(context, builtin,
// args().withThis().fixedArgs(1).createArgumentNodes(context));
// case equals:
// return JSTemporalPlainDateEqualsNodeGen.create(context, builtin,
// args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainDateTime:
                return JSTemporalPlainDateToPlainDateTimeNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(1).createArgumentNodes(context));
// case toZonedDate:
// return JSTemporalPlainDateToZonedDateNodeGen.create(context, builtin,
// args().withThis().fixedArgs(1).createArgumentNodes(context));
            case getISOFields:
                return JSTemporalPlainDateGetISOFieldsNodeGen.create(context, builtin,
                                args().withThis().createArgumentNodes(context));
            case toString:
                return JSTemporalPlainDateToStringNodeGen.create(context, builtin,
                                args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalPlainDateToLocaleStringNodeGen.create(context, builtin,
                                args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSTemporalPlainDateValueOfNodeGen.create(context, builtin,
                                args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    // 4.3.10
    public abstract static class JSTemporalPlainDateAdd extends JSBuiltinNode {

        protected JSTemporalPlainDateAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject add(DynamicObject thisObj, DynamicObject temporalDurationLike, Object optParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) thisObj;
            DynamicObject duration = JSTemporalDuration.toLimitedTemporalDuration(temporalDurationLike,
                            Collections.emptySet(), getContext(), isObject, toString, toInt);
            JSTemporalDuration.rejectDurationSign(
                            getLong(duration, YEARS),
                            getLong(duration, MONTHS),
                            getLong(duration, WEEKS),
                            getLong(duration, DAYS),
                            getLong(duration, HOURS),
                            getLong(duration, MINUTES),
                            getLong(duration, SECONDS),
                            getLong(duration, MILLISECONDS),
                            getLong(duration, MICROSECONDS),
                            getLong(duration, NANOSECONDS));
            JSTemporalPlainDateTimePluralRecord balanceResult = JSTemporalDuration.balanceDuration(
                            getLong(duration, DAYS),
                            getLong(duration, HOURS),
                            getLong(duration, MINUTES),
                            getLong(duration, SECONDS),
                            getLong(duration, MILLISECONDS),
                            getLong(duration, MICROSECONDS),
                            getLong(duration, NANOSECONDS), "days", Undefined.instance);
            DynamicObject balancedDuration = JSTemporalDuration.createTemporalDuration(getLong(duration, YEARS),
                            getLong(duration, MONTHS),
                            getLong(duration, WEEKS),
                            balanceResult.getDays(), 0, 0, 0, 0, 0, 0, getContext());
            DynamicObject options = TemporalUtil.getOptionsObject(optParam, getContext());
            return TemporalUtil.calendarDateAdd(date.getCalendar(), thisObj, balancedDuration, options, Undefined.instance);
        }
    }

    public abstract static class JSTemporalPlainDateSince extends JSBuiltinNode {

        protected JSTemporalPlainDateSince(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject since(DynamicObject thisObj, DynamicObject otherObj, DynamicObject optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber) {
            TemporalDate temporalDate = TemporalUtil.requireTemporalDate(thisObj);
            JSTemporalPlainDateObject other = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(otherObj, null, getContext(), isObject, toBoolean, toString);
            if (!TemporalUtil.calenderEquals(temporalDate.getCalendar(), other.getCalendar())) {
                throw Errors.createRangeError("identical calendar expected");
            }

            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext().getRealm(), isObject);
            Set<String> disallowedUnits = TemporalUtil.toSet(HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS);
            String smallestUnit = TemporalUtil.toSmallestTemporalDurationUnit(options, DAYS, disallowedUnits, isObject, toBoolean, toString);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(options, disallowedUnits, DAYS, isObject, toBoolean, toString);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, TRUNC, isObject, toBoolean, toString);
            roundingMode = TemporalUtil.negateTemporalRoundingMode(roundingMode);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, null, false, isObject, toNumber);
            JSTemporalDurationObject result = (JSTemporalDurationObject) TemporalUtil.calendarDateUntil(temporalDate.getCalendar(), other, thisObj, options, Undefined.instance);

            if (DAYS.equals(smallestUnit) && (roundingIncrement == 1)) {
                return JSTemporalDuration.createTemporalDuration(result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), 0, 0, 0, 0, 0, 0, getContext());
            }
            DynamicObject relativeTo = JSTemporalPlainDateTime.createTemporalDateTime(getContext(), temporalDate.getISOYear(), temporalDate.getISOMonth(), temporalDate.getISODay(), 0, 0, 0, 0, 0, 0,
                            temporalDate.getCalendar());
            JSTemporalPlainDateTimePluralRecord result2 = JSTemporalDuration.roundDuration(-result.getYears(), -result.getMonths(), -result.getWeeks(), -result.getDays(), 0, 0, 0, 0, 0, 0,
                            (long) roundingIncrement, smallestUnit,
                            roundingMode, relativeTo, getContext());

            return JSTemporalDuration.createTemporalDuration(-result2.getYears(), -result2.getMonths(), -result2.getWeeks(), -result2.getDays(), 0, 0, 0, 0, 0, 0, getContext());
        }
    }

    public abstract static class JSTemporalPlainDateUntil extends JSBuiltinNode {

        protected JSTemporalPlainDateUntil(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject since(DynamicObject thisObj, DynamicObject otherObj, DynamicObject optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber) {
            TemporalDate temporalDate = TemporalUtil.requireTemporalDate(thisObj);
            JSTemporalPlainDateObject other = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(otherObj, null, getContext(), isObject, toBoolean, toString);
            if (!TemporalUtil.calenderEquals(temporalDate.getCalendar(), other.getCalendar())) {
                throw Errors.createRangeError("identical calendar expected");
            }

            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext().getRealm(), isObject);
            Set<String> disallowedUnits = TemporalUtil.toSet(HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS);
            String smallestUnit = TemporalUtil.toSmallestTemporalDurationUnit(options, DAYS, disallowedUnits, isObject, toBoolean, toString);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(options, disallowedUnits, DAYS, isObject, toBoolean, toString);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, TRUNC, isObject, toBoolean, toString);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, null, false, isObject, toNumber);
            JSTemporalDurationObject result = (JSTemporalDurationObject) TemporalUtil.calendarDateUntil(temporalDate.getCalendar(), other, thisObj, options, Undefined.instance);

            if (!DAYS.equals(smallestUnit) || (roundingIncrement != 1)) {
                DynamicObject relativeTo = JSTemporalPlainDateTime.createTemporalDateTime(getContext(), temporalDate.getISOYear(), temporalDate.getISOMonth(), temporalDate.getISODay(), 0, 0, 0, 0, 0,
                                0, temporalDate.getCalendar());
                JSTemporalPlainDateTimePluralRecord result2 = JSTemporalDuration.roundDuration(result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), 0, 0, 0, 0, 0, 0,
                                (long) roundingIncrement, smallestUnit, roundingMode, relativeTo, getContext());
                return JSTemporalDuration.createTemporalDuration(result2.getYears(), result2.getMonths(), result2.getWeeks(), result2.getDays(), 0, 0, 0, 0, 0, 0, getContext());
            }

            return JSTemporalDuration.createTemporalDuration(result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), 0, 0, 0, 0, 0, 0, getContext());
        }
    }

    public abstract static class JSTemporalPlainDateGetISOFields extends JSBuiltinNode {

        protected JSTemporalPlainDateGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject getISOFields(Object thisObj) {
            TemporalDate dt = TemporalUtil.requireTemporalDate(thisObj);
            DynamicObject obj = JSObjectUtil.createOrdinaryPrototypeObject(getContext().getRealm());
            JSObjectUtil.putDataProperty(getContext(), obj, CALENDAR, dt.getCalendar());
            JSObjectUtil.putDataProperty(getContext(), obj, "isoDay", dt.getISODay());
            JSObjectUtil.putDataProperty(getContext(), obj, "isoMonth", dt.getISOMonth());
            JSObjectUtil.putDataProperty(getContext(), obj, "isoYear", dt.getISOYear());
            return obj;
        }
    }

    public abstract static class JSTemporalPlainDateToString extends JSBuiltinNode {

        protected JSTemporalPlainDateToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(DynamicObject thisObj, DynamicObject optionsParam,
                        @Cached("create()") IsObjectNode isObject) {
            TemporalDate date = TemporalUtil.requireTemporalDate(thisObj);
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext().getRealm(), isObject);
            String showCalendar = TemporalUtil.toShowCalendarOption(options);
            return JSTemporalPlainDate.temporalDateToString(date, showCalendar);
        }
    }

    public abstract static class JSTemporalPlainDateToLocaleString extends JSBuiltinNode {

        protected JSTemporalPlainDateToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toLocaleString(DynamicObject thisObj) {
            TemporalDate date = TemporalUtil.requireTemporalDate(thisObj);
            return JSTemporalPlainDate.temporalDateToString(date, AUTO);
        }
    }

    public abstract static class JSTemporalPlainDateValueOf extends JSBuiltinNode {

        protected JSTemporalPlainDateValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") DynamicObject thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }

    public abstract static class JSTemporalPlainDateToPlainDateTime extends JSBuiltinNode {

        protected JSTemporalPlainDateToPlainDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toPlainDateTime(DynamicObject thisObj, DynamicObject temporalTimeObj,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString) {
            TemporalDate date = TemporalUtil.requireTemporalDate(thisObj);
            if (temporalTimeObj == Undefined.instance) {
                return JSTemporalPlainDateTime.createTemporalDateTime(getContext(), date.getISOYear(), date.getISOMonth(), date.getISODay(), 0, 0, 0, 0, 0, 0, date.getCalendar());
            }
            TemporalTime time = (TemporalTime) JSTemporalPlainTime.toTemporalTime(temporalTimeObj, null, getContext(), isObject, toString);

            return TemporalUtil.createTemporalDateTime(date.getISOYear(), date.getISOMonth(), date.getISODay(), time.getHours(),
                            time.getMinutes(), time.getSeconds(), time.getMilliseconds(), time.getMicroseconds(), time.getNanoseconds(),
                            date.getCalendar(), getContext());

        }
    }

}
