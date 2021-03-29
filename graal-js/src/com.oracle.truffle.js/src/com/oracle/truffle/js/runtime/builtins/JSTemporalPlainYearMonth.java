package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class JSTemporalPlainYearMonth extends JSNonProxy implements JSConstructorFactory.Default.WithSpecies,
        PrototypeSupplier{

    public static final JSTemporalPlainYearMonth INSTANCE = new JSTemporalPlainYearMonth();

    public static final String CLASS_NAME = "TemporalPlainYearMonth";
    public static final String PROTOTYPE_NAME = "TemporalPlainYearMonth.prototype";

    public static final String CALENDAR = "calendar";
    public static final String YEAR = "year";
    public static final String MONTH = "month";
    public static final String MONTH_CODE = "monthCode";
    public static final String DAYS_IN_YEAR = "daysInYear";
    public static final String DAYS_IN_MONTH = "daysInMonth";
    public static final String MONTHS_IN_YEAR = "monthsInYear";
    public static final String IN_LEAP_YEAR = "inLeapYear";

    private JSTemporalPlainYearMonth() {
    }

    public static DynamicObject create(JSContext context, long isoYear, long isoMonth, JSTemporalCalendarObject calendar,
                                       long referenceISODay) {
        if (!JSTemporalPlainDate.validateISODate(isoYear, isoMonth, referenceISODay)) {
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
                                return JSTemporalCalendar.calendarYear(
                                        temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth,
                                        DynamicObjectLibrary.getUncached(), JSFunctionCallNode.createCall()
                                );
                            case MONTH:
                                return JSTemporalCalendar.calendarMonth(
                                        temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth,
                                        DynamicObjectLibrary.getUncached(), JSFunctionCallNode.createCall()
                                );
                            case MONTH_CODE:
                                return JSTemporalCalendar.calendarMonthCode(
                                        temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth,
                                        DynamicObjectLibrary.getUncached(), JSFunctionCallNode.createCall()
                                );
                            case DAYS_IN_YEAR:
                                return JSTemporalCalendar.calendarDaysInYear(
                                        temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth,
                                        DynamicObjectLibrary.getUncached(), JSFunctionCallNode.createCall()
                                );
                            case DAYS_IN_MONTH:
                                return JSTemporalCalendar.calendarDaysInMonth(
                                        temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth,
                                        DynamicObjectLibrary.getUncached(), JSFunctionCallNode.createCall()
                                );
                            case MONTHS_IN_YEAR:
                                return JSTemporalCalendar.calendarMonthsInYear(
                                        temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth,
                                        DynamicObjectLibrary.getUncached(), JSFunctionCallNode.createCall()
                                );
                            case IN_LEAP_YEAR:
                                return JSTemporalCalendar.calendarInLeapYear(
                                        temporalPlainYearMonth.getCalendar(), temporalPlainYearMonth,
                                        DynamicObjectLibrary.getUncached(), JSFunctionCallNode.createCall()
                                );
                            default:
                                errorBranch.enter();
                                throw Errors.createTypeErrorTemporalPlainMonthYearExpected();
                        }
                    } else {
                        errorBranch.enter();
                        throw  Errors.createTypeErrorTemporalPlainMonthYearExpected();
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
        return INSTANCE.createConstructorAndPrototype(realm);
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

}
