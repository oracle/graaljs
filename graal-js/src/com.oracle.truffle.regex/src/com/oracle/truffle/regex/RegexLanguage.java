/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.tregex.parser.RegexParser;

import java.util.Collections;

/**
 * Truffle Regular Expression Language
 * <p>
 * This language represents classic regular expressions, currently in JavaScript flavor only. By
 * evaluating any source, or by importing the T_REGEX_ENGINE_BUILDER, you get access to the
 * {@link RegexEngineBuilder}. By calling this builder, you can build your custom
 * {@link RegexEngine} which implements your flavor of regular expressions and uses your fallback
 * compiler for expressions not covered. The {@link RegexEngine} accepts regular expression patterns
 * and flags and compiles them to {@link RegexObject}s, which you can use to match the regular
 * expressions against strings.
 * <p>
 * 
 * <pre>
 * Usage example in JavaScript:
 * {@code
 * var engineBuilder = Interop.eval("application/js-regex", "");
 * // or var engineBuilder = Interop.import("T_REGEX_ENGINE_BUILDER"); after initializing the language
 * var engine = engineBuilder();
 * var pattern = engine("(a|(b))c", "i");
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

    static final String NO_MATCH_RESULT_IDENTIFIER = "T_REGEX_NO_MATCH_RESULT";
    public static final RegexResult EXPORT_NO_MATCH_RESULT = RegexResult.NO_MATCH;
    static final String ENGINE_BUILDER_IDENTIFIER = "T_REGEX_ENGINE_BUILDER";
    public final RegexEngineBuilder engineBuilder = new RegexEngineBuilder(this);
    private final Iterable<Scope> tRegexGlobalsScope = Collections.singleton(Scope.newBuilder("global", new TRegexScopeObject(this)).build());

    private final CallTarget getEngineBuilderCT = Truffle.getRuntime().createCallTarget(RootNode.createConstantNode(engineBuilder));

    public static void validateRegex(String pattern, String flags) throws RegexSyntaxException {
        RegexParser.validate(new RegexSource(pattern, RegexFlags.parseFlags(flags)));
    }

    @Override
    protected CallTarget parse(ParsingRequest parsingRequest) {
        return getEngineBuilderCT;
    }

    @Override
    protected Void createContext(Env env) {
        env.exportSymbol(NO_MATCH_RESULT_IDENTIFIER, EXPORT_NO_MATCH_RESULT);
        env.exportSymbol(ENGINE_BUILDER_IDENTIFIER, engineBuilder);
        return null;
    }

    @Override
    protected Object findExportedSymbol(Void context, String globalName, boolean onlyExplicit) {
        if (globalName.equals(NO_MATCH_RESULT_IDENTIFIER)) {
            return EXPORT_NO_MATCH_RESULT;
        }
        if (globalName.equals(ENGINE_BUILDER_IDENTIFIER)) {
            return engineBuilder;
        }
        return null;
    }

    @Override
    protected Iterable<Scope> findTopScopes(Void context) {
        return tRegexGlobalsScope;
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
