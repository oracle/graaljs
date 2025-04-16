/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.temporal;

import java.math.BigDecimal;
import java.math.MathContext;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.NormalizedDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDurationWithTotalRecord;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Disambiguation;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;
import com.oracle.truffle.js.runtime.util.TemporalUtil.UnsignedRoundingMode;

/**
 * Implements Temporal RoundRelativeDuration and related operations.
 */
public abstract class RoundRelativeDurationNode extends JavaScriptBaseNode {

    protected RoundRelativeDurationNode() {
    }

    public abstract TemporalDurationWithTotalRecord execute(NormalizedDurationRecord duration, BigInt destEpochNs, ISODateTimeRecord dateTime,
                    TruffleString calendar, TruffleString timeZone,
                    Unit largestUnit, int increment, Unit smallestUnit, RoundingMode roundingMode);

    @Specialization
    protected final TemporalDurationWithTotalRecord roundRelativeDuration(NormalizedDurationRecord duration0, BigInt destEpochNs, ISODateTimeRecord dateTime,
                    TruffleString calendar, TruffleString timeZone,
                    Unit largestUnit0, int increment, Unit smallestUnit, RoundingMode roundingMode,
                    @Cached TemporalAddDateTimeNode addDateTimeNode,
                    @Cached TemporalDifferenceDateNode differenceDateNode,
                    @Cached InlinedBranchProfile errorBranch) {
        NormalizedDurationRecord duration = duration0;
        Unit largestUnit = largestUnit0;
        boolean irregularLengthUnit = smallestUnit.isCalendarUnit();
        if (timeZone != null && smallestUnit == Unit.DAY) {
            irregularLengthUnit = true;
        }
        int sign = TemporalUtil.durationSign(duration.years(), duration.months(), duration.weeks(), duration.days(),
                        TemporalUtil.normalizedTimeDurationSign(duration.normalizedTimeTotalNanoseconds()), 0, 0, 0, 0, 0) < 0 ? -1 : 1;
        DurationNudgeResultRecord nudgeResult;
        if (irregularLengthUnit) {
            nudgeResult = nudgeToCalendarUnit(sign, duration, destEpochNs, dateTime, calendar, timeZone, increment, smallestUnit, roundingMode,
                            addDateTimeNode, differenceDateNode, errorBranch);
        } else if (timeZone != null) {
            nudgeResult = nudgeToZonedTime(sign, duration, dateTime, calendar, timeZone, increment, smallestUnit, roundingMode,
                            addDateTimeNode, errorBranch);
        } else {
            nudgeResult = nudgeToDayOrTime(duration, destEpochNs, largestUnit, increment, smallestUnit, roundingMode);
        }
        duration = nudgeResult.duration();
        if (nudgeResult.didExpandCalendarUnit() && smallestUnit != Unit.WEEK) {
            Unit startUnit = TemporalUtil.largerOfTwoTemporalUnits(smallestUnit, Unit.DAY);
            duration = bubbleRelativeDuration(sign, duration, nudgeResult.nudgedEpochNs(), dateTime, calendar, timeZone, largestUnit, startUnit,
                            addDateTimeNode, errorBranch);
        }
        if (largestUnit.isCalendarUnit() || largestUnit == Unit.DAY) {
            largestUnit = Unit.HOUR;
        }
        var balanceResult = TemporalUtil.balanceTimeDuration(duration.normalizedTimeTotalNanoseconds(), largestUnit);
        return new TemporalDurationWithTotalRecord(JSTemporalDurationRecord.createWeeks(
                        duration.years(), duration.months(), duration.weeks(), duration.days(),
                        balanceResult.hours(), balanceResult.minutes(), balanceResult.seconds(),
                        balanceResult.milliseconds(), balanceResult.microseconds(), balanceResult.nanoseconds()), nudgeResult.total());
    }

