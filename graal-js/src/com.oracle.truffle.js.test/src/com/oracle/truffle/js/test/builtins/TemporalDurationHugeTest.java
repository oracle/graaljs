/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
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
        testTrue(code);
    }

    @Test
    public void testInstantToString() {
        String code = "const i1 = new Temporal.Instant(1234567890123456789n);\n" +
                        "const i2 = new Temporal.Instant(8640000000000000000000n); \n" +
                        "i1.toString() === '2009-02-13T23:31:30.123456789Z' && i2.toString() === '+275760-09-13T00:00:00Z';";
        testTrue(code);
    }

    @Test
    public void testZonedDateTimeSince() {
        // `since` results in Duration, that might lose precision
        String code = "const thePast = new Temporal.ZonedDateTime(1234567890123456789n, '-08:00');\n" +
                        "const theFuture = new Temporal.ZonedDateTime(2345678901234567890n, '-08:00');\n" +
                        "var r = theFuture.since(thePast);\n" +
                        "r.toString() === 'PT308641H56M51.111111101S' && r.nanoseconds === 101 && r.seconds === 51;";
        testTrue(code);
    }

    @Test
    public void testDurationHugeYears() {
        assertThrowsRangeError("new Temporal.Duration(9223372036854776000);");
    }

    @Test
    public void testDurationHugeYearsScientific() {
        assertThrowsRangeError("new Temporal.Duration(1e100);");
    }

    @Test
    public void testDurationHugeNanoseconds() {
        String code = "let d = new Temporal.Duration(0,0,0,0,0,0,0,0,0,9223372036854776000);\n" +
                        "d.nanoseconds === 9223372036854776000 && d.toString() === 'PT9223372036.854775808S' ;";
        testTrue(code);
    }

    @Test
    public void testDurationConstructorAndFromHuge() {
        assertThrowsRangeError("Temporal.Duration.from({nanoseconds: 1e100})");
        assertThrowsRangeError("new Temporal.Duration(0, 0, 0, 0, 2**100)");
    }

    private static void testTrue(String code) {
        try (Context ctx = getJSContext()) {
            Assert.assertTrue(ctx.eval(ID, code).asBoolean());
        }
    }

    private static void assertThrowsRangeError(String code) {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, code);
            Assert.fail("RangeError expected");
        } catch (PolyglotException ex) {
            MatcherAssert.assertThat(ex.getMessage(), CoreMatchers.startsWith("RangeError"));
        }
    }

}
