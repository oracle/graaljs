/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Instrument;
import org.graalvm.polyglot.Source;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;

public class ScopeInstrumentTest {

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
    public void asyncFindScopes() {
        asyncFindScopes("Promise.resolve(42).then(async function foo (x) {});", "foo");
    }

    @Test
    public void anonAsyncFindScopes() {
        asyncFindScopes("Promise.resolve(42).then(async (x) => {});", ":=>");
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
            Scope scope = findFirstLocalScope(env, node, frame);
            int line = node.getSourceSection().getStartLine();
            assertEquals("Function name", "testParams", scope.getName());
            assertEquals("Line = " + line, 2, line);

            Object vars = scope.getVariables();
            int varCount = getSize(getKeys(vars));
            assertEquals("Line = " + line + ", num vars:", 4, varCount);
            assertTrue("Param a", INTEROP.isMemberReadable(vars, "a"));
            assertTrue("Param b", INTEROP.isMemberReadable(vars, "b"));
            assertTrue("Param c", INTEROP.isMemberReadable(vars, "c"));
            assertTrue("Param d", INTEROP.isMemberReadable(vars, "d"));
            assertEquals("Param a", 4, INTEROP.readMember(vars, "a"));
            assertEquals("Param b", 7, INTEROP.readMember(vars, "b"));
            assertEquals("Param c", 6, INTEROP.readMember(vars, "c"));
            assertEquals("Param d", 9, INTEROP.readMember(vars, "d"));
        });
        context.eval(source);
        TestJSScopeInstrument.instance.checkSuccess(1);
    }

    public void asyncFindScopes(String src, String expectedAsyncFunctionName) {
        Source source = Source.create(ID, src);
        TestJSScopeInstrument.filter = SourceSectionFilter.newBuilder().tagIs(StandardTags.RootBodyTag.class).build();
        ensureCreated(context.getEngine().getInstruments().get("TestJSScopeInstrument"));
        int[] scopes = new int[]{0, 0};
        TestJSScopeInstrument.instance.setTester((env, node, frame) -> {
            Scope scope = findFirstLocalScope(env, node, frame);
            if (expectedAsyncFunctionName.equals(scope.getName())) {
                Object vars = scope.getVariables();
                int varCount = getSize(getKeys(vars));
                int line = node.getSourceSection().getStartLine();
                assertEquals("Line = " + line + ", num vars:", 1, varCount);
                assertTrue("Param x", INTEROP.isMemberReadable(vars, "x"));
                assertEquals("Param x", 42, INTEROP.readMember(vars, "x"));
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

    private static final InteropLibrary INTEROP = InteropLibrary.getFactory().getUncached();

    private static Scope findFirstLocalScope(TruffleInstrument.Env env, Node node, Frame frame) {
        Iterable<Scope> lscopes = env.findLocalScopes(node, frame);
        assertNotNull(lscopes);
        Iterator<Scope> iterator = lscopes.iterator();
        assertTrue(iterator.hasNext());
        Scope lscope = iterator.next();
        assertFalse(iterator.hasNext());
        return lscope;
    }

    private static Object getKeys(Object object) throws UnsupportedMessageException {
        return INTEROP.getMembers(object);
    }

    private static int getSize(Object keys) throws UnsupportedMessageException {
        return (int) INTEROP.getArraySize(keys);
    }

    private static class DefaultScopeTester implements ScopeTester {

        @Override
        public void testScope(TruffleInstrument.Env env, Node node, VirtualFrame frame) throws Exception {
            Scope dynamicScope = findFirstLocalScope(env, node, frame);
            Scope lexicalScope = findFirstLocalScope(env, node, null);
            int line = node.getSourceSection().getStartLine();
            if (line > 1 && line < 6) {
                assertEquals("Line = " + line + ", function name: ", "testFunction", lexicalScope.getName());

                final int numVars = 2;
                final int setVars = Math.min(Math.max(0, line - 2), numVars);
                Object vars;
                int varCount;

                // Dynamic access:
                vars = dynamicScope.getVariables();
                varCount = getSize(getKeys(vars));
                assertEquals("Line = " + line + ", num vars:", numVars, varCount);
                assertTrue("Var a: ", INTEROP.isMemberReadable(vars, "a"));
                assertTrue("Var b: ", INTEROP.isMemberReadable(vars, "b"));
                if (setVars >= 1) {
                    assertEquals("Var a: ", 10, INTEROP.readMember(vars, "a"));
                }
                if (setVars >= 2) {
                    assertEquals("Var b: ", 20, INTEROP.readMember(vars, "b"));
                }

                // Lexical access:
                vars = lexicalScope.getVariables();
                varCount = getSize(getKeys(vars));
                assertEquals("Line = " + line + ", num vars:", numVars, varCount);
                assertTrue("Var a: ", INTEROP.isMemberReadable(vars, "a"));
                assertTrue("Var b: ", INTEROP.isMemberReadable(vars, "b"));
                if (setVars >= 1) {
                    assertTrue(INTEROP.isNull(INTEROP.readMember(vars, "a")));
                }
                if (setVars >= 2) {
                    assertTrue(INTEROP.isNull(INTEROP.readMember(vars, "b")));
                }
            }
            if (line == 6) {
                testGlobalScope(env);
            }
        }

        private static void testGlobalScope(TruffleInstrument.Env env) throws Exception {
            Iterable<Scope> topScopes = env.findTopScopes(ID);
            Iterator<Scope> iterator = topScopes.iterator();
            assertTrue(iterator.hasNext());
            iterator.next(); // skip lexical global scope
            assertTrue(iterator.hasNext());
            Scope globalScope = iterator.next();
            assertEquals("global", globalScope.getName());
            assertNull(globalScope.getNode());
            assertNull(globalScope.getArguments());
            Object variables = globalScope.getVariables();
            Object keys = getKeys(variables);
            assertNotNull(keys);
            assertTrue("number of keys >= 1", getSize(keys) >= 1);
            String functionName = "testFunction";
            assertTrue(hasValue(keys, "testFunction"));
            Object function = INTEROP.readMember(variables, functionName);
            assertTrue(INTEROP.isExecutable(function));
        }

        private static boolean hasValue(Object object, String key) throws UnsupportedMessageException, InvalidArrayIndexException {
            int size = getSize(object);
            for (int i = 0; i < size; i++) {
                Object read = INTEROP.readArrayElement(object, i);
                if (read instanceof String && key.equals(read)) {
                    return true;
                }
            }
            return false;
        }
    }
}
