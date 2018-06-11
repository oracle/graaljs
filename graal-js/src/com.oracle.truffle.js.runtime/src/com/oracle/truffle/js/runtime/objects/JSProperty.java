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
package com.oracle.truffle.js.runtime.objects;

import static com.oracle.truffle.js.runtime.objects.JSAttributes.NOT_CONFIGURABLE;
import static com.oracle.truffle.js.runtime.objects.JSAttributes.NOT_ENUMERABLE;
import static com.oracle.truffle.js.runtime.objects.JSAttributes.NOT_WRITABLE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

/**
 * Property objects represent the mapping between low-level stores and high-level data. The simplest
 * Property could be nothing more than a map of one index to one property's value, but abstracting
 * the interface allows for getter/setter methods, type-checked properties, and other such
 * specialized and language-specific behavior. ECMAScript[8.6.1]
 */
public class JSProperty {
    /** Is this property an accessor or data property? */
    public static final int ACCESSOR = 1 << 3;

    /** Is this property a proxy property? */
    public static final int PROXY = 1 << 4;

    public static final int CONST = 1 << 5;

    @TruffleBoundary
    public String toString(Property property) {
        return "\"" + property.getKey() + "\"" + getAttributeString(property) + ":" + property.getLocation();
    }

    private static String getAttributeString(Property property) {
        String negative = getAttributeString(property, false);
        return negative.isEmpty() ? "" : ("-" + negative);
    }

    protected static String getAttributeString(Property property, boolean positive) {
        return (isEnumerable(property) == positive ? "e" : "") + (isConfigurable(property) == positive ? "c" : "") + (isData(property) && (isWritable(property) == positive) ? "w" : "");
    }

    /**
     * Get the value assigned to this property in the given object and store.
     *
     * @param thisObj the object that this property was found in
     * @param store the store that this property's value resides in
     * @return the value assigned to this property
     */
    public static Object getValue(Property property, DynamicObject store, Object thisObj, boolean floatingCondition) {
        Object value = property.get(store, floatingCondition);
        if (isAccessor(property)) {
            return getValueAccessor(thisObj, value);
        } else if (isProxy(property)) {
            return ((PropertyProxy) value).get(store);
        } else {
            assert isData(property);
            return value;
        }
    }

    private static Object getValueAccessor(Object thisObj, Object value) {
        DynamicObject getter = ((Accessor) value).getGetter();
        if (getter != Undefined.instance) {
            return JSFunction.call(getter, thisObj, JSArguments.EMPTY_ARGUMENTS_ARRAY);
        } else {
            return Undefined.instance;
        }
    }

    /**
     * Set the value assigned to this property in the given object and store.
     *
     * @param thisObj the object that this property was found in
     * @param store the store that this property's value resides in
     * @param value the value to assign to this property
     * @param isStrict whether the set is in a strict mode function
     */
    public static void setValueThrow(Property property, DynamicObject store, Object thisObj, Object value, Shape shape, boolean isStrict) throws IncompatibleLocationException, FinalLocationException {
        if (isAccessor(property)) {
            setValueAccessor(property, store, thisObj, value, isStrict);
        } else {
            if (isWritable(property)) {
                if (isProxy(property)) {
                    boolean ret = ((PropertyProxy) property.get(store, false)).set(store, value);
                    if (!ret && isStrict) {
                        throw Errors.createTypeErrorNotWritableProperty(property.getKey(), thisObj);
                    }
                } else {
                    assert isData(property);
                    property.set(store, value, shape);
                }
            } else {
                if (isStrict) {
                    throw Errors.createTypeErrorNotWritableProperty(property.getKey(), thisObj);
                }
            }
        }
    }

    /**
     * Set the value assigned to this property in the given object and store.
     *
     * @param thisObj the object that this property was found in
     * @param store the store that this property's value resides in
     * @param value the value to assign to this property
     * @param isStrict whether the set is in a strict mode function
     */
    public static void setValue(Property property, DynamicObject store, Object thisObj, Object value, Shape shape, boolean isStrict) {
        if (isAccessor(property)) {
            setValueAccessor(property, store, thisObj, value, isStrict);
        } else {
            if (isWritable(property)) {
                if (isProxy(property)) {
                    boolean ret = ((PropertyProxy) property.get(store, false)).set(store, value);
                    if (!ret && isStrict) {
                        throw Errors.createTypeErrorNotWritableProperty(property.getKey(), thisObj);
                    }
                } else {
                    assert isData(property);
                    property.setGeneric(store, value, shape);
                }
            } else {
                if (isStrict) {
                    throw Errors.createTypeErrorNotWritableProperty(property.getKey(), thisObj);
                }
            }
        }
    }

    private static void setValueAccessor(Property property, DynamicObject store, Object thisObj, Object value, boolean isStrict) {
        DynamicObject setter = ((Accessor) property.get(store, false)).getSetter();
        if (setter != Undefined.instance) {
            JSFunction.call(setter, thisObj, new Object[]{value});
        } else if (isStrict) {
            throw Errors.createTypeErrorCannotSetAccessorProperty(property.getKey(), store);
        }
    }

    public static Property seal(Property property) {
        if (property.isHidden()) {
            return property;
        }
        if (isConfigurable(property)) {
            return property.copyWithFlags(property.getFlags() | NOT_CONFIGURABLE);
        }
        return property;
    }

    public static Property freeze(Property property) {
        if (property.isHidden()) {
            return property;
        }
        if (isAccessor(property)) {
            return seal(property);
        }
        if (isConfigurable(property) || isWritable(property)) {
            return property.copyWithFlags(property.getFlags() | NOT_CONFIGURABLE | NOT_WRITABLE);
        }
        return property;
    }

    public static boolean isConfigurable(Property property) {
        return (property.getFlags() & NOT_CONFIGURABLE) == 0;
    }

    public static boolean isEnumerable(Property property) {
        return (property.getFlags() & NOT_ENUMERABLE) == 0;
    }

    public static boolean isWritable(Property property) {
        return (property.getFlags() & NOT_WRITABLE) == 0;
    }

    public static boolean isProxy(Property property) {
        return (property.getFlags() & PROXY) != 0;
    }

    public static boolean isAccessor(Property property) {
        return (property.getFlags() & ACCESSOR) != 0;
    }

    public static boolean isData(Property property) {
        return (property.getFlags() & ACCESSOR) == 0;
    }

    public static boolean isConst(Property property) {
        return (property.getFlags() & CONST) != 0;
    }

    public static PropertyProxy getConstantProxy(Property proxyProperty) {
        assert isProxy(proxyProperty);
        return proxyProperty.getLocation().isConstant() ? (PropertyProxy) proxyProperty.get(null, false) : null;
    }
}
