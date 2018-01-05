/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.parser.JSEngine;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;

/**
 * Adapted from jdk.nashorn.api.scripting.NashornScriptEngineFactory.
 */
public final class GraalJSEngineFactory implements ScriptEngineFactory {

    public static final String ENGINE_NAME = "Graal.js";
    private static final String NAME = "javascript";
    private static final String ENGINE_VERSION = "0.9";
    private static final String LANGUAGE = "ECMAScript";
    private static final String LANGUAGE_VERSION = "ECMA - 262 Edition 6";
    private static final String THREADING = null;

    private static final List<String> names = new ArrayList<>(Arrays.asList("Graal.js", "graal.js", "Graal-js", "graal-js", "Graal.JS", "Graal-JS", "GraalJS", "GraalJS"));
    private static final List<String> mimeTypes = new ArrayList<>(0);
    private static final List<String> extensions = new ArrayList<>(0);

    @Override
    public String getEngineName() {
        return (String) getParameter(ScriptEngine.ENGINE);
    }

    @Override
    public String getEngineVersion() {
        return (String) getParameter(ScriptEngine.ENGINE_VERSION);
    }

    @Override
    public List<String> getExtensions() {
        return Collections.unmodifiableList(extensions);
    }

    @Override
    public String getLanguageName() {
        return (String) getParameter(ScriptEngine.LANGUAGE);
    }

    @Override
    public String getLanguageVersion() {
        return (String) getParameter(ScriptEngine.LANGUAGE_VERSION);
    }

    @Override
    public String getMethodCallSyntax(final String obj, final String method, final String... args) {
        final StringBuilder sb = new StringBuilder().append(obj).append('.').append(method).append('(');
        final int len = args.length;

        if (len > 0) {
            sb.append(args[0]);
        }
        for (int i = 1; i < len; i++) {
            sb.append(',').append(args[i]);
        }
        sb.append(')');

        return sb.toString();
    }

    @Override
    public List<String> getMimeTypes() {
        return Collections.unmodifiableList(mimeTypes);
    }

    @Override
    public List<String> getNames() {
        return Collections.unmodifiableList(names);
    }

    @Override
    @TruffleBoundary
    public String getOutputStatement(final String toDisplay) {
        return "print(" + toDisplay + ")";
    }

    @Override
    public Object getParameter(final String key) {
        switch (key) {
            case ScriptEngine.NAME:
                return NAME;
            case ScriptEngine.ENGINE:
                return ENGINE_NAME;
            case ScriptEngine.ENGINE_VERSION:
                return ENGINE_VERSION;
            case ScriptEngine.LANGUAGE:
                return LANGUAGE;
            case ScriptEngine.LANGUAGE_VERSION:
                return LANGUAGE_VERSION;
            case "THREADING":
                return THREADING;
            default:
                throw new IllegalArgumentException("Invalid key");
        }
    }

    @Override
    public String getProgram(final String... statements) {
        final StringBuilder sb = new StringBuilder();

        for (final String statement : statements) {
            sb.append(statement).append(';');
        }

        return sb.toString();
    }

    @Override
    public GraalJSEngine getScriptEngine() {
        return new GraalJSEngine(this);
    }

    public GraalJSEngine getScriptEngine(JSContextOptions contextOptions) {
        return new GraalJSEngine(this, JSEngine.getInstance().createContext(null, contextOptions, null).createRealm().getContext());
    }

    public GraalJSEngine getScriptEngine(JSContext context) {
        return context == null ? getScriptEngine() : new GraalJSEngine(this, context);
    }
}
