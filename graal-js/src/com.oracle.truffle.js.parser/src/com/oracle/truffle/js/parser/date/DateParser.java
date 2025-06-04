/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.js.parser.date;

import static java.lang.Character.DECIMAL_DIGIT_NUMBER;
import static java.lang.Character.LOWERCASE_LETTER;
import static java.lang.Character.OTHER_PUNCTUATION;
import static java.lang.Character.SPACE_SEPARATOR;
import static java.lang.Character.UPPERCASE_LETTER;

import java.util.HashMap;
import java.util.Locale;

import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;

// @formatter:off
/**
 * JavaScript date parser. This class first tries to parse a date string
 * according to the extended ISO 8601 format specified in ES5 15.9.1.15.
 * If that fails, it falls back to legacy mode in which it accepts a range
 * of different formats.
 *
 * <p>This class is neither thread-safe nor reusable. Calling the
 * <tt>parse()</tt> method more than once will yield undefined results.</p>
 */
public class DateParser {

    /** Constant for index position of parsed year value. */
    public static final int YEAR        = 0;
    /** Constant for index position of parsed month value. */
    public static final int MONTH       = 1;
    /** Constant for index position of parsed day value. */
    public static final int DAY         = 2;
    /** Constant for index position of parsed hour value. */
    public static final int HOUR        = 3;
    /** Constant for index position of parsed minute value. */
    public static final int MINUTE      = 4;
    /** Constant for index position of parsed second value. */
    public static final int SECOND      = 5;
    /** Constant for index position of parsed millisecond value. */
    public static final int MILLISECOND = 6;
    /** Constant for index position of parsed time zone offset value. */
    public static final int TIMEZONE    = 7;

    private enum Token {
        UNKNOWN, NUMBER, SEPARATOR, PARENTHESIS, NAME, SIGN, END
    }

    private final String string;
    private final int length;
    private final Integer[] fields;
    private int pos = 0;
    private Token token;
    private int tokenLength;
    private Name nameValue;
    private int numValue;
    private int currentField = YEAR;
    private int yearSign = 0;
    private boolean namedMonth = false;
    private final JSRealm realm;
    private final boolean extraLenient; //necessary for Temporal

    private static final HashMap<String, Name> names = new HashMap<>();

    static {
        addName("monday", Name.DAY_OF_WEEK, 0);
        addName("tuesday", Name.DAY_OF_WEEK, 0);
        addName("wednesday", Name.DAY_OF_WEEK, 0);
        addName("thursday", Name.DAY_OF_WEEK, 0);
        addName("friday", Name.DAY_OF_WEEK, 0);
        addName("saturday", Name.DAY_OF_WEEK, 0);
        addName("sunday", Name.DAY_OF_WEEK, 0);
        addName("january", Name.MONTH_NAME, 1);
        addName("february", Name.MONTH_NAME, 2);
        addName("march", Name.MONTH_NAME, 3);
        addName("april", Name.MONTH_NAME, 4);
        addName("may", Name.MONTH_NAME, 5);
        addName("june", Name.MONTH_NAME, 6);
        addName("july", Name.MONTH_NAME, 7);
        addName("august", Name.MONTH_NAME, 8);
        addName("september", Name.MONTH_NAME, 9);
        addName("october", Name.MONTH_NAME, 10);
        addName("november", Name.MONTH_NAME, 11);
        addName("december", Name.MONTH_NAME, 12);
        addName("am", Name.AM_PM, 0);
        addName("pm", Name.AM_PM, 12);
        addName("z", Name.TIMEZONE_ID, 0);
        addName("gmt", Name.TIMEZONE_ID, 0);
        addName("ut", Name.TIMEZONE_ID, 0);
        addName("utc", Name.TIMEZONE_ID, 0);
        addName("est", Name.TIMEZONE_ID, -5 * 60);
        addName("edt", Name.TIMEZONE_ID, -4 * 60);
        addName("cst", Name.TIMEZONE_ID, -6 * 60);
        addName("cdt", Name.TIMEZONE_ID, -5 * 60);
        addName("mst", Name.TIMEZONE_ID, -7 * 60);
        addName("mdt", Name.TIMEZONE_ID, -6 * 60);
        addName("pst", Name.TIMEZONE_ID, -8 * 60);
        addName("pdt", Name.TIMEZONE_ID, -7 * 60);
        addName("t", Name.TIME_SEPARATOR, 0);
    }

