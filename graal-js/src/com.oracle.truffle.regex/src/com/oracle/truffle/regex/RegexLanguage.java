/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.RootNode;
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
 * var engineBuilder = Polyglot.eval("application/tregex", "");
 * // or var engineBuilder = Polyglot.import("T_REGEX_ENGINE_BUILDER"); after initializing the language
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
    public static final String MIME_TYPE = "application/tregex";

    public final RegexEngineBuilder engineBuilder = new RegexEngineBuilder(this);

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
        return null;
    }

    @Override
    protected Iterable<Scope> findTopScopes(Void context) {
        return Collections.emptySet();
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
