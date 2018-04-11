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
package com.oracle.truffle.regex.tregex.parser;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.List;

public class CodePointRange implements Comparable<CodePointRange>, ContainsRange {

    public final int lo;
    public final int hi;

    public CodePointRange(int lo, int hi) {
        assert hi >= lo;
        this.lo = lo;
        this.hi = hi;
    }

    public CodePointRange(int c) {
        this(c, c);
    }

    public CodePointRange(char lo, char hi) {
        this((int) lo, (int) hi);
    }

    public CodePointRange(char c) {
        this((int) c);
    }

    public static CodePointRange fromUnordered(int c1, int c2) {
        return new CodePointRange(Math.min(c1, c2), Math.max(c1, c2));
    }

    @Override
    public CodePointRange getRange() {
        return this;
    }

    public CodePointRange move(int delta) {
        return new CodePointRange(lo + delta, hi + delta);
    }

    public CodePointRange expand(CodePointRange o) {
        assert intersects(o) || adjacent(o);
        return new CodePointRange(Math.min(lo, o.lo), Math.max(hi, o.hi));
    }

    public CodePointRange createIntersection(CodePointRange o) {
        return intersects(o) ? new CodePointRange(Math.max(lo, o.lo), Math.min(hi, o.hi)) : null;
    }

    public boolean isSingle() {
        return lo == hi;
    }

    public boolean contains(CodePointRange o) {
        return lo <= o.lo && hi >= o.hi;
    }

    public boolean intersects(CodePointRange o) {
        return lo <= o.hi && o.lo <= hi;
    }

    public boolean rightOf(CodePointRange o) {
        return lo > o.hi;
    }

    public boolean adjacent(CodePointRange o) {
        return hi + 1 == o.lo || lo - 1 == o.hi;
    }

    @Override
    public int compareTo(CodePointRange o) {
        return lo - o.lo;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof CodePointRange && lo == ((CodePointRange) obj).lo && hi == ((CodePointRange) obj).hi;
    }

    @Override
    public int hashCode() {
        return (31 * lo) + (31 * hi);
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        if (isSingle()) {
            return DebugUtil.charToString(lo);
        }
        return DebugUtil.charToString(lo) + "-" + DebugUtil.charToString(hi);
    }

    public static boolean binarySearchExactMatch(List<? extends ContainsRange> ranges, ContainsRange range, int searchResult) {
        return searchResult >= 0 && ranges.get(searchResult).getRange().equals(range.getRange());
    }

    public static boolean binarySearchExactMatch(ContainsRange[] ranges, ContainsRange range, int searchResult) {
        return searchResult >= 0 && ranges[searchResult].getRange().equals(range.getRange());
    }

    public static int binarySearchGetFirstIntersecting(List<? extends ContainsRange> ranges, ContainsRange range, int searchResult) {
        assert CodePointRange.rangesAreSortedAndDisjoint(ranges);
        if (searchResult >= 0) {
            assert !ranges.get(searchResult).getRange().equals(range.getRange());
            return searchResult;
        }
        int insertionPoint = (searchResult + 1) * (-1);
        if (insertionPoint > 0 && ranges.get(insertionPoint - 1).getRange().intersects(range.getRange())) {
            return insertionPoint - 1;
        }
        return insertionPoint;
    }

    public static int binarySearchGetFirstIntersecting(ContainsRange[] ranges, ContainsRange range, int searchResult) {
        assert CodePointRange.rangesAreSortedAndDisjoint(ranges);
        if (searchResult >= 0) {
            assert !ranges[searchResult].getRange().equals(range.getRange());
            return searchResult;
        }
        int insertionPoint = (searchResult + 1) * (-1);
        if (insertionPoint > 0 && ranges[insertionPoint - 1].getRange().intersects(range.getRange())) {
            return insertionPoint - 1;
        }
        return insertionPoint;
    }

    public static boolean binarySearchNoIntersectingFound(ContainsRange[] ranges, int firstIntersecting) {
        return firstIntersecting == ranges.length;
    }

    public static boolean rangesAreSortedAndDisjoint(List<? extends ContainsRange> ranges) {
        for (int i = 1; i < ranges.size(); i++) {
            if ((ranges.get(i - 1).getRange().lo > ranges.get(i).getRange().lo) ||
                            ranges.get(i - 1).getRange().intersects(ranges.get(i).getRange())) {
                return false;
            }
        }
        return true;
    }

    public static boolean rangesAreSortedAndDisjoint(ContainsRange[] ranges) {
        for (int i = 1; i < ranges.length; i++) {
            if ((ranges[i - 1].getRange().lo > ranges[i].getRange().lo) ||
                            ranges[i - 1].getRange().intersects(ranges[i].getRange())) {
                return false;
            }
        }
        return true;
    }
}
