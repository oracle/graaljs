/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.api.debug.Breakpoint;
import com.oracle.truffle.api.debug.DebugScope;
import com.oracle.truffle.api.debug.DebugStackFrame;
import com.oracle.truffle.api.debug.DebugStackTraceElement;
import com.oracle.truffle.api.debug.DebugValue;
import com.oracle.truffle.api.debug.DebuggerSession;
import com.oracle.truffle.api.debug.SuspendAnchor;
import com.oracle.truffle.api.debug.SuspendedCallback;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.tck.DebuggerTester;

@RunWith(Parameterized.class)
public class JSDebugTest {

    @Parameters(name = "{0}")
    public static List<Boolean> data() {
        return Arrays.asList(Boolean.TRUE, Boolean.FALSE);
    }

    private DebuggerTester tester;
    @Parameter(value = 0) public boolean propertyCaches;

    @Before
    public void before() {
        Context.Builder contextBuilder = JSTest.newContextBuilder();
        contextBuilder.allowHostAccess(HostAccess.ALL);
        contextBuilder.option(JSContextOptions.DEBUG_BUILTIN_NAME, Boolean.toString(true));
        if (!propertyCaches) {
            contextBuilder.option(JSContextOptions.PROPERTY_CACHE_LIMIT_NAME, String.valueOf(0));
        }
        tester = new DebuggerTester(contextBuilder);
    }

    @After
    public void dispose() {
        tester.close();
    }

    private static Source createFactorial() {
        return Source.newBuilder("js", "function main() {\n" +
                        "  res = fac(2);\n" + "  print(res);\n" +
                        "  return res;\n" +
                        "}\n" +
                        "function fac(n) {\n" +
                        "  if (n <= 1) {\n" +
                        "    return 1;\n" + "  }\n" +
                        "  nMinusOne = n - 1;\n" +
                        "  nMOFact = fac(nMinusOne);\n" +
                        "  res = n * nMOFact;\n" +
                        "  return res;\n" + "}\n" +
                        "main();\n", "factorial.js").buildLiteral();
    }

    private static Source createGlobalFactorial() {
        return Source.newBuilder("js", "var f = 1;\n" +
                        "function fac() {\n" +
                        "  var n = 10;\n" +
                        "  while (n > 1) {\n" +
                        "    f *= n;\n" +
                        "    n--;\n" +
                        "  }\n" +
                        "  return f;\n" +
                        "}\n" +
                        "fac();\n", "factorial.js").buildLiteral();
    }

    private static Source createTestFor() {
        return Source.newBuilder("js", "function testFor(n) {\n" +
                        "  var res = 0;\n" +
                        "  for (var i = 0; i < n; i++) {\n" +
                        "    res = res + 1;\n" +
                        "  }\n" +
                        "  return res;\n" +
                        "}\n" +
                        "testFor(2);\n", "testFor.js").buildLiteral();
    }

    private static Source createTestDebuggerStmt() {
        return Source.newBuilder("js", "function testDebuggerStmt(n) {\n" +
                        "  var n = 2;\n" +
                        "  while (n-- > 0) {\n" +
                        "    debugger;\n" +
                        "  }\n" +
                        "}\n" +
                        "testDebuggerStmt();\n", "testDebuggerStmt.js").buildLiteral();
    }

    private static Source createTestEnclosingScopeAccess() {
        return Source.newBuilder("js", "function test1() {\n" +
                        "  var a = 2;\n" +
                        "  function test2() {\n" +
                        "    var b = 40;\n" +
                        "    function test3() {\n" +
                        "      let c = a + b;\n" +
                        "      return c;\n" +
                        "    }\n" +
                        "    return test3();\n" +
                        "  }\n" +
                        "  return test2();\n" +
                        "}\n" +
                        "test1();\n", "testEnclosingScopeAccess.js").buildLiteral();
    }

    private static Source createTestTypes() {
        return Source.newBuilder("js", "function typesTest() {\n" +
                        "    let a1 = [];\n" +
                        "    let a2 = [1, 2, [3, 4]];\n" +
                        "    let b1 = true;\n" +
                        "    let b2 = false;\n" +
                        "    let c1 = new TestClass();\n" +
                        "    let i1 = 42;\n" +
                        "    let i2 = 42.42;\n" +
                        "    let i3 = 1000000000000000;\n" +
                        "    let i4 = -0.0;\n" +
                        "    let i5 = 1/i4;\n" +
                        "    let i6 = 1/0.0;\n" +
                        "    let i7 = 0.0/0.0;\n" +
                        "    let i8 = Debug.createSafeInteger(4242);\n" +
                        "    let i9 = 2**69;\n" +
                        "    let bi1 = 42n;\n" +
                        "    let bi2 = 42n**24n;\n" +
                        "    let s1 = \"String\";\n" +
                        "    let f1 = function pow2(x) {\n" +
                        "        return x*x;\n" +
                        "    };\n" +
                        "    let d1 = new Date(Date.UTC(80, 0, 1, 10, 10, 10));\n" +
                        "    let undef;\n" +
                        "    let nul = null;\n" +
                        "    let sy1 = Symbol();\n" +
                        "    let sy2 = Symbol('symbolic');\n" +
                        "    let o1 = {};\n" +
                        "    let o2 = new Test();\n" +
                        "    let o3 = {};\n" +
                        "    o3.a = \"A\";\n" +
                        "\n" +
                        "    debugger;\n" +
                        "}\n" +
                        "\n" +
                        "function Test() {\n" +
                        "}\n" +
                        "\n" +
                        "class TestClass {\n" +
                        "}\n" +
                        "\n" +
                        "typesTest();\n", "testTypes.js").buildLiteral();
    }

