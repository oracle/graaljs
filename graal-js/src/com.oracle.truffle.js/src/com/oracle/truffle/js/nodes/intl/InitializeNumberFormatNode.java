/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSNumberFormat;
import com.oracle.truffle.js.runtime.util.IntlUtil;

/*
 * https://tc39.github.io/ecma402/#sec-initializenumberformat
 */
public abstract class InitializeNumberFormatNode extends JavaScriptBaseNode {

    private final JSContext context;

    @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;
    @Child CreateOptionsObjectNode createOptionsNode;

    @Child GetStringOptionNode getLocaleMatcherOption;
    @Child GetStringOptionNode getNumberingSystemOption;

    @Child GetNumberOptionNode getMinIntDigitsOption;
    @Child GetNumberOptionNode getMinFracDigitsOption;
    @Child GetNumberOptionNode getMaxFracDigitsOption;
    @Child PropertyGetNode getMinSignificantDigitsOption;
    @Child PropertyGetNode getMaxSignificantDigitsOption;
    @Child DefaultNumberOptionNode getMnsdDNO;
    @Child DefaultNumberOptionNode getMxsdDNO;

    @Child GetStringOptionNode getStyleOption;

    @Child GetStringOptionNode getCurrencyOption;
    @Child GetStringOptionNode getCurrencyDisplayOption;

    @Child GetBooleanOptionNode getUseGroupingOption;

    protected InitializeNumberFormatNode(JSContext context) {
        this.context = context;
        this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        this.createOptionsNode = CreateOptionsObjectNodeGen.create(context);
        this.getLocaleMatcherOption = GetStringOptionNode.create(context, IntlUtil.LOCALE_MATCHER,
                        new String[]{IntlUtil.LOOKUP, IntlUtil.BEST_FIT}, IntlUtil.BEST_FIT);
        this.getNumberingSystemOption = GetStringOptionNode.create(context, IntlUtil.NUMBERING_SYSTEM, null, null);
        this.getStyleOption = GetStringOptionNode.create(context, IntlUtil.STYLE, new String[]{IntlUtil.DECIMAL, IntlUtil.PERCENT, IntlUtil.CURRENCY}, IntlUtil.DECIMAL);
        this.getCurrencyOption = GetStringOptionNode.create(context, IntlUtil.CURRENCY, null, null);
        this.getCurrencyDisplayOption = GetStringOptionNode.create(context, IntlUtil.CURRENCY_DISPLAY, new String[]{IntlUtil.CODE, IntlUtil.SYMBOL, IntlUtil.NAME}, IntlUtil.SYMBOL);
        this.getUseGroupingOption = GetBooleanOptionNode.create(context, IntlUtil.USE_GROUPING, true);
        this.getMinIntDigitsOption = GetNumberOptionNode.create(context, IntlUtil.MINIMUM_INTEGER_DIGITS, 21);
        this.getMinSignificantDigitsOption = PropertyGetNode.create(IntlUtil.MINIMUM_SIGNIFICANT_DIGITS, false, context);
        this.getMaxSignificantDigitsOption = PropertyGetNode.create(IntlUtil.MAXIMUM_SIGNIFICANT_DIGITS, false, context);
        this.getMinFracDigitsOption = GetNumberOptionNode.create(context, IntlUtil.MINIMUM_FRACTION_DIGITS, 21);
        this.getMaxFracDigitsOption = GetNumberOptionNode.create(context, IntlUtil.MAXIMUM_FRACTION_DIGITS, 20);
        this.getMnsdDNO = DefaultNumberOptionNode.create(21, 1);
        this.getMxsdDNO = DefaultNumberOptionNode.create(21, 21);
    }

    public abstract DynamicObject executeInit(DynamicObject collator, Object locales, Object options);

    public static InitializeNumberFormatNode createInitalizeNumberFormatNode(JSContext context) {
        return InitializeNumberFormatNodeGen.create(context);
    }

