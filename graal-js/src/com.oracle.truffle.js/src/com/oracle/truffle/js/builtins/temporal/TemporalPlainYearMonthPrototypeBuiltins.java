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
package com.oracle.truffle.js.builtins.temporal;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.AUTO;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.OVERFLOW;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.REJECT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;

import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltins.JSTemporalBuiltinOperation;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthAddNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthSubtractNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthToPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthUntilNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthValueOfNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthWithNodeGen;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonth;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonthObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.ShowCalendar;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

public class TemporalPlainYearMonthPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainYearMonthPrototypeBuiltins.TemporalPlainYearMonthPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainYearMonthPrototypeBuiltins();

    protected TemporalPlainYearMonthPrototypeBuiltins() {
        super(JSTemporalPlainYearMonth.PROTOTYPE_NAME, TemporalPlainYearMonthPrototype.class);
    }

    public enum TemporalPlainYearMonthPrototype implements BuiltinEnum<TemporalPlainYearMonthPrototype> {
        // getters
        calendar(0),
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
            return EnumSet.of(calendar, year, month, monthCode, daysInMonth, daysInYear, monthsInYear, inLeapYear).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainYearMonthPrototype builtinEnum) {
        switch (builtinEnum) {
            case calendar:
            case year:
            case month:
            case monthCode:
            case daysInMonth:
            case daysInYear:
            case monthsInYear:
            case inLeapYear:
                return JSTemporalPlainYearMonthGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

            case add:
                return JSTemporalPlainYearMonthAddNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case subtract:
                return JSTemporalPlainYearMonthSubtractNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case until:
                return JSTemporalPlainYearMonthUntilNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalPlainYearMonthSinceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
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
                return JSTemporalPlainYearMonthValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainYearMonthGetterNode extends JSBuiltinNode {

        public final TemporalPlainYearMonthPrototype property;

        public JSTemporalPlainYearMonthGetterNode(JSContext context, JSBuiltin builtin, TemporalPlainYearMonthPrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization(guards = "isJSTemporalYearMonth(thisObj)")
        protected Object dateGetter(Object thisObj) {
            JSTemporalPlainYearMonthObject temporalYM = (JSTemporalPlainYearMonthObject) thisObj;
            switch (property) {
                case calendar:
                    return temporalYM.getCalendar();
                case year:
                    return TemporalUtil.calendarYear(temporalYM.getCalendar(), temporalYM);
                case month:
                    return TemporalUtil.calendarMonth(temporalYM.getCalendar(), temporalYM);
                case monthCode:
                    return TemporalUtil.calendarMonthCode(temporalYM.getCalendar(), temporalYM);
                case daysInYear:
                    return TemporalUtil.calendarDaysInYear(temporalYM.getCalendar(), temporalYM);
                case daysInMonth:
                    return TemporalUtil.calendarDaysInMonth(temporalYM.getCalendar(), temporalYM);
                case monthsInYear:
                    return TemporalUtil.calendarMonthsInYear(temporalYM.getCalendar(), temporalYM);
                case inLeapYear:
                    return TemporalUtil.calendarInLeapYear(temporalYM.getCalendar(), temporalYM);
            }
            CompilerDirectives.transferToInterpreter();
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalYearMonth(thisObj)")
        protected static int error(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    // 4.3.20
    public abstract static class JSTemporalPlainYearMonthToString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(Object thisObj, Object optParam) {
            JSTemporalPlainYearMonthObject md = requireTemporalYearMonth(thisObj);
            DynamicObject options = getOptionsObject(optParam);
            ShowCalendar showCalendar = TemporalUtil.toShowCalendarOption(options, getOptionNode());
            return JSTemporalPlainYearMonth.temporalYearMonthToString(md, showCalendar);
        }
    }

    public abstract static class JSTemporalPlainYearMonthToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public TruffleString toLocaleString(Object thisObj) {
            JSTemporalPlainYearMonthObject time = requireTemporalYearMonth(thisObj);
            return JSTemporalPlainYearMonth.temporalYearMonthToString(time, ShowCalendar.AUTO);
        }
    }

    public abstract static class JSTemporalPlainYearMonthValueOf extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }

    public abstract static class JSTemporalPlainYearMonthToPlainDateNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthToPlainDateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object toPlainDate(Object thisObj, Object item,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalPlainYearMonthObject yearMonth = requireTemporalYearMonth(thisObj);
            if (!JSRuntime.isObject(item)) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
            }
            DynamicObject calendar = yearMonth.getCalendar();
            List<TruffleString> receiverFieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.listMCY);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), yearMonth, receiverFieldNames, TemporalUtil.listEmpty);
            List<TruffleString> inputFieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.listD);
            DynamicObject inputFields = TemporalUtil.prepareTemporalFields(getContext(), (DynamicObject) item, inputFieldNames, TemporalUtil.listEmpty);
            DynamicObject mergedFields = TemporalUtil.calendarMergeFields(getContext(), namesNode, calendar, fields, inputFields);
            List<TruffleString> mergedFieldNames = TemporalUtil.listJoinRemoveDuplicates(receiverFieldNames, inputFieldNames);
            mergedFields = TemporalUtil.prepareTemporalFields(getContext(), mergedFields, mergedFieldNames, TemporalUtil.listEmpty);
            DynamicObject options = JSOrdinary.createWithNullPrototype(getContext());
            TemporalUtil.createDataPropertyOrThrow(getContext(), options, OVERFLOW, REJECT);
            return TemporalUtil.dateFromFields(calendar, mergedFields, options);
        }
    }

    public abstract static class JSTemporalPlainYearMonthGetISOFields extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject getISOFields(Object thisObj) {
            JSTemporalPlainYearMonthObject ym = requireTemporalYearMonth(thisObj);
            DynamicObject obj = JSOrdinary.create(getContext(), getRealm());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, CALENDAR, ym.getCalendar());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_DAY, ym.getDay());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_MONTH, ym.getMonth());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_YEAR, ym.getYear());
            return obj;
        }
    }

    public abstract static class JSTemporalPlainYearMonthEqualsNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthEqualsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean equals(Object thisObj, Object otherParam,
                        @Cached JSToStringNode toStringNode,
                        @Cached TemporalGetOptionNode getOptionNode) {
            JSTemporalPlainYearMonthObject md = requireTemporalYearMonth(thisObj);
            JSTemporalPlainYearMonthObject other = (JSTemporalPlainYearMonthObject) TemporalUtil.toTemporalYearMonth(getContext(), getRealm(), otherParam, Undefined.instance, getOptionNode);
            if (md.getMonth() != other.getMonth()) {
                return false;
            }
            if (md.getDay() != other.getDay()) {
                return false;
            }
            if (md.getYear() != other.getYear()) {
                return false;
            }
            return TemporalUtil.calendarEquals(md.getCalendar(), other.getCalendar(), toStringNode);
        }
    }

    public abstract static class JSTemporalPlainYearMonthWithNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthWithNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject with(Object thisObj, Object temporalYearMonthLike, Object optParam,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalPlainYearMonthObject ym = requireTemporalYearMonth(thisObj);
            if (!JSRuntime.isObject(temporalYearMonthLike)) {
                errorBranch.enter();
                throw Errors.createTypeError("Object expected");
            }
            DynamicObject ymLikeObj = (DynamicObject) temporalYearMonthLike;
            TemporalUtil.rejectTemporalCalendarType(ymLikeObj, errorBranch);
            Object calendarProperty = JSObject.get(ymLikeObj, CALENDAR);
            if (calendarProperty != Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorUnexpectedCalendar();
            }
            Object timezoneProperty = JSObject.get(ymLikeObj, TIME_ZONE);
            if (timezoneProperty != Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorUnexpectedTimeZone();
            }
            DynamicObject calendar = ym.getCalendar();
            List<TruffleString> fieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.listMMCY);
            DynamicObject partialMonthDay = TemporalUtil.preparePartialTemporalFields(getContext(), ymLikeObj, fieldNames);
            DynamicObject options = getOptionsObject(optParam);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), ym, fieldNames, TemporalUtil.listEmpty);
            fields = TemporalUtil.calendarMergeFields(getContext(), namesNode, calendar, fields, partialMonthDay);
            fields = TemporalUtil.prepareTemporalFields(getContext(), fields, fieldNames, TemporalUtil.listEmpty);
            return TemporalUtil.yearMonthFromFields(calendar, fields, options);
        }
    }

    public abstract static class JSTemporalPlainYearMonthAddNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthAddNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject add(Object thisObj, Object temporalDurationLike, Object optParam,
                        @Cached("create()") JSToStringNode toStringNode,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalPlainYearMonthObject ym = requireTemporalYearMonth(thisObj);

            JSTemporalDurationRecord duration = TemporalUtil.toLimitedTemporalDuration(temporalDurationLike, TemporalUtil.listEmpty, isObjectNode, toStringNode);
            JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDuration(getContext(), namesNode, duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), Unit.DAY);
            DynamicObject options = getOptionsObject(optParam);
            DynamicObject calendar = ym.getCalendar();
            List<TruffleString> fieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.listMCY);
            int sign = TemporalUtil.durationSign(duration.getYears(), duration.getMonths(), duration.getWeeks(), balanceResult.getDays(), 0, 0, 0, 0, 0, 0);
            int day = 0;
            if (sign < 0) {
                Object dayFromCalendar = TemporalUtil.calendarDaysInMonth(calendar, ym);
                day = TemporalUtil.toPositiveIntegerConstrainInt(dayFromCalendar);
            } else {
                day = 1;
            }
            DynamicObject date = TemporalUtil.createTemporalDate(getContext(), ym.getYear(), ym.getMonth(), day, calendar);
            DynamicObject durationToAdd = JSTemporalDuration.createTemporalDuration(getContext(), duration.getYears(), duration.getMonths(), duration.getWeeks(), balanceResult.getDays(), 0,
                            0, 0, 0, 0, 0);
            DynamicObject addedDate = TemporalUtil.calendarDateAdd(calendar, date, durationToAdd, options, Undefined.instance);
            DynamicObject addedDateFields = TemporalUtil.prepareTemporalFields(getContext(), addedDate, fieldNames, TemporalUtil.listEmpty);
            return TemporalUtil.yearMonthFromFields(calendar, addedDateFields, options);
        }
    }

    public abstract static class JSTemporalPlainYearMonthSubtractNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthSubtractNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject subtract(Object thisObj, Object temporalDurationLike, Object optParam,
                        @Cached("create()") JSToStringNode toStringNode,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalPlainYearMonthObject ym = requireTemporalYearMonth(thisObj);

            JSTemporalDurationRecord duration = TemporalUtil.toLimitedTemporalDuration(temporalDurationLike, TemporalUtil.listEmpty, isObjectNode, toStringNode);
            duration = TemporalUtil.createNegatedTemporalDuration(duration); // #1862, PR 2002
            JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDuration(getContext(), namesNode, duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), Unit.DAY);
            DynamicObject options = getOptionsObject(optParam);
            DynamicObject calendar = ym.getCalendar();
            List<TruffleString> fieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.listMCY);
            int sign = TemporalUtil.durationSign(duration.getYears(), duration.getMonths(), duration.getWeeks(), balanceResult.getDays(), 0, 0, 0, 0, 0, 0);
            int day = 0;
            if (sign < 0) {
                day = TemporalUtil.toPositiveIntegerConstrainInt(TemporalUtil.calendarDaysInMonth(calendar, ym));
            } else {
                day = 1;
            }
            DynamicObject date = TemporalUtil.createTemporalDate(getContext(), ym.getYear(), ym.getMonth(), day, calendar);
            DynamicObject durationToAdd = JSTemporalDuration.createTemporalDuration(getContext(), duration.getYears(), duration.getMonths(), duration.getWeeks(), balanceResult.getDays(), 0, 0, 0,
                            0, 0, 0);
            DynamicObject addedDate = TemporalUtil.calendarDateAdd(calendar, date, durationToAdd, options, Undefined.instance);
            DynamicObject addedDateFields = TemporalUtil.prepareTemporalFields(getContext(), addedDate, fieldNames, TemporalUtil.listEmpty);
            return TemporalUtil.yearMonthFromFields(calendar, addedDateFields, options);
        }
    }

    public abstract static class JSTemporalPlainYearMonthUntilNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthUntilNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object until(Object thisObj, Object otherParam, Object optParam,
                        @Cached("create()") JSToStringNode toStringNode,
                        @Cached("create()") JSToBooleanNode toBooleanNode,
                        @Cached("create()") JSToNumberNode toNumberNode,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached TemporalGetOptionNode getOptionNode) {
            JSTemporalPlainYearMonthObject ym = requireTemporalYearMonth(thisObj);
            JSTemporalPlainYearMonthObject other = (JSTemporalPlainYearMonthObject) TemporalUtil.toTemporalYearMonth(getContext(), getRealm(), otherParam, Undefined.instance, getOptionNode);
            DynamicObject calendar = ym.getCalendar();
            if (!TemporalUtil.calendarEquals(calendar, other.getCalendar(), toStringNode)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }
            DynamicObject options = getOptionsObject(optParam);
            List<TruffleString> disallowedUnits = TemporalUtil.listWDHMSMMN;
            Unit smallestUnit = toSmallestTemporalUnit(options, disallowedUnits, MONTH);
            Unit largestUnit = toLargestTemporalUnit(options, disallowedUnits, AUTO, Unit.YEAR);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, null, false, isObjectNode, toNumberNode);
            List<TruffleString> fieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.listMCY);
            DynamicObject otherFields = TemporalUtil.prepareTemporalFields(getContext(), other, fieldNames, TemporalUtil.listEmpty);
            TemporalUtil.createDataPropertyOrThrow(getContext(), otherFields, DAY, 1);
            DynamicObject otherDate = TemporalUtil.dateFromFields(calendar, otherFields, Undefined.instance);
            DynamicObject thisFields = TemporalUtil.prepareTemporalFields(getContext(), ym, fieldNames, TemporalUtil.listEmpty);
            TemporalUtil.createDataPropertyOrThrow(getContext(), thisFields, DAY, 1);
            JSTemporalPlainDateObject thisDate = TemporalUtil.dateFromFields(calendar, thisFields, Undefined.instance);
            DynamicObject untilOptions = TemporalUtil.mergeLargestUnitOption(getContext(), namesNode, options, largestUnit);
            JSTemporalDurationObject result = TemporalUtil.calendarDateUntil(calendar, thisDate, otherDate, untilOptions, Undefined.instance);
            if (Unit.MONTH == smallestUnit && roundingIncrement == 1) {
                return JSTemporalDuration.createTemporalDuration(getContext(), result.getYears(), result.getMonths(), 0, 0, 0, 0, 0, 0, 0, 0);
            }
            JSTemporalDurationRecord result2 = TemporalUtil.roundDuration(getContext(), getRealm(), namesNode, result.getYears(), result.getMonths(), 0, 0, 0, 0, 0, 0, 0, 0, (long) roundingIncrement,
                            smallestUnit, roundingMode, thisDate);
            return JSTemporalDuration.createTemporalDuration(getContext(), result2.getYears(), result2.getMonths(), 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    public abstract static class JSTemporalPlainYearMonthSinceNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainYearMonthSinceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object since(Object thisObj, Object otherParam, Object optParam,
                        @Cached("create()") JSToNumberNode toNumberNode,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached JSToStringNode toStringNode,
                        @Cached TemporalGetOptionNode getOptionNode) {
            JSTemporalPlainYearMonthObject ym = requireTemporalYearMonth(thisObj);
            JSTemporalPlainYearMonthObject other = (JSTemporalPlainYearMonthObject) TemporalUtil.toTemporalYearMonth(getContext(), getRealm(), otherParam, Undefined.instance, getOptionNode);
            DynamicObject calendar = ym.getCalendar();
            if (!TemporalUtil.calendarEquals(calendar, other.getCalendar(), toStringNode)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }
            DynamicObject options = getOptionsObject(optParam);
            List<TruffleString> disallowedUnits = TemporalUtil.listWDHMSMMN;
            Unit smallestUnit = toSmallestTemporalUnit(options, disallowedUnits, MONTH);
            Unit largestUnit = toLargestTemporalUnit(options, disallowedUnits, AUTO, Unit.YEAR);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC);
            roundingMode = TemporalUtil.negateTemporalRoundingMode(roundingMode);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, null, false, isObjectNode, toNumberNode);
            List<TruffleString> fieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.listMCY);
            DynamicObject otherFields = TemporalUtil.prepareTemporalFields(getContext(), other, fieldNames, TemporalUtil.listEmpty);
            TemporalUtil.createDataPropertyOrThrow(getContext(), otherFields, DAY, 1);
            DynamicObject otherDate = TemporalUtil.dateFromFields(calendar, otherFields, Undefined.instance);
            DynamicObject thisFields = TemporalUtil.prepareTemporalFields(getContext(), ym, fieldNames, TemporalUtil.listEmpty);
            TemporalUtil.createDataPropertyOrThrow(getContext(), thisFields, DAY, 1);
            JSTemporalPlainDateObject thisDate = TemporalUtil.dateFromFields(calendar, thisFields, Undefined.instance);
            DynamicObject untilOptions = TemporalUtil.mergeLargestUnitOption(getContext(), namesNode, options, largestUnit);
            JSTemporalDurationObject result = TemporalUtil.calendarDateUntil(calendar, thisDate, otherDate, untilOptions, Undefined.instance);
            if (Unit.MONTH == smallestUnit && roundingIncrement == 1) {
                return JSTemporalDuration.createTemporalDuration(getContext(), -result.getYears(), -result.getMonths(), 0, 0, 0, 0, 0, 0, 0, 0);
            }
            JSTemporalDurationRecord result2 = TemporalUtil.roundDuration(getContext(), getRealm(), namesNode, result.getYears(), result.getMonths(), 0, 0, 0, 0, 0, 0, 0, 0, (long) roundingIncrement,
                            smallestUnit, roundingMode, thisDate);
            return JSTemporalDuration.createTemporalDuration(getContext(), -result2.getYears(), -result2.getMonths(), 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}
