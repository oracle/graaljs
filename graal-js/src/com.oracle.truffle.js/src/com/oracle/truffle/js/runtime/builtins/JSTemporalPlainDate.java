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
package com.oracle.truffle.js.runtime.builtins;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTES;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;

import java.util.Collections;
import java.util.Set;

import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class JSTemporalPlainDate extends JSNonProxy implements JSConstructorFactory.Default.WithSpecies,
                PrototypeSupplier {

    public static final JSTemporalPlainDate INSTANCE = new JSTemporalPlainDate();

    public static final String CLASS_NAME = "TemporalPlainDate";
    public static final String PROTOTYPE_NAME = "TemporalPlainDate.prototype";

    private JSTemporalPlainDate() {
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.PlainDate";
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putToStringTag(prototype, "Temporal.PlainDate");
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainDate.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainDatePrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static boolean isJSTemporalPlainDate(Object obj) {
        return obj instanceof JSTemporalPlainDateObject;
    }

    public static DynamicObject createTemporalDate(JSContext context, long y, long m, long d, DynamicObject calendar) {
        rejectDate(y, m, d);
        if (!dateTimeWithinLimits(y, m, d, 12, 0, 0, 0, 0, 0)) {
            throw Errors.createRangeError("Date is not within range.");
        }
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalPlainDateFactory();
        DynamicObject object = factory.initProto(new JSTemporalPlainDateObject(factory.getShape(realm),
                        (int) y, (int) m, (int) d, calendar), realm);
        return context.trackAllocation(object);
    }

    private static void rejectDate(long year, long month, long day) {
        if (!validateDate(year, month, day)) {
            throw Errors.createRangeError("Given date outside the range.");
        }
    }

    private static boolean validateDate(long year, long month, long day) {
        if (month < 1 || month > 12) {
            return false;
        }
        long daysInMonth = daysInMonth(year, month);
        if (day < 1 || day > daysInMonth) {
            return false;
        }
        return true;
    }

    private static int daysInMonth(long year, long month) {
        assert month >= 1;
        assert month <= 12;
        if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {
            return 31;
        }
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        }
        if (isLeapYear(year)) {
            return 29;
        }
        return 28;
    }

    private static boolean isLeapYear(long year) {
        if (year % 4 != 0) {
            return false;
        }
        if (year % 400 == 0) {
            return true;
        }
        if (year % 100 == 0) {
            return false;
        }
        return true;
    }

    private static boolean dateTimeWithinLimits(long year, long month, long day, long hour, long minute, long second,
                    long millisecond, long microsecond, long nanosecond) {
        long ns = getEpochFromParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
        if ((ns / 100_000_000_000_000L) <= -864_000L - 864L) {
            return false;
        } else if ((ns / 100_000_000_000_000L) >= 864_000L + 864L) {
            return false;
        }
        return true;
    }

    private static long getEpochFromParts(long year, long month, long day, long hour, long minute, long second,
                    long millisecond, long microsecond, long nanosecond) {
        assert month >= 1 && month <= 12;
        assert day >= 1 && day <= daysInMonth(year, month);
        assert JSTemporalPlainTime.validateTime(hour, minute, second, millisecond, microsecond, nanosecond);
        double date = JSDate.makeDay(year, month, day);
        double time = JSDate.makeTime(hour, minute, second, millisecond);
        double ms = JSDate.makeDate(date, time);
        assert isFinite(ms);
        return (long) ((ms * 1_000_000) + (microsecond * 1_000) + nanosecond);
    }

    private static boolean isFinite(double d) {
        return !(Double.isNaN(d) || Double.isInfinite(d));
    }

    // 3.5.3
    public static Object createTemporalDateFromStatic(DynamicObject constructor, long isoYear, long isoMonth, long isoDay,
                    DynamicObject calendar, IsConstructorNode isConstructor,
                    JSFunctionCallNode callNode) {
        assert validateISODate(isoYear, isoMonth, isoDay);
        if (!isConstructor.executeBoolean(constructor)) {
            throw Errors.createTypeError("Given constructor is not an constructor.");
        }
        Object[] ctorArgs = new Object[]{isoYear, isoMonth, isoDay, calendar};
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, ctorArgs.length);
        System.arraycopy(ctorArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, ctorArgs.length);
        Object result = callNode.executeCall(args);
        return result;
    }

    // 3.5.4
    public static DynamicObject toTemporalDate(DynamicObject item, DynamicObject optionsParam,
                    JSRealm realm, IsObjectNode isObject, DynamicObjectLibrary dol,
                    JSToBooleanNode toBoolean, JSToStringNode toString) {
        try {
            DynamicObject options = optionsParam != null ? optionsParam : JSObjectUtil.createOrdinaryPrototypeObject(realm);
            if (isObject.executeBoolean(item)) {
                if (isJSTemporalPlainDate(item)) {
                    return item;
                }
                DynamicObject calendar = TemporalUtil.getOptionalTemporalCalendar(item);
                Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{"day", "month", "monthCode", "year"});
                DynamicObject fields = toTemporalDateFields(item, fieldNames, realm, isObject, dol);
                return TemporalUtil.dateFromFields(calendar, fields, options);
            }
            String overflows = TemporalUtil.toTemporalOverflow(options, dol, isObject, toBoolean, toString);
            DynamicObject result = TemporalUtil.parseTemporalDateString(toString.executeString(item));
            if (!validateISODate(dol.getLongOrDefault(result, YEAR, 0),
                            dol.getLongOrDefault(result, MONTH, 0), dol.getLongOrDefault(result, DAY, 0))) {
                throw Errors.createRangeError("Given date is not valid.");
            }
            DynamicObject calendar = TemporalUtil.getOptionalTemporalCalendar(item);
            result = regulateISODate(dol.getLongOrDefault(result, YEAR, 0),
                            dol.getLongOrDefault(result, MONTH, 0), dol.getLongOrDefault(result, DAY, 0),
                            overflows, realm);

            return createTemporalDate(realm.getContext(), dol.getLongOrDefault(result, YEAR, 0),
                            dol.getLongOrDefault(result, MONTH, 0), dol.getLongOrDefault(result, DAY, 0),
                            calendar);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 3.5.5
    public static DynamicObject differenceISODate(long y1, long m1, long d1, long y2, long m2, long d2, String largestUnitParam,
                    JSRealm realm, DynamicObjectLibrary dol) {
        try {
            String largestUnit = largestUnitParam;
            assert largestUnit.equals(YEARS) || largestUnit.equals(MONTHS) ||
                            largestUnit.equals(WEEKS) || largestUnit.equals(DAYS) ||
                            largestUnit.equals(HOURS) || largestUnit.equals(MINUTES) ||
                            largestUnit.equals(SECONDS);
            if (!largestUnit.equals(YEARS) && !largestUnit.equals(MONTHS) && !largestUnit.equals(WEEKS)) {
                largestUnit = DAYS;
            }
            if (largestUnit.equals(YEARS) || largestUnit.equals(MONTHS)) {
                long sign = compareISODate(y1, m1, d1, y2, m2, d2);
                if (sign == 0) {
                    return toRecord(0, 0, 0, realm);
                }
                DynamicObject start = toRecord(y1, m1, d1, realm);
                DynamicObject end = toRecord(y2, m2, d2, realm);
                long years = dol.getLongOrDefault(end, YEAR, 0L) - dol.getLongOrDefault(start, YEAR, 0L);
                DynamicObject mid = addISODate(y1, m1, d1, years, 0, 0, 0, "constrain", realm, dol);
                long midSign = compareISODate(
                                dol.getLongOrDefault(mid, YEAR, 0L),
                                dol.getLongOrDefault(mid, MONTH, 0L),
                                dol.getLongOrDefault(mid, DAY, 0L),
                                y2, m2, d2);
                if (midSign == 0) {
                    DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
                    if (largestUnit.equals(YEARS)) {
                        JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, years);
                        JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, 0);
                    } else {
                        JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, 0);
                        JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, years * 12);
                    }
                    JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, 0);
                    JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, 0);
                    return record;
                }
                long months = dol.getLongOrDefault(end, MONTH, 0L) - dol.getLongOrDefault(start, MONTH, 0L);
                if (midSign != sign) {
                    years = years - sign;
                    months = months + (sign * 12);
                }
                mid = addISODate(y1, m1, d1, years, months, 0, 0, "constrain", realm, dol);
                midSign = compareISODate(
                                dol.getLongOrDefault(mid, YEAR, 0L),
                                dol.getLongOrDefault(mid, MONTH, 0L),
                                dol.getLongOrDefault(mid, DAY, 0L),
                                y2, m2, d2);
                if (midSign == 0) {
                    DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
                    if (largestUnit.equals(YEARS)) {
                        JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, years);
                        JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, months);
                    } else {
                        JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, 0);
                        JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, months + (years * 12));
                    }
                    JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, 0);
                    JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, 0);
                    return record;
                }
                if (midSign != sign) {
                    months = months - sign;
                    if (months == -sign) {
                        years = years - sign;
                        months = 11 * sign;
                    }
                    mid = addISODate(y1, m1, d1, years, months, 0, 0, "constrain", realm, dol);
                    midSign = compareISODate(
                                    dol.getLongOrDefault(mid, YEAR, 0L),
                                    dol.getLongOrDefault(mid, MONTH, 0L),
                                    dol.getLongOrDefault(mid, DAY, 0L),
                                    y2, m2, d2);
                }
                long days = 0;
                if (dol.getLongOrDefault(mid, MONTH, 0L) == dol.getLongOrDefault(end, MONTH, 0L) && dol.getLongOrDefault(mid, YEAR, 0L) == dol.getLongOrDefault(end, YEAR, 0L)) {
                    days = dol.getLongOrDefault(end, DAY, 0L) - dol.getLongOrDefault(mid, DAY, 0L);
                } else if (sign < 0) {
                    days = -dol.getLongOrDefault(mid, DAY, 0L) -
                                    (JSTemporalCalendar.isoDaysInMonth(
                                                    dol.getLongOrDefault(end, YEAR, 0L), dol.getLongOrDefault(end, MONTH, 0L)) - dol.getLongOrDefault(end, DAY, 0L));
                } else {
                    days = dol.getLongOrDefault(end, DAY, 0L) +
                                    (JSTemporalCalendar.isoDaysInMonth(
                                                    dol.getLongOrDefault(mid, YEAR, 0L), dol.getLongOrDefault(mid, MONTH, 0L)) - dol.getLongOrDefault(mid, DAY, 0L));
                }
                if (largestUnit.equals(MONTHS)) {
                    months = months + (years * 12);
                    years = 0;
                }
                DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
                JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, years);
                JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, months);
                JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, 0);
                JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, days);
                return record;
            }
            if (largestUnit.equals(DAYS) || largestUnit.equals(WEEKS)) {
                DynamicObject smaller;
                DynamicObject greater;
                long sign;
                if (compareISODate(y1, m1, d1, y2, m2, d2) < 0) {
                    smaller = toRecord(y1, m1, d1, realm);
                    greater = toRecord(y2, m2, d2, realm);
                    sign = 1;
                } else {
                    smaller = toRecord(y2, m2, d2, realm);
                    greater = toRecord(y1, m1, d1, realm);
                    sign = -1;
                }
                long years = dol.getLongOrDefault(greater, YEAR, 0L) - dol.getLongOrDefault(smaller, YEAR, 0L);
                long days = JSTemporalCalendar.toISODayOfYear(
                                dol.getLongOrDefault(greater, YEAR, 0L),
                                dol.getLongOrDefault(greater, MONTH, 0L),
                                dol.getLongOrDefault(greater, DAY, 0L)) -
                                JSTemporalCalendar.toISODayOfYear(
                                                dol.getLongOrDefault(smaller, YEAR, 0L),
                                                dol.getLongOrDefault(smaller, MONTH, 0L),
                                                dol.getLongOrDefault(smaller, DAY, 0L));
                assert years >= 0;
                while (years > 0) {
                    days = days + JSTemporalCalendar.isoDaysInYear(
                                    dol.getLongOrDefault(smaller, YEAR, 0L) + years - 1);
                    years = years - 1;
                }
                long weeks = 0;
                if (largestUnit.equals(WEEKS)) {
                    weeks = Math.floorDiv(days, 7);
                    days = days % 7;
                }
                DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
                JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, 0);
                JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, 0);
                JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, weeks * sign);
                JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, days * sign);
                return record;
            }
            return null;
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 3.5.6
    public static DynamicObject toTemporalDateFields(DynamicObject temporalDateLike, Set<String> fieldNames, JSRealm realm, IsObjectNode isObject, DynamicObjectLibrary dol) {
        return TemporalUtil.prepareTemporalFields(temporalDateLike, fieldNames, Collections.emptySet(), realm, isObject, dol);
    }

    // 3.5.7
    public static DynamicObject regulateISODate(long year, long month, long day, String overflow, JSRealm realm) {
        assert overflow.equals("constrain") || overflow.equals("reject");
        if (overflow.equals("reject")) {
            rejectISODate(year, month, day);
            return toRecord(year, month, day, realm);
        }
        if (overflow.equals("constrain")) {
            return constrainISODate(year, month, day, realm);
        }
        throw new RuntimeException("This should never have happened.");
    }

    // 3.5.8
    public static boolean validateISODate(long year, long month, long day) {
        if (month < 1 || month > 12) {
            return false;
        }
        long daysInMonth = JSTemporalCalendar.isoDaysInMonth(year, month);
        if (day < 1 || day > daysInMonth) {
            return false;
        }
        return true;
    }

    // 3.5.9
    public static void rejectISODate(long year, long month, long day) {
        if (!validateISODate(year, month, day)) {
            throw Errors.createRangeError("Given date is not valid.");
        }
    }

    // 3.5.10
    public static DynamicObject constrainISODate(long year, long month, long day, JSRealm realm) {
        long monthPrepared = TemporalUtil.constrainToRange(month, 1, 12);
        long dayPrepared = TemporalUtil.constrainToRange(day, 1, TemporalUtil.isoDaysInMonth(year, month));
        return toRecord(year, monthPrepared, dayPrepared, realm);
    }

    // 3.5.11
    public static DynamicObject balanceISODate(long yearParam, long monthParam, long dayParam, JSRealm realm, DynamicObjectLibrary dol) {
        try {
            DynamicObject balancedYearMonth = TemporalUtil.balanceISOYearMonth(yearParam, monthParam, realm);
            long month = dol.getLongOrDefault(balancedYearMonth, MONTH, 0L);
            long year = dol.getLongOrDefault(balancedYearMonth, YEAR, 0L);
            long day = dayParam;
            long testYear;
            if (month > 2) {
                testYear = year;
            } else {
                testYear = year - 1;
            }
            while (day < -1 * JSTemporalCalendar.isoDaysInYear(testYear)) {
                day = day + JSTemporalCalendar.isoDaysInYear(testYear);
                year = year - 1;
                testYear = testYear - 1;
            }
            testYear = year + 1;
            while (day > JSTemporalCalendar.isoDaysInYear(testYear)) {
                day = day - JSTemporalCalendar.isoDaysInYear(testYear);
                year = year + 1;
                testYear = testYear + 1;
            }
            while (day < 1) {
                balancedYearMonth = TemporalUtil.balanceISOYearMonth(year, month - 1, realm);
                year = dol.getLongOrDefault(balancedYearMonth, YEAR, 0L);
                month = dol.getLongOrDefault(balancedYearMonth, MONTH, 0L);
                day = day + JSTemporalCalendar.isoDaysInMonth(year, month);
            }
            while (day > JSTemporalCalendar.isoDaysInMonth(year, month)) {
                day = day - JSTemporalCalendar.isoDaysInMonth(year, month);
                balancedYearMonth = TemporalUtil.balanceISOYearMonth(year, month + 1, realm);
                year = dol.getLongOrDefault(balancedYearMonth, YEAR, 0L);
                month = dol.getLongOrDefault(balancedYearMonth, MONTH, 0L);
            }
            return toRecord(year, month, day, realm);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 3.5.14
    public static DynamicObject addISODate(long year, long month, long day, long years, long months, long weeks,
                    long days, String overflow, JSRealm realm, DynamicObjectLibrary dol) {
        try {
            assert overflow.equals("constrain") || overflow.equals("reject");
            long y = year + years;
            long m = month + months;
            DynamicObject intermediate = TemporalUtil.balanceISOYearMonth(y, m, realm);
            intermediate = regulateISODate(dol.getLongOrDefault(intermediate, YEAR, 0L),
                            dol.getLongOrDefault(intermediate, MONTH, 0L), day, overflow, realm);
            long d = dol.getLongOrDefault(intermediate, DAY, 0L) + (days + (7 * weeks));
            intermediate = balanceISODate(dol.getLongOrDefault(intermediate, YEAR, 0L),
                            dol.getLongOrDefault(intermediate, MONTH, 0L), d, realm, dol);
            return regulateISODate(dol.getLongOrDefault(intermediate, YEAR, 0L),
                            dol.getLongOrDefault(intermediate, MONTH, 0L),
                            dol.getLongOrDefault(intermediate, DAY, 0L), overflow, realm);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 3.5.15
    public static long compareISODate(long y1, long m1, long d1, long y2, long m2, long d2) {
        if (y1 > y2) {
            return 1;
        }
        if (y1 < y2) {
            return -1;
        }
        if (m1 > m2) {
            return 1;
        }
        if (m1 < m2) {
            return -1;
        }
        if (d1 > d2) {
            return 1;
        }
        if (d1 < d2) {
            return -1;
        }
        return 0;
    }

    private static DynamicObject toRecord(long year, long month, long day, JSRealm realm) {
        DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putDataProperty(realm.getContext(), record, YEAR, year);
        JSObjectUtil.putDataProperty(realm.getContext(), record, MONTH, month);
        JSObjectUtil.putDataProperty(realm.getContext(), record, DAY, day);
        return record;
    }
}
