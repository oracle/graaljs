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

import static com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey.TemporalCalendarId;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH_CODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.TemporalCalendarFunctionBuiltins;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltins;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class JSTemporalCalendar extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalCalendar INSTANCE = new JSTemporalCalendar();

    public static final String CLASS_NAME = "TemporalCalendar";
    public static final String PROTOTYPE_NAME = "TemporalCalendar.prototype";

    public static final String ID = "id";

    private JSTemporalCalendar() {

    }

    public static DynamicObject create(JSContext context, String id) {
        if (!isBuiltinCalendar(id)) {
            throw Errors.createRangeError("Given calendar id not supported.");
        }

        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalCalendarFactory();
        DynamicObject obj = factory.initProto(new JSTemporalCalendarObject(factory.getShape(realm), id), realm);
        return context.trackAllocation(obj);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.Calendar";
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static DynamicObject getIdFunction(JSRealm realm) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(TemporalCalendarId, (c) -> {
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(c.getLanguage(), null, null) {
                private final BranchProfile errorBranch = BranchProfile.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object obj = frame.getArguments()[0];
                    if (JSTemporalCalendar.isJSTemporalCalendar(obj)) {
                        JSTemporalCalendarObject temporalCalendar = (JSTemporalCalendarObject) obj;
                        return temporalCalendar.getId();
                    } else {
                        errorBranch.enter();
                        throw Errors.createTypeErrorTemporalCalenderExpected();
                    }
                }
            });
            return JSFunctionData.createCallOnly(c, callTarget, 0, "get id");
        });
        DynamicObject getter = JSFunction.create(realm, getterData);
        return getter;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, constructor);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, ID, getIdFunction(realm), Undefined.instance);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalCalendarPrototypeBuiltins.INSTANCE);
        JSObjectUtil.putToStringTag(prototype, "Temporal.Calendar");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalCalendar.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalCalendarPrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalCalendarFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalCalendar(Object obj) {
        return obj instanceof JSTemporalCalendarObject;
    }

    // 12.1.1
    public static Object createTemporalCalendarFromStatic(DynamicObject constructor, String id,
                    IsConstructorNode isConstructor,
                    JSFunctionCallNode callNode) {
        assert isBuiltinCalendar(id);
        if (!isConstructor.executeBoolean(constructor)) {
            throw Errors.createTypeError("Given constructor is not an constructor.");
        }
        Object[] ctorArgs = new Object[]{id};
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, ctorArgs.length);
        System.arraycopy(ctorArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, ctorArgs.length);
        Object result = callNode.executeCall(args);
        return result;
    }

    // 12.1.2
    public static boolean isBuiltinCalendar(String id) {
        return id.equals("iso8601");
    }

    // 12.1.3
    public static JSTemporalCalendarObject getBuiltinCalendar(String id, DynamicObject constructor, IsConstructorNode isConstructorNode,
                    JSFunctionCallNode callNode) {
        if (!isBuiltinCalendar(id)) {
            throw Errors.createRangeError("Given calender identifier is not a builtin.");
        }
        return (JSTemporalCalendarObject) createTemporalCalendarFromStatic(constructor, id, isConstructorNode, callNode);
    }

    // 12.1.4
    public static JSTemporalCalendarObject getISO8601Calender(JSRealm realm, IsConstructorNode isConstructorNode,
                    JSFunctionCallNode callNode) {
        return getBuiltinCalendar("iso8601", realm.getTemporalCalendarConstructor(), isConstructorNode, callNode);
    }

    private static Object executeFunction(JSTemporalCalendarObject calendarObject, String functionName,
                    DynamicObjectLibrary dol, JSFunctionCallNode callNode, Object... funcArgs) {
        Object function = dol.getOrDefault(calendarObject.getPrototypeOf(), functionName, null);
        Object[] args = JSArguments.createInitial(calendarObject, function, funcArgs.length);
        System.arraycopy(funcArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, funcArgs.length);
        return callNode.executeCall(args);
    }

    // 12.1.9
    public static long calendarYear(JSTemporalCalendarObject calendar, DynamicObject dateLike, DynamicObjectLibrary dol,
                    JSFunctionCallNode callNode) {
        return (long) executeFunction(calendar, "year", dol, callNode, dateLike);
    }

    // 12.1.10
    public static long calendarMonth(JSTemporalCalendarObject calendar, DynamicObject dateLike, DynamicObjectLibrary dol,
                    JSFunctionCallNode callNode) {
        return (long) executeFunction(calendar, "month", dol, callNode, dateLike);
    }

    // 12.1.11
    public static String calendarMonthCode(JSTemporalCalendarObject calendar, DynamicObject dateLike, DynamicObjectLibrary dol,
                    JSFunctionCallNode callNode) {
        return (String) executeFunction(calendar, "monthCode", dol, callNode, dateLike);
    }

    // 12.1.12
    public static long calendarDay(JSTemporalCalendarObject calendar, DynamicObject dateLike, DynamicObjectLibrary dol,
                    JSFunctionCallNode callNode) {
        return (long) executeFunction(calendar, "day", dol, callNode, dateLike);
    }

    // 12.1.13
    public static long calendarDayOfWeek(JSTemporalCalendarObject calendar, DynamicObject dateLike, DynamicObjectLibrary dol,
                    JSFunctionCallNode callNode) {
        return (long) executeFunction(calendar, "dayOfWeek", dol, callNode, dateLike);
    }

    // 12.1.14
    public static long calendarDayOfYear(JSTemporalCalendarObject calendar, DynamicObject dateLike, DynamicObjectLibrary dol,
                    JSFunctionCallNode callNode) {
        return (long) executeFunction(calendar, "dayOfYear", dol, callNode, dateLike);
    }

    // 12.1.15
    public static long calendarWeekOfYear(JSTemporalCalendarObject calendar, DynamicObject dateLike, DynamicObjectLibrary dol,
                    JSFunctionCallNode callNode) {
        return (long) executeFunction(calendar, "weekOfYear", dol, callNode, dateLike);
    }

    // 12.1.16
    public static long calendarDaysInWeek(JSTemporalCalendarObject calendar, DynamicObject dateLike, DynamicObjectLibrary dol,
                    JSFunctionCallNode callNode) {
        return (long) executeFunction(calendar, "daysInWeek", dol, callNode, dateLike);
    }

    // 12.1.17
    public static long calendarDaysInMonth(JSTemporalCalendarObject calendar, DynamicObject dateLike, DynamicObjectLibrary dol,
                    JSFunctionCallNode callNode) {
        return (long) executeFunction(calendar, "daysInMonth", dol, callNode, dateLike);
    }

    // 12.1.18
    public static long calendarDaysInYear(JSTemporalCalendarObject calendar, DynamicObject dateLike, DynamicObjectLibrary dol,
                    JSFunctionCallNode callNode) {
        return (long) executeFunction(calendar, "daysInYear", dol, callNode, dateLike);
    }

    // 12.1.19
    public static long calendarMonthsInYear(JSTemporalCalendarObject calendar, DynamicObject dateLike, DynamicObjectLibrary dol,
                    JSFunctionCallNode callNode) {
        return (long) executeFunction(calendar, "monthsInYear", dol, callNode, dateLike);
    }

    // 12.1.20
    public static boolean calendarInLeapYear(JSTemporalCalendarObject calendar, DynamicObject dateLike, DynamicObjectLibrary dol,
                    JSFunctionCallNode callNode) {
        return (boolean) executeFunction(calendar, "inLeapYear", dol, callNode, dateLike);
    }

    // 12.1.21
    public static Object toTemporalCalendar(Object temporalCalendarLike, JSRealm realm, DynamicObjectLibrary dol,
                    IsObjectNode isObjectNode, JSToStringNode toStringNode,
                    IsConstructorNode isConstructorNode, JSFunctionCallNode callNode) {
        if (isObjectNode.executeBoolean(temporalCalendarLike)) {
            return temporalCalendarLike;
        }
        return calendarFrom(temporalCalendarLike, realm.getTemporalCalendarConstructor(), dol, isObjectNode, toStringNode,
                        isConstructorNode, callNode);
    }

    // 12.1.22
    public static Object toOptionalTemporalCalendar(Object temporalCalendarLike, JSRealm realm,
                    DynamicObjectLibrary dol, JSToStringNode toString,
                    IsObjectNode isObject, IsConstructorNode isConstructor,
                    JSFunctionCallNode callNode) {
        if (Undefined.instance.equals(temporalCalendarLike) || temporalCalendarLike == null) {
            return getISO8601Calender(realm, isConstructor, callNode);
        }
        return toTemporalCalendar(temporalCalendarLike, realm, dol, isObject, toString, isConstructor, callNode);
    }

    // 12.1.24
    public static Object calendarFrom(Object itemParam, DynamicObject constructor,
                    DynamicObjectLibrary dol, IsObjectNode isObject, JSToStringNode toString,
                    IsConstructorNode isConstructor, JSFunctionCallNode callNode) {
        Object item = itemParam;
        if (isObject.executeBoolean(item)) {
            if (!dol.containsKey((DynamicObject) item, "calendar")) {
                return item;
            }
            item = dol.getOrDefault((DynamicObject) item, "calendar", null);
            if (isObject.executeBoolean(item) && !dol.containsKey((DynamicObject) item, "calendar")) {
                return item;
            }
        }
        String string = toString.executeString(item);
        if (!isBuiltinCalendar(string)) {
            string = TemporalUtil.parseTemporalCalendarString(string);
        }
        return createTemporalCalendarFromStatic(constructor, string, isConstructor, callNode);
    }

    // 12.1.32
    public static boolean isISOLeapYear(long year) {
        if (year % 4 != 0) {
            return false;
        }
        if (year % 400 == 0) {
            return true;
        }
        if (year % 100 == 0) {
            return false;
        }
        return true;
    }

    // 12.1.33
    public static long isoDaysInYear(long year) {
        if (isISOLeapYear(year)) {
            return 366;
        }
        return 365;
    }

    // 12.1.34
    public static long isoDaysInMonth(long year, long month) {
        assert month >= 1 && month <= 12;
        if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {
            return 31;
        }
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        }
        if (isISOLeapYear(year)) {
            return 29;
        }
        return 28;
    }

    // 12.1.35
    // Formula: https://cs.uwaterloo.ca/~alopez-o/math-faq/node73.html
    public static long toISODayOfWeek(long year, long month, long day) {
        long m = month - 2;
        if (m == -1) {  // Jan
            m = 11;
        } else if (m == 0) { // Feb
            m = 12;
        }
        long c = Math.floorDiv(year, 100);
        long y = Math.floorMod(year, 100);
        if (m == 11 || m == 12) {
            y = y - 1;
        }
        long weekDay = Math.floorMod((day + (long) Math.floor((2.6 * m) - 0.2) - (2 * c) + y + Math.floorDiv(y, 4) + Math.floorDiv(c, 4)), 7);
        if (weekDay == 0) { // Sunday
            return 7;
        }
        return weekDay;
    }

    // 12.1.36
    public static long toISODayOfYear(long year, long month, long day) {
        long days = 0;
        for (int m = 1; m < month; m++) {
            days += isoDaysInMonth(year, m);
        }
        return days + day;
    }

    // 12.1.37
    public static long toISOWeekOfYear(long year, long month, long day) {
        long doy = toISODayOfYear(year, month, day);
        long dow = toISODayOfWeek(year, month, day);
        long doj = toISODayOfWeek(year, 1, 1);

        long week = Math.floorDiv(doy - dow + 10, 7);
        if (week < 1) {
            if (doj == 5 || (doj == 6 && isISOLeapYear(year - 1))) {
                return 53;
            } else {
                return 52;
            }
        }
        if (week == 53) {
            if (isoDaysInYear(year) - doy < 4 - dow) {
                return 1;
            }
        }

        return week;
    }

    // 12.1.38
    public static Object resolveISOMonth(DynamicObject fields, DynamicObjectLibrary dol,
                    JSStringToNumberNode stringToNumber, JSIdenticalNode identicalNode) {
        Object month = dol.getOrDefault(fields, MONTH, null);
        Object monthCode = dol.getOrDefault(fields, MONTH_CODE, null);
        if (monthCode == null) {
            if (month == null) {
                throw Errors.createTypeError("No month or month code present.");
            }
            return month;
        }
        assert monthCode instanceof String;
        int monthLength = ((String) monthCode).length();
        if (monthLength != 3) {
            throw Errors.createRangeError("Month code should be in 3 character code.");
        }
        String numberPart = ((String) monthCode).substring(2);
        double numberPart2 = stringToNumber.executeString(numberPart);
        if (Double.isNaN(numberPart2)) {
            throw Errors.createRangeError("The last character of the monthCode should be a number.");
        }
        if (month != null && !month.equals(numberPart2)) {
            throw Errors.createTypeError("Month equals not the month code.");
        }
        if (!identicalNode.executeBoolean(monthCode, TemporalUtil.buildISOMonthCode(numberPart))) {
            throw Errors.createRangeError("Not same value");
        }

        return (long) numberPart2;
    }

    // 12.1.39
    public static DynamicObject isoDateFromFields(DynamicObject fields, DynamicObject options, JSRealm realm, IsObjectNode isObject,
                    DynamicObjectLibrary dol, JSToBooleanNode toBoolean,
                    JSToStringNode toString, JSStringToNumberNode stringToNumber,
                    JSIdenticalNode identicalNode) {
        assert isObject.executeBoolean(fields);
        String overflow = TemporalUtil.toTemporalOverflow(options, dol, isObject, toBoolean, toString);
        DynamicObject preparedFields = JSTemporalPlainDate.toTemporalDateFields(fields,
                        TemporalUtil.toSet(DAY, MONTH,
                                        MONTH_CODE, YEAR),
                        realm, isObject, dol);
        Object year = dol.getOrDefault(preparedFields, YEAR, null);
        if (year == null) {
            throw Errors.createTypeError("Year not present.");
        }
        Object month = resolveISOMonth(preparedFields, dol, stringToNumber, identicalNode);
        Object day = dol.getOrDefault(preparedFields, DAY, null);
        return JSTemporalPlainDate.regulateISODate((Long) year, (Long) month, (Long) day, overflow, realm);
    }

    // 12.1.40
    public static DynamicObject isoYearMonthFromFields(DynamicObject fields, DynamicObject options, JSRealm realm, IsObjectNode isObject,
                    DynamicObjectLibrary dol, JSToBooleanNode toBoolean,
                    JSToStringNode toString, JSStringToNumberNode stringToNumber,
                    JSIdenticalNode identicalNode) {
        assert isObject.executeBoolean(fields);
        String overflow = TemporalUtil.toTemporalOverflow(options, dol, isObject, toBoolean, toString);
        DynamicObject preparedFields = JSTemporalPlainDate.toTemporalDateFields(fields,
                        TemporalUtil.toSet(MONTH,
                                        MONTH_CODE, YEAR),
                        realm, isObject, dol);
        Object year = dol.getOrDefault(preparedFields, YEAR, null);
        if (year == null) {
            throw Errors.createTypeError("Year not present.");
        }
        Object month = resolveISOMonth(preparedFields, dol, stringToNumber, identicalNode);

        DynamicObject result = TemporalUtil.regulateISOYearMonth(TemporalUtil.asLong(year), TemporalUtil.asLong(month), overflow, realm);
        JSObjectUtil.putDataProperty(realm.getContext(), result, "referenceISODay", 1);
        return result;
    }

    // 12.1.41
    public static DynamicObject isoMonthDayFromFields(DynamicObject fields, DynamicObject options, JSRealm realm, IsObjectNode isObject,
                    DynamicObjectLibrary dol, JSToBooleanNode toBoolean,
                    JSToStringNode toString, JSStringToNumberNode stringToNumber,
                    JSIdenticalNode identicalNode) {
        assert isObject.executeBoolean(fields);
        String overflow = TemporalUtil.toTemporalOverflow(options, dol, isObject, toBoolean, toString);
        DynamicObject preparedFields = JSTemporalPlainDate.toTemporalDateFields(fields,
                        TemporalUtil.toSet(DAY, MONTH, MONTH_CODE, YEAR),
                        realm, isObject, dol);
        Object month = dol.getOrDefault(preparedFields, MONTH, null);
        Object monthCode = dol.getOrDefault(preparedFields, MONTH_CODE, null);
        Object year = dol.getOrDefault(preparedFields, YEAR, null);
        if (month != null && monthCode == null && year == null) {
            throw Errors.createTypeError("A year or a month code should be present.");
        }
        month = resolveISOMonth(preparedFields, dol, stringToNumber, identicalNode);
        Object day = dol.getOrDefault(preparedFields, DAY, null);
        if (day == null) {
            throw Errors.createTypeError("Day not present.");
        }
        long referenceISOYear = 1972;
        DynamicObject result = null;
        if (monthCode == null) {
            result = JSTemporalPlainDate.regulateISODate((Long) year, (Long) month, (Long) day, overflow, realm);
        } else {
            result = JSTemporalPlainDate.regulateISODate(referenceISOYear, (Long) month, (Long) day, overflow, realm);
        }
        DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putDataProperty(realm.getContext(), record, MONTH, dol.getOrDefault(result, MONTH, 0L));
        JSObjectUtil.putDataProperty(realm.getContext(), record, DAY, dol.getOrDefault(result, DAY, 0L));
        JSObjectUtil.putDataProperty(realm.getContext(), record, "referenceISOYear", referenceISOYear);
        return record;
    }

    // 12.1.42
    public static long isoYear(DynamicObject dateOrDateTime, JSRealm realm, IsObjectNode isObject,
                    DynamicObjectLibrary dol, JSToBooleanNode toBoolean, JSToStringNode toString,
                    JSToIntegerAsLongNode toInt) {
        if (!isObject.executeBoolean(dateOrDateTime) || !dol.containsKey(dateOrDateTime, YEAR)) {
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(
                            dateOrDateTime, null, realm, isObject, dol, toBoolean, toString);
            return date.getYear();
        }
        return toInt.executeLong(dol.getOrDefault(dateOrDateTime, YEAR, 0L));
    }

    // 12.1.43
    public static long isoMonth(DynamicObject dateOrDateTime, JSRealm realm, IsObjectNode isObject,
                    DynamicObjectLibrary dol, JSToBooleanNode toBoolean, JSToStringNode toString,
                    JSToIntegerAsLongNode toInt) {
        if (!isObject.executeBoolean(dateOrDateTime) || !dol.containsKey(dateOrDateTime, MONTH)) {
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(
                            dateOrDateTime, null, realm, isObject, dol, toBoolean, toString);
            return date.getMonth();
        }
        return toInt.executeLong(dol.getOrDefault(dateOrDateTime, MONTH, 0L));
    }

    // 12.1.44
    public static String isoMonthCode(DynamicObject dateOrDateTime, JSRealm realm, IsObjectNode isObject,
                    DynamicObjectLibrary dol, JSToBooleanNode toBoolean, JSToStringNode toString,
                    JSToIntegerAsLongNode toInt) {
        long month = isoMonth(dateOrDateTime, realm, isObject, dol, toBoolean, toString, toInt);
        String monthCode = String.format("%1$2d", month).replace(" ", "0");
        return "M".concat(monthCode);
    }

    public static String isoMonthCode(JSTemporalPlainYearMonthObject yearMonth) {
        long month = yearMonth.getIsoMonth();
        String monthCode = String.format("%1$2d", month).replace(" ", "0");
        return "M".concat(monthCode);
    }

    // 12.1.45
    public static long isoDay(DynamicObject dateOrDateTime, JSRealm realm, IsObjectNode isObject,
                    DynamicObjectLibrary dol, JSToBooleanNode toBoolean, JSToStringNode toString,
                    JSToIntegerAsLongNode toInt) {
        if (!isObject.executeBoolean(dateOrDateTime) || !dol.containsKey(dateOrDateTime, MONTH)) {
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(
                            dateOrDateTime, null, realm, isObject, dol, toBoolean, toString);
            return date.getDay();
        }
        return toInt.executeLong(dol.getOrDefault(dateOrDateTime, DAY, 0L));
    }
}
