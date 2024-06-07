/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.LARGEST_UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtol;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.CalendarMethodsRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.MoveRelativeDateResult;
import com.oracle.truffle.js.runtime.builtins.temporal.NormalizedTimeDurationToDaysResult;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeZoneMethodsRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Overflow;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

/**
 * Implementation of the RoundDuration operation.
 */
public abstract class TemporalRoundDurationNode extends JavaScriptBaseNode {

    protected TemporalRoundDurationNode() {
    }

    public final JSTemporalDurationRecord execute(double y, double m, double w, double d, double h, double min,
                    double sec, double milsec, double micsec, double nsec, int increment,
                    TemporalUtil.Unit unit, TemporalUtil.RoundingMode roundingMode) {
        return execute(y, m, w, d, h, min, sec, milsec, micsec, nsec, increment, unit, roundingMode,
                        null, null, null, null, null);
    }

    public final JSTemporalDurationRecord execute(double y, double m, double w, double d, double h, double min,
                    double sec, double milsec, double micsec, double nsec, int increment,
                    TemporalUtil.Unit unit, TemporalUtil.RoundingMode roundingMode,
                    JSTemporalPlainDateObject plainRelativeTo,
                    CalendarMethodsRecord calendarRec) {
        return execute(y, m, w, d, h, min, sec, milsec, micsec, nsec, increment, unit, roundingMode,
                        plainRelativeTo, null, calendarRec, null, null);
    }

    public abstract JSTemporalDurationRecord execute(double y, double m, double w, double d, double h, double min,
                    double sec, double milsec, double micsec, double nsec, int increment,
                    TemporalUtil.Unit unit, TemporalUtil.RoundingMode roundingMode,
                    JSTemporalPlainDateObject plainRelativeTo, JSTemporalZonedDateTimeObject zonedRelativeTo,
                    CalendarMethodsRecord calendarRec, TimeZoneMethodsRecord timeZoneRec,
                    JSTemporalPlainDateTimeObject precalculatedPlainDateTime);

