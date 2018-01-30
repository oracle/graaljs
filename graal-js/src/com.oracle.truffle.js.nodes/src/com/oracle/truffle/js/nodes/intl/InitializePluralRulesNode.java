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
import com.oracle.truffle.js.runtime.builtins.JSPluralRules;
import com.oracle.truffle.js.runtime.util.IntlUtil;

/*
 * https://tc39.github.io/ecma402/#sec-initializepluralrules
 */
public abstract class InitializePluralRulesNode extends JavaScriptBaseNode {

    @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;
    @Child CreateOptionsObjectNode createOptionsNode;

    @Child GetStringOptionNode getLocaleMatcherOption;
    @Child GetStringOptionNode getTypeOption;
    @Child GetNumberOptionNode getMinIntDigitsOption;
    @Child GetNumberOptionNode getMinFracDigitsOption;
    @Child GetNumberOptionNode getMaxFracDigitsOption;
    @Child PropertyGetNode getMinSignificantDigitsOption;
    @Child PropertyGetNode getMaxSignificantDigitsOption;
    @Child DefaultNumberOptionNode getMnsdDNO;
    @Child DefaultNumberOptionNode getMxsdDNO;

    protected InitializePluralRulesNode(JSContext context) {
        this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        this.createOptionsNode = CreateOptionsObjectNodeGen.create(context);
        this.getLocaleMatcherOption = GetStringOptionNode.create(context, "localeMatcher", new String[]{"lookup", "best fit"}, "best fit");
        this.getTypeOption = GetStringOptionNode.create(context, "type", new String[]{"cardinal", "ordinal"}, "cardinal");
        this.getMinIntDigitsOption = GetNumberOptionNode.create(context, "minimumIntegerDigits", 21);
        this.getMinSignificantDigitsOption = PropertyGetNode.create("minimumSignificantDigits", false, context);
        this.getMaxSignificantDigitsOption = PropertyGetNode.create("maximumSignificantDigits", false, context);
        this.getMinFracDigitsOption = GetNumberOptionNode.create(context, "minimumFractionDigits", 21);
        this.getMaxFracDigitsOption = GetNumberOptionNode.create(context, "maximumFractionDigits", 20);
        this.getMnsdDNO = DefaultNumberOptionNode.create(21, 1);
        this.getMxsdDNO = DefaultNumberOptionNode.create(21, 21);
    }

    public abstract DynamicObject executeInit(DynamicObject collator, Object locales, Object options);

    public static InitializePluralRulesNode createInitalizePluralRulesNode(JSContext context) {
        return InitializePluralRulesNodeGen.create(context);
    }

    @Specialization
    @TruffleBoundary
    public DynamicObject initializePluralRules(DynamicObject pluralRulesObj, Object localesArg, Object optionsArg) {

        JSPluralRules.InternalState state = JSPluralRules.getInternalState(pluralRulesObj);

        String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(localesArg);
        DynamicObject options = createOptionsNode.execute(optionsArg);

        getLocaleMatcherOption.executeValue(options);
        String optType = getTypeOption.executeValue(options);

        state.initialized = true;

        JSPluralRules.setLocaleAndNumberingSystem(state, locales);

        state.type = optType;
        int mnfdDefault = 0;
        int mxfdDefault = 3;
        setPluralRulesDigitOptions(state, options, mnfdDefault, mxfdDefault);
        return pluralRulesObj;
    }

    // https://tc39.github.io/ecma402/#sec-setnfdigitoptions
    private void setPluralRulesDigitOptions(JSPluralRules.InternalState state, DynamicObject options, int mnfdDefault, int mxfdDefault) {
        Number mnid = getMinIntDigitsOption.executeValue(options, 1, 1);
        Number mnfd = getMinFracDigitsOption.executeValue(options, 0, mnfdDefault);
        int mxfdActualDefault = Math.max(mnfd.intValue(), mxfdDefault);
        Number mxfd = getMaxFracDigitsOption.executeValue(options, mnfdDefault, mxfdActualDefault);
        state.minimumIntegerDigits = mnid.intValue();
        state.minimumFractionDigits = mnfd.intValue();
        state.maximumFractionDigits = mxfd.intValue();
        Object mnsd = getMinSignificantDigitsOption.getValue(options);
        Object mxsd = getMaxSignificantDigitsOption.getValue(options);
        if (!JSGuards.isUndefined(mnsd) || !JSGuards.isUndefined(mxsd)) {
            Number mnsdNumber = getMnsdDNO.executeValue(mnsd, 1);
            Number mxsdNumber = getMxsdDNO.executeValue(mxsd, mnsdNumber.intValue());
            state.minimumSignificantDigits = mnsdNumber.intValue();
            state.maximumSignificantDigits = mxsdNumber.intValue();
        }
    }
}
