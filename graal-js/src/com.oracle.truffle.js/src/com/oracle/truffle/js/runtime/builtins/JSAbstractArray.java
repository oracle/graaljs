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
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantEmptyPrototypeArray;
import com.oracle.truffle.js.runtime.array.dyn.LazyRegexResultArray;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;
import com.oracle.truffle.js.runtime.util.IteratorUtil;

public abstract class JSAbstractArray extends JSNonProxy {

    public static final String LENGTH = "length";

    protected static final String ARRAY_LENGTH_NOT_WRITABLE = "array length is not writable";
    private static final String LENGTH_PROPERTY_NOT_WRITABLE = "length property not writable";
    protected static final String CANNOT_REDEFINE_PROPERTY_LENGTH = "Cannot redefine property: length";
    protected static final String MAKE_SLOW_ARRAY_NEVER_PART_OF_COMPILATION_MESSAGE = "do not convert to slow array from compiled code";
    public static final String ARRAY_PROTOTYPE_NO_ELEMENTS_INVALIDATION = "Array.prototype no element assumption";

    public static final HiddenKey LAZY_REGEX_RESULT_ID = new HiddenKey("lazyRegexResult");
    public static final HiddenKey LAZY_REGEX_ORIGINAL_INPUT_ID = new HiddenKey("lazyRegexResultOriginalInput");

    public static ScriptArray arrayGetArrayType(DynamicObject thisObj) {
        assert JSArray.isJSArray(thisObj) || JSArgumentsArray.isJSArgumentsObject(thisObj) || JSObjectPrototype.isJSObjectPrototype(thisObj);
        return arrayAccess().getArrayType(thisObj);
    }

    public static long arrayGetLength(DynamicObject thisObj) {
        return arrayAccess().getLength(thisObj);
    }

    public static int arrayGetUsedLength(DynamicObject thisObj) {
        return arrayAccess().getUsedLength(thisObj);
    }

    public static long arrayGetIndexOffset(DynamicObject thisObj) {
        return arrayAccess().getIndexOffset(thisObj);
    }

    public static int arrayGetArrayOffset(DynamicObject thisObj) {
        return arrayAccess().getArrayOffset(thisObj);
    }

    public static void arraySetArrayType(DynamicObject thisObj, ScriptArray arrayType) {
        arrayAccess().setArrayType(thisObj, arrayType);
    }

    public static void arraySetLength(DynamicObject thisObj, int length) {
        assert length >= 0;
        arrayAccess().setLength(thisObj, length);
    }

    public static void arraySetLength(DynamicObject thisObj, long length) {
        assert JSRuntime.isValidArrayLength(length);
        arrayAccess().setLength(thisObj, length);
    }

    public static void arraySetUsedLength(DynamicObject thisObj, int usedLength) {
        assert usedLength >= 0;
        arrayAccess().setUsedLength(thisObj, usedLength);
    }

    public static void arraySetIndexOffset(DynamicObject thisObj, long indexOffset) {
        arrayAccess().setIndexOffset(thisObj, indexOffset);
    }

    public static void arraySetArrayOffset(DynamicObject thisObj, int arrayOffset) {
        assert arrayOffset >= 0;
        arrayAccess().setArrayOffset(thisObj, arrayOffset);
    }

    public static Object arrayGetArray(DynamicObject thisObj) {
        assert JSObject.hasArray(thisObj);
        return arrayAccess().getArray(thisObj);
    }

    public static void arraySetArray(DynamicObject thisObj, Object array) {
        assert JSObject.hasArray(thisObj);
        assert array != null && (array.getClass().isArray() || array instanceof TreeMap<?, ?>);
        arrayAccess().setArray(thisObj, array);
    }

    public static int arrayGetHoleCount(DynamicObject thisObj) {
        return arrayAccess().getHoleCount(thisObj);
    }

    public static void arraySetHoleCount(DynamicObject thisObj, int holeCount) {
        assert holeCount >= 0;
        arrayAccess().setHoleCount(thisObj, holeCount);
    }

    public static ArrayAllocationSite arrayGetAllocationSite(DynamicObject thisObj) {
        return arrayAccess().getAllocationSite(thisObj);
    }

    public static Object arrayGetRegexResult(DynamicObject thisObj, DynamicObjectLibrary lazyRegexResult) {
        assert JSArray.isJSArray(thisObj) && JSArray.arrayGetArrayType(thisObj) == LazyRegexResultArray.LAZY_REGEX_RESULT_ARRAY;
        return lazyRegexResult.getOrDefault(thisObj, LAZY_REGEX_RESULT_ID, null);
    }

    public static String arrayGetRegexResultOriginalInput(DynamicObject thisObj, DynamicObjectLibrary lazyRegexResultOriginalInput) {
        return (String) lazyRegexResultOriginalInput.getOrDefault(thisObj, LAZY_REGEX_ORIGINAL_INPUT_ID, null);
    }

