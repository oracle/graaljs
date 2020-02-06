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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.function.Predicate;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.HostAccess.Export;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.hamcrest.core.StringContains;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oracle.truffle.js.test.JSTest;

/**
 * Various tests for controlling access from JavaScript to Java classes and methods. Using host
 * access policy and {@link Builder#allowHostClassLookup(Predicate)}.
 */
public class ClassFilterInteropTest {

    @Rule public ExpectedException expectedException = ExpectedException.none();

    public static class MyClass {
        @Export
        public MyClass() {
        }

        @Export
        public int accessibleMethod() {
            return 42;
        }

        public int accessibleMethod(int x) {
            return x;
        }

        @Export
        public static int staticAccessibleMethod() {
            return 43;
        }
    }

    /**
     * Test that Java class allowed by {@link Builder#allowHostClassLookup(Predicate)} can be
     * accessed in JavaScript as well as its members allowed by {@link HostAccess host access
     * policy}. If host access policy is not specified by
     * {@link Builder#allowHostAccess(HostAccess)}, methods annotated by {@link Export} are
     * automatically accessible. If host access policy was specified, the annotation would have to
     * be explicitly allowed.
     */
    @Test
    public void testHostClassLookupPositive() {
        try (Context context = JSTest.newContextBuilder().allowHostClassLookup(c -> c.equals("com" +
                        ".oracle.truffle.js.test.interop" +
                        ".ClassFilterInteropTest$MyClass")).build()) {
            int result = context.eval("js", "" +
                            "var MyClass = Java.type('com.oracle.truffle.js.test.interop.ClassFilterInteropTest$MyClass');" +
                            "(new MyClass()).accessibleMethod() + MyClass.staticAccessibleMethod() +" +
                            "(new com.oracle.truffle.js.test.interop.ClassFilterInteropTest$MyClass()).accessibleMethod() +" +
                            "com.oracle.truffle.js.test.interop.ClassFilterInteropTest$MyClass.staticAccessibleMethod();").asInt();
            assertEquals(170, result);
        }
    }

    /**
     * Test that methods with the same name that differ only in their signature can be
     * differentiated by host access policy, namely, one can be accessible and the other not.
     */
    @Test
    public void testHostClassLookupNegativeInaccessibleMethod() {
        expectedException.expect(PolyglotException.class);
        expectedException.expectMessage("Arity error - expected: 0 actual: 1");
        try (Context context = JSTest.newContextBuilder().allowHostClassLookup(c -> c.equals("com" +
                        ".oracle.truffle.js.test.interop" +
                        ".ClassFilterInteropTest$MyClass")).build()) {
            context.eval("js", "" +
                            "var MyClass = Java.type('com.oracle.truffle.js.test.interop.ClassFilterInteropTest$MyClass');" +
                            "(new MyClass()).accessibleMethod(42);").asInt();
            fail("Exception should have been thrown.");
        }
    }

    /**
     * Test that a class not specified in {@link Builder#allowHostClassLookup(Predicate)} cannot be
     * accessed via "Java.type" in JavaScript. Please note that the class could still be accessed
     * via reflection using some other class. But in order for this to be possible
     * {@link Builder#allowAllAccess(boolean) all access} or
     * {@link HostAccess.Builder#allowPublicAccess public access} would have to be <code>true</code>
     * , or the necessary methods would have to be made accessible via
     * {@link HostAccess.Builder#allowAccess}.
     */
    @Test
    public void testHostClassLookupNegativeHostClassLookupDisallowed1() {
        expectedException.expect(PolyglotException.class);
        expectedException.expectMessage("ReferenceError: Java is not defined");
        try (Context context = JSTest.newContextBuilder().build()) {
            context.eval("js", "" +
                            "var MyClass = Java.type('com.oracle.truffle.js.test.interop.ClassFilterInteropTest$MyClass');" +
                            "(new MyClass()).accessibleMethod();").asInt();
            fail("Exception should have been thrown.");
        }
    }

    /**
     * Test that a class not specified in {@link Builder#allowHostClassLookup(Predicate)} cannot be
     * accessed via its class name in JavaScript. Please note that the class could still be accessed
     * via reflection using some other class. But in order for this to be possible
     * {@link Builder#allowAllAccess(boolean) all access} or
     * {@link HostAccess.Builder#allowPublicAccess public access} would have to be <code>true</code>
     * , or the necessary methods would have to be made accessible via
     * {@link HostAccess.Builder#allowAccess}.
     */
    @Test
    public void testHostClassLookupNegativeHostClassLookupDisallowed2() {
        expectedException.expect(PolyglotException.class);
        expectedException.expectMessage("ReferenceError: com is not defined");
        try (Context context = JSTest.newContextBuilder().build()) {
            context.eval("js", "" +
                            "(new com.oracle.truffle.js.test.interop.ClassFilterInteropTest$MyClass()).accessibleMethod()" +
                            "").asInt();
            fail("Exception should have been thrown.");
        }
    }

