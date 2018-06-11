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
import static com.oracle.truffle.regex.nashorn.regexp.joni.Option.isDynamic;
import static com.oracle.truffle.regex.nashorn.regexp.joni.Option.isIgnoreCase;
import static com.oracle.truffle.regex.nashorn.regexp.joni.Option.isMultiline;
import static com.oracle.truffle.regex.nashorn.regexp.joni.ast.QuantifierNode.isRepeatInfinite;

import com.oracle.truffle.regex.nashorn.regexp.joni.ast.AnchorNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.BackRefNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.CClassNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.ConsAltNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.EncloseNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.Node;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.QuantifierNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.ast.StringNode;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.AnchorType;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.EncloseType;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.NodeType;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.OPCode;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.OPSize;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.TargetInfo;

final class ArrayCompiler extends Compiler {
    private int[] code;
    private int codeLength;

    private char[][] templates;
    private int templateNum;

    ArrayCompiler(final Analyser analyser) {
        super(analyser);
    }

    @Override
    protected final void prepare() {
        final int codeSize = Config.USE_STRING_TEMPLATES ? 8 : ((analyser.getEnd() - analyser.getBegin()) * 2 + 2);
        code = new int[codeSize];
        codeLength = 0;
    }

    @Override
    protected final void finish() {
        addOpcode(OPCode.END);
        addOpcode(OPCode.FINISH); // for stack bottom

        regex.code = code;
        regex.codeLength = codeLength;
        regex.templates = templates;
        regex.templateNum = templateNum;
        regex.factory = MatcherFactory.DEFAULT;
    }

    @Override
    protected void compileAltNode(final ConsAltNode node) {
        ConsAltNode aln = node;
        int len = 0;

        do {
            len += compileLengthTree(aln.car);
            if (aln.cdr != null) {
                len += OPSize.PUSH + OPSize.JUMP;
            }
        } while ((aln = aln.cdr) != null);

        final int pos = codeLength + len;  /* goal position */

        aln = node;
        do {
            len = compileLengthTree(aln.car);
            if (aln.cdr != null) {
                addOpcodeRelAddr(OPCode.PUSH, len + OPSize.JUMP);
            }
            compileTree(aln.car);
            if (aln.cdr != null) {
                len = pos - (codeLength + OPSize.JUMP);
                addOpcodeRelAddr(OPCode.JUMP, len);
            }
        } while ((aln = aln.cdr) != null);
    }

    private static boolean isNeedStrLenOpExact(final int op) {
        return  op == OPCode.EXACTN || op == OPCode.EXACTN_IC;
    }

    private static boolean opTemplated(final int op) {
        return isNeedStrLenOpExact(op);
    }

    private static int selectStrOpcode(final int strLength, final boolean ignoreCase) {
        int op;

        if (ignoreCase) {
            switch(strLength) {
            case 1: op = OPCode.EXACT1_IC; break;
            default:op = OPCode.EXACTN_IC; break;
            } // switch
        } else {
            switch (strLength) {
            case 1: op = OPCode.EXACT1; break;
            case 2: op = OPCode.EXACT2; break;
            case 3: op = OPCode.EXACT3; break;
            case 4: op = OPCode.EXACT4; break;
            case 5: op = OPCode.EXACT5; break;
            default:op = OPCode.EXACTN; break;
            } // inner switch
        }
        return op;
    }

    private void compileTreeEmptyCheck(final Node node, final int emptyInfo) {
        final int savedNumNullCheck = regex.numNullCheck;

        if (emptyInfo != 0) {
            addOpcode(OPCode.NULL_CHECK_START);
            addMemNum(regex.numNullCheck); /* NULL CHECK ID */
            regex.numNullCheck++;
        }

        compileTree(node);

        if (emptyInfo != 0) {
            switch (emptyInfo) {
            case TargetInfo.IS_EMPTY:
                addOpcode(OPCode.NULL_CHECK_END);
                break;
            case TargetInfo.IS_EMPTY_MEM:
                addOpcode(OPCode.NULL_CHECK_END_MEMST);
                break;
            default:
                break;
            } // switch

            addMemNum(savedNumNullCheck); /* NULL CHECK ID */
        }
    }

    @SuppressWarnings("unused")
    private static int addCompileStringlength(final char[] chars, final int p, final int strLength, final boolean ignoreCase) {
        final int op = selectStrOpcode(strLength, ignoreCase);
        int len = OPSize.OPCODE;

        if (Config.USE_STRING_TEMPLATES && opTemplated(op)) {
            // string length, template index, template string pointer
            len += OPSize.LENGTH + OPSize.INDEX + OPSize.INDEX;
        } else {
            if (isNeedStrLenOpExact(op)) {
                len += OPSize.LENGTH;
            }
            len += strLength;
        }
        return len;
    }

