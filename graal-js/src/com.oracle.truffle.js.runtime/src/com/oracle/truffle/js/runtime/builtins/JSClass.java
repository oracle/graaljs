/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

/**
 * Basic interface for all JavaScript "classes". A JSClass defines the internal and access methods
 * of a JSObject and allows for overriding their behavior for different types of objects.
 *
 * See also ECMA 8.6.2 "Object Internal Properties and Methods" for a list of internal properties
 * and methods.
 *
 * <pre>
 * Implementation notes:
 * - keep parameter order consistent: JSObject receiver[, the rest...].
 * - keep interface clean, avoid redundant methods, maximize consistency with JSObject and ECMAScript
 * </pre>
 */
public abstract class JSClass extends ObjectType {

    protected JSClass() {
    }

    /**
     * 9.1.1 [[GetPrototypeOf]] ().
     */
    public abstract DynamicObject getPrototypeOf(DynamicObject thisObj);

    /**
     * 9.1.2 [[SetPrototypeOf]] (V).
     */
    public abstract boolean setPrototypeOf(DynamicObject thisObj, DynamicObject newPrototype);

    /**
     * 9.1.3 [[IsExtensible]] ().
     */
    public abstract boolean isExtensible(DynamicObject thisObj);

    /**
     * 9.1.4 [[PreventExtensions]] ().
     */
    public abstract boolean preventExtensions(DynamicObject thisObj);

    /**
     * 9.1.5 [[GetOwnProperty]] (P).
     */
    public abstract PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object propertyKey);

    /**
     * 9.1.6 [[DefineOwnProperty]] (P, Desc).
     */
    public abstract boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor value, boolean doThrow);

    /**
     * 9.1.7 [[HasProperty]] (P).
     */
    public abstract boolean hasProperty(DynamicObject thisObj, Object key);

    public abstract boolean hasProperty(DynamicObject thisObj, long index);

    public abstract boolean hasOwnProperty(DynamicObject thisObj, Object propName);

    public abstract boolean hasOwnProperty(DynamicObject thisObj, long index);

    /**
     * 9.1.8 [[Get]] (P, Receiver).
     */
    public final Object get(DynamicObject thisObj, Object key) {
        return JSRuntime.nullToUndefined(getHelper(thisObj, thisObj, key));
    }

    public Object get(DynamicObject thisObj, long index) {
        return JSRuntime.nullToUndefined(getHelper(thisObj, thisObj, index));
    }

    public abstract Object getHelper(DynamicObject store, Object thisObj, Object key);

    public abstract Object getHelper(DynamicObject store, Object thisObj, long index);

    public abstract Object getOwnHelper(DynamicObject store, Object thisObj, Object key);

    public abstract Object getOwnHelper(DynamicObject store, Object thisObj, long index);

    public abstract Object getMethodHelper(DynamicObject store, Object thisObj, Object key);

    /**
     * 9.1.9 [[Set]] (P, V, Receiver).
     */
    public abstract boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict);

    public abstract boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict);

    public abstract boolean setOwn(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict);

    public abstract boolean setOwn(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict);

    /**
     * 9.1.10 [[Delete]] (P).
     */
    public abstract boolean delete(DynamicObject thisObj, Object key, boolean isStrict);

    public abstract boolean delete(DynamicObject thisObj, long propIdx, boolean isStrict);

    /**
     * 9.1.12 [[OwnPropertyKeys]]().
     *
     * Provides all <em>own</em> properties of this object with a <em>String</em> or <em>Symbol</em>
     * key. Represents the [[OwnPropertyKeys]] internal method.
     *
     * @return an List<Object> of the keys of all own properties of that object
     */
    public abstract Iterable<Object> ownPropertyKeys(DynamicObject obj);

    /**
     * If true, {@link #ownPropertyKeys} and {@link JSShape#getProperties} enumerate the same keys.
     */
    public abstract boolean hasOnlyShapeProperties(DynamicObject obj);

    /**
     * The [[Class]] internal property.
     *
     * For ES5, this is the second part of what Object.prototype.toString.call(myObj) returns, e.g.
     * "[object Array]".
     *
     * @param object object to be used
     */
    public abstract String getClassName(DynamicObject object);

    @Override
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
        String result = null;
        if (JSRuntime.isObject(object)) {
            Object toStringTag = JSObject.get(object, Symbol.SYMBOL_TO_STRING_TAG);
            if (JSRuntime.isString(toStringTag)) {
                result = Boundaries.javaToString(toStringTag);
            }
        }
        if (result == null) {
            result = getBuiltinToStringTag(object);
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
     * A more informative but side-effect-free toString variant, mainly used for exception messages.
     *
     * Follows Nashorn's safeToString.
     */
    public abstract String safeToString(DynamicObject object);

    public final boolean isInstance(DynamicObject object) {
        return isInstance(object, this);
    }

    public final boolean isInstance(Object object) {
        return isInstance(object, this);
    }

    public static boolean isInstance(Object object, JSClass jsclass) {
        return JSObject.isDynamicObject(object) && isInstance((DynamicObject) object, jsclass);
    }

    public static boolean isInstance(DynamicObject object, JSClass jsclass) {
        return object.getShape().getObjectType() == jsclass;
    }

    /**
     * ES2015 7.3.15 TestIntegrityLevel(O, level).
     */
    @TruffleBoundary
    public boolean testIntegrityLevel(DynamicObject obj, boolean frozen) {
        assert JSRuntime.isObject(obj);
        boolean status = isExtensible(obj);
        if (status) {
            return false;
        }
        for (Object key : ownPropertyKeys(obj)) {
            PropertyDescriptor desc = getOwnProperty(obj, key);
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

    private static final PropertyDescriptor FREEZE_ACC_DESC;
    private static final PropertyDescriptor FREEZE_DATA_DESC;

    static {
        FREEZE_ACC_DESC = PropertyDescriptor.createEmpty();
        FREEZE_ACC_DESC.setConfigurable(false);

        FREEZE_DATA_DESC = PropertyDescriptor.createEmpty();
        FREEZE_DATA_DESC.setConfigurable(false);
        FREEZE_DATA_DESC.setWritable(false);
    }

    /**
     * ES2015 7.3.14 SetIntegrityLevel(O, level).
     */
    @TruffleBoundary
    public boolean setIntegrityLevel(DynamicObject obj, boolean freeze) {
        assert JSRuntime.isObject(obj);
        if (!preventExtensions(obj)) {
            return false;
        }
        Iterable<Object> keys = ownPropertyKeys(obj);
        JSContext context = JSObject.getJSContext(obj);
        if (freeze) {
            // FREEZE
            for (Object key : keys) {
                PropertyDescriptor currentDesc = getOwnProperty(obj, key);
                if (currentDesc != null) {
                    PropertyDescriptor newDesc = null;
                    if (currentDesc.isAccessorDescriptor()) {
                        newDesc = FREEZE_ACC_DESC;
                    } else {
                        newDesc = FREEZE_DATA_DESC;
                    }
                    JSRuntime.definePropertyOrThrow(obj, key, newDesc, context);
                }
            }
        } else {
            // SEAL
            for (Object key : keys) {
                JSRuntime.definePropertyOrThrow(obj, key, FREEZE_ACC_DESC, context);
            }
        }
        return true;
    }
}
