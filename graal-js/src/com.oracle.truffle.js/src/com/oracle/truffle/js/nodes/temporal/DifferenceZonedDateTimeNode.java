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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.NormalizedDurationRecord;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Disambiguation;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

/**
 * Implements the DifferenceZonedDateTime operation.
 */
@ImportStatic(TemporalConstants.class)
public abstract class DifferenceZonedDateTimeNode extends JavaScriptBaseNode {

    protected DifferenceZonedDateTimeNode() {
    }

    public abstract NormalizedDurationRecord execute(BigInt ns1, BigInt ns2,
                    TruffleString timeZone, TruffleString calendar,
                    Unit largestUnit, JSTemporalPlainDateTimeObject startDateTime);

    @Specialization
    final NormalizedDurationRecord differenceZonedDateTime(BigInt ns1, BigInt ns2,
                    TruffleString timeZone, TruffleString calendar,
                    Unit largestUnit, JSTemporalPlainDateTimeObject startDateTime,
                    @Cached TemporalDifferenceDateNode differenceDateNode) {
        int sign = ns2.compareTo(ns1);
        if (sign == 0) { // ns1 == ns2
            return new NormalizedDurationRecord(0, 0, 0, 0, TemporalUtil.zeroTimeDuration());
        }

        JSContext ctx = getJSContext();
        JSRealm realm = getRealm();

        JSTemporalInstantObject endInstant = JSTemporalInstant.create(ctx, realm, ns2);
        JSTemporalPlainDateTimeObject endDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, realm, timeZone, endInstant, calendar);
        int maxDayCorrection = sign == 1 ? 2 : 1;
        int dayCorrection = 0;

        BigInt timeDuration = TemporalUtil.differenceTime(
                        startDateTime.getHour(), startDateTime.getMinute(), startDateTime.getSecond(),
                        startDateTime.getMillisecond(), startDateTime.getMicrosecond(), startDateTime.getNanosecond(),
                        endDateTime.getHour(), endDateTime.getMinute(), endDateTime.getSecond(),
                        endDateTime.getMillisecond(), endDateTime.getMicrosecond(), endDateTime.getNanosecond());
        int normalizedTimeDurationSign = TemporalUtil.normalizedTimeDurationSign(timeDuration);
        if (normalizedTimeDurationSign == -sign) {
            dayCorrection++;
        }

        JSTemporalPlainDateTimeObject intermediateDateTime = null;
        BigInt norm = null;
        boolean success = false;
        for (; dayCorrection <= maxDayCorrection; dayCorrection++) {
            ISODateRecord intermediateDate = TemporalUtil.balanceISODate(endDateTime.getYear(), endDateTime.getMonth(), endDateTime.getDay() - dayCorrection * sign);
            intermediateDateTime = JSTemporalPlainDateTime.create(ctx, realm,
                            intermediateDate.year(), intermediateDate.month(), intermediateDate.day(),
                            startDateTime.getHour(), startDateTime.getMinute(), startDateTime.getSecond(),
                            startDateTime.getMillisecond(), startDateTime.getMicrosecond(), startDateTime.getNanosecond(), calendar);
            BigInt intermediateNs = TemporalUtil.builtinTimeZoneGetInstantFor(ctx, realm, timeZone, intermediateDateTime, Disambiguation.COMPATIBLE);
            norm = TemporalUtil.normalizedTimeDurationFromEpochNanosecondsDifference(ns2, intermediateNs);
            int timeSign = TemporalUtil.normalizedTimeDurationSign(norm);
            if (sign != -timeSign) {
                success = true;
                break;
            }
        }
        if (success) {
            var date1 = JSTemporalPlainDate.create(ctx, realm, startDateTime.getYear(), startDateTime.getMonth(), startDateTime.getDay(), calendar,
                            null, InlinedBranchProfile.getUncached());
            var date2 = JSTemporalPlainDate.create(ctx, realm, intermediateDateTime.getYear(), intermediateDateTime.getMonth(), intermediateDateTime.getDay(), calendar,
                            null, InlinedBranchProfile.getUncached());
            Unit dateLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(largestUnit, Unit.DAY);
            JSTemporalDurationObject dateDifference = differenceDateNode.execute(calendar, date1, date2, dateLargestUnit);
            return TemporalUtil.createNormalizedDurationRecord(dateDifference.getYears(), dateDifference.getMonths(), dateDifference.getWeeks(), dateDifference.getDays(), norm);
        }
        throw Errors.createRangeError("custom calendar or time zone methods returned inconsistent values");
    }
}
