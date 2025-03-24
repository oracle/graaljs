/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.intl.JSDurationFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSDurationFormatObject;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.Pair;

import static com.oracle.truffle.js.nodes.intl.GetDurationUnitOptionsNode.Unit.DAYS;
import static com.oracle.truffle.js.nodes.intl.GetDurationUnitOptionsNode.Unit.HOURS;
import static com.oracle.truffle.js.nodes.intl.GetDurationUnitOptionsNode.Unit.MICROSECONDS;
import static com.oracle.truffle.js.nodes.intl.GetDurationUnitOptionsNode.Unit.MILLISECONDS;
import static com.oracle.truffle.js.nodes.intl.GetDurationUnitOptionsNode.Unit.MINUTES;
import static com.oracle.truffle.js.nodes.intl.GetDurationUnitOptionsNode.Unit.MONTHS;
import static com.oracle.truffle.js.nodes.intl.GetDurationUnitOptionsNode.Unit.NANOSECONDS;
import static com.oracle.truffle.js.nodes.intl.GetDurationUnitOptionsNode.Unit.SECONDS;
import static com.oracle.truffle.js.nodes.intl.GetDurationUnitOptionsNode.Unit.WEEKS;
import static com.oracle.truffle.js.nodes.intl.GetDurationUnitOptionsNode.Unit.YEARS;

public abstract class InitializeDurationFormatNode extends JavaScriptBaseNode {

    @Child JSToCanonicalizedLocaleListNode canonicalizedLocaleList;
    @Child GetOptionsObjectNode getOptionsObject;
    @Child GetStringOptionNode getLocaleMatcherOption;
    @Child GetStringOptionNode getNumberingSystemOption;
    @Child GetStringOptionNode getStyleOption;

    @Child GetDurationUnitOptionsNode getYearsOptions;
    @Child GetDurationUnitOptionsNode getMonthsOptions;
    @Child GetDurationUnitOptionsNode getWeeksOptions;
    @Child GetDurationUnitOptionsNode getDaysOptions;
    @Child GetDurationUnitOptionsNode getHoursOptions;
    @Child GetDurationUnitOptionsNode getMinutesOptions;
    @Child GetDurationUnitOptionsNode getSecondsOptions;
    @Child GetDurationUnitOptionsNode getMillisecondsOptions;
    @Child GetDurationUnitOptionsNode getMicrosecondsOptions;
    @Child GetDurationUnitOptionsNode getNanosecondsOptions;

    @Child GetNumberOptionNode getFractionalDigitsOption;

    private final BranchProfile errorBranch = BranchProfile.create();

    protected InitializeDurationFormatNode(JSContext context) {
        this.canonicalizedLocaleList = JSToCanonicalizedLocaleListNode.create(context);
        this.getOptionsObject = GetOptionsObjectNodeGen.create(context);
        this.getLocaleMatcherOption = GetStringOptionNode.create(context, IntlUtil.KEY_LOCALE_MATCHER, GetStringOptionNode.LOCALE_MATCHER_OPTION_VALUES, IntlUtil.BEST_FIT);
        this.getNumberingSystemOption = GetStringOptionNode.create(context, IntlUtil.KEY_NUMBERING_SYSTEM, null, null);
        this.getStyleOption = GetStringOptionNode.create(context, IntlUtil.KEY_STYLE, GetStringOptionNode.LONG_SHORT_NARROW_DIGITAL_OPTION_VALUES, IntlUtil.SHORT);

        this.getYearsOptions = GetDurationUnitOptionsNode.create(context, YEARS, GetDurationUnitOptionsNode.LONG_SHORT_NARROW_STYLES, IntlUtil.SHORT);
        this.getMonthsOptions = GetDurationUnitOptionsNode.create(context, MONTHS, GetDurationUnitOptionsNode.LONG_SHORT_NARROW_STYLES, IntlUtil.SHORT);
        this.getWeeksOptions = GetDurationUnitOptionsNode.create(context, WEEKS, GetDurationUnitOptionsNode.LONG_SHORT_NARROW_STYLES, IntlUtil.SHORT);
        this.getDaysOptions = GetDurationUnitOptionsNode.create(context, DAYS, GetDurationUnitOptionsNode.LONG_SHORT_NARROW_STYLES, IntlUtil.SHORT);
        this.getHoursOptions = GetDurationUnitOptionsNode.create(context, HOURS, GetDurationUnitOptionsNode.LONG_SHORT_NARROW_NUMERIC_2DIGIT_STYLES, IntlUtil.NUMERIC);
        this.getMinutesOptions = GetDurationUnitOptionsNode.create(context, MINUTES, GetDurationUnitOptionsNode.LONG_SHORT_NARROW_NUMERIC_2DIGIT_STYLES, IntlUtil.NUMERIC);
        this.getSecondsOptions = GetDurationUnitOptionsNode.create(context, SECONDS, GetDurationUnitOptionsNode.LONG_SHORT_NARROW_NUMERIC_2DIGIT_STYLES, IntlUtil.NUMERIC);
        this.getMillisecondsOptions = GetDurationUnitOptionsNode.create(context, MILLISECONDS, GetDurationUnitOptionsNode.LONG_SHORT_NARROW_NUMERIC_STYLES, IntlUtil.NUMERIC);
        this.getMicrosecondsOptions = GetDurationUnitOptionsNode.create(context, MICROSECONDS, GetDurationUnitOptionsNode.LONG_SHORT_NARROW_NUMERIC_STYLES, IntlUtil.NUMERIC);
        this.getNanosecondsOptions = GetDurationUnitOptionsNode.create(context, NANOSECONDS, GetDurationUnitOptionsNode.LONG_SHORT_NARROW_NUMERIC_STYLES, IntlUtil.NUMERIC);

        this.getFractionalDigitsOption = GetNumberOptionNode.create(context, IntlUtil.KEY_FRACTIONAL_DIGITS);
    }

