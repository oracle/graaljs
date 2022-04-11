/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.array.dyn;

import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class AbstractConstantArray extends DynamicArray {
    protected AbstractConstantArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    public final ScriptArray setElementImpl(JSDynamicObject object, long index, Object value, boolean strict) {
        if (index <= Integer.MAX_VALUE) {
            if (value instanceof Integer) {
                return createWriteableInt(object, index, (int) value, ProfileHolder.empty()).setElementImpl(object, index, value, strict);
            } else if (value instanceof Double) {
                return createWriteableDouble(object, index, (double) value, ProfileHolder.empty()).setElementImpl(object, index, value, strict);
            } else {
                return createWriteableObject(object, index, value, ProfileHolder.empty()).setElementImpl(object, index, value, strict);
            }
        } else {
            return SparseArray.makeSparseArray(object, this).setElementImpl(object, index, value, strict);
        }
    }

    @Override
    public final Object getElement(JSDynamicObject object, long index) {
        if (isInBoundsFast(object, index)) {
            return getElementInBounds(object, (int) index);
        } else {
            return Undefined.instance;
        }
    }

    @Override
    public final Object getElementInBounds(JSDynamicObject object, long index) {
        assert isInBoundsFast(object, index);
        return getElementInBounds(object, (int) index);
    }

    public abstract Object getElementInBounds(JSDynamicObject object, int index);

    @Override
    public final long length(JSDynamicObject object) {
        return lengthInt(object);
    }

    @Override
    public long firstElementIndex(JSDynamicObject object) {
        return 0;
    }

    @Override
    public long lastElementIndex(JSDynamicObject object) {
        return length(object) - 1;
    }

    @Override
    public long nextElementIndex(JSDynamicObject object, long index) {
        if (index >= lastElementIndex(object)) {
            return JSRuntime.MAX_SAFE_INTEGER_LONG;
        }
        return index + 1;
    }

    @Override
    public long previousElementIndex(JSDynamicObject object, long index) {
        return index - 1;
    }

    /**
     * Returns true if the index can be written using inBoundsFast access mode.
     */
    @Override
    public final boolean isInBoundsFast(JSDynamicObject object, long index) {
        return firstElementIndex(object) <= index && index <= lastElementIndex(object);
    }

    public abstract AbstractWritableArray createWriteableDouble(JSDynamicObject object, long index, double value, ProfileHolder profile);

    public abstract AbstractWritableArray createWriteableInt(JSDynamicObject object, long index, int value, ProfileHolder profile);

    public abstract AbstractWritableArray createWriteableObject(JSDynamicObject object, long index, Object value, ProfileHolder profile);

    public abstract AbstractWritableArray createWriteableJSObject(JSDynamicObject object, long index, JSDynamicObject value, ProfileHolder profile);

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
    public boolean hasHoles(JSDynamicObject object) {
        return false;
    }
}
