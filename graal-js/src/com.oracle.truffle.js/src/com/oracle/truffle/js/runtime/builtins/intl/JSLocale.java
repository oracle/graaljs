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
package com.oracle.truffle.js.runtime.builtins.intl;

import java.util.Locale;

import org.graalvm.shadowed.com.ibm.icu.util.ULocale;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public final class JSLocale extends JSNonProxy implements JSConstructorFactory.Default, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("Locale");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Locale.prototype");
    public static final TruffleString TO_STRING_TAG = Strings.constant("Intl.Locale");

    public static final JSLocale INSTANCE = new JSLocale();

    private JSLocale() {
    }

    public static boolean isJSLocale(Object obj) {
        return obj instanceof JSLocaleObject;
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSObject localePrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(localePrototype, ctor);
        JSObjectUtil.putToStringTag(localePrototype, TO_STRING_TAG);
        JSObjectUtil.putFunctionsFromContainer(realm, localePrototype, LocalePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putAccessorsFromContainer(realm, localePrototype, LocalePrototypeBuiltins.BUILTINS);
        return localePrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static JSLocaleObject create(JSContext context, JSRealm realm, JSDynamicObject proto) {
        InternalState state = new InternalState();
        JSObjectFactory factory = context.getLocaleFactory();
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSLocaleObject(shape, proto, state), realm, proto);
        return factory.trackAllocation(newObj);
    }

    public static class InternalState {
        private Locale locale;
        String calendar;
        String caseFirst;
        String collation;
        String firstDayOfWeek;
        String hourCycle;
        boolean numeric;
        String numberingSystem;

        @TruffleBoundary
        public ULocale getULocale() {
            return ULocale.forLocale(locale);
        }

        @TruffleBoundary
        public String getLocale() {
            return IntlUtil.maybeAppendMissingLanguageSubTag(locale.toLanguageTag());
        }

        @TruffleBoundary
        public String getBaseName() {
            return locale.stripExtensions().toLanguageTag();
        }

        public String getCalendar() {
            return calendar;
        }

        public String getCaseFirst() {
            return caseFirst;
        }

        public String getCollation() {
            return collation;
        }

        public String getFirstDayOfWeek() {
            return firstDayOfWeek;
        }

        public String getHourCycle() {
            return hourCycle;
        }

        public boolean getNumeric() {
            return numeric;
        }

        public String getNumberingSystem() {
            return numberingSystem;
        }

        @TruffleBoundary
        public String getLanguage() {
            return locale.getLanguage();
        }

        @TruffleBoundary
        public String getScript() {
            return locale.getScript();
        }

        @TruffleBoundary
        public String getRegion() {
            return locale.getCountry();
        }

        @TruffleBoundary
        public String maximize() {
            // ULocale.addLikelySubtags() tends to add "yes" type to Unicode extensions
            // => use this method to get language/script/region only.
            ULocale max = ULocale.addLikelySubtags(ULocale.forLocale(locale));
            Locale.Builder builder = new Locale.Builder().setLocale(locale);
            builder.setLanguage(max.getLanguage());
            builder.setScript(max.getScript());
            builder.setRegion(max.getCountry());
            return builder.build().toLanguageTag();
        }

        @TruffleBoundary
        public String minimize() {
            // minimize() corresponds to Remove Likely Subtags.
            // This operation is supposed to invoke Add Likely Subtags
            // before the minimization. Unfortunately, ULocale.minimizeSubtags()
            // fails to do so => we invoke addLikelySubtags() explicitly.
            ULocale max = ULocale.addLikelySubtags(ULocale.forLocale(locale));
            // ULocale.minimizeSubtags() tends to add "yes" type to Unicode extensions
            // => use this method to get language/script/region only.
            ULocale min = ULocale.minimizeSubtags(max);
            Locale.Builder builder = new Locale.Builder().setLocale(locale);
            builder.setLanguage(min.getLanguage());
            builder.setScript(min.getScript());
            builder.setRegion(min.getCountry());
            return builder.build().toLanguageTag();
        }
    }

    @TruffleBoundary
    public static void setupInternalState(InternalState state, Locale locale) {
        state.locale = locale;
        state.calendar = locale.getUnicodeLocaleType("ca");
        state.caseFirst = locale.getUnicodeLocaleType("kf");
        state.collation = locale.getUnicodeLocaleType("co");
        state.firstDayOfWeek = locale.getUnicodeLocaleType("fw");
        state.hourCycle = locale.getUnicodeLocaleType("hc");
        String kn = locale.getUnicodeLocaleType("kn");
        state.numeric = "true".equals(kn) || "".equals(kn);
        state.numberingSystem = locale.getUnicodeLocaleType("nu");
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getLocalePrototype();
    }

}