    private static Source createTestGettersSetters() {
        return Source.newBuilder("js", "function gsTest() {\n" +
                        "    let person = {\n" +
                        "        firstName: 'Jimmy',\n" +
                        "        lastName: 'Smith',\n" +
                        "        numQueries: 0,\n" +
                        "        get fullName() {\n" +
                        "            this.numQueries++;\n" +
                        "            return this.firstName + ' ' + this.lastName;\n" +
                        "        },\n" +
                        "        set fullName(name) {\n" +
                        "            var words = name.toString().split(' ');\n" +
                        "            this.firstName = words[0] || '';\n" +
                        "            this.lastName = words[1] || '';\n" +
                        "        },\n" +
                        "        get justGet() {\n" +
                        "            this.numQueries++;\n" +
                        "            return \"Get for \" + this.firstName;\n" +
                        "        },\n" +
                        "        set justSet(firstName) {\n" +
                        "            this.firstName = firstName;\n" +
                        "        },\n" +
                        "    }\n" +
                        "    debugger;\n" +
                        "    person.justSet = person.fullName + \" \" + person.justGet;\n" +
                        "    person.fullName = person.justGet.length > 0 ? person.firstName : \"\";\n" +
                        "    return person.fullName;\n" +
                        "}" +
                        "gsTest();", "gsTest.js").buildLiteral();
    }

    private static File createGlobalFactorialFile() throws IOException {
        File file = File.createTempFile("factorial", "js");
        file.deleteOnExit();
        try (Writer w = new FileWriter(file)) {
            w.append(createGlobalFactorial().getCharacters());
        }
        return file;
    }

    private void startEval(Source code) {
        tester.startEval(code);
    }

    private DebuggerSession startSession() {
        return tester.startSession();
    }

    private String expectDone() {
        return tester.expectDone();
    }

    private void expectSuspended(SuspendedCallback callback) {
        tester.expectSuspended(callback);
    }

    private static SuspendedEvent checkState(SuspendedEvent suspendedEvent, String name, final int expectedLineNumber, final boolean expectedIsBefore, final String expectedCode,
                    final String... expectedFrame) {
        return checkState(suspendedEvent, name, expectedLineNumber, -1, expectedIsBefore, expectedCode, expectedFrame);
    }

    private static SuspendedEvent checkState(SuspendedEvent suspendedEvent, String name, final int expectedLineNumber, final int expectedColumnNumber,
                    final boolean expectedIsBefore, final String expectedCode, final String... expectedFrame) {
        final int actualLineNumber;
        final int actualColumnNumber;
        SourceSection sourceSection = suspendedEvent.getSourceSection();
        if (expectedIsBefore) {
            actualLineNumber = sourceSection.getStartLine();
            actualColumnNumber = sourceSection.getStartColumn();
        } else {
            actualLineNumber = sourceSection.getEndLine();
            actualColumnNumber = sourceSection.getEndColumn();
        }
        Assert.assertEquals(expectedLineNumber, actualLineNumber);
        if (expectedColumnNumber != -1) {
            Assert.assertEquals(expectedColumnNumber, actualColumnNumber);
        }
        final String actualCode = suspendedEvent.getSourceSection().getCharacters().toString();
        Assert.assertEquals(expectedCode, actualCode);
        final boolean actualIsBefore = suspendedEvent.getSuspendAnchor() == SuspendAnchor.BEFORE;
        Assert.assertEquals(expectedIsBefore, actualIsBefore);

        checkStack(suspendedEvent.getTopStackFrame(), name, expectedFrame);
        return suspendedEvent;
    }

    private static void checkStack(DebugStackFrame frame, String name, String... expectedFrame) {
        String[] expectedVars = expectedFrame;
        assertEquals(name, frame.getName());
        for (int i = 0; i < expectedFrame.length; i += 2) {
            if ("this".equals(expectedFrame[i])) {
                String thisValue = expectedFrame[i + 1];
                assertEquals(thisValue, TestScope.getScopeReceiver(frame));
                expectedVars = new String[expectedFrame.length - 2];
                System.arraycopy(expectedFrame, 0, expectedVars, 0, i);
                System.arraycopy(expectedFrame, i + 2, expectedVars, i, expectedVars.length - i);
                break;
            }
        }
        checkDebugValues("variables", frame.getScope().getDeclaredValues(), expectedVars);
    }

    private static void checkDebugValues(String msg, Iterable<DebugValue> values, String... expectedFrame) {
        Map<String, DebugValue> valMap = new LinkedHashMap<>();
        for (DebugValue value : values) {
            if ("this".equals(value.getName())) {
                continue;
            }
            valMap.put(value.getName(), value);
        }
        String message = String.format("Frame %s expected %s got %s", msg, Arrays.toString(expectedFrame),
                        valMap.entrySet().stream().map(e -> e.getKey() + ", " + e.getValue().toDisplayString()).collect(Collectors.joining(", ", "[", "]")));
        Assert.assertEquals(message, expectedFrame.length / 2, valMap.size());
        for (int i = 0; i < expectedFrame.length; i = i + 2) {
            String expectedIdentifier = expectedFrame[i];
            String expectedValue = expectedFrame[i + 1];
            DebugValue value = valMap.get(expectedIdentifier);
            Assert.assertNotNull(expectedIdentifier + " not found", value);
            Assert.assertEquals(expectedIdentifier, value.getName());
            Assert.assertEquals(expectedValue, value.toDisplayString());
        }
    }

