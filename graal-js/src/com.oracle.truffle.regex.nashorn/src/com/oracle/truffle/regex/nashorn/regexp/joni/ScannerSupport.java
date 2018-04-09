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

import com.oracle.truffle.regex.nashorn.regexp.joni.encoding.IntHolder;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.ErrorMessages;

abstract class ScannerSupport extends IntHolder implements ErrorMessages {

    protected final char[] chars;       // pattern
    protected int p;                    // current scanner position
    protected int stop;                 // pattern end (mutable)
    private int lastFetched;            // last fetched value for unfetch support
    protected int c;                    // current code point

    private final int begin;            // pattern begin position for reset() support
    private final int end;              // pattern end position for reset() support
    protected int _p;                   // used by mark()/restore() to mark positions

    private final static int INT_SIGN_BIT = 1 << 31;

    protected ScannerSupport(final char[] chars, final int p, final int end) {
        this.chars = chars;
        this.begin = p;
        this.end = end;

        reset();
    }

    protected int getBegin() {
        return begin;
    }

    protected int getEnd() {
        return end;
    }

    protected final int scanUnsignedNumber() {
        final int last = c;
        int num = 0; // long ???
        while(left()) {
            fetch();
            if (Character.isDigit(c)) {
                final int onum = num;
                num = num * 10 + EncodingHelper.digitVal(c);
                if (((onum ^ num) & INT_SIGN_BIT) != 0) {
                    return -1;
                }
            } else {
                unfetch();
                break;
            }
        }
        c = last;
        return num;
    }

    protected final int scanUnsignedHexadecimalNumber(final int maxLength) {
        final int last = c;
        int num = 0;
        int ml = maxLength;
        while(left() && ml-- != 0) {
            fetch();
            if (EncodingHelper.isXDigit(c)) {
                final int onum = num;
                final int val = EncodingHelper.xdigitVal(c);
                num = (num << 4) + val;
                if (((onum ^ num) & INT_SIGN_BIT) != 0) {
                    return -1;
                }
            } else {
                unfetch();
                break;
            }
        }
        c = last;
        return num;
    }

    protected final int scanUnsignedOctalNumber(final int maxLength) {
        final int last = c;
        int num = 0;
        int ml = maxLength;
        while(left() && ml-- != 0) {
            fetch();
            if (Character.isDigit(c) && c < '8') {
                final int onum = num;
                final int val = EncodingHelper.odigitVal(c);
                num = (num << 3) + val;
                if (((onum ^ num) & INT_SIGN_BIT) != 0) {
                    return -1;
                }
            } else {
                unfetch();
                break;
            }
        }
        c = last;
        return num;
    }

    protected final void reset() {
        p = begin;
        stop = end;
    }

    protected final void mark() {
        _p = p;
    }

    protected final void restore() {
        p = _p;
    }

    protected final void inc() {
        lastFetched = p;
        p++;
    }

    protected final void fetch() {
        lastFetched = p;
        c = chars[p++];
    }

    protected int fetchTo() {
        lastFetched = p;
        return chars[p++];
    }

    protected final void unfetch() {
        p = lastFetched;
    }

    protected final int peek() {
        return p < stop ? chars[p] : 0;
    }

    protected final boolean peekIs(final int ch) {
        return peek() == ch;
    }

    protected final boolean left() {
        return p < stop;
    }

}
