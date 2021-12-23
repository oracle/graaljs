/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalParserRecord;

/**
 * This class holds all the date/time/timezone/calender parsing of Temporal. This is all the code
 * that is mentioned by grammar in the spec, but where no explicit code is given. If there is code
 * expressed in terms of the spec, it should not be here (but in TemporalUtil, etc.).
 */
public final class TemporalParser {

    private static final String patternDate = "^([+\\-\\u2212]\\d\\d\\d\\d\\d\\d|\\d\\d\\d\\d)[\\-]?(\\d\\d)[\\-]?(\\d\\d)";
    private static final String patternTime = "^(\\d\\d):?((\\d\\d):?((\\d\\d)([\\.,]([\\d]*)?)?)?)?";
    private static final String patternCalendar = "^(\\[u-ca=([^\\]]*)\\])";
    private static final String patternCalendarName = "^(\\w*)$";
    private static final String patternTimeZoneBracketedAnnotation = "^(\\[([^\\]]*)\\])";
    private static final String patternTimeZoneNumericUTCOffset = "^([+\\-\\u2212])(\\d\\d):?((\\d\\d):?((\\d\\d)([\\.,]([\\d]*)?)?)?)?";
    private static final String patternDateSpecYearMonth = "^([+\\-\\u2212]\\d\\d\\d\\d\\d\\d|\\d\\d\\d\\d)[\\-]?(\\d\\d)";
    private static final String patternDateSpecMonthDay = "^[\\-]?(\\d\\d)[\\-]?(\\d\\d)";
    private static final String patternTimeZoneIANAName = "^([A-Za-z_]+(/[A-Za-z\\-_]+)*)";

    private final String input;
    private String rest;
    private int pos;

    // results
    private String year;
    private String month;
    private String day;
    private String hour;
    private String minute;
    private String second;
    private String fraction;

    private String calendar;
    private String timeZoneIANAName;
    private String timeZoneUTCOffsetName;
    private String timeZoneEtcName;
    private String utcDesignator;

    private String offsetSign;
    private String offsetHour;
    private String offsetMinute;
    private String offsetSecond;
    private String offsetFraction;

    public TemporalParser(String input) {
        this.input = input;
    }

    // TODO this needs to be improved!
    // should accept: Time, DateTime, or CalendarDateTime
    // timeZone might be parsed as part of that, but is ignored afterwards.
    public JSTemporalParserRecord parseISODateTime() {
        reset();
        if (parseDate()) {
            parseDateTimeSeparator();
            parseTime();
            parseTimeZone();
            parseCalendar();
            if (atEnd()) {
                return result();
            }
        }
        reset();
        if (parseTime()) {
            parseTimeZone();
            parseCalendar();
            if (atEnd()) {
                return result();
            }
        }
        return null;
    }

    // TemporalYearMonthString
    public JSTemporalParserRecord parseYearMonth() {
        // DateSpecYearMonth
        reset();
        if (parseDateSpecYearMonth()) {
            if (atEnd()) {
                return result();
            }
        }
        // CalendarDateTime
        reset();
        if (parseDate()) {
            parseDateTimeSeparator();
            parseTime();
            parseTimeZone();
            parseCalendar();
            if (atEnd()) {
                return result();
            }
        }
        return null;
    }

    public JSTemporalParserRecord parseMonthDay() {
        // DateSpecMonthDay
        reset();
        if (parseDateSpecMonthDay()) {
            if (atEnd()) {
                return result();
            }
        }
        // CalendarDateTime
        reset();
        if (parseDate()) {
            parseDateTimeSeparator();
            parseTime();
            parseTimeZone();
            parseCalendar();
            if (atEnd()) {
                return result();
            }
        }
        return null;
    }

    public JSTemporalParserRecord parseTimeZoneString() {
        reset();
        // TemporalTimeZoneIdentifier
        if (parseTimeZoneIdentifier()) {
            return result();
        }

        reset();
        if (parseDate()) {
            parseDateTimeSeparator();
            parseTime();
            if (parseTimeZone()) {
                parseCalendar();
                return result();
            }
        }
        return null;
    }

