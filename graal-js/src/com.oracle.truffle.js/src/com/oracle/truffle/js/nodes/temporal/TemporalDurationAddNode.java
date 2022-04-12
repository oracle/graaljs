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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.LARGEST_UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.doubleIsInteger;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtol;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of the Temporal addDuration operation.
 */
public abstract class TemporalDurationAddNode extends JavaScriptBaseNode {

    protected final JSContext ctx;
    @Child private GetMethodNode getMethodDateAddNode;
    @Child private JSFunctionCallNode callDateAddNode;
    @Child private GetMethodNode getMethodDateUntilNode;
    @Child private JSFunctionCallNode callDateUntilNode;
    @Child EnumerableOwnPropertyNamesNode namesNode;
    private final BranchProfile errorBranch = BranchProfile.create();
    private final BranchProfile relativeToUndefinedBranch = BranchProfile.create();
    private final BranchProfile relativeToPlainDateBranch = BranchProfile.create();
    private final BranchProfile relativeToZonedDateTimeBranch = BranchProfile.create();
    private final ConditionProfile largetUnitYMWDProfile = ConditionProfile.createBinaryProfile();

    protected TemporalDurationAddNode(JSContext ctx) {
        this.ctx = ctx;
        this.getMethodDateAddNode = GetMethodNode.create(ctx, TemporalConstants.DATE_ADD);
        this.getMethodDateUntilNode = GetMethodNode.create(ctx, TemporalConstants.DATE_UNTIL);
        this.namesNode = EnumerableOwnPropertyNamesNode.createKeys(ctx);
    }

    public static TemporalDurationAddNode create(JSContext ctx) {
        return TemporalDurationAddNodeGen.create(ctx);
    }

    public abstract JSTemporalDurationRecord execute(double y1, double mon1, double w1, double d1, double h1, double min1, double s1, double ms1, double mus1, double ns1,
                    double y2, double mon2, double w2, double d2, double h2, double min2, double s2, double ms2, double mus2, double ns2, JSDynamicObject relativeTo);

