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
package com.oracle.truffle.js.builtins;

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetDateNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetDayNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetFullYearNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetHoursNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetMillisecondsNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetMinutesNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetMonthNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetSecondsNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetTimeNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetTimezoneOffsetNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetYearNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateSetDateNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateSetFullYearNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateSetHoursNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateSetMillisecondsNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateSetMinutesNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateSetMonthNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateSetSecondsNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateSetTimeNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateSetYearNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToDateStringNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToISOStringNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToJSONNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToLocaleDateStringIntlNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToLocaleDateStringNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToLocaleTimeStringIntlNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToLocaleTimeStringNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToPrimitiveNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToStringIntlNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToStringNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToTimeStringNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateValueOfNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltins.ObjectOperation;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.OrdinaryToPrimitiveNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.InitializeDateTimeFormatNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Contains builtins for {@linkplain JSDate}.prototype.
 */
public final class DatePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<DatePrototypeBuiltins.DatePrototype> {
    protected DatePrototypeBuiltins() {
        super(JSDate.PROTOTYPE_NAME, DatePrototype.class);
    }

    public enum DatePrototype implements BuiltinEnum<DatePrototype> {
        valueOf(0),
        toString(0),
        toDateString(0),
        toTimeString(0),
        toLocaleString(0),
        toLocaleDateString(0),
        toLocaleTimeString(0),
        toUTCString(0),
        toISOString(0),
        getTime(0),
        getFullYear(0),
        getUTCFullYear(0),
        getMonth(0),
        getUTCMonth(0),
        getDate(0),
        getUTCDate(0),
        getDay(0),
        getUTCDay(0),
        getHours(0),
        getUTCHours(0),
        getMinutes(0),
        getUTCMinutes(0),
        getSeconds(0),
        getUTCSeconds(0),
        getMilliseconds(0),
        getUTCMilliseconds(0),
        setTime(1),
        setDate(1),
        setUTCDate(1),
        setFullYear(3),
        setUTCFullYear(3),
        setMonth(2),
        setUTCMonth(2),
        setHours(4),
        setUTCHours(4),
        setMinutes(3),
        setUTCMinutes(3),
        setSeconds(2),
        setUTCSeconds(2),
        setMilliseconds(1),
        setUTCMilliseconds(1),
        getTimezoneOffset(0),
        toJSON(1),

        _toPrimitive(1) {
            @Override
            public Object getKey() {
                return Symbol.SYMBOL_TO_PRIMITIVE;
            }

            @Override
            public boolean isWritable() {
                return false;
            }
        },

        // Annex B
        getYear(0),
        setYear(1);

        private final int length;

        DatePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isAnnexB() {
            return EnumSet.of(getYear, setYear).contains(this);
        }

