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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.utilities.TriState;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.builtins.JSClass;

/**
 * The common base class for all JavaScript objects as well as {@code null} and {@code undefined}.
 */
@ExportLibrary(InteropLibrary.class)
public abstract sealed class JSDynamicObject extends DynamicObject implements TruffleObject permits JSObject, Nullish {

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

    public final JSClass getJSClass() {
        return (JSClass) getDynamicType(this);
    }

    /**
     * [[GetPrototypeOf]] ().
     */
    @TruffleBoundary
    public abstract JSDynamicObject getPrototypeOf();

    /**
     * [[SetPrototypeOf]] (V).
     */
    @TruffleBoundary
    public abstract boolean setPrototypeOf(JSDynamicObject newPrototype);

    /**
     * [IsExtensible]] ().
     */
    @TruffleBoundary
    public abstract boolean isExtensible();

    /**
     * [[PreventExtensions]] ().
     */
    @TruffleBoundary
    public abstract boolean preventExtensions(boolean doThrow);

    /**
     * [[GetOwnProperty]] (P).
     */
    @TruffleBoundary
    public abstract PropertyDescriptor getOwnProperty(Object propertyKey);

    /**
     * [[DefineOwnProperty]] (P, Desc).
     */
    @TruffleBoundary
    public abstract boolean defineOwnProperty(Object key, PropertyDescriptor value, boolean doThrow);

    /**
     * [[HasProperty]] (P).
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
     * [[Get]] (P, Receiver).
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
     * [[Set]] (P, V, Receiver).
     */
    @TruffleBoundary
    public abstract boolean set(Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode);

    @TruffleBoundary
    public abstract boolean set(long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode);

    /**
     * [[Delete]] (P).
     */
    @TruffleBoundary
    public abstract boolean delete(Object key, boolean isStrict);

    @TruffleBoundary
    public abstract boolean delete(long propIdx, boolean isStrict);

    /**
     * [[OwnPropertyKeys]]().
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
     * If true, {@link #ownPropertyKeys} and {@link JSShape#getPropertyKeyList} enumerate the same
     * keys.
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
    public abstract TruffleString getClassName();

    boolean isObject() {
        return true;
    }

    /**
     * Returns the equivalent of Object.prototype.toString(), i.e., for ES2015+:
     * {@code "[object " + toStringTag + "]"}, where toStringTag is either the value of the object's
     * {@code Symbol.toStringTag} property, if present and a string value, or else, the builtinTag
     * (default: "Object") according to Object.prototype.toString().
     *
     * For ES5, the [[Class]] internal property is used instead, i.e.:
     * {@code "[object " + [[Class]] + "]"}, although in some cases we still use
     * {@code Symbol.toStringTag} to override [[Class]] for Nashorn compatibility.
     *
     * @see #getBuiltinToStringTag()
     */
    @TruffleBoundary
    public TruffleString defaultToString() {
        TruffleString result = getJSContext().getEcmaScriptVersion() > 5 ? getBuiltinToStringTag() : getClassName();
        if (isObject()) {
            Object toStringTag = getValue(Symbol.SYMBOL_TO_STRING_TAG);
            if (toStringTag instanceof TruffleString) {
                result = (TruffleString) toStringTag;
            }
        }
        return JSObjectUtil.formatToString(result);
    }

    /**
     * Returns builtinTag as per Object.prototype.toString(). By default returns "Object".
     *
     * @return built-in toStringTag
     */
    @TruffleBoundary
    public abstract TruffleString getBuiltinToStringTag();

    /**
     * A more informative toString variant, mainly used for error messages.
     *
     * @param format formatting parameters
     * @param depth current nesting depth
     */
    @TruffleBoundary
    public abstract TruffleString toDisplayStringImpl(boolean allowSideEffects, ToDisplayStringFormat format, int depth);

