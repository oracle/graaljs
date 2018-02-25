/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.runtime.RegexObjectExecMethod;
import com.oracle.truffle.regex.runtime.RegexObjectMessageResolutionForeign;

public class RegexObject implements RegexLanguageObject {

    private final RegexCompiler compiler;
    private final RegexSource source;
    private TruffleObject compiledRegexObject;
    private final RegexObjectExecMethod execMethod;
    private RegexProfile regexProfile;

    public RegexObject(RegexCompiler compiler, RegexSource source) {
        this.compiler = compiler;
        this.source = source;
        execMethod = new RegexObjectExecMethod(this);
    }

    public RegexSource getSource() {
        return source;
    }

    public TruffleObject getCompiledRegexObject() {
        if (compiledRegexObject == null) {
            compiledRegexObject = compileRegex();
        }
        return compiledRegexObject;
    }

    @CompilerDirectives.TruffleBoundary
    private TruffleObject compileRegex() {
        try {
            return compiler.compile(source);
        } catch (RegexSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCompiledRegexObject(TruffleObject compiledRegexObject) {
        this.compiledRegexObject = compiledRegexObject;
    }

    public RegexObjectExecMethod getExecMethod() {
        return execMethod;
    }

    public RegexProfile getRegexProfile() {
        if (regexProfile == null) {
            regexProfile = new RegexProfile();
        }
        return regexProfile;
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexObject;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexObjectMessageResolutionForeign.ACCESS;
    }
}
