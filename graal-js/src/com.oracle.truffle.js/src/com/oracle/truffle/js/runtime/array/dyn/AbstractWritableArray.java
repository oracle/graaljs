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

import java.util.Locale;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.InlineSupport;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.DynamicArray;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.InlinedProfileBag;

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

    protected static final void setArrayProperties(JSDynamicObject object, Object array, long length, int usedLength, long indexOffset, int arrayOffset) {
        arraySetArray(object, array);
        setArrayProperties(object, length, usedLength, indexOffset, arrayOffset);
    }

    protected static final void setArrayProperties(JSDynamicObject object, long length, int usedLength, long indexOffset, int arrayOffset) {
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

    abstract AbstractWritableArray sameTypeHolesArray(JSDynamicObject object, int length, Object array, long indexOffset, int arrayOffset, int usedLength, int holeCount);

    abstract void fillWithHoles(Object array, int fromIndex, int toIndex);

    /**
     * Returns true if the index can be written using inBoundsFast access mode.
     */
    @Override
    public final boolean isInBoundsFast(JSDynamicObject object, long index) {
        return firstElementIndex(object) <= index && index <= lastElementIndex(object);
    }

    protected abstract int prepareInBoundsFast(JSDynamicObject object, long index);

    public final boolean isInBounds(JSDynamicObject object, int index) {
        return isSupported(object, index) && rangeCheck(object, index);
    }

    protected abstract int prepareInBounds(JSDynamicObject object, int index, Node node, SetSupportedProfileAccess profile);

    protected static void prepareInBoundsZeroBased(JSDynamicObject object, int index, Node node, SetSupportedProfileAccess profile) {
        long length = arrayGetLength(object);
        if (profile.inBoundsZeroBasedSetLength(node, index >= length)) {
            arraySetLength(object, length + 1);
        }
        int usedLength = getUsedLength(object);
        if (profile.inBoundsZeroBasedSetUsedLength(node, index >= usedLength)) {
            arraySetUsedLength(object, usedLength + 1);
        }
    }

    Object getArrayObject(JSDynamicObject object) {
        return JSAbstractArray.arrayGetArray(object);
    }

    abstract int getArrayLength(Object array);

    protected static int getUsedLength(JSDynamicObject object) {
        return arrayGetUsedLength(object);
    }

    protected final int prepareInBoundsContiguous(JSDynamicObject object, int index, Node node, SetSupportedProfileAccess profile) {
        int internalIndex = ensureCapacityContiguous(object, prepareInBoundsFast(object, index), node, profile);
        updateContiguousState(object, internalIndex, node, profile);
        return internalIndex;
    }

    protected final int prepareInBoundsHoles(JSDynamicObject object, int index, Node node, SetSupportedProfileAccess profile) {
        int internalIndex = prepareInBoundsFast(object, index);
        fillHoles(object, internalIndex, updateHolesState(object, internalIndex, node, profile), node, profile);
        return internalIndex;
    }

    private boolean rangeCheck(JSDynamicObject object, int index) {
        int internalIndex = prepareInBoundsFast(object, index);
        return internalIndex >= 0 && internalIndex < getArrayCapacity(object);
    }

    @SuppressWarnings("unused")
    public boolean containsHoles(JSDynamicObject object, long index) {
        return false;
    }

    public abstract boolean isSupported(JSDynamicObject object, long index);

    public static boolean isSupportedZeroBased(JSDynamicObject object, int index) {
        return index >= 0 && index <= getUsedLength(object); // lastIndex+1 can be set!
    }

    public final boolean isSupportedContiguous(JSDynamicObject object, long index) {
        return index >= firstElementIndex(object) - 1 && index <= lastElementIndex(object) + 1 && index < Integer.MAX_VALUE;
    }

    public final boolean isSupportedHoles(JSDynamicObject object, long index) {
        return index >= firstElementIndex(object) - JSConfig.MaxArrayHoleSize && index <= lastElementIndex(object) + JSConfig.MaxArrayHoleSize && index < Integer.MAX_VALUE;
    }

    protected abstract int prepareSupported(JSDynamicObject object, int index, Node node, SetSupportedProfileAccess profile);

    protected final void prepareSupportedZeroBased(JSDynamicObject object, int index, Node node, SetSupportedProfileAccess profile) {
        ensureCapacity(object, index, 0, node, profile);
        prepareInBoundsZeroBased(object, index, node, profile);
    }

    protected final int prepareSupportedContiguous(JSDynamicObject object, int index, Node node, SetSupportedProfileAccess profile) {
        int internalIndex = ensureCapacityContiguous(object, prepareInBoundsFast(object, index), node, profile);
        updateContiguousState(object, internalIndex, node, profile);
        return internalIndex;
    }

    protected final int prepareSupportedHoles(JSDynamicObject object, int index, Node node, SetSupportedProfileAccess profile) {
        int internalIndex = prepareInBoundsFast(object, index);
        internalIndex = ensureCapacityContiguous(object, internalIndex, node, profile);
        fillHoles(object, internalIndex, updateHolesState(object, internalIndex, node, profile), node, profile);
        return internalIndex;
    }

    @SuppressWarnings("unused")
    protected final void incrementHolesCount(JSDynamicObject object, int offset) {
        assert isHolesType();
        arraySetHoleCount(object, arrayGetHoleCount(object) + offset);
    }

    protected abstract void setHoleValue(JSDynamicObject object, int index);

    protected abstract int getArrayCapacity(JSDynamicObject object);

    /**
     * The arrayOffset (int) is the first element in internal array.
     */
    @SuppressWarnings("unused")
    protected int getArrayOffset(JSDynamicObject object) {
        return 0;
    }

    @SuppressWarnings("unused")
    protected void setArrayOffset(JSDynamicObject object, int value) {
        throw Errors.shouldNotReachHere();
    }

    /**
     * The indexOffset (int) is the first element is in array[indexOffset + arrayOffset].
     */
    @SuppressWarnings("unused")
    protected long getIndexOffset(JSDynamicObject object) {
        throw Errors.shouldNotReachHere();
    }

    @SuppressWarnings("unused")
    protected void setIndexOffset(JSDynamicObject object, long value) {
        throw Errors.shouldNotReachHere();
    }

    private int ensureCapacity(JSDynamicObject object, int internalIndex, long indexOffset, Node node, SetSupportedProfileAccess profile) {
        assert -indexOffset <= internalIndex; // 0 <= index
        int capacity = getArrayCapacity(object);
        if (profile.ensureCapacityGrow(node, internalIndex >= 0 && internalIndex < capacity)) {
            return 0;
        } else {
            long minCapacity;
            if (profile.ensureCapacityGrowLeft(node, internalIndex < 0)) {
                minCapacity = -internalIndex + (long) capacity;
            } else {
                minCapacity = internalIndex + 1L;
            }
            long newCapacity = Math.max(minCapacity, (long) capacity + (capacity >>> 1));
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.SLOWPATH_PROBABILITY, newCapacity > JSConfig.SOFT_MAX_ARRAY_LENGTH)) {
                if (JSConfig.SOFT_MAX_ARRAY_LENGTH < minCapacity) {
                    profile.enterArrayTooLargeBranch(node);
                    throw Errors.outOfMemoryError();
                }
                newCapacity = JSConfig.SOFT_MAX_ARRAY_LENGTH;
            }

            int offset = 0;
            if (internalIndex < 0) {
                offset = (int) newCapacity - capacity;
            }
            resizeArray(object, (int) newCapacity, capacity, offset);
            return offset;
        }
    }

    private int ensureCapacityContiguous(JSDynamicObject object, int internalIndex, Node node, SetSupportedProfileAccess profile) {
        int offset = ensureCapacity(object, internalIndex, getIndexOffset(object), node, profile);
        if (offset != 0) {
            setIndexOffset(object, getIndexOffset(object) - offset);
            setArrayOffset(object, getArrayOffset(object) + offset);
        }
        return internalIndex + offset;
    }

    private void updateContiguousState(JSDynamicObject object, int internalIndex, Node node, SetSupportedProfileAccess profile) {
        int offset = getArrayOffset(object);
        int used = getUsedLength(object);
        if (profile.updateStatePrepend(node, internalIndex < offset)) {
            arraySetUsedLength(object, used + 1);
            setArrayOffset(object, offset - 1);
        } else if (profile.updateStateAppend(node, internalIndex >= offset + used)) {
            arraySetUsedLength(object, used + 1);
            long length = arrayGetLength(object);
            long calcLength = getIndexOffset(object) + offset + used + 1;
            if (profile.updateStateSetLength(node, calcLength > length)) {
                arraySetLength(object, calcLength);
            }
        }
    }

    private int updateHolesState(JSDynamicObject object, int internalIndex, Node node, SetSupportedProfileAccess profile) {
        int offset = getArrayOffset(object);
        int used = getUsedLength(object);
        int size;
        if (profile.updateStatePrepend(node, internalIndex < offset)) {
            size = -(offset - internalIndex);
        } else if (profile.updateStateAppend(node, internalIndex >= offset + used)) {
            if (used == 0) {
                // empty array, array offset should match the new element
                offset = internalIndex;
            }
            size = internalIndex - (offset + used) + 1;
        } else {
            if (profile.updateHolesStateIsHole(node, isHolePrepared(object, internalIndex))) {
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
            if (profile.updateStateSetLength(node, calcLength > length)) {
                arraySetLength(object, calcLength);
            }
        }
        arraySetUsedLength(object, used);
        setArrayOffset(object, offset);

        return size;
    }

    protected void fillHoles(JSDynamicObject object, int internalIndex, int grown, Node node, SetSupportedProfileAccess profile) {
        int start;
        int end;
        if (profile.fillHolesRight(node, grown > 1)) {
            start = internalIndex - grown + 1;
            end = internalIndex;
        } else if (profile.fillHolesLeft(node, grown < -1)) {
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

    protected final boolean checkFillHoles(JSDynamicObject object, int internalIndex, int grown) {
        int start;
        int end;
        if (grown > 1) {
            start = internalIndex - grown + 1;
            end = internalIndex;
        } else if (grown < -1) {
            start = internalIndex + 1;
            end = internalIndex - grown;
        } else {
            return true;
        }

        for (int i = start; i < end; i++) {
            if (!isHolePrepared(object, i)) {
                return false;
            }
        }
        return true;
    }

    public abstract AbstractWritableArray toDouble(JSDynamicObject object, long index, double value);

    public abstract AbstractWritableArray toObject(JSDynamicObject object, long index, Object value);

    @SuppressWarnings("unused")
    public AbstractWritableArray toContiguous(JSDynamicObject object, long index, Object value) {
        return this;
    }

    public abstract AbstractWritableArray toHoles(JSDynamicObject object, long index, Object value);

    @SuppressWarnings("unused")
    public AbstractWritableArray toNonHoles(JSDynamicObject object, long index, Object value) {
        assert !isHolesType();
        return this;
    }

    public final SparseArray toSparse(JSDynamicObject object, long index, Object value) {
        SparseArray newArray = SparseArray.makeSparseArray(object, this);
        if (JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    protected abstract void resizeArray(JSDynamicObject object, int newCapacity, int oldCapacity, int offset);

    public final boolean isSparse(JSDynamicObject object, long index) {
        return !isSupportedHoles(object, index);
    }

    @Override
    public boolean hasElement(JSDynamicObject object, long index) {
        return isInBoundsFast(object, index);
    }

    @Override
    public long nextElementIndex(JSDynamicObject object, long index) {
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
    protected abstract boolean isHolePrepared(JSDynamicObject object, int index);

    protected final long nextElementIndexHoles(JSDynamicObject object, long index0) {
        long index = index0;
        long firstIdx = firstElementIndex(object);
        long lastI = lastElementIndex(object);
        if (index0 < firstIdx) {
            return (firstIdx <= lastI) ? firstIdx : JSRuntime.MAX_SAFE_INTEGER_LONG;
        }
        do {
            index++;
            if (index > lastI) {
                return JSRuntime.MAX_SAFE_INTEGER_LONG;
            }
        } while (isHolePrepared(object, prepareInBoundsFast(object, index)));
        return index;
    }

    protected final long nextElementIndexZeroBased(JSDynamicObject object, long index) {
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
    public long previousElementIndex(JSDynamicObject object, long index) {
        long lastIdx = lastElementIndex(object);
        if (index > lastIdx) {
            return lastIdx;
        }
        if (index - 1 < firstElementIndex(object)) {
            return -1;
        }
        return index - 1;
    }

    protected final long previousElementIndexHoles(JSDynamicObject object, long index0) {
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
    public final long length(JSDynamicObject object) {
        return arrayGetLength(object);
    }

    @Override
    public final int lengthInt(JSDynamicObject object) {
        return (int) length(object);
    }

    @Override
    public final ScriptArray setLengthImpl(JSDynamicObject object, long length, Node node, SetLengthProfileAccess profile) {
        if (profile.lengthZero(node, length == 0)) {
            arraySetLength(object, length);
            return ConstantEmptyArray.createConstantEmptyArray();
        } else if (profile.lengthLess(node, length < length(object))) {
            setLengthLess(object, length, node, profile);
        } else {
            arraySetLength(object, length);
        }
        return this;
    }

    protected abstract void setLengthLess(JSDynamicObject object, long length, Node node, SetLengthProfileAccess profile);

    protected void setLengthLessZeroBased(JSDynamicObject object, long length, Node node, SetLengthProfileAccess profile) {
        long oldLength = arrayGetLength(object);
        arraySetLength(object, length);
        if (profile.zeroBasedSetUsedLength(node, getUsedLength(object) > length)) {
            arraySetUsedLength(object, (int) length);
        }
        if (profile.zeroBasedClearUnusedArea(node, length < oldLength)) {
            clearUnusedArea(object, (int) length, (int) oldLength, 0, node, profile);
        }
    }

    protected final void setLengthLessContiguous(JSDynamicObject object, long length, Node node, SetLengthProfileAccess profile) {
        long indexOffset = getIndexOffset(object);
        int arrayOffset = getArrayOffset(object);
        arraySetLength(object, length);
        if (profile.contiguousZeroUsed(node, length <= indexOffset)) {
            arraySetUsedLength(object, 0);
            setIndexOffset(object, length - 1);
            setArrayOffset(object, 0);
            long arrayCapacity = getArrayCapacity(object);
            clearUnusedArea(object, 0, (int) arrayCapacity, 0, node, profile);
        } else {
            int oldUsed = getUsedLength(object);
            int newUsed = Math.min(oldUsed, (int) (length - indexOffset - arrayOffset));
            int newUsedLength = (int) (previousElementIndex(object, indexOffset + arrayOffset + newUsed) + 1 - arrayOffset - indexOffset);

            if (profile.contiguousNegativeUsed(node, newUsedLength < 0)) {
                newUsedLength = 0;
                setArrayOffset(object, 0);
                setIndexOffset(object, 0);
            }
            arraySetUsedLength(object, newUsedLength);
            if (profile.contiguousShrinkUsed(node, newUsedLength < oldUsed)) {
                if (isHolesType()) {
                    incrementHolesCount(object, -countHolesPrepared(object, arrayOffset + newUsedLength, arrayOffset + oldUsed));
                    assert assertHoleCount(object);
                }

                // use old arrayOffset
                clearUnusedArea(object, newUsedLength, oldUsed, arrayOffset, node, profile);
            }
        }
    }

    /**
     * After shortening the array, the now unused area has to be cleared.
     */
    protected void clearUnusedArea(JSDynamicObject object, int startIdx, int endIdx, int arrayOffset, Node node, SetLengthProfileAccess profile) {
        int arrayCapacity = getArrayCapacity(object);
        if (profile.clearUnusedArea(node, startIdx < -1 || (startIdx + arrayOffset) >= arrayCapacity)) {
            return;
        }
        int start = startIdx + arrayOffset;
        int end = Math.min(endIdx + arrayOffset, arrayCapacity - 1);
        for (int i = start; i <= end; i++) {
            setHoleValue(object, i);
        }
    }

    @Override
    public final Object getElement(JSDynamicObject object, long index) {
        if (isInBoundsFast(object, index)) {
            return getInBoundsFast(object, (int) index);
        } else {
            return Undefined.instance;
        }
    }

    @Override
    public final Object getElementInBounds(JSDynamicObject object, long index) {
        assert isInBoundsFast(object, index);
        return getInBoundsFast(object, (int) index);
    }

    public abstract Object getInBoundsFast(JSDynamicObject object, int index);

    public int getInBoundsFastInt(JSDynamicObject object, int index) throws UnexpectedResultException {
        Object value = getInBoundsFast(object, index);
        if (value instanceof Integer) {
            return (int) value;
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnexpectedResultException(value);
        }
    }

    public double getInBoundsFastDouble(JSDynamicObject object, int index) throws UnexpectedResultException {
        Object value = getInBoundsFast(object, index);
        if (value instanceof Double) {
            return (double) value;
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnexpectedResultException(value);
        }
    }

    private void fixHolesArrayStartingWithAHole(JSDynamicObject object, long index, int preparedindex) {
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
    }

    protected final ScriptArray deleteElementHoles(JSDynamicObject object, long index) {
        if (isInBoundsFast(object, index)) {
            int preparedindex = prepareInBoundsFast(object, (int) index);
            if (!isHolePrepared(object, preparedindex)) {
                int arrayOffset = getArrayOffset(object);
                if (arrayOffset == preparedindex) {
                    fixHolesArrayStartingWithAHole(object, index, preparedindex);
                } else if (arrayOffset + arrayGetUsedLength(object) == preparedindex) {
                    long previousNonHoles = previousElementIndexHoles(object, index);
                    assert previousNonHoles >= 0;
                    int preparedPreviousNonHoles = prepareInBoundsFast(object, (int) previousNonHoles);
                    arraySetUsedLength(object, arrayGetUsedLength(object) - preparedindex + preparedPreviousNonHoles);
                    incrementHolesCount(object, -countHolesPrepared(object, preparedPreviousNonHoles, preparedindex));
                } else {
                    incrementHolesCount(object, +1);
                }
                setHoleValue(object, preparedindex); // clear unused
            }
        }
        assert assertHoleCount(object);
        return this;
    }

    @TruffleBoundary
    protected final void traceWriteValue(String access, int index, Object value) {
        traceWrite(getClass().getSimpleName() + "." + access, index, value);
    }

    @SuppressWarnings("unused")
    public ScriptArray toNonContiguous(JSDynamicObject object, int index, Object value, Node node, SetSupportedProfileAccess profile) {
        return this;
    }

    @Override
    protected abstract AbstractWritableArray withIntegrityLevel(int newIntegrityLevel);

    public abstract Object allocateArray(int length);

    ScriptArray addRangeImplContiguous(JSDynamicObject object, long offset, int size) {
        long indexOffset = getIndexOffset(object);
        int arrayOffset = getArrayOffset(object);
        if (offset <= indexOffset + arrayOffset) {
            setIndexOffset(object, indexOffset + size);
            return this;
        } else {
            Object array = getArrayObject(object);
            int usedLength = getUsedLength(object);
            int arrayLength = getArrayLength(array);
            if (arrayOffset + usedLength + size <= arrayLength) {
                int lastIndex = (arrayOffset + usedLength);
                int effectiveOffset = (int) (offset - indexOffset);
                int copySize = (lastIndex - effectiveOffset);
                if (copySize > 0) {
                    System.arraycopy(array, effectiveOffset, array, (effectiveOffset + size), copySize);
                    fillWithHoles(array, effectiveOffset, effectiveOffset + size);
                    if (isHolesType()) {
                        arraySetHoleCount(object, arrayGetHoleCount(object) + size);
                    }
                    arraySetUsedLength(object, usedLength + size);
                }
                return this;
            } else {
                return addRangeGrow(object, array, arrayLength, usedLength, lengthInt(object), (int) (offset - indexOffset), size, arrayOffset, indexOffset);
            }
        }
    }

    private ScriptArray addRangeGrow(JSDynamicObject object, Object array, int arrayLength, int usedLength, int length, int offset, int size, int arrayOffset, long indexOffset) {
        Object newArray = allocateArray(nextPower(arrayLength + size));
        if (offset >= arrayOffset + usedLength) {
            System.arraycopy(array, arrayOffset, newArray, arrayOffset, usedLength);
            fillWithHoles(newArray, arrayOffset + usedLength, arrayOffset + usedLength + size);
            return ensureHolesArray(object, length + size, newArray, indexOffset, arrayOffset, usedLength + size, arrayGetHoleCount(object) + size);
        } else {
            System.arraycopy(array, arrayOffset, newArray, arrayOffset, offset - arrayOffset);
            int toCopy = (arrayOffset + usedLength) - offset;
            System.arraycopy(array, offset, newArray, offset + size, toCopy);
            arraySetLength(object, length + size);
            arraySetArray(object, newArray);
            arraySetUsedLength(object, usedLength + size);
            if (isHolesType()) {
                fillWithHoles(newArray, offset, offset + size);
                arraySetHoleCount(object, arrayGetHoleCount(object) + size);
            }
            return this;
        }
    }

    private ScriptArray ensureHolesArray(JSDynamicObject object, int length, Object newArray, long indexOffset, int arrayOffset, int usedLength, int holesCount) {
        AbstractWritableArray newArrayObject = sameTypeHolesArray(object, length, newArray, indexOffset, arrayOffset, usedLength, holesCount);
        if (newArrayObject != this && JSConfig.TraceArrayTransitions) {
            traceArrayTransition(this, newArrayObject, 0, null);
        }
        return newArrayObject;
    }

    ScriptArray addRangeImplZeroBased(JSDynamicObject object, long offset, int size) {
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

    protected final ScriptArray removeRangeContiguous(JSDynamicObject object, long start, long end) {
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
                assert usedStartIntl == arrayOffset;
                assert usedEndIntl == arrayOffset + usedLength;
                fillWithHoles(getArrayObject(object), usedStartIntl, usedEndIntl);
                return this;
            }
        }

        int arrayOffsetNew = arrayOffset;
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
                arrayOffsetNew = Math.max(startIntl, arrayOffset - (endIntl - startIntl));
                setArrayOffset(object, arrayOffsetNew);
            }

            int length = usedLength + arrayOffset - endIntl;
            if (length > 0) {
                moveRangePrepared(object, endIntl, startIntl, length);
            }
        }

        if (usedDelta > 0) {
            // Unused part of the internal array should not keep objects alive
            fillWithHoles(getArrayObject(object), arrayOffsetNew + newUsedLength, arrayOffset + usedLength);
        }

        return this;
    }

    protected final ScriptArray removeRangeHoles(JSDynamicObject object, long start, long end) {
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
        arrayOffset = getArrayOffset(object);
        if (isHolePrepared(object, arrayOffset)) {
            fixHolesArrayStartingWithAHole(object, arrayOffset + getIndexOffset(object), arrayOffset);
        }
        return this;
    }

    @Override
    public boolean hasHoles(JSDynamicObject object) {
        assert arrayGetHoleCount(object) == 0 : arrayGetHoleCount(object);
        return false;
    }

    @Override
    public boolean hasHolesOrUnused(JSDynamicObject object) {
        int length = lengthInt(object);
        int usedLength = getUsedLength(object);
        return usedLength < length || hasHoles(object);
    }

    protected final int countHoles(JSDynamicObject object) {
        assert isHolesType();
        int arrayOffset = getArrayOffset(object);
        return countHolesPrepared(object, arrayOffset, arrayOffset + getUsedLength(object));
    }

    private int countHolesPrepared(JSDynamicObject object, int start, int end) {
        assert isHolesType();
        int holeCount = 0;
        for (int index = start; index < end; index++) {
            if (isHolePrepared(object, index)) {
                holeCount++;
            }
        }
        return holeCount;
    }

    protected final boolean assertHoleCount(JSDynamicObject object) {
        assert isHolesType();
        int holeCount = arrayGetHoleCount(object);
        int countedHoles = countHoles(object);
        assert holeCount == countedHoles : String.format(Locale.ROOT, "holeCount, %d, differs from the actual count, %d", holeCount, countedHoles);
        return true;
    }

    /**
     * Move {@code len} elements from {@code src} to {@code dst}.
     */
    protected abstract void moveRangePrepared(JSDynamicObject object, int src, int dst, int len);

    @Override
    public ScriptArray shiftRangeImpl(JSDynamicObject object, long from) {
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
                fillWithHoles(getArrayObject(object), arrayOffset, newArrayOffset);
                setArrayProperties(object, newLength, newUsedLength, newIndexOffset, newArrayOffset);
                return this;
            }
        }
        return removeRangeImpl(object, 0, from);
    }

    protected static boolean unusedElementsAreHoles(Object[] array, int usedStart, int usedLength) {
        for (int i = 0; i < usedStart; i++) {
            if (array[i] != null) {
                return false;
            }
        }
        for (int i = usedStart + usedLength; i < array.length; i++) {
            if (array[i] != null) {
                return false;
            }
        }
        return true;
    }

    public static class SetSupportedProfileAccess extends InlinedProfileBag {

        protected static final int REQUIRED_BITS = 10 * CONDITION_PROFILE_STATE_BITS + 1 * BRANCH_PROFILE_STATE_BITS;
        private static final int ensureCapacityGrow;
        private static final int ensureCapacityGrowLeft;
        private static final int inBoundsZeroBasedSetLength;
        private static final int inBoundsZeroBasedSetUsedLength;
        private static final int updateStatePrepend;
        private static final int updateStateAppend;
        private static final int updateStateSetLength;
        private static final int updateHolesStateIsHole;
        private static final int fillHolesLeft;
        private static final int fillHolesRight;
        private static final int arrayTooLargeBranch;

        private static final SetSupportedProfileAccess UNCACHED = new SetSupportedProfileAccess(null);

        static {
            try (var b = new Builder(REQUIRED_BITS)) {
                ensureCapacityGrow = b.conditionProfile();
                ensureCapacityGrowLeft = b.conditionProfile();
                inBoundsZeroBasedSetLength = b.conditionProfile();
                inBoundsZeroBasedSetUsedLength = b.conditionProfile();
                updateStatePrepend = b.conditionProfile();
                updateStateAppend = b.conditionProfile();
                updateStateSetLength = b.conditionProfile();
                updateHolesStateIsHole = b.conditionProfile();
                fillHolesLeft = b.conditionProfile();
                fillHolesRight = b.conditionProfile();
                arrayTooLargeBranch = b.branchProfile();
            }
        }

        @NeverDefault
        public static SetSupportedProfileAccess getUncached() {
            return UNCACHED;
        }

        @NeverDefault
        public static SetSupportedProfileAccess inline(
                        @InlineSupport.RequiredField(value = InlineSupport.StateField.class, bits = REQUIRED_BITS) InlineSupport.InlineTarget inlineTarget) {
            return new SetSupportedProfileAccess(inlineTarget.getState(0, REQUIRED_BITS));
        }

        protected SetSupportedProfileAccess(InlineSupport.StateField stateField) {
            super(stateField);
        }

        public final boolean ensureCapacityGrow(Node node, boolean condition) {
            return profile(node, condition, ensureCapacityGrow);
        }

        public final boolean ensureCapacityGrowLeft(Node node, boolean condition) {
            return profile(node, condition, ensureCapacityGrowLeft);
        }

        public final boolean inBoundsZeroBasedSetLength(Node node, boolean condition) {
            return profile(node, condition, inBoundsZeroBasedSetLength);
        }

        public final boolean inBoundsZeroBasedSetUsedLength(Node node, boolean condition) {
            return profile(node, condition, inBoundsZeroBasedSetUsedLength);
        }

        public final boolean updateStatePrepend(Node node, boolean condition) {
            return profile(node, condition, updateStatePrepend);
        }

        public final boolean updateStateAppend(Node node, boolean condition) {
            return profile(node, condition, updateStateAppend);
        }

        public final boolean updateStateSetLength(Node node, boolean condition) {
            return profile(node, condition, updateStateSetLength);
        }

        public final boolean updateHolesStateIsHole(Node node, boolean condition) {
            return profile(node, condition, updateHolesStateIsHole);
        }

        public final boolean fillHolesLeft(Node node, boolean condition) {
            return profile(node, condition, fillHolesLeft);
        }

        public final boolean fillHolesRight(Node node, boolean condition) {
            return profile(node, condition, fillHolesRight);
        }

        public final void enterArrayTooLargeBranch(Node node) {
            enter(node, arrayTooLargeBranch);
        }
    }
}
