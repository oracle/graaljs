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
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

/**
 * These tests should verify compatibility around very large numbers, especially in
 * Temporal.Duration and related functionality.
 *
 * The spec is somewhat unclear on the limits of the fields of the Duration object.
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
    public void testInstantToString() {
        String code = "const i1 = new Temporal.Instant(1234567890123456789n);\n" +
                        "const i2 = new Temporal.Instant(8640000000000000000000n); \n" +
                        "console.log(i1.toString()); console.log(i2.toString()); \n" +
                        "i1.toString() === '2009-02-13T23:31:30.123456789Z' && i2.toString() === '+275760-09-13T00:00:00Z';";
        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, code);
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testZonedDateTimeSince() {
        // `since` results in Duration, that might lose precision
        String code = "const thePast = new Temporal.ZonedDateTime(1234567890123456789n, '-08:00');\n" +
                        "const theFuture = new Temporal.ZonedDateTime(2345678901234567890n, '-08:00');\n" +
                        "var r = theFuture.since(thePast);\n" +
                        "r.toString() === 'PT308641H56M51.111111101S' && r.nanoseconds === 101 && r.seconds === 51;";

        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, code);
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testDurationHugeYears() {
        // consistent with Polyfill run on Node 14
        String code = "let d = new Temporal.Duration(9223372036854776000);\n" +
                        // "print(d.toString());" +
                        "d.years === 9223372036854776000 && d.toString() === 'P9223372036854775808Y';";

        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, code);
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testDurationHugeYearsScientific() {
        // consistent with Polyfill
        String code = "let d = new Temporal.Duration(1e100);\n" +
                        // "print(d.years); print(d.toString());" +
                        "d.years === 1e+100 && d.toString() === 'P10000000000000000159028911097599180468360808563945281389781327557747838772170381060813469985856815104Y';";

        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, code);
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testDurationHugeNanoseconds() {
        // consistent with Polyfill run on Node 14
        String code = "let d = new Temporal.Duration(0,0,0,0,0,0,0,0,0,9223372036854776000);\n" +
                        // "print(d.toString()); \n" +
                        "d.nanoseconds === 9223372036854776000 && d.toString() === 'PT9223372036.854775808S' ;";

        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, code);
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testDurationFromHuge() {
        String code = "var duration = Temporal.Duration.from({nanoseconds: 1e100}); duration.toString();\n";
        String expected = "PT10000000000000000159028911097599180468360808563945281389781327557747838772170381060813469985.856815104S";
        try (Context ctx = getJSContext()) {
            Assert.assertEquals(expected, ctx.eval(ID, code).asString());
        }
    }

    @Test
    public void testDurationHugeAdd() {
        String code = "var duration = new Temporal.Duration(0,0,0,5e18);\n" +
                        "duration = duration.add(duration);" +
                        "duration.toString() === 'P10000000000000000000D' ;";

        try (Context ctx = getJSContext()) {
            Value result = ctx.eval(ID, code);
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testCalendarDateAddHuge() {
        String code = "var calendar = new Temporal.Calendar('iso8601');\n" +
                        "var date = new Temporal.PlainDate(2022, 1, 1);\n" +
                        "var duration = new Temporal.Duration(1e100);\n" +
                        "var result = calendar.dateAdd(date, duration);\n" +
                        "throw TypeError('should not reach here');";

        try (Context ctx = getJSContext()) {
            ctx.eval(ID, code);
            Assert.fail("exception expected");
        } catch (PolyglotException ex) {
            Assert.assertTrue(ex.getMessage().contains("RangeError"));
            Assert.assertTrue(ex.getMessage().contains("out of range"));
        }
    }

    @Test
    public void testPlainDateAddHuge() {
        String code = "var date = new Temporal.PlainDate(2022, 1, 1);\n" +
                        "var duration = new Temporal.Duration(1e100);\n" +
                        "var result = date.add(duration);\n" +
                        "throw TypeError('should not reach here');";

        try (Context ctx = getJSContext()) {
            ctx.eval(ID, code);
            Assert.fail("exception expected");
        } catch (PolyglotException ex) {
            Assert.assertTrue(ex.getMessage().contains("RangeError"));
            Assert.assertTrue(ex.getMessage().contains("out of range"));
        }
    }

    @Test
    public void testPlainTimeAddHugeNanoseconds() {
        String code = "var time = new Temporal.PlainTime(1, 2, 3);\n" +
                        "var duration = Temporal.Duration.from({nanoseconds: 1e100});\n" +
                        "var result = time.add(duration);\n" +
                        "result.toString();";
        String expected = "00:48:00.328088104";
        try (Context ctx = getJSContext()) {
            Assert.assertEquals(expected, ctx.eval(ID, code).asString());
        }
    }
}
