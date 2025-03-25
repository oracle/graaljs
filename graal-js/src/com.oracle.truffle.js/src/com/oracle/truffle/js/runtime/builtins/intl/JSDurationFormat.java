/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins.intl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.AttributedCharacterIterator;
import java.text.Format;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.graalvm.shadowed.com.ibm.icu.number.FormattedNumber;
import org.graalvm.shadowed.com.ibm.icu.number.IntegerWidth;
import org.graalvm.shadowed.com.ibm.icu.number.LocalizedNumberFormatter;
import org.graalvm.shadowed.com.ibm.icu.number.NumberFormatter;
import org.graalvm.shadowed.com.ibm.icu.number.Precision;
import org.graalvm.shadowed.com.ibm.icu.text.ConstrainedFieldPosition;
import org.graalvm.shadowed.com.ibm.icu.text.DateFormatSymbols;
import org.graalvm.shadowed.com.ibm.icu.text.FormattedValue;
import org.graalvm.shadowed.com.ibm.icu.text.ListFormatter;
import org.graalvm.shadowed.com.ibm.icu.util.MeasureUnit;
import org.graalvm.shadowed.com.ibm.icu.util.ULocale;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.intl.DurationFormatFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.DurationFormatPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public final class JSDurationFormat extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {
    private static final BigDecimal BD_1E3 = BigDecimal.valueOf(1000);
    private static final BigDecimal BD_1E6 = BigDecimal.valueOf(1000_000);
    private static final BigDecimal BD_1E9 = BigDecimal.valueOf(1000_000_000);

    public static final TruffleString CLASS_NAME = Strings.constant("DurationFormat");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("DurationFormat.prototype");
    public static final TruffleString TO_STRING_TAG = Strings.constant("Intl.DurationFormat");

    public static final JSDurationFormat INSTANCE = new JSDurationFormat();

    private JSDurationFormat() {
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSObject durationFormatPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(durationFormatPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, durationFormatPrototype, DurationFormatPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(durationFormatPrototype, TO_STRING_TAG);
        return durationFormatPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, DurationFormatFunctionBuiltins.BUILTINS);
    }

    public static JSDurationFormatObject create(JSContext context, JSRealm realm, JSDynamicObject proto) {
        InternalState state = new InternalState();
        JSObjectFactory factory = context.getDurationFormatFactory();
        Shape shape = factory.getShape(realm, proto);
        JSDurationFormatObject newObj = factory.initProto(new JSDurationFormatObject(shape, proto, state), realm, proto);
        return factory.trackAllocation(newObj);
    }

    public static class InternalState extends AbstractInternalState {
        private String style;
        private String yearsStyle;
        private String yearsDisplay;
        private String monthsStyle;
        private String monthsDisplay;
        private String weeksStyle;
        private String weeksDisplay;
        private String daysStyle;
        private String daysDisplay;
        private String hoursStyle;
        private String hoursDisplay;
        private String minutesStyle;
        private String minutesDisplay;
        private String secondsStyle;
        private String secondsDisplay;
        private String millisecondsStyle;
        private String millisecondsDisplay;
        private String microsecondsStyle;
        private String microsecondsDisplay;
        private String nanosecondsStyle;
        private String nanosecondsDisplay;
        private int fractionalDigits;

        public void setStyle(String style) {
            this.style = style;
        }

        public void setYearsOptions(Pair<String, String> options) {
            this.yearsStyle = options.getFirst();
            this.yearsDisplay = options.getSecond();
        }

        public void setMonthsOptions(Pair<String, String> options) {
            this.monthsStyle = options.getFirst();
            this.monthsDisplay = options.getSecond();
        }

        public void setWeeksOptions(Pair<String, String> options) {
            this.weeksStyle = options.getFirst();
            this.weeksDisplay = options.getSecond();
        }

        public void setDaysOptions(Pair<String, String> options) {
            this.daysStyle = options.getFirst();
            this.daysDisplay = options.getSecond();
        }

        public void setHoursOptions(Pair<String, String> options) {
            this.hoursStyle = options.getFirst();
            this.hoursDisplay = options.getSecond();
        }

        public void setMinutesOptions(Pair<String, String> options) {
            this.minutesStyle = options.getFirst();
            this.minutesDisplay = options.getSecond();
        }

        public void setSecondsOptions(Pair<String, String> options) {
            this.secondsStyle = options.getFirst();
            this.secondsDisplay = options.getSecond();
        }

        public void setMillisecondsOptions(Pair<String, String> options) {
            this.millisecondsStyle = options.getFirst();
            this.millisecondsDisplay = options.getSecond();
        }

        public void setMicrosecondsOptions(Pair<String, String> options) {
            this.microsecondsStyle = options.getFirst();
            this.microsecondsDisplay = options.getSecond();
        }

        public void setNanosecondsOptions(Pair<String, String> options) {
            this.nanosecondsStyle = options.getFirst();
            this.nanosecondsDisplay = options.getSecond();
        }

        public void setFractionalDigits(int fractionalDigits) {
            this.fractionalDigits = fractionalDigits;
        }

        public JSObject toResolvedOptions(JSContext context, JSRealm realm) {
            JSObject result = JSOrdinary.create(context, realm);
            int defaultAttrs = JSAttributes.getDefault();
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_LOCALE, Strings.fromJavaString(locale), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_NUMBERING_SYSTEM, Strings.fromJavaString(numberingSystem), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_STYLE, Strings.fromJavaString(style), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_YEARS, Strings.fromJavaString(yearsStyle), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_YEARS_DISPLAY, Strings.fromJavaString(yearsDisplay), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_MONTHS, Strings.fromJavaString(monthsStyle), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_MONTHS_DISPLAY, Strings.fromJavaString(monthsDisplay), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_WEEKS, Strings.fromJavaString(weeksStyle), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_WEEKS_DISPLAY, Strings.fromJavaString(weeksDisplay), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_DAYS, Strings.fromJavaString(daysStyle), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_DAYS_DISPLAY, Strings.fromJavaString(daysDisplay), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_HOURS, Strings.fromJavaString(hoursStyle), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_HOURS_DISPLAY, Strings.fromJavaString(hoursDisplay), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_MINUTES, Strings.fromJavaString(minutesStyle), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_MINUTES_DISPLAY, Strings.fromJavaString(minutesDisplay), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_SECONDS, Strings.fromJavaString(secondsStyle), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_SECONDS_DISPLAY, Strings.fromJavaString(secondsDisplay), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_MILLISECONDS, toResolvedOption(millisecondsStyle), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_MILLISECONDS_DISPLAY, Strings.fromJavaString(millisecondsDisplay), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_MICROSECONDS, toResolvedOption(microsecondsStyle), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_MICROSECONDS_DISPLAY, Strings.fromJavaString(microsecondsDisplay), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_NANOSECONDS, toResolvedOption(nanosecondsStyle), defaultAttrs);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_NANOSECONDS_DISPLAY, Strings.fromJavaString(nanosecondsDisplay), defaultAttrs);
            if (fractionalDigits >= 0) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_FRACTIONAL_DIGITS, fractionalDigits, defaultAttrs);
            }
            return result;
        }

        private static TruffleString toResolvedOption(String subSecondStyle) {
            if (IntlUtil.FRACTIONAL.equals(subSecondStyle)) {
                return IntlUtil.KEY_NUMERIC;
            } else {
                return Strings.fromJavaString(subSecondStyle);
            }
        }

    }

    @TruffleBoundary
    public static JSObject resolvedOptions(JSContext context, JSRealm realm, JSDurationFormatObject durationFormat) {
        return durationFormat.getInternalState().toResolvedOptions(context, realm);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getPluralRulesPrototype();
    }

    @TruffleBoundary
    public static String format(InternalState state, JSTemporalDurationRecord duration) {
        String separator = getSeparator(state.javaLocale);
        List<FormattedNumber> numbers = toFormattedNumbers(state, duration, null);
        Deque<String> strings = toFormattedStrings(separator, numbers);
        ListFormatter listFormatter = ListFormatter.getInstance(ULocale.forLocale(state.javaLocale), ListFormatter.Type.UNITS, toICUListFormatterWidth(state.style));
        return listFormatter.format(strings);
    }

    private static Deque<String> toFormattedStrings(String separator, List<FormattedNumber> numbers) {
        Deque<String> strings = new ArrayDeque<>();
        boolean addSeparator = false;
        for (FormattedNumber number : numbers) {
            if (number == null) {
                addSeparator = true;
            } else if (addSeparator && !strings.isEmpty()) {
                addSeparator = false;
                strings.add(strings.removeLast() + separator + number);
            } else {
                addSeparator = false;
                strings.add(number.toString());
            }
        }
        return strings;
    }

    @TruffleBoundary
    public static Object formatToParts(JSContext context, JSRealm realm, InternalState state, JSTemporalDurationRecord duration) {
        String separator = getSeparator(state.javaLocale);
        List<MeasureUnit> units = new ArrayList<>();
        List<FormattedNumber> numbers = toFormattedNumbers(state, duration, units);
        Deque<String> strings = toFormattedStrings(separator, numbers);

        ListFormatter listFormatter = ListFormatter.getInstance(ULocale.forLocale(state.javaLocale), ListFormatter.Type.UNITS, toICUListFormatterWidth(state.style));
        ListFormatter.FormattedList formattedList = listFormatter.formatToValue(strings);

        List<Object> resultParts = new ArrayList<>();

        int idx = 0;
        Iterator<MeasureUnit> unitsIterator = units.iterator();
        ConstrainedFieldPosition cfPos = new ConstrainedFieldPosition();
        while (formattedList.nextPosition(cfPos)) {
            Format.Field field = cfPos.getField();
            if (field == ListFormatter.Field.LITERAL) {
                String value = formattedList.subSequence(cfPos.getStart(), cfPos.getLimit()).toString();
                resultParts.add(IntlUtil.makePart(context, realm, IntlUtil.LITERAL, value));
            } else if (field == ListFormatter.Field.ELEMENT) {
                do {
                    FormattedValue formattedValue = numbers.get(idx++);
                    if (formattedValue == null) {
                        resultParts.add(IntlUtil.makePart(context, realm, IntlUtil.LITERAL, separator));
                        formattedValue = numbers.get(idx++);
                    }
                    MeasureUnit icuUnit = unitsIterator.next();
                    String unit = icuUnit.getIdentifier();
                    AttributedCharacterIterator fit = formattedValue.toCharacterIterator();
                    String formatted = formattedValue.toString();
                    resultParts.addAll(JSNumberFormat.innerFormatToParts(context, realm, fit, 0, formatted, unit, false));
                } while (idx < numbers.size() && numbers.get(idx) == null);
            }
        }

        return JSArray.createConstant(context, realm, resultParts.toArray());
    }

    private static ListFormatter.Width toICUListFormatterWidth(String style) {
        return switch (style) {
            case IntlUtil.DIGITAL -> ListFormatter.Width.SHORT;
            case IntlUtil.LONG -> ListFormatter.Width.WIDE;
            case IntlUtil.NARROW -> ListFormatter.Width.NARROW;
            case IntlUtil.SHORT -> ListFormatter.Width.SHORT;
            default -> throw Errors.shouldNotReachHereUnexpectedValue(style);
        };
    }

    @TruffleBoundary
    public static List<FormattedNumber> toFormattedNumbers(InternalState state, JSTemporalDurationRecord duration, List<MeasureUnit> units) {
        boolean displaySign = TemporalUtil.durationSign(duration.getYears(), duration.getMonths(), duration.getWeeks(),
                        duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                        duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds()) < 0;
        List<FormattedNumber> numbers = new ArrayList<>();
        LocalizedNumberFormatter numberFormatter = NumberFormatter.withLocale(state.javaLocale);

        displaySign &= formatUnit(numberFormatter, duration.getYears(), MeasureUnit.YEAR, state.yearsStyle, state.yearsDisplay, numbers, false, displaySign, units);
        displaySign &= formatUnit(numberFormatter, duration.getMonths(), MeasureUnit.MONTH, state.monthsStyle, state.monthsDisplay, numbers, false, displaySign, units);
        displaySign &= formatUnit(numberFormatter, duration.getWeeks(), MeasureUnit.WEEK, state.weeksStyle, state.weeksDisplay, numbers, false, displaySign, units);
        displaySign &= formatUnit(numberFormatter, duration.getDays(), MeasureUnit.DAY, state.daysStyle, state.daysDisplay, numbers, false, displaySign, units);
        displaySign &= formatUnit(numberFormatter, duration.getHours(), MeasureUnit.HOUR, state.hoursStyle, state.hoursDisplay, numbers, false, displaySign, units);
        boolean addSeparator = (duration.getHours() != 0 || IntlUtil.ALWAYS.equals(state.hoursDisplay)) && IntlUtil.NUMERIC.equals(state.hoursStyle) || IntlUtil._2_DIGIT.equals(state.hoursStyle);
        String minutesDisplay = addSeparator && (IntlUtil.ALWAYS.equals(state.secondsDisplay) || duration.getSeconds() != 0 || duration.getMilliseconds() != 0 || duration.getMicroseconds() != 0 ||
                        duration.getNanoseconds() != 0) ? IntlUtil.ALWAYS : state.minutesDisplay;
        displaySign &= formatUnit(numberFormatter, duration.getMinutes(), MeasureUnit.MINUTE, state.minutesStyle, minutesDisplay, numbers, addSeparator, displaySign, units);

        int maximumFractionDigits;
        int minimumFractionDigits;
        if (state.fractionalDigits == -1) {
            maximumFractionDigits = 9;
            minimumFractionDigits = 0;
        } else {
            maximumFractionDigits = minimumFractionDigits = state.fractionalDigits;
        }
        numberFormatter = numberFormatter.precision(Precision.minMaxFraction(minimumFractionDigits, maximumFractionDigits)).roundingMode(RoundingMode.DOWN);

        if (IntlUtil.FRACTIONAL.equals(state.millisecondsStyle)) {
            BigDecimal value = new BigDecimal(duration.getSeconds());
            value = value.add(new BigDecimal(duration.getMilliseconds()).divide(BD_1E3));
            value = value.add(new BigDecimal(duration.getMicroseconds()).divide(BD_1E6));
            value = value.add(new BigDecimal(duration.getNanoseconds()).divide(BD_1E9));
            formatUnit(numberFormatter, value, MeasureUnit.SECOND, state.secondsStyle, state.secondsDisplay, numbers, true, displaySign, units);
        } else {
            displaySign &= formatUnit(numberFormatter, duration.getSeconds(), MeasureUnit.SECOND, state.secondsStyle, state.secondsDisplay, numbers, true, displaySign, units);
            if (IntlUtil.FRACTIONAL.equals(state.microsecondsStyle)) {
                BigDecimal value = new BigDecimal(duration.getMilliseconds());
                value = value.add(new BigDecimal(duration.getMicroseconds()).divide(BD_1E3));
                value = value.add(new BigDecimal(duration.getNanoseconds()).divide(BD_1E6));
                formatUnit(numberFormatter, value, MeasureUnit.MILLISECOND, state.millisecondsStyle, state.millisecondsDisplay, numbers, false, displaySign, units);
            } else {
                displaySign &= formatUnit(numberFormatter, duration.getMilliseconds(), MeasureUnit.MILLISECOND, state.millisecondsStyle, state.millisecondsDisplay, numbers, false, displaySign, units);
                if (IntlUtil.FRACTIONAL.equals(state.nanosecondsStyle)) {
                    BigDecimal value = new BigDecimal(duration.getMicroseconds());
                    value = value.add(new BigDecimal(duration.getNanoseconds()).divide(BD_1E3));
                    formatUnit(numberFormatter, value, MeasureUnit.MICROSECOND, state.microsecondsStyle, state.microsecondsDisplay, numbers, false, displaySign, units);
                } else {
                    displaySign &= formatUnit(numberFormatter, duration.getMicroseconds(), MeasureUnit.MICROSECOND, state.microsecondsStyle, state.microsecondsDisplay, numbers, false, displaySign,
                                    units);
                    formatUnit(numberFormatter, duration.getNanoseconds(), MeasureUnit.NANOSECOND, state.nanosecondsStyle, state.nanosecondsDisplay, numbers, false, displaySign, units);
                }
            }
        }

        return numbers;
    }

    @SuppressWarnings("deprecation")
    private static String getSeparator(Locale locale) {
        return DateFormatSymbols.getInstance(locale).getTimeSeparatorString();
    }

    private static boolean formatUnit(LocalizedNumberFormatter baseFormatter, Number value, MeasureUnit unit, String style, String display, List<FormattedNumber> numbers, boolean addSeparator,
                    boolean displaySign, List<MeasureUnit> units) {
        boolean valueIsZero = (value.doubleValue() == 0);
        if (valueIsZero && !IntlUtil.ALWAYS.equals(display)) {
            return true;
        }

        boolean styleNumeric = IntlUtil.NUMERIC.equals(style);
        boolean style2Digit = !styleNumeric && IntlUtil._2_DIGIT.equals(style);
        if (addSeparator && !numbers.isEmpty() && (unit == MeasureUnit.SECOND || unit == MeasureUnit.MINUTE) && (styleNumeric || style2Digit)) {
            numbers.add(null); // separator
        }

        Number valueToFormat = value;
        LocalizedNumberFormatter signedFormatter = baseFormatter;
        boolean signDisplayed = false;
        if (displaySign) {
            signDisplayed = true;
            if (valueIsZero) {
                valueToFormat = -0.0;
            }
        } else {
            signedFormatter = baseFormatter.sign(NumberFormatter.SignDisplay.NEVER);
        }

        FormattedNumber formatted;
        if (styleNumeric) {
            formatted = signedFormatter.grouping(NumberFormatter.GroupingStrategy.OFF).format(valueToFormat);
        } else if (style2Digit) {
            formatted = signedFormatter.integerWidth(IntegerWidth.zeroFillTo(2)).grouping(NumberFormatter.GroupingStrategy.OFF).format(valueToFormat);
        } else {
            formatted = signedFormatter.unit(unit).unitWidth(toICUUnitWidth(style)).format(valueToFormat);
        }

        numbers.add(formatted);
        if (units != null) {
            units.add(unit);
        }

        return !signDisplayed;
    }

    private static NumberFormatter.UnitWidth toICUUnitWidth(String style) {
        return switch (style) {
            case IntlUtil.LONG -> NumberFormatter.UnitWidth.FULL_NAME;
            case IntlUtil.SHORT -> NumberFormatter.UnitWidth.SHORT;
            case IntlUtil.NARROW -> NumberFormatter.UnitWidth.NARROW;
            default -> throw Errors.shouldNotReachHereUnexpectedValue(style);
        };
    }

}