    @Override
    protected final void addCompileString(final char[] chars, final int p, final int strLength, final boolean ignoreCase) {
        final int op = selectStrOpcode(strLength, ignoreCase);
        addOpcode(op);

        if (isNeedStrLenOpExact(op)) {
            addLength(strLength);
        }

        if (Config.USE_STRING_TEMPLATES && opTemplated(op)) {
            addInt(templateNum);
            addInt(p);
            addTemplate(chars);
        } else {
            addChars(chars, p, strLength);
        }
    }

    private static int compileLengthStringNode(final Node node) {
        final StringNode sn = (StringNode)node;
        if (sn.length() <= 0) {
            return 0;
        }
        final boolean ambig = sn.isAmbig();

        int p, prev;
        p = prev = sn.p;
        final int end = sn.end;
        final char[] chars = sn.chars;
        p++;

        int slen = 1;
        int rlen = 0;

        while (p < end) {
            slen++;
            p++;
        }
        final int r = addCompileStringlength(chars, prev, slen, ambig);
        rlen += r;
        return rlen;
    }

    private static int compileLengthStringRawNode(final StringNode sn) {
        if (sn.length() <= 0) {
            return 0;
        }
        return addCompileStringlength(sn.chars, sn.p, sn.length(), false);
    }

    private void addMultiByteCClass(final CodeRangeBuffer mbuf) {
        addLength(mbuf.used);
        addInts(mbuf.p, mbuf.used);
    }

    private static int compileLengthCClassNode(final CClassNode cc) {
        if (cc.isShare()) {
            return OPSize.OPCODE + OPSize.POINTER;
        }

        int len;
        if (cc.mbuf == null) {
            len = OPSize.OPCODE + BitSet.BITSET_SIZE;
        } else {
            if (cc.bs.isEmpty()) {
                len = OPSize.OPCODE;
            } else {
                len = OPSize.OPCODE + BitSet.BITSET_SIZE;
            }

            len += OPSize.LENGTH + cc.mbuf.used;
        }
        return len;
    }

    @Override
    protected void compileCClassNode(final CClassNode cc) {
        if (cc.isShare()) { // shared char class
            addOpcode(OPCode.CCLASS_NODE);
            addPointer(cc);
            return;
        }

        if (cc.mbuf == null) {
            if (cc.isNot()) {
                addOpcode(OPCode.CCLASS_NOT);
            } else {
                addOpcode(OPCode.CCLASS);
            }
            addInts(cc.bs.bits, BitSet.BITSET_SIZE); // add_bitset
        } else {
            if (cc.bs.isEmpty()) {
                if (cc.isNot()) {
                    addOpcode(OPCode.CCLASS_MB_NOT);
                } else {
                    addOpcode(OPCode.CCLASS_MB);
                }
                addMultiByteCClass(cc.mbuf);
            } else {
                if (cc.isNot()) {
                    addOpcode(OPCode.CCLASS_MIX_NOT);
                } else {
                    addOpcode(OPCode.CCLASS_MIX);
                }
                // store the bit set and mbuf themself!
                addInts(cc.bs.bits, BitSet.BITSET_SIZE); // add_bitset
                addMultiByteCClass(cc.mbuf);
            }
        }
    }

    @Override
    protected void compileAnyCharNode() {
        if (isMultiline(regex.options)) {
            addOpcode(OPCode.ANYCHAR_ML);
        } else {
            addOpcode(OPCode.ANYCHAR);
        }
    }

    @Override
    protected void compileBackrefNode(final BackRefNode node) {
        if (isIgnoreCase(regex.options)) {
            addOpcode(OPCode.BACKREFN_IC);
            addMemNum(node.backRef);
        } else {
            switch (node.backRef) {
                case 1:
                    addOpcode(OPCode.BACKREF1);
                    break;
                case 2:
                    addOpcode(OPCode.BACKREF2);
                    break;
                default:
                    addOpcode(OPCode.BACKREFN);
                    addOpcode(node.backRef);
                    break;
            } // switch
        }
    }

