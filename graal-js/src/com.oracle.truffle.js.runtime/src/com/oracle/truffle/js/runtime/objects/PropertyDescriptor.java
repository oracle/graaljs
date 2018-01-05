/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

    public static PropertyDescriptor createAccessor(DynamicObject setter, DynamicObject getter) {
        PropertyDescriptor desc = new PropertyDescriptor();
        if (setter != null) {
            desc.setSet(setter);
        }
        if (getter != null) {
            desc.setGet(getter);
        }
        return desc;
    }

    public static PropertyDescriptor createAccessor(DynamicObject setter, DynamicObject getter, boolean isEnumerable, boolean isWritable, boolean isConfigurable) {
        PropertyDescriptor desc = new PropertyDescriptor();
        desc.setSet(setter);
        desc.setGet(getter);
        desc.setEnumerable(isEnumerable);
        desc.setWritable(isWritable);
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
