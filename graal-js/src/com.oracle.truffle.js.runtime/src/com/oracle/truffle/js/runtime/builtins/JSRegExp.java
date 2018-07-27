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

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetRegexResult;

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.RegexCompilerInterface;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexMaterializeResultNode;

public final class JSRegExp extends JSBuiltinObject implements JSConstructorFactory.Default {

    public static final JSRegExp INSTANCE = new JSRegExp();

    public static final String CLASS_NAME = "RegExp";
    public static final String PROTOTYPE_NAME = CLASS_NAME + ".prototype";
    public static final String MULTILINE = "multiline";
    public static final String GLOBAL = "global";
    public static final String IGNORE_CASE = "ignoreCase";
    public static final String STICKY = "sticky";
    public static final String UNICODE = "unicode";
    public static final String DOT_ALL = "dotAll";
    public static final String SOURCE = "source";
    public static final String FLAGS = "flags";
    public static final String LAST_INDEX = "lastIndex";
    public static final String INPUT = "input";
    public static final String GROUPS = "groups";

    public static final String PROTOTYPE_GETTER_NAME = PROTOTYPE_NAME + " getter";

    private static final HiddenKey COMPILED_REGEX_ID = new HiddenKey("compiledRegex");
    private static final Property COMPILED_REGEX_PROPERTY;
    private static final HiddenKey GROUPS_FACTORY_ID = new HiddenKey("groupsFactory");
    private static final Property GROUPS_FACTORY_PROPERTY;

    private static final Property LAZY_INDEX_PROXY = JSObjectUtil.makeProxyProperty("index", new LazyRegexResultIndexProxyProperty(), JSAttributes.getDefault());

    // A pointer from the `groups` object of a regex result back to the regex result.
    // Needed to calculate the contents of the `groups` object lazily.
    public static final HiddenKey GROUPS_RESULT_ID = new HiddenKey("regexResult");
    private static final Property GROUPS_RESULT_PROPERTY;

    private static final TRegexUtil.TRegexCompiledRegexSingleFlagAccessor STATIC_MULTILINE_ACCESSOR = TRegexUtil.TRegexCompiledRegexSingleFlagAccessor.create(TRegexUtil.Props.Flags.MULTILINE);
    private static final TRegexUtil.TRegexResultAccessor STATIC_RESULT_ACCESSOR = TRegexUtil.TRegexResultAccessor.create();

    /**
     * Since we cannot use nodes here, access to this property is special-cased in
     * {@code com.oracle.truffle.js.nodes.access.PropertyGetNode.LazyRegexResultIndexPropertyGetNode}
     * .
     */
    public static class LazyRegexResultIndexProxyProperty implements PropertyProxy {

        private final Node readStartArrayNode = TRegexUtil.createReadNode();
        private final Node readStartArrayElementNode = TRegexUtil.createReadNode();

        @TruffleBoundary
        @Override
        public Object get(DynamicObject object) {
            return TRegexUtil.readResultStartIndex(readStartArrayNode, readStartArrayElementNode, arrayGetRegexResult(object), 0);
        }

        @TruffleBoundary
        @Override
        public boolean set(DynamicObject object, Object value) {
            JSObjectUtil.defineDataProperty(object, "index", value, JSAttributes.getDefault());
            return true;
        }
    }

    public static class LazyNamedCaptureGroupProperty implements PropertyProxy {

        private final String groupName;
        private final int groupIndex;

        public LazyNamedCaptureGroupProperty(String groupName, int groupIndex) {
            this.groupName = groupName;
            this.groupIndex = groupIndex;
        }

        public int getGroupIndex() {
            return groupIndex;
        }

        private final TRegexMaterializeResultNode materializeNode = TRegexMaterializeResultNode.create();

        @Override
        public Object get(DynamicObject object) {
            TruffleObject regexResult = (TruffleObject) GROUPS_RESULT_PROPERTY.get(object, false);
            return materializeNode.materializeGroup(regexResult, groupIndex);
        }

        @Override
        public boolean set(DynamicObject object, Object value) {
            JSObjectUtil.defineDataProperty(object, groupName, value, JSAttributes.getDefault());
            return true;
        }
    }

