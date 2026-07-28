/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.oracle.truffle.js.builtins.helper.JSCollectionsHashCodeNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.SuppressFBWarnings;
import com.oracle.truffle.js.runtime.util.JSHashMap;

public class JSHashMapTest {

    @Test
    public void testBasicOperations() {
        var map = newJSHashMap();
        assertEquals(0, map.size());
        assertFalse(map.has("key"));
        assertNull(map.get("key"));
        assertFalse(map.remove("key"));
        assertEquals("{}", map.toString());

        map.put("key", "value");
        assertEquals(1, map.size());
        assertTrue(map.has("key"));
        assertEquals("value", map.get("key"));

        map.put("key", "updated");
        assertEquals(1, map.size());
        assertEquals("updated", map.get("key"));
        assertEquals("updated", map.getOrInsert("key", "unused"));
        assertEquals("inserted", map.getOrInsert("new", "inserted"));
        assertEquals(2, map.size());

        assertTrue(map.remove("key"));
        assertFalse(map.has("key"));
        map.clear();
        assertEquals(0, map.size());
        assertFalse(map.has("new"));
        assertEquals("{}", map.toString());
    }

    @Test
    public void testCollectionHashCodeNode() {
        JSCollectionsHashCodeNode hashCodeNode = JSCollectionsHashCodeNode.getUncached();
        Object[] keys = {42, 1.5, Double.NaN, true, Strings.fromJavaString("string"), new Object()};
        for (Object key : keys) {
            assertEquals(key.hashCode(), hashCodeNode.execute(key));
        }
    }

    @Test
    public void testPrecomputedHashCode() {
        var map = newJSHashMap();
        CountingKey key = new CountingKey(42);
        int hashCode = key.hashCode();

        map.put(key, hashCode, "value");
        assertEquals("value", map.putIfAbsent(key, hashCode, "unused"));
        assertEquals("value", map.getOrInsert(key, hashCode, "unused"));
        assertEquals("value", map.getOrDefault(key, hashCode, null));
        assertTrue(map.has(key, hashCode));
        assertTrue(map.remove(key, hashCode));
        assertEquals(1, key.getHashCodeCalls());
    }

    @Test
    public void testPutIfAbsent() {
        var map = newJSHashMap();

        assertNull(map.putIfAbsent("existing", "first"));
        assertEquals("first", map.putIfAbsent("existing", "second"));
        assertEquals("first", map.get("existing"));
    }

    @Test
    public void testCursorSeesInsertionsUntilDone() {
        var map = newJSHashMap();
        JSHashMap.Cursor cursor = map.getEntries();
        assertTrue(cursor.shouldAdvance());

        map.put("first", 1);
        assertTrue(cursor.advance());
        assertEquals("first", cursor.getKey());
        assertEquals(1, cursor.getValue());
        assertFalse(cursor.shouldAdvance());

        map.put("second", 2);
        assertTrue(cursor.advance());
        assertEquals("second", cursor.getKey());
        assertFalse(cursor.advance());
        assertFalse(cursor.shouldAdvance());

        map.put("third", 3);
        assertFalse(cursor.advance());
    }

    @Test
    public void testCursorHandlesRemoval() {
        var map = newJSHashMap();
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        JSHashMap.Cursor cursor = map.getEntries();
        assertTrue(cursor.advance());
        assertEquals("first", cursor.getKey());
        map.remove("first");
        assertTrue(cursor.shouldAdvance());
        assertTrue(cursor.advance());
        assertEquals("second", cursor.getKey());

        map.remove("second");
        map.remove("third");
        map.put("fourth", 4);
        assertTrue(cursor.advance());
        assertEquals("fourth", cursor.getKey());
        assertFalse(cursor.advance());
    }

