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

package com.oracle.truffle.js.test.interop;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

public class TemporalInteropToJavaTest extends JSTest {

    private static Context getJSContext() {
        return JSTest.newContextBuilder(ID).option("js.temporal", "true").build();
    }

    // Calendar, PlainMonthDay, PlainYearMonth cannot be converted to Instant, Date or Time

    @Test
    public void testInstant() {
        try (Context ctx = getJSContext()) {
            Value val = ctx.eval(ID, "new Temporal.Instant(0n);");
            Instant inst = val.asInstant();
            Assert.assertEquals(0, inst.getNano());
            Assert.assertEquals(0, inst.getEpochSecond());
        }

        try (Context ctx = getJSContext()) {
            Value val = ctx.eval(ID, "new Temporal.Instant(100_000_000n);");
            Instant inst = val.asInstant();
            Assert.assertEquals(100_000_000L, inst.getNano());
            Assert.assertEquals(0, inst.getEpochSecond());
        }

        try (Context ctx = getJSContext()) {
            Value val = ctx.eval(ID, "new Temporal.Instant(100_123_456_789n);");
            Instant inst = val.asInstant();
            Assert.assertEquals(123_456_789L, inst.getNano());
            Assert.assertEquals(100, inst.getEpochSecond());

            LocalDate ld = val.asDate();
            Assert.assertEquals(1970, ld.getYear());
            Assert.assertEquals(Month.JANUARY, ld.getMonth());
            Assert.assertEquals(1, ld.getDayOfYear());

            LocalTime lt = val.asTime();
            Assert.assertEquals(0, lt.getHour());
            Assert.assertEquals(1, lt.getMinute());
            Assert.assertEquals(40, lt.getSecond());
            Assert.assertEquals(123_456_789L, lt.getNano());

            ZoneId zid = val.asTimeZone();
            Assert.assertEquals("UTC", zid.getId());
        }
    }

    @Test
    public void testPlainDate() {
        try (Context ctx = getJSContext()) {
            Value val = ctx.eval(ID, "new Temporal.PlainDate(1982, 11, 26);");
            LocalDate ld = val.asDate();
            Assert.assertEquals(1982, ld.getYear());
            Assert.assertEquals(Month.NOVEMBER, ld.getMonth());
            Assert.assertEquals(26, ld.getDayOfMonth());

            Assert.assertFalse(val.isTime());
            Assert.assertFalse(val.isTimeZone());
        }
    }

    @Test
    public void testPlainTime() {
        try (Context ctx = getJSContext()) {
            Value val = ctx.eval(ID, "new Temporal.PlainTime(12, 34, 56, 987, 654, 321);");
            LocalTime lt = val.asTime();
            Assert.assertEquals(12, lt.getHour());
            Assert.assertEquals(34, lt.getMinute());
            Assert.assertEquals(56, lt.getSecond());
            Assert.assertEquals(987_654_321L, lt.getNano());

            Assert.assertFalse(val.isDate());
            Assert.assertFalse(val.isTimeZone());
        }
    }

    @Test
    public void testPlainDateTime() {
        try (Context ctx = getJSContext()) {
            Value val = ctx.eval(ID, "new Temporal.PlainDateTime(1982, 11, 26, 12, 34, 56, 987, 654, 321);");
            LocalDate ld = val.asDate();
            Assert.assertEquals(1982, ld.getYear());
            Assert.assertEquals(Month.NOVEMBER, ld.getMonth());
            Assert.assertEquals(26, ld.getDayOfMonth());

            LocalTime lt = val.asTime();
            Assert.assertEquals(12, lt.getHour());
            Assert.assertEquals(34, lt.getMinute());
            Assert.assertEquals(56, lt.getSecond());
            Assert.assertEquals(987_654_321L, lt.getNano());

            Assert.assertFalse(val.isTimeZone());
        }
    }

    @Test
    public void testZonedDateTime() {
        try (Context ctx = getJSContext()) {
            Value val = ctx.eval(ID, "new Temporal.ZonedDateTime(100_123_456_789n, 'Europe/Vienna');");
            Instant inst = val.asInstant();
            Assert.assertEquals(123_456_789L, inst.getNano());
            Assert.assertEquals(100, inst.getEpochSecond());

            LocalDate ld = val.asDate();
            Assert.assertEquals(1970, ld.getYear());
            Assert.assertEquals(Month.JANUARY, ld.getMonth());
            Assert.assertEquals(1, ld.getDayOfYear());

            LocalTime lt = val.asTime();
            Assert.assertEquals(1, lt.getHour()); // offset of +1 due to timezone
            Assert.assertEquals(1, lt.getMinute());
            Assert.assertEquals(40, lt.getSecond());
            Assert.assertEquals(123_456_789L, lt.getNano());

            ZoneId zid = val.asTimeZone();
            Assert.assertEquals("Europe/Vienna", zid.getId());
        }
    }

    @Test
    public void testDuration() {
        try (Context ctx = getJSContext()) {
            Value val = ctx.eval(ID, "Temporal.Duration.from('PT2H3M4.987654321S');");
            Duration dur = val.asDuration();

            long expectedSeconds = 2 * 60 * 60 + 3 * 60 + 4;
            long expectedNanos = 987654321;
            Assert.assertEquals(0, dur.toDays());
            Assert.assertEquals(4, dur.toSecondsPart());
            Assert.assertEquals(expectedSeconds, dur.toSeconds());
            Assert.assertEquals(expectedNanos, dur.toNanosPart());
            Assert.assertEquals(expectedSeconds * 1_000_000_000 + expectedNanos, dur.toNanos());

            // invalid duration; java.time.Duration does not accept units larger or equals to DAY
            val = ctx.eval(ID, "Temporal.Duration.from('P1Y');");
            Assert.assertFalse(val.isDuration());
            val = ctx.eval(ID, "Temporal.Duration.from('P2M');");
            Assert.assertFalse(val.isDuration());
            val = ctx.eval(ID, "Temporal.Duration.from('P3W');");
            Assert.assertFalse(val.isDuration());
            val = ctx.eval(ID, "Temporal.Duration.from('P4D');");
            Assert.assertFalse(val.isDuration());
        }
    }
}
