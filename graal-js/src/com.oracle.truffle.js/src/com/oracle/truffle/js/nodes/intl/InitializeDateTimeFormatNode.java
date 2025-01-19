/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.intl;

import java.util.List;
import java.util.MissingResourceException;

import org.graalvm.shadowed.com.ibm.icu.util.TimeZone;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormatObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.Pair;

/*
 * https://tc39.github.io/ecma402/#sec-initializedatetimeformat
 */
public abstract class InitializeDateTimeFormatNode extends JavaScriptBaseNode {

    public static final List<String> FORMAT_MATCHER_OPTION_VALUES = List.of(IntlUtil.BASIC, IntlUtil.BEST_FIT);
    /** Valid option values for year, day, hour, minute, second. */
    public static final List<String> TWO_DIGIT_NUMERIC_OPTION_VALUES = List.of(IntlUtil._2_DIGIT, IntlUtil.NUMERIC);
    public static final List<String> DAY_OPTION_VALUES = List.of(IntlUtil._2_DIGIT, IntlUtil.NUMERIC, IntlUtil.NARROW, IntlUtil.SHORT, IntlUtil.LONG);
    /** Valid option values for dateStyle and timeStyle. */
    public static final List<String> DATE_TIME_STYLE_OPTION_VALUES = List.of(IntlUtil.FULL, IntlUtil.LONG, IntlUtil.MEDIUM, IntlUtil.SHORT);

    public static final List<String> TIME_ZONE_NAME_OPTION_VALUES = List.of(IntlUtil.SHORT, IntlUtil.LONG);
    public static final List<String> TIME_ZONE_NAME_OPTION_VALUES_ES2022 = List.of(IntlUtil.SHORT, IntlUtil.LONG,
                    IntlUtil.SHORT_OFFSET, IntlUtil.LONG_OFFSET, IntlUtil.SHORT_GENERIC, IntlUtil.LONG_GENERIC);

    public enum Required {
        DATE,
        TIME,
        ANY
    }

    public enum Defaults {
        DATE,
        TIME,
        ALL
    }

    private final Required required;
    private final Defaults defaults;

    @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;
    @Child CoerceOptionsToObjectNode coerceOptionsToObjectNode;
    @Child PropertyGetNode getTimeZoneNode;

    @Child GetStringOptionNode getLocaleMatcherOption;
    @Child GetStringOptionNode getFormatMatcherOption;
    @Child GetStringOptionNode getHourCycleOption;
    @Child GetStringOptionNode getCalendarOption;
    @Child GetStringOptionNode getNumberingSystemOption;
    @Child GetBooleanOptionNode getHour12Option;

    // https://tc39.github.io/ecma402/#table-datetimeformat-components
    @Child GetStringOptionNode getWeekdayOption;
    @Child GetStringOptionNode getEraOption;
    @Child GetStringOptionNode getYearOption;
    @Child GetStringOptionNode getMonthOption;
    @Child GetStringOptionNode getDayOption;
    @Child GetStringOptionNode getDayPeriodOption;
    @Child GetStringOptionNode getHourOption;
    @Child GetStringOptionNode getMinuteOption;
    @Child GetStringOptionNode getSecondOption;
    @Child GetNumberOptionNode getFractionalSecondDigitsOption;
    @Child GetStringOptionNode getTimeZoneNameOption;
    @Child GetStringOptionNode getDateStyleOption;
    @Child GetStringOptionNode getTimeStyleOption;

    @Child JSToStringNode toStringNode;
    @Child TruffleString.ToJavaStringNode toJavaStringNode;

    private final BranchProfile errorBranch = BranchProfile.create();

    private final JSContext context;

