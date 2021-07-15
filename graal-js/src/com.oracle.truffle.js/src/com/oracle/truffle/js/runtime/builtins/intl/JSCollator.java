/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.intl.CollatorFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.CollatorPrototypeBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public final class JSCollator extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String CLASS_NAME = "Collator";
    public static final String PROTOTYPE_NAME = "Collator.prototype";

    static final HiddenKey BOUND_OBJECT_KEY = new HiddenKey(CLASS_NAME);

    public static final JSCollator INSTANCE = new JSCollator();

    // Valid values of Unicode collation ("co") type key.
    // Based on https://github.com/unicode-org/cldr/blob/master/common/bcp47/collation.xml
    // "standard" and "search" are missing from the list because ECMAScript spec says:
    // The values "standard" and "search" must not be used as elements
    // in any [[SortLocaleData]].[[<locale>]].[[co]]
    // and [[SearchLocaleData]].[[<locale>]].[[co]] list.
    private static final Set<String> VALID_COLLATION_TYPES = new HashSet<>(Arrays.asList(new String[]{
                    "big5han",
                    "compat",
                    "dict",
                    "direct",
                    "ducet",
                    "emoji",
                    "eor",
                    "gb2312",
                    "phonebk",
                    "phonetic",
                    "pinyin",
                    "reformed",
                    "searchjl",
                    "stroke",
                    "trad",
                    "unihan",
                    "zhuyin"
    }));

    private JSCollator() {
    }

    public static boolean isJSCollator(Object obj) {
        return obj instanceof JSCollatorObject;
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
        DynamicObject collatorPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, collatorPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, collatorPrototype, CollatorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putBuiltinAccessorProperty(collatorPrototype, "compare", createCompareFunctionGetter(realm, ctx), Undefined.instance);
        JSObjectUtil.putToStringTag(collatorPrototype, "Intl.Collator");
        return collatorPrototype;
    }

    // localeMatcher unused as our lookup matcher and best fit matcher are the same at the moment
    @TruffleBoundary
    public static void initializeCollator(JSContext ctx, JSCollator.InternalState state, String[] locales, String usage, @SuppressWarnings("unused") String localeMatcher, String optco, Boolean optkn,
                    String optkf, String sensitivity, Boolean ignorePunctuation) {
        state.initializedCollator = true;
        state.usage = usage;
        Locale selectedLocale = IntlUtil.selectedLocale(ctx, locales);
        Locale strippedLocale = selectedLocale.stripExtensions();
        Locale.Builder builder = new Locale.Builder().setLocale(strippedLocale);

        Boolean kn = optkn;
        if (kn == null) {
            String knType = selectedLocale.getUnicodeLocaleType("kn");
            if ("".equals(knType) || "true".equals(knType)) {
                kn = true;
            } else if ("false".equals(knType)) {
                kn = false;
            }
            if (kn != null) {
                // "BCP 47 Language Tag to Unicode BCP 47 Locale Identifier" algorithm
                // used during CanonicalizeLanguageTag() operation requires the removal
                // of "true" value of a unicode extension i.e. -u-kn-true should be converted to
                // -u-kn.
                String value = kn ? "" : "false";
                builder.setUnicodeLocaleKeyword("kn", value);
            }
        }
        if (kn != null) {
            state.numeric = kn;
        }

        String kf = optkf;
        if (kf == null) {
            String kfType = selectedLocale.getUnicodeLocaleType("kf");
            if ("upper".equals(kfType) || "lower".equals(kfType) || "false".equals(kfType)) {
                kf = kfType;
                builder.setUnicodeLocaleKeyword("kf", kfType);
            }
        }
        if (kf != null) {
            state.caseFirst = kf;
        }

        String collation = (optco == null) ? selectedLocale.getUnicodeLocaleType("co") : optco;
        if (!VALID_COLLATION_TYPES.contains(collation)) {
            collation = null;
        }

        // "search" maps to -u-co-search, "sort" means the default behavior
        boolean searchUsage = IntlUtil.SEARCH.equals(usage);
        if (!searchUsage && collation != null) {
            state.collation = collation;
            builder.setUnicodeLocaleKeyword("co", collation);
        }

        if (sensitivity != null) {
            state.sensitivity = sensitivity;
        }
        state.ignorePunctuation = ignorePunctuation;
        Locale collatorLocale = builder.build();
        state.locale = collatorLocale.toLanguageTag();

        // "search" is not allowed in r.[[co]] but it must be set in the Locale
        // used by the Collator (so that the Collator uses "search" collation).
        if (searchUsage) {
            collatorLocale = builder.setUnicodeLocaleKeyword("co", IntlUtil.SEARCH).build();
        }

        state.collator = Collator.getInstance(collatorLocale);
        state.collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        switch (state.sensitivity) {
            case IntlUtil.BASE:
                state.collator.setStrength(Collator.PRIMARY);
                break;
            case IntlUtil.ACCENT:
                state.collator.setStrength(Collator.SECONDARY);
                break;
            case IntlUtil.CASE:
                state.collator.setStrength(Collator.PRIMARY);
                if (state.collator instanceof RuleBasedCollator) {
                    ((RuleBasedCollator) state.collator).setCaseLevel(true);
                }
                break;
            case IntlUtil.VARIANT:
                state.collator.setStrength(Collator.TERTIARY);
                break;
        }
        if (state.ignorePunctuation) {
            if (state.collator instanceof RuleBasedCollator) {
                ((RuleBasedCollator) state.collator).setAlternateHandlingShifted(true);
            }
        }
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, CollatorFunctionBuiltins.BUILTINS);
    }

    public static DynamicObject create(JSContext context) {
        InternalState state = new InternalState();
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getCollatorFactory();
        JSCollatorObject obj = new JSCollatorObject(factory.getShape(realm), state);
        factory.initProto(obj, realm);
        assert isJSCollator(obj);
        return context.trackAllocation(obj);
    }

    public static Collator getCollatorProperty(DynamicObject obj) {
        return getInternalState(obj).collator;
    }

    @TruffleBoundary
    public static int compare(DynamicObject collatorObj, String one, String two) {
        Collator collator = getCollatorProperty(collatorObj);
        return collator.compare(normalize(one), normalize(two));
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD);
    }

    public static class InternalState {

        private boolean initializedCollator = false;
        private Collator collator;

        private DynamicObject boundCompareFunction = null;

        private String locale;
        private String usage = IntlUtil.SORT;
        private String sensitivity = IntlUtil.VARIANT;
        private String collation = IntlUtil.DEFAULT;
        private boolean ignorePunctuation = false;
        private boolean numeric = false;
        private String caseFirst = IntlUtil.FALSE;

        DynamicObject toResolvedOptionsObject(JSContext context) {
            DynamicObject result = JSOrdinary.create(context);
            JSObjectUtil.defineDataProperty(result, IntlUtil.LOCALE, locale, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, IntlUtil.USAGE, usage, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, IntlUtil.SENSITIVITY, sensitivity, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, IntlUtil.IGNORE_PUNCTUATION, ignorePunctuation, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, IntlUtil.COLLATION, collation, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, IntlUtil.NUMERIC, numeric, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, IntlUtil.CASE_FIRST, caseFirst, JSAttributes.getDefault());
            return result;
        }
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, DynamicObject collatorObj) {
        InternalState state = getInternalState(collatorObj);
        return state.toResolvedOptionsObject(context);
    }

    public static InternalState getInternalState(DynamicObject collatorObj) {
        assert isJSCollator(collatorObj);
        return ((JSCollatorObject) collatorObj).getInternalState();
    }

    private static CallTarget createGetCompareCallTarget(JSContext context) {
        return Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {

            @CompilationFinal private ContextReference<JSRealm> realmRef;
            @Child private PropertySetNode setBoundObjectNode = PropertySetNode.createSetHidden(BOUND_OBJECT_KEY, context);
            private final BranchProfile errorBranch = BranchProfile.create();

            @Override
            public Object execute(VirtualFrame frame) {

                Object[] frameArgs = frame.getArguments();
                Object collatorObj = JSArguments.getThisObject(frameArgs);

                if (isJSCollator(collatorObj)) {

                    InternalState state = getInternalState((DynamicObject) collatorObj);

                    if (state == null || !state.initializedCollator) {
                        errorBranch.enter();
                        throw Errors.createTypeErrorMethodCalledOnNonObjectOrWrongType("compare");
                    }

                    if (state.boundCompareFunction == null) {
                        if (realmRef == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            realmRef = lookupContextReference(JavaScriptLanguage.class);
                        }
                        JSRealm realm = realmRef.get();
                        JSFunctionData compareFunctionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.CollatorCompare, c -> createCompareFunctionData(c));
                        DynamicObject compareFn = JSFunction.create(realm, compareFunctionData);
                        setBoundObjectNode.setValue(compareFn, collatorObj);
                        state.boundCompareFunction = compareFn;
                    }

                    return state.boundCompareFunction;
                }
                errorBranch.enter();
                throw Errors.createTypeErrorTypeXExpected(CLASS_NAME);
            }
        });
    }

    private static JSFunctionData createCompareFunctionData(JSContext context) {
        return JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private PropertyGetNode getBoundObjectNode = PropertyGetNode.createGetHidden(BOUND_OBJECT_KEY, context);
            @Child private JSToStringNode toString1Node = JSToStringNode.create();
            @Child private JSToStringNode toString2Node = JSToStringNode.create();

            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                DynamicObject thisObj = (DynamicObject) getBoundObjectNode.getValue(JSArguments.getFunctionObject(arguments));
                assert isJSCollator(thisObj);
                int argumentCount = JSArguments.getUserArgumentCount(arguments);
                String one = (argumentCount > 0) ? toString1Node.executeString(JSArguments.getUserArgument(arguments, 0)) : Undefined.NAME;
                String two = (argumentCount > 1) ? toString2Node.executeString(JSArguments.getUserArgument(arguments, 1)) : Undefined.NAME;
                return compare(thisObj, one, two);
            }
        }), 2, "");
    }

    private static DynamicObject createCompareFunctionGetter(JSRealm realm, JSContext context) {
        JSFunctionData fd = realm.getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.CollatorGetCompare, (c) -> {
            CallTarget ct = createGetCompareCallTarget(context);
            return JSFunctionData.create(context, ct, ct, 0, "get compare", false, false, false, true);
        });
        return JSFunction.create(realm, fd);
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getCollatorPrototype();
    }
}
