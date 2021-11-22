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
package com.oracle.truffle.js.runtime.objects;

import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.utilities.TriState;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.builtins.JSClass;

/**
 * The common base class for all JavaScript objects as well as {@code null} and {@code undefined}.
 */
@ExportLibrary(InteropLibrary.class)
public abstract class JSDynamicObject extends DynamicObject implements TruffleObject {

    protected JSDynamicObject(Shape shape) {
        super(shape);
    }

    @ExportMessage
    public static final class IsIdenticalOrUndefined {
        @Specialization
        public static TriState doHostObject(JSDynamicObject receiver, JSDynamicObject other) {
            return TriState.valueOf(receiver == other);
        }

        @SuppressWarnings("unused")
        @Fallback
        public static TriState doOther(JSDynamicObject receiver, Object other) {
            return TriState.UNDEFINED;
        }
    }

    @ExportMessage
    @TruffleBoundary
    public final int identityHashCode() {
        return super.hashCode();
    }

    public final JSContext getJSContext() {
        return getJSSharedData(this).getContext();
    }

    public JSClass getJSClass() {
        return (JSClass) getDynamicType(this);
    }

    /**
     * 9.1.1 [[GetPrototypeOf]] ().
     */
    @TruffleBoundary
    public abstract JSDynamicObject getPrototypeOf();

    /**
     * 9.1.2 [[SetPrototypeOf]] (V).
     */
    @TruffleBoundary
    public abstract boolean setPrototypeOf(JSDynamicObject newPrototype);

    /**
     * 9.1.3 [[IsExtensible]] ().
     */
    @TruffleBoundary
    public abstract boolean isExtensible();

    /**
     * 9.1.4 [[PreventExtensions]] ().
     */
    @TruffleBoundary
    public abstract boolean preventExtensions(boolean doThrow);

    /**
     * 9.1.5 [[GetOwnProperty]] (P).
     */
    @TruffleBoundary
    public abstract PropertyDescriptor getOwnProperty(Object propertyKey);

    /**
     * 9.1.6 [[DefineOwnProperty]] (P, Desc).
     */
    @TruffleBoundary
    public abstract boolean defineOwnProperty(Object key, PropertyDescriptor value, boolean doThrow);

    /**
     * 9.1.7 [[HasProperty]] (P).
     */
    @TruffleBoundary
    public abstract boolean hasProperty(Object key);

    @TruffleBoundary
    public abstract boolean hasProperty(long index);

    @TruffleBoundary
    public abstract boolean hasOwnProperty(Object propName);

    @TruffleBoundary
    public abstract boolean hasOwnProperty(long index);

    /**
     * 9.1.8 [[Get]] (P, Receiver).
     *
     * TODO: rename to {@code get} once {@link DynamicObject#get(Object)} is removed.
     */
    @SuppressWarnings("javadoc")
    public Object getValue(Object key) {
        return JSRuntime.nullToUndefined(getHelper(this, key, null));
    }

    public Object getValue(long index) {
        return JSRuntime.nullToUndefined(getHelper(this, index, null));
    }

    @TruffleBoundary
    public abstract Object getHelper(Object receiver, Object key, Node encapsulatingNode);

    @TruffleBoundary
    public abstract Object getHelper(Object receiver, long index, Node encapsulatingNode);

    @TruffleBoundary
    public abstract Object getOwnHelper(Object receiver, Object key, Node encapsulatingNode);

    @TruffleBoundary
    public abstract Object getOwnHelper(Object receiver, long index, Node encapsulatingNode);

    @TruffleBoundary
    public abstract Object getMethodHelper(Object receiver, Object key, Node encapsulatingNode);

    /**
     * 9.1.9 [[Set]] (P, V, Receiver).
     */
    @TruffleBoundary
    public abstract boolean set(Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode);

    @TruffleBoundary
    public abstract boolean set(long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode);

    /**
     * 9.1.10 [[Delete]] (P).
     */
    @TruffleBoundary
    public abstract boolean delete(Object key, boolean isStrict);

    @TruffleBoundary
    public abstract boolean delete(long propIdx, boolean isStrict);

