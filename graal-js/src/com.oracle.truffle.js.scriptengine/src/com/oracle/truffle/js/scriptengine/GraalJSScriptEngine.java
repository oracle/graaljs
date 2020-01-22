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
import java.util.function.Predicate;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
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
    private static final String JS_SYNTAX_EXTENSIONS_OPTION = "js.syntax-extensions";
    private static final String JS_SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT_OPTION = "js.script-engine-global-scope-import";
    private static final String JS_LOAD_OPTION = "js.load";
    private static final String JS_PRINT_OPTION = "js.print";
    private static final String SCRIPT_CONTEXT_GLOBAL_BINDINGS_IMPORT_FUNCTION_NAME = "importScriptEngineGlobalBindings";
    private static final String NASHORN_COMPATIBILITY_MODE_SYSTEM_PROPERTY = "polyglot.js.nashorn-compat";
    static final String MAGIC_OPTION_PREFIX = "polyglot.js.";

    interface MagicBindingsOptionSetter {

        String getOptionKey();

        Context.Builder setOption(Builder builder, Object value);
    }

    private static boolean toBoolean(MagicBindingsOptionSetter optionSetter, Object value) {
        if (!(value instanceof Boolean)) {
            throw magicOptionValueErrorBool(optionSetter.getOptionKey(), value);
        }
        return (Boolean) value;
    }

    private static final MagicBindingsOptionSetter[] MAGIC_OPTION_SETTERS = new MagicBindingsOptionSetter[]{
                    new MagicBindingsOptionSetter() {

                        @Override
                        public String getOptionKey() {
                            return MAGIC_OPTION_PREFIX + "allowHostAccess";
                        }

                        @Override
                        public Builder setOption(Builder builder, Object value) {
                            return builder.allowHostAccess(toBoolean(this, value) ? HostAccess.ALL : HostAccess.NONE);
                        }
                    },
                    new MagicBindingsOptionSetter() {

                        @Override
                        public String getOptionKey() {
                            return MAGIC_OPTION_PREFIX + "allowNativeAccess";
                        }

                        @Override
                        public Builder setOption(Builder builder, Object value) {
                            return builder.allowNativeAccess(toBoolean(this, value));
                        }
                    },
                    new MagicBindingsOptionSetter() {

                        @Override
                        public String getOptionKey() {
                            return MAGIC_OPTION_PREFIX + "allowCreateThread";
                        }

                        @Override
                        public Builder setOption(Builder builder, Object value) {
                            return builder.allowCreateThread(toBoolean(this, value));
                        }
                    },
                    new MagicBindingsOptionSetter() {

                        @Override
                        public String getOptionKey() {
                            return MAGIC_OPTION_PREFIX + "allowIO";
                        }

                        @Override
                        public Builder setOption(Builder builder, Object value) {
                            return builder.allowIO(toBoolean(this, value));
                        }
                    },
                    new MagicBindingsOptionSetter() {

                        @Override
                        public String getOptionKey() {
                            return MAGIC_OPTION_PREFIX + "allowHostClassLookup";
                        }

                        @SuppressWarnings("unchecked")
                        @Override
                        public Builder setOption(Builder builder, Object value) {
                            if (value instanceof Boolean) {
                                boolean enabled = (Boolean) value;
                                return builder.allowHostClassLookup(enabled ? s -> true : null);
                            } else {
                                try {
                                    return builder.allowHostClassLookup((Predicate<String>) value);
                                } catch (ClassCastException e) {
                                    throw new IllegalArgumentException(
                                                    String.format("failed to set graal-js option \"%s\": expected a boolean or Predicate<String> value, got \"%s\"", getOptionKey(), value));
                                }
                            }
                        }
                    },
                    new MagicBindingsOptionSetter() {

                        @Override
                        public String getOptionKey() {
                            return MAGIC_OPTION_PREFIX + "allowHostClassLoading";
                        }

                        @Override
                        public Builder setOption(Builder builder, Object value) {
                            return builder.allowHostClassLoading(toBoolean(this, value));
                        }
                    },
                    new MagicBindingsOptionSetter() {

                        @Override
                        public String getOptionKey() {
                            return MAGIC_OPTION_PREFIX + "allowAllAccess";
                        }

                        @Override
                        public Builder setOption(Builder builder, Object value) {
                            return builder.allowAllAccess(toBoolean(this, value));
                        }
                    },
                    new MagicBindingsOptionSetter() {

                        @Override
                        public String getOptionKey() {
                            return MAGIC_OPTION_PREFIX + "nashorn-compat";
                        }

                        @Override
                        public Builder setOption(Builder builder, Object value) {
                            boolean val = toBoolean(this, value);
                            return (val ? builder.allowAllAccess(true) : builder).option("js.nashorn-compat", String.valueOf(val));
                        }
                    }
    };

    private static final EconomicSet<String> MAGIC_BINDINGS_OPTION_KEYS = EconomicSet.create();
    static final EconomicMap<String, MagicBindingsOptionSetter> MAGIC_BINDINGS_OPTION_MAP = EconomicMap.create();
    private static final boolean NASHORN_COMPATIBILITY_MODE = Boolean.getBoolean(NASHORN_COMPATIBILITY_MODE_SYSTEM_PROPERTY);

    static {
        for (MagicBindingsOptionSetter setter : MAGIC_OPTION_SETTERS) {
            MAGIC_BINDINGS_OPTION_KEYS.add(setter.getOptionKey());
            MAGIC_BINDINGS_OPTION_MAP.put(setter.getOptionKey(), setter);
        }
    }

    private final GraalJSEngineFactory factory;
    private final Context.Builder contextConfig;

    private volatile boolean closed;
    private boolean evalCalled;

    GraalJSScriptEngine(GraalJSEngineFactory factory) {
        this(factory.getPolyglotEngine(), null);
    }

    GraalJSScriptEngine(Engine engine, Context.Builder contextConfig) {
        Engine engineToUse = engine;
        if (engineToUse == null) {
            engineToUse = Engine.newBuilder().allowExperimentalOptions(true).build();
        }
        Context.Builder contextConfigToUse = contextConfig;
        if (contextConfigToUse == null) {
            // default config
            contextConfigToUse = Context.newBuilder(ID).allowExperimentalOptions(true).option(JS_SYNTAX_EXTENSIONS_OPTION, "true").option(JS_LOAD_OPTION, "true").option(JS_PRINT_OPTION, "true");
            if (NASHORN_COMPATIBILITY_MODE) {
                contextConfigToUse.allowAllAccess(true);
            }
        }
        this.factory = new GraalJSEngineFactory(engineToUse);
        this.contextConfig = contextConfigToUse.option(JS_SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT_OPTION, "true").engine(engineToUse);
        this.context.setBindings(new GraalJSBindings(this.contextConfig), ScriptContext.ENGINE_SCOPE);
    }

    static Context createDefaultContext(Context.Builder builder) {
        DelegatingInputStream in = new DelegatingInputStream();
        DelegatingOutputStream out = new DelegatingOutputStream();
        DelegatingOutputStream err = new DelegatingOutputStream();
        builder.in(in).out(out).err(err);
        Context ctx = builder.build();
        ctx.getPolyglotBindings().putMember(OUT_SYMBOL, out);
        ctx.getPolyglotBindings().putMember(ERR_SYMBOL, err);
        ctx.getPolyglotBindings().putMember(IN_SYMBOL, in);
        return ctx;
    }

    /**
     * Closes the current context and makes it unusable. Operations performed after closing will
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
        return getOrCreateGraalJSBindings(ctxt).getContext();
    }

    static Value evalInternal(Context context, String script) {
        return context.eval(Source.newBuilder(ID, script, "internal-script").internal(true).buildLiteral());
    }

    @Override
    public Bindings createBindings() {
        return new GraalJSBindings(contextConfig);
    }

    @Override
    public Object eval(Reader reader, ScriptContext ctxt) throws ScriptException {
        return eval(createSource(reader, ctxt), ctxt);
    }

    private static Source createSource(Reader reader, ScriptContext ctxt) throws ScriptException {
        try {
            return Source.newBuilder(ID, reader, getScriptName(ctxt)).build();
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object eval(String script, ScriptContext ctxt) throws ScriptException {
        return eval(createSource(script, ctxt), ctxt);
    }

    private static Source createSource(String script, ScriptContext ctxt) {
        return Source.newBuilder(ID, script, getScriptName(ctxt)).buildLiteral();
    }

    private static String getScriptName(final ScriptContext ctxt) {
        final Object val = ctxt.getAttribute(ScriptEngine.FILENAME);
        return (val != null) ? val.toString() : "<eval>";
    }

    private Object eval(Source source, ScriptContext scriptContext) throws ScriptException {
        GraalJSBindings engineBindings = getOrCreateGraalJSBindings(scriptContext);
        Context polyglotContext = engineBindings.getContext();
        ((DelegatingOutputStream) polyglotContext.getPolyglotBindings().getMember(OUT_SYMBOL).asProxyObject()).setWriter(scriptContext.getWriter());
        ((DelegatingOutputStream) polyglotContext.getPolyglotBindings().getMember(ERR_SYMBOL).asProxyObject()).setWriter(scriptContext.getErrorWriter());
        ((DelegatingInputStream) polyglotContext.getPolyglotBindings().getMember(IN_SYMBOL).asProxyObject()).setReader(scriptContext.getReader());
        try {
            if (!evalCalled) {
                jrunscriptInitWorkaround(source, polyglotContext);
            }
            importGlobalBindings(scriptContext, engineBindings);
            return polyglotContext.eval(source).as(Object.class);
        } catch (PolyglotException e) {
            throw new ScriptException(e);
        } finally {
            evalCalled = true;
        }
    }

    private static void importGlobalBindings(ScriptContext scriptContext, GraalJSBindings graalJSBindings) {
        Bindings globalBindings = scriptContext.getBindings(ScriptContext.GLOBAL_SCOPE);
        if (globalBindings != null && !globalBindings.isEmpty() && graalJSBindings != globalBindings) {
            graalJSBindings.getContext().getBindings(ID).getMember(SCRIPT_CONTEXT_GLOBAL_BINDINGS_IMPORT_FUNCTION_NAME).execute(globalBindings);
        }
    }

    private GraalJSBindings getOrCreateGraalJSBindings(ScriptContext scriptContext) {
        Bindings engineB = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        if (engineB instanceof GraalJSBindings) {
            return ((GraalJSBindings) engineB);
        } else {
            GraalJSBindings bindings = new GraalJSBindings(createContext(engineB));
            bindings.putAll(engineB);
            return bindings;
        }
    }

    private Context createContext(Bindings engineB) {
        Object ctx = engineB.get(POLYGLOT_CONTEXT);
        if (!(ctx instanceof Context)) {
            Context.Builder builder = contextConfig;
            for (MagicBindingsOptionSetter optionSetter : MAGIC_OPTION_SETTERS) {
                Object value = engineB.get(optionSetter.getOptionKey());
                if (value != null) {
                    builder = optionSetter.setOption(builder, value);
                    engineB.remove(optionSetter.getOptionKey());
                }
            }
            ctx = createDefaultContext(builder);
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
        GraalJSBindings engineBindings = getOrCreateGraalJSBindings(context);
        importGlobalBindings(context, engineBindings);
        Value thisValue = engineBindings.getContext().asValue(thiz);

        if (!thisValue.canInvokeMember(name)) {
            if (!thisValue.hasMember(name)) {
                throw noSuchMethod(name);
            } else {
                throw notCallable(name);
            }
        }
        try {
            return thisValue.invokeMember(name, args).as(Object.class);
        } catch (PolyglotException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        GraalJSBindings engineBindings = getOrCreateGraalJSBindings(context);
        importGlobalBindings(context, engineBindings);
        Value function = engineBindings.getContext().getBindings(ID).getMember(name);

        if (function == null) {
            throw noSuchMethod(name);
        } else if (!function.canExecute()) {
            throw notCallable(name);
        }
        try {
            return function.execute(args).as(Object.class);
        } catch (PolyglotException e) {
            throw new ScriptException(e);
        }
    }

    private static NoSuchMethodException noSuchMethod(String name) throws NoSuchMethodException {
        throw new NoSuchMethodException(name);
    }

    private static NoSuchMethodException notCallable(String name) throws NoSuchMethodException {
        throw new NoSuchMethodException(name + " is not a function");
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
        Source source = createSource(script, getContext());
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
        Source source = createSource(reader, getContext());
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
            if (writer != null) {
                writer.flush();
            }
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

    /**
     * Detects jrunscript "init.js" and installs a JSAdapter polyfill if needed.
     */
    private static void jrunscriptInitWorkaround(Source source, Context polyglotContext) {
        if (source.getName().equals(JRUNSCRIPT_INIT_NAME)) {
            String initCode = source.getCharacters().toString();
            if (initCode.contains("jrunscript") && initCode.contains("JSAdapter") && !polyglotContext.getBindings(ID).hasMember("JSAdapter")) {
                polyglotContext.eval(ID, JSADAPTER_POLYFILL);
            }
        }
    }

    private static final String JRUNSCRIPT_INIT_NAME = "<system-init>";
    private static final String JSADAPTER_POLYFILL = "this.JSAdapter || " +
                    "Object.defineProperty(this, \"JSAdapter\", {configurable:true, writable:true, enumerable: false, value: function(t) {\n" +
                    "    var target = {};\n" +
                    "    var handler = {\n" +
                    "        get: function(target, name) {return typeof t.__get__ == 'function' ? t.__get__.call(target, name) : undefined;},\n" +
                    "        has: function(target, name) {return typeof t.__has__ == 'function' ? t.__has__.call(target, name) : false;},\n" +
                    "        deleteProperty: function(target, name) {return typeof t.__delete__ == 'function' ? t.__delete__.call(target, name) : true;},\n" +
                    "        set: function(target, name, value) {return typeof t.__put__ == 'function' ? t.__put__.call(target, name, value) : undefined;},\n" +
                    "        ownKeys: function(target) {return typeof t.__getIds__ == 'function' ? t.__getIds__.call(target) : [];},\n" +
                    "    }\n" +
                    "    return new Proxy(target, handler);\n" +
                    "}});\n";

    private static IllegalArgumentException magicOptionValueErrorBool(String name, Object v) {
        return new IllegalArgumentException(String.format("failed to set graal-js option \"%s\": expected a boolean value, got \"%s\"", name, v));
    }
}
