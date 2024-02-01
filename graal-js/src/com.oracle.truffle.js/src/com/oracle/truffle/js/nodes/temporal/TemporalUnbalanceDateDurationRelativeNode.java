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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.CalendarMethodsRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of the Temporal UnbalanceDateDurationRelative operation.
 */
public abstract class TemporalUnbalanceDateDurationRelativeNode extends JavaScriptBaseNode {

    @Child private ToTemporalCalendarObjectNode toCalendarObjectNode;
    @Child private JSFunctionCallNode callDateAddNode;
    @Child private JSFunctionCallNode callDateUntilNode;

    protected TemporalUnbalanceDateDurationRelativeNode() {
    }

    public abstract JSTemporalDurationRecord execute(double year, double month, double week, double day, TemporalUtil.Unit largestUnit,
                    JSTemporalPlainDateObject plainRelativeTo, CalendarMethodsRecord calendarRec);

    @Specialization
    protected JSTemporalDurationRecord unbalanceDurationRelative(double years, double months, double weeks, double days, TemporalUtil.Unit largestUnit,
                    JSTemporalPlainDateObject plainRelativeTo, CalendarMethodsRecord calendarRec,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached InlinedConditionProfile unitIsYear,
                    @Cached InlinedConditionProfile unitIsWeek,
                    @Cached InlinedConditionProfile unitIsMonth) {
        assert plainRelativeTo == null || calendarRec != null;

        if (unitIsYear.profile(this, TemporalUtil.Unit.YEAR == largestUnit)) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        }

