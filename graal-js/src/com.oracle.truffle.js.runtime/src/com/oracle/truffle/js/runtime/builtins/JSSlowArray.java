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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;

public final class JSSlowArray extends JSAbstractArray {

    public static final String CLASS_NAME = "Array";

    public static final JSSlowArray INSTANCE = new JSSlowArray();

    private JSSlowArray() {
    }

    public static boolean isJSSlowArray(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSSlowArray((DynamicObject) obj);
    }

    public static boolean isJSSlowArray(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @TruffleBoundary
    @Override
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index) {
        String indexAsString = Boundaries.stringValueOf(index);
        if (JSUserObject.INSTANCE.hasOwnProperty(store, indexAsString)) {
            return JSUserObject.INSTANCE.getOwnHelper(store, thisObj, indexAsString);
        }
        return super.getOwnHelper(store, thisObj, index);
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict) {
        String indexAsString = Boundaries.stringValueOf(index);
        if (JSUserObject.INSTANCE.hasOwnProperty(thisObj, indexAsString)) {
            return JSUserObject.INSTANCE.setOwn(thisObj, indexAsString, value, receiver, isStrict);
        }
        return super.set(thisObj, index, value, receiver, isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        ScriptArray array = arrayGetArrayType(thisObj);
        if (array.hasElement(thisObj, index)) {
            if (array.isSealed()) {
                if (isStrict) {
                    throw Errors.createTypeErrorCannotDeletePropertyOfSealedArray(index);
                }
                return false;
            }
            arraySetArrayType(thisObj, array.deleteElement(thisObj, index, isStrict));
            return true;
        } else {
            return JSUserObject.INSTANCE.delete(thisObj, index, isStrict);
        }
    }

    @Override
    protected DynamicObject makeSlowArray(DynamicObject thisObj) {
        assert JSSlowArray.isJSSlowArray(thisObj);
        return thisObj;
    }

    /**
     * Implements part "3" of 15.4.5.1 [[DefineOwnProperty]], redefining one of the index property
     * of an Array.
     *
     * @return whether the operation was successful
     */
    @Override
    protected boolean defineOwnPropertyIndex(DynamicObject thisObj, String name, PropertyDescriptor descriptor, boolean doThrow) {
        long index = JSRuntime.toUInt32(name);
        if (index >= this.getLength(thisObj)) {
            PropertyDescriptor desc = getOwnProperty(thisObj, LENGTH);
            if (!desc.getWritable()) {
                return DefinePropertyUtil.reject(doThrow, ARRAY_LENGTH_NOT_WRITABLE);
            }
        }

        if (this.getLength(thisObj) <= index) {
            this.setLength(thisObj, (index + 1), doThrow);
        }
        if (arrayGetArrayType(thisObj).hasElement(thisObj, index) && !JSUserObject.INSTANCE.hasOwnProperty(thisObj, name)) {
            // apply the default attributes to the property first
            JSContext context = JSObject.getJSContext(thisObj);
            JSObjectUtil.putDataProperty(context, thisObj, name, get(thisObj, index), JSAttributes.getDefault());
            arraySetArrayType(thisObj, arrayGetArrayType(thisObj).deleteElement(thisObj, index, false));
        }

        boolean succeeded = jsDefineProperty(thisObj, index, descriptor, false);
        if (!succeeded) {
            return DefinePropertyUtil.reject(doThrow, JSTruffleOptions.NashornCompatibilityMode ? "cannot set property" : "Cannot redefine property");
        }
        return true;
    }

    protected static boolean jsDefineProperty(DynamicObject thisObj, long index, PropertyDescriptor descriptor, boolean doThrow) {
        ScriptArray internalArray = arrayGetArrayType(thisObj);
        boolean copyValue = (internalArray.hasElement(thisObj, index) && (!descriptor.hasValue() && !descriptor.hasGet()));
        boolean succeed = DefinePropertyUtil.ordinaryDefineOwnProperty(thisObj, Boundaries.stringValueOf(index), descriptor, doThrow);
        if (copyValue) {
            JSObject.set(thisObj, index, internalArray.getElement(thisObj, index), doThrow);
        }
        return succeed;
    }

    @TruffleBoundary
    @Override
    public boolean setLength(DynamicObject thisObj, long length, boolean doThrow) {
        if (testIntegrityLevel(thisObj, true)) {
            throw Errors.createTypeError("cannot set length of a frozen array");
        }
        long oldLen = getLength(thisObj);
        long newLen = length;
        ScriptArray internalArray = arrayGetArrayType(thisObj);
        boolean sealed = internalArray.isSealed();
        boolean deleteSucceeded = true;
        if (newLen < oldLen) {
            for (long idx = oldLen - 1; idx >= newLen; idx--) {
                if (internalArray.hasElement(thisObj, idx)) {
                    deleteSucceeded = !sealed;
                } else {
                    deleteSucceeded = JSUserObject.INSTANCE.delete(thisObj, idx, false);
                }
                if (!deleteSucceeded) {
                    newLen = idx + 1;
                    break;
                }
            }
        }
        if (newLen > Integer.MAX_VALUE && !(internalArray instanceof SparseArray)) {
            internalArray = SparseArray.makeSparseArray(thisObj, internalArray);
        }
        arraySetArrayType(thisObj, internalArray.setLength(thisObj, newLen, doThrow));

        if (!deleteSucceeded) {
            return DefinePropertyUtil.reject(doThrow, JSTruffleOptions.NashornCompatibilityMode ? "cannot set property: length" : "Cannot redefine property: length");
        }
        return true;
    }
}
