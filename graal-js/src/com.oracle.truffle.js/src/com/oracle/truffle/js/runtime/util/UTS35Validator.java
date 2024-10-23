/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.regex.Pattern;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * Validation of patterns from Unicode Technical Standard #35: UNICODE LOCALE DATA MARKUP LANGUAGE.
 * https://unicode.org/reports/tr35/
 */
public final class UTS35Validator {
    private static final Pattern LANGUAGE_ID_PATTERN = Pattern.compile(unicodeLanguageID());
    private static final Pattern LOCALE_ID_PATTERN = Pattern.compile(unicodeLocaleID());

    private UTS35Validator() {
    }

    @TruffleBoundary
    public static boolean isWellFormedUnicodeBCP47LocaleIdentifier(String languageTag) {
        return LOCALE_ID_PATTERN.matcher(languageTag).matches();
    }

    public static boolean isDigit(char c) {
        // digit = [0-9]
        return '0' <= c && c <= '9';
    }

    public static boolean isAlpha(char c) {
        // alpha = [A-Z a-z]
        return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
    }

    public static boolean isAlphanum(char c) {
        // alphanum = [0-9 A-Z a-z]
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || ('0' <= c && c <= '9');
    }

    @TruffleBoundary
    public static boolean isStructurallyValidLanguageId(String languageId) {
        return LANGUAGE_ID_PATTERN.matcher(languageId).matches();
    }

