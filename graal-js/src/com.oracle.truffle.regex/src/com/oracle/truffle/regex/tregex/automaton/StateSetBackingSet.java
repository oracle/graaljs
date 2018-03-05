/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.automaton;

import java.util.PrimitiveIterator;

/**
 * A set of short values to be used as backing set by {@link StateSet}.
 */
public interface StateSetBackingSet extends Iterable<Integer> {

    /**
     * @return a deep copy of this set.
     */
    StateSetBackingSet copy();

    /**
     * Initialize the backing set. This method must be called before calling any other method of the
     * set. After this method is called, {@link #isActive()} will return {@code true}.
     * 
     * @param stateIndexSize the maximum {@code short} value to be expected.
     */
    void create(int stateIndexSize);

    /**
     * @return {@code true} if the set was initialized by {@link #create(int)}.
     */
    boolean isActive();

    /**
     * Analogous to {@link java.util.Set#contains(Object)}.
     */
    boolean contains(short id);

    /**
     * Analogous to {@link java.util.Set#add(Object)}.
     */
    boolean add(short id);

    /**
     * Add a value in batch mode. After calling this method, the set is in "add batch mode" until
     * {@link #addBatchFinish()} is called. The purpose of this is to allow more efficient add
     * operations that temporarily break the set's consistency. While this mode is active, other
     * methods are not guaranteed to work correctly!
     * 
     * @param id the id to add to the set.
     */
    void addBatch(short id);

    /**
     * Stop "add batch mode". This method will restore the set to a sane state after a batch add
     * operation.
     */
    void addBatchFinish();

    /**
     * Efficient version of {@code remove(oldId); add(newId)}. This method assumes that the set
     * contains {@code oldId} and the set does not contain {@code newId}!
     * 
     * @param oldId id to remove. Assumed to be contained in the set.
     * @param newId id to add. Assumed to be absent in the set.
     */
    void replace(short oldId, short newId);

    /**
     * Analogous to {@link java.util.Set#remove(Object)}.
     */
    boolean remove(short id);

    /**
     * Analogous to {@link java.util.Set#clear()} .
     */
    void clear();

    /**
     * Check whether this set is disjoint to another {@link StateSetBackingSet}.
     * 
     * @param other the other backing set.
     * @return {@code true} if this set is disjoint to the other set.
     */
    boolean isDisjoint(StateSetBackingSet other);

    @Override
    PrimitiveIterator.OfInt iterator();
}