    /**
     * Construct a new <tt>DateParser</tt> instance for parsing the given string.
     * @param string the string to be parsed
     */
    public DateParser(final JSRealm realm, final String string, boolean extraLenient) {
        this.string = string;
        this.length = string.length();
        this.fields = new Integer[TIMEZONE + 1];
        this.realm = realm;
        this.extraLenient = extraLenient;
    }

    /**
     * Try parsing the given string as date according to the extended ISO 8601 format
     * specified in ES5 15.9.1.15. Fall back to legacy mode if that fails.
     * This method returns <tt>true</tt> if the string could be parsed.
     * @return true if the string could be parsed as date
     */
    public boolean parse() {
        return parseEcmaDate() || parseLegacyDate();
    }

    /**
     * Try parsing the date string according to the rules laid out in ES5 15.9.1.15.
     * The date string must conform to the following format:
     *
     * <pre>  [('-'|'+')yy]yyyy[-MM[-dd]][Thh:mm[:ss[.sss]][Z|(+|-)hh:mm]] </pre>
     *
     * <p>If the string does not contain a time zone offset, the <tt>TIMEZONE</tt> field
     * is set to <tt>0</tt> (GMT).</p>
     * @return true if string represents a valid ES5 date string.
     */
    public boolean parseEcmaDate() {

        if (token == null) {
            token = next();
        }

        while (token != Token.END) {

            switch (token) {
                case NUMBER:
                    if (currentField == YEAR && yearSign != 0) {
                        // 15.9.1.15.1 Extended year must have six digits
                        if (tokenLength != 6) {
                            return false;
                        }
                        if (numValue == 0 && yearSign == -1) {
                            // The representation of the year 0 as -000000 is invalid
                            return false;
                        }
                        numValue *= yearSign;
                    } else if (!checkEcmaField(currentField, numValue)) {
                        return false;
                    }
                    if (!skipEcmaDelimiter()) {
                        return false;
                    }
                    if (currentField < TIMEZONE) {
                        set(currentField++, numValue);
                    }
                    break;

                case NAME:
                    if (nameValue == null) {
                        return false;
                    }
                    switch (nameValue.type) {
                        case Name.TIME_SEPARATOR:
                            if (currentField == YEAR || currentField > HOUR) {
                                return false;
                            }
                            currentField = HOUR;
                            break;
                        case Name.TIMEZONE_ID:
                            if (!nameValue.key.equals("z") || !setTimezone(nameValue.value, false)) {
                                return false;
                            }
                            break;
                        default:
                            return false;
                    }
                    break;

                case SIGN:
                    if (peek() == -1) {
                        // END after sign - wrong!
                        return false;
                    }

                    if (currentField == YEAR) {
                        yearSign = numValue;
                    } else if (currentField < SECOND || !setTimezone(readTimeZoneOffset(), true)) {
                        // Note: Spidermonkey won't parse timezone unless time includes seconds and milliseconds
                        return false;
                    }
                    break;

                default:
                    return false;
            }
            token = next();
        }

        return patchResult(true);
    }

