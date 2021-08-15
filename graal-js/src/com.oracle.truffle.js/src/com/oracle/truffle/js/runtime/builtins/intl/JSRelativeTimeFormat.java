/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.UnmodifiableEconomicMap;

import com.ibm.icu.text.DisplayContext;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RelativeDateTimeFormatter;
import com.ibm.icu.text.RelativeDateTimeFormatter.RelativeDateTimeUnit;
import com.ibm.icu.util.ULocale;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.intl.RelativeTimeFormatFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.RelativeTimeFormatPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.LazyValue;

public final class JSRelativeTimeFormat extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String CLASS_NAME = "RelativeTimeFormat";
    public static final String PROTOTYPE_NAME = "RelativeTimeFormat.prototype";

    public static final JSRelativeTimeFormat INSTANCE = new JSRelativeTimeFormat();

    private JSRelativeTimeFormat() {
    }

    public static boolean isJSRelativeTimeFormat(Object obj) {
        return obj instanceof JSRelativeTimeFormatObject;
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
        DynamicObject relativeTimeFormatPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, relativeTimeFormatPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, relativeTimeFormatPrototype, RelativeTimeFormatPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(relativeTimeFormatPrototype, "Intl.RelativeTimeFormat");
        return relativeTimeFormatPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, RelativeTimeFormatFunctionBuiltins.BUILTINS);
    }

    public static DynamicObject create(JSContext context, JSRealm realm) {
        InternalState state = new InternalState();
        JSObjectFactory factory = context.getRelativeTimeFormatFactory();
        JSRelativeTimeFormatObject obj = new JSRelativeTimeFormatObject(factory.getShape(realm), state);
        factory.initProto(obj, realm);
        assert isJSRelativeTimeFormat(obj);
        return context.trackAllocation(obj);
    }

    public static RelativeDateTimeFormatter getRelativeDateTimeFormatterProperty(DynamicObject obj) {
        return getInternalState(obj).getRelativeDateTimeFormatter();
    }

    private static void ensureFiniteNumber(double d) {
        if (!Double.isFinite(d)) {
            throw Errors.createRangeError("Value need to be finite number for Intl.RelativeTimeFormat operation");
        }
    }

    @TruffleBoundary
    public static String format(DynamicObject relativeTimeFormatObj, double amount, String unit) {
        ensureFiniteNumber(amount);
        InternalState state = getInternalState(relativeTimeFormatObj);
        RelativeDateTimeUnit icuUnit = singularRelativeTimeUnit("format", unit);
        return innerFormat(amount, state, state.getRelativeDateTimeFormatter(), icuUnit);
    }

    private static String innerFormat(double amount, InternalState state, RelativeDateTimeFormatter relativeDateTimeFormatter, RelativeDateTimeUnit icuUnit) {
        if (state.getNumeric().equals("always")) {
            return relativeDateTimeFormatter.formatNumeric(amount, icuUnit);
        } else {
            return relativeDateTimeFormatter.format(amount, icuUnit);
        }
    }

    @TruffleBoundary
    public static DynamicObject formatToParts(JSContext context, JSRealm realm, DynamicObject relativeTimeFormatObj, double amount, String unit) {
        ensureFiniteNumber(amount);
        InternalState state = getInternalState(relativeTimeFormatObj);
        RelativeDateTimeFormatter relativeDateTimeFormatter = state.getRelativeDateTimeFormatter();
        NumberFormat numberFormat = relativeDateTimeFormatter.getNumberFormat();
        RelativeDateTimeUnit icuUnit = singularRelativeTimeUnit("formatToParts", unit);
        String formattedText = innerFormat(amount, state, relativeDateTimeFormatter, icuUnit);
        double positiveAmount = Math.abs(amount);
        String formattedNumber = numberFormat.format(positiveAmount);
        int numberIndex = formattedText.indexOf(formattedNumber);
        boolean numberPresentInFormattedText = numberIndex > -1;

        List<Object> resultParts = new ArrayList<>();
        if (numberPresentInFormattedText) {

            if (numberIndex > 0) {
                resultParts.add(IntlUtil.makePart(context, realm, "literal", formattedText.substring(0, numberIndex)));
            }

            String esUnit = icuUnit.toString().toLowerCase();
            AttributedCharacterIterator iterator = numberFormat.formatToCharacterIterator(positiveAmount);
            String formatted = numberFormat.format(positiveAmount);
            resultParts.addAll(JSNumberFormat.innerFormatToParts(context, realm, iterator, positiveAmount, formatted, esUnit, false));

            if (numberIndex + formattedNumber.length() < formattedText.length()) {
                resultParts.add(IntlUtil.makePart(context, realm, "literal", formattedText.substring(numberIndex + formattedNumber.length(), formattedText.length())));
            }
        } else {
            resultParts.add(IntlUtil.makePart(context, realm, "literal", formattedText));
        }
        return JSArray.createConstant(context, realm, resultParts.toArray());
    }

    public static class InternalState extends JSNumberFormat.BasicInternalState {

        private RelativeDateTimeFormatter relativeDateTimeFormatter;

        private String style;
        private String numeric;

        @Override
        DynamicObject toResolvedOptionsObject(JSContext context, JSRealm realm) {
            DynamicObject result = JSOrdinary.create(context, realm);
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.LOCALE, getLocale(), JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.STYLE, style, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.NUMERIC, numeric, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.NUMBERING_SYSTEM, getNumberingSystem(), JSAttributes.getDefault());
            return result;
        }

        @TruffleBoundary
        public void initializeRelativeTimeFormatter() {
            relativeDateTimeFormatter = createFormatter(getJavaLocale(), style);
        }

        public RelativeDateTimeFormatter getRelativeDateTimeFormatter() {
            return relativeDateTimeFormatter;
        }

        public void setStyle(String style) {
            this.style = style;
        }

        public void setNumeric(String numeric) {
            this.numeric = numeric;
        }

        public String getNumeric() {
            return numeric;
        }
    }

    private static RelativeDateTimeFormatter createFormatter(Locale locale, String style) {
        ULocale ulocale = ULocale.forLocale(locale);
        return RelativeDateTimeFormatter.getInstance(ulocale, null,
                        RelativeDateTimeFormatter.Style.valueOf(style.toUpperCase()), DisplayContext.CAPITALIZATION_NONE);
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, JSRealm realm, DynamicObject relativeTimeFormatObj) {
        InternalState state = getInternalState(relativeTimeFormatObj);
        return state.toResolvedOptionsObject(context, realm);
    }

    public static InternalState getInternalState(DynamicObject obj) {
        assert isJSRelativeTimeFormat(obj);
        return ((JSRelativeTimeFormatObject) obj).getInternalState();
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getRelativeTimeFormatPrototype();
    }

    private static RelativeDateTimeFormatter.RelativeDateTimeUnit toRelTimeUnit(String unit) {
        return timeUnitMap.get().get(unit);
    }

    private static final LazyValue<UnmodifiableEconomicMap<String, RelativeDateTimeFormatter.RelativeDateTimeUnit>> timeUnitMap = new LazyValue<>(JSRelativeTimeFormat::initTimeUnitMap);

    private static UnmodifiableEconomicMap<String, RelativeDateTimeFormatter.RelativeDateTimeUnit> initTimeUnitMap() {
        CompilerAsserts.neverPartOfCompilation();
        EconomicMap<String, RelativeDateTimeUnit> map = EconomicMap.create(16);
        map.put("second", RelativeDateTimeUnit.SECOND);
        map.put("seconds", RelativeDateTimeUnit.SECOND);
        map.put("minute", RelativeDateTimeUnit.MINUTE);
        map.put("minutes", RelativeDateTimeUnit.MINUTE);
        map.put("hour", RelativeDateTimeUnit.HOUR);
        map.put("hours", RelativeDateTimeUnit.HOUR);
        map.put("day", RelativeDateTimeUnit.DAY);
        map.put("days", RelativeDateTimeUnit.DAY);
        map.put("week", RelativeDateTimeUnit.WEEK);
        map.put("weeks", RelativeDateTimeUnit.WEEK);
        map.put("month", RelativeDateTimeUnit.MONTH);
        map.put("months", RelativeDateTimeUnit.MONTH);
        map.put("quarter", RelativeDateTimeUnit.QUARTER);
        map.put("quarters", RelativeDateTimeUnit.QUARTER);
        map.put("year", RelativeDateTimeUnit.YEAR);
        map.put("years", RelativeDateTimeUnit.YEAR);
        return map;
    }

    private static RelativeDateTimeUnit singularRelativeTimeUnit(String functionName, String unit) {
        RelativeDateTimeUnit result = toRelTimeUnit(unit);
        if (result != null) {
            return result;
        } else {
            throw Errors.createRangeErrorInvalidUnitArgument(functionName, unit);
        }
    }
}
