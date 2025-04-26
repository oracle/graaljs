/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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
// method starting with `parse` return a JSTemporalParserRecord, and are allowed to call reset();
// methods starting with `try` are intermediate steps during recursive decent and return a boolean,
// do not call reset();
public final class TemporalParser {

    private static final String patternDate = "^([+\\-]\\d\\d\\d\\d\\d\\d|\\d\\d\\d\\d)[\\-]?(\\d\\d)[\\-]?(\\d\\d)";
    private static final String patternTime = "^([01][0-9]|2[0-3])(:?([0-5][0-9])(?::?([0-5][0-9]|60)(?:[.,]([0-9]{1,9}))?)?)?";
    private static final String patternCalendarName = "^(\\w*)$";
    // Hour 0[0-9]|1[0-9]|2[0-3]
    // MinuteSecond [0-5][0-9]
    // TimeSeparator :?
    // TemporalSign [-+]
    // UTCOffsetMinutePrecision/TimeZoneUTCOffsetName
    // [-+](?:0[0-9]|1[0-9]|2[0-3])(?::?[0-5][0-9])?
    // Alpha [A-Za-z]
    // TZLeadingChar [A-Za-z._]
    // TZChar [A-Za-z._0-9+-]
    // TimeZoneIANANameComponent [A-Za-z._][A-Za-z._0-9+-]*
    // TimeZoneIANAName [A-Za-z._][A-Za-z._0-9+-]*(?:/[A-Za-z._][A-Za-z._0-9+-]*)*
    // TimeZoneIdentifier
    // (?:[-+](?:0[0-9]|1[0-9]|2[0-3])(?::?[0-5][0-9])?)|(?:[A-Za-z._][A-Za-z._0-9+-]*(?:/[A-Za-z._][A-Za-z._0-9+-]*)*)
    // TimeZoneAnnotation
    // [!?(?:[-+](?:0[0-9]|1[0-9]|2[0-3])(?::?[0-5][0-9])?)|(?:[A-Za-z._][A-Za-z._0-9+-]*(?:/[A-Za-z._][A-Za-z._0-9+-]*)*)]
    private static final String patternTimeZoneAnnotation = "(\\[!?((?:[-+](?:0[0-9]|1[0-9]|2[0-3])(?::?[0-5][0-9])?)|(?:[A-Za-z._][A-Za-z._0-9+-]*(?:/[A-Za-z._][A-Za-z._0-9+-]*)*))\\])";
    private static final String patternTimeZoneNumericUTCOffset = "^([+\\-])(\\d\\d):?((\\d\\d):?(?:(\\d\\d)(?:[\\.,]([\\d]*)?)?)?)?";
    private static final String patternDateSpecYearMonth = "^([0-9]{4}|[+-][0-9]{6})-?(0[1-9]|1[0-2])";
    private static final String patternDateSpecMonthDay = "^(?:--)?(0[1-9]|1[012])-?(0[1-9]|[12][0-9]|3[01])";
    private static final String patternTimeZoneIANANameComponent = "^([A-Za-z_]+(/[A-Za-z\\-_]+)*)";
    // LowercaseAlpha [a-z]
    // AKeyLeadingChar [a-z_]
    // AKeyChar [0-9a-z_-]
    // AnnotationKey [a-z_][0-9a-z_-]*
    // AnnotationValueComponent [A-Za-z0-9]+
    // AnnotationValue [A-Za-z0-9]+(?:-[A-Za-z0-9]+)*
    // AnnotationCriticalFlag !
    // Annotation \[(!?)([a-z_][0-9a-z_-]*)=([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)\]
    private static final String patternAnnotation = "(\\[(!?)([a-z_][0-9a-z_-]*)=([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)\\])";

    private final JSContext context;

    private static final TruffleString UC_T = Strings.constant("T");
    private static final TruffleString T = Strings.constant("t");
    private static final TruffleString ETC_GMT = Strings.constant("Etc/GMT");
    private static final TruffleString SIX_ZEROS = Strings.constant("000000");

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

    public JSTemporalParserRecord parseISODateTime() {
        JSTemporalParserRecord rec;

        // TemporalDateTimeString => AnnotatedDateTime
        // TemporalTimeString => AnnotatedDateTime OR AnnotatedTime
        rec = parseAnnotatedDateTime(true, false);
        if (rec != null) {
            return rec;
        }

        rec = parseAnnotatedTime();
        if (rec != null) {
            return rec;
        }

        // TemporalYearMonthString => DateSpecYearMonth OR CalendarDateTime (above already!)
        rec = parseAnnotatedYearMonth();
        if (rec != null) {
            return rec;
        }

        // TemporalMonthDayString
        rec = parseTemporalMonthDayString();
        if (rec != null) {
            return rec;
        }

        // TemporalInstantString
        rec = parseTemporalInstantString();
        if (rec != null) {
            return rec;
        }

        // TemporalZonedDateTimeString => Date TimeSpecSeparator(opt) TimeZoneNameRequired
        // Calendar(opt)
        rec = parseZonedDateTimeString();
        if (rec != null) {
            return rec;
        }

        return null;
    }

