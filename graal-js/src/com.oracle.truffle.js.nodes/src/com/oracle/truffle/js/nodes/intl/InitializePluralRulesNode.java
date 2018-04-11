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
package com.oracle.truffle.js.nodes.intl;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSNumberFormat;
import com.oracle.truffle.js.runtime.builtins.JSPluralRules;

/*
 * https://tc39.github.io/ecma402/#sec-initializepluralrules
 */
public abstract class InitializePluralRulesNode extends JavaScriptBaseNode {

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

        state.type = optType;
        JSNumberFormat.setLocaleAndNumberingSystem(state, locales);
        JSPluralRules.setupInternalPluralRulesAndNumberFormat(state);

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