    @Test
    public void testCursorHandlesClear() {
        var map = newJSHashMap();
        map.put("first", 1);
        map.put("second", 2);

        JSHashMap.Cursor cursor = map.getEntries();
        assertTrue(cursor.advance());
        assertEquals("first", cursor.getKey());

        map.clear();
        map.put("third", 3);
        assertTrue(cursor.shouldAdvance());
        assertTrue(cursor.advance());
        assertEquals("third", cursor.getKey());
        assertFalse(cursor.advance());

        var emptyMap = newJSHashMap();
        JSHashMap.Cursor emptyCursor = emptyMap.getEntries();
        emptyMap.clear();
        emptyMap.put("new", 1);
        assertTrue(emptyCursor.advance());
        assertEquals("new", emptyCursor.getKey());
    }

    @Test
    public void testMultipleCursors() {
        var map = newJSHashMap();
        map.put("first", 1);
        map.put("second", 2);

        JSHashMap.Cursor firstCursor = map.getEntries();
        JSHashMap.Cursor secondCursor = map.getEntries();
        assertTrue(firstCursor.advance());
        assertTrue(secondCursor.advance());
        assertEquals("first", firstCursor.getKey());
        assertEquals("first", secondCursor.getKey());

        map.clear();
        map.put("third", 3);
        assertTrue(firstCursor.advance());
        assertTrue(secondCursor.advance());
        assertEquals("third", firstCursor.getKey());
        assertEquals("third", secondCursor.getKey());
    }

    @Test
    public void testHashTableCollisionsAndGrowth() {
        var map = newJSHashMap();
        CollisionKey[] keys = new CollisionKey[300];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = new CollisionKey(i);
            map.put(keys[i], i);
        }
        assertEquals(keys.length, map.size());

        for (int i = 0; i < keys.length; i++) {
            assertEquals(i, map.get(new CollisionKey(i)));
            assertTrue(map.has(new CollisionKey(i)));
        }
        for (int i = 0; i < keys.length; i += 3) {
            assertTrue(map.remove(new CollisionKey(i)));
        }
        for (int i = 1; i < keys.length; i += 3) {
            map.put(new CollisionKey(i), -i);
        }
        for (int i = 0; i < keys.length; i++) {
            if (i % 3 == 0) {
                assertFalse(map.has(new CollisionKey(i)));
            } else if (i % 3 == 1) {
                assertEquals(-i, map.get(new CollisionKey(i)));
            } else {
                assertEquals(i, map.get(new CollisionKey(i)));
            }
        }

