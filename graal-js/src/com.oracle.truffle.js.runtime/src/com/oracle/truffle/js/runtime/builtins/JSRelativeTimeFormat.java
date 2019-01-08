/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.text.DisplayContext;
import com.ibm.icu.text.RelativeDateTimeFormatter;
import com.ibm.icu.text.RelativeDateTimeFormatter.RelativeDateTimeUnit;
import com.ibm.icu.util.ULocale;
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

public final class JSRelativeTimeFormat extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String CLASS_NAME = "RelativeTimeFormat";
    public static final String PROTOTYPE_NAME = "RelativeTimeFormat.prototype";

    private static final HiddenKey INTERNAL_STATE_ID = new HiddenKey("_internalState");
    private static final Property INTERNAL_STATE_PROPERTY;

    public static final JSRelativeTimeFormat INSTANCE = new JSRelativeTimeFormat();

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        INTERNAL_STATE_PROPERTY = JSObjectUtil.makeHiddenProperty(INTERNAL_STATE_ID, allocator.locationForType(InternalState.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)));
    }

    private JSRelativeTimeFormat() {
    }

    public static boolean isJSRelativeTimeFormat(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSRelativeTimeFormat((DynamicObject) obj);
    }

    public static boolean isJSRelativeTimeFormat(DynamicObject obj) {
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
        DynamicObject relativeTimeFormatPrototype = JSObject.createInit(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, relativeTimeFormatPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, relativeTimeFormatPrototype, PROTOTYPE_NAME);
        JSObjectUtil.putDataProperty(ctx, relativeTimeFormatPrototype, Symbol.SYMBOL_TO_STRING_TAG, "Intl.RelativeTimeFormat", JSAttributes.configurableNotEnumerableNotWritable());
        return relativeTimeFormatPrototype;
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
        DynamicObject result = JSObject.create(context, context.getRelativeTimeFormatFactory(), state);
        assert isJSRelativeTimeFormat(result);
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
    public static void setupInternalRelativeTimeFormatter(InternalState state) {
        state.javaLocale = Locale.forLanguageTag(state.locale);
        state.relativeDateTimeFormatter = createFormatter(state.javaLocale, state.style);
    }

    public static RelativeDateTimeFormatter getRelativeDateTimeFormatterProperty(DynamicObject obj) {
        return getInternalState(obj).relativeDateTimeFormatter;
    }

    @TruffleBoundary
    public static String format(DynamicObject relativeTimeFormatObj, double amount, String unit) {
        ensureIsRelativeTimeFormat(relativeTimeFormatObj);
        RelativeDateTimeFormatter relativeDateTimeFormatter = getRelativeDateTimeFormatterProperty(relativeTimeFormatObj);
        return relativeDateTimeFormatter.formatNumeric(amount, singularRelativeTimeUnit("format", unit));
    }

    public static class InternalState {

        public boolean initialized = false;
        public RelativeDateTimeFormatter relativeDateTimeFormatter;

        public String locale;
        public Locale javaLocale;

        public String style = "long";
        public String numeric = "always";

        DynamicObject toResolvedOptionsObject(JSContext context) {
            DynamicObject result = JSUserObject.create(context);
            JSObjectUtil.defineDataProperty(result, "locale", locale, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "style", style, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "numeric", numeric, JSAttributes.getDefault());
            return result;
        }
    }

    private static RelativeDateTimeFormatter createFormatter(Locale locale, String style) {
        ULocale ulocale = ULocale.forLocale(locale);
        return RelativeDateTimeFormatter.getInstance(ulocale, null,
                        RelativeDateTimeFormatter.Style.valueOf(style.toUpperCase()), DisplayContext.CAPITALIZATION_NONE);
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, DynamicObject RelativeTimeFormatObj) {
        ensureIsRelativeTimeFormat(RelativeTimeFormatObj);
        InternalState state = getInternalState(RelativeTimeFormatObj);
        return state.toResolvedOptionsObject(context);
    }

    public static InternalState getInternalState(DynamicObject relativeTimeFormatObj) {
        return (InternalState) INTERNAL_STATE_PROPERTY.get(relativeTimeFormatObj, isJSRelativeTimeFormat(relativeTimeFormatObj));
    }

    private static void ensureIsRelativeTimeFormat(Object obj) {
        if (!isJSRelativeTimeFormat(obj)) {
            throw Errors.createTypeError("RelativeTimeFormatter method called on a non-object or on a wrong type of object (uninitialized RelativeTimeFormatter?).");
        }
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getRelativeTimeFormatConstructor().getPrototype();
    }

    private static final Map<String, RelativeDateTimeFormatter.RelativeDateTimeUnit> TimeUnitMAP = new HashMap<>();
    static {
        TimeUnitMAP.put("second", RelativeDateTimeUnit.SECOND);
        TimeUnitMAP.put("seconds", RelativeDateTimeUnit.SECOND);
        TimeUnitMAP.put("minute", RelativeDateTimeUnit.MINUTE);
        TimeUnitMAP.put("minutes", RelativeDateTimeUnit.MINUTE);
        TimeUnitMAP.put("hour", RelativeDateTimeUnit.HOUR);
        TimeUnitMAP.put("hours", RelativeDateTimeUnit.HOUR);
        TimeUnitMAP.put("day", RelativeDateTimeUnit.DAY);
        TimeUnitMAP.put("days", RelativeDateTimeUnit.DAY);
        TimeUnitMAP.put("week", RelativeDateTimeUnit.WEEK);
        TimeUnitMAP.put("weeks", RelativeDateTimeUnit.WEEK);
        TimeUnitMAP.put("month", RelativeDateTimeUnit.MONTH);
        TimeUnitMAP.put("months", RelativeDateTimeUnit.MONTH);
        TimeUnitMAP.put("quarter", RelativeDateTimeUnit.QUARTER);
        TimeUnitMAP.put("quarters", RelativeDateTimeUnit.QUARTER);
        TimeUnitMAP.put("year", RelativeDateTimeUnit.YEAR);
        TimeUnitMAP.put("years", RelativeDateTimeUnit.YEAR);
    }

    private static RelativeDateTimeUnit singularRelativeTimeUnit(String functionName, String unit) {
        RelativeDateTimeUnit result = TimeUnitMAP.get(unit);
        if (result != null) {
            return result;
        } else {
            throw Errors.createRangeErrorInvalidUnitArgument(functionName, unit);
        }
    }
}
