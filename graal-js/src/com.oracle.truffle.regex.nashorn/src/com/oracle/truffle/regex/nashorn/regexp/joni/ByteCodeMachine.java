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

import static com.oracle.truffle.regex.nashorn.regexp.joni.BitStatus.bsAt;
import static com.oracle.truffle.regex.nashorn.regexp.joni.EncodingHelper.isNewLine;
import static com.oracle.truffle.regex.nashorn.regexp.joni.Option.isFindCondition;
import static com.oracle.truffle.regex.nashorn.regexp.joni.Option.isFindLongest;
import static com.oracle.truffle.regex.nashorn.regexp.joni.Option.isFindNotEmpty;
import static com.oracle.truffle.regex.nashorn.regexp.joni.Option.isNotBol;
import static com.oracle.truffle.regex.nashorn.regexp.joni.Option.isNotEol;
import static com.oracle.truffle.regex.nashorn.regexp.joni.Option.isPosixRegion;

import com.oracle.truffle.regex.nashorn.regexp.joni.ast.CClassNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.OPCode;
import com.oracle.truffle.regex.nashorn.regexp.joni.encoding.IntHolder;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.ErrorMessages;
import com.oracle.truffle.regex.nashorn.regexp.joni.exception.InternalException;

class ByteCodeMachine extends StackMachine {
    private int bestLen;          // return value
    private int s = 0;            // current char

    private int range;            // right range
    private int sstart;

    private final int[] code;       // byte code
    private int ip;                 // instruction pointer

    ByteCodeMachine(final Regex regex, final String chars, final int p, final int end) {
        super(regex, chars, p, end);
        this.code = regex.code;
    }

    @SuppressWarnings("unused")
    private boolean stringCmpIC(final int caseFoldFlag, final int s1p, final IntHolder ps2, final int mbLen, final int textEnd) {
        int s1 = s1p;
        int s2 = ps2.value;
        final int end1 = s1 + mbLen;

        while (s1 < end1) {
            final char c1 = EncodingHelper.toUpperCase(charAt(s1++));
            final char c2 = EncodingHelper.toUpperCase(charAt(s2++));

            if (c1 != c2) {
                return false;
            }
        }
        ps2.value = s2;
        return true;
    }

    private void debugMatchBegin() {
        Config.log.println("match_at: " +
                "str: " + str +
                ", end: " + end +
                ", start: " + this.sstart);
        Config.log.println("size: " + (end - str) + ", start offset: " + (this.sstart - str));
    }

    private void debugMatchLoop() {
        if (Config.DEBUG_MATCH) {
            Config.log.printf("%4d", (s - str)).print("> \"");
            int q, i;
            for (i=0, q=s; i<7 && q<end && s>=0; i++) {
                if (q < end) {
                    Config.log.print(new String(new char[]{charAt(q++)}));
                }
            }
            final String string = q < end ? "...\"" : "\"";
            q += string.length();
            Config.log.print(string);
            for (i=0; i<20-(q-s);i++) {
                Config.log.print(" ");
            }
            final StringBuilder sb = new StringBuilder();
            new ByteCodePrinter(regex).compiledByteCodeToString(sb, ip);
            Config.log.println(sb.toString());
        }
    }

