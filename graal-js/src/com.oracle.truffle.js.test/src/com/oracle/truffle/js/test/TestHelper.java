/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assume;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.function.JSFunctionExpressionNode;
import com.oracle.truffle.js.parser.JSParser;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.Null;

public class TestHelper implements AutoCloseable {

    private static final double DELTA = 1e-15;

    private final Context.Builder contextBuilder;
    private Context ctx;

    public TestHelper() {
        this(JSTest.newContextBuilder().option(JSContextOptions.DEBUG_BUILTIN_NAME, "true"));
    }

    public TestHelper(Context.Builder contextBuilder) {
        this.contextBuilder = contextBuilder;
    }

    @Override
    public void close() {
        if (ctx != null) {
            ctx.close();
            ctx = null;
        }
    }

    public Context getPolyglotContext() {
        if (ctx == null) {
            ctx = contextBuilder.build();
        }
        return ctx;
    }

    public JSContext getJSContext() {
        return getRealm().getContext();
    }

    public JSRealm getRealm() {
        return JavaScriptLanguage.getJSRealm(getPolyglotContext());
    }

    public DynamicObject getGlobalObject() {
        return getRealm().getGlobalObject();
    }

    public Object getBinding(String key) {
        return DynamicObjectLibrary.getUncached().getOrDefault(getGlobalObject(), key, null);
    }

    public void putBinding(String key, Object value) {
        DynamicObjectLibrary.getUncached().putIfPresent(getGlobalObject(), key, value);
    }

    public Object run(String sourceCode) {
        return toHostValue(runValue(sourceCode));
    }

    public double runDouble(String sourceCode) {
        return runValue(sourceCode).asDouble();
    }

    public boolean runBoolean(String sourceCode) {
        return runValue(sourceCode).asBoolean();
    }

    public Value runValue(String sourceCode) {
        Source source = Source.create(JavaScriptLanguage.ID, sourceCode);
        return getPolyglotContext().eval(source);
    }

    public void runVoid(String sourceCode) {
        runValue(sourceCode);
    }

    public boolean runExpectUndefined(String sourceCode) {
        Value result = runValue(sourceCode);

        Source checkUndefined = Source.create(JavaScriptLanguage.ID, "(function(arg) { return arg === undefined; });");
        Value fnCheckUndefined = getPolyglotContext().eval(checkUndefined);
        return fnCheckUndefined.execute(result).asBoolean();
    }

    public void runExpectSyntaxError(String sourceCode) {
        enterContext();
        try {
            getParser().parseScript(getJSContext(), sourceCode);
            fail("expected syntax error to be thrown");
        } catch (JSException e) {
            assertTrue(e.isSyntaxError());
        } finally {
            leaveContext();
        }
    }

    public Object runNoPolyglot(String source) {
        enterContext();
        Object result = null;
        try {
            ScriptNode program = getParser().parseScript(getJSContext(), source);
            result = runNoPolyglot(program);
        } finally {
            leaveContext();
        }
        return result;
    }

    public Object runNoPolyglot(ScriptNode scriptNode) {
        return transformResult(scriptNode.run(getRealm()));
    }

    private static Object transformResult(Object value) {
        if (value instanceof JSLazyString) {
            return value.toString();
        }
        return value;
    }

    public Value runRedirectOutput(String sourceCode, PrintStream writer, PrintStream errorWriter, boolean isInteractive, Map<String, Object> bindings) {
        Context specialCtx = JSTest.newContextBuilder().out(writer).err(errorWriter).build();
        Value jsBindings = specialCtx.getBindings(JavaScriptLanguage.ID);
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            jsBindings.putMember(entry.getKey(), entry.getValue());
        }
        Source source = Source.newBuilder(JavaScriptLanguage.ID, sourceCode, "TestCase").interactive(isInteractive).buildLiteral();
        return specialCtx.eval(source);
    }

    public String runToString(String source) {
        return runToString(source, false);
    }

    public String runToString(String source, boolean isInteractive) {
        return runToString(source, isInteractive, Collections.emptyMap());
    }

    public String runToString(String source, boolean isInteractive, Map<String, Object> bindings) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (PrintStream stream = new PrintStream(baos, false, "UTF-8")) {
                runRedirectOutput(source, stream, stream, isInteractive, bindings);
            }
            return baos.toString(StandardCharsets.UTF_8.displayName()).replaceAll("\r\n", "\n");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public Object runSilent(String sourceCode) {
        OutputStream out = new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                // ignore
            }
        };
        PrintStream stream = new PrintStream(out);
        return toHostValue(runRedirectOutput(sourceCode, stream, stream, false, Collections.emptyMap()));
    }

    public DynamicObject runJSArray(String source) {
        Object obj = runNoPolyglot(source);
        assert JSArray.isJSArray(obj);
        return (DynamicObject) obj;
    }

    public class ParsedFunction {
        private final JSFunctionData functionData;

        public ParsedFunction(JSFunctionData functionData) {
            this.functionData = functionData;
        }

        public Object call(Object[] args) {
            DynamicObject funObj = JSFunction.create(getRealm(), functionData);
            return JSFunction.call(funObj, Null.instance, args);
        }

        public RootNode getRootNode() {
            return ((RootCallTarget) functionData.getCallTarget()).getRootNode();
        }
    }

    public ParsedFunction parseFirstFunction(String source) {
        return new ParsedFunction(findFirstNodeInstance(parse(source).getRootNode(), JSFunctionExpressionNode.class).getFunctionData());
    }

    private static <T> T findFirstNodeInstance(Node root, Class<T> clazz) {
        if (clazz.isInstance(root)) {
            return clazz.cast(root);
        }
        for (Node child : root.getChildren()) {
            T node = findFirstNodeInstance(child, clazz);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    /**
     * Like assertEquals, but does not care what the actual type of the returned value is, as long
     * as it can be cast to a double.
     */
    public static void assertNumberEquals(Number expected, Object actual) {
        assertThat(actual, instanceOf(Number.class));
        assertEquals(expected.doubleValue(), ((Number) actual).doubleValue(), DELTA);
    }

    private JSParser getParser() {
        return (JSParser) getJSContext().getEvaluator();
    }

    public ScriptNode parse(String script) {
        return getParser().parseScript(getJSContext(), script);
    }

    public static Object toHostValue(final Value value) {
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isHostObject()) {
            return value.asHostObject();
        }
        if (value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            }
            if (value.fitsInDouble()) {
                return value.asDouble();
            }
            return new AssertionError("Unknown number value: " + value);
        }
        if (value.isString()) {
            return value.asString();
        }
        throw new AssertionError("Unknown value: " + value);
    }

    public void enterContext() {
        getPolyglotContext().enter();
    }

    public void leaveContext() {
        getPolyglotContext().leave();
    }

    public void assumeES6OrLater() {
        enterContext();
        Assume.assumeTrue(getJSContext().getEcmaScriptVersion() >= 6);
        leaveContext();
    }
}
