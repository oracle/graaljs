/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.debug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.api.debug.Breakpoint;
import com.oracle.truffle.api.debug.DebugScope;
import com.oracle.truffle.api.debug.DebugStackFrame;
import com.oracle.truffle.api.debug.DebugValue;
import com.oracle.truffle.api.debug.DebuggerSession;
import com.oracle.truffle.api.debug.SourceElement;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.tck.DebuggerTester;

@RunWith(Parameterized.class)
public class TestScope {
    private static final String IGNORE_VALUE = null;

    private DebuggerTester tester;

    @Parameters(name = "{0}")
    public static List<Boolean> data() {
        return Arrays.asList(Boolean.TRUE, Boolean.FALSE);
    }

    @Parameter(value = 0) public boolean closureOpt;

    @Before
    public void before() {
        Context.Builder contextBuilder = JSTest.newContextBuilder();
        contextBuilder.allowHostAccess(HostAccess.ALL);
        contextBuilder.allowHostClassLookup(s -> true);
        contextBuilder.option(JSContextOptions.SCOPE_OPTIMIZATION_NAME, Boolean.toString(closureOpt));
        tester = new DebuggerTester(contextBuilder);
    }

    @After
    public void dispose() {
        tester.close();
    }

    @Test
    public void testGlobalScope() {
        Source globalCode = Source.newBuilder("js", "var v1 = 10;\n" +
                        "let v2 = 1234;\n" +
                        "let v3 = {};\n" +
                        "v3.a = \"a\";\n" +
                        "v1;\n", "globalCode.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.suspendNextExecution();
            tester.startEval(globalCode);

            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 1, "var v1 = 10", new String[]{});
                checkGlobalScope(event, new String[]{}, new String[]{"v1", "undefined"});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 2, "let v2 = 1234", new String[]{});
                checkGlobalScope(event, new String[]{}, new String[]{"v1", "10"});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 3, "let v3 = {}", new String[]{});
                checkGlobalScope(event, new String[]{"v2", "1234"}, new String[]{"v1", "10"});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 4, "v3.a = \"a\"", new String[]{});
                checkGlobalScope(event, new String[]{"v2", "1234", "v3", "{}"}, new String[]{"v1", "10"});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 5, "v1", new String[]{});
                checkGlobalScope(event, new String[]{"v2", "1234", "v3", "{a: \"a\"}"}, new String[]{"v1", "10"});
                event.prepareStepOver(1);
            });
        }
        assertEquals("10", tester.expectDone());
    }

    @Test
    public void testFunctionScope() {
        Source function = Source.newBuilder("js", "function main(a1, a2) {\n" +
                        "  var v1 = a1 + a2;\n" +
                        "  let v2 = 1234;\n" +
                        "  let v3 = {};\n" +
                        "  v3.a = \"a\";\n" +
                        "  return v1;\n" +
                        "}\n" +
                        "main(10, 20);\n", "function.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.suspendNextExecution();
            tester.startEval(function);

            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 8, "main(10, 20)", new String[]{});
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 2, "var v1 = a1 + a2", new String[]{"a1", "10", "a2", "20", "v1", "undefined"});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 3, "let v2 = 1234", new String[]{"a1", "10", "a2", "20", "v1", "30"});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 4, "let v3 = {}", new String[]{"a1", "10", "a2", "20", "v1", "30", "v2", "1234"});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 5, "v3.a = \"a\"", new String[]{"a1", "10", "a2", "20", "v1", "30", "v2", "1234", "v3", "{}"});
                event.prepareContinue();
            });
        }
        assertEquals("30", tester.expectDone());
    }

    @Ignore
    @Test
    public void testFunctionArguments() {
        String function = "function main(a1, a2) {\n" +
                        "  var v1 = a1 + a2;\n" +
                        "  let v2 = 1234;\n" +
                        "  let v3 = {};\n" +
                        "  v3.a = \"a\";\n" +
                        "  return v1;\n" +
                        "}";
        Source source = Source.newBuilder("js", function + "\n" +
                        "main(10, 20);\n", "function.js").buildLiteral();
        try (DebuggerSession session = tester.startSession(SourceElement.ROOT)) {
            session.suspendNextExecution();
            tester.startEval(source);

            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 1, source.getCharacters().toString(), new String[]{});
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                // We have the function arguments:
                checkScope(event, "main", 1, function, new String[]{"a1", "10", "a2", "20"});
                event.prepareContinue();
            });
        }
        assertEquals("30", tester.expectDone());
    }

    @Test
    public void testBlockScope() {
        Source function = Source.newBuilder("js", "function main(a1, a2) {\n" +
                        "  var v1 = a1 + a2;\n" +
                        "  let v2 = 1234;\n" +
                        "  {\n" +
                        "    let v3 = {};\n" +
                        "    let v2 = 11.11;\n" +
                        "    v3.a = \"a\";\n" +
                        "  }\n" +
                        "  return v1;\n" +
                        "}\n" +
                        "main(10, 20);\n", "function.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.suspendNextExecution();
            tester.startEval(function);

            tester.expectSuspended((SuspendedEvent event) -> {
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 2, "var v1 = a1 + a2", new String[]{"a1", "10", "a2", "20", "v1", "undefined"});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 3, "let v2 = 1234", new String[]{"a1", "10", "a2", "20", "v1", "30"});
                event.prepareStepOver(1);
            });
            // In block:
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 5, "let v3 = {}", new String[]{}, new String[]{"a1", "10", "a2", "20", "v1", "30", "v2", "1234"});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 6, "let v2 = 11.11", new String[]{"v3", "{}"}, new String[]{"a1", "10", "a2", "20", "v1", "30", "v2", "1234"});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 7, "v3.a = \"a\"", new String[]{"v3", "{}", "v2", "11.11"}, new String[]{"a1", "10", "a2", "20", "v1", "30", "v2", "1234"});
                event.prepareStepOver(1);
            });
            // Out of block:
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 9, "return v1;", new String[]{"a1", "10", "a2", "20", "v1", "30", "v2", "1234"});
                event.prepareContinue();
            });
        }
        assertEquals("30", tester.expectDone());
    }

    @Test
    public void testSimpleOneLineFunction() {
        Source function = Source.newBuilder("js", "function foo() {var i = 0; i++; i++; return i;} foo()", "function.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.suspendNextExecution();
            tester.startEval(function);

            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 1, "foo()", new String[]{});
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "foo", 1, "var i = 0", new String[]{"i", "undefined"});
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "foo", 1, "i++", new String[]{"i", "0"});
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "foo", 1, "i++", new String[]{"i", "1"});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "foo", 1, "return i;", new String[]{"i", "2"});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 1, "foo()", new String[]{});
                event.prepareStepInto(1);
            });
        }
        tester.expectDone();
    }

    @Test
    public void testLoopBlocks() {
        int n = 3;
        String ns = Integer.toString(n);
        Source function = Source.newBuilder("js", "function loops(n) {\n" +
                        "  let s = 0;\n" +
                        "  for (let i = 0; i < n; i++) {\n" +
                        "    for (let j = 0; j < n; j++) {\n" +
                        "      let k = i*j;\n" +
                        "      s += i + j + k;\n" +
                        "    }\n" +
                        "  }\n" +
                        "  return s;\n" +
                        "}\n" +
                        "loops(" + n + ");\n", "function.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.suspendNextExecution();
            tester.startEval(function);

            tester.expectSuspended((SuspendedEvent event) -> {
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "loops", 2, "let s = 0", new String[]{"n", ns});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "loops", 3, "let i = 0", new String[]{}, new String[]{"n", ns, "s", "0"});
                event.prepareStepOver(1);
            });
            int s = 0;
            for (int i = 0; i < n; i++) {
                String is = Integer.toString(i);
                String ss = Integer.toString(s);
                tester.expectSuspended((SuspendedEvent event) -> {
                    checkScope(event, "loops", 3, "i < n", new String[]{"i", is}, new String[]{"n", ns, "s", ss});
                    event.prepareStepOver(1);
                });
                tester.expectSuspended((SuspendedEvent event) -> {
                    checkScope(event, "loops", 4, "let j = 0", new String[]{}, new String[]{"i", is}, new String[]{"n", ns, "s", ss});
                    event.prepareStepOver(1);
                });
                for (int j = 0; j < n; j++) {
                    String js = Integer.toString(j);
                    String ss2 = Integer.toString(s);
                    tester.expectSuspended((SuspendedEvent event) -> {
                        checkScope(event, "loops", 4, "j < n", new String[]{"j", js}, new String[]{"i", is}, new String[]{"n", ns, "s", ss2});
                        event.prepareStepOver(1);
                    });
                    tester.expectSuspended((SuspendedEvent event) -> {
                        checkScope(event, "loops", 5, "let k = i*j", new String[]{}, new String[]{"j", js}, new String[]{"i", is}, new String[]{"n", ns, "s", ss2});
                        event.prepareStepOver(1);
                    });
                    String ks = Integer.toString(i * j);
                    tester.expectSuspended((SuspendedEvent event) -> {
                        checkScope(event, "loops", 6, "s += i + j + k", new String[]{"k", ks}, new String[]{"j", js}, new String[]{"i", is}, new String[]{"n", ns, "s", ss2});
                        event.prepareStepOver(1);
                    });
                    s += i + j + i * j;
                    String ss3 = Integer.toString(s);
                    tester.expectSuspended((SuspendedEvent event) -> {
                        checkScope(event, "loops", 4, "j++", new String[]{"j", js}, new String[]{"i", is}, new String[]{"n", ns, "s", ss3});
                        event.prepareStepOver(1);
                    });
                }
                String ss4 = Integer.toString(s);
                tester.expectSuspended((SuspendedEvent event) -> {
                    checkScope(event, "loops", 4, "j < n", new String[]{"j", ns}, new String[]{"i", is}, new String[]{"n", ns, "s", ss4});
                    event.prepareStepOver(1);
                });
                tester.expectSuspended((SuspendedEvent event) -> {
                    checkScope(event, "loops", 3, "i++", new String[]{"i", is}, new String[]{"n", ns, "s", ss4});
                    event.prepareStepOver(1);
                });
            }
            String ss5 = Integer.toString(s);
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "loops", 3, "i < n", new String[]{"i", ns}, new String[]{"n", ns, "s", ss5});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "loops", 9, "return s;", new String[]{"n", ns, "s", ss5});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 11, "loops(" + n + ")", new String[]{});
                event.prepareContinue();
            });

            assertEquals(ss5, tester.expectDone());
        }
    }

    private static void checkFunctionScopeName(DebugStackFrame topStackFrame, String expectedFunctionName) {
        DebugScope functionTopScope = topStackFrame.getScope();
        while (functionTopScope.getParent() != null) {
            functionTopScope = functionTopScope.getParent();
        }
        Assert.assertEquals(expectedFunctionName, functionTopScope.getName());
    }

    @Test
    public void testFunctionCallFromBlock() {
        Source function = Source.newBuilder("js", "" +
                        "const n = 10;\n" +
                        "\n" +
                        "function factorial(n) {\n" +
                        "  let f = 1;\n" +
                        "  for (let i = 2; i <= n; i++) {\n" +
                        "    f *= (x => x)(i);\n" + // L6
                        "  }\n" +
                        "  return f;\n" +
                        "}\n" +
                        "\n" +
                        "var fac;\n" +
                        "for (let i = 0; i <= n; i++) {\n" +
                        "  fac = factorial(i);\n" + // L13
                        "}", "function.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(function)).lineIs(13).oneShot().build());
            session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(function)).lineIs(6).oneShot().build());
            tester.startEval(function);

            // Before invocation of factorial(i):
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 13, "fac = factorial(i)", new String[]{"i", "0"}, new String[]{});
                checkGlobalScope(event, new String[]{"n", "10"}, new String[]{"fac", "undefined"});
                event.prepareStepInto(1);
            });
            // In factorial(i):
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "factorial", 4, "let f = 1", new String[]{"n", "0"});
                checkFunctionScopeName(event.getTopStackFrame(), "factorial");
                // Look into the invoke frame:
                Iterator<DebugStackFrame> framesIterator = event.getStackFrames().iterator();
                assertEquals(event.getTopStackFrame(), framesIterator.next()); // skip the top one
                DebugStackFrame callerFrame = framesIterator.next();
                checkScopes(callerFrame.getScope(), new String[]{"i", "0"}, new String[]{});
                event.prepareContinue();
            });
            // In factorial(i), for:
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "factorial", 6, "f *= (x => x)(i)", new String[]{"i", "2"}, new String[]{"n", "2", "f", "1"});
                checkFunctionScopeName(event.getTopStackFrame(), "factorial");

                Iterator<DebugStackFrame> framesIterator = event.getStackFrames().iterator();
                assertEquals(event.getTopStackFrame(), framesIterator.next()); // skip the top one
                DebugStackFrame callerFrame = framesIterator.next();
                checkScopes(callerFrame.getScope(), new String[]{"i", "2"}, new String[]{});

                checkGlobalScope(event, new String[]{"n", "10"}, new String[]{"fac", "1"});
                event.prepareStepInto(1);
            });
            // In factorial(i), for, closure:
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":=>", 6, "x", new String[]{"x", "2"});

                Iterator<DebugStackFrame> framesIterator = event.getStackFrames().iterator();
                assertEquals(event.getTopStackFrame(), framesIterator.next()); // skip the top one
                DebugStackFrame callerFrame = framesIterator.next();
                checkScopes(callerFrame.getScope(), new String[]{"i", "2"}, new String[]{"n", "2", "f", "1"});
                callerFrame = framesIterator.next();
                checkScopes(callerFrame.getScope(), new String[]{"i", "2"}, new String[]{});

                checkGlobalScope(event, new String[]{"n", "10"}, new String[]{"fac", "1"});
                event.prepareContinue();
            });
        }
        assertEquals("3628800", tester.expectDone());
    }

    @Test
    public void testGenerator() {
        Source function = Source.newBuilder("js", "" +
                        "const n = 10;\n" +
                        "\n" +
                        "function* factorial(n) {\n" +
                        "  let f = 1;\n" +
                        "  for (let i = 1; i <= n; i++) {\n" +
                        "    f *= (x => x)(i);\n" + // L6
                        "    yield f;\n" +
                        "  }\n" +
                        "  return f;\n" +
                        "}\n" +
                        "\n" +
                        "var fac;\n" +
                        "for (let i of factorial(n)) {\n" + // L13
                        "  fac = i;\n" +
                        "}", "generator.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(function)).lineIs(13).oneShot().build());
            session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(function)).lineIs(6).build());
            tester.startEval(function);

            // Before invocation of factorial():
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 13, "factorial(n)", new String[]{}, new String[]{});
                checkGlobalScope(event, new String[]{"n", "10"}, new String[]{"fac", "undefined"});
                event.prepareContinue();
            });
            // In factorial(), for:
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "factorial", 6, "f *= (x => x)(i)", new String[]{"i", "1"}, new String[]{"n", "10", "f", "1"});
                checkFunctionScopeName(event.getTopStackFrame(), "factorial");

                Iterator<DebugStackFrame> framesIterator = event.getStackFrames().iterator();
                assertEquals(event.getTopStackFrame(), framesIterator.next()); // skip the top one
                DebugStackFrame callerFrame = framesIterator.next();
                checkScopes(callerFrame.getScope(), new String[]{});

                checkGlobalScope(event, new String[]{"n", "10"}, new String[]{"fac", "undefined"});
                event.prepareContinue();
            });
            // In factorial(), for, closure:
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":=>", 6, "x", new String[]{"x", "1"});

                Iterator<DebugStackFrame> framesIterator = event.getStackFrames().iterator();
                assertEquals(event.getTopStackFrame(), framesIterator.next()); // skip the top one
                DebugStackFrame callerFrame = framesIterator.next();
                checkScopes(callerFrame.getScope(), new String[]{"i", "1"}, new String[]{"n", "10", "f", "1"});

                checkGlobalScope(event, new String[]{"n", "10"}, new String[]{"fac", "undefined"});
                event.prepareContinue();
            });
            // In factorial(), for:
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "factorial", 6, "f *= (x => x)(i)", new String[]{"i", "2"}, new String[]{"n", "10", "f", "1"});
                checkFunctionScopeName(event.getTopStackFrame(), "factorial");

                Iterator<DebugStackFrame> framesIterator = event.getStackFrames().iterator();
                assertEquals(event.getTopStackFrame(), framesIterator.next()); // skip the top one
                DebugStackFrame callerFrame = framesIterator.next();
                checkScopes(callerFrame.getScope(), new String[]{});

                checkGlobalScope(event, new String[]{"n", "10"}, new String[]{"fac", "1"});
                event.prepareContinue();
            });
            // In factorial(), for, closure:
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":=>", 6, "x", new String[]{"x", "2"});

                Iterator<DebugStackFrame> framesIterator = event.getStackFrames().iterator();
                assertEquals(event.getTopStackFrame(), framesIterator.next()); // skip the top one
                DebugStackFrame callerFrame = framesIterator.next();
                checkScopes(callerFrame.getScope(), new String[]{"i", "2"}, new String[]{"n", "10", "f", "1"});

                checkGlobalScope(event, new String[]{"n", "10"}, new String[]{"fac", "1"});
                event.prepareContinue();
            });
            // In factorial(), for:
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "factorial", 6, "f *= (x => x)(i)", new String[]{"i", "3"}, new String[]{"n", "10", "f", "2"});
                checkFunctionScopeName(event.getTopStackFrame(), "factorial");

                Iterator<DebugStackFrame> framesIterator = event.getStackFrames().iterator();
                assertEquals(event.getTopStackFrame(), framesIterator.next()); // skip the top one
                DebugStackFrame callerFrame = framesIterator.next();
                checkScopes(callerFrame.getScope(), new String[]{});

                checkGlobalScope(event, new String[]{"n", "10"}, new String[]{"fac", "2"});
                event.prepareContinue();
            });
            // In factorial(), for, closure:
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":=>", 6, "x", new String[]{"x", "3"});

                Iterator<DebugStackFrame> framesIterator = event.getStackFrames().iterator();
                assertEquals(event.getTopStackFrame(), framesIterator.next()); // skip the top one
                DebugStackFrame callerFrame = framesIterator.next();
                checkScopes(callerFrame.getScope(), new String[]{"i", "3"}, new String[]{"n", "10", "f", "2"});

                checkGlobalScope(event, new String[]{"n", "10"}, new String[]{"fac", "2"});
                event.prepareContinue();
            });
        }
        assertEquals("3628800", tester.expectDone());
    }

    @Test
    public void testWriteVars() {
        Source function = Source.newBuilder("js", "function main(a1, a2) {\n" +
                        "  var v1 = a1 + a2;\n" +
                        "  let v2 = a1 - a2;\n" +
                        "  let v3 = v1 + v2;\n" +
                        "  return v3;\n" +
                        "}\n" +
                        "main(10, 20);\n", "function.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.suspendNextExecution();
            tester.startEval(function);

            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 7, "main(10, 20)", new String[]{});
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 2, "var v1 = a1 + a2", new String[]{"a1", "10", "a2", "20", "v1", "undefined"});
                DebugStackFrame frame = event.getTopStackFrame();
                DebugScope dscope = frame.getScope();

                DebugValue a1 = dscope.getDeclaredValue("a1");
                DebugValue a2 = dscope.getDeclaredValue("a2");
                assertTrue(a1.isWritable());
                assertTrue(a2.isWritable());
                a1.set(event.getTopStackFrame().eval("100"));
                a2.set(event.getTopStackFrame().eval("200"));
                assertEquals("100", a1.toDisplayString());
                assertEquals("200", a2.toDisplayString());
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 3, "let v2 = a1 - a2", new String[]{"a1", "100", "a2", "200", "v1", "300"});
                DebugStackFrame frame = event.getTopStackFrame();
                DebugScope dscope = frame.getScope();
                DebugValue v1 = dscope.getDeclaredValue("v1");
                assertTrue(v1.isWritable());
                v1.set(event.getTopStackFrame().eval("333"));
                assertEquals("333", v1.toDisplayString());
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 4, "let v3 = v1 + v2", new String[]{"a1", "100", "a2", "200", "v1", "333", "v2", "-100"});
                DebugStackFrame frame = event.getTopStackFrame();
                DebugScope dscope = frame.getScope();
                DebugValue v2 = dscope.getDeclaredValue("v2");
                assertTrue(v2.isWritable());
                v2.set(event.getTopStackFrame().eval("222"));
                assertEquals("222", v2.toDisplayString());
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 5, "return v3;", new String[]{"a1", "100", "a2", "200", "v1", "333", "v2", "222", "v3", "555"});
                DebugStackFrame frame = event.getTopStackFrame();
                DebugScope dscope = frame.getScope();
                DebugValue v3 = dscope.getDeclaredValue("v3");
                assertTrue(v3.isWritable());
                v3.set(event.getTopStackFrame().eval("5"));
                assertEquals("5", v3.toDisplayString());
                event.prepareContinue();
            });
        }
        assertEquals("5", tester.expectDone());
    }

    @Test
    public void testJavaInterop() {
        Source jInterop = Source.newBuilder("js", "let ic = new (Java.type('" + TestInteropClass.class.getName() + "'))();\n" +
                        "let array = ic.getArray();\n" +
                        "let size = array.length;\n" +
                        "size;\n", "jInterop.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.suspendNextExecution();
            tester.startEval(jInterop);

            tester.expectSuspended((SuspendedEvent event) -> {
                int actualLineNumber = event.getSourceSection().getStartLine();
                Assert.assertEquals("Line", 1, actualLineNumber);
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                DebugScope globalScope = event.getSession().getTopScope(JavaScriptLanguage.ID);
                DebugValue ic = globalScope.getDeclaredValue("ic");
                assertNotNull(ic);
                assertFalse(ic.isArray());
                Collection<DebugValue> properties = ic.getProperties();
                int visitedProperties = 0;
                for (DebugValue p : properties) {
                    String name = p.getName();
                    if (name.equals("intField")) {
                        visitedProperties++;
                        assertEquals("10", p.toDisplayString());
                    } else if (name.equals("thisField")) {
                        visitedProperties++;
                        assertFalse(p.getProperties().isEmpty());
                    }
                }
                assertEquals(2, visitedProperties);
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                DebugScope globalScope = event.getSession().getTopScope(JavaScriptLanguage.ID);
                DebugValue array = globalScope.getDeclaredValue("array");
                assertNotNull(array);
                assertTrue(array.isArray());
                List<DebugValue> arrayList = array.getArray();
                assertEquals(3, arrayList.size());
                for (int i = 0; i < 3; i++) {
                    String value = arrayList.get(i).toDisplayString();
                    assertEquals(new String(new char[]{(char) ('A' + i)}), value);
                }
                event.prepareContinue();
            });
        }
        assertEquals("3", tester.expectDone());
    }

    @Test
    public void testClosures() {
        Source function = Source.newBuilder("js", "" +
                        "let outerMost = 42;\n" +
                        "function main(a1, a2) {\n" +
                        "  var v1 = a1 + a2 + outerMost;\n" +
                        "  let v2 = 1234;\n" +
                        "  let f = function(ia1) {\n" +
                        "    let v3 = {};\n" +
                        "    let v2 = 11.11 + v1;\n" +
                        "    v3.a = \"a\";\n" +
                        "    return v2;\n" +
                        "  };\n" +
                        "  return f;\n" +
                        "}\n" +
                        "f = main(10, 20);\n" +
                        "f(5);\n", "function.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.suspendNextExecution();
            tester.startEval(function);

            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 1, "let outerMost = 42", new String[]{});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 13, "f = main(10, 20)", new String[]{});
                checkGlobalScope(event, new String[]{"outerMost", "42"}, new String[]{"main", IGNORE_VALUE});
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 3, "var v1 = a1 + a2 + outerMost", new String[]{"a1", "10", "a2", "20", "v1", "undefined"});
                checkGlobalScope(event, new String[]{"outerMost", "42"}, new String[]{"main", IGNORE_VALUE});
                event.prepareStepOut(1).prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 14, "f(5)", new String[]{});
                checkGlobalScope(event, new String[]{"outerMost", "42"}, new String[]{"main", IGNORE_VALUE, "f", IGNORE_VALUE});
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                if (closureOpt) {
                    checkScope(event, "f", 6, "let v3 = {}", new String[]{"ia1", "5"}, new String[]{"v1", "72"});
                } else {
                    checkScope(event, "f", 6, "let v3 = {}", new String[]{"ia1", "5"}, new String[]{"a1", "10", "a2", "20", "v1", "72", "v2", "1234", "f", IGNORE_VALUE});
                }
                checkGlobalScope(event, new String[]{"outerMost", "42"}, new String[]{"main", IGNORE_VALUE, "f", IGNORE_VALUE});
                event.prepareStepOut(1).prepareStepOver(1);
            });

        }
        assertEquals(Double.toString(11.11 + 10 + 20 + 42), tester.expectDone());
    }

    @Test
    public void testClosures2() {
        Source function = Source.newBuilder("js", "" +
                        "function main(a1, a2) {\n" +
                        "  let v1 = a1 + a2;\n" +
                        "  function nested() {" +
                        "    let v2 = a1 + 2;\n" +
                        "    return (function nested2() {\n" +
                        "      let v3 = v1 + v2;\n" +
                        "      return v3;" +
                        "    })();\n" +
                        "  }\n" +
                        "  return nested();\n" +
                        "}\n" +
                        "main(10, 20);\n", "function.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.suspendNextExecution();
            tester.startEval(function);

            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 10, "main(10, 20)", new String[]{});
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 2, "let v1 = a1 + a2", new String[]{"a1", "10", "a2", "20", "nested", IGNORE_VALUE});
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 8, "return nested();", new String[]{"a1", "10", "a2", "20", "v1", "30", "nested", IGNORE_VALUE});
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                if (closureOpt) {
                    checkScope(event, "nested", 3, "let v2 = a1 + 2", new String[]{}, new String[]{"a1", "10", "v1", "30"});
                } else {
                    checkScope(event, "nested", 3, "let v2 = a1 + 2", new String[]{}, new String[]{"a1", "10", "a2", "20", "v1", "30", "nested", IGNORE_VALUE});
                }
                event.prepareStepOver(1);
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                if (closureOpt) {
                    checkScope(event, "nested2", 5, "let v3 = v1 + v2", new String[]{}, new String[]{"v2", "12"}, new String[]{"a1", "10", "v1", "30"});
                } else {
                    checkScope(event, "nested2", 5, "let v3 = v1 + v2", new String[]{}, new String[]{"v2", "12"}, new String[]{"a1", "10", "a2", "20", "v1", "30", "nested", IGNORE_VALUE});
                }
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                if (closureOpt) {
                    checkScope(event, "nested2", 6, "return v3;", new String[]{"v3", "42"}, new String[]{"v2", "12"}, new String[]{"a1", "10", "v1", "30"});
                } else {
                    checkScope(event, "nested2", 6, "return v3;", new String[]{"v3", "42"}, new String[]{"v2", "12"}, new String[]{"a1", "10", "a2", "20", "v1", "30", "nested", IGNORE_VALUE});
                }
                event.prepareContinue();
            });
        }
        assertEquals("42", tester.expectDone());
    }

    @Test
    public void testScopeReceiverGlobal() {
        Source global = Source.newBuilder("js", "debugger;\n", "thisGlobal.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            tester.startEval(global);
            tester.expectSuspended((SuspendedEvent event) -> {
                String receiver = getScopeReceiver(event);
                assertTrue(receiver, receiver.startsWith("global"));
            });
        }
        tester.expectDone();
    }

    @Test
    public void testScopeReceiverGlobalFunc() {
        Source globalFunc = Source.newBuilder("js", "" +
                        "(function test() {\n" +
                        "  debugger;\n" +
                        "}) ();\n", "thisGlobalFunc.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            tester.startEval(globalFunc);
            tester.expectSuspended((SuspendedEvent event) -> {
                String receiver = getScopeReceiver(event);
                assertTrue(receiver, receiver.startsWith("global"));
            });
        }
        tester.expectDone();
    }

    @Test
    public void testScopeReceiverGlobalFunc2() {
        Source globalFunc = Source.newBuilder("js", "" +
                        "function test() {\n" +
                        "  debugger;\n" +
                        "}\n" +
                        "test();\n", "thisGlobalFunc2.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            tester.startEval(globalFunc);
            tester.expectSuspended((SuspendedEvent event) -> {
                String receiver = getScopeReceiver(event);
                assertTrue(receiver, receiver.startsWith("global"));
            });
        }
        tester.expectDone();
    }

    @Test
    public void testScopeReceiverGlobalStrict() {
        Source globalFunc = Source.newBuilder("js", "" +
                        "\"use strict\";\n" +
                        "(function test() {\n" +
                        "  debugger;\n" +
                        "}) ();\n", "thisGlobalStrict.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            tester.startEval(globalFunc);
            tester.expectSuspended((SuspendedEvent event) -> {
                String receiver = getScopeReceiver(event);
                assertEquals("undefined", receiver);
            });
        }
        tester.expectDone();
    }

    @Test
    public void testScopeReceiverObject() {
        Source objFunc = Source.newBuilder("js", "" +
                        "var obj = {\n" +
                        "  a : 42,\n" +
                        "  testFunc : function() {\n" +
                        "    debugger;\n" +
                        "  }\n" +
                        "};\n" +
                        "obj.testFunc();\n", "thisObject.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            tester.startEval(objFunc);
            tester.expectSuspended((SuspendedEvent event) -> {
                String receiver = getScopeReceiver(event);
                assertEquals("{a: 42, testFunc: function() { debugger; }}", receiver.replaceAll("\\s+", " "));
            });
        }
        tester.expectDone();
    }

    @Test
    public void testScopeReceiverCalled() {
        Source objFunc = Source.newBuilder("js", "" +
                        "function test() {\n" +
                        "  debugger;\n" +
                        "}\n" +
                        "obj = {c : 42};\n" +
                        "test.call(obj);\n", "thisObjectCalled.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            tester.startEval(objFunc);
            tester.expectSuspended((SuspendedEvent event) -> {
                String receiver = getScopeReceiver(event);
                assertEquals("{c: 42}", receiver);
            });
        }
        tester.expectDone();
    }

    @Test
    public void testScopeSourceSection() {
        Source function = Source.newBuilder("js", "function main(a1, a2) {\n" +
                        "  var v1 = a1 + a2;\n" +
                        "  {\n" +
                        "    let v3 = {};\n" +
                        "    let v2 = 11.11;\n" +
                        "    v3.a = v2;\n" +
                        "  }\n" +
                        "  return v1;\n" +
                        "}\n" +
                        "main(10, 20);\n", "function.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.suspendNextExecution();
            tester.startEval(function);

            tester.expectSuspended((SuspendedEvent event) -> {
                event.prepareStepInto(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 2, "var v1 = a1 + a2", new String[]{"a1", "10", "a2", "20", "v1", "undefined"});
                SourceSection scopeSourceSection = event.getTopStackFrame().getScope().getSourceSection();
                assertTrue(String.valueOf(scopeSourceSection), scopeSourceSection != null && scopeSourceSection.isAvailable());
                assertEquals(1, scopeSourceSection.getStartLine());
                event.prepareStepOver(1);
            });
            // In block:
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 4, "let v3 = {}", new String[]{}, new String[]{"a1", "10", "a2", "20", "v1", "30"});
                SourceSection scopeSourceSection = event.getTopStackFrame().getScope().getSourceSection();
                assertTrue(String.valueOf(scopeSourceSection), scopeSourceSection != null && scopeSourceSection.isAvailable());
                assertEquals(3, scopeSourceSection.getStartLine());
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 5, "let v2 = 11.11", new String[]{"v3", "{}"}, new String[]{"a1", "10", "a2", "20", "v1", "30"});
                SourceSection scopeSourceSection = event.getTopStackFrame().getScope().getSourceSection();
                assertTrue(String.valueOf(scopeSourceSection), scopeSourceSection != null && scopeSourceSection.isAvailable());
                assertEquals(3, scopeSourceSection.getStartLine());
                event.prepareStepOver(1);
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 6, "v3.a = v2", new String[]{"v3", "{}", "v2", "11.11"}, new String[]{"a1", "10", "a2", "20", "v1", "30"});
                SourceSection scopeSourceSection = event.getTopStackFrame().getScope().getSourceSection();
                assertTrue(String.valueOf(scopeSourceSection), scopeSourceSection != null && scopeSourceSection.isAvailable());
                assertEquals(3, scopeSourceSection.getStartLine());
                event.prepareStepOver(1);
            });
            // Out of block:
            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, "main", 8, "return v1;", new String[]{"a1", "10", "a2", "20", "v1", "30"});
                SourceSection scopeSourceSection = event.getTopStackFrame().getScope().getSourceSection();
                assertTrue(String.valueOf(scopeSourceSection), scopeSourceSection != null && scopeSourceSection.isAvailable());
                assertEquals(1, scopeSourceSection.getStartLine());
                event.prepareContinue();
            });
        }
        assertEquals("30", tester.expectDone());
    }

    @Test
    public void testEvalDynamicScope() throws Throwable {
        final Source testDebugger = Source.newBuilder("js", "" +
                        "var s = `\n" +
                        "var x = 10;\n" +
                        "var y = 11;\n" +
                        "var z = 12;\n" +
                        "debugger;\n" +
                        "var total = x + y + z;\n" +
                        "total;`;\n" +
                        "var x = function(s) { return eval(s); };\n" +
                        "x(s);\n" +
                        "eval(s);\n", "testEvalScope.js").buildLiteral();

        try (DebuggerSession session = tester.startSession()) {
            tester.startEval(testDebugger);

            tester.expectSuspended((SuspendedEvent event) -> {
                checkScope(event, ":program", 5, "debugger;",
                                new String[]{},
                                new String[]{"s", IGNORE_VALUE, "x", "10", "y", "11", "z", "12", "total", "undefined", "arguments", IGNORE_VALUE},
                                new String[]{});
                assertTrue(event.getTopStackFrame().eval("s").isString());
                DebugValue x = event.getTopStackFrame().eval("x");
                assertTrue(x.isNumber());
                assertEquals(10, x.asInt());
                DebugValue y = event.getTopStackFrame().eval("y");
                assertTrue(y.isNumber());
                assertEquals(11, y.asInt());
                DebugValue z = event.getTopStackFrame().eval("z");
                assertTrue(z.isNumber());
                assertEquals(12, z.asInt());
                event.prepareContinue();
            });
        }
    }

    private static String getScopeReceiver(SuspendedEvent suspendedEvent) {
        return getScopeReceiver(suspendedEvent.getTopStackFrame());
    }

    static String getScopeReceiver(DebugStackFrame frame) {
        DebugScope scope = frame.getScope();
        while (!scope.isFunctionScope()) {
            assertNull(scope.getReceiver());
            scope = scope.getParent();
        }
        DebugValue receiver = scope.getReceiver();
        assertEquals("this", receiver.getName());
        return receiver.toDisplayString();
    }

    private static void checkScope(SuspendedEvent suspendedEvent, String name, final int expectedLineNumber, final String expectedCode, final String[]... expectedScopes) {
        final int actualLineNumber = suspendedEvent.getSourceSection().getStartLine();
        Assert.assertEquals("Line", expectedLineNumber, actualLineNumber);
        final String actualCode = suspendedEvent.getSourceSection().getCharacters().toString();
        Assert.assertEquals("Code", expectedCode, actualCode);

        DebugStackFrame frame = suspendedEvent.getTopStackFrame();
        assertEquals(name, frame.getName());
        DebugScope dscope = frame.getScope();
        checkScopes(dscope, expectedScopes);
    }

    private static void checkGlobalScope(SuspendedEvent suspendedEvent, final String[]... expectedScopes) {
        checkScopes(suspendedEvent.getSession().getTopScope(JavaScriptLanguage.ID), expectedScopes);
    }

    private static void checkScopes(DebugScope topScope, final String[]... expectedScopes) {
        DebugScope dscope = topScope;
        for (String[] expectedScope : expectedScopes) {
            Assert.assertNotNull("No debug scope for " + Arrays.toString(expectedScope), dscope);
            Map<String, DebugValue> values = new LinkedHashMap<>();
            for (DebugValue value : dscope.getDeclaredValues()) {
                values.put(value.getName(), value);
            }

            // Ignore additional global object properties.
            boolean isGlobalObject = dscope.getDeclaredValue("undefined") != null;
            if (!isGlobalObject) {
                String message = String.format("Frame expected %s got %s", Arrays.toString(expectedScope), values.toString());
                Assert.assertEquals(message, expectedScope.length / 2, values.size());
            }

            for (int i = 0; i < expectedScope.length; i = i + 2) {
                String expectedIdentifier = expectedScope[i];
                String expectedValue = expectedScope[i + 1];
                DebugValue value = values.get(expectedIdentifier);
                Assert.assertNotNull(expectedIdentifier + " not found", value);
                Assert.assertEquals(expectedIdentifier, value.getName());
                if (!Objects.equals(IGNORE_VALUE, expectedValue)) {
                    Assert.assertEquals("Variable " + expectedIdentifier, expectedValue, value.toDisplayString());
                }
            }
            dscope = dscope.getParent();
        }
        Assert.assertNull("An extra scope", dscope);
    }

    public static class TestInteropClass {

        public int intField = 10;
        public TestInteropClass thisField = this;

        public String[] getArray() {
            return new String[]{"A", "B", "C"};
        }
    }
}
