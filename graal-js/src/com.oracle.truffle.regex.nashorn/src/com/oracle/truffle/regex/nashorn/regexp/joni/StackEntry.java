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

final class StackEntry {
    int type;
    private int E1, E2, E3, E4;

    // first union member
    /* byte code position */
    void setStatePCode(final int pcode) {
        E1 = pcode;
    }
    int getStatePCode() {
        return E1;
    }
    /* string position */
    void setStatePStr(final int pstr) {
        E2 = pstr;
    }
    int getStatePStr() {
        return E2;
    }

    // second union member
    /* for OP_REPEAT_INC, OP_REPEAT_INC_NG */
    void setRepeatCount(final int count) {
        E1 = count;
    }
    int getRepeatCount() {
        return E1;
    }
    void decreaseRepeatCount() {
        E1--;
    }
    void increaseRepeatCount() {
        E1++;
    }
    /* byte code position (head of repeated target) */
    void setRepeatPCode(final int pcode) {
        E2 = pcode;
    }
    int getRepeatPCode() {
        return E2;
    }
    /* repeat id */
    void setRepeatNum(final int num) {
        E3 = num;
    }
    int getRepeatNum() {
        return E3;
    }
    /* the beginning of the match of the last iteration of the repeat,
       used to perform null checks */
    void setRepeatPStr(final int pstr) {
        E4 = pstr;
    }
    int getRepeatPStr() {
        return E4;
    }

    // third union member
    /* index of stack */ /*int repeat_inc struct*/
    void setRepeatIncSi(final int si) {
        E1 = si;
    }
    int getRepeatIncSi() {
        return E1;
    }
    /* the start of the match of the last iteration of the repeat when this REPEAT_INC was pushed,
       used to restore its original value when undoing/unwinding this entry from the stack */
    void setRepeatIncPStr(final int pstr) {
        E2 = pstr;
    }
    int getRepeatIncPStr() {
        return E2;
    }

    // fourth union member
    /* memory num */
    void setMemNum(final int num) {
        E1 = num;
    }
    int getMemNum() {
        return E1;
    }
    /* start/end position */
    void setMemPstr(final int pstr) {
        E2 = pstr;
    }
    int getMemPStr() {
        return E2;
    }

    /* Following information is set, if this stack type is MEM-START */
    /* prev. info (for backtrack  "(...)*" ) */
    void setMemStart(final int start) {
        E3 = start;
    }
    int getMemStart() {
        return E3;
    }
    /* prev. info (for backtrack  "(...)*" ) */
    void setMemEnd(final int end) {
        E4 = end;
    }
    int getMemEnd() {
        return E4;
    }

    // fifth union member
    /* null check id */
    void setNullCheckNum(final int num) {
        E1 = num;
    }
    int getNullCheckNum() {
        return E1;
    }
    /* start position */
    void setNullCheckPStr(final int pstr) {
        E2 = pstr;
    }
    int getNullCheckPStr() {
        return E2;
    }

    // sixth union member
    /* byte code position */
    void setCallFrameRetAddr(final int addr) {
        E1 = addr;
    }
    int getCallFrameRetAddr() {
        return E1;
    }
    /* null check id */
    void setCallFrameNum(final int num) {
        E2 = num;
    }
    int getCallFrameNum() {
        return E2;
    }
    /* string position */
    void setCallFramePStr(final int pstr) {
        E3 = pstr;
    }
    int getCallFramePStr() {
        return E3;
    }
}