    private DurationNudgeResultRecord nudgeToCalendarUnit(int sign, NormalizedDurationRecord duration, BigInt destEpochNs, ISODateTimeRecord dateTime, TruffleString calendar,
                    TruffleString timeZone, int increment, Unit unit, RoundingMode roundingMode,
                    TemporalAddDateTimeNode addDateTimeNode, TemporalDifferenceDateNode differenceDateNode, InlinedBranchProfile errorBranch) {
        JSRealm realm = getRealm();
        JSContext ctx = getJSContext();
        double r1;
        double r2;
        NormalizedDurationRecord startDuration;
        NormalizedDurationRecord endDuration;
        switch (unit) {
            case YEAR -> {
                double years = TemporalUtil.roundNumberToIncrement(duration.years(), increment, RoundingMode.TRUNC);
                r1 = years;
                r2 = years + increment * sign;
                startDuration = TemporalUtil.createNormalizedDurationRecord(r1, 0, 0, 0, TemporalUtil.zeroTimeDuration());
                endDuration = TemporalUtil.createNormalizedDurationRecord(r2, 0, 0, 0, TemporalUtil.zeroTimeDuration());
            }
            case MONTH -> {
                double months = TemporalUtil.roundNumberToIncrement(duration.months(), increment, RoundingMode.TRUNC);
                r1 = months;
                r2 = months + increment * sign;
                startDuration = TemporalUtil.createNormalizedDurationRecord(duration.years(), r1, 0, 0, TemporalUtil.zeroTimeDuration());
                endDuration = TemporalUtil.createNormalizedDurationRecord(duration.years(), r2, 0, 0, TemporalUtil.zeroTimeDuration());
            }
            case WEEK -> {
                ISODateRecord isoResult1 = TemporalUtil.balanceISODate(dateTime.year() + duration.years(), dateTime.month() + duration.months(), dateTime.day());
                ISODateRecord isoResult2 = TemporalUtil.balanceISODate(dateTime.year() + duration.years(), dateTime.month() + duration.months(), dateTime.day() + duration.days());
                JSTemporalPlainDateObject weeksStart = JSTemporalPlainDate.create(ctx, realm,
                                isoResult1.year(), isoResult1.month(), isoResult1.day(), calendar, this, errorBranch);
                JSTemporalPlainDateObject weeksEnd = JSTemporalPlainDate.create(ctx, realm,
                                isoResult2.year(), isoResult2.month(), isoResult2.day(), calendar, this, errorBranch);
                JSTemporalDurationObject untilResult = differenceDateNode.execute(calendar, weeksStart, weeksEnd, Unit.WEEK);
                double weeks = TemporalUtil.roundNumberToIncrement(duration.weeks() + untilResult.getWeeks(), increment, RoundingMode.TRUNC);
                r1 = weeks;
                r2 = weeks + increment * sign;
                startDuration = TemporalUtil.createNormalizedDurationRecord(duration.years(), duration.months(), r1, 0, TemporalUtil.zeroTimeDuration());
                endDuration = TemporalUtil.createNormalizedDurationRecord(duration.years(), duration.months(), r2, 0, TemporalUtil.zeroTimeDuration());
            }
            case DAY -> {
                double days = TemporalUtil.roundNumberToIncrement(duration.days(), increment, RoundingMode.TRUNC);
                r1 = days;
                r2 = days + increment * sign;
                startDuration = TemporalUtil.createNormalizedDurationRecord(duration.years(), duration.months(), duration.weeks(), r1, TemporalUtil.zeroTimeDuration());
                endDuration = TemporalUtil.createNormalizedDurationRecord(duration.years(), duration.months(), duration.weeks(), r2, TemporalUtil.zeroTimeDuration());
            }
            default -> throw Errors.shouldNotReachHereUnexpectedValue(unit);
        }
        JSTemporalDateTimeRecord start = addDateTimeNode.execute(dateTime.year(), dateTime.month(), dateTime.day(),
                        dateTime.hour(), dateTime.minute(), dateTime.second(), dateTime.millisecond(), dateTime.microsecond(), dateTime.nanosecond(),
                        calendar,
                        startDuration.years(), startDuration.months(), startDuration.weeks(), startDuration.days(), startDuration.normalizedTimeTotalNanoseconds(),
                        TemporalUtil.Overflow.CONSTRAIN);
        JSTemporalDateTimeRecord end = addDateTimeNode.execute(dateTime.year(), dateTime.month(), dateTime.day(),
                        dateTime.hour(), dateTime.minute(), dateTime.second(), dateTime.millisecond(), dateTime.microsecond(), dateTime.nanosecond(),
                        calendar,
                        endDuration.years(), endDuration.months(), endDuration.weeks(), endDuration.days(), endDuration.normalizedTimeTotalNanoseconds(),
                        TemporalUtil.Overflow.CONSTRAIN);
        BigInt startEpochNs;
        BigInt endEpochNs;
        if (timeZone == null) {
            startEpochNs = TemporalUtil.getUTCEpochNanoseconds(start.getYear(), start.getMonth(), start.getDay(),
                            start.getHour(), start.getMinute(), start.getSecond(), start.getMillisecond(), start.getMicrosecond(), start.getNanosecond());
            endEpochNs = TemporalUtil.getUTCEpochNanoseconds(end.getYear(), end.getMonth(), end.getDay(),
                            end.getHour(), end.getMinute(), end.getSecond(), end.getMillisecond(), end.getMicrosecond(), end.getNanosecond());
        } else {
            var startDateTime = JSTemporalPlainDateTime.create(ctx, realm, start.getYear(), start.getMonth(), start.getDay(),
                            start.getHour(), start.getMinute(), start.getSecond(), start.getMillisecond(), start.getMicrosecond(), start.getNanosecond(),
                            calendar, this, errorBranch);
            startEpochNs = TemporalUtil.builtinTimeZoneGetInstantFor(ctx, realm, timeZone, startDateTime, Disambiguation.COMPATIBLE);
            var endDateTime = JSTemporalPlainDateTime.create(ctx, realm, end.getYear(), end.getMonth(), end.getDay(),
                            end.getHour(), end.getMinute(), end.getSecond(), end.getMillisecond(), end.getMicrosecond(), end.getNanosecond(),
                            calendar, this, errorBranch);
            endEpochNs = TemporalUtil.builtinTimeZoneGetInstantFor(ctx, realm, timeZone, endDateTime, Disambiguation.COMPATIBLE);
        }
        if (startEpochNs.compareTo(endEpochNs) == 0) {
            throw Errors.createRangeError("custom calendar method returned an illegal result");
        }

        BigInt numerator = destEpochNs.subtract(startEpochNs);
        BigInt denominator = endEpochNs.subtract(startEpochNs);
        BigDecimal progress = new BigDecimal(numerator.bigIntegerValue()).divide(new BigDecimal(denominator.bigIntegerValue()), MathContext.DECIMAL128);
        BigDecimal total = new BigDecimal(r1).add(progress.multiply(new BigDecimal(increment * sign)));

        boolean isNegative = sign < 0;
        UnsignedRoundingMode unsignedRoundingMode = TemporalUtil.getUnsignedRoundingMode(roundingMode, isNegative);

        double roundedUnit;
        if (BigDecimal.ONE.equals(progress)) {
            roundedUnit = Math.abs(r2);
        } else {
            // ApplyUnsignedRoundingMode(abs(total), abs(r1), abs(r2), unsignedRoundingMode)
            roundedUnit = TemporalUtil.applyUnsignedRoundingMode(numerator.abs(), denominator.abs(), Math.abs(r1), Math.abs(r2), unsignedRoundingMode);
        }

        boolean didExpandCalendarUnit;
        NormalizedDurationRecord resultDuration;
        BigInt nudgedEpochNs;
        if (roundedUnit == Math.abs(r2)) {
            didExpandCalendarUnit = true;
            resultDuration = endDuration;
            nudgedEpochNs = endEpochNs;
        } else {
            didExpandCalendarUnit = false;
            resultDuration = startDuration;
            nudgedEpochNs = startEpochNs;
        }

        return new DurationNudgeResultRecord(resultDuration, total.doubleValue(), nudgedEpochNs, didExpandCalendarUnit);
    }

