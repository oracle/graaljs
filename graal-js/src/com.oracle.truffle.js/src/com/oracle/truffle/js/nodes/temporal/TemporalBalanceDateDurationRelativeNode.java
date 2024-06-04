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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.CalendarMethodsRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.DateDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

/**
 * Implementation of the Temporal balanceDurationRelative operation.
 */
@ImportStatic(TemporalConstants.class)
public abstract class TemporalBalanceDateDurationRelativeNode extends JavaScriptBaseNode {

    @Child private JSFunctionCallNode callDateAddNode;
    @Child private JSFunctionCallNode callDateUntilNode;

    protected TemporalBalanceDateDurationRelativeNode() {
    }

    public abstract DateDurationRecord execute(double years, double months, double weeks, double days,
                    TemporalUtil.Unit largestUnit, TemporalUtil.Unit smallestUnit, JSTemporalPlainDateObject plainRelativeTo, CalendarMethodsRecord calendarRec);

    @Specialization
    protected DateDurationRecord balanceDurationRelative(double years, double months, double weeks, double days,
                    Unit largestUnit, Unit smallestUnit, JSTemporalPlainDateObject plainRelativeTo, CalendarMethodsRecord calendarRec,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached InlinedBranchProfile unitIsYear,
                    @Cached InlinedBranchProfile unitIsMonth,
                    @Cached InlinedBranchProfile unitIsWeek,
                    @Cached InlinedConditionProfile unitIsDay,
                    @Cached("create(getLanguage().getJSContext(), LARGEST_UNIT)") CreateDataPropertyNode createLargestUnitProperty) {
        boolean allZero = (years == 0 && months == 0 && weeks == 0 && days == 0);
        if (unitIsDay.profile(this, (largestUnit != TemporalUtil.Unit.YEAR && largestUnit != TemporalUtil.Unit.MONTH && largestUnit != TemporalUtil.Unit.WEEK) || allZero)) {
            return new DateDurationRecord(years, months, weeks, days);
        }
        if (plainRelativeTo == null) {
            errorBranch.enter(this);
            throw TemporalErrors.createRangeErrorRelativeToNotUndefined();
        }

        JSContext ctx = getLanguage().getJSContext();
        JSRealm realm = getRealm();
        JSObject untilOptions = JSOrdinary.createWithNullPrototype(ctx);
        createLargestUnitProperty.executeVoid(untilOptions, largestUnit.toTruffleString());

        switch (largestUnit) {
            case YEAR -> {
                unitIsYear.enter(this);
                return getUnitYear(years, months, weeks, days, smallestUnit, plainRelativeTo, calendarRec, untilOptions,
                                this, errorBranch, ctx, realm);
            }
            case MONTH -> {
                unitIsMonth.enter(this);
                return getUnitMonth(years, months, weeks, days, smallestUnit, plainRelativeTo, calendarRec, untilOptions,
                                this, errorBranch, ctx, realm);
            }
            case WEEK -> {
                unitIsWeek.enter(this);
                return getUnitWeek(years, months, weeks, days, plainRelativeTo, calendarRec, untilOptions,
                                this, errorBranch, ctx, realm);
            }
            default -> throw Errors.shouldNotReachHereUnexpectedValue(largestUnit);
        }
    }

    private DateDurationRecord getUnitYear(double years, double months, double weeks, double days, Unit smallestUnit,
                    JSTemporalPlainDateObject plainRelativeTo, CalendarMethodsRecord calendarRec, JSObject untilOptions,
                    Node node, InlinedBranchProfile errorBranch, JSContext ctx, JSRealm realm) {
        if (smallestUnit == Unit.WEEK) {
            assert days == 0 : days;
            var yearsMonthsDuration = JSTemporalDuration.createTemporalDuration(ctx, realm,
                            years, months, 0, 0, 0, 0, 0, 0, 0, 0, this, errorBranch);
            var later = calendarDateAdd(calendarRec, plainRelativeTo, yearsMonthsDuration, node, errorBranch, ctx, realm);
            var untilResult = calendarDateUntil(calendarRec, plainRelativeTo, later, untilOptions, ctx, realm);
            return new DateDurationRecord(untilResult.getYears(), untilResult.getMonths(), weeks, days);
        } else {
            var yearsMonthsWeeksDaysDuration = JSTemporalDuration.createTemporalDuration(ctx, realm,
                            years, months, weeks, days, 0, 0, 0, 0, 0, 0, this, errorBranch);
            var later = calendarDateAdd(calendarRec, plainRelativeTo, yearsMonthsWeeksDaysDuration, node, errorBranch, ctx, realm);
            var untilResult = calendarDateUntil(calendarRec, plainRelativeTo, later, untilOptions, ctx, realm);
            return new DateDurationRecord(untilResult.getYears(), untilResult.getMonths(), untilResult.getWeeks(), untilResult.getDays());
        }
    }

