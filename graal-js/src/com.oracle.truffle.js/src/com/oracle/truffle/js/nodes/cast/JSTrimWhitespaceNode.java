/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;

public abstract class JSTrimWhitespaceNode extends JavaScriptBaseNode {

    private final ConditionProfile isFastNonWhitespace = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isFastWhitespace = ConditionProfile.createBinaryProfile();

    public static JSTrimWhitespaceNode create() {
        return JSTrimWhitespaceNodeGen.create();
    }

    public abstract TruffleString executeString(TruffleString operand);

    protected boolean startsOrEndsWithWhitespace(TruffleString.ReadCharUTF16Node readRawNode, TruffleString string) {
        assert Strings.length(string) > 0;
        return isWhiteSpace(readRawNode, string, 0) || isWhiteSpace(readRawNode, string, Strings.length(string) - 1);
    }

    @Specialization(guards = "stringLength(string) == 0")
    protected TruffleString doStringZero(TruffleString string) {
        return string;
    }

    @Specialization(guards = {"stringLength(string) > 0", "!startsOrEndsWithWhitespace(readRawNode, string)"})
    protected TruffleString doStringNoWhitespace(TruffleString string,
                    @Cached @SuppressWarnings("unused") TruffleString.ReadCharUTF16Node readRawNode) {
        return string;
    }

    @Specialization(guards = {"stringLength(string) > 0", "startsOrEndsWithWhitespace(readRawNode, string)"})
    protected TruffleString doString(TruffleString string,
                    @Cached BranchProfile needFirstBranch,
                    @Cached BranchProfile needLastBranch,
                    @Cached ConditionProfile needSubstring,
                    @Cached TruffleString.ReadCharUTF16Node readRawNode,
                    @Cached TruffleString.SubstringByteIndexNode substringNode) {
        int len = Strings.length(string);
        int firstIdx = 0;
        if (isWhiteSpace(readRawNode, string, 0)) {
            needFirstBranch.enter();
            firstIdx = JSRuntime.firstNonWhitespaceIndex(string, false, readRawNode);
        }
        int lastIdx = len - 1;
        if (isWhiteSpace(readRawNode, string, len - 1)) {
            needLastBranch.enter();
            lastIdx = JSRuntime.lastNonWhitespaceIndex(string, false, readRawNode);
        }
        if (needSubstring.profile(firstIdx > lastIdx)) {
            return Strings.EMPTY_STRING;
        } else {
            return Strings.substring(substringNode, string, firstIdx, lastIdx + 1 - firstIdx);
        }
    }

    private boolean isWhiteSpace(TruffleString.ReadCharUTF16Node readRawNode, TruffleString str, int index) {
        char c = Strings.charAt(readRawNode, str, index);
        if (isFastNonWhitespace.profile(0x0020 < c && c < 0x00A0)) {
            return false;
        } else if (isFastWhitespace.profile(c == ' ' || c == '\n' || c == '\r' || c == '\t')) {
            return true;
        } else {
            return JSRuntime.isWhiteSpace(c);
        }
    }
}
