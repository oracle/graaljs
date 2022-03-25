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
import static com.oracle.truffle.js.runtime.util.TemporalUtil.bitoi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltins;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSNumberToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.UnitPlural;

public final class JSTemporalDuration extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalDuration INSTANCE = new JSTemporalDuration();

    public static final TruffleString CLASS_NAME = Strings.constant("Duration");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Duration.prototype");
    public static final TruffleString TO_STRING_TAG = Strings.constant("Temporal.Duration");

    private JSTemporalDuration() {
    }

    public static JSTemporalDurationObject createTemporalDuration(JSContext context, double years, double months, double weeks, double days, double hours,
                    double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds) {
        if (!TemporalUtil.validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds,
                        nanoseconds)) {
            throw Errors.createRangeError("Given duration outside range.");
        }
        JSRealm realm = JSRealm.get(null);
        JSObjectFactory factory = context.getTemporalDurationFactory();
        JSTemporalDurationObject obj = factory.initProto(new JSTemporalDurationObject(factory.getShape(realm),
                        nnz(years), nnz(months), nnz(weeks), nnz(days), nnz(hours), nnz(minutes), nnz(seconds), nnz(milliseconds), nnz(microseconds), nnz(nanoseconds)), realm);
        return context.trackAllocation(obj);
    }

    private static double nnz(double d) {
        if (JSRuntime.isNegativeZero(d)) {
            return 0;
        }
        return d;
    }

    @Override
    public TruffleString getClassName(DynamicObject object) {
        return TO_STRING_TAG;
    }

    @Override
    public TruffleString getClassName() {
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
        JSObjectUtil.putToStringTag(prototype, TO_STRING_TAG);

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, JSTemporalDuration.INSTANCE, context);
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
    public static DynamicObject toTemporalDuration(Object item, JSContext ctx, IsObjectNode isObject, JSToStringNode toString) {
        JSTemporalDurationRecord result;
        if (isObject.executeBoolean(item)) {
            if (isJSTemporalDuration(item)) {
                return (DynamicObject) item;
            }
            result = toTemporalDurationRecord((DynamicObject) item);
        } else {
            TruffleString string = toString.executeString(item);
            result = parseTemporalDurationString(string);
        }
        return createTemporalDuration(ctx, result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), result.getHours(), result.getMinutes(), result.getSeconds(),
                        result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds());
    }

    @TruffleBoundary
    public static JSTemporalDurationRecord parseTemporalDurationString(TruffleString string) {
        long yearsMV = 0;
        long monthsMV = 0;
        long daysMV = 0;
        long weeksMV = 0;
        double hoursMV = 0;
        double minutesMV = 0;
        double secondsMV = 0;
        BigDecimal millisecondsMV = BigDecimal.ZERO;
        BigDecimal microsecondsMV = BigDecimal.ZERO;
        BigDecimal nanosecondsMV = BigDecimal.ZERO;

        TruffleString minutes = Strings.EMPTY_STRING;
        TruffleString seconds = Strings.EMPTY_STRING;

        TruffleString fHours = Strings.EMPTY_STRING;
        TruffleString fMinutes = Strings.EMPTY_STRING;
        TruffleString fSeconds = Strings.EMPTY_STRING;

        // P1Y1M1W1DT1H1M1.123456789S
        Pattern regex = Pattern.compile("^([\\+\u2212-]?)[Pp](\\d+[Yy])?(\\d+[Mm])?(\\d+[Ww])?(\\d+[Dd])?([Tt]([\\d.]+[Hh])?([\\d.]+[Mm])?([\\d.]+[Ss])?)?$");
        Matcher matcher = regex.matcher(Strings.toJavaString(string));
        if (matcher.matches()) {
            TruffleString sign = group(string, matcher, 1);

            yearsMV = parseDurationIntl(string, matcher, 2);
            monthsMV = parseDurationIntl(string, matcher, 3);
            weeksMV = parseDurationIntl(string, matcher, 4);
            daysMV = parseDurationIntl(string, matcher, 5);
            Pair<TruffleString, TruffleString> hoursPair;
            Pair<TruffleString, TruffleString> minutesPair;
            Pair<TruffleString, TruffleString> secondsPair;

            TruffleString timeGroup = group(string, matcher, 6);
            if (timeGroup != null && Strings.length(timeGroup) > 0) {
                hoursPair = parseDurationIntlWithFraction(string, matcher, 7);
                minutesPair = parseDurationIntlWithFraction(string, matcher, 8);
                secondsPair = parseDurationIntlWithFraction(string, matcher, 9);

                hoursMV = TemporalUtil.toIntegerOrInfinity(hoursPair.getFirst()).doubleValue();
                fHours = hoursPair.getSecond();

                minutes = minutesPair.getFirst();
                fMinutes = minutesPair.getSecond();

                seconds = secondsPair.getFirst();
                fSeconds = secondsPair.getSecond();
            }

            if (!empty(fHours)) {
                if (!empty(minutes) || !empty(fMinutes) || !empty(seconds) || !empty(fSeconds)) {
                    throw TemporalErrors.createRangeErrorTemporalMalformedDuration();
                }
                assert !Strings.contains(fHours, '.');
                TruffleString fHoursDigits = fHours; // substring(1) handled above
                int fHoursScale = Strings.length(fHoursDigits);
                minutesMV = 60.0 * TemporalUtil.toIntegerOrInfinity(fHoursDigits).doubleValue() / Math.pow(10, fHoursScale);
            } else {
                minutesMV = TemporalUtil.toIntegerOrInfinity(minutes).doubleValue();
            }
            if (!empty(fMinutes)) {
                if (!empty(seconds) || !empty(fSeconds)) {
                    throw TemporalErrors.createRangeErrorTemporalMalformedDuration();
                }
                assert !Strings.contains(fMinutes, '.');
                TruffleString fMinutesDigits = fMinutes; // substring(1) handled above
                int fMinutesScale = Strings.length(fMinutesDigits);
                secondsMV = 60.0 * TemporalUtil.toIntegerOrInfinity(fMinutesDigits).doubleValue() / Math.pow(10, fMinutesScale);
            } else if (!empty(seconds)) {
                secondsMV = TemporalUtil.toIntegerOrInfinity(seconds).doubleValue();
            } else {
                secondsMV = TemporalUtil.remainder(minutesMV, 1) * 60.0;
            }

            if (!empty(fSeconds)) {
                assert !Strings.contains(fSeconds, '.'); // substring(1) handled above
                TruffleString fSecondsDigits = fSeconds;
                int fSecondsScale = Strings.length(fSecondsDigits);
                millisecondsMV = TemporalUtil.bd_1000.multiply(BigDecimal.valueOf(TemporalUtil.toIntegerOrInfinity(fSecondsDigits).longValue())).divide(
                                TemporalUtil.bd_10.pow(fSecondsScale));
            } else {
                millisecondsMV = new BigDecimal(secondsMV).remainder(BigDecimal.ONE, TemporalUtil.mc_20_floor).multiply(TemporalUtil.bd_1000, TemporalUtil.mc_20_floor);
            }
            microsecondsMV = millisecondsMV.remainder(BigDecimal.ONE, TemporalUtil.mc_20_floor).multiply(TemporalUtil.bd_1000);
            nanosecondsMV = microsecondsMV.remainder(BigDecimal.ONE, TemporalUtil.mc_20_floor).multiply(TemporalUtil.bd_1000);

            int factor = (sign.equals(Strings.SYMBOL_MINUS) || sign.equals(Strings.UNICODE_MINUS_SIGN)) ? -1 : 1;

            return JSTemporalDurationRecord.createWeeks(yearsMV * factor, monthsMV * factor, weeksMV * factor, daysMV * factor, hoursMV * factor,
                            (long) (Math.floor(minutesMV) * factor), (long) (Math.floor(secondsMV) * factor),
                            millisecondsMV.longValue() * factor, microsecondsMV.longValue() * factor, nanosecondsMV.longValue() * factor);
        }
        throw TemporalErrors.createRangeErrorTemporalMalformedDuration();
    }

    private static TruffleString group(TruffleString string, Matcher matcher, int groupNumber) {
        int start = matcher.start(groupNumber);
        // lazy string never escapes in only caller parseTemporalDurationString
        return start < 0 ? null : Strings.lazySubstring(string, start, matcher.end(groupNumber) - start);
    }

    private static boolean empty(TruffleString s) {
        assert s != null;
        return s.isEmpty();
    }

    private static long parseDurationIntl(TruffleString string, Matcher matcher, int i) {
        int start = matcher.start(i);
        if (start >= 0) {
            TruffleString numstr = Strings.lazySubstring(string, start, matcher.end(i) - (start + 1));
            try {
                return TemporalUtil.toIntegerOrInfinity(numstr).longValue();
            } catch (NumberFormatException ex) {
                throw Errors.createRangeError("decimal numbers only allowed in time units");
            }
        }
        return 0L;
    }

    private static Pair<TruffleString, TruffleString> parseDurationIntlWithFraction(TruffleString string, Matcher matcher, int i) {
        int start = matcher.start(i);
        if (start >= 0) {
            // using lazySubstring, because the return value never escapes in the only caller,
            // parseTemporalDurationString
            TruffleString numstr = Strings.lazySubstring(string, start, matcher.end(i) - (start + 1));

            int idx = Strings.indexOf(numstr, '.');
            if (idx >= 0) {
                TruffleString wholePart = Strings.lazySubstring(numstr, 0, idx);
                TruffleString fractionalPart = Strings.lazySubstring(numstr, idx + 1);
                return new Pair<>(wholePart, fractionalPart);
            } else {
                return new Pair<>(numstr, Strings.EMPTY_STRING);
            }
        }
        return new Pair<>(Strings.EMPTY_STRING, Strings.EMPTY_STRING);
    }

    // 7.5.2
    @TruffleBoundary
    public static JSTemporalDurationRecord toTemporalDurationRecord(DynamicObject temporalDurationLike) {
        if (isJSTemporalDuration(temporalDurationLike)) {
            JSTemporalDurationObject d = (JSTemporalDurationObject) temporalDurationLike;
            return JSTemporalDurationRecord.createWeeks(d.getYears(), d.getMonths(), d.getWeeks(), d.getDays(), d.getHours(), d.getMinutes(), d.getSeconds(), d.getMilliseconds(),
                            d.getMicroseconds(), d.getNanoseconds());
        }
        boolean any = false;
        double year = 0;
        double month = 0;
        double week = 0;
        double day = 0;
        double hour = 0;
        double minute = 0;
        double second = 0;
        double millis = 0;
        double micros = 0;
        double nanos = 0;
        for (UnitPlural unit : TemporalUtil.DURATION_PROPERTIES) {
            Object val = JSObject.get(temporalDurationLike, unit.toTruffleString());

            double lVal = 0;
            if (val == Undefined.instance) {
                lVal = 0;
            } else {
                any = true;
                lVal = TemporalUtil.toIntegerWithoutRounding(val);
            }
            switch (unit) {
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
        return JSTemporalDurationRecord.createWeeks(year, month, week, day, hour, minute, second, millis, micros, nanos);
    }

    @TruffleBoundary
    public static TruffleString temporalDurationToString(double yearsP, double monthsP, double weeksP, double daysP, double hoursP, double minutesP, double secondsP, double millisecondsP,
                    double microsecondsP, double nanosecondsP, Object precision, JSNumberToBigIntNode toBigIntNode) {
        int sign = TemporalUtil.durationSign(yearsP, monthsP, weeksP, daysP, hoursP, minutesP, secondsP, millisecondsP, microsecondsP, nanosecondsP);

        BigInteger nanosecondsBI = toBigInteger(nanosecondsP, toBigIntNode);
        BigInteger microsecondsBI = toBigInteger(microsecondsP, toBigIntNode);
        BigInteger millisecondsBI = toBigInteger(millisecondsP, toBigIntNode);
        BigInteger secondsBI = toBigInteger(secondsP, toBigIntNode);

        BigInteger yearsBI = toBigIntegerOrNull(Math.abs(yearsP), toBigIntNode);
        BigInteger monthsBI = toBigIntegerOrNull(Math.abs(monthsP), toBigIntNode);
        BigInteger weeksBI = toBigIntegerOrNull(Math.abs(weeksP), toBigIntNode);
        BigInteger daysBI = toBigIntegerOrNull(Math.abs(daysP), toBigIntNode);
        BigInteger hoursBI = toBigIntegerOrNull(Math.abs(hoursP), toBigIntNode);
        BigInteger minutesBI = toBigIntegerOrNull(Math.abs(minutesP), toBigIntNode);

        boolean condition = secondsP != 0 || millisecondsP != 0 || microsecondsP != 0 || nanosecondsP != 0 ||
                        (yearsP == 0 && monthsP == 0 && weeksP == 0 && daysP == 0 && hoursP == 0 && minutesP == 0);
        return temporalDurationToStringIntl(yearsBI, monthsBI, weeksBI, daysBI, hoursBI, minutesBI, secondsBI, millisecondsBI, microsecondsBI, nanosecondsBI, precision, sign, condition);
    }

    @TruffleBoundary
    private static TruffleString temporalDurationToStringIntl(BigInteger yearsP, BigInteger monthsP, BigInteger weeksP, BigInteger daysP, BigInteger hoursP, BigInteger minutesP, BigInteger secondsP,
                    BigInteger millisecondsP, BigInteger microsecondsP, BigInteger nanosecondsP, Object precision, int sign, boolean condition) {
        BigInteger[] res = nanosecondsP.divideAndRemainder(TemporalUtil.bi_1000);
        BigInteger microseconds = microsecondsP.add(res[0]);
        BigInteger nanoseconds = res[1];

        res = microseconds.divideAndRemainder(TemporalUtil.bi_1000);
        BigInteger milliseconds = millisecondsP.add(res[0]);
        microseconds = res[1];

        res = milliseconds.divideAndRemainder(TemporalUtil.bi_1000);
        BigInteger seconds = secondsP.add(res[0]);
        milliseconds = res[1];

        StringBuilder datePart = new StringBuilder();
        if (yearsP != null) {
            datePart.append(yearsP.toString());
            datePart.append("Y");
        }
        if (monthsP != null) {
            datePart.append(monthsP.toString());
            datePart.append("M");
        }
        if (weeksP != null) {
            datePart.append(weeksP.toString());
            datePart.append("W");
        }
        if (daysP != null) {
            datePart.append(daysP.toString());
            datePart.append("D");
        }
        StringBuilder timePart = new StringBuilder();
        if (hoursP != null) {
            timePart.append(hoursP.toString());
            timePart.append("H");
        }
        if (minutesP != null) {
            timePart.append(minutesP.toString());
            timePart.append("M");
        }
        if (condition || !AUTO.equals(precision)) {
            // values clamped above
            long fraction = Math.abs(bitoi(milliseconds)) * 1_000_000L + Math.abs(bitoi(microseconds)) * 1_000L + Math.abs(bitoi(nanoseconds));
            TruffleString decimalPart = Strings.format("000000000%1$09d", fraction);
            decimalPart = Strings.lazySubstring(decimalPart, Strings.length(decimalPart) - 9);

            if (AUTO.equals(precision)) {
                int pos = Strings.length(decimalPart) - 1;
                while (pos >= 0 && Strings.charAt(decimalPart, pos) == '0') {
                    pos--;
                }
                if (pos != (Strings.length(decimalPart) - 1)) {
                    decimalPart = Strings.lazySubstring(decimalPart, 0, pos + 1);
                }
            } else if ((precision instanceof Number) && (((Number) precision).doubleValue() == 0.0)) {
                decimalPart = Strings.EMPTY_STRING;
            } else {
                Number n = (Number) precision;
                decimalPart = Strings.lazySubstring(decimalPart, 0, Math.min(Strings.length(decimalPart), n.intValue()));
            }
            TruffleString secondsPart = Strings.fromJavaString(seconds.abs().toString());
            if (!decimalPart.equals(Strings.EMPTY_STRING)) {
                secondsPart = Strings.concatAll(secondsPart, Strings.DOT, decimalPart);
            }
            timePart.append(secondsPart);
            timePart.append("S");
        }
        TruffleString signPart = sign < 0 ? Strings.SYMBOL_MINUS : Strings.EMPTY_STRING;
        TruffleStringBuilder result = Strings.builderCreate();
        Strings.builderAppend(result, signPart);
        Strings.builderAppend(result, "P");
        Strings.builderAppend(result, datePart.toString());
        if (timePart.length() > 0) {
            Strings.builderAppend(result, "T");
            Strings.builderAppend(result, timePart.toString());
        }
        return Strings.builderToString(result);
    }

    private static BigInteger toBigIntegerOrNull(double value, JSNumberToBigIntNode toBigIntNode) {
        return (value != 0) ? toBigIntNode.executeBigInt(value).bigIntegerValue() : null;
    }

    private static BigInteger toBigInteger(double value, JSNumberToBigIntNode toBigIntNode) {
        return toBigIntNode.executeBigInt(value).bigIntegerValue();
    }
    // endregion
}
