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
package com.oracle.truffle.js.parser;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.js.builtins.JSDefaultBuiltinLookup;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.builtins.JSFunctionLookup;

public final class JSEngine {
    private static final JSEngine INSTANCE = new JSEngine();

    private final JSFunctionLookup functionLookup;
    private final JSParser parser;

    private JSEngine() {
        this.functionLookup = new JSDefaultBuiltinLookup();
        this.parser = new GraalJSEvaluator();
    }

    public static JSEngine getInstance() {
        return INSTANCE;
    }

    public Evaluator getEvaluator() {
        return parser;
    }

    public JSFunctionLookup getFunctionLookup() {
        return functionLookup;
    }

    public JSParser getParser() {
        return parser;
    }

    private JSContext createContext(JavaScriptLanguage language, TruffleLanguage.Env env) {
        return createContext(language, new GraalJSParserOptions(), env);
    }

    public JSContext createContext(JavaScriptLanguage language, GraalJSParserOptions parserOptions, TruffleLanguage.Env env) {
        JSContextOptions contextOptions = new JSContextOptions(parserOptions);
        return JSContext.createContext(parser, functionLookup, contextOptions, language, env);
    }

    public JSContext createContext(JavaScriptLanguage language, JSContextOptions contextOptions, TruffleLanguage.Env env) {
        return JSContext.createContext(parser, functionLookup, contextOptions, language, env);
    }

    public static JavaScriptLanguage createLanguage() {
        return new JavaScriptLanguage();
    }

    public static JSContext createJSContext(JavaScriptLanguage language, TruffleLanguage.Env env) {
        return JSEngine.getInstance().createContext(language, env);
    }
}
