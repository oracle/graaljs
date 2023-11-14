/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 1995, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.truffle.js.runtime.builtins;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.Strings;

/**
 * Utility class for {@code encodeURI} and {@code encodeURIComponent}.
 *
 * <p>
 * When encoding a String, the following rules apply:
 * <ul>
 * <li>The alphanumeric characters &quot;<code>a</code>&quot; through &quot;<code>z</code>&quot;,
 * &quot;<code>A</code>&quot; through &quot;<code>Z</code>&quot; and &quot;<code>0</code>&quot;
 * through &quot;<code>9</code>&quot; remain the same.
 * <li>The special characters in the {@code mark} set in the case of {@code encodeURIComponent}, and
 * additionally those in the {@code reserved} set plus &quot;{@code #}&quot in the case of
 * {@code encodeURI}; remain the same.
 * <li>The space character &quot;<code>&nbsp;</code>&quot; is converted into a plus sign &quot;
 * <code>+</code>&quot;.
 * <li>All other characters are unsafe and are first converted into one or more bytes using some
 * encoding scheme. Then each byte is represented by the 3-character string &quot;
 * <code>%<i>xy</i></code>&quot;, where <i>xy</i> is the two-digit hexadecimal representation of the
 * byte. The default encoding scheme is UTF-8.
 * </ul>
 */
public final class JSURLEncoder {

    static final BitSet unreservedURISet;
    static final BitSet reservedURISet;

    private final boolean isSpecial;
    private final Charset charset;

    static {
        /*
         * RFC 2396 states:
         *
         * Data characters that are allowed in a URI but do not have a reserved purpose are called
         * unreserved. These include upper and lower case letters, decimal digits, and a limited set
         * of punctuation marks and symbols.
         *
         * unreserved = alphanum | mark
         *
         * mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
         *
         * Unreserved characters can be escaped without changing the semantics of the URI, but this
         * should not be done unless the URI is being used in a context that does not allow the
         * unescaped character to appear.
         *
         * -----
         *
         * Many URI include components consisting of or delimited by, certain special characters.
         * These characters are called "reserved", since their usage within the URI component is
         * limited to their reserved purpose. If the data for a URI component would conflict with
         * the reserved purpose, then the conflicting data must be escaped before forming the URI.
         *
         * reserved = ";" | "/" | "?" | ":" | "@" | "&" | "=" | "+" | "$" | ","
         */

        BitSet unreserved = new BitSet(128);
        unreserved.set('a', 'z' + 1);
        unreserved.set('A', 'Z' + 1);
        unreserved.set('0', '9' + 1);

        unreserved.set('-');
        unreserved.set('_');
        unreserved.set('.');
        unreserved.set('*');

        unreserved.set('!');
        unreserved.set('~');
        unreserved.set('\'');
        unreserved.set('(');
        unreserved.set(')');

        // reserved plus "#"
        BitSet reserved = new BitSet(128);
        reserved.set(';');
        reserved.set('/');
        reserved.set('?');
        reserved.set(':');
        reserved.set('@');
        reserved.set('&');
        reserved.set('=');
        reserved.set('+');
        reserved.set('$');
        reserved.set(',');

        reserved.set('#');

        unreservedURISet = unreserved;
        reservedURISet = reserved;
    }

    public JSURLEncoder(boolean isSpecial) {
        this(isSpecial, StandardCharsets.UTF_8);
    }

    public JSURLEncoder(boolean isSpecial, Charset charset) {
        this.charset = charset;
        this.isSpecial = isSpecial;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public TruffleString encode(TruffleString s) {
        int length = Strings.length(s);
        TruffleStringBuilderUTF16 sb = null;
        CharsetEncoder encoder = null;

        int i = 0;
        String javaStr = Strings.toJavaString(s);
        while (i < length) {
            int c = Strings.charAt(s, i);
            if (needsNoEncoding(c)) {
                if (sb != null) {
                    Strings.builderAppend(sb, (char) c);
                }
                i++;
            } else {
                if (sb == null) {
                    sb = allocStringBuilder(s, i, length + 16);
                }
                if (encoder == null) {
                    encoder = charset.newEncoder();
                }
                i = encodeConvert(javaStr, i, c, sb, encoder);
            }
        }
        return sb != null ? Strings.builderToString(sb) : s;
    }

    static TruffleStringBuilderUTF16 allocStringBuilder(TruffleString s, int i, int estimatedLength) {
        var sb = Strings.builderCreate(estimatedLength);
        if (i > 0) {
            Strings.builderAppend(sb, s, 0, i);
        }
        return sb;
    }

    private int encodeConvert(String s, int iParam, int cParam, TruffleStringBuilderUTF16 buffer, CharsetEncoder encoder) {
        int i = iParam;
        int c = cParam;
        int startPos = i;
        do {
            if (0xDC00 <= c && c <= 0xDFFF) {
                throw cannotEscapeError();
            }
            if (c >= 0xD800 && c <= 0xDBFF) {
                if ((i + 1) < s.length()) {
                    int d = s.charAt(i + 1);
                    if (d >= 0xDC00 && d <= 0xDFFF) {
                        i++;
                    } else {
                        throw cannotEscapeError();
                    }
                } else {
                    throw cannotEscapeError();
                }
            }
            i++;
        } while (i < s.length() && !needsNoEncoding(c = s.charAt(i)));

        ByteBuffer bb = encodeSubstring(s, startPos, i, encoder);
        byte[] ba = bb.array();
        assert bb.arrayOffset() + bb.position() == 0;
        int length = bb.limit();
        for (int j = 0; j < length; j++) {
            Strings.builderAppend(buffer, '%');
            char ch = charForDigit((ba[j] >> 4) & 0xF, 16);
            Strings.builderAppend(buffer, ch);
            ch = charForDigit(ba[j] & 0xF, 16);
            Strings.builderAppend(buffer, ch);
        }
        return i;
    }

    /**
     * Like {@link Character#forDigit}, but returns uppercase letters.
     */
    public static char charForDigit(int digit, int radix) {
        assert digit >= 0 && digit < radix && radix >= Character.MIN_RADIX && radix <= Character.MAX_RADIX;
        if (digit < 10) {
            return (char) ('0' + digit);
        }
        return (char) ('A' - 10 + digit);
    }

    private static JSException cannotEscapeError() {
        throw Errors.createURIError("cannot escape");
    }

    private static ByteBuffer encodeSubstring(String s, int off, int len, CharsetEncoder encoder) {
        CharBuffer cb = CharBuffer.wrap(s, off, len);
        try {
            return encoder.encode(cb);
        } catch (CharacterCodingException ex) {
            throw cannotEscapeError();
        }
    }

    private boolean needsNoEncoding(int c) {
        if (isSpecial) {
            return unreservedURISet.get(c) || reservedURISet.get(c);
        } else {
            return unreservedURISet.get(c);
        }
    }
}
