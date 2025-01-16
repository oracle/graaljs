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
package com.oracle.truffle.js.test.instrumentation;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Instrument;
import org.graalvm.polyglot.Source;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.NodeLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;

public class ScopeInstrumentTest {

    private static final InteropLibrary INTEROP = InteropLibrary.getUncached();
    private static final NodeLibrary NODE_LIBRARY = NodeLibrary.getUncached();

    protected Context context;

    @Before
    public void setup() {
        context = TestUtil.newContextBuilder().build();
    }

    @After
    public void tearDown() {
        context.close();
    }

    private static void ensureCreated(Instrument instrument) {
        Assert.assertNotNull("Instrument not found", instrument);
        instrument.lookup(Object.class);
        Assert.assertNotNull("Instrument not created", TestJSScopeInstrument.instance);
    }

    @Test
    public void testDefaultScope() throws Throwable {
        Source source = Source.create(ID, "" +
                        "function testFunction() {\n" +
                        "  var a = 10;\n" +
                        "  var b = 20;\n" +
                        "  return a + b;\n" +
                        "}\n" +
                        "testFunction();\n");

        TestJSScopeInstrument.filter = SourceSectionFilter.newBuilder().tagIs(StandardTags.StatementTag.class).build();
        ensureCreated(context.getEngine().getInstruments().get("TestJSScopeInstrument"));
        TestJSScopeInstrument.instance.setTester(new DefaultScopeTester());
        context.eval(source);
        TestJSScopeInstrument.instance.checkSuccess(4);
    }

    @Test
    public void testParams() throws Throwable {
        Source source = Source.create(ID, "" +
                        "function testParams(a, [b, c], d = 9) {\n" +
                        "   return a + b + c + d;\n" +
                        "}\n" +
                        "testParams(4, [7, 6, 2]);\n");

        TestJSScopeInstrument.filter = SourceSectionFilter.newBuilder().tagIs(StandardTags.RootBodyTag.class).rootNameIs("testParams"::equals).build();
        ensureCreated(context.getEngine().getInstruments().get("TestJSScopeInstrument"));
        TestJSScopeInstrument.instance.setTester((env, node, frame) -> {
            Object scope = findLocalScope(node, frame);
            int line = node.getSourceSection().getStartLine();
            assertEquals("Function name", "testParams", getScopeName(scope));
            assertEquals("Line = " + line, 2, line);

            List<String> keyList = getKeyList(scope);
            keyList.remove("this");
            int varCount = keyList.size();
            assertEquals("Line = " + line + ", num vars:", 4, varCount);
            assertTrue("Param a", INTEROP.isMemberReadable(scope, "a"));
            assertTrue("Param b", INTEROP.isMemberReadable(scope, "b"));
            assertTrue("Param c", INTEROP.isMemberReadable(scope, "c"));
            assertTrue("Param d", INTEROP.isMemberReadable(scope, "d"));
            assertEquals("Param a", 4, INTEROP.readMember(scope, "a"));
            assertEquals("Param b", 7, INTEROP.readMember(scope, "b"));
            assertEquals("Param c", 6, INTEROP.readMember(scope, "c"));
            assertEquals("Param d", 9, INTEROP.readMember(scope, "d"));
        });
        context.eval(source);
        TestJSScopeInstrument.instance.checkSuccess(1);
    }

    @Test
    public void testParamsSourceLocation() throws Throwable {
        Source source = Source.create(ID, "" +
                        "function testParams(a, [b, c], d = 9) {\n" +
                        "   return a + b + c + d;\n" +
                        "}\n" +
                        "testParams(4, [7, 6, 2]);\n");

        TestJSScopeInstrument.filter = SourceSectionFilter.newBuilder().tagIs(StandardTags.RootBodyTag.class).rootNameIs("testParams"::equals).build();
        ensureCreated(context.getEngine().getInstruments().get("TestJSScopeInstrument"));
        TestJSScopeInstrument.instance.setTester((env, node, frame) -> {
            Object scope = findLocalScope(node, frame);
            int line = node.getSourceSection().getStartLine();
            assertEquals("Function name", "testParams", getScopeName(scope));
            assertEquals("Line = " + line, 2, line);

            Object keys = getKeys(scope);
            int nKeys = getSize(keys);
            for (int i = 0; i < nKeys; i++) {
                Object key = INTEROP.readArrayElement(keys, i);
                assertTrue(INTEROP.isString(key));
                String keyAsString = INTEROP.asString(key);
                assertTrue(keyAsString, INTEROP.hasSourceLocation(key));
                assertTrue(keyAsString, INTEROP.getSourceLocation(key).isAvailable());
                assertEquals(keyAsString, 1, INTEROP.getSourceLocation(key).getStartLine());
            }
        });
        context.eval(source);
        TestJSScopeInstrument.instance.checkSuccess(1);
    }

