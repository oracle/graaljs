/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class AbstractConstantArray extends DynamicArray {
    protected AbstractConstantArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    public final ScriptArray setElementImpl(DynamicObject object, long index, Object value, boolean strict, boolean condition) {
        if (index <= Integer.MAX_VALUE) {
            if (value instanceof Integer) {
                return createWriteableInt(object, index, (int) value, ProfileHolder.empty()).setElementImpl(object, index, value, strict, condition);
            } else if (value instanceof Double) {
                return createWriteableDouble(object, index, (double) value, ProfileHolder.empty()).setElementImpl(object, index, value, strict, condition);
            } else {
                return createWriteableObject(object, index, value, ProfileHolder.empty()).setElementImpl(object, index, value, strict, condition);
            }
        } else {
            return SparseArray.makeSparseArray(object, this).setElementImpl(object, index, value, strict, condition);
        }
    }

    @Override
    public final Object getElement(DynamicObject object, long index, boolean condition) {
        if (isInBoundsFast(object, index, condition)) {
            return getElementInBounds(object, (int) index, condition);
        } else {
            return Undefined.instance;
        }
    }

    @Override
    public final Object getElementInBounds(DynamicObject object, long index, boolean condition) {
        assert isInBoundsFast(object, index, condition);
        return getElementInBounds(object, (int) index, condition);
    }

    public abstract Object getElementInBounds(DynamicObject object, int index, boolean condition);

    @Override
    public final long length(DynamicObject object, boolean condition) {
        return lengthInt(object, condition);
    }

    @Override
    public long firstElementIndex(DynamicObject object, boolean condition) {
        return 0;
    }

    @Override
    public long lastElementIndex(DynamicObject object, boolean condition) {
        return length(object, condition) - 1;
    }

    @Override
    public long nextElementIndex(DynamicObject object, long index, boolean condition) {
        if (index >= lastElementIndex(object, condition)) {
            return JSRuntime.MAX_SAFE_INTEGER_LONG;
        }
        return index + 1;
    }

    @Override
    public long previousElementIndex(DynamicObject object, long index, boolean condition) {
        return index - 1;
    }

    /**
     * Returns true if the index can be written using inBoundsFast access mode.
     */
    @Override
    public final boolean isInBoundsFast(DynamicObject object, long index, boolean condition) {
        return firstElementIndex(object, condition) <= index && index <= lastElementIndex(object, condition);
    }

    public abstract AbstractWritableArray createWriteableDouble(DynamicObject object, long index, double value, ProfileHolder profile);

    public abstract AbstractWritableArray createWriteableInt(DynamicObject object, long index, int value, ProfileHolder profile);

    public abstract AbstractWritableArray createWriteableObject(DynamicObject object, long index, Object value, ProfileHolder profile);

    public abstract AbstractWritableArray createWriteableJSObject(DynamicObject object, long index, DynamicObject value, ProfileHolder profile);

    protected interface CreateWritableProfileAccess extends ProfileAccess {
        default boolean lengthZero(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 0, condition);
        }

        default boolean lengthBelowLimit(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 1, condition);
        }

        default boolean indexZero(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 2, condition);
        }

        default boolean indexLessThanLength(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 3, condition);
        }
    }

    protected static final CreateWritableProfileAccess CREATE_WRITABLE_PROFILE = new CreateWritableProfileAccess() {
    };

    public static ProfileHolder createCreateWritableProfile() {
        return ProfileHolder.create(4, CreateWritableProfileAccess.class);
    }

    @Override
    public boolean hasHoles(DynamicObject object, boolean condition) {
        return false;
    }
}
