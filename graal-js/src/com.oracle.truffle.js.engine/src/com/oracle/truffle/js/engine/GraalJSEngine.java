/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.engine;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.parser.GraalJSParserOptions;
import com.oracle.truffle.js.parser.JSEngine;
import com.oracle.truffle.js.parser.JSParser;
import com.oracle.truffle.js.parser.env.Environment;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.interop.JavaAccess;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Note that this implementation is not thread-safe.
 */
public final class GraalJSEngine extends AbstractScriptEngine implements Compilable, Invocable {
    private static final String ENGINE_PROPERTY = "__engine";
    private static final int BUFSIZE = 1024;

    private static final long creatorThreadId = (JSTruffleOptions.SingleThreaded) ? Thread.currentThread().getId() : Long.MIN_VALUE;

    private final GraalJSEngineFactory factory;

    GraalJSEngine(GraalJSEngineFactory factory) {
        this(factory, JSEngine.createJSContext());
    }

    GraalJSEngine(GraalJSEngineFactory factory, JSContext jsContext) {
        this.factory = factory;
        context.setBindings(new GraalJSBindings(jsContext, jsContext.getRealm()), ScriptContext.ENGINE_SCOPE);
    }

    @Override
    public GraalJSBindings createBindings() {
        JSContext newContext = JSEngine.createJSContext();
        return new GraalJSBindings(newContext, newContext.getRealm());
    }

    @SuppressWarnings("static-method")
    public GraalJSBindings createBindings(JSRealm realm) {
        return new GraalJSBindings(realm.getContext(), realm);
    }

    @Override
    public Object eval(Reader reader, ScriptContext ctxt) throws ScriptException {
        return eval(readSource(reader), ctxt);
    }

    @Override
    public Object eval(String script, ScriptContext ctxt) throws ScriptException {
        return eval(parse(script), ctxt);
    }

    public Object eval(File file) throws ScriptException {
        return eval(parse(file), context);
    }

    public Object eval(Source source) throws ScriptException {
        return eval(parse(source), context);
    }

    public Object eval(ScriptNode scriptNode) throws ScriptException {
        return eval(scriptNode, context);
    }

    public Object eval(ScriptNode scriptNode, ScriptContext ctxt) throws ScriptException {
        return evalScript(scriptNode, ctxt);
    }

    protected Object evalScript(ScriptNode scriptNode, ScriptContext ctxt) throws ScriptException {
        checkSingleThreadedMode();
        updateJSContext(ctxt);
        JSRealm realm = getJSRealm(ctxt);
        JSContext cx = realm.getContext();
        try {
            /**
             * In ECMA6 mode we need to emulate an event loop to support {@link Promise} when
             * running in non-Node.JS mode.
             */
            Object result;
            if (cx.getEcmaScriptVersion() >= 6) {
                result = scriptNode.run(realm);
                while (cx.processAllPendingPromiseJobs()) {
                    // Keep processing all jobs in the microtasks queue.
                }
                // No jobs in our queue. In non-Node.JS mode this means that we are done.
            } else {
                result = scriptNode.run(realm);
            }
            return JSRuntime.jsObjectToJavaObject(result);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    private static void checkSingleThreadedMode() {
        if (JSTruffleOptions.SingleThreaded && Thread.currentThread().getId() != creatorThreadId) {
            throw new Error("attempted to execute javascript function in a different thread");
        }
    }

    @Override
    public GraalJSEngineFactory getFactory() {
        return factory;
    }

    @Override
    public GraalJSCompiledScript compile(Reader reader) throws ScriptException {
        return compile(readSource(reader));
    }

    @Override
    public GraalJSCompiledScript compile(String script) throws ScriptException {
        return new GraalJSCompiledScript(this, parse(script));
    }

    public GraalJSCompiledScript compile(File file) throws ScriptException {
        return new GraalJSCompiledScript(this, parse(file));
    }

    public GraalJSCompiledScript compile(Source source) throws ScriptException {
        return new GraalJSCompiledScript(this, parse(source));
    }

    public static Object getProperty(Object obj, String name) {
        if (obj instanceof DynamicObject) {
            return JSObject.get((DynamicObject) obj, name);
        }
        return null;
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        if (thiz == null || !(JSObject.isDynamicObject(thiz))) {
            throw new IllegalArgumentException("thiz is not a JSObject.");
        }
        return invoke((DynamicObject) thiz, name, args);
    }

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        JSRealm realm = getJSContext().getRealm();
        return invoke(realm.getGlobalObject(), name, args);
    }

    public static Object invoke(DynamicObject thisObj, String name, Object... args) throws NoSuchMethodException {
        checkSingleThreadedMode();

        Object funObj = JSObject.get(thisObj, name);
        Object[] convertedArgs = null;
        if (args != null) {
            convertedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                convertedArgs[i] = JSRuntime.toJSNull(args[i]);
            }
        }
        if (funObj != Undefined.instance && JSObject.isDynamicObject(funObj)) {
            return JSRuntime.jsObjectToJavaObject(JSFunction.call((DynamicObject) funObj, thisObj, convertedArgs));
        } else {
            throw getFunctionNotFoundException(name);
        }
    }

