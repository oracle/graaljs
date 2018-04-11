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
package com.oracle.truffle.regex.util;

import com.oracle.truffle.regex.tregex.parser.CaseFoldTable;
import com.oracle.truffle.regex.tregex.parser.CodePointRange;
import com.oracle.truffle.regex.tregex.parser.CodePointSet;

public class Constants {

    public static final int MIN_CODEPOINT = 0;
    public static final int MAX_CODEPOINT = 0x10FFFF;

    public static final CodePointRange BMP_RANGE = new CodePointRange(MIN_CODEPOINT, 0xFFFF);
    public static final CodePointRange ASTRAL_RANGE = new CodePointRange(0x10000, MAX_CODEPOINT);

    public static final CodePointRange LEAD_SURROGATE_RANGE = new CodePointRange(0xD800, 0xDBFF);
    public static final CodePointRange TRAIL_SURROGATE_RANGE = new CodePointRange(0xDC00, 0xDFFF);

    public static final CodePointRange BMP_BEFORE_SURROGATES_RANGE = new CodePointRange(BMP_RANGE.lo, LEAD_SURROGATE_RANGE.lo - 1);
    public static final CodePointRange BMP_AFTER_SURROGATES_RANGE = new CodePointRange(TRAIL_SURROGATE_RANGE.hi + 1, BMP_RANGE.hi);

    public static final CodePointSet BMP_WITHOUT_SURROGATES = CodePointSet.create(BMP_BEFORE_SURROGATES_RANGE, BMP_AFTER_SURROGATES_RANGE).freeze();
    public static final CodePointSet ASTRAL_SYMBOLS = CodePointSet.create(ASTRAL_RANGE).freeze();
    public static final CodePointSet LEAD_SURROGATES = CodePointSet.create(LEAD_SURROGATE_RANGE).freeze();
    public static final CodePointSet TRAIL_SURROGATES = CodePointSet.create(TRAIL_SURROGATE_RANGE).freeze();

    public static final CodePointSet DIGITS = CodePointSet.create(new CodePointRange('0', '9')).freeze();
    public static final CodePointSet NON_DIGITS = DIGITS.createInverse().freeze();
    public static final CodePointSet WORD_CHARS = CodePointSet.create(
                    new CodePointRange('a', 'z'),
                    new CodePointRange('A', 'Z'),
                    new CodePointRange('0', '9'),
                    new CodePointRange('_')).freeze();
    public static final CodePointSet NON_WORD_CHARS = WORD_CHARS.createInverse().freeze();
    // If we want to store negations of basic character classes, then we also need to store their
    // case-folded variants because one must apply case-folding *before* inverting the character
    // class. The WORD_CHARS (\w) character class is the only one of the basic classes (\w, \d, \s)
    // that is affected by case-folding and only so when both the Unicode and IgnoreCase flags are
    // set.
    public static final CodePointSet WORD_CHARS_UNICODE_IGNORE_CASE = CaseFoldTable.applyCaseFold(WORD_CHARS, true).freeze();
    public static final CodePointSet NON_WORD_CHARS_UNICODE_IGNORE_CASE = WORD_CHARS_UNICODE_IGNORE_CASE.createInverse().freeze();

    // WhiteSpace defined in ECMA-262 2018 11.2
    // 0x0009, CHARACTER TABULATION, <TAB>
    // 0x000B, LINE TABULATION, <VT>
    // 0x000C, FORM FEED (FF), <FF>
    // 0x0020, SPACE, <SP>
    // 0x00A0, NO-BREAK SPACE, <NBSP>
    // 0xFEFF, ZERO WIDTH NO-BREAK SPACE, <ZWNBSP>
    // Unicode 10.0 whitespaces (category 'Zs')
    // 0x0020, SPACE
    // 0x00A0, NO-BREAK SPACE
    // 0x1680, OGHAM SPACE MARK
    // 0x2000, EN QUAD
    // 0x2001, EM QUAD
    // 0x2002, EN SPACE
    // 0x2003, EM SPACE
    // 0x2004, THREE-PER-EM SPACE
    // 0x2005, FOUR-PER-EM SPACE
    // 0x2006, SIX-PER-EM SPACE
    // 0x2007, FIGURE SPACE
    // 0x2008, PUNCTUATION SPACE
    // 0x2009, THIN SPACE
    // 0x200A, HAIR SPACE
    // 0x202F, NARROW NO-BREAK SPACE
    // 0x205F, MEDIUM MATHEMATICAL SPACE
    // 0x3000, IDEOGRAPHIC SPACE
    // LineTerminator defined in ECMA-262 2018 11.3
    // 0x000A, LINE FEED (LF), <LF>
    // 0x000D, CARRIAGE RETURN (CR), <CR>
    // 0x2028, LINE SEPARATOR, <LS>
    // 0x2029, PARAGRAPH SEPARATOR, <PS>
    public static final CodePointSet WHITE_SPACE = CodePointSet.create(
                    new CodePointRange('\t', '\r'),
                    new CodePointRange(' '),
                    new CodePointRange('\u00a0'),
                    new CodePointRange('\u1680'),
                    new CodePointRange('\u2000', '\u200a'),
                    new CodePointRange('\u2028', '\u2029'),
                    new CodePointRange('\u202f'),
                    new CodePointRange('\u205f'),
                    new CodePointRange('\u3000'),
                    new CodePointRange('\ufeff')).freeze();
    public static final CodePointSet NON_WHITE_SPACE = WHITE_SPACE.createInverse().freeze();

    // In versions of Unicode older than 6.3, 0x180E MONGOLIAN VOWEL SEPARATOR is also part of
    // the 'Zs' category, and therefore considered WhiteSpace. Such versions of Unicode are used by
    // ECMAScript 6 and older.
    public static final CodePointSet LEGACY_WHITE_SPACE = WHITE_SPACE.copy().addRange(new CodePointRange('\u180e')).freeze();
    public static final CodePointSet LEGACY_NON_WHITE_SPACE = LEGACY_WHITE_SPACE.createInverse().freeze();

    public static final CodePointSet LINE_TERMINATOR = CodePointSet.create(
                    new CodePointRange('\n'),
                    new CodePointRange('\r'),
                    new CodePointRange('\u2028', '\u2029')).freeze();
    public static final CodePointSet DOT = LINE_TERMINATOR.createInverse().freeze();
    public static final CodePointSet DOT_ALL = CodePointSet.create(new CodePointRange(MIN_CODEPOINT, MAX_CODEPOINT)).freeze();

    public static final CodePointSet HEX_CHARS = CodePointSet.create(
                    new CodePointRange('0', '9'),
                    new CodePointRange('A', 'F'),
                    new CodePointRange('a', 'f')).freeze();
}
