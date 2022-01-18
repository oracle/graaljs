/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.COMPATIBLE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CONSTRAIN;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DISAMBIGUATION;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.EARLIER;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ERA;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ERA_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.FLOOR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.IGNORE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ISO8601;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.LARGEST_UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.LATER;
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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.PREFER;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.REJECT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.RELATIVE_TO;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ROUNDING_INCREMENT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ROUNDING_MODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SMALLEST_UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE_NAME;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.USE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.UTC;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEK;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerWithoutRoundingNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.temporal.GetTemporalCalendarWithISODefaultNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode.OptionTypeEnum;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDateNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDateTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalZonedDateTimeNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendarObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalNanosecondsDaysRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalParserRecord;
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
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZone;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZoneRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalYearMonthDayRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDay;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalMonth;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalYear;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class TemporalUtil {

    private static final Function<Object, Object> toIntegerThrowOnInfinity = TemporalUtil::toIntegerThrowOnInfinity;
    private static final Function<Object, Object> toPositiveInteger = TemporalUtil::toPositiveInteger;
    private static final Function<Object, Object> toString = (argument -> JSRuntime.toString(argument));

    public static final Set<String> pluralUnits = Set.of(YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS,
                    MILLISECONDS, MICROSECONDS, NANOSECONDS);
    public static final Map<String, String> pluralToSingular = toMap(
                    new String[]{YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS},
                    new String[]{YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND});
    @SuppressWarnings("unchecked") private static final Map<String, Function<Object, Object>> temporalFieldConversion = toMap(
                    new String[]{YEAR, MONTH, MONTH_CODE, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND, OFFSET, ERA, ERA_YEAR},
                    new Function[]{toIntegerThrowOnInfinity, toPositiveInteger, toString, toPositiveInteger, toIntegerThrowOnInfinity, toIntegerThrowOnInfinity, toIntegerThrowOnInfinity,
                                    toIntegerThrowOnInfinity, toIntegerThrowOnInfinity, toIntegerThrowOnInfinity, toString, toString, toIntegerThrowOnInfinity});
    public static final Map<String, Object> temporalFieldDefaults = toMap(
                    new String[]{YEAR, MONTH, MONTH_CODE, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND, YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS,
                                    MICROSECONDS, NANOSECONDS, OFFSET, ERA, ERA_YEAR},
                    new Object[]{Undefined.instance, Undefined.instance, Undefined.instance, Undefined.instance, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Undefined.instance, Undefined.instance,
                                    Undefined.instance});

    public static final List<String> listEmpty = List.of();
    public static final List<String> listYMWD = List.of(YEAR, MONTH, WEEK, DAY);
    public static final List<String> listPluralYMWD = List.of(YEARS, MONTHS, WEEKS, DAYS);
    public static final List<String> listYMW = List.of(YEAR, MONTH, WEEK);
    public static final List<String> listYMWDH = List.of(YEAR, MONTH, WEEK, DAY, HOUR);
    public static final List<String> listTime = List.of(HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND);
    public static final List<String> listDMMCY = List.of(DAY, MONTH, MONTH_CODE, YEAR);
    public static final List<String> listMMCY = List.of(MONTH, MONTH_CODE, YEAR);
    public static final List<String> listMCY = List.of(MONTH_CODE, YEAR);
    public static final List<String> listDMC = List.of(DAY, MONTH_CODE);
    public static final List<String> listY = List.of(YEAR);
    public static final List<String> listD = List.of(DAY);
    public static final List<String> listWDHMSMMN = List.of(WEEK, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND);
    public static final List<String> listAllDateTime = List.of(YEARS, YEAR, MONTHS, MONTH, WEEKS, WEEK, DAYS, DAY, HOURS, HOUR, MINUTES, MINUTE, SECONDS, SECOND, MILLISECONDS, MILLISECOND,
                    MICROSECONDS, MICROSECOND, NANOSECONDS, NANOSECOND);
    public static final List<String> listAllDateTimeAuto = List.of(AUTO, YEARS, YEAR, MONTHS, MONTH, WEEKS, WEEK, DAYS, DAY, HOURS, HOUR, MINUTES, MINUTE, SECONDS, SECOND, MILLISECONDS, MILLISECOND,
                    MICROSECONDS, MICROSECOND, NANOSECONDS, NANOSECOND);
    public static final List<String> listDHMMMMMNSY = List.of(DAY, HOUR, MICROSECOND, MILLISECOND, MINUTE, MONTH, MONTH_CODE, NANOSECOND, SECOND, YEAR);

    public static final List<String> listAuto = List.of(AUTO);
    public static final List<String> listAutoNever = List.of(AUTO, NEVER);
    public static final List<String> listAutoAlwaysNever = List.of(AUTO, ALWAYS, NEVER);
    public static final List<String> listConstrainReject = List.of(CONSTRAIN, REJECT);
    public static final List<String> listTimeZone = List.of(TIME_ZONE);
    public static final List<String> listTimeZoneOffset = List.of(TIME_ZONE, OFFSET);
    public static final List<String> listCFTH = List.of(CEIL, FLOOR, TRUNC, HALF_EXPAND);
    public static final List<String> listPUIR = List.of(PREFER, USE, IGNORE, REJECT);
    public static final List<String> listCELR = List.of(COMPATIBLE, EARLIER, LATER, REJECT);

    public static final String[] TIME_LIKE_PROPERTIES = new String[]{HOUR, MICROSECOND, MILLISECOND, MINUTE, NANOSECOND, SECOND};
    public static final String[] DURATION_PROPERTIES = new String[]{DAYS, HOURS, MICROSECONDS, MILLISECONDS, MINUTES, MONTHS, NANOSECONDS, SECONDS, WEEKS, YEARS};

    private static final BigInt upperEpochNSLimit = new BigInt(BigInteger.valueOf(86400).multiply(BigInteger.valueOf(10).pow(17)));
    private static final BigInt lowerEpochNSLimit = upperEpochNSLimit.negate();

    // 8.64* 10^21 + 8.64 * 10^13
    private static final BigInteger isoTimeUpperBound = new BigInteger("8640000086400000000000");
    private static final BigInteger isoTimeLowerBound = isoTimeUpperBound.negate();

    // 8.64 * 10^21
    private static final BigInteger temporalInstantUpperBound = new BigInteger("8640000000000000000000");
    private static final BigInteger temporalInstantLowerBound = temporalInstantUpperBound.negate();

    // 8.64 * 10^13
    private static final BigInteger bi_8_64_13 = new BigInteger("86400000000000");

    // 10 ^ 9
    private static final BigInteger bi_10_pow_9 = new BigInteger("1000000000");

    public static final BigDecimal bd_10 = new BigDecimal("10");
    public static final BigDecimal bd_1000 = new BigDecimal("1000");

    public static final MathContext mc_20_floor = new MathContext(20, RoundingMode.FLOOR);

    public enum TemporalOverflowEnum {
        CONSTRAIN,
        REJECT
    }

    public enum OffsetBehaviour {
        OPTION,
        WALL,
        EXACT
    }

    public enum MatchBehaviour {
        MATCH_EXACTLY,
        MATCH_MINUTES
    }

    /**
     * Note there also is {@link TemporalGetOptionNode}.
     */
    public static Object getOption(DynamicObject options, String property, OptionTypeEnum types, List<?> values, Object fallback, JSToBooleanNode toBoolean, JSToNumberNode toNumber,
                    JSToStringNode toStringNode) {
        assert JSRuntime.isObject(options);
        Object value = JSObject.get(options, property);
        if (value == Undefined.instance) {
            return fallback;
        }
        OptionTypeEnum type;
        if ((types == OptionTypeEnum.BOOLEAN && value instanceof Boolean) || (types == OptionTypeEnum.STRING && JSRuntime.isString(value)) ||
                        (types == OptionTypeEnum.NUMBER && JSRuntime.isNumber(value))) {
            type = types;
        } else {
            type = types; // TODO "last entry of", but this is not a list yet
        }

        if (type == OptionTypeEnum.BOOLEAN) {
            value = toBoolean.executeBoolean(value);
        } else if (type == OptionTypeEnum.NUMBER) {
            value = toNumber.executeNumber(value);
            if (Double.isNaN(((Number) value).doubleValue())) {
                throw TemporalErrors.createRangeErrorNumberIsNaN();
            }
        } else {
            assert type == OptionTypeEnum.STRING;
            value = toStringNode.executeString(value);
        }
        if (value != Undefined.instance && !Boundaries.listContainsUnchecked(values, value)) {
            throw TemporalErrors.createRangeErrorOptionsNotContained(values, value);
        }
        return value;
    }

    /**
     * Note there also is {@link TemporalGetOptionNode}, and there is an override that accept
     * conversion Nodes.
     */
    @TruffleBoundary
    public static Object getOption(DynamicObject options, String property, OptionTypeEnum type, List<String> values, Object fallback) {
        assert JSRuntime.isObject(options);
        Object value = JSObject.get(options, property);
        if (value == Undefined.instance) {
            return fallback;
        }
        if (type == OptionTypeEnum.BOOLEAN) {
            value = JSRuntime.toBoolean(value);
        } else if (type == OptionTypeEnum.NUMBER) {
            value = JSRuntime.toNumber(value);
            if (Double.isNaN(((Number) value).doubleValue())) {
                throw TemporalErrors.createRangeErrorNumberIsNaN();
            }
        } else {
            assert type == OptionTypeEnum.STRING;
            value = JSRuntime.toString(value);
        }
        if (values != null && !values.contains(value)) {
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
        double numberValue = JSRuntime.doubleValue(toNumber.executeNumber(value));
        if (Double.isNaN(numberValue) || numberValue < minimum || numberValue > maximum || (Double.isInfinite(numberValue) && Double.isInfinite(maximum))) {
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
    public static Object getStringOrNumberOption(DynamicObject options, String property, List<String> stringValues,
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
    public static TemporalOverflowEnum toTemporalOverflow(DynamicObject options, JSToBooleanNode toBoolean, JSToNumberNode toNumberNode, JSToStringNode toStringNode) {
        return overflowStringToEnum((String) getOption(options, OVERFLOW, OptionTypeEnum.STRING, listConstrainReject, CONSTRAIN, toBoolean, toNumberNode, toStringNode));
    }

    // 13.11
    public static String toTemporalRoundingMode(DynamicObject options, String fallback, JSToBooleanNode toBoolean, JSToNumberNode toNumberNode, JSToStringNode toStringNode) {
        return (String) getOption(options, ROUNDING_MODE, OptionTypeEnum.STRING, listCFTH, fallback, toBoolean, toNumberNode, toStringNode);
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
            dDividend = JSRuntime.doubleValue(dividend);
            if (inclusive) {
                maximum = dDividend;
            } else if (dDividend > 1) {
                maximum = dDividend - 1;
            } else {
                maximum = 1;
            }
        }

        double increment = getNumberOption(options, ROUNDING_INCREMENT, 1, maximum, 1, isObject, toNumber);
        if (dividend != null && dDividend % increment != 0) {
            throw Errors.createRangeError("Increment out of range.");
        }
        return increment;
    }

    public static JSTemporalPrecisionRecord toSecondsStringPrecision(DynamicObject options, TemporalGetOptionNode getOptionNode) {
        String smallestUnit = toSmallestTemporalUnit(options, listYMWDH, null, getOptionNode);

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

        Object digits = getStringOrNumberOption(options, "fractionalSecondDigits", listAuto, 0, 9, AUTO);
        if (Boundaries.equals(digits, AUTO)) {
            return JSTemporalPrecisionRecord.create(AUTO, NANOSECOND, 1);
        }
        int iDigit = JSRuntime.intValue((Number) digits);

        if (iDigit == 0) {
            return JSTemporalPrecisionRecord.create(0, SECOND, 1);
        }
        if (iDigit == 1 || iDigit == 2 || iDigit == 3) {
            return JSTemporalPrecisionRecord.create(digits, MILLISECOND, Math.pow(10, 3 - toLong(digits)));
        }
        if (iDigit == 4 || iDigit == 5 || iDigit == 6) {
            return JSTemporalPrecisionRecord.create(digits, MICROSECOND, Math.pow(10, 6 - toLong(digits)));
        }
        assert iDigit == 7 || iDigit == 8 || iDigit == 9;
        return JSTemporalPrecisionRecord.create(digits, NANOSECOND, Math.pow(10, 9 - toLong(digits)));
    }

    // TODO this whole method should be unnecessary
    @TruffleBoundary
    private static long toLong(Object digits) {
        if (digits instanceof Number) {
            return ((Number) digits).longValue();
        }
        return JSRuntime.toNumber(digits).longValue();
    }

    // 13.22
    public static String toSmallestTemporalUnit(DynamicObject normalizedOptions, List<String> disallowedUnits, String fallback, TemporalGetOptionNode getOptionNode) {
        String smallestUnit = (String) getOptionNode.execute(normalizedOptions, SMALLEST_UNIT, OptionTypeEnum.STRING, listAllDateTime, fallback);
        if (smallestUnit != null && Boundaries.setContains(pluralUnits, smallestUnit)) {
            smallestUnit = Boundaries.mapGet(pluralToSingular, smallestUnit);
        }
        if (smallestUnit != null && Boundaries.listContains(disallowedUnits, smallestUnit)) {
            throw Errors.createRangeError("Smallest unit not allowed.");
        }
        return smallestUnit;
    }

    // 13.26
    @TruffleBoundary
    public static DynamicObject toRelativeTemporalObject(JSContext ctx, JSRealm realm, DynamicObject options) {
        Object value = JSObject.get(options, RELATIVE_TO);
        if (value == Undefined.instance) {
            return Undefined.instance;
        }
        JSTemporalDateTimeRecord result = null;
        Object timeZone = null;
        DynamicObject calendar = null;
        Object offset = null;
        OffsetBehaviour offsetBehaviour = OffsetBehaviour.OPTION;
        MatchBehaviour matchBehaviour = MatchBehaviour.MATCH_EXACTLY;
        if (JSRuntime.isObject(value)) {
            DynamicObject valueObj = (DynamicObject) value;
            if (valueObj instanceof JSTemporalPlainDateObject || valueObj instanceof JSTemporalZonedDateTimeObject) {
                return valueObj;
            }
            if (valueObj instanceof JSTemporalPlainDateTimeObject) {
                JSTemporalPlainDateTimeObject pd = (JSTemporalPlainDateTimeObject) valueObj;
                return JSTemporalPlainDate.create(ctx, pd.getYear(), pd.getMonth(), pd.getDay(), pd.getCalendar());
            }
            calendar = TemporalUtil.getTemporalCalendarWithISODefault(ctx, realm, valueObj);
            List<String> fieldNames = TemporalUtil.calendarFields(ctx, calendar, listDHMMMMMNSY);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(ctx, valueObj, fieldNames, listEmpty);

            DynamicObject dateOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(ctx, dateOptions, OVERFLOW, CONSTRAIN);
            result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, dateOptions);
            offset = JSObject.get(valueObj, OFFSET);
            timeZone = JSObject.get(valueObj, TIME_ZONE);
            if (offset == Undefined.instance) {
                offsetBehaviour = OffsetBehaviour.WALL;
            }
        } else {
            String string = JSRuntime.toString(value);
            JSTemporalZonedDateTimeRecord resultZDT = parseTemporalRelativeToString(string);
            result = resultZDT;
            calendar = toTemporalCalendarWithISODefault(ctx, realm, result.getCalendar());

            offset = resultZDT.getTimeZoneOffsetString();
            timeZone = resultZDT.getTimeZoneName();
            if (resultZDT.getTimeZoneZ()) {
                offsetBehaviour = OffsetBehaviour.EXACT;
            } else {
                offsetBehaviour = OffsetBehaviour.WALL;
            }
            matchBehaviour = MatchBehaviour.MATCH_MINUTES;
        }
        if (!isNullish(timeZone)) {
            DynamicObject timeZoneObj = toTemporalTimeZone(ctx, timeZone);
            timeZone = timeZoneObj;
            Object offsetNs = 0;
            if (offsetBehaviour == OffsetBehaviour.OPTION) {
                offsetNs = parseTimeZoneOffsetString(JSRuntime.toString(offset));
            } else {
                offsetNs = Undefined.instance;
            }
            BigInt epochNanoseconds = interpretISODateTimeOffset(ctx, realm,
                            result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                            result.getMicrosecond(), result.getNanosecond(), offsetBehaviour, offsetNs, timeZoneObj, COMPATIBLE, REJECT, matchBehaviour);
            return createTemporalZonedDateTime(ctx, epochNanoseconds, timeZoneObj, calendar);
        }
        return JSTemporalPlainDate.create(ctx, result.getYear(), result.getMonth(), result.getDay(), calendar);
    }

    private static JSTemporalZonedDateTimeRecord parseTemporalRelativeToString(String isoString) {
        JSTemporalDateTimeRecord result = parseISODateTime(isoString, true, false);
        boolean z = false;
        String offset = null;
        String timeZone = null;
        if (isoString.length() > 0) {
            try {
                JSTemporalTimeZoneRecord timeZoneResult = parseTemporalTimeZoneString(isoString);
                z = timeZoneResult.isZ();
                offset = timeZoneResult.getOffsetString();
                timeZone = timeZoneResult.getName();
            } catch (Exception ex) {
                // fall-through
            }
        } // else handled with defaults above
        return JSTemporalZonedDateTimeRecord.create(result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                        result.getMicrosecond(), result.getNanosecond(), result.getCalendar(), z, offset, timeZone);
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalMonthDayString(String string) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseMonthDay();
        if (rec != null) {
            if (rec.getZ()) {
                throw TemporalErrors.createRangeErrorUnexpectedUTCDesignator();
            }

            long y = rec.getYear() == Long.MIN_VALUE ? Long.MIN_VALUE : rec.getYear();
            long m = rec.getMonth() == Long.MIN_VALUE ? 1 : rec.getMonth();
            long d = rec.getDay() == Long.MIN_VALUE ? 1 : rec.getDay();

            // from ParseISODateTime
            if (!isValidISODate(y, m, d)) {
                throw TemporalErrors.createRangeErrorDateOutsideRange();
            }

            return JSTemporalDateTimeRecord.createCalendar(y, m, d, 0, 0, 0, 0, 0, 0, rec.getCalendar());
        }
        throw Errors.createRangeError("cannot parse MonthDay");
    }

    private static JSTemporalDateTimeRecord parseISODateTime(String string) {
        return parseISODateTime(string, false, false);
    }

    @TruffleBoundary
    private static JSTemporalDateTimeRecord parseISODateTime(String string, boolean dateExpected, boolean failWithUTCDesignator) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseISODateTime();
        if (rec != null) {
            if (dateExpected && (rec.getYear() <= 0 || rec.getMonth() < 0 || rec.getDay() < 0)) {
                throw Errors.createRangeError("cannot parse the ISO date time string");
            }
            if (failWithUTCDesignator && rec.getZ()) {
                throw TemporalErrors.createRangeErrorUnexpectedUTCDesignator();
            }

            String fraction = rec.getFraction();
            if (fraction == null) {
                fraction = "000000000";
            } else {
                fraction += "000000000";
            }

            long y = rec.getYear() == Long.MIN_VALUE ? 0 : rec.getYear();
            long m = rec.getMonth() == Long.MIN_VALUE ? 1 : rec.getMonth();
            long d = rec.getDay() == Long.MIN_VALUE ? 1 : rec.getDay();
            long h = rec.getHour() == Long.MIN_VALUE ? 0 : rec.getHour();
            long min = rec.getMinute() == Long.MIN_VALUE ? 0 : rec.getMinute();
            long s = rec.getSecond() == Long.MIN_VALUE ? 0 : rec.getSecond();
            long ms = Long.parseLong(fraction.substring(0, 3));
            long mus = Long.parseLong(fraction.substring(3, 6));
            long ns = Long.parseLong(fraction.substring(6, 9));

            if (s == 60) {
                s = 59;
            }

            if (!isValidISODate(y, m, d)) {
                throw TemporalErrors.createRangeErrorDateOutsideRange();
            }
            if (!isValidTime(h, min, s, ms, mus, ns)) {
                throw TemporalErrors.createRangeErrorTimeOutsideRange();
            }

            return JSTemporalDateTimeRecord.createCalendar(y, m, d, h, min, s, ms, mus, ns, rec.getCalendar());
        }
        throw Errors.createRangeError("cannot parse the ISO date time string");
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
    public static Double maximumTemporalDurationRoundingIncrement(String unit) {
        if (unit.equals(YEAR) || unit.equals(MONTH) || unit.equals(WEEK) || unit.equals(DAY)) {
            return null; // Undefined according to spec, we fix at consumer
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
        String secondString = String.format(":%1$02d", second);
        long fraction = (millisecond * 1_000_000) + (microsecond * 1_000) + nanosecond;
        String fractionString = "";
        if (precision.equals(AUTO)) {
            if (fraction == 0) {
                return secondString;
            }
            fractionString = fractionString.concat(String.format("%1$03d", millisecond));
            fractionString = fractionString.concat(String.format("%1$03d", microsecond));
            fractionString = fractionString.concat(String.format("%1$03d", nanosecond));
            fractionString = longestSubstring(fractionString);
        } else {
            if (precision.equals(0)) {
                return secondString;
            }
            fractionString = fractionString.concat(String.format("%1$03d", millisecond));
            fractionString = fractionString.concat(String.format("%1$03d", microsecond));
            fractionString = fractionString.concat(String.format("%1$03d", nanosecond));
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
    @TruffleBoundary
    public static BigDecimal roundHalfAwayFromZero(BigDecimal x) {
        if (x.compareTo(BigDecimal.ZERO) < 0) {
            return x.setScale(0, RoundingMode.HALF_DOWN);
        } else {
            return x.setScale(0, RoundingMode.HALF_UP);
        }
    }

    // 13.37
    @TruffleBoundary
    public static long roundNumberToIncrement(double x, double increment, String roundingMode) {
        return roundNumberToIncrement(BigDecimal.valueOf(x), BigDecimal.valueOf(increment), roundingMode);
    }

    @TruffleBoundary // could throw exception
    public static long roundNumberToIncrement(BigDecimal x, BigDecimal increment, String roundingMode) {
        assert roundingMode.equals(CEIL) || roundingMode.equals(FLOOR) || roundingMode.equals(TRUNC) || roundingMode.equals(HALF_EXPAND);

        // algorithm from polyfill
        BigDecimal[] divRes = x.divideAndRemainder(increment);
        BigDecimal quotient = divRes[0];
        BigDecimal remainder = divRes[1];
        int sign = remainder.signum() < 0 ? -1 : 1;

        if (roundingMode.equals(CEIL)) {
            if (sign > 0) {
                quotient = quotient.add(BigDecimal.ONE);
            }
        } else if (roundingMode.equals(FLOOR)) {
            if (sign < 0) {
                quotient = quotient.add(BigDecimal.ONE);
            }
        } else if (roundingMode.equals(TRUNC)) {
            // divMod already is truncation
        } else if (roundingMode.equals(HALF_EXPAND)) {
            if (remainder.multiply(BigDecimal.valueOf(2)).abs().compareTo(increment) >= 0) {
                quotient = quotient.add(BigDecimal.valueOf(sign));
            }
        }
        BigDecimal result = quotient.multiply(increment);
        return result.longValue();
    }

    // 13.43
    @TruffleBoundary
    public static String parseTemporalCalendarString(String string) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseCalendarString();
        if (rec == null) {
            throw Errors.createRangeError("cannot parse Calendar");
        }
        String id = rec.getCalendar();
        if (isNullish(id)) {
            return ISO8601;
        }
        return id;
    }

    // 13.51
    public static long toPositiveInteger(Object value) {
        long integer = toIntegerThrowOnInfinity(value);
        if (integer <= 0) {
            throw Errors.createRangeError("positive value expected");
        }
        return integer;
    }

    // 13.52
    @TruffleBoundary
    public static DynamicObject prepareTemporalFields(JSContext ctx, DynamicObject fields, List<String> fieldNames, List<String> requiredFields) {
        JSRealm realm = JSRealm.get(null);
        DynamicObject result = JSOrdinary.create(ctx, realm);
        for (String property : fieldNames) {
            Object value = JSObject.get(fields, property);
            if (isNullish(value)) {
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
            createDataPropertyOrThrow(ctx, result, property, value);
        }
        return result;
    }

    @TruffleBoundary
    public static DynamicObject preparePartialTemporalFields(JSContext ctx, DynamicObject fields, List<String> fieldNames) {
        JSRealm realm = JSRealm.get(null);
        DynamicObject result = JSOrdinary.create(ctx, realm);
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
            createDataPropertyOrThrow(ctx, result, property, value);
        }
        if (!any) {
            throw Errors.createTypeError("Given dateTime like object has no relevant properties.");
        }
        return result;
    }

    @TruffleBoundary
    private static <T, I> Map<T, I> toMap(T[] keys, I[] values) {
        Map<T, I> map = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }
        return map;
    }

    public static JSTemporalYearMonthDayRecord regulateISOYearMonth(long year, long month, TemporalOverflowEnum overflow) {
        assert isInteger(year);
        assert isInteger(month);
        assert TemporalOverflowEnum.CONSTRAIN == overflow || TemporalOverflowEnum.REJECT == overflow;

        if (TemporalOverflowEnum.CONSTRAIN == overflow) {
            return constrainISOYearMonth(year, month);
        } else if (TemporalOverflowEnum.REJECT == overflow) {
            if (!validateISOYearMonth(year, month)) {
                throw Errors.createRangeError("validation of year and month failed");
            }
        }
        return JSTemporalYearMonthDayRecord.create(year, month);
    }

    private static boolean validateISOYearMonth(long year, long month) {
        assert isInteger(year);
        assert isInteger(month);
        return (1 <= month) && (month <= 12);
    }

    private static JSTemporalYearMonthDayRecord constrainISOYearMonth(long year, long month) {
        assert isInteger(year);
        assert isInteger(month);

        long monthPrepared = constrainToRange(month, 1, 12);
        return JSTemporalYearMonthDayRecord.create(year, monthPrepared);
    }

    // 12.1.35
    // Formula: https://cs.uwaterloo.ca/~alopez-o/math-faq/node73.html
    public static long toISODayOfWeek(long year, long month, long day) {
        long m = month - 2;
        if (m == -1) {  // Jan
            m = 11;
        } else if (m == 0) { // Feb
            m = 12;
        }
        long c = Math.floorDiv(year, 100);
        long y = Math.floorMod(year, 100);
        if (m == 11 || m == 12) {
            y = y - 1;
        }
        long weekDay = Math.floorMod((day + (long) Math.floor((2.6 * m) - 0.2) - (2 * c) + y + Math.floorDiv(y, 4) + Math.floorDiv(c, 4)), 7);
        if (weekDay == 0) { // Sunday
            return 7;
        }
        return weekDay;
    }

    // 12.1.36
    public static long toISODayOfYear(long year, long month, long day) {
        long days = 0;
        for (int m = 1; m < month; m++) {
            days += isoDaysInMonth(year, m);
        }
        return days + day;
    }

    // 12.1.37
    public static long toISOWeekOfYear(long year, long month, long day) {
        long doy = toISODayOfYear(year, month, day);
        long dow = toISODayOfWeek(year, month, day);
        long doj = toISODayOfWeek(year, 1, 1);

        long week = Math.floorDiv(doy - dow + 10, 7);
        if (week < 1) {
            if (doj == 5 || (doj == 6 && isISOLeapYear(year - 1))) {
                return 53;
            } else {
                return 52;
            }
        }
        if (week == 53) {
            if (isoDaysInYear(year) - doy < 4 - dow) {
                return 1;
            }
        }

        return week;
    }

    // 12.1.32
    public static boolean isISOLeapYear(long year) {
        if (year % 4 != 0) {
            return false;
        }
        if (year % 400 == 0) {
            return true;
        }
        if (year % 100 == 0) {
            return false;
        }
        return true;
    }

    // 12.1.33
    public static long isoDaysInYear(long year) {
        if (isISOLeapYear(year)) {
            return 366;
        }
        return 365;
    }

    // 12.1.34
    public static long isoDaysInMonth(long year, long month) {
        assert month >= 1 && month <= 12;
        if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {
            return 31;
        }
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        }
        if (isISOLeapYear(year)) {
            return 29;
        }
        return 28;
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

    /**
     * Note there also is {@link GetTemporalCalendarWithISODefaultNode}.
     */
    public static DynamicObject getTemporalCalendarWithISODefault(JSContext ctx, JSRealm realm, Object item) {
        if (item instanceof TemporalCalendar) {
            return ((TemporalCalendar) item).getCalendar();
        } else {
            Object calendar = JSObject.get(toDynamicObject(item), TemporalConstants.CALENDAR);
            return toTemporalCalendarWithISODefault(ctx, realm, calendar);
        }
    }

    public static DynamicObject getISO8601Calendar(JSRealm realm) {
        return getBuiltinCalendar(realm, ISO8601);
    }

    @TruffleBoundary
    public static DynamicObject getBuiltinCalendar(JSRealm realm, String id) {
        Object cal = JSRuntime.construct(realm.getTemporalCalendarConstructor(), new Object[]{id});
        return toDynamicObject(cal);
    }

    /**
     * Note there also is {@link ToTemporalCalendarNode}.
     */
    @TruffleBoundary
    public static DynamicObject toTemporalCalendar(JSContext ctx, Object temporalCalendarLikeParam) {
        Object temporalCalendarLike = temporalCalendarLikeParam;
        if (JSRuntime.isObject(temporalCalendarLike)) {
            DynamicObject obj = (DynamicObject) temporalCalendarLike;
            if (temporalCalendarLike instanceof TemporalCalendar) {
                return ((TemporalCalendar) temporalCalendarLike).getCalendar();
            }
            if (!JSObject.hasProperty(obj, CALENDAR)) {
                return obj;
            }
            temporalCalendarLike = JSObject.get(obj, CALENDAR);
            if (JSRuntime.isObject(temporalCalendarLike) && !JSObject.hasProperty((DynamicObject) temporalCalendarLike, CALENDAR)) {
                return (DynamicObject) temporalCalendarLike;
            }
        }
        String identifier = JSRuntime.toString(temporalCalendarLike);
        if (!JSTemporalCalendar.isBuiltinCalendar(identifier)) {
            identifier = parseTemporalCalendarString(identifier);
            if (!JSTemporalCalendar.isBuiltinCalendar(identifier)) {
                throw TemporalErrors.createRangeErrorCalendarUnknown();
            }
        }
        return JSTemporalCalendar.create(ctx, identifier);
    }

    @TruffleBoundary
    public static List<String> calendarFields(JSContext ctx, DynamicObject calendar, List<String> strings) {
        Object fields = JSObject.getMethod(calendar, TemporalConstants.FIELDS);
        if (fields == Undefined.instance) {
            return strings;
        } else {
            JSRealm realm = JSRealm.get(null);
            DynamicObject fieldsArray = JSArray.createConstant(ctx, realm, strings.toArray(new String[]{}));
            fieldsArray = toDynamicObject(JSRuntime.call(fields, calendar, new Object[]{fieldsArray}));
            return iterableToListOfTypeString(fieldsArray);
        }
    }

    @TruffleBoundary
    public static List<String> iterableToListOfTypeString(DynamicObject items) {
        IteratorRecord iter = JSRuntime.getIterator(items /* , sync */);
        List<String> values = new ArrayList<>();
        Object next = Boolean.TRUE;
        while (next != Boolean.FALSE) {
            next = JSRuntime.iteratorStep(iter);
            if (next != Boolean.FALSE) {
                Object nextValue = JSRuntime.iteratorValue((DynamicObject) next);
                if (!JSRuntime.isString(nextValue)) {
                    JSRuntime.iteratorClose(iter.getIterator());
                    throw Errors.createTypeError("string expected");
                }
                String str = JSRuntime.toString(nextValue);
                values.add(str);
            }
        }
        return values;
    }

    @TruffleBoundary
    public static Set<String> arrayToStringSet(DynamicObject array) {
        Object lObj = JSObject.get(array, JSArray.LENGTH);
        long len = ((Number) lObj).longValue();
        Set<String> set = new HashSet<>();
        for (int i = 0; i < len; i++) {
            set.add(JSObject.get(array, i).toString());
        }
        return set;
    }

    public static JSTemporalPlainDateObject dateFromFields(DynamicObject calendar, DynamicObject fields, Object options) {
        Object dateFromFields = JSObject.get(calendar, TemporalConstants.DATE_FROM_FIELDS);
        Object date = JSRuntime.call(dateFromFields, calendar, new Object[]{fields, options});
        return requireTemporalDate(date);
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalDateTimeString(String string) {
        // TODO 2. If isoString does not satisfy the syntax of a TemporalDateTimeString (see 13.39)
        JSTemporalDateTimeRecord result = parseISODateTime(string, false, true);
        return result;
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalDateString(String string) {
        // TODO 2. If isoString does not satisfy the syntax of a TemporalDateTimeString (see 13.39)
        JSTemporalDateTimeRecord result = parseISODateTime(string, false, true);
        return JSTemporalDateTimeRecord.createCalendar(result.getYear(), result.getMonth(), result.getDay(), 0, 0, 0, 0, 0, 0, result.getCalendar());
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalTimeString(String string) {
        JSTemporalDateTimeRecord result = parseISODateTime(string, false, true);
        if (result.hasCalendar()) {
            return JSTemporalDateTimeRecord.createCalendar(0, 0, 0, result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(),
                            result.getCalendar());
        } else {
            return JSTemporalDateTimeRecord.create(0, 0, 0, result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
        }
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

    /**
     * Note there also is {@link ToTemporalTimeZoneNode}.
     */
    public static DynamicObject toTemporalTimeZone(JSContext ctx, Object temporalTimeZoneLikeParam) {
        Object temporalTimeZoneLike = temporalTimeZoneLikeParam;
        if (JSRuntime.isObject(temporalTimeZoneLike)) {
            DynamicObject tzObj = (DynamicObject) temporalTimeZoneLike;
            if (isTemporalZonedDateTime(tzObj)) {
                return ((JSTemporalZonedDateTimeObject) tzObj).getTimeZone();
            } else if (!JSObject.hasProperty(tzObj, TIME_ZONE)) {
                return tzObj;
            }
            temporalTimeZoneLike = JSObject.get(tzObj, TIME_ZONE);
            if (JSRuntime.isObject(temporalTimeZoneLike) && !JSObject.hasProperty((DynamicObject) temporalTimeZoneLike, TIME_ZONE)) {
                return (DynamicObject) temporalTimeZoneLike;
            }
        }
        String identifier = JSRuntime.toString(temporalTimeZoneLike);
        return createTemporalTimeZone(ctx, parseTemporalTimeZone(identifier));
    }

    public static DynamicObject createTemporalTimeZone(JSContext ctx, String identifier) {
        BigInt offset = null;
        try {
            long offsetLong = parseTimeZoneOffsetString(identifier);
            offset = new BigInt(Boundaries.bigIntegerValueOf(offsetLong));
        } catch (Exception ex) {
            assert canonicalizeTimeZoneName(identifier).equals(identifier);
            offset = null;
        }
        return JSTemporalTimeZone.create(ctx, offset, identifier);
    }

    public static String canonicalizeTimeZoneName(String timeZone) {
        assert isValidTimeZoneName(timeZone);
        return JSDateTimeFormat.canonicalizeTimeZoneName(timeZone);
    }

    public static boolean isValidTimeZoneName(String timeZone) {
        return JSDateTimeFormat.canonicalizeTimeZoneName(timeZone) != null;
    }

    public static JSTemporalPlainDateTimeObject createTemporalDateTime(JSContext context, long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond, DynamicObject calendar) {
        return JSTemporalPlainDateTime.create(context, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, calendar);
    }

    public static JSTemporalZonedDateTimeObject createTemporalZonedDateTime(JSContext ctx, BigInt ns, DynamicObject timeZone, DynamicObject calendar) {
        return (JSTemporalZonedDateTimeObject) JSTemporalZonedDateTime.create(ctx, ns, timeZone, calendar);
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
    public static Object getOrDefault(DynamicObject ob, String key, Object defaultValue) {
        Object value = JSObject.get(ob, key);
        return value != null ? value : defaultValue;
    }

    @TruffleBoundary
    public static boolean isoDateTimeWithinLimits(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond) {
        BigInteger ns = getEpochFromISOParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);

        if (ns.compareTo(isoTimeLowerBound) <= 0 || ns.compareTo(isoTimeUpperBound) >= 0) {
            return false;
        }
        return true;
    }

    @TruffleBoundary
    public static BigInteger getEpochFromISOParts(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond) {
        assert isValidISODate(year, month, day);
        assert isValidTime(hour, minute, second, millisecond, microsecond, nanosecond);

        double date = JSDate.makeDay(year, month - 1, day);
        double time = JSDate.makeTime(hour, minute, second, millisecond);
        double ms = JSDate.makeDate(date, time);
        assert isFinite(ms);

        BigInteger bi = BigInteger.valueOf((long) ms).multiply(BigInteger.valueOf(1_000_000L));
        BigInteger bims = BigInteger.valueOf(microsecond).multiply(BigInteger.valueOf(1000L));
        BigInteger biresult = bi.add(bims).add(BigInteger.valueOf(nanosecond));

        return biresult;
    }

    private static boolean isFinite(double d) {
        return !(Double.isNaN(d) || Double.isInfinite(d));
    }

    public static TemporalOverflowEnum toTemporalOverflow(DynamicObject options, TemporalGetOptionNode getOptionNode) {
        String result = (String) getOptionNode.execute(options, OVERFLOW, OptionTypeEnum.STRING, listConstrainReject, CONSTRAIN);
        return overflowStringToEnum(result);
    }

    public static TemporalOverflowEnum toTemporalOverflow(DynamicObject options) {
        String result = (String) getOption(options, OVERFLOW, OptionTypeEnum.STRING, listConstrainReject, CONSTRAIN);
        return overflowStringToEnum(result);
    }

    @TruffleBoundary
    public static TemporalOverflowEnum overflowStringToEnum(String result) {
        if (CONSTRAIN.equals(result)) {
            return TemporalOverflowEnum.CONSTRAIN;
        } else if (TemporalConstants.REJECT.equals(result)) {
            return TemporalOverflowEnum.REJECT;
        }
        CompilerDirectives.transferToInterpreter();
        throw Errors.shouldNotReachHere("unknown overflow type: " + result);
    }

    public static JSTemporalDateTimeRecord interpretTemporalDateTimeFields(DynamicObject calendar, DynamicObject fields, DynamicObject options) {
        JSTemporalDateTimeRecord timeResult = toTemporalTimeRecord(fields);
        JSTemporalPlainDateObject date = dateFromFields(calendar, fields, options);
        TemporalOverflowEnum overflow = toTemporalOverflow(options);
        JSTemporalDurationRecord timeResult2 = TemporalUtil.regulateTime(
                        timeResult.getHour(), timeResult.getMinute(), timeResult.getSecond(), timeResult.getMillisecond(), timeResult.getMicrosecond(), timeResult.getNanosecond(),
                        overflow);

        return JSTemporalDateTimeRecord.create(
                        date.getYear(), date.getMonth(), date.getDay(),
                        timeResult2.getHours(), timeResult2.getMinutes(), timeResult2.getSeconds(),
                        timeResult2.getMilliseconds(), timeResult2.getMicroseconds(), timeResult2.getNanoseconds());
    }

    public static JSTemporalDurationRecord regulateTime(long hours, long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds, TemporalOverflowEnum overflow) {
        assert overflow == TemporalOverflowEnum.CONSTRAIN || overflow == TemporalOverflowEnum.REJECT;
        if (overflow == TemporalOverflowEnum.CONSTRAIN) {
            return constrainTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        } else {
            if (!TemporalUtil.isValidTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
                throw Errors.createRangeError("Given time outside the range.");
            }
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
        boolean any = false;

        long hour = 0;
        long minute = 0;
        long second = 0;
        long millisecond = 0;
        long microsecond = 0;
        long nanosecond = 0;

        for (String property : TemporalUtil.TIME_LIKE_PROPERTIES) {
            Object val = JSObject.get(temporalTimeLike, property);

            long lVal = 0;
            if (val == Undefined.instance) {
                lVal = 0;
            } else {
                any = true;
                lVal = TemporalUtil.toIntegerThrowOnInfinity(val);
            }
            switch (property) {
                case HOUR:
                    hour = lVal;
                    break;
                case MINUTE:
                    minute = lVal;
                    break;
                case SECOND:
                    second = lVal;
                    break;
                case MILLISECOND:
                    millisecond = lVal;
                    break;
                case MICROSECOND:
                    microsecond = lVal;
                    break;
                case NANOSECOND:
                    nanosecond = lVal;
                    break;
            }
        }
        if (!any) {
            throw Errors.createTypeError("at least one time-like field expected");
        }
        return JSTemporalDateTimeRecord.create(0, 0, 0, hour, minute, second, millisecond, microsecond, nanosecond);
    }

    public static long toIntegerThrowOnInfinity(Object value) {
        Number integer = toIntegerOrInfinity(value);
        if (Double.isInfinite(integer.doubleValue())) {
            throw Errors.createRangeError("value outside bounds");
        }
        return integer.longValue();
    }

    public static long toIntegerWithoutRounding(Object argument) {
        Number number = JSRuntime.toNumber(argument);
        double dNumber = number.doubleValue();
        if (Double.isNaN(dNumber) || dNumber == 0.0d) {
            return 0;
        }
        if (!JSRuntime.isIntegralNumber(dNumber)) {
            throw Errors.createRangeError("value expected to be integer");
        }
        return number.longValue();
    }

    @TruffleBoundary
    public static Number toIntegerOrInfinity(Object value) {
        Number number = JSRuntime.toNumber(value);
        double d = number.doubleValue();
        if (d == 0 || Double.isNaN(d)) {
            return 0L;
        }
        if (Double.isInfinite(d)) {
            return d;
        }
        return number.longValue();
    }

    public static DynamicObject calendarDateAdd(DynamicObject calendar, DynamicObject datePart, DynamicObject dateDuration, DynamicObject options) {
        return calendarDateAdd(calendar, datePart, dateDuration, options, Undefined.instance);
    }

    public static JSTemporalPlainDateObject calendarDateAdd(DynamicObject calendar, DynamicObject date, DynamicObject duration, DynamicObject options, Object dateAdd) {
        Object dateAddPrepared = dateAdd;
        if (dateAddPrepared == Undefined.instance) {
            dateAddPrepared = JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
        }
        Object addedDate = JSRuntime.call(dateAddPrepared, calendar, new Object[]{date, duration, options});
        return requireTemporalDate(addedDate);
    }

    public static JSTemporalDurationObject calendarDateUntil(DynamicObject calendar, DynamicObject one, DynamicObject two, DynamicObject options) {
        return calendarDateUntil(calendar, one, two, options, Undefined.instance);
    }

    public static JSTemporalDurationObject calendarDateUntil(DynamicObject calendar, DynamicObject one, DynamicObject two, DynamicObject options, Object dateUntil) {
        Object dateUntilPrepared = dateUntil;
        if (dateUntilPrepared == Undefined.instance) {
            dateUntilPrepared = JSObject.getMethod(calendar, TemporalConstants.DATE_UNTIL);
        }
        Object date = JSRuntime.call(dateUntilPrepared, calendar, new Object[]{one, two, options});
        return requireTemporalDuration(date);
    }

    @TruffleBoundary
    public static long roundTemporalInstant(BigInt ns, long increment, String unit, String roundingMode) {
        return roundTemporalInstant(new BigDecimal(ns.bigIntegerValue()), increment, unit, roundingMode);
    }

    @TruffleBoundary
    public static long roundTemporalInstant(BigDecimal ns, long increment, String unit, String roundingMode) {
        BigDecimal incrementNs = BigDecimal.valueOf(increment);
        if (HOUR.equals(unit)) {
            incrementNs = incrementNs.multiply(BigDecimal.valueOf(3_600_000_000_000L));
        } else if (MINUTE.equals(unit)) {
            incrementNs = incrementNs.multiply(BigDecimal.valueOf(60_000_000_000L));
        } else if (SECOND.equals(unit)) {
            incrementNs = incrementNs.multiply(BigDecimal.valueOf(1_000_000_000L));
        } else if (MILLISECOND.equals(unit)) {
            incrementNs = incrementNs.multiply(BigDecimal.valueOf(1_000_000L));
        } else if (MICROSECOND.equals(unit)) {
            incrementNs = incrementNs.multiply(BigDecimal.valueOf(1_000L));
        } else {
            assert NANOSECOND.equals(unit);

        }
        return roundNumberToIncrement(ns, incrementNs, roundingMode);
    }

    /**
     * Note there also is {@link ToTemporalDateNode}.
     */
    public static JSTemporalPlainDateObject toTemporalDate(JSContext ctx, JSRealm realm, Object itemParam, DynamicObject optionsParam) {
        DynamicObject options = isNullish(optionsParam) ? JSOrdinary.createWithNullPrototype(ctx) : optionsParam;
        if (JSRuntime.isObject(itemParam)) {
            DynamicObject item = (DynamicObject) itemParam;
            if (JSTemporalPlainDate.isJSTemporalPlainDate(item)) {
                return (JSTemporalPlainDateObject) item;
            } else if (JSTemporalZonedDateTime.isJSTemporalZonedDateTime(item)) {
                JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) item;
                JSTemporalInstantObject instant = JSTemporalInstant.create(ctx, zdt.getNanoseconds());
                JSTemporalPlainDateTimeObject plainDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, zdt.getTimeZone(), instant, zdt.getCalendar());
                return TemporalUtil.createTemporalDate(ctx, plainDateTime.getYear(), plainDateTime.getMonth(), plainDateTime.getDay(), plainDateTime.getCalendar());
            } else if (JSTemporalPlainDateTime.isJSTemporalPlainDateTime(item)) {
                JSTemporalPlainDateTimeObject dt = (JSTemporalPlainDateTimeObject) item;
                return TemporalUtil.createTemporalDate(ctx, dt.getYear(), dt.getMonth(), dt.getDay(), dt.getCalendar());
            }
            DynamicObject calendar = getTemporalCalendarWithISODefault(ctx, realm, item);
            List<String> fieldNames = calendarFields(ctx, calendar, listDMMCY);
            DynamicObject fields = prepareTemporalFields(ctx, item, fieldNames, listEmpty);
            return dateFromFields(calendar, fields, options);
        }
        TemporalOverflowEnum overflows = toTemporalOverflow(options);
        JSTemporalDateTimeRecord result = parseTemporalDateString(JSRuntime.toString(itemParam));
        if (!validateISODate(result.getYear(), result.getMonth(), result.getDay())) {
            throw TemporalErrors.createRangeErrorDateOutsideRange();
        }
        DynamicObject calendar = toTemporalCalendarWithISODefault(ctx, realm, result.getCalendar());
        result = regulateISODate(result.getYear(), result.getMonth(), result.getDay(), overflows);

        return (JSTemporalPlainDateObject) JSTemporalPlainDate.create(ctx, result.getYear(), result.getMonth(), result.getDay(), calendar);
    }

    /**
     * Note there also is {@link ToTemporalTimeNode}.
     */
    public static JSTemporalPlainTimeObject toTemporalTime(Object item, TemporalOverflowEnum overflowParam, JSContext ctx, JSRealm realm, IsObjectNode isObject, JSToStringNode toStringNode) {
        TemporalOverflowEnum overflow = overflowParam == null ? TemporalOverflowEnum.CONSTRAIN : overflowParam;
        assert overflow == TemporalOverflowEnum.CONSTRAIN || overflow == TemporalOverflowEnum.REJECT;
        JSTemporalDurationRecord result2 = null;
        if (isObject.executeBoolean(item)) {
            if (JSTemporalPlainTime.isJSTemporalPlainTime(item)) {
                return (JSTemporalPlainTimeObject) item;
            } else if (TemporalUtil.isTemporalZonedDateTime(item)) {
                JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) item;
                JSTemporalInstantObject instant = JSTemporalInstant.create(ctx, zdt.getNanoseconds());
                JSTemporalPlainDateTimeObject plainDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, zdt.getTimeZone(), instant, zdt.getCalendar());
                return TemporalUtil.createTemporalTime(ctx, plainDateTime.getHour(), plainDateTime.getMinute(),
                                plainDateTime.getSecond(), plainDateTime.getMillisecond(), plainDateTime.getMicrosecond(), plainDateTime.getNanosecond());
            } else if (JSTemporalPlainDateTime.isJSTemporalPlainDateTime(item)) {
                JSTemporalPlainDateTimeObject dt = (JSTemporalPlainDateTimeObject) item;
                return TemporalUtil.createTemporalTime(ctx, dt.getHour(), dt.getMinute(), dt.getSecond(), dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond());
            }
            DynamicObject calendar = TemporalUtil.getTemporalCalendarWithISODefault(ctx, realm, item);
            if (!JSRuntime.toString(calendar).equals(TemporalConstants.ISO8601)) {
                throw TemporalErrors.createRangeErrorTemporalISO8601Expected();
            }
            JSTemporalDateTimeRecord result = TemporalUtil.toTemporalTimeRecord((DynamicObject) item);
            result2 = TemporalUtil.regulateTime(
                            result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), overflow);
        } else {
            String string = toStringNode.executeString(item);
            JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalTimeString(string);
            assert TemporalUtil.isValidTime(
                            result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
            if (result.hasCalendar() && !JSRuntime.toString(result.getCalendar()).equals(TemporalConstants.ISO8601)) {
                throw TemporalErrors.createRangeErrorTemporalISO8601Expected();
            }
            result2 = JSTemporalDurationRecord.create(result);
        }
        return (JSTemporalPlainTimeObject) JSTemporalPlainTime.create(ctx, result2.getHours(), result2.getMinutes(), result2.getSeconds(), result2.getMilliseconds(), result2.getMicroseconds(),
                        result2.getNanoseconds());
    }

    // 3.5.8
    public static boolean validateISODate(long year, long month, long day) {
        if (month < 1 || month > 12) {
            return false;
        }
        long daysInMonth = isoDaysInMonth(year, month);
        if (day < 1 || day > daysInMonth) {
            return false;
        }
        return true;
    }

    // 3.5.6
    public static DynamicObject toTemporalDateFields(JSContext ctx, DynamicObject temporalDateLike, List<String> fieldNames) {
        return TemporalUtil.prepareTemporalFields(ctx, temporalDateLike, fieldNames, listEmpty);
    }

    // 3.5.7
    @TruffleBoundary
    public static JSTemporalDateTimeRecord regulateISODate(long yearParam, long monthParam, long dayParam, TemporalOverflowEnum overflow) {
        assert overflow == TemporalOverflowEnum.CONSTRAIN || overflow == TemporalOverflowEnum.REJECT;
        long year = yearParam;
        long month = monthParam;
        long day = dayParam;
        if (overflow == TemporalOverflowEnum.REJECT) {
            if (!isValidISODate(year, month, day)) {
                throw TemporalErrors.createRangeErrorDateOutsideRange();
            }
            return JSTemporalDateTimeRecord.create(year, month, day, 0, 0, 0, 0, 0, 0);
        }
        if (overflow == TemporalOverflowEnum.CONSTRAIN) {
            month = constrainToRange(month, 1, 12);
            day = constrainToRange(day, 1, isoDaysInMonth(year, month));
            return JSTemporalDateTimeRecord.create(year, month, day, 0, 0, 0, 0, 0, 0);
        }
        throw new RuntimeException("This should never have happened.");
    }

    // 3.5.11
    public static JSTemporalDateTimeRecord balanceISODate(long yearParam, long monthParam, long dayParam) {
        JSTemporalDateTimeRecord balancedYearMonth = balanceISOYearMonth(yearParam, monthParam);
        long month = balancedYearMonth.getMonth();
        long year = balancedYearMonth.getYear();
        long day = dayParam;
        long testYear;
        if (month > 2) {
            testYear = year;
        } else {
            testYear = year - 1;
        }
        while (day < -1 * isoDaysInYear(testYear)) {
            day = day + isoDaysInYear(testYear);
            year = year - 1;
            testYear = testYear - 1;
        }
        testYear = testYear + 1;
        while (day > isoDaysInYear(testYear)) {
            day = day - isoDaysInYear(testYear);
            year = year + 1;
            testYear = testYear + 1;
        }
        while (day < 1) {
            balancedYearMonth = balanceISOYearMonth(year, month - 1);
            year = balancedYearMonth.getYear();
            month = balancedYearMonth.getMonth();
            day = day + isoDaysInMonth(year, month);
        }
        while (day > isoDaysInMonth(year, month)) {
            day = day - isoDaysInMonth(year, month);
            balancedYearMonth = balanceISOYearMonth(year, month + 1);
            year = balancedYearMonth.getYear();
            month = balancedYearMonth.getMonth();
        }
        return JSTemporalPlainDate.toRecord(year, month, day);
    }

    // 3.5.14
    @TruffleBoundary
    public static JSTemporalDateTimeRecord addISODate(long year, long month, long day, long years, long months, long weeks, long daysP, TemporalOverflowEnum overflow) {
        assert overflow == TemporalOverflowEnum.CONSTRAIN || overflow == TemporalOverflowEnum.REJECT;

        long days = daysP;
        JSTemporalDateTimeRecord intermediate = balanceISOYearMonth(year + years, month + months);
        intermediate = regulateISODate(intermediate.getYear(), intermediate.getMonth(), day, overflow);
        days = days + 7 * weeks;
        long d = intermediate.getDay() + days;
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

    public static JSTemporalPlainDateObject requireTemporalDate(Object obj) {
        if (!(obj instanceof JSTemporalPlainDateObject)) {
            throw TemporalErrors.createTypeErrorTemporalDateExpected();
        }
        return (JSTemporalPlainDateObject) obj;
    }

    public static JSTemporalPlainDateTimeObject requireTemporalDateTime(Object obj) {
        if (!(obj instanceof JSTemporalPlainDateTimeObject)) {
            throw TemporalErrors.createTypeErrorTemporalDateTimeExpected();
        }
        return (JSTemporalPlainDateTimeObject) obj;
    }

    public static JSTemporalDurationObject requireTemporalDuration(Object obj) {
        if (!(obj instanceof JSTemporalDurationObject)) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
        return (JSTemporalDurationObject) obj;
    }

    public static JSTemporalPlainMonthDayObject requireTemporalMonthDay(Object obj) {
        if (!(obj instanceof JSTemporalPlainMonthDayObject)) {
            throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
        }
        return (JSTemporalPlainMonthDayObject) obj;
    }

    public static JSTemporalPlainYearMonthObject requireTemporalYearMonth(Object obj) {
        if (!(obj instanceof JSTemporalPlainYearMonthObject)) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
        return (JSTemporalPlainYearMonthObject) obj;
    }

    public static boolean isTemporalZonedDateTime(Object obj) {
        return JSTemporalZonedDateTime.isJSTemporalZonedDateTime(obj);
    }

    public static String toShowCalendarOption(DynamicObject options, TemporalGetOptionNode getOptionNode) {
        return (String) getOptionNode.execute(options, "calendarName", OptionTypeEnum.STRING, listAutoAlwaysNever, AUTO);
    }

    @TruffleBoundary
    public static String padISOYear(long year) {
        if (999 < year && year < 9999) {
            return String.valueOf(year);
        }
        String sign = year >= 0 ? "+" : "-";
        long y = Math.abs(year);
        String result = sign + String.format("%1$06d", y);
        return result;
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
    public static DynamicObject toTemporalYearMonth(JSContext ctx, JSRealm realm, Object item, DynamicObject optParam) {
        DynamicObject options = optParam;
        if (optParam == Undefined.instance) {
            options = JSOrdinary.createWithNullPrototype(ctx);
        }
        if (JSRuntime.isObject(item)) {
            DynamicObject itemObj = (DynamicObject) item;
            if (JSTemporalPlainYearMonth.isJSTemporalPlainYearMonth(itemObj)) {
                return itemObj;
            }
            DynamicObject calendar = TemporalUtil.getTemporalCalendarWithISODefault(ctx, realm, itemObj);

            List<String> fieldNames = TemporalUtil.calendarFields(ctx, calendar, listMMCY);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(ctx, itemObj, fieldNames, listEmpty);
            return yearMonthFromFields(calendar, fields, options);
        }
        TemporalUtil.toTemporalOverflow(options);

        String string = JSRuntime.toString(item);
        JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalYearMonthString(string);
        DynamicObject calendar = toTemporalCalendarWithISODefault(ctx, realm, result.getCalendar());
        DynamicObject result2 = JSTemporalPlainYearMonth.create(ctx, result.getYear(), result.getMonth(), calendar, result.getDay());
        DynamicObject canonicalYearMonthOptions = JSOrdinary.createWithNullPrototype(ctx);
        return yearMonthFromFields(calendar, result2, canonicalYearMonthOptions);
    }

    public static DynamicObject yearMonthFromFields(DynamicObject calendar, DynamicObject fields, DynamicObject optionsParam) {
        DynamicObject options;
        if (isNullish(optionsParam)) {
            options = Undefined.instance;
        } else {
            options = optionsParam;
        }
        Object fn = JSObject.getMethod(calendar, "yearMonthFromFields");
        Object yearMonth = JSRuntime.call(fn, calendar, new Object[]{fields, options});
        return requireTemporalYearMonth(yearMonth);
    }

    public static DynamicObject monthDayFromFields(DynamicObject calendar, DynamicObject fields, DynamicObject options) {
        Object fn = JSObject.getMethod(calendar, "monthDayFromFields");
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

    public static void rejectTemporalCalendarType(DynamicObject obj, BranchProfile errorBranch) {
        if (obj instanceof JSTemporalPlainDateObject || obj instanceof JSTemporalPlainDateTimeObject || obj instanceof JSTemporalPlainMonthDayObject ||
                        obj instanceof JSTemporalPlainTimeObject || obj instanceof JSTemporalPlainYearMonthObject || isTemporalZonedDateTime(obj)) {
            errorBranch.enter();
            throw Errors.createTypeError("rejecting calendar types");
        }
    }

    public static JSTemporalPlainTimeObject createTemporalTime(JSContext ctx, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond) {
        return (JSTemporalPlainTimeObject) JSTemporalPlainTime.create(ctx, hour, minute, second, millisecond, microsecond, nanosecond);
    }

    public static JSTemporalPlainDateObject createTemporalDate(JSContext context, long year, long month, long day, DynamicObject calendar) {
        return (JSTemporalPlainDateObject) JSTemporalPlainDate.create(context, year, month, day, calendar);
    }

    public static long remainder(long x, long y) {
        long magnitude = x % y;
        // assert Math.signum(y) == Math.signum(magnitude);

        long m2 = Math.abs(x) % y;
        if (x < 0 && m2 > 0) {
            m2 = -m2;
        }
        return magnitude;
    }

    public static double remainder(double x, double y) {
        double magnitude = x % y;
        // assert Math.signum(y) == Math.signum(magnitude);
        return magnitude;
    }

    public static long getPropertyFromRecord(JSTemporalDurationRecord d, String property) {
        switch (property) {
            case YEARS:
                return d.getYears();
            case MONTHS:
                return d.getMonths();
            case WEEKS:
                return d.getWeeks();
            case DAYS:
                return d.getDays();
            case HOURS:
                return d.getHours();
            case MINUTES:
                return d.getMinutes();
            case SECONDS:
                return d.getSeconds();
            case MILLISECONDS:
                return d.getMilliseconds();
            case MICROSECONDS:
                return d.getMicroseconds();
            case NANOSECONDS:
                return d.getNanoseconds();
        }
        CompilerDirectives.transferToInterpreter();
        throw Errors.createTypeError("unknown property");
    }

    public static DynamicObject calendarMergeFields(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, DynamicObject calendar, DynamicObject fields, DynamicObject additionalFields) {
        Object mergeFields = JSObject.getMethod(calendar, TemporalConstants.MERGE_FIELDS);
        if (mergeFields == Undefined.instance) {
            return defaultMergeFields(ctx, namesNode, fields, additionalFields);
        }
        Object result = JSRuntime.call(mergeFields, calendar, new Object[]{fields, additionalFields});
        if (!JSRuntime.isObject(result)) {
            throw TemporalErrors.createTypeErrorObjectExpected();
        }
        return (DynamicObject) result;
    }

    @TruffleBoundary
    public static DynamicObject defaultMergeFields(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, DynamicObject fields, DynamicObject additionalFields) {
        JSRealm realm = JSRealm.get(null);
        DynamicObject merged = JSOrdinary.create(ctx, realm);
        UnmodifiableArrayList<? extends Object> originalKeys = namesNode.execute(fields);
        for (Object nextKey : originalKeys) {
            if (!MONTH.equals(nextKey) && !MONTH_CODE.equals(nextKey)) {
                Object propValue = JSObject.get(fields, nextKey);
                if (propValue != Undefined.instance) {
                    createDataPropertyOrThrow(ctx, merged, nextKey.toString(), propValue);
                }
            }
        }
        boolean hasMonthOrMonthCode = false;
        UnmodifiableArrayList<? extends Object> newKeys = namesNode.execute(additionalFields);
        for (Object nextKey : newKeys) {
            Object propValue = JSObject.get(additionalFields, nextKey);
            if (propValue != Undefined.instance) {
                createDataPropertyOrThrow(ctx, merged, nextKey.toString(), propValue);
                if (MONTH.equals(nextKey) || MONTH_CODE.equals(nextKey)) {
                    hasMonthOrMonthCode = true;
                }
            }
        }
        // TODO this is wrong. See PlainMonthYear.with({year:....});
        // this(=fields) has a month, but the additionalFields (=argument) does not
        // so we don't take the value from this (exception in for loop from above),
        // but we HAVE copied Undefined into additionalFields (so it is there, but empty).
        // if (!newKeys.contains(MONTH) && !newKeys.contains(MONTH_CODE)) {
        if (!hasMonthOrMonthCode) {
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
    public static List<String> listJoinRemoveDuplicates(List<String> first, List<String> second) {
        List<String> newList = new ArrayList<>(first.size() + second.size());
        newList.addAll(first);
        for (String elem : second) {
            if (!first.contains(elem)) {
                newList.add(elem);
            }
        }
        return newList;
    }

    public static DynamicObject toTemporalCalendarWithISODefault(JSContext ctx, JSRealm realm, Object calendar) {
        if (isNullish(calendar)) {
            return getISO8601Calendar(realm);
        } else {
            return toTemporalCalendar(ctx, calendar);
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

    /**
     * Note there also is {@link ToTemporalDateTimeNode}.
     */
    @TruffleBoundary
    public static JSTemporalPlainDateTimeObject toTemporalDateTime(JSContext ctx, JSRealm realm, Object item, DynamicObject optParam) {
        DynamicObject options = optParam;
        if (isNullish(optParam)) {
            options = JSOrdinary.createWithNullPrototype(ctx);
        }
        JSTemporalDateTimeRecord result = null;
        DynamicObject calendar = null;
        if (JSRuntime.isObject(item)) {
            DynamicObject itemObj = (DynamicObject) item;
            if (itemObj instanceof JSTemporalPlainDateTimeObject) {
                return (JSTemporalPlainDateTimeObject) itemObj;
            } else if (isTemporalZonedDateTime(itemObj)) {
                JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) itemObj;
                JSTemporalInstantObject instant = JSTemporalInstant.create(ctx, zdt.getNanoseconds());
                return TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, zdt.getTimeZone(), instant, zdt.getCalendar());
            } else if (itemObj instanceof JSTemporalPlainDateObject) {
                JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) itemObj;
                return JSTemporalPlainDateTime.create(ctx, date.getYear(), date.getMonth(), date.getDay(), 0, 0, 0, 0, 0, 0, date.getCalendar());
            }
            calendar = TemporalUtil.getTemporalCalendarWithISODefault(ctx, realm, itemObj);
            List<String> fieldNames = TemporalUtil.calendarFields(ctx, calendar, listDHMMMMMNSY);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(ctx, itemObj, fieldNames, listEmpty);
            result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, options);
        } else {
            TemporalUtil.toTemporalOverflow(options);
            String string = JSRuntime.toString(item);
            result = TemporalUtil.parseTemporalDateTimeString(string);
            assert isValidISODate(result.getYear(), result.getMonth(), result.getDay());
            assert isValidTime(result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
            calendar = TemporalUtil.toTemporalCalendarWithISODefault(ctx, realm, result.getCalendar());
        }
        return JSTemporalPlainDateTime.create(ctx,
                        result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                        result.getMicrosecond(), result.getNanosecond(),
                        calendar);
    }

    @TruffleBoundary
    public static JSTemporalDurationRecord differenceISODateTime(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, long y1, long mon1, long d1, long h1, long min1, long s1,
                    long ms1, long mus1, long ns1, long y2, long mon2, long d2, long h2, long min2, long s2, long ms2,
                    long mus2, long ns2, DynamicObject calendar, String largestUnit, DynamicObject optionsParam) {
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
            timeDifference = balanceDuration(ctx, namesNode, -timeSign, timeDifference.getHours(),
                            timeDifference.getMinutes(), timeDifference.getSeconds(), timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds(), largestUnit);
        }
        DynamicObject date1 = createTemporalDate(ctx, balanceResult.getYear(), balanceResult.getMonth(), balanceResult.getDay(), calendar);
        DynamicObject date2 = createTemporalDate(ctx, y2, mon2, d2, calendar);
        String dateLargestUnit = largerOfTwoTemporalUnits(DAY, largestUnit);
        DynamicObject untilOptions = mergeLargestUnitOption(ctx, namesNode, options, dateLargestUnit);
        JSTemporalDurationObject dateDifference = calendarDateUntil(calendar, date1, date2, untilOptions, Undefined.instance);
        JSTemporalDurationRecord result = balanceDuration(ctx, namesNode, dateDifference.getDays(), timeDifference.getHours(), timeDifference.getMinutes(), timeDifference.getSeconds(),
                        timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds(), largestUnit);
        return JSTemporalDurationRecord.createWeeks(dateDifference.getYears(), dateDifference.getMonths(), dateDifference.getWeeks(), result.getDays(), result.getHours(), result.getMinutes(),
                        result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds());
    }

    @TruffleBoundary
    public static DynamicObject mergeLargestUnitOption(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, DynamicObject options, String largestUnit) {
        JSRealm realm = JSRealm.get(null);
        DynamicObject merged = JSOrdinary.create(ctx, realm);
        UnmodifiableArrayList<?> keys = namesNode.execute(options);
        for (Object nextKey : keys) {
            String key = nextKey.toString();
            Object propValue = JSObject.get(options, key);
            createDataPropertyOrThrow(ctx, merged, key, propValue);
        }
        createDataPropertyOrThrow(ctx, merged, LARGEST_UNIT, largestUnit);
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
        if (months > 0) {
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
    @TruffleBoundary
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

    public static JSTemporalDurationRecord balanceDuration(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, long days, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds, String largestUnit) {
        return balanceDuration(ctx, namesNode, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, largestUnit, Undefined.instance);
    }

    public static JSTemporalDurationRecord balanceDuration(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, long days, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds, String largestUnit, DynamicObject relativeTo) {
        long ns;
        if (TemporalUtil.isTemporalZonedDateTime(relativeTo)) {
            JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) relativeTo;
            BigInt endNs = TemporalUtil.addZonedDateTime(ctx, zdt.getNanoseconds(), zdt.getTimeZone(), zdt.getCalendar(), 0, 0, 0, days, hours, minutes, seconds, milliseconds, microseconds,
                            nanoseconds);
            ns = bigIntToLong(endNs.subtract(zdt.getNanoseconds()));
        } else {
            ns = totalDurationNanoseconds(days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
        }
        long d;
        if (largestUnit.equals(YEAR) || largestUnit.equals(MONTH) || largestUnit.equals(WEEK) || largestUnit.equals(DAY)) {
            JSTemporalNanosecondsDaysRecord result = nanosecondsToDays(ctx, namesNode, BigInt.valueOf(ns), relativeTo);
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
    @TruffleBoundary
    public static JSTemporalDurationRecord unbalanceDurationRelative(JSContext ctx, JSRealm realm, long y, long m, long w,
                    long d, String largestUnit, DynamicObject relTo) {
        long years = y;
        long months = m;
        long weeks = w;
        long days = d;
        DynamicObject relativeTo = relTo;
        if (YEAR.equals(largestUnit) || (years == 0 && months == 0 && weeks == 0 && days == 0)) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        }
        long sign = durationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        assert sign != 0;
        DynamicObject oneYear = createTemporalDuration(ctx, sign, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        DynamicObject oneMonth = createTemporalDuration(ctx, 0, sign, 0, 0, 0, 0, 0, 0, 0, 0);
        DynamicObject oneWeek = createTemporalDuration(ctx, 0, 0, sign, 0, 0, 0, 0, 0, 0, 0);
        DynamicObject calendar = Undefined.instance;
        if (relativeTo != Undefined.instance) {
            relativeTo = toTemporalDate(ctx, realm, relativeTo, Undefined.instance);
            calendar = toDynamicObject(JSObject.get(relativeTo, CALENDAR));
        }
        if (MONTH.equals(largestUnit)) {
            if (calendar == Undefined.instance) {
                throw Errors.createRangeError("No calendar provided.");
            }
            Object dateAdd = JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            Object dateUntil = JSObject.getMethod(calendar, TemporalConstants.DATE_UNTIL);

            while (Math.abs(years) > 0) {
                DynamicObject addOptions = JSOrdinary.createWithNullPrototype(ctx);
                DynamicObject newRelativeTo = TemporalUtil.calendarDateAdd(calendar, relativeTo, oneYear, addOptions, dateAdd);

                DynamicObject untilOptions = JSOrdinary.createWithNullPrototype(ctx);
                JSObjectUtil.putDataProperty(ctx, untilOptions, "largestUnit", MONTH);
                JSTemporalDurationObject untilResult = TemporalUtil.calendarDateUntil(calendar, relativeTo, newRelativeTo, untilOptions, dateUntil);
                long oneYearMonths = untilResult.getMonths();
                relativeTo = newRelativeTo;
                years = years - sign;
                months = months + oneYearMonths;
            }
        } else if (WEEK.equals(largestUnit)) {
            if (calendar == Undefined.instance) {
                throw Errors.createRangeError("Calendar should not be undefined.");
            }
            while (Math.abs(years) > 0) {
                JSTemporalRelativeDateRecord moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneYear);
                relativeTo = moveResult.getRelativeTo();
                long oneYearDays = moveResult.getDays();
                years = years - sign;
                days = days + oneYearDays;
            }
            while (Math.abs(months) > 0) {
                JSTemporalRelativeDateRecord moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneMonth);
                relativeTo = moveResult.getRelativeTo();
                long oneMonthDays = moveResult.getDays();
                months = months - sign;
                days = days + oneMonthDays;
            }
        } else {
            if (years != 0 || months != 0 || weeks != 0) {
                if (calendar == Undefined.instance) {
                    throw Errors.createRangeError("Calendar should not be undefined.");
                }
                while (Math.abs(years) > 0) {
                    JSTemporalRelativeDateRecord moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneYear);
                    relativeTo = moveResult.getRelativeTo();
                    long oneYearDays = moveResult.getDays();
                    years = years - sign;
                    days = days + oneYearDays;
                }
                while (Math.abs(months) > 0) {
                    JSTemporalRelativeDateRecord moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneMonth);
                    relativeTo = moveResult.getRelativeTo();
                    long oneMonthDays = moveResult.getDays();
                    months = months - sign;
                    days = days + oneMonthDays;
                }
                while (Math.abs(weeks) > 0) {
                    JSTemporalRelativeDateRecord moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneWeek);
                    relativeTo = moveResult.getRelativeTo();
                    long oneWeekDays = moveResult.getDays();
                    weeks = weeks - sign;
                    days = days + oneWeekDays;
                }
            }
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    private static DynamicObject toDynamicObject(Object obj) {
        if (obj instanceof DynamicObject) {
            return (DynamicObject) obj;
        } else {
            throw Errors.createTypeErrorNotAnObject(obj);
        }
    }

    // 7.5.15
    @TruffleBoundary
    public static JSTemporalDurationRecord balanceDurationRelative(JSContext ctx, JSRealm realm, long y, long m, long w, long d, String largestUnit, DynamicObject relativeToParam) {
        long years = y;
        long months = m;
        long weeks = w;
        long days = d;
        if ((!largestUnit.equals(YEAR) && !largestUnit.equals(MONTH) && !largestUnit.equals(WEEK)) || (years == 0 && months == 0 && weeks == 0 && days == 0)) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        }
        long sign = durationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        assert sign != 0;
        DynamicObject oneYear = createTemporalDuration(ctx, sign, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        DynamicObject oneMonth = createTemporalDuration(ctx, 0, sign, 0, 0, 0, 0, 0, 0, 0, 0);
        DynamicObject oneWeek = createTemporalDuration(ctx, 0, 0, sign, 0, 0, 0, 0, 0, 0, 0);
        DynamicObject relativeTo = toTemporalDate(ctx, realm, relativeToParam, Undefined.instance);
        assert JSObject.hasProperty(relativeTo, CALENDAR);
        DynamicObject calendar = toDynamicObject(JSObject.get(relativeTo, CALENDAR));
        if (largestUnit.equals(YEAR)) {
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneYear);
            relativeTo = moveResult.getRelativeTo();
            long oneYearDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneYearDays)) {
                days = days - oneYearDays;
                years = years + sign;
                moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneYear);
                relativeTo = moveResult.getRelativeTo();
                oneYearDays = moveResult.getDays();
            }
            moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneMonth);
            relativeTo = moveResult.getRelativeTo();
            long oneMonthDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneMonthDays)) {
                days = days - oneMonthDays;
                months = months + sign;
                moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneMonth);
                relativeTo = moveResult.getRelativeTo();
                oneMonthDays = moveResult.getDays();
            }

            Object dateAdd = JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject options = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject newRelativeTo = TemporalUtil.calendarDateAdd(calendar, relativeTo, oneYear, options, dateAdd);

            Object dateUntil = JSObject.getMethod(calendar, TemporalConstants.DATE_UNTIL);
            options = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(ctx, options, TemporalConstants.LARGEST_UNIT, MONTH);
            JSTemporalDurationObject untilResult = TemporalUtil.calendarDateUntil(calendar, relativeTo, newRelativeTo, options, dateUntil);

            long oneYearMonths = untilResult.getMonths();
            while (Math.abs(months) >= Math.abs((oneYearMonths))) {
                months = months - oneYearMonths;
                years = years + sign;
                relativeTo = newRelativeTo;

                options = JSOrdinary.createWithNullPrototype(ctx);
                newRelativeTo = TemporalUtil.calendarDateAdd(calendar, relativeTo, oneYear, options, dateAdd);
                options = JSOrdinary.createWithNullPrototype(ctx);
                untilResult = TemporalUtil.calendarDateUntil(calendar, relativeTo, newRelativeTo, options, dateUntil);
                oneYearMonths = untilResult.getMonths();
            }
        } else if (largestUnit.equals(MONTH)) {
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneMonth);
            relativeTo = moveResult.getRelativeTo();
            long oneMonthDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneMonthDays)) {
                days = days - oneMonthDays;
                months = months + sign;
                moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneMonth);
                relativeTo = moveResult.getRelativeTo();
                oneMonthDays = moveResult.getDays();
            }
        } else {
            assert largestUnit.equals(WEEK);
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneWeek);
            relativeTo = moveResult.getRelativeTo();
            long oneWeekDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneWeekDays)) {
                days = days - oneWeekDays;
                weeks = weeks + sign;
                moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneWeek);
                relativeTo = moveResult.getRelativeTo();
                oneWeekDays = moveResult.getDays();
            }
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    // 7.5.16
    public static JSTemporalDurationRecord addDuration(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, long y1, long mon1, long w1, long d1, long h1, long min1, long s1, long ms1, long mus1,
                    long ns1, long y2, long mon2, long w2, long d2, long h2, long min2, long s2, long ms2, long mus2, long ns2, DynamicObject relativeTo) {
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
            JSTemporalDurationRecord result = balanceDuration(ctx, namesNode, d1 + d2, h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2, ns1 + ns2, largestUnit);
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
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) relativeTo;
            DynamicObject calendar = date.getCalendar();
            DynamicObject dateDuration1 = createTemporalDuration(ctx, y1, mon1, w1, d1, 0, 0, 0, 0, 0, 0);
            DynamicObject dateDuration2 = createTemporalDuration(ctx, y2, mon2, w2, d2, 0, 0, 0, 0, 0, 0);

            Object dateAdd = JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject firstAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject intermediate = TemporalUtil.calendarDateAdd(calendar, date, dateDuration1, firstAddOptions, dateAdd);

            DynamicObject secondAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject end = TemporalUtil.calendarDateAdd(calendar, intermediate, dateDuration2, secondAddOptions, dateAdd);

            String dateLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(DAY, largestUnit);

            DynamicObject differenceOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(ctx, differenceOptions, LARGEST_UNIT, dateLargestUnit);
            JSTemporalDurationObject dateDifference = TemporalUtil.calendarDateUntil(calendar, date, end, differenceOptions, Undefined.instance);
            JSTemporalDurationRecord result = balanceDuration(ctx, namesNode, dateDifference.getDays(),
                            h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2, ns1 + ns2, largestUnit);
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
            assert isTemporalZonedDateTime(relativeTo);
            JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) relativeTo;
            DynamicObject timeZone = zdt.getTimeZone();
            DynamicObject calendar = zdt.getCalendar();
            BigInt intermediateNs = addZonedDateTime(ctx, zdt.getNanoseconds(), timeZone, calendar, y1, mon1, w1, d1, h1, min1, s1, ms1, mus1, ns1);
            BigInt endNs = addZonedDateTime(ctx, intermediateNs, timeZone, calendar, y2, mon2, w2, d2, h2, min2, s2, ms2, mus2, ns2);
            if (!YEAR.equals(largestUnit) && !MONTH.equals(largestUnit) && !WEEK.equals(largestUnit) && !DAY.equals(largestUnit)) {
                long diffNs = differenceInstant(zdt.getNanoseconds(), endNs, 1d, NANOSECOND, HALF_EXPAND);
                JSTemporalDurationRecord result = balanceDuration(ctx, namesNode, 0, 0, 0, 0, 0, 0, diffNs, largestUnit);
                years = 0;
                months = 0;
                weeks = 0;
                days = 0;
                hours = result.getHours();
                minutes = result.getMinutes();
                seconds = result.getSeconds();
                milliseconds = result.getMilliseconds();
                microseconds = result.getMicroseconds();
                nanoseconds = result.getNanoseconds();
            } else {
                JSTemporalDurationRecord result = differenceZonedDateTime(ctx, namesNode, zdt.getNanoseconds(), endNs, timeZone, calendar, largestUnit);
                years = result.getYears();
                months = result.getMonths();
                weeks = result.getWeeks();
                days = result.getDays();
                hours = result.getHours();
                minutes = result.getMinutes();
                seconds = result.getSeconds();
                milliseconds = result.getMilliseconds();
                microseconds = result.getMicroseconds();
                nanoseconds = result.getNanoseconds();
            }
        }
        if (!validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
            throw Errors.createRangeError("Duration out of range");
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    public static JSTemporalDurationRecord differenceZonedDateTime(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, BigInt ns1, BigInt ns2, DynamicObject timeZone, DynamicObject calendar,
                    String largestUnit) {
        return differenceZonedDateTime(ctx, namesNode, ns1, ns2, timeZone, calendar, largestUnit, Undefined.instance);
    }

    public static JSTemporalDurationRecord differenceZonedDateTime(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, BigInt ns1, BigInt ns2, DynamicObject timeZone, DynamicObject calendar,
                    String largestUnit, DynamicObject options) {
        if (ns1.equals(ns2)) {
            return JSTemporalDurationRecord.createWeeks(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        JSTemporalInstantObject startInstant = JSTemporalInstant.create(ctx, ns1);
        JSTemporalPlainDateTimeObject startDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, timeZone, startInstant, calendar);
        JSTemporalInstantObject endInstant = JSTemporalInstant.create(ctx, ns2);
        JSTemporalPlainDateTimeObject endDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, timeZone, endInstant, calendar);
        JSTemporalDurationRecord dateDifference = differenceISODateTime(ctx, namesNode, startDateTime.getYear(), startDateTime.getMonth(), startDateTime.getDay(), startDateTime.getHour(),
                        startDateTime.getMinute(), startDateTime.getSecond(), startDateTime.getMillisecond(), startDateTime.getMicrosecond(), startDateTime.getNanosecond(),
                        endDateTime.getYear(), endDateTime.getMonth(), endDateTime.getDay(), endDateTime.getHour(), endDateTime.getMinute(), endDateTime.getSecond(),
                        endDateTime.getMillisecond(), endDateTime.getMicrosecond(), endDateTime.getNanosecond(), calendar, largestUnit, options);
        BigInt intermediateNs = addZonedDateTime(ctx, ns1, timeZone, calendar, dateDifference.getYears(), dateDifference.getMonths(), dateDifference.getWeeks(), 0, 0, 0, 0, 0, 0, 0);
        BigInt timeRemainderNs = ns2.subtract(intermediateNs);
        DynamicObject intermediate = createTemporalZonedDateTime(ctx, intermediateNs, timeZone, calendar);
        JSTemporalNanosecondsDaysRecord result = nanosecondsToDays(ctx, namesNode, timeRemainderNs, intermediate);
        JSTemporalDurationRecord timeDifference = balanceDuration(ctx, namesNode, 0, 0, 0, 0, 0, 0, result.getNanoseconds(), HOUR);
        return JSTemporalDurationRecord.createWeeks(dateDifference.getYears(), dateDifference.getMonths(), dateDifference.getWeeks(), result.getDays(), timeDifference.getHours(),
                        timeDifference.getMinutes(), timeDifference.getSeconds(), timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds());
    }

    // 7.5.5
    public static boolean validateTemporalDuration(long years, long months, long weeks, long days, long hours,
                    long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
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
    public static String defaultTemporalLargestUnit(long years, long months, long weeks, long days, long hours, long minutes, long seconds, long milliseconds, long microseconds) {
        if (years != 0) {
            return YEAR;
        }
        if (months != 0) {
            return MONTH;
        }
        if (weeks != 0) {
            return WEEK;
        }
        if (days != 0) {
            return DAY;
        }
        if (hours != 0) {
            return HOUR;
        }
        if (minutes != 0) {
            return MINUTE;
        }
        if (seconds != 0) {
            return SECOND;
        }
        if (milliseconds != 0) {
            return MILLISECOND;
        }
        if (microseconds != 0) {
            return MICROSECOND;
        }
        return NANOSECOND;
    }

    // 7.5.7
    public static DynamicObject toPartialDuration(DynamicObject temporalDurationLike, JSContext ctx, IsObjectNode isObjectNode, JSToIntegerWithoutRoundingNode toInt) {
        if (!isObjectNode.executeBoolean(temporalDurationLike)) {
            throw Errors.createTypeError("Given duration like is not a object.");
        }
        JSRealm realm = JSRealm.get(null);
        DynamicObject result = JSOrdinary.create(ctx, realm);
        boolean any = false;
        for (String property : DURATION_PROPERTIES) {
            Object value = JSObject.get(temporalDurationLike, property);
            if (value != Undefined.instance) {
                any = true;
                JSObjectUtil.putDataProperty(ctx, result, property, toInt.execute(value));
            }
        }
        if (!any) {
            throw Errors.createTypeError("Given duration like object has no duration properties.");
        }
        return result;
    }

    // 7.5.15
    public static JSTemporalRelativeDateRecord moveRelativeDate(JSContext ctx, DynamicObject calendar, DynamicObject relativeTo, DynamicObject duration) {
        DynamicObject options = JSOrdinary.createWithNullPrototype(ctx);
        JSTemporalPlainDateObject newDate = TemporalUtil.calendarDateAdd(calendar, relativeTo, duration, options, Undefined.instance);
        long days = daysUntil(relativeTo, newDate);
        return JSTemporalRelativeDateRecord.create(newDate, days);
    }

    // 7.5.20
    public static JSTemporalDurationRecord roundDuration(JSContext ctx, JSRealm realm, EnumerableOwnPropertyNamesNode namesNode, long y, long m, long w, long d, long h, long min, long sec,
                    long milsec, long micsec,
                    long nsec, long increment, String unit, String roundingMode, DynamicObject relTo) {
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
        BigDecimal fractionalSeconds = BigDecimal.ZERO;

        if (relativeTo != Undefined.instance) {
            if (TemporalUtil.isTemporalZonedDateTime(relativeTo)) {
                zonedRelativeTo = relativeTo;
                relativeTo = toTemporalDate(ctx, realm, relativeTo, Undefined.instance);
            } else {
                TemporalUtil.requireTemporalDate(relativeTo);
            }
            calendar = ((JSTemporalPlainDateObject) relativeTo).getCalendar();
        }
        if (unit.equals(YEAR) || unit.equals(MONTH) || unit.equals(WEEK) || unit.equals(DAY)) {
            nanoseconds = totalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
            DynamicObject intermediate = Undefined.instance;
            if (zonedRelativeTo != Undefined.instance) {
                intermediate = moveRelativeZonedDateTime(ctx, zonedRelativeTo, years, months, weeks, days);
            }
            JSTemporalNanosecondsDaysRecord result = nanosecondsToDays(ctx, namesNode, BigInt.valueOf(nanoseconds), intermediate);
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
            fractionalSeconds = roundDurationCalculateFractionalSeconds(seconds, microseconds, milliseconds, nanoseconds);
        }
        double remainder = 0;
        if (unit.equals(YEAR)) {
            DynamicObject yearsDuration = createTemporalDuration(ctx, years, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            Object dateAdd = JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject firstAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsDuration, firstAddOptions, dateAdd);
            DynamicObject yearsMonthsWeeks = createTemporalDuration(ctx, years, months, weeks, 0, 0, 0, 0, 0, 0, 0);

            DynamicObject secondAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsMonthsWeeksLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsMonthsWeeks, secondAddOptions, dateAdd);
            long monthsWeeksInDays = daysUntil(yearsLater, yearsMonthsWeeksLater);
            relativeTo = yearsLater;
            days = days + monthsWeeksInDays;
            DynamicObject daysDuration = createTemporalDuration(ctx, 0, 0, 0, days, 0, 0, 0, 0, 0, 0);
            DynamicObject thirdAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject daysLater = calendarDateAdd(calendar, relativeTo, daysDuration, thirdAddOptions, dateAdd);
            DynamicObject untilOptions = JSOrdinary.createWithNullPrototype(ctx);
            createDataPropertyOrThrow(ctx, untilOptions, LARGEST_UNIT, YEAR);
            JSTemporalDurationObject timePassed = calendarDateUntil(calendar, relativeTo, daysLater, untilOptions);
            long yearsPassed = timePassed.getYears();
            years = years + yearsPassed;
            DynamicObject oldRelativeTo = relativeTo;

            yearsDuration = createTemporalDuration(ctx, yearsPassed, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            DynamicObject fourthAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            relativeTo = calendarDateAdd(calendar, relativeTo, yearsDuration, fourthAddOptions, dateAdd);
            long daysPassed = daysUntil(oldRelativeTo, relativeTo);
            days = days - daysPassed;

            long sign = TemporalUtil.sign(days);
            if (sign == 0) {
                sign = 1;
            }
            DynamicObject oneYear = createTemporalDuration(ctx, sign, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneYear);

            long oneYearDays = moveResult.getDays();
            double fractionalYears = years + ((double) days / Math.abs(oneYearDays));
            years = TemporalUtil.roundNumberToIncrement(fractionalYears, increment, roundingMode);
            remainder = fractionalYears - years;
            months = 0;
            weeks = 0;
            days = 0;
        } else if (unit.equals(MONTH)) {
            DynamicObject yearsMonths = createTemporalDuration(ctx, years, months, 0, 0, 0, 0, 0, 0, 0, 0);
            Object dateAdd = JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject firstAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsMonthsLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsMonths, firstAddOptions, dateAdd);
            DynamicObject yearsMonthsWeeks = createTemporalDuration(ctx, years, months, weeks, 0, 0, 0, 0, 0, 0, 0);
            DynamicObject secondAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsMonthsWeeksLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsMonthsWeeks, secondAddOptions, dateAdd);
            long weeksInDays = daysUntil(yearsMonthsLater, yearsMonthsWeeksLater);
            relativeTo = yearsMonthsLater;
            days = days + weeksInDays;
            long sign = TemporalUtil.sign(days);
            if (sign == 0) {
                sign = 1;
            }
            DynamicObject oneMonth = createTemporalDuration(ctx, 0, sign, 0, 0, 0, 0, 0, 0, 0, 0);
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneMonth);
            relativeTo = moveResult.getRelativeTo();
            long oneMonthDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneMonthDays)) {
                months = months + sign;
                days = days - oneMonthDays;
                moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneMonth);
                relativeTo = moveResult.getRelativeTo();
                oneMonthDays = moveResult.getDays();
            }
            double fractionalMonths = months + ((double) days / Math.abs(oneMonthDays));
            months = TemporalUtil.roundNumberToIncrement(fractionalMonths, increment, roundingMode);
            remainder = fractionalMonths - months;
            weeks = 0;
            days = 0;
        } else if (unit.equals(WEEK)) {
            long sign = TemporalUtil.sign(days);
            if (sign == 0) {
                sign = 1;
            }
            DynamicObject oneWeek = createTemporalDuration(ctx, 0, 0, sign, 0, 0, 0, 0, 0, 0, 0);
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneWeek);
            relativeTo = moveResult.getRelativeTo();
            long oneWeekDays = moveResult.getDays();
            while (Math.abs(days) >= Math.abs(oneWeekDays)) {
                weeks = weeks - sign;
                days = days - oneWeekDays;
                moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneWeek);
                relativeTo = moveResult.getRelativeTo();
                oneWeekDays = moveResult.getDays();
            }
            double fractionalWeeks = weeks + ((double) days / Math.abs(oneWeekDays));
            weeks = TemporalUtil.roundNumberToIncrement(fractionalWeeks, increment, roundingMode);
            remainder = fractionalWeeks - weeks;
            days = 0;
        } else if (unit.equals(DAY)) {
            double fractionalDays = days;
            days = TemporalUtil.roundNumberToIncrement(fractionalDays, increment, roundingMode);
            remainder = fractionalDays - days;
        } else if (unit.equals(HOUR)) {
            double secondsPart = roundDurationFractionalDecondsDiv60(fractionalSeconds);
            double fractionalHours = ((secondsPart + minutes) / 60.0) + hours;
            hours = TemporalUtil.roundNumberToIncrement(fractionalHours, increment, roundingMode);
            remainder = fractionalHours - hours;
            minutes = 0;
            seconds = 0;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit.equals(MINUTE)) {
            double secondsPart = roundDurationFractionalDecondsDiv60(fractionalSeconds);
            double fractionalMinutes = secondsPart + minutes;
            minutes = TemporalUtil.roundNumberToIncrement(fractionalMinutes, increment, roundingMode);
            remainder = fractionalMinutes - minutes;
            seconds = 0;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit.equals(SECOND)) {
            seconds = TemporalUtil.roundNumberToIncrement(fractionalSeconds, Boundaries.bigDecimalValueOf(increment), roundingMode);
            remainder = roundDurationFractionalSecondsSubtract(seconds, fractionalSeconds);
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit.equals(MILLISECOND)) {
            double fractionalMilliseconds = (nanoseconds * 0.000_001) + (microseconds * 0.001) + milliseconds;
            milliseconds = TemporalUtil.roundNumberToIncrement(fractionalMilliseconds, increment, roundingMode);
            remainder = fractionalMilliseconds - milliseconds;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit.equals(MICROSECOND)) {
            double fractionalMicroseconds = (nanoseconds * 0.001) + microseconds;
            microseconds = TemporalUtil.roundNumberToIncrement(fractionalMicroseconds, increment, roundingMode);
            remainder = fractionalMicroseconds - microseconds;
            nanoseconds = 0;
        } else {
            assert unit.equals(NANOSECOND);
            remainder = nanoseconds;
            nanoseconds = TemporalUtil.roundNumberToIncrement(nanoseconds, increment, roundingMode);
            remainder = remainder - nanoseconds;
        }

        return JSTemporalDurationRecord.createWeeksRemainder(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, remainder);
    }

    @TruffleBoundary
    private static double roundDurationFractionalSecondsSubtract(long seconds, BigDecimal fractionalSeconds) {
        return fractionalSeconds.subtract(BigDecimal.valueOf(seconds)).doubleValue();
    }

    @TruffleBoundary
    private static double roundDurationFractionalDecondsDiv60(BigDecimal fractionalSeconds) {
        return fractionalSeconds.divide(BigDecimal.valueOf(60), mc_20_floor).doubleValue();
    }

    @TruffleBoundary
    private static BigDecimal roundDurationCalculateFractionalSeconds(long seconds, long microseconds, long milliseconds, long nanoseconds) {
        BigDecimal part1 = BigDecimal.valueOf(nanoseconds).multiply(new BigDecimal("0.000000001"));
        BigDecimal part2 = BigDecimal.valueOf(microseconds).multiply(new BigDecimal("0.000001"));
        BigDecimal part3 = BigDecimal.valueOf(milliseconds).multiply(new BigDecimal("0.001"));
        return part1.add(part2).add(part3).add(BigDecimal.valueOf(seconds));
    }

    private static JSTemporalNanosecondsDaysRecord nanosecondsToDays(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, BigInt nanosecondsParam, DynamicObject relativeTo) {
        BigInt nanoseconds = nanosecondsParam;
        long sign = nanoseconds.signum();
        BigInt signBI = BigInt.valueOf(sign);
        BigInt dayLengthNs = BigInt.valueOf(86_400_000_000_000L);
        if (sign == 0) {
            return JSTemporalNanosecondsDaysRecord.create(0, 0, dayLengthNs.longValue());
        }
        if (!TemporalUtil.isTemporalZonedDateTime(relativeTo)) {
            BigInt val = nanoseconds.divide(dayLengthNs);
            BigInt val2 = nanoseconds.abs().mod(dayLengthNs).multiply(signBI);
            return JSTemporalNanosecondsDaysRecord.create(val.longValue(), val2.longValue(), dayLengthNs.longValue());
        }
        JSTemporalZonedDateTimeObject relativeZDT = (JSTemporalZonedDateTimeObject) relativeTo;
        BigInt startNs = relativeZDT.getNanoseconds();
        JSTemporalInstantObject startInstant = JSTemporalInstant.create(ctx, startNs);
        JSTemporalPlainDateTimeObject startDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, relativeZDT.getTimeZone(), startInstant, relativeZDT.getCalendar());
        BigInt endNs = startNs.add(nanoseconds);
        JSTemporalInstantObject endInstant = JSTemporalInstant.create(ctx, endNs);
        JSTemporalPlainDateTimeObject endDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, relativeZDT.getTimeZone(), endInstant,
                        relativeZDT.getCalendar());
        JSTemporalDurationRecord dateDifference = TemporalUtil.differenceISODateTime(ctx, namesNode, startDateTime.getYear(), startDateTime.getMonth(),
                        startDateTime.getDay(), startDateTime.getHour(), startDateTime.getMinute(), startDateTime.getSecond(), startDateTime.getMillisecond(),
                        startDateTime.getMicrosecond(), startDateTime.getNanosecond(), endDateTime.getYear(), endDateTime.getMonth(), endDateTime.getDay(), endDateTime.getHour(),
                        endDateTime.getMinute(), endDateTime.getSecond(), endDateTime.getMillisecond(), endDateTime.getMicrosecond(), endDateTime.getNanosecond(), relativeZDT.getCalendar(), DAY,
                        Undefined.instance);
        long days = dateDifference.getDays();
        BigInt intermediateNs = TemporalUtil.addZonedDateTime(ctx, startNs, relativeZDT.getTimeZone(), relativeZDT.getCalendar(), 0, 0, 0, days, 0, 0, 0, 0, 0, 0);
        if (sign == 1) {
            while (days > 0 && intermediateNs.compareTo(endNs) == 1) {
                days = days - 1;
                intermediateNs = TemporalUtil.addZonedDateTime(ctx, startNs, relativeZDT.getTimeZone(), relativeZDT.getCalendar(), 0, 0, 0, days, 0, 0, 0, 0, 0, 0);
            }
        }
        nanoseconds = endNs.subtract(intermediateNs);
        boolean done = false;
        while (!done) {
            BigInt oneDayFartherNs = TemporalUtil.addZonedDateTime(ctx, intermediateNs, relativeZDT.getTimeZone(), relativeZDT.getCalendar(), 0, 0, 0, sign, 0, 0, 0, 0, 0, 0);
            dayLengthNs = oneDayFartherNs.subtract(intermediateNs);
            if (nanoseconds.subtract(dayLengthNs).multiply(signBI).compareTo(BigInt.valueOf(0)) != -1) {
                nanoseconds = nanoseconds.subtract(dayLengthNs);
                intermediateNs = oneDayFartherNs;
                days = days + sign;
            } else {
                done = true;
            }
        }
        return JSTemporalNanosecondsDaysRecord.create(days, nanoseconds.longValue(), Math.abs(dayLengthNs.longValue()));
    }

    // 7.5.21
    public static JSTemporalDurationRecord adjustRoundedDurationDays(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, long years, long months, long weeks, long days, long hours,
                    long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds, long increment, String unit, String roundingMode, DynamicObject relativeToParam) {
        if (!(TemporalUtil.isTemporalZonedDateTime(relativeToParam)) || unit.equals(YEAR) || unit.equals(MONTH) || unit.equals(WEEK) || unit.equals(DAY) ||
                        (unit.equals(NANOSECOND) && increment == 1)) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        JSTemporalZonedDateTimeObject relativeTo = (JSTemporalZonedDateTimeObject) relativeToParam;
        long timeRemainderNs = totalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
        long direction = TemporalUtil.sign(timeRemainderNs);
        BigInt dayStart = TemporalUtil.addZonedDateTime(ctx,
                        relativeTo.getNanoseconds(), relativeTo.getTimeZone(), relativeTo.getCalendar(),
                        years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        BigInt dayEnd = TemporalUtil.addZonedDateTime(ctx, dayStart, relativeTo.getTimeZone(), relativeTo.getCalendar(), 0, 0, 0, direction, 0, 0, 0, 0, 0, 0);
        long dayLengthNs = bigIntToLong(dayEnd.subtract(dayStart));
        if (((timeRemainderNs - dayLengthNs) * direction) < 0) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        timeRemainderNs = TemporalUtil.roundTemporalInstant(Boundaries.bigDecimalValueOf(timeRemainderNs - dayLengthNs), increment, unit, roundingMode);
        JSTemporalDurationRecord add = addDuration(ctx, namesNode, years, months, weeks, days, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, direction, 0, 0, 0, 0, 0, 0, relativeToParam);
        JSTemporalDurationRecord atd = balanceDuration(ctx, namesNode, 0, 0, 0, 0, 0, 0, timeRemainderNs, HOUR, Undefined.instance);

        return JSTemporalDurationRecord.createWeeks(add.getYears(), add.getMonths(), add.getWeeks(), add.getDays(),
                        atd.getHours(), atd.getMinutes(), atd.getSeconds(), atd.getMilliseconds(), atd.getMicroseconds(), atd.getNanoseconds());
    }

    // TODO this should not be necessary.
    // working assumption is that it is ok to use it in the places where we DO use it
    @TruffleBoundary
    public static long bigIntToLong(BigInt val) {
        return val.longValueExact();
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
    @TruffleBoundary
    public static long calculateOffsetShift(JSContext ctx, DynamicObject relativeTo, long y, long mon, long w, long d, long h, long min,
                    long s, long ms, long mus, long ns) {
        if (!(isTemporalZonedDateTime(relativeTo))) {
            return 0;
        }
        JSTemporalZonedDateTimeObject relativeToZDT = (JSTemporalZonedDateTimeObject) relativeTo;
        DynamicObject instant = JSTemporalInstant.create(ctx, relativeToZDT.getNanoseconds());
        long offsetBefore = getOffsetNanosecondsFor(relativeToZDT.getTimeZone(), instant);
        BigInt after = addZonedDateTime(ctx, relativeToZDT.getNanoseconds(), relativeToZDT.getTimeZone(), relativeToZDT.getCalendar(), y, mon, w, d, h, min, s, ms, mus, ns);
        DynamicObject instantAfter = JSTemporalInstant.create(ctx, after);
        long offsetAfter = getOffsetNanosecondsFor(relativeToZDT.getTimeZone(), instantAfter);
        return offsetAfter - offsetBefore;
    }

    // 7.5.17
    public static long daysUntil(DynamicObject earlier, DynamicObject later) {
        assert isTemporalDate(earlier) && isTemporalDate(later);
        JSTemporalDurationRecord difference = JSTemporalPlainDate.differenceISODate(
                        ((TemporalYear) earlier).getYear(), ((TemporalMonth) earlier).getMonth(), ((TemporalDay) earlier).getDay(),
                        ((TemporalYear) later).getYear(), ((TemporalMonth) later).getMonth(), ((TemporalDay) later).getDay(), DAY);
        return difference.getDays();
    }

    private static boolean isTemporalDate(DynamicObject d) {
        return d instanceof TemporalYear && d instanceof TemporalMonth && d instanceof TemporalDay;
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
        JSTemporalDurationRecord bt = balanceTime(hours * sign, minutes * sign, seconds * sign, milliseconds * sign, microseconds * sign, nanoseconds * sign);
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
        long result = TemporalUtil.roundNumberToIncrement(quantity, increment, roundingMode);
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

    // 7.5.22
    @TruffleBoundary
    public static JSTemporalDurationRecord toLimitedTemporalDuration(Object temporalDurationLike,
                    List<String> disallowedFields, IsObjectNode isObject, JSToStringNode toStringNode) {
        JSTemporalDurationRecord d;
        if (!isObject.executeBoolean(temporalDurationLike)) {
            String str = toStringNode.executeString(temporalDurationLike);
            d = JSTemporalDuration.parseTemporalDurationString(str);
        } else {
            d = JSTemporalDuration.toTemporalDurationRecord((DynamicObject) temporalDurationLike);
        }
        if (!TemporalUtil.validateTemporalDuration(d.getYears(), d.getMonths(), d.getWeeks(), d.getDays(), d.getHours(), d.getMinutes(), d.getSeconds(), d.getMilliseconds(),
                        d.getMicroseconds(), d.getNanoseconds())) {
            throw Errors.createRangeError("Given duration outside range.");
        }

        for (String property : TemporalUtil.DURATION_PROPERTIES) {
            long value = TemporalUtil.getPropertyFromRecord(d, property);
            if (value > 0 && disallowedFields.contains(property)) {
                throw TemporalErrors.createRangeErrorDisallowedField(property);
            }
        }
        return d;
    }

    public static JSTemporalPlainDateTimeObject systemDateTime(Object temporalTimeZoneLike, Object calendarLike, JSContext ctx) {
        DynamicObject timeZone = null;
        if (temporalTimeZoneLike == Undefined.instance) {
            timeZone = systemTimeZone(ctx);
        } else {
            timeZone = toTemporalTimeZone(ctx, temporalTimeZoneLike);
        }
        DynamicObject calendar = toTemporalCalendar(ctx, calendarLike);
        DynamicObject instant = systemInstant(ctx);
        return builtinTimeZoneGetPlainDateTimeFor(ctx, timeZone, instant, calendar);
    }

    public static JSTemporalPlainDateTimeObject builtinTimeZoneGetPlainDateTimeFor(JSContext ctx, DynamicObject timeZone, DynamicObject instant, DynamicObject calendar) {
        long offsetNanoseconds = getOffsetNanosecondsFor(timeZone, instant);
        JSTemporalDateTimeRecord result = getISOPartsFromEpoch(((JSTemporalInstantObject) instant).getNanoseconds());
        JSTemporalDateTimeRecord result2 = balanceISODateTime(result.getYear(), result.getMonth(),
                        result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                        result.getMicrosecond(), result.getNanosecond() + offsetNanoseconds);
        return createTemporalDateTime(ctx, result2.getYear(), result2.getMonth(), result2.getDay(), result2.getHour(), result2.getMinute(), result2.getSecond(),
                        result2.getMillisecond(), result2.getMicrosecond(), result2.getNanosecond(), calendar);
    }

    public static JSTemporalDateTimeRecord balanceISODateTime(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond) {
        JSTemporalDurationRecord bt = balanceTime(hour, minute, second, millisecond, microsecond, nanosecond);
        JSTemporalDateTimeRecord bd = balanceISODate(year, month, day + bt.getDays());
        return JSTemporalDateTimeRecord.create(bd.getYear(), bd.getMonth(), bd.getDay(), bt.getHours(), bt.getMinutes(), bt.getSeconds(),
                        bt.getMilliseconds(), bt.getMicroseconds(), bt.getNanoseconds());
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord getISOPartsFromEpoch(BigInt epochNanoseconds) {
        long remainderNs = epochNanoseconds.longValue() % 1_000_000;
        long epochMilliseconds = (epochNanoseconds.longValue() - remainderNs) / 1_000_000;
        long year = JSDate.yearFromTime(epochMilliseconds);
        long month = JSDate.monthFromTime(epochMilliseconds) + 1;
        long day = JSDate.dateFromTime(epochMilliseconds);
        long hour = JSDate.hourFromTime(epochMilliseconds);
        long minute = JSDate.minFromTime(epochMilliseconds);
        long second = JSDate.secFromTime(epochMilliseconds);
        long millisecond = JSDate.msFromTime(epochMilliseconds);
        long microsecond = (remainderNs / 1000) % 1000;
        long nanosecond = remainderNs % 1000;
        return JSTemporalDateTimeRecord.create(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
    }

    @TruffleBoundary
    public static long getOffsetNanosecondsFor(DynamicObject timeZone, DynamicObject instant) {
        Object getOffsetNanosecondsFor = JSObject.getMethod(timeZone, "getOffsetNanosecondsFor");
        Object offsetNanoseconds = JSRuntime.call(getOffsetNanosecondsFor, timeZone, new Object[]{instant});
        if (!JSRuntime.isNumber(offsetNanoseconds)) {
            throw Errors.createTypeError("Number expected");
        }
        Double nanos = ((Number) offsetNanoseconds).doubleValue();
        if (!JSRuntime.isInteger(nanos) || Math.abs(nanos) > 86400.0 * 1_000_000_000d) {
            throw Errors.createRangeError("out-of-range Number");
        }
        return nanos.longValue();
    }

    public static DynamicObject systemZonedDateTime(Object temporalTimeZoneLike, Object calendarLike, JSContext ctx) {
        DynamicObject timeZone = null;
        if (temporalTimeZoneLike == Undefined.instance) {
            timeZone = systemTimeZone(ctx);
        } else {
            timeZone = toTemporalTimeZone(ctx, temporalTimeZoneLike);
        }
        DynamicObject calendar = toTemporalCalendar(ctx, calendarLike);
        BigInt ns = systemUTCEpochNanoseconds();
        return createTemporalZonedDateTime(ctx, ns, timeZone, calendar);
    }

    public static DynamicObject systemInstant(JSContext ctx) {
        BigInt ns = systemUTCEpochNanoseconds();
        return JSTemporalInstant.create(ctx, ns);
    }

    @TruffleBoundary
    public static BigInt systemUTCEpochNanoseconds() {
        JSRealm realm = JSRealm.get(null);
        BigInt ns = BigInt.valueOf(realm.nanoTimeWallClock());
        // clamping omitted (see Note 2 in spec)
        assert ns.compareTo(upperEpochNSLimit) <= 0 && ns.compareTo(lowerEpochNSLimit) >= 0;
        return ns;
    }

    public static DynamicObject systemTimeZone(JSContext ctx) {
        String identifier = defaultTimeZone();
        return createTemporalTimeZone(ctx, identifier);
    }

    public static String defaultTimeZone() {
        return UTC;
    }

    public static boolean isTemporalInstant(Object obj) {
        return JSTemporalInstant.isJSTemporalInstant(obj);
    }

    public static DynamicObject toTemporalInstant(JSContext ctx, Object item) {
        if (JSRuntime.isObject(item)) {
            if (isTemporalInstant(item)) {
                return (DynamicObject) item;
            }
            if (isTemporalZonedDateTime(item)) {
                return JSTemporalInstant.create(ctx, ((JSTemporalZonedDateTimeObject) item).getNanoseconds());
            }
        }
        String string = JSRuntime.toString(item);
        BigInt epochNanoseconds = parseTemporalInstant(string);
        return JSTemporalInstant.create(ctx, epochNanoseconds);
    }

    public static int compareEpochNanoseconds(BigInt one, BigInt two) {
        return one.compareTo(two);
    }

    @TruffleBoundary
    public static boolean isValidEpochNanoseconds(BigInt nanoseconds) {
        if (nanoseconds == null) {
            return true; // suspicious, but relevant
        }
        if (nanoseconds.compareTo(lowerEpochNSLimit) == -1 || nanoseconds.compareTo(upperEpochNSLimit) == 1) {
            return false;
        }
        return true;
    }

    @TruffleBoundary
    public static BigInt addInstant(BigInt epochNanoseconds, long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        BigInteger res = epochNanoseconds.bigIntegerValue().add(BigInteger.valueOf(nanoseconds));
        res = res.add(BigInteger.valueOf(microseconds).multiply(BigInteger.valueOf(1000)));
        res = res.add(BigInteger.valueOf(milliseconds).multiply(BigInteger.valueOf(1_000_000)));
        res = res.add(BigInteger.valueOf(seconds).multiply(BigInteger.valueOf(1_000_000_000L)));
        res = res.add(BigInteger.valueOf(minutes).multiply(BigInteger.valueOf(60_000_000_000L)));
        res = res.add(BigInteger.valueOf(hours).multiply(BigInteger.valueOf(3_600_000_000_000L)));
        BigInt result = new BigInt(res);
        if (!isValidEpochNanoseconds(result)) {
            throw TemporalErrors.createRangeErrorInvalidNanoseconds();
        }
        return result;
    }

    @TruffleBoundary
    public static long differenceInstant(BigInt ns1, BigInt ns2, double roundingIncrement, String smallestUnit, String roundingMode) {
        return roundTemporalInstant(ns2.subtract(ns1), (long) roundingIncrement, smallestUnit, roundingMode);
    }

    @TruffleBoundary
    public static String temporalInstantToString(JSContext ctx, JSRealm realm, DynamicObject instant, DynamicObject timeZone, Object precision) {
        DynamicObject outputTimeZone = timeZone;
        if (outputTimeZone == Undefined.instance) {
            outputTimeZone = createTemporalTimeZone(ctx, UTC);
        }
        DynamicObject isoCalendar = getISO8601Calendar(realm);
        JSTemporalPlainDateTimeObject dateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, outputTimeZone, instant, isoCalendar);
        String dateTimeString = JSTemporalPlainDateTime.temporalDateTimeToString(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(),
                        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(), Undefined.instance,
                        precision, NEVER);
        String timeZoneString = null;
        if (timeZone == Undefined.instance) {
            timeZoneString = "Z";
        } else {
            long offsetNs = getOffsetNanosecondsFor(timeZone, instant);
            timeZoneString = formatISOTimeZoneOffsetString(offsetNs);
        }
        return dateTimeString + timeZoneString;
    }

    public static String builtinTimeZoneGetOffsetStringFor(DynamicObject timeZone, DynamicObject instant) {
        long offsetNanoseconds = getOffsetNanosecondsFor(timeZone, instant);
        return formatTimeZoneOffsetString(offsetNanoseconds);
    }

    @TruffleBoundary
    public static String formatTimeZoneOffsetString(long offsetNanosecondsParam) {
        String sign = offsetNanosecondsParam >= 0 ? "+" : "-";
        long offsetNanoseconds = Math.abs(offsetNanosecondsParam);
        long nanoseconds = offsetNanoseconds % 1_000_000_000L;
        double s1 = (Math.floor(offsetNanoseconds / 1_000_000_000.0) % 60.0);
        double m1 = (Math.floor(offsetNanoseconds / 60_000_000_000.0) % 60.0);
        double h1 = Math.floor(offsetNanoseconds / 3_600_000_000_000.0);

        long seconds = (long) s1;
        long minutes = (long) m1;
        long hours = (long) h1;

        String h = String.format("%1$02d", hours);
        String m = String.format("%1$02d", minutes);
        String s = String.format("%1$02d", seconds);

        String post = "";
        if (nanoseconds != 0) {
            String fraction = String.format("%1$09d", nanoseconds);
            while (fraction.endsWith("0")) {
                fraction = fraction.substring(0, fraction.length() - 1);
            }
            post = ":" + s + "." + fraction;
        } else if (seconds != 0) {
            post = ":" + s;
        }
        return sign + h + ":" + m + post;
    }

    @TruffleBoundary
    public static long parseTimeZoneOffsetString(String string) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseTimeZoneNumericUTCOffset();
        if (rec == null) {
            throw Errors.createRangeError("TemporalTimeZoneNumericUTCOffset expected");
        }

        long nanoseconds;
        if (rec.getOffsetFraction() == null) {
            nanoseconds = 0;
        } else {
            String fraction = rec.getOffsetFraction() + "000000000";
            fraction = fraction.substring(0, 9);
            nanoseconds = Long.parseLong(fraction, 10);
        }

        String signS = rec.getOffsetSign();
        int sign = ("-".equals(signS) || "\u2212".equals(signS)) ? -1 : 1;

        long hours = rec.getOffsetHour() == Long.MIN_VALUE ? 0 : rec.getOffsetHour();
        long minutes = rec.getOffsetMinute() == Long.MIN_VALUE ? 0 : rec.getOffsetMinute();
        long seconds = rec.getOffsetSecond() == Long.MIN_VALUE ? 0 : rec.getOffsetSecond();

        long result = sign * (((hours * 60 + minutes) * 60 + seconds) * 1_000_000_000L + nanoseconds);
        return result;
    }

    @TruffleBoundary
    public static String parseTemporalTimeZone(String string) {
        JSTemporalTimeZoneRecord result = parseTemporalTimeZoneString(string);
        if (!isNullish(result.getName())) {
            return result.getName();
        }
        if (result.isZ()) {
            return UTC;
        }
        return result.getOffsetString();
    }

    private static JSTemporalTimeZoneRecord parseTemporalTimeZoneString(String string) {
        return parseTemporalTimeZoneString(string, false);
    }

    @TruffleBoundary
    private static JSTemporalTimeZoneRecord parseTemporalTimeZoneString(String string, boolean offsetRequired) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseTimeZoneString();
        if (rec == null) {
            throw Errors.createRangeError("TemporalTimeZoneString expected");
        }
        if (offsetRequired) {
            if (rec.getOffsetHour() == Long.MIN_VALUE && !rec.getZ()) {
                throw TemporalErrors.createRangeErrorTimeZoneOffsetExpected();
            }
        }
        // 3. Let z, sign, hours, minutes, seconds, fraction, and name;
        if (rec.getZ()) {
            return JSTemporalTimeZoneRecord.create(true, null, rec.getTimeZoneIANAName());
        }

        String offsetString = null;
        if (rec.getOffsetHour() < 0) {
            offsetString = null;
        } else {
            long nanoseconds;
            if (rec.getOffsetFraction() == null) {
                nanoseconds = 0;
            } else {
                String fraction = rec.getOffsetFraction() + "000000000";
                fraction = fraction.substring(0, 9); // spec says 1-10, but includes dot!
                assert !fraction.contains(".");
                nanoseconds = Long.parseLong(fraction, 10);
            }

            String signS = rec.getOffsetSign();
            int sign = ("-".equals(signS) || "\u2212".equals(signS)) ? -1 : 1;

            long hours = rec.getOffsetHour() == Long.MIN_VALUE ? 0 : rec.getOffsetHour();
            long minutes = rec.getOffsetMinute() == Long.MIN_VALUE ? 0 : rec.getOffsetMinute();
            long seconds = rec.getOffsetSecond() == Long.MIN_VALUE ? 0 : rec.getOffsetSecond();

            long offsetNanoseconds = sign * (((hours * 60 + minutes) * 60 + seconds) * 1_000_000_000L + nanoseconds);
            offsetString = formatTimeZoneOffsetString(offsetNanoseconds);
        }

        String name = rec.getTimeZoneIANAName();
        if (!isNullish(name)) {
            if (!isValidTimeZoneName(name)) {
                throw TemporalErrors.createRangeErrorInvalidTimeZoneString();
            }
            name = canonicalizeTimeZoneName(name);
        }
        return JSTemporalTimeZoneRecord.create(false, offsetString, name);
    }

    public static String toTemporalDisambiguation(DynamicObject options, TemporalGetOptionNode getOptionNode) {
        return (String) getOptionNode.execute(options, DISAMBIGUATION, OptionTypeEnum.STRING, listCELR, COMPATIBLE);
    }

    public static String toTemporalDisambiguation(DynamicObject options) {
        return (String) getOption(options, DISAMBIGUATION, OptionTypeEnum.STRING, listCELR, COMPATIBLE);
    }

    /**
     * Note there also is {@link ToTemporalZonedDateTimeNode}.
     */
    @TruffleBoundary
    public static JSTemporalZonedDateTimeObject toTemporalZonedDateTime(JSContext ctx, JSRealm realm, Object item, DynamicObject optionsParam) {
        DynamicObject options = optionsParam;
        if (isNullish(options)) {
            options = JSOrdinary.createWithNullPrototype(ctx);
        }
        JSTemporalDateTimeRecord result;
        String offsetString = null;
        DynamicObject timeZone = null;
        DynamicObject calendar = null;
        OffsetBehaviour offsetBehaviour = OffsetBehaviour.OPTION;
        MatchBehaviour matchBehaviour = MatchBehaviour.MATCH_EXACTLY;
        if (JSRuntime.isObject(item)) {
            DynamicObject itemObj = (DynamicObject) item;
            if (isTemporalZonedDateTime(itemObj)) {
                return (JSTemporalZonedDateTimeObject) itemObj;
            }
            calendar = getTemporalCalendarWithISODefault(ctx, realm, itemObj);
            List<String> fieldNames = calendarFields(ctx, calendar, listDHMMMMMNSY);
            fieldNames.add(TIME_ZONE);
            fieldNames.add(OFFSET);
            DynamicObject fields = prepareTemporalFields(ctx, itemObj, fieldNames, listTimeZone);
            Object timeZoneObj = JSObject.get(fields, TIME_ZONE);
            timeZone = toTemporalTimeZone(ctx, timeZoneObj);
            Object offsetStringObj = JSObject.get(fields, OFFSET);
            if (isNullish(offsetStringObj)) {
                offsetBehaviour = OffsetBehaviour.WALL;
            } else {
                offsetString = JSRuntime.toString(offsetStringObj);
            }
            result = interpretTemporalDateTimeFields(calendar, fields, options);
        } else {
            toTemporalOverflow(options);
            String string = JSRuntime.toString(item);
            JSTemporalZonedDateTimeRecord resultZDT = parseTemporalZonedDateTimeString(string);
            result = resultZDT;
            if (resultZDT.getTimeZoneZ()) {
                offsetBehaviour = OffsetBehaviour.EXACT;
            } else if (offsetString == null) {
                offsetBehaviour = OffsetBehaviour.WALL;
            }
            timeZone = createTemporalTimeZone(ctx, resultZDT.getTimeZoneName());
            offsetString = resultZDT.getTimeZoneOffsetString();
            calendar = toTemporalCalendarWithISODefault(ctx, realm, result.getCalendar());
            matchBehaviour = MatchBehaviour.MATCH_MINUTES;
        }
        long offsetNanoseconds = parseTimeZoneOffsetString(offsetString);
        String disambiguation = toTemporalDisambiguation(options);
        String offset = toTemporalOffset(options, REJECT);
        BigInt epochNanoseconds = interpretISODateTimeOffset(ctx, realm, result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(),
                        result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), offsetBehaviour, offsetNanoseconds, timeZone, disambiguation, offset,
                        matchBehaviour);
        return createTemporalZonedDateTime(ctx, epochNanoseconds, timeZone, calendar);
    }

    public static String toTemporalOffset(DynamicObject options, String fallback, TemporalGetOptionNode getOptionNode) {
        return (String) getOptionNode.execute(options, OFFSET, OptionTypeEnum.STRING, listPUIR, fallback);
    }

    public static String toTemporalOffset(DynamicObject options, String fallback) {
        return (String) getOption(options, OFFSET, OptionTypeEnum.STRING, listPUIR, fallback);
    }

    public static String toShowTimeZoneNameOption(DynamicObject options, TemporalGetOptionNode getOptionNode) {
        return (String) getOptionNode.execute(options, TIME_ZONE_NAME, OptionTypeEnum.STRING, listAutoNever, AUTO);
    }

    public static String toShowOffsetOption(DynamicObject options, TemporalGetOptionNode getOptionNode) {
        return (String) getOptionNode.execute(options, OFFSET, OptionTypeEnum.STRING, listAutoNever, AUTO);
    }

    public static String temporalZonedDateTimeToString(JSContext ctx, JSRealm realm, DynamicObject zonedDateTime, Object precision, String showCalendar, String showTimeZone, String showOffset) {
        return temporalZonedDateTimeToString(ctx, realm, zonedDateTime, precision, showCalendar, showTimeZone, showOffset, null, null, null);
    }

    public static JSTemporalDateTimeRecord addDateTime(JSContext ctx, long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond, DynamicObject calendar, long years, long months, long weeks, long days, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds, DynamicObject options) {
        JSTemporalDurationRecord timeResult = TemporalUtil.addTime(hour, minute, second, millisecond, microsecond, nanosecond, hours, minutes, seconds, milliseconds, microseconds,
                        nanoseconds);
        DynamicObject datePart = JSTemporalPlainDate.create(ctx, year, month, day, calendar);
        DynamicObject dateDuration = createTemporalDuration(ctx, years, months, weeks, days + timeResult.getDays(), 0L, 0L, 0L, 0L, 0L, 0L);
        JSTemporalPlainDateObject addedDate = (JSTemporalPlainDateObject) TemporalUtil.calendarDateAdd(calendar, datePart, dateDuration, options);
        return JSTemporalDateTimeRecord.create(addedDate.getYear(), addedDate.getMonth(), addedDate.getDay(),
                        timeResult.getHours(), timeResult.getMinutes(), timeResult.getSeconds(),
                        timeResult.getMilliseconds(), timeResult.getMicroseconds(), timeResult.getNanoseconds());
    }

    public static int compareISODateTime(long year, long month, long day, long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds, long year2, long month2,
                    long day2, long hours2, long minutes2, long seconds2, long milliseconds2, long microseconds2, long nanoseconds2) {
        int date = TemporalUtil.compareISODate(year, month, day, year2, month2, day2);
        if (date == 0) {
            return TemporalUtil.compareTemporalTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds, hours2, minutes2, seconds2, milliseconds2, microseconds2, nanoseconds2);
        }
        return date;
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalYearMonthString(String string) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseYearMonth();
        if (rec != null) {
            if (rec.getZ()) {
                throw TemporalErrors.createRangeErrorUnexpectedUTCDesignator();
            }

            long y = rec.getYear() == Long.MIN_VALUE ? 0 : rec.getYear();
            long m = rec.getMonth() == Long.MIN_VALUE ? 0 : rec.getMonth();
            long d = rec.getDay() == Long.MIN_VALUE ? 1 : rec.getDay();
            return JSTemporalDateTimeRecord.createCalendar(y, m, d, 0, 0, 0, 0, 0, 0, rec.getCalendar());
        } else {
            throw Errors.createRangeError("cannot parse YearMonth");
        }
    }

    @TruffleBoundary
    public static String temporalZonedDateTimeToString(JSContext ctx, JSRealm realm, DynamicObject zonedDateTimeParam, Object precision, String showCalendar, String showTimeZone, String showOffset,
                    Double incrementParam, String unitParam, String roundingModeParam) {
        assert isTemporalZonedDateTime(zonedDateTimeParam);
        JSTemporalZonedDateTimeObject zonedDateTime = (JSTemporalZonedDateTimeObject) zonedDateTimeParam;
        double increment = incrementParam == null ? 1 : (double) incrementParam;
        String unit = isNullish(unitParam) ? NANOSECOND : unitParam;
        String roundingMode = isNullish(roundingModeParam) ? TRUNC : roundingModeParam;

        long ns = roundTemporalInstant(zonedDateTime.getNanoseconds(), (long) increment, unit, roundingMode);
        DynamicObject timeZone = zonedDateTime.getTimeZone();
        JSTemporalInstantObject instant = JSTemporalInstant.create(ctx, BigInt.valueOf(ns));
        JSTemporalCalendarObject isoCalendar = (JSTemporalCalendarObject) getISO8601Calendar(realm);
        JSTemporalPlainDateTimeObject temporalDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, timeZone, instant, isoCalendar);
        String dateTimeString = JSTemporalPlainDateTime.temporalDateTimeToString(temporalDateTime.getYear(), temporalDateTime.getMonth(), temporalDateTime.getDay(),
                        temporalDateTime.getHour(), temporalDateTime.getMinute(), temporalDateTime.getSecond(), temporalDateTime.getMillisecond(),
                        temporalDateTime.getMicrosecond(), temporalDateTime.getNanosecond(), isoCalendar, precision, NEVER);
        String offsetString = null;
        String timeZoneString = null;
        if (NEVER.equals(showOffset)) {
            offsetString = "";
        } else {
            long offsetNs = getOffsetNanosecondsFor(timeZone, instant);
            offsetString = formatISOTimeZoneOffsetString(offsetNs);
        }
        if (NEVER.equals(showTimeZone)) {
            timeZoneString = "";
        } else {
            String timeZoneID = JSRuntime.toString(timeZone);
            timeZoneString = "[" + timeZoneID + "]";
        }
        String calendarID = JSRuntime.toString(zonedDateTime.getCalendar());
        String calendarString = formatCalendarAnnotation(calendarID, showCalendar);
        return dateTimeString + offsetString + timeZoneString + calendarString;
    }

    @TruffleBoundary
    private static String formatISOTimeZoneOffsetString(long offsetNs) {
        long offsetNanoseconds = roundNumberToIncrement(offsetNs, 60_000_000_000L, HALF_EXPAND);
        String sign = "";
        sign = (offsetNanoseconds >= 0) ? "+" : "-";
        offsetNanoseconds = Math.abs(offsetNanoseconds);
        long minutes = (offsetNanoseconds / 60_000_000_000L) % 60;
        long hours = (long) Math.floor(offsetNanoseconds / 3_600_000_000_000L);

        String h = String.format("%1$02d", hours);
        String m = String.format("%1$02d", minutes);

        return sign + h + ":" + m;
    }

    @TruffleBoundary
    public static JSTemporalZonedDateTimeRecord parseTemporalZonedDateTimeString(String string) {
        if (!(new TemporalParser(string)).isTemporalZonedDateTimeString()) {
            throw Errors.createRangeError("cannot be parsed as TemporalZonedDateTimeString");
        }
        JSTemporalDateTimeRecord result;
        try {
            result = parseISODateTime(string);
        } catch (Exception ex) {
            throw Errors.createRangeError("cannot be parsed as TemporalZonedDateTimeString");
        }
        JSTemporalTimeZoneRecord timeZoneResult = parseTemporalTimeZoneString(string);
        return JSTemporalZonedDateTimeRecord.create(result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(),
                        result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), result.getCalendar(),
                        timeZoneResult.isZ(), timeZoneResult.getOffsetString(), timeZoneResult.getName());
    }

    @TruffleBoundary
    private static BigInt parseTemporalInstant(String string) {
        JSTemporalZonedDateTimeRecord result = parseTemporalInstantString(string);
        String offsetString = result.getTimeZoneOffsetString();
        if (isNullish(offsetString)) {
            throw Errors.createRangeError("timeZoneOffsetString expected");
        }
        BigInteger utc = getEpochFromISOParts(result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(),
                        result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
        if (utc.compareTo(temporalInstantLowerBound) < 0 || utc.compareTo(temporalInstantUpperBound) > 0) {
            throw Errors.createRangeError("value out of bounds");
        }
        long offsetNanoseconds = parseTimeZoneOffsetString(offsetString);
        return new BigInt(utc.subtract(BigInteger.valueOf(offsetNanoseconds)));
    }

    @TruffleBoundary
    private static JSTemporalZonedDateTimeRecord parseTemporalInstantString(String string) {
        try {
            JSTemporalDateTimeRecord result = parseISODateTime(string);
            JSTemporalTimeZoneRecord timeZoneResult = parseTemporalTimeZoneString(string, true);
            String offsetString = timeZoneResult.getOffsetString();
            if (timeZoneResult.isZ()) {
                offsetString = "+00:00";
            }
            assert !isNullish(offsetString);
            return JSTemporalZonedDateTimeRecord.create(result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(),
                            result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), null, false, offsetString, null);
        } catch (Exception ex) {
            throw Errors.createRangeError("Instant cannot be parsed");
        }
    }

    @TruffleBoundary
    public static JSTemporalInstantObject builtinTimeZoneGetInstantFor(JSContext ctx, DynamicObject timeZone, JSTemporalPlainDateTimeObject dateTime, String disambiguation) {
        List<JSTemporalInstantObject> possibleInstants = getPossibleInstantsFor(timeZone, dateTime);
        return disambiguatePossibleInstants(ctx, possibleInstants, timeZone, dateTime, disambiguation);
    }

    @TruffleBoundary
    public static JSTemporalInstantObject disambiguatePossibleInstants(JSContext ctx, List<JSTemporalInstantObject> possibleInstants, DynamicObject timeZone, JSTemporalPlainDateTimeObject dateTime,
                    String disambiguation) {
        int n = possibleInstants.size();
        if (n == 1) {
            return possibleInstants.get(0);
        } else if (n != 0) {
            if (EARLIER.equals(disambiguation) || COMPATIBLE.equals(disambiguation)) {
                return possibleInstants.get(0);
            } else if (LATER.equals(disambiguation)) {
                return possibleInstants.get(n - 1);
            }
            assert REJECT.equals(disambiguation);
            throw Errors.createRangeError("invalid disambiguation");
        }
        assert n == 0;
        if (REJECT.equals(disambiguation)) {
            throw Errors.createRangeError("disambiguation failed");
        }
        BigInteger epochNanoseconds = getEpochFromISOParts(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
                        dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond());
        JSTemporalInstantObject dayBefore = JSTemporalInstant.create(ctx, new BigInt(epochNanoseconds.subtract(bi_8_64_13)));
        JSTemporalInstantObject dayAfter = JSTemporalInstant.create(ctx, new BigInt(epochNanoseconds.add(bi_8_64_13)));
        long offsetBefore = getOffsetNanosecondsFor(timeZone, dayBefore);
        long offsetAfter = getOffsetNanosecondsFor(timeZone, dayAfter);
        long nanoseconds = offsetAfter - offsetBefore;
        if (EARLIER.equals(disambiguation)) {
            JSTemporalDateTimeRecord earlier = addDateTime(ctx, dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
                            dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(), dateTime.getCalendar(), 0, 0, 0, 0, 0, 0, 0, 0, 0, -nanoseconds, Undefined.instance);
            JSTemporalPlainDateTimeObject earlierDateTime = createTemporalDateTime(ctx, earlier.getYear(), earlier.getMonth(), earlier.getDay(), earlier.getHour(), earlier.getMinute(),
                            earlier.getSecond(), earlier.getMillisecond(), earlier.getMicrosecond(), earlier.getNanosecond(), dateTime.getCalendar());
            List<JSTemporalInstantObject> possibleInstants2 = getPossibleInstantsFor(timeZone, earlierDateTime);
            if (possibleInstants2.size() == 0) {
                throw Errors.createRangeError("nothing found");
            }
            return possibleInstants2.get(0);
        }
        assert LATER.equals(disambiguation) || COMPATIBLE.equals(disambiguation);
        JSTemporalDateTimeRecord later = addDateTime(ctx, dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
                        dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(), dateTime.getCalendar(), 0, 0, 0, 0, 0, 0, 0, 0, 0, nanoseconds, Undefined.instance);
        JSTemporalPlainDateTimeObject laterDateTime = createTemporalDateTime(ctx, later.getYear(), later.getMonth(), later.getDay(), later.getHour(), later.getMinute(),
                        later.getSecond(), later.getMillisecond(), later.getMicrosecond(), later.getNanosecond(), dateTime.getCalendar());

        List<JSTemporalInstantObject> possibleInstants2 = getPossibleInstantsFor(timeZone, laterDateTime);
        n = possibleInstants2.size();
        if (n == 0) {
            throw Errors.createRangeError("nothing found");
        }
        return possibleInstants2.get(n - 1);
    }

    @TruffleBoundary
    public static BigInt interpretISODateTimeOffset(JSContext ctx, JSRealm realm, long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond, OffsetBehaviour offsetBehaviour, Object offsetNanosecondsParam, DynamicObject timeZone, String disambiguation, String offsetOption,
                    MatchBehaviour matchBehaviour) {
        double offsetNs = isNullish(offsetNanosecondsParam) ? Double.NaN : ((Number) offsetNanosecondsParam).doubleValue();
        DynamicObject calendar = getISO8601Calendar(realm);
        JSTemporalPlainDateTimeObject dateTime = createTemporalDateTime(ctx, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, calendar);
        if (offsetBehaviour == OffsetBehaviour.WALL || IGNORE.equals(offsetOption)) {
            JSTemporalInstantObject instant = builtinTimeZoneGetInstantFor(ctx, timeZone, dateTime, disambiguation);
            return instant.getNanoseconds();
        }
        if (offsetBehaviour == OffsetBehaviour.EXACT || USE.equals(offsetOption)) {
            BigInteger epochNanoseconds = getEpochFromISOParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
            return new BigInt(epochNanoseconds.subtract(BigInteger.valueOf((long) offsetNs)));
        }
        assert offsetBehaviour == OffsetBehaviour.OPTION;
        assert PREFER.equals(offsetOption) || REJECT.equals(offsetOption);
        List<JSTemporalInstantObject> possibleInstants = getPossibleInstantsFor(timeZone, dateTime);
        for (JSTemporalInstantObject candidate : possibleInstants) {
            long candidateNanoseconds = getOffsetNanosecondsFor(timeZone, candidate);
            if (candidateNanoseconds == offsetNs) {
                return candidate.getNanoseconds();
            }
            if (matchBehaviour == MatchBehaviour.MATCH_MINUTES) {
                long roundedCandidateNanoseconds = roundNumberToIncrement(candidateNanoseconds, 60_000_000_000L, HALF_EXPAND);
                if (roundedCandidateNanoseconds == offsetNs) {
                    return candidate.getNanoseconds();
                }
            }
        }
        if (REJECT.equals(offsetOption)) {
            throw Errors.createRangeError("cannot interpret DateTime offset");
        }
        JSTemporalInstantObject instant = builtinTimeZoneGetInstantFor(ctx, timeZone, dateTime, disambiguation);
        return instant.getNanoseconds();
    }

    public static BigInt addZonedDateTime(JSContext ctx, BigInt epochNanoseconds, DynamicObject timeZone, DynamicObject calendar, long years, long months, long weeks, long days,
                    long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        return addZonedDateTime(ctx, epochNanoseconds, timeZone, calendar, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, Undefined.instance);
    }

    public static BigInt addZonedDateTime(JSContext ctx, long epochNanoseconds, DynamicObject timeZone, DynamicObject calendar, long years, long months, long weeks, long days,
                    long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        return addZonedDateTime(ctx, BigInt.valueOf(epochNanoseconds), timeZone, calendar, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds,
                        nanoseconds, Undefined.instance);
    }

    public static BigInt addZonedDateTime(JSContext ctx, BigInt epochNanoseconds, DynamicObject timeZone, DynamicObject calendar, long years, long months, long weeks, long days,
                    long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds, DynamicObject options) {
        if (years == 0 && months == 0 && weeks == 0 && days == 0) {
            return addInstant(epochNanoseconds, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        JSTemporalInstantObject instant = JSTemporalInstant.create(ctx, epochNanoseconds);
        JSTemporalPlainDateTimeObject temporalDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, timeZone, instant, calendar);
        JSTemporalPlainDateObject datePart = createTemporalDate(ctx, temporalDateTime.getYear(), temporalDateTime.getMonth(), temporalDateTime.getDay(), calendar);
        JSTemporalDurationObject dateDuration = createTemporalDuration(ctx, years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        JSTemporalPlainDateObject addedDate = (JSTemporalPlainDateObject) calendarDateAdd(calendar, datePart, dateDuration, options);
        JSTemporalPlainDateTimeObject intermediateDateTime = createTemporalDateTime(ctx, addedDate.getYear(), addedDate.getMonth(), addedDate.getDay(),
                        temporalDateTime.getHour(), temporalDateTime.getMinute(), temporalDateTime.getSecond(),
                        temporalDateTime.getMillisecond(), temporalDateTime.getMicrosecond(), temporalDateTime.getNanosecond(), calendar);
        JSTemporalInstantObject intermediateInstant = builtinTimeZoneGetInstantFor(ctx, timeZone, intermediateDateTime, COMPATIBLE);
        return addInstant(intermediateInstant.getNanoseconds(), hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    public static JSTemporalDurationObject createTemporalDuration(JSContext ctx, long years, long months, long weeks, long days, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds) {
        return (JSTemporalDurationObject) JSTemporalDuration.create(ctx, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    public static DynamicObject moveRelativeZonedDateTime(JSContext ctx, DynamicObject zonedDateTime, long years, long months, long weeks, long days) {
        JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) zonedDateTime;
        BigInt intermediateNs = addZonedDateTime(ctx, zdt.getNanoseconds(), zdt.getTimeZone(), zdt.getCalendar(), years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        return createTemporalZonedDateTime(ctx, intermediateNs, zdt.getTimeZone(), zdt.getCalendar());
    }

    @TruffleBoundary
    public static boolean timeZoneEquals(DynamicObject tz1, DynamicObject tz2) {
        if (tz1 == tz2) {
            return true;
        }
        String s1 = JSRuntime.toString(tz1);
        String s2 = JSRuntime.toString(tz2);
        return s1.equals(s2);
    }

    @TruffleBoundary
    public static DynamicObject consolidateCalendars(DynamicObject one, DynamicObject two) {
        if (one == two) {
            return two;
        }
        String s1 = JSRuntime.toString(one);
        String s2 = JSRuntime.toString(two);
        if (s1.equals(s2)) {
            return two;
        }
        if (ISO8601.equals(s1)) {
            return two;
        }
        if (ISO8601.equals(s2)) {
            return one;
        }
        throw Errors.createRangeError("cannot consolidate calendars");
    }

    private static List<JSTemporalInstantObject> getPossibleInstantsFor(DynamicObject timeZone, DynamicObject dateTime) {
        Object fn = JSObject.get(timeZone, "getPossibleInstantsFor");
        DynamicObject possibleInstants = toDynamicObject(JSRuntime.call(fn, timeZone, new Object[]{dateTime}));
        IteratorRecord iteratorRecord = JSRuntime.getIterator(possibleInstants);
        List<JSTemporalInstantObject> list = new ArrayList<>();
        Object next = true;
        while (next != Boolean.FALSE) {
            next = JSRuntime.iteratorStep(iteratorRecord);
            if (next != Boolean.FALSE) {
                Object nextValue = JSRuntime.iteratorValue((DynamicObject) next);
                if (!isTemporalInstant(nextValue)) {
                    JSRuntime.iteratorClose(possibleInstants);
                    throw Errors.createTypeError("unexpected value");
                }
                list.add((JSTemporalInstantObject) nextValue);
            }
        }
        return list;
    }

    @TruffleBoundary
    @SuppressWarnings("unused")
    public static List<BigInt> getIANATimeZoneEpochValue(String identifier, long isoYear, long isoMonth, long isoDay, long hours, long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds) {
        List<BigInt> list = new ArrayList<>();
        try {
            ZoneId zoneId = ZoneId.of(identifier);
            long fractions = milliseconds * 1_000_000L + microseconds * 1_000L + nanoseconds;
            ZonedDateTime zdt = ZonedDateTime.of((int) isoYear, (int) isoMonth, (int) isoDay, (int) hours, (int) minutes, (int) seconds, (int) fractions, zoneId);
            list.add(BigInt.valueOf(zdt.toEpochSecond() * 1_000_000_000L + fractions));
        } catch (Exception ex) {
            assert false;
        }
        return list;
    }

    @TruffleBoundary
    @SuppressWarnings("unused")
    public static long getIANATimeZoneOffsetNanoseconds(BigInt nanoseconds, String identifier) {
        try {
            Instant instant = Instant.ofEpochSecond(0, nanoseconds.longValue()); // TODO wrong
            ZoneId zoneId = ZoneId.of(identifier);
            ZoneRules zoneRule = zoneId.getRules();
            ZoneOffset offset = zoneRule.getOffset(instant);
            return offset.getTotalSeconds() * 1_000_000_000L;
        } catch (Exception ex) {
            assert false;
            return Long.MIN_VALUE;
        }
    }

    @TruffleBoundary
    @SuppressWarnings("unused")
    public static OptionalLong getIANATimeZoneNextTransition(BigInt nanoseconds, String identifier) {
        try {
            BigInteger[] sec = nanoseconds.bigIntegerValue().divideAndRemainder(bi_10_pow_9);
            Instant instant = Instant.ofEpochSecond(sec[0].longValue(), sec[1].longValue());
            ZoneId zoneId = ZoneId.of(identifier);
            ZoneRules zoneRule = zoneId.getRules();
            ZoneOffsetTransition nextTransition = zoneRule.nextTransition(instant);
            if (nextTransition == null) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(nextTransition.toEpochSecond() * 1_000_000_000L);
        } catch (Exception ex) {
            assert false;
            return OptionalLong.of(Long.MIN_VALUE);
        }
    }

    @TruffleBoundary
    @SuppressWarnings("unused")
    public static OptionalLong getIANATimeZonePreviousTransition(BigInt nanoseconds, String identifier) {
        try {
            BigInteger[] sec = nanoseconds.bigIntegerValue().divideAndRemainder(bi_10_pow_9);
            Instant instant = Instant.ofEpochSecond(sec[0].longValue(), sec[1].longValue());
            ZoneId zoneId = ZoneId.of(identifier);
            ZoneRules zoneRule = zoneId.getRules();
            ZoneOffsetTransition previousTransition = zoneRule.previousTransition(instant);
            if (previousTransition == null) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(previousTransition.toEpochSecond() * 1_000_000_000L);
        } catch (Exception ex) {
            assert false;
            return OptionalLong.empty();
        }
    }

    // this should not be used according to spec (but polyfill uses it)
    // TODO https://github.com/tc39/proposal-temporal/issues/1754
    private static long trunc(double in) {
        if (in >= 0) {
            return (long) Math.floor(in);
        } else {
            return -(long) Math.floor(-in);
        }
    }

    public static long integralPartOf(double in) {
        return trunc(in);
    }
}