    @Override
    public <T> T getInterface(Object thisObj, Class<T> clazz) {
        if (thisObj == null || !(JSObject.isDynamicObject(thisObj))) {
            throw new IllegalArgumentException("thiz is not a JSObject.");
        }
        return getInterfaceImpl((DynamicObject) thisObj, clazz);
    }

    @Override
    public <T> T getInterface(Class<T> clazz) {
        return getInterface(getJSRealm().getGlobalObject(), clazz);
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        checkSingleThreadedMode();
        super.setBindings(bindings, scope);
    }

    public JSContext getJSContext() {
        return getJSRealm().getContext();
    }

    public JSRealm getJSRealm() {
        return getJSRealm(getContext());
    }

    public void setArguments(String[] args) {
        put(Environment.ARGUMENTS_NAME, args);
    }

    public static void throwExitException() {
        throw new ControlFlowException();
    }

    public static boolean isExitException(Throwable t) {
        for (Throwable throwable = t; throwable != null; throwable = throwable.getCause()) {
            if (throwable instanceof ControlFlowException) {
                return true;
            }
        }
        return false;
    }

    private static JSRealm getJSRealm(ScriptContext ctxt) {
        Bindings engineB = ctxt.getBindings(ScriptContext.ENGINE_SCOPE);
        if (engineB instanceof GraalJSBindings) {
            return ((GraalJSBindings) engineB).getJSRealm();
        } else {
            return createJSRealm(engineB);
        }
    }

    private static JSRealm createJSRealm(Bindings engineB) {
        Object realm = engineB.get(GraalJSBindings.GRAALJS_CONTEXT);
        if (realm == null || !(realm instanceof JSRealm)) {
            realm = JSEngine.createJSContext().getRealm();
            engineB.put(GraalJSBindings.GRAALJS_CONTEXT, realm);
        }
        return (JSRealm) realm;
    }

    private JSParser getParser() {
        return (JSParser) getJSContext().getEvaluator();
    }

