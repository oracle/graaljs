/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;

import java.text.Collator;
import java.text.Normalizer;
import java.util.EnumSet;
import java.util.Locale;
import java.util.regex.Pattern;

public final class JSCollator extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctions {

    public static final String CLASS_NAME = "Collator";
    public static final String PROTOTYPE_NAME = "Collator.prototype";

    private static final HiddenKey INTERNAL_STATE_ID = new HiddenKey("_internalState");
    private static final Property INTERNAL_STATE_PROPERTY;

    private static final JSCollator INSTANCE = new JSCollator();

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        INTERNAL_STATE_PROPERTY = JSObjectUtil.makeHiddenProperty(INTERNAL_STATE_ID, allocator.locationForType(InternalState.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)));
    }

    private JSCollator() {
    }

    public static boolean isJSCollator(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSCollator((DynamicObject) obj);
    }

    public static boolean isJSCollator(DynamicObject obj) {
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
        DynamicObject collatorPrototype = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, collatorPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, collatorPrototype, PROTOTYPE_NAME);
        JSObjectUtil.putConstantAccessorProperty(ctx, collatorPrototype, "compare", createCompareFunctionGetter(realm, ctx), Undefined.instance, JSAttributes.configurableNotEnumerableNotWritable());
        return collatorPrototype;
    }

    // localeMatcher unused as our lookup matcher and best fit matcher are the same at the moment
    @TruffleBoundary
    public static void initializeCollator(JSCollator.InternalState state, String[] locales, String usage, @SuppressWarnings("unused") String localeMatcher, Boolean optkn, String optkf,
                    String sensitivity, Boolean ignorePunctuation) {
        Boolean kn = optkn;
        String kf = optkf;
        state.initializedCollator = true;
        state.usage = usage;
        String selectedTag = IntlUtil.selectedLocale(locales);
        Locale selectedLocale = selectedTag != null ? Locale.forLanguageTag(selectedTag) : Locale.getDefault();
        Locale strippedLocale = selectedLocale.stripExtensions();
        for (String ek : selectedLocale.getUnicodeLocaleKeys()) {
            if (kn == null && ek.equals("kn")) {
                String ktype = selectedLocale.getUnicodeLocaleType(ek);
                if (ktype.isEmpty() || ktype.equals("true")) {
                    kn = true;
                }
            }
            if (kf == null && ek.equals("kf")) {
                String ktype = selectedLocale.getUnicodeLocaleType(ek);
                if (!ktype.isEmpty()) {
                    kf = ktype;
                }
            }
        }
        if (kn != null) {
            state.numeric = kn;
        }
        if (kf != null) {
            state.caseFirst = kf;
        }
        if (sensitivity != null) {
            state.sensitivity = sensitivity;
        }
        state.ignorePunctuation = ignorePunctuation;
        state.locale = strippedLocale.toLanguageTag();
        state.collator = Collator.getInstance(Locale.forLanguageTag(state.locale));
        state.collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        switch (state.sensitivity) {
            case "base":
                state.collator.setStrength(Collator.PRIMARY);
                break;
            case "accent":
                state.collator.setStrength(Collator.SECONDARY);
                break;
            case "case":
                state.collator.setStrength(Collator.TERTIARY);
                break;
            case "variant":
                state.collator.setStrength(Collator.IDENTICAL);
                break;
        }
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
        DynamicObject result = JSObject.create(context, context.getCollatorFactory(), state);
        assert isJSCollator(result);
        return result;
    }

    public static Collator getCollatorProperty(DynamicObject obj) {
        return getInternalState(obj).collator;
    }

    @TruffleBoundary
    public static int compare(DynamicObject collatorObj, Object one, Object two) {
        Collator collator = getCollatorProperty(collatorObj);
        return collator.compare(one, two);
    }

    @TruffleBoundary
    public static int caseSensitiveCompare(DynamicObject collatorObj, Object one, Object two) {
        Collator collator = getCollatorProperty(collatorObj);
        String a = stripAccents(one.toString());
        String b = stripAccents(two.toString());
        return collator.compare(a, b);
    }

    private static String stripAccents(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder resultBuilder = new StringBuilder(Normalizer.normalize(input, Normalizer.Form.NFD));
        stripLlAccents(resultBuilder);
        Pattern accentMatchingPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return accentMatchingPattern.matcher(resultBuilder).replaceAll("");
    }

    private static void stripLlAccents(StringBuilder s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\u0141') {
                s.setCharAt(i, 'L');
            } else if (s.charAt(i) == '\u0142') {
                s.setCharAt(i, 'l');
            }
        }
    }

    public static class InternalState {

        public boolean initializedCollator = false;
        public Collator collator;

        DynamicObject boundCompareFunction = null;

        public String locale;
        public String usage = "sort";
        public String sensitivity = "variant";
        public String collation = "default";
        public boolean ignorePunctuation = false;
        public boolean numeric = false;
        public String caseFirst = "false";

        DynamicObject toResolvedOptionsObject(JSContext context) {
            DynamicObject result = JSUserObject.create(context);
            JSObjectUtil.defineDataProperty(result, "locale", locale, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "usage", usage, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "sensitivity", sensitivity, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "collation", collation, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "ignorePunctuation", ignorePunctuation, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "numeric", numeric, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, "caseFirst", caseFirst, JSAttributes.getDefault());
            return result;
        }
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, DynamicObject collatorObj) {
        ensureIsCollator(collatorObj);
        InternalState state = getInternalState(collatorObj);
        return state.toResolvedOptionsObject(context);
    }

    public static InternalState getInternalState(DynamicObject collatorObj) {
        return (InternalState) INTERNAL_STATE_PROPERTY.get(collatorObj, isJSCollator(collatorObj));
    }

    private static CallTarget createGetCompareCallTarget(JSRealm realm, JSContext context) {
        return Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {

            @Override
            public Object execute(VirtualFrame frame) {

                Object[] frameArgs = frame.getArguments();
                Object collatorObj = JSArguments.getThisObject(frameArgs);

                if (isJSCollator(collatorObj)) {

                    InternalState state = getInternalState((DynamicObject) collatorObj);

                    if (state == null || !state.initializedCollator) {
                        throw Errors.createTypeError("Method compare called on a non-object or on a wrong type of object (uninitialized collator?).");
                    }

                    if (state.boundCompareFunction == null) {
                        DynamicObject compareFn = state.sensitivity.equals("case") ? createCaseSensitiveCompareFunction(realm, context) : createCompareFunction(realm, context);
                        DynamicObject boundFn = JSFunction.boundFunctionCreate(context, realm, compareFn, collatorObj, new Object[]{}, JSObject.getPrototype(compareFn), true);
                        state.boundCompareFunction = boundFn;
                    }

                    return state.boundCompareFunction;
                }
                throw Errors.createTypeError("expected collator object");
            }
        });
    }

    private static void ensureIsCollator(Object obj) {
        if (!isJSCollator(obj)) {
            throw Errors.createTypeError("Collator method called on a non-object or on a wrong type of object (uninitialized collator?).");
        }
    }

    private static DynamicObject createCompareFunction(JSRealm realm, JSContext context) {
        DynamicObject result = JSFunction.create(realm, JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                DynamicObject thisObj = JSRuntime.toObject(context, JSArguments.getThisObject(arguments));
                Object one = JSArguments.getUserArgument(arguments, 0);
                Object two = JSArguments.getUserArgument(arguments, 1);
                return compare(thisObj, one, two);
            }
        }), 2, "compare"));
        return result;
    }

    private static DynamicObject createCaseSensitiveCompareFunction(JSRealm realm, JSContext context) {
        DynamicObject result = JSFunction.create(realm, JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                DynamicObject thisObj = JSRuntime.toObject(context, JSArguments.getThisObject(arguments));
                Object one = JSArguments.getUserArgumentCount(arguments) > 0 ? JSArguments.getUserArgument(arguments, 0) : Undefined.instance;
                Object two = JSArguments.getUserArgumentCount(arguments) > 1 ? JSArguments.getUserArgument(arguments, 1) : Undefined.instance;
                return caseSensitiveCompare(thisObj, one, two);
            }
        }), 2, "compare"));
        return result;
    }

    private static DynamicObject createCompareFunctionGetter(JSRealm realm, JSContext context) {
        CallTarget ct = createGetCompareCallTarget(realm, context);
        JSFunctionData fd = JSFunctionData.create(context, ct, ct, 0, "get compare", false, false, false, true);
        DynamicObject compareFunction = JSFunction.create(realm, fd);
        return compareFunction;
    }
}
