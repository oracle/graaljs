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
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetHoleCount;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetLength;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetUsedLength;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayOffset;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetHoleCount;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetIndexOffset;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetLength;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetUsedLength;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * Base class of a javascript dynamic writable array. The array implementation uses three write
 * access modes:
 * <ul>
 * <li>InBoundsFast: Accessed index is in bounds of the physical and logical array. So no
 * housekeeping should usually be necessary in this access mode.</li>
 * <li>InBounds: Accessed index is in bounds of the physical backing array. Little housekeeping.
 * </li>
 * <li>Supported: If a write to this index is supported by the array. Full housekeeping.</li>
 * <li>Not Supported: If a write to this index is not supported by the array. The array will
 * transition to another implementation that supports the written index.</li>
 * </ul>
 *
 */
public abstract class AbstractWritableArray extends DynamicArray {

    protected static final void setArrayProperties(DynamicObject object, Object array, long length, int usedLength, long indexOffset, int arrayOffset) {
        arraySetArray(object, array);
        setArrayProperties(object, length, usedLength, indexOffset, arrayOffset);
    }

    protected static final void setArrayProperties(DynamicObject object, long length, int usedLength, long indexOffset, int arrayOffset) {
        arraySetLength(object, length);
        arraySetUsedLength(object, usedLength);
        arraySetIndexOffset(object, indexOffset);
        arraySetArrayOffset(object, arrayOffset);
    }

    // /** The length of the array that is reported to the user. */
    // protected int length;
    // /** The actual length occupied in the backing array. */
    // protected int usedLength;

    protected AbstractWritableArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    abstract AbstractWritableArray sameTypeHolesArray(DynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount);

    abstract void fillWithHoles(Object array, int fromIndex, int toIndex);

    /**
     * Returns true if the index can be written using inBoundsFast access mode.
     */
    @Override
    public final boolean isInBoundsFast(DynamicObject object, long index) {
        return firstElementIndex(object) <= index && index <= lastElementIndex(object);
    }

    protected abstract int prepareInBoundsFast(DynamicObject object, long index);

    public final boolean isInBounds(DynamicObject object, int index) {
        return isSupported(object, index) && rangeCheck(object, index);
    }

    protected abstract int prepareInBounds(DynamicObject object, int index, ProfileHolder profile);

    protected static void prepareInBoundsZeroBased(DynamicObject object, int index, ProfileHolder profile) {
        long length = arrayGetLength(object);
        if (SET_SUPPORTED_PROFILE_ACCESS.inBoundsZeroBasedSetLength(profile, index >= length)) {
            arraySetLength(object, length + 1);
        }
        int usedLength = getUsedLength(object);
        if (SET_SUPPORTED_PROFILE_ACCESS.inBoundsZeroBasedSetUsedLength(profile, index >= usedLength)) {
            arraySetUsedLength(object, usedLength + 1);
        }
    }

    Object getArrayObject(DynamicObject object) {
        return JSAbstractArray.arrayGetArray(object);
    }

    abstract int getArrayLength(Object array);

    protected static int getUsedLength(DynamicObject object) {
        return arrayGetUsedLength(object);
    }

    protected final int prepareInBoundsContiguous(DynamicObject object, int index, ProfileHolder profile) {
        int internalIndex = ensureCapacityContiguous(object, prepareInBoundsFast(object, index), profile);
        updateContiguousState(object, internalIndex, profile);
        return internalIndex;
    }

    protected final int prepareInBoundsHoles(DynamicObject object, int index, ProfileHolder profile) {
        int internalIndex = prepareInBoundsFast(object, index);
        fillHoles(object, internalIndex, updateHolesState(object, internalIndex, profile), profile);
        return internalIndex;
    }

    private boolean rangeCheck(DynamicObject object, int index) {
        int internalIndex = prepareInBoundsFast(object, index);
        return internalIndex >= 0 && internalIndex < getArrayCapacity(object);
    }

    @SuppressWarnings("unused")
    public boolean containsHoles(DynamicObject object, long index) {
        return false;
    }

    public abstract boolean isSupported(DynamicObject object, long index);

    public static boolean isSupportedZeroBased(DynamicObject object, int index) {
        return index >= 0 && index <= getUsedLength(object); // lastIndex+1 can be set!
    }

