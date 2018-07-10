/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * Utility class for calls to library methods that require a {@link TruffleBoundary}.
 */
public final class Boundaries {

    private Boundaries() {
        // don't instantiate this
    }

    @TruffleBoundary
    public static String javaToString(Object value) {
        return value.toString();
    }

    @TruffleBoundary
    public static String stringValueOf(long l) {
        return String.valueOf(l);
    }

    @TruffleBoundary
    public static String stringValueOf(int i) {
        return String.valueOf(i);
    }

    @TruffleBoundary
    public static String stringValueOf(double d) {
        return String.valueOf(d);
    }

    @TruffleBoundary
    public static String stringValueOf(Object o) {
        return String.valueOf(o);
    }

    @TruffleBoundary
    public static Integer integerValueOf(String s) {
        return Integer.valueOf(s);
    }

    @TruffleBoundary
    public static Double doubleValueOf(String s) {
        return Double.valueOf(s);
    }

    @TruffleBoundary
    public static Float floatValueOf(String s) {
        return Float.valueOf(s);
    }

    @TruffleBoundary
    public static Long longValueOf(String s) {
        return Long.valueOf(s);
    }

    @TruffleBoundary
    public static String builderToString(StringBuilder res) {
        return res.toString();
    }

    @TruffleBoundary
    public static void builderAppend(StringBuilder sb, CharSequence seq) {
        sb.append(seq);
    }

    @TruffleBoundary
    public static void builderAppend(StringBuilder sb, String str) {
        sb.append(str);
    }

    @TruffleBoundary
    public static void builderAppend(StringBuilder sb, char chr) {
        sb.append(chr);
    }

    @TruffleBoundary
    public static void builderAppend(StringBuilder sb, CharSequence seq, int start, int end) {
        sb.append(seq, start, end);
    }

    @TruffleBoundary
    public static void builderAppend(StringBuilder sb, String str, int start, int end) {
        sb.append(str, start, end);
    }

    @TruffleBoundary
    public static char charAt(CharSequence cs, int idx) {
        return cs.charAt(idx);
    }

    @TruffleBoundary
    public static CharSequence subSequence(CharSequence s, int begin, int end) {
        return s.subSequence(begin, end);
    }

    @TruffleBoundary
    public static CharSequence subSequence(CharSequence s, int begin) {
        return s.subSequence(begin, s.length());
    }

    @TruffleBoundary
    public static String substring(String s, int begin, int end) {
        return s.substring(begin, end);
    }

    @TruffleBoundary
    public static String substring(String s, int begin) {
        return s.substring(begin);
    }

    @TruffleBoundary
    public static String stringFormat(String format, Object... params) {
        return String.format(format, params);
    }

    @TruffleBoundary
    public static int stringLastIndexOf(String s, String pattern) {
        return s.lastIndexOf(pattern);
    }

    @TruffleBoundary
    public static int stringLastIndexOf(String s, String pattern, int startPos) {
        return s.lastIndexOf(pattern, startPos);
    }

    @TruffleBoundary
    public static int stringLastIndexOf(String s, char pattern) {
        return s.lastIndexOf(pattern);
    }

    @TruffleBoundary
    public static int stringLastIndexOf(String s, char pattern, int startPos) {
        return s.lastIndexOf(pattern, startPos);
    }

    @TruffleBoundary
    public static int stringCompareTo(String a, String b) {
        return a.compareTo(b);
    }

    @TruffleBoundary
    public static boolean stringStartsWith(String s, String pattern) {
        return s.startsWith(pattern);
    }

    @TruffleBoundary
    public static boolean stringStartsWith(String s, String pattern, int startPos) {
        return s.startsWith(pattern, startPos);
    }

    @TruffleBoundary
    public static boolean stringEndsWith(String s, String pattern) {
        return s.endsWith(pattern);
    }

    @TruffleBoundary
    public static int stringCodePointAt(String s, int pos) {
        return s.codePointAt(pos);
    }

    @TruffleBoundary
    public static boolean characterIsDigit(char ch) {
        return Character.isDigit(ch);
    }

    @TruffleBoundary
    public static boolean equals(Object a, Object b) {
        return a.equals(b);
    }

    @TruffleBoundary
    public static <K, V> Set<Map.Entry<K, V>> mapEntrySet(Map<K, V> map) {
        return map.entrySet();
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
    public static <K, V> boolean mapContainsKey(Map<K, V> map, Object key) {
        return map.containsKey(key);
    }

    @TruffleBoundary
    public static <K, V> V mapGet(Map<K, V> map, Object key) {
        return map.get(key);
    }

    @TruffleBoundary
    public static <K, V> V mapRemove(Map<K, V> map, Object key) {
        return map.remove(key);
    }

    @TruffleBoundary
    public static <T> T listGet(List<T> list, int intValue) {
        return list.get(intValue);
    }

    @TruffleBoundary
    public static <T> void listSet(List<T> list, int intValue, T value) {
        list.set(intValue, value);
    }

    @TruffleBoundary
    public static <T> int listSize(List<T> list) {
        return list.size();
    }

    @TruffleBoundary
    public static <T> int listIndexOf(List<T> list, T element) {
        return list.indexOf(element);
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
    public static <T> Object[] listToArray(List<T> list) {
        return list.toArray();
    }

    @TruffleBoundary
    public static <T> boolean iteratorHasNext(Iterator<T> it) {
        return it.hasNext();
    }

    @TruffleBoundary
    public static <T> T iteratorNext(Iterator<T> it) {
        return it.next();
    }

    @TruffleBoundary
    public static <T> Iterator<T> iterator(Iterable<T> iterable) {
        return iterable.iterator();
    }
}
