/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.PluralType;

import java.text.ParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Locale;

public final class JSPluralRules extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctions {

    public static final String CLASS_NAME = "PluralRules";
    public static final String PROTOTYPE_NAME = "PluralRules.prototype";

    private static final HiddenKey INTERNAL_STATE_ID = new HiddenKey("_internalState");
    private static final Property INTERNAL_STATE_PROPERTY;

    private static final JSPluralRules INSTANCE = new JSPluralRules();

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        INTERNAL_STATE_PROPERTY = JSObjectUtil.makeHiddenProperty(INTERNAL_STATE_ID, allocator.locationForType(InternalState.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)));
    }

    private JSPluralRules() {
    }

    public static boolean isJSPluralRules(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSPluralRules((DynamicObject) obj);
    }

    public static boolean isJSPluralRules(DynamicObject obj) {
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
    public String getBuiltinToStringTag(DynamicObject object) {
        return "Object";
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject pluralRulesPrototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, pluralRulesPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, pluralRulesPrototype, PROTOTYPE_NAME);
        return pluralRulesPrototype;
    }

    public static Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        assert JSShape.getProtoChildTree(prototype.getShape(), INSTANCE) == null;
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        initialShape = initialShape.addProperty(INTERNAL_STATE_PROPERTY);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static DynamicObject create(JSContext context) {
        InternalState state = new InternalState();
        DynamicObject result = JSObject.create(context, context.getPluralRulesFactory(), state);
        assert isJSPluralRules(result);
        return result;
    }

    @TruffleBoundary
    public static void setLocaleAndType(InternalState state, String[] locales) {
        String selectedTag = IntlUtil.selectedLocale(locales);
        Locale selectedLocale = selectedTag != null ? Locale.forLanguageTag(selectedTag) : Locale.getDefault();
        Locale strippedLocale = selectedLocale.stripExtensions();
        state.locale = strippedLocale.toLanguageTag();
        // see https://tc39.github.io/ecma402/#sec-intl.numberformat-internal-slots (NOTE 1)
        state.javaLocale = IntlUtil.withoutUnicodeExtension(selectedLocale, "cu");
        switch (state.type) {
            case "ordinal":
                state.pluralRules = PluralRules.forLocale(state.javaLocale, PluralType.ORDINAL);
                break;
            case "cardinal":
            default:
                state.pluralRules = PluralRules.forLocale(state.javaLocale, PluralType.CARDINAL);
                break;
        }
        state.pluralCategories.addAll(state.pluralRules.getKeywords());
        state.numberFormat = NumberFormat.getInstance(state.javaLocale);
    }

    @TruffleBoundary
    public static void setSignificantDigits(InternalState state) {
        if (state.numberFormat instanceof DecimalFormat) {
            DecimalFormat df = (DecimalFormat) state.numberFormat;
            df.setMinimumSignificantDigits(state.minimumSignificantDigits.intValue());
            df.setMaximumSignificantDigits(state.maximumSignificantDigits.intValue());
        }
    }

    public static PluralRules getPluralRulesProperty(DynamicObject obj) {
        ensureIsPluralRules(obj);
        return getInternalState(obj).pluralRules;
    }

    public static NumberFormat getNumberFormatProperty(DynamicObject obj) {
        return getInternalState(obj).numberFormat;
    }

    @TruffleBoundary
    public static String select(DynamicObject pluralRulesObj, Object n) {
        PluralRules pluralRules = getPluralRulesProperty(pluralRulesObj);
        NumberFormat numberFormat = getNumberFormatProperty(pluralRulesObj);
        Number x = JSRuntime.toNumber(n);
        String s = numberFormat.format(x);
        try {
            Number toSelect = numberFormat.parse(s);
            return pluralRules.select(toSelect.doubleValue());
        } catch (ParseException pe) {
            return pluralRules.select(x.doubleValue());
        }
    }

    public static class InternalState {

        public boolean initialized = false;
        public PluralRules pluralRules;
        public NumberFormat numberFormat;
        public Locale javaLocale;

        public List<String> pluralCategories = new LinkedList<>();

        DynamicObject boundFormatFunction = null;

        public String locale;
        public String type = "cardinal";
        public Number minimumIntegerDigits = 1;
        public Number minimumFractionDigits = 0;
        public Number maximumFractionDigits = 3;
        public Number minimumSignificantDigits;
        public Number maximumSignificantDigits;
        public String positivePattern = "";
        public String negativePattern = "";

        DynamicObject toResolvedOptionsObject(JSContext context) {
            DynamicObject result = JSUserObject.create(context);
            JSObjectUtil.defineDataProperty(result, "locale", locale, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "type", type, JSAttributes.getDefault());
            if (minimumIntegerDigits != null) {
                JSObjectUtil.defineDataProperty(result, "minimumIntegerDigits", minimumIntegerDigits, JSAttributes.getDefault());
            }
            if (minimumFractionDigits != null) {
                JSObjectUtil.defineDataProperty(result, "minimumFractionDigits", minimumFractionDigits, JSAttributes.getDefault());
            }
            if (maximumFractionDigits != null) {
                JSObjectUtil.defineDataProperty(result, "maximumFractionDigits", maximumFractionDigits, JSAttributes.getDefault());
            }
            if (minimumSignificantDigits != null) {
                JSObjectUtil.defineDataProperty(result, "minimumSignificantDigits", minimumSignificantDigits, JSAttributes.getDefault());
            }
            if (maximumSignificantDigits != null) {
                JSObjectUtil.defineDataProperty(result, "maximumSignificantDigits", maximumSignificantDigits, JSAttributes.getDefault());
            }
            JSObjectUtil.defineDataProperty(result, "pluralCategories", pluralCategories, JSAttributes.getDefault());
            return result;
        }
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, DynamicObject pluralRulesObj) {
        ensureIsPluralRules(pluralRulesObj);
        InternalState state = getInternalState(pluralRulesObj);
        return state.toResolvedOptionsObject(context);
    }

    public static InternalState getInternalState(DynamicObject pluralRulesObj) {
        return (InternalState) INTERNAL_STATE_PROPERTY.get(pluralRulesObj, isJSPluralRules(pluralRulesObj));
    }

    private static void ensureIsPluralRules(Object obj) {
        if (!isJSPluralRules(obj)) {
            throw Errors.createTypeError("PluralRules method called on a non-object or on a wrong type of object (uninitialized PluralRules?).");
        }
    }
}
