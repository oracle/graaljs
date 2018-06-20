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
    private static final String ENGINE_VERSION = "1.0"; // also in AbstractJavaScriptLanguage
    private static final String LANGUAGE = "ECMAScript";
    private static final String LANGUAGE_VERSION = "ECMA - 262 Edition 6";

    private static final String NASHORN_ENGINE_NAME = "Oracle Nashorn";
    private static final List<String> names = new ArrayList<>(Arrays.asList("Graal.js", "graal.js", "Graal-js", "graal-js", "Graal.JS", "Graal-JS", "GraalJS", "GraalJSPolyglot", "js", "JS",
                    "JavaScript", "javascript", "ECMAScript", "ecmascript"));
    private static final List<String> mimeTypes = new ArrayList<>(Arrays.asList("application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript"));
    private static final List<String> extensions = new ArrayList<>(Arrays.asList("js"));

    public static final boolean RegisterAsNashornScriptEngineFactory = Boolean.getBoolean("graaljs.RegisterGraalJSAsNashorn");

    static {
        boolean java8 = System.getProperty("java.specification.version").compareTo("1.9") < 0;
        if (java8) {
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
