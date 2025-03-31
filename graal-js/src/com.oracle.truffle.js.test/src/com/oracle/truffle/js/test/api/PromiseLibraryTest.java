/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.api;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.js.api.PromiseLibrary;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;

@RunWith(Parameterized.class)
public class PromiseLibraryTest extends JSTest {
    private static final String[] INVALID_EXPRESSIONS = new String[]{"42", "undefined", "new Object()"};

    @Parameters(name = "{0}")
    public static List<Boolean> data() {
        return List.of(Boolean.TRUE, Boolean.FALSE);
    }

    @Parameter(value = 0) public boolean cached;

    private PromiseLibrary getLibrary(Object receiver) {
        if (cached) {
            return TestHelper.adopt(PromiseLibrary.getFactory().create(receiver));
        } else {
            return PromiseLibrary.getUncached();
        }
    }

    @Test
    public void testIsPromise() {
        testIsPromise("42", false);
        testIsPromise("undefined", false);
        testIsPromise("Promise", false);
        testIsPromise("new Promise(() => {})", true);
        testIsPromise("new Proxy(new Promise(() => {}), {})", false);
    }

    private void testIsPromise(String expression, boolean expected) {
        Object value = testHelper.runNoPolyglot(expression);
        boolean actual = getLibrary(value).isPromise(value);
        assertSame(expected, actual);
    }

    @Test
    public void testGetStateInvalid() {
        for (String expression : INVALID_EXPRESSIONS) {
            try {
                invokeGetState(expression);
                fail();
            } catch (UnsupportedMessageException ex) {
                // expected
            }
        }
    }

    @Test
    public void testGetState() {
        testGetState("Promise.resolve()", PromiseLibrary.State.FULFILLED);
        testGetState("Promise.reject()", PromiseLibrary.State.REJECTED);
        testGetState("(async () => {})()", PromiseLibrary.State.FULFILLED);
        testGetState("(async () => { throw new Error() })()", PromiseLibrary.State.REJECTED);
        testGetState("new Promise(() => {})", PromiseLibrary.State.PENDING);
    }

    private void testGetState(String expression, PromiseLibrary.State expected) {
        try {
            PromiseLibrary.State actual = invokeGetState(expression);
            assertSame(expected, actual);
        } catch (UnsupportedMessageException umex) {
            fail();
        }
    }

    private PromiseLibrary.State invokeGetState(String expression) throws UnsupportedMessageException {
        Object value = testHelper.runNoPolyglot(expression);
        return getLibrary(value).getState(value);
    }

    @Test
    public void testMarkHandledInvalid() {
        for (String expression : INVALID_EXPRESSIONS) {
            Object value = testHelper.runNoPolyglot(expression);
            try {
                getLibrary(value).markHandled(value);
                fail();
            } catch (UnsupportedMessageException ex) {
                // expected
            }
        }
    }

    @Test
    public void testMarkHandled() {
        testHelper = new TestHelper(newContextBuilder().option("js.unhandled-rejections", "throw"));
        Object promise = testHelper.runNoPolyglot("var promise = Promise.withResolvers(); promise.promise");

        try {
            // mark as handled
            getLibrary(promise).markHandled(promise);
        } catch (UnsupportedMessageException umex) {
            fail();
        }

        // reject the promise => it should not throw because it is handled
        testHelper.run("promise.reject()");
    }

}
