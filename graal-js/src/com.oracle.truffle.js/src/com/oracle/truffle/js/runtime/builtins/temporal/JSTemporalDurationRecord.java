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

public final class JSTemporalDurationRecord {
    private final double years;
    private final double months;
    private final double days;
    private final double hours;
    private final double minutes;
    private final double seconds;
    private final double milliseconds;
    private final double microseconds;
    private final double nanoseconds;

    private final double weeks;

    private JSTemporalDurationRecord(double years, double months, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds,
                    double weeks) {
        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.milliseconds = milliseconds;
        this.microseconds = microseconds;
        this.nanoseconds = nanoseconds;
        this.weeks = weeks;
    }

    public static JSTemporalDurationRecord create(double years, double months, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds,
                    double nanoseconds) {
        return new JSTemporalDurationRecord(years, months, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
    }

    public static JSTemporalDurationRecord createWeeks(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds,
                    double nanoseconds) {
        return new JSTemporalDurationRecord(years, months, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, weeks);
    }

    public double getYears() {
        return years;
    }

    public double getMonths() {
        return months;
    }

    public double getDays() {
        return days;
    }

    public double getHours() {
        return hours;
    }

    public double getMinutes() {
        return minutes;
    }

    public double getSeconds() {
        return seconds;
    }

    public double getMilliseconds() {
        return milliseconds;
    }

    public double getMicroseconds() {
        return microseconds;
    }

    public double getNanoseconds() {
        return nanoseconds;
    }

    public double getWeeks() {
        return weeks;
    }

    public static JSTemporalDurationRecord create(JSTemporalDateTimeRecord r) {
        return create(r.getYear(), r.getMonth(), r.getDay(), r.getHour(), r.getMinute(), r.getSecond(), r.getMillisecond(), r.getMicrosecond(), r.getNanosecond());
    }

    public static JSTemporalDurationRecord create(JSTemporalDurationObject duration) {
        return createWeeks(duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                        duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
    }

    public static JSTemporalDurationRecord createZero() {
        return new JSTemporalDurationRecord(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
