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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.AUTO;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTES;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.getLong;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltins;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public final class JSTemporalDuration extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalDuration INSTANCE = new JSTemporalDuration();

    public static final String CLASS_NAME = "TemporalDuration";
    public static final String PROTOTYPE_NAME = "TemporalDuration.prototype";

    public static final String SIGN = "sign";
    public static final String BLANK = "blank";

    public static final String[] PROPERTIES = new String[]{
                    DAYS, HOURS, MICROSECONDS, MILLISECONDS, MINUTES, MONTHS,
                    TemporalConstants.NANOSECONDS, TemporalConstants.SECONDS, TemporalConstants.WEEKS, TemporalConstants.YEARS
    };

    private JSTemporalDuration() {
    }

    public static DynamicObject create(JSContext context, long years, long months, long weeks, long days, long hours,
                    long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        if (!validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds,
                        nanoseconds)) {
            throw Errors.createRangeError("Given duration outside range.");
        }
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalDurationFactory();
        DynamicObject obj = factory.initProto(new JSTemporalDurationObject(factory.getShape(realm),
                        years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds), realm);
        return context.trackAllocation(obj);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.Duration";
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, constructor);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, YEARS, realm.lookupAccessor(TemporalDurationPrototypeBuiltins.BUILTINS, YEARS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTHS, realm.lookupAccessor(TemporalDurationPrototypeBuiltins.BUILTINS, MONTHS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, WEEKS, realm.lookupAccessor(TemporalDurationPrototypeBuiltins.BUILTINS, WEEKS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS, realm.lookupAccessor(TemporalDurationPrototypeBuiltins.BUILTINS, DAYS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, HOURS, realm.lookupAccessor(TemporalDurationPrototypeBuiltins.BUILTINS, HOURS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MINUTES, realm.lookupAccessor(TemporalDurationPrototypeBuiltins.BUILTINS, MINUTES));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, SECONDS, realm.lookupAccessor(TemporalDurationPrototypeBuiltins.BUILTINS, SECONDS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MILLISECONDS, realm.lookupAccessor(TemporalDurationPrototypeBuiltins.BUILTINS, MILLISECONDS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MICROSECONDS, realm.lookupAccessor(TemporalDurationPrototypeBuiltins.BUILTINS, MICROSECONDS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, NANOSECONDS, realm.lookupAccessor(TemporalDurationPrototypeBuiltins.BUILTINS, NANOSECONDS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, SIGN, realm.lookupAccessor(TemporalDurationPrototypeBuiltins.BUILTINS, SIGN));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, BLANK, realm.lookupAccessor(TemporalDurationPrototypeBuiltins.BUILTINS, BLANK));
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalDurationPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, "Temporal.Duration");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalDuration.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalDurationPrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalDurationFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalDuration(Object obj) {
        return obj instanceof JSTemporalDurationObject;
    }

    // region Abstract methods
    // 7.2.1
    public static Object toTemporalDuration(Object item, JSContext ctx, IsObjectNode isObject, JSToIntegerAsLongNode toInt, JSToStringNode toString) {
        JSTemporalPlainDateTimeRecord result;
        if (isObject.executeBoolean(item)) {
            if (isJSTemporalDuration(item)) {
                return item;
            }
            result = toTemporalDurationRecord((DynamicObject) item, isObject, toInt);
        } else {
            String string = toString.executeString(item);
            result = parseTemporalDurationString(string);
        }
        return createTemporalDuration(result.getYear(), result.getMonth(), result.getWeeks(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(),
                        result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), ctx);
    }

    @TruffleBoundary
    private static JSTemporalPlainDateTimeRecord parseTemporalDurationString(String string) {

        // PT1H10M29.876543211S
        // P1Y1M1W1DT1H1M1.123456789S

        // Pattern regex =
        // Pattern.compile("^([\\+-])+[Pp]([Tt]((\\d+)[Hh])?((\\d+)[Mm])?((\\d+)[Ss])?)$");

        long year = 0;
        long month = 0;
        long day = 0;
        long week = 0;
        double second = 0;
        double hour = 0;
        double minute = 0;
        double millisecond = 0;
        double microsecond = 0;
        double nanosecond = 0;
        double fHour = 0;
        double fMinute = 0;

        // (\\d+[Yy])?(\\d+[Mm])?(\\d+[Ww])?(\\d+[Dd])?
        Pattern regex = Pattern.compile("^([\\+-]?)[Pp]([Tt]([\\d.]+[Hh])?([\\d.]+[Mm])?([\\d.]+[Ss])?)?$");
        Matcher matcher = regex.matcher(string);
        if (matcher.matches()) {
            String sign = matcher.group(1);

            int count = 1;
            boolean datePart = true;
            while (++count < matcher.groupCount()) {
                String val = matcher.group(count);
                if (val == null) {
                    // why?
                } else if (val.startsWith("T") || val.startsWith("t")) {
                    datePart = false;
                } else {
                    char id = val.charAt(val.length() - 1);
                    String numstr = val.substring(0, val.length() - 1);

                    if (id == 'Y' || id == 'y') {
                        year = Long.parseLong(numstr);
                    } else if (id == 'M' || id == 'm') {
                        if (datePart) {
                            month = Long.parseLong(numstr);
                        } else {
                            minute = Double.parseDouble(numstr);
                        }
                    } else if (id == 'D' || id == 'd') {
                        day = Long.parseLong(numstr);
                    } else if (id == 'W' || id == 'w') {
                        week = Long.parseLong(numstr);
                    } else if (id == 'H' || id == 'h') {
                        hour = Double.parseDouble(numstr);
                    } else if (id == 'S' || id == 's') {
                        second = Double.parseDouble(numstr);
                    } else {
                        throw Errors.createTypeError("malformed Duration");
                    }
                }
            }

            int factor = (sign.equals("-") || sign.equals("\u2212")) ? -1 : 1;

            year = year * factor;
            month = month * factor;
            week = week * factor;
            day = day * factor;
            hour = hour * factor;
            minute = minute * factor;
            second = second * factor;
            if (hasFraction(second)) {
                double fpart = fractionPart(second) * 1000;
                millisecond = factor * (fpart % 1000);
                fpart *= 1000;
                microsecond = factor * (fpart % 1000);
                fpart *= 1000;
                nanosecond = factor * (fpart % 1000);
            } else {
                millisecond = 0;
                microsecond = 0;
                nanosecond = 0;
            }
            if (hasFraction(hour)) {
                double fpart = fractionPart(hour);
                fHour = factor * fpart;
                // b. Set fHours to ! ToIntegerOrInfinity(fraction) * factor / 10 raised to the
                // power of the length of fHours.
            } else {
                fHour = 0;
            }
            if (hasFraction(minute)) {
                double fpart = fractionPart(minute);
                fMinute = factor * fpart;
                // b. Set fMinutes to ! ToIntegerOrInfinity(fraction) * factor / 10 raised to the
                // power of the length of fMinutes.
            } else {
                fMinute = 0;
            }
            JSTemporalPlainDateTimeRecord result = TemporalUtil.durationHandleFractions(fHour, minute, fMinute, second, 0, millisecond, 0, microsecond, 0, nanosecond, 0);
            return JSTemporalPlainDateTimeRecord.createWeeks(year, month, week, day, (long) hour,
                            result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
        }
        throw Errors.createTypeError("malformed Duration");
    }

    private static double fractionPart(double second) {
        return second - ((int) second);
    }

    private static boolean hasFraction(double val) {
        return !JSRuntime.doubleIsRepresentableAsInt(val);
    }

    // 7.5.2
    public static JSTemporalPlainDateTimeRecord toTemporalDurationRecord(DynamicObject temporalDurationLike, IsObjectNode isObject, JSToIntegerAsLongNode toInt) {
        assert isObject.executeBoolean(temporalDurationLike);
        if (isJSTemporalDuration(temporalDurationLike)) {
            JSTemporalDurationObject d = (JSTemporalDurationObject) temporalDurationLike;
            return JSTemporalPlainDateTimeRecord.createWeeks(d.getYears(), d.getMonths(), d.getWeeks(), d.getDays(), d.getHours(), d.getMinutes(), d.getSeconds(), d.getMilliseconds(),
                            d.getMicroseconds(), d.getNanoseconds());
        }
        boolean any = false;
        long year = 0;
        long month = 0;
        long week = 0;
        long day = 0;
        long hour = 0;
        long minute = 0;
        long second = 0;
        long millis = 0;
        long micros = 0;
        long nanos = 0;
        for (String property : PROPERTIES) {
            Object val = JSObject.get(temporalDurationLike, property);
            if (val != Undefined.instance) {
                any = true;
            }
            long lVal = toInt.executeLong(val);
            // TODO check for integer property before converting!
            switch (property) {
                case YEARS:
                    year = lVal;
                    break;
                case MONTHS:
                    month = lVal;
                    break;
                case WEEKS:
                    week = lVal;
                    break;
                case DAYS:
                    day = lVal;
                    break;
                case HOURS:
                    hour = lVal;
                    break;
                case MINUTES:
                    minute = lVal;
                    break;
                case SECONDS:
                    second = lVal;
                    break;
                case MILLISECONDS:
                    millis = lVal;
                    break;
                case MICROSECONDS:
                    micros = lVal;
                    break;
                case NANOSECONDS:
                    nanos = lVal;
                    break;

                default:
                    throw Errors.unsupported("wrong type");
            }
        }
        if (!any) {
            throw Errors.createTypeError("Given duration like object has no duration properties.");
        }
        return JSTemporalPlainDateTimeRecord.createWeeks(year, month, week, day, hour, minute, second, millis, micros, nanos);
    }

    // 7.5.3
    public static int durationSign(long years, long months, long weeks, long days, long hours, long minutes,
                    long seconds, long milliseconds, long microseconds, long nanoseconds) {
        if (years < 0) {
            return -1;
        }
        if (years > 0) {
            return 1;
        }
        if (months < 0) {
            return -1;
        }
        if (months > 1) {
            return 1;
        }
        if (weeks < 0) {
            return -1;
        }
        if (weeks > 0) {
            return 1;
        }
        if (days < 0) {
            return -1;
        }
        if (days > 0) {
            return 1;
        }
        if (hours < 0) {
            return -1;
        }
        if (hours > 0) {
            return 1;
        }
        if (minutes < 0) {
            return -1;
        }
        if (minutes > 0) {
            return 1;
        }
        if (seconds < 0) {
            return -1;
        }
        if (seconds > 0) {
            return 1;
        }
        if (milliseconds < 0) {
            return -1;
        }
        if (milliseconds > 0) {
            return 1;
        }
        if (microseconds < 0) {
            return -1;
        }
        if (microseconds > 0) {
            return 1;
        }
        if (nanoseconds < 0) {
            return -1;
        }
        if (nanoseconds > 0) {
            return 1;
        }
        return 0;
    }

    // 7.5.4
    public static void rejectDurationSign(long years, long months, long weeks, long days, long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        long sign = durationSign(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        if (years < 0 && sign > 0) {
            throw Errors.createRangeError("Years is negative but it should be positive.");
        }
        if (years > 0 && sign < 0) {
            throw Errors.createRangeError("Years is positive but it should be negative.");
        }
        if (months < 0 && sign > 0) {
            throw Errors.createRangeError("Months is negative but it should be positive.");
        }
        if (months > 0 && sign < 0) {
            throw Errors.createRangeError("Months is positive but it should be negative.");
        }
        if (weeks < 0 && sign > 0) {
            throw Errors.createRangeError("Weeks is negative but it should be positive.");
        }
        if (weeks > 0 && sign < 0) {
            throw Errors.createRangeError("Weeks is positive but it should be negative.");
        }
        if (days < 0 && sign > 0) {
            throw Errors.createRangeError("Days is negative but it should be positive.");
        }
        if (days > 0 && sign < 0) {
            throw Errors.createRangeError("Days is positive but it should be negative.");
        }
        if (hours < 0 && sign > 0) {
            throw Errors.createRangeError("Hours is negative but it should be positive.");
        }
        if (hours > 0 && sign < 0) {
            throw Errors.createRangeError("Hours is positive but it should be negative.");
        }
        if (minutes < 0 && sign > 0) {
            throw Errors.createRangeError("Minutes is negative but it should be positive.");
        }
        if (minutes > 0 && sign < 0) {
            throw Errors.createRangeError("Minutes is positive but it should be negative.");
        }
        if (seconds < 0 && sign > 0) {
            throw Errors.createRangeError("Seconds is negative but it should be positive.");
        }
        if (seconds > 0 && sign < 0) {
            throw Errors.createRangeError("Seconds is positive but it should be negative.");
        }
        if (milliseconds < 0 && sign > 0) {
            throw Errors.createRangeError("Milliseconds is negative but it should be positive.");
        }
        if (milliseconds > 0 && sign < 0) {
            throw Errors.createRangeError("Milliseconds is positive but it should be negative.");
        }
        if (microseconds < 0 && sign > 0) {
            throw Errors.createRangeError("Microseconds is negative but it should be positive.");
        }
        if (microseconds > 0 && sign < 0) {
            throw Errors.createRangeError("Microseconds is positive but it should be negative.");
        }
        if (nanoseconds < 0 && sign > 0) {
            throw Errors.createRangeError("Nanoseconds is negative but it should be positive.");
        }
        if (nanoseconds > 0 && sign < 0) {
            throw Errors.createRangeError("Nanoseconds is positive but it should be negative.");
        }
    }

    // 7.5.5
    public static boolean validateTemporalDuration(long years, long months, long weeks, long days, long hours,
                    long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds) {
        int sign = durationSign(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds,
                        nanoseconds);
        if (years < 0 && sign > 0) {
            return false;
        }
        if (years > 0 && sign < 0) {
            return false;
        }
        if (months < 0 && sign > 0) {
            return false;
        }
        if (months > 0 && sign < 0) {
            return false;
        }
        if (weeks < 0 && sign > 0) {
            return false;
        }
        if (weeks > 0 && sign < 0) {
            return false;
        }
        if (days < 0 && sign > 0) {
            return false;
        }
        if (days > 0 && sign < 0) {
            return false;
        }
        if (hours < 0 && sign > 0) {
            return false;
        }
        if (hours > 0 && sign < 0) {
            return false;
        }
        if (minutes < 0 && sign > 0) {
            return false;
        }
        if (minutes > 0 && sign < 0) {
            return false;
        }
        if (seconds < 0 && sign > 0) {
            return false;
        }
        if (seconds > 0 && sign < 0) {
            return false;
        }
        if (milliseconds < 0 && sign > 0) {
            return false;
        }
        if (milliseconds > 0 && sign < 0) {
            return false;
        }
        if (microseconds < 0 && sign > 0) {
            return false;
        }
        if (microseconds > 0 && sign < 0) {
            return false;
        }
        if (nanoseconds < 0 && sign > 0) {
            return false;
        }
        if (nanoseconds > 0 && sign < 0) {
            return false;
        }
        return true;
    }

    // 7.5.6
    public static String defaultTemporalLargestUnit(long years, long months, long weeks, long days, long hours,
                    long minutes, long seconds, long milliseconds, long microseconds) {
        if (years != 0) {
            return YEARS;
        }
        if (months != 0) {
            return MONTHS;
        }
        if (weeks != 0) {
            return WEEKS;
        }
        if (days != 0) {
            return DAYS;
        }
        if (hours != 0) {
            return HOURS;
        }
        if (minutes != 0) {
            return MINUTES;
        }
        if (seconds != 0) {
            return SECONDS;
        }
        if (milliseconds != 0) {
            return MILLISECONDS;
        }
        if (microseconds != 0) {
            return MICROSECONDS;
        }
        return NANOSECONDS;
    }

    // 7.5.7
    public static DynamicObject toPartialDuration(DynamicObject temporalDurationLike, JSContext ctx,
                    IsObjectNode isObjectNode, JSToIntegerAsLongNode toInt) {
        if (!isObjectNode.executeBoolean(temporalDurationLike)) {
            throw Errors.createTypeError("Given duration like is not a object.");
        }
        DynamicObject result = JSOrdinary.create(ctx);
        boolean any = false;
        for (String property : PROPERTIES) {
            Object value = JSObject.get(temporalDurationLike, property);
            if (value != Undefined.instance) {
                any = true;
                JSObjectUtil.putDataProperty(ctx, result, property, toInt.executeLong(value));
            }
        }
        if (!any) {
            throw Errors.createTypeError("Given duration like object has no duration properties.");
        }
        return result;
    }

    // 7.5.8
    public static DynamicObject createTemporalDuration(long years, long months, long weeks, long days, long hours,
                    long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds, JSContext ctx) {
        if (!validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
            throw Errors.createRangeError("Duration not valid.");
        }
        return create(ctx, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    // 7.5.9
    public static JSTemporalDurationObject createTemporalDurationFromInstance(long years, long months, long weeks, long days,
                    long hours, long minutes, long seconds,
                    long milliseconds, long microseconds,
                    long nanoseconds, JSRealm realm,
                    JSFunctionCallNode callNode) {
        assert validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        DynamicObject constructor = realm.getTemporalDurationConstructor();
        Object[] ctorArgs = new Object[]{years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds};
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, ctorArgs.length);
        System.arraycopy(ctorArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, ctorArgs.length);
        Object result = callNode.executeCall(args);
        return (JSTemporalDurationObject) result;
    }

    // 7.5.11
    @SuppressWarnings("unused")
    public static long calculateOffsetShift(DynamicObject relativeTo, long y, long mon, long w, long d, long h, long min,
                    long s, long ms, long mus, long ns, IsObjectNode isObject) {
        if (isObject.executeBoolean(relativeTo)) { // TODO: Check if there is an internal slot for
                                                   // InitializedTemporalZoneDateTime
            return 0;
        }
        DynamicObject instant = null;   // TODO: Call JSTemporalInstant.createTemporalInstant()
        long offsetBefore = 0;          // TODO: Call JSTemporalTimeZone.getOffsetNanoSecondsFor()
        long after = 0;                 // TODO: Call JSTemporalZonedDateTime.addZonedDateTime()
        DynamicObject instantAfter = null;  // TODO: Call JSTemporalInstant.createTemporalInstant()
        long offsetAfter = 0;           // TODO: Call JSTemporalTimeZone.getOffsetNanoSecondsFor()
        return offsetAfter - offsetBefore;
    }

    // 7.5.12
    public static long totalDurationNanoseconds(long days, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds, long offsetShift) {
        long ns = nanoseconds;
        if (days != 0) {
            ns -= offsetShift;
        }
        long h = hours + days * 24;
        long min = minutes + h * 60;
        long s = seconds + min * 60;
        long ms = milliseconds + s * 1000;
        long mus = microseconds + ms * 1000;
        return ns + mus * 1000;
    }

    // 7.5.13
    public static JSTemporalPlainDateTimePluralRecord balanceDuration(long days, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds, String largestUnit, DynamicObject relativeTo) {
        long ns;
        if (TemporalUtil.isTemporalZonedDateTime(relativeTo)) {
            // TODO implement that branch
            ns = nanoseconds;
        } else {
            ns = totalDurationNanoseconds(days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
        }
        long d;
        if (largestUnit.equals(YEARS) || largestUnit.equals(MONTHS) || largestUnit.equals(WEEKS) || largestUnit.equals(DAYS)) {
            d = days;
        } else {
            d = 0;
        }
        long h = 0;
        long min = 0;
        long s = 0;
        long ms = 0;
        long mus = 0;
        long sign = ns < 0 ? -1 : 1;
        ns = Math.abs(ns);
        if (largestUnit.equals(YEARS) || largestUnit.equals(MONTHS) || largestUnit.equals(WEEKS) ||
                        largestUnit.equals(DAYS) || largestUnit.equals(HOURS)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
            ms = Math.floorDiv(mus, 1000);
            mus = mus % 1000;
            s = Math.floorDiv(ms, 1000);
            ms = ms % 1000;
            min = Math.floorDiv(s, 60);
            s = s % 60;
            h = Math.floorDiv(min, 60);
            min = min % 60;
        } else if (largestUnit.equals(MINUTES)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
            ms = Math.floorDiv(mus, 1000);
            mus = mus % 1000;
            s = Math.floorDiv(ms, 1000);
            ms = ms % 1000;
            min = Math.floorDiv(s, 60);
            s = s % 60;
        } else if (largestUnit.equals(SECONDS)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
            ms = Math.floorDiv(mus, 1000);
            mus = mus % 1000;
            s = Math.floorDiv(ms, 1000);
            ms = ms % 1000;
        } else if (largestUnit.equals(MILLISECONDS)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
            ms = Math.floorDiv(mus, 1000);
            mus = mus % 1000;
        } else if (largestUnit.equals(MICROSECONDS)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
        } else {
            assert largestUnit.equals(NANOSECONDS);
        }

        return JSTemporalPlainDateTimePluralRecord.create(0, 0, d, h * sign, min * sign, s * sign, ms * sign, mus * sign, ns * sign);
    }

    // 7.5.14
    public static JSTemporalPlainDateTimePluralRecord unbalanceDurationRelative(long y, long m, long w, long d,
                    String largestUnit, DynamicObject relTo, JSContext ctx) {
        long years = y;
        long months = m;
        long weeks = w;
        long days = d;
        DynamicObject relativeTo = relTo;
        if (largestUnit.equals(YEARS) || (years == 0 && months == 0 && weeks == 0 && days == 0)) {
            return JSTemporalPlainDateTimePluralRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        }
        long sign = JSTemporalDuration.durationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        assert sign != 0;
        DynamicObject oneYear = JSTemporalDuration.createTemporalDuration(sign, 0, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
        DynamicObject oneMonth = JSTemporalDuration.createTemporalDuration(0, sign, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
        DynamicObject oneWeek = JSTemporalDuration.createTemporalDuration(0, 0, sign, 0, 0, 0, 0, 0, 0, 0, ctx);
        DynamicObject calendar = Undefined.instance;
        if (relativeTo != Undefined.instance) {
            assert JSObject.hasProperty(relativeTo, CALENDAR);
            calendar = (DynamicObject) JSObject.get(relativeTo, CALENDAR);
        }
        if (largestUnit.equals(MONTHS)) {
            if (calendar == Undefined.instance) {
                throw Errors.createRangeError("No calendar provided.");
            }
            DynamicObject dateAdd = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject dateUntil = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_UNTIL);

            while (Math.abs(years) > 0) {
                DynamicObject addOptions = JSOrdinary.createWithNullPrototype(ctx);
                DynamicObject newRelativeTo = TemporalUtil.calendarDateAdd(calendar, relativeTo, oneYear, addOptions, dateAdd);

                DynamicObject untilOptions = JSOrdinary.createWithNullPrototype(ctx);
                JSObjectUtil.putDataProperty(ctx, untilOptions, "largestUnit", MONTHS);
                DynamicObject untilResult = TemporalUtil.calendarDateUntil(calendar, relativeTo, newRelativeTo, untilOptions, dateUntil);
                long oneYearMonths = getLong(untilResult, MONTHS, 0);
                relativeTo = newRelativeTo;
                years = years - sign;
                months = months + oneYearMonths;
            }
        } else if (largestUnit.equals(WEEKS)) {
            if (calendar == Undefined.instance) {
                throw Errors.createRangeError("Calendar should be not undefined.");
            }
            while (Math.abs(years) > 0) {
                DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneYear, ctx);
                relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
                long oneYearDays = getLong(moveResult, DAYS, 0);
                years = years - sign;
                days = days + oneYearDays;
            }
            while (Math.abs(months) > 0) {
                DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
                relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
                long oneMonthDays = getLong(moveResult, DAYS, 0);
                months = months - sign;
                days = days + oneMonthDays;
            }
        } else {
            if (years != 0 || months != 0 || days != 0) {
                if (calendar == Undefined.instance) {
                    throw Errors.createRangeError("Calendar should be not undefined.");
                }
                while (Math.abs(years) > 0) {
                    DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneYear, ctx);
                    relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
                    long oneYearDays = getLong(moveResult, DAYS, 0);
                    years = years - sign;
                    days = days + oneYearDays;
                }
                while (Math.abs(months) > 0) {
                    DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
                    relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
                    long oneMonthDays = getLong(moveResult, DAYS, 0);
                    months = months - sign;
                    days = days + oneMonthDays;
                }
                while (Math.abs(weeks) > 0) {
                    DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, ctx);
                    relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
                    long oneWeekDays = getLong(moveResult, DAYS, 0);
                    weeks = weeks - sign;
                    days = days + oneWeekDays;
                }
            }
        }
        return JSTemporalPlainDateTimePluralRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    // 7.5.15
    public static JSTemporalPlainDateTimePluralRecord balanceDurationRelative(long y, long m, long w, long d, String largestUnit, DynamicObject relTo, JSContext ctx) {
        long years = y;
        long months = m;
        long weeks = w;
        long days = d;
        DynamicObject relativeTo = relTo;
        if ((!largestUnit.equals(YEARS) && !largestUnit.equals(MONTHS) && !largestUnit.equals(WEEKS)) || (years == 0 && months == 0 && weeks == 0 && days == 0)) {
            return JSTemporalPlainDateTimePluralRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        }
        long sign = durationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        assert sign != 0;
        DynamicObject oneYear = createTemporalDuration(sign, 0, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
        DynamicObject oneMonth = createTemporalDuration(0, sign, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
        DynamicObject oneWeek = createTemporalDuration(0, 0, sign, 0, 0, 0, 0, 0, 0, 0, ctx);
        if (relativeTo == Undefined.instance) {
            throw Errors.createRangeError("RelativeTo should not be null.");
        }
        assert JSObject.hasProperty(relativeTo, "calendar");
        DynamicObject calendar = (DynamicObject) JSObject.get(relativeTo, "calendar");
        if (largestUnit.equals(YEARS)) {

            DynamicObject untilOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(ctx, untilOptions, "largestUnit", MONTHS);

            DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
            relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
            long oneYearDays = getLong(moveResult, DAYS, 0);
            while (Math.abs(days) >= Math.abs(oneYearDays)) {
                days = days - oneYearDays;
                years = years + sign;
                moveResult = moveRelativeDate(calendar, relativeTo, oneYear, ctx);
                relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
                oneYearDays = getLong(moveResult, DAYS, 0);
            }
            moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
            relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
            long oneMonthDays = getLong(moveResult, DAYS, 0);
            while (Math.abs(days) >= Math.abs(oneMonthDays)) {
                days = days - oneMonthDays;
                months = months + sign;
                moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
                relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
                oneMonthDays = getLong(moveResult, DAYS, 0);
            }

            DynamicObject dateAdd = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject addOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject newRelativeTo = TemporalUtil.calendarDateAdd(calendar, relativeTo, oneYear, addOptions, dateAdd);

            DynamicObject dateUntil = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_UNTIL);
            untilOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject untilResult = TemporalUtil.calendarDateUntil(calendar, relativeTo, newRelativeTo, untilOptions, dateUntil);

            long oneYearMonths = getLong(untilResult, MONTHS, 0);
            while (Math.abs(months) >= Math.abs((oneYearMonths))) {
                months = months - oneYearMonths;
                years = years + sign;
                relativeTo = newRelativeTo;

                addOptions = JSOrdinary.createWithNullPrototype(ctx);
                newRelativeTo = TemporalUtil.calendarDateAdd(calendar, relativeTo, oneYear, addOptions, dateAdd);
                untilOptions = JSOrdinary.createWithNullPrototype(ctx);
                untilResult = TemporalUtil.calendarDateUntil(calendar, relativeTo, newRelativeTo, untilOptions, dateUntil);
                oneYearMonths = getLong(untilResult, MONTHS, 0);
            }
        } else if (largestUnit.equals(MONTHS)) {
            DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
            relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
            long oneMonthDays = getLong(moveResult, DAYS);
            while (Math.abs(days) >= Math.abs(oneMonthDays)) {
                days = days - oneMonthDays;
                months = months + sign;
                moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
                relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
                oneMonthDays = getLong(moveResult, DAYS, 0);
            }
        } else {
            assert largestUnit.equals(WEEKS);
            DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, ctx);
            relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
            long oneWeekDays = getLong(moveResult, DAYS, 0);
            while (Math.abs(days) >= Math.abs(oneWeekDays)) {
                days = days - oneWeekDays;
                weeks = weeks + sign;
                moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, ctx);
                relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
                oneWeekDays = getLong(moveResult, DAYS, 0);
            }
        }
        return JSTemporalPlainDateTimePluralRecord.createWeeks(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
    }

    // 7.5.16
    public static JSTemporalPlainDateTimePluralRecord addDuration(long y1, long mon1, long w1, long d1, long h1, long min1, long s1, long ms1, long mus1, long ns1,
                    long y2, long mon2, long w2, long d2, long h2, long min2, long s2, long ms2, long mus2, long ns2,
                    DynamicObject relativeTo, JSContext ctx) {
        String largestUnit1 = defaultTemporalLargestUnit(y1, mon1, w1, d1, h1, min1, s1, ms1, mus1);
        String largestUnit2 = defaultTemporalLargestUnit(y2, mon2, w2, d2, h2, min2, s2, ms2, mus2);
        String largestUnit = TemporalUtil.largerOfTwoTemporalDurationUnits(largestUnit1, largestUnit2);
        long years = 0;
        long months = 0;
        long weeks = 0;
        long days = 0;
        long hours = 0;
        long minutes = 0;
        long seconds = 0;
        long milliseconds = 0;
        long microseconds = 0;
        long nanoseconds = 0;
        if (relativeTo == Undefined.instance) {
            if (largestUnit.equals(YEARS) || largestUnit.equals(MONTHS) || largestUnit.equals(WEEKS)) {
                throw Errors.createRangeError("Largest unit allowed with no relative is 'days'.");
            }
            JSTemporalPlainDateTimePluralRecord result = balanceDuration(d1 + d2, h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2,
                            ns1 + ns2, largestUnit, Undefined.instance);
            years = 0;
            months = 0;
            weeks = 0;
            days = result.getDays();
            hours = result.getHours();
            minutes = result.getMinutes();
            seconds = result.getSeconds();
            milliseconds = result.getMilliseconds();
            microseconds = result.getMicroseconds();
            nanoseconds = result.getNanoseconds();
        } else if (JSTemporalPlainDate.isJSTemporalPlainDate(relativeTo)) {
            DynamicObject calendar = (DynamicObject) JSObject.get(relativeTo, CALENDAR);
            DynamicObject datePart = JSTemporalPlainDate.createTemporalDate(ctx,
                            getLong(relativeTo, "ISOYear", 0L),
                            getLong(relativeTo, "ISOMonth", 0L),
                            getLong(relativeTo, "ISODay", 0L),
                            (DynamicObject) JSObject.get(relativeTo, "Calendar"));
            DynamicObject dateDuration1 = JSTemporalDuration.createTemporalDuration(y1, mon1, w1, d1, 0, 0, 0, 0, 0, 0, ctx);
            DynamicObject dateDuration2 = JSTemporalDuration.createTemporalDuration(y2, mon2, w2, d2, 0, 0, 0, 0, 0, 0, ctx);

            DynamicObject dateAdd = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject firstAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject intermediate = TemporalUtil.calendarDateAdd(calendar, datePart, dateDuration1, firstAddOptions, dateAdd);

            DynamicObject secondAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject end = TemporalUtil.calendarDateAdd(calendar, intermediate, dateDuration2, secondAddOptions, dateAdd);

            String dateLargestUnit = TemporalUtil.largerOfTwoTemporalDurationUnits("days", largestUnit);

            DynamicObject differenceOptions = JSOrdinary.createWithNullPrototype(ctx);
            JSObjectUtil.putDataProperty(ctx, differenceOptions, "largestUnit", dateLargestUnit);
            DynamicObject dateDifference = TemporalUtil.calendarDateUntil(calendar, datePart, end, differenceOptions, Undefined.instance);
            JSTemporalPlainDateTimePluralRecord result = JSTemporalDuration.balanceDuration(getLong(dateDifference, DAYS, 0L),
                            h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2, ns1 + ns2, largestUnit, Undefined.instance);
            years = getLong(dateDifference, YEARS);
            months = getLong(dateDifference, MONTHS);
            weeks = getLong(dateDifference, WEEKS);
            days = result.getDays();
            hours = result.getHours();
            minutes = result.getMinutes();
            seconds = result.getSeconds();
            milliseconds = result.getMilliseconds();
            microseconds = result.getMicroseconds();
            nanoseconds = result.getNanoseconds();
        } else {
            // TODO: Handle ZonedDateTime
        }
        if (!validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds,
                        microseconds, nanoseconds)) {
            throw Errors.createRangeError("Duration out of range!");
        }
        return JSTemporalPlainDateTimePluralRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    // 7.5.17
    public static long daysUntil(DynamicObject earlier, DynamicObject later) {
        assert JSObject.hasProperty(earlier, "ISOYear") && JSObject.hasProperty(later, "ISOYear") &&
                        JSObject.hasProperty(earlier, "ISOMonth") && JSObject.hasProperty(later, "ISOMonth") &&
                        JSObject.hasProperty(earlier, "ISODay") && JSObject.hasProperty(later, "ISODay");
        DynamicObject difference = null;    // TODO: Call differenceDate.
        return getLong(difference, DAYS, 0);
    }

    // 7.5.18
    // TODO should probably return a record, not an object?
    @SuppressWarnings("unused")
    public static DynamicObject moveRelativeDate(DynamicObject calendar, DynamicObject relativeTo, DynamicObject duration, JSContext ctx) {
        DynamicObject options = JSOrdinary.createWithNullPrototype(ctx);
        DynamicObject later = null; // TODO: Invoke dateAdd on calendar
        long days = daysUntil(relativeTo, later);
        DynamicObject dateTime = null; // TODO: Call createTemporalDateTime
        DynamicObject record = JSOrdinary.create(ctx);
        JSObjectUtil.putDataProperty(ctx, record, "relativeTo", dateTime);
        JSObjectUtil.putDataProperty(ctx, record, DAYS, days);
        return record;
    }

    // 7.5.20
    public static JSTemporalPlainDateTimePluralRecord roundDuration(long y, long m, long w, long d, long h, long min, long sec, long milsec, long micsec, long nsec,
                    long increment, String unit, String roundingMode, DynamicObject relTo, JSContext ctx) {
        long years = y;
        long months = m;
        long weeks = w;
        long days = d;
        long hours = h;
        long minutes = min;
        long seconds = sec;
        long microseconds = micsec;
        long milliseconds = milsec;
        long nanoseconds = nsec;
        DynamicObject relativeTo = relTo;
        if ((unit.equals(YEARS) || unit.equals(MONTHS) || unit.equals(WEEKS)) && relativeTo == Undefined.instance) {
            throw TemporalErrors.createRangeErrorRelativeToNotUndefined(unit);
        }
        DynamicObject zonedRelativeTo = Undefined.instance;
        DynamicObject calendar = Undefined.instance;
        double fractionalSeconds = 0;

        if (relativeTo != Undefined.instance) {
            // TODO: Check if relativeTo has InitializedTemporalZonedDateTime
            calendar = (DynamicObject) JSObject.get(relativeTo, "calendar");
        }
        if (unit.equals(YEARS) || unit.equals(MONTHS) || unit.equals(WEEKS) || unit.equals(DAYS)) {
            nanoseconds = totalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
            DynamicObject intermediate = Undefined.instance;
            if (zonedRelativeTo != Undefined.instance) {
                // TODO: intermediate = moveRelativeZonedDateTime
            }
            JSTemporalNanosecondsDaysRecord result = nanosecondsToDays(nanoseconds, intermediate);
            days = days + result.getDays() +
                            (result.getNanoseconds() /
                                            Math.abs(result.getDayLength()));
            hours = 0;
            minutes = 0;
            seconds = 0;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else {
            fractionalSeconds = (nanoseconds * 0.000_000_001) + (microseconds * 0.000_001) + (milliseconds * 0.001) + seconds;
        }
        double remainder = 0;
        if (unit.equals(YEARS)) {
            DynamicObject yearsDuration = createTemporalDuration(years, 0, 0, 0, 0, 0, 0, 0, 0, 0, ctx);

            DynamicObject dateAdd = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject firstAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsDuration, firstAddOptions, dateAdd);
            DynamicObject yearsMonthsWeeks = createTemporalDuration(years, months, weeks, 0, 0, 0, 0, 0, 0, 0, ctx);

            DynamicObject secondAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsMonthsWeeksLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsMonthsWeeks, secondAddOptions, dateAdd);
            long monthsWeeksInDays = daysUntil(yearsLater, yearsMonthsWeeksLater);
            relativeTo = yearsLater;
            days = days + monthsWeeksInDays;
            long sign = TemporalUtil.sign(days);
            if (sign == 0) {
                sign = 1;
            }
            DynamicObject oneYear = createTemporalDuration(sign, 0, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
            DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneYear, ctx);
            relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
            long oneYearDays = getLong(moveResult, DAYS, 0);
            while (Math.abs(days) >= Math.abs(oneYearDays)) {
                years = years + sign;
                days = days - oneYearDays;
                moveResult = moveRelativeDate(calendar, relativeTo, oneYear, ctx);
                relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
                oneYearDays = getLong(moveResult, DAYS, 0);
            }
            double fractionalYears = years + ((double) days / Math.abs(oneYearDays));
            years = (long) TemporalUtil.roundNumberToIncrement(fractionalYears, increment, roundingMode);
            remainder = fractionalYears - years;
            months = 0;
            weeks = 0;
            years = 0;
        } else if (unit.equals(MONTHS)) {
            DynamicObject yearsMonths = createTemporalDuration(years, months, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
            DynamicObject dateAdd = (DynamicObject) JSObject.getMethod(calendar, TemporalConstants.DATE_ADD);
            DynamicObject firstAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsMonthsLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsMonths, firstAddOptions, dateAdd);
            DynamicObject yearsMonthsWeeks = createTemporalDuration(years, months, weeks, 0, 0, 0, 0, 0, 0, 0, ctx);
            DynamicObject secondAddOptions = JSOrdinary.createWithNullPrototype(ctx);
            DynamicObject yearsMonthsWeeksLater = TemporalUtil.calendarDateAdd(calendar, relativeTo, yearsMonthsWeeks, secondAddOptions, dateAdd);
            long weeksInDays = daysUntil(yearsMonthsLater, yearsMonthsWeeksLater);
            relativeTo = yearsMonthsLater;
            days = days + weeksInDays;
            long sign = TemporalUtil.sign(days);
            if (sign == 0) {
                sign = 1;
            }
            DynamicObject oneMonth = createTemporalDuration(0, sign, 0, 0, 0, 0, 0, 0, 0, 0, ctx);
            DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
            relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
            long oneMonthDays = getLong(moveResult, DAYS, 0);
            while (Math.abs(days) >= Math.abs(oneMonthDays)) {
                months = months + sign;
                days = days - oneMonthDays;
                moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, ctx);
                relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
                oneMonthDays = getLong(moveResult, DAYS, 0);
            }
            double fractionalMonths = months + ((double) days / Math.abs(oneMonthDays));
            months = (long) TemporalUtil.roundNumberToIncrement(fractionalMonths, increment, roundingMode);
            remainder = fractionalMonths - months;
            weeks = 0;
            days = 0;
        } else if (unit.equals(WEEKS)) {
            long sign = TemporalUtil.sign(days);
            if (sign == 0) {
                sign = 1;
            }
            DynamicObject oneWeek = createTemporalDuration(0, 0, sign, 0, 0, 0, 0, 0, 0, 0, ctx);
            DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, ctx);
            relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
            long oneWeekDays = getLong(moveResult, DAYS, 0);
            while (Math.abs(days) >= Math.abs(oneWeekDays)) {
                weeks = weeks - sign;
                days = days - oneWeekDays;
                moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, ctx);
                relativeTo = (DynamicObject) JSObject.get(moveResult, "relativeTo");
                oneWeekDays = getLong(moveResult, DAYS, 0);
            }
            double fractionalWeeks = weeks + ((double) days / Math.abs(oneWeekDays));
            weeks = (long) TemporalUtil.roundNumberToIncrement(fractionalWeeks, increment, roundingMode);
            remainder = fractionalWeeks - weeks;
            days = 0;
        } else if (unit.equals(DAYS)) {
            double fractionalDays = days;
            days = (long) TemporalUtil.roundNumberToIncrement(fractionalDays, increment, roundingMode);
            remainder = fractionalDays - days;
        } else if (unit.equals(HOURS)) {
            double fractionalHours = (((fractionalSeconds / 60) + minutes) / 60) + hours;
            hours = (long) TemporalUtil.roundNumberToIncrement(fractionalHours, increment, roundingMode);
            remainder = fractionalHours - hours;
            minutes = 0;
            seconds = 0;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit.equals(MINUTES)) {
            double fractionalMinutes = (fractionalSeconds / 60) + minutes;
            minutes = (long) TemporalUtil.roundNumberToIncrement(fractionalMinutes, increment, roundingMode);
            remainder = fractionalMinutes - minutes;
            seconds = 0;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit.equals(SECONDS)) {
            seconds = (long) TemporalUtil.roundNumberToIncrement(fractionalSeconds, increment, roundingMode);
            remainder = fractionalSeconds - seconds;
            milliseconds = 0;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit.equals(MILLISECONDS)) {
            double fractionalMilliseconds = (nanoseconds * 0.000_000_1) + (microseconds * 0.000_1) + milliseconds;
            milliseconds = (long) TemporalUtil.roundNumberToIncrement(fractionalMilliseconds, increment, roundingMode);
            remainder = fractionalMilliseconds - milliseconds;
            microseconds = 0;
            nanoseconds = 0;
        } else if (unit.equals(MICROSECONDS)) {
            double fractionalMicroseconds = (nanoseconds * 0.000_1) + microseconds;
            microseconds = (long) TemporalUtil.roundNumberToIncrement(fractionalMicroseconds, increment, roundingMode);
            remainder = fractionalMicroseconds - microseconds;
            nanoseconds = 0;
        } else {
            assert unit.equals(NANOSECONDS);
            remainder = nanoseconds;
            nanoseconds = (long) TemporalUtil.roundNumberToIncrement(nanoseconds, increment, roundingMode);
            remainder = remainder - nanoseconds;
        }

        return JSTemporalPlainDateTimePluralRecord.createWeeksRemainder(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, remainder);
    }

    @SuppressWarnings("unused")
    private static JSTemporalNanosecondsDaysRecord nanosecondsToDays(long nanoseconds, DynamicObject intermediate) {
        // TODO implement
        return JSTemporalNanosecondsDaysRecord.create(0, 0, 0);
    }

    // 7.5.21
    public static JSTemporalPlainDateTimePluralRecord adjustRoundedDurationDays(long years, long months, long weeks, long days, long hours,
                    long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds, long increment, String unit,
                    String roundingMode, DynamicObject relativeTo,
                    JSContext ctx) {
        if (!(TemporalUtil.isTemporalZonedDateTime(relativeTo)) || unit.equals(YEARS) || unit.equals(MONTHS) || unit.equals(WEEKS) || unit.equals(DAYS) ||
                        (unit.equals(NANOSECONDS) && increment == 1)) {
            return JSTemporalPlainDateTimePluralRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        long timeRemainderNs = totalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
        long direction = TemporalUtil.sign(timeRemainderNs);
        long dayStart = TemporalUtil.addZonedDateTime(
                        getLong(relativeTo, NANOSECONDS),
                        JSObject.get(relativeTo, TIME_ZONE),
                        (DynamicObject) JSObject.get(relativeTo, CALENDAR), years, months, weeks, days, 0, 0, 0, 0, 0, 0);
        long dayEnd = TemporalUtil.addZonedDateTime(dayStart, JSObject.get(relativeTo, TIME_ZONE), (DynamicObject) JSObject.get(relativeTo, CALENDAR), 0, 0, 0, direction, 0, 0, 0, 0, 0, 0);
        long dayLengthNs = dayEnd - dayStart;
        if ((timeRemainderNs - dayLengthNs) * direction < 0) {
            return JSTemporalPlainDateTimePluralRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
        timeRemainderNs = TemporalUtil.roundTemporalInstant(timeRemainderNs - dayLengthNs, increment, unit, roundingMode);
        JSTemporalPlainDateTimePluralRecord add = addDuration(years, months, weeks, days, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, direction, 0, 0, 0, 0, 0, 0,
                        relativeTo, ctx);
        JSTemporalPlainDateTimePluralRecord atd = balanceDuration(0, 0, 0, 0, 0, 0, timeRemainderNs, "hours", Undefined.instance);

        return JSTemporalPlainDateTimePluralRecord.createWeeks(add.getYears(), add.getMonths(), add.getWeeks(), add.getDays(),
                        atd.getHours(), atd.getMinutes(), atd.getSeconds(), atd.getMilliseconds(), atd.getMicroseconds(), atd.getNanoseconds());
    }

    // 7.5.22
    public static JSTemporalPlainDateTimeRecord toLimitedTemporalDuration(Object temporalDurationLike,
                    Set<String> disallowedFields, IsObjectNode isObject, JSToStringNode toString, JSToIntegerAsLongNode toInt) {
        JSTemporalPlainDateTimeRecord d;
        if (!isObject.executeBoolean(temporalDurationLike)) {
            String str = toString.executeString(temporalDurationLike);
            d = parseTemporalDurationString(str);
        } else {
            d = toTemporalDurationRecord((DynamicObject) temporalDurationLike, isObject, toInt);
        }
        if (!validateTemporalDuration(d.getYear(), d.getMonth(), d.getWeeks(), d.getDay(), d.getHour(), d.getMinute(), d.getSecond(), d.getMillisecond(),
                        d.getMicrosecond(), d.getNanosecond())) {
            throw Errors.createRangeError("Given duration outside range.");
        }

        for (String property : PROPERTIES) {
            long value = TemporalUtil.getPropertyFromRecord(d, property);
            if (value > 0 && disallowedFields.contains(property)) {
                throw TemporalErrors.createRangeErrorDisallowedField(property);
            }
        }
        return d;
    }

    // 7.5.23
    @TruffleBoundary
    public static String temporalDurationToString(long yearsP, long monthsP, long weeksP, long daysP, long hoursP, long minutesP, long secondsP, long millisecondsP, long microsecondsP,
                    long nanosecondsP, Object precision) {
        long years = yearsP;
        long months = monthsP;
        long weeks = weeksP;
        long days = daysP;
        long hours = hoursP;
        long minutes = minutesP;
        long seconds = secondsP;
        long milliseconds = millisecondsP;
        long microseconds = microsecondsP;
        long nanoseconds = nanosecondsP;

        int sign = durationSign(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        microseconds += nanoseconds / 1000;
        nanoseconds = nanoseconds % 1000;
        milliseconds += microseconds / 1000;
        microseconds = microseconds % 1000;
        seconds += milliseconds / 1000;
        milliseconds = milliseconds % 1000;
        if (years == 0 && months == 0 && weeks == 0 && days == 0 && hours == 0 && minutes == 0 && seconds == 0 && milliseconds == 0 && microseconds == 0 && nanoseconds == 0) {
            return "PT0S";
        }
        StringBuilder datePart = new StringBuilder();
        if (years != 0) {
            datePart.append(Math.abs(years));
            datePart.append("Y");
        }
        if (months != 0) {
            datePart.append(Math.abs(months));
            datePart.append("M");
        }
        if (weeks != 0) {
            datePart.append(Math.abs(weeks));
            datePart.append("W");
        }
        if (days != 0) {
            datePart.append(Math.abs(days));
            datePart.append("D");
        }
        StringBuilder timePart = new StringBuilder();
        if (hours != 0) {
            timePart.append(Math.abs(hours));
            timePart.append("H");
        }
        if (minutes != 0) {
            timePart.append(Math.abs(minutes));
            timePart.append("M");
        }
        if (seconds != 0 || milliseconds != 0 || microseconds != 0 || nanoseconds != 0) {
            String nanosecondPart = "";
            String microsecondPart = "";
            String millisecondPart = "";
            if (nanoseconds != 0) {
                nanosecondPart = String.format("%1$3d", Math.abs(nanoseconds)).replace(" ", "0");
                microsecondPart = "000";
                millisecondPart = "000";
            }
            if (microseconds != 0) {
                microsecondPart = String.format("%1$3d", Math.abs(microseconds)).replace(" ", "0");
                millisecondPart = "000";
            }
            if (milliseconds != 0) {
                millisecondPart = String.format("%1$3d", Math.abs(milliseconds)).replace(" ", "0");
            }
            String decimalPart = millisecondPart + microsecondPart + nanosecondPart;
            if (AUTO.equals(precision)) {
                // TODO Set decimalPart to the longest possible substring of decimalPart starting at
                // position 0 and not ending with the code unit 0x0030 (DIGIT ZERO).
            } else {
                decimalPart = decimalPart.substring(0, (int) precision);
            }
            String secondsPart = String.format("%d", Math.abs(seconds));
            if (!decimalPart.equals("")) {
                secondsPart += "." + decimalPart;
            }
            timePart.append(secondsPart);
            timePart.append("S");
        }
        String signPart = sign < 0 ? "-" : "";
        StringBuilder result = new StringBuilder();
        result.append(signPart).append("P").append(datePart);
        if (!timePart.toString().equals("")) {
            result.append("T").append(timePart);
        }
        return result.toString();
    }
    // endregion
}
