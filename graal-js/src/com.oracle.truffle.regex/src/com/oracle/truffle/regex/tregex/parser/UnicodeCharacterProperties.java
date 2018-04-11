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
package com.oracle.truffle.regex.tregex.parser;

public class UnicodeCharacterProperties {

    public static CodePointSet getProperty(String propertySpec) {
        return evaluatePropertySpec(normalizePropertySpec(propertySpec));
    }

    /**
     * @param propertySpec *Normalized* Unicode character property specification (i.e. only
     *            abbreviated properties and property values)
     */
    private static CodePointSet evaluatePropertySpec(String propertySpec) {
        switch (propertySpec) {
            // The following aggregate general categories are defined in Unicode Standard Annex 44,
            // Section 5.7.1. (http://www.unicode.org/reports/tr44/#GC_Values_Table).
            case "gc=LC":
                return unionOfGeneralCategories("Lu", "Ll", "Lt");
            case "gc=L":
                return unionOfGeneralCategories("Lu", "Ll", "Lt", "Lm", "Lo");
            case "gc=M":
                return unionOfGeneralCategories("Mn", "Mc", "Me");
            case "gc=N":
                return unionOfGeneralCategories("Nd", "Nl", "No");
            case "gc=P":
                return unionOfGeneralCategories("Pc", "Pd", "Ps", "Pe", "Pi", "Pf", "Po");
            case "gc=S":
                return unionOfGeneralCategories("Sm", "Sc", "Sk", "So");
            case "gc=Z":
                return unionOfGeneralCategories("Zs", "Zl", "Zp");
            case "gc=C":
                return unionOfGeneralCategories("Cc", "Cf", "Cs", "Co", "Cn");
        }
        return UnicodeCharacterPropertyData.retrieveProperty(propertySpec);
    }

    /**
     * @param generalCategoryNames *Abbreviated* names of general categories
     */
    private static CodePointSet unionOfGeneralCategories(String... generalCategoryNames) {
        CodePointSet set = CodePointSet.createEmpty();
        for (String generalCategoryName : generalCategoryNames) {
            set.addSet(evaluatePropertySpec("gc=" + generalCategoryName));
        }
        return set;
    }

    private static String normalizePropertySpec(String propertySpec) {
        int equals = propertySpec.indexOf('=');
        if (equals >= 0) {
            String propertyName = normalizePropertyName(propertySpec.substring(0, equals));
            String propertyValue = propertySpec.substring(equals + 1);
            switch (propertyName) {
                case "gc":
                    propertyValue = normalizeGeneralCategoryName(propertyValue);
                    break;
                case "sc":
                case "scx":
                    propertyValue = normalizeScriptName(propertyValue);
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Binary property %s cannot appear to the left of '=' in a Unicode property escape", propertySpec.substring(0, equals)));
            }
            return propertyName + "=" + propertyValue;
        } else if (isGeneralCategoryName(propertySpec)) {
            return "gc=" + normalizeGeneralCategoryName(propertySpec);
        } else {
            return normalizePropertyName(propertySpec);
        }
    }

    private static boolean isGeneralCategoryName(String generalCategoryName) {
        return UnicodeCharacterPropertyData.GENERAL_CATEGORY_ALIASES.containsKey(generalCategoryName);
    }

    private static String normalizePropertyName(String propertyName) {
        if (!UnicodeCharacterPropertyData.PROPERTY_ALIASES.containsKey(propertyName)) {
            throw new IllegalArgumentException(String.format("Unsupported Unicode character property '%s'", propertyName));
        }
        return UnicodeCharacterPropertyData.PROPERTY_ALIASES.get(propertyName);
    }

    private static String normalizeGeneralCategoryName(String generalCategoryName) {
        if (!UnicodeCharacterPropertyData.GENERAL_CATEGORY_ALIASES.containsKey(generalCategoryName)) {
            throw new IllegalArgumentException(String.format("Unknown Unicode character general category '%s'", generalCategoryName));
        }
        return UnicodeCharacterPropertyData.GENERAL_CATEGORY_ALIASES.get(generalCategoryName);
    }

    private static String normalizeScriptName(String scriptName) {
        if (!UnicodeCharacterPropertyData.SCRIPT_ALIASES.containsKey(scriptName)) {
            throw new IllegalArgumentException(String.format("Unkown Unicode script name '%s'", scriptName));
        }
        return UnicodeCharacterPropertyData.SCRIPT_ALIASES.get(scriptName);
    }
}
