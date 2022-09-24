/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

/**
 * ScriptEngine implementation returned by {@code GraalJSEngineFactory} when JavaScript support is
 * not installed. Unfortunately, there is no JavaDoc-compliant way for {@code ScriptEngineFactory}
 * to claim that it is not able to provide {@code ScriptEngine} (because of a missing dependency).
 * Hence, we provide this minimal compliant implementation instead. An encounter of this engine is a
 * sign of a misconfiguration. Users should not see this engine when JavaScript support is installed
 * properly.
 */
public final class PlaceholderScriptEngine extends AbstractScriptEngine {
    private static final String OUTPUT_PREFIX = "print(";

    private final GraalJSEngineFactory factory;

    PlaceholderScriptEngine(GraalJSEngineFactory factory) {
        this.factory = factory;
        setBindings(new PlaceholderBindings(), ScriptContext.ENGINE_SCOPE);
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        // engine bindings use global bindings as a fallback
        if (scope == ScriptContext.ENGINE_SCOPE) {
            if (bindings instanceof PlaceholderBindings) {
                ((PlaceholderBindings) bindings).setGlobalBindings(getBindings(ScriptContext.GLOBAL_SCOPE));
            }
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
            Bindings engineBindings = getBindings(ScriptContext.ENGINE_SCOPE);
            if (engineBindings instanceof PlaceholderBindings) {
                ((PlaceholderBindings) engineBindings).setGlobalBindings(bindings);
            }
        }
        super.setBindings(bindings, scope);
    }

    @Override
    public Object eval(String script, ScriptContext ctx) throws ScriptException {
        Objects.requireNonNull(ctx);
        for (String statement : script.split(";")) {
            if (statement.isEmpty()) {
                continue;
            }
            if (!statement.endsWith(")")) {
                throw error();
            }
            if (statement.startsWith(OUTPUT_PREFIX)) {
                try {
                    String output = statement.substring(OUTPUT_PREFIX.length(), statement.length() - 1);
                    ctx.getWriter().write(output);
                } catch (IOException ioex) {
                    throw new ScriptException(ioex);
                }
            } else {
                int dot = statement.indexOf('.');
                if (dot == -1) {
                    throw error();
                }
                int argsStart = statement.indexOf('(', dot);
                if (argsStart == -1) {
                    throw error();
                }

                String objectName = statement.substring(0, dot);
                String methodName = statement.substring(dot + 1, argsStart);
                String argsString = statement.substring(argsStart + 1, statement.length() - 1);
                String[] argNames = argsString.isEmpty() ? new String[0] : argsString.split(",");

                Bindings bindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE);
                Object object = bindings.get(objectName);
                if (object == null) {
                    throw error();
                }

                Object[] args = new Object[argNames.length];
                Class<?>[] argClasses = new Class<?>[argNames.length];
                for (int i = 0; i < argNames.length; i++) {
                    Object arg = bindings.get(argNames[i]);
                    if (arg == null) {
                        throw error();
                    }
                    argClasses[i] = arg.getClass();
                    args[i] = arg;
                }

                try {
                    Method method = object.getClass().getMethod(methodName, argClasses);
                    return method.invoke(object, args);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                    throw error();
                }
            }
        }
        return null;
    }

    private static ScriptException error() {
        return new ScriptException("JavaScript implementation not found!");
    }

    @Override
    public Object eval(Reader reader, ScriptContext ctx) throws ScriptException {
        return eval(GraalJSScriptEngine.read(reader), ctx);
    }

    @Override
    public Bindings createBindings() {
        return new PlaceholderBindings();
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    private static final class PlaceholderBindings extends SimpleBindings {

        private Bindings globalBindings;

        void setGlobalBindings(Bindings bindings) {
            this.globalBindings = bindings;
        }

        @Override
        public Object get(Object key) {
            return (globalBindings == null || containsKey(key)) ? super.get(key) : globalBindings.get(key);
        }

    }

}
