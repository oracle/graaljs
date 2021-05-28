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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH_CODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;

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
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeUntilNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeValueOfNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltinsFactory.JSTemporalPlainDateTimeWithNodeGen;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
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
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimePluralRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDateTime;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
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
        // withPlainTime(1),
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
        // toPlainYearMonth(0),
        // toPlainMonthDay(0),
        toPlainTime(0),
        // toZonedDateTime(1),
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
// case toZonedDateTime:
// return JSTemporalPlainDateTimeToZonedDateTimeNodeGen.create(context, builtin,
// args().withThis().fixedArgs(1).createArgumentNodes(context));
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
                    return temporalDT.getHours();
                case minute:
                    return temporalDT.getMinutes();
                case second:
                    return temporalDT.getSeconds();
                case millisecond:
                    return temporalDT.getMilliseconds();
                case microsecond:
                    return temporalDT.getMicroseconds();
                case nanosecond:
                    return temporalDT.getNanoseconds();
                case year:
                    return temporalDT.getISOYear();
                case month:
                    return temporalDT.getISOMonth();
                case day:
                    return temporalDT.getISODay();
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

        @Specialization(guards = "isJSTemporalDateTime(thisObj)")
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
        public DynamicObject add(DynamicObject thisObj, DynamicObject temporalDurationLike, Object optParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            TemporalDateTime dateTime = TemporalUtil.requireTemporalDateTime(thisObj);
            JSTemporalPlainDateTimeRecord duration = JSTemporalDuration.toLimitedTemporalDuration(temporalDurationLike,
                            Collections.emptySet(), isObject, toString, toInt);
            JSTemporalDuration.rejectDurationSign(
                            duration.getYear(), duration.getMonth(), duration.getWeeks(), duration.getDay(),
                            duration.getHour(), duration.getMinute(), duration.getSecond(),
                            duration.getMillisecond(), duration.getMicrosecond(), duration.getNanosecond());
            DynamicObject options = TemporalUtil.getOptionsObject(optParam, getContext());
            JSTemporalPlainDateTimeRecord result = JSTemporalPlainDateTime.addDateTime(
                            dateTime.getISOYear(), dateTime.getISOMonth(), dateTime.getISODay(),
                            dateTime.getHours(), dateTime.getMinutes(), dateTime.getSeconds(),
                            dateTime.getMilliseconds(), dateTime.getMicroseconds(), dateTime.getNanoseconds(),
                            dateTime.getCalendar(),
                            duration.getYear(), duration.getMonth(), duration.getWeeks(), duration.getDay(),
                            duration.getHour(), duration.getMinute(), duration.getSecond(),
                            duration.getMillisecond(), duration.getMicrosecond(), duration.getNanosecond(),
                            options,
                            getContext());

            return JSTemporalPlainDateTime.createTemporalDateTime(getContext(),
                            result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(),
                            result.getNanosecond(),
                            dateTime.getCalendar());
        }
    }

    public abstract static class JSTemporalPlainDateTimeSince extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeSince(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization
        public DynamicObject since(DynamicObject thisObj, Object other, Object optParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            // TODO
            return Undefined.instance;
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
            JSObjectUtil.putDataProperty(getContext(), obj, CALENDAR, dt.getCalendar());
            JSObjectUtil.putDataProperty(getContext(), obj, "isoDay", dt.getISODay());
            JSObjectUtil.putDataProperty(getContext(), obj, "isoHour", dt.getHours());
            JSObjectUtil.putDataProperty(getContext(), obj, "isoMonth", dt.getISOMonth());
            JSObjectUtil.putDataProperty(getContext(), obj, "isoMicrosecond", dt.getMicroseconds());
            JSObjectUtil.putDataProperty(getContext(), obj, "isoMillisecond", dt.getMilliseconds());
            JSObjectUtil.putDataProperty(getContext(), obj, "isoMinute", dt.getMinutes());
            JSObjectUtil.putDataProperty(getContext(), obj, "isoNanosecond", dt.getNanoseconds());
            JSObjectUtil.putDataProperty(getContext(), obj, "isoSecond", dt.getSeconds());
            JSObjectUtil.putDataProperty(getContext(), obj, "isoYear", dt.getISOYear());
            return obj;
        }
    }

    // 4.3.11
    public abstract static class JSTemporalPlainDateTimeSubtract extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject subtract(DynamicObject thisObj, DynamicObject temporalDurationLike, Object optParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            TemporalDateTime dateTime = TemporalUtil.requireTemporalDateTime(thisObj);
            JSTemporalPlainDateTimeRecord duration = JSTemporalDuration.toLimitedTemporalDuration(temporalDurationLike,
                            Collections.emptySet(), isObject, toString, toInt);
            JSTemporalDuration.rejectDurationSign(
                            duration.getYear(), duration.getMonth(), duration.getWeeks(), duration.getDay(),
                            duration.getHour(), duration.getMinute(), duration.getSecond(),
                            duration.getMillisecond(), duration.getMicrosecond(), duration.getNanosecond());
            DynamicObject options = TemporalUtil.getOptionsObject(optParam, getContext());
            JSTemporalPlainDateTimeRecord result = JSTemporalPlainDateTime.addDateTime(
                            dateTime.getISOYear(), dateTime.getISOMonth(), dateTime.getISODay(),
                            dateTime.getHours(), dateTime.getMinutes(), dateTime.getSeconds(),
                            dateTime.getMilliseconds(), dateTime.getMicroseconds(), dateTime.getNanoseconds(),
                            dateTime.getCalendar(),
                            -duration.getYear(), -duration.getMonth(), -duration.getWeeks(), -duration.getDay(),
                            -duration.getHour(), -duration.getMinute(), -duration.getSecond(),
                            -duration.getMillisecond(), -duration.getMicrosecond(), -duration.getNanosecond(),
                            options,
                            getContext());

            return JSTemporalPlainDateTime.createTemporalDateTime(getContext(),
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
        protected String toString(DynamicObject thisObj, Object optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            TemporalDateTime dt = TemporalUtil.requireTemporalDateTime(thisObj);
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext(), isObject);
            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecision(options);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, "trunc", toBoolean, toString);
            String showCalendar = TemporalUtil.toShowCalendarOption(options);
            JSTemporalPlainDateTimePluralRecord result = JSTemporalPlainDateTime.roundISODateTime(
                            dt.getISOYear(), dt.getISOMonth(), dt.getISODay(),
                            dt.getHours(), dt.getMinutes(), dt.getSeconds(),
                            dt.getMilliseconds(), dt.getMicroseconds(), dt.getNanoseconds(),
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
        public String toLocaleString(DynamicObject thisObj) {
            TemporalDateTime dt = TemporalUtil.requireTemporalDateTime(thisObj);
            return JSTemporalPlainDateTime.temporalDateTimeToString(dt.getISOYear(), dt.getISOMonth(), dt.getISODay(),
                            dt.getHours(), dt.getMinutes(), dt.getSeconds(),
                            dt.getMilliseconds(), dt.getMicroseconds(), dt.getNanoseconds(),
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
                throw Errors.createTypeError("Unexpected calendar property");
            }
            Object timeZoneProperty = JSObject.get(temporalDTObj, TIME_ZONE);
            if (timeZoneProperty != Undefined.instance) {
                throw Errors.createTypeError("Unexpected timeZone property");
            }
            DynamicObject calendar = dateTime.getCalendar();
            Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{DAY, HOUR, MICROSECOND, MILLISECOND, MINUTE, MONTH, MONTH_CODE, NANOSECOND, SECOND, YEAR}, getContext());
            DynamicObject partialDateTime = TemporalUtil.preparePartialTemporalFields(temporalDTObj, fieldNames, getContext());
            DynamicObject options = TemporalUtil.getOptionsObject(optParam, getContext(), isObject);
            Object fields = TemporalUtil.prepareTemporalFields((DynamicObject) dateTime, fieldNames, TemporalUtil.toSet(), getContext());
            fields = TemporalUtil.calendarMergeFields(calendar, (DynamicObject) fields, partialDateTime, namesNode, getContext());
            fields = TemporalUtil.prepareTemporalFields((DynamicObject) fields, fieldNames, TemporalUtil.toSet(), getContext());
            JSTemporalPlainDateTimeRecord result = TemporalUtil.interpretTemporalDateTimeFields(calendar, (DynamicObject) fields, options);
            assert TemporalUtil.validateISODateTime(result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                            result.getMicrosecond(), result.getNanosecond());
            return TemporalUtil.createTemporalDateTime(result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                            result.getMicrosecond(), result.getNanosecond(), calendar, getContext());
        }
    }

    public abstract static class JSTemporalPlainDateTimeUntil extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeUntil(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization
        public DynamicObject until(DynamicObject thisObj, Object other, Object optParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            // TODO
            return Undefined.instance;
        }
    }

    public abstract static class JSTemporalPlainDateTimeEquals extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSTemporalDateTime(otherObj)")
        protected static boolean equalsOtherObj(DynamicObject thisObj, DynamicObject otherObj) {
            TemporalDateTime temporalDT = TemporalUtil.requireTemporalDateTime(thisObj);
            return equalsIntl(temporalDT, (TemporalDateTime) otherObj);
        }

        @Specialization(guards = "!isJSTemporalDateTime(other)")
        protected boolean equalsGeneric(DynamicObject thisObj, Object other) {
            TemporalDateTime one = TemporalUtil.requireTemporalDateTime(thisObj);
            TemporalDateTime two = (TemporalDateTime) JSTemporalPlainDateTime.toTemporalDateTime(other, null, getContext());
            return equalsIntl(one, two);
        }

        private static boolean equalsIntl(TemporalDateTime one, TemporalDateTime two) {
            int result = JSTemporalPlainDateTime.compareISODateTime(
                            one.getISOYear(), one.getISOMonth(), one.getISODay(),
                            one.getHours(), one.getMinutes(), one.getSeconds(),
                            one.getMilliseconds(), one.getMicroseconds(), one.getNanoseconds(),
                            two.getISOYear(), two.getISOMonth(), two.getISODay(),
                            two.getHours(), two.getMinutes(), two.getSeconds(),
                            two.getMilliseconds(), two.getMicroseconds(), two.getNanoseconds());
            if (result != 0) {
                return false;
            } else {
                return TemporalUtil.calendarEquals(one.getCalendar(), two.getCalendar());
            }
        }
    }

    public abstract static class JSTemporalPlainDateTimeRound extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization
        public DynamicObject round(DynamicObject thisObj, Object other) {
            // TODO
            return Undefined.instance;
        }
    }

    public abstract static class JSTemporalPlainDateTimeToPlainTimeNode extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeToPlainTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toPlainTime(Object thisObj) {
            TemporalDateTime dt = TemporalUtil.requireTemporalDateTime(thisObj);
            return TemporalUtil.createTemporalTime(getContext(), dt.getHours(), dt.getMinutes(), dt.getSeconds(), dt.getMilliseconds(), dt.getMicroseconds(), dt.getNanoseconds());
        }
    }

    public abstract static class JSTemporalPlainDateTimeToPlainDateNode extends JSBuiltinNode {

        protected JSTemporalPlainDateTimeToPlainDateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject toPlainDate(Object thisObj) {
            TemporalDateTime dt = TemporalUtil.requireTemporalDateTime(thisObj);
            return TemporalUtil.createTemporalDate(getContext(), dt.getISOYear(), dt.getISOMonth(), dt.getISODay(), dt.getCalendar());
        }
    }

}