    private boolean tryParseTimeDesignator() {
        if (Strings.startsWith(rest, UC_T) || Strings.startsWith(rest, T)) {
            move(1);
            return true;
        }
        return false;
    }

    public JSTemporalParserRecord parseTemporalTimeString() {
        JSTemporalParserRecord rec = parseAnnotatedTime();
        if (rec != null) {
            return rec;
        }

        return parseAnnotatedDateTime(false, true);
    }

    private JSTemporalParserRecord parseAnnotatedTime() {
        reset();

        int start = pos;
        boolean hasTimeDesignator = tryParseTimeDesignator();
        if (tryParseTimeSpec()) {
            tryParseDateTimeUTCOffset(false);
            if (!hasTimeDesignator) {
                int length = pos - start;
                TruffleString timeDateTimeUTCOffset = Strings.lazySubstring(input, start, length);
                // Early errors for AnnotatedTime
                if (createMatch(patternDateSpecYearMonth, timeDateTimeUTCOffset, false).matches()) {
                    return null;
                }
                Matcher matcher = createMatch(patternDateSpecMonthDay, timeDateTimeUTCOffset, false);
                if (matcher.matches() && isValidMonthDay(matcher.group(1), matcher.group(2))) {
                    return null;
                }
            }
            tryParseTimeZoneAnnotation();
            if (parseAnnotations() && atEnd()) {
                return result();
            }
        }

        return null;
    }

    public JSTemporalParserRecord parseAnnotatedDateTime(boolean zoned, boolean timeRequired) {
        reset();
        if (tryParseDateTime(zoned, timeRequired)) {
            tryParseTimeZoneAnnotation();
            if (parseAnnotations() && atEnd()) {
                return result();
            }
        }
        return null;
    }

    private boolean tryParseDateTime(boolean zoned, boolean timeRequired) {
        if (!parseDate()) {
            return false;
        }
        if (!tryParseDateTimeSeparator()) {
            return !timeRequired;
        }
        if (!tryParseTimeSpec()) {
            return false; // DateTimeSeparator without Time
        }
        tryParseDateTimeUTCOffset(zoned);
        return true;
    }

    private boolean parseTimeSpecSeparator() {
        int posBackup = pos;
        TruffleString restBackup = rest;

        if (!tryParseDateTimeSeparator()) {
            return false;
        }
        if (!tryParseTimeSpec()) {
            // we found a separator, but no time.
            pos = posBackup;
            rest = restBackup;
            return false;
        }
        return true;
    }

    // TemporalYearMonthString
    public JSTemporalParserRecord parseYearMonth() {
        JSTemporalParserRecord rec;

        // DateSpecYearMonth
        rec = parseAnnotatedYearMonth();
        if (rec != null) {
            return rec;
        }

        // AnnotatedDateTime
        rec = parseAnnotatedDateTime(false, false);
        if (rec != null) {
            return rec;
        }

        return null;
    }

    private JSTemporalParserRecord parseAnnotatedYearMonth() {
        reset();
        if (tryParseDateSpecYearMonth()) {
            tryParseTimeZoneAnnotation();
            if (parseAnnotations() && atEnd()) {
                return result();
            }
        }
        return null;
    }

    public JSTemporalParserRecord parseTemporalMonthDayString() {
        reset();

        // DateSpecMonthDay or DateTime
        if (tryParseDateSpecMonthDay() || tryParseDateTime(false, false)) {
            // optional TimeZoneAnnotation
            tryParseTimeZoneAnnotation();
            // optional Annotations
            if (parseAnnotations() && atEnd()) {
                return result();
            }
        }

        return null;
    }

    private boolean parseTimeZoneIANAName() {
        TruffleString ianaName = rest;

        // Etc/GMT
        if (Strings.startsWith(rest, ETC_GMT)) {
            move(Strings.length(ETC_GMT));
            if (!rest.isEmpty() && (Strings.charAt(rest, 0) == '+' || Strings.charAt(rest, 0) == '-')) {
                move(1);
                try {
                    int unpaddedHour = rest.parseIntUncached();
                    if (0 <= unpaddedHour && unpaddedHour <= 23) {
                        this.timeZoneIANAName = ianaName;
                        return true;
                    }
                } catch (TruffleString.NumberFormatException e) {
                    // parsingError, intentionally left blank
                }
            }
        }

        reset();
        // TimeZoneIANANameTail
        Matcher matcher = createMatch(patternTimeZoneIANANameComponent, rest);
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
        if (tryParseTimeZoneNumericUTCOffset()) {
            if (atEnd()) { // catches "+00:01.1"
                return result();
            }
        }
        return null;
    }