    @Override
    protected final int matchAt(final int r, final int ss) {
        this.range = r;
        this.sstart = ss;

        stk = 0;
        ip = 0;

        if (Config.DEBUG_MATCH) {
            debugMatchBegin();
        }

        init();

        bestLen = -1;
        s = ss;

        final int[] c = this.code;
        while (true) {
            if (Config.DEBUG_MATCH) {
                debugMatchLoop();
            }

            switch (c[ip++]) {
                case OPCode.END:    if (opEnd()) {
                    return finish();
                }                  break;
                case OPCode.EXACT1:                     opExact1();                break;
                case OPCode.EXACT2:                     opExact2();                break;
                case OPCode.EXACT3:                     opExact3();                break;
                case OPCode.EXACT4:                     opExact4();                break;
                case OPCode.EXACT5:                     opExact5();                break;
                case OPCode.EXACTN:                     opExactN();                break;

                case OPCode.EXACT1_IC:                  opExact1IC();              break;
                case OPCode.EXACTN_IC:                  opExactNIC();              break;

                case OPCode.CCLASS:                     opCClass();                break;
                case OPCode.CCLASS_MB:                  opCClassMB();              break;
                case OPCode.CCLASS_MIX:                 opCClassMIX();             break;
                case OPCode.CCLASS_NOT:                 opCClassNot();             break;
                case OPCode.CCLASS_MB_NOT:              opCClassMBNot();           break;
                case OPCode.CCLASS_MIX_NOT:             opCClassMIXNot();          break;
                case OPCode.CCLASS_NODE:                opCClassNode();            break;

                case OPCode.ANYCHAR:                    opAnyChar();               break;
                case OPCode.ANYCHAR_ML:                 opAnyCharML();             break;
                case OPCode.ANYCHAR_STAR:               opAnyCharStar();           break;
                case OPCode.ANYCHAR_ML_STAR:            opAnyCharMLStar();         break;
                case OPCode.ANYCHAR_STAR_PEEK_NEXT:     opAnyCharStarPeekNext();   break;
                case OPCode.ANYCHAR_ML_STAR_PEEK_NEXT:  opAnyCharMLStarPeekNext(); break;

                case OPCode.WORD:                       opWord();                  break;
                case OPCode.NOT_WORD:                   opNotWord();               break;
                case OPCode.WORD_BOUND:                 opWordBound();             break;
                case OPCode.NOT_WORD_BOUND:             opNotWordBound();          break;
                case OPCode.WORD_BEGIN:                 opWordBegin();             break;
                case OPCode.WORD_END:                   opWordEnd();               break;

                case OPCode.BEGIN_BUF:                  opBeginBuf();              break;
                case OPCode.END_BUF:                    opEndBuf();                break;
                case OPCode.BEGIN_LINE:                 opBeginLine();             break;
                case OPCode.END_LINE:                   opEndLine();               break;
                case OPCode.SEMI_END_BUF:               opSemiEndBuf();            break;
                case OPCode.BEGIN_POSITION:             opBeginPosition();         break;

                case OPCode.MEMORY_START_PUSH:          opMemoryStartPush();       break;
                case OPCode.MEMORY_START:               opMemoryStart();           break;
                case OPCode.MEMORY_END_PUSH:            opMemoryEndPush();         break;
                case OPCode.MEMORY_END:                 opMemoryEnd();             break;
                case OPCode.MEMORY_CLEAR:               opMemoryClear();           break;

                case OPCode.BACKREF1:                   opBackRef1();              break;
                case OPCode.BACKREF2:                   opBackRef2();              break;
                case OPCode.BACKREFN:                   opBackRefN();              break;
                case OPCode.BACKREFN_IC:                opBackRefNIC();            break;
                case OPCode.BACKREF_MULTI:              opBackRefMulti();          break;
                case OPCode.BACKREF_MULTI_IC:           opBackRefMultiIC();        break;
                case OPCode.BACKREF_WITH_LEVEL:         opBackRefAtLevel();        break;

                case OPCode.NULL_CHECK_START:           opNullCheckStart();        break;
                case OPCode.NULL_CHECK_END:             opNullCheckEnd();          break;
                case OPCode.NULL_CHECK_END_MEMST:       opNullCheckEndMemST();     break;

                case OPCode.JUMP:                       opJump();                  break;
                case OPCode.PUSH:                       opPush();                  break;

                case OPCode.POP:                        opPop();                   break;
                case OPCode.PUSH_OR_JUMP_EXACT1:        opPushOrJumpExact1();      break;
                case OPCode.PUSH_IF_PEEK_NEXT:          opPushIfPeekNext();        break;

                case OPCode.REPEAT:                     opRepeat();                break;
                case OPCode.REPEAT_NG:                  opRepeatNG();              break;
                case OPCode.REPEAT_INC:                 opRepeatInc();             break;
                case OPCode.REPEAT_INC_SG:              opRepeatIncSG();           break;
                case OPCode.REPEAT_INC_NG:              opRepeatIncNG();           break;
                case OPCode.REPEAT_INC_NG_SG:           opRepeatIncNGSG();         break;

                case OPCode.PUSH_POS:                   opPushPos();               break;
                case OPCode.POP_POS:                    opPopPos();                break;
                case OPCode.PUSH_POS_NOT:               opPushPosNot();            break;
                case OPCode.FAIL_POS:                   opFailPos();               break;
                case OPCode.PUSH_STOP_BT:               opPushStopBT();            break;
                case OPCode.POP_STOP_BT:                opPopStopBT();             break;

                case OPCode.LOOK_BEHIND:                opLookBehind();            break;
                case OPCode.PUSH_LOOK_BEHIND_NOT:       opPushLookBehindNot();     break;
                case OPCode.FAIL_LOOK_BEHIND_NOT:       opFailLookBehindNot();     break;

                case OPCode.FINISH:
                    return finish();

                case OPCode.FAIL:                       opFail();                  continue;

                default:
                    throw new InternalException(ErrorMessages.ERR_UNDEFINED_BYTECODE);

            } // main switch
        } // main while
    }

