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
package com.oracle.truffle.regex.nashorn.regexp.joni.encoding;

// @formatter:off

public interface CharacterType {

    final int NEWLINE   = 0;
    final int ALPHA     = 1;
    final int BLANK     = 2;
    final int CNTRL     = 3;
    final int DIGIT     = 4;
    final int GRAPH     = 5;
    final int LOWER     = 6;
    final int PRINT     = 7;
    final int PUNCT     = 8;
    final int SPACE     = 9;
    final int UPPER     = 10;
    final int XDIGIT    = 11;
    final int WORD      = 12;
    final int ALNUM     = 13;      /* alpha || digit */
    final int ASCII     = 14;

    final int SPECIAL_MASK = 256;
    final int S = SPECIAL_MASK | SPACE;
    final int D = SPECIAL_MASK | DIGIT;
    final int W = SPECIAL_MASK | WORD;

    final int LETTER_MASK = (1 << Character.UPPERCASE_LETTER)
                          | (1 << Character.LOWERCASE_LETTER)
                          | (1 << Character.TITLECASE_LETTER)
                          | (1 << Character.MODIFIER_LETTER)
                          | (1 << Character.OTHER_LETTER);
    final int ALPHA_MASK = LETTER_MASK
                          | (1 << Character.COMBINING_SPACING_MARK)
                          | (1 << Character.NON_SPACING_MARK)
                          | (1 << Character.ENCLOSING_MARK);
    final int ALNUM_MASK = ALPHA_MASK
                          | (1 << Character.DECIMAL_DIGIT_NUMBER);
    final int WORD_MASK = ALNUM_MASK
                          | (1 << Character.CONNECTOR_PUNCTUATION);
    final int PUNCT_MASK =  (1 << Character.CONNECTOR_PUNCTUATION)
                          | (1 << Character.DASH_PUNCTUATION)
                          | (1 << Character.END_PUNCTUATION)
                          | (1 << Character.FINAL_QUOTE_PUNCTUATION)
                          | (1 << Character.INITIAL_QUOTE_PUNCTUATION)
                          | (1 << Character.OTHER_PUNCTUATION)
                          | (1 << Character.START_PUNCTUATION);
    final int CNTRL_MASK =  (1 << Character.CONTROL)
                          | (1 << Character.FORMAT)
                          | (1 << Character.PRIVATE_USE)
                          | (1 << Character.SURROGATE);
    final int SPACE_MASK =  (1 << Character.SPACE_SEPARATOR)
                          | (1 << Character.LINE_SEPARATOR)        // 0x2028
                          | (1 << Character.PARAGRAPH_SEPARATOR);  // 0x2029
    final int GRAPH_MASK = SPACE_MASK
                          | (1 << Character.CONTROL)
                          | (1 << Character.SURROGATE);
    final int PRINT_MASK =  (1 << Character.CONTROL)
                          | (1 << Character.SURROGATE);


}
