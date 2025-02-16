/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.graalvm.shadowed.com.ibm.icu.text.ConstrainedFieldPosition;
import org.graalvm.shadowed.com.ibm.icu.text.DateFormat;
import org.graalvm.shadowed.com.ibm.icu.text.DateIntervalFormat;
import org.graalvm.shadowed.com.ibm.icu.text.DateTimePatternGenerator;
import org.graalvm.shadowed.com.ibm.icu.text.NumberingSystem;
import org.graalvm.shadowed.com.ibm.icu.text.SimpleDateFormat;
import org.graalvm.shadowed.com.ibm.icu.util.Calendar;
import org.graalvm.shadowed.com.ibm.icu.util.GregorianCalendar;
import org.graalvm.shadowed.com.ibm.icu.util.TimeZone;
import org.graalvm.shadowed.com.ibm.icu.util.ULocale;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.intl.DateTimeFormatFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.DateTimeFormatPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.LazyValue;
import com.oracle.truffle.js.runtime.util.Pair;

public final class JSDateTimeFormat extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("DateTimeFormat");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("DateTimeFormat.prototype");
    public static final TruffleString TO_STRING_TAG = Strings.constant("Intl.DateTimeFormat");

    public static final JSDateTimeFormat INSTANCE = new JSDateTimeFormat();

    /**
     * Maps the upper-case version of a supported time zone to the corresponding case-regularized
     * canonical ID.
     */
    private static final LazyValue<UnmodifiableEconomicMap<String, Pair<String, String>>> canonicalTimeZoneIDMap = new LazyValue<>(JSDateTimeFormat::initCanonicalTimeZoneIDMap);

    private JSDateTimeFormat() {
    }

    public static boolean isJSDateTimeFormat(Object obj) {
        return obj instanceof JSDateTimeFormatObject;
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSObject numberFormatPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(numberFormatPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, numberFormatPrototype, DateTimeFormatPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putAccessorsFromContainer(realm, numberFormatPrototype, DateTimeFormatPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(numberFormatPrototype, TO_STRING_TAG);
        return numberFormatPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, DateTimeFormatFunctionBuiltins.BUILTINS);
    }

    public static JSDateTimeFormatObject create(JSContext context, JSRealm realm) {
        JSObjectFactory factory = context.getDateTimeFormatFactory();
        return create(factory, realm, factory.getPrototype(realm));
    }

    public static JSDateTimeFormatObject create(JSContext context, JSRealm realm, JSDynamicObject proto) {
        JSObjectFactory factory = context.getDateTimeFormatFactory();
        return create(factory, realm, proto);
    }

    private static JSDateTimeFormatObject create(JSObjectFactory factory, JSRealm realm, JSDynamicObject proto) {
        InternalState state = new InternalState();
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSDateTimeFormatObject(shape, proto, state), realm, proto);
        return factory.trackAllocation(newObj);
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
                    String timeZoneId,
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
                boolean h24RequestedByLocaleExtension = IntlUtil.H24.equals(selectedLocale.getUnicodeLocaleType("hc"));
                hc = h24RequestedByLocaleExtension ? IntlUtil.H24 : IntlUtil.H23;
            }
        }

        DateFormat dateFormat;
        if (timeStyleOpt == null) {
            if (dateStyleOpt == null) {
                String skeleton = makeSkeleton(weekdayOpt, eraOpt, yearOpt, monthOpt, dayOpt, dayPeriodOpt, hourOpt, hc, minuteOpt, secondOpt, fractionalSecondDigitsOpt, tzNameOpt);
                String bestPattern = patternGenerator.getBestPattern(skeleton, DateTimePatternGenerator.MATCH_HOUR_FIELD_LENGTH);

                if (containsOneOf(bestPattern, "eEc")) {
                    state.weekday = weekdayOpt;
                }

                if (bestPattern.contains("G")) {
                    state.era = eraOpt;
                }

                if (containsOneOf(bestPattern, "YyUu")) {
                    state.year = yearOpt;
                }

                if (containsOneOf(bestPattern, "ML")) {
                    state.month = monthOpt;
                }

                if (containsOneOf(bestPattern, "dDFg")) {
                    state.day = dayOpt;
                }

                if (containsOneOf(bestPattern, "Bb")) {
                    state.dayPeriod = dayPeriodOpt;
                }

                if (containsOneOf(bestPattern, "hHKk")) {
                    if (bestPattern.contains("hh") || bestPattern.contains("HH") || bestPattern.contains("KK") || bestPattern.contains("kk")) {
                        state.hour = IntlUtil._2_DIGIT;
                    } else {
                        state.hour = IntlUtil.NUMERIC;
                    }
                    state.hourCycle = hc;
                }

                if (bestPattern.contains("m")) {
                    state.minute = bestPattern.contains("mm") ? IntlUtil._2_DIGIT : IntlUtil.NUMERIC;
                }

                if (bestPattern.contains("s")) {
                    state.second = bestPattern.contains("ss") ? IntlUtil._2_DIGIT : IntlUtil.NUMERIC;
                }

                if (containsOneOf(bestPattern, "SA")) {
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

        Locale intervalFormatLocale = new Locale.Builder().setLocale(javaLocale).setUnicodeLocaleKeyword("hc", hc).build();
        try {
            state.dateIntervalFormat = DateIntervalFormat.getInstance(skeleton, intervalFormatLocale);
        } catch (IllegalArgumentException iaex) {
            // workaround for ICU-22202
            state.dateIntervalFormat = DateIntervalFormat.getInstance(patchSkeletonToAvoidICU22202(skeleton), intervalFormatLocale);
        }

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
        state.timeZone = timeZoneId;
        state.initialized = true;
    }

    // workaround for ICU-22202, replaces less common year-related parts
    // of the skeleton by the most common pattern symbol for year
    // and removes day-period-related pattern symbols
    private static String patchSkeletonToAvoidICU22202(String skeleton) {
        StringBuilder sb = new StringBuilder();
        for (char c : skeleton.toCharArray()) {
            switch (c) {
                case 'a':
                case 'b':
                case 'B':
                    continue;
                case 'Y':
                case 'u':
                case 'U':
                case 'r':
                    sb.append('y');
                    break;
                case 'L':
                    sb.append('M');
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
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
        boolean quoted = false;
        for (char c : pattern.toCharArray()) {
            if (c == '\'') {
                quoted = !quoted;
            } else if (!quoted) {
                switch (c) {
                    case 'K':
                        return IntlUtil.H11;
                    case 'h':
                        return IntlUtil.H12;
                    case 'H':
                        return IntlUtil.H23;
                    case 'k':
                        return IntlUtil.H24;
                }
            }
        }
        return null;
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
        boolean quoted = false;
        for (char c : pattern.toCharArray()) {
            if (c == '\'') {
                quoted = !quoted;
            } else if (!quoted) {
                switch (c) {
                    case 'K':
                    case 'h':
                    case 'H':
                    case 'k':
                        sb.append(replacement);
                        continue;
                }
            }
            sb.append(c);
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
                return "z";
            case IntlUtil.LONG:
                return "zzzz";
            case IntlUtil.SHORT_OFFSET:
                return "O";
            case IntlUtil.LONG_OFFSET:
                return "OOOO";
            case IntlUtil.SHORT_GENERIC:
                return "v";
            case IntlUtil.LONG_GENERIC:
                return "vvvv";
        }
        return "";
    }

    private static String makeSkeleton(String weekdayOpt, String eraOpt, String yearOpt, String monthOpt, String dayOpt, String dayPeriodOpt, String hourOpt, String hcOpt,
                    String minuteOpt, String secondOpt, int fractionalSecondDigitsOpt, String timeZoneNameOpt) {
        return weekdayOptToSkeleton(weekdayOpt) + eraOptToSkeleton(eraOpt) + yearOptToSkeleton(yearOpt) + monthOptToSkeleton(monthOpt) + dayOptToSkeleton(dayOpt) +
                        dayPeriodOptToSkeleton(dayPeriodOpt) + hourOptToSkeleton(hourOpt, hcOpt) + minuteOptToSkeleton(minuteOpt) + secondOptToSkeleton(secondOpt, fractionalSecondDigitsOpt) +
                        timeZoneNameOptToSkeleton(timeZoneNameOpt);
    }

    private static UnmodifiableEconomicMap<String, Pair<String, String>> initCanonicalTimeZoneIDMap() {
        CompilerAsserts.neverPartOfCompilation();
        EconomicMap<String, Pair<String, String>> map = EconomicMap.create();
        for (String available : TimeZone.getAvailableIDs()) {
            String canonical = TimeZone.getCanonicalID(available);
            if ("Etc/UTC".equals(canonical) || "Etc/GMT".equals(canonical)) {
                canonical = "UTC";
            }
            map.put(IntlUtil.toUpperCase(available), new Pair<>(available, canonical));
        }
        return map;
    }

    @TruffleBoundary
    public static Pair<String, String> getAvailableNamedTimeZoneIdentifier(String tzId) {
        String ucTzId = IntlUtil.toUpperCase(tzId);
        return canonicalTimeZoneIDMap.get().get(ucTzId);
    }

    private static boolean containsOneOf(String suspect, String containees) {
        for (int c : containees.getBytes()) {
            if (suspect.indexOf(c) > -1) {
                return true;
            }
        }
        return false;
    }

    public static DateFormat getDateFormatProperty(JSDateTimeFormatObject obj) {
        return obj.getInternalState().dateFormat;
    }

    private static String maybeReplaceNarrowSpaces(String formatted) {
        if (!JSRealm.getMain(null).getContextOptions().allowNarrowSpacesInDateFormat()) {
            return formatted.replace('\u202f', ' ');
        }
        return formatted;
    }

    @TruffleBoundary
    public static TruffleString format(JSDateTimeFormatObject numberFormatObj, Object n) {
        DateFormat dateFormat = getDateFormatProperty(numberFormatObj);
        String formatted = dateFormat.format(timeClip(n));
        formatted = maybeReplaceNarrowSpaces(formatted);
        return Strings.fromJavaString(formatted);
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
    public static JSDynamicObject formatToParts(JSContext context, JSRealm realm, JSDateTimeFormatObject numberFormatObj, Object n, String source) {

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

    private static DateIntervalFormat.FormattedDateInterval formatRangeImpl(JSDateTimeFormatObject dateTimeFormat, double startDate, double endDate) {
        InternalState state = dateTimeFormat.getInternalState();
        DateFormat dateFormat = state.dateFormat;
        Calendar calendar = dateFormat.getCalendar();
        Calendar fromCalendar = ((Calendar) calendar.clone());
        Calendar toCalendar = ((Calendar) calendar.clone());
        fromCalendar.setTimeInMillis((long) startDate);
        toCalendar.setTimeInMillis((long) endDate);
        return state.dateIntervalFormat.formatToValue(fromCalendar, toCalendar);
    }

    @TruffleBoundary
    public static TruffleString formatRange(JSDateTimeFormatObject dateTimeFormat, double startDate, double endDate) {
        DateIntervalFormat.FormattedDateInterval formattedRange = formatRangeImpl(dateTimeFormat, startDate, endDate);

        if (dateFieldsPracticallyEqual(formattedRange)) {
            return JSDateTimeFormat.format(dateTimeFormat, startDate);
        }

        return Strings.fromJavaString(formattedRange.toString());
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
    public static JSDynamicObject formatRangeToParts(JSContext context, JSRealm realm, JSDateTimeFormatObject dateTimeFormat, double startDate, double endDate) {
        DateIntervalFormat.FormattedDateInterval formattedRange = formatRangeImpl(dateTimeFormat, startDate, endDate);

        if (dateFieldsPracticallyEqual(formattedRange)) {
            return JSDateTimeFormat.formatToParts(context, realm, dateTimeFormat, startDate, IntlUtil.SHARED);
        }

        String formattedString = formattedRange.toString();
        String digits = null;

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
                    throw Errors.shouldNotReachHereUnexpectedValue(fieldValue);
                }
            } else if (field instanceof DateFormat.Field) {
                String value = formattedString.substring(start, limit);
                String type;
                if (field == DateFormat.Field.YEAR) {
                    // DateFormat.Field.YEAR covers both "year" and "yearName"
                    if (digits == null) {
                        String numberingSystem = dateTimeFormat.getInternalState().numberingSystem;
                        digits = NumberingSystem.getInstanceByName(numberingSystem).getDescription();
                    }
                    boolean year = (value.length() > 0) && (digits.indexOf(value.charAt(0)) != -1);
                    type = year ? IntlUtil.YEAR : IntlUtil.YEAR_NAME;
                } else {
                    type = fieldToType((DateFormat.Field) field);
                }
                String source = IntlUtil.sourceString(start, limit, startRangeStart, startRangeLimit, endRangeStart, endRangeLimit);
                parts.add(makePart(context, realm, type, value, source));
                lastLimit = limit;
            } else {
                throw Errors.shouldNotReachHereUnexpectedValue(field);
            }
        }

        int length = formattedString.length();
        if (lastLimit < length) { // Literal at the end
            String literal = formattedString.substring(lastLimit, length);
            String source = IntlUtil.sourceString(lastLimit, length, startRangeStart, startRangeLimit, endRangeStart, endRangeLimit);
            parts.add(makePart(context, realm, IntlUtil.LITERAL, literal, source));
        }

        return JSArray.createConstant(context, realm, parts.toArray());
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
        JSObject p = JSOrdinary.create(context, realm);
        JSObject.set(p, IntlUtil.KEY_TYPE, Strings.fromJavaString(type));
        JSObject.set(p, IntlUtil.KEY_VALUE, Strings.fromJavaString(value));
        if (source != null) {
            JSObject.set(p, IntlUtil.KEY_SOURCE, Strings.fromJavaString(source));
        }
        return p;
    }

    public static class InternalState {

        private boolean initialized = false;
        private DateFormat dateFormat;
        private DateIntervalFormat dateIntervalFormat;

        private JSDynamicObject boundFormatFunction = null;

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

        JSObject toResolvedOptionsObject(JSContext context, JSRealm realm) {
            JSObject result = JSOrdinary.create(context, realm);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_LOCALE, Strings.fromJavaString(locale), JSAttributes.getDefault());
            if (calendar != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_CALENDAR, Strings.fromJavaString(calendar), JSAttributes.getDefault());
            }
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_NUMBERING_SYSTEM, Strings.fromJavaString(numberingSystem), JSAttributes.getDefault());
            if (timeZone != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_TIME_ZONE, Strings.fromJavaString(timeZone), JSAttributes.getDefault());
            }
            if (hourCycle != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_HOUR_CYCLE, Strings.fromJavaString(hourCycle), JSAttributes.getDefault());
                boolean hour12 = IntlUtil.H11.equals(hourCycle) || IntlUtil.H12.equals(hourCycle);
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_HOUR12, hour12, JSAttributes.getDefault());
            }
            if (weekday != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_WEEKDAY, Strings.fromJavaString(weekday), JSAttributes.getDefault());
            }
            if (era != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_ERA, Strings.fromJavaString(era), JSAttributes.getDefault());
            }
            if (year != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_YEAR, Strings.fromJavaString(year), JSAttributes.getDefault());
            }
            if (month != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_MONTH, Strings.fromJavaString(month), JSAttributes.getDefault());
            }
            if (day != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_DAY, Strings.fromJavaString(day), JSAttributes.getDefault());
            }
            if (dayPeriod != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_DAY_PERIOD, Strings.fromJavaString(dayPeriod), JSAttributes.getDefault());
            }
            if (hour != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_HOUR, Strings.fromJavaString(hour), JSAttributes.getDefault());
            }
            if (minute != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_MINUTE, Strings.fromJavaString(minute), JSAttributes.getDefault());
            }
            if (second != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_SECOND, Strings.fromJavaString(second), JSAttributes.getDefault());
            }
            if (fractionalSecondDigits != 0) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_FRACTIONAL_SECOND_DIGITS, fractionalSecondDigits, JSAttributes.getDefault());
            }
            if (timeZoneName != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_TIME_ZONE_NAME, Strings.fromJavaString(timeZoneName), JSAttributes.getDefault());
            }
            if (dateStyle != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_DATE_STYLE, Strings.fromJavaString(dateStyle), JSAttributes.getDefault());
            }
            if (timeStyle != null) {
                JSObjectUtil.putDataProperty(result, IntlUtil.KEY_TIME_STYLE, Strings.fromJavaString(timeStyle), JSAttributes.getDefault());
            }
            return result;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public JSDynamicObject getBoundFormatFunction() {
            return boundFormatFunction;
        }

        public void setBoundFormatFunction(JSDynamicObject boundFormatFunction) {
            this.boundFormatFunction = boundFormatFunction;
        }
    }

    @TruffleBoundary
    public static JSObject resolvedOptions(JSContext context, JSRealm realm, JSDateTimeFormatObject dateTimeFormatObj) {
        InternalState state = dateTimeFormatObj.getInternalState();
        return state.toResolvedOptionsObject(context, realm);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getDateTimeFormatPrototype();
    }
}
