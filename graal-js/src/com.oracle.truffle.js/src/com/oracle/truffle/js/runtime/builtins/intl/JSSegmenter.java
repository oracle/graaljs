/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.shadowed.com.ibm.icu.text.BreakIterator;
import org.graalvm.shadowed.com.ibm.icu.util.ULocale;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.intl.SegmentIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.intl.SegmenterFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.SegmenterPrototypeBuiltins;
import com.oracle.truffle.js.builtins.intl.SegmentsPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
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

public final class JSSegmenter extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("Segmenter");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Segmenter.prototype");

    public static final TruffleString SEGMENTS_PROTOTYPE_NAME = Strings.constant("Segments.prototype");

    public static final TruffleString ITERATOR_CLASS_NAME = Strings.constant("Segmenter String Iterator");
    public static final TruffleString ITERATOR_PROTOTYPE_NAME = Strings.constant("Segment Iterator.prototype");

    public static final TruffleString TO_STRING_TAG = Strings.constant("Intl.Segmenter");

    public static final JSSegmenter INSTANCE = new JSSegmenter();

    public static class IteratorState {
        private final TruffleString iteratedString;
        private final BreakIterator breakIterator;
        private final Granularity granularity;

        public IteratorState(TruffleString iteratedObject, BreakIterator breakIterator, Granularity granularity) {
            this.iteratedString = iteratedObject;
            this.breakIterator = breakIterator;
            this.granularity = granularity;
        }

        public TruffleString getIteratedString() {
            return iteratedString;
        }

        public Granularity getSegmenterGranularity() {
            return granularity;
        }

        public BreakIterator getBreakIterator() {
            return breakIterator;
        }

    }

    interface IcuIteratorHelper {

        BreakIterator getIterator(ULocale locale);

    }

    public enum Granularity implements IcuIteratorHelper {

        GRAPHEME(IntlUtil.GRAPHEME) {
            @Override
            @TruffleBoundary
            public BreakIterator getIterator(ULocale locale) {
                return BreakIterator.getCharacterInstance(locale);
            }
        },
        WORD(IntlUtil.WORD) {
            @Override
            @TruffleBoundary
            public BreakIterator getIterator(ULocale locale) {
                return BreakIterator.getWordInstance(locale);
            }
        },
        SENTENCE(IntlUtil.SENTENCE) {
            @Override
            @TruffleBoundary
            public BreakIterator getIterator(ULocale locale) {
                return BreakIterator.getSentenceInstance(locale);
            }
        };

        private final String name;

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
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSObject segmenterPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(segmenterPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, segmenterPrototype, SegmenterPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(segmenterPrototype, TO_STRING_TAG);
        return segmenterPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, SegmenterFunctionBuiltins.BUILTINS);
    }

    public static JSSegmenterObject create(JSContext context, JSRealm realm, JSDynamicObject proto) {
        InternalState state = new InternalState();
        JSObjectFactory factory = context.getSegmenterFactory();
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSSegmenterObject(shape, proto, state), realm, proto);
        return factory.trackAllocation(newObj);
    }

    public static JSSegmentIteratorObject createSegmentIterator(JSContext context, JSRealm realm, JSSegmenterObject segmenter, TruffleString value) {
        BreakIterator icuIterator = JSSegmenter.createBreakIterator(segmenter, Strings.toJavaString(value));
        Granularity granularity = JSSegmenter.getGranularity(segmenter);
        JSSegmenter.IteratorState iteratorState = new JSSegmenter.IteratorState(value, icuIterator, granularity);
        JSObjectFactory factory = context.getSegmentIteratorFactory();
        var proto = factory.getPrototype(realm);
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSSegmentIteratorObject(shape, proto, iteratorState), realm, proto);
        return factory.trackAllocation(newObj);
    }

    public static JSSegmentsObject createSegments(JSContext context, JSRealm realm, JSSegmenterObject segmenter, TruffleString string) {
        JSObjectFactory factory = context.getSegmentsFactory();
        var proto = factory.getPrototype(realm);
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSSegmentsObject(shape, proto, segmenter, string), realm, proto);
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

        JSObject toResolvedOptionsObject(JSContext context, JSRealm realm) {
            JSObject result = JSOrdinary.create(context, realm);
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_LOCALE, Strings.fromJavaString(locale), JSAttributes.getDefault());
            JSObjectUtil.putDataProperty(result, IntlUtil.KEY_GRANULARITY, Strings.fromJavaString(granularity.getName()), JSAttributes.getDefault());
            return result;
        }
    }

    @TruffleBoundary
    public static BreakIterator createBreakIterator(JSSegmenterObject segmenterObj) {
        InternalState state = segmenterObj.getInternalState();
        ULocale ulocale = ULocale.forLocale(state.javaLocale);
        BreakIterator icuIterator = state.granularity.getIterator(ulocale);
        return icuIterator;
    }

    @TruffleBoundary
    public static BreakIterator createBreakIterator(JSSegmenterObject segmenterObj, String text) {
        BreakIterator icuIterator = createBreakIterator(segmenterObj);
        icuIterator.setText(text);
        return icuIterator;
    }

    public static Granularity getGranularity(JSSegmenterObject segmenterObj) {
        InternalState state = segmenterObj.getInternalState();
        return state.granularity;
    }

    @TruffleBoundary
    public static JSObject resolvedOptions(JSContext context, JSRealm realm, JSSegmenterObject segmenterObj) {
        InternalState state = segmenterObj.getInternalState();
        return state.toResolvedOptionsObject(context, realm);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getSegmenterPrototype();
    }

    // Segments Object

    public static boolean isJSSegments(Object obj) {
        return obj instanceof JSSegmentsObject;
    }

    public static JSObject createSegmentsPrototype(JSRealm realm) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, SegmentsPrototypeBuiltins.BUILTINS);
        return prototype;
    }

    // Segment Iterator

    public static boolean isJSSegmentIterator(Object obj) {
        return obj instanceof JSSegmentIteratorObject;
    }

    /**
     * Creates the %SegmentIteratorPrototype% object.
     */
    public static JSObject createSegmentIteratorPrototype(JSRealm realm) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm, realm.getIteratorPrototype());
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, SegmentIteratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, ITERATOR_CLASS_NAME);
        return prototype;
    }
}