    /**
     * TestIntegrityLevel(O, level).
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
     * SetIntegrityLevel(O, level).
     */
    @TruffleBoundary
    public boolean setIntegrityLevel(boolean freeze, boolean doThrow) {
        assert isObject();
        if (!preventExtensions(doThrow)) {
            return false;
        }

        /**
         * Extra class for lazy constants to break the following class initialization cycle:
         * JSDynamicObject -> PropertyDescriptor -> Undefined -> Nullish -> JSDynamicObject.
         */
        final class Desc {
            static final PropertyDescriptor NOT_CONFIGURABLE;
            static final PropertyDescriptor NOT_CONFIGURABLE_NOT_WRITABLE;

            static {
                NOT_CONFIGURABLE = PropertyDescriptor.createEmpty();
                NOT_CONFIGURABLE.setConfigurable(false);

                NOT_CONFIGURABLE_NOT_WRITABLE = PropertyDescriptor.createEmpty();
                NOT_CONFIGURABLE_NOT_WRITABLE.setConfigurable(false);
                NOT_CONFIGURABLE_NOT_WRITABLE.setWritable(false);
            }
        }

        Iterable<Object> keys = ownPropertyKeys();
        if (freeze) {
            // FREEZE
            for (Object key : keys) {
                PropertyDescriptor currentDesc = getOwnProperty(key);
                if (currentDesc != null) {
                    PropertyDescriptor desc;
                    if (currentDesc.isAccessorDescriptor()) {
                        desc = Desc.NOT_CONFIGURABLE;
                    } else {
                        desc = Desc.NOT_CONFIGURABLE_NOT_WRITABLE;
                    }
                    defineOwnProperty(key, desc, true);
                }
            }
        } else {
            // SEAL
            PropertyDescriptor desc = Desc.NOT_CONFIGURABLE;
            for (Object key : keys) {
                defineOwnProperty(key, desc, true);
            }
        }
        return true;
    }

    // -- static --

    /**
     * Returns whether object is a JSDynamicObject (JSObject or null/undefined).
     */
    public static boolean isJSDynamicObject(Object object) {
        return object instanceof JSDynamicObject;
    }

    public static void setJSClass(JSDynamicObject obj, JSClass jsclass) {
        DynamicObjectLibrary.getUncached().setDynamicType(obj, jsclass);
    }

    public static Object getDynamicType(JSDynamicObject obj) {
        return obj.getShape().getDynamicType();
    }

    public static boolean hasProperty(JSDynamicObject obj, Object key) {
        return Properties.containsKeyUncached(obj, key);
    }

    public static Property[] getPropertyArray(JSDynamicObject obj) {
        return obj.getShape().getPropertyList().toArray(new Property[0]);
    }

    public static Object getOrNull(JSDynamicObject obj, Object key) {
        return Properties.getOrDefaultUncached(obj, key, null);
    }

    public static Object getOrDefault(JSDynamicObject obj, Object key, Object defaultValue) {
        return Properties.getOrDefaultUncached(obj, key, defaultValue);
    }

    public static int getObjectFlags(JSDynamicObject obj) {
        return obj.getShape().getFlags();
    }

    public static void setObjectFlags(JSDynamicObject obj, int flags) {
        DynamicObjectLibrary.getUncached().setShapeFlags(obj, flags);
    }

    public static void setPropertyFlags(JSDynamicObject obj, Object key, int flags) {
        Properties.setPropertyFlagsUncached(obj, key, flags);
    }

    public static int getPropertyFlags(JSDynamicObject obj, Object key) {
        return Properties.getPropertyUncached(obj, key).getFlags();
    }

    /**
     * Update property flags, changing the object's shape if need be.
     *
     * @param updateFunction An idempotent function that returns the updated property flags based on
     *            the previous flags.
     * @return {@code true} if successful, {@code false} if there was no such property or no change
     *         was made.
     * @see #setPropertyFlags(JSDynamicObject, Object, int)
     * @see #getPropertyFlags(JSDynamicObject, Object)
     */
    public static boolean updatePropertyFlags(JSDynamicObject obj, Object key, IntUnaryOperator updateFunction) {
        DynamicObjectLibrary uncached = DynamicObjectLibrary.getUncached();
        Property property = Properties.getProperty(uncached, obj, key);
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

    public static boolean testProperties(JSDynamicObject obj, Predicate<Property> predicate) {
        return obj.getShape().allPropertiesMatch(predicate);
    }

    public static JSSharedData getJSSharedData(JSDynamicObject obj) {
        return JSShape.getSharedData(obj.getShape());
    }
}