    private boolean opEnd() {
        final int n = s - sstart;

        if (n > bestLen) {
            if (Config.USE_FIND_LONGEST_SEARCH_ALL_OF_RANGE) {
                if (isFindLongest(regex.options)) {
                    if (n > msaBestLen) {
                        msaBestLen = n;
                        msaBestS = sstart;
                    } else {
                        // goto end_best_len;
                        return endBestLength();
                    }
                }
            } // USE_FIND_LONGEST_SEARCH_ALL_OF_RANGE

            bestLen = n;
            final Region region = msaRegion;
            if (region != null) {
                // USE_POSIX_REGION_OPTION ... else ...
                region.beg[0] = msaBegin = sstart - str;
                region.end[0] = msaEnd   = s      - str;
                for (int i = 1; i <= regex.numMem; i++) {
                    // opt!
                    if (repeatStk[memEndStk + i] != INVALID_INDEX) {
                        region.beg[i] = bsAt(regex.btMemStart, i) ?
                                        stack[repeatStk[memStartStk + i]].getMemPStr() - str :
                                        repeatStk[memStartStk + i] - str;


                        region.end[i] = bsAt(regex.btMemEnd, i) ?
                                        stack[repeatStk[memEndStk + i]].getMemPStr() :
                                        repeatStk[memEndStk + i] - str;

                    } else {
                        region.beg[i] = region.end[i] = Region.REGION_NOTPOS;
                    }

                }

            } else {
                msaBegin = sstart - str;
                msaEnd   = s      - str;
            }
        } else {
            final Region region = msaRegion;
            if (Config.USE_POSIX_API_REGION_OPTION) {
                if (!isPosixRegion(regex.options)) {
                    if (region != null) {
                        region.clear();
                    } else {
                        msaBegin = msaEnd = 0;
                    }
                }
            } else {
                if (region != null) {
                    region.clear();
                } else {
                    msaBegin = msaEnd = 0;
                }
            } // USE_POSIX_REGION_OPTION
        }
        // end_best_len:
        /* default behavior: return first-matching result. */
        return endBestLength();
    }

    private boolean endBestLength() {
        if (isFindCondition(regex.options)) {
            if (isFindNotEmpty(regex.options) && s == sstart) {
                bestLen = -1;
                {opFail(); return false;} /* for retry */
            }
            if (isFindLongest(regex.options) && s < range) {
                {opFail(); return false;} /* for retry */
            }
        }
        // goto finish;
        return true;
    }

    private void opExact1() {
        if (s >= range || code[ip] != charAt(s++)) {opFail(); return;}
        //if (s > range) {opFail(); return;}
        ip++;
    }

    private void opExact2() {
        if (s + 2 > range) {opFail(); return;}
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
    }

    private void opExact3() {
        if (s + 3 > range) {opFail(); return;}
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
    }

    private void opExact4() {
        if (s + 4 > range) {opFail(); return;}
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
    }

    private void opExact5() {
        if (s + 5 > range) {opFail(); return;}
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
        if (code[ip] != charAt(s)) {opFail(); return;}
        ip++; s++;
    }

