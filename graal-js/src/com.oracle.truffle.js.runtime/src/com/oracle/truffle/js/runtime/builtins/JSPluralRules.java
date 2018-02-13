/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.PluralType;

import java.text.ParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.LinkedList;

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
    public static void setupInternalPluralRulesAndNumberFormat(InternalState state) {
        state.pluralRules = PluralRules.forLocale(state.javaLocale, state.type.equals("ordinal") ? PluralType.ORDINAL : PluralType.CARDINAL);
        state.pluralCategories.addAll(state.pluralRules.getKeywords());
        state.numberFormat = NumberFormat.getInstance(state.javaLocale);
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
            Number toSelectFrom = numberFormat.parse(s);
            return pluralRules.select(JSRuntime.doubleValue(toSelectFrom));
        } catch (ParseException pe) {
            return pluralRules.select(JSRuntime.doubleValue(x));
        }
    }

    public static class InternalState extends JSNumberFormat.BasicInternalState {

        public String type = "cardinal";
        public PluralRules pluralRules;
        public List<Object> pluralCategories = new LinkedList<>();

        @Override
        DynamicObject toResolvedOptionsObject(JSContext context) {
            DynamicObject result = super.toResolvedOptionsObject(context);
            JSObjectUtil.defineDataProperty(result, "type", type, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "pluralCategories", JSRuntime.createArrayFromList(context, pluralCategories), JSAttributes.getDefault());
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
