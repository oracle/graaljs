/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * Utility class for calls to library methods that require a {@link TruffleBoundary}.
 */
public final class Boundaries {

    private Boundaries() {
        // don't instantiate this
    }

    @TruffleBoundary
    public static String stringFormat(String format, Object... params) {
        return String.format(format, params);
    }

    @TruffleBoundary
    public static boolean characterIsUpperCase(char ch) {
        return Character.isUpperCase(ch);
    }

    @TruffleBoundary
    public static boolean characterIsSpaceChar(int cp) {
        return Character.isSpaceChar(cp);
    }

    @TruffleBoundary
    public static String integerToString(int i, int radix) {
        return Integer.toString(i, radix);
    }

    @TruffleBoundary
    public static boolean equals(Object a, Object b) {
        return a.equals(b);
    }

    @TruffleBoundary(allowInlining = true)
    public static <K, V> Map.Entry<K, V> mapEntry(K key, V value) {
        return Map.entry(key, value);
    }

    @TruffleBoundary
    public static <K, V> V mapPut(Map<K, V> map, K key, V value) {
        return map.put(key, value);
    }

    @TruffleBoundary
    public static <K, V> V mapPutIfAbsent(Map<K, V> map, K key, V value) {
        return map.putIfAbsent(key, value);
    }

    @TruffleBoundary
    public static <K, V> boolean mapContainsKey(Map<K, V> map, K key) {
        return map.containsKey(key);
    }

    @TruffleBoundary
    public static <K, V> V mapGet(Map<K, V> map, K key) {
        return map.get(key);
    }

    @TruffleBoundary
    public static <K, V> V mapRemove(Map<K, V> map, K key) {
        return map.remove(key);
    }

    @TruffleBoundary
    public static <T> T listGet(List<T> list, int index) {
        return list.get(index);
    }

    @TruffleBoundary
    public static <T> int listSize(List<T> list) {
        return list.size();
    }

    @TruffleBoundary
    public static <T> void listAdd(List<T> list, T element) {
        list.add(element);
    }

    @TruffleBoundary
    public static <T> void listAddAll(List<T> list, List<T> addList) {
        list.addAll(addList);
    }

    @TruffleBoundary
    public static <T> boolean listContains(List<T> list, T element) {
        return list.contains(element);
    }

    @TruffleBoundary
    public static boolean listContainsUnchecked(List<?> list, Object element) {
        return list.contains(element);
    }

    @TruffleBoundary
    public static <T> Object[] listToArray(List<T> list) {
        return list.toArray();
    }

    @TruffleBoundary
    public static String[] listToStringArray(List<String> list) {
        return list.toArray(new String[0]);
    }

    public static <T> List<T> listToEditable(List<T> list) {
        if (list instanceof ArrayList) {
            return list;
        }
        return listEditableCopy(list);
    }

    @TruffleBoundary
    public static <T> List<T> listEditableCopy(List<T> list) {
        return new ArrayList<>(list);
    }

    @TruffleBoundary
    public static <T> EconomicSet<T> economicSetCreate() {
        return EconomicSet.create();
    }

    @TruffleBoundary
    public static <T> boolean economicSetAdd(EconomicSet<T> economicSet, T element) {
        return economicSet.add(element);
    }

    @TruffleBoundary
    public static <T> boolean economicSetContains(EconomicSet<T> economicSet, T element) {
        return economicSet.contains(element);
    }

    @TruffleBoundary
    public static <K, V> EconomicMap<K, V> economicMapCreate() {
        return EconomicMap.create();
    }

    @TruffleBoundary
    public static <K, V> V economicMapPut(EconomicMap<K, V> map, K key, V value) {
        return map.put(key, value);
    }

    @TruffleBoundary
    public static <K, V> boolean economicMapContainsKey(EconomicMap<K, V> map, K key) {
        return map.containsKey(key);
    }

    @TruffleBoundary
    public static <K, V> V economicMapGet(EconomicMap<K, V> map, K key) {
        return map.get(key);
    }

    @TruffleBoundary(allowInlining = true)
    public static byte[] byteBufferArray(ByteBuffer buffer) {
        return buffer.array();
    }

    @TruffleBoundary(allowInlining = true)
    public static void byteBufferPutSlice(ByteBuffer dst, int dstPos, ByteBuffer src, int srcPos, int srcLimit) {
        dst.put(dstPos, src, srcPos, srcLimit - srcPos);
    }

    @TruffleBoundary(allowInlining = true)
    public static ByteBuffer byteBufferSlice(ByteBuffer buf, int pos, int limit) {
        return buf.slice(pos, limit - pos);
    }

    @TruffleBoundary(allowInlining = true)
    public static void byteBufferGet(ByteBuffer src, int srcPos, byte[] dst, int dstPos, int length) {
        src.get(srcPos, dst, dstPos, length);
    }

    @TruffleBoundary(allowInlining = true)
    public static void byteBufferPutArray(ByteBuffer dst, int dstPos, byte[] src, int srcPos, int srcLength) {
        dst.put(dstPos, src, srcPos, srcLength);
    }

    @TruffleBoundary(allowInlining = true)
    public static ByteBuffer byteBufferWrap(byte[] array) {
        return ByteBuffer.wrap(array);
    }

    @TruffleBoundary
    public static boolean setContains(Set<?> set, Object element) {
        return set.contains(element);
    }

    @TruffleBoundary
    public static <T> void queueAdd(Queue<? super T> queue, T request) {
        queue.add(request);
    }

    @TruffleBoundary
    public static <T> void arraySort(T[] array, Comparator<? super T> comparator) {
        try {
            Arrays.sort(array, comparator);
        } catch (IllegalArgumentException e) {
            /*
             * Arrays.sort throws IllegalArgumentException when the comparator violates its general
             * consistent ordering contract. We can just ignore the exception in this case because,
             * if "comparefn" is not a consistent comparator for the elements of this array, or is
             * undefined (i.e. using natural ordering), and all applications of ToString do not
             * produce the same result, the behavior of sort is implementation-defined.
             *
             * See SortIndexedProperties, https://tc39.es/ecma262/#sec-sortindexedproperties.
             */
        }
    }

    @TruffleBoundary
    public static void arraySort(int[] array) {
        Arrays.sort(array);
    }

    @TruffleBoundary
    public static void arraySort(long[] array) {
        Arrays.sort(array);
    }

    @TruffleBoundary
    public static void arraySort(double[] array) {
        Arrays.sort(array);
    }
}
