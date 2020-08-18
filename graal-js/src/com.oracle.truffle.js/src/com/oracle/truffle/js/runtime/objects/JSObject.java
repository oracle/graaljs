/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.objects;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBase;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSObjectPrototype;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * The common base class for all JavaScript objects (values of type Object according to the spec).
 *
 * Includes static methods for dealing with JS objects (internal methods).
 */
public abstract class JSObject extends JSDynamicObject {

    public static final String CONSTRUCTOR = "constructor";
    public static final String PROTOTYPE = "prototype";
    public static final String PROTO = "__proto__";
    public static final HiddenKey HIDDEN_PROTO = new HiddenKey(PROTO);

    public static final String NO_SUCH_PROPERTY_NAME = "__noSuchProperty__";
    public static final String NO_SUCH_METHOD_NAME = "__noSuchMethod__";

    protected JSObject(Shape shape) {
        super(shape);
    }

    protected JSObject copyWithoutProperties(@SuppressWarnings("unused") Shape shape) {
        throw Errors.notImplemented("copy");
    }

    /**
     * Returns whether object is a DynamicObject of JavaScript.
     */
    public static boolean isJSDynamicObject(Object object) {
        return object instanceof JSDynamicObject;
    }

    /**
     * Returns whether object is a proper JavaScript Object.
     */
    public static boolean isJSObject(Object object) {
        return JSRuntime.isObject(object);
    }

    public static JSClass getJSClass(DynamicObject obj) {
        return JSShape.getJSClass(obj.getShape());
    }

    @TruffleBoundary
    public static DynamicObject getPrototype(DynamicObject obj) {
        return JSObject.getJSClass(obj).getPrototypeOf(obj);
    }

    public static DynamicObject getPrototype(DynamicObject obj, JSClassProfile jsclassProfile) {
        return jsclassProfile.getJSClass(obj).getPrototypeOf(obj);
    }

    @TruffleBoundary
    public static boolean setPrototype(DynamicObject obj, DynamicObject newPrototype) {
        assert newPrototype != null;
        return JSObject.getJSClass(obj).setPrototypeOf(obj, newPrototype);
    }

    public static boolean setPrototype(DynamicObject obj, DynamicObject newPrototype, JSClassProfile jsclassProfile) {
        assert newPrototype != null;
        return jsclassProfile.getJSClass(obj).setPrototypeOf(obj, newPrototype);
    }

    @TruffleBoundary
    public static Object get(DynamicObject obj, long index) {
        return JSObject.getJSClass(obj).get(obj, index);
    }

