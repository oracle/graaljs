/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.scriptengine;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.graalvm.polyglot.Engine;

public final class GraalJSEngineFactory implements ScriptEngineFactory {

    private static final String ENGINE_NAME = "Graal.js";
    private static final String NAME = "javascript";
    private static final String ENGINE_VERSION = "0.9";
    private static final String LANGUAGE = "ECMAScript";
    private static final String LANGUAGE_VERSION = "ECMA - 262 Edition 6";

    private static final String NASHORN_ENGINE_NAME = "Oracle Nashorn";
    private static final List<String> names = new ArrayList<>(Arrays.asList("Graal.js", "graal.js", "Graal-js", "graal-js", "Graal.JS", "Graal-JS", "GraalJS", "GraalJSPolyglot", "js", "JS",
                    "JavaScript", "javascript", "ECMAScript", "ecmascript"));
    private static final List<String> mimeTypes = new ArrayList<>(Arrays.asList("application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript"));
    private static final List<String> extensions = new ArrayList<>(Arrays.asList("js"));

    public static final boolean RegisterAsNashornScriptEngineFactory = Boolean.getBoolean("graaljs.RegisterGraalJSAsNashorn");

    static {
        ScriptEngineFactory nashornFactory = getNashornEngineFactory();
        if (nashornFactory != null) {
            if (RegisterAsNashornScriptEngineFactory) {
                names.addAll(nashornFactory.getNames());
                mimeTypes.addAll(nashornFactory.getMimeTypes());
                extensions.addAll(nashornFactory.getExtensions());
            }
            clearEngineFactory(nashornFactory);
        }
    }

    private final Engine engine;

    public GraalJSEngineFactory() {
        this.engine = Engine.create();
    }

    GraalJSEngineFactory(Engine engine) {
        this.engine = engine;
    }

    /**
     * Returns the underlying polyglot engine.
     */
    public Engine getPolyglotEngine() {
        return engine;
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public String getEngineVersion() {
        return ENGINE_VERSION;
    }

    @Override
    public List<String> getExtensions() {
        return extensions;
    }

    @Override
    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    @Override
    public List<String> getNames() {
        return names;
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
                throw new IllegalArgumentException("Invalid key");
        }
    }

    @Override
    public GraalJSScriptEngine getScriptEngine() {
        return new GraalJSScriptEngine(this);
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
    public String getOutputStatement(final String toDisplay) {
        return "print(" + toDisplay + ")";
    }

    @Override
    public String getProgram(final String... statements) {
        final StringBuilder sb = new StringBuilder();

        for (final String statement : statements) {
            sb.append(statement).append(';');
        }

        return sb.toString();
    }

    private static ScriptEngineFactory getNashornEngineFactory() {
        for (ScriptEngineFactory factory : new ScriptEngineManager().getEngineFactories()) {
            if (NASHORN_ENGINE_NAME.equals(factory.getEngineName())) {
                return factory;
            }
        }
        return null;
    }

    private static void clearEngineFactory(ScriptEngineFactory factory) {
        assert factory != null;

        try {
            Class<?> clazz = factory.getClass();
            for (String immutableListFieldName : new String[]{"names", "mimeTypes", "extensions"}) {
                Field immutableListField = clazz.getDeclaredField(immutableListFieldName);
                immutableListField.setAccessible(true);
                Object immutableList = immutableListField.get(null);

                Class<?> unmodifiableListClazz = Class.forName("java.util.Collections$UnmodifiableList");
                Field unmodifiableListField = unmodifiableListClazz.getDeclaredField("list");
                unmodifiableListField.setAccessible(true);

                List<?> list = (List<?>) unmodifiableListField.get(immutableList);

                for (int i = 0; i < list.size(); i++) {
                    if (RegisterAsNashornScriptEngineFactory || !list.get(i).toString().toLowerCase().equals("nashorn")) {
                        list.set(i, null);
                    }
                }
            }
        } catch (NullPointerException | ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
            System.err.println("Failed to clear engine names [" + factory.getEngineName() + "]");
            e.printStackTrace();
        }
    }
}
