/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins.temporal;

import com.oracle.truffle.api.strings.TruffleString;

/**
 * Represents the information of a parsed TemporalTimeZoneString format.
 */
public final class JSTemporalParserRecord {
    private final boolean z;

    private final long year;
    private final long month;
    private final long day;
    private final long hour;
    private final long minute;
    private final long second;
    private final TruffleString fraction;

    private final TruffleString calendar;
    private final TruffleString timeZoneIANAName;
    private final TruffleString timeZoneUTCOffsetName;
    private final TruffleString timeZoneNumericUTCOffset;

    private final TruffleString offsetSign;
    private final long offsetHour;
    private final long offsetMinute;
    private final long offsetSecond;
    private final TruffleString offsetFraction;

    public JSTemporalParserRecord(boolean z, long year, long month, long day, long hour, long minute, long second, TruffleString fraction, TruffleString offsetSign, long offsetHour,
                    long offsetMinute, long offsetSecond, TruffleString offsetFraction, TruffleString timeZoneIANAName, TruffleString timeZoneUTCOffsetName, TruffleString calendar,
                    TruffleString timeZoneNumericUTCOffset) {
        this.z = z;

        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.fraction = fraction;

        this.timeZoneNumericUTCOffset = timeZoneNumericUTCOffset;
        this.offsetSign = offsetSign;
        this.offsetHour = offsetHour;
        this.offsetMinute = offsetMinute;
        this.offsetSecond = offsetSecond;
        this.offsetFraction = offsetFraction;

        this.calendar = calendar;
        this.timeZoneIANAName = timeZoneIANAName;
        this.timeZoneUTCOffsetName = timeZoneUTCOffsetName;
    }

    public boolean getZ() {
        return z;
    }

    public TruffleString getOffsetSign() {
        return offsetSign;
    }

    public long getHour() {
        return hour;
    }

    public long getMinute() {
        return minute;
    }

    public long getSecond() {
        return second;
    }

    public TruffleString getFraction() {
        return fraction;
    }

    public TruffleString getTimeZoneIANAName() {
        return timeZoneIANAName;
    }

    public TruffleString getTimeZoneUTCOffsetName() {
        return timeZoneUTCOffsetName;
    }

    public TruffleString getTimeZoneANYName() {
        if (timeZoneIANAName != null) {
            return timeZoneIANAName;
        }
        if (timeZoneUTCOffsetName != null) {
            return timeZoneUTCOffsetName;
        }
        return null;
    }

    public TruffleString getTimeZoneIdentifier() {
        if (timeZoneIANAName != null) {
            return timeZoneIANAName;
        }
        if (timeZoneUTCOffsetName != null) {
            return timeZoneUTCOffsetName;
        }
        return null;
    }

    public long getYear() {
        return year;
    }

    public long getMonth() {
        return month;
    }

    public long getDay() {
        return day;
    }

    public TruffleString getCalendar() {
        return calendar;
    }

    public long getOffsetHour() {
        return offsetHour;
    }

    public long getOffsetMinute() {
        return offsetMinute;
    }

    public long getOffsetSecond() {
        return offsetSecond;
    }

    public TruffleString getOffsetFraction() {
        return offsetFraction;
    }

    public TruffleString getTimeZoneNumericUTCOffset() {
        return timeZoneNumericUTCOffset;
    }

}
