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

final class OptMapInfo {

    final MinMaxLen mmd = new MinMaxLen();          /* info position */
    final OptAnchorInfo anchor = new OptAnchorInfo();

    int value;                                      /* weighted value */
    final byte map[] = new byte[Config.CHAR_TABLE_SIZE];

    void clear() {
        mmd.clear();
        anchor.clear();
        value = 0;
        for (int i=0; i<map.length; i++) {
            map[i] = 0;
        }
    }

    void copy(final OptMapInfo other) {
        mmd.copy(other.mmd);
        anchor.copy(other.anchor);
        value = other.value;
        //for(int i=0; i<map.length; i++) map[i] = other.map[i];
        System.arraycopy(other.map, 0, map, 0, other.map.length);
    }

    void addChar(final int c) {
        final int c_ = c & 0xff;
        if (map[c_] == 0) {
            map[c_] = 1;
            value += positionValue(c_);
        }
    }

    @SuppressWarnings("unused")
    void addCharAmb(final char[] chars, final int p, final int end, final int caseFoldFlag) {
        addChar(chars[p]);

        final char[]items = EncodingHelper.caseFoldCodesByString(caseFoldFlag & ~Config.INTERNAL_ENC_CASE_FOLD_MULTI_CHAR, chars[p]);

        for (int i=0; i<items.length; i++) {
            addChar(items[i]);
        }
    }

    // select_opt_map_info
    private static final int z = 1<<15; /* 32768: something big value */
    void select(final OptMapInfo alt) {
        if (alt.value == 0) {
            return;
        }
        if (value == 0) {
            copy(alt);
            return;
        }

        final int v1 = z / value;
        final int v2 = z /alt.value;

        if (mmd.compareDistanceValue(alt.mmd, v1, v2) > 0) {
            copy(alt);
        }
    }

    // alt_merge_opt_map_info
    void altMerge(final OptMapInfo other) {
        /* if (! is_equal_mml(&to->mmd, &add->mmd)) return ; */
        if (value == 0) {
            return;
        }
        if (other.value == 0 || mmd.max < other.mmd.max) {
            clear();
            return;
        }

        mmd.altMerge(other.mmd);

        int val = 0;
        for (int i=0; i<Config.CHAR_TABLE_SIZE; i++) {
            if (other.map[i] != 0) {
                map[i] = 1;
            }
            if (map[i] != 0) {
                val += positionValue(i);
            }
        }

        value = val;
        anchor.altMerge(other.anchor);
    }

    static final short ByteValTable[] = {
        5,  1,  1,  1,  1,  1,  1,  1,  1, 10, 10,  1,  1, 10,  1,  1,
        1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
       12,  4,  7,  4,  4,  4,  4,  4,  4,  5,  5,  5,  5,  5,  5,  5,
        6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  5,  5,  5,  5,  5,  5,
        5,  6,  6,  6,  6,  7,  6,  6,  6,  6,  6,  6,  6,  6,  6,  6,
        6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  5,  6,  5,  5,  5,
        5,  6,  6,  6,  6,  7,  6,  6,  6,  6,  6,  6,  6,  6,  6,  6,
        6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  5,  5,  5,  5,  1
     };

    // map_position_value
    static int positionValue(final int i) {
        if (i < ByteValTable.length) {
            return ByteValTable[i];
        }
        return 4; /* Take it easy. */
    }

}
