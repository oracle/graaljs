/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.temporal.DurationNudgeResultRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.NormalizedDurationRecord;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Disambiguation;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

/**
 * Implements Temporal RoundRelativeDuration and related operations.
 */
public abstract class RoundRelativeDurationNode extends JavaScriptBaseNode {

    protected RoundRelativeDurationNode() {
    }

    public abstract JSTemporalDurationRecord execute(NormalizedDurationRecord duration, BigInt originEpochNs, BigInt destEpochNs, ISODateTimeRecord isoDateTime,
                    TruffleString timeZone, TruffleString calendar,
                    Unit largestUnit, int increment, Unit smallestUnit, RoundingMode roundingMode);

    @Specialization
    protected final JSTemporalDurationRecord roundRelativeDuration(NormalizedDurationRecord duration0, BigInt originEpochNs, BigInt destEpochNs, ISODateTimeRecord isoDateTime,
                    TruffleString timeZone, TruffleString calendar,
                    Unit largestUnit0, int increment, Unit smallestUnit, RoundingMode roundingMode,
                    @Cached TemporalAddDateTimeNode addDateTimeNode,
                    @Cached NudgeToCalendarUnitNode nudgeToCalendarUnitNode,
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
            nudgeResult = nudgeToCalendarUnitNode.execute(sign, duration, originEpochNs, destEpochNs, isoDateTime, timeZone, calendar, increment, smallestUnit, roundingMode).nudgeResult();
        } else if (timeZone != null) {
            nudgeResult = nudgeToZonedTime(sign, duration, isoDateTime, calendar, timeZone, increment, smallestUnit, roundingMode,
                            addDateTimeNode, errorBranch);
        } else {
            nudgeResult = nudgeToDayOrTime(duration, destEpochNs, largestUnit, increment, smallestUnit, roundingMode);
        }
        duration = nudgeResult.duration();
        if (nudgeResult.didExpandCalendarUnit() && smallestUnit != Unit.WEEK) {
            Unit startUnit = TemporalUtil.largerOfTwoTemporalUnits(smallestUnit, Unit.DAY);
            duration = bubbleRelativeDuration(sign, duration, nudgeResult.nudgedEpochNs(), isoDateTime, calendar, timeZone, largestUnit, startUnit,
                            addDateTimeNode, errorBranch);
        }
        if (largestUnit.isCalendarUnit() || largestUnit == Unit.DAY) {
            largestUnit = Unit.HOUR;
        }
        var balanceResult = TemporalUtil.balanceTimeDuration(duration.normalizedTimeTotalNanoseconds(), largestUnit);
        return JSTemporalDurationRecord.createWeeks(
                        duration.years(), duration.months(), duration.weeks(), duration.days(),
                        balanceResult.hours(), balanceResult.minutes(), balanceResult.seconds(),
                        balanceResult.milliseconds(), balanceResult.microseconds(), balanceResult.nanoseconds());
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
        BigInt startEpochNs = TemporalUtil.getEpochNanosecondsFor(ctx, realm, timeZone, startDateTime, Disambiguation.COMPATIBLE);
        BigInt endEpochNs = TemporalUtil.getEpochNanosecondsFor(ctx, realm, timeZone, endDateTime, Disambiguation.COMPATIBLE);

        BigInt daySpan = TemporalUtil.normalizedTimeDurationFromEpochNanosecondsDifference(endEpochNs, startEpochNs);
        assert TemporalUtil.normalizedTimeDurationSign(daySpan) == sign;
        long unitLength = unit.getLengthInNanoseconds();
        BigInt roundedNorm = TemporalUtil.roundTimeDurationToIncrement(duration.normalizedTimeTotalNanoseconds(), unitLength, increment, roundingMode);
        BigInt beyondDaySpan = TemporalUtil.subtractNormalizedTimeDuration(roundedNorm, daySpan);
        boolean didRoundBeyondDay;
        int dayDelta;
        BigInt nudgedEpochNs;
        if (TemporalUtil.normalizedTimeDurationSign(beyondDaySpan) != -sign) {
            didRoundBeyondDay = true;
            dayDelta = sign;
            roundedNorm = TemporalUtil.roundTimeDurationToIncrement(beyondDaySpan, unitLength, increment, roundingMode);
            nudgedEpochNs = TemporalUtil.addNormalizedTimeDurationToEpochNanoseconds(roundedNorm, endEpochNs);
        } else {
            didRoundBeyondDay = false;
            dayDelta = 0;
            nudgedEpochNs = TemporalUtil.addNormalizedTimeDurationToEpochNanoseconds(roundedNorm, startEpochNs);
        }
        var resultDuration = TemporalUtil.createNormalizedDurationRecord(duration.years(), duration.months(), duration.weeks(), duration.days() + dayDelta, roundedNorm);
        return new DurationNudgeResultRecord(resultDuration, nudgedEpochNs, didRoundBeyondDay);
    }

    private static DurationNudgeResultRecord nudgeToDayOrTime(NormalizedDurationRecord duration, BigInt destEpochNs, Unit largestUnit, int increment, Unit unit, RoundingMode roundingMode) {
        BigInt norm = TemporalUtil.add24HourDaysToNormalizedTimeDuration(duration.normalizedTimeTotalNanoseconds(), duration.days());
        long unitLength = unit.getLengthInNanoseconds();
        BigInt roundedNorm = TemporalUtil.roundTimeDurationToIncrement(norm, unitLength, increment, roundingMode);
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
        return new DurationNudgeResultRecord(resultDuration, nudgedEpochNs, didExpandDays);
    }

    private NormalizedDurationRecord bubbleRelativeDuration(int sign, NormalizedDurationRecord duration0, BigInt nudgedEpochNs, ISODateTimeRecord dateTime, TruffleString calendar,
                    TruffleString timeZone, Unit largestUnit, Unit smallestUnit,
                    TemporalAddDateTimeNode addDateTimeNode, InlinedBranchProfile errorBranch) {
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
                endEpochNs = TemporalUtil.getEpochNanosecondsFor(ctx, realm, timeZone, endDateTime, Disambiguation.COMPATIBLE);
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

}