        @Override
        public int getECMAScriptVersion() {
            if (this == DatePrototype._toPrimitive) {
                return 6;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    private static final boolean UTC = true;
    private static final boolean NO_UTC = false;

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, DatePrototype builtinEnum) {
        switch (builtinEnum) {
            case valueOf:
                return JSDateValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toString:
                return JSDateToStringNodeGen.create(context, builtin, NO_UTC, args().withThis().createArgumentNodes(context));
            case toDateString:
                return JSDateToDateStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toTimeString:
                return JSDateToTimeStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toLocaleString:
                if (context.isOptionIntl402()) {
                    return JSDateToStringIntlNodeGen.create(context, builtin, NO_UTC, args().withThis().fixedArgs(2).createArgumentNodes(context));
                } else {
                    return JSDateToStringNodeGen.create(context, builtin, NO_UTC, args().withThis().createArgumentNodes(context));
                }
            case toLocaleDateString:
                if (context.isOptionIntl402()) {
                    return JSDateToLocaleDateStringIntlNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
                } else {
                    return JSDateToLocaleDateStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                }
            case toLocaleTimeString:
                if (context.isOptionIntl402()) {
                    return JSDateToLocaleTimeStringIntlNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
                } else {
                    return JSDateToLocaleTimeStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                }
            case toUTCString:
                return JSDateToStringNodeGen.create(context, builtin, UTC, args().withThis().createArgumentNodes(context));
            case toISOString:
                return JSDateToISOStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case getTime:
                return JSDateGetTimeNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case getFullYear:
                return JSDateGetFullYearNodeGen.create(context, builtin, NO_UTC, args().withThis().createArgumentNodes(context));
            case getYear:
                return JSDateGetYearNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case getUTCFullYear:
                return JSDateGetFullYearNodeGen.create(context, builtin, UTC, args().withThis().createArgumentNodes(context));
            case getMonth:
                return JSDateGetMonthNodeGen.create(context, builtin, NO_UTC, args().withThis().createArgumentNodes(context));
            case getUTCMonth:
                return JSDateGetMonthNodeGen.create(context, builtin, UTC, args().withThis().createArgumentNodes(context));
            case getDate:
                return JSDateGetDateNodeGen.create(context, builtin, NO_UTC, args().withThis().createArgumentNodes(context));
            case getUTCDate:
                return JSDateGetDateNodeGen.create(context, builtin, UTC, args().withThis().createArgumentNodes(context));
            case getDay:
                return JSDateGetDayNodeGen.create(context, builtin, NO_UTC, args().withThis().createArgumentNodes(context));
            case getUTCDay:
                return JSDateGetDayNodeGen.create(context, builtin, UTC, args().withThis().createArgumentNodes(context));
            case getHours:
                return JSDateGetHoursNodeGen.create(context, builtin, NO_UTC, args().withThis().createArgumentNodes(context));
            case getUTCHours:
                return JSDateGetHoursNodeGen.create(context, builtin, UTC, args().withThis().createArgumentNodes(context));
            case getMinutes:
                return JSDateGetMinutesNodeGen.create(context, builtin, NO_UTC, args().withThis().createArgumentNodes(context));
            case getUTCMinutes:
                return JSDateGetMinutesNodeGen.create(context, builtin, UTC, args().withThis().createArgumentNodes(context));
            case getSeconds:
                return JSDateGetSecondsNodeGen.create(context, builtin, NO_UTC, args().withThis().createArgumentNodes(context));
            case getUTCSeconds:
                return JSDateGetSecondsNodeGen.create(context, builtin, UTC, args().withThis().createArgumentNodes(context));
            case getMilliseconds:
                return JSDateGetMillisecondsNodeGen.create(context, builtin, NO_UTC, args().withThis().createArgumentNodes(context));
            case getUTCMilliseconds:
                return JSDateGetMillisecondsNodeGen.create(context, builtin, UTC, args().withThis().createArgumentNodes(context));
            case setTime:
                return JSDateSetTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case setDate:
                return JSDateSetDateNodeGen.create(context, builtin, NO_UTC, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case setUTCDate:
                return JSDateSetDateNodeGen.create(context, builtin, UTC, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case setYear:
                return JSDateSetYearNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case setFullYear:
                return JSDateSetFullYearNodeGen.create(context, builtin, NO_UTC, args().withThis().varArgs().createArgumentNodes(context));
            case setUTCFullYear:
                return JSDateSetFullYearNodeGen.create(context, builtin, UTC, args().withThis().varArgs().createArgumentNodes(context));
            case setMonth:
                return JSDateSetMonthNodeGen.create(context, builtin, NO_UTC, args().withThis().varArgs().createArgumentNodes(context));
            case setUTCMonth:
                return JSDateSetMonthNodeGen.create(context, builtin, UTC, args().withThis().varArgs().createArgumentNodes(context));
            case setHours:
                return JSDateSetHoursNodeGen.create(context, builtin, NO_UTC, args().withThis().varArgs().createArgumentNodes(context));
            case setUTCHours:
                return JSDateSetHoursNodeGen.create(context, builtin, UTC, args().withThis().varArgs().createArgumentNodes(context));
            case setMinutes:
                return JSDateSetMinutesNodeGen.create(context, builtin, NO_UTC, args().withThis().varArgs().createArgumentNodes(context));
            case setUTCMinutes:
                return JSDateSetMinutesNodeGen.create(context, builtin, UTC, args().withThis().varArgs().createArgumentNodes(context));
            case setSeconds:
                return JSDateSetSecondsNodeGen.create(context, builtin, NO_UTC, args().withThis().varArgs().createArgumentNodes(context));
            case setUTCSeconds:
                return JSDateSetSecondsNodeGen.create(context, builtin, UTC, args().withThis().varArgs().createArgumentNodes(context));
            case setMilliseconds:
                return JSDateSetMillisecondsNodeGen.create(context, builtin, NO_UTC, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case setUTCMilliseconds:
                return JSDateSetMillisecondsNodeGen.create(context, builtin, UTC, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case getTimezoneOffset:
                return JSDateGetTimezoneOffsetNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toJSON:
                return JSDateToJSONNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case _toPrimitive:
                return JSDateToPrimitiveNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSDateOperation extends JSBuiltinNode {
        protected final boolean isUTC;

        public JSDateOperation(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin);
            this.isUTC = isUTC;
        }

        private final ConditionProfile isDate = ConditionProfile.createBinaryProfile();
        protected final ConditionProfile isNaN = ConditionProfile.createBinaryProfile();

        /**
         * Coerce to Date or throw TypeError. Must be the first statement (evaluation order!).
         */
        protected final DynamicObject asDate(Object object) {
            if (isDate.profile(JSDate.isJSDate(object))) {
                return (DynamicObject) object;
            } else {
                throw Errors.createTypeError("not a Date object");
            }
        }

        protected final double asDateMillis(Object thisDate) {
            return JSDate.getTimeMillisField(asDate(thisDate));
        }

        protected static void checkTimeValid(double time) {
            if (!JSDate.isTimeValid(time)) {
                throw Errors.createRangeError("time value is not a finite number");
            }
        }

        protected DynamicObject createDateTimeFormat(InitializeDateTimeFormatNode initDateTimeFormatNode, Object locales, Object options) {
            DynamicObject dateTimeFormatObj = JSDateTimeFormat.create(getContext());
            initDateTimeFormatNode.executeInit(dateTimeFormatObj, locales, options);
            return dateTimeFormatObj;
        }
    }

    public abstract static class JSDateOperationWithToNumberNode extends JSDateOperation {
        public JSDateOperationWithToNumberNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Child protected JSToNumberNode toNumberNode = JSToNumberNode.create();

        protected double toDouble(Object target) {
            return JSRuntime.doubleValue(toNumberNode.executeNumber(target));
        }
    }

    public abstract static class JSDateValueOfNode extends JSDateOperation {

        public JSDateValueOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            return JSDate.getTimeMillisField(asDate(thisDate));
        }
    }

    public abstract static class JSDateToStringNode extends JSDateOperation {

        public JSDateToStringNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization(guards = "isJSObject(thisDate)")
        protected String doOperationWithDate(DynamicObject thisDate) {
            double t = asDateMillis(thisDate);
            if (isUTC) {
                if (isNaN.profile(Double.isNaN(t))) {
                    return JSDate.INVALID_DATE_STRING;
                }
                return JSDate.formatUTC(JSDate.getJSDateUTCFormat(), t);
            } else {
                return JSDate.toString(t, getContext());
            }
        }

        @Specialization(guards = {"!isForeignObject(thisDate)", "!isJSObject(thisDate)"})
        protected String doOperationDefault(Object thisDate) {
            asDateMillis(thisDate); // throws
            return JSDate.INVALID_DATE_STRING;
        }

        @Specialization(guards = "isForeignObject(thisDate)")
        protected String doOperationForeign(@SuppressWarnings("unused") TruffleObject thisDate) {
            return JSDate.INVALID_DATE_STRING;
        }
    }

    public abstract static class JSDateToStringIntlNode extends JSDateOperation {

        @Child InitializeDateTimeFormatNode initDateTimeFormatNode;

        public JSDateToStringIntlNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
            this.initDateTimeFormatNode = InitializeDateTimeFormatNode.createInitalizeDateTimeFormatNode(context, "any", "all");
        }

        @Specialization(guards = "isJSObject(thisDate)")
        protected String doOperationWithDate(DynamicObject thisDate, Object locales, Object options) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            DynamicObject formatter = createDateTimeFormat(initDateTimeFormatNode, locales, options);
            return JSDateTimeFormat.format(formatter, t);
        }

        @Specialization(guards = {"!isForeignObject(thisDate)", "!isJSObject(thisDate)"})
        @SuppressWarnings("unused")
        protected String doOperationDefault(Object thisDate, Object locales, Object options) {
            asDateMillis(thisDate); // throws
            return JSDate.INVALID_DATE_STRING;
        }

        @Specialization(guards = "isForeignObject(thisDate)")
        @SuppressWarnings("unused")
        protected String doOperationForeign(TruffleObject thisDate, Object locales, Object options) {
            return JSDate.INVALID_DATE_STRING;
        }
    }

    public abstract static class JSDateToDateStringNode extends JSDateOperation {

        public JSDateToDateStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
        }

        @Specialization
        protected String doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            return JSDate.formatLocal(JSDate.getJSShortDateFormat(), t, getContext());
        }
    }

    public abstract static class JSDateToTimeStringNode extends JSDateOperation {

        public JSDateToTimeStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
        }

        @Specialization
        protected String doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            return JSDate.formatLocal(JSDate.getJSShortTimeFormat(), t, getContext());
        }
    }

