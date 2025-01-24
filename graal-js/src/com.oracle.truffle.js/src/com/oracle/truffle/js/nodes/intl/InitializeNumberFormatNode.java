/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import java.util.MissingResourceException;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormatObject;
import com.oracle.truffle.js.runtime.util.IntlUtil;

/*
 * https://tc39.github.io/ecma402/#sec-initializenumberformat
 */
public abstract class InitializeNumberFormatNode extends JavaScriptBaseNode {

    private static final List<String> CURRENCY_SIGN_OPTION_VALUES = List.of(IntlUtil.STANDARD, IntlUtil.ACCOUNTING);
    private static final List<String> STYLE_OPTION_VALUES = List.of(IntlUtil.DECIMAL, IntlUtil.PERCENT, IntlUtil.CURRENCY, IntlUtil.UNIT);
    private static final List<String> CURRENCY_DISPLAY_OPTION_VALUES = List.of(IntlUtil.CODE, IntlUtil.SYMBOL, IntlUtil.NARROW_SYMBOL, IntlUtil.NAME);
    private static final List<String> NOTATION_OPTION_VALUES = List.of(IntlUtil.STANDARD, IntlUtil.SCIENTIFIC, IntlUtil.ENGINEERING, IntlUtil.COMPACT);
    private static final List<String> COMPACT_OPTION_VALUES = List.of(IntlUtil.SHORT, IntlUtil.LONG);
    private static final List<String> USE_GROUPING_OPTION_VALUES = List.of(IntlUtil.MIN2, IntlUtil.AUTO, IntlUtil.ALWAYS);
    private static final List<String> SIGN_DISPLAY_OPTION_VALUES = List.of(IntlUtil.AUTO, IntlUtil.NEVER, IntlUtil.ALWAYS, IntlUtil.EXCEPT_ZERO, IntlUtil.NEGATIVE);

    private final JSContext context;

    @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;
    @Child CoerceOptionsToObjectNode coerceOptionsToObjectNode;

    @Child GetStringOptionNode getLocaleMatcherOption;
    @Child GetStringOptionNode getNumberingSystemOption;

    @Child SetNumberFormatDigitOptionsNode setNumberFormatDigitOptions;

    @Child GetStringOptionNode getStyleOption;

    @Child GetStringOptionNode getCurrencyOption;
    @Child GetStringOptionNode getCurrencyDisplayOption;
    @Child GetStringOptionNode getCurrencySignOption;

    @Child GetStringOptionNode getUnitOption;
    @Child GetStringOptionNode getUnitDisplayOption;

    @Child GetStringOptionNode getNotationOption;
    @Child GetStringOptionNode getCompactDisplayOption;
    @Child GetStringOrBooleanOptionNode getUseGroupingOption;
    @Child GetStringOptionNode getSignDisplayOption;
    private final BranchProfile errorBranch = BranchProfile.create();

    protected InitializeNumberFormatNode(JSContext context) {
        this.context = context;
        this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        this.coerceOptionsToObjectNode = CoerceOptionsToObjectNodeGen.create(context);
        this.getLocaleMatcherOption = GetStringOptionNode.create(context, IntlUtil.KEY_LOCALE_MATCHER, GetStringOptionNode.LOCALE_MATCHER_OPTION_VALUES, IntlUtil.BEST_FIT);
        this.getNumberingSystemOption = GetStringOptionNode.create(context, IntlUtil.KEY_NUMBERING_SYSTEM, null, null);
        this.getStyleOption = GetStringOptionNode.create(context, IntlUtil.KEY_STYLE, STYLE_OPTION_VALUES, IntlUtil.DECIMAL);
        this.getCurrencyOption = GetStringOptionNode.create(context, IntlUtil.KEY_CURRENCY, null, null);
        this.getCurrencyDisplayOption = GetStringOptionNode.create(context, IntlUtil.KEY_CURRENCY_DISPLAY, CURRENCY_DISPLAY_OPTION_VALUES, IntlUtil.SYMBOL);
        this.getCurrencySignOption = GetStringOptionNode.create(context, IntlUtil.KEY_CURRENCY_SIGN, CURRENCY_SIGN_OPTION_VALUES, IntlUtil.STANDARD);
        this.getUnitOption = GetStringOptionNode.create(context, IntlUtil.KEY_UNIT, null, null);
        this.getUnitDisplayOption = GetStringOptionNode.create(context, IntlUtil.KEY_UNIT_DISPLAY, GetStringOptionNode.LONG_SHORT_NARROW_OPTION_VALUES, IntlUtil.SHORT);
        this.getNotationOption = GetStringOptionNode.create(context, IntlUtil.KEY_NOTATION, NOTATION_OPTION_VALUES, IntlUtil.STANDARD);
        this.getCompactDisplayOption = GetStringOptionNode.create(context, IntlUtil.KEY_COMPACT_DISPLAY, COMPACT_OPTION_VALUES, IntlUtil.SHORT);
        this.getUseGroupingOption = GetStringOrBooleanOptionNode.create(context, IntlUtil.KEY_USE_GROUPING, USE_GROUPING_OPTION_VALUES, IntlUtil.ALWAYS, false, null);
        this.getSignDisplayOption = GetStringOptionNode.create(context, IntlUtil.KEY_SIGN_DISPLAY, SIGN_DISPLAY_OPTION_VALUES, IntlUtil.AUTO);
        this.setNumberFormatDigitOptions = SetNumberFormatDigitOptionsNode.create(context);
    }

