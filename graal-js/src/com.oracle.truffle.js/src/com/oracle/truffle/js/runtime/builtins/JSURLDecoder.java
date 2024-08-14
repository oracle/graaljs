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
 * Copyright (c) 1998, 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;

public class JSURLDecoder {

    private final boolean isSpecial;

    public JSURLDecoder(boolean isSpecial) {
        this.isSpecial = isSpecial;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public TruffleString decode(TruffleString string) {
        int strLen = Strings.length(string);
        TruffleStringBuilderUTF16 sb = null;
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        int k = 0;

        while (k < strLen) {
            char c = Strings.charAt(string, k);
            if (c != '%') {
                if (sb != null) {
                    Strings.builderAppend(sb, c);
                }
            } else {
                if (sb == null) {
                    sb = JSURLEncoder.allocStringBuilder(string, k, strLen);
                }
                k = decodeConvert(string, strLen, k, sb, decoder);
            }
            k++;
        }
        return sb != null ? Strings.builderToString(sb) : string;
    }

    private int decodeConvert(TruffleString string, int strLen, int start, TruffleStringBuilderUTF16 buffer, CharsetDecoder decoder) {
        int k = start;
        if (k + 2 >= strLen) {
            throw Errors.createURIError("illegal escape sequence");
        }
        int hex1 = getHexValue(Strings.charAt(string, k + 1));
        int hex2 = getHexValue(Strings.charAt(string, k + 2));
        byte b = (byte) ((hex1 << 4) + hex2);
        k += 2;
        if ((b & 0x80) == 0) { // vi. most significant bit is 0
            char c = (char) b;
            if (!isReserved(c)) {
                Strings.builderAppend(buffer, c);
            } else {
                Strings.builderAppend(buffer, string, start, k + 1);
            }
        } else { // vii. most significant bit is 1
            k = decodeConvertIntl(string, strLen, k, b, buffer, decoder);
        }
        return k;
    }

    private int decodeConvertIntl(TruffleString string, int strLen, int kParam, byte b, TruffleStringBuilderUTF16 buffer, CharsetDecoder decoder) {
        int k = kParam;
        int n = findN(b);
        if (n == 1 || n > 4) {
            throw invalidEncodingError();
        }
        byte[] octetsB = new byte[n];
        octetsB[0] = b;
        if ((k + (3 * (n - 1))) >= strLen) {
            throw invalidEncodingError();
        }
        int j = 1;
        while (j < n) {
            k++;
            if (Strings.charAt(string, k) != '%') {
                throw invalidEncodingError();
            }
            int hex3 = getHexValue(Strings.charAt(string, k + 1));
            int hex4 = getHexValue(Strings.charAt(string, k + 2));
            byte b2 = (byte) ((hex3 << 4) + hex4);
            if ((b2 & 0xC0) != 0x80) {
                throw invalidEncodingError();
            }
            k += 2;
            octetsB[j] = b2;
            j++;
        }
        ByteBuffer bb = ByteBuffer.wrap(octetsB);
        CharBuffer cb = CharBuffer.wrap(new char[2]);
        decoder.reset();
        cb.rewind();
        CoderResult coderResult = decoder.decode(bb, cb, true);
        if (coderResult.isError()) {
            throw invalidEncodingError();
        }
        if (cb.position() == 1) {
            assert !isReserved(cb.get(0));
            Strings.builderAppend(buffer, cb.get(0));
        } else {
            Strings.builderAppend(buffer, cb.get(0));
            Strings.builderAppend(buffer, cb.get(1));
        }
        return k;
    }

    private static JSException invalidEncodingError() {
        throw Errors.createURIError("invalid encoding");
    }

    private static int getHexValue(char digit) {
        int value = JSRuntime.valueInHex(digit);
        if (value < 0) {
            throw Errors.createURIError("decode: Illegal hex characters in escape (%) pattern");
        }
        return value;
    }

    private boolean isReserved(char c) {
        if (isSpecial) {
            return JSURLEncoder.reservedURISet.get(c);
        }
        return false;
    }

    private static int findN(byte b) {
        if ((b & 0x40) == 0) {
            return 1;
        }
        if ((b & 0x20) == 0) {
            return 2;
        }
        if ((b & 0x10) == 0) {
            return 3;
        }
        if ((b & 0x08) == 0) {
            return 4;
        }
        if ((b & 0x04) == 0) {
            return 5;
        }
        if ((b & 0x02) == 0) {
            return 6;
        }
        if ((b & 0x01) == 0) {
            return 7;
        }
        return 8;
    }

    @TruffleBoundary
    public static String decodePercentEncoding(String input, Charset charset) {
        int n = input.length();
        if (n == 0) {
            return input;
        }
        if (input.indexOf('%') < 0) {
            return input;
        }

        StringBuilder sb = new StringBuilder(n);
        ByteBuffer bb = ByteBuffer.allocate(n);
        CharBuffer cb = CharBuffer.allocate(n);
        CharsetDecoder dec = charset.newDecoder();
        dec.onMalformedInput(CodingErrorAction.REPLACE);
        dec.onUnmappableCharacter(CodingErrorAction.REPLACE);

        char c = input.charAt(0);
        for (int i = 0; i < n;) {
            assert c == input.charAt(i);
            if (c != '%') {
                sb.append(c);
                if (++i >= n) {
                    break;
                }
                c = input.charAt(i);
                continue;
            }
            // decode percent-encoded sequence
            bb.clear();
            for (;;) {
                assert (n - i >= 2);
                char c1 = input.charAt(++i);
                char c2 = input.charAt(++i);
                bb.put((byte) ((getHexValue(c1) << 4) |
                                getHexValue(c2)));
                if (++i >= n) {
                    break;
                }
                c = input.charAt(i);
                if (c != '%') {
                    break;
                }
            }
            bb.flip();
            cb.clear();
            dec.reset();
            CoderResult cr = dec.decode(bb, cb, true);
            assert cr.isUnderflow();
            cr = dec.flush(cb);
            assert cr.isUnderflow();
            sb.append(cb.flip().toString());
        }

        return sb.toString();
    }
}
