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
package com.oracle.truffle.js.runtime.builtins.temporal;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ISO8601;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH_CODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;

import java.util.HashSet;
import java.util.Set;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class JSTemporalPlainMonthDay extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalPlainMonthDay INSTANCE = new JSTemporalPlainMonthDay();

    public static final String CLASS_NAME = "TemporalPlainMonthDay";
    public static final String PROTOTYPE_NAME = "TemporalPlainMonthDay.prototype";

    public static DynamicObject create(JSContext context, long isoMonth, long isoDay, long referenceISOYear, DynamicObject calendar) {
        if (!TemporalUtil.validateISODate(referenceISOYear, isoMonth, isoDay)) {
            throw TemporalErrors.createRangeErrorDateOutsideRange();
        }

        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalPlainMonthDayFactory();
        DynamicObject obj = factory.initProto(new JSTemporalPlainMonthDayObject(factory.getShape(realm), isoMonth,
                        isoDay, calendar, referenceISOYear), realm);
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

    private static DynamicObject createGetterFunction(JSRealm realm, BuiltinFunctionKey functionKey, String property) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(functionKey, (c) -> {
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(c.getLanguage(), null, null) {
                private final BranchProfile errorBranch = BranchProfile.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object obj = frame.getArguments()[0];
                    if (JSTemporalPlainMonthDay.isJSTemporalPlainMonthDay(obj)) {
                        JSTemporalPlainMonthDayObject plainMD = (JSTemporalPlainMonthDayObject) obj;
                        switch (property) {
                            case DAY:
                                // TODO wrong
                                return (int) plainMD.getISODay();
                            case MONTH_CODE:
                                DynamicObject calendar = plainMD.getCalendar();
                                return JSTemporalCalendar.calendarMonthCode(calendar, (DynamicObject) obj);
                            case CALENDAR:
                                return plainMD.getCalendar();

                            default:
                                errorBranch.enter();
                                throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
                        }
                    } else {
                        errorBranch.enter();
                        throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
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
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalPlainMonthDayCalendar, CALENDAR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTH_CODE,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalPlainMonthDayMonthCode, MONTH_CODE), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAY,
                        createGetterFunction(realm, BuiltinFunctionKey.TemporalPlainMonthDayDay, DAY), Undefined.instance);

        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalPlainMonthDayPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, "Temporal.PlainMonthDay");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainMonthDay.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainMonthDayPrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalPlainMonthDayFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalPlainMonthDay(Object obj) {
        return obj instanceof JSTemporalPlainMonthDayObject;
    }

    @TruffleBoundary
    public static DynamicObject toTemporalMonthDay(Object item, DynamicObject optParam, JSContext ctx) {
        DynamicObject options = optParam;
        if (optParam == Undefined.instance) {
            options = JSOrdinary.createWithNullPrototype(ctx);
        }
        long referenceISOYear = 1972;
        if (JSRuntime.isObject(item)) {
            DynamicObject itemObj = (DynamicObject) item;
            if (JSTemporalPlainMonthDay.isJSTemporalPlainMonthDay(itemObj)) {
                return itemObj;
            }
            DynamicObject calendar = null;
            boolean calendarAbsent = false;
            if (JSTemporalPlainDate.isJSTemporalPlainDate(itemObj) || JSTemporalPlainDateTime.isJSTemporalPlainDateTime(itemObj) || JSTemporalPlainTime.isJSTemporalPlainTime(itemObj) ||
                            JSTemporalPlainYearMonth.isJSTemporalPlainYearMonth(itemObj) || TemporalUtil.isTemporalZonedDateTime(itemObj)) {
                assert itemObj instanceof TemporalCalendar; // basically, that's above line's check,
                calendar = ((TemporalCalendar) itemObj).getCalendar();
                calendarAbsent = false;
            } else {
                calendar = (DynamicObject) JSObject.get(itemObj, CALENDAR);
                calendarAbsent = calendar == Undefined.instance;
                calendar = TemporalUtil.toOptionalTemporalCalendar(calendar, ctx);
            }
            Set<String> fieldNames = TemporalUtil.calendarFields(calendar, new String[]{DAY, MONTH, MONTH_CODE, YEAR}, ctx);
            DynamicObject fields = TemporalUtil.prepareTemporalFields(itemObj, fieldNames, new HashSet<>(), ctx);

            Object month = JSObject.get(fields, MONTH);
            Object monthCode = JSObject.get(fields, MONTH_CODE);
            Object year = JSObject.get(fields, YEAR);
            if (calendarAbsent && month != Undefined.instance && monthCode == Undefined.instance && year == Undefined.instance) {
                JSObjectUtil.putDataProperty(ctx, fields, YEAR, referenceISOYear);
                return monthDayFromFields(calendar, fields, options);
            }
        }
        TemporalUtil.toTemporalOverflow(options);
        String string = JSRuntime.toString(item);
        JSTemporalPlainDateTimeRecord result = TemporalUtil.parseTemporalMonthDayString(string, ctx);
        DynamicObject calendar = TemporalUtil.toOptionalTemporalCalendar(result.getCalendar(), ctx);
        if (result.getYear() == 0) { // TODO Check for undefined here!
            if (!TemporalUtil.validateISODate(referenceISOYear, result.getMonth(), result.getDay())) {
                throw Errors.createRangeError("invalid date");
            }
            // TODO year should be undefined!
            return JSTemporalPlainMonthDay.create(ctx, result.getMonth(), result.getDay(), 0, calendar);
        }
        DynamicObject result2 = JSTemporalPlainMonthDay.create(ctx, result.getMonth(), result.getDay(), referenceISOYear, calendar);
        DynamicObject canonicalMonthDayOptions = JSOrdinary.createWithNullPrototype(ctx);
        return monthDayFromFields(calendar, result2, canonicalMonthDayOptions);
    }

    private static DynamicObject monthDayFromFields(DynamicObject calendar, DynamicObject fields, DynamicObject options) {
        DynamicObject fn = (DynamicObject) JSObject.getMethod(calendar, "monthDayFromFields");
        Object monthDay = JSRuntime.call(fn, calendar, new Object[]{fields, options});
        return TemporalUtil.requireTemporalMonthDay(monthDay);
    }

    @TruffleBoundary
    public static String temporalMonthDayToString(JSTemporalPlainMonthDayObject md, String showCalendar) {
        String monthString = String.format("%1$2d", md.getISOMonth()).replace(" ", "0");
        String dayString = String.format("%1$2d", md.getISODay()).replace(" ", "0");

        String calendarID = JSRuntime.toString(md.getCalendar());
        if (!ISO8601.equals(calendarID)) {
            String year = TemporalUtil.padISOYear(md.getISOYear());
            return String.format("%s-%s-%s", year, monthString, dayString);
        }
        String calendar = TemporalUtil.formatCalendarAnnotation(calendarID, showCalendar);
        if ("".equals(calendar)) {
            return String.format("%s-%s", monthString, dayString);
        } else {
            return String.format("%s-%s%s", monthString, dayString, calendar);
        }
    }
}
