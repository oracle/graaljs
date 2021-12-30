/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormat;
import com.oracle.truffle.js.runtime.util.IntlUtil;

/*
 * https://tc39.github.io/ecma402/#sec-initializenumberformat
 */
public abstract class InitializeNumberFormatNode extends JavaScriptBaseNode {

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
    @Child GetNumberOptionNode getRoundingIncrementOption;
    @Child GetStringOptionNode getTrailingZeroDisplayOption;
    @Child GetStringOptionNode getRoundingModeOption;
    private final BranchProfile errorBranch = BranchProfile.create();

    protected InitializeNumberFormatNode(JSContext context) {
        this.context = context;
        this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        this.coerceOptionsToObjectNode = CoerceOptionsToObjectNodeGen.create(context);
        this.getLocaleMatcherOption = GetStringOptionNode.create(context, IntlUtil.LOCALE_MATCHER,
                        new String[]{IntlUtil.LOOKUP, IntlUtil.BEST_FIT}, IntlUtil.BEST_FIT);
        this.getNumberingSystemOption = GetStringOptionNode.create(context, IntlUtil.NUMBERING_SYSTEM, null, null);
        this.getStyleOption = GetStringOptionNode.create(context, IntlUtil.STYLE, new String[]{IntlUtil.DECIMAL, IntlUtil.PERCENT, IntlUtil.CURRENCY, IntlUtil.UNIT}, IntlUtil.DECIMAL);
        this.getCurrencyOption = GetStringOptionNode.create(context, IntlUtil.CURRENCY, null, null);
        this.getCurrencyDisplayOption = GetStringOptionNode.create(context, IntlUtil.CURRENCY_DISPLAY, new String[]{IntlUtil.CODE, IntlUtil.SYMBOL, IntlUtil.NARROW_SYMBOL, IntlUtil.NAME},
                        IntlUtil.SYMBOL);
        this.getCurrencySignOption = GetStringOptionNode.create(context, IntlUtil.CURRENCY_SIGN, new String[]{IntlUtil.STANDARD, IntlUtil.ACCOUNTING}, IntlUtil.STANDARD);
        this.getUnitOption = GetStringOptionNode.create(context, IntlUtil.UNIT, null, null);
        this.getUnitDisplayOption = GetStringOptionNode.create(context, IntlUtil.UNIT_DISPLAY, new String[]{IntlUtil.SHORT, IntlUtil.NARROW, IntlUtil.LONG}, IntlUtil.SHORT);
        this.getNotationOption = GetStringOptionNode.create(context, IntlUtil.NOTATION, new String[]{IntlUtil.STANDARD, IntlUtil.SCIENTIFIC, IntlUtil.ENGINEERING, IntlUtil.COMPACT},
                        IntlUtil.STANDARD);
        this.getCompactDisplayOption = GetStringOptionNode.create(context, IntlUtil.COMPACT_DISPLAY, new String[]{IntlUtil.SHORT, IntlUtil.LONG}, IntlUtil.SHORT);
        this.getUseGroupingOption = GetStringOrBooleanOptionNode.create(context, IntlUtil.USE_GROUPING, new String[]{IntlUtil.MIN2, IntlUtil.AUTO, IntlUtil.ALWAYS}, IntlUtil.ALWAYS, false, null);
        this.getSignDisplayOption = GetStringOptionNode.create(context, IntlUtil.SIGN_DISPLAY, new String[]{IntlUtil.AUTO, IntlUtil.NEVER, IntlUtil.ALWAYS, IntlUtil.EXCEPT_ZERO, IntlUtil.NEGATIVE},
                        IntlUtil.AUTO);
        this.getRoundingIncrementOption = GetNumberOptionNode.create(context, IntlUtil.ROUNDING_INCREMENT);
        this.getTrailingZeroDisplayOption = GetStringOptionNode.create(context, IntlUtil.TRAILING_ZERO_DISPLAY, new String[]{IntlUtil.AUTO, IntlUtil.STRIP_IF_INTEGER}, IntlUtil.AUTO);
        this.getRoundingModeOption = GetStringOptionNode.create(context, IntlUtil.ROUNDING_MODE, new String[]{IntlUtil.CEIL, IntlUtil.FLOOR, IntlUtil.EXPAND, IntlUtil.TRUNC, IntlUtil.HALF_CEIL,
                        IntlUtil.HALF_FLOOR, IntlUtil.HALF_EXPAND, IntlUtil.HALF_TRUNC, IntlUtil.HALF_EVEN}, IntlUtil.HALF_EXPAND);
        this.setNumberFormatDigitOptions = SetNumberFormatDigitOptionsNode.create(context);
    }