    private boolean parseTimeZoneIANAName() {
        Matcher matcher = createMatch(patternTimeZoneIANAName, rest);
        if (matcher.matches()) {
            this.timeZoneIANAName = matcher.group(1);

            assert timeZoneIANAName == null || isTZLeadingChar(timeZoneIANAName.charAt(0));

            move(matcher.end(1));
            return true;
        }
        return false;
    }

    public JSTemporalParserRecord parseTimeZoneNumericUTCOffset() {
        reset();
        if (parseTimeZone()) {
            return result();
        }
        return null;
    }

    // production TemporalCalendarString
    public JSTemporalParserRecord parseCalendarString() {
        // CalendarName
        reset();
        if (parseCalendarName()) {
            return result();
        }

        // TemporalInstantString
        reset();
        try {
            JSTemporalParserRecord rec1 = parseISODateTime();
            if (rec1 != null) {
                JSTemporalParserRecord rec2 = parseTimeZoneString();
                if (rec1.getCalendar() != null) {
                    this.calendar = rec1.getCalendar();
                }
                if (rec2 != null && rec2.getCalendar() != null) {
                    this.calendar = rec2.getCalendar();
                }
                return result();
            }
        } catch (Exception ex) {
            // fallthrough ignored
        }

        // TODO CalendarDateTime
        // TODO Time
        // TODO DateSpecYearMonth
        // TODO DateSpecMonthDay
        return null;
    }

    public boolean isTemporalZonedDateTimeString() {
        reset();
        if (parseDate()) {
            parseDateTimeSeparator();
            parseTime();
            if (!parseTimeZoneNameRequired()) {
                return false;
            }
            parseCalendar();
            return true;
        }

        return false;
    }

    private boolean parseTimeZoneNameRequired() {
        // this effectively is rule `TimeZoneNameRequired`
        return rest.contains("[") && rest.contains("]") && parseTimeZone();
    }

    private boolean parseCalendarName() {
        Matcher matcher = createMatch(patternCalendarName, rest);
        if (matcher.matches()) {
            this.calendar = matcher.group(1);

            move(matcher.end(1));
            return true;
        }
        return false;
    }

    public JSTemporalParserRecord result() {
        try {
            // TODO MinuteSecond has 59 seconds, TimeSecond has 60 seconds
            return new JSTemporalParserRecord(utcDesignator != null, prepare(year, Long.MAX_VALUE), prepare(month, 12), prepare(day, 31), prepare(hour, 23), prepare(minute, 59), prepare(second, 60),
                            fraction, offsetSign, prepare(offsetHour, 23), prepare(offsetMinute, 59), prepare(offsetSecond, 59), offsetFraction, timeZoneIANAName, timeZoneEtcName,
                            timeZoneUTCOffsetName, calendar);
        } catch (Exception ex) {
            return null;
        }
    }

    private static long prepare(String value, long max) {
        if (value == null) {
            return Long.MIN_VALUE;
        }
        long l = Long.parseLong(value);
        if (l < 0 || l > max) {
            throw new RuntimeException("date value out of bounds");
        }
        return l;
    }

    private boolean atEnd() {
        return pos >= input.length();
    }

    private void reset() {
        pos = 0;
        rest = input;

        year = null;
        month = null;
        day = null;
        hour = null;
        minute = null;
        second = null;
        fraction = null;

        calendar = null;
        timeZoneIANAName = null;
        timeZoneUTCOffsetName = null;
        timeZoneEtcName = null;
        utcDesignator = null;

        offsetSign = null;
        offsetHour = null;
        offsetMinute = null;
        offsetSecond = null;
        offsetFraction = null;
    }

    private void move(int newPos) {
        pos += newPos;
        rest = (pos >= 0 && input.length() > pos) ? input.substring(pos) : "";
    }

    private boolean parseDateSpecYearMonth() {
        Matcher matcher = createMatch(patternDateSpecYearMonth, rest);
        if (matcher.matches()) {
            year = matcher.group(1);
            month = matcher.group(2);
            if (year.charAt(0) == '\u2212') {
                year = "-" + year.substring(1);
            }
            move(matcher.end(2));
            return true;
        }
        return false;
    }

    private boolean parseDateSpecMonthDay() {
        Matcher matcher = createMatch(patternDateSpecMonthDay, rest);
        if (matcher.matches()) {
            month = matcher.group(1);
            day = matcher.group(2);

            move(matcher.end(2));
            return true;
        }
        return false;
    }