    private void opExactN() {
        int tlen = code[ip++];
        if (s + tlen > range) {opFail(); return;}

        if (Config.USE_STRING_TEMPLATES) {
            final char[] bs = regex.templates[code[ip++]];
            int ps = code[ip++];

            while (tlen-- > 0) {
                if (bs[ps++] != charAt(s++)) {opFail(); return;}
            }

        } else {
            while (tlen-- > 0) {
                if (code[ip++] != charAt(s++)) {opFail(); return;}
            }
        }
    }

    private void opExact1IC() {
        if (s >= range || code[ip] != EncodingHelper.toLowerCase(charAt(s++))) {opFail(); return;}
        ip++;
    }

    private void opExactNIC() {
        int tlen = code[ip++];
        if (s + tlen > range) {opFail(); return;}

        if (Config.USE_STRING_TEMPLATES) {
            final char[] bs = regex.templates[code[ip++]];
            int ps = code[ip++];

            while (tlen-- > 0) {
                if (bs[ps++] != EncodingHelper.toLowerCase(charAt(s++))) {opFail(); return;}
            }
        } else {

            while (tlen-- > 0) {
                if (code[ip++] != EncodingHelper.toLowerCase(charAt(s++))) {opFail(); return;}
            }
        }
    }

    private boolean isInBitSet() {
        final int c = charAt(s);
        return (c <= 0xff && (code[ip + (c >>> BitSet.ROOM_SHIFT)] & (1 << c)) != 0);
    }

    private void opCClass() {
        if (s >= range || !isInBitSet()) {opFail(); return;}
        ip += BitSet.BITSET_SIZE;
        s++;
    }

    private boolean isInClassMB() {
        final int tlen = code[ip++];
        if (s >= range) {
            return false;
        }
        final int ss = s;
        s++;
        final int c = charAt(ss);
        if (!EncodingHelper.isInCodeRange(code, ip, c)) {
            return false;
        }
        ip += tlen;
        return true;
    }

    private void opCClassMB() {
        // beyond string check
        if (s >= range || charAt(s) <= 0xff) {opFail(); return;}
        if (!isInClassMB()) {opFail(); return;} // not!!!
    }

    private void opCClassMIX() {
        if (s >= range) {opFail(); return;}
        if (charAt(s) > 0xff) {
            ip += BitSet.BITSET_SIZE;
            if (!isInClassMB()) {opFail(); return;}
        } else {
            if (!isInBitSet()) {opFail(); return;}
            ip += BitSet.BITSET_SIZE;
            final int tlen = code[ip++]; // by code range length
            ip += tlen;
            s++;
        }
    }

    private void opCClassNot() {
        if (s >= range || isInBitSet()) {opFail(); return;}
        ip += BitSet.BITSET_SIZE;
        s++;
    }

    private boolean isNotInClassMB() {
        final int tlen = code[ip++];

        if (!(s + 1 <= range)) {
            if (s >= range) {
                return false;
            }
            s = end;
            ip += tlen;
            return true;
        }

        final int ss = s;
        s++;
        final int c = charAt(ss);

        if (EncodingHelper.isInCodeRange(code, ip, c)) {
            return false;
        }
        ip += tlen;
        return true;
    }

    private void opCClassMBNot() {
        if (s >= range) {opFail(); return;}
        if (charAt(s) <= 0xff) {
            s++;
            final int tlen = code[ip++];
            ip += tlen;
            return;
        }
        if (!isNotInClassMB()) {opFail(); return;}
    }

    private void opCClassMIXNot() {
        if (s >= range) {opFail(); return;}
        if (charAt(s) > 0xff) {
            ip += BitSet.BITSET_SIZE;
            if (!isNotInClassMB()) {opFail(); return;}
        } else {
            if (isInBitSet()) {opFail(); return;}
            ip += BitSet.BITSET_SIZE;
            final int tlen = code[ip++];
            ip += tlen;
            s++;
        }
    }

    private void opCClassNode() {
        if (s >= range) {opFail(); return;}
        final CClassNode cc = (CClassNode)regex.operands[code[ip++]];
        final int ss = s;
        s++;
        final int c = charAt(ss);
        if (!cc.isCodeInCCLength(c)) {opFail(); return;}
    }

    private void opAnyChar() {
        if (s >= range) {opFail(); return;}
        if (isNewLine(charAt(s))) {opFail(); return;}
        s++;
    }

