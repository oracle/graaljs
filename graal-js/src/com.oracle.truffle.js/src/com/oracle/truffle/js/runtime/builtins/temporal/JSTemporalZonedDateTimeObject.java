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
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@ExportLibrary(InteropLibrary.class)
public class JSTemporalZonedDateTimeObject extends JSNonProxyObject implements TemporalCalendar {

    private final BigInt nanoseconds; // 6.4. A BigInt value
    private final JSDynamicObject timeZone;
    private final JSDynamicObject calendar;

    protected JSTemporalZonedDateTimeObject(Shape shape, BigInt nanoseconds, JSDynamicObject timeZone, JSDynamicObject calendar) {
        super(shape);
        this.nanoseconds = nanoseconds;
        this.calendar = calendar;
        this.timeZone = timeZone;
    }

    public BigInt getNanoseconds() {
        return nanoseconds;
    }

    @Override
    public JSDynamicObject getCalendar() {
        return calendar;
    }

    public JSDynamicObject getTimeZone() {
        return timeZone;
    }

    @TruffleBoundary
    private Instant toInstant() {
        BigInteger[] res = nanoseconds.bigIntegerValue().divideAndRemainder(TemporalUtil.BI_10_POW_9);
        return Instant.ofEpochSecond(res[0].longValue(), res[1].intValue());
    }

    @ExportMessage
    final boolean isTimeZone() {
        return getZoneIdIntl() != null;
    }

    @ExportMessage
    @TruffleBoundary
    final ZoneId asTimeZone() throws UnsupportedMessageException {
        ZoneId tzObj = getZoneIdIntl();
        if (tzObj == null) {
            throw UnsupportedMessageException.create();
        }
        return tzObj;
    }

    @TruffleBoundary
    private ZoneId getZoneIdIntl() {
        if (timeZone instanceof JSTemporalTimeZoneObject) {
            JSTemporalTimeZoneObject tzObj = (JSTemporalTimeZoneObject) timeZone;
            return tzObj.asTimeZone();
        }
        Object tzID = JSObject.get(timeZone, TemporalConstants.TIME_ZONE);
        if (tzID instanceof TruffleString) {
            String id = ((TruffleString) tzID).toJavaStringUncached();
            return ZoneId.of(id);
        }
        return null;
    }

    @ExportMessage
    final boolean isDate() {
        return isTimeZone();
    }

    @ExportMessage
    @TruffleBoundary
    final LocalDate asDate() throws UnsupportedMessageException {
        LocalDate ld = LocalDate.ofInstant(toInstant(), asTimeZone());
        return ld;
    }

    @ExportMessage
    final boolean isTime() {
        return isTimeZone();
    }

    @ExportMessage
    @TruffleBoundary
    final LocalTime asTime() throws UnsupportedMessageException {
        LocalTime lt = LocalTime.ofInstant(toInstant(), asTimeZone());
        return lt;
    }
}
