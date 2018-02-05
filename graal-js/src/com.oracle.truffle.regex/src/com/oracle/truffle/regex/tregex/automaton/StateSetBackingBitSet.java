/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.automaton;

import com.oracle.truffle.regex.util.CompilationFinalBitSet;

import java.util.PrimitiveIterator;

public class StateSetBackingBitSet implements StateSetBackingSet {

    private CompilationFinalBitSet bitSet;

    public StateSetBackingBitSet() {
    }

    private StateSetBackingBitSet(StateSetBackingBitSet copy) {
        bitSet = copy.bitSet == null ? null : copy.bitSet.copy();
    }

    @Override
    public StateSetBackingSet copy() {
        return new StateSetBackingBitSet(this);
    }

    @Override
    public void create(StateIndex stateIndex) {
        assert bitSet == null;
        bitSet = new CompilationFinalBitSet(stateIndex.getNumberOfStates());
    }

    @Override
    public boolean isActive() {
        return bitSet != null;
    }

    @Override
    public boolean contains(short id) {
        return bitSet.get(id);
    }

    @Override
    public boolean add(short id) {
        if (bitSet.get(id)) {
            return false;
        }
        bitSet.set(id);
        return true;
    }

    @Override
    public void addBatch(short id) {
        bitSet.set(id);
    }

    @Override
    public void addBatchFinish() {
    }

    @Override
    public void replace(short oldId, short newId) {
        bitSet.clear(oldId);
        bitSet.set(newId);
    }

    @Override
    public boolean remove(short id) {
        if (bitSet.get(id)) {
            bitSet.clear(id);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        bitSet.clear();
    }

    @Override
    public boolean isDisjoint(StateSetBackingSet other) {
        return bitSet.isDisjoint(((StateSetBackingBitSet) other).bitSet);
    }

    @Override
    public PrimitiveIterator.OfInt iterator() {
        return bitSet.iterator();
    }

    @Override
    public int hashCode() {
        return bitSet == null ? 0 : bitSet.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof StateSetBackingBitSet && bitSet.equals(((StateSetBackingBitSet) obj).bitSet);
    }
}
