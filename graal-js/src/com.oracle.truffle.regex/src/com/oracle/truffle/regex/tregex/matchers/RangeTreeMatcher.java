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
package com.oracle.truffle.regex.tregex.matchers;

import com.oracle.truffle.api.CompilerDirectives;

/**
 * Character range matcher using a left-balanced tree of ranges.
 */
public final class RangeTreeMatcher extends ProfiledCharMatcher {

    /**
     * Constructs a new {@link RangeTreeMatcher}.
     * 
     * @param invert see {@link ProfiledCharMatcher}.
     * @param ranges a sorted array of character ranges in the form [lower inclusive bound of range
     *            0, higher inclusive bound of range 0, lower inclusive bound of range 1, higher
     *            inclusive bound of range 1, ...]. The array contents are not modified by this
     *            method.
     * @return a new {@link RangeTreeMatcher}.
     */
    public static RangeTreeMatcher fromRanges(boolean invert, char[] ranges) {
        char[] tree = new char[ranges.length];
        buildTree(tree, 0, ranges, 0, ranges.length / 2);
        return new RangeTreeMatcher(invert, tree);
    }

    /**
     * Adapted version of the algorithm given by J. Andreas Baerentzen in
     * <a href="http://www2.imm.dtu.dk/pubdb/views/edoc_download.php/2535/pdf/imm2535.pdf">"On
     * Left-balancing Binary Trees"</a>.
     * 
     * @param tree The array that will hold the tree. Its size must be equal to that of the
     *            parameter "ranges".
     * @param curTreeElement Index of the current tree node to be computed. Note that every tree
     *            element takes up two array slots, since every node corresponds to a character
     *            range.
     * @param ranges Sorted array of character ranges, in the form [lower bound of range 0, higher
     *            bound of range 0, lower bound of range 1, higher bound of range 1, ...]
     * @param offset Starting index of the current part of the list that shall be converted to a
     *            subtree.
     * @param nRanges Number of _ranges_ in the current part of the list that shall be converted to
     *            a subtree. Again, note that every range takes up two array slots!
     */
    private static void buildTree(char[] tree, int curTreeElement, char[] ranges, int offset, int nRanges) {
        if (nRanges == 0) {
            return;
        }
        if (nRanges == 1) {
            tree[curTreeElement] = ranges[offset];
            tree[curTreeElement + 1] = ranges[offset + 1];
            return;
        }
        int nearestPowerOf2 = Integer.highestOneBit(nRanges);
        int remainder = nRanges - (nearestPowerOf2 - 1);
        int nLeft;
        int nRight;
        if (remainder <= nearestPowerOf2 / 2) {
            nLeft = (nearestPowerOf2 - 2) / 2 + remainder;
            nRight = (nearestPowerOf2 - 2) / 2;
        } else {
            nLeft = nearestPowerOf2 - 1;
            nRight = remainder - 1;
        }
        int median = offset + nLeft * 2;
        tree[curTreeElement] = ranges[median];
        tree[curTreeElement + 1] = ranges[median + 1];
        buildTree(tree, leftChild(curTreeElement), ranges, offset, nLeft);
        buildTree(tree, rightChild(curTreeElement), ranges, median + 2, nRight);
    }

    private static int leftChild(int i) {
        return (i * 2) + 2;
    }

    private static int rightChild(int i) {
        return (i * 2) + 4;
    }

    @CompilerDirectives.CompilationFinal(dimensions = 1) private final char[] tree;

    private RangeTreeMatcher(boolean invert, char[] tree) {
        super(invert);
        this.tree = tree;
    }

    @Override
    public boolean matchChar(char c) {
        int i = 0;
        while (i < tree.length) {
            final char lo = tree[i];
            final char hi = tree[i + 1];
            if (lo <= c) {
                if (hi >= c) {
                    return true;
                } else {
                    i = rightChild(i);
                }
            } else {
                i = leftChild(i);
            }
        }
        return false;
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        return "tree " + modifiersToString() + MatcherBuilder.rangesToString(tree);
    }
}