    private void opAnyCharML() {
        if (s >= range) {opFail(); return;}
        s++;
    }

    private void opAnyCharStar() {
        while (s < range) {
            pushAlt(ip, s);
            if (isNewLineAt(s)) {opFail(); return;}
            s++;
        }
    }

    private void opAnyCharMLStar() {
        while (s < range) {
            pushAlt(ip, s);
            s++;
        }
    }

    private void opAnyCharStarPeekNext() {
        final char c = (char)code[ip];

        while (s < range) {
            final char b = charAt(s);
            if (c == b) {
                pushAlt(ip + 1, s);
            }
            if (isNewLine(b)) {opFail(); return;}
            s++;
        }
        ip++;
    }

    private void opAnyCharMLStarPeekNext() {
        final char c = (char)code[ip];

        while (s < range) {
            if (c == charAt(s)) {
                pushAlt(ip + 1, s);
            }
            s++;
        }
        ip++;
    }

    private void opWord() {
        if (s >= range || !EncodingHelper.isWord(charAt(s))) {opFail(); return;}
        s++;
    }

    private void opNotWord() {
        if (s >= range || EncodingHelper.isWord(charAt(s))) {opFail(); return;}
        s++;
    }

    private void opWordBound() {
        if (isWordAt(s) == isWordAt(s - 1)) {opFail(); return;}
    }

    private void opNotWordBound() {
        if (isWordAt(s) != isWordAt(s - 1)) {opFail(); return;}
    }

    private void opWordBegin() {
        if (!isWordAt(s - 1) && isWordAt(s)) {
            return;
        }
        opFail();
    }

    private void opWordEnd() {
        if (isWordAt(s - 1) && !isWordAt(s)) {
            return;
        }
        opFail();
    }

    private void opBeginBuf() {
        if (s != str) {
            opFail();
        }
    }

    private void opEndBuf() {
        if (s != end) {
            opFail();
        }
    }

    private void opBeginLine() {
        if (s == str) {
            if (isNotBol(msaOptions)) {
                opFail();
            }
            return;
        } else if (isNewLineAt(s - 1)) {
            return;
        }
        opFail();
    }

    private void opEndLine()  {
        if (s == end) {
            if (Config.USE_NEWLINE_AT_END_OF_STRING_HAS_EMPTY_LINE) {
                if (str == end || !isNewLineAt(s - 1)) {
                    if (isNotEol(msaOptions)) {
                        opFail();
                    }
                }
                return;
            }
            if (isNotEol(msaOptions)) {
                opFail();
            }
            return;
        } else if (isNewLineAt(s)) {
            return;
        }
        opFail();
    }

    private void opSemiEndBuf() {
        if (s == end) {
            if (Config.USE_NEWLINE_AT_END_OF_STRING_HAS_EMPTY_LINE) {
                if (str == end || !isNewLineAt(s - 1)) {
                    if (isNotEol(msaOptions)) {
                        opFail();
                    }
                }
                return;
            }
            if (isNotEol(msaOptions)) {
                opFail();
            }
            return;
        } else if (isNewLineAt(s) && s + 1 == end) {
            return;
        }
        opFail();
    }

    private void opBeginPosition() {
        if (s != msaStart) {
            opFail();
        }
    }

    private void opMemoryStartPush() {
        final int mem = code[ip++];
        pushMemStart(mem, s);
    }

    private void opMemoryStart() {
        final int mem = code[ip++];
        repeatStk[memStartStk + mem] = s;
    }

    private void opMemoryEndPush() {
        final int mem = code[ip++];
        pushMemEnd(mem, s);
    }

    private void opMemoryEnd() {
        final int mem = code[ip++];
        repeatStk[memEndStk + mem] = s;
    }

    private void opMemoryClear() {
        final int fromMem = code[ip++];
        final int toMem = code[ip++];
        for (int mem = fromMem; mem < toMem; mem++) {
            if (bsAt(regex.btMemStart, mem)) {
                pushMemStart(mem, INVALID_INDEX);
            } else {
                repeatStk[memStartStk + mem] = INVALID_INDEX;
            }
            if (bsAt(regex.btMemEnd, mem)) {
                pushMemEnd(mem, INVALID_INDEX);
            } else {
                repeatStk[memEndStk + mem] = INVALID_INDEX;
            }
        }
    }

