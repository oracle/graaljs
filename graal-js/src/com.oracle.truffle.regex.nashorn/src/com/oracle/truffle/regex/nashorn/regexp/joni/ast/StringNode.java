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

import com.oracle.truffle.regex.nashorn.regexp.joni.EncodingHelper;
import com.oracle.truffle.regex.nashorn.regexp.joni.constants.StringType;

public final class StringNode extends Node implements StringType {

    private static final int NODE_STR_MARGIN = 16;
    private static final int NODE_STR_BUF_SIZE = 24;
    public static final StringNode EMPTY = new StringNode(null, Integer.MAX_VALUE, Integer.MAX_VALUE);

    public char[] chars;
    public int p;
    public int end;

    public int flag;

    public StringNode() {
        this.chars = new char[NODE_STR_BUF_SIZE];
    }

    public StringNode(final char[] chars, final int p, final int end) {
        this.chars = chars;
        this.p = p;
        this.end = end;
        setShared();
    }

    public StringNode(final char c) {
        this();
        chars[end++] = c;
    }

    /* Ensure there is ahead bytes available in node's buffer
     * (assumes that the node is not shared)
     */
    public void ensure(final int ahead) {
        final int len = (end - p) + ahead;
        if (len >= chars.length) {
            final char[] tmp = new char[len + NODE_STR_MARGIN];
            System.arraycopy(chars, p, tmp, 0, end - p);
            chars = tmp;
        }
    }

    /* COW and/or ensure there is ahead bytes available in node's buffer
     */
    private void modifyEnsure(final int ahead) {
        if (isShared()) {
            final int len = (end - p) + ahead;
            final char[] tmp = new char[len + NODE_STR_MARGIN];
            System.arraycopy(chars, p, tmp, 0, end - p);
            chars = tmp;
            end = end - p;
            p = 0;
            clearShared();
        } else {
            ensure(ahead);
        }
    }

    @Override
    public int getType() {
        return STR;
    }

    @Override
    public String getName() {
        return "String";
    }

    @Override
    public String toString(final int level) {
        final StringBuilder value = new StringBuilder();
        value.append("\n  bytes: '");
        for (int i=p; i<end; i++) {
            if (chars[i] >= 0x20 && chars[i] < 0x7f) {
                value.append(chars[i]);
            } else {
                value.append(String.format("[0x%04x]", (int)chars[i]));
            }
        }
        value.append("'");
        return value.toString();
    }

    public int length() {
        return end - p;
    }

    public StringNode splitLastChar() {
        StringNode n = null;

        if (end > p) {
            final int prev = EncodingHelper.prevCharHead(p, end);
            if (prev != -1 && prev > p) { /* can be splitted. */
                n = new StringNode(chars, prev, end);
                if (isRaw()) n.setRaw();
                end = prev;
            }
        }
        return n;
    }

    public boolean canBeSplit() {
        return end > p && 1 < (end - p);
    }

    public void set(final char[] chars, final int p, final int end) {
        this.chars = chars;
        this.p = p;
        this.end = end;
        setShared();
    }

    public void cat(final char[] cat, final int catP, final int catEnd) {
        final int len = catEnd - catP;
        modifyEnsure(len);
        System.arraycopy(cat, catP, chars, end, len);
        end += len;
    }

    public void cat(final char c) {
        modifyEnsure(1);
        chars[end++] = c;
    }

    public void catCode(final int code) {
        cat((char)code);
    }

    public void clear() {
        if (chars.length > NODE_STR_BUF_SIZE) chars = new char[NODE_STR_BUF_SIZE];
        flag = 0;
        p = end = 0;
    }

    public void setRaw() {
        flag |= NSTR_RAW;
    }

    public void clearRaw() {
        flag &= ~NSTR_RAW;
    }

    public boolean isRaw() {
        return (flag & NSTR_RAW) != 0;
    }

    public void setAmbig() {
        flag |= NSTR_AMBIG;
    }

    public void clearAmbig() {
        flag &= ~NSTR_AMBIG;
    }

    public boolean isAmbig() {
        return (flag & NSTR_AMBIG) != 0;
    }

    public void setDontGetOptInfo() {
        flag |= NSTR_DONT_GET_OPT_INFO;
    }

    public void clearDontGetOptInfo() {
        flag &= ~NSTR_DONT_GET_OPT_INFO;
    }

    public boolean isDontGetOptInfo() {
        return (flag & NSTR_DONT_GET_OPT_INFO) != 0;
    }

    public void setShared() {
        flag |= NSTR_SHARED;
    }

    public void clearShared() {
        flag &= ~NSTR_SHARED;
    }

    public boolean isShared() {
        return (flag & NSTR_SHARED) != 0;
    }
}
