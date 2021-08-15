/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.intl;

import java.util.MissingResourceException;
import java.util.Locale;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocale;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public abstract class InitializeLocaleNode extends JavaScriptBaseNode {
    @Child CoerceOptionsToObjectNode coerceOptionsToObjectNode;
    @Child GetStringOptionNode getLanguageOption;
    @Child GetStringOptionNode getScriptOption;
    @Child GetStringOptionNode getRegionOption;
    @Child GetStringOptionNode getCalendarOption;
    @Child GetStringOptionNode getCollationOption;
    @Child GetStringOptionNode getHourCycleOption;
    @Child GetStringOptionNode getCaseFirstOption;
    @Child GetBooleanOptionNode getNumericOption;
    @Child GetStringOptionNode getNumberingSystemOption;
    private final BranchProfile errorBranch = BranchProfile.create();

    protected InitializeLocaleNode(JSContext context) {
        this.coerceOptionsToObjectNode = CoerceOptionsToObjectNodeGen.create(context);
        this.getLanguageOption = GetStringOptionNode.create(context, IntlUtil.LANGUAGE, null, null);
        this.getScriptOption = GetStringOptionNode.create(context, IntlUtil.SCRIPT, null, null);
        this.getRegionOption = GetStringOptionNode.create(context, IntlUtil.REGION, null, null);
        this.getCalendarOption = GetStringOptionNode.create(context, IntlUtil.CALENDAR, null, null);
        this.getCollationOption = GetStringOptionNode.create(context, IntlUtil.COLLATION, null, null);
        this.getHourCycleOption = GetStringOptionNode.create(context, IntlUtil.HOUR_CYCLE, new String[]{IntlUtil.H11, IntlUtil.H12, IntlUtil.H23, IntlUtil.H24}, null);
        this.getCaseFirstOption = GetStringOptionNode.create(context, IntlUtil.CASE_FIRST, new String[]{IntlUtil.UPPER, IntlUtil.LOWER, IntlUtil.FALSE}, null);
        this.getNumericOption = GetBooleanOptionNode.create(context, IntlUtil.NUMERIC, null);
        this.getNumberingSystemOption = GetStringOptionNode.create(context, IntlUtil.NUMBERING_SYSTEM, null, null);
    }

    public abstract DynamicObject executeInit(DynamicObject locale, Object tag, Object options);

    public static InitializeLocaleNode createInitalizeLocaleNode(JSContext context) {
        return InitializeLocaleNodeGen.create(context);
    }

    @Specialization
    public DynamicObject initializeLocaleUsingString(DynamicObject localeObject, String tagArg, Object optionsArg) {
        try {
            DynamicObject options = coerceOptionsToObjectNode.execute(optionsArg);
            String tag = applyOptionsToTag(tagArg, options);
            String optCalendar = getCalendarOption.executeValue(options);
            if (optCalendar != null) {
                IntlUtil.validateUnicodeLocaleIdentifierType(optCalendar, errorBranch);
            }
            String optCollation = getCollationOption.executeValue(options);
            if (optCollation != null) {
                IntlUtil.validateUnicodeLocaleIdentifierType(optCollation, errorBranch);
            }
            String optHourCycle = getHourCycleOption.executeValue(options);
            String optCaseFirst = getCaseFirstOption.executeValue(options);
            Boolean optNumeric = getNumericOption.executeValue(options);
            String optNumberingSystem = getNumberingSystemOption.executeValue(options);
            if (optNumberingSystem != null) {
                IntlUtil.validateUnicodeLocaleIdentifierType(optNumberingSystem, errorBranch);
            }
            Locale locale = applyUnicodeExtensionToTag(tag, optCalendar, optCollation, optHourCycle, optCaseFirst, optNumeric, optNumberingSystem);
            JSLocale.InternalState state = JSLocale.getInternalState(localeObject);
            JSLocale.setupInternalState(state, locale);
        } catch (MissingResourceException e) {
            errorBranch.enter();
            throw Errors.createICU4JDataError(e);
        }
        return localeObject;
    }

    @Specialization(guards = "isJSLocale(tagArg)")
    public DynamicObject initializeLocaleUsingLocale(DynamicObject localeObject, DynamicObject tagArg, Object optionsArg) {
        JSLocale.InternalState state = JSLocale.getInternalState(tagArg);
        return initializeLocaleUsingString(localeObject, state.getLocale(), optionsArg);
    }

    @Specialization(guards = {"isJSObject(tagArg)", "!isJSLocale(tagArg)"})
    public DynamicObject initializeLocaleUsingObject(DynamicObject localeObject, DynamicObject tagArg, Object optionsArg,
                    @Cached("create()") JSToStringNode toStringNode) {
        return initializeLocaleUsingString(localeObject, toStringNode.executeString(tagArg), optionsArg);
    }

    @Specialization(guards = {"!isJSObject(tagArg)", "!isString(tagArg)"})
    public DynamicObject initializeLocaleOther(@SuppressWarnings("unused") DynamicObject localeObject, @SuppressWarnings("unused") Object tagArg, @SuppressWarnings("unused") Object optionsArg) {
        throw Errors.createTypeError("Tag should be a string or an object.");
    }

    private String applyOptionsToTag(String tag, DynamicObject options) {
        String canonicalizedTag = IntlUtil.validateAndCanonicalizeLanguageTag(tag);
        String optLanguage = getLanguageOption.executeValue(options);
        if (optLanguage != null) {
            IntlUtil.ensureIsStructurallyValidLanguageSubtag(optLanguage);
        }
        String optScript = getScriptOption.executeValue(options);
        if (optScript != null) {
            IntlUtil.ensureIsStructurallyValidScriptSubtag(optScript);
        }
        String optRegion = getRegionOption.executeValue(options);
        if (optRegion != null) {
            IntlUtil.ensureIsStructurallyValidRegionSubtag(optRegion);
        }
        return IntlUtil.validateAndCanonicalizeLanguageTag(applyOptionsToTag(canonicalizedTag, optLanguage, optScript, optRegion));
    }

    @TruffleBoundary
    private static String applyOptionsToTag(String tag, String optLanguage, String optScript, String optRegion) {
        Locale.Builder builder = new Locale.Builder().setLanguageTag(tag);
        if (optLanguage != null) {
            builder.setLanguage(optLanguage);
        }
        if (optScript != null) {
            builder.setScript(optScript);
        }
        if (optRegion != null) {
            builder.setRegion(optRegion);
        }
        return IntlUtil.maybeAppendMissingLanguageSubTag(builder.build().toLanguageTag());
    }

    @TruffleBoundary
    private static Locale applyUnicodeExtensionToTag(String tag, String optCalendar, String optCollation, String optHourCycle, String optCaseFirst, Boolean optNumeric,
                    String optNumberingSystem) {
        Locale.Builder builder = new Locale.Builder().setLanguageTag(tag);
        if (optCalendar != null) {
            setUnicodeLocaleKeywordHelper(builder, "ca", IntlUtil.normalizeCAType(optCalendar));
        }
        if (optCollation != null) {
            setUnicodeLocaleKeywordHelper(builder, "co", optCollation);
        }
        if (optHourCycle != null) {
            setUnicodeLocaleKeywordHelper(builder, "hc", optHourCycle);
        }
        if (optCaseFirst != null) {
            setUnicodeLocaleKeywordHelper(builder, "kf", optCaseFirst);
        }
        if (optNumeric != null) {
            setUnicodeLocaleKeywordHelper(builder, "kn", optNumeric.toString());
        }
        if (optNumberingSystem != null) {
            setUnicodeLocaleKeywordHelper(builder, "nu", optNumberingSystem);
        }
        return builder.build();
    }

    private static void setUnicodeLocaleKeywordHelper(Locale.Builder builder, String key, String type) {
        builder.setUnicodeLocaleKeyword(key, "true".equals(type) ? "" : type);
    }

}
