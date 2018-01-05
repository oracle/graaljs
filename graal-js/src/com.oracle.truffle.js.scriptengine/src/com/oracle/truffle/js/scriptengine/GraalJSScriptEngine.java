/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.Proxy;

public class GraalJSScriptEngine extends AbstractScriptEngine implements Compilable, Invocable {

    private static final String POLYGLOT_CONTEXT = "polyglot.context";
    private static final String OUT_SYMBOL = "$$internal.out$$";
    private static final String IN_SYMBOL = "$$internal.in$$";
    private static final String ERR_SYMBOL = "$$internal.err$$";

    private final GraalJSEngineFactory factory;

    GraalJSScriptEngine(GraalJSEngineFactory factory) {
        this.factory = factory;
        this.context.setBindings(new GraalJSBindings(createDefaultContext()), ScriptContext.ENGINE_SCOPE);
    }

    private Context createDefaultContext() {
        DelegatingInputStream in = new DelegatingInputStream();
        DelegatingOutputStream out = new DelegatingOutputStream();
        DelegatingOutputStream err = new DelegatingOutputStream();
        Context ctx = Context.newBuilder("js").engine(factory.getEngine()).allowHostAccess(true).allowCreateThread(true).in(in).out(out).err(err).build();
        Value global = evalInternal(ctx, "this");
        evalInternal(ctx, "Object.defineProperty(this,'arguments',{enumerable:false,iterable:false})");
        evalInternal(ctx, "Object.defineProperty(this,'__engine',{enumerable:false,iterable:false})");
        global.putMember("arguments", evalInternal(ctx, "new Array(0)"));
        global.putMember("__engine", this);
        ctx.exportSymbol(OUT_SYMBOL, out);
        ctx.exportSymbol(ERR_SYMBOL, err);
        ctx.exportSymbol(IN_SYMBOL, in);
        return ctx;
    }

    static Value evalInternal(Context context, String script) {
        return context.eval(Source.newBuilder("js", script, "internal-script").internal(true).buildLiteral());
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
            return Source.newBuilder("js", reader, "eval-source").build();
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object eval(String script, ScriptContext ctxt) throws ScriptException {
        return eval(createSource(script), ctxt);
    }

    private static Source createSource(String script) {
        return Source.newBuilder("js", script, "eval-source").buildLiteral();
    }

    private Object eval(Source source, ScriptContext scriptContext) throws ScriptException {
        Context polyglotContext = getContext(scriptContext);
        ((DelegatingOutputStream) polyglotContext.importSymbol(OUT_SYMBOL).asHostObject()).setWriter(scriptContext.getWriter());
        ((DelegatingOutputStream) polyglotContext.importSymbol(ERR_SYMBOL).asHostObject()).setWriter(scriptContext.getErrorWriter());
        ((DelegatingInputStream) polyglotContext.importSymbol(IN_SYMBOL).asHostObject()).setReader(scriptContext.getReader());
        try {
            return GraalJSBindings.convertGuestToHost(polyglotContext.eval(source));
        } catch (PolyglotException e) {
            throw new ScriptException(e);
        }
    }

    private Context getContext(ScriptContext ctxt) {
        return getBindings(ctxt).getContext();
    }

    private GraalJSBindings getBindings(ScriptContext ctxt) {
        Bindings engineB = ctxt.getBindings(ScriptContext.ENGINE_SCOPE);
        if (engineB instanceof GraalJSBindings) {
            return (GraalJSBindings) engineB;
        } else {
            Context polyglotContext = createContext(engineB);
            GraalJSBindings bindings = new GraalJSBindings(polyglotContext);
            bindings.clear();
            bindings.putAll(ctxt.getBindings(ScriptContext.ENGINE_SCOPE));
            return bindings;
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
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        if (thiz == null || !(thiz instanceof Value)) {
            throw new IllegalArgumentException("thiz is not a valid object.");
        }
        Value value = (Value) thiz;
        Value function = value.getMember(name);
        return invoke(name, function, args);
    }

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        Value value = getContext(context).lookup("js", name);
        return invoke(name, value, args);
    }

    public static Object invoke(String methodName, Value function, Object... args) throws NoSuchMethodException, ScriptException {
        if (function == null) {
            throw new NoSuchMethodException(methodName);
        } else if (!function.canExecute()) {
            throw new NoSuchMethodException(methodName + " is not a fucntion");
        }

        try {
            return GraalJSBindings.convertGuestToHost(function.execute(args));
        } catch (PolyglotException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public <T> T getInterface(Class<T> clasz) {
        // not supported yet. could be implemented with proxies
        return null;
    }

    @Override
    public <T> T getInterface(Object thiz, Class<T> clasz) {
        // not supported yet. could be implemented with proxies
        return null;
    }

    @Override
    public CompiledScript compile(String script) throws ScriptException {
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

}
