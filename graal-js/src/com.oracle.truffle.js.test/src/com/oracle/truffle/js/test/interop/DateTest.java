/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.Test;

public class DateTest {

    @Test
    public void testDateValue() {
        for (ZoneId timeZone : new ZoneId[]{ZoneId.systemDefault(), ZoneId.of("UTC+9")}) {
            try (Context context = Context.newBuilder(ID).timeZone(timeZone).build()) {
                Value date = context.eval(ID, "new Date('2019-07-02 13:37');");
                assertTrue(date.isInstant());
                assertTrue(date.isDate());
                assertTrue(date.isTime());
                assertTrue(date.isTimeZone());
                ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2019, Month.JULY, 2, 13, 37), timeZone);
                assertEquals(expected.toInstant(), date.asInstant());
                assertEquals(expected.toLocalDate(), date.asDate());
                assertEquals(expected.toLocalTime(), date.asTime());
                assertEquals(timeZone, date.asTimeZone());
            }
        }
    }

    @Test
    public void testImportDate() {
        for (ZoneId timeZone : new ZoneId[]{ZoneId.systemDefault(), ZoneId.of("UTC+9")}) {
            try (Context context = Context.newBuilder(ID).timeZone(timeZone).build()) {
                Value toJSDate = context.eval(ID, "(date) => new Date(date);");
                ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2019, Month.JULY, 2, 13, 37), timeZone);
                for (int i = 0; i < 3; i++) {
                    Object foreignDate;
                    if (i == 0) {
                        foreignDate = expected.toInstant();
                    } else if (i == 1) {
                        foreignDate = Date.from(expected.toInstant());
                    } else {
                        foreignDate = expected;
                    }
                    Value date = toJSDate.execute(foreignDate);
                    assertFalse(date.isHostObject());
                    assertTrue(date.isInstant());
                    assertTrue(date.isDate());
                    assertTrue(date.isTime());
                    assertEquals(expected.toInstant(), date.asInstant());
                    assertEquals(expected.toLocalDate(), date.asDate());
                    assertEquals(expected.toLocalTime(), date.asTime());
                }
            }
        }
    }

    @Test
    public void testJavaInterop() {
        for (ZoneId timeZone : new ZoneId[]{ZoneId.systemDefault(), ZoneId.of("UTC+9")}) {
            try (Context context = Context.newBuilder(ID).timeZone(timeZone).allowHostAccess(HostAccess.ALL).build()) {
                DateConsumer consumer = new DateConsumer();
                context.getBindings(ID).putMember("consumer", consumer);
                context.eval(ID, "var date = new Date('2019-07-02 13:37');");
                context.eval(ID, "consumer.acceptInstant(date);");
                context.eval(ID, "consumer.acceptLocalDate(date);");
                context.eval(ID, "consumer.acceptLocalTime(date);");
                context.eval(ID, "consumer.acceptLocalDateTime(date);");
                ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2019, Month.JULY, 2, 13, 37), timeZone);
                assertEquals(expected.toInstant(), consumer.instant);
                assertEquals(expected.toLocalDate(), consumer.localDate);
                assertEquals(expected.toLocalTime(), consumer.localTime);
                assertEquals(expected.toLocalDateTime(), consumer.localDateTime);
            }
        }
    }

    public static class DateConsumer {
        Instant instant;
        LocalDate localDate;
        LocalTime localTime;
        LocalDateTime localDateTime;

        public void acceptInstant(Instant date) {
            instant = date;
        }

        public void acceptLocalDate(LocalDate date) {
            localDate = date;
        }

        public void acceptLocalTime(LocalTime date) {
            localTime = date;
        }

        public void acceptLocalDateTime(LocalDateTime date) {
            localDateTime = date;
        }
    }
}
