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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.OVERFLOW;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.REJECT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;

import java.util.EnumSet;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayGetISOFieldsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayToPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayValueOfNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayWithNodeGen;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDay;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDayObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalPlainMonthDayPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainMonthDayPrototypeBuiltins.TemporalPlainMonthDayPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainMonthDayPrototypeBuiltins();

    protected TemporalPlainMonthDayPrototypeBuiltins() {
        super(JSTemporalPlainMonthDay.PROTOTYPE_NAME, TemporalPlainMonthDayPrototype.class);
    }

    public enum TemporalPlainMonthDayPrototype implements BuiltinEnum<TemporalPlainMonthDayPrototype> {
        // getters
        calendar(0),
        monthCode(0),
        day(0),

        // methods
        with(2),
        equals(1),
        toPlainDate(1),
        getISOFields(0),
        toString(1),
        toLocaleString(0),
        toJSON(0),
        valueOf(0);

        private final int length;

        TemporalPlainMonthDayPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(calendar, monthCode, day).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainMonthDayPrototype builtinEnum) {
        switch (builtinEnum) {
            case calendar:
            case monthCode:
            case day:
                return JSTemporalPlainMonthDayGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

            case with:
                return JSTemporalPlainMonthDayWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case equals:
                return JSTemporalPlainMonthDayEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainDate:
                return JSTemporalPlainMonthDayToPlainDateNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case getISOFields:
                return JSTemporalPlainMonthDayGetISOFieldsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toString:
                return JSTemporalPlainMonthDayToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalPlainMonthDayToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSTemporalPlainMonthDayValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainMonthDayGetterNode extends JSBuiltinNode {

        public final TemporalPlainMonthDayPrototype property;

        public JSTemporalPlainMonthDayGetterNode(JSContext context, JSBuiltin builtin, TemporalPlainMonthDayPrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization(guards = "isJSTemporalMonthDay(thisObj)")
        protected Object monthDayGetter(Object thisObj) {
            JSTemporalPlainMonthDayObject plainMD = (JSTemporalPlainMonthDayObject) thisObj;
            switch (property) {
                case day:
                    return JSTemporalCalendar.calendarDay(plainMD.getCalendar(), plainMD);
                case monthCode:
                    return JSTemporalCalendar.calendarMonthCode(plainMD.getCalendar(), plainMD);
                case calendar:
                    return plainMD.getCalendar();
            }
            CompilerDirectives.transferToInterpreter();
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalMonthDay(thisObj)")
        protected static int error(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
        }
    }

    // 4.3.20
    public abstract static class JSTemporalPlainMonthDayToString extends JSBuiltinNode {

        protected JSTemporalPlainMonthDayToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(Object thisObj, Object optParam,
                        @Cached("create()") IsObjectNode isObject) {
            JSTemporalPlainMonthDayObject md = TemporalUtil.requireTemporalMonthDay(thisObj);
            DynamicObject options = TemporalUtil.getOptionsObject(optParam, getContext(), isObject);
            String showCalendar = TemporalUtil.toShowCalendarOption(options);
            return JSTemporalPlainMonthDay.temporalMonthDayToString(md, showCalendar);
        }
    }

    public abstract static class JSTemporalPlainMonthDayToLocaleString extends JSBuiltinNode {

        protected JSTemporalPlainMonthDayToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toLocaleString(Object thisObj) {
            JSTemporalPlainMonthDayObject time = TemporalUtil.requireTemporalMonthDay(thisObj);
            return JSTemporalPlainMonthDay.temporalMonthDayToString(time, TemporalConstants.AUTO);
        }
    }

    public abstract static class JSTemporalPlainMonthDayValueOf extends JSBuiltinNode {

        protected JSTemporalPlainMonthDayValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") DynamicObject thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }

    public abstract static class JSTemporalPlainMonthDayToPlainDateNode extends JSBuiltinNode {

        protected JSTemporalPlainMonthDayToPlainDateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object toPlainDate(Object thisObj, Object item,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalPlainMonthDayObject monthDay = TemporalUtil.requireTemporalMonthDay(thisObj);

            DynamicObject calendar = monthDay.getCalendar();
            Set<String> receiverFieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.ARR_DMC);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), monthDay, receiverFieldNames, TemporalUtil.setEmpty);
            if (!JSRuntime.isObject(item)) {
                throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
            }
            Set<String> inputFieldNames = TemporalUtil.calendarFields(getContext(), calendar, new String[]{YEAR});
            DynamicObject inputFields = TemporalUtil.prepareTemporalFields(getContext(), (DynamicObject) item, inputFieldNames, TemporalUtil.setEmpty);
            Object mergedFields = TemporalUtil.calendarMergeFields(getContext(), namesNode, calendar, fields, inputFields);
            Set<String> mergedFieldNames = TemporalUtil.listJoinRemoveDuplicates(receiverFieldNames, inputFieldNames);
            mergedFields = TemporalUtil.prepareTemporalFields(getContext(), (DynamicObject) mergedFields, mergedFieldNames, TemporalUtil.setEmpty);
            DynamicObject options = JSOrdinary.createWithNullPrototype(getContext());
            TemporalUtil.createDataPropertyOrThrow(getContext(), options, OVERFLOW, REJECT);
            return TemporalUtil.dateFromFields(calendar, (DynamicObject) mergedFields, options);
        }
    }

    public abstract static class JSTemporalPlainMonthDayGetISOFields extends JSBuiltinNode {

        protected JSTemporalPlainMonthDayGetISOFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject getISOFields(Object thisObj) {
            JSTemporalPlainMonthDayObject md = TemporalUtil.requireTemporalMonthDay(thisObj);
            DynamicObject obj = JSOrdinary.create(getContext());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, CALENDAR, md.getCalendar());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoDay", md.getISODay());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoMonth", md.getISOMonth());
            TemporalUtil.createDataPropertyOrThrow(getContext(), obj, "isoYear", md.getISOYear());
            return obj;
        }
    }

    public abstract static class JSTemporalPlainMonthDayWithNode extends JSBuiltinNode {

        protected JSTemporalPlainMonthDayWithNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject with(Object thisObj, Object temporalMonthDayLike, Object optParam,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalPlainMonthDayObject md = TemporalUtil.requireTemporalMonthDay(thisObj);
            if (!JSRuntime.isObject(temporalMonthDayLike)) {
                throw Errors.createTypeError("Object expected");
            }
            DynamicObject mdLikeObj = (DynamicObject) temporalMonthDayLike;
            TemporalUtil.rejectTemporalCalendarType(mdLikeObj);
            Object calendarProperty = JSObject.get(mdLikeObj, CALENDAR);
            if (calendarProperty != Undefined.instance) {
                throw TemporalErrors.createTypeErrorUnexpectedCalendar();
            }
            Object timezoneProperty = JSObject.get(mdLikeObj, TIME_ZONE);
            if (timezoneProperty != Undefined.instance) {
                throw TemporalErrors.createTypeErrorUnexpectedTimeZone();
            }
            DynamicObject calendar = md.getCalendar();
            Set<String> fieldNames = TemporalUtil.calendarFields(getContext(), calendar, TemporalUtil.ARR_DMMCY);
            DynamicObject partialMonthDay = TemporalUtil.preparePartialTemporalFields(getContext(), mdLikeObj, fieldNames);
            DynamicObject options = TemporalUtil.getOptionsObject(getContext(), optParam);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), md, fieldNames, TemporalUtil.setEmpty);
            fields = (DynamicObject) TemporalUtil.calendarMergeFields(getContext(), namesNode, calendar, fields, partialMonthDay);
            fields = TemporalUtil.prepareTemporalFields(getContext(), fields, fieldNames, TemporalUtil.setEmpty);
            return TemporalUtil.monthDayFromFields(calendar, fields, options);
        }
    }

    public abstract static class JSTemporalPlainMonthDayEqualsNode extends JSBuiltinNode {

        protected JSTemporalPlainMonthDayEqualsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean equals(Object thisObj, Object otherParam) {
            JSTemporalPlainMonthDayObject md = TemporalUtil.requireTemporalMonthDay(thisObj);
            JSTemporalPlainMonthDayObject other = (JSTemporalPlainMonthDayObject) JSTemporalPlainMonthDay.toTemporalMonthDay(otherParam, Undefined.instance, getContext());
            if (md.getISOMonth() != other.getISOMonth()) {
                return false;
            }
            if (md.getISODay() != other.getISODay()) {
                return false;
            }
            if (md.getISOYear() != other.getISOYear()) {
                return false;
            }
            return TemporalUtil.calendarEquals(md.getCalendar(), other.getCalendar());
        }
    }
}
