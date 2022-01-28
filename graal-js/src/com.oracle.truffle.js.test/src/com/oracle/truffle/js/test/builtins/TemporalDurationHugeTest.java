/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

/**
 * These tests should verify compatibility around very large numbers, especially in
 * Temporal.Duration and related functionality.
 *
 * The spec is somewhat unclear on the limits of the fields of the Duraton object.
 */
public class TemporalDurationHugeTest extends JSTest {

    private static Context getJSContext() {
        return JSTest.newContextBuilder(ID).option("js.temporal", "true").build();
    }

    @Test
    public void testInstantSince() {
        // `since` results in Duration, that might lose precision
        String code = "const i1 = new Temporal.Instant(1234567890123456789n);\n" +
                        "const i2 = new Temporal.Instant(2345678901234567890n);" +
                        "const result = i2.since(i1); \n" +
                        "result.nanoseconds === 101 && result.seconds === 1111111011;";

        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, code);
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testDurationHugeYears() {
        // consistent with Polyfill
        String code = "let d = new Temporal.Duration(9223372036854776000);\n" +
                        "d.years === 9223372036854776000 && d.toString() === 'P9223372036854775807Y';";

        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, code);
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testDurationHugeNanoseconds() {
        // consistent with Polyfill
        String code = "let d = new Temporal.Duration(0,0,0,0,0,0,0,0,0,9223372036854776000);\n" +
                        "print(d.nanoseconds); print(d.toString());" +
                        "d.nanoseconds === 9223372036854776000 && d.toString() === 'PT9223372036.854775807S' ;";

        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, code);
            Assert.assertTrue(result.asBoolean());
        }
    }

}
