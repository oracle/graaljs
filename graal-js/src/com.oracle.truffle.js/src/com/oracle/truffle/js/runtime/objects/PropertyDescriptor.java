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

import java.util.StringJoiner;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;

/**
 * Objects of this type are used by the defineProperty() and defineProperties() builtin functions.
 * Reason to use this class and not a JSObject directly is to avoid double evaluation.
 *
 */
public final class PropertyDescriptor {
    private Object data; // either a value (data descriptor) or an instance of Pair (Accessor)
    private int flags;

    private static final int ENUMERABLE = 1 << 0;
    private static final int WRITABLE = 1 << 1;
    private static final int CONFIGURABLE = 1 << 2;

    private static final int HAS_VALUE = 1 << 3;
    private static final int HAS_GET = 1 << 4;
    private static final int HAS_SET = 1 << 5;
    private static final int HAS_ENUMERABLE = 1 << 6;
    private static final int HAS_WRITABLE = 1 << 7;
    private static final int HAS_CONFIGURABLE = 1 << 8;

    public static final PropertyDescriptor undefinedDataDesc = PropertyDescriptor.createDataDefault(Undefined.instance);
    public static final PropertyDescriptor undefinedDataDescNotConfigurable = PropertyDescriptor.createData(Undefined.instance, true, true, false);

    private PropertyDescriptor() {
    }

    public static PropertyDescriptor createEmpty() {
        return new PropertyDescriptor();
    }

    public static PropertyDescriptor createData(Object value, boolean isEnumerable, boolean isWritable, boolean isConfigurable) {
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setValue(value);
        desc.setEnumerable(isEnumerable);
        desc.setWritable(isWritable);
        desc.setConfigurable(isConfigurable);
        return desc;
    }

    public static PropertyDescriptor createData(Object value, int attributes) {
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setValue(value);
        desc.setEnumerable(JSAttributes.isEnumerable(attributes));
        desc.setWritable(JSAttributes.isWritable(attributes));
        desc.setConfigurable(JSAttributes.isConfigurable(attributes));
        return desc;
    }

    public static PropertyDescriptor createData(Object value) {
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setValue(value);
        return desc;
    }

    public static PropertyDescriptor createDataDefault(Object value) {
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setValue(value);
        desc.flags = ENUMERABLE | WRITABLE | CONFIGURABLE | HAS_VALUE | HAS_ENUMERABLE | HAS_WRITABLE | HAS_CONFIGURABLE;
        return desc;
    }

    public static PropertyDescriptor createAccessor(DynamicObject getter, DynamicObject setter) {
        PropertyDescriptor desc = new PropertyDescriptor();
        if (setter != null) {
            desc.setSet(setter);
        }
        if (getter != null) {
            desc.setGet(getter);
        }
        return desc;
    }

    public static PropertyDescriptor createAccessor(DynamicObject getter, DynamicObject setter, int attributes) {
        PropertyDescriptor desc = createAccessor(getter, setter);
        desc.setEnumerable(JSAttributes.isEnumerable(attributes));
        desc.setConfigurable(JSAttributes.isConfigurable(attributes));
        return desc;
    }

    public static PropertyDescriptor createAccessor(DynamicObject getter, DynamicObject setter, boolean isEnumerable, boolean isConfigurable) {
        PropertyDescriptor desc = createAccessor(getter, setter);
        desc.setEnumerable(isEnumerable);
        desc.setConfigurable(isConfigurable);
        return desc;
    }

    public Object getValue() {
        if (data instanceof Accessor) {
            return null;
        }
        return data;
    }

    public void setValue(Object value) {
        this.data = value;
        this.flags |= HAS_VALUE;
    }

    public Object getGet() {
        if (!(data instanceof Accessor)) {
            return null;
        }
        return ((Accessor) data).getGetter();
    }

    public void setGet(DynamicObject get) {
        if (data instanceof Accessor) {
            data = new Accessor(get, ((Accessor) data).getSetter());
        } else {
            data = new Accessor(get, null);
        }
        this.flags |= HAS_GET;
    }