    @TruffleBoundary
    private static double computeTotal(double r1, int increment, int sign, BigInt numerator, BigInt denominator) {
        /*
         * The following two steps cannot be implemented directly using floating-point arithmetic.
         * This division can be implemented as if constructing Normalized Time Duration Records for
         * the denominator and numerator of total and performing one division operation with a
         * floating-point result.
         */
        BigDecimal progress = new BigDecimal(numerator.bigIntegerValue()).divide(new BigDecimal(denominator.bigIntegerValue()), MathContext.DECIMAL128);
        BigDecimal total = new BigDecimal(r1).add(progress.multiply(new BigDecimal(increment * sign)));
        return total.doubleValue();
    }

    private DurationNudgeResultRecord nudgeToZonedTime(int sign, NormalizedDurationRecord duration, ISODateTimeRecord dateTime, TruffleString calendar,
                    TruffleString timeZone, int increment, Unit unit, RoundingMode roundingMode,
                    TemporalAddDateTimeNode addDateTimeNode, InlinedBranchProfile errorBranch) {
        JSRealm realm = getRealm();
        JSContext ctx = getJSContext();
        JSTemporalDateTimeRecord start = addDateTimeNode.execute(dateTime.year(), dateTime.month(), dateTime.day(),
                        dateTime.hour(), dateTime.minute(), dateTime.second(), dateTime.millisecond(), dateTime.microsecond(), dateTime.nanosecond(),
                        calendar,
                        duration.years(), duration.months(), duration.weeks(), duration.days(), TemporalUtil.zeroTimeDuration(),
                        TemporalUtil.Overflow.CONSTRAIN);
        var startDateTime = JSTemporalPlainDateTime.create(ctx, realm, start.getYear(), start.getMonth(), start.getDay(),
                        start.getHour(), start.getMinute(), start.getSecond(), start.getMillisecond(), start.getMicrosecond(), start.getNanosecond(),
                        calendar, this, errorBranch);
        ISODateRecord endDate = TemporalUtil.balanceISODate(start.getYear(), start.getMonth(), start.getDay() + sign);
        var endDateTime = JSTemporalPlainDateTime.create(ctx, realm, endDate.year(), endDate.month(), endDate.day(),
                        start.getHour(), start.getMinute(), start.getSecond(), start.getMillisecond(), start.getMicrosecond(), start.getNanosecond(),
                        calendar, this, errorBranch);
        BigInt startEpochNs = TemporalUtil.builtinTimeZoneGetInstantFor(ctx, realm, timeZone, startDateTime, Disambiguation.COMPATIBLE);
        BigInt endEpochNs = TemporalUtil.builtinTimeZoneGetInstantFor(ctx, realm, timeZone, endDateTime, Disambiguation.COMPATIBLE);

        BigInt daySpan = TemporalUtil.normalizedTimeDurationFromEpochNanosecondsDifference(endEpochNs, startEpochNs);
        assert TemporalUtil.normalizedTimeDurationSign(daySpan) == sign;
        long unitLength = unit.getLengthInNanoseconds();
        BigInt roundedNorm = TemporalUtil.roundNormalizedTimeDurationToIncrement(duration.normalizedTimeTotalNanoseconds(), unitLength, increment, roundingMode);
        BigInt beyondDaySpan = TemporalUtil.subtractNormalizedTimeDuration(roundedNorm, daySpan);
        boolean didRoundBeyondDay;
        int dayDelta;
        BigInt nudgedEpochNs;
        if (TemporalUtil.normalizedTimeDurationSign(beyondDaySpan) != -sign) {
            didRoundBeyondDay = true;
            dayDelta = sign;
            roundedNorm = TemporalUtil.roundNormalizedTimeDurationToIncrement(beyondDaySpan, unitLength, increment, roundingMode);
            nudgedEpochNs = TemporalUtil.addNormalizedTimeDurationToEpochNanoseconds(roundedNorm, endEpochNs);
        } else {
            didRoundBeyondDay = false;
            dayDelta = 0;
            nudgedEpochNs = TemporalUtil.addNormalizedTimeDurationToEpochNanoseconds(roundedNorm, startEpochNs);
        }
        var resultDuration = TemporalUtil.createNormalizedDurationRecord(duration.years(), duration.months(), duration.weeks(), duration.days() + dayDelta, roundedNorm);
        return new DurationNudgeResultRecord(resultDuration, Double.NaN /* unset */, nudgedEpochNs, didRoundBeyondDay);
    }

