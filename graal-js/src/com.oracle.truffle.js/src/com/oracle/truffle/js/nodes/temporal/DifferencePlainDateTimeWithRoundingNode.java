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
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.NormalizedDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDurationWithTotalRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeDurationRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

/**
 * Implements the DifferencePlainDateTimeWithRounding operation.
 */
@ImportStatic(TemporalConstants.class)
public abstract class DifferencePlainDateTimeWithRoundingNode extends JavaScriptBaseNode {

    protected DifferencePlainDateTimeWithRoundingNode() {
    }

    public abstract TemporalDurationWithTotalRecord execute(
                    JSTemporalPlainDateObject plainDate1, int h1, int min1, int s1, int ms1, int mus1, int ns1,
                    int y2, int mon2, int d2, int h2, int min2, int s2, int ms2, int mus2, int ns2,
                    TruffleString calendar, Unit largestUnit, int roundingIncrement, Unit smallestUnit, RoundingMode roundingMode, JSDynamicObject resolvedOptions);

    @Specialization
    static TemporalDurationWithTotalRecord differencePlainDateTimeWithRounding(
                    JSTemporalPlainDateObject plainDate1, int h1, int min1, int s1, int ms1, int mus1, int ns1,
                    int y2, int mon2, int d2, int h2, int min2, int s2, int ms2, int mus2, int ns2,
                    TruffleString calendar, Unit largestUnit, int roundingIncrement, Unit smallestUnit, RoundingMode roundingMode, JSDynamicObject resolvedOptions,
                    @Cached DifferenceISODateTimeNode differenceISODateTime,
                    @Cached RoundRelativeDurationNode roundRelativeDuration) {
        int y1 = plainDate1.getYear();
        int mon1 = plainDate1.getMonth();
        int d1 = plainDate1.getDay();
        if (TemporalUtil.compareISODateTime(
                        y1, mon1, d1, h1, min1, s1, ms1, mus1, ns1,
                        y2, mon2, d2, h2, min2, s2, ms2, mus2, ns2) == 0) {
            return new TemporalDurationWithTotalRecord(JSTemporalDurationRecord.createZero(), 0);
        }

        NormalizedDurationRecord diff = differenceISODateTime.execute(
                        y1, mon1, d1, h1, min1, s1, ms1, mus1, ns1,
                        y2, mon2, d2, h2, min2, s2, ms2, mus2, ns2,
                        calendar, largestUnit, resolvedOptions);
        if (smallestUnit == Unit.NANOSECOND && roundingIncrement == 1) {
            BigInt normWithDays = TemporalUtil.add24HourDaysToNormalizedTimeDuration(diff.normalizedTimeTotalNanoseconds(), diff.days());
            TimeDurationRecord timeResult = TemporalUtil.balanceTimeDuration(normWithDays, largestUnit);
            double total = normWithDays.doubleValue();
            var durationRecord = JSTemporalDurationRecord.createWeeks(diff.years(), diff.months(), diff.weeks(), diff.days(),
                            timeResult.hours(), timeResult.minutes(), timeResult.seconds(),
                            timeResult.milliseconds(), timeResult.microseconds(), timeResult.nanoseconds());
            return new TemporalDurationWithTotalRecord(durationRecord, total);
        }

        ISODateTimeRecord dateTime = new ISODateTimeRecord(y1, mon1, d1, h1, min1, s1, ms1, mus1, ns1);
        BigInt destEpochNs = TemporalUtil.getUTCEpochNanoseconds(y2, mon2, d2, h2, min2, s2, ms2, mus2, ns2);
        return roundRelativeDuration.execute(diff, destEpochNs, dateTime, calendar, null, largestUnit, roundingIncrement, smallestUnit, roundingMode);
    }
}
