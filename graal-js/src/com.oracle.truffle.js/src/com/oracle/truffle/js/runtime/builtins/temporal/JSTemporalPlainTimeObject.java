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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

import java.time.LocalTime;

@ExportLibrary(InteropLibrary.class)
public class JSTemporalPlainTimeObject extends JSNonProxyObject implements TemporalTime {

    // all values guaranteed to fit into int
    // https://tc39.es/proposal-temporal/#sec-temporal-isvalidtime
    private final int hour;
    private final int minute;
    private final int second;
    private final int millisecond;
    private final int microsecond;
    private final int nanosecond;
    private final JSDynamicObject calendar;

    protected JSTemporalPlainTimeObject(Shape shape, int hour, int minute, int second, int millisecond,
                    int microsecond, int nanosecond, JSDynamicObject calendar) {
        super(shape);
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.millisecond = millisecond;
        this.microsecond = microsecond;
        this.nanosecond = nanosecond;
        this.calendar = calendar;
    }

    @Override
    public int getHour() {
        return hour;
    }

    @Override
    public int getMinute() {
        return minute;
    }

    @Override
    public int getSecond() {
        return second;
    }

    @Override
    public int getMillisecond() {
        return millisecond;
    }

    @Override
    public int getMicrosecond() {
        return microsecond;
    }

    @Override
    public int getNanosecond() {
        return nanosecond;
    }

    @Override
    public JSDynamicObject getCalendar() {
        return calendar;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    final boolean isTime() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    final LocalTime asTime() {
        int ns = millisecond * 1_000_000 + microsecond * 1_000 + nanosecond;
        LocalTime lt = LocalTime.of(hour, minute, second, ns);
        return lt;
    }
}
