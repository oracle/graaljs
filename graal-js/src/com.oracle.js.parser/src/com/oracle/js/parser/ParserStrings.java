/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.js.parser;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;

public class ParserStrings {

    public static final TruffleString EMPTY_STRING = constant("");
    public static final TruffleString DASH = constant("-");

    public static TruffleString constant(String s) {
        TruffleString ret = TruffleString.fromJavaStringUncached(s, TruffleString.Encoding.UTF_16);
        ret.hashCodeUncached(TruffleString.Encoding.UTF_16);
        return ret;
    }

    public static int length(TruffleString s) {
        return s.byteLength(TruffleString.Encoding.UTF_16) >> 1;
    }

    public static char charAt(TruffleString s, int i) {
        return (char) s.readCharUTF16Uncached(i);
    }

    public static TruffleStringBuilder builderCreate() {
        return TruffleStringBuilder.create(TruffleString.Encoding.UTF_16);
    }

    public static TruffleStringBuilder builderCreate(int capacity) {
        return TruffleStringBuilder.create(TruffleString.Encoding.UTF_16, capacity << 1);
    }

    public static TruffleString fromLong(long longValue) {
        return fromLong(TruffleString.FromLongNode.getUncached(), longValue);
    }

    public static TruffleString fromLong(TruffleString.FromLongNode node, long longValue) {
        return node.execute(longValue, TruffleString.Encoding.UTF_16, true);
    }

    public static TruffleString concat(TruffleString a, TruffleString b) {
        return a.concatUncached(b, TruffleString.Encoding.UTF_16, true);
    }

    public static TruffleString concatAll(TruffleString s, TruffleString... concat) {
        int len = length(s);
        for (TruffleString c : concat) {
            len += length(c);
        }
        TruffleStringBuilder sb = builderCreate(len);
        TruffleStringBuilder.AppendStringNode.getUncached().execute(sb, s);
        for (TruffleString c : concat) {
            TruffleStringBuilder.AppendStringNode.getUncached().execute(sb, c);
        }
        return TruffleStringBuilder.ToStringNode.getUncached().execute(sb);
    }

    public static TruffleString substring(TruffleString s, int fromIndex) {
        int fromByteIndex = fromIndex * 2;
        return s.substringByteIndexUncached(fromByteIndex, s.byteLength(TruffleString.Encoding.UTF_16) - fromByteIndex, TruffleString.Encoding.UTF_16, true);
    }

    public static int indexOf(TruffleString string, char c) {
        return string.charIndexOfAnyCharUTF16Uncached(0, length(string), new char[]{c});
    }

    public static boolean startsWith(TruffleString s1, TruffleString s2) {
        return startsWith(s1, s2, 0);
    }

    public static boolean startsWith(TruffleString s1, TruffleString s2, int startPos) {
        return startsWith(TruffleString.RegionEqualByteIndexNode.getUncached(), s1, s2, startPos);
    }

    public static boolean startsWith(TruffleString.RegionEqualByteIndexNode regionEqualsNode, TruffleString s1, TruffleString s2) {
        return startsWith(regionEqualsNode, s1, s2, 0);
    }

    public static boolean startsWith(TruffleString.RegionEqualByteIndexNode regionEqualsNode, TruffleString s1, TruffleString s2, int startPos) {
        return length(s1) - startPos >= length(s2) && regionEquals(regionEqualsNode, s1, startPos, s2, 0, length(s2));
    }

    public static boolean regionEquals(TruffleString s1, int offset1, TruffleString s2, int offset2, int length) {
        return regionEquals(TruffleString.RegionEqualByteIndexNode.getUncached(), s1, offset1, s2, offset2, length);
    }

    public static boolean regionEquals(TruffleString.RegionEqualByteIndexNode regionEqualsNode, TruffleString s1, int offset1, TruffleString s2, int offset2, int length) {
        return regionEqualsNode.execute(s1, offset1 << 1, s2, offset2 << 1, length << 1, TruffleString.Encoding.UTF_16);
    }
}
