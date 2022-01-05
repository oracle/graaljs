/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.UnmodifiableEconomicMap;

import com.ibm.icu.number.FormattedNumberRange;
import com.ibm.icu.number.FractionPrecision;
import com.ibm.icu.number.IntegerWidth;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.LocalizedNumberRangeFormatter;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.number.NumberRangeFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.Scale;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.text.ConstrainedFieldPosition;
import com.ibm.icu.text.FormattedValue;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.MeasureUnit;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.intl.NumberFormatFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.NumberFormatPrototypeBuiltins;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.intl.ToIntlMathematicalValue;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.LazyValue;

public final class JSNumberFormat extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String CLASS_NAME = "NumberFormat";
    public static final String PROTOTYPE_NAME = "NumberFormat.prototype";

    static final HiddenKey BOUND_OBJECT_KEY = new HiddenKey(CLASS_NAME);

    public static final JSNumberFormat INSTANCE = new JSNumberFormat();

    private JSNumberFormat() {
    }

    public static boolean isJSNumberFormat(Object obj) {
        return obj instanceof JSNumberFormatObject;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject numberFormatPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, numberFormatPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, numberFormatPrototype, NumberFormatPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putBuiltinAccessorProperty(numberFormatPrototype, "format", createFormatFunctionGetter(realm, ctx), Undefined.instance);
        JSObjectUtil.putToStringTag(numberFormatPrototype, "Intl.NumberFormat");
        return numberFormatPrototype;
    }

    // JDK does not attempt to track historical currencies but it keeps (some) data about currencies
    // that were active currencies in previous JDK releases. Historical currencies do not have the
    // minor unit value defined by ISO 4217. Unfortunately, JDK does not return -1 ("undefined") for
    // fraction digits for historical currencies => we have to keep track of these currencies
    // to return the digits expected by ECMAScript specification.
    private static final Set<String> historicalCurrenciesInJDK = new HashSet<>(Arrays.asList(new String[]{
                    "ADP",
                    "BEF",
                    "BYB",
                    "BYR",
                    "ESP",
                    "GRD",
                    "ITL",
                    "LUF",
                    "MGF",
                    "PTE",
                    "ROL",
                    "TPE",
                    "TRL",
    }));

    // https://tc39.github.io/ecma402/#sec-currencydigits
    @TruffleBoundary
    public static int currencyDigits(JSContext context, String currencyCode) {
        if (context.isOptionV8CompatibilityMode()) {
            // ICU is using CLDR data that differ from ISO 4217 data for several currencies.
            return com.ibm.icu.util.Currency.getInstance(currencyCode).getDefaultFractionDigits();
        } else {
            if (historicalCurrenciesInJDK.contains(currencyCode)) {
                return 2;
            }
            try {
                int digits = Currency.getInstance(currencyCode).getDefaultFractionDigits();
                return (digits == -1) ? 2 : digits;
            } catch (IllegalArgumentException e) {
                return 2;
            }
        }
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, NumberFormatFunctionBuiltins.BUILTINS);
    }

    public static DynamicObject create(JSContext context, JSRealm realm) {
        InternalState state = new InternalState();
        JSObjectFactory factory = context.getNumberFormatFactory();
        JSNumberFormatObject obj = new JSNumberFormatObject(factory.getShape(realm), state);
        factory.initProto(obj, realm);
        assert isJSNumberFormat(obj);
        return context.trackAllocation(obj);
    }

    private static Notation notationToICUNotation(String notation, String compactDisplay) {
        Notation icuNotation;
        switch (notation) {
            case IntlUtil.STANDARD:
                icuNotation = Notation.simple();
                break;
            case IntlUtil.SCIENTIFIC:
                icuNotation = Notation.scientific();
                break;
            case IntlUtil.ENGINEERING:
                icuNotation = Notation.engineering();
                break;
            case IntlUtil.COMPACT:
                icuNotation = IntlUtil.LONG.equals(compactDisplay) ? Notation.compactLong() : Notation.compactShort();
                break;
            default:
                throw Errors.shouldNotReachHere(notation);
        }
        return icuNotation;
    }

    private static UnitWidth currencyDisplayToUnitWidth(String currencyDisplay) {
        UnitWidth unitWidth;
        switch (currencyDisplay) {
            case IntlUtil.CODE:
                unitWidth = UnitWidth.ISO_CODE;
                break;
            case IntlUtil.SYMBOL:
                unitWidth = UnitWidth.SHORT;
                break;
            case IntlUtil.NARROW_SYMBOL:
                unitWidth = UnitWidth.NARROW;
                break;
            case IntlUtil.NAME:
                unitWidth = UnitWidth.FULL_NAME;
                break;
            default:
                throw Errors.shouldNotReachHere(currencyDisplay);
        }
        return unitWidth;
    }

    private static UnitWidth unitDisplayToUnitWidth(String unitDisplay) {
        UnitWidth unitWidth;
        switch (unitDisplay) {
            case IntlUtil.SHORT:
                unitWidth = UnitWidth.SHORT;
                break;
            case IntlUtil.NARROW:
                unitWidth = UnitWidth.NARROW;
                break;
            case IntlUtil.LONG:
                unitWidth = UnitWidth.FULL_NAME;
                break;
            default:
                throw Errors.shouldNotReachHere(unitDisplay);
        }
        return unitWidth;
    }

    private static NumberFormatter.GroupingStrategy useGroupingToGroupingStrategy(Object useGrouping) {
        NumberFormatter.GroupingStrategy strategy;
        if (Boolean.FALSE.equals(useGrouping)) {
            strategy = NumberFormatter.GroupingStrategy.OFF;
        } else if (IntlUtil.MIN2.equals(useGrouping)) {
            strategy = NumberFormatter.GroupingStrategy.MIN2;
        } else if (IntlUtil.AUTO.equals(useGrouping)) {
            strategy = NumberFormatter.GroupingStrategy.AUTO;
        } else {
            assert IntlUtil.ALWAYS.equals(useGrouping);
            strategy = NumberFormatter.GroupingStrategy.ON_ALIGNED;
        }
        return strategy;
    }

    private static RoundingMode roundingModeToICURoundingMode(String roundingMode) {
        RoundingMode mode;
        switch (roundingMode) {
            case IntlUtil.CEIL:
                mode = RoundingMode.CEILING;
                break;
            case IntlUtil.FLOOR:
                mode = RoundingMode.FLOOR;
                break;
            case IntlUtil.EXPAND:
                mode = RoundingMode.UP;
                break;
            case IntlUtil.TRUNC:
                mode = RoundingMode.DOWN;
                break;
            case IntlUtil.HALF_CEIL:
                // ICU4J does not support HALF_CEIL directly => we use
                // HALF_UP for non-negative and HALF_DOWN for negative
                // as a workaround, see formattedValue()
                mode = RoundingMode.HALF_UP;
                break;
            case IntlUtil.HALF_FLOOR:
                // ICU4J does not support HALF_FLOOR directly => we use
                // HALF_DOWN for non-negative and HALF_UP for negative
                // as a workaround, see formattedValue()
                mode = RoundingMode.HALF_DOWN;
                break;
            case IntlUtil.HALF_EXPAND:
                mode = RoundingMode.HALF_UP;
                break;
            case IntlUtil.HALF_TRUNC:
                mode = RoundingMode.HALF_DOWN;
                break;
            case IntlUtil.HALF_EVEN:
                mode = RoundingMode.HALF_EVEN;
                break;
            default:
                throw Errors.shouldNotReachHere(roundingMode);
        }
        return mode;
    }

    private static MeasureUnit unitToMeasureUnit(String unit) {
        MeasureUnit measureUnit;
        switch (unit) {
            case "acre":
                measureUnit = MeasureUnit.ACRE;
                break;
            case "bit":
                measureUnit = MeasureUnit.BIT;
                break;
            case "byte":
                measureUnit = MeasureUnit.BYTE;
                break;
            case "celsius":
                measureUnit = MeasureUnit.CELSIUS;
                break;
            case "centimeter":
                measureUnit = MeasureUnit.CENTIMETER;
                break;
            case "day":
                measureUnit = MeasureUnit.DAY;
                break;
            case "degree":
                measureUnit = MeasureUnit.DEGREE;
                break;
            case "fahrenheit":
                measureUnit = MeasureUnit.FAHRENHEIT;
                break;
            case "fluid-ounce":
                measureUnit = MeasureUnit.FLUID_OUNCE;
                break;
            case "foot":
                measureUnit = MeasureUnit.FOOT;
                break;
            case "gallon":
                measureUnit = MeasureUnit.GALLON;
                break;
            case "gigabit":
                measureUnit = MeasureUnit.GIGABIT;
                break;
            case "gigabyte":
                measureUnit = MeasureUnit.GIGABYTE;
                break;
            case "gram":
                measureUnit = MeasureUnit.GRAM;
                break;
            case "hectare":
                measureUnit = MeasureUnit.HECTARE;
                break;
            case "hour":
                measureUnit = MeasureUnit.HOUR;
                break;
            case "inch":
                measureUnit = MeasureUnit.INCH;
                break;
            case "kilobit":
                measureUnit = MeasureUnit.KILOBIT;
                break;
            case "kilobyte":
                measureUnit = MeasureUnit.KILOBYTE;
                break;
            case "kilogram":
                measureUnit = MeasureUnit.KILOGRAM;
                break;
            case "kilometer":
                measureUnit = MeasureUnit.KILOMETER;
                break;
            case "liter":
                measureUnit = MeasureUnit.LITER;
                break;
            case "megabit":
                measureUnit = MeasureUnit.MEGABIT;
                break;
            case "megabyte":
                measureUnit = MeasureUnit.MEGABYTE;
                break;
            case "meter":
                measureUnit = MeasureUnit.METER;
                break;
            case "mile":
                measureUnit = MeasureUnit.MILE;
                break;
            case "mile-scandinavian":
                measureUnit = MeasureUnit.MILE_SCANDINAVIAN;
                break;
            case "milliliter":
                measureUnit = MeasureUnit.MILLILITER;
                break;
            case "millimeter":
                measureUnit = MeasureUnit.MILLIMETER;
                break;
            case "millisecond":
                measureUnit = MeasureUnit.MILLISECOND;
                break;
            case "minute":
                measureUnit = MeasureUnit.MINUTE;
                break;
            case "month":
                measureUnit = MeasureUnit.MONTH;
                break;
            case "ounce":
                measureUnit = MeasureUnit.OUNCE;
                break;
            case "percent":
                measureUnit = MeasureUnit.PERCENT;
                break;
            case "petabyte":
                measureUnit = MeasureUnit.PETABYTE;
                break;
            case "pound":
                measureUnit = MeasureUnit.POUND;
                break;
            case "second":
                measureUnit = MeasureUnit.SECOND;
                break;
            case "stone":
                measureUnit = MeasureUnit.STONE;
                break;
            case "terabit":
                measureUnit = MeasureUnit.TERABIT;
                break;
            case "terabyte":
                measureUnit = MeasureUnit.TERABYTE;
                break;
            case "week":
                measureUnit = MeasureUnit.WEEK;
                break;
            case "yard":
                measureUnit = MeasureUnit.YARD;
                break;
            case "year":
                measureUnit = MeasureUnit.YEAR;
                break;
            default:
                throw Errors.shouldNotReachHere(unit);
        }
        return measureUnit;
    }

    private static SignDisplay signDisplay(String signDisplay, boolean accounting) {
        switch (signDisplay) {
            case IntlUtil.AUTO:
                return accounting ? SignDisplay.ACCOUNTING : SignDisplay.AUTO;
            case IntlUtil.NEVER:
                return SignDisplay.NEVER;
            case IntlUtil.ALWAYS:
                return accounting ? SignDisplay.ACCOUNTING_ALWAYS : SignDisplay.ALWAYS;
            case IntlUtil.EXCEPT_ZERO:
                return accounting ? SignDisplay.ACCOUNTING_EXCEPT_ZERO : SignDisplay.EXCEPT_ZERO;
            case IntlUtil.NEGATIVE:
                return accounting ? SignDisplay.ACCOUNTING_NEGATIVE : SignDisplay.NEGATIVE;
            default:
                throw Errors.shouldNotReachHere(signDisplay);
        }
    }

    private static FormattedValue formattedValue(InternalState state, Number x) {
        LocalizedNumberFormatter numberFormatter = state.getNumberFormatter(x.doubleValue() < 0);
        return numberFormatter.format(x);
    }

    @TruffleBoundary
    public static String format(DynamicObject numberFormatObj, Object n) {
        InternalState state = getInternalState(numberFormatObj);
        Number x = toInternalNumberRepresentation(JSRuntime.toNumeric(n));
        return formattedValue(state, x).toString();
    }

    public static String formatMV(DynamicObject numberFormatObj, Number mv) {
        InternalState state = getInternalState(numberFormatObj);
        return formattedValue(state, mv).toString();
    }

    private static FormattedNumberRange formatRangeImpl(DynamicObject numberFormatObj, Number x, Number y) {
        boolean rangeError = false;
        if (JSRuntime.isNaN(x) || JSRuntime.isNaN(y)) {
            rangeError = true;
        }
        if (x instanceof BigDecimal) {
            BigDecimal xDecimal = (BigDecimal) x;
            if (y instanceof BigDecimal && ((BigDecimal) y).compareTo(xDecimal) < 0) {
                rangeError = true;
            } else if (y instanceof Double) {
                double yDouble = (Double) y;
                if (Double.NEGATIVE_INFINITY == yDouble) {
                    rangeError = true;
                } else if (JSRuntime.isNegativeZero(yDouble) && xDecimal.signum() >= 0) {
                    rangeError = true;
                }
            }
        } else if (x instanceof Double) {
            double xDouble = (Double) x;
            if (Double.POSITIVE_INFINITY == xDouble) {
                if (y instanceof BigDecimal) {
                    rangeError = true;
                } else if (y instanceof Double) {
                    double yDouble = (Double) y;
                    if (Double.NEGATIVE_INFINITY == yDouble || JSRuntime.isNegativeZero(yDouble)) {
                        rangeError = true;
                    }
                }
            } else if (JSRuntime.isNegativeZero(xDouble)) {
                if (y instanceof BigDecimal && ((BigDecimal) y).signum() == -1) {
                    rangeError = true;
                } else if (y instanceof Double && Double.NEGATIVE_INFINITY == (Double) y) {
                    rangeError = true;
                }
            }
        }
        if (rangeError) {
            throw Errors.createRangeError("invalid range");
        }
        InternalState state = getInternalState(numberFormatObj);
        LocalizedNumberRangeFormatter formatter = state.getNumberRangeFormatter();
        return formatter.formatRange(x, y);
    }

    @TruffleBoundary
    public static String formatRange(DynamicObject numberFormatObj, Number x, Number y) {
        return formatRangeImpl(numberFormatObj, x, y).toString();
    }

    @TruffleBoundary
    public static DynamicObject formatRangeToParts(JSContext context, JSRealm realm, DynamicObject numberFormatObj, Number x, Number y) {
        FormattedNumberRange formattedRange = formatRangeImpl(numberFormatObj, x, y);
        String formattedString = formattedRange.toString();

        List<Object> parts = new ArrayList<>();
        int startRangeStart = 0;
        int startRangeLimit = 0;
        int endRangeStart = 0;
        int endRangeLimit = 0;
        int lastLimit = 0;

        ConstrainedFieldPosition cfPos = new ConstrainedFieldPosition();
        while (formattedRange.nextPosition(cfPos)) {
            int start = cfPos.getStart();
            int limit = cfPos.getLimit();

            if (lastLimit < start) { // Literal
                String literal = formattedString.substring(lastLimit, start);
                String source = IntlUtil.sourceString(lastLimit, start, startRangeStart, startRangeLimit, endRangeStart, endRangeLimit);
                parts.add(IntlUtil.makePart(context, realm, IntlUtil.LITERAL, literal, null, source));
                lastLimit = start;
            }

            Format.Field field = cfPos.getField();
            if (field instanceof NumberRangeFormatter.SpanField) {
                Object fieldValue = cfPos.getFieldValue();
                if (fieldValue.equals(0)) {
                    startRangeStart = start;
                    startRangeLimit = limit;
                } else if (fieldValue.equals(1)) {
                    endRangeStart = start;
                    endRangeLimit = limit;
                } else {
                    throw Errors.shouldNotReachHere(fieldValue.toString());
                }
            } else if (field instanceof NumberFormat.Field) {
                String type;
                String value = formattedString.substring(start, limit);
                String source = IntlUtil.sourceString(start, limit, startRangeStart, startRangeLimit, endRangeStart, endRangeLimit);
                if (field == NumberFormat.Field.SIGN) {
                    type = isPlusSign(value) ? "plusSign" : "minusSign";
                } else if (field == NumberFormat.Field.INTEGER) {
                    Object val = IntlUtil.END_RANGE.equals(source) ? y : y;
                    if (JSRuntime.isNaN(val)) {
                        type = "nan";
                    } else if (val instanceof Double && Double.isInfinite((Double) val)) {
                        type = "infinity";
                    } else {
                        type = "integer";
                    }
                } else {
                    type = fieldToType((NumberFormat.Field) field);
                    assert type != null : field;
                }
                parts.add(IntlUtil.makePart(context, realm, type, value, null, source));
                lastLimit = limit;
            } else {
                throw Errors.shouldNotReachHere(field.toString());
            }
        }

        int length = formattedString.length();
        if (lastLimit < length) { // Literal at the end
            String literal = formattedString.substring(lastLimit, length);
            String source = IntlUtil.sourceString(lastLimit, length, startRangeStart, startRangeLimit, endRangeStart, endRangeLimit);
            parts.add(IntlUtil.makePart(context, realm, IntlUtil.LITERAL, literal, null, source));
        }

        return JSArray.createConstant(context, realm, parts.toArray());
    }

    private static final LazyValue<UnmodifiableEconomicMap<NumberFormat.Field, String>> fieldToTypeMap = new LazyValue<>(JSNumberFormat::initializeFieldToTypeMap);

    private static UnmodifiableEconomicMap<NumberFormat.Field, String> initializeFieldToTypeMap() {
        CompilerAsserts.neverPartOfCompilation();
        EconomicMap<NumberFormat.Field, String> map = EconomicMap.create(6);
        map.put(NumberFormat.Field.DECIMAL_SEPARATOR, "decimal");
        map.put(NumberFormat.Field.FRACTION, "fraction");
        map.put(NumberFormat.Field.GROUPING_SEPARATOR, "group");
        map.put(NumberFormat.Field.CURRENCY, "currency");
        map.put(NumberFormat.Field.MEASURE_UNIT, "unit");
        map.put(NumberFormat.Field.EXPONENT_SYMBOL, "exponentSeparator");
        map.put(NumberFormat.Field.EXPONENT_SIGN, "exponentMinusSign");
        map.put(NumberFormat.Field.EXPONENT, "exponentInteger");
        map.put(NumberFormat.Field.COMPACT, "compact");
        return map;
    }

    private static String fieldToType(NumberFormat.Field field) {
        return fieldToTypeMap.get().get(field);
    }

    @TruffleBoundary
    public static DynamicObject formatToParts(JSContext context, JSRealm realm, DynamicObject numberFormatObj, Object n) {
        InternalState state = getInternalState(numberFormatObj);
        Number x = toInternalNumberRepresentation(JSRuntime.toNumeric(n));
        FormattedValue formattedValue = formattedValue(state, x);
        AttributedCharacterIterator fit = formattedValue.toCharacterIterator();
        String formatted = formattedValue.toString();
        List<DynamicObject> resultParts = innerFormatToParts(context, realm, fit, x.doubleValue(), formatted, null, IntlUtil.PERCENT.equals(state.getStyle()));
        return JSArray.createConstant(context, realm, resultParts.toArray());
    }

    static List<DynamicObject> innerFormatToParts(JSContext context, JSRealm realm, AttributedCharacterIterator iterator, double value, String formattedValue, String unit, boolean stylePercent) {
        List<DynamicObject> resultParts = new ArrayList<>();
        int i = iterator.getBeginIndex();
        while (i < iterator.getEndIndex()) {
            iterator.setIndex(i);
            Map<AttributedCharacterIterator.Attribute, Object> attributes = iterator.getAttributes();
            Set<AttributedCharacterIterator.Attribute> attKeySet = attributes.keySet();
            if (!attKeySet.isEmpty()) {
                for (AttributedCharacterIterator.Attribute a : attKeySet) {
                    if (a instanceof NumberFormat.Field) {
                        String run = formattedValue.substring(iterator.getRunStart(), iterator.getRunLimit());
                        String type;
                        if (a == NumberFormat.Field.INTEGER) {
                            if (Double.isNaN(value)) {
                                type = "nan";
                            } else if (Double.isInfinite(value)) {
                                type = "infinity";
                            } else {
                                type = "integer";
                            }
                        } else if (a == NumberFormat.Field.SIGN) {
                            type = isPlusSign(run) ? "plusSign" : "minusSign";
                        } else if (a == NumberFormat.Field.PERCENT) {
                            type = stylePercent ? "percentSign" : "unit";
                        } else {
                            type = fieldToType((NumberFormat.Field) a);
                            assert type != null : a;
                        }
                        resultParts.add(IntlUtil.makePart(context, realm, type, run, unit));
                        i = iterator.getRunLimit();
                        break;
                    } else {
                        throw Errors.shouldNotReachHere();
                    }
                }
            } else {
                String run = formattedValue.substring(iterator.getRunStart(), iterator.getRunLimit());
                resultParts.add(IntlUtil.makePart(context, realm, IntlUtil.LITERAL, run, unit));
                i = iterator.getRunLimit();
            }
        }
        return resultParts;
    }

    private static boolean isPlusSign(String str) {
        return str.length() == 1 && str.charAt(0) == '+';
    }

    private static Number toInternalNumberRepresentation(Object o) {
        if (o instanceof SafeInteger) {
            return ((SafeInteger) o).doubleValue();
        } else if (o instanceof Double) {
            // ICU4J checks sign bit even for NaN => normalize it so that it is
            // not formatted as "-NaN"
            return Double.isNaN((Double) o) ? Double.NaN : (Double) o;
        } else if (o instanceof Number) {
            return (Number) o;
        } else if (o instanceof BigInt) {
            return ((BigInt) o).bigIntegerValue();
        } else {
            throw Errors.shouldNotReachHere();
        }
    }

    public static class BasicInternalState {
        private UnlocalizedNumberFormatter unlocalizedFormatter;
        private Precision precision;

        private Locale javaLocale;
        private String locale;

        private String numberingSystem;
        private int minimumIntegerDigits;
        private Integer minimumFractionDigits;
        private Integer maximumFractionDigits;
        private Integer minimumSignificantDigits;
        private Integer maximumSignificantDigits;
        private String roundingType;

        DynamicObject toResolvedOptionsObject(JSContext context, JSRealm realm) {
            DynamicObject resolvedOptions = JSOrdinary.create(context, realm);
            fillResolvedOptions(context, realm, resolvedOptions);
            return resolvedOptions;
        }

        void fillResolvedOptions(JSContext context, @SuppressWarnings("unused") JSRealm realm, DynamicObject result) {
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.MINIMUM_INTEGER_DIGITS, minimumIntegerDigits, JSAttributes.getDefault());
            if (minimumFractionDigits != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.MINIMUM_FRACTION_DIGITS, minimumFractionDigits, JSAttributes.getDefault());
            }
            if (maximumFractionDigits != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.MAXIMUM_FRACTION_DIGITS, maximumFractionDigits, JSAttributes.getDefault());
            }
            if (minimumSignificantDigits != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.MINIMUM_SIGNIFICANT_DIGITS, minimumSignificantDigits, JSAttributes.getDefault());
            }
            if (maximumSignificantDigits != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.MAXIMUM_SIGNIFICANT_DIGITS, maximumSignificantDigits, JSAttributes.getDefault());
            }
        }

        @TruffleBoundary
        public void resolveLocaleAndNumberingSystem(JSContext ctx, String[] locales, String numberingSystemOpt) {
            Locale selectedLocale = IntlUtil.selectedLocale(ctx, locales);
            Locale strippedLocale = selectedLocale.stripExtensions();
            if (strippedLocale.toLanguageTag().equals(IntlUtil.UND)) {
                selectedLocale = ctx.getLocale();
                strippedLocale = selectedLocale.stripExtensions();
            }
            Locale.Builder builder = new Locale.Builder();
            builder.setLocale(strippedLocale);

            String nuType = selectedLocale.getUnicodeLocaleType("nu");
            if ((nuType != null) && IntlUtil.isValidNumberingSystem(nuType) && (numberingSystemOpt == null || numberingSystemOpt.equals(nuType))) {
                this.numberingSystem = nuType;
                builder.setUnicodeLocaleKeyword("nu", nuType);
            }

            this.locale = builder.build().toLanguageTag();

            if (numberingSystemOpt != null && IntlUtil.isValidNumberingSystem(numberingSystemOpt)) {
                this.numberingSystem = numberingSystemOpt;
                builder.setUnicodeLocaleKeyword("nu", numberingSystemOpt);
            }

            this.javaLocale = builder.build();

            if (this.numberingSystem == null) {
                this.numberingSystem = IntlUtil.defaultNumberingSystemName(ctx, this.javaLocale);
            }
        }

        @TruffleBoundary
        public void initializeNumberFormatter() {
            UnlocalizedNumberFormatter formatter = NumberFormatter.with();

            formatter = formatter.roundingMode(RoundingMode.HALF_UP);
            formatter = formatter.symbols(NumberingSystem.getInstanceByName(numberingSystem));
            formatter = formatter.integerWidth(IntegerWidth.zeroFillTo(minimumIntegerDigits));

            if (IntlUtil.SIGNIFICANT_DIGITS.equals(roundingType)) {
                precision = Precision.minMaxSignificantDigits(minimumSignificantDigits, maximumSignificantDigits);
            } else {
                FractionPrecision fractionPrecision = Precision.minMaxFraction(minimumFractionDigits, maximumFractionDigits);
                if (IntlUtil.FRACTION_DIGITS.equals(roundingType)) {
                    precision = fractionPrecision;
                } else {
                    boolean morePrecision = IntlUtil.MORE_PRECISION.equals(roundingType);
                    assert morePrecision || IntlUtil.LESS_PRECISION.equals(roundingType);
                    precision = fractionPrecision.withSignificantDigits(minimumSignificantDigits, maximumSignificantDigits,
                                    morePrecision ? NumberFormatter.RoundingPriority.RELAXED : NumberFormatter.RoundingPriority.STRICT);
                }
            }
            formatter = formatter.precision(precision);

            this.unlocalizedFormatter = formatter;
        }

        public UnlocalizedNumberFormatter getUnlocalizedFormatter() {
            return unlocalizedFormatter;
        }

        public Precision getPrecision() {
            return precision;
        }

        public Locale getJavaLocale() {
            return javaLocale;
        }

        public String getLocale() {
            return locale;
        }

        public String getNumberingSystem() {
            return numberingSystem;
        }

        public void setMinimumIntegerDigits(int minimumIntegerDigits) {
            this.minimumIntegerDigits = minimumIntegerDigits;
        }

        public int getMinimumIntegerDigits() {
            return minimumIntegerDigits;
        }

        public void setMinimumFractionDigits(int minimumFractionDigits) {
            this.minimumFractionDigits = minimumFractionDigits;
        }

        public Integer getMinimumFractionDigits() {
            return minimumFractionDigits;
        }

        public void setMaximumFractionDigits(int maximumFractionDigits) {
            this.maximumFractionDigits = maximumFractionDigits;
        }

        public Integer getMaximumFractionDigits() {
            return maximumFractionDigits;
        }

        public void setMinimumSignificantDigits(int minimumSignificantDigits) {
            this.minimumSignificantDigits = minimumSignificantDigits;
        }

        public Integer getMinimumSignificantDigits() {
            return minimumSignificantDigits;
        }

        public void setMaximumSignificantDigits(int maximumSignificantDigits) {
            this.maximumSignificantDigits = maximumSignificantDigits;
        }

        public Integer getMaximumSignificantDigits() {
            return maximumSignificantDigits;
        }

        public void setRoundingType(String roundingType) {
            this.roundingType = roundingType;
        }

        public String getRoundingType() {
            return roundingType;
        }
    }

    public static class InternalState extends BasicInternalState {
        private LocalizedNumberFormatter positiveNumberFormatter;
        private LocalizedNumberFormatter negativeNumberFormatter;
        private LocalizedNumberRangeFormatter numberRangeFormatter;

        private String style;
        private String currency;
        private String currencyDisplay;
        private String currencySign;
        private String unit;
        private String unitDisplay;
        private Object useGrouping;
        private String notation;
        private String compactDisplay;
        private String signDisplay;
        private String roundingMode;
        private int roundingIncrement;
        private String trailingZeroDisplay;

        DynamicObject boundFormatFunction;

        @Override
        void fillResolvedOptions(JSContext context, JSRealm realm, DynamicObject result) {
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.LOCALE, getLocale(), JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.NUMBERING_SYSTEM, getNumberingSystem(), JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.STYLE, style, JSAttributes.getDefault());
            if (currency != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.CURRENCY, currency, JSAttributes.getDefault());
            }
            if (currencyDisplay != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.CURRENCY_DISPLAY, currencyDisplay, JSAttributes.getDefault());
            }
            if (currencySign != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.CURRENCY_SIGN, currencySign, JSAttributes.getDefault());
            }
            if (unit != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.UNIT, unit, JSAttributes.getDefault());
            }
            if (unitDisplay != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.UNIT_DISPLAY, unitDisplay, JSAttributes.getDefault());
            }
            super.fillResolvedOptions(context, realm, result);
            Object resolvedUseGrouping = useGrouping;
            if (useGrouping instanceof String && context.getEcmaScriptVersion() < JSConfig.StagingECMAScriptVersion) {
                resolvedUseGrouping = true;
            }
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.USE_GROUPING, resolvedUseGrouping, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.NOTATION, notation, JSAttributes.getDefault());
            if (compactDisplay != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.COMPACT_DISPLAY, compactDisplay, JSAttributes.getDefault());
            }
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.SIGN_DISPLAY, signDisplay, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.ROUNDING_MODE, roundingMode, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.ROUNDING_INCREMENT, roundingIncrement, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.TRAILING_ZERO_DISPLAY, trailingZeroDisplay, JSAttributes.getDefault());

            String roundingType = getRoundingType();
            String resolvedRoundingType = (IntlUtil.MORE_PRECISION.equals(roundingType) || IntlUtil.LESS_PRECISION.equals(roundingType)) ? roundingType : IntlUtil.AUTO;
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.ROUNDING_PRIORITY, resolvedRoundingType, JSAttributes.getDefault());
        }

        @TruffleBoundary
        @Override
        public void initializeNumberFormatter() {
            super.initializeNumberFormatter();

            UnlocalizedNumberFormatter formatter = getUnlocalizedFormatter();

            formatter = formatter.notation(notationToICUNotation(notation, compactDisplay));
            formatter = formatter.grouping(useGroupingToGroupingStrategy(useGrouping));

            if (IntlUtil.CURRENCY.equals(style)) {
                formatter = formatter.unit(com.ibm.icu.util.Currency.getInstance(currency));
                formatter = formatter.unitWidth(currencyDisplayToUnitWidth(currencyDisplay));
            } else if (IntlUtil.PERCENT.equals(style)) {
                formatter = formatter.unit(MeasureUnit.PERCENT);
                formatter = formatter.scale(Scale.powerOfTen(2));
            } else if (IntlUtil.UNIT.equals(style)) {
                String per = "-per-";
                int index = unit.indexOf(per);
                if (index == -1) {
                    formatter = formatter.unit(unitToMeasureUnit(unit));
                } else {
                    String numerator = unit.substring(0, index);
                    String denominator = unit.substring(index + per.length());
                    formatter = formatter.unit(unitToMeasureUnit(numerator));
                    formatter = formatter.perUnit(unitToMeasureUnit(denominator));
                }
                formatter = formatter.unitWidth(unitDisplayToUnitWidth(unitDisplay));
            }

            formatter = formatter.sign(signDisplay(signDisplay, IntlUtil.ACCOUNTING.equals(currencySign)));

            formatter = formatter.roundingMode(roundingModeToICURoundingMode(roundingMode));

            Precision precision = getPrecision();
            if (roundingIncrement != 1) {
                // Note that minimumFractionDigits digits are ignored here.
                // ICU4J does not support the combination of increment and minimumFractionDigits
                BigDecimal increment = BigDecimal.ONE.movePointLeft(getMaximumFractionDigits()).multiply(BigDecimal.valueOf(roundingIncrement));
                precision = Precision.increment(increment);
            }
            if (IntlUtil.STRIP_IF_INTEGER.equals(trailingZeroDisplay)) {
                precision = precision.trailingZeroDisplay(NumberFormatter.TrailingZeroDisplay.HIDE_IF_WHOLE);
            }
            formatter = formatter.precision(precision);

            // ICU4J does not support HALF_CEIL and HALF_FLOOR directly =>
            // we map them to HALF_DOWN/HALF_UP as a workaround (separately
            // for positive and negative numbers).
            this.positiveNumberFormatter = formatter.locale(getJavaLocale());
            if (IntlUtil.HALF_CEIL.equals(roundingMode)) {
                negativeNumberFormatter = positiveNumberFormatter.roundingMode(RoundingMode.HALF_DOWN);
            } else if (IntlUtil.HALF_FLOOR.equals(roundingMode)) {
                negativeNumberFormatter = positiveNumberFormatter.roundingMode(RoundingMode.HALF_UP);
            } else {
                negativeNumberFormatter = positiveNumberFormatter;
            }

            this.numberRangeFormatter = NumberRangeFormatter.withLocale(getJavaLocale()).numberFormatterBoth(formatter);
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public void setCurrencyDisplay(String currencyDisplay) {
            this.currencyDisplay = currencyDisplay;
        }

        public void setCurrencySign(String currencySign) {
            this.currencySign = currencySign;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public void setUnitDisplay(String unitDisplay) {
            this.unitDisplay = unitDisplay;
        }

        public void setGroupingUsed(Object useGrouping) {
            this.useGrouping = useGrouping;
        }

        public void setNotation(String notation) {
            this.notation = notation;
        }

        public void setCompactDisplay(String compactDisplay) {
            this.compactDisplay = compactDisplay;
        }

        public void setSignDisplay(String signDisplay) {
            this.signDisplay = signDisplay;
        }

        public void setRoundingMode(String roundingMode) {
            this.roundingMode = roundingMode;
        }

        public void setRoundingIncrement(int roundingIncrement) {
            this.roundingIncrement = roundingIncrement;
        }

        public void setTrailingZeroDisplay(String trailingZeroDisplay) {
            this.trailingZeroDisplay = trailingZeroDisplay;
        }

        public LocalizedNumberFormatter getNumberFormatter(boolean forNegativeNumbers) {
            return forNegativeNumbers ? negativeNumberFormatter : positiveNumberFormatter;
        }

        public LocalizedNumberRangeFormatter getNumberRangeFormatter() {
            return numberRangeFormatter;
        }
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, JSRealm realm, DynamicObject numberFormatObj) {
        InternalState state = getInternalState(numberFormatObj);
        return state.toResolvedOptionsObject(context, realm);
    }

    public static InternalState getInternalState(DynamicObject obj) {
        assert isJSNumberFormat(obj);
        return ((JSNumberFormatObject) obj).getInternalState();
    }

    private static CallTarget createGetFormatCallTarget(JSContext context) {
        return new JavaScriptRootNode(context.getLanguage(), null, null) {
            private final BranchProfile errorBranch = BranchProfile.create();
            @Child private PropertySetNode setBoundObjectNode = PropertySetNode.createSetHidden(BOUND_OBJECT_KEY, context);

            @Override
            public Object execute(VirtualFrame frame) {

                Object[] frameArgs = frame.getArguments();
                Object numberFormatObj = JSArguments.getThisObject(frameArgs);

                if (isJSNumberFormat(numberFormatObj)) {

                    InternalState state = getInternalState((DynamicObject) numberFormatObj);

                    if (state == null) {
                        errorBranch.enter();
                        throw Errors.createTypeErrorMethodCalledOnNonObjectOrWrongType("format");
                    }

                    if (state.boundFormatFunction == null) {
                        JSFunctionData formatFunctionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.NumberFormatFormat, c -> createFormatFunctionData(c));
                        DynamicObject formatFn = JSFunction.create(getRealm(), formatFunctionData);
                        setBoundObjectNode.setValue(formatFn, numberFormatObj);
                        state.boundFormatFunction = formatFn;
                    }

                    return state.boundFormatFunction;
                }
                errorBranch.enter();
                throw Errors.createTypeErrorTypeXExpected(CLASS_NAME);
            }
        }.getCallTarget();
    }

    private static JSFunctionData createFormatFunctionData(JSContext context) {
        return JSFunctionData.createCallOnly(context, new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private PropertyGetNode getBoundObjectNode = PropertyGetNode.createGetHidden(BOUND_OBJECT_KEY, context);
            @Child private ToIntlMathematicalValue toIntlMVValueNode = ToIntlMathematicalValue.create(false);

            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                DynamicObject thisObj = (DynamicObject) getBoundObjectNode.getValue(JSArguments.getFunctionObject(arguments));
                assert isJSNumberFormat(thisObj);
                Object n = JSArguments.getUserArgumentCount(arguments) > 0 ? JSArguments.getUserArgument(arguments, 0) : Undefined.instance;
                return formatMV(thisObj, toIntlMVValueNode.executeNumber(n));
            }
        }.getCallTarget(), 1, "");
    }

    private static DynamicObject createFormatFunctionGetter(JSRealm realm, JSContext context) {
        JSFunctionData fd = realm.getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.NumberFormatGetFormat, (c) -> {
            CallTarget ct = createGetFormatCallTarget(context);
            return JSFunctionData.create(context, ct, ct, 0, "get format", false, false, false, true);
        });
        return JSFunction.create(realm, fd);
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getNumberFormatPrototype();
    }
}