    private static final int REPEAT_RANGE_ALLOC = 8;
    private void entryRepeatRange(final int id, final int lower, final int upper) {
        if (regex.repeatRangeLo == null) {
            regex.repeatRangeLo = new int[REPEAT_RANGE_ALLOC];
            regex.repeatRangeHi = new int[REPEAT_RANGE_ALLOC];
        } else if (id >= regex.repeatRangeLo.length){
            int[]tmp = new int[regex.repeatRangeLo.length + REPEAT_RANGE_ALLOC];
            System.arraycopy(regex.repeatRangeLo, 0, tmp, 0, regex.repeatRangeLo.length);
            regex.repeatRangeLo = tmp;
            tmp = new int[regex.repeatRangeHi.length + REPEAT_RANGE_ALLOC];
            System.arraycopy(regex.repeatRangeHi, 0, tmp, 0, regex.repeatRangeHi.length);
            regex.repeatRangeHi = tmp;
        }

        regex.repeatRangeLo[id] = lower;
        regex.repeatRangeHi[id] = isRepeatInfinite(upper) ? 0x7fffffff : upper;
    }

    private void compileRangeRepeatNode(final QuantifierNode qn, final int targetLen) {
        final int numRepeat = regex.numRepeat;
        addOpcode(qn.greedy ? OPCode.REPEAT : OPCode.REPEAT_NG);
        addMemNum(numRepeat); /* OP_REPEAT ID */
        regex.numRepeat++;
        addRelAddr(targetLen + OPSize.REPEAT_INC);

        entryRepeatRange(numRepeat, qn.lower, qn.upper);

        compileTreeWithMemoryClear(qn.target);

        if (qn.isInRepeat()) {
            addOpcode(qn.greedy ? OPCode.REPEAT_INC_SG : OPCode.REPEAT_INC_NG_SG);
        } else {
            addOpcode(qn.greedy ? OPCode.REPEAT_INC : OPCode.REPEAT_INC_NG);
        }

        addMemNum(numRepeat); /* OP_REPEAT ID */
    }

    private static final int QUANTIFIER_EXPAND_LIMIT_SIZE   = 50; // was 50

    @SuppressWarnings("unused")
    private static boolean cknOn(final int ckn) {
        return ckn > 0;
    }

    private void addMemoryClear(final Range clearCaptureGroups) {
        if (!clearCaptureGroups.empty) {
            addOpcode(OPCode.MEMORY_CLEAR);
            addMemNum(clearCaptureGroups.from);
            addMemNum(clearCaptureGroups.to);
        }
    }

    private void compileTreeWithMemoryClear(final Node node) {
        addMemoryClear(findEnclosedCaptureGroups(node));
        compileTree(node);
    }

    private void compileTreeEmptyCheckWithMemoryClear(final Node node, final int emptyInfo) {
        addMemoryClear(findEnclosedCaptureGroups(node));
        compileTreeEmptyCheck(node, emptyInfo);
    }

    private void compileTreeNTimesWithMemoryClear(final Node node, final int n) {
        Range enclosedCaptureGroups = findEnclosedCaptureGroups(node);
        for (int i=0; i<n; i++) {
            addMemoryClear(enclosedCaptureGroups);
            compileTree(node);
        }
    }

    private int compileLengthTreeWithMemoryClear(final Node node) {
        final Range enclosedCaptureGroups = findEnclosedCaptureGroups(node);
        if (enclosedCaptureGroups.empty) {
            return compileLengthTree(node);
        } else {
            return compileLengthTree(node) + OPSize.MEMORY_CLEAR;
        }
    }

    private static long calculateSmallRangeExpandedLength(QuantifierNode qn, int tlen, int modTLen) {
        int n = qn.upper - qn.lower;
        boolean needsJumps = qn.targetEmptyInfo == TargetInfo.IS_EMPTY;
        return (long)qn.lower * tlen + (long)n * (OPSize.PUSH + modTLen + (needsJumps ? 2 * OPSize.JUMP : 0));
    }

