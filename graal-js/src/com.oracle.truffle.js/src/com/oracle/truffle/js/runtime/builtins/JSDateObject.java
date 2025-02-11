/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

@ExportLibrary(InteropLibrary.class)
public final class JSDateObject extends JSNonProxyObject {
    private double value;

    protected JSDateObject(Shape shape, JSDynamicObject proto, double value) {
        super(shape, proto);
        this.value = value;
    }

    public double getTimeMillis() {
        return value;
    }

    public void setTimeMillis(double value) {
        this.value = value;
    }

    public static JSDateObject create(Shape shape, JSDynamicObject proto, double value) {
        return new JSDateObject(shape, proto, value);
    }

    @Override
    public TruffleString getClassName() {
        return getBuiltinToStringTag();
    }

    @Override
    public TruffleString getBuiltinToStringTag() {
        return JSDate.CLASS_NAME;
    }

    @ExportMessage(name = "isDate")
    @ExportMessage(name = "isTime")
    @ExportMessage(name = "isTimeZone")
    protected boolean isDate() {
        return JSDate.isValidDate(this);
    }

    @ExportMessage
    public LocalDate asDate(
                    @CachedLibrary("this") InteropLibrary self) throws UnsupportedMessageException {
        if (isDate()) {
            return JSDate.asLocalDate(this, JSRealm.get(self));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    public LocalTime asTime(
                    @CachedLibrary("this") InteropLibrary self) throws UnsupportedMessageException {
        if (isDate()) {
            return JSDate.asLocalTime(this, JSRealm.get(self));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    public ZoneId asTimeZone(
                    @CachedLibrary("this") InteropLibrary self) throws UnsupportedMessageException {
        if (isDate()) {
            return JSRealm.get(self).getLocalTimeZoneId();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @TruffleBoundary
    @Override
    public TruffleString toDisplayStringImpl(boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        double time = getTimeMillis();
        TruffleString formattedDate;
        if (JSDate.isTimeValid(time)) {
            formattedDate = JSDate.toISOStringIntl(time, JSRealm.get(null));
        } else {
            formattedDate = JSDate.INVALID_DATE_STRING;
        }
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return Strings.concatAll(Strings.BRACKET_DATE_SPC, formattedDate, Strings.BRACKET_CLOSE);
        } else {
            return formattedDate;
        }
    }
}