    private DateDurationRecord getUnitMonth(double years, double months, double weeks, double days, Unit smallestUnit,
                    JSTemporalPlainDateObject plainRelativeTo, CalendarMethodsRecord calendarRec, JSObject untilOptions,
                    Node node, InlinedBranchProfile errorBranch, JSContext ctx, JSRealm realm) {
        assert years == 0 : years;
        if (smallestUnit == Unit.WEEK) {
            assert days == 0 : days;
            return new DateDurationRecord(years, months, weeks, days);
        } else {
            var monthsWeeksDaysDuration = JSTemporalDuration.createTemporalDuration(ctx, realm,
                            years, months, weeks, days, 0, 0, 0, 0, 0, 0, this, errorBranch);
            var later = calendarDateAdd(calendarRec, plainRelativeTo, monthsWeeksDaysDuration, node, errorBranch, ctx, realm);
            var untilResult = calendarDateUntil(calendarRec, plainRelativeTo, later, untilOptions, ctx, realm);
            return new DateDurationRecord(years, untilResult.getMonths(), untilResult.getWeeks(), untilResult.getDays());
        }
    }

    private DateDurationRecord getUnitWeek(double years, double months, double weeks, double days,
                    JSTemporalPlainDateObject plainRelativeTo, CalendarMethodsRecord calendarRec, JSObject untilOptions,
                    Node node, InlinedBranchProfile errorBranch, JSContext ctx, JSRealm realm) {
        assert years == 0 : years;
        assert months == 0 : months;
        var weeksDaysDuration = JSTemporalDuration.createTemporalDuration(ctx, realm,
                        years, months, weeks, days, 0, 0, 0, 0, 0, 0, this, errorBranch);
        var later = calendarDateAdd(calendarRec, plainRelativeTo, weeksDaysDuration, node, errorBranch, ctx, realm);
        var untilResult = calendarDateUntil(calendarRec, plainRelativeTo, later, untilOptions, ctx, realm);
        return new DateDurationRecord(years, months, untilResult.getWeeks(), untilResult.getDays());
    }

    protected JSTemporalPlainDateObject calendarDateAdd(CalendarMethodsRecord calendarRec, JSTemporalPlainDateObject plainDate, JSTemporalDurationObject duration,
                    Node node, InlinedBranchProfile errorBranch, JSContext ctx, JSRealm realm) {
        if (callDateAddNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callDateAddNode = insert(JSFunctionCallNode.createCall());
        }
        Object calendar = toCalendarObject(calendarRec.receiver(), ctx, realm);
        Object addedDate = callDateAddNode.executeCall(JSArguments.create(calendar, calendarRec.dateAdd(), plainDate, duration));
        return TemporalUtil.requireTemporalDate(addedDate, node, errorBranch);
    }

    protected JSTemporalDurationObject calendarDateUntil(CalendarMethodsRecord calendarRec, JSTemporalPlainDateObject one, JSTemporalPlainDateObject two, JSDynamicObject options,
                    JSContext ctx, JSRealm realm) {
        if (callDateUntilNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callDateUntilNode = insert(JSFunctionCallNode.createCall());
        }
        Object calendar = toCalendarObject(calendarRec.receiver(), ctx, realm);
        Object addedDate = callDateUntilNode.executeCall(JSArguments.create(calendar, calendarRec.dateUntil(), one, two, options));
        return TemporalUtil.requireTemporalDuration(addedDate);
    }

    private static Object toCalendarObject(Object calendarSlotValue, JSContext ctx, JSRealm realm) {
        Object calendar;
        if (calendarSlotValue instanceof TruffleString calendarID) {
            calendar = JSTemporalCalendar.create(ctx, realm, calendarID);
        } else {
            calendar = calendarSlotValue;
        }
        return calendar;
    }
}