    public final boolean isSupportedContiguous(DynamicObject object, long index) {
        return index >= firstElementIndex(object) - 1 && index <= lastElementIndex(object) + 1 && index < Integer.MAX_VALUE;
    }

    public final boolean isSupportedHoles(DynamicObject object, long index) {
        return index >= firstElementIndex(object) - JSConfig.MaxArrayHoleSize && index <= lastElementIndex(object) + JSConfig.MaxArrayHoleSize && index < Integer.MAX_VALUE;
    }

    protected abstract int prepareSupported(DynamicObject object, int index, ProfileHolder profile);

    protected final void prepareSupportedZeroBased(DynamicObject object, int index, ProfileHolder profile) {
        ensureCapacity(object, index, 0, profile);
        prepareInBoundsZeroBased(object, index, profile);
    }

    protected final int prepareSupportedContiguous(DynamicObject object, int index, ProfileHolder profile) {
        int internalIndex = ensureCapacityContiguous(object, prepareInBoundsFast(object, index), profile);
        updateContiguousState(object, internalIndex, profile);
        return internalIndex;
    }

    protected final int prepareSupportedHoles(DynamicObject object, int index, ProfileHolder profile) {
        int internalIndex = prepareInBoundsFast(object, index);
        internalIndex = ensureCapacityContiguous(object, internalIndex, profile);
        fillHoles(object, internalIndex, updateHolesState(object, internalIndex, profile), profile);
        return internalIndex;
    }

    @SuppressWarnings("unused")
    protected void incrementHolesCount(DynamicObject object, int offset) {
        throw Errors.shouldNotReachHere();
    }

    protected abstract void setHoleValue(DynamicObject object, int index);

    protected abstract int getArrayCapacity(DynamicObject object);

    /**
     * The arrayOffset (int) is the first element in internal array.
     */
    @SuppressWarnings("unused")
    protected int getArrayOffset(DynamicObject object) {
        return 0;
    }

    @SuppressWarnings("unused")
    protected void setArrayOffset(DynamicObject object, int value) {
        throw Errors.shouldNotReachHere();
    }