    // @Cached parameters create unused variable in generated code, see GR-37931
    @Specialization
    protected JSTemporalDurationRecord round(double years, double months, double weeks, double d, double h, double min,
                    double sec, double milsec, double micsec, double nsec, int increment,
                    TemporalUtil.Unit unit, TemporalUtil.RoundingMode roundingMode,
                    JSTemporalPlainDateObject plainRelativeTo, JSTemporalZonedDateTimeObject zonedRelativeTo,
                    CalendarMethodsRecord calendarRec, TimeZoneMethodsRecord timeZoneRec,
                    JSTemporalPlainDateTimeObject precalculatedPlainDateTime,
                    @Cached TemporalDifferenceDateNode differenceDateNode,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached InlinedConditionProfile unitYMWD,
                    @Cached InlinedBranchProfile yearBranch,
                    @Cached InlinedBranchProfile monthBranch,
                    @Cached InlinedBranchProfile weekBranch,
                    @Cached InlinedBranchProfile dayOrLessBranch,
                    @Cached TemporalMoveRelativeDateNode moveRelativeDateNode) {
        assert plainRelativeTo == null && zonedRelativeTo == null || calendarRec != null && calendarRec.dateAdd() != null && calendarRec.dateUntil() != null;

        double days = d;
        double hours = h;
        double minutes = min;
        double seconds = sec;
        double microseconds = micsec;
        double milliseconds = milsec;
        double nanoseconds = nsec;

        if ((unit == Unit.YEAR || unit == Unit.MONTH || unit == Unit.WEEK) && plainRelativeTo == null) {
            errorBranch.enter(this);
            throw TemporalErrors.createRangeErrorRelativeToNotUndefined(unit);
        }
        JSContext ctx = getLanguage().getJSContext();
        JSRealm realm = getRealm();

        double fractionalSeconds;
        if (unitYMWD.profile(this, unit == Unit.YEAR || unit == Unit.MONTH || unit == Unit.WEEK || unit == Unit.DAY)) {
            BigInt totalNs = TemporalUtil.totalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
            JSTemporalZonedDateTimeObject intermediate = null;
            if (zonedRelativeTo != null) {
                intermediate = TemporalUtil.moveRelativeZonedDateTime(ctx, realm, zonedRelativeTo, calendarRec, timeZoneRec, dtol(years), dtol(months), dtol(weeks), dtol(days),
                                precalculatedPlainDateTime);
                NormalizedTimeDurationToDaysResult result = TemporalUtil.normalizedTimeDurationToDays(ctx, realm, totalNs, intermediate, timeZoneRec);
                days = calculateDays(days, result);
            } else {
                days = days + nanoseconds / TemporalUtil.NS_PER_DAY;
            }
            hours = 0;
            minutes = 0;
            seconds = 0;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
            fractionalSeconds = 0; // fractionalSeconds is not used below.
        } else {
            fractionalSeconds = TemporalUtil.roundDurationCalculateFractionalSeconds(seconds, milliseconds, microseconds, nanoseconds);
        }
        switch (unit) {
            case YEAR:
                yearBranch.enter(this);
                return getUnitYear(increment, roundingMode, years, months, weeks, days, hours, minutes, seconds, microseconds, milliseconds, nanoseconds,
                                plainRelativeTo, calendarRec,
                                moveRelativeDateNode, differenceDateNode, this, dayOrLessBranch);
            case MONTH:
                monthBranch.enter(this);
                return getUnitMonth(increment, roundingMode, years, months, weeks, days, hours, minutes, seconds, microseconds, milliseconds, nanoseconds,
                                plainRelativeTo, calendarRec,
                                moveRelativeDateNode, differenceDateNode, this, dayOrLessBranch);
            case WEEK:
                weekBranch.enter(this);
                return getUnitWeek(increment, roundingMode, years, months, weeks, days, hours, minutes, seconds, microseconds, milliseconds, nanoseconds,
                                plainRelativeTo, calendarRec,
                                moveRelativeDateNode, differenceDateNode, this, dayOrLessBranch);
            case DAY:
                dayOrLessBranch.enter(this);
                return getUnitDay(increment, roundingMode, years, months, weeks, days, hours, minutes, seconds, microseconds, milliseconds, nanoseconds);
            case HOUR:
                dayOrLessBranch.enter(this);
                return getUnitHour(increment, roundingMode, years, months, weeks, days, hours, minutes, fractionalSeconds);
            case MINUTE:
                dayOrLessBranch.enter(this);
                return getUnitMinute(increment, roundingMode, years, months, weeks, days, hours, minutes, fractionalSeconds);
            case SECOND:
                dayOrLessBranch.enter(this);
                return getUnitSecond(increment, roundingMode, years, months, weeks, days, hours, minutes, fractionalSeconds);
            case MILLISECOND:
                dayOrLessBranch.enter(this);
                return getUnitMillisecond(increment, roundingMode, years, months, weeks, days, hours, minutes, seconds, microseconds, milliseconds, nanoseconds);
            case MICROSECOND:
                dayOrLessBranch.enter(this);
                return getUnitMicrosecond(increment, roundingMode, years, months, weeks, days, hours, minutes, seconds, microseconds, milliseconds, nanoseconds);
            case NANOSECOND:
                dayOrLessBranch.enter(this);
                return getUnitNanosecond(increment, roundingMode, years, months, weeks, days, hours, minutes, seconds, microseconds, milliseconds, nanoseconds);
        }
        throw Errors.shouldNotReachHere();
    }

    private static JSTemporalDurationRecord getUnitNanosecond(int increment, TemporalUtil.RoundingMode roundingMode, final double years, final double months, final double weeks, final double days,
                    final double hours, final double minutes, final double seconds, final double microseconds, final double milliseconds, final double nanosecondsP) {
        double nanoseconds = nanosecondsP;
        double remainder = nanoseconds;
        nanoseconds = TemporalUtil.roundNumberToIncrement(nanoseconds, increment, roundingMode);
        remainder = remainder - nanoseconds;
        return JSTemporalDurationRecord.createWeeksRemainder(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, remainder);
    }

    private static JSTemporalDurationRecord getUnitMicrosecond(int increment, TemporalUtil.RoundingMode roundingMode, final double years, final double months, final double weeks, final double days,
                    final double hours, final double minutes, final double seconds, final double microsecondsP, final double milliseconds, final double nanoseconds) {
        double microseconds = microsecondsP;
        double fractionalMicroseconds = (nanoseconds * 0.001) + microseconds;
        microseconds = TemporalUtil.roundNumberToIncrement(fractionalMicroseconds, increment, roundingMode);
        double remainder = fractionalMicroseconds - microseconds;
        return JSTemporalDurationRecord.createWeeksRemainder(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, 0, remainder);
    }