    private int compileNonCECLengthQuantifierNode(final QuantifierNode qn) {
        final boolean infinite = isRepeatInfinite(qn.upper);
        final int emptyInfo = qn.targetEmptyInfo;

        final int tlen = compileLengthTreeWithMemoryClear(qn.target);

        /* anychar repeat */
        if (qn.isAnyCharStar() && tlen * (long)qn.lower <= QUANTIFIER_EXPAND_LIMIT_SIZE) {
            if (qn.nextHeadExact != null) {
                return OPSize.ANYCHAR_STAR_PEEK_NEXT + tlen * qn.lower;
            }
            return OPSize.ANYCHAR_STAR + tlen * qn.lower;
        }

        int modTLen = 0;
        if (emptyInfo != 0) {
            modTLen = tlen + (OPSize.NULL_CHECK_START + OPSize.NULL_CHECK_END);
        } else {
            modTLen = tlen;
        }

        int len;
        if (infinite && ((qn.lower == 1 && emptyInfo == TargetInfo.ISNOT_EMPTY) || tlen * (long)qn.lower <= QUANTIFIER_EXPAND_LIMIT_SIZE)) {
            if (qn.lower == 1 && emptyInfo == TargetInfo.ISNOT_EMPTY && tlen > QUANTIFIER_EXPAND_LIMIT_SIZE) {
                len = OPSize.JUMP;
            } else {
                len = tlen * qn.lower;
            }

            if (qn.greedy) {
                if (qn.headExact != null) {
                    len += OPSize.PUSH_OR_JUMP_EXACT1 + modTLen + OPSize.JUMP;
                } else if (qn.nextHeadExact != null) {
                    len += OPSize.PUSH_IF_PEEK_NEXT + modTLen + OPSize.JUMP;
                } else {
                    len += OPSize.PUSH + modTLen + OPSize.JUMP;
                }
            } else {
                len += OPSize.JUMP + modTLen + OPSize.PUSH;
            }

        } else if (qn.upper == 0 && qn.isRefered) { /* /(?<n>..){0}/ */
            len = OPSize.JUMP + tlen;
        } else if (!infinite && qn.greedy &&
                  (qn.upper == 1 || calculateSmallRangeExpandedLength(qn, tlen, modTLen) <= QUANTIFIER_EXPAND_LIMIT_SIZE)) {
            len = (int)calculateSmallRangeExpandedLength(qn, tlen, modTLen);
        } else if (!qn.greedy && qn.upper == 1 && qn.lower == 0) { /* '??' */
            len = OPSize.PUSH + OPSize.JUMP + tlen;
        } else {
            len = OPSize.REPEAT + tlen + OPSize.REPEAT_INC;
        }
        return len;
    }

