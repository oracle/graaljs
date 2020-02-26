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
package com.oracle.truffle.js.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.PolyglotException.StackFrame;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;

public class SourceURLTest {

    @Test
    public void testSourceURL() {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.eval(Source.newBuilder(JavaScriptLanguage.ID, "" +
                            "function dumpStack() { return new Error().stack; }\n" +
                            "function throwError() { throw new Error(); }",
                            "common.js").buildLiteral());

            Value result;
            result = context.eval(Source.newBuilder(JavaScriptLanguage.ID, "dumpStack(); //# sourceURL=testsource.js", "test1.js").buildLiteral());
            assertTrue(result.isString());
            assertTrue(result.asString(), result.asString().contains("testsource.js"));
            assertFalse(result.asString(), result.asString().contains("test1.js"));
            assertTrue(result.asString(), result.asString().contains("common.js"));

            result = context.eval(Source.newBuilder(JavaScriptLanguage.ID, "eval('dumpStack(); //# sourceURL=evalsource.js');", "test2.js").buildLiteral());
            assertTrue(result.isString());
            assertTrue(result.asString(), result.asString().contains("evalsource.js"));
            assertTrue(result.asString(), result.asString().contains("test2.js"));
            assertTrue(result.asString(), result.asString().contains("common.js"));

            try {
                context.eval(Source.newBuilder(JavaScriptLanguage.ID, "throwError(); //# sourceURL=testsource.js", "test3.js").buildLiteral());
                fail();
            } catch (PolyglotException e) {
                assertTrue(e.isGuestException());
                verifyGuestFrames(e, Arrays.asList("common.js", "testsource.js"));
            }
        }
    }

    @Test
    public void testSourceURLInternal() {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.eval(Source.newBuilder(JavaScriptLanguage.ID, "" +
                            "function dumpStack() { return new Error().stack; }\n" +
                            "function throwError() { throw new Error(); }",
                            "common.js").buildLiteral());

            Value result;
            result = context.eval(Source.newBuilder(JavaScriptLanguage.ID, "dumpStack(); //# sourceURL=internal:testsource.js", "test1.js").buildLiteral());
            assertTrue(result.isString());
            assertFalse(result.asString(), result.asString().contains("testsource.js"));
            assertFalse(result.asString(), result.asString().contains("test1.js"));
            assertTrue(result.asString(), result.asString().contains("common.js"));

            result = context.eval(Source.newBuilder(JavaScriptLanguage.ID, "eval('dumpStack(); //# sourceURL=internal:evalsource.js');", "test2.js").buildLiteral());
            assertTrue(result.isString());
            assertFalse(result.asString(), result.asString().contains("evalsource.js"));
            assertTrue(result.asString(), result.asString().contains("test2.js"));
            assertTrue(result.asString(), result.asString().contains("common.js"));

            try {
                context.eval(Source.newBuilder(JavaScriptLanguage.ID, "throwError(); //# sourceURL=internal:testsource.js", "test3.js").buildLiteral());
                fail();
            } catch (PolyglotException e) {
                assertTrue(e.isGuestException());
                verifyGuestFrames(e, Arrays.asList("common.js"));
            }
        }
    }

    private static void verifyGuestFrames(PolyglotException e, List<String> expectedNames) {
        List<PolyglotException.StackFrame> stack = new ArrayList<>();
        for (StackFrame stackFrame : e.getPolyglotStackTrace()) {
            if (stackFrame.isGuestFrame()) {
                stack.add(stackFrame);
            }
        }
        assertEquals(stack.toString(), expectedNames.size(), stack.size());
        for (int i = 0; i < expectedNames.size(); i++) {
            String sourceName = stack.get(i).getSourceLocation().getSource().getName();
            assertEquals(sourceName, expectedNames.get(i), sourceName);
        }
    }
}
