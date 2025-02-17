/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetDateNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetDayNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetFullYearNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetHoursNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetMillisecondsNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetMinutesNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetMonthNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateGetSecondsNodeGen;
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
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToTemporalInstantNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateToTimeStringNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltinsFactory.JSDateValueOfNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSNumberToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.OrdinaryToPrimitiveNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.intl.InitializeDateTimeFormatNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSDateObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSDateTimeFormatObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Contains builtins for {@linkplain JSDate}.prototype.
 */
public final class DatePrototypeBuiltins {

    public static final JSBuiltinsContainer BUILTINS = JSBuiltinsContainer.fromEnum(JSDate.PROTOTYPE_NAME, DatePrototype.class);

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

        // Temporal
        toTemporalInstant(0),

        // Annex B
        getYear(0),
        setYear(1);

        private final int length;
        private final boolean isUTC;

        DatePrototype(int length) {
            this.length = length;
            this.isUTC = name().startsWith("UTC", 2) || name().startsWith("UTC", 3); // {to,get,set}UTC*
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
        public boolean isOptional() {
            return (this == toTemporalInstant);
        }

        @Override
        public int getECMAScriptVersion() {
            return switch (this) {
                case _toPrimitive -> 6;
                default -> BuiltinEnum.super.getECMAScriptVersion();
            };
        }

        @Override
        public Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
            switch (this) {
                case valueOf:
                case getTime:
                    return JSDateValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case toString:
                case toUTCString:
                    return JSDateToStringNodeGen.create(context, builtin, isUTC, args().withThis().createArgumentNodes(context));
                case toDateString:
                    return JSDateToDateStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case toTimeString:
                    return JSDateToTimeStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case toLocaleString:
                    return context.isOptionIntl402()
                                    ? JSDateToStringIntlNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context))
                                    : JSDateToStringNodeGen.create(context, builtin, isUTC, args().withThis().createArgumentNodes(context));
                case toLocaleDateString:
                    return context.isOptionIntl402()
                                    ? JSDateToLocaleDateStringIntlNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context))
                                    : JSDateToLocaleDateStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case toLocaleTimeString:
                    return context.isOptionIntl402()
                                    ? JSDateToLocaleTimeStringIntlNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context))
                                    : JSDateToLocaleTimeStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case toISOString:
                    return JSDateToISOStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case getFullYear:
                case getUTCFullYear:
                    return JSDateGetFullYearNodeGen.create(context, builtin, isUTC, args().withThis().createArgumentNodes(context));
                case getYear:
                    return JSDateGetYearNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case getMonth:
                case getUTCMonth:
                    return JSDateGetMonthNodeGen.create(context, builtin, isUTC, args().withThis().createArgumentNodes(context));
                case getDate:
                case getUTCDate:
                    return JSDateGetDateNodeGen.create(context, builtin, isUTC, args().withThis().createArgumentNodes(context));
                case getDay:
                case getUTCDay:
                    return JSDateGetDayNodeGen.create(context, builtin, isUTC, args().withThis().createArgumentNodes(context));
                case getHours:
                case getUTCHours:
                    return JSDateGetHoursNodeGen.create(context, builtin, isUTC, args().withThis().createArgumentNodes(context));
                case getMinutes:
                case getUTCMinutes:
                    return JSDateGetMinutesNodeGen.create(context, builtin, isUTC, args().withThis().createArgumentNodes(context));
                case getSeconds:
                case getUTCSeconds:
                    return JSDateGetSecondsNodeGen.create(context, builtin, isUTC, args().withThis().createArgumentNodes(context));
                case getMilliseconds:
                case getUTCMilliseconds:
                    return JSDateGetMillisecondsNodeGen.create(context, builtin, isUTC, args().withThis().createArgumentNodes(context));
                case setTime:
                    return JSDateSetTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case setDate:
                case setUTCDate:
                    return JSDateSetDateNodeGen.create(context, builtin, isUTC, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case setFullYear:
                case setUTCFullYear:
                    return JSDateSetFullYearNodeGen.create(context, builtin, isUTC, args().withThis().varArgs().createArgumentNodes(context));
                case setYear:
                    return JSDateSetYearNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case setMonth:
                case setUTCMonth:
                    return JSDateSetMonthNodeGen.create(context, builtin, isUTC, args().withThis().varArgs().createArgumentNodes(context));
                case setHours:
                case setUTCHours:
                    return JSDateSetHoursNodeGen.create(context, builtin, isUTC, args().withThis().varArgs().createArgumentNodes(context));
                case setMinutes:
                case setUTCMinutes:
                    return JSDateSetMinutesNodeGen.create(context, builtin, isUTC, args().withThis().varArgs().createArgumentNodes(context));
                case setSeconds:
                case setUTCSeconds:
                    return JSDateSetSecondsNodeGen.create(context, builtin, isUTC, args().withThis().varArgs().createArgumentNodes(context));
                case setMilliseconds:
                case setUTCMilliseconds:
                    return JSDateSetMillisecondsNodeGen.create(context, builtin, isUTC, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case getTimezoneOffset:
                    return JSDateGetTimezoneOffsetNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case toJSON:
                    return JSDateToJSONNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case _toPrimitive:
                    return JSDateToPrimitiveNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case toTemporalInstant:
                    return JSDateToTemporalInstantNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                default:
                    return null;
            }
        }
    }

    private static final boolean NO_UTC = false;

    public abstract static class JSDateOperation extends JSBuiltinNode {
        protected final boolean isUTC;

        public JSDateOperation(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin);
            this.isUTC = isUTC;
        }

        private final ConditionProfile isDate = ConditionProfile.create();
        protected final ConditionProfile isNaN = ConditionProfile.create();
        @Child private InteropLibrary interopLibrary;

        /**
         * Coerce to Date or throw TypeError. Must be the first statement (evaluation order!).
         */
        protected final JSDateObject asDate(Object object) {
            if (isDate.profile(JSDate.isJSDate(object))) {
                return (JSDateObject) object;
            } else {
                throw Errors.createTypeErrorNotADate();
            }
        }

        protected final double asDateMillis(Object thisDate) {
            if (isDate.profile(JSDate.isJSDate(thisDate))) {
                return ((JSDateObject) thisDate).getTimeMillis();
            }
            InteropLibrary interop = interopLibrary;
            if (interop == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                interop = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
                interopLibrary = interop;
            }
            if (interop.isInstant(thisDate)) {
                return JSDate.getDateValueFromInstant(thisDate, interop);
            } else {
                throw Errors.createTypeErrorNotADate();
            }
        }

        protected static void checkTimeValid(double time) {
            if (!JSDate.isTimeValid(time)) {
                throw Errors.createRangeError("time value is not a finite number");
            }
        }

        protected JSDateTimeFormatObject createDateTimeFormat(InitializeDateTimeFormatNode initDateTimeFormatNode, Object locales, Object options) {
            JSDateTimeFormatObject dateTimeFormatObj = JSDateTimeFormat.create(getContext(), getRealm());
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
            return asDateMillis(thisDate);
        }
    }

    public abstract static class JSDateToStringNode extends JSDateOperation {

        public JSDateToStringNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected TruffleString doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isUTC) {
                if (isNaN.profile(Double.isNaN(t))) {
                    return JSDate.INVALID_DATE_STRING;
                }
                return JSDate.format(getRealm().getJSDateUTCFormat(), t);
            } else {
                return JSDate.toString(t, getRealm());
            }
        }
    }

    public abstract static class JSDateToStringIntlNode extends JSDateOperation {

        @Child InitializeDateTimeFormatNode initDateTimeFormatNode;

        public JSDateToStringIntlNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
            this.initDateTimeFormatNode = InitializeDateTimeFormatNode.createInitalizeDateTimeFormatNode(context, InitializeDateTimeFormatNode.Required.ANY, InitializeDateTimeFormatNode.Defaults.ALL);
        }

        @Specialization
        protected TruffleString doOperation(Object thisDate, Object locales, Object options) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            JSDateTimeFormatObject formatter = createDateTimeFormat(initDateTimeFormatNode, locales, options);
            return JSDateTimeFormat.format(formatter, t);
        }
    }

    public abstract static class JSDateToDateStringNode extends JSDateOperation {

        public JSDateToDateStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
        }

        @Specialization
        protected TruffleString doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            return JSDate.format(getRealm().getJSShortDateFormat(), t);
        }
    }

    public abstract static class JSDateToTimeStringNode extends JSDateOperation {

        public JSDateToTimeStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
        }

        @Specialization
        protected TruffleString doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            return JSDate.format(getRealm().getJSShortTimeFormat(), t);
        }
    }

    public abstract static class JSDateToLocaleDateStringNode extends JSDateOperation {

        public JSDateToLocaleDateStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
        }

        @Specialization
        protected TruffleString doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            return JSDate.format(getRealm().getJSShortDateLocalFormat(), t);
        }
    }

    public abstract static class JSDateToLocaleDateStringIntlNode extends JSDateOperation {

        @Child InitializeDateTimeFormatNode initDateTimeFormatNode;

        public JSDateToLocaleDateStringIntlNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
            this.initDateTimeFormatNode = InitializeDateTimeFormatNode.createInitalizeDateTimeFormatNode(context, InitializeDateTimeFormatNode.Required.DATE,
                            InitializeDateTimeFormatNode.Defaults.DATE);
        }

        @Specialization
        protected TruffleString doOperation(Object thisDate, Object locales, Object options) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            JSDateTimeFormatObject formatter = createDateTimeFormat(initDateTimeFormatNode, locales, options);
            return JSDateTimeFormat.format(formatter, t);
        }
    }

    public abstract static class JSDateToLocaleTimeStringNode extends JSDateOperation {

        public JSDateToLocaleTimeStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
        }

        @Specialization
        protected TruffleString doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            return JSDate.format(getRealm().getJSShortTimeLocalFormat(), t);
        }
    }

    public abstract static class JSDateToLocaleTimeStringIntlNode extends JSDateOperation {

        @Child InitializeDateTimeFormatNode initDateTimeFormatNode;

        public JSDateToLocaleTimeStringIntlNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
            this.initDateTimeFormatNode = InitializeDateTimeFormatNode.createInitalizeDateTimeFormatNode(context, InitializeDateTimeFormatNode.Required.TIME,
                            InitializeDateTimeFormatNode.Defaults.TIME);
        }

        @Specialization
        protected TruffleString doOperation(Object thisDate, Object locales, Object options) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return JSDate.INVALID_DATE_STRING;
            }
            JSDateTimeFormatObject formatter = createDateTimeFormat(initDateTimeFormatNode, locales, options);
            return JSDateTimeFormat.format(formatter, t);
        }
    }

    public abstract static class JSDateToISOStringNode extends JSDateOperation {

        public JSDateToISOStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
        }

        @Specialization
        protected TruffleString doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            checkTimeValid(t);
            return JSDate.toISOStringIntl(t, getRealm());
        }
    }

    public abstract static class JSDateGetFullYearNode extends JSDateOperation {

        public JSDateGetFullYearNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            t = isUTC ? t : JSDate.localTime(t, this);
            return JSDate.yearFromTime((long) t);
        }
    }

    public abstract static class JSDateGetYearNode extends JSDateOperation {

        public JSDateGetYearNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            t = JSDate.localTime(t, this);
            return JSDate.yearFromTime((long) t) - 1900d;
        }
    }

    public abstract static class JSDateGetMonthNode extends JSDateOperation {

        public JSDateGetMonthNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            t = isUTC ? t : JSDate.localTime(t, this);
            return JSDate.monthFromTime(t);
        }
    }

    public abstract static class JSDateGetDateNode extends JSDateOperation {

        public JSDateGetDateNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            t = isUTC ? t : JSDate.localTime(t, this);
            return JSDate.dateFromTime(t);
        }
    }

    public abstract static class JSDateGetDayNode extends JSDateOperation {

        public JSDateGetDayNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double doOperation(Object thisDate) {
            double t = asDateMillis(thisDate);
            if (isNaN.profile(Double.isNaN(t))) {
                return Double.NaN;
            }
            t = isUTC ? t : JSDate.localTime(t, this);
            return JSDate.weekDay(t);
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
                t = JSDate.localTime(t, this);
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
                t = JSDate.localTime(t, this);
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
                t = JSDate.localTime(t, this);
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
            JSDateObject dateObject = asDate(thisDate);
            double t = dateObject.getTimeMillis();
            double dt = toDouble(date);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            double u = JSDate.setDate(t, dt, isUTC, this);
            dateObject.setTimeMillis(u);
            return u;
        }
    }

    public abstract static class JSDateSetYearNode extends JSDateOperationWithToNumberNode {

        public JSDateSetYearNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
        }

        @Specialization
        protected double setYear(Object thisDate, Object year) {
            JSDateObject dateObject = asDate(thisDate);
            double t = dateObject.getTimeMillis();
            double u = JSDate.setYear(t, toDouble(year), this);
            dateObject.setTimeMillis(u);
            return u;
        }
    }

    public abstract static class JSDateSetFullYearNode extends JSDateOperationWithToNumberNode {

        public JSDateSetFullYearNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double setFullYear(Object thisDate, Object[] args) {
            JSDateObject dateObject = asDate(thisDate);
            double t = dateObject.getTimeMillis();
            double iYear = toDouble(JSRuntime.getArgOrUndefined(args, 0));
            double iMonth = toDouble(JSRuntime.getArgOrUndefined(args, 1));
            double iDay = toDouble(JSRuntime.getArgOrUndefined(args, 2));
            double u = JSDate.setFullYear(t, iYear, iMonth, args.length >= 2, iDay, args.length >= 3, isUTC, this);
            dateObject.setTimeMillis(u);
            return u;
        }
    }

    public abstract static class JSDateSetMonthNode extends JSDateOperationWithToNumberNode {

        public JSDateSetMonthNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double setMonth(Object thisDate, Object[] args) {
            JSDateObject dateObject = asDate(thisDate);
            double t = dateObject.getTimeMillis();
            double month = toDouble(JSRuntime.getArgOrUndefined(args, 0));
            double date = toDouble(JSRuntime.getArgOrUndefined(args, 1));
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            double u = JSDate.setMonth(t, month, date, args.length >= 2, isUTC, this);
            dateObject.setTimeMillis(u);
            return u;
        }
    }

    public abstract static class JSDateSetHoursNode extends JSDateOperationWithToNumberNode {

        public JSDateSetHoursNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double setHours(Object thisDate, Object[] args) {
            JSDateObject dateObject = asDate(thisDate);
            double t = dateObject.getTimeMillis();
            double hour = toDouble(JSRuntime.getArgOrUndefined(args, 0));
            double min = toDouble(JSRuntime.getArgOrUndefined(args, 1));
            double sec = toDouble(JSRuntime.getArgOrUndefined(args, 2));
            double ms = toDouble(JSRuntime.getArgOrUndefined(args, 3));
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            double u = JSDate.setHours(t, hour, min, args.length >= 2, sec, args.length >= 3, ms, args.length >= 4, isUTC, this);
            dateObject.setTimeMillis(u);
            return u;
        }
    }

    public abstract static class JSDateSetMinutesNode extends JSDateOperationWithToNumberNode {

        public JSDateSetMinutesNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double doOperation(Object thisDate, Object[] args) {
            JSDateObject dateObject = asDate(thisDate);
            double t = dateObject.getTimeMillis();
            double min = toDouble(JSRuntime.getArgOrUndefined(args, 0));
            double sec = toDouble(JSRuntime.getArgOrUndefined(args, 1));
            double ms = toDouble(JSRuntime.getArgOrUndefined(args, 2));
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            double u = JSDate.setMinutes(t, min, sec, args.length >= 2, ms, args.length >= 3, isUTC, this);
            dateObject.setTimeMillis(u);
            return u;
        }
    }

    public abstract static class JSDateSetSecondsNode extends JSDateOperationWithToNumberNode {

        public JSDateSetSecondsNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double setSeconds(Object thisDate, Object[] args) {
            JSDateObject dateObject = asDate(thisDate);
            double t = dateObject.getTimeMillis();
            double sec = toDouble(JSRuntime.getArgOrUndefined(args, 0));
            double ms = toDouble(JSRuntime.getArgOrUndefined(args, 1));
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            double u = JSDate.setSeconds(t, sec, ms, args.length >= 2, isUTC, this);
            dateObject.setTimeMillis(u);
            return u;
        }
    }

    public abstract static class JSDateSetMillisecondsNode extends JSDateOperationWithToNumberNode {

        public JSDateSetMillisecondsNode(JSContext context, JSBuiltin builtin, boolean isUTC) {
            super(context, builtin, isUTC);
        }

        @Specialization
        protected double setMilliseconds(Object thisDate, Object msParam) {
            JSDateObject dateObject = asDate(thisDate);
            double t = dateObject.getTimeMillis();
            double ms = toDouble(msParam);
            if (Double.isNaN(t)) {
                return Double.NaN;
            }
            double u = JSDate.setMilliseconds(t, ms, isUTC, this);
            dateObject.setTimeMillis(u);
            return u;
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
            return (t - JSDate.localTime(t, this)) / JSDate.MS_PER_MINUTE;
        }
    }

    public abstract static class JSDateToJSONNode extends JSBuiltinNode {

        @Child private PropertyGetNode getToISOStringFnNode;
        @Child private JSFunctionCallNode callToISOStringFnNode;
        @Child private JSToObjectNode toObjectNode;
        @Child private JSToPrimitiveNode toPrimitiveNode;

        public JSDateToJSONNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toObjectNode = JSToObjectNode.create();
            toPrimitiveNode = JSToPrimitiveNode.createHintNumber();
        }

        @Specialization
        protected Object toJSON(Object thisDate, @SuppressWarnings("unused") Object key) {
            Object o = toObjectNode.execute(thisDate);
            Object tv = toPrimitiveNode.execute(o);
            if (JSRuntime.isNumber(tv)) {
                double d = JSRuntime.doubleValue(((Number) tv));
                if (Double.isInfinite(d) || Double.isNaN(d)) {
                    return Null.instance;
                }
            }
            Object toISO = getToISOStringFn(o);
            return getCallToISOStringFnNode().executeCall(JSArguments.create(o, toISO, JSArguments.EMPTY_ARGUMENTS_ARRAY));
        }

        private Object getToISOStringFn(Object obj) {
            if (getToISOStringFnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getToISOStringFnNode = insert(PropertyGetNode.create(Strings.TO_ISO_STRING, false, getContext()));
            }
            return getToISOStringFnNode.getValue(obj);
        }

        private JSFunctionCallNode getCallToISOStringFnNode() {
            if (callToISOStringFnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callToISOStringFnNode = insert(JSFunctionCallNode.createCall());
            }
            return callToISOStringFnNode;
        }
    }

    public abstract static class JSDateToPrimitiveNode extends JSBuiltinNode {

        public JSDateToPrimitiveNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final Object toPrimitive(Object obj, Object hint,
                        @Cached IsObjectNode isObjectNode,
                        @Cached DateToPrimitiveHelperNode dateToPrimitiveHelper,
                        @Cached InlinedBranchProfile errorBranch) {
            if (isObjectNode.executeBoolean(obj)) {
                return dateToPrimitiveHelper.execute(this, obj, hint);
            } else {
                errorBranch.enter(this);
                throw Errors.createTypeErrorNotAnObject(obj);
            }
        }

        @SuppressWarnings("truffle-inlining") // TruffleString nodes do not support object inlining.
        @GenerateInline
        @GenerateCached(false)
        @ImportStatic(Strings.class)
        abstract static class DateToPrimitiveHelperNode extends JavaScriptBaseNode {

            abstract Object execute(Node node, Object obj, Object hint);

            @SuppressWarnings("unused")
            @Specialization(guards = {"equals(strEqual, HINT_NUMBER, hint)"})
            static Object toPrimitiveHintNumber(Object obj, TruffleString hint,
                            @Cached @Shared TruffleString.EqualNode strEqual,
                            @Cached("createHintNumber()") OrdinaryToPrimitiveNode ordinaryToPrimitiveHintNumber) {
                return ordinaryToPrimitiveHintNumber.execute(obj);
            }

            @SuppressWarnings("unused")
            @Specialization(guards = {"equals(strEqual, HINT_STRING, hint) || equals(strEqual, HINT_DEFAULT, hint)"})
            static Object toPrimitiveHintStringOrDefault(Object obj, TruffleString hint,
                            @Cached @Shared TruffleString.EqualNode strEqual,
                            @Cached("createHintString()") OrdinaryToPrimitiveNode ordinaryToPrimitiveHintString) {
                return ordinaryToPrimitiveHintString.execute(obj);
            }

            @Fallback
            static Object invalidHint(@SuppressWarnings("unused") Object obj, Object hint) {
                assert !(Strings.HINT_STRING.equals(hint) || Strings.HINT_NUMBER.equals(hint) || Strings.HINT_DEFAULT.equals(hint)) : hint;
                throw Errors.createTypeError("invalid hint");
            }
        }
    }

    public abstract static class JSDateToTemporalInstantNode extends JSDateOperation {

        public JSDateToTemporalInstantNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, NO_UTC);
        }

        @Specialization
        protected final Object toTemporalInstant(Object thisObject,
                        @Cached JSNumberToBigIntNode numberToBigInt) {
            JSDateObject dateObject = asDate(thisObject);
            double t = dateObject.getTimeMillis();
            BigInt ns = numberToBigInt.executeBigInt(t).multiply(TemporalUtil.BI_NS_PER_MS);
            return JSTemporalInstant.create(getContext(), getRealm(), ns);
        }

    }

}