    static {
        Shape.Allocator regExpAllocator = JSShape.makeAllocator(JSObject.LAYOUT);
        COMPILED_REGEX_PROPERTY = JSObjectUtil.makeHiddenProperty(COMPILED_REGEX_ID, regExpAllocator.locationForType(TruffleObject.class, EnumSet.of(LocationModifier.NonNull)));
        GROUPS_FACTORY_PROPERTY = JSObjectUtil.makeHiddenProperty(GROUPS_FACTORY_ID, regExpAllocator.locationForType(DynamicObjectFactory.class));

        Shape.Allocator resultAllocator = JSShape.makeAllocator(JSObject.LAYOUT);
        GROUPS_RESULT_PROPERTY = JSObjectUtil.makeHiddenProperty(GROUPS_RESULT_ID, resultAllocator.locationForType(TruffleObject.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    private JSRegExp() {
    }

    public static TruffleObject getCompiledRegex(DynamicObject thisObj) {
        assert isJSRegExp(thisObj);
        return (TruffleObject) COMPILED_REGEX_PROPERTY.get(thisObj, isJSRegExp(thisObj));
    }

    public static DynamicObjectFactory getGroupsFactory(DynamicObject thisObj) {
        assert isJSRegExp(thisObj);
        return (DynamicObjectFactory) GROUPS_FACTORY_PROPERTY.get(thisObj, isJSRegExp(thisObj));
    }

    /**
     * Creates a new JavaScript RegExp object (with a {@code lastIndex} of 0).
     * <p>
     * This overload incurs hitting a {@link TruffleBoundary} when having to examine the
     * {@code compiledRegex} for information about named capture groups. In order to avoid a
     * {@link TruffleBoundary} in cases when your regular expression has no named capture groups,
     * consider using the {@code com.oracle.truffle.js.nodes.intl.CreateRegExpNode}.
     */
    public static DynamicObject create(JSContext ctx, TruffleObject compiledRegex) {
        DynamicObject obj = create(ctx, compiledRegex, computeGroupsFactory(ctx, compiledRegex));
        JSObjectUtil.putDataProperty(ctx, obj, LAST_INDEX, 0, JSAttributes.notConfigurableNotEnumerableWritable());
        return obj;
    }

    /**
     * Creates a new JavaScript RegExp object <em>without</em> a {@code lastIndex} property.
     */
    public static DynamicObject create(JSContext ctx, TruffleObject compiledRegex, DynamicObjectFactory groupsFactory) {
        // (compiledRegex, groupsFactory)
        DynamicObject regExp = JSObject.create(ctx, ctx.getRegExpFactory(), compiledRegex, groupsFactory);
        assert isJSRegExp(regExp);
        return regExp;
    }

    private static void initialize(JSContext ctx, DynamicObject regExp, TruffleObject regex) {
        COMPILED_REGEX_PROPERTY.setSafe(regExp, regex, null);
        GROUPS_FACTORY_PROPERTY.setSafe(regExp, computeGroupsFactory(ctx, regex), null);
    }

    public static void updateCompilation(JSContext ctx, DynamicObject thisObj, TruffleObject regex) {
        assert isJSRegExp(thisObj) && regex != null;
        initialize(ctx, thisObj, regex);
    }

    @TruffleBoundary
    private static DynamicObjectFactory computeGroupsFactory(JSContext ctx, TruffleObject compiledRegex) {
        TruffleObject namedCaptureGroups = TRegexUtil.readNamedCaptureGroups(JSInteropUtil.createRead(), compiledRegex);
        if (JSInteropNodeUtil.isNull(namedCaptureGroups)) {
            return null;
        } else {
            return buildGroupsFactory(ctx, namedCaptureGroups);
        }
    }

    @TruffleBoundary
    public static DynamicObjectFactory buildGroupsFactory(JSContext ctx, TruffleObject namedCaptureGroups) {
        Shape groupsShape = ctx.getEmptyShape();
        groupsShape = groupsShape.addProperty(GROUPS_RESULT_PROPERTY);
        for (Object key : JSInteropNodeUtil.keys(namedCaptureGroups)) {
            String groupName = (String) key;
            int groupIndex = ((Number) JSInteropNodeUtil.read(namedCaptureGroups, groupName)).intValue();
            Property groupProperty = JSObjectUtil.makeProxyProperty(groupName, new LazyNamedCaptureGroupProperty(groupName, groupIndex), JSAttributes.getDefault());
            groupsShape = groupsShape.addProperty(groupProperty);
        }
        return groupsShape.createFactory();
    }

    /**
     * Format: '/' pattern '/' flags, flags may contain 'g' (global), 'i' (ignore case) and 'm'
     * (multiline).<br>
     * Example: <code>/ab*c/gi</code>
     */
    @TruffleBoundary
    public static String prototypeToString(DynamicObject thisObj) {
        TruffleObject regex = getCompiledRegex(thisObj);
        String pattern = (String) JSInteropNodeUtil.readRaw(regex, TRegexUtil.Props.CompiledRegex.PATTERN);
        if (pattern.length() == 0) {
            pattern = "(?:)";
        }
        String flags = (String) JSInteropNodeUtil.readRaw((TruffleObject) JSInteropNodeUtil.readRaw(regex, TRegexUtil.Props.CompiledRegex.FLAGS), TRegexUtil.Props.Flags.SOURCE);
        return "/" + pattern + '/' + flags;
    }

    // non-standard according to ES2015, 7.2.8 IsRegExp (@@match check missing)
    public static boolean isJSRegExp(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSRegExp((DynamicObject) obj);
    }

    // non-standard according to ES2015, 7.2.8 IsRegExp (@@match check missing)
    public static boolean isJSRegExp(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObject.create(realm, realm.getObjectPrototype(), ctx.getEcmaScriptVersion() < 6 ? JSRegExp.INSTANCE : JSUserObject.INSTANCE);

        if (ctx.getEcmaScriptVersion() < 6) {
            JSObjectUtil.putHiddenProperty(prototype, COMPILED_REGEX_PROPERTY, RegexCompilerInterface.compile("", "", ctx));
            JSObjectUtil.putDataProperty(ctx, prototype, LAST_INDEX, 0, JSAttributes.notConfigurableNotEnumerableWritable());
        }
        JSObjectUtil.putConstantAccessorProperty(ctx, prototype, FLAGS, realm.lookupFunction(PROTOTYPE_GETTER_NAME, "get " + FLAGS), Undefined.instance);

        putRegExpPropertyAccessor(realm, prototype, MULTILINE,
                        new CompiledRegexFlagPropertyAccessor(prototype, TRegexUtil.Props.Flags.MULTILINE, Undefined.instance));
        putRegExpPropertyAccessor(realm, prototype, GLOBAL,
                        new CompiledRegexFlagPropertyAccessor(prototype, TRegexUtil.Props.Flags.GLOBAL, Undefined.instance));
        putRegExpPropertyAccessor(realm, prototype, IGNORE_CASE,
                        new CompiledRegexFlagPropertyAccessor(prototype, TRegexUtil.Props.Flags.IGNORE_CASE, Undefined.instance));
        putRegExpPropertyAccessor(realm, prototype, SOURCE, new CompiledRegexPatternAccessor(prototype));

        if (ctx.getEcmaScriptVersion() >= 6) {
            putRegExpPropertyAccessor(realm, prototype, STICKY,
                            new CompiledRegexFlagPropertyAccessor(prototype, TRegexUtil.Props.Flags.STICKY, Undefined.instance));
            putRegExpPropertyAccessor(realm, prototype, UNICODE,
                            new CompiledRegexFlagPropertyAccessor(prototype, TRegexUtil.Props.Flags.UNICODE, Undefined.instance));
        }

        if (ctx.getEcmaScriptVersion() >= 9) {
            putRegExpPropertyAccessor(realm, prototype, DOT_ALL,
                            new CompiledRegexFlagPropertyAccessor(prototype, TRegexUtil.Props.Flags.DOT_ALL, Undefined.instance));
        }

        // ctor and functions
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, PROTOTYPE_NAME);

        return prototype;
    }

    public static Shape makeInitialShape(JSContext ctx, DynamicObject thisObj) {
        assert JSShape.getProtoChildTree(thisObj.getShape(), INSTANCE) == null;
        // @formatter:off
        return JSObjectUtil.getProtoChildShape(thisObj, INSTANCE, ctx).
                        addProperty(COMPILED_REGEX_PROPERTY).
                        addProperty(GROUPS_FACTORY_PROPERTY);
        // @formatter:on
    }

    public static Shape makeInitialShapeLazyArray(JSContext ctx, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSArray.INSTANCE, ctx);
        initialShape = JSArray.addArrayProperties(initialShape);
        initialShape = initialShape.addProperty(JSAbstractArray.LAZY_REGEX_RESULT_PROPERTY);
        Shape.Allocator allocator = initialShape.allocator();
        final Property inputProperty = JSObjectUtil.makeDataProperty(JSRegExp.INPUT, allocator.locationForType(String.class, EnumSet.of(LocationModifier.NonNull)), JSAttributes.getDefault());
        initialShape = initialShape.addProperty(inputProperty);
        initialShape = initialShape.addProperty(JSArray.makeArrayLengthProxyProperty());
        initialShape = initialShape.addProperty(LAZY_INDEX_PROXY);
        Property groupsProperty = JSObjectUtil.makeDataProperty(GROUPS, allocator.locationForType(DynamicObject.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)),
                        JSAttributes.getDefault());
        initialShape = initialShape.addProperty(groupsProperty);
        return initialShape;
    }