    public abstract JSNumberFormatObject executeInit(JSNumberFormatObject numberFormatObj, Object locales, Object options);

    public static InitializeNumberFormatNode createInitalizeNumberFormatNode(JSContext context) {
        return InitializeNumberFormatNodeGen.create(context);
    }

    @Specialization
    public JSNumberFormatObject initializeNumberFormat(JSNumberFormatObject numberFormatObj, Object localesArg, Object optionsArg) {
        try {
            JSNumberFormat.InternalState state = numberFormatObj.getInternalState();

            String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(localesArg);
            Object options = coerceOptionsToObjectNode.execute(optionsArg);

            getLocaleMatcherOption.executeValue(options);
            String numberingSystem = getNumberingSystemOption.executeValue(options);
            if (numberingSystem != null) {
                IntlUtil.validateUnicodeLocaleIdentifierType(numberingSystem, errorBranch);
                numberingSystem = IntlUtil.normalizeUnicodeLocaleIdentifierType(numberingSystem);
            }
            state.resolveLocaleAndNumberingSystem(context, locales, numberingSystem);

            setNumberFormatUnitOptions(state, options);

            String style = state.getStyle();

            String notation = getNotationOption.executeValue(options);
            state.setNotation(notation);

            int mnfdDefault;
            int mxfdDefault;
            if (IntlUtil.CURRENCY.equals(style) && IntlUtil.STANDARD.equals(notation)) {
                int cDigits = JSNumberFormat.currencyDigits(context, state.getCurrency());
                mnfdDefault = cDigits;
                mxfdDefault = cDigits;
            } else {
                mnfdDefault = 0;
                mxfdDefault = IntlUtil.PERCENT.equals(style) ? 0 : 3;
            }

            boolean compactNotation = IntlUtil.COMPACT.equals(notation);
            setNumberFormatDigitOptions.execute(state, options, mnfdDefault, mxfdDefault, compactNotation);

            String compactDisplay = getCompactDisplayOption.executeValue(options);
            String defaultUseGrouping = IntlUtil.AUTO;
            if (compactNotation) {
                state.setCompactDisplay(compactDisplay);
                defaultUseGrouping = IntlUtil.MIN2;
            }

            Object useGrouping = getUseGroupingOption.executeValue(options);
            if (useGrouping == null) {
                useGrouping = defaultUseGrouping;
            }
            state.setGroupingUsed(useGrouping);

            String signDisplay = getSignDisplayOption.executeValue(options);
            state.setSignDisplay(signDisplay);

            state.initializeNumberFormatter();
        } catch (MissingResourceException e) {
            errorBranch.enter();
            throw Errors.createICU4JDataError(e);
        }
        return numberFormatObj;
    }

    private void setNumberFormatUnitOptions(JSNumberFormat.InternalState state, Object options) {
        String style = getStyleOption.executeValue(options);
        state.setStyle(style);
        boolean styleIsCurrency = IntlUtil.CURRENCY.equals(style);
        boolean styleIsUnit = IntlUtil.UNIT.equals(style);

        String currency = getCurrencyOption.executeValue(options);
        if (currency == null) {
            if (styleIsCurrency) {
                errorBranch.enter();
                throw Errors.createTypeError("Currency can not be undefined when style is \"currency\".");
            }
        } else {
            IntlUtil.ensureIsWellFormedCurrencyCode(currency);
        }
        String currencyDisplay = getCurrencyDisplayOption.executeValue(options);
        String currencySign = getCurrencySignOption.executeValue(options);

        String unit = getUnitOption.executeValue(options);
        if (unit == null) {
            if (styleIsUnit) {
                errorBranch.enter();
                throw Errors.createTypeError("Unit can not be undefined when style is \"unit\".");
            }
        } else {
            IntlUtil.ensureIsWellFormedUnitIdentifier(unit);
        }
        String unitDisplay = getUnitDisplayOption.executeValue(options);

        if (styleIsCurrency) {
            currency = IntlUtil.toUpperCase(currency);
            state.setCurrency(currency);
            state.setCurrencyDisplay(currencyDisplay);
            state.setCurrencySign(currencySign);
        } else if (styleIsUnit) {
            state.setUnit(unit);
            state.setUnitDisplay(unitDisplay);
        }
    }

}
