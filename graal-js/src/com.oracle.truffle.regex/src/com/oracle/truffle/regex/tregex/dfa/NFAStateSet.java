/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.dfa;

import com.oracle.truffle.regex.tregex.automaton.StateSet;
import com.oracle.truffle.regex.tregex.nfa.NFA;
import com.oracle.truffle.regex.tregex.nfa.NFAState;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.Arrays;
import java.util.PrimitiveIterator;

public final class NFAStateSet extends StateSet<NFAState> {

    private int[] stateIndexMap;

    public NFAStateSet(NFA nfa) {
        super(nfa);
    }

    public NFAStateSet(NFA nfa, NFAState state) {
        super(nfa);
        add(state);
    }

    private NFAStateSet(NFAStateSet copy) {
        super(copy);
        this.stateIndexMap = copy.isStateIndexMapCreated() ? Arrays.copyOf(copy.stateIndexMap, copy.stateIndexMap.length) : null;
    }

    @Override
    public NFAStateSet copy() {
        return new NFAStateSet(this);
    }

    private boolean isStateIndexMapCreated() {
        return stateIndexMap != null;
    }

    @Override
    public boolean add(NFAState state) {
        if (isStateIndexMapCreated()) {
            throw new IllegalStateException("state set must not be altered after state index map creation!");
        }
        return super.add(state);
    }

    private void createStateIndexMap() {
        if (!isStateIndexMapCreated()) {
            stateIndexMap = new int[size()];
            int i = 0;
            PrimitiveIterator.OfInt iterator = intIterator();
            while (iterator.hasNext()) {
                stateIndexMap[i++] = iterator.nextInt();
            }
            assert isSorted(stateIndexMap);
        }
    }

    private static boolean isSorted(int[] array) {
        int prev = Integer.MIN_VALUE;
        for (int i : array) {
            if (prev > i) {
                return false;
            }
            prev = i;
        }
        return true;
    }

    public int getStateIndex(NFAState state) {
        createStateIndexMap();
        assert contains(state);
        return Arrays.binarySearch(stateIndexMap, 0, size(), state.getId());
    }

    public DebugUtil.Table toTable(String name) {
        DebugUtil.Table table = new DebugUtil.Table(name);
        for (NFAState state : this) {
            table.append(state.toTable());
        }
        return table;
    }
}
