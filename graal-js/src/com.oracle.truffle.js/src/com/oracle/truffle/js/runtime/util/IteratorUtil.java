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

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.runtime.Errors;

public final class IteratorUtil {

    private IteratorUtil() {
        // this utility class should not be instantiated
    }

    public static <T> List<T> concatLists(final List<T> list0, final List<T> list1) {
        final int size0 = list0.size();
        final int size1 = list1.size();
        final int size = size0 + size1;
        if (size < 0) {
            // int32 overflow
            throw Errors.createRangeErrorInvalidArrayLength();
        }
        if (size0 == 0) {
            return list1;
        } else if (size1 == 0) {
            return list0;
        }
        return new AbstractList<>() {
            @Override
            public T get(int index) {
                if (index >= 0 && index < size0) {
                    return list0.get(index);
                } else if (index >= 0 && index < size) {
                    return list1.get(index - size0);
                }
                throw outOfBounds(index);
            }

            @Override
            public int size() {
                return size;
            }

            @TruffleBoundary
            private IndexOutOfBoundsException outOfBounds(int index) {
                return new IndexOutOfBoundsException("Index: " + index + " Size: " + size());
            }
        };
    }

    /**
     * Like {@code Arrays.asList(array).iterator()}, but without concurrent modification checks.
     */
    public static <T> Iterator<T> simpleArrayIterator(T[] array) {
        return new Iterator<>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < array.length;
            }

            @Override
            public T next() {
                if (hasNext()) {
                    return array[cursor++];
                }
                throw new NoSuchElementException();
            }
        };
    }
}
