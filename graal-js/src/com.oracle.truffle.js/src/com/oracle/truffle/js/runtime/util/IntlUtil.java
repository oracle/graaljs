/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

import com.ibm.icu.text.CaseMap;
import com.ibm.icu.text.CaseMap.Lower;
import com.ibm.icu.text.CaseMap.Upper;
import com.ibm.icu.util.ULocale;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 *
 * ECMA 402 Utilities.
 */
public final class IntlUtil {

    private IntlUtil() {
        // should not be constructed
    }

    // based on http://unicode.org/repos/cldr/trunk/common/bcp47/number.xml
    private static final List<String> BCP47_NU_KEYS = Arrays.asList(new String[]{"adlm", "ahom", "arab", "arabext", "armn", "armnlow", "bali", "beng", "bhks", "brah", "cakm", "cham", "cyrl", "deva",
                    "ethi", "finance", "fullwide", "geor", "gonm", "grek", "greklow", "gujr", "guru", "hanidays", "hanidec", "hans", "hansfin", "hant", "hantfin", "hebr", "hmng", "java", "jpan",
                    "jpanfin", "kali", "khmr", "knda", "lana", "lanatham", "laoo", "latn", "lepc", "limb", "mathbold", "mathdbl", "mathmono", "mathsanb", "mathsans", "mlym", "modi", "mong", "mroo",
                    "mtei", "mymr", "mymrshan", "mymrtlng", "native", "newa", "nkoo", "olck", "orya", "osma", "roman", "romanlow", "saur", "shrd", "sind", "sinh", "sora", "sund", "takr", "talu",
                    "taml", "tamldec", "telu", "thai", "tirh", "tibt", "traditio", "vaii", "wara"});
    private static final List<String> BANNED_BCP47_NU_KEYS = Arrays.asList("native", "traditio", "finance");

    public static final String _2_DIGIT = "2-digit";
    public static final String ACCENT = "accent";
    public static final String ALWAYS = "always";
    public static final String AUTO = "auto";
    public static final String BEST_FIT = "best fit";
    public static final String BASE = "base";
    public static final String BASIC = "basic";
    public static final String BREAK_TYPE = "breakType";
    public static final String CALENDAR = "calendar";
    public static final String CARDINAL = "cardinal";
    public static final String CASE = "case";
    public static final String CASE_FIRST = "caseFirst";
    public static final String CODE = "code";
    public static final String COLLATION = "collation";
    public static final String CONJUNCTION = "conjunction";
    public static final String CURRENCY = "currency";
    public static final String CURRENCY_DISPLAY = "currencyDisplay";
    public static final String DAY = "day";
    public static final String DEFAULT = "default";
    public static final String DECIMAL = "decimal";
    public static final String DISJUNCTION = "disjunction";
    public static final String ELEMENT = "element";
    public static final String ERA = "era";
    public static final String FALSE = "false";
    public static final String FORMAT_MATCHER = "formatMatcher";
    public static final String GRANULARITY = "granularity";
    public static final String GRAPHEME = "grapheme";
    public static final String H11 = "h11";
    public static final String H12 = "h12";
    public static final String H23 = "h23";
    public static final String H24 = "h24";
    public static final String HOUR = "hour";
    public static final String HOUR_CYCLE = "hourCycle";
    public static final String HOUR12 = "hour12";
    public static final String INDEX = "index";
    public static final String IGNORE_PUNCTUATION = "ignorePunctuation";
    public static final String LITERAL = "literal";
    public static final String LOCALE = "locale";
    public static final String LOCALE_MATCHER = "localeMatcher";
    public static final String LONG = "long";
    public static final String LOOKUP = "lookup";
    public static final String LOOSE = "loose";
    public static final String LOWER = "lower";
    public static final String MAXIMUM_FRACTION_DIGITS = "maximumFractionDigits";
    public static final String MAXIMUM_SIGNIFICANT_DIGITS = "maximumSignificantDigits";
    public static final String MINIMUM_FRACTION_DIGITS = "minimumFractionDigits";
    public static final String MINIMUM_INTEGER_DIGITS = "minimumIntegerDigits";
    public static final String MINIMUM_SIGNIFICANT_DIGITS = "minimumSignificantDigits";
    public static final String MINUTE = "minute";
    public static final String MONTH = "month";
    public static final String NAME = "name";
    public static final String NARROW = "narrow";
    public static final String NONE = "none";
    public static final String NORMAL = "normal";
    public static final String NUMERIC = "numeric";
    public static final String NUMBERING_SYSTEM = "numberingSystem";
    public static final String OR = "or";
    public static final String OR_NARROW = "or-narrow";
    public static final String OR_SHORT = "or-short";
    public static final String ORDINAL = "ordinal";
    public static final String PERCENT = "percent";
    public static final String SEARCH = "search";
    public static final String SEP = "sep";
    public static final String SECOND = "second";
    public static final String SEGMENT = "segment";
    public static final String SENTENCE = "sentence";
    public static final String SENSITIVITY = "sensitivity";
    public static final String SHORT = "short";
    public static final String SORT = "sort";
    public static final String STANDARD = "standard";
    public static final String STANDARD_NARROW = "standard-narrow";
    public static final String STANDARD_SHORT = "standard-short";
    public static final String STRICT = "strict";
    public static final String STYLE = "style";
    public static final String SYMBOL = "symbol";
    public static final String TERM = "term";
    public static final String TIME_ZONE = "timeZone";
    public static final String TIME_ZONE_NAME = "timeZoneName";
    public static final String TYPE = "type";
    public static final String UND = "und";
    public static final String UNIT = "unit";
    public static final String UNIT_NARROW = "unit-narrow";
    public static final String UNIT_SHORT = "unit-short";
    public static final String UPPER = "upper";
    public static final String USAGE = "usage";
    public static final String USE_GROUPING = "useGrouping";
    public static final String VALUE = "value";
    public static final String VARIANT = "variant";
    public static final String WORD = "word";
    public static final String WEEKDAY = "weekday";
    public static final String YEAR = "year";

