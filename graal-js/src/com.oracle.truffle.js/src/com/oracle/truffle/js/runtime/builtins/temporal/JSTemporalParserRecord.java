/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

    private final String year;
    private final String month;
    private final String day;
    private final String hour;
    private final String minute;
    private final String second;
    private final String fraction;

    private final String calendar;
    private final String name;

    private final String offsetSign;
    private final String offsetHour;
    private final String offsetMinute;
    private final String offsetSecond;
    private final String offsetFraction;

    public JSTemporalParserRecord(boolean z, String year, String month, String day, String hour, String minute, String second, String fraction, String offsetSign, String offsetHour,
                    String offsetMinute, String offsetSecond, String offsetFraction, String name, String calendar) {
        this.z = z;

        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.fraction = fraction;

        this.offsetSign = offsetSign;
        this.offsetHour = offsetHour;
        this.offsetMinute = offsetMinute;
        this.offsetSecond = offsetSecond;
        this.offsetFraction = offsetFraction;

        this.calendar = calendar;
        this.name = name;
    }

    public boolean getZ() {
        return z;
    }

    public String getOffsetSign() {
        return offsetSign;
    }

    public String getHour() {
        return hour;
    }

    public String getMinute() {
        return minute;
    }

    public String getSecond() {
        return second;
    }

    public String getFraction() {
        return fraction;
    }

    public String getName() {
        return name;
    }

    public String getYear() {
        return year;
    }

    public String getMonth() {
        return month;
    }

    public String getDay() {
        return day;
    }

    public String getCalendar() {
        return calendar;
    }

    public String getOffsetHour() {
        return offsetHour;
    }

    public String getOffsetMinute() {
        return offsetMinute;
    }

    public String getOffsetSecond() {
        return offsetSecond;
    }

    public String getOffsetFraction() {
        return offsetFraction;
    }

}