    @Test
    public void testVariableSourceLocation() throws Throwable {
        Source source = Source.create(ID, "" +
                        "function testFunction() {\n" +
                        "  var a = 10;\n" +
                        "  let b = 20;\n" +
                        "  const c = 30\n" +
                        "  return a + b + c;\n" +
                        "}\n" +
                        "testFunction();\n");

        TestJSScopeInstrument.filter = SourceSectionFilter.newBuilder().tagIs(StandardTags.RootBodyTag.class).rootNameIs("testFunction"::equals).build();
        ensureCreated(context.getEngine().getInstruments().get("TestJSScopeInstrument"));
        TestJSScopeInstrument.instance.setTester((env, node, frame) -> {
            Object scope = findLocalScope(node, frame);
            int line = node.getSourceSection().getStartLine();
            assertEquals("Function name", "testFunction", getScopeName(scope));
            assertEquals("Line = " + line, 1, line);

            Object keys = getKeys(scope);
            int nKeys = getSize(keys);
            for (int i = 0; i < nKeys; i++) {
                Object key = INTEROP.readArrayElement(keys, i);
                assertTrue(INTEROP.isString(key));
                String keyAsString = INTEROP.asString(key);

                if (NODE_LIBRARY.hasReceiverMember(node, frame)) {
                    if (INTEROP.asString(NODE_LIBRARY.getReceiverMember(node, frame)).equals(keyAsString)) {
                        continue;
                    }
                }

                assertTrue(keyAsString, INTEROP.hasSourceLocation(key));
                assertTrue(keyAsString, INTEROP.getSourceLocation(key).isAvailable());
                assertEquals(keyAsString, 2 + i, INTEROP.getSourceLocation(key).getStartLine());
            }
        });
        context.eval(source);
        TestJSScopeInstrument.instance.checkSuccess(1);
    }

    @Test
    public void asyncFindScopes() {
        asyncFindScopes("Promise.resolve(42).then(async function foo (x) {});", "foo");
    }

    @Test
    public void anonAsyncFindScopes() {
        asyncFindScopes("Promise.resolve(42).then(async (x) => {});", ":=>");
    }

    private void asyncFindScopes(String src, String expectedAsyncFunctionName) {
        Source source = Source.create(ID, src);
        TestJSScopeInstrument.filter = SourceSectionFilter.newBuilder().tagIs(StandardTags.RootBodyTag.class).build();
        ensureCreated(context.getEngine().getInstruments().get("TestJSScopeInstrument"));
        int[] scopes = new int[]{0, 0};
        TestJSScopeInstrument.instance.setTester((env, node, frame) -> {
            Object scope = findLocalScope(node, frame);
            String scopeName = getScopeName(scope);
            if (expectedAsyncFunctionName.equals(scopeName)) {
                List<String> keyList = getKeyList(scope);
                keyList.remove("this");
                int varCount = keyList.size();
                int line = node.getSourceSection().getStartLine();
                assertEquals("Line = " + line + ", num vars: " + varCount + " " + keyList, 1, varCount);
                assertTrue("Param x", INTEROP.isMemberReadable(scope, "x"));
                assertEquals("Param x", 42, INTEROP.readMember(scope, "x"));
                scopes[1] = 1;
            }
            scopes[0]++;
        });
        context.eval(source);
        assertEquals("Async function scope inspected", scopes[1], 1);
        assertEquals("All scopes have been entered", 9, scopes[0]);
    }

    @TruffleInstrument.Registration(id = "TestJSScopeInstrument", services = Object.class)
    public static class TestJSScopeInstrument extends TruffleInstrument {

        static SourceSectionFilter filter;
        static TestJSScopeInstrument instance;

        private ScopeTester tester;
        private int count;
        private Exception failure;

