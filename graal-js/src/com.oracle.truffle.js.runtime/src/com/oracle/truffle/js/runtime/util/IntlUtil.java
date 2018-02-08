/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.util;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.text.CaseMap;
import com.ibm.icu.text.CaseMap.Lower;
import com.ibm.icu.text.CaseMap.Upper;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllformedLocaleException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 *
 * ECMA 402 Utilities.
 */
public class IntlUtil {

    // based on http://unicode.org/repos/cldr/trunk/common/bcp47/number.xml
    public static final List<String> BCP47_NU_KEYS = Arrays.asList(new String[]{"adlm", "ahom", "arab", "arabext", "armn", "armnlow", "bali", "beng", "bhks", "brah", "cakm", "cham", "cyrl", "deva",
                    "ethi", "finance", "fullwide", "geor", "gonm", "grek", "greklow", "gujr", "guru", "hanidays", "hanidec", "hans", "hansfin", "hant", "hantfin", "hebr", "hmng", "java", "jpan",
                    "jpanfin", "kali", "khmr", "knda", "lana", "lanatham", "laoo", "latn", "lepc", "limb", "mathbold", "mathdbl", "mathmono", "mathsanb", "mathsans", "mlym", "modi", "mong", "mroo",
                    "mtei", "mymr", "mymrshan", "mymrtlng", "native", "newa", "nkoo", "olck", "orya", "osma", "roman", "romanlow", "saur", "shrd", "sind", "sinh", "sora", "sund", "takr", "talu",
                    "taml", "tamldec", "telu", "thai", "tirh", "tibt", "traditio", "vaii", "wara"});
    public static final List<String> BANNED_BCP47_NU_KEYS = Arrays.asList(new String[]{"native", "traditio", "finance"});
    public static final List<String> RFC5646_LANGUAGE_TAGS = Arrays.asList(new String[]{"af", "af-ZA", "ar", "ar-AE", "ar-BH", "ar-DZ", "ar-EG", "ar-IQ", "ar-JO", "ar-KW", "ar-LB", "ar-LY", "ar-MA",
                    "ar-OM", "ar-QA", "ar-SA", "ar-SY", "ar-TN", "ar-YE", "az", "az-AZ", "az-Cyrl-AZ", "be", "be-BY", "bg", "bg-BG", "bs-BA", "ca", "ca-ES", "cs", "cs-CZ", "cy", "cy-GB", "da",
                    "da-DK", "de", "de-AT", "de-CH", "de-DE", "de-LI", "de-LU", "dv", "dv-MV", "el", "el-GR", "en", "en-AU", "en-BZ", "en-CA", "en-CB", "en-GB", "en-IE", "en-JM", "en-NZ", "en-PH",
                    "en-TT", "en-US", "en-ZA", "en-ZW", "eo", "es", "es-AR", "es-BO", "es-CL", "es-CO", "es-CR", "es-DO", "es-EC", "es-ES", "es-GT", "es-HN", "es-MX", "es-NI", "es-PA", "es-PE",
                    "es-PR", "es-PY", "es-SV", "es-UY", "es-VE", "et", "et-EE", "eu", "eu-ES", "fa", "fa-IR", "fi", "fi-FI", "fo", "fo-FO", "fr", "fr-BE", "fr-CA", "fr-CH", "fr-FR", "fr-LU", "fr-MC",
                    "gl", "gl-ES", "gu", "gu-IN", "he", "he-IL", "hi", "hi-IN", "hr", "hr-BA", "hr-HR", "hu", "hu-HU", "hy", "hy-AM", "id", "id-ID", "is", "is-IS", "it", "it-CH", "it-IT", "ja",
                    "ja-JP", "ka", "ka-GE", "kk", "kk-KZ", "kn", "kn-IN", "ko", "ko-KR", "kok", "kok-IN", "ky", "ky-KG", "lt", "lt-LT", "lv", "lv-LV", "mi", "mi-NZ", "mk", "mk-MK", "mn", "mn-MN",
                    "mr", "mr-IN", "ms", "ms-BN", "ms-MY", "mt", "mt-MT", "nb", "nb-NO", "nl", "nl-BE", "nl-NL", "nn-NO", "ns", "ns-ZA", "pa", "pa-IN", "pl", "pl-PL", "ps", "ps-AR", "pt", "pt-BR",
                    "pt-PT", "qu", "qu-BO", "qu-EC", "qu-PE", "ro", "ro-RO", "ru", "ru-RU", "sa", "sa-IN", "se", "se-FI", "se-NO", "se-SE", "sk", "sk-SK", "sl", "sl-SI", "sq", "sq-AL", "sr-BA",
                    "sr-Cyrl-BA", "sr-SP", "sr-Cyrl-SP", "sv", "sv-FI", "sv-SE", "sw", "sw-KE", "syr", "syr-SY", "ta", "ta-IN", "te", "te-IN", "th", "th-TH", "tl", "tl-PH", "tn", "tn-ZA", "tr",
                    "tr-TR", "tt", "tt-RU", "ts", "uk", "uk-UA", "ur", "ur-PK", "uz", "uz-UZ", "uz-Cyrl-UZ", "vi", "vi-VN", "xh", "xh-ZA", "zh", "zh-CN", "zh-HK", "zh-MO", "zh-SG", "zh-TW", "zu",
                    "zu-ZA"});