    public static boolean isStructurallyValidLanguageSubtag(String language) {
        // unicode_language_subtag = alpha{2,3} | alpha{5,8}
        int length = language.length();
        if (length < 2 || length == 4 || length > 8) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!isAlpha(language.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isStructurallyValidRegionSubtag(String region) {
        // unicode_region_subtag = (alpha{2} | digit{3})
        int length = region.length();
        return ((length == 2) && isAlpha(region.charAt(0)) && isAlpha(region.charAt(1))) ||
                        ((length == 3) && isDigit(region.charAt(0)) && isDigit(region.charAt(1)) && isDigit(region.charAt(2)));
    }

    public static boolean isStructurallyValidScriptSubtag(String script) {
        // unicode_script_subtag = alpha{4}
        return (script.length() == 4) && isAlpha(script.charAt(0)) && isAlpha(script.charAt(1)) && isAlpha(script.charAt(2)) && isAlpha(script.charAt(3));
    }

    public static boolean isStructurallyValidType(String type) {
        // type = alphanum{3,8} (sep alphanum{3,8})*
        int alphanumStart = 0;
        for (int i = 0; i < type.length(); i++) {
            char c = type.charAt(i);
            if (!isAlphanum(c)) {
                if (c == '-') { // c is sep
                    int alphanumLength = i - alphanumStart;
                    if (3 <= alphanumLength && alphanumLength <= 8) {
                        alphanumStart = i + 1;
                    } else {
                        // not alphanum{3,8}
                        return false;
                    }
                } else {
                    // unexpected character
                    return false;
                }
            }
        }
        int alphanumLength = type.length() - alphanumStart;
        return 3 <= alphanumLength && alphanumLength <= 8;
    }

    private static String unicodeLanguageID() {
        // unicode_language_id = "root" | (unicode_language_subtag (sep unicode_script_subtag)? |
        // unicode_script_subtag) (sep unicode_region_subtag)? (sep unicode_variant_subtag)*

        // "root" and tags starting with a script subtag are backwards compatibility syntax
        // (not allowed in Unicode BCP 47 locale identifier)
        return group(unicodeLanguageSubtag() + group(sep() + unicodeScriptSubtag()) + "?" + group(sep() + unicodeRegionSubtag()) + "?" + group(sep() + unicodeVariantSubtag()) + "*");
    }

    private static String unicodeLanguageSubtag() {
        // unicode_language_subtag = alpha{2,3} | alpha{5,8}
        return group(alpha() + "{2,3}|" + alpha() + "{5,8}");
    }

    private static String unicodeScriptSubtag() {
        // unicode_script_subtag = alpha{4}
        return alpha() + "{4}";
    }

    private static String unicodeRegionSubtag() {
        // unicode_region_subtag = (alpha{2} | digit{3})
        return group(alpha() + "{2}|" + digit() + "{3}");
    }

    private static String unicodeVariantSubtag() {
        // unicode_variant_subtag = (alphanum{5,8} | digit alphanum{3})
        return group(alphanum() + "{5,8}|" + digit() + alphanum() + "{3}");
    }

    private static String sep() {
        // sep = [-_]

        // _ is backwards compatibility syntax
        // (not allowed in Unicode BCP 47 locale identifier)
        return "-";
    }

    private static String digit() {
        // digit = [0-9]
        return "[0-9]";
    }

    private static String alpha() {
        // alpha = [A-Z a-z]
        return "[A-Za-z]";
    }

    private static String alphanum() {
        // alphanum = [0-9 A-Z a-z]
        return "[0-9A-Za-z]";
    }

    private static String unicodeLocaleID() {
        // unicode_locale_id = unicode_language_id extensions* pu_extensions?
        return group(unicodeLanguageID() + extensions() + "*" + puExtensions() + "?");
    }

    private static String extensions() {
        // extensions = unicode_locale_extensions | transformed_extensions | other_extensions
        return group(unicodeLocaleExtensions() + "|" + transformedExtensions() + "|" + otherExtensions());
    }

    private static String unicodeLocaleExtensions() {
        // unicode_locale_extensions = sep [uU] ((sep keyword)+ |(sep attribute)+ (sep keyword)*)
        return group(sep() + "[uU]" + group(group(sep() + keyword()) + "+|" + group(sep() + attribute()) + "+" + group(sep() + keyword()) + "*"));
    }

    private static String transformedExtensions() {
        // transformed_extensions = sep [tT] ((sep tlang (sep tfield)*) | (sep tfield)+)
        return group(sep() + "[tT]" + group(group(sep() + tLang() + group(sep() + tField()) + "*") + "|" + group(sep() + tField()) + "+"));
    }

    private static String puExtensions() {
        // pu_extensions = sep [xX] (sep alphanum{1,8})+
        return group(sep() + "[xX]" + group(sep() + alphanum() + "{1,8}") + "+");
    }

    private static String otherExtensions() {
        // other_extensions = sep [alphanum-[tTuUxX]] (sep alphanum{2,8})+
        return group(sep() + "[0-9a-svwyzA-SVWYZ]" + group(sep() + alphanum() + "{2,8}") + "+");
    }

    private static String keyword() {
        // keyword = key (sep type)?
        return group(key() + group(sep() + type()) + "?");
    }

    private static String key() {
        // key = alphanum alpha
        return group(alphanum() + alpha());
    }

    private static String type() {
        // type = alphanum{3,8} (sep alphanum{3,8})*
        return group(alphanum() + "{3,8}" + group(sep() + alphanum() + "{3,8}") + "*");
    }

    private static String attribute() {
        // attribute = alphanum{3,8}
        return alphanum() + "{3,8}";
    }

    private static String tLang() {
        // tlang = unicode_language_subtag (sep unicode_script_subtag)? (sep unicode_region_subtag)?
        // (sep unicode_variant_subtag)*
        return group(unicodeLanguageSubtag() + group(sep() + unicodeScriptSubtag()) + "?" + group(sep() + unicodeRegionSubtag()) + "?" + group(sep() + unicodeVariantSubtag()) + "*");
    }

    private static String tField() {
        // tfield = tkey tvalue
        return group(tKey() + tValue());
    }

    private static String tKey() {
        // tkey = alpha digit
        return group(alpha() + digit());
    }

    private static String tValue() {
        // tvalue = (sep alphanum{3,8})+
        return group(sep() + alphanum() + "{3,8}") + "+";
    }

    private static String group(String expression) {
        return "(?:" + expression + ")";
    }

}
