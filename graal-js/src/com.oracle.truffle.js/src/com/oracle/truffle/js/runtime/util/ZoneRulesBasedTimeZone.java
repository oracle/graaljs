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
package com.oracle.truffle.js.runtime.util;

import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import com.oracle.truffle.js.runtime.Errors;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.util.Date;

/**
 * Implementation of ICU4J {@code TimeZone} that takes time-zone data from the provided
 * {@code ZoneRules} object (instead of ICU4J tzdb-related data files).
 */
public class ZoneRulesBasedTimeZone extends TimeZone {
    private static final long serialVersionUID = 3774721234048749245L;
    private final ZoneRules rules;

    @SuppressWarnings("deprecation")
    public ZoneRulesBasedTimeZone(String id, ZoneRules rules) {
        super(id);
        this.rules = rules;
    }

    @Override
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        LocalDate date = LocalDate.of((era == GregorianCalendar.BC) ? -year : year, month + 1, day);
        LocalTime time = LocalTime.ofNanoOfDay(1000000L * milliseconds);
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        return toMillis(rules.getOffset(dateTime));
    }

    @Override
    public int getRawOffset() {
        return toMillis(rules.getStandardOffset(Instant.now()));
    }

    @Override
    public boolean useDaylightTime() {
        return !rules.isFixedOffset();
    }

    @Override
    public boolean inDaylightTime(Date date) {
        Instant instant = Instant.ofEpochMilli(date.getTime());
        return !rules.getDaylightSavings(instant).isZero();
    }

    @Override
    public void setRawOffset(int offsetMillis) {
        throw Errors.shouldNotReachHere();
    }

    private static int toMillis(ZoneOffset offset) {
        return offset.getTotalSeconds() * 1000;
    }

}
