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

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class JSTemporalDateTimeRecord {
    private final long year;
    private final long month;
    private final long day;
    private final long hour;
    private final long minute;
    private final long second;
    private final long millisecond;
    private final long microsecond;
    private final long nanosecond;

    private final DynamicObject calendar;
    private final boolean hasCalendar;

    private final long weeks;
    private final boolean hasWeeks;

    protected JSTemporalDateTimeRecord(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond,
                    long weeks, boolean hasWeeks, DynamicObject calendar, boolean hasCalendar) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.millisecond = millisecond;
        this.microsecond = microsecond;
        this.nanosecond = nanosecond;

        this.calendar = calendar;
        this.hasCalendar = hasCalendar;

        this.weeks = weeks;
        this.hasWeeks = hasWeeks;
    }

    @SuppressWarnings("hiding")
    public static JSTemporalDateTimeRecord create(long year, long month, long day, long hour, long minute, long second,
                    long millisecond, long microsecond, long nanosecond) {
        return new JSTemporalDateTimeRecord(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, 0, false, null, false);
    }

    public static JSTemporalDateTimeRecord createCalendar(long year, long month, long day, long hour, long minute, long second,
                    long millisecond, long microsecond, long nanosecond, DynamicObject calendar) {
        return new JSTemporalDateTimeRecord(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, 0, false, calendar, true);
    }

    public static JSTemporalDateTimeRecord createWeeks(long year, long month, long weeks, long day, long hour, long minute, long second,
                    long millisecond, long microsecond, long nanosecond) {
        return new JSTemporalDateTimeRecord(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, weeks, true, null, false);
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

    public long getHour() {
        return hour;
    }

    public long getMinute() {
        return minute;
    }

    public long getSecond() {
        return second;
    }

    public long getMillisecond() {
        return millisecond;
    }

    public long getMicrosecond() {
        return microsecond;
    }

    public long getNanosecond() {
        return nanosecond;
    }

    public DynamicObject getCalendar() {
        return hasCalendar ? calendar : Undefined.instance;
    }

    public long getWeeks() {
        assert hasWeeks;
        return weeks;
    }

    @SuppressWarnings("static-method")
    public Object getTimeZoneOffset() {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("static-method")
    public Object getTimeZoneIANAName() {
        // TODO Auto-generated method stub
        return Undefined.instance;
    }

    public boolean hasCalendar() {
        return hasCalendar && calendar != Undefined.instance;
    }
}