    private boolean backrefInvalid(final int mem) {
        return repeatStk[memEndStk + mem] == INVALID_INDEX || repeatStk[memStartStk + mem] == INVALID_INDEX;
    }

    private int backrefStart(final int mem) {
        return bsAt(regex.btMemStart, mem) ? stack[repeatStk[memStartStk + mem]].getMemPStr() : repeatStk[memStartStk + mem];
    }

    private int backrefEnd(final int mem) {
        return bsAt(regex.btMemEnd, mem) ? stack[repeatStk[memEndStk + mem]].getMemPStr() : repeatStk[memEndStk + mem];
    }

    private void backref(final int mem, boolean ignoreCase) {
        /* if you want to remove following line,
        you should check in parse and compile time. (numMem) */
        if (mem > regex.numMem) {opFail(); return;}

        int pstart;
        final int pend;
        if (backrefInvalid(mem)) {
            pstart = pend = 0;
        } else {
            pstart = backrefStart(mem);
            pend = backrefEnd(mem);
        }

        int n = pend - pstart;
        if (s + n > range) {opFail(); return;}

        if (ignoreCase) {
            value = s;
            if (!stringCmpIC(regex.caseFoldFlag, pstart, this, n, end)) {opFail(); return;}
            s = value;
        } else {
            // STRING_CMP
            while(n-- > 0) {
                if (charAt(pstart++) != charAt(s++)) {opFail(); return;}
            }
        }
    }

    private void opBackRef1() {
        backref(1, false);
    }

    private void opBackRef2() {
        backref(2, false);
    }

    private void opBackRefN() {
        backref(code[ip++], false);
    }

    private void opBackRefNIC() {
        backref(code[ip++], true);
    }

    private void opBackRefMulti() {
        final int tlen = code[ip++];

        int i;
        loop:for (i=0; i<tlen; i++) {
            final int mem = code[ip++];
            if (backrefInvalid(mem)) {
                continue;
            }

            int pstart = backrefStart(mem);
            final int pend = backrefEnd(mem);

            int n = pend - pstart;
            if (s + n > range) {opFail(); return;}

            int swork = s;

            while (n-- > 0) {
                if (charAt(pstart++) != charAt(swork++)) {
                    continue loop;
                }
            }

            s = swork;

            ip += tlen - i  - 1; // * SIZE_MEMNUM (1)
            break; /* success */
        }
        if (i == tlen) {opFail(); return;}
    }

    private void opBackRefMultiIC() {
        final int tlen = code[ip++];

        int i;
        loop:for (i=0; i<tlen; i++) {
            final int mem = code[ip++];
            if (backrefInvalid(mem)) {
                continue;
            }

            final int pstart = backrefStart(mem);
            final int pend = backrefEnd(mem);

            final int n = pend - pstart;
            if (s + n > range) {opFail(); return;}

            value = s;
            if (!stringCmpIC(regex.caseFoldFlag, pstart, this, n, end))
             {
                continue loop; // STRING_CMP_VALUE_IC
            }
            s = value;

            ip += tlen - i  - 1; // * SIZE_MEMNUM (1)
            break;  /* success */
        }
        if (i == tlen) {opFail(); return;}
    }

    private boolean memIsInMemp(final int mem, final int num, final int mempp) {
        for (int i=0, memp = mempp; i<num; i++) {
            final int m = code[memp++];
            if (mem == m) {
                return true;
            }
        }
        return false;
    }

