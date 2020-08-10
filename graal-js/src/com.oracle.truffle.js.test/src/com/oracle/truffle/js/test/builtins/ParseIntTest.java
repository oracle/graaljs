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
package com.oracle.truffle.js.test.builtins;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.test.JSTest;

public class ParseIntTest {

    @Test
    public void testJSONParseNumber() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value result;
            result = context.eval(ID, "parseInt('-0')");
            assertTrue(result.fitsInDouble());
            assertEquals(Double.valueOf(-0.0), Double.valueOf(result.asDouble()));
            result = context.eval(ID, "parseInt(new String('-0'))");
            assertTrue(result.fitsInDouble());
            assertEquals(Double.valueOf(-0.0), Double.valueOf(result.asDouble()));
        }
    }

    @Test
    public void testSpecializations() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value result;

            // parseIntStringInt10
            result = context.eval(ID, "parseInt('12345', 10)");
            assertTrue(result.fitsInInt());
            assertEquals(12345, result.asInt());

            result = context.eval(ID, "parseInt('3', 10)"); // check short input
            assertTrue(result.fitsInInt());
            assertEquals(3, result.asInt());

            // parseIntStringInt10 with whitespace
            result = context.eval(ID, "parseInt('\t 12345 \t', 10)");
            assertTrue(result.fitsInInt());
            assertEquals(12345, result.asInt());

            // parseIntGeneric
            result = context.eval(ID, "parseInt('12345', 9)");
            assertTrue(result.fitsInInt());
            assertEquals(8303, result.asInt());

            // parseIntGeneric with whitespace
            result = context.eval(ID, "parseInt('\t 12345 \t', 9)");
            assertTrue(result.fitsInInt());
            assertEquals(8303, result.asInt());

            // parseIntGeneric with whitespace and hexStart
            result = context.eval(ID, "parseInt('\t -0xABCDEF \t', 16)");
            assertTrue(result.fitsInInt());
            assertEquals(-11259375, result.asInt());

            result = context.eval(ID, "parseInt('\t 3 \t', 16)"); // check short input
            assertTrue(result.fitsInInt());
            assertEquals(3, result.asInt());

            // parseIntGeneric with hexStart
            result = context.eval(ID, "parseInt('  0xA  ', 16)");
            assertTrue(result.fitsInInt());
            assertEquals(10, result.asInt());
        }
    }

    @Test
    public void testInvalid() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value result;

            result = context.eval(ID, "parseInt('0x  ', 16)");
            assertTrue(result.fitsInDouble());
            assertTrue(Double.isNaN(result.asDouble()));

            // identify valid part of string
            result = context.eval(ID, "parseInt('  1234---  ', 10)");
            assertTrue(result.fitsInInt());
            assertEquals(1234, result.asInt());
        }
    }

    @Test
    public void testLongs() {
        try (Context context = JSTest.newContextBuilder().build()) {
            assertEquals(-1234567890L, parseInt("-1234567890", 10, context).asLong());
            assertEquals(1234567890L, parseInt("+1234567890", 10, context).asLong());
            assertEquals(1234567890L, parseInt("1234567890", 10, context).asLong());
            assertEquals(162254319L, parseInt("9abcdef", 16, context).asLong());

            assertEquals(9223372036854778000d, parseInt("1104332401304422434310342411", 5, context).asDouble(), 0.000001);
        }
    }

    @Test
    public void testNegativeZero() {
        try (Context context = JSTest.newContextBuilder().build()) {
            assertTrue(JSRuntime.isNegativeZero(parseInt("-0.000001", 10, context).asDouble()));
            assertTrue(JSRuntime.isNegativeZero(parseInt("-0.0000000000000001", 10, context).asDouble()));
            assertTrue(JSRuntime.isNegativeZero(parseInt("-0", 10, context).asDouble()));
            assertTrue(JSRuntime.isNegativeZero(parseInt("-000000000000000000", 16, context).asDouble()));
        }
    }

    private static Value parseInt(String string, int radix, Context context) {
        return context.eval(ID, "parseInt('" + string + "', " + radix + ");");
    }

    @Test
    public void testGR25478() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value result = context.eval(ID, "parseInt('18446462598732840000\" at LowLevelInterpreter.asm:194', 10)");
            assertTrue(result.fitsInDouble());
            assertEquals(18446462598732840000d, result.asDouble(), 0.000001);

            result = context.eval(ID, "parseInt('-10000000000000', 10)");
            assertTrue(result.fitsInDouble());
            assertEquals(-10000000000000d, result.asDouble(), 0.000001);

        }
    }
}
