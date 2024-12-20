/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

@ExportLibrary(InteropLibrary.class)
public final class JSTemporalZonedDateTimeObject extends JSTemporalCalendarHolder {

    private final BigInt nanoseconds;
    private final TruffleString timeZone;

    protected JSTemporalZonedDateTimeObject(Shape shape, JSDynamicObject proto, BigInt nanoseconds, TruffleString timeZone, Object calendar) {
        super(shape, proto, calendar);
        assert TemporalUtil.isValidEpochNanoseconds(nanoseconds);
        this.nanoseconds = nanoseconds;
        this.timeZone = timeZone;
    }

    public BigInt getNanoseconds() {
        return nanoseconds;
    }

    public TruffleString getTimeZone() {
        return timeZone;
    }

    @TruffleBoundary
    private Instant toInstant() {
        BigInt[] res = nanoseconds.divideAndRemainder(TemporalUtil.BI_NS_PER_SECOND);
        return Instant.ofEpochSecond(res[0].longValue(), res[1].intValue());
    }

    @ExportMessage(name = "isTimeZone")
    @ExportMessage(name = "isDate")
    @ExportMessage(name = "isTime")
    @TruffleBoundary
    boolean isTimeZone() {
        return getZoneIdIntl() != null;
    }

    @ExportMessage
    @TruffleBoundary
    ZoneId asTimeZone() throws UnsupportedMessageException {
        ZoneId tzObj = getZoneIdIntl();
        if (tzObj == null) {
            throw UnsupportedMessageException.create();
        }
        return tzObj;
    }

    @TruffleBoundary
    private ZoneId getZoneIdIntl() {
        String id = timeZone.toJavaStringUncached();
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(id);
        } catch (DateTimeException ex) {
            zoneId = null;
        }
        return zoneId;
    }

    @ExportMessage
    @TruffleBoundary
    LocalDate asDate() throws UnsupportedMessageException {
        return LocalDate.ofInstant(toInstant(), asTimeZone());
    }

    @ExportMessage
    @TruffleBoundary
    LocalTime asTime() throws UnsupportedMessageException {
        return LocalTime.ofInstant(toInstant(), asTimeZone());
    }

    @Override
    public TruffleString getClassName() {
        return JSTemporalZonedDateTime.TO_STRING_TAG;
    }
}