    /**
     * 9.1.12 [[OwnPropertyKeys]]().
     *
     * Provides all <em>own</em> properties of this object with a <em>String</em> or <em>Symbol</em>
     * key. Represents the [[OwnPropertyKeys]] internal method.
     *
     * @return a List of the keys of all own properties of that object
     */
    @TruffleBoundary
    public List<Object> ownPropertyKeys() {
        return getOwnPropertyKeys(true, true);
    }

    /**
     * GetOwnPropertyKeys (O, type).
     *
     * @return a List of the keys of all own properties of that object with the specified types
     */
    @TruffleBoundary
    public abstract List<Object> getOwnPropertyKeys(boolean strings, boolean symbols);

    /**
     * If true, {@link #ownPropertyKeys} and {@link JSShape#getProperties} enumerate the same keys.
     */
    @TruffleBoundary
    public abstract boolean hasOnlyShapeProperties();

    /**
     * The [[Class]] internal property.
     *
     * For ES5, this is the second part of what Object.prototype.toString.call(myObj) returns, e.g.
     * "[object Array]".
     */
    @TruffleBoundary
    public abstract String getClassName();

    @Override
    @TruffleBoundary
    public abstract String toString();

    boolean isObject() {
        return true;
    }

    /**
     * Follows 19.1.3.6 Object.prototype.toString(), basically: "[object " + [[Symbol.toStringTag]]
     * + "]" or typically "[object Object]" (for non built-in types) if [[Symbol.toStringTag]] is
     * not present.
     * <p>
     * For ES5, if follows 15.2.4.2 Object.prototype.toString(), basically: "[object " + [[Class]] +
     * "]".
     *
     * @see #getBuiltinToStringTag()
     */
    @TruffleBoundary
    public String defaultToString() {
        JSContext context = getJSContext();
        if (context.getEcmaScriptVersion() <= 5) {
            return JSObjectUtil.formatToString(getClassName());
        }
        String result = null;
        if (isObject()) {
            Object toStringTag = getValue(Symbol.SYMBOL_TO_STRING_TAG);
            if (JSRuntime.isString(toStringTag)) {
                result = Boundaries.javaToString(toStringTag);
            }
        }
        if (result == null) {
            result = getBuiltinToStringTag();
        }
        return JSObjectUtil.formatToString(result);
    }

    /**
     * Returns builtinTag from step 14 of ES6+ 19.1.3.6. By default returns "Object".
     *
     * @return "Object" by default
     * @see #defaultToString()
     */
    @TruffleBoundary
    public String getBuiltinToStringTag() {
        return getClassName();
    }

    /**
     * A more informative toString variant, mainly used for error messages.
     *
     * @param format formatting parameters
     * @param depth current nesting depth
     */
    @TruffleBoundary
    public abstract String toDisplayStringImpl(boolean allowSideEffects, ToDisplayStringFormat format, int depth);