    private static JSTemporalDurationRecord getUnitMillisecond(int increment, TemporalUtil.RoundingMode roundingMode, final double years, final double months, final double weeks, final double days,
                    final double hours, final double minutes, final double seconds, final double microseconds, final double millisecondsP, final double nanoseconds) {
        double milliseconds = millisecondsP;
        double fractionalMilliseconds = (nanoseconds * 0.000_001) + (microseconds * 0.001) + milliseconds;
        milliseconds = TemporalUtil.roundNumberToIncrement(fractionalMilliseconds, increment, roundingMode);
        double remainder = fractionalMilliseconds - milliseconds;
        return JSTemporalDurationRecord.createWeeksRemainder(years, months, weeks, days, hours, minutes, seconds, milliseconds, 0, 0, remainder);
    }

    private static JSTemporalDurationRecord getUnitMinute(int increment, TemporalUtil.RoundingMode roundingMode, final double years, final double months, final double weeks, final double days,
                    final double hours, final double minutesP, double fractionalSeconds) {
        double minutes = minutesP;
        double fractionalMinutes = (fractionalSeconds / 60) + minutes;
        minutes = TemporalUtil.roundNumberToIncrement(fractionalMinutes, increment, roundingMode);
        double remainder = fractionalMinutes - minutes;
        return JSTemporalDurationRecord.createWeeksRemainder(years, months, weeks, days, hours, minutes, 0, 0, 0, 0, remainder);
    }

    private static JSTemporalDurationRecord getUnitHour(int increment, TemporalUtil.RoundingMode roundingMode, final double years, final double months, final double weeks, final double days,
                    final double hoursP, final double minutes, double fractionalSeconds) {
        double hours = hoursP;
        double fractionalHours = (((fractionalSeconds / 60) + minutes) / 60) + hours;
        hours = TemporalUtil.roundNumberToIncrement(fractionalHours, increment, roundingMode);
        double remainder = fractionalHours - hours;
        return JSTemporalDurationRecord.createWeeksRemainder(years, months, weeks, days, hours, 0, 0, 0, 0, 0, remainder);
    }

    private static JSTemporalDurationRecord getUnitDay(int increment, TemporalUtil.RoundingMode roundingMode, final double years, final double months, final double weeks, final double daysP,
                    final double hours, final double minutes, final double seconds, final double microseconds, final double milliseconds, final double nanoseconds) {
        double fractionalDays = daysP;
        double days = TemporalUtil.roundNumberToIncrement(daysP, increment, roundingMode);
        double remainder = fractionalDays - days;
        return JSTemporalDurationRecord.createWeeksRemainder(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, remainder);
    }

