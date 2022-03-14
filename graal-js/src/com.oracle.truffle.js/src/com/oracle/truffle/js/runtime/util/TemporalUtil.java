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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.GREGORY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.IGNORE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ISO8601;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.JAPANESE;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerOrInfinityNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerWithoutRoundingNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.temporal.GetTemporalCalendarWithISODefaultNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
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
import com.oracle.truffle.js.runtime.Strings;
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
    private static final Function<Object, Object> toString = JSRuntime::toString;

    public static final Set<TruffleString> pluralUnits = Set.of(YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS,
                    MILLISECONDS, MICROSECONDS, NANOSECONDS);
    public static final Map<TruffleString, TruffleString> pluralToSingular = toMap(
                    new TruffleString[]{YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS},
                    new TruffleString[]{YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND});
    @SuppressWarnings("unchecked") private static final Map<TruffleString, Function<Object, Object>> temporalFieldConversion = toMap(
                    new TruffleString[]{YEAR, MONTH, MONTH_CODE, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND, OFFSET, ERA, ERA_YEAR},
                    new Function[]{toIntegerThrowOnInfinity, toPositiveInteger, toString, toPositiveInteger, toIntegerThrowOnInfinity, toIntegerThrowOnInfinity, toIntegerThrowOnInfinity,
                                    toIntegerThrowOnInfinity, toIntegerThrowOnInfinity, toIntegerThrowOnInfinity, toString, toString, toIntegerThrowOnInfinity});
    public static final Map<TruffleString, Object> temporalFieldDefaults = toMap(
                    new TruffleString[]{YEAR, MONTH, MONTH_CODE, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND, YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS,
                                    MICROSECONDS, NANOSECONDS, OFFSET, ERA, ERA_YEAR},
                    new Object[]{Undefined.instance, Undefined.instance, Undefined.instance, Undefined.instance, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Undefined.instance, Undefined.instance,
                                    Undefined.instance});

    public static final List<TruffleString> listEmpty = List.of();
    public static final List<TruffleString> listYMWD = List.of(YEAR, MONTH, WEEK, DAY);
    public static final List<TruffleString> listPluralYMWD = List.of(YEARS, MONTHS, WEEKS, DAYS);
    public static final List<TruffleString> listYMW = List.of(YEAR, MONTH, WEEK);
    public static final List<TruffleString> listYMWDH = List.of(YEAR, MONTH, WEEK, DAY, HOUR);
    public static final List<TruffleString> listTime = List.of(HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND);
    public static final List<TruffleString> listDMMCY = List.of(DAY, MONTH, MONTH_CODE, YEAR);
    public static final List<TruffleString> listMMCY = List.of(MONTH, MONTH_CODE, YEAR);
    public static final List<TruffleString> listMCY = List.of(MONTH_CODE, YEAR);
    public static final List<TruffleString> listDMC = List.of(DAY, MONTH_CODE);
    public static final List<TruffleString> listY = List.of(YEAR);
    public static final List<TruffleString> listD = List.of(DAY);
    public static final List<TruffleString> listWDHMSMMN = List.of(WEEK, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND);
    public static final List<TruffleString> listAllDateTime = List.of(YEARS, YEAR, MONTHS, MONTH, WEEKS, WEEK, DAYS, DAY, HOURS, HOUR, MINUTES, MINUTE, SECONDS, SECOND, MILLISECONDS, MILLISECOND,
                    MICROSECONDS, MICROSECOND, NANOSECONDS, NANOSECOND);
    public static final List<TruffleString> listAllDateTimeAuto = List.of(AUTO, YEARS, YEAR, MONTHS, MONTH, WEEKS, WEEK, DAYS, DAY, HOURS, HOUR, MINUTES, MINUTE, SECONDS, SECOND, MILLISECONDS,
                    MILLISECOND,
                    MICROSECONDS, MICROSECOND, NANOSECONDS, NANOSECOND);
    public static final List<TruffleString> listDHMMMMMNSY = List.of(DAY, HOUR, MICROSECOND, MILLISECOND, MINUTE, MONTH, MONTH_CODE, NANOSECOND, SECOND, YEAR);

    public static final List<TruffleString> listAuto = List.of(AUTO);
    public static final List<TruffleString> listAutoNever = List.of(AUTO, NEVER);
    public static final List<TruffleString> listAutoAlwaysNever = List.of(AUTO, ALWAYS, NEVER);
    public static final List<TruffleString> listConstrainReject = List.of(CONSTRAIN, REJECT);
    public static final List<TruffleString> listTimeZone = List.of(TIME_ZONE);
    public static final List<TruffleString> listTimeZoneOffset = List.of(TIME_ZONE, OFFSET);
    public static final List<TruffleString> listRoundingMode = List.of(CEIL, FLOOR, TRUNC, HALF_EXPAND);
    public static final List<TruffleString> listOffset = List.of(PREFER, USE, IGNORE, REJECT);
    public static final List<TruffleString> listDisambiguation = List.of(COMPATIBLE, EARLIER, LATER, REJECT);

    public static final TruffleString[] TIME_LIKE_PROPERTIES = new TruffleString[]{HOUR, MICROSECOND, MILLISECOND, MINUTE, NANOSECOND, SECOND};
    public static final TruffleString[] DURATION_PROPERTIES = new TruffleString[]{DAYS, HOURS, MICROSECONDS, MILLISECONDS, MINUTES, MONTHS, NANOSECONDS, SECONDS, WEEKS, YEARS};

    private static final BigInt upperEpochNSLimit = new BigInt(BigInteger.valueOf(86400).multiply(BigInteger.valueOf(10).pow(17)));
    private static final BigInt lowerEpochNSLimit = upperEpochNSLimit.negate();

    // 8.64* 10^21 + 8.64 * 10^13; roughly 273,000 years
    private static final BigInteger isoTimeUpperBound = new BigInteger("8640000086400000000000");
    private static final BigInteger isoTimeLowerBound = isoTimeUpperBound.negate();

    // 8.64 * 10^21
    private static final BigInteger temporalInstantUpperBound = new BigInteger("8640000000000000000000");
    private static final BigInteger temporalInstantLowerBound = temporalInstantUpperBound.negate();

    // 8.64 * 10^13
    private static final BigInteger bi_8_64_13 = new BigInteger("86400000000000");

    public static final BigInteger bi_10_pow_9 = new BigInteger("1000000000"); // 10 ^ 9
    public static final BigInteger bi_10_pow_6 = new BigInteger("1000000"); // 10 ^ 6
    public static final BigInteger bi_1000 = new BigInteger("1000");  // 10 ^ 3

    public static final BigDecimal bd_10 = new BigDecimal("10");
    public static final BigDecimal bd_1000 = new BigDecimal("1000");

    public static final char UNICODE_MINUS_SIGN = '\u2212';

    public static final MathContext mc_20_floor = new MathContext(20, java.math.RoundingMode.FLOOR);

    public static final TruffleString FRACTIONAL_SECOND_DIGITS = Strings.constant("fractionalSecondDigits");
    public static final TruffleString ZEROS = Strings.constant("000000000");
    public static final TruffleString OFFSET_ZERO = Strings.constant("+00:00");
    public static final TruffleString CALENDAR_NAME = Strings.constant("calendarName");
    public static final TruffleString BRACKET_U_CA_EQUALS = Strings.constant("[u-ca=");

    public static final TruffleString GET_OFFSET_NANOSECONDS_FOR = Strings.constant("getOffsetNanosecondsFor");
    public static final TruffleString YEAR_MONTH_FROM_FIELDS = Strings.constant("yearMonthFromFields");
    public static final TruffleString MONTH_DAY_FROM_FIELDS = Strings.constant("monthDayFromFields");
    public static final TruffleString GET_POSSIBLE_INSTANTS_FOR = Strings.constant("getPossibleInstantsFor");

    public enum Overflow {
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

    public enum OptionType {
        STRING,
        NUMBER,
        BOOLEAN,
        NUMBER_AND_STRING;

        public boolean allowsNumber() {
            return this == NUMBER || this == NUMBER_AND_STRING;
        }

        public boolean allowsString() {
            return this == STRING || this == NUMBER_AND_STRING;
        }

        public boolean allowsBoolean() {
            return this == BOOLEAN;
        }

        public OptionType getLast() {
            switch (this) {
                case STRING:
                    return STRING;
                case NUMBER:
                    return NUMBER;
                case BOOLEAN:
                    return BOOLEAN;
                case NUMBER_AND_STRING:
                    return STRING;
            }
            throw Errors.shouldNotReachHere();
        }
    }

    public enum Unit {
        EMPTY,
        AUTO,
        YEAR,
        MONTH,
        WEEK,
        DAY,
        HOUR,
        MINUTE,
        SECOND,
        MILLISECOND,
        MICROSECOND,
        NANOSECOND;

        @TruffleBoundary
        @Override
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        @TruffleBoundary
        public TruffleString toTruffleString() {
            return Strings.fromJavaString(toString());
        }
    }

    public enum RoundingMode {
        EMPTY,
        CEIL,
        FLOOR,
        TRUNC,
        HALF_EXPAND
    }

    public enum Disambiguation {
        EARLIER,
        LATER,
        COMPATIBLE,
        REJECT
    }

    public enum OffsetOption {
        USE,
        IGNORE,
        PREFER,
        REJECT
    }

    public enum ShowCalendar {
        AUTO,
        ALWAYS,
        NEVER
    }

    /**
     * Note there also is {@link TemporalGetOptionNode}.
     */
    public static Object getOption(DynamicObject options, TruffleString property, OptionType types, List<?> values, Object fallback, JSToBooleanNode toBoolean, JSToNumberNode toNumber,
                                   JSToStringNode toStringNode) {
        assert JSRuntime.isObject(options);
        Object value = JSObject.get(options, property);
        if (value == Undefined.instance) {
            return fallback;
        }
        OptionType type;
        if (value instanceof Boolean && types.allowsBoolean()) {
            type = OptionType.BOOLEAN;
        } else if (Strings.isTString(value) && types.allowsString()) {
            type = OptionType.STRING;
        } else if (JSRuntime.isNumber(value) && types.allowsNumber()) {
            type = OptionType.NUMBER;
        } else {
            type = types.getLast();
        }

        if (type.allowsBoolean()) {
            value = toBoolean.executeBoolean(value);
        } else if (type.allowsNumber()) {
            value = toNumber.executeNumber(value);
            if (Double.isNaN(JSRuntime.doubleValue((Number) value))) {
                throw TemporalErrors.createRangeErrorNumberIsNaN();
            }
        } else {
            assert type.allowsString();
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
    public static Object getOption(DynamicObject options, TruffleString property, OptionType types, List<TruffleString> values, Object fallback) {
        assert JSRuntime.isObject(options);
        Object value = JSObject.get(options, property);
        if (value == Undefined.instance) {
            return fallback;
        }
        OptionType type;
        if (value instanceof Boolean && types.allowsBoolean()) {
            type = OptionType.BOOLEAN;
        } else if (Strings.isTString(value) && types.allowsString()) {
            type = OptionType.STRING;
        } else if (JSRuntime.isNumber(value) && types.allowsNumber()) {
            type = OptionType.NUMBER;
        } else {
            type = types.getLast();
        }

        if (type.allowsBoolean()) {
            value = JSRuntime.toBoolean(value);
        } else if (type.allowsNumber()) {
            value = JSRuntime.toNumber(value);
            if (Double.isNaN(((Number) value).doubleValue())) {
                throw TemporalErrors.createRangeErrorNumberIsNaN();
            }
        } else {
            assert type.allowsString();
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
    public static double getNumberOption(DynamicObject options, TruffleString property, double minimum, double maximum,
                    double fallback, IsObjectNode isObject,
                    JSToNumberNode numberNode) {
        assert isObject.executeBoolean(options);
        Object value = JSObject.get(options, property);
        return defaultNumberOptions(value, minimum, maximum, fallback, numberNode);
    }

    // 13.5
    @TruffleBoundary
    public static Object getStringOrNumberOption(DynamicObject options, TruffleString property, List<TruffleString> stringValues,
                    double minimum, double maximum, Object fallback) {
        assert JSRuntime.isObject(options);
        Object value = getOption(options, property, OptionType.NUMBER_AND_STRING, null, fallback);
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
    public static Overflow toTemporalOverflow(DynamicObject options, JSToBooleanNode toBoolean, JSToNumberNode toNumberNode, JSToStringNode toStringNode) {
        return toOverflow((TruffleString) getOption(options, OVERFLOW, OptionType.STRING, listConstrainReject, CONSTRAIN, toBoolean, toNumberNode, toStringNode));
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
        Unit smallestUnit = toSmallestTemporalUnit(options, listYMWDH, null, getOptionNode);

        if (Unit.MINUTE == smallestUnit) {
            return JSTemporalPrecisionRecord.create(MINUTE, Unit.MINUTE, 1);
        } else if (Unit.SECOND == smallestUnit) {
            return JSTemporalPrecisionRecord.create(0, Unit.SECOND, 1);
        } else if (Unit.MILLISECOND == smallestUnit) {
            return JSTemporalPrecisionRecord.create(3, Unit.MILLISECOND, 1);
        } else if (Unit.MICROSECOND == smallestUnit) {
            return JSTemporalPrecisionRecord.create(6, Unit.MICROSECOND, 1);
        } else if (Unit.NANOSECOND == smallestUnit) {
            return JSTemporalPrecisionRecord.create(9, Unit.NANOSECOND, 1);
        }

        assert smallestUnit == Unit.EMPTY;

        Object digits = getStringOrNumberOption(options, FRACTIONAL_SECOND_DIGITS, listAuto, 0, 9, AUTO);
        if (Boundaries.equals(digits, AUTO)) {
            return JSTemporalPrecisionRecord.create(AUTO, Unit.NANOSECOND, 1);
        }
        int iDigit = JSRuntime.intValue((Number) digits);

        if (iDigit == 0) {
            return JSTemporalPrecisionRecord.create(0, Unit.SECOND, 1);
        }
        if (iDigit == 1 || iDigit == 2 || iDigit == 3) {
            return JSTemporalPrecisionRecord.create(digits, Unit.MILLISECOND, Math.pow(10, 3 - toLong(digits)));
        }
        if (iDigit == 4 || iDigit == 5 || iDigit == 6) {
            return JSTemporalPrecisionRecord.create(digits, Unit.MICROSECOND, Math.pow(10, 6 - toLong(digits)));
        }
        assert iDigit == 7 || iDigit == 8 || iDigit == 9;
        return JSTemporalPrecisionRecord.create(digits, Unit.NANOSECOND, Math.pow(10, 9 - toLong(digits)));
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
    public static Unit toSmallestTemporalUnit(DynamicObject normalizedOptions, List<TruffleString> disallowedUnits, TruffleString fallback, TemporalGetOptionNode getOptionNode) {
        TruffleString smallestUnit = (TruffleString) getOptionNode.execute(normalizedOptions, SMALLEST_UNIT, OptionType.STRING, listAllDateTime, fallback);
        if (smallestUnit != null && Boundaries.setContains(pluralUnits, smallestUnit)) {
            smallestUnit = Boundaries.mapGet(pluralToSingular, smallestUnit);
        }
        if (smallestUnit != null && Boundaries.listContains(disallowedUnits, smallestUnit)) {
            throw Errors.createRangeError("Smallest unit not allowed.");
        }
        return TemporalUtil.toUnit(smallestUnit);
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
            List<TruffleString> fieldNames = TemporalUtil.calendarFields(ctx, calendar, listDHMMMMMNSY);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(ctx, valueObj, fieldNames, listEmpty);

            DynamicObject dateOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(ctx, dateOptions, OVERFLOW, CONSTRAIN);
            result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, dateOptions);
            offset = JSObject.get(valueObj, OFFSET);
            timeZone = JSObject.get(valueObj, TIME_ZONE);
            if (timeZone != Undefined.instance) {
                timeZone = toTemporalTimeZone(ctx, timeZone);
            }
            if (offset == Undefined.instance) {
                offsetBehaviour = OffsetBehaviour.WALL;
            }
        } else {
            TruffleString string = JSRuntime.toString(value);
            JSTemporalZonedDateTimeRecord resultZDT = parseTemporalRelativeToString(string);
            result = resultZDT;
            calendar = toTemporalCalendarWithISODefault(ctx, realm, result.getCalendar());

            offset = resultZDT.getTimeZoneOffsetString();
            TruffleString timeZoneName = resultZDT.getTimeZoneName();
            if (!isNullish(timeZoneName)) {
                // If ParseText(! StringToCodePoints(timeZoneName), TimeZoneNumericUTCOffset)
                // is not a List of errors
                if (!isValidTimeZoneName(timeZoneName)) {
                    throw TemporalErrors.createRangeErrorInvalidTimeZoneString();
                }
                timeZoneName = canonicalizeTimeZoneName(timeZoneName);
                timeZone = createTemporalTimeZone(ctx, timeZoneName);
            }

            if (resultZDT.getTimeZoneZ()) {
                offsetBehaviour = OffsetBehaviour.EXACT;
            } else {
                offsetBehaviour = OffsetBehaviour.WALL;
            }
            matchBehaviour = MatchBehaviour.MATCH_MINUTES;
        }
        if (!isNullish(timeZone)) {
            DynamicObject timeZoneObj = isNullish(timeZone) ? Undefined.instance : toDynamicObject(timeZone);
            Object offsetNs = 0;
            if (offsetBehaviour == OffsetBehaviour.OPTION) {
                offsetNs = parseTimeZoneOffsetString(JSRuntime.toString(offset));
            } else {
                offsetNs = Undefined.instance;
            }
            BigInt epochNanoseconds = interpretISODateTimeOffset(ctx, realm,
                            result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                            result.getMicrosecond(), result.getNanosecond(), offsetBehaviour, offsetNs, timeZoneObj, Disambiguation.COMPATIBLE, OffsetOption.REJECT, matchBehaviour);
            return createTemporalZonedDateTime(ctx, epochNanoseconds, timeZoneObj, calendar);
        }
        return JSTemporalPlainDate.create(ctx, result.getYear(), result.getMonth(), result.getDay(), calendar);
    }

    @TruffleBoundary
    private static JSTemporalZonedDateTimeRecord parseTemporalRelativeToString(TruffleString isoString) {
        if (!(new TemporalParser(isoString)).isTemporalRelativeToString()) {
            throw TemporalErrors.createRangeErrorInvalidRelativeToString();
        }
        JSTemporalDateTimeRecord result = parseISODateTime(isoString, false, false);
        boolean z = false;
        TruffleString offsetString = null;
        TruffleString timeZone = null;
        if (!isoString.isEmpty()) {
            try {
                JSTemporalTimeZoneRecord timeZoneResult = parseTemporalTimeZoneString(isoString);
                z = timeZoneResult.isZ();
                offsetString = timeZoneResult.getOffsetString();
                timeZone = timeZoneResult.getName();
            } catch (Exception ex) {
                // fall-through
            }
        } // else handled with defaults above
        return JSTemporalZonedDateTimeRecord.create(result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                        result.getMicrosecond(), result.getNanosecond(), result.getCalendar(), z, offsetString, timeZone);
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalMonthDayString(TruffleString string) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseMonthDay();
        if (rec != null) {
            if (rec.getZ()) {
                throw TemporalErrors.createRangeErrorUnexpectedUTCDesignator();
            }
            if (rec.getYear() == 0 && (Strings.indexOf(string, TemporalConstants.MINUS_000000) >= 0 || Strings.indexOf(string, TemporalConstants.UNICODE_MINUS_SIGN_000000) >= 0)) {
                throw TemporalErrors.createRangeErrorInvalidPlainDateTime();
            }

            int y = rec.getYear() == Long.MIN_VALUE ? Integer.MIN_VALUE : ltoi(rec.getYear());
            int m = rec.getMonth() == Long.MIN_VALUE ? 1 : ltoi(rec.getMonth());
            int d = rec.getDay() == Long.MIN_VALUE ? 1 : ltoi(rec.getDay());

            // from ParseISODateTime
            if (!isValidISODate(y, m, d)) {
                throw TemporalErrors.createRangeErrorDateOutsideRange();
            }

            return JSTemporalDateTimeRecord.createCalendar(y, m, d, 0, 0, 0, 0, 0, 0, rec.getCalendar());
        }
        throw Errors.createRangeError("cannot parse MonthDay");
    }

    private static JSTemporalDateTimeRecord parseISODateTime(TruffleString string) {
        return parseISODateTime(string, false, false);
    }

    @TruffleBoundary
    private static JSTemporalDateTimeRecord parseISODateTime(TruffleString string, boolean failWithUTCDesignator, boolean timeExpected) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseISODateTime();
        if (rec != null) {
            if (failWithUTCDesignator && rec.getZ()) {
                throw TemporalErrors.createRangeErrorUnexpectedUTCDesignator();
            }
            if (timeExpected && (rec.getHour() == Long.MIN_VALUE)) {
                throw Errors.createRangeError("cannot parse the ISO date time string");
            }

            TruffleString fraction = rec.getFraction();
            if (fraction == null) {
                fraction = ZEROS;
            } else {
                fraction = Strings.concat(fraction, ZEROS);
            }

            if (rec.getYear() == 0 && (Strings.indexOf(string, TemporalConstants.MINUS_000000) >= 0 || Strings.indexOf(string, TemporalConstants.UNICODE_MINUS_SIGN_000000) >= 0)) {
                throw TemporalErrors.createRangeErrorInvalidPlainDateTime();
            }

            int y = rec.getYear() == Long.MIN_VALUE ? 0 : ltoi(rec.getYear());
            int m = rec.getMonth() == Long.MIN_VALUE ? 1 : ltoi(rec.getMonth());
            int d = rec.getDay() == Long.MIN_VALUE ? 1 : ltoi(rec.getDay());
            int h = rec.getHour() == Long.MIN_VALUE ? 0 : ltoi(rec.getHour());
            int min = rec.getMinute() == Long.MIN_VALUE ? 0 : ltoi(rec.getMinute());
            int s = rec.getSecond() == Long.MIN_VALUE ? 0 : ltoi(rec.getSecond());
            int ms = 0;
            int mus = 0;
            int ns = 0;
            try {
                ms = (int) Strings.parseLong(Strings.lazySubstring(fraction, 0, 3));
                mus = (int) Strings.parseLong(Strings.lazySubstring(fraction, 3, 3));
                ns = (int) Strings.parseLong(Strings.lazySubstring(fraction, 6, 3));
            } catch (TruffleString.NumberFormatException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }

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

      public static void validateTemporalUnitRange(Unit largestUnit, Unit smallestUnit) {
        boolean error = false;
        if (smallestUnit == Unit.YEAR) {
            if (!(largestUnit == Unit.YEAR)) {
                error=true;
            }
        } else if (smallestUnit == Unit.MONTH) {
            if (!(largestUnit == Unit.YEAR || largestUnit == Unit.MONTH)) {
                error = true;
            }
        } else if (smallestUnit == Unit.WEEK) {
            if (!(largestUnit == Unit.YEAR || largestUnit == Unit.MONTH || largestUnit == Unit.WEEK)) {
                error = true;
            }
        } else if (smallestUnit == Unit.DAY) {
            if (!(largestUnit == Unit.YEAR || largestUnit == Unit.MONTH || largestUnit == Unit.WEEK || largestUnit == Unit.DAY)) {
                error = true;
            }
        } else if (smallestUnit == Unit.HOUR) {
            if (!(largestUnit == Unit.YEAR || largestUnit == Unit.MONTH || largestUnit == Unit.WEEK || largestUnit == Unit.DAY || largestUnit == Unit.HOUR)) {
                error = true;
            }
        } else if (smallestUnit == Unit.MINUTE) {
            if (largestUnit == Unit.SECOND || largestUnit == Unit.MILLISECOND || largestUnit == Unit.MICROSECOND || largestUnit == Unit.NANOSECOND) {
                error = true;
            }
        } else if (smallestUnit == Unit.SECOND) {
            if (largestUnit == Unit.MILLISECOND || largestUnit == Unit.MICROSECOND || largestUnit == Unit.NANOSECOND) {
                error = true;
            }
        } else if (smallestUnit == Unit.MILLISECOND) {
            if (largestUnit == Unit.MICROSECOND || largestUnit == Unit.NANOSECOND) {
                error = true;
            }
        } else if (smallestUnit == Unit.MICROSECOND) {
            if (largestUnit == Unit.NANOSECOND) {
                error = true;
            }
        }
        if (error) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
    }

    public static Double maximumTemporalDurationRoundingIncrement(Unit unit) {
        if (unit == Unit.YEAR || unit == Unit.MONTH || unit == Unit.WEEK || unit == Unit.DAY) {
            return null; // Undefined according to spec, we fix at consumer
        }
        if (unit == Unit.HOUR) {
            return 24d;
        }
        if (unit == Unit.MINUTE || unit == Unit.SECOND) {
            return 60d;
        }
        assert unit == Unit.MILLISECOND || unit == Unit.MICROSECOND || unit == Unit.NANOSECOND;
        return 1000d;
    }

    // 13.32
    @TruffleBoundary
    public static TruffleString formatSecondsStringPart(long second, long millisecond, long microsecond, long nanosecond,
                    Object precision) {
        if (precision.equals(MINUTE)) {
            return Strings.EMPTY_STRING;
        }
        TruffleString secondString = Strings.format(":%1$02d", second);
        long fraction = (millisecond * 1_000_000) + (microsecond * 1_000) + nanosecond;
        TruffleString fractionString = Strings.EMPTY_STRING;
        if (precision.equals(AUTO)) {
            if (fraction == 0) {
                return secondString;
            }
            fractionString = Strings.concatAll(fractionString,
                            Strings.format("%1$03d", millisecond),
                            Strings.format("%1$03d", microsecond),
                            Strings.format("%1$03d", nanosecond));
            fractionString = longestSubstring(fractionString);
        } else {
            if (precision.equals(0)) {
                return secondString;
            }
            fractionString = Strings.concatAll(fractionString,
                            Strings.format("%1$03d", millisecond),
                            Strings.format("%1$03d", microsecond),
                            Strings.format("%1$03d", nanosecond));
            // no leak, because this string is concatenated immediately after
            fractionString = Strings.lazySubstring(fractionString, 0, (int) toLong(precision));
        }
        return Strings.concatAll(secondString, Strings.DOT, fractionString);
    }

    private static TruffleString longestSubstring(TruffleString str) {
        int length = Strings.length(str);
        while (length > 0 && Strings.charAt(str, length - 1) == '0') {
            length--;
        }
        if (length == 0) {
            return Strings.EMPTY_STRING;
        }
        if (length == Strings.length(str)) {
            return str;
        }
        assert Strings.length(str) <= 9;
        // leaks no more than 8 chars
        return Strings.lazySubstring(str, 0, length);
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

    // 13.35
    public static int constrainToRange(long value, int minimum, int maximum) {
        return (int) (Math.min(Math.max(value, minimum), maximum));
    }

    // 13.36
    @TruffleBoundary
    public static BigDecimal roundHalfAwayFromZero(BigDecimal x) {
        if (x.compareTo(BigDecimal.ZERO) < 0) {
            return x.setScale(0, java.math.RoundingMode.HALF_DOWN);
        } else {
            return x.setScale(0, java.math.RoundingMode.HALF_UP);
        }
    }

    @TruffleBoundary
    public static BigInteger roundNumberToIncrement(BigDecimal x, BigDecimal increment, RoundingMode roundingMode) {
        assert roundingMode == RoundingMode.CEIL || roundingMode == RoundingMode.FLOOR || roundingMode == RoundingMode.TRUNC || roundingMode == RoundingMode.HALF_EXPAND;

        // algorithm from polyfill
        BigDecimal[] divRes = x.divideAndRemainder(increment);
        BigDecimal quotient = divRes[0];
        BigDecimal remainder = divRes[1];
        int sign = remainder.signum() < 0 ? -1 : 1;

        if (roundingMode == RoundingMode.CEIL) {
            if (sign > 0) {
                quotient = quotient.add(BigDecimal.ONE);
            }
        } else if (roundingMode == RoundingMode.FLOOR) {
            if (sign < 0) {
                quotient = quotient.add(BigDecimal.valueOf(-1));
            }
        } else if (roundingMode == RoundingMode.TRUNC) {
            // divMod already is truncation
        } else {
            assert roundingMode == RoundingMode.HALF_EXPAND;
            if (remainder.multiply(BigDecimal.valueOf(2)).abs().compareTo(increment) >= 0) {
                quotient = quotient.add(BigDecimal.valueOf(sign));
            }
        }
        BigDecimal result = quotient.multiply(increment);
        return result.toBigInteger();
    }

    @TruffleBoundary
    public static double roundNumberToIncrement(double x, double increment, RoundingMode roundingMode) {
        assert roundingMode == RoundingMode.CEIL || roundingMode == RoundingMode.FLOOR || roundingMode == RoundingMode.TRUNC || roundingMode == RoundingMode.HALF_EXPAND;

        double quotient = x / increment;
        double rounded = 0;

        if (roundingMode == RoundingMode.CEIL) {
            rounded = -Math.floor(-quotient);
        } else if (roundingMode == RoundingMode.FLOOR) {
            rounded = Math.floor(quotient);
        } else if (roundingMode == RoundingMode.TRUNC) {
            if (quotient > 0) {
                rounded = Math.floor(quotient);
            } else {
                rounded = Math.ceil(quotient);
            }
        } else if (roundingMode == RoundingMode.HALF_EXPAND) {
            rounded = roundHalfAwayFromZero(quotient);
        }
        return rounded * increment;
    }

    @TruffleBoundary
    public static double roundHalfAwayFromZero(double x) {
        if (x >= 0) {
            return Math.round(x);
        } else {
            return -Math.round(-x); // further away from zero
        }
    }

    // 13.43
    @TruffleBoundary
    public static TruffleString parseTemporalCalendarString(TruffleString string) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseCalendarString();
        if (rec == null) {
            throw Errors.createRangeError("cannot parse Calendar");
        }
        TruffleString id = rec.getCalendar();
        if (isNullish(id)) {
            return ISO8601;
        }
        return id;
    }

    public static double toPositiveInteger(Object value) {
        double result = JSRuntime.doubleValue(toIntegerThrowOnInfinity(value));
        if (result <= 0) {
            throw Errors.createRangeError("positive value expected");
        }
        return result;
    }

    // like toPositiveInteger, but constrains to int values
    // used when caller would constrain/reject anyway
    public static int toPositiveIntegerConstrainInt(Object value) {
        Number result = toIntegerThrowOnInfinity(value);
        double dResult = JSRuntime.doubleValue(result);
        if (dResult <= 0) {
            throw Errors.createRangeError("positive value expected");
        }
        if (dResult > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return JSRuntime.intValue(result);
    }

    // 13.52
    @TruffleBoundary
    public static DynamicObject prepareTemporalFields(JSContext ctx, DynamicObject fields, List<TruffleString> fieldNames, List<TruffleString> requiredFields) {
        JSRealm realm = JSRealm.get(null);
        DynamicObject result = JSOrdinary.create(ctx, realm);
        for (TruffleString property : fieldNames) {
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
    public static DynamicObject preparePartialTemporalFields(JSContext ctx, DynamicObject fields, List<TruffleString> fieldNames) {
        JSRealm realm = JSRealm.get(null);
        DynamicObject result = JSOrdinary.create(ctx, realm);
        boolean any = false;
        for (TruffleString property : fieldNames) {
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

    public static JSTemporalYearMonthDayRecord regulateISOYearMonth(int year, int month, Overflow overflow) {
        assert Overflow.CONSTRAIN == overflow || Overflow.REJECT == overflow;

        if (Overflow.CONSTRAIN == overflow) {
            return constrainISOYearMonth(year, month);
        } else {
            assert Overflow.REJECT == overflow;
            if (!isValidISOMonth(month)) {
                throw Errors.createRangeError("validation of year and month failed");
            }
        }
        return JSTemporalYearMonthDayRecord.create(year, month);
    }

    private static boolean isValidISOMonth(int month) {
        return (1 <= month) && (month <= 12);
    }

    private static JSTemporalYearMonthDayRecord constrainISOYearMonth(int year, int month) {
        int monthPrepared = constrainToRange(month, 1, 12);
        return JSTemporalYearMonthDayRecord.create(ltoi(year), monthPrepared);
    }

    // 12.1.35
    // Formula: https://cs.uwaterloo.ca/~alopez-o/math-faq/node73.html
    public static long toISODayOfWeek(int year, int month, int day) {
        int m = month - 2;
        if (m == -1) {  // Jan
            m = 11;
        } else if (m == 0) { // Feb
            m = 12;
        }
        int c = Math.floorDiv(year, 100);
        int y = Math.floorMod(year, 100);
        if (m == 11 || m == 12) {
            y = y - 1;
        }
        int weekDay = Math.floorMod((day + (long) Math.floor((2.6 * m) - 0.2) - (2 * c) + y + Math.floorDiv(y, 4) + Math.floorDiv(c, 4)), 7);
        if (weekDay == 0) { // Sunday
            return 7;
        }
        return weekDay;
    }

    public static int toISODayOfYear(int year, int month, int day) {
        int days = 0;
        for (int m = 1; m < month; m++) {
            days += isoDaysInMonth(year, m);
        }
        return days + day;
    }

    public static long toISOWeekOfYear(int year, int month, int day) {
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

    public static boolean isISOLeapYear(int year) {
        return (year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0));
    }

    public static boolean isISOLeapYear(double year) {
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

    public static int isoDaysInYear(int year) {
        if (isISOLeapYear(year)) {
            return 366;
        }
        return 365;
    }

    public static int isoDaysInMonth(double year, int month) {
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

    public static int isoDaysInMonth(int year, int month) {
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

    public static JSTemporalDateTimeRecord balanceISOYearMonth(int year, int month) {
        if (year == Integer.MAX_VALUE || year == Integer.MIN_VALUE || month == Integer.MAX_VALUE || year == Integer.MIN_VALUE) {
            throw Errors.createRangeError("value out of range");
        }

        int yearPrepared = (int) (year + Math.floor((month - 1.0) / 12.0));
        int monthPrepared = (int) nonNegativeModulo(month - 1, 12) + 1;

        return JSTemporalDateTimeRecord.create(yearPrepared, monthPrepared, 0, 0, 0, 0, 0, 0, 0);
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

    // 12.1.2
    public static boolean isBuiltinCalendar(TruffleString id) {
        return id.equals(ISO8601) || id.equals(GREGORY) || id.equals(JAPANESE);
    }

    public static DynamicObject getISO8601Calendar(JSContext ctx, JSRealm realm) {
        return getBuiltinCalendar(ISO8601, ctx, realm);
    }

    public static JSTemporalCalendarObject getBuiltinCalendar(TruffleString id, JSContext ctx, JSRealm realm) {
        if (!isBuiltinCalendar(id)) {
            throw TemporalErrors.createRangeErrorCalendarNotSupported();
        }
        return (JSTemporalCalendarObject) JSTemporalCalendar.create(ctx, realm, id);
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
        TruffleString identifier = JSRuntime.toString(temporalCalendarLike);
        if (!isBuiltinCalendar(identifier)) {
            identifier = parseTemporalCalendarString(identifier);
            if (!isBuiltinCalendar(identifier)) {
                throw TemporalErrors.createRangeErrorCalendarUnknown();
            }
        }
        return JSTemporalCalendar.create(ctx, identifier);
    }

    @TruffleBoundary
    public static List<TruffleString> calendarFields(JSContext ctx, DynamicObject calendar, List<TruffleString> strings) {
        Object fields = JSObject.getMethod(calendar, TemporalConstants.FIELDS);
        if (fields == Undefined.instance) {
            return strings;
        } else {
            JSRealm realm = JSRealm.get(null);
            DynamicObject fieldsArray = JSArray.createConstant(ctx, realm, strings.toArray(new TruffleString[]{}));
            fieldsArray = toDynamicObject(JSRuntime.call(fields, calendar, new Object[]{fieldsArray}));
            return iterableToListOfTypeString(fieldsArray);
        }
    }

    @TruffleBoundary
    public static List<TruffleString> iterableToListOfTypeString(DynamicObject items) {
        IteratorRecord iter = JSRuntime.getIterator(items /* , sync */);
        List<TruffleString> values = new ArrayList<>();
        Object next = Boolean.TRUE;
        while (next != Boolean.FALSE) {
            next = JSRuntime.iteratorStep(iter);
            if (next != Boolean.FALSE) {
                Object nextValue = JSRuntime.iteratorValue((DynamicObject) next);
                if (!Strings.isTString(nextValue)) {
                    JSRuntime.iteratorClose(iter.getIterator());
                    throw Errors.createTypeError("string expected");
                }
                TruffleString str = JSRuntime.toString(nextValue);
                values.add(str);
            }
        }
        return values;
    }

    public static JSTemporalPlainDateObject dateFromFields(DynamicObject calendar, DynamicObject fields, Object options) {
        Object dateFromFields = JSObject.get(calendar, TemporalConstants.DATE_FROM_FIELDS);
        Object date = JSRuntime.call(dateFromFields, calendar, new Object[]{fields, options});
        return requireTemporalDate(date);
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalDateTimeString(TruffleString string) {
        // TODO 2. If isoString does not satisfy the syntax of a TemporalDateTimeString (see 13.39)
        JSTemporalDateTimeRecord result = parseISODateTime(string, true, false);
        return result;
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalDateString(TruffleString string) {
        // TODO 2. If isoString does not satisfy the syntax of a TemporalDateTimeString (see 13.39)
        JSTemporalDateTimeRecord result = parseISODateTime(string, true, false);
        return JSTemporalDateTimeRecord.createCalendar(result.getYear(), result.getMonth(), result.getDay(), 0, 0, 0, 0, 0, 0, result.getCalendar());
    }

    @TruffleBoundary

    public static JSTemporalDateTimeRecord parseTemporalTimeString(TruffleString string) {
        JSTemporalDateTimeRecord result = parseISODateTime(string, true, true);
        if (result.hasCalendar()) {
            return JSTemporalDateTimeRecord.createCalendar(0, 0, 0, result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(),
                            result.getCalendar());
        } else {
            return JSTemporalDateTimeRecord.create(0, 0, 0, result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
        }
    }

    @TruffleBoundary
    public static Object buildISOMonthCode(int month) {
        TruffleString numberPart = Strings.fromInt(month);
        assert 1 <= Strings.length(numberPart) && Strings.length(numberPart) <= 2;
        return Strings.concat(Strings.length(numberPart) >= 2 ? Strings.UC_M : Strings.UC_M0, numberPart);
    }

    public static TruffleString isoMonthCode(TemporalMonth date) {
        long month = date.getMonth();
        return buildISOMonthCode(month);
    }

    @TruffleBoundary
    private static TruffleString buildISOMonthCode(long month) {
        TruffleString monthCode = Strings.format("%1$02d", month);
        return Strings.concat(TemporalConstants.M, monthCode);
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
        TruffleString identifier = JSRuntime.toString(temporalTimeZoneLike);

        JSTemporalTimeZoneRecord parseResult = parseTemporalTimeZoneString(identifier);
        if (!isNullish(parseResult.getName())) {
            boolean canParse = canParseAsTimeZoneNumericUTCOffset(parseResult.getName());
            if (canParse) {
                if (!isNullish(parseResult.getOffsetString()) && parseTimeZoneOffsetString(parseResult.getOffsetString()) != parseTimeZoneOffsetString(parseResult.getName())) {
                    throw TemporalErrors.createRangeErrorInvalidTimeZoneString();
                }
            } else {
                if (!isValidTimeZoneName(parseResult.getName())) {
                    throw TemporalErrors.createRangeErrorInvalidTimeZoneString();
                }
            }
            return createTemporalTimeZone(ctx, canonicalizeTimeZoneName(parseResult.getName()));
        }
        if (parseResult.isZ()) {
            return createTemporalTimeZone(ctx, UTC);
        }
        return createTemporalTimeZone(ctx, parseResult.getOffsetString());
    }

    public static DynamicObject createTemporalTimeZone(JSContext ctx, TruffleString identifier) {
        BigInt offsetNs;
        TruffleString newIdentifier = identifier;
        try {
            long result = parseTimeZoneOffsetString(identifier);
            // no abrupt completion
            newIdentifier = formatTimeZoneOffsetString(result);
            offsetNs = BigInt.valueOf(result);
        } catch (Exception ex) {
            assert canonicalizeTimeZoneName(identifier).equals(identifier);
            offsetNs = null;
        }
        return JSTemporalTimeZone.create(ctx, offsetNs, newIdentifier);
    }

    public static TruffleString canonicalizeTimeZoneName(TruffleString timeZone) {
        assert isValidTimeZoneName(timeZone);
        return Strings.fromJavaString(JSDateTimeFormat.canonicalizeTimeZoneName(timeZone));
    }

    public static boolean isValidTimeZoneName(TruffleString timeZone) {
        return JSDateTimeFormat.canonicalizeTimeZoneName(timeZone) != null;
    }

    public static JSTemporalPlainDateTimeObject createTemporalDateTime(JSContext context, int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond,
                    int nanosecond, DynamicObject calendar) {
        return JSTemporalPlainDateTime.create(context, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, calendar);
    }

    public static JSTemporalZonedDateTimeObject createTemporalZonedDateTime(JSContext ctx, BigInt ns, DynamicObject timeZone, DynamicObject calendar) {
        return (JSTemporalZonedDateTimeObject) JSTemporalZonedDateTime.create(ctx, ns, timeZone, calendar);
    }

    @TruffleBoundary
    public static double getDouble(DynamicObject ob, TruffleString key, double defaultValue) {
        Object value = JSObject.get(ob, key);
        if (value == Undefined.instance) {
            return defaultValue;
        }
        Number n = (Number) value;
        return n.longValue();
    }

    @TruffleBoundary
    public static boolean isoDateTimeWithinLimits(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond) {
        BigInteger ns = getEpochFromISOParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
        return ns.compareTo(isoTimeLowerBound) > 0 && ns.compareTo(isoTimeUpperBound) < 0;
    }

    @TruffleBoundary
    public static BigInteger getEpochFromISOParts(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond) {
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

    public static Overflow toTemporalOverflow(DynamicObject options, TemporalGetOptionNode getOptionNode) {
        TruffleString result = (TruffleString) getOptionNode.execute(options, OVERFLOW, OptionType.STRING, listConstrainReject, CONSTRAIN);
        return toOverflow(result);
    }

    public static Overflow toTemporalOverflow(DynamicObject options) {
        TruffleString result = (TruffleString) getOption(options, OVERFLOW, OptionType.STRING, listConstrainReject, CONSTRAIN);
        return toOverflow(result);
    }

    @TruffleBoundary
    public static Overflow toOverflow(TruffleString result) {
        if (CONSTRAIN.equals(result)) {
            return Overflow.CONSTRAIN;
        } else if (TemporalConstants.REJECT.equals(result)) {
            return Overflow.REJECT;
        }
        CompilerDirectives.transferToInterpreter();
        throw Errors.shouldNotReachHere("unknown overflow type: " + result);
    }

    public static JSTemporalDateTimeRecord interpretTemporalDateTimeFields(DynamicObject calendar, DynamicObject fields, DynamicObject options) {
        JSTemporalDateTimeRecord timeResult = toTemporalTimeRecord(fields);
        JSTemporalPlainDateObject date = dateFromFields(calendar, fields, options);
        Overflow overflow = toTemporalOverflow(options);
        JSTemporalDurationRecord timeResult2 = TemporalUtil.regulateTime(
                        timeResult.getHour(), timeResult.getMinute(), timeResult.getSecond(), timeResult.getMillisecond(), timeResult.getMicrosecond(), timeResult.getNanosecond(),
                        overflow);
        return JSTemporalDateTimeRecord.create(
                        date.getYear(), date.getMonth(), date.getDay(),
                        dtoi(timeResult2.getHours()), dtoi(timeResult2.getMinutes()), dtoi(timeResult2.getSeconds()),
                        dtoi(timeResult2.getMilliseconds()), dtoi(timeResult2.getMicroseconds()), dtoi(timeResult2.getNanoseconds()));
    }

    public static JSTemporalDurationRecord regulateTime(double hours, double minutes, double seconds, double milliseconds, double microseconds,
                    double nanoseconds, Overflow overflow) {
        assert overflow == Overflow.CONSTRAIN || overflow == Overflow.REJECT;
        if (overflow == Overflow.CONSTRAIN) {
            return constrainTime(dtoi(hours), dtoi(minutes), dtoi(seconds), dtoi(milliseconds), dtoi(microseconds), dtoi(nanoseconds));
        } else {
            if (!TemporalUtil.isValidTime(dtoi(hours), dtoi(minutes), dtoi(seconds), dtoi(milliseconds), dtoi(microseconds), dtoi(nanoseconds))) {
                throw Errors.createRangeError("Given time outside the range.");
            }
            return JSTemporalDurationRecord.create(0, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
    }

    public static JSTemporalDurationRecord regulateTime(int hours, int minutes, int seconds, int milliseconds, int microseconds,
                    int nanoseconds, Overflow overflow) {
        assert overflow == Overflow.CONSTRAIN || overflow == Overflow.REJECT;
        if (overflow == Overflow.CONSTRAIN) {
            return constrainTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        } else {
            if (!TemporalUtil.isValidTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
                throw Errors.createRangeError("Given time outside the range.");
            }
            return JSTemporalDurationRecord.create(0, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
    }

    public static JSTemporalDurationRecord constrainTime(int hours, int minutes, int seconds, int milliseconds,
                    int microseconds, int nanoseconds) {
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

        int hour = 0;
        int minute = 0;
        int second = 0;
        int millisecond = 0;
        int microsecond = 0;
        int nanosecond = 0;

        for (TruffleString property : TemporalUtil.TIME_LIKE_PROPERTIES) {
            Object val = JSObject.get(temporalTimeLike, property);

            int iVal = 0;
            if (val == Undefined.instance) {
                iVal = 0;
            } else {
                any = true;
                iVal = JSRuntime.intValue(TemporalUtil.toIntegerThrowOnInfinity(val));
            }
            if (HOUR.equals(property)) {
                hour = iVal;
            } else if (MINUTE.equals(property)) {
                minute = iVal;
            } else if (SECOND.equals(property)) {
                second = iVal;
            } else if (MILLISECOND.equals(property)) {
                millisecond = iVal;
            } else if (MICROSECOND.equals(property)) {
                microsecond = iVal;
            } else if (NANOSECOND.equals(property)) {
                nanosecond = iVal;
            }
        }
        if (!any) {
            throw Errors.createTypeError("at least one time-like field expected");
        }
        return JSTemporalDateTimeRecord.create(0, 0, 0, hour, minute, second, millisecond, microsecond, nanosecond);
    }

    public static Number toIntegerThrowOnInfinity(Object value) {
        Number integer = toIntegerOrInfinity(value);
        if (Double.isInfinite(JSRuntime.doubleValue(integer))) {
            throw Errors.createRangeError("value outside bounds");
        }
        return integer;
    }

    public static double toIntegerWithoutRounding(Object argument) {
        Number number = JSRuntime.toNumber(argument);
        double dNumber = JSRuntime.doubleValue(number);
        if (Double.isNaN(dNumber) || dNumber == 0.0d) {
            return 0.0;
        }
        if (!JSRuntime.isIntegralNumber(dNumber)) {
            throw Errors.createRangeError("value expected to be integer");
        }
        return dNumber;
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
        return number;
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
    public static BigInteger roundTemporalInstant(BigInt ns, double increment, Unit unit, RoundingMode roundingMode) {
        return roundTemporalInstant(new BigDecimal(ns.bigIntegerValue()), increment, unit, roundingMode);
    }

    @TruffleBoundary
    public static BigInteger roundTemporalInstant(BigDecimal ns, double increment, Unit unit, RoundingMode roundingMode) {
        BigDecimal incrementNs = BigDecimal.valueOf(increment);
        if (Unit.HOUR == unit) {
            incrementNs = incrementNs.multiply(BigDecimal.valueOf(3_600_000_000_000L));
        } else if (Unit.MINUTE == unit) {
            incrementNs = incrementNs.multiply(BigDecimal.valueOf(60_000_000_000L));
        } else if (Unit.SECOND == unit) {
            incrementNs = incrementNs.multiply(BigDecimal.valueOf(1_000_000_000L));
        } else if (Unit.MILLISECOND == unit) {
            incrementNs = incrementNs.multiply(BigDecimal.valueOf(1_000_000L));
        } else if (Unit.MICROSECOND == unit) {
            incrementNs = incrementNs.multiply(BigDecimal.valueOf(1_000L));
        } else {
            assert Unit.NANOSECOND == unit;

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
            List<TruffleString> fieldNames = calendarFields(ctx, calendar, listDMMCY);
            DynamicObject fields = prepareTemporalFields(ctx, item, fieldNames, listEmpty);
            return dateFromFields(calendar, fields, options);
        }
        Overflow overflows = toTemporalOverflow(options);
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
    public static JSTemporalPlainTimeObject toTemporalTime(Object item, Overflow overflowParam, JSContext ctx, JSRealm realm, IsObjectNode isObject, JSToStringNode toStringNode) {
        Overflow overflow = overflowParam == null ? Overflow.CONSTRAIN : overflowParam;
        assert overflow == Overflow.CONSTRAIN || overflow == Overflow.REJECT;
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
            TruffleString string = toStringNode.executeString(item);
            JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalTimeString(string);
            assert TemporalUtil.isValidTime(
                            result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
            if (result.hasCalendar() && !JSRuntime.toString(result.getCalendar()).equals(TemporalConstants.ISO8601)) {
                throw TemporalErrors.createRangeErrorTemporalISO8601Expected();
            }
            result2 = JSTemporalDurationRecord.create(result);
        }
        return (JSTemporalPlainTimeObject) JSTemporalPlainTime.create(ctx, dtoi(result2.getHours()), dtoi(result2.getMinutes()), dtoi(result2.getSeconds()), dtoi(result2.getMilliseconds()),
                        dtoi(result2.getMicroseconds()), dtoi(result2.getNanoseconds()));
    }

    public static boolean validateISODate(int year, int month, int day) {
        if (month < 1 || month > 12) {
            return false;
        }
        long daysInMonth = isoDaysInMonth(year, month);
        if (day < 1 || day > daysInMonth) {
            return false;
        }
        return true;
    }

    // 3.5.7
    @TruffleBoundary
    public static JSTemporalDateTimeRecord regulateISODate(int yearParam, int monthParam, int dayParam, Overflow overflow) {
        assert overflow == Overflow.CONSTRAIN || overflow == Overflow.REJECT;
        int year = yearParam;
        int month = monthParam;
        int day = dayParam;
        if (overflow == Overflow.REJECT) {
            if (!isValidISODate(year, month, day)) {
                throw TemporalErrors.createRangeErrorDateOutsideRange();
            }
            return JSTemporalDateTimeRecord.create(year, month, day, 0, 0, 0, 0, 0, 0);
        }
        if (overflow == Overflow.CONSTRAIN) {
            month = constrainToRange(month, 1, 12);
            day = constrainToRange(day, 1, isoDaysInMonth(year, month));
            return JSTemporalDateTimeRecord.create(year, month, day, 0, 0, 0, 0, 0, 0);
        }
        throw new RuntimeException("This should never have happened.");
    }

    // input values always in int range
    public static JSTemporalDateTimeRecord balanceISODate(int yearParam, int monthParam, int dayParam) {
        JSTemporalDateTimeRecord balancedYearMonth = balanceISOYearMonth(yearParam, monthParam);
        int month = balancedYearMonth.getMonth();
        int year = balancedYearMonth.getYear();
        int day = dayParam;
        int testYear;
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

    @TruffleBoundary
    // AddISODate only called with int range values, or constrained immediately afterwards
    public static JSTemporalDateTimeRecord addISODate(int year, int month, int day, int years, int months, int weeks, int daysP, Overflow overflow) {
        assert overflow == Overflow.CONSTRAIN || overflow == Overflow.REJECT;

        int days = daysP;
        JSTemporalDateTimeRecord intermediate = balanceISOYearMonth(add(year, years, overflow), add(month, months, overflow));
        intermediate = regulateISODate(intermediate.getYear(), intermediate.getMonth(), day, overflow);
        days = days + 7 * weeks;
        int d = add(intermediate.getDay(), days, overflow);
        intermediate = balanceISODate(intermediate.getYear(), intermediate.getMonth(), d);
        return regulateISODate(intermediate.getYear(), intermediate.getMonth(), intermediate.getDay(), overflow);
    }

    // 3.5.15
    public static int compareISODate(int y1, int m1, int d1, int y2, int m2, int d2) {
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

    public static ShowCalendar toShowCalendarOption(DynamicObject options, TemporalGetOptionNode getOptionNode) {
        return toShowCalendar((TruffleString) getOptionNode.execute(options, CALENDAR_NAME, OptionType.STRING, listAutoAlwaysNever, AUTO));
    }

    @TruffleBoundary
    public static TruffleString padISOYear(int year) {
        if (999 < year && year < 9999) {
            return Strings.fromLong(year);
        }
        TruffleString sign = year >= 0 ? Strings.SYMBOL_PLUS : Strings.SYMBOL_MINUS;
        long y = Math.abs(year);
        return Strings.concat(sign, Strings.format("%1$06d", y));
    }

    @TruffleBoundary
    public static TruffleString formatCalendarAnnotation(TruffleString id, ShowCalendar showCalendar) {
        if (ShowCalendar.NEVER == showCalendar) {
            return Strings.EMPTY_STRING;
        } else if (ShowCalendar.AUTO == showCalendar && ISO8601.equals(id)) {
            return Strings.EMPTY_STRING;
        } else {
            return Strings.concatAll(BRACKET_U_CA_EQUALS, id, Strings.BRACKET_CLOSE);
        }
    }

    // used in cases where (Java) null is used even though Undefined is meant.
    public static boolean isNullish(Object obj) {
        return obj == null || obj == Undefined.instance;
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

            List<TruffleString> fieldNames = TemporalUtil.calendarFields(ctx, calendar, listMMCY);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(ctx, itemObj, fieldNames, listEmpty);
            return yearMonthFromFields(calendar, fields, options);
        }
        TemporalUtil.toTemporalOverflow(options);

        TruffleString string = JSRuntime.toString(item);
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
        Object fn = JSObject.getMethod(calendar, YEAR_MONTH_FROM_FIELDS);
        Object yearMonth = JSRuntime.call(fn, calendar, new Object[]{fields, options});
        return requireTemporalYearMonth(yearMonth);
    }

    public static DynamicObject monthDayFromFields(DynamicObject calendar, DynamicObject fields, DynamicObject options) {
        Object fn = JSObject.getMethod(calendar, MONTH_DAY_FROM_FIELDS);
        Object monthDay = JSRuntime.call(fn, calendar, new Object[]{fields, options});
        return TemporalUtil.requireTemporalMonthDay(monthDay);
    }

    public static RoundingMode negateTemporalRoundingMode(RoundingMode roundingMode) {
        if (RoundingMode.CEIL == roundingMode) {
            return RoundingMode.FLOOR;
        } else if (RoundingMode.FLOOR == roundingMode) {
            return RoundingMode.CEIL;
        }
        return roundingMode;
    }

    public static boolean calendarEquals(DynamicObject one, DynamicObject two, JSToStringNode toStringNode) {
        if (one == two) {
            return true;
        }
        if (Boundaries.equals(toStringNode.executeString(one), toStringNode.executeString(two))) {
            return true;
        }
        return false;
    }

    public static Object dayOfWeek(DynamicObject calendar, DynamicObject dt) {
        return calendarDayOfWeek(calendar, dt);
    }

    public static Object dayOfYear(DynamicObject calendar, DynamicObject dt) {
        return calendarDayOfYear(calendar, dt);
    }

    public static void rejectTemporalCalendarType(DynamicObject obj, BranchProfile errorBranch) {
        if (obj instanceof JSTemporalPlainDateObject || obj instanceof JSTemporalPlainDateTimeObject || obj instanceof JSTemporalPlainMonthDayObject ||
                        obj instanceof JSTemporalPlainTimeObject || obj instanceof JSTemporalPlainYearMonthObject || isTemporalZonedDateTime(obj)) {
            errorBranch.enter();
            throw Errors.createTypeError("rejecting calendar types");
        }
    }

    public static JSTemporalPlainTimeObject createTemporalTime(JSContext ctx, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond) {
        return (JSTemporalPlainTimeObject) JSTemporalPlainTime.create(ctx, hour, minute, second, millisecond, microsecond, nanosecond);
    }

    public static JSTemporalPlainDateObject createTemporalDate(JSContext context, int year, int month, int day, DynamicObject calendar) {
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

    @TruffleBoundary
    public static double getPropertyFromRecord(JSTemporalDurationRecord d, TruffleString property) {
        if (YEARS.equals(property)) {
            return d.getYears();
        } else if (MONTHS.equals(property)) {
            return d.getMonths();
        } else if (WEEKS.equals(property)) {
            return d.getWeeks();
        } else if (DAYS.equals(property)) {
            return d.getDays();
        } else if (HOURS.equals(property)) {
            return d.getHours();
        } else if (MINUTES.equals(property)) {
            return d.getMinutes();
        } else if (SECONDS.equals(property)) {
            return d.getSeconds();
        } else if (MILLISECONDS.equals(property)) {
            return d.getMilliseconds();
        } else if (MICROSECONDS.equals(property)) {
            return d.getMicroseconds();
        } else if (NANOSECONDS.equals(property)) {
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
                    // TODO: is JSRuntime.toString correct here?
                    createDataPropertyOrThrow(ctx, merged, JSRuntime.toString(nextKey), propValue);
                }
            }
        }
        boolean hasMonthOrMonthCode = false;
        UnmodifiableArrayList<? extends Object> newKeys = namesNode.execute(additionalFields);
        for (Object nextKey : newKeys) {
            Object propValue = JSObject.get(additionalFields, nextKey);
            if (propValue != Undefined.instance) {
                // TODO: is JSRuntime.toString correct here?
                createDataPropertyOrThrow(ctx, merged, JSRuntime.toString(nextKey), propValue);
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

    public static void createDataPropertyOrThrow(JSContext ctx, DynamicObject obj, TruffleString key, Object value) {
        JSObjectUtil.defineDataProperty(ctx, obj, key, value, JSAttributes.configurableEnumerableWritable());
    }

    @TruffleBoundary
    public static List<TruffleString> listJoinRemoveDuplicates(List<TruffleString> first, List<TruffleString> second) {
        List<TruffleString> newList = new ArrayList<>(first.size() + second.size());
        newList.addAll(first);
        for (TruffleString elem : second) {
            if (!first.contains(elem)) {
                newList.add(elem);
            }
        }
        return newList;
    }

    public static DynamicObject toTemporalCalendarWithISODefault(JSContext ctx, JSRealm realm, Object calendar) {
        if (isNullish(calendar)) {
            return getISO8601Calendar(ctx, realm);
        } else {
            return toTemporalCalendar(ctx, calendar);
        }
    }

    public static Unit largerOfTwoTemporalUnits(Unit a, Unit b) {
        if (Unit.YEAR == a || Unit.YEAR == b) {
            return Unit.YEAR;
        }
        if (Unit.MONTH == a || Unit.MONTH == b) {
            return Unit.MONTH;
        }
        if (Unit.WEEK == a || Unit.WEEK == b) {
            return Unit.WEEK;
        }
        if (Unit.DAY == a || Unit.DAY == b) {
            return Unit.DAY;
        }
        if (Unit.HOUR == a || Unit.HOUR == b) {
            return Unit.HOUR;
        }
        if (Unit.MINUTE == a || Unit.MINUTE == b) {
            return Unit.MINUTE;
        }
        if (Unit.SECOND == a || Unit.SECOND == b) {
            return Unit.SECOND;
        }
        if (Unit.MILLISECOND == a || Unit.MILLISECOND == b) {
            return Unit.MILLISECOND;
        }
        if (Unit.MICROSECOND == a || Unit.MICROSECOND == b) {
            return Unit.MICROSECOND;
        }
        return Unit.NANOSECOND;
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
            List<TruffleString> fieldNames = TemporalUtil.calendarFields(ctx, calendar, listDHMMMMMNSY);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(ctx, itemObj, fieldNames, listEmpty);
            result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, options);
        } else {
            TemporalUtil.toTemporalOverflow(options);
            TruffleString string = JSRuntime.toString(item);
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
    public static JSTemporalDurationRecord differenceISODateTime(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, int y1, int mon1, int d1, int h1, int min1, int s1,
                    int ms1, int mus1, int ns1, int y2, int mon2, int d2, int h2, int min2, int s2, int ms2,
                    int mus2, int ns2, DynamicObject calendar, Unit largestUnit, DynamicObject optionsParam) {
        DynamicObject options = optionsParam;
        if (isNullish(optionsParam)) {
            options = JSOrdinary.createWithNullPrototypeInit(ctx);
        }
        JSTemporalDurationRecord timeDifference = differenceTime(h1, min1, s1, ms1, mus1, ns1, h2, min2, s2, ms2, mus2, ns2);

        int timeSign = durationSign(0, 0, 0, timeDifference.getDays(), timeDifference.getHours(), timeDifference.getMinutes(), timeDifference.getSeconds(),
                        timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds());
        int dateSign = compareISODate(y2, mon2, d2, y1, mon1, d1);
        JSTemporalDateTimeRecord balanceResult = balanceISODate(dtoi(y1), dtoi(mon1), dtoi(d1) + dtoi(timeDifference.getDays()));
        if (timeSign == -dateSign) {
            balanceResult = balanceISODate(balanceResult.getYear(), balanceResult.getMonth(), balanceResult.getDay() - timeSign);
            timeDifference = balanceDuration(ctx, namesNode, -timeSign, timeDifference.getHours(),
                            timeDifference.getMinutes(), timeDifference.getSeconds(), timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds(), largestUnit);
        }
        DynamicObject date1 = createTemporalDate(ctx, balanceResult.getYear(), balanceResult.getMonth(), balanceResult.getDay(), calendar);
        DynamicObject date2 = createTemporalDate(ctx, y2, mon2, d2, calendar);
        Unit dateLargestUnit = largerOfTwoTemporalUnits(Unit.DAY, largestUnit);
        DynamicObject untilOptions = mergeLargestUnitOption(ctx, namesNode, options, dateLargestUnit);
        JSTemporalDurationObject dateDifference = calendarDateUntil(calendar, date1, date2, untilOptions, Undefined.instance);
        JSTemporalDurationRecord result = balanceDuration(ctx, namesNode, dateDifference.getDays(), timeDifference.getHours(), timeDifference.getMinutes(), timeDifference.getSeconds(),
                        timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds(), largestUnit);
        return JSTemporalDurationRecord.createWeeks(dateDifference.getYears(), dateDifference.getMonths(), dateDifference.getWeeks(), result.getDays(), result.getHours(), result.getMinutes(),
                        result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds());
    }

    @TruffleBoundary
    public static DynamicObject mergeLargestUnitOption(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, DynamicObject options, Unit largestUnit) {
        JSRealm realm = JSRealm.get(null);
        DynamicObject merged = JSOrdinary.create(ctx, realm);
        UnmodifiableArrayList<?> keys = namesNode.execute(options);
        for (Object nextKey : keys) {
            // TODO: is JSRuntime.toString correct here?
            TruffleString key = JSRuntime.toString(nextKey);
            Object propValue = JSObject.get(options, key);
            createDataPropertyOrThrow(ctx, merged, key, propValue);
        }
        createDataPropertyOrThrow(ctx, merged, LARGEST_UNIT, largestUnit.toTruffleString());
        return merged;
    }

    // 7.5.3
    public static int durationSign(double years, double months, double weeks, double days, double hours, double minutes,
                    double seconds, double milliseconds, double microseconds, double nanoseconds) {
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
    public static void rejectDurationSign(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds,
                    double nanoseconds) {
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

    @TruffleBoundary
    public static JSTemporalDurationRecord balanceDuration(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, double days, double hours, double minutes, double seconds, double milliseconds,
                    double microseconds, double nanoseconds, Unit largestUnit) {
        return balanceDuration(ctx, namesNode, days, hours, minutes, seconds, milliseconds, microseconds, BigInteger.valueOf(dtol(nanoseconds)), largestUnit, Undefined.instance);
    }

    // nanoseconds can exceed double range, see TemporalDurationHugeTest.testInstantSince
    @TruffleBoundary
    public static JSTemporalDurationRecord balanceDuration(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, double days, double hours, double minutes, double seconds, double milliseconds,
                    double microseconds, BigInteger nanoseconds, Unit largestUnit, DynamicObject relativeTo) {
        BigInt nsBi;
        if (TemporalUtil.isTemporalZonedDateTime(relativeTo)) {
            JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) relativeTo;
            // conversion set to fail. addZonedDateTime creates PlainDateTime that would fail anyway
            BigInt endNs = TemporalUtil.addZonedDateTime(ctx, zdt.getNanoseconds(), zdt.getTimeZone(), zdt.getCalendar(), 0, 0, 0, dtol(days, true), dtol(hours, true), dtol(minutes, true),
                            dtol(seconds, true), dtol(milliseconds, true), dtol(microseconds, true), nanoseconds, Undefined.instance);
            nsBi = endNs.subtract(zdt.getNanoseconds());
        } else {
            nsBi = new BigInt(totalDurationNanoseconds(days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds));
        }
        double d;
        if (largestUnit == Unit.YEAR || largestUnit == Unit.MONTH || largestUnit == Unit.WEEK || largestUnit == Unit.DAY) {
            JSTemporalNanosecondsDaysRecord result = nanosecondsToDays(ctx, namesNode, nsBi, relativeTo);
            d = bitod(result.getDays());
            nsBi = new BigInt(result.getNanoseconds());
        } else {
            d = 0;
        }
        double h = 0;
        double min = 0;
        double s = 0;
        double ms = 0;
        double mus = 0;
        // from now on, `ns` is a mathematical value in the spec
        BigInteger nsBi2 = nsBi.bigIntegerValue();
        double sign = nsBi2.compareTo(BigInteger.ZERO) < 0 ? -1 : 1;
        nsBi2 = nsBi2.abs();
        if (largestUnit == Unit.YEAR || largestUnit == Unit.MONTH || largestUnit == Unit.WEEK ||
                        largestUnit == Unit.DAY || largestUnit == Unit.HOUR) {
            BigInteger[] res = nsBi2.divideAndRemainder(bi_1000);
            mus = bitod(res[0]);
            nsBi2 = res[1];
            ms = Math.floor(mus / 1000.0);
            mus = mus % 1000;
            s = Math.floor(ms / 1000.0);
            ms = ms % 1000;
            min = Math.floor(s / 60.0);
            s = s % 60;
            h = Math.floor(min / 60.0);
            min = min % 60;
        } else if (largestUnit == Unit.MINUTE) {
            BigInteger[] res = nsBi2.divideAndRemainder(bi_1000);
            mus = bitod(res[0]);
            nsBi2 = res[1];
            ms = Math.floor(mus / 1000.0);
            mus = mus % 1000;
            s = Math.floor(ms / 1000.0);
            ms = ms % 1000;
            min = Math.floor(s / 60.0);
            s = s % 60;
        } else if (largestUnit == Unit.SECOND) {
            BigInteger[] res = nsBi2.divideAndRemainder(bi_1000);
            mus = bitod(res[0]);
            nsBi2 = res[1];
            ms = Math.floor(mus / 1000.0);
            mus = mus % 1000;
            s = Math.floor(ms / 1000.0);
            ms = ms % 1000;
        } else if (largestUnit == Unit.MILLISECOND) {
            BigInteger[] res = nsBi2.divideAndRemainder(bi_1000);
            mus = bitod(res[0]);
            nsBi2 = res[1];
            ms = Math.floor(mus / 1000.0);
            mus = mus % 1000;
        } else if (largestUnit == Unit.MICROSECOND) {
            BigInteger[] res = nsBi2.divideAndRemainder(bi_1000);
            mus = bitod(res[0]);
            nsBi2 = res[1];
        } else {
            assert largestUnit == Unit.NANOSECOND;
        }

        return JSTemporalDurationRecord.create(0, 0, d, h * sign, min * sign, s * sign, ms * sign, mus * sign, sign < 0 ? bitod(nsBi2.negate()) : bitod(nsBi2));
    }

    // TODO still using (some) long arithmetics here, should use double?
    // 7.5.14
    @TruffleBoundary
    public static JSTemporalDurationRecord unbalanceDurationRelative(JSContext ctx, JSRealm realm, double y, double m, double w,
                    double d, Unit largestUnit, DynamicObject relTo) {
        long years = dtol(y);
        long months = dtol(m);
        long weeks = dtol(w);
        long days = dtol(d);

        DynamicObject relativeTo = relTo;
        if (Unit.YEAR == largestUnit || (years == 0 && months == 0 && weeks == 0 && days == 0)) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        }
        long sign = durationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        assert sign != 0;
        DynamicObject oneYear = JSTemporalDuration.createTemporalDuration(ctx, sign, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        DynamicObject oneMonth = JSTemporalDuration.createTemporalDuration(ctx, 0, sign, 0, 0, 0, 0, 0, 0, 0, 0);
        DynamicObject oneWeek = JSTemporalDuration.createTemporalDuration(ctx, 0, 0, sign, 0, 0, 0, 0, 0, 0, 0);
        DynamicObject calendar = Undefined.instance;
        if (relativeTo != Undefined.instance) {
            relativeTo = toTemporalDate(ctx, realm, relativeTo, Undefined.instance);
            calendar = toDynamicObject(JSObject.get(relativeTo, CALENDAR));
        }
        if (Unit.MONTH == largestUnit) {
            if (calendar == Undefined.instance) {
                throw Errors.createRangeError("No calendar provided.");
            }
            Object dateAdd = JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            Object dateUntil = JSObject.getMethod(calendar, TemporalConstants.DATE_UNTIL);

            while (Math.abs(years) > 0) {
                DynamicObject addOptions = JSOrdinary.createWithNullPrototype(ctx);
                DynamicObject newRelativeTo = TemporalUtil.calendarDateAdd(calendar, relativeTo, oneYear, addOptions, dateAdd);

                DynamicObject untilOptions = JSOrdinary.createWithNullPrototype(ctx);
                JSObjectUtil.putDataProperty(ctx, untilOptions, LARGEST_UNIT, MONTH);
                JSTemporalDurationObject untilResult = TemporalUtil.calendarDateUntil(calendar, relativeTo, newRelativeTo, untilOptions, dateUntil);
                long oneYearMonths = dtol(untilResult.getMonths());
                relativeTo = newRelativeTo;
                years = years - sign;
                months = months + oneYearMonths;
            }
        } else if (Unit.WEEK == largestUnit) {
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

    // TODO still using (some) long arithmetics here, should use double?
    // 7.5.15
    @TruffleBoundary
    public static JSTemporalDurationRecord balanceDurationRelative(JSContext ctx, JSRealm realm, double y, double m, double w, double d, Unit largestUnit, DynamicObject relativeToParam) {
        long years = dtol(y);
        long months = dtol(m);
        long weeks = dtol(w);
        long days = dtol(d);

        if ((largestUnit != Unit.YEAR && largestUnit != Unit.MONTH && largestUnit != Unit.WEEK) || (years == 0 && months == 0 && weeks == 0 && days == 0)) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        }
        long sign = durationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        assert sign != 0;
        DynamicObject oneYear = JSTemporalDuration.createTemporalDuration(ctx, sign, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        DynamicObject oneMonth = JSTemporalDuration.createTemporalDuration(ctx, 0, sign, 0, 0, 0, 0, 0, 0, 0, 0);
        DynamicObject oneWeek = JSTemporalDuration.createTemporalDuration(ctx, 0, 0, sign, 0, 0, 0, 0, 0, 0, 0);
        DynamicObject relativeTo = toTemporalDate(ctx, realm, relativeToParam, Undefined.instance);
        assert JSObject.hasProperty(relativeTo, CALENDAR);
        DynamicObject calendar = toDynamicObject(JSObject.get(relativeTo, CALENDAR));
        if (largestUnit == Unit.YEAR) {
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

            long oneYearMonths = dtol(untilResult.getMonths());
            while (Math.abs(months) >= Math.abs((oneYearMonths))) {
                months = months - oneYearMonths;
                years = years + sign;
                relativeTo = newRelativeTo;

                options = JSOrdinary.createWithNullPrototype(ctx);
                newRelativeTo = TemporalUtil.calendarDateAdd(calendar, relativeTo, oneYear, options, dateAdd);
                options = JSOrdinary.createWithNullPrototype(ctx);
                JSObjectUtil.putDataProperty(ctx, options, TemporalConstants.LARGEST_UNIT, MONTH);
                untilResult = TemporalUtil.calendarDateUntil(calendar, relativeTo, newRelativeTo, options, dateUntil);
                oneYearMonths = dtol(untilResult.getMonths());
            }
        } else if (largestUnit == Unit.MONTH) {
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
            assert largestUnit == Unit.WEEK;
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

    private static boolean doubleIsInteger(double l) {
        return Math.rint(l) == l;
    }

    // 7.5.16
    public static JSTemporalDurationRecord addDuration(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, double y1, double mon1, double w1, double d1, double h1, double min1, double s1,
                    double ms1, double mus1,
                    double ns1, double y2, double mon2, double w2, double d2, double h2, double min2, double s2, double ms2, double mus2, double ns2, DynamicObject relativeTo) {
        assert doubleIsInteger(y1) && doubleIsInteger(mon1) && doubleIsInteger(w1) && doubleIsInteger(d1);
        assert doubleIsInteger(h1) && doubleIsInteger(min1) && doubleIsInteger(s1) && doubleIsInteger(ms1) && doubleIsInteger(mus1) && doubleIsInteger(ns1);
        assert doubleIsInteger(y2) && doubleIsInteger(mon2) && doubleIsInteger(w2) && doubleIsInteger(d2);
        assert doubleIsInteger(h2) && doubleIsInteger(min2) && doubleIsInteger(s2) && doubleIsInteger(ms2) && doubleIsInteger(mus2) && doubleIsInteger(ns2);

        Unit largestUnit1 = defaultTemporalLargestUnit(y1, mon1, w1, d1, h1, min1, s1, ms1, mus1);
        Unit largestUnit2 = defaultTemporalLargestUnit(y2, mon2, w2, d2, h2, min2, s2, ms2, mus2);
        Unit largestUnit = TemporalUtil.largerOfTwoTemporalUnits(largestUnit1, largestUnit2);
        double years = 0;
        double months = 0;
        double weeks = 0;
        double days = 0;
        double hours = 0;
        double minutes = 0;
        double seconds = 0;
        double milliseconds = 0;
        double microseconds = 0;
        double nanoseconds = 0;
        if (relativeTo == Undefined.instance) {
            if (largestUnit == Unit.YEAR || largestUnit == Unit.MONTH || largestUnit == Unit.WEEK) {
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
            DynamicObject dateDuration1 = JSTemporalDuration.createTemporalDuration(ctx, y1, mon1, w1, d1, 0, 0, 0, 0, 0, 0);
            DynamicObject dateDuration2 = JSTemporalDuration.createTemporalDuration(ctx, y2, mon2, w2, d2, 0, 0, 0, 0, 0, 0);

            Object dateAdd = JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject firstAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject intermediate = TemporalUtil.calendarDateAdd(calendar, date, dateDuration1, firstAddOptions, dateAdd);

            DynamicObject secondAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject end = TemporalUtil.calendarDateAdd(calendar, intermediate, dateDuration2, secondAddOptions, dateAdd);

            Unit dateLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(Unit.DAY, largestUnit);

            DynamicObject differenceOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(ctx, differenceOptions, LARGEST_UNIT, dateLargestUnit.toTruffleString());
            JSTemporalDurationObject dateDifference = TemporalUtil.calendarDateUntil(calendar, date, end, differenceOptions, Undefined.instance);
            JSTemporalDurationRecord result = balanceDuration(ctx, namesNode, dtol(dateDifference.getDays()),
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
            BigInt intermediateNs = addZonedDateTime(ctx, zdt.getNanoseconds(), timeZone, calendar, dtol(y1), dtol(mon1), dtol(w1), dtol(d1), dtol(h1), dtol(min1), dtol(s1), dtol(ms1), dtol(mus1),
                            dtol(ns1));
            BigInt endNs = addZonedDateTime(ctx, intermediateNs, timeZone, calendar, dtol(y2), dtol(mon2), dtol(w2), dtol(d2), dtol(h2), dtol(min2), dtol(s2), dtol(ms2), dtol(mus2), dtol(ns2));
            if (Unit.YEAR != largestUnit && Unit.MONTH != largestUnit && Unit.WEEK != largestUnit && Unit.DAY != largestUnit) {
                long diffNs = bitol(differenceInstant(zdt.getNanoseconds(), endNs, 1d, Unit.NANOSECOND, RoundingMode.HALF_EXPAND));
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
                    Unit largestUnit) {
        return differenceZonedDateTime(ctx, namesNode, ns1, ns2, timeZone, calendar, largestUnit, Undefined.instance);
    }

    public static JSTemporalDurationRecord differenceZonedDateTime(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, BigInt ns1, BigInt ns2, DynamicObject timeZone, DynamicObject calendar,
                    Unit largestUnit, DynamicObject options) {
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
        BigInt intermediateNs = addZonedDateTime(ctx, ns1, timeZone, calendar, dtol(dateDifference.getYears()), dtol(dateDifference.getMonths()), dtol(dateDifference.getWeeks()), 0, 0, 0, 0, 0, 0, 0);
        BigInt timeRemainderNs = ns2.subtract(intermediateNs);
        DynamicObject intermediate = createTemporalZonedDateTime(ctx, intermediateNs, timeZone, calendar);
        JSTemporalNanosecondsDaysRecord result = nanosecondsToDays(ctx, namesNode, timeRemainderNs, intermediate);
        JSTemporalDurationRecord timeDifference = balanceDuration(ctx, namesNode, 0, 0, 0, 0, 0, 0, result.getNanoseconds(), Unit.HOUR, Undefined.instance);
        return JSTemporalDurationRecord.createWeeks(dateDifference.getYears(), dateDifference.getMonths(), dateDifference.getWeeks(), bitod(result.getDays()), timeDifference.getHours(),
                        timeDifference.getMinutes(), timeDifference.getSeconds(), timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds());
    }

    // 7.5.5
    public static boolean validateTemporalDuration(double years, double months, double weeks, double days, double hours,
                    double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds) {
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
    public static Unit defaultTemporalLargestUnit(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds,
                    double microseconds) {
        if (years != 0) {
            return Unit.YEAR;
        }
        if (months != 0) {
            return Unit.MONTH;
        }
        if (weeks != 0) {
            return Unit.WEEK;
        }
        if (days != 0) {
            return Unit.DAY;
        }
        if (hours != 0) {
            return Unit.HOUR;
        }
        if (minutes != 0) {
            return Unit.MINUTE;
        }
        if (seconds != 0) {
            return Unit.SECOND;
        }
        if (milliseconds != 0) {
            return Unit.MILLISECOND;
        }
        if (microseconds != 0) {
            return Unit.MICROSECOND;
        }
        return Unit.NANOSECOND;
    }

    // 7.5.7
    public static DynamicObject toPartialDuration(DynamicObject temporalDurationLike, JSContext ctx, IsObjectNode isObjectNode, JSToIntegerWithoutRoundingNode toInt) {
        if (!isObjectNode.executeBoolean(temporalDurationLike)) {
            throw Errors.createTypeError("Given duration like is not a object.");
        }
        JSRealm realm = JSRealm.get(null);
        DynamicObject result = JSOrdinary.create(ctx, realm);
        boolean any = false;
        for (TruffleString property : DURATION_PROPERTIES) {
            Object value = JSObject.get(temporalDurationLike, property);
            if (value != Undefined.instance) {
                any = true;
                JSObjectUtil.putDataProperty(ctx, result, property, toInt.executeDouble(value));
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
    // TODO doing long arithmetics here. Might need to change to double/BigInteger
    @TruffleBoundary
    public static JSTemporalDurationRecord roundDuration(JSContext ctx, JSRealm realm, EnumerableOwnPropertyNamesNode namesNode, double y, double m, double w, double d, double h, double min,
                    double sec, double milsec, double micsec, double nsec, double increment, Unit unit, RoundingMode roundingMode, DynamicObject relTo) {
        double years = y;
        double months = m;
        double weeks = w;
        double days = d;
        double hours = h;
        double minutes = min;
        double seconds = sec;
        double microseconds = micsec;
        double milliseconds = milsec;
        double nanoseconds = nsec;

        DynamicObject relativeTo = relTo;
        if ((unit == Unit.YEAR || unit == Unit.MONTH || unit == Unit.WEEK) && relativeTo == Undefined.instance) {
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
        if (unit == Unit.YEAR || unit == Unit.MONTH || unit == Unit.WEEK || unit == Unit.DAY) {
            nanoseconds = totalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
            DynamicObject intermediate = Undefined.instance;
            if (zonedRelativeTo != Undefined.instance) {
                intermediate = moveRelativeZonedDateTime(ctx, zonedRelativeTo, dtol(years), dtol(months), dtol(weeks), dtol(days));
            }
            JSTemporalNanosecondsDaysRecord result = nanosecondsToDays(ctx, namesNode, BigInt.valueOf((long) nanoseconds), intermediate);
            days = days + bitod(result.getDays().add(result.getNanoseconds().divide(result.getDayLength().abs())));
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
        if (unit == Unit.YEAR) {
            DynamicObject yearsDuration = JSTemporalDuration.createTemporalDuration(ctx, years, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            Object dateAdd = JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject firstAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsDuration, firstAddOptions, dateAdd);
            DynamicObject yearsMonthsWeeks = JSTemporalDuration.createTemporalDuration(ctx, years, months, weeks, 0, 0, 0, 0, 0, 0, 0);

            DynamicObject secondAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsMonthsWeeksLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsMonthsWeeks, secondAddOptions, dateAdd);
            long monthsWeeksInDays = daysUntil(yearsLater, yearsMonthsWeeksLater);
            relativeTo = yearsLater;
            days = days + monthsWeeksInDays;
            DynamicObject daysDuration = JSTemporalDuration.createTemporalDuration(ctx, 0, 0, 0, days, 0, 0, 0, 0, 0, 0);
            DynamicObject thirdAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject daysLater = calendarDateAdd(calendar, relativeTo, daysDuration, thirdAddOptions, dateAdd);
            DynamicObject untilOptions = JSOrdinary.createWithNullPrototype(ctx);
            createDataPropertyOrThrow(ctx, untilOptions, LARGEST_UNIT, YEAR);
            JSTemporalDurationObject timePassed = calendarDateUntil(calendar, relativeTo, daysLater, untilOptions);
            long yearsPassed = dtol(timePassed.getYears());
            years = years + yearsPassed;
            DynamicObject oldRelativeTo = relativeTo;

            yearsDuration = JSTemporalDuration.createTemporalDuration(ctx, yearsPassed, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            DynamicObject fourthAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            relativeTo = calendarDateAdd(calendar, relativeTo, yearsDuration, fourthAddOptions, dateAdd);
            long daysPassed = daysUntil(oldRelativeTo, relativeTo);
            days = days - daysPassed;

            long sign = (days >= 0) ? 1 : -1;
            DynamicObject oneYear = JSTemporalDuration.createTemporalDuration(ctx, sign, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(ctx, calendar, relativeTo, oneYear);

            long oneYearDays = moveResult.getDays();
            double fractionalYears = years + (days / Math.abs(oneYearDays));
            years = TemporalUtil.roundNumberToIncrement(fractionalYears, increment, roundingMode);
            remainder = fractionalYears - years;
            months = 0;
            weeks = 0;
            days = 0;
        } else if (unit == Unit.MONTH) {
            DynamicObject yearsMonths = JSTemporalDuration.createTemporalDuration(ctx, years, months, 0, 0, 0, 0, 0, 0, 0, 0);
            Object dateAdd = JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject firstAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsMonthsLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsMonths, firstAddOptions, dateAdd);
            DynamicObject yearsMonthsWeeks = JSTemporalDuration.createTemporalDuration(ctx, years, months, weeks, 0, 0, 0, 0, 0, 0, 0);
            DynamicObject secondAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsMonthsWeeksLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsMonthsWeeks, secondAddOptions, dateAdd);
            long weeksInDays = daysUntil(yearsMonthsLater, yearsMonthsWeeksLater);
            relativeTo = yearsMonthsLater;
            days = days + weeksInDays;
            long sign = (days >= 0) ? 1 : -1;
            DynamicObject oneMonth = JSTemporalDuration.createTemporalDuration(ctx, 0, sign, 0, 0, 0, 0, 0, 0, 0, 0);
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
            double fractionalMonths = months + (days / Math.abs(oneMonthDays));
            months = TemporalUtil.roundNumberToIncrement(fractionalMonths, increment, roundingMode);
            remainder = fractionalMonths - months;
            weeks = 0;
            days = 0;
        } else if (unit == Unit.WEEK) {
            long sign = (days >= 0) ? 1 : -1;
            DynamicObject oneWeek = JSTemporalDuration.createTemporalDuration(ctx, 0, 0, sign, 0, 0, 0, 0, 0, 0, 0);
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
            double fractionalWeeks = weeks + (days / Math.abs(oneWeekDays));
            weeks = TemporalUtil.roundNumberToIncrement(fractionalWeeks, increment, roundingMode);
            remainder = fractionalWeeks - weeks;
            days = 0;
        } else if (unit == Unit.DAY) {
            double fractionalDays = days;
            days = TemporalUtil.roundNumberToIncrement(fractionalDays, increment, roundingMode);
            remainder = fractionalDays - days;
        } else if (unit == Unit.HOUR) {
            double secondsPart = roundDurationFractionalDecondsDiv60(fractionalSeconds);
            double fractionalHours = ((secondsPart + minutes) / 60.0) + hours;
            hours = TemporalUtil.roundNumberToIncrement(fractionalHours, increment, roundingMode);
            remainder = fractionalHours - hours;
            minutes = 0;
            seconds = 0;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit == Unit.MINUTE) {
            double secondsPart = roundDurationFractionalDecondsDiv60(fractionalSeconds);
            double fractionalMinutes = secondsPart + minutes;
            minutes = TemporalUtil.roundNumberToIncrement(fractionalMinutes, increment, roundingMode);
            remainder = fractionalMinutes - minutes;
            seconds = 0;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit == Unit.SECOND) {
            seconds = bitod(TemporalUtil.roundNumberToIncrement(fractionalSeconds, new BigDecimal(increment), roundingMode));
            remainder = roundDurationFractionalSecondsSubtract(seconds, fractionalSeconds);
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit == Unit.MILLISECOND) {
            double fractionalMilliseconds = (nanoseconds * 0.000_001) + (microseconds * 0.001) + milliseconds;
            milliseconds = TemporalUtil.roundNumberToIncrement(fractionalMilliseconds, increment, roundingMode);
            remainder = fractionalMilliseconds - milliseconds;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit == Unit.MICROSECOND) {
            double fractionalMicroseconds = (nanoseconds * 0.001) + microseconds;
            microseconds = TemporalUtil.roundNumberToIncrement(fractionalMicroseconds, increment, roundingMode);
            remainder = fractionalMicroseconds - microseconds;
            nanoseconds = 0;
        } else {
            assert unit == Unit.NANOSECOND;
            remainder = nanoseconds;
            nanoseconds = TemporalUtil.roundNumberToIncrement(nanoseconds, increment, roundingMode);
            remainder = remainder - nanoseconds;
        }

        return JSTemporalDurationRecord.createWeeksRemainder(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, remainder);
    }

    @TruffleBoundary
    private static double roundDurationFractionalSecondsSubtract(double seconds, BigDecimal fractionalSeconds) {
        return fractionalSeconds.subtract(BigDecimal.valueOf(seconds)).doubleValue();
    }

    @TruffleBoundary
    private static double roundDurationFractionalDecondsDiv60(BigDecimal fractionalSeconds) {
        return fractionalSeconds.divide(BigDecimal.valueOf(60), mc_20_floor).doubleValue();
    }

    @TruffleBoundary
    private static BigDecimal roundDurationCalculateFractionalSeconds(double seconds, double microseconds, double milliseconds, double nanoseconds) {
        BigDecimal part1 = BigDecimal.valueOf(nanoseconds).multiply(new BigDecimal("0.000000001"));
        BigDecimal part2 = BigDecimal.valueOf(microseconds).multiply(new BigDecimal("0.000001"));
        BigDecimal part3 = BigDecimal.valueOf(milliseconds).multiply(new BigDecimal("0.001"));
        return part1.add(part2).add(part3).add(BigDecimal.valueOf(seconds));
    }

    @TruffleBoundary
    private static JSTemporalNanosecondsDaysRecord nanosecondsToDays(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, BigInt nanosecondsParam, DynamicObject relativeTo) {
        BigInteger nanoseconds = nanosecondsParam.bigIntegerValue();
        long sign = nanoseconds.signum();
        BigInteger signBI = BigInteger.valueOf(sign);
        BigInteger dayLengthNs = bi_8_64_13;
        if (sign == 0) {
            return JSTemporalNanosecondsDaysRecord.create(BigInteger.ZERO, BigInteger.ZERO, dayLengthNs);
        }
        if (!TemporalUtil.isTemporalZonedDateTime(relativeTo)) {
            BigInteger val = nanoseconds.divide(dayLengthNs);
            BigInteger val2 = nanoseconds.abs().mod(dayLengthNs).multiply(signBI);
            return JSTemporalNanosecondsDaysRecord.create(val, val2, dayLengthNs);
        }
        JSTemporalZonedDateTimeObject relativeZDT = (JSTemporalZonedDateTimeObject) relativeTo;
        BigInt startNs = relativeZDT.getNanoseconds();
        JSTemporalInstantObject startInstant = JSTemporalInstant.create(ctx, startNs);
        JSTemporalPlainDateTimeObject startDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, relativeZDT.getTimeZone(), startInstant, relativeZDT.getCalendar());
        BigInt endNs = startNs.add(nanosecondsParam);
        JSTemporalInstantObject endInstant = JSTemporalInstant.create(ctx, endNs);
        JSTemporalPlainDateTimeObject endDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, relativeZDT.getTimeZone(), endInstant,
                        relativeZDT.getCalendar());
        JSTemporalDurationRecord dateDifference = TemporalUtil.differenceISODateTime(ctx, namesNode, startDateTime.getYear(), startDateTime.getMonth(),
                        startDateTime.getDay(), startDateTime.getHour(), startDateTime.getMinute(), startDateTime.getSecond(), startDateTime.getMillisecond(),
                        startDateTime.getMicrosecond(), startDateTime.getNanosecond(), endDateTime.getYear(), endDateTime.getMonth(), endDateTime.getDay(), endDateTime.getHour(),
                        endDateTime.getMinute(), endDateTime.getSecond(), endDateTime.getMillisecond(), endDateTime.getMicrosecond(), endDateTime.getNanosecond(), relativeZDT.getCalendar(), Unit.DAY,
                        Undefined.instance);
        long days = dtol(dateDifference.getDays());
        BigInt intermediateNs = TemporalUtil.addZonedDateTime(ctx, startNs, relativeZDT.getTimeZone(), relativeZDT.getCalendar(), 0, 0, 0, days, 0, 0, 0, 0, 0, 0);
        if (sign == 1) {
            while (days > 0 && intermediateNs.compareTo(endNs) == 1) {
                days = days - 1;
                intermediateNs = TemporalUtil.addZonedDateTime(ctx, startNs, relativeZDT.getTimeZone(), relativeZDT.getCalendar(), 0, 0, 0, days, 0, 0, 0, 0, 0, 0);
            }
        }
        nanoseconds = endNs.subtract(intermediateNs).bigIntegerValue();
        boolean done = false;
        while (!done) {
            BigInteger oneDayFartherNs = TemporalUtil.addZonedDateTime(ctx, intermediateNs, relativeZDT.getTimeZone(), relativeZDT.getCalendar(), 0, 0, 0, sign, 0, 0, 0, 0, 0, 0).bigIntegerValue();
            dayLengthNs = oneDayFartherNs.subtract(intermediateNs.bigIntegerValue());
            if (nanoseconds.subtract(dayLengthNs).multiply(signBI).compareTo(BigInteger.ZERO) != -1) {
                nanoseconds = nanoseconds.subtract(dayLengthNs);
                intermediateNs = new BigInt(oneDayFartherNs);
                days = days + sign;
            } else {
                done = true;
            }
        }
        return JSTemporalNanosecondsDaysRecord.create(BigInteger.valueOf(days), nanoseconds, dayLengthNs.abs());
    }

    // 7.5.21
    // TODO doing some long arithmetics here. Might need double/BigInteger
    public static JSTemporalDurationRecord adjustRoundedDurationDays(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, double years, double months, double weeks, double days, double hours,
                    double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, double increment, Unit unit, RoundingMode roundingMode,
                    DynamicObject relativeToParam) {
        if (!(TemporalUtil.isTemporalZonedDateTime(relativeToParam)) || unit == Unit.YEAR || unit == Unit.MONTH || unit == Unit.WEEK || unit == Unit.DAY ||
                        (unit == Unit.NANOSECOND && increment == 1)) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        JSTemporalZonedDateTimeObject relativeTo = (JSTemporalZonedDateTimeObject) relativeToParam;
        long timeRemainderNs = dtol(totalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0));
        long direction = Long.signum(timeRemainderNs);
        BigInt dayStart = TemporalUtil.addZonedDateTime(ctx,
                        relativeTo.getNanoseconds(), relativeTo.getTimeZone(), relativeTo.getCalendar(),
                        dtol(years), dtol(months), dtol(weeks), dtol(days), 0, 0, 0, 0, 0, 0);
        BigInt dayEnd = TemporalUtil.addZonedDateTime(ctx, dayStart, relativeTo.getTimeZone(), relativeTo.getCalendar(), 0, 0, 0, direction, 0, 0, 0, 0, 0, 0);
        long dayLengthNs = bigIntToLong(dayEnd.subtract(dayStart));
        if (((timeRemainderNs - dayLengthNs) * direction) < 0) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        BigInteger timeRemainderNsBi = TemporalUtil.roundTemporalInstant(Boundaries.bigDecimalValueOf(timeRemainderNs - dayLengthNs), increment, unit, roundingMode);
        JSTemporalDurationRecord add = addDuration(ctx, namesNode, dtol(years), dtol(months), dtol(weeks), dtol(days), 0, 0, 0, 0,
                        0, 0, 0, 0, 0, direction, 0, 0, 0, 0, 0, 0, relativeToParam);
        JSTemporalDurationRecord atd = balanceDuration(ctx, namesNode, 0, 0, 0, 0, 0, 0, timeRemainderNsBi, Unit.HOUR, Undefined.instance);

        return JSTemporalDurationRecord.createWeeks(add.getYears(), add.getMonths(), add.getWeeks(), add.getDays(),
                        atd.getHours(), atd.getMinutes(), atd.getSeconds(), atd.getMilliseconds(), atd.getMicroseconds(), atd.getNanoseconds());
    }

    // 7.5.12
    public static double totalDurationNanoseconds(double days, double hours, double minutes, double seconds, double milliseconds,
                    double microseconds, double nanoseconds, double offsetShift) {
        double ns = nanoseconds;
        if (days != 0) {
            ns -= offsetShift;
        }
        double h = hours + days * 24;
        double min = minutes + h * 60;
        double s = seconds + min * 60;
        double ms = milliseconds + s * 1000;
        double mus = microseconds + ms * 1000;
        return ns + mus * 1000; // TODO loss in precision?
    }

    // used by balanceDuration. offsetShift == 0
    @TruffleBoundary
    public static BigInteger totalDurationNanoseconds(double days, double hours, double minutes, double seconds, double milliseconds,
                    double microseconds, BigInteger nanoseconds) {
        double h = hours + days * 24;
        double min = minutes + h * 60;
        double s = seconds + min * 60;
        double ms = milliseconds + s * 1000;
        double mus = microseconds + ms * 1000;
        return nanoseconds.add(BigDecimal.valueOf(mus).toBigInteger().multiply(bi_1000));
    }

    // 7.5.11
    @TruffleBoundary
    public static double calculateOffsetShift(JSContext ctx, DynamicObject relativeTo, double y, double mon, double w, double d, double h, double min,
                    double s, double ms, double mus, double ns) {
        if (!(isTemporalZonedDateTime(relativeTo))) {
            return 0;
        }
        JSTemporalZonedDateTimeObject relativeToZDT = (JSTemporalZonedDateTimeObject) relativeTo;
        DynamicObject instant = JSTemporalInstant.create(ctx, relativeToZDT.getNanoseconds());
        long offsetBefore = getOffsetNanosecondsFor(relativeToZDT.getTimeZone(), instant);
        BigInt after = addZonedDateTime(ctx, relativeToZDT.getNanoseconds(), relativeToZDT.getTimeZone(), relativeToZDT.getCalendar(), dtol(y), dtol(mon), dtol(w), dtol(d), dtol(h), dtol(min),
                        dtol(s), dtol(ms), dtol(mus), dtol(ns));
        DynamicObject instantAfter = JSTemporalInstant.create(ctx, after);
        long offsetAfter = getOffsetNanosecondsFor(relativeToZDT.getTimeZone(), instantAfter);
        return offsetAfter - offsetBefore;
    }

    // 7.5.17
    public static long daysUntil(DynamicObject earlier, DynamicObject later) {
        assert isTemporalDate(earlier) && isTemporalDate(later);
        JSTemporalDurationRecord difference = JSTemporalPlainDate.differenceISODate(
                        ((TemporalYear) earlier).getYear(), ((TemporalMonth) earlier).getMonth(), ((TemporalDay) earlier).getDay(),
                        ((TemporalYear) later).getYear(), ((TemporalMonth) later).getMonth(), ((TemporalDay) later).getDay(), Unit.DAY);
        return dtol(difference.getDays());
    }

    private static boolean isTemporalDate(DynamicObject d) {
        return d instanceof TemporalYear && d instanceof TemporalMonth && d instanceof TemporalDay;
    }

    public static JSTemporalDurationRecord differenceTime(int h1, int min1, int s1, int ms1, int mus1, int ns1,
                    int h2, int min2, int s2, int ms2, int mus2, int ns2) {
        int hours = h2 - h1;
        int minutes = min2 - min1;
        int seconds = s2 - s1;
        int milliseconds = ms2 - ms1;
        int microseconds = mus2 - mus1;
        int nanoseconds = ns2 - ns1;
        int sign = durationSign(0, 0, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        JSTemporalDurationRecord bt = balanceTime(hours * sign, minutes * sign, seconds * sign, milliseconds * sign, microseconds * sign, nanoseconds * sign);
        return JSTemporalDurationRecord.create(0, 0, bt.getDays() * sign, bt.getHours() * sign, bt.getMinutes() * sign, bt.getSeconds() * sign,
                        bt.getMilliseconds() * sign, bt.getMicroseconds() * sign, bt.getNanoseconds() * sign);
    }

    // 4.5.15
    public static JSTemporalDurationRecord roundTime(int hours, int minutes, int seconds, int milliseconds, int microseconds,
                    int nanoseconds, double increment, Unit unit, RoundingMode roundingMode, Long dayLengthNsParam) {
        double fractionalSecond = ((double) nanoseconds / 1_000_000_000) + ((double) microseconds / 1_000_000) +
                        ((double) milliseconds / 1_000) + seconds;
        double quantity;
        if (unit == Unit.DAY) {
            long dayLengthNs = dayLengthNsParam == null ? 86_300_000_000_000L : (long) dayLengthNsParam;
            quantity = ((double) (((((hours * 60 + minutes) * 60 + seconds) * 1000 + milliseconds) * 1000 + microseconds) * 1000 + nanoseconds)) / dayLengthNs;
        } else if (unit == Unit.HOUR) {
            quantity = (fractionalSecond / 60 + minutes) / 60 + hours;
        } else if (unit == Unit.MINUTE) {
            quantity = fractionalSecond / 60 + minutes;
        } else if (unit == Unit.SECOND) {
            quantity = fractionalSecond;
        } else if (unit == Unit.MILLISECOND) {
            quantity = ((double) nanoseconds / 1_000_000) + ((double) microseconds / 1_000) + milliseconds;
        } else if (unit == Unit.MICROSECOND) {
            quantity = ((double) nanoseconds / 1_000) + microseconds;
        } else {
            assert unit == Unit.NANOSECOND;
            quantity = nanoseconds;
        }
        long result = dtol(TemporalUtil.roundNumberToIncrement(quantity, increment, roundingMode));
        if (unit == Unit.DAY) {
            return JSTemporalDurationRecord.create(0, 0, result, 0, 0, 0, 0, 0, 0);
        }
        if (unit == Unit.HOUR) {
            return balanceTime(result, 0, 0, 0, 0, 0);
        }
        if (unit == Unit.MINUTE) {
            return balanceTime(hours, result, 0, 0, 0, 0);
        }
        if (unit == Unit.SECOND) {
            return balanceTime(hours, minutes, result, 0, 0, 0);
        }
        if (unit == Unit.MILLISECOND) {
            return balanceTime(hours, minutes, seconds, result, 0, 0);
        }
        if (unit == Unit.MICROSECOND) {
            return balanceTime(hours, minutes, seconds, milliseconds, result, 0);
        }
        assert unit == Unit.NANOSECOND;
        return balanceTime(hours, minutes, seconds, milliseconds, microseconds, result);
    }

    // used when double precision is necessary, around Duration
    public static JSTemporalDurationRecord balanceTimeDouble(double h, double min, double sec, double mils, double mics, double ns) {
        if (h == Double.POSITIVE_INFINITY || h == Double.NEGATIVE_INFINITY ||
                        min == Double.POSITIVE_INFINITY || min == Double.NEGATIVE_INFINITY ||
                        sec == Double.POSITIVE_INFINITY || sec == Double.NEGATIVE_INFINITY ||
                        mils == Double.POSITIVE_INFINITY || mils == Double.NEGATIVE_INFINITY ||
                        mics == Double.POSITIVE_INFINITY || mics == Double.NEGATIVE_INFINITY ||
                        ns == Double.POSITIVE_INFINITY || ns == Double.NEGATIVE_INFINITY) {
            throw Errors.createRangeError("Time is infinite");
        }
        double microseconds = mics;
        double milliseconds = mils;
        double nanoseconds = ns;
        double seconds = sec;
        double minutes = min;
        double hours = h;
        microseconds = microseconds + Math.floor(nanoseconds / 1000.0);
        nanoseconds = TemporalUtil.nonNegativeModulo(nanoseconds, 1000);
        milliseconds = milliseconds + Math.floor(microseconds / 1000.0);
        microseconds = TemporalUtil.nonNegativeModulo(microseconds, 1000);
        seconds = seconds + Math.floor(milliseconds / 1000.0);
        milliseconds = TemporalUtil.nonNegativeModulo(milliseconds, 1000);
        minutes = minutes + Math.floor(seconds / 60.0);
        seconds = TemporalUtil.nonNegativeModulo(seconds, 60);
        hours = hours + Math.floor(minutes / 60.0);
        minutes = TemporalUtil.nonNegativeModulo(minutes, 60);
        double days = Math.floor(hours / 24.0);
        hours = TemporalUtil.nonNegativeModulo(hours, 24);
        return JSTemporalDurationRecord.create(0, 0, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    // note: there also is balanceTimeDouble
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

    public static int compareTemporalTime(int h1, int min1, int s1, int ms1, int mus1, int ns1,
                    int h2, int min2, int s2, int ms2, int mus2, int ns2) {
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

    // when used with Duration, double is necessary
    // e.g. from Temporal.PlainTime.prototype.add(duration);
    public static JSTemporalDurationRecord addTimeDouble(int hour, int minute, int second, int millisecond, int microsecond, double nanosecond,
                    double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds) {
        return balanceTimeDouble(hour + hours, minute + minutes, second + seconds, millisecond + milliseconds,
                        microsecond + microseconds, nanosecond + nanoseconds);
    }

    public static JSTemporalDurationRecord roundISODateTime(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond,
                    int nanosecond, double increment, Unit unit, RoundingMode roundingMode, Long dayLength) {
        JSTemporalDurationRecord rt = TemporalUtil.roundTime(hour, minute, second, millisecond, microsecond, nanosecond, increment, unit, roundingMode, dayLength);
        JSTemporalDateTimeRecord br = TemporalUtil.balanceISODate(year, month, day + dtoi(rt.getDays()));
        return JSTemporalDurationRecord.create(br.getYear(), br.getMonth(), br.getDay(),
                        rt.getHours(), rt.getMinutes(), rt.getSeconds(),
                        rt.getMilliseconds(), rt.getMicroseconds(), rt.getNanoseconds());
    }

    public static double toTemporalDateTimeRoundingIncrement(DynamicObject options, Unit smallestUnit, IsObjectNode isObject, JSToNumberNode toNumber) {
        int maximum = 0;
        if (Unit.DAY == smallestUnit) {
            maximum = 1;
        } else if (Unit.HOUR == smallestUnit) {
            maximum = 24;
        } else if (Unit.MINUTE == smallestUnit || Unit.SECOND == smallestUnit) {
            maximum = 60;
        } else {
            assert Unit.MILLISECOND == smallestUnit || Unit.MICROSECOND == smallestUnit || Unit.NANOSECOND == smallestUnit;
            maximum = 1000;
        }
        return toTemporalRoundingIncrement(options, (double) maximum, false, isObject, toNumber);
    }

    public static boolean isValidTime(int hours, int minutes, int seconds, int milliseconds, int microseconds, int nanoseconds) {
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

    public static boolean isValidISODate(int year, int month, int day) {
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
                    List<TruffleString> disallowedFields, IsObjectNode isObject, JSToStringNode toStringNode) {
        JSTemporalDurationRecord d;
        if (!isObject.executeBoolean(temporalDurationLike)) {
            TruffleString str = toStringNode.executeString(temporalDurationLike);
            d = JSTemporalDuration.parseTemporalDurationString(str);
        } else {
            d = JSTemporalDuration.toTemporalDurationRecord((DynamicObject) temporalDurationLike);
        }
        if (!TemporalUtil.validateTemporalDuration(d.getYears(), d.getMonths(), d.getWeeks(), d.getDays(), d.getHours(), d.getMinutes(), d.getSeconds(), d.getMilliseconds(),
                        d.getMicroseconds(), d.getNanoseconds())) {
            throw Errors.createRangeError("Given duration outside range.");
        }

        for (TruffleString property : TemporalUtil.DURATION_PROPERTIES) {
            double value = TemporalUtil.getPropertyFromRecord(d, property);
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

    public static JSTemporalDateTimeRecord balanceISODateTime(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond, long nanosecond) {
        JSTemporalDurationRecord bt = balanceTime(hour, minute, second, millisecond, microsecond, nanosecond);
        JSTemporalDateTimeRecord bd = balanceISODate(year, month, day + dtoi(bt.getDays()));
        return JSTemporalDateTimeRecord.create(bd.getYear(), bd.getMonth(), bd.getDay(), dtoi(bt.getHours()), dtoi(bt.getMinutes()), dtoi(bt.getSeconds()),
                        dtoi(bt.getMilliseconds()), dtoi(bt.getMicroseconds()), dtoi(bt.getNanoseconds()));
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord getISOPartsFromEpoch(BigInt epochNanoseconds) {
        long remainderNs;
        long epochMilliseconds;
        if (epochNanoseconds.fitsInLong()) {
            remainderNs = epochNanoseconds.longValue() % 1_000_000;
            epochMilliseconds = (epochNanoseconds.longValue() - remainderNs) / 1_000_000;
        } else {
            BigInteger[] result = epochNanoseconds.bigIntegerValue().divideAndRemainder(bi_10_pow_6);
            remainderNs = result[1].longValue();
            epochMilliseconds = result[0].longValue();
        }
        int year = JSDate.yearFromTime(epochMilliseconds);
        int month = JSDate.monthFromTime(epochMilliseconds) + 1;
        int day = JSDate.dateFromTime(epochMilliseconds);
        int hour = JSDate.hourFromTime(epochMilliseconds);
        int minute = JSDate.minFromTime(epochMilliseconds);
        int second = JSDate.secFromTime(epochMilliseconds);
        int millisecond = JSDate.msFromTime(epochMilliseconds);
        int microsecond = (int) ((remainderNs / 1000) % 1000);
        int nanosecond = (int) (remainderNs % 1000);
        return JSTemporalDateTimeRecord.create(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
    }

    @TruffleBoundary
    public static long getOffsetNanosecondsFor(DynamicObject timeZone, DynamicObject instant) {
        Object getOffsetNanosecondsFor = JSObject.getMethod(timeZone, GET_OFFSET_NANOSECONDS_FOR);
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
        TruffleString identifier = defaultTimeZone();
        return createTemporalTimeZone(ctx, identifier);
    }

    public static TruffleString defaultTimeZone() {
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
        TruffleString string = JSRuntime.toString(item);
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
    public static BigInt addInstant(BigInt epochNanoseconds, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds) {
        return addInstant(epochNanoseconds, dtol(hours), dtol(minutes), dtol(seconds), dtol(milliseconds), dtol(microseconds), BigInteger.valueOf(dtol(nanoseconds)));
    }

    @TruffleBoundary
    public static BigInt addInstant(BigInt epochNanoseconds, long hours, long minutes, long seconds, long milliseconds, long microseconds, BigInteger nanoseconds) {
        BigInteger res = epochNanoseconds.bigIntegerValue().add(nanoseconds);
        res = res.add(BigInteger.valueOf(microseconds).multiply(BigInteger.valueOf(1000)));
        res = res.add(BigInteger.valueOf(milliseconds).multiply(BigInteger.valueOf(1_000_000)));
        res = res.add(BigInteger.valueOf(seconds).multiply(BigInteger.valueOf(1_000_000_000L)));
        res = res.add(BigInteger.valueOf(minutes).multiply(BigInteger.valueOf(60_000_000_000L)));
        res = res.add(BigInteger.valueOf(hours).multiply(BigInteger.valueOf(3_600_000_000_000L)));
        BigInt result = new BigInt(res);
        if (!isValidEpochNanoseconds(result)) {
            throw TemporalErrors.createRangeErrorInvalidNanoseconds();
        }
        return result; // spec return type: BigInt
    }

    @TruffleBoundary

    public static BigInteger differenceInstant(BigInt ns1, BigInt ns2, double roundingIncrement, Unit smallestUnit, RoundingMode roundingMode) {
        return roundTemporalInstant(ns2.subtract(ns1), roundingIncrement, smallestUnit, roundingMode);
    }

    @TruffleBoundary
    public static TruffleString temporalInstantToString(JSContext ctx, JSRealm realm, DynamicObject instant, DynamicObject timeZone, Object precision) {
        DynamicObject outputTimeZone = timeZone;
        if (outputTimeZone == Undefined.instance) {
            outputTimeZone = createTemporalTimeZone(ctx, UTC);
        }
        DynamicObject isoCalendar = getISO8601Calendar(ctx, realm);
        JSTemporalPlainDateTimeObject dateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, outputTimeZone, instant, isoCalendar);
        TruffleString dateTimeString = JSTemporalPlainDateTime.temporalDateTimeToString(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(),
                        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(), Undefined.instance,
                        precision, ShowCalendar.NEVER);
        TruffleString timeZoneString = null;
        if (timeZone == Undefined.instance) {
            timeZoneString = Strings.UC_Z;
        } else {
            long offsetNs = getOffsetNanosecondsFor(timeZone, instant);
            timeZoneString = formatISOTimeZoneOffsetString(offsetNs);
        }
        return Strings.concat(dateTimeString, timeZoneString);
    }

    public static TruffleString builtinTimeZoneGetOffsetStringFor(DynamicObject timeZone, DynamicObject instant) {
        long offsetNanoseconds = getOffsetNanosecondsFor(timeZone, instant);
        return formatTimeZoneOffsetString(offsetNanoseconds);
    }

    @TruffleBoundary
    public static TruffleString formatTimeZoneOffsetString(long offsetNanosecondsParam) {
        TruffleString sign = offsetNanosecondsParam >= 0 ? Strings.SYMBOL_PLUS : Strings.SYMBOL_MINUS;
        long offsetNanoseconds = Math.abs(offsetNanosecondsParam);
        long nanoseconds = offsetNanoseconds % 1_000_000_000L;
        double s1 = (Math.floor(offsetNanoseconds / 1_000_000_000.0) % 60.0);
        double m1 = (Math.floor(offsetNanoseconds / 60_000_000_000.0) % 60.0);
        double h1 = Math.floor(offsetNanoseconds / 3_600_000_000_000.0);

        long seconds = (long) s1;
        long minutes = (long) m1;
        long hours = (long) h1;

        TruffleString h = Strings.format("%1$02d", hours);
        TruffleString m = Strings.format("%1$02d", minutes);
        TruffleString s = Strings.format("%1$02d", seconds);

        TruffleString post = Strings.EMPTY_STRING;
        if (nanoseconds != 0) {
            TruffleString fraction = longestSubstring(Strings.format("%1$09d", nanoseconds));
            post = Strings.concatAll(Strings.COLON, s, Strings.DOT, fraction);
        } else if (seconds != 0) {
            post = Strings.concat(Strings.COLON, s);
        }
        return Strings.concatAll(sign, h, Strings.COLON, m, post);
    }

    @TruffleBoundary
    public static long parseTimeZoneOffsetString(TruffleString string) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseTimeZoneNumericUTCOffset();
        if (rec == null) {
            throw Errors.createRangeError("TemporalTimeZoneNumericUTCOffset expected");
        }

        long nanoseconds;
        if (rec.getOffsetFraction() == null) {
            nanoseconds = 0;
        } else {
            TruffleString fraction = Strings.concat(rec.getOffsetFraction(), ZEROS);
            fraction = Strings.lazySubstring(fraction, 0, 9);
            try {
                nanoseconds = Strings.parseLong(fraction, 10);
            } catch (TruffleString.NumberFormatException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        TruffleString signS = rec.getOffsetSign();
        int sign = (Strings.SYMBOL_MINUS.equals(signS) || Strings.UNICODE_MINUS_SIGN.equals(signS)) ? -1 : 1;

        long hours = rec.getOffsetHour() == Long.MIN_VALUE ? 0 : rec.getOffsetHour();
        long minutes = rec.getOffsetMinute() == Long.MIN_VALUE ? 0 : rec.getOffsetMinute();
        long seconds = rec.getOffsetSecond() == Long.MIN_VALUE ? 0 : rec.getOffsetSecond();

        return sign * (((hours * 60 + minutes) * 60 + seconds) * 1_000_000_000L + nanoseconds);
    }

    public static JSTemporalTimeZoneRecord parseTemporalTimeZoneString(TruffleString string) {
        return parseTemporalTimeZoneString(string, false);
    }

    @TruffleBoundary
    private static JSTemporalTimeZoneRecord parseTemporalTimeZoneString(TruffleString string, boolean offsetRequired) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseTimeZoneString();
        if (rec == null) {
            throw Errors.createRangeError("TemporalTimeZoneString expected");
        }
        if (offsetRequired) {
            if (rec.getOffsetHour() == Long.MIN_VALUE && !rec.getZ()) {
                throw TemporalErrors.createRangeErrorTimeZoneOffsetExpected();
            }
        }
        TruffleString name = rec.getTimeZoneIANAName();
        TruffleString offsetString = rec.getTimeZoneNumericUTCOffset();
        if (rec.getZ()) {
            return JSTemporalTimeZoneRecord.create(true, null, name);
        }
        return JSTemporalTimeZoneRecord.create(false, offsetString, name);
    }

    public static Disambiguation toTemporalDisambiguation(DynamicObject options, TemporalGetOptionNode getOptionNode) {
        return toDisambiguation((TruffleString) getOptionNode.execute(options, DISAMBIGUATION, OptionType.STRING, listDisambiguation, COMPATIBLE));
    }

    public static Disambiguation toTemporalDisambiguation(DynamicObject options) {
        return toDisambiguation((TruffleString) getOption(options, DISAMBIGUATION, OptionType.STRING, listDisambiguation, COMPATIBLE));
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
        TruffleString offsetString = null;
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
            List<TruffleString> fieldNames = calendarFields(ctx, calendar, listDHMMMMMNSY);
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
            TruffleString string = JSRuntime.toString(item);
            JSTemporalZonedDateTimeRecord resultZDT = parseTemporalZonedDateTimeString(string);
            result = resultZDT;
            TruffleString timeZoneName = resultZDT.getTimeZoneName();
            assert !isNullish(timeZoneName);
            if (!canParseAsTimeZoneNumericUTCOffset(timeZoneName)) {
                if (!isValidTimeZoneName(timeZoneName)) {
                    throw TemporalErrors.createRangeErrorInvalidTimeZoneString();
                }
                timeZoneName = canonicalizeTimeZoneName(timeZoneName);
            }
            offsetString = resultZDT.getTimeZoneOffsetString();
            if (resultZDT.getTimeZoneZ()) {
                offsetBehaviour = OffsetBehaviour.EXACT;
            } else if (offsetString == null) {
                offsetBehaviour = OffsetBehaviour.WALL;
            }
            timeZone = createTemporalTimeZone(ctx, timeZoneName);
            calendar = toTemporalCalendarWithISODefault(ctx, realm, result.getCalendar());
            matchBehaviour = MatchBehaviour.MATCH_MINUTES;
        }
        long offsetNanoseconds = 0;
        if (offsetBehaviour == OffsetBehaviour.OPTION) {
            offsetNanoseconds = TemporalUtil.parseTimeZoneOffsetString(offsetString);
        }
        Disambiguation disambiguation = TemporalUtil.toTemporalDisambiguation(options);
        OffsetOption offset = TemporalUtil.toTemporalOffset(options, REJECT);
        BigInt epochNanoseconds = TemporalUtil.interpretISODateTimeOffset(ctx, realm, result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(),
                        result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), offsetBehaviour, offsetNanoseconds, timeZone, disambiguation, offset,
                        matchBehaviour);
        return TemporalUtil.createTemporalZonedDateTime(ctx, epochNanoseconds, timeZone, calendar);
    }

    public static OffsetOption toTemporalOffset(DynamicObject options, TruffleString fallback, TemporalGetOptionNode getOptionNode) {
        return toOffsetOption((TruffleString) getOptionNode.execute(options, OFFSET, OptionType.STRING, listOffset, fallback));
    }

    public static OffsetOption toTemporalOffset(DynamicObject options, TruffleString fallback) {
        return toOffsetOption((TruffleString) getOption(options, OFFSET, OptionType.STRING, listOffset, fallback));
    }

    public static TruffleString toShowTimeZoneNameOption(DynamicObject options, TemporalGetOptionNode getOptionNode) {
        return (TruffleString) getOptionNode.execute(options, TIME_ZONE_NAME, OptionType.STRING, listAutoNever, AUTO);
    }

    public static TruffleString toShowOffsetOption(DynamicObject options, TemporalGetOptionNode getOptionNode) {
        return (TruffleString) getOptionNode.execute(options, OFFSET, OptionType.STRING, listAutoNever, AUTO);
    }

    public static TruffleString temporalZonedDateTimeToString(JSContext ctx, JSRealm realm, DynamicObject zonedDateTime, Object precision, ShowCalendar showCalendar, TruffleString showTimeZone,
                    TruffleString showOffset) {
        return temporalZonedDateTimeToString(ctx, realm, zonedDateTime, precision, showCalendar, showTimeZone, showOffset, null, null, null);
    }

    public static JSTemporalDateTimeRecord addDateTime(JSContext ctx, int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond,
                    double nanosecond, DynamicObject calendar, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds,
                    double microseconds, double nanoseconds, DynamicObject options) {
        JSTemporalDurationRecord timeResult = TemporalUtil.addTimeDouble(hour, minute, second, millisecond, microsecond, nanosecond,
                        hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        DynamicObject datePart = JSTemporalPlainDate.create(ctx, year, month, day, calendar);
        DynamicObject dateDuration = JSTemporalDuration.createTemporalDuration(ctx, years, months, weeks, days + timeResult.getDays(), 0L, 0L, 0L, 0L, 0L, 0L);
        JSTemporalPlainDateObject addedDate = (JSTemporalPlainDateObject) TemporalUtil.calendarDateAdd(calendar, datePart, dateDuration, options);
        return JSTemporalDateTimeRecord.create(addedDate.getYear(), addedDate.getMonth(), addedDate.getDay(),
                        dtoi(timeResult.getHours()), dtoi(timeResult.getMinutes()), dtoi(timeResult.getSeconds()),
                        dtoi(timeResult.getMilliseconds()), dtoi(timeResult.getMicroseconds()), dtoi(timeResult.getNanoseconds()));
    }

    public static int compareISODateTime(int year, int month, int day, int hours, int minutes, int seconds, int milliseconds, int microseconds, int nanoseconds, int year2, int month2,
                    int day2, int hours2, int minutes2, int seconds2, int milliseconds2, int microseconds2, int nanoseconds2) {
        int date = TemporalUtil.compareISODate(year, month, day, year2, month2, day2);
        if (date == 0) {
            return TemporalUtil.compareTemporalTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds, hours2, minutes2, seconds2, milliseconds2, microseconds2, nanoseconds2);
        }
        return date;
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalYearMonthString(TruffleString string) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseYearMonth();
        if (rec != null) {
            if (rec.getZ()) {
                throw TemporalErrors.createRangeErrorUnexpectedUTCDesignator();
            }
            if (rec.getYear() == 0 && (Strings.indexOf(string, TemporalConstants.MINUS_000000) >= 0 || Strings.indexOf(string, TemporalConstants.UNICODE_MINUS_SIGN_000000) >= 0)) {
                throw TemporalErrors.createRangeErrorInvalidPlainDateTime();
            }

            int y = rec.getYear() == Long.MIN_VALUE ? 0 : ltoi(rec.getYear());
            int m = rec.getMonth() == Long.MIN_VALUE ? 0 : ltoi(rec.getMonth());
            int d = rec.getDay() == Long.MIN_VALUE ? 1 : ltoi(rec.getDay());
            return JSTemporalDateTimeRecord.createCalendar(y, m, d, 0, 0, 0, 0, 0, 0, rec.getCalendar());
        } else {
            throw Errors.createRangeError("cannot parse YearMonth");
        }
    }

    @TruffleBoundary
    public static TruffleString temporalZonedDateTimeToString(JSContext ctx, JSRealm realm, DynamicObject zonedDateTimeParam, Object precision, ShowCalendar showCalendar, TruffleString showTimeZone,
                    TruffleString showOffset, Double incrementParam, Unit unitParam, RoundingMode roundingModeParam) {
        assert isTemporalZonedDateTime(zonedDateTimeParam);
        JSTemporalZonedDateTimeObject zonedDateTime = (JSTemporalZonedDateTimeObject) zonedDateTimeParam;
        double increment = incrementParam == null ? 1 : (double) incrementParam;
        Unit unit = (isNullish(unitParam) || unitParam == Unit.EMPTY) ? Unit.NANOSECOND : unitParam;
        RoundingMode roundingMode = (isNullish(roundingModeParam) || roundingModeParam == RoundingMode.EMPTY) ? RoundingMode.TRUNC : roundingModeParam;

        BigInteger ns = roundTemporalInstant(zonedDateTime.getNanoseconds(), (long) increment, unit, roundingMode);
        DynamicObject timeZone = zonedDateTime.getTimeZone();
        JSTemporalInstantObject instant = JSTemporalInstant.create(ctx, new BigInt(ns));
        JSTemporalCalendarObject isoCalendar = (JSTemporalCalendarObject) getISO8601Calendar(ctx, realm);
        JSTemporalPlainDateTimeObject temporalDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, timeZone, instant, isoCalendar);
        TruffleString dateTimeString = JSTemporalPlainDateTime.temporalDateTimeToString(temporalDateTime.getYear(), temporalDateTime.getMonth(), temporalDateTime.getDay(),
                        temporalDateTime.getHour(), temporalDateTime.getMinute(), temporalDateTime.getSecond(), temporalDateTime.getMillisecond(),
                        temporalDateTime.getMicrosecond(), temporalDateTime.getNanosecond(), isoCalendar, precision, ShowCalendar.NEVER);
        TruffleString offsetString = null;
        TruffleString timeZoneString = null;
        if (NEVER.equals(showOffset)) {
            offsetString = Strings.EMPTY_STRING;
        } else {
            long offsetNs = getOffsetNanosecondsFor(timeZone, instant);
            offsetString = formatISOTimeZoneOffsetString(offsetNs);
        }
        if (NEVER.equals(showTimeZone)) {
            timeZoneString = Strings.EMPTY_STRING;
        } else {
            TruffleString timeZoneID = JSRuntime.toString(timeZone);
            timeZoneString = Strings.addBrackets(timeZoneID);
        }
        TruffleString calendarID = JSRuntime.toString(zonedDateTime.getCalendar());
        TruffleString calendarString = formatCalendarAnnotation(calendarID, showCalendar);
        return Strings.concatAll(dateTimeString, offsetString, timeZoneString, calendarString);
    }

    @TruffleBoundary
    private static TruffleString formatISOTimeZoneOffsetString(long offsetNs) {
        long offsetNanoseconds = dtol(roundNumberToIncrement(offsetNs, 60_000_000_000L, RoundingMode.HALF_EXPAND));
        TruffleString sign = Strings.EMPTY_STRING;
        sign = (offsetNanoseconds >= 0) ? Strings.SYMBOL_PLUS : Strings.SYMBOL_MINUS;
        offsetNanoseconds = Math.abs(offsetNanoseconds);
        long minutes = (offsetNanoseconds / 60_000_000_000L) % 60;
        long hours = (long) Math.floor(offsetNanoseconds / 3_600_000_000_000L);

        TruffleString h = Strings.format("%1$02d", hours);
        TruffleString m = Strings.format("%1$02d", minutes);

        return Strings.concatAll(sign, h, Strings.COLON, m);
    }

    @TruffleBoundary
    public static JSTemporalZonedDateTimeRecord parseTemporalZonedDateTimeString(TruffleString string) {
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
    private static BigInt parseTemporalInstant(TruffleString string) {
        JSTemporalZonedDateTimeRecord result = parseTemporalInstantString(string);
        TruffleString offsetString = result.getTimeZoneOffsetString();
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
    private static JSTemporalZonedDateTimeRecord parseTemporalInstantString(TruffleString string) {
        try {
            JSTemporalDateTimeRecord result = parseISODateTime(string);
            JSTemporalTimeZoneRecord timeZoneResult = parseTemporalTimeZoneString(string, true);
            TruffleString offsetString = timeZoneResult.getOffsetString();
            if (timeZoneResult.isZ()) {
                offsetString = OFFSET_ZERO;
            }
            assert !isNullish(offsetString);
            return JSTemporalZonedDateTimeRecord.create(result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(),
                            result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), null, false, offsetString, null);
        } catch (Exception ex) {
            throw Errors.createRangeError("Instant cannot be parsed");
        }
    }

    @TruffleBoundary
    public static JSTemporalInstantObject builtinTimeZoneGetInstantFor(JSContext ctx, DynamicObject timeZone, JSTemporalPlainDateTimeObject dateTime, Disambiguation disambiguation) {
        List<JSTemporalInstantObject> possibleInstants = getPossibleInstantsFor(timeZone, dateTime);
        return disambiguatePossibleInstants(ctx, possibleInstants, timeZone, dateTime, disambiguation);
    }

    @TruffleBoundary
    public static JSTemporalInstantObject disambiguatePossibleInstants(JSContext ctx, List<JSTemporalInstantObject> possibleInstants, DynamicObject timeZone, JSTemporalPlainDateTimeObject dateTime,
                    Disambiguation disambiguation) {
        int n = possibleInstants.size();
        if (n == 1) {
            return possibleInstants.get(0);
        } else if (n != 0) {
            if (Disambiguation.EARLIER == disambiguation || Disambiguation.COMPATIBLE == disambiguation) {
                return possibleInstants.get(0);
            } else if (Disambiguation.LATER == disambiguation) {
                return possibleInstants.get(n - 1);
            }
            assert Disambiguation.REJECT == disambiguation;
            throw Errors.createRangeError("invalid disambiguation");
        }
        assert n == 0;
        if (Disambiguation.REJECT == disambiguation) {
            throw Errors.createRangeError("disambiguation failed");
        }
        BigInteger epochNanoseconds = getEpochFromISOParts(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
                        dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond());
        JSTemporalInstantObject dayBefore = JSTemporalInstant.create(ctx, new BigInt(epochNanoseconds.subtract(bi_8_64_13)));
        JSTemporalInstantObject dayAfter = JSTemporalInstant.create(ctx, new BigInt(epochNanoseconds.add(bi_8_64_13)));
        long offsetBefore = getOffsetNanosecondsFor(timeZone, dayBefore);
        long offsetAfter = getOffsetNanosecondsFor(timeZone, dayAfter);
        long nanoseconds = offsetAfter - offsetBefore;
        if (Disambiguation.EARLIER == disambiguation) {
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
        assert Disambiguation.LATER == disambiguation || Disambiguation.COMPATIBLE == disambiguation;
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
    public static BigInt interpretISODateTimeOffset(JSContext ctx, JSRealm realm, int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond,
                    int nanosecond, OffsetBehaviour offsetBehaviour, Object offsetNanosecondsParam, DynamicObject timeZone, Disambiguation disambiguation, OffsetOption offsetOption,
                    MatchBehaviour matchBehaviour) {
        double offsetNs = isNullish(offsetNanosecondsParam) ? Double.NaN : ((Number) offsetNanosecondsParam).doubleValue();
        DynamicObject calendar = getISO8601Calendar(ctx, realm);
        JSTemporalPlainDateTimeObject dateTime = createTemporalDateTime(ctx, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, calendar);
        if (offsetBehaviour == OffsetBehaviour.WALL || OffsetOption.IGNORE == offsetOption) {
            JSTemporalInstantObject instant = builtinTimeZoneGetInstantFor(ctx, timeZone, dateTime, disambiguation);
            return instant.getNanoseconds();
        }
        if (offsetBehaviour == OffsetBehaviour.EXACT || OffsetOption.USE == offsetOption) {
            BigInteger epochNanoseconds = getEpochFromISOParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
            return new BigInt(epochNanoseconds.subtract(BigInteger.valueOf((long) offsetNs)));
        }
        assert offsetBehaviour == OffsetBehaviour.OPTION;
        assert OffsetOption.PREFER == offsetOption || OffsetOption.REJECT == offsetOption;
        List<JSTemporalInstantObject> possibleInstants = getPossibleInstantsFor(timeZone, dateTime);
        for (JSTemporalInstantObject candidate : possibleInstants) {
            long candidateNanoseconds = getOffsetNanosecondsFor(timeZone, candidate);
            if (candidateNanoseconds == offsetNs) {
                return candidate.getNanoseconds();
            }
            if (matchBehaviour == MatchBehaviour.MATCH_MINUTES) {
                long roundedCandidateNanoseconds = dtol(roundNumberToIncrement(candidateNanoseconds, 60_000_000_000L, RoundingMode.HALF_EXPAND));
                if (roundedCandidateNanoseconds == offsetNs) {
                    return candidate.getNanoseconds();
                }
            }
        }
        if (OffsetOption.REJECT == offsetOption) {
            throw Errors.createRangeError("cannot interpret DateTime offset");
        }
        JSTemporalInstantObject instant = builtinTimeZoneGetInstantFor(ctx, timeZone, dateTime, disambiguation);
        return instant.getNanoseconds();
    }

    @TruffleBoundary
    public static BigInt addZonedDateTime(JSContext ctx, BigInt epochNanoseconds, DynamicObject timeZone, DynamicObject calendar, long years, long months, long weeks, long days,
                    long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        return addZonedDateTime(ctx, epochNanoseconds, timeZone, calendar, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, BigInteger.valueOf(nanoseconds),
                        Undefined.instance);
    }

    @TruffleBoundary
    public static BigInt addZonedDateTime(JSContext ctx, BigInt epochNanoseconds, DynamicObject timeZone, DynamicObject calendar, long years, long months, long weeks, long days,
                    long hours, long minutes, long seconds, long milliseconds, long microseconds, BigInteger nanoseconds, DynamicObject options) {
        if (years == 0 && months == 0 && weeks == 0 && days == 0) {
            return addInstant(epochNanoseconds, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        JSTemporalInstantObject instant = JSTemporalInstant.create(ctx, epochNanoseconds);
        JSTemporalPlainDateTimeObject temporalDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, timeZone, instant, calendar);
        JSTemporalPlainDateObject datePart = createTemporalDate(ctx, temporalDateTime.getYear(), temporalDateTime.getMonth(), temporalDateTime.getDay(), calendar);
        JSTemporalDurationObject dateDuration = JSTemporalDuration.createTemporalDuration(ctx, years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        JSTemporalPlainDateObject addedDate = (JSTemporalPlainDateObject) calendarDateAdd(calendar, datePart, dateDuration, options);
        JSTemporalPlainDateTimeObject intermediateDateTime = createTemporalDateTime(ctx, addedDate.getYear(), addedDate.getMonth(), addedDate.getDay(),
                        temporalDateTime.getHour(), temporalDateTime.getMinute(), temporalDateTime.getSecond(),
                        temporalDateTime.getMillisecond(), temporalDateTime.getMicrosecond(), temporalDateTime.getNanosecond(), calendar);
        JSTemporalInstantObject intermediateInstant = builtinTimeZoneGetInstantFor(ctx, timeZone, intermediateDateTime, Disambiguation.COMPATIBLE);
        return addInstant(intermediateInstant.getNanoseconds(), hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    public static DynamicObject moveRelativeZonedDateTime(JSContext ctx, DynamicObject zonedDateTime, long years, long months, long weeks, long days) {
        JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) zonedDateTime;
        BigInt intermediateNs = addZonedDateTime(ctx, zdt.getNanoseconds(), zdt.getTimeZone(), zdt.getCalendar(), years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        return createTemporalZonedDateTime(ctx, intermediateNs, zdt.getTimeZone(), zdt.getCalendar());
    }

    public static boolean timeZoneEquals(DynamicObject tz1, DynamicObject tz2, JSToStringNode toStringNode) {
        if (tz1 == tz2) {
            return true;
        }
        TruffleString s1 = toStringNode.executeString(tz1);
        TruffleString s2 = toStringNode.executeString(tz2);
        return Boundaries.equals(s1, s2);
    }

    public static DynamicObject consolidateCalendars(DynamicObject one, DynamicObject two, JSToStringNode toStringNode) {
        if (one == two) {
            return two;
        }
        TruffleString s1 = toStringNode.executeString(one);
        TruffleString s2 = toStringNode.executeString(two);
        return consolidateCalendarsIntl(one, two, s1, s2);
    }

    @TruffleBoundary
    private static DynamicObject consolidateCalendarsIntl(DynamicObject one, DynamicObject two, TruffleString s1, TruffleString s2) {
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
        Object fn = JSObject.get(timeZone, GET_POSSIBLE_INSTANTS_FOR);
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
    public static List<BigInt> getIANATimeZoneEpochValue(TruffleString identifier, long isoYear, long isoMonth, long isoDay, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds,
                    long nanoseconds) {
        List<BigInt> list = new ArrayList<>();
        try {
            ZoneId zoneId = ZoneId.of(Strings.toJavaString(identifier));
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
    public static long getIANATimeZoneOffsetNanoseconds(BigInt nanoseconds, TruffleString identifier) {
        try {
            Instant instant = Instant.ofEpochSecond(0, nanoseconds.longValue()); // TODO wrong
            ZoneId zoneId = ZoneId.of(Strings.toJavaString(identifier));
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
    public static OptionalLong getIANATimeZoneNextTransition(BigInt nanoseconds, TruffleString identifier) {
        try {
            BigInteger[] sec = nanoseconds.bigIntegerValue().divideAndRemainder(bi_10_pow_9);
            Instant instant = Instant.ofEpochSecond(sec[0].longValue(), sec[1].longValue());
            ZoneId zoneId = ZoneId.of(Strings.toJavaString(identifier));
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
    public static OptionalLong getIANATimeZonePreviousTransition(BigInt nanoseconds, TruffleString identifier) {
        try {
            BigInteger[] sec = nanoseconds.bigIntegerValue().divideAndRemainder(bi_10_pow_9);
            Instant instant = Instant.ofEpochSecond(sec[0].longValue(), sec[1].longValue());
            ZoneId zoneId = ZoneId.of(Strings.toJavaString(identifier));
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

    @TruffleBoundary
    public static boolean canParseAsTimeZoneNumericUTCOffset(TruffleString string) {
        try {
            JSTemporalParserRecord rec = (new TemporalParser(string)).parseTimeZoneNumericUTCOffset();
            if (rec == null) {
                return false; // it cannot be parsed
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isoYearMonthWithinLimits(int year, int month) {
        if (year < -271821 || year > 275760) {
            return false;
        }
        if (year == -271821 && month < 4) {
            return false;
        }
        if (year == 275760 && month > 9) {
            return false;
        }
        return true;
    }

    private static Object executeFunction(DynamicObject obj, TruffleString fnName, Object... funcArgs) {
        DynamicObject fn = (DynamicObject) JSObject.getMethod(obj, fnName);
        return JSRuntime.call(fn, obj, funcArgs);
    }

    // 12.1.9
    public static Number calendarYear(DynamicObject calendar, DynamicObject dateLike) {
        Object result = executeFunction(calendar, YEAR, dateLike);
        if (result == Undefined.instance) {
            throw Errors.createRangeError("expected year value.");
        }
        return TemporalUtil.toIntegerThrowOnInfinity(result);
    }

    // 12.1.10
    public static Number calendarMonth(DynamicObject calendar, DynamicObject dateLike) {
        Object result = executeFunction(calendar, MONTH, dateLike);
        if (result == Undefined.instance) {
            throw Errors.createRangeError("expected month value.");
        }
        return TemporalUtil.toIntegerThrowOnInfinity(result);
    }

    // 12.1.11
    public static TruffleString calendarMonthCode(DynamicObject calendar, DynamicObject dateLike) {
        Object result = executeFunction(calendar, MONTH_CODE, dateLike);
        if (result == Undefined.instance) {
            throw Errors.createRangeError("expected monthCode value.");
        }
        return JSRuntime.toString(result);
    }

    // 12.1.12
    public static Number calendarDay(DynamicObject calendar, DynamicObject dateLike) {
        Object result = executeFunction(calendar, DAY, dateLike);
        if (result == Undefined.instance) {
            throw Errors.createRangeError("expected day value.");
        }
        return TemporalUtil.toIntegerThrowOnInfinity(result);
    }

    // 12.1.13
    public static Object calendarDayOfWeek(DynamicObject calendar, DynamicObject dateLike) {
        return executeFunction(calendar, TemporalConstants.DAY_OF_WEEK, dateLike);
    }

    // 12.1.14
    public static Object calendarDayOfYear(DynamicObject calendar, DynamicObject dateLike) {
        return executeFunction(calendar, TemporalConstants.DAY_OF_YEAR, dateLike);
    }

    // 12.1.15
    public static Object calendarWeekOfYear(DynamicObject calendar, DynamicObject dateLike) {
        return executeFunction(calendar, TemporalConstants.WEEK_OF_YEAR, dateLike);
    }

    // 12.1.16
    public static Object calendarDaysInWeek(DynamicObject calendar, DynamicObject dateLike) {
        return executeFunction(calendar, TemporalConstants.DAYS_IN_WEEK, dateLike);
    }

    // 12.1.17
    public static Object calendarDaysInMonth(DynamicObject calendar, DynamicObject dateLike) {
        return executeFunction(calendar, TemporalConstants.DAYS_IN_MONTH, dateLike);
    }

    // 12.1.18
    public static Object calendarDaysInYear(DynamicObject calendar, DynamicObject dateLike) {
        return executeFunction(calendar, TemporalConstants.DAYS_IN_YEAR, dateLike);
    }

    // 12.1.19
    public static Object calendarMonthsInYear(DynamicObject calendar, DynamicObject dateLike) {
        return executeFunction(calendar, TemporalConstants.MONTHS_IN_YEAR, dateLike);
    }

    // 12.1.20
    public static Object calendarInLeapYear(DynamicObject calendar, DynamicObject dateLike) {
        return executeFunction(calendar, TemporalConstants.IN_LEAP_YEAR, dateLike);
    }

    // 12.1.38
    public static Object resolveISOMonth(JSContext ctx, DynamicObject fields, JSToIntegerOrInfinityNode toIntegerOrInfinity, JSIdenticalNode identicalNode) {
        Object month = JSObject.get(fields, MONTH);
        Object monthCode = JSObject.get(fields, MONTH_CODE);
        if (TemporalUtil.isNullish(monthCode)) {
            if (TemporalUtil.isNullish(month)) {
                throw Errors.createTypeError("No month or month code present.");
            }
            return month;
        }
        assert monthCode instanceof TruffleString;
        int monthLength = Strings.length((TruffleString) monthCode);
        if (monthLength != 3) {
            throw Errors.createRangeError("Month code should be in 3 character code.");
        }
        TruffleString numberPart = Strings.substring(ctx, (TruffleString) monthCode, 1);
        double numberPart2 = JSRuntime.doubleValue(toIntegerOrInfinity.executeNumber(numberPart));

        if (Double.isNaN(numberPart2)) {
            throw Errors.createRangeError("The last character of the monthCode should be a number.");
        }
        if (numberPart2 < 1 || numberPart2 > 12) {
            throw Errors.createRangeError("monthCode out of bounds");
        }

        double m1 = TemporalUtil.isNullish(month) ? -1 : JSRuntime.doubleValue(toIntegerOrInfinity.executeNumber(month));

        if (!TemporalUtil.isNullish(month) && m1 != numberPart2) {
            throw Errors.createRangeError("Month does not equal the month code.");
        }
        if (!identicalNode.executeBoolean(monthCode, TemporalUtil.buildISOMonthCode((int) numberPart2))) {
            throw Errors.createRangeError("Not same value");
        }

        return (long) numberPart2;
    }

    // 12.1.39
    public static JSTemporalDateTimeRecord isoDateFromFields(DynamicObject fields, DynamicObject options, JSContext ctx, IsObjectNode isObject,
                    JSToBooleanNode toBoolean, JSToNumberNode toNumber, JSToStringNode toStringNode, JSToIntegerOrInfinityNode toIntOrInfinityNode,
                    JSIdenticalNode identicalNode) {
        assert isObject.executeBoolean(fields);
        Overflow overflow = TemporalUtil.toTemporalOverflow(options, toBoolean, toNumber, toStringNode);
        DynamicObject preparedFields = TemporalUtil.prepareTemporalFields(ctx, fields, TemporalUtil.listDMMCY, TemporalUtil.listEmpty);
        Object year = JSObject.get(preparedFields, YEAR);
        if (year == Undefined.instance) {
            throw TemporalErrors.createTypeErrorTemporalYearNotPresent();
        }
        Object month = resolveISOMonth(ctx, preparedFields, toIntOrInfinityNode, identicalNode);
        Object day = JSObject.get(preparedFields, DAY);
        if (day == Undefined.instance) {
            throw TemporalErrors.createTypeErrorTemporalDayNotPresent();
        }
        return TemporalUtil.regulateISODate(dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(year))), dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(month))),
                        dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(day))), overflow);
    }

    // 12.1.40
    public static JSTemporalYearMonthDayRecord isoYearMonthFromFields(DynamicObject fields, DynamicObject options, JSContext ctx, IsObjectNode isObject,
                    JSToBooleanNode toBoolean, JSToNumberNode toNumber, JSToStringNode toStringNode, JSToIntegerOrInfinityNode toIntOrInfinityNode,
                    JSIdenticalNode identicalNode) {
        assert isObject.executeBoolean(fields);
        Overflow overflow = TemporalUtil.toTemporalOverflow(options, toBoolean, toNumber, toStringNode);
        DynamicObject preparedFields = TemporalUtil.prepareTemporalFields(ctx, fields, TemporalUtil.listMMCY, TemporalUtil.listEmpty);
        Object year = JSObject.get(preparedFields, YEAR);
        if (year == Undefined.instance) {
            throw TemporalErrors.createTypeErrorTemporalYearNotPresent();
        }
        Object month = resolveISOMonth(ctx, preparedFields, toIntOrInfinityNode, identicalNode);

        JSTemporalYearMonthDayRecord result = TemporalUtil.regulateISOYearMonth(dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(year))),
                        dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(month))), overflow);
        return JSTemporalYearMonthDayRecord.create(result.getYear(), result.getMonth(), 1);
    }

    public static JSTemporalYearMonthDayRecord isoMonthDayFromFields(DynamicObject fields, DynamicObject options, JSContext ctx, IsObjectNode isObject,
                    JSToBooleanNode toBoolean, JSToNumberNode toNumber, JSToStringNode toStringNode, JSToIntegerOrInfinityNode toIntOrInfinityNode, JSIdenticalNode identicalNode) {
        assert isObject.executeBoolean(fields);
        Overflow overflow = TemporalUtil.toTemporalOverflow(options, toBoolean, toNumber, toStringNode);
        DynamicObject preparedFields = TemporalUtil.prepareTemporalFields(ctx, fields, TemporalUtil.listDMMCY, TemporalUtil.listEmpty);
        Object month = JSObject.get(preparedFields, MONTH);
        Object monthCode = JSObject.get(preparedFields, MONTH_CODE);
        Object year = JSObject.get(preparedFields, YEAR);
        if (!TemporalUtil.isNullish(month) && TemporalUtil.isNullish(monthCode) && TemporalUtil.isNullish(year)) {
            throw Errors.createTypeError("A year or a month code should be present.");
        }
        month = resolveISOMonth(ctx, preparedFields, toIntOrInfinityNode, identicalNode);
        Object day = JSObject.get(preparedFields, DAY);
        if (day == Undefined.instance) {
            throw Errors.createTypeError("Day not present.");
        }
        int referenceISOYear = 1972;
        JSTemporalDateTimeRecord result = null;
        if (monthCode == Undefined.instance) {
            result = TemporalUtil.regulateISODate(dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(year))), dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(month))),
                            dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(day))), overflow);
        } else {
            result = TemporalUtil.regulateISODate(referenceISOYear, dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(month))),
                            dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(day))), overflow);
        }
        return JSTemporalYearMonthDayRecord.create(referenceISOYear, result.getMonth(), result.getDay());
    }

    // 12.1.45
    public static long isoDay(DynamicObject temporalObject) {
        TemporalDay day = (TemporalDay) temporalObject;
        return day.getDay();
    }

    // TODO ultimately, dtoi should probably throw instead of having an assertion
    // Legitimate uses are in the Duration area, elsewhere it could be missing cleanup
    public static long dtol(double d) {
        assert JSRuntime.doubleIsRepresentableAsLong(d);
        return (long) d;
    }

    // TODO ultimately, dtoi should probably throw instead of having an assertion
    public static int dtoi(double d) {
        if (d == 0) {
            // ignore -0.0
            return 0;
        }
        assert JSRuntime.doubleIsRepresentableAsInt(d);
        return (int) d;
    }

    @TruffleBoundary
    public static BigInteger dtobi(double d) {
        assert doubleIsInteger(d);
        // JSNumberToBigInt is an alternative, but not giving the exact same result
        return new BigDecimal(d).toBigInteger();
    }

    @TruffleBoundary
    public static long dtol(double d, boolean failOnError) {
        if (failOnError && !JSRuntime.doubleIsRepresentableAsLong(d)) {
            throw Errors.createRangeError("value out of range");
        }
        return (long) d;
    }

    // in contrast to `dtol`, set to Long.MAX_VALUE/MIN_VALUE if outside range.
    // later operations either CONSTRAIN or REJECT anyway!
    @TruffleBoundary
    public static long dtolConstrain(double d) {
        if (!JSRuntime.doubleIsRepresentableAsLong(d)) {
            return d > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        return (long) d;
    }

    // in contrast to `dtoi`, set to Integer.MAX_VALUE/MIN_VALUE if outside range.
    // later operations either CONSTRAIN or REJECT anyway!
    @TruffleBoundary
    public static int dtoiConstrain(double d) {
        if (!JSRuntime.doubleIsRepresentableAsInt(d)) {
            return d > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        }
        return (int) d;
    }

    // always fails if long does not fit into int
    @TruffleBoundary
    public static int ltoi(long l) {
        if (!JSRuntime.longIsRepresentableAsInt(l)) {
            throw Errors.createRangeError("value out of range");
        }
        return (int) l;
    }

    @TruffleBoundary
    public static int bitoi(BigInteger bi) {
        double value = bi.doubleValue();
        assert Double.isFinite(value);
        assert JSRuntime.doubleIsRepresentableAsInt(value);
        return bi.intValue();
    }

    @TruffleBoundary
    public static double bitod(BigInteger bi) {
        double value = bi.doubleValue();
        assert Double.isFinite(value);
        return value;
    }

    @TruffleBoundary
    public static long bitol(BigInteger bi) {
        long value = bi.longValueExact(); // throws!
        return value;
    }

    @TruffleBoundary
    public static long bigIntToLong(BigInt val) {
        return val.longValueExact(); // throws
    }

    @TruffleBoundary
    private static int add(int a, int b, Overflow overflow) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException ex) {
            if (overflow == Overflow.REJECT) {
                throw TemporalErrors.createRangeErrorDateOutsideRange();
            } else {
                assert overflow == Overflow.CONSTRAIN;
                return Integer.MAX_VALUE;
            }
        }
    }

    public static JSTemporalDurationRecord createNegatedTemporalDuration(JSTemporalDurationRecord d) {
        return d.copyNegated();
    }

    @TruffleBoundary
    public static Unit toUnit(TruffleString unit) {
        if (isNullish(unit)) {
            return Unit.EMPTY;
        } else if (unit.equals(YEAR)) {
            return Unit.YEAR;
        } else if (unit.equals(MONTH)) {
            return Unit.MONTH;
        } else if (unit.equals(WEEK)) {
            return Unit.WEEK;
        } else if (unit.equals(DAY)) {
            return Unit.DAY;
        } else if (unit.equals(HOUR)) {
            return Unit.HOUR;
        } else if (unit.equals(MINUTE)) {
            return Unit.MINUTE;
        } else if (unit.equals(SECOND)) {
            return Unit.SECOND;
        } else if (unit.equals(MILLISECOND)) {
            return Unit.MILLISECOND;
        } else if (unit.equals(MICROSECOND)) {
            return Unit.MICROSECOND;
        } else if (unit.equals(NANOSECOND)) {
            return Unit.NANOSECOND;
        } else if (unit.equals(AUTO)) {
            return Unit.AUTO;
        }
        throw Errors.createTypeError("unexpected unit");
    }

    @TruffleBoundary
    public static RoundingMode toRoundingMode(TruffleString mode) {
        if (isNullish(mode)) {
            return RoundingMode.EMPTY;
        } else if (mode.equals(FLOOR)) {
            return RoundingMode.FLOOR;
        } else if (mode.equals(CEIL)) {
            return RoundingMode.CEIL;
        } else if (mode.equals(HALF_EXPAND)) {
            return RoundingMode.HALF_EXPAND;
        } else if (mode.equals(TRUNC)) {
            return RoundingMode.TRUNC;
        }
        throw Errors.createTypeError("unexpected roundingMode");
    }

    @TruffleBoundary
    public static Disambiguation toDisambiguation(TruffleString disambiguation) {
        if (disambiguation.equals(EARLIER)) {
            return Disambiguation.EARLIER;
        } else if (disambiguation.equals(LATER)) {
            return Disambiguation.LATER;
        } else if (disambiguation.equals(COMPATIBLE)) {
            return Disambiguation.COMPATIBLE;
        } else if (disambiguation.equals(REJECT)) {
            return Disambiguation.REJECT;
        }
        throw Errors.createTypeError("unexpected disambiguation");
    }

    @TruffleBoundary
    public static OffsetOption toOffsetOption(TruffleString offsetOption) {
        if (offsetOption.equals(USE)) {
            return OffsetOption.USE;
        } else if (offsetOption.equals(IGNORE)) {
            return OffsetOption.IGNORE;
        } else if (offsetOption.equals(PREFER)) {
            return OffsetOption.PREFER;
        } else if (offsetOption.equals(REJECT)) {
            return OffsetOption.REJECT;
        }
        throw Errors.createTypeError("unexpected offsetOption");
    }

    @TruffleBoundary
    public static ShowCalendar toShowCalendar(TruffleString showCalendar) {
        if (showCalendar.equals(AUTO)) {
            return ShowCalendar.AUTO;
        } else if (showCalendar.equals(NEVER)) {
            return ShowCalendar.NEVER;
        } else if (showCalendar.equals(ALWAYS)) {
            return ShowCalendar.ALWAYS;
        }
        throw Errors.createTypeError("unexpected showCalendar");
    }
}