    /**
     * The indexOffset (int) is the first element is in array[indexOffset + arrayOffset].
     */
    @SuppressWarnings("unused")
    protected long getIndexOffset(DynamicObject object) {
        throw Errors.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setIndexOffset(DynamicObject object, long value) {
        throw Errors.shouldNotReachHere();
    }

    private int ensureCapacity(DynamicObject object, int internalIndex, long indexOffset, ProfileHolder profile) {
        assert -indexOffset <= internalIndex; // 0 <= index
        int capacity = getArrayCapacity(object);
        if (SET_SUPPORTED_PROFILE_ACCESS.ensureCapacityGrow(profile, internalIndex >= 0 && internalIndex < capacity)) {
            return 0;
        } else {
            long minCapacity;
            if (SET_SUPPORTED_PROFILE_ACCESS.ensureCapacityGrowLeft(profile, internalIndex < 0)) {
                minCapacity = -internalIndex + (long) capacity;
            } else {
                minCapacity = internalIndex + 1L;
            }
            long newCapacity = minCapacity << 1;
            if (newCapacity > SimpleArrayList.MAX_ARRAY_SIZE) {
                if (SimpleArrayList.MAX_ARRAY_SIZE < minCapacity) {
                    CompilerDirectives.transferToInterpreter();
                    throw new OutOfMemoryError();
                }
                newCapacity = SimpleArrayList.MAX_ARRAY_SIZE;
            }

            int offset = 0;
            if (internalIndex < 0) {
                offset = (int) newCapacity - capacity;
                // alignment to zero index
                if (indexOffset < offset) {
                    offset = (int) indexOffset;
                }
            }
            resizeArray(object, (int) newCapacity, capacity, offset);
            return offset;
        }
    }

    private int ensureCapacityContiguous(DynamicObject object, int internalIndex, ProfileHolder profile) {
        int offset = ensureCapacity(object, internalIndex, getIndexOffset(object), profile);
        if (offset != 0) {
            setIndexOffset(object, getIndexOffset(object) - offset);
            setArrayOffset(object, getArrayOffset(object) + offset);
        }
        return internalIndex + offset;
    }

    private void updateContiguousState(DynamicObject object, int internalIndex, ProfileHolder profile) {
        int offset = getArrayOffset(object);
        int used = getUsedLength(object);
        if (SET_SUPPORTED_PROFILE_ACCESS.updateStatePrepend(profile, internalIndex < offset)) {
            arraySetUsedLength(object, used + 1);
            setArrayOffset(object, offset - 1);
        } else if (SET_SUPPORTED_PROFILE_ACCESS.updateStateAppend(profile, internalIndex >= offset + used)) {
            arraySetUsedLength(object, used + 1);
            long length = arrayGetLength(object);
            long calcLength = getIndexOffset(object) + offset + used + 1;
            if (SET_SUPPORTED_PROFILE_ACCESS.updateStateSetLength(profile, calcLength > length)) {
                arraySetLength(object, calcLength);
            }
        }
    }

    private int updateHolesState(DynamicObject object, int internalIndex, ProfileHolder profile) {
        int offset = getArrayOffset(object);
        int used = getUsedLength(object);
        int size;
        if (SET_SUPPORTED_PROFILE_ACCESS.updateStatePrepend(profile, internalIndex < offset)) {
            size = -(offset - internalIndex);
        } else if (SET_SUPPORTED_PROFILE_ACCESS.updateStateAppend(profile, internalIndex >= offset + used)) {
            if (used == 0) {
                // empty array, array offset should match the new element
                offset = internalIndex;
            }
            size = internalIndex - (offset + used) + 1;
        } else {
            if (SET_SUPPORTED_PROFILE_ACCESS.updateHolesStateIsHole(profile, isHolePrepared(object, internalIndex))) {
                incrementHolesCount(object, -1);
            }
            return 0;
        }

        if (size < 0) {
            used -= size;
            offset += size;
        } else {
            used += size;
            long length = arrayGetLength(object);
            long calcLength = getIndexOffset(object) + offset + used;
            if (SET_SUPPORTED_PROFILE_ACCESS.updateStateSetLength(profile, calcLength > length)) {
                arraySetLength(object, calcLength);
            }
        }
        arraySetUsedLength(object, used);
        setArrayOffset(object, offset);

        return size;
    }

    protected void fillHoles(DynamicObject object, int internalIndex, int grown, ProfileHolder profile) {
        int start;
        int end;
        if (SET_SUPPORTED_PROFILE_ACCESS.fillHolesRight(profile, grown > 1)) {
            start = internalIndex - grown + 1;
            end = internalIndex;
        } else if (SET_SUPPORTED_PROFILE_ACCESS.fillHolesLeft(profile, grown < -1)) {
            start = internalIndex + 1;
            end = internalIndex - grown;
        } else {
            return;
        }

        incrementHolesCount(object, end - start);

        for (int i = start; i < end; i++) {
            setHoleValue(object, i);
        }
    }

    public abstract AbstractWritableArray toDouble(DynamicObject object, long index, double value);

    public abstract AbstractWritableArray toObject(DynamicObject object, long index, Object value);

    @SuppressWarnings("unused")
    public AbstractWritableArray toContiguous(DynamicObject object, long index, Object value) {
        return this;
    }

    public abstract AbstractWritableArray toHoles(DynamicObject object, long index, Object value);

    @SuppressWarnings("unused")
    public AbstractWritableArray toNonHoles(DynamicObject object, long index, Object value) {
        assert !isHolesType();
        return this;
    }

    public final SparseArray toSparse(DynamicObject object, long index, Object value) {
        SparseArray newArray = SparseArray.makeSparseArray(object, this);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    protected abstract void resizeArray(DynamicObject object, int newCapacity, int oldCapacity, int offset);

    public final boolean isSparse(DynamicObject object, long index) {
        return !isSupportedHoles(object, index);
    }

    @Override
    public boolean hasElement(DynamicObject object, long index) {
        return isInBoundsFast(object, index);
    }

    @Override
    public long nextElementIndex(DynamicObject object, long index) {
        long firstI = firstElementIndex(object);
        if (index < firstI) {
            return firstI;
        }
        long lastI = lastElementIndex(object);
        if ((index + 1) > lastI) {
            // length is not enough; could be the
            // prototype with shorter length
            return JSRuntime.MAX_SAFE_INTEGER_LONG;
        }
        return index + 1;
    }

    /**
     * Returns true when the array contains a hole at that index. The index is a prepared (internal)
     * index.
     */
    protected abstract boolean isHolePrepared(DynamicObject object, int index);

    protected final long nextElementIndexHoles(DynamicObject object, long index0) {
        long index = index0;
        long firstIdx = firstElementIndex(object);
        if (index0 < firstIdx) {
            return firstIdx;
        }
        long lastI = lastElementIndex(object);
        do {
            index++;
            if (index > lastI) {
                return JSRuntime.MAX_SAFE_INTEGER_LONG;
            }
        } while (isHolePrepared(object, prepareInBoundsFast(object, index)));
        return index;
    }

    protected final long nextElementIndexZeroBased(DynamicObject object, long index) {
        assert index >= -1;
        long lastI = lastElementIndex(object);
        if ((index + 1) > lastI) {
            // length is not enough; could be the
            // prototype with shorter length
            return JSRuntime.MAX_SAFE_INTEGER_LONG;
        }
        return index + 1;
    }

    @Override
    public long previousElementIndex(DynamicObject object, long index) {
        long lastIdx = lastElementIndex(object);
        if (index > lastIdx) {
            return lastIdx;
        }
        if (index - 1 < firstElementIndex(object)) {
            return -1;
        }
        return index - 1;
    }

    protected final long previousElementIndexHoles(DynamicObject object, long index0) {
        long index = index0;
        long lastIdx = lastElementIndex(object);
        if (index0 > lastIdx) {
            return lastIdx;
        }
        long firstIdx = firstElementIndex(object);
        do {
            index--;
        } while (index >= firstIdx && isHolePrepared(object, prepareInBoundsFast(object, index)));
        if (index < firstIdx) {
            return -1;
        }
        return index;
    }

    @Override
    public final long length(DynamicObject object) {
        return arrayGetLength(object);
    }

    @Override
    public final int lengthInt(DynamicObject object) {
        return (int) length(object);
    }

    @Override
    public final ScriptArray setLengthImpl(DynamicObject object, long length, ProfileHolder profile) {
        if (SET_LENGTH_PROFILE.lengthZero(profile, length == 0)) {
            arraySetLength(object, length);
            return ConstantEmptyArray.createConstantEmptyArray();
        } else if (SET_LENGTH_PROFILE.lengthLess(profile, length < length(object))) {
            setLengthLess(object, length, profile);
        } else {
            arraySetLength(object, length);
        }
        return this;
    }

    protected abstract void setLengthLess(DynamicObject object, long length, ProfileHolder profile);

    protected void setLengthLessZeroBased(DynamicObject object, long length, ProfileHolder profile) {
        long oldLength = arrayGetLength(object);
        arraySetLength(object, length);
        if (SET_LENGTH_PROFILE.zeroBasedSetUsedLength(profile, getUsedLength(object) > length)) {
            arraySetUsedLength(object, (int) length);
        }
        if (SET_LENGTH_PROFILE.zeroBasedClearUnusedArea(profile, length < oldLength)) {
            clearUnusedArea(object, (int) length, (int) oldLength, 0, profile);
        }
    }

    protected final void setLengthLessContiguous(DynamicObject object, long length, ProfileHolder profile) {
        long indexOffset = getIndexOffset(object);
        int arrayOffset = getArrayOffset(object);
        arraySetLength(object, length);
        if (SET_LENGTH_PROFILE.contiguousZeroUsed(profile, length <= indexOffset)) {
            arraySetUsedLength(object, 0);
            setIndexOffset(object, length - 1);
            setArrayOffset(object, 0);
            long arrayCapacity = getArrayCapacity(object);
            clearUnusedArea(object, 0, (int) arrayCapacity, 0, profile);
        } else {
            int oldUsed = getUsedLength(object);
            int newUsed = Math.min(oldUsed, (int) (length - indexOffset - arrayOffset));
            int newUsedLength = (int) (previousElementIndex(object, indexOffset + arrayOffset + newUsed) + 1 - arrayOffset - indexOffset);

            if (SET_LENGTH_PROFILE.contiguousNegativeUsed(profile, newUsedLength < 0)) {
                newUsedLength = 0;
                setArrayOffset(object, 0);
                setIndexOffset(object, 0);
            }
            arraySetUsedLength(object, newUsedLength);
            if (SET_LENGTH_PROFILE.contiguousShrinkUsed(profile, newUsedLength < oldUsed)) {
                if (isHolesType()) {
                    incrementHolesCount(object, -countHolesPrepared(object, arrayOffset + newUsedLength, arrayOffset + oldUsed));
                    assert arrayGetHoleCount(object) == countHoles(object);
                }

                // use old arrayOffset
                clearUnusedArea(object, newUsedLength, oldUsed, arrayOffset, profile);
            }
        }
    }

    /**
     * After shortening the array, the now unused area has to be cleared.
     */
    protected void clearUnusedArea(DynamicObject object, int startIdx, int endIdx, int arrayOffset, ProfileHolder profile) {
        int arrayCapacity = getArrayCapacity(object);
        if (SET_LENGTH_PROFILE.clearUnusedArea(profile, startIdx < -1 || (startIdx + arrayOffset) >= arrayCapacity)) {
            return;
        }
        int start = startIdx + arrayOffset;
        int end = Math.min(endIdx + arrayOffset, arrayCapacity - 1);
        for (int i = start; i <= end; i++) {
            setHoleValue(object, i);
        }
    }

    @Override
    public final Object getElement(DynamicObject object, long index) {
        if (isInBoundsFast(object, index)) {
            return getInBoundsFast(object, (int) index);
        } else {
            return Undefined.instance;
        }
    }

    @Override
    public final Object getElementInBounds(DynamicObject object, long index) {
        assert isInBoundsFast(object, index);
        return getInBoundsFast(object, (int) index);
    }

    public abstract Object getInBoundsFast(DynamicObject object, int index);

    public int getInBoundsFastInt(DynamicObject object, int index) throws UnexpectedResultException {
        Object value = getInBoundsFast(object, index);
        if (value instanceof Integer) {
            return (int) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public double getInBoundsFastDouble(DynamicObject object, int index) throws UnexpectedResultException {
        Object value = getInBoundsFast(object, index);
        if (value instanceof Double) {
            return (double) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    protected final Object[] toArrayZeroBased(DynamicObject object) {
        int newLength = getUsedLength(object);
        Object[] newArray = new Object[newLength];
        for (int i = 0; i < newLength; i++) {
            newArray[i] = getInBoundsFast(object, i);
        }
        return newArray;
    }

    protected final ScriptArray deleteElementHoles(DynamicObject object, long index) {
        if (isInBoundsFast(object, index)) {
            int preparedindex = prepareInBoundsFast(object, (int) index);
            if (!isHolePrepared(object, preparedindex)) {
                int arrayOffset = getArrayOffset(object);
                if (arrayOffset == preparedindex) {
                    long nextNonHoles = nextElementIndexHoles(object, index);
                    if (nextNonHoles == JSRuntime.MAX_SAFE_INTEGER_LONG) {
                        // there are no more elements in this Object
                        setArrayOffset(object, 0);
                        arraySetUsedLength(object, 0);
                        arraySetHoleCount(object, 0);
                    } else {
                        int preparedNextNonHoles = prepareInBoundsFast(object, (int) nextNonHoles);
                        int delta = preparedNextNonHoles - preparedindex;
                        setArrayOffset(object, preparedindex + delta);
                        arraySetUsedLength(object, arrayGetUsedLength(object) - delta);
                        incrementHolesCount(object, -countHolesPrepared(object, preparedindex, preparedNextNonHoles));
                    }
                    setHoleValue(object, preparedindex); // clear unused
                } else if (arrayOffset + arrayGetUsedLength(object) == preparedindex) {
                    long previousNonHoles = previousElementIndexHoles(object, index);
                    assert previousNonHoles >= 0;
                    int preparedPreviousNonHoles = prepareInBoundsFast(object, (int) previousNonHoles);
                    arraySetUsedLength(object, arrayGetUsedLength(object) - preparedindex + preparedPreviousNonHoles);
                    incrementHolesCount(object, -countHolesPrepared(object, preparedPreviousNonHoles, preparedindex));
                    setHoleValue(object, preparedindex); // clear unused
                } else {
                    incrementHolesCount(object, +1);
                    setHoleValue(object, preparedindex);
                }
            }
        }
        assert arrayGetHoleCount(object) == countHoles(object);
        return this;
    }

    @TruffleBoundary
    protected final void traceWriteValue(String access, int index, Object value) {
        traceWrite(getClass().getSimpleName() + "." + access, index, value);
    }

    @SuppressWarnings("unused")
    public ScriptArray toNonContiguous(DynamicObject object, int index, Object value, ProfileHolder profile) {
        return this;
    }

    @Override
    protected abstract AbstractWritableArray withIntegrityLevel(int newIntegrityLevel);

    public abstract Object allocateArray(int length);

    ScriptArray addRangeImplContiguous(DynamicObject object, long offset, int size) {
        long indexOffset = getIndexOffset(object);
        int arrayOffset = getArrayOffset(object);
        if (offset <= indexOffset + arrayOffset) {
            setIndexOffset(object, indexOffset + size);
            return this;
        } else {
            Object array = getArrayObject(object);
            int usedLength = getUsedLength(object);
            int arrayLength = getArrayLength(array);
            if (arrayOffset + usedLength + size < arrayLength) {
                int lastIndex = (arrayOffset + usedLength);
                int effectiveOffset = (int) (offset - indexOffset);
                int copySize = (lastIndex - effectiveOffset);
                if (copySize > 0) {
                    System.arraycopy(array, effectiveOffset, array, (effectiveOffset + size), copySize);
                }
                arraySetUsedLength(object, usedLength + size);
                return this;
            } else {
                return addRangeGrow(object, array, arrayLength, usedLength, lengthInt(object), (int) (offset - indexOffset), size, arrayOffset, indexOffset);
            }
        }
    }

    private ScriptArray addRangeGrow(DynamicObject object, Object array, int arrayLength, int usedLength, int length, int offset, int size, int arrayOffset, long indexOffset) {
        Object newArray = allocateArray(nextPower(arrayLength + size));
        if (offset - arrayOffset > arrayLength) {
            System.arraycopy(array, arrayOffset, newArray, arrayOffset, arrayLength);
            fillWithHoles(newArray, usedLength, usedLength + size);
            return ensureHolesArray(object, length + size, newArray, indexOffset, arrayOffset, usedLength + size, arrayGetHoleCount(object) + size);
        } else {
            System.arraycopy(array, arrayOffset, newArray, arrayOffset, offset - arrayOffset);
            int toCopy = (arrayOffset + usedLength) - offset;
            System.arraycopy(array, offset, newArray, offset + size, toCopy);
            arraySetLength(object, length + size);
            arraySetArray(object, newArray);
            arraySetUsedLength(object, usedLength + size);
            return this;
        }
    }

    private ScriptArray ensureHolesArray(DynamicObject object, int length, Object newArray, long indexOffset, int arrayOffset, int usedLength, int holesCount) {
        AbstractWritableArray newArrayObject = sameTypeHolesArray(object, length, newArray, indexOffset, arrayOffset, usedLength, holesCount);
        if (newArrayObject != this && JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArrayObject, 0, null);
        }
        return newArrayObject;
    }

    ScriptArray addRangeImplZeroBased(DynamicObject object, long offset, int size) {
        int iOffset = (int) offset;
        Object array = getArrayObject(object);
        int arrayLength = getArrayLength(array);
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);
        if (usedLength < offset) {
            arraySetLength(object, length + size);
            return this;
        } else if (size + usedLength <= arrayLength) {
            int toCopy = usedLength - iOffset;
            System.arraycopy(array, iOffset, array, iOffset + size, toCopy);
            arraySetUsedLength(object, usedLength + size);
            return this;
        } else {
            return addRangeGrow(object, array, arrayLength, usedLength, arrayLength, iOffset, size, 0, 0L);
        }
    }

    protected final ScriptArray removeRangeContiguous(DynamicObject object, long start, long end) {
        assert start >= 0 && start <= end;

        int usedLength = getUsedLength(object);
        long indexOffset = getIndexOffset(object);
        int arrayOffset = getArrayOffset(object);

        int startIntl = (int) (start - indexOffset);
        int endIntl = (int) (end - indexOffset);

        int usedStartIntl = Math.max(arrayOffset, startIntl);
        int usedEndIntl = Math.min(arrayOffset + usedLength, endIntl);

        int usedDelta = usedEndIntl - usedStartIntl;
        int newUsedLength = usedLength - usedDelta;
        if (usedDelta > 0) {
            arraySetUsedLength(object, newUsedLength);

            if (newUsedLength == 0) {
                setArrayOffset(object, 0);
                setIndexOffset(object, 0);
                return this;
            }
        }

        if (startIntl < 0) {
            int indexOffsetDelta = endIntl - startIntl;
            long indexOffsetNew = Math.max(0, indexOffset - indexOffsetDelta);

            if (endIntl > 0) {
                int length = usedLength + arrayOffset - endIntl;
                if (length > 0) {
                    moveRangePrepared(object, endIntl, 0, length);
                }
                indexOffsetNew = start;
            }

            setIndexOffset(object, indexOffsetNew);
        } else {
            if (startIntl < arrayOffset) {
                int arrayOffsetNew = Math.max(startIntl, arrayOffset - (endIntl - startIntl));
                setArrayOffset(object, arrayOffsetNew);
            }

            int length = usedLength + arrayOffset - endIntl;
            if (length > 0) {
                moveRangePrepared(object, endIntl, startIntl, length);
            }
        }

        return this;
    }

    protected final ScriptArray removeRangeHoles(DynamicObject object, long start, long end) {
        assert isHolesType();
        assert start >= 0 && start <= end;

        int usedLength = getUsedLength(object);
        long indexOffset = getIndexOffset(object);
        int arrayOffset = getArrayOffset(object);

        int startIntl = (int) (start - indexOffset);
        int endIntl = (int) (end - indexOffset);

        if (endIntl > 0) {
            int actualStartIntl = Math.max(arrayOffset, startIntl);
            int actualEndIntl = Math.min(arrayOffset + usedLength, endIntl);
            for (int i = actualStartIntl; i < actualEndIntl; i++) {
                if (isHolePrepared(object, i)) {
                    incrementHolesCount(object, -1);
                }
            }
        }

        removeRangeContiguous(object, start, end);
        return this;
    }

    protected final int countHoles(DynamicObject object) {
        assert isHolesType();
        int arrayOffset = getArrayOffset(object);
        return countHolesPrepared(object, arrayOffset, arrayOffset + getUsedLength(object));
    }

    private int countHolesPrepared(DynamicObject object, int start, int end) {
        assert isHolesType();
        int holeCount = 0;
        for (int index = start; index < end; index++) {
            if (isHolePrepared(object, index)) {
                holeCount++;
            }
        }
        return holeCount;
    }

    /**
     * Move {@code len} elements from {@code src} to {@code dst}.
     */
    protected abstract void moveRangePrepared(DynamicObject object, int src, int dst, int len);

    @Override
    public ScriptArray shiftRangeImpl(DynamicObject object, long from) {
        if (!isHolesType()) {
            long indexOffset = getIndexOffset(object);
            int arrayOffset = getArrayOffset(object);
            long first = indexOffset + arrayOffset;
            if (first >= from) {
                // Can just decrease index offset
                setIndexOffset(object, indexOffset - from);
                return this;
            } // else internal array is affected

            long internalArrayShift = from - first;
            int usedLength = getUsedLength(object);
            if (internalArrayShift < usedLength) {
                long newLength = length(object) - from;
                int newUsedLength = (int) (usedLength - internalArrayShift);
                long newIndexOffset = indexOffset - from;
                int newArrayOffset = (int) (arrayOffset + internalArrayShift);
                setArrayProperties(object, newLength, newUsedLength, newIndexOffset, newArrayOffset);
                return this;
            }
        }
        return removeRangeImpl(object, 0, from);
    }

    protected interface SetSupportedProfileAccess extends ProfileAccess {
        default boolean ensureCapacityGrow(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 0, condition);
        }

        default boolean ensureCapacityGrowLeft(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 1, condition);
        }

        default boolean inBoundsZeroBasedSetLength(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 2, condition);
        }

        default boolean inBoundsZeroBasedSetUsedLength(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 3, condition);
        }

        default boolean updateStatePrepend(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 4, condition);
        }

        default boolean updateStateAppend(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 5, condition);
        }

        default boolean updateStateSetLength(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 6, condition);
        }

        default boolean updateHolesStateIsHole(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 7, condition);
        }

        default boolean fillHolesLeft(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 8, condition);
        }

        default boolean fillHolesRight(ProfileHolder profile, boolean condition) {
            return profile.profile(this, 9, condition);
        }
    }

    public static ProfileHolder createSetSupportedProfile() {
        return ProfileHolder.create(10, SetSupportedProfileAccess.class);
    }

    protected static final SetSupportedProfileAccess SET_SUPPORTED_PROFILE_ACCESS = new SetSupportedProfileAccess() {
    };
}