    public static final Comparator<Object> DEFAULT_JSARRAY_COMPARATOR = new DefaultJSArrayComparator();
    public static final Comparator<Object> DEFAULT_JSARRAY_INTEGER_COMPARATOR = new DefaultJSArrayIntegerComparator();
    public static final Comparator<Object> DEFAULT_JSARRAY_DOUBLE_COMPARATOR = new DefaultJSArrayDoubleComparator();

    static final class DefaultJSArrayComparator implements Comparator<Object> {
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

    static final class DefaultJSArrayIntegerComparator implements Comparator<Object> {
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

    static final class DefaultJSArrayDoubleComparator implements Comparator<Object> {
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

    protected static final ArrayAccess arrayAccess() {
        return ArrayAccess.SINGLETON;
    }

    public long getLength(DynamicObject thisObj) {
        return arrayGetLength(thisObj);
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
    public final Object getOwnHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        long idx = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(idx)) {
            return getOwnHelper(store, thisObj, idx, encapsulatingNode);
        }
        return super.getOwnHelper(store, thisObj, key, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public final boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        if (receiver != thisObj) {
            return ordinarySetWithReceiver(thisObj, key, value, receiver, isStrict, encapsulatingNode);
        }
        assert receiver == thisObj;
        long idx = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(idx)) {
            return set(thisObj, idx, value, receiver, isStrict, encapsulatingNode);
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
        assert receiver == thisObj;
        if (arrayGetArrayType(thisObj).hasElement(thisObj, index)) {
            return setElement(thisObj, index, value, isStrict);
        } else {
            return setPropertySlow(thisObj, index, value, receiver, isStrict, encapsulatingNode);
        }
    }