        @Override
        protected void onCreate(TruffleInstrument.Env env) {
            instance = this;
            assertNotNull("SourceSectionFilter", filter);
            env.getInstrumenter().attachExecutionEventListener(filter, new ExecutionEventListener() {
                @Override
                public void onEnter(EventContext context, VirtualFrame frame) {
                    count++;
                    try {
                        tester.testScope(env, context.getInstrumentedNode(), frame);
                    } catch (Exception t) {
                        failure = t;
                    }
                }

                @Override
                public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
                }

                @Override
                public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
                }
            });
            env.registerService(new Object());
        }

        void setTester(ScopeTester tester) {
            this.count = 0;
            this.tester = tester;
        }

        void checkSuccess(int expectedEventCount) throws Exception {
            reset();
            assertTrue("Scope instrument not triggered", count > 0);
            if (failure != null) {
                throw failure;
            }
            assertEquals("Number of tested statements", expectedEventCount, count);
        }

        private void reset() {
            tester = null;
            instance = null;
            filter = null;
        }
    }

    interface ScopeTester {
        void testScope(TruffleInstrument.Env env, Node node, VirtualFrame frame) throws Exception;
    }

    private static Object findLocalScope(Node node, VirtualFrame frame) throws UnsupportedMessageException {
        assertTrue(NODE_LIBRARY.hasScope(node, frame));
        return NODE_LIBRARY.getScope(node, frame, true);
    }

    private static String getScopeName(Object scope) throws UnsupportedMessageException {
        return INTEROP.asString(INTEROP.toDisplayString(scope));
    }

    private static Object getKeys(Object object) throws UnsupportedMessageException {
        return INTEROP.getMembers(object);
    }

    private static int getSize(Object keys) throws UnsupportedMessageException {
        return (int) INTEROP.getArraySize(keys);
    }

    private static List<String> getKeyList(Object membersObj) throws InteropException {
        Object keys = getKeys(membersObj);
        int len = getSize(keys);
        List<String> list = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            list.add(INTEROP.asString(INTEROP.readArrayElement(keys, i)));
        }
        return list;
    }

    private static final class DefaultScopeTester implements ScopeTester {

        @Override
        public void testScope(TruffleInstrument.Env env, Node node, VirtualFrame frame) throws Exception {
            Object dynamicScope = findLocalScope(node, frame);
            Object lexicalScope = findLocalScope(node, null);
            int line = node.getSourceSection().getStartLine();
            if (line > 1 && line < 6) {
                assertEquals("Line = " + line + ", function name: ", "testFunction", getScopeName(lexicalScope));

                final int numVars = 2;
                final int setVars = Math.min(Math.max(0, line - 2), numVars);
                int varCount;
                List<String> keyList;

                // Dynamic access:
                keyList = getKeyList(dynamicScope);
                keyList.remove("this");
                varCount = keyList.size();

                assertEquals("Line = " + line + ", num vars:", numVars, varCount);
                assertTrue("Var a: ", INTEROP.isMemberReadable(dynamicScope, "a"));
                assertTrue("Var b: ", INTEROP.isMemberReadable(dynamicScope, "b"));
                if (setVars >= 1) {
                    assertEquals("Var a: ", 10, INTEROP.readMember(dynamicScope, "a"));
                }
                if (setVars >= 2) {
                    assertEquals("Var b: ", 20, INTEROP.readMember(dynamicScope, "b"));
                }

                // Lexical access:
                keyList = getKeyList(lexicalScope);
                keyList.remove("this");
                varCount = keyList.size();

                assertEquals("Line = " + line + ", num vars:", numVars, varCount);
                assertTrue("Var a: ", INTEROP.isMemberReadable(lexicalScope, "a"));
                assertTrue("Var b: ", INTEROP.isMemberReadable(lexicalScope, "b"));
                if (setVars >= 1) {
                    assertTrue(INTEROP.isNull(INTEROP.readMember(lexicalScope, "a")));
                }
                if (setVars >= 2) {
                    assertTrue(INTEROP.isNull(INTEROP.readMember(lexicalScope, "b")));
                }
            }
            if (line == 6) {
                testGlobalScope(env);
            }
        }

        private static void testGlobalScope(TruffleInstrument.Env env) throws Exception {
            Object globalScope = env.getScope(env.getLanguages().get(ID));
            assertEquals("global", getScopeName(globalScope));
            Object keys = getKeys(globalScope);
            assertNotNull(keys);
            assertTrue("number of keys >= 1", getSize(keys) >= 1);
            String functionName = "testFunction";
            assertTrue(hasValue(keys, "testFunction"));
            Object function = INTEROP.readMember(globalScope, functionName);
            assertTrue(INTEROP.isExecutable(function));
        }

        private static boolean hasValue(Object object, String key) throws UnsupportedMessageException, InvalidArrayIndexException {
            int size = getSize(object);
            for (int i = 0; i < size; i++) {
                Object read = INTEROP.readArrayElement(object, i);
                if (INTEROP.isString(read) && key.equals(INTEROP.asString(read))) {
                    return true;
                }
            }
            return false;
        }
    }
}
