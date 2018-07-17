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
package com.oracle.truffle.js.nodes;

import java.nio.ByteBuffer;
import java.util.List;

import javax.script.Bindings;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSSIMD;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.builtins.JSWeakMap;
import com.oracle.truffle.js.runtime.builtins.JSWeakSet;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.interop.JavaPackage;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSGuards {

    private JSGuards() {
        // this class should not be instantiated
    }

    /**
     * Is this a DynamicObject representing a JavaScript object; this excludes Null and Undefined,
     * and excludes objects from other languages.
     */
    public static boolean isJSObject(Object value) {
        return JSRuntime.isObject(value);
    }

    /**
     * Is this a DynamicObject representing a JavaScript object; this excludes Null and Undefined,
     * and excludes objects from other languages.
     */
    public static boolean isJSObject(DynamicObject value) {
        return JSRuntime.isObject(value);
    }

    /**
     * Like isJSObject, but including Null and Undefined.
     */
    public static boolean isJSType(Object value) {
        return JSObject.isJSObject(value);
    }

    /**
     * Like isJSObject, but including Null and Undefined.
     */
    public static boolean isJSType(DynamicObject value) {
        return JSObject.isJSObject(value);
    }

    public static boolean isTruffleObject(Object value) {
        return value instanceof TruffleObject;
    }

    public static boolean isJavaLangString(Object value) {
        return value instanceof String;
    }

    public static boolean isForeignObject(Object value) {
        return JSRuntime.isForeignObject(value);
    }

    public static boolean isForeignObject(TruffleObject value) {
        return !JSObject.isJSObject(value) && !(value instanceof Symbol) && !(value instanceof JSLazyString) && !(value instanceof LargeInteger) && !(value instanceof BigInt);
    }

    public static boolean isUndefined(Object value) {
        return value == Undefined.instance;
    }

    public static boolean isJavaNull(Object o) {
        return o == null;
    }

    public static boolean isJSNull(Object value) {
        return value == Null.instance;
    }

    public static boolean isDynamicObject(Object value) {
        return JSObject.isDynamicObject(value);
    }

    public static boolean isJSFunction(Object value) {
        return JSFunction.isJSFunction(value);
    }

    public static boolean isBoundJSFunction(Object value) {
        return isJSFunction(value) && JSFunction.isBoundFunction((DynamicObject) value);
    }

    public static boolean isJSFunction(DynamicObject value) {
        return JSFunction.isJSFunction(value);
    }

    public static boolean isCallable(Object reviver) {
        return JSRuntime.isCallable(reviver);
    }

    public static boolean isCallableProxy(DynamicObject proxy) {
        return JSRuntime.isCallableProxy(proxy);
    }

    public static boolean isJSString(DynamicObject value) {
        return JSString.isJSString(value);
    }

    public static boolean isJSString(Object value) {
        return JSString.isJSString(value);
    }

    public static boolean isJSNumber(DynamicObject value) {
        return JSNumber.isJSNumber(value);
    }

    public static boolean isJSNumber(Object value) {
        return JSNumber.isJSNumber(value);
    }

    public static boolean isJSBigInt(DynamicObject value) {
        return JSBigInt.isJSBigInt(value);
    }

    public static boolean isJSBigInt(Object value) {
        return JSBigInt.isJSBigInt(value);
    }

    public static boolean isJSBoolean(DynamicObject value) {
        return JSBoolean.isJSBoolean(value);
    }

    public static boolean isJSBoolean(Object value) {
        return JSBoolean.isJSBoolean(value);
    }

    public static boolean isJSDate(DynamicObject value) {
        return JSDate.isJSDate(value);
    }

    public static boolean isJSDate(Object value) {
        return JSDate.isJSDate(value);
    }

    public static boolean isJSAbstractArray(DynamicObject value) {
        return isJSArray(value) || isJSArguments(value);
    }

    public static boolean isJSAbstractArray(Object value) {
        return isJSArray(value) || isJSArguments(value);
    }

    public static boolean isJSArray(DynamicObject value) {
        return JSArray.isJSArray(value);
    }

    public static boolean isJSArray(Object value) {
        return JSArray.isJSArray(value);
    }

    public static boolean isJSArguments(DynamicObject value) {
        return JSArgumentsObject.isJSArgumentsObject(value);
    }

    public static boolean isJSArguments(Object value) {
        return JSArgumentsObject.isJSArgumentsObject(value);
    }

    public static boolean isJSRegExp(Object value) {
        return JSRegExp.isJSRegExp(value);
    }

    public static boolean isJSRegExp(DynamicObject value) {
        return JSRegExp.isJSRegExp(value);
    }

    public static boolean isJSUserObject(DynamicObject value) {
        return JSUserObject.isJSUserObject(value);
    }

    public static boolean isJSUserObject(Object value) {
        return JSUserObject.isJSUserObject(value);
    }

    public static boolean isNumber(Object operand) {
        return JSRuntime.isNumber(operand);
    }

    public static boolean isJavaNumber(Object operand) {
        return JSRuntime.isJavaNumber(operand);
    }

    public static boolean isJavaObject(Object operand) {
        return JSRuntime.isJavaObject(operand);
    }

    public static boolean isNumberInteger(Object thisObj) {
        return thisObj instanceof Integer;
    }

    /**
     * Guard used to ensure that the parameter is a JSObject containing a JSNumber, that hosts an
     * Integer.
     */
    public static boolean isJSNumberInteger(DynamicObject thisObj) {
        return JSNumber.valueOf(thisObj) instanceof Integer;
    }

    public static boolean isString(Object operand) {
        return JSRuntime.isString(operand);
    }

    public static boolean isBoolean(Object operand) {
        return operand instanceof Boolean;
    }

    public static boolean isSymbol(Object operand) {
        return operand instanceof Symbol;
    }

    public static boolean isJSHeapArrayBuffer(DynamicObject thisObj) {
        return JSArrayBuffer.isJSHeapArrayBuffer(thisObj);
    }

    public static boolean isJSHeapArrayBuffer(Object thisObj) {
        return JSArrayBuffer.isJSHeapArrayBuffer(thisObj);
    }

    public static boolean isJSDirectArrayBuffer(DynamicObject thisObj) {
        return JSArrayBuffer.isJSDirectArrayBuffer(thisObj);
    }

    public static boolean isJSDirectArrayBuffer(Object thisObj) {
        return JSArrayBuffer.isJSDirectArrayBuffer(thisObj);
    }

    public static boolean isJSSharedArrayBuffer(DynamicObject thisObj) {
        return JSSharedArrayBuffer.isJSSharedArrayBuffer(thisObj);
    }

    public static boolean isJSSharedArrayBuffer(Object thisObj) {
        return JSSharedArrayBuffer.isJSSharedArrayBuffer(thisObj);
    }

    public static boolean isJSArrayBufferView(DynamicObject thisObj) {
        return JSArrayBufferView.isJSArrayBufferView(thisObj);
    }

    public static boolean isJSArrayBufferView(Object thisObj) {
        return JSArrayBufferView.isJSArrayBufferView(thisObj);
    }

    public static boolean isJSJavaWrapper(Object value) {
        return JSJavaWrapper.isJSJavaWrapper(value);
    }

    public static boolean isJSJavaWrapper(DynamicObject value) {
        return JSJavaWrapper.isJSJavaWrapper(value);
    }

    public static boolean isJSFastArray(DynamicObject value) {
        return JSArray.isJSFastArray(value);
    }

    public static boolean isJSFastArray(Object value) {
        return JSArray.isJSFastArray(value);
    }

    public static boolean isJSProxy(DynamicObject value) {
        return JSProxy.isProxy(value);
    }

    public static boolean isJSProxy(Object value) {
        return JSProxy.isProxy(value);
    }

    public static boolean isJSFastArguments(DynamicObject value) {
        return JSArgumentsObject.isJSFastArgumentsObject(value);
    }

    public static boolean isJSFastArguments(Object value) {
        return JSArgumentsObject.isJSFastArgumentsObject(value);
    }

    public static boolean isJSSymbol(DynamicObject value) {
        return JSSymbol.isJSSymbol(value);
    }

    public static boolean isJSSymbol(Object value) {
        return JSSymbol.isJSSymbol(value);
    }

    public static boolean isJSMap(DynamicObject value) {
        return JSMap.isJSMap(value);
    }

    public static boolean isJSMap(Object value) {
        return JSMap.isJSMap(value);
    }

    public static boolean isJSSet(DynamicObject value) {
        return JSSet.isJSSet(value);
    }

    public static boolean isJSSet(Object value) {
        return JSSet.isJSSet(value);
    }

    public static boolean isJSWeakMap(DynamicObject value) {
        return JSWeakMap.isJSWeakMap(value);
    }

    public static boolean isJSWeakMap(Object value) {
        return JSWeakMap.isJSWeakMap(value);
    }

    public static boolean isJSWeakSet(DynamicObject value) {
        return JSWeakSet.isJSWeakSet(value);
    }

    public static boolean isJSWeakSet(Object value) {
        return JSWeakSet.isJSWeakSet(value);
    }

    public static boolean isJSAdapter(Object object) {
        return JSAdapter.isJSAdapter(object);
    }

    public static boolean isJSAdapter(DynamicObject object) {
        return JSAdapter.isJSAdapter(object);
    }

    public static boolean isByteBuffer(Object buffer) {
        return buffer instanceof ByteBuffer;
    }

    public static boolean isJSSIMD(Object object) {
        return JSSIMD.isJSSIMD(object);
    }

    public static boolean isClass(Object value) {
        return value instanceof Class;
    }

    public static boolean isList(Object value) {
        return value instanceof List;
    }

    public static boolean isBindings(Object value) {
        return value instanceof Bindings;
    }

    public static boolean isJavaClass(Object target) {
        return target instanceof JavaClass;
    }

    public static boolean isJavaMethod(Object target) {
        return target instanceof JavaMethod;
    }

    public static boolean isJavaConstructor(Object target) {
        return target instanceof JavaMethod && ((JavaMethod) target).isConstructor();
    }

    public static boolean isJavaPackage(Object target) {
        return JavaPackage.isJavaPackage(target);
    }

    public static boolean isJavaArray(Object value) {
        return value != null && value.getClass().isArray();
    }

    public static boolean isBigInt(Object target) {
        return target instanceof BigInt;
    }

    public static boolean isBigIntZero(BigInt a) {
        return BigInt.ZERO.equals(a);
    }

    public static boolean isBigIntNegativeVal(BigInt a) {
        return a != null && a.signum() == -1;
    }

    public static boolean isDoubleInInt32Range(double value) {
        return Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE;
    }

    public static boolean isDoubleLargerThan2e32(double d) {
        return Math.abs(d) >= JSRuntime.TWO32;
    }

    public static boolean isLongFitsInt32(long value) {
        return JSRuntime.longIsRepresentableAsInt(value);
    }

    public static boolean isDoubleRepresentableAsLong(double d) {
        return JSRuntime.doubleIsRepresentableAsLong(d);
    }

    public static boolean isDoubleSafeInteger(double d) {
        return JSRuntime.isSafeInteger(d);
    }

    public static boolean isIntArrayIndex(int i) {
        return JSRuntime.isArrayIndex(i);
    }

    public static boolean isLongArrayIndex(long i) {
        return JSRuntime.isArrayIndex(i);
    }

    public static boolean isArgumentsDisconnected(DynamicObject argumentsArray) {
        return JSArgumentsObject.hasDisconnectedIndices(argumentsArray);
    }

    public static Class<? extends Number> getJavaNumberClass(Object value) {
        if (value != null && isJavaNumber(value)) {
            return ((Number) value).getClass();
        }
        return null;
    }

    public static Class<?> getNonJSObjectClass(Object value) {
        if (value != null && !JSObject.isJSObject(value)) {
            return value.getClass();
        }
        return null;
    }

    public static Class<?> getNonDynamicObjectClass(Object value) {
        if (value != null && !(value instanceof DynamicObject)) {
            return value.getClass();
        }
        return null;
    }

    public static Class<?> getNonTruffleObjectClass(Object value) {
        if (value != null && !(value instanceof TruffleObject)) {
            return value.getClass();
        }
        return null;
    }

    public static Class<?> getJavaObjectClass(Object value) {
        if (value != null && JSRuntime.isJavaObject(value)) {
            return value.getClass();
        }
        return null;
    }

    public static JSClass getJSClassChecked(DynamicObject object) {
        if (JSObject.isJSObject(object)) {
            return JSObject.getJSClass(object);
        } else {
            return null;
        }
    }

    public static boolean isReferenceEquals(Object a, Object b) {
        return a == b;
    }

    public static boolean isJavaPrimitive(Object o) {
        return o.getClass().isPrimitive();
    }

    public static boolean isNullOrUndefined(Object value) {
        return JSObject.isDynamicObject(value) && isNullOrUndefined((DynamicObject) value);
    }

    public static boolean isNullOrUndefined(DynamicObject value) {
        return value.getShape().getObjectType() == Null.NULL_CLASS;
    }

    public static boolean isTruffleJavaObject(TruffleObject object) {
        return AbstractJavaScriptLanguage.getCurrentEnv().isHostObject(object);
    }
}
