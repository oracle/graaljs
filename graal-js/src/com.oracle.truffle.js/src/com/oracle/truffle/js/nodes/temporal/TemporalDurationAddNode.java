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

import java.util.stream.DoubleStream;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.CalendarMethodsRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendarHolder;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeZoneMethodsRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of the Temporal addDuration operation.
 */
public abstract class TemporalDurationAddNode extends JavaScriptBaseNode {

    @Child EnumerableOwnPropertyNamesNode namesNode;

    protected TemporalDurationAddNode() {
        JSContext ctx = JavaScriptLanguage.get(null).getJSContext();
        this.namesNode = EnumerableOwnPropertyNamesNode.createKeys(ctx);
    }

    public abstract JSTemporalDurationRecord execute(double y1, double mon1, double w1, double d1, double h1, double min1, double s1, double ms1, double mus1, double ns1,
                    double y2, double mon2, double w2, double d2, double h2, double min2, double s2, double ms2, double mus2, double ns2,
                    JSTemporalCalendarHolder relativeTo, CalendarMethodsRecord calendarRec, TimeZoneMethodsRecord timeZoneRec,
                    JSTemporalPlainDateTimeObject precalculatedPlainDateTime);

    // @Cached parameters create unused variable in generated code, see GR-37931
    @Specialization
    protected JSTemporalDurationRecord add(double y1, double mon1, double w1, double d1, double h1, double min1, double s1, double ms1, double mus1, double ns1,
                    double y2, double mon2, double w2, double d2, double h2, double min2, double s2, double ms2, double mus2, double ns2,
                    JSTemporalCalendarHolder relativeTo, CalendarMethodsRecord calendarRec, TimeZoneMethodsRecord timeZoneRec,
                    JSTemporalPlainDateTimeObject precalculatedPlainDateTime,
                    @Cached TemporalRoundDurationNode roundDurationNode,
                    @Cached TemporalAddDateNode addDateNode,
                    @Cached TemporalDifferenceDateNode differenceDateNode,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached InlinedBranchProfile relativeToUndefinedBranch,
                    @Cached InlinedBranchProfile relativeToPlainDateBranch,
                    @Cached InlinedBranchProfile relativeToZonedDateTimeBranch,
                    @Cached InlinedConditionProfile largetUnitYMWDProfile) {
        assert DoubleStream.of(
                        y1, mon1, w1, d1, h1, min1, s1, ms1, mus1, ns1,
                        y2, mon2, w2, d2, h2, min2, s2, ms2, mus2, ns2).allMatch(JSRuntime::isIntegralNumber);

        TemporalUtil.Unit largestUnit1 = TemporalUtil.defaultTemporalLargestUnit(y1, mon1, w1, d1, h1, min1, s1, ms1, mus1);
        TemporalUtil.Unit largestUnit2 = TemporalUtil.defaultTemporalLargestUnit(y2, mon2, w2, d2, h2, min2, s2, ms2, mus2);
        TemporalUtil.Unit largestUnit = TemporalUtil.largerOfTwoTemporalUnits(largestUnit1, largestUnit2);
        JSContext ctx = getLanguage().getJSContext();
        JSRealm realm = getRealm();
        if (relativeTo == null) {
            relativeToUndefinedBranch.enter(this);
            if (largestUnit == TemporalUtil.Unit.YEAR || largestUnit == TemporalUtil.Unit.MONTH || largestUnit == TemporalUtil.Unit.WEEK) {
                errorBranch.enter(this);
                throw Errors.createRangeError("Largest unit allowed with no relativeTo is 'days'.");
            }
            var result = TemporalUtil.balanceTimeDuration(d1 + d2, h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2, ns1 + ns2, largestUnit);
            return TemporalUtil.createDurationRecord(0, 0, 0, result.days(), result.hours(), result.minutes(), result.seconds(), result.milliseconds(), result.microseconds(), result.nanoseconds());
        } else if (relativeTo instanceof JSTemporalPlainDateObject plainRelativeTo) {
            relativeToPlainDateBranch.enter(this);
            var dateDuration1 = JSTemporalDuration.createTemporalDuration(ctx, realm, y1, mon1, w1, d1, 0, 0, 0, 0, 0, 0, this, errorBranch);
            var dateDuration2 = JSTemporalDuration.createTemporalDuration(ctx, realm, y2, mon2, w2, d2, 0, 0, 0, 0, 0, 0, this, errorBranch);

            JSTemporalPlainDateObject intermediate = addDateNode.execute(calendarRec, plainRelativeTo, dateDuration1, Undefined.instance);
            JSTemporalPlainDateObject end = addDateNode.execute(calendarRec, intermediate, dateDuration2, Undefined.instance);

            TemporalUtil.Unit dateLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(TemporalUtil.Unit.DAY, largestUnit);

            JSObject differenceOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(differenceOptions, LARGEST_UNIT, dateLargestUnit.toTruffleString());
            JSTemporalDurationObject dateDifference = differenceDateNode.execute(calendarRec, plainRelativeTo, end, largestUnit, differenceOptions);
            var result = TemporalUtil.balanceTimeDuration(dateDifference.getDays(),
                            h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2, ns1 + ns2, largestUnit);
            return TemporalUtil.createDurationRecord(dateDifference.getYears(), dateDifference.getMonths(), dateDifference.getWeeks(), result.days(),
                            result.hours(), result.minutes(), result.seconds(), result.milliseconds(), result.microseconds(), result.nanoseconds());
        } else if (relativeTo instanceof JSTemporalZonedDateTimeObject zonedRelativeTo) {
            relativeToZonedDateTimeBranch.enter(this);
            BigInt intermediateNs = TemporalUtil.addZonedDateTime(ctx, realm, zonedRelativeTo.getNanoseconds(), timeZoneRec, calendarRec,
                            dtol(y1), dtol(mon1), dtol(w1), dtol(d1), dtol(h1), dtol(min1), dtol(s1), dtol(ms1), dtol(mus1), dtol(ns1), precalculatedPlainDateTime);
            BigInt endNs = TemporalUtil.addZonedDateTime(ctx, realm, intermediateNs, timeZoneRec, calendarRec,
                            dtol(y2), dtol(mon2), dtol(w2), dtol(d2), dtol(h2), dtol(min2), dtol(s2), dtol(ms2), dtol(mus2), dtol(ns2), precalculatedPlainDateTime);
            if (largetUnitYMWDProfile.profile(this,
                            TemporalUtil.Unit.YEAR != largestUnit && TemporalUtil.Unit.MONTH != largestUnit && TemporalUtil.Unit.WEEK != largestUnit && TemporalUtil.Unit.DAY != largestUnit)) {
                TimeDurationRecord result = TemporalUtil.differenceInstant(zonedRelativeTo.getNanoseconds(), endNs, 1,
                                TemporalUtil.Unit.NANOSECOND, largestUnit, TemporalUtil.RoundingMode.HALF_EXPAND, roundDurationNode);
                return TemporalUtil.createDurationRecord(0, 0, 0, 0, result.hours(), result.minutes(), result.seconds(), result.milliseconds(), result.microseconds(), result.nanoseconds());
            } else {
                return TemporalUtil.differenceZonedDateTime(ctx, realm, namesNode, zonedRelativeTo.getNanoseconds(), endNs, timeZoneRec, calendarRec, largestUnit, precalculatedPlainDateTime);
            }
        } else {
            throw Errors.shouldNotReachHereUnexpectedValue(relativeTo);
        }
    }
}
