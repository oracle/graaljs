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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.BLANK;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTES;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SIGN;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;

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
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public final class JSTemporalDuration extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalDuration INSTANCE = new JSTemporalDuration();

    public static final String CLASS_NAME = "TemporalDuration";
    public static final String PROTOTYPE_NAME = "TemporalDuration.prototype";

    private JSTemporalDuration() {
    }

    public static DynamicObject create(JSContext context, long years, long months, long weeks, long days, long hours,
                    long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        if (!TemporalUtil.validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds,
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
        JSTemporalDateTimeRecord result;
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
    private static JSTemporalDateTimeRecord parseTemporalDurationString(String string) {

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
            JSTemporalDateTimeRecord result = TemporalUtil.durationHandleFractions(fHour, minute, fMinute, second, 0, millisecond, 0, microsecond, 0, nanosecond, 0);
            return JSTemporalDateTimeRecord.createWeeks(year, month, week, day, (long) hour,
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
    public static JSTemporalDateTimeRecord toTemporalDurationRecord(DynamicObject temporalDurationLike, IsObjectNode isObject, JSToIntegerAsLongNode toInt) {
        assert isObject.executeBoolean(temporalDurationLike);
        if (isJSTemporalDuration(temporalDurationLike)) {
            JSTemporalDurationObject d = (JSTemporalDurationObject) temporalDurationLike;
            return JSTemporalDateTimeRecord.createWeeks(d.getYears(), d.getMonths(), d.getWeeks(), d.getDays(), d.getHours(), d.getMinutes(), d.getSeconds(), d.getMilliseconds(),
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
        for (String property : TemporalUtil.DURATION_PROPERTIES) {
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
        return JSTemporalDateTimeRecord.createWeeks(year, month, week, day, hour, minute, second, millis, micros, nanos);
    }

    // 7.5.8
    public static DynamicObject createTemporalDuration(long years, long months, long weeks, long days, long hours,
                    long minutes, long seconds, long milliseconds, long microseconds,
                    long nanoseconds, JSContext ctx) {
        if (!TemporalUtil.validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
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
        assert TemporalUtil.validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        DynamicObject constructor = realm.getTemporalDurationConstructor();
        Object[] ctorArgs = new Object[]{years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds};
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, ctorArgs.length);
        System.arraycopy(ctorArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, ctorArgs.length);
        Object result = callNode.executeCall(args);
        return (JSTemporalDurationObject) result;
    }

    // 7.5.22
    public static JSTemporalDateTimeRecord toLimitedTemporalDuration(Object temporalDurationLike,
                    Set<String> disallowedFields, IsObjectNode isObject, JSToStringNode toString, JSToIntegerAsLongNode toInt) {
        JSTemporalDateTimeRecord d;
        if (!isObject.executeBoolean(temporalDurationLike)) {
            String str = toString.executeString(temporalDurationLike);
            d = parseTemporalDurationString(str);
        } else {
            d = toTemporalDurationRecord((DynamicObject) temporalDurationLike, isObject, toInt);
        }
        if (!TemporalUtil.validateTemporalDuration(d.getYear(), d.getMonth(), d.getWeeks(), d.getDay(), d.getHour(), d.getMinute(), d.getSecond(), d.getMillisecond(),
                        d.getMicrosecond(), d.getNanosecond())) {
            throw Errors.createRangeError("Given duration outside range.");
        }

        for (String property : TemporalUtil.DURATION_PROPERTIES) {
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

        int sign = TemporalUtil.durationSign(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
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