    public Object getSet() {
        if (!(data instanceof Accessor)) {
            return null;
        }
        return ((Accessor) data).getSetter();
    }

    public void setSet(DynamicObject set) {
        if (data instanceof Accessor) {
            data = new Accessor(((Accessor) data).getGetter(), set);
        } else {
            data = new Accessor(null, set);
        }
        this.flags |= HAS_SET;
    }

    public boolean getEnumerable() {
        return (this.flags & ENUMERABLE) != 0;
    }

    public boolean getIfHasEnumerable(boolean defaultValue) {
        if (hasEnumerable()) {
            return getEnumerable();
        }
        return defaultValue;
    }

    public void setEnumerable(boolean enumerable) {
        if (enumerable) {
            this.flags |= ENUMERABLE;
        } else {
            this.flags &= ~ENUMERABLE;
        }
        this.flags |= HAS_ENUMERABLE;
    }

    public boolean getWritable() {
        return (this.flags & WRITABLE) != 0;
    }

    public boolean getIfHasWritable(boolean defaultValue) {
        if (hasWritable()) {
            return getWritable();
        }
        return defaultValue;
    }

    public void setWritable(boolean writable) {
        if (writable) {
            this.flags |= WRITABLE;
        } else {
            this.flags &= ~WRITABLE;
        }
        this.flags |= HAS_WRITABLE;
    }

    public boolean getConfigurable() {
        return (this.flags & CONFIGURABLE) != 0;
    }

    public boolean getIfHasConfigurable(boolean defaultValue) {
        if (hasConfigurable()) {
            return getConfigurable();
        }
        return defaultValue;
    }

    public void setConfigurable(boolean configurable) {
        if (configurable) {
            this.flags |= CONFIGURABLE;
        } else {
            this.flags &= ~CONFIGURABLE;
        }
        this.flags |= HAS_CONFIGURABLE;
    }

    public boolean hasSet() {
        return (this.flags & HAS_SET) != 0;
    }

    public boolean hasGet() {
        return (this.flags & HAS_GET) != 0;
    }

    public boolean hasValue() {
        return (this.flags & HAS_VALUE) != 0;
    }

    public boolean hasEnumerable() {
        return (this.flags & HAS_ENUMERABLE) != 0;
    }

    public boolean hasWritable() {
        return (this.flags & HAS_WRITABLE) != 0;
    }

    public boolean hasConfigurable() {
        return (this.flags & HAS_CONFIGURABLE) != 0;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "PropertyDescriptor[", "]");
        String kvsep = ": ";
        if (hasEnumerable()) {
            joiner.add(JSAttributes.ENUMERABLE + kvsep + getEnumerable());
        }
        if (hasConfigurable()) {
            joiner.add(JSAttributes.CONFIGURABLE + kvsep + getConfigurable());
        }
        if (hasWritable()) {
            joiner.add(JSAttributes.WRITABLE + kvsep + getWritable());
        }
        if (hasValue()) {
            assert !(data instanceof Accessor);
            joiner.add(JSAttributes.VALUE + kvsep + data);
        }
        if (hasGet()) {
            assert (data instanceof Accessor);
            joiner.add(JSAttributes.GET + kvsep + ((Accessor) data).getGetter());
        }
        if (hasSet()) {
            assert (data instanceof Accessor);
            joiner.add(JSAttributes.SET + kvsep + ((Accessor) data).getSetter());
        }
        return joiner.toString();
    }

    /**
     * 8.10.1 IsAccessorDescriptor ( Desc ).
     */
    public boolean isAccessorDescriptor() {
        return (hasGet() || hasSet());
    }

    /**
     * 8.10.2 IsDataDescriptor ( Desc ).
     */
    public boolean isDataDescriptor() {
        return (hasValue() || hasWritable());
    }

    /**
     * Implementing 8.10.3 IsGenericDescriptor.
     */
    public boolean isGenericDescriptor() {
        return !(isAccessorDescriptor() || isDataDescriptor());
    }

    public int getFlags() {
        return JSAttributes.fromConfigurableEnumerableWritable(getIfHasConfigurable(false), getIfHasEnumerable(false), getIfHasWritable(false));
    }
}