    @TruffleBoundary
    public static Object get(DynamicObject obj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).get(obj, key);
    }

    @TruffleBoundary
    public static Object get(TruffleObject obj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (isJSDynamicObject(obj)) {
            return get((DynamicObject) obj, key);
        } else {
            return JSInteropUtil.readMemberOrDefault(obj, key, Undefined.instance);
        }
    }

    @TruffleBoundary
    public static Object get(TruffleObject obj, long index) {
        if (isJSDynamicObject(obj)) {
            return get((DynamicObject) obj, index);
        } else {
            return JSInteropUtil.readArrayElementOrDefault(obj, index, Undefined.instance);
        }
    }

    @TruffleBoundary
    public static Object getMethod(DynamicObject obj, Object name) {
        assert JSRuntime.isPropertyKey(name);
        Object result = JSRuntime.nullToUndefined(JSObject.getJSClass(obj).getMethodHelper(obj, obj, name));
        return (result == Null.instance) ? Undefined.instance : result;
    }

    @TruffleBoundary
    public static boolean set(DynamicObject obj, long index, Object value) {
        return set(obj, index, value, false);
    }

    @TruffleBoundary
    public static boolean set(DynamicObject obj, Object key, Object value) {
        return set(obj, key, value, false);
    }

    @TruffleBoundary
    public static boolean set(DynamicObject obj, long index, Object value, boolean isStrict) {
        return JSObject.getJSClass(obj).set(obj, index, value, obj, isStrict);
    }

    @TruffleBoundary
    public static boolean set(DynamicObject obj, Object key, Object value, boolean isStrict) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).set(obj, key, value, obj, isStrict);
    }

    /**
     * [[Set]] with a receiver different than the default.
     */
    @TruffleBoundary
    public static boolean setWithReceiver(DynamicObject obj, Object key, Object value, Object receiver, boolean isStrict) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).set(obj, key, value, receiver, isStrict);
    }

    @TruffleBoundary
    public static boolean setWithReceiver(DynamicObject obj, long index, Object value, Object receiver, boolean isStrict) {
        return JSObject.getJSClass(obj).set(obj, index, value, receiver, isStrict);
    }

    public static boolean setWithReceiver(DynamicObject obj, Object key, Object value, Object receiver, boolean isStrict, JSClassProfile classProfile) {
        assert JSRuntime.isPropertyKey(key);
        return classProfile.getJSClass(obj).set(obj, key, value, receiver, isStrict);
    }

    public static boolean setWithReceiver(DynamicObject obj, long index, Object value, Object receiver, boolean isStrict, JSClassProfile classProfile) {
        return classProfile.getJSClass(obj).set(obj, index, value, receiver, isStrict);
    }

    @TruffleBoundary
    public static boolean delete(DynamicObject obj, long index) {
        return JSObject.getJSClass(obj).delete(obj, index, false);
    }

    @TruffleBoundary
    public static boolean delete(DynamicObject obj, long index, boolean isStrict) {
        return JSObject.getJSClass(obj).delete(obj, index, isStrict);
    }

    public static boolean delete(DynamicObject obj, long index, boolean isStrict, JSClassProfile classProfile) {
        return classProfile.getJSClass(obj).delete(obj, index, isStrict);
    }

    @TruffleBoundary
    public static boolean delete(DynamicObject obj, Object key) {
        return delete(obj, key, false);
    }

    @TruffleBoundary
    public static boolean delete(DynamicObject obj, Object key, boolean isStrict) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).delete(obj, key, isStrict);
    }

    public static boolean delete(DynamicObject obj, Object key, boolean isStrict, JSClassProfile classProfile) {
        assert JSRuntime.isPropertyKey(key);
        return classProfile.getJSClass(obj).delete(obj, key, isStrict);
    }

    @TruffleBoundary
    public static boolean hasOwnProperty(DynamicObject obj, long index) {
        return JSObject.getJSClass(obj).hasOwnProperty(obj, index);
    }

    public static boolean hasOwnProperty(DynamicObject obj, long index, JSClassProfile classProfile) {
        return classProfile.getJSClass(obj).hasOwnProperty(obj, index);
    }

    @TruffleBoundary
    public static boolean hasOwnProperty(DynamicObject obj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).hasOwnProperty(obj, key);
    }

    public static boolean hasOwnProperty(DynamicObject obj, Object key, JSClassProfile classProfile) {
        assert JSRuntime.isPropertyKey(key);
        return classProfile.getJSClass(obj).hasOwnProperty(obj, key);
    }

    @TruffleBoundary
    public static boolean hasProperty(DynamicObject obj, long index) {
        return JSObject.getJSClass(obj).hasProperty(obj, index);
    }

    public static boolean hasProperty(DynamicObject obj, long index, JSClassProfile classProfile) {
        return classProfile.getJSClass(obj).hasProperty(obj, index);
    }

    @TruffleBoundary
    public static boolean hasProperty(DynamicObject obj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).hasProperty(obj, key);
    }

    public static boolean hasProperty(DynamicObject obj, Object key, JSClassProfile classProfile) {
        assert JSRuntime.isPropertyKey(key);
        return classProfile.getJSClass(obj).hasProperty(obj, key);
    }

    public static PropertyDescriptor getOwnProperty(DynamicObject obj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).getOwnProperty(obj, key);
    }

    public static PropertyDescriptor getOwnProperty(DynamicObject obj, Object key, JSClassProfile classProfile) {
        assert JSRuntime.isPropertyKey(key);
        return classProfile.getJSClass(obj).getOwnProperty(obj, key);
    }

    /**
     * [[OwnPropertyKeys]]. The returned keys are instanceof (String, Symbol).
     */
    public static List<Object> ownPropertyKeys(DynamicObject obj) {
        return JSObject.getJSClass(obj).ownPropertyKeys(obj);
    }

    public static List<Object> ownPropertyKeys(DynamicObject obj, JSClassProfile classProfile) {
        return classProfile.getJSClass(obj).ownPropertyKeys(obj);
    }

    /**
     * 7.3.21 EnumerableOwnNames (O).
     */
    @TruffleBoundary
    public static List<String> enumerableOwnNames(DynamicObject thisObj) {
        JSClass jsclass = JSObject.getJSClass(thisObj);
        if (JSConfig.FastOwnKeys && jsclass.hasOnlyShapeProperties(thisObj)) {
            return JSShape.getEnumerablePropertyNames(thisObj.getShape());
        }
        Iterable<Object> ownKeys = jsclass.ownPropertyKeys(thisObj);
        List<String> names = new ArrayList<>();
        for (Object obj : ownKeys) {
            if (obj instanceof String) {
                PropertyDescriptor desc = jsclass.getOwnProperty(thisObj, obj);
                if (desc != null && desc.getEnumerable()) {
                    names.add((String) obj);
                }
            }
        }
        return names;
    }

    @TruffleBoundary
    public static boolean defineOwnProperty(DynamicObject obj, Object key, PropertyDescriptor desc) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).defineOwnProperty(obj, key, desc, false);
    }

    @TruffleBoundary
    public static boolean defineOwnProperty(DynamicObject obj, Object key, PropertyDescriptor desc, boolean doThrow) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).defineOwnProperty(obj, key, desc, doThrow);
    }

    public static Object get(DynamicObject obj, Object key, JSClassProfile jsclassProfile) {
        assert JSRuntime.isPropertyKey(key);
        return jsclassProfile.getJSClass(obj).get(obj, key);
    }

    public static Object get(DynamicObject obj, long index, JSClassProfile jsclassProfile) {
        return jsclassProfile.getJSClass(obj).get(obj, index);
    }

    public static Object getOrDefault(DynamicObject obj, Object key, Object receiver, Object defaultValue, JSClassProfile jsclassProfile) {
        assert JSRuntime.isPropertyKey(key);
        Object result = jsclassProfile.getJSClass(obj).getHelper(obj, receiver, key);
        return result == null ? defaultValue : result;
    }

    public static Object getOrDefault(DynamicObject obj, long index, Object receiver, Object defaultValue, JSClassProfile jsclassProfile) {
        Object result = jsclassProfile.getJSClass(obj).getHelper(obj, receiver, index);
        return result == null ? defaultValue : result;
    }

    @TruffleBoundary
    public static Object getWithReceiver(DynamicObject obj, Object key, Object receiver) {
        assert JSRuntime.isPropertyKey(key);
        Object result = getJSClass(obj).getHelper(obj, receiver, key);
        return result == null ? Undefined.instance : result;
    }

    @TruffleBoundary
    public static String defaultToString(DynamicObject obj) {
        return JSObject.getJSClass(obj).defaultToString(obj);
    }

    @TruffleBoundary
    public static String toDisplayString(DynamicObject obj, int depth, boolean allowSideEffects) {
        return JSObject.getJSClass(obj).toDisplayStringImpl(obj, depth, allowSideEffects, JavaScriptLanguage.getCurrentLanguage().getJSContext());
    }

    /**
     * ES2015 7.1.1 ToPrimitive in case an Object is passed.
     */
    @TruffleBoundary
    public static Object toPrimitive(DynamicObject obj, String hint) {
        assert obj != Null.instance && obj != Undefined.instance;
        Object exoticToPrim = JSObject.getMethod(obj, Symbol.SYMBOL_TO_PRIMITIVE);
        if (exoticToPrim != Undefined.instance) {
            Object result = JSRuntime.call(exoticToPrim, obj, new Object[]{hint});
            if (JSRuntime.isObject(result)) {
                throw Errors.createTypeError("[Symbol.toPrimitive] method returned a non-primitive object");
            }
            return result;
        }
        if (hint.equals(JSRuntime.HINT_DEFAULT)) {
            return ordinaryToPrimitive(obj, JSRuntime.HINT_NUMBER);
        } else {
            return ordinaryToPrimitive(obj, hint);
        }
    }

    @TruffleBoundary
    public static Object toPrimitive(DynamicObject obj) {
        return toPrimitive(obj, JSRuntime.HINT_DEFAULT);
    }

    /**
     * ES2018 7.1.1.1 OrdinaryToPrimitive.
     */
    @TruffleBoundary
    public static Object ordinaryToPrimitive(DynamicObject obj, String hint) {
        assert JSRuntime.isObject(obj);
        assert JSRuntime.HINT_STRING.equals(hint) || JSRuntime.HINT_NUMBER.equals(hint);
        String[] methodNames;
        if (JSRuntime.HINT_STRING.equals(hint)) {
            methodNames = new String[]{JSRuntime.TO_STRING, JSRuntime.VALUE_OF};
        } else {
            methodNames = new String[]{JSRuntime.VALUE_OF, JSRuntime.TO_STRING};
        }
        for (String name : methodNames) {
            Object method = JSObject.getMethod(obj, name);
            if (JSRuntime.isCallable(method)) {
                Object result = JSRuntime.call(method, obj, new Object[]{});
                if (!JSRuntime.isObject(result)) {
                    return result;
                }
            }
        }
        throw Errors.createTypeErrorCannotConvertToPrimitiveValue();
    }

    @TruffleBoundary
    public static boolean preventExtensions(DynamicObject obj) {
        return preventExtensions(obj, false);
    }

    @TruffleBoundary
    public static boolean preventExtensions(DynamicObject obj, boolean doThrow) {
        return JSObject.getJSClass(obj).preventExtensions(obj, doThrow);
    }

    @TruffleBoundary
    public static boolean isExtensible(DynamicObject obj) {
        return JSObject.getJSClass(obj).isExtensible(obj);
    }

    public static boolean isExtensible(DynamicObject obj, JSClassProfile classProfile) {
        return classProfile.getJSClass(obj).isExtensible(obj);
    }

    /**
     * The property [[Class]] of the object. This is the second part of what
     * Object.prototype.toString.call(myObj) returns, e.g. "[object Array]".
     *
     * @return the internal property [[Class]] of the object.
     */
    @TruffleBoundary
    public static String getClassName(DynamicObject obj) {
        return JSObject.getJSClass(obj).getClassName(obj);
    }

    @TruffleBoundary
    public static boolean isFrozen(DynamicObject obj) {
        return testIntegrityLevel(obj, true);
    }

    @TruffleBoundary
    public static boolean isSealed(DynamicObject obj) {
        return testIntegrityLevel(obj, false);
    }

    public static ScriptArray getArray(DynamicObject obj) {
        assert hasArray(obj);
        if (obj instanceof JSArrayBase) {
            return ((JSArrayBase) obj).getArrayType();
        } else {
            return ((JSTypedArrayObject) obj).getArrayType();
        }
        // return ((JSArrayLike) obj).getArrayType();
    }

    public static void setArray(DynamicObject obj, ScriptArray array) {
        assert hasArray(obj);
        ((JSArrayBase) obj).setArrayType(array);
    }

    public static boolean hasArray(DynamicObject obj) {
        return JSArray.isJSArray(obj) || JSArgumentsObject.isJSArgumentsObject(obj) || JSArrayBufferView.isJSArrayBufferView(obj) || JSObjectPrototype.isJSObjectPrototype(obj);
    }

    public static JSContext getJSContext(DynamicObject obj) {
        return JSShape.getJSContext(obj.getShape());
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public static boolean testIntegrityLevel(DynamicObject obj, boolean frozen) {
        return JSObject.getJSClass(obj).testIntegrityLevel(obj, frozen);
    }

    @TruffleBoundary
    public static boolean setIntegrityLevel(DynamicObject obj, boolean freeze) {
        return setIntegrityLevel(obj, freeze, false);
    }

    @TruffleBoundary
    public static boolean setIntegrityLevel(DynamicObject obj, boolean freeze, boolean doThrow) {
        return JSObject.getJSClass(obj).setIntegrityLevel(obj, freeze, doThrow);
    }

}
