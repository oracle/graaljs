/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.interop;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.test.JSTest;

/**
 * Various tests for accessing JavaScript Error in Java and accessing Java Exception in JavaScript.
 */
public class ExceptionInteropTest {

    public static class ToBePassedToJS {
        private final Engine engine;
        private final HostAccess hostAccess;
        private Exception exception;
        private String exceptionName;
        private String exceptionMessage;
        private Map<?, ?> map;
        private GraalJSException.JSStackTraceElement[] jsStackTrace;

        public ToBePassedToJS(Engine engine, HostAccess hostAccess) {
            this.engine = engine;
            this.hostAccess = hostAccess;
        }

        @HostAccess.Export
        public void methodThatThrowsJSException() throws IOException {
            // Host access must be the same for all contexts using the same engine
            try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).engine(engine).build()) {
                context.eval(Source.newBuilder(ID, "\nisNotDefined.doesNotMakeSense = 1;\n", "nestedisnotdefined.js").build());
                fail("PolyglotException not thrown");
            }
        }

        @HostAccess.Export
        public void methodThatThrowsException() {
            this.jsStackTrace = JSException.getJSStackTrace(null);
            throw new UnsupportedOperationException("This operation is not supported!");
        }

        @HostAccess.Export
        public void methodThatThrowsNestedJavaException() throws IOException {
            // Host access must be the same for all contexts using the same engine
            try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).engine(engine).build()) {
                Value bindings = context.getBindings(ID);
                bindings.putMember("objectFromJava", this);
                context.eval(Source.newBuilder(ID, "\nobjectFromJava.methodThatThrowsException();\n",
                                "nestedhostexception.js").build());
            }
        }

        @HostAccess.Export
        public void methodWithExceptionArgument(Exception argException, String name, String message) {
            this.exception = argException;
            this.exceptionName = name;
            this.exceptionMessage = message;
        }

        @HostAccess.Export
        public void methodWithMapArgument(Map<?, ?> argMap) {
            this.map = argMap;
        }
    }

    private static int EXCEPTION_LINE_NUMBER;

    @BeforeClass
    public static void initializeExceptionLineNumber() {
        HostAccess hostAccess = HostAccess.newBuilder().allowAccessAnnotatedBy(HostAccess.Export.class).build();
        int lineNumber = -1;
        try (Engine graalEngine = JSTest.newEngineBuilder().build(); Context context = JSTest.newContextBuilder().engine(graalEngine).allowHostAccess(hostAccess).build()) {
            context.enter();
            context.initialize(ID);
            ToBePassedToJS toBePassedToJS = new ToBePassedToJS(null, null);
            try {
                // This needs Context to be created already
                toBePassedToJS.methodThatThrowsException();
            } catch (UnsupportedOperationException e) {
                lineNumber = e.getStackTrace()[0].getLineNumber();
            }
            context.leave();
        } finally {
            EXCEPTION_LINE_NUMBER = lineNumber;
        }
    }

    /**
     * Test that JavaScript Error can be caught and accessed in Java.
     */
    @Test
    public void testJSException() throws IOException {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.eval(Source.newBuilder(ID, "\nisNotDefined.doesNotMakeSense = 1;\n", "isnotdefined.js").build());
            fail("PolyglotException not thrown");
        } catch (PolyglotException e) {
            assertTrue(e.isGuestException());
            assertEquals("ReferenceError: isNotDefined is not defined", e.getMessage());
            StackTraceElement[] stackTrace = e.getStackTrace();
            assertEquals("isnotdefined.js", stackTrace[0].getFileName());
            assertEquals(2, stackTrace[0].getLineNumber());
        }
    }

    /**
     * Test that a "nested" JavaScript Error thrown from a nested JavaScript execution called from a
     * Java method called in JavaScript can be caught and accessed in Java.
     */
    @Test
    public void testNestedJSException() throws IOException {
        final HostAccess hostAccess = HostAccess.newBuilder().allowAccessAnnotatedBy(HostAccess.Export.class).build();
        try (Engine graalEngine = JSTest.newEngineBuilder().build(); Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).engine(graalEngine).build()) {
            Value bindings = context.getBindings(ID);
            ToBePassedToJS objectFromJava = new ToBePassedToJS(graalEngine, hostAccess);
            bindings.putMember("objectFromJava", objectFromJava);
            context.eval(Source.newBuilder(ID, "\nobjectFromJava.methodThatThrowsJSException();\n", "testmain.js").build());
            fail("PolyglotException not thrown");
        } catch (PolyglotException e) {
            assertTrue(e.isHostException());
            assertTrue(e.asHostException() instanceof PolyglotException);
            assertTrue(((PolyglotException) e.asHostException()).isGuestException());
            assertEquals("ReferenceError: isNotDefined is not defined", e.getMessage());
            StackTraceElement[] stackTrace = Arrays.stream(e.getStackTrace()).filter(ste -> ste.getClassName().equals("<js>")).toArray(StackTraceElement[]::new);
            assertEquals("nestedisnotdefined.js", stackTrace[0].getFileName());
            assertEquals(2, stackTrace[0].getLineNumber());
            assertEquals("testmain.js", stackTrace[1].getFileName());
            assertEquals(2, stackTrace[1].getLineNumber());
        }
    }

    /**
     * Test that Java Error caught in JavaScript can be passed to Java and its properties accessed.
     */
    @Test
    @SuppressWarnings("RedundantOperationOnEmptyContainer")
    public void testCaughtJSException() throws IOException {
        final HostAccess hostAccess = HostAccess.newBuilder().allowAccessAnnotatedBy(HostAccess.Export.class).build();
        try (Engine graalEngine = JSTest.newEngineBuilder().build(); Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).engine(graalEngine).build()) {
            Value bindings = context.getBindings(ID);
            ToBePassedToJS objectFromJava = new ToBePassedToJS(graalEngine, hostAccess);
            bindings.putMember("objectFromJava", objectFromJava);
            Value val = context.eval(Source.newBuilder(ID, "var ex;\n" +
                            "try {\n" +
                            "    isNotDefined.doesNotMakeSense = 1;\n" +
                            "} catch (e) {\n" +
                            "    objectFromJava.methodWithMapArgument(e);\n" +
                            "    ex = e;" +
                            "}\n" +
                            "ex;\n", "isnotdefinedcaught.js").build());
            // The returned value is a JS Error, so hasMembers returns true, which is essentially
            // telling you whether the object can have properies, meaning that it is a JS Object.
            // However, in this case, all properties are inherited from the Error prototype, not
            // own properties of the object, that is why getMemberKeys returns empty set, even
            // though getMember("name") is not empty, for instance.
            assertTrue(val.hasMembers());
            assertTrue(val.getMemberKeys().isEmpty());
            assertTrue(val.getMember("name").isString());
            assertEquals("ReferenceError", val.getMember("name").asString());
            assertTrue(val.getMember("message").isString());
            assertEquals("isNotDefined is not defined", val.getMember("message").asString());
            assertTrue(val.getMember("stack").isString());
            assertEquals("ReferenceError: isNotDefined is not defined\n" +
                            "    at isnotdefinedcaught.js:3:5", val.getMember("stack").asString());

            // For the same reason as above, the keySet is empty even though for example get("name")
            // is not empty.
            assertTrue(objectFromJava.map.keySet().isEmpty());
            assertEquals("ReferenceError", objectFromJava.map.get("name"));
            assertEquals("isNotDefined is not defined", objectFromJava.map.get("message"));
            assertEquals("ReferenceError: isNotDefined is not defined\n" +
                            "    at isnotdefinedcaught.js:3:5", objectFromJava.map.get("stack"));

        }
    }

    /**
     * Test that Java Exception thrown from a Java method called from JavaScript can be caught and
     * accessed in Java.
     */
    @Test
    public void testJavaExceptionThroughJS() throws IOException {
        final HostAccess hostAccess = HostAccess.newBuilder().allowAccessAnnotatedBy(HostAccess.Export.class).build();
        try (Engine graalEngine = JSTest.newEngineBuilder().build(); Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).engine(graalEngine).build()) {
            Value bindings = context.getBindings(ID);
            ToBePassedToJS objectFromJava = new ToBePassedToJS(graalEngine, hostAccess);
            bindings.putMember("objectFromJava", objectFromJava);
            context.eval(Source.newBuilder(ID, "\nobjectFromJava.methodThatThrowsException();\n", "hostexception.js").build());
            fail("PolyglotException not thrown");
        } catch (PolyglotException e) {
            assertEquals("This operation is not supported!", e.getMessage());
            StackTraceElement[] stackTrace = e.getStackTrace();
            assertEquals(this.getClass().getSimpleName() + ".java", stackTrace[0].getFileName());
            assertEquals(EXCEPTION_LINE_NUMBER, stackTrace[0].getLineNumber());
            assertEquals("hostexception.js", stackTrace[1].getFileName());
            assertEquals(2, stackTrace[1].getLineNumber());
            assertTrue(e.isHostException());
            Throwable hostException = e.asHostException();
            assertTrue(hostException instanceof UnsupportedOperationException);

            StackTraceElement[] hostExceptionStackTrace = hostException.getStackTrace();
            assertEquals("This operation is not supported!", hostException.getMessage());
            assertEquals(this.getClass().getSimpleName() + ".java", hostExceptionStackTrace[0].getFileName());
            assertEquals(EXCEPTION_LINE_NUMBER, hostExceptionStackTrace[0].getLineNumber());
        }
    }

    /**
     * Test that a "nested" Java Exception thrown from a Java method called in nested JavaScript
     * execution called from a Java method called in JavaScript can be caught and accessed in Java.
     */
    @Test
    public void testNestedJavaExceptionThroughJS() throws IOException {
        final HostAccess hostAccess = HostAccess.newBuilder().allowAccessAnnotatedBy(HostAccess.Export.class).build();
        try (Engine graalEngine = JSTest.newEngineBuilder().build(); Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).engine(graalEngine).build()) {
            Value bindings = context.getBindings(ID);
            ToBePassedToJS objectFromJava = new ToBePassedToJS(graalEngine, hostAccess);
            bindings.putMember("objectFromJava", objectFromJava);
            context.eval(Source.newBuilder(ID, "\nobjectFromJava.methodThatThrowsNestedJavaException();\n", "testmain" +
                            ".js").build());
            fail("PolyglotException not thrown");
        } catch (PolyglotException e) {
            assertEquals("This operation is not supported!", e.getMessage());
            StackTraceElement[] stackTrace = e.getStackTrace();
            assertEquals(this.getClass().getSimpleName() + ".java", stackTrace[0].getFileName());
            assertEquals(EXCEPTION_LINE_NUMBER, stackTrace[0].getLineNumber());

            StackTraceElement[] jsStackTrace = Arrays.stream(e.getStackTrace()).filter(ste -> ste.getClassName().equals("<js>")).toArray(StackTraceElement[]::new);
            assertEquals("nestedhostexception.js", jsStackTrace[0].getFileName());
            assertEquals(2, jsStackTrace[0].getLineNumber());
            assertEquals("testmain.js", jsStackTrace[1].getFileName());
            assertEquals(2, jsStackTrace[1].getLineNumber());
            assertTrue(e.isHostException());
            Throwable hostException = e.asHostException();
            assertTrue(hostException instanceof UnsupportedOperationException);

            StackTraceElement[] hostExceptionStackTrace = hostException.getStackTrace();
            assertEquals("This operation is not supported!", hostException.getMessage());
            assertEquals(this.getClass().getSimpleName() + ".java", hostExceptionStackTrace[0].getFileName());
            assertEquals(EXCEPTION_LINE_NUMBER, hostExceptionStackTrace[0].getLineNumber());
        }
    }

    /**
     * Test that Java Exception caught in JavaScript can be accessed in JavaScript, passed to Java
     * and accessed normally. Please note that in order to be able to access methods of a Java
     * Exception in JavaScript, access to them must be allowed, e.g., by
     * {@link HostAccess.Builder#allowAccess}.
     */
    @Test
    public void testCaughtJavaException() throws IOException, NoSuchMethodException {
        final HostAccess hostAccess = HostAccess.newBuilder().allowAccessAnnotatedBy(HostAccess.Export.class).allowAccess(Throwable.class.getMethod("getMessage")).allowAccess(
                        Object.class.getMethod("getClass")).allowAccess(Class.class.getMethod("getName")).build();
        try (Engine graalEngine = JSTest.newEngineBuilder().build(); Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).engine(graalEngine).build()) {
            Value bindings = context.getBindings(ID);
            ToBePassedToJS objectFromJava = new ToBePassedToJS(graalEngine, hostAccess);
            bindings.putMember("objectFromJava", objectFromJava);
            Value val = context.eval(Source.newBuilder(ID, "var ex;\n" +
                            "try {\n" +
                            "    objectFromJava.methodThatThrowsException();\n" +
                            "} catch (e) {\n" +
                            "    objectFromJava.methodWithExceptionArgument(e, e.getClass().getName(), e.getMessage());\n" +
                            "    ex = e;" +
                            "}\n" +
                            "ex;\n", "isnotdefinedcaught.js").build());
            assertTrue(val.isHostObject());
            assertTrue(val.isException());
            assertEquals(objectFromJava.exception, val.asHostObject());
            assertTrue(val.asHostObject() instanceof UnsupportedOperationException);
            Exception hostException = val.asHostObject();
            StackTraceElement[] hostExceptionStackTrace = hostException.getStackTrace();
            assertEquals("This operation is not supported!", hostException.getMessage());
            assertEquals(this.getClass().getSimpleName() + ".java", hostExceptionStackTrace[0].getFileName());
            assertEquals(EXCEPTION_LINE_NUMBER, hostExceptionStackTrace[0].getLineNumber());

            assertEquals("isnotdefinedcaught.js", objectFromJava.jsStackTrace[0].getFileName());
            assertEquals(3, objectFromJava.jsStackTrace[0].getLineNumber());
            assertEquals(objectFromJava.exception.getClass().getName(), objectFromJava.exceptionName);
            assertEquals(objectFromJava.exception.getMessage(), objectFromJava.exceptionMessage);
        }
    }
}