    public static String selectedLocale(JSContext ctx, String[] locales) {
        Locale matchedLocale = lookupMatcher(ctx, locales);
        return matchedLocale != null ? matchedLocale.toLanguageTag() : null;
    }

    @TruffleBoundary
    private static Locale lookupMatcher(JSContext ctx, String[] languageTags) {
        for (String lt : languageTags) {
            // BestAvailableLocale: http://ecma-international.org/ecma-402/1.0/#sec-9.2.2
            Locale candidate = Locale.forLanguageTag(lt);
            while (true) {
                if (lookupMatch(ctx, candidate, true)) {
                    return candidate;
                }
                String candidateLanguageTag = candidate.toLanguageTag();
                int pos = candidateLanguageTag.lastIndexOf('-');
                if (pos != -1) {
                    if (pos >= 2 && candidateLanguageTag.charAt(pos - 2) == '-') {
                        pos -= 2;
                    }
                    candidateLanguageTag = candidateLanguageTag.substring(0, pos);
                    candidate = Locale.forLanguageTag(candidateLanguageTag);
                } else {
                    break;
                }
            }
        }
        return null;
    }

    @TruffleBoundary
    public static List<Object> supportedLocales(JSContext ctx, String[] locales, @SuppressWarnings("unused") String matcher) {
        List<Object> result = new ArrayList<>();
        for (String l : locales) {
            if (lookupMatch(ctx, Locale.forLanguageTag(l), true)) {
                result.add(l);
            }
        }
        return result;
    }

    private static boolean lookupMatch(JSContext ctx, Locale locale, boolean stripIt) {
        Locale lookForLocale = stripIt ? locale.stripExtensions() : locale;
        // default locale might be missing in the available locale list
        return getAvailableLocales().contains(lookForLocale) || ctx.getLocale().equals(lookForLocale);
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
            }
        } catch (MissingResourceException e) {
            throw Errors.createICU4JDataError(e);
        }

        return result;
    }

    public static boolean isSupportedNumberSystemKey(String nuKey) {
        return BCP47_NU_KEYS.contains(nuKey) && !BANNED_BCP47_NU_KEYS.contains(nuKey);
    }

    @TruffleBoundary
    public static Locale withoutUnicodeExtension(Locale originalLocale, String key) {
        if (!originalLocale.getUnicodeLocaleKeys().contains(key)) {
            return originalLocale;
        } else {
            String value = originalLocale.getUnicodeLocaleType(key);
            String originalTag = originalLocale.toLanguageTag();
            String toRemove = "-u-" + key + "-" + value;
            String strippedTag = originalTag.replace(toRemove, "");
            return new Locale(strippedTag);
        }
    }

    @TruffleBoundary
    public static void ensureIsStructurallyValidLanguageTag(String languageTag) {
        createValidatedLocale(languageTag);
    }

    private static Locale createValidatedLocale(String languageTag) throws JSException {
        try {
            return new Locale.Builder().setLanguageTag(languageTag).build();
        } catch (IllformedLocaleException e) {
            throw Errors.createRangeError(e.getMessage());
        }
    }

    @TruffleBoundary
    // https://tc39.github.io/ecma402/#sec-canonicalizelanguagetag
    public static String validateAndCanonicalizeLanguageTag(String languageTag) {
        return createValidatedLocale(languageTag).toLanguageTag();
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
        String selectedTag = IntlUtil.selectedLocale(ctx, locales);
        Locale selectedLocale = selectedTag != null ? Locale.forLanguageTag(selectedTag) : ctx.getLocale();
        return selectedLocale.stripExtensions();
    }

    public static DynamicObject makePart(JSContext context, String type, String value) {
        return makePart(context, type, value, null);
    }

    public static DynamicObject makePart(JSContext context, String type, String value, String unit) {
        DynamicObject p = JSUserObject.create(context);
        JSObject.set(p, TYPE, type);
        JSObject.set(p, VALUE, value);
        if (unit != null) {
            JSObject.set(p, UNIT, unit);
        }
        return p;
    }
}