    /**
     * ES2015 7.3.15 TestIntegrityLevel(O, level).
     */
    @TruffleBoundary
    public boolean testIntegrityLevel(boolean frozen) {
        assert isObject();
        boolean status = isExtensible();
        if (status) {
            return false;
        }
        for (Object key : ownPropertyKeys()) {
            PropertyDescriptor desc = getOwnProperty(key);
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
     * ES2015 7.3.14 SetIntegrityLevel(O, level).
     */
    @TruffleBoundary
    public boolean setIntegrityLevel(boolean freeze, boolean doThrow) {
        assert isObject();
        if (!preventExtensions(doThrow)) {
            return false;
        }
        Iterable<Object> keys = ownPropertyKeys();
        if (freeze) {
            // FREEZE
            PropertyDescriptor accDesc = PropertyDescriptor.createEmpty();
            accDesc.setConfigurable(false);
            PropertyDescriptor dataDesc = PropertyDescriptor.createEmpty();
            dataDesc.setConfigurable(false);
            dataDesc.setWritable(false);

            for (Object key : keys) {
                PropertyDescriptor currentDesc = getOwnProperty(key);
                if (currentDesc != null) {
                    PropertyDescriptor newDesc = null;
                    if (currentDesc.isAccessorDescriptor()) {
                        newDesc = accDesc;
                    } else {
                        newDesc = dataDesc;
                    }
                    defineOwnProperty(key, newDesc, true);
                }
            }
        } else {
            // SEAL
            PropertyDescriptor desc = PropertyDescriptor.createEmpty();
            desc.setConfigurable(false);
            for (Object key : keys) {
                defineOwnProperty(key, desc, true);
            }
        }
        return true;
    }

    // -- static --

    /**
     * Returns whether object is a DynamicObject of JavaScript.
     */
    public static boolean isJSDynamicObject(Object object) {
        return object instanceof JSDynamicObject;
    }

    public static JSContext getJSContext(DynamicObject obj) {
        return getJSSharedData(obj).getContext();
    }

    public static JSClass getJSClass(DynamicObject obj) {
        return (JSClass) getDynamicType(obj);
    }

    public static void setJSClass(DynamicObject obj, JSClass jsclass) {
        DynamicObjectLibrary.getUncached().setDynamicType(obj, jsclass);
    }

    public static Object getDynamicType(DynamicObject obj) {
        return obj.getShape().getDynamicType();
    }

    public static boolean hasProperty(DynamicObject obj, Object key) {
        return DynamicObjectLibrary.getUncached().containsKey(obj, key);
    }

    public static Property getProperty(DynamicObject obj, Object key) {
        return DynamicObjectLibrary.getUncached().getProperty(obj, key);
    }

    public static Object[] getKeyArray(DynamicObject obj) {
        return obj.getShape().getKeyList().toArray();
    }

    public static Property[] getPropertyArray(DynamicObject obj) {
        return obj.getShape().getPropertyList().toArray(new Property[0]);
    }

    public static Object getOrNull(DynamicObject obj, Object key) {
        return DynamicObjectLibrary.getUncached().getOrDefault(obj, key, null);
    }

    public static Object getOrDefault(DynamicObject obj, Object key, Object defaultValue) {
        return DynamicObjectLibrary.getUncached().getOrDefault(obj, key, defaultValue);
    }

    public static int getIntOrDefault(DynamicObject obj, Object key, int defaultValue) {
        try {
            return DynamicObjectLibrary.getUncached().getIntOrDefault(obj, key, defaultValue);
        } catch (UnexpectedResultException e) {
            throw Errors.shouldNotReachHere();
        }
    }

    public static int getObjectFlags(DynamicObject obj) {
        return obj.getShape().getFlags();
    }

    public static void setObjectFlags(DynamicObject obj, int flags) {
        DynamicObjectLibrary.getUncached().setShapeFlags(obj, flags);
    }

    public static void setPropertyFlags(DynamicObject obj, Object key, int flags) {
        DynamicObjectLibrary.getUncached().setPropertyFlags(obj, key, flags);
    }

    public static int getPropertyFlags(DynamicObject obj, Object key) {
        return DynamicObjectLibrary.getUncached().getProperty(obj, key).getFlags();
    }

    /**
     * Update property flags, changing the object's shape if need be.
     *
     * @param updateFunction An idempotent function that returns the updated property flags based on
     *            the previous flags.
     * @return {@code true} if successful, {@code false} if there was no such property or no change
     *         was made.
     * @see #setPropertyFlags(DynamicObject, Object, int)
     * @see #getPropertyFlags(DynamicObject, Object)
     */
    public static boolean updatePropertyFlags(DynamicObject obj, Object key, IntUnaryOperator updateFunction) {
        DynamicObjectLibrary uncached = DynamicObjectLibrary.getUncached();
        Property property = uncached.getProperty(obj, key);
        if (property == null) {
            return false;
        }
        int oldFlags = property.getFlags();
        int newFlags = updateFunction.applyAsInt(oldFlags);
        if (oldFlags == newFlags) {
            return false;
        }
        return uncached.setPropertyFlags(obj, key, newFlags);
    }

    public static boolean testProperties(DynamicObject obj, Predicate<Property> predicate) {
        return obj.getShape().allPropertiesMatch(predicate);
    }

    public static boolean removeKey(DynamicObject obj, Object key) {
        return DynamicObjectLibrary.getUncached().removeKey(obj, key);
    }

    public static JSSharedData getJSSharedData(DynamicObject obj) {
        return JSShape.getSharedData(obj.getShape());
    }
}
