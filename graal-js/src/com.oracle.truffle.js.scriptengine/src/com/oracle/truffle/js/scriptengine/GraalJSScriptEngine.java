/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
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
import org.graalvm.polyglot.HostAccess.TargetMappingPrecedence;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.SourceSection;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
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
    private static final String JS_GLOBAL_ARGUMENTS_OPTION = "js.global-arguments";
    private static final String JS_CHARSET_OPTION = "js.charset";
    private static final String NASHORN_COMPATIBILITY_MODE_SYSTEM_PROPERTY = "polyglot.js.nashorn-compat";
    private static final String INSECURE_SCRIPTENGINE_ACCESS_SYSTEM_PROPERTY = "graaljs.insecure-scriptengine-access";
    static final String MAGIC_OPTION_PREFIX = "polyglot.js.";

    private static final HostAccess NASHORN_HOST_ACCESS = createNashornHostAccess();

    private static HostAccess createNashornHostAccess() {
        HostAccess.Builder b = HostAccess.newBuilder(HostAccess.ALL);
        // Last resort conversions similar to those in NashornBottomLinker.
        b.targetTypeMapping(Value.class, String.class, v -> !v.isNull(), v -> toString(v), TargetMappingPrecedence.LOWEST);
        b.targetTypeMapping(Number.class, Integer.class, n -> true, n -> n.intValue(), TargetMappingPrecedence.LOWEST);
        b.targetTypeMapping(Number.class, Double.class, n -> true, n -> n.doubleValue(), TargetMappingPrecedence.LOWEST);
        b.targetTypeMapping(Number.class, Long.class, n -> true, n -> n.longValue(), TargetMappingPrecedence.LOWEST);
        b.targetTypeMapping(Number.class, Boolean.class, n -> true, n -> toBoolean(n.doubleValue()), TargetMappingPrecedence.LOWEST);
        b.targetTypeMapping(String.class, Boolean.class, n -> true, n -> !n.isEmpty(), TargetMappingPrecedence.LOWEST);
        // Resembles the conversions in NashornPrimitiveLinker/JavaArgumentConverters
        b.targetTypeMapping(Double.class, Float.class, n -> true, n -> n.floatValue(), TargetMappingPrecedence.LOWEST);
        b.targetTypeMapping(Double.class, Short.class, n -> true, n -> n.shortValue(), TargetMappingPrecedence.LOWEST);
        b.targetTypeMapping(Double.class, Byte.class, n -> true, n -> n.byteValue(), TargetMappingPrecedence.LOWEST);
        return b.build();
    }

    // ToString() operation
    private static String toString(Value value) {
        return toPrimitive(value).toString();
    }

    // "Type(result) is not Object" heuristic for the purpose of ToPrimitive() conversion
    private static boolean isPrimitive(Value value) {
        return value.isString() || value.isNumber() || value.isBoolean() || value.isNull();
    }

    // ToPrimitive()/OrdinaryToPrimitive() operation
    private static Value toPrimitive(Value value) {
        if (value.hasMembers()) {
            for (String methodName : new String[]{"toString", "valueOf"}) {
                if (value.canInvokeMember(methodName)) {
                    Value maybePrimitive = value.invokeMember(methodName);
                    if (isPrimitive(maybePrimitive)) {
                        return maybePrimitive;
                    }
                }
            }
        }
        if (isPrimitive(value)) {
            return value;
        } else {
            throw new ClassCastException();
        }
    }

    private static boolean toBoolean(double d) {
        return d != 0.0 && !Double.isNaN(d);
    }

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

    private static final MagicBindingsOptionSetter[] MAGIC_OPTION_SETTERS = new MagicBindingsOptionSetter[]{new MagicBindingsOptionSetter() {

        @Override
        public String getOptionKey() {
            return MAGIC_OPTION_PREFIX + "nashorn-compat";
        }

        @Override
        public Builder setOption(Builder builder, Object value) {
            boolean val = toBoolean(this, value);
            if (val) {
                updateForNashornCompatibilityMode(builder);
            }
            return builder.option("js.nashorn-compat", String.valueOf(val));
        }
    }, new MagicBindingsOptionSetter() {

        @Override
        public String getOptionKey() {
            return MAGIC_OPTION_PREFIX + "allowAllAccess";
        }

        @Override
        public Builder setOption(Builder builder, Object value) {
            return builder.allowAllAccess(toBoolean(this, value));
        }
    }, new MagicBindingsOptionSetter() {

        @Override
        public String getOptionKey() {
            return MAGIC_OPTION_PREFIX + "allowHostAccess";
        }

        @Override
        public Builder setOption(Builder builder, Object value) {
            return builder.allowHostAccess(toBoolean(this, value) ? HostAccess.ALL : HostAccess.NONE);
        }
    }, new MagicBindingsOptionSetter() {

        @Override
        public String getOptionKey() {
            return MAGIC_OPTION_PREFIX + "allowNativeAccess";
        }

        @Override
        public Builder setOption(Builder builder, Object value) {
            return builder.allowNativeAccess(toBoolean(this, value));
        }
    }, new MagicBindingsOptionSetter() {

        @Override
        public String getOptionKey() {
            return MAGIC_OPTION_PREFIX + "allowCreateThread";
        }

        @Override
        public Builder setOption(Builder builder, Object value) {
            return builder.allowCreateThread(toBoolean(this, value));
        }
    }, new MagicBindingsOptionSetter() {

        @Override
        public String getOptionKey() {
            return MAGIC_OPTION_PREFIX + "allowIO";
        }

        @Override
        public Builder setOption(Builder builder, Object value) {
            boolean enabled = toBoolean(this, value);
            return builder.allowIO(enabled ? IOAccess.ALL : IOAccess.NONE);
        }
    }, new MagicBindingsOptionSetter() {

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
                    throw new IllegalArgumentException(String.format("failed to set graal-js option \"%s\": expected a boolean or Predicate<String> value, got \"%s\"", getOptionKey(), value));
                }
            }
        }
    }, new MagicBindingsOptionSetter() {

        @Override
        public String getOptionKey() {
            return MAGIC_OPTION_PREFIX + "allowHostClassLoading";
        }

        @Override
        public Builder setOption(Builder builder, Object value) {
            return builder.allowHostClassLoading(toBoolean(this, value));
        }
    }, new MagicBindingsOptionSetter() {

        @Override
        public String getOptionKey() {
            return MAGIC_OPTION_PREFIX + "ecmascript-version";
        }

        @Override
        public Builder setOption(Builder builder, Object value) {
            return builder.option("js.ecmascript-version", String.valueOf(value));
        }
    }, new MagicBindingsOptionSetter() {

        @Override
        public String getOptionKey() {
            return MAGIC_OPTION_PREFIX + "intl-402";
        }

        @Override
        public Builder setOption(Builder builder, Object value) {
            return builder.option("js.intl-402", String.valueOf(toBoolean(this, value)));
        }
    }};

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

    private boolean evalCalled;

    GraalJSScriptEngine(GraalJSEngineFactory factory) {
        this(factory, factory.getPolyglotEngine(), null);
    }

    GraalJSScriptEngine(GraalJSEngineFactory factory, Engine engine, Context.Builder contextConfig) {
        Engine engineToUse = engine;
        if (engineToUse == null) {
            engineToUse = Engine.newBuilder().allowExperimentalOptions(true).build();
        }
        Context.Builder contextConfigToUse = contextConfig;
        if (contextConfigToUse == null) {
            // default config
            contextConfigToUse = Context.newBuilder(ID).allowExperimentalOptions(true);
            contextConfigToUse.option(JS_SYNTAX_EXTENSIONS_OPTION, "true");
            contextConfigToUse.option(JS_LOAD_OPTION, "true");
            contextConfigToUse.option(JS_PRINT_OPTION, "true");
            contextConfigToUse.option(JS_GLOBAL_ARGUMENTS_OPTION, "true");
            // ScriptContext provides Reader/Writer while Context.Builder requires
            // InputStream/OutpuStream. We use DelegatingInput/OutputStream for this conversion. We
            // cannot use the default charset for that because it may not be able to represent all
            // the needed characters. So, we hard-code the usage of UTF-8 in
            // DelegatingInput/OutputStream => we have to tell the engine to use UTF-8 (not the
            // default charset) to read input/output.
            contextConfigToUse.option(JS_CHARSET_OPTION, "UTF-8");
            if (NASHORN_COMPATIBILITY_MODE) {
                updateForNashornCompatibilityMode(contextConfigToUse);
            } else if (Boolean.getBoolean(INSECURE_SCRIPTENGINE_ACCESS_SYSTEM_PROPERTY)) {
                updateForScriptEngineAccessibility(contextConfigToUse);
            }
        }
        this.factory = (factory == null) ? new GraalJSEngineFactory(engineToUse) : factory;
        this.contextConfig = contextConfigToUse.option(JS_SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT_OPTION, "true").engine(engineToUse);
        this.context.setBindings(new GraalJSBindings(this.contextConfig, this.context, this), ScriptContext.ENGINE_SCOPE);
    }

    private static void updateForNashornCompatibilityMode(Context.Builder builder) {
        builder.allowAllAccess(true);
        builder.allowHostAccess(NASHORN_HOST_ACCESS);
        builder.useSystemExit(true);
    }

    private static void updateForScriptEngineAccessibility(Context.Builder builder) {
        builder.allowHostAccess(HostAccess.ALL);
    }

    static Context createDefaultContext(Context.Builder builder, ScriptContext ctxt) {
        DelegatingInputStream in = new DelegatingInputStream();
        DelegatingOutputStream out = new DelegatingOutputStream();
        DelegatingOutputStream err = new DelegatingOutputStream();
        if (ctxt != null) {
            in.setReader(ctxt.getReader());
            out.setWriter(ctxt.getWriter());
            err.setWriter(ctxt.getErrorWriter());
        }
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
        return new GraalJSBindings(contextConfig, null, this);
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        if (scope == ScriptContext.ENGINE_SCOPE) {
            Bindings oldBindings = getBindings(scope);
            if (oldBindings instanceof GraalJSBindings) {
                ((GraalJSBindings) oldBindings).updateEngineScriptContext(null);
            }
        }
        super.setBindings(bindings, scope);
        if (scope == ScriptContext.ENGINE_SCOPE && (bindings instanceof GraalJSBindings)) {
            ((GraalJSBindings) bindings).updateEngineScriptContext(getContext());
        }
    }

    @Override
    public Object eval(Reader reader, ScriptContext ctxt) throws ScriptException {
        return eval(createSource(read(reader), ctxt), ctxt);
    }

    static String read(Reader reader) throws ScriptException {
        final StringBuilder builder = new StringBuilder();
        final char[] buffer = new char[1024];
        try {
            try {
                while (true) {
                    final int count = reader.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    builder.append(buffer, 0, count);
                }
            } finally {
                reader.close();
            }
            return builder.toString();
        } catch (IOException ioex) {
            throw new ScriptException(ioex);
        }
    }

    @Override
    public Object eval(String script, ScriptContext ctxt) throws ScriptException {
        return eval(createSource(script, ctxt), ctxt);
    }

    private static Source createSource(String script, ScriptContext ctxt) throws ScriptException {
        final Object val = ctxt.getAttribute(ScriptEngine.FILENAME);
        if (val == null) {
            return Source.newBuilder(ID, script, "<eval>").buildLiteral();
        } else {
            try {
                return Source.newBuilder(ID, new File(val.toString())).content(script).build();
            } catch (IOException ioex) {
                throw new ScriptException(ioex);
            }
        }
    }

    private static void updateDelegatingIOStreams(Context polyglotContext, ScriptContext scriptContext) {
        Value polyglotBindings = polyglotContext.getPolyglotBindings();
        ((DelegatingOutputStream) polyglotBindings.getMember(OUT_SYMBOL).asProxyObject()).setWriter(scriptContext.getWriter());
        ((DelegatingOutputStream) polyglotBindings.getMember(ERR_SYMBOL).asProxyObject()).setWriter(scriptContext.getErrorWriter());
        ((DelegatingInputStream) polyglotBindings.getMember(IN_SYMBOL).asProxyObject()).setReader(scriptContext.getReader());
    }

    private Object eval(Source source, ScriptContext scriptContext) throws ScriptException {
        GraalJSBindings engineBindings = getOrCreateGraalJSBindings(scriptContext);
        Context polyglotContext = engineBindings.getContext();
        updateDelegatingIOStreams(polyglotContext, scriptContext);
        try {
            if (!evalCalled) {
                jrunscriptInitWorkaround(source, polyglotContext);
            }
            engineBindings.importGlobalBindings(scriptContext);
            return polyglotContext.eval(source).as(Object.class);
        } catch (PolyglotException e) {
            throw toScriptException(e);
        } finally {
            evalCalled = true;
        }
    }

    private static ScriptException toScriptException(PolyglotException ex) {
        ScriptException sex;
        if (ex.isHostException()) {
            Throwable hostException = ex.asHostException();
            // ScriptException (unlike almost any other exception) does not
            // accept Throwable cause (requires the cause to be Exception)
            Exception cause;
            if (hostException instanceof Exception) {
                cause = (Exception) hostException;
            } else {
                cause = new Exception(hostException);
            }
            // Make the host exception accessible through the cause chain
            sex = new ScriptException(cause);
            // Re-use the stack-trace of PolyglotException (with guest-language stack-frames)
            sex.setStackTrace(ex.getStackTrace());
        } else {
            SourceSection sourceSection = ex.getSourceLocation();
            if (sourceSection != null && sourceSection.isAvailable()) {
                Source source = sourceSection.getSource();
                String fileName = source.getPath();
                if (fileName == null) {
                    fileName = source.getName();
                }
                int lineNo = sourceSection.getStartLine();
                int columnNo = sourceSection.getStartColumn();
                sex = new ScriptException(ex.getMessage(), fileName, lineNo, columnNo);
                sex.initCause(ex);
            } else {
                sex = new ScriptException(ex);
            }
        }
        return sex;
    }

    private GraalJSBindings getOrCreateGraalJSBindings(ScriptContext scriptContext) {
        Bindings engineB = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        if (engineB instanceof GraalJSBindings) {
            return ((GraalJSBindings) engineB);
        } else {
            GraalJSBindings bindings = new GraalJSBindings(createContext(engineB), scriptContext, this);
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
            ctx = createDefaultContext(builder, context);
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
        engineBindings.importGlobalBindings(context);
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
            throw toScriptException(e);
        }
    }

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        GraalJSBindings engineBindings = getOrCreateGraalJSBindings(context);
        engineBindings.importGlobalBindings(context);
        Value function = engineBindings.getContext().getBindings(ID).getMember(name);

        if (function == null) {
            throw noSuchMethod(name);
        } else if (!function.canExecute()) {
            throw notCallable(name);
        }
        try {
            return function.execute(args).as(Object.class);
        } catch (PolyglotException e) {
            throw toScriptException(e);
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
        checkInterface(clasz);
        return getInterfaceInner(evalInternal(getPolyglotContext(), "this"), clasz);
    }

    @Override
    public <T> T getInterface(Object thiz, Class<T> clasz) {
        if (thiz == null) {
            throw new IllegalArgumentException("this cannot be null");
        }
        checkInterface(clasz);
        Value thisValue = getPolyglotContext().asValue(thiz);
        checkThis(thisValue);
        return getInterfaceInner(thisValue, clasz);
    }

    private static void checkInterface(Class<?> clasz) {
        if (clasz == null || !clasz.isInterface()) {
            throw new IllegalArgumentException("interface Class expected in getInterface");
        }
    }

    private static void checkThis(Value thiz) {
        if (thiz.isHostObject() || !thiz.hasMembers()) {
            throw new IllegalArgumentException("getInterface cannot be called on non-script object");
        }
    }

    private static <T> T getInterfaceInner(Value thiz, Class<T> iface) {
        if (!isInterfaceImplemented(iface, thiz)) {
            return null;
        }
        return thiz.as(iface);
    }

    @Override
    public CompiledScript compile(String script) throws ScriptException {
        Source source = createSource(script, getContext());
        return compile(source);
    }

    @Override
    public CompiledScript compile(Reader reader) throws ScriptException {
        Source source = createSource(read(reader), getContext());
        return compile(source);
    }

    private CompiledScript compile(Source source) throws ScriptException {
        checkSyntax(source);
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

    private void checkSyntax(Source source) throws ScriptException {
        try {
            getPolyglotContext().parse(source);
        } catch (PolyglotException pex) {
            throw toScriptException(pex);
        }
    }

    private static class DelegatingInputStream extends InputStream implements Proxy {

        private Reader reader;
        private CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        private CharBuffer charBuffer = CharBuffer.allocate(2);
        private ByteBuffer byteBuffer = ByteBuffer.allocate((int) encoder.maxBytesPerChar() * 2);

        DelegatingInputStream() {
            byteBuffer.flip();
        }

        @Override
        public int read() throws IOException {
            if (reader != null) {
                while (!byteBuffer.hasRemaining()) {
                    int c = reader.read();
                    if (c == -1) {
                        return -1;
                    }
                    byteBuffer.clear();
                    charBuffer.put((char) c);
                    charBuffer.flip();
                    encoder.encode(charBuffer, byteBuffer, false);
                    charBuffer.compact();
                    byteBuffer.flip();
                }
                return byteBuffer.get();
            }
            return 0;
        }

        void setReader(Reader reader) {
            this.reader = reader;
        }

    }

    private static final class DelegatingOutputStream extends OutputStream implements Proxy {

        private Writer writer;
        private CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        private ByteBuffer byteBuffer = ByteBuffer.allocate((int) StandardCharsets.UTF_8.newEncoder().maxBytesPerChar() * 2);
        private CharBuffer charBuffer = CharBuffer.allocate(byteBuffer.capacity() * (int) decoder.maxCharsPerByte());

        @Override
        public void write(int b) throws IOException {
            if (writer != null) {
                byteBuffer.put((byte) b);
                byteBuffer.flip();
                decoder.decode(byteBuffer, charBuffer, false);
                byteBuffer.compact();
                charBuffer.flip();
                while (charBuffer.hasRemaining()) {
                    char c = charBuffer.get();
                    writer.write(c);
                }
                charBuffer.clear();
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
        return new GraalJSScriptEngine(null, engine, newContextConfig);
    }

    private static boolean isInterfaceImplemented(final Class<?> iface, final Value obj) {
        for (final Method method : iface.getMethods()) {
            // ignore methods of java.lang.Object class
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }

            // skip check for default methods - non-abstract, interface methods
            if (!Modifier.isAbstract(method.getModifiers())) {
                continue;
            }

            if (!obj.canInvokeMember(method.getName())) {
                return false;
            }
        }
        return true;
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
