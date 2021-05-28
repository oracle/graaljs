/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainYearMonthPrototypeBuiltinsFactory.JSTemporalPlainYearMonthValueOfNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonth;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonthObject;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

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
        // with(2),
        // add(1),
        // subtract(1),
        // until(1),
        // since(1),
        // equals(1),
        toString(1),
        toLocaleString(0),
        toJSON(0),
        valueOf(0);
        // toPlainDate(1),
        // getISOFields(0);

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

// case with:
// return JSTemporalPlainYearMonthWithNodeGen.create(context, builtin,
// args().withThis().fixedArgs(2).createArgumentNodes(context));
// case equals:
// return JSTemporalPlainYearMonthEqualsNodeGen.create(context, builtin,
// args().withThis().fixedArgs(1).createArgumentNodes(context));
// case toPlainDate:
// return JSTemporalPlainYearMonthToPlainDateNodeGen.create(context, builtin,
// args().withThis().fixedArgs(1).createArgumentNodes(context));
// case getISOFields:
// return JSTemporalPlainYearMonthGetISOFieldsNodeGen.create(context, builtin,
// args().withThis().createArgumentNodes(context));
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
                    return JSTemporalCalendar.calendarYear(temporalYM.getCalendar(), temporalYM);
                case month:
                    return JSTemporalCalendar.calendarMonth(temporalYM.getCalendar(), temporalYM);
                case monthCode:
                    return JSTemporalCalendar.calendarMonthCode(temporalYM.getCalendar(), temporalYM);
                case daysInYear:
                    return JSTemporalCalendar.calendarDaysInYear(temporalYM.getCalendar(), temporalYM);
                case daysInMonth:
                    return JSTemporalCalendar.calendarDaysInMonth(temporalYM.getCalendar(), temporalYM);
                case monthsInYear:
                    return JSTemporalCalendar.calendarMonthsInYear(temporalYM.getCalendar(), temporalYM);
                case inLeapYear:
                    return JSTemporalCalendar.calendarInLeapYear(temporalYM.getCalendar(), temporalYM);

                // TODO more are missing
                // TODO according 3.3.4 this might be more complex
            }
            CompilerDirectives.transferToInterpreter();
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "isJSTemporalYearMonth(thisObj)")
        protected static int error(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
    }

    // 4.3.20
    public abstract static class JSTemporalPlainYearMonthToString extends JSBuiltinNode {

        protected JSTemporalPlainYearMonthToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(Object thisObj, DynamicObject optParam,
                        @Cached("create()") IsObjectNode isObject) {
            JSTemporalPlainYearMonthObject md = TemporalUtil.requireTemporalYearMonth(thisObj);
            DynamicObject options = TemporalUtil.getOptionsObject(optParam, getContext(), isObject);
            String showCalendar = TemporalUtil.toShowCalendarOption(options);
            return JSTemporalPlainYearMonth.temporalYearMonthToString(md, showCalendar);
        }
    }

    public abstract static class JSTemporalPlainYearMonthToLocaleString extends JSBuiltinNode {

        protected JSTemporalPlainYearMonthToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toLocaleString(DynamicObject thisObj) {
            JSTemporalPlainYearMonthObject time = TemporalUtil.requireTemporalYearMonth(thisObj);
            return JSTemporalPlainYearMonth.temporalYearMonthToString(time, "auto");
        }
    }

    public abstract static class JSTemporalPlainYearMonthValueOf extends JSBuiltinNode {

        protected JSTemporalPlainYearMonthValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") DynamicObject thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }
}
