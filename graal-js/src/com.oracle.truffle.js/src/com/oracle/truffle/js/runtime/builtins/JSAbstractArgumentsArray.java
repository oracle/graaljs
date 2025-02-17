/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;

public abstract class JSAbstractArgumentsArray extends JSAbstractArray {

    public static final TruffleString CALLEE = Strings.CALLEE;
    public static final TruffleString CALLER = Strings.CALLER;
    protected static final TruffleString CLASS_NAME = Strings.constant("Arguments");

    @TruffleBoundary
    @Override
    public long getLength(JSDynamicObject thisObj) {
        Object lengthValue = get(thisObj, LENGTH);
        return JSRuntime.toInteger(JSRuntime.toNumber(lengthValue));
    }

    @TruffleBoundary
    @Override
    public boolean delete(JSDynamicObject thisObj, long index, boolean isStrict) {
        if (isMappedArguments(thisObj)) {
            makeSlowArray(thisObj);
            return JSObject.delete(thisObj, index, isStrict);
        } else {
            return super.delete(thisObj, index, isStrict);
        }
    }

    @TruffleBoundary
    @Override
    public boolean delete(JSDynamicObject thisObj, Object key, boolean isStrict) {
        long index = JSRuntime.propertyKeyToArrayIndex(key);
        if (index >= 0 && JSRuntime.isArrayIndex(index)) {
            return delete(thisObj, index, isStrict);
        } else {
            return super.delete(thisObj, key, isStrict);
        }
    }

    protected static boolean isMappedArguments(JSDynamicObject thisObj) {
        return thisObj instanceof JSArgumentsObject.Mapped;
    }

    @Override
    protected JSDynamicObject makeSlowArray(JSDynamicObject thisObj) {
        CompilerAsserts.neverPartOfCompilation(MAKE_SLOW_ARRAY_NEVER_PART_OF_COMPILATION_MESSAGE);
        if (isSlowArray(thisObj)) {
            return thisObj;
        }

        assert JSArgumentsArray.isJSFastArgumentsObject(thisObj);
        JSDynamicObject.setJSClass(thisObj, JSSlowArgumentsArray.INSTANCE);
        if (isMappedArguments(thisObj)) {
            ((JSArgumentsObject.Mapped) thisObj).initDisconnectedIndices();
        }
        JSObject.getJSContext(thisObj).getFastArgumentsObjectAssumption().invalidate("create slow ArgumentsObject");
        return thisObj;
    }

    public static int getConnectedArgumentCount(JSDynamicObject argumentsArray) {
        assert JSArgumentsArray.isJSArgumentsObject(argumentsArray);
        return ((JSArgumentsObject.Mapped) argumentsArray).getConnectedArgumentCount();
    }

    @TruffleBoundary
    private static Map<Long, Object> getDisconnectedIndices(JSDynamicObject argumentsArray) {
        assert hasDisconnectedIndices(argumentsArray);
        return ((JSArgumentsObject.Mapped) argumentsArray).getDisconnectedIndices();
    }

    @TruffleBoundary
    public static boolean wasIndexDisconnected(JSDynamicObject argumentsArray, long index) {
        assert hasDisconnectedIndices(argumentsArray);
        return getDisconnectedIndices(argumentsArray).containsKey(index);
    }

    @TruffleBoundary
    public static Object getDisconnectedIndexValue(JSDynamicObject argumentsArray, long index) {
        assert hasDisconnectedIndices(argumentsArray);
        assert wasIndexDisconnected(argumentsArray, index);
        return getDisconnectedIndices(argumentsArray).get(index);
    }

    @TruffleBoundary
    public static Object setDisconnectedIndexValue(JSDynamicObject argumentsArray, long index, Object value) {
        assert hasDisconnectedIndices(argumentsArray);
        assert wasIndexDisconnected(argumentsArray, index);
        getDisconnectedIndices(argumentsArray).put(index, value);
        return value;
    }