    protected InitializeDateTimeFormatNode(JSContext context, Required required, Defaults defaults) {

        this.context = context;

        this.required = required;
        this.defaults = defaults;

        this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        this.coerceOptionsToObjectNode = CoerceOptionsToObjectNodeGen.create(context);
        this.getTimeZoneNode = PropertyGetNode.create(IntlUtil.KEY_TIME_ZONE, context);

        this.getLocaleMatcherOption = GetStringOptionNode.create(context, IntlUtil.KEY_LOCALE_MATCHER, GetStringOptionNode.LOCALE_MATCHER_OPTION_VALUES, IntlUtil.BEST_FIT);
        this.getFormatMatcherOption = GetStringOptionNode.create(context, IntlUtil.KEY_FORMAT_MATCHER, FORMAT_MATCHER_OPTION_VALUES, IntlUtil.BEST_FIT);
        this.getHourCycleOption = GetStringOptionNode.create(context, IntlUtil.KEY_HOUR_CYCLE, GetStringOptionNode.HOUR_CYCLE_OPTION_VALUES, null);
        this.getCalendarOption = GetStringOptionNode.create(context, IntlUtil.KEY_CALENDAR, null, null);
        this.getNumberingSystemOption = GetStringOptionNode.create(context, IntlUtil.KEY_NUMBERING_SYSTEM, null, null);
        this.getHour12Option = GetBooleanOptionNode.create(context, IntlUtil.KEY_HOUR12, null);

        this.getWeekdayOption = GetStringOptionNode.create(context, IntlUtil.KEY_WEEKDAY, GetStringOptionNode.NARROW_SHORT_LONG_OPTION_VALUES, null);
        this.getEraOption = GetStringOptionNode.create(context, IntlUtil.KEY_ERA, GetStringOptionNode.NARROW_SHORT_LONG_OPTION_VALUES, null);
        this.getYearOption = GetStringOptionNode.create(context, IntlUtil.KEY_YEAR, TWO_DIGIT_NUMERIC_OPTION_VALUES, null);
        this.getMonthOption = GetStringOptionNode.create(context, IntlUtil.KEY_MONTH, DAY_OPTION_VALUES, null);
        this.getDayOption = GetStringOptionNode.create(context, IntlUtil.KEY_DAY, TWO_DIGIT_NUMERIC_OPTION_VALUES, null);
        this.getDayPeriodOption = GetStringOptionNode.create(context, IntlUtil.KEY_DAY_PERIOD, GetStringOptionNode.NARROW_SHORT_LONG_OPTION_VALUES, null);
        this.getHourOption = GetStringOptionNode.create(context, IntlUtil.KEY_HOUR, TWO_DIGIT_NUMERIC_OPTION_VALUES, null);
        this.getMinuteOption = GetStringOptionNode.create(context, IntlUtil.KEY_MINUTE, TWO_DIGIT_NUMERIC_OPTION_VALUES, null);
        this.getSecondOption = GetStringOptionNode.create(context, IntlUtil.KEY_SECOND, TWO_DIGIT_NUMERIC_OPTION_VALUES, null);
        this.getFractionalSecondDigitsOption = GetNumberOptionNode.create(context, IntlUtil.KEY_FRACTIONAL_SECOND_DIGITS);
        this.getTimeZoneNameOption = GetStringOptionNode.create(context, IntlUtil.KEY_TIME_ZONE_NAME, timeZoneNameOptions(context), null);
        this.getDateStyleOption = GetStringOptionNode.create(context, IntlUtil.KEY_DATE_STYLE, DATE_TIME_STYLE_OPTION_VALUES, null);
        this.getTimeStyleOption = GetStringOptionNode.create(context, IntlUtil.KEY_TIME_STYLE, DATE_TIME_STYLE_OPTION_VALUES, null);

        this.toStringNode = JSToStringNode.create();
        this.toJavaStringNode = TruffleString.ToJavaStringNode.create();
    }

    public abstract JSDateTimeFormatObject executeInit(JSDateTimeFormatObject dateTimeFormatObj, Object locales, Object options);

    public static InitializeDateTimeFormatNode createInitalizeDateTimeFormatNode(JSContext context, Required required, Defaults defaults) {
        return InitializeDateTimeFormatNodeGen.create(context, required, defaults);
    }

