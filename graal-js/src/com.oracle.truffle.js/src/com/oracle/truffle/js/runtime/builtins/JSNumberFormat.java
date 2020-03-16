/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.UnmodifiableEconomicMap;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.intl.NumberFormatFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.NumberFormatPrototypeBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.LazyValue;

public final class JSNumberFormat extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String CLASS_NAME = "NumberFormat";
    public static final String PROTOTYPE_NAME = "NumberFormat.prototype";

    private static final HiddenKey INTERNAL_STATE_ID = new HiddenKey("_internalState");
    private static final Property INTERNAL_STATE_PROPERTY;

    static final HiddenKey BOUND_OBJECT_KEY = new HiddenKey(CLASS_NAME);

    public static final JSNumberFormat INSTANCE = new JSNumberFormat();

    protected static final List<String> BCP47_CU_KEYS = Arrays.asList(new String[]{
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
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject numberFormatPrototype = JSObject.createInit(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, numberFormatPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, numberFormatPrototype, NumberFormatPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putConstantAccessorProperty(ctx, numberFormatPrototype, "format", createFormatFunctionGetter(realm, ctx), Undefined.instance);
        JSObjectUtil.putDataProperty(ctx, numberFormatPrototype, Symbol.SYMBOL_TO_STRING_TAG, "Object", JSAttributes.configurableNotEnumerableNotWritable());
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

    @Override
    public Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        initialShape = initialShape.addProperty(INTERNAL_STATE_PROPERTY);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, NumberFormatFunctionBuiltins.BUILTINS);
    }

    public static DynamicObject create(JSContext context) {
        InternalState state = new InternalState();
        DynamicObject result = JSObject.create(context, context.getNumberFormatFactory(), state);
        assert isJSNumberFormat(result);
        return result;
    }

    @TruffleBoundary
    public static void setLocaleAndNumberingSystem(JSContext ctx, BasicInternalState state, String[] locales, String numberingSystemOpt) {
        String selectedTag = IntlUtil.selectedLocale(ctx, locales);
        Locale selectedLocale = selectedTag != null ? Locale.forLanguageTag(selectedTag) : ctx.getLocale();
        Locale strippedLocale = selectedLocale.stripExtensions();
        if (strippedLocale.toLanguageTag().equals(IntlUtil.UND)) {
            selectedLocale = ctx.getLocale();
            strippedLocale = selectedLocale.stripExtensions();
        }
        Locale.Builder builder = new Locale.Builder();
        builder.setLocale(strippedLocale);

        String nuType = selectedLocale.getUnicodeLocaleType("nu");
        if ((nuType != null) && IntlUtil.isValidNumberingSystem(nuType) && (numberingSystemOpt == null || numberingSystemOpt.equals(nuType))) {
            state.numberingSystem = nuType;
            builder.setUnicodeLocaleKeyword("nu", nuType);
        }

        state.locale = builder.build().toLanguageTag();

        if (numberingSystemOpt != null && IntlUtil.isValidNumberingSystem(numberingSystemOpt)) {
            state.numberingSystem = numberingSystemOpt;
            builder.setUnicodeLocaleKeyword("nu", numberingSystemOpt);
        }

        state.javaLocale = builder.build();

        if (state.numberingSystem == null) {
            state.numberingSystem = IntlUtil.defaultNumberingSystemName(ctx, state.javaLocale);
        }
    }

    @TruffleBoundary
    public static void setupInternalNumberFormat(InternalState state) {
        if (state.style.equals(IntlUtil.CURRENCY)) {
            state.numberFormat = NumberFormat.getCurrencyInstance(state.javaLocale);
        } else if (state.style.equals(IntlUtil.PERCENT)) {
            state.numberFormat = NumberFormat.getPercentInstance(state.javaLocale);
        } else {
            state.numberFormat = NumberFormat.getInstance(state.javaLocale);
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

    private static final LazyValue<UnmodifiableEconomicMap<NumberFormat.Field, String>> fieldToTypeMap = new LazyValue<>(JSNumberFormat::initializeFieldToTypeMap);

    private static UnmodifiableEconomicMap<NumberFormat.Field, String> initializeFieldToTypeMap() {
        CompilerAsserts.neverPartOfCompilation();
        EconomicMap<NumberFormat.Field, String> map = EconomicMap.create(6);
        map.put(NumberFormat.Field.INTEGER, "integer");
        map.put(NumberFormat.Field.DECIMAL_SEPARATOR, "decimal");
        map.put(NumberFormat.Field.FRACTION, "fraction");
        map.put(NumberFormat.Field.GROUPING_SEPARATOR, "group");
        map.put(NumberFormat.Field.CURRENCY, "currency");
        map.put(NumberFormat.Field.PERCENT, "percentSign");
        return map;
    }

    private static String fieldToType(NumberFormat.Field field) {
        return fieldToTypeMap.get().get(field);
    }

    @TruffleBoundary
    public static DynamicObject formatToParts(JSContext context, DynamicObject numberFormatObj, Object n) {

        NumberFormat numberFormat = getNumberFormatProperty(numberFormatObj);
        Number x = toInternalNumberRepresentation(JSRuntime.toNumeric(n));

        List<DynamicObject> resultParts = innerFormatToParts(context, numberFormat, x, null);
        return JSArray.createConstant(context, resultParts.toArray());
    }

    static List<DynamicObject> innerFormatToParts(JSContext context, NumberFormat numberFormat, Number x, String unit) {
        List<DynamicObject> resultParts = new ArrayList<>();
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
                        String type;
                        if (a == NumberFormat.Field.INTEGER) {
                            double xDouble = x.doubleValue();
                            if (Double.isNaN(xDouble)) {
                                type = "nan";
                            } else if (Double.isInfinite(xDouble)) {
                                type = "infinite";
                            } else {
                                type = "integer";
                            }
                        } else if (a == NumberFormat.Field.SIGN) {
                            type = isPlusSign(value) ? "plusSign" : "minusSign";
                        } else {
                            type = fieldToType((NumberFormat.Field) a);
                            assert type != null;
                        }
                        resultParts.add(IntlUtil.makePart(context, type, value, unit));
                        i = fit.getRunLimit();
                        break;
                    } else {
                        throw Errors.shouldNotReachHere();
                    }
                }
            } else {
                String value = formatted.substring(fit.getRunStart(), fit.getRunLimit());
                resultParts.add(IntlUtil.makePart(context, IntlUtil.LITERAL, value, unit));
                i = fit.getRunLimit();
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
        } else if (o instanceof Number) {
            return (Number) o;
        } else if (o instanceof BigInt) {
            return ((BigInt) o).bigIntegerValue();
        } else {
            throw Errors.shouldNotReachHere();
        }
    }

    public static class BasicInternalState {

        protected boolean initialized = false;

        protected NumberFormat numberFormat;

        protected Locale javaLocale;
        protected String locale;

        protected String numberingSystem;

        protected int minimumIntegerDigits = 1;
        protected int minimumFractionDigits = 0;
        protected int maximumFractionDigits = 3;
        protected Integer minimumSignificantDigits;
        protected Integer maximumSignificantDigits;

        DynamicObject toResolvedOptionsObject(JSContext context) {
            DynamicObject resolvedOptions = JSUserObject.create(context);
            fillResolvedOptions(context, resolvedOptions);
            return resolvedOptions;
        }

        void fillResolvedOptions(@SuppressWarnings("unused") JSContext context, DynamicObject result) {
            JSObjectUtil.defineDataProperty(result, IntlUtil.MINIMUM_INTEGER_DIGITS, minimumIntegerDigits, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, IntlUtil.MINIMUM_FRACTION_DIGITS, minimumFractionDigits, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, IntlUtil.MAXIMUM_FRACTION_DIGITS, maximumFractionDigits, JSAttributes.getDefault());
            if (minimumSignificantDigits != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.MINIMUM_SIGNIFICANT_DIGITS, minimumSignificantDigits, JSAttributes.getDefault());
            }
            if (maximumSignificantDigits != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.MAXIMUM_SIGNIFICANT_DIGITS, maximumSignificantDigits, JSAttributes.getDefault());
            }
        }

        @TruffleBoundary
        public void setIntegerAndFractionsDigits(int minimumIntegerDigits, int minimumFractionDigits, int maximumFractionDigits) {
            this.minimumIntegerDigits = minimumIntegerDigits;
            this.minimumFractionDigits = minimumFractionDigits;
            this.maximumFractionDigits = maximumFractionDigits;
            numberFormat.setMinimumIntegerDigits(minimumIntegerDigits);
            numberFormat.setMinimumFractionDigits(minimumFractionDigits);
            numberFormat.setMaximumFractionDigits(maximumFractionDigits);
        }

        @TruffleBoundary
        public void setSignificantDigits(int minimumSignificantDigits, int maximumSignificantDigits) {
            this.minimumSignificantDigits = minimumSignificantDigits;
            this.maximumSignificantDigits = maximumSignificantDigits;
            if (numberFormat instanceof DecimalFormat) {
                DecimalFormat df = (DecimalFormat) numberFormat;
                df.setMinimumSignificantDigits(minimumSignificantDigits);
                df.setMaximumSignificantDigits(maximumSignificantDigits);
            }
        }

        public boolean isInitialized() {
            return initialized;
        }

        public NumberFormat getNumberFormat() {
            return numberFormat;
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

        public int getMinimumIntegerDigits() {
            return minimumIntegerDigits;
        }

        public int getMinimumFractionDigits() {
            return minimumFractionDigits;
        }

        public int getMaximumFractionDigits() {
            return maximumFractionDigits;
        }

        public Integer getMinimumSignificantDigits() {
            return minimumSignificantDigits;
        }

        public Integer getMaximumSignificantDigits() {
            return maximumSignificantDigits;
        }

        public void setInitialized(boolean initialized) {
            this.initialized = initialized;
        }

        public void setNumberFormat(NumberFormat numberFormat) {
            this.numberFormat = numberFormat;
        }

        public void setJavaLocale(Locale javaLocale) {
            this.javaLocale = javaLocale;
        }

        public void setLocale(String locale) {
            this.locale = locale;
        }

        public void setNumberingSystem(String numberingSystem) {
            this.numberingSystem = numberingSystem;
        }

        public void setMinimumIntegerDigits(int minimumIntegerDigits) {
            this.minimumIntegerDigits = minimumIntegerDigits;
        }

        public void setMinimumFractionDigits(int minimumFractionDigits) {
            this.minimumFractionDigits = minimumFractionDigits;
        }

        public void setMaximumFractionDigits(int maximumFractionDigits) {
            this.maximumFractionDigits = maximumFractionDigits;
        }

        public void setMinimumSignificantDigits(Integer minimumSignificantDigits) {
            this.minimumSignificantDigits = minimumSignificantDigits;
        }

        public void setMaximumSignificantDigits(Integer maximumSignificantDigits) {
            this.maximumSignificantDigits = maximumSignificantDigits;
        }
    }

    public static class InternalState extends BasicInternalState {

        private String style = IntlUtil.DECIMAL;
        private String currency;
        private String currencyDisplay;
        private boolean useGrouping = true;

        DynamicObject boundFormatFunction = null;

        @Override
        void fillResolvedOptions(JSContext context, DynamicObject result) {
            JSObjectUtil.defineDataProperty(result, IntlUtil.LOCALE, locale, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, IntlUtil.NUMBERING_SYSTEM, numberingSystem, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, IntlUtil.STYLE, style, JSAttributes.getDefault());
            if (currency != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.CURRENCY, currency, JSAttributes.getDefault());
            }
            if (currencyDisplay != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.CURRENCY_DISPLAY, currencyDisplay, JSAttributes.getDefault());
            }
            super.fillResolvedOptions(context, result);
            JSObjectUtil.defineDataProperty(result, IntlUtil.USE_GROUPING, useGrouping, JSAttributes.getDefault());
        }

        @TruffleBoundary
        public void setGroupingUsed(boolean useGrouping) {
            this.useGrouping = useGrouping;
            this.numberFormat.setGroupingUsed(useGrouping);
        }

        public String getStyle() {
            return style;
        }

        public String getCurrency() {
            return currency;
        }

        public String getCurrencyDisplay() {
            return currencyDisplay;
        }

        public void setStyle(String style) {
            this.style = style;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public void setCurrencyDisplay(String currencyDisplay) {
            this.currencyDisplay = currencyDisplay;
        }
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, DynamicObject numberFormatObj) {
        InternalState state = getInternalState(numberFormatObj);
        return state.toResolvedOptionsObject(context);
    }

    public static InternalState getInternalState(DynamicObject numberFormatObj) {
        return (InternalState) INTERNAL_STATE_PROPERTY.get(numberFormatObj, isJSNumberFormat(numberFormatObj));
    }

    private static CallTarget createGetFormatCallTarget(JSContext context) {
        return Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            private final BranchProfile errorBranch = BranchProfile.create();
            @CompilationFinal private ContextReference<JSRealm> realmRef;
            @Child private PropertySetNode setBoundObjectNode = PropertySetNode.createSetHidden(BOUND_OBJECT_KEY, context);

            @Override
            public Object execute(VirtualFrame frame) {

                Object[] frameArgs = frame.getArguments();
                Object numberFormatObj = JSArguments.getThisObject(frameArgs);

                if (isJSNumberFormat(numberFormatObj)) {

                    InternalState state = getInternalState((DynamicObject) numberFormatObj);

                    if (state == null || !state.initialized) {
                        errorBranch.enter();
                        throw Errors.createTypeErrorMethodCalledOnNonObjectOrWrongType("format");
                    }

                    if (state.boundFormatFunction == null) {
                        if (realmRef == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            realmRef = lookupContextReference(JavaScriptLanguage.class);
                        }
                        JSFunctionData formatFunctionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.NumberFormatFormat, c -> createFormatFunctionData(c));
                        DynamicObject formatFn = JSFunction.create(realmRef.get(), formatFunctionData);
                        setBoundObjectNode.setValue(formatFn, numberFormatObj);
                        state.boundFormatFunction = formatFn;
                    }

                    return state.boundFormatFunction;
                }
                errorBranch.enter();
                throw Errors.createTypeErrorTypeXExpected(CLASS_NAME);
            }
        });
    }

    private static JSFunctionData createFormatFunctionData(JSContext context) {
        return JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private PropertyGetNode getBoundObjectNode = PropertyGetNode.createGetHidden(BOUND_OBJECT_KEY, context);

            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                DynamicObject thisObj = (DynamicObject) getBoundObjectNode.getValue(JSArguments.getFunctionObject(arguments));
                assert isJSNumberFormat(thisObj);
                Object n = JSArguments.getUserArgumentCount(arguments) > 0 ? JSArguments.getUserArgument(arguments, 0) : Undefined.instance;
                return format(thisObj, n);
            }
        }), 1, "");
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
