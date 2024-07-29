/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.graalvm.shadowed.com.ibm.icu.impl.ICUResourceBundle;
import org.graalvm.shadowed.com.ibm.icu.text.ConstrainedFieldPosition;
import org.graalvm.shadowed.com.ibm.icu.text.ListFormatter;
import org.graalvm.shadowed.com.ibm.icu.util.ULocale;
import org.graalvm.shadowed.com.ibm.icu.util.UResourceBundle;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.intl.ListFormatFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.ListFormatPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public final class JSListFormat extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("ListFormat");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("ListFormat.prototype");
    public static final TruffleString TO_STRING_TAG = Strings.constant("Intl.ListFormat");

    public static final JSListFormat INSTANCE = new JSListFormat();

    private JSListFormat() {
    }

    public static boolean isJSListFormat(Object obj) {
        return obj instanceof JSListFormatObject;
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
        JSObject listFormatPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(listFormatPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, listFormatPrototype, ListFormatPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(listFormatPrototype, TO_STRING_TAG);
        return listFormatPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, ListFormatFunctionBuiltins.BUILTINS);
    }

    public static JSListFormatObject create(JSContext context, JSRealm realm, JSDynamicObject proto) {
        InternalState state = new InternalState();
        JSObjectFactory factory = context.getListFormatFactory();
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSListFormatObject(shape, proto, state), realm, proto);
        return factory.trackAllocation(newObj);
    }

    @TruffleBoundary
    public static void setLocale(JSContext ctx, InternalState state, String[] locales) {
        Locale selectedLocale = IntlUtil.selectedLocale(ctx, locales);
        Locale strippedLocale = selectedLocale.stripExtensions();
        if (strippedLocale.toLanguageTag().equals(IntlUtil.UND)) {
            selectedLocale = ctx.getLocale();
            strippedLocale = selectedLocale.stripExtensions();
        }
        state.locale = strippedLocale.toLanguageTag();
        state.javaLocale = strippedLocale;
    }

    @TruffleBoundary
    public static void setupInternalListFormatter(InternalState state) {
        state.javaLocale = Locale.forLanguageTag(state.locale);
        state.listFormatter = createFormatter(state.javaLocale, getICUListFormatterStyle(state.type, state.style));
    }

    private static String getICUListFormatterStyle(String type, String style) {
        switch (type) {
            case IntlUtil.CONJUNCTION:
                switch (style) {
                    case IntlUtil.LONG:
                        return IntlUtil.STANDARD;
                    case IntlUtil.NARROW:
                        return IntlUtil.STANDARD_NARROW;
                    case IntlUtil.SHORT:
                        return IntlUtil.STANDARD_SHORT;
                    default:
                        throw Errors.shouldNotReachHere(style);
                }
            case IntlUtil.DISJUNCTION:
                switch (style) {
                    case IntlUtil.LONG:
                        return IntlUtil.OR;
                    case IntlUtil.NARROW:
                        return IntlUtil.OR_NARROW;
                    case IntlUtil.SHORT:
                        return IntlUtil.OR_SHORT;
                    default:
                        throw Errors.shouldNotReachHere(style);
                }
            case IntlUtil.UNIT:
                switch (style) {
                    case IntlUtil.LONG:
                        return IntlUtil.UNIT;
                    case IntlUtil.NARROW:
                        return IntlUtil.UNIT_NARROW;
                    case IntlUtil.SHORT:
                        return IntlUtil.UNIT_SHORT;
                    default:
                        throw Errors.shouldNotReachHere(style);
                }
            default:
                throw Errors.shouldNotReachHere(type);
        }
    }

    public static ListFormatter getListFormatterProperty(JSListFormatObject obj) {
        return obj.getInternalState().listFormatter;
    }

    @TruffleBoundary
    public static TruffleString format(JSListFormatObject listFormatObj, List<String> list) {
        ListFormatter listFormatter = getListFormatterProperty(listFormatObj);
        return Strings.fromJavaString(listFormatter.format(list));
    }

    @TruffleBoundary
    public static JSDynamicObject formatToParts(JSContext context, JSRealm realm, JSListFormatObject listFormatObj, List<String> list) {
        if (list.isEmpty()) {
            return JSArray.createConstantEmptyArray(context, realm);
        }
        ListFormatter listFormatter = getListFormatterProperty(listFormatObj);
        ListFormatter.FormattedList formattedList = listFormatter.formatToValue(list);
        List<Object> resultParts = new ArrayList<>();

        ConstrainedFieldPosition cfPos = new ConstrainedFieldPosition();
        while (formattedList.nextPosition(cfPos)) {
            Format.Field field = cfPos.getField();
            String type;
            if (field == ListFormatter.Field.LITERAL) {
                type = IntlUtil.LITERAL;
            } else if (field == ListFormatter.Field.ELEMENT) {
                type = IntlUtil.ELEMENT;
            } else {
                continue;
            }
            String value = formattedList.subSequence(cfPos.getStart(), cfPos.getLimit()).toString();
            resultParts.add(IntlUtil.makePart(context, realm, type, value));
        }

        return JSArray.createConstant(context, realm, resultParts.toArray());
    }

    public static class InternalState {
        private ListFormatter listFormatter;

        private String locale;
        private Locale javaLocale;

        private String type = IntlUtil.CONJUNCTION;
        private String style = IntlUtil.LONG;

        JSObject toResolvedOptionsObject(JSContext context, JSRealm realm) {
            JSObject result = JSOrdinary.create(context, realm);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_LOCALE, Strings.fromJavaString(locale), JSAttributes.getDefault());
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_TYPE, Strings.fromJavaString(type), JSAttributes.getDefault());
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_STYLE, Strings.fromJavaString(style), JSAttributes.getDefault());
            return result;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setStyle(String style) {
            this.style = style;
        }
    }

    // there is currently no way currently to use any style but standard with the non-deprecated API
    @SuppressWarnings("deprecation")
    private static ListFormatter createFormatter(Locale locale, String style) {
        ULocale ulocale = ULocale.forLocale(locale);
        ICUResourceBundle r = (ICUResourceBundle) UResourceBundle.getBundleInstance(null, ulocale);

        String end = r.getWithFallback("listPattern/" + style + "/end").getString();
        String middle = r.getWithFallback("listPattern/" + style + "/middle").getString();
        String two = r.getWithFallback("listPattern/" + style + "/2").getString();
        String start = r.getWithFallback("listPattern/" + style + "/start").getString();

        return new ListFormatter(two, start, middle, end);
    }

    @TruffleBoundary
    public static JSObject resolvedOptions(JSContext context, JSRealm realm, JSListFormatObject listFormatObj) {
        InternalState state = listFormatObj.getInternalState();
        return state.toResolvedOptionsObject(context, realm);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getListFormatPrototype();
    }
}
