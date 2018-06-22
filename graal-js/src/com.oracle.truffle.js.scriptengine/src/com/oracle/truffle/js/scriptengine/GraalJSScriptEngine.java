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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.Proxy;

/**
 * A Graal.JS implementation of the script engine. It provides access to the polyglot context using
 * {@link #getPolyglotContext()}.
 */
public final class GraalJSScriptEngine extends AbstractScriptEngine implements Compilable, Invocable, AutoCloseable {

    private static final String ID = "js";
    private static final String POLYGLOT_CONTEXT = "polyglot.context";
    private static final String OUT_SYMBOL = "$$internal.out$$";
    private static final String IN_SYMBOL = "$$internal.in$$";
    private static final String ERR_SYMBOL = "$$internal.err$$";

    private final GraalJSEngineFactory factory;
    private final Context.Builder contextConfig;

    private volatile boolean closed;

    GraalJSScriptEngine(GraalJSEngineFactory factory) {
        this(factory.getPolyglotEngine(), null);
    }

    GraalJSScriptEngine(Engine engine, Context.Builder contextConfig) {
        Engine engineToUse = engine;
        if (engineToUse == null) {
            engineToUse = Engine.create();
        }
        Context.Builder contextConfigToUse = contextConfig;
        if (contextConfigToUse == null) {
            // default config
            contextConfigToUse = Context.newBuilder(ID).allowHostAccess(true).allowCreateThread(true);
        }
        this.factory = new GraalJSEngineFactory(engineToUse);
        this.contextConfig = contextConfigToUse.engine(engineToUse);
        this.context.setBindings(new GraalJSBindings(createDefaultContext()), ScriptContext.ENGINE_SCOPE);
    }

    private Context createDefaultContext() {
        DelegatingInputStream in = new DelegatingInputStream();
        DelegatingOutputStream out = new DelegatingOutputStream();
        DelegatingOutputStream err = new DelegatingOutputStream();
        Context.Builder builder = this.contextConfig;
        builder.in(in).out(out).err(err);
        Context ctx = builder.build();
        ctx.getPolyglotBindings().putMember(OUT_SYMBOL, out);
        ctx.getPolyglotBindings().putMember(ERR_SYMBOL, err);
        ctx.getPolyglotBindings().putMember(IN_SYMBOL, in);
        return ctx;
    }

    /**
     * Closes the current context and makes it unusable. Opertions performed after closing will
     * throw an {@link IllegalStateException}.
     */
    @Override
    public void close() {
        getPolyglotContext().close();
        closed = true;
    }

    /**
     * Returns the polyglot engine associated with this script engine.
     */
    public Engine getPolyglotEngine() {
        return factory.getPolyglotEngine();
    }

    /**
     * Returns the polyglot context associated with the default ScriptContext of the engine.
     *
     * @see #getPolyglotContext(ScriptContext) to access the polyglot context of a particular
     *      context.
     */
    public Context getPolyglotContext() {
        return getPolyglotContext(context);
    }

    /**
     * Returns the polyglot context associated with a ScriptContext. If the context is not yet
     * initialized then it will be initialized using the default context builder specified in
     * {@link #create(Engine, org.graalvm.polyglot.Context.Builder)}.
     */
    public Context getPolyglotContext(ScriptContext ctxt) {
        return getOrCreateContext(ctxt);
    }

    static Value evalInternal(Context context, String script) {
        return context.eval(Source.newBuilder(ID, script, "internal-script").internal(true).buildLiteral());
    }

    @Override
    public Bindings createBindings() {
        return new GraalJSBindings(createDefaultContext());
    }

    @Override
    public Object eval(Reader reader, ScriptContext ctxt) throws ScriptException {
        return eval(createSource(reader), ctxt);
    }