    /**
     * Try parsing the date using a fuzzy algorithm that can handle a variety of formats.
     *
     * <p>Numbers separated by <tt>':'</tt> are treated as time values, optionally followed by a
     * millisecond value separated by <tt>'.'</tt>. Other number values are treated as date values.
     * The exact sequence of day, month, and year values to apply is determined heuristically.</p>
     *
     * <p>English month names and selected time zone names as well as AM/PM markers are recognized
     * and handled properly. Additionally, numeric time zone offsets such as <tt>(+|-)hh:mm</tt> or
     * <tt>(+|-)hhmm</tt> are recognized. If the string does not contain a time zone offset
     * the <tt>TIMEZONE</tt>field is left undefined, meaning the local time zone should be applied.</p>
     *
     * <p>English weekday names are recognized but ignored. All text in parentheses is ignored as well.
     * All other text causes parsing to fail.</p>
     *
     * @return true if the string could be parsed
     */
    public boolean parseLegacyDate() {

        if (currentField > DAY) {
            return false;
        }
        if (token == null) {
            token = next();
        }

        while (token != Token.END) {

            switch (token) {
                case NUMBER:
                    if (skipDelimiter(':')) {
                        // A number followed by ':' is parsed as time
                        if (!setTimeField(numValue)) {
                            return false;
                        }
                        // consume remaining time tokens
                        do {
                            token = next();
                            if (!((token == Token.NUMBER && setTimeField(numValue))
                                    || ((token == Token.END || token == Token.SEPARATOR) && setTimeField(0)))) {
                                return false;
                            }
                        } while (isSet(SECOND) ? (skipDelimiter('.') || skipDelimiter(':')) : skipDelimiter(':'));

                    } else {
                        // Parse as date token
                        if (!setDateField(numValue)) {
                            return false;
                        }
                        skipDelimiter('-');
                    }
                    break;

                case NAME:
                    if (nameValue == null) {
                        return false;
                    }
                    switch (nameValue.type) {
                        case Name.AM_PM:
                            if (!setAmPm(nameValue.value)) {
                                return false;
                            }
                            break;
                        case Name.MONTH_NAME:
                            if (!setMonth(nameValue.value)) {
                                return false;
                            }
                            break;
                        case Name.TIMEZONE_ID:
                            if (!setTimezone(nameValue.value, false)) {
                                return false;
                            }
                            break;
                        case Name.TIME_SEPARATOR:
                            return false;
                        default:
                            break;
                    }
                    if (nameValue.type != Name.TIMEZONE_ID) {
                        skipDelimiter('-');
                    }
                    break;

                case SIGN:
                    if (peek() == -1) {
                        // END after sign - wrong!
                        return false;
                    }

                    if (!setTimezone(readTimeZoneOffset(), true)) {
                        return false;
                    }
                    break;

                case PARENTHESIS:
                    if (!skipParentheses()) {
                        return false;
                    }
                    break;

                case SEPARATOR:
                    break;

                default:
                    return false;
            }
            token = next();
        }

        return patchResult(false);
    }

    /**
     * Get the parsed date and time fields as an array of <tt>Integers</tt>.
     *
     * <p>If parsing was successful, all fields are guaranteed to be set except for the
     * <tt>TIMEZONE</tt> field which may be <tt>null</tt>, meaning that local time zone
     * offset should be applied.</p>
     *
     * @return the parsed date fields
     */
    public Integer[] getDateFields() {
        return fields;
    }

    private boolean isSet(final int field) {
        return fields[field] != null;
    }

    private Integer get(final int field) {
        return fields[field];
    }

    private void set(final int field, final int value) {
        fields[field] = value;
    }

    private int peek() {
        return pos < length ? string.charAt(pos) : -1;
    }

    // Skip delimiter if followed by a number. Used for ISO 8601 formatted dates
    private boolean skipNumberDelimiter(final char c) {
        if (pos < length - 1 && string.charAt(pos) == c
                && Character.getType(string.charAt(pos + 1)) == DECIMAL_DIGIT_NUMBER) {
            token = null;
            pos++;
            return true;
        }
        return false;
    }

    private boolean skipDelimiter(final char c) {
        if (pos < length && string.charAt(pos) == c) {
            token = null;
            pos++;
            return true;
        }
        return false;
    }

    private Token next() {
        if (pos >= length) {
            tokenLength = 0;
            return Token.END;
        }

        final char c = string.charAt(pos);

        final int type = Character.getType(c);
        if (c > 0x80 && type != SPACE_SEPARATOR) {
            tokenLength = 1;
            pos++;
            return Token.UNKNOWN; // We only deal with ASCII here
        }

        switch (type) {
            case DECIMAL_DIGIT_NUMBER:
                numValue = readNumber(9);
                if (pos < length && isAsciiDigit(string.charAt(pos))) {
                    return Token.UNKNOWN; // number longer than 9 digits
                }
                return Token.NUMBER;
            case SPACE_SEPARATOR:
            case OTHER_PUNCTUATION:
                tokenLength = 1;
                pos++;
                return Token.SEPARATOR;
            case UPPERCASE_LETTER:
            case LOWERCASE_LETTER:
                nameValue = readName();
                return Token.NAME;
            default:
                tokenLength = 1;
                pos++;
                switch (c) {
                    case '(':
                        return Token.PARENTHESIS;
                    case '-':
                    case '+':
                        numValue = c == '-' ? -1 : 1;
                        return Token.SIGN;
                    default:
                        return Character.isWhitespace(c) ? Token.SEPARATOR : Token.UNKNOWN;
                }
        }
    }

