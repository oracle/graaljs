/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.ibm.icu.text.DecimalFormat;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import java.text.AttributedCharacterIterator;

import com.ibm.icu.text.NumberFormat;

import java.util.Arrays;
import java.util.Currency;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class JSNumberFormat extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctions {

    public static final String CLASS_NAME = "NumberFormat";
    public static final String PROTOTYPE_NAME = "NumberFormat.prototype";

    private static final HiddenKey INTERNAL_STATE_ID = new HiddenKey("_internalState");
    private static final Property INTERNAL_STATE_PROPERTY;

    private static final JSNumberFormat INSTANCE = new JSNumberFormat();

    public static final List<String> BCP47_CU_KEYS = Arrays.asList(new String[]{
                    "ADP",
                    "AED",
                    "AFA",
                    "AFN",
                    "ALK",
                    "ALL",
                    "AMD",
                    "ANG",
                    "AOA",
                    "AOK",
                    "AON",
                    "AOR",
                    "ARA",
                    "ARL",
                    "ARM",
                    "ARP",
                    "ARS",
                    "ATS",
                    "AUD",
                    "AWG",
                    "AZM",
                    "AZN",
                    "BAD",
                    "BAM",
                    "BAN",
                    "BBD",
                    "BDT",
                    "BEC",
                    "BEF",
                    "BEL",
                    "BGL",
                    "BGM",
                    "BGN",
                    "BGO",
                    "BHD",
                    "BIF",
                    "BMD",
                    "BND",
                    "BOB",
                    "BOL",
                    "BOP",
                    "BOV",
                    "BRB",
                    "BRC",
                    "BRE",
                    "BRL",
                    "BRN",
                    "BRR",
                    "BRZ",
                    "BSD",
                    "BTN",
                    "BUK",
                    "BWP",
                    "BYB",
                    "BYN",
                    "BYR",
                    "BZD",
                    "CAD",
                    "CDF",
                    "CHE",
                    "CHF",
                    "CHW",
                    "CLE",
                    "CLF",
                    "CLP",
                    "CNH",
                    "CNX",
                    "CNY",
                    "COP",
                    "COU",
                    "CRC",
                    "CSD",
                    "CSK",
                    "CUC",
                    "CUP",
                    "CVE",
                    "CYP",
                    "CZK",
                    "DDM",
                    "DEM",
                    "DJF",
                    "DKK",
                    "DOP",
                    "DZD",
                    "ECS",
                    "ECV",
                    "EEK",
                    "EGP",
                    "ERN",
                    "ESA",
                    "ESB",
                    "ESP",
                    "ETB",
                    "EUR",
                    "FIM",
                    "FJD",
                    "FKP",
                    "FRF",
                    "GBP",
                    "GEK",
                    "GEL",
                    "GHC",
                    "GHS",
                    "GIP",
                    "GMD",
                    "GNF",
                    "GNS",
                    "GQE",
                    "GRD",
                    "GTQ",
                    "GWE",
                    "GWP",
                    "GYD",
                    "HKD",
                    "HNL",
                    "HRD",
                    "HRK",
                    "HTG",
                    "HUF",
                    "IDR",
                    "IEP",
                    "ILP",
                    "ILR",
                    "ILS",
                    "INR",
                    "IQD",
                    "IRR",
                    "ISJ",
                    "ISK",
                    "ITL",
                    "JMD",
                    "JOD",
                    "JPY",
                    "KES",
                    "KGS",
                    "KHR",
                    "KMF",
                    "KPW",
                    "KRH",
                    "KRO",
                    "KRW",
                    "KWD",
                    "KYD",
                    "KZT",
                    "LAK",
                    "LBP",
                    "LKR",
                    "LRD",
                    "LSL",
                    "LTL",
                    "LTT",
                    "LUC",
                    "LUF",
                    "LUL",
                    "LVL",
                    "LVR",
                    "LYD",
                    "MAD",
                    "MAF",
                    "MCF",
                    "MDC",
                    "MDL",
                    "MGA",
                    "MGF",
                    "MKD",
                    "MKN",
                    "MLF",
                    "MMK",
                    "MNT",
                    "MOP",
                    "MRO",
                    "MTL",
                    "MTP",
                    "MUR",
                    "MVP",
                    "MVR",
                    "MWK",
                    "MXN",
                    "MXP",
                    "MXV",
                    "MYR",
                    "MZE",
                    "MZM",
                    "MZN",
                    "NAD",
                    "NGN",
                    "NIC",
                    "NIO",
                    "NLG",
                    "NOK",
                    "NPR",
                    "NZD",
                    "OMR",
                    "PAB",
                    "PEI",
                    "PEN",
                    "PES",
                    "PGK",
                    "PHP",
                    "PKR",
                    "PLN",
                    "PLZ",
                    "PTE",
                    "PYG",
                    "QAR",
                    "RHD",
                    "ROL",
                    "RON",
                    "RSD",
                    "RUB",
                    "RUR",
                    "RWF",
                    "SAR",
                    "SBD",
                    "SCR",
                    "SDD",
                    "SDG",
                    "SDP",
                    "SEK",
                    "SGD",
                    "SHP",
                    "SIT",
                    "SKK",
                    "SLL",
                    "SOS",
                    "SRD",
                    "SRG",
                    "SSP",
                    "STD",
                    "STN",
                    "SUR",
                    "SVC",
                    "SYP",
                    "SZL",
                    "THB",
                    "TJR",
                    "TJS",
                    "TMM",
                    "TMT",
                    "TND",
                    "TOP",
                    "TPE",
                    "TRL",
                    "TRY",
                    "TTD",
                    "TWD",
                    "TZS",
                    "UAH",
                    "UAK",
                    "UGS",
                    "UGX",
                    "USD",
                    "USN",
                    "USS",
                    "UYI",
                    "UYP",
                    "UYU",
                    "UZS",
                    "VEB",
                    "VEF",
                    "VND",
                    "VNN",
                    "VUV",
                    "WST",
                    "XAF",
                    "XAG",
                    "XAU",
                    "XBA",
                    "XBB",
                    "XBC",
                    "XBD",
                    "XCD",
                    "XDR",
                    "XEU",
                    "XFO",
                    "XFU",
                    "XOF",
                    "XPD",
                    "XPF",
                    "XPT",
                    "XRE",
                    "XSU",
                    "XTS",
                    "XUA",
                    "XXX",
                    "YDD",
                    "YER",
                    "YUD",
                    "YUM",
                    "YUN",
                    "YUR",
                    "ZAL",
                    "ZAR",
                    "ZMK",
                    "ZMW",
                    "ZRN",
                    "ZRZ",
                    "ZWD",
                    "ZWL",
                    "ZWR"
    });

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        INTERNAL_STATE_PROPERTY = JSObjectUtil.makeHiddenProperty(INTERNAL_STATE_ID, allocator.locationForType(InternalState.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)));
    }

    private JSNumberFormat() {
    }

    public static boolean isSupportedCurrencyKey(String cuKey) {
        return BCP47_CU_KEYS.contains(cuKey);
    }

    public static boolean isJSNumberFormat(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSNumberFormat((DynamicObject) obj);
    }

    public static boolean isJSNumberFormat(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
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
    public String getBuiltinToStringTag(DynamicObject object) {
        return "Object";
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject numberFormatPrototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, numberFormatPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, numberFormatPrototype, PROTOTYPE_NAME);
        JSObjectUtil.putConstantAccessorProperty(ctx, numberFormatPrototype, "format", createFormatFunctionGetter(realm, ctx), Undefined.instance);
        return numberFormatPrototype;
    }

    // https://tc39.github.io/ecma402/#sec-currencydigits
    @TruffleBoundary
    public static int currencyDigits(String currencyCode) {
        if (currencyCode == null) {
            return 2;
        }
        try {
            Currency currency = Currency.getInstance(currencyCode);
            return (currency != null) ? currency.getDefaultFractionDigits() : 2;
        } catch (IllegalArgumentException e) {
            return 2;
        }
    }

    // https://tc39.github.io/ecma402/#sec-iswellformedcurrencycode
    @TruffleBoundary
    public static boolean isWellFormedCurrencyCode(String currency) {
        if (currency == null || currency.length() != 3) {
            return false;
        }
        String normalized = IntlUtil.toUpperCase(currency);
        char a = normalized.charAt(0);
        char b = normalized.charAt(1);
        char c = normalized.charAt(2);
        return a >= 'A' && a <= 'Z' && b >= 'A' && b <= 'Z' && c >= 'A' && c <= 'Z';
    }

    public static Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        assert JSShape.getProtoChildTree(prototype.getShape(), INSTANCE) == null;
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        initialShape = initialShape.addProperty(INTERNAL_STATE_PROPERTY);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static DynamicObject create(JSContext context) {
        InternalState state = new InternalState();
        DynamicObject result = JSObject.create(context, context.getNumberFormatFactory(), state);
        assert isJSNumberFormat(result);
        return result;
    }

    @TruffleBoundary
    public static void setLocaleAndNumberingSystem(BasicInternalState state, String[] locales) {
        String selectedTag = IntlUtil.selectedLocale(locales);
        Locale selectedLocale = selectedTag != null ? Locale.forLanguageTag(selectedTag) : Locale.getDefault();
        Locale strippedLocale = selectedLocale.stripExtensions();
        if (selectedLocale.getUnicodeLocaleKeys().contains("nu")) {
            String unicodeLocaleType = selectedLocale.getUnicodeLocaleType("nu");
            if (IntlUtil.isSupportedNumberSystemKey(unicodeLocaleType)) {
                state.numberingSystem = unicodeLocaleType;
            } else {
                selectedLocale = IntlUtil.withoutUnicodeExtension(selectedLocale, "nu");
            }
        }
        state.locale = strippedLocale.toLanguageTag();
        // see https://tc39.github.io/ecma402/#sec-intl.numberformat-internal-slots (NOTE 1)
        state.javaLocale = IntlUtil.withoutUnicodeExtension(selectedLocale, "cu");
    }

    @TruffleBoundary
    public static void setupInternalNumberFormat(InternalState state) {
        if (state.style.equals("currency")) {
            state.numberFormat = NumberFormat.getCurrencyInstance(state.javaLocale);
        } else if (state.style.equals("percent")) {
            state.numberFormat = NumberFormat.getPercentInstance(state.javaLocale);
        } else {
            state.numberFormat = NumberFormat.getInstance(state.javaLocale);
        }
        state.numberFormat.setGroupingUsed(state.useGrouping);
    }

    @TruffleBoundary
    public static void setSignificantDigits(BasicInternalState state) {
        if (state.numberFormat instanceof DecimalFormat) {
            DecimalFormat df = (DecimalFormat) state.numberFormat;
            df.setMinimumSignificantDigits(state.minimumSignificantDigits.intValue());
            df.setMaximumSignificantDigits(state.maximumSignificantDigits.intValue());
        }
    }

    public static NumberFormat getNumberFormatProperty(DynamicObject obj) {
        return getInternalState(obj).numberFormat;
    }

    @TruffleBoundary
    public static String format(DynamicObject numberFormatObj, Object n) {
        NumberFormat numberFormat = getNumberFormatProperty(numberFormatObj);
        Number x = toInternalNumberRepresentation(JSRuntime.toNumeric(n));
        return numberFormat.format(x);
    }

    static final Map<NumberFormat.Field, String> fieldToType = new HashMap<>();

    static {
        fieldToType.put(NumberFormat.Field.INTEGER, "integer");
        fieldToType.put(NumberFormat.Field.DECIMAL_SEPARATOR, "decimal");
        fieldToType.put(NumberFormat.Field.FRACTION, "fraction");
        fieldToType.put(NumberFormat.Field.GROUPING_SEPARATOR, "group");
        fieldToType.put(NumberFormat.Field.CURRENCY, "currency");
    }

    @TruffleBoundary
    public static DynamicObject formatToParts(JSContext context, DynamicObject numberFormatObj, Object n) {

        ensureIsNumberFormat(numberFormatObj);
        NumberFormat numberFormat = getNumberFormatProperty(numberFormatObj);
        Number x = toInternalNumberRepresentation(JSRuntime.toNumeric(n));

        List<Object> resultParts = new LinkedList<>();
        AttributedCharacterIterator fit = numberFormat.formatToCharacterIterator(x);
        String formatted = numberFormat.format(x);
        int i = fit.getBeginIndex();
        while (i < fit.getEndIndex()) {
            fit.setIndex(i);
            Map<AttributedCharacterIterator.Attribute, Object> attributes = fit.getAttributes();
            Set<AttributedCharacterIterator.Attribute> attKeySet = attributes.keySet();
            if (!attKeySet.isEmpty()) {
                for (AttributedCharacterIterator.Attribute a : attKeySet) {
                    if (a instanceof NumberFormat.Field) {
                        String value = formatted.substring(fit.getRunStart(), fit.getRunLimit());
                        String type = fieldToType.get(a);
                        resultParts.add(makePart(context, type, value));
                        i = fit.getRunLimit();
                        break;
                    } else {
                        throw Errors.shouldNotReachHere();
                    }
                }
            } else {
                String value = formatted.substring(fit.getRunStart(), fit.getRunLimit());
                resultParts.add(makePart(context, "literal", value));
                i = fit.getRunLimit();
            }
        }
        return JSArray.createConstant(context, resultParts.toArray());
    }

    private static Number toInternalNumberRepresentation(Object o) {
        if (o instanceof LargeInteger) {
            return ((LargeInteger) o).doubleValue();
        } else if (o instanceof Number) {
            return (Number) o;
        } else if (o instanceof BigInt) {
            return ((BigInt) o).bigIntegerValue();
        } else {
            throw Errors.shouldNotReachHere();
        }
    }

    private static Object makePart(JSContext context, String type, String value) {
        DynamicObject p = JSUserObject.create(context);
        JSObject.set(p, "type", type);
        JSObject.set(p, "value", value);
        return p;
    }

    public static class BasicInternalState {

        public boolean initialized = false;

        public NumberFormat numberFormat;

        public Locale javaLocale;
        public String locale;

        public String numberingSystem = "latn";

        public Number minimumIntegerDigits = 1;
        public Number minimumFractionDigits = 0;
        public Number maximumFractionDigits = 3;
        public Number minimumSignificantDigits;
        public Number maximumSignificantDigits;

        DynamicObject toResolvedOptionsObject(JSContext context) {

            DynamicObject result = JSUserObject.create(context);
            JSObjectUtil.defineDataProperty(result, "locale", locale, JSAttributes.getDefault());
            if (minimumIntegerDigits != null) {
                JSObjectUtil.defineDataProperty(result, "minimumIntegerDigits", minimumIntegerDigits, JSAttributes.getDefault());
            }
            if (minimumFractionDigits != null) {
                JSObjectUtil.defineDataProperty(result, "minimumFractionDigits", minimumFractionDigits, JSAttributes.getDefault());
            }
            if (maximumFractionDigits != null) {
                JSObjectUtil.defineDataProperty(result, "maximumFractionDigits", maximumFractionDigits, JSAttributes.getDefault());
            }
            if (minimumSignificantDigits != null) {
                JSObjectUtil.defineDataProperty(result, "minimumSignificantDigits", minimumSignificantDigits, JSAttributes.getDefault());
            }
            if (maximumSignificantDigits != null) {
                JSObjectUtil.defineDataProperty(result, "maximumSignificantDigits", maximumSignificantDigits, JSAttributes.getDefault());
            }
            return result;
        }
    }

    public static class InternalState extends BasicInternalState {

        public String style = "decimal";
        public String currency;
        public String currencyDisplay;
        public boolean useGrouping = true;

        DynamicObject boundFormatFunction = null;

        @Override
        DynamicObject toResolvedOptionsObject(JSContext context) {

            DynamicObject result = super.toResolvedOptionsObject(context);

            JSObjectUtil.defineDataProperty(result, "numberingSystem", numberingSystem, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "style", style, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "useGrouping", useGrouping, JSAttributes.getDefault());
            if (currency != null) {
                JSObjectUtil.defineDataProperty(result, "currency", currency, JSAttributes.getDefault());
            }
            if (currencyDisplay != null) {
                JSObjectUtil.defineDataProperty(result, "currencyDisplay", currencyDisplay, JSAttributes.getDefault());
            }
            return result;
        }
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, DynamicObject numberFormatObj) {
        ensureIsNumberFormat(numberFormatObj);
        InternalState state = getInternalState(numberFormatObj);
        return state.toResolvedOptionsObject(context);
    }

    public static InternalState getInternalState(DynamicObject numberFormatObj) {
        return (InternalState) INTERNAL_STATE_PROPERTY.get(numberFormatObj, isJSNumberFormat(numberFormatObj));
    }

    private static CallTarget createGetFormatCallTarget(JSRealm realm, JSContext context) {
        return Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {

            @Override
            public Object execute(VirtualFrame frame) {

                Object[] frameArgs = frame.getArguments();
                Object numberFormatObj = JSArguments.getThisObject(frameArgs);

                if (isJSNumberFormat(numberFormatObj)) {

                    InternalState state = getInternalState((DynamicObject) numberFormatObj);

                    if (state == null || !state.initialized) {
                        throw Errors.createTypeError("Method format called on a non-object or on a wrong type of object (uninitialized NumberFormat?).");
                    }

                    if (state.boundFormatFunction == null) {
                        JSFunctionData formatFunctionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.NumberFormatFormat, c -> createFormatFunctionData(c));
                        DynamicObject formatFn = JSFunction.create(realm, formatFunctionData);
                        DynamicObject boundFn = JSFunction.boundFunctionCreate(context, realm, formatFn, numberFormatObj, new Object[]{}, JSObject.getPrototype(formatFn), true);
                        state.boundFormatFunction = boundFn;
                    }

                    return state.boundFormatFunction;
                }
                throw Errors.createTypeError("expected NumberFormat object");
            }
        });
    }

    private static void ensureIsNumberFormat(Object obj) {
        if (!isJSNumberFormat(obj)) {
            throw Errors.createTypeError("NumberFormat method called on a non-object or on a wrong type of object (uninitialized NumberFormat?).");
        }
    }

    private static JSFunctionData createFormatFunctionData(JSContext context) {
        return JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                DynamicObject thisObj = JSRuntime.toObject(context, JSArguments.getThisObject(arguments));
                Object n = JSArguments.getUserArgumentCount(arguments) > 0 ? JSArguments.getUserArgument(arguments, 0) : Undefined.instance;
                return format(thisObj, n);
            }
        }), 1, "format");
    }

    private static DynamicObject createFormatFunctionGetter(JSRealm realm, JSContext context) {
        CallTarget ct = createGetFormatCallTarget(realm, context);
        JSFunctionData fd = JSFunctionData.create(context, ct, ct, 0, "get format", false, false, false, true);
        DynamicObject compareFunction = JSFunction.create(realm, fd);
        return compareFunction;
    }
}
