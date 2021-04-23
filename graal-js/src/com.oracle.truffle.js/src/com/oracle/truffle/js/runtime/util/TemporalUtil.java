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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.CONSTRAIN;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ERA;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ERA_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.REJECT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.OVERFLOW;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.COMPATIBLE;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class TemporalUtil {

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
    public static Object getOptions(DynamicObject options, String property, String type, Set<Object> values, Object fallback, IsObjectNode isObjectNode, JSToBooleanNode toBoolean,
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
        if (values != null && !values.contains(value)) {
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
                    double minimum, double maximum, Object fallback, IsObjectNode isObject, JSToNumberNode toNumber, JSToStringNode toStringNode) {
        assert isObject.executeBoolean(options);
        Object value = JSObject.get(options, property);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            double numberValue = toNumber.executeNumber(value).doubleValue();
            if (numberValue == Double.NaN || numberValue < minimum || numberValue > maximum) {
                throw Errors.createRangeError("Numeric value out of range.");
            }
            return Math.floor(numberValue);
        }
        value = toStringNode.executeString(value);
        if (stringValues != null && !stringValues.contains(value)) {
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
        return (String) getOptions(options, "overflow", "string", values, CONSTRAIN, isObjectNode, toBoolean, toStringNode);
    }

    // 13.11
    public static String toTemporalRoundingMode(DynamicObject options, String fallback, IsObjectNode isObjectNode,
                    JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        return (String) getOptions(options, "roundingMode", "string", toSet("ceil", "floor", "trunc", "nearest"),
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

    // 13.19
    public static DynamicObject toSecondsStringPrecision(DynamicObject options, IsObjectNode isObject, JSToBooleanNode toBoolean, JSToStringNode toStringNode,
                    JSToNumberNode toNumberNode, JSRealm realm) {
        String smallestUnit = (String) getOptions(options, "smallestUnit", "string", toSet(MINUTE, SECOND,
                        MILLISECOND, MICROSECOND, NANOSECOND, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS),
                        null, isObject, toBoolean, toStringNode);
        if (pluralUnits.contains(smallestUnit)) {
            smallestUnit = pluralToSingular.get(smallestUnit);
        }
        DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        if (smallestUnit != null) {
            if (smallestUnit.equals(MINUTE)) {
                JSObjectUtil.putDataProperty(realm.getContext(), record, "precision", MINUTE);
                JSObjectUtil.putDataProperty(realm.getContext(), record, "unit", MINUTE);
                JSObjectUtil.putDataProperty(realm.getContext(), record, "increment", 1);
                return record;
            }
            if (smallestUnit.equals(SECOND)) {
                JSObjectUtil.putDataProperty(realm.getContext(), record, "precision", 0);
                JSObjectUtil.putDataProperty(realm.getContext(), record, "unit", SECOND);
                JSObjectUtil.putDataProperty(realm.getContext(), record, "increment", 1);
                return record;
            }
            if (smallestUnit.equals(MILLISECOND)) {
                JSObjectUtil.putDataProperty(realm.getContext(), record, "precision", 3);
                JSObjectUtil.putDataProperty(realm.getContext(), record, "unit", MILLISECOND);
                JSObjectUtil.putDataProperty(realm.getContext(), record, "increment", 1);
                return record;
            }
            if (smallestUnit.equals(MICROSECOND)) {
                JSObjectUtil.putDataProperty(realm.getContext(), record, "precision", 6);
                JSObjectUtil.putDataProperty(realm.getContext(), record, "unit", MICROSECOND);
                JSObjectUtil.putDataProperty(realm.getContext(), record, "increment", 1);
                return record;
            }
            if (smallestUnit.equals(NANOSECOND)) {
                JSObjectUtil.putDataProperty(realm.getContext(), record, "precision", 9);
                JSObjectUtil.putDataProperty(realm.getContext(), record, "unit", NANOSECOND);
                JSObjectUtil.putDataProperty(realm.getContext(), record, "increment", 1);
                return record;
            }
        }
        assert smallestUnit == null;
        Object digits = getStringOrNumberOption(options, "fractionalSecondDigits",
                        Collections.singleton("auto"), 0, 9, "auto", isObject, toNumberNode, toStringNode);
        if (digits.equals("auto")) {
            JSObjectUtil.putDataProperty(realm.getContext(), record, "precision", "auto");
            JSObjectUtil.putDataProperty(realm.getContext(), record, "unit", NANOSECOND);
            JSObjectUtil.putDataProperty(realm.getContext(), record, "increment", 1);
            return record;
        }
        if (digits.equals(0)) {
            JSObjectUtil.putDataProperty(realm.getContext(), record, "precision", 0);
            JSObjectUtil.putDataProperty(realm.getContext(), record, "unit", SECOND);
            JSObjectUtil.putDataProperty(realm.getContext(), record, "increment", 1);
            return record;
        }
        if (digits.equals(1) || digits.equals(2) || digits.equals(3)) {
            JSObjectUtil.putDataProperty(realm.getContext(), record, "precision", digits);
            JSObjectUtil.putDataProperty(realm.getContext(), record, "unit", MILLISECOND);
            JSObjectUtil.putDataProperty(realm.getContext(), record, "increment", Math.pow(10, 3 - (long) digits));
            return record;
        }
        if (digits.equals(4) || digits.equals(5) || digits.equals(6)) {
            JSObjectUtil.putDataProperty(realm.getContext(), record, "precision", digits);
            JSObjectUtil.putDataProperty(realm.getContext(), record, "unit", MICROSECOND);
            JSObjectUtil.putDataProperty(realm.getContext(), record, "increment", Math.pow(10, 6 - (long) digits));
            return record;
        }
        assert digits.equals(7) || digits.equals(8) || digits.equals(9);
        JSObjectUtil.putDataProperty(realm.getContext(), record, "precision", digits);
        JSObjectUtil.putDataProperty(realm.getContext(), record, "unit", NANOSECOND);
        JSObjectUtil.putDataProperty(realm.getContext(), record, "increment", Math.pow(10, 9 - (long) digits));
        return record;
    }

    // 13.21
    public static String toLargestTemporalUnit(DynamicObject normalizedOptions, Set<String> disallowedUnits, String defaultUnit,
                    IsObjectNode isObjectNode,
                    JSToBooleanNode toBoolean, JSToStringNode toStringNode) {
        assert !disallowedUnits.contains(defaultUnit) && !disallowedUnits.contains("auto");
        String largestUnit = (String) getOptions(normalizedOptions, "largestUnit", "string", toSet(
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
        String smallestUnit = (String) getOptions(normalizedOptions, "smallestUnit", "string", toSet("day", "days", "hour",
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
        String smallestUnit = (String) getOptions(normalizedOptions, "smallestUnit", "string", toSet(
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
        String unit = (String) getOptions(normalizedOptions, "unit", "string", toSet(YEARS, YEAR, MONTHS, MONTH, WEEKS,
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
        DynamicObject result = null;
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
                return JSTemporalPlainDateTime.createTemporalDateTime(ctx, pd.getYear(), pd.getMonth(), pd.getDay(), 0, 0, 0, 0, 0, 0, pd.getCalendar());
            }
            calendar = TemporalUtil.getOptionalTemporalCalendar((DynamicObject) value, ctx);
            Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{DAY, MONTH, MONTH_CODE, YEAR}, ctx);
            DynamicObject fields = TemporalUtil.prepareTemporalFields((DynamicObject) value, fieldNames, new HashSet<>(), ctx.getRealm());

            DynamicObject dateOptions = JSObjectUtil.createOrdinaryPrototypeObject(ctx.getRealm(), Null.instance);
            JSObjectUtil.putDataProperty(ctx, dateOptions, OVERFLOW, CONSTRAIN);
            result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, dateOptions);
            offset = JSObject.get((DynamicObject) value, OFFSET);
            timeZone = JSObject.get((DynamicObject) value, TIME_ZONE);
        } else {
            String string = JSRuntime.toString(value);
            result = parseISODateTime(string);
            calendar = toOptionalTemporalCalendar(JSObject.get(result, CALENDAR), ctx);
            offset = JSObject.get(result, TemporalConstants.TIME_ZONE_OFFSET);
            timeZone = JSObject.get(result, TemporalConstants.TIME_ZONE_IANA_NAME);
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
                            getLong(result, YEAR),
                            getLong(result, MONTH),
                            getLong(result, DAY),
                            getLong(result, HOUR),
                            getLong(result, MINUTE),
                            getLong(result, SECOND),
                            getLong(result, MILLISECOND),
                            getLong(result, MICROSECOND),
                            getLong(result, NANOSECOND),
                            offsetNs, timeZone, COMPATIBLE, REJECT);
            return createTemporalZonedDateTime(epochNanoseconds, timeZone, calendar);
        }
        return JSTemporalPlainDateTime.createTemporalDateTime(ctx,
                        getLong(result, YEAR),
                        getLong(result, MONTH),
                        getLong(result, DAY),
                        getLong(result, HOUR),
                        getLong(result, MINUTE),
                        getLong(result, SECOND),
                        getLong(result, MILLISECOND),
                        getLong(result, MICROSECOND),
                        getLong(result, NANOSECOND),
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

    private static DynamicObject parseISODateTime(String string) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
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
    public static long toPositiveIntegerOrInfinity(Object argument) {
        double integer = JSToDoubleNode.create().executeDouble(argument);
        if (integer == Double.NEGATIVE_INFINITY) {
            throw Errors.createRangeError("Integer should not be negative infinity.");
        }
        if (integer <= 0) {
            throw Errors.createRangeError("Integer should be positive.");
        }
        return (long) integer;
    }

    // 13.52
    public static DynamicObject prepareTemporalFields(DynamicObject fields, Set<String> fieldNames, Set<String> requiredFields, JSRealm realm) {
        DynamicObject result = JSObjectUtil.createOrdinaryPrototypeObject(realm);
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
            JSObjectUtil.putDataProperty(realm.getContext(), result, property, value);
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

    public static DynamicObject regulateISOYearMonth(long year, long month, String overflow, JSRealm realm) {
        assert isInteger(year);
        assert isInteger(month);
        assert CONSTRAIN.equals(overflow) || REJECT.equals(overflow);

        if (CONSTRAIN.equals(overflow)) {
            return constrainISOYearMonth(year, month, realm);
        } else if (REJECT.equals(overflow)) {
            if (!validateISOYearMonth(year, month)) {
                throw Errors.createRangeError("validation of year and month failed");
            }
        }

        return createRecordYearMonth(year, month, realm);
    }

    private static boolean validateISOYearMonth(long year, long month) {
        assert isInteger(year);
        assert isInteger(month);
        return (1 <= month) && (month <= 12);
    }

    private static DynamicObject constrainISOYearMonth(long year, long month, JSRealm realm) {
        assert isInteger(year);
        assert isInteger(month);

        long monthPrepared = constrainToRange(month, 1, 12);

        return createRecordYearMonth(year, monthPrepared, realm);
    }

    private static DynamicObject createRecordYearMonth(long year, long monthPrepared, JSRealm realm) {
        DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putDataProperty(realm.getContext(), record, MONTH, monthPrepared);
        JSObjectUtil.putDataProperty(realm.getContext(), record, YEAR, year);
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

    public static DynamicObject balanceISOYearMonth(long year, long month, JSRealm realm) {
        assert isInteger(year);
        assert isInteger(month);

        if (year == Long.MAX_VALUE || year == Long.MIN_VALUE || month == Long.MAX_VALUE || year == Long.MIN_VALUE) {
            throw Errors.createRangeError("value our of range");
        }

        long yearPrepared = year + (long) Math.floor((month - 1) / 12);
        long monthPrepared = (long) nonNegativeModulo(month - 1, 12) + 1;
        return createRecordYearMonth(yearPrepared, monthPrepared, realm);
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

    public static DynamicObject toTemporalCalendar(Object calendar, JSContext ctx) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
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
        requireInternalSlot(date, "InitializedTemporalDate");
        return date;
    }

    public static DynamicObject parseTemporalDateString(String string) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    private static Object parseTemporalTimeZone(String string) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    public static DynamicObject parseTemporalTimeString(String string) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    public static Object buildISOMonthCode(String numberPart) {
        assert 1 <= numberPart.length() && numberPart.length() <= 2;
        return numberPart.length() >= 2 ? "M" + numberPart : "M0" + numberPart;
    }

    public static void requireInternalSlot(DynamicObject obj, String str) {
        // TODO ECMAScript 10.1.15
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

    public static DynamicObject createTemporalDateTime(int year, int month, int day, long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds,
                    DynamicObject calendar) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO"); // probably JSTemporalPlainDateTime.create ....
    }

    public static DynamicObject builtinTimeZoneGetInstantFor(DynamicObject timeZone, DynamicObject temporalDateTime, String string) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    public static DynamicObject createTemporalZonedDateTime(long longOrDefault, DynamicObject timeZone, DynamicObject calendar) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    // TODO get rid of this, just for legacy
    public static long getLong(DynamicObject ob, String key, Object defaultValue) {
        return getLong(ob, key);
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

    public static DynamicObject parseTemporalDateTimeString(String string) {
        // TODO Auto-generated method stub
        throw Errors.unsupported("TODO");
    }

    public static void toTemporalOverflow(DynamicObject options) {
        // TODO Auto-generated method stub

    }

    public static DynamicObject interpretTemporalDateTimeFields(DynamicObject calendar, DynamicObject fieldNames, DynamicObject options) {
        // TODO Auto-generated method stub
        return null;
    }
}
