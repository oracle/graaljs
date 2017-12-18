/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.automaton;

import com.oracle.truffle.regex.util.CompilationFinalBitSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StateSet<S extends IndexedState> implements Set<S>, Iterable<S> {

    private static final int SWITCH_TO_BITSET_THRESHOLD = 4;

    private static final int FLAG_STATE_LIST_SORTED = 1 << 30;
    private static final int FLAG_HASH_COMPUTED = 1 << 29;

    private static final long SHORT_MASK = 0xffff;

    private final StateIndex<? super S> stateIndex;
    private CompilationFinalBitSet bitSet;
    private int size = 0;
    private long stateList = 0;
    private int cachedHash;

    public StateSet(StateIndex<? super S> stateIndex) {
        this.stateIndex = stateIndex;
    }

    protected StateSet(StateSet<S> copy) {
        this.stateIndex = copy.stateIndex;
        this.size = copy.size;
        this.bitSet = copy.useBitSet() ? copy.bitSet.copy() : null;
        this.stateList = copy.stateList;
        this.cachedHash = copy.cachedHash;
    }

    public StateSet<S> copy() {
        return new StateSet<>(this);
    }

    private void checkSwitchToBitSet(int newSize) {
        if (!useBitSet() && newSize > SWITCH_TO_BITSET_THRESHOLD) {
            bitSet = new CompilationFinalBitSet(stateIndex.getNumberOfStates());
            for (int i = 0; i < size(); i++) {
                bitSet.set(stateListElement(stateList));
                stateList >>>= Short.SIZE;
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        return size & (int) SHORT_MASK;
    }

    private boolean useBitSet() {
        return bitSet != null;
    }

    private void setFlag(int flag, boolean value) {
        if (value) {
            size |= flag;
        } else {
            size &= ~flag;
        }
    }

    private boolean isFlagSet(int flag) {
        return (size & flag) != 0;
    }

    private boolean isStateListSorted() {
        return isFlagSet(FLAG_STATE_LIST_SORTED);
    }

    private void setStateListSorted(boolean stateListSorted) {
        setFlag(FLAG_STATE_LIST_SORTED, stateListSorted);
    }

    private boolean isHashComputed() {
        return isFlagSet(FLAG_HASH_COMPUTED);
    }

    private void setHashComputed(boolean hashComputed) {
        setFlag(FLAG_HASH_COMPUTED, hashComputed);
    }

    private static int stateListElement(long stateList, int i) {
        return stateListElement(stateList >>> (Short.SIZE * i));
    }

    private static int stateListElement(long stateList) {
        return (int) (stateList & SHORT_MASK);
    }

    private void setStateListElement(int index, int value) {
        stateList = (stateList & ~(SHORT_MASK << (Short.SIZE * index))) | ((long) value << (Short.SIZE * index));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object state) {
        return contains(((S) state).getId());
    }

    private boolean contains(int index) {
        if (useBitSet()) {
            return bitSet.get(index);
        }
        long sl = stateList;
        for (int i = 0; i < size(); i++) {
            if (stateListElement(sl) == index) {
                return true;
            }
            sl >>>= Short.SIZE;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean add(S state) {
        return add(state.getId());
    }

    private boolean add(int index) {
        assert index >= 0 && index <= 0xffff;
        if (contains(index)) {
            return false;
        }
        setHashComputed(false);
        checkSwitchToBitSet(size() + 1);
        if (useBitSet()) {
            bitSet.set(index);
            size++;
        } else {
            stateList = (stateList << Short.SIZE) | index;
            size++;
            setStateListSorted(false);
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends S> c) {
        checkSwitchToBitSet(size() + c.size());
        boolean ret = false;
        for (S s : c) {
            ret |= add(s);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        return remove(((S) o).getId());
    }

    private boolean remove(int stateID) {
        if (useBitSet()) {
            if (bitSet.get(stateID)) {
                bitSet.clear(stateID);
                size--;
                setHashComputed(false);
                return true;
            }
        } else {
            long sl = stateList;
            for (int i = 0; i < size(); i++) {
                if (stateListElement(sl) == stateID) {
                    switch (i) {
                        case 0:
                            stateList = sl >>> Short.SIZE;
                            break;
                        case 1:
                            stateList = (sl & 0xffff_ffff_ffff_0000L) | (stateList & 0xffffL);
                            break;
                        case 2:
                            stateList = ((sl & 0xffff_ffff_ffff_0000L) << Short.SIZE) | (stateList & 0xffff_ffffL);
                            break;
                        case 3:
                            stateList &= 0xffff_ffff_ffffL;
                            break;
                    }
                    size--;
                    setHashComputed(false);
                    return true;
                }
                sl >>>= Short.SIZE;
            }
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean ret = false;
        for (Object s : c) {
            ret |= remove(s);
        }
        return ret;
    }

    @Override
    public void clear() {
        if (useBitSet()) {
            bitSet.clear();
        } else {
            stateList = 0;
        }
        size = 0;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean isDisjoint(StateSet<? extends S> other) {
        if (other.useBitSet()) {
            if (useBitSet()) {
                return bitSet.isDisjoint(other.bitSet);
            }
            long sl = stateList;
            for (int i = 0; i < size(); i++) {
                if (other.contains(stateListElement(sl))) {
                    return false;
                }
                sl >>>= Short.SIZE;
            }
            return true;
        }
        long sl = other.stateList;
        for (int i = 0; i < other.size(); i++) {
            if (contains(stateListElement(sl))) {
                return false;
            }
            sl >>>= Short.SIZE;
        }
        return true;
    }

    private void requireStateListSorted() {
        if (!isStateListSorted()) {
            for (int i = 1; i < size(); i++) {
                int sli = stateListElement(stateList, i);
                int j;
                for (j = i - 1; j >= 0; j--) {
                    int slj = stateListElement(stateList, j);
                    if (slj <= sli) {
                        break;
                    }
                    setStateListElement(j + 1, slj);
                }
                setStateListElement(j + 1, sli);
            }
            setStateListSorted(true);
        }
    }

    /**
     * Returns the hash code value for this set.
     * 
     * Note that unlike other {@link Set}s, the hash code value returned by this implementation is
     * <em>not</em> the sum of the hash codes of its elements.
     * 
     * @see Set#hashCode()
     */
    @Override
    public int hashCode() {
        if (!isHashComputed()) {
            if (useBitSet()) {
                if (size <= SWITCH_TO_BITSET_THRESHOLD) {
                    long hashStateList = 0;
                    int shift = 0;
                    for (int i : bitSet) {
                        hashStateList |= (long) i << shift;
                        shift += Short.SIZE;
                    }
                    cachedHash = Long.hashCode(hashStateList);
                } else {
                    cachedHash = bitSet.hashCode();
                }
            } else {
                if (size == 0) {
                    cachedHash = 0;
                } else {
                    requireStateListSorted();
                    if (size < SWITCH_TO_BITSET_THRESHOLD) {
                        // clear unused slots in stateList
                        stateList &= (~0L >>> (Short.SIZE * (SWITCH_TO_BITSET_THRESHOLD - size)));
                    }
                    cachedHash = Long.hashCode(stateList);
                }
            }
            setHashComputed(true);
        }
        return cachedHash;
    }

    /**
     * Compares the specified object with this set for equality.
     * 
     * Note that unlike other {@link Set} s, {@link StateSet}s are only equal to other
     * {@link StateSet}s.
     * 
     * @see Set#equals(Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StateSet)) {
            return false;
        }
        StateSet<S> o = (StateSet<S>) obj;
        if (size() != o.size()) {
            return false;
        }
        if (useBitSet() && o.useBitSet()) {
            return bitSet.equals(o.bitSet);
        }
        if (useBitSet() != o.useBitSet()) {
            PrimitiveIterator.OfInt thisIterator = intIterator();
            PrimitiveIterator.OfInt otherIterator = o.intIterator();
            while (thisIterator.hasNext()) {
                if (thisIterator.nextInt() != otherIterator.nextInt()) {
                    return false;
                }
            }
            return true;
        }
        assert !useBitSet() && !o.useBitSet();
        requireStateListSorted();
        o.requireStateListSorted();
        return stateList == o.stateList;
    }

    protected PrimitiveIterator.OfInt intIterator() {
        if (useBitSet()) {
            return bitSet.iterator();
        }
        requireStateListSorted();
        return new StateListIterator(stateList, size());
    }

    @Override
    public Iterator<S> iterator() {
        return new StateSetIterator(stateIndex, intIterator());
    }

    @Override
    public Object[] toArray() {
        Object[] ret = new Object[size()];
        int i = 0;
        for (S s : this) {
            ret[i] = s;
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        T[] r = a.length >= size() ? a : (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size());
        int i = 0;
        for (S s : this) {
            r[i] = (T) s;
        }
        return r;
    }

    private static final class StateListIterator implements PrimitiveIterator.OfInt {

        private long stateList;
        private int size;

        private StateListIterator(long stateList, int size) {
            this.stateList = stateList;
            this.size = size;
        }

        @Override
        public int nextInt() {
            assert size > 0;
            size--;
            int ret = stateListElement(stateList);
            stateList >>>= Short.SIZE;
            return ret;
        }

        @Override
        public boolean hasNext() {
            return size > 0;
        }
    }

    private final class StateSetIterator implements Iterator<S> {

        private final StateIndex<? super S> stateIndex;
        private final PrimitiveIterator.OfInt intIterator;
        private int lastID = -1;

        private StateSetIterator(StateIndex<? super S> stateIndex, PrimitiveIterator.OfInt intIterator) {
            this.stateIndex = stateIndex;
            this.intIterator = intIterator;
        }

        @Override
        public boolean hasNext() {
            return intIterator.hasNext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public S next() {
            lastID = intIterator.nextInt();
            return (S) stateIndex.getState(lastID);
        }

        @Override
        public void remove() {
            assert lastID >= 0;
            StateSet.this.remove(lastID);
        }
    }

    @Override
    public Stream<S> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    @Override
    public String toString() {
        return stream().map(S::toString).collect(Collectors.joining(",", "{", "}"));
    }
}
