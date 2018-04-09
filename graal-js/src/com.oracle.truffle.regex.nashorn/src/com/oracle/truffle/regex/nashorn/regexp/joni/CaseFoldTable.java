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
// GENERATED FILE. DO NOT EDIT.

package com.oracle.truffle.regex.nashorn.regexp.joni;

public final class CaseFoldTable {

    static final int RANGE_TYPE_SELF = 0;
    static final int RANGE_TYPE_SET = 1;
    static final int RANGE_TYPE_DELTA_POSITIVE = 2;
    static final int RANGE_TYPE_DELTA_NEGATIVE = 3;
    static final int RANGE_TYPE_ALTERNATING_ALIGNED = 4;
    static final int RANGE_TYPE_ALTERNATING_UNALIGNED = 5;

    static final int RANGE_SIZE = 4;

    private final int[] rangeTable;
    private final int[][] setTable;

    public CaseFoldTable(int[] rangeTable, int[][] setTable) {
        this.rangeTable = rangeTable;
        this.setTable = setTable;
    }

    public int[] getRangeTable() {
        return rangeTable;
    }

    public int[][] getSetTable() {
        return setTable;
    }

    public int getRangeCount() {
        return rangeTable.length / RANGE_SIZE;
    }

    public int getRangeStart(int rangeIndex) {
        return rangeTable[rangeIndex * RANGE_SIZE + 0];
    }

    public int getRangeEnd(int rangeIndex) {
        return rangeTable[rangeIndex * RANGE_SIZE + 1];
    }

    public int getRangeValue(int rangeIndex) {
        return rangeTable[rangeIndex * RANGE_SIZE + 2];
    }

    public int getRangeType(int rangeIndex) {
        return rangeTable[rangeIndex * RANGE_SIZE + 3];
    }

    static final class UCS2 {

        private static final int[][] UCS2_CHARACTER_SET_TABLE = {
                        {0x01c4, 0x01c5, 0x01c6},
                        {0x01c7, 0x01c8, 0x01c9},
                        {0x01ca, 0x01cb, 0x01cc},
                        {0x01f1, 0x01f2, 0x01f3},
                        {0x0392, 0x03b2, 0x03d0},
                        {0x0395, 0x03b5, 0x03f5},
                        {0x0398, 0x03b8, 0x03d1},
                        {0x0345, 0x0399, 0x03b9, 0x1fbe},
                        {0x039a, 0x03ba, 0x03f0},
                        {0x00b5, 0x039c, 0x03bc},
                        {0x03a0, 0x03c0, 0x03d6},
                        {0x03a1, 0x03c1, 0x03f1},
                        {0x03a3, 0x03c2, 0x03c3},
                        {0x03a6, 0x03c6, 0x03d5},
                        {0x1e60, 0x1e61, 0x1e9b},
        }; // 15

