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
package com.oracle.truffle.js.runtime.builtins;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_WEEK;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY_OF_WEEK;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY_OF_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.IN_LEAP_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ISO_DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ISO_MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ISO_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS_IN_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH_CODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEK_OF_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.getLong;

import java.util.HashSet;
import java.util.Set;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.TemporalPlainDateTimeFunctionBuiltins;
import com.oracle.truffle.js.builtins.TemporalPlainDateTimePrototypeBuiltins;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimePluralRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeRecord;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class JSTemporalPlainDateTime extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalPlainDateTime INSTANCE = new JSTemporalPlainDateTime();

    public static final String CLASS_NAME = "TemporalPlainDateTime";
    public static final String PROTOTYPE_NAME = "TemporalPlainDateTime.prototype";

    private JSTemporalPlainDateTime() {
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.PlainDateTime";
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    public static DynamicObject createTemporalDateTime(JSContext context, long y, long m, long d, long hour, long minute, long second, long millisecond, long microsecond, long nanosecond,
                    DynamicObject calendar) {
        if (!TemporalUtil.validateISODateTime(y, m, d, hour, minute, second, millisecond, microsecond, nanosecond)) {
            throw Errors.createRangeError("DateTime is not within range.");
        }
        if (!TemporalUtil.isoDateTimeWithinLimits(y, m, d, hour, minute, second, millisecond, microsecond, nanosecond)) {
            throw Errors.createRangeError("DateTime is not within range.");
        }
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalPlainDateTimeFactory();
        DynamicObject object = factory.initProto(new JSTemporalPlainDateTimeObject(factory.getShape(realm),
                        y, m, d, hour, minute, second, millisecond, microsecond, nanosecond, calendar), realm);
        return context.trackAllocation(object);
    }

    private static DynamicObject createGetterFunction(JSRealm realm, BuiltinFunctionKey functionKey, String property) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(functionKey, (c) -> {
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(c.getLanguage(), null, null) {
                private final BranchProfile errorBranch = BranchProfile.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object obj = frame.getArguments()[0];
                    if (JSTemporalPlainDateTime.isJSTemporalDateTime(obj)) {
                        JSTemporalPlainDateTimeObject temporalDT = (JSTemporalPlainDateTimeObject) obj;
                        switch (property) {
                            case HOUR:
                                return temporalDT.getHours();
                            case MINUTE:
                                return temporalDT.getMinutes();
                            case SECOND:
                                return temporalDT.getSeconds();
                            case MILLISECOND:
                                return temporalDT.getMilliseconds();
                            case MICROSECOND:
                                return temporalDT.getMicroseconds();
                            case NANOSECOND:
                                return temporalDT.getNanoseconds();

                            case YEAR:
                                return temporalDT.getYear();
                            case MONTH:
                                return temporalDT.getMonth();
                            case DAY:
                                return temporalDT.getDay();
                            case CALENDAR:
                                return temporalDT.getCalendar();
                            // TODO more are missing
                            // TODO according 3.3.4 this might be more complex
                            default:
                                errorBranch.enter();
                                throw Errors.createTypeErrorTemporalDateTimeExpected();
                        }
                    } else {
                        errorBranch.enter();
                        throw Errors.createTypeErrorTemporalDateTimeExpected();
                    }
                }
            });
            return JSFunctionData.createCallOnly(c, callTarget, 0, "get " + property);
        });
        DynamicObject getter = JSFunction.create(realm, getterData);
        return getter;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, constructor);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, HOUR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalTimeHour, HOUR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MINUTE,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalTimeMinute, MINUTE), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, SECOND,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalTimeSecond, SECOND), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MILLISECOND,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalTimeMillisecond, MILLISECOND), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MICROSECOND,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalTimeMicrosecond, MICROSECOND), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, NANOSECOND,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalTimeNanosecond, NANOSECOND), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, CALENDAR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateCalendar, CALENDAR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, YEAR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateYear, YEAR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTH,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateMonth, MONTH), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTH_CODE,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateMonthCode, MONTH_CODE), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateDay, DAY), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY_OF_WEEK,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateDayOfWeek, DAY_OF_WEEK), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY_OF_YEAR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateDayOfYear, DAY_OF_YEAR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, WEEK_OF_YEAR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateWeekOfYear, WEEK_OF_YEAR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_WEEK,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateDaysInWeek, DAYS_IN_WEEK), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_MONTH,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateDaysInMonth, DAYS_IN_MONTH), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_YEAR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateDaysInYear, DAYS_IN_YEAR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTHS_IN_YEAR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateMonthsInYear, MONTHS_IN_YEAR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, IN_LEAP_YEAR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalDateInLeapYear, IN_LEAP_YEAR), Undefined.instance);

        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalPlainDateTimePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, "Temporal.PlainDateTime");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainDateTime.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainTimePrototype();
    }

    @Override
    public void fillConstructor(JSRealm realm, DynamicObject constructor) {
        WithFunctionsAndSpecies.super.fillConstructor(realm, constructor);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalPlainDateTimeFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalDateTime(Object obj) {
        return obj instanceof JSTemporalPlainDateTimeObject;
    }

    // 4.5.9
    public static JSTemporalPlainTimeObject createTemporalTimeFromInstance(long hour, long minute, long second,
                    long millisecond, long microsecond,
                    long nanosecond, JSRealm realm,
                    JSFunctionCallNode callNode) {
        assert TemporalUtil.validateTime(hour, minute, second, millisecond, microsecond, nanosecond);
        DynamicObject constructor = realm.getTemporalPlainTimeConstructor();
        Object[] ctorArgs = new Object[]{hour, minute, second, millisecond, microsecond, nanosecond};
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, ctorArgs.length);
        System.arraycopy(ctorArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, ctorArgs.length);
        return (JSTemporalPlainTimeObject) callNode.executeCall(args);
    }

    // 4.5.10
    public static Object createTemporalTimeFromStatic(DynamicObject constructor, long hours, long minutes,
                    long seconds, long milliseconds, long microseconds,
                    long nanoseconds,
                    IsConstructorNode isConstructor,
                    JSFunctionCallNode callNode) {
        assert TemporalUtil.validateTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        if (!isConstructor.executeBoolean(constructor)) {
            throw Errors.createTypeError("Given constructor is not an constructor.");
        }
        Object[] ctorArgs = new Object[]{hours, minutes, seconds, milliseconds, microseconds, nanoseconds};
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, ctorArgs.length);
        System.arraycopy(ctorArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, ctorArgs.length);
        Object result = callNode.executeCall(args);
        return result;
    }

    public static DynamicObject toTemporalDateTime(Object item, DynamicObject optParam, JSContext ctx) {
        DynamicObject options = optParam;
        if (options == Undefined.instance) {
            options = JSObjectUtil.createOrdinaryPrototypeObject(ctx.getRealm(), Null.instance);
        }
        JSTemporalPlainDateTimeRecord result = null;
        if (JSRuntime.isObject(item)) {
            if (item instanceof JSTemporalPlainDateTimeObject) {
                return (DynamicObject) item;
            }
            // TODO
            // if (item instanceof JSTemporalZonedDateTimeObject) {
            // long instant = TemporalUtil.createTemporalInstant(item.getNanoseconds());
            // ii. Return ? BuiltinTimeZoneGetPlainDateTimeFor(item.[[TimeZone]], instant,
            // item.[[Calendar]]).
            // }
            if (item instanceof JSTemporalPlainDateObject) {
                JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) item;
                return createTemporalDateTime(ctx, date.getYear(), date.getMonth(), date.getDay(), 0, 0, 0, 0, 0, 0, date.getCalendar());
            }
            DynamicObject calendar = TemporalUtil.getOptionalTemporalCalendar((DynamicObject) item, ctx);
            Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{"day", "hour", "microsecond", "millisecond", "minute",
                            "month", "monthCode", "nanosecond", "second", "year"}, ctx);
            DynamicObject fields = TemporalUtil.prepareTemporalFields((DynamicObject) item, fieldNames, new HashSet<>(), ctx);
            result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, options, ctx);
        } else {
            TemporalUtil.toTemporalOverflow(options);
            String string = JSRuntime.toString(item);
            result = TemporalUtil.parseTemporalDateTimeString(string);
            if (!TemporalUtil.validateISODateTime(result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                            result.getMicrosecond(), result.getNanosecond())) {
                throw TemporalErrors.createRangeErrorInvalidPlainDateTime();
            }
        }
        DynamicObject calendar = TemporalUtil.toOptionalTemporalCalendar(result.getCalendar(), ctx);
        return createTemporalDateTime(ctx,
                        result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                        result.getMicrosecond(), result.getNanosecond(),
                        calendar);
    }

    public static JSTemporalPlainDateTimeRecord addDateTime(long year, long month, long day, long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond, DynamicObject calendar, long years, long months, long weeks, long days, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds, DynamicObject options, JSContext ctx) {
        JSTemporalPlainDateTimePluralRecord timeResult = JSTemporalPlainTime.addTime(hour, minute, second, millisecond, microsecond, nanosecond, hours, minutes, seconds, milliseconds, microseconds,
                        nanoseconds);
        DynamicObject datePart = JSTemporalPlainDate.createTemporalDate(ctx, year, month, day, calendar);
        DynamicObject dateDuration = JSTemporalDuration.createTemporalDuration(years, months, weeks, days + timeResult.getDays(), 0l, 0l, 0l, 0l, 0l, 0l, ctx);

        DynamicObject addedDate = TemporalUtil.dateAdd(calendar, datePart, dateDuration, options);

        return JSTemporalPlainDateTimeRecord.create(getLong(addedDate, ISO_YEAR), getLong(addedDate, ISO_MONTH), getLong(addedDate, ISO_DAY),
                        timeResult.getHours(), timeResult.getMinutes(), timeResult.getSeconds(),
                        timeResult.getMilliseconds(), timeResult.getMicroseconds(), timeResult.getNanoseconds());
    }

    public static int compareISODateTime(long year, long month, long day, long hours, long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds, long year2, long month2,
                    long day2, long hours2, long minutes2, long seconds2, long milliseconds2, long microseconds2, long nanoseconds2) {
        int date = JSTemporalPlainDate.compareISODate(year, month, day, year2, month2, day2);
        if (date == 0) {
            return JSTemporalPlainTime.compareTemporalTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds, hours2, minutes2, seconds2, milliseconds2, microseconds2, nanoseconds2);
        }
        return date;
    }

}
