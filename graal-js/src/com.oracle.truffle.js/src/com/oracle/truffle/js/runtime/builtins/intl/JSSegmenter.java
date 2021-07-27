/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.intl.SegmentIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.intl.SegmenterFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.SegmenterPrototypeBuiltins;
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
import com.oracle.truffle.js.runtime.util.CompilableFunction;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public final class JSSegmenter extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String CLASS_NAME = "Segmenter";
    public static final String PROTOTYPE_NAME = "Segmenter.prototype";

    public static final String ITERATOR_CLASS_NAME = "Segment Iterator";
    public static final String ITERATOR_PROTOTYPE_NAME = "Segment Iterator.prototype";

    public static final JSSegmenter INSTANCE = new JSSegmenter();

    public static class IteratorState {
        private String iteratedString;
        private BreakIterator breakIterator;
        private Granularity granularity;
        private String breakType;
        private int index;

        public IteratorState(String iteratedObject, BreakIterator breakIterator, Granularity granularity, String breakType, int index) {
            this.iteratedString = iteratedObject;
            this.breakIterator = breakIterator;
            this.granularity = granularity;
            this.breakType = breakType;
            this.index = index;
        }

        public String getIteratedString() {
            return iteratedString;
        }

        public Granularity getSegmenterGranularity() {
            return granularity;
        }

        public String getBreakType() {
            return breakType;
        }

        public int getIndex() {
            return index;
        }

        public BreakIterator getBreakIterator() {
            return breakIterator;
        }

        public void setIteratedString(String iteratedString) {
            this.iteratedString = iteratedString;
        }

        public void setBreakType(String breakType) {
            this.breakType = breakType;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    interface IcuIteratorHelper {

        BreakIterator getIterator(ULocale locale);

        String getBreakType(int icuStatus);
    }

    public enum Granularity implements IcuIteratorHelper {

        GRAPHEME(IntlUtil.GRAPHEME) {
            @Override
            @TruffleBoundary
            public BreakIterator getIterator(ULocale locale) {
                return BreakIterator.getCharacterInstance(locale);
            }

            @Override
            public String getBreakType(int icuStatus) {
                return null;
            }
        },
        WORD(IntlUtil.WORD) {
            @Override
            @TruffleBoundary
            public BreakIterator getIterator(ULocale locale) {
                return BreakIterator.getWordInstance(locale);
            }

            @Override
            public String getBreakType(int icuStatus) {
                return icuStatus == BreakIterator.WORD_NONE ? IntlUtil.NONE : IntlUtil.WORD;
            }
        },
        SENTENCE(IntlUtil.SENTENCE) {
            @Override
            @TruffleBoundary
            public BreakIterator getIterator(ULocale locale) {
                return BreakIterator.getSentenceInstance(locale);
            }

            @Override
            public String getBreakType(int icuStatus) {
                return icuStatus == BreakIterator.WORD_NONE ? IntlUtil.SEP : IntlUtil.TERM;
            }
        };

        private String name;

        Granularity(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

    private JSSegmenter() {
    }

    public static boolean isJSSegmenter(Object obj) {
        return obj instanceof JSSegmenterObject;
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
        DynamicObject segmenterPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, segmenterPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, segmenterPrototype, SegmenterPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(segmenterPrototype, "Intl.Segmenter");
        return segmenterPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, SegmenterFunctionBuiltins.BUILTINS);
    }

    public static DynamicObject create(JSContext context, JSRealm realm) {
        InternalState state = new InternalState();
        JSObjectFactory factory = context.getSegmenterFactory();
        JSSegmenterObject obj = new JSSegmenterObject(factory.getShape(realm), state);
        factory.initProto(obj, realm);
        assert isJSSegmenter(obj);
        return context.trackAllocation(obj);
    }

    public static DynamicObject createSegmentIterator(JSContext context, JSRealm realm, DynamicObject segmenter, String value) {
        BreakIterator icuIterator = JSSegmenter.createBreakIterator(segmenter, value);
        Granularity granularity = JSSegmenter.getGranularity(segmenter);
        JSSegmenter.IteratorState iteratorState = new JSSegmenter.IteratorState(value, icuIterator, granularity, null, 0);
        JSObjectFactory factory = context.getSegmentIteratorFactory();
        JSSegmenterIteratorObject segmentIterator = new JSSegmenterIteratorObject(factory.getShape(realm), iteratorState);
        factory.initProto(segmentIterator, realm);
        return context.trackAllocation(segmentIterator);
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
    public static void setupInternalBreakIterator(InternalState state, String granularity) {
        state.javaLocale = Locale.forLanguageTag(state.locale);
        switch (granularity) {
            case IntlUtil.GRAPHEME:
                state.granularity = Granularity.GRAPHEME;
                break;
            case IntlUtil.WORD:
                state.granularity = Granularity.WORD;
                break;
            case IntlUtil.SENTENCE:
                state.granularity = Granularity.SENTENCE;
                break;
            default:
                throw Errors.shouldNotReachHere(String.format("Segmenter with granularity, %s, is not supported", granularity));
        }
    }

    public static class InternalState {
        private String locale;
        private Locale javaLocale;

        Granularity granularity = Granularity.GRAPHEME;

        DynamicObject toResolvedOptionsObject(JSContext context, JSRealm realm) {
            DynamicObject result = JSOrdinary.create(context, realm);
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.LOCALE, locale, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.GRANULARITY, granularity.getName(), JSAttributes.getDefault());
            return result;
        }
    }

    @TruffleBoundary
    public static BreakIterator createBreakIterator(DynamicObject segmenterObj, String text) {
        InternalState state = getInternalState(segmenterObj);
        ULocale ulocale = ULocale.forLocale(state.javaLocale);
        BreakIterator icuIterator = state.granularity.getIterator(ulocale);
        icuIterator.setText(text);
        return icuIterator;
    }

    public static Granularity getGranularity(DynamicObject segmenterObj) {
        InternalState state = getInternalState(segmenterObj);
        return state.granularity;
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, JSRealm realm, DynamicObject segmenterObj) {
        InternalState state = getInternalState(segmenterObj);
        return state.toResolvedOptionsObject(context, realm);
    }

    public static InternalState getInternalState(DynamicObject segmenterObj) {
        assert isJSSegmenter(segmenterObj);
        return ((JSSegmenterObject) segmenterObj).getInternalState();
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getSegmenterPrototype();
    }

    // Iterator

    public static Shape makeInitialSegmentIteratorShape(JSContext ctx, DynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, JSOrdinary.BARE_INSTANCE, ctx);
    }

    public static boolean isJSSegmenterIterator(Object obj) {
        return obj instanceof JSSegmenterIteratorObject;
    }

    private static CallTarget createPropertyGetterCallTarget(JSContext context, CompilableFunction<JSSegmenter.IteratorState, Object> getter) {
        return Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {

            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = JSArguments.getThisObject(frame.getArguments());
                if (isJSSegmenterIterator(obj)) {
                    return getter.apply(((JSSegmenterIteratorObject) obj).getIteratorState());
                }
                throw Errors.createTypeErrorTypeXExpected(ITERATOR_CLASS_NAME);
            }
        });
    }

    /**
     * Creates the %SegmentIteratorPrototype% object.
     */
    public static DynamicObject createSegmentIteratorPrototype(JSContext context, JSRealm realm) {
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm, realm.getIteratorPrototype());
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, SegmentIteratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, ITERATOR_CLASS_NAME);
        JSFunctionData breakTypeFd = realm.getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.SegmenterBreakType, (c) -> {
            CallTarget ct = createPropertyGetterCallTarget(context, it -> it.getBreakType() != null ? it.getBreakType() : Undefined.instance);
            return JSFunctionData.createCallOnly(c, ct, 0, "get " + IntlUtil.BREAK_TYPE);
        });
        DynamicObject breakTypeGetter = JSFunction.create(realm, breakTypeFd);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, IntlUtil.BREAK_TYPE, breakTypeGetter, Undefined.instance);
        JSFunctionData positionFd = realm.getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.SegmenterPosition, (c) -> {
            return JSFunctionData.createCallOnly(context, createPropertyGetterCallTarget(context, JSSegmenter.IteratorState::getIndex), 0, "get " + IntlUtil.INDEX);
        });
        DynamicObject positionGetter = JSFunction.create(realm, positionFd);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, IntlUtil.INDEX, positionGetter, Undefined.instance);
        return prototype;
    }
}
