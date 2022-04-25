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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.LARGEST_UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ROUNDING_MODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SMALLEST_UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.UNIT;

import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateAddNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateSubtractNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToPlainDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToPlainMonthDayNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToPlainYearMonthNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateToZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateUntilNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateValueOfNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateWithCalendarNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltinsFactory.JSTemporalPlainDateWithNodeGen;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarDateFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarGetterNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.TemporalMonthDayFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalRoundDurationNode;
import com.oracle.truffle.js.nodes.temporal.TemporalYearMonthFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.ToLimitedTemporalDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDateNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendarObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDayObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonthObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZoneObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalTime;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Disambiguation;
import com.oracle.truffle.js.runtime.util.TemporalUtil.OptionType;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.ShowCalendar;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

public class TemporalPlainDatePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainDatePrototypeBuiltins.TemporalPlainDatePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainDatePrototypeBuiltins();

    protected TemporalPlainDatePrototypeBuiltins() {
        super(JSTemporalPlainDate.PROTOTYPE_NAME, TemporalPlainDatePrototype.class);
    }

    public enum TemporalPlainDatePrototype implements BuiltinEnum<TemporalPlainDatePrototype> {
        // getters
        calendar(0),
        year(0),
        month(0),
        monthCode(0),
        day(0),
        dayOfYear(0),
        dayOfWeek(0),
        weekOfYear(0),
        daysInWeek(0),
        daysInMonth(0),
        daysInYear(0),
        monthsInYear(0),
        inLeapYear(0),

        // methods
        toPlainYearMonth(0),
        toPlainMonthDay(0),
        getISOFields(0),
        add(1),
        subtract(1),
        with(1),
        withCalendar(1),
        until(1),
        since(1),
        equals(1),
        toPlainDateTime(0),
        toZonedDateTime(1),
        toString(0),
        toLocaleString(0),
        toJSON(0),
        valueOf(0);

        private final int length;

        TemporalPlainDatePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(calendar, year, month, monthCode, day, dayOfYear, dayOfWeek, weekOfYear, daysInWeek, daysInMonth, daysInYear,
                            monthsInYear, inLeapYear).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainDatePrototype builtinEnum) {
        switch (builtinEnum) {
            case calendar:
            case year:
            case month:
            case monthCode:
            case day:
            case dayOfYear:
            case dayOfWeek:
            case weekOfYear:
            case daysInWeek:
            case daysInMonth:
            case daysInYear:
            case monthsInYear:
            case inLeapYear:
                return JSTemporalPlainDateGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

            case add:
                return JSTemporalPlainDateAddNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case subtract:
                return JSTemporalPlainDateSubtractNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case with:
                return JSTemporalPlainDateWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case withCalendar:
                return JSTemporalPlainDateWithCalendarNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case until:
                return JSTemporalPlainDateUntilNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalPlainDateSinceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case equals:
                return JSTemporalPlainDateEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainDateTime:
                return JSTemporalPlainDateToPlainDateTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainYearMonth:
                return JSTemporalPlainDateToPlainYearMonthNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainMonthDay:
                return JSTemporalPlainDateToPlainMonthDayNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toZonedDateTime:
                return JSTemporalPlainDateToZonedDateTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case getISOFields:
                return JSTemporalPlainDateGetISOFieldsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toString:
                return JSTemporalPlainDateToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalPlainDateToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSTemporalPlainDateValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainDateGetterNode extends JSBuiltinNode {

        public final TemporalPlainDatePrototype property;

        public JSTemporalPlainDateGetterNode(JSContext context, JSBuiltin builtin, TemporalPlainDatePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization(guards = "isJSTemporalDate(thisObj)")
        protected Object dateGetter(Object thisObj,
                        @Cached("create(getContext())") TemporalCalendarGetterNode calendarGetterNode) {
            JSTemporalPlainDateObject temporalDT = (JSTemporalPlainDateObject) thisObj;
            switch (property) {
                case calendar:
                    return temporalDT.getCalendar();
                case year:
                    return TemporalUtil.calendarYear(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case month:
                    return TemporalUtil.calendarMonth(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case day:
                    return TemporalUtil.calendarDay(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case dayOfWeek:
                    return TemporalUtil.calendarDayOfWeek(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case dayOfYear:
                    return TemporalUtil.calendarDayOfYear(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case monthCode:
                    return TemporalUtil.calendarMonthCode(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case weekOfYear:
                    return TemporalUtil.calendarWeekOfYear(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case daysInWeek:
                    return TemporalUtil.calendarDaysInWeek(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case daysInMonth:
                    return TemporalUtil.calendarDaysInMonth(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case daysInYear:
                    return TemporalUtil.calendarDaysInYear(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case monthsInYear:
                    return TemporalUtil.calendarMonthsInYear(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
                case inLeapYear:
                    return TemporalUtil.calendarInLeapYear(calendarGetterNode, temporalDT.getCalendar(), temporalDT);
            }
            CompilerDirectives.transferToInterpreter();
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalDate(thisObj)")
        protected static int error(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalDateExpected();
        }
    }

    /**
     * Common base class for ALL Temporal Builtin operations.
     *
     */
    public abstract static class JSTemporalBuiltinOperation extends JSBuiltinNode {
        protected final BranchProfile errorBranch = BranchProfile.create();
        protected final ConditionProfile optionUndefined = ConditionProfile.createBinaryProfile();
        @Child protected IsObjectNode isObjectNode = IsObjectNode.create();
        @Child private TemporalGetOptionNode getOptionNode;

        public JSTemporalBuiltinOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected JSTemporalPlainDateObject requireTemporalDate(Object obj) {
            if (!(obj instanceof JSTemporalPlainDateObject)) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalDateExpected();
            }
            return (JSTemporalPlainDateObject) obj;
        }

        protected TemporalTime requireTemporalTime(Object obj) {
            if (!(obj instanceof TemporalTime)) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalTimeExpected();
            }
            return (TemporalTime) obj;
        }

        protected JSTemporalPlainDateTimeObject requireTemporalDateTime(Object obj) {
            if (!(obj instanceof JSTemporalPlainDateTimeObject)) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalDateTimeExpected();
            }
            return (JSTemporalPlainDateTimeObject) obj;
        }

        protected JSTemporalPlainMonthDayObject requireTemporalMonthDay(Object obj) {
            if (!(obj instanceof JSTemporalPlainMonthDayObject)) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
            }
            return (JSTemporalPlainMonthDayObject) obj;
        }

        protected JSTemporalPlainYearMonthObject requireTemporalYearMonth(Object obj) {
            if (!(obj instanceof JSTemporalPlainYearMonthObject)) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
            }
            return (JSTemporalPlainYearMonthObject) obj;
        }

        protected JSTemporalInstantObject requireTemporalInstant(Object obj) {
            if (!(obj instanceof JSTemporalInstantObject)) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalInstantExpected();
            }
            return (JSTemporalInstantObject) obj;
        }

        protected JSTemporalZonedDateTimeObject requireTemporalZonedDateTime(Object obj) {
            if (!(obj instanceof JSTemporalZonedDateTimeObject)) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
            }
            return (JSTemporalZonedDateTimeObject) obj;
        }

        protected JSTemporalCalendarObject requireTemporalCalendar(Object obj) {
            if (!(obj instanceof JSTemporalCalendarObject)) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalCalendarExpected();
            }
            return (JSTemporalCalendarObject) obj;
        }

        protected JSTemporalDurationObject requireTemporalDuration(Object obj) {
            if (!(obj instanceof JSTemporalDurationObject)) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalDurationExpected();
            }
            return (JSTemporalDurationObject) obj;
        }

        protected JSTemporalTimeZoneObject requireTemporalTimeZone(Object obj) {
            if (!(obj instanceof JSTemporalTimeZoneObject)) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorTemporalTimeZoneExpected();
            }
            return (JSTemporalTimeZoneObject) obj;
        }

        protected JSDynamicObject getOptionsObject(Object options) {
            if (optionUndefined.profile(options == Undefined.instance)) {
                return JSOrdinary.createWithNullPrototype(getContext());
            }
            if (isObject(options)) {
                return TemporalUtil.toJSDynamicObject(options, errorBranch);
            }
            errorBranch.enter();
            throw TemporalErrors.createTypeErrorOptions();
        }

        protected boolean isObject(Object obj) {
            return isObjectNode.executeBoolean(obj);
        }

        protected Unit toLargestTemporalUnit(JSDynamicObject normalizedOptions, List<TruffleString> disallowedUnits, TruffleString fallback, Unit autoValue, TruffleString.EqualNode equalNode) {
            assert fallback == null || (!disallowedUnits.contains(fallback) && !disallowedUnits.contains(AUTO));
            TruffleString largestUnit = (TruffleString) getOption(normalizedOptions, LARGEST_UNIT, TemporalUtil.OptionType.STRING, TemporalUtil.listAllDateTimeAuto, fallback);
            if (largestUnit != null && largestUnit.equals(AUTO) && autoValue != null) {
                return autoValue;
            }
            if (largestUnit != null && Boundaries.setContains(TemporalUtil.pluralUnits, largestUnit)) {
                largestUnit = Boundaries.mapGet(TemporalUtil.pluralToSingular, largestUnit);
            }
            if (largestUnit != null && Boundaries.listContains(disallowedUnits, largestUnit)) {
                errorBranch.enter();
                throw Errors.createRangeError("Largest unit is not allowed.");
            }
            return TemporalUtil.toUnit(largestUnit, equalNode);
        }

        protected Unit toSmallestTemporalUnit(JSDynamicObject normalizedOptions, List<TruffleString> disallowedUnits, TruffleString fallback, TruffleString.EqualNode equalNode) {
            TruffleString smallestUnit = (TruffleString) getOption(normalizedOptions, SMALLEST_UNIT, TemporalUtil.OptionType.STRING, TemporalUtil.listAllDateTime, fallback);
            if (smallestUnit != null && Boundaries.setContains(TemporalUtil.pluralUnits, smallestUnit)) {
                smallestUnit = Boundaries.mapGet(TemporalUtil.pluralToSingular, smallestUnit);
            }
            if (smallestUnit != null && Boundaries.listContains(disallowedUnits, smallestUnit)) {
                errorBranch.enter();
                throw Errors.createRangeError("Smallest unit not allowed.");
            }
            return TemporalUtil.toUnit(smallestUnit, equalNode);
        }

        protected Object getOption(JSDynamicObject option, TruffleString properties, OptionType type, List<TruffleString> values, TruffleString fallback) {
            return getOptionNode().execute(option, properties, type, values, fallback);
        }

        protected Unit toTemporalDurationTotalUnit(JSDynamicObject normalizedOptions, TruffleString.EqualNode equalNode) {
            TruffleString unit = (TruffleString) getOption(normalizedOptions, UNIT, TemporalUtil.OptionType.STRING, TemporalUtil.listAllDateTime, null);
            if (unit != null && Boundaries.setContains(TemporalUtil.pluralUnits, unit)) {
                unit = Boundaries.mapGet(TemporalUtil.pluralToSingular, unit);
            }
            return TemporalUtil.toUnit(unit, equalNode);
        }

        protected RoundingMode toTemporalRoundingMode(JSDynamicObject options, TruffleString fallback, TruffleString.EqualNode equalNode) {
            return TemporalUtil.toRoundingMode((TruffleString) getOption(options, ROUNDING_MODE, TemporalUtil.OptionType.STRING, TemporalUtil.listRoundingMode, fallback), equalNode);
        }

        protected TemporalGetOptionNode getOptionNode() {
            if (getOptionNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getOptionNode = insert(TemporalGetOptionNode.create());
            }
            return getOptionNode;
        }
    }

    // 4.3.10
    public abstract static class JSTemporalPlainDateAdd extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject add(Object thisObj, Object temporalDurationLike, Object optParam,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached("create(getContext())") ToLimitedTemporalDurationNode toLimitedTemporalDurationNode) {
            JSTemporalPlainDateObject date = requireTemporalDate(thisObj);
            JSTemporalDurationRecord duration = toLimitedTemporalDurationNode.executeDynamicObject(temporalDurationLike, TemporalUtil.listEmpty);
            JSDynamicObject options = getOptionsObject(optParam);
            JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDuration(getContext(), namesNode,
                            duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), Unit.DAY);
            JSDynamicObject balancedDuration = JSTemporalDuration.createTemporalDuration(getContext(), duration.getYears(), duration.getMonths(), duration.getWeeks(),
                            balanceResult.getDays(), 0, 0, 0, 0, 0, 0, errorBranch);
            return TemporalUtil.calendarDateAdd(date.getCalendar(), date, balancedDuration, options, Undefined.instance);
        }
    }

    public abstract static class JSTemporalPlainDateSubtract extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject subtract(Object thisObj, Object temporalDurationLike, Object optParam,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached("create(getContext())") ToLimitedTemporalDurationNode toLimitedTemporalDurationNode) {
            JSTemporalPlainDateObject date = requireTemporalDate(thisObj);
            JSTemporalDurationRecord duration = toLimitedTemporalDurationNode.executeDynamicObject(temporalDurationLike, TemporalUtil.listEmpty);
            JSDynamicObject options = getOptionsObject(optParam);
            JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDuration(getContext(), namesNode,
                            duration.getDays(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), Unit.DAY);
            JSDynamicObject balancedDuration = JSTemporalDuration.createTemporalDuration(getContext(), -duration.getYears(), -duration.getMonths(), -duration.getWeeks(),
                            -balanceResult.getDays(), 0, 0, 0, 0, 0, 0, errorBranch);
            return TemporalUtil.calendarDateAdd(date.getCalendar(), date, balancedDuration, options);
        }
    }

    public abstract static class JSTemporalPlainDateWith extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject with(Object thisObj, Object temporalDateLike, Object optParam,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode nameNode,
                        @Cached("create(getContext())") TemporalCalendarFieldsNode calendarFieldsNode,
                        @Cached("create(getContext())") TemporalCalendarDateFromFieldsNode dateFromFieldsNode) {
            JSTemporalPlainDateObject temporalDate = requireTemporalDate(thisObj);
            if (!isObject(temporalDateLike)) {
                errorBranch.enter();
                throw Errors.createTypeError("Object expected");
            }
            JSDynamicObject temporalDateLikeObject = TemporalUtil.toJSDynamicObject(temporalDateLike, errorBranch);
            TemporalUtil.rejectTemporalCalendarType(temporalDateLikeObject, errorBranch);
            Object calendarProperty = JSObject.get(temporalDateLikeObject, CALENDAR);
            if (calendarProperty != Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorUnexpectedCalendar();
            }
            Object timeZoneProperty = JSObject.get(temporalDateLikeObject, TIME_ZONE);
            if (timeZoneProperty != Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorUnexpectedTimeZone();
            }
            JSDynamicObject calendar = temporalDate.getCalendar();
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listDMMCY);
            JSDynamicObject partialDate = TemporalUtil.preparePartialTemporalFields(getContext(), temporalDateLikeObject, fieldNames);
            JSDynamicObject options = getOptionsObject(optParam);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), temporalDate, fieldNames, TemporalUtil.listEmpty);
            fields = TemporalUtil.calendarMergeFields(getContext(), nameNode, errorBranch, calendar, fields, partialDate);
            fields = TemporalUtil.prepareTemporalFields(getContext(), fields, fieldNames, TemporalUtil.listEmpty);
            return dateFromFieldsNode.execute(calendar, fields, options);
        }
    }

    public abstract static class JSTemporalPlainDateWithCalendar extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateWithCalendar(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject withCalendar(Object thisObj, Object calendarParam,
                        @Cached("create(getContext())") ToTemporalCalendarNode toTemporalCalendar) {
            JSTemporalPlainDateObject td = requireTemporalDate(thisObj);
            JSDynamicObject calendar = toTemporalCalendar.executeDynamicObject(calendarParam);
            return JSTemporalPlainDate.create(getContext(), td.getYear(), td.getMonth(), td.getDay(), calendar, errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateSince extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateSince(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject since(Object thisObj, Object otherObj, Object optionsParam,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached("create(getContext())") ToTemporalDateNode toTemporalDate,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext())") TemporalRoundDurationNode roundDurationNode) {
            JSTemporalPlainDateObject temporalDate = requireTemporalDate(thisObj);
            JSTemporalPlainDateObject other = toTemporalDate.executeDynamicObject(otherObj, Undefined.instance);
            if (!TemporalUtil.calendarEquals(temporalDate.getCalendar(), other.getCalendar(), toStringNode)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }

            JSDynamicObject options = getOptionsObject(optionsParam);
            List<TruffleString> disallowedUnits = TemporalUtil.listTime;
            Unit smallestUnit = toSmallestTemporalUnit(options, disallowedUnits, DAY, equalNode);
            Unit defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(Unit.DAY, smallestUnit);
            Unit largestUnit = toLargestTemporalUnit(options, disallowedUnits, AUTO, defaultLargestUnit, equalNode);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode);
            roundingMode = TemporalUtil.negateTemporalRoundingMode(roundingMode);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, null, false, isObjectNode, toNumber);
            JSDynamicObject untilOptions = TemporalUtil.mergeLargestUnitOption(getContext(), namesNode, options, largestUnit);
            JSTemporalDurationObject result = TemporalUtil.calendarDateUntil(temporalDate.getCalendar(), temporalDate, other, untilOptions, Undefined.instance);

            if (smallestUnit == Unit.DAY && (roundingIncrement == 1)) {
                return JSTemporalDuration.createTemporalDuration(getContext(), -result.getYears(), -result.getMonths(), -result.getWeeks(), -result.getDays(), 0, 0, 0, 0, 0, 0, errorBranch);
            }
            JSTemporalDurationRecord result2 = roundDurationNode.execute(result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), 0, 0, 0, 0,
                            0, 0, (long) roundingIncrement, smallestUnit, roundingMode, temporalDate);

            return JSTemporalDuration.createTemporalDuration(getContext(), -result2.getYears(), -result2.getMonths(), -result2.getWeeks(), -result2.getDays(), 0, 0, 0, 0, 0, 0, errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateUntil extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateUntil(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject until(Object thisObj, Object otherObj, Object optionsParam,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached("create(getContext())") ToTemporalDateNode toTemporalDate,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext())") TemporalRoundDurationNode roundDurationNode) {
            JSTemporalPlainDateObject temporalDate = requireTemporalDate(thisObj);
            JSTemporalPlainDateObject other = toTemporalDate.executeDynamicObject(otherObj, Undefined.instance);
            if (!TemporalUtil.calendarEquals(temporalDate.getCalendar(), other.getCalendar(), toStringNode)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorIdenticalCalendarExpected();
            }
            JSDynamicObject options = getOptionsObject(optionsParam);
            List<TruffleString> disallowedUnits = TemporalUtil.listTime;
            Unit smallestUnit = toSmallestTemporalUnit(options, disallowedUnits, DAY, equalNode);
            Unit defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(Unit.DAY, smallestUnit);
            Unit largestUnit = toLargestTemporalUnit(options, disallowedUnits, AUTO, defaultLargestUnit, equalNode);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, null, false, isObjectNode, toNumber);
            JSDynamicObject untilOptions = TemporalUtil.mergeLargestUnitOption(getContext(), namesNode, options, largestUnit);
            JSTemporalDurationObject result = TemporalUtil.calendarDateUntil(temporalDate.getCalendar(), temporalDate, other, untilOptions);

            if ((Unit.DAY != smallestUnit) || (roundingIncrement != 1)) {
                JSTemporalDurationRecord result2 = roundDurationNode.execute(result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), 0, 0, 0,
                                0, 0, 0, (long) roundingIncrement, smallestUnit, roundingMode, temporalDate);
                return JSTemporalDuration.createTemporalDuration(getContext(), result2.getYears(), result2.getMonths(), result2.getWeeks(), result2.getDays(), 0, 0, 0, 0, 0, 0, errorBranch);
            }
            return JSTemporalDuration.createTemporalDuration(getContext(), result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), 0, 0, 0, 0, 0, 0, errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateGetISOFields extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject getISOFields(Object thisObj) {
            JSTemporalPlainDateObject dt = requireTemporalDate(thisObj);
            JSObject obj = JSOrdinary.create(getContext(), getRealm());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, CALENDAR, dt.getCalendar());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_DAY, dt.getDay());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_MONTH, dt.getMonth());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, TemporalConstants.ISO_YEAR, dt.getYear());
            return obj;
        }
    }

    public abstract static class JSTemporalPlainDateToString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(Object thisObj, Object optionsParam,
                        @Cached TruffleString.EqualNode equalNode) {
            JSTemporalPlainDateObject date = requireTemporalDate(thisObj);
            JSDynamicObject options = getOptionsObject(optionsParam);
            ShowCalendar showCalendar = TemporalUtil.toShowCalendarOption(options, getOptionNode(), equalNode);
            return JSTemporalPlainDate.temporalDateToString(date, showCalendar);
        }
    }

    public abstract static class JSTemporalPlainDateToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public TruffleString toLocaleString(Object thisObj) {
            JSTemporalPlainDateObject date = requireTemporalDate(thisObj);
            return JSTemporalPlainDate.temporalDateToString(date, ShowCalendar.AUTO);
        }
    }

    public abstract static class JSTemporalPlainDateValueOf extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }

    public abstract static class JSTemporalPlainDateToPlainDateTime extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToPlainDateTime(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject toPlainDateTime(Object thisObj, Object temporalTimeObj,
                        @Cached("create(getContext())") ToTemporalTimeNode toTemporalTime) {
            JSTemporalPlainDateObject date = requireTemporalDate(thisObj);
            if (temporalTimeObj == Undefined.instance) {
                return JSTemporalPlainDateTime.create(getContext(), date.getYear(), date.getMonth(), date.getDay(), 0, 0, 0, 0, 0, 0, date.getCalendar(), errorBranch);
            }
            TemporalTime time = (TemporalTime) toTemporalTime.executeDynamicObject(temporalTimeObj, null);
            return JSTemporalPlainDateTime.create(getContext(), date.getYear(), date.getMonth(), date.getDay(),
                            time.getHour(), time.getMinute(), time.getSecond(), time.getMillisecond(), time.getMicrosecond(),
                            time.getNanosecond(), date.getCalendar(), errorBranch);
        }
    }

    public abstract static class JSTemporalPlainDateToPlainYearMonth extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToPlainYearMonth(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject toPlainYearMonth(Object thisObj,
                        @Cached("create(getContext())") TemporalYearMonthFromFieldsNode yearMonthFromFieldsNode,
                        @Cached("create(getContext())") TemporalCalendarFieldsNode calendarFieldsNode) {
            JSTemporalPlainDateObject date = requireTemporalDate(thisObj);
            JSDynamicObject calendar = date.getCalendar();
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listMCY);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), date, fieldNames, TemporalUtil.listEmpty);
            return yearMonthFromFieldsNode.execute(calendar, fields, Undefined.instance);
        }
    }

    public abstract static class JSTemporalPlainDateToPlainMonthDay extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToPlainMonthDay(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject toPlainMonthDay(Object thisObj,
                        @Cached("create(getContext())") TemporalMonthDayFromFieldsNode monthDayFromFieldsNode,
                        @Cached("create(getContext())") TemporalCalendarFieldsNode calendarFieldsNode) {
            JSTemporalPlainDateObject date = requireTemporalDate(thisObj);
            JSDynamicObject calendar = date.getCalendar();
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listDMC);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), date, fieldNames, TemporalUtil.listEmpty);
            return monthDayFromFieldsNode.execute(calendar, fields, Undefined.instance);
        }
    }

    public abstract static class JSTemporalPlainDateEquals extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public boolean equals(Object thisObj, Object otherParam,
                        @Cached("create(getContext())") ToTemporalDateNode toTemporalDate,
                        @Cached JSToStringNode toStringNode) {
            JSTemporalPlainDateObject temporalDate = requireTemporalDate(thisObj);
            JSTemporalPlainDateObject other = toTemporalDate.executeDynamicObject(otherParam, Undefined.instance);
            if (temporalDate.getYear() != other.getYear()) {
                return false;
            }
            if (temporalDate.getMonth() != other.getMonth()) {
                return false;
            }
            if (temporalDate.getDay() != other.getDay()) {
                return false;
            }
            return TemporalUtil.calendarEquals(temporalDate.getCalendar(), other.getCalendar(), toStringNode);
        }
    }

    public abstract static class JSTemporalPlainDateToZonedDateTimeNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainDateToZonedDateTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public JSDynamicObject toZonedDateTime(Object thisObj, Object item,
                        @Cached ConditionProfile timeZoneIsUndefined,
                        @Cached ConditionProfile timeIsUndefined,
                        @Cached("create(getContext())") ToTemporalTimeNode toTemporalTime,
                        @Cached("create(getContext())") ToTemporalTimeZoneNode toTemporalTimeZone) {
            JSTemporalPlainDateObject td = requireTemporalDate(thisObj);
            JSTemporalTimeZoneObject timeZone;
            Object temporalTime;
            JSTemporalPlainDateTimeObject temporalDateTime;
            if (isObject(item)) {
                JSDynamicObject itemObj = TemporalUtil.toJSDynamicObject(item, errorBranch);
                Object timeZoneLike = JSObject.get(itemObj, TIME_ZONE);
                if (timeZoneIsUndefined.profile(timeZoneLike == Undefined.instance)) {
                    timeZone = (JSTemporalTimeZoneObject) toTemporalTimeZone.executeDynamicObject(item);
                    temporalTime = Undefined.instance;
                } else {
                    timeZone = (JSTemporalTimeZoneObject) toTemporalTimeZone.executeDynamicObject(timeZoneLike);
                    temporalTime = JSObject.get(itemObj, TemporalConstants.PLAIN_TIME);
                }
            } else {
                timeZone = (JSTemporalTimeZoneObject) toTemporalTimeZone.executeDynamicObject(item);
                temporalTime = Undefined.instance;
            }
            if (timeIsUndefined.profile(temporalTime == Undefined.instance)) {
                temporalDateTime = JSTemporalPlainDateTime.create(getContext(), td.getYear(), td.getMonth(), td.getDay(), 0, 0, 0, 0, 0, 0,
                                td.getCalendar(), errorBranch);
            } else {
                JSTemporalPlainTimeObject tt = (JSTemporalPlainTimeObject) toTemporalTime.executeDynamicObject(temporalTime, null);
                temporalDateTime = JSTemporalPlainDateTime.create(getContext(), td.getYear(), td.getMonth(), td.getDay(),
                                tt.getHour(), tt.getMinute(), tt.getSecond(), tt.getMillisecond(), tt.getMicrosecond(),
                                tt.getNanosecond(), td.getCalendar(), errorBranch);
            }
            JSTemporalInstantObject instant = TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, temporalDateTime, Disambiguation.COMPATIBLE);
            return JSTemporalZonedDateTime.create(getContext(), getRealm(), instant.getNanoseconds(), timeZone, td.getCalendar());
        }
    }

}