    @Override
    protected void compileNonCECQuantifierNode(final QuantifierNode qn) {
        final boolean infinite = isRepeatInfinite(qn.upper);
        final int emptyInfo = qn.targetEmptyInfo;

        final int tlen = compileLengthTreeWithMemoryClear(qn.target);

        if (qn.isAnyCharStar() && tlen * (long)qn.lower <= QUANTIFIER_EXPAND_LIMIT_SIZE) {
            compileTreeNTimesWithMemoryClear(qn.target, qn.lower);
            if (qn.nextHeadExact != null) {
                if (isMultiline(regex.options)) {
                    addOpcode(OPCode.ANYCHAR_ML_STAR_PEEK_NEXT);
                } else {
                    addOpcode(OPCode.ANYCHAR_STAR_PEEK_NEXT);
                }
                final StringNode sn = (StringNode)qn.nextHeadExact;
                addChars(sn.chars, sn.p, 1);
                return;
            }
            if (isMultiline(regex.options)) {
                addOpcode(OPCode.ANYCHAR_ML_STAR);
            } else {
                addOpcode(OPCode.ANYCHAR_STAR);
            }
            return;
        }

        int modTLen;
        if (emptyInfo != 0) {
            modTLen = tlen + (OPSize.NULL_CHECK_START + OPSize.NULL_CHECK_END);
        } else {
            modTLen = tlen;
        }
        if (infinite && ((qn.lower == 1 && emptyInfo == TargetInfo.ISNOT_EMPTY) || tlen * (long)qn.lower <= QUANTIFIER_EXPAND_LIMIT_SIZE)) {
            // If emptyInfo == TargetInfo.ISNOT_EMPTY, then we know that the target of this
            // quantifier can never be empty and we can therefore use the optimization below which
            // skips over the first PUSH in the loop in order to enforce the lower bound qn.lower
            // equal to 1.
            if (qn.lower == 1 && emptyInfo == TargetInfo.ISNOT_EMPTY && tlen > QUANTIFIER_EXPAND_LIMIT_SIZE) {
                if (qn.greedy) {
                    if (qn.headExact != null) {
                        addOpcodeRelAddr(OPCode.JUMP, OPSize.PUSH_OR_JUMP_EXACT1);
                    } else if (qn.nextHeadExact != null) {
                        addOpcodeRelAddr(OPCode.JUMP, OPSize.PUSH_IF_PEEK_NEXT);
                    } else {
                        addOpcodeRelAddr(OPCode.JUMP, OPSize.PUSH);
                    }
                } else {
                    addOpcodeRelAddr(OPCode.JUMP, OPSize.JUMP);
                }
            } else {
                compileTreeNTimesWithMemoryClear(qn.target, qn.lower);
            }

            if (qn.greedy) {
                if (qn.headExact != null) {
                    addOpcodeRelAddr(OPCode.PUSH_OR_JUMP_EXACT1, modTLen + OPSize.JUMP);
                    final StringNode sn = (StringNode)qn.headExact;
                    addChars(sn.chars, sn.p, 1);
                    compileTreeEmptyCheckWithMemoryClear(qn.target, emptyInfo);
                    addOpcodeRelAddr(OPCode.JUMP, -(modTLen + OPSize.JUMP + OPSize.PUSH_OR_JUMP_EXACT1));
                } else if (qn.nextHeadExact != null) {
                    addOpcodeRelAddr(OPCode.PUSH_IF_PEEK_NEXT, modTLen + OPSize.JUMP);
                    final StringNode sn = (StringNode)qn.nextHeadExact;
                    addChars(sn.chars, sn.p, 1);
                    compileTreeEmptyCheckWithMemoryClear(qn.target, emptyInfo);
                    addOpcodeRelAddr(OPCode.JUMP, -(modTLen + OPSize.JUMP + OPSize.PUSH_IF_PEEK_NEXT));
                } else {
                    addOpcodeRelAddr(OPCode.PUSH, modTLen + OPSize.JUMP);
                    compileTreeEmptyCheckWithMemoryClear(qn.target, emptyInfo);
                    addOpcodeRelAddr(OPCode.JUMP, -(modTLen + OPSize.JUMP + OPSize.PUSH));
                }
            } else {
                addOpcodeRelAddr(OPCode.JUMP, modTLen);
                compileTreeEmptyCheckWithMemoryClear(qn.target, emptyInfo);
                addOpcodeRelAddr(OPCode.PUSH, -(modTLen + OPSize.PUSH));
            }
        } else if (qn.upper == 0 && qn.isRefered) { /* /(?<n>..){0}/ */
            addOpcodeRelAddr(OPCode.JUMP, tlen);
            compileTreeWithMemoryClear(qn.target);
        } else if (!infinite && qn.greedy &&
                  (qn.upper == 1 || calculateSmallRangeExpandedLength(qn, tlen, modTLen) <= QUANTIFIER_EXPAND_LIMIT_SIZE)) {
            final int n = qn.upper - qn.lower;
            compileTreeNTimesWithMemoryClear(qn.target, qn.lower);

            // The emptiness check included when emptyInfo == TargetInfo.IS_EMPTY needs to be
            // followed by a jump or a similar control instruction. In case the check is failed
            // (the contents were empty), the following jump is ignored.
            boolean needsJumps = emptyInfo == TargetInfo.IS_EMPTY;
            for (int i=0; i<n; i++) {
                addOpcodeRelAddr(OPCode.PUSH, (n - i) * (OPSize.PUSH + modTLen + (needsJumps ? 2 * OPSize.JUMP : 0)) - OPSize.PUSH);
                compileTreeEmptyCheckWithMemoryClear(qn.target, emptyInfo);
                if (needsJumps) {
                    // If we pass the emptiness check, skip the next jump.
                    addOpcodeRelAddr(OPCode.JUMP, OPSize.JUMP);
                    // If we fail the emptiness check, jump to the end of this expression (i.e. do
                    // not match any more iterations of this range).
                    addOpcodeRelAddr(OPCode.JUMP, (n - i - 1) * (OPSize.PUSH + modTLen + 2 * OPSize.JUMP));
                }
            }
        } else if (!qn.greedy && qn.upper == 1 && qn.lower == 0) { /* '??' */
            addOpcodeRelAddr(OPCode.PUSH, OPSize.JUMP);
            addOpcodeRelAddr(OPCode.JUMP, tlen);
            compileTreeWithMemoryClear(qn.target);
        } else {
            compileRangeRepeatNode(qn, tlen);
        }
    }

    private int compileLengthOptionNode(final EncloseNode node) {
        final int prev = regex.options;
        regex.options = node.option;
        final int tlen = compileLengthTree(node.target);
        regex.options = prev;

        if (isDynamic(prev ^ node.option)) {
            return OPSize.SET_OPTION_PUSH + OPSize.SET_OPTION + OPSize.FAIL + tlen + OPSize.SET_OPTION;
        }
        return tlen;
    }

    @Override
    protected void compileOptionNode(final EncloseNode node) {
        final int prev = regex.options;

        if (isDynamic(prev ^ node.option)) {
            addOpcodeOption(OPCode.SET_OPTION_PUSH, node.option);
            addOpcodeOption(OPCode.SET_OPTION, prev);
            addOpcode(OPCode.FAIL);
        }

        regex.options = node.option;
        compileTree(node.target);
        regex.options = prev;

        if (isDynamic(prev ^ node.option)) {
            addOpcodeOption(OPCode.SET_OPTION, prev);
        }
    }

