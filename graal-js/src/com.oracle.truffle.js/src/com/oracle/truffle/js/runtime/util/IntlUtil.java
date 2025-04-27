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
package com.oracle.truffle.js.runtime.util;

import java.time.ZoneId;
import java.time.zone.ZoneRulesProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;

import org.graalvm.shadowed.com.ibm.icu.text.CaseMap;
import org.graalvm.shadowed.com.ibm.icu.text.CaseMap.Lower;
import org.graalvm.shadowed.com.ibm.icu.text.CaseMap.Upper;
import org.graalvm.shadowed.com.ibm.icu.text.Collator;
import org.graalvm.shadowed.com.ibm.icu.text.CurrencyMetaInfo;
import org.graalvm.shadowed.com.ibm.icu.text.DateFormat;
import org.graalvm.shadowed.com.ibm.icu.text.NumberingSystem;
import org.graalvm.shadowed.com.ibm.icu.util.Calendar;
import org.graalvm.shadowed.com.ibm.icu.util.TimeZone;
import org.graalvm.shadowed.com.ibm.icu.util.ULocale;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 *
 * ECMA 402 Utilities.
 */
public final class IntlUtil {

    private IntlUtil() {
        // should not be constructed
    }

    public static final String _2_DIGIT = "2-digit";
    public static final String ACCENT = "accent";
    public static final String ACCOUNTING = "accounting";
    public static final String ALWAYS = "always";
    public static final String AUTO = "auto";
    public static final String BEST_FIT = "best fit";
    public static final String BASE = "base";
    public static final String BASIC = "basic";
    public static final String CALENDAR = "calendar";
    public static final String CARDINAL = "cardinal";
    public static final String CASE = "case";
    public static final String CASE_FIRST = "caseFirst";
    public static final String CEIL = "ceil";
    public static final String CODE = "code";
    public static final String COLLATION = "collation";
    public static final String COMPACT = "compact";
    public static final String COMPACT_DISPLAY = "compactDisplay";
    public static final String CONJUNCTION = "conjunction";
    public static final String CURRENCY = "currency";
    public static final String CURRENCY_DISPLAY = "currencyDisplay";
    public static final String CURRENCY_SIGN = "currencySign";
    public static final String DATE_STYLE = "dateStyle";
    public static final String DATE_TIME_FIELD = "dateTimeField";
    public static final String DAY = "day";
    public static final String DAYS = "days";
    public static final String DAYS_DISPLAY = "daysDisplay";
    public static final String DAY_PERIOD = "dayPeriod";
    public static final String DEFAULT = "default";
    public static final String DECIMAL = "decimal";
    public static final String DIALECT = "dialect";
    public static final String DIGITAL = "digital";
    public static final String DIRECTION = "direction";
    public static final String DISJUNCTION = "disjunction";
    public static final String ELEMENT = "element";
    public static final String END_RANGE = "endRange";
    public static final String ENGINEERING = "engineering";
    public static final String ERA = "era";
    public static final String EXCEPT_ZERO = "exceptZero";
    public static final String EXPAND = "expand";
    public static final String FALLBACK = "fallback";
    public static final String FALSE = "false";
    public static final String FIRST_DAY = "firstDay";
    public static final String FIRST_DAY_OF_WEEK = "firstDayOfWeek";
    public static final String FLOOR = "floor";
    public static final String FORMAT_MATCHER = "formatMatcher";
    public static final String FRACTIONAL = "fractional";
    public static final String FRACTIONAL_DIGITS = "fractionalDigits";
    public static final String FRACTIONAL_SECOND_DIGITS = "fractionalSecondDigits";
    public static final String FRACTION_DIGITS = "fractionDigits";
    public static final String FULL = "full";
    public static final String GRANULARITY = "granularity";
    public static final String GRAPHEME = "grapheme";
    public static final String H11 = "h11";
    public static final String H12 = "h12";
    public static final String H23 = "h23";
    public static final String H24 = "h24";
    public static final String HALF_CEIL = "halfCeil";
    public static final String HALF_EVEN = "halfEven";
    public static final String HALF_EXPAND = "halfExpand";
    public static final String HALF_FLOOR = "halfFloor";
    public static final String HALF_TRUNC = "halfTrunc";
    public static final String HOUR = "hour";
    public static final String HOURS = "hours";
    public static final String HOURS_DISPLAY = "hoursDisplay";
    public static final String HOUR_CYCLE = "hourCycle";
    public static final String HOUR12 = "hour12";
    public static final String INDEX = "index";
    public static final String INFINITY = "infinity";
    public static final String INPUT = "input";
    public static final String INTEGER = "integer";
    public static final String IS_WORD_LIKE = "isWordLike";
    public static final String IGNORE_PUNCTUATION = "ignorePunctuation";
    public static final String LANGUAGE = "language";
    public static final String LANGUAGE_DISPLAY = "languageDisplay";
    public static final String LESS_PRECISION = "lessPrecision";
    public static final String LITERAL = "literal";
    public static final String LOCALE = "locale";
    public static final String LOCALE_MATCHER = "localeMatcher";
    public static final String LONG = "long";
    public static final String LONG_GENERIC = "longGeneric";
    public static final String LONG_OFFSET = "longOffset";
    public static final String LOOKUP = "lookup";
    public static final String LOWER = "lower";
    public static final String LTR = "ltr";
    public static final String MAXIMUM_FRACTION_DIGITS = "maximumFractionDigits";
    public static final String MAXIMUM_SIGNIFICANT_DIGITS = "maximumSignificantDigits";
    public static final String MEDIUM = "medium";
    public static final String MICROSECONDS = "microseconds";
    public static final String MICROSECONDS_DISPLAY = "microsecondsDisplay";
    public static final String MILLISECONDS = "milliseconds";
    public static final String MILLISECONDS_DISPLAY = "millisecondsDisplay";
    public static final String MIN2 = "min2";
    public static final String MINIMAL_DAYS = "minimalDays";
    public static final String MINIMUM_FRACTION_DIGITS = "minimumFractionDigits";
    public static final String MINIMUM_INTEGER_DIGITS = "minimumIntegerDigits";
    public static final String MINIMUM_SIGNIFICANT_DIGITS = "minimumSignificantDigits";
    public static final String MINUS_SIGN = "minusSign";
    public static final String MINUTE = "minute";
    public static final String MINUTES = "minutes";
    public static final String MINUTES_DISPLAY = "minutesDisplay";
    public static final String MONTH = "month";
    public static final String MONTHS = "months";
    public static final String MONTHS_DISPLAY = "monthsDisplay";
    public static final String MORE_PRECISION = "morePrecision";
    public static final String NAME = "name";
    public static final String NANOSECONDS = "nanoseconds";
    public static final String NANOSECONDS_DISPLAY = "nanosecondsDisplay";
    public static final String NARROW = "narrow";
    public static final String NARROW_SYMBOL = "narrowSymbol";
    public static final String NEGATIVE = "negative";
    public static final String NEVER = "never";
    public static final String NEXT = "next";
    public static final String NONE = "none";
    public static final String NOTATION = "notation";
    public static final String NUMERIC = "numeric";
    public static final String NUMBERING_SYSTEM = "numberingSystem";
    public static final String ORDINAL = "ordinal";
    public static final String PERCENT = "percent";
    public static final String PREVIOUS = "previous";
    public static final String PLURAL_CATEGORIES = "pluralCategories";
    public static final String PLUS_SIGN = "plusSign";
    public static final String QUARTER = "quarter";
    public static final String REGION = "region";
    public static final String ROUNDING_INCREMENT = "roundingIncrement";
    public static final String ROUNDING_MODE = "roundingMode";
    public static final String ROUNDING_PRIORITY = "roundingPriority";
    public static final String RTL = "rtl";
    public static final String SCIENTIFIC = "scientific";
    public static final String SCRIPT = "script";
    public static final String SEARCH = "search";
    public static final String SECOND = "second";
    public static final String SECONDS = "seconds";
    public static final String SECONDS_DISPLAY = "secondsDisplay";
    public static final String SEGMENT = "segment";
    public static final String SENTENCE = "sentence";
    public static final String SENSITIVITY = "sensitivity";
    public static final String SHARED = "shared";
    public static final String SHORT = "short";
    public static final String SHORT_GENERIC = "shortGeneric";
    public static final String SHORT_OFFSET = "shortOffset";
    public static final String SIGNIFICANT_DIGITS = "significantDigits";
    public static final String SIGN_DISPLAY = "signDisplay";
    public static final String SORT = "sort";
    public static final String SOURCE = "source";
    public static final String STANDARD = "standard";
    public static final String START_RANGE = "startRange";
    public static final String STRICT = "strict";
    public static final String STRIP_IF_INTEGER = "stripIfInteger";
    public static final String STYLE = "style";
    public static final String SYMBOL = "symbol";
    public static final String TIME_STYLE = "timeStyle";
    public static final String TIME_ZONE = "timeZone";
    public static final String TIME_ZONE_NAME = "timeZoneName";
    public static final String TRAILING_ZERO_DISPLAY = "trailingZeroDisplay";
    public static final String TRUNC = "trunc";
    public static final String TYPE = "type";
    public static final String UND = "und";
    public static final String UNIT = "unit";
    public static final String UNIT_DISPLAY = "unitDisplay";
    public static final String UPPER = "upper";
    public static final String USAGE = "usage";
    public static final String USE_GROUPING = "useGrouping";
    public static final String VALUE = "value";
    public static final String VARIANT = "variant";
    public static final String WORD = "word";
    public static final String WEEKDAY = "weekday";
    public static final String WEEKEND = "weekend";
    public static final String WEEKS = "weeks";
    public static final String WEEKS_DISPLAY = "weeksDisplay";
    public static final String WEEK_OF_YEAR = "weekOfYear";
    public static final String YEAR = "year";
    public static final String YEARS = "years";
    public static final String YEARS_DISPLAY = "yearsDisplay";
    public static final String YEAR_NAME = "yearName";