    @Override
    public void fillConstructor(JSRealm realm, DynamicObject constructor) {
        final JSContext context = realm.getContext();
        putConstructorSpeciesGetter(realm, constructor);
        if (context.isOptionRegexpStaticResult()) {
            Object defaultValue = "";

            RegExpPropertyGetter getInput = obj -> {
                TruffleObject result = context.getRealm().getRegexResult();
                return staticResultGetInput(defaultValue, result);
            };
            RegExpPropertyGetter getMultiline = obj -> {
                TruffleObject result = context.getRealm().getRegexResult();
                return staticResultGetMultiline(false, result);
            };
            RegExpPropertyGetter getLastMatch = obj -> {
                TruffleObject result = context.getRealm().getRegexResult();
                return staticResultGetLastMatch(defaultValue, result);
            };
            RegExpPropertyGetter getLastParen = obj -> {
                TruffleObject result = context.getRealm().getRegexResult();
                return staticResultGetLastParen(defaultValue, result);
            };
            RegExpPropertyGetter getLeftContext = obj -> {
                TruffleObject result = context.getRealm().getRegexResult();
                return staticResultGetLeftContext(defaultValue, result);
            };
            RegExpPropertyGetter getRightContext = obj -> {
                TruffleObject result = context.getRealm().getRegexResult();
                return staticResultGetRightContext(defaultValue, result);
            };

            putRegExpStaticResultPropertyAccessor(realm, constructor, "input", getInput);
            if (!JSTruffleOptions.NashornCompatibilityMode) {
                putRegExpStaticResultPropertyAccessor(realm, constructor, "$_", getInput);
            }

            if (JSTruffleOptions.NashornCompatibilityMode) {
                putRegExpStaticResultPropertyAccessor(realm, constructor, "multiline", getMultiline);
            }

            putRegExpStaticResultPropertyAccessor(realm, constructor, "lastMatch", getLastMatch);
            if (!JSTruffleOptions.NashornCompatibilityMode) {
                putRegExpStaticResultPropertyAccessor(realm, constructor, "$&", getLastMatch);
            }

            putRegExpStaticResultPropertyAccessor(realm, constructor, "lastParen", getLastParen);
            if (!JSTruffleOptions.NashornCompatibilityMode) {
                putRegExpStaticResultPropertyAccessor(realm, constructor, "$+", getLastParen);
            }

            putRegExpStaticResultPropertyAccessor(realm, constructor, "leftContext", getLeftContext);
            if (!JSTruffleOptions.NashornCompatibilityMode) {
                putRegExpStaticResultPropertyAccessor(realm, constructor, "$`", getLeftContext);
            }

            putRegExpStaticResultPropertyAccessor(realm, constructor, "rightContext", getRightContext);
            if (!JSTruffleOptions.NashornCompatibilityMode) {
                putRegExpStaticResultPropertyAccessor(realm, constructor, "$'", getRightContext);
            }

            for (int i = 1; i <= 9; i++) {
                putRegExpStaticResultPropertyWithIndexAccessor(realm, constructor, "$" + i, i);
            }
        }
    }

