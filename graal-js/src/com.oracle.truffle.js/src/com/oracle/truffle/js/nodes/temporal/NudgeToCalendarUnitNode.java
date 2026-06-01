/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.js.runtime.builtins.temporal.DurationNudgeResultRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.NormalizedDurationRecord;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Disambiguation;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Overflow;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;
import com.oracle.truffle.js.runtime.util.TemporalUtil.UnsignedRoundingMode;

/**
 * Implements the NudgeToCalendarUnit operation.
 */
public abstract class NudgeToCalendarUnitNode extends JavaScriptBaseNode {

    protected NudgeToCalendarUnitNode() {
    }

    /**
     * Record returned by the NudgeToCalendarUnit abstract operation.
     */
    public record Result(
                    DurationNudgeResultRecord nudgeResult,
                    double total) {
    }

    public abstract Result execute(int sign, NormalizedDurationRecord duration, BigInt originEpochNs, BigInt destEpochNs, ISODateTimeRecord isoDateTime,
                    TruffleString timeZone, TruffleString calendar, int increment, Unit unit, RoundingMode roundingMode);

    @Specialization
    protected final Result nudgeToCalendarUnit(int sign, NormalizedDurationRecord duration, BigInt originEpochNs, BigInt destEpochNs, ISODateTimeRecord isoDateTime,
                    TruffleString timeZone, TruffleString calendar, int increment, Unit unit, RoundingMode roundingMode,
                    @Cached TemporalAddDateNode addDateNode,
                    @Cached TemporalDifferenceDateNode differenceDateNode,
                    @Cached InlinedBranchProfile errorBranch) {
        JSRealm realm = getRealm();
        JSContext ctx = getJSContext();
        double r1 = 0;
        double r2 = 0;
        NormalizedDurationRecord startDuration = null;
        NormalizedDurationRecord endDuration = null;
        BigInt startEpochNs = null;
        BigInt endEpochNs = null;
        boolean didExpandCalendarUnit;

        switch (unit) {
            case YEAR -> r1 = TemporalUtil.roundNumberToIncrement(duration.years(), increment, RoundingMode.TRUNC);
            case MONTH -> r1 = TemporalUtil.roundNumberToIncrement(duration.months(), increment, RoundingMode.TRUNC);
            case WEEK -> {
                JSTemporalPlainDateObject weeksStart = calendarDateAdd(ctx, realm, calendar, isoDateTime, duration.years(), duration.months(), 0, 0, addDateNode, errorBranch);
                ISODateRecord weeksEndRecord = TemporalUtil.addISODate(weeksStart.getYear(), weeksStart.getMonth(), weeksStart.getDay(), 0, 0, 0, duration.days(), Overflow.CONSTRAIN);
                JSTemporalPlainDateObject weeksEnd = JSTemporalPlainDate.create(ctx, realm, weeksEndRecord.year(), weeksEndRecord.month(), weeksEndRecord.day(), calendar, this, errorBranch);
                JSTemporalDurationObject untilResult = differenceDateNode.execute(calendar, weeksStart, weeksEnd, Unit.WEEK);
                r1 = TemporalUtil.roundNumberToIncrement(duration.weeks() + untilResult.getWeeks(), increment, RoundingMode.TRUNC);
            }
            case DAY -> r1 = TemporalUtil.roundNumberToIncrement(duration.days(), increment, RoundingMode.TRUNC);
            default -> throw Errors.shouldNotReachHereUnexpectedValue(unit);
        }
        r2 = r1 + increment * sign;
        startDuration = createNudgeDurationForUnit(duration, unit, r1);
        endDuration = createNudgeDurationForUnit(duration, unit, r2);
        startEpochNs = (r1 == 0) ? originEpochNs : getEpochNanosecondsForDateDuration(ctx, realm, startDuration, isoDateTime, calendar, timeZone, addDateNode, errorBranch);
        endEpochNs = getEpochNanosecondsForDateDuration(ctx, realm, endDuration, isoDateTime, calendar, timeZone, addDateNode, errorBranch);

        if (containsEpochNs(sign, startEpochNs, destEpochNs, endEpochNs)) {
            didExpandCalendarUnit = false;
        } else {
            didExpandCalendarUnit = true;
            r1 = r2;
            r2 = r1 + increment * sign;
            startDuration = endDuration;
            startEpochNs = endEpochNs;
            endDuration = createNudgeDurationForUnit(duration, unit, r2);
            endEpochNs = getEpochNanosecondsForDateDuration(ctx, realm, endDuration, isoDateTime, calendar, timeZone, addDateNode, errorBranch);
            assert containsEpochNs(sign, startEpochNs, destEpochNs, endEpochNs);
        }
        assert startEpochNs.compareTo(endEpochNs) != 0;

        BigInt numerator = destEpochNs.subtract(startEpochNs);
        BigInt denominator = endEpochNs.subtract(startEpochNs);
        double total = computeTotal(r1, numerator, denominator, increment, sign);

        boolean isNegative = sign < 0;
        UnsignedRoundingMode unsignedRoundingMode = TemporalUtil.getUnsignedRoundingMode(roundingMode, isNegative);

        double roundedUnit;
        if (numerator.equals(denominator)) {
            roundedUnit = Math.abs(r2);
        } else {
            roundedUnit = TemporalUtil.applyUnsignedRoundingMode(numerator.abs(), denominator.abs(), Math.abs(r1), Math.abs(r2), unsignedRoundingMode);
        }

        NormalizedDurationRecord resultDuration;
        BigInt nudgedEpochNs;
        if (roundedUnit == Math.abs(r2)) {
            didExpandCalendarUnit = true;
            resultDuration = endDuration;
            nudgedEpochNs = endEpochNs;
        } else {
            resultDuration = startDuration;
            nudgedEpochNs = startEpochNs;
        }

        return new Result(new DurationNudgeResultRecord(resultDuration, nudgedEpochNs, didExpandCalendarUnit), total);
    }

