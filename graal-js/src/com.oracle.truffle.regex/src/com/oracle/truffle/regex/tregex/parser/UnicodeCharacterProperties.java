/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
