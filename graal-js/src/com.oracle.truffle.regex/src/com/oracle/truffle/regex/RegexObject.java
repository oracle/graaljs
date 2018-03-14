/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.runtime.RegexObjectExecMethod;
import com.oracle.truffle.regex.runtime.RegexObjectMessageResolutionForeign;

/**
 * {@link RegexObject} represents a compiled regular expression that can be used to match against
 * input strings. It is the result of executing a {@link RegexEngine}. It exposes the following
 * three properties:
 * <ol>
 * <li>{@link String} {@code pattern}: the source of the compiled regular expression</li>
 * <li>{@link RegexFlags} {@code flags}: the set of flags passed to the regular expression compiler
 * </li>
 * <li>{@link RegexObjectExecMethod} {@code exec}: an executable method that matches the compiled
 * regular expression against a string. The method accepts two parameters:
 * <ol>
 * <li>{@link Object} {@code input}: the character sequence to search in. This may either be a
 * {@link String} or a {@link TruffleObject} that responds to {@link Message#GET_SIZE} and returns
 * {@link Character}s on indexed {@link Message#READ} requests.</li>
 * <li>{@link Number} {@code fromIndex}: the position to start searching from. This argument will be
 * cast to {@code int}, since a {@link String} can not be longer than {@link Integer#MAX_VALUE}. If
 * {@code fromIndex} is greater than {@link Integer#MAX_VALUE}, this method will immediately return
 * NO_MATCH.</li>
 * </ol>
 * The return value is a {@link RegexResult}. The contents of the {@code exec} can be compiled
 * lazily and so its first invocation might involve a longer delay as the regular expression is
 * compiled on the fly.
 * </ol>
 * <p>
 */
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
