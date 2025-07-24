/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CRITICAL;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DISAMBIGUATION;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.EARLIER;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ERA;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ERA_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.FLOOR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_CEIL;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EVEN;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_FLOOR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_TRUNC;
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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
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
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.graalvm.collections.EconomicSet;
import org.graalvm.shadowed.com.ibm.icu.util.Calendar;
import org.graalvm.shadowed.com.ibm.icu.util.HebrewCalendar;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.ExactMath;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerOrInfinityNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarDateFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.ToFractionalSecondDigitsNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarIdentifierNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneIdentifierNode;
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
import com.oracle.truffle.js.runtime.builtins.temporal.DateDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalParserRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZoneRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.NormalizedDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.ParseISODateTimeResult;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeRecord;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class TemporalUtil {

    private static final Function<Object, Object> toIntegerWithTruncation = TemporalUtil::toIntegerWithTruncation;
    private static final Function<Object, Object> toMonthCode = TemporalUtil::toMonthCode;
    private static final Function<Object, Object> toOffsetString = TemporalUtil::toOffsetString;
    private static final Function<Object, Object> toPositiveInteger = TemporalUtil::toPositiveInteger;
    private static final Function<Object, Object> toString = JSRuntime::toString;
    private static final Function<Object, Object> toTemporalTimeZoneIdentifier = TemporalUtil::toTemporalTimeZoneIdentifier;

    public static final Map<TruffleString, TruffleString> singularToPlural = Map.ofEntries(
                    Map.entry(YEAR, YEARS),
                    Map.entry(MONTH, MONTHS),
                    Map.entry(WEEK, WEEKS),
                    Map.entry(DAY, DAYS),
                    Map.entry(HOUR, HOURS),
                    Map.entry(MINUTE, MINUTES),
                    Map.entry(SECOND, SECONDS),
                    Map.entry(MILLISECOND, MILLISECONDS),
                    Map.entry(MICROSECOND, MICROSECONDS),
                    Map.entry(NANOSECOND, NANOSECONDS));

    private static final Map<TruffleString, Function<Object, Object>> temporalFieldConversion = Map.ofEntries(
                    Map.entry(YEAR, toIntegerWithTruncation),
                    Map.entry(MONTH, toPositiveInteger),
                    Map.entry(MONTH_CODE, toMonthCode),
                    Map.entry(DAY, toPositiveInteger),
                    Map.entry(HOUR, toIntegerWithTruncation),
                    Map.entry(MINUTE, toIntegerWithTruncation),
                    Map.entry(SECOND, toIntegerWithTruncation),
                    Map.entry(MILLISECOND, toIntegerWithTruncation),
                    Map.entry(MICROSECOND, toIntegerWithTruncation),
                    Map.entry(NANOSECOND, toIntegerWithTruncation),
                    Map.entry(OFFSET, toOffsetString),
                    Map.entry(TIME_ZONE, toTemporalTimeZoneIdentifier),
                    Map.entry(ERA, toString),
                    Map.entry(ERA_YEAR, toIntegerWithTruncation));

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
                    Map.entry(TIME_ZONE, Undefined.instance),
                    Map.entry(ERA, Undefined.instance),
                    Map.entry(ERA_YEAR, Undefined.instance));

    public static final List<TruffleString> listEmpty = List.of();
    public static final List<TruffleString> listDMMCY = List.of(DAY, MONTH, MONTH_CODE, YEAR);
    public static final List<TruffleString> listMMCY = List.of(MONTH, MONTH_CODE, YEAR);
    public static final List<TruffleString> listMCY = List.of(MONTH_CODE, YEAR);
    public static final List<TruffleString> listDMC = List.of(DAY, MONTH_CODE);
    public static final List<TruffleString> listYD = List.of(YEAR, DAY);
    public static final List<TruffleString> listY = List.of(YEAR);
    public static final List<TruffleString> listD = List.of(DAY);
    public static final List<TruffleString> listTimeUnits = List.of(HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND);
    public static final List<TruffleString> listTimeUnitsOffset = List.of(HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND, OFFSET);
    public static final List<TruffleString> listTimeUnitsOffsetTZ = List.of(HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND, OFFSET, TIME_ZONE);

    private static final List<TruffleString> singularDateUnits = List.of(YEAR, MONTH, WEEK, DAY);
    private static final List<TruffleString> singularTimeUnits = listTimeUnits;
    private static final List<TruffleString> singularDateTimeUnits = List.of(YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND);
    private static final List<TruffleString> singularYearMonthUnits = List.of(YEAR, MONTH);
    public static final Map<TruffleString, Unit> unitMappingDate = createUnitMapping(singularDateUnits);
    public static final Map<TruffleString, Unit> unitMappingDateOrAuto = createUnitMapping(singularDateUnits, AUTO);
    public static final Map<TruffleString, Unit> unitMappingTime = createUnitMapping(singularTimeUnits);
    public static final Map<TruffleString, Unit> unitMappingTimeOrDay = createUnitMapping(singularTimeUnits, DAY);
    public static final Map<TruffleString, Unit> unitMappingTimeOrAuto = createUnitMapping(singularTimeUnits, AUTO);
    public static final Map<TruffleString, Unit> unitMappingDateTime = createUnitMapping(singularDateTimeUnits);
    public static final Map<TruffleString, Unit> unitMappingDateTimeOrAuto = createUnitMapping(singularDateTimeUnits, AUTO);
    public static final Map<TruffleString, Unit> unitMappingYearMonth = createUnitMapping(singularYearMonthUnits);
    public static final Map<TruffleString, Unit> unitMappingYearMonthOrAuto = createUnitMapping(singularYearMonthUnits, AUTO);

    public static final List<TruffleString> listAuto = List.of(AUTO);
    public static final List<TruffleString> listAutoNever = List.of(AUTO, NEVER);
    public static final List<TruffleString> listAutoNeverCritical = List.of(AUTO, NEVER, CRITICAL);
    public static final List<TruffleString> listAutoAlwaysNeverCritical = List.of(AUTO, ALWAYS, NEVER, CRITICAL);
    public static final List<TruffleString> listConstrainReject = List.of(CONSTRAIN, REJECT);
    public static final List<TruffleString> listTimeZone = List.of(TIME_ZONE);
    public static final List<TruffleString> listTimeZoneOffset = List.of(TIME_ZONE, OFFSET);
    public static final List<TruffleString> listRoundingMode = List.of(CEIL, FLOOR, EXPAND, TRUNC, HALF_FLOOR, HALF_CEIL, HALF_EXPAND, HALF_TRUNC, HALF_EVEN);
    public static final List<TruffleString> listOffset = List.of(OFFSET);
    public static final List<TruffleString> listOffsets = List.of(PREFER, USE, IGNORE, REJECT);
    public static final List<TruffleString> listDisambiguation = List.of(COMPATIBLE, EARLIER, LATER, REJECT);

    public static final TruffleString[] TIME_LIKE_PROPERTIES = new TruffleString[]{HOUR, MICROSECOND, MILLISECOND, MINUTE, NANOSECOND, SECOND};

    public static final int MS_PER_DAY = 86_400_000; // 24 * 60 * 60 * 1000
    public static final long NS_PER_DAY_LONG = 1_000_000L * MS_PER_DAY;
    public static final double NS_PER_DAY = NS_PER_DAY_LONG; // 8.64e13
    public static final BigInt BI_NS_PER_DAY = BigInt.valueOf(NS_PER_DAY_LONG);

    /** Instant range is 100 million days (inclusive) before or after epoch (~273,790 years). */
    private static final int INSTANT_MAX_OFFSET_EPOCH_DAYS = 100_000_000; // 10**8
    /** nsMaxInstant (inclusive) = 10**8 * nsPerDay = 8.64 * 10**21. */
    private static final BigInt upperEpochNSLimit = BI_NS_PER_DAY.multiply(BigInt.valueOf(INSTANT_MAX_OFFSET_EPOCH_DAYS));
    /** nsMinInstant (inclusive) = -nsMaxInstant = -8.64 * 10**21. */
    private static final BigInt lowerEpochNSLimit = upperEpochNSLimit.negate();
    /** nsMaxInstant + nsPerDay (exclusive) = 8.64 * 10**21 + 8.64 * 10**13. */
    private static final BigInt isoTimeUpperBound = upperEpochNSLimit.add(BI_NS_PER_DAY);
    /** nsMinInstant - nsPerDay (exclusive) = -8.64 * 10**21 - 8.64 * 10**13. */
    private static final BigInt isoTimeLowerBound = isoTimeUpperBound.negate();
    private static final int isoTimeBoundYears = 270000;
    private static final int ISO_DATE_MAX_UTC_OFFSET_DAYS = INSTANT_MAX_OFFSET_EPOCH_DAYS + 1;

    /** maxTimeDuration = 2**53 * 10**9 - 1 = 9,007,199,254,740,991,999,999,999. */
    private static final BigInt MAX_TIME_DURATION = BigInt.valueOf(1L << 53).multiply(BigInt.valueOf(1_000_000_000L)).subtract(BigInt.ONE);

    public static final BigInt BI_NS_PER_HOUR = BigInt.valueOf(3_600_000_000_000L);
    public static final BigInt BI_NS_PER_MINUTE = BigInt.valueOf(60_000_000_000L);
    public static final BigInt BI_NS_PER_SECOND = BigInt.valueOf(1_000_000_000);
    public static final BigInt BI_1000_000 = BigInt.valueOf(1_000_000);
    public static final BigInt BI_NS_PER_MS = BI_1000_000;
    public static final BigInt BI_1000 = BigInt.valueOf(1_000);
    public static final BigInt BI_24 = BigInt.valueOf(24);
    public static final BigInt BI_60 = BigInt.valueOf(60);
    public static final BigInt BI_3600 = BigInt.valueOf(3600);
    public static final BigInt BI_86400 = BigInt.valueOf(86400);

    public static final BigDecimal BD_10 = BigDecimal.valueOf(10);
    public static final BigDecimal BD_60 = BigDecimal.valueOf(60);
    public static final BigDecimal BD_1000 = BigDecimal.valueOf(1000);

    public static final MathContext mc_20_floor = new MathContext(20, java.math.RoundingMode.FLOOR);

    public static final int HOURS_PER_DAY = 24;
    public static final int MINUTES_PER_HOUR = 60;
    public static final int SECONDS_PER_MINUTE = 60;

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

        @CompilationFinal(dimensions = 1) //
        public static final Unit[] VALUES = values();
        public static final Unit REQUIRED = null;

        private final TruffleString name;

        Unit(TruffleString name) {
            this.name = name;
        }

        public TruffleString toTruffleString() {
            return name;
        }

        public long getLengthInNanoseconds() {
            return switch (this) {
                case NANOSECOND -> 1;
                case MICROSECOND -> 1_000L;
                case MILLISECOND -> 1_000_000L;
                case SECOND -> 1_000_000_000L;
                case MINUTE -> 1_000_000_000L * SECONDS_PER_MINUTE;
                case HOUR -> 1_000_000_000L * SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
                case DAY -> NS_PER_DAY_LONG;
                default -> throw Errors.shouldNotReachHereUnexpectedValue(this);
            };
        }

        public boolean isCalendarUnit() {
            return switch (this) {
                case YEAR, MONTH, WEEK -> true;
                default -> false;
            };
        }

        public boolean isDateUnit() {
            return switch (this) {
                case YEAR, MONTH, WEEK, DAY -> true;
                default -> false;
            };
        }

        public boolean isTimeUnit() {
            return switch (this) {
                case HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND -> true;
                default -> false;
            };
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
        NEVER,
        CRITICAL
    }

    private static Map<TruffleString, Unit> createUnitMapping(List<TruffleString> singularUnits, TruffleString... extraValues) {
        Map<TruffleString, Unit> map = new HashMap<>();
        for (TruffleString singular : IteratorUtil.concatLists(singularUnits, List.of(extraValues))) {
            Unit unit = Unit.valueOf(singular.toJavaStringUncached().toUpperCase(Locale.ROOT));
            map.put(singular, unit);
            TruffleString plural = singularToPlural.get(singular);
            if (plural != null) {
                map.put(plural, unit);
            }
        }
        return Map.copyOf(map);
    }

    public static int validateTemporalRoundingIncrement(int increment, long dividend, boolean inclusive,
                    Node node, InlinedBranchProfile errorBranch) {
        double maximum;
        if (inclusive) {
            maximum = dividend;
        } else {
            assert dividend > 1 : dividend;
            maximum = dividend - 1;
        }
        if (increment > maximum || dividend % increment != 0) {
            errorBranch.enter(node);
            throw Errors.createRangeError("Increment out of range.");
        }
        return increment;
    }

    @TruffleBoundary
    public static JSTemporalPrecisionRecord toSecondsStringPrecisionRecord(Unit smallestUnit, int fractionalDigitCount) {
        return switch (smallestUnit) {
            case MINUTE -> JSTemporalPrecisionRecord.create(MINUTE, Unit.MINUTE, 1);
            case SECOND -> JSTemporalPrecisionRecord.create(0, Unit.SECOND, 1);
            case MILLISECOND -> JSTemporalPrecisionRecord.create(3, Unit.MILLISECOND, 1);
            case MICROSECOND -> JSTemporalPrecisionRecord.create(6, Unit.MICROSECOND, 1);
            case NANOSECOND -> JSTemporalPrecisionRecord.create(9, Unit.NANOSECOND, 1);
            case EMPTY -> switch (fractionalDigitCount) {
                case ToFractionalSecondDigitsNode.AUTO -> JSTemporalPrecisionRecord.create(AUTO, Unit.NANOSECOND, 1);
                case 0 -> JSTemporalPrecisionRecord.create(0, Unit.SECOND, 1);
                case 1, 2, 3 -> JSTemporalPrecisionRecord.create(fractionalDigitCount, Unit.MILLISECOND, (int) Math.pow(10, 3 - fractionalDigitCount));
                case 4, 5, 6 -> JSTemporalPrecisionRecord.create(fractionalDigitCount, Unit.MICROSECOND, (int) Math.pow(10, 6 - fractionalDigitCount));
                case 7, 8, 9 -> JSTemporalPrecisionRecord.create(fractionalDigitCount, Unit.NANOSECOND, (int) Math.pow(10, 9 - fractionalDigitCount));
                default -> throw Errors.shouldNotReachHereUnexpectedValue(fractionalDigitCount);
            };
            default -> throw Errors.shouldNotReachHereUnexpectedValue(smallestUnit);
        };
    }

    @TruffleBoundary
    public static ParseISODateTimeResult parseTemporalRelativeToString(TruffleString isoString) {
        TemporalParser parser = new TemporalParser(isoString);
        JSTemporalParserRecord rec = parser.parseAnnotatedDateTime(true, false);
        if (rec == null) {
            rec = parser.parseAnnotatedDateTime(false, false);
        }
        if (rec == null) {
            throw TemporalErrors.createRangeErrorInvalidRelativeToString();
        }
        return parseISODateTimeIntl(isoString, rec);
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalMonthDayString(TruffleString string) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseTemporalMonthDayString();
        if (rec != null) {
            if (rec.getYear() == 0 && Strings.indexOf(string, TemporalConstants.MINUS_000000) >= 0) {
                throw TemporalErrors.createRangeErrorInvalidPlainDateTime();
            }
            if (rec.getCalendar() != null && rec.getYear() == Long.MIN_VALUE && !"iso8601".equalsIgnoreCase(rec.getCalendar().toJavaStringUncached())) {
                throw TemporalErrors.createRangeErrorTemporalISO8601Expected();
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

    private static ParseISODateTimeResult parseISODateTimeIntl(TruffleString string, JSTemporalParserRecord rec) {
        TruffleString fraction = rec.getFraction();
        if (fraction == null) {
            fraction = TemporalConstants.ZEROS;
        } else {
            fraction = Strings.concat(fraction, TemporalConstants.ZEROS);
        }

        if (rec.getYear() == 0 && Strings.indexOf(string, TemporalConstants.MINUS_000000) >= 0) {
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

        JSTemporalTimeZoneRecord timeZoneResult = JSTemporalTimeZoneRecord.create(rec.getZ(), rec.getTimeZoneNumericUTCOffset(), rec.getTimeZoneAnnotation());
        return new ParseISODateTimeResult(y, m, d, h, min, s, ms, mus, ns, rec.getCalendar(), timeZoneResult);
    }

    public static void validateTemporalUnitRange(Unit largestUnit, Unit smallestUnit) {
        if (largerOfTwoTemporalUnits(largestUnit, smallestUnit) != largestUnit) {
            throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
        }
    }

    public static Integer maximumTemporalDurationRoundingIncrement(Unit unit) {
        if (unit == Unit.YEAR || unit == Unit.MONTH || unit == Unit.WEEK || unit == Unit.DAY) {
            return null; // Undefined according to spec, we fix at consumer
        }
        if (unit == Unit.HOUR) {
            return 24;
        }
        if (unit == Unit.MINUTE || unit == Unit.SECOND) {
            return 60;
        }
        assert unit == Unit.MILLISECOND || unit == Unit.MICROSECOND || unit == Unit.NANOSECOND;
        return 1000;
    }

    // 13.32
    @TruffleBoundary
    public static TruffleString formatSecondsStringPart(long second, long millisecond, long microsecond, long nanosecond,
                    Object precision) {
        assert JSTemporalPrecisionRecord.isValidPrecision(precision) : precision;
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
            fractionString = Strings.lazySubstring(fractionString, 0, (int) precision);
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

    public static int nonNegativeModulo(double x, int y) {
        assert y > 0 : y;
        int result = (int) (x % y);
        if (result < 0) {
            result = result + y;
        }
        return result;
    }

    public static int nonNegativeModulo(long x, int y) {
        assert y > 0 : y;
        int result = (int) (x % y);
        if (result < 0) {
            result = result + y;
        }
        return result;
    }

    public static int nonNegativeModulo(int x, int y) {
        assert y > 0 : y;
        int result = x % y;
        if (result < 0) {
            result = result + y;
        }
        return result;
    }

    public static int constrainToRange(int value, int minimum, int maximum) {
        return Math.min(Math.max(value, minimum), maximum);
    }

    public static UnsignedRoundingMode getUnsignedRoundingMode(RoundingMode roundingMode, boolean isNegative) {
        switch (roundingMode) {
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
            default:
                return UnsignedRoundingMode.EMPTY;
        }
    }

    public static double applyUnsignedRoundingMode(double x, double r1, double r2, UnsignedRoundingMode urm) {
        assert urm != UnsignedRoundingMode.EMPTY;
        if (x == r1) {
            return r1;
        }
        assert r1 <= x && x <= r2;
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
    public static double applyUnsignedRoundingMode(BigInt numerator, BigInt denominator, double r1, double r2, UnsignedRoundingMode urm) {
        assert urm != UnsignedRoundingMode.EMPTY;
        if (numerator.signum() == 0) { // x == r1
            return r1;
        }
        if (urm == UnsignedRoundingMode.ZERO) {
            return r1;
        }
        if (urm == UnsignedRoundingMode.INFINITY) {
            return r2;
        }
        // (|n|/|d| <=> 0.5) == (2*|n| <=> |d|)
        int half = TemporalUtil.compareHalf(numerator, denominator);
        if (half < 0) {
            return r1;
        } else if (half > 0) {
            return r2;
        }
        // exactly half
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
    public static double roundNumberToIncrement(double x, double increment, RoundingMode roundingMode) {
        assert JSRuntime.isIntegralNumber(increment) : increment;

        double quotient = x / increment;
        boolean isNegative;
        if (quotient < 0) {
            isNegative = true;
            quotient = -quotient;
        } else {
            isNegative = false;
        }
        UnsignedRoundingMode unsignedRoundingMode = getUnsignedRoundingMode(roundingMode, isNegative);

        // Let r1 be the largest integer such that r1 <= quotient.
        double r1 = Math.floor(quotient);
        // Let r2 be the smallest integer such that r2 > quotient.
        double r2 = r1 + 1;

        double rounded = applyUnsignedRoundingMode(quotient, r1, r2, unsignedRoundingMode);
        if (isNegative) {
            rounded = -rounded;
        }

        return rounded * increment;
    }

    @TruffleBoundary
    public static BigInt roundNumberToIncrementAsIfPositive(BigInt x, BigInt increment, RoundingMode roundingMode) {
        BigInt[] divRes = x.divideAndRemainder(increment);
        BigInt quotient = divRes[0];
        BigInt remainder = divRes[1];
        int sign = remainder.signum();
        if (sign == 0) {
            // already a multiple of increment, no rounding needed.
            return x;
        }

        UnsignedRoundingMode unsignedRoundingMode = getUnsignedRoundingMode(roundingMode, false);
        BigInt r1 = sign < 0 ? quotient.subtract(BigInt.ONE) : quotient;
        BigInt r2 = sign >= 0 ? quotient.add(BigInt.ONE) : quotient;
        BigInt rounded = applyUnsignedRoundingMode(remainder, increment, r1, r2, unsignedRoundingMode);
        return rounded.multiply(increment);
    }

    private static BigInt applyUnsignedRoundingMode(BigInt remainder, BigInt increment, BigInt r1, BigInt r2, UnsignedRoundingMode unsignedRoundingMode) {
        int half = compareHalf(remainder, increment);
        return switch (unsignedRoundingMode) {
            // ("floor", "trunc")
            case ZERO -> r1;
            // ("ceil", "expand")
            case INFINITY -> r2;
            // ("halfFloor", "halfTrunc")
            case HALF_ZERO -> half <= 0 ? r1 : r2;
            // ("halfCeil", "halfExpand")
            case HALF_INFINITY -> half < 0 ? r1 : r2;
            // ("halfEven")
            case HALF_EVEN -> {
                if (half < 0) {
                    yield r1;
                } else if (half > 0) {
                    yield r2;
                } else {
                    yield isEven(r1) ? r1 : r2;
                }
            }
            default -> throw Errors.shouldNotReachHereUnexpectedValue(unsignedRoundingMode);
        };
    }

    /**
     * Returns the equivalent of comparing the decimal part of the quotient against 0.5, but
     * calculated from the division's remainder and divisor.
     */
    private static int compareHalf(BigInt remainder, BigInt divisor) {
        return remainder.multiply(BigInt.valueOf(2)).abs().compareTo(divisor.abs());
    }

    private static boolean isEven(BigInt value) {
        return !value.testBit(0);
    }

    @TruffleBoundary
    public static BigInt roundNormalizedTimeDurationToIncrement(BigInt normalizedTimeDuration, long unitLengthInNs, int increment, RoundingMode roundingMode) {
        BigInt normalizedIncrement = BigInt.valueOf(unitLengthInNs).multiply(BigInt.valueOf(increment));
        return roundNormalizedTimeDurationToIncrement(normalizedTimeDuration, normalizedIncrement, roundingMode);
    }

    @TruffleBoundary
    public static BigInt roundNormalizedTimeDurationToIncrement(BigInt normalizedTimeDuration, BigInt increment, RoundingMode roundingMode) {
        BigInt[] divRes = normalizedTimeDuration.divideAndRemainder(increment);
        BigInt quotient = divRes[0];
        BigInt remainder = divRes[1];
        int sign = remainder.signum();
        if (sign == 0) {
            // already a multiple of increment, no rounding needed.
            return normalizedTimeDuration;
        }
        boolean isNegative = sign < 0;
        if (isNegative) {
            quotient = quotient.negate();
        }
        UnsignedRoundingMode unsignedRoundingMode = getUnsignedRoundingMode(roundingMode, isNegative);
        BigInt r1 = quotient;
        BigInt r2 = quotient.add(BigInt.ONE);
        BigInt rounded = applyUnsignedRoundingMode(remainder, increment, r1, r2, unsignedRoundingMode);
        if (isNegative) {
            rounded = rounded.negate();
        }
        return rounded.multiply(increment);
    }

    @TruffleBoundary
    public static TruffleString parseTemporalCalendarString(TruffleString string) {
        TemporalParser parser = new TemporalParser(string);
        JSTemporalParserRecord rec = parser.parseCalendarString();
        if (rec != null) {
            TruffleString calendar = rec.getCalendar();
            if (calendar == null) {
                return ISO8601;
            }
            return calendar;
        }
        if (parser.parseAnnotationValue()) {
            return string;
        } else {
            throw Errors.createRangeError("cannot parse Calendar");
        }
    }

    public static double toPositiveInteger(Object value) {
        double result = JSRuntime.doubleValue(toIntegerWithTruncation(value));
        if (result <= 0) {
            throw Errors.createRangeError("positive value expected");
        }
        return result;
    }

    @TruffleBoundary
    public static JSObject prepareCalendarFields(JSContext ctx, TruffleString calendar, Object fields, List<TruffleString> calendarFieldNames, List<TruffleString> nonCalendarFieldNames,
                    List<TruffleString> requiredFields) {
        List<TruffleString> fieldNames = calendarFieldNames;
        if (!nonCalendarFieldNames.isEmpty()) {
            fieldNames = new ArrayList<>(fieldNames);
            fieldNames.addAll(nonCalendarFieldNames);
        }
        List<TruffleString> extraFieldNames = calendarExtraFields(calendar, calendarFieldNames);
        if (!extraFieldNames.isEmpty()) {
            fieldNames = new ArrayList<>(fieldNames);
            fieldNames.addAll(extraFieldNames);
        }

        JSObject result = JSOrdinary.createWithNullPrototype(ctx);
        boolean any = false;
        List<TruffleString> sortedPropertyNames = new ArrayList<>(fieldNames);
        sortedPropertyNames.sort(Strings::compareTo);

        for (TruffleString property : sortedPropertyNames) {
            Object value = JSRuntime.get(fields, property);
            if (value == Undefined.instance) {
                if (requiredFields == null) {
                    continue;
                } else if (requiredFields.contains(property)) {
                    throw TemporalErrors.createTypeErrorPropertyRequired(property);
                } else {
                    assert temporalFieldDefaults.containsKey(property);
                    value = temporalFieldDefaults.get(property);
                }
            } else {
                any = true;
                assert temporalFieldConversion.containsKey(property);
                Function<Object, Object> conversion = temporalFieldConversion.get(property);
                value = conversion.apply(value);
            }
            createDataPropertyOrThrow(ctx, result, property, value);
        }

        // If requiredFields is PARTIAL and any is false
        if (requiredFields == null && !any) {
            throw Errors.createTypeError("No relevant field provided");
        }
        return result;
    }

    @TruffleBoundary
    public static List<TruffleString> calendarExtraFields(TruffleString calendar, List<TruffleString> calendarFieldNames) {
        if (!ISO8601.equals(calendar) && calendarFieldNames.contains(YEAR) && IntlUtil.calendarSupportsEra(calendar)) {
            return List.of(ERA, ERA_YEAR);
        }
        return listEmpty;
    }

    public record ISOYearMonthRecord(int year, int month) {
    }

    public static ISOYearMonthRecord regulateISOYearMonth(int year, int month, Overflow overflow) {
        assert Overflow.CONSTRAIN == overflow || Overflow.REJECT == overflow;

        if (Overflow.CONSTRAIN == overflow) {
            return constrainISOYearMonth(year, month);
        } else {
            assert Overflow.REJECT == overflow;
            if (!isValidISOMonth(month)) {
                throw Errors.createRangeError("validation of year and month failed");
            }
        }
        return new ISOYearMonthRecord(year, month);
    }

    private static boolean isValidISOMonth(int month) {
        return (1 <= month) && (month <= 12);
    }

    private static ISOYearMonthRecord constrainISOYearMonth(int year, int month) {
        int monthPrepared = constrainToRange(month, 1, 12);
        return new ISOYearMonthRecord(ltoi(year), monthPrepared);
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

    // ToISOWeekOfYear(year, month, day).[[Week]]
    @TruffleBoundary
    public static long weekOfToISOWeekOfYear(int year, int month, int day) {
        long wednesday = 3;
        long thursday = 4;
        long friday = 5;
        long saturday = 6;
        long daysInWeek = 7;
        long maxWeekNumber = 53;
        long dayOfYear = toISODayOfYear(year, month, day);
        long dayOfWeek = toISODayOfWeek(year, month, day);

        long week = Math.floorDiv(dayOfYear + daysInWeek - dayOfWeek + wednesday, daysInWeek);
        if (week < 1) {
            long dayOfJan1st = toISODayOfWeek(year, 1, 1);
            if (dayOfJan1st == friday || (dayOfJan1st == saturday && JSDate.isLeapYear(year - 1))) {
                return maxWeekNumber;
            } else {
                return maxWeekNumber - 1;
            }
        }
        if (week == maxWeekNumber) {
            long daysInYear = isoDaysInYear(year);
            long daysLaterInYear = daysInYear - dayOfYear;
            long daysAfterThursday = thursday - dayOfWeek;
            if (daysLaterInYear < daysAfterThursday) {
                return 1;
            }
        }

        return week;
    }

    // ToISOWeekOfYear(year, month, day).[[Year]]
    @TruffleBoundary
    public static long yearOfToISOWeekOfYear(int year, int month, int day) {
        long wednesday = 3;
        long thursday = 4;
        long daysInWeek = 7;
        long maxWeekNumber = 53;
        long dayOfYear = toISODayOfYear(year, month, day);
        long dayOfWeek = toISODayOfWeek(year, month, day);

        long week = Math.floorDiv(dayOfYear + daysInWeek - dayOfWeek + wednesday, daysInWeek);
        if (week < 1) {
            return year - 1;
        }
        if (week == maxWeekNumber) {
            long daysInYear = isoDaysInYear(year);
            long daysLaterInYear = daysInYear - dayOfYear;
            long daysAfterThursday = thursday - dayOfWeek;
            if (daysLaterInYear < daysAfterThursday) {
                return year + 1;
            }
        }

        return year;
    }

    public static int isoDaysInYear(int year) {
        if (JSDate.isLeapYear(year)) {
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
        if (JSDate.isLeapYear(year)) {
            return 29;
        }
        return 28;
    }

    public static ISODateRecord balanceISOYearMonth(int year, int month) {
        if (year == Integer.MAX_VALUE || year == Integer.MIN_VALUE || month == Integer.MAX_VALUE || month == Integer.MIN_VALUE) {
            throw Errors.createRangeError("value out of range");
        }
        int yearPrepared = (int) (year + Math.floor((month - 1.0) / 12.0));
        int monthPrepared = nonNegativeModulo(month - 1, 12) + 1;
        return new ISODateRecord(yearPrepared, monthPrepared, 0);
    }

    public static ISODateRecord balanceISOYearMonth(double year, double month) {
        assert JSRuntime.isIntegralNumber(year) : year;
        assert JSRuntime.isIntegralNumber(month) : month;
        double yearPrepared = year + Math.floor((month - 1) / 12.0);
        int monthPrepared = nonNegativeModulo(month - 1, 12) + 1;
        // Cast to int is ok since BalanceISODate will anyway throw if the year is outside the
        // supported range.
        return new ISODateRecord((int) yearPrepared, monthPrepared, 0);
    }

    @TruffleBoundary
    public static ParseISODateTimeResult parseTemporalDateTimeString(boolean zoned, TruffleString string) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseAnnotatedDateTime(zoned, false);
        if (rec == null) {
            throw Errors.createRangeError("cannot parse the date string");
        }
        return parseISODateTimeIntl(string, rec);
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalDateString(TruffleString string) {
        JSTemporalDateTimeRecord rec = parseTemporalDateTimeString(false, string);
        return JSTemporalDateTimeRecord.createCalendar(rec.getYear(), rec.getMonth(), rec.getDay(), 0, 0, 0, 0, 0, 0, rec.getCalendar());
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord parseTemporalTimeString(TruffleString string) {
        JSTemporalParserRecord rec = (new TemporalParser(string)).parseTemporalTimeString();
        if (rec != null) {
            JSTemporalDateTimeRecord result = parseISODateTimeIntl(string, rec);
            if (result.hasCalendar()) {
                return JSTemporalDateTimeRecord.createCalendar(0, 0, 0, result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(),
                                result.getNanosecond(), result.getCalendar());
            } else {
                return JSTemporalDateTimeRecord.create(0, 0, 0, result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
            }
        } else {
            throw Errors.createRangeError("cannot parse time");
        }
    }

    @TruffleBoundary
    public static TruffleString buildISOMonthCode(int month) {
        TruffleString numberPart = Strings.fromInt(month);
        assert 1 <= Strings.length(numberPart) && Strings.length(numberPart) <= 2;
        return Strings.concat(Strings.length(numberPart) >= 2 ? Strings.UC_M : Strings.UC_M0, numberPart);
    }

    @TruffleBoundary
    public static Pair<TruffleString, TruffleString> getAvailableNamedTimeZoneIdentifier(TruffleString timeZone) {
        Pair<String, String> pair = JSDateTimeFormat.getAvailableNamedTimeZoneIdentifier(Strings.toJavaString(timeZone));
        return (pair == null) ? null : new Pair<>(Strings.fromJavaString(pair.getFirst()), Strings.fromJavaString(pair.getSecond()));
    }

    public static boolean isoDateWithinLimits(int year, int month, int day) {
        return isoDateTimeWithinLimits(year, month, day, 0, 0, 0, 0, 0, 0);
    }

    @TruffleBoundary
    public static boolean isoDateTimeWithinLimits(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond) {
        if (-isoTimeBoundYears <= year && year <= isoTimeBoundYears) {
            // fastpath check
            assert isoDateTimeWithinLimitsExact(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
            return true;
        } else if (year >= Year.MIN_VALUE && year <= Year.MAX_VALUE) {
            return isoDateTimeWithinLimitsExact(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
        } else {
            // way past the supported range, not supported by LocalDate.
            return false;
        }
    }

    @TruffleBoundary
    private static boolean isoDateTimeWithinLimitsExact(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond) {
        BigInt ns = getUTCEpochNanoseconds(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
        return ns.compareTo(isoTimeLowerBound) > 0 && ns.compareTo(isoTimeUpperBound) < 0;
    }

    /**
     * The abstract operation GetUTCEpochNanoseconds.
     *
     * @param year an integer
     * @param month an integer in the inclusive interval from 1 to 12
     * @param day an integer in the inclusive interval from 1 to 31
     * @param hour an integer in the inclusive interval from 0 to 23
     * @param minute an integer in the inclusive interval from 0 to 59
     * @param second an integer in the inclusive interval from 0 to 59
     * @param millisecond an integer in the inclusive interval from 0 to 999
     * @param microsecond an integer in the inclusive interval from 0 to 999
     * @param nanosecond an integer in the inclusive interval from 0 to 999
     * @return number of nanoseconds since the epoch that corresponds to the given ISO 8601 calendar
     *         date and wall-clock time in UTC.
     */
    @TruffleBoundary
    public static BigInt getUTCEpochNanoseconds(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond) {
        // 1. Let date be MakeDay(year, month - 1, day).
        long date = JSDate.isoDateToEpochDays(year, month - 1, day);
        // 2. Let time be MakeTime(hour, minute, second, millisecond).
        long time = hour * JSDate.MS_PER_HOUR + minute * JSDate.MS_PER_MINUTE + second * JSDate.MS_PER_SECOND + millisecond;
        // 3. Let ms be MakeDate(date, time).
        BigInt ms = BigInt.valueOf(date).multiply(BigInt.valueOf(JSDate.MS_PER_DAY)).add(BigInt.valueOf(time));

        // Let epochNanoseconds be ms * 1e6 + microsecond * 1e3 + nanosecond.
        BigInt epochNanoseconds = ms.multiply(BI_NS_PER_MS).add(BigInt.valueOf(microsecond * 1000)).add(BigInt.valueOf(nanosecond));
        return epochNanoseconds;
    }

    @TruffleBoundary
    public static Overflow toTemporalOverflow(Object options) {
        if (options == Undefined.instance) {
            return Overflow.CONSTRAIN;
        }
        Object value = JSRuntime.get(options, OVERFLOW);
        if (value == Undefined.instance) {
            return Overflow.CONSTRAIN;
        }
        TruffleString strValue = JSRuntime.toString(value);
        if (!listConstrainReject.contains(strValue)) {
            throw TemporalErrors.createRangeErrorOptionsNotContained(listConstrainReject, strValue);
        }
        return toOverflow(strValue);
    }

    @InliningCutoff
    public static Overflow toTemporalOverflow(Object options, TemporalGetOptionNode getOptionNode) {
        if (options == Undefined.instance) {
            return Overflow.CONSTRAIN;
        }
        TruffleString result = (TruffleString) getOptionNode.execute(options, OVERFLOW, OptionType.STRING, listConstrainReject, CONSTRAIN);
        return toOverflow(result);
    }

    @InliningCutoff
    public static Overflow getTemporalOverflowOption(Object options, TemporalGetOptionNode getOptionNode) {
        TruffleString result = (TruffleString) getOptionNode.execute(options, OVERFLOW, OptionType.STRING, listConstrainReject, CONSTRAIN);
        return toOverflow(result);
    }

    @TruffleBoundary
    private static Overflow toOverflow(TruffleString result) {
        if (CONSTRAIN.equals(result)) {
            return Overflow.CONSTRAIN;
        } else if (REJECT.equals(result)) {
            return Overflow.REJECT;
        }
        throw Errors.shouldNotReachHereUnexpectedValue(result);
    }

    public static JSTemporalDateTimeRecord interpretTemporalDateTimeFields(TruffleString calendar, JSDynamicObject fields, Overflow overflow,
                    TemporalCalendarDateFromFieldsNode dateFromFieldsNode) {
        JSTemporalDateTimeRecord timeResult = toTemporalTimeRecord(fields);
        JSTemporalPlainDateObject date = dateFromFieldsNode.execute(calendar, fields, overflow);
        JSTemporalDurationRecord timeResult2 = regulateTime(
                        timeResult.getHour(), timeResult.getMinute(), timeResult.getSecond(), timeResult.getMillisecond(), timeResult.getMicrosecond(), timeResult.getNanosecond(),
                        overflow);
        return JSTemporalDateTimeRecord.create(
                        date.getYear(), date.getMonth(), date.getDay(),
                        dtoi(timeResult2.getHours()), dtoi(timeResult2.getMinutes()), dtoi(timeResult2.getSeconds()),
                        dtoi(timeResult2.getMilliseconds()), dtoi(timeResult2.getMicroseconds()), dtoi(timeResult2.getNanoseconds()));
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

    @TruffleBoundary
    public static JSTemporalDateTimeRecord toTemporalTimeRecord(Object temporalTimeLike) {
        boolean any = false;

        int hour = 0;
        int minute = 0;
        int second = 0;
        int millisecond = 0;
        int microsecond = 0;
        int nanosecond = 0;

        for (TruffleString property : TIME_LIKE_PROPERTIES) {
            Object val = JSRuntime.get(temporalTimeLike, property);

            int iVal = 0;
            if (val == Undefined.instance) {
                iVal = 0;
            } else {
                any = true;
                iVal = JSRuntime.intValue(toIntegerWithTruncation(val));
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
        return JSRuntime.truncateDouble(d);
    }

    @TruffleBoundary
    public static Number toIntegerWithTruncation(Object value) {
        Number number = JSRuntime.toNumber(value);
        double d = number.doubleValue();
        if (!Double.isFinite(d)) {
            throw Errors.createRangeError("Invalid integer");
        }
        return JSRuntime.truncateDouble(d);
    }

    public static JSTemporalPlainDateObject calendarDateAdd(JSContext context, JSRealm realm, TruffleString calendar, JSTemporalPlainDateObject isoDate, JSTemporalDurationObject duration,
                    Overflow overflow,
                    Node node, InlinedBranchProfile errorBranch) {
        BigInt norm = TemporalUtil.normalizeTimeDuration(duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                        duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
        TimeDurationRecord balanceResult = TemporalUtil.balanceTimeDuration(norm, Unit.DAY);
        double days = duration.getDays() + balanceResult.days();
        ISODateRecord result = TemporalUtil.addISODate(isoDate.getYear(), isoDate.getMonth(), isoDate.getDay(),
                        duration.getYears(), duration.getMonths(), duration.getWeeks(), days, overflow);
        return JSTemporalPlainDate.create(context, realm, result.year(), result.month(), result.day(), calendar, node, errorBranch);
    }

    // This method implements CalendarDateUntil() operation. Unfortunately,
    // the algorithm used by the specification is very slow. So, we use
    // an optimized version of this algorithm (i.e. we use slightly different
    // steps than the specification).
    public static JSTemporalDurationObject calendarDateUntil(JSContext context, JSRealm realm, @SuppressWarnings("unused") TruffleString calendar, JSTemporalPlainDateObject one,
                    JSTemporalPlainDateObject two, Unit largestUnit, Node node, InlinedBranchProfile errorBranch) {
        int sign = -compareISODate(one.getYear(), one.getMonth(), one.getDay(), two.getYear(), two.getMonth(), two.getDay());
        if (sign == 0) {
            return JSTemporalDuration.createTemporalDuration(context, realm, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
        }

        int years = 0;
        int months = 0;
        ISODateRecord intermediate;
        if (largestUnit == Unit.YEAR || largestUnit == Unit.MONTH) {
            int candidateYears = two.getYear() - one.getYear();
            if (candidateYears != 0) {
                candidateYears -= sign;
            }
            while (!isoDateSurpasses(sign, one.getYear() + candidateYears, one.getMonth(), one.getDay(), two)) {
                years = candidateYears;
                candidateYears += sign;
            }

            int candidateMonths = sign;
            intermediate = balanceISOYearMonth(one.getYear() + years, one.getMonth() + candidateMonths);
            while (!isoDateSurpasses(sign, intermediate.year(), intermediate.month(), one.getDay(), two)) {
                months = candidateMonths;
                candidateMonths += sign;
                intermediate = balanceISOYearMonth(intermediate.year(), intermediate.month() + sign);
            }
            if (largestUnit == Unit.MONTH) {
                months += 12 * years;
                years = 0;
            }
        }

        intermediate = balanceISOYearMonth(one.getYear() + years, one.getMonth() + months);
        ISODateRecord constrained = regulateISODate(intermediate.year(), intermediate.month(), one.getDay(), Overflow.CONSTRAIN);
        int weeks = 0;
        int days = (int) (JSDate.isoDateToEpochDays(two.getYear(), two.getMonth() - 1, two.getDay()) - JSDate.isoDateToEpochDays(constrained.year(), constrained.month() - 1, constrained.day()));

        if (largestUnit == Unit.WEEK) {
            weeks = days / 7;
            days %= 7;
        }

        return JSTemporalDuration.createTemporalDuration(context, realm, years, months, weeks, days, 0, 0, 0, 0, 0, 0, node, errorBranch);
    }

    private static boolean isoDateSurpasses(int sign, int y1, int m1, int d1, JSTemporalPlainDateObject isoDate2) {
        if (y1 != isoDate2.getYear()) {
            return (sign * (y1 - isoDate2.getYear()) > 0);
        } else if (m1 != isoDate2.getMonth()) {
            return (sign * (m1 - isoDate2.getMonth()) > 0);
        } else if (d1 != isoDate2.getDay()) {
            return (sign * (d1 - isoDate2.getDay()) > 0);
        } else {
            return false;
        }
    }

    @TruffleBoundary
    public static BigInt roundTemporalInstant(BigInt ns, int increment, Unit unit, RoundingMode roundingMode) {
        BigInt incrementNs = BigInt.valueOf(increment);
        if (Unit.HOUR == unit) {
            incrementNs = incrementNs.multiply(BI_NS_PER_HOUR);
        } else if (Unit.MINUTE == unit) {
            incrementNs = incrementNs.multiply(BI_NS_PER_MINUTE);
        } else if (Unit.SECOND == unit) {
            incrementNs = incrementNs.multiply(BI_NS_PER_SECOND);
        } else if (Unit.MILLISECOND == unit) {
            incrementNs = incrementNs.multiply(BI_NS_PER_MS);
        } else if (Unit.MICROSECOND == unit) {
            incrementNs = incrementNs.multiply(BI_1000);
        } else {
            assert Unit.NANOSECOND == unit : unit;
            if (incrementNs.compareTo(BigInt.ONE) == 0) {
                return ns;
            }
        }
        return roundNumberToIncrementAsIfPositive(ns, incrementNs, roundingMode);
    }

    public static ISODateRecord regulateISODate(int year, int monthParam, int dayParam, Overflow overflow) {
        assert overflow == Overflow.CONSTRAIN || overflow == Overflow.REJECT;
        int month = monthParam;
        int day = dayParam;
        if (overflow == Overflow.REJECT) {
            if (!isValidISODate(year, month, day)) {
                throw TemporalErrors.createRangeErrorDateOutsideRange();
            }
        } else {
            assert overflow == Overflow.CONSTRAIN;
            month = constrainToRange(month, 1, 12);
            day = constrainToRange(day, 1, isoDaysInMonth(year, month));
        }
        return new ISODateRecord(year, month, day);
    }

    @TruffleBoundary
    public static ISODateRecord balanceISODate(int year, int month, int day) {
        if (year < -1000000 || 1000000 < year || day < -ISO_DATE_MAX_UTC_OFFSET_DAYS || ISO_DATE_MAX_UTC_OFFSET_DAYS < day) {
            // This check is sometimes performed only after AddISODate in the spec.
            // We do it earlier to avoid having to deal with non-finite epoch days.
            // This is OK since all callers would throw a RangeError immediately after anyway.
            throw Errors.createRangeError("Date outside of supported range");
        }
        long epochDays = JSDate.isoDateToEpochDays(year, month - 1, day);
        long ms = epochDays * JSDate.MS_PER_DAY;
        return createISODateRecord(JSDate.yearFromTime(ms), JSDate.monthFromTime(ms) + 1, JSDate.dateFromTime(ms));
    }

    @TruffleBoundary
    public static ISODateRecord createISODateRecord(int year, int month, int day) {
        assert isValidISODate(year, month, day);
        return new ISODateRecord(year, month, day);
    }

    @TruffleBoundary
    public static ISODateRecord balanceISODate(int year, int month, double day) {
        assert JSRuntime.isIntegralNumber(day);
        return balanceISODate(year, month, (int) day);
    }

    @TruffleBoundary
    public static ISODateRecord balanceISODate(double year, double month, double day) {
        assert JSRuntime.isIntegralNumber(year) && JSRuntime.isIntegralNumber(month) && JSRuntime.isIntegralNumber(day);
        return balanceISODate((int) year, (int) month, (int) day);
    }

    /**
     * Add ISO date. Only called with int range values, or constrained immediately afterwards.
     */
    @TruffleBoundary
    public static ISODateRecord addISODate(int year, int month, int day, int years, int months, int weeks, int daysP, Overflow overflow) {
        assert overflow == Overflow.CONSTRAIN || overflow == Overflow.REJECT;

        int days = daysP;
        var intermediateYM = balanceISOYearMonth(year + years, month + months);
        ISODateRecord intermediate = regulateISODate(intermediateYM.year(), intermediateYM.month(), day, overflow);
        days = days + 7 * weeks;
        int d = add(intermediate.day(), days, overflow);
        return balanceISODate(intermediate.year(), intermediate.month(), d);
    }

    /**
     * Add duration. Both the duration and the result can be outside the valid ISO Date range.
     * However, we eager throw for values outside the supported range for simplicity.
     */
    @TruffleBoundary
    public static ISODateRecord addISODate(int year, int month, int day, double years, double months, double weeks, double daysP, Overflow overflow) {
        assert overflow == Overflow.CONSTRAIN || overflow == Overflow.REJECT;

        double days = daysP;
        var intermediateYM = balanceISOYearMonth(year + years, month + months);
        ISODateRecord intermediate = regulateISODate(intermediateYM.year(), intermediateYM.month(), day, overflow);
        days = days + 7 * weeks;
        intermediate = balanceISODate(intermediate.year(), intermediate.month(), intermediate.day() + days);
        return regulateISODate(intermediate.year(), intermediate.month(), intermediate.day(), overflow);
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

    public static boolean isTemporalZonedDateTime(Object obj) {
        return JSTemporalZonedDateTime.isJSTemporalZonedDateTime(obj);
    }

    public static ShowCalendar toShowCalendarOption(JSDynamicObject options, TemporalGetOptionNode getOptionNode, TruffleString.EqualNode equalNode) {
        return toShowCalendar((TruffleString) getOptionNode.execute(options, TemporalConstants.CALENDAR_NAME, OptionType.STRING, listAutoAlwaysNeverCritical, AUTO), equalNode);
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
    public static TruffleString maybeFormatCalendarAnnotation(Object calendar, ShowCalendar showCalendar) {
        if (showCalendar == ShowCalendar.NEVER) {
            return Strings.EMPTY_STRING;
        } else {
            TruffleString calendarID = ToTemporalCalendarIdentifierNode.getUncached().executeString(calendar);
            return formatCalendarAnnotation(calendarID, showCalendar);
        }
    }

    @TruffleBoundary
    public static TruffleString formatCalendarAnnotation(TruffleString id, ShowCalendar showCalendar) {
        if (ShowCalendar.NEVER == showCalendar) {
            return Strings.EMPTY_STRING;
        } else if (ShowCalendar.AUTO == showCalendar && ISO8601.equals(id)) {
            return Strings.EMPTY_STRING;
        } else {
            TruffleString flag = (showCalendar == ShowCalendar.CRITICAL) ? Strings.EXCLAMATION_MARK : Strings.EMPTY_STRING;
            return Strings.concatAll(Strings.BRACKET_OPEN, flag, TemporalConstants.U_CA_EQUALS, id, Strings.BRACKET_CLOSE);
        }
    }

    public static RoundingMode negateTemporalRoundingMode(RoundingMode roundingMode) {
        return switch (roundingMode) {
            case CEIL -> RoundingMode.FLOOR;
            case FLOOR -> RoundingMode.CEIL;
            case HALF_FLOOR -> RoundingMode.HALF_CEIL;
            case HALF_CEIL -> RoundingMode.HALF_FLOOR;
            default -> roundingMode;
        };
    }

    public static boolean calendarEquals(Object one, Object two, ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
        if (one == two) {
            return true;
        }
        return Boundaries.equals(toCalendarIdentifier.executeString(one), toCalendarIdentifier.executeString(two));
    }

    @TruffleBoundary
    public static JSDynamicObject calendarMergeFields(JSContext ctx, TruffleString calendar, JSDynamicObject fields, JSDynamicObject additionalFields) {
        List<Object> additionalKeys = calendarFieldKeysPresent(additionalFields);
        EconomicSet<Object> overriddenKeys = calendarFieldKeysToIgnore(calendar, additionalKeys);
        JSDynamicObject merged = JSOrdinary.createWithNullPrototype(ctx);
        List<Object> fieldKeys = calendarFieldKeysPresent(fields);
        for (Object key : fieldKeys) {
            if (!overriddenKeys.contains(key)) {
                Object propValue = JSObject.get(fields, key);
                createDataPropertyOrThrow(ctx, merged, key, propValue);
            }
        }
        for (Object key : additionalKeys) {
            Object propValue = JSObject.get(additionalFields, key);
            createDataPropertyOrThrow(ctx, merged, key, propValue);
        }
        return merged;
    }

    private static List<Object> calendarFieldKeysPresent(JSDynamicObject fields) {
        return JSObject.ownPropertyKeys(fields);
    }

    private static EconomicSet<Object> calendarFieldKeysToIgnore(@SuppressWarnings("unused") TruffleString calendar, List<Object> keys) {
        EconomicSet<Object> ignoredKeys = EconomicSet.create();
        for (Object key : keys) {
            if (MONTH.equals(key) || MONTH_CODE.equals(key)) {
                ignoredKeys.add(MONTH);
                ignoredKeys.add(MONTH_CODE);
            } else if (YEAR.equals(key) || ERA.equals(key) || ERA_YEAR.equals(key)) {
                ignoredKeys.add(YEAR);
                ignoredKeys.add(ERA);
                ignoredKeys.add(ERA_YEAR);
            } else {
                ignoredKeys.add(key);
            }
        }
        return ignoredKeys;
    }

    public static void createDataPropertyOrThrow(JSContext ctx, JSDynamicObject obj, Object key, Object value) {
        JSObjectUtil.defineDataProperty(ctx, obj, key, value, JSAttributes.configurableEnumerableWritable());
    }

    public static Unit largerOfTwoTemporalUnits(Unit a, Unit b) {
        assert Unit.YEAR.compareTo(a) <= 0 && a.compareTo(Unit.NANOSECOND) <= 0 : a;
        assert Unit.YEAR.compareTo(b) <= 0 && b.compareTo(Unit.NANOSECOND) <= 0 : b;
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static DateDurationRecord createDateDurationRecord(double years, double months, double weeks, double days) {
        if (!isValidDuration(years, months, weeks, days, 0, 0, 0, 0, 0, 0)) {
            throw TemporalErrors.createTypeErrorDurationOutsideRange();
        }
        return new DateDurationRecord(years, months, weeks, days);
    }

    public static NormalizedDurationRecord createNormalizedDurationRecord(double years, double months, double weeks, double days, BigInt normalizedTimeDuration) {
        DateDurationRecord dateDurationRecord = createDateDurationRecord(years, months, weeks, days);
        return combineDateAndNormalizedTimeDuration(dateDurationRecord, normalizedTimeDuration);
    }

    public static NormalizedDurationRecord combineDateAndNormalizedTimeDuration(DateDurationRecord dateDuration, BigInt normalizedTimeDuration) {
        int dateSign = durationSign(dateDuration.years(), dateDuration.months(), dateDuration.weeks(), dateDuration.days(), 0, 0, 0, 0, 0, 0);
        int timeSign = normalizedTimeDurationSign(normalizedTimeDuration);
        if (dateSign != 0 && timeSign != 0 && dateSign != timeSign) {
            throw Errors.createRangeError("mixed-sign values not allowed as duration fields");
        }
        return new NormalizedDurationRecord(dateDuration.years(), dateDuration.months(), dateDuration.weeks(), dateDuration.days(), normalizedTimeDuration);
    }

    @TruffleBoundary
    public static JSObject mergeLargestUnitOption(JSContext ctx, EnumerableOwnPropertyNamesNode namesNode, JSDynamicObject options, Unit largestUnit) {
        JSObject merged = JSOrdinary.createWithNullPrototype(ctx);
        UnmodifiableArrayList<?> keys = namesNode.execute(options);
        for (Object nextKey : keys) {
            if (nextKey instanceof TruffleString key) {
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
    public static TimeDurationRecord balanceTimeDuration(BigInt normalizedTimeDuration, Unit largestUnit) {
        BigInt d = BigInt.ZERO;
        BigInt h = BigInt.ZERO;
        BigInt min = BigInt.ZERO;
        BigInt s = BigInt.ZERO;
        BigInt ms = BigInt.ZERO;
        BigInt us = BigInt.ZERO;
        double sign = normalizedTimeDurationSign(normalizedTimeDuration);
        BigInt ns = normalizedTimeDurationAbs(normalizedTimeDuration);
        switch (largestUnit) {
            case YEAR, MONTH, WEEK, DAY -> {
                var qr = ns.divideAndRemainder(BI_1000);
                us = qr[0];
                ns = qr[1];
                qr = us.divideAndRemainder(BI_1000);
                ms = qr[0];
                us = qr[1];
                qr = ms.divideAndRemainder(BI_1000);
                s = qr[0];
                ms = qr[1];
                qr = s.divideAndRemainder(BI_60);
                min = qr[0];
                s = qr[1];
                qr = min.divideAndRemainder(BI_60);
                h = qr[0];
                min = qr[1];
                qr = h.divideAndRemainder(BI_24);
                d = qr[0];
                h = qr[1];
            }
            case HOUR -> {
                var qr = ns.divideAndRemainder(BI_1000);
                us = qr[0];
                ns = qr[1];
                qr = us.divideAndRemainder(BI_1000);
                ms = qr[0];
                us = qr[1];
                qr = ms.divideAndRemainder(BI_1000);
                s = qr[0];
                ms = qr[1];
                qr = s.divideAndRemainder(BI_60);
                min = qr[0];
                s = qr[1];
                qr = min.divideAndRemainder(BI_60);
                h = qr[0];
                min = qr[1];
            }
            case MINUTE -> {
                var qr = ns.divideAndRemainder(BI_1000);
                us = qr[0];
                ns = qr[1];
                qr = us.divideAndRemainder(BI_1000);
                ms = qr[0];
                us = qr[1];
                qr = ms.divideAndRemainder(BI_1000);
                s = qr[0];
                ms = qr[1];
                qr = s.divideAndRemainder(BI_60);
                min = qr[0];
                s = qr[1];
            }
            case SECOND -> {
                var qr = ns.divideAndRemainder(BI_1000);
                us = qr[0];
                ns = qr[1];
                qr = us.divideAndRemainder(BI_1000);
                ms = qr[0];
                us = qr[1];
                qr = ms.divideAndRemainder(BI_1000);
                s = qr[0];
                ms = qr[1];
            }
            case MILLISECOND -> {
                var qr = ns.divideAndRemainder(BI_1000);
                us = qr[0];
                ns = qr[1];
                qr = us.divideAndRemainder(BI_1000);
                ms = qr[0];
                us = qr[1];
            }
            case MICROSECOND -> {
                var qr = ns.divideAndRemainder(BI_1000);
                us = qr[0];
                ns = qr[1];
            }
            default -> {
                assert largestUnit == Unit.NANOSECOND : largestUnit;
            }
        }
        double days = d.doubleValue() * sign;
        double hours = h.doubleValue() * sign;
        double minutes = min.doubleValue() * sign;
        double seconds = s.doubleValue() * sign;
        double milliseconds = ms.doubleValue() * sign;
        double microseconds = us.doubleValue() * sign;
        double nanoseconds = ns.doubleValue() * sign;
        if (!isValidDuration(0, 0, 0, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
            throw Errors.createRangeError("Time is infinite");
        }
        return new TimeDurationRecord(days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
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

    @TruffleBoundary
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
        double twoPow32 = 1L << 32;
        if (Math.abs(years) >= twoPow32 || Math.abs(months) >= twoPow32 || Math.abs(weeks) >= twoPow32) {
            return false;
        }
        BigInteger normalizedSeconds;
        normalizedSeconds = JSRuntime.toBigInteger(days).multiply(BI_86400.bigIntegerValue());
        normalizedSeconds = normalizedSeconds.add(JSRuntime.toBigInteger(hours).multiply(BI_3600.bigIntegerValue()));
        normalizedSeconds = normalizedSeconds.add(JSRuntime.toBigInteger(minutes).multiply(BI_60.bigIntegerValue()));
        normalizedSeconds = normalizedSeconds.add(JSRuntime.toBigInteger(seconds));
        BigInteger normalizedNanos = normalizedSeconds.multiply(BI_NS_PER_SECOND.bigIntegerValue());
        normalizedNanos = normalizedNanos.add(JSRuntime.toBigInteger(milliseconds).multiply(BI_1000_000.bigIntegerValue()));
        normalizedNanos = normalizedNanos.add(JSRuntime.toBigInteger(microseconds).multiply(BI_1000.bigIntegerValue()));
        normalizedNanos = normalizedNanos.add(JSRuntime.toBigInteger(nanoseconds));
        return normalizedNanos.abs().compareTo(MAX_TIME_DURATION.bigIntegerValue()) <= 0;
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

    public record AddDaysToZonedDateTimeResult(BigInt epochNanoseconds, JSTemporalInstantObject instant, JSTemporalPlainDateTimeObject dateTime) {
    }

    public static AddDaysToZonedDateTimeResult addDaysToZonedDateTime(JSContext ctx, JSRealm realm,
                    JSTemporalInstantObject instant, JSTemporalPlainDateTimeObject dateTime, TruffleString timeZone, int days) {
        return addDaysToZonedDateTime(ctx, realm, instant, dateTime, timeZone, days, Overflow.CONSTRAIN);
    }

    public static AddDaysToZonedDateTimeResult addDaysToZonedDateTime(JSContext ctx, JSRealm realm,
                    JSTemporalInstantObject instant, JSTemporalPlainDateTimeObject dateTime, TruffleString timeZone, int days, Overflow overflow) {
        if (days == 0) {
            return new AddDaysToZonedDateTimeResult(instant.getNanoseconds(), instant, dateTime);
        }

        ISODateRecord addedDate = addISODate(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), 0, 0, 0, days, overflow);
        JSTemporalPlainDateTimeObject dateTimeResult = JSTemporalPlainDateTime.create(ctx, realm,
                        addedDate.year(), addedDate.month(), addedDate.day(),
                        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
                        dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(),
                        dateTime.getCalendar());

        BigInt result = getEpochNanosecondsFor(ctx, realm, timeZone, dateTimeResult, Disambiguation.COMPATIBLE);
        return new AddDaysToZonedDateTimeResult(result, instant, dateTimeResult);
    }

    private static BigInt dtobi(double d) {
        CompilerAsserts.neverPartOfCompilation();
        return BigInt.fromBigInteger(new BigDecimal(d).toBigInteger());
    }

    /**
     * {@return normalized time duration consisting of whole seconds, and subseconds expressed in
     * nanoseconds}.
     *
     * The normalized time duration can be stored losslessly in two 64-bit floating point numbers.
     * Alternatively, normalizedSeconds * 10**9 + subseconds can be stored as a 96-bit integer.
     */
    @TruffleBoundary
    public static BigInt normalizeTimeDuration(double hours, double minutes, double seconds,
                    double milliseconds, double microseconds, double nanoseconds) {
        BigInt h = dtobi(hours);
        BigInt min = dtobi(minutes).add(h.multiply(BI_60));
        BigInt s = dtobi(seconds).add(min.multiply(BI_60));
        BigInt ms = dtobi(milliseconds).add(s.multiply(BI_1000));
        BigInt us = dtobi(microseconds).add(ms.multiply(BI_1000));
        BigInt ns = dtobi(nanoseconds).add(us.multiply(BI_1000));
        return ns;
    }

    public static JSTemporalDurationObject toDateDurationRecordWithoutTime(JSContext context, JSRealm realm, JSTemporalDurationObject duration, Node node, InlinedBranchProfile errorBranch) {
        // ToInternalDurationRecordWith24HourDays
        BigInt timeDuration = normalizeTimeDuration(duration.getHours(), duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(),
                        duration.getNanoseconds());
        timeDuration = add24HourDaysToNormalizedTimeDuration(timeDuration, duration.getDays());

        BigInt days = timeDuration.divide(BI_NS_PER_DAY);
        return JSTemporalDuration.createTemporalDuration(context, realm, duration.getYears(), duration.getMonths(), duration.getWeeks(), days.doubleValue(), 0, 0, 0, 0, 0, 0, node, errorBranch);
    }

    @TruffleBoundary
    public static BigInt add24HourDaysToNormalizedTimeDuration(BigInt timeDurationTotalNanoseconds, double days) {
        assert JSRuntime.isIntegralNumber(days) : days;
        BigInt result = timeDurationTotalNanoseconds.add(dtobi(days).multiply(BI_NS_PER_DAY));
        if (result.abs().compareTo(MAX_TIME_DURATION) > 0) {
            throw Errors.createRangeError("Time duration out of range");
        }
        return result;
    }

    @TruffleBoundary
    public static BigInt addNormalizedTimeDurationToEpochNanoseconds(BigInt timeDurationTotalNanoseconds, BigInt epochNs) {
        return epochNs.add(timeDurationTotalNanoseconds);
    }

    @TruffleBoundary
    public static BigInt addNormalizedTimeDuration(BigInt one, BigInt two) {
        BigInt result = one.add(two);
        if (result.abs().compareTo(MAX_TIME_DURATION) > 0) {
            throw Errors.createRangeError("Time duration out of range");
        }
        return result;
    }

    @TruffleBoundary
    public static BigInt subtractNormalizedTimeDuration(BigInt one, BigInt two) {
        BigInt result = one.subtract(two);
        if (result.abs().compareTo(MAX_TIME_DURATION) > 0) {
            throw Errors.createRangeError("Time duration out of range");
        }
        return result;
    }

    @TruffleBoundary
    public static BigInt normalizedTimeDurationFromEpochNanosecondsDifference(BigInt one, BigInt two) {
        BigInt result = one.subtract(two);
        assert result.abs().compareTo(MAX_TIME_DURATION) <= 0 : result;
        return result;
    }

    @TruffleBoundary
    public static double divideNormalizedTimeDurationAsDouble(BigInt normalizedTimeDuration, long divisor) {
        assert divisor != 0;
        BigDecimal result = new BigDecimal(normalizedTimeDuration.bigIntegerValue()).divide(BigDecimal.valueOf(divisor), MathContext.DECIMAL128);
        return result.doubleValue();
    }

    @TruffleBoundary
    public static double divideNormalizedTimeDurationAsDoubleTruncate(BigInt normalizedTimeDuration, long divisor) {
        assert divisor != 0;
        return normalizedTimeDuration.divide(BigInt.valueOf(divisor)).doubleValue();
    }

    @TruffleBoundary
    public static BigInt remainderNormalizedTimeDuration(BigInt normalizedTimeDuration, long divisor) {
        assert divisor != 0;
        BigDecimal result = new BigDecimal(normalizedTimeDuration.bigIntegerValue()).remainder(BigDecimal.valueOf(divisor), MathContext.DECIMAL128);
        return BigInt.fromBigInteger(result.toBigInteger());
    }

    @TruffleBoundary
    public static double normalizeTimeDurationSeconds(BigInt timeDurationTotalNanoseconds) {
        return timeDurationTotalNanoseconds.divide(BI_NS_PER_SECOND).doubleValue();
    }

    @TruffleBoundary
    public static double normalizeTimeDurationSubseconds(BigInt timeDurationTotalNanoseconds) {
        return timeDurationTotalNanoseconds.remainder(BI_NS_PER_SECOND).doubleValue();
    }

    public static BigInt normalizedTimeDurationAbs(BigInt timeDurationTotalNanoseconds) {
        return timeDurationTotalNanoseconds.abs();
    }

    public static int normalizedTimeDurationSign(BigInt timeDurationTotalNanoseconds) {
        return timeDurationTotalNanoseconds.signum();
    }

    public static BigInt zeroTimeDuration() {
        return BigInt.ZERO;
    }

    @TruffleBoundary
    public static long daysUntil(JSTemporalPlainDateObject earlier, JSTemporalPlainDateObject later) {
        long epochDays1 = JSDate.isoDateToEpochDays(earlier.getYear(), earlier.getMonth() - 1, earlier.getDay());
        long epochDays2 = JSDate.isoDateToEpochDays(later.getYear(), later.getMonth() - 1, later.getDay());
        return epochDays2 - epochDays1;
    }

    public static BigInt differenceTime(
                    int h1, int min1, int s1, int ms1, int mus1, int ns1,
                    int h2, int min2, int s2, int ms2, int mus2, int ns2) {
        int hours = h2 - h1;
        int minutes = min2 - min1;
        int seconds = s2 - s1;
        int milliseconds = ms2 - ms1;
        int microseconds = mus2 - mus1;
        int nanoseconds = ns2 - ns1;
        BigInt norm = normalizeTimeDuration(hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        assert normalizedTimeDurationAbs(norm).compareTo(BI_NS_PER_DAY) < 0 : norm;
        return norm;
    }

    /**
     * RoundTimeDuration result.
     */
    public record NormalizedDurationWithTotalRecord(NormalizedDurationRecord normalizedDuration, double total) {
    }

    public static NormalizedDurationWithTotalRecord roundTimeDuration(double days0, BigInt norm0, int increment, Unit unit, RoundingMode roundingMode) {
        assert !unit.isCalendarUnit() : unit;
        double days = days0;
        BigInt norm = norm0;
        double total;
        if (unit == Unit.DAY) {
            double fractionalDays = days + divideNormalizedTimeDurationAsDouble(norm, NS_PER_DAY_LONG);
            days = roundNumberToIncrement(fractionalDays, increment, roundingMode);
            total = fractionalDays;
            norm = zeroTimeDuration();
        } else {
            long divisor = unit.getLengthInNanoseconds();
            total = divideNormalizedTimeDurationAsDouble(norm, divisor);
            norm = roundNormalizedTimeDurationToIncrement(norm, divisor, increment, roundingMode);
        }
        return new NormalizedDurationWithTotalRecord(createNormalizedDurationRecord(0, 0, 0, days, norm), total);
    }

    @TruffleBoundary
    public static TimeRecord roundTime(int hours, int minutes, int seconds, int milliseconds, int microseconds,
                    int nanoseconds, int increment, Unit unit, RoundingMode roundingMode) {
        double quantity;
        long unitLength;
        if (unit == Unit.DAY || unit == Unit.HOUR) {
            quantity = (((((double) hours * 60 + minutes) * 60 + seconds) * 1000 + milliseconds) * 1000 + microseconds) * 1000 + nanoseconds;
            unitLength = (unit == Unit.DAY) ? NS_PER_DAY_LONG : 3_600_000_000_000L;
        } else if (unit == Unit.MINUTE) {
            quantity = ((((double) minutes * 60 + seconds) * 1000 + milliseconds) * 1000 + microseconds) * 1000 + nanoseconds;
            unitLength = 60_000_000_000L;
        } else if (unit == Unit.SECOND) {
            quantity = (((double) seconds * 1000 + milliseconds) * 1000 + microseconds) * 1000 + nanoseconds;
            unitLength = 1_000_000_000;
        } else if (unit == Unit.MILLISECOND) {
            quantity = ((double) milliseconds * 1000 + microseconds) * 1000 + nanoseconds;
            unitLength = 1_000_000;
        } else if (unit == Unit.MICROSECOND) {
            quantity = (double) microseconds * 1000 + nanoseconds;
            unitLength = 1_000;
        } else {
            assert unit == Unit.NANOSECOND;
            quantity = nanoseconds;
            unitLength = 1;
        }
        long result = dtol(roundNumberToIncrement(quantity, increment * unitLength, roundingMode)) / unitLength;
        if (unit == Unit.DAY) {
            return new TimeRecord(result, 0, 0, 0, 0, 0, 0);
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
    public static TimeRecord balanceTimeDouble(double h, double min, double sec, double mils, double mics, double ns,
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
        return new TimeRecord(days, (int) hours, (int) minutes, (int) seconds, (int) milliseconds, (int) microseconds, (int) nanoseconds);
    }

    // note: there also is balanceTimeDouble
    public static TimeRecord balanceTime(long h, long min, long sec, long mils, long mics, long ns) {
        long microseconds = mics;
        long milliseconds = mils;
        long nanoseconds = ns;
        long seconds = sec;
        long minutes = min;
        long hours = h;
        microseconds = microseconds + Math.floorDiv(nanoseconds, 1000);
        nanoseconds = nonNegativeModulo(nanoseconds, 1000);
        milliseconds = milliseconds + Math.floorDiv(microseconds, 1000);
        microseconds = nonNegativeModulo(microseconds, 1000);
        seconds = seconds + Math.floorDiv(milliseconds, 1000);
        milliseconds = nonNegativeModulo(milliseconds, 1000);
        minutes = minutes + Math.floorDiv(seconds, 60);
        seconds = nonNegativeModulo(seconds, 60);
        hours = hours + Math.floorDiv(minutes, 60);
        minutes = nonNegativeModulo(minutes, 60);
        long days = Math.floorDiv(hours, 24);
        hours = nonNegativeModulo(hours, 24);
        return new TimeRecord(days, (int) hours, (int) minutes, (int) seconds, (int) milliseconds, (int) microseconds, (int) nanoseconds);
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
    @TruffleBoundary
    public static TimeRecord addTimeDouble(int hour, int minute, int second, int millisecond, int microsecond, int nanosecond,
                    double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds) {
        var qr = JSRuntime.toBigInteger(nanoseconds).add(BigInteger.valueOf(nanosecond)).divideAndRemainder(BI_1000.bigIntegerValue());
        int ns = qr[1].intValue();
        if (ns < 0) {
            ns += 1000;
            qr[0] = qr[0].subtract(BigInteger.ONE);
        }
        qr = JSRuntime.toBigInteger(microseconds).add(BigInteger.valueOf(microsecond)).add(qr[0]).divideAndRemainder(BI_1000.bigIntegerValue());
        int us = qr[1].intValue();
        if (us < 0) {
            us += 1000;
            qr[0] = qr[0].subtract(BigInteger.ONE);
        }
        qr = JSRuntime.toBigInteger(milliseconds).add(BigInteger.valueOf(millisecond)).add(qr[0]).divideAndRemainder(BI_1000.bigIntegerValue());
        int ms = qr[1].intValue();
        if (ms < 0) {
            ms += 1000;
            qr[0] = qr[0].subtract(BigInteger.ONE);
        }
        qr = JSRuntime.toBigInteger(seconds).add(BigInteger.valueOf(second)).add(qr[0]).divideAndRemainder(BI_60.bigIntegerValue());
        int s = qr[1].intValue();
        if (s < 0) {
            s += 60;
            qr[0] = qr[0].subtract(BigInteger.ONE);
        }
        qr = JSRuntime.toBigInteger(minutes).add(BigInteger.valueOf(minute)).add(qr[0]).divideAndRemainder(BI_60.bigIntegerValue());
        int m = qr[1].intValue();
        if (m < 0) {
            m += 60;
            qr[0] = qr[0].subtract(BigInteger.ONE);
        }
        qr = JSRuntime.toBigInteger(hours).add(BigInteger.valueOf(hour)).add(qr[0]).divideAndRemainder(BI_24.bigIntegerValue());
        int h = qr[1].intValue();
        double days = qr[0].doubleValue();
        if (h < 0) {
            h += 24;
            days--;
        }
        return new TimeRecord(days, h, m, s, ms, us, ns);
    }

    @TruffleBoundary
    public static TimeRecord addTime(int hour, int minute, int second, int millisecond, int microsecond, double nanosecond,
                    BigInt normalizedTimeDuration,
                    Node node, InlinedBranchProfile errorBranch) {
        BigInt[] qr = normalizedTimeDuration.divideAndRemainder(BI_NS_PER_SECOND);
        double seconds = second + qr[0].doubleValue(); // NormalizedTimeDurationSeconds
        double nanoseconds = nanosecond + qr[1].doubleValue(); // NormalizedTimeDurationSubseconds
        return balanceTimeDouble(hour, minute, seconds, millisecond, microsecond, nanoseconds, node, errorBranch);
    }

    @TruffleBoundary
    public static JSTemporalDurationRecord roundISODateTime(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond,
                    int nanosecond, int increment, Unit unit, RoundingMode roundingMode) {
        TimeRecord rt = roundTime(hour, minute, second, millisecond, microsecond, nanosecond, increment, unit, roundingMode);
        ISODateRecord br = balanceISODate(year, month, day + dtoi(rt.days()));
        return JSTemporalDurationRecord.create(br.year(), br.month(), br.day(),
                        rt.hour(), rt.minute(), rt.second(), rt.millisecond(), rt.microsecond(), rt.nanosecond());
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

    public static boolean isValidISODate(double year, double month, double day) {
        if (month < 1 || month > 12) {
            return false;
        }
        if (day < 1 || day > isoDaysInMonth((int) (year % 400), (int) month)) {
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

    public static JSTemporalDateTimeRecord systemDateTime(JSRealm realm, Object temporalTimeZoneLike, ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifier) {
        TruffleString timeZone;
        if (temporalTimeZoneLike == Undefined.instance) {
            timeZone = systemTimeZoneIdentifier(realm);
        } else {
            timeZone = toTimeZoneIdentifier.execute(temporalTimeZoneLike);
        }
        BigInt epochNs = systemUTCEpochNanoseconds(realm);
        return getISODateTimeFor(timeZone, epochNs);
    }

    @TruffleBoundary
    public static JSTemporalPlainDateTimeObject builtinTimeZoneGetPlainDateTimeFor(JSContext ctx, JSRealm realm,
                    TruffleString timeZone, JSTemporalInstantObject instant, TruffleString calendar) {
        long offsetNanoseconds = getOffsetNanosecondsFor(timeZone, instant.getNanoseconds());
        return builtinTimeZoneGetPlainDateTimeFor(ctx, realm, instant, calendar, offsetNanoseconds);
    }

    @TruffleBoundary
    public static JSTemporalPlainDateTimeObject builtinTimeZoneGetPlainDateTimeFor(JSContext ctx, JSRealm realm,
                    JSTemporalInstantObject instant, TruffleString calendar, long precalculatedOffsetNanoseconds) {
        long offsetNanoseconds = precalculatedOffsetNanoseconds;
        // checked in GetOffsetNanosecondsFor
        assert Math.abs(offsetNanoseconds) < NS_PER_DAY : offsetNanoseconds;
        JSTemporalDateTimeRecord result = getISOPartsFromEpoch(instant.getNanoseconds());
        JSTemporalDateTimeRecord result2 = balanceISODateTime(result.getYear(), result.getMonth(),
                        result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                        result.getMicrosecond(), result.getNanosecond() + offsetNanoseconds);
        return JSTemporalPlainDateTime.create(ctx, realm, result2.getYear(), result2.getMonth(), result2.getDay(), result2.getHour(), result2.getMinute(), result2.getSecond(),
                        result2.getMillisecond(), result2.getMicrosecond(), result2.getNanosecond(), calendar);
    }

    public static JSTemporalDateTimeRecord balanceISODateTime(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond, long nanosecond) {
        TimeRecord bt = balanceTime(hour, minute, second, millisecond, microsecond, nanosecond);
        ISODateRecord bd = balanceISODate(year, month, day + (int) bt.days());
        return JSTemporalDateTimeRecord.create(bd.year(), bd.month(), bd.day(),
                        bt.hour(), bt.minute(), bt.second(), bt.millisecond(), bt.microsecond(), bt.nanosecond());
    }

    public static JSTemporalDateTimeRecord getISODateTimeFor(TruffleString timeZone, BigInt epochNs) {
        long offsetNanoseconds = getOffsetNanosecondsFor(timeZone, epochNs);
        JSTemporalDateTimeRecord result = getISOPartsFromEpoch(epochNs);
        return balanceISODateTime(result.getYear(), result.getMonth(), result.getDay(),
                        result.getHour(), result.getMinute(), result.getSecond(),
                        result.getMillisecond(), result.getMicrosecond(), result.getNanosecond() + offsetNanoseconds);
    }

    @TruffleBoundary
    public static JSTemporalDateTimeRecord getISOPartsFromEpoch(BigInt epochNanoseconds) {
        long remainderNs;
        long epochMilliseconds;
        if (epochNanoseconds.fitsInLong()) {
            remainderNs = epochNanoseconds.longValue() % 1_000_000;
            epochMilliseconds = (epochNanoseconds.longValue() - remainderNs) / 1_000_000;
        } else {
            BigInt[] result = epochNanoseconds.divideAndRemainder(BI_NS_PER_MS);
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
    public static long getOffsetNanosecondsFor(TruffleString timeZone, BigInt epochNs) {
        JSTemporalParserRecord rec = new TemporalParser(timeZone).parseTimeZoneIdentifier();
        if (rec.getTimeZoneIANAName() == null) {
            return parseTimeZoneOffsetNs(rec);
        } else {
            return getIANATimeZoneOffsetNanoseconds(epochNs, rec.getTimeZoneIANAName());
        }
    }

    public static JSTemporalInstantObject systemInstant(JSContext ctx, JSRealm realm) {
        BigInt ns = systemUTCEpochNanoseconds(realm);
        return JSTemporalInstant.create(ctx, realm, ns);
    }

    @TruffleBoundary
    public static BigInt systemUTCEpochNanoseconds(JSRealm realm) {
        BigInt ns = BigInt.valueOf(realm.nanoTimeWallClock());
        // clamping omitted (see Note 2 in spec)
        assert ns.compareTo(upperEpochNSLimit) <= 0 && ns.compareTo(lowerEpochNSLimit) >= 0;
        return ns;
    }

    public static TruffleString systemTimeZoneIdentifier(JSRealm realm) {
        return Strings.fromJavaString(realm.getLocalTimeZoneId().getId());
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
        BigInteger res = epochNanoseconds.bigIntegerValue().add(JSRuntime.toBigInteger(nanoseconds));
        res = res.add(JSRuntime.toBigInteger(microseconds).multiply(BI_1000.bigIntegerValue()));
        res = res.add(JSRuntime.toBigInteger(milliseconds).multiply(BI_NS_PER_MS.bigIntegerValue()));
        res = res.add(JSRuntime.toBigInteger(seconds).multiply(BI_NS_PER_SECOND.bigIntegerValue()));
        res = res.add(JSRuntime.toBigInteger(minutes).multiply(BI_NS_PER_MINUTE.bigIntegerValue()));
        res = res.add(JSRuntime.toBigInteger(hours).multiply(BI_NS_PER_HOUR.bigIntegerValue()));
        BigInt result = new BigInt(res);
        if (!isValidEpochNanoseconds(result)) {
            throw TemporalErrors.createRangeErrorInvalidNanoseconds();
        }
        return result;
    }

    @TruffleBoundary
    public static BigInt addInstant(BigInt epochNanoseconds, BigInt normalizedTimeDuration) {
        BigInt result = addNormalizedTimeDurationToEpochNanoseconds(normalizedTimeDuration, epochNanoseconds);
        if (!isValidEpochNanoseconds(result)) {
            throw TemporalErrors.createRangeErrorInvalidNanoseconds();
        }
        return result; // spec return type: BigInt
    }

    /**
     * DifferenceInstant result.
     */
    public record NormalizedTimeDurationWithTotalRecord(BigInt normalizedTimeDuration, double total) {
    }

    @TruffleBoundary
    public static NormalizedTimeDurationWithTotalRecord differenceInstant(BigInt ns1, BigInt ns2, int roundingIncrement, Unit smallestUnit, RoundingMode roundingMode) {
        BigInt difference = normalizedTimeDurationFromEpochNanosecondsDifference(ns2, ns1);
        var roundRecord = roundTimeDuration(0, difference, roundingIncrement, smallestUnit, roundingMode);
        return new NormalizedTimeDurationWithTotalRecord(roundRecord.normalizedDuration().normalizedTimeTotalNanoseconds(), roundRecord.total());
    }

    @TruffleBoundary
    public static TruffleString temporalInstantToString(JSTemporalInstantObject instant, Object timeZone, Object precision) {
        TruffleString outputTimeZone;
        if (timeZone == Undefined.instance) {
            outputTimeZone = UTC;
        } else {
            outputTimeZone = (TruffleString) timeZone;
        }
        BigInt epochNs = instant.getNanoseconds();
        JSTemporalDateTimeRecord isoDateTime = getISODateTimeFor(outputTimeZone, epochNs);
        TruffleString dateTimeString = JSTemporalPlainDateTime.temporalDateTimeToString(isoDateTime.getYear(), isoDateTime.getMonth(), isoDateTime.getDay(),
                        isoDateTime.getHour(), isoDateTime.getMinute(), isoDateTime.getSecond(), isoDateTime.getMillisecond(), isoDateTime.getMicrosecond(), isoDateTime.getNanosecond(),
                        Undefined.instance, precision, ShowCalendar.NEVER);
        TruffleString timeZoneString;
        if (timeZone == Undefined.instance) {
            timeZoneString = Strings.UC_Z;
        } else {
            long offsetNanoseconds = getOffsetNanosecondsFor(outputTimeZone, epochNs);
            timeZoneString = formatISOTimeZoneOffsetString(offsetNanoseconds);
        }
        return Strings.concat(dateTimeString, timeZoneString);
    }

    @TruffleBoundary
    public static TruffleString builtinTimeZoneGetOffsetStringFor(TruffleString timeZone, JSTemporalInstantObject instant) {
        long offsetNanoseconds = getOffsetNanosecondsFor(timeZone, instant.getNanoseconds());
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
    public static JSTemporalParserRecord parseTimeZoneOffsetStringHelper(TruffleString string) {
        return new TemporalParser(string).parseTimeZoneNumericUTCOffset();
    }

    @TruffleBoundary
    public static long parseTimeZoneOffsetString(TruffleString string) {
        JSTemporalParserRecord rec = parseTimeZoneOffsetStringHelper(string);
        if (rec == null) {
            throw Errors.createRangeError("TemporalTimeZoneNumericUTCOffset expected");
        }
        return parseTimeZoneOffsetNs(rec);
    }

    @TruffleBoundary
    public static long parseTimeZoneOffsetNs(JSTemporalParserRecord rec) {
        long nanoseconds;
        if (rec.getOffsetFraction() == null) {
            nanoseconds = 0;
        } else {
            TruffleString fraction = Strings.concat(rec.getOffsetFraction(), TemporalConstants.ZEROS);
            fraction = Strings.lazySubstring(fraction, 0, 9);
            try {
                nanoseconds = Strings.parseLong(fraction, 10);
            } catch (TruffleString.NumberFormatException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }

        TruffleString signS = rec.getOffsetSign();
        int sign = Strings.SYMBOL_MINUS.equals(signS) ? -1 : 1;

        long hours = rec.hasOffsetHour() ? rec.getOffsetHour() : 0;
        long minutes = rec.hasOffsetMinute() ? rec.getOffsetMinute() : 0;
        long seconds = rec.hasOffsetSecond() ? rec.getOffsetSecond() : 0;

        return sign * (((hours * 60 + minutes) * 60 + seconds) * 1_000_000_000L + nanoseconds);
    }

    @TruffleBoundary
    public static JSTemporalParserRecord parseTemporalTimeZoneIdentifier(TruffleString string) {
        return new TemporalParser(string).parseTimeZoneIdentifier();
    }

    @TruffleBoundary
    public static JSTemporalTimeZoneRecord parseTemporalTimeZoneString(TruffleString string) {
        TemporalParser parser = new TemporalParser(string);
        JSTemporalParserRecord rec = parser.parseTimeZoneIdentifier();
        if (rec == null) {
            // TemporalDateTimeString[+Zoned]
            rec = parser.parseAnnotatedDateTime(true, false);
            if (rec == null) {
                // TemporalDateTimeString[~Zoned]
                rec = parser.parseAnnotatedDateTime(false, false);
            }
            if (rec == null) {
                rec = parser.parseTemporalInstantString();
            }
            if (rec == null) {
                rec = parser.parseTemporalTimeString();
            }
            if (rec == null) {
                rec = parser.parseTemporalMonthDayString();
            }
            if (rec == null) {
                rec = parser.parseYearMonth();
            }
            if (rec != null) {
                if (rec.getTimeZoneAnnotation() != null) {
                    rec = new TemporalParser(rec.getTimeZoneAnnotation()).parseTimeZoneIdentifier();
                } else if (rec.getZ()) {
                    return JSTemporalTimeZoneRecord.create(true, null, TemporalConstants.UTC);
                } else if (rec.getTimeZoneNumericUTCOffset() != null) {
                    rec = (new TemporalParser(rec.getTimeZoneNumericUTCOffset())).parseTimeZoneIdentifier();
                } else {
                    rec = null;
                }
            }
        }
        if (rec == null) {
            throw Errors.createRangeError("TemporalTimeZoneString expected");
        }
        TruffleString name = rec.getTimeZoneIANAName();
        TruffleString offsetString = rec.getTimeZoneNumericUTCOffset();
        return JSTemporalTimeZoneRecord.create(false, offsetString, name);
    }

    public static Disambiguation toTemporalDisambiguation(Object options, TemporalGetOptionNode getOptionNode, TruffleString.EqualNode equalNode) {
        if (options == Undefined.instance) {
            return Disambiguation.COMPATIBLE;
        }
        return toDisambiguation((TruffleString) getOptionNode.execute(options, DISAMBIGUATION, OptionType.STRING, listDisambiguation, COMPATIBLE), equalNode);
    }

    public static OffsetOption toTemporalOffset(Object options, TruffleString fallback, TemporalGetOptionNode getOptionNode, TruffleString.EqualNode equalNode) {
        TruffleString result = fallback;
        if (options != Undefined.instance) {
            result = (TruffleString) getOptionNode.execute(options, OFFSET, OptionType.STRING, listOffsets, fallback);
        }
        return toOffsetOption(result, equalNode);
    }

    public static TruffleString toShowTimeZoneNameOption(JSDynamicObject options, TemporalGetOptionNode getOptionNode) {
        return (TruffleString) getOptionNode.execute(options, TIME_ZONE_NAME, OptionType.STRING, listAutoNeverCritical, AUTO);
    }

    public static TruffleString toShowOffsetOption(JSDynamicObject options, TemporalGetOptionNode getOptionNode) {
        return (TruffleString) getOptionNode.execute(options, OFFSET, OptionType.STRING, listAutoNever, AUTO);
    }

    public static TruffleString temporalZonedDateTimeToString(JSContext ctx, JSRealm realm, JSDynamicObject zonedDateTime, Object precision, ShowCalendar showCalendar, TruffleString showTimeZone,
                    TruffleString showOffset) {
        return temporalZonedDateTimeToString(ctx, realm, zonedDateTime, precision, showCalendar, showTimeZone, showOffset, null, Unit.EMPTY, RoundingMode.EMPTY);
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
            if (rec.getYear() == 0 && Strings.indexOf(string, TemporalConstants.MINUS_000000) >= 0) {
                throw TemporalErrors.createRangeErrorInvalidPlainDateTime();
            }
            if (rec.getCalendar() != null && rec.getDay() == Long.MIN_VALUE && !"iso8601".equalsIgnoreCase(rec.getCalendar().toJavaStringUncached())) {
                throw TemporalErrors.createRangeErrorTemporalISO8601Expected();
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
                    TruffleString showOffset, Integer incrementParam, Unit unitParam, RoundingMode roundingModeParam) {
        assert isTemporalZonedDateTime(zonedDateTimeParam);
        assert unitParam != null && roundingModeParam != null;
        JSTemporalZonedDateTimeObject zonedDateTime = (JSTemporalZonedDateTimeObject) zonedDateTimeParam;
        int increment = incrementParam == null ? 1 : incrementParam;
        Unit unit = unitParam == Unit.EMPTY ? Unit.NANOSECOND : unitParam;
        RoundingMode roundingMode = roundingModeParam == RoundingMode.EMPTY ? RoundingMode.TRUNC : roundingModeParam;

        BigInt ns = roundTemporalInstant(zonedDateTime.getNanoseconds(), increment, unit, roundingMode);
        TruffleString timeZone = zonedDateTime.getTimeZone();
        JSTemporalInstantObject instant = JSTemporalInstant.create(ctx, realm, ns);
        long offsetNanoseconds = getOffsetNanosecondsFor(timeZone, instant.getNanoseconds());
        JSTemporalPlainDateTimeObject temporalDateTime = builtinTimeZoneGetPlainDateTimeFor(ctx, realm, instant, ISO8601, offsetNanoseconds);
        TruffleString dateTimeString = JSTemporalPlainDateTime.temporalDateTimeToString(temporalDateTime.getYear(), temporalDateTime.getMonth(), temporalDateTime.getDay(),
                        temporalDateTime.getHour(), temporalDateTime.getMinute(), temporalDateTime.getSecond(), temporalDateTime.getMillisecond(),
                        temporalDateTime.getMicrosecond(), temporalDateTime.getNanosecond(), ISO8601, precision, ShowCalendar.NEVER);
        TruffleString offsetString;
        TruffleString timeZoneString;
        if (NEVER.equals(showOffset)) {
            offsetString = Strings.EMPTY_STRING;
        } else {
            offsetString = formatISOTimeZoneOffsetString(offsetNanoseconds);
        }
        if (NEVER.equals(showTimeZone)) {
            timeZoneString = Strings.EMPTY_STRING;
        } else {
            TruffleString timeZoneID = ToTemporalTimeZoneIdentifierNode.getUncached().execute(timeZone);
            if (CRITICAL.equals(showTimeZone)) {
                timeZoneID = Strings.concat(Strings.EXCLAMATION_MARK, timeZoneID);
            }
            timeZoneString = Strings.addBrackets(timeZoneID);
        }
        TruffleString calendarString = maybeFormatCalendarAnnotation(zonedDateTime.getCalendar(), showCalendar);
        return Strings.concatAll(dateTimeString, offsetString, timeZoneString, calendarString);
    }

    @TruffleBoundary
    public static TruffleString formatISOTimeZoneOffsetString(long offsetNs) {
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
    public static BigInt parseTemporalInstant(TruffleString string) {
        ParseISODateTimeResult result = parseTemporalInstantString(string);
        JSTemporalTimeZoneRecord timeZoneRec = result.getTimeZoneResult();
        assert timeZoneRec.isZ() != (timeZoneRec.getOffsetString() != null);
        long offsetNanoseconds;
        if (timeZoneRec.isZ()) {
            offsetNanoseconds = 0;
        } else {
            offsetNanoseconds = parseTimeZoneOffsetString(timeZoneRec.getOffsetString());
        }
        BigInt utc = getUTCEpochNanoseconds(result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(),
                        result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
        BigInt instant = utc.subtract(BigInt.valueOf(offsetNanoseconds));
        if (!isValidEpochNanoseconds(instant)) {
            throw TemporalErrors.createRangeErrorInvalidNanoseconds();
        }
        return instant;
    }

    @TruffleBoundary
    private static ParseISODateTimeResult parseTemporalInstantString(TruffleString string) {
        try {
            JSTemporalParserRecord rec = (new TemporalParser(string)).parseTemporalInstantString();
            ParseISODateTimeResult result = parseISODateTimeIntl(string, rec);
            return result;
        } catch (Exception ex) {
            throw Errors.createRangeError("Instant cannot be parsed");
        }
    }

    @TruffleBoundary
    public static BigInt getEpochNanosecondsFor(JSContext ctx, JSRealm realm, TruffleString timeZone, JSTemporalPlainDateTimeObject isoDateTime,
                    Disambiguation disambiguation) {
        List<BigInt> possibleEpochNs = getPossibleEpochNanoseconds(timeZone, isoDateTime);
        return disambiguatePossibleEpochNanoseconds(ctx, realm, possibleEpochNs, timeZone, isoDateTime, disambiguation);
    }

    @TruffleBoundary
    public static BigInt disambiguatePossibleEpochNanoseconds(JSContext ctx, JSRealm realm, List<BigInt> possibleEpochNs,
                    TruffleString timeZone, JSTemporalPlainDateTimeObject dateTime, Disambiguation disambiguation) {
        int n = possibleEpochNs.size();
        if (n == 1) {
            return possibleEpochNs.get(0);
        } else if (n != 0) {
            if (Disambiguation.EARLIER == disambiguation || Disambiguation.COMPATIBLE == disambiguation) {
                return possibleEpochNs.get(0);
            } else if (Disambiguation.LATER == disambiguation) {
                return possibleEpochNs.get(n - 1);
            }
            assert Disambiguation.REJECT == disambiguation;
            throw Errors.createRangeError("invalid disambiguation");
        }
        assert n == 0;
        if (Disambiguation.REJECT == disambiguation) {
            throw Errors.createRangeError("disambiguation failed");
        }
        BigInt epochNanoseconds = getUTCEpochNanoseconds(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
                        dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond());
        BigInt dayBeforeNs = epochNanoseconds.subtract(BI_NS_PER_DAY);
        BigInt dayAfterNs = epochNanoseconds.add(BI_NS_PER_DAY);
        if (!TemporalUtil.isValidEpochNanoseconds(dayBeforeNs) || !TemporalUtil.isValidEpochNanoseconds(dayAfterNs)) {
            throw TemporalErrors.createRangeErrorInvalidNanoseconds();
        }
        long offsetBefore = getOffsetNanosecondsFor(timeZone, dayBeforeNs);
        long offsetAfter = getOffsetNanosecondsFor(timeZone, dayAfterNs);
        long nanoseconds = offsetAfter - offsetBefore;
        if (Disambiguation.EARLIER == disambiguation) {
            TimeRecord earlierTime = addTimeDouble(
                            dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(),
                            0, 0, 0, 0, 0, -nanoseconds);
            ISODateRecord earlierDate = addISODate(
                            dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(),
                            0, 0, 0, dtoi(earlierTime.days()), Overflow.CONSTRAIN);
            JSTemporalPlainDateTimeObject earlierDateTime = JSTemporalPlainDateTime.create(ctx, realm,
                            earlierDate.year(), earlierDate.month(), earlierDate.day(),
                            earlierTime.hour(), earlierTime.minute(), earlierTime.second(), earlierTime.millisecond(), earlierTime.microsecond(), earlierTime.nanosecond(),
                            ISO8601, null, InlinedBranchProfile.getUncached());
            List<BigInt> possibleEpochNs2 = getPossibleEpochNanoseconds(timeZone, earlierDateTime);
            if (possibleEpochNs2.isEmpty()) {
                throw Errors.createRangeError("nothing found");
            }
            return possibleEpochNs2.get(0);
        }
        assert Disambiguation.LATER == disambiguation || Disambiguation.COMPATIBLE == disambiguation;
        TimeRecord laterTime = addTimeDouble(
                        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond(),
                        0, 0, 0, 0, 0, nanoseconds);
        ISODateRecord laterDate = addISODate(
                        dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(),
                        0, 0, 0, dtoi(laterTime.days()), Overflow.CONSTRAIN);
        JSTemporalPlainDateTimeObject laterDateTime = JSTemporalPlainDateTime.create(ctx, realm,
                        laterDate.year(), laterDate.month(), laterDate.day(),
                        laterTime.hour(), laterTime.minute(), laterTime.second(), laterTime.millisecond(), laterTime.microsecond(), laterTime.nanosecond(),
                        ISO8601, null, InlinedBranchProfile.getUncached());

        List<BigInt> possibleEpochNs2 = getPossibleEpochNanoseconds(timeZone, laterDateTime);
        n = possibleEpochNs2.size();
        if (n == 0) {
            throw Errors.createRangeError("nothing found");
        }
        return possibleEpochNs2.get(n - 1);
    }

    @TruffleBoundary
    public static BigInt interpretISODateTimeOffset(JSContext ctx, JSRealm realm, int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond,
                    int nanosecond, OffsetBehaviour offsetBehaviour, long offsetNanoseconds, TruffleString timeZone, Disambiguation disambiguation, OffsetOption offsetOption,
                    MatchBehaviour matchBehaviour) {
        JSTemporalPlainDateTimeObject dateTime = JSTemporalPlainDateTime.create(ctx, realm, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, ISO8601);
        if (offsetBehaviour == OffsetBehaviour.WALL || OffsetOption.IGNORE == offsetOption) {
            return getEpochNanosecondsFor(ctx, realm, timeZone, dateTime, disambiguation);
        }
        if (offsetBehaviour == OffsetBehaviour.EXACT || OffsetOption.USE == offsetOption) {
            var balanced = balanceISODateTime(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond - offsetNanoseconds);
            checkISODaysRange(balanced.getYear(), balanced.getMonth(), balanced.getDay());
            BigInt epochNanoseconds = getUTCEpochNanoseconds(balanced.getYear(), balanced.getMonth(), balanced.getDay(), balanced.getHour(), balanced.getMinute(), balanced.getSecond(),
                            balanced.getMillisecond(), balanced.getMicrosecond(), balanced.getNanosecond());
            if (!TemporalUtil.isValidEpochNanoseconds(epochNanoseconds)) {
                throw TemporalErrors.createRangeErrorInvalidNanoseconds();
            }
            return epochNanoseconds;
        }
        assert offsetBehaviour == OffsetBehaviour.OPTION;
        assert OffsetOption.PREFER == offsetOption || OffsetOption.REJECT == offsetOption;
        checkISODaysRange(year, month, day);
        BigInt utcEpochNanoseconds = getUTCEpochNanoseconds(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
        List<BigInt> possibleEpochNanoseconds = getPossibleEpochNanoseconds(timeZone, dateTime);
        for (BigInt candidate : possibleEpochNanoseconds) {
            long candidateOffset = utcEpochNanoseconds.subtract(candidate).longValueExact();
            if (candidateOffset == offsetNanoseconds) {
                return candidate;
            }
            if (matchBehaviour == MatchBehaviour.MATCH_MINUTES) {
                long roundedCandidateNanoseconds = dtol(roundNumberToIncrement(candidateOffset, 60_000_000_000L, RoundingMode.HALF_EXPAND));
                if (roundedCandidateNanoseconds == offsetNanoseconds) {
                    return candidate;
                }
            }
        }
        if (OffsetOption.REJECT == offsetOption) {
            throw Errors.createRangeError("cannot interpret DateTime offset");
        }
        return disambiguatePossibleEpochNanoseconds(ctx, realm, possibleEpochNanoseconds, timeZone, dateTime, disambiguation);
    }

    public static boolean timeZoneEquals(Object one, Object two, ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifier) {
        if (one == two) {
            return true;
        }
        TruffleString timeZoneOne = toTimeZoneIdentifier.execute(one);
        TruffleString timeZoneTwo = toTimeZoneIdentifier.execute(two);
        if (Boundaries.equals(timeZoneOne, timeZoneTwo)) {
            return true;
        } else {
            boolean numOffsetOne = canParseAsTimeZoneNumericUTCOffset(timeZoneOne);
            boolean numOffsetTwo = canParseAsTimeZoneNumericUTCOffset(timeZoneTwo);
            if (numOffsetOne) {
                if (numOffsetTwo) {
                    return parseTimeZoneOffsetString(timeZoneOne) == parseTimeZoneOffsetString(timeZoneTwo);
                } else {
                    return false;
                }
            } else {
                if (numOffsetTwo) {
                    return false;
                } else {
                    TruffleString primaryOne = getAvailableNamedTimeZoneIdentifier(timeZoneOne).getSecond();
                    TruffleString primaryTwo = getAvailableNamedTimeZoneIdentifier(timeZoneTwo).getSecond();
                    return Boundaries.equals(primaryOne, primaryTwo);
                }
            }
        }
    }

    public static TruffleString consolidateCalendars(TruffleString one, TruffleString two, ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
        if (one == two) {
            return two;
        }
        TruffleString s1 = toCalendarIdentifier.executeString(one);
        TruffleString s2 = toCalendarIdentifier.executeString(two);
        return consolidateCalendarsIntl(one, two, s1, s2);
    }

    @TruffleBoundary
    private static TruffleString consolidateCalendarsIntl(TruffleString one, TruffleString two, TruffleString s1, TruffleString s2) {
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

    private static List<BigInt> getPossibleEpochNanoseconds(TruffleString timeZone, JSTemporalPlainDateTimeObject isoDateTime) {
        JSTemporalParserRecord rec = new TemporalParser(timeZone).parseTimeZoneIdentifier();
        List<BigInt> possibleEpochNanoseconds;
        if (rec.getTimeZoneIANAName() == null) {
            int offsetMinutes = (int) (parseTimeZoneOffsetNs(rec) / 60_000_000_000L);
            JSTemporalDateTimeRecord balanced = balanceISODateTime(isoDateTime.getYear(), isoDateTime.getMonth(), isoDateTime.getDay(), isoDateTime.getHour(), isoDateTime.getMinute() - offsetMinutes,
                            isoDateTime.getSecond(), isoDateTime.getMillisecond(), isoDateTime.getMicrosecond(), isoDateTime.getNanosecond());
            checkISODaysRange(balanced.getYear(), balanced.getMonth(), balanced.getDay());
            BigInt epochNanoseconds = TemporalUtil.getUTCEpochNanoseconds(balanced.getYear(), balanced.getMonth(), balanced.getDay(), balanced.getHour(), balanced.getMinute(),
                            balanced.getSecond(), balanced.getMillisecond(), balanced.getMicrosecond(), balanced.getNanosecond());
            possibleEpochNanoseconds = List.of(epochNanoseconds);
        } else {
            checkISODaysRange(isoDateTime.getYear(), isoDateTime.getMonth(), isoDateTime.getDay());
            possibleEpochNanoseconds = TemporalUtil.getNamedTimeZoneEpochNanoseconds(timeZone, isoDateTime.getYear(), isoDateTime.getMonth(), isoDateTime.getDay(),
                            isoDateTime.getHour(), isoDateTime.getMinute(), isoDateTime.getSecond(), isoDateTime.getMillisecond(), isoDateTime.getMicrosecond(), isoDateTime.getNanosecond());
        }
        for (BigInt epochNanoseconds : possibleEpochNanoseconds) {
            if (!TemporalUtil.isValidEpochNanoseconds(epochNanoseconds)) {
                throw TemporalErrors.createRangeErrorInvalidNanoseconds();
            }
        }
        return possibleEpochNanoseconds;
    }

    public static void checkISODaysRange(int year, int month, int date) {
        if (Math.abs(JSDate.isoDateToEpochDays(year, month - 1, date)) > INSTANT_MAX_OFFSET_EPOCH_DAYS) {
            throw TemporalErrors.createRangeErrorDateOutsideRange();
        }
    }

    @TruffleBoundary
    public static List<BigInt> getNamedTimeZoneEpochNanoseconds(TruffleString identifier, long isoYear, long isoMonth, long isoDay, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds,
                    long nanoseconds) {
        List<BigInt> list = new ArrayList<>();
        try {
            ZoneId zoneId = ZoneId.of(Strings.toJavaString(identifier));
            long fractions = milliseconds * 1_000_000L + microseconds * 1_000L + nanoseconds;
            LocalDateTime localTime = LocalDateTime.of((int) isoYear, (int) isoMonth, (int) isoDay, (int) hours, (int) minutes, (int) seconds, (int) fractions);
            List<ZoneOffset> offsets = zoneId.getRules().getValidOffsets(localTime);
            for (ZoneOffset offset : offsets) {
                list.add(BigInt.valueOf(localTime.atOffset(offset).toEpochSecond()).multiply(BI_NS_PER_SECOND).add(BigInt.valueOf(fractions)));
            }
        } catch (Exception ex) {
            assert false;
        }
        return list;
    }

    @TruffleBoundary
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
    public static BigInt getIANATimeZoneNextTransition(TruffleString timeZoneIdentifier, BigInt epochNanoseconds) {
        BigInt[] sec = epochNanoseconds.divideAndRemainder(BI_NS_PER_SECOND);
        Instant instant = Instant.ofEpochSecond(sec[0].longValue(), sec[1].longValue());
        ZoneId zoneId = ZoneId.of(Strings.toJavaString(timeZoneIdentifier));
        ZoneRules zoneRule = zoneId.getRules();
        ZoneOffsetTransition nextTransition = zoneRule.nextTransition(instant);
        if (nextTransition == null) {
            return null;
        }
        return BigInt.valueOf(nextTransition.toEpochSecond()).multiply(BI_NS_PER_SECOND);
    }

    @TruffleBoundary
    public static BigInt getIANATimeZonePreviousTransition(TruffleString timeZoneIdentifier, BigInt epochNanoseconds) {
        BigInt[] sec = epochNanoseconds.divideAndRemainder(BI_NS_PER_SECOND);
        Instant instant = Instant.ofEpochSecond(sec[0].longValue(), sec[1].longValue());
        ZoneId zoneId = ZoneId.of(Strings.toJavaString(timeZoneIdentifier));
        ZoneRules zoneRule = zoneId.getRules();
        ZoneOffsetTransition previousTransition = zoneRule.previousTransition(instant);
        if (previousTransition == null) {
            return null;
        }
        return BigInt.valueOf(previousTransition.toEpochSecond()).multiply(BI_NS_PER_SECOND);
    }

    @TruffleBoundary
    public static boolean canParseAsTimeZoneNumericUTCOffset(TruffleString string) {
        try {
            return new TemporalParser(string).parseTimeZoneNumericUTCOffset() != null;
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

    public enum FieldsType {
        DATE,
        YEAR_MONTH,
        MONTH_DAY
    }

    @TruffleBoundary
    public static JSObject isoDateToFields(JSContext ctx, TruffleString calendar, ISODateRecord isoDate, FieldsType type) {
        JSObject fields = JSOrdinary.createWithNullPrototype(ctx);
        JSObject calendarDate = calendarISOToDate(ctx, calendar, isoDate);
        JSObject.set(fields, MONTH_CODE, JSObject.get(calendarDate, MONTH_CODE));
        if (type != FieldsType.YEAR_MONTH) {
            JSObject.set(fields, DAY, JSObject.get(calendarDate, DAY));
        }
        if (type != FieldsType.MONTH_DAY) {
            JSObject.set(fields, YEAR, JSObject.get(calendarDate, YEAR));
        }
        return fields;
    }

    @TruffleBoundary
    public static JSObject calendarISOToDate(JSContext ctx, TruffleString calendar, ISODateRecord isoDate) {
        JSObject record = JSOrdinary.createWithNullPrototype(ctx);
        if (ISO8601.equals(calendar)) {
            JSObject.set(record, YEAR, isoDate.year());
            JSObject.set(record, MONTH, isoDate.month());
            JSObject.set(record, DAY, isoDate.day());
            JSObject.set(record, MONTH_CODE, TemporalUtil.buildISOMonthCode(isoDate.month()));
        } else {
            Calendar cal = IntlUtil.getCalendar(calendar);
            cal.setTimeInMillis(JSDate.MS_PER_DAY * JSDate.isoDateToEpochDays(isoDate.year(), isoDate.month() - 1, isoDate.day()));
            JSObject.set(record, YEAR, cal.get(Calendar.EXTENDED_YEAR));
            if (IntlUtil.calendarSupportsEra(cal)) {
                JSObject.set(record, ERA, IntlUtil.getEra(cal));
                JSObject.set(record, ERA_YEAR, IntlUtil.getEraYear(cal));
            }
            JSObject.set(record, MONTH, cal.get(Calendar.ORDINAL_MONTH) + 1);
            JSObject.set(record, DAY, cal.get(Calendar.DAY_OF_MONTH));
            JSObject.set(record, MONTH_CODE, Strings.fromJavaString(cal.getTemporalMonthCode()));
        }
        return record;
    }

    // 12.1.38
    @TruffleBoundary
    public static void calendarResolveFields(JSContext ctx, TruffleString calendar, JSDynamicObject fields, FieldsType type, JSToIntegerOrInfinityNode toIntegerOrInfinity) {
        boolean hasLeapYears;
        int monthCount;
        boolean supportsEras;
        boolean seenYearInfo = false;
        boolean isoCalendar = ISO8601.equals(calendar);
        if (isoCalendar) {
            hasLeapYears = false;
            monthCount = 12;
            supportsEras = false;
        } else {
            Calendar cal = IntlUtil.getCalendar(calendar);
            hasLeapYears = IntlUtil.hasLeapYears(cal);
            monthCount = cal.getMaximum(Calendar.ORDINAL_MONTH) + 1;
            supportsEras = IntlUtil.calendarSupportsEra(cal);

            if (supportsEras) {
                Object era = JSObject.get(fields, ERA);
                Object eraYear = JSObject.get(fields, ERA_YEAR);
                boolean eraSet = (era != Undefined.instance);
                boolean eraYearSet = (eraYear != Undefined.instance);

                if (eraSet != eraYearSet) {
                    throw Errors.createTypeError("Both era and eraYear should be set (or none of them).");
                } else {
                    Object year = JSObject.get(fields, YEAR);
                    if (eraSet) { // both era and eraYear set
                        Integer canonicalEra = IntlUtil.canonicalizeEraInCalendar(cal, (TruffleString) era);
                        if (canonicalEra == null) {
                            throw Errors.createTypeError("Invalid era");
                        }
                        if (year != Undefined.instance) {
                            // year should be consistent with era and eraYear
                            cal.setTimeInMillis(0);
                            cal.set(Calendar.ERA, canonicalEra);
                            cal.set(Calendar.YEAR, ((Number) eraYear).intValue());
                            long eraYearMS = cal.getTimeInMillis();

                            cal.setTimeInMillis(0);
                            cal.set(Calendar.EXTENDED_YEAR, ((Number) year).intValue());
                            long yearMS = cal.getTimeInMillis();

                            if (yearMS != eraYearMS) {
                                throw Errors.createRangeError("Year is not consistent with era and eraYear.");
                            }
                        }
                        seenYearInfo = true;
                    } else { // both era and eraYear unset
                        if (year == Undefined.instance) {
                            if (type != FieldsType.MONTH_DAY) {
                                throw Errors.createTypeError("Neither year nor era and eraYear set.");
                            }
                        } else {
                            seenYearInfo = true;
                        }
                    }
                }
            }
        }
        if (!seenYearInfo) {
            Object year = JSObject.get(fields, YEAR);
            seenYearInfo = (year != Undefined.instance);
        }
        if (type != FieldsType.MONTH_DAY && !seenYearInfo) {
            throw Errors.createTypeError("No year present.");
        }
        if (type != FieldsType.YEAR_MONTH) {
            Object day = JSObject.get(fields, DAY);
            if (day == Undefined.instance) {
                throw Errors.createTypeError("No day present.");
            }
        }
        Object month = JSObject.get(fields, MONTH);
        Object monthCode = JSObject.get(fields, MONTH_CODE);
        if (monthCode == Undefined.instance) {
            if (!isoCalendar && type == FieldsType.MONTH_DAY && !seenYearInfo) {
                throw Errors.createTypeError("No year or month code present.");
            }
            if (month == Undefined.instance) {
                throw Errors.createTypeError("No month or month code present.");
            }
            return;
        }
        int monthLength = Strings.length((TruffleString) monthCode);
        if (monthLength < 3 || 4 < monthLength || (monthLength == 4 && !hasLeapYears)) {
            throw Errors.createRangeError("Invalid month code length");
        }
        if (Strings.charAt((TruffleString) monthCode, 0) != 'M') {
            throw Errors.createRangeError("Month code should start with 'M'");
        }
        if (monthLength == 4 && Strings.charAt((TruffleString) monthCode, 3) != 'L') {
            throw Errors.createRangeError("Code of a leap month should end with 'L'");
        }
        TruffleString monthCodeDigits = Strings.substring(ctx, (TruffleString) monthCode, 1, 2);
        double monthCodeInteger = JSRuntime.doubleValue(toIntegerOrInfinity.executeNumber(monthCodeDigits));

        if (Double.isNaN(monthCodeInteger) || monthCodeInteger < 1 || monthCount < monthCodeInteger) {
            throw Errors.createRangeErrorFormat("Invalid month code: %s", null, monthCode);
        }

        if (month != Undefined.instance && JSRuntime.doubleValue((Number) month) != monthCodeInteger) {
            throw Errors.createRangeError("Month does not equal the month code.");
        }

        createDataPropertyOrThrow(ctx, fields, MONTH, monthCodeInteger);
    }

    @TruffleBoundary
    public static ISODateRecord calendarDateToISO(TruffleString calendar, JSDynamicObject fields, Overflow overflow) {
        Object yearObject = JSObject.get(fields, YEAR);
        int month = ((Number) JSObject.get(fields, MONTH)).intValue();
        int day = ((Number) JSObject.get(fields, DAY)).intValue();
        if (ISO8601.equals(calendar)) {
            int year = ((Number) yearObject).intValue();
            return regulateISODate(year, month, day, overflow);
        } else {
            Calendar cal = IntlUtil.getCalendar(calendar);
            cal.setTimeInMillis(0);
            if (yearObject == Undefined.instance) {
                Object eraObject = JSObject.get(fields, ERA);
                if (eraObject != Undefined.instance) {
                    TruffleString era = (TruffleString) JSObject.get(fields, ERA);
                    cal.set(Calendar.ERA, IntlUtil.canonicalizeEraInCalendar(cal, era));
                    int eraYear = ((Number) JSObject.get(fields, ERA_YEAR)).intValue();
                    cal.set(Calendar.YEAR, eraYear);
                }
            } else {
                int year = ((Number) yearObject).intValue();
                cal.set(Calendar.EXTENDED_YEAR, year);
            }
            Object monthCode = JSObject.get(fields, MONTH_CODE);
            if (monthCode == Undefined.instance) {
                int newValue = month - 1;
                int maxMonth = cal.getActualMaximum(Calendar.ORDINAL_MONTH);
                if (newValue > maxMonth) {
                    if (overflow == Overflow.REJECT) {
                        throw Errors.createRangeError("Invalid month");
                    } else {
                        assert overflow == Overflow.CONSTRAIN;
                        newValue = maxMonth;
                    }
                }
                // Workaround for Hebrew calendar's behaviour for plain
                // cal.set(Calendar.ORDINAL_MONTH, newValue);
                int oldValue = cal.get(Calendar.ORDINAL_MONTH);
                cal.roll(Calendar.ORDINAL_MONTH, newValue - oldValue);
            } else {
                int extendedYear = cal.get(Calendar.EXTENDED_YEAR);
                String monthCodeStr = monthCode.toString();
                cal.setTemporalMonthCode(monthCodeStr);
                if (!monthCodeStr.equals(cal.getTemporalMonthCode())) {
                    // monthCode does not exist in extendedYear
                    if (overflow == Overflow.REJECT) {
                        throw Errors.createRangeError("Invalid monthCode");
                    } else if (monthCodeStr.length() == 4) { // is leap month
                        assert overflow == Overflow.CONSTRAIN;
                        // leap month does not exist in extendedYear
                        // => constrain to non-leap month
                        String nonLeapMonthCode;
                        if (cal instanceof HebrewCalendar && "M05L".equals(monthCodeStr)) {
                            nonLeapMonthCode = "M06";
                        } else {
                            nonLeapMonthCode = monthCodeStr.substring(0, 3);
                        }
                        // the setting of non existing leap month could have
                        // rolled the year to the next one => restore the original value
                        if (extendedYear != cal.get(Calendar.EXTENDED_YEAR)) {
                            cal.set(Calendar.EXTENDED_YEAR, extendedYear);
                        }
                        cal.setTemporalMonthCode(nonLeapMonthCode);
                    }
                }
            }
            int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (maxDay < day) {
                if (overflow == Overflow.REJECT) {
                    throw Errors.createRangeError("Invalid day");
                } else {
                    day = maxDay;
                }
            }
            cal.set(Calendar.DAY_OF_MONTH, day);
            long ms = cal.getTimeInMillis();
            return createISODateRecord(JSDate.yearFromTime(ms), JSDate.monthFromTime(ms) + 1, JSDate.dateFromTime(ms));
        }
    }

    @TruffleBoundary
    public static ISODateRecord isoYearMonthFromFields(JSDynamicObject fields, Overflow overflow) {
        Number year = (Number) JSObject.get(fields, YEAR);
        Number month = (Number) JSObject.get(fields, MONTH);
        ISOYearMonthRecord result = regulateISOYearMonth(dtoi(JSRuntime.doubleValue(year)),
                        dtoi(JSRuntime.doubleValue(month)), overflow);
        return new ISODateRecord(result.year(), result.month(), 1);
    }

    @TruffleBoundary
    public static ISODateRecord calendarMonthDayToISOReferenceDate(TruffleString calendar, JSDynamicObject fields, Overflow overflow) {
        Number month = (Number) JSObject.get(fields, MONTH);
        Number day = (Number) JSObject.get(fields, DAY);
        Object year = JSObject.get(fields, YEAR);
        if (ISO8601.equals(calendar)) {
            int referenceISOYear = 1972;
            int yearForRegulateISODate;
            if (year == Undefined.instance) {
                yearForRegulateISODate = referenceISOYear;
            } else {
                yearForRegulateISODate = dtoi(JSRuntime.doubleValue((Number) year));
            }
            ISODateRecord result = regulateISODate(yearForRegulateISODate, dtoi(JSRuntime.doubleValue(month)),
                            dtoi(JSRuntime.doubleValue(day)), overflow);
            return new ISODateRecord(referenceISOYear, result.month(), result.day());
        } else {
            Calendar cal = IntlUtil.getCalendar(calendar);
            cal.setTimeInMillis(0);

            int m = month.intValue() - 1;
            int mMax = cal.getMaximum(Calendar.ORDINAL_MONTH);
            if (m > mMax) {
                if (overflow == Overflow.CONSTRAIN) {
                    m = mMax;
                } else {
                    throw TemporalErrors.createRangeErrorDateOutsideRange();
                }
            }

            Object monthCodeValue = JSObject.get(fields, MONTH_CODE);
            String monthCode;
            if (monthCodeValue == Undefined.instance) {
                monthCode = String.format(Locale.ROOT, "M%02d", m + 1);
            } else {
                TruffleString monthCodeTS = (TruffleString) monthCodeValue;
                monthCode = Strings.toJavaString(monthCodeTS);
            }
            try {
                cal.setTemporalMonthCode(monthCode);
            } catch (IllegalArgumentException iaex) {
                throw TemporalErrors.createRangeErrorDateOutsideRange();
            }

            int d = day.intValue();
            int dMax = IntlUtil.maxDayInMonth(cal, monthCode);
            if (d > dMax) {
                if (overflow == Overflow.CONSTRAIN) {
                    d = dMax;
                } else {
                    throw TemporalErrors.createRangeErrorDateOutsideRange();
                }
            }

            if (year != Undefined.instance) {
                // check month/restrict day according to the provided year
                cal.set(Calendar.YEAR, ((Number) year).intValue());
                cal.setTemporalMonthCode(monthCode);
                if (monthCode.equals(cal.getTemporalMonthCode())) {
                    dMax = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                    if (d > dMax) {
                        if (overflow == Overflow.CONSTRAIN) {
                            d = dMax;
                        } else {
                            // no day d in this month
                            throw TemporalErrors.createRangeErrorDateOutsideRange();
                        }
                    }
                } else if (overflow == Overflow.REJECT) {
                    // no month with monthCode in the provided year
                    throw TemporalErrors.createRangeErrorDateOutsideRange();
                }
            }

            long msUpperBound = JSDate.isoDateToEpochDays(1972, 11, 31) * JSDate.MS_PER_DAY;
            cal.setTimeInMillis(msUpperBound);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            long ms;
            while (true) {
                cal.setTemporalMonthCode(monthCode);
                if (monthCode.equals(cal.getTemporalMonthCode())) {
                    dMax = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                    if (d <= dMax) {
                        cal.set(Calendar.DAY_OF_MONTH, d);
                        ms = cal.getTimeInMillis();
                        if (ms <= msUpperBound) {
                            break;
                        }
                    }
                }
                cal.set(Calendar.EXTENDED_YEAR, cal.get(Calendar.EXTENDED_YEAR) - 1);
            }
            return new ISODateRecord(JSDate.yearFromTime(ms), JSDate.monthFromTime(ms) + 1, JSDate.dateFromTime(ms));
        }
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

    // always fails if long does not fit into int
    @TruffleBoundary
    public static int ltoi(long l) {
        if (!JSRuntime.longIsRepresentableAsInt(l)) {
            throw Errors.createRangeError("value out of range");
        }
        return (int) l;
    }

    @TruffleBoundary
    public static int bitoi(BigInt bi) {
        double value = bi.doubleValue();
        assert Double.isFinite(value);
        assert JSRuntime.doubleIsRepresentableAsInt(value);
        return bi.intValue();
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

    @TruffleBoundary
    public static RoundingMode toRoundingMode(TruffleString mode, TruffleString.EqualNode equalNode) {
        if (mode == null) {
            return RoundingMode.EMPTY;
        } else if (equalNode.execute(mode, FLOOR, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.FLOOR;
        } else if (equalNode.execute(mode, CEIL, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.CEIL;
        } else if (equalNode.execute(mode, EXPAND, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.EXPAND;
        } else if (equalNode.execute(mode, TRUNC, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.TRUNC;
        } else if (equalNode.execute(mode, HALF_FLOOR, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.HALF_FLOOR;
        } else if (equalNode.execute(mode, HALF_CEIL, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.HALF_CEIL;
        } else if (equalNode.execute(mode, HALF_EXPAND, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.HALF_EXPAND;
        } else if (equalNode.execute(mode, HALF_TRUNC, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.HALF_TRUNC;
        } else if (equalNode.execute(mode, HALF_EVEN, TruffleString.Encoding.UTF_16)) {
            return RoundingMode.HALF_EVEN;
        }
        throw Errors.shouldNotReachHereUnexpectedValue(mode);
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
        throw Errors.shouldNotReachHereUnexpectedValue(disambiguation);
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
        throw Errors.shouldNotReachHereUnexpectedValue(offsetOption);
    }

    public static ShowCalendar toShowCalendar(TruffleString showCalendar, TruffleString.EqualNode equalNode) {
        if (Strings.equals(equalNode, showCalendar, AUTO)) {
            return ShowCalendar.AUTO;
        } else if (Strings.equals(equalNode, showCalendar, NEVER)) {
            return ShowCalendar.NEVER;
        } else if (Strings.equals(equalNode, showCalendar, ALWAYS)) {
            return ShowCalendar.ALWAYS;
        } else if (Strings.equals(equalNode, showCalendar, CRITICAL)) {
            return ShowCalendar.CRITICAL;
        }
        throw Errors.shouldNotReachHereUnexpectedValue(showCalendar);
    }

    public static double roundTowardsZero(double d) {
        return ExactMath.truncate(d);
    }

    public static TruffleString toMonthCode(Object argument) {
        Object primitive = JSRuntime.toPrimitive(argument, JSToPrimitiveNode.Hint.String);
        if (primitive instanceof TruffleString monthCode) {
            int length = Strings.length(monthCode);
            if (length == 3 || length == 4) {
                char c0 = Strings.charAt(monthCode, 0);
                char c1 = Strings.charAt(monthCode, 1);
                char c2 = Strings.charAt(monthCode, 2);
                if (c0 == 'M' && '0' <= c1 && c1 <= '9' && '0' <= c2 && c2 <= '9' //
                                && (length == 4 || c1 != '0' || c2 != '0') //
                                && (length == 3 || Strings.charAt(monthCode, 3) == 'L')) {
                    return monthCode;
                }
            }
            throw Errors.createRangeErrorFormat("Invalid month code: %s", null, monthCode);
        } else {
            throw Errors.createTypeErrorNotAString(primitive);
        }
    }

    @TruffleBoundary
    public static double nanosToMillis(BigInt nanos) {
        BigInteger[] qr = nanos.bigIntegerValue().divideAndRemainder(BI_NS_PER_MS.bigIntegerValue());
        double ms = qr[0].doubleValue();
        if (qr[1].signum() < 0) {
            ms--;
        }
        return ms;
    }

    public static TruffleString toOffsetString(Object argument) {
        Object offset = JSRuntime.toPrimitive(argument, JSToPrimitiveNode.Hint.String);
        if (offset instanceof TruffleString offsetTS) {
            parseTimeZoneOffsetString(offsetTS);
            return offsetTS;
        } else {
            throw Errors.createTypeErrorNotAString(offset);
        }
    }

    public static TruffleString toTemporalTimeZoneIdentifier(Object argument) {
        return ToTemporalTimeZoneIdentifierNode.getUncached().execute(argument);
    }

}