    // @Cached parameters create unused variable in generated code, see GR-37931
    @Specialization
    protected JSTemporalDurationRecord add(double y1, double mon1, double w1, double d1, double h1, double min1, double s1, double ms1, double mus1, double ns1,
                    double y2, double mon2, double w2, double d2, double h2, double min2, double s2, double ms2, double mus2, double ns2, JSDynamicObject relativeTo) {
        assert doubleIsInteger(y1) && doubleIsInteger(mon1) && doubleIsInteger(w1) && doubleIsInteger(d1);
        assert doubleIsInteger(h1) && doubleIsInteger(min1) && doubleIsInteger(s1) && doubleIsInteger(ms1) && doubleIsInteger(mus1) && doubleIsInteger(ns1);
        assert doubleIsInteger(y2) && doubleIsInteger(mon2) && doubleIsInteger(w2) && doubleIsInteger(d2);
        assert doubleIsInteger(h2) && doubleIsInteger(min2) && doubleIsInteger(s2) && doubleIsInteger(ms2) && doubleIsInteger(mus2) && doubleIsInteger(ns2);

        TemporalUtil.Unit largestUnit1 = TemporalUtil.defaultTemporalLargestUnit(y1, mon1, w1, d1, h1, min1, s1, ms1, mus1);
        TemporalUtil.Unit largestUnit2 = TemporalUtil.defaultTemporalLargestUnit(y2, mon2, w2, d2, h2, min2, s2, ms2, mus2);
        TemporalUtil.Unit largestUnit = TemporalUtil.largerOfTwoTemporalUnits(largestUnit1, largestUnit2);
        if (relativeTo == Undefined.instance) {
            relativeToUndefinedBranch.enter();
            if (largestUnit == TemporalUtil.Unit.YEAR || largestUnit == TemporalUtil.Unit.MONTH || largestUnit == TemporalUtil.Unit.WEEK) {
                errorBranch.enter();
                throw Errors.createRangeError("Largest unit allowed with no relative is 'days'.");
            }
            JSTemporalDurationRecord result = TemporalUtil.balanceDuration(ctx, namesNode, d1 + d2, h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2, ns1 + ns2, largestUnit);
            return TemporalUtil.createDurationRecord(0, 0, 0, result.getDays(), result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(),
                            result.getNanoseconds());
        } else if (JSTemporalPlainDate.isJSTemporalPlainDate(relativeTo)) {
            relativeToPlainDateBranch.enter();
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) relativeTo;
            JSDynamicObject calendar = date.getCalendar();
            JSDynamicObject dateDuration1 = JSTemporalDuration.createTemporalDuration(ctx, y1, mon1, w1, d1, 0, 0, 0, 0, 0, 0, errorBranch);
            JSDynamicObject dateDuration2 = JSTemporalDuration.createTemporalDuration(ctx, y2, mon2, w2, d2, 0, 0, 0, 0, 0, 0, errorBranch);

            Object dateAdd = getMethodDateAddNode.executeWithTarget(calendar);
            JSDynamicObject intermediate = calendarDateAdd(calendar, date, dateDuration1, Undefined.instance, dateAdd);
            JSDynamicObject end = calendarDateAdd(calendar, intermediate, dateDuration2, Undefined.instance, dateAdd);

            TemporalUtil.Unit dateLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(TemporalUtil.Unit.DAY, largestUnit);

            JSDynamicObject differenceOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(ctx, differenceOptions, LARGEST_UNIT, dateLargestUnit.toTruffleString());
            JSTemporalDurationObject dateDifference = calendarDateUntil(calendar, date, end, differenceOptions, Undefined.instance);
            JSTemporalDurationRecord result = TemporalUtil.balanceDuration(ctx, namesNode, TemporalUtil.dtol(dateDifference.getDays()),
                            h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2, ns1 + ns2, largestUnit);
            return TemporalUtil.createDurationRecord(dateDifference.getYears(), dateDifference.getMonths(), dateDifference.getWeeks(), result.getDays(),
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds());
        } else {
            relativeToZonedDateTimeBranch.enter();
            assert TemporalUtil.isTemporalZonedDateTime(relativeTo);
            JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) relativeTo;
            JSDynamicObject timeZone = zdt.getTimeZone();
            JSDynamicObject calendar = zdt.getCalendar();
            BigInt intermediateNs = TemporalUtil.addZonedDateTime(ctx, zdt.getNanoseconds(), timeZone, calendar, dtol(y1), dtol(mon1), dtol(w1), dtol(d1), dtol(h1), dtol(min1), dtol(s1), dtol(ms1),
                            dtol(mus1), dtol(ns1));
            BigInt endNs = TemporalUtil.addZonedDateTime(ctx, intermediateNs, timeZone, calendar, dtol(y2), dtol(mon2), dtol(w2), dtol(d2), dtol(h2), dtol(min2), dtol(s2), dtol(ms2), dtol(mus2),
                            dtol(ns2));
            if (largetUnitYMWDProfile.profile(
                            TemporalUtil.Unit.YEAR != largestUnit && TemporalUtil.Unit.MONTH != largestUnit && TemporalUtil.Unit.WEEK != largestUnit && TemporalUtil.Unit.DAY != largestUnit)) {
                long diffNs = TemporalUtil.bitol(TemporalUtil.differenceInstant(zdt.getNanoseconds(), endNs, 1d, TemporalUtil.Unit.NANOSECOND, TemporalUtil.RoundingMode.HALF_EXPAND));
                JSTemporalDurationRecord result = TemporalUtil.balanceDuration(ctx, namesNode, 0, 0, 0, 0, 0, 0, diffNs, largestUnit);
                return TemporalUtil.createDurationRecord(0, 0, 0, 0, result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(),
                                result.getNanoseconds());
            } else {
                return TemporalUtil.differenceZonedDateTime(ctx, namesNode, zdt.getNanoseconds(), endNs, timeZone, calendar, largestUnit);
            }
        }
    }

    protected JSTemporalPlainDateObject calendarDateAdd(JSDynamicObject calendar, JSDynamicObject date, JSDynamicObject duration, JSDynamicObject options, Object dateAddPrepared) {
        if (callDateAddNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callDateAddNode = insert(JSFunctionCallNode.createCall());
        }
        Object addedDate = callDateAddNode.executeCall(JSArguments.create(calendar, dateAddPrepared, date, duration, options));
        return TemporalUtil.requireTemporalDate(addedDate, errorBranch);
    }

    protected JSTemporalDurationObject calendarDateUntil(JSDynamicObject calendar, JSDynamicObject date, JSDynamicObject duration, JSDynamicObject options, Object dateUntil) {
        Object dateUntilPrepared = dateUntil;
        if (dateUntilPrepared == Undefined.instance) {
            dateUntilPrepared = getMethodDateUntilNode.executeWithTarget(calendar);
        }
        if (callDateUntilNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callDateUntilNode = insert(JSFunctionCallNode.createCall());
        }
        Object addedDate = callDateUntilNode.executeCall(JSArguments.create(calendar, dateUntilPrepared, date, duration, options));
        return TemporalUtil.requireTemporalDuration(addedDate);
    }
}
