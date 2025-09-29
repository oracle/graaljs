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
package com.oracle.truffle.js.builtins.temporal;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;

import java.util.EnumSet;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthAddSubNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthCalendarGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthToJSONNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthToPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthUntilSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthWithNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.GetOptionsObjectNode;
import com.oracle.truffle.js.nodes.intl.InitializeDateTimeFormatNode;
import com.oracle.truffle.js.nodes.temporal.GetDifferenceSettingsNode;
import com.oracle.truffle.js.nodes.temporal.IsPartialTemporalObjectNode;
import com.oracle.truffle.js.nodes.temporal.RoundRelativeDurationNode;
import com.oracle.truffle.js.nodes.temporal.TemporalAddDateNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarDateFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.TemporalYearMonthFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarIdentifierNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalYearMonthNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormatObject;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.ISODateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonth;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonthObject;
import com.oracle.truffle.js.runtime.builtins.temporal.NormalizedDurationRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.ShowCalendar;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;
import com.oracle.truffle.js.runtime.util.TemporalUtil.UnitGroup;
import org.graalvm.shadowed.com.ibm.icu.util.Calendar;

public class TemporalPlainYearMonthPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainYearMonthPrototypeBuiltins.TemporalPlainYearMonthPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainYearMonthPrototypeBuiltins();

    protected TemporalPlainYearMonthPrototypeBuiltins() {
        super(JSTemporalPlainYearMonth.PROTOTYPE_NAME, TemporalPlainYearMonthPrototype.class);
    }

    public enum TemporalPlainYearMonthPrototype implements BuiltinEnum<TemporalPlainYearMonthPrototype> {
        // getters
        calendarId(0),
        era(0),
        eraYear(0),
        year(0),
        month(0),
        monthCode(0),
        daysInMonth(0),
        daysInYear(0),
        monthsInYear(0),
        inLeapYear(0),

        // methods
        with(1),
        add(1),
        subtract(1),
        until(1),
        since(1),
        equals(1),
        toString(0),
        toLocaleString(0),
        toJSON(0),
        valueOf(0),
        toPlainDate(1);

        private final int length;

        TemporalPlainYearMonthPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(calendarId, era, eraYear, year, month, monthCode, daysInMonth, daysInYear, monthsInYear, inLeapYear).contains(this);
        }

    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainYearMonthPrototype builtinEnum) {
        switch (builtinEnum) {
            case calendarId:
                return JSTemporalPlainYearMonthCalendarGetterNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case era:
            case eraYear:
            case year:
            case month:
            case monthCode:
            case daysInMonth:
            case daysInYear:
            case monthsInYear:
            case inLeapYear:
                return JSTemporalPlainYearMonthGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));
            case add:
                return JSTemporalPlainYearMonthAddSubNodeGen.create(context, builtin, TemporalUtil.ADD, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case subtract:
                return JSTemporalPlainYearMonthAddSubNodeGen.create(context, builtin, TemporalUtil.SUBTRACT, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case until:
                return JSTemporalPlainYearMonthUntilSinceNodeGen.create(context, builtin, TemporalUtil.UNTIL, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalPlainYearMonthUntilSinceNodeGen.create(context, builtin, TemporalUtil.SINCE, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case with:
                return JSTemporalPlainYearMonthWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case equals:
                return JSTemporalPlainYearMonthEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainDate:
                return JSTemporalPlainYearMonthToPlainDateNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toString:
                return JSTemporalPlainYearMonthToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
                return context.isOptionIntl402()
                                ? JSTemporalPlainYearMonthToLocaleStringNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context))
                                : JSTemporalPlainYearMonthToJSONNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toJSON:
                return JSTemporalPlainYearMonthToJSONNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return UnsupportedValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainYearMonthGetterNode extends JSBuiltinNode {

        protected final TemporalPlainYearMonthPrototype property;

        protected JSTemporalPlainYearMonthGetterNode(JSContext context, JSBuiltin builtin, TemporalPlainYearMonthPrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization
        protected Object dateGetter(JSTemporalPlainYearMonthObject temporalYM,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached InlinedConditionProfile isoCalendarProfile) {
            TruffleString calendar = temporalYM.getCalendar();
            boolean isoCalendar = isoCalendarProfile.profile(this, Strings.equals(equalNode, TemporalConstants.ISO8601, calendar));
            Calendar cal = null;
            if (!isoCalendar) {
                cal = IntlUtil.getCalendar(calendar, temporalYM.getYear(), temporalYM.getMonth(), temporalYM.getDay());
            }
            switch (property) {
                case era:
                    return isoCalendar ? Undefined.instance : IntlUtil.getEra(cal);
                case eraYear:
                    return isoCalendar ? Undefined.instance : IntlUtil.getEraYear(cal);
                case year:
                    return isoCalendar ? temporalYM.getYear() : IntlUtil.getExtendedYear(cal);
                case month:
                    return isoCalendar ? temporalYM.getMonth() : (IntlUtil.getCalendarField(cal, Calendar.ORDINAL_MONTH) + 1);
                case monthCode:
                    return isoCalendar ? TemporalUtil.buildISOMonthCode(temporalYM.getMonth()) : Strings.fromJavaString(IntlUtil.getTemporalMonthCode(cal));
                case daysInYear:
                    return isoCalendar ? TemporalUtil.isoDaysInYear(temporalYM.getYear()) : IntlUtil.getCalendarFieldMax(cal, Calendar.DAY_OF_YEAR);
                case daysInMonth:
                    return isoCalendar ? TemporalUtil.isoDaysInMonth(temporalYM.getYear(), temporalYM.getMonth()) : IntlUtil.getCalendarFieldMax(cal, Calendar.DAY_OF_MONTH);
                case monthsInYear:
                    return isoCalendar ? 12 : (IntlUtil.getCalendarFieldMax(cal, Calendar.ORDINAL_MONTH) + 1);
                case inLeapYear:
                    return isoCalendar ? JSDate.isLeapYear(temporalYM.getYear()) : IntlUtil.isLeapYear(cal);
            }
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    public abstract static class JSTemporalPlainYearMonthCalendarGetterNode extends JSBuiltinNode {

        protected JSTemporalPlainYearMonthCalendarGetterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString calendarId(JSTemporalPlainYearMonthObject yearMonth,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
            return toCalendarIdentifier.executeString(yearMonth.getCalendar());
        }

        @Specialization(guards = "!isJSTemporalYearMonth(yearMonth)")
        protected static TruffleString invalidReceiver(@SuppressWarnings("unused") Object yearMonth) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    // 4.3.20
    public abstract static class JSTemporalPlainYearMonthToString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(JSTemporalPlainYearMonthObject yearMonth, Object optParam,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSDynamicObject options = getOptionsObject(optParam, this, errorBranch, optionUndefined);
            ShowCalendar showCalendar = TemporalUtil.toShowCalendarOption(options, getOptionNode, equalNode);
            return JSTemporalPlainYearMonth.temporalYearMonthToString(yearMonth, showCalendar);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    public abstract static class JSTemporalPlainYearMonthToJSON extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthToJSON(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toJSON(JSTemporalPlainYearMonthObject yearMonth) {
            return JSTemporalPlainYearMonth.temporalYearMonthToString(yearMonth, ShowCalendar.AUTO);
        }

        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    public abstract static class JSTemporalPlainYearMonthToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public TruffleString toLocaleString(JSTemporalPlainYearMonthObject thisTime, Object locales, Object options,
                        @Cached("createDateDate(getContext())") InitializeDateTimeFormatNode initDateTimeFormatNode) {
            JSDateTimeFormatObject dateTimeFormatObj = JSDateTimeFormat.create(getContext(), getRealm());
            initDateTimeFormatNode.initialize(dateTimeFormatObj, locales, options);
            return JSDateTimeFormat.format(dateTimeFormatObj, thisTime);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object locales, Object options) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    public abstract static class JSTemporalPlainYearMonthToPlainDateNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthToPlainDateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalPlainDateObject toPlainDate(JSTemporalPlainYearMonthObject yearMonth, Object item,
                        @Cached TemporalCalendarDateFromFieldsNode dateFromFieldsNode,
                        @Cached InlinedBranchProfile errorBranch) {
            if (!JSRuntime.isObject(item)) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
            }

            JSContext ctx = getContext();
            TruffleString calendar = yearMonth.getCalendar();
            JSDynamicObject fields = TemporalUtil.isoDateToFields(ctx, calendar, yearMonth.isoDate(), TemporalUtil.FieldsType.YEAR_MONTH);
            JSDynamicObject inputFields = TemporalUtil.prepareCalendarFields(ctx, calendar, item, TemporalUtil.listD, TemporalUtil.listEmpty, TemporalUtil.listEmpty);
            JSDynamicObject mergedFields = TemporalUtil.calendarMergeFields(ctx, calendar, fields, inputFields);
            return dateFromFieldsNode.execute(calendar, mergedFields, TemporalUtil.Overflow.CONSTRAIN);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object item) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    public abstract static class JSTemporalPlainYearMonthEqualsNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthEqualsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean equals(JSTemporalPlainYearMonthObject yearMonth, Object otherParam,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier,
                        @Cached ToTemporalYearMonthNode toTemporalYearMonthNode) {
            JSTemporalPlainYearMonthObject other = toTemporalYearMonthNode.execute(otherParam, Undefined.instance);
            if (yearMonth.getMonth() != other.getMonth()) {
                return false;
            }
            if (yearMonth.getDay() != other.getDay()) {
                return false;
            }
            if (yearMonth.getYear() != other.getYear()) {
                return false;
            }
            return TemporalUtil.calendarEquals(yearMonth.getCalendar(), other.getCalendar(), toCalendarIdentifier);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object otherParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    public abstract static class JSTemporalPlainYearMonthWithNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthWithNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalPlainYearMonthObject with(JSTemporalPlainYearMonthObject yearMonth, Object temporalYearMonthLike, Object options,
                        @Cached IsPartialTemporalObjectNode isPartialTemporalObjectNode,
                        @Cached TemporalYearMonthFromFieldsNode yearMonthFromFieldsNode,
                        @Cached("create(getJSContext())") GetOptionsObjectNode getOptionsObject,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch) {
            if (!isPartialTemporalObjectNode.execute(temporalYearMonthLike)) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorPartialTemporalObjectExpected();
            }

            JSContext ctx = getContext();
            TruffleString calendar = yearMonth.getCalendar();
            JSDynamicObject fields = TemporalUtil.isoDateToFields(ctx, calendar, yearMonth.isoDate(), TemporalUtil.FieldsType.YEAR_MONTH);
            JSDynamicObject partialYearMonth = TemporalUtil.prepareCalendarFields(ctx, calendar, temporalYearMonthLike, TemporalUtil.listMMCY, TemporalUtil.listEmpty, null);
            fields = TemporalUtil.calendarMergeFields(ctx, calendar, fields, partialYearMonth);
            Object resolvedOptions = getOptionsObject.execute(options);
            TemporalUtil.Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            return yearMonthFromFieldsNode.execute(calendar, fields, overflow);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object temporalYearMonthLike, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    public abstract static class JSTemporalPlainYearMonthAddSubNode extends JSTemporalBuiltinOperation {

        private final int operation;

        protected JSTemporalPlainYearMonthAddSubNode(JSContext context, JSBuiltin builtin, int operation) {
            super(context, builtin);
            this.operation = operation;
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected JSTemporalPlainYearMonthObject addDurationToOrSubtractDurationFromPlainYearMonth(
                        JSTemporalPlainYearMonthObject yearMonth, Object temporalDurationLike, Object options,
                        @Bind Node node,
                        @Cached ToTemporalDurationNode toTemporalDurationNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached TemporalAddDateNode addDateNode,
                        @Cached TemporalYearMonthFromFieldsNode yearMonthFromFieldsNode,
                        @Cached TemporalCalendarDateFromFieldsNode dateFromFieldsNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSContext ctx = getContext();
            JSRealm realm = getRealm();
            JSTemporalDurationObject duration = toTemporalDurationNode.execute(temporalDurationLike);
            if (operation == TemporalUtil.SUBTRACT) {
                duration = JSTemporalDuration.createNegatedTemporalDuration(ctx, realm, duration);
            }
            Object resolvedOptions = getOptionsObject(options, node, errorBranch, optionUndefined);
            TemporalUtil.Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            int sign = TemporalUtil.durationSign(duration.getYears(), duration.getMonths(), duration.getWeeks(),
                            duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
            TruffleString calendar = yearMonth.getCalendar();
            JSDynamicObject fields = TemporalUtil.isoDateToFields(ctx, calendar, yearMonth.isoDate(), TemporalUtil.FieldsType.YEAR_MONTH);
            TemporalUtil.createDataPropertyOrThrow(ctx, fields, TemporalConstants.DAY, 1);
            JSTemporalPlainDateObject intermediateDate = dateFromFieldsNode.execute(calendar, fields, TemporalUtil.Overflow.CONSTRAIN);

            JSTemporalPlainDateObject date;
            if (sign < 0) {
                JSTemporalDurationObject oneMonthDuration = JSTemporalDuration.createTemporalDuration(ctx, realm,
                                0, 1, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
                JSTemporalPlainDateObject nextMonth = addDateNode.execute(calendar, intermediateDate, oneMonthDuration, TemporalUtil.Overflow.CONSTRAIN);
                ISODateRecord record = TemporalUtil.balanceISODate(nextMonth.getYear(), nextMonth.getMonth(), nextMonth.getDay() - 1);
                date = JSTemporalPlainDate.create(ctx, realm, record.year(), record.month(), record.day(), calendar, node, errorBranch);
            } else {
                date = intermediateDate;
            }

            JSTemporalDurationObject durationToAdd = TemporalUtil.toDateDurationRecordWithoutTime(ctx, realm, duration, this, errorBranch);
            JSTemporalPlainDateObject addedDate = addDateNode.execute(calendar, date, durationToAdd, overflow);
            JSObject addedDateFields = TemporalUtil.isoDateToFields(ctx, calendar, addedDate.isoDate(), TemporalUtil.FieldsType.YEAR_MONTH);
            return yearMonthFromFieldsNode.execute(calendar, addedDateFields, overflow);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object temporalDurationLike, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    public abstract static class JSTemporalPlainYearMonthUntilSinceNode extends JSTemporalBuiltinOperation {
        private static final EnumSet<Unit> WEEK_DAY_SET = EnumSet.of(Unit.WEEK, Unit.DAY);
        private final int sign;

        protected JSTemporalPlainYearMonthUntilSinceNode(JSContext context, JSBuiltin builtin, int sign) {
            super(context, builtin);
            this.sign = sign;
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected JSTemporalDurationObject differenceTemporalPlainYearMonth(JSTemporalPlainYearMonthObject yearMonth, Object otherParam, Object options,
                        @Bind Node node,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier,
                        @Cached GetDifferenceSettingsNode getDifferenceSettings,
                        @Cached RoundRelativeDurationNode roundRelativeDuration,
                        @Cached ToTemporalYearMonthNode toTemporalYearMonthNode,
                        @Cached TemporalCalendarDateFromFieldsNode dateFromFieldsNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSTemporalPlainYearMonthObject other = toTemporalYearMonthNode.execute(otherParam, Undefined.instance);
            TruffleString calendar = yearMonth.getCalendar();
            if (!TemporalUtil.calendarEquals(calendar, other.getCalendar(), toCalendarIdentifier)) {
                errorBranch.enter(node);
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }
            JSDynamicObject resolvedOptions = getOptionsObject(options, node, errorBranch, optionUndefined);
            var settings = getDifferenceSettings.execute(sign, resolvedOptions, UnitGroup.DATE, WEEK_DAY_SET, Unit.MONTH, Unit.YEAR);

            JSContext ctx = getContext();
            JSRealm realm = getRealm();
            if (TemporalUtil.compareISODate(yearMonth.getYear(), yearMonth.getMonth(), yearMonth.getDay(), other.getYear(), other.getMonth(), other.getDay()) == 0) {
                return JSTemporalDuration.createTemporalDuration(ctx, realm, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
            }

            JSObject thisFields = TemporalUtil.isoDateToFields(ctx, calendar, yearMonth.isoDate(), TemporalUtil.FieldsType.YEAR_MONTH);
            TemporalUtil.createDataPropertyOrThrow(ctx, thisFields, DAY, 1);
            JSTemporalPlainDateObject thisDate = dateFromFieldsNode.execute(calendar, thisFields, TemporalUtil.Overflow.CONSTRAIN);

            JSObject otherFields = TemporalUtil.isoDateToFields(ctx, calendar, other.isoDate(), TemporalUtil.FieldsType.YEAR_MONTH);
            TemporalUtil.createDataPropertyOrThrow(ctx, otherFields, DAY, 1);
            JSTemporalPlainDateObject otherDate = dateFromFieldsNode.execute(calendar, otherFields, TemporalUtil.Overflow.CONSTRAIN);

            JSTemporalDurationObject result = TemporalUtil.calendarDateUntil(ctx, realm, calendar, thisDate, otherDate, settings.largestUnit(), this, errorBranch);
            NormalizedDurationRecord duration = TemporalUtil.createNormalizedDurationRecord(result.getYears(), result.getMonths(), 0, 0, TemporalUtil.zeroTimeDuration());
            double durationYears = duration.years();
            double durationMonths = duration.months();
            boolean roundingGranularityIsNoop = settings.smallestUnit() == Unit.MONTH && settings.roundingIncrement() == 1;
            if (!roundingGranularityIsNoop) {
                var isoDateTime = new ISODateTimeRecord(thisDate.getYear(), thisDate.getMonth(), thisDate.getDay(), 0, 0, 0, 0, 0, 0);
                BigInt originEpochNs = TemporalUtil.getUTCEpochNanoseconds(isoDateTime.year(), isoDateTime.month(), isoDateTime.day(), 0, 0, 0, 0, 0, 0);
                BigInt destEpochNs = TemporalUtil.getUTCEpochNanoseconds(otherDate.getYear(), otherDate.getMonth(), otherDate.getDay(), 0, 0, 0, 0, 0, 0);
                var roundedDuration = roundRelativeDuration.execute(duration, originEpochNs, destEpochNs, isoDateTime, calendar, null,
                                settings.largestUnit(), settings.roundingIncrement(), settings.smallestUnit(), settings.roundingMode()).duration();
                durationYears = roundedDuration.getYears();
                durationMonths = roundedDuration.getMonths();
            }
            return JSTemporalDuration.createTemporalDuration(ctx, realm, sign * durationYears, sign * durationMonths, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object otherParam, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }
}