    // production TemporalCalendarString
    public JSTemporalParserRecord parseCalendarString() {
        JSTemporalParserRecord rec;

        // CalendarName
        rec = parseCalendarName();
        if (rec != null) {
            return rec;
        }

        // TemporalInstantString
        rec = parseTemporalInstantString();
        if (rec != null) {
            return rec;
        }

        // AnnotatedDateTime
        rec = parseAnnotatedDateTime(true, false);
        if (rec != null) {
            return rec;
        }

        // AnnotatedTime
        rec = parseAnnotatedTime();
        if (rec != null) {
            return rec;
        }

        // AnnotatedYearMonth
        rec = parseAnnotatedYearMonth();
        if (rec != null) {
            return rec;
        }

        // TemporalMonthDayString
        rec = parseTemporalMonthDayString();
        if (rec != null) {
            return rec;
        }

        return null;
    }

    public JSTemporalParserRecord parseTemporalInstantString() {
        reset();
        if (parseDate()) {
            if (parseTimeSpecSeparator()) {
                if (tryParseDateTimeUTCOffset(true)) {
                    tryParseTimeZoneAnnotation();
                    if (parseAnnotations() && atEnd()) {
                        return result();
                    }
                }
            }
        }
        return null;
    }

    private JSTemporalParserRecord parseCalendarName() {
        // CalendarName
        reset();
        if (tryParseCalendarName()) {
            return result();
        }
        return null;
    }

    private JSTemporalParserRecord parseZonedDateTimeString() {
        reset();
        if (parseDate()) {
            tryParseDateTimeSeparator();
            tryParseTimeSpec();
            if (!tryParseTimeZoneNameRequired()) {
                return null;
            }
            if (parseAnnotations() && atEnd()) {
                return result();
            }
        }
        return null;
    }

    public boolean isTemporalZonedDateTimeString() {
        return parseZonedDateTimeString() != null;
    }

    public boolean isTemporalDateTimeString() {
        reset();
        JSTemporalParserRecord rec = parseAnnotatedDateTime(true, false);
        if (rec != null) {
            return true;
        }
        return false;
    }

    private boolean tryParseTimeZoneNameRequired() {
        tryParseDateTimeUTCOffset(true); // optional

        if (tryParseTimeZoneAnnotation()) {
            return true;
        }
        return false;
    }

