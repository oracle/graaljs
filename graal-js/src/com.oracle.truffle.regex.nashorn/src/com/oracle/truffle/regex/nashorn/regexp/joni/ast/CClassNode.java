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
package com.oracle.truffle.regex.nashorn.regexp.joni.ast;

// @formatter:off

import com.oracle.truffle.regex.nashorn.regexp.joni.BitSet;
import com.oracle.truffle.regex.nashorn.regexp.joni.CodeRangeBuffer;
import com.oracle.truffle.regex.nashorn.regexp.joni.Config;
import com.oracle.truffle.regex.nashorn.regexp.joni.EncodingHelper;
import com.oracle.truffle.regex.nashorn.regexp.joni.Option;
import com.oracle.truffle.regex.nashorn.regexp.joni.ScanEnvironment;
import com.oracle.truffle.regex.nashorn.regexp.joni.Syntax;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.CCSTATE;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.CCVALTYPE;
import com.oracle.truffle.regex.nashorn.regexp.joni.encoding.CharacterType;
import com.oracle.truffle.regex.nashorn.regexp.joni.encoding.IntHolder;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.ErrorMessages;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.InternalException;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.SyntaxException;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.ValueException;

public final class CClassNode extends Node {
    private static final int FLAG_NCCLASS_NOT = 1<<0;
    private static final int FLAG_NCCLASS_SHARE = 1<<1;

    int flags;
    public final BitSet bs = new BitSet();  // conditional creation ?
    public CodeRangeBuffer mbuf;            /* multi-byte info or NULL */

    private int ctype;                      // for hashing purposes

    private final static short AsciiCtypeTable[] = {
            0x4008, 0x4008, 0x4008, 0x4008, 0x4008, 0x4008, 0x4008, 0x4008,
            0x4008, 0x420c, 0x4209, 0x4208, 0x4208, 0x4208, 0x4008, 0x4008,
            0x4008, 0x4008, 0x4008, 0x4008, 0x4008, 0x4008, 0x4008, 0x4008,
            0x4008, 0x4008, 0x4008, 0x4008, 0x4008, 0x4008, 0x4008, 0x4008,
            0x4284, 0x41a0, 0x41a0, 0x41a0, 0x41a0, 0x41a0, 0x41a0, 0x41a0,
            0x41a0, 0x41a0, 0x41a0, 0x41a0, 0x41a0, 0x41a0, 0x41a0, 0x41a0,
            0x78b0, 0x78b0, 0x78b0, 0x78b0, 0x78b0, 0x78b0, 0x78b0, 0x78b0,
            0x78b0, 0x78b0, 0x41a0, 0x41a0, 0x41a0, 0x41a0, 0x41a0, 0x41a0,
            0x41a0, 0x7ca2, 0x7ca2, 0x7ca2, 0x7ca2, 0x7ca2, 0x7ca2, 0x74a2,
            0x74a2, 0x74a2, 0x74a2, 0x74a2, 0x74a2, 0x74a2, 0x74a2, 0x74a2,
            0x74a2, 0x74a2, 0x74a2, 0x74a2, 0x74a2, 0x74a2, 0x74a2, 0x74a2,
            0x74a2, 0x74a2, 0x74a2, 0x41a0, 0x41a0, 0x41a0, 0x41a0, 0x51a0,
            0x41a0, 0x78e2, 0x78e2, 0x78e2, 0x78e2, 0x78e2, 0x78e2, 0x70e2,
            0x70e2, 0x70e2, 0x70e2, 0x70e2, 0x70e2, 0x70e2, 0x70e2, 0x70e2,
            0x70e2, 0x70e2, 0x70e2, 0x70e2, 0x70e2, 0x70e2, 0x70e2, 0x70e2,
            0x70e2, 0x70e2, 0x70e2, 0x41a0, 0x41a0, 0x41a0, 0x41a0, 0x4008,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
    };

    // node_new_cclass
    public CClassNode() {}

