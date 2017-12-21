/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.runtime.RegexCompiledRegexExec;
import com.oracle.truffle.regex.runtime.RegexCompiledRegexMessageResolutionForeign;

public class RegexCompiledRegex implements TruffleObject, RegexLanguageObject {

    private final RegexSource source;
    private final CallTarget callTarget;
    private final RegexCompiledRegexExec execMethod;
    private final RegexProfile regexProfile;

    public RegexCompiledRegex(RegexSource source, CallTarget callTarget) {
        this.source = source;
        this.callTarget = callTarget;
        execMethod = new RegexCompiledRegexExec(this);
        regexProfile = new RegexProfile();
    }

    public RegexSource getSource() {
        return source;
    }

    /**
     * A call target to the underlying {@link RegexNode} which will return a {@link RegexResult}.
     * The signature of this operation corresponds to:
     * <code>{@link RegexResult} find(String input, int fromIndex);</code>
     */
    public CallTarget getCallTarget() {
        return callTarget;
    }

    public RegexCompiledRegexExec getExecMethod() {
        return execMethod;
    }

    public RegexProfile getRegexProfile() {
        return regexProfile;
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexCompiledRegex;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexCompiledRegexMessageResolutionForeign.ACCESS;
    }
}