    @TruffleBoundary
    private static boolean setPropertySlow(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        if (!JSObject.getJSContext(thisObj).getArrayPrototypeNoElementsAssumption().isValid() && setPropertyPrototypes(thisObj, index, value, receiver, isStrict, encapsulatingNode)) {
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

    private static boolean setPropertyPrototypes(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        // check prototype chain for accessors
        DynamicObject current = JSObject.getPrototype(thisObj);
        String propertyName = null;
        while (current != Null.instance) {
            if (JSProxy.isJSProxy(current)) {
                return JSObject.getJSClass(current).set(current, index, value, receiver, false, encapsulatingNode);
            }
            if (canHaveReadOnlyOrAccessorProperties(current)) {
                if (JSObject.hasOwnProperty(current, index)) {
                    if (propertyName == null) {
                        propertyName = Boundaries.stringValueOf(index);
                    }
                    PropertyDescriptor desc = JSObject.getOwnProperty(current, propertyName);
                    if (desc != null) {
                        if (desc.isAccessorDescriptor()) {
                            invokeAccessorPropertySetter(desc, thisObj, propertyName, value, receiver, isStrict, encapsulatingNode);
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
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        ScriptArray array = arrayGetArrayType(store);
        if (array.hasElement(store, index)) {
            return array.getElement(store, index);
        }
        return super.getOwnHelper(store, thisObj, Boundaries.stringValueOf(index), encapsulatingNode);
    }

    /**
     * Creates an Object[] from this array, of size array.length. Does not check the prototype
     * chain, i.e. result can be wrong. Use JSToObjectArrayNode for more correct results.
     *
     * This is mostly used in tests, but also in a few places in Node.js.
     */
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
        assert JSArray.isJSFastArray(thisObj) || JSArgumentsArray.isJSFastArgumentsObject(thisObj);
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

    protected static long toArrayLengthOrRangeError(Object obj) {
        Number len = JSRuntime.toNumber(obj);
        Number len32 = JSRuntime.toUInt32(len);
        /*
         * ArraySetLength, steps 3 and 4: if Desc.[[Value]] is an object then its valueOf method is
         * called twice. This is legacy behaviour that was specified with this effect.
         */
        Number numberLen = JSRuntime.toNumber(obj);
        return toArrayLengthOrRangeError(numberLen, len32);
    }

    public static long toArrayLengthOrRangeError(Number len, Number len32) {
        double d32 = JSRuntime.doubleValue(len32);
        double d = JSRuntime.doubleValue(len);

        if (d32 == d) {
            return JSRuntime.longValue(len32);
        }
        if (d == 0) {
            return 0; // also handles the -0.0
        }
        throw Errors.createRangeErrorInvalidArrayLength();
    }

    @Override
    @TruffleBoundary
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor descriptor, boolean doThrow) {
        if (key.equals(LENGTH)) {
            return defineOwnPropertyLength(thisObj, descriptor, doThrow);
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
    private boolean defineOwnPropertyLength(DynamicObject thisObj, PropertyDescriptor descriptor, boolean doThrow) {
        if (!descriptor.hasValue()) {
            boolean success = DefinePropertyUtil.ordinaryDefineOwnProperty(thisObj, LENGTH, descriptor, doThrow);
            if (success && descriptor.hasWritable() && !descriptor.getWritable()) {
                setLengthNotWritable(thisObj);
            }
            return success;
        }

        long newLen = JSRuntime.toUInt32(descriptor.getValue());
        Number numberLen = JSRuntime.toNumber(descriptor.getValue());
        if (JSRuntime.doubleValue(numberLen) != newLen) {
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
        assert !currentDesc.getConfigurable();
        boolean currentWritable = currentDesc.getWritable();
        boolean currentEnumerable = currentDesc.getEnumerable();

        boolean newWritable = descriptor.getIfHasWritable(currentWritable);
        boolean newEnumerable = descriptor.getIfHasEnumerable(currentEnumerable);
        boolean newConfigurable = descriptor.getIfHasConfigurable(false);

        if (newConfigurable || (newEnumerable != currentEnumerable)) {
            // ES2020 9.1.6.3, 4.a and 4.b
            return DefinePropertyUtil.reject(doThrow, CANNOT_REDEFINE_PROPERTY_LENGTH);
        }
        if (currentWritable == newWritable && currentEnumerable == newEnumerable) {
            if (!descriptor.hasValue() || len == getLength(thisObj)) {
                return true; // nothing changed
            }
        }
        if (!currentWritable) {
            return DefinePropertyUtil.reject(doThrow, LENGTH_PROPERTY_NOT_WRITABLE);
        }

        try {
            setLength(thisObj, len, doThrow);
        } finally {
            int newAttr = JSAttributes.fromConfigurableEnumerableWritable(newConfigurable, newEnumerable, newWritable);
            JSObjectUtil.changePropertyFlags(thisObj, LENGTH, newAttr);
        }

        if (!newWritable) {
            setLengthNotWritable(thisObj);
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
        boolean wasNotExtensible = !JSShape.isExtensible(thisObj.getShape());
        boolean success = JSObject.defineOwnProperty(makeSlowArray(thisObj), name, descriptor, doThrow);
        if (wasNotExtensible) {
            assert !JSShape.isExtensible(thisObj.getShape());
        }
        return success;
    }

    protected DynamicObject makeSlowArray(DynamicObject thisObj) {
        CompilerAsserts.neverPartOfCompilation(MAKE_SLOW_ARRAY_NEVER_PART_OF_COMPILATION_MESSAGE);
        if (isSlowArray(thisObj)) {
            return thisObj;
        }

        assert !JSSlowArray.isJSSlowArray(thisObj);
        JSDynamicObject.setJSClass(thisObj, JSSlowArray.INSTANCE);
        JSContext context = JSObject.getJSContext(thisObj);
        context.getFastArrayAssumption().invalidate("create slow ArgumentsObject");
        if (isArrayPrototype(thisObj)) {
            context.getArrayPrototypeNoElementsAssumption().invalidate("Array.prototype has no elements");
        }
        assert JSSlowArray.isJSSlowArray(thisObj);
        return thisObj;
    }

    private static boolean isArrayPrototype(DynamicObject thisObj) {
        return arrayGetArrayType(thisObj) instanceof ConstantEmptyPrototypeArray;
    }

    @Override
    public boolean testIntegrityLevel(DynamicObject thisObj, boolean frozen) {
        ScriptArray array = arrayGetArrayType(thisObj);
        boolean arrayIs = frozen ? array.isFrozen() : array.isSealed();
        return arrayIs && super.testIntegrityLevelFast(thisObj, frozen);
    }

    @Override
    public boolean setIntegrityLevel(DynamicObject thisObj, boolean freeze, boolean doThrow) {
        if (testIntegrityLevel(thisObj, freeze)) {
            return true;
        }

        ScriptArray arr = arrayGetArrayType(thisObj);
        arraySetArrayType(thisObj, freeze ? arr.freeze() : arr.seal());
        return super.setIntegrityLevelFast(thisObj, freeze);
    }

    @TruffleBoundary
    @Override
    public final boolean preventExtensions(DynamicObject thisObj, boolean doThrow) {
        boolean result = super.preventExtensions(thisObj, doThrow);
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
        JSObject.getJSContext(thisObj).getArrayPrototypeNoElementsAssumption().invalidate(ARRAY_PROTOTYPE_NO_ELEMENTS_INVALIDATION);
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
        assert JSRuntime.isPropertyKey(key);

        long idx = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(idx)) {
            ScriptArray array = arrayGetArrayType(thisObj);
            if (array.hasElement(thisObj, idx)) {
                Object value = array.getElement(thisObj, idx);
                return PropertyDescriptor.createData(value, true, !array.isFrozen(), !array.isSealed());
            }
        }
        Property prop = thisObj.getShape().getProperty(key);
        if (prop == null) {
            return null;
        }
        return JSNonProxy.ordinaryGetOwnPropertyIntl(thisObj, key, prop);
    }

    @Override
    public String toDisplayStringImpl(DynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return defaultToString(obj);
        } else {
            return JSRuntime.objectToDisplayString(obj, allowSideEffects, format, depth, null);
        }
    }

    protected boolean isSlowArray(DynamicObject thisObj) {
        return JSSlowArray.isJSSlowArray(thisObj);
    }

    @Override
    public boolean usesOrdinaryGetOwnProperty() {
        return false;
    }
}