    @TruffleBoundary
    private static Object staticResultGetInput(Object defaultValue, TruffleObject result) {
        if (STATIC_RESULT_ACCESSOR.isMatch(result)) {
            return STATIC_RESULT_ACCESSOR.input(result);
        } else {
            return defaultValue;
        }
    }

    @TruffleBoundary
    private static Object staticResultGetMultiline(Object defaultValue, TruffleObject result) {
        if (!JSTruffleOptions.NashornCompatibilityMode && STATIC_RESULT_ACCESSOR.isMatch(result)) {
            return STATIC_MULTILINE_ACCESSOR.get(STATIC_RESULT_ACCESSOR.regex(result));
        } else {
            return defaultValue;
        }
    }

    @TruffleBoundary
    private static Object staticResultGetLastMatch(Object defaultValue, TruffleObject result) {
        if (STATIC_RESULT_ACCESSOR.isMatch(result)) {
            return Boundaries.substring(STATIC_RESULT_ACCESSOR.input(result), STATIC_RESULT_ACCESSOR.captureGroupStart(result, 0), STATIC_RESULT_ACCESSOR.captureGroupEnd(result, 0));
        } else {
            return defaultValue;
        }
    }

    @TruffleBoundary
    private static Object staticResultGetLastParen(Object defaultValue, TruffleObject result) {
        if (STATIC_RESULT_ACCESSOR.isMatch(result)) {
            int groupNumber = STATIC_RESULT_ACCESSOR.groupCount(result) - 1;
            if (groupNumber > 0) {
                int start = STATIC_RESULT_ACCESSOR.captureGroupStart(result, groupNumber);
                if (start >= 0) {
                    return Boundaries.substring(STATIC_RESULT_ACCESSOR.input(result), start, STATIC_RESULT_ACCESSOR.captureGroupEnd(result, groupNumber));
                }
            }
            return defaultValue;
        } else {
            return defaultValue;
        }
    }