    public void clear() {
        bs.clear();
        flags = 0;
        mbuf = null;
    }

    @Override
    public int getType() {
        return CCLASS;
    }

    @Override
    public String getName() {
        return "Character Class";
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof CClassNode)) {
            return false;
        }
        final CClassNode cc = (CClassNode)other;
        return ctype == cc.ctype && isNot() == cc.isNot();
    }

    @Override
    public int hashCode() {
        if (Config.USE_SHARED_CCLASS_TABLE) {
            int hash = 0;
            hash += ctype;
            if (isNot()) {
                hash++;
            }
            return hash + (hash >> 5);
        }
        return super.hashCode();
    }

    @Override
    public String toString(final int level) {
        final StringBuilder value = new StringBuilder();
        value.append("\n  flags: " + flagsToString());
        value.append("\n  bs: " + pad(bs, level + 1));
        value.append("\n  mbuf: " + pad(mbuf, level + 1));

        return value.toString();
    }

    public String flagsToString() {
        final StringBuilder f = new StringBuilder();
        if (isNot()) {
            f.append("NOT ");
        }
        if (isShare()) {
            f.append("SHARE ");
        }
        return f.toString();
    }

    public boolean isEmpty() {
        return mbuf == null && bs.isEmpty();
    }

    public void addCodeRangeToBuf(final int from, final int to) {
        mbuf = CodeRangeBuffer.addCodeRangeToBuff(mbuf, from, to);
    }

    public void addCodeRange(final ScanEnvironment env, final int from, final int to) {
        mbuf = CodeRangeBuffer.addCodeRange(mbuf, env, from, to);
    }

    public void addAllMultiByteRange() {
        mbuf = CodeRangeBuffer.addAllMultiByteRange(mbuf);
    }

    public void clearNotFlag() {
        if (isNot()) {
            bs.invert();

            mbuf = CodeRangeBuffer.notCodeRangeBuff(mbuf);
            clearNot();
        }
    }

    // and_cclass
    public void and(final CClassNode other) {
        final boolean not1 = isNot();
        BitSet bsr1 = bs;
        final CodeRangeBuffer buf1 = mbuf;
        final boolean not2 = other.isNot();
        BitSet bsr2 = other.bs;
        final CodeRangeBuffer buf2 = other.mbuf;

        if (not1) {
            final BitSet bs1 = new BitSet();
            bsr1.invertTo(bs1);
            bsr1 = bs1;
        }

        if (not2) {
            final BitSet bs2 = new BitSet();
            bsr2.invertTo(bs2);
            bsr2 = bs2;
        }

        bsr1.and(bsr2);

        if (bsr1 != bs) {
            bs.copy(bsr1);
            bsr1 = bs;
        }

        if (not1) {
            bs.invert();
        }

        CodeRangeBuffer pbuf = null;

        if (not1 && not2) {
            pbuf = CodeRangeBuffer.orCodeRangeBuff(buf1, false, buf2, false);
        } else {
            pbuf = CodeRangeBuffer.andCodeRangeBuff(buf1, not1, buf2, not2);

            if (not1) {
                pbuf = CodeRangeBuffer.notCodeRangeBuff(pbuf);
            }
        }
        mbuf = pbuf;

    }

    // or_cclass
    public void or(final CClassNode other) {
        final boolean not1 = isNot();
        BitSet bsr1 = bs;
        final CodeRangeBuffer buf1 = mbuf;
        final boolean not2 = other.isNot();
        BitSet bsr2 = other.bs;
        final CodeRangeBuffer buf2 = other.mbuf;

        if (not1) {
            final BitSet bs1 = new BitSet();
            bsr1.invertTo(bs1);
            bsr1 = bs1;
        }

        if (not2) {
            final BitSet bs2 = new BitSet();
            bsr2.invertTo(bs2);
            bsr2 = bs2;
        }

        bsr1.or(bsr2);

        if (bsr1 != bs) {
            bs.copy(bsr1);
            bsr1 = bs;
        }

        if (not1) {
            bs.invert();
        }

        CodeRangeBuffer pbuf = null;
        if (not1 && not2) {
            pbuf = CodeRangeBuffer.andCodeRangeBuff(buf1, false, buf2, false);
        } else {
            pbuf = CodeRangeBuffer.orCodeRangeBuff(buf1, not1, buf2, not2);
            if (not1) {
                pbuf = CodeRangeBuffer.notCodeRangeBuff(pbuf);
            }
        }
        mbuf = pbuf;
    }

    // add_ctype_to_cc_by_range // Encoding out!
    @SuppressWarnings("unused")
    public void addCTypeByRange(final int ct, final boolean not, final int sbOut, final int mbr[]) {
        final int n = mbr[0];

        if (!not) {
            for (int i=0; i<n; i++) {
                for (int j=mbr[i * 2 + 1]; j<=mbr[i * 2 + 2]; j++) {
                    if (j >= sbOut) {
                        if (Config.VANILLA) {
                            if (j == mbr[i * 2 + 2]) {
                                i++;
                            } else if (j > mbr[i * 2 + 1]) {
                                addCodeRangeToBuf(j, mbr[i * 2 + 2]);
                                i++;
                            }
                        } else {
                            if (j >= mbr[i * 2 + 1]) {
                                addCodeRangeToBuf(j, mbr[i * 2 + 2]);
                                i++;
                            }
                        }
                        // !goto sb_end!, remove duplication!
                        for (; i<n; i++) {
                            addCodeRangeToBuf(mbr[2 * i + 1], mbr[2 * i + 2]);
                        }
                        return;
                    }
                    bs.set(j);
                }
            }
            // !sb_end:!
            for (int i=0; i<n; i++) {
                addCodeRangeToBuf(mbr[2 * i + 1], mbr[2 * i + 2]);
            }

        } else {
            int prev = 0;

            for (int i=0; i<n; i++) {
                for (int j=prev; j < mbr[2 * i + 1]; j++) {
                    if (j >= sbOut) {
                        // !goto sb_end2!, remove duplication
                        prev = sbOut;
                        for (i=0; i<n; i++) {
                            if (prev < mbr[2 * i + 1]) {
                                addCodeRangeToBuf(prev, mbr[i * 2 + 1] - 1);
                            }
                            prev = mbr[i * 2 + 2] + 1;
                        }
                        if (prev < 0x7fffffff/*!!!*/) {
                            addCodeRangeToBuf(prev, 0x7fffffff);
                        }
                        return;
                    }
                    bs.set(j);
                }
                prev = mbr[2 * i + 2] + 1;
            }

            for (int j=prev; j<sbOut; j++) {
                bs.set(j);
            }

            // !sb_end2:!
            prev = sbOut;
            for (int i=0; i<n; i++) {
                if (prev < mbr[2 * i + 1]) {
                    addCodeRangeToBuf(prev, mbr[i * 2 + 1] - 1);
                }
                prev = mbr[i * 2 + 2] + 1;
            }
            if (prev < 0x7fffffff/*!!!*/) {
                addCodeRangeToBuf(prev, 0x7fffffff);
            }
        }
    }

    public void addCType(final int ctp, final boolean not, final ScanEnvironment env, final IntHolder sbOut) {
        int ct = ctp;
        if (Config.NON_UNICODE_SDW) {
            switch (ct) {
            case CharacterType.D:
            case CharacterType.S:
            case CharacterType.W:
                ct ^= CharacterType.SPECIAL_MASK;

                if (env.syntax == Syntax.JAVASCRIPT && ct == CharacterType.SPACE) {
                    // \s in JavaScript includes unicode characters.
                    break;
                }

                if (not) {
                    for (int c = 0; c < BitSet.SINGLE_BYTE_SIZE; c++) {
                        // if (!ASCIIEncoding.INSTANCE.isCodeCType(c, ctype)) bs.set(c);
                        if ((AsciiCtypeTable[c] & (1 << ct)) == 0) {
                            bs.set(c);
                        }
                    }
                    addAllMultiByteRange();
                } else {
                    for (int c = 0; c < BitSet.SINGLE_BYTE_SIZE; c++) {
                        // if (ASCIIEncoding.INSTANCE.isCodeCType(c, ctype)) bs.set(c);
                        if ((AsciiCtypeTable[c] & (1 << ct)) != 0) {
                            bs.set(c);
                        }
                    }
                }
                return;
            default:
                break;
            }
        }

        final int[] ranges = EncodingHelper.ctypeCodeRange(ct, sbOut);
        if (ranges != null) {
            addCTypeByRange(ct, not, sbOut.value, ranges);
            return;
        }

        switch(ct) {
        case CharacterType.ALPHA:
        case CharacterType.BLANK:
        case CharacterType.CNTRL:
        case CharacterType.DIGIT:
        case CharacterType.LOWER:
        case CharacterType.PUNCT:
        case CharacterType.SPACE:
        case CharacterType.UPPER:
        case CharacterType.XDIGIT:
        case CharacterType.ASCII:
        case CharacterType.ALNUM:
            if (not) {
                for (int c=0; c<BitSet.SINGLE_BYTE_SIZE; c++) {
                    if (!EncodingHelper.isCodeCType(c, ct)) {
                        bs.set(c);
                    }
                }
                addAllMultiByteRange();
            } else {
                for (int c=0; c<BitSet.SINGLE_BYTE_SIZE; c++) {
                    if (EncodingHelper.isCodeCType(c, ct)) {
                        bs.set(c);
                    }
                }
            }
            break;

        case CharacterType.GRAPH:
        case CharacterType.PRINT:
            if (not) {
                for (int c=0; c<BitSet.SINGLE_BYTE_SIZE; c++) {
                    if (!EncodingHelper.isCodeCType(c, ct)) {
                        bs.set(c);
                    }
                }
            } else {
                for (int c=0; c<BitSet.SINGLE_BYTE_SIZE; c++) {
                    if (EncodingHelper.isCodeCType(c, ct)) {
                        bs.set(c);
                    }
                }
                addAllMultiByteRange();
            }
            break;

        case CharacterType.WORD:
            if (!not) {
                for (int c=0; c<BitSet.SINGLE_BYTE_SIZE; c++) {
                    if (EncodingHelper.isWord(c)) {
                        bs.set(c);
                    }
                }

                addAllMultiByteRange();
            } else {
                for (int c=0; c<BitSet.SINGLE_BYTE_SIZE; c++) {
                    if (!EncodingHelper.isWord(c)) {
                        bs.set(c);
                    }
                }
            }
            break;

        default:
            throw new InternalException(ErrorMessages.ERR_PARSER_BUG);
        } // switch
    }

    public static final class CCStateArg {
        static final int VS_UNSET = -1;
        public int v;
        public int vs = VS_UNSET;
        public CCVALTYPE inType;
        public CCVALTYPE type;
        public CCSTATE state;
    }

    public void nextStateClass(final CCStateArg arg, final ScanEnvironment env) {
        if (arg.state == CCSTATE.RANGE) {
            // [x-\d] or [\s-\d]
            if (Option.isUnicode(env.reg.getOptions())) {
                throw new SyntaxException(ErrorMessages.ERR_CHAR_CLASS_VALUE_AT_END_OF_RANGE);
            } else {
                bs.set('-');
                if (arg.vs != CCStateArg.VS_UNSET) {
                    bs.set(arg.vs);
                }
            }
        }

        if (arg.state == CCSTATE.VALUE && arg.type != CCVALTYPE.CLASS) {
            if (arg.type == CCVALTYPE.SB) {
                bs.set(arg.vs);
            } else if (arg.type == CCVALTYPE.CODE_POINT) {
                addCodeRange(env, arg.vs, arg.vs);
            }
        }
        arg.state = CCSTATE.VALUE;
        arg.vs = CCStateArg.VS_UNSET;
        arg.type = CCVALTYPE.CLASS;
    }

    public void nextStateValue(final CCStateArg arg, final ScanEnvironment env) {

        switch(arg.state) {
        case VALUE:
            if (arg.type == CCVALTYPE.SB) {
                if (arg.vs > 0xff) {
                    throw new ValueException(ErrorMessages.ERR_INVALID_CODE_POINT_VALUE);
                }
                bs.set(arg.vs);
            } else if (arg.type == CCVALTYPE.CODE_POINT) {
                addCodeRange(env, arg.vs, arg.vs);
            }
            break;

        case RANGE:
            if (arg.inType == arg.type) {
                if (arg.inType == CCVALTYPE.SB) {
                    if (arg.vs > 0xff || arg.v > 0xff) {
                        throw new ValueException(ErrorMessages.ERR_INVALID_CODE_POINT_VALUE);
                    }

                    if (arg.vs > arg.v) {
                        if (env.syntax.allowEmptyRangeInCC()) {
                            // goto ccs_range_end
                            arg.state = CCSTATE.COMPLETE;
                            break;
                        }
                        throw new ValueException(ErrorMessages.ERR_EMPTY_RANGE_IN_CHAR_CLASS);
                    }
                    bs.setRange(arg.vs, arg.v);
                } else {
                    addCodeRange(env, arg.vs, arg.v);
                }
            } else {
                if (arg.vs > arg.v) {
                    if (env.syntax.allowEmptyRangeInCC()) {
                        // goto ccs_range_end
                        arg.state = CCSTATE.COMPLETE;
                        break;
                    }
                    throw new ValueException(ErrorMessages.ERR_EMPTY_RANGE_IN_CHAR_CLASS);
                }
                if (arg.vs == CCStateArg.VS_UNSET) {
                    // [\d-x]
                    if (Option.isUnicode(env.reg.getOptions())) {
                        throw new SyntaxException(ErrorMessages.ERR_CHAR_CLASS_VALUE_AT_START_OF_RANGE);
                    } else {
                        if (arg.inType == CCVALTYPE.SB) {
                            if (arg.v > 0xff) {
                                throw new ValueException(ErrorMessages.ERR_INVALID_CODE_POINT_VALUE);
                            }
                            bs.set(arg.v);
                        } else { // arg.inType == CCVALTYPE.CODE_POINT
                            addCodeRange(env, arg.v, arg.v);
                        }
                        bs.set('-');
                    }
                } else {
                    bs.setRange(arg.vs, arg.v < 0xff ? arg.v : 0xff);
                    addCodeRange(env, arg.vs, arg.v);
                }
            }
            // ccs_range_end:
            arg.state = CCSTATE.COMPLETE;
            break;

        case COMPLETE:
        case START:
            arg.state = CCSTATE.VALUE;
            break;

        default:
            break;

        } // switch

        arg.vs = arg.v;
        arg.type = arg.inType;
    }

    // onig_is_code_in_cc_len
    public boolean isCodeInCCLength(final int code) {
        boolean found;

        if (code > 0xff) {
            found = mbuf != null && mbuf.isInCodeRange(code);
        } else {
            found = bs.at(code);
        }

        if (isNot()) {
            return !found;
        }
        return found;
    }

    // onig_is_code_in_cc
    public boolean isCodeInCC(final int code) {
         return isCodeInCCLength(code);
    }

    public void setNot() {
        flags |= FLAG_NCCLASS_NOT;
    }

    public void clearNot() {
        flags &= ~FLAG_NCCLASS_NOT;
    }

    public boolean isNot() {
        return (flags & FLAG_NCCLASS_NOT) != 0;
    }

    public void setShare() {
        flags |= FLAG_NCCLASS_SHARE;
    }

    public void clearShare() {
        flags &= ~FLAG_NCCLASS_SHARE;
    }

    public boolean isShare() {
        return (flags & FLAG_NCCLASS_SHARE) != 0;
    }

}
