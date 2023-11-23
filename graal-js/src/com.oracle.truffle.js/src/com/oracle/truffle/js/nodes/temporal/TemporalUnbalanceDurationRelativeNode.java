/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtol;

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
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.MoveRelativeDateResult;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of the Temporal UnbalanceDateDurationRelative operation.
 */
public abstract class TemporalUnbalanceDurationRelativeNode extends JavaScriptBaseNode {

    @Child private JSFunctionCallNode callDateAddNode;
    @Child private JSFunctionCallNode callDateUntilNode;

    protected TemporalUnbalanceDurationRelativeNode() {
    }

    public abstract JSTemporalDurationRecord execute(double year, double month, double week, double day, TemporalUtil.Unit largestUnit,
                    JSTemporalPlainDateObject plainRelativeTo, Object dateAdd, Object dateUntil);

    // TODO still using (some) long arithmetics here, should use double?
    @Specialization
    protected JSTemporalDurationRecord unbalanceDurationRelative(double y, double m, double w, double d, TemporalUtil.Unit largestUnit,
                    JSTemporalPlainDateObject plainRelativeTo, Object dateAdd, Object dateUntil,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached InlinedConditionProfile unitIsYear,
                    @Cached InlinedConditionProfile unitIsWeek,
                    @Cached InlinedConditionProfile unitIsMonth,
                    @Cached InlinedConditionProfile relativeToAvailable,
                    @Cached TemporalMoveRelativeDateNode moveRelativeDateNode) {
        long years = dtol(y);
        long months = dtol(m);
        long weeks = dtol(w);
        long days = dtol(d);

        if (unitIsYear.profile(this, TemporalUtil.Unit.YEAR == largestUnit || (years == 0 && months == 0 && weeks == 0 && days == 0))) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        }
        long sign = TemporalUtil.durationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        assert sign != 0;
        JSContext ctx = getLanguage().getJSContext();
        JSRealm realm = getRealm();
        var oneYear = JSTemporalDuration.createTemporalDuration(ctx, realm, sign, 0, 0, 0, 0, 0, 0, 0, 0, 0, this, errorBranch);
        var oneMonth = JSTemporalDuration.createTemporalDuration(ctx, realm, 0, sign, 0, 0, 0, 0, 0, 0, 0, 0, this, errorBranch);
        var oneWeek = JSTemporalDuration.createTemporalDuration(ctx, realm, 0, 0, sign, 0, 0, 0, 0, 0, 0, 0, this, errorBranch);
        JSDynamicObject calendar = Undefined.instance;
        if (relativeToAvailable.profile(this, plainRelativeTo != null)) {
            calendar = plainRelativeTo.getCalendar();
        }
        if (unitIsMonth.profile(this, TemporalUtil.Unit.MONTH == largestUnit)) {
            return unitIsMonth(years, months, weeks, days, plainRelativeTo, sign, oneYear, calendar, dateAdd, dateUntil, this, errorBranch);
        } else if (unitIsWeek.profile(this, TemporalUtil.Unit.WEEK == largestUnit)) {
            return unitIsWeek(years, months, weeks, days, plainRelativeTo, sign, oneYear, oneMonth, calendar, dateAdd, this, errorBranch, moveRelativeDateNode);
        } else {
            return unitIsDay(years, months, weeks, days, plainRelativeTo, sign, oneYear, oneMonth, oneWeek, calendar, dateAdd, this, errorBranch, moveRelativeDateNode);
        }
    }

    private static JSTemporalDurationRecord unitIsDay(long yearsP, long monthsP, long weeksP, long daysP, JSTemporalPlainDateObject relativeToP, long sign,
                    JSTemporalDurationObject oneYear, JSTemporalDurationObject oneMonth, JSTemporalDurationObject oneWeek,
                    JSDynamicObject calendar, Object dateAdd, Node node, InlinedBranchProfile errorBranch, TemporalMoveRelativeDateNode moveRelativeDateNode) {
        long years = yearsP;
        long months = monthsP;
        long weeks = weeksP;
        long days = daysP;
        JSTemporalPlainDateObject relativeTo = relativeToP;
        if (years != 0 || months != 0 || weeks != 0) {
            if (calendar == Undefined.instance) {
                errorBranch.enter(node);
                throw Errors.createRangeError("Calendar should not be undefined.");
            }
            while (Math.abs(years) > 0) {
                MoveRelativeDateResult moveResult = moveRelativeDateNode.execute(calendar, dateAdd, relativeTo, oneYear);
                relativeTo = moveResult.relativeTo();
                long oneYearDays = moveResult.days();
                years = years - sign;
                days = days + oneYearDays;
            }
            while (Math.abs(months) > 0) {
                MoveRelativeDateResult moveResult = moveRelativeDateNode.execute(calendar, dateAdd, relativeTo, oneMonth);
                relativeTo = moveResult.relativeTo();
                long oneMonthDays = moveResult.days();
                months = months - sign;
                days = days + oneMonthDays;
            }
            while (Math.abs(weeks) > 0) {
                MoveRelativeDateResult moveResult = moveRelativeDateNode.execute(calendar, dateAdd, relativeTo, oneWeek);
                relativeTo = moveResult.relativeTo();
                long oneWeekDays = moveResult.days();
                weeks = weeks - sign;
                days = days + oneWeekDays;
            }
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    private static JSTemporalDurationRecord unitIsWeek(long yearsP, long monthsP, long weeks, long daysP, JSTemporalPlainDateObject relativeToP, long sign,
                    JSTemporalDurationObject oneYear, JSTemporalDurationObject oneMonth,
                    JSDynamicObject calendar, Object dateAdd, Node node, InlinedBranchProfile errorBranch, TemporalMoveRelativeDateNode moveRelativeDateNode) {
        long years = yearsP;
        long months = monthsP;
        long days = daysP;
        JSTemporalPlainDateObject relativeTo = relativeToP;
        if (calendar == Undefined.instance) {
            errorBranch.enter(node);
            throw Errors.createRangeError("Calendar should not be undefined.");
        }
        while (Math.abs(years) > 0) {
            MoveRelativeDateResult moveResult = moveRelativeDateNode.execute(calendar, dateAdd, relativeTo, oneYear);
            relativeTo = moveResult.relativeTo();
            long oneYearDays = moveResult.days();
            years = years - sign;
            days = days + oneYearDays;
        }
        while (Math.abs(months) > 0) {
            MoveRelativeDateResult moveResult = moveRelativeDateNode.execute(calendar, dateAdd, relativeTo, oneMonth);
            relativeTo = moveResult.relativeTo();
            long oneMonthDays = moveResult.days();
            months = months - sign;
            days = days + oneMonthDays;
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    private JSTemporalDurationRecord unitIsMonth(long yearsP, long monthsP, long weeks, long days, JSTemporalPlainDateObject relativeToP, long sign, JSDynamicObject oneYear,
                    JSDynamicObject calendar, Object dateAdd, Object dateUntil, Node node, InlinedBranchProfile errorBranch) {
        long years = yearsP;
        long months = monthsP;
        JSTemporalPlainDateObject relativeTo = relativeToP;
        if (calendar == Undefined.instance) {
            errorBranch.enter(node);
            throw Errors.createRangeError("No calendar provided.");
        }
        JSContext ctx = getLanguage().getJSContext();
        while (Math.abs(years) > 0) {
            JSDynamicObject addOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSTemporalPlainDateObject newRelativeTo = calendarDateAdd(calendar, relativeTo, oneYear, addOptions, dateAdd, node, errorBranch);

            JSDynamicObject untilOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(untilOptions, LARGEST_UNIT, MONTH);
            JSTemporalDurationObject untilResult = calendarDateUntil(calendar, relativeTo, newRelativeTo, untilOptions, dateUntil);
            long oneYearMonths = dtol(untilResult.getMonths());
            relativeTo = newRelativeTo;
            years = years - sign;
            months = months + oneYearMonths;
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    protected JSTemporalPlainDateObject calendarDateAdd(JSDynamicObject calendar, JSDynamicObject date, JSDynamicObject duration, JSDynamicObject options, Object dateAdd,
                    Node node, InlinedBranchProfile errorBranch) {
        if (callDateAddNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callDateAddNode = insert(JSFunctionCallNode.createCall());
        }
        Object addedDate = callDateAddNode.executeCall(JSArguments.create(calendar, dateAdd, date, duration, options));
        return TemporalUtil.requireTemporalDate(addedDate, node, errorBranch);
    }

    protected JSTemporalDurationObject calendarDateUntil(JSDynamicObject calendar, JSDynamicObject date, JSDynamicObject duration, JSDynamicObject options, Object dateUntil) {
        if (callDateUntilNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callDateUntilNode = insert(JSFunctionCallNode.createCall());
        }
        Object addedDate = callDateUntilNode.executeCall(JSArguments.create(calendar, dateUntil, date, duration, options));
        return TemporalUtil.requireTemporalDuration(addedDate);
    }
}