        List<Integer> iterationOrder = new ArrayList<>();
        JSHashMap.Cursor cursor = map.getEntries();
        while (cursor.advance()) {
            iterationOrder.add(((CollisionKey) cursor.getKey()).id);
        }
        List<Integer> expectedOrder = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            if (i % 3 != 0) {
                expectedOrder.add(i);
            }
        }
        assertEquals(expectedOrder, iterationOrder);
    }

    @Test
    public void testCollisionChainRemovalAndReinsertion() {
        var map = newJSHashMap();
        CollisionKey[] keys = new CollisionKey[10];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = new CollisionKey(i);
            map.put(keys[i], i);
        }
        for (int i = 0; i < 3; i++) {
            assertTrue(map.remove(keys[i]));
        }

        for (int i = 3; i < keys.length; i++) {
            assertEquals(i, map.get(new CollisionKey(i)));
        }
        assertFalse(map.has(new CollisionKey(10)));

        map.put(new CollisionKey(1), 100);
        assertEquals(100, map.get(new CollisionKey(1)));
        List<Integer> expectedOrder = List.of(3, 4, 5, 6, 7, 8, 9, 1);
        List<Integer> iterationOrder = new ArrayList<>();
        JSHashMap.Cursor cursor = map.getEntries();
        while (cursor.advance()) {
            iterationOrder.add(((CollisionKey) cursor.getKey()).id);
        }
        assertEquals(expectedOrder, iterationOrder);
    }

    @Test
    public void testTableCreationUsesCachedHashes() {
        var map = newJSHashMap();
        ThrowingHashKey[] keys = new ThrowingHashKey[6];
        for (int i = 0; i < 4; i++) {
            keys[i] = new ThrowingHashKey(i);
            map.put(keys[i], i);
        }
        keys[2].setThrowOnHashCode(true);
        keys[4] = new ThrowingHashKey(4);
        map.put(keys[4], 4);

        keys[2].setThrowOnHashCode(false);
        for (int i = 0; i <= 4; i++) {
            assertEquals(i, map.get(keys[i]));
        }
        assertEquals(5, map.size());

        keys[5] = new ThrowingHashKey(5);
        map.put(keys[5], 5);
        for (int i = 0; i < keys.length; i++) {
            assertEquals(i, map.get(keys[i]));
        }
        assertEquals(6, map.size());
    }

    @Test
    public void testCopyUsesCachedHashes() {
        for (int entryCount : new int[]{0, 4, 12}) {
            var map = newJSHashMap();
            ThrowingHashKey[] keys = new ThrowingHashKey[entryCount];
            for (int i = 0; i < entryCount; i++) {
                keys[i] = new ThrowingHashKey(i);
                map.put(keys[i], i);
                keys[i].setThrowOnHashCode(true);
            }

            var copy = map.copy();
            assertEquals(entryCount, copy.size());

            JSHashMap.Cursor cursor = copy.getEntries();
            for (int i = 0; i < entryCount; i++) {
                keys[i].setThrowOnHashCode(false);
                assertEquals(i, copy.get(keys[i]));
                assertTrue(cursor.advance());
                assertEquals(keys[i], cursor.getKey());
            }
            assertFalse(cursor.advance());
            if (entryCount > 0) {
                assertTrue(map.remove(keys[0]));
                assertEquals(0, copy.get(keys[0]));
            }
        }
    }

    @Test
    public void testNewEntryIsHashedOnce() {
        var map = newJSHashMap();
        for (int i = 0; i < 5; i++) {
            map.put(i, i);
        }

        ThrowOnSecondHashKey key = new ThrowOnSecondHashKey(5);
        map.put(key, 5);
        assertEquals(1, key.getHashCodeCalls());
        assertThrows(IllegalStateException.class, () -> map.get(key));
        assertEquals(6, map.size());
        assertEquals(5, map.get(key));
        for (int i = 0; i < 5; i++) {
            assertEquals(i, map.get(i));
        }
    }

    @Test
    public void testFailedInsertionPreservesCursors() {
        var map = newJSHashMap();
        for (int i = 0; i < 12; i++) {
            map.put(i, i);
        }
        JSHashMap.Cursor survivingCursor = map.getEntries();
        JSHashMap.Cursor deletedCursor = map.getEntries();
        for (int i = 0; i <= 3; i++) {
            assertTrue(survivingCursor.advance());
        }
        for (int i = 0; i <= 4; i++) {
            assertTrue(deletedCursor.advance());
        }
        for (int i = 4; i < 12; i++) {
            assertTrue(map.remove(i));
        }

        ThrowingHashKey failedKey = new ThrowingHashKey(12);
        failedKey.setThrowOnHashCode(true);
        assertThrows(IllegalStateException.class, () -> map.put(failedKey, 12));
        assertEquals(4, map.size());

        ThrowingHashKey key = new ThrowingHashKey(12);
        map.put(key, 12);
        assertTrue(survivingCursor.advance());
        assertEquals(key, survivingCursor.getKey());
        assertTrue(deletedCursor.advance());
        assertEquals(key, deletedCursor.getKey());
    }

    @Test
    public void testFailedEqualityLeavesMapUnchanged() {
        var map = newJSHashMap();
        ThrowingEqualsKey stored = new ThrowingEqualsKey(1, false);
        map.put(stored, 1);

        ThrowingEqualsKey query = new ThrowingEqualsKey(2, true);
        assertThrows(IllegalStateException.class, () -> map.has(query));
        assertEquals(1, map.size());
        assertEquals(1, map.get(stored));
        JSHashMap.Cursor cursor = map.getEntries();
        assertTrue(cursor.advance());
        assertEquals(stored, cursor.getKey());
        assertFalse(cursor.advance());
    }

    @Test
    public void testLargeHashTable() {
        var map = newJSHashMap();
        int entryCount = 70_000;
        for (int i = 0; i < entryCount; i++) {
            map.put(i, -i);
        }
        assertEquals(entryCount, map.size());
        for (int i = 0; i < entryCount; i += 997) {
            assertEquals(-i, map.get(i));
        }
        for (int i = 0; i < entryCount; i += 3) {
            assertTrue(map.remove(i));
        }

        int iterated = 0;
        JSHashMap.Cursor cursor = map.getEntries();
        while (cursor.advance()) {
            assertTrue((int) cursor.getKey() % 3 != 0);
            iterated++;
        }
        assertEquals(entryCount - ((entryCount + 2) / 3), iterated);
    }

    @Test
    public void testCursorHandlesHashedClear() {
        var map = newJSHashMap();
        for (int i = 0; i < 20; i++) {
            map.put(i, i);
        }
        JSHashMap.Cursor cursor = map.getEntries();
        for (int i = 0; i < 5; i++) {
            assertTrue(cursor.advance());
            assertEquals(i, cursor.getKey());
        }

        map.clear();
        for (int i = 20; i < 40; i++) {
            map.put(i, i);
        }
        for (int i = 20; i < 40; i++) {
            assertTrue(cursor.advance());
            assertEquals(i, cursor.getKey());
        }
        assertFalse(cursor.advance());
    }

    @Test
    public void testCursorSurvivesEarlierRemovals() {
        var map = newJSHashMap();
        for (int i = 0; i < 20; i++) {
            map.put(i, i);
        }
        JSHashMap.Cursor cursor = map.getEntries();
        for (int i = 0; i <= 10; i++) {
            assertTrue(cursor.advance());
            assertEquals(i, cursor.getKey());
        }

        for (int i = 0; i < 20; i++) {
            if (i != 0 && i != 5 && i != 10 && i != 15 && i != 19) {
                assertTrue(map.remove(i));
            }
        }
        assertFalse(cursor.shouldAdvance());
        assertEquals(10, cursor.getKey());
        assertTrue(cursor.advance());
        assertEquals(15, cursor.getKey());
        assertTrue(cursor.advance());
        assertEquals(19, cursor.getKey());
        assertFalse(cursor.advance());
    }

    @Test
    public void testCursorRecoversAfterCurrentRemoval() {
        var map = newJSHashMap();
        for (int i = 0; i < 20; i++) {
            map.put(i, i);
        }
        JSHashMap.Cursor cursor = map.getEntries();
        for (int i = 0; i <= 10; i++) {
            assertTrue(cursor.advance());
            assertEquals(i, cursor.getKey());
        }

        for (int i = 0; i < 20; i++) {
            if (i != 0 && i != 5 && i != 10 && i != 11 && i != 15 && i != 19) {
                assertTrue(map.remove(i));
            }
        }
        assertTrue(map.remove(10));
        map.put(10, 10);

        assertTrue(cursor.shouldAdvance());
        int[] expected = {11, 15, 19, 10};
        for (int key : expected) {
            assertTrue(cursor.advance());
            assertEquals(key, cursor.getKey());
        }
        assertFalse(cursor.advance());
    }

    @Test
    public void testCursorsRecoverAcrossRemovalChains() {
        var map = newJSHashMap();
        for (int i = 0; i < 40; i++) {
            map.put(i, i);
        }
        JSHashMap.Cursor cursor = map.getEntries();
        assertTrue(cursor.advance());
        assertEquals(0, cursor.getKey());
        JSHashMap.Cursor copy = cursor.copy();
        JSHashMap.Cursor deletedCursor = map.getEntries();
        for (int i = 0; i <= 10; i++) {
            assertTrue(deletedCursor.advance());
        }
        JSHashMap.Cursor deletedCopy = deletedCursor.copy();

        for (int i = 1; i <= 30; i++) {
            assertTrue(map.remove(i));
        }
        for (int i = 40; i < 70; i++) {
            map.put(i, i);
        }
        for (int i = 31; i <= 60; i++) {
            assertTrue(map.remove(i));
        }

        for (JSHashMap.Cursor current : new JSHashMap.Cursor[]{cursor, copy}) {
            assertFalse(current.shouldAdvance());
            assertEquals(0, current.getKey());
            for (int i = 61; i < 70; i++) {
                assertTrue(current.advance());
                assertEquals(i, current.getKey());
            }
            assertFalse(current.advance());
        }
        for (JSHashMap.Cursor current : new JSHashMap.Cursor[]{deletedCursor, deletedCopy}) {
            assertTrue(current.shouldAdvance());
            for (int i = 61; i < 70; i++) {
                assertTrue(current.advance());
                assertEquals(i, current.getKey());
            }
            assertFalse(current.advance());
        }
    }

    @Test
    public void testInsertionAfterCurrentRemoval() {
        var map = newJSHashMap();
        for (int i = 0; i < 4; i++) {
            map.put(i, i);
        }
        JSHashMap.Cursor cursor = map.getEntries();
        assertTrue(cursor.advance());
        assertEquals(0, cursor.getKey());

        assertTrue(map.remove(0));
        map.put(4, 4);
        assertTrue(cursor.shouldAdvance());
        for (int i = 1; i <= 4; i++) {
            assertTrue(cursor.advance());
            assertEquals(i, cursor.getKey());
        }
        assertFalse(cursor.advance());
    }

    @Test
    public void testRemovalUsesCachedHashes() {
        var map = newJSHashMap();
        CountingKey[] keys = new CountingKey[13];
        for (int i = 0; i < 12; i++) {
            keys[i] = new CountingKey(i);
            map.put(keys[i], i);
        }
        for (int i = 0; i < 7; i++) {
            assertTrue(map.remove(keys[i]));
        }
        for (CountingKey key : keys) {
            if (key != null) {
                key.resetHashCodeCalls();
            }
        }

        keys[12] = new CountingKey(12);
        map.put(keys[12], 12);
        for (int i = 7; i < 12; i++) {
            assertEquals(0, keys[i].getHashCodeCalls());
        }

        JSHashMap.Cursor cursor = map.getEntries();
        for (int i = 7; i <= 12; i++) {
            assertTrue(cursor.advance());
            assertEquals(keys[i], cursor.getKey());
        }
        assertFalse(cursor.advance());
    }

    @Test
    public void testResizeUsesCachedHashes() {
        var map = newJSHashMap();
        CountingKey[] keys = new CountingKey[13];
        for (int i = 0; i < 12; i++) {
            keys[i] = new CountingKey(i);
            map.put(keys[i], i);
            keys[i].resetHashCodeCalls();
        }

        keys[12] = new CountingKey(12);
        map.put(keys[12], 12);
        for (int i = 0; i < 12; i++) {
            assertEquals(0, keys[i].getHashCodeCalls());
        }
        assertEquals(1, keys[12].getHashCodeCalls());
    }

    @Test
    public void testShrinkUsesCachedHashesAndPreservesOrder() {
        var map = newJSHashMap();
        CountingKey[] keys = new CountingKey[48];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = new CountingKey(i);
            map.put(keys[i], i);
            keys[i].resetHashCodeCalls();
        }

        for (int i = 0; i <= 32; i++) {
            assertTrue(map.remove(keys[i]));
        }
        assertEquals(15, map.size());
        for (int i = 33; i < keys.length; i++) {
            assertEquals(0, keys[i].getHashCodeCalls());
        }

        JSHashMap.Cursor cursor = map.getEntries();
        for (int i = 33; i < keys.length; i++) {
            assertTrue(cursor.advance());
            assertEquals(keys[i], cursor.getKey());
            assertEquals(i, cursor.getValue());
        }
        assertFalse(cursor.advance());
    }

    @Test
    public void testRemovalPreservesCursorsAndCachedHashes() {
        var map = newJSHashMap();
        CountingKey[] keys = new CountingKey[13];
        for (int i = 0; i < 12; i++) {
            keys[i] = new CountingKey(i);
            map.put(keys[i], i);
        }
        JSHashMap.Cursor deletedCursor = map.getEntries();
        for (int i = 0; i <= 3; i++) {
            assertTrue(deletedCursor.advance());
        }
        JSHashMap.Cursor survivingCursor = map.getEntries();
        for (int i = 0; i <= 8; i++) {
            assertTrue(survivingCursor.advance());
        }

        for (int i = 0; i < 8; i++) {
            assertTrue(map.remove(keys[i]));
        }
        for (CountingKey key : keys) {
            if (key != null) {
                key.resetHashCodeCalls();
            }
        }

        keys[12] = new CountingKey(12);
        map.put(keys[12], 12);
        for (int i = 8; i < 12; i++) {
            assertEquals(0, keys[i].getHashCodeCalls());
        }

        assertTrue(deletedCursor.shouldAdvance());
        for (int i = 8; i <= 12; i++) {
            assertTrue(deletedCursor.advance());
            assertEquals(keys[i], deletedCursor.getKey());
        }
        assertFalse(deletedCursor.advance());

        assertFalse(survivingCursor.shouldAdvance());
        assertEquals(keys[8], survivingCursor.getKey());
        for (int i = 9; i <= 12; i++) {
            assertTrue(survivingCursor.advance());
            assertEquals(keys[i], survivingCursor.getKey());
        }
        assertFalse(survivingCursor.advance());
    }

    @Test
    public void testRepeatedClearTransitions() {
        var map = newJSHashMap();
        map.put(0, 0);
        map.put(1, 1);
        JSHashMap.Cursor cursor = map.getEntries();
        assertTrue(cursor.advance());
        JSHashMap.Cursor copy = cursor.copy();

        map.clear();
        map.put(10, 10);
        map.put(11, 11);
        map.clear();
        map.put(20, 20);

        for (JSHashMap.Cursor current : new JSHashMap.Cursor[]{cursor, copy}) {
            assertTrue(current.shouldAdvance());
            assertTrue(current.advance());
            assertEquals(20, current.getKey());
            assertFalse(current.advance());
        }
    }

    @Test
    public void testCollisionRemovalWithLiveCursor() {
        var map = newJSHashMap();
        CollisionKey[] keys = new CollisionKey[40];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = new CollisionKey(i);
            map.put(keys[i], i);
        }
        JSHashMap.Cursor cursor = map.getEntries();
        assertTrue(cursor.advance());
        assertEquals(keys[0], cursor.getKey());

        for (int i = 1; i <= 30; i++) {
            assertTrue(map.remove(new CollisionKey(i)));
        }
        assertFalse(cursor.shouldAdvance());
        for (int i = 31; i < keys.length; i++) {
            assertEquals(i, map.get(new CollisionKey(i)));
            assertTrue(cursor.advance());
            assertEquals(keys[i], cursor.getKey());
        }
        assertFalse(cursor.advance());
    }

    @Test
    public void testExhaustedCursorAcrossTransitions() {
        var map = newJSHashMap();
        map.put(0, 0);
        JSHashMap.Cursor cursor = map.getEntries();
        assertTrue(cursor.advance());
        assertFalse(cursor.advance());
        JSHashMap.Cursor copy = cursor.copy();

        map.remove(0);
        map.put(1, 1);
        map.clear();
        map.put(2, 2);
        assertFalse(cursor.advance());
        assertFalse(copy.advance());
    }

    @Test
    @SuppressFBWarnings(value = "DMI_RANDOM_USED_ONLY_ONCE", justification = "The seeded generator is reused for all 10,000 iterations of this randomized test")
    public void testRandomizedMutationAndIteration() {
        var map = newJSHashMap();
        ModelMap model = new ModelMap();
        List<JSHashMap.Cursor> cursors = new ArrayList<>();
        List<ModelCursor> modelCursors = new ArrayList<>();
        Random random = new Random(42);

        for (int step = 0; step < 10_000; step++) {
            int operation = random.nextInt(8);
            int key = random.nextInt(30);
            switch (operation) {
                case 0 -> {
                    map.put(key, step);
                    model.put(key, step);
                }
                case 1 -> assertEquals(model.remove(key), map.remove(key));
                case 2 -> {
                    map.clear();
                    model.clear();
                }
                case 3 -> {
                    if (cursors.size() < 30) {
                        cursors.add(map.getEntries());
                        modelCursors.add(model.getEntries());
                    }
                }
                case 4 -> {
                    if (!cursors.isEmpty() && cursors.size() < 30) {
                        int cursorIndex = random.nextInt(cursors.size());
                        cursors.add(cursors.get(cursorIndex).copy());
                        modelCursors.add(modelCursors.get(cursorIndex).copy());
                    }
                }
                case 5 -> {
                    if (!cursors.isEmpty()) {
                        int cursorIndex = random.nextInt(cursors.size());
                        assertCursorAdvance(cursors.get(cursorIndex), modelCursors.get(cursorIndex));
                    }
                }
                case 6 -> {
                    if (!cursors.isEmpty()) {
                        int cursorIndex = random.nextInt(cursors.size());
                        JSHashMap.Cursor cursor = cursors.get(cursorIndex);
                        ModelCursor modelCursor = modelCursors.get(cursorIndex);
                        boolean shouldAdvance = modelCursor.shouldAdvance();
                        assertEquals(shouldAdvance, cursor.shouldAdvance());
                        if (!modelCursor.done && !shouldAdvance) {
                            assertEquals(modelCursor.getKey(), cursor.getKey());
                            assertEquals(modelCursor.getValue(), cursor.getValue());
                        }
                    }
                }
                case 7 -> {
                    assertEquals(model.has(key), map.has(key));
                    assertEquals(model.get(key), map.get(key));
                }
                default -> throw new AssertionError();
            }
        }

        for (int i = 0; i < cursors.size(); i++) {
            while (assertCursorAdvance(cursors.get(i), modelCursors.get(i))) {
                // Drain the cursor.
            }
        }
    }

    private static boolean assertCursorAdvance(JSHashMap.Cursor cursor, ModelCursor modelCursor) {
        boolean expected = modelCursor.advance();
        assertEquals(expected, cursor.advance());
        if (expected) {
            assertEquals(modelCursor.getKey(), cursor.getKey());
            assertEquals(modelCursor.getValue(), cursor.getValue());
        }
        return expected;
    }

    @Test
    public void testCursorCopy() {
        var map = newJSHashMap();
        map.put("first", 1);
        map.put("second", 2);

        JSHashMap.Cursor cursor = map.getEntries();
        assertTrue(cursor.advance());
        JSHashMap.Cursor copy = cursor.copy();
        map.remove("first");

        assertTrue(cursor.advance());
        assertEquals("second", cursor.getKey());
        assertTrue(copy.advance());
        assertEquals("second", copy.getKey());
    }

    private static final class ModelMap {
        private final List<ModelEntry> entries = new ArrayList<>();

        void put(int key, int value) {
            ModelEntry entry = find(key);
            if (entry == null) {
                entries.add(new ModelEntry(key, value));
            } else {
                entry.value = value;
            }
        }

        boolean remove(int key) {
            ModelEntry entry = find(key);
            if (entry == null) {
                return false;
            }
            entry.alive = false;
            return true;
        }

        void clear() {
            for (ModelEntry entry : entries) {
                entry.alive = false;
            }
        }

        boolean has(int key) {
            return find(key) != null;
        }

        Object get(int key) {
            ModelEntry entry = find(key);
            return entry == null ? null : entry.value;
        }

        ModelCursor getEntries() {
            return new ModelCursor(this);
        }

        private ModelEntry find(int key) {
            for (ModelEntry entry : entries) {
                if (entry.alive && entry.key == key) {
                    return entry;
                }
            }
            return null;
        }
    }

    private static final class ModelEntry {
        private final int key;
        private int value;
        private boolean alive = true;

        ModelEntry(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final class ModelCursor {
        private final ModelMap map;
        private int current = -1;
        private boolean done;

        ModelCursor(ModelMap map) {
            this.map = map;
        }

        private ModelCursor(ModelMap map, int current, boolean done) {
            this.map = map;
            this.current = current;
            this.done = done;
        }

        boolean advance() {
            if (done) {
                return false;
            }
            int next = current + 1;
            while (next < map.entries.size() && !map.entries.get(next).alive) {
                next++;
            }
            if (next == map.entries.size()) {
                done = true;
                return false;
            }
            current = next;
            return true;
        }

        boolean shouldAdvance() {
            return !done && (current == -1 || !map.entries.get(current).alive);
        }

        int getKey() {
            return map.entries.get(current).key;
        }

        int getValue() {
            return map.entries.get(current).value;
        }

        ModelCursor copy() {
            return new ModelCursor(map, current, done);
        }
    }

    private static final class ThrowingHashKey {
        private final int id;
        private boolean throwOnHashCode;

        private ThrowingHashKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            if (throwOnHashCode) {
                throw new IllegalStateException("hash code failure");
            }
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ThrowingHashKey other && id == other.id;
        }

        void setThrowOnHashCode(boolean throwOnHashCode) {
            this.throwOnHashCode = throwOnHashCode;
        }
    }

    private static final class ThrowOnSecondHashKey {
        private final int id;
        private int hashCodeCalls;

        private ThrowOnSecondHashKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            if (++hashCodeCalls == 2) {
                throw new IllegalStateException("hash code failure");
            }
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ThrowOnSecondHashKey other && id == other.id;
        }

        int getHashCodeCalls() {
            return hashCodeCalls;
        }
    }

    private static final class CountingKey {
        private final int id;
        private int hashCodeCalls;

        private CountingKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            hashCodeCalls++;
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CountingKey other && id == other.id;
        }

        int getHashCodeCalls() {
            return hashCodeCalls;
        }

        void resetHashCodeCalls() {
            hashCodeCalls = 0;
        }
    }

    private static final class ThrowingEqualsKey {
        private final int id;
        private final boolean throwOnEquals;

        private ThrowingEqualsKey(int id, boolean throwOnEquals) {
            this.id = id;
            this.throwOnEquals = throwOnEquals;
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(Object obj) {
            if (throwOnEquals) {
                throw new IllegalStateException("equals failure");
            }
            return obj instanceof ThrowingEqualsKey other && id == other.id;
        }
    }

    private static final class CollisionKey {
        private final int id;

        private CollisionKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CollisionKey other && id == other.id;
        }
    }

    private static JSHashMapFacade newJSHashMap() {
        return new JSHashMapFacade();
    }

    private static final class JSHashMapFacade {
        private final JSHashMap map;

        JSHashMapFacade() {
            this(new JSHashMap());
        }

        private JSHashMapFacade(JSHashMap map) {
            this.map = map;
        }

        int size() {
            return map.size();
        }

        void put(Object key, Object value) {
            map.put(key, key.hashCode(), value);
        }

        void put(Object key, int hashCode, Object value) {
            map.put(key, hashCode, value);
        }

        Object putIfAbsent(Object key, Object value) {
            return map.putIfAbsent(key, key.hashCode(), value);
        }

        Object putIfAbsent(Object key, int hashCode, Object value) {
            return map.putIfAbsent(key, hashCode, value);
        }

        Object getOrInsert(Object key, Object value) {
            return map.getOrInsert(key, key.hashCode(), value);
        }

        Object getOrInsert(Object key, int hashCode, Object value) {
            return map.getOrInsert(key, hashCode, value);
        }

        Object get(Object key) {
            return map.get(key, key.hashCode());
        }

        Object get(Object key, int hashCode) {
            return map.get(key, hashCode);
        }

        Object getOrDefault(Object key, int hashCode, Object defaultValue) {
            return map.getOrDefault(key, hashCode, defaultValue);
        }

        boolean has(Object key) {
            return map.has(key, key.hashCode());
        }

        boolean has(Object key, int hashCode) {
            return map.has(key, hashCode);
        }

        boolean remove(Object key) {
            return map.remove(key, key.hashCode());
        }

        boolean remove(Object key, int hashCode) {
            return map.remove(key, hashCode);
        }

        void clear() {
            map.clear();
        }

        JSHashMap.Cursor getEntries() {
            return map.getEntries();
        }

        JSHashMapFacade copy() {
            return new JSHashMapFacade(map.copy());
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }
}
