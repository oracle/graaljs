/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.LARGEST_UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtol;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalRelativeDateRecord;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of the Temporal unbalanceDurationRelative operation.
 */
public abstract class TemporalUnbalanceDurationRelativeNode extends JavaScriptBaseNode {

    protected final JSContext ctx;
    private final BranchProfile errorBranch = BranchProfile.create();
    @Child private PropertyGetNode getCalendarNode;
    @Child private GetMethodNode getMethodDateAddNode;
    @Child private JSFunctionCallNode callDateAddNode;
    @Child private GetMethodNode getMethodDateUntilNode;
    @Child private JSFunctionCallNode callDateUntilNode;
    @Child private TemporalMoveRelativeDateNode moveRelativeDateNode;

    protected TemporalUnbalanceDurationRelativeNode(JSContext ctx) {
        this.ctx = ctx;
    }

    public static TemporalUnbalanceDurationRelativeNode create(JSContext ctx) {
        return TemporalUnbalanceDurationRelativeNodeGen.create(ctx);
    }

    public abstract JSTemporalDurationRecord execute(double year, double month, double week, double day, TemporalUtil.Unit largestUnit, DynamicObject relTo);

    // TODO still using (some) long arithmetics here, should use double?
    @Specialization
    protected JSTemporalDurationRecord unbalanceDurationRelative(double y, double m, double w, double d, TemporalUtil.Unit largestUnit, DynamicObject relTo,
                    @Cached ConditionProfile unitIsYear,
                    @Cached ConditionProfile unitIsWeek,
                    @Cached ConditionProfile unitIsMonth,
                    @Cached ConditionProfile relativeToAvailable,
                    @Cached("create(ctx)") ToTemporalDateNode toTemporalDateNode) {
        long years = dtol(y);
        long months = dtol(m);
        long weeks = dtol(w);
        long days = dtol(d);

        DynamicObject relativeTo = relTo;
        if (unitIsYear.profile(TemporalUtil.Unit.YEAR == largestUnit || (years == 0 && months == 0 && weeks == 0 && days == 0))) {
            return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        }
        long sign = TemporalUtil.durationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        assert sign != 0;
        DynamicObject oneYear = JSTemporalDuration.createTemporalDuration(ctx, sign, 0, 0, 0, 0, 0, 0, 0, 0, 0, errorBranch);
        DynamicObject oneMonth = JSTemporalDuration.createTemporalDuration(ctx, 0, sign, 0, 0, 0, 0, 0, 0, 0, 0, errorBranch);
        DynamicObject oneWeek = JSTemporalDuration.createTemporalDuration(ctx, 0, 0, sign, 0, 0, 0, 0, 0, 0, 0, errorBranch);
        DynamicObject calendar = Undefined.instance;
        if (relativeToAvailable.profile(relativeTo != Undefined.instance)) {
            relativeTo = toTemporalDateNode.executeDynamicObject(relativeTo, Undefined.instance);
            calendar = getCalendar(relativeTo);
        }
        if (unitIsMonth.profile(TemporalUtil.Unit.MONTH == largestUnit)) {
            return unitIsMonth(years, months, weeks, days, relativeTo, sign, oneYear, calendar);
        } else if (unitIsWeek.profile(TemporalUtil.Unit.WEEK == largestUnit)) {
            return unitIsWeek(years, months, weeks, days, relativeTo, sign, oneYear, oneMonth, calendar);
        } else {
            return unitIsDay(years, months, weeks, days, relativeTo, sign, oneYear, oneMonth, oneWeek, calendar);
        }
    }

