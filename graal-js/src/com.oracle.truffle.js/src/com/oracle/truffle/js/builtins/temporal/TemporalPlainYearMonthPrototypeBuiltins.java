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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;

import java.util.EnumSet;
import java.util.List;

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
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthToPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthUntilSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthWithNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.GetOptionsObjectNode;
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
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
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
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.ShowCalendar;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

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
        toPlainDate(1),
        getISOFields(0);

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
            case getISOFields:
                return JSTemporalPlainYearMonthGetISOFieldsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toString:
                return JSTemporalPlainYearMonthToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalPlainYearMonthToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
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
        protected Object dateGetter(JSTemporalPlainYearMonthObject temporalYM) {
            switch (property) {
                case era:
                    return Undefined.instance;
                case eraYear:
                    return Undefined.instance;
                case year:
                    return temporalYM.getYear();
                case month:
                    return temporalYM.getMonth();
                case monthCode:
                    return TemporalUtil.buildISOMonthCode(temporalYM.getMonth());
                case daysInYear:
                    return TemporalUtil.isoDaysInYear(temporalYM.getYear());
                case daysInMonth:
                    return TemporalUtil.isoDaysInMonth(temporalYM.getYear(), temporalYM.getMonth());
                case monthsInYear:
                    return 12;
                case inLeapYear:
                    return JSDate.isLeapYear(temporalYM.getYear());
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

    public abstract static class JSTemporalPlainYearMonthToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toLocaleString(JSTemporalPlainYearMonthObject yearMonth) {
            return JSTemporalPlainYearMonth.temporalYearMonthToString(yearMonth, ShowCalendar.AUTO);
        }

        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
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
            TruffleString calendar = yearMonth.getCalendar();
            List<TruffleString> receiverFieldNames = TemporalUtil.listMCY;
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), yearMonth, receiverFieldNames, TemporalUtil.listEmpty);
            List<TruffleString> inputFieldNames = TemporalUtil.listD;
            JSDynamicObject inputFields = TemporalUtil.prepareTemporalFields(getContext(), TemporalUtil.toJSDynamicObject(item, this, errorBranch), inputFieldNames, TemporalUtil.listEmpty);
            JSDynamicObject mergedFields = TemporalUtil.calendarMergeFields(getContext(), calendar, fields, inputFields);
            List<TruffleString> mergedFieldNames = TemporalUtil.listJoinRemoveDuplicates(receiverFieldNames, inputFieldNames);
            mergedFields = TemporalUtil.prepareTemporalFields(getContext(), mergedFields, mergedFieldNames, TemporalUtil.listEmpty);
            return dateFromFieldsNode.execute(calendar, mergedFields, TemporalUtil.Overflow.CONSTRAIN);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object item) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    public abstract static class JSTemporalPlainYearMonthGetISOFields extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSObject getISOFields(JSTemporalPlainYearMonthObject ym) {
            JSObject obj = JSOrdinary.create(getContext(), getRealm());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, CALENDAR, ym.getCalendar());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_DAY, ym.getDay());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_MONTH, ym.getMonth());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_YEAR, ym.getYear());
            return obj;
        }

        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
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

            TruffleString calendar = yearMonth.getCalendar();

            List<TruffleString> fieldNames = TemporalUtil.listMMCY;
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), yearMonth, fieldNames, TemporalUtil.listEmpty);
            JSDynamicObject partialYearMonth = TemporalUtil.prepareTemporalFields(getContext(), temporalYearMonthLike, fieldNames, null);
            fields = TemporalUtil.calendarMergeFields(getContext(), calendar, fields, partialYearMonth);
            fields = TemporalUtil.prepareTemporalFields(getContext(), fields, fieldNames, TemporalUtil.listEmpty);
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
            JSTemporalDurationObject duration = toTemporalDurationNode.execute(temporalDurationLike);
            JSRealm realm = getRealm();
            if (operation == TemporalUtil.SUBTRACT) {
                duration = JSTemporalDuration.createNegatedTemporalDuration(getContext(), realm, duration);
            }
            Object resolvedOptions = getOptionsObject(options, node, errorBranch, optionUndefined);
            TemporalUtil.Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            int sign = TemporalUtil.durationSign(duration.getYears(), duration.getMonths(), duration.getWeeks(),
                            duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
            TruffleString calendar = yearMonth.getCalendar();

            List<TruffleString> fieldNames = TemporalUtil.listMCY;
            JSObject fields = TemporalUtil.prepareTemporalFields(getContext(), yearMonth, fieldNames, TemporalUtil.listEmpty);
            TemporalUtil.createDataPropertyOrThrow(getContext(), fields, TemporalConstants.DAY, 1);

            JSTemporalPlainDateObject intermediateDate = dateFromFieldsNode.execute(calendar, fields, TemporalUtil.Overflow.CONSTRAIN);

            JSTemporalPlainDateObject date;
            if (sign < 0) {
                JSTemporalDurationObject oneMonthDuration = JSTemporalDuration.createTemporalDuration(getContext(), realm,
                                0, 1, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
                JSTemporalPlainDateObject nextMonth = addDateNode.execute(calendar, intermediateDate, oneMonthDuration, TemporalUtil.Overflow.CONSTRAIN);
                ISODateRecord record = TemporalUtil.balanceISODate(nextMonth.getYear(), nextMonth.getMonth(), nextMonth.getDay() - 1);
                date = JSTemporalPlainDate.create(getContext(), realm, record.year(), record.month(), record.day(), calendar, node, errorBranch);
            } else {
                date = intermediateDate;
            }

            JSTemporalDurationObject durationToAdd = TemporalUtil.toDateDurationRecordWithoutTime(getContext(), realm, duration, this, errorBranch);
            JSTemporalPlainDateObject addedDate = addDateNode.execute(calendar, date, durationToAdd, overflow);
            JSObject addedDateFields = TemporalUtil.prepareTemporalFields(getContext(), addedDate, fieldNames, TemporalUtil.listEmpty);
            return yearMonthFromFieldsNode.execute(calendar, addedDateFields, overflow);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object temporalDurationLike, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    public abstract static class JSTemporalPlainYearMonthUntilSinceNode extends JSTemporalBuiltinOperation {

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
            var settings = getDifferenceSettings.execute(sign, resolvedOptions, TemporalUtil.unitMappingYearMonthOrAuto, TemporalUtil.unitMappingYearMonth, Unit.MONTH, Unit.YEAR);

            JSRealm realm = getRealm();
            if (TemporalUtil.compareISODate(yearMonth.getYear(), yearMonth.getMonth(), yearMonth.getDay(), other.getYear(), other.getMonth(), other.getDay()) == 0) {
                return JSTemporalDuration.createTemporalDuration(getContext(), realm, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
            }

            List<TruffleString> fieldNames = TemporalUtil.listMCY;
            JSDynamicObject otherFields = TemporalUtil.prepareTemporalFields(getContext(), other, fieldNames, TemporalUtil.listEmpty);
            TemporalUtil.createDataPropertyOrThrow(getContext(), otherFields, DAY, 1);
            JSTemporalPlainDateObject otherDate = dateFromFieldsNode.execute(calendar, otherFields, TemporalUtil.Overflow.CONSTRAIN);
            JSObject thisFields = TemporalUtil.prepareTemporalFields(getContext(), yearMonth, fieldNames, TemporalUtil.listEmpty);
            TemporalUtil.createDataPropertyOrThrow(getContext(), thisFields, DAY, 1);
            JSTemporalPlainDateObject thisDate = dateFromFieldsNode.execute(calendar, thisFields, TemporalUtil.Overflow.CONSTRAIN);
            JSTemporalDurationObject result = TemporalUtil.calendarDateUntil(getContext(), realm, calendar, thisDate, otherDate, settings.largestUnit(), this, errorBranch);
            NormalizedDurationRecord duration = TemporalUtil.createNormalizedDurationRecord(result.getYears(), result.getMonths(), 0, 0, TemporalUtil.zeroTimeDuration());
            double durationYears = duration.years();
            double durationMonths = duration.months();
            boolean roundingGranularityIsNoop = settings.smallestUnit() == Unit.MONTH && settings.roundingIncrement() == 1;
            if (!roundingGranularityIsNoop) {
                BigInt destEpochNs = TemporalUtil.getUTCEpochNanoseconds(otherDate.getYear(), otherDate.getMonth(), otherDate.getDay(), 0, 0, 0, 0, 0, 0);
                var dateTime = new ISODateTimeRecord(thisDate.getYear(), thisDate.getMonth(), thisDate.getDay(), 0, 0, 0, 0, 0, 0);
                var roundedDuration = roundRelativeDuration.execute(duration, destEpochNs, dateTime, calendar, null,
                                settings.largestUnit(), settings.roundingIncrement(), settings.smallestUnit(), settings.roundingMode()).duration();
                durationYears = roundedDuration.getYears();
                durationMonths = roundedDuration.getMonths();
            }
            return JSTemporalDuration.createTemporalDuration(getContext(), realm, sign * durationYears, sign * durationMonths, 0, 0, 0, 0, 0, 0, 0, 0, node, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object otherParam, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }
}
