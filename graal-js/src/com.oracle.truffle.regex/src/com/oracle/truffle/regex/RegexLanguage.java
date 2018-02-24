/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.joni.JoniRegexCompiler;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.tregex.TRegexCompiler;
import com.oracle.truffle.regex.tregex.parser.RegexParser;

import java.util.Collections;

/**
 * Truffle Regular Expression Language
 * <p>
 * This language represents classic regular expressions, currently in JavaScript flavor only.
 * Accepted sources are single-line regular expressions in the form "/regex/flags". If the provided
 * source is a valid expression, it will return a {@link RegexObject}, which is executable.
 * <p>
 * {@link RegexObject} accepts two parameters:
 * <ol>
 * <li>{@link Object} {@code input}: the character sequence to search in. This may either be a
 * {@link String} or a {@link TruffleObject} that responds to {@link Message#GET_SIZE} and returns
 * {@link Character}s on indexed {@link Message#READ} requests.</li>
 * <li>{@link Number} {@code fromIndex}: the position to start searching from. This argument will be
 * cast to {@code int}, since a {@link String} can not be longer than {@link Integer#MAX_VALUE}. If
 * {@code fromIndex} is greater than {@link Integer#MAX_VALUE}, this method will immediately return
 * NO_MATCH.</li>
 * <li>The return value is a {@link RegexResult}, which has the following properties:
 * <ol>
 * <li>{@code Object input}: The input sequence this result was calculated from. If the result is no
 * match, this property is {@code null}.</li>
 * <li>{@code boolean isMatch}: {@code true} if a match was found, {@code false} otherwise.</li>
 * <li>{@code int groupCount}: number of capture groups present in the regular expression, including
 * group 0. If the result is no match, this property is {@code 0}.</li>
 * <li>{@link TruffleObject}{@code start}: array of positions where the beginning of the capture
 * group with the given number was found. If the result is no match, this property is an empty
 * array. Capture group number {@code 0} denotes the boundaries of the entire expression. If no
 * match was found for a particular capture group, the returned value at its respective index is
 * {@code -1}.</li>
 * <li>{@link TruffleObject}{@code end}: array of positions where the end of the capture group with
 * the given number was found. If the result is no match, this property is an empty array. Capture
 * group number {@code 0} denotes the boundaries of the entire expression. If no match was found for
 * a particular capture group, the returned value at its respective index is {@code -1}.</li>
 * </ol>
 * </li>
 * </ol>
 * 
 * <pre>
 * Usage example in JavaScript:
 * {@code
 * var pattern = Interop.eval("application/js-regex", "/(a|(b))c/i");
 * var result = pattern.exec("xacy", 0);
 * print(result.isMatch);    // true
 * print(result.input);      // "xacy"
 * print(result.groupCount); // 3
 * print(result.start[0] + ", " + result.end[0]); // "1, 3"
 * print(result.start[1] + ", " + result.end[1]); // "1, 2"
 * print(result.start[2] + ", " + result.end[2]); // "-1, -1"
 * var result2 = pattern.exec("xxx", 0);
 * print(result.isMatch);    // false
 * print(result.input);      // null
 * print(result.groupCount); // 0
 * print(result.start[0] + ", " + result.end[0]); // throws IndexOutOfBoundsException
 * }
 * </pre>
 */

@TruffleLanguage.Registration(name = RegexLanguage.NAME, id = RegexLanguage.ID, mimeType = RegexLanguage.MIME_TYPE, version = "0.1", internal = true)
@ProvidedTags(StandardTags.RootTag.class)
public final class RegexLanguage extends TruffleLanguage<Void> {

    public static final String NAME = "REGEX";
    public static final String ID = "regex";
    public static final String MIME_TYPE = "application/js-regex";

    private final TRegexCompiler tRegexCompiler = new TRegexCompiler(this);
    static final String NO_MATCH_RESULT_IDENTIFIER = "T_REGEX_NO_MATCH_RESULT";
    public static final RegexResult EXPORT_NO_MATCH_RESULT = RegexResult.NO_MATCH;
    static final String THE_ENGINE_IDENTIFIER = "TREGEX_ENGINE";
    public final RegexEngine THE_ENGINE = new RegexEngine(new CachingRegexCompiler(new RegexCompilerWithFallback(tRegexCompiler, new JoniRegexCompiler(this))));
    private final Iterable<Scope> TREGEX_GLOBALS_SCOPE = Collections.singleton(Scope.newBuilder("global", new TRegexScopeObject(this)).build());

    public TRegexCompiler getTRegexCompiler() {
        return tRegexCompiler;
    }

    public static void tRegexValidate(String pattern, String flags) throws RegexSyntaxException {
        RegexParser.validate(new RegexSource(null, pattern, RegexFlags.parseFlags(flags), RegexOptions.DEFAULT));
    }

    @Override
    protected Void createContext(Env env) {
        env.exportSymbol(NO_MATCH_RESULT_IDENTIFIER, EXPORT_NO_MATCH_RESULT);
        env.exportSymbol(THE_ENGINE_IDENTIFIER, THE_ENGINE);
        return null;
    }

    @Override
    protected Object findExportedSymbol(Void context, String globalName, boolean onlyExplicit) {
        if (globalName.equals(NO_MATCH_RESULT_IDENTIFIER)) {
            return EXPORT_NO_MATCH_RESULT;
        }
        if (globalName.equals(THE_ENGINE_IDENTIFIER)) {
            return THE_ENGINE;
        }
        return null;
    }

    @Override
    protected Iterable<Scope> findTopScopes(Void context) {
        return TREGEX_GLOBALS_SCOPE;
    }

    @Override
    protected Object getLanguageGlobal(Void context) {
        return null;
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return object instanceof RegexLanguageObject;
    }

    /**
     * {@link RegexLanguage} is thread-safe - it supports parallel parsing requests as well as
     * parallel access to all {@link RegexLanguageObject}s. Parallel access to
     * {@link com.oracle.truffle.regex.result.LazyCaptureGroupsResult} objects may lead to duplicate
     * execution of code, but no wrong results.
     * 
     * @param thread the thread that accesses the context for the first time.
     * @param singleThreaded <code>true</code> if the access is considered single-threaded,
     *            <code>false</code> if more than one thread is active at the same time.
     * @return always <code>true</code>
     */
    @Override
    protected boolean isThreadAccessAllowed(Thread thread, boolean singleThreaded) {
        return true;
    }
}
