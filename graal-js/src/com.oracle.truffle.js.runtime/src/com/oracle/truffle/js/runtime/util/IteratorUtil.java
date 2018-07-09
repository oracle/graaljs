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
package com.oracle.truffle.js.runtime.util;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import com.oracle.truffle.api.CompilerDirectives;

public final class IteratorUtil {

    private IteratorUtil() {
        // this utility class should not be instantiated
    }

    public static <S, T> Iterable<T> convertIterable(final Iterable<S> source, final Function<S, T> converter) {
        return new ConvertIterable<>(source, converter);
    }

    public static <S, T> Iterator<T> convertIterator(final Iterator<S> source, final Function<S, T> converter) {
        return new ConvertIterator<>(source, converter);
    }

    private static final class ConvertIterable<S, T> implements Iterable<T> {
        private final Iterable<S> source;
        private final Function<S, T> converter;

        ConvertIterable(Iterable<S> source, Function<S, T> converter) {
            this.source = source;
            this.converter = converter;
        }

        @Override
        public Iterator<T> iterator() {
            return new ConvertIterator<>(source.iterator(), converter);
        }
    }

    private static final class ConvertIterator<S, T> implements Iterator<T> {
        private final Iterator<S> nested;
        private final Function<S, T> converter;

        ConvertIterator(Iterator<S> nested, Function<S, T> converter) {
            this.nested = nested;
            this.converter = converter;
        }

        @Override
        public T next() {
            return converter.apply(nested.next());
        }

        @Override
        public boolean hasNext() {
            return nested.hasNext();
        }
    }

    public static <T> Iterable<T> concatIterables(final Iterable<T> first, final Iterable<T> second) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private final Iterator<T> firstIterator = first.iterator();
                    private final Iterator<T> secondIterator = second.iterator();

                    @Override
                    public T next() {
                        if (firstIterator.hasNext()) {
                            return firstIterator.next();
                        } else if (secondIterator.hasNext()) {
                            return secondIterator.next();
                        }
                        CompilerDirectives.transferToInterpreter();
                        throw new NoSuchElementException();
                    }

                    @Override
                    public boolean hasNext() {
                        return firstIterator.hasNext() || secondIterator.hasNext();
                    }
                };
            }
        };
    }

    public static <T> Iterable<T> concatIterablesDistinct(final Iterable<T> first, final Iterable<T> second, final BiPredicate<T, T> comparator) {
        if (!second.iterator().hasNext()) {
            return first;
        }
        return new DistinctConcatIterable<>(first, second, comparator);
    }

    private static final class DistinctConcatIterable<T> implements Iterable<T> {
        private final Iterable<T> first;
        private final Iterable<T> second;
        private final BiPredicate<T, T> comparator;

        DistinctConcatIterable(Iterable<T> first, Iterable<T> second, BiPredicate<T, T> comparator) {
            this.first = first;
            this.second = second;
            this.comparator = comparator;
        }

        @Override
        public Iterator<T> iterator() {
            return new DistinctConcatIterator();
        }

        private final class DistinctConcatIterator implements Iterator<T> {
            private final Iterator<T> firstIterator = first.iterator();
            private final Iterator<T> secondIterator = second.iterator();
            private T next = forward();

            @Override
            public T next() {
                if (next != null) {
                    try {
                        return next;
                    } finally {
                        next = forward();
                    }
                }
                CompilerDirectives.transferToInterpreter();
                throw new NoSuchElementException();
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            private T forward() {
                if (firstIterator.hasNext()) {
                    return firstIterator.next();
                }
                next: while (secondIterator.hasNext()) {
                    T item = secondIterator.next();
                    for (T visitedItem : first) {
                        if (comparator.test(item, visitedItem)) {
                            continue next;
                        }
                    }
                    return item;
                }
                return null;
            }
        }
    }

    public static <T> Iterable<T> filterIterable(final Iterable<T> iterable, final Predicate<T> filter) {
        return () -> filterIterator(iterable.iterator(), filter);
    }

    public static <T> Iterator<T> filterIterator(final Iterator<T> iterator, final Predicate<T> filter) {
        return new FilteredIterator<>(iterator, filter);
    }

    private static final class FilteredIterator<T> implements Iterator<T> {
        private final Iterator<T> iterator;
        private final Predicate<T> filter;
        private T next;

        FilteredIterator(Iterator<T> iterator, Predicate<T> filter) {
            this.iterator = iterator;
            this.filter = filter;
            this.next = forward();
        }

        @Override
        public T next() {
            if (next != null) {
                try {
                    return next;
                } finally {
                    next = forward();
                }
            }
            CompilerDirectives.transferToInterpreter();
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        private T forward() {
            while (iterator.hasNext()) {
                T item = iterator.next();
                if (!filter.test(item)) {
                    continue;
                }
                return item;
            }
            return null;
        }
    }

    /**
     * Like {@code Arrays.asList(array).iterator()}, but without concurrent modification checks.
     */
    public static <T> Iterator<T> simpleArrayIterator(T[] array) {
        return new Iterator<T>() {
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

    /**
     * Like {@link AbstractList#iterator()}, but without concurrent modification checks.
     */
    public static <T> Iterator<T> simpleListIterator(List<T> list) {
        return new Iterator<T>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < list.size();
            }

            @Override
            public T next() {
                if (hasNext()) {
                    return list.get(cursor++);
                }
                throw new NoSuchElementException();
            }
        };
    }

    public static Iterator<Integer> rangeIterator(int length) {
        return new RangeIterator(length);
    }

    private static final class RangeIterator implements Iterator<Integer> {
        private final int length;
        private int index;

        RangeIterator(int length) {
            this.length = length;
        }

        @Override
        public boolean hasNext() {
            return index < length;
        }

        @Override
        public Integer next() {
            if (index < length) {
                return index++;
            }
            throw new NoSuchElementException();
        }
    }
}
