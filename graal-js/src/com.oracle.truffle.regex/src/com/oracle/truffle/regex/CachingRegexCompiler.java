/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.tregex.TRegexOptions;
import com.oracle.truffle.regex.util.LRUCache;
import java.util.Collections;
import java.util.Map;

public class CachingRegexCompiler extends RegexCompiler {

    private final RegexCompiler compiler;

    public CachingRegexCompiler(TruffleObject compiler) {
        this.compiler = ForeignRegexCompiler.importRegexCompiler(compiler);
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

        private final TruffleObject compiledRegexObject;
        private final RegexSyntaxException syntaxException;
        private final UnsupportedRegexException unsupportedRegexException;

        private CompilationResult(TruffleObject compiledRegexObject) {
            this.compiledRegexObject = compiledRegexObject;
            this.syntaxException = null;
            this.unsupportedRegexException = null;
        }

        private CompilationResult(RegexSyntaxException syntaxException) {
            this.compiledRegexObject = null;
            this.syntaxException = syntaxException;
            this.unsupportedRegexException = null;
        }

        private CompilationResult(UnsupportedRegexException unsupportedRegexException) {
            this.compiledRegexObject = null;
            this.syntaxException = null;
            this.unsupportedRegexException = unsupportedRegexException;
        }
    }

    private final Map<RegexSource, CompilationResult> cache = Collections.synchronizedMap(new LRUCache<>(TRegexOptions.RegexMaxCacheSize));

    @Override
    public TruffleObject compile(RegexSource source) throws RegexSyntaxException {
        CompilationResult result = cache.get(source);
        if (result == null) {
            result = doCompile(source);
            cache.put(source, result);
        }
        if (result.compiledRegexObject != null) {
            assert result.syntaxException == null;
            assert result.unsupportedRegexException == null;
            return result.compiledRegexObject;
        } else if (result.syntaxException != null) {
            assert result.compiledRegexObject == null;
            assert result.unsupportedRegexException == null;
            throw result.syntaxException;
        } else {
            assert result.compiledRegexObject == null;
            assert result.syntaxException == null;
            assert result.unsupportedRegexException != null;
            throw result.unsupportedRegexException;
        }
    }

    private CompilationResult doCompile(RegexSource regexSource) {
        try {
            TruffleObject regex = compiler.compile(regexSource);
            return new CompilationResult(regex);
        } catch (RegexSyntaxException e) {
            return new CompilationResult(e);
        } catch (UnsupportedRegexException e) {
            return new CompilationResult(e);
        }
    }
}
