/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.array;

import java.util.Comparator;
import java.util.Objects;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArray.AbstractUint32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedBigIntArray;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedFloatArray;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedIntArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;

/**
 * Sorts the elements of a {@link JSTypedArrayObject}.
 *
 * Copies the elements to a temporary array (of a suitable primitive or boxed type, depending on the
 * element type and comparator), sorts the array using the comparator (natural order if null),
 * performs a detached buffer check, and then copies it to a typed array of the same element type
 * and length, which may be the same, or a newly allocated typed array instance.
 *
 * Used by {@code %TypedArray%.prototype.sort,toSorted}.
 */
@ImportStatic({JSConfig.class})
public abstract class JSTypedArraySortNode extends JavaScriptBaseNode {

    static final int CACHE_LIMIT = 6;

    protected JSTypedArraySortNode() {
    }

    public final void execute(JSTypedArrayObject fromArray, JSTypedArrayObject toArray, long length, Comparator<Object> comparator) {
        assert JSRuntime.longIsRepresentableAsInt(length);
        int iLen = (int) length;
        execute(fromArray, toArray, iLen, comparator);
    }

    public abstract void execute(JSTypedArrayObject fromArray, JSTypedArrayObject toArray, int length, Comparator<Object> comparator);

    @SuppressWarnings("unused")
    @Specialization(guards = {"cachedFromType.isInstance(fromType)", "cachedToType.isInstance(toType)", "cachedComparatorIsNull == comparatorIsNull"}, limit = "CACHE_LIMIT")
    protected final void doCached(JSTypedArrayObject fromArray, JSTypedArrayObject toArray, int length, Comparator<Object> comparator,
                    @Bind("fromArray.getArrayType()") TypedArray fromType,
                    @Bind("toArray.getArrayType()") TypedArray toType,
                    @Bind("comparator == null") boolean comparatorIsNull,
                    @Cached("fromType") TypedArray cachedFromType,
                    @Cached("toType") TypedArray cachedToType,
                    @Cached("comparatorIsNull") boolean cachedComparatorIsNull,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary interop) {
        doUncached(fromArray, toArray, length, comparator, cachedFromType, cachedToType, cachedComparatorIsNull, interop);
    }

    @Specialization(replaces = "doCached")
    protected final void doUncached(JSTypedArrayObject fromArray, JSTypedArrayObject toArray, int length, Comparator<Object> comparator,
                    @Bind("fromArray.getArrayType()") TypedArray fromType,
                    @Bind("toArray.getArrayType()") TypedArray toType,
                    @Bind("comparator == null") boolean comparatorIsNull,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary interop) {
        assert fromType.getElementType() == toType.getElementType() && fromArray.getArrayType() == fromType && toArray.getArrayType() == toType;
        assert JSArrayBufferView.typedArrayGetLength(fromArray) == length && JSArrayBufferView.typedArrayGetLength(toArray) == length;
        Comparator<Object> useComparator = comparatorIsNull ? null : Objects.requireNonNull(comparator);
        if (fromType instanceof TypedIntArray) {
            sortIntArray(fromArray, toArray, length, useComparator, (TypedIntArray) fromType, (TypedIntArray) toType, interop);
        } else if (fromType instanceof TypedFloatArray) {
            sortFloatArray(fromArray, toArray, length, useComparator, (TypedFloatArray) fromType, (TypedFloatArray) toType, interop);
        } else if (fromType instanceof TypedBigIntArray) {
            sortBigInt64Array(fromArray, toArray, length, useComparator, (TypedBigIntArray) fromType, (TypedBigIntArray) toType, interop);
        } else {
            throw Errors.shouldNotReachHereUnexpectedValue(fromType);
        }
        LoopNode.reportLoopCount(this, length);
    }

