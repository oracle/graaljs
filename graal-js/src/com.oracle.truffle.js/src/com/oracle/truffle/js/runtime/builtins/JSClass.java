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
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

/**
 * Basic interface for all JavaScript (ordinary and exotic) object types. A JSClass defines the
 * internal and access methods of a JSObject and allows for overriding their behavior for different
 * types of objects. It can be obtained from an object via {@link Shape#getDynamicType()}.
 *
 * The interface is inspired by ECMAScript internal methods.
 *
 * Parameter order: JSDynamicObject receiver[, the rest...].
 */
public abstract class JSClass extends ObjectType {
    protected static final String[] EMPTY_STRING_ARRAY = new String[0];

    protected JSClass() {
    }

    /**
     * 9.1.1 [[GetPrototypeOf]] ().
     */
    @TruffleBoundary
    public abstract DynamicObject getPrototypeOf(DynamicObject thisObj);

    /**
     * 9.1.2 [[SetPrototypeOf]] (V).
     */
    @TruffleBoundary
    public abstract boolean setPrototypeOf(DynamicObject thisObj, DynamicObject newPrototype);

    /**
     * 9.1.3 [[IsExtensible]] ().
     */
    @TruffleBoundary
    public abstract boolean isExtensible(DynamicObject thisObj);

    /**
     * 9.1.4 [[PreventExtensions]] ().
     */
    @TruffleBoundary
    public abstract boolean preventExtensions(DynamicObject thisObj, boolean doThrow);