    public static final TruffleString KEY_CALENDAR = Strings.CALENDAR;
    public static final TruffleString KEY_CASE_FIRST = Strings.constant(CASE_FIRST);
    public static final TruffleString KEY_COLLATION = Strings.constant(COLLATION);
    public static final TruffleString KEY_COMPACT_DISPLAY = Strings.constant(COMPACT_DISPLAY);
    public static final TruffleString KEY_CURRENCY = Strings.constant(CURRENCY);
    public static final TruffleString KEY_CURRENCY_DISPLAY = Strings.constant(CURRENCY_DISPLAY);
    public static final TruffleString KEY_CURRENCY_SIGN = Strings.constant(CURRENCY_SIGN);
    public static final TruffleString KEY_DATE_STYLE = Strings.constant(DATE_STYLE);
    public static final TruffleString KEY_DAY = Strings.DAY;
    public static final TruffleString KEY_DAYS = Strings.constant(DAYS);
    public static final TruffleString KEY_DAYS_DISPLAY = Strings.constant(DAYS_DISPLAY);
    public static final TruffleString KEY_DAY_PERIOD = Strings.constant(DAY_PERIOD);
    public static final TruffleString KEY_DIRECTION = Strings.constant(DIRECTION);
    public static final TruffleString KEY_ERA = Strings.ERA;
    public static final TruffleString KEY_FALLBACK = Strings.constant(FALLBACK);
    public static final TruffleString KEY_FIRST_DAY = Strings.constant(FIRST_DAY);
    public static final TruffleString KEY_FIRST_DAY_OF_WEEK = Strings.constant(FIRST_DAY_OF_WEEK);
    public static final TruffleString KEY_FORMAT_MATCHER = Strings.constant(FORMAT_MATCHER);
    public static final TruffleString KEY_FRACTIONAL_DIGITS = Strings.constant(FRACTIONAL_DIGITS);
    public static final TruffleString KEY_FRACTIONAL_SECOND_DIGITS = Strings.FRACTIONAL_SECOND_DIGITS;
    public static final TruffleString KEY_GRANULARITY = Strings.constant(GRANULARITY);
    public static final TruffleString KEY_HOUR = Strings.HOUR;
    public static final TruffleString KEY_HOURS = Strings.constant(HOURS);
    public static final TruffleString KEY_HOURS_DISPLAY = Strings.constant(HOURS_DISPLAY);
    public static final TruffleString KEY_HOUR_CYCLE = Strings.constant(HOUR_CYCLE);
    public static final TruffleString KEY_HOUR12 = Strings.constant(HOUR12);
    public static final TruffleString KEY_INDEX = Strings.INDEX;
    public static final TruffleString KEY_INPUT = Strings.INPUT;
    public static final TruffleString KEY_IS_WORD_LIKE = Strings.constant(IS_WORD_LIKE);
    public static final TruffleString KEY_IGNORE_PUNCTUATION = Strings.constant(IGNORE_PUNCTUATION);
    public static final TruffleString KEY_LANGUAGE = Strings.LANGUAGE;
    public static final TruffleString KEY_LANGUAGE_DISPLAY = Strings.constant(LANGUAGE_DISPLAY);
    public static final TruffleString KEY_LOCALE = Strings.constant(LOCALE);
    public static final TruffleString KEY_LOCALE_MATCHER = Strings.constant(LOCALE_MATCHER);
    public static final TruffleString KEY_LTR = Strings.constant(LTR);
    public static final TruffleString KEY_MAXIMUM_FRACTION_DIGITS = Strings.constant(MAXIMUM_FRACTION_DIGITS);
    public static final TruffleString KEY_MAXIMUM_SIGNIFICANT_DIGITS = Strings.constant(MAXIMUM_SIGNIFICANT_DIGITS);
    public static final TruffleString KEY_MICROSECONDS = Strings.constant(MICROSECONDS);
    public static final TruffleString KEY_MICROSECONDS_DISPLAY = Strings.constant(MICROSECONDS_DISPLAY);
    public static final TruffleString KEY_MILLISECONDS = Strings.constant(MILLISECONDS);
    public static final TruffleString KEY_MILLISECONDS_DISPLAY = Strings.constant(MILLISECONDS_DISPLAY);
    public static final TruffleString KEY_MINIMAL_DAYS = Strings.constant(MINIMAL_DAYS);
    public static final TruffleString KEY_MINIMUM_FRACTION_DIGITS = Strings.constant(MINIMUM_FRACTION_DIGITS);
    public static final TruffleString KEY_MINIMUM_INTEGER_DIGITS = Strings.constant(MINIMUM_INTEGER_DIGITS);
    public static final TruffleString KEY_MINIMUM_SIGNIFICANT_DIGITS = Strings.constant(MINIMUM_SIGNIFICANT_DIGITS);
    public static final TruffleString KEY_MINUTE = Strings.MINUTE;
    public static final TruffleString KEY_MINUTES = Strings.constant(MINUTES);
    public static final TruffleString KEY_MINUTES_DISPLAY = Strings.constant(MINUTES_DISPLAY);
    public static final TruffleString KEY_MONTH = Strings.MONTH;
    public static final TruffleString KEY_MONTHS = Strings.constant(MONTHS);
    public static final TruffleString KEY_MONTHS_DISPLAY = Strings.constant(MONTHS_DISPLAY);
    public static final TruffleString KEY_NANOSECONDS = Strings.constant(NANOSECONDS);
    public static final TruffleString KEY_NANOSECONDS_DISPLAY = Strings.constant(NANOSECONDS_DISPLAY);
    public static final TruffleString KEY_NOTATION = Strings.constant(NOTATION);
    public static final TruffleString KEY_NUMERIC = Strings.constant(NUMERIC);
    public static final TruffleString KEY_NUMBERING_SYSTEM = Strings.constant(NUMBERING_SYSTEM);
    public static final TruffleString KEY_PLURAL_CATEGORIES = Strings.constant(PLURAL_CATEGORIES);
    public static final TruffleString KEY_REGION = Strings.constant(REGION);
    public static final TruffleString KEY_ROUNDING_INCREMENT = Strings.ROUNDING_INCREMENT;
    public static final TruffleString KEY_ROUNDING_MODE = Strings.ROUNDING_MODE;
    public static final TruffleString KEY_ROUNDING_PRIORITY = Strings.constant(ROUNDING_PRIORITY);
    public static final TruffleString KEY_RTL = Strings.constant(RTL);
    public static final TruffleString KEY_SCRIPT = Strings.SCRIPT;
    public static final TruffleString KEY_SECOND = Strings.SECOND;
    public static final TruffleString KEY_SECONDS = Strings.constant(SECONDS);
    public static final TruffleString KEY_SECONDS_DISPLAY = Strings.constant(SECONDS_DISPLAY);
    public static final TruffleString KEY_SEGMENT = Strings.constant(SEGMENT);
    public static final TruffleString KEY_SENSITIVITY = Strings.constant(SENSITIVITY);
    public static final TruffleString KEY_SIGN_DISPLAY = Strings.constant(SIGN_DISPLAY);
    public static final TruffleString KEY_SOURCE = Strings.SOURCE;
    public static final TruffleString KEY_STYLE = Strings.constant(STYLE);
    public static final TruffleString KEY_TIME_STYLE = Strings.constant(TIME_STYLE);
    public static final TruffleString KEY_TIME_ZONE = Strings.TIME_ZONE;
    public static final TruffleString KEY_TIME_ZONE_NAME = Strings.TIME_ZONE_NAME;
    public static final TruffleString KEY_TRAILING_ZERO_DISPLAY = Strings.constant(TRAILING_ZERO_DISPLAY);
    public static final TruffleString KEY_TYPE = Strings.TYPE;
    public static final TruffleString KEY_UNIT = Strings.UNIT;
    public static final TruffleString KEY_UNIT_DISPLAY = Strings.constant(UNIT_DISPLAY);
    public static final TruffleString KEY_USAGE = Strings.constant(USAGE);
    public static final TruffleString KEY_USE_GROUPING = Strings.constant(USE_GROUPING);
    public static final TruffleString KEY_VALUE = Strings.VALUE;
    public static final TruffleString KEY_WEEKDAY = Strings.constant(WEEKDAY);
    public static final TruffleString KEY_WEEKEND = Strings.constant(WEEKEND);
    public static final TruffleString KEY_WEEKS = Strings.constant(WEEKS);
    public static final TruffleString KEY_WEEKS_DISPLAY = Strings.constant(WEEKS_DISPLAY);
    public static final TruffleString KEY_YEAR = Strings.YEAR;
    public static final TruffleString KEY_YEARS = Strings.constant(YEARS);
    public static final TruffleString KEY_YEARS_DISPLAY = Strings.constant(YEARS_DISPLAY);

