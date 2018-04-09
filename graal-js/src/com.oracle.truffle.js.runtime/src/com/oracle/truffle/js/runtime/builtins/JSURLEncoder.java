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

import java.net.*;
import java.nio.charset.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.runtime.*;

/**
 * Utility class for HTML form encoding. This class contains static methods for converting a String
 * to the <CODE>application/x-www-form-urlencoded</CODE> MIME format. For more information about
 * HTML form encoding, consult the HTML <A HREF="http://www.w3.org/TR/html4/">specification</A>.
 *
 * <p>
 * When encoding a String, the following rules apply:
 *
 * <p>
 * <ul>
 * <li>The alphanumeric characters &quot;<code>a</code>&quot; through &quot;<code>z</code>&quot;,
 * &quot;<code>A</code>&quot; through &quot;<code>Z</code>&quot; and &quot;<code>0</code>&quot;
 * through &quot;<code>9</code>&quot; remain the same.
 * <li>The special characters &quot;<code>.</code>&quot;, &quot;<code>-</code>&quot;, &quot;
 * <code>*</code>&quot;, and &quot;<code>_</code>&quot; remain the same.
 * <li>The space character &quot;<code>&nbsp;</code>&quot; is converted into a plus sign &quot;
 * <code>+</code>&quot;.
 * <li>All other characters are unsafe and are first converted into one or more bytes using some
 * encoding scheme. Then each byte is represented by the 3-character string &quot;
 * <code>%<i>xy</i></code>&quot;, where <i>xy</i> is the two-digit hexadecimal representation of the
 * byte. The recommended encoding scheme to use is UTF-8. However, for compatibility reasons, if an
 * encoding is not specified, then the default encoding of the platform is used.
 * </ul>
 *
 * <p>
 * For example using UTF-8 as the encoding scheme the string &quot;The string &#252;@foo-bar&quot;
 * would get converted to &quot;The+string+%C3%BC%40foo-bar&quot; because in UTF-8 the character
 * &#252; is encoded as two bytes C3 (hex) and BC (hex), and the character @ is encoded as one byte
 * 40 (hex).
 *
 * @author Herb Jellinek
 * @since JDK1.0
 */
public final class JSURLEncoder {

    private static BitSet dontNeedEncoding;
    private static BitSet dontNeedEncodingSpecial;
    private static final int caseDiff = ('a' - 'A');

    private final boolean isSpecial;
    private final Charset charset;

    private static synchronized void init() {
        if (dontNeedEncoding == null) {
            /*
             * The list of characters that are not encoded has been determined as follows:
             *
             * RFC 2396 states: ----- Data characters that are allowed in a URI but do not have a
             * reserved purpose are called unreserved. These include upper and lower case letters,
             * decimal digits, and a limited set of punctuation marks and symbols.
             *
             * unreserved = alphanum | mark
             *
             * mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
             *
             * Unreserved characters can be escaped without changing the semantics of the URI, but
             * this should not be done unless the URI is being used in a context that does not allow
             * the unescaped character to appear. -----
             *
             * It appears that both Netscape and Internet Explorer escape all special characters
             * from this list with the exception of "-", "_", ".", "*". While it is not clear why
             * they are escaping the other characters, perhaps it is safest to assume that there
             * might be contexts in which the others are unsafe if not escaped. Therefore, we will
             * use the same list. It is also noteworthy that this is consistent with O'Reilly's
             * "HTML: The Definitive Guide" (page 164).
             *
             * As a last note, Intenet Explorer does not encode the "@" character which is clearly
             * not unreserved according to the RFC. We are being consistent with the RFC in this
             * matter, as is Netscape.
             */

            dontNeedEncoding = new BitSet(256);
            int i;
            for (i = 'a'; i <= 'z'; i++) {
                dontNeedEncoding.set(i);
            }
            for (i = 'A'; i <= 'Z'; i++) {
                dontNeedEncoding.set(i);
            }
            for (i = '0'; i <= '9'; i++) {
                dontNeedEncoding.set(i);
            }

            dontNeedEncoding.set('-');
            dontNeedEncoding.set('_');
            dontNeedEncoding.set('.');
            dontNeedEncoding.set('*');

            // added for Javascript:
            dontNeedEncoding.set('!');
            dontNeedEncoding.set('~');
            dontNeedEncoding.set('\'');
            dontNeedEncoding.set('(');
            dontNeedEncoding.set(')');

            dontNeedEncodingSpecial = new BitSet(256);
            // ; / ? : @ & = + $ ,
            dontNeedEncodingSpecial.set(';');
            dontNeedEncodingSpecial.set('/');
            dontNeedEncodingSpecial.set('?');
            dontNeedEncodingSpecial.set(':');
            dontNeedEncodingSpecial.set('@');
            dontNeedEncodingSpecial.set('&');
            dontNeedEncodingSpecial.set('=');
            dontNeedEncodingSpecial.set('+');
            dontNeedEncodingSpecial.set('$');
            dontNeedEncodingSpecial.set(',');
            dontNeedEncodingSpecial.set('#');
        }
    }

