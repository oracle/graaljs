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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;

public final class JSSlowArray extends JSAbstractArray {

    public static final String CLASS_NAME = "Array";

    public static final JSSlowArray INSTANCE = new JSSlowArray();

    private JSSlowArray() {
    }

    public static boolean isJSSlowArray(Object obj) {
        return JSDynamicObject.isJSDynamicObject(obj) && isJSSlowArray((DynamicObject) obj);
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
    public Object getOwnHelper(DynamicObject store, Object thisObj, long index, Node encapsulatingNode) {
        String indexAsString = Boundaries.stringValueOf(index);
        if (JSOrdinary.INSTANCE.hasOwnProperty(store, indexAsString)) {
            return JSOrdinary.INSTANCE.getOwnHelper(store, thisObj, indexAsString, encapsulatingNode);
        }
        return super.getOwnHelper(store, thisObj, index, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        String indexAsString = Boundaries.stringValueOf(index);
        if (JSOrdinary.INSTANCE.hasOwnProperty(thisObj, indexAsString)) {
            return ordinarySet(thisObj, indexAsString, value, receiver, isStrict, encapsulatingNode);
        }
        return super.set(thisObj, index, value, receiver, isStrict, encapsulatingNode);
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        ScriptArray array = arrayAccess().getArrayType(thisObj);
        if (array.hasElement(thisObj, index)) {
            ScriptArray arrayType = arrayGetArrayType(thisObj);
            if (arrayType.canDeleteElement(thisObj, index, isStrict)) {
                arraySetArrayType(thisObj, arrayType.deleteElement(thisObj, index, isStrict));
                return true;
            } else {
                return false;
            }
        } else {
            return JSOrdinary.INSTANCE.delete(thisObj, index, isStrict);
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
        if (index >= getLength(thisObj)) {
            PropertyDescriptor desc = getOwnProperty(thisObj, LENGTH);
            if (!desc.getWritable()) {
                return DefinePropertyUtil.reject(doThrow, ARRAY_LENGTH_NOT_WRITABLE);
            }
        }

        if (this.getLength(thisObj) <= index) {
            this.setLength(thisObj, (index + 1), doThrow);
        }
        ScriptArray arrayType = arrayGetArrayType(thisObj);
        if (arrayType.hasElement(thisObj, index) && !JSOrdinary.INSTANCE.hasOwnProperty(thisObj, name)) {
            // replace with a regular property first
            JSContext context = JSObject.getJSContext(thisObj);
            boolean wasNotExtensible = !JSShape.isExtensible(thisObj.getShape());
            JSObjectUtil.putDataProperty(context, thisObj, name, get(thisObj, index), JSAttributes.fromConfigurableEnumerableWritable(!arrayType.isSealed(), true, !arrayType.isFrozen()));
            if (wasNotExtensible) {
                assert !JSShape.isExtensible(thisObj.getShape());
            }

            // Using deleteElementImpl() instead of deleteElement() because the property
            // should be removed even from sealed arrays (it is being replaced by
            // by a regular data property defined above).
            arraySetArrayType(thisObj, arrayType.deleteElementImpl(thisObj, index, false));
        }

        boolean succeeded = jsDefineProperty(thisObj, index, descriptor, false);
        if (!succeeded) {
            JSContext context = JavaScriptLanguage.getCurrentLanguage().getJSContext();
            return DefinePropertyUtil.reject(doThrow, context.isOptionNashornCompatibilityMode() ? "cannot set property" : "Cannot redefine property");
        }
        return true;
    }

    private static boolean jsDefineProperty(DynamicObject thisObj, long index, PropertyDescriptor descriptor, boolean doThrow) {
        ScriptArray internalArray = arrayAccess().getArrayType(thisObj);
        boolean copyValue = (internalArray.hasElement(thisObj, index) && (!descriptor.hasValue() && !descriptor.hasGet()));
        boolean succeed = DefinePropertyUtil.ordinaryDefineOwnProperty(thisObj, Boundaries.stringValueOf(index), descriptor, doThrow);
        if (copyValue) {
            JSObject.set(thisObj, index, internalArray.getElement(thisObj, index), doThrow, null);
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
                    deleteSucceeded = JSOrdinary.INSTANCE.delete(thisObj, idx, false);
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
            JSContext context = JavaScriptLanguage.getCurrentLanguage().getJSContext();
            return DefinePropertyUtil.reject(doThrow, context.isOptionNashornCompatibilityMode() ? "cannot set property: length" : CANNOT_REDEFINE_PROPERTY_LENGTH);
        }
        return true;
    }
}