    private boolean checkLegacyField(final int field, final int value) {
        switch (field) {
            case HOUR:
                return isHour(value);
            case MINUTE:
            case SECOND:
                return isMinuteOrSecond(value);
            case MILLISECOND:
                return checkMilliseconds(value);
            default:
                // skip validation on other legacy fields as we don't know what's what
                return true;
        }
    }

    private boolean checkEcmaField(final int field, final int value) {
        switch (field) {
            case YEAR:
                return tokenLength == 4;
            case MONTH:
                return tokenLength == 2 && isMonth(value);
            case DAY:
                return tokenLength == 2 && isDay(value);
            case HOUR:
                return tokenLength == 2 && isHour(value);
            case MINUTE:
            case SECOND:
                return tokenLength == 2 && isMinuteOrSecond(value);
            case MILLISECOND:
                return checkMilliseconds(value);
            default:
                return true;
        }
    }

    private boolean checkMilliseconds(final int value) {
        if (value < 0) {
            return false;
        }
        // convert numValue to milliseconds (i.e. to length 3)
        int currentLength = tokenLength;
        while (currentLength < 3) {
            numValue *= 10;
            currentLength++;
        }
        while (currentLength > 3) {
            numValue /= 10;
            currentLength--;
        }
        return true;
    }

    private boolean skipEcmaDelimiter() {
        switch (currentField) {
            case YEAR:
            case MONTH:
                return skipNumberDelimiter('-') || peek() == 'T' || peek() == -1;
            case DAY:
                return peek() == 'T' || peek() == -1;
            case HOUR:
            case MINUTE:
                return skipNumberDelimiter(':') || endOfTime();
            case SECOND:
                return skipNumberDelimiter('.') || endOfTime();
            default:
                return true;
        }
    }

    private boolean endOfTime() {
        final int c = peek();
        return c == -1 || c == 'Z' || c == '-' || c == '+' || c == ' ';
    }

    private static boolean isAsciiLetter(final char ch) {
        return ('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z');
    }

    private static boolean isAsciiDigit(final char ch) {
        return '0' <= ch && ch <= '9';
    }

    private int readNumber(final int maxDigits) {
        final int start = pos;
        int n = 0;
        final int max = Math.min(length, pos + maxDigits);
        while (pos < max && isAsciiDigit(string.charAt(pos))) {
            n = n * 10 + string.charAt(pos++) - '0';
        }
        tokenLength = pos - start;
        return n;
    }

    private Name readName() {
        final int start = pos;
        final int limit = Math.min(pos + 3, length);

        // first read up to the key length
        while (pos < limit && isAsciiLetter(string.charAt(pos))) {
            pos++;
        }
        final String key = string.substring(start, pos).toLowerCase(Locale.ENGLISH);
        final Name name = names.get(key);
        // then advance to end of name
        while (pos < length && isAsciiLetter(string.charAt(pos))) {
            pos++;
        }

        tokenLength = pos - start;
        // make sure we have the full name or a prefix
        if (name != null && name.matches(string, start, tokenLength)) {
            return name;
        }
        return null;
    }

    private int readTimeZoneOffset() {
        final int sign = string.charAt(pos - 1) == '+' ? 1 : -1;
        int hours = readNumber(2);
        boolean delimiter = skipDelimiter(':');
        int minutes = readNumber(2);
        if (!delimiter && tokenLength == 1) {
            // three digits in hmm format (the second digit of hours belongs to minutes)
            minutes += 10 * (hours % 10);
            hours /= 10;
        }
        return sign * (60 * hours + minutes);
    }

