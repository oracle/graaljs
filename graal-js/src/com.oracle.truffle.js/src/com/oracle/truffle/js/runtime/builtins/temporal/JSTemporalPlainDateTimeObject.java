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

import java.time.LocalDate;
import java.time.LocalTime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

@ExportLibrary(InteropLibrary.class)
public final class JSTemporalPlainDateTimeObject extends JSTemporalCalendarHolder {

    // from time
    private final int hours;
    private final int minutes;
    private final int seconds;
    private final int milliseconds;
    private final int microseconds;
    private final int nanoseconds;
    // from date
    private final int year;
    private final int month;
    private final int day;

    protected JSTemporalPlainDateTimeObject(Shape shape, JSDynamicObject proto, int year, int month, int day, int hours, int minutes, int seconds, int milliseconds,
                    int microseconds, int nanoseconds, TruffleString calendar) {
        super(shape, proto, calendar);
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.milliseconds = milliseconds;
        this.microseconds = microseconds;
        this.nanoseconds = nanoseconds;

        this.year = year;
        this.month = month;
        this.day = day;
    }

    public int getHour() {
        return hours;
    }

    public int getMinute() {
        return minutes;
    }

    public int getSecond() {
        return seconds;
    }

    public int getMillisecond() {
        return milliseconds;
    }

    public int getMicrosecond() {
        return microseconds;
    }

    public int getNanosecond() {
        return nanoseconds;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    boolean isTime() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    LocalTime asTime() {
        int ns = milliseconds * 1_000_000 + microseconds * 1_000 + nanoseconds;
        return LocalTime.of(hours, minutes, seconds, ns);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    boolean isDate() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    LocalDate asDate() {
        return LocalDate.of(year, month, day);
    }

    @Override
    public TruffleString getClassName() {
        return JSTemporalPlainDateTime.TO_STRING_TAG;
    }
}
