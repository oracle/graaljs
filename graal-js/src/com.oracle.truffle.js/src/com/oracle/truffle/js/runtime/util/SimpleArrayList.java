/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.util;

import java.util.Arrays;
import java.util.StringJoiner;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.JSConfig;

/**
 * A simple array-based quasi list. Prepared for use-cases in Graal/Truffle to avoid
 * TruffleBoundaries.
 */
public class SimpleArrayList<E> {

    private static final int MAX_ARRAY_SIZE = JSConfig.SOFT_MAX_ARRAY_LENGTH;
    private static final int DEFAULT_CAPACITY = 8;

    private Object[] elements;
    private int size;

    public SimpleArrayList() {
        // this does not have an EMPTY entry. This class is fit for use-cases where the list is
        // created and transfer to some other data structure rather immediately, not store it for
        // longer time. Might have slightly higher memory footprint, but avoids grow calls.
        this(DEFAULT_CAPACITY);
    }

    public SimpleArrayList(int capacity) {
        elements = capacity != 0 ? new Object[capacity] : ScriptArray.EMPTY_OBJECT_ARRAY;
    }

    public static <E> SimpleArrayList<E> create(long maxAssumedLength) {
        // simple heuristic: take provided assumption, but cap at limit.
        return new SimpleArrayList<>((int) Math.min(maxAssumedLength, 100));
    }

    public static <E> SimpleArrayList<E> createEmpty() {
        return new SimpleArrayList<>(0);
    }

    public void add(E e, Node node, InlinedBranchProfile growProfile) {
        ensureCapacity(size + 1, node, growProfile);
        elements[size++] = e;
    }

    public void addUncached(E e) {
        ensureCapacity(size + 1, null, InlinedBranchProfile.getUncached());
        elements[size++] = e;
    }

    public void addUnchecked(E e) {
        elements[size++] = e;
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        assert index < size : "out of bounds";
        return (E) elements[index];
    }

    public void set(int index, E elem) {
        assert index < size : "out of bounds";
        elements[index] = elem;
    }

    public Object pop() {
        assert size > 0;
        return elements[--size];
    }

    public int size() {
        return size;
    }

    public Object[] toArray() {
        return Arrays.copyOf(elements, size);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size) {
            return (T[]) Arrays.copyOf(elements, size, a.getClass());
        }
        System.arraycopy(elements, 0, a, 0, size);
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    private void ensureCapacity(int minCapacity, Node node, InlinedBranchProfile growProfile) {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, elements.length < minCapacity)) {
            growProfile.enter(node);
            ensureCapacityIntl(minCapacity);
        }
    }

    private void ensureCapacityIntl(int minCapacity) throws OutOfMemoryError {
        long curCapacity = elements.length;
        long newCapacity = curCapacity + (curCapacity >> 1L);
        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }
        if (newCapacity < DEFAULT_CAPACITY) {
            newCapacity = DEFAULT_CAPACITY;
        }
        if (newCapacity > MAX_ARRAY_SIZE) {
            if (MAX_ARRAY_SIZE < minCapacity) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.outOfMemoryError();
            }
            newCapacity = MAX_ARRAY_SIZE;
        }
        elements = Arrays.copyOf(elements, (int) newCapacity);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (Object element : elements) {
            sj.add(String.valueOf(element));
        }
        return sj.toString();
    }
}
