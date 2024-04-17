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
package com.oracle.truffle.js.runtime.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * View/wrapper of keys used by {@code ConcurrentWeakIdentityHashMap}.
 *
 * There are two style of keys; one for entries in the backing map (weak) and one for queries to the
 * backing map (strong). This second style avoids the overhead of a Reference object.
 *
 * Inspired by {@code jdk.internal.util.ReferenceKey}.
 *
 * @param <T> key type
 */
sealed interface ReferenceKey<T> {
    /**
     * Returns the value of the unwrapped key.
     */
    T get();

    /**
     * Clears the unused key.
     */
    void clear();

    /**
     * {@link ReferenceKey} with object identity comparison semantics ({@code ==}).
     */
    sealed interface WithIdentity<T> extends ReferenceKey<T> {
        /**
         * Tests if the referent of this reference object is {@code key}. Equivalent to:
         * {@code get() == key}.
         */
        boolean refersTo(T key);
    }

    final class WeakKeyWithIdentity<T> extends WeakReference<T> implements ReferenceKey.WithIdentity<T> {
        /**
         * Saved hash code of the key. Used when {@link WeakReference} is null.
         */
        private final int hashcode;

        WeakKeyWithIdentity(T key, ReferenceQueue<T> queue) {
            super(key, queue);
            this.hashcode = System.identityHashCode(Objects.requireNonNull(key, "key"));
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object obj) {
            // Necessary when removing a null (i.e. stale) reference
            if (obj == this) {
                return true;
            }
            // Note: ref.refersTo(key) is preferable to (ref.get() == key).
            T otherKey;
            if (obj instanceof ReferenceKey.WithIdentity<?> ref) {
                otherKey = (T) ref.get();
            } else {
                // Allows comparing against an unwrapped key
                otherKey = (T) obj;
            }
            return otherKey != null && refersTo(otherKey);
        }

        @Override
        public int hashCode() {
            return hashcode;
        }
    }

    final class StrongKeyWithIdentity<T> implements ReferenceKey.WithIdentity<T> {
        private final int hashcode;
        private T key;

        StrongKeyWithIdentity(T key) {
            this.hashcode = System.identityHashCode(Objects.requireNonNull(key, "key"));
            this.key = key;
        }

        @Override
        public T get() {
            return key;
        }

        @Override
        public void clear() {
            key = null;
        }

        @Override
        public boolean refersTo(T otherKey) {
            return get() == otherKey;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ReferenceKey.WithIdentity<?>) {
                // Note: ref.refersTo(key) is preferable to (ref.get() == key).
                return ((ReferenceKey.WithIdentity<T>) obj).refersTo(get());
            } else {
                // Allows comparing against an unwrapped key
                return refersTo((T) obj);
            }
        }

        @Override
        public int hashCode() {
            return hashcode;
        }
    }

    /**
     * {@link ReferenceKey} with equality comparison semantics ({@link Object#equals(Object)}).
     */
    sealed interface WithEquality<T> extends ReferenceKey<T> {
    }

    final class WeakKeyWithEquality<T> extends WeakReference<T> implements ReferenceKey.WithEquality<T> {
        /**
         * Saved hash code of the key. Used when {@link WeakReference} is null.
         */
        private final int hashcode;

        WeakKeyWithEquality(T key, ReferenceQueue<T> queue) {
            super(key, queue);
            this.hashcode = key.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            // Necessary when removing a null (i.e. stale) reference.
            if (obj == this) {
                return true;
            }
            // Note: refersTo is insufficient since keys require equivalence.
            Object otherKey;
            if (obj instanceof ReferenceKey.WithEquality<?> ref) {
                otherKey = ref.get();
            } else {
                // Allows comparing against an unwrapped key.
                otherKey = obj;
            }
            return otherKey != null && Objects.equals(get(), otherKey);
        }

        @Override
        public int hashCode() {
            return hashcode;
        }
    }

    final class StrongKeyWithEquality<T> implements ReferenceKey.WithEquality<T> {
        private final int hashcode;
        private T key;

        StrongKeyWithEquality(T key) {
            this.hashcode = key.hashCode();
            this.key = key;
        }

        @Override
        public T get() {
            return key;
        }

        @Override
        public void clear() {
            key = null;
        }

        @Override
        public boolean equals(Object obj) {
            Object otherKey;
            if (obj instanceof ReferenceKey.WithEquality<?> ref) {
                otherKey = ref.get();
            } else {
                // Allows comparing against an unwrapped key.
                otherKey = obj;
            }
            return Objects.equals(get(), otherKey);
        }

        @Override
        public int hashCode() {
            return hashcode;
        }
    }
}