    private static DurationNudgeResultRecord nudgeToDayOrTime(NormalizedDurationRecord duration, BigInt destEpochNs, Unit largestUnit, int increment, Unit unit, RoundingMode roundingMode) {
        BigInt norm = TemporalUtil.add24HourDaysToNormalizedTimeDuration(duration.normalizedTimeTotalNanoseconds(), duration.days());
        long unitLength = unit.getLengthInNanoseconds();
        double total = TemporalUtil.divideNormalizedTimeDurationAsDouble(norm, unitLength);
        BigInt roundedNorm = TemporalUtil.roundNormalizedTimeDurationToIncrement(norm, unitLength, increment, roundingMode);
        BigInt diffNorm = TemporalUtil.subtractNormalizedTimeDuration(roundedNorm, norm);
        double wholeDays = TemporalUtil.divideNormalizedTimeDurationAsDoubleTruncate(norm, TemporalUtil.NS_PER_DAY_LONG);
        double roundedWholeDays = TemporalUtil.divideNormalizedTimeDurationAsDoubleTruncate(roundedNorm, TemporalUtil.NS_PER_DAY_LONG);
        double dayDelta = roundedWholeDays - wholeDays;
        int dayDeltaSign = Double.compare(dayDelta, 0);
        boolean didExpandDays = dayDeltaSign == TemporalUtil.normalizedTimeDurationSign(norm);
        BigInt nudgedEpochNs = TemporalUtil.addNormalizedTimeDurationToEpochNanoseconds(diffNorm, destEpochNs);
        double days = 0;
        BigInt remainder = roundedNorm;
        if (TemporalUtil.largerOfTwoTemporalUnits(largestUnit, Unit.DAY) == largestUnit) {
            days = roundedWholeDays;
            remainder = TemporalUtil.remainderNormalizedTimeDuration(roundedNorm, TemporalUtil.NS_PER_DAY_LONG);
        }
        var resultDuration = TemporalUtil.createNormalizedDurationRecord(duration.years(), duration.months(), duration.weeks(), days, remainder);
        return new DurationNudgeResultRecord(resultDuration, total, nudgedEpochNs, didExpandDays);
    }