    // https://tc39.es/ecma402/#table-sanctioned-simple-unit-identifiers
    private static final Set<String> SANCTIONED_SIMPLE_UNIT_IDENTIFIERS = Set.of(new String[]{
                    "acre",
                    "bit",
                    "byte",
                    "celsius",
                    "centimeter",
                    "day",
                    "degree",
                    "fahrenheit",
                    "fluid-ounce",
                    "foot",
                    "gallon",
                    "gigabit",
                    "gigabyte",
                    "gram",
                    "hectare",
                    "hour",
                    "inch",
                    "kilobit",
                    "kilobyte",
                    "kilogram",
                    "kilometer",
                    "liter",
                    "megabit",
                    "megabyte",
                    "meter",
                    "microsecond",
                    "mile",
                    "mile-scandinavian",
                    "milliliter",
                    "millimeter",
                    "millisecond",
                    "minute",
                    "month",
                    "nanosecond",
                    "ounce",
                    "percent",
                    "petabyte",
                    "pound",
                    "second",
                    "stone",
                    "terabit",
                    "terabyte",
                    "week",
                    "yard",
                    "year"
    });

    public static Locale selectedLocale(JSContext ctx, String[] locales) {
        // We don't distinguish BestFitMatcher and LookupMatcher i.e.
        // the implementation dependent BestFitMatcher is implemented as LookupMatcher
        return lookupMatcher(ctx, locales);
    }

