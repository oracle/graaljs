/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

    private TemporalConstants() {
    }

    public static final TruffleString TEMPORAL = Strings.constant("Temporal");

    public static final TruffleString YEAR = Strings.YEAR;
    public static final TruffleString MONTH = Strings.MONTH;
    public static final TruffleString MONTH_CODE = Strings.constant("monthCode");
    public static final TruffleString WEEK = Strings.constant("week");
    public static final TruffleString DAY = Strings.DAY;
    public static final TruffleString HOUR = Strings.HOUR;
    public static final TruffleString MINUTE = Strings.MINUTE;
    public static final TruffleString SECOND = Strings.SECOND;
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
    public static final TruffleString ERA = Strings.ERA;
    public static final TruffleString ERA_YEAR = Strings.constant("eraYear");
    public static final TruffleString FRACTIONAL_SECOND_DIGITS = Strings.FRACTIONAL_SECOND_DIGITS;

    public static final TruffleString FIELDS = Strings.constant("fields");
    public static final TruffleString CALENDAR = Strings.CALENDAR;
    public static final TruffleString CALENDAR_NAME = Strings.constant("calendarName");
    public static final TruffleString DAYS_IN_YEAR = Strings.constant("daysInYear");
    public static final TruffleString DAYS_IN_MONTH = Strings.constant("daysInMonth");
    public static final TruffleString DAYS_IN_WEEK = Strings.constant("daysInWeek");
    public static final TruffleString MONTHS_IN_YEAR = Strings.constant("monthsInYear");
    public static final TruffleString IN_LEAP_YEAR = Strings.constant("inLeapYear");

    public static final TruffleString DAY_OF_WEEK = Strings.constant("dayOfWeek");
    public static final TruffleString DAY_OF_YEAR = Strings.constant("dayOfYear");
    public static final TruffleString WEEK_OF_YEAR = Strings.constant("weekOfYear");
    public static final TruffleString YEAR_OF_WEEK = Strings.constant("yearOfWeek");

    public static final TruffleString DATE_FROM_FIELDS = Strings.constant("dateFromFields");
    public static final TruffleString MONTH_DAY_FROM_FIELDS = Strings.constant("monthDayFromFields");
    public static final TruffleString YEAR_MONTH_FROM_FIELDS = Strings.constant("yearMonthFromFields");

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
    public static final TruffleString REJECT = Strings.REJECT;
    public static final TruffleString PREFER = Strings.constant("prefer");
    public static final TruffleString USE = Strings.constant("ure");
    public static final TruffleString IGNORE = Strings.constant("ignore");
    public static final TruffleString OVERFLOW = Strings.constant("overflow");
    public static final TruffleString COMPATIBLE = Strings.constant("compatible");

    public static final TruffleString TIME_ZONE = Strings.TIME_ZONE;
    public static final TruffleString TIME_ZONE_NAME = Strings.TIME_ZONE_NAME;

    public static final TruffleString DATE_UNTIL = Strings.constant("dateUntil");
    public static final TruffleString DATE_ADD = Strings.constant("dateAdd");

    public static final TruffleString UNIT = Strings.UNIT;
    public static final TruffleString SMALLEST_UNIT = Strings.constant("smallestUnit");

    public static final TruffleString AUTO = Strings.constant("auto");
    public static final TruffleString ALWAYS = Strings.constant("always");
    public static final TruffleString NEVER = Strings.constant("never");
    public static final TruffleString CRITICAL = Strings.constant("critical");
    public static final TruffleString EARLIER = Strings.constant("earlier");
    public static final TruffleString LATER = Strings.constant("later");

    public static final TruffleString FLOOR = Strings.constant("floor");
    public static final TruffleString CEIL = Strings.constant("ceil");
    public static final TruffleString EXPAND = Strings.constant("expand");
    public static final TruffleString TRUNC = Strings.constant("trunc");
    public static final TruffleString HALF_FLOOR = Strings.constant("halfFloor");
    public static final TruffleString HALF_CEIL = Strings.constant("halfCeil");
    public static final TruffleString HALF_EXPAND = Strings.constant("halfExpand");
    public static final TruffleString HALF_TRUNC = Strings.constant("halfTrunc");
    public static final TruffleString HALF_EVEN = Strings.constant("halfEven");
    public static final TruffleString ROUNDING_MODE = Strings.ROUNDING_MODE;
    public static final TruffleString ROUNDING_INCREMENT = Strings.ROUNDING_INCREMENT;

    public static final TruffleString MERGE_FIELDS = Strings.constant("mergeFields");
    public static final TruffleString RELATIVE_TO = Strings.constant("relativeTo");
    public static final TruffleString LARGEST_UNIT = Strings.constant("largestUnit");

    public static final TruffleString UTC = Strings.constant("UTC");

    public static final TruffleString DISAMBIGUATION = Strings.constant("disambiguation");
    public static final TruffleString GET_OFFSET_NANOSECONDS_FOR = Strings.constant("getOffsetNanosecondsFor");
    public static final TruffleString GET_POSSIBLE_INSTANTS_FOR = Strings.constant("getPossibleInstantsFor");
    public static final TruffleString NOW = Strings.constant("Now");
    public static final TruffleString TEMPORAL_NOW_TO_STRING_TAG = Strings.constant("Temporal.Now");

    public static final TruffleString MINUS_000000 = Strings.constant("-000000");
    public static final TruffleString ZEROS = Strings.constant("000000000");
    public static final TruffleString OFFSET_ZERO = Strings.constant("+00:00");
    public static final TruffleString U_CA_EQUALS = Strings.constant("u-ca=");

}
