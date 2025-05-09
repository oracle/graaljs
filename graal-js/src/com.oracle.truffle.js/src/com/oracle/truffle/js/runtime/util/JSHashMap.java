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
package com.oracle.truffle.js.runtime.util;

import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * ES6-compliant hash map implementation.
 */
public final class JSHashMap {
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

    private final HashMap<Object, Node> map;
    private final Node head;
    private Node tail;

    @TruffleBoundary(allowInlining = true)
    public JSHashMap() {
        this.map = new HashMap<>();
        Node dummy = new Node(null, null, null, null);
        this.head = dummy;
        this.tail = dummy;
    }

    @TruffleBoundary(allowInlining = true)
    public int size() {
        return map.size();
    }

    /**
     * Insert new entry, if key does not already exist, otherwise update the existing entry's value.
     */
    @TruffleBoundary
    public void put(Object key, Object value) {
        Node newNode = new Node(key, value, null, null);
        Node oldNode = map.putIfAbsent(key, newNode);
        if (oldNode == null) {
            newNode.setPrev(tail);
            tail.setNext(newNode);
            tail = newNode;
        } else {
            oldNode.setValue(value);
        }
    }

    @TruffleBoundary
    public Object getOrInsert(Object key, Object value) {
        Node newNode = new Node(key, value, null, null);
        Node oldNode = map.putIfAbsent(key, newNode);
        if (oldNode == null) {
            newNode.setPrev(tail);
            tail.setNext(newNode);
            tail = newNode;
            return value;
        } else {
            return oldNode.getValue();
        }
    }

    @TruffleBoundary
    public Object get(Object key) {
        Node node = map.get(key);
        return node == null ? null : node.getValue();
    }

    @TruffleBoundary
    public boolean has(Object key) {
        return map.containsKey(key);
    }

    @TruffleBoundary
    public boolean remove(Object key) {
        Node node = map.remove(key);
        if (node == null) {
            return false;
        } else {
            unlink(node);
            return true;
        }
    }

    private void unlink(Node node) {
        Node next = node.getNext();
        Node prev = node.getPrev();
        prev.setNext(next);
        if (next != null) {
            next.setPrev(prev);
        } else {
            tail = prev;
        }
        node.setEmpty();
    }

    @TruffleBoundary
    public void clear() {
        map.clear();
        for (Node current = head.getNext(); current != null; current = current.getNext()) {
            current.setEmpty();
        }
        head.setNext(null);
        tail = head;
    }

    @TruffleBoundary
    @Override
    public String toString() {
        return map.toString();
    }

    public Cursor getEntries() {
        return new CursorImpl(head);
    }

    @TruffleBoundary
    public JSHashMap copy() {
        JSHashMap result = new JSHashMap();
        JSHashMap.Cursor cursor = getEntries();
        while (cursor.advance()) {
            result.put(cursor.getKey(), cursor.getValue());
        }
        return result;
    }

    private static final class CursorImpl implements Cursor {
        private Node current;

        CursorImpl(Node head) {
            this.current = head;
        }

        @Override
        public boolean advance() {
            if (current == null) {
                return false;
            } else {
                // if current is no longer in the map, back up to a previous node still in the map
                while (current.isEmpty() && current.getPrev() != null) {
                    current = current.getPrev();
                }
                Node next = current.getNext();
                assert next == null || next.getKey() != null;
                current = next;
                return next != null;
            }
        }

        @Override
        public boolean shouldAdvance() {
            return current != null && current.isEmpty();
        }

        @Override
        public Object getKey() {
            Object key = current.getKey();
            assert key != null;
            return key;
        }

        @Override
        public Object getValue() {
            Object value = current.getValue();
            assert value != null;
            return value;
        }

        @Override
        public String toString() {
            return "Cursor [current=" + current + "]";
        }

        @Override
        public Cursor copy() {
            return new CursorImpl(current);
        }
    }

    private static final class Node {
        private Object key;
        private Object value;
        private Node prev;
        private Node next;

        Node(Object key, Object value, Node prev, Node next) {
            this.key = key;
            this.value = value;
            this.prev = prev;
            this.next = next;
        }

        Object getKey() {
            return key;
        }

        Object getValue() {
            return value;
        }

        void setValue(Object value) {
            this.value = value;
        }

        Node getPrev() {
            return prev;
        }

        void setPrev(Node prev) {
            this.prev = prev;
        }

        Node getNext() {
            return next;
        }

        void setNext(Node next) {
            this.next = next;
        }

        void setEmpty() {
            this.key = null;
            this.value = null;
        }

        boolean isEmpty() {
            return this.key == null;
        }

        @Override
        public String toString() {
            return "Node [key=" + key + ", value=" + value + "]";
        }
    }
}
