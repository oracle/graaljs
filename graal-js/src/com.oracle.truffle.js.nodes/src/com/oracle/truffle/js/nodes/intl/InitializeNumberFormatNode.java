/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.intl;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.intl.CreateOptionsObjectNodeGen;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSNumberFormat;
import com.oracle.truffle.js.runtime.util.IntlUtil;

/*
 * https://tc39.github.io/ecma402/#sec-initializenumberformat
 */
public abstract class InitializeNumberFormatNode extends JavaScriptBaseNode {

    @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;
    @Child CreateOptionsObjectNode createOptionsNode;

    @Child GetStringOptionNode getLocaleMatcherOption;

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
        this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        this.createOptionsNode = CreateOptionsObjectNodeGen.create(context);
        this.getLocaleMatcherOption = GetStringOptionNode.create(context, "localeMatcher", new String[]{"lookup", "best fit"}, "best fit");
        this.getStyleOption = GetStringOptionNode.create(context, "style", new String[]{"decimal", "percent", "currency"}, "decimal");
        this.getCurrencyOption = GetStringOptionNode.create(context, "currency", null, null);
        this.getCurrencyDisplayOption = GetStringOptionNode.create(context, "currencyDisplay", new String[]{"code", "symbol", "name"}, "symbol");
        this.getUseGroupingOption = GetBooleanOptionNode.create(context, "useGrouping", true);
        this.getMinIntDigitsOption = GetNumberOptionNode.create(context, "minimumIntegerDigits", 21);
        this.getMinSignificantDigitsOption = PropertyGetNode.create("minimumSignificantDigits", false, context);
        this.getMaxSignificantDigitsOption = PropertyGetNode.create("maximumSignificantDigits", false, context);
        this.getMinFracDigitsOption = GetNumberOptionNode.create(context, "minimumFractionDigits", 21);
        this.getMaxFracDigitsOption = GetNumberOptionNode.create(context, "maximumFractionDigits", 20);
        this.getMnsdDNO = DefaultNumberOptionNode.create(21, 1);
        this.getMxsdDNO = DefaultNumberOptionNode.create(21, 21);
    }

    public abstract DynamicObject executeInit(DynamicObject collator, Object locales, Object options);

    public static InitializeNumberFormatNode createInitalizeNumberFormatNode(JSContext context) {
        return InitializeNumberFormatNodeGen.create(context);
    }

    @Specialization
    @TruffleBoundary
    public DynamicObject initializeNumberFormat(DynamicObject numberFormatObj, Object localesArg, Object optionsArg) {

        JSNumberFormat.InternalState state = JSNumberFormat.getInternalState(numberFormatObj);

        String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(localesArg);
        DynamicObject options = createOptionsNode.execute(optionsArg);

        getLocaleMatcherOption.executeValue(options);
        String optStyle = getStyleOption.executeValue(options);

        String optCurrency = getCurrencyOption.executeValue(options);
        String optCurrencyDisplay = getCurrencyDisplayOption.executeValue(options);
        Boolean optUseGrouping = getUseGroupingOption.executeValue(options);

        state.initialized = true;

        JSNumberFormat.setLocaleAndNumberingSystem(state, locales);

        state.style = optStyle;
        String currencyCode = optCurrency;
        if (currencyCode != null && !JSNumberFormat.isWellFormedCurrencyCode(currencyCode)) {
            throw Errors.createRangeError(String.format("Currency, %s, is not well formed.", currencyCode));
        }
        if (optStyle.equals("currency")) {
            if (currencyCode == null) {
                throw Errors.createTypeError("Currency can not be undefined when style is \"currency\"");
            } else {
                state.currency = IntlUtil.toUpperCase(currencyCode);
            }
        }
        int cDigits = JSNumberFormat.currencyDigits(state.currency);
        int mnfdDefault = cDigits;
        int mxfdDefault = cDigits;
        if (state.style.equals("currency")) {
            state.currencyDisplay = optCurrencyDisplay;
        } else {
            mnfdDefault = 0;
            if (state.style.equals("percent")) {
                mxfdDefault = 0;
            } else {
                mxfdDefault = 3;
            }
        }
        state.useGrouping = optUseGrouping;
        JSNumberFormat.setupInternalNumberFormat(state);
        setNumberFormatDigitOptions(state, options, mnfdDefault, mxfdDefault);
        return numberFormatObj;
    }

    // https://tc39.github.io/ecma402/#sec-setnfdigitoptions
    private void setNumberFormatDigitOptions(JSNumberFormat.BasicInternalState state, DynamicObject options, int mnfdDefault, int mxfdDefault) {
        Number mnid = getMinIntDigitsOption.executeValue(options, 1, 1);
        Number mnfd = getMinFracDigitsOption.executeValue(options, 0, mnfdDefault);
        int mxfdActualDefault = Math.max(mnfd.intValue(), mxfdDefault);
        Number mxfd = getMaxFracDigitsOption.executeValue(options, mnfdDefault, mxfdActualDefault);
        state.minimumIntegerDigits = mnid.intValue();
        state.minimumFractionDigits = mnfd.intValue();
        state.maximumFractionDigits = mxfd.intValue();
        state.numberFormat.setMinimumIntegerDigits(state.minimumIntegerDigits.intValue());
        state.numberFormat.setMinimumFractionDigits(state.minimumFractionDigits.intValue());
        state.numberFormat.setMaximumFractionDigits(state.maximumFractionDigits.intValue());
        Object mnsd = getMinSignificantDigitsOption.getValue(options);
        Object mxsd = getMaxSignificantDigitsOption.getValue(options);
        if (!JSGuards.isUndefined(mnsd) || !JSGuards.isUndefined(mxsd)) {
            Number mnsdNumber = getMnsdDNO.executeValue(mnsd, 1);
            Number mxsdNumber = getMxsdDNO.executeValue(mxsd, mnsdNumber.intValue());
            state.minimumSignificantDigits = mnsdNumber.intValue();
            state.maximumSignificantDigits = mxsdNumber.intValue();
            JSNumberFormat.setSignificantDigits(state);
        }
    }
}
