/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Strings;

public final class TemporalConstants {
    public static final TruffleString TEMPORAL = Strings.constant("Temporal");

    public static final TruffleString YEAR = Strings.constant("year");
    public static final TruffleString MONTH = Strings.constant("month");
    public static final TruffleString MONTH_CODE = Strings.constant("monthCode");
    public static final TruffleString WEEK = Strings.constant("week");
    public static final TruffleString DAY = Strings.constant("day");
    public static final TruffleString HOUR = Strings.constant("hour");
    public static final TruffleString MINUTE = Strings.constant("minute");
    public static final TruffleString SECOND = Strings.constant("second");
    public static final TruffleString MILLISECOND = Strings.constant("millisecond");
    public static final TruffleString MICROSECOND = Strings.constant("microsecond");
    public static final TruffleString NANOSECOND = Strings.constant("nanosecond");

    public static final TruffleString YEARS = Strings.constant("years");
    public static final TruffleString MONTHS = Strings.constant("months");
    public static final TruffleString WEEKS = Strings.constant("weeks");
    public static final TruffleString DAYS = Strings.constant("days");
    public static final TruffleString HOURS = Strings.constant("hours");
    public static final TruffleString MINUTES = Strings.constant("minutes");
    public static final TruffleString SECONDS = Strings.constant("seconds");
    public static final TruffleString MILLISECONDS = Strings.constant("milliseconds");
    public static final TruffleString MICROSECONDS = Strings.constant("microseconds");
    public static final TruffleString NANOSECONDS = Strings.constant("nanoseconds");

    public static final TruffleString OFFSET = Strings.constant("offset");
    public static final TruffleString ERA = Strings.constant("era");
    public static final TruffleString ERA_YEAR = Strings.constant("eraYear");
    public static final TruffleString OFFSET_NANOSECONDS = Strings.constant("offsetNanoseconds");

    public static final TruffleString FIELDS = Strings.constant("fields");
    public static final TruffleString CALENDAR = Strings.constant("calendar");
    public static final TruffleString DAYS_IN_YEAR = Strings.constant("daysInYear");
    public static final TruffleString DAYS_IN_MONTH = Strings.constant("daysInMonth");
    public static final TruffleString DAYS_IN_WEEK = Strings.constant("daysInWeek");
    public static final TruffleString MONTHS_IN_YEAR = Strings.constant("monthsInYear");
    public static final TruffleString HOURS_IN_DAY = Strings.constant("hoursInDay");
    public static final TruffleString IN_LEAP_YEAR = Strings.constant("inLeapYear");

    public static final TruffleString DAY_OF_WEEK = Strings.constant("dayOfWeek");
    public static final TruffleString DAY_OF_YEAR = Strings.constant("dayOfYear");
    public static final TruffleString WEEK_OF_YEAR = Strings.constant("weekOfYear");

    public static final TruffleString DATE_FROM_FIELDS = Strings.constant("dateFromFields");
    public static final TruffleString REFERENCE_ISO_DAY = Strings.constant("referenceIsoDay");
    public static final TruffleString REFERENCE_ISO_YEAR = Strings.constant("referenceIsoYear");

    public static final TruffleString ISO_DAY = Strings.constant("isoDay");
    public static final TruffleString ISO_HOUR = Strings.constant("isoHour");
    public static final TruffleString ISO_MICROSECOND = Strings.constant("isoMicrosecond");
    public static final TruffleString ISO_MILLISECOND = Strings.constant("isoMillisecond");
    public static final TruffleString ISO_MINUTE = Strings.constant("isoMinute");
    public static final TruffleString ISO_MONTH = Strings.constant("isoMonth");
    public static final TruffleString ISO_NANOSECOND = Strings.constant("isoNanosecond");
    public static final TruffleString ISO_SECOND = Strings.constant("isoSecond");
    public static final TruffleString ISO_YEAR = Strings.constant("isoYear");

    public static final TruffleString PLAIN_DATE = Strings.constant("plainDate");
    public static final TruffleString PLAIN_TIME = Strings.constant("plainTime");

    public static final TruffleString ISO8601 = Strings.constant("iso8601");
    public static final TruffleString GREGORY = Strings.constant("gregory");
    public static final TruffleString JAPANESE = Strings.constant("japanese");

