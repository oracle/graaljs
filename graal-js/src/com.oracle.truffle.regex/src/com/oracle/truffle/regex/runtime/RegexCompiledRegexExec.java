/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.runtime;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.RegexCompiledRegex;
import com.oracle.truffle.regex.RegexLanguageObject;

public final class RegexCompiledRegexExec implements TruffleObject, RegexLanguageObject {

    private final RegexCompiledRegex regex;

    public RegexCompiledRegexExec(RegexCompiledRegex regex) {
        this.regex = regex;
    }

    public RegexCompiledRegex getRegexCompiledRegex() {
        return regex;
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexCompiledRegexExec;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexCompiledRegexExecMessageResolutionForeign.ACCESS;
    }
}
