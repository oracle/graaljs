/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.text.Format;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.UnmodifiableEconomicMap;

import com.ibm.icu.text.ConstrainedFieldPosition;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateIntervalFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.DateInterval;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.intl.DateTimeFormatFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.DateTimeFormatPrototypeBuiltins;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.LazyValue;

public final class JSDateTimeFormat extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String CLASS_NAME = "DateTimeFormat";
    public static final String PROTOTYPE_NAME = "DateTimeFormat.prototype";

    static final HiddenKey BOUND_OBJECT_KEY = new HiddenKey(CLASS_NAME);

    public static final JSDateTimeFormat INSTANCE = new JSDateTimeFormat();

    /**
     * Maps the upper-case version of a supported time zone to the corresponding case-regularized
     * canonical ID.
     */
    private static final LazyValue<UnmodifiableEconomicMap<String, String>> canonicalTimeZoneIDMap = new LazyValue<>(JSDateTimeFormat::initCanonicalTimeZoneIDMap);

    private JSDateTimeFormat() {
    }

    public static boolean isJSDateTimeFormat(Object obj) {
        return obj instanceof JSDateTimeFormatObject;
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
        JSObjectUtil.putFunctionsFromContainer(realm, numberFormatPrototype, DateTimeFormatPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putBuiltinAccessorProperty(numberFormatPrototype, "format", createFormatFunctionGetter(realm, ctx), Undefined.instance);
        JSObjectUtil.putToStringTag(numberFormatPrototype, "Intl.DateTimeFormat");
        return numberFormatPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, DateTimeFormatFunctionBuiltins.BUILTINS);
    }

    public static DynamicObject create(JSContext context, JSRealm realm) {
        InternalState state = new InternalState();
        JSObjectFactory factory = context.getDateTimeFormatFactory();
        JSDateTimeFormatObject obj = new JSDateTimeFormatObject(factory.getShape(realm), state);
        factory.initProto(obj, realm);
        assert isJSDateTimeFormat(obj);
        return context.trackAllocation(obj);
    }

    @TruffleBoundary
    public static void setupInternalDateTimeFormat(
                    JSContext ctx,
                    InternalState state, String[] locales,
                    String weekdayOpt,
                    String eraOpt,
                    String yearOpt,
                    String monthOpt,
                    String dayOpt,
                    String dayPeriodOpt,
                    String hourOpt,
                    String hcOpt,
                    Boolean hour12Opt,
                    String minuteOpt,
                    String secondOpt,
                    int fractionalSecondDigitsOpt,
                    String tzNameOpt,
                    TimeZone timeZone,
                    String calendarOpt,
                    String numberingSystemOpt,
                    String dateStyleOpt,
                    String timeStyleOpt) {
        Locale selectedLocale = IntlUtil.selectedLocale(ctx, locales);
        Locale strippedLocale = selectedLocale.stripExtensions();

        Locale.Builder builder = new Locale.Builder();
        builder.setLocale(strippedLocale);

        String hc;
        if (hour12Opt == null) {
            String hcType = selectedLocale.getUnicodeLocaleType("hc");
            if (hcType != null && (hcOpt == null || hcOpt.equals(hcType)) && isValidHCType(hcType)) {
                hc = hcType;
                builder.setUnicodeLocaleKeyword("hc", hcType);
            } else {
                hc = hcOpt;
            }
        } else {
            hc = null;
        }

        String caType = IntlUtil.normalizeCAType(selectedLocale.getUnicodeLocaleType("ca"));
        String normCalendarOpt = IntlUtil.normalizeCAType(calendarOpt);
        if (caType != null && (normCalendarOpt == null || normCalendarOpt.equals(caType)) && isValidCAType(strippedLocale, caType)) {
            state.calendar = caType;
            builder.setUnicodeLocaleKeyword("ca", caType);
        }

        String nuType = selectedLocale.getUnicodeLocaleType("nu");
        if ((nuType != null) && IntlUtil.isValidNumberingSystem(nuType) && (numberingSystemOpt == null || numberingSystemOpt.equals(nuType))) {
            state.numberingSystem = nuType;
            builder.setUnicodeLocaleKeyword("nu", nuType);
        }

        state.locale = builder.build().toLanguageTag();

        if (normCalendarOpt != null && isValidCAType(strippedLocale, normCalendarOpt)) {
            state.calendar = normCalendarOpt;
            builder.setUnicodeLocaleKeyword("ca", normCalendarOpt);
        }

        if (numberingSystemOpt != null && IntlUtil.isValidNumberingSystem(numberingSystemOpt)) {
            state.numberingSystem = numberingSystemOpt;
            builder.setUnicodeLocaleKeyword("nu", numberingSystemOpt);
        }

        Locale javaLocale = builder.build();

        if (state.numberingSystem == null) {
            state.numberingSystem = IntlUtil.defaultNumberingSystemName(ctx, javaLocale);
        }

        state.dateStyle = dateStyleOpt;
        state.timeStyle = timeStyleOpt;

        DateTimePatternGenerator patternGenerator = DateTimePatternGenerator.getInstance(javaLocale);
        String hcDefault = IntlUtil.toJSHourCycle(patternGenerator.getDefaultHourCycle());
        if (hc == null) {
            hc = hcDefault;
        }
        if (hour12Opt != null) {
            boolean h11or23 = IntlUtil.H11.equals(hcDefault) || IntlUtil.H23.equals(hcDefault);
            if (hour12Opt) {
                hc = h11or23 ? IntlUtil.H11 : IntlUtil.H12;
            } else {
                hc = h11or23 ? IntlUtil.H23 : IntlUtil.H24;
            }
        }

        DateFormat dateFormat;
        if (timeStyleOpt == null) {
            if (dateStyleOpt == null) {
                String skeleton = makeSkeleton(weekdayOpt, eraOpt, yearOpt, monthOpt, dayOpt, dayPeriodOpt, hourOpt, hc, minuteOpt, secondOpt, fractionalSecondDigitsOpt, tzNameOpt);

                String bestPattern = patternGenerator.getBestPattern(skeleton);
                String baseSkeleton = patternGenerator.getBaseSkeleton(bestPattern);

                if (containsOneOf(baseSkeleton, "eEc")) {
                    state.weekday = weekdayOpt;
                }

                if (baseSkeleton.contains("G")) {
                    state.era = eraOpt;
                }

                if (containsOneOf(baseSkeleton, "YyUu")) {
                    state.year = yearOpt;
                }

                if (containsOneOf(baseSkeleton, "ML")) {
                    state.month = monthOpt;
                }

                if (containsOneOf(baseSkeleton, "dDFg")) {
                    state.day = dayOpt;
                }

                if (containsOneOf(baseSkeleton, "Bb")) {
                    state.dayPeriod = dayPeriodOpt;
                }

                if (containsOneOf(baseSkeleton, "hHKk")) {
                    state.hour = hourOpt;
                    state.hourCycle = hc;
                }

                if (baseSkeleton.contains("m")) {
                    state.minute = minuteOpt;
                }

                if (baseSkeleton.contains("s")) {
                    state.second = secondOpt;
                }

                if (containsOneOf(baseSkeleton, "SA")) {
                    state.fractionalSecondDigits = fractionalSecondDigitsOpt;
                }

                dateFormat = new SimpleDateFormat(bestPattern, javaLocale);
            } else {
                dateFormat = DateFormat.getDateInstance(dateFormatStyle(dateStyleOpt), javaLocale);
            }
        } else {
            if (dateStyleOpt == null) {
                dateFormat = DateFormat.getTimeInstance(dateFormatStyle(timeStyleOpt), javaLocale);
            } else {
                dateFormat = DateFormat.getDateTimeInstance(dateFormatStyle(dateStyleOpt), dateFormatStyle(timeStyleOpt), javaLocale);
            }
            state.hourCycle = hc;
        }

        String pattern = ((SimpleDateFormat) dateFormat).toPattern();
        String skeleton = patternGenerator.getSkeleton(pattern);
        if (!Objects.equals(state.hourCycle, hourCycleFromPattern(pattern))) {
            skeleton = replaceHourCycle(skeleton, hc);
            String bestPattern = patternGenerator.getBestPattern(skeleton, DateTimePatternGenerator.MATCH_HOUR_FIELD_LENGTH);
            dateFormat = new SimpleDateFormat(replaceHourCycle(bestPattern, hc), javaLocale);
        }

        state.dateFormat = dateFormat;
        state.dateIntervalFormat = DateIntervalFormat.getInstance(skeleton, javaLocale);

        if (state.calendar == null) {
            state.calendar = IntlUtil.normalizeCAType(Calendar.getInstance(javaLocale).getType());
        }
        if ("gregory".equals(state.calendar)) {
            // Ensure that Gregorian calendar is used for all dates.
            // GregorianCalendar used by SimpleDateFormat is using
            // Julian calendar for dates before 1582 otherwise.
            Calendar calendar = dateFormat.getCalendar();
            if (!(calendar instanceof GregorianCalendar)) {
                calendar = new GregorianCalendar(javaLocale);
                dateFormat.setCalendar(calendar);
            }
            ((GregorianCalendar) calendar).setGregorianChange(new Date(Long.MIN_VALUE));
        }

        if (tzNameOpt != null && !tzNameOpt.isEmpty()) {
            state.timeZoneName = tzNameOpt;
        }

        state.dateFormat.setTimeZone(timeZone);
        state.timeZone = timeZone.getID();
        state.initialized = true;
    }

    private static int dateFormatStyle(String style) {
        if (IntlUtil.FULL.equals(style)) {
            return DateFormat.FULL;
        } else if (IntlUtil.LONG.equals(style)) {
            return DateFormat.LONG;
        } else if (IntlUtil.MEDIUM.equals(style)) {
            return DateFormat.MEDIUM;
        } else {
            assert IntlUtil.SHORT.equals(style);
            return DateFormat.SHORT;
        }
    }

    private static String hourCycleFromPattern(String pattern) {
        if (pattern.indexOf('K') != -1) {
            return IntlUtil.H11;
        } else if (pattern.indexOf('h') != -1) {
            return IntlUtil.H12;
        } else if (pattern.indexOf('H') != -1) {
            return IntlUtil.H23;
        } else if (pattern.indexOf('k') != -1) {
            return IntlUtil.H24;
        } else {
            return null;
        }
    }

    private static String replaceHourCycle(String pattern, String hourCycle) {
        StringBuilder sb = new StringBuilder();
        char replacement;
        if (IntlUtil.H11.equals(hourCycle)) {
            replacement = 'K';
        } else if (IntlUtil.H12.equals(hourCycle)) {
            replacement = 'h';
        } else if (IntlUtil.H23.equals(hourCycle)) {
            replacement = 'H';
        } else {
            assert IntlUtil.H24.equals(hourCycle);
            replacement = 'k';
        }
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case 'K':
                case 'h':
                case 'H':
                case 'k':
                    sb.append(replacement);
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isValidHCType(String hcType) {
        return IntlUtil.H11.equals(hcType) || IntlUtil.H12.equals(hcType) || IntlUtil.H23.equals(hcType) || IntlUtil.H24.equals(hcType);
    }

    private static boolean isValidCAType(Locale locale, String calendar) {
        assert Objects.equals(calendar, IntlUtil.normalizeCAType(calendar));
        String[] validValues = Calendar.getKeywordValuesForLocale("ca", ULocale.forLocale(locale), false);
        for (String validValue : validValues) {
            if (IntlUtil.normalizeCAType(validValue).equals(calendar)) {
                return true;
            }
        }
        return false;
    }

    private static String weekdayOptToSkeleton(String weekdayOpt) {
        if (weekdayOpt == null) {
            return "";
        }
        switch (weekdayOpt) {
            case IntlUtil.NARROW:
                return "eeeee";
            case IntlUtil.SHORT:
                return "eee";
            case IntlUtil.LONG:
                return "eeee";
        }
        return "";
    }

    private static String eraOptToSkeleton(String eraOpt) {
        if (eraOpt == null) {
            return "";
        }
        switch (eraOpt) {
            case IntlUtil.NARROW:
                return "GGGGG";
            case IntlUtil.SHORT:
                return "GGG";
            case IntlUtil.LONG:
                return "GGGG";
        }
        return "";
    }

    private static String yearOptToSkeleton(String yearOpt) {
        if (yearOpt == null) {
            return "";
        }
        switch (yearOpt) {
            case IntlUtil._2_DIGIT:
                return "yy";
            case IntlUtil.NUMERIC:
                return "y";
        }
        return "";
    }

    private static String monthOptToSkeleton(String monthOpt) {
        if (monthOpt == null) {
            return "";
        }
        switch (monthOpt) {
            case IntlUtil._2_DIGIT:
                return "MM";
            case IntlUtil.NUMERIC:
                return "M";
            case IntlUtil.NARROW:
                return "MMMMM";
            case IntlUtil.SHORT:
                return "MMM";
            case IntlUtil.LONG:
                return "MMMM";
        }
        return "";
    }

    private static String dayOptToSkeleton(String dayOpt) {
        if (dayOpt == null) {
            return "";
        }
        switch (dayOpt) {
            case IntlUtil._2_DIGIT:
                return "dd";
            case IntlUtil.NUMERIC:
                return "d";
        }
        return "";
    }

    private static String dayPeriodOptToSkeleton(String dayPeriodOpt) {
        if (dayPeriodOpt == null) {
            return "";
        }
        switch (dayPeriodOpt) {
            case IntlUtil.NARROW:
                return "BBBBB";
            case IntlUtil.SHORT:
                return "B";
            case IntlUtil.LONG:
                return "BBBB";
        }
        return "";
    }

    private static String hourOptToSkeleton(String hourOpt, String hcOpt) {
        if (hourOpt == null) {
            return "";
        }
        switch (hourOpt) {
            case IntlUtil._2_DIGIT:
                switch (hcOpt) {
                    case IntlUtil.H11:
                        return "KK";
                    case IntlUtil.H12:
                        return "hh";
                    case IntlUtil.H23:
                        return "HH";
                    case IntlUtil.H24:
                        return "kk";
                }
                break;
            case IntlUtil.NUMERIC:
                switch (hcOpt) {
                    case IntlUtil.H11:
                        return "K";
                    case IntlUtil.H12:
                        return "h";
                    case IntlUtil.H23:
                        return "H";
                    case IntlUtil.H24:
                        return "k";
                }
                break;
        }
        return "";
    }

    private static String minuteOptToSkeleton(String minuteOpt) {
        if (minuteOpt == null) {
            return "";
        }
        switch (minuteOpt) {
            case IntlUtil._2_DIGIT:
                return "mm";
            case IntlUtil.NUMERIC:
                return "m";
        }
        return "";
    }

    private static String secondOptToSkeleton(String secondOpt, int fractionalSecondDigitsOpt) {
        StringBuilder skeleton = new StringBuilder();
        if (secondOpt != null) {
            if (IntlUtil.NUMERIC.equals(secondOpt)) {
                skeleton.append("s");
            } else {
                assert IntlUtil._2_DIGIT.equals(secondOpt);
                skeleton.append("ss");
            }
        }
        for (int i = 0; i < fractionalSecondDigitsOpt; i++) {
            skeleton.append('S');
        }
        return skeleton.toString();
    }

    private static String timeZoneNameOptToSkeleton(String timeZoneNameOpt) {
        if (timeZoneNameOpt == null) {
            return "";
        }
        switch (timeZoneNameOpt) {
            case IntlUtil.SHORT:
                return "v";
            case IntlUtil.LONG:
                return "vvvv";
        }
        return "";
    }

    private static String makeSkeleton(String weekdayOpt, String eraOpt, String yearOpt, String monthOpt, String dayOpt, String dayPeriodOpt, String hourOpt, String hcOpt, String minuteOpt,
                    String secondOpt, int fractionalSecondDigitsOpt, String timeZoneNameOpt) {
        return weekdayOptToSkeleton(weekdayOpt) + eraOptToSkeleton(eraOpt) + yearOptToSkeleton(yearOpt) + monthOptToSkeleton(monthOpt) + dayOptToSkeleton(dayOpt) +
                        dayPeriodOptToSkeleton(dayPeriodOpt) + hourOptToSkeleton(hourOpt, hcOpt) + minuteOptToSkeleton(minuteOpt) + secondOptToSkeleton(secondOpt, fractionalSecondDigitsOpt) +
                        timeZoneNameOptToSkeleton(timeZoneNameOpt);
    }

    private static UnmodifiableEconomicMap<String, String> initCanonicalTimeZoneIDMap() {
        CompilerAsserts.neverPartOfCompilation();
        EconomicMap<String, String> map = EconomicMap.create();
        for (String available : TimeZone.getAvailableIDs()) {
            map.put(IntlUtil.toUpperCase(available), TimeZone.getCanonicalID(available));
        }
        return map;
    }

    /**
     * Returns the canonical and case-regularized form of the timeZone argument. Returns null if the
     * argument is not a valid time zone name.
     *
     * https://tc39.github.io/ecma402/#sec-canonicalizetimezonename
     */
    @TruffleBoundary
    public static String canonicalizeTimeZoneName(String tzId) {
        String ucTzId = IntlUtil.toUpperCase(tzId);
        String canTzId = canonicalTimeZoneIDMap.get().get(ucTzId);
        if (canTzId == null) {
            return null;
        }
        if (canTzId.equals("Etc/UTC") || canTzId.equals("Etc/GMT")) {
            return "UTC";
        } else {
            return canTzId;
        }
    }

    private static boolean containsOneOf(String suspect, String containees) {
        for (int c : containees.getBytes()) {
            if (suspect.indexOf(c) > -1) {
                return true;
            }
        }
        return false;
    }

    public static DateFormat getDateFormatProperty(DynamicObject obj) {
        return getInternalState(obj).dateFormat;
    }

    @TruffleBoundary
    public static String format(DynamicObject numberFormatObj, Object n) {
        DateFormat dateFormat = getDateFormatProperty(numberFormatObj);
        return dateFormat.format(timeClip(n));
    }

    private static double timeClip(Object n) {
        double x;
        if (n == Undefined.instance) {
            x = JSRealm.get(null).currentTimeMillis();
        } else {
            x = JSDate.timeClip(JSRuntime.toDouble(n));
            if (Double.isNaN(x)) {
                throwDateOutOfRange();
            }
        }
        return x;
    }

    private static void throwDateOutOfRange() throws JSException {
        throw Errors.createRangeError("Provided date is not in valid range.");
    }

    private static final LazyValue<UnmodifiableEconomicMap<DateFormat.Field, String>> fieldToTypeMap = new LazyValue<>(JSDateTimeFormat::initializeFieldToTypeMap);

    @SuppressWarnings("deprecation")
    private static UnmodifiableEconomicMap<DateFormat.Field, String> initializeFieldToTypeMap() {
        CompilerAsserts.neverPartOfCompilation();
        EconomicMap<DateFormat.Field, String> map = EconomicMap.create(14);
        map.put(DateFormat.Field.AM_PM, "dayPeriod");
        map.put(DateFormat.Field.AM_PM_MIDNIGHT_NOON, "dayPeriod");
        map.put(DateFormat.Field.FLEXIBLE_DAY_PERIOD, "dayPeriod");
        map.put(DateFormat.Field.ERA, "era");
        map.put(DateFormat.Field.YEAR, "year");
        map.put(DateFormat.Field.RELATED_YEAR, "relatedYear");
        map.put(DateFormat.Field.MONTH, "month");
        map.put(DateFormat.Field.DOW_LOCAL, "weekday");
        map.put(DateFormat.Field.DAY_OF_WEEK, "weekday");
        map.put(DateFormat.Field.DAY_OF_MONTH, "day");
        map.put(DateFormat.Field.HOUR0, "hour");
        map.put(DateFormat.Field.HOUR1, "hour");
        map.put(DateFormat.Field.HOUR_OF_DAY0, "hour");
        map.put(DateFormat.Field.HOUR_OF_DAY1, "hour");
        map.put(DateFormat.Field.MINUTE, "minute");
        map.put(DateFormat.Field.SECOND, "second");
        map.put(DateFormat.Field.MILLISECOND, "fractionalSecond");
        map.put(DateFormat.Field.MILLISECONDS_IN_DAY, "fractionalSecond");
        map.put(DateFormat.Field.TIME_ZONE, "timeZoneName");
        return map;
    }

    private static String fieldToType(DateFormat.Field field) {
        return fieldToTypeMap.get().get(field);
    }

    @TruffleBoundary
    public static DynamicObject formatToParts(JSContext context, JSRealm realm, DynamicObject numberFormatObj, Object n, String source) {

        DateFormat dateFormat = getDateFormatProperty(numberFormatObj);
        String yearPattern = yearRelatedSubpattern(dateFormat);
        int yearPatternIndex = 0;

        double x = timeClip(n);

        List<Object> resultParts = new ArrayList<>();
        AttributedCharacterIterator fit = dateFormat.formatToCharacterIterator(x);
        String formatted = dateFormat.format(x);
        int i = fit.getBeginIndex();
        while (i < fit.getEndIndex()) {
            fit.setIndex(i);
            Map<AttributedCharacterIterator.Attribute, Object> attributes = fit.getAttributes();
            Set<AttributedCharacterIterator.Attribute> attKeySet = attributes.keySet();
            if (!attKeySet.isEmpty()) {
                for (AttributedCharacterIterator.Attribute a : attKeySet) {
                    if (a instanceof DateFormat.Field) {
                        String value = formatted.substring(fit.getRunStart(), fit.getRunLimit());
                        String type;
                        if (a == DateFormat.Field.YEAR) {
                            // DateFormat.Field.YEAR covers both "year" and "yearName"
                            if (yearPatternIndex < yearPattern.length() && yearPattern.charAt(yearPatternIndex) == 'U') {
                                type = IntlUtil.YEAR_NAME;
                            } else {
                                type = IntlUtil.YEAR;
                            }
                            yearPatternIndex++;
                        } else {
                            type = fieldToType((DateFormat.Field) a);
                            assert type != null : a;
                        }
                        resultParts.add(makePart(context, realm, type, value, source));
                        i = fit.getRunLimit();
                        break;
                    } else {
                        throw Errors.shouldNotReachHere();
                    }
                }
            } else {
                String value = formatted.substring(fit.getRunStart(), fit.getRunLimit());
                resultParts.add(makePart(context, realm, IntlUtil.LITERAL, value, source));
                i = fit.getRunLimit();
            }
        }
        return JSArray.createConstant(context, realm, resultParts.toArray());
    }

    @TruffleBoundary
    public static String formatRange(DynamicObject dateTimeFormat, double startDate, double endDate) {
        DateInterval range = new DateInterval((long) startDate, (long) endDate);
        DateIntervalFormat dateIntervalFormat = getInternalState(dateTimeFormat).dateIntervalFormat;
        DateIntervalFormat.FormattedDateInterval formattedRange = dateIntervalFormat.formatToValue(range);

        if (dateFieldsPracticallyEqual(formattedRange)) {
            return JSDateTimeFormat.format(dateTimeFormat, startDate);
        }

        return formattedRange.toString();
    }

    private static boolean dateFieldsPracticallyEqual(DateIntervalFormat.FormattedDateInterval formattedRange) {
        ConstrainedFieldPosition cfPos = new ConstrainedFieldPosition();
        while (formattedRange.nextPosition(cfPos)) {
            if (cfPos.getField() instanceof DateIntervalFormat.SpanField) {
                return false;
            }
        }
        return true;
    }

    @TruffleBoundary
    public static DynamicObject formatRangeToParts(JSContext context, JSRealm realm, DynamicObject dateTimeFormat, double startDate, double endDate) {
        DateInterval range = new DateInterval((long) startDate, (long) endDate);
        DateIntervalFormat dateIntervalFormat = getInternalState(dateTimeFormat).dateIntervalFormat;
        DateIntervalFormat.FormattedDateInterval formattedRange = dateIntervalFormat.formatToValue(range);

        if (dateFieldsPracticallyEqual(formattedRange)) {
            return JSDateTimeFormat.formatToParts(context, realm, dateTimeFormat, startDate, IntlUtil.SHARED);
        }

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
                String source = sourceString(lastLimit, start, startRangeStart, startRangeLimit, endRangeStart, endRangeLimit);
                parts.add(makePart(context, realm, IntlUtil.LITERAL, literal, source));
                lastLimit = start;
            }

            Format.Field field = cfPos.getField();
            if (field instanceof DateIntervalFormat.SpanField) {
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
            } else if (field instanceof DateFormat.Field) {
                String type = fieldToType((DateFormat.Field) field);
                String value = formattedString.substring(start, limit);
                String source = sourceString(start, limit, startRangeStart, startRangeLimit, endRangeStart, endRangeLimit);
                parts.add(makePart(context, realm, type, value, source));
                lastLimit = limit;
            } else {
                throw Errors.shouldNotReachHere(field.toString());
            }
        }

        int length = formattedString.length();
        if (lastLimit < length) { // Literal at the end
            String literal = formattedString.substring(lastLimit, length);
            String source = sourceString(lastLimit, length, startRangeStart, startRangeLimit, endRangeStart, endRangeLimit);
            parts.add(makePart(context, realm, IntlUtil.LITERAL, literal, source));
        }

        return JSArray.createConstant(context, realm, parts.toArray());
    }

    private static String sourceString(int start, int limit, int startRangeStart, int startRangeLimit, int endRangeStart, int endRangeLimit) {
        String source;
        if (startRangeStart <= start && limit <= startRangeLimit) {
            source = IntlUtil.START_RANGE;
        } else if (endRangeStart <= start && limit <= endRangeLimit) {
            source = IntlUtil.END_RANGE;
        } else {
            source = IntlUtil.SHARED;
        }
        return source;
    }

    private static String yearRelatedSubpattern(DateFormat dateFormat) {
        if (dateFormat instanceof SimpleDateFormat) {
            String pattern = ((SimpleDateFormat) dateFormat).toPattern();
            StringBuilder sb = new StringBuilder();
            boolean quoted = false;
            for (char c : pattern.toCharArray()) {
                if (c == '\'') {
                    quoted = !quoted;
                } else if (!quoted && (c == 'y' || c == 'Y' || c == 'u' || c == 'U')) {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        return "";
    }

    private static Object makePart(JSContext context, JSRealm realm, String type, String value, String source) {
        DynamicObject p = JSOrdinary.create(context, realm);
        JSObject.set(p, IntlUtil.TYPE, type);
        JSObject.set(p, IntlUtil.VALUE, value);
        if (source != null) {
            JSObject.set(p, IntlUtil.SOURCE, source);
        }
        return p;
    }

    public static class InternalState {

        private boolean initialized = false;
        private DateFormat dateFormat;
        private DateIntervalFormat dateIntervalFormat;

        private DynamicObject boundFormatFunction = null;

        private String locale;
        private String calendar;
        private String numberingSystem;

        private String weekday;
        private String era;
        private String year;
        private String month;
        private String day;
        private String dayPeriod;
        private String hour;
        private String minute;
        private String second;
        private int fractionalSecondDigits;

        private String hourCycle;

        private String timeZoneName;
        private String timeZone;

        private String dateStyle;
        private String timeStyle;

        DynamicObject toResolvedOptionsObject(JSContext context, JSRealm realm) {
            DynamicObject result = JSOrdinary.create(context, realm);
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.LOCALE, locale, JSAttributes.getDefault());
            if (calendar != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.CALENDAR, calendar, JSAttributes.getDefault());
            }
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.NUMBERING_SYSTEM, numberingSystem, JSAttributes.getDefault());
            if (timeZone != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.TIME_ZONE, timeZone, JSAttributes.getDefault());
            }
            if (hourCycle != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.HOUR_CYCLE, hourCycle, JSAttributes.getDefault());
                boolean hour12 = IntlUtil.H11.equals(hourCycle) || IntlUtil.H12.equals(hourCycle);
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.HOUR12, hour12, JSAttributes.getDefault());
            }
            if (weekday != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.WEEKDAY, weekday, JSAttributes.getDefault());
            }
            if (era != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.ERA, era, JSAttributes.getDefault());
            }
            if (year != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.YEAR, year, JSAttributes.getDefault());
            }
            if (month != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.MONTH, month, JSAttributes.getDefault());
            }
            if (day != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.DAY, day, JSAttributes.getDefault());
            }
            if (dayPeriod != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.DAY_PERIOD, dayPeriod, JSAttributes.getDefault());
            }
            if (hour != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.HOUR, hour, JSAttributes.getDefault());
            }
            if (minute != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.MINUTE, minute, JSAttributes.getDefault());
            }
            if (second != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.SECOND, second, JSAttributes.getDefault());
            }
            if (fractionalSecondDigits != 0) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.FRACTIONAL_SECOND_DIGITS, fractionalSecondDigits, JSAttributes.getDefault());
            }
            if (timeZoneName != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.TIME_ZONE_NAME, timeZoneName, JSAttributes.getDefault());
            }
            if (dateStyle != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.DATE_STYLE, dateStyle, JSAttributes.getDefault());
            }
            if (timeStyle != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.TIME_STYLE, timeStyle, JSAttributes.getDefault());
            }
            return result;
        }
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, JSRealm realm, DynamicObject numberFormatObj) {
        InternalState state = getInternalState(numberFormatObj);
        return state.toResolvedOptionsObject(context, realm);
    }

    public static InternalState getInternalState(DynamicObject obj) {
        assert isJSDateTimeFormat(obj);
        return ((JSDateTimeFormatObject) obj).getInternalState();
    }

    private static CallTarget createGetFormatCallTarget(JSContext context) {
        return new JavaScriptRootNode(context.getLanguage(), null, null) {
            private final BranchProfile errorBranch = BranchProfile.create();
            @Child private PropertySetNode setBoundObjectNode = PropertySetNode.createSetHidden(BOUND_OBJECT_KEY, context);

            @Override
            public Object execute(VirtualFrame frame) {

                Object[] frameArgs = frame.getArguments();
                Object dateTimeFormatObj = JSArguments.getThisObject(frameArgs);

                if (isJSDateTimeFormat(dateTimeFormatObj)) {

                    InternalState state = getInternalState((DynamicObject) dateTimeFormatObj);

                    if (state == null || !state.initialized) {
                        errorBranch.enter();
                        throw Errors.createTypeErrorMethodCalledOnNonObjectOrWrongType("format");
                    }

                    if (state.boundFormatFunction == null) {
                        JSFunctionData formatFunctionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.DateTimeFormatFormat, c -> createFormatFunctionData(c));
                        DynamicObject formatFn = JSFunction.create(getRealm(), formatFunctionData);
                        setBoundObjectNode.setValue(formatFn, dateTimeFormatObj);
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

            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                DynamicObject thisObj = (DynamicObject) getBoundObjectNode.getValue(JSArguments.getFunctionObject(arguments));
                assert isJSDateTimeFormat(thisObj);
                Object n = JSArguments.getUserArgumentCount(arguments) > 0 ? JSArguments.getUserArgument(arguments, 0) : Undefined.instance;
                return format(thisObj, n);
            }
        }.getCallTarget(), 1, "");
    }

    private static DynamicObject createFormatFunctionGetter(JSRealm realm, JSContext context) {
        JSFunctionData fd = realm.getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.DateTimeFormatGetFormat, (c) -> {
            CallTarget ct = createGetFormatCallTarget(context);
            return JSFunctionData.create(context, ct, ct, 0, "get format", false, false, false, true);
        });
        return JSFunction.create(realm, fd);
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getDateTimeFormatPrototype();
    }
}
