/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.LinkedList;
import java.util.List;

import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.number.FormattedNumberRange;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.LocalizedNumberRangeFormatter;
import com.ibm.icu.number.NumberRangeFormatter;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.PluralType;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.intl.PluralRulesFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.PluralRulesPrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public final class JSPluralRules extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("PluralRules");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("PluralRules.prototype");
    public static final TruffleString TO_STRING_TAG = Strings.constant("Intl.PluralRules");

    public static final JSPluralRules INSTANCE = new JSPluralRules();

    private JSPluralRules() {
    }

    public static boolean isJSPluralRules(Object obj) {
        return obj instanceof JSPluralRulesObject;
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public TruffleString getClassName(JSDynamicObject object) {
        return getClassName();
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSContext ctx = realm.getContext();
        JSObject pluralRulesPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, pluralRulesPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, pluralRulesPrototype, PluralRulesPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(pluralRulesPrototype, TO_STRING_TAG);
        return pluralRulesPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, PluralRulesFunctionBuiltins.BUILTINS);
    }

    public static JSPluralRulesObject create(JSContext context, JSRealm realm) {
        InternalState state = new InternalState();
        JSObjectFactory factory = context.getPluralRulesFactory();
        JSPluralRulesObject obj = new JSPluralRulesObject(factory.getShape(realm), state);
        factory.initProto(obj, realm);
        assert isJSPluralRules(obj);
        return context.trackAllocation(obj);
    }

    public static PluralRules getPluralRulesProperty(JSDynamicObject obj) {
        return getInternalState(obj).getPluralRules();
    }

    public static LocalizedNumberFormatter getNumberFormatter(JSDynamicObject obj) {
        return getInternalState(obj).getNumberFormatter();
    }

    @TruffleBoundary
    public static TruffleString select(JSDynamicObject pluralRulesObj, Object n) {
        PluralRules pluralRules = getPluralRulesProperty(pluralRulesObj);
        LocalizedNumberFormatter numberFormatter = getNumberFormatter(pluralRulesObj);
        Number number = JSRuntime.toNumber(n);
        FormattedNumber formattedNumber = numberFormatter.format(number);
        return Strings.fromJavaString(pluralRules.select(formattedNumber));
    }

    @TruffleBoundary
    public static TruffleString selectRange(JSDynamicObject pluralRulesObj, double x, double y) {
        PluralRules pluralRules = getPluralRulesProperty(pluralRulesObj);
        LocalizedNumberRangeFormatter rangeFormatter = getInternalState(pluralRulesObj).getNumberRangeFormatter();
        FormattedNumberRange formattedRange = rangeFormatter.formatRange(x, y);
        return Strings.fromJavaString(pluralRules.select(formattedRange));
    }

    public static class InternalState extends JSNumberFormat.BasicInternalState {
        private LocalizedNumberFormatter numberFormatter;
        private LocalizedNumberRangeFormatter numberRangeFormatter;

        private String type;
        private PluralRules pluralRules;
        private final List<TruffleString> pluralCategories = new LinkedList<>();

        @Override
        void fillResolvedOptions(JSContext context, JSRealm realm, JSDynamicObject result) {
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.KEY_LOCALE, Strings.fromJavaString(getLocale()), JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.KEY_TYPE, Strings.fromJavaString(type), JSAttributes.getDefault());
            super.fillResolvedOptions(context, realm, result);
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.KEY_PLURAL_CATEGORIES, JSRuntime.createArrayFromList(realm.getContext(), realm, pluralCategories), JSAttributes.getDefault());
            String roundingType = getRoundingType();
            String resolvedRoundingType = (IntlUtil.MORE_PRECISION.equals(roundingType) || IntlUtil.LESS_PRECISION.equals(roundingType)) ? roundingType : IntlUtil.AUTO;
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.KEY_ROUNDING_PRIORITY, Strings.fromJavaString(resolvedRoundingType), JSAttributes.getDefault());
        }

        @TruffleBoundary
        public void initializePluralRules() {
            pluralRules = PluralRules.forLocale(getJavaLocale(), IntlUtil.ORDINAL.equals(type) ? PluralType.ORDINAL : PluralType.CARDINAL);
            for (String keyword : pluralRules.getKeywords()) {
                pluralCategories.add(Strings.fromJavaString(keyword));
            }
        }

        @Override
        @TruffleBoundary
        public void initializeNumberFormatter() {
            super.initializeNumberFormatter();
            numberFormatter = getUnlocalizedFormatter().locale(getJavaLocale());
            numberRangeFormatter = NumberRangeFormatter.withLocale(getJavaLocale()).numberFormatterBoth(getUnlocalizedFormatter());
        }

        public PluralRules getPluralRules() {
            return pluralRules;
        }

        public LocalizedNumberFormatter getNumberFormatter() {
            return numberFormatter;
        }

        public LocalizedNumberRangeFormatter getNumberRangeFormatter() {
            return numberRangeFormatter;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    @TruffleBoundary
    public static JSDynamicObject resolvedOptions(JSContext context, JSRealm realm, JSDynamicObject pluralRulesObj) {
        InternalState state = getInternalState(pluralRulesObj);
        return state.toResolvedOptionsObject(context, realm);
    }

    public static InternalState getInternalState(JSDynamicObject obj) {
        assert isJSPluralRules(obj);
        return ((JSPluralRulesObject) obj).getInternalState();
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getPluralRulesPrototype();
    }
}