    @Specialization
    public DynamicObject initializeNumberFormat(DynamicObject numberFormatObj, Object localesArg, Object optionsArg) {

        // must be invoked before any code that tries to access ICU library data
        try {
            JSNumberFormat.InternalState state = JSNumberFormat.getInternalState(numberFormatObj);

            String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(localesArg);
            DynamicObject options = createOptionsNode.execute(optionsArg);

            getLocaleMatcherOption.executeValue(options);
            String numberingSystemOpt = getNumberingSystemOption.executeValue(options);
            if (numberingSystemOpt != null) {
                IntlUtil.validateUnicodeLocaleIdentifierType(numberingSystemOpt);
                numberingSystemOpt = IntlUtil.normalizeUnicodeLocaleIdentifierType(numberingSystemOpt);
            }

            String optStyle = getStyleOption.executeValue(options);

            String optCurrency = getCurrencyOption.executeValue(options);
            String optCurrencyDisplay = getCurrencyDisplayOption.executeValue(options);

            state.setInitialized(true);

            JSNumberFormat.setLocaleAndNumberingSystem(context, state, locales, numberingSystemOpt);

            state.setStyle(optStyle);
            String currencyCode = optCurrency;
            if (currencyCode != null) {
                IntlUtil.ensureIsWellFormedCurrencyCode(currencyCode);
            }
            if (optStyle.equals(IntlUtil.CURRENCY)) {
                if (currencyCode == null) {
                    throw Errors.createTypeErrorFormat("Currency can not be undefined when style is \"%s\"", IntlUtil.CURRENCY);
                } else {
                    state.setCurrency(IntlUtil.toUpperCase(currencyCode));
                }
            }
            int cDigits = JSNumberFormat.currencyDigits(state.getCurrency());
            int mnfdDefault = cDigits;
            int mxfdDefault = cDigits;
            if (state.getStyle().equals(IntlUtil.CURRENCY)) {
                state.setCurrencyDisplay(optCurrencyDisplay);
            } else {
                mnfdDefault = 0;
                if (state.getStyle().equals(IntlUtil.PERCENT)) {
                    mxfdDefault = 0;
                } else {
                    mxfdDefault = 3;
                }
            }
            JSNumberFormat.setupInternalNumberFormat(state);
            setNumberFormatDigitOptions(state, options, mnfdDefault, mxfdDefault);

            state.setGroupingUsed(getUseGroupingOption.executeValue(options));
        } catch (MissingResourceException e) {
            throw Errors.createICU4JDataError(e);
        }
        return numberFormatObj;
    }

    // https://tc39.github.io/ecma402/#sec-setnfdigitoptions
    private void setNumberFormatDigitOptions(JSNumberFormat.BasicInternalState state, DynamicObject options, int mnfdDefault, int mxfdDefault) {
        Number mnid = getMinIntDigitsOption.executeValue(options, 1, 1);
        Number mnfd = getMinFracDigitsOption.executeValue(options, 0, mnfdDefault);
        int mxfdActualDefault = Math.max(mnfd.intValue(), mxfdDefault);
        Number mxfd = getMaxFracDigitsOption.executeValue(options, mnfd, mxfdActualDefault);
        state.setIntegerAndFractionsDigits(mnid.intValue(), mnfd.intValue(), mxfd.intValue());
        Object mnsd = getMinSignificantDigitsOption.getValue(options);
        Object mxsd = getMaxSignificantDigitsOption.getValue(options);
        if (!JSGuards.isUndefined(mnsd) || !JSGuards.isUndefined(mxsd)) {
            Number mnsdNumber = getMnsdDNO.executeValue(mnsd, 1);
            Number mxsdNumber = getMxsdDNO.executeValue(mxsd, mnsdNumber.intValue());
            state.setSignificantDigits(mnsdNumber.intValue(), mxsdNumber.intValue());
        }
    }
}
