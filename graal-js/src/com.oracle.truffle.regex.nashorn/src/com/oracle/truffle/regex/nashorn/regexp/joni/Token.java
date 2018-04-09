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

import com.oracle.truffle.regex.nashorn.regexp.joni.constants.TokenType;

final class Token {
    TokenType type;
    boolean escaped;
    int backP;

    // union fields
    private int INT1, INT2, INT3, INT4;

    // union accessors
    int getC() {
        return INT1;
    }
    void setC(final int c) {
        INT1 = c;
    }

    int getCode() {
        return INT1;
    }
    void setCode(final int code) {
        INT1 = code;
    }

    int getAnchor() {
        return INT1;
    }
    void setAnchor(final int anchor) {
        INT1 = anchor;
    }

    // repeat union member
    int getRepeatLower() {
        return INT1;
    }
    void setRepeatLower(final int lower) {
        INT1 = lower;
    }

    int getRepeatUpper() {
        return INT2;
    }
    void setRepeatUpper(final int upper) {
        INT2 = upper;
    }

    boolean getRepeatGreedy() {
        return INT3 != 0;
    }
    void setRepeatGreedy(final boolean greedy) {
        INT3 = greedy ? 1 : 0;
    }

    boolean getRepeatPossessive() {
        return INT4 != 0;
    }
    void setRepeatPossessive(final boolean possessive) {
        INT4 = possessive ? 1 : 0;
    }

    int getBackrefRef() {
        return INT2;
    }
    void setBackrefRef(final int ref1) {
        INT2 = ref1;
    }

    // prop union member
    int getPropCType() {
        return INT1;
    }
    void setPropCType(final int ctype) {
        INT1 = ctype;
    }

    boolean getPropNot() {
        return INT2 != 0;
    }
    void setPropNot(final boolean not) {
        INT2 = not ? 1 : 0;
    }
}
