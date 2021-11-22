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
package com.oracle.truffle.js.runtime.builtins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.StringFunctionBuiltins;
import com.oracle.truffle.js.builtins.StringPrototypeBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;
import com.oracle.truffle.js.runtime.util.IteratorUtil;

public final class JSString extends JSPrimitive implements JSConstructorFactory.Default.WithFunctions {

    public static final JSString INSTANCE = new JSString();

    public static final String TYPE_NAME = "string";
    public static final String CLASS_NAME = "String";
    public static final String PROTOTYPE_NAME = "String.prototype";
    public static final String CLASS_NAME_EXTENSIONS = "StringExtensions";

    public static final String LENGTH = "length";

    public static final String ITERATOR_CLASS_NAME = "String Iterator";
    public static final String ITERATOR_PROTOTYPE_NAME = "String Iterator.prototype";
    public static final HiddenKey ITERATED_STRING_ID = new HiddenKey("IteratedString");
    public static final HiddenKey STRING_ITERATOR_NEXT_INDEX_ID = new HiddenKey("StringIteratorNextIndex");

    public static final String REGEXP_ITERATOR_CLASS_NAME = "RegExp String Iterator";
    public static final String REGEXP_ITERATOR_PROTOTYPE_NAME = "RegExp String Iterator.prototype";
    public static final HiddenKey REGEXP_ITERATOR_ITERATING_REGEXP_ID = new HiddenKey("IteratingRegExp");
    public static final HiddenKey REGEXP_ITERATOR_ITERATED_STRING_ID = new HiddenKey("IteratedString");
    public static final HiddenKey REGEXP_ITERATOR_GLOBAL_ID = new HiddenKey("Global");
    public static final HiddenKey REGEXP_ITERATOR_UNICODE_ID = new HiddenKey("Unicode");
    public static final HiddenKey REGEXP_ITERATOR_DONE_ID = new HiddenKey("Done");

    private static final PropertyProxy LENGTH_PROXY = new StringLengthProxyProperty();

    private JSString() {
    }

    public static DynamicObject create(JSContext context, JSRealm realm, CharSequence value) {
        DynamicObject stringObj = JSStringObject.create(realm, context.getStringFactory(), value);
        assert isJSString(stringObj);
        return context.trackAllocation(stringObj);
    }

