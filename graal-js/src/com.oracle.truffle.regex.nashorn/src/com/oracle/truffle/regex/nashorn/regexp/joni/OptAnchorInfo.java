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
/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.regex.nashorn.regexp.joni;

// @formatter:off

import com.oracle.truffle.regex.nashorn.regexp.joni.constants.AnchorType;

final class OptAnchorInfo implements AnchorType {
    int leftAnchor;
    int rightAnchor;

    void clear() {
        leftAnchor = rightAnchor = 0;
    }

    void copy(final OptAnchorInfo other) {
        leftAnchor = other.leftAnchor;
        rightAnchor = other.rightAnchor;
    }

    void concat(final OptAnchorInfo left, final OptAnchorInfo right, final int leftLength, final int rightLength) {
        leftAnchor = left.leftAnchor;
        if (leftLength == 0) {
            leftAnchor |= right.leftAnchor;
        }

        rightAnchor = right.rightAnchor;
        if (rightLength == 0) {
            rightAnchor |= left.rightAnchor;
        }
    }

    boolean isSet(final int anchor) {
        if ((leftAnchor & anchor) != 0) {
            return true;
        }
        return (rightAnchor & anchor) != 0;
    }

    void add(final int anchor) {
        if (isLeftAnchor(anchor)) {
            leftAnchor |= anchor;
        } else {
            rightAnchor |= anchor;
        }
    }

    void remove(final int anchor) {
        if (isLeftAnchor(anchor)) {
            leftAnchor &= ~anchor;
        } else {
            rightAnchor &= ~anchor;
        }
    }

    void altMerge(final OptAnchorInfo other) {
        leftAnchor &= other.leftAnchor;
        rightAnchor &= other.rightAnchor;
    }

    static boolean isLeftAnchor(final int anchor) { // make a mask for it ?
        return !(anchor == END_BUF || anchor == SEMI_END_BUF ||
                 anchor == END_LINE || anchor == PREC_READ ||
                 anchor == PREC_READ_NOT);
    }

    static String anchorToString(final int anchor) {
        final StringBuffer s = new StringBuffer("[");

        if ((anchor & AnchorType.BEGIN_BUF) !=0 ) {
            s.append("begin-buf ");
        }
        if ((anchor & AnchorType.BEGIN_LINE) !=0 ) {
            s.append("begin-line ");
        }
        if ((anchor & AnchorType.BEGIN_POSITION) !=0 ) {
            s.append("begin-pos ");
        }
        if ((anchor & AnchorType.END_BUF) !=0 ) {
            s.append("end-buf ");
        }
        if ((anchor & AnchorType.SEMI_END_BUF) !=0 ) {
            s.append("semi-end-buf ");
        }
        if ((anchor & AnchorType.END_LINE) !=0 ) {
            s.append("end-line ");
        }
        if ((anchor & AnchorType.ANYCHAR_STAR) !=0 ) {
            s.append("anychar-star ");
        }
        if ((anchor & AnchorType.ANYCHAR_STAR_ML) !=0 ) {
            s.append("anychar-star-pl ");
        }
        s.append("]");

        return s.toString();
    }
}