    private NormalizedDurationRecord bubbleRelativeDuration(int sign, NormalizedDurationRecord duration0, BigInt nudgedEpochNs, ISODateTimeRecord dateTime, TruffleString calendar,
                    TruffleString timeZone, Unit largestUnit, Unit smallestUnit,
                    TemporalAddDateTimeNode addDateTimeNode, InlinedBranchProfile errorBranch) {
        assert largestUnit.isDateUnit() : largestUnit;
        assert smallestUnit.isDateUnit() : smallestUnit;
        NormalizedDurationRecord duration = duration0;
        if (smallestUnit == Unit.YEAR) {
            return duration;
        }
        JSRealm realm = getRealm();
        JSContext ctx = getJSContext();
        int largestUnitIndex = largestUnit.ordinal();
        int smallestUnitIndex = smallestUnit.ordinal();
        for (int unitIndex = smallestUnitIndex - 1; unitIndex >= largestUnitIndex; unitIndex--) {
            Unit unit = Unit.VALUES[unitIndex];
            if (!(unit != Unit.WEEK || largestUnit == Unit.WEEK)) {
                continue;
            }
            NormalizedDurationRecord endDuration;
            switch (unit) {
                case YEAR -> {
                    double years = duration.years() + sign;
                    endDuration = TemporalUtil.createNormalizedDurationRecord(years, 0, 0, 0, TemporalUtil.zeroTimeDuration());
                }
                case MONTH -> {
                    double months = duration.months() + sign;
                    endDuration = TemporalUtil.createNormalizedDurationRecord(duration.years(), months, 0, 0, TemporalUtil.zeroTimeDuration());
                }
                case WEEK -> {
                    double weeks = duration.weeks() + sign;
                    endDuration = TemporalUtil.createNormalizedDurationRecord(duration.years(), duration.months(), weeks, 0, TemporalUtil.zeroTimeDuration());
                }
                case DAY -> {
                    double days = duration.days() + sign;
                    endDuration = TemporalUtil.createNormalizedDurationRecord(duration.years(), duration.months(), duration.weeks(), days, TemporalUtil.zeroTimeDuration());
                }
                default -> throw Errors.shouldNotReachHereUnexpectedValue(unit);
            }
            JSTemporalDateTimeRecord end = addDateTimeNode.execute(dateTime.year(), dateTime.month(), dateTime.day(),
                            dateTime.hour(), dateTime.minute(), dateTime.second(), dateTime.millisecond(), dateTime.microsecond(), dateTime.nanosecond(),
                            calendar,
                            endDuration.years(), endDuration.months(), endDuration.weeks(), endDuration.days(), endDuration.normalizedTimeTotalNanoseconds(),
                            TemporalUtil.Overflow.CONSTRAIN);
            BigInt endEpochNs;
            if (timeZone == null) {
                endEpochNs = TemporalUtil.getUTCEpochNanoseconds(end.getYear(), end.getMonth(), end.getDay(),
                                end.getHour(), end.getMinute(), end.getSecond(), end.getMillisecond(), end.getMicrosecond(), end.getNanosecond());
            } else {
                var endDateTime = JSTemporalPlainDateTime.create(ctx, realm, end.getYear(), end.getMonth(), end.getDay(),
                                end.getHour(), end.getMinute(), end.getSecond(), end.getMillisecond(), end.getMicrosecond(), end.getNanosecond(),
                                calendar, this, errorBranch);
                endEpochNs = TemporalUtil.builtinTimeZoneGetInstantFor(ctx, realm, timeZone, endDateTime, Disambiguation.COMPATIBLE);
            }
            BigInt beyondEnd = nudgedEpochNs.subtract(endEpochNs);
            int beyondEndSign = beyondEnd.signum();
            if (beyondEndSign != -sign) {
                duration = endDuration;
            } else {
                break;
            }
        }
        return duration;
    }

    /**
     * A Duration Nudge Result Record is a value used to represent the result of rounding a duration
     * up or down to an increment relative to a date-time. Returned by the following operations:
     * NudgeToCalendarUnit, NudgeToZonedTime, NudgeToDayOrTime.
     *
     * @param duration The resulting duration.
     * @param total The possibly fractional total of the smallest unit before the rounding
     *            operation, for use in Temporal.Duration.prototype.total, or NaN if not relevant.
     * @param nudgedEpochNs The epoch time corresponding to the rounded duration, relative to the
     *            starting point.
     * @param didExpandCalendarUnit Whether the rounding operation caused the duration to expand to
     *            the next day or larger unit.
     */
    public record DurationNudgeResultRecord(
                    NormalizedDurationRecord duration,
                    double total,
                    BigInt nudgedEpochNs,
                    boolean didExpandCalendarUnit) {
    }
}
