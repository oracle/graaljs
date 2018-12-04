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
package com.oracle.truffle.js.runtime.builtins;

import java.util.EnumSet;
import java.util.Locale;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public final class JSListFormat extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String CLASS_NAME = "ListFormat";
    public static final String PROTOTYPE_NAME = "ListFormat.prototype";

    private static final HiddenKey INTERNAL_STATE_ID = new HiddenKey("_internalState");
    private static final Property INTERNAL_STATE_PROPERTY;

    public static final JSListFormat INSTANCE = new JSListFormat();

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        INTERNAL_STATE_PROPERTY = JSObjectUtil.makeHiddenProperty(INTERNAL_STATE_ID, allocator.locationForType(InternalState.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)));
    }

    private JSListFormat() {
    }

    public static boolean isJSListFormat(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSListFormat((DynamicObject) obj);
    }

    public static boolean isJSListFormat(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject listFormatPrototype = JSObject.createInit(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, listFormatPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, listFormatPrototype, PROTOTYPE_NAME);
        JSObjectUtil.putDataProperty(ctx, listFormatPrototype, Symbol.SYMBOL_TO_STRING_TAG, "Object", JSAttributes.configurableNotEnumerableNotWritable());
        return listFormatPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        initialShape = initialShape.addProperty(INTERNAL_STATE_PROPERTY);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static DynamicObject create(JSContext context) {
        InternalState state = new InternalState();
        DynamicObject result = JSObject.create(context, context.getListFormatFactory(), state);
        assert isJSListFormat(result);
        return result;
    }

    @TruffleBoundary
    public static void setLocale(JSContext ctx, InternalState state, String[] locales) {
        String selectedTag = IntlUtil.selectedLocale(ctx, locales);
        Locale selectedLocale = selectedTag != null ? Locale.forLanguageTag(selectedTag) : Locale.getDefault();
        Locale strippedLocale = selectedLocale.stripExtensions();
        state.locale = strippedLocale.toLanguageTag();
        state.javaLocale = strippedLocale;
    }

    @TruffleBoundary
    public static void setupInternalListFormatter(InternalState state) {
        state.javaLocale = Locale.forLanguageTag(state.locale);
        state.listFormatter = createFormatter(state.javaLocale, "standard");
    }

    public static ListFormatter getListFormatterProperty(DynamicObject obj) {
        return getInternalState(obj).listFormatter;
    }

    @TruffleBoundary
    public static String format(DynamicObject listFormatObj, Object n) {
        ListFormatter listFormatter = getListFormatterProperty(listFormatObj);
        return listFormatter.format(n);
    }

    @TruffleBoundary
    public static String formatToParts(DynamicObject listFormatObj, Object n) {
        ListFormatter listFormatter = getListFormatterProperty(listFormatObj);
        return listFormatter.format(n);
    }

    public static class InternalState {

        public boolean initialized = false;
        public ListFormatter listFormatter;

        DynamicObject boundCompareFunction = null;

        public String locale;
        public Locale javaLocale;

        public String type = "conjunction";
        public String style = "long";

        DynamicObject toResolvedOptionsObject(JSContext context) {
            DynamicObject result = JSUserObject.create(context);
            JSObjectUtil.defineDataProperty(result, "locale", locale, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "type", type, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "style", style, JSAttributes.getDefault());
            return result;
        }
    }

    @SuppressWarnings("deprecation")
    private static ListFormatter createFormatter(Locale locale, String style) {
        ULocale ulocale = ULocale.forLocale(locale);
        ICUResourceBundle r = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ulocale);

        String end = r.getWithFallback("listPattern/" + style + "/end").getString();
        String middle = r.getWithFallback("listPattern/" + style + "/middle").getString();
        String two = r.getWithFallback("listPattern/" + style + "/2").getString();
        String start = r.getWithFallback("listPattern/" + style + "/start").getString();

        return new ListFormatter(two, start, middle, end);
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, DynamicObject listFormatObj) {
        ensureIsListFormat(listFormatObj);
        InternalState state = getInternalState(listFormatObj);
        return state.toResolvedOptionsObject(context);
    }

    public static InternalState getInternalState(DynamicObject listFormatObj) {
        return (InternalState) INTERNAL_STATE_PROPERTY.get(listFormatObj, isJSListFormat(listFormatObj));
    }

    private static void ensureIsListFormat(Object obj) {
        if (!isJSListFormat(obj)) {
            throw Errors.createTypeError("ListFormatter method called on a non-object or on a wrong type of object (uninitialized ListFormatter?).");
        }
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getListFormatConstructor().getPrototype();
    }
}
