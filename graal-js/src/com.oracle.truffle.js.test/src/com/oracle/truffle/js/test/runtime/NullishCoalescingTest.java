/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.runtime;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class NullishCoalescingTest {

    @Test
    public void testNullish() {
        assertTrue(testIntl("null ?? 'exp'").asString().equals("exp"));
        assertTrue(testIntl("undefined ?? 'exp'").asString().equals("exp"));
        assertTrue(testIntl("42 ?? 'exp'").asInt() == 42);
        assertTrue(testIntl("42.42 ?? 'exp'").asDouble() == 42.42);
        assertTrue(testIntl("false ?? 'exp'").asBoolean() == false);
        assertTrue(testIntl("true ?? 'exp'").asBoolean() == true);
    }

    @Test
    public void testNullishErrors() {
        String expected = "cannot immediately contain";
        testFail("2020", expected, "1 ?? 1 || 1");
        testFail("2020", expected, "1 ?? 1 && 1");
        testFail("2020", expected, "1 || 1 ?? 1");
        testFail("2020", expected, "1 && 1 ?? 1");
    }

    private static Value testIntl(String source) {
        testFail("2019", "Expected an operand but found ?", source);

        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2020").build()) {
            return context.eval(JavaScriptLanguage.ID, source);
        }
    }

    private static void testFail(String version, String expectedFail, String source) {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ECMASCRIPT_VERSION_NAME, version).build()) {
            try {
                context.eval(JavaScriptLanguage.ID, source);
                fail();
            } catch (PolyglotException e) {
                assertTrue(e.isSyntaxError());
                assertTrue(e.getMessage(), e.getMessage().contains(expectedFail));
            }
        }
    }
}
