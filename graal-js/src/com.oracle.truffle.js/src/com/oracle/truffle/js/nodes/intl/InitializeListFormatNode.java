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
import com.oracle.truffle.js.runtime.builtins.intl.JSListFormat;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public abstract class InitializeListFormatNode extends JavaScriptBaseNode {

    private final JSContext context;

    @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;
    @Child CoerceOptionsToObjectNode coerceOptionsToObjectNode;

    @Child GetStringOptionNode getLocaleMatcherOption;

    @Child GetStringOptionNode getTypeOption;
    @Child GetStringOptionNode getStyleOption;
    private final BranchProfile errorBranch = BranchProfile.create();

    protected InitializeListFormatNode(JSContext context) {
        this.context = context;
        this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        this.coerceOptionsToObjectNode = CoerceOptionsToObjectNodeGen.create(context);
        this.getTypeOption = GetStringOptionNode.create(context, IntlUtil.TYPE, new String[]{IntlUtil.CONJUNCTION, IntlUtil.DISJUNCTION, IntlUtil.UNIT}, IntlUtil.CONJUNCTION);
        this.getStyleOption = GetStringOptionNode.create(context, IntlUtil.STYLE, new String[]{IntlUtil.LONG, IntlUtil.SHORT, IntlUtil.NARROW}, IntlUtil.LONG);
        this.getLocaleMatcherOption = GetStringOptionNode.create(context, IntlUtil.LOCALE_MATCHER,
                        new String[]{IntlUtil.LOOKUP, IntlUtil.BEST_FIT}, IntlUtil.BEST_FIT);
    }

    public abstract DynamicObject executeInit(DynamicObject collator, Object locales, Object options);

    public static InitializeListFormatNode createInitalizeListFormatNode(JSContext context) {
        return InitializeListFormatNodeGen.create(context);
    }

    @Specialization
    public DynamicObject initializeListFormat(DynamicObject listFormatObj, Object localesArg, Object optionsArg) {

        // must be invoked before any code that tries to access ICU library data
        try {

            JSListFormat.InternalState state = JSListFormat.getInternalState(listFormatObj);

            String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(localesArg);
            DynamicObject options = coerceOptionsToObjectNode.execute(optionsArg);

            getLocaleMatcherOption.executeValue(options);
            String optType = getTypeOption.executeValue(options);
            String optStyle = getStyleOption.executeValue(options);

            state.setType(optType);
            state.setStyle(optStyle);

            JSListFormat.setLocale(context, state, locales);
            JSListFormat.setupInternalListFormatter(state);

        } catch (MissingResourceException e) {
            errorBranch.enter();
            throw Errors.createICU4JDataError(e);
        }

        return listFormatObj;
    }
}
