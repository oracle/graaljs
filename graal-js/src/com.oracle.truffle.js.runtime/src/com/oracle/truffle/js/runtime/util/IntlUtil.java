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
package com.oracle.truffle.js.runtime.util;

import com.ibm.icu.text.CaseMap;
import com.ibm.icu.text.CaseMap.Lower;
import com.ibm.icu.text.CaseMap.Upper;
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
            availableLocales = doGetAvailableLocales();
        }
        return availableLocales;
    }

    private static void addIfMissing(List<Locale> locales, Locale locale) {
        if (!locales.contains(locale)) {
            locales.add(locale);
        }
    }

    private static List<Locale> doGetAvailableLocales() {

        List<Locale> result = new ArrayList<>();

        if (JSTruffleOptions.SubstrateVM) {
            // GR-6347 (Locale.getAvailableLocales() support is missing in SVM)
            ULocale[] localesAvailable = ULocale.getAvailableLocales();
            Locale[] javaLocalesAvailable = new Locale[localesAvailable.length];
            int i = 0;
            for (ULocale ul : localesAvailable) {
                javaLocalesAvailable[i++] = ul.toLocale();
            }

            result.addAll(Arrays.asList(javaLocalesAvailable));
        } else {
            result.addAll(Arrays.asList(Locale.getAvailableLocales()));
        }
        // As of Unicode 10.0, the availableLocales list
        // contains the elements "az", "lt", and "tr".
        addIfMissing(result, Locale.forLanguageTag("az"));
        addIfMissing(result, Locale.forLanguageTag("lt"));
        addIfMissing(result, Locale.forLanguageTag("tr"));

        // default locale might be missing in the available locale list
        addIfMissing(result, Locale.getDefault());

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
