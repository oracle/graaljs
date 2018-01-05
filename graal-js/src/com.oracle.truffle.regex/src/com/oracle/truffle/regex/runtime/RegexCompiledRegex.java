/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.runtime;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.CompiledRegex;
import com.oracle.truffle.regex.RegexLanguageObject;

public final class RegexCompiledRegex implements TruffleObject, RegexLanguageObject {

    private final CompiledRegex regex;
    private final RegexFlagsObject flagsObject;
    private final RegexCompiledRegexExec execMethod;

    public RegexCompiledRegex(CompiledRegex regex) {
        this.regex = regex;
        flagsObject = new RegexFlagsObject(regex.getSource().getFlags());
        execMethod = new RegexCompiledRegexExec(this);
    }

    public CompiledRegex getRegex() {
        return regex;
    }

    public RegexFlagsObject getFlagsObject() {
        return flagsObject;
    }

    public RegexCompiledRegexExec getExecMethod() {
        return execMethod;
    }

    public static boolean isInstance(Object object) {
        return object instanceof RegexCompiledRegex;
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexCompiledRegex;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexCompiledRegexMessageResolutionForeign.ACCESS;
    }
}