    public JSURLEncoder(boolean isSpecial) {
        this(isSpecial, StandardCharsets.UTF_8);
    }

    public JSURLEncoder(boolean isSpecial, Charset charset) {
        this.charset = charset;
        this.isSpecial = isSpecial;
    }

    /**
     * Translates a string into <code>application/x-www-form-urlencoded</code> format using a
     * specific encoding scheme. This method uses the supplied encoding scheme to obtain the bytes
     * for unsafe characters.
     * <p>
     * <em><strong>Note:</strong> The
     * <a href= "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars"> World Wide Web
     * Consortium Recommendation</a> states that UTF-8 should be used. Not doing so may introduce
     * incompatibilites.</em>
     *
     * @param s <code>String</code> to be translated.
     * @return the translated <code>String</code>.
     * @see URLDecoder#decode(java.lang.String, java.lang.String)
     * @since 1.4
     */
    @TruffleBoundary(transferToInterpreterOnException = false)
    public String encode(String s) {
        StringBuilder buffer = null;
        init();

        int i = 0;
        while (i < s.length()) {
            int c = s.charAt(i);
            if (needsNoEncoding(c)) {
                if (c == ' ') {
                    c = '+';
                    buffer = initBuffer(buffer, s, i);
                }
                if (buffer != null) {
                    buffer.append((char) c);
                }
                i++;
            } else {
                buffer = initBuffer(buffer, s, i);
                i = encodeConvert(s, i, c, buffer);
            }
        }
        String returnString = (buffer != null ? buffer.toString() : s);
        return returnString;
    }

    /**
     * Initialize the buffer lazily, only when needed.
     */
    private static StringBuilder initBuffer(StringBuilder buffer, String s, int i) {
        if (buffer != null) {
            return buffer;
        } else {
            StringBuilder newBuffer = new StringBuilder(s.length() + 20);
            if (i >= 1) {
                newBuffer.append(s, 0, i);
            }
            return newBuffer;
        }
    }

    private int encodeConvert(String s, int iParam, int cParam, StringBuilder buffer) {
        int i = iParam;
        int c = cParam;
        // convert to external encoding before hex conversion
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

        byte[] ba = s.substring(startPos, i).getBytes(charset);
        for (int j = 0; j < ba.length; j++) {
            buffer.append('%');
            char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
            // converting to use uppercase letter as part of
            // the hex value if ch is a letter.
            if (Character.isLetter(ch)) {
                ch -= caseDiff;
            }
            buffer.append(ch);
            ch = Character.forDigit(ba[j] & 0xF, 16);
            if (Character.isLetter(ch)) {
                ch -= caseDiff;
            }
            buffer.append(ch);
        }
        return i;
    }

    private static JSException cannotEscapeError() {
        throw Errors.createURIError("cannot escape");
    }

    private boolean needsNoEncoding(int c) {
        if (isSpecial) {
            return dontNeedEncoding.get(c) || dontNeedEncodingSpecial.get(c);
        } else {
            return dontNeedEncoding.get(c);
        }
    }
}