    private JSTemporalDurationRecord getUnitWeek(int increment, TemporalUtil.RoundingMode roundingMode,
                    final double years, final double months, final double weeksP, final double daysP,
                    final double hours, final double minutes, final double seconds, final double microseconds, final double milliseconds, final double nanoseconds,
                    JSTemporalPlainDateObject relativeToP, CalendarMethodsRecord calendarRec,
                    TemporalMoveRelativeDateNode moveRelativeDateNode, TemporalDifferenceDateNode differenceDateNode, Node node, InlinedBranchProfile errorBranch) {
        double weeks = weeksP;
        double fractionalDays = daysP;
        JSTemporalPlainDateObject relativeTo = relativeToP;
        JSContext ctx = getLanguage().getJSContext();
        JSRealm realm = getRealm();

        var isoResult = TemporalUtil.addISODate(relativeTo.getYear(), relativeTo.getMonth(), relativeTo.getDay(),
                        0, 0, 0, JSRuntime.truncateDouble(fractionalDays), Overflow.CONSTRAIN);
        var wholeDaysLater = JSTemporalPlainDate.create(ctx, getRealm(), isoResult.year(), isoResult.month(), isoResult.day(), calendarRec.receiver(), node, errorBranch);
        var untilOptions = createUntilOptions(Unit.WEEK);
        var timePassed = differenceDateNode.execute(calendarRec, relativeTo, wholeDaysLater, Unit.WEEK, untilOptions);
        var weeksPassed = timePassed.getWeeks();
        weeks += weeksPassed;

        var weeksPassedDuration = JSTemporalDuration.createTemporalDuration(ctx, realm, 0, 0, weeksPassed, 0, 0, 0, 0, 0, 0, 0, this, errorBranch);
        var moveResult = moveRelativeDateNode.execute(calendarRec, relativeTo, weeksPassedDuration);
        relativeTo = moveResult.relativeTo();
        double daysPassed = moveResult.days();
        fractionalDays -= daysPassed;

        double sign = fractionalDays < 0 ? -1 : 1;
        var oneWeek = JSTemporalDuration.createTemporalDuration(ctx, realm, 0, 0, sign, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
        moveResult = moveRelativeDateNode.execute(calendarRec, relativeTo, oneWeek);
        double oneWeekDays = moveResult.days();
        if (oneWeekDays == 0) {
            errorBranch.enter(node);
            throw Errors.createRangeError("dateAdd of one week moved date by 0 days");
        }

        double fractionalWeeks = weeks + (fractionalDays / Math.abs(oneWeekDays));
        weeks = TemporalUtil.roundNumberToIncrement(fractionalWeeks, increment, roundingMode);
        double remainder = fractionalWeeks - weeks;
        return JSTemporalDurationRecord.createWeeksRemainder(years, months, weeks, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, remainder);
    }

    private JSTemporalDurationRecord getUnitMonth(int increment, TemporalUtil.RoundingMode roundingMode,
                    final double years, final double monthsP, final double weeks, final double daysP,
                    final double hours, final double minutes, final double seconds, final double microseconds, final double milliseconds, final double nanoseconds,
                    JSTemporalPlainDateObject relativeToP, CalendarMethodsRecord calendarRec,
                    TemporalMoveRelativeDateNode moveRelativeDateNode, TemporalDifferenceDateNode differenceDateNode, Node node, InlinedBranchProfile errorBranch) {
        double months = monthsP;
        double fractionalDays = daysP;
        JSTemporalPlainDateObject relativeTo = relativeToP;
        JSContext ctx = getLanguage().getJSContext();
        JSRealm realm = getRealm();

        JSDynamicObject yearsMonths = JSTemporalDuration.createTemporalDuration(ctx, realm, years, months, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
        JSTemporalPlainDateObject yearsMonthsLater = TemporalUtil.calendarDateAdd(calendarRec, relativeTo, yearsMonths, Undefined.instance);
        JSDynamicObject yearsMonthsWeeks = JSTemporalDuration.createTemporalDuration(ctx, realm, years, months, weeks, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
        JSTemporalPlainDateObject yearsMonthsWeeksLater = TemporalUtil.calendarDateAdd(calendarRec, relativeTo, yearsMonthsWeeks, Undefined.instance);
        double weeksInDays = TemporalUtil.daysUntil(yearsMonthsLater, yearsMonthsWeeksLater);
        relativeTo = yearsMonthsLater;
        fractionalDays = fractionalDays + weeksInDays;

        var isoResult = TemporalUtil.addISODate(relativeTo.getYear(), relativeTo.getMonth(), relativeTo.getDay(),
                        0, 0, 0, JSRuntime.truncateDouble(fractionalDays), Overflow.CONSTRAIN);
        var wholeDaysLater = JSTemporalPlainDate.create(ctx, getRealm(), isoResult.year(), isoResult.month(), isoResult.day(), calendarRec.receiver(), node, errorBranch);
        var untilOptions = createUntilOptions(Unit.MONTH);
        var timePassed = differenceDateNode.execute(calendarRec, relativeTo, wholeDaysLater, Unit.MONTH, untilOptions);
        var monthsPassed = timePassed.getMonths();
        months += monthsPassed;

        var monthsPassedDuration = JSTemporalDuration.createTemporalDuration(ctx, realm, 0, monthsPassed, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
        MoveRelativeDateResult moveResult = moveRelativeDateNode.execute(calendarRec, relativeTo, monthsPassedDuration);
        relativeTo = moveResult.relativeTo();
        double daysPassed = moveResult.days();
        fractionalDays -= daysPassed;

        double sign = fractionalDays < 0 ? -1 : 1;
        var oneMonth = JSTemporalDuration.createTemporalDuration(ctx, realm, 0, sign, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
        moveResult = moveRelativeDateNode.execute(calendarRec, relativeTo, oneMonth);
        double oneMonthDays = moveResult.days();
        if (oneMonthDays == 0) {
            errorBranch.enter(node);
            throw Errors.createRangeError("dateAdd of one month moved date by 0 days");
        }

        double fractionalMonths = months + (fractionalDays / Math.abs(oneMonthDays));
        months = TemporalUtil.roundNumberToIncrement(fractionalMonths, increment, roundingMode);
        double remainder = fractionalMonths - months;
        return JSTemporalDurationRecord.createWeeksRemainder(years, months, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, remainder);
    }

    @TruffleBoundary
    private static JSTemporalDurationRecord getUnitSecond(int increment, TemporalUtil.RoundingMode roundingMode,
                    double years, double months, double weeks, double days,
                    double hours, double minutes, double fractionalSeconds) {
        double seconds = TemporalUtil.roundNumberToIncrement(fractionalSeconds, increment, roundingMode);
        double remainder = fractionalSeconds - seconds;
        return JSTemporalDurationRecord.createWeeksRemainder(years, months, weeks, days, hours, minutes, seconds, 0, 0, 0, remainder);
    }

    private JSTemporalDurationRecord getUnitYear(final int increment, TemporalUtil.RoundingMode roundingMode,
                    final double yearsP, final double months, final double weeks, final double daysP,
                    final double hours, final double minutes, final double seconds, final double microseconds, final double milliseconds, final double nanoseconds,
                    JSTemporalPlainDateObject relativeToP, CalendarMethodsRecord calendarRec,
                    TemporalMoveRelativeDateNode moveRelativeDateNode, TemporalDifferenceDateNode differenceDateNode, Node node, InlinedBranchProfile errorBranch) {
        double years = yearsP;
        double fractionalDays = daysP;
        JSTemporalPlainDateObject relativeTo = relativeToP;
        JSContext ctx = getLanguage().getJSContext();
        JSRealm realm = getRealm();

        var yearsDuration = JSTemporalDuration.createTemporalDuration(ctx, realm, years, 0, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
        var yearsLater = TemporalUtil.calendarDateAdd(calendarRec, relativeTo, yearsDuration);
        var yearsMonthsWeeks = JSTemporalDuration.createTemporalDuration(ctx, realm, years, months, weeks, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
        var yearsMonthsWeeksLater = TemporalUtil.calendarDateAdd(calendarRec, relativeTo, yearsMonthsWeeks);
        double monthsWeeksInDays = TemporalUtil.daysUntil(yearsLater, yearsMonthsWeeksLater);
        relativeTo = yearsLater;
        fractionalDays += monthsWeeksInDays;

        var isoResult = TemporalUtil.addISODate(relativeTo.getYear(), relativeTo.getMonth(), relativeTo.getDay(),
                        0, 0, 0, JSRuntime.truncateDouble(fractionalDays), Overflow.CONSTRAIN);
        var wholeDaysLater = JSTemporalPlainDate.create(ctx, getRealm(), isoResult.year(), isoResult.month(), isoResult.day(), calendarRec.receiver(), node, errorBranch);
        var untilOptions = createUntilOptions(Unit.YEAR);
        var timePassed = differenceDateNode.execute(calendarRec, relativeTo, wholeDaysLater, Unit.YEAR, untilOptions);
        var yearsPassed = timePassed.getYears();
        years += yearsPassed;

        var yearsPassedDuration = JSTemporalDuration.createTemporalDuration(ctx, realm, yearsPassed, 0, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
        MoveRelativeDateResult moveResult = moveRelativeDateNode.execute(calendarRec, relativeTo, yearsPassedDuration);
        relativeTo = moveResult.relativeTo();
        double daysPassed = moveResult.days();
        fractionalDays -= daysPassed;

        double sign = fractionalDays < 0 ? -1 : 1;
        var oneYear = JSTemporalDuration.createTemporalDuration(ctx, realm, sign, 0, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
        moveResult = moveRelativeDateNode.execute(calendarRec, relativeTo, oneYear);
        double oneYearDays = moveResult.days();
        if (oneYearDays == 0) {
            errorBranch.enter(node);
            throw Errors.createRangeError("dateAdd of one year moved date by 0 days");
        }

        double fractionalYears = years + (fractionalDays / Math.abs(oneYearDays));
        years = TemporalUtil.roundNumberToIncrement(fractionalYears, increment, roundingMode);
        double remainder = fractionalYears - years;
        return JSTemporalDurationRecord.createWeeksRemainder(years, 0, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, remainder);
    }

    private JSObject createUntilOptions(Unit largestUnit) {
        JSContext ctx = getLanguage().getJSContext();
        JSObject untilOptions = JSOrdinary.createWithNullPrototype(ctx);
        TemporalUtil.createDataPropertyOrThrow(ctx, untilOptions, LARGEST_UNIT, largestUnit.toTruffleString());
        return untilOptions;
    }

    @TruffleBoundary
    private static double calculateDays(double days, NormalizedTimeDurationToDaysResult result) {
        return days + TemporalUtil.bitod(result.days().add(result.remainder().divide(result.dayLength().abs())));
    }
}
