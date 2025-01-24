/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;
import java.util.MissingResourceException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocale;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocaleObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public abstract class InitializeLocaleNode extends JavaScriptBaseNode {

    private static final String MON = "mon";
    private static final String TUE = "tue";
    private static final String WED = "wed";
    private static final String THU = "thu";
    private static final String FRI = "fri";
    private static final String SAT = "sat";
    private static final String SUN = "sun";

    @Child CoerceOptionsToObjectNode coerceOptionsToObjectNode;
    @Child GetStringOptionNode getLanguageOption;
    @Child GetStringOptionNode getScriptOption;
    @Child GetStringOptionNode getRegionOption;
    @Child GetStringOptionNode getCalendarOption;
    @Child GetStringOptionNode getCollationOption;
    @Child GetStringOptionNode getFirstDayOfWeekOption;
    @Child GetStringOptionNode getHourCycleOption;
    @Child GetStringOptionNode getCaseFirstOption;
    @Child GetBooleanOptionNode getNumericOption;
    @Child GetStringOptionNode getNumberingSystemOption;
    private final BranchProfile errorBranch = BranchProfile.create();

    protected InitializeLocaleNode(JSContext context) {
        this.coerceOptionsToObjectNode = CoerceOptionsToObjectNodeGen.create(context);
        this.getLanguageOption = GetStringOptionNode.create(context, IntlUtil.KEY_LANGUAGE, null, null);
        this.getScriptOption = GetStringOptionNode.create(context, IntlUtil.KEY_SCRIPT, null, null);
        this.getRegionOption = GetStringOptionNode.create(context, IntlUtil.KEY_REGION, null, null);
        this.getCalendarOption = GetStringOptionNode.create(context, IntlUtil.KEY_CALENDAR, null, null);
        this.getCollationOption = GetStringOptionNode.create(context, IntlUtil.KEY_COLLATION, null, null);
        this.getFirstDayOfWeekOption = GetStringOptionNode.create(context, IntlUtil.KEY_FIRST_DAY_OF_WEEK, null, null);
        this.getHourCycleOption = GetStringOptionNode.create(context, IntlUtil.KEY_HOUR_CYCLE, GetStringOptionNode.HOUR_CYCLE_OPTION_VALUES, null);
        this.getCaseFirstOption = GetStringOptionNode.create(context, IntlUtil.KEY_CASE_FIRST, GetStringOptionNode.CASE_FIRST_OPTION_VALUES, null);
        this.getNumericOption = GetBooleanOptionNode.create(context, IntlUtil.KEY_NUMERIC, null);
        this.getNumberingSystemOption = GetStringOptionNode.create(context, IntlUtil.KEY_NUMBERING_SYSTEM, null, null);
    }

    public abstract JSLocaleObject executeInit(JSLocaleObject locale, Object tag, Object options);

    public static InitializeLocaleNode createInitalizeLocaleNode(JSContext context) {
        return InitializeLocaleNodeGen.create(context);
    }

    @Specialization
    public JSLocaleObject initializeLocaleUsingString(JSLocaleObject localeObject, TruffleString tagArg, Object optionsArg) {
        return initializeLocaleUsingJString(localeObject, Strings.toJavaString(tagArg), optionsArg);
    }

    private JSLocaleObject initializeLocaleUsingJString(JSLocaleObject localeObject, String tagArg, Object optionsArg) {
        try {
            Object options = coerceOptionsToObjectNode.execute(optionsArg);
            String tag = applyOptionsToTag(tagArg, options);
            String optCalendar = getCalendarOption.executeValue(options);
            if (optCalendar != null) {
                IntlUtil.validateUnicodeLocaleIdentifierType(optCalendar, errorBranch);
            }
            String optCollation = getCollationOption.executeValue(options);
            if (optCollation != null) {
                IntlUtil.validateUnicodeLocaleIdentifierType(optCollation, errorBranch);
            }
            String optFirstDayOfWeek = getFirstDayOfWeekOption.executeValue(options);
            if (optFirstDayOfWeek != null) {
                optFirstDayOfWeek = weekDayToString(optFirstDayOfWeek);
                IntlUtil.validateUnicodeLocaleIdentifierType(optFirstDayOfWeek, errorBranch);
            }
            String optHourCycle = getHourCycleOption.executeValue(options);
            String optCaseFirst = getCaseFirstOption.executeValue(options);
            Boolean optNumeric = getNumericOption.executeValue(options);
            String optNumberingSystem = getNumberingSystemOption.executeValue(options);
            if (optNumberingSystem != null) {
                IntlUtil.validateUnicodeLocaleIdentifierType(optNumberingSystem, errorBranch);
            }
            Locale locale = applyUnicodeExtensionToTag(tag, optCalendar, optCollation, optFirstDayOfWeek, optHourCycle, optCaseFirst, optNumeric, optNumberingSystem);
            JSLocale.InternalState state = localeObject.getInternalState();
            JSLocale.setupInternalState(state, locale);
        } catch (MissingResourceException e) {
            errorBranch.enter();
            throw Errors.createICU4JDataError(e);
        }
        return localeObject;
    }

    @Specialization
    public JSLocaleObject initializeLocaleUsingLocale(JSLocaleObject localeObject, JSLocaleObject tagArg, Object optionsArg) {
        JSLocale.InternalState state = tagArg.getInternalState();
        return initializeLocaleUsingJString(localeObject, state.getLocale(), optionsArg);
    }

    @Specialization(guards = {"!isJSLocale(tagArg)"})
    public JSLocaleObject initializeLocaleUsingObject(JSLocaleObject localeObject, JSObject tagArg, Object optionsArg,
                    @Cached JSToStringNode toStringNode) {
        return initializeLocaleUsingString(localeObject, toStringNode.executeString(tagArg), optionsArg);
    }

    @Specialization(guards = {"!isJSObject(tagArg)", "!isString(tagArg)"})
    public JSLocaleObject initializeLocaleOther(@SuppressWarnings("unused") JSLocaleObject localeObject, @SuppressWarnings("unused") Object tagArg, @SuppressWarnings("unused") Object optionsArg) {
        throw Errors.createTypeError("Tag should be a string or an object.");
    }

    private String applyOptionsToTag(String tag, Object options) {
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
    private static Locale applyUnicodeExtensionToTag(String tag, String optCalendar, String optCollation, String optFirstDayOfWeek, String optHourCycle, String optCaseFirst, Boolean optNumeric,
                    String optNumberingSystem) {
        Locale.Builder builder = new Locale.Builder().setLanguageTag(tag);
        if (optCalendar != null) {
            setUnicodeLocaleKeywordHelper(builder, "ca", IntlUtil.normalizeCAType(optCalendar));
        }
        if (optCollation != null) {
            setUnicodeLocaleKeywordHelper(builder, "co", optCollation);
        }
        if (optFirstDayOfWeek != null) {
            setUnicodeLocaleKeywordHelper(builder, "fw", optFirstDayOfWeek);
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

    private static String weekDayToString(String fw) {
        return switch (fw) {
            case "0" -> SUN;
            case "1" -> MON;
            case "2" -> TUE;
            case "3" -> WED;
            case "4" -> THU;
            case "5" -> FRI;
            case "6" -> SAT;
            case "7" -> SUN;
            default -> fw;
        };
    }

    public static int stringToWeekdayValue(String fw) {
        if (fw == null) {
            return -1;
        } else {
            return switch (fw) {
                case MON -> 1;
                case TUE -> 2;
                case WED -> 3;
                case THU -> 4;
                case FRI -> 5;
                case SAT -> 6;
                case SUN -> 7;
                default -> -1;
            };
        }
    }

}
