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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_WEEK;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY_OF_WEEK;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY_OF_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.IN_LEAP_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS_IN_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH_CODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEK_OF_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimeFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimePrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public final class JSTemporalPlainDateTime extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalPlainDateTime INSTANCE = new JSTemporalPlainDateTime();

    public static final String CLASS_NAME = "PlainDateTime";
    public static final String PROTOTYPE_NAME = "PlainDateTime.prototype";

    private JSTemporalPlainDateTime() {
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.PlainDateTime";
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    public static DynamicObject create(JSContext context, long y, long m, long d, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond,
                    DynamicObject calendar) {
        if (!TemporalUtil.isValidISODate(y, m, d)) {
            throw TemporalErrors.createRangeErrorDateTimeOutsideRange();
        }
        if (!TemporalUtil.isValidTime(hour, minute, second, millisecond, microsecond, nanosecond)) {
            throw TemporalErrors.createRangeErrorDateTimeOutsideRange();
        }
        if (!TemporalUtil.isoDateTimeWithinLimits(y, m, d, hour, minute, second, millisecond, microsecond, nanosecond)) {
            throw TemporalErrors.createRangeErrorDateTimeOutsideRange();
        }
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalPlainDateTimeFactory();
        DynamicObject object = factory.initProto(new JSTemporalPlainDateTimeObject(factory.getShape(realm),
                        y, m, d, hour, minute, second, millisecond, microsecond, nanosecond, calendar), realm);
        return context.trackAllocation(object);
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, constructor);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, HOUR, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, HOUR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MINUTE, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, MINUTE));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, SECOND, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, SECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MILLISECOND, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, MILLISECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MICROSECOND, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, MICROSECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, NANOSECOND, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, NANOSECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, CALENDAR, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, CALENDAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, YEAR, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTH, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, MONTH));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTH_CODE, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, MONTH_CODE));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, DAY));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY_OF_WEEK, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, DAY_OF_WEEK));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY_OF_YEAR, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, DAY_OF_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, WEEK_OF_YEAR, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, WEEK_OF_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_WEEK, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, DAYS_IN_WEEK));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_MONTH, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, DAYS_IN_MONTH));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_YEAR, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, DAYS_IN_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTHS_IN_YEAR, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, MONTHS_IN_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, IN_LEAP_YEAR, realm.lookupAccessor(TemporalPlainDateTimePrototypeBuiltins.BUILTINS, IN_LEAP_YEAR));

        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalPlainDateTimePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, "Temporal.PlainDateTime");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainDateTime.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainDateTimePrototype();
    }

    @Override
    public void fillConstructor(JSRealm realm, DynamicObject constructor) {
        WithFunctionsAndSpecies.super.fillConstructor(realm, constructor);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalPlainDateTimeFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalPlainDateTime(Object obj) {
        return obj instanceof JSTemporalPlainDateTimeObject;
    }

    public static JSTemporalDateTimeRecord addDateTime(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond, DynamicObject calendar, long years, long months, long weeks, long days, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds, DynamicObject options, JSContext ctx) {
        JSTemporalDurationRecord timeResult = TemporalUtil.addTime(hour, minute, second, millisecond, microsecond, nanosecond, hours, minutes, seconds, milliseconds, microseconds,
                        nanoseconds);
        DynamicObject datePart = JSTemporalPlainDate.create(ctx, year, month, day, calendar);
        DynamicObject dateDuration = JSTemporalDuration.createTemporalDuration(years, months, weeks, days + timeResult.getDays(), 0L, 0L, 0L, 0L, 0L, 0L, ctx);

        TemporalDate addedDate = (TemporalDate) TemporalUtil.calendarDateAdd(calendar, datePart, dateDuration, options);

        return JSTemporalDateTimeRecord.create(addedDate.getISOYear(), addedDate.getISOMonth(), addedDate.getISODay(),
                        timeResult.getHours(), timeResult.getMinutes(), timeResult.getSeconds(),
                        timeResult.getMilliseconds(), timeResult.getMicroseconds(), timeResult.getNanoseconds());
    }

    public static int compareISODateTime(long year, long month, long day, long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds, long year2, long month2,
                    long day2, long hours2, long minutes2, long seconds2, long milliseconds2, long microseconds2, long nanoseconds2) {
        int date = TemporalUtil.compareISODate(year, month, day, year2, month2, day2);
        if (date == 0) {
            return TemporalUtil.compareTemporalTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds, hours2, minutes2, seconds2, milliseconds2, microseconds2, nanoseconds2);
        }
        return date;
    }

    @TruffleBoundary
    public static String temporalDateTimeToString(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond,
                    DynamicObject calendar, Object precision, String showCalendar) {
        String yearString = TemporalUtil.padISOYear(year);
        String monthString = String.format("%1$2d", month).replace(" ", "0");
        String dayString = String.format("%1$2d", day).replace(" ", "0");
        String hourString = String.format("%1$2d", hour).replace(" ", "0");
        String minuteString = String.format("%1$2d", minute).replace(" ", "0");
        String secondString = TemporalUtil.formatSecondsStringPart(second, millisecond, microsecond, nanosecond, precision);
        String calendarID = JSRuntime.toString(calendar);
        String calendarString = TemporalUtil.formatCalendarAnnotation(calendarID, showCalendar);
        return String.format("%s-%s-%sT%s:%s%s%s", yearString, monthString, dayString, hourString, minuteString, secondString, calendarString);
    }

}
