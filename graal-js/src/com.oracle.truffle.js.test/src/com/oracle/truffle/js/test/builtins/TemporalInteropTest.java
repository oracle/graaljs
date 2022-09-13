/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class TemporalInteropTest extends JSTest {

    @Ignore // dont know why it fails
    @Test
    public void testInstantGetter() {
        String code = "javaInst.epochMilliseconds;";

        java.time.Instant inst = Instant.ofEpochMilli(100_000_000);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(100_000_000L, result.asLong());
        }
    }

    @Test
    public void testInstantAdd() {
        String code = "javaInst.add(Temporal.Duration.from('PT1H')).epochMilliseconds;";

        java.time.Instant inst = Instant.ofEpochMilli(100_000_000);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(103_600_000L, result.asLong());
        }
    }

    @Test
    public void testInstantSubtract() {
        String code = "javaInst.subtract(Temporal.Duration.from('PT1H')).epochMilliseconds;";

        java.time.Instant inst = Instant.ofEpochMilli(100_000_000);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(96_400_000L, result.asLong());
        }
    }
    @Ignore // arity error
    @Test
    public void testInstantUntil() {
        String code = "javaInst.until(new Temporal.Instant(BigInt(3_000_000))).milliseconds;";

        java.time.Instant inst = Instant.ofEpochMilli(1);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(2, result.asLong());
        }
    }

    @Test
    public void testInstantSince() {
        String code = "javaInst.since(new Temporal.Instant(BigInt(1_000_000))).milliseconds;";

        java.time.Instant inst = Instant.ofEpochMilli(3);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(2, result.asLong());
        }
    }

    @Test
    public void testInstantSince2() {
        String code = "new Temporal.Instant(BigInt(3_000_000)).since(javaInst).milliseconds;";

        java.time.Instant inst = Instant.ofEpochMilli(1);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(2, result.asLong());
        }
    }

    @Test
    public void testInstantUntil2() {
        String code = "new Temporal.Instant(BigInt(1_000_000)).until(javaInst).milliseconds;";

        java.time.Instant inst = Instant.ofEpochMilli(3);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(2, result.asLong());
        }
    }

    @Test
    public void testInstantRound() {
        String code = "javaInst.round('second').epochSeconds;";

        java.time.Instant inst = Instant.ofEpochMilli(1_000_203);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(1000, result.asLong());
        }
    }

    @Test
    public void testInstantEquals() {
        String code = "new Temporal.Instant(BigInt(100_000_000)).equals(javaInst);";

        java.time.Instant inst = Instant.ofEpochMilli(100);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(true, result.asBoolean());
        }
    }
    @Ignore // arity error
    @Test
    public void testInstantEquals2() {
        String code = "javaInst.equals(new Temporal.Instant(BigInt(100_000_000)));";

        java.time.Instant inst = Instant.ofEpochMilli(100);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testInstantToStringOnlyJS() {
        String code = "new Temporal.Instant(BigInt(1_574_074_321_816_000_000)).toString();";

        //java.time.Instant inst = Instant.ofEpochMilli(123_456_789);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            //ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2019-11-18T10:52:01.816Z", result.toString());
        }
    }

    @Test
    public void testInstantToString() {
        String code = "javaInst.toString();";

        java.time.Instant inst = Instant.ofEpochMilli(1_574_074_321_816L);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2019-11-18T10:52:01.816Z", result.toString());
        }
    }

    @Test
    public void testInstantToZonedDateTime() {
        String code = "timeZone = Temporal.TimeZone.from('Asia/Tokyo'); " +
                "calendar = Temporal.Calendar.from('japanese'); " +
                "javaInst.toZonedDateTime({ timeZone, calendar }).toString();";

        java.time.Instant inst = Instant.ofEpochMilli(0);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("1970-01-01T09:00:00+09:00[Asia/Tokyo][u-ca=japanese]", result.toString());
        }
    }

    @Test
    public void testInstantToZonedDateTimeISO() {
        String code = "javaInst.toZonedDateTimeISO('UTC').year";

        java.time.Instant inst = Instant.ofEpochMilli(0);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaInst", inst);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(1970, result.asLong());
        }
    }

    // ============================================================================================================== //
    // Temporal Duration
    // ============================================================================================================== //

    @Ignore
    @Test
    public void testDurationGetter() {
        String code = "javaDur.hours;";

        java.time.Duration dur = Duration.ofHours(12);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDur", dur);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(12, result.asLong());
        }
    }

    @Test
    public void testDurationWith() {
        String code = "javaDur.with({hours: 50}).hours;";

        java.time.Duration dur = Duration.ofHours(12);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDur", dur);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(50, result.asLong());
        }
    }

    @Test
    public void testDurationAdd() {
        String code = "javaDur.add({hours: 50}).hours";

        java.time.Duration dur = Duration.ofHours(12);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDur", dur);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(62, result.asLong());
        }
    }

    @Test
    public void testDurationSubtract() {
        String code = "javaDur.subtract({hours: 5}).hours";

        java.time.Duration dur = Duration.ofHours(12);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDur", dur);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(7, result.asLong());
        }
    }
    @Ignore // negated() does exist for java Duration.
    @Test
    public void testDurationNegated() {
        String code = "javaDur.negated().hours";

        java.time.Duration dur = Duration.ofHours(12);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDur", dur);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(-12, result.asLong());
        }
    }

    @Ignore // abs() exists for java Duration
    @Test
    public void testDurationAbs() {
        String code = "javaDur.abs().hours";

        java.time.Duration dur = Duration.ofHours(-12);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDur", dur);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(12, result.asLong());
        }
    }

    @Test
    public void testDurationRound() {
        String code = "javaDur.round('minute').minutes";

        java.time.Duration dur = Duration.ofSeconds(95); // same as 1minute and 35 seconds, rounded up to 2 minutes

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDur", dur);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(2, result.asLong());
        }
    }

    @Test
    public void testDurationTotal() {
        String code = "javaDur.total({unit: 'second'})";

        java.time.Duration dur = Duration.ofMinutes(2);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDur", dur);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(120, result.asLong());
        }
    }

    @Test
    public void testDurationToString() {
        String code = "javaDur.toString();";

        java.time.Duration dur = Duration.ofHours(10);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDur", dur);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("PT10H", result.toString());
        }
    }

    @Test
    public void testDurationToJSON() {
        String code = "javaDur.toJSON();";

        java.time.Duration dur = Duration.ofHours(10);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDur", dur);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("PT10H", result.toString());
        }
    }

    @Test
    public void testDurationToLocaleString() {
        String code = "javaDur.toLocaleString();";

        java.time.Duration dur = Duration.ofHours(10);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDur", dur);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("PT10H", result.toString());
        }
    }

    @Test(expected = RuntimeException.class)
    public void testDurationValueOf() {
        String code = "javaDur.valueOf();";

        java.time.Duration dur = Duration.ofHours(10);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDur", dur);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("PT10H", result.toString());
        }
    }

    // ============================================================================================================== //
    // Temporal PlainDate
    // ============================================================================================================== //

    @Ignore // Unsupported operation identifier 'adjustInto' and  object '{day: 1}'(language: JavaScript, type: Object). Identifier is not executable or instantiable.
    @Test
    public void testPlainDateWith() {
        //String code = "Temporal.PlainDate.from('2006-01-24').with({day: 1}).toString();";
        String code = "javaDate.with({day: 1}).toString();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2006-01-01", result.toString());
        }
    }
    @Test
    public void testPlainDateWithCalendar() {
        String code = "javaDate.withCalendar('gregory').toString();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2000-01-24[u-ca=gregory]", result.toString());
        }
    }

    @Test
    public void testPlainDateAdd() {
        String code = "javaDate.add({years: 22}).toString();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2022-01-24", result.toString());
        }
    }

    @Test
    public void testPlainDateSubtract() {
        String code = "javaDate.subtract({years: 10}).toString();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("1990-01-24", result.toString());
        }
    }

    @Test
    public void testPlainDateUntil() {
        //String code = "javaDate.until(Temporal.PlainDate.from('2000-01-25')).toString();"; // arity error
        String code = "Temporal.PlainDate.from('2000-01-24').until(javaDate).toString();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 25);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("P1D", result.toString());
        }
    }

    @Test
    public void testPlainDateSince() {
        String code = "javaDate.since(Temporal.PlainDate.from('2000-01-24')).toString();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 25);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("P1D", result.toString());
        }
    }

    @Test
    public void testPlainDateEquals() {
        //String code = "javaDate.equals(Temporal.PlainDate.from('2000-01-24')).toString();"; // Arity Error
        String code = "Temporal.PlainDate.from('2000-01-24').equals(javaDate);";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testPlainDateToString() {
        String code = "javaDate.toString();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2000-01-24", result.toString());
        }
    }

    @Test
    public void testPlainDateToLocaleString() {
        String code = "javaDate.toLocaleString();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2000-01-24", result.toString());
        }
    }

    @Test
    public void testPlainDateToJSON() {
        String code = "javaDate.toJSON();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2000-01-24", result.toString());
        }
    }

    @Test(expected = RuntimeException.class)
    public void testPlainDateValueOf() {
        String code = "javaDate.valueOf();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2000-01-24", result.toString());
        }
    }

    @Test
    public void testPlainDateToZonedDateTime() {
        String code = "Temporal.PlainDate.from('2000-01-24').toZonedDateTime({ timeZone: 'America/Los_Angeles' }).toString();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2000-01-24T00:00:00-08:00[America/Los_Angeles]", result.toString());
        }
    }

    @Test
    public void testPlainDateToPlainDateTime() {
        String code = "javaDate.toPlainDateTime(Temporal.PlainTime.from('15:30:30')).toString();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2000-01-24T15:30:30", result.toString());
        }
    }

    @Test
    public void testPlainDateToPlainYearMonth() {
        String code = "javaDate.toPlainYearMonth().toString();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2000-01", result.toString());
        }
    }

    @Test
    public void testPlainDateToPlainMonthDay() {
        String code = "javaDate.toPlainMonthDay().toString();";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("01-24", result.toString());
        }
    }

    @Test
    public void testPlainDateGetISOFields() {
        String code = "javaDate.getISOFields().isoDay;";

        java.time.LocalDate date = LocalDate.of(2000, 1, 24);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaDate", date);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("24", result.toString());
        }
    }

    // ============================================================================================================== //
    // Temporal TimeZone
    // ============================================================================================================== //

    @Test
    public void testTimeZoneGetOffsetNanosecondsFor() {
        String code = "javaZone.getOffsetNanosecondsFor(Temporal.Instant.fromEpochSeconds(1_553_993_100));";

        java.time.ZoneId zone = ZoneId.of("-08:00");

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaZone", zone);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(-28_800_000_000_000L, result.asLong());
        }
    }

    @Test
    public void testTimeZonegetOffsetStringFor() {
        String code = "javaZone.getOffsetStringFor(Temporal.Instant.fromEpochSeconds(1_553_993_100));";

        java.time.ZoneId zone = ZoneId.of("-08:00");

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaZone", zone);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("-08:00", result.toString());
        }
    }

    @Test
    public void testTimeZoneGetPlainDateTimeFor() {
        String code = "javaZone.getPlainDateTimeFor(Temporal.Instant.fromEpochSeconds(1_553_993_100)).toString();";

        java.time.ZoneId zone = ZoneId.of("Europe/Berlin");

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaZone", zone);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2019-03-31T01:45:00", result.toString());
        }
    }

    @Test
    public void testTimeZoneGetInstantFor() {
        String code = "javaZone.getInstantFor(Temporal.PlainDateTime.from('2019-03-31T01:45:00')).toString();";

        java.time.ZoneId zone = ZoneId.of("Europe/Berlin");

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaZone", zone);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2019-03-31T00:45:00Z", result.toString());
        }
    }

    @Test
    public void testTimeZoneGetPossibleInstantsFor() {
        String code = "javaZone.getPossibleInstantsFor(Temporal.PlainDateTime.from('2019-03-31T01:45:00')).toString();";

        java.time.ZoneId zone = ZoneId.of("Europe/Berlin");

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaZone", zone);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2019-03-31T00:45:00Z", result.toString());
        }
    }

    @Test
    public void testTimeZoneGetNextTransition() {
        String code = "javaZone.getNextTransition(Temporal.Instant.fromEpochSeconds(1_553_993_100)).toString();";

        java.time.ZoneId zone = ZoneId.of("Europe/Berlin");

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaZone", zone);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2019-03-31T01:00:00Z", result.toString());
        }
    }

    @Test
    public void testTimeZoneGetPreviousTransition() {
        String code = "javaZone.getPreviousTransition(Temporal.Instant.fromEpochSeconds(1_553_993_100)).toString();";

        java.time.ZoneId zone = ZoneId.of("Europe/Berlin");

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaZone", zone);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2018-10-28T01:00:00Z", result.toString());
        }
    }

    @Test
    public void testTimeZoneToString() {
        String code = "javaZone.toString();";

        java.time.ZoneId zone = ZoneId.of("Europe/Berlin");

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaZone", zone);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("Europe/Berlin", result.toString());
        }
    }

    @Test
    public void testTimeZoneToJSON() {
        String code = "javaZone.toJSON();";

        java.time.ZoneId zone = ZoneId.of("Europe/Berlin");

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaZone", zone);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("Europe/Berlin", result.toString());
        }
    }

    // ============================================================================================================== //
    // Temporal PlainTime
    // ============================================================================================================== //

    @Ignore // Unsupported operation identifier 'adjustInto' and  object '{hours: 12}'(language: JavaScript, type: Object). Identifier is not executable or instantiable.
    @Test
    public void testPlainTimeWith() {
        String code = "javaTime.with({hours: 12}).toString();";
        //String code = "Temporal.PlainTime.from('10:30:05').with({hours: 12}).toString();";

        java.time.LocalTime time = LocalTime.of(10, 30, 5);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("12:30:05", result.toString());
        }
    }

    @Test
    public void testPlainTimeAdd() {
        String code = "javaTime.add({hours: 12}).toString();";
        //String code = "Temporal.PlainTime.from('10:30:05').add({hours: 12}).toString();";

        java.time.LocalTime time = LocalTime.of(10, 30, 5);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("22:30:05", result.toString());
        }
    }

    @Test
    public void testPlainTimeSubtract() {
        String code = "javaTime.subtract({hours: 2}).toString();";
        //String code = "Temporal.PlainTime.from('10:30:05').subtract({hours: 2}).toString();";

        java.time.LocalTime time = LocalTime.of(10, 30, 5);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("08:30:05", result.toString());
        }
    }
    @Ignore // Arity Error
    @Test
    public void testPlainTimeUntil() {
        String code = "javaTime.until(Temporal.PlainTime.from('11:00:00')).toString();";
        //String code = "Temporal.PlainTime.from('10:30:00').until(Temporal.PlainTime.from('11:00:00')).toString();";

        java.time.LocalTime time = LocalTime.of(10, 30);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("PT30M", result.toString());
        }
    }

    @Test
    public void testPlainTimeSince() {
        String code = "javaTime.since(Temporal.PlainTime.from('10:00:00')).toString();";
        //String code = "Temporal.PlainTime.from('10:30:00').since(Temporal.PlainTime.from('10:00:00')).toString();";

        java.time.LocalTime time = LocalTime.of(10, 30);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("PT30M", result.toString());
        }
    }

    @Test
    public void testPlainTimeRound() {
        String code = "javaTime.round({smallestUnit: 'hour'}).toString();";
        //String code = "Temporal.PlainTime.from('10:30:05').round({smallestUnit: 'hour'}).toString();";

        java.time.LocalTime time = LocalTime.of(10, 30, 5);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("11:00:00", result.toString());
        }
    }

    @Test
    public void testPlainTimeEquals() {
        //String code = "javaTime.equals(Temporal.PlainTime.from('10:30:05'));";
        //String code = "Temporal.PlainTime.from('10:30:05').equals(Temporal.PlainTime.from('10:30:05'));";
        String code = "Temporal.PlainTime.from('10:30:05').equals(javaTime);";

        java.time.LocalTime time = LocalTime.of(10, 30, 5);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(true, result.asBoolean());
        }
    }

    @Test
    public void testPlainTimeToString() {
        String code = "javaTime.toString();";
        //String code = "Temporal.PlainTime.from('10:30:05').toString();";

        java.time.LocalTime time = LocalTime.of(10, 30, 5);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("10:30:05", result.toString());
        }
    }

    @Test
    public void testPlainTimeToLocaleString() {
        String code = "javaTime.toLocaleString();";
        //String code = "Temporal.PlainTime.from('10:30:05').toLocaleString();";

        java.time.LocalTime time = LocalTime.of(10, 30, 5);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("10:30:05", result.toString());
        }
    }

    @Test
    public void testPlainTimeToJSON() {
        String code = "javaTime.toJSON();";
        //String code = "Temporal.PlainTime.from('10:30:05').toJSON();";

        java.time.LocalTime time = LocalTime.of(10, 30, 5);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("10:30:05", result.toString());
        }
    }

    @Test(expected = RuntimeException.class)
    public void testPlainTimeValueOf() {
        String code = "javaTime.valueOf();";
        //String code = "Temporal.PlainTime.from('10:30:05').valueOf();";

        java.time.LocalTime time = LocalTime.of(10, 30, 5);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("10:30:05", result.toString());
        }
    }

    @Test
    public void testPlainTimeToZonedDateTime() {
        String code = "javaTime.toZonedDateTime({ timeZone: 'America/Los_Angeles', plainDate: Temporal.PlainDate.from('2006-08-24') }).toString();";

        java.time.LocalTime time = LocalTime.of(10, 30, 5);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2006-08-24T10:30:05-07:00[America/Los_Angeles]", result.toString());
        }
    }

    @Test
    public void testPlainTimeToPlainDateTime() {
        String code = "javaTime.toPlainDateTime(Temporal.PlainDate.from('2000-01-01')).toString();";
        //String code = "Temporal.PlainTime.from('10:30:05').toPlainDateTime(Temporal.PlainDate.from('2000-01-01')).toString();";

        java.time.LocalTime time = LocalTime.of(10, 30, 5);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals("2000-01-01T10:30:05", result.toString());
        }
    }

    @Test
    public void testPlainTimeGetISOFields() {
        String code = "javaTime.getISOFields().isoHour;";
        //String code = "Temporal.PlainTime.from('10:30:05').getISOFields().isoHour;";

        java.time.LocalTime time = LocalTime.of(10, 30, 5);

        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).//
                option(JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME, "true").//
                option(JSContextOptions.TEMPORAL_NAME, "true").build()) {
            ctx.getBindings(ID).putMember("javaTime", time);
            Value result = ctx.eval(ID, code);
            Assert.assertEquals(10, result.asLong());
        }
    }
}