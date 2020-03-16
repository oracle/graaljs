/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.intl.SegmentIteratorPrototypeBuiltins;
import com.oracle.truffle.js.builtins.intl.SegmenterFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.SegmenterPrototypeBuiltins;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public final class JSSegmenter extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String CLASS_NAME = "Segmenter";
    public static final String PROTOTYPE_NAME = "Segmenter.prototype";

    private static final HiddenKey INTERNAL_STATE_ID = new HiddenKey("_internalState");
    private static final Property INTERNAL_STATE_PROPERTY;

    public static final String ITERATOR_CLASS_NAME = "Segment Iterator";
    public static final String ITERATOR_PROTOTYPE_NAME = "Segment Iterator.prototype";

    public static final HiddenKey SEGMENT_ITERATOR_GRANULARITY_ID = new HiddenKey("granularity");
    public static final HiddenKey SEGMENT_ITERATOR_SEGMENTER_ID = new HiddenKey("segmenter");
    public static final HiddenKey SEGMENT_ITERATOR_INDEX_ID = new HiddenKey("index");
    public static final HiddenKey SEGMENT_ITERATOR_BREAK_TYPE_ID = new HiddenKey("breakType");

    public static final Property ITERATED_OBJECT_PROPERTY;
    public static final Property SEGMENTER_PROPERTY;
    public static final Property ITER_GRANULARITY_PROPERTY;
    public static final Property BREAK_TYPE_PROPERTY;
    public static final Property INDEX_PROPERTY;

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        INTERNAL_STATE_PROPERTY = JSObjectUtil.makeHiddenProperty(INTERNAL_STATE_ID,
                        allocator.locationForType(InternalState.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)));

        Shape.Allocator iterAllocator = JSShape.makeAllocator(JSObject.LAYOUT);
        ITERATED_OBJECT_PROPERTY = JSObjectUtil.makeHiddenProperty(JSRuntime.ITERATED_OBJECT_ID, iterAllocator.locationForType(String.class));
        SEGMENTER_PROPERTY = JSObjectUtil.makeHiddenProperty(SEGMENT_ITERATOR_SEGMENTER_ID, iterAllocator.locationForType(BreakIterator.class));
        ITER_GRANULARITY_PROPERTY = JSObjectUtil.makeHiddenProperty(SEGMENT_ITERATOR_GRANULARITY_ID, iterAllocator.locationForType(Granularity.class));
        BREAK_TYPE_PROPERTY = JSObjectUtil.makeHiddenProperty(SEGMENT_ITERATOR_BREAK_TYPE_ID, iterAllocator.locationForType(String.class));
        INDEX_PROPERTY = JSObjectUtil.makeHiddenProperty(SEGMENT_ITERATOR_INDEX_ID, iterAllocator.locationForType(Integer.class));
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

    public static final JSSegmenter INSTANCE = new JSSegmenter();

    private JSSegmenter() {
    }

    public static boolean isJSSegmenter(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSSegmenter((DynamicObject) obj);
    }

    public static boolean isJSSegmenter(DynamicObject obj) {
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
        DynamicObject segmenterPrototype = JSObject.createInit(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, segmenterPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, segmenterPrototype, SegmenterPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putDataProperty(ctx, segmenterPrototype, Symbol.SYMBOL_TO_STRING_TAG, "Intl.Segmenter", JSAttributes.configurableNotEnumerableNotWritable());
        return segmenterPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        initialShape = initialShape.addProperty(INTERNAL_STATE_PROPERTY);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, SegmenterFunctionBuiltins.BUILTINS);
    }

    public static DynamicObject create(JSContext context) {
        InternalState state = new InternalState();
        DynamicObject result = JSObject.create(context, context.getSegmenterFactory(), state);
        assert isJSSegmenter(result);
        return result;
    }

    @TruffleBoundary
    public static void setLocale(JSContext ctx, InternalState state, String[] locales) {
        String selectedTag = IntlUtil.selectedLocale(ctx, locales);
        Locale selectedLocale = selectedTag != null ? Locale.forLanguageTag(selectedTag) : ctx.getLocale();
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

        DynamicObject toResolvedOptionsObject(JSContext context) {
            DynamicObject result = JSUserObject.create(context);
            JSObjectUtil.defineDataProperty(result, IntlUtil.LOCALE, locale, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(result, IntlUtil.GRANULARITY, granularity.getName(), JSAttributes.getDefault());
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
    public static DynamicObject resolvedOptions(JSContext context, DynamicObject segmenterObj) {
        InternalState state = getInternalState(segmenterObj);
        return state.toResolvedOptionsObject(context);
    }

    public static InternalState getInternalState(DynamicObject segmenterObj) {
        return (InternalState) INTERNAL_STATE_PROPERTY.get(segmenterObj, isJSSegmenter(segmenterObj));
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getSegmenterPrototype();
    }

    // Iterator

    public static Shape makeInitialSegmentIteratorShape(JSContext ctx, DynamicObject prototype) {
        Shape iteratorShape = JSObjectUtil.getProtoChildShape(prototype, JSUserObject.INSTANCE, ctx);
        iteratorShape = iteratorShape.addProperty(ITERATED_OBJECT_PROPERTY);
        iteratorShape = iteratorShape.addProperty(SEGMENTER_PROPERTY);
        iteratorShape = iteratorShape.addProperty(ITER_GRANULARITY_PROPERTY);
        iteratorShape = iteratorShape.addProperty(BREAK_TYPE_PROPERTY);
        iteratorShape = iteratorShape.addProperty(INDEX_PROPERTY);
        return iteratorShape;
    }

    private static CallTarget createPropertyGetterCallTarget(JSContext context, Property property) {
        return Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private HasHiddenKeyCacheNode isSegmentIteratorNode = HasHiddenKeyCacheNode.create(JSSegmenter.SEGMENT_ITERATOR_GRANULARITY_ID);

            @Override
            public Object execute(VirtualFrame frame) {
                Object obj = JSArguments.getThisObject(frame.getArguments());
                if (isSegmentIteratorNode.executeHasHiddenKey(obj)) {
                    DynamicObject iteratorObj = (DynamicObject) obj;
                    Object result = property.get(iteratorObj, true);
                    return result == null ? Undefined.instance : result;
                }
                throw Errors.createTypeErrorTypeXExpected(ITERATOR_CLASS_NAME);
            }
        });
    }

    /**
     * Creates the %SegmentIteratorPrototype% object.
     */
    public static DynamicObject createSegmentIteratorPrototype(JSContext context, JSRealm realm) {
        DynamicObject prototype = JSObject.createInit(realm, realm.getIteratorPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, SegmentIteratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putDataProperty(context, prototype, Symbol.SYMBOL_TO_STRING_TAG, ITERATOR_CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        JSFunctionData breakTypeFd = realm.getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.SegmeterBreakType, (c) -> {
            CallTarget ct = createPropertyGetterCallTarget(context, BREAK_TYPE_PROPERTY);
            return JSFunctionData.createCallOnly(c, ct, 0, "get " + IntlUtil.BREAK_TYPE);
        });
        DynamicObject breakTypeGetter = JSFunction.create(realm, breakTypeFd);

        JSObjectUtil.putConstantAccessorProperty(context, prototype, IntlUtil.BREAK_TYPE, breakTypeGetter, Undefined.instance);
        JSFunctionData positionFd = realm.getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.SegmeterPosition, (c) -> {
            return JSFunctionData.createCallOnly(context, createPropertyGetterCallTarget(context, INDEX_PROPERTY), 0, "get " + IntlUtil.INDEX);
        });
        DynamicObject positionGetter = JSFunction.create(realm, positionFd);

        JSObjectUtil.putConstantAccessorProperty(context, prototype, IntlUtil.INDEX, positionGetter, Undefined.instance);
        return prototype;
    }
}