    private int compileLengthEncloseNode(final EncloseNode node) {
        if (node.isOption()) {
            return compileLengthOptionNode(node);
        }

        int tlen;
        if (node.target != null) {
            tlen = compileLengthTree(node.target);
        } else {
            tlen = 0;
        }

        int len;
        switch (node.type) {
        case EncloseType.MEMORY:
            if (bsAt(regex.btMemStart, node.regNum)) {
                len = OPSize.MEMORY_START_PUSH;
            } else {
                len = OPSize.MEMORY_START;
            }
            len += tlen + (bsAt(regex.btMemEnd, node.regNum) ? OPSize.MEMORY_END_PUSH : OPSize.MEMORY_END);
            break;

        case EncloseType.STOP_BACKTRACK:
            if (node.isStopBtSimpleRepeat()) {
                final QuantifierNode qn = (QuantifierNode)node.target;
                tlen = compileLengthTree(qn.target);
                // The following is safe w.r.t. to numerical overflow given the argument in the
                // companion method #compileEncloseNode.
                len = tlen * qn.lower + OPSize.PUSH + tlen + OPSize.POP + OPSize.JUMP;
            } else {
                len = OPSize.PUSH_STOP_BT + tlen + OPSize.POP_STOP_BT;
            }
            break;

        default:
            newInternalException(ERR_PARSER_BUG);
            return 0; // not reached
        } // switch
        return len;
    }

    @Override
    protected void compileEncloseNode(final EncloseNode node) {
        int len;
        switch (node.type) {
        case EncloseType.MEMORY:
            if (bsAt(regex.btMemStart, node.regNum)) {
                addOpcode(OPCode.MEMORY_START_PUSH);
            } else {
                addOpcode(OPCode.MEMORY_START);
            }

            addMemNum(node.regNum);
            compileTree(node.target);

            if (bsAt(regex.btMemEnd, node.regNum)) {
                addOpcode(OPCode.MEMORY_END_PUSH);
            } else {
                addOpcode(OPCode.MEMORY_END);
            }
            addMemNum(node.regNum);
            break;

        case EncloseType.STOP_BACKTRACK:
            if (node.isStopBtSimpleRepeat()) {
                final QuantifierNode qn = (QuantifierNode)node.target;

                // The following is safe w.r.t. running out of memory since STOP_BACKTRACK nodes
                // can appear in the AST only in the three following cases:
                //   1) As part of the (?>...) committed choice expression.
                //   2) As part of a possessive quantifier (++, ?+, {n,m}+...).
                //   3) As an optimization introduced by Analyser#nextSetup.
                // Cases 1 and 2 are not supported in ECMAScript's regular expressions. The use of
                // Case 3 is guarded by qn.lower <= 1 and so qn.lower cannot have dangerously high
                // values.
                compileTreeNTimes(qn.target, qn.lower);

                len = compileLengthTree(qn.target);
                addOpcodeRelAddr(OPCode.PUSH, len + OPSize.POP + OPSize.JUMP);
                compileTree(qn.target);
                addOpcode(OPCode.POP);
                addOpcodeRelAddr(OPCode.JUMP, -(OPSize.PUSH + len + OPSize.POP + OPSize.JUMP));
            } else {
                addOpcode(OPCode.PUSH_STOP_BT);
                compileTree(node.target);
                addOpcode(OPCode.POP_STOP_BT);
            }
            break;

        default:
            newInternalException(ERR_PARSER_BUG);
            break;
        } // switch
    }

    private int compileLengthAnchorNode(final AnchorNode node) {
        int tlen;
        if (node.target != null) {
            tlen = compileLengthTree(node.target);
        } else {
            tlen = 0;
        }

        int len;
        switch (node.type) {
        case AnchorType.PREC_READ:
            len = OPSize.PUSH_POS + tlen + OPSize.POP_POS;
            break;

        case AnchorType.PREC_READ_NOT:
            len = OPSize.PUSH_POS_NOT + tlen + OPSize.FAIL_POS;
            break;

        case AnchorType.LOOK_BEHIND:
            len = OPSize.LOOK_BEHIND + tlen;
            break;

        case AnchorType.LOOK_BEHIND_NOT:
            len = OPSize.PUSH_LOOK_BEHIND_NOT + tlen + OPSize.FAIL_LOOK_BEHIND_NOT;
            break;

        default:
            len = OPSize.OPCODE;
            break;
        } // switch
        return len;
    }