    @Specialization
    public JSDateTimeFormatObject initializeDateTimeFormat(JSDateTimeFormatObject dateTimeFormatObj, Object localesArg, Object optionsArg) {

        // must be invoked before any code that tries to access ICU library data
        try {
            JSDateTimeFormat.InternalState state = dateTimeFormatObj.getInternalState();

            String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(localesArg);
            Object options = coerceOptionsToObjectNode.execute(optionsArg);

            // enforce validity check
            getLocaleMatcherOption.executeValue(options);

            String calendarOpt = getCalendarOption.executeValue(options);
            if (calendarOpt != null) {
                IntlUtil.validateUnicodeLocaleIdentifierType(calendarOpt, errorBranch);
                calendarOpt = IntlUtil.normalizeUnicodeLocaleIdentifierType(calendarOpt);
            }
            String numberingSystemOpt = getNumberingSystemOption.executeValue(options);
            if (numberingSystemOpt != null) {
                IntlUtil.validateUnicodeLocaleIdentifierType(numberingSystemOpt, errorBranch);
                numberingSystemOpt = IntlUtil.normalizeUnicodeLocaleIdentifierType(numberingSystemOpt);
            }

            Boolean hour12Opt = getHour12Option.executeValue(options);
            String hcOpt = getHourCycleOption.executeValue(options);

            Object timeZoneValue = getTimeZoneNode.getValue(options);
            Pair<TimeZone, String> timeZone = toTimeZone(timeZoneValue);

            String weekdayOpt = getWeekdayOption.executeValue(options);
            String eraOpt = getEraOption.executeValue(options);
            String yearOpt = getYearOption.executeValue(options);
            String monthOpt = getMonthOption.executeValue(options);
            String dayOpt = getDayOption.executeValue(options);
            String dayPeriodOpt = getDayPeriodOption.executeValue(options);
            String hourOpt = getHourOption.executeValue(options);
            String minuteOpt = getMinuteOption.executeValue(options);
            String secondOpt = getSecondOption.executeValue(options);
            int fractionalSecondDigitsOpt = getFractionalSecondDigitsOption.executeInt(options, 1, 3, 0);
            String tzNameOpt = getTimeZoneNameOption.executeValue(options);

            boolean hasExplicitFormatComponents = (weekdayOpt != null || eraOpt != null || yearOpt != null || monthOpt != null || dayOpt != null || dayPeriodOpt != null ||
                            hourOpt != null || minuteOpt != null || secondOpt != null || fractionalSecondDigitsOpt != 0 || tzNameOpt != null);

            getFormatMatcherOption.executeValue(options);

            String dateStyleOpt = getDateStyleOption.executeValue(options);
            String timeStyleOpt = getTimeStyleOption.executeValue(options);

            if ((dateStyleOpt != null || timeStyleOpt != null)) {
                if (hasExplicitFormatComponents || (required == Required.DATE && timeStyleOpt != null) || (required == Required.TIME && dateStyleOpt != null)) {
                    errorBranch.enter();
                    throw Errors.createTypeError("dateStyle and timeStyle options cannot be mixed with other date/time options");
                }
            } else {
                boolean needDefaults = true;
                if (required == Required.DATE || required == Required.ANY) {
                    if (weekdayOpt != null || yearOpt != null || monthOpt != null || dayOpt != null) {
                        needDefaults = false;
                    }
                }
                if (required == Required.TIME || required == Required.ANY) {
                    if (dayPeriodOpt != null || hourOpt != null || minuteOpt != null || secondOpt != null || fractionalSecondDigitsOpt != 0) {
                        needDefaults = false;
                    }
                }
                if (needDefaults && (defaults == Defaults.DATE || defaults == Defaults.ALL)) {
                    yearOpt = IntlUtil.NUMERIC;
                    monthOpt = IntlUtil.NUMERIC;
                    dayOpt = IntlUtil.NUMERIC;
                }
                if (needDefaults && (defaults == Defaults.TIME || defaults == Defaults.ALL)) {
                    hourOpt = IntlUtil.NUMERIC;
                    minuteOpt = IntlUtil.NUMERIC;
                    secondOpt = IntlUtil.NUMERIC;
                }
            }

            JSDateTimeFormat.setupInternalDateTimeFormat(context, state, locales, weekdayOpt, eraOpt, yearOpt, monthOpt, dayOpt, dayPeriodOpt, hourOpt, hcOpt, hour12Opt, minuteOpt, secondOpt,
                            fractionalSecondDigitsOpt, tzNameOpt, timeZone.getFirst(), timeZone.getSecond(), calendarOpt, numberingSystemOpt, dateStyleOpt, timeStyleOpt);

        } catch (MissingResourceException e) {
            errorBranch.enter();
            throw Errors.createICU4JDataError(e);
        }

        return dateTimeFormatObj;
    }

