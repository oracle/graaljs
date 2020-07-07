/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.objects.JSObjectUtil.putHiddenProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.TreeMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantEmptyPrototypeArray;
import com.oracle.truffle.js.runtime.array.dyn.LazyRegexResultArray;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;
import com.oracle.truffle.js.runtime.util.IteratorUtil;

public abstract class JSAbstractArray extends JSBuiltinObject {

    public static final String LENGTH = "length";

    protected static final String ARRAY_LENGTH_NOT_WRITABLE = "array length is not writable";
    private static final String LENGTH_PROPERTY_NOT_WRITABLE = "length property not writable";
    protected static final String MAKE_SLOW_ARRAY_NEVER_PART_OF_COMPILATION_MESSAGE = "do not convert to slow array from compiled code";
    public static final String ARRAY_PROTOTYPE_NO_ELEMENTS_INVALIDATION = "Array.prototype no element assumption";

    private static final HiddenKey ARRAY_ID = new HiddenKey("array");
    private static final HiddenKey ARRAY_TYPE_ID = new HiddenKey("arraytype");
    private static final HiddenKey ALLOCATION_SITE_ID = new HiddenKey("allocationSite");
    private static final HiddenKey LENGTH_ID = new HiddenKey(LENGTH);
    private static final HiddenKey USED_LENGTH_ID = new HiddenKey("usedLength");
    private static final HiddenKey INDEX_OFFSET_ID = new HiddenKey("indexOffset");
    private static final HiddenKey ARRAY_OFFSET_ID = new HiddenKey("arrayOffset");
    private static final HiddenKey HOLE_COUNT_ID = new HiddenKey("holeCount");
    public static final HiddenKey LAZY_REGEX_RESULT_ID = new HiddenKey("lazyRegexResult");
    public static final HiddenKey LAZY_REGEX_ORIGINAL_INPUT_ID = new HiddenKey("lazyRegexResultOriginalInput");
    public static final Property ARRAY_PROPERTY;
    public static final Property ARRAY_TYPE_PROPERTY;
    private static final Property ALLOCATION_SITE_PROPERTY;
    private static final Property LENGTH_PROPERTY;
    private static final Property USED_LENGTH_PROPERTY;
    private static final Property INDEX_OFFSET_PROPERTY;
    private static final Property ARRAY_OFFSET_PROPERTY;
    private static final Property HOLE_COUNT_PROPERTY;
    public static final Property LAZY_REGEX_RESULT_PROPERTY;
    public static final Property LAZY_REGEX_ORIGINAL_INPUT_PROPERTY;

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        ARRAY_PROPERTY = JSObjectUtil.makeHiddenProperty(ARRAY_ID, allocator.locationForType(Object.class, EnumSet.of(LocationModifier.NonNull)));
        ARRAY_TYPE_PROPERTY = JSObjectUtil.makeHiddenProperty(ARRAY_TYPE_ID, allocator.locationForType(ScriptArray.class, EnumSet.of(LocationModifier.NonNull)));
        ALLOCATION_SITE_PROPERTY = JSObjectUtil.makeHiddenProperty(ALLOCATION_SITE_ID, allocator.locationForType(ArrayAllocationSite.class), false);
        LENGTH_PROPERTY = JSObjectUtil.makeHiddenProperty(LENGTH_ID, allocator.locationForType(int.class));
        USED_LENGTH_PROPERTY = JSObjectUtil.makeHiddenProperty(USED_LENGTH_ID, allocator.locationForType(int.class), false);
        INDEX_OFFSET_PROPERTY = JSObjectUtil.makeHiddenProperty(INDEX_OFFSET_ID, allocator.locationForType(int.class), false);
        ARRAY_OFFSET_PROPERTY = JSObjectUtil.makeHiddenProperty(ARRAY_OFFSET_ID, allocator.locationForType(int.class), false);
        HOLE_COUNT_PROPERTY = JSObjectUtil.makeHiddenProperty(HOLE_COUNT_ID, allocator.locationForType(int.class), false);
        LAZY_REGEX_RESULT_PROPERTY = JSObjectUtil.makeHiddenProperty(LAZY_REGEX_RESULT_ID,
                        allocator.locationForType(TruffleObject.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
        LAZY_REGEX_ORIGINAL_INPUT_PROPERTY = JSObjectUtil.makeHiddenProperty(LAZY_REGEX_ORIGINAL_INPUT_ID,
                        allocator.locationForType(String.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    public static ScriptArray arrayGetArrayType(DynamicObject thisObj) {
        assert JSArray.isJSArray(thisObj) || JSArgumentsObject.isJSArgumentsObject(thisObj);
        return arrayGetArrayType(thisObj, JSArray.isJSArray(thisObj));
    }

    public static ScriptArray arrayGetArrayType(DynamicObject thisObj, boolean arrayCondition) {
        assert JSArray.isJSArray(thisObj) || JSArgumentsObject.isJSArgumentsObject(thisObj) || JSObjectPrototype.isJSObjectPrototype(thisObj);
        return (ScriptArray) ARRAY_TYPE_PROPERTY.get(thisObj, arrayCondition);
    }

    public static long arrayGetLength(DynamicObject thisObj) {
        return arrayGetLength(thisObj, JSArray.isJSArray(thisObj));
    }

    public static long arrayGetLength(DynamicObject thisObj, boolean arrayCondition) {
        return Integer.toUnsignedLong((int) LENGTH_PROPERTY.get(thisObj, arrayCondition));
    }

    public static int arrayGetUsedLength(DynamicObject thisObj) {
        return arrayGetUsedLength(thisObj, JSArray.isJSArray(thisObj));
    }

    public static int arrayGetUsedLength(DynamicObject thisObj, boolean arrayCondition) {
        return (int) USED_LENGTH_PROPERTY.get(thisObj, arrayCondition);
    }

    public static long arrayGetIndexOffset(DynamicObject thisObj) {
        return arrayGetIndexOffset(thisObj, JSArray.isJSArray(thisObj));
    }

    public static long arrayGetIndexOffset(DynamicObject thisObj, boolean arrayCondition) {
        return Integer.toUnsignedLong((int) INDEX_OFFSET_PROPERTY.get(thisObj, arrayCondition));
    }

    public static int arrayGetArrayOffset(DynamicObject thisObj) {
        return arrayGetArrayOffset(thisObj, JSArray.isJSArray(thisObj));
    }

    public static int arrayGetArrayOffset(DynamicObject thisObj, boolean arrayCondition) {
        return (int) ARRAY_OFFSET_PROPERTY.get(thisObj, arrayCondition);
    }

    public static void arraySetArrayType(DynamicObject thisObj, ScriptArray arrayType) {
        ARRAY_TYPE_PROPERTY.setSafe(thisObj, arrayType, null);
    }

    public static void arraySetLength(DynamicObject thisObj, long length) {
        assert JSRuntime.isRepresentableAsUnsignedInt(length);
        LENGTH_PROPERTY.setSafe(thisObj, (int) length, null);
    }

    public static void arraySetUsedLength(DynamicObject thisObj, int usedLength) {
        assert usedLength >= 0;
        USED_LENGTH_PROPERTY.setSafe(thisObj, usedLength, null);
    }

    public static void arraySetIndexOffset(DynamicObject thisObj, long indexOffset) {
        assert JSRuntime.isRepresentableAsUnsignedInt(indexOffset);
        INDEX_OFFSET_PROPERTY.setSafe(thisObj, (int) indexOffset, null);
    }

    public static void arraySetArrayOffset(DynamicObject thisObj, int arrayOffset) {
        assert arrayOffset >= 0;
        ARRAY_OFFSET_PROPERTY.setSafe(thisObj, arrayOffset, null);
    }

    public static Object arrayGetArray(DynamicObject thisObj) {
        return arrayGetArray(thisObj, JSObject.hasArray(thisObj));
    }

    public static Object arrayGetArray(DynamicObject thisObj, boolean arrayCondition) {
        assert JSObject.hasArray(thisObj);
        return ARRAY_PROPERTY.get(thisObj, arrayCondition);
    }

    public static void arraySetArray(DynamicObject thisObj, Object array) {
        assert JSObject.hasArray(thisObj);
        assert array != null && (array.getClass().isArray() || array instanceof TreeMap<?, ?>);
        JSAbstractArray.ARRAY_PROPERTY.setSafe(thisObj, array, null);
    }

    public static int arrayGetHoleCount(DynamicObject thisObj) {
        return arrayGetHoleCount(thisObj, JSArray.isJSArray(thisObj));
    }

    public static int arrayGetHoleCount(DynamicObject thisObj, boolean arrayCondition) {
        return (int) HOLE_COUNT_PROPERTY.get(thisObj, arrayCondition);
    }

    public static void arraySetHoleCount(DynamicObject thisObj, int holeCount) {
        HOLE_COUNT_PROPERTY.setSafe(thisObj, holeCount, null);
    }

    public static ArrayAllocationSite arrayGetAllocationSite(DynamicObject thisObj) {
        return arrayGetAllocationSite(thisObj, JSArray.isJSArray(thisObj));
    }

    public static ArrayAllocationSite arrayGetAllocationSite(DynamicObject thisObj, boolean arrayCondition) {
        return (ArrayAllocationSite) ALLOCATION_SITE_PROPERTY.get(thisObj, arrayCondition);
    }

    public static TruffleObject arrayGetRegexResult(DynamicObject thisObj) {
        return arrayGetRegexResult(thisObj, JSArray.isJSArray(thisObj) && JSArray.arrayGetArrayType(thisObj) == LazyRegexResultArray.LAZY_REGEX_RESULT_ARRAY);
    }

    public static TruffleObject arrayGetRegexResult(DynamicObject thisObj, boolean arrayCondition) {
        return (TruffleObject) LAZY_REGEX_RESULT_PROPERTY.get(thisObj, arrayCondition);
    }

    public static String arrayGetRegexResultOriginalInput(DynamicObject thisObj) {
        return arrayGetRegexResultOriginalInput(thisObj, JSArray.isJSArray(thisObj) && JSArray.arrayGetArrayType(thisObj) == LazyRegexResultArray.LAZY_REGEX_RESULT_ARRAY);
    }

    public static String arrayGetRegexResultOriginalInput(DynamicObject thisObj, boolean arrayCondition) {
        return (String) LAZY_REGEX_ORIGINAL_INPUT_PROPERTY.get(thisObj, arrayCondition);
    }

    public static void putArrayProperties(DynamicObject arrayPrototype, ScriptArray arrayType) {
        putHiddenProperty(arrayPrototype, ARRAY_PROPERTY, ScriptArray.EMPTY_OBJECT_ARRAY);
        putHiddenProperty(arrayPrototype, ARRAY_TYPE_PROPERTY, arrayType);
        putHiddenProperty(arrayPrototype, ALLOCATION_SITE_PROPERTY, null);
        putHiddenProperty(arrayPrototype, LENGTH_PROPERTY, 0);
        putHiddenProperty(arrayPrototype, USED_LENGTH_PROPERTY, 0);
        putHiddenProperty(arrayPrototype, INDEX_OFFSET_PROPERTY, 0);
        putHiddenProperty(arrayPrototype, ARRAY_OFFSET_PROPERTY, 0);
        putHiddenProperty(arrayPrototype, HOLE_COUNT_PROPERTY, 0);
    }

    protected static Shape addArrayProperties(Shape initialShape) {
        Shape shape = initialShape;
        shape = shape.addProperty(ARRAY_PROPERTY);
        shape = shape.addProperty(ARRAY_TYPE_PROPERTY);
        shape = shape.addProperty(ALLOCATION_SITE_PROPERTY);
        shape = shape.addProperty(LENGTH_PROPERTY);
        shape = shape.addProperty(USED_LENGTH_PROPERTY);
        shape = shape.addProperty(INDEX_OFFSET_PROPERTY);
        shape = shape.addProperty(ARRAY_OFFSET_PROPERTY);
        shape = shape.addProperty(HOLE_COUNT_PROPERTY);
        return shape;
    }

    public static class DefaultJSArrayComparator implements Comparator<Object> {
        @Override
        public int compare(Object arg0, Object arg1) {
            if (arg0 == Undefined.instance) {
                if (arg1 == Undefined.instance) {
                    return 0;
                }
                return 1;
            } else if (arg1 == Undefined.instance) {
                return -1;
            }
            String str0 = JSRuntime.toString(arg0);
            String str1 = JSRuntime.toString(arg1);
            if (str0 == null) {
                if (str1 == null) {
                    return 0;
                }
                return 1;
            } else if (str1 == null) {
                return -1;
            }
            return Boundaries.stringCompareTo(str0, str1);
        }
    }

    public static class DefaultJSArrayIntegerComparator implements Comparator<Object> {
        @Override
        public int compare(Object arg0, Object arg1) {
            int i1 = (int) JSRuntime.toInteger((Number) arg0);
            int i2 = (int) JSRuntime.toInteger((Number) arg1);
            if (i1 == i2) {
                return 0;
            }
            if (i1 <= 0 && i2 > 0) {
                return -1;
            } else if (i2 <= 0 && i1 > 0) {
                return 1;
            }
            String str0 = Integer.toString(i1);
            String str1 = Integer.toString(i2);
            return Boundaries.stringCompareTo(str0, str1);
        }
    }

    public static class DefaultJSArrayDoubleComparator implements Comparator<Object> {
        @Override
        public int compare(Object arg0, Object arg1) {
            double d1 = JSRuntime.doubleValue((Number) arg0);
            double d2 = JSRuntime.doubleValue((Number) arg1);
            if (d1 == d2) {
                return 0;
            }
            if (d1 <= 0 && d2 > 0) {
                return -1;
            } else if (d2 <= 0 && d1 > 0) {
                return 1;
            }
            String str0 = JSRuntime.doubleToString(d1);
            String str1 = JSRuntime.doubleToString(d2);
            return Boundaries.stringCompareTo(str0, str1);
        }
    }

    protected JSAbstractArray() {
    }

    public long getLength(DynamicObject thisObj) {
        return arrayGetArrayType(thisObj).length(thisObj);
    }

    @TruffleBoundary
    public boolean setLength(DynamicObject thisObj, long length, boolean doThrow) {
        if (length < 0) {
            throw Errors.createRangeErrorInvalidArrayLength();
        }
        ScriptArray array = arrayGetArrayType(thisObj);
        if (length > Integer.MAX_VALUE && !(array instanceof SparseArray)) {
            array = SparseArray.makeSparseArray(thisObj, array);
        }
        if (array.isSealed()) {
            long minIndex = array.lastElementIndex(thisObj) + 1;
            if (length < minIndex) {
                array = array.setLength(thisObj, minIndex, doThrow);
                arraySetArrayType(thisObj, array);
                return array.canDeleteElement(thisObj, minIndex - 1, doThrow);
            }
        }
        arraySetArrayType(thisObj, array.setLength(thisObj, length, doThrow));
        return true;
    }

    @Override
    public String getBuiltinToStringTag(DynamicObject object) {
        return getClassName(object);
    }

    @TruffleBoundary
    @Override
    public final Object getOwnHelper(DynamicObject store, Object thisObj, Object key) {
        long idx = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(idx)) {
            return getOwnHelper(store, thisObj, idx);
        }
        return super.getOwnHelper(store, thisObj, key);
    }

    @TruffleBoundary
    @Override
    public final boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict) {
        if (receiver != thisObj) {
            return ordinarySetWithReceiver(thisObj, key, value, receiver, isStrict);
        }
        assert receiver == thisObj;
        long idx = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(idx)) {
            return set(thisObj, idx, value, receiver, isStrict);
        } else {
            return super.set(thisObj, key, value, receiver, isStrict);
        }
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        if (receiver != thisObj) {
            return ordinarySetWithReceiver(thisObj, Boundaries.stringValueOf(index), value, receiver, isStrict);
        }
        assert receiver == thisObj;
        if (arrayGetArrayType(thisObj).hasElement(thisObj, index)) {
            return setElement(thisObj, index, value, isStrict);
        } else {
            return setPropertySlow(thisObj, index, value, receiver, isStrict);
        }
    }

    @TruffleBoundary
    private static boolean setPropertySlow(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        if (!JSObject.getJSContext(thisObj).getArrayPrototypeNoElementsAssumption().isValid() && setPropertyPrototypes(thisObj, index, value, receiver, isStrict)) {
            return true;
        }

        if (!JSObject.isExtensible(thisObj)) {
            if (isStrict) {
                throw Errors.createTypeErrorNotExtensible(thisObj, Boundaries.stringValueOf(index));
            }
            return true;
        }
        return setElement(thisObj, index, value, isStrict);
    }

    private static boolean setPropertyPrototypes(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        // check prototype chain for accessors
        DynamicObject current = JSObject.getPrototype(thisObj);
        String propertyName = null;
        while (current != Null.instance) {
            if (JSProxy.isProxy(current)) {
                return JSObject.setWithReceiver(current, index, value, receiver, false);
            }
            if (canHaveReadOnlyOrAccessorProperties(current)) {
                if (JSObject.hasOwnProperty(current, index)) {
                    if (propertyName == null) {
                        propertyName = Boundaries.stringValueOf(index);
                    }
                    PropertyDescriptor desc = JSObject.getOwnProperty(current, propertyName);
                    if (desc != null) {
                        if (desc.isAccessorDescriptor()) {
                            invokeAccessorPropertySetter(desc, thisObj, propertyName, value, receiver, isStrict);
                            return true;
                        } else if (!desc.getWritable()) {
                            if (isStrict) {
                                throw Errors.createTypeError("Cannot assign to read only property '" + index + "' of " + JSObject.defaultToString(thisObj));
                            }
                            return true;
                        } else {
                            break;
                        }
                    }
                }
            }
            current = JSObject.getPrototype(current);
        }
        return false;
    }

    private static boolean canHaveReadOnlyOrAccessorProperties(DynamicObject current) {
        return !JSArrayBufferView.isJSArrayBufferView(current);
    }

    private static boolean setElement(DynamicObject thisObj, long index, Object value, boolean isStrict) {
        arraySetArrayType(thisObj, arrayGetArrayType(thisObj).setElement(thisObj, index, value, isStrict));
        return true;
    }

    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        ScriptArray arrayType = arrayGetArrayType(thisObj);
        if (arrayType.canDeleteElement(thisObj, index, isStrict)) {
            arraySetArrayType(thisObj, arrayType.deleteElement(thisObj, index, isStrict));
            return true;
        } else {
            return false;
        }
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index) {
        ScriptArray array = arrayGetArrayType(store);
        if (array.hasElement(store, index)) {
            return array.getElement(store, index);
        }
        return super.getOwnHelper(store, thisObj, Boundaries.stringValueOf(index));
    }