    public static Locale bestAvailableLocale(JSContext context, Locale locale) {
        Locale candidate = locale;
        while (true) {
            if (isAvailableLocale(context, candidate)) {
                return candidate;
            }
            String candidateLanguageTag = candidate.toLanguageTag();
            int pos = candidateLanguageTag.lastIndexOf('-');
            if (pos == -1) {
                return null;
            } else {
                if (pos >= 2 && candidateLanguageTag.charAt(pos - 2) == '-') {
                    pos -= 2;
                }
                candidateLanguageTag = candidateLanguageTag.substring(0, pos);
                candidate = Locale.forLanguageTag(candidateLanguageTag);
            }
        }
    }

    @TruffleBoundary
    public static Locale lookupMatcher(JSContext ctx, String[] requestedLocales) {
        for (String locale : requestedLocales) {
            Locale requestedLocale = Locale.forLanguageTag(locale);
            Locale noExtensionsLocale = requestedLocale.stripExtensions();
            Locale availableLocale = bestAvailableLocale(ctx, noExtensionsLocale);
            if (availableLocale != null) {
                String unicodeExtension = requestedLocale.getExtension('u');
                if (unicodeExtension != null) {
                    availableLocale = new Locale.Builder().setLocale(availableLocale).setExtension('u', unicodeExtension).build();
                }
                return availableLocale;
            }
        }
        return ctx.getLocale();
    }

