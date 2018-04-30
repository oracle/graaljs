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
        DynamicObject pluralRulesPrototype = JSObject.create(ctx, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, pluralRulesPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, pluralRulesPrototype, PROTOTYPE_NAME);
        return pluralRulesPrototype;
    }

    public static Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        assert JSShape.getProtoChildTree(prototype, INSTANCE) == null;
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