    public abstract JSDurationFormatObject executeInit(JSDurationFormatObject durationFormat, Object locales, Object options);

    @NeverDefault
    public static InitializeDurationFormatNode create(JSContext context) {
        return InitializeDurationFormatNodeGen.create(context);
    }

    @Specialization
    public JSDurationFormatObject initialize(JSDurationFormatObject durationFormat, Object locales, Object optionsArg) {
        JSDurationFormat.InternalState state = durationFormat.getInternalState();

        String[] requestedLocales = canonicalizedLocaleList.executeLanguageTags(locales);
        Object options = getOptionsObject.execute(optionsArg);

        getLocaleMatcherOption.executeValue(options);

        String numberingSystem = getNumberingSystemOption.executeValue(options);
        if (numberingSystem != null) {
            IntlUtil.validateUnicodeLocaleIdentifierType(numberingSystem, errorBranch);
        }

        state.resolveLocaleAndNumberingSystem(getJSContext(), requestedLocales, numberingSystem);

        boolean twoDigitHours = false;

        String style = getStyleOption.executeValue(options);
        state.setStyle(style);

        Pair<String, String> unitOptions;
        unitOptions = getYearsOptions.executeOptions(options, style, "", twoDigitHours);
        state.setYearsOptions(unitOptions);
        unitOptions = getMonthsOptions.executeOptions(options, style, unitOptions.getFirst(), twoDigitHours);
        state.setMonthsOptions(unitOptions);
        unitOptions = getWeeksOptions.executeOptions(options, style, unitOptions.getFirst(), twoDigitHours);
        state.setWeeksOptions(unitOptions);
        unitOptions = getDaysOptions.executeOptions(options, style, unitOptions.getFirst(), twoDigitHours);
        state.setDaysOptions(unitOptions);
        unitOptions = getHoursOptions.executeOptions(options, style, unitOptions.getFirst(), twoDigitHours);
        state.setHoursOptions(unitOptions);
        unitOptions = getMinutesOptions.executeOptions(options, style, unitOptions.getFirst(), twoDigitHours);
        state.setMinutesOptions(unitOptions);
        unitOptions = getSecondsOptions.executeOptions(options, style, unitOptions.getFirst(), twoDigitHours);
        state.setSecondsOptions(unitOptions);
        unitOptions = getMillisecondsOptions.executeOptions(options, style, unitOptions.getFirst(), twoDigitHours);
        state.setMillisecondsOptions(unitOptions);
        unitOptions = getMicrosecondsOptions.executeOptions(options, style, unitOptions.getFirst(), twoDigitHours);
        state.setMicrosecondsOptions(unitOptions);
        unitOptions = getNanosecondsOptions.executeOptions(options, style, unitOptions.getFirst(), twoDigitHours);
        state.setNanosecondsOptions(unitOptions);

        int fractionalDigits = getFractionalDigitsOption.executeInt(options, 0, 9, -1);
        state.setFractionalDigits(fractionalDigits);

        return durationFormat;
    }

}