    @Override
    protected void compileAnchorNode(final AnchorNode node) {
        int len;
        int n;

        switch (node.type) {
        case AnchorType.BEGIN_BUF:          addOpcode(OPCode.BEGIN_BUF);            break;
        case AnchorType.END_BUF:            addOpcode(OPCode.END_BUF);              break;
        case AnchorType.BEGIN_LINE:         addOpcode(OPCode.BEGIN_LINE);           break;
        case AnchorType.END_LINE:           addOpcode(OPCode.END_LINE);             break;
        case AnchorType.SEMI_END_BUF:       addOpcode(OPCode.SEMI_END_BUF);         break;
        case AnchorType.BEGIN_POSITION:     addOpcode(OPCode.BEGIN_POSITION);       break;

        case AnchorType.WORD_BOUND:
            addOpcode(OPCode.WORD_BOUND);
            break;

        case AnchorType.NOT_WORD_BOUND:
            addOpcode(OPCode.NOT_WORD_BOUND);
            break;

        case AnchorType.WORD_BEGIN:
            if (Config.USE_WORD_BEGIN_END) {
                addOpcode(OPCode.WORD_BEGIN);
            }
            break;

        case AnchorType.WORD_END:
            if (Config.USE_WORD_BEGIN_END) {
                addOpcode(OPCode.WORD_END);
            }
            break;

        case AnchorType.PREC_READ:
            addOpcode(OPCode.PUSH_POS);
            compileTree(node.target);
            addOpcode(OPCode.POP_POS);
            break;

        case AnchorType.PREC_READ_NOT:
            len = compileLengthTree(node.target);
            addOpcodeRelAddr(OPCode.PUSH_POS_NOT, len + OPSize.FAIL_POS);
            compileTree(node.target);
            addOpcode(OPCode.FAIL_POS);
            break;

        case AnchorType.LOOK_BEHIND:
            addOpcode(OPCode.LOOK_BEHIND);
            if (node.charLength < 0) {
                n = analyser.getCharLengthTree(node.target);
                if (analyser.returnCode != 0) {
                    newSyntaxException(ERR_INVALID_LOOK_BEHIND_PATTERN);
                }
            } else {
                n = node.charLength;
            }
            addLength(n);
            compileTree(node.target);
            break;

        case AnchorType.LOOK_BEHIND_NOT:
            len = compileLengthTree(node.target);
            addOpcodeRelAddr(OPCode.PUSH_LOOK_BEHIND_NOT, len + OPSize.FAIL_LOOK_BEHIND_NOT);
            if (node.charLength < 0) {
                n = analyser.getCharLengthTree(node.target);
                if (analyser.returnCode != 0) {
                    newSyntaxException(ERR_INVALID_LOOK_BEHIND_PATTERN);
                }
            } else {
                n = node.charLength;
            }
            addLength(n);
            compileTree(node.target);
            addOpcode(OPCode.FAIL_LOOK_BEHIND_NOT);
            break;

        default:
            newInternalException(ERR_PARSER_BUG);
        } // switch
    }

    private int compileLengthTree(final Node node) {
        int len = 0;

        switch (node.getType()) {
        case NodeType.LIST:
            ConsAltNode lin = (ConsAltNode)node;
            do {
                len += compileLengthTree(lin.car);
            } while ((lin = lin.cdr) != null);
            break;

        case NodeType.ALT:
            ConsAltNode aln = (ConsAltNode)node;
            int n = 0;
            do {
                len += compileLengthTree(aln.car);
                n++;
            } while ((aln = aln.cdr) != null);
            len += (OPSize.PUSH + OPSize.JUMP) * (n - 1);
            break;

        case NodeType.STR:
            final StringNode sn = (StringNode)node;
            if (sn.isRaw()) {
                len = compileLengthStringRawNode(sn);
            } else {
                len = compileLengthStringNode(sn);
            }
            break;

        case NodeType.CCLASS:
            len = compileLengthCClassNode((CClassNode)node);
            break;

        case NodeType.CTYPE:
        case NodeType.CANY:
            len = OPSize.OPCODE;
            break;

        case NodeType.BREF:
            final BackRefNode br = (BackRefNode)node;

            len = ((!isIgnoreCase(regex.options) && br.backRef <= 2)
                    ? OPSize.OPCODE : (OPSize.OPCODE + OPSize.MEMNUM));
            break;

        case NodeType.QTFR:
            len = compileNonCECLengthQuantifierNode((QuantifierNode)node);
            break;

        case NodeType.ENCLOSE:
            len = compileLengthEncloseNode((EncloseNode)node);
            break;

        case NodeType.ANCHOR:
            len = compileLengthAnchorNode((AnchorNode)node);
            break;

        default:
            newInternalException(ERR_PARSER_BUG);

        } //switch
        return len;
    }

