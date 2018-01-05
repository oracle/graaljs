/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;

public class JSURLDecoder {

    private final boolean isSpecial;
    private final Charset charset;

    public JSURLDecoder(boolean isSpecial) {
        this(isSpecial, StandardCharsets.UTF_8);
    }

    public JSURLDecoder(boolean isSpecial, Charset charset) {
        this.charset = charset;
        this.isSpecial = isSpecial;
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    public String decode(String string) {
        int strLen = string.length();
        StringBuilder buffer = null;
        CharsetDecoder decoder = charset.newDecoder();
        int k = 0;

        while (k < strLen) {
            char c = string.charAt(k);
            if (c != '%') {
                if (buffer != null) {
                    buffer.append(c);
                }
            } else {
                buffer = initBuffer(buffer, string, k);
                k = decodeConvert(string, strLen, k, buffer, decoder);
            }
            k++;
        }
        String returnStr = (buffer != null ? buffer.toString() : string);
        return returnStr;
    }

    /**
     * Initialize the buffer lazily, only when needed.
     */
    private static StringBuilder initBuffer(StringBuilder buffer, String s, int i) {
        if (buffer != null) {
            return buffer;
        } else {
            StringBuilder newBuffer = new StringBuilder(s.length());
            if (i >= 1) {
                newBuffer.append(s, 0, i);
            }
            return newBuffer;
        }
    }

    private int decodeConvert(String string, int strLen, int kParam, StringBuilder buffer, CharsetDecoder decoder) {
        int k = kParam;
        if (k + 2 >= strLen) {
            throw Errors.createURIError("illegal escape sequence");
        }
        int hex1 = getHexValue(string.charAt(k + 1));
        int hex2 = getHexValue(string.charAt(k + 2));
        byte b = (byte) ((hex1 << 4) + hex2);
        k += 2;
        if ((b & 0x80) == 0) { // vi.
            appendChar(string, kParam, k, (char) b, buffer);
        } else { // vii.
            k = decodeConvertIntl(string, strLen, k, kParam, b, buffer, decoder);
        }
        return k;
    }

    private int decodeConvertIntl(String string, int strLen, int kParam, int start, byte b, StringBuilder buffer, CharsetDecoder decoder) {
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
            if (string.charAt(k) != '%') {
                throw invalidEncodingError();
            }
            int hex3 = getHexValue(string.charAt(k + 1));
            int hex4 = getHexValue(string.charAt(k + 2));
            byte b2 = (byte) ((hex3 << 4) + hex4);
            if ((b2 & 0xC0) != 0x80) {
                throw invalidEncodingError();
            }
            k += 2;
            octetsB[j] = b2;
            j++;
        }
        ByteBuffer bb = ByteBuffer.wrap(octetsB);
        CharBuffer cb = CharBuffer.wrap(new char[10]);
        decoder.reset();
        cb.rewind();
        CoderResult coderResult = decoder.decode(bb, cb, true);
        if (coderResult.isError()) {
            throw invalidEncodingError();
        }
        if (cb.position() == 1) {
            appendChar(string, start, k, cb.get(0), buffer);
        } else {
            buffer.append(cb.get(0));
            buffer.append(cb.get(1));
        }
        return k;
    }

    private static JSException invalidEncodingError() {
        throw Errors.createURIError("invalid encoding");
    }

    private void appendChar(String string, int start, int k, char c, StringBuilder buffer) {
        if (needsDecoding(c)) {
            buffer.append(c);
        } else {
            buffer.append(string.substring(start, k + 1));
        }
    }

    private static int getHexValue(char digit) {
        int value = JSRuntime.valueInHex(digit);
        if (value < 0) {
            throw Errors.createURIError("decode: Illegal hex characters in escape (%) pattern");
        }
        return value;
    }

    private boolean needsDecoding(char c) {
        if (isSpecial) {
            switch (c) {
                case ';':
                case '/':
                case '?':
                case ':':
                case '@':
                case '&':
                case '=':
                case '+':
                case '$':
                case ',':
                case '#':
                    return false;
            }
        }
        return true;
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
}
