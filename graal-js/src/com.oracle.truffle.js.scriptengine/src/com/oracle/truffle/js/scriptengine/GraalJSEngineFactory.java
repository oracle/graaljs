/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.scriptengine;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.graalvm.polyglot.Engine;

public final class GraalJSEngineFactory implements ScriptEngineFactory {

    private static final String ENGINE_NAME = "Graal.js";
    private static final String NAME = "javascript";
    private static final String LANGUAGE = "ECMAScript";
    private static final String LANGUAGE_VERSION = "ECMAScript 262 Edition 11";

    private static final List<String> NAMES = List.of("js", "JS", "JavaScript", "javascript", "ECMAScript", "ecmascript",
                    "Graal.js", "graal.js", "Graal-js", "graal-js", "Graal.JS", "Graal-JS", "GraalJS", "GraalJSPolyglot");
    private static final List<String> MIME_TYPES = List.of("application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript");
    private static final List<String> EXTENSIONS = List.of("js", "mjs");

    private WeakReference<Engine> defaultEngine;
    private final Engine userDefinedEngine;

    public GraalJSEngineFactory() {
        this.defaultEngine = null; // lazy
        this.userDefinedEngine = null;
    }

    GraalJSEngineFactory(Engine engine) {
        this.userDefinedEngine = engine;
    }

    private static Engine createDefaultEngine() {
        return Engine.newBuilder().allowExperimentalOptions(true).build();
    }

    /**
     * Returns the underlying polyglot engine.
     */
    public Engine getPolyglotEngine() {
        if (userDefinedEngine != null) {
            return userDefinedEngine;
        } else {
            Engine engine = defaultEngine == null ? null : defaultEngine.get();
            if (engine == null) {
                engine = createDefaultEngine();
                defaultEngine = new WeakReference<>(engine);
            }
            return engine;
        }
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public String getEngineVersion() {
        return getPolyglotEngine().getVersion();
    }

    @Override
    public List<String> getExtensions() {
        return EXTENSIONS;
    }

    @Override
    public List<String> getMimeTypes() {
        return MIME_TYPES;
    }

    @Override
    public List<String> getNames() {
        return NAMES;
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE;
    }

    @Override
    public String getLanguageVersion() {
        return LANGUAGE_VERSION;
    }

    @Override
    public Object getParameter(String key) {
        switch (key) {
            case ScriptEngine.NAME:
                return NAME;
            case ScriptEngine.ENGINE:
                return getEngineName();
            case ScriptEngine.ENGINE_VERSION:
                return getEngineVersion();
            case ScriptEngine.LANGUAGE:
                return getLanguageName();
            case ScriptEngine.LANGUAGE_VERSION:
                return getLanguageVersion();
            default:
                return null;
        }
    }

    @Override
    public GraalJSScriptEngine getScriptEngine() {
        return new GraalJSScriptEngine(this);
    }

    @Override
    public String getMethodCallSyntax(final String obj, final String method, final String... args) {
        Objects.requireNonNull(obj);
        Objects.requireNonNull(method);
        final StringBuilder sb = new StringBuilder().append(obj).append('.').append(method).append('(');
        final int len = args.length;

        if (len > 0) {
            Objects.requireNonNull(args[0]);
            sb.append(args[0]);
        }
        for (int i = 1; i < len; i++) {
            Objects.requireNonNull(args[i]);
            sb.append(',').append(args[i]);
        }
        sb.append(')');

        return sb.toString();
    }

    @Override
    public String getOutputStatement(final String toDisplay) {
        return "print(" + toDisplay + ")";
    }

    @Override
    public String getProgram(final String... statements) {
        final StringBuilder sb = new StringBuilder();

        for (final String statement : statements) {
            Objects.requireNonNull(statement);
            sb.append(statement).append(';');
        }

        return sb.toString();
    }
}
