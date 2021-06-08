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
package com.oracle.truffle.js.runtime.util;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.ALWAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.AUTO;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CEIL;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.JAPANESE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.COMPATIBLE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CONSTRAIN;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ERA;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ERA_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.FLOOR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.GREGORY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ISO8601;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.LARGEST_UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTES;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH_CODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NEVER;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.OFFSET;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.OVERFLOW;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.REJECT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEK;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendarObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalNanosecondsDaysRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDayObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonth;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonthObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalRelativeDateRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDate;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalTime;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class TemporalUtil {

    // TODO for every call to requireTemporalTime etc all, make sure we pass in an Object; if we are
    // passing an DynamicObject, chances are we have an UnsupportedSpecializationException as we
    // don't accept a non-object type

    // TODO check the records we have and unify them

    private static final Function<Object, Object> toIntegerOrInfinity = (argument -> (long) JSToDoubleNode.create().executeDouble(argument));
    private static final Function<Object, Object> toPositiveIntegerOrInfinity = TemporalUtil::toPositiveIntegerOrInfinity;
    private static final Function<Object, Object> toInteger = (argument -> (long) JSToIntegerAsLongNode.create().executeLong(argument));
    private static final Function<Object, Object> toString = (argument -> JSToStringNode.create().executeString(argument));

    // private static final Set<String> singularUnits = toSet(YEAR, MONTH, WEEK, DAY, HOUR, MINUTE,
    // SECOND,
    // MILLISECOND, MICROSECOND, NANOSECOND);
    private static final Set<String> pluralUnits = toSet(YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS,
                    MILLISECONDS, MICROSECONDS, NANOSECONDS);
    private static final Map<String, String> pluralToSingular = toMap(
                    new String[]{YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS},
                    new String[]{YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND});
    // private static final Map<String, String> singularToPlural = toMap(
    // new String[]{YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND,
    // NANOSECOND},
    // new String[]{YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS,
    // NANOSECONDS});
    @SuppressWarnings("unchecked") private static final Map<String, Function<Object, Object>> temporalFieldConversion = toMap(
                    new String[]{YEAR, MONTH, MONTH_CODE, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND, YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS,
                                    MICROSECONDS, NANOSECONDS, OFFSET, ERA, ERA_YEAR},
                    new Function[]{toIntegerOrInfinity, toPositiveIntegerOrInfinity, toString, toIntegerOrInfinity, toIntegerOrInfinity, toIntegerOrInfinity, toIntegerOrInfinity, toIntegerOrInfinity,
                                    toIntegerOrInfinity, toIntegerOrInfinity,
                                    toIntegerOrInfinity, toIntegerOrInfinity, toIntegerOrInfinity, toIntegerOrInfinity, toIntegerOrInfinity, toIntegerOrInfinity, toIntegerOrInfinity,
                                    toIntegerOrInfinity, toIntegerOrInfinity, toIntegerOrInfinity, toString, toString, toInteger});
    private static final Map<String, Object> temporalFieldDefaults = toMap(
                    new String[]{YEAR, MONTH, MONTH_CODE, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND, YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS,
                                    MICROSECONDS, NANOSECONDS, OFFSET, ERA, ERA_YEAR},
                    new Object[]{Undefined.instance, Undefined.instance, Undefined.instance, Undefined.instance, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Undefined.instance, Undefined.instance,
                                    Undefined.instance});
    public static final Set<String> setYMWD = TemporalUtil.toSet(YEAR, MONTH, WEEK, DAY);

    public static final String[] TIME_LIKE_PROPERTIES = new String[]{HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND};

    public static final String[] DURATION_PROPERTIES = new String[]{DAYS, HOURS, MICROSECONDS, MILLISECONDS, MINUTES, MONTHS, NANOSECONDS, SECONDS, WEEKS, YEARS
    };

    // 13.1
    public static DynamicObject getOptionsObject(Object options, JSContext ctx, IsObjectNode isObject) {
        if (options == Undefined.instance) {
            return JSOrdinary.createWithNullPrototype(ctx);
        }
        if (isObject.executeBoolean(options)) {
            return (DynamicObject) options;
        }
        throw TemporalErrors.createTypeErrorOptions();
    }

    public static DynamicObject getOptionsObject(Object opt, JSContext ctx) {
        if (opt == Undefined.instance) {
            return JSOrdinary.createWithNullPrototype(ctx);
        }
        if (JSRuntime.isObject(opt)) {
            return (DynamicObject) opt;
        }
        throw TemporalErrors.createTypeErrorOptions();
    }

    // 13.2
    public static Object getOption(DynamicObject options, String property, String type, Set<?> values, Object fallback, JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        assert JSRuntime.isObject(options);
        Object value = JSObject.get(options, property);
        if (value == Undefined.instance) {
            return fallback;
        }
        assert type.equals("boolean") || type.equals("string");
        if (type.equals("boolean")) {
            value = toBoolean.executeBoolean(value);
        } else if (type.equals("string")) {
            value = toStringNode.executeString(value);
        }
        if (value != Undefined.instance && !Boundaries.setContains(values, value)) {
            throw TemporalErrors.createRangeErrorOptionsNotContained(values, value);
        }
        return value;
    }

    // 13.3
    public static double defaultNumberOptions(Object value, double minimum, double maximum, double fallback,
                    JSToNumberNode toNumber) {
        if (value == Undefined.instance) {
            return fallback;
        }
        double numberValue = Boundaries.doubleValue(toNumber.executeNumber(value));
        if (Double.isNaN(numberValue) || numberValue < minimum || numberValue > maximum) {
            throw Errors.createRangeError("Numeric value out of range.");
        }
        return Math.floor(numberValue);
    }

    // 13.4
    public static double getNumberOption(DynamicObject options, String property, double minimum, double maximum,
                    double fallback, IsObjectNode isObject,
                    JSToNumberNode numberNode) {
        assert isObject.executeBoolean(options);
        Object value = JSObject.get(options, property);
        return defaultNumberOptions(value, minimum, maximum, fallback, numberNode);
    }

    // 13.5
    @TruffleBoundary
    public static Object getStringOrNumberOption(DynamicObject options, String property, Set<String> stringValues,
                    double minimum, double maximum, Object fallback) {
        assert JSRuntime.isObject(options);
        Object value = JSObject.get(options, property);
        if (value == Undefined.instance) {
            return fallback;
        }
        if (value instanceof Number) {
            double numberValue = ((Number) value).doubleValue();
            if (Double.isNaN(numberValue) || numberValue < minimum || numberValue > maximum) {
                throw Errors.createRangeError("Numeric value out of range.");
            }
            return Math.floor(numberValue);
        }
        value = JSRuntime.toString(value);
        if (stringValues != null && !stringValues.contains(value)) {
            throw Errors.createRangeError("Given string value is not in string values");
        }
        return value;
    }

    // 13.8
    public static String toTemporalOverflow(DynamicObject options, JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        return (String) getOption(options, "overflow", "string", prepareSet(), CONSTRAIN, toBoolean, toStringNode);
    }

    @TruffleBoundary
    private static Set<?> prepareSet() {
        Set<Object> set = new HashSet<>();
        set.add(CONSTRAIN);
        set.add(REJECT);
        return set;
    }

    // 13.11
    public static String toTemporalRoundingMode(DynamicObject options, String fallback, JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        return (String) getOption(options, "roundingMode", "string", toSet("ceil", "floor", "trunc", "halfExpand"), fallback, toBoolean, toStringNode);
    }

    // 13.17
    public static double toTemporalRoundingIncrement(DynamicObject options, Double dividend, boolean inclusive,
                    IsObjectNode isObject,
                    JSToNumberNode toNumber) {
        double maximum;
        double dDividend = Double.NaN;
        if (dividend == null) {
            maximum = Double.POSITIVE_INFINITY;
        } else {
            dDividend = dividend.doubleValue();
            if (inclusive) {
                maximum = dDividend;
            } else if (dDividend > 1) {
                maximum = dDividend - 1;
            } else {
                maximum = 1;
            }
        }

        double increment = getNumberOption(options, "roundingIncrement", 1, maximum, 1, isObject, toNumber);
        if (dividend != null && dDividend % increment != 0) {
            throw Errors.createRangeError("Increment out of range.");
        }
        return increment;
    }

    @TruffleBoundary
    public static JSTemporalPrecisionRecord toSecondsStringPrecision(DynamicObject options, JSToBooleanNode toBooleanNode, JSToStringNode toStringNode) {
        String smallestUnit = toSmallestTemporalUnit(options, toSet(YEAR, MONTH, WEEK, DAY, HOUR), null, toBooleanNode, toStringNode);

        if (MINUTE.equals(smallestUnit)) {
            return JSTemporalPrecisionRecord.create(MINUTE, MINUTE, 1);
        } else if (SECOND.equals(smallestUnit)) {
            return JSTemporalPrecisionRecord.create(0, SECOND, 1);
        } else if (MILLISECOND.equals(smallestUnit)) {
            return JSTemporalPrecisionRecord.create(3, MILLISECOND, 1);
        } else if (MICROSECOND.equals(smallestUnit)) {
            return JSTemporalPrecisionRecord.create(6, MICROSECOND, 1);
        } else if (NANOSECOND.equals(smallestUnit)) {
            return JSTemporalPrecisionRecord.create(9, NANOSECOND, 1);
        }

        assert smallestUnit == null;

        Object digits = TemporalUtil.getStringOrNumberOption(options, "fractionalSecondDigits", TemporalUtil.toSet(AUTO), 0, 9, AUTO);
        if (digits.equals(AUTO)) {
            return JSTemporalPrecisionRecord.create(AUTO, NANOSECOND, 1);
        }
        int iDigit = ((Number) digits).intValue();

        if (iDigit == 0) {
            return JSTemporalPrecisionRecord.create(0, SECOND, 1);
        }
        if (iDigit == 1 || iDigit == 2 || iDigit == 3) {
            return JSTemporalPrecisionRecord.create(digits, MILLISECOND, Math.pow(10, 3 - TemporalUtil.toLong(digits)));
        }
        if (iDigit == 4 || iDigit == 5 || iDigit == 6) {
            return JSTemporalPrecisionRecord.create(digits, MICROSECOND, Math.pow(10, 6 - TemporalUtil.toLong(digits)));
        }
        assert iDigit == 7 || iDigit == 8 || iDigit == 9;
        return JSTemporalPrecisionRecord.create(digits, NANOSECOND, Math.pow(10, 9 - TemporalUtil.toLong(digits)));
    }

    // TODO this whole method should be unnecessary
    @TruffleBoundary
    private static long toLong(Object digits) {
        if (digits instanceof Number) {
            return ((Number) digits).longValue();
        }
        return JSRuntime.toNumber(digits).longValue();
    }

    // 13.21
    public static String toLargestTemporalUnit(DynamicObject normalizedOptions, Set<String> disallowedUnits, String fallback, String autoValue, JSToBooleanNode toBoolean,
                    JSToStringNode toStringNode) {
        assert !disallowedUnits.contains(fallback) && !disallowedUnits.contains(AUTO);
        String largestUnit = (String) getOption(normalizedOptions, "largestUnit", "string", toSet(
                        "auto", "year", "years", "month", "months", "week", "weeks", "day", "days", "hour",
                        "hours", "minute", "minutes", "second", "seconds", "millisecond", "milliseconds", "microsecond",
                        "microseconds", "nanosecond", "nanoseconds"), fallback, toBoolean, toStringNode);
        if (largestUnit.equals(AUTO) && autoValue != null) {
            return autoValue;
        }
        if (Boundaries.setContains(pluralUnits, largestUnit)) {
            largestUnit = Boundaries.mapGet(pluralToSingular, largestUnit);
        }
        if (Boundaries.setContains(disallowedUnits, largestUnit)) {
            throw Errors.createRangeError("Largest unit is not allowed.");
        }
        return largestUnit;
    }

    // 13.22
    public static String toSmallestTemporalUnit(DynamicObject normalizedOptions, Set<String> disallowedUnits, String fallback, JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        String smallestUnit = (String) getOption(normalizedOptions, "smallestUnit", "string", toSet("year", "years", "month", "months", "weeks", "day", "days", "hour",
                        "hours", "minute", "minutes", "second", "seconds", "millisecond", "milliseconds", "microsecond",
                        "microseconds", "nanosecond", "nanoseconds"), fallback, toBoolean, toStringNode);
        if (Boundaries.setContains(pluralUnits, smallestUnit)) {
            smallestUnit = Boundaries.mapGet(pluralToSingular, smallestUnit);
        }
        if (Boundaries.setContains(disallowedUnits, smallestUnit)) {
            throw Errors.createRangeError("Smallest unit not allowed.");
        }
        return smallestUnit;
    }

    // 13.24
    public static String toTemporalDurationTotalUnit(DynamicObject normalizedOptions, JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        String unit = (String) getOption(normalizedOptions, "unit", "string", toSet(YEARS, YEAR, MONTHS, MONTH, WEEKS,
                        DAYS, DAY, HOURS, HOUR, MINUTES, MINUTE, SECONDS, SECOND, MILLISECONDS, MILLISECOND, MICROSECONDS,
                        MICROSECOND, NANOSECONDS, NANOSECONDS),
                        null, toBoolean, toStringNode);
        if (Boundaries.setContains(pluralUnits, unit)) {
            unit = Boundaries.mapGet(pluralToSingular, unit);
        }
        return unit;
    }

    // 13.26
    @SuppressWarnings("unused")
    @TruffleBoundary
    public static DynamicObject toRelativeTemporalObject(DynamicObject options, JSContext ctx) {
        Object value = JSObject.get(options, "relativeTo");
        if (value == Undefined.instance) {
            return Undefined.instance;
        }
        JSTemporalDateTimeRecord result = null;
        Object timeZone = null;
        DynamicObject calendar = null;
        Object offset = null;
        if (JSRuntime.isObject(value)) {
            DynamicObject valueObj = (DynamicObject) value;
            if (value instanceof JSTemporalPlainDateTimeObject) {
                return (DynamicObject) value;
            }
            // if (value instanceof JSTemporalPlainDateTimeObject) {
            // return (DynamicObject) value;
            // }
            if (value instanceof JSTemporalPlainDateObject) {
                JSTemporalPlainDateObject pd = (JSTemporalPlainDateObject) valueObj;
                return JSTemporalPlainDateTime.createTemporalDateTime(ctx, pd.getISOYear(), pd.getISOMonth(), pd.getISODay(), 0, 0, 0, 0, 0, 0, pd.getCalendar());
            }
            calendar = TemporalUtil.getTemporalCalendarWithISODefault(valueObj, ctx);
            Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{DAY, MONTH, MONTH_CODE, YEAR}, ctx);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(valueObj, fieldNames, new HashSet<>(), ctx);

            DynamicObject dateOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(ctx, dateOptions, OVERFLOW, CONSTRAIN);
            result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, dateOptions);
            offset = JSObject.get(valueObj, OFFSET);
            timeZone = JSObject.get(valueObj, TIME_ZONE);
        } else {
            String string = JSRuntime.toString(value);
            result = parseISODateTime(string, ctx);
            calendar = toTemporalCalendarWithISODefault(result.getCalendar(), ctx);
            offset = result.getTimeZoneOffset();
            timeZone = result.getTimeZoneIANAName();
        }
        if (timeZone != Undefined.instance) {
            timeZone = toTemporalTimeZone(timeZone);
            Object offsetNs = 0;
            if (offset != Undefined.instance) {
                offset = JSRuntime.toString(offset);
                offsetNs = parseTimeZoneOffsetString(offset);
            } else {
                offsetNs = Undefined.instance;
            }
            long epochNanoseconds = interpretISODateTimeOffset(
                            result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                            result.getMicrosecond(), result.getNanosecond(),
                            offsetNs, timeZone, COMPATIBLE, REJECT);
            return createTemporalZonedDateTime(epochNanoseconds, timeZone, calendar);
        }
        return JSTemporalPlainDateTime.createTemporalDateTime(ctx,
                        result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                        result.getMicrosecond(), result.getNanosecond(),
                        calendar);
    }

    @SuppressWarnings("unused")
    private static DynamicObject createTemporalZonedDateTime(long epochNanoseconds, Object timeZone, DynamicObject calendar) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    @SuppressWarnings("unused")
    private static long interpretISODateTimeOffset(long long1, long long2, long long3, long long4, long long5, long long6, long long7, long long8, long long9, Object offsetNs, Object timeZone,
                    String compatible, String reject) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    @SuppressWarnings("unused")
    private static Object parseTimeZoneOffsetString(Object offset) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    public static JSTemporalDateTimeRecord parseTemporalMonthDayString(String string, JSContext ctx) {
        JSTemporalDateTimeRecord res = parseISODateTime(string, ctx);
        // TODO this is not according to the spec, yet
        return res;
    }

    private static JSTemporalDateTimeRecord parseTemporalYearMonthString(String string, JSContext ctx) {
        JSTemporalDateTimeRecord res = parseISODateTime(string, ctx);
        // TODO this is not according to the spec, yet
        return res;
    }

    // TODO this needs to be improved!
    @TruffleBoundary
    private static JSTemporalDateTimeRecord parseISODateTime(String string, JSContext ctx) {
        JSTemporalDateTimeRecord jsDate = tryJSDateParser(string, ctx);
        if (jsDate != null) {
            return jsDate;
        }
        JSTemporalDateTimeRecord instant = tryParseInstant(string);
        if (instant != null) {
            return instant;
        }
        JSTemporalDateTimeRecord date = tryParseDate(string);
        if (date != null) {
            return date;
        }
        JSTemporalDateTimeRecord date2 = tryParseDate2(string);
        if (date2 != null) {
            return date2;
        }
        JSTemporalDateTimeRecord time = tryParseTime(string);
        if (time != null) {
            return time;
        }
        JSTemporalDateTimeRecord time2 = tryParseTime2(string);
        if (time2 != null) {
            return time2;
        }
        JSTemporalDateTimeRecord time3 = tryParseTime3(string);
        if (time3 != null) {
            return time3;
        }
        throw Errors.createRangeError("cannot parse the ISO date time string");
    }

    private static JSTemporalDateTimeRecord tryJSDateParser(String string, JSContext ctx) {
        Integer[] f = ctx.getEvaluator().parseDate(ctx.getRealm(), string, true);
        if (f != null) {
            int millis = get(f, 6);
            int ms = 0;
            int mics = 0;
            int ns = 0;
            if (millis < 1000) {
                ms = millis;
            } else if (1000 <= millis && millis < 1_000_000) {
                if (millis < 10_000) {
                    millis *= 100;
                } else if (millis < 100_000) {
                    millis *= 10;
                }
                mics = millis % 1000;
                ms = (millis - mics) / 1000;
            } else {
                if (millis < 10_000_000) {
                    millis *= 100;
                } else if (millis < 100_000_000) {
                    millis *= 10;
                }
                ns = millis % 1000;
                millis /= 1000;
                mics = millis % 1000;
                millis /= 1000;
                ms = millis;
            }

            int month = get(f, 1) + 1; // +1 !
            return JSTemporalDateTimeRecord.create(get(f, 0), month, get(f, 2), get(f, 3), get(f, 4), get(f, 5), ms, mics, ns);
        }
        return null;
    }

    private static int get(Integer[] f, int i) {
        return (f.length > i && f[i] != null) ? f[i] : 0;
    }

    @SuppressWarnings("deprecation")
    @TruffleBoundary
    private static JSTemporalDateTimeRecord tryParseInstant(String string) {
        try {
            java.util.Date d = Date.from(Instant.parse(string));
            return JSTemporalDateTimeRecord.create(d.getYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds(), 0, 0, 0);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    @TruffleBoundary
    private static JSTemporalDateTimeRecord tryParseDate(String string) {
        try {
            DateFormat df = new SimpleDateFormat();
            java.util.Date d = df.parse(string);
            return JSTemporalDateTimeRecord.create(d.getYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds(), 0, 0, 0);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    @TruffleBoundary
    private static JSTemporalDateTimeRecord tryParseDate2(String string) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date d = df.parse(string);
            return JSTemporalDateTimeRecord.create(d.getYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds(), 0, 0, 0);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    @TruffleBoundary
    private static JSTemporalDateTimeRecord tryParseTime(String string) {
        try {
            DateFormat df = new SimpleDateFormat("hh:mm:ss.SSS");
            df.setLenient(true);
            java.util.Date d = df.parse(string);
            return JSTemporalDateTimeRecord.create(d.getYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds(), 0, 0, 0);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    @TruffleBoundary
    private static JSTemporalDateTimeRecord tryParseTime2(String string) {
        try {
            DateFormat df = new SimpleDateFormat("hh:mm:ss");
            df.setLenient(true);
            java.util.Date d = df.parse(string);
            return JSTemporalDateTimeRecord.create(d.getYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds(), 0, 0, 0);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    @TruffleBoundary
    private static JSTemporalDateTimeRecord tryParseTime3(String string) {
        try {
            DateFormat df = new SimpleDateFormat("hh:mm");
            df.setLenient(true);
            java.util.Date d = df.parse(string);
            return JSTemporalDateTimeRecord.create(d.getYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds(), 0, 0, 0);
        } catch (Exception e) {
            return null;
        }
    }

    // 13.27
    public static void validateTemporalUnitRange(String largestUnit, String smallestUnit) {
        if (smallestUnit.equals(YEAR) && !largestUnit.equals(YEAR)) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(MONTH) && !largestUnit.equals(YEAR) && !largestUnit.equals(MONTH)) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(WEEK) && !largestUnit.equals(YEAR) && !largestUnit.equals(MONTH) && !largestUnit.equals(WEEK)) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(DAY) && !largestUnit.equals(YEAR) && !largestUnit.equals(MONTH) && !largestUnit.equals(WEEK) && !largestUnit.equals(DAY)) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(HOUR) && !largestUnit.equals(YEAR) && !largestUnit.equals(MONTH) && !largestUnit.equals(WEEK) && !largestUnit.equals(DAY) && !largestUnit.equals(HOUR)) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(MINUTE) && (largestUnit.equals(SECOND) || largestUnit.equals(MILLISECOND) || largestUnit.equals(MICROSECOND) || largestUnit.equals(NANOSECOND))) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(SECOND) && (largestUnit.equals(MILLISECOND) || largestUnit.equals(MICROSECOND) || largestUnit.equals(NANOSECOND))) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(MILLISECOND) && (largestUnit.equals(MICROSECOND) || largestUnit.equals(NANOSECOND))) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(MICROSECOND) && largestUnit.equals(NANOSECOND)) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
    }

    // 13.31
    public static double maximumTemporalDurationRoundingIncrement(String unit) {
        if (unit.equals(YEAR) || unit.equals(MONTH) || unit.equals(WEEK) || unit.equals(DAY)) {
            return Double.POSITIVE_INFINITY; // Undefined according to spec, we fix in caller
        }
        if (unit.equals(HOUR)) {
            return 24d;
        }
        if (unit.equals(MINUTE) || unit.equals(SECOND)) {
            return 60d;
        }
        assert unit.equals(MILLISECOND) || unit.equals(MICROSECOND) || unit.equals(NANOSECOND);
        return 1000d;
    }

    // 13.32
    @TruffleBoundary
    public static String formatSecondsStringPart(long second, long millisecond, long microsecond, long nanosecond,
                    Object precision) {
        if (precision.equals(MINUTE)) {
            return "";
        }
        String secondString = String.format(":%1$2d", second).replace(" ", "0");
        long fraction = (millisecond * 1_000_000) + (microsecond * 1_000) + nanosecond;
        String fractionString = "";
        if (precision.equals("auto")) {
            if (fraction == 0) {
                return secondString;
            }
            fractionString = fractionString.concat(String.format("%1$3d", millisecond).replace(" ", "0"));
            fractionString = fractionString.concat(String.format("%1$3d", microsecond).replace(" ", "0"));
            fractionString = fractionString.concat(String.format("%1$3d", nanosecond).replace(" ", "0"));
            fractionString = longestSubstring(fractionString);
        } else {
            if (precision.equals(0)) {
                return secondString;
            }
            fractionString = fractionString.concat(String.format("%1$3d", millisecond).replace(" ", "0"));
            fractionString = fractionString.concat(String.format("%1$3d", microsecond).replace(" ", "0"));
            fractionString = fractionString.concat(String.format("%1$3d", nanosecond).replace(" ", "0"));
            fractionString = fractionString.substring(0, (int) toLong(precision));
        }
        return secondString.concat(".").concat(fractionString);
    }

    private static String longestSubstring(String str) {
        String s = str;
        while (s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    // 13.33
    public static double nonNegativeModulo(double x, double y) {
        double result = x % y;
        if (result == -0) {
            return 0;
        }
        if (result < 0) {
            result = result + y;
        }
        return result;
    }

    // 13.34
    public static long sign(long n) {
        if (n > 0) {
            return 1;
        }
        if (n < 0) {
            return -1;
        }
        return n;
    }

    // 13.35
    public static long constrainToRange(long value, long minimum, long maximum) {
        return Math.min(Math.max(value, minimum), maximum);
    }

    // 13.36
    public static double roundHalfAwayFromZero(double x) {
        return Math.round(x);
    }

    // 13.37
    public static double roundNumberToIncrement(double x, double increment, String roundingMode) {
        assert roundingMode.equals("ceil") || roundingMode.equals("floor") || roundingMode.equals("trunc") || roundingMode.equals("halfExpand");
        double quotient = x / increment;
        double rounded;
        if (roundingMode.equals("ceil")) {
            rounded = -Math.floor(-quotient);
        } else if (roundingMode.equals("floor")) {
            rounded = Math.floor(quotient);
        } else if (roundingMode.equals("trunc")) {
            rounded = (long) quotient;
        } else {
            rounded = roundHalfAwayFromZero(quotient);
        }
        return rounded * increment;
    }

    // 13.43
    @SuppressWarnings("unused")
    public static String parseTemporalCalendarString(String isoString) {
        // TODO: parseTemporalCalendarString
        if (isoString.contains(ISO8601)) {
            return ISO8601;
        } else if (isoString.contains(GREGORY)) {
            return GREGORY;
        } else if (isoString.contains(JAPANESE)) {
            return JAPANESE;
        }
        return ISO8601;
    }

    // 13.51
    public static long toPositiveIntegerOrInfinity(Object value) {
        double integer = JSRuntime.toDouble(value);
        if (integer == Double.NEGATIVE_INFINITY) {
            throw Errors.createRangeError("Integer should not be negative infinity.");
        }
        if (integer <= 0) {
            throw Errors.createRangeError("Integer should be positive.");
        }
        return (long) integer;
    }

    private static long toIntegerOrInfinity(Object value) {
        double integer = JSRuntime.toDouble(value);
        return (long) integer;
    }

    // 13.52
    @TruffleBoundary
    public static DynamicObject prepareTemporalFields(DynamicObject fields, Set<String> fieldNames, Set<String> requiredFields, JSContext ctx) {
        DynamicObject result = JSOrdinary.create(ctx);
        for (String property : fieldNames) {
            Object value = JSObject.get(fields, property);
            if (value == null || value == Undefined.instance) { // TODO (CW) null is probably wrong
                if (requiredFields.contains(property)) {
                    throw TemporalErrors.createTypeErrorPropertyRequired(property);
                } else {
                    if (temporalFieldDefaults.containsKey(property)) {
                        value = temporalFieldDefaults.get(property);
                    }
                }
            } else {
                if (temporalFieldConversion.containsKey(property)) {
                    Function<Object, Object> conversion = temporalFieldConversion.get(property);
                    value = conversion.apply(value);
                }
            }
            JSObjectUtil.defineDataProperty(ctx, result, property, value, JSAttributes.configurableEnumerableWritable());
        }
        return result;
    }

    @TruffleBoundary
    public static DynamicObject preparePartialTemporalFields(DynamicObject fields, Set<String> fieldNames, JSContext ctx) {
        DynamicObject result = JSOrdinary.create(ctx);
        boolean any = false;
        for (String property : fieldNames) {
            Object value = JSObject.get(fields, property);
            if (!isNullish(value)) {
                any = true;
                if (temporalFieldConversion.containsKey(property)) {
                    Function<Object, Object> conversion = temporalFieldConversion.get(property);
                    value = conversion.apply(value);
                }
            }
            JSObjectUtil.defineDataProperty(ctx, result, property, value, JSAttributes.configurableEnumerableWritable());
        }
        if (!any) {
            throw Errors.createTypeError("Given dateTime like object has no relevant properties.");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    public static <T> Set<T> toSet(T... values) {
        return Arrays.stream(values).collect(Collectors.toSet());
    }

    @TruffleBoundary
    private static <T, I> Map<T, I> toMap(T[] keys, I[] values) {
        Map<T, I> map = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }
        return map;
    }

    public static DynamicObject regulateISOYearMonth(long year, long month, String overflow, JSContext ctx) {
        assert isInteger(year);
        assert isInteger(month);
        assert CONSTRAIN.equals(overflow) || REJECT.equals(overflow);

        if (CONSTRAIN.equals(overflow)) {
            return constrainISOYearMonth(year, month, ctx);
        } else if (REJECT.equals(overflow)) {
            if (!validateISOYearMonth(year, month)) {
                throw Errors.createRangeError("validation of year and month failed");
            }
        }

        return createRecordYearMonth(year, month, ctx);
    }

    private static boolean validateISOYearMonth(long year, long month) {
        assert isInteger(year);
        assert isInteger(month);
        return (1 <= month) && (month <= 12);
    }

    private static DynamicObject constrainISOYearMonth(long year, long month, JSContext ctx) {
        assert isInteger(year);
        assert isInteger(month);

        long monthPrepared = constrainToRange(month, 1, 12);

        return createRecordYearMonth(year, monthPrepared, ctx);
    }

    // TODO this should be a record, not an object
    private static DynamicObject createRecordYearMonth(long year, long monthPrepared, JSContext ctx) {
        DynamicObject record = JSOrdinary.create(ctx);
        JSObjectUtil.putDataProperty(ctx, record, MONTH, monthPrepared);
        JSObjectUtil.putDataProperty(ctx, record, YEAR, year);
        return record;
    }

    public static long isoDaysInMonth(long y, long m) {
        if (m == 1 || m == 3 || m == 5 || m == 7 || m == 8 || m == 10 || m == 12) {
            return 31;
        } else if (m == 4 || m == 6 || m == 9 || m == 11) {
            return 30;
        } else {
            assert m == 2;
            return isISOLeapYear(y) ? 29 : 28;
        }
    }

    private static boolean isISOLeapYear(long year) {
        assert isInteger(year);
        int y = (int) year;
        if ((y % 4) != 0) {
            return false;
        }
        if ((y % 400) == 0) {
            return true;
        }
        if ((y % 100) == 0) {
            return false;
        }
        return true;
    }

    private static boolean isInteger(Object l) {
        return (l instanceof Long && JSRuntime.longIsRepresentableAsInt((long) l));
    }

    private static boolean isInteger(long l) {
        return JSRuntime.longIsRepresentableAsInt(l);
    }

    public static JSTemporalDateTimeRecord balanceISOYearMonth(long year, long month) {
        assert isInteger(year);
        assert isInteger(month);

        if (year == Long.MAX_VALUE || year == Long.MIN_VALUE || month == Long.MAX_VALUE || year == Long.MIN_VALUE) {
            throw Errors.createRangeError("value our of range");
        }

        long yearPrepared = year + (long) Math.floor((month - 1.0) / 12.0);
        long monthPrepared = (long) nonNegativeModulo(month - 1, 12) + 1;

        return JSTemporalDateTimeRecord.create(yearPrepared, monthPrepared, 0, 0, 0, 0, 0, 0, 0);
    }

    // helper method. According to spec, this already is an integer value
    // so help Java see that
    public static long asLong(Object value) {
        assert isInteger(value);
        return (long) value;
    }

    public static DynamicObject getTemporalCalendarWithISODefault(Object item, JSContext ctx) {
        if (item instanceof TemporalCalendar) {
            return ((TemporalCalendar) item).getCalendar();
        } else {
            Object calendar = JSObject.get((DynamicObject) item, TemporalConstants.CALENDAR);
            return toTemporalCalendarWithISODefault(calendar, ctx);
        }
    }

    public static DynamicObject getISO8601Calendar(JSContext ctx) {
        return getBuiltinCalendar(ISO8601, ctx);
    }

    @TruffleBoundary
    public static DynamicObject getBuiltinCalendar(String id, JSContext ctx) {
        Object cal = JSRuntime.construct(ctx.getRealm().getTemporalCalendarConstructor(), new Object[]{id});
        return (DynamicObject) cal;
    }

    @TruffleBoundary
    public static DynamicObject toTemporalCalendar(Object temporalCalendarLike, JSContext ctx) {
        if (JSRuntime.isObject(temporalCalendarLike)) {
            DynamicObject obj = (DynamicObject) temporalCalendarLike;
            if (temporalCalendarLike instanceof TemporalCalendar) {
                return ((TemporalCalendar) temporalCalendarLike).getCalendar();
            }
            if (!JSObject.hasProperty(obj, CALENDAR)) {
                return obj;
            }
            Object obj2 = JSObject.get(obj, CALENDAR);
            if (JSRuntime.isObject(obj2) && !JSObject.hasProperty((DynamicObject) obj2, CALENDAR)) {
                return (DynamicObject) obj2;
            }
        }
        String identifier = JSRuntime.toString(temporalCalendarLike);
        if (!JSTemporalCalendar.isBuiltinCalendar(identifier)) {
            identifier = parseTemporalCalendarString(identifier);
        }
        return JSTemporalCalendar.create(ctx, identifier);
    }

    @TruffleBoundary
    public static Set<String> calendarFields(DynamicObject calendar, String[] strings, JSContext ctx) {
        DynamicObject fields = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.FIELDS);

        if (fields == Undefined.instance) {
            Set<String> set = new HashSet<>();
            for (String s : strings) {
                set.add(s);
            }
            return set;

        } else {
            DynamicObject fieldsArray = JSArray.createConstant(ctx, strings);
            return arrayToStringSet((DynamicObject) JSRuntime.call(fields, calendar, new Object[]{fieldsArray}));
        }
    }

    @TruffleBoundary
    public static Set<String> arrayToStringSet(DynamicObject array) {
        long len = (long) JSObject.get(array, "length");
        Set<String> set = new HashSet<>();
        for (int i = 0; i < len; i++) {
            set.add(JSObject.get(array, i).toString());
        }
        return set;
    }

    public static DynamicObject dateFromFields(DynamicObject calendar, DynamicObject fields, Object options) {
        Object dateFromFields = JSObject.get(calendar, TemporalConstants.DATE_FROM_FIELDS);
        DynamicObject date = (DynamicObject) JSRuntime.call(dateFromFields, calendar, new Object[]{fields, options});
        requireTemporalDate(date);
        return date;
    }

    public static JSTemporalDateTimeRecord parseTemporalDateTimeString(String string, JSContext ctx) {
        // TODO 2. If isoString does not satisfy the syntax of a TemporalDateTimeString (see 13.39)
        JSTemporalDateTimeRecord result = parseISODateTime(string, ctx);
        return result;
    }

    public static JSTemporalDateTimeRecord parseTemporalDateString(String string, JSContext ctx) {
        // TODO 2. If isoString does not satisfy the syntax of a TemporalDateTimeString (see 13.39)
        JSTemporalDateTimeRecord result = parseISODateTime(string, ctx);
        DynamicObject calendar = getISO8601Calendar(ctx); // TODO: should be result.getCalendar();
        return JSTemporalDateTimeRecord.createCalendar(result.getYear(), result.getMonth(), result.getDay(), 0, 0, 0, 0, 0, 0, calendar);
    }

    public static JSTemporalDateTimeRecord parseTemporalTimeString(String string, JSContext ctx) {
        // TODO 2. If isoString does not satisfy the syntax of a TemporalDateTimeString (see 13.39)
        JSTemporalDateTimeRecord result = parseISODateTime(string, ctx);
        return JSTemporalDateTimeRecord.create(0, 0, 0, result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
    }

    @SuppressWarnings("unused")
    private static Object parseTemporalTimeZone(String string) {
        // TODO Auto-generated method stub
        return Undefined.instance;
    }

    @TruffleBoundary
    public static Object buildISOMonthCode(String numberPart) {
        assert 1 <= numberPart.length() && numberPart.length() <= 2;
        return numberPart.length() >= 2 ? "M" + numberPart : "M0" + numberPart;
    }

    @TruffleBoundary
    public static Object isoMonthCode(int month) {
        return buildISOMonthCode(String.valueOf(month));
    }

    public static DynamicObject toTemporalTimeZone(Object temporalTimeZoneLike) {
        if (JSRuntime.isObject(temporalTimeZoneLike)) {
            DynamicObject tzObj = (DynamicObject) temporalTimeZoneLike;
            if (JSObject.hasOwnProperty(tzObj, "InitializedTemporalZonedDateTime")) {
                return (DynamicObject) JSObject.get(tzObj, "TimeZone");
            } else if (!JSObject.hasProperty(tzObj, TIME_ZONE)) {
                return tzObj;
            }
            Object temp = JSObject.get(tzObj, TIME_ZONE);
            if (JSRuntime.isObject(temp) && !JSObject.hasProperty((DynamicObject) temp, TIME_ZONE)) {
                return (DynamicObject) temp;
            }
        }
        String identifier = JSRuntime.toString(temporalTimeZoneLike);
        return createTemporalTimeZone(parseTemporalTimeZone(identifier));
    }

    @SuppressWarnings("unused")
    private static DynamicObject createTemporalTimeZone(Object parseTemporalTimeZone) {
        // TODO Auto-generated method stub
        throw Errors.createTypeError("not yet implemented");
    }

    public static DynamicObject createTemporalDateTime(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond,
                    DynamicObject calendar, JSContext context) {
        return JSTemporalPlainDateTime.createTemporalDateTime(context, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, calendar);
    }

    @SuppressWarnings("unused")
    public static DynamicObject builtinTimeZoneGetInstantFor(DynamicObject timeZone, DynamicObject temporalDateTime, String string) {
        // TODO Auto-generated method stub
        throw Errors.createTypeError("not yet implemented");
    }

    @SuppressWarnings("unused")
    public static DynamicObject createTemporalZonedDateTime(long longOrDefault, DynamicObject timeZone, DynamicObject calendar) {
        // TODO Auto-generated method stub
        throw Errors.createTypeError("not yet implemented");
    }

    @TruffleBoundary
    public static long getLong(DynamicObject ob, String key, long defaultValue) {
        Object value = JSObject.get(ob, key);
        if (value == Undefined.instance) {
            return defaultValue;
        }
        Number n = (Number) value;
        return n.longValue();
    }

    @TruffleBoundary
    public static long getLong(DynamicObject ob, String key) {
        Object value = JSObject.get(ob, key);
        Number n = (Number) value;
        return n.longValue();
    }

    @TruffleBoundary
    public static int getInt(DynamicObject ob, String key) {
        Object value = JSObject.get(ob, key);
        Number n = (Number) value;
        return n.intValue();
    }

    @TruffleBoundary
    public static Object getOrDefault(DynamicObject ob, String key, Object defaultValue) {
        Object value = JSObject.get(ob, key);
        return value != null ? value : defaultValue;
    }

    public static boolean isoDateTimeWithinLimits(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond) {
        double ns = getEpochFromISOParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);

        final double lowerBound = -8.64 * Math.pow(10, 21) - 8.64 * Math.pow(10, 16);
        final double upperBound = 8.64 * Math.pow(10, 21) + 8.64 * Math.pow(10, 16);

        if (ns <= lowerBound || ns >= upperBound) {
            return false;
        }
        return true;
    }

    public static double getEpochFromISOParts(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond) {
        assert isValidISODate(year, month, day);
        assert isValidTime(hour, minute, second, millisecond, microsecond, nanosecond);

        double date = JSDate.makeDay(year, month - 1, day);
        double time = JSDate.makeTime(hour, minute, second, millisecond);
        double ms = JSDate.makeDate(date, time);
        assert isFinite(ms);
        return ms * 1_000_000L + microsecond * 1_000L + nanosecond;
    }

    private static boolean isFinite(double d) {
        return !(Double.isNaN(d) || Double.isInfinite(d));
    }

    public static String toTemporalOverflow(DynamicObject options) {
        String type = "string"; // TODO there is a bug in the current spec, missing this argument!
        return (String) getOption(options, OVERFLOW, type, TemporalUtil.toSet(CONSTRAIN, REJECT), CONSTRAIN);
    }

    @TruffleBoundary
    public static Object getOption(DynamicObject options, String property, String type, Set<Object> values, Object fallback) {
        assert JSRuntime.isObject(options);
        Object value = JSObject.get(options, property);
        if (value == Undefined.instance) {
            return fallback;
        }
        assert type.equals("boolean") || type.equals("string");
        if (type.equals("boolean")) {
            value = JSRuntime.toBoolean(value);
        } else if (type.equals("string")) {
            value = JSRuntime.toString(value);
        }
        if (values != null && !values.contains(value)) {
            throw TemporalErrors.createRangeErrorOptionsNotContained(values, value);
        }
        return value;
    }

    public static JSTemporalDateTimeRecord interpretTemporalDateTimeFields(DynamicObject calendar, DynamicObject fields, DynamicObject options) {
        JSTemporalDateTimeRecord timeResult = toTemporalTimeRecord(fields);
        DynamicObject temporalDate = dateFromFields(calendar, fields, options);
        TemporalDate date = (TemporalDate) temporalDate;
        String overflow = toTemporalOverflow(options);
        JSTemporalDurationRecord timeResult2 = TemporalUtil.regulateTime(
                        timeResult.getHour(), timeResult.getMinute(), timeResult.getSecond(), timeResult.getMillisecond(), timeResult.getMicrosecond(), timeResult.getNanosecond(),
                        overflow);

        return JSTemporalDateTimeRecord.create(
                        date.getISOYear(), date.getISOMonth(), date.getISODay(),
                        timeResult2.getHours(), timeResult2.getMinutes(), timeResult2.getSeconds(),
                        timeResult2.getMilliseconds(), timeResult2.getMicroseconds(), timeResult2.getNanoseconds());
    }

    public static JSTemporalDurationRecord regulateTime(long hours, long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds, String overflow) {
        assert overflow.equals(CONSTRAIN) || overflow.equals(REJECT);
        if (overflow.equals(CONSTRAIN)) {
            return constrainTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        } else {
            if (!TemporalUtil.isValidTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
                throw Errors.createRangeError("Given time outside the range.");
            }
            // sets [[Days]] but [[Hour]]
            return JSTemporalDurationRecord.create(0, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
    }

    public static JSTemporalDurationRecord constrainTime(long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds) {
        return JSTemporalDurationRecord.create(0, 0, 0,
                        TemporalUtil.constrainToRange(hours, 0, 23),
                        TemporalUtil.constrainToRange(minutes, 0, 59),
                        TemporalUtil.constrainToRange(seconds, 0, 59),
                        TemporalUtil.constrainToRange(milliseconds, 0, 999),
                        TemporalUtil.constrainToRange(microseconds, 0, 999),
                        TemporalUtil.constrainToRange(nanoseconds, 0, 999));
    }

    public static JSTemporalDateTimeRecord toTemporalTimeRecord(DynamicObject temporalTimeLike) {
        return JSTemporalDateTimeRecord.create(0, 0, 0,
                        getOrFail(temporalTimeLike, HOUR),
                        getOrFail(temporalTimeLike, MINUTE),
                        getOrFail(temporalTimeLike, SECOND),
                        getOrFail(temporalTimeLike, MILLISECOND),
                        getOrFail(temporalTimeLike, MICROSECOND),
                        getOrFail(temporalTimeLike, NANOSECOND));

    }

    private static long getOrFail(DynamicObject temporalTimeLike, String property) {
        Object value = JSObject.get(temporalTimeLike, property);
        if (value == null) {
            throw TemporalErrors.createTypeErrorPropertyNotUndefined(property);
        }
        return toIntegerOrInfinity(value);
    }

    public static DynamicObject dateAdd(DynamicObject calendar, DynamicObject datePart, DynamicObject dateDuration, DynamicObject options, DynamicObject dateUntilParam) {
        DynamicObject dateUntil = dateUntilParam == Undefined.instance ? (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_UNTIL) : dateUntilParam;
        Object duration = JSRuntime.call(dateUntil, calendar, new Object[]{datePart, dateDuration, options});
        TemporalUtil.requireTemporalDuration(duration);
        return (DynamicObject) duration;
    }

    public static DynamicObject dateAdd(DynamicObject calendar, DynamicObject datePart, DynamicObject dateDuration, DynamicObject options) {
        return dateAdd(calendar, datePart, dateDuration, options, Undefined.instance);
    }

    public static DynamicObject calendarDateAdd(DynamicObject calendar, DynamicObject date, DynamicObject duration, DynamicObject options, DynamicObject dateAdd) {
        DynamicObject dateAddPrepared = dateAdd;
        if (dateAddPrepared == Undefined.instance) {
            dateAddPrepared = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
        }
        DynamicObject addedDate = (DynamicObject) JSRuntime.call(dateAddPrepared, calendar, new Object[]{date, duration, options});
        requireTemporalDate(addedDate);
        return addedDate;
    }

    public static DynamicObject calendarDateUntil(DynamicObject calendar, DynamicObject one, DynamicObject two, DynamicObject options, DynamicObject dateUntil) {
        DynamicObject dateUntilPrepared = dateUntil;
        if (dateUntilPrepared == Undefined.instance) {
            dateUntilPrepared = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_UNTIL);
        }
        DynamicObject addedDate = (DynamicObject) JSRuntime.call(dateUntilPrepared, calendar, new Object[]{one, two, options});
        requireTemporalDuration(addedDate);
        return addedDate;
    }

    @SuppressWarnings("unused")
    public static long addZonedDateTime(long long1, Object timeZone, DynamicObject calendar, long years, long months, long weeks, long days, int i, int j, int k, int l, int m, int n) {
        // TODO Auto-generated method stub
        return 0L;
    }

    public static long roundTemporalInstant(long ns, long increment, String unit, String roundingMode) {
        long incrementNs = 0;
        if (HOUR.equals(unit)) {
            incrementNs = increment * 3_600_000_000_000L;
        } else if (MINUTE.equals(unit)) {
            incrementNs = increment * 60_000_000_000L;
        } else if (SECOND.equals(unit)) {
            incrementNs = increment * 1_000_000_000L;
        } else if (MILLISECOND.equals(unit)) {
            incrementNs = increment * 1_000_000L;
        } else if (MICROSECOND.equals(unit)) {
            incrementNs = increment * 1_000L;
        }
        return (long) roundNumberToIncrement(ns, incrementNs, roundingMode);
    }

    public static DynamicObject toTemporalDate(Object itemParam, DynamicObject optionsParam, JSContext ctx) {
        DynamicObject options = isNullish(optionsParam) ? JSOrdinary.createWithNullPrototype(ctx) : optionsParam;
        if (JSRuntime.isObject(itemParam)) {
            DynamicObject item = (DynamicObject) itemParam;
            if (JSTemporalPlainDate.isJSTemporalPlainDate(item)) {
                return item;
            }
            DynamicObject calendar = TemporalUtil.getTemporalCalendarWithISODefault(item, ctx);
            Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{DAY, MONTH, MONTH_CODE, YEAR}, ctx);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(item, fieldNames, TemporalUtil.toSet(), ctx);
            return TemporalUtil.dateFromFields(calendar, fields, options);
        }
        String overflows = TemporalUtil.toTemporalOverflow(options);
        JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalDateString(JSRuntime.toString(itemParam), ctx);
        if (!validateISODate(result.getYear(), result.getMonth(), result.getDay())) {
            throw TemporalErrors.createRangeErrorDateOutsideRange();
        }
        DynamicObject calendar = TemporalUtil.toTemporalCalendarWithISODefault(result.getCalendar(), ctx);
        result = regulateISODate(result.getYear(), result.getMonth(), result.getDay(), overflows);

        return JSTemporalPlainDate.createTemporalDate(ctx, result.getYear(), result.getMonth(), result.getDay(), calendar);
    }

    // 3.5.8
    public static boolean validateISODate(long year, long month, long day) {
        if (month < 1 || month > 12) {
            return false;
        }
        long daysInMonth = JSTemporalCalendar.isoDaysInMonth(year, month);
        if (day < 1 || day > daysInMonth) {
            return false;
        }
        return true;
    }

    // 3.5.6
    public static DynamicObject toTemporalDateFields(DynamicObject temporalDateLike, Set<String> fieldNames, JSContext ctx) {
        return TemporalUtil.prepareTemporalFields(temporalDateLike, fieldNames, Collections.emptySet(), ctx);
    }

    // 3.5.7
    @TruffleBoundary
    public static JSTemporalDateTimeRecord regulateISODate(long year, long month, long day, String overflow) {
        assert overflow.equals(CONSTRAIN) || overflow.equals(REJECT);
        if (overflow.equals(REJECT)) {
            rejectISODate(year, month, day);
            return JSTemporalDateTimeRecord.create(year, month, day, 0, 0, 0, 0, 0, 0);
        }
        if (overflow.equals(CONSTRAIN)) {
            return constrainISODate(year, month, day);
        }
        throw new RuntimeException("This should never have happened.");
    }

    // 3.5.9
    public static void rejectISODate(long year, long month, long day) {
        if (!validateISODate(year, month, day)) {
            throw TemporalErrors.createRangeErrorDateOutsideRange();
        }
    }

    // 3.5.10
    public static JSTemporalDateTimeRecord constrainISODate(long year, long month, long day) {
        long monthPrepared = TemporalUtil.constrainToRange(month, 1, 12);
        long dayPrepared = TemporalUtil.constrainToRange(day, 1, TemporalUtil.isoDaysInMonth(year, month));
        return JSTemporalDateTimeRecord.create(year, monthPrepared, dayPrepared, 0, 0, 0, 0, 0, 0);
    }

    // 3.5.11
    public static JSTemporalDateTimeRecord balanceISODate(long yearParam, long monthParam, long dayParam) {
        JSTemporalDateTimeRecord balancedYearMonth = TemporalUtil.balanceISOYearMonth(yearParam, monthParam);
        long month = balancedYearMonth.getMonth();
        long year = balancedYearMonth.getYear();
        long day = dayParam;
        long testYear;
        if (month > 2) {
            testYear = year;
        } else {
            testYear = year - 1;
        }
        while (day < -1 * JSTemporalCalendar.isoDaysInYear(testYear)) {
            day = day + JSTemporalCalendar.isoDaysInYear(testYear);
            year = year - 1;
            testYear = testYear - 1;
        }
        testYear = year + 1;
        while (day > JSTemporalCalendar.isoDaysInYear(testYear)) {
            day = day - JSTemporalCalendar.isoDaysInYear(testYear);
            year = year + 1;
            testYear = testYear + 1;
        }
        while (day < 1) {
            balancedYearMonth = TemporalUtil.balanceISOYearMonth(year, month - 1);
            year = balancedYearMonth.getYear();
            month = balancedYearMonth.getMonth();
            day = day + JSTemporalCalendar.isoDaysInMonth(year, month);
        }
        while (day > JSTemporalCalendar.isoDaysInMonth(year, month)) {
            day = day - JSTemporalCalendar.isoDaysInMonth(year, month);
            balancedYearMonth = TemporalUtil.balanceISOYearMonth(year, month + 1);
            year = balancedYearMonth.getYear();
            month = balancedYearMonth.getMonth();
        }
        return JSTemporalPlainDate.toRecord(year, month, day);
    }

    // 3.5.14
    @TruffleBoundary
    public static JSTemporalDateTimeRecord addISODate(long year, long month, long day, long years, long months, long weeks, long days, String overflow) {
        assert overflow.equals(CONSTRAIN) || overflow.equals(REJECT);
        long y = year + years;
        long m = month + months;
        JSTemporalDateTimeRecord intermediate = TemporalUtil.balanceISOYearMonth(y, m);
        intermediate = regulateISODate(intermediate.getYear(), intermediate.getMonth(), day, overflow);
        long d = intermediate.getDay() + (days + (7 * weeks));
        intermediate = balanceISODate(intermediate.getYear(), intermediate.getMonth(), d);
        return regulateISODate(intermediate.getYear(), intermediate.getMonth(), intermediate.getDay(), overflow);
    }

    // 3.5.15
    public static int compareISODate(long y1, long m1, long d1, long y2, long m2, long d2) {
        if (y1 > y2) {
            return 1;
        }
        if (y1 < y2) {
            return -1;
        }
        if (m1 > m2) {
            return 1;
        }
        if (m1 < m2) {
            return -1;
        }
        if (d1 > d2) {
            return 1;
        }
        if (d1 < d2) {
            return -1;
        }
        return 0;
    }

    public static TemporalTime requireTemporalTime(Object obj) {
        // spec says RequireInternalSlot(obj, "InitializedTemporalTime");
        if (!(obj instanceof TemporalTime)) {
            throw Errors.createTypeError("InitializedTemporalTime expected");
        }
        return (TemporalTime) obj;
    }

    public static TemporalDate requireTemporalDate(Object obj) {
        // spec says RequireInternalSlot(obj, "InitializedTemporalDate");
        if (!(obj instanceof TemporalDate)) {
            throw Errors.createTypeError("InitializedTemporalDate expected");
        }
        return (TemporalDate) obj;
    }

    public static TemporalDateTime requireTemporalDateTime(Object obj) {
        // spec says RequireInternalSlot(obj, "InitializedTemporalDateTime");
        if (!(obj instanceof TemporalDateTime)) {
            throw Errors.createTypeError("InitializedTemporalDateTime expected");
        }
        return (TemporalDateTime) obj;
    }

    public static JSTemporalCalendarObject requireTemporalCalendar(Object obj) {
        // spec says RequireInternalSlot(obj, "InitializedTemporalCalendar");
        if (!(obj instanceof JSTemporalCalendarObject)) {
            throw Errors.createTypeError("InitializedTemporalCalendar expected");
        }
        return (JSTemporalCalendarObject) obj;
    }

    public static JSTemporalDurationObject requireTemporalDuration(Object obj) {
        // spec says RequireInternalSlot(obj, "InitializedTemporalDuration");
        if (!(obj instanceof JSTemporalDurationObject)) {
            throw Errors.createTypeError("InitializedTemporalDuration expected");
        }
        return (JSTemporalDurationObject) obj;
    }

    public static JSTemporalPlainMonthDayObject requireTemporalMonthDay(Object obj) {
        // spec says RequireInternalSlot(obj, "InitializedTemporalMonthDay");
        if (!(obj instanceof JSTemporalPlainMonthDayObject)) {
            throw Errors.createTypeError("InitializedTemporalMonthDay expected");
        }
        return (JSTemporalPlainMonthDayObject) obj;
    }

    public static JSTemporalPlainYearMonthObject requireTemporalYearMonth(Object obj) {
        // spec says RequireInternalSlot(obj, "InitializedTemporalYearMonth");
        if (!(obj instanceof JSTemporalPlainYearMonthObject)) {
            throw Errors.createTypeError("InitializedTemporalYearMonth expected");
        }
        return (JSTemporalPlainYearMonthObject) obj;
    }

    public static Object toPlural(Object u) {
        if (u == Undefined.instance) {
            return u;
        }
        if (HOUR.equals(u) || HOURS.equals(u)) {
            return HOURS;
        } else if (MINUTE.equals(u) || MINUTES.equals(u)) {
            return MINUTES;
        } else if (SECOND.equals(u) || SECONDS.equals(u)) {
            return SECONDS;
        } else if (MILLISECOND.equals(u) || MILLISECONDS.equals(u)) {
            return MILLISECONDS;
        } else if (MICROSECOND.equals(u) || MICROSECONDS.equals(u)) {
            return MICROSECONDS;
        } else if (NANOSECOND.equals(u) || NANOSECONDS.equals(u)) {
            return NANOSECONDS;
        }
        throw Errors.createTypeError("cannot convert Unit to plural");
    }

    public static boolean isTemporalZonedDateTime(@SuppressWarnings("unused") Object obj) {
        // TODO this should be an instanceof check on JSTemporalZonedDateTimeObject
        // but that class does not exist yet :-)
        return false;
    }

    public static String toShowCalendarOption(DynamicObject options) {
        return (String) getOption(options, "calendarName", "string", toSet(AUTO, ALWAYS, NEVER), AUTO);
    }

    @TruffleBoundary
    public static String padISOYear(long year) {
        if (999 < year && year < 9999) {
            return String.valueOf(year);
        }
        String sign = year >= 0 ? "+" : "-";
        long y = Math.abs(year);
        String result = sign + String.format("%1$6d", y);
        return result.replace(" ", "0");
    }

    @TruffleBoundary
    public static String formatCalendarAnnotation(String id, String showCalendar) {
        if (NEVER.equals(showCalendar)) {
            return "";
        } else if (AUTO.equals(showCalendar) && ISO8601.equals(id)) {
            return "";
        } else {
            return "[u-ca=" + id + "]";
        }
    }

    public static boolean isNullish(Object obj) {
        return obj == null || obj == Undefined.instance; // TODO this might not be exactly right
    }

    @TruffleBoundary
    public static DynamicObject toTemporalYearMonth(Object item, DynamicObject optParam, JSContext ctx) {
        DynamicObject options = optParam;
        if (optParam == Undefined.instance) {
            options = JSOrdinary.createWithNullPrototype(ctx);
        }
        if (JSRuntime.isObject(item)) {
            DynamicObject itemObj = (DynamicObject) item;
            if (JSTemporalPlainYearMonth.isJSTemporalPlainYearMonth(itemObj)) {
                return itemObj;
            }
            DynamicObject calendar = TemporalUtil.getTemporalCalendarWithISODefault(itemObj, ctx);

            Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{MONTH, MONTH_CODE, YEAR}, ctx);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(itemObj, fieldNames, new HashSet<>(), ctx);
            return yearMonthFromFields(calendar, fields, options);
        }
        TemporalUtil.toTemporalOverflow(options);

        String string = JSRuntime.toString(item);
        JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalYearMonthString(string, ctx);
        DynamicObject calendar = TemporalUtil.toTemporalCalendarWithISODefault(result.getCalendar(), ctx);
        if (result.getYear() == 0) { // TODO Check for undefined here!
            if (!TemporalUtil.validateISODate(result.getYear(), result.getMonth(), 1)) {
                throw TemporalErrors.createRangeErrorDateOutsideRange();
            }
            // TODO year should be undefined!
            return JSTemporalPlainYearMonth.create(ctx, result.getYear(), result.getMonth(), calendar, 0);
        }
        DynamicObject result2 = JSTemporalPlainYearMonth.create(ctx, result.getYear(), result.getMonth(), calendar, result.getDay());
        DynamicObject canonicalYearMonthOptions = JSOrdinary.createWithNullPrototype(ctx);
        return yearMonthFromFields(calendar, result2, canonicalYearMonthOptions);
    }

    public static DynamicObject yearMonthFromFields(DynamicObject calendar, DynamicObject fields, DynamicObject options) {
        Object fn = JSObject.getMethod(calendar, "yearMonthFromFields");
        Object yearMonth = JSRuntime.call(fn, calendar, new Object[]{fields, options});
        return requireTemporalYearMonth(yearMonth);
    }

    public static DynamicObject monthDayFromFields(DynamicObject calendar, DynamicObject fields, DynamicObject options) {
        DynamicObject fn = (DynamicObject) JSObject.getMethod(calendar, "monthDayFromFields");
        Object monthDay = JSRuntime.call(fn, calendar, new Object[]{fields, options});
        return TemporalUtil.requireTemporalMonthDay(monthDay);
    }

    public static String negateTemporalRoundingMode(String roundingMode) {
        if (CEIL.equals(roundingMode)) {
            return FLOOR;
        } else if (FLOOR.equals(roundingMode)) {
            return CEIL;
        }
        return roundingMode;
    }

    public static boolean calendarEquals(DynamicObject one, DynamicObject two) {
        if (one == two) {
            return true;
        }
        if (JSRuntime.toString(one).equals(JSRuntime.toString(two))) {
            return true;
        }
        return false;
    }

    public static Object dayOfWeek(DynamicObject calendar, DynamicObject dt) {
        return JSTemporalCalendar.calendarDayOfWeek(calendar, dt);
    }

    public static Object dayOfYear(DynamicObject calendar, DynamicObject dt) {
        return JSTemporalCalendar.calendarDayOfYear(calendar, dt);
    }

    public static void rejectTemporalCalendarType(DynamicObject obj) {
        if (obj instanceof JSTemporalPlainDateObject || obj instanceof JSTemporalPlainDateTimeObject || obj instanceof JSTemporalPlainMonthDayObject ||
                        obj instanceof JSTemporalPlainTimeObject || obj instanceof JSTemporalPlainYearMonthObject) {
            // TODO TemporalZonedDateTime: ` || obj instanceof JSTemporalZonedDateTimeObject`
            throw Errors.createTypeError("rejecting calendar types");
        }
    }

    public static DynamicObject createTemporalTime(JSContext ctx, long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        return JSTemporalPlainTime.create(ctx, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    public static DynamicObject createTemporalDate(JSContext context, long year, long month, long day, DynamicObject calendar) {
        return JSTemporalPlainDate.createTemporalDate(context, year, month, day, calendar);
    }

    @SuppressWarnings("unused")
    public static JSTemporalDateTimeRecord durationHandleFractions(double fHour, double minute, double fMinute, double second, double fSecond, double millisecond, double fMillisecond,
                    double microsecond, double fMicrosecond, double nanosecond, double fNanosecond) {
        // TODO lots of compution missing here!!
        long hour = (long) fHour;
        return JSTemporalDateTimeRecord.create(0, 0, 0, hour, (long) minute, (long) second, (long) millisecond, (long) microsecond, (long) nanosecond);
    }

    public static long getPropertyFromRecord(JSTemporalDateTimeRecord d, String property) {
        switch (property) {
            case YEARS:
                return d.getYear();
            case MONTHS:
                return d.getMonth();
            case WEEKS:
                return d.getWeeks();
            case DAYS:
                return d.getDay();
            case HOURS:
                return d.getHour();
            case MINUTES:
                return d.getMinute();
            case SECONDS:
                return d.getSecond();
            case MILLISECONDS:
                return d.getMillisecond();
            case MICROSECONDS:
                return d.getMicrosecond();
            case NANOSECONDS:
                return d.getNanosecond();
        }
        throw Errors.createTypeError("unknown property");
    }

    public static Object calendarMergeFields(DynamicObject calendar, DynamicObject fields, DynamicObject additionalFields, EnumerableOwnPropertyNamesNode namesNode, JSContext ctx) {
        Object mergeFields = JSObject.getMethod(calendar, TemporalConstants.MERGE_FIELDS);
        if (mergeFields == Undefined.instance) {
            return defaultMergeFields(fields, additionalFields, namesNode, ctx);
        }
        return JSRuntime.call(mergeFields, calendar, new Object[]{fields, additionalFields});
    }

    @TruffleBoundary
    private static Object defaultMergeFields(DynamicObject fields, DynamicObject additionalFields, EnumerableOwnPropertyNamesNode namesNode, JSContext ctx) {
        DynamicObject merged = JSOrdinary.create(ctx);
        UnmodifiableArrayList<? extends Object> originalKeys = namesNode.execute(fields);
        for (Object nextKey : originalKeys) {
            if (!MONTH.equals(nextKey) && !MONTH_CODE.equals(nextKey)) {
                Object propValue = JSObject.get(fields, nextKey);
                if (propValue != Undefined.instance) {
                    createDataPropertyOrThrow(ctx, merged, nextKey.toString(), propValue);
                }
            }
        }
        UnmodifiableArrayList<? extends Object> newKeys = namesNode.execute(additionalFields);
        for (Object nextKey : newKeys) {
            Object propValue = JSObject.get(additionalFields, nextKey);
            if (propValue != Undefined.instance) {
                createDataPropertyOrThrow(ctx, merged, nextKey.toString(), propValue);
            }
        }
        if (!newKeys.contains(MONTH) && !newKeys.contains(MONTH_CODE)) {
            Object month = JSObject.get(fields, MONTH);
            if (month != Undefined.instance) {
                createDataPropertyOrThrow(ctx, merged, MONTH, month);
            }
            Object monthCode = JSObject.get(fields, MONTH_CODE);
            if (monthCode != Undefined.instance) {
                createDataPropertyOrThrow(ctx, merged, MONTH_CODE, monthCode);
            }

        }
        return merged;
    }

    public static void createDataPropertyOrThrow(JSContext ctx, DynamicObject obj, String key, Object value) {
        JSObjectUtil.defineDataProperty(ctx, obj, key, value, JSAttributes.configurableEnumerableWritable());
    }

    @TruffleBoundary
    public static Set<String> listJoinRemoveDuplicates(Set<String> receiverFieldNames, Set<String> inputFieldNames) {
        Set<String> newSet = new HashSet<>();
        newSet.addAll(receiverFieldNames);
        newSet.addAll(inputFieldNames);
        return newSet;
    }

    public static DynamicObject toTemporalCalendarWithISODefault(Object calendar, JSContext ctx) {
        if (calendar == Undefined.instance) {
            return getISO8601Calendar(ctx);
        } else {
            return toTemporalCalendar(calendar, ctx);
        }
    }

    public static String largerOfTwoTemporalUnits(String a, String b) {
        if (YEAR.equals(a) || YEAR.equals(b)) {
            return YEAR;
        }
        if (MONTH.equals(a) || MONTH.equals(b)) {
            return MONTH;
        }
        if (WEEK.equals(a) || WEEK.equals(b)) {
            return WEEK;
        }
        if (DAY.equals(a) || DAY.equals(b)) {
            return DAY;
        }
        if (HOUR.equals(a) || HOUR.equals(b)) {
            return HOUR;
        }
        if (MINUTE.equals(a) || MINUTE.equals(b)) {
            return MINUTE;
        }
        if (SECOND.equals(a) || SECOND.equals(b)) {
            return SECOND;
        }
        if (MILLISECOND.equals(a) || MILLISECOND.equals(b)) {
            return MILLISECOND;
        }
        if (MICROSECOND.equals(a) || MICROSECOND.equals(b)) {
            return MICROSECOND;
        }
        return NANOSECOND;
    }

    @TruffleBoundary
    public static DynamicObject toTemporalDateTime(Object item, DynamicObject optParam, JSContext ctx) {
        DynamicObject options = optParam;
        if (isNullish(optParam)) {
            options = JSOrdinary.createWithNullPrototype(ctx);
        }
        JSTemporalDateTimeRecord result = null;
        DynamicObject calendar = null;
        if (JSRuntime.isObject(item)) {
            DynamicObject itemObj = (DynamicObject) item;
            if (itemObj instanceof JSTemporalPlainDateTimeObject) {
                return itemObj;
            }
            // TODO
            // if (item instanceof JSTemporalZonedDateTimeObject) {
            // long instant = TemporalUtil.createTemporalInstant(item.getNanoseconds());
            // ii. Return ? BuiltinTimeZoneGetPlainDateTimeFor(item.[[TimeZone]], instant,
            // item.[[Calendar]]).
            // }
            if (itemObj instanceof JSTemporalPlainDateObject) {
                TemporalDate date = (TemporalDate) itemObj;
                return JSTemporalPlainDateTime.createTemporalDateTime(ctx, date.getISOYear(), date.getISOMonth(), date.getISODay(), 0, 0, 0, 0, 0, 0, date.getCalendar());
            }
            calendar = TemporalUtil.getTemporalCalendarWithISODefault(itemObj, ctx);
            Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{"day", "hour", "microsecond", "millisecond", "minute",
                            "month", "monthCode", "nanosecond", "second", "year"}, ctx);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(itemObj, fieldNames, new HashSet<>(), ctx);
            result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, options);
        } else {
            TemporalUtil.toTemporalOverflow(options);
            String string = JSRuntime.toString(item);
            result = TemporalUtil.parseTemporalDateTimeString(string, ctx);
            assert isValidISODate(result.getYear(), result.getMonth(), result.getDay());
            assert isValidTime(result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
            calendar = TemporalUtil.toTemporalCalendarWithISODefault(result.getCalendar(), ctx);
        }
        return JSTemporalPlainDateTime.createTemporalDateTime(ctx,
                        result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                        result.getMicrosecond(), result.getNanosecond(),
                        calendar);
    }

    public static JSTemporalDurationRecord differenceISODateTime(long y1, long mon1, long d1, long h1, long min1, long s1, long ms1, long mus1,
                    long ns1, long y2, long mon2, long d2, long h2, long min2, long s2, long ms2, long mus2, long ns2,
                    DynamicObject calendar, String largestUnit, DynamicObject optionsParam, EnumerableOwnPropertyNamesNode namesNode, JSContext ctx) {

        DynamicObject options = optionsParam;
        if (isNullish(optionsParam)) {
            options = JSOrdinary.createWithNullPrototypeInit(ctx);
        }
        JSTemporalDurationRecord timeDifference = differenceTime(h1, min1, s1, ms1, mus1, ns1, h2, min2, s2, ms2, mus2, ns2);

        int timeSign = durationSign(0, 0, 0, timeDifference.getDays(), timeDifference.getHours(), timeDifference.getMinutes(), timeDifference.getSeconds(),
                        timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds());
        int dateSign = compareISODate(y2, mon2, d2, y1, mon1, d1);
        JSTemporalDateTimeRecord balanceResult = balanceISODate(y1, mon1, d1 + timeDifference.getDays());
        if (timeSign == -dateSign) {
            balanceResult = balanceISODate(balanceResult.getYear(), balanceResult.getMonth(), balanceResult.getDay() - timeSign);
            timeDifference = balanceDuration(-timeSign, timeDifference.getHours(),
                            timeDifference.getMinutes(), timeDifference.getSeconds(), timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds(), largestUnit,
                            Undefined.instance);
        }
        DynamicObject date1 = createTemporalDate(ctx, balanceResult.getYear(), balanceResult.getMonth(), balanceResult.getDay(), calendar);
        DynamicObject date2 = createTemporalDate(ctx, y2, mon2, d2, calendar);
        String dateLargestUnit = largerOfTwoTemporalUnits(DAY, largestUnit);
        DynamicObject untilOptions = mergeLargestUnitOption(options, dateLargestUnit, namesNode, ctx);
        JSTemporalDurationObject dateDifference = (JSTemporalDurationObject) calendarDateUntil(calendar, date1, date2, untilOptions, Undefined.instance);
        // TODO spec on 2021-06-09 says to add years, months and weeks here??
        return balanceDuration(dateDifference.getDays(), timeDifference.getHours(), timeDifference.getMinutes(), timeDifference.getSeconds(), timeDifference.getMilliseconds(),
                        timeDifference.getMicroseconds(),
                        timeDifference.getNanoseconds(), largestUnit, Undefined.instance);
    }

    @TruffleBoundary
    public static DynamicObject mergeLargestUnitOption(DynamicObject options, String largestUnit, EnumerableOwnPropertyNamesNode namesNode, JSContext ctx) {
        DynamicObject merged = JSOrdinary.create(ctx);
        UnmodifiableArrayList<?> keys = namesNode.execute(options);
        for (Object nextKey : keys) {
            String key = nextKey.toString();
            Object propValue = JSObject.get(options, key);
            createDataPropertyOrThrow(ctx, merged, key, propValue);
        }
        createDataPropertyOrThrow(ctx, merged, "largestUnit", largestUnit);
        return merged;
    }

    // 7.5.3
    public static int durationSign(long years, long months, long weeks, long days, long hours, long minutes,
                    long seconds, long milliseconds, long microseconds, long nanoseconds) {
        if (years < 0) {
            return -1;
        }
        if (years > 0) {
            return 1;
        }
        if (months < 0) {
            return -1;
        }
        if (months > 1) {
            return 1;
        }
        if (weeks < 0) {
            return -1;
        }
        if (weeks > 0) {
            return 1;
        }
        if (days < 0) {
            return -1;
        }
        if (days > 0) {
            return 1;
        }
        if (hours < 0) {
            return -1;
        }
        if (hours > 0) {
            return 1;
        }
        if (minutes < 0) {
            return -1;
        }
        if (minutes > 0) {
            return 1;
        }
        if (seconds < 0) {
            return -1;
        }
        if (seconds > 0) {
            return 1;
        }
        if (milliseconds < 0) {
            return -1;
        }
        if (milliseconds > 0) {
            return 1;
        }
        if (microseconds < 0) {
            return -1;
        }
        if (microseconds > 0) {
            return 1;
        }
        if (nanoseconds < 0) {
            return -1;
        }
        if (nanoseconds > 0) {
            return 1;
        }
        return 0;
    }

    // 7.5.4
    public static void rejectDurationSign(long years, long months, long weeks, long days, long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        long sign = durationSign(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        if (years < 0 && sign > 0) {
            throw Errors.createRangeError("Years is negative but it should be positive.");
        }
        if (years > 0 && sign < 0) {
            throw Errors.createRangeError("Years is positive but it should be negative.");
        }
        if (months < 0 && sign > 0) {
            throw Errors.createRangeError("Months is negative but it should be positive.");
        }
        if (months > 0 && sign < 0) {
            throw Errors.createRangeError("Months is positive but it should be negative.");
        }
        if (weeks < 0 && sign > 0) {
            throw Errors.createRangeError("Weeks is negative but it should be positive.");
        }
        if (weeks > 0 && sign < 0) {
            throw Errors.createRangeError("Weeks is positive but it should be negative.");
        }
        if (days < 0 && sign > 0) {
            throw Errors.createRangeError("Days is negative but it should be positive.");
        }
        if (days > 0 && sign < 0) {
            throw Errors.createRangeError("Days is positive but it should be negative.");
        }
        if (hours < 0 && sign > 0) {
            throw Errors.createRangeError("Hours is negative but it should be positive.");
        }
        if (hours > 0 && sign < 0) {
            throw Errors.createRangeError("Hours is positive but it should be negative.");
        }
        if (minutes < 0 && sign > 0) {
            throw Errors.createRangeError("Minutes is negative but it should be positive.");
        }
        if (minutes > 0 && sign < 0) {
            throw Errors.createRangeError("Minutes is positive but it should be negative.");
        }
        if (seconds < 0 && sign > 0) {
            throw Errors.createRangeError("Seconds is negative but it should be positive.");
        }
        if (seconds > 0 && sign < 0) {
            throw Errors.createRangeError("Seconds is positive but it should be negative.");
        }
        if (milliseconds < 0 && sign > 0) {
            throw Errors.createRangeError("Milliseconds is negative but it should be positive.");
        }
        if (milliseconds > 0 && sign < 0) {
            throw Errors.createRangeError("Milliseconds is positive but it should be negative.");
        }
        if (microseconds < 0 && sign > 0) {
            throw Errors.createRangeError("Microseconds is negative but it should be positive.");
        }
        if (microseconds > 0 && sign < 0) {
            throw Errors.createRangeError("Microseconds is positive but it should be negative.");
        }
        if (nanoseconds < 0 && sign > 0) {
            throw Errors.createRangeError("Nanoseconds is negative but it should be positive.");
        }
        if (nanoseconds > 0 && sign < 0) {
            throw Errors.createRangeError("Nanoseconds is positive but it should be negative.");
        }
    }

    public static JSTemporalDurationRecord balanceDuration(long days, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds, String largestUnit, DynamicObject relativeTo) {
        long ns;
        if (TemporalUtil.isTemporalZonedDateTime(relativeTo)) {
            // TODO implement that branch
            ns = nanoseconds;
        } else {
            ns = totalDurationNanoseconds(days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
        }
        long d;
        if (largestUnit.equals(YEAR) || largestUnit.equals(MONTH) || largestUnit.equals(WEEK) || largestUnit.equals(DAY)) {
            JSTemporalNanosecondsDaysRecord result = nanosecondsToDays(ns, relativeTo);
            d = result.getDays();
            ns = result.getNanoseconds();
        } else {
            d = 0;
        }
        long h = 0;
        long min = 0;
        long s = 0;
        long ms = 0;
        long mus = 0;
        long sign = ns < 0 ? -1 : 1;
        ns = Math.abs(ns);
        if (largestUnit.equals(YEAR) || largestUnit.equals(MONTH) || largestUnit.equals(WEEK) ||
                        largestUnit.equals(DAY) || largestUnit.equals(HOUR)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
            ms = Math.floorDiv(mus, 1000);
            mus = mus % 1000;
            s = Math.floorDiv(ms, 1000);
            ms = ms % 1000;
            min = Math.floorDiv(s, 60);
            s = s % 60;
            h = Math.floorDiv(min, 60);
            min = min % 60;
        } else if (largestUnit.equals(MINUTE)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
            ms = Math.floorDiv(mus, 1000);
            mus = mus % 1000;
            s = Math.floorDiv(ms, 1000);
            ms = ms % 1000;
            min = Math.floorDiv(s, 60);
            s = s % 60;
        } else if (largestUnit.equals(SECOND)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
            ms = Math.floorDiv(mus, 1000);
            mus = mus % 1000;
            s = Math.floorDiv(ms, 1000);
            ms = ms % 1000;
        } else if (largestUnit.equals(MILLISECOND)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
            ms = Math.floorDiv(mus, 1000);
            mus = mus % 1000;
        } else if (largestUnit.equals(MICROSECOND)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
        } else {
            assert largestUnit.equals(NANOSECOND);
        }

        return JSTemporalDurationRecord.create(0, 0, d, h * sign, min * sign, s * sign, ms * sign, mus * sign, ns * sign);
    }

    // 7.5.14
    public static JSTemporalDurationRecord unbalanceDurationRelative(long y, long m, long w, long d,
                    String largestUnit, DynamicObject relTo, JSContext ctx) {
        long years = y;
        long months = m;
        long weeks = w;
        long days = d;
        DynamicObject relativeTo = relTo;
        if (largestUnit.equals(YEAR) || (years == 0 && months == 0 && weeks == 0 && days == 0)) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        }
        long sign = durationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        assert sign != 0;
        DynamicObject oneYear = JSTemporalDuration.createTemporalDuration(sign, 0, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
        DynamicObject oneMonth = JSTemporalDuration.createTemporalDuration(0, sign, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
        DynamicObject oneWeek = JSTemporalDuration.createTemporalDuration(0, 0, sign, 0, 0, 0, 0, 0, 0, 0, ctx);
        DynamicObject calendar = Undefined.instance;
        if (relativeTo != Undefined.instance) {
            assert JSObject.hasProperty(relativeTo, CALENDAR);
            calendar = (DynamicObject) JSObject.get(relativeTo, CALENDAR);
        }
        if (largestUnit.equals(MONTH)) {
            if (calendar == Undefined.instance) {
                throw Errors.createRangeError("No calendar provided.");
            }
            DynamicObject dateAdd = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject dateUntil = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_UNTIL);

            while (Math.abs(years) > 0) {
                DynamicObject addOptions = JSOrdinary.createWithNullPrototype(ctx);
                DynamicObject newRelativeTo = TemporalUtil.calendarDateAdd(calendar, relativeTo, oneYear, addOptions, dateAdd);

                DynamicObject untilOptions = JSOrdinary.createWithNullPrototype(ctx);
                JSObjectUtil.putDataProperty(ctx, untilOptions, "largestUnit", MONTH);
                DynamicObject untilResult = TemporalUtil.calendarDateUntil(calendar, relativeTo, newRelativeTo, untilOptions, dateUntil);
                long oneYearMonths = getLong(untilResult, MONTHS, 0);
                relativeTo = newRelativeTo;
                years = years - sign;
                months = months + oneYearMonths;
            }
        } else if (largestUnit.equals(WEEK)) {
            if (calendar == Undefined.instance) {
                throw Errors.createRangeError("Calendar should be not undefined.");
            }
            while (Math.abs(years) > 0) {
                JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneYear, ctx);
                relativeTo = moveResult.getRelativeTo();
                long oneYearDays = moveResult.getDays();
                years = years - sign;
                days = days + oneYearDays;
            }
            while (Math.abs(months) > 0) {
                JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
                relativeTo = moveResult.getRelativeTo();
                long oneMonthDays = moveResult.getDays();
                months = months - sign;
                days = days + oneMonthDays;
            }
        } else {
            if (years != 0 || months != 0 || days != 0) {
                if (calendar == Undefined.instance) {
                    throw Errors.createRangeError("Calendar should be not undefined.");
                }
                while (Math.abs(years) > 0) {
                    JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneYear, ctx);
                    relativeTo = moveResult.getRelativeTo();
                    long oneYearDays = moveResult.getDays();
                    years = years - sign;
                    days = days + oneYearDays;
                }
                while (Math.abs(months) > 0) {
                    JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
                    relativeTo = moveResult.getRelativeTo();
                    long oneMonthDays = moveResult.getDays();
                    months = months - sign;
                    days = days + oneMonthDays;
                }
                while (Math.abs(weeks) > 0) {
                    JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, ctx);
                    relativeTo = moveResult.getRelativeTo();
                    long oneWeekDays = moveResult.getDays();
                    weeks = weeks - sign;
                    days = days + oneWeekDays;
                }
            }
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    // 7.5.15
    public static JSTemporalDurationRecord balanceDurationRelative(long y, long m, long w, long d, String largestUnit, DynamicObject relTo, JSContext ctx) {
        long years = y;
        long months = m;
        long weeks = w;
        long days = d;
        DynamicObject relativeTo = relTo;
        if ((!largestUnit.equals(YEAR) && !largestUnit.equals(MONTH) && !largestUnit.equals(WEEK)) || (years == 0 && months == 0 && weeks == 0 && days == 0)) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        }
        long sign = durationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        assert sign != 0;
        DynamicObject oneYear = JSTemporalDuration.createTemporalDuration(sign, 0, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
        DynamicObject oneMonth = JSTemporalDuration.createTemporalDuration(0, sign, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
        DynamicObject oneWeek = JSTemporalDuration.createTemporalDuration(0, 0, sign, 0, 0, 0, 0, 0, 0, 0, ctx);
        if (relativeTo == Undefined.instance) {
            throw Errors.createRangeError("RelativeTo should not be null.");
        }
        assert JSObject.hasProperty(relativeTo, CALENDAR);
        DynamicObject calendar = (DynamicObject) JSObject.get(relativeTo, CALENDAR);
        if (largestUnit.equals(YEAR)) {
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
            relativeTo = moveResult.getRelativeTo();
            long oneYearDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneYearDays)) {
                days = days - oneYearDays;
                years = years + sign;
                moveResult = moveRelativeDate(calendar, relativeTo, oneYear, ctx);
                relativeTo = moveResult.getRelativeTo();
                oneYearDays = moveResult.getDays();
            }
            moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
            relativeTo = moveResult.getRelativeTo();
            long oneMonthDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneMonthDays)) {
                days = days - oneMonthDays;
                months = months + sign;
                moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
                relativeTo = moveResult.getRelativeTo();
                oneMonthDays = moveResult.getDays();
            }

            DynamicObject dateAdd = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject options = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject newRelativeTo = TemporalUtil.calendarDateAdd(calendar, relativeTo, oneYear, options, dateAdd);

            DynamicObject dateUntil = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_UNTIL);
            options = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(ctx, options, TemporalConstants.LARGEST_UNIT, MONTH);
            DynamicObject untilResult = TemporalUtil.calendarDateUntil(calendar, relativeTo, newRelativeTo, options, dateUntil);

            long oneYearMonths = getLong(untilResult, MONTHS, 0);
            while (Math.abs(months) >= Math.abs((oneYearMonths))) {
                months = months - oneYearMonths;
                years = years + sign;
                relativeTo = newRelativeTo;

                options = JSOrdinary.createWithNullPrototype(ctx);
                newRelativeTo = TemporalUtil.calendarDateAdd(calendar, relativeTo, oneYear, options, dateAdd);
                options = JSOrdinary.createWithNullPrototype(ctx);
                untilResult = TemporalUtil.calendarDateUntil(calendar, relativeTo, newRelativeTo, options, dateUntil);
                oneYearMonths = getLong(untilResult, MONTHS, 0);
            }
        } else if (largestUnit.equals(MONTH)) {
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
            relativeTo = moveResult.getRelativeTo();
            long oneMonthDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneMonthDays)) {
                days = days - oneMonthDays;
                months = months + sign;
                moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
                relativeTo = moveResult.getRelativeTo();
                oneMonthDays = moveResult.getDays();
            }
        } else {
            assert largestUnit.equals(WEEK);
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, ctx);
            relativeTo = moveResult.getRelativeTo();
            long oneWeekDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneWeekDays)) {
                days = days - oneWeekDays;
                weeks = weeks + sign;
                moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, ctx);
                relativeTo = moveResult.getRelativeTo();
                oneWeekDays = moveResult.getDays();
            }
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    // 7.5.16
    public static JSTemporalDurationRecord addDuration(long y1, long mon1, long w1, long d1, long h1, long min1, long s1, long ms1, long mus1, long ns1,
                    long y2, long mon2, long w2, long d2, long h2, long min2, long s2, long ms2, long mus2, long ns2,
                    DynamicObject relativeTo, JSContext ctx) {
        String largestUnit1 = defaultTemporalLargestUnit(y1, mon1, w1, d1, h1, min1, s1, ms1, mus1);
        String largestUnit2 = defaultTemporalLargestUnit(y2, mon2, w2, d2, h2, min2, s2, ms2, mus2);
        String largestUnit = TemporalUtil.largerOfTwoTemporalUnits(largestUnit1, largestUnit2);
        long years = 0;
        long months = 0;
        long weeks = 0;
        long days = 0;
        long hours = 0;
        long minutes = 0;
        long seconds = 0;
        long milliseconds = 0;
        long microseconds = 0;
        long nanoseconds = 0;
        if (relativeTo == Undefined.instance) {
            if (largestUnit.equals(YEAR) || largestUnit.equals(MONTH) || largestUnit.equals(WEEK)) {
                throw Errors.createRangeError("Largest unit allowed with no relative is 'days'.");
            }
            JSTemporalDurationRecord result = balanceDuration(d1 + d2, h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2,
                            ns1 + ns2, largestUnit, Undefined.instance);
            years = 0;
            months = 0;
            weeks = 0;
            days = result.getDays();
            hours = result.getHours();
            minutes = result.getMinutes();
            seconds = result.getSeconds();
            milliseconds = result.getMilliseconds();
            microseconds = result.getMicroseconds();
            nanoseconds = result.getNanoseconds();
        } else if (JSTemporalPlainDate.isJSTemporalPlainDate(relativeTo)) {
            DynamicObject calendar = (DynamicObject) JSObject.get(relativeTo, CALENDAR);
            DynamicObject datePart = JSTemporalPlainDate.createTemporalDate(ctx,
                            getLong(relativeTo, "ISOYear", 0L),
                            getLong(relativeTo, "ISOMonth", 0L),
                            getLong(relativeTo, "ISODay", 0L),
                            (DynamicObject) JSObject.get(relativeTo, "Calendar"));
            DynamicObject dateDuration1 = JSTemporalDuration.createTemporalDuration(y1, mon1, w1, d1, 0, 0, 0, 0, 0, 0, ctx);
            DynamicObject dateDuration2 = JSTemporalDuration.createTemporalDuration(y2, mon2, w2, d2, 0, 0, 0, 0, 0, 0, ctx);

            DynamicObject dateAdd = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject firstAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject intermediate = TemporalUtil.calendarDateAdd(calendar, datePart, dateDuration1, firstAddOptions, dateAdd);

            DynamicObject secondAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject end = TemporalUtil.calendarDateAdd(calendar, intermediate, dateDuration2, secondAddOptions, dateAdd);

            String dateLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(DAY, largestUnit);

            DynamicObject differenceOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(ctx, differenceOptions, LARGEST_UNIT, dateLargestUnit);
            JSTemporalDurationObject dateDifference = (JSTemporalDurationObject) TemporalUtil.calendarDateUntil(calendar, datePart, end, differenceOptions, Undefined.instance);
            JSTemporalDurationRecord result = balanceDuration(dateDifference.getDays(),
                            h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2, ns1 + ns2, largestUnit, Undefined.instance);
            years = dateDifference.getYears();
            months = dateDifference.getMonths();
            weeks = dateDifference.getWeeks();
            days = result.getDays();
            hours = result.getHours();
            minutes = result.getMinutes();
            seconds = result.getSeconds();
            milliseconds = result.getMilliseconds();
            microseconds = result.getMicroseconds();
            nanoseconds = result.getNanoseconds();
        } else {
            // TODO: Handle ZonedDateTime
        }
        if (!validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds,
                        microseconds, nanoseconds)) {
            throw Errors.createRangeError("Duration out of range!");
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    // 7.5.5
    public static boolean validateTemporalDuration(long years, long months, long weeks, long days, long hours,
                    long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds) {
        int sign = durationSign(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds,
                        nanoseconds);
        if (years < 0 && sign > 0) {
            return false;
        }
        if (years > 0 && sign < 0) {
            return false;
        }
        if (months < 0 && sign > 0) {
            return false;
        }
        if (months > 0 && sign < 0) {
            return false;
        }
        if (weeks < 0 && sign > 0) {
            return false;
        }
        if (weeks > 0 && sign < 0) {
            return false;
        }
        if (days < 0 && sign > 0) {
            return false;
        }
        if (days > 0 && sign < 0) {
            return false;
        }
        if (hours < 0 && sign > 0) {
            return false;
        }
        if (hours > 0 && sign < 0) {
            return false;
        }
        if (minutes < 0 && sign > 0) {
            return false;
        }
        if (minutes > 0 && sign < 0) {
            return false;
        }
        if (seconds < 0 && sign > 0) {
            return false;
        }
        if (seconds > 0 && sign < 0) {
            return false;
        }
        if (milliseconds < 0 && sign > 0) {
            return false;
        }
        if (milliseconds > 0 && sign < 0) {
            return false;
        }
        if (microseconds < 0 && sign > 0) {
            return false;
        }
        if (microseconds > 0 && sign < 0) {
            return false;
        }
        if (nanoseconds < 0 && sign > 0) {
            return false;
        }
        if (nanoseconds > 0 && sign < 0) {
            return false;
        }
        return true;
    }

    // 7.5.6
    public static String defaultTemporalLargestUnit(long years, long months, long weeks, long days, long hours,
                    long minutes, long seconds, long milliseconds, long microseconds) {
        if (years != 0) {
            return YEARS;
        }
        if (months != 0) {
            return MONTHS;
        }
        if (weeks != 0) {
            return WEEKS;
        }
        if (days != 0) {
            return DAYS;
        }
        if (hours != 0) {
            return HOURS;
        }
        if (minutes != 0) {
            return MINUTES;
        }
        if (seconds != 0) {
            return SECONDS;
        }
        if (milliseconds != 0) {
            return MILLISECONDS;
        }
        if (microseconds != 0) {
            return MICROSECONDS;
        }
        return NANOSECONDS;
    }

    // 7.5.7
    public static DynamicObject toPartialDuration(DynamicObject temporalDurationLike, JSContext ctx,
                    IsObjectNode isObjectNode, JSToIntegerAsLongNode toInt) {
        if (!isObjectNode.executeBoolean(temporalDurationLike)) {
            throw Errors.createTypeError("Given duration like is not a object.");
        }
        DynamicObject result = JSOrdinary.create(ctx);
        boolean any = false;
        for (String property : DURATION_PROPERTIES) {
            Object value = JSObject.get(temporalDurationLike, property);
            if (value != Undefined.instance) {
                any = true;
                JSObjectUtil.putDataProperty(ctx, result, property, toInt.executeLong(value));
            }
        }
        if (!any) {
            throw Errors.createTypeError("Given duration like object has no duration properties.");
        }
        return result;
    }

    // 7.5.18
    public static JSTemporalRelativeDateRecord moveRelativeDate(DynamicObject calendar, DynamicObject relativeTo, DynamicObject duration, JSContext ctx) {
        DynamicObject options = JSOrdinary.createWithNullPrototype(ctx);
        TemporalDate later = (TemporalDate) TemporalUtil.calendarDateAdd(calendar, relativeTo, duration, options, Undefined.instance);
        long days = daysUntil(relativeTo, (DynamicObject) later);
        DynamicObject dateTime = TemporalUtil.createTemporalDateTime(later.getISOYear(), later.getISOMonth(), later.getISODay(),
                        TemporalUtil.getLong(relativeTo, HOUR, 0), TemporalUtil.getLong(relativeTo, MINUTE, 0), TemporalUtil.getLong(relativeTo, SECOND, 0),
                        TemporalUtil.getLong(relativeTo, MILLISECOND, 0), TemporalUtil.getLong(relativeTo, MICROSECOND, 0), TemporalUtil.getLong(relativeTo, NANOSECOND, 0), calendar, ctx);
        return JSTemporalRelativeDateRecord.create(dateTime, days);
    }

    // 7.5.20
    public static JSTemporalDurationRecord roundDuration(long y, long m, long w, long d, long h, long min, long sec, long milsec, long micsec, long nsec,
                    long increment, String unit, String roundingMode, DynamicObject relTo, JSContext ctx) {
        long years = y;
        long months = m;
        long weeks = w;
        long days = d;
        long hours = h;
        long minutes = min;
        long seconds = sec;
        long microseconds = micsec;
        long milliseconds = milsec;
        long nanoseconds = nsec;
        DynamicObject relativeTo = relTo;
        if ((unit.equals(YEAR) || unit.equals(MONTH) || unit.equals(WEEK)) && relativeTo == Undefined.instance) {
            throw TemporalErrors.createRangeErrorRelativeToNotUndefined(unit);
        }
        DynamicObject zonedRelativeTo = Undefined.instance;
        DynamicObject calendar = Undefined.instance;
        double fractionalSeconds = 0;

        if (relativeTo != Undefined.instance) {
            // TODO: Check if relativeTo has InitializedTemporalZonedDateTime
            calendar = (DynamicObject) JSObject.get(relativeTo, "calendar");
        }
        if (unit.equals(YEAR) || unit.equals(MONTH) || unit.equals(WEEK) || unit.equals(DAY)) {
            nanoseconds = totalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
            DynamicObject intermediate = Undefined.instance;
            if (zonedRelativeTo != Undefined.instance) {
                // TODO: intermediate = moveRelativeZonedDateTime
            }
            JSTemporalNanosecondsDaysRecord result = nanosecondsToDays(nanoseconds, intermediate);
            days = days + result.getDays() +
                            (result.getNanoseconds() /
                                            Math.abs(result.getDayLength()));
            hours = 0;
            minutes = 0;
            seconds = 0;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else {
            fractionalSeconds = (nanoseconds * 0.000_000_001) + (microseconds * 0.000_001) + (milliseconds * 0.001) + seconds;
        }
        double remainder = 0;
        if (unit.equals(YEAR)) {
            DynamicObject yearsDuration = JSTemporalDuration.createTemporalDuration(years, 0, 0, 0, 0, 0, 0, 0, 0, 0, ctx);

            DynamicObject dateAdd = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject firstAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsDuration, firstAddOptions, dateAdd);
            DynamicObject yearsMonthsWeeks = JSTemporalDuration.createTemporalDuration(years, months, weeks, 0, 0, 0, 0, 0, 0, 0, ctx);

            DynamicObject secondAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsMonthsWeeksLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsMonthsWeeks, secondAddOptions, dateAdd);
            long monthsWeeksInDays = daysUntil(yearsLater, yearsMonthsWeeksLater);
            relativeTo = yearsLater;
            days = days + monthsWeeksInDays;
            long sign = TemporalUtil.sign(days);
            if (sign == 0) {
                sign = 1;
            }
            DynamicObject oneYear = JSTemporalDuration.createTemporalDuration(sign, 0, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneYear, ctx);
            relativeTo = moveResult.getRelativeTo();
            long oneYearDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneYearDays)) {
                years = years + sign;
                days = days - oneYearDays;
                moveResult = moveRelativeDate(calendar, relativeTo, oneYear, ctx);
                relativeTo = moveResult.getRelativeTo();
                oneYearDays = moveResult.getDays();
            }
            double fractionalYears = years + ((double) days / Math.abs(oneYearDays));
            years = (long) TemporalUtil.roundNumberToIncrement(fractionalYears, increment, roundingMode);
            remainder = fractionalYears - years;
            months = 0;
            weeks = 0;
            years = 0;
        } else if (unit.equals(MONTH)) {
            DynamicObject yearsMonths = JSTemporalDuration.createTemporalDuration(years, months, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
            DynamicObject dateAdd = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject firstAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsMonthsLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsMonths, firstAddOptions, dateAdd);
            DynamicObject yearsMonthsWeeks = JSTemporalDuration.createTemporalDuration(years, months, weeks, 0, 0, 0, 0, 0, 0, 0, ctx);
            DynamicObject secondAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsMonthsWeeksLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsMonthsWeeks, secondAddOptions, dateAdd);
            long weeksInDays = daysUntil(yearsMonthsLater, yearsMonthsWeeksLater);
            relativeTo = yearsMonthsLater;
            days = days + weeksInDays;
            long sign = TemporalUtil.sign(days);
            if (sign == 0) {
                sign = 1;
            }
            DynamicObject oneMonth = JSTemporalDuration.createTemporalDuration(0, sign, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
            relativeTo = moveResult.getRelativeTo();
            long oneMonthDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneMonthDays)) {
                months = months + sign;
                days = days - oneMonthDays;
                moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
                relativeTo = moveResult.getRelativeTo();
                oneMonthDays = moveResult.getDays();
            }
            double fractionalMonths = months + ((double) days / Math.abs(oneMonthDays));
            months = (long) TemporalUtil.roundNumberToIncrement(fractionalMonths, increment, roundingMode);
            remainder = fractionalMonths - months;
            weeks = 0;
            days = 0;
        } else if (unit.equals(WEEK)) {
            long sign = TemporalUtil.sign(days);
            if (sign == 0) {
                sign = 1;
            }
            DynamicObject oneWeek = JSTemporalDuration.createTemporalDuration(0, 0, sign, 0, 0, 0, 0, 0, 0, 0, ctx);
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, ctx);
            relativeTo = moveResult.getRelativeTo();
            long oneWeekDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneWeekDays)) {
                weeks = weeks - sign;
                days = days - oneWeekDays;
                moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, ctx);
                relativeTo = moveResult.getRelativeTo();
                oneWeekDays = moveResult.getDays();
            }
            double fractionalWeeks = weeks + ((double) days / Math.abs(oneWeekDays));
            weeks = (long) TemporalUtil.roundNumberToIncrement(fractionalWeeks, increment, roundingMode);
            remainder = fractionalWeeks - weeks;
            days = 0;
        } else if (unit.equals(DAY)) {
            double fractionalDays = days;
            days = (long) TemporalUtil.roundNumberToIncrement(fractionalDays, increment, roundingMode);
            remainder = fractionalDays - days;
        } else if (unit.equals(HOUR)) {
            double fractionalHours = (((fractionalSeconds / 60) + minutes) / 60) + hours;
            hours = (long) TemporalUtil.roundNumberToIncrement(fractionalHours, increment, roundingMode);
            remainder = fractionalHours - hours;
            minutes = 0;
            seconds = 0;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit.equals(MINUTE)) {
            double fractionalMinutes = (fractionalSeconds / 60) + minutes;
            minutes = (long) TemporalUtil.roundNumberToIncrement(fractionalMinutes, increment, roundingMode);
            remainder = fractionalMinutes - minutes;
            seconds = 0;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit.equals(SECOND)) {
            seconds = (long) TemporalUtil.roundNumberToIncrement(fractionalSeconds, increment, roundingMode);
            remainder = fractionalSeconds - seconds;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit.equals(MILLISECOND)) {
            double fractionalMilliseconds = (nanoseconds * 0.000_000_1) + (microseconds * 0.000_1) + milliseconds;
            milliseconds = (long) TemporalUtil.roundNumberToIncrement(fractionalMilliseconds, increment, roundingMode);
            remainder = fractionalMilliseconds - milliseconds;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit.equals(MICROSECOND)) {
            double fractionalMicroseconds = (nanoseconds * 0.000_1) + microseconds;
            microseconds = (long) TemporalUtil.roundNumberToIncrement(fractionalMicroseconds, increment, roundingMode);
            remainder = fractionalMicroseconds - microseconds;
            nanoseconds = 0;
        } else {
            assert unit.equals(NANOSECOND);
            remainder = nanoseconds;
            nanoseconds = (long) TemporalUtil.roundNumberToIncrement(nanoseconds, increment, roundingMode);
            remainder = remainder - nanoseconds;
        }

        return JSTemporalDurationRecord.createWeeksRemainder(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, remainder);
    }

    @SuppressWarnings("unused")
    private static JSTemporalNanosecondsDaysRecord nanosecondsToDays(long nanoseconds, DynamicObject relativeTo) {
        long sign = TemporalUtil.sign(nanoseconds);
        double dayLengthNs = 86_400_000_000_000L;
        if (sign == 0) {
            return JSTemporalNanosecondsDaysRecord.create(0, 0, (long) dayLengthNs);
        }
        if (!TemporalUtil.isTemporalZonedDateTime(relativeTo)) {
            double val = nanoseconds / dayLengthNs;
            double val2 = nanoseconds % dayLengthNs;
            return JSTemporalNanosecondsDaysRecord.create((long) val, (long) val2, (long) (sign * dayLengthNs));
        }
        // TODO
        // 5. Let startNs be relativeTo.[[Nanoseconds]].
        // 6. Let startInstant be ? CreateTemporalInstant(startNs).
        // 7. Let startDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(relativeTo.[[TimeZone]],
        // startInstant, relativeTo.[[Calendar]]).
        // 8. Let endNs be startNs + nanoseconds.
        // 9. Let endInstant be ? CreateTemporalInstant(endNs).
        // 10. Let endDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(relativeTo.[[TimeZone]],
        // endInstant,
        // relativeTo.[[Calendar]]).
        // 11. Let dateDifference be ? DifferenceISODateTime(startDateTime.[[ISOYear]],
        // startDateTime.[[ISOMonth]], startDateTime.[[ISODay]], startDateTime.[[ISOHour]],
        // startDateTime.[[ISOMinute]], startDateTime.[[ISOSecond]],
        // startDateTime.[[ISOMillisecond]],
        // startDateTime.[[ISOMicrosecond]], startDateTime.[[ISONanosecond]],
        // endDateTime.[[ISOYear]],
        // endDateTime.[[ISOMonth]], endDateTime.[[ISODay]], endDateTime.[[ISOHour]],
        // endDateTime.[[ISOMinute]], endDateTime.[[ISOSecond]], endDateTime.[[ISOMillisecond]],
        // endDateTime.[[ISOMicrosecond]], endDateTime.[[ISONanosecond]], relativeTo.[[Calendar]],
        // "days").
        // 12. Let days be dateDifference.[[Days]].
        // 13. Let intermediateNs be ? AddZonedDateTime(startNs, relativeTo.[[TimeZone]],
        // relativeTo.[[Calendar]], 0, 0, 0, days, 0, 0, 0, 0, 0, 0).
        if (sign == 1) {
            // a. Repeat, while days > 0 and intermediateNs > endNs,
            // i. Set days to days - 1.
            // ii. Set intermediateNs to ? AddZonedDateTime(startNs, relativeTo.[[TimeZone]],
            // relativeTo.[[Calendar]], 0, 0, 0, days, 0, 0, 0, 0, 0, 0).
        }
        // 15. Set nanoseconds to endNs - intermediateNs.
        // 16. Let done be false.
        // 17. Repeat, while done is false,
        // a. Let oneDayFartherNs be ? AddZonedDateTime(intermediateNs, relativeTo.[[TimeZone]],
        // relativeTo.[[Calendar]], 0, 0, 0, sign, 0, 0, 0, 0, 0, 0).
        // b. Set dayLengthNs to oneDayFartherNs - intermediateNs.
        // c. If (nanoseconds - dayLengthNs) * sign >= 0, then
        // i. Set nanoseconds to nanoseconds - dayLengthNs.
        // ii. Set intermediateNs to oneDayFartherNs.
        // iii. Set days to days + sign.
        // d. Else,
        // i. Set done to true.
        // 18. Return the new Record { [[Days]]: days, [[Nanoseconds]]: nanoseconds, [[DayLength]]:
// dayLengthNs }.

        return JSTemporalNanosecondsDaysRecord.create(0, 0, 0);
    }

    // 7.5.21
    public static JSTemporalDurationRecord adjustRoundedDurationDays(long years, long months, long weeks, long days, long hours,
                    long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds, long increment, String unit,
                    String roundingMode, DynamicObject relativeTo,
                    JSContext ctx) {
        if (!(TemporalUtil.isTemporalZonedDateTime(relativeTo)) || unit.equals(YEARS) || unit.equals(MONTHS) || unit.equals(WEEKS) || unit.equals(DAYS) ||
                        (unit.equals(NANOSECONDS) && increment == 1)) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        long timeRemainderNs = totalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
        long direction = TemporalUtil.sign(timeRemainderNs);
        long dayStart = TemporalUtil.addZonedDateTime(
                        getLong(relativeTo, NANOSECONDS),
                        JSObject.get(relativeTo, TIME_ZONE),
                        (DynamicObject) JSObject.get(relativeTo, CALENDAR), years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        long dayEnd = TemporalUtil.addZonedDateTime(dayStart, JSObject.get(relativeTo, TIME_ZONE), (DynamicObject) JSObject.get(relativeTo, CALENDAR), 0, 0, 0, direction, 0, 0, 0, 0, 0, 0);
        long dayLengthNs = dayEnd - dayStart;
        if ((timeRemainderNs - dayLengthNs) * direction < 0) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        timeRemainderNs = TemporalUtil.roundTemporalInstant(timeRemainderNs - dayLengthNs, increment, unit, roundingMode);
        JSTemporalDurationRecord add = addDuration(years, months, weeks, days, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, direction, 0, 0, 0, 0, 0, 0,
                        relativeTo, ctx);
        JSTemporalDurationRecord atd = balanceDuration(0, 0, 0, 0, 0, 0, timeRemainderNs, "hours", Undefined.instance);

        return JSTemporalDurationRecord.createWeeks(add.getYears(), add.getMonths(), add.getWeeks(), add.getDays(),
                        atd.getHours(), atd.getMinutes(), atd.getSeconds(), atd.getMilliseconds(), atd.getMicroseconds(), atd.getNanoseconds());
    }

    // 7.5.12
    public static long totalDurationNanoseconds(long days, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds, long offsetShift) {
        long ns = nanoseconds;
        if (days != 0) {
            ns -= offsetShift;
        }
        long h = hours + days * 24;
        long min = minutes + h * 60;
        long s = seconds + min * 60;
        long ms = milliseconds + s * 1000;
        long mus = microseconds + ms * 1000;
        return ns + mus * 1000;
    }

    // 7.5.11
    @SuppressWarnings("unused")
    public static long calculateOffsetShift(DynamicObject relativeTo, long y, long mon, long w, long d, long h, long min,
                    long s, long ms, long mus, long ns, IsObjectNode isObject) {
        if (isObject.executeBoolean(relativeTo)) { // TODO: Check if there is an internal slot for
                                                   // InitializedTemporalZoneDateTime
            return 0;
        }
        DynamicObject instant = null;   // TODO: Call JSTemporalInstant.createTemporalInstant()
        long offsetBefore = 0;          // TODO: Call JSTemporalTimeZone.getOffsetNanoSecondsFor()
        long after = 0;                 // TODO: Call JSTemporalZonedDateTime.addZonedDateTime()
        DynamicObject instantAfter = null;  // TODO: Call JSTemporalInstant.createTemporalInstant()
        long offsetAfter = 0;           // TODO: Call JSTemporalTimeZone.getOffsetNanoSecondsFor()
        return offsetAfter - offsetBefore;
    }

    // 7.5.17
    public static long daysUntil(DynamicObject earlier, DynamicObject later) {
        assert earlier instanceof TemporalDate && later instanceof TemporalDate;
        TemporalDate ed = (TemporalDate) earlier;
        TemporalDate ld = (TemporalDate) later;
        JSTemporalDurationRecord difference = JSTemporalPlainDate.differenceISODate(ed.getISOYear(), ed.getISOMonth(), ed.getISODay(), ld.getISOYear(), ld.getISOMonth(), ld.getISODay(), DAY);
        return difference.getDays();
    }

    // 4.5.1
    public static JSTemporalDurationRecord differenceTime(long h1, long min1, long s1, long ms1, long mus1, long ns1,
                    long h2, long min2, long s2, long ms2, long mus2, long ns2) {
        long hours = h2 - h1;
        long minutes = min2 - min1;
        long seconds = s2 - s1;
        long milliseconds = ms2 - ms1;
        long microseconds = mus2 - mus1;
        long nanoseconds = ns2 - ns1;
        long sign = durationSign(0, 0, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        JSTemporalDurationRecord bt = balanceTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds);

        return JSTemporalDurationRecord.create(0, 0, bt.getDays() * sign, bt.getHours() * sign, bt.getMinutes() * sign, bt.getSeconds() * sign,
                        bt.getMilliseconds() * sign, bt.getMicroseconds() * sign, bt.getNanoseconds() * sign);
    }

    // 4.5.15
    public static JSTemporalDurationRecord roundTime(long hours, long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds, double increment, String unit, String roundingMode,
                    Long dayLengthNsParam) {
        double fractionalSecond = ((double) nanoseconds / 1_000_000_000) + ((double) microseconds / 1_000_000) +
                        ((double) milliseconds / 1_000) + seconds;
        double quantity;
        if (unit.equals(DAY)) {
            long dayLengthNs = dayLengthNsParam == null ? 86_300_000_000_000L : (long) dayLengthNsParam;
            quantity = ((double) (((((hours * 60 + minutes) * 60 + seconds) * 1000 + milliseconds) * 1000 + microseconds) * 1000 + nanoseconds)) / dayLengthNs;
        } else if (unit.equals(HOUR)) {
            quantity = (fractionalSecond / 60 + minutes) / 60 + hours;
        } else if (unit.equals(MINUTE)) {
            quantity = fractionalSecond / 60 + minutes;
        } else if (unit.equals(SECOND)) {
            quantity = fractionalSecond;
        } else if (unit.equals(MILLISECOND)) {
            quantity = ((double) nanoseconds / 1_000_000) + ((double) microseconds / 1_000) + milliseconds;
        } else if (unit.equals(MICROSECOND)) {
            quantity = ((double) nanoseconds / 1_000) + microseconds;
        } else {
            assert unit.equals(NANOSECOND);
            quantity = nanoseconds;
        }
        long result = (long) TemporalUtil.roundNumberToIncrement(quantity, increment, roundingMode);
        if (unit.equals(DAY)) {
            return JSTemporalDurationRecord.create(0, 0, result, 0, 0, 0, 0, 0, 0);
        }
        if (unit.equals(HOUR)) {
            return balanceTime(result, 0, 0, 0, 0, 0);
        }
        if (unit.equals(MINUTE)) {
            return balanceTime(hours, result, 0, 0, 0, 0);
        }
        if (unit.equals(SECOND)) {
            return balanceTime(hours, minutes, result, 0, 0, 0);
        }
        if (unit.equals(MILLISECOND)) {
            return balanceTime(hours, minutes, seconds, result, 0, 0);
        }
        if (unit.equals(MICROSECOND)) {
            return balanceTime(hours, minutes, seconds, milliseconds, result, 0);
        }
        assert unit.equals(NANOSECOND);
        return balanceTime(hours, minutes, seconds, milliseconds, microseconds, result);
    }

    // 4.5.6
    public static JSTemporalDurationRecord balanceTime(long h, long min, long sec, long mils, long mics, long ns) {
        if (h == Double.POSITIVE_INFINITY || h == Double.NEGATIVE_INFINITY ||
                        min == Double.POSITIVE_INFINITY || min == Double.NEGATIVE_INFINITY ||
                        sec == Double.POSITIVE_INFINITY || sec == Double.NEGATIVE_INFINITY ||
                        mils == Double.POSITIVE_INFINITY || mils == Double.NEGATIVE_INFINITY ||
                        mics == Double.POSITIVE_INFINITY || mics == Double.NEGATIVE_INFINITY ||
                        ns == Double.POSITIVE_INFINITY || ns == Double.NEGATIVE_INFINITY) {
            throw Errors.createRangeError("Time is infinite");
        }
        long microseconds = mics;
        long milliseconds = mils;
        long nanoseconds = ns;
        long seconds = sec;
        long minutes = min;
        long hours = h;
        microseconds = microseconds + (long) Math.floor(nanoseconds / 1000.0);
        nanoseconds = (long) TemporalUtil.nonNegativeModulo(nanoseconds, 1000);
        milliseconds = milliseconds + (long) Math.floor(microseconds / 1000.0);
        microseconds = (long) TemporalUtil.nonNegativeModulo(microseconds, 1000);
        seconds = seconds + (long) Math.floor(milliseconds / 1000.0);
        milliseconds = (long) TemporalUtil.nonNegativeModulo(milliseconds, 1000);
        minutes = minutes + (long) Math.floor(seconds / 60.0);
        seconds = (long) TemporalUtil.nonNegativeModulo(seconds, 60);
        hours = hours + (long) Math.floor(minutes / 60.0);
        minutes = (long) TemporalUtil.nonNegativeModulo(minutes, 60);
        long days = (long) Math.floor(hours / 24.0);
        hours = (long) TemporalUtil.nonNegativeModulo(hours, 24);

        // TODO [[Days]] is plural, rest is singular WTF
        return JSTemporalDurationRecord.create(0, 0, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    // 4.5.13
    public static int compareTemporalTime(long h1, long min1, long s1, long ms1, long mus1, long ns1,
                    long h2, long min2, long s2, long ms2, long mus2, long ns2) {
        if (h1 > h2) {
            return 1;
        }
        if (h1 < h2) {
            return -1;
        }
        if (min1 > min2) {
            return 1;
        }
        if (min1 < min2) {
            return -1;
        }
        if (s1 > s2) {
            return 1;
        }
        if (s1 < s2) {
            return -1;
        }
        if (ms1 > ms2) {
            return 1;
        }
        if (ms1 < ms2) {
            return -1;
        }
        if (mus1 > mus2) {
            return 1;
        }
        if (mus1 < mus2) {
            return -1;
        }
        if (ns1 > ns2) {
            return 1;
        }
        if (ns1 < ns2) {
            return -1;
        }
        return 0;
    }

    // 4.5.14
    public static JSTemporalDurationRecord addTime(long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds) {
        return balanceTime(hour + hours, minute + minutes, second + seconds, millisecond + milliseconds,
                        microsecond + microseconds, nanosecond + nanoseconds);
    }

    public static JSTemporalDurationRecord roundISODateTime(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond, double increment, String unit, String roundingMode, Long dayLength) {
        JSTemporalDurationRecord rt = TemporalUtil.roundTime(hour, minute, second, millisecond, microsecond, nanosecond, increment, unit, roundingMode, dayLength);
        JSTemporalDateTimeRecord br = TemporalUtil.balanceISODate(year, month, day + rt.getDays());
        return JSTemporalDurationRecord.create(br.getYear(), br.getMonth(), br.getDay(),
                        rt.getHours(), rt.getMinutes(), rt.getSeconds(),
                        rt.getMilliseconds(), rt.getMicroseconds(), rt.getNanoseconds());
    }

    public static double toTemporalDateTimeRoundingIncrement(DynamicObject options, String smallestUnit, IsObjectNode isObject, JSToNumberNode toNumber) {
        int maximum = 0;
        if (DAY.equals(smallestUnit)) {
            maximum = 1;
        } else if (HOUR.equals(smallestUnit)) {
            maximum = 24;
        } else if (MINUTE.equals(smallestUnit) || SECOND.equals(smallestUnit)) {
            maximum = 60;
        } else {
            assert MILLISECOND.equals(smallestUnit) || MICROSECOND.equals(smallestUnit) || NANOSECOND.equals(smallestUnit);
            maximum = 1000;
        }
        return toTemporalRoundingIncrement(options, (double) maximum, false, isObject, toNumber);
    }

    public static boolean isValidTime(long hours, long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds) {
        if (hours < 0 || hours > 23) {
            return false;
        }
        if (minutes < 0 || minutes > 59) {
            return false;
        }
        if (seconds < 0 || seconds > 59) {
            return false;
        }
        if (milliseconds < 0 || milliseconds > 999) {
            return false;
        }
        if (microseconds < 0 || microseconds > 999) {
            return false;
        }
        if (nanoseconds < 0 || nanoseconds > 999) {
            return false;
        }
        return true;
    }

    public static boolean isValidISODate(long year, long month, long day) {
        if (month < 1 || month > 12) {
            return false;
        }
        if (day < 1 || day > isoDaysInMonth(year, month)) {
            return false;
        }
        return true;
    }

    public static boolean dateTimeWithinLimits(long year, long month, long day, long hour, long minute, long second,
                    long millisecond, long microsecond, long nanosecond) {
        double ns = TemporalUtil.getEpochFromISOParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
        if ((ns / 100_000_000_000_000L) <= -864_000L - 864L) {
            return false;
        } else if ((ns / 100_000_000_000_000L) >= 864_000L + 864L) {
            return false;
        }
        return true;
    }
}
