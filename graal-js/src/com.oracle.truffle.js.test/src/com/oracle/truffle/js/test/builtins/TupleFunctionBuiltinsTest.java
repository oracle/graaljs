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
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TupleFunctionBuiltinsTest {

    private static final String testName = "tuple-function-builtins-test";

    private static Value execute(String sourceText) {
        try (Context context = JSTest.newContextBuilder()
                .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022")
                .build()) {
            return context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceText, testName).buildLiteral());
        }
    }

    private static void expectError(String sourceText, String expectedMessage) {
        try (Context context = JSTest.newContextBuilder()
                .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022")
                .build()) {
            context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceText, testName).buildLiteral());
            Assert.fail("should have thrown");
        } catch (Exception ex) {
            Assert.assertTrue(
                    String.format("\"%s\" should contain \"%s\"", ex.getMessage(), expectedMessage),
                    ex.getMessage().contains(expectedMessage)
            );
        }
    }

    @Test
    public void testIsTuple() {
        assertTrue(execute("Tuple.isTuple(#[])").asBoolean());
        assertTrue(execute("Tuple.isTuple(Object(#[]))").asBoolean());
        assertFalse(execute("Tuple.isTuple()").asBoolean());
        assertFalse(execute("Tuple.isTuple([])").asBoolean());
    }

    @Test
    public void testFrom() {
        assertTrue(execute("Tuple.from('foo') === #['f', 'o', 'o']").asBoolean());
        assertTrue(execute("Tuple.from([1, 2, 3], x => x + x) === #[2, 4, 6]").asBoolean());
        assertTrue(execute("Tuple.from([1, 2, 3], function (x) { return x + this}, 10) === #[11, 12, 13]").asBoolean());
        assertTrue(execute("Tuple.from({a: 1}) === #[]").asBoolean());
        assertTrue(execute("Tuple.from({0: 'data', length: 3}) === #['data', undefined, undefined]").asBoolean());
        assertTrue(execute("Tuple.from({0: 'data', length: 1}, it => it.length) === #[4]").asBoolean());
        expectError("Tuple.from([], 10)", "mapping function");
        expectError("Tuple.from([{}])", "non-primitive values");
        expectError("Tuple.from({0: {}, length: 1})", "non-primitive values");
    }

    @Test
    public void testOf() {
        assertTrue(execute("Tuple.of() === #[]").asBoolean());
        assertTrue(execute("Tuple.of(1, 2, 3) === #[1, 2, 3]").asBoolean());
        assertTrue(execute("Tuple.of(1, #['2', #[3n]]) === #[1, #['2', #[3n]]]").asBoolean());
        assertTrue(execute("Tuple.of(null, undefined) === #[null, undefined]").asBoolean());
        expectError("Tuple.of({})", "non-primitive values");
    }
}
