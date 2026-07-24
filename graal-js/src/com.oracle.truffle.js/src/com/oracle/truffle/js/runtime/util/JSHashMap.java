/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * ES6-compliant hash map implementation. A single node links each entry into both a hash bucket and
 * the insertion-order list. Removed nodes retain their order links so that live cursors can recover
 * from removal and clear operations.
 */
public final class JSHashMap {
    private static final int LINEAR_LOOKUP_THRESHOLD = 4;
    // Both table capacities must be powers of two because bucket indices use a bit mask.
    private static final int INITIAL_TABLE_CAPACITY = 8;
    private static final int MAXIMUM_TABLE_CAPACITY = 1 << 30;
    /** The 3/4 load threshold of the largest supported power-of-two bucket table. */
    private static final int MAX_ELEMENT_COUNT = MAXIMUM_TABLE_CAPACITY - (MAXIMUM_TABLE_CAPACITY >> 2);

    static {
        assert Integer.bitCount(INITIAL_TABLE_CAPACITY) == 1;
        assert Integer.bitCount(MAXIMUM_TABLE_CAPACITY) == 1;
    }

    public interface Cursor {
        /**
         * Advances to the next entry.
         *
         * @return {@code true} if a next entry exists, {@code false} if there is no next entry.
         */
        boolean advance();

        /**
         * Determines whether the current entry is valid.
         *
         * @return {@code true} if {@code advance()} has not been called yet or if the current entry
         *         is not valid anymore (i.e. has been removed), returns {@code false} otherwise.
         */
        boolean shouldAdvance();

        /**
         * The key of the current entry.
         */
        Object getKey();

        /**
         * The value of the current entry.
         */
        Object getValue();

        /**
         * Copies the cursor (including the current state/entry).
         */
        Cursor copy();
    }

    private int size;
    private Node[] table;
    private Node head;
    private Node tail;

    public JSHashMap() {
    }

    public int size() {
        return size;
    }

    /**
     * Insert new entry, if key does not already exist, otherwise update the existing entry's value.
     */
    @TruffleBoundary
    public void put(Object key, Object value) {
        assert key != null && value != null;
        int hash = getHash(key);
        Node node = find(key, hash);
        if (node == null) {
            putNewEntry(key, value, hash);
        } else {
            node.value = value;
        }
    }

    /**
     * Inserts a new entry if the key is absent.
     *
     * @return the existing value, or {@code null} if the new entry was inserted
     */
    @TruffleBoundary
    public Object putIfAbsent(Object key, Object value) {
        assert key != null && value != null;
        int hash = getHash(key);
        Node node = find(key, hash);
        if (node == null) {
            putNewEntry(key, value, hash);
            return null;
        }
        return node.value;
    }

    @TruffleBoundary
    public Object getOrInsert(Object key, Object value) {
        assert key != null && value != null;
        int hash = getHash(key);
        Node node = find(key, hash);
        if (node == null) {
            putNewEntry(key, value, hash);
            return value;
        }
        return node.value;
    }

    private void putNewEntry(Object key, Object value, int hash) {
        if (size == MAX_ELEMENT_COUNT) {
            throw capacityExceededException();
        }
        ensureCapacityForInsertion(size + 1);
        Node node = new Node(hash, key, value, tail);
        if (table != null) {
            int bucketIndex = hash & (table.length - 1);
            node.nextBucket = table[bucketIndex];
            table[bucketIndex] = node;
        }
        if (tail == null) {
            head = node;
        } else {
            tail.nextOrder = node;
        }
        tail = node;
        size++;
    }

    /**
     * Allocates or grows the bucket table before publishing a new entry.
     */
    private void ensureCapacityForInsertion(int newSize) {
        if (table == null) {
            if (newSize > LINEAR_LOOKUP_THRESHOLD) {
                rebuildTable(INITIAL_TABLE_CAPACITY);
            }
        } else if (newSize > table.length - (table.length >> 2)) {
            if (table.length == MAXIMUM_TABLE_CAPACITY) {
                throw capacityExceededException();
            }
            rebuildTable(table.length << 1);
        }
    }

    private static RuntimeException capacityExceededException() {
        return new IllegalStateException("maximum size exceeded");
    }

    /**
     * Rebuilds bucket chains using cached hashes and therefore cannot invoke user callbacks.
     */
    private void rebuildTable(int newCapacity) {
        Node[] newTable = new Node[newCapacity];
        int mask = newCapacity - 1;
        for (Node node = head; node != null; node = node.nextOrder) {
            int bucketIndex = node.hash & mask;
            node.nextBucket = newTable[bucketIndex];
            newTable[bucketIndex] = node;
        }
        table = newTable;
    }

    @TruffleBoundary
    public Object get(Object key) {
        assert key != null;
        int hash = getHash(key);
        Node node = find(key, hash);
        return node == null ? null : node.value;
    }

    @TruffleBoundary
    public Object getOrDefault(Object key, Object defaultValue) {
        assert key != null;
        int hash = getHash(key);
        Node node = find(key, hash);
        return node == null ? defaultValue : node.value;
    }

