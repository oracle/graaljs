/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine.MagicBindingsOptionSetter;

final class GraalJSBindings extends AbstractMap<String, Object> implements Bindings, AutoCloseable {
    private static final String SCRIPT_CONTEXT_GLOBAL_BINDINGS_IMPORT_FUNCTION_NAME = "importScriptEngineGlobalBindings";

    private static final TypeLiteral<Map<String, Object>> STRING_MAP = new TypeLiteral<>() {
    };

    private Context context;
    private Map<String, Object> global;
    private Value deleteProperty;
    private Value clear;
    private Context.Builder contextBuilder;
    // ScriptContext of the ScriptEngine where these bindings form ENGINE_SCOPE bindings
    private ScriptContext engineScriptContext;
    private ScriptEngine engineBinding;

    GraalJSBindings(Context.Builder contextBuilder, ScriptContext scriptContext, ScriptEngine engine) {
        this.contextBuilder = contextBuilder;
        this.engineScriptContext = scriptContext;
        this.engineBinding = engine;
    }

    GraalJSBindings(Context context, ScriptContext scriptContext, ScriptEngine engine) {
        this.context = context;
        this.engineScriptContext = scriptContext;
        this.engineBinding = engine;
        initGlobal();
    }

    private void requireContext() {
        if (context == null) {
            initContext();
        }
    }

    private void initContext() {
        context = GraalJSScriptEngine.createDefaultContext(contextBuilder, engineScriptContext);
        initGlobal();
    }

    private void initGlobal() {
        this.global = GraalJSScriptEngine.evalInternal(context, "this").as(STRING_MAP);
        updateEngineBinding();
        updateContextBinding();
    }

    private void updateEngineBinding() {
        updateBinding("engine", engineBinding);
    }

    private void updateContextBinding() {
        if (engineScriptContext != null) {
            updateBinding("context", engineScriptContext);
        }
    }

    private void updateBinding(String key, Object value) {
        String code = "(function(key, value) {" +
                        "try {" +
                        "    Object.defineProperty(this, key, { value: value, writable: true, configurable: true });" +
                        "} catch (e) {}" +
                        "})";
        GraalJSScriptEngine.evalInternal(context, code).execute(key, value);
    }

    private Value deletePropertyFunction() {
        if (this.deleteProperty == null) {
            this.deleteProperty = GraalJSScriptEngine.evalInternal(context, "(function(obj, prop) {delete obj[prop]})");
        }
        return this.deleteProperty;
    }

    private Value clearFunction() {
        if (this.clear == null) {
            this.clear = GraalJSScriptEngine.evalInternal(context, "(function(obj) {for (var prop in obj) {delete obj[prop]}})");
        }
        return this.clear;
    }

    @Override
    public Object put(String name, Object v) {
        checkKey(name);
        if (name.startsWith(GraalJSScriptEngine.MAGIC_OPTION_PREFIX)) {
            if (context == null) {
                MagicBindingsOptionSetter optionSetter = GraalJSScriptEngine.MAGIC_BINDINGS_OPTION_MAP.get(name);
                if (optionSetter == null) {
                    throw new IllegalArgumentException("unkown graal-js option \"" + name + "\"");
                } else {
                    contextBuilder = optionSetter.setOption(contextBuilder, v);
                    return true;
                }
            } else {
                throw magicOptionContextInitializedError(name);
            }
        }
        requireContext();
        return global.put(name, v);
    }

    @Override
    public void clear() {
        if (context != null) {
            clearFunction().execute(global);
        }
    }

    @Override
    public Object get(Object key) {
        checkKey((String) key);
        requireContext();
        if (engineScriptContext != null) {
            importGlobalBindings(engineScriptContext);
        }
        return global.get(key);
    }

    private static void checkKey(String key) {
        Objects.requireNonNull(key, "key can not be null");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key can not be empty");
        }
    }

    @Override
    public Object remove(Object key) {
        requireContext();
        Object prev = get(key);
        deletePropertyFunction().execute(global, key);
        return prev;
    }

    public Context getContext() {
        requireContext();
        return context;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        requireContext();
        return global.entrySet();
    }

    @Override
    public void close() {
        if (context != null) {
            context.close();
        }
    }

    private static IllegalStateException magicOptionContextInitializedError(String name) {
        return new IllegalStateException(String.format("failed to set graal-js option \"%s\": js context is already initialized", name));
    }

    void importGlobalBindings(ScriptContext scriptContext) {
        Bindings globalBindings = scriptContext.getBindings(ScriptContext.GLOBAL_SCOPE);
        if (globalBindings != null && !globalBindings.isEmpty() && this != globalBindings) {
            ProxyObject bindingsProxy = ProxyObject.fromMap(Collections.unmodifiableMap(globalBindings));
            getContext().getBindings("js").getMember(SCRIPT_CONTEXT_GLOBAL_BINDINGS_IMPORT_FUNCTION_NAME).execute(bindingsProxy);
        }
    }

    void updateEngineScriptContext(ScriptContext scriptContext) {
        engineScriptContext = scriptContext;
        if (context != null) {
            updateContextBinding();
        } else {
            initContext(); // This will also call updateContextBinding()
        }
    }

}
