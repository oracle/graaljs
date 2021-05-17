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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS_IN_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.IN_LEAP_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ISO8601;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS_IN_YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH_CODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.TemporalPlainYearMonthFunctionBuiltins;
import com.oracle.truffle.js.builtins.TemporalPlainYearMonthPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class JSTemporalPlainYearMonth extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalPlainYearMonth INSTANCE = new JSTemporalPlainYearMonth();

    public static final String CLASS_NAME = "TemporalPlainYearMonth";
    public static final String PROTOTYPE_NAME = "TemporalPlainYearMonth.prototype";

    private JSTemporalPlainYearMonth() {
    }

    public static DynamicObject create(JSContext context, long isoYear, long isoMonth, DynamicObject calendar,
                    long referenceISODay) {
        if (!TemporalUtil.validateISODate(isoYear, isoMonth, referenceISODay)) {
            throw Errors.createRangeError("Not a valid date.");
        }
        if (!validateISOYearMonthRange(isoYear, isoMonth)) {
            throw Errors.createRangeError("Invalid year month range.");
        }

        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalPlainYearMonthFactory();
        DynamicObject obj = factory.initProto(new JSTemporalPlainYearMonthObject(factory.getShape(realm), isoYear,
                        isoMonth, referenceISODay, calendar), realm);
        return context.trackAllocation(obj);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.PlainYearMonth";
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static DynamicObject createGetterFunction(JSRealm realm, BuiltinFunctionKey functionKey,
                    String property) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(functionKey, (c) -> {
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(c.getLanguage(), null, null) {
                private final BranchProfile errorBranch = BranchProfile.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object obj = frame.getArguments()[0];
                    if (JSTemporalPlainYearMonth.isJSTemporalPlainYearMonth(obj)) {
                        JSTemporalPlainYearMonthObject temporalPlainYearMonth = (JSTemporalPlainYearMonthObject) obj;
                        switch (property) {
                            case CALENDAR:
                                return temporalPlainYearMonth.getCalendar();
                            case YEAR:
                                return JSTemporalCalendar.calendarYear(temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth);
                            case MONTH:
                                return JSTemporalCalendar.calendarMonth(temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth);
                            case MONTH_CODE:
                                return JSTemporalCalendar.calendarMonthCode(temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth);
                            case DAYS_IN_YEAR:
                                return JSTemporalCalendar.calendarDaysInYear(temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth);
                            case DAYS_IN_MONTH:
                                return JSTemporalCalendar.calendarDaysInMonth(temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth);
                            case MONTHS_IN_YEAR:
                                return JSTemporalCalendar.calendarMonthsInYear(temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth);
                            case IN_LEAP_YEAR:
                                return JSTemporalCalendar.calendarInLeapYear(temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth);
                            default:
                                errorBranch.enter();
                                throw Errors.createTypeErrorTemporalPlainMonthYearExpected();
                        }
                    } else {
                        errorBranch.enter();
                        throw Errors.createTypeErrorTemporalPlainMonthYearExpected();
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

        JSObjectUtil.putBuiltinAccessorProperty(prototype, CALENDAR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalPlainYearMonthCalendar, CALENDAR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, YEAR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalPlainYearMonthYear, YEAR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTH,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalPlainYearMonthMonth, MONTH), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTH_CODE,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalPlainYearMonthMonthCode, MONTH_CODE), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_YEAR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalPlainYearMonthDaysInYear, DAYS_IN_YEAR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS_IN_MONTH,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalPlainYearMonthDaysInMonth, DAYS_IN_MONTH), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTHS_IN_YEAR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalPlainYearMonthMonthsInYear, MONTHS_IN_YEAR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, IN_LEAP_YEAR,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalPlainYearMonthInLeapYear, IN_LEAP_YEAR), Undefined.instance);

        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalPlainYearMonthPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, "Temporal.PlainYearMonth");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainYearMonth.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainYearMonthPrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalPlainYearMonthFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalPlainYearMonth(Object obj) {
        return obj instanceof JSTemporalPlainYearMonthObject;
    }

    // 9.5.4
    public static boolean validateISOYearMonthRange(long year, long month) {
        if (year < -271821 || year > 275760) {
            return false;
        }
        if (year == -271821 && month < 4) {
            return false;
        }
        if (year == 275760 && month > 9) {
            return false;
        }
        return true;
    }

    public static String temporalYearMonthToString(JSTemporalPlainYearMonthObject ym, String showCalendar) {
        Object year = TemporalUtil.padISOYear(ym.getISOYear());
        String month = String.format("%1$2d", ym.getISOMonth()).replace(" ", "0");
        String result = year + "-" + month;
        String calendarID = JSRuntime.toString(ym.getCalendar());
        if (!ISO8601.equals(calendarID)) {
            String day = String.format("%1$2d", ym.getISODay()).replace(" ", "0");
            result += "-" + day;
        }
        String calendarString = TemporalUtil.formatCalendarAnnotation(calendarID, showCalendar);
        if (!"".equals(calendarString)) {
            result += calendarString;
        }
        return result;
    }

}