    private boolean parseDate() {
        Matcher matcher = createMatch(patternDate, rest);
        if (matcher.matches()) {
            year = matcher.group(1);
            month = matcher.group(2);
            day = matcher.group(3);
            if (year.charAt(0) == '\u2212') {
                year = "-" + year.substring(1);
            }
            move(matcher.end(3));
            return true;
        }
        return false;
    }

    private boolean parseTime() {
        Matcher matcher = createMatch(patternTime, rest);
        if (matcher.matches()) {
            hour = matcher.group(1);
            minute = matcher.group(3);
            second = matcher.group(5);
            fraction = matcher.group(7);

            move(matcher.end(2));
            return true;
        }
        return false;
    }

    private boolean parseDateTimeSeparator() {
        if (rest.length() <= 0) {
            return false;
        }
        char ch = rest.charAt(0);
        if (ch == 't' || ch == 'T' || ch == ' ') {
            move(1);
            return true;
        }
        return false;
    }

    private boolean parseCalendar() {
        Matcher matcher = createMatch(patternCalendar, rest);
        if (matcher.matches()) {
            calendar = matcher.group(2);

            move(matcher.end(1));
            return true;
        }
        return false;
    }

    private boolean parseTimeZoneIdentifier() {
        // TimeZOneNumericUTCOffset
        Matcher matcher = createMatch(patternTimeZoneNumericUTCOffset, rest, false);
        if (matcher.matches()) {
            offsetSign = matcher.group(1);
            offsetHour = matcher.group(2);
            offsetMinute = matcher.group(4);
            offsetSecond = matcher.group(6);
            offsetFraction = matcher.group(8);

            move(matcher.end(3));

            parseTimeZoneBracket();
            return true;
        }

        // TimeZoneIANAName
        if (parseTimeZoneIANAName()) {
            return true;
        }

        return false;
    }

    private boolean parseTimeZone() {
        // first two options are from `TimeZoneOffsetRequired` (with bracket optional)
        Matcher matcher = createMatch(patternTimeZoneNumericUTCOffset, rest, true);
        if (matcher.matches()) {
            offsetSign = matcher.group(1);
            offsetHour = matcher.group(2);
            offsetMinute = matcher.group(4);
            offsetSecond = matcher.group(6);
            offsetFraction = matcher.group(8);

            if (offsetHour == null) {
                return false;
            }

            move(matcher.end(3));

            if (parseTimeZoneBracket()) {
                // there might still be a calendar
                return true;
            }

            if ((rest == null || rest.length() == 0)) {
                return true;
            }
        }

        if (rest.startsWith("Z") || rest.startsWith("z")) {
            move(1);
            this.timeZoneIANAName = TemporalConstants.UTC; // TODO is this correct?
            this.utcDesignator = "Z";

            parseTimeZoneBracket(); // optional
            return true;
        }

        // last option is from `TimeZoneNameRequired`
        // (the optional numeric offset was parsed in first option)
        if (parseTimeZoneBracket()) {
            return true;
        }

        return false;
    }

    private boolean parseTimeZoneBracket() {
        Matcher matcher = createMatch(patternTimeZoneBracketedAnnotation, rest);
        if (matcher.matches()) {
            String content = matcher.group(2);

            // content could be TimeZoneIANAName, Etc/GMT, or TimeZOneUTCOffsetName
            if (content != null) {
                if (content.startsWith("Etc")) {
                    timeZoneEtcName = content;
                } else if (isSign(content.charAt(0))) {
                    timeZoneUTCOffsetName = content;
                } else {
                    assert isTZLeadingChar(content.charAt(0));
                    timeZoneIANAName = content;
                }
            }

            move(matcher.end(1));
            return true;
        }
        return false;
    }

    private static boolean isTZLeadingChar(char c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '.' || c == '_';
    }

    private static boolean isSign(char c) {
        return c == '+' || c == '-' || c == '\u2212';
    }

    private static Matcher createMatch(String pattern, String input) {
        return createMatch(pattern, input, true);
    }

    private static Matcher createMatch(String pattern, String input, boolean addMatchAll) {
        Pattern patternObj = Pattern.compile(pattern + (addMatchAll ? ".*" : ""));
        return patternObj.matcher(input);
    }

}
