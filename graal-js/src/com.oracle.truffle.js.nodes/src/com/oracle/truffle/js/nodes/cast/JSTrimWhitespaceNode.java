/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSRuntime;

public abstract class JSTrimWhitespaceNode extends JavaScriptBaseNode {

    private final ConditionProfile isFastNonWhitespace = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isFastWhitespace = ConditionProfile.createBinaryProfile();

    public static JSTrimWhitespaceNode create() {
        return JSTrimWhitespaceNodeGen.create();
    }

    public abstract String executeString(String operand);

    protected boolean startsOrEndsWithWhitespace(String string) {
        assert string.length() > 0;
        return isWhiteSpace(string, 0) || isWhiteSpace(string, string.length() - 1);
    }

    @Specialization(guards = "string.length() == 0")
    protected String doStringZero(String string) {
        return string;
    }

    @Specialization(guards = {"string.length() > 0", "!startsOrEndsWithWhitespace(string)"})
    protected String doStringNoWhitespace(String string) {
        return string;
    }

    @Specialization(guards = {"string.length() > 0", "startsOrEndsWithWhitespace(string)"})
    protected String doString(String string,
                    @Cached("create()") BranchProfile needFirstBranch,
                    @Cached("create()") BranchProfile needLastBranch,
                    @Cached("createBinaryProfile()") ConditionProfile needSubstring) {
        int len = string.length();
        int firstIdx = 0;
        if (isWhiteSpace(string, 0)) {
            needFirstBranch.enter();
            firstIdx = JSRuntime.firstNonWhitespaceIndex(string, false);
        }
        int lastIdx = len - 1;
        if (isWhiteSpace(string, len - 1)) {
            needLastBranch.enter();
            lastIdx = JSRuntime.lastNonWhitespaceIndex(string, false);
        }
        if (needSubstring.profile(firstIdx > lastIdx)) {
            return "";
        } else {
            return Boundaries.substring(string, firstIdx, lastIdx + 1);
        }
    }

    private boolean isWhiteSpace(String str, int index) {
        char c = str.charAt(index);
        if (isFastNonWhitespace.profile(0x0020 < c && c < 0x00A0)) {
            return false;
        } else if (isFastWhitespace.profile(c == ' ' || c == '\n' || c == '\r' || c == '\t')) {
            return true;
        } else {
            return JSRuntime.isWhiteSpace(str.charAt(index));
        }
    }
}