    /**
     * 9.1.5 [[GetOwnProperty]] (P).
     */
    @TruffleBoundary
    public abstract PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key);

    /**
     * 9.1.6 [[DefineOwnProperty]] (P, Desc).
     */
    @TruffleBoundary
    public abstract boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor value, boolean doThrow);

    /**
     * 9.1.7 [[HasProperty]] (P).
     */
    @TruffleBoundary
    public abstract boolean hasProperty(DynamicObject thisObj, Object key);

    @TruffleBoundary
    public abstract boolean hasProperty(DynamicObject thisObj, long index);

    @TruffleBoundary
    public abstract boolean hasOwnProperty(DynamicObject thisObj, Object key);

    @TruffleBoundary
    public abstract boolean hasOwnProperty(DynamicObject thisObj, long index);

    /**
     * 9.1.8 [[Get]] (P, Receiver).
     */
    public final Object get(DynamicObject thisObj, Object key) {
        return JSRuntime.nullToUndefined(getHelper(thisObj, thisObj, key, null));
    }

    public Object get(DynamicObject thisObj, long index) {
        return JSRuntime.nullToUndefined(getHelper(thisObj, thisObj, index, null));
    }

    @TruffleBoundary
    public abstract Object getHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode);

    @TruffleBoundary
    public abstract Object getHelper(DynamicObject store, Object thisObj, long index, Node encapsulatingNode);

    @TruffleBoundary
    public abstract Object getOwnHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode);

    @TruffleBoundary
    public abstract Object getOwnHelper(DynamicObject store, Object thisObj, long index, Node encapsulatingNode);

    @TruffleBoundary
    public abstract Object getMethodHelper(DynamicObject store, Object thisObj, Object key, Node encapsulatingNode);

    /**
     * 9.1.9 [[Set]] (P, V, Receiver).
     */
    @TruffleBoundary
    public abstract boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode);

    @TruffleBoundary
    public abstract boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode);

    /**
     * 9.1.10 [[Delete]] (P).
     */
    @TruffleBoundary
    public abstract boolean delete(DynamicObject thisObj, Object key, boolean isStrict);

    @TruffleBoundary
    public abstract boolean delete(DynamicObject thisObj, long index, boolean isStrict);

    /**
     * 9.1.12 [[OwnPropertyKeys]]().
     *
     * Provides all <em>own</em> properties of this object with a <em>String</em> or <em>Symbol</em>
     * key. Represents the [[OwnPropertyKeys]] internal method.
     *
     * @return a List of the keys of all own properties of that object
     */
    public final List<Object> ownPropertyKeys(DynamicObject obj) {
        return getOwnPropertyKeys(obj, true, true);
    }

    /**
     * GetOwnPropertyKeys (O, type).
     *
     * @return a List of the keys of all own properties of that object with the specified types
     */
    @TruffleBoundary
    public abstract List<Object> getOwnPropertyKeys(DynamicObject obj, boolean strings, boolean symbols);

    @TruffleBoundary
    public static List<Object> filterOwnPropertyKeys(List<Object> ownPropertyKeys, boolean strings, boolean symbols) {
        if (strings && symbols) {
            return ownPropertyKeys;
        }
        List<Object> names = new ArrayList<>();
        for (Object key : ownPropertyKeys) {
            if ((!symbols && key instanceof Symbol) || (!strings && key instanceof String)) {
                continue;
            }
            names.add(key);
        }
        return names;
    }

    /**
     * If true, {@link #ownPropertyKeys} and {@link JSShape#getProperties} enumerate the same keys.
     */
    @TruffleBoundary
    public abstract boolean hasOnlyShapeProperties(DynamicObject obj);

    /**
     * The [[Class]] internal property.
     *
     * For ES5, this is the second part of what Object.prototype.toString.call(myObj) returns, e.g.
     * "[object Array]".
     *
     * @param object object to be used
     */
    @TruffleBoundary
    public abstract String getClassName(DynamicObject object);

    @Override
    @TruffleBoundary
    public abstract String toString();

    /**
     * Follows 19.1.3.6 Object.prototype.toString(), basically: "[object " + [[Symbol.toStringTag]]
     * + "]" or typically "[object Object]" (for non built-in types) if [[Symbol.toStringTag]] is
     * not present.
     * <p>
     * For ES5, if follows 15.2.4.2 Object.prototype.toString(), basically: "[object " + [[Class]] +
     * "]".
     *
     * @see #getBuiltinToStringTag(DynamicObject)
     */
    @TruffleBoundary
    public String defaultToString(DynamicObject object) {
        JSContext context = JSObject.getJSContext(object);
        if (context.getEcmaScriptVersion() <= 5) {
            return formatToString(getClassName(object));
        }
        String result = getToStringTag(object);
        return formatToString(result);
    }

    protected String getToStringTag(DynamicObject object) {
        String result = getBuiltinToStringTag(object);
        if (JSRuntime.isObject(object)) {
            Object toStringTag = JSObject.get(object, Symbol.SYMBOL_TO_STRING_TAG);
            if (JSRuntime.isString(toStringTag)) {
                result = JSRuntime.toStringIsString(toStringTag);
            }
        }
        return result;
    }

    /**
     * Returns builtinTag from step 14 of ES6+ 19.1.3.6. By default returns "Object".
     *
     * @param object object to be used
     * @return "Object" by default
     * @see #defaultToString(DynamicObject)
     */
    @TruffleBoundary
    public String getBuiltinToStringTag(DynamicObject object) {
        return getClassName(object);
    }

    /**
     * Formats {@link #defaultToString(DynamicObject)}, by default returns "[object ...]".
     *
     * @param object object to be used
     * @return "[object ...]" by default
     */
    @TruffleBoundary
    protected String formatToString(String object) {
        return "[object " + object + "]";
    }

    /**
     * A more informative toString variant, mainly used for error messages.
     *
     * @param format formatting parameters
     * @param depth current nesting depth
     */
    @TruffleBoundary
    public abstract String toDisplayStringImpl(DynamicObject object, boolean allowSideEffects, ToDisplayStringFormat format, int depth);

    public final boolean isInstance(DynamicObject object) {
        return isInstance(object, this);
    }

    public final boolean isInstance(Object object) {
        return isInstance(object, this);
    }

    public static boolean isInstance(Object object, JSClass jsclass) {
        return JSDynamicObject.isJSDynamicObject(object) && isInstance((DynamicObject) object, jsclass);
    }

    public static boolean isInstance(DynamicObject object, JSClass jsclass) {
        return object.getShape().getDynamicType() == jsclass;
    }

    /**
     * ES2015 7.3.15 TestIntegrityLevel(O, level).
     */
    @TruffleBoundary
    public boolean testIntegrityLevel(DynamicObject obj, boolean frozen) {
        return testIntegrityLevelDefault(obj, frozen);
    }

    @TruffleBoundary
    protected final boolean testIntegrityLevelDefault(DynamicObject obj, boolean frozen) {
        assert JSRuntime.isObject(obj);
        boolean status = isExtensible(obj);
        if (status) {
            return false;
        }
        for (Object key : JSObject.ownPropertyKeys(obj)) {
            PropertyDescriptor desc = JSObject.getOwnProperty(obj, key);
            if (desc != null) {
                if (desc.getConfigurable()) {
                    return false;
                }
                if (frozen && desc.isDataDescriptor() && desc.getWritable()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This class is used to break the class initialization cycle JSClass -> PropertyDescriptor ->
     * Undefined -> Null -> NullClass -> AbstractJSClass -> JSClass.
     */
    private static final class FreezeHolder {
        private static final PropertyDescriptor FREEZE_ACC_DESC;
        private static final PropertyDescriptor FREEZE_DATA_DESC;

        static {
            FREEZE_ACC_DESC = PropertyDescriptor.createEmpty();
            FREEZE_ACC_DESC.setConfigurable(false);

            FREEZE_DATA_DESC = PropertyDescriptor.createEmpty();
            FREEZE_DATA_DESC.setConfigurable(false);
            FREEZE_DATA_DESC.setWritable(false);
        }
    }

    /**
     * ES2015 7.3.14 SetIntegrityLevel(O, level).
     */
    @TruffleBoundary
    public boolean setIntegrityLevel(DynamicObject obj, boolean freeze, boolean doThrow) {
        return setIntegrityLevelDefault(obj, freeze, doThrow);
    }

    @TruffleBoundary
    private boolean setIntegrityLevelDefault(DynamicObject obj, boolean freeze, boolean doThrow) {
        assert JSRuntime.isObject(obj);
        if (!preventExtensions(obj, doThrow)) {
            return false;
        }
        Iterable<Object> keys = JSObject.ownPropertyKeys(obj);
        if (freeze) {
            // FREEZE
            for (Object key : keys) {
                PropertyDescriptor currentDesc = JSObject.getOwnProperty(obj, key);
                if (currentDesc != null) {
                    PropertyDescriptor newDesc = null;
                    if (currentDesc.isAccessorDescriptor()) {
                        newDesc = FreezeHolder.FREEZE_ACC_DESC;
                    } else {
                        newDesc = FreezeHolder.FREEZE_DATA_DESC;
                    }
                    JSRuntime.definePropertyOrThrow(obj, key, newDesc);
                }
            }
        } else {
            // SEAL
            for (Object key : keys) {
                JSRuntime.definePropertyOrThrow(obj, key, FreezeHolder.FREEZE_ACC_DESC);
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        throw Errors.shouldNotReachHere(getClass().getName());
    }

    /**
     * Allows invokevirtual of this method instead of invokeinterface. Classes overriding this
     * method should implement {@link PrototypeSupplier}.
     *
     * @see PrototypeSupplier#getIntrinsicDefaultProto
     */
    public DynamicObject getIntrinsicDefaultProto(@SuppressWarnings("unused") JSRealm realm) {
        throw Errors.shouldNotReachHere(getClass().getName());
    }

    public abstract boolean usesOrdinaryGetOwnProperty();

    public abstract boolean usesOrdinaryIsExtensible();

}