    private void ensure(final int size) {
        if (size >= code.length) {
            int length = code.length << 1;
            while (length <= size) {
                length <<= 1;
            }
            final int[]tmp = new int[length];
            System.arraycopy(code, 0, tmp, 0, code.length);
            code = tmp;
        }
    }

    private void addInt(final int i) {
        if (codeLength >= code.length) {
            final int[]tmp = new int[code.length << 1];
            System.arraycopy(code, 0, tmp, 0, code.length);
            code = tmp;
        }
        code[codeLength++] = i;
    }

    void setInt(final int i, final int offset) {
        ensure(offset);
        regex.code[offset] = i;
    }

    private void addObject(final Object o) {
        if (regex.operands == null) {
            regex.operands = new Object[4];
        } else if (regex.operandLength >= regex.operands.length) {
            final Object[]tmp = new Object[regex.operands.length << 1];
            System.arraycopy(regex.operands, 0, tmp, 0, regex.operands.length);
            regex.operands = tmp;
        }
        addInt(regex.operandLength);
        regex.operands[regex.operandLength++] = o;
    }

    private void addChars(final char[] chars, final int pp ,final int length) {
        ensure(codeLength + length);
        int p = pp;
        final int end = p + length;

        while (p < end) {
            code[codeLength++] = chars[p++];
        }
    }

    private void addInts(final int[]ints, final int length) {
        ensure(codeLength + length);
        System.arraycopy(ints, 0, code, codeLength, length);
        codeLength += length;
    }

    private void addOpcode(final int opcode) {
        addInt(opcode);

        switch(opcode) {
        case OPCode.ANYCHAR_STAR:
        case OPCode.ANYCHAR_ML_STAR:
        case OPCode.ANYCHAR_STAR_PEEK_NEXT:
        case OPCode.ANYCHAR_ML_STAR_PEEK_NEXT:
        case OPCode.STATE_CHECK_ANYCHAR_STAR:
        case OPCode.STATE_CHECK_ANYCHAR_ML_STAR:
        case OPCode.MEMORY_START_PUSH:
        case OPCode.MEMORY_END_PUSH:
        case OPCode.NULL_CHECK_START:
        case OPCode.NULL_CHECK_END_MEMST_PUSH:
        case OPCode.PUSH:
        case OPCode.STATE_CHECK_PUSH:
        case OPCode.STATE_CHECK_PUSH_OR_JUMP:
        case OPCode.STATE_CHECK:
        case OPCode.PUSH_OR_JUMP_EXACT1:
        case OPCode.PUSH_IF_PEEK_NEXT:
        case OPCode.REPEAT:
        case OPCode.REPEAT_NG:
        case OPCode.REPEAT_INC_SG:
        case OPCode.REPEAT_INC_NG:
        case OPCode.REPEAT_INC_NG_SG:
        case OPCode.PUSH_POS:
        case OPCode.PUSH_POS_NOT:
        case OPCode.PUSH_STOP_BT:
        case OPCode.PUSH_LOOK_BEHIND_NOT:
        case OPCode.CALL:
        case OPCode.RETURN: // it will appear only with CALL though
            regex.stackNeeded = true;
            break;
        default:
            break;
        }
    }

    @SuppressWarnings("unused")
    private void addStateCheckNum(final int num) {
        addInt(num);
    }

    private void addRelAddr(final int addr) {
        addInt(addr);
    }

    @SuppressWarnings("unused")
    private void addAbsAddr(final int addr) {
        addInt(addr);
    }

    private void addLength(final int length) {
        addInt(length);
    }

    private void addMemNum(final int num) {
        addInt(num);
    }

    private void addPointer(final Object o) {
        addObject(o);
    }

    private void addOption(final int option) {
        addInt(option);
    }

    private void addOpcodeRelAddr(final int opcode, final int addr) {
        addOpcode(opcode);
        addRelAddr(addr);
    }

    private void addOpcodeOption(final int opcode, final int option) {
        addOpcode(opcode);
        addOption(option);
    }

    private void addTemplate(final char[] chars) {
        if (templateNum == 0) {
            templates = new char[2][];
        } else if (templateNum == templates.length) {
            final char[][] tmp = new char[templateNum * 2][];
            System.arraycopy(templates, 0, tmp, 0, templateNum);
            templates = tmp;
        }
        templates[templateNum++] = chars;
    }
}