    static final ULocale[] localesAvailable = ULocale.getAvailableLocales();
    static final Locale[] javaLocalesAvailable = new Locale[localesAvailable.length];

    static {
        if (JSTruffleOptions.SubstrateVM) {
            int i = 0;
            for (ULocale ul : localesAvailable) {
                javaLocalesAvailable[i++] = ul.toLocale();
            }
            Locale el = Locale.ENGLISH;
            NumberFormat.getInstance(el);
            ICUResourceBundle.clearCache();
        }
    }

    public static String selectedLocale(String[] locales) {
        Locale matchedLocale = lookupMatcher(locales);
        return matchedLocale != null ? matchedLocale.toLanguageTag() : null;
    }

    @TruffleBoundary
    private static Locale lookupMatcher(String[] languageTags) {
        for (String lt : languageTags) {
            // BestAvailableLocale: http://ecma-international.org/ecma-402/1.0/#sec-9.2.2
            Locale candidate = Locale.forLanguageTag(lt);
            while (true) {
                if (lookupMatch(candidate)) {
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
    public static List<Object> supportedLocales(String[] locales) {
        List<Object> result = new LinkedList<>();
        for (String l : locales) {
            if (lookupMatch(Locale.forLanguageTag(l))) {
                result.add(l);
            }
        }
        return result;
    }

    private static boolean lookupMatch(Locale locale) {
        Locale strippedLocale = locale.stripExtensions();
        return getAvailableLocales().contains(strippedLocale);
    }

    static List<Locale> availableLocales = null;

    @TruffleBoundary
    private static List<Locale> getAvailableLocales() {

        if (availableLocales == null) {
            availableLocales = new ArrayList<>();

            availableLocales.addAll(Arrays.asList(doGetAvailableLocales()));
            // As of Unicode 10.0, the availableLocales list
            // contains the elements "az", "lt", and "tr".
            addIfMissing(availableLocales, Locale.forLanguageTag("az"));
            addIfMissing(availableLocales, Locale.forLanguageTag("lt"));
            addIfMissing(availableLocales, Locale.forLanguageTag("tr"));
        }

        return availableLocales;
    }

    private static void addIfMissing(List<Locale> locales, Locale locale) {
        if (!locales.contains(locale)) {
            locales.add(locale);
        }
    }

    private static Locale[] doGetAvailableLocales() {
        if (JSTruffleOptions.SubstrateVM) {
            // GR-6347 (Locale.getAvailableLocales() support is missing in SVM)
            return javaLocalesAvailable;
        } else {
            return Locale.getAvailableLocales();
        }
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
            Locale l = new Locale.Builder().setLanguageTag(languageTag).build();
            if (JSTruffleOptions.Intl402LocaleInRFC5646 && !RFC5646_LANGUAGE_TAGS.contains(l.getLanguage())) {
                throw Errors.createRangeError(String.format("invalid language, %s, as per RFC5646", l.getLanguage()));
            }
            return l;
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
    public static String toLowerCase(String s, String[] locales) {
        Locale strippedLocale = selectedLocaleStripped(locales);
        StringBuilder result = new StringBuilder();
        Lower tr = CaseMap.toLower();
        tr.apply(strippedLocale, s, result, null);
        return result.toString();
    }

    @TruffleBoundary
    public static String toUpperCase(String s, String[] locales) {
        Locale strippedLocale = selectedLocaleStripped(locales);
        StringBuilder result = new StringBuilder();
        Upper tr = CaseMap.toUpper();
        tr.apply(strippedLocale, s, result, null);
        return result.toString();
    }

    @TruffleBoundary
    public static Locale selectedLocaleStripped(String[] locales) {
        String selectedTag = IntlUtil.selectedLocale(locales);
        Locale selectedLocale = selectedTag != null ? Locale.forLanguageTag(selectedTag) : Locale.getDefault();
        Locale strippedLocale = selectedLocale.stripExtensions();
        return strippedLocale;
    }
}
