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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.AUTO;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CEIL;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.COMPATIBLE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CONSTRAIN;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ERA;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ERA_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.FLOOR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ISO8601;
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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.OFFSET;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.OVERFLOW;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.REJECT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
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
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.JSTemporalCalendarObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainMonthDay;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainMonthDayObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainYearMonth;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainYearMonthObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimePluralRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDate;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalTime;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
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

    private static final Set<String> singularUnits = toSet(YEAR, MONTH, DAY, HOUR, MINUTE, SECOND,
                    MILLISECOND, MICROSECOND, NANOSECOND);
    private static final Set<String> pluralUnits = toSet(YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS,
                    MILLISECONDS, MICROSECONDS, NANOSECONDS);
    private static final Map<String, String> pluralToSingular = toMap(
                    new String[]{YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS},
                    new String[]{YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND});
    private static final Map<String, String> singularToPlural = toMap(
                    new String[]{YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND},
                    new String[]{YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS});
    private static final Map<String, Function<Object, Object>> temporalFieldConversion = toMap(
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

    public static final String[] TIME_LIKE_PROPERTIES = new String[]{HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND};

    // 13.1
    public static DynamicObject getOptionsObject(DynamicObject options,
                    JSRealm realm,
                    IsObjectNode isObject) {
        if (JSRuntime.isNullOrUndefined(options)) {
            DynamicObject newOptions = JSObjectUtil.createOrdinaryPrototypeObject(realm);
            return newOptions;
        }
        if (isObject.executeBoolean(options)) {
            return options;
        }
        throw TemporalErrors.createTypeErrorOptions();
    }

    public static DynamicObject getOptionsObject(Object opt, JSContext ctx) {
        if (opt == Undefined.instance) {
            return JSObjectUtil.createOrdinaryPrototypeObject(ctx.getRealm(), Null.instance);
        }
        if (JSRuntime.isObject(opt)) {
            return (DynamicObject) opt;
        }
        throw TemporalErrors.createTypeErrorOptions();
    }

    // 13.2
    public static Object getOption(DynamicObject options, String property, String type, Set<Object> values, Object fallback, IsObjectNode isObjectNode, JSToBooleanNode toBoolean,
                    JSToStringNode toStringNode) {
        assert isObjectNode.executeBoolean(options);
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
        if (values != Undefined.instance && !values.contains(value)) {
            throw Errors.createRangeError(
                            String.format("Given options value: %s is not contained in values: %s", value, values));
        }
        return value;
    }

    // 13.3
    public static double defaultNumberOptions(Object value, double minimum, double maximum, double fallback,
                    JSToNumberNode toNumber) {
        if (value == Undefined.instance) {
            return fallback;
        }
        double numberValue = toNumber.executeNumber(value).doubleValue();
        if (numberValue == Double.NaN || numberValue < minimum || numberValue > maximum) {
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
    public static Object getStringOrNumberOption(DynamicObject options, String property, Set<String> stringValues,
                    double minimum, double maximum, Object fallback) {
        assert JSRuntime.isObject(options);
        Object value = JSObject.get(options, property);
        if (value == Undefined.instance) {
            return fallback;
        }
        if (value instanceof Number) {
            double numberValue = ((Number) value).doubleValue();
            if (numberValue == Double.NaN || numberValue < minimum || numberValue > maximum) {
                throw Errors.createRangeError("Numeric value out of range.");
            }
            return Math.floor(numberValue);
        }
        value = JSRuntime.toString(value);
        if (stringValues != Undefined.instance && !stringValues.contains(value)) {
            throw Errors.createRangeError("Given string value is not in string values");
        }
        return value;
    }

    // 13.8
    public static String toTemporalOverflow(DynamicObject options,
                    IsObjectNode isObjectNode,
                    JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        Set<Object> values = new HashSet<>();
        values.add(CONSTRAIN);
        values.add(REJECT);
        return (String) getOption(options, "overflow", "string", values, CONSTRAIN, isObjectNode, toBoolean, toStringNode);
    }

    // 13.11
    public static String toTemporalRoundingMode(DynamicObject options, String fallback, IsObjectNode isObjectNode,
                    JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        return (String) getOption(options, "roundingMode", "string", toSet("ceil", "floor", "trunc", "nearest"),
                        fallback, isObjectNode, toBoolean, toStringNode);
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

    public static JSTemporalPrecisionRecord toSecondsStringPrecision(DynamicObject options) {
        return toSecondsStringPrecisionIntl(options, true, false);
    }

    public static JSTemporalPrecisionRecord toDurationSecondsStringPrecision(DynamicObject options) {
        return toSecondsStringPrecisionIntl(options, false, true);
    }

    private static JSTemporalPrecisionRecord toSecondsStringPrecisionIntl(DynamicObject options, boolean supportMinutes, boolean plural) {
        Set<Object> set = TemporalUtil.toSet(SECONDS, MILLISECONDS, MICROSECONDS, SECOND, MILLISECOND, MICROSECOND, NANOSECOND);
        if (supportMinutes) {
            set.add(MINUTE);
            set.add(MINUTES);
        }
        Object smallestUnit = TemporalUtil.getOption(options, "smallestUnit", "string", set, Undefined.instance);

        smallestUnit = TemporalUtil.toPlural(smallestUnit);

        if (supportMinutes && MINUTES.equals(smallestUnit)) {
            return JSTemporalPrecisionRecord.create(MINUTE, plural ? MINUTES : MINUTE, 1);
        } else if (SECONDS.equals(smallestUnit)) {
            return JSTemporalPrecisionRecord.create(0, plural ? SECONDS : SECOND, 1);
        } else if (MILLISECONDS.equals(smallestUnit)) {
            return JSTemporalPrecisionRecord.create(3, plural ? MILLISECONDS : MILLISECOND, 1);
        } else if (MICROSECONDS.equals(smallestUnit)) {
            return JSTemporalPrecisionRecord.create(6, plural ? MICROSECONDS : MICROSECOND, 1);
        } else if (NANOSECONDS.equals(smallestUnit)) {
            return JSTemporalPrecisionRecord.create(9, plural ? NANOSECONDS : NANOSECOND, 1);
        }

        assert smallestUnit == Undefined.instance;

        Object digits = TemporalUtil.getStringOrNumberOption(options, "fractionalSecondDigits", TemporalUtil.toSet(AUTO), 0, 9, AUTO);
        if (digits.equals(AUTO)) {
            return JSTemporalPrecisionRecord.create(AUTO, plural ? NANOSECONDS : NANOSECOND, 1);
        }
        if (digits.equals(0)) {
            return JSTemporalPrecisionRecord.create(0, plural ? SECONDS : SECONDS, 1);
        }
        if (digits.equals(1) || digits.equals(2) || digits.equals(3)) {
            return JSTemporalPrecisionRecord.create(digits, plural ? MILLISECONDS : MILLISECOND, Math.pow(10, 3 - (long) digits));
        }
        if (digits.equals(4) || digits.equals(5) || digits.equals(6)) {
            return JSTemporalPrecisionRecord.create(digits, plural ? MICROSECONDS : MICROSECOND, Math.pow(10, 6 - (long) digits));
        }
        assert digits.equals(7) || digits.equals(8) || digits.equals(9);
        return JSTemporalPrecisionRecord.create(digits, plural ? NANOSECONDS : NANOSECOND, Math.pow(10, 9 - (long) digits));
    }

    // 13.21
    public static String toLargestTemporalUnit(DynamicObject normalizedOptions, Set<String> disallowedUnits, String defaultUnit,
                    IsObjectNode isObjectNode,
                    JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        assert !disallowedUnits.contains(defaultUnit) && !disallowedUnits.contains("auto");
        String largestUnit = (String) getOption(normalizedOptions, "largestUnit", "string", toSet(
                        "auto", "year", "years", "month", "months", "week", "weeks", "day", "days", "hour",
                        "hours", "minute", "minutes", "second", "seconds", "millisecond", "milliseconds", "microsecond",
                        "microseconds", "nanosecond", "nanoseconds"), "auto", isObjectNode, toBoolean, toStringNode);
        if (largestUnit.equals("auto")) {
            return defaultUnit;
        }
        if (singularUnits.contains(largestUnit)) {
            largestUnit = singularToPlural.get(largestUnit);
        }
        if (disallowedUnits.contains(largestUnit)) {
            throw Errors.createRangeError("Largest unit is not allowed.");
        }
        return largestUnit;
    }

    // 13.22
    public static String toSmallestTemporalUnit(DynamicObject normalizedOptions, Set<String> disallowedUnits,
                    IsObjectNode isObjectNode, JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        String smallestUnit = (String) getOption(normalizedOptions, "smallestUnit", "string", toSet("day", "days", "hour",
                        "hours", "minute", "minutes", "second", "seconds", "millisecond", "milliseconds", "microsecond",
                        "microseconds", "nanosecond", "nanoseconds"), null, isObjectNode, toBoolean, toStringNode);
        if (smallestUnit == null) {
            throw Errors.createRangeError("No smallest unit found.");
        }
        if (pluralUnits.contains(smallestUnit)) {
            smallestUnit = pluralToSingular.get(smallestUnit);
        }
        if (disallowedUnits.contains(smallestUnit)) {
            throw Errors.createRangeError("Smallest unit not allowed.");
        }
        return smallestUnit;
    }

    // 13.23
    public static String toSmallestTemporalDurationUnit(DynamicObject normalizedOptions, String fallback, Set<String> disallowedUnits,
                    IsObjectNode isObjectNode,
                    JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        String smallestUnit = (String) getOption(normalizedOptions, "smallestUnit", "string", toSet(
                        "year", "years", "month", "months", "week", "weeks", "day", "days", "hour",
                        "hours", "minute", "minutes", "second", "seconds", "millisecond", "milliseconds", "microsecond",
                        "microseconds", "nanosecond", "nanoseconds"), fallback, isObjectNode, toBoolean, toStringNode);
        if (singularUnits.contains(smallestUnit)) {
            smallestUnit = singularToPlural.get(smallestUnit);
        }
        if (disallowedUnits.contains(smallestUnit)) {
            throw Errors.createRangeError("Smallest unit not allowed.");
        }
        return smallestUnit;
    }

    // 13.24
    public static String toTemporalDurationTotalUnit(DynamicObject normalizedOptions, IsObjectNode isObjectNode, JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        String unit = (String) getOption(normalizedOptions, "unit", "string", toSet(YEARS, YEAR, MONTHS, MONTH, WEEKS,
                        DAYS, DAY, HOURS, HOUR, MINUTES, MINUTE, SECONDS, SECOND, MILLISECONDS, MILLISECOND, MICROSECONDS,
                        MICROSECOND, NANOSECONDS, NANOSECONDS),
                        null, isObjectNode, toBoolean, toStringNode);
        if (singularUnits.contains(unit)) {
            unit = singularToPlural.get(unit);
        }
        return unit;
    }

    // 13.26
    public static DynamicObject toRelativeTemporalObject(DynamicObject options, JSContext ctx) {
        Object value = JSObject.get(options, "relativeTo");
        if (value == Undefined.instance) {
            return Undefined.instance;
        }
        JSTemporalPlainDateTimeRecord result = null;
        Object timeZone = null;
        DynamicObject calendar = null;
        Object offset = null;
        if (JSRuntime.isObject(value)) {
            if (value instanceof JSTemporalPlainDateTimeObject) {
                return (DynamicObject) value;
            }
            // if (value instanceof JSTemporalPlainDateTimeObject) {
            // return (DynamicObject) value;
            // }
            if (value instanceof JSTemporalPlainDateObject) {
                JSTemporalPlainDateObject pd = (JSTemporalPlainDateObject) value;
                return JSTemporalPlainDateTime.createTemporalDateTime(ctx, pd.getISOYear(), pd.getISOMonth(), pd.getISODay(), 0, 0, 0, 0, 0, 0, pd.getCalendar());
            }
            calendar = TemporalUtil.getOptionalTemporalCalendar((DynamicObject) value, ctx);
            Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{DAY, MONTH, MONTH_CODE, YEAR}, ctx);
            DynamicObject fields = TemporalUtil.prepareTemporalFields((DynamicObject) value, fieldNames, new HashSet<>(), ctx);

            DynamicObject dateOptions = JSObjectUtil.createOrdinaryPrototypeObject(ctx.getRealm(), Null.instance);
            JSObjectUtil.putDataProperty(ctx, dateOptions, OVERFLOW, CONSTRAIN);
            result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, dateOptions, ctx);
            offset = JSObject.get((DynamicObject) value, OFFSET);
            timeZone = JSObject.get((DynamicObject) value, TIME_ZONE);
        } else {
            String string = JSRuntime.toString(value);
            result = parseISODateTime(string, ctx);
            calendar = toOptionalTemporalCalendar(result.getCalendar(), ctx);
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

    private static DynamicObject createTemporalZonedDateTime(long epochNanoseconds, Object timeZone, DynamicObject calendar) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    private static long interpretISODateTimeOffset(long long1, long long2, long long3, long long4, long long5, long long6, long long7, long long8, long long9, Object offsetNs, Object timeZone,
                    String compatible, String reject) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    private static Object parseTimeZoneOffsetString(Object offset) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    public static JSTemporalPlainDateTimeRecord parseTemporalMonthDayString(String string, JSContext ctx) {
        JSTemporalPlainDateTimeRecord res = parseISODateTime(string, ctx);
        // TODO this is not according to the spec, yet
        return res;
    }

    private static JSTemporalPlainDateTimeRecord parseTemporalYearMonthString(String string, JSContext ctx) {
        JSTemporalPlainDateTimeRecord res = parseISODateTime(string, ctx);
        // TODO this is not according to the spec, yet
        return res;
    }

    private static JSTemporalPlainDateTimeRecord parseISODateTime(String string, JSContext ctx) {
        JSTemporalPlainDateTimeRecord jsDate = tryJSDateParser(string, ctx);
        if (jsDate != null) {
            return jsDate;
        }
        JSTemporalPlainDateTimeRecord instant = tryParseInstant(string);
        if (instant != null) {
            return instant;
        }
        JSTemporalPlainDateTimeRecord date = tryParseDate(string);
        if (date != null) {
            return date;
        }
        JSTemporalPlainDateTimeRecord date2 = tryParseDate2(string);
        if (date2 != null) {
            return date2;
        }
        JSTemporalPlainDateTimeRecord time = tryParseTime(string);
        if (time != null) {
            return time;
        }
        JSTemporalPlainDateTimeRecord time2 = tryParseTime2(string);
        if (time2 != null) {
            return time2;
        }
        JSTemporalPlainDateTimeRecord time3 = tryParseTime3(string);
        if (time3 != null) {
            return time3;
        }
        throw Errors.createRangeError("cannot parse the ISO date time string");
    }

    private static JSTemporalPlainDateTimeRecord tryJSDateParser(String string, JSContext ctx) {
        Integer[] f = ctx.getEvaluator().parseDate(ctx.getRealm(), string);
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
            return JSTemporalPlainDateTimeRecord.create(get(f, 0), month, get(f, 2), get(f, 3), get(f, 4), get(f, 5), ms, mics, ns);
        }
        return null;
    }

    private static int get(Integer[] f, int i) {
        return (f.length > i && f[i] != null) ? f[i] : 0;
    }

    private static JSTemporalPlainDateTimeRecord tryParseInstant(String string) {
        try {
            java.util.Date d = Date.from(Instant.parse(string));
            return JSTemporalPlainDateTimeRecord.create(d.getYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds(), 0, 0, 0);
        } catch (Exception e) {
            return null;
        }
    }

    private static JSTemporalPlainDateTimeRecord tryParseDate(String string) {
        try {
            DateFormat df = new SimpleDateFormat();
            java.util.Date d = df.parse(string);
            return JSTemporalPlainDateTimeRecord.create(d.getYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds(), 0, 0, 0);
        } catch (Exception e) {
            return null;
        }
    }

    private static JSTemporalPlainDateTimeRecord tryParseDate2(String string) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date d = df.parse(string);
            return JSTemporalPlainDateTimeRecord.create(d.getYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds(), 0, 0, 0);
        } catch (Exception e) {
            return null;
        }
    }

    private static JSTemporalPlainDateTimeRecord tryParseTime(String string) {
        try {
            DateFormat df = new SimpleDateFormat("hh:mm:ss.SSS");
            df.setLenient(true);
            java.util.Date d = df.parse(string);
            return JSTemporalPlainDateTimeRecord.create(d.getYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds(), 0, 0, 0);
        } catch (Exception e) {
            return null;
        }
    }

    private static JSTemporalPlainDateTimeRecord tryParseTime2(String string) {
        try {
            DateFormat df = new SimpleDateFormat("hh:mm:ss");
            df.setLenient(true);
            java.util.Date d = df.parse(string);
            return JSTemporalPlainDateTimeRecord.create(d.getYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds(), 0, 0, 0);
        } catch (Exception e) {
            return null;
        }
    }

    private static JSTemporalPlainDateTimeRecord tryParseTime3(String string) {
        try {
            DateFormat df = new SimpleDateFormat("hh:mm");
            df.setLenient(true);
            java.util.Date d = df.parse(string);
            return JSTemporalPlainDateTimeRecord.create(d.getYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds(), 0, 0, 0);
        } catch (Exception e) {
            return null;
        }
    }

    // 13.27
    public static void validateTemporalUnitRange(String largestUnit, String smallestUnit) {
        if (smallestUnit.equals(YEARS) && !largestUnit.equals(YEARS)) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(MONTHS) && !largestUnit.equals(YEARS) && !largestUnit.equals(MONTHS)) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(WEEKS) && !largestUnit.equals(YEARS) && !largestUnit.equals(MONTHS) && !largestUnit.equals(WEEKS)) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(DAYS) && !largestUnit.equals(YEARS) && !largestUnit.equals(MONTHS) && !largestUnit.equals(WEEKS) && !largestUnit.equals(DAYS)) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(HOURS) && !largestUnit.equals(YEARS) && !largestUnit.equals(MONTHS) && !largestUnit.equals(WEEKS) && !largestUnit.equals(DAYS) && !largestUnit.equals(HOURS)) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(MINUTES) && (largestUnit.equals(SECONDS) || largestUnit.equals(MILLISECONDS) || largestUnit.equals(MICROSECONDS) || largestUnit.equals(NANOSECONDS))) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(SECONDS) && (largestUnit.equals(MILLISECONDS) || largestUnit.equals(MICROSECONDS) || largestUnit.equals(NANOSECONDS))) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(MILLISECONDS) && (largestUnit.equals(MICROSECONDS) || largestUnit.equals(NANOSECONDS))) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
        if (smallestUnit.equals(MICROSECONDS) && largestUnit.equals(NANOSECONDS)) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
    }

    // 13.28
    public static String largerOfTwoTemporalDurationUnits(String u1, String u2) {
        if (u1.equals(YEARS) || u2.equals(YEARS)) {
            return YEARS;
        }
        if (u1.equals(MONTHS) || u2.equals(MONTHS)) {
            return MONTHS;
        }
        if (u1.equals(WEEKS) || u2.equals(WEEKS)) {
            return WEEKS;
        }
        if (u1.equals(DAYS) || u2.equals(DAYS)) {
            return DAYS;
        }
        if (u1.equals(HOURS) || u2.equals(HOURS)) {
            return HOURS;
        }
        if (u1.equals(MINUTES) || u2.equals(MINUTES)) {
            return MINUTES;
        }
        if (u1.equals(SECONDS) || u2.equals(SECONDS)) {
            return SECONDS;
        }
        if (u1.equals(MILLISECONDS) || u2.equals(MILLISECONDS)) {
            return MILLISECONDS;
        }
        if (u1.equals(MICROSECONDS) || u2.equals(MICROSECONDS)) {
            return MICROSECONDS;
        }
        return NANOSECONDS;
    }

    // 13.31
    public static double maximumTemporalDurationRoundingIncrement(String unit) {
        if (unit.equals(YEARS) || unit.equals(MONTHS) || unit.equals(WEEKS) || unit.equals(DAYS)) {
            return Double.POSITIVE_INFINITY; // Undefined according to spec, we fix in caller
        }
        if (unit.equals(HOURS)) {
            return 24d;
        }
        if (unit.equals(MINUTES) || unit.equals(SECONDS)) {
            return 60d;
        }
        assert unit.equals(MILLISECONDS) || unit.equals(MICROSECONDS) || unit.equals(NANOSECONDS);
        return 1000d;
    }

    // 13.32
    public static String formatSecondsStringPart(long second, long millisecond, long microsecond, long nanosecond,
                    Object precision) {
        if (precision.equals(MINUTE)) {
            return "";
        }
        String secondString = String.format("%1$2d", second).replace(" ", "0");
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
            fractionString = fractionString.substring(0, (int) precision);
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
        assert roundingMode.equals("ceil") || roundingMode.equals("floor") || roundingMode.equals("trunc") || roundingMode.equals("nearest");
        double quotient = x / increment;
        double rounded;
        if (roundingMode.equals("ceil")) {
            rounded = -Math.floor(-quotient);
        } else if (roundingMode.equals("floor")) {
            rounded = Math.floor(quotient);
        } else if (roundingMode.equals("trunc")) {
            rounded = (long) quotient;
        } else {
            rounded = roundHalfAwayFromZero(x);
        }
        return rounded * increment;
    }

    // 13.43
    public static String parseTemporalCalendarString(String isoString) {
        return "iso8601";   // TODO: parseTemporalCalendarString
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
    public static DynamicObject prepareTemporalFields(DynamicObject fields, Set<String> fieldNames, Set<String> requiredFields, JSContext ctx) {
        DynamicObject result = JSObjectUtil.createOrdinaryPrototypeObject(ctx.getRealm());
        for (String property : fieldNames) {
            Object value = JSObject.get(fields, property);
            if (value == null || value == Undefined.instance) { // TODO (CW) null is probably wrong
                if (requiredFields.contains(property)) {
                    throw Errors.createTypeError(String.format("Property %s is required.", property));
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
            JSObjectUtil.putDataProperty(ctx, result, property, value);
        }
        return result;
    }

    public static <T> Set<T> toSet(T... values) {
        return Arrays.stream(values).collect(Collectors.toSet());
    }

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

    private static DynamicObject createRecordYearMonth(long year, long monthPrepared, JSContext ctx) {
        DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(ctx.getRealm());
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

    public static JSTemporalPlainDateTimeRecord balanceISOYearMonth(long year, long month) {
        assert isInteger(year);
        assert isInteger(month);

        if (year == Long.MAX_VALUE || year == Long.MIN_VALUE || month == Long.MAX_VALUE || year == Long.MIN_VALUE) {
            throw Errors.createRangeError("value our of range");
        }

        long yearPrepared = year + (long) Math.floor((month - 1) / 12);
        long monthPrepared = (long) nonNegativeModulo(month - 1, 12) + 1;

        return JSTemporalPlainDateTimeRecord.create(yearPrepared, monthPrepared, 0, 0, 0, 0, 0, 0, 0);
    }

    // helper method. According to spec, this already is an integer value
    // so help Java see that
    public static long asLong(Object value) {
        assert isInteger(value);
        return (long) value;
    }

    public static DynamicObject getOptionalTemporalCalendar(DynamicObject item, JSContext ctx) {
        if (item instanceof JSTemporalPlainDateObject) {
            return ((JSTemporalPlainDateObject) item).getCalendar();
            // TODO implement other types here, see 12.1.22
        } else {
            Object calendar = JSObject.get(item, TemporalConstants.CALENDAR);
            return toOptionalTemporalCalendar(calendar, ctx);
        }
    }

    public static DynamicObject toOptionalTemporalCalendar(Object calendar, JSContext ctx) {
        if (calendar == Undefined.instance) {
            return getISO8601Calendar(ctx);
        } else {
            return toTemporalCalendar(calendar, ctx);
        }
    }

    public static DynamicObject getISO8601Calendar(JSContext ctx) {
        return getBuiltinCalendar("iso8601", ctx);
    }

    public static DynamicObject getBuiltinCalendar(String id, JSContext ctx) {
        Object cal = JSRuntime.construct(ctx.getRealm().getTemporalCalendarConstructor(), new Object[]{id});
        return (DynamicObject) cal; // TODO does that hold?
    }

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

    public static Set<String> arrayToStringSet(DynamicObject array) {
        long len = (long) JSObject.get(array, "length");
        Set<String> set = new HashSet<>();
        for (int i = 0; i < len; i++) {
            set.add(JSObject.get(array, i).toString());
        }
        return set;
    }

    public static DynamicObject dateFromFields(DynamicObject calendar, DynamicObject fields, DynamicObject options) {
        Object dateFromFields = JSObject.get(calendar, TemporalConstants.DATE_FROM_FIELDS);
        DynamicObject date = (DynamicObject) JSRuntime.call(dateFromFields, calendar, new Object[]{fields, options});
        requireTemporalDate(date);
        return date;
    }

    public static JSTemporalPlainDateTimeRecord parseTemporalDateTimeString(String string, JSContext ctx) {
        // TODO 2. If isoString does not satisfy the syntax of a TemporalDateTimeString (see 13.39)
        JSTemporalPlainDateTimeRecord result = parseISODateTime(string, ctx);
        return result;
    }

    public static JSTemporalPlainDateTimeRecord parseTemporalDateString(String string, JSContext ctx) {
        // TODO 2. If isoString does not satisfy the syntax of a TemporalDateTimeString (see 13.39)
        JSTemporalPlainDateTimeRecord result = parseISODateTime(string, ctx);
        return JSTemporalPlainDateTimeRecord.createCalendar(result.getYear(), result.getMonth(), result.getDay(), 0, 0, 0, 0, 0, 0, result.getCalendar());
    }

    public static JSTemporalPlainDateTimeRecord parseTemporalTimeString(String string, JSContext ctx) {
        // TODO 2. If isoString does not satisfy the syntax of a TemporalDateTimeString (see 13.39)
        JSTemporalPlainDateTimeRecord result = parseISODateTime(string, ctx);
        return JSTemporalPlainDateTimeRecord.create(0, 0, 0, result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
    }

    private static Object parseTemporalTimeZone(String string) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

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

    private static DynamicObject createTemporalTimeZone(Object parseTemporalTimeZone) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    public static DynamicObject createTemporalDateTime(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond,
                    DynamicObject calendar, JSContext context) {
        return JSTemporalPlainDateTime.createTemporalDateTime(context, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, calendar);
    }

    public static DynamicObject builtinTimeZoneGetInstantFor(DynamicObject timeZone, DynamicObject temporalDateTime, String string) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    public static DynamicObject createTemporalZonedDateTime(long longOrDefault, DynamicObject timeZone, DynamicObject calendar) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    public static long getLong(DynamicObject ob, String key, Object defaultValue) {
        return getLong(ob, key, defaultValue);
    }

    public static long getLong(DynamicObject ob, String key, long defaultValue) {
        Object value = JSObject.get(ob, key);
        if (value == Undefined.instance) {
            return defaultValue;
        }
        Number n = (Number) value;
        return n.longValue();
    }

    public static long getLong(DynamicObject ob, String key) {
        Object value = JSObject.get(ob, key);
        Number n = (Number) value;
        return n.longValue();
    }

    public static int getInt(DynamicObject ob, String key) {
        Object value = JSObject.get(ob, key);
        Number n = (Number) value;
        return n.intValue();
    }

    public static Object getOrDefault(DynamicObject ob, String key, Object defaultValue) {
        Object value = JSObject.get(ob, key);
        return value != null ? value : defaultValue;
    }

    public static boolean validateTime(long hours, long minutes, long seconds, long milliseconds, long microseconds,
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

    public static boolean validateISODateTime(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond) {
        if (month < 1 || month > 12) {
            return false;
        }
        if (day < 1 || day > isoDaysInMonth(year, month)) {
            return false;
        }
        return validateTime(hour, minute, second, millisecond, microsecond, nanosecond);
    }

    public static boolean isoDateTimeWithinLimits(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond) {
        double ns = getEpochFromISOParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);

        final double lowerBound = -8.64 * Math.pow(10, 21) - 8.64 * Math.pow(10, 16);
        final double upperBound = 8.64 * Math.pow(10, 21) + 8.64 * Math.pow(10, 16);

        if (ns <= lowerBound || ns > upperBound) {
            return false;
        }
        return true;
    }

    private static double getEpochFromISOParts(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond) {
        double date = JSDate.makeDay(year, month, day);
        double time = JSDate.makeTime(hour, minute, second, millisecond);
        double ms = JSDate.makeDate(date, time);
        assert Double.isFinite(ms);
        return ms * 1_000_000l + microsecond * 1_000l + nanosecond;
    }

    public static String toTemporalOverflow(DynamicObject options) {
        String type = "string"; // TODO there is a bug in the current spec, missing this argument!
        return (String) getOption(options, OVERFLOW, type, TemporalUtil.toSet(CONSTRAIN, REJECT), CONSTRAIN);
    }

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
            throw Errors.createRangeError(
                            String.format("Given options value: %s is not contained in values: %s", value, values));
        }
        return value;
    }

    public static JSTemporalPlainDateTimeRecord interpretTemporalDateTimeFields(DynamicObject calendar, DynamicObject fields, DynamicObject options, JSContext ctx) {
        JSTemporalPlainDateTimeRecord timeResult = toTemporalTimeRecord(fields);
        DynamicObject temporalDate = dateFromFields(calendar, fields, options);
        TemporalDate date = (TemporalDate) temporalDate;
        String overflow = toTemporalOverflow(options);
        JSTemporalPlainDateTimePluralRecord timeResult2 = TemporalUtil.regulateTime(
                        timeResult.getHour(), timeResult.getMinute(), timeResult.getSecond(), timeResult.getMillisecond(), timeResult.getMicrosecond(), timeResult.getNanosecond(),
                        overflow);

        return JSTemporalPlainDateTimeRecord.create(
                        date.getISOYear(), date.getISOMonth(), date.getISODay(),
                        timeResult2.getHours(), timeResult2.getMinutes(), timeResult2.getSeconds(),
                        timeResult2.getMilliseconds(), timeResult2.getMicroseconds(), timeResult2.getNanoseconds());
    }

    public static JSTemporalPlainDateTimePluralRecord regulateTime(long hours, long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds, String overflow) {
        assert overflow.equals(CONSTRAIN) || overflow.equals(REJECT);
        if (overflow.equals(CONSTRAIN)) {
            return constrainTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        } else {
            if (!TemporalUtil.validateTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
                throw Errors.createRangeError("Given time outside the range.");
            }

            // sets [[Days]] but [[Hour]]
            return JSTemporalPlainDateTimePluralRecord.create(0, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
    }

    public static JSTemporalPlainDateTimePluralRecord constrainTime(long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds) {
        return JSTemporalPlainDateTimePluralRecord.create(0, 0, 0,
                        TemporalUtil.constrainToRange(hours, 0, 23),
                        TemporalUtil.constrainToRange(minutes, 0, 59),
                        TemporalUtil.constrainToRange(seconds, 0, 59),
                        TemporalUtil.constrainToRange(milliseconds, 0, 999),
                        TemporalUtil.constrainToRange(microseconds, 0, 999),
                        TemporalUtil.constrainToRange(nanoseconds, 0, 999));
    }

    public static JSTemporalPlainDateTimeRecord toTemporalTimeRecord(DynamicObject temporalTimeLike) {
        return JSTemporalPlainDateTimeRecord.create(0, 0, 0,
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
            throw Errors.createTypeError(String.format("Property %s should not be undefined.", property));
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

    public static long addZonedDateTime(long long1, Object timeZone, DynamicObject calendar, long years, long months, long weeks, long days, int i, int j, int k, int l, int m, int n) {
        // TODO Auto-generated method stub
        return 0l;
    }

    public static long roundTemporalInstant(long ns, long increment, String unit, String roundingMode) {
        long incrementNs = 0;
        if (HOUR.equals(unit)) {
            incrementNs = increment * 3_600_000_000_000l;
        } else if (MINUTE.equals(unit)) {
            incrementNs = increment * 60_000_000_000l;
        } else if (SECOND.equals(unit)) {
            incrementNs = increment * 1_000_000_000l;
        } else if (MILLISECOND.equals(unit)) {
            incrementNs = increment * 1_000_000l;
        } else if (MICROSECOND.equals(unit)) {
            incrementNs = increment * 1_000l;
        }
        return (long) roundNumberToIncrement(ns, incrementNs, roundingMode);
    }

    public static DynamicObject toTemporalDate(Object itemParam, DynamicObject optionsParam, JSContext ctx) {
        DynamicObject options = optionsParam != null ? optionsParam : JSObjectUtil.createOrdinaryPrototypeObject(ctx.getRealm());
        if (JSRuntime.isObject(itemParam)) {
            DynamicObject item = (DynamicObject) itemParam;
            if (JSTemporalPlainDate.isJSTemporalPlainDate(item)) {
                return item;
            }
            DynamicObject calendar = TemporalUtil.getOptionalTemporalCalendar(item, ctx);
            Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{"day", "month", "monthCode", "year"}, ctx);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(item, fieldNames, TemporalUtil.toSet(), ctx);
            return TemporalUtil.dateFromFields(calendar, fields, options);
        }
        String overflows = TemporalUtil.toTemporalOverflow(options);
        JSTemporalPlainDateTimeRecord result = TemporalUtil.parseTemporalDateString(JSRuntime.toString(itemParam), ctx);
        if (!validateISODate(result.getYear(), result.getMonth(), result.getDay())) {
            throw Errors.createRangeError("Given date is not valid.");
        }
        DynamicObject calendar = TemporalUtil.toOptionalTemporalCalendar(result.getCalendar(), ctx);
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
    public static JSTemporalPlainDateTimeRecord regulateISODate(long year, long month, long day, String overflow) {
        assert overflow.equals(CONSTRAIN) || overflow.equals(REJECT);
        if (overflow.equals(REJECT)) {
            rejectISODate(year, month, day);
            return JSTemporalPlainDateTimeRecord.create(year, month, day, 0, 0, 0, 0, 0, 0);
        }
        if (overflow.equals(CONSTRAIN)) {
            return constrainISODate(year, month, day);
        }
        throw new RuntimeException("This should never have happened.");
    }

    // 3.5.9
    public static void rejectISODate(long year, long month, long day) {
        if (!validateISODate(year, month, day)) {
            throw Errors.createRangeError("Given date is not valid.");
        }
    }

    // 3.5.10
    public static JSTemporalPlainDateTimeRecord constrainISODate(long year, long month, long day) {
        long monthPrepared = TemporalUtil.constrainToRange(month, 1, 12);
        long dayPrepared = TemporalUtil.constrainToRange(day, 1, TemporalUtil.isoDaysInMonth(year, month));
        return JSTemporalPlainDateTimeRecord.create(year, monthPrepared, dayPrepared, 0, 0, 0, 0, 0, 0);
    }

    // 3.5.11
    public static JSTemporalPlainDateTimeRecord balanceISODate(long yearParam, long monthParam, long dayParam) {
        JSTemporalPlainDateTimeRecord balancedYearMonth = TemporalUtil.balanceISOYearMonth(yearParam, monthParam);
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
    public static JSTemporalPlainDateTimeRecord addISODate(long year, long month, long day, long years, long months, long weeks,
                    long days, String overflow) {
        assert overflow.equals(CONSTRAIN) || overflow.equals(REJECT);
        long y = year + years;
        long m = month + months;
        JSTemporalPlainDateTimeRecord intermediate = TemporalUtil.balanceISOYearMonth(y, m);
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
        if (HOUR.equals(u)) {
            return HOURS;
        } else if (MINUTE.equals(u)) {
            return MINUTES;
        } else if (SECOND.equals(u)) {
            return SECONDS;
        } else if (MILLISECOND.equals(u)) {
            return MILLISECONDS;
        } else if (MICROSECOND.equals(u)) {
            return MICROSECONDS;
        } else if (NANOSECOND.equals(u)) {
            return NANOSECONDS;
        }
        throw Errors.createTypeError("cannot convert Unit to plural");
    }

    public static boolean isTemporalZonedDateTime(@SuppressWarnings("unused") DynamicObject obj) {
        // TODO this should be an instanceof check on JSTemporalZonedDateTimeObject
        // but that class does not exist yet :-)
        return false;
    }

    public static String toShowCalendarOption(DynamicObject options) {
        // TODO 1. Return ? GetOption(normalizedOptions, "calendarName", "string", « "auto",
        // "always", "never" », "auto").
        return "auto";
    }

    public static String padISOYear(long year) {
        if (999 < year && year < 9999) {
            return String.valueOf(year);
        }
        String sign = year >= 0 ? "+" : "-";
        return String.format("%s%1$6d", sign, Math.abs(year)).replace(" ", "0");
    }

    @TruffleBoundary
    public static String formatCalendarAnnotation(String id, String showCalendar) {
        if ("never".equals(showCalendar)) {
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

    public static DynamicObject toTemporalYearMonth(Object item, DynamicObject optParam, JSContext ctx) {
        DynamicObject options = optParam;
        if (optParam == Undefined.instance) {
            options = JSObjectUtil.createOrdinaryPrototypeObject(ctx.getRealm(), Null.instance);
        }
        if (JSRuntime.isObject(item)) {
            DynamicObject itemObj = (DynamicObject) item;
            if (JSTemporalPlainYearMonth.isJSTemporalPlainYearMonth(itemObj)) {
                return itemObj;
            }
            DynamicObject calendar = TemporalUtil.getOptionalTemporalCalendar(itemObj, ctx);

            Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{MONTH, MONTH_CODE, YEAR}, ctx);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(itemObj, fieldNames, new HashSet<>(), ctx);
            return yearMonthFromFields(calendar, fields, options);
        }
        TemporalUtil.toTemporalOverflow(options);

        String string = JSRuntime.toString(item);
        JSTemporalPlainDateTimeRecord result = TemporalUtil.parseTemporalYearMonthString(string, ctx);
        DynamicObject calendar = TemporalUtil.toOptionalTemporalCalendar(result.getCalendar(), ctx);
        if (result.getYear() == 0) { // TODO Check for undefined here!
            if (!TemporalUtil.validateISODate(result.getYear(), result.getMonth(), 1)) {
                throw Errors.createRangeError("invalid date");
            }
            // TODO year should be undefined!
            return JSTemporalPlainYearMonth.create(ctx, result.getYear(), result.getMonth(), calendar, 0);
        }
        DynamicObject result2 = JSTemporalPlainMonthDay.create(ctx, result.getYear(), result.getMonth(), result.getDay(), calendar);
        DynamicObject canonicalYearMonthOptions = JSObjectUtil.createOrdinaryPrototypeObject(ctx.getRealm(), Null.instance);
        return yearMonthFromFields(calendar, result2, canonicalYearMonthOptions);
    }

    private static DynamicObject yearMonthFromFields(DynamicObject calendar, DynamicObject fields, DynamicObject options) {
        Object yearMonth = JSRuntime.call("yearMonthFromFields", calendar, new Object[]{fields, options});
        return requireTemporalYearMonth(yearMonth);
    }

    public static String negateTemporalRoundingMode(String roundingMode) {
        if (CEIL.equals(roundingMode)) {
            return FLOOR;
        } else if (FLOOR.equals(roundingMode)) {
            return CEIL;
        }
        return roundingMode;
    }

    public static boolean calenderEquals(DynamicObject one, DynamicObject two) {
        if (one == two) {
            return true;
        }
        if (JSRuntime.toString(one).equals(JSRuntime.toString(two))) {
            return true;
        }
        return false;
    }

}
