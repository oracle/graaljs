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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
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

    private final JSContext context;
    private final TruffleString input;
    private TruffleString rest;
    private int pos;

    // results
    private TruffleString year;
    private TruffleString month;
    private TruffleString day;
    private TruffleString hour;
    private TruffleString minute;
    private TruffleString second;
    private TruffleString fraction;

    private TruffleString calendar;
    private TruffleString timeZoneIANAName;
    private TruffleString timeZoneUTCOffsetName;
    private TruffleString timeZoneNumericUTCOffset;
    private TruffleString timeZoneEtcName;
    private TruffleString utcDesignator;

    private TruffleString offsetSign;
    private TruffleString offsetHour;
    private TruffleString offsetMinute;
    private TruffleString offsetSecond;
    private TruffleString offsetFraction;

    public TemporalParser(TruffleString input) {
        context = JavaScriptLanguage.get(null).getJSContext();
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
            this.timeZoneIANAName = group(rest, matcher, 1);

            assert timeZoneIANAName == null || isTZLeadingChar(Strings.charAt(timeZoneIANAName, 0));

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
        return Strings.indexOf(rest, '[') >= 0 && Strings.indexOf(rest, ']') >= 0 && parseTimeZone();
    }

    private boolean parseCalendarName() {
        Matcher matcher = createMatch(patternCalendarName, rest);
        if (matcher.matches()) {
            this.calendar = group(rest, matcher, 1);

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
                            timeZoneUTCOffsetName, calendar, timeZoneNumericUTCOffset);
        } catch (Exception ex) {
            return null;
        }
    }

    private static long prepare(TruffleString value, long max) {
        if (value == null) {
            return Long.MIN_VALUE;
        }
        long l = 0;
        try {
            l = Strings.parseLong(value);
        } catch (TruffleString.NumberFormatException e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }
        if (l < 0 || l > max) {
            throw new RuntimeException("date value out of bounds");
        }
        return l;
    }

    private boolean atEnd() {
        return pos >= Strings.length(input);
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
        timeZoneNumericUTCOffset = null;
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
        // using unconditional lazy substrings because "rest" doesn't escape the parser
        rest = (pos >= 0 && Strings.length(input) > pos) ? Strings.lazySubstring(input, pos) : Strings.EMPTY_STRING;
    }

    private boolean parseDateSpecYearMonth() {
        Matcher matcher = createMatch(patternDateSpecYearMonth, rest);
        if (matcher.matches()) {
            year = group(rest, matcher, 1);
            month = group(rest, matcher, 2);
            if (Strings.charAt(year, 0) == '\u2212') {
                year = Strings.concat(Strings.DASH, Strings.lazySubstring(year, 1));
            }
            move(matcher.end(2));
            return true;
        }
        return false;
    }

    private boolean parseDateSpecMonthDay() {
        Matcher matcher = createMatch(patternDateSpecMonthDay, rest);
        if (matcher.matches()) {
            month = group(rest, matcher, 1);
            day = group(rest, matcher, 2);

            move(matcher.end(2));
            return true;
        }
        return false;
    }

    private boolean parseDate() {
        Matcher matcher = createMatch(patternDate, rest);
        if (matcher.matches()) {
            year = group(rest, matcher, 1);
            month = group(rest, matcher, 2);
            day = group(rest, matcher, 3);
            if (Strings.charAt(year, 0) == '\u2212') {
                year = Strings.concat(Strings.DASH, Strings.lazySubstring(year, 1));
            }
            move(matcher.end(3));
            return true;
        }
        return false;
    }

    private boolean parseTime() {
        Matcher matcher = createMatch(patternTime, rest);
        if (matcher.matches()) {
            hour = group(rest, matcher, 1);
            minute = group(rest, matcher, 3);
            second = group(rest, matcher, 5);
            fraction = group(rest, matcher, 7);

            move(matcher.end(2));
            return true;
        }
        return false;
    }

    private boolean parseDateTimeSeparator() {
        if (Strings.length(rest) <= 0) {
            return false;
        }
        char ch = Strings.charAt(rest, 0);
        if (ch == 't' || ch == 'T' || ch == ' ') {
            move(1);
            return true;
        }
        return false;
    }

    private boolean parseCalendar() {
        Matcher matcher = createMatch(patternCalendar, rest);
        if (matcher.matches()) {
            calendar = group(rest, matcher, 2);

            move(matcher.end(1));
            return true;
        }
        return false;
    }

    private boolean parseTimeZoneIdentifier() {
        // TimeZOneNumericUTCOffset
        Matcher matcher = createMatch(patternTimeZoneNumericUTCOffset, rest, false);
        if (matcher.matches()) {
            offsetSign = group(rest, matcher, 1);
            offsetHour = group(rest, matcher, 2);
            offsetMinute = group(rest, matcher, 4);
            offsetSecond = group(rest, matcher, 6);
            offsetFraction = group(rest, matcher, 8);
            timeZoneNumericUTCOffset = Strings.substring(context, rest, matcher.start(1), matcher.end(3) != -1 ? matcher.end(3) : Strings.length(rest));

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
            offsetSign = group(rest, matcher, 1);
            offsetHour = group(rest, matcher, 2);
            offsetMinute = group(rest, matcher, 4);
            offsetSecond = group(rest, matcher, 6);
            offsetFraction = group(rest, matcher, 8);
            timeZoneNumericUTCOffset = Strings.substring(context, rest, matcher.start(1), matcher.end(3) != -1 ? matcher.end(3) : Strings.length(rest));

            if (offsetHour == null) {
                return false;
            }

            move(matcher.end(3));

            if (parseTimeZoneBracket()) {
                // there might still be a calendar
                return true;
            }

            if ((rest == null || Strings.length(rest) == 0)) {
                return true;
            }
        }

        if (Strings.startsWith(rest, Strings.UC_Z) || Strings.startsWith(rest, Strings.Z)) {
            move(1);
            this.timeZoneIANAName = TemporalConstants.UTC; // TODO is this correct?
            this.utcDesignator = Strings.UC_Z;

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
            TruffleString content = group(rest, matcher, 2);

            // content could be TimeZoneIANAName, Etc/GMT, or TimeZOneUTCOffsetName
            if (content != null) {
                if (Strings.startsWith(content, Strings.UC_ETC)) {
                    timeZoneEtcName = content;
                } else if (isSign(Strings.charAt(content, 0))) {
                    timeZoneUTCOffsetName = content;
                } else {
                    assert isTZLeadingChar(Strings.charAt(content, 0));
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

    private static Matcher createMatch(String pattern, TruffleString input) {
        return createMatch(pattern, input, true);
    }

    private static Matcher createMatch(String pattern, TruffleString input, boolean addMatchAll) {
        Pattern patternObj = Pattern.compile(pattern + (addMatchAll ? ".*" : ""));
        return patternObj.matcher(Strings.toJavaString(input));
    }

    private TruffleString group(TruffleString string, Matcher matcher, int groupNumber) {
        int start = matcher.start(groupNumber);
        return start < 0 ? null : Strings.substring(context, string, start, matcher.end(groupNumber) - start);
    }
}
