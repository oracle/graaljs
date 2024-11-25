/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.ExactMath;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerOrInfinityNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerThrowOnInfinityNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerWithoutRoundingNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarDateFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarGetterNode;
import com.oracle.truffle.js.nodes.temporal.TemporalDurationAddNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
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
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonthObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZone;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZoneObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZoneRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalYearMonthDayRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDay;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalMonth;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalYear;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class TemporalUtil {

    private static final Function<Object, Object> toIntegerThrowOnInfinity = TemporalUtil::toIntegerThrowOnInfinity;
    private static final Function<Object, Object> toPositiveInteger = TemporalUtil::toPositiveInteger;
    private static final Function<Object, Object> toString = JSRuntime::toString;

    public static final Set<TruffleString> pluralUnits = Set.of(YEARS, MONTHS, WEEKS, DAYS, HOURS, MINUTES, SECONDS,
                    MILLISECONDS, MICROSECONDS, NANOSECONDS);
    public static final Map<TruffleString, TruffleString> pluralToSingular = Map.ofEntries(
                    Map.entry(YEARS, YEAR),
                    Map.entry(MONTHS, MONTH),
                    Map.entry(WEEKS, WEEK),
                    Map.entry(DAYS, DAY),
                    Map.entry(HOURS, HOUR),
                    Map.entry(MINUTES, MINUTE),
                    Map.entry(SECONDS, SECOND),
                    Map.entry(MILLISECONDS, MILLISECOND),
                    Map.entry(MICROSECONDS, MICROSECOND),
                    Map.entry(NANOSECONDS, NANOSECOND));

    private static final Map<TruffleString, Function<Object, Object>> temporalFieldConversion = Map.ofEntries(
                    Map.entry(YEAR, toIntegerThrowOnInfinity),
                    Map.entry(MONTH, toPositiveInteger),
                    Map.entry(MONTH_CODE, toString),
                    Map.entry(DAY, toPositiveInteger),
                    Map.entry(HOUR, toIntegerThrowOnInfinity),
                    Map.entry(MINUTE, toIntegerThrowOnInfinity),
                    Map.entry(SECOND, toIntegerThrowOnInfinity),
                    Map.entry(MILLISECOND, toIntegerThrowOnInfinity),
                    Map.entry(MICROSECOND, toIntegerThrowOnInfinity),
                    Map.entry(NANOSECOND, toIntegerThrowOnInfinity),
                    Map.entry(OFFSET, toString),
                    Map.entry(ERA, toString),
                    Map.entry(ERA_YEAR, toIntegerThrowOnInfinity));

    public static final Map<TruffleString, Object> temporalFieldDefaults = Map.ofEntries(
                    Map.entry(YEAR, Undefined.instance),
                    Map.entry(MONTH, Undefined.instance),
                    Map.entry(MONTH_CODE, Undefined.instance),
                    Map.entry(DAY, Undefined.instance),
                    Map.entry(HOUR, 0),
                    Map.entry(MINUTE, 0),
                    Map.entry(SECOND, 0),
                    Map.entry(MILLISECOND, 0),
                    Map.entry(MICROSECOND, 0),
                    Map.entry(NANOSECOND, 0),
                    Map.entry(YEARS, 0),
                    Map.entry(MONTHS, 0),
                    Map.entry(WEEKS, 0),
                    Map.entry(DAYS, 0),
                    Map.entry(HOURS, 0),
                    Map.entry(MINUTES, 0),
                    Map.entry(SECONDS, 0),
                    Map.entry(MILLISECONDS, 0),
                    Map.entry(MICROSECONDS, 0),
                    Map.entry(NANOSECONDS, 0),
                    Map.entry(OFFSET, Undefined.instance),
                    Map.entry(ERA, Undefined.instance),
                    Map.entry(ERA_YEAR, Undefined.instance));

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
    public static final List<TruffleString> listYD = List.of(YEAR, DAY);
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
    public static final UnitPlural[] DURATION_PROPERTIES = new UnitPlural[]{UnitPlural.DAYS, UnitPlural.HOURS, UnitPlural.MICROSECONDS, UnitPlural.MILLISECONDS,
                    UnitPlural.MINUTES, UnitPlural.MONTHS, UnitPlural.NANOSECONDS, UnitPlural.SECONDS, UnitPlural.WEEKS, UnitPlural.YEARS};

    private static final BigInt upperEpochNSLimit = new BigInt(BigInteger.valueOf(86400).multiply(BigInteger.valueOf(10).pow(17)));
    private static final BigInt lowerEpochNSLimit = upperEpochNSLimit.negate();

    // 8.64* 10^21 + 8.64 * 10^13; roughly 273,000 years
    private static final BigInteger isoTimeUpperBound = new BigInteger("8640000086400000000000");
    private static final BigInteger isoTimeLowerBound = isoTimeUpperBound.negate();
    private static final int isoTimeBoundYears = 270000;

    // 8.64 * 10^13
    private static final BigInteger BI_8_64_13 = new BigInteger("86400000000000");

    public static final BigInteger BI_36_10_POW_11 = new BigInteger("3600000000000");
    public static final BigInteger BI_6_10_POW_10 = new BigInteger("60000000000");
    public static final BigInteger BI_10_POW_9 = new BigInteger("1000000000"); // 10 ^ 9
    public static final BigInteger BI_10_POW_6 = new BigInteger("1000000"); // 10 ^ 6
    public static final BigInteger BI_1000 = new BigInteger("1000");  // 10 ^ 3

    public static final BigDecimal BD_10 = new BigDecimal("10");
    public static final BigDecimal BD_60 = new BigDecimal("60");
    public static final BigDecimal BD_1000 = new BigDecimal("1000");
    public static final BigDecimal BD_10_POW_M_3 = new BigDecimal("0.001");
    public static final BigDecimal BD_10_POW_M_6 = new BigDecimal("0.000001");
    public static final BigDecimal BD_10_POW_M_9 = new BigDecimal("0.000000001");

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

    public static final int HOURS_PER_DAY = 24;
    public static final int MINUTES_PER_HOUR = 60;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final double MS_PER_DAY = 8.64 * 10_000_000;
    public static final double NS_PER_DAY = 8.64 * 10_000_000_000_000D;

    public static final int SINCE = -1;
    public static final int UNTIL = 1;

    public static final int SUBTRACT = -1;
    public static final int ADD = 1;

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
                case NUMBER_AND_STRING:
                    return STRING;
                case NUMBER:
                    return NUMBER;
                case BOOLEAN:
                    return BOOLEAN;
            }
            throw Errors.shouldNotReachHere();
        }
    }

    public enum Unit {
        EMPTY(Strings.EMPTY_STRING),
        AUTO(TemporalConstants.AUTO),
        YEAR(TemporalConstants.YEAR),
        MONTH(TemporalConstants.MONTH),
        WEEK(TemporalConstants.WEEK),
        DAY(TemporalConstants.DAY),
        HOUR(TemporalConstants.HOUR),
        MINUTE(TemporalConstants.MINUTE),
        SECOND(TemporalConstants.SECOND),
        MILLISECOND(TemporalConstants.MILLISECOND),
        MICROSECOND(TemporalConstants.MICROSECOND),
        NANOSECOND(TemporalConstants.NANOSECOND);

        private final TruffleString name;

        Unit(TruffleString name) {
            this.name = name;
        }

        public TruffleString toTruffleString() {
            return name;
        }
    }

    public enum UnitPlural {
        YEARS(TemporalConstants.YEARS),
        MONTHS(TemporalConstants.MONTHS),
        WEEKS(TemporalConstants.WEEKS),
        DAYS(TemporalConstants.DAYS),
        HOURS(TemporalConstants.HOURS),
        MINUTES(TemporalConstants.MINUTES),
        SECONDS(TemporalConstants.SECONDS),
        MILLISECONDS(TemporalConstants.MILLISECONDS),
        MICROSECONDS(TemporalConstants.MICROSECONDS),
        NANOSECONDS(TemporalConstants.NANOSECONDS);

        private final TruffleString name;

        UnitPlural(TruffleString name) {
            this.name = name;
        }

        public TruffleString toTruffleString() {
            return name;
        }
    }

    public enum RoundingMode {
        EMPTY,
        CEIL,
        FLOOR,
        EXPAND,
        TRUNC,
        HALF_EXPAND,
        HALF_TRUNC,
        HALF_EVEN,
        HALF_FLOOR,
        HALF_CEIL
    }

    public enum UnsignedRoundingMode {
        EMPTY,
        ZERO,
        INFINITY,
        HALF_INFINITY,
        HALF_ZERO,
        HALF_EVEN
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
    public static double getNumberOption(JSDynamicObject options, TruffleString property, double minimum, double maximum,
                    double fallback, IsObjectNode isObject,
                    JSToNumberNode numberNode) {
        assert isObject.executeBoolean(options);
        Object value = JSObject.get(options, property);
        return defaultNumberOptions(value, minimum, maximum, fallback, numberNode);
    }

    // 13.5
    public static Object getStringOrNumberOption(JSDynamicObject options, TruffleString property, List<TruffleString> stringValues,
                    double minimum, double maximum, Object fallback, JSToStringNode toStringNode, TemporalGetOptionNode getOptionNode) {
        assert JSRuntime.isObject(options);
        Object value = getOptionNode.execute(options, property, OptionType.NUMBER_AND_STRING, null, fallback);
        if (value instanceof Number) {
            double numberValue = JSRuntime.doubleValue((Number) value);
            if (Double.isNaN(numberValue) || numberValue < minimum || numberValue > maximum) {
                throw Errors.createRangeError("Numeric value out of range.");
            }
            return Math.floor(numberValue);
        }
        value = toStringNode.executeString(value);
        if (stringValues != null && !Boundaries.listContainsUnchecked(stringValues, value)) {
            throw Errors.createRangeError("Given string value is not in string values");
        }
        return value;
    }

    // 13.17
    public static double toTemporalRoundingIncrement(JSDynamicObject options, Double dividend, boolean inclusive,
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

    public static JSTemporalPrecisionRecord toSecondsStringPrecision(JSDynamicObject options, JSToStringNode toStringNode, TemporalGetOptionNode getOptionNode, TruffleString.EqualNode equalNode) {
        Unit smallestUnit = toSmallestTemporalUnit(options, listYMWDH, null, getOptionNode, equalNode);

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

        Object digits = getStringOrNumberOption(options, FRACTIONAL_SECOND_DIGITS, listAuto, 0, 9, AUTO, toStringNode, getOptionNode);
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

    public static Unit toSmallestTemporalUnit(JSDynamicObject normalizedOptions, List<TruffleString> disallowedUnits, TruffleString fallback, TemporalGetOptionNode getOptionNode,
                    TruffleString.EqualNode equalNode) {
        TruffleString smallestUnit = (TruffleString) getOptionNode.execute(normalizedOptions, SMALLEST_UNIT, OptionType.STRING, listAllDateTime, fallback);
        if (smallestUnit != null && Boundaries.setContains(pluralUnits, smallestUnit)) {
            smallestUnit = Boundaries.mapGet(pluralToSingular, smallestUnit);
        }
        if (smallestUnit != null && Boundaries.listContains(disallowedUnits, smallestUnit)) {
            throw Errors.createRangeError("Smallest unit not allowed.");
        }
        return toUnit(smallestUnit, equalNode);
    }

    @TruffleBoundary
    public static JSTemporalZonedDateTimeRecord parseTemporalRelativeToString(TruffleString isoString) {
        if (!(new TemporalParser(isoString)).isTemporalDateTimeString()) {
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
            return parseISODateTimeIntl(string, rec);
        }
        throw Errors.createRangeError("cannot parse the ISO date time string");
    }

    private static JSTemporalDateTimeRecord parseISODateTimeIntl(TruffleString string, JSTemporalParserRecord rec) {
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

    public static void validateTemporalUnitRange(Unit largestUnit, Unit smallestUnit) {
        boolean error = false;
        switch (smallestUnit) {
            case YEAR:
                if (!(largestUnit == Unit.YEAR)) {
                    error = true;
                }
                break;
            case MONTH:
                if (!(largestUnit == Unit.YEAR || largestUnit == Unit.MONTH)) {
                    error = true;
                }
                break;
            case WEEK:
                if (!(largestUnit == Unit.YEAR || largestUnit == Unit.MONTH || largestUnit == Unit.WEEK)) {
                    error = true;
                }
                break;
            case DAY:
                if (!(largestUnit == Unit.YEAR || largestUnit == Unit.MONTH || largestUnit == Unit.WEEK || largestUnit == Unit.DAY)) {
                    error = true;
                }
                break;
            case HOUR:
                if (!(largestUnit == Unit.YEAR || largestUnit == Unit.MONTH || largestUnit == Unit.WEEK || largestUnit == Unit.DAY || largestUnit == Unit.HOUR)) {
                    error = true;
                }
                break;
            case MINUTE:
                if (largestUnit == Unit.SECOND || largestUnit == Unit.MILLISECOND || largestUnit == Unit.MICROSECOND || largestUnit == Unit.NANOSECOND) {
                    error = true;
                }
                break;
            case SECOND:
                if (largestUnit == Unit.MILLISECOND || largestUnit == Unit.MICROSECOND || largestUnit == Unit.NANOSECOND) {
                    error = true;
                }
                break;
            case MILLISECOND:
                if (largestUnit == Unit.MICROSECOND || largestUnit == Unit.NANOSECOND) {
                    error = true;
                }
                break;
            case MICROSECOND:
                if (largestUnit == Unit.NANOSECOND) {
                    error = true;
                }
                break;
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
        TruffleString secondString = Strings.concat(Strings.COLON, toZeroPaddedDecimalString(second, 2));
        long fraction = (millisecond * 1_000_000) + (microsecond * 1_000) + nanosecond;
        TruffleString fractionString = Strings.EMPTY_STRING;
        if (precision.equals(AUTO)) {
            if (fraction == 0) {
                return secondString;
            }
            fractionString = Strings.concatAll(fractionString,
                            toZeroPaddedDecimalString(millisecond, 3),
                            toZeroPaddedDecimalString(microsecond, 3),
                            toZeroPaddedDecimalString(nanosecond, 3));
            fractionString = longestSubstring(fractionString);
        } else {
            if (precision.equals(0)) {
                return secondString;
            }
            fractionString = Strings.concatAll(fractionString,
                            toZeroPaddedDecimalString(millisecond, 3),
                            toZeroPaddedDecimalString(microsecond, 3),
                            toZeroPaddedDecimalString(nanosecond, 3));
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

    public static UnsignedRoundingMode getUnsignedRoundingMode(RoundingMode rm, boolean isNegative) {
        switch (rm) {
            case CEIL:
                return isNegative ? UnsignedRoundingMode.ZERO : UnsignedRoundingMode.INFINITY;
            case FLOOR:
                return isNegative ? UnsignedRoundingMode.INFINITY : UnsignedRoundingMode.ZERO;
            case EXPAND:
                return UnsignedRoundingMode.INFINITY;
            case TRUNC:
                return UnsignedRoundingMode.ZERO;
            case HALF_CEIL:
                return isNegative ? UnsignedRoundingMode.HALF_ZERO : UnsignedRoundingMode.HALF_INFINITY;
            case HALF_FLOOR:
                return isNegative ? UnsignedRoundingMode.HALF_INFINITY : UnsignedRoundingMode.HALF_ZERO;
            case HALF_EXPAND:
                return UnsignedRoundingMode.HALF_INFINITY;
            case HALF_TRUNC:
                return UnsignedRoundingMode.HALF_ZERO;
            case HALF_EVEN:
                return UnsignedRoundingMode.HALF_EVEN;
        }
        return UnsignedRoundingMode.EMPTY;
    }

    public static double applyUnsignedRoundingMode(double x, double r1, double r2, UnsignedRoundingMode urm) {
        if (x == r1) {
            return r1;
        }
        assert r1 < x && x < r2;
        assert urm != UnsignedRoundingMode.EMPTY;
        if (urm == UnsignedRoundingMode.ZERO) {
            return r1;
        }
        if (urm == UnsignedRoundingMode.INFINITY) {
            return r2;
        }
        double d1 = x - r1;
        double d2 = r2 - x;
        if (d1 < d2) {
            return r1;
        }
        if (d2 < d1) {
            return r2;
        }
        assert d1 == d2;
        if (urm == UnsignedRoundingMode.HALF_ZERO) {
            return r1;
        }
        if (urm == UnsignedRoundingMode.HALF_INFINITY) {
            return r2;
        }
        assert urm == UnsignedRoundingMode.HALF_EVEN;
        double cardinality = (r1 / (r2 - r1)) % 2;
        if (cardinality == 0) {
            return r1;
        }
        return r2;
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
        if (id == null) {
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

    public static int toPositiveIntegerConstrainInt(Object value, JSToIntegerThrowOnInfinityNode toIntegerThrowOnInfinityNode, Node node, InlinedBranchProfile errorBranch) {
        int integer = toIntegerThrowOnInfinityNode.executeIntOrThrow(value);
        if (integer <= 0) {
            errorBranch.enter(node);
            throw Errors.createRangeError("positive value expected");
        }
        return integer;
    }

    // 13.52
    @TruffleBoundary
    public static JSObject prepareTemporalFields(JSContext ctx, JSDynamicObject fields, List<TruffleString> fieldNames, List<TruffleString> requiredFields) {
        JSObject result = JSOrdinary.createWithNullPrototype(ctx);
        for (TruffleString property : fieldNames) {
            Object value = JSObject.get(fields, property);
            assert value != null;
            if (value == Undefined.instance) {
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
    public static JSObject preparePartialTemporalFields(JSContext ctx, JSDynamicObject fields, List<TruffleString> fieldNames) {
        JSObject result = JSOrdinary.createWithNullPrototype(ctx);
        boolean any = false;
        for (TruffleString property : fieldNames) {
            Object value = JSObject.get(fields, property);
            assert value != null;
            if (value != Undefined.instance) {
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

    public static int isoDaysInYear(int year) {
        if (isISOLeapYear(year)) {
            return 366;
        }
        return 365;
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
        if (year == Integer.MAX_VALUE || year == Integer.MIN_VALUE || month == Integer.MAX_VALUE || month == Integer.MIN_VALUE) {
            throw Errors.createRangeError("value out of range");
        }

        int yearPrepared = (int) (year + Math.floor((month - 1.0) / 12.0));
        int monthPrepared = (int) nonNegativeModulo(month - 1, 12) + 1;

        return JSTemporalDateTimeRecord.create(yearPrepared, monthPrepared, 0, 0, 0, 0, 0, 0, 0);
    }

    @TruffleBoundary // one instead of three boundaries
    public static boolean isBuiltinCalendar(TruffleString id) {
        return id.equals(ISO8601) || id.equals(GREGORY) || id.equals(JAPANESE);
    }

    public static JSTemporalCalendarObject getISO8601Calendar(JSContext ctx, JSRealm realm) {
        return getBuiltinCalendar(ISO8601, ctx, realm);
    }

    public static JSTemporalCalendarObject getBuiltinCalendar(TruffleString id, JSContext ctx, JSRealm realm) {
        assert isBuiltinCalendar(id) : id;
        return JSTemporalCalendar.create(ctx, realm, id);
    }

    @TruffleBoundary
    public static List<TruffleString> iterableToListOfTypeString(JSDynamicObject items) {
        IteratorRecord iter = JSRuntime.getIterator(items /* , sync */);
        List<TruffleString> values = new ArrayList<>();
        Object next = Boolean.TRUE;
        while (next != Boolean.FALSE) {
            next = JSRuntime.iteratorStep(iter);
            if (next != Boolean.FALSE) {
                Object nextValue = JSRuntime.iteratorValue(next);
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

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalDateTimeString(TruffleString string) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseCalendarDateTime();
        if (rec == null) {
            throw Errors.createRangeError("cannot parse the date string");
        }
        if (rec.getZ()) {
            throw TemporalErrors.createRangeErrorUnexpectedUTCDesignator();
        }
        JSTemporalDateTimeRecord result = parseISODateTime(string, true, false);
        return result;
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalDateString(TruffleString string) {
        JSTemporalDateTimeRecord rec = parseTemporalDateTimeString(string);
        return JSTemporalDateTimeRecord.createCalendar(rec.getYear(), rec.getMonth(), rec.getDay(), 0, 0, 0, 0, 0, 0, rec.getCalendar());
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
        TruffleString monthCode = toZeroPaddedDecimalString(month, 2);
        return Strings.concat(TemporalConstants.M, monthCode);
    }

    public static JSTemporalTimeZoneObject createTemporalTimeZone(JSContext ctx, JSRealm realm, TruffleString identifier) {
        return createTemporalTimeZone(ctx, realm, ctx.getTemporalTimeZoneFactory().getPrototype(realm), identifier);
    }

    public static JSTemporalTimeZoneObject createTemporalTimeZone(JSContext ctx, JSRealm realm, JSDynamicObject proto, TruffleString identifier) {
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
        return JSTemporalTimeZone.create(ctx, realm, proto, offsetNs, newIdentifier);
    }

    public static TruffleString canonicalizeTimeZoneName(TruffleString timeZone) {
        assert isValidTimeZoneName(timeZone);
        return Strings.fromJavaString(JSDateTimeFormat.canonicalizeTimeZoneName(timeZone));
    }

    public static boolean isValidTimeZoneName(TruffleString timeZone) {
        return JSDateTimeFormat.canonicalizeTimeZoneName(timeZone) != null;
    }

    @TruffleBoundary
    public static double getDouble(JSDynamicObject ob, TruffleString key, double defaultValue) {
        Object value = JSObject.get(ob, key);
        if (value == Undefined.instance) {
            return defaultValue;
        }
        Number n = (Number) value;
        return n.longValue();
    }

    public static boolean isoDateTimeWithinLimits(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond) {
        if (-isoTimeBoundYears <= year && year <= isoTimeBoundYears) {
            // fastpath check
            return true;
        } else {
            return isoDateTimeWithinLimitsIntl(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
        }
    }

    @TruffleBoundary
    private static boolean isoDateTimeWithinLimitsIntl(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond) {
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
        if (Double.isNaN(ms)) {
            throw TemporalErrors.createRangeErrorDateOutsideRange();
        }
        assert isFinite(ms);

        BigInteger bi = BigInteger.valueOf((long) ms).multiply(BI_10_POW_6);
        BigInteger bims = BigInteger.valueOf(microsecond).multiply(BI_1000);
        BigInteger biresult = bi.add(bims).add(BigInteger.valueOf(nanosecond));

        return biresult;
    }

    private static boolean isFinite(double d) {
        return !(Double.isNaN(d) || Double.isInfinite(d));
    }

    public static Overflow toTemporalOverflow(JSDynamicObject options, TemporalGetOptionNode getOptionNode) {
        if (options == Undefined.instance) {
            return Overflow.CONSTRAIN;
        }
        TruffleString result = (TruffleString) getOptionNode.execute(options, OVERFLOW, OptionType.STRING, listConstrainReject, CONSTRAIN);
        return toOverflow(result);
    }

    @TruffleBoundary
    private static Overflow toOverflow(TruffleString result) {
        if (CONSTRAIN.equals(result)) {
            return Overflow.CONSTRAIN;
        } else if (TemporalConstants.REJECT.equals(result)) {
            return Overflow.REJECT;
        }
        CompilerDirectives.transferToInterpreter();
        throw Errors.shouldNotReachHere("unknown overflow type: " + result);
    }

    public static JSTemporalDateTimeRecord interpretTemporalDateTimeFields(JSDynamicObject calendar, JSDynamicObject fields, JSDynamicObject options, TemporalGetOptionNode getOptionNode,
                    TemporalCalendarDateFromFieldsNode dateFromFieldsNode) {
        JSTemporalDateTimeRecord timeResult = toTemporalTimeRecord(fields);
        JSTemporalPlainDateObject date = dateFromFieldsNode.execute(calendar, fields, options);
        Overflow overflow = toTemporalOverflow(options, getOptionNode);
        JSTemporalDurationRecord timeResult2 = regulateTime(
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
            if (!isValidTime(dtoi(hours), dtoi(minutes), dtoi(seconds), dtoi(milliseconds), dtoi(microseconds), dtoi(nanoseconds))) {
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
            if (!isValidTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
                throw Errors.createRangeError("Given time outside the range.");
            }
            return JSTemporalDurationRecord.create(0, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
    }

    public static JSTemporalDurationRecord constrainTime(int hours, int minutes, int seconds, int milliseconds,
                    int microseconds, int nanoseconds) {
        return JSTemporalDurationRecord.create(0, 0, 0,
                        constrainToRange(hours, 0, 23),
                        constrainToRange(minutes, 0, 59),
                        constrainToRange(seconds, 0, 59),
                        constrainToRange(milliseconds, 0, 999),
                        constrainToRange(microseconds, 0, 999),
                        constrainToRange(nanoseconds, 0, 999));
    }

    public static JSTemporalDateTimeRecord toTemporalTimeRecord(JSDynamicObject temporalTimeLike) {
        boolean any = false;

        int hour = 0;
        int minute = 0;
        int second = 0;
        int millisecond = 0;
        int microsecond = 0;
        int nanosecond = 0;

        for (TruffleString property : TIME_LIKE_PROPERTIES) {
            Object val = JSObject.get(temporalTimeLike, property);

            int iVal = 0;
            if (val == Undefined.instance) {
                iVal = 0;
            } else {
                any = true;
                iVal = JSRuntime.intValue(toIntegerThrowOnInfinity(val));
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

    public static JSTemporalPlainDateObject calendarDateAdd(JSDynamicObject calendar, JSDynamicObject datePart, JSDynamicObject dateDuration, JSDynamicObject options) {
        return calendarDateAdd(calendar, datePart, dateDuration, options, Undefined.instance);
    }

    public static JSTemporalPlainDateObject calendarDateAdd(JSDynamicObject calendar, JSDynamicObject date, JSDynamicObject duration, JSDynamicObject options, Object dateAdd) {
        Object dateAddPrepared = dateAdd;
        if (dateAddPrepared == Undefined.instance) {
            dateAddPrepared = JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
        }
        Object addedDate = JSRuntime.call(dateAddPrepared, calendar, new Object[]{date, duration, options});
        return requireTemporalDate(addedDate);
    }

    public static JSTemporalDurationObject calendarDateUntil(JSDynamicObject calendar, JSDynamicObject one, JSDynamicObject two, JSDynamicObject options) {
        return calendarDateUntil(calendar, one, two, options, Undefined.instance);
    }

    public static JSTemporalDurationObject calendarDateUntil(JSDynamicObject calendar, JSDynamicObject one, JSDynamicObject two, JSDynamicObject options, Object dateUntil) {
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

    public static boolean validateISODate(int year, int month, int day) {
        if (month < 1 || month > 12) {
            return false;
        }
        long daysInMonth = isoDaysInMonth(year, month);
        return 1 <= day && day <= daysInMonth;
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord regulateISODate(int yearParam, int monthParam, int dayParam, Overflow overflow) {
        assert overflow == Overflow.CONSTRAIN || overflow == Overflow.REJECT;
        int month = monthParam;
        int day = dayParam;
        if (overflow == Overflow.REJECT) {
            if (!isValidISODate(yearParam, month, day)) {
                throw TemporalErrors.createRangeErrorDateOutsideRange();
            }
        } else {
            assert overflow == Overflow.CONSTRAIN;
            month = constrainToRange(month, 1, 12);
            day = constrainToRange(day, 1, isoDaysInMonth(yearParam, month));
        }
        return JSTemporalDateTimeRecord.create(yearParam, month, day, 0, 0, 0, 0, 0, 0);
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord balanceISODate(int yearParam, int monthParam, int dayParam) {
        double epochDays = JSDate.makeDay(yearParam, monthParam - 1, dayParam);
        assert Double.isFinite(epochDays);
        double ms = JSDate.makeDate(epochDays, 0);
        return JSTemporalPlainDate.toRecord(JSDate.yearFromTime((long) ms), JSDate.monthFromTime(ms) + 1, JSDate.dateFromTime(ms));
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

    public static JSTemporalPlainDateObject requireTemporalDate(Object obj, Node node, InlinedBranchProfile errorBranch) {
        if (!(obj instanceof JSTemporalPlainDateObject)) {
            errorBranch.enter(node);
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
        return (JSTemporalPlainDateObject) obj;
    }

    public static JSTemporalPlainDateObject requireTemporalDate(Object obj) {
        if (!(obj instanceof JSTemporalPlainDateObject)) {
            throw TemporalErrors.createTypeErrorTemporalPlainDateExpected();
        }
        return (JSTemporalPlainDateObject) obj;
    }

    public static JSTemporalDurationObject requireTemporalDuration(Object obj) {
        if (!(obj instanceof JSTemporalDurationObject)) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
        return (JSTemporalDurationObject) obj;
    }

    public static boolean isTemporalZonedDateTime(Object obj) {
        return JSTemporalZonedDateTime.isJSTemporalZonedDateTime(obj);
    }

    public static ShowCalendar toShowCalendarOption(JSDynamicObject options, TemporalGetOptionNode getOptionNode, TruffleString.EqualNode equalNode) {
        return toShowCalendar((TruffleString) getOptionNode.execute(options, CALENDAR_NAME, OptionType.STRING, listAutoAlwaysNever, AUTO), equalNode);
    }

    @TruffleBoundary
    public static TruffleString toZeroPaddedDecimalString(long number, int digits) {
        TruffleString decimalStr = Strings.fromLong(number);
        int length = Strings.length(decimalStr);
        if (length < digits) {
            var sb = TruffleStringBuilderUTF16.createUTF16(digits);
            for (int i = length; i < digits; i++) {
                Strings.builderAppend(sb, '0');
            }
            Strings.builderAppend(sb, decimalStr);
            return Strings.builderToString(sb);
        }
        return decimalStr;
    }

    @TruffleBoundary
    public static TruffleString padISOYear(int year) {
        if (0 <= year && year <= 9999) {
            return toZeroPaddedDecimalString(year, 4);
        }
        TruffleString sign = year > 0 ? Strings.SYMBOL_PLUS : Strings.SYMBOL_MINUS;
        int y = Math.abs(year);
        return Strings.concat(sign, toZeroPaddedDecimalString(y, 6));
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

    public static RoundingMode negateTemporalRoundingMode(RoundingMode roundingMode) {
        if (RoundingMode.CEIL == roundingMode) {
            return RoundingMode.FLOOR;
        } else if (RoundingMode.FLOOR == roundingMode) {
            return RoundingMode.CEIL;
        }
        return roundingMode;
    }

    public static boolean calendarEquals(JSDynamicObject one, JSDynamicObject two, JSToStringNode toStringNode) {
        if (one == two) {
            return true;
        }
        return Boundaries.equals(toStringNode.executeString(one), toStringNode.executeString(two));
    }

    public static void rejectTemporalCalendarType(JSDynamicObject obj, Node node, InlinedBranchProfile errorBranch) {
        if (obj instanceof JSTemporalPlainDateObject || obj instanceof JSTemporalPlainDateTimeObject || obj instanceof JSTemporalPlainMonthDayObject ||
                        obj instanceof JSTemporalPlainTimeObject || obj instanceof JSTemporalPlainYearMonthObject || isTemporalZonedDateTime(obj)) {
            errorBranch.enter(node);
            throw Errors.createTypeError("rejecting calendar types");
        }
    }

    public static double remainder(double x, double y) {
        double magnitude = x % y;
        // assert Math.signum(y) == Math.signum(magnitude);
        return magnitude;
    }

    public static double getPropertyFromRecord(JSTemporalDurationRecord d, UnitPlural unit) {
        switch (unit) {
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

    public static JSDynamicObject calendarMergeFields(JSContext ctx, JSRealm realm, JSDynamicObject calendar, JSDynamicObject fields,
                    JSDynamicObject additionalFields, EnumerableOwnPropertyNamesNode namesNode, Node node, InlinedBranchProfile errorBranch) {
        Object mergeFields = JSObject.getMethod(calendar, TemporalConstants.MERGE_FIELDS);
        if (mergeFields == Undefined.instance) {
            return defaultMergeFields(ctx, realm, fields, additionalFields, namesNode);
        }
        Object result = JSRuntime.call(mergeFields, calendar, new Object[]{fields, additionalFields});
        if (!JSRuntime.isObject(result)) {
            throw TemporalErrors.createTypeErrorObjectExpected();
        }
        return toJSDynamicObject(result, node, errorBranch);
    }

    @TruffleBoundary
    public static JSDynamicObject defaultMergeFields(JSContext ctx, JSRealm realm, JSDynamicObject fields, JSDynamicObject additionalFields, EnumerableOwnPropertyNamesNode namesNode) {
        JSDynamicObject merged = JSOrdinary.create(ctx, realm);
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

    public static void createDataPropertyOrThrow(JSContext ctx, JSDynamicObject obj, TruffleString key, Object value) {
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

    @TruffleBoundary
    public static JSTemporalDurationRecord differenceISODateTime(JSContext ctx, JSRealm realm, EnumerableOwnPropertyNamesNode namesNode, int y1, int mon1, int d1, int h1, int min1, int s1,
                    int ms1, int mus1, int ns1, int y2, int mon2, int d2, int h2, int min2, int s2, int ms2,
                    int mus2, int ns2, JSDynamicObject calendar, Unit largestUnit, JSDynamicObject options) {
        assert options != null;
        JSTemporalDurationRecord timeDifference = differenceTime(h1, min1, s1, ms1, mus1, ns1, h2, min2, s2, ms2, mus2, ns2);

        int timeSign = durationSign(0, 0, 0, timeDifference.getDays(), timeDifference.getHours(), timeDifference.getMinutes(), timeDifference.getSeconds(),
                        timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds());
        int dateSign = compareISODate(y2, mon2, d2, y1, mon1, d1);
        JSTemporalDateTimeRecord balanceResult = balanceISODate(dtoi(y1), dtoi(mon1), dtoi(d1) + dtoi(timeDifference.getDays()));
        if (timeSign == -dateSign) {
            balanceResult = balanceISODate(balanceResult.getYear(), balanceResult.getMonth(), balanceResult.getDay() - timeSign);
            timeDifference = balanceDuration(ctx, realm, namesNode, -timeSign, timeDifference.getHours(),
                            timeDifference.getMinutes(), timeDifference.getSeconds(), timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds(), largestUnit);
        }
        JSDynamicObject date1 = JSTemporalPlainDate.create(ctx, realm, balanceResult.getYear(), balanceResult.getMonth(), balanceResult.getDay(), calendar, null, InlinedBranchProfile.getUncached());
        JSDynamicObject date2 = JSTemporalPlainDate.create(ctx, realm, y2, mon2, d2, calendar, null, InlinedBranchProfile.getUncached());
        Unit dateLargestUnit = largerOfTwoTemporalUnits(Unit.DAY, largestUnit);
        JSDynamicObject untilOptions = mergeLargestUnitOption(ctx, namesNode, options, dateLargestUnit);
        JSTemporalDurationObject dateDifference = calendarDateUntil(calendar, date1, date2, untilOptions, Undefined.instance);
        JSTemporalDurationRecord result = balanceDuration(ctx, realm, namesNode, dateDifference.getDays(), timeDifference.getHours(), timeDifference.getMinutes(), timeDifference.getSeconds(),
                        timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds(), largestUnit);
        return JSTemporalDurationRecord.createWeeks(dateDifference.getYears(), dateDifference.getMonths(), dateDifference.getWeeks(), result.getDays(), result.getHours(), result.getMinutes(),
                        result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds());
    }

    @TruffleBoundary
    public static JSDynamicObject mergeLargestUnitOption(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, JSDynamicObject options, Unit largestUnit) {
        JSDynamicObject merged = JSOrdinary.createWithNullPrototype(ctx);
        UnmodifiableArrayList<?> keys = namesNode.execute(options);
        for (Object nextKey : keys) {
            if (nextKey instanceof TruffleString) {
                TruffleString key = (TruffleString) nextKey;
                Object propValue = JSObject.get(options, key);
                createDataPropertyOrThrow(ctx, merged, key, propValue);
            }
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
    public static JSTemporalDurationRecord balanceDuration(JSContext ctx, JSRealm realm, EnumerableOwnPropertyNamesNode namesNode,
                    double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, Unit largestUnit) {
        return balanceDuration(ctx, realm, namesNode, days, hours, minutes, seconds, milliseconds, microseconds, BigInteger.valueOf(dtol(nanoseconds)), largestUnit, Undefined.instance);
    }

    // nanoseconds can exceed double range, see TemporalDurationHugeTest.testInstantSince
    @TruffleBoundary
    public static JSTemporalDurationRecord balanceDuration(JSContext ctx, JSRealm realm, EnumerableOwnPropertyNamesNode namesNode,
                    double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, BigInteger nanoseconds, Unit largestUnit, JSDynamicObject relativeTo) {
        BigInt nsBi;
        if (isTemporalZonedDateTime(relativeTo)) {
            JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) relativeTo;
            // conversion set to fail. addZonedDateTime creates PlainDateTime that would fail anyway
            BigInt endNs = addZonedDateTime(ctx, realm, zdt.getNanoseconds(), zdt.getTimeZone(), zdt.getCalendar(), 0, 0, 0, dtol(days, true), dtol(hours, true), dtol(minutes, true),
                            dtol(seconds, true), dtol(milliseconds, true), dtol(microseconds, true), nanoseconds, Undefined.instance);
            nsBi = endNs.subtract(zdt.getNanoseconds());
        } else {
            nsBi = new BigInt(totalDurationNanoseconds(days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds));
        }
        double d;
        if (largestUnit == Unit.YEAR || largestUnit == Unit.MONTH || largestUnit == Unit.WEEK || largestUnit == Unit.DAY) {
            JSTemporalNanosecondsDaysRecord result = nanosecondsToDays(ctx, realm, namesNode, nsBi, relativeTo);
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
            BigInteger[] res = nsBi2.divideAndRemainder(BI_1000);
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
            BigInteger[] res = nsBi2.divideAndRemainder(BI_1000);
            mus = bitod(res[0]);
            nsBi2 = res[1];
            ms = Math.floor(mus / 1000.0);
            mus = mus % 1000;
            s = Math.floor(ms / 1000.0);
            ms = ms % 1000;
            min = Math.floor(s / 60.0);
            s = s % 60;
        } else if (largestUnit == Unit.SECOND) {
            BigInteger[] res = nsBi2.divideAndRemainder(BI_1000);
            mus = bitod(res[0]);
            nsBi2 = res[1];
            ms = Math.floor(mus / 1000.0);
            mus = mus % 1000;
            s = Math.floor(ms / 1000.0);
            ms = ms % 1000;
        } else if (largestUnit == Unit.MILLISECOND) {
            BigInteger[] res = nsBi2.divideAndRemainder(BI_1000);
            mus = bitod(res[0]);
            nsBi2 = res[1];
            ms = Math.floor(mus / 1000.0);
            mus = mus % 1000;
        } else if (largestUnit == Unit.MICROSECOND) {
            BigInteger[] res = nsBi2.divideAndRemainder(BI_1000);
            mus = bitod(res[0]);
            nsBi2 = res[1];
        } else {
            assert largestUnit == Unit.NANOSECOND;
        }

        return JSTemporalDurationRecord.create(0, 0, d, h * sign, min * sign, s * sign, ms * sign, mus * sign, sign < 0 ? bitod(nsBi2.negate()) : bitod(nsBi2));
    }

    public static JSDynamicObject toDynamicObject(Object obj) {
        if (obj instanceof JSDynamicObject) {
            return (JSDynamicObject) obj;
        } else {
            throw Errors.createTypeErrorNotAnObject(obj);
        }
    }

    // TODO (GR-32375) for interop support, this needs to detect and convert foreign temporal values
    public static JSDynamicObject toJSDynamicObject(Object item, Node node, InlinedBranchProfile errorBranch) {
        if (item instanceof JSDynamicObject) {
            return (JSDynamicObject) item;
        } else {
            errorBranch.enter(node);
            throw Errors.createTypeError("Interop types not supported in Temporal");
        }
    }

    public static boolean doubleIsInteger(double l) {
        return Math.rint(l) == l;
    }

    public static JSTemporalDurationRecord differenceZonedDateTime(JSContext ctx, JSRealm realm, EnumerableOwnPropertyNamesNode namesNode,
                    BigInt ns1, BigInt ns2, JSDynamicObject timeZone, JSDynamicObject calendar, Unit largestUnit) {
        return differenceZonedDateTime(ctx, realm, namesNode, ns1, ns2, timeZone, calendar, largestUnit, Undefined.instance);
    }

    public static JSTemporalDurationRecord differenceZonedDateTime(JSContext ctx, JSRealm realm, EnumerableOwnPropertyNamesNode namesNode,
                    BigInt ns1, BigInt ns2, JSDynamicObject timeZone, JSDynamicObject calendar, Unit largestUnit, JSDynamicObject options) {
        if (ns1.equals(ns2)) {
            return JSTemporalDurationRecord.createWeeks(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        JSTemporalInstantObject startInstant = JSTemporalInstant.create(ctx, realm, ns1);
        JSTemporalPlainDateTimeObject startDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, realm, timeZone, startInstant, calendar);
        JSTemporalInstantObject endInstant = JSTemporalInstant.create(ctx, realm, ns2);
        JSTemporalPlainDateTimeObject endDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, realm, timeZone, endInstant, calendar);
        JSTemporalDurationRecord dateDifference = differenceISODateTime(ctx, realm, namesNode, startDateTime.getYear(), startDateTime.getMonth(), startDateTime.getDay(), startDateTime.getHour(),
                        startDateTime.getMinute(), startDateTime.getSecond(), startDateTime.getMillisecond(), startDateTime.getMicrosecond(), startDateTime.getNanosecond(),
                        endDateTime.getYear(), endDateTime.getMonth(), endDateTime.getDay(), endDateTime.getHour(), endDateTime.getMinute(), endDateTime.getSecond(),
                        endDateTime.getMillisecond(), endDateTime.getMicrosecond(), endDateTime.getNanosecond(), calendar, largestUnit, options);
        BigInt intermediateNs = addZonedDateTime(ctx, realm, ns1, timeZone, calendar, dtol(dateDifference.getYears()), dtol(dateDifference.getMonths()), dtol(dateDifference.getWeeks()), 0, 0, 0, 0, 0,
                        0, 0);
        BigInt timeRemainderNs = ns2.subtract(intermediateNs);
        JSDynamicObject intermediate = JSTemporalZonedDateTime.create(ctx, realm, intermediateNs, timeZone, calendar);
        JSTemporalNanosecondsDaysRecord result = nanosecondsToDays(ctx, realm, namesNode, timeRemainderNs, intermediate);
        JSTemporalDurationRecord timeDifference = balanceDuration(ctx, realm, namesNode, 0, 0, 0, 0, 0, 0, result.getNanoseconds(), Unit.HOUR, Undefined.instance);
        return JSTemporalDurationRecord.createWeeks(dateDifference.getYears(), dateDifference.getMonths(), dateDifference.getWeeks(), bitod(result.getDays()), timeDifference.getHours(),
                        timeDifference.getMinutes(), timeDifference.getSeconds(), timeDifference.getMilliseconds(), timeDifference.getMicroseconds(), timeDifference.getNanoseconds());
    }

    public static boolean isValidDuration(double years, double months, double weeks, double days, double hours,
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

    public static JSDynamicObject toPartialDuration(Object temporalDurationLike,
                    JSContext ctx, IsObjectNode isObjectNode, JSToIntegerWithoutRoundingNode toInt, Node node, InlinedBranchProfile errorBranch) {
        if (!isObjectNode.executeBoolean(temporalDurationLike)) {
            errorBranch.enter(node);
            throw Errors.createTypeError("Given duration like is not a object.");
        }
        JSDynamicObject temporalDurationLikeObj = toJSDynamicObject(temporalDurationLike, node, errorBranch);
        JSRealm realm = JSRealm.get(null);
        JSDynamicObject result = JSOrdinary.create(ctx, realm);
        boolean any = false;
        for (UnitPlural unit : DURATION_PROPERTIES) {
            Object value = JSObject.get(temporalDurationLikeObj, unit.toTruffleString());
            if (value != Undefined.instance) {
                any = true;
                JSObjectUtil.putDataProperty(result, unit.toTruffleString(), toInt.executeDouble(value));
            }
        }
        if (!any) {
            errorBranch.enter(node);
            throw Errors.createTypeError("Given duration like object has no duration properties.");
        }
        return result;
    }

    @TruffleBoundary
    public static double roundDurationFractionalSecondsSubtract(double seconds, BigDecimal fractionalSeconds) {
        return fractionalSeconds.subtract(BigDecimal.valueOf(seconds)).doubleValue();
    }

    @TruffleBoundary
    public static double roundDurationFractionalDecondsDiv60(BigDecimal fractionalSeconds) {
        return fractionalSeconds.divide(BigDecimal.valueOf(60), mc_20_floor).doubleValue();
    }

    @TruffleBoundary
    public static BigDecimal roundDurationCalculateFractionalSeconds(double seconds, double microseconds, double milliseconds, double nanoseconds) {
        BigDecimal part1 = BigDecimal.valueOf(nanoseconds).multiply(BD_10_POW_M_9);
        BigDecimal part2 = BigDecimal.valueOf(microseconds).multiply(BD_10_POW_M_6);
        BigDecimal part3 = BigDecimal.valueOf(milliseconds).multiply(BD_10_POW_M_3);
        return part1.add(part2).add(part3).add(BigDecimal.valueOf(seconds));
    }

    @TruffleBoundary
    public static JSTemporalNanosecondsDaysRecord nanosecondsToDays(JSContext ctx, JSRealm realm, EnumerableOwnPropertyNamesNode namesNode, BigInt nanosecondsParam, JSDynamicObject relativeTo) {
        BigInteger nanoseconds = nanosecondsParam.bigIntegerValue();
        long sign = nanoseconds.signum();
        BigInteger signBI = BigInteger.valueOf(sign);
        BigInteger dayLengthNs = BI_8_64_13;
        if (sign == 0) {
            return JSTemporalNanosecondsDaysRecord.create(BigInteger.ZERO, BigInteger.ZERO, dayLengthNs);
        }
        if (!isTemporalZonedDateTime(relativeTo)) {
            BigInteger val = nanoseconds.divide(dayLengthNs);
            BigInteger val2 = nanoseconds.abs().mod(dayLengthNs).multiply(signBI);
            return JSTemporalNanosecondsDaysRecord.create(val, val2, dayLengthNs);
        }
        JSTemporalZonedDateTimeObject relativeZDT = (JSTemporalZonedDateTimeObject) relativeTo;
        BigInt startNs = relativeZDT.getNanoseconds();
        JSTemporalInstantObject startInstant = JSTemporalInstant.create(ctx, realm, startNs);
        JSTemporalPlainDateTimeObject startDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, realm, relativeZDT.getTimeZone(), startInstant, relativeZDT.getCalendar());
        BigInt endNs = startNs.add(nanosecondsParam);
        JSTemporalInstantObject endInstant = JSTemporalInstant.create(ctx, realm, endNs);
        JSTemporalPlainDateTimeObject endDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, realm, relativeZDT.getTimeZone(), endInstant,
                        relativeZDT.getCalendar());
        JSTemporalDurationRecord dateDifference = differenceISODateTime(ctx, realm, namesNode, startDateTime.getYear(), startDateTime.getMonth(),
                        startDateTime.getDay(), startDateTime.getHour(), startDateTime.getMinute(), startDateTime.getSecond(), startDateTime.getMillisecond(),
                        startDateTime.getMicrosecond(), startDateTime.getNanosecond(), endDateTime.getYear(), endDateTime.getMonth(), endDateTime.getDay(), endDateTime.getHour(),
                        endDateTime.getMinute(), endDateTime.getSecond(), endDateTime.getMillisecond(), endDateTime.getMicrosecond(), endDateTime.getNanosecond(), relativeZDT.getCalendar(), Unit.DAY,
                        Undefined.instance);
        long days = dtol(dateDifference.getDays());
        BigInt intermediateNs = addZonedDateTime(ctx, realm, startNs, relativeZDT.getTimeZone(), relativeZDT.getCalendar(), 0, 0, 0, days, 0, 0, 0, 0, 0, 0);
        if (sign == 1) {
            while (days > 0 && intermediateNs.compareTo(endNs) > 0) {
                days = days - 1;
                intermediateNs = addZonedDateTime(ctx, realm, startNs, relativeZDT.getTimeZone(), relativeZDT.getCalendar(), 0, 0, 0, days, 0, 0, 0, 0, 0, 0);
            }
        }
        nanoseconds = endNs.subtract(intermediateNs).bigIntegerValue();
        boolean done = false;
        while (!done) {
            BigInteger oneDayFartherNs = addZonedDateTime(ctx, realm, intermediateNs, relativeZDT.getTimeZone(), relativeZDT.getCalendar(), 0, 0, 0, sign, 0, 0, 0, 0, 0, 0).bigIntegerValue();
            dayLengthNs = oneDayFartherNs.subtract(intermediateNs.bigIntegerValue());
            if (nanoseconds.subtract(dayLengthNs).multiply(signBI).compareTo(BigInteger.ZERO) >= 0) {
                nanoseconds = nanoseconds.subtract(dayLengthNs);
                intermediateNs = new BigInt(oneDayFartherNs);
                days = days + sign;
            } else {
                done = true;
            }
        }
        return JSTemporalNanosecondsDaysRecord.create(BigInteger.valueOf(days), nanoseconds, dayLengthNs.abs());
    }

    // TODO doing some long arithmetics here. Might need double/BigInteger
    public static JSTemporalDurationRecord adjustRoundedDurationDays(JSContext ctx, JSRealm realm, EnumerableOwnPropertyNamesNode namesNode, TemporalDurationAddNode durationAddNode,
                    double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds,
                    double increment, Unit unit, RoundingMode roundingMode,
                    JSDynamicObject relativeToParam) {
        if (!(isTemporalZonedDateTime(relativeToParam)) || unit == Unit.YEAR || unit == Unit.MONTH || unit == Unit.WEEK || unit == Unit.DAY ||
                        (unit == Unit.NANOSECOND && increment == 1)) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        JSTemporalZonedDateTimeObject relativeTo = (JSTemporalZonedDateTimeObject) relativeToParam;
        long timeRemainderNs = dtol(totalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0));
        long direction = Long.signum(timeRemainderNs);
        BigInt dayStart = addZonedDateTime(ctx, realm,
                        relativeTo.getNanoseconds(), relativeTo.getTimeZone(), relativeTo.getCalendar(),
                        dtol(years), dtol(months), dtol(weeks), dtol(days), 0, 0, 0, 0, 0, 0);
        BigInt dayEnd = addZonedDateTime(ctx, realm, dayStart, relativeTo.getTimeZone(), relativeTo.getCalendar(), 0, 0, 0, direction, 0, 0, 0, 0, 0, 0);
        long dayLengthNs = bigIntToLong(dayEnd.subtract(dayStart));
        if (((timeRemainderNs - dayLengthNs) * direction) < 0) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        BigInteger timeRemainderNsBi = roundTemporalInstant(Boundaries.bigDecimalValueOf(timeRemainderNs - dayLengthNs), increment, unit, roundingMode);
        JSTemporalDurationRecord add = durationAddNode.execute(dtol(years), dtol(months), dtol(weeks), dtol(days), 0, 0, 0, 0,
                        0, 0, 0, 0, 0, direction, 0, 0, 0, 0, 0, 0, relativeToParam);
        JSTemporalDurationRecord atd = balanceDuration(ctx, realm, namesNode, 0, 0, 0, 0, 0, 0, timeRemainderNsBi, Unit.HOUR, Undefined.instance);

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
        return nanoseconds.add(BigDecimal.valueOf(mus).toBigInteger().multiply(BI_1000));
    }

    // 7.5.11
    @TruffleBoundary
    public static double calculateOffsetShift(JSContext ctx, JSRealm realm, JSDynamicObject relativeTo, double y, double mon, double w, double d, double h, double min,
                    double s, double ms, double mus, double ns) {
        if (!(isTemporalZonedDateTime(relativeTo))) {
            return 0;
        }
        JSTemporalZonedDateTimeObject relativeToZDT = (JSTemporalZonedDateTimeObject) relativeTo;
        JSDynamicObject instant = JSTemporalInstant.create(ctx, realm, relativeToZDT.getNanoseconds());
        long offsetBefore = getOffsetNanosecondsFor(relativeToZDT.getTimeZone(), instant);
        BigInt after = addZonedDateTime(ctx, realm, relativeToZDT.getNanoseconds(), relativeToZDT.getTimeZone(), relativeToZDT.getCalendar(), dtol(y), dtol(mon), dtol(w), dtol(d), dtol(h), dtol(min),
                        dtol(s), dtol(ms), dtol(mus), dtol(ns));
        JSDynamicObject instantAfter = JSTemporalInstant.create(ctx, realm, after);
        long offsetAfter = getOffsetNanosecondsFor(relativeToZDT.getTimeZone(), instantAfter);
        return offsetAfter - offsetBefore;
    }

    @TruffleBoundary
    public static long daysUntil(JSDynamicObject earlier, JSDynamicObject later) {
        double epochDays1 = JSDate.makeDay(((TemporalYear) earlier).getYear(), ((TemporalMonth) earlier).getMonth() - 1, ((TemporalDay) earlier).getDay());
        assert Double.isFinite(epochDays1);
        double epochDays2 = JSDate.makeDay(((TemporalYear) later).getYear(), ((TemporalMonth) later).getMonth() - 1, ((TemporalDay) later).getDay());
        assert Double.isFinite(epochDays2);
        return dtol(epochDays2 - epochDays1);
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
        long result = dtol(roundNumberToIncrement(quantity, increment, roundingMode));
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
    public static JSTemporalDurationRecord balanceTimeDouble(double h, double min, double sec, double mils, double mics, double ns,
                    Node node, InlinedBranchProfile errorBranch) {
        if (h == Double.POSITIVE_INFINITY || h == Double.NEGATIVE_INFINITY ||
                        min == Double.POSITIVE_INFINITY || min == Double.NEGATIVE_INFINITY ||
                        sec == Double.POSITIVE_INFINITY || sec == Double.NEGATIVE_INFINITY ||
                        mils == Double.POSITIVE_INFINITY || mils == Double.NEGATIVE_INFINITY ||
                        mics == Double.POSITIVE_INFINITY || mics == Double.NEGATIVE_INFINITY ||
                        ns == Double.POSITIVE_INFINITY || ns == Double.NEGATIVE_INFINITY) {
            errorBranch.enter(node);
            throw Errors.createRangeError("Time is infinite");
        }
        double microseconds = mics;
        double milliseconds = mils;
        double nanoseconds = ns;
        double seconds = sec;
        double minutes = min;
        double hours = h;
        microseconds = microseconds + Math.floor(nanoseconds / 1000.0);
        nanoseconds = nonNegativeModulo(nanoseconds, 1000);
        milliseconds = milliseconds + Math.floor(microseconds / 1000.0);
        microseconds = nonNegativeModulo(microseconds, 1000);
        seconds = seconds + Math.floor(milliseconds / 1000.0);
        milliseconds = nonNegativeModulo(milliseconds, 1000);
        minutes = minutes + Math.floor(seconds / 60.0);
        seconds = nonNegativeModulo(seconds, 60);
        hours = hours + Math.floor(minutes / 60.0);
        minutes = nonNegativeModulo(minutes, 60);
        double days = Math.floor(hours / 24.0);
        hours = nonNegativeModulo(hours, 24);
        return JSTemporalDurationRecord.create(0, 0, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    // note: there also is balanceTimeDouble
    public static JSTemporalDurationRecord balanceTime(long h, long min, long sec, long mils, long mics, long ns) {
        long microseconds = mics;
        long milliseconds = mils;
        long nanoseconds = ns;
        long seconds = sec;
        long minutes = min;
        long hours = h;
        microseconds = microseconds + (long) Math.floor(nanoseconds / 1000.0);
        nanoseconds = (long) nonNegativeModulo(nanoseconds, 1000);
        milliseconds = milliseconds + (long) Math.floor(microseconds / 1000.0);
        microseconds = (long) nonNegativeModulo(microseconds, 1000);
        seconds = seconds + (long) Math.floor(milliseconds / 1000.0);
        milliseconds = (long) nonNegativeModulo(milliseconds, 1000);
        minutes = minutes + (long) Math.floor(seconds / 60.0);
        seconds = (long) nonNegativeModulo(seconds, 60);
        hours = hours + (long) Math.floor(minutes / 60.0);
        minutes = (long) nonNegativeModulo(minutes, 60);
        long days = (long) Math.floor(hours / 24.0);
        hours = (long) nonNegativeModulo(hours, 24);
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
                    double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds,
                    Node node, InlinedBranchProfile errorBranch) {
        return balanceTimeDouble(hour + hours, minute + minutes, second + seconds, millisecond + milliseconds,
                        microsecond + microseconds, nanosecond + nanoseconds, node, errorBranch);
    }

    public static JSTemporalDurationRecord roundISODateTime(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond,
                    int nanosecond, double increment, Unit unit, RoundingMode roundingMode, Long dayLength) {
        JSTemporalDurationRecord rt = roundTime(hour, minute, second, millisecond, microsecond, nanosecond, increment, unit, roundingMode, dayLength);
        JSTemporalDateTimeRecord br = balanceISODate(year, month, day + dtoi(rt.getDays()));
        return JSTemporalDurationRecord.create(br.getYear(), br.getMonth(), br.getDay(),
                        rt.getHours(), rt.getMinutes(), rt.getSeconds(),
                        rt.getMilliseconds(), rt.getMicroseconds(), rt.getNanoseconds());
    }

    public static double toTemporalDateTimeRoundingIncrement(JSDynamicObject options, Unit smallestUnit, IsObjectNode isObject, JSToNumberNode toNumber) {
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

    public static JSTemporalPlainDateTimeObject systemDateTime(Object temporalTimeZoneLike, Object calendarLike,
                    JSContext ctx, JSRealm realm, ToTemporalCalendarNode toTemporalCalendar, ToTemporalTimeZoneNode toTemporalTimeZone) {
        JSDynamicObject timeZone;
        if (temporalTimeZoneLike == Undefined.instance) {
            timeZone = systemTimeZone(ctx, realm);
        } else {
            timeZone = toTemporalTimeZone.execute(temporalTimeZoneLike);
        }
        JSDynamicObject calendar = toTemporalCalendar.execute(calendarLike);
        JSDynamicObject instant = systemInstant(ctx, realm);
        return builtinTimeZoneGetPlainDateTimeFor(ctx, realm, timeZone, instant, calendar);
    }

    @TruffleBoundary
    public static JSTemporalPlainDateTimeObject builtinTimeZoneGetPlainDateTimeFor(JSContext ctx, JSRealm realm, JSDynamicObject timeZone, JSDynamicObject instant, JSDynamicObject calendar) {
        long offsetNanoseconds = getOffsetNanosecondsFor(timeZone, instant);
        JSTemporalDateTimeRecord result = getISOPartsFromEpoch(((JSTemporalInstantObject) instant).getNanoseconds());
        JSTemporalDateTimeRecord result2 = balanceISODateTime(result.getYear(), result.getMonth(),
                        result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                        result.getMicrosecond(), result.getNanosecond() + offsetNanoseconds);
        return JSTemporalPlainDateTime.create(ctx, realm, result2.getYear(), result2.getMonth(), result2.getDay(), result2.getHour(), result2.getMinute(), result2.getSecond(),
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
            BigInteger[] result = epochNanoseconds.bigIntegerValue().divideAndRemainder(BI_10_POW_6);
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
    public static long getOffsetNanosecondsFor(JSDynamicObject timeZone, JSDynamicObject instant) {
        Object getOffsetNanosecondsFor = JSObject.getMethod(timeZone, GET_OFFSET_NANOSECONDS_FOR);
        Object offsetNanoseconds = JSRuntime.call(getOffsetNanosecondsFor, timeZone, new Object[]{instant});
        if (!(JSRuntime.isNumber(offsetNanoseconds) || offsetNanoseconds instanceof Long)) {
            throw Errors.createTypeError("Number expected");
        }
        Double nanos = ((Number) offsetNanoseconds).doubleValue();
        if (!JSRuntime.isInteger(nanos) || Math.abs(nanos) > 86400.0 * 1_000_000_000d) {
            throw Errors.createRangeError("out-of-range Number");
        }
        return nanos.longValue();
    }

    public static JSTemporalZonedDateTimeObject systemZonedDateTime(Object temporalTimeZoneLike, Object calendarLike,
                    JSContext ctx, JSRealm realm, ToTemporalCalendarNode toTemporalCalendar, ToTemporalTimeZoneNode toTemporalTimeZone) {
        JSDynamicObject timeZone;
        if (temporalTimeZoneLike == Undefined.instance) {
            timeZone = systemTimeZone(ctx, realm);
        } else {
            timeZone = toTemporalTimeZone.execute(temporalTimeZoneLike);
        }
        JSDynamicObject calendar = toTemporalCalendar.execute(calendarLike);
        BigInt ns = systemUTCEpochNanoseconds();
        return JSTemporalZonedDateTime.create(ctx, realm, ns, timeZone, calendar);
    }

    public static JSTemporalInstantObject systemInstant(JSContext ctx, JSRealm realm) {
        BigInt ns = systemUTCEpochNanoseconds();
        return JSTemporalInstant.create(ctx, realm, ns);
    }

    @TruffleBoundary
    public static BigInt systemUTCEpochNanoseconds() {
        JSRealm realm = JSRealm.get(null);
        BigInt ns = BigInt.valueOf(realm.nanoTimeWallClock());
        // clamping omitted (see Note 2 in spec)
        assert ns.compareTo(upperEpochNSLimit) <= 0 && ns.compareTo(lowerEpochNSLimit) >= 0;
        return ns;
    }

    public static JSTemporalTimeZoneObject systemTimeZone(JSContext ctx, JSRealm realm) {
        TruffleString identifier = defaultTimeZone();
        return createTemporalTimeZone(ctx, realm, identifier);
    }

    public static TruffleString defaultTimeZone() {
        return UTC;
    }

    public static boolean isTemporalInstant(Object obj) {
        return JSTemporalInstant.isJSTemporalInstant(obj);
    }

    public static int compareEpochNanoseconds(BigInt one, BigInt two) {
        return one.compareTo(two);
    }

    @TruffleBoundary
    public static boolean isValidEpochNanoseconds(BigInt nanoseconds) {
        if (nanoseconds == null) {
            return true; // suspicious, but relevant
        }
        if (nanoseconds.compareTo(lowerEpochNSLimit) < 0 || nanoseconds.compareTo(upperEpochNSLimit) > 0) {
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
        res = res.add(BigInteger.valueOf(microseconds).multiply(BI_1000));
        res = res.add(BigInteger.valueOf(milliseconds).multiply(BI_10_POW_6));
        res = res.add(BigInteger.valueOf(seconds).multiply(BI_10_POW_9));
        res = res.add(BigInteger.valueOf(minutes).multiply(BI_6_10_POW_10));
        res = res.add(BigInteger.valueOf(hours).multiply(BI_36_10_POW_11));
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
    public static TruffleString temporalInstantToString(JSContext ctx, JSRealm realm, JSDynamicObject instant, JSDynamicObject timeZone, Object precision) {
        JSDynamicObject outputTimeZone = timeZone;
        if (outputTimeZone == Undefined.instance) {
            outputTimeZone = createTemporalTimeZone(ctx, realm, UTC);
        }
        JSDynamicObject isoCalendar = getISO8601Calendar(ctx, realm);
        JSTemporalPlainDateTimeObject dateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, realm, outputTimeZone, instant, isoCalendar);
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

    public static TruffleString builtinTimeZoneGetOffsetStringFor(JSDynamicObject timeZone, JSDynamicObject instant) {
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

        TruffleString h = toZeroPaddedDecimalString(hours, 2);
        TruffleString m = toZeroPaddedDecimalString(minutes, 2);
        TruffleString s = toZeroPaddedDecimalString(seconds, 2);

        TruffleString post = Strings.EMPTY_STRING;
        if (nanoseconds != 0) {
            TruffleString fraction = longestSubstring(toZeroPaddedDecimalString(nanoseconds, 9));
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

    public static Disambiguation toTemporalDisambiguation(JSDynamicObject options, TemporalGetOptionNode getOptionNode, TruffleString.EqualNode equalNode) {
        if (options == Undefined.instance) {
            return Disambiguation.COMPATIBLE;
        }
        return toDisambiguation((TruffleString) getOptionNode.execute(options, DISAMBIGUATION, OptionType.STRING, listDisambiguation, COMPATIBLE), equalNode);
    }

    public static OffsetOption toTemporalOffset(JSDynamicObject options, TruffleString fallback, TemporalGetOptionNode getOptionNode, TruffleString.EqualNode equalNode) {
        TruffleString result = fallback;
        if (options != Undefined.instance) {
            result = (TruffleString) getOptionNode.execute(options, OFFSET, OptionType.STRING, listOffset, fallback);
        }
        return toOffsetOption(result, equalNode);
    }

    public static TruffleString toShowTimeZoneNameOption(JSDynamicObject options, TemporalGetOptionNode getOptionNode) {
        return (TruffleString) getOptionNode.execute(options, TIME_ZONE_NAME, OptionType.STRING, listAutoNever, AUTO);
    }

    public static TruffleString toShowOffsetOption(JSDynamicObject options, TemporalGetOptionNode getOptionNode) {
        return (TruffleString) getOptionNode.execute(options, OFFSET, OptionType.STRING, listAutoNever, AUTO);
    }

    public static TruffleString temporalZonedDateTimeToString(JSContext ctx, JSRealm realm, JSDynamicObject zonedDateTime, Object precision, ShowCalendar showCalendar, TruffleString showTimeZone,
                    TruffleString showOffset) {
        return temporalZonedDateTimeToString(ctx, realm, zonedDateTime, precision, showCalendar, showTimeZone, showOffset, null, Unit.EMPTY, RoundingMode.EMPTY);
    }

    public static JSTemporalDateTimeRecord addDateTime(JSContext ctx, JSRealm realm, int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond,
                    double nanosecond, JSDynamicObject calendar, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds,
                    double microseconds, double nanoseconds, JSDynamicObject options,
                    Node node, InlinedBranchProfile errorBranch) {
        JSTemporalDurationRecord timeResult = addTimeDouble(hour, minute, second, millisecond, microsecond, nanosecond,
                        hours, minutes, seconds, milliseconds, microseconds, nanoseconds, node, errorBranch);
        JSTemporalPlainDateObject datePart = JSTemporalPlainDate.create(ctx, realm, year, month, day, calendar, node, errorBranch);
        JSDynamicObject dateDuration = JSTemporalDuration.createTemporalDuration(ctx, realm, years, months, weeks, days + timeResult.getDays(), 0L, 0L, 0L, 0L, 0L, 0L, node, errorBranch);
        JSTemporalPlainDateObject addedDate = calendarDateAdd(calendar, datePart, dateDuration, options);
        return JSTemporalDateTimeRecord.create(addedDate.getYear(), addedDate.getMonth(), addedDate.getDay(),
                        dtoi(timeResult.getHours()), dtoi(timeResult.getMinutes()), dtoi(timeResult.getSeconds()),
                        dtoi(timeResult.getMilliseconds()), dtoi(timeResult.getMicroseconds()), dtoi(timeResult.getNanoseconds()));
    }

    public static int compareISODateTime(int year, int month, int day, int hours, int minutes, int seconds, int milliseconds, int microseconds, int nanoseconds, int year2, int month2,
                    int day2, int hours2, int minutes2, int seconds2, int milliseconds2, int microseconds2, int nanoseconds2) {
        int date = compareISODate(year, month, day, year2, month2, day2);
        if (date == 0) {
            return compareTemporalTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds, hours2, minutes2, seconds2, milliseconds2, microseconds2, nanoseconds2);
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
    public static TruffleString temporalZonedDateTimeToString(JSContext ctx, JSRealm realm, JSDynamicObject zonedDateTimeParam, Object precision, ShowCalendar showCalendar, TruffleString showTimeZone,
                    TruffleString showOffset, Double incrementParam, Unit unitParam, RoundingMode roundingModeParam) {
        assert isTemporalZonedDateTime(zonedDateTimeParam);
        assert unitParam != null && roundingModeParam != null;
        JSTemporalZonedDateTimeObject zonedDateTime = (JSTemporalZonedDateTimeObject) zonedDateTimeParam;
        double increment = incrementParam == null ? 1 : (double) incrementParam;
        Unit unit = unitParam == Unit.EMPTY ? Unit.NANOSECOND : unitParam;
        RoundingMode roundingMode = roundingModeParam == RoundingMode.EMPTY ? RoundingMode.TRUNC : roundingModeParam;

        BigInteger ns = roundTemporalInstant(zonedDateTime.getNanoseconds(), (long) increment, unit, roundingMode);
        JSDynamicObject timeZone = zonedDateTime.getTimeZone();
        JSTemporalInstantObject instant = JSTemporalInstant.create(ctx, realm, new BigInt(ns));
        JSTemporalCalendarObject isoCalendar = getISO8601Calendar(ctx, realm);
        JSTemporalPlainDateTimeObject temporalDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, realm, timeZone, instant, isoCalendar);
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

        TruffleString h = toZeroPaddedDecimalString(hours, 2);
        TruffleString m = toZeroPaddedDecimalString(minutes, 2);

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
    public static BigInt parseTemporalInstant(TruffleString string) {
        JSTemporalZonedDateTimeRecord result = parseTemporalInstantString(string);
        TruffleString offsetString = result.getTimeZoneOffsetString();
        assert (offsetString != null);
        BigInteger utc = getEpochFromISOParts(result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(),
                        result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
        long offsetNanoseconds = parseTimeZoneOffsetString(offsetString);
        BigInt instant = new BigInt(utc.subtract(BigInteger.valueOf(offsetNanoseconds)));
        if (!isValidEpochNanoseconds(instant)) {
            throw TemporalErrors.createRangeErrorInvalidNanoseconds();
        }
        return instant;
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
            assert offsetString != null;
            return JSTemporalZonedDateTimeRecord.create(result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(),
                            result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), null, false, offsetString, null);
        } catch (Exception ex) {
            throw Errors.createRangeError("Instant cannot be parsed");
        }
    }

    @TruffleBoundary
    public static JSTemporalInstantObject builtinTimeZoneGetInstantFor(JSContext ctx, JSRealm realm, JSDynamicObject timeZone, JSTemporalPlainDateTimeObject dateTime, Disambiguation disambiguation) {
        List<JSTemporalInstantObject> possibleInstants = getPossibleInstantsFor(timeZone, dateTime);
        return disambiguatePossibleInstants(ctx, realm, possibleInstants, timeZone, dateTime, disambiguation);
    }

    @TruffleBoundary
    public static JSTemporalInstantObject disambiguatePossibleInstants(JSContext ctx, JSRealm realm, List<JSTemporalInstantObject> possibleInstants, JSDynamicObject timeZone,
                    JSTemporalPlainDateTimeObject dateTime,
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
        JSTemporalInstantObject dayBefore = JSTemporalInstant.create(ctx, realm, new BigInt(epochNanoseconds.subtract(BI_8_64_13)));
        JSTemporalInstantObject dayAfter = JSTemporalInstant.create(ctx, realm, new BigInt(epochNanoseconds.add(BI_8_64_13)));
        long offsetBefore = getOffsetNanosecondsFor(timeZone, dayBefore);
        long offsetAfter = getOffsetNanosecondsFor(timeZone, dayAfter);
        long nanoseconds = offsetAfter - offsetBefore;
        if (Disambiguation.EARLIER == disambiguation) {
            JSTemporalDateTimeRecord earlier = addDateTime(ctx, realm, dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
                            dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(), dateTime.getCalendar(), 0, 0, 0, 0, 0, 0, 0, 0, 0, -nanoseconds, Undefined.instance,
                            null, InlinedBranchProfile.getUncached());
            JSTemporalPlainDateTimeObject earlierDateTime = JSTemporalPlainDateTime.create(ctx, realm, earlier.getYear(), earlier.getMonth(), earlier.getDay(), earlier.getHour(), earlier.getMinute(),
                            earlier.getSecond(), earlier.getMillisecond(), earlier.getMicrosecond(), earlier.getNanosecond(), dateTime.getCalendar(), null, null);
            List<JSTemporalInstantObject> possibleInstants2 = getPossibleInstantsFor(timeZone, earlierDateTime);
            if (possibleInstants2.size() == 0) {
                throw Errors.createRangeError("nothing found");
            }
            return possibleInstants2.get(0);
        }
        assert Disambiguation.LATER == disambiguation || Disambiguation.COMPATIBLE == disambiguation;
        JSTemporalDateTimeRecord later = addDateTime(ctx, realm, dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
                        dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(), dateTime.getCalendar(), 0, 0, 0, 0, 0, 0, 0, 0, 0, nanoseconds, Undefined.instance,
                        null, InlinedBranchProfile.getUncached());
        JSTemporalPlainDateTimeObject laterDateTime = JSTemporalPlainDateTime.create(ctx, realm, later.getYear(), later.getMonth(), later.getDay(), later.getHour(), later.getMinute(),
                        later.getSecond(), later.getMillisecond(), later.getMicrosecond(), later.getNanosecond(), dateTime.getCalendar(), null, null);

        List<JSTemporalInstantObject> possibleInstants2 = getPossibleInstantsFor(timeZone, laterDateTime);
        n = possibleInstants2.size();
        if (n == 0) {
            throw Errors.createRangeError("nothing found");
        }
        return possibleInstants2.get(n - 1);
    }

    @TruffleBoundary
    public static BigInt interpretISODateTimeOffset(JSContext ctx, JSRealm realm, int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond,
                    int nanosecond, OffsetBehaviour offsetBehaviour, Object offsetNanosecondsParam, JSDynamicObject timeZone, Disambiguation disambiguation, OffsetOption offsetOption,
                    MatchBehaviour matchBehaviour) {
        double offsetNs = (offsetNanosecondsParam == null || offsetNanosecondsParam == Undefined.instance) ? Double.NaN : ((Number) offsetNanosecondsParam).doubleValue();
        JSDynamicObject calendar = getISO8601Calendar(ctx, realm);
        JSTemporalPlainDateTimeObject dateTime = JSTemporalPlainDateTime.create(ctx, realm, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, calendar);
        if (offsetBehaviour == OffsetBehaviour.WALL || OffsetOption.IGNORE == offsetOption) {
            JSTemporalInstantObject instant = builtinTimeZoneGetInstantFor(ctx, realm, timeZone, dateTime, disambiguation);
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
        JSTemporalInstantObject instant = builtinTimeZoneGetInstantFor(ctx, realm, timeZone, dateTime, disambiguation);
        return instant.getNanoseconds();
    }

    @TruffleBoundary
    public static BigInt addZonedDateTime(JSContext ctx, JSRealm realm, BigInt epochNanoseconds, JSDynamicObject timeZone, JSDynamicObject calendar, long years, long months, long weeks, long days,
                    long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        return addZonedDateTime(ctx, realm, epochNanoseconds, timeZone, calendar, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, BigInteger.valueOf(nanoseconds),
                        Undefined.instance);
    }

    @TruffleBoundary
    public static BigInt addZonedDateTime(JSContext ctx, JSRealm realm, BigInt epochNanoseconds, JSDynamicObject timeZone, JSDynamicObject calendar, long years, long months, long weeks, long days,
                    long hours, long minutes, long seconds, long milliseconds, long microseconds, BigInteger nanoseconds, JSDynamicObject options) {
        if (years == 0 && months == 0 && weeks == 0 && days == 0) {
            return addInstant(epochNanoseconds, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        JSTemporalInstantObject instant = JSTemporalInstant.create(ctx, realm, epochNanoseconds);
        JSTemporalPlainDateTimeObject temporalDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, realm, timeZone, instant, calendar);
        JSTemporalPlainDateObject datePart = JSTemporalPlainDate.create(ctx, realm, temporalDateTime.getYear(), temporalDateTime.getMonth(), temporalDateTime.getDay(), calendar, null,
                        InlinedBranchProfile.getUncached());
        JSTemporalDurationObject dateDuration = JSTemporalDuration.createTemporalDuration(ctx, realm, years, months, weeks, days, 0, 0, 0, 0, 0, 0, null, null);
        JSTemporalPlainDateObject addedDate = calendarDateAdd(calendar, datePart, dateDuration, options);
        JSTemporalPlainDateTimeObject intermediateDateTime = JSTemporalPlainDateTime.create(ctx, realm, addedDate.getYear(), addedDate.getMonth(), addedDate.getDay(),
                        temporalDateTime.getHour(), temporalDateTime.getMinute(), temporalDateTime.getSecond(),
                        temporalDateTime.getMillisecond(), temporalDateTime.getMicrosecond(), temporalDateTime.getNanosecond(), calendar);
        JSTemporalInstantObject intermediateInstant = builtinTimeZoneGetInstantFor(ctx, realm, timeZone, intermediateDateTime, Disambiguation.COMPATIBLE);
        return addInstant(intermediateInstant.getNanoseconds(), hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    public static JSDynamicObject moveRelativeZonedDateTime(JSContext ctx, JSRealm realm, JSTemporalZonedDateTimeObject zdt, long years, long months, long weeks, long days) {
        BigInt intermediateNs = addZonedDateTime(ctx, realm, zdt.getNanoseconds(), zdt.getTimeZone(), zdt.getCalendar(), years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        return JSTemporalZonedDateTime.create(ctx, realm, intermediateNs, zdt.getTimeZone(), zdt.getCalendar());
    }

    public static boolean timeZoneEquals(JSDynamicObject tz1, JSDynamicObject tz2, JSToStringNode toStringNode) {
        if (tz1 == tz2) {
            return true;
        }
        TruffleString s1 = toStringNode.executeString(tz1);
        TruffleString s2 = toStringNode.executeString(tz2);
        return Boundaries.equals(s1, s2);
    }

    public static JSDynamicObject consolidateCalendars(JSDynamicObject one, JSDynamicObject two, JSToStringNode toStringNode) {
        if (one == two) {
            return two;
        }
        TruffleString s1 = toStringNode.executeString(one);
        TruffleString s2 = toStringNode.executeString(two);
        return consolidateCalendarsIntl(one, two, s1, s2);
    }

    @TruffleBoundary
    private static JSDynamicObject consolidateCalendarsIntl(JSDynamicObject one, JSDynamicObject two, TruffleString s1, TruffleString s2) {
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

    private static List<JSTemporalInstantObject> getPossibleInstantsFor(JSDynamicObject timeZone, JSDynamicObject dateTime) {
        Object fn = JSObject.get(timeZone, GET_POSSIBLE_INSTANTS_FOR);
        JSDynamicObject possibleInstants = toDynamicObject(JSRuntime.call(fn, timeZone, new Object[]{dateTime}));
        IteratorRecord iteratorRecord = JSRuntime.getIterator(possibleInstants);
        List<JSTemporalInstantObject> list = new ArrayList<>();
        Object next = true;
        while (next != Boolean.FALSE) {
            next = JSRuntime.iteratorStep(iteratorRecord);
            if (next != Boolean.FALSE) {
                Object nextValue = JSRuntime.iteratorValue(next);
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
    public static double getIANATimeZoneOffsetNanoseconds(BigInt nanoseconds, TruffleString identifier) {
        try {
            Instant instant = Instant.ofEpochSecond(0, nanoseconds.longValue()); // TODO wrong
            ZoneId zoneId = ZoneId.of(Strings.toJavaString(identifier));
            ZoneRules zoneRule = zoneId.getRules();
            ZoneOffset offset = zoneRule.getOffset(instant);
            return offset.getTotalSeconds() * 1_000_000_000D;
        } catch (Exception ex) {
            assert false;
            return Long.MIN_VALUE;
        }
    }

    @TruffleBoundary
    public static OptionalLong getIANATimeZoneNextTransition(BigInt nanoseconds, TruffleString identifier) {
        try {
            BigInteger[] sec = nanoseconds.bigIntegerValue().divideAndRemainder(BI_10_POW_9);
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
    public static OptionalLong getIANATimeZonePreviousTransition(BigInt nanoseconds, TruffleString identifier) {
        try {
            BigInteger[] sec = nanoseconds.bigIntegerValue().divideAndRemainder(BI_10_POW_9);
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

    public static Number calendarYear(TemporalCalendarGetterNode getterNode, JSDynamicObject calendar, JSDynamicObject dateLike) {
        return getterNode.executeInteger(calendar, dateLike, YEAR);
    }

    public static Number calendarMonth(TemporalCalendarGetterNode getterNode, JSDynamicObject calendar, JSDynamicObject dateLike) {
        return getterNode.executeInteger(calendar, dateLike, MONTH);
    }

    public static TruffleString calendarMonthCode(TemporalCalendarGetterNode getterNode, JSDynamicObject calendar, JSDynamicObject dateLike) {
        return getterNode.executeString(calendar, dateLike, MONTH_CODE);
    }

    public static Number calendarDay(TemporalCalendarGetterNode getterNode, JSDynamicObject calendar, JSDynamicObject dateLike) {
        return getterNode.executeInteger(calendar, dateLike, DAY);
    }

    public static Object calendarDayOfWeek(TemporalCalendarGetterNode getterNode, JSDynamicObject calendar, JSDynamicObject dateLike) {
        return getterNode.execute(calendar, dateLike, TemporalConstants.DAY_OF_WEEK);
    }

    public static Object calendarDayOfYear(TemporalCalendarGetterNode getterNode, JSDynamicObject calendar, JSDynamicObject dateLike) {
        return getterNode.execute(calendar, dateLike, TemporalConstants.DAY_OF_YEAR);
    }

    public static Object calendarWeekOfYear(TemporalCalendarGetterNode getterNode, JSDynamicObject calendar, JSDynamicObject dateLike) {
        return getterNode.execute(calendar, dateLike, TemporalConstants.WEEK_OF_YEAR);
    }

    public static Object calendarDaysInWeek(TemporalCalendarGetterNode getterNode, JSDynamicObject calendar, JSDynamicObject dateLike) {
        return getterNode.execute(calendar, dateLike, TemporalConstants.DAYS_IN_WEEK);
    }

    public static Object calendarDaysInMonth(TemporalCalendarGetterNode getterNode, JSDynamicObject calendar, JSDynamicObject dateLike) {
        return getterNode.execute(calendar, dateLike, TemporalConstants.DAYS_IN_MONTH);
    }

    public static Object calendarDaysInYear(TemporalCalendarGetterNode getterNode, JSDynamicObject calendar, JSDynamicObject dateLike) {
        return getterNode.execute(calendar, dateLike, TemporalConstants.DAYS_IN_YEAR);
    }

    public static Object calendarMonthsInYear(TemporalCalendarGetterNode getterNode, JSDynamicObject calendar, JSDynamicObject dateLike) {
        return getterNode.execute(calendar, dateLike, TemporalConstants.MONTHS_IN_YEAR);
    }

    public static Object calendarInLeapYear(TemporalCalendarGetterNode getterNode, JSDynamicObject calendar, JSDynamicObject dateLike) {
        return getterNode.execute(calendar, dateLike, TemporalConstants.IN_LEAP_YEAR);
    }

    // 12.1.38
    public static Object resolveISOMonth(JSContext ctx, JSDynamicObject fields, JSToIntegerOrInfinityNode toIntegerOrInfinity, JSIdenticalNode identicalNode) {
        Object month = JSObject.get(fields, MONTH);
        Object monthCode = JSObject.get(fields, MONTH_CODE);
        if (monthCode == Undefined.instance) {
            if (month == Undefined.instance) {
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

        double m1 = (month == Undefined.instance) ? -1 : JSRuntime.doubleValue(toIntegerOrInfinity.executeNumber(month));

        if (month != Undefined.instance && m1 != numberPart2) {
            throw Errors.createRangeError("Month does not equal the month code.");
        }
        if (!identicalNode.executeBoolean(monthCode, buildISOMonthCode((int) numberPart2))) {
            throw Errors.createRangeError("Not same value");
        }

        return (long) numberPart2;
    }

    // 12.1.39
    public static JSTemporalDateTimeRecord isoDateFromFields(JSDynamicObject fields, JSDynamicObject options, JSContext ctx, IsObjectNode isObject,
                    TemporalGetOptionNode getOptionNode, JSToIntegerOrInfinityNode toIntOrInfinityNode, JSIdenticalNode identicalNode) {
        assert isObject.executeBoolean(fields);
        Overflow overflow = toTemporalOverflow(options, getOptionNode);
        JSDynamicObject preparedFields = prepareTemporalFields(ctx, fields, listDMMCY, listYD);
        Object year = JSObject.get(preparedFields, YEAR);
        Object month = resolveISOMonth(ctx, preparedFields, toIntOrInfinityNode, identicalNode);
        Object day = JSObject.get(preparedFields, DAY);
        return regulateISODate(dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(year))), dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(month))),
                        dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(day))), overflow);
    }

    public static JSTemporalYearMonthDayRecord isoYearMonthFromFields(JSDynamicObject fields, JSDynamicObject options, JSContext ctx, IsObjectNode isObject,
                    TemporalGetOptionNode getOptionNode, JSToIntegerOrInfinityNode toIntOrInfinityNode, JSIdenticalNode identicalNode) {
        assert isObject.executeBoolean(fields);
        Overflow overflow = toTemporalOverflow(options, getOptionNode);
        JSDynamicObject preparedFields = prepareTemporalFields(ctx, fields, listMMCY, listY);
        Object year = JSObject.get(preparedFields, YEAR);
        Object month = resolveISOMonth(ctx, preparedFields, toIntOrInfinityNode, identicalNode);

        JSTemporalYearMonthDayRecord result = regulateISOYearMonth(dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(year))),
                        dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(month))), overflow);
        return JSTemporalYearMonthDayRecord.create(result.getYear(), result.getMonth(), 1);
    }

    public static JSTemporalYearMonthDayRecord isoMonthDayFromFields(JSDynamicObject fields, JSDynamicObject options, JSContext ctx, IsObjectNode isObject,
                    TemporalGetOptionNode getOptionNode, JSToIntegerOrInfinityNode toIntOrInfinityNode, JSIdenticalNode identicalNode) {
        assert isObject.executeBoolean(fields);
        Overflow overflow = toTemporalOverflow(options, getOptionNode);
        JSDynamicObject preparedFields = prepareTemporalFields(ctx, fields, listDMMCY, listD);
        Object month = JSObject.get(preparedFields, MONTH);
        Object monthCode = JSObject.get(preparedFields, MONTH_CODE);
        Object year = JSObject.get(preparedFields, YEAR);
        if (month != Undefined.instance && monthCode == Undefined.instance && year == Undefined.instance) {
            throw Errors.createTypeError("A year or a month code should be present.");
        }
        month = resolveISOMonth(ctx, preparedFields, toIntOrInfinityNode, identicalNode);
        Object day = JSObject.get(preparedFields, DAY);
        int referenceISOYear = 1972;
        JSTemporalDateTimeRecord result = null;
        if (monthCode == Undefined.instance) {
            result = regulateISODate(dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(year))), dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(month))),
                            dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(day))), overflow);
        } else {
            result = regulateISODate(referenceISOYear, dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(month))),
                            dtoi(JSRuntime.doubleValue(toIntOrInfinityNode.executeNumber(day))), overflow);
        }
        return JSTemporalYearMonthDayRecord.create(referenceISOYear, result.getMonth(), result.getDay());
    }

    // 12.1.45
    public static long isoDay(JSDynamicObject temporalObject) {
        TemporalDay day = (TemporalDay) temporalObject;
        return day.getDay();
    }

    public static JSTemporalDurationRecord createDurationRecord(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds,
                    double microseconds, double nanoseconds) {
        if (!isValidDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
            throw TemporalErrors.createTypeErrorDurationOutsideRange();
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
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
    public static long dtol(double d, boolean failOnError) {
        if (failOnError && !JSRuntime.doubleIsRepresentableAsLong(d)) {
            throw Errors.createRangeError("value out of range");
        }
        return (long) d;
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

    public static Unit toUnit(TruffleString unit, TruffleString.EqualNode equalNode) {
        if (unit == null) {
            return Unit.EMPTY;
        } else if (equalNode.execute(unit, YEAR, TruffleString.Encoding.UTF_16)) {
            return Unit.YEAR;
        } else if (equalNode.execute(unit, MONTH, TruffleString.Encoding.UTF_16)) {
            return Unit.MONTH;
        } else if (equalNode.execute(unit, WEEK, TruffleString.Encoding.UTF_16)) {
            return Unit.WEEK;
        } else if (equalNode.execute(unit, DAY, TruffleString.Encoding.UTF_16)) {
            return Unit.DAY;
        } else if (equalNode.execute(unit, HOUR, TruffleString.Encoding.UTF_16)) {
            return Unit.HOUR;
        } else if (equalNode.execute(unit, MINUTE, TruffleString.Encoding.UTF_16)) {
            return Unit.MINUTE;
        } else if (equalNode.execute(unit, SECOND, TruffleString.Encoding.UTF_16)) {
            return Unit.SECOND;
        } else if (equalNode.execute(unit, MILLISECOND, TruffleString.Encoding.UTF_16)) {
            return Unit.MILLISECOND;
        } else if (equalNode.execute(unit, MICROSECOND, TruffleString.Encoding.UTF_16)) {
            return Unit.MICROSECOND;
        } else if (equalNode.execute(unit, NANOSECOND, TruffleString.Encoding.UTF_16)) {
            return Unit.NANOSECOND;
        } else if (equalNode.execute(unit, AUTO, TruffleString.Encoding.UTF_16)) {
            return Unit.AUTO;
        }
        throw Errors.createTypeError("unexpected unit");
    }

    @TruffleBoundary
    public static RoundingMode toRoundingMode(TruffleString mode, TruffleString.EqualNode equalNode) {
        if (mode == null) {
            return RoundingMode.EMPTY;
        } else if (equalNode.execute(mode, FLOOR, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.FLOOR;
        } else if (equalNode.execute(mode, CEIL, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.CEIL;
        } else if (equalNode.execute(mode, HALF_EXPAND, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.HALF_EXPAND;
        } else if (equalNode.execute(mode, TRUNC, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.TRUNC;
        }
        throw Errors.createTypeError("unexpected roundingMode");
    }

    @TruffleBoundary
    public static Disambiguation toDisambiguation(TruffleString disambiguation, TruffleString.EqualNode equalNode) {
        if (equalNode.execute(disambiguation, EARLIER, TruffleString.Encoding.UTF_16)) {
            return Disambiguation.EARLIER;
        } else if (equalNode.execute(disambiguation, LATER, TruffleString.Encoding.UTF_16)) {
            return Disambiguation.LATER;
        } else if (equalNode.execute(disambiguation, COMPATIBLE, TruffleString.Encoding.UTF_16)) {
            return Disambiguation.COMPATIBLE;
        } else if (equalNode.execute(disambiguation, REJECT, TruffleString.Encoding.UTF_16)) {
            return Disambiguation.REJECT;
        }
        throw Errors.createTypeError("unexpected disambiguation");
    }

    @TruffleBoundary
    public static OffsetOption toOffsetOption(TruffleString offsetOption, TruffleString.EqualNode equalNode) {
        if (equalNode.execute(offsetOption, USE, TruffleString.Encoding.UTF_16)) {
            return OffsetOption.USE;
        } else if (equalNode.execute(offsetOption, IGNORE, TruffleString.Encoding.UTF_16)) {
            return OffsetOption.IGNORE;
        } else if (equalNode.execute(offsetOption, PREFER, TruffleString.Encoding.UTF_16)) {
            return OffsetOption.PREFER;
        } else if (equalNode.execute(offsetOption, REJECT, TruffleString.Encoding.UTF_16)) {
            return OffsetOption.REJECT;
        }
        throw Errors.createTypeError("unexpected offsetOption");
    }

    @TruffleBoundary
    public static ShowCalendar toShowCalendar(TruffleString showCalendar, TruffleString.EqualNode equalNode) {
        if (equalNode.execute(showCalendar, AUTO, TruffleString.Encoding.UTF_16)) {
            return ShowCalendar.AUTO;
        } else if (equalNode.execute(showCalendar, NEVER, TruffleString.Encoding.UTF_16)) {
            return ShowCalendar.NEVER;
        } else if (equalNode.execute(showCalendar, ALWAYS, TruffleString.Encoding.UTF_16)) {
            return ShowCalendar.ALWAYS;
        }
        throw Errors.createTypeError("unexpected showCalendar");
    }

    public static double roundTowardsZero(double d) {
        return ExactMath.truncate(d);
    }
}
