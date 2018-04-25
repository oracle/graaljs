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
package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.truffleinterop.JSLazyStringForeignAccessFactoryForeign;

public final class JSLazyString implements CharSequence, TruffleObject {
    @TruffleBoundary
    public static CharSequence create(CharSequence left, CharSequence right) {
        assert JSRuntime.isString(left);
        assert JSRuntime.isString(right);
        if (JSTruffleOptions.LazyStrings) {
            if (left.length() == 0) {
                return right;
            } else if (right.length() == 0) {
                return left;
            }
            int resultLength = left.length() + right.length();
            if (resultLength > JSTruffleOptions.StringLengthLimit) {
                throw Errors.createRangeErrorInvalidStringLength();
            }
            if (resultLength < JSTruffleOptions.MinLazyStringLength) {
                return left.toString() + right.toString();
            }
            return new JSLazyString(left, right, resultLength);
        } else {
            return left.toString() + right.toString();
        }
    }

    /**
     * Only use when invariants are checked already, e.g. from specializing nodes.
     */
    @TruffleBoundary
    public static CharSequence createChecked(CharSequence left, CharSequence right, int length) {
        assert assertChecked(left, right, length);
        return new JSLazyString(left, right, length);
    }

    private static boolean assertChecked(CharSequence left, CharSequence right, int length) {
        assert JSTruffleOptions.LazyStrings;
        assert JSRuntime.isString(left) && JSRuntime.isString(right);
        assert length == left.length() + right.length();
        assert left.length() > 0 && right.length() > 0;
        assert JSRuntime.stringLengthValid(left, right);
        assert length >= JSTruffleOptions.MinLazyStringLength;
        return true;
    }

    /**
     * Variant of {@link #createChecked} that tries to concatenate a very short string to an already
     * short root leaf up-front, e.g. when appending single characters.
     */
    @TruffleBoundary
    public static CharSequence createCheckedShort(CharSequence left, CharSequence right, int length) {
        assertChecked(left, right, length);
        final int tinyLimit = 1;
        final int appendToLeafLimit = JSTruffleOptions.MinLazyStringLength / 2;
        if (left instanceof JSLazyString && right instanceof String && right.length() <= tinyLimit) {
            CharSequence ll = ((JSLazyString) left).left;
            CharSequence lr = ((JSLazyString) left).right;
            if (lr != null && lr instanceof String && lr.length() + right.length() <= appendToLeafLimit) {
                return new JSLazyString(ll, lr.toString() + right.toString(), length);
            }
        } else if (left instanceof String && left.length() <= tinyLimit && right instanceof JSLazyString) {
            CharSequence ll = ((JSLazyString) right).left;
            CharSequence lr = ((JSLazyString) right).right;
            if (lr != null && ll instanceof String && left.length() + ll.length() <= appendToLeafLimit) {
                return new JSLazyString(left.toString() + ll.toString(), lr, length);
            }
        }
        return new JSLazyString(left, right, length);
    }

    /**
     * Only use when invariants are checked already, e.g. from specializing nodes. Converts the
     * right int param lazily.
     */
    @TruffleBoundary
    public static CharSequence createLazyInt(CharSequence left, int right) {
        assert JSRuntime.isString(left);
        assert JSTruffleOptions.LazyStrings;
        if (left.length() == 0) {
            return String.valueOf(right); // bailout
        }
        return new JSLazyString(left, new JSLazyIntWrapper(right));
    }

    /**
     * Only use when invariants are checked already, e.g. from specializing nodes. Converts the left
     * int param lazily.
     */
    @TruffleBoundary
    public static CharSequence createLazyInt(int left, CharSequence right) {
        assert JSRuntime.isString(right);
        assert JSTruffleOptions.LazyStrings;
        if (right.length() == 0) {
            return String.valueOf(left); // bailout
        }
        return new JSLazyString(new JSLazyIntWrapper(left), right);
    }

    private CharSequence left;
    private CharSequence right;
    private final int length;

    private JSLazyString(CharSequence left, CharSequence right, int length) {
        assert left.length() > 0 && right.length() > 0 && length == left.length() + right.length();
        this.left = left;
        this.right = right;
        this.length = length;
    }

    private JSLazyString(CharSequence left, CharSequence right) {
        this(left, right, left.length() + right.length());
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public String toString() {
        if (!isFlat()) {
            flatten();
        }
        return (String) left;
    }

    public String toString(ConditionProfile profile) {
        if (profile.profile(!isFlat())) {
            flatten();
        }
        return (String) left;
    }

    private boolean isFlat() {
        return right == null;
    }

    @TruffleBoundary
    private void flatten() {
        char[] dst = new char[length];
        flatten(this, 0, length, dst, 0);
        left = new String(dst);
        right = null;
    }

    private static void flatten(CharSequence src, int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        CompilerAsserts.neverPartOfCompilation();
        CharSequence str = src;
        int from = srcBegin;
        int to = srcEnd;
        int dstFrom = dstBegin;
        for (;;) {
            assert 0 <= from && from <= to && to <= str.length();
            if (str instanceof JSLazyString) {
                JSLazyString lazyString = (JSLazyString) str;
                CharSequence left = lazyString.left;
                CharSequence right = lazyString.right;
                int mid = left.length();

                if (to - mid >= mid - from) {
                    // right is longer, recurse left
                    if (from < mid) {
                        if (left instanceof String) {
                            ((String) left).getChars(from, mid, dst, dstFrom);
                        } else {
                            flatten(left, from, mid, dst, dstFrom);
                        }
                        dstFrom += mid - from;
                        from = 0;
                    } else {
                        from -= mid;
                    }
                    to -= mid;
                    str = right;
                } else {
                    // left is longer, recurse right
                    if (to > mid) {
                        if (right instanceof String) {
                            ((String) right).getChars(0, to - mid, dst, dstFrom + mid - from);
                        } else {
                            flatten(right, 0, to - mid, dst, dstFrom + mid - from);
                        }
                        to = mid;
                    }
                    str = left;
                }
            } else if (str instanceof String) {
                ((String) str).getChars(from, to, dst, dstFrom);
                return;
            } else {
                assert JSRuntime.isString(str) || str instanceof JSLazyIntWrapper;
                str.toString().getChars(from, to, dst, dstFrom);
                return;
            }
        }
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    public boolean isEmpty() {
        return length == 0;
    }

    // accessed via Java Interop, JDK-8062624.js
    @TruffleBoundary
    public boolean startsWith(String prefix) {
        return toString().startsWith(prefix);
    }

    // accessed via Java Interop, JDK-8062624.js
    @TruffleBoundary
    public boolean endsWith(String prefix) {
        return toString().endsWith(prefix);
    }

    // accessed via Java Interop, JDK-8062624.js
    @TruffleBoundary
    public byte[] getBytes() {
        return toString().getBytes();
    }

    private static class JSLazyIntWrapper implements CharSequence {

        private final int value;
        private String str;

        JSLazyIntWrapper(int value) {
            this.value = value;
            this.str = null;
        }

        @Override
        public int length() {
            long absValue = Math.abs((long) value);
            long temp = 10;
            int count = 1;
            while (absValue >= temp) {
                count++;
                temp *= 10;
            }
            return value >= 0 ? count : count + 1;
        }

        @Override
        public char charAt(int index) {
            return toString().charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return toString().subSequence(start, end);
        }

        @Override
        public String toString() {
            if (str == null) {
                str = String.valueOf(value);
            }
            return str;
        }

    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof JSLazyString;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return JSLazyStringForeignAccessFactoryForeign.ACCESS;
    }
}
