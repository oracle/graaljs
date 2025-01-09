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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimeFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.ShowCalendar;

public final class JSTemporalPlainDateTime extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final JSTemporalPlainDateTime INSTANCE = new JSTemporalPlainDateTime();

    public static final TruffleString CLASS_NAME = Strings.constant("PlainDateTime");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("PlainDateTime.prototype");
    public static final TruffleString TO_STRING_TAG = Strings.constant("Temporal.PlainDateTime");

    private JSTemporalPlainDateTime() {
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    public static JSTemporalPlainDateTimeObject create(JSContext context, JSRealm realm,
                    int y, int m, int d, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond, TruffleString calendar) {
        return create(context, realm, INSTANCE.getIntrinsicDefaultProto(realm),
                        y, m, d, hour, minute, second, millisecond, microsecond, nanosecond, calendar);
    }

    public static JSTemporalPlainDateTimeObject create(JSContext context, JSRealm realm,
                    int y, int m, int d, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond, TruffleString calendar,
                    Node node, InlinedBranchProfile errorBranch) {
        return create(context, realm, INSTANCE.getIntrinsicDefaultProto(realm),
                        y, m, d, hour, minute, second, millisecond, microsecond, nanosecond, calendar,
                        node, errorBranch);
    }

    public static JSTemporalPlainDateTimeObject create(JSContext context, JSRealm realm, JSDynamicObject proto,
                    int y, int m, int d, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond, TruffleString calendar,
                    Node node, InlinedBranchProfile errorBranch) {
        if (!TemporalUtil.isValidISODate(y, m, d)) {
            errorBranch.enter(node);
            throw TemporalErrors.createRangeErrorDateTimeOutsideRange();
        }
        if (!TemporalUtil.isValidTime(hour, minute, second, millisecond, microsecond, nanosecond)) {
            errorBranch.enter(node);
            throw TemporalErrors.createRangeErrorDateTimeOutsideRange();
        }
        if (!TemporalUtil.isoDateTimeWithinLimits(y, m, d, hour, minute, second, millisecond, microsecond, nanosecond)) {
            errorBranch.enter(node);
            throw TemporalErrors.createRangeErrorDateTimeOutsideRange();
        }
        return createIntl(context, realm, proto, y, m, d, hour, minute, second, millisecond, microsecond, nanosecond, calendar);
    }

    public static JSTemporalPlainDateTimeObject create(JSContext context, JSRealm realm, JSDynamicObject proto,
                    int y, int m, int d, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond, TruffleString calendar) {
        return create(context, realm, proto, y, m, d, hour, minute, second, millisecond, microsecond, nanosecond, calendar, null, InlinedBranchProfile.getUncached());
    }

    @InliningCutoff
    private static JSTemporalPlainDateTimeObject createIntl(JSContext context, JSRealm realm, JSDynamicObject proto,
                    int y, int m, int d, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond, TruffleString calendar) {
        JSObjectFactory factory = context.getTemporalPlainDateTimeFactory();
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSTemporalPlainDateTimeObject(shape, proto, y, m, d, hour, minute, second, millisecond, microsecond, nanosecond, calendar), realm, proto);
        return factory.trackAllocation(newObj);
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject constructor) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(prototype, constructor);
        JSObjectUtil.putAccessorsFromContainer(realm, prototype, TemporalPlainDateTimePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalPlainDateTimePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, TO_STRING_TAG);
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainDateTime.INSTANCE, context);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainDateTimePrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalPlainDateTimeFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalPlainDateTime(Object obj) {
        return obj instanceof JSTemporalPlainDateTimeObject;
    }

    @TruffleBoundary
    public static TruffleString temporalDateTimeToString(int year, int month, int day, int hour, int minute, int second, int millisecond, int microsecond, int nanosecond,
                    Object calendar, Object precision, ShowCalendar showCalendar) {
        TruffleString yearString = TemporalUtil.padISOYear(year);
        TruffleString monthString = TemporalUtil.toZeroPaddedDecimalString(month, 2);
        TruffleString dayString = TemporalUtil.toZeroPaddedDecimalString(day, 2);
        TruffleString hourString = TemporalUtil.toZeroPaddedDecimalString(hour, 2);
        TruffleString minuteString = TemporalUtil.toZeroPaddedDecimalString(minute, 2);
        TruffleString secondString = TemporalUtil.formatSecondsStringPart(second, millisecond, microsecond, nanosecond, precision);
        TruffleString calendarString = TemporalUtil.maybeFormatCalendarAnnotation(calendar, showCalendar);
        return Strings.format("%s-%s-%sT%s:%s%s%s", yearString, monthString, dayString, hourString, minuteString, secondString, calendarString);
    }

}