    @TruffleBoundary
    public static List<Object> supportedLocales(JSContext ctx, String[] requestedLocales, @SuppressWarnings("unused") String matcher) {
        List<Object> result = new ArrayList<>();
        for (String locale : requestedLocales) {
            Locale noExtensionsLocale = Locale.forLanguageTag(locale).stripExtensions();
            Locale availableLocale = bestAvailableLocale(ctx, noExtensionsLocale);
            if (availableLocale != null) {
                result.add(Strings.fromJavaString(locale));
            }
        }
        return result;
    }

    private static boolean isAvailableLocale(JSContext ctx, Locale locale) {
        // default locale might be missing in the available locale list
        return getAvailableLocales().contains(locale) || ctx.getLocale().equals(locale);
    }

    private static final LazyValue<Set<Locale>> AVAILABLE_LOCALES = new LazyValue<>(IntlUtil::initAvailableLocales);

    private static Set<Locale> getAvailableLocales() {
        return AVAILABLE_LOCALES.get();
    }

    private static Set<Locale> initAvailableLocales() {
        Set<Locale> result = new HashSet<>();

        try {
            for (ULocale ul : ULocale.getAvailableLocales()) {
                result.add(ul.toLocale());
                if (!ul.getScript().isEmpty()) {
                    // Add also a version without the script subtag
                    result.add(new Locale.Builder().setLanguage(ul.getLanguage()).setRegion(ul.getCountry()).build());
                }
            }
        } catch (MissingResourceException e) {
            throw Errors.createICU4JDataError(e);
        }

        return Set.of(result.toArray(new Locale[result.size()]));
    }

    public static boolean isValidNumberingSystem(String numberingSystem) {
        return Arrays.asList(NumberingSystem.getAvailableNames()).contains(numberingSystem);
    }

    public static String defaultNumberingSystemName(JSContext context, Locale locale) {
        if (context.isOptionV8CompatibilityMode() && "ar".equals(locale.toLanguageTag())) {
            // https://chromium.googlesource.com/chromium/deps/icu/+/6cca29a092eef02178e13b7461fe8fbf3021d04b
            // V8 is using a patched version of ICU (where the default numbering system for "ar"
            // locale is "latn")
            return "latn";
        }
        return NumberingSystem.getInstance(locale).getName();
    }

    public static void validateUnicodeLocaleIdentifierType(String type, BranchProfile errorBranch) {
        if (!UTS35Validator.isStructurallyValidType(type)) {
            errorBranch.enter();
            throw Errors.createRangeErrorFormat("Invalid option: %s", null, type);
        }
    }

    @TruffleBoundary
    public static String normalizeUnicodeLocaleIdentifierType(String type) {
        return type.toLowerCase();
    }

    public static boolean isWellFormedCurrencyCode(String currency) {
        return currency.length() == 3 && UTS35Validator.isAlpha(currency.charAt(0)) && UTS35Validator.isAlpha(currency.charAt(1)) && UTS35Validator.isAlpha(currency.charAt(2));
    }

    public static void ensureIsWellFormedCurrencyCode(String currency) {
        if (!isWellFormedCurrencyCode(currency)) {
            throw Errors.createRangeErrorCurrencyNotWellFormed(currency);
        }
    }

    public static void ensureIsStructurallyValidLanguageId(String languageId) {
        if (!UTS35Validator.isStructurallyValidLanguageId(languageId)) {
            throw Errors.createRangeErrorInvalidLanguageId(languageId);
        }
    }

    public static void ensureIsStructurallyValidLanguageSubtag(String region) {
        if (!UTS35Validator.isStructurallyValidLanguageSubtag(region)) {
            throw Errors.createRangeErrorInvalidLanguageSubtag(region);
        }
    }

    public static void ensureIsStructurallyValidRegionSubtag(String region) {
        if (!UTS35Validator.isStructurallyValidRegionSubtag(region)) {
            throw Errors.createRangeErrorInvalidRegion(region);
        }
    }

    public static void ensureIsStructurallyValidScriptSubtag(String script) {
        if (!UTS35Validator.isStructurallyValidScriptSubtag(script)) {
            throw Errors.createRangeErrorInvalidScript(script);
        }
    }

    public static void ensureIsStructurallyValidCalendar(String calendar) {
        if (!UTS35Validator.isStructurallyValidType(calendar)) {
            throw Errors.createRangeErrorInvalidCalendar(calendar);
        }
    }

    // Placeholders used to work around incorrect modifications of local extensions
    // performed by ULocale.Builder
    private static final String YES_PLACEHOLDER = "yes31415";
    private static final String TRUE_PLACEHOLDER = "true2718";

    @TruffleBoundary
    public static String validateAndCanonicalizeLanguageTag(String languageTag) {
        // We cannot use (U)Locale class to check whether the tag is well-formed.
        // Locale class allows wider range of tags (irregular grandfathered tags,
        // extlang subtags, private use only tags etc.)
        if (!UTS35Validator.isWellFormedUnicodeBCP47LocaleIdentifier(languageTag)) {
            throw Errors.createRangeErrorFormat("Language tag is not well-formed: %s", null, languageTag);
        }

        return canonicalizeLanguageTag(languageTag);
    }