    @TruffleBoundary
    private static Object staticResultGetLeftContext(Object defaultValue, TruffleObject result) {
        if (STATIC_RESULT_ACCESSOR.isMatch(result)) {
            int start = STATIC_RESULT_ACCESSOR.captureGroupStart(result, 0);
            return Boundaries.substring(STATIC_RESULT_ACCESSOR.input(result), 0, start);
        } else {
            return defaultValue;
        }
    }

    @TruffleBoundary
    private static Object staticResultGetRightContext(Object defaultValue, TruffleObject result) {
        if (STATIC_RESULT_ACCESSOR.isMatch(result)) {
            int end = STATIC_RESULT_ACCESSOR.captureGroupEnd(result, 0);
            return Boundaries.substring(STATIC_RESULT_ACCESSOR.input(result), end);
        } else {
            return defaultValue;
        }
    }

    private static void putRegExpStaticResultPropertyAccessor(JSRealm realm, DynamicObject prototype, String name, RegExpPropertyGetter getterImpl) {
        JSContext ctx = realm.getContext();
        DynamicObject getter = JSFunction.create(realm, JSFunctionData.createCallOnly(ctx, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(ctx.getLanguage(), null, null) {

            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = JSArguments.getThisObject(frame.getArguments());
                DynamicObject view = JSObject.castJSObject(obj);
                return getterImpl.get(view);
            }
        }), 0, "get " + name));
        DynamicObject setter = JSFunction.create(realm, JSFunctionData.createCallOnly(ctx, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(ctx.getLanguage(), null, null) {

            @Override
            public Object execute(VirtualFrame frame) {
                return Undefined.instance;
            }
        }), 0, "set " + name));
        JSObjectUtil.putConstantAccessorProperty(ctx, prototype, name, getter, setter, getRegExpStaticResultPropertyAccessorJSAttributes());
    }

    private static void putRegExpStaticResultPropertyWithIndexAccessor(JSRealm realm, DynamicObject prototype, String name, int index) {
        JSContext ctx = realm.getContext();
        DynamicObject getter = JSFunction.create(realm, JSFunctionData.createCallOnly(ctx, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(ctx.getLanguage(), null, null) {

            @Child TRegexUtil.TRegexResultAccessor resultAccessor = TRegexUtil.TRegexResultAccessor.create();

            @Override
            public Object execute(VirtualFrame frame) {
                TruffleObject result = ctx.getRealm().getRegexResult();
                if (resultAccessor.isMatch(result) && resultAccessor.groupCount(result) > index) {
                    int start = resultAccessor.captureGroupStart(result, index);
                    if (start >= 0) {
                        return Boundaries.substring(resultAccessor.input(result), start, resultAccessor.captureGroupEnd(result, index));
                    }
                }
                return "";
            }
        }), 0, "get " + name));
        DynamicObject setter = JSFunction.create(realm, JSFunctionData.createCallOnly(ctx, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(ctx.getLanguage(), null, null) {

            @Override
            public Object execute(VirtualFrame frame) {
                return Undefined.instance;
            }
        }), 0, "set " + name));
        JSObjectUtil.putConstantAccessorProperty(ctx, prototype, name, getter, setter, getRegExpStaticResultPropertyAccessorJSAttributes());
    }

    // https://github.com/tc39/proposal-regexp-legacy-features#additional-properties-of-the-regexp-constructor
    private static int getRegExpStaticResultPropertyAccessorJSAttributes() {
        return JSTruffleOptions.NashornCompatibilityMode ? JSAttributes.notConfigurableEnumerableWritable() : JSAttributes.configurableNotEnumerableWritable();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
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
        return getClassName(object);
    }

    @Override
    @TruffleBoundary
    public String safeToString(DynamicObject obj) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return "[RegExp " + prototypeToString(obj) + "]";
        } else {
            return prototypeToString(obj);
        }
    }

    interface RegExpPropertyGetter {
        Object get(DynamicObject obj);
    }

    private static class CompiledRegexPatternAccessor extends JavaScriptRootNode {

        private static final String DEFAULT_RETURN = "(?:)";

        private final ConditionProfile isObject = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isRegExp = ConditionProfile.createBinaryProfile();
        private final Object regexPrototype;
        @Child Node readNode = Message.READ.createNode();

        CompiledRegexPatternAccessor(Object regexPrototype) {
            this.regexPrototype = regexPrototype;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object obj = JSArguments.getThisObject(frame.getArguments());
            if (isObject.profile(JSObject.isDynamicObject(obj))) {
                DynamicObject view = JSObject.castJSObject(obj);
                if (isRegExp.profile(isJSRegExp(view))) {
                    return escapeRegExpPattern(TRegexUtil.readPattern(readNode, getCompiledRegex(view)));
                } else if (obj == regexPrototype) {
                    return DEFAULT_RETURN;
                }
            }
            throw Errors.createTypeErrorIncompatibleReceiver(obj);
        }
    }

    private static class CompiledRegexFlagPropertyAccessor extends JavaScriptRootNode {

        private final ConditionProfile isObject = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isRegExp = ConditionProfile.createBinaryProfile();
        private final Object regexPrototype;
        private final Object defaultReturn;
        @Child TRegexUtil.TRegexCompiledRegexSingleFlagAccessor readNode;

        CompiledRegexFlagPropertyAccessor(Object regexPrototype, String flagName, Object defaultReturn) {
            this.regexPrototype = regexPrototype;
            this.defaultReturn = defaultReturn;
            readNode = TRegexUtil.TRegexCompiledRegexSingleFlagAccessor.create(flagName);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object obj = JSArguments.getThisObject(frame.getArguments());
            if (isObject.profile(JSObject.isDynamicObject(obj))) {
                DynamicObject view = JSObject.castJSObject(obj);
                if (isRegExp.profile(isJSRegExp(view))) {
                    return readNode.get(getCompiledRegex(view));
                } else if (obj == regexPrototype) {
                    return defaultReturn;
                }
            }
            throw Errors.createTypeErrorIncompatibleReceiver(obj);
        }
    }

    private static void putRegExpPropertyAccessor(JSRealm realm, DynamicObject prototype, String name, JavaScriptRootNode accessor) {
        JSContext ctx = realm.getContext();
        DynamicObject getter = JSFunction.create(realm, JSFunctionData.createCallOnly(ctx, Truffle.getRuntime().createCallTarget(accessor), 0, "get " + name));
        JSObjectUtil.putConstantAccessorProperty(ctx, prototype, name, getter, Undefined.instance);
    }

    @TruffleBoundary
    private static Object escapeRegExpPattern(CharSequence pattern) {
        if (pattern.length() == 0) {
            return "(?:)";
        }
        int extraChars = escapeRegExpExtraCharCount(pattern);
        if (extraChars == 0) {
            return pattern;
        } else {
            return escapeRegExpPattern(pattern, extraChars);
        }
    }

    /**
     * Returns the number of extra characters that need to be inserted into {@code pattern} in order
     * for it to be correctly escaped for use in a RegExp literal (according to the requirements of
     * EscapeRegExpPattern).
     *
     * This method satisfies the following property: if its return value is 0, the pattern does not
     * need to be modified by EscapeRegExpPattern. In order to satisfy this property, this method
     * can sometimes return a result that is 1 higher than the advertised value. This is the case
     * when the pattern needs escaping but none of the escapes actually prolong the pattern, as in
     * {@code "\\\n"}, which is escaped as {@code "\\n"} and where both the original and the escaped
     * pattern are of length 2.
     */
    private static int escapeRegExpExtraCharCount(CharSequence pattern) {
        // The body of this method mirrors that of escapeRegExpPattern. However, instead of actually
        // allocating and filling a new StringBuilder, it only scans the input pattern and takes
        // note of any characters that will need to be escaped.
        int extraChars = 0;
        boolean insideCharClass = false;
        int i = 0;
        while (i < pattern.length()) {
            switch (pattern.charAt(i)) {
                case '\\':
                    assert i + 1 < pattern.length();
                    i++;
                    switch (pattern.charAt(i)) {
                        case '\n':
                        case '\r':
                            // We are replacing "\\\n" with "\\n" or "\\\r" with "\\r". We are not
                            // adding any extra characters but we are still modifying the pattern.
                            // Therefore, we make sure that resulting value extraChars is at least
                            // 1.
                            extraChars = Math.max(extraChars, 1);
                            break;
                        case '\u2028':
                        case '\u2029':
                            extraChars += 4;
                            break;
                    }
                    break;
                case '\n':
                case '\r':
                    extraChars += 1;
                    break;
                case '\u2028':
                case '\u2029':
                    extraChars += 5;
                    break;
                case '/':
                    if (!insideCharClass) {
                        extraChars += 1;
                    }
                    break;
                case '[':
                    insideCharClass = true;
                    break;
                case ']':
                    insideCharClass = false;
                    break;
            }
            i++;
        }
        return extraChars;
    }

    /**
     * Implements the EscapeRegExpPattern abstract operation from the ECMAScript spec.
     *
     * @param pattern the input pattern, which is assumed to be non-empty
     * @param extraChars an estimate on the difference of sizes between the original pattern and the
     *            escaped pattern
     * @return the escaped pattern
     */
    @TruffleBoundary
    private static Object escapeRegExpPattern(CharSequence pattern, int extraChars) {
        StringBuilder sb = new StringBuilder(pattern.length() + extraChars);
        boolean insideCharClass = false;
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            switch (c) {
                case '\\':
                    assert i + 1 < pattern.length();
                    sb.append(c);
                    i++;
                    c = pattern.charAt(i);
                    // The patterns used in RegExp objects can not only have literal LineTerminators
                    // (e.g. RegExp("\n")), they can also have identity escapes of literal
                    // LineTerminators (e.g. RegExp("\\\n")) (note that this is only valid when the
                    // Unicode flag is not present). Since LineTerminators are not allowed in RegExp
                    // literals, we have to replace these identity escapes with other escapes.
                    switch (c) {
                        case '\n':
                            sb.append('n');
                            break;
                        case '\r':
                            sb.append('r');
                            break;
                        case '\u2028':
                            sb.append("u2028");
                            break;
                        case '\u2029':
                            sb.append("u2029");
                            break;
                        default:
                            sb.append(c);
                    }
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\u2028':
                    sb.append("\\u2028");
                    break;
                case '\u2029':
                    sb.append("\\u2029");
                    break;
                case '/':
                    // According to the syntax of RegularExpressionLiterals, forward slashes are
                    // allowed inside character classes and therefore do not have to be escaped.
                    if (!insideCharClass) {
                        sb.append("\\/");
                    } else {
                        sb.append('/');
                    }
                    break;
                case '[':
                    insideCharClass = true;
                    sb.append(c);
                    break;
                case ']':
                    insideCharClass = false;
                    sb.append(c);
                    break;
                default:
                    sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }
}
