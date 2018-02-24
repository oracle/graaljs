/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.regex.tregex.TRegexOptions;
import com.oracle.truffle.regex.util.LRUCache;
import java.util.Collections;
import java.util.Map;

public class CachingRegexEngine extends RegexEngine {

    private final RegexEngine engine;

    public CachingRegexEngine(RegexEngine engine) {
        this.engine = engine;
    }

    /**
     * Trying to parse and compile a regular expression can produce one of three results. This class
     * encodes the sum of these three possibilities.
     * 
     * <ul>
     * <li>the regular expression is successfully compiled: compiledRegex is not null</li>
     * <li>there is a syntax error in the regular expression: syntaxException is not null</li>
     * <li>the regular expression is not supported by the engine: unsupportedRegexException is not null</li>
     * </ul>
     */
    private static final class CompilationResult {

        private final CompiledRegex compiledRegex;
        private final RegexSyntaxException syntaxException;
        private final UnsupportedRegexException unsupportedRegexException;

        private CompilationResult(CompiledRegex compiledRegex) {
            this.compiledRegex = compiledRegex;
            this.syntaxException = null;
            this.unsupportedRegexException = null;
        }

        private CompilationResult(RegexSyntaxException syntaxException) {
            this.compiledRegex = null;
            this.syntaxException = syntaxException;
            this.unsupportedRegexException = null;
        }

        private CompilationResult(UnsupportedRegexException unsupportedRegexException) {
            this.compiledRegex = null;
            this.syntaxException = null;
            this.unsupportedRegexException = unsupportedRegexException;
        }
    }

    private final Map<RegexSource, CompilationResult> cache = Collections.synchronizedMap(new LRUCache<>(TRegexOptions.RegexMaxCacheSize));

    @Override
    public CompiledRegex compile(RegexSource source) throws RegexSyntaxException {
        CompilationResult result = cache.get(source);
        if (result == null) {
            result = doCompile(source);
            cache.put(source, result);
        }
        if (result.compiledRegex != null) {
            assert result.syntaxException == null;
            assert result.unsupportedRegexException == null;
            return result.compiledRegex;
        } else if (result.syntaxException != null) {
            assert result.compiledRegex == null;
            assert result.unsupportedRegexException == null;
            throw result.syntaxException;
        } else {
            assert result.compiledRegex == null;
            assert result.syntaxException == null;
            assert result.unsupportedRegexException != null;
            throw result.unsupportedRegexException;
        }
    }

    private CompilationResult doCompile(RegexSource regexSource) {
        try {
            CompiledRegex regex = engine.compile(regexSource);
            return new CompilationResult(regex);
        } catch (RegexSyntaxException e) {
            return new CompilationResult(e);
        } catch (UnsupportedRegexException e) {
            return new CompilationResult(e);
        }
    }
}
