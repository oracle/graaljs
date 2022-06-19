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

import java.time.Duration;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

@ExportLibrary(InteropLibrary.class)
public class JSTemporalDurationObject extends JSNonProxyObject {

    private final double years;
    private final double months;
    private final double weeks;
    private final double days;
    private final double hours;
    private final double minutes;
    private final double seconds;
    private final double milliseconds;
    private final double microseconds;
    private final double nanoseconds;

    public JSTemporalDurationObject(Shape shape, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds,
                    double nanoseconds) {
        super(shape);
        this.years = years;
        this.months = months;
        this.weeks = weeks;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.milliseconds = milliseconds;
        this.microseconds = microseconds;
        this.nanoseconds = nanoseconds;
    }

    public double getYears() {
        return years;
    }

    public double getMonths() {
        return months;
    }

    public double getWeeks() {
        return weeks;
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

    @ExportMessage
    final boolean isDuration() {
        // note: java.time.Duration only considers DAYS, while JS Temporal also has W, M, Y.
        // due to vague DAYS support in Java, we disallow conversion when any of D, W, M, Y is != 0.
        return years == 0 && months == 0 && weeks == 0 && days == 0 && JSRuntime.doubleIsRepresentableAsLong(calcSeconds()) && JSRuntime.doubleIsRepresentableAsLong(calcNanoseconds());
    }

    @ExportMessage
    @TruffleBoundary
    final Duration asDuration() throws UnsupportedMessageException {
        if (!isDuration()) {
            throw UnsupportedMessageException.create();
        }
        double sec = calcSeconds();
        double nanos = calcNanoseconds();
        Duration dur = Duration.ofSeconds((long) sec, (long) nanos);
        return dur;
    }

    private double calcNanoseconds() {
        return nanoseconds + microseconds * 1_000 + milliseconds * 1_000_000;
    }

    private double calcSeconds() {
        return seconds + minutes * 60 + hours * 60 * 60;
    }
}
