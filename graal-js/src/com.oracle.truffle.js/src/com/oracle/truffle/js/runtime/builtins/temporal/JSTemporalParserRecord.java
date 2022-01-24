/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Represents the information of a parsed TemporalTimeZoneString format.
 */
public final class JSTemporalParserRecord {
    private boolean z;

    private final long year;
    private final long month;
    private final long day;
    private final long hour;
    private final long minute;
    private final long second;
    private final String fraction;

    private final String calendar;
    private final String timeZoneIANAName;
    private final String timeZoneEtcName;
    private final String timeZoneUTCOffsetName;
    private final String timeZoneNumericUTCOffset;

    private final String offsetSign;
    private final long offsetHour;
    private final long offsetMinute;
    private final long offsetSecond;
    private final String offsetFraction;

    public JSTemporalParserRecord(boolean z, long year, long month, long day, long hour, long minute, long second, String fraction, String offsetSign, long offsetHour,
                    long offsetMinute, long offsetSecond, String offsetFraction, String timeZoneIANAName, String timeZoneEtcName, String timeZoneUTCOffsetName, String calendar,
                    String timeZoneNumericUTCOffset) {
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
        this.timeZoneEtcName = timeZoneEtcName;
        this.timeZoneUTCOffsetName = timeZoneUTCOffsetName;
    }

    public boolean getZ() {
        return z;
    }

    public String getOffsetSign() {
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

    public String getFraction() {
        return fraction;
    }

    public String getTimeZoneIANAName() {
        return timeZoneIANAName;
    }

    public String getTimeZoneUTCOffsetName() {
        return timeZoneIANAName;
    }

    public String getTimeZoneEtcName() {
        return timeZoneIANAName;
    }

    public String getTimeZoneANYName() {
        if (timeZoneIANAName != null) {
            return timeZoneIANAName;
        }
        if (timeZoneUTCOffsetName != null) {
            return timeZoneUTCOffsetName;
        }
        if (timeZoneEtcName != null) {
            return timeZoneEtcName;
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

    public String getCalendar() {
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

    public String getOffsetFraction() {
        return offsetFraction;
    }

    public String getTimeZoneNumericUTCOffset() {
        return timeZoneNumericUTCOffset;
    }

}
