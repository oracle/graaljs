/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins.intl;

import java.util.Locale;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.util.IntlUtil;

/**
 * Common subclass of internal states of internationalization-related built-ins that use numbering
 * systems.
 */
public abstract class AbstractInternalState {
    protected Locale javaLocale;
    protected String locale;
    protected String numberingSystem;

    @TruffleBoundary
    public void resolveLocaleAndNumberingSystem(JSContext ctx, String[] locales, String numberingSystemOpt) {
        Locale selectedLocale = IntlUtil.selectedLocale(ctx, locales);
        Locale strippedLocale = selectedLocale.stripExtensions();
        if (strippedLocale.toLanguageTag().equals(IntlUtil.UND)) {
            selectedLocale = ctx.getLocale();
            strippedLocale = selectedLocale.stripExtensions();
        }
        Locale.Builder builder = new Locale.Builder();
        builder.setLocale(strippedLocale);

        String nuType = selectedLocale.getUnicodeLocaleType("nu");
        if (!IntlUtil.isValidNumberingSystem(nuType)) {
            nuType = null;
        }
        String nuOpt = numberingSystemOpt;
        if (!IntlUtil.isValidNumberingSystem(nuOpt) || Objects.equals(nuType, nuOpt)) {
            nuOpt = null;
        }
        if (nuOpt == null && nuType != null) {
            numberingSystem = nuType;
            builder.setUnicodeLocaleKeyword("nu", nuType);
        }

        this.locale = builder.build().toLanguageTag();

        if (nuOpt != null) {
            numberingSystem = nuOpt;
            builder.setUnicodeLocaleKeyword("nu", nuOpt);
        }

        this.javaLocale = builder.build();

        if (this.numberingSystem == null) {
            this.numberingSystem = IntlUtil.defaultNumberingSystemName(ctx, this.javaLocale);
        }
    }

}