    // https://tc39.github.io/ecma402/#sec-canonicalizelanguagetag
    @TruffleBoundary
    public static String canonicalizeLanguageTag(String languageTag) {
        try {
            ULocale locale = ULocale.createCanonical(ULocale.getName(languageTag));
            ULocale.Builder builder = new ULocale.Builder().setLocale(locale);

            String variant = locale.getVariant();
            if (variant.indexOf('_') != -1 || variant.indexOf('-') != -1) {
                String[] variants = variant.split("[_-]");
                if (new HashSet<>(Arrays.asList(variants)).size() != variants.length) {
                    throw Errors.createRangeErrorFormat("Language tag with duplicate variants: %s", null, languageTag);
                }
                // Canonicalization is supposed to sort variants but (U)Locale fails to do so.
                Arrays.sort(variants);
                StringBuilder sb = new StringBuilder(variants[0]);
                for (int i = 1; i < variants.length; i++) {
                    sb.append('-').append(variants[i]);
                }
                builder.setVariant(sb.toString());
            }

            Set<Character> extensions = locale.getExtensionKeys();
            if (!extensions.isEmpty()) {
                // Singletons are case-insensitive.
                String tag = languageTag.toLowerCase();
                int privateExtIdx = extensions.contains('x') ? tag.indexOf("-x-") : tag.length();
                for (Character ext : extensions) {
                    if (ext != 'x') {
                        String extDelimiter = "-" + ext + "-";
                        int idx = tag.indexOf(extDelimiter);
                        int idx2 = tag.indexOf(extDelimiter, idx + 1);
                        if (idx2 != -1 && idx2 < privateExtIdx) {
                            throw Errors.createRangeErrorFormat("Language tag with duplicate singletons: %s", null, languageTag);
                        }
                    }
                }

                Locale loc = new Locale.Builder().setLanguageTag(languageTag).build();
                for (String key : loc.getUnicodeLocaleKeys()) {
                    String type = loc.getUnicodeLocaleType(key);
                    if ("yes".equals(type)) {
                        if (!("kb".equals(key) || "kc".equals(key) || "kh".equals(key) || "kk".equals(key) || "kn".equals(key))) {
                            type = YES_PLACEHOLDER;
                        }
                    }
                    if ("rg".equals(key) || "sd".equals(key)) {
                        type = normalizeRGType(type);
                    }
                    builder.setUnicodeLocaleKeyword(key, type);
                }

                // Validate and canonicalize the transformed extension.
                // We cannot start with locale.getExtension('t') here. locale is
                // canonicalized and ICU started (ICU-21406) to remove duplicate
                // variants during canonicalization. Unfortunately, we have
                // to detect the duplicates because ECMAScript specification
                // refuses such locales as invalid explicitly =>
                // we start with a non-canonicalized locale here.
                String transformedExt = new ULocale(languageTag).getExtension('t');
                if (transformedExt != null) {
                    builder.setExtension('t', normalizeTransformedExtension(transformedExt));
                }
            }
            String result = maybeAppendMissingLanguageSubTag(builder.build().toLanguageTag());
            return result.replaceAll("-" + YES_PLACEHOLDER, "-yes").replaceAll("-" + TRUE_PLACEHOLDER, "-true");
        } catch (IllformedLocaleException e) {
            throw Errors.createRangeError(e.getMessage());
        }
    }

    public static String normalizeCAType(String type) {
        // (Preferred) aliases from
        // https://github.com/unicode-org/cldr/blob/master/common/bcp47/calendar.xml
        if ("gregorian".equals(type)) {
            return "gregory";
        } else if ("ethiopic-amete-alem".equals(type)) {
            return "ethioaa";
        } else if ("islamicc".equals(type)) {
            return "islamic-civil";
        }
        return type;
    }

    private static String normalizeRGType(String type) {
        if ("cn11".equals(type)) {
            return "cnbj";
        } else if ("cz10a".equals(type)) {
            return "cz110";
        } else if ("fra".equals(type) || "frg".equals(type)) {
            return "frges";
        } else if ("lud".equals(type)) {
            return "lucl";
        } else if ("no23".equals(type)) {
            return "no50";
        }
        return type;
    }

    private static String normalizeTransformedExtension(String extension) {
        // Parse transformed extension
        String tlang = null;
        Map<String, String> fields = new TreeMap<>();
        boolean seenDash = true;
        String lastKey = null;
        int lastValueStart = -1;
        for (int i = 0; i < extension.length() - 1; i++) {
            if (seenDash && UTS35Validator.isAlpha(extension.charAt(i)) && UTS35Validator.isDigit(extension.charAt(i + 1)) && (i + 2 == extension.length() || extension.charAt(i + 2) == '-')) {
                if (lastKey == null) {
                    tlang = extension.substring(0, Math.max(0, i - 1));
                } else {
                    fields.put(lastKey, extension.substring(lastValueStart, i - 1));
                }
                lastKey = extension.substring(i, i + 2);
                lastValueStart = i + 3;
            }
            seenDash = (extension.charAt(i) == '-');
        }
        if (tlang == null) {
            tlang = extension;
        }
        if (lastKey != null) {
            fields.put(lastKey, extension.substring(lastValueStart));
        }

        StringBuilder normalized = new StringBuilder();

        // Canonicalize tlang
        if (!tlang.isEmpty()) {
            tlang = validateAndCanonicalizeLanguageTag(tlang);
            normalized.append(tlang);
        }

        // Canonicalize fields
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (normalized.length() != 0) {
                normalized.append('-');
            }
            String value = entry.getValue();
            if ("names".equalsIgnoreCase(value)) {
                value = "prprname";
            }
            if ("true".equals(value)) {
                value = TRUE_PLACEHOLDER;
            }
            normalized.append(entry.getKey()).append('-').append(value);
        }