        private static final int[] UCS2_RANGE_TABLE = {
                        0x0000, 0x0040, 0x0000, RANGE_TYPE_SELF,
                        0x0041, 0x005a, 0x0020, RANGE_TYPE_DELTA_POSITIVE,
                        0x005b, 0x0060, 0x0000, RANGE_TYPE_SELF,
                        0x0061, 0x007a, 0x0020, RANGE_TYPE_DELTA_NEGATIVE,
                        0x007b, 0x00b4, 0x0000, RANGE_TYPE_SELF,
                        0x00b5, 0x00b5, 0x0009, RANGE_TYPE_SET,
                        0x00b6, 0x00bf, 0x0000, RANGE_TYPE_SELF,
                        0x00c0, 0x00d6, 0x0020, RANGE_TYPE_DELTA_POSITIVE,
                        0x00d7, 0x00d7, 0x0000, RANGE_TYPE_SELF,
                        0x00d8, 0x00de, 0x0020, RANGE_TYPE_DELTA_POSITIVE,
                        0x00df, 0x00df, 0x0000, RANGE_TYPE_SELF,
                        0x00e0, 0x00f6, 0x0020, RANGE_TYPE_DELTA_NEGATIVE,
                        0x00f7, 0x00f7, 0x0000, RANGE_TYPE_SELF,
                        0x00f8, 0x00fe, 0x0020, RANGE_TYPE_DELTA_NEGATIVE,
                        0x00ff, 0x00ff, 0x0079, RANGE_TYPE_DELTA_POSITIVE,
                        0x0100, 0x012f, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x0130, 0x0131, 0x0000, RANGE_TYPE_SELF,
                        0x0132, 0x0137, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x0138, 0x0138, 0x0000, RANGE_TYPE_SELF,
                        0x0139, 0x0148, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x0149, 0x0149, 0x0000, RANGE_TYPE_SELF,
                        0x014a, 0x0177, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x0178, 0x0178, 0x0079, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0179, 0x017e, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x017f, 0x017f, 0x0000, RANGE_TYPE_SELF,
                        0x0180, 0x0180, 0x00c3, RANGE_TYPE_DELTA_POSITIVE,
                        0x0181, 0x0181, 0x00d2, RANGE_TYPE_DELTA_POSITIVE,
                        0x0182, 0x0185, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x0186, 0x0186, 0x00ce, RANGE_TYPE_DELTA_POSITIVE,
                        0x0187, 0x0188, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x0189, 0x018a, 0x00cd, RANGE_TYPE_DELTA_POSITIVE,
                        0x018b, 0x018c, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x018d, 0x018d, 0x0000, RANGE_TYPE_SELF,
                        0x018e, 0x018e, 0x004f, RANGE_TYPE_DELTA_POSITIVE,
                        0x018f, 0x018f, 0x00ca, RANGE_TYPE_DELTA_POSITIVE,
                        0x0190, 0x0190, 0x00cb, RANGE_TYPE_DELTA_POSITIVE,
                        0x0191, 0x0192, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x0193, 0x0193, 0x00cd, RANGE_TYPE_DELTA_POSITIVE,
                        0x0194, 0x0194, 0x00cf, RANGE_TYPE_DELTA_POSITIVE,
                        0x0195, 0x0195, 0x0061, RANGE_TYPE_DELTA_POSITIVE,
                        0x0196, 0x0196, 0x00d3, RANGE_TYPE_DELTA_POSITIVE,
                        0x0197, 0x0197, 0x00d1, RANGE_TYPE_DELTA_POSITIVE,
                        0x0198, 0x0199, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x019a, 0x019a, 0x00a3, RANGE_TYPE_DELTA_POSITIVE,
                        0x019b, 0x019b, 0x0000, RANGE_TYPE_SELF,
                        0x019c, 0x019c, 0x00d3, RANGE_TYPE_DELTA_POSITIVE,
                        0x019d, 0x019d, 0x00d5, RANGE_TYPE_DELTA_POSITIVE,
                        0x019e, 0x019e, 0x0082, RANGE_TYPE_DELTA_POSITIVE,
                        0x019f, 0x019f, 0x00d6, RANGE_TYPE_DELTA_POSITIVE,
                        0x01a0, 0x01a5, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x01a6, 0x01a6, 0x00da, RANGE_TYPE_DELTA_POSITIVE,
                        0x01a7, 0x01a8, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x01a9, 0x01a9, 0x00da, RANGE_TYPE_DELTA_POSITIVE,
                        0x01aa, 0x01ab, 0x0000, RANGE_TYPE_SELF,
                        0x01ac, 0x01ad, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x01ae, 0x01ae, 0x00da, RANGE_TYPE_DELTA_POSITIVE,
                        0x01af, 0x01b0, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x01b1, 0x01b2, 0x00d9, RANGE_TYPE_DELTA_POSITIVE,
                        0x01b3, 0x01b6, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x01b7, 0x01b7, 0x00db, RANGE_TYPE_DELTA_POSITIVE,
                        0x01b8, 0x01b9, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x01ba, 0x01bb, 0x0000, RANGE_TYPE_SELF,
                        0x01bc, 0x01bd, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x01be, 0x01be, 0x0000, RANGE_TYPE_SELF,
                        0x01bf, 0x01bf, 0x0038, RANGE_TYPE_DELTA_POSITIVE,
                        0x01c0, 0x01c3, 0x0000, RANGE_TYPE_SELF,
                        0x01c4, 0x01c6, 0x0000, RANGE_TYPE_SET,
                        0x01c7, 0x01c9, 0x0001, RANGE_TYPE_SET,
                        0x01ca, 0x01cc, 0x0002, RANGE_TYPE_SET,
                        0x01cd, 0x01dc, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x01dd, 0x01dd, 0x004f, RANGE_TYPE_DELTA_NEGATIVE,
                        0x01de, 0x01ef, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x01f0, 0x01f0, 0x0000, RANGE_TYPE_SELF,
                        0x01f1, 0x01f3, 0x0003, RANGE_TYPE_SET,
                        0x01f4, 0x01f5, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x01f6, 0x01f6, 0x0061, RANGE_TYPE_DELTA_NEGATIVE,
                        0x01f7, 0x01f7, 0x0038, RANGE_TYPE_DELTA_NEGATIVE,
                        0x01f8, 0x021f, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x0220, 0x0220, 0x0082, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0221, 0x0221, 0x0000, RANGE_TYPE_SELF,
                        0x0222, 0x0233, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x0234, 0x0239, 0x0000, RANGE_TYPE_SELF,
                        0x023a, 0x023a, 0x2a2b, RANGE_TYPE_DELTA_POSITIVE,
                        0x023b, 0x023c, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x023d, 0x023d, 0x00a3, RANGE_TYPE_DELTA_NEGATIVE,
                        0x023e, 0x023e, 0x2a28, RANGE_TYPE_DELTA_POSITIVE,
                        0x023f, 0x0240, 0x2a3f, RANGE_TYPE_DELTA_POSITIVE,
                        0x0241, 0x0242, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x0243, 0x0243, 0x00c3, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0244, 0x0244, 0x0045, RANGE_TYPE_DELTA_POSITIVE,
                        0x0245, 0x0245, 0x0047, RANGE_TYPE_DELTA_POSITIVE,
                        0x0246, 0x024f, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x0250, 0x0250, 0x2a1f, RANGE_TYPE_DELTA_POSITIVE,
                        0x0251, 0x0251, 0x2a1c, RANGE_TYPE_DELTA_POSITIVE,
                        0x0252, 0x0252, 0x2a1e, RANGE_TYPE_DELTA_POSITIVE,
                        0x0253, 0x0253, 0x00d2, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0254, 0x0254, 0x00ce, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0255, 0x0255, 0x0000, RANGE_TYPE_SELF,
                        0x0256, 0x0257, 0x00cd, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0258, 0x0258, 0x0000, RANGE_TYPE_SELF,
                        0x0259, 0x0259, 0x00ca, RANGE_TYPE_DELTA_NEGATIVE,
                        0x025a, 0x025a, 0x0000, RANGE_TYPE_SELF,
                        0x025b, 0x025b, 0x00cb, RANGE_TYPE_DELTA_NEGATIVE,
                        0x025c, 0x025c, 0xa54f, RANGE_TYPE_DELTA_POSITIVE,
                        0x025d, 0x025f, 0x0000, RANGE_TYPE_SELF,
                        0x0260, 0x0260, 0x00cd, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0261, 0x0261, 0xa54b, RANGE_TYPE_DELTA_POSITIVE,
                        0x0262, 0x0262, 0x0000, RANGE_TYPE_SELF,
                        0x0263, 0x0263, 0x00cf, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0264, 0x0264, 0x0000, RANGE_TYPE_SELF,
                        0x0265, 0x0265, 0xa528, RANGE_TYPE_DELTA_POSITIVE,
                        0x0266, 0x0266, 0xa544, RANGE_TYPE_DELTA_POSITIVE,
                        0x0267, 0x0267, 0x0000, RANGE_TYPE_SELF,
                        0x0268, 0x0268, 0x00d1, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0269, 0x0269, 0x00d3, RANGE_TYPE_DELTA_NEGATIVE,
                        0x026a, 0x026a, 0x0000, RANGE_TYPE_SELF,
                        0x026b, 0x026b, 0x29f7, RANGE_TYPE_DELTA_POSITIVE,
                        0x026c, 0x026c, 0xa541, RANGE_TYPE_DELTA_POSITIVE,
                        0x026d, 0x026e, 0x0000, RANGE_TYPE_SELF,
                        0x026f, 0x026f, 0x00d3, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0270, 0x0270, 0x0000, RANGE_TYPE_SELF,
                        0x0271, 0x0271, 0x29fd, RANGE_TYPE_DELTA_POSITIVE,
                        0x0272, 0x0272, 0x00d5, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0273, 0x0274, 0x0000, RANGE_TYPE_SELF,
                        0x0275, 0x0275, 0x00d6, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0276, 0x027c, 0x0000, RANGE_TYPE_SELF,
                        0x027d, 0x027d, 0x29e7, RANGE_TYPE_DELTA_POSITIVE,
                        0x027e, 0x027f, 0x0000, RANGE_TYPE_SELF,
                        0x0280, 0x0280, 0x00da, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0281, 0x0282, 0x0000, RANGE_TYPE_SELF,
                        0x0283, 0x0283, 0x00da, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0284, 0x0286, 0x0000, RANGE_TYPE_SELF,
                        0x0287, 0x0287, 0xa52a, RANGE_TYPE_DELTA_POSITIVE,
                        0x0288, 0x0288, 0x00da, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0289, 0x0289, 0x0045, RANGE_TYPE_DELTA_NEGATIVE,
                        0x028a, 0x028b, 0x00d9, RANGE_TYPE_DELTA_NEGATIVE,
                        0x028c, 0x028c, 0x0047, RANGE_TYPE_DELTA_NEGATIVE,
                        0x028d, 0x0291, 0x0000, RANGE_TYPE_SELF,
                        0x0292, 0x0292, 0x00db, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0293, 0x029d, 0x0000, RANGE_TYPE_SELF,
                        0x029e, 0x029e, 0xa512, RANGE_TYPE_DELTA_POSITIVE,
                        0x029f, 0x0344, 0x0000, RANGE_TYPE_SELF,
                        0x0345, 0x0345, 0x0007, RANGE_TYPE_SET,
                        0x0346, 0x036f, 0x0000, RANGE_TYPE_SELF,
                        0x0370, 0x0373, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x0374, 0x0375, 0x0000, RANGE_TYPE_SELF,
                        0x0376, 0x0377, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x0378, 0x037a, 0x0000, RANGE_TYPE_SELF,
                        0x037b, 0x037d, 0x0082, RANGE_TYPE_DELTA_POSITIVE,
                        0x037e, 0x037e, 0x0000, RANGE_TYPE_SELF,
                        0x037f, 0x037f, 0x0074, RANGE_TYPE_DELTA_POSITIVE,
                        0x0380, 0x0385, 0x0000, RANGE_TYPE_SELF,
                        0x0386, 0x0386, 0x0026, RANGE_TYPE_DELTA_POSITIVE,
                        0x0387, 0x0387, 0x0000, RANGE_TYPE_SELF,
                        0x0388, 0x038a, 0x0025, RANGE_TYPE_DELTA_POSITIVE,
                        0x038b, 0x038b, 0x0000, RANGE_TYPE_SELF,
                        0x038c, 0x038c, 0x0040, RANGE_TYPE_DELTA_POSITIVE,
                        0x038d, 0x038d, 0x0000, RANGE_TYPE_SELF,
                        0x038e, 0x038f, 0x003f, RANGE_TYPE_DELTA_POSITIVE,
                        0x0390, 0x0390, 0x0000, RANGE_TYPE_SELF,
                        0x0391, 0x0391, 0x0020, RANGE_TYPE_DELTA_POSITIVE,
                        0x0392, 0x0392, 0x0004, RANGE_TYPE_SET,
                        0x0393, 0x0394, 0x0020, RANGE_TYPE_DELTA_POSITIVE,
                        0x0395, 0x0395, 0x0005, RANGE_TYPE_SET,
                        0x0396, 0x0397, 0x0020, RANGE_TYPE_DELTA_POSITIVE,
                        0x0398, 0x0398, 0x0006, RANGE_TYPE_SET,
                        0x0399, 0x0399, 0x0007, RANGE_TYPE_SET,
                        0x039a, 0x039a, 0x0008, RANGE_TYPE_SET,
                        0x039b, 0x039b, 0x0020, RANGE_TYPE_DELTA_POSITIVE,
                        0x039c, 0x039c, 0x0009, RANGE_TYPE_SET,
                        0x039d, 0x039f, 0x0020, RANGE_TYPE_DELTA_POSITIVE,
                        0x03a0, 0x03a0, 0x000a, RANGE_TYPE_SET,
                        0x03a1, 0x03a1, 0x000b, RANGE_TYPE_SET,
                        0x03a2, 0x03a2, 0x0000, RANGE_TYPE_SELF,
                        0x03a3, 0x03a3, 0x000c, RANGE_TYPE_SET,
                        0x03a4, 0x03a5, 0x0020, RANGE_TYPE_DELTA_POSITIVE,
                        0x03a6, 0x03a6, 0x000d, RANGE_TYPE_SET,
                        0x03a7, 0x03ab, 0x0020, RANGE_TYPE_DELTA_POSITIVE,
                        0x03ac, 0x03ac, 0x0026, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03ad, 0x03af, 0x0025, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03b0, 0x03b0, 0x0000, RANGE_TYPE_SELF,
                        0x03b1, 0x03b1, 0x0020, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03b2, 0x03b2, 0x0004, RANGE_TYPE_SET,
                        0x03b3, 0x03b4, 0x0020, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03b5, 0x03b5, 0x0005, RANGE_TYPE_SET,
                        0x03b6, 0x03b7, 0x0020, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03b8, 0x03b8, 0x0006, RANGE_TYPE_SET,
                        0x03b9, 0x03b9, 0x0007, RANGE_TYPE_SET,
                        0x03ba, 0x03ba, 0x0008, RANGE_TYPE_SET,
                        0x03bb, 0x03bb, 0x0020, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03bc, 0x03bc, 0x0009, RANGE_TYPE_SET,
                        0x03bd, 0x03bf, 0x0020, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03c0, 0x03c0, 0x000a, RANGE_TYPE_SET,
                        0x03c1, 0x03c1, 0x000b, RANGE_TYPE_SET,
                        0x03c2, 0x03c3, 0x000c, RANGE_TYPE_SET,
                        0x03c4, 0x03c5, 0x0020, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03c6, 0x03c6, 0x000d, RANGE_TYPE_SET,
                        0x03c7, 0x03cb, 0x0020, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03cc, 0x03cc, 0x0040, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03cd, 0x03ce, 0x003f, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03cf, 0x03cf, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x03d0, 0x03d0, 0x0004, RANGE_TYPE_SET,
                        0x03d1, 0x03d1, 0x0006, RANGE_TYPE_SET,
                        0x03d2, 0x03d4, 0x0000, RANGE_TYPE_SELF,
                        0x03d5, 0x03d5, 0x000d, RANGE_TYPE_SET,
                        0x03d6, 0x03d6, 0x000a, RANGE_TYPE_SET,
                        0x03d7, 0x03d7, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03d8, 0x03ef, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x03f0, 0x03f0, 0x0008, RANGE_TYPE_SET,
                        0x03f1, 0x03f1, 0x000b, RANGE_TYPE_SET,
                        0x03f2, 0x03f2, 0x0007, RANGE_TYPE_DELTA_POSITIVE,
                        0x03f3, 0x03f3, 0x0074, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03f4, 0x03f4, 0x0000, RANGE_TYPE_SELF,
                        0x03f5, 0x03f5, 0x0005, RANGE_TYPE_SET,
                        0x03f6, 0x03f6, 0x0000, RANGE_TYPE_SELF,
                        0x03f7, 0x03f8, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x03f9, 0x03f9, 0x0007, RANGE_TYPE_DELTA_NEGATIVE,
                        0x03fa, 0x03fb, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x03fc, 0x03fc, 0x0000, RANGE_TYPE_SELF,
                        0x03fd, 0x03ff, 0x0082, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0400, 0x040f, 0x0050, RANGE_TYPE_DELTA_POSITIVE,
                        0x0410, 0x042f, 0x0020, RANGE_TYPE_DELTA_POSITIVE,
                        0x0430, 0x044f, 0x0020, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0450, 0x045f, 0x0050, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0460, 0x0481, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x0482, 0x0489, 0x0000, RANGE_TYPE_SELF,
                        0x048a, 0x04bf, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x04c0, 0x04c0, 0x000f, RANGE_TYPE_DELTA_POSITIVE,
                        0x04c1, 0x04ce, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x04cf, 0x04cf, 0x000f, RANGE_TYPE_DELTA_NEGATIVE,
                        0x04d0, 0x052f, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x0530, 0x0530, 0x0000, RANGE_TYPE_SELF,
                        0x0531, 0x0556, 0x0030, RANGE_TYPE_DELTA_POSITIVE,
                        0x0557, 0x0560, 0x0000, RANGE_TYPE_SELF,
                        0x0561, 0x0586, 0x0030, RANGE_TYPE_DELTA_NEGATIVE,
                        0x0587, 0x109f, 0x0000, RANGE_TYPE_SELF,
                        0x10a0, 0x10c5, 0x1c60, RANGE_TYPE_DELTA_POSITIVE,
                        0x10c6, 0x10c6, 0x0000, RANGE_TYPE_SELF,
                        0x10c7, 0x10c7, 0x1c60, RANGE_TYPE_DELTA_POSITIVE,
                        0x10c8, 0x10cc, 0x0000, RANGE_TYPE_SELF,
                        0x10cd, 0x10cd, 0x1c60, RANGE_TYPE_DELTA_POSITIVE,
                        0x10ce, 0x1d78, 0x0000, RANGE_TYPE_SELF,
                        0x1d79, 0x1d79, 0x8a04, RANGE_TYPE_DELTA_POSITIVE,
                        0x1d7a, 0x1d7c, 0x0000, RANGE_TYPE_SELF,
                        0x1d7d, 0x1d7d, 0x0ee6, RANGE_TYPE_DELTA_POSITIVE,
                        0x1d7e, 0x1dff, 0x0000, RANGE_TYPE_SELF,
                        0x1e00, 0x1e5f, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x1e60, 0x1e61, 0x000e, RANGE_TYPE_SET,
                        0x1e62, 0x1e95, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x1e96, 0x1e9a, 0x0000, RANGE_TYPE_SELF,
                        0x1e9b, 0x1e9b, 0x000e, RANGE_TYPE_SET,
                        0x1e9c, 0x1e9f, 0x0000, RANGE_TYPE_SELF,
                        0x1ea0, 0x1eff, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x1f00, 0x1f07, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f08, 0x1f0f, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1f10, 0x1f15, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f16, 0x1f17, 0x0000, RANGE_TYPE_SELF,
                        0x1f18, 0x1f1d, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1f1e, 0x1f1f, 0x0000, RANGE_TYPE_SELF,
                        0x1f20, 0x1f27, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f28, 0x1f2f, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1f30, 0x1f37, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f38, 0x1f3f, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1f40, 0x1f45, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f46, 0x1f47, 0x0000, RANGE_TYPE_SELF,
                        0x1f48, 0x1f4d, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1f4e, 0x1f50, 0x0000, RANGE_TYPE_SELF,
                        0x1f51, 0x1f51, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f52, 0x1f52, 0x0000, RANGE_TYPE_SELF,
                        0x1f53, 0x1f53, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f54, 0x1f54, 0x0000, RANGE_TYPE_SELF,
                        0x1f55, 0x1f55, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f56, 0x1f56, 0x0000, RANGE_TYPE_SELF,
                        0x1f57, 0x1f57, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f58, 0x1f58, 0x0000, RANGE_TYPE_SELF,
                        0x1f59, 0x1f59, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1f5a, 0x1f5a, 0x0000, RANGE_TYPE_SELF,
                        0x1f5b, 0x1f5b, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1f5c, 0x1f5c, 0x0000, RANGE_TYPE_SELF,
                        0x1f5d, 0x1f5d, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1f5e, 0x1f5e, 0x0000, RANGE_TYPE_SELF,
                        0x1f5f, 0x1f5f, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1f60, 0x1f67, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f68, 0x1f6f, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1f70, 0x1f71, 0x004a, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f72, 0x1f75, 0x0056, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f76, 0x1f77, 0x0064, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f78, 0x1f79, 0x0080, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f7a, 0x1f7b, 0x0070, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f7c, 0x1f7d, 0x007e, RANGE_TYPE_DELTA_POSITIVE,
                        0x1f7e, 0x1faf, 0x0000, RANGE_TYPE_SELF,
                        0x1fb0, 0x1fb1, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1fb2, 0x1fb7, 0x0000, RANGE_TYPE_SELF,
                        0x1fb8, 0x1fb9, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1fba, 0x1fbb, 0x004a, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1fbc, 0x1fbd, 0x0000, RANGE_TYPE_SELF,
                        0x1fbe, 0x1fbe, 0x0007, RANGE_TYPE_SET,
                        0x1fbf, 0x1fc7, 0x0000, RANGE_TYPE_SELF,
                        0x1fc8, 0x1fcb, 0x0056, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1fcc, 0x1fcf, 0x0000, RANGE_TYPE_SELF,
                        0x1fd0, 0x1fd1, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1fd2, 0x1fd7, 0x0000, RANGE_TYPE_SELF,
                        0x1fd8, 0x1fd9, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1fda, 0x1fdb, 0x0064, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1fdc, 0x1fdf, 0x0000, RANGE_TYPE_SELF,
                        0x1fe0, 0x1fe1, 0x0008, RANGE_TYPE_DELTA_POSITIVE,
                        0x1fe2, 0x1fe4, 0x0000, RANGE_TYPE_SELF,
                        0x1fe5, 0x1fe5, 0x0007, RANGE_TYPE_DELTA_POSITIVE,
                        0x1fe6, 0x1fe7, 0x0000, RANGE_TYPE_SELF,
                        0x1fe8, 0x1fe9, 0x0008, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1fea, 0x1feb, 0x0070, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1fec, 0x1fec, 0x0007, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1fed, 0x1ff7, 0x0000, RANGE_TYPE_SELF,
                        0x1ff8, 0x1ff9, 0x0080, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1ffa, 0x1ffb, 0x007e, RANGE_TYPE_DELTA_NEGATIVE,
                        0x1ffc, 0x2131, 0x0000, RANGE_TYPE_SELF,
                        0x2132, 0x2132, 0x001c, RANGE_TYPE_DELTA_POSITIVE,
                        0x2133, 0x214d, 0x0000, RANGE_TYPE_SELF,
                        0x214e, 0x214e, 0x001c, RANGE_TYPE_DELTA_NEGATIVE,
                        0x214f, 0x215f, 0x0000, RANGE_TYPE_SELF,
                        0x2160, 0x216f, 0x0010, RANGE_TYPE_DELTA_POSITIVE,
                        0x2170, 0x217f, 0x0010, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2180, 0x2182, 0x0000, RANGE_TYPE_SELF,
                        0x2183, 0x2184, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x2185, 0x24b5, 0x0000, RANGE_TYPE_SELF,
                        0x24b6, 0x24cf, 0x001a, RANGE_TYPE_DELTA_POSITIVE,
                        0x24d0, 0x24e9, 0x001a, RANGE_TYPE_DELTA_NEGATIVE,
                        0x24ea, 0x2bff, 0x0000, RANGE_TYPE_SELF,
                        0x2c00, 0x2c2e, 0x0030, RANGE_TYPE_DELTA_POSITIVE,
                        0x2c2f, 0x2c2f, 0x0000, RANGE_TYPE_SELF,
                        0x2c30, 0x2c5e, 0x0030, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2c5f, 0x2c5f, 0x0000, RANGE_TYPE_SELF,
                        0x2c60, 0x2c61, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x2c62, 0x2c62, 0x29f7, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2c63, 0x2c63, 0x0ee6, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2c64, 0x2c64, 0x29e7, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2c65, 0x2c65, 0x2a2b, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2c66, 0x2c66, 0x2a28, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2c67, 0x2c6c, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x2c6d, 0x2c6d, 0x2a1c, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2c6e, 0x2c6e, 0x29fd, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2c6f, 0x2c6f, 0x2a1f, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2c70, 0x2c70, 0x2a1e, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2c71, 0x2c71, 0x0000, RANGE_TYPE_SELF,
                        0x2c72, 0x2c73, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x2c74, 0x2c74, 0x0000, RANGE_TYPE_SELF,
                        0x2c75, 0x2c76, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x2c77, 0x2c7d, 0x0000, RANGE_TYPE_SELF,
                        0x2c7e, 0x2c7f, 0x2a3f, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2c80, 0x2ce3, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x2ce4, 0x2cea, 0x0000, RANGE_TYPE_SELF,
                        0x2ceb, 0x2cee, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0x2cef, 0x2cf1, 0x0000, RANGE_TYPE_SELF,
                        0x2cf2, 0x2cf3, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0x2cf4, 0x2cff, 0x0000, RANGE_TYPE_SELF,
                        0x2d00, 0x2d25, 0x1c60, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2d26, 0x2d26, 0x0000, RANGE_TYPE_SELF,
                        0x2d27, 0x2d27, 0x1c60, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2d28, 0x2d2c, 0x0000, RANGE_TYPE_SELF,
                        0x2d2d, 0x2d2d, 0x1c60, RANGE_TYPE_DELTA_NEGATIVE,
                        0x2d2e, 0xa63f, 0x0000, RANGE_TYPE_SELF,
                        0xa640, 0xa66d, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0xa66e, 0xa67f, 0x0000, RANGE_TYPE_SELF,
                        0xa680, 0xa69b, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0xa69c, 0xa721, 0x0000, RANGE_TYPE_SELF,
                        0xa722, 0xa72f, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0xa730, 0xa731, 0x0000, RANGE_TYPE_SELF,
                        0xa732, 0xa76f, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0xa770, 0xa778, 0x0000, RANGE_TYPE_SELF,
                        0xa779, 0xa77c, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0xa77d, 0xa77d, 0x8a04, RANGE_TYPE_DELTA_NEGATIVE,
                        0xa77e, 0xa787, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0xa788, 0xa78a, 0x0000, RANGE_TYPE_SELF,
                        0xa78b, 0xa78c, 0x0000, RANGE_TYPE_ALTERNATING_UNALIGNED,
                        0xa78d, 0xa78d, 0xa528, RANGE_TYPE_DELTA_NEGATIVE,
                        0xa78e, 0xa78f, 0x0000, RANGE_TYPE_SELF,
                        0xa790, 0xa793, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0xa794, 0xa795, 0x0000, RANGE_TYPE_SELF,
                        0xa796, 0xa7a9, 0x0000, RANGE_TYPE_ALTERNATING_ALIGNED,
                        0xa7aa, 0xa7aa, 0xa544, RANGE_TYPE_DELTA_NEGATIVE,
                        0xa7ab, 0xa7ab, 0xa54f, RANGE_TYPE_DELTA_NEGATIVE,
                        0xa7ac, 0xa7ac, 0xa54b, RANGE_TYPE_DELTA_NEGATIVE,
                        0xa7ad, 0xa7ad, 0xa541, RANGE_TYPE_DELTA_NEGATIVE,
                        0xa7ae, 0xa7af, 0x0000, RANGE_TYPE_SELF,
                        0xa7b0, 0xa7b0, 0xa512, RANGE_TYPE_DELTA_NEGATIVE,
                        0xa7b1, 0xa7b1, 0xa52a, RANGE_TYPE_DELTA_NEGATIVE,
                        0xa7b2, 0xff20, 0x0000, RANGE_TYPE_SELF,
                        0xff21, 0xff3a, 0x0020, RANGE_TYPE_DELTA_POSITIVE,
                        0xff3b, 0xff40, 0x0000, RANGE_TYPE_SELF,
                        0xff41, 0xff5a, 0x0020, RANGE_TYPE_DELTA_NEGATIVE,
                        0xff5b, 0xffff, 0x0000, RANGE_TYPE_SELF,
        }; // 391

        private static final CaseFoldTable SINGLETON = new CaseFoldTable(UCS2_RANGE_TABLE, UCS2_CHARACTER_SET_TABLE);

        static CaseFoldTable instance() {
            return SINGLETON;
        }
    }
}
