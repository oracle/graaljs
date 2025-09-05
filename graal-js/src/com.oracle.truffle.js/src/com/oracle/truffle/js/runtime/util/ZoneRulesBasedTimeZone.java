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
package com.oracle.truffle.js.runtime.util;

import org.graalvm.shadowed.com.ibm.icu.util.GregorianCalendar;
import org.graalvm.shadowed.com.ibm.icu.util.BasicTimeZone;
import org.graalvm.shadowed.com.ibm.icu.util.InitialTimeZoneRule;
import org.graalvm.shadowed.com.ibm.icu.util.TimeZoneRule;
import org.graalvm.shadowed.com.ibm.icu.util.TimeZoneTransition;
import com.oracle.truffle.js.runtime.Errors;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.Date;
import java.util.List;

/**
 * Implementation of ICU4J {@code TimeZone} that takes time-zone data from the provided
 * {@code ZoneRules} object (instead of ICU4J tzdb-related data files).
 */
public class ZoneRulesBasedTimeZone extends BasicTimeZone {
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
        List<ZoneOffset> offsets = rules.getValidOffsets(dateTime);
        ZoneOffset offset;
        if (offsets.size() == 1) {
            offset = offsets.get(0);
        } else {
            offset = rules.getTransition(dateTime).getOffsetAfter();
        }
        return toMillis(offset);
    }

    @Override
    public int getOffset(long date) {
        return toMillis(rules.getOffset(Instant.ofEpochMilli(date)));
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

    @Override
    public TimeZoneTransition getNextTransition(long base, boolean inclusive) {
        return toTimeZoneTransition(rules.nextTransition(Instant.ofEpochMilli(base - (inclusive ? 1 : 0))));
    }

    @Override
    public TimeZoneTransition getPreviousTransition(long base, boolean inclusive) {
        return toTimeZoneTransition(rules.previousTransition(Instant.ofEpochMilli(base + (inclusive ? 1 : 0))));
    }

    private TimeZoneTransition toTimeZoneTransition(ZoneOffsetTransition transition) {
        if (transition == null) {
            return null;
        }
        int before = toMillis(transition.getOffsetBefore());
        int after = toMillis(transition.getOffsetAfter());
        int rawOffset = Math.min(before, after);
        int dstSavings = Math.max(before, after) - rawOffset;
        TimeZoneRule from = new InitialTimeZoneRule(getID(), rawOffset, (before == rawOffset) ? 0 : dstSavings);
        TimeZoneRule to = new InitialTimeZoneRule(getID(), rawOffset, (after == rawOffset) ? 0 : dstSavings);
        return new TimeZoneTransition(transition.getInstant().toEpochMilli(), from, to);
    }

    @Override
    public void getOffsetFromLocal(long date,
                    LocalOption nonExistingTimeOpt, LocalOption duplicatedTimeOpt, int[] offsets) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(Math.floorDiv(date, 1000), Math.floorMod(date, 1000) * 1_000_000, ZoneOffset.UTC);
        List<ZoneOffset> validOffsets = rules.getValidOffsets(dateTime);
        ZoneOffset offset;
        if (validOffsets.size() == 1) {
            offset = validOffsets.get(0);
        } else {
            LocalOption timeOpt = validOffsets.isEmpty() ? nonExistingTimeOpt : duplicatedTimeOpt;
            ZoneOffsetTransition transition = rules.getTransition(dateTime);
            offset = switch (timeOpt) {
                case FORMER -> transition.getOffsetBefore();
                case LATTER -> transition.getOffsetAfter();
                default -> throw new UnsupportedOperationException();
            };
        }
        offsets[0] = toMillis(offset);
    }

    @Override
    public TimeZoneRule[] getTimeZoneRules() {
        throw new UnsupportedOperationException();
    }

}
