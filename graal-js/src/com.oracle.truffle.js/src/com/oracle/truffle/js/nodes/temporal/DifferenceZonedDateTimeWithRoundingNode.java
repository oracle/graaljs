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
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDurationWithTotalRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeDurationRecord;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

/**
 * Implements the DifferenceZonedDateTimeWithRounding operation.
 */
@ImportStatic(TemporalConstants.class)
public abstract class DifferenceZonedDateTimeWithRoundingNode extends JavaScriptBaseNode {

    protected DifferenceZonedDateTimeWithRoundingNode() {
    }

    public abstract TemporalDurationWithTotalRecord execute(BigInt ns1, BigInt ns2,
                    TruffleString calendar, TruffleString timeZone,
                    JSTemporalPlainDateTimeObject precalculatedPlainDateTime,
                    Unit largestUnit, int roundingIncrement, Unit smallestUnit, RoundingMode roundingMode);

    @Specialization
    static TemporalDurationWithTotalRecord differenceZonedDateTimeWithRounding(BigInt ns1, BigInt ns2,
                    TruffleString calendar, TruffleString timeZone,
                    JSTemporalPlainDateTimeObject precalculatedPlainDateTime,
                    Unit largestUnit, int roundingIncrement, Unit smallestUnit, RoundingMode roundingMode,
                    @Cached DifferenceZonedDateTimeNode differenceZonedDateTime,
                    @Cached RoundRelativeDurationNode roundRelativeDuration) {
        if (!largestUnit.isCalendarUnit() && largestUnit != Unit.DAY) {
            var diffRecord = TemporalUtil.differenceInstant(ns1, ns2, roundingIncrement, smallestUnit, roundingMode);
            BigInt norm = diffRecord.normalizedTimeDuration();
            TimeDurationRecord result = TemporalUtil.balanceTimeDuration(norm, largestUnit);
            var durationRecord = JSTemporalDurationRecord.createWeeks(0, 0, 0, 0,
                            result.hours(), result.minutes(), result.seconds(), result.milliseconds(), result.microseconds(), result.nanoseconds());
            return new TemporalDurationWithTotalRecord(durationRecord, diffRecord.total());
        }

        var difference = differenceZonedDateTime.execute(ns1, ns2, timeZone, calendar, largestUnit, precalculatedPlainDateTime);
        boolean roundingGranularityIsNoop = smallestUnit == Unit.NANOSECOND && roundingIncrement == 1;
        if (roundingGranularityIsNoop) {
            TimeDurationRecord timeResult = TemporalUtil.balanceTimeDuration(difference.normalizedTimeTotalNanoseconds(), Unit.HOUR);
            double total = difference.normalizedTimeTotalNanoseconds().doubleValue();
            var durationRecord = JSTemporalDurationRecord.createWeeks(difference.years(), difference.months(), difference.weeks(), difference.days(),
                            timeResult.hours(), timeResult.minutes(), timeResult.seconds(), timeResult.milliseconds(), timeResult.microseconds(), timeResult.nanoseconds());
            return new TemporalDurationWithTotalRecord(durationRecord, total);
        }

        ISODateTimeRecord dateTime = new ISODateTimeRecord(
                        precalculatedPlainDateTime.getYear(), precalculatedPlainDateTime.getMonth(), precalculatedPlainDateTime.getDay(),
                        precalculatedPlainDateTime.getHour(), precalculatedPlainDateTime.getMinute(), precalculatedPlainDateTime.getSecond(),
                        precalculatedPlainDateTime.getMillisecond(), precalculatedPlainDateTime.getMicrosecond(), precalculatedPlainDateTime.getNanosecond());
        return roundRelativeDuration.execute(difference, ns2, dateTime, calendar, timeZone, largestUnit, roundingIncrement, smallestUnit, roundingMode);
    }
}