        return normalized.toString();
    }

    public static String maybeAppendMissingLanguageSubTag(String tag) {
        // (U)Locale.Builder.build() tends to strip und- prefix even
        // in cases where it results in an invalid (private use only) language tag.
        return tag.startsWith("x-") ? ("und-" + tag) : tag;
    }

    @TruffleBoundary
    // https://tc39.github.io/ecma402/#sec-case-sensitivity-and-case-mapping
    public static String toUpperCase(String in) {
        StringBuilder result = new StringBuilder(in.length());
        for (int i = 0; i < in.length(); i++) {
            int c = in.codePointAt(i);
            if (c >= 'a' && c <= 'z') {
                result.append((char) (c - 32));
            } else {
                result.append((char) c);
            }
        }
        return result.toString();
    }

    @TruffleBoundary
    public static String toLowerCase(JSContext ctx, String s, String[] locales) {
        Locale strippedLocale = selectedLocaleStripped(ctx, locales);
        StringBuilder result = new StringBuilder();
        Lower tr = CaseMap.toLower();
        tr.apply(strippedLocale, s, result, null);
        return result.toString();
    }

    @TruffleBoundary
    public static String toUpperCase(JSContext ctx, String s, String[] locales) {
        Locale strippedLocale = selectedLocaleStripped(ctx, locales);
        StringBuilder result = new StringBuilder();
        Upper tr = CaseMap.toUpper();
        tr.apply(strippedLocale, s, result, null);
        return result.toString();
    }

    @TruffleBoundary
    public static Locale selectedLocaleStripped(JSContext ctx, String[] locales) {
        return IntlUtil.selectedLocale(ctx, locales).stripExtensions();
    }

    public static JSObject makePart(JSContext context, JSRealm realm, String type, String value) {
        return makePart(context, realm, type, value, null);
    }

    public static JSObject makePart(JSContext context, JSRealm realm, String type, String value, String unit) {
        return makePart(context, realm, type, value, unit, null);
    }

    public static JSObject makePart(JSContext context, JSRealm realm, String type, String value, String unit, String source) {
        JSObject p = JSOrdinary.create(context, realm);
        JSObject.set(p, KEY_TYPE, Strings.fromJavaString(type));
        JSObject.set(p, KEY_VALUE, Strings.fromJavaString(value));
        if (unit != null) {
            JSObject.set(p, KEY_UNIT, Strings.fromJavaString(unit));
        }
        if (source != null) {
            JSObject.set(p, IntlUtil.KEY_SOURCE, Strings.fromJavaString(source));
        }
        return p;
    }

    @TruffleBoundary
    private static boolean isSanctionedSimpleUnitIdentifier(String unitIdentifier) {
        return SANCTIONED_SIMPLE_UNIT_IDENTIFIERS.contains(unitIdentifier);
    }

    @TruffleBoundary
    private static boolean isWellFormedUnitIdentifier(String unitIdentifier) {
        if (isSanctionedSimpleUnitIdentifier(unitIdentifier)) {
            return true;
        }
        String per = "-per-";
        int index = unitIdentifier.indexOf(per);
        if (index == -1) {
            return false;
        }
        String numerator = unitIdentifier.substring(0, index);
        String denominator = unitIdentifier.substring(index + per.length());
        return isSanctionedSimpleUnitIdentifier(numerator) && isSanctionedSimpleUnitIdentifier(denominator);
    }

    public static void ensureIsWellFormedUnitIdentifier(String unitIdentifier) {
        if (!isWellFormedUnitIdentifier(unitIdentifier)) {
            throw Errors.createRangeErrorInvalidUnitIdentifier(unitIdentifier);
        }
    }

    @TruffleBoundary
    public static TimeZone getICUTimeZone(String tzId, JSContext context) {
        assert tzId != null;
        if (context.getLanguageOptions().zoneRulesBasedTimeZones()) {
            return new ZoneRulesBasedTimeZone(tzId, ZoneRulesProvider.getRules(tzId, false));
        } else {
            return TimeZone.getTimeZone(tzId);
        }
    }

    @TruffleBoundary
    public static TimeZone getICUTimeZone(ZoneId zoneId, JSContext context) {
        if (context.getLanguageOptions().zoneRulesBasedTimeZones()) {
            return new ZoneRulesBasedTimeZone(zoneId.getId(), zoneId.getRules());
        } else {
            return TimeZone.getTimeZone(toICUTimeZoneId(zoneId));
        }
    }

    @TruffleBoundary
    public static TimeZone getICUTimeZoneForOffset(String tzOffset) {
        assert tzOffset != null;
        return TimeZone.getTimeZone(prependGMT(tzOffset));
    }

    @TruffleBoundary
    private static String prependGMT(String tzId) {
        return "GMT" + tzId;
    }

    private static String toICUTimeZoneId(ZoneId zoneId) {
        String tzid = zoneId.getId();
        char c = tzid.charAt(0);
        if (c == '+' || c == '-') {
            tzid = "GMT" + tzid;
        } else if (c == 'Z' && tzid.length() == 1) {
            tzid = "UTC";
        } else if (tzid.startsWith("UTC")) {
            tzid = "GMT" + tzid.substring(3);
        } else if (tzid.startsWith("UT")) {
            tzid = "GMT" + tzid.substring(2);
        }
        return tzid;
    }

    public static String toJSHourCycle(DateFormat.HourCycle hourCycle) {
        switch (hourCycle) {
            case HOUR_CYCLE_11:
                return IntlUtil.H11;
            case HOUR_CYCLE_12:
                return IntlUtil.H12;
            case HOUR_CYCLE_23:
                return IntlUtil.H23;
            case HOUR_CYCLE_24:
                return IntlUtil.H24;
            default:
                throw Errors.shouldNotReachHereUnexpectedValue(hourCycle);
        }
    }

    @TruffleBoundary
    public static String[] availableCalendars(ULocale locale, boolean commonlyUsed) {
        String[] calendars = Calendar.getKeywordValuesForLocale(IntlUtil.CALENDAR, locale, commonlyUsed);

        int length = 0;
        for (String calendar : calendars) {
            // ICU4J returns "unknown" available calendar but it does not provide any useful data
            // for this calendar
            if (!"unknown".equals(calendar)) {
                calendars[length++] = IntlUtil.normalizeCAType(calendar);
            }
        }
        if (length != calendars.length) {
            String[] trimmed = new String[length];
            System.arraycopy(calendars, 0, trimmed, 0, length);
            calendars = trimmed;
        }

        return calendars;
    }

    @TruffleBoundary
    public static String[] availableCalendars() {
        String[] calendars = availableCalendars(ULocale.ROOT, false);
        Arrays.sort(calendars);
        return calendars;
    }

    @TruffleBoundary
    public static String canonicalizeCalendar(String id) {
        String lcID = id.toLowerCase(Locale.ROOT);
        if (Arrays.binarySearch(availableCalendars(), lcID) < 0) {
            throw Errors.createRangeErrorInvalidCalendar(id);
        }
        return normalizeCAType(lcID);
    }

    // The returned collations are supposed to be "lower case String values conforming to the
    // type sequence from UTS 35 Unicode Locale Identifier, section 3.2" =>
    // replacing non-conforming collations by their conforming aliases according to
    // https://github.com/unicode-org/cldr/blob/main/common/bcp47/collation.xml
    public static String normalizeCollation(String collation) {
        String normalizedCollation;
        switch (collation) {
            case "dictionary":
                normalizedCollation = "dict";
                break;
            case "gb2312han":
                normalizedCollation = "gb2312";
                break;
            case "phonebook":
                normalizedCollation = "phonebk";
                break;
            case "traditional":
                normalizedCollation = "trad";
                break;
            default:
                normalizedCollation = collation;
                break;
        }
        return normalizedCollation;
    }

    @TruffleBoundary
    public static String[] availableCollations(ULocale locale, boolean commonOnly) {
        String[] collations;
        if (locale == null) {
            collations = Collator.getKeywordValues(IntlUtil.COLLATION);
        } else {
            collations = Collator.getKeywordValuesForLocale(IntlUtil.COLLATION, locale, commonOnly);
        }

        int length = 0;
        for (String element : collations) {
            // The values "standard" and "search" must be excluded
            if (!IntlUtil.SEARCH.equals(element) && !IntlUtil.STANDARD.equals(element)) {
                collations[length++] = normalizeCollation(element);
            }
        }
        if (length != collations.length) {
            String[] trimmed = new String[length];
            System.arraycopy(collations, 0, trimmed, 0, length);
            collations = trimmed;
        }

        Arrays.sort(collations);

        return collations;
    }

    @TruffleBoundary
    public static String[] availableCollations() {
        return availableCollations(null, false);
    }

    @TruffleBoundary
    public static String[] availableCurrencies() {
        List<String> list = CurrencyMetaInfo.getInstance().currencies(CurrencyMetaInfo.CurrencyFilter.all());
        String[] currencies = list.toArray(new String[list.size()]);
        Arrays.sort(currencies);
        return currencies;
    }

    @TruffleBoundary
    public static String[] availableNumberingSystems(JSContext context) {
        String[] numberingSystems = NumberingSystem.getAvailableNames();

        if (context.isOptionV8CompatibilityMode()) {
            // V8 filters out algorithmic numbering systems because of Chrome
            int length = 0;
            for (String numberingSystem : numberingSystems) {
                if (!NumberingSystem.getInstanceByName(numberingSystem).isAlgorithmic()) {
                    numberingSystems[length++] = numberingSystem;
                }
            }
            if (length != numberingSystems.length) {
                String[] trimmed = new String[length];
                System.arraycopy(numberingSystems, 0, trimmed, 0, length);
                numberingSystems = trimmed;
            }
        }

        Arrays.sort(numberingSystems);
        return numberingSystems;
    }

    @TruffleBoundary
    public static String[] availableTimeZones() {
        Set<String> set = TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL_LOCATION, null, null);
        String[] timeZones = set.toArray(new String[set.size()]);
        Arrays.sort(timeZones);
        return timeZones;
    }

    @TruffleBoundary
    public static String[] availableUnits() {
        Set<String> set = SANCTIONED_SIMPLE_UNIT_IDENTIFIERS;
        String[] units = set.toArray(new String[set.size()]);
        Arrays.sort(units);
        return units;
    }

    public static String sourceString(int start, int limit, int startRangeStart, int startRangeLimit, int endRangeStart, int endRangeLimit) {
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

}
