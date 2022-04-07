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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_WEEK;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY_OF_WEEK;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY_OF_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.IN_LEAP_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS_IN_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH_CODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEK_OF_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
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
import com.oracle.truffle.js.runtime.util.TemporalUtil.Overflow;
import com.oracle.truffle.js.runtime.util.TemporalUtil.ShowCalendar;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

public final class JSTemporalPlainDate extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalPlainDate INSTANCE = new JSTemporalPlainDate();

    public static final TruffleString CLASS_NAME = Strings.constant("PlainDate");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("PlainDate.prototype");
    public static final TruffleString TO_STRING_TAG = Strings.constant("Temporal.PlainDate");

    private JSTemporalPlainDate() {
    }

    @Override
    public TruffleString getClassName(JSDynamicObject object) {
        return TO_STRING_TAG;
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject constructor) {
        JSContext ctx = realm.getContext();
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, constructor);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, CALENDAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, CALENDAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, YEAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTH, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, MONTH));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTH_CODE, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, MONTH_CODE));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, DAY));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY_OF_WEEK, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, DAY_OF_WEEK));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY_OF_YEAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, DAY_OF_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, WEEK_OF_YEAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, WEEK_OF_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_WEEK, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, DAYS_IN_WEEK));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_MONTH, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, DAYS_IN_MONTH));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_YEAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, DAYS_IN_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTHS_IN_YEAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, MONTHS_IN_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, IN_LEAP_YEAR, realm.lookupAccessor(TemporalPlainDatePrototypeBuiltins.BUILTINS, IN_LEAP_YEAR));

        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalPlainDatePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, TO_STRING_TAG);
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainDate.INSTANCE, context);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainDatePrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalPlainDateFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalPlainDate(Object obj) {
        return obj instanceof JSTemporalPlainDateObject;
    }

    public static JSTemporalPlainDateObject create(JSContext context, int year, int month, int day, JSDynamicObject calendar, BranchProfile errorBranch) {
        if (!TemporalUtil.validateISODate(year, month, day)) {
            errorBranch.enter();
            throw TemporalErrors.createRangeErrorDateTimeOutsideRange();
        }
        if (!TemporalUtil.isoDateTimeWithinLimits(year, month, day, 12, 0, 0, 0, 0, 0)) {
            errorBranch.enter();
            throw TemporalErrors.createRangeErrorDateOutsideRange();
        }
        return createIntl(context, year, month, day, calendar);
    }

    public static JSTemporalPlainDateObject create(JSContext context, int year, int month, int day, JSDynamicObject calendar) {
        if (!TemporalUtil.validateISODate(year, month, day)) {
            throw TemporalErrors.createRangeErrorDateTimeOutsideRange();
        }
        if (!TemporalUtil.isoDateTimeWithinLimits(year, month, day, 12, 0, 0, 0, 0, 0)) {
            throw TemporalErrors.createRangeErrorDateOutsideRange();
        }
        return createIntl(context, year, month, day, calendar);
    }

    private static JSTemporalPlainDateObject createIntl(JSContext context, int year, int month, int day, JSDynamicObject calendar) {
        JSRealm realm = JSRealm.get(null);
        JSObjectFactory factory = context.getTemporalPlainDateFactory();
        JSTemporalPlainDateObject object = factory.initProto(new JSTemporalPlainDateObject(factory.getShape(realm),
                        year, month, day, calendar), realm);
        return context.trackAllocation(object);
    }

    // 3.5.5
    public static JSTemporalDurationRecord differenceISODate(int y1, int m1, int d1, int y2, int m2, int d2, Unit largestUnit) {
        assert largestUnit == Unit.YEAR || largestUnit == Unit.MONTH || largestUnit == Unit.WEEK || largestUnit == Unit.DAY;
        if (largestUnit == Unit.YEAR || largestUnit == Unit.MONTH) {
            int sign = -TemporalUtil.compareISODate(y1, m1, d1, y2, m2, d2);
            if (sign == 0) {
                return toRecordWeeksPlural(0, 0, 0, 0);
            }
            JSTemporalDateTimeRecord start = toRecord(y1, m1, d1);
            JSTemporalDateTimeRecord end = toRecord(y2, m2, d2);
            int years = end.getYear() - start.getYear();
            JSTemporalDateTimeRecord mid = TemporalUtil.addISODate(y1, m1, d1, years, 0, 0, 0, Overflow.CONSTRAIN);
            int midSign = -TemporalUtil.compareISODate(mid.getYear(), mid.getMonth(), mid.getDay(), y2, m2, d2);
            if (midSign == 0) {
                if (largestUnit == Unit.YEAR) {
                    return toRecordWeeksPlural(years, 0, 0, 0);
                } else {
                    return toRecordWeeksPlural(0, years * 12, 0, 0); // sic!
                }
            }
            int months = end.getMonth() - start.getMonth();
            if (midSign != sign) {
                years = years - sign;
                months = months + (sign * 12);
            }
            mid = TemporalUtil.addISODate(y1, m1, d1, years, months, 0, 0, Overflow.CONSTRAIN);
            midSign = -TemporalUtil.compareISODate(mid.getYear(), mid.getMonth(), mid.getDay(), y2, m2, d2);
            if (midSign == 0) {
                if (largestUnit == Unit.YEAR) {
                    return toRecordPlural(years, months, 0);
                } else {
                    return toRecordWeeksPlural(0, months + (years * 12), 0, 0); // sic!
                }
            }
            if (midSign != sign) {
                months = months - sign;
                if (months == -sign) {
                    years = years - sign;
                    months = 11 * sign;
                }
                mid = TemporalUtil.addISODate(y1, m1, d1, years, months, 0, 0, Overflow.CONSTRAIN);
                midSign = -TemporalUtil.compareISODate(mid.getYear(), mid.getMonth(), mid.getDay(), y2, m2, d2);
            }
            int days = 0;
            if (mid.getMonth() == end.getMonth() && mid.getYear() == end.getYear()) {
                days = end.getDay() - mid.getDay();
            } else if (sign < 0) {
                days = -mid.getDay() - (TemporalUtil.isoDaysInMonth(end.getYear(), end.getMonth()) - end.getDay());
            } else {
                days = end.getDay() + (TemporalUtil.isoDaysInMonth(mid.getYear(), mid.getMonth()) - mid.getDay());
            }
            if (largestUnit == Unit.MONTH) {
                months = months + (years * 12);
                years = 0;
            }
            return toRecordWeeksPlural(years, months, 0, days);
        }
        if (largestUnit == Unit.DAY || largestUnit == Unit.WEEK) {
            JSTemporalDateTimeRecord smaller;
            JSTemporalDateTimeRecord greater;
            int sign;
            if (TemporalUtil.compareISODate(y1, m1, d1, y2, m2, d2) < 0) {
                smaller = toRecord(y1, m1, d1);
                greater = toRecord(y2, m2, d2);
                sign = 1;
            } else {
                smaller = toRecord(y2, m2, d2);
                greater = toRecord(y1, m1, d1);
                sign = -1;
            }
            int years = greater.getYear() - smaller.getYear();
            int days = TemporalUtil.toISODayOfYear(greater.getYear(), greater.getMonth(), greater.getDay()) - TemporalUtil.toISODayOfYear(smaller.getYear(), smaller.getMonth(), smaller.getDay());
            assert years >= 0;
            while (years > 0) {
                days = days + TemporalUtil.isoDaysInYear(smaller.getYear() + years - 1);
                years = years - 1;
            }
            int weeks = 0;
            if (largestUnit == Unit.WEEK) {
                weeks = Math.floorDiv(days, 7);
                days = days % 7;
            }
            return toRecordWeeksPlural(0, 0, weeks * sign, days * sign);
        }
        CompilerDirectives.transferToInterpreter();
        throw Errors.shouldNotReachHere("unexpected largest unit: " + largestUnit);
    }

    private static JSTemporalDurationRecord toRecordPlural(long year, long month, long day) {
        return JSTemporalDurationRecord.create(year, month, day, 0, 0, 0, 0, 0, 0);
    }

    private static JSTemporalDurationRecord toRecordWeeksPlural(long year, long month, long weeks, long day) {
        return JSTemporalDurationRecord.createWeeks(year, month, weeks, day, 0, 0, 0, 0, 0, 0);
    }

    public static JSTemporalDateTimeRecord toRecord(int year, int month, int day) {
        return JSTemporalDateTimeRecord.create(year, month, day, 0, 0, 0, 0, 0, 0);
    }

    @TruffleBoundary
    public static TruffleString temporalDateToString(JSTemporalPlainDateObject date, ShowCalendar showCalendar) {
        TruffleString yearString = TemporalUtil.padISOYear(date.getYear());
        TruffleString monthString = Strings.format("%1$02d", date.getMonth());
        TruffleString dayString = Strings.format("%1$02d", date.getDay());

        TruffleString calendarID = JSRuntime.toString(date.getCalendar());
        Object calendar = TemporalUtil.formatCalendarAnnotation(calendarID, showCalendar);

        return Strings.format("%s-%s-%s%s", yearString, monthString, dayString, calendar);
    }

}