    private ScriptNode parse(String script) throws ScriptException {
        try {
            return getParser().parseScriptNode(getJSContext(), script);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    public ScriptNode parse(File file) throws ScriptException {
        try {
            return getParser().parseScriptNode(getJSContext(), file);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    public ScriptNode parse(Source source) throws ScriptException {
        try {
            return getParser().parseScriptNode(getJSContext(), source);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    private void updateJSContext(ScriptContext ctxt) {
        JSContext jsContext = getJSContext();
        JSRealm realm = jsContext.getRealm();

        // Set the global properties
        setProperty(jsContext, realm, ENGINE_PROPERTY, this);

        // Set the writers
        jsContext.setWriter(ctxt.getWriter(), null);
        jsContext.setErrorWriter(ctxt.getErrorWriter(), null);

        // The global object must have all properties that are part of the current bindings.
        // Note that properties declared in lower scopes override those declared in higher
        // scopes.
        // In case no GLOBAL_SCOPE bindings are defined AND the ENGINE_SCOPE bindings are of type
        // TruffleJSBindings, this property is already respected.
        if (ctxt.getBindings(ScriptContext.GLOBAL_SCOPE) == null || !(ctxt.getBindings(ScriptContext.ENGINE_SCOPE) instanceof GraalJSBindings)) {
            updateContextCreateBindings(ctxt, jsContext, realm);
        }
    }

    private static void updateContextCreateBindings(ScriptContext ctxt, JSContext jsContext, JSRealm realm) {
        // 1. create temporary TruffleJSBindings.
        Bindings tempBindings = new SimpleBindings();
        // 2. get all scopes accessible from the current ScriptContext
        Integer[] scopes = ctxt.getScopes().toArray(new Integer[0]);
        // 3. sort the scopes into ascending order
        Arrays.sort(scopes);

        Object argsValue = null;
        // 4. process all scopes in descending order
        for (int i = scopes.length - 1; i >= 0; i--) {
            Bindings otherBindings = ctxt.getBindings(scopes[i]);
            if (otherBindings != null) {
                tempBindings.putAll(otherBindings);

                // The 'arguments' property is not enumerable and not accessible otherwise.
                Object tmpArgsValue = otherBindings.get(Environment.ARGUMENTS_NAME);
                if (tmpArgsValue != null) {
                    argsValue = tmpArgsValue;
                }
            }
        }

        // 5. add all properties in the temporary bindings to the global object.
        // For doing so, we create an instance of GraalJSBindings sharing the current
        // jsContext. In this way, all property added to these bindings are added also to the
        // current global object.
        Bindings truffleBindings = new GraalJSBindings(jsContext, realm);
        truffleBindings.putAll(tempBindings);
        truffleBindings.put(Environment.ARGUMENTS_NAME, argsValue);
    }

    private static void setProperty(JSContext jsContext, JSRealm realm, String name, Object value) {
        checkSingleThreadedMode();

        DynamicObject globalObj = realm.getGlobalObject();
        if (JSObject.hasOwnProperty(globalObj, name)) {
            JSObject.set(globalObj, name, value, ((GraalJSParserOptions) jsContext.getParserOptions()).isStrict());
        } else {
            JSObjectUtil.putDataProperty(jsContext, globalObj, name, value, JSAttributes.notConfigurableNotEnumerableWritable());
        }
    }

    private static String readSource(Reader reader) throws ScriptException {
        try {
            final char[] arr = new char[BUFSIZE];
            final StringBuilder sb = new StringBuilder();

            try {
                int numChars;
                while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
                    sb.append(arr, 0, numChars);
                }
            } finally {
                reader.close();
            }

            return sb.toString();
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    @TruffleBoundary
    private static NoSuchMethodException getFunctionNotFoundException(String name) {
        return new NoSuchMethodException("Function " + name + " not found.");
    }

    private static <T> T getInterfaceImpl(DynamicObject thisObj, Class<T> clazz) {
        assert thisObj != null;
        checkSingleThreadedMode();

        if (clazz == null || !clazz.isInterface()) {
            throw new IllegalArgumentException("Interface class expected");
        }

        if (!JSTruffleOptions.NashornJavaInterop) {
            return null;
        }

        // perform security access check as early as possible
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (!Modifier.isPublic(clazz.getModifiers())) {
                throw new SecurityException("Cannot implement non-public interface: " + clazz.getName());
            }
            JavaAccess.checkPackageAccess(clazz);
        }

        if (!isInterfaceImplemented(clazz, thisObj)) {
            return null;
        }

        return clazz.cast(JavaClass.forClass(clazz).extend(null).newInstance(new Object[]{thisObj}));
    }

    private static boolean isInterfaceImplemented(final Class<?> iface, final DynamicObject sobj) {
        for (final Method method : iface.getMethods()) {
            // ignore methods of java.lang.Object class
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }

            // skip check for default methods - non-abstract interface methods
            if (!Modifier.isAbstract(method.getModifiers())) {
                continue;
            }

            Object obj = JSObject.get(sobj, method.getName());
            if (!(JSFunction.isJSFunction(obj))) {
                return false;
            }
        }
        return true;
    }

    public void setMainJSAgent(JSAgent agent) {
        getJSContext().setJSAgent(agent);
    }
}
