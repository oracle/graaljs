/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.polyglot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;

public class InnerContextTest {
    @Test
    public void testInnerWithoutOuterJSContext() throws Exception {
        try (AutoCloseable languageScope = TestLanguage.withTestLanguage(new TestLanguage() {
            @Override
            protected CallTarget parse(ParsingRequest request) throws Exception {
                return Truffle.getRuntime().createCallTarget(new RootNode(languageInstance) {
                    @Override
                    public Object execute(VirtualFrame frame) {
                        return innerJS();
                    }

                    @TruffleBoundary
                    private Object innerJS() {
                        TruffleLanguage.Env env = TruffleLanguage.getCurrentContext(TestLanguage.class).getEnv();
                        TruffleContext innerContext = env.newContextBuilder().build();
                        Object prev = innerContext.enter(this);
                        try {
                            TruffleLanguage.Env innerEnv = TruffleLanguage.getCurrentContext(TestLanguage.class).getEnv();
                            CallTarget answer = innerEnv.parsePublic(com.oracle.truffle.api.source.Source.newBuilder(JavaScriptLanguage.ID, "42", "test.js").build());
                            return answer.call();
                        } finally {
                            innerContext.leave(this, prev);
                            innerContext.close();
                        }
                    }
                });
            }
        })) {
            try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                Value result = context.eval(Source.create(TestLanguage.ID, ""));
                assertEquals(result.asInt(), 42);
            }
            try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                context.initialize(JavaScriptLanguage.ID);
                context.eval(Source.create(TestLanguage.ID, ""));
            }
        }
    }

    @Test
    public void testInnerWithoutOuterJSContextWithInterop() throws Exception {
        try (AutoCloseable languageScope = TestLanguage.withTestLanguage(new TestLanguage() {
            @Override
            protected CallTarget parse(ParsingRequest request) throws Exception {
                return Truffle.getRuntime().createCallTarget(new RootNode(languageInstance) {
                    @Override
                    public Object execute(VirtualFrame frame) {
                        return innerJS();
                    }

                    @TruffleBoundary
                    private Object innerJS() {
                        TruffleLanguage.Env env = TruffleLanguage.getCurrentContext(TestLanguage.class).getEnv();
                        TruffleContext innerContext = env.newContextBuilder().build();
                        Object prev = innerContext.enter(this);
                        try {
                            TruffleLanguage.Env innerEnv = TruffleLanguage.getCurrentContext(TestLanguage.class).getEnv();
                            String jsCode = "b + s + i + l + f + d + c + str";
                            com.oracle.truffle.api.source.Source source = com.oracle.truffle.api.source.Source.newBuilder(
                                            JavaScriptLanguage.ID, jsCode, "test.js").build();
                            CallTarget answer = innerEnv.parsePublic(source, "b", "s", "i", "l", "f", "d", "c", "str");
                            return answer.call((byte) 1, (short) 2, 3, 4L, 0.1f, 1.5, ':', "test");
                        } finally {
                            innerContext.leave(this, prev);
                            innerContext.close();
                        }
                    }
                });
            }
        })) {
            try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                String result = context.eval(Source.create(TestLanguage.ID, "")).as(String.class);
                // (1 + 2 + 3 + 4 + 0.1 + 1.5) + ':' + 'test'
                assertEquals("11.600000001490116:test", result);
            }
            try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                context.initialize(JavaScriptLanguage.ID);
                context.eval(Source.create(TestLanguage.ID, ""));
            }
        }
    }

    @Rule public final ExpectedException exception = ExpectedException.none();

    @Test
    public void innerParseWithArgumentsHasLocalEnvironment() throws Exception {
        exception.expect(PolyglotException.class);
        exception.expectMessage("x is not defined");
        try (AutoCloseable languageScope = TestLanguage.withTestLanguage(new ProxyParsingLanguage("a"))) {
            try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                Value constEval1 = context.eval(Source.create(TestLanguage.ID, "var x = 3; x"));
                int x1 = constEval1.execute().asInt();
                assertEquals(3, x1);
                Value constEval2 = context.eval(Source.create(TestLanguage.ID, "x"));
                // Should throw exception when executing `x` as `x` should be undefined...
                constEval2.execute().asInt();
            }
        }
    }

    @Test
    public void innerParseWithArgumentsEnvironmentVarClash() throws Exception {
        try (AutoCloseable languageScope = TestLanguage.withTestLanguage(new ProxyParsingLanguage("a"))) {
            try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                // Redeclare 'a' as local variable and return the redeclaration.
                Value constEval1 = context.eval(Source.create(TestLanguage.ID, "if(a != 10) { throw 'Expected 10.'; }; var a = 3; a"));
                int x1 = constEval1.execute(10).asInt();
                assertEquals(3, x1);
                // And now return original argument (ensuring `var a = 3` is not declared globally).
                Value constEval2 = context.eval(Source.create(TestLanguage.ID, "a"));
                int x2 = constEval2.execute(15).asInt();
                assertEquals(15, x2);
            }
        }
    }

    @Test
    public void innerParseWithArgumentsEnvironmentLetClash() throws Exception {
        exception.expect(PolyglotException.class);
        exception.expectMessage("Variable \"a\" has already been declared");
        try (AutoCloseable languageScope = TestLanguage.withTestLanguage(new ProxyParsingLanguage("a"))) {
            try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                Value constEval1 = context.eval(Source.create(TestLanguage.ID, "let a = 3; a"));
                constEval1.execute(10).asInt();
            }
        }
    }

    @Test
    public void innerParseConsistencyWithAndWithoutArguments() throws Exception {
        String noParameters;
        String withParameters;
        try (AutoCloseable languageScope = TestLanguage.withTestLanguage(new ProxyParsingLanguage())) {
            try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                Value constEval = context.eval(Source.create(TestLanguage.ID, "'hello world'"));
                Value helloWorld = constEval.execute();
                noParameters = helloWorld.as(String.class);
            }
        }
        try (AutoCloseable languageScope = TestLanguage.withTestLanguage(new ProxyParsingLanguage("unused-parameter"))) {
            try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                Value constEval = context.eval(Source.create(TestLanguage.ID, "'hello world'"));
                Value helloWorld = constEval.execute("unused parameter value");
                withParameters = helloWorld.as(String.class);
            }
        }
        assertEquals(noParameters, withParameters);
    }

    @Test
    public void innerParseSimpleExpression() throws Exception {
        try (AutoCloseable languageScope = TestLanguage.withTestLanguage(new ProxyParsingLanguage("multiplier"))) {
            try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                Value mul = context.eval(Source.create(TestLanguage.ID, "6 * multiplier"));
                Value fourtyTwo = mul.execute(7);
                assertEquals(42, fourtyTwo.asInt());
            }
        }
    }

    @Test
    public void innerParseSingleStatement() throws Exception {
        try (AutoCloseable languageScope = TestLanguage.withTestLanguage(new ProxyParsingLanguage("a", "b"))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).out(out).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                Value mul = context.eval(Source.create(TestLanguage.ID,
                                "print(a + ' * ' + b + ' = ' + (a * b));" //
                ));
                Value undefined = mul.execute(6, 7);
                String output = out.toString("UTF-8");
                assertEquals("6 * 7 = 42\n", output);
                assertTrue(undefined.isNull());
            }
        }
    }

    @Test
    public void innerParseMultipleStatement() throws Exception {
        try (AutoCloseable languageScope = TestLanguage.withTestLanguage(new ProxyParsingLanguage("a", "b"))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).out(out).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                // @formatter:off
                Value mul = context.eval(Source.create(TestLanguage.ID,
                    "print(a + ' + ' + b + ' = ' + (a + b));" +
                    "print(a + ' * ' + b + ' = ' + (a * b));"
                ));
                // @formatter:on
                Value undefined = mul.execute(6, 7);
                String output = out.toString("UTF-8");
                // @formatter:off
                assertEquals(
                    "6 + 7 = 13\n" +
                    "6 * 7 = 42\n",
                    output
                );
                // @formatter:on
                assertTrue(undefined.isNull());
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class ExecutableObject implements TruffleObject {
        private final CallTarget target;

        ExecutableObject(CallTarget target) {
            this.target = target;
        }

        @ExportMessage
        static boolean isExecutable(ExecutableObject obj) {
            return obj.target != null;
        }

        @ExportMessage
        static Object execute(ExecutableObject obj, Object[] args) {
            Object res = obj.target.call(args);
            return res == null ? new ExecutableObject(null) : res;
        }

        @ExportMessage
        static boolean isNull(ExecutableObject obj) {
            return obj.target == null;
        }
    }

    private static class ProxyParsingLanguage extends TestLanguage {
        private final String[] argumentNames;

        ProxyParsingLanguage(String... argumentNames) {
            this.argumentNames = argumentNames;
        }

        @Override
        protected CallTarget parse(TruffleLanguage.ParsingRequest request) throws Exception {
            final String jsCode = request.getSource().getCharacters().toString();
            class ParseJsRootNode extends RootNode {
                ParseJsRootNode(TruffleLanguage<?> language) {
                    super(language);
                }

                @Override
                public Object execute(VirtualFrame frame) {
                    CompilerDirectives.transferToInterpreter();
                    TestLanguage.LanguageContext ctx = TruffleLanguage.getCurrentContext(TestLanguage.class);
                    TruffleLanguage.Env e = ctx.env;
                    com.oracle.truffle.api.source.Source src = com.oracle.truffle.api.source.Source.newBuilder("js", jsCode, "jscode.js").build();
                    CallTarget call = e.parseInternal(src, argumentNames);
                    return new ExecutableObject(call);
                }
            }
            return Truffle.getRuntime().createCallTarget(new ParseJsRootNode(languageInstance));
        }
    }
}