    // USE_BACKREF_AT_LEVEL // (s) and (end) implicit
    private boolean backrefMatchAtNestedLevel(final boolean ignoreCase, final int caseFoldFlag,
                                              final int nest, final int memNum, final int memp) {
        int pend = -1;
        int level = 0;
        int k = stk - 1;

        while (k >= 0) {
            final StackEntry e = stack[k];

            if (e.type == CALL_FRAME) {
                level--;
            } else if (e.type == RETURN) {
                level++;
            } else if (level == nest) {
                if (e.type == MEM_START) {
                    if (memIsInMemp(e.getMemNum(), memNum, memp)) {
                        final int pstart = e.getMemPStr();
                        if (pend != -1) {
                            if (pend - pstart > end - s) {
                                return false; /* or goto next_mem; */
                            }
                            int p = pstart;

                            value = s;
                            if (ignoreCase) {
                                if (!stringCmpIC(caseFoldFlag, pstart, this, pend - pstart, end)) {
                                    return false; /* or goto next_mem; */
                                }
                            } else {
                                while (p < pend) {
                                    if (charAt(p++) != charAt(value++)) {
                                        return false; /* or goto next_mem; */
                                    }
                                }
                            }
                            s = value;

                            return true;
                        }
                    }
                } else if (e.type == MEM_END) {
                    if (memIsInMemp(e.getMemNum(), memNum, memp)) {
                        pend = e.getMemPStr();
                    }
                }
            }
            k--;
        }
        return false;
    }

    private void opBackRefAtLevel() {
        final int ic      = code[ip++];
        final int level   = code[ip++];
        final int tlen    = code[ip++];

        if (backrefMatchAtNestedLevel(ic != 0, regex.caseFoldFlag, level, tlen, ip)) { // (s) and (end) implicit
            ip += tlen; // * SIZE_MEMNUM
        } else {
            {opFail(); return;}
        }
    }

    private void opNullCheckStart() {
        final int mem = code[ip++];
        pushNullCheckStart(mem, s);
    }

    private void nullCheckFound() {
        // null_check_found:
        /* empty loop founded, skip next instruction */
        switch(code[ip++]) {
        case OPCode.JUMP:
        case OPCode.PUSH:
            ip++;       // p += SIZE_RELADDR;
            break;
        case OPCode.REPEAT_INC:
        case OPCode.REPEAT_INC_NG:
        case OPCode.REPEAT_INC_SG:
        case OPCode.REPEAT_INC_NG_SG:
            ip++;        // p += SIZE_MEMNUM;
            break;
        default:
            throw new InternalException(ErrorMessages.ERR_UNEXPECTED_BYTECODE);
        } // switch
    }

    private void opNullCheckEnd() {
        final int mem = code[ip++];
        final int isNull = nullCheck(mem, s); /* mem: null check id */

        if (isNull != 0) {
            if (Config.DEBUG_MATCH) {
                Config.log.println("NULL_CHECK_END: skip  id:" + mem + ", s:" + s);
            }

            nullCheckFound();
        }
    }

    // USE_INFINITE_REPEAT_MONOMANIAC_MEM_STATUS_CHECK
    private void opNullCheckEndMemST() {
        final int mem = code[ip++];   /* mem: null check id */
        final int isNull = nullCheckMemSt(mem, s);

        if (isNull != 0) {
            if (Config.DEBUG_MATCH) {
                Config.log.println("NULL_CHECK_END_MEMST: skip  id:" + mem + ", s:" + s);
            }

            if (isNull == -1) {opFail(); return;}
            nullCheckFound();
        }
    }

    private void opJump() {
        ip += code[ip] + 1;
    }

    private void opPush() {
        final int addr = code[ip++];
        pushAlt(ip + addr, s);
    }

    private void opPop() {
        popOne();
    }

    private void opPushOrJumpExact1() {
        final int addr = code[ip++];
        // beyond string check
        if (s < range && code[ip] == charAt(s)) {
            ip++;
            pushAlt(ip + addr, s);
            return;
        }
        ip += addr + 1;
    }

    private void opPushIfPeekNext() {
        final int addr = code[ip++];
        // beyond string check
        if (s < range && code[ip] == charAt(s)) {
            ip++;
            pushAlt(ip + addr, s);
            return;
        }
        ip++;
    }

    private void opRepeat() {
        final int mem = code[ip++];   /* mem: OP_REPEAT ID */
        final int addr= code[ip++];

        // ensure1();
        repeatStk[mem] = stk;
        pushRepeat(mem, ip, s);

        if (regex.repeatRangeLo[mem] == 0) { // lower
            pushAlt(ip + addr, s);
        }
    }

    private void opRepeatNG() {
        final int mem = code[ip++];   /* mem: OP_REPEAT ID */
        final int addr= code[ip++];

        // ensure1();
        repeatStk[mem] = stk;
        pushRepeat(mem, ip, s);

        if (regex.repeatRangeLo[mem] == 0) {
            pushAlt(ip, s);
            ip += addr;
        }
    }

