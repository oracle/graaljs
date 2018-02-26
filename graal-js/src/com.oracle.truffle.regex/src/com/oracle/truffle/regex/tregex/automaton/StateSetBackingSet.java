/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.automaton;

import java.util.PrimitiveIterator;

public interface StateSetBackingSet extends Iterable<Integer> {

    StateSetBackingSet copy();

    void create(int stateIndexSize);

    boolean isActive();

    boolean contains(short id);

    boolean add(short id);

    void addBatch(short id);

    void addBatchFinish();

    void replace(short oldId, short newId);

    boolean remove(short id);

    void clear();

    boolean isDisjoint(StateSetBackingSet other);

    @Override
    PrimitiveIterator.OfInt iterator();
}