    /**
     * Test that a {@link HostAccess host access policy} with
     * {@link HostAccess.Builder#allowPublicAccess public access} allowed is powerful. Namely,
     * arbitrary Java class can be loaded and its methods called.
     */
    @Test
    public void testPublicAccessIsPowerfull() {
        final HostAccess hostAccess = HostAccess.newBuilder().allowPublicAccess(true).allowArrayAccess(true).build();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).build()) {
            Value bindings = context.getBindings(ID);
            MyClass myClass = new MyClass();
            bindings.putMember("myClass", myClass);
            int result = context.eval("js", "" +
                            "myClass.getClass().getClassLoader().loadClass('java.lang.Math').getDeclaredMethods()[0].invoke" +
                            "(null, 42) +" +
                            "myClass.getClass().getClass().static.forName('java.lang.Math').getDeclaredMethods()[0].invoke" +
                            "(null, 43);").asInt();
            assertEquals(85, result);
        }
    }

    /**
     * Test that a {@link HostAccess host access policy} with
     * {@link HostAccess.Builder#allowPublicAccess public access} allowed is very powerful. Namely,
     * new processes can be started.
     */
    @Test
    public void testPublicAccessIsReallyPowerfull() {
        final HostAccess hostAccess = HostAccess.newBuilder().allowPublicAccess(true).build();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).build()) {
            Value bindings = context.getBindings(ID);
            MyClass myClass = new MyClass();
            bindings.putMember("myClass", myClass);
            String osName = System.getProperty("os.name");
            boolean isWindows = osName != null && osName.toLowerCase().contains("windows");
            String result = context.eval("js", "function readIs(is) { " +
                            "var r = \"\"; " +
                            "while(true) { " +
                            "var c = is.read(); " +
                            "if(c == -1) { " +
                            "break; " +
                            "} " +
                            "r += String.fromCharCode(c) " +
                            "} " +
                            "is.close();" +
                            "return r; " +
                            "} " +
                            "var runtime = myClass.getClass().getClassLoader().loadClass('java.lang.Runtime')" +
                            ".getDeclaredMethod('getRuntime').invoke(null);" +
                            "var process = runtime.getClass().getMethod('exec', myClass.getClass().getClassLoader().loadClass" +
                            "('java.lang.String')).invoke(runtime,'" + (isWindows ? "cmd /C cd" : "pwd") + "');" +
                            "process.getOutputStream().close();" +
                            "var stdout = readIs(process.getInputStream());" +
                            "var stderr = readIs(process.getErrorStream());" +
                            "process.waitFor();" +
                            "stdout;").asString();
            String userDir = System.getProperty("user.dir");
            assertThat(result.trim(), StringContains.containsString(userDir));
        }
    }

    /**
     * Test restricting reflection access using {@link HostAccess.Builder#denyAccess}.
     */
    @Test
    public void testPublicAccessIsLessPowerfullWithDeniedClassAccess1() {
        expectedException.expect(PolyglotException.class);
        expectedException.expectMessage("Unknown identifier: getClassLoader");
        final HostAccess hostAccess = HostAccess.newBuilder().allowPublicAccess(true).allowArrayAccess(true).denyAccess(Class.class).build();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).build()) {
            Value bindings = context.getBindings(ID);
            MyClass myClass = new MyClass();
            bindings.putMember("myClass", myClass);
            context.eval("js", "" +
                            "myClass.getClass().getClassLoader().loadClass('java.lang.Math').getDeclaredMethods()[0].invoke" +
                            "(null, 42);").asInt();
            fail("Exception should have been thrown");
        }
    }

    /**
     * Test restricting reflection access using {@link HostAccess.Builder#denyAccess}.
     */
    @Test
    public void testPublicAccessIsLessPowerfullWithDeniedClassAccess2() {
        expectedException.expect(PolyglotException.class);
        expectedException.expectMessage("Unknown identifier: forName");
        final HostAccess hostAccess = HostAccess.newBuilder().allowPublicAccess(true).allowArrayAccess(true).denyAccess(Class.class).build();
        try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).build()) {
            Value bindings = context.getBindings(ID);
            MyClass myClass = new MyClass();
            bindings.putMember("myClass", myClass);
            context.eval("js", "" +
                            "myClass.getClass().getClass().static.forName('java.lang.Math').getDeclaredMethods()[0].invoke" +
                            "(null, 43);").asInt();
            fail("Exception should have been thrown");
        }
    }

}
