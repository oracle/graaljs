/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.graalvm.shadowed.com.ibm.icu.impl.Grego;
import org.graalvm.shadowed.com.ibm.icu.text.DateFormat;
import org.graalvm.shadowed.com.ibm.icu.util.GregorianCalendar;
import org.graalvm.shadowed.com.ibm.icu.util.TimeZone;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.DateFunctionBuiltins;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSDate extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("Date");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Date.prototype");

    public static final JSDate INSTANCE = new JSDate();

    private static final int HOURS_PER_DAY = 24;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MS_PER_SECOND = 1000;
    public static final int MS_PER_MINUTE = 60000;
    private static final int MS_PER_HOUR = 3600000;
    public static final int MS_PER_DAY = 3600000 * 24;
    public static final double MAX_DATE = 8.64E15;
    // slightly beyond MAX_DATE (+/- 273,790 years)
    // cf. https://tc39.es/ecma262/#sec-time-values-and-time-range
    public static final double MAX_YEAR_VALUE = 300000;

    private static final int DAYS_IN_4_YEARS = 4 * 365 + 1;
    private static final int DAYS_IN_100_YEARS = 25 * DAYS_IN_4_YEARS - 1;
    private static final int DAYS_IN_400_YEARS = 4 * DAYS_IN_100_YEARS + 1;
    private static final int DAYS_FROM_1970_TO_2000 = 30 * 365 + 7;

    // Helper constants for yearFromTime(), YEAR_SHIFT must be divisible by 400
    // and represent more than 30 years plus 100,000,000 days
    private static final int YEAR_SHIFT = 280000;
    private static final int DAY_SHIFT = (YEAR_SHIFT / 400) * DAYS_IN_400_YEARS;

    public static final TruffleString INVALID_DATE_STRING = Strings.constant("Invalid Date");

    private JSDate() {
    }

    public static void setTimeMillisField(JSDateObject obj, double timeMillis) {
        assert isJSDate(obj);
        obj.setTimeMillis(timeMillis);
    }

    public static double getTimeMillisField(JSDateObject obj) {
        assert isJSDate(obj);
        return obj.getTimeMillis();
    }

    public static boolean isJSDate(Object obj) {
        return obj instanceof JSDateObject;
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public TruffleString getClassName(JSDynamicObject object) {
        return getClassName();
    }

    @Override
    public TruffleString getBuiltinToStringTag(JSDynamicObject object) {
        return getClassName(object);
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSContext ctx = realm.getContext();

        JSObject datePrototype;
        if (ctx.getEcmaScriptVersion() < 6) {
            Shape protoShape = JSShape.createPrototypeShape(realm.getContext(), INSTANCE, realm.getObjectPrototype());
            datePrototype = JSDateObject.create(protoShape, Double.NaN);
            JSObjectUtil.setOrVerifyPrototype(ctx, datePrototype, realm.getObjectPrototype());
        } else {
            datePrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        }

        JSObjectUtil.putConstructorProperty(datePrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, datePrototype, DatePrototypeBuiltins.BUILTINS);

        if (ctx.isOptionAnnexB()) {
            Object utcStringFunction = JSDynamicObject.getOrNull(datePrototype, Strings.TO_UTC_STRING);
            JSObjectUtil.putDataProperty(datePrototype, Strings.TO_GMT_STRING, utcStringFunction, JSAttributes.getDefaultNotEnumerable());
        }
        return datePrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, DateFunctionBuiltins.BUILTINS);
    }

    @TruffleBoundary
    public static double executeConstructor(double[] argsEvaluated, boolean inputIsUTC) {
        double year = argsEvaluated.length > 0 ? argsEvaluated[0] : Double.NaN;
        double month = argsEvaluated.length > 1 ? argsEvaluated[1] : 0;

        if (Double.isNaN(year) || Double.isInfinite(year) || Double.isNaN(month) || Double.isInfinite(month)) {
            return Double.NaN;
        }

        double day = getArgOrDefault(argsEvaluated, 2, 1);
        double hour = getArgOrDefault(argsEvaluated, 3, 0);
        double minute = getArgOrDefault(argsEvaluated, 4, 0);
        double second = getArgOrDefault(argsEvaluated, 5, 0);
        double ms = getArgOrDefault(argsEvaluated, 6, 0);

        return makeDate(toFullYear(year), month, day, hour, minute, second, ms, inputIsUTC ? 0 : null);
    }

    private static double getArgOrDefault(double[] argsEvaluated, int index, int def) {
        if (argsEvaluated.length > index) {
            return argsEvaluated[index];
        }
        return def;
    }

    // 15.9.1.2
    private static double day(double t) {
        return floor(t / MS_PER_DAY);
    }

    // 15.9.1.2
    private static double timeWithinDay(double t) {
        return secureNegativeModulo(t, MS_PER_DAY);
    }

    public static int dayFromYear(int y) {
        return 365 * (y - 1970) + Math.floorDiv(y - 1969, 4) - Math.floorDiv(y - 1901, 100) + Math.floorDiv(y - 1601, 400);
    }

    @TruffleBoundary
    public static int yearFromTime(long t) {
        long daysAfter1970 = Math.floorDiv(t, MS_PER_DAY);
        assert JSRuntime.longIsRepresentableAsInt(daysAfter1970);
        return yearFromDays((int) daysAfter1970);
    }

    public static int yearFromDays(int daysAfter1970) {
        // we need days relative to a year divisible by 400
        int daysAfter2000 = daysAfter1970 - DAYS_FROM_1970_TO_2000;
        // days after year (2000 - yearShift)
        int days = daysAfter2000 + DAY_SHIFT;
        // we need days > 0 to ensure that integer division rounds correctly
        assert days > 0;

        int year = 400 * (days / DAYS_IN_400_YEARS);
        int remainingDays = days % DAYS_IN_400_YEARS;
        remainingDays--;
        year += 100 * (remainingDays / DAYS_IN_100_YEARS);
        remainingDays %= DAYS_IN_100_YEARS;
        remainingDays++;
        year += 4 * (remainingDays / DAYS_IN_4_YEARS);
        remainingDays %= DAYS_IN_4_YEARS;
        remainingDays--;
        year += remainingDays / 365;

        return year - YEAR_SHIFT + 2000;
    }

    private static boolean isLeapYear(int year) {
        if (year % 4 != 0) {
            return false;
        }
        if (year % 100 != 0) {
            return true;
        }
        return year % 400 == 0;
    }

    // 15.9.1.4
    @TruffleBoundary
    public static int monthFromTime(double dt) {
        assert JSRuntime.doubleIsRepresentableAsLong(dt);
        long t = (long) dt;
        int year = yearFromTime(t);
        boolean leapYear = isLeapYear(year);
        int day = dayWithinYear(t, year);

        return monthFromTimeIntl(leapYear, day);
    }

    private static int monthFromTimeIntl(boolean leapYear, int day) {
        assert (0 <= day) && (day < (365 + (leapYear ? 1 : 0))) : "should not reach here";

        if (day < 31) {
            return 0;
        }
        if (!leapYear) {
            if (day < 59) {
                return 1;
            }
            if (day < 90) {
                return 2;
            }
            if (day < 120) {
                return 3;
            }
            if (day < 151) {
                return 4;
            }
            if (day < 181) {
                return 5;
            }
            if (day < 212) {
                return 6;
            }
            if (day < 243) {
                return 7;
            }
            if (day < 273) {
                return 8;
            }
            if (day < 304) {
                return 9;
            }
            if (day < 334) {
                return 10;
            }
            return 11;
        } else {
            if (day < 60) {
                return 1;
            }
            if (day < 91) {
                return 2;
            }
            if (day < 121) {
                return 3;
            }
            if (day < 152) {
                return 4;
            }
            if (day < 182) {
                return 5;
            }
            if (day < 213) {
                return 6;
            }
            if (day < 244) {
                return 7;
            }
            if (day < 274) {
                return 8;
            }
            if (day < 305) {
                return 9;
            }
            if (day < 335) {
                return 10;
            }
            return 11;
        }
    }

    // 15.9.1.4
    private static int dayWithinYear(long t, int year) {
        return (int) Math.floorDiv(t, MS_PER_DAY) - dayFromYear(year);
    }

    // 15.9.1.5
    @TruffleBoundary
    public static int dateFromTime(double dt) {
        assert JSRuntime.doubleIsRepresentableAsLong(dt);
        long t = (long) dt;
        int year = yearFromTime(t);
        int day = dayWithinYear(t, year);
        return dateFromDayInYear(year, day);
    }

    public static int dateFromDayInYear(int year, int day) {
        if (day < 31) {
            return day + 1;
        }
        boolean leapYear = isLeapYear(year);
        int dayMinusLeap = day - (leapYear ? 1 : 0);
        switch (monthFromTimeIntl(leapYear, day)) {
            // case 0: //handled above
            case 1:
                return day - 30;
            case 2:
                return dayMinusLeap - 58;
            case 3:
                return dayMinusLeap - 89;
            case 4:
                return dayMinusLeap - 119;
            case 5:
                return dayMinusLeap - 150;
            case 6:
                return dayMinusLeap - 180;
            case 7:
                return dayMinusLeap - 211;
            case 8:
                return dayMinusLeap - 242;
            case 9:
                return dayMinusLeap - 272;
            case 10:
                return dayMinusLeap - 303;
            case 11:
                return dayMinusLeap - 333;
        }
        assert false : "should not reach here";
        return -1;
    }

    // 15.9.1.6
    public static double weekDay(double t) {
        int result = ((int) day(t) + 4) % 7; // cast to int to avoid -0.0
        return result >= 0 ? result : result + 7;
    }

    public static double localTime(double t, Node node) {
        return t + localTZA(t, true, node);
    }

    private static double utc(double t, Node node) {
        return t - localTZA(t, false, node);
    }

    public static long localTZA(double t, boolean isUTC, Node node) {
        return localTZA(t, isUTC, JSRealm.get(node).getLocalTimeZone());
    }

    private static int getOffset(TimeZone timeZone, long date, int[] fields) {
        Grego.timeToFields(date, fields);
        return timeZone.getOffset(GregorianCalendar.AD, fields[0], fields[1], fields[2], fields[3], fields[5]);
    }

    private static int getOffset(TimeZone timeZone, long t, boolean isUTC) {
        int rawOffset = timeZone.getRawOffset();
        long date = isUTC ? (t + rawOffset) : t; // now in local standard millis

        int[] fields = new int[6];
        int offset = getOffset(timeZone, date, fields);

        if (isUTC) {
            return offset;
        }

        // getOffset() does not match the needs of ECMAScript specification
        // when local time does not exist (during STD->DST transition) or when
        // it occurs twice (during DST->STD transition). We have to check
        // for these corner cases by looking back in time. This apporach
        // is taken from TimeZone.getOffset() that does similar tricks
        // (but is not ECMAScript compliant still).

        if (offset != rawOffset) { // dstOffset != 0
            int dstOffset = offset - rawOffset;
            // dstOffset != 0 => good, we know how far back in time to look
            return getOffset(timeZone, date - dstOffset, fields);
        }

        // dstOffset = 0, look back by standard DST savings
        int dstSavings = timeZone.getDSTSavings();
        if (dstSavings == 0) {
            // getDSTSavings() returns 0 for some time-zones (like America/Sao_Paulo)
            // that stopped to use DST. Unfortunately, we may have a date that
            // used DST still => try to use the usual DST savings (1 hour)
            dstSavings = 3600000;
        }
        offset = getOffset(timeZone, date - dstSavings, fields);
        int dstOffset = offset - rawOffset;
        if (dstOffset != 0 && dstOffset != dstSavings) {
            // unexpected irregular (historical) DST => have to correct how far
            // back in time we look
            offset = getOffset(timeZone, date - dstOffset, fields);
        }
        return offset;
    }

    @TruffleBoundary
    public static int localTZA(double t, boolean isUTC, TimeZone timeZone) {
        return getOffset(timeZone, (long) t, isUTC);
    }

    // 15.9.1.10
    @TruffleBoundary
    public static int hourFromTime(double t) {
        return (int) secureNegativeModulo(floor(t / MS_PER_HOUR), HOURS_PER_DAY);
    }

    @TruffleBoundary
    public static int minFromTime(double t) {
        return (int) secureNegativeModulo(floor(t / MS_PER_MINUTE), MINUTES_PER_HOUR);
    }

    @TruffleBoundary
    public static int secFromTime(double t) {
        return (int) secureNegativeModulo(floor(t / MS_PER_SECOND), SECONDS_PER_MINUTE);
    }

    @TruffleBoundary
    public static int msFromTime(double t) {
        return (int) secureNegativeModulo(t, MS_PER_SECOND);
    }

    private static double secureNegativeModulo(double value, double modulo) {
        double result = value % modulo;
        if (result >= 0) {
            return result;
        } else {
            return result + modulo;
        }
    }

    // 15.9.1.11
    @TruffleBoundary
    public static double makeTime(double hour, double min, double sec, double ms) {
        if (!isFinite(hour) || !isFinite(min) || !isFinite(sec) || !isFinite(ms)) {
            return Double.NaN;
        }
        double h = JSRuntime.truncateDouble(hour);
        double m = JSRuntime.truncateDouble(min);
        double s = JSRuntime.truncateDouble(sec);
        double milli = JSRuntime.truncateDouble(ms);
        return h * MS_PER_HOUR + m * MS_PER_MINUTE + s * MS_PER_SECOND + milli;
    }

    // 15.9.1.12
    @TruffleBoundary
    public static double makeDay(double year, double month, double date) {
        if (!isFinite(year) || !isFinite(month) || !isFinite(date)) {
            return Double.NaN;
        }
        double y = JSRuntime.truncateDouble(year);
        double m = JSRuntime.truncateDouble(month);
        double dt = JSRuntime.truncateDouble(date);

        double ym = y + floor(m / 12);
        int mn = (int) (m % 12);
        if (mn < 0) {
            mn += 12;
        }

        if (ym < -MAX_YEAR_VALUE || ym > MAX_YEAR_VALUE) {
            return Double.NaN;
        }

        double t = LocalDate.of((int) ym, mn + 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        return day(t) + dt - 1;
    }

    // 15.9.1.13
    @TruffleBoundary
    public static double makeDate(double day, double time) {
        if (!isFinite(day) || !isFinite(time)) {
            return Double.NaN;
        }
        return (day * MS_PER_DAY + time);
    }

    @TruffleBoundary
    public static double makeDate(double y, double m, double d, double h, double min, double sec, double ms, Integer timezone) {
        double day = makeDay(y, m, d);
        double time = makeTime(h, min, sec, ms);
        double date = makeDate(day, time);

        if (timezone == null) {
            date = utc(date, null);
        } else {
            date -= timezone * 60000;
        }
        return timeClip(date);
    }

    /**
     * Implementation of ECMAScript 5.1 15.9.1.14 TimeClip.
     */
    public static double timeClip(double time) {
        if (Double.isInfinite(time) || Double.isNaN(time) || Math.abs(time) > MAX_DATE) {
            return Double.NaN;
        }
        // The standard expects only integer values, cf. 15.9.1.1
        // it does not state, however, WHERE the conversion should happen
        return ((Double) time).longValue();
    }

    // helper function
    private static boolean isFinite(double d) {
        return !(Double.isNaN(d) || Double.isInfinite(d));
    }

    // helper function
    private static double floor(double d) {
        return Math.floor(d);
    }

    public static JSDateObject create(JSContext context, JSRealm realm, double timeMillis) {
        JSObjectFactory factory = context.getDateFactory();
        JSDateObject obj = JSDateObject.create(factory.getShape(realm), timeMillis);
        factory.initProto(obj, realm);
        return context.trackAllocation(obj);
    }

    public static double setTime(JSDateObject thisDate, double time) {
        double v = timeClip(time);
        setTimeMillisField(thisDate, v);
        return v;
    }

    public static double setMilliseconds(JSDateObject thisDate, double ms, boolean isUTC, Node node) {
        double t = localTime(getTimeMillisField(thisDate), isUTC, node);
        double time = makeTime(hourFromTime(t), minFromTime(t), secFromTime(t), ms);
        double u = timeClip(utc(makeDate(day(t), time), isUTC, node));
        setTimeMillisField(thisDate, u);
        return u;
    }

    public static double setSeconds(JSDateObject thisDate, double s, double ms, boolean msSpecified, boolean isUTC, Node node) {
        double t = localTime(getTimeMillisField(thisDate), isUTC, node);
        double milli = msSpecified ? ms : msFromTime(t);
        double date = makeDate(day(t), makeTime(hourFromTime(t), minFromTime(t), s, milli));
        double u = timeClip(utc(date, isUTC, node));
        setTimeMillisField(thisDate, u);
        return u;
    }

    public static double setMinutes(JSDateObject thisDate, double m, double s, boolean sSpecified, double ms, boolean msSpecified, boolean isUTC, Node node) {
        double t = localTime(getTimeMillisField(thisDate), isUTC, node);
        double milli = msSpecified ? ms : msFromTime(t);
        double sec = sSpecified ? s : secFromTime(t);
        double date = makeDate(day(t), makeTime(hourFromTime(t), m, sec, milli));
        double u = timeClip(utc(date, isUTC, node));
        setTimeMillisField(thisDate, u);
        return u;
    }

    public static double setHours(JSDateObject thisDate, double h, double m, boolean mSpecified, double s, boolean sSpecified, double ms, boolean msSpecified, boolean isUTC, Node node) {
        double t = localTime(getTimeMillisField(thisDate), isUTC, node);
        double milli = msSpecified ? ms : msFromTime(t);
        double sec = sSpecified ? s : secFromTime(t);
        double min = mSpecified ? m : minFromTime(t);
        double date = makeDate(day(t), makeTime(h, min, sec, milli));
        double u = timeClip(utc(date, isUTC, node));
        setTimeMillisField(thisDate, u);
        return u;
    }

    public static double setDate(JSDateObject thisDate, double date, boolean isUTC, Node node) {
        double t = localTime(getTimeMillisField(thisDate), isUTC, node);
        double u;
        if (Double.isNaN(t)) {
            u = Double.NaN;
        } else {
            double newDate = makeDate(makeDay(yearFromTime((long) t), monthFromTime(t), date), timeWithinDay(t));
            u = timeClip(utc(newDate, isUTC, node));
        }
        setTimeMillisField(thisDate, u);
        return u;
    }

    public static double setMonth(JSDateObject thisDate, double month, double date, boolean dateSpecified, boolean isUTC, Node node) {
        double t = localTime(getTimeMillisField(thisDate), isUTC, node);
        double newDate;
        if (Double.isNaN(t)) {
            newDate = Double.NaN;
        } else {
            double dt = dateSpecified ? date : dateFromTime(t);
            newDate = timeClip(utc(makeDate(makeDay(yearFromTime((long) t), month, dt), timeWithinDay(t)), isUTC, node));
        }
        setTimeMillisField(thisDate, newDate);
        return newDate;
    }

    public static double setFullYear(JSDateObject thisDate, double year, double month, boolean monthSpecified, double date, boolean dateSpecified, boolean isUTC, Node node) {
        double timeFieldValue = getTimeMillisField(thisDate);
        double t = Double.isNaN(timeFieldValue) ? 0 : localTime(timeFieldValue, isUTC, node);
        double dt = dateSpecified ? date : dateFromTime(t);
        double m = monthSpecified ? month : monthFromTime(t);
        double newDate = makeDate(makeDay(year, m, dt), timeWithinDay(t));
        double u = timeClip(utc(newDate, isUTC, node));
        setTimeMillisField(thisDate, u);
        return u;
    }

    public static double setYear(JSDateObject thisDate, double year, Node node) {
        double t = getTimeMillisField(thisDate);
        t = Double.isNaN(t) ? 0 : localTime(t, node); // cf. B.2.5, clause 1
        if (Double.isNaN(year)) {
            setTimeMillisField(thisDate, Double.NaN);
            return Double.NaN;
        }
        double fullYear = toFullYear(year);
        double r5 = makeDay(fullYear, monthFromTime(t), dateFromTime(t));
        double r6 = timeClip(utc(makeDate(r5, timeWithinDay(t)), node));
        setTimeMillisField(thisDate, r6);
        return r6;
    }

    private static double toFullYear(double year) {
        // 0 <= ToInteger(year) <= 99 according to standard, but we are omitting the ToInteger here!
        if (-1 < year && year < 100) {
            return 1900 + (int) year;
        }
        return year;
    }

    @TruffleBoundary
    public static TruffleString format(DateFormat format, double time) {
        return Strings.fromJavaString(format.format(time));
    }

    public static TruffleString toString(double time, JSRealm realm) {
        if (Double.isNaN(time)) {
            return INVALID_DATE_STRING;
        }
        return format(realm.getDateToStringFormat(), time);
    }

    public static TruffleString toISOStringIntl(double time, JSRealm realm) {
        return format(realm.getJSDateISOFormat(time), time);
    }

    public static boolean isTimeValid(double time) {
        return !(Double.isNaN(time) || Double.isInfinite(time));
    }

    private static double localTime(double time, boolean isUTC, Node node) {
        return isUTC ? time : localTime(time, node);
    }

    private static double utc(double time, boolean isUTC, Node node) {
        return isUTC ? time : utc(time, node);
    }

    public static boolean isValidDate(JSDateObject date) {
        return !Double.isNaN(getTimeMillisField(date));
    }

    @TruffleBoundary
    public static Instant asInstant(JSDateObject date) {
        assert isValidDate(date);
        return Instant.ofEpochMilli((long) getTimeMillisField(date));
    }

    @TruffleBoundary
    public static LocalDate asLocalDate(JSDateObject date, JSRealm realm) {
        return LocalDate.from(asInstant(date).atZone(realm.getLocalTimeZoneId()));
    }

    @TruffleBoundary
    public static LocalTime asLocalTime(JSDateObject date, JSRealm realm) {
        return LocalTime.from(asInstant(date).atZone(realm.getLocalTimeZoneId()));
    }

    public static double getDateValueFromInstant(Object receiver, InteropLibrary interop) {
        Instant instant;
        try {
            instant = interop.asInstant(receiver);
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(receiver, e, "asInstant", null);
        }
        try {
            return instant.toEpochMilli();
        } catch (ArithmeticException e) {
            return Double.NaN;
        }
    }

    @TruffleBoundary
    @Override
    public TruffleString toDisplayStringImpl(JSDynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        double time = getTimeMillisField((JSDateObject) obj);
        TruffleString formattedDate;
        if (isTimeValid(time)) {
            formattedDate = toISOStringIntl(time, JSRealm.get(null));
        } else {
            formattedDate = INVALID_DATE_STRING;
        }
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return Strings.concatAll(Strings.BRACKET_DATE_SPC, formattedDate, Strings.BRACKET_CLOSE);
        } else {
            return formattedDate;
        }
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getDatePrototype();
    }
}
