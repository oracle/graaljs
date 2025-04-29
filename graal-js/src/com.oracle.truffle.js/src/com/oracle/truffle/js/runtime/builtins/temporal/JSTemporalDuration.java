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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.AUTO;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.bitoi;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltins;
import com.oracle.truffle.js.nodes.cast.JSNumberToBigIntNode;
import com.oracle.truffle.js.runtime.BigInt;
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
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public final class JSTemporalDuration extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final JSTemporalDuration INSTANCE = new JSTemporalDuration();

    public static final TruffleString CLASS_NAME = Strings.constant("Duration");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Duration.prototype");
    public static final TruffleString TO_STRING_TAG = Strings.constant("Temporal.Duration");

    private JSTemporalDuration() {
    }

    public static JSTemporalDurationObject createTemporalDuration(JSContext context, JSRealm realm,
                    double years, double months, double weeks, double days,
                    double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds,
                    Node node, InlinedBranchProfile errorBranch) {
        return createTemporalDuration(context, realm, INSTANCE.getIntrinsicDefaultProto(realm),
                        years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds,
                        node, errorBranch);
    }

    public static JSTemporalDurationObject createTemporalDuration(JSContext context, JSRealm realm, JSDynamicObject proto,
                    double years, double months, double weeks, double days,
                    double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds,
                    Node node, InlinedBranchProfile errorBranch) {
        if (!TemporalUtil.isValidDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
            errorBranch.enter(node);
            throw TemporalErrors.createTypeErrorDurationOutsideRange();
        }
        return createIntl(context, realm, proto, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }

    @InliningCutoff
    private static JSTemporalDurationObject createIntl(JSContext context, JSRealm realm, JSDynamicObject proto,
                    double years, double months, double weeks, double days,
                    double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds) {
        JSObjectFactory factory = context.getTemporalDurationFactory();
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSTemporalDurationObject(shape, proto,
                        nnz(years), nnz(months), nnz(weeks), nnz(days), nnz(hours), nnz(minutes), nnz(seconds), nnz(milliseconds), nnz(microseconds), nnz(nanoseconds)), realm, proto);
        return factory.trackAllocation(newObj);
    }

    public static JSTemporalDurationObject createNegatedTemporalDuration(JSContext context, JSRealm realm, JSTemporalDurationObject duration) {
        var proto = INSTANCE.getIntrinsicDefaultProto(realm);
        return createIntl(context, realm, proto,
                        -duration.getYears(), -duration.getMonths(), -duration.getWeeks(), -duration.getDays(),
                        -duration.getHours(), -duration.getMinutes(), -duration.getSeconds(),
                        -duration.getMilliseconds(), -duration.getMicroseconds(), -duration.getNanoseconds());
    }

    private static double nnz(double d) {
        if (JSRuntime.isNegativeZero(d)) {
            return 0;
        }
        return d;
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject constructor) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(prototype, constructor);
        JSObjectUtil.putAccessorsFromContainer(realm, prototype, TemporalDurationPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalDurationPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, TO_STRING_TAG);
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, JSTemporalDuration.INSTANCE, context);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalDurationPrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalDurationFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalDuration(Object obj) {
        return obj instanceof JSTemporalDurationObject;
    }

    // region Abstract methods
    @TruffleBoundary
    public static JSTemporalDurationRecord parseTemporalDurationString(TruffleString string) {
        long yearsMV;
        long monthsMV;
        long daysMV;
        long weeksMV;
        double hoursMV = 0;
        BigDecimal minutesMV;
        BigDecimal secondsMV;
        BigDecimal millisecondsMV;
        BigDecimal microsecondsMV;
        BigDecimal nanosecondsMV;

        TruffleString minutes = Strings.EMPTY_STRING;
        TruffleString seconds = Strings.EMPTY_STRING;

        TruffleString fHours = Strings.EMPTY_STRING;
        TruffleString fMinutes = Strings.EMPTY_STRING;
        TruffleString fSeconds = Strings.EMPTY_STRING;

        // P1Y1M1W1DT1H1M1.123456789S
        Pattern regex = Pattern.compile("^([\\+-]?)[Pp](\\d+[Yy])?(\\d+[Mm])?(\\d+[Ww])?(\\d+[Dd])?([Tt]([\\d.,]+[Hh])?([\\d.,]+[Mm])?([\\d.,]+[Ss])?)?$");
        Matcher matcher = regex.matcher(Strings.toJavaString(string));
        if (matcher.matches()) {
            if (matcher.start(2) < 0 && matcher.start(3) < 0 && matcher.start(4) < 0 && matcher.start(5) < 0 && matcher.start(7) < 0 && matcher.start(8) < 0 && matcher.start(9) < 0) {
                // neither DurationDate nor DurationTime found.
                throw TemporalErrors.createRangeErrorTemporalMalformedDuration();
            }

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

                hoursMV = TemporalUtil.toIntegerWithTruncation(hoursPair.getFirst()).doubleValue();
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
                minutesMV = new BigDecimal(TemporalUtil.toIntegerWithTruncation(fHoursDigits).doubleValue()).multiply(TemporalUtil.BD_60).scaleByPowerOfTen(-fHoursScale);
            } else {
                minutesMV = new BigDecimal(TemporalUtil.toIntegerWithTruncation(minutes).doubleValue());
            }
            if (!empty(fMinutes)) {
                if (!empty(seconds) || !empty(fSeconds)) {
                    throw TemporalErrors.createRangeErrorTemporalMalformedDuration();
                }
                assert !Strings.contains(fMinutes, '.');
                TruffleString fMinutesDigits = fMinutes; // substring(1) handled above
                int fMinutesScale = Strings.length(fMinutesDigits);
                secondsMV = new BigDecimal(TemporalUtil.toIntegerWithTruncation(fMinutesDigits).doubleValue()).multiply(TemporalUtil.BD_60).scaleByPowerOfTen(-fMinutesScale);
            } else if (!empty(seconds)) {
                secondsMV = new BigDecimal(TemporalUtil.toIntegerWithTruncation(seconds).doubleValue());
            } else {
                secondsMV = minutesMV.remainder(BigDecimal.ONE, TemporalUtil.mc_20_floor).multiply(TemporalUtil.BD_60);
            }

            if (!empty(fSeconds)) {
                assert !Strings.contains(fSeconds, '.'); // substring(1) handled above
                TruffleString fSecondsDigits = fSeconds;
                int fSecondsScale = Strings.length(fSecondsDigits);
                millisecondsMV = TemporalUtil.BD_1000.multiply(BigDecimal.valueOf(TemporalUtil.toIntegerWithTruncation(fSecondsDigits).longValue())).divide(
                                TemporalUtil.BD_10.pow(fSecondsScale));
            } else {
                millisecondsMV = secondsMV.remainder(BigDecimal.ONE, TemporalUtil.mc_20_floor).multiply(TemporalUtil.BD_1000, TemporalUtil.mc_20_floor);
            }
            microsecondsMV = millisecondsMV.remainder(BigDecimal.ONE, TemporalUtil.mc_20_floor).multiply(TemporalUtil.BD_1000);
            nanosecondsMV = microsecondsMV.remainder(BigDecimal.ONE, TemporalUtil.mc_20_floor).multiply(TemporalUtil.BD_1000);

            int factor = sign.equals(Strings.SYMBOL_MINUS) ? -1 : 1;

            return JSTemporalDurationRecord.createWeeks(yearsMV * factor, monthsMV * factor, weeksMV * factor, daysMV * factor, hoursMV * factor,
                            minutesMV.longValue() * factor, secondsMV.longValue() * factor,
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

            int idx = findDecimalSeparator(numstr, 0);
            if (idx >= 0) {
                TruffleString wholePart = Strings.lazySubstring(numstr, 0, idx);
                TruffleString fractionalPart = Strings.lazySubstring(numstr, idx + 1);
                if (Strings.length(fractionalPart) > 9) {
                    throw TemporalErrors.createRangeErrorTemporalMalformedDuration();
                }
                return new Pair<>(wholePart, fractionalPart);
            } else {
                return new Pair<>(numstr, Strings.EMPTY_STRING);
            }
        }
        return new Pair<>(Strings.EMPTY_STRING, Strings.EMPTY_STRING);
    }

    private static int findDecimalSeparator(TruffleString str, int startPos) {
        int idxDot = Strings.indexOf(str, '.', startPos);
        int idxComma = Strings.indexOf(str, ',', startPos);
        if (idxDot >= 0) {
            if (idxComma >= 0) {
                // cannot have both dot and comma in one number string
                throw TemporalErrors.createRangeErrorTemporalMalformedDuration();
            }
            if (Strings.indexOf(str, '.', idxDot + 1) >= 0) {
                // second dot found
                throw TemporalErrors.createRangeErrorTemporalMalformedDuration();
            }
            return idxDot;
        }
        if (Strings.indexOf(str, ',', idxComma + 1) >= 0) {
            // second comma found
            throw TemporalErrors.createRangeErrorTemporalMalformedDuration();
        }
        return idxComma;
    }

    @TruffleBoundary
    public static TruffleString temporalDurationToString(double yearsP, double monthsP, double weeksP, double daysP, double hoursP, double minutesP, double secondsP, double millisecondsP,
                    double microsecondsP, double nanosecondsP, Object precision, JSNumberToBigIntNode toBigIntNode) {
        int sign = TemporalUtil.durationSign(yearsP, monthsP, weeksP, daysP, hoursP, minutesP, secondsP, millisecondsP, microsecondsP, nanosecondsP);

        BigInt nanosecondsBI = toBigInteger(nanosecondsP, toBigIntNode);
        BigInt microsecondsBI = toBigInteger(microsecondsP, toBigIntNode);
        BigInt millisecondsBI = toBigInteger(millisecondsP, toBigIntNode);
        BigInt secondsBI = toBigInteger(secondsP, toBigIntNode);

        BigInt yearsBI = toBigIntegerOrNull(Math.abs(yearsP), toBigIntNode);
        BigInt monthsBI = toBigIntegerOrNull(Math.abs(monthsP), toBigIntNode);
        BigInt weeksBI = toBigIntegerOrNull(Math.abs(weeksP), toBigIntNode);
        BigInt daysBI = toBigIntegerOrNull(Math.abs(daysP), toBigIntNode);
        BigInt hoursBI = toBigIntegerOrNull(Math.abs(hoursP), toBigIntNode);
        BigInt minutesBI = toBigIntegerOrNull(Math.abs(minutesP), toBigIntNode);

        boolean condition = secondsP != 0 || millisecondsP != 0 || microsecondsP != 0 || nanosecondsP != 0 ||
                        (yearsP == 0 && monthsP == 0 && weeksP == 0 && daysP == 0 && hoursP == 0 && minutesP == 0);
        return temporalDurationToStringIntl(yearsBI, monthsBI, weeksBI, daysBI, hoursBI, minutesBI, secondsBI, millisecondsBI, microsecondsBI, nanosecondsBI, precision, sign, condition);
    }

    @TruffleBoundary
    private static TruffleString temporalDurationToStringIntl(BigInt yearsP, BigInt monthsP, BigInt weeksP, BigInt daysP, BigInt hoursP, BigInt minutesP, BigInt secondsP,
                    BigInt millisecondsP, BigInt microsecondsP, BigInt nanosecondsP, Object precision, int sign, boolean condition) {
        BigInt[] res = nanosecondsP.divideAndRemainder(TemporalUtil.BI_1000);
        BigInt microseconds = microsecondsP.add(res[0]);
        BigInt nanoseconds = res[1];

        res = microseconds.divideAndRemainder(TemporalUtil.BI_1000);
        BigInt milliseconds = millisecondsP.add(res[0]);
        microseconds = res[1];

        res = milliseconds.divideAndRemainder(TemporalUtil.BI_1000);
        BigInt seconds = secondsP.add(res[0]);
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
        var result = Strings.builderCreate();
        Strings.builderAppend(result, signPart);
        Strings.builderAppend(result, "P");
        Strings.builderAppend(result, datePart.toString());
        if (timePart.length() > 0) {
            Strings.builderAppend(result, "T");
            Strings.builderAppend(result, timePart.toString());
        }
        return Strings.builderToString(result);
    }

    private static BigInt toBigIntegerOrNull(double value, JSNumberToBigIntNode toBigIntNode) {
        return (value != 0) ? toBigIntNode.executeBigInt(value) : null;
    }

    private static BigInt toBigInteger(double value, JSNumberToBigIntNode toBigIntNode) {
        return toBigIntNode.executeBigInt(value);
    }
    // endregion
}
