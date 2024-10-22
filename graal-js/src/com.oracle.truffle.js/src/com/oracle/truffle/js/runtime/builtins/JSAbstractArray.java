/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantEmptyPrototypeArray;
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

    public static final TruffleString LENGTH = Strings.LENGTH;

    protected static final String ARRAY_LENGTH_NOT_WRITABLE = "array length is not writable";
    private static final String LENGTH_PROPERTY_NOT_WRITABLE = "length property not writable";
    protected static final String CANNOT_REDEFINE_PROPERTY_LENGTH = "Cannot redefine property: length";
    protected static final String MAKE_SLOW_ARRAY_NEVER_PART_OF_COMPILATION_MESSAGE = "do not convert to slow array from compiled code";
    public static final String ARRAY_PROTOTYPE_NO_ELEMENTS_INVALIDATION = "Array.prototype no elements assumption";

    public static final HiddenKey LAZY_REGEX_RESULT_ID = new HiddenKey("lazyRegexResult");
    public static final HiddenKey LAZY_REGEX_ORIGINAL_INPUT_ID = new HiddenKey("lazyRegexResultOriginalInput");

    public static ScriptArray arrayGetArrayType(JSDynamicObject thisObj) {
        assert JSArray.isJSArray(thisObj) || JSArgumentsArray.isJSArgumentsObject(thisObj) || JSObjectPrototype.isJSObjectPrototype(thisObj);
        return arrayAccess().getArrayType(thisObj);
    }

    public static long arrayGetLength(JSDynamicObject thisObj) {
        return arrayAccess().getLength(thisObj);
    }

    public static int arrayGetUsedLength(JSDynamicObject thisObj) {
        return arrayAccess().getUsedLength(thisObj);
    }

    public static long arrayGetIndexOffset(JSDynamicObject thisObj) {
        return arrayAccess().getIndexOffset(thisObj);
    }

    public static int arrayGetArrayOffset(JSDynamicObject thisObj) {
        return arrayAccess().getArrayOffset(thisObj);
    }

    public static void arraySetArrayType(JSDynamicObject thisObj, ScriptArray arrayType) {
        arrayAccess().setArrayType(thisObj, arrayType);
    }

    public static void arraySetLength(JSDynamicObject thisObj, int length) {
        assert length >= 0;
        arrayAccess().setLength(thisObj, length);
    }

    public static void arraySetLength(JSDynamicObject thisObj, long length) {
        assert JSRuntime.isValidArrayLength(length);
        arrayAccess().setLength(thisObj, length);
    }

    public static void arraySetUsedLength(JSDynamicObject thisObj, int usedLength) {
        assert usedLength >= 0;
        arrayAccess().setUsedLength(thisObj, usedLength);
    }

    public static void arraySetIndexOffset(JSDynamicObject thisObj, long indexOffset) {
        arrayAccess().setIndexOffset(thisObj, indexOffset);
    }

    public static void arraySetArrayOffset(JSDynamicObject thisObj, int arrayOffset) {
        assert arrayOffset >= 0;
        arrayAccess().setArrayOffset(thisObj, arrayOffset);
    }

    public static Object arrayGetArray(JSDynamicObject thisObj) {
        assert JSObject.hasArray(thisObj);
        return arrayAccess().getArray(thisObj);
    }

    public static void arraySetArray(JSDynamicObject thisObj, Object array) {
        assert JSObject.hasArray(thisObj);
        assert array != null && (array.getClass().isArray() || array instanceof TreeMap<?, ?>);
        arrayAccess().setArray(thisObj, array);
    }

    public static int arrayGetHoleCount(JSDynamicObject thisObj) {
        return arrayAccess().getHoleCount(thisObj);
    }

    public static void arraySetHoleCount(JSDynamicObject thisObj, int holeCount) {
        assert holeCount >= 0;
        arrayAccess().setHoleCount(thisObj, holeCount);
    }

    public static ArrayAllocationSite arrayGetAllocationSite(JSDynamicObject thisObj) {
        return arrayAccess().getAllocationSite(thisObj);
    }

    public static Object arrayGetRegexResult(JSDynamicObject thisObj, DynamicObjectLibrary lazyRegexResult) {
        assert JSArray.isJSArray(thisObj);
        return Properties.getOrDefault(lazyRegexResult, thisObj, LAZY_REGEX_RESULT_ID, null);
    }

    public static TruffleString arrayGetRegexResultOriginalInput(JSDynamicObject thisObj, DynamicObjectLibrary lazyRegexResultOriginalInput) {
        return (TruffleString) Properties.getOrDefault(lazyRegexResultOriginalInput, thisObj, LAZY_REGEX_ORIGINAL_INPUT_ID, null);
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
            TruffleString str0 = JSRuntime.toString(arg0);
            TruffleString str1 = JSRuntime.toString(arg1);
            if (str0 == null) {
                if (str1 == null) {
                    return 0;
                }
                return 1;
            } else if (str1 == null) {
                return -1;
            }
            return Strings.compareTo(str0, str1);
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
            TruffleString str0 = Strings.fromInt(i1);
            TruffleString str1 = Strings.fromInt(i2);
            return Strings.compareTo(str0, str1);
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
            TruffleString str0 = JSRuntime.doubleToString(d1);
            TruffleString str1 = JSRuntime.doubleToString(d2);
            return Strings.compareTo(str0, str1);
        }
    }

    protected JSAbstractArray() {
    }

    protected static final ArrayAccess arrayAccess() {
        return ArrayAccess.SINGLETON;
    }

    public long getLength(JSDynamicObject thisObj) {
        return arrayGetLength(thisObj);
    }

    @TruffleBoundary
    public boolean setLength(JSDynamicObject thisObj, long length, boolean doThrow) {
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

    @TruffleBoundary
    @Override
    public final Object getOwnHelper(JSDynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        long idx = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(idx)) {
            return getOwnHelper(store, thisObj, idx, encapsulatingNode);
        }
        return super.getOwnHelper(store, thisObj, key, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public final boolean set(JSDynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
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
    public boolean set(JSDynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        if (receiver != thisObj) {
            return ordinarySetWithReceiver(thisObj, Strings.fromLong(index), value, receiver, isStrict, encapsulatingNode);
        }
        assert receiver == thisObj;
        if (arrayGetArrayType(thisObj).hasElement(thisObj, index)) {
            return setElement(thisObj, index, value, isStrict);
        } else {
            return setPropertySlow(thisObj, index, value, receiver, isStrict, encapsulatingNode);
        }
    }

    @TruffleBoundary
    private static boolean setPropertySlow(JSDynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        if (!JSObject.getJSContext(thisObj).getArrayPrototypeNoElementsAssumption().isValid() && setPropertyPrototypes(thisObj, index, value, receiver, isStrict, encapsulatingNode)) {
            return true;
        }

        if (!JSObject.isExtensible(thisObj)) {
            if (isStrict) {
                throw Errors.createTypeErrorNotExtensible(thisObj, Strings.fromLong(index));
            }
            return true;
        }
        return setElement(thisObj, index, value, isStrict);
    }

    private static boolean setPropertyPrototypes(JSDynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        // check prototype chain for accessors
        JSDynamicObject current = JSObject.getPrototype(thisObj);
        Object propertyName = null;
        while (current != Null.instance) {
            if (JSProxy.isJSProxy(current) || JSArrayBufferView.isJSArrayBufferView(current)) {
                return JSObject.getJSClass(current).set(current, index, value, receiver, false, encapsulatingNode);
            }
            if (JSObject.hasOwnProperty(current, index)) {
                if (propertyName == null) {
                    propertyName = Strings.fromLong(index);
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
            current = JSObject.getPrototype(current);
        }
        return false;
    }

    private static boolean setElement(JSDynamicObject thisObj, long index, Object value, boolean isStrict) {
        arraySetArrayType(thisObj, arrayGetArrayType(thisObj).setElement(thisObj, index, value, isStrict));
        return true;
    }

    @Override
    public boolean delete(JSDynamicObject thisObj, long index, boolean isStrict) {
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
    public Object getOwnHelper(JSDynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        ScriptArray array = arrayGetArrayType(store);
        if (array.hasElement(store, index)) {
            return array.getElement(store, index);
        }
        return super.getOwnHelper(store, thisObj, Strings.fromLong(index), encapsulatingNode);
    }

    /**
     * Creates an Object[] from this array, of size array.length. Does not check the prototype
     * chain, i.e. result can be wrong. Use JSToObjectArrayNode for more correct results.
     *
     * This is mostly used in tests, but also in a few places in Node.js.
     */
    @TruffleBoundary
    public static Object[] toArray(JSDynamicObject thisObj) {
        return arrayGetArrayType(thisObj).toArray(thisObj);
    }

    @TruffleBoundary
    @Override
    public final boolean hasOwnProperty(JSDynamicObject thisObj, Object key) {
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
    public final boolean hasOwnProperty(JSDynamicObject thisObj, long index) {
        ScriptArray array = arrayGetArrayType(thisObj);
        if (array.hasElement(thisObj, index)) {
            return true;
        }
        return super.hasOwnProperty(thisObj, Strings.fromLong(index));
    }

    @TruffleBoundary
    @Override
    public List<Object> getOwnPropertyKeys(JSDynamicObject thisObj, boolean strings, boolean symbols) {
        return ownPropertyKeysSlowArray(thisObj, strings, symbols);
    }

    @TruffleBoundary
    protected static List<Object> ownPropertyKeysFastArray(JSDynamicObject thisObj, boolean strings, boolean symbols) {
        assert JSArray.isJSFastArray(thisObj) || JSArgumentsArray.isJSFastArgumentsObject(thisObj);
        List<Object> indices = strings ? arrayGetArrayType(thisObj).ownPropertyKeys(thisObj) : Collections.emptyList();
        List<Object> keyList = thisObj.getShape().getKeyList();
        if (keyList.isEmpty()) {
            return indices;
        } else {
            List<Object> list = new ArrayList<>(keyList.size());
            if (strings) {
                keyList.forEach(k -> {
                    assert !(k instanceof TruffleString str && JSRuntime.isArrayIndexString(str));
                    if (k instanceof TruffleString) {
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
    protected static List<Object> ownPropertyKeysSlowArray(JSDynamicObject thisObj, boolean strings, boolean symbols) {
        List<Object> list = new ArrayList<>();

        if (strings) {
            ScriptArray array = arrayGetArrayType(thisObj);
            long currentIndex = array.firstElementIndex(thisObj);
            while (currentIndex <= array.lastElementIndex(thisObj)) {
                list.add(Strings.fromLong(currentIndex));
                currentIndex = array.nextElementIndex(thisObj, currentIndex);
            }
        }

        List<Object> keyList = thisObj.getShape().getKeyList();
        if (!keyList.isEmpty()) {
            if (strings) {
                int before = list.size();
                keyList.forEach(k -> {
                    if (k instanceof TruffleString str && JSRuntime.isArrayIndexString(str)) {
                        list.add(k);
                    }
                });
                int after = list.size();
                if (after != before) {
                    list.sort((o1, o2) -> {
                        return Long.compare(JSRuntime.propertyKeyToArrayIndex(o1), JSRuntime.propertyKeyToArrayIndex(o2));
                    });
                }
                keyList.forEach(k -> {
                    if (k instanceof TruffleString str && !JSRuntime.isArrayIndexString(str)) {
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

    public static long toArrayLengthOrRangeError(Object obj, Node originatingNode) {
        Number len = JSRuntime.toNumber(obj);
        Number len32 = JSRuntime.toUInt32(len);
        /*
         * ArraySetLength, steps 3 and 4: if Desc.[[Value]] is an object then its valueOf method is
         * called twice. This is legacy behaviour that was specified with this effect.
         */
        Number numberLen = JSRuntime.toNumber(obj);
        return toArrayLengthOrRangeError(numberLen, len32, originatingNode);
    }

    public static long toArrayLengthOrRangeError(Number len, Number len32, Node originatingNode) {
        double d32 = JSRuntime.doubleValue(len32);
        double d = JSRuntime.doubleValue(len);

        if (d32 == d) {
            return JSRuntime.longValue(len32);
        }
        if (d == 0) {
            return 0; // also handles the -0.0
        }
        throw Errors.createRangeErrorInvalidArrayLength(originatingNode);
    }

    @Override
    @TruffleBoundary
    public boolean defineOwnProperty(JSDynamicObject thisObj, Object key, PropertyDescriptor descriptor, boolean doThrow) {
        if (key instanceof TruffleString name) {
            if (Strings.equals(LENGTH, name)) {
                return defineOwnPropertyLength(thisObj, descriptor, doThrow);
            } else if (JSRuntime.isArrayIndexString(name)) {
                return defineOwnPropertyIndex(thisObj, name, descriptor, doThrow);
            }
        }
        return super.defineOwnProperty(thisObj, key, descriptor, doThrow);
    }

    /**
     * Implements the abstract operation ArraySetLength (A, Desc), redefining the "length" property
     * of an Array exotic object.
     *
     * @return whether the operation was successful
     */
    private boolean defineOwnPropertyLength(JSDynamicObject thisObj, PropertyDescriptor descriptor, boolean doThrow) {
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

    private static void setLengthNotWritable(JSDynamicObject thisObj) {
        arraySetArrayType(thisObj, arrayGetArrayType(thisObj).setLengthNotWritable());
    }

    private boolean deleteElementsAfterShortening(JSDynamicObject thisObj, PropertyDescriptor descriptor, boolean doThrow, long newLen, PropertyDescriptor lenDesc, long startPos) {
        assert JSRuntime.isValidArrayLength(newLen);
        long pos = startPos;
        while (pos > newLen) {
            pos--;
            Object key = Strings.fromLong(pos);
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

    private boolean definePropertyLength(JSDynamicObject thisObj, PropertyDescriptor descriptor, PropertyDescriptor currentDesc, long len, boolean doThrow) {
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
    protected boolean defineOwnPropertyIndex(JSDynamicObject thisObj, TruffleString name, PropertyDescriptor descriptor, boolean doThrow) {
        CompilerAsserts.neverPartOfCompilation();
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

    protected JSDynamicObject makeSlowArray(JSDynamicObject thisObj) {
        CompilerAsserts.neverPartOfCompilation(MAKE_SLOW_ARRAY_NEVER_PART_OF_COMPILATION_MESSAGE);
        if (isSlowArray(thisObj)) {
            return thisObj;
        }

        assert !JSSlowArray.isJSSlowArray(thisObj);
        JSDynamicObject.setJSClass(thisObj, JSSlowArray.INSTANCE);
        JSContext context = JSObject.getJSContext(thisObj);
        context.getFastArrayAssumption().invalidate("[[DefineOwnProperty]]");
        if (JSShape.isArrayPrototypeOrDerivative(thisObj)) {
            // The only Array exotic object that may reach here is the %Array.prototype%.
            if (context.getArrayPrototypeNoElementsAssumption().isValid()) {
                assert arrayGetArrayType(thisObj) instanceof ConstantEmptyPrototypeArray;
                context.getArrayPrototypeNoElementsAssumption().invalidate("Array.prototype.[[DefineOwnProperty]]");
            }
        }
        assert JSSlowArray.isJSSlowArray(thisObj);
        return thisObj;
    }

    @TruffleBoundary
    @Override
    public final boolean preventExtensions(JSDynamicObject thisObj, boolean doThrow) {
        boolean result = super.preventExtensions(thisObj, doThrow);
        ScriptArray arr = arrayGetArrayType(thisObj);
        arraySetArrayType(thisObj, arr.preventExtensions());
        assert !isExtensible(thisObj);
        return result;
    }

    @TruffleBoundary
    @Override
    public boolean delete(JSDynamicObject thisObj, Object key, boolean isStrict) {
        long index = JSRuntime.propertyKeyToArrayIndex(key);
        if (index >= 0) {
            return delete(thisObj, index, isStrict);
        } else {
            return super.delete(thisObj, key, isStrict);
        }
    }

    @Override
    public PropertyDescriptor getOwnProperty(JSDynamicObject thisObj, Object key) {
        return ordinaryGetOwnPropertyArray(thisObj, key);
    }

    /**
     * 9.1.5.1 OrdinaryGetOwnProperty (O, P), implemented for Arrays.
     */
    @TruffleBoundary
    public static PropertyDescriptor ordinaryGetOwnPropertyArray(JSDynamicObject thisObj, Object key) {
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

    protected boolean isSlowArray(JSDynamicObject thisObj) {
        return JSSlowArray.isJSSlowArray(thisObj);
    }

    @Override
    public boolean usesOrdinaryGetOwnProperty() {
        return false;
    }
}