    private JSTemporalDurationRecord unitIsDay(long yearsP, long monthsP, long weeksP, long daysP, DynamicObject relativeToP, long sign, DynamicObject oneYear,
                    DynamicObject oneMonth, DynamicObject oneWeek, DynamicObject calendar) {
        long years = yearsP;
        long months = monthsP;
        long weeks = weeksP;
        long days = daysP;
        DynamicObject relativeTo = relativeToP;
        if (years != 0 || months != 0 || weeks != 0) {
            if (calendar == Undefined.instance) {
                errorBranch.enter();
                throw Errors.createRangeError("Calendar should not be undefined.");
            }
            while (Math.abs(years) > 0) {
                JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneYear);
                relativeTo = moveResult.getRelativeTo();
                long oneYearDays = moveResult.getDays();
                years = years - sign;
                days = days + oneYearDays;
            }
            while (Math.abs(months) > 0) {
                JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneMonth);
                relativeTo = moveResult.getRelativeTo();
                long oneMonthDays = moveResult.getDays();
                months = months - sign;
                days = days + oneMonthDays;
            }
            while (Math.abs(weeks) > 0) {
                JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneWeek);
                relativeTo = moveResult.getRelativeTo();
                long oneWeekDays = moveResult.getDays();
                weeks = weeks - sign;
                days = days + oneWeekDays;
            }
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    private JSTemporalDurationRecord unitIsWeek(long yearsP, long monthsP, long weeks, long daysP, DynamicObject relativeToP, long sign, DynamicObject oneYear,
                    DynamicObject oneMonth, DynamicObject calendar) {
        long years = yearsP;
        long months = monthsP;
        long days = daysP;
        DynamicObject relativeTo = relativeToP;
        if (calendar == Undefined.instance) {
            errorBranch.enter();
            throw Errors.createRangeError("Calendar should not be undefined.");
        }
        while (Math.abs(years) > 0) {
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneYear);
            relativeTo = moveResult.getRelativeTo();
            long oneYearDays = moveResult.getDays();
            years = years - sign;
            days = days + oneYearDays;
        }
        while (Math.abs(months) > 0) {
            JSTemporalRelativeDateRecord moveResult = moveRelativeDate(calendar, relativeTo, oneMonth);
            relativeTo = moveResult.getRelativeTo();
            long oneMonthDays = moveResult.getDays();
            months = months - sign;
            days = days + oneMonthDays;
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    private JSTemporalDurationRecord unitIsMonth(long yearsP, long monthsP, long weeks, long days, DynamicObject relativeToP, long sign, DynamicObject oneYear,
                    DynamicObject calendar) {
        long years = yearsP;
        long months = monthsP;
        DynamicObject relativeTo = relativeToP;
        if (calendar == Undefined.instance) {
            errorBranch.enter();
            throw Errors.createRangeError("No calendar provided.");
        }
        if (getMethodDateAddNode == null || getMethodDateUntilNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getMethodDateAddNode = insert(GetMethodNode.create(ctx, TemporalConstants.DATE_ADD));
            getMethodDateUntilNode = insert(GetMethodNode.create(ctx, TemporalConstants.DATE_UNTIL));
        }
        Object dateAdd = getMethodDateAddNode.executeWithTarget(calendar);
        Object dateUntil = getMethodDateUntilNode.executeWithTarget(calendar);

        while (Math.abs(years) > 0) {
            DynamicObject addOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject newRelativeTo = calendarDateAdd(calendar, relativeTo, oneYear, addOptions, dateAdd);

            DynamicObject untilOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(ctx, untilOptions, LARGEST_UNIT, MONTH);
            JSTemporalDurationObject untilResult = calendarDateUntil(calendar, relativeTo, newRelativeTo, untilOptions, dateUntil);
            long oneYearMonths = dtol(untilResult.getMonths());
            relativeTo = newRelativeTo;
            years = years - sign;
            months = months + oneYearMonths;
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    private DynamicObject getCalendar(DynamicObject obj) {
        if (getCalendarNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.getCalendarNode = insert(PropertyGetNode.create(CALENDAR, ctx));
        }
        return TemporalUtil.toDynamicObject(getCalendarNode.getValue(obj));
    }

    protected JSTemporalPlainDateObject calendarDateAdd(DynamicObject calendar, DynamicObject date, DynamicObject duration, DynamicObject options, Object dateAdd) {
        if (callDateAddNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callDateAddNode = insert(JSFunctionCallNode.createCall());
        }
        Object addedDate = callDateAddNode.executeCall(JSArguments.create(calendar, dateAdd, date, duration, options));
        return TemporalUtil.requireTemporalDate(addedDate);
    }

    protected JSTemporalDurationObject calendarDateUntil(DynamicObject calendar, DynamicObject date, DynamicObject duration, DynamicObject options, Object dateUntil) {
        if (callDateUntilNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callDateUntilNode = insert(JSFunctionCallNode.createCall());
        }
        Object addedDate = callDateUntilNode.executeCall(JSArguments.create(calendar, dateUntil, date, duration, options));
        return TemporalUtil.requireTemporalDuration(addedDate);
    }

    private JSTemporalRelativeDateRecord moveRelativeDate(DynamicObject calendar, DynamicObject relativeTo, DynamicObject oneMonth) {
        if (moveRelativeDateNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            moveRelativeDateNode = insert(TemporalMoveRelativeDateNode.create(ctx));
        }
        return moveRelativeDateNode.execute(calendar, relativeTo, oneMonth);
    }
}
