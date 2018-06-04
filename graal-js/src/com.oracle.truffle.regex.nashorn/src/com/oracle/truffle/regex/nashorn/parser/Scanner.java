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
package com.oracle.truffle.regex.nashorn.parser;

// @formatter:off

/**
 * Utility for scanning thru a char array.
 *
 */
public class Scanner {
    /** Characters to scan. */
    protected final char[] content;

    /** Position in content. */
    protected int position;

    /** Scan limit. */
    protected final int limit;

    /** Current character in stream */
    protected char ch0;
    /** 1 character lookahead */
    protected char ch1;
    /** 2 character lookahead */
    protected char ch2;
    /** 3 character lookahead */
    protected char ch3;

    /**
     * Constructor
     *
     * @param content content to scan
     * @param start   position index in content where to start
     * @param length  length of input
     */
    protected Scanner(final char[] content, final int start, final int length) {
        this.content  = content;
        this.position = start;
        this.limit    = start + length;

        reset(position);
    }

    /**
     * Constructor
     *
     * Scan content from beginning to end. Content given as a string
     *
     * @param content content to scan
     */
    protected Scanner(final String content) {
        this(content.toCharArray(), 0, content.length());
    }

    /**
     * Returns true of scanner is at end of input
     * @return true if no more input
     */
    protected final boolean atEOF() {
        return position == limit;
    }

    /**
     * Get the ith character from the content.
     * @param i Index of character.
     * @return ith character or '\0' if beyond limit.
     */
    protected final char charAt(final int i) {
        // Get a character from the content, '\0' if beyond the end of file.
        return i < limit ? content[i] : '\0';
    }

    /**
     * Reset to a character position.
     * @param i Position in content.
     */
    protected final void reset(final int i) {
        ch0 = charAt(i);
        ch1 = charAt(i + 1);
        ch2 = charAt(i + 2);
        ch3 = charAt(i + 3);
        position = i < limit? i : limit;
    }

    /**
     * Skip ahead a number of characters.
     * @param n Number of characters to skip.
     */
    protected final void skip(final int n) {
        if (n == 1 && !atEOF()) {
            ch0 = ch1;
            ch1 = ch2;
            ch2 = ch3;
            ch3 = charAt(position + 4);
            position++;
        } else if (n != 0) {
            reset(position + n);
        }
    }

    protected String getContents() {
        return new String(content);
    }
}
