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
package com.oracle.truffle.js.builtins.helper;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * String escape/unescape utility class. Used by B.2.1 escape() and B.2.2 unescape() methods as
 * defined in ECMAScript 5.1.
 *
 */
public class StringEscape {

    private static final BitSet dontEscapeSet;

    static {
        BitSet unescaped = new BitSet(128);
        unescaped.set('a', 'z' + 1);
        unescaped.set('A', 'Z' + 1);
        unescaped.set('0', '9' + 1);
        unescaped.set('@');
        unescaped.set('*');
        unescaped.set('_');
        unescaped.set('+');
        unescaped.set('-');
        unescaped.set('.');
        unescaped.set('/');
        dontEscapeSet = unescaped;
    }

    @TruffleBoundary
    public static String escape(String s) {
        boolean didEscape = false;
        StringBuffer out = new StringBuffer(s.length());

        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            if (dontEscapeSet.get(c)) {
                out.append((char) c);
            } else {
                didEscape = true;
                out.append('%');
                if (c < 256) {
                    char ch = hexChar((c >> 4) & 0xF);
                    out.append(ch);
                    ch = hexChar(c & 0xF);
                    out.append(ch);
                } else {
                    out.append('u');
                    char ch = hexChar((c >> 12) & 0xF);
                    out.append(ch);
                    ch = hexChar((c >> 8) & 0xF);
                    out.append(ch);
                    ch = hexChar((c >> 4) & 0xF);
                    out.append(ch);
                    ch = hexChar(c & 0xF);
                    out.append(ch);
                }
            }
        }
        return didEscape ? out.toString() : s;
    }

    @TruffleBoundary
    public static String unescape(String string) {
        int len = string.length();
        StringBuilder builder = new StringBuilder();
        int k = 0;
        while (k < len) {
            char c = string.charAt(k);
            if (c == '%') {
                if (k <= (len - 6)) {
                    if (unescapeU0000(string, builder, k)) {
                        k += 6;
                        continue;
                    }
                }
                if (k <= (len - 3)) {
                    if (unescape00(string, builder, k)) {
                        k += 3;
                        continue;
                    }
                }
            }
            builder.append(c);
            k++;
        }
        return builder.toString();
    }

    private static boolean unescapeU0000(String string, StringBuilder builder, int k) {
        char c1 = string.charAt(k + 1);
        if (c1 == 'u') {
            char c2 = string.charAt(k + 2);
            char c3 = string.charAt(k + 3);
            char c4 = string.charAt(k + 4);
            char c5 = string.charAt(k + 5);
            if (JSRuntime.isHex(c2) && JSRuntime.isHex(c3) && JSRuntime.isHex(c4) && JSRuntime.isHex(c5)) {
                char newC = (char) (hexVal(c2) * 16 * 16 * 16 + hexVal(c3) * 16 * 16 + hexVal(c4) * 16 + hexVal(c5));
                builder.append(newC);
                return true;
            }
        }
        return false;
    }

    private static boolean unescape00(String string, StringBuilder builder, int k) {
        char c1 = string.charAt(k + 1);
        char c2 = string.charAt(k + 2);
        if (JSRuntime.isHex(c1) && JSRuntime.isHex(c2)) {
            char newC = (char) (hexVal(c1) * 16 + hexVal(c2));
            builder.append(newC);
            return true;
        }
        return false;
    }

    private static char hexChar(int value) {
        if (value < 10) {
            return (char) ('0' + value);
        } else {
            return (char) ('A' + value - 10);
        }
    }

    private static int hexVal(char c) {
        int value = JSRuntime.valueInHex(c);
        if (value < 0) {
            assert false : "not a hex character";
            return 0;
        }
        return value;
    }

}