    public static final TruffleString CONSTRAIN = Strings.constant("constrain");
    public static final TruffleString REJECT = Strings.constant("reject");
    public static final TruffleString PREFER = Strings.constant("prefer");
    public static final TruffleString USE = Strings.constant("ure");
    public static final TruffleString IGNORE = Strings.constant("ignore");
    public static final TruffleString OVERFLOW = Strings.constant("overflow");
    public static final TruffleString COMPATIBLE = Strings.constant("compatible");

    public static final TruffleString TIME_ZONE = Strings.constant("timeZone");
    public static final TruffleString TIME_ZONE_OFFSET = Strings.constant("timeZoneOffset");
    public static final TruffleString TIME_ZONE_IANA_NAME = Strings.constant("timeZoneIANAName");
    public static final TruffleString TIME_ZONE_NAME = Strings.constant("timeZoneName");

    public static final TruffleString DATE_UNTIL = Strings.constant("dateUntil");
    public static final TruffleString DATE_ADD = Strings.constant("dateAdd");

    public static final TruffleString PRECISION = Strings.constant("Precision");
    public static final TruffleString INCREMENT = Strings.constant("Increment");
    public static final TruffleString UNIT = Strings.constant("unit");
    public static final TruffleString SMALLEST_UNIT = Strings.constant("smallestUnit");

    public static final TruffleString AUTO = Strings.constant("auto");
    public static final TruffleString ALWAYS = Strings.constant("always");
    public static final TruffleString NEVER = Strings.constant("never");
    public static final TruffleString EARLIER = Strings.constant("earlier");
    public static final TruffleString LATER = Strings.constant("later");

    public static final TruffleString FLOOR = Strings.constant("floor");
    public static final TruffleString CEIL = Strings.constant("ceil");
    public static final TruffleString TRUNC = Strings.constant("trunc");
    public static final TruffleString HALF_EXPAND = Strings.constant("halfExpand");
    public static final TruffleString ROUNDING_MODE = Strings.constant("roundingMode");
    public static final TruffleString ROUNDING_INCREMENT = Strings.constant("roundingIncrement");

    public static final TruffleString SIGN = Strings.constant("sign");
    public static final TruffleString BLANK = Strings.constant("blank");
    public static final TruffleString ID = Strings.constant("id");

    public static final TruffleString MERGE_FIELDS = Strings.constant("mergeFields");
    public static final TruffleString RELATIVE_TO = Strings.constant("relativeTo");
    public static final TruffleString LARGEST_UNIT = Strings.constant("largestUnit");

    public static final TruffleString EPOCH_SECONDS = Strings.constant("epochSeconds");
    public static final TruffleString EPOCH_MILLISECONDS = Strings.constant("epochMilliseconds");
    public static final TruffleString EPOCH_MICROSECONDS = Strings.constant("epochMicroseconds");
    public static final TruffleString EPOCH_NANOSECONDS = Strings.constant("epochNanoseconds");

    public static final TruffleString UTC = Strings.constant("UTC");

    public static final TruffleString DISAMBIGUATION = Strings.constant("disambiguation");
    public static final TruffleString NOW = Strings.constant("Now");

    public static final TruffleString GLOBAL_PLAIN_TIME = Strings.constant("PlainTime");
    public static final TruffleString GLOBAL_PLAIN_DATE = Strings.constant("PlainDate");
    public static final TruffleString GLOBAL_PLAIN_DATE_TIME = Strings.constant("PlainDateTime");
    public static final TruffleString GLOBAL_DURATION = Strings.constant("Duration");
    public static final TruffleString GLOBAL_CALENDAR = Strings.constant("Calendar");
    public static final TruffleString GLOBAL_PLAIN_YEAR_MONTH = Strings.constant("PlainYearMonth");
    public static final TruffleString GLOBAL_PLAIN_MONTH_DAY = Strings.constant("PlainMonthDay");
    public static final TruffleString GLOBAL_TIME_ZONE = Strings.constant("TimeZone");
    public static final TruffleString GLOBAL_INSTANT = Strings.constant("Instant");
    public static final TruffleString GLOBAL_ZONED_DATE_TIME = Strings.constant("ZonedDateTime");
    public static final TruffleString GLOBAL_TEMPORAL_NOW = Strings.constant("Temporal.Now");
}
