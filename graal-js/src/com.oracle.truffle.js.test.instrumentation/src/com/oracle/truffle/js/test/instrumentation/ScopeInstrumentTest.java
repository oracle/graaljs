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
package com.oracle.truffle.js.test.instrumentation;

import static com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Instrument;
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
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;

public class ScopeInstrumentTest {

    protected Context context;

    @Before
    public void setup() {
        context = Context.create(ID);
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
        ensureCreated(context.getEngine().getInstruments().get("TestJSScopeInstrument"));
        TestJSScopeInstrument.instance.setTester(new DefaultScopeTester());
        context.eval(ID, "" +
                        "function testFunction() {\n" +
                        "  var a = 10;\n" +
                        "  var b = 20;\n" +
                        "  return a + b;\n" +
                        "}\n" +
                        "testFunction();\n");
        TestJSScopeInstrument.instance.checkSuccess(4);
    }

    @TruffleInstrument.Registration(id = "TestJSScopeInstrument", services = Object.class)
    public static class TestJSScopeInstrument extends TruffleInstrument {

        static TestJSScopeInstrument instance;

        private ScopeTester tester;
        private int count;
        private Exception failure;

        @Override
        protected void onCreate(TruffleInstrument.Env env) {
            instance = this;
            SourceSectionFilter statements = SourceSectionFilter.newBuilder().tagIs(StandardTags.StatementTag.class).build();
            env.getInstrumenter().attachExecutionEventListener(statements, new ExecutionEventListener() {
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
            tester = null;
            assertTrue("Scope instrument not triggered", count > 0);
            if (failure != null) {
                throw failure;
            }
            assertEquals("Number of tested statements", expectedEventCount, count);
        }
    }

    interface ScopeTester {
        void testScope(TruffleInstrument.Env env, Node node, VirtualFrame frame) throws Exception;
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
                TruffleObject vars;
                int varCount;

                // Dynamic access:
                vars = (TruffleObject) dynamicScope.getVariables();
                varCount = getSize(getKeys(vars));
                assertEquals("Line = " + line + ", num vars:", numVars, varCount);
                assertTrue("Var a: ", hasKey(vars, "a"));
                assertTrue("Var b: ", hasKey(vars, "b"));
                if (setVars >= 1) {
                    assertEquals("Var a: ", 10, read(vars, "a"));
                }
                if (setVars >= 2) {
                    assertEquals("Var b: ", 20, read(vars, "b"));
                }

                // Lexical access:
                vars = (TruffleObject) lexicalScope.getVariables();
                varCount = getSize(getKeys(vars));
                assertEquals("Line = " + line + ", num vars:", numVars, varCount);
                assertTrue("Var a: ", hasKey(vars, "a"));
                assertTrue("Var b: ", hasKey(vars, "b"));
                if (setVars >= 1) {
                    assertTrue(isNull(read(vars, "a")));
                }
                if (setVars >= 2) {
                    assertTrue(isNull(read(vars, "b")));
                }
            }
            if (line == 6) {
                testGlobalScope(env);
            }
        }

        private static Scope findFirstLocalScope(TruffleInstrument.Env env, Node node, Frame frame) {
            Iterable<Scope> lscopes = env.findLocalScopes(node, frame);
            assertNotNull(lscopes);
            Iterator<Scope> iterator = lscopes.iterator();
            assertTrue(iterator.hasNext());
            Scope lscope = iterator.next();
            assertFalse(iterator.hasNext());
            return lscope;
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
            TruffleObject variables = (TruffleObject) globalScope.getVariables();
            TruffleObject keys = getKeys(variables);
            assertNotNull(keys);
            assertTrue("number of keys >= 1", getSize(keys) >= 1);
            String functionName = "testFunction";
            assertTrue(hasValue(keys, "testFunction"));
            TruffleObject function = (TruffleObject) read(variables, functionName);
            assertTrue(ForeignAccess.sendIsExecutable(Message.IS_EXECUTABLE.createNode(), function));
        }

        private static TruffleObject getKeys(TruffleObject object) throws UnsupportedMessageException {
            return ForeignAccess.sendKeys(Message.KEYS.createNode(), object);
        }

        private static int getSize(TruffleObject keys) throws UnsupportedMessageException {
            Number size = (Number) ForeignAccess.sendGetSize(Message.GET_SIZE.createNode(), keys);
            return size.intValue();
        }

        private static boolean hasValue(TruffleObject object, String key) throws UnsupportedMessageException, UnknownIdentifierException {
            int size = getSize(object);
            for (int i = 0; i < size; i++) {
                Object read = read(object, i);
                if (read instanceof String && key.equals(read)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean hasKey(TruffleObject object, String key) {
            int keyInfo = ForeignAccess.sendKeyInfo(Message.KEY_INFO.createNode(), object, key);
            return KeyInfo.isReadable(keyInfo);
        }

        private static Object read(TruffleObject object, Object key) throws UnknownIdentifierException, UnsupportedMessageException {
            return ForeignAccess.sendRead(Message.READ.createNode(), object, key);
        }

        private static boolean isNull(Object object) {
            return ForeignAccess.sendIsNull(Message.IS_NULL.createNode(), (TruffleObject) object);
        }
    }
}
