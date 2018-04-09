/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.intl.InitializeDateTimeFormatNodeGen.ToDateTimeOptionsNodeGen;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

/*
 * https://tc39.github.io/ecma402/#sec-initializedatetimeformat
 */
public abstract class InitializeDateTimeFormatNode extends JavaScriptBaseNode {

    String required;
    String defaults;

    @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;
    @Child ToDateTimeOptionsNode createOptionsNode;

    @Child GetStringOptionNode getLocaleMatcherOption;
    @Child GetStringOptionNode getFormatMatcherOption;
    @Child GetStringOptionNode getHourCycleOption;
    @Child GetBooleanOptionNode getHour12Option;

    // https://tc39.github.io/ecma402/#table-datetimeformat-components
    @Child GetStringOptionNode getWeekdayOption;
    @Child GetStringOptionNode getEraOption;
    @Child GetStringOptionNode getYearOption;
    @Child GetStringOptionNode getMonthOption;
    @Child GetStringOptionNode getDayOption;
    @Child GetStringOptionNode getHourOption;
    @Child GetStringOptionNode getMinuteOption;
    @Child GetStringOptionNode getSecondOption;
    @Child GetStringOptionNode getTimeZoneNameOption;

    protected InitializeDateTimeFormatNode(JSContext context, String required, String defaults) {

        this.required = required;
        this.defaults = defaults;

        this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        this.createOptionsNode = ToDateTimeOptionsNodeGen.create(context);

        this.getLocaleMatcherOption = GetStringOptionNode.create(context, "localeMatcher", new String[]{"lookup", "best fit"}, "best fit");
        this.getFormatMatcherOption = GetStringOptionNode.create(context, "formatMatcher", new String[]{"basic", "best fit"}, "best fit");
        this.getHourCycleOption = GetStringOptionNode.create(context, "hourCycle", new String[]{"h11", "h12", "h23", "h24"}, null);
        this.getHour12Option = GetBooleanOptionNode.create(context, "hour12", null);

        this.getWeekdayOption = GetStringOptionNode.create(context, "weekday", new String[]{"narrow", "short", "long"}, null);
        this.getEraOption = GetStringOptionNode.create(context, "era", new String[]{"narrow", "short", "long"}, null);
        this.getYearOption = GetStringOptionNode.create(context, "year", new String[]{"2-digit", "numeric"}, null);
        this.getMonthOption = GetStringOptionNode.create(context, "month", new String[]{"2-digit", "numeric", "narrow", "short", "long"}, null);
        this.getDayOption = GetStringOptionNode.create(context, "day", new String[]{"2-digit", "numeric"}, null);
        this.getHourOption = GetStringOptionNode.create(context, "hour", new String[]{"2-digit", "numeric"}, null);
        this.getMinuteOption = GetStringOptionNode.create(context, "minute", new String[]{"2-digit", "numeric"}, null);
        this.getSecondOption = GetStringOptionNode.create(context, "second", new String[]{"2-digit", "numeric"}, null);
        this.getTimeZoneNameOption = GetStringOptionNode.create(context, "timeZoneName", new String[]{"short", "long"}, null);
    }

    public abstract DynamicObject executeInit(DynamicObject collator, Object locales, Object options);

    public static InitializeDateTimeFormatNode createInitalizeDateTimeFormatNode(JSContext context, String required, String defaults) {
        return InitializeDateTimeFormatNodeGen.create(context, required, defaults);
    }

