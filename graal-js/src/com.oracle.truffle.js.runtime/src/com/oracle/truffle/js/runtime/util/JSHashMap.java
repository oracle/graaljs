/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

    private final HashMap<Object, Node> map = new HashMap<>();
    private final Node head = new Node(null, null, null, null);
    private Node tail = head;

    public JSHashMap() {
    }

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