    @TruffleBoundary
    public static void disconnectIndex(JSDynamicObject argumentsArray, long index, Object oldValue) {
        if (!hasDisconnectedIndices(argumentsArray)) {
            JSArgumentsArray.INSTANCE.makeSlowArray(argumentsArray);
        }
        getDisconnectedIndices(argumentsArray).put(index, oldValue);
    }

    public static boolean hasDisconnectedIndices(JSDynamicObject argumentsArray) {
        return JSSlowArgumentsArray.isJSSlowArgumentsObject(argumentsArray);
    }

    @TruffleBoundary
    @Override
    public boolean defineOwnProperty(JSDynamicObject thisObj, Object key, PropertyDescriptor descriptor, boolean doThrow) {
        boolean isMappedArguments = isMappedArguments(thisObj);
        long index = JSRuntime.propertyKeyToArrayIndex(key);
        Object oldValue = null;
        boolean isIndexConnected = false;
        if (index >= 0) {
            makeSlowArray(thisObj);
            isIndexConnected = isMappedArguments && !wasIndexDisconnected(thisObj, index);
            oldValue = super.get(thisObj, index);

            ScriptArray arrayType = arrayGetArrayType(thisObj);
            if (arrayType.hasElement(thisObj, index)) {
                // apply the default attributes to the property first
                JSObjectUtil.defineDataProperty(thisObj, key, oldValue, JSAttributes.getDefault());
                if (arrayType.canDeleteElement(thisObj, index, false)) {
                    arraySetArrayType(thisObj, arrayType.deleteElement(thisObj, index, false));
                }
            }
        }

        boolean allowed = DefinePropertyUtil.ordinaryDefineOwnProperty(thisObj, key, descriptor, doThrow);
        if (!allowed) {
            return DefinePropertyUtil.reject(doThrow, "not allowed to defineProperty on an arguments object");
        }

        if (isIndexConnected) {
            definePropertyMapped(thisObj, (TruffleString) key, descriptor, index, oldValue, thisObj);
        }
        return true;
    }

    @TruffleBoundary
    private static void definePropertyMapped(JSDynamicObject thisObj, TruffleString name, PropertyDescriptor descriptor, long index, Object oldValueParam, JSDynamicObject obj) {
        if (descriptor.isAccessorDescriptor()) {
            disconnectIndex(thisObj, index, oldValueParam);
        } else {
            Object oldValue = oldValueParam;
            if (descriptor.hasValue()) {
                Object value = descriptor.getValue();
                JSObject.set(obj, name, value);
                oldValue = value;
            }
            if (descriptor.hasWritable()) {
                Object value = descriptor.getWritable();
                if (Boolean.FALSE.equals(value)) {
                    disconnectIndex(thisObj, index, oldValue);
                }
            }
        }
    }

    @TruffleBoundary
    @Override
    public PropertyDescriptor getOwnProperty(JSDynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);

        PropertyDescriptor desc = ordinaryGetOwnPropertyArray(thisObj, key);
        if (desc == null) {
            return null;
        }
        long index = JSRuntime.propertyKeyToArrayIndex(key);
        if (index >= 0) {
            boolean isMapped = JSArgumentsArray.isJSFastArgumentsObject(thisObj) || (isMappedArguments(thisObj) && !wasIndexDisconnected(thisObj, index));
            if (isMapped) {
                desc.setValue(super.get(thisObj, index));
            }
        }
        if (desc.isDataDescriptor() && CALLER.equals(key) && desc.getValue() instanceof JSFunctionObject function && JSFunction.isStrict(function)) {
            throw Errors.createTypeError("caller not allowed in strict mode");
        }
        return desc;
    }

    @Override
    protected boolean isSlowArray(JSDynamicObject thisObj) {
        return JSSlowArgumentsArray.isJSSlowArgumentsObject(thisObj);
    }
}