    private Pair<TimeZone, String> toTimeZone(Object timeZoneValue) {
        TimeZone timeZone;
        String tzId;
        if (timeZoneValue != Undefined.instance) {
            TruffleString nameTS = toStringNode.executeString(timeZoneValue);
            String name = toJavaStringNode.execute(nameTS);
            return parseAndGetICUTimeZone(name);
        } else {
            timeZone = getRealm().getLocalTimeZone();
            tzId = timeZone.getID();
            return new Pair<>(timeZone, tzId);
        }
    }

    @TruffleBoundary
    private Pair<TimeZone, String> parseAndGetICUTimeZone(String name) {
        TimeZone timeZone;
        String tzId;
        tzId = maybeParseAndFormatTimeZoneOffset(name);
        if (tzId == null) {
            tzId = JSDateTimeFormat.canonicalizeTimeZoneName(name);
            if (tzId == null) {
                throw Errors.createRangeErrorInvalidTimeZone(name);
            }
            timeZone = IntlUtil.getICUTimeZone(tzId, context);
        } else {
            timeZone = IntlUtil.getICUTimeZoneForOffset(tzId);
        }
        return new Pair<>(timeZone, tzId);
    }

    private static String maybeParseAndFormatTimeZoneOffset(String name) {
        // TemporalSign Hour TimeSeparator MinuteSecond
        boolean reformatNeeded = false;

        // TemporalSign
        int length = name.length();
        if (length < 3) {
            return null;
        }
        char sign = name.charAt(0);
        if (sign != '+' && sign != '-') {
            return null;
        }

        // Hour
        int hourTens = name.charAt(1) - '0';
        int hourOnes = name.charAt(2) - '0';
        int hours = 10 * hourTens + hourOnes;
        if (hourOnes < 0 || 9 < hourOnes || hours < 0 || 23 < hours) {
            return null;
        }

        int minutes;
        if (length > 3) {
            // TimeSeparator
            int pos = 3;
            if (name.charAt(pos) == ':') {
                pos++;
            } else {
                reformatNeeded = true;
            }
            if (length != pos + 2) {
                return null;
            }

            // MinuteSecond
            int minuteTens = name.charAt(pos) - '0';
            int minuteOnes = name.charAt(pos + 1) - '0';
            minutes = 10 * minuteTens + minuteOnes;
            if (minuteOnes < 0 || 9 < minuteOnes || minutes < 0 || 59 < minutes) {
                return null;
            }
        } else {
            reformatNeeded = true;
            minutes = 0;
        }

        if (sign == '-' && hours == 0 && minutes == 0) {
            reformatNeeded = true;
            sign = '+';
        }

        if (reformatNeeded) {
            return formatOffsetTimeZoneIdentifier(sign, hours, minutes);
        } else {
            return name;
        }
    }

    @TruffleBoundary
    private static String formatOffsetTimeZoneIdentifier(char sign, int hours, int minutes) {
        return sign + formatUsing2Digits(hours) + ':' + formatUsing2Digits(minutes);
    }

    @TruffleBoundary
    private static String formatUsing2Digits(int value) {
        String valueStr = Integer.toString(value);
        return (valueStr.length() == 1) ? ("0" + valueStr) : valueStr;
    }

    private static List<String> timeZoneNameOptions(JSContext context) {
        if (context.getEcmaScriptVersion() >= JSConfig.ECMAScript2022) {
            return TIME_ZONE_NAME_OPTION_VALUES_ES2022;
        } else {
            return TIME_ZONE_NAME_OPTION_VALUES;
        }
    }

}