    private static Source createSource(Reader reader) throws ScriptException {
        try {
            return Source.newBuilder(ID, reader, "eval-source").build();
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object eval(String script, ScriptContext ctxt) throws ScriptException {
        return eval(createSource(script), ctxt);
    }

    private static Source createSource(String script) {
        return Source.newBuilder(ID, script, "eval-source").buildLiteral();
    }

    private Object eval(Source source, ScriptContext scriptContext) throws ScriptException {
        Context polyglotContext = getOrCreateContext(scriptContext);
        ((DelegatingOutputStream) polyglotContext.getPolyglotBindings().getMember(OUT_SYMBOL).asProxyObject()).setWriter(scriptContext.getWriter());
        ((DelegatingOutputStream) polyglotContext.getPolyglotBindings().getMember(ERR_SYMBOL).asProxyObject()).setWriter(scriptContext.getErrorWriter());
        ((DelegatingInputStream) polyglotContext.getPolyglotBindings().getMember(IN_SYMBOL).asProxyObject()).setReader(scriptContext.getReader());
        try {
            return polyglotContext.eval(source).as(Object.class);
        } catch (PolyglotException e) {
            throw new ScriptException(e);
        }
    }

    private Context getOrCreateContext(ScriptContext ctxt) {
        Bindings engineB = ctxt.getBindings(ScriptContext.ENGINE_SCOPE);
        if (engineB instanceof GraalJSBindings) {
            return ((GraalJSBindings) engineB).getContext();
        } else {
            Context polyglotContext = createContext(engineB);
            GraalJSBindings bindings = new GraalJSBindings(polyglotContext);
            bindings.clear();
            bindings.putAll(ctxt.getBindings(ScriptContext.ENGINE_SCOPE));
            return polyglotContext;
        }
    }

    private Context createContext(Bindings engineB) {
        Object ctx = engineB.get(POLYGLOT_CONTEXT);
        if (ctx == null || !(ctx instanceof Context)) {
            ctx = createDefaultContext();
            engineB.put(POLYGLOT_CONTEXT, ctx);
        }
        return (Context) ctx;
    }

    @Override
    public GraalJSEngineFactory getFactory() {
        return factory;
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        if (thiz == null) {
            throw new IllegalArgumentException("thiz is not a valid object.");
        }
        Value value = getPolyglotContext().asValue(thiz);
        Value function = value.getMember(name);
        return invoke(name, function, args);
    }

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        Value value = getOrCreateContext(context).getBindings(ID).getMember(name);
        return invoke(name, value, args);
    }

    public static Object invoke(String methodName, Value function, Object... args) throws NoSuchMethodException, ScriptException {
        if (function == null) {
            throw new NoSuchMethodException(methodName);
        } else if (!function.canExecute()) {
            throw new NoSuchMethodException(methodName + " is not a function");
        }
        try {
            return function.execute(args).as(Object.class);
        } catch (PolyglotException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public <T> T getInterface(Class<T> clasz) {
        return evalInternal(getPolyglotContext(), "this").as(clasz);
    }

    @Override
    public <T> T getInterface(Object thiz, Class<T> clasz) {
        return getPolyglotContext().asValue(thiz).as(clasz);
    }

    @Override
    public CompiledScript compile(String script) throws ScriptException {
        if (closed) {
            throw new IllegalStateException("Context already closed.");
        }
        Source source = createSource(script);
        return new CompiledScript() {
            @Override
            public ScriptEngine getEngine() {
                return GraalJSScriptEngine.this;
            }

            @Override
            public Object eval(ScriptContext ctx) throws ScriptException {
                return GraalJSScriptEngine.this.eval(source, ctx);
            }
        };
    }

    @Override
    public CompiledScript compile(Reader reader) throws ScriptException {
        if (closed) {
            throw new IllegalStateException("Context already closed.");
        }
        Source source = createSource(reader);
        return new CompiledScript() {
            @Override
            public ScriptEngine getEngine() {
                return GraalJSScriptEngine.this;
            }

            @Override
            public Object eval(ScriptContext ctx) throws ScriptException {
                return GraalJSScriptEngine.this.eval(source, ctx);
            }
        };
    }

    private static class DelegatingInputStream extends InputStream implements Proxy {

        private Reader reader;

        @Override
        public int read() throws IOException {
            if (reader != null) {
                return reader.read();
            }
            return 0;
        }

        void setReader(Reader reader) {
            this.reader = reader;
        }

    }

    private static class DelegatingOutputStream extends OutputStream implements Proxy {

        private Writer writer;

        @Override
        public void write(int b) throws IOException {
            if (writer != null) {
                writer.write(b);
            }
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        void setWriter(Writer writer) {
            this.writer = writer;
        }

    }

    /**
     * Creates a new GraalJSScriptEngine with default configuration.
     *
     * @see #create(Engine, Context.Builder) to customize the configuration.
     */
    public static GraalJSScriptEngine create() {
        return create(null, null);
    }

    /**
     * Creates a new GraalJS script engine from a polyglot Engine instance with a base configuration
     * for new polyglot {@link Context} instances. Polyglot context instances can be accessed from
     * {@link ScriptContext} instances using {@link #getPolyglotContext()}. The
     * {@link Builder#out(OutputStream) out},{@link Builder#err(OutputStream) err} and
     * {@link Builder#in(InputStream) in} stream configuration are not inherited from the provided
     * polyglot context config. Instead {@link ScriptContext} output and input streams are used.
     *
     * @param engine the engine to be used for context configurations or <code>null</code> if a
     *            default engine should be used.
     * @param newContextConfig a base configuration to create new context instances or
     *            <code>null</code> if the default configuration should be used to construct new
     *            context instances.
     */
    public static GraalJSScriptEngine create(Engine engine, Context.Builder newContextConfig) {
        return new GraalJSScriptEngine(engine, newContextConfig);
    }

}