    private boolean skipParentheses() {
        int parenCount = 1;
        while (pos < length && parenCount != 0) {
            final char c = string.charAt(pos++);
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
            }
        }
        return true;
    }

    private static int getDefaultValue(final int field) {
        switch (field) {
            case MONTH:
            case DAY:
                return 1;
            default:
                return 0;
        }
    }

    private static boolean isDay(final int n) {
        return 1 <= n && n <= 31;
    }

    private static boolean isMonth(final int n) {
        return 1 <= n && n <= 12;
    }

    private static boolean isHour(final int n) {
        return 0 <= n && n <= 24;
    }

    private static boolean isMinuteOrSecond(final int n) {
        return 0 <= n && n < 60;
    }

    private boolean setMonth(final int m) {
        if (!isSet(MONTH)) {
            namedMonth = true;
            set(MONTH, m);
            return true;
        }
        return false;
    }

    private boolean setDateField(final int n) {
        for (int field = YEAR; field != HOUR; field++) {
            if (!isSet(field)) {
                // no validation on legacy date fields
                set(field, n);
                return true;
            }
        }
        return false;
    }

    private boolean setTimeField(final int n) {
        for (int field = HOUR; field != TIMEZONE; field++) {
            if (!isSet(field)) {
                if (checkLegacyField(field, n)) {
                    set(field, n);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private boolean setTimezone(final int offset, final boolean asNumericOffset) {
        if (!isSet(TIMEZONE) || (asNumericOffset && get(TIMEZONE) == 0)) {
            set(TIMEZONE, offset);
            return true;
        }
        return false;
    }

    private boolean setAmPm(final int offset) {
        if (!isSet(HOUR)) {
            return false;
        }
        int hour = get(HOUR);
        if (hour >= 0 && hour <= 12) {
            if (hour == 12) {
                // 12:30 am == 00:30; 12:30 pm == 12:30 (24h)
                hour = 0;
            }
            set(HOUR, hour + offset);
        }
        return true;
    }

    private boolean patchResult(final boolean strict) {
        // sanity checks - make sure we have something
        if (!isSet(YEAR) && !isSet(HOUR)) {
            return false;
        }
        if (isSet(HOUR) && !isSet(MINUTE)) {
            return false;
        }
        JSContext context = realm.getContext();
        if (context.isOptionV8CompatibilityMode() && !extraLenient) {
            if (!isSet(YEAR) && !isSet(DAY) && !isSet(MONTH)) {
                return false;
            }
        }
        boolean dateOnly = !isSet(HOUR);
        // fill in default values for unset fields except timezone
        for (int field = YEAR; field <= TIMEZONE; field++) {
            if (get(field) == null) {
                if (field == TIMEZONE && !isUTCDefaultTimezone(dateOnly, strict)) {
                    // When the UTC offset representation is absent,
                    // date-only forms are interpreted as a UTC time and
                    // date-time forms are interpreted as a local time (= empty TIMEZONE).
                    continue;
                }
                final int value = getDefaultValue(field);
                set(field, value);
            }
        }

        if (!strict) {
            // swap year, month, and day if it looks like the right thing to do
            if (isDay(get(YEAR))) {
                final int d = get(YEAR);
                set(YEAR, get(DAY));
                if (namedMonth) {
                    // d-m-y
                    set(DAY, d);
                } else {
                    // m-d-y
                    final int d2 = get(MONTH);
                    set(MONTH, d);
                    set(DAY, d2);
                }
            }
            // sanity checks now that we know what's what
            if (!isMonth(get(MONTH)) || !isDay(get(DAY))) {
                return false;
            }

            // add 1900 or 2000 to year if it's between 0 and 100
            final int year = get(YEAR);
            if (year >= 0 && year < 100) {
                set(YEAR, year >= 50 ? 1900 + year : 2000 + year);
            }
        } else {
            // 24 hour value is only allowed if all other time values are zero
            if (get(HOUR) == 24 &&
                    (get(MINUTE) != 0 || get(SECOND) != 0 || get(MILLISECOND) != 0)) {
                return false;
            }
        }

        // set month to 0-based
        set(MONTH, get(MONTH) - 1);
        return true;
    }

    private boolean isUTCDefaultTimezone(boolean dateOnly, boolean strict) {
        return realm.getContext().isOptionNashornCompatibilityMode() ? strict :
                (dateOnly && (strict || realm.getContext().getLanguageOptions().useUTCForLegacyDates()));
    }

    private static void addName(final String str, final int type, final int value) {
        final Name name = new Name(str, type, value);
        names.put(name.key, name);
    }

    private static class Name {
        final String name;
        final String key;
        final int value;
        final int type;

        static final int DAY_OF_WEEK    = -1;
        static final int MONTH_NAME     = 0;
        static final int AM_PM          = 1;
        static final int TIMEZONE_ID    = 2;
        static final int TIME_SEPARATOR = 3;

        Name(final String name, final int type, final int value) {
            assert name != null;
            assert name.equals(name.toLowerCase(Locale.ENGLISH));

            this.name = name;
            // use first three characters as lookup key
            this.key = name.substring(0, Math.min(3, name.length()));
            this.type = type;
            this.value = value;
        }

        public boolean matches(final String str, final int offset, final int len) {
            return name.regionMatches(true, 0, str, offset, len);
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