    public abstract DynamicObject executeInit(DynamicObject collator, Object locales, Object options);

    public static InitializeNumberFormatNode createInitalizeNumberFormatNode(JSContext context) {
        return InitializeNumberFormatNodeGen.create(context);
    }

    @Specialization
    public DynamicObject initializeNumberFormat(DynamicObject numberFormatObj, Object localesArg, Object optionsArg) {
        try {
            JSNumberFormat.InternalState state = JSNumberFormat.getInternalState(numberFormatObj);

            String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(localesArg);
            DynamicObject options = coerceOptionsToObjectNode.execute(optionsArg);

            getLocaleMatcherOption.executeValue(options);
            String numberingSystem = getNumberingSystemOption.executeValue(options);
            if (numberingSystem != null) {
                IntlUtil.validateUnicodeLocaleIdentifierType(numberingSystem, errorBranch);
                numberingSystem = IntlUtil.normalizeUnicodeLocaleIdentifierType(numberingSystem);
            }
            state.resolveLocaleAndNumberingSystem(context, locales, numberingSystem);

            setNumberFormatUnitOptions(state, options);

            int mnfdDefault;
            int mxfdDefault;
            String style = state.getStyle();
            if (IntlUtil.CURRENCY.equals(style)) {
                int cDigits = JSNumberFormat.currencyDigits(context, state.getCurrency());
                mnfdDefault = cDigits;
                mxfdDefault = cDigits;
            } else {
                mnfdDefault = 0;
                mxfdDefault = IntlUtil.PERCENT.equals(style) ? 0 : 3;
            }

            String notation = getNotationOption.executeValue(options);
            state.setNotation(notation);

            boolean compactNotation = IntlUtil.COMPACT.equals(notation);
            setNumberFormatDigitOptions.execute(state, options, mnfdDefault, mxfdDefault, compactNotation);

            int roundingIncrement = getRoundingIncrementOption.executeInt(options, 1, 5000, 1);
            if (!isValidRoundingIncrement(roundingIncrement) || (roundingIncrement != 1 && !IntlUtil.FRACTION_DIGITS.equals(state.getRoundingType()))) {
                errorBranch.enter();
                throw Errors.createRangeError("roundingIncrement value is out of range.");
            }
            state.setRoundingIncrement(roundingIncrement);

            String trailingZeroDisplay = getTrailingZeroDisplayOption.executeValue(options);
            state.setTrailingZeroDisplay(trailingZeroDisplay);

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

            String roundingMode = getRoundingModeOption.executeValue(options);
            state.setRoundingMode(roundingMode);

            state.initializeNumberFormatter();
        } catch (MissingResourceException e) {
            errorBranch.enter();
            throw Errors.createICU4JDataError(e);
        }
        return numberFormatObj;
    }

    private void setNumberFormatUnitOptions(JSNumberFormat.InternalState state, DynamicObject options) {
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

    private static boolean isValidRoundingIncrement(int roundingIncrement) {
        switch (roundingIncrement) {
            case 1:
            case 2:
            case 5:
            case 10:
            case 20:
            case 25:
            case 50:
            case 100:
            case 200:
            case 250:
            case 500:
            case 1000:
            case 2000:
            case 2500:
            case 5000:
                return true;
            default:
                return false;
        }
    }

}
