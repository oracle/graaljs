/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeRecord;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of the Temporal AddDateTime operation.
 */
public abstract class TemporalAddDateTimeNode extends JavaScriptBaseNode {

    protected TemporalAddDateTimeNode() {
    }

    public abstract JSTemporalDateTimeRecord execute(int year, int month, int day,
                    int hour, int minute, int second, int millisecond, int microsecond, double nanosecond,
                    TruffleString calendar,
                    double years, double months, double weeks, double days, BigInt normalizedTimeDuration,
                    TemporalUtil.Overflow overflow);

    @Specialization
    protected JSTemporalDateTimeRecord addDateTime(int year, int month, int day,
                    int hour, int minute, int second, int millisecond, int microsecond, double nanosecond,
                    TruffleString calendar,
                    double years, double months, double weeks, double days, BigInt normalizedTimeDuration,
                    TemporalUtil.Overflow overflow,
                    @Cached TemporalAddDateNode addDateNode,
                    @Cached InlinedBranchProfile errorBranch) {
        JSContext ctx = getJSContext();
        JSRealm realm = getRealm();

        TimeRecord timeResult = TemporalUtil.addTime(hour, minute, second, millisecond, microsecond, nanosecond, normalizedTimeDuration, this, errorBranch);
        JSTemporalPlainDateObject datePart = JSTemporalPlainDate.create(ctx, realm, year, month, day, calendar, this, errorBranch);
        JSTemporalDurationObject dateDuration = JSTemporalDuration.createTemporalDuration(ctx, realm, years, months, weeks, days + timeResult.days(), 0L, 0L, 0L, 0L, 0L, 0L, this, errorBranch);
        JSTemporalPlainDateObject addedDate = addDateNode.execute(calendar, datePart, dateDuration, overflow);
        return JSTemporalDateTimeRecord.create(addedDate.getYear(), addedDate.getMonth(), addedDate.getDay(),
                        timeResult.hour(), timeResult.minute(), timeResult.second(), timeResult.millisecond(), timeResult.microsecond(), timeResult.nanosecond());
    }

}