    /**
     * Checks whether the last iteration of the REPEAT described by the {@link StackEntry} {@code e}
     * passes the null check or not.
     */
    private boolean nullCheckRepeat(StackEntry e) {
        return e.getRepeatPStr() != s || e.getRepeatCount() < regex.repeatRangeLo[e.getRepeatNum()];
    }

    private void repeatInc(final int mem, final int si) {
        final StackEntry e = stack[si];

        if (!nullCheckRepeat(e)) {
            opFail();
            return;
        }

        int slast = e.getRepeatPStr();
        e.increaseRepeatCount();
        e.setRepeatPStr(s);

        if (e.getRepeatCount() >= regex.repeatRangeHi[mem]) {
            /* end of repeat. Nothing to do. */
        } else if (e.getRepeatCount() >= regex.repeatRangeLo[mem]) {
            pushAlt(ip, s);
            ip = e.getRepeatPCode(); /* Don't use stkp after PUSH. */
        } else {
            ip = e.getRepeatPCode();
        }
        pushRepeatInc(si, slast);
    }

    private void opRepeatInc() {
        final int mem = code[ip++];   /* mem: OP_REPEAT ID */
        final int si = repeatStk[mem];
        repeatInc(mem, si);
    }

    private void opRepeatIncSG() {
        final int mem = code[ip++];   /* mem: OP_REPEAT ID */
        final int si = getRepeat(mem);
        repeatInc(mem, si);
    }

    private void repeatIncNG(final int mem, final int si) {
        final StackEntry e = stack[si];

        if (!nullCheckRepeat(e)) {
            opFail();
            return;
        }

        int slast = e.getRepeatPStr();
        e.increaseRepeatCount();
        e.setRepeatPStr(s);

        if (e.getRepeatCount() < regex.repeatRangeHi[mem]) {
            if (e.getRepeatCount() >= regex.repeatRangeLo[mem]) {
                final int pcode = e.getRepeatPCode();
                pushRepeatInc(si, slast);
                pushAlt(pcode, s);
            } else {
                ip = e.getRepeatPCode();
                pushRepeatInc(si, slast);
            }
        } else if (e.getRepeatCount() == regex.repeatRangeHi[mem]) {
            pushRepeatInc(si, slast);
        }
    }

    private void opRepeatIncNG() {
        final int mem = code[ip++];
        final int si = repeatStk[mem];
        repeatIncNG(mem, si);
    }

    private void opRepeatIncNGSG() {
        final int mem = code[ip++];
        final int si = getRepeat(mem);
        repeatIncNG(mem, si);
    }

    private void opPushPos() {
        pushPos(s);
    }

    private void opPopPos() {
        final StackEntry e = stack[posEnd()];
        s    = e.getStatePStr();
    }

    private void opPushPosNot() {
        final int addr = code[ip++];
        pushPosNot(ip + addr, s);
    }

    private void opFailPos() {
        popTilPosNot();
        opFail();
    }

    private void opPushStopBT() {
        pushStopBT();
    }

    private void opPopStopBT() {
        stopBtEnd();
    }

    private void opLookBehind() {
        final int tlen = code[ip++];
        s = EncodingHelper.stepBack(str, s, tlen);
        if (s == -1) {opFail(); return;}
    }

    private void opPushLookBehindNot() {
        final int addr = code[ip++];
        final int tlen = code[ip++];
        final int q = EncodingHelper.stepBack(str, s, tlen);
        if (q == -1) {
            /* too short case -> success. ex. /(?<!XXX)a/.match("a")
            If you want to change to fail, replace following line. */
            ip += addr;
            // return FAIL;
        } else {
            pushLookBehindNot(ip + addr, s);
            s = q;
        }
    }

    private void opFailLookBehindNot() {
        popTilLookBehindNot();
        opFail();
    }

    private void opFail() {
        if (stack == null) {
            ip = regex.codeLength - 1;
            return;
        }


        final StackEntry e = pop();
        ip    = e.getStatePCode();
        s     = e.getStatePStr();
    }

    private int finish() {
        return bestLen;
    }
}
