/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.js.runtime.builtins.JSPluralRules;
import com.oracle.truffle.js.runtime.util.IntlUtil;

/*
 * https://tc39.github.io/ecma402/#sec-initializepluralrules
 */
public abstract class InitializePluralRulesNode extends JavaScriptBaseNode {

    private final JSContext context;

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

    @Child GetStringOptionNode getTypeOption;

    protected InitializePluralRulesNode(JSContext context) {
        this.context = context;
        this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        this.createOptionsNode = CreateOptionsObjectNodeGen.create(context);
        this.getLocaleMatcherOption = GetStringOptionNode.create(context, IntlUtil.LOCALE_MATCHER,
                        new String[]{IntlUtil.LOOKUP, IntlUtil.BEST_FIT}, IntlUtil.BEST_FIT);
        this.getTypeOption = GetStringOptionNode.create(context, IntlUtil.TYPE, new String[]{IntlUtil.CARDINAL, IntlUtil.ORDINAL}, IntlUtil.CARDINAL);
        this.getMinIntDigitsOption = GetNumberOptionNode.create(context, IntlUtil.MINIMUM_INTEGER_DIGITS, 21);
        this.getMinSignificantDigitsOption = PropertyGetNode.create(IntlUtil.MINIMUM_SIGNIFICANT_DIGITS, false, context);
        this.getMaxSignificantDigitsOption = PropertyGetNode.create(IntlUtil.MAXIMUM_SIGNIFICANT_DIGITS, false, context);
        this.getMinFracDigitsOption = GetNumberOptionNode.create(context, IntlUtil.MINIMUM_FRACTION_DIGITS, 21);
        this.getMaxFracDigitsOption = GetNumberOptionNode.create(context, IntlUtil.MAXIMUM_FRACTION_DIGITS, 20);
        this.getMnsdDNO = DefaultNumberOptionNode.create(21, 1);
        this.getMxsdDNO = DefaultNumberOptionNode.create(21, 21);
    }

    public abstract DynamicObject executeInit(DynamicObject collator, Object locales, Object options);

    public static InitializePluralRulesNode createInitalizePluralRulesNode(JSContext context) {
        return InitializePluralRulesNodeGen.create(context);
    }

    @Specialization
    public DynamicObject initializePluralRules(DynamicObject pluralRulesObj, Object localesArg, Object optionsArg) {

        // must be invoked before any code that tries to access ICU library data
        try {
            JSPluralRules.InternalState state = JSPluralRules.getInternalState(pluralRulesObj);

            String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(localesArg);
            DynamicObject options = createOptionsNode.execute(optionsArg);

            getLocaleMatcherOption.executeValue(options);
            String optType = getTypeOption.executeValue(options);

            state.setInitialized(true);

            state.setType(optType);

            JSNumberFormat.setLocaleAndNumberingSystem(context, state, locales, null);
            JSPluralRules.setupInternalPluralRulesAndNumberFormat(state);

            int mnfdDefault = 0;
            int mxfdDefault = 3;
            setPluralRulesDigitOptions(state, options, mnfdDefault, mxfdDefault);

        } catch (MissingResourceException e) {
            throw Errors.createICU4JDataError(e);
        }
        return pluralRulesObj;
    }

    // https://tc39.github.io/ecma402/#sec-setnfdigitoptions
    private void setPluralRulesDigitOptions(JSPluralRules.InternalState state, DynamicObject options, int mnfdDefault, int mxfdDefault) {
        Number mnid = getMinIntDigitsOption.executeValue(options, 1, 1);
        Number mnfd = getMinFracDigitsOption.executeValue(options, 0, mnfdDefault);
        int mxfdActualDefault = Math.max(mnfd.intValue(), mxfdDefault);
        Number mxfd = getMaxFracDigitsOption.executeValue(options, mnfdDefault, mxfdActualDefault);
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
