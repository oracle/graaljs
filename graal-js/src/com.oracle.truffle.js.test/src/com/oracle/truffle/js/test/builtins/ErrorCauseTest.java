/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.builtins;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

public class ErrorCauseTest {
    private static final String[] errorTypes = {
                    "Error",
                    "RangeError",
                    "TypeError",
                    "ReferenceError",
                    "SyntaxError",
                    "EvalError",
                    "URIError",
                    "AggregateError"
    };

    private static final String[] defaults = {
                    "'message'",
                    "'message'",
                    "'message'",
                    "'message'",
                    "'message'",
                    "'message'",
                    "'message'",
                    "[],'message'"
    };

    private static final String[] defaultChecks = {
                    "e.message === 'message'",
                    "e.message === 'message'",
                    "e.message === 'message'",
                    "e.message === 'message'",
                    "e.message === 'message'",
                    "e.message === 'message'",
                    "e.message === 'message'",
                    "e.message === 'message'"
    };

    private static void runErrorTest(String[] errors) {
        for (String source : errors) {
            try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ERROR_CAUSE_NAME, "true").build()) {
                Value value = context.eval(JavaScriptLanguage.ID, source);
                Assert.assertTrue(value.isBoolean());
                Assert.assertTrue(value.asBoolean());
            } catch (Exception e) {
                Assert.fail();
            }
        }
    }

    private static String[] buildErrors(String cause, String additionalChecks) {
        String[] errors = new String[errorTypes.length];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errorTypes.length; i++) {
            sb.setLength(0);
            sb.append("let e = new ").append(errorTypes[i]).append('(').append(defaults[i]);
            if (!cause.isEmpty()) {
                sb.append(',').append(cause);
            }
            sb.append(");").append(defaultChecks[i]);
            if (!additionalChecks.isEmpty()) {
                sb.append(" && ").append(additionalChecks);
            }
            sb.append(';');
            errors[i] = sb.toString();
        }
        return errors;
    }

    @Test
    public void testNoCause() {
        runErrorTest(buildErrors("", "e.cause === undefined"));
    }

    @Test
    public void testEmptyCause() {
        runErrorTest(buildErrors("{}", "e.cause === undefined"));
    }

    @Test
    public void testStringCause() {
        runErrorTest(buildErrors("{ cause: 'test'}", "e.cause === 'test'"));
    }

    @Test
    public void testNumberCause() {
        runErrorTest(buildErrors("{ cause: 0 }", "e.cause === 0"));
    }

    @Test
    public void testBooleanCause() {
        runErrorTest(buildErrors("{ cause: false }", "e.cause === false"));
    }

    @Test
    public void testObjectCause() {
        runErrorTest(buildErrors("{ cause: { a: 0, b: false}}", "e.cause.a === 0 && e.cause.b === false"));
    }

    @Test
    public void testFunctionCause() {
        runErrorTest(buildErrors("{ cause: () => 0}", "e.cause() === 0"));
    }

    @Test
    public void testNullCause() {
        runErrorTest(buildErrors("{ cause: null }", "e.cause === null"));
    }

    @Test
    public void testUndefinedCause() {
        runErrorTest(buildErrors("{ cause: undefined }", "e.cause === undefined"));
    }

    @Test
    public void testOtherProperties() {
        runErrorTest(buildErrors("{ a: 0, b: false}", "e.cause === undefined && e.a === undefined && e.b === undefined"));
        runErrorTest(buildErrors("{ cause: 0, a: 0, b: false}", "e.cause === 0 && e.a === undefined && e.b === undefined"));
    }

    @Test
    public void testCauseIsNotObject() {
        runErrorTest(buildErrors("'test'", "e.cause === undefined"));
        runErrorTest(buildErrors("0", "e.cause === undefined"));
        runErrorTest(buildErrors("true", "e.cause === undefined"));
        runErrorTest(buildErrors("() => {}", "e.cause === undefined"));
        runErrorTest(buildErrors("null", "e.cause === undefined"));
    }

    @Test
    public void testEnumerableProperty() {
        runErrorTest(buildErrors("'test'", "!e.propertyIsEnumerable('cause')"));
    }
}