        JSContext ctx = getLanguage().getJSContext();
        JSRealm realm = getRealm();
        if (unitIsMonth.profile(this, TemporalUtil.Unit.MONTH == largestUnit)) {
            return unitIsMonth(ctx, realm, years, months, weeks, days, plainRelativeTo, calendarRec, this, errorBranch);
        } else if (unitIsWeek.profile(this, TemporalUtil.Unit.WEEK == largestUnit)) {
            return unitIsWeek(ctx, realm, years, months, weeks, days, plainRelativeTo, calendarRec, this, errorBranch);
        } else {
            return unitIsDay(ctx, realm, years, months, weeks, days, plainRelativeTo, calendarRec, this, errorBranch);
        }
    }

    private JSTemporalDurationRecord unitIsDay(JSContext ctx, JSRealm realm, double years, double months, double weeks, double days, JSTemporalPlainDateObject plainRelativeTo,
                    CalendarMethodsRecord calendarRec, Node node, InlinedBranchProfile errorBranch) {
        if (years == 0 && months == 0 && weeks == 0) {
            return JSTemporalDurationRecord.createWeeks(0, 0, 0, days, 0, 0, 0, 0, 0, 0);
        }

        checkCalendar(calendarRec, this, errorBranch);
        assert calendarRec.dateAdd() != null;

        var yearsMonthsWeeksDuration = JSTemporalDuration.createTemporalDuration(ctx, realm, years, months, weeks, 0, 0, 0, 0, 0, 0, 0, this, errorBranch);
        JSTemporalPlainDateObject later = calendarDateAdd(calendarRec, plainRelativeTo, yearsMonthsWeeksDuration, node, errorBranch);
        double yearsMonthsWeeksInDays = TemporalUtil.daysUntil(plainRelativeTo, later);

        return JSTemporalDurationRecord.createWeeks(0, 0, 0, days + yearsMonthsWeeksInDays, 0, 0, 0, 0, 0, 0);
    }

    private JSTemporalDurationRecord unitIsWeek(JSContext ctx, JSRealm realm, double years, double months, double weeks, double days, JSTemporalPlainDateObject plainRelativeTo,
                    CalendarMethodsRecord calendarRec, Node node, InlinedBranchProfile errorBranch) {
        if (years == 0 && months == 0) {
            return JSTemporalDurationRecord.createWeeks(0, 0, weeks, days, 0, 0, 0, 0, 0, 0);
        }

        checkCalendar(calendarRec, this, errorBranch);
        assert calendarRec.dateAdd() != null;

        var yearsMonthsDuration = JSTemporalDuration.createTemporalDuration(ctx, realm, years, months, 0, 0, 0, 0, 0, 0, 0, 0, this, errorBranch);
        JSTemporalPlainDateObject later = calendarDateAdd(calendarRec, plainRelativeTo, yearsMonthsDuration, node, errorBranch);
        double yearsMonthsInDays = TemporalUtil.daysUntil(plainRelativeTo, later);

        return JSTemporalDurationRecord.createWeeks(0, 0, weeks, days + yearsMonthsInDays, 0, 0, 0, 0, 0, 0);
    }

    private JSTemporalDurationRecord unitIsMonth(JSContext ctx, JSRealm realm, double years, double months, double weeks, double days, JSTemporalPlainDateObject plainRelativeTo,
                    CalendarMethodsRecord calendarRec, Node node, InlinedBranchProfile errorBranch) {
        if (years == 0) {
            return JSTemporalDurationRecord.createWeeks(0, months, weeks, days, 0, 0, 0, 0, 0, 0);
        }

        checkCalendar(calendarRec, this, errorBranch);
        assert calendarRec.dateAdd() != null && calendarRec.dateUntil() != null;

        var yearsDuration = JSTemporalDuration.createTemporalDuration(ctx, realm, years, 0, 0, 0, 0, 0, 0, 0, 0, 0, this, errorBranch);
        JSTemporalPlainDateObject later = calendarDateAdd(calendarRec, plainRelativeTo, yearsDuration, node, errorBranch);
        JSDynamicObject untilOptions = JSOrdinary.createWithNullPrototype(ctx);
        JSObjectUtil.putDataProperty(untilOptions, LARGEST_UNIT, MONTH);
        JSTemporalDurationObject untilResult = calendarDateUntil(calendarRec, plainRelativeTo, later, untilOptions);
        double yearsInMonths = untilResult.getMonths();

        return JSTemporalDurationRecord.createWeeks(0, months + yearsInMonths, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    protected JSTemporalPlainDateObject calendarDateAdd(CalendarMethodsRecord calendarRec, JSDynamicObject date, JSDynamicObject duration,
                    Node node, InlinedBranchProfile errorBranch) {
        if (callDateAddNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callDateAddNode = insert(JSFunctionCallNode.createCall());
        }
        Object calendar = toCalendarObject(calendarRec);
        Object addedDate = callDateAddNode.executeCall(JSArguments.create(calendar, calendarRec.dateAdd(), date, duration));
        return TemporalUtil.requireTemporalDate(addedDate, node, errorBranch);
    }

    protected JSTemporalDurationObject calendarDateUntil(CalendarMethodsRecord calendarRec, JSDynamicObject date, JSDynamicObject duration, JSDynamicObject options) {
        if (callDateUntilNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callDateUntilNode = insert(JSFunctionCallNode.createCall());
        }
        Object calendar = toCalendarObject(calendarRec);
        Object addedDate = callDateUntilNode.executeCall(JSArguments.create(calendar, calendarRec.dateUntil(), date, duration, options));
        return TemporalUtil.requireTemporalDuration(addedDate);
    }

    private Object toCalendarObject(CalendarMethodsRecord calendarRec) {
        if (toCalendarObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toCalendarObjectNode = insert(ToTemporalCalendarObjectNode.create());
        }
        return toCalendarObjectNode.execute(calendarRec.receiver());
    }

    private static void checkCalendar(CalendarMethodsRecord calendarRec, Node node, InlinedBranchProfile errorBranch) {
        if (calendarRec == null) {
            errorBranch.enter(node);
            throw Errors.createRangeError("Calendar should not be undefined.");
        }
    }

}