    private void sortIntArray(JSTypedArrayObject fromArray, JSTypedArrayObject toArray, int length, Comparator<Object> comparator,
                    TypedIntArray fromType, TypedIntArray toType, InteropLibrary interop) {
        if (comparator == null) {
            if (fromType instanceof AbstractUint32Array) {
                long[] arr = new long[length];
                for (int i = 0; i < length; i++) {
                    arr[i] = Integer.toUnsignedLong(fromType.getInt(fromArray, i, interop));
                }

                Boundaries.arraySort(arr);

                for (int i = 0; i < length; i++) {
                    toType.setInt(toArray, i, (int) arr[i], interop);
                }
            } else {
                int[] arr = new int[length];
                for (int i = 0; i < length; i++) {
                    arr[i] = fromType.getInt(fromArray, i, interop);
                }

                Boundaries.arraySort(arr);

                for (int i = 0; i < length; i++) {
                    toType.setInt(toArray, i, arr[i], interop);
                }
            }
        } else {
            boolean zeroExtend = fromType instanceof AbstractUint32Array;
            Object[] arr = new Object[length];
            for (int i = 0; i < length; i++) {
                int intValue = fromType.getInt(fromArray, i, interop);
                arr[i] = zeroExtend ? SafeInteger.valueOf(Integer.toUnsignedLong(intValue)) : intValue;
            }

            Boundaries.arraySort(arr, comparator);
            // Comparator or ToNumber(cmpResult) may have detached the buffer.
            if (isTypedArrayDetached(toArray)) {
                return;
            }

            for (int i = 0; i < length; i++) {
                Object value = arr[i];
                int intValue = zeroExtend ? ((SafeInteger) value).intValue() : (int) value;
                toType.setInt(toArray, i, intValue, interop);
            }
        }
    }

    private void sortFloatArray(JSTypedArrayObject fromArray, JSTypedArrayObject toArray, int length, Comparator<Object> comparator,
                    TypedFloatArray fromType, TypedFloatArray toType, InteropLibrary interop) {
        if (comparator == null) {
            double[] arr = new double[length];
            for (int i = 0; i < length; i++) {
                arr[i] = fromType.getDouble(fromArray, i, interop);
            }

            Boundaries.arraySort(arr);

            for (int i = 0; i < length; i++) {
                toType.setDouble(toArray, i, arr[i], interop);
            }
        } else {
            Double[] arr = new Double[length];
            for (int i = 0; i < length; i++) {
                arr[i] = fromType.getDouble(fromArray, i, interop);
            }

            Boundaries.arraySort(arr, comparator);
            // Comparator or ToNumber(cmpResult) may have detached the buffer.
            if (isTypedArrayDetached(toArray)) {
                return;
            }

            for (int i = 0; i < length; i++) {
                toType.setDouble(toArray, i, arr[i], interop);
            }
        }
    }

    private void sortBigInt64Array(JSTypedArrayObject fromArray, JSTypedArrayObject toArray, int length, Comparator<Object> comparator,
                    TypedBigIntArray fromType, TypedBigIntArray toType, InteropLibrary interop) {
        if (comparator == null && (fromType instanceof TypedArray.BigInt64Array ||
                        fromType instanceof TypedArray.DirectBigInt64Array ||
                        fromType instanceof TypedArray.InteropBigInt64Array)) {
            long[] arr = new long[length];
            for (int i = 0; i < length; i++) {
                arr[i] = fromType.getLong(fromArray, i, interop);
            }

            Boundaries.arraySort(arr);

            for (int i = 0; i < length; i++) {
                toType.setLong(toArray, i, arr[i], interop);
            }
        } else {
            BigInt[] arr = new BigInt[length];
            for (int i = 0; i < length; i++) {
                arr[i] = fromType.getBigInt(fromArray, i, interop);
            }

            Boundaries.arraySort(arr, comparator);
            // Comparator or ToNumber(cmpResult) may have detached the buffer.
            if (isTypedArrayDetached(toArray)) {
                return;
            }

            for (int i = 0; i < length; i++) {
                toType.setBigInt(toArray, i, arr[i], interop);
            }
        }
    }

    private boolean isTypedArrayDetached(JSTypedArrayObject toArray) {
        return JSArrayBufferView.hasDetachedBuffer(toArray, getJSContext());
    }
}