    private static NormalizedDurationRecord createNudgeDurationForUnit(NormalizedDurationRecord duration, Unit unit, double value) {
        return switch (unit) {
            case YEAR -> TemporalUtil.createNormalizedDurationRecord(value, 0, 0, 0, TemporalUtil.zeroTimeDuration());
            case MONTH -> TemporalUtil.createNormalizedDurationRecord(duration.years(), value, 0, 0, TemporalUtil.zeroTimeDuration());
            case WEEK -> TemporalUtil.createNormalizedDurationRecord(duration.years(), duration.months(), value, 0, TemporalUtil.zeroTimeDuration());
            case DAY -> TemporalUtil.createNormalizedDurationRecord(duration.years(), duration.months(), duration.weeks(), value, TemporalUtil.zeroTimeDuration());
            default -> throw Errors.shouldNotReachHereUnexpectedValue(unit);
        };
    }

    private JSTemporalPlainDateObject calendarDateAdd(JSContext ctx, JSRealm realm, TruffleString calendar, ISODateTimeRecord dateTime,
                    double years, double months, double weeks, double days,
                    TemporalAddDateNode addDateNode, InlinedBranchProfile errorBranch) {
        JSTemporalPlainDateObject plainDate = JSTemporalPlainDate.create(ctx, realm, dateTime.year(), dateTime.month(), dateTime.day(), calendar, this, errorBranch);
        JSTemporalDurationObject duration = JSTemporalDuration.createTemporalDuration(ctx, realm, years, months, weeks, days, 0, 0, 0, 0, 0, 0, this, errorBranch);
        return addDateNode.execute(calendar, plainDate, duration, Overflow.CONSTRAIN);
    }

    private BigInt getEpochNanosecondsForDateDuration(JSContext ctx, JSRealm realm, NormalizedDurationRecord duration, ISODateTimeRecord dateTime, TruffleString calendar, TruffleString timeZone,
                    TemporalAddDateNode addDateNode, InlinedBranchProfile errorBranch) {
        JSTemporalPlainDateObject date = calendarDateAdd(ctx, realm, calendar, dateTime, duration.years(), duration.months(), duration.weeks(), duration.days(), addDateNode, errorBranch);
        if (timeZone == null) {
            return TemporalUtil.getUTCEpochNanoseconds(date.getYear(), date.getMonth(), date.getDay(),
                            dateTime.hour(), dateTime.minute(), dateTime.second(), dateTime.millisecond(), dateTime.microsecond(), dateTime.nanosecond());
        }
        var resultDateTime = JSTemporalPlainDateTime.create(ctx, realm, date.getYear(), date.getMonth(), date.getDay(),
                        dateTime.hour(), dateTime.minute(), dateTime.second(), dateTime.millisecond(), dateTime.microsecond(), dateTime.nanosecond(),
                        calendar, this, errorBranch);
        return TemporalUtil.getEpochNanosecondsFor(ctx, realm, timeZone, resultDateTime, Disambiguation.COMPATIBLE);
    }

    private static boolean containsEpochNs(int sign, BigInt startEpochNs, BigInt destEpochNs, BigInt endEpochNs) {
        if (sign == 1) {
            return startEpochNs.compareTo(destEpochNs) <= 0 && destEpochNs.compareTo(endEpochNs) <= 0;
        }
        return endEpochNs.compareTo(destEpochNs) <= 0 && destEpochNs.compareTo(startEpochNs) <= 0;
    }

    @TruffleBoundary
    private static double computeTotal(double r1, BigInt numerator, BigInt denominator, int increment, int sign) {
        BigDecimal denominatorDecimal = new BigDecimal(denominator.bigIntegerValue());
        BigDecimal dividend = new BigDecimal(r1).multiply(denominatorDecimal).add(new BigDecimal(numerator.bigIntegerValue()).multiply(BigDecimal.valueOf((long) increment * sign)));
        return dividend.divide(denominatorDecimal, MathContext.DECIMAL128).doubleValue();
    }
}
