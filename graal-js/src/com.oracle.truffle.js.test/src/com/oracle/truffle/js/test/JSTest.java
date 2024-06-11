/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.Strings;

public abstract class JSTest {

    static {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    }

    protected TestHelper testHelper;

    @Before
    public void setup() {
        testHelper = new TestHelper();
    }

    @After
    public void close() {
        testHelper.close();
    }

    public static Context.Builder newContextBuilder(String... permittedLanguages) {
        return Context.newBuilder(permittedLanguages.length == 0 ? new String[]{JavaScriptLanguage.ID} : permittedLanguages).allowExperimentalOptions(true);
    }

    public static Engine.Builder newEngineBuilder() {
        return Engine.newBuilder().allowExperimentalOptions(true);
    }

    public static void assertTStringEquals(String a, Object b) {
        Assert.assertEquals(Strings.fromJavaString(a), b);
    }

    public static void assertThrows(Runnable test, Consumer<PolyglotException> exceptionVerifier) {
        assertThrows(test, PolyglotException.class, exceptionVerifier);
    }

    public static <T extends Throwable> void assertThrows(Runnable test, Class<T> exceptionType, Consumer<T> exceptionVerifier) {
        try {
            test.run();
            fail("should have thrown " + exceptionType.getSimpleName());
        } catch (Throwable e) {
            if (exceptionType.isInstance(e)) {
                exceptionVerifier.accept(exceptionType.cast(e));
            } else {
                throw e;
            }
        }
    }

    public static <T extends Throwable> void assertThrows(Runnable test, Class<T> exceptionType) {
        assertThrows(test, exceptionType, (T e) -> {
        });
    }

    public static void assertThrows(Runnable test, JSErrorType expectedJSError) {
        try {
            test.run();
            fail("should have thrown " + expectedJSError.name());
        } catch (PolyglotException e) {
            assertTrue(e.isGuestException());
            assertThat(e.getMessage(), startsWith(expectedJSError.name()));
        }
    }
}