    @TruffleBoundary
    public static Object[] toArray(DynamicObject thisObj) {
        return arrayGetArrayType(thisObj).toArray(thisObj);
    }

    @TruffleBoundary
    @Override
    public final boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        if (super.hasOwnProperty(thisObj, key)) {
            return true;
        }
        long index = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(index)) {
            return arrayGetArrayType(thisObj).hasElement(thisObj, index);
        }
        return false;
    }

    @TruffleBoundary
    @Override
    public final boolean hasOwnProperty(DynamicObject thisObj, long index) {
        ScriptArray array = arrayGetArrayType(thisObj);
        if (array.hasElement(thisObj, index)) {
            return true;
        }
        return super.hasOwnProperty(thisObj, Boundaries.stringValueOf(index));
    }

    @TruffleBoundary
    @Override
    public List<Object> getOwnPropertyKeys(DynamicObject thisObj, boolean strings, boolean symbols) {
        return ownPropertyKeysSlowArray(thisObj, strings, symbols);
    }

    @TruffleBoundary
    protected static List<Object> ownPropertyKeysFastArray(DynamicObject thisObj, boolean strings, boolean symbols) {
        assert JSArray.isJSFastArray(thisObj) || JSArgumentsObject.isJSFastArgumentsObject(thisObj);
        List<Object> indices = strings ? arrayGetArrayType(thisObj).ownPropertyKeys(thisObj) : Collections.emptyList();
        List<Object> keyList = thisObj.getShape().getKeyList();
        if (keyList.isEmpty()) {
            return indices;
        } else {
            List<Object> list = new ArrayList<>(keyList.size());
            if (strings) {
                keyList.forEach(k -> {
                    assert !(k instanceof String && JSRuntime.isArrayIndex((String) k));
                    if (k instanceof String) {
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

    @TruffleBoundary
    protected static List<Object> ownPropertyKeysSlowArray(DynamicObject thisObj, boolean strings, boolean symbols) {
        List<Object> list = new ArrayList<>();

        if (strings) {
            ScriptArray array = arrayGetArrayType(thisObj);
            long currentIndex = array.firstElementIndex(thisObj);
            while (currentIndex <= array.lastElementIndex(thisObj)) {
                list.add(Boundaries.stringValueOf(currentIndex));
                currentIndex = array.nextElementIndex(thisObj, currentIndex);
            }
        }

        List<Object> keyList = thisObj.getShape().getKeyList();
        if (!keyList.isEmpty()) {
            if (strings) {
                int before = list.size();
                keyList.forEach(k -> {
                    if (k instanceof String && JSRuntime.isArrayIndex((String) k)) {
                        list.add(k);
                    }
                });
                int after = list.size();
                if (after != before) {
                    Collections.sort(list, new Comparator<Object>() {
                        @Override
                        public int compare(Object o1, Object o2) {
                            long l1 = JSRuntime.propertyKeyToArrayIndex(o1);
                            long l2 = JSRuntime.propertyKeyToArrayIndex(o2);
                            return l1 < l2 ? -1 : (l1 == l2 ? 0 : 1);
                        }
                    });
                }
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
        }
        return list;
    }

    protected static long toArrayIndexOrRangeError(Object obj) {
        Number len = JSRuntime.toNumber(obj);
        Number len32 = JSRuntime.toUInt32(len);
        return toArrayIndexOrRangeError(len, len32);
    }

    public static long toArrayIndexOrRangeError(Number len, Number len32) {
        double d32 = JSRuntime.doubleValue(len32);
        double d = JSRuntime.doubleValue(len);

        if (d32 == d) {
            return len32.longValue();
        }
        if (d == 0) {
            return 0; // also handles the -0.0
        }
        throw Errors.createRangeErrorInvalidArrayLength();
    }

    public static long toArrayIndexOrRangeError(Number len, Number len32, BranchProfile profileError, BranchProfile profileDouble1, BranchProfile profileDouble2) {
        double d32 = JSRuntime.doubleValue(len32, profileDouble1);
        double d = JSRuntime.doubleValue(len, profileDouble2);

        if (d32 == d) {
            return len32.longValue();
        }
        if (d == 0) {
            return 0; // also handles the -0.0
        }
        profileError.enter();
        throw Errors.createRangeErrorInvalidArrayLength();
    }

    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor descriptor, boolean doThrow) {
        if (key.equals(LENGTH)) {
            return defineOwnPropertyLength(thisObj, key, descriptor, doThrow);
        } else if (key instanceof String && JSRuntime.isArrayIndex((String) key)) {
            return defineOwnPropertyIndex(thisObj, (String) key, descriptor, doThrow);
        } else {
            return super.defineOwnProperty(thisObj, key, descriptor, doThrow);
        }
    }

    /**
     * Implements the abstract operation ArraySetLength (A, Desc), redefining the "length" property
     * of an Array exotic object.
     *
     * @return whether the operation was successful
     */
    private boolean defineOwnPropertyLength(DynamicObject thisObj, Object key, PropertyDescriptor descriptor, boolean doThrow) {
        if (!descriptor.hasValue()) {
            if (descriptor.hasWritable() && !descriptor.getWritable()) {
                setLengthNotWritable(thisObj);
            }
            return DefinePropertyUtil.ordinaryDefineOwnProperty(thisObj, key, descriptor, doThrow);
        }

        Number newLenNum = JSRuntime.toNumber(descriptor.getValue());
        long newLen = JSRuntime.toUInt32(newLenNum);
        if (JSRuntime.doubleValue(newLenNum) != newLen) {
            throw Errors.createRangeErrorInvalidArrayLength();
        }
        PropertyDescriptor lenDesc = getOwnProperty(thisObj, LENGTH);
        if (newLen >= getLength(thisObj)) {
            return definePropertyLength(thisObj, descriptor, lenDesc, newLen, doThrow);
        }

        if (!lenDesc.getWritable()) {
            return DefinePropertyUtil.reject(doThrow, ARRAY_LENGTH_NOT_WRITABLE);
        }

        long pos = getLength(thisObj);
        if (!definePropertyLength(thisObj, descriptor, lenDesc, newLen, doThrow)) {
            return false;
        }
        // If newLen < oldLen, we need to delete all the elements from oldLen-1 to newLen
        if (JSSlowArray.isJSSlowArray(thisObj)) {
            return deleteElementsAfterShortening(thisObj, descriptor, doThrow, newLen, lenDesc, pos);
        } else {
            // fast array: elements with newLen <= i < oldLen are already deleted by setting newLen
            return true;
        }
    }

    private static void setLengthNotWritable(DynamicObject thisObj) {
        arraySetArrayType(thisObj, arrayGetArrayType(thisObj).setLengthNotWritable());
    }

    private boolean deleteElementsAfterShortening(DynamicObject thisObj, PropertyDescriptor descriptor, boolean doThrow, long newLen, PropertyDescriptor lenDesc, long startPos) {
        assert JSRuntime.isValidArrayLength(newLen);
        long pos = startPos;
        while (pos > newLen) {
            pos--;
            String key = String.valueOf(pos);
            Property prop = DefinePropertyUtil.getPropertyByKey(thisObj, key);
            if (prop != null) {
                if (JSProperty.isConfigurable(prop)) {
                    delete(thisObj, key, doThrow);
                } else {
                    // delete did not succeed, increase the length to include the current element
                    long len = pos + 1;
                    descriptor.setValue(JSRuntime.longToIntOrDouble(len));
                    definePropertyLength(thisObj, descriptor, lenDesc, len, doThrow);
                    DefinePropertyUtil.ordinaryDefineOwnProperty(thisObj, LENGTH, descriptor, false);
                    return DefinePropertyUtil.reject(doThrow, "cannot set the length to expected value");
                }
            }
        }
        return true;
    }

    private boolean definePropertyLength(DynamicObject thisObj, PropertyDescriptor descriptor, PropertyDescriptor currentDesc, long len, boolean doThrow) {
        assert JSRuntime.isValidArrayLength(len);
        boolean currentWritable = currentDesc.getWritable();
        boolean currentEnumerable = currentDesc.getEnumerable();
        boolean currentConfigurable = currentDesc.getConfigurable();

        boolean newWritable = descriptor.getIfHasWritable(currentWritable);
        boolean newEnumerable = descriptor.getIfHasEnumerable(currentEnumerable);
        boolean newConfigurable = descriptor.getIfHasConfigurable(currentConfigurable);

        if (currentConfigurable) {
            if (!currentWritable && !newWritable) {
                return DefinePropertyUtil.reject(doThrow, LENGTH_PROPERTY_NOT_WRITABLE);
            }
        } else {
            if (currentWritable == newWritable && currentEnumerable == newEnumerable) {
                if (!descriptor.hasValue() || len == getLength(thisObj)) {
                    return true; // nothing changed
                }
            }
            if (!currentWritable) {
                return DefinePropertyUtil.reject(doThrow, LENGTH_PROPERTY_NOT_WRITABLE);
            }
        }

        try {
            setLength(thisObj, len, doThrow);
        } finally {
            int newAttr = JSAttributes.fromConfigurableEnumerableWritable(newConfigurable, newEnumerable, newWritable);
            JSObjectUtil.changeFlags(thisObj, LENGTH, newAttr);
        }

        return true;
    }

    /**
     * Implements part "3" of 15.4.5.1 [[DefineOwnProperty]], redefining one of the index property
     * of an Array.
     *
     * @return whether the operation was successful
     */
    protected boolean defineOwnPropertyIndex(DynamicObject thisObj, String name, PropertyDescriptor descriptor, boolean doThrow) {
        long index = JSRuntime.toUInt32(name);
        if (index >= this.getLength(thisObj)) {
            PropertyDescriptor lenDesc = getOwnProperty(thisObj, LENGTH);
            if (!lenDesc.getWritable()) {
                DefinePropertyUtil.reject(doThrow, ARRAY_LENGTH_NOT_WRITABLE);
            }
        }
        return JSObject.defineOwnProperty(makeSlowArray(thisObj), name, descriptor, doThrow);
    }

    protected DynamicObject makeSlowArray(DynamicObject thisObj) {
        CompilerAsserts.neverPartOfCompilation(MAKE_SLOW_ARRAY_NEVER_PART_OF_COMPILATION_MESSAGE);
        assert !JSSlowArray.isJSSlowArray(thisObj);
        Shape oldShape = thisObj.getShape();
        thisObj.setShapeAndGrow(oldShape, oldShape.changeType(JSSlowArray.INSTANCE));
        JSContext context = JSObject.getJSContext(thisObj);
        context.getFastArrayAssumption().invalidate("create slow ArgumentsObject");
        if (isArrayPrototype(thisObj)) {
            context.getArrayPrototypeNoElementsAssumption().invalidate("Array.prototype has no elements");
        }
        return thisObj;
    }

    private static boolean isArrayPrototype(DynamicObject thisObj) {
        return arrayGetArrayType(thisObj) instanceof ConstantEmptyPrototypeArray;
    }

    @Override
    public boolean testIntegrityLevel(DynamicObject thisObj, boolean frozen) {
        ScriptArray array = arrayGetArrayType(thisObj);
        boolean arrayIs = frozen ? array.isFrozen() : array.isSealed();
        return arrayIs && super.testIntegrityLevel(thisObj, frozen);
    }

    @Override
    public boolean setIntegrityLevel(DynamicObject thisObj, boolean freeze) {
        boolean result = super.setIntegrityLevel(thisObj, freeze);
        ScriptArray arr = arrayGetArrayType(thisObj);
        arraySetArrayType(thisObj, freeze ? arr.freeze() : arr.seal());
        assert testIntegrityLevel(thisObj, freeze);
        return result;
    }

    @TruffleBoundary
    @Override
    public final boolean preventExtensions(DynamicObject thisObj) {
        boolean result = super.preventExtensions(thisObj);
        ScriptArray arr = arrayGetArrayType(thisObj);
        arraySetArrayType(thisObj, arr.preventExtensions());
        assert !isExtensible(thisObj);
        return result;
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        long index = JSRuntime.propertyKeyToArrayIndex(key);
        if (index >= 0) {
            return delete(thisObj, index, isStrict);
        } else {
            return super.delete(thisObj, key, isStrict);
        }
    }

    @TruffleBoundary
    @Override
    public boolean setPrototypeOf(DynamicObject thisObj, DynamicObject newPrototype) {
        JSObject.getJSContext(thisObj).getArrayPrototypeNoElementsAssumption().invalidate(JSAbstractArray.ARRAY_PROTOTYPE_NO_ELEMENTS_INVALIDATION);
        return super.setPrototypeOf(thisObj, newPrototype);
    }

    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key) {
        return ordinaryGetOwnPropertyArray(thisObj, key);
    }

    /**
     * 9.1.5.1 OrdinaryGetOwnProperty (O, P), implemented for Arrays.
     */
    @TruffleBoundary
    public static PropertyDescriptor ordinaryGetOwnPropertyArray(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key) || key instanceof HiddenKey;

        long idx = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(idx)) {
            ScriptArray array = arrayGetArrayType(thisObj, false);
            if (array.hasElement(thisObj, idx)) {
                Object value = array.getElement(thisObj, idx);
                return PropertyDescriptor.createData(value, true, !array.isFrozen(), !array.isSealed());
            }
        }
        Property prop = thisObj.getShape().getProperty(key);
        if (prop == null) {
            return null;
        }
        return JSBuiltinObject.ordinaryGetOwnPropertyIntl(thisObj, key, prop);
    }

    @Override
    public String safeToString(DynamicObject obj, int depth) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return defaultToString(obj);
        } else {
            return JSRuntime.objectToConsoleString(obj, null, depth);
        }
    }

    @Override
    public boolean usesOrdinaryGetOwnProperty() {
        return false;
    }
}