    @Specialization
    public DynamicObject initializeDateTimeFormat(DynamicObject dateTimeFormatObj, Object localesArg, Object optionsArg) {

        JSDateTimeFormat.InternalState state = JSDateTimeFormat.getInternalState(dateTimeFormatObj);

        String[] locales = toCanonicalizedLocaleListNode.executeLanguageTags(localesArg);
        DynamicObject options = createOptionsNode.execute(optionsArg, required, defaults);

        // enforce validity check
        getLocaleMatcherOption.executeValue(options);
        getFormatMatcherOption.executeValue(options);

        String hcOpt = getHourCycleOption.executeValue(options);
        Boolean hour12Opt = getHour12Option.executeValue(options);

        String weekdayOpt = getWeekdayOption.executeValue(options);
        String eraOpt = getEraOption.executeValue(options);
        String yearOpt = getYearOption.executeValue(options);
        String monthOpt = getMonthOption.executeValue(options);
        String dayOpt = getDayOption.executeValue(options);
        String hourOpt = getHourOption.executeValue(options);
        String minuteOpt = getMinuteOption.executeValue(options);
        String secondOpt = getSecondOption.executeValue(options);
        String tzNameOpt = getTimeZoneNameOption.executeValue(options);

        JSDateTimeFormat.setupInternalDateTimeFormat(state, locales, options, weekdayOpt, eraOpt, yearOpt, monthOpt, dayOpt, hourOpt, hcOpt, hour12Opt, minuteOpt, secondOpt, tzNameOpt);

        return dateTimeFormatObj;
    }

    // https://tc39.github.io/ecma402/#sec-todatetimeoptions
    public abstract static class ToDateTimeOptionsNode extends JavaScriptBaseNode {

        @Child JSToObjectNode toObjectNode;
        private final JSContext context;

        public JSContext getContext() {
            return context;
        }

        public ToDateTimeOptionsNode(JSContext context) {
            super();
            this.context = context;
        }

        public abstract DynamicObject execute(Object opts, String required, String defaults);

        @SuppressWarnings("unused")
        @Specialization(guards = "isUndefined(opts)")
        public DynamicObject fromUndefined(Object opts, String required, String defaults) {
            return setDefaultsIfNeeded(JSUserObject.createWithPrototype(null, getContext()), required, defaults);
        }

        @Specialization(guards = "!isUndefined(opts)")
        public DynamicObject fromOtherThenUndefined(Object opts, String required, String defaults) {
            return setDefaultsIfNeeded(JSUserObject.createWithPrototype(toDynamicObject(opts), getContext()), required, defaults);
        }

        // from step 4 (Let needDefaults be true)
        private static DynamicObject setDefaultsIfNeeded(DynamicObject options, String required, String defaults) {
            boolean needDefaults = true;
            if (required != null) {
                if (required.equals("date") || required.equals("any")) {
                    if (!JSGuards.isUndefined(JSObject.get(options, "weekday")) || !JSGuards.isUndefined(JSObject.get(options, "year")) || !JSGuards.isUndefined(JSObject.get(options, "month")) ||
                                    !JSGuards.isUndefined(JSObject.get(options, "day"))) {
                        needDefaults = false;
                    }
                }
                if (required.equals("time") || required.equals("any")) {
                    if (!JSGuards.isUndefined(JSObject.get(options, "hour")) || !JSGuards.isUndefined(JSObject.get(options, "minute")) || !JSGuards.isUndefined(JSObject.get(options, "second"))) {
                        needDefaults = false;
                    }
                }
            }
            if (defaults != null) {
                if (needDefaults && (defaults.equals("date") || defaults.equals("all"))) {
                    JSRuntime.createDataPropertyOrThrow(options, "year", "numeric");
                    JSRuntime.createDataPropertyOrThrow(options, "month", "numeric");
                    JSRuntime.createDataPropertyOrThrow(options, "day", "numeric");
                }
                if (needDefaults && (defaults.equals("time") || defaults.equals("all"))) {
                    JSRuntime.createDataPropertyOrThrow(options, "hour", "numeric");
                    JSRuntime.createDataPropertyOrThrow(options, "minute", "numeric");
                    JSRuntime.createDataPropertyOrThrow(options, "second", "numeric");
                }
            }
            return options;
        }

        private DynamicObject toDynamicObject(Object o) {
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
            }
            return (DynamicObject) toObjectNode.executeTruffleObject(o);
        }
    }
}