    public abstract static class JSDateToLocaleDateStringNode extends JSDateOperation {

        public JSDateToLocaleDateStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
        }

        @Specialization
        protected String doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            return JSDate.formatLocal(JSDate.getJSShortDateLocalFormat(), t, getContext());
        }
    }

    public abstract static class JSDateToLocaleDateStringIntlNode extends JSDateOperation {

        @Child InitializeDateTimeFormatNode initDateTimeFormatNode;

        public JSDateToLocaleDateStringIntlNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
            this.initDateTimeFormatNode = InitializeDateTimeFormatNode.createInitalizeDateTimeFormatNode(context, "date", "date");
        }

        @Specialization
        protected String doOperation(Object thisDate, Object locales, Object options) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            DynamicObject formatter = createDateTimeFormat(initDateTimeFormatNode, locales, options);
            return JSDateTimeFormat.format(formatter, t);
        }
    }

    public abstract static class JSDateToLocaleTimeStringNode extends JSDateOperation {

        public JSDateToLocaleTimeStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
        }

        @Specialization
        protected String doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            return JSDate.formatLocal(JSDate.getJSShortTimeLocalFormat(), t, getContext());
        }
    }

    public abstract static class JSDateToLocaleTimeStringIntlNode extends JSDateOperation {

        @Child InitializeDateTimeFormatNode initDateTimeFormatNode;

        public JSDateToLocaleTimeStringIntlNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
            this.initDateTimeFormatNode = InitializeDateTimeFormatNode.createInitalizeDateTimeFormatNode(context, "time", "time");
        }

        @Specialization
        protected String doOperation(Object thisDate, Object locales, Object options) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            DynamicObject formatter = createDateTimeFormat(initDateTimeFormatNode, locales, options);
            return JSDateTimeFormat.format(formatter, t);
        }
    }

    public abstract static class JSDateToISOStringNode extends JSDateOperation {

        public JSDateToISOStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
        }

        @Specialization
        protected String doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            checkTimeValid(t);
            return JSDate.toISOStringIntl(t);
        }
    }

    public abstract static class JSDateGetTimeNode extends JSDateOperation {

        public JSDateGetTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            return JSDate.getTimeMillisField(asDate(thisDate));
        }
    }

    public abstract static class JSDateGetFullYearNode extends JSDateOperation {
        @Child protected LocalDayNode localDayNode;

        public JSDateGetFullYearNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
            localDayNode = isUTC ? null : LocalDayNode.create(context);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            if (isUTC) {
                return JSDate.yearFromTime((long) t);
            } else {
                int daysAfter1970 = localDayNode.execute((long) t);
                return JSDate.yearFromDays(daysAfter1970);
            }
        }
    }

    public abstract static class JSDateGetYearNode extends JSDateOperation {
        @Child protected LocalDayNode localDayNode;

        public JSDateGetYearNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
            localDayNode = isUTC ? null : LocalDayNode.create(context);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            int daysAfter1970 = localDayNode.execute((long) t);
            return JSDate.yearFromDays(daysAfter1970) - 1900;
        }
    }

    public abstract static class JSDateGetMonthNode extends JSDateOperation {
        @Child protected LocalDayNode localDayNode;

        public JSDateGetMonthNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
            localDayNode = isUTC ? null : LocalDayNode.create(context);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            if (isUTC) {
                return JSDate.monthFromTime(t);
            } else {
                int daysAfter1970 = localDayNode.execute((long) t);
                return JSDate.monthFromDays(daysAfter1970);
            }
        }
    }

    public abstract static class JSDateGetDateNode extends JSDateOperation {
        private static final int DAYS_FROM_1970_TO_1901 = JSDate.dayFromYear(1901);
        private static final int DAYS_FROM_1970_TO_2100 = JSDate.dayFromYear(2100);
        @Child protected LocalDayNode localDayNode;

        public JSDateGetDateNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
            localDayNode = isUTC ? null : LocalDayNode.create(context);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            if (isUTC) {
                return JSDate.dateFromTime(t);
            } else {
                int daysAfter1970 = localDayNode.execute((long) t);
                if (DAYS_FROM_1970_TO_1901 <= daysAfter1970 && daysAfter1970 <= DAYS_FROM_1970_TO_2100) {
                    // There are regular leap years between 1.1.1901 and 1.1.2100
                    return JSDate.dateFromDaysRegularLeapYears(daysAfter1970);
                } else {
                    return JSDate.dateFromDays(daysAfter1970);
                }
            }
        }
    }

    public abstract static class JSDateGetDayNode extends JSDateOperation {
        @Child protected LocalDayNode localDayNode;

        public JSDateGetDayNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
            localDayNode = isUTC ? null : LocalDayNode.create(context);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            if (isUTC) {
                return JSDate.weekDay(t);
            } else {
                int daysAfter1970 = localDayNode.execute((long) t);
                int result = (daysAfter1970 + 4) % 7;
                return result >= 0 ? result : (result + 7);
            }

        }
    }

    public abstract static class JSDateGetHoursNode extends JSDateOperation {

        public JSDateGetHoursNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            if (!isUTC) {
                t = JSDate.localTime(t, getContext());
            }
            return JSDate.hourFromTime(t);
        }
    }

    public abstract static class JSDateGetMinutesNode extends JSDateOperation {

        public JSDateGetMinutesNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            if (!isUTC) {
                t = JSDate.localTime(t, getContext());
            }
            return JSDate.minFromTime(t);
        }
    }

    public abstract static class JSDateGetSecondsNode extends JSDateOperation {

        public JSDateGetSecondsNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            if (!isUTC) {
                t = JSDate.localTime(t, getContext());
            }
            return JSDate.secFromTime(t);
        }
    }

    public abstract static class JSDateGetMillisecondsNode extends JSDateOperation {

        public JSDateGetMillisecondsNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            // No need to convert to local time - DST offset and localTZA
            // are always in full seconds
            return JSDate.msFromTime(t);
        }
    }

    public abstract static class JSDateSetTimeNode extends JSDateOperationWithToNumberNode {

        public JSDateSetTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
        }

        @Specialization
        protected double doOperation(Object thisDate, Object time) {
            return JSDate.setTime(asDate(thisDate), toDouble(time));
        }
    }

    public abstract static class JSDateSetDateNode extends JSDateOperationWithToNumberNode {

        public JSDateSetDateNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double doOperation(Object thisDate, Object date) {
            return JSDate.setDate(asDate(thisDate), toDouble(date), isUTC, getContext());
        }
    }

    public abstract static class JSDateSetYearNode extends JSDateOperationWithToNumberNode {

        public JSDateSetYearNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
        }

        @Specialization
        protected double setYear(Object thisDate, Object year) {
            return JSDate.setYear(asDate(thisDate), toDouble(year), getContext());
        }
    }

    public abstract static class JSDateSetFullYearNode extends JSDateOperationWithToNumberNode {

        public JSDateSetFullYearNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double setFullYear(Object thisDate, Object[] args) {
            DynamicObject asDate = asDate(thisDate);
            double iYear = toDouble(JSRuntime.getArgOrUndefined(args, 0));
            double iMonth = toDouble(JSRuntime.getArgOrUndefined(args, 1));
            double iDay = toDouble(JSRuntime.getArgOrUndefined(args, 2));
            return JSDate.setFullYear(asDate, iYear, iMonth, args.length >= 2, iDay, args.length >= 3, isUTC, getContext());
        }
    }

    public abstract static class JSDateSetMonthNode extends JSDateOperationWithToNumberNode {

        public JSDateSetMonthNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double setMonth(Object thisDate, Object[] args) {
            DynamicObject date = asDate(thisDate);
            double month = toDouble(JSRuntime.getArgOrUndefined(args, 0));
            double date2 = toDouble(JSRuntime.getArgOrUndefined(args, 1));
            return JSDate.setMonth(date, month, date2, args.length >= 2, isUTC, getContext());
        }
    }

    public abstract static class JSDateSetHoursNode extends JSDateOperationWithToNumberNode {

        public JSDateSetHoursNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double setHours(Object thisDate, Object[] args) {
            DynamicObject date = asDate(thisDate);
            double hour = toDouble(JSRuntime.getArgOrUndefined(args, 0));
            double min = toDouble(JSRuntime.getArgOrUndefined(args, 1));
            double sec = toDouble(JSRuntime.getArgOrUndefined(args, 2));
            double ms = toDouble(JSRuntime.getArgOrUndefined(args, 3));
            return JSDate.setHours(date, hour, min, args.length >= 2, sec, args.length >= 3, ms, args.length >= 4, isUTC, getContext());
        }
    }

    public abstract static class JSDateSetMinutesNode extends JSDateOperationWithToNumberNode {

        public JSDateSetMinutesNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double doOperation(Object thisDate, Object[] args) {
            DynamicObject date = asDate(thisDate);
            double min = toDouble(JSRuntime.getArgOrUndefined(args, 0));
            double sec = toDouble(JSRuntime.getArgOrUndefined(args, 1));
            double ms = toDouble(JSRuntime.getArgOrUndefined(args, 2));
            return JSDate.setMinutes(date, min, sec, args.length >= 2, ms, args.length >= 3, isUTC, getContext());
        }
    }

    public abstract static class JSDateSetSecondsNode extends JSDateOperationWithToNumberNode {

        public JSDateSetSecondsNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double setSeconds(Object thisDate, Object[] args) {
            DynamicObject date = asDate(thisDate);
            double sec = toDouble(JSRuntime.getArgOrUndefined(args, 0));
            double ms = toDouble(JSRuntime.getArgOrUndefined(args, 1));
            return JSDate.setSeconds(date, sec, ms, args.length >= 2, isUTC, getContext());
        }
    }

    public abstract static class JSDateSetMillisecondsNode extends JSDateOperationWithToNumberNode {

        public JSDateSetMillisecondsNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double setMilliseconds(Object thisDate, Object ms) {
            return JSDate.setMilliseconds(asDate(thisDate), toDouble(ms), isUTC, getContext());
        }
    }

    public abstract static class JSDateGetTimezoneOffsetNode extends JSDateOperation {

        public JSDateGetTimezoneOffsetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
        }

        @Specialization
        protected double getTimezoneOffset(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            return (t - JSDate.localTime(t, getContext())) / JSDate.MS_PER_MINUTE;
        }
    }

    public abstract static class JSDateToJSONNode extends ObjectOperation {

        @Child private PropertyGetNode getToISOStringFnNode;
        @Child private JSToPrimitiveNode toPrimitiveNode;

        public JSDateToJSONNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toPrimitiveNode = JSToPrimitiveNode.createHintNumber();
        }

        @Specialization
        protected Object toJSON(Object thisDate, @SuppressWarnings("unused") Object key) {
            DynamicObject o = toObject(thisDate);
            Object tv = toPrimitiveNode.execute(o);
            if (JSRuntime.isNumber(tv)) {
                double d = JSRuntime.doubleValue(((Number) tv));
                if (Double.isInfinite(d) || Double.isNaN(d)) {
                    return Null.instance;
                }
            }
            Object toISO = getToISOStringFn(o);
            JSFunction.checkIsFunction(toISO);
            return JSFunction.call((DynamicObject) toISO, o, JSArguments.EMPTY_ARGUMENTS_ARRAY);
        }

        private Object getToISOStringFn(Object obj) {
            if (getToISOStringFnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getToISOStringFnNode = insert(PropertyGetNode.create("toISOString", false, getContext()));
            }
            return getToISOStringFnNode.getValue(obj);
        }
    }

    public abstract static class JSDateToPrimitiveNode extends JSBuiltinNode {

        private final ConditionProfile isHintNumber = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isHintStringOrDefault = ConditionProfile.createBinaryProfile();
        @Child private IsObjectNode isObjectNode;
        @Child private OrdinaryToPrimitiveNode ordinaryToPrimitiveHintNumber;
        @Child private OrdinaryToPrimitiveNode ordinaryToPrimitiveHintString;

        public JSDateToPrimitiveNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.isObjectNode = IsObjectNode.create();
        }

        @Specialization
        protected Object toPrimitive(Object obj, Object hint) {
            if (!isObjectNode.executeBoolean(obj)) {
                throw Errors.createTypeErrorNotAnObject(obj);
            }
            if (isHintNumber.profile(JSRuntime.HINT_NUMBER.equals(hint))) {
                if (ordinaryToPrimitiveHintNumber == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    ordinaryToPrimitiveHintNumber = insert(OrdinaryToPrimitiveNode.createHintNumber(getContext()));
                }
                return ordinaryToPrimitiveHintNumber.execute((DynamicObject) obj);
            } else if (isHintStringOrDefault.profile(JSRuntime.HINT_STRING.equals(hint) || JSRuntime.HINT_DEFAULT.equals(hint))) {
                if (ordinaryToPrimitiveHintString == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    ordinaryToPrimitiveHintString = insert(OrdinaryToPrimitiveNode.createHintString(getContext()));
                }
                return ordinaryToPrimitiveHintString.execute((DynamicObject) obj);
            } else {
                throw Errors.createTypeError("invalid hint");
            }
        }
    }

    public static final class LocalDayNode extends JavaScriptBaseNode {
        private final JSContext context;
        private final BranchProfile dstNeededProfile = BranchProfile.create();

        private LocalDayNode(JSContext context) {
            this.context = context;
        }

        public static LocalDayNode create(JSContext context) {
            return new LocalDayNode(context);
        }

        public int execute(long t) {
            long localNoDST = t + context.getLocalTZA();
            long day = Math.floorDiv(localNoDST, JSDate.MS_PER_DAY);
            assert JSRuntime.longIsRepresentableAsInt(day);
            int iday = (int) day;
            long timeInDay = localNoDST - JSDate.MS_PER_DAY * day;
            if (timeInDay < JSDate.MS_PER_DAY - JSDate.MS_MAX_DST) {
                // DST offset cannot change the day
                return iday;
            } else {
                dstNeededProfile.enter();
                timeInDay += JSDate.daylightSavingTA(context.getLocalTimeZoneId(), t);
                return (timeInDay < JSDate.MS_PER_DAY) ? iday : (iday + 1);
            }
        }

    }

}
