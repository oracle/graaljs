/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.MissingResourceException;

import com.ibm.icu.util.TimeZone;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;

/*
 * https://tc39.github.io/ecma402/#sec-initializedatetimeformat
 */
public abstract class InitializeDateTimeFormatNode extends JavaScriptBaseNode {

    String required;
    String defaults;

    @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;
    @Child ToDateTimeOptionsNode createOptionsNode;
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

    @Child private JSToStringNode toStringNode;
    private final BranchProfile errorBranch = BranchProfile.create();

    private final JSContext context;

    protected InitializeDateTimeFormatNode(JSContext context, String required, String defaults) {

        this.context = context;

        this.required = required;
        this.defaults = defaults;

        this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        this.createOptionsNode = ToDateTimeOptionsNodeGen.create(context);
        this.getTimeZoneNode = PropertyGetNode.create(IntlUtil.TIME_ZONE, context);

        this.getLocaleMatcherOption = GetStringOptionNode.create(context, IntlUtil.LOCALE_MATCHER, new String[]{IntlUtil.LOOKUP, IntlUtil.BEST_FIT}, IntlUtil.BEST_FIT);
        this.getFormatMatcherOption = GetStringOptionNode.create(context, IntlUtil.FORMAT_MATCHER, new String[]{IntlUtil.BASIC, IntlUtil.BEST_FIT}, IntlUtil.BEST_FIT);
        this.getHourCycleOption = GetStringOptionNode.create(context, IntlUtil.HOUR_CYCLE, new String[]{IntlUtil.H11, IntlUtil.H12, IntlUtil.H23, IntlUtil.H24}, null);
        this.getCalendarOption = GetStringOptionNode.create(context, IntlUtil.CALENDAR, null, null);
        this.getNumberingSystemOption = GetStringOptionNode.create(context, IntlUtil.NUMBERING_SYSTEM, null, null);
        this.getHour12Option = GetBooleanOptionNode.create(context, IntlUtil.HOUR12, null);

        this.getWeekdayOption = GetStringOptionNode.create(context, IntlUtil.WEEKDAY, new String[]{IntlUtil.NARROW, IntlUtil.SHORT, IntlUtil.LONG}, null);
        this.getEraOption = GetStringOptionNode.create(context, IntlUtil.ERA, new String[]{IntlUtil.NARROW, IntlUtil.SHORT, IntlUtil.LONG}, null);
        this.getYearOption = GetStringOptionNode.create(context, IntlUtil.YEAR, new String[]{IntlUtil._2_DIGIT, IntlUtil.NUMERIC}, null);
        this.getMonthOption = GetStringOptionNode.create(context, IntlUtil.MONTH, new String[]{IntlUtil._2_DIGIT, IntlUtil.NUMERIC, IntlUtil.NARROW, IntlUtil.SHORT, IntlUtil.LONG}, null);
        this.getDayOption = GetStringOptionNode.create(context, IntlUtil.DAY, new String[]{IntlUtil._2_DIGIT, IntlUtil.NUMERIC}, null);
        this.getDayPeriodOption = GetStringOptionNode.create(context, IntlUtil.DAY_PERIOD, new String[]{IntlUtil.NARROW, IntlUtil.SHORT, IntlUtil.LONG}, null);
        this.getHourOption = GetStringOptionNode.create(context, IntlUtil.HOUR, new String[]{IntlUtil._2_DIGIT, IntlUtil.NUMERIC}, null);
        this.getMinuteOption = GetStringOptionNode.create(context, IntlUtil.MINUTE, new String[]{IntlUtil._2_DIGIT, IntlUtil.NUMERIC}, null);
        this.getSecondOption = GetStringOptionNode.create(context, IntlUtil.SECOND, new String[]{IntlUtil._2_DIGIT, IntlUtil.NUMERIC}, null);
        this.getFractionalSecondDigitsOption = GetNumberOptionNode.create(context, IntlUtil.FRACTIONAL_SECOND_DIGITS);
        this.getTimeZoneNameOption = GetStringOptionNode.create(context, IntlUtil.TIME_ZONE_NAME, new String[]{IntlUtil.SHORT, IntlUtil.LONG}, null);
        this.getDateStyleOption = GetStringOptionNode.create(context, IntlUtil.DATE_STYLE, new String[]{IntlUtil.FULL, IntlUtil.LONG, IntlUtil.MEDIUM, IntlUtil.SHORT}, null);
        this.getTimeStyleOption = GetStringOptionNode.create(context, IntlUtil.TIME_STYLE, new String[]{IntlUtil.FULL, IntlUtil.LONG, IntlUtil.MEDIUM, IntlUtil.SHORT}, null);

        this.toStringNode = JSToStringNode.create();
    }

    public abstract DynamicObject executeInit(DynamicObject collator, Object locales, Object options);

    public static InitializeDateTimeFormatNode createInitalizeDateTimeFormatNode(JSContext context, String required, String defaults) {
        return InitializeDateTimeFormatNodeGen.create(context, required, defaults);
    }

    @Specialization
    public DynamicObject initializeDateTimeFormat(DynamicObject dateTimeFormatObj, Object localesArg, Object optionsArg) {

        // must be invoked before any code that tries to access ICU library data
        try {
            JSDateTimeFormat.InternalState state = JSDateTimeFormat.getInternalState(dateTimeFormatObj);

            String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(localesArg);
            DynamicObject options = createOptionsNode.execute(optionsArg, required, defaults);

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
            TimeZone timeZone = toTimeZone(timeZoneValue);

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

            getFormatMatcherOption.executeValue(options);

            String dateStyleOpt = getDateStyleOption.executeValue(options);
            String timeStyleOpt = getTimeStyleOption.executeValue(options);

            if ((dateStyleOpt != null || timeStyleOpt != null) && (weekdayOpt != null || eraOpt != null || yearOpt != null || monthOpt != null || dayOpt != null || dayPeriodOpt != null ||
                            hourOpt != null || minuteOpt != null || secondOpt != null || fractionalSecondDigitsOpt != 0 || tzNameOpt != null)) {
                errorBranch.enter();
                throw Errors.createTypeError("dateStyle and timeStyle options cannot be mixed with other date/time options");
            }

            JSDateTimeFormat.setupInternalDateTimeFormat(context, state, locales, weekdayOpt, eraOpt, yearOpt, monthOpt, dayOpt, dayPeriodOpt, hourOpt, hcOpt, hour12Opt, minuteOpt, secondOpt,
                            fractionalSecondDigitsOpt, tzNameOpt, timeZone, calendarOpt, numberingSystemOpt, dateStyleOpt, timeStyleOpt);

        } catch (MissingResourceException e) {
            errorBranch.enter();
            throw Errors.createICU4JDataError(e);
        }

        return dateTimeFormatObj;
    }

    private TimeZone toTimeZone(Object timeZoneValue) {
        String tzId;
        if (timeZoneValue != Undefined.instance) {
            String name = toStringNode.executeString(timeZoneValue);
            tzId = JSDateTimeFormat.canonicalizeTimeZoneName(name);
            if (tzId == null) {
                errorBranch.enter();
                throw Errors.createRangeErrorInvalidTimeZone(name);
            }
            return IntlUtil.getICUTimeZone(tzId);
        } else {
            return getRealm().getLocalTimeZone();
        }
    }

}
