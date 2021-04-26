/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights resetoTemporalDaterved.
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
package com.oracle.truffle.js.builtins;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ISO8601;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTES;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.REFERENCE_ISO_DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.REFERENCE_ISO_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.getLong;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDateAddNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDateFromFieldsNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDateUntilNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDayNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDayOfWeekNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDayOfYearNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDaysInMonthNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDaysInWeekNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDaysInYearNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarInLeapYearNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarMonthCodeNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarMonthDayFromFieldsNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarMonthNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarMonthsInYearNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarToStringNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarWeekOfYearNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarYearMonthFromFieldsNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarYearNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.JSTemporalCalendarObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainMonthDay;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainYearMonth;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainYearMonthObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalCalendarPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalCalendarPrototypeBuiltins.TemporalCalendarPrototype> {

    public static final TemporalCalendarPrototypeBuiltins INSTANCE = new TemporalCalendarPrototypeBuiltins();

    protected TemporalCalendarPrototypeBuiltins() {
        super(JSTemporalCalendar.PROTOTYPE_NAME, TemporalCalendarPrototype.class);
    }

    public enum TemporalCalendarPrototype implements BuiltinEnum<TemporalCalendarPrototype> {
        dateFromFields(3),
        yearMonthFromFields(3),
        monthDayFromFields(3),
        dateAdd(4),
        dateUntil(3),
        year(1),
        month(1),
        monthCode(1),
        day(1),
        dayOfWeek(1),
        dayOfYear(1),
        weekOfYear(1),
        daysInWeek(1),
        daysInMonth(1),
        daysInYear(1),
        monthsInYear(1),
        inLeapYear(1),
        toString(0),
        toJSON(0);

        private final int length;

        TemporalCalendarPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalCalendarPrototype builtinEnum) {
        switch (builtinEnum) {
            case dateFromFields:
                return JSTemporalCalendarDateFromFieldsNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case yearMonthFromFields:
                return JSTemporalCalendarYearMonthFromFieldsNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case monthDayFromFields:
                return JSTemporalCalendarMonthDayFromFieldsNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case dateAdd:
                return JSTemporalCalendarDateAddNodeGen.create(context, builtin, args().withThis().fixedArgs(4).createArgumentNodes(context));
            case dateUntil:
                return JSTemporalCalendarDateUntilNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case year:
                return JSTemporalCalendarYearNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case month:
                return JSTemporalCalendarMonthNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case monthCode:
                return JSTemporalCalendarMonthCodeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case day:
                return JSTemporalCalendarDayNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case dayOfWeek:
                return JSTemporalCalendarDayOfWeekNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case dayOfYear:
                return JSTemporalCalendarDayOfYearNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case daysInWeek:
                return JSTemporalCalendarDaysInWeekNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case weekOfYear:
                return JSTemporalCalendarWeekOfYearNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case daysInMonth:
                return JSTemporalCalendarDaysInMonthNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case daysInYear:
                return JSTemporalCalendarDaysInYearNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case monthsInYear:
                return JSTemporalCalendarMonthsInYearNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case inLeapYear:
                return JSTemporalCalendarInLeapYearNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toString:
            case toJSON:
                return JSTemporalCalendarToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    // 12.4.4
    public abstract static class JSTemporalCalendarDateFromFields extends JSBuiltinNode {

        protected JSTemporalCalendarDateFromFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object dateFromFields(DynamicObject thisObj, DynamicObject fields, DynamicObject optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("createSameValue()") JSIdenticalNode identicalNode,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSStringToNumberNode stringToNumber) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            if (!isObject.executeBoolean(fields)) {
                throw TemporalErrors.createTypeErrorFieldsNotAnObject();
            }
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext().getRealm(), isObject);
            DynamicObject result = JSTemporalCalendar.isoDateFromFields(fields, options, getContext(),
                            isObject, toBoolean, toString, stringToNumber, identicalNode);

            return JSTemporalPlainDate.createTemporalDate(getContext(), getLong(result, YEAR), getLong(result, MONTH), getLong(result, DAY), calendar);
        }
    }

    // 12.4.5
    public abstract static class JSTemporalCalendarYearMonthFromFields extends JSBuiltinNode {

        protected JSTemporalCalendarYearMonthFromFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object yearMonthFromFields(DynamicObject thisObj, DynamicObject fields, DynamicObject optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("createSameValue()") JSIdenticalNode identicalNode,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSStringToNumberNode stringToNumber) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            if (!isObject.executeBoolean(fields)) {
                throw TemporalErrors.createTypeErrorFieldsNotAnObject();
            }
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext().getRealm(), isObject);
            DynamicObject result = JSTemporalCalendar.isoYearMonthFromFields(fields, options, getContext(),
                            isObject, toBoolean, toString, stringToNumber, identicalNode);
            return JSTemporalPlainYearMonth.create(getContext(), getLong(result, YEAR), getLong(result, MONTH),
                            calendar, getLong(result, REFERENCE_ISO_DAY));
        }
    }

    // 12.4.6
    public abstract static class JSTemporalCalendarMonthDayFromFields extends JSBuiltinNode {

        protected JSTemporalCalendarMonthDayFromFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object monthDayFromFields(DynamicObject thisObj, DynamicObject fields, DynamicObject optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("createSameValue()") JSIdenticalNode identicalNode,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSStringToNumberNode stringToNumber) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            if (!isObject.executeBoolean(fields)) {
                throw TemporalErrors.createTypeErrorFieldsNotAnObject();
            }
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext().getRealm(), isObject);
            DynamicObject result = JSTemporalCalendar.isoMonthDayFromFields(fields, options, getContext(),
                            isObject, toBoolean, toString, stringToNumber, identicalNode);

            return JSTemporalPlainMonthDay.create(getContext(),
                            getLong(result, MONTH),
                            getLong(result, DAY),
                            calendar,
                            getLong(result, REFERENCE_ISO_YEAR));
        }
    }

    // 12.4.7
    public abstract static class JSTemporalCalendarDateAdd extends JSBuiltinNode {

        protected JSTemporalCalendarDateAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object dateAdd(DynamicObject thisObj, DynamicObject dateObj, DynamicObject durationObj, DynamicObject optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") IsConstructorNode isConstructor,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(dateObj,
                            null, getContext(), isObject, toBoolean, toString);
            JSTemporalDurationObject duration = (JSTemporalDurationObject) JSTemporalDuration.toTemporalDuration(
                            durationObj, null, getContext(), isObject, toInt, toString, isConstructor, callNode);
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext().getRealm(), isObject);
            String overflow = TemporalUtil.toTemporalOverflow(options, isObject, toBoolean, toString);
            DynamicObject result = JSTemporalPlainDate.addISODate(date.getYear(), date.getMonth(), date.getDay(),
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(), overflow,
                            getContext());

            return JSTemporalPlainDate.createTemporalDate(getContext(), getLong(result, YEAR, 0L), getLong(result, MONTH, 0L), getLong(result, DAY, 0L), calendar);
        }
    }

    // 12.4.8
    public abstract static class JSTemporalCalendarDateUntil extends JSBuiltinNode {

        protected JSTemporalCalendarDateUntil(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object dateUntil(DynamicObject thisObj, DynamicObject oneObj, DynamicObject twoObj, DynamicObject optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            JSTemporalPlainDateObject one = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(oneObj,
                            null, getContext(), isObject, toBoolean, toString);
            JSTemporalPlainDateObject two = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(twoObj,
                            null, getContext(), isObject, toBoolean, toString);
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext().getRealm(), isObject);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(options,
                            TemporalUtil.toSet(HOURS, MINUTES, SECONDS,
                                            MILLISECONDS, MICROSECONDS,
                                            NANOSECONDS),
                            DAYS, isObject, toBoolean, toString);
            DynamicObject result = JSTemporalPlainDate.differenceISODate(
                            one.getYear(), one.getMonth(), one.getDay(), two.getYear(), two.getMonth(), two.getDay(),
                            largestUnit, getContext());
            return JSTemporalDuration.createTemporalDuration(
                            getLong(result, YEARS),
                            getLong(result, MONTHS),
                            getLong(result, WEEKS),
                            getLong(result, DAYS),
                            0, 0, 0, 0, 0, 0,
                            getContext());
        }
    }

    // 12.4.9
    public abstract static class JSTemporalCalendarYear extends JSBuiltinNode {

        protected JSTemporalCalendarYear(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSOrdinaryObject(dateOrDateTime)")
        public long year(DynamicObject thisObj, DynamicObject dateOrDateTime,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            return JSTemporalCalendar.isoYear(
                            dateOrDateTime, getContext(), isObject, toBoolean, toString, toInt);
        }

        @Specialization(guards = "isJSTemporalPlainYearMonth(yearMonthObj)")
        public long year(DynamicObject thisObj, DynamicObject yearMonthObj) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            JSTemporalPlainYearMonthObject yearMonth = (JSTemporalPlainYearMonthObject) yearMonthObj;
            assert calendar.getId().equals(ISO8601);
            return yearMonth.getIsoYear();
        }
    }

    // 12.4.10
    public abstract static class JSTemporalCalendarMonth extends JSBuiltinNode {

        protected JSTemporalCalendarMonth(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSOrdinaryObject(dateOrDateTime)")
        public long month(DynamicObject thisObj, DynamicObject dateOrDateTime,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            // TODO: Check if dateOrDateTime is TemporalMonthDay
            assert calendar.getId().equals(ISO8601);
            return JSTemporalCalendar.isoMonth(
                            dateOrDateTime, getContext(), isObject, toBoolean, toString, toInt);
        }

        @Specialization(guards = "isJSTemporalPlainYearMonth(yearMonthObj)")
        public long month(DynamicObject thisObj, DynamicObject yearMonthObj) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            JSTemporalPlainYearMonthObject yearMonth = (JSTemporalPlainYearMonthObject) yearMonthObj;
            assert calendar.getId().equals(ISO8601);
            return yearMonth.getIsoMonth();
        }
    }

    // 12.4.11
    public abstract static class JSTemporalCalendarMonthCode extends JSBuiltinNode {

        protected JSTemporalCalendarMonthCode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSOrdinaryObject(dateOrDateTime)")
        public String monthCode(DynamicObject thisObj, DynamicObject dateOrDateTime,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            return JSTemporalCalendar.isoMonthCode(
                            dateOrDateTime, getContext(), isObject, toBoolean, toString, toInt);
        }

        @Specialization(guards = "isJSTemporalPlainYearMonth(yearMonthObj)")
        public String monthCode(DynamicObject thisObj, DynamicObject yearMonthObj) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            JSTemporalPlainYearMonthObject yearMonth = (JSTemporalPlainYearMonthObject) yearMonthObj;
            assert calendar.getId().equals(ISO8601);
            return JSTemporalCalendar.isoMonthCode(yearMonth);
        }
    }

    // 12.4.12
    public abstract static class JSTemporalCalendarDay extends JSBuiltinNode {

        protected JSTemporalCalendarDay(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public long day(DynamicObject thisObj, DynamicObject dateOrDateTime,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            return JSTemporalCalendar.isoDay(
                            dateOrDateTime, getContext(), isObject, toBoolean, toString, toInt);
        }
    }

    // 12.4.13
    public abstract static class JSTemporalCalendarDayOfWeek extends JSBuiltinNode {

        protected JSTemporalCalendarDayOfWeek(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public long dayOfWeek(DynamicObject thisObj, DynamicObject dateOrDateTime,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(
                            dateOrDateTime, null, getContext(), isObject, toBoolean,
                            toString);
            return JSTemporalCalendar.toISODayOfWeek(date.getYear(), date.getMonth(), date.getDay());
        }
    }

    // 12.4.14
    public abstract static class JSTemporalCalendarDayOfYear extends JSBuiltinNode {

        protected JSTemporalCalendarDayOfYear(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public long dayOfYear(DynamicObject thisObj, DynamicObject dateOrDateTime,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(
                            dateOrDateTime, null, getContext(), isObject, toBoolean,
                            toString);
            return JSTemporalCalendar.toISODayOfYear(date.getYear(), date.getMonth(), date.getDay());
        }
    }

    // 12.4.15
    public abstract static class JSTemporalCalendarWeekOfYear extends JSBuiltinNode {

        protected JSTemporalCalendarWeekOfYear(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public long weekOfYear(DynamicObject thisObj, DynamicObject dateOrDateTime,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(
                            dateOrDateTime, null, getContext(), isObject, toBoolean,
                            toString);
            return JSTemporalCalendar.toISOWeekOfYear(date.getYear(), date.getMonth(), date.getDay());
        }
    }

    // 12.4.16
    public abstract static class JSTemporalCalendarDaysInWeek extends JSBuiltinNode {

        protected JSTemporalCalendarDaysInWeek(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public long daysInWeek(DynamicObject thisObj, DynamicObject dateOrDateTime,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            JSTemporalPlainDate.toTemporalDate(dateOrDateTime, null, getContext(),
                            isObject, toBoolean, toString);
            return 7;
        }
    }

    // 12.4.17
    public abstract static class JSTemporalCalendarDaysInMonth extends JSBuiltinNode {

        protected JSTemporalCalendarDaysInMonth(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSOrdinaryObject(dateOrDateTime)")
        public long daysInMonth(DynamicObject thisObj, DynamicObject dateOrDateTime,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            if (!JSObject.hasProperty(dateOrDateTime, YEAR) &&
                            !JSObject.hasProperty(dateOrDateTime, MONTH)) {
                JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(
                                dateOrDateTime, null, getContext(), isObject, toBoolean,
                                toString);
                return JSTemporalCalendar.isoDaysInMonth(date.getYear(), date.getMonth());
            }
            return JSTemporalCalendar.isoDaysInMonth(
                            toInt.executeLong(getLong(dateOrDateTime, YEAR)),
                            toInt.executeLong(getLong(dateOrDateTime, MONTH)));
        }

        @Specialization(guards = "isJSTemporalPlainYearMonth(yearMonthObj)")
        public long daysInMonth(DynamicObject thisObj, DynamicObject yearMonthObj) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            JSTemporalPlainYearMonthObject yearMonth = (JSTemporalPlainYearMonthObject) yearMonthObj;
            assert calendar.getId().equals(ISO8601);
            return JSTemporalCalendar.isoDaysInMonth(yearMonth.getIsoYear(), yearMonth.getIsoMonth());
        }
    }

    // 12.4.18
    public abstract static class JSTemporalCalendarDaysInYear extends JSBuiltinNode {

        protected JSTemporalCalendarDaysInYear(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSOrdinaryObject(dateOrDateTime)")
        public long daysInYear(DynamicObject thisObj, DynamicObject dateOrDateTime,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            long year = JSTemporalCalendar.isoYear(dateOrDateTime, getContext(), isObject, toBoolean,
                            toString, toInt);
            return JSTemporalCalendar.isoDaysInYear(year);
        }

        @Specialization(guards = "isJSTemporalPlainYearMonth(yearMonthObj)")
        public long daysInYear(DynamicObject thisObj, DynamicObject yearMonthObj) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            JSTemporalPlainYearMonthObject yearMonth = (JSTemporalPlainYearMonthObject) yearMonthObj;
            assert calendar.getId().equals(ISO8601);
            long year = yearMonth.getIsoYear();
            return JSTemporalCalendar.isoDaysInYear(year);
        }
    }

    // 12.4.19
    public abstract static class JSTemporalCalendarMonthsInYear extends JSBuiltinNode {

        protected JSTemporalCalendarMonthsInYear(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSOrdinaryObject(dateOrDateTime)")
        public long monthsInYear(DynamicObject thisObj, DynamicObject dateOrDateTime,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            JSTemporalPlainDate.toTemporalDate(dateOrDateTime, null, getContext(),
                            isObject, toBoolean, toString);
            return 12;
        }

        @Specialization(guards = "!isJSOrdinaryObject(notAOrdinaryObject)")
        public long monthsInYear(DynamicObject thisObj, @SuppressWarnings("unused") DynamicObject notAOrdinaryObject) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            return 12;
        }
    }

    // 12.4.20
    public abstract static class JSTemporalCalendarInLeapYear extends JSBuiltinNode {

        protected JSTemporalCalendarInLeapYear(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSOrdinaryObject(dateOrDateTime)")
        public boolean inLeapYear(DynamicObject thisObj, DynamicObject dateOrDateTime,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals(ISO8601);
            long year = JSTemporalCalendar.isoYear(dateOrDateTime, getContext(), isObject, toBoolean, toString, toInt);
            return JSTemporalCalendar.isISOLeapYear(year);
        }

        @Specialization(guards = "isJSTemporalPlainYearMonth(yearMonthObj)")
        public boolean inLeapYear(DynamicObject thisObj, DynamicObject yearMonthObj) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            JSTemporalPlainYearMonthObject yearMonth = (JSTemporalPlainYearMonthObject) yearMonthObj;
            assert calendar.getId().equals(ISO8601);
            long year = yearMonth.getIsoYear();
            return JSTemporalCalendar.isISOLeapYear(year);
        }
    }

    // 12.4.23
    public abstract static class JSTemporalCalendarToString extends JSBuiltinNode {

        protected JSTemporalCalendarToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toString(DynamicObject thisObj) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            return calendar.getId();
        }
    }
}
