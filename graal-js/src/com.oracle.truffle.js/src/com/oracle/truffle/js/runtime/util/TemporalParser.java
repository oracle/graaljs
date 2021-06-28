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

    private static final String patternDate = "^([+-]\\d\\d\\d\\d\\d\\d|\\d\\d\\d\\d)[-]?(\\d\\d)[-]?(\\d\\d)";
    private static final String patternTime = "^(\\d\\d)[:]?((\\d\\d)[:]?((\\d\\d)(\\.([\\d]*)?)?)?)?";
    private static final String patternCalendar = "^(\\[u-ca=([^\\]]*)\\])";
    private static final String patternTimeZoneBracketedAnnotation = "^(\\[([^\\]]*)\\])";
    private static final String patternTimeZoneNumericUTCOffset = "^([+-\\u2212]?)(\\d\\d)[:]?((\\d\\d)[:]?((\\d\\d)(\\.([\\d]*)?)?)?)?";
    private static final String patternDateSpecYearMonth = "^([+-]\\d\\d\\d\\d\\d\\d|\\d\\d\\d\\d)[-]?(\\d\\d)";
    private static final String patternDateSpecMonthDay = "^[-]?(\\d\\d)[-]?(\\d\\d)";
    private static final String patternTimeZoneIANAName = "^(\\w*(/\\w*)*)";

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
    private String timeZoneName;

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
            return result();
        }
        reset();
        if (parseTime()) {
            parseTimeZone();
            return result();
        }
        return null;
    }

    public JSTemporalParserRecord parseYearMonth() {
        reset();
        if (parseDateSpecYearMonth()) {
            return result();
        }
        if (parseDate()) {
            parseDateTimeSeparator();
            parseTime();
            return result();
        }
        return null;
    }

    public JSTemporalParserRecord parseMonthDay() {
        reset();
        if (parseDateSpecMonthDay()) {
            return result();
        }
        if (parseDate()) {
            parseDateTimeSeparator();
            parseTime();
            return result();
        }
        return null;
    }

    public JSTemporalParserRecord parseTimeZoneString() {
        reset();
        // TemporalTimeZoneIdentifier
        if (parseTimeZone()) {
            return result();
        }
        reset();
        if (parseTimeZoneIANAName()) {
            return result();
        }
        reset();
        // TemporalInstantString
        if (parseDate()) {
            parseDateTimeSeparator();
            parseTime();
            parseTimeZone();
            return result();
        }
        return null;
    }

    private boolean parseTimeZoneIANAName() {
        Matcher matcher = createMatch(patternTimeZoneIANAName, rest);
        if (matcher.matches()) {
            this.timeZoneName = matcher.group(1);

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

    public JSTemporalParserRecord result() {
        String z = null;
        return new JSTemporalParserRecord(z, year, month, day, hour, minute, second, fraction, offsetSign, offsetHour, offsetMinute, offsetSecond, offsetFraction, timeZoneName, calendar);
    }

    private void reset() {
        pos = 0;
        rest = input;
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

    private boolean parseTimeZone() {
        Matcher matcher = createMatch(patternTimeZoneNumericUTCOffset, rest);
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
        return parseTimeZoneBracket();
    }

    private boolean parseTimeZoneBracket() {
        Matcher matcher = createMatch(patternTimeZoneBracketedAnnotation, rest);
        if (matcher.matches()) {
            timeZoneName = matcher.group(2);

            move(matcher.end(1));
            return true;
        }
        return false;
    }

    private static Matcher createMatch(String pattern, String input) {
        Pattern patternObj = Pattern.compile(pattern + ".*");
        return patternObj.matcher(input);
    }
}