    @TruffleBoundary
    public boolean has(Object key) {
        assert key != null;
        int hash = getHash(key);
        return find(key, hash) != null;
    }

    @TruffleBoundary
    public boolean remove(Object key) {
        assert key != null;
        int hash = getHash(key);
        Node node = find(key, hash);
        if (node == null) {
            return false;
        }
        if (table != null) {
            unlinkBucket(node);
        }
        unlinkOrder(node);
        node.key = null;
        node.value = null;
        node.nextBucket = null;
        node.nextOrder = null;
        size--;
        if (table != null && size <= LINEAR_LOOKUP_THRESHOLD) {
            table = null;
            for (Node current = head; current != null; current = current.nextOrder) {
                current.nextBucket = null;
            }
        }
        return true;
    }

    private void unlinkBucket(Node removed) {
        int bucketIndex = removed.hash & (table.length - 1);
        Node previous = null;
        Node node = table[bucketIndex];
        while (node != removed) {
            assert node != null;
            previous = node;
            node = node.nextBucket;
        }
        if (previous == null) {
            table[bucketIndex] = removed.nextBucket;
        } else {
            previous.nextBucket = removed.nextBucket;
        }
    }

    private void unlinkOrder(Node removed) {
        Node previous = removed.prevOrder;
        Node next = removed.nextOrder;
        if (previous == null) {
            head = next;
        } else {
            previous.nextOrder = next;
        }
        if (next == null) {
            tail = previous;
        } else {
            next.prevOrder = previous;
        }
    }

    @TruffleBoundary
    public void clear() {
        if (size == 0) {
            return;
        }
        Node oldHead = head;
        table = null;
        head = null;
        tail = null;
        size = 0;
        for (Node node = oldHead; node != null;) {
            Node next = node.nextOrder;
            node.key = null;
            node.value = null;
            node.nextBucket = null;
            node.nextOrder = null;
            node = next;
        }
    }

    @TruffleBoundary
    @Override
    public String toString() {
        if (size == 0) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        String separator = "";
        for (Node node = head; node != null; node = node.nextOrder) {
            sb.append(separator).append(node.key).append('=').append(node.value);
            separator = ", ";
        }
        return sb.append('}').toString();
    }

    public Cursor getEntries() {
        return new CursorImpl(this);
    }

    @TruffleBoundary
    public JSHashMap copy() {
        JSHashMap result = new JSHashMap();
        for (Node node = head; node != null; node = node.nextOrder) {
            result.put(node.key, node.value);
        }
        return result;
    }

    private Node find(Object key, int hash) {
        if (table == null) {
            return findLinear(key, hash);
        }
        for (Node node = table[hash & (table.length - 1)]; node != null; node = node.nextBucket) {
            if (node.hash == hash && compareKeys(key, node.key)) {
                return node;
            }
        }
        return null;
    }

    private Node findLinear(Object key, int hash) {
        for (Node node = head; node != null; node = node.nextOrder) {
            if (node.hash == hash && compareKeys(key, node.key)) {
                return node;
            }
        }
        return null;
    }

    private static boolean compareKeys(Object key, Object entryKey) {
        return key == entryKey || key.equals(entryKey);
    }

    private static int getHash(Object key) {
        int hash = key.hashCode();
        return hash ^ (hash >>> 16);
    }

    private static final class Node {
        private final int hash;
        private Object key;
        private Object value;
        private Node nextBucket;
        private Node prevOrder;
        private Node nextOrder;

        private Node(int hash, Object key, Object value, Node prevOrder) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.prevOrder = prevOrder;
        }

        @Override
        public String toString() {
            return "Node [key=" + key + ", value=" + value + "]";
        }
    }

    private static final class CursorImpl implements Cursor {
        /** {@code null} after exhaustion. */
        private JSHashMap owner;
        private Node current;

        private CursorImpl(JSHashMap owner) {
            this.owner = owner;
        }

        private CursorImpl(JSHashMap owner, Node current) {
            this.owner = owner;
            this.current = current;
        }

        @Override
        public boolean advance() {
            if (owner == null) {
                return false;
            }
            Node next;
            if (current == null) {
                next = owner.head;
            } else {
                Node rewound = current;
                while (rewound.key == null && rewound.prevOrder != null) {
                    rewound = rewound.prevOrder;
                }
                next = rewound.key == null ? owner.head : rewound.nextOrder;
            }
            if (next == null) {
                current = null;
                owner = null;
                return false;
            }
            assert next.key != null;
            current = next;
            return true;
        }

        @Override
        public boolean shouldAdvance() {
            return owner != null && (current == null || current.key == null);
        }

        @Override
        public Object getKey() {
            assert current != null && current.key != null;
            return current.key;
        }

        @Override
        public Object getValue() {
            assert current != null && current.value != null;
            return current.value;
        }

        @Override
        public Cursor copy() {
            return new CursorImpl(owner, current);
        }

        @Override
        public String toString() {
            return "Cursor [current=" + current + "]";
        }
    }
}