    @Test
    public void testBreakpoint() throws Throwable {
        final Source factorial = createFactorial();

        try (DebuggerSession session = startSession()) {
            startEval(factorial);
            Breakpoint breakpoint = session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(factorial)).lineIs(8).build());

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "fac", 8, true, "return 1;", "n", "1");
                assertSame(breakpoint, event.getBreakpoints().iterator().next());
                event.prepareContinue();
            });
        }
        assertEquals("2", expectDone());
    }

    @Test
    public void testConditionalBreakpoint() throws Throwable {
        final Source factorial = createGlobalFactorial();

        try (DebuggerSession session = startSession()) {
            startEval(factorial);
            Breakpoint breakpoint = Breakpoint.newBuilder(DebuggerTester.getSourceImpl(factorial)).lineIs(5).build();
            breakpoint.setCondition("n === 5");     // Condition with a local variable 'n'
            session.install(breakpoint);

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "fac", 5, true, "f *= n", "n", "5");
                assertSame(breakpoint, event.getBreakpoints().iterator().next());
                event.prepareContinue();
            });
        }
        assertEquals("3628800", expectDone());
    }

    @Test
    public void testConditionalURIBreakpoint() throws Throwable {
        final File factorialFile = createGlobalFactorialFile().getCanonicalFile();
        final Source factorialSource = Source.newBuilder("js", factorialFile).build();

        try (DebuggerSession session = startSession()) {
            startEval(factorialSource);
            Breakpoint breakpoint1 = Breakpoint.newBuilder(factorialFile.toURI()).lineIs(5).build();
            // Condition with a local variable 'n'
            breakpoint1.setCondition("n === 5");
            session.install(breakpoint1);

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "fac", 5, true, "f *= n", "n", "5");
                assertSame(breakpoint1, event.getBreakpoints().iterator().next());
                event.prepareContinue();
            });
            assertEquals("3628800", expectDone());

            startEval(factorialSource);
            Breakpoint breakpoint2 = Breakpoint.newBuilder(factorialFile.toURI()).lineIs(6).build();
            // Condition with a global variable 'f'
            breakpoint2.setCondition("f > " + 10 * 9 * 8 * 7 * 6 * 5 * 4);
            session.install(breakpoint2);

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "fac", 5, true, "f *= n", "n", "5");
                assertSame(breakpoint1, event.getBreakpoints().iterator().next());
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "fac", 6, true, "n--", "n", "3");
                assertSame(breakpoint2, event.getBreakpoints().iterator().next());
                event.prepareContinue();
            });
        }
        assertEquals("3628800", expectDone());
    }

    @Test
    public void testStepInStepOver() throws Throwable {
        final Source factorial = createFactorial();

        try (DebuggerSession session = startSession()) {
            session.suspendNextExecution();
            startEval(factorial);

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, ":program", 15, true, "main()");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "main", 2, true, "res = fac(2)");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "fac", 7, true, "if (n <= 1) {\n    return 1;\n  }", "n", "2");
                event.prepareStepOver(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "fac", 10, true, "nMinusOne = n - 1", "n", "2");
                event.prepareStepOver(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "fac", 11, true, "nMOFact = fac(nMinusOne)", "n", "2");
                event.prepareStepOver(2);
            });
            expectSuspended((SuspendedEvent event) -> {
                // Should this be "return res"?
                checkState(event, "fac", 13, true, "return res;", "n", "2");
                event.prepareStepOver(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "main", 2, false, "fac(2)");
                event.prepareStepOut(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, ":program", 15, false, "main()");
                event.prepareStepOver(1);
            });
            assertEquals("2", expectDone());
        }
    }

    @Test
    public void testFor() throws Throwable {
        final Source testFor = createTestFor();

        try (DebuggerSession session = startSession()) {
            session.suspendNextExecution();
            startEval(testFor);

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, ":program", 8, true, "testFor(2)");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 2, true, "var res = 0", "n", "2",
                                "res", "undefined", "i", "undefined");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 3, true, "var i = 0", "n", "2",
                                "res", "0", "i", "undefined");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 3, true, "i < n", "n", "2",
                                "res", "0", "i", "0");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 4, true, "res = res + 1", "n", "2",
                                "res", "0", "i", "0");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 3, true, "i++", "n", "2",
                                "res", "1", "i", "0");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 3, true, "i < n", "n", "2",
                                "res", "1", "i", "1");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 4, true, "res = res + 1", "n", "2",
                                "res", "1", "i", "1");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 3, true, "i++", "n", "2",
                                "res", "2", "i", "1");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 3, true, "i < n", "n", "2",
                                "res", "2", "i", "2");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 6, true, "return res;", "n", "2",
                                "res", "2", "i", "2");
                event.prepareContinue();
            });
        }
        assertEquals("2", expectDone());
    }

    @Test
    public void testDebuggerStmt() throws Throwable {
        final Source testDebugger = createTestDebuggerStmt();

        try (DebuggerSession session = startSession()) {
            startEval(testDebugger);

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testDebuggerStmt", 4, true, "debugger;", "n", "1");
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testDebuggerStmt", 4, true, "debugger;", "n", "0");
                event.prepareContinue();
            });
            expectDone();
        }
    }

    @Test
    public void testEvalModifyVar() throws Throwable {
        final Source testDebugger = createTestDebuggerStmt();

        try (DebuggerSession session = startSession()) {
            startEval(testDebugger);

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testDebuggerStmt", 4, true, "debugger;", "n", "1");
                DebugValue value = event.getTopStackFrame().eval("--n");
                assertEquals(0, value.asInt());
                event.prepareContinue();
            });
            expectDone();
        }
    }

    @Test
    public void testEnclosingScopeAccess() throws Throwable {
        final Source source = createTestEnclosingScopeAccess();

        try (DebuggerSession session = startSession()) {
            startEval(source);

            Breakpoint breakpoint1 = Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(7).build();
            breakpoint1.setCondition("print(a, b, c) || (a + b + c) === 2*42");
            session.install(breakpoint1);

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "test3", 7, true, "return c;", "c", "42");
                event.prepareContinue();
            });
        }
        assertEquals("42", expectDone());
    }

    private static Source createTestForLet() {
        return Source.newBuilder("js", "" +
                        "function testFor(n) {\n" +
                        "  var res = 0;\n" +
                        "  for (let i = 0; i < n; i++) {\n" +
                        "    res = res + 1;\n" +
                        "  }\n" +
                        "  return res;\n" +
                        "}\n" +
                        "testFor(3);\n", "testForLet.js").buildLiteral();
    }

    @Test
    public void testForLet() throws Throwable {
        final Source testForLet = createTestForLet();

        try (DebuggerSession session = startSession()) {
            session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(testForLet)).lineIs(4).build());
            startEval(testForLet);

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 4, true, "res = res + 1", "i", "0");
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 4, true, "res = res + 1", "i", "1");
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 4, true, "res = res + 1", "i", "2");
                event.prepareContinue();
            });
        }
        assertEquals("3", expectDone());
    }

    @Test
    public void testForLetConditional() throws Throwable {
        final Source testForLet = createTestForLet();

        try (DebuggerSession session = startSession()) {
            Breakpoint breakpoint = Breakpoint.newBuilder(DebuggerTester.getSourceImpl(testForLet)).lineIs(4).build();
            breakpoint.setCondition("i % 2 == 0");
            session.install(breakpoint);
            startEval(testForLet);

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 4, true, "res = res + 1", "i", "0");
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor", 4, true, "res = res + 1", "i", "2");
                event.prepareContinue();
            });
        }
        assertEquals("3", expectDone());
    }

    private static Source createTestForLetNested() {
        return Source.newBuilder("js", "" +
                        "function testFor1(n) {\n" +
                        "  var res = 0;\n" +
                        "  for (let i = 0; i < n; i++) {\n" +
                        "    let inc = i;\n" +
                        "    (function testFor2() {\n" +
                        "      for (let j = 0; j < n; j++) {\n" +
                        "        res = res + inc;\n" +
                        "      }\n" +
                        "    })();\n" +
                        "  }\n" +
                        "  return res;\n" +
                        "}\n" +
                        "testFor1(4);\n", "testForLetNested.js").buildLiteral();
    }

    @Test
    public void testForLetNested() throws Throwable {
        final Source source = createTestForLetNested();

        try (DebuggerSession session = startSession()) {
            Breakpoint breakpoint = Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(7).build();
            breakpoint.setCondition("i % 2 == 0 && j % 2 == 1");
            session.install(breakpoint);
            session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(11).build());
            startEval(source);

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor2", 7, true, "res = res + inc", "j", "1");
                assertEquals("0", event.getTopStackFrame().eval("i").toDisplayString());
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor2", 7, true, "res = res + inc", "j", "3");
                assertEquals("0", event.getTopStackFrame().eval("i").toDisplayString());
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor2", 7, true, "res = res + inc", "j", "1");
                assertEquals("2", event.getTopStackFrame().eval("i").toDisplayString());
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor2", 7, true, "res = res + inc", "j", "3");
                assertEquals("2", event.getTopStackFrame().eval("i").toDisplayString());
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testFor1", 11, true, "return res;", "n", "4", "res", "24");
                event.prepareContinue();
            });
        }
        assertEquals("24", expectDone());
    }

    @Test
    public void testTypes() throws Throwable {
        try (DebuggerSession session = startSession()) {
            startEval(createTestTypes());

            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(32, frame.getSourceSection().getStartLine());
                DebugScope scope = frame.getScope();

                DebugValue dv = scope.getDeclaredValue("a1");
                assertEquals(true, dv.isArray());
                assertEquals(true, dv.getArray().isEmpty());
                assertEquals("[]", dv.toDisplayString());
                checkMetaObject("a1 meta object", dv, "Array");
                assertTrue("a1.isArray()", dv.isArray());

                dv = scope.getDeclaredValue("a2");
                assertEquals(true, dv.isArray());
                assertEquals(3, dv.getArray().size());
                assertEquals("(3)[1, 2, [3, 4]]", dv.toDisplayString());
                checkMetaObject("a2 meta object", dv, "Array");
                assertTrue("a2.isArray()", dv.isArray());

                dv = scope.getDeclaredValue("b1");
                assertEquals(false, dv.isArray());
                assertEquals("true", dv.toDisplayString());
                checkMetaObject("b1 meta object", dv, "boolean");
                assertTrue("b1.isBoolean()", dv.isBoolean());

                dv = scope.getDeclaredValue("b2");
                assertEquals("false", dv.toDisplayString());
                checkMetaObject("b2 meta object", dv, "boolean");
                assertTrue("b2.isBoolean()", dv.isBoolean());

                dv = scope.getDeclaredValue("c1");
                assertEquals("{}", dv.toDisplayString());
                checkMetaObject("b1 meta object", dv, "TestClass");

                dv = scope.getDeclaredValue("i1");
                assertEquals("42", dv.toDisplayString());
                checkMetaObject("i1 meta object", dv, "number");
                assertTrue("i1.isNumber()", dv.isNumber());
                assertTrue("i1.fitsInInt()", dv.fitsInInt());

                dv = scope.getDeclaredValue("i2");
                assertEquals("42.42", dv.toDisplayString());
                checkMetaObject("i2 meta object", dv, "number");
                assertTrue("i2.isNumber()", dv.isNumber());
                assertTrue("i2.fitsInDouble()", dv.fitsInDouble());
                assertFalse("i2.fitsInInt()", dv.fitsInInt());

                dv = scope.getDeclaredValue("i3");
                assertEquals("1000000000000000", dv.toDisplayString());
                checkMetaObject("i3 meta object", dv, "number");
                assertTrue("i4.isNumber()", dv.isNumber());
                assertTrue("i4.fitsInDouble()", dv.fitsInDouble());
                assertFalse("i4.fitsInInt()", dv.fitsInInt());

                dv = scope.getDeclaredValue("i4");
                assertEquals("-0", dv.toDisplayString());
                checkMetaObject("i4 meta object", dv, "number");
                assertTrue("i4.isNumber()", dv.isNumber());
                assertTrue("i4.fitsInDouble()", dv.fitsInDouble());
                assertFalse("i4.fitsInInt()", dv.fitsInInt());

                dv = scope.getDeclaredValue("i5");
                assertEquals("-Infinity", dv.toDisplayString());
                checkMetaObject("i5 meta object", dv, "number");
                assertTrue("i5.isNumber()", dv.isNumber());
                assertTrue("i5.fitsInDouble()", dv.fitsInDouble());
                assertFalse("i5.fitsInInt()", dv.fitsInInt());

                dv = scope.getDeclaredValue("i6");
                assertEquals("Infinity", dv.toDisplayString());
                checkMetaObject("i6 meta object", dv, "number");
                assertTrue("i6.isNumber()", dv.isNumber());
                assertTrue("i6.fitsInDouble()", dv.fitsInDouble());
                assertFalse("i6.fitsInInt()", dv.fitsInInt());

                dv = scope.getDeclaredValue("i7");
                assertEquals("NaN", dv.toDisplayString());
                checkMetaObject("i7 meta object", dv, "number");
                assertTrue("i7.isNumber()", dv.isNumber());
                assertTrue("i7.fitsInDouble()", dv.fitsInDouble());

                dv = scope.getDeclaredValue("i8");
                assertEquals("4242", dv.toDisplayString());
                checkMetaObject("i8 meta object", dv, "number");
                assertTrue("i8.isNumber()", dv.isNumber());
                assertTrue("i8.fitsInInt()", dv.fitsInInt());

                dv = scope.getDeclaredValue("i9");
                assertEquals("590295810358705700000", dv.toDisplayString());
                checkMetaObject("i9 meta object", dv, "number");
                assertTrue("i9.isNumber()", dv.isNumber());
                assertTrue("i9.fitsInDouble()", dv.fitsInDouble());
                assertFalse("i9.fitsInInt()", dv.fitsInInt());

                dv = scope.getDeclaredValue("bi1");
                assertEquals("42n", dv.toDisplayString());
                checkMetaObject("bi1 meta object", dv, "bigint");

                dv = scope.getDeclaredValue("bi2");
                assertEquals("907784931546351634835748413459499319296n", dv.toDisplayString());
                checkMetaObject("bi2 meta object", dv, "bigint");

                dv = scope.getDeclaredValue("s1");
                assertEquals("String", dv.toDisplayString());
                checkMetaObject("s1 meta object", dv, "string");
                assertTrue("s1.isString()", dv.isString());

                String fnc = "function pow2(x) {\n" +
                                "        return x*x;\n" +
                                "    }";
                dv = scope.getDeclaredValue("f1");
                assertEquals(fnc, dv.toDisplayString());
                checkMetaObject("f1 meta object", dv, "Function");
                assertTrue("f1.isMetaObject()", dv.isMetaObject());
                assertEquals("pow2", dv.getMetaSimpleName());
                assertTrue("f1.canExecute()", dv.canExecute());
                assertEquals(fnc, dv.getProperty("toString").execute().asString());

                dv = scope.getDeclaredValue("d1");
                assertEquals("1980-01-01T10:10:10.000Z", dv.toDisplayString());
                checkMetaObject("d1 meta object", dv, "Date");
                assertTrue("d1.isInstant()", dv.isInstant());

                dv = scope.getDeclaredValue("undef");
                assertEquals("undefined", dv.toDisplayString());
                checkMetaObject("undef meta object", dv, "undefined");
                assertTrue("undef.isNull()", dv.isNull());

                dv = scope.getDeclaredValue("nul");
                assertEquals("null", dv.toDisplayString());
                checkMetaObject("nul meta object", dv, "null");
                assertTrue("nul.isNull()", dv.isNull());

                dv = scope.getDeclaredValue("sy1");
                assertEquals("Symbol()", dv.toDisplayString());
                checkMetaObject("sy1 meta object", dv, "symbol");

                dv = scope.getDeclaredValue("sy2");
                assertEquals("Symbol(symbolic)", dv.toDisplayString());
                checkMetaObject("sy2 meta object", dv, "symbol");

                dv = scope.getDeclaredValue("o1");
                assertEquals("{}", dv.toDisplayString());
                checkMetaObject("o1 meta object", dv, "Object");

                dv = scope.getDeclaredValue("o2");
                assertEquals("{}", dv.toDisplayString());
                checkMetaObject("o2 meta object", dv, "Test");

                dv = scope.getDeclaredValue("o3");
                assertEquals("{a: \"A\"}", dv.toDisplayString());
                checkMetaObject("o3 meta object", dv, "Object");
            });
        }
    }

    private static void checkMetaObject(String msg, DebugValue dv, String metaObjectName) {
        DebugValue metaObject = dv.getMetaObject();
        assertNotNull(msg, metaObject);
        assertEquals(msg, metaObjectName, metaObject.getMetaQualifiedName());
        assertEquals(msg, metaObjectName, metaObject.getMetaSimpleName());
    }

    @Test
    public void testReenterArgumentsAndValues() throws Throwable {
        // Test that after a re-enter, arguments are kept and variables are cleared.
        final Source source = Source.newBuilder("js", "" +
                        "function main() {\n" +
                        "  let i = 10;\n" +
                        "  return fnc(i = i + 1, 20);\n" +
                        "}\n" +
                        "function fnc(n, m) {\n" +
                        "  let x = n + m;\n" +
                        "  n = m - n;\n" +
                        "  m = m / 2;\n" +
                        "  x = x + n * m;\n" +
                        "  return x;\n" +
                        "}\n" +
                        "main();\n", "testReenterArgsAndVals.js").buildLiteral();

        try (DebuggerSession session = startSession()) {
            session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(6).build());
            startEval(source);

            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(6, frame.getSourceSection().getStartLine());
                checkStack(frame, "fnc", "n", "11", "m", "20");
                event.prepareStepOver(4);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(10, frame.getSourceSection().getStartLine());
                checkStack(frame, "fnc", "n", "9", "m", "10", "x", "121");
                event.prepareUnwindFrame(frame);
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(3, frame.getSourceSection().getStartLine());
                checkStack(frame, "main", "i", "11");
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(6, frame.getSourceSection().getStartLine());
                checkStack(frame, "fnc", "n", "11", "m", "20");
            });
            assertEquals("121", expectDone());
        }
    }

    @Test
    public void testArguments() {
        // Test calling a function with less arguments than declared.
        final Source source = Source.newBuilder("js", "" +
                        "function main(a1, a2) {\n" +
                        "  return a1 + a2;\n" +
                        "}\n" +
                        "main();\n" +
                        "main(10);\n" +
                        "main(10, 20);\n" +
                        "main(10, 20, 30);\n",
                        "function.js").buildLiteral();
        try (DebuggerSession session = tester.startSession()) {
            session.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(2).build());
            tester.startEval(source);

            tester.expectSuspended((SuspendedEvent event) -> {
                checkStack(event.getTopStackFrame(), "main", new String[]{"a1", "undefined", "a2", "undefined"});
                event.prepareContinue();
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkStack(event.getTopStackFrame(), "main", new String[]{"a1", "10", "a2", "undefined"});
                event.prepareContinue();
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkStack(event.getTopStackFrame(), "main", new String[]{"a1", "10", "a2", "20"});
                event.prepareContinue();
            });
            tester.expectSuspended((SuspendedEvent event) -> {
                checkStack(event.getTopStackFrame(), "main", new String[]{"a1", "10", "a2", "20"});
                event.prepareContinue();
            });
        }
        assertEquals("30", tester.expectDone());
    }

    @Test
    public void testEval() {
        try (DebuggerSession session = startSession()) {
            startEval(createTestTypes());

            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                assertEquals(32, frame.getSourceSection().getStartLine());

                // Evaluate var values
                DebugValue a2 = frame.eval("a2");
                Assert.assertTrue(a2.isArray());
                assertEquals("(3)[1, 2, [3, 4]]", a2.toDisplayString());
                DebugValue b2 = frame.eval("b2");
                Assert.assertFalse(b2.asBoolean());
                DebugValue i2 = frame.eval("i2");
                assertEquals(42.42, i2.asDouble(), 1e-10);
                DebugValue f1 = frame.eval("f1");
                Assert.assertTrue(f1.canExecute());
                DebugValue o3 = frame.eval("o3");
                assertEquals("{a: \"A\"}", o3.toDisplayString());
                assertEquals("A", o3.getProperty("a").toDisplayString());
                Assert.assertNull(o3.getProperty("none"));

                // Evaluate expressions
                assertEquals(2, frame.eval("a2[1]").asInt());
                assertEquals(52, frame.eval("10 + i1").asInt());
                assertEquals(69, frame.eval("f1(2) + o3.a.charCodeAt(0)").asInt());
                DebugValue f = frame.eval("(function f(x) {" +
                                "  let s = 0;" +
                                "  for (let i = 1; i <= x; i++) {" +
                                "    s += i;" +
                                "  }" +
                                "  return s;" +
                                "})(5)");
                assertEquals(15, f.asInt());

                // Evaluate creation of a global variable
                DebugValue gv = frame.eval("gv = 10");
                assertEquals(10, gv.asInt());
                gv = event.getSession().getTopScope("js").getParent().getDeclaredValue("gv");
                assertEquals(10, gv.asInt());
            });
        }
    }

    @Test
    public void testVarDeclInGlobalScope() {
        try (DebuggerSession session = startSession()) {
            startEval(Source.create("js", "" +
                            "debugger;\n" +
                            "gv *= 2;\n" +
                            "debugger;\n"));

            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();

                // Evaluate global var declaration in global context.
                frame.eval("var gv = 10;");
                DebugValue gv = frame.eval("gv");
                assertEquals(10, gv.asInt());
                gv = event.getSession().getTopScope("js").getParent().getDeclaredValue("gv");
                assertEquals(10, gv.asInt());
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();

                DebugValue gv = frame.eval("gv");
                assertEquals(20, gv.asInt());
                gv = event.getSession().getTopScope("js").getParent().getDeclaredValue("gv");
                assertEquals(20, gv.asInt());
                event.prepareContinue();
            });
            expectDone();
        }
    }

    @Test
    public void testVarDeclInLocalScope() {
        try (DebuggerSession session = startSession()) {
            startEval(createTestDebuggerStmt());

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testDebuggerStmt", 4, true, "debugger;", "n", "1");
                DebugStackFrame frame = event.getTopStackFrame();

                // Evaluate global var declaration in function context.
                frame.eval("var gv = 10;");

                DebugValue gv = frame.eval("gv");
                assertEquals(10, gv.asInt());

                gv = event.getSession().getTopScope("js").getParent().getDeclaredValue("gv");
                assertEquals(10, gv.asInt());
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testDebuggerStmt", 4, true, "debugger;", "n", "0");
                DebugStackFrame frame = event.getTopStackFrame();

                DebugValue gv = frame.eval("gv");
                assertEquals(10, gv.asInt());
                event.prepareContinue();
            });
            expectDone();
        }
    }

    @Test
    public void testFunctionDeclInGlobalScope() {
        try (DebuggerSession session = startSession()) {
            startEval(Source.create("js", "" +
                            "var gv = 10;\n" +
                            "debugger;\n" +
                            "debugger;\n"));

            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();

                DebugValue gv = frame.eval("gv");
                assertEquals(10, gv.asInt());

                frame.eval("function gf(n){return gv *= n;}");

                DebugValue gf = frame.eval("gf(2)");
                assertEquals(20, gf.asInt());
                gv = frame.eval("gv");
                assertEquals(20, gv.asInt());

                gf = event.getSession().getTopScope("js").getParent().getDeclaredValue("gf");
                assertNotNull(gf);
                assertTrue(gf.canExecute());
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();

                DebugValue gf = frame.eval("gf(2)");
                assertEquals(40, gf.asInt());
                DebugValue gv = frame.eval("gv");
                assertEquals(40, gv.asInt());
                event.prepareContinue();
            });
            expectDone();
        }
    }

    @Test
    public void testFunctionDeclInLocalScope() {
        try (DebuggerSession session = startSession()) {
            startEval(createTestDebuggerStmt());

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testDebuggerStmt", 4, true, "debugger;", "n", "1");
                DebugStackFrame frame = event.getTopStackFrame();

                frame.eval("function gf(){return 42 + n;}");

                DebugValue gf = frame.eval("gf()");
                assertEquals(43, gf.asInt());

                gf = event.getSession().getTopScope("js").getParent().getDeclaredValue("gf");
                assertNotNull(gf);
                assertTrue(gf.canExecute());
                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "testDebuggerStmt", 4, true, "debugger;", "n", "0");
                DebugStackFrame frame = event.getTopStackFrame();

                DebugValue gf = frame.eval("gf()");
                assertEquals(42, gf.asInt());
                event.prepareContinue();
            });
            expectDone();
        }
    }

    @Test
    public void testBreakpointEverywhereBreaks() throws Throwable {
        final String sourceCode = "/* Test */\n" +
                        "\n/* A comment */\n" +
                        " var x = 1;\n" +
                        " s = (f = function\n(\n)\n {\n" +
                        "   let\n a\n =\n {};\n" +
                        "   var b = [];\n" +
                        "   a.\nc = b;\n" +
                        "   b\n[\n3\n]\n \n=\n 4;\n" +
                        "   var\n res\n = \n0\n;\n" +
                        "   let n = \nb\n.\nlength\n;\n" +
                        "   for (var i = 0 ; i < n ; i++) {\n" +
                        "     res = res + 1;\n" +
                        "   }\n" +
                        "   return res;\n" +
                        " })();\n" +
                        " function ff(nn) {\n" +
                        "   let ret;\n" +
                        "   if (nn > 0) {\n" +
                        "     ret = ff(nn - 2);\n" +
                        "   } else {\n" +
                        "     nn = Math.sin(nn);\n" +
                        "   }\n" +
                        "   \n" +
                        "   return ret * nn;\n" +
                        " }\n" +
                        " ff(s);\n" +
                        " //\n";
        Source source = Source.newBuilder("js", sourceCode, "testBreakpointsAnywhere.js").build();
        tester.assertBreakpointsBreakEverywhere(source);
    }

    @Test
    public void testMisplacedLineBreakpoints() throws Throwable {
        final String source = "// A comment\n" +                  // 1
                        "function invocable(n) {\n" +
                        "  R3_R25_if (n <= 1) {\n" +
                        "    R4_var fce = function() {\n" +
                        "      R5_one = 1;\n" +             // 5
                        "      R6-7_return one;\n" +
                        "    };\n" +
                        "    R8-9_return fce;\n" +
                        "  } else {\n" +
                        "    // A comment\n" +              // 10
                        "    \n" +
                        "        R10-14_var fce2\n" +
                        "             = \n" +
                        "               function() {\n" +
                        "      \n" +                        // 15
                        "      R15-18_one \n" +
                        "          = \n" +
                        "            2;\n" +
                        "      \n" +
                        "      R19-21_return one + 1;\n" +  // 20
                        "    };\n" +
                        "    R22-24_return fce2;\n" +
                        "    \n" +
                        "  }\n" +
                        "}\n" +                             // 25
                        "R1-2_R26-28_var res = invocable(1)() + invocable(2)();\n" +
                        "// return res;\n" +
                        "\n";

        tester.assertLineBreakpointsResolution(source, "R", "js");
    }

    @Test
    public void testMisplacedColumnBreakpoints() throws Throwable {
        // A source on a single line with BX_ and RX_ marks.
        // BX_ denotes a submitted breakpoint X at the given location
        // RX_ denotes a resolved breakpoint X at the given location
        String sourceString = "B0_ B1_function invB2_ocable(n) {" +
                        "B3_  R2-3_R23_if (n B4_<= 1) B5_ {B6_" +
                        "    R4-6_var fce = functionB7_() {B8_" +
                        "      R7-8_one = 1;B9_" +
                        "      R9_return one;" +
                        "    };B10_" +
                        "    B11_R10-12_return B12_fce;" +
                        "  } B13_else {B14_" +
                        "    /* A comment B15_*/" +
                        "    R13-15_var fce2 = function() {B16_" +
                        "      R16-18_one B17_ = B18_ 2;B19_" +
                        "      R19_R20_return one + 1;" +
                        "    B20_};B21_" +
                        "    R21_R22_return fce2;" +
                        "  B22_}" +
                        "B23_}B24_" +
                        "R0-1_R24_R25_var lazy = invoB25_cable;" +
                        "R26_R27_var res = lazy(1)() + lazy(2)();" +
                        "B26_/*return res;*/B27_ ";
        tester.assertColumnBreakpointsResolution(sourceString, "B", "R", "js");
    }

    @Test
    public void testGettersSettersSideEffects() throws Exception {
        try (DebuggerSession session = startSession()) {
            startEval(createTestGettersSetters());

            expectSuspended((SuspendedEvent event) -> {
                DebugStackFrame frame = event.getTopStackFrame();
                DebugScope scope = frame.getScope();
                DebugValue person = scope.getDeclaredValue("person");

                DebugValue firstName = person.getProperty("firstName");
                assertFalse(firstName.hasReadSideEffects());
                assertFalse(firstName.hasWriteSideEffects());
                DebugValue lastName = person.getProperty("lastName");
                assertFalse(lastName.hasReadSideEffects());
                assertFalse(lastName.hasWriteSideEffects());
                DebugValue numQueries = person.getProperty("numQueries");
                assertFalse(numQueries.hasReadSideEffects());
                assertFalse(numQueries.hasWriteSideEffects());
                DebugValue fullName = person.getProperty("fullName");
                assertTrue(fullName.hasReadSideEffects());
                assertTrue(fullName.hasWriteSideEffects());
                DebugValue justGet = person.getProperty("justGet");
                assertTrue(justGet.hasReadSideEffects());
                assertFalse(justGet.hasWriteSideEffects());
                assertTrue(justGet.isReadable());
                assertFalse(justGet.isWritable());
                DebugValue justSet = person.getProperty("justSet");
                assertFalse(justSet.hasReadSideEffects());
                assertTrue(justSet.hasWriteSideEffects());
                assertFalse(justSet.isReadable());
                assertTrue(justSet.isWritable());

                assertEquals(0, person.getProperty("numQueries").asInt());
                assertEquals("Jimmy Smith", person.getProperty("fullName").toDisplayString());
                assertEquals(1, person.getProperty("numQueries").asInt());

                person.getProperty("fullName").set(session.createPrimitiveValue("Jack Jones", null));
                assertEquals(1, person.getProperty("numQueries").asInt());
                assertEquals("Jack", person.getProperty("firstName").toDisplayString());
                assertEquals("Jones", person.getProperty("lastName").toDisplayString());
                assertEquals(1, person.getProperty("numQueries").asInt());
                assertEquals("Jack Jones", person.getProperty("fullName").toDisplayString());
                assertEquals(2, person.getProperty("numQueries").asInt());
            });
            expectDone();
        }
    }

    @Test
    public void testStepThroughGettersSetters() throws Exception {
        try (DebuggerSession session = startSession()) {
            startEval(createTestGettersSetters());

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "gsTest", 23, true, "debugger;", "person", "{firstName: \"Jimmy\", lastName: \"Smith\", numQueries: 0, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepInto(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "gsTest", 24, true, "person.justSet = person.fullName + \" \" + person.justGet",
                                "person", "{firstName: \"Jimmy\", lastName: \"Smith\", numQueries: 0, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepInto(1);
            });
            // In a getter
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "get fullName", 7, true, "this.numQueries++",
                                "this", "{firstName: \"Jimmy\", lastName: \"Smith\", numQueries: 0, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepOut(1);
            });
            // After the getter
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "gsTest", 24, 36, false, "person.fullName",
                                "person", "{firstName: \"Jimmy\", lastName: \"Smith\", numQueries: 1, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepInto(1);
            });
            // In a different getter
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "get justGet", 16, true, "this.numQueries++",
                                "this", "{firstName: \"Jimmy\", lastName: \"Smith\", numQueries: 1, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepOver(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "get justGet", 17, true, "return \"Get for \" + this.firstName;",
                                "this", "{firstName: \"Jimmy\", lastName: \"Smith\", numQueries: 2, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepOver(1);
            });
            // After the getter
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "gsTest", 24, 59, false, "person.justGet",
                                "person", "{firstName: \"Jimmy\", lastName: \"Smith\", numQueries: 2, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepInto(1);
            });
            // In a setter
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "set justSet", 20, true, "this.firstName = firstName",
                                "firstName", "Jimmy Smith Get for Jimmy",
                                "this", "{firstName: \"Jimmy\", lastName: \"Smith\", numQueries: 2, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepInto(1);
            });
            // After the setter
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "gsTest", 24, 59, false, "person.justSet = person.fullName + \" \" + person.justGet",
                                "person", "{firstName: \"Jimmy Smith Get for Jimmy\", lastName: \"Smith\", numQueries: 2, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepInto(1);
            });
            // On a next line
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "gsTest", 25, 5, true, "person.fullName = person.justGet.length > 0 ? person.firstName : \"\"",
                                "person", "{firstName: \"Jimmy Smith Get for Jimmy\", lastName: \"Smith\", numQueries: 2, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepOver(1);
            });
            // Step over skips getter and setter
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "gsTest", 26, 5, true, "return person.fullName;",
                                "person", "{firstName: \"Jimmy\", lastName: \"Smith\", numQueries: 3, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepInto(1);
            });
            // In a getter
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "get fullName", 7, true, "this.numQueries++",
                                "this", "{firstName: \"Jimmy\", lastName: \"Smith\", numQueries: 3, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepInto(1);
            });
            // Still in the getter
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "get fullName", 8, true, "return this.firstName + ' ' + this.lastName;",
                                "this", "{firstName: \"Jimmy\", lastName: \"Smith\", numQueries: 4, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepOver(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "gsTest", 26, 26, false, "person.fullName",
                                "person", "{firstName: \"Jimmy\", lastName: \"Smith\", numQueries: 4, fullName: accessor, justGet: accessor, justSet: accessor}");
                event.prepareStepOver(1);
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, ":program", 27, 9, false, "gsTest()");
                event.prepareStepOver(1);
            });

            expectDone();
        }
    }

    @Test
    public void testAsynchronousStacks() {
        final Source source = Source.newBuilder("js", "" +
                        "async function one(x) {\n" +
                        "  return await Promise.all([two(x), one_and_a_half(x)]);\n" +
                        "}\n" +
                        "async function one_and_a_half(x) {\n" +
                        "  return await two(x);\n" +
                        "}\n" +
                        "async function two(x) {\n" +
                        "  x = await x;\n" +
                        "  debugger;\n" +
                        "}\n" +
                        "(function start(){\n" +
                        "  one(Promise.resolve(42)).catch(e => {throw e;});\n" +
                        "})();\n", "testAsynchronousStacks.js").buildLiteral();

        try (DebuggerSession session = startSession()) {
            startEval(source);

            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "two", 9, true, "debugger;", "x", "42");

                assertEquals(1, event.getAsynchronousStacks().size());
                List<DebugStackTraceElement> asynchronousStack = event.getAsynchronousStacks().get(0);
                assertEquals(Arrays.asList("Promise.all", "one"), asynchronousStack.stream().map(DebugStackTraceElement::getName).collect(Collectors.toList()));

                event.prepareContinue();
            });
            expectSuspended((SuspendedEvent event) -> {
                checkState(event, "two", 9, true, "debugger;", "x", "42");

                assertEquals(1, event.getAsynchronousStacks().size());
                List<DebugStackTraceElement> asynchronousStack = event.getAsynchronousStacks().get(0);
                assertEquals(Arrays.asList("one_and_a_half", "Promise.all", "one"), asynchronousStack.stream().map(DebugStackTraceElement::getName).collect(Collectors.toList()));

                event.prepareContinue();
            });

            expectDone();
        }
    }

}