    private boolean tryParseCalendarName() {
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
            return new JSTemporalParserRecord(utcDesignator != null, prepare(year, Long.MAX_VALUE, true), prepare(month, 12), prepare(day, 31), prepare(hour, 23), prepare(minute, 59),
                            prepare(second, 60), fraction, offsetSign, prepare(offsetHour, 23), prepare(offsetMinute, 59), prepare(offsetSecond, 59), offsetFraction, timeZoneIANAName,
                            timeZoneUTCOffsetName, calendar, timeZoneNumericUTCOffset);
        } catch (Exception ex) {
            return null;
        }
    }

    private static long prepare(TruffleString value, long max) {
        return prepare(value, max, false);
    }

    private static long prepare(TruffleString value, long max, boolean canBeNegative) {
        if (value == null) {
            return Long.MIN_VALUE;
        }
        long l = 0;
        try {
            l = Strings.parseLong(value);
        } catch (TruffleString.NumberFormatException e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }
        if ((!canBeNegative && (l < 0)) || l > max) {
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

    private boolean tryParseDateSpecYearMonth() {
        Matcher matcher = createMatch(patternDateSpecYearMonth, rest);
        if (matcher.matches()) {
            year = group(rest, matcher, 1);
            month = group(rest, matcher, 2);
            move(matcher.end(2));
            return true;
        }
        return false;
    }

    private boolean tryParseDateSpecMonthDay() {
        Matcher matcher = createMatch(patternDateSpecMonthDay, rest);
        if (matcher.matches()) {
            month = group(rest, matcher, 1);
            day = group(rest, matcher, 2);

            move(matcher.end(2));
            return true;
        }
        return false;
    }

    private static boolean isValidMonthDay(String dateMonth, String dateDay) {
        if ("31".equals(dateDay) && ("02".equals(dateMonth) || "04".equals(dateMonth) || "06".equals(dateMonth) || "09".equals(dateMonth) || "11".equals(dateMonth))) {
            return false;
        }
        return !("02".equals(dateMonth) && "30".equals(dateDay));
    }

    private boolean parseDate() {
        Matcher matcher = createMatch(patternDate, rest);
        if (matcher.matches()) {
            year = group(rest, matcher, 1);
            month = group(rest, matcher, 2);
            day = group(rest, matcher, 3);
            char yearCh0 = Strings.charAt(year, 0);
            if (yearCh0 == '-' && Strings.startsWith(year, SIX_ZEROS, 1)) {
                // It is a Syntax Error if DateYear is "-000000"
                return false;
            }
            move(matcher.end(3));
            return true;
        }
        return false;
    }

    private boolean tryParseTimeSpec() {
        Matcher matcher = createMatch(patternTime, rest);
        if (matcher.matches()) {
            hour = group(rest, matcher, 1);
            minute = group(rest, matcher, 3);
            second = group(rest, matcher, 4);
            fraction = group(rest, matcher, 5);

            move(matcher.end(2) < 0 ? matcher.end(1) : matcher.end(2));
            return true;
        }
        return false;
    }

    private boolean tryParseDateTimeSeparator() {
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

    private boolean parseAnnotations() {
        assert calendar == null;
        String foundCalendar = null;
        boolean calendarWasCritical = false;
        while (true) {
            Matcher matcher = createMatch(patternAnnotation, rest);
            if (matcher.matches()) {
                String key = matcher.group(3);
                boolean critical = !matcher.group(2).isEmpty();
                if ("u-ca".equals(key)) {
                    if (calendar == null) {
                        foundCalendar = matcher.group(4).toLowerCase();
                        calendar = group(rest, matcher, 4);
                        calendarWasCritical = critical;
                    } else {
                        if (!"iso8601".equals(foundCalendar)) {
                            return false;
                        }
                        if (critical || calendarWasCritical) {
                            return false;
                        }
                    }
                } else {
                    if (critical) {
                        return false;
                    }
                }
                move(matcher.end(1));
            } else {
                break;
            }
        }
        return true;
    }

    private boolean tryParseDateTimeUTCOffset(boolean zoned) {
        if (zoned && tryParseUTCDesignator()) {
            return true;
        }

        if (tryParseTimeZoneNumericUTCOffset()) {
            return true;
        }

        return false;
    }

    private boolean tryParseUTCDesignator() {
        if (Strings.startsWith(rest, Strings.UC_Z) || Strings.startsWith(rest, Strings.Z)) {
            move(1);
            this.utcDesignator = Strings.Z;

            return true;
        }
        return false;
    }

    private boolean tryParseTimeZoneNumericUTCOffset() {
        Matcher matcher = createMatch(patternTimeZoneNumericUTCOffset, rest, true);
        if (matcher.matches()) {
            offsetSign = group(rest, matcher, 1);
            offsetHour = group(rest, matcher, 2);
            offsetMinute = group(rest, matcher, 4);
            offsetSecond = group(rest, matcher, 5);
            offsetFraction = group(rest, matcher, 6);
            timeZoneNumericUTCOffset = Strings.substring(context, rest, matcher.start(1), matcher.end(3) != -1 ? matcher.end(3) : Strings.length(rest));

            if (offsetHour == null) {
                return false;
            }

            // differentiate between "-08" and "-08:00" here!
            move(offsetMinute != null ? matcher.end(3) : matcher.end(2));
            return true;
        }

        return false;
    }

    public JSTemporalParserRecord parseTimeZoneIdentifier() {
        reset();
        if (tryParseTimeZoneIdentifier()) {
            if (atEnd()) {
                return result();
            }
        }
        return null;
    }

    private boolean tryParseTimeZoneIdentifier() {
        // TimeZoneIANAName
        reset();
        if (parseTimeZoneIANAName()) {
            return true;
        }

        // TimeZoneNumericUTCOffset
        reset();
        if (tryParseTimeZoneNumericUTCOffset()) {
            if (offsetSecond != null || offsetFraction != null) {
                return false;
            }
            return true;
        }

        return false;
    }

    private boolean tryParseTimeZoneAnnotation() {
        Matcher matcher = createMatch(patternTimeZoneAnnotation, rest);
        if (matcher.matches()) {
            TruffleString content = group(rest, matcher, 2);
            // content could be UTCOffset or TimeZoneIANAName
            if (isSign(Strings.charAt(content, 0))) {
                timeZoneUTCOffsetName = content;
            } else {
                assert isTZLeadingChar(Strings.charAt(content, 0));
                timeZoneIANAName = content;
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
        return c == '+' || c == '-';
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
