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
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.NormalizedDurationRecord;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

/**
 * Implements the DifferenceISODateTime operation.
 */
@ImportStatic(TemporalConstants.class)
public abstract class DifferenceISODateTimeNode extends JavaScriptBaseNode {

    protected DifferenceISODateTimeNode() {
    }

    public abstract NormalizedDurationRecord execute(
                    int y1, int mon1, int d1, int h1, int min1, int s1, int ms1, int mus1, int ns1,
                    int y2, int mon2, int d2, int h2, int min2, int s2, int ms2, int mus2, int ns2,
                    TruffleString calendar, Unit largestUnit);

    @Specialization
    final NormalizedDurationRecord differencePlainDateTimeWithRounding(
                    int y1, int mon1, int d1, int h1, int min1, int s1, int ms1, int mus1, int ns1,
                    int y2, int mon2, int d2, int h2, int min2, int s2, int ms2, int mus2, int ns2,
                    TruffleString calendar, Unit largestUnit,
                    @Cached TemporalDifferenceDateNode differenceDate) {
        JSContext ctx = getJSContext();
        JSRealm realm = getRealm();

        BigInt timeDuration = TemporalUtil.differenceTime(h1, min1, s1, ms1, mus1, ns1, h2, min2, s2, ms2, mus2, ns2);
        int timeSign = TemporalUtil.normalizedTimeDurationSign(timeDuration);
        int dateSign = TemporalUtil.compareISODate(y2, mon2, d2, y1, mon1, d1);

        ISODateRecord adjustedDate = TemporalUtil.createISODateRecord(y1, mon1, d1);
        if (timeSign == -dateSign) {
            adjustedDate = TemporalUtil.balanceISODate(adjustedDate.year(), adjustedDate.month(), adjustedDate.day() - timeSign);
            timeDuration = TemporalUtil.add24HourDaysToNormalizedTimeDuration(timeDuration, -timeSign);
        }

        var date1 = JSTemporalPlainDate.create(ctx, realm, adjustedDate.year(), adjustedDate.month(), adjustedDate.day(), calendar, null,
                        InlinedBranchProfile.getUncached());
        var date2 = JSTemporalPlainDate.create(ctx, realm, y2, mon2, d2, calendar, null, InlinedBranchProfile.getUncached());

        Unit dateLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(Unit.DAY, largestUnit);

        JSTemporalDurationObject dateDifference = differenceDate.execute(calendar, date1, date2, dateLargestUnit);
        double days = dateDifference.getDays();
        if (largestUnit != dateLargestUnit) {
            timeDuration = TemporalUtil.add24HourDaysToNormalizedTimeDuration(timeDuration, days);
            days = 0;
        }
        return TemporalUtil.createNormalizedDurationRecord(dateDifference.getYears(), dateDifference.getMonths(), dateDifference.getWeeks(), days, timeDuration);
    }
}