    @TruffleBoundary
    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        if (super.hasOwnProperty(thisObj, key)) {
            return true;
        }
        long index = JSRuntime.propertyKeyToArrayIndex(key);
        return index >= 0 && index < getStringLength(thisObj);
    }

    public static CharSequence getCharSequence(DynamicObject obj) {
        assert isJSString(obj);
        return ((com.oracle.truffle.js.runtime.builtins.JSStringObject) obj).getCharSequence();
    }

    public static String getString(DynamicObject obj) {
        return Boundaries.stringValueOf(getCharSequence(obj));
    }

    @TruffleBoundary
    public static int getStringLength(DynamicObject obj) {
        assert isJSString(obj);
        return getCharSequence(obj).length();
    }

    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, long index) {
        if (index >= 0 && index < getStringLength(thisObj)) {
            return true;
        }
        return super.hasOwnProperty(thisObj, Boundaries.stringValueOf(index));
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        long value = JSRuntime.propertyKeyToArrayIndex(key);
        if (0 <= value && value < getStringLength(store)) {
            return String.valueOf(getCharSequence(store).charAt((int) value));
        }
        return super.getOwnHelper(store, thisObj, key, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        if (0 <= index && index < getStringLength(store)) {
            return String.valueOf(getCharSequence(store).charAt((int) index));
        }
        return super.getOwnHelper(store, thisObj, Boundaries.stringValueOf(index), encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        if (receiver != thisObj) {
            return ordinarySetWithReceiver(thisObj, key, value, receiver, isStrict, encapsulatingNode);
        }
        long index = JSRuntime.propertyKeyToArrayIndex(key);
        if (index >= 0 && index < getStringLength(thisObj)) {
            // Indexed properties of a String are non-writable and non-configurable.
            if (isStrict) {
                throw Errors.createTypeErrorNotWritableProperty(Boundaries.stringValueOf(index), thisObj);
            }
            return true;
        } else {
            return super.set(thisObj, key, value, receiver, isStrict, encapsulatingNode);
        }
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        if (receiver != thisObj) {
            return ordinarySetWithReceiver(thisObj, Boundaries.stringValueOf(index), value, receiver, isStrict, encapsulatingNode);
        }
        if (index < getStringLength(thisObj)) {
            // Indexed properties of a String are non-writable and non-configurable.
            if (isStrict) {
                throw Errors.createTypeErrorNotWritableProperty(Boundaries.stringValueOf(index), thisObj);
            }
            return true;
        } else {
            return super.set(thisObj, index, value, receiver, isStrict, encapsulatingNode);
        }
    }

    @TruffleBoundary
    @Override
    public List<Object> getOwnPropertyKeys(DynamicObject thisObj, boolean strings, boolean symbols) {
        int len = getStringLength(thisObj);
        List<Object> indices = strings ? ScriptArray.makeRangeList(0, len) : Collections.emptyList();
        List<Object> keyList = thisObj.getShape().getKeyList();
        if (keyList.isEmpty()) {
            return indices;
        } else {
            List<Object> list = new ArrayList<>(keyList.size());
            if (strings) {
                keyList.forEach(k -> {
                    if (k instanceof String && JSRuntime.isArrayIndex((String) k)) {
                        assert JSRuntime.propertyKeyToArrayIndex(k) >= len;
                        list.add(k);
                    }
                });
                Collections.sort(list, JSRuntime::comparePropertyKeys);
                keyList.forEach(k -> {
                    if (k instanceof String && !JSRuntime.isArrayIndex((String) k)) {
                        list.add(k);
                    }
                });
            }
            if (symbols) {
                keyList.forEach(k -> {
                    if (k instanceof Symbol) {
                        list.add(k);
                    }
                });
            }
            return IteratorUtil.concatLists(indices, list);
        }
    }

    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        long idx = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(idx) && ((0 <= idx) && (idx < getStringLength(thisObj)))) {
            if (isStrict) {
                throw Errors.createTypeError("cannot delete index");
            } else {
                return false;
            }
        }
        return deletePropertyDefault(thisObj, key, isStrict);
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        Shape protoShape = JSShape.createPrototypeShape(ctx, INSTANCE, realm.getObjectPrototype());
        DynamicObject prototype = JSStringObject.create(protoShape, "");
        JSObjectUtil.setOrVerifyPrototype(ctx, prototype, realm.getObjectPrototype());

        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        // sets the length just for the prototype
        JSObjectUtil.putDataProperty(ctx, prototype, LENGTH, 0, JSAttributes.notConfigurableNotEnumerableNotWritable());
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, StringPrototypeBuiltins.BUILTINS);
        if (ctx.isOptionNashornCompatibilityMode() || ctx.getParserOptions().getEcmaScriptVersion() >= JSConfig.ECMAScript2019) {
            JSObjectUtil.putFunctionsFromContainer(realm, prototype, StringPrototypeBuiltins.EXTENSION_BUILTINS);
        }
        if (ctx.isOptionAnnexB()) {
            // trimLeft/trimRight are the same objects as trimStart/trimEnd
            Object trimStart = JSObject.get(prototype, "trimStart");
            Object trimEnd = JSObject.get(prototype, "trimEnd");
            JSObjectUtil.putDataProperty(ctx, prototype, "trimLeft", trimStart, JSAttributes.configurableNotEnumerableWritable());
            JSObjectUtil.putDataProperty(ctx, prototype, "trimRight", trimEnd, JSAttributes.configurableNotEnumerableWritable());
        }
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSString.INSTANCE, context);
        initialShape = Shape.newBuilder(initialShape).addConstantProperty(LENGTH, LENGTH_PROXY, JSAttributes.notConfigurableNotEnumerableNotWritable() | JSProperty.PROXY).build();
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, StringFunctionBuiltins.BUILTINS);
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

    @TruffleBoundary
    @Override
    public String toDisplayStringImpl(DynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return "[" + CLASS_NAME + " " + getCharSequence(obj) + "]";
        } else {
            String primitiveValue = JSString.getString(obj);
            return JSRuntime.objectToDisplayString(obj, allowSideEffects, format, depth,
                            getBuiltinToStringTag(obj), new String[]{JSRuntime.PRIMITIVE_VALUE}, new Object[]{primitiveValue});
        }
    }

    public static boolean isJSString(Object obj) {
        return obj instanceof JSStringObject;
    }

    public static final class StringLengthProxyProperty implements PropertyProxy {
        @Override
        public Object get(DynamicObject store) {
            return getStringLength(store);
        }
    }

    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        PropertyDescriptor desc = ordinaryGetOwnProperty(thisObj, key);
        if (desc == null) {
            return stringGetIndexProperty(thisObj, key);
        } else {
            return desc;
        }
    }

    @Override
    public boolean usesOrdinaryGetOwnProperty() {
        return false;
    }

    @TruffleBoundary
    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        assert JSRuntime.isPropertyKey(key) : key.getClass().getName();
        long idx = JSRuntime.propertyKeyToArrayIndex(key);
        if (idx >= 0 && idx < getStringLength(thisObj)) {
            return DefinePropertyUtil.isCompatiblePropertyDescriptor(isExtensible(thisObj), desc, stringGetIndexProperty(thisObj, key), doThrow);
        }
        return super.defineOwnProperty(thisObj, key, desc, doThrow);
    }

    @TruffleBoundary
    private static JSException createTypeErrorCannotRedefineStringIndex(Object key) {
        return Errors.createTypeError("Cannot redefine property: " + key);
    }

    /**
     * ES6, 9.4.3.1.1 StringGetIndexProperty (S, P).
     */
    @TruffleBoundary
    public static PropertyDescriptor stringGetIndexProperty(DynamicObject thisObj, Object key) {
        assert JSString.isJSString(thisObj);
        long index = JSRuntime.propertyKeyToArrayIndex(key);
        if (index < 0) {
            return null;
        }
        String s = getString(thisObj);
        int len = s.length();
        if (len <= index) {
            return null;
        }
        String resultStr = s.substring((int) index, (int) index + 1);
        return PropertyDescriptor.createData(resultStr, true, false, false);
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getStringPrototype();
    }

}
