package com.oracle.truffle.js.runtime.builtins;

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

import static com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey.TemporalCalendarId;

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
        Object[] ctorArgs = new Object[] {id};
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

    // 12.1.21
    public static Object toTemporalCalendar(DynamicObject temporalCalendarLike, JSRealm realm, DynamicObjectLibrary dol,
                                            IsObjectNode isObjectNode, JSToStringNode toStringNode,
                                            IsConstructorNode isConstructorNode, JSFunctionCallNode callNode) {
        if (isObjectNode.executeBoolean(temporalCalendarLike)) {
            return temporalCalendarLike;
        }
        return calendarFrom(temporalCalendarLike, realm.getTemporalCalendarConstructor(), dol, isObjectNode, toStringNode,
                isConstructorNode, callNode);
    }

    // 12.1.22
    public static Object toOptionalTemporalCalendar(DynamicObject temporalCalendarLike, JSRealm realm,
                                                    DynamicObjectLibrary dol, JSToStringNode toString,
                                                    IsObjectNode isObject, IsConstructorNode isConstructor,
                                                    JSFunctionCallNode callNode) {
        if (Undefined.instance.equals(temporalCalendarLike) || temporalCalendarLike == null) {
            return getISO8601Calender(realm, isConstructor, callNode);
        }
        return toTemporalCalendar(temporalCalendarLike, realm, dol, isObject, toString, isConstructor, callNode);
    }

    // 12.1.24
    public static Object calendarFrom(DynamicObject item, DynamicObject constructor,
                                      DynamicObjectLibrary dol, IsObjectNode isObject, JSToStringNode toString,
                                      IsConstructorNode isConstructor, JSFunctionCallNode callNode) {
        if (isObject.executeBoolean(item)) {
            if (!dol.containsKey(item, "calendar")) {
                return item;
            }
            item = (DynamicObject) dol.getOrDefault(item, "calendar", null);
            if (isObject.executeBoolean(item) && !dol.containsKey(item,"calendar")) {
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
        long weekDay = Math.floorMod((day + (long) Math.floor((2.6 * m) - 0.2) - (2 * c) + y + Math.floorDiv(y, 4)
                + Math.floorDiv(c, 4)), 7);
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
        Object month = dol.getOrDefault(fields, JSTemporalPlainDate.MONTH, null);
        Object monthCode = dol.getOrDefault(fields, JSTemporalPlainDate.MONTH_CODE, null);
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
        if(month != null && !month.equals(numberPart2)) {
            throw Errors.createTypeError("Month equals not the month code.");
        }
//        if(!identicalNode.executeBoolean(monthCode, numberPart)) {  // Doesn't make sense to me
//            throw Errors.createRangeError("Not same value");
//        }
        return (long) numberPart2;
    }

    // 12.1.39
    public static DynamicObject isoDateFromFields(DynamicObject fields, DynamicObject options, JSRealm realm, IsObjectNode isObject,
                                                  DynamicObjectLibrary dol, JSToBooleanNode toBoolean,
                                                  JSToStringNode toString, JSStringToNumberNode stringToNumber,
                                                  JSIdenticalNode identicalNode) {
        assert isObject.executeBoolean(fields);
        String overflow = TemporalUtil.toTemporalOverflow(options, dol, isObject, toBoolean, toString);
        fields = JSTemporalPlainDate.toTemporalDateFields(fields,
                TemporalUtil.toSet(JSTemporalPlainDate.DAY, JSTemporalPlainDate.MONTH,
                        JSTemporalPlainDate.MONTH_CODE, JSTemporalPlainDate.YEAR), realm, isObject, dol);
        Object year = dol.getOrDefault(fields, JSTemporalPlainDate.YEAR, null);
        if (year == null) {
            throw Errors.createTypeError("Year not present.");
        }
        Object month = resolveISOMonth(fields, dol, stringToNumber, identicalNode);
        Object day = dol.getOrDefault(fields, JSTemporalPlainDate.DAY, null);
        return JSTemporalPlainDate.regulateISODate((Long) year, (Long) month, (Long) day, overflow, realm);
    }

    // 12.1.40
    public static DynamicObject isoYearMonthFromFields(DynamicObject fields, DynamicObject options, JSRealm realm, IsObjectNode isObject,
                                                       DynamicObjectLibrary dol, JSToBooleanNode toBoolean,
                                                       JSToStringNode toString, JSStringToNumberNode stringToNumber,
                                                       JSIdenticalNode identicalNode) {
        assert isObject.executeBoolean(fields);
        String overflow = TemporalUtil.toTemporalOverflow(options, dol, isObject, toBoolean, toString);
        fields = fields;    // TODO: Call JSTemporalPlainYearMonth.toTemporalYearMonthFields()
        Object year = dol.getOrDefault(fields, JSTemporalPlainDate.YEAR, null);
        if (year == null) {
            throw Errors.createTypeError("Year not present.");
        }
        Object month = resolveISOMonth(fields, dol, stringToNumber, identicalNode);
        DynamicObject result = null;    // TODO: Call JSTemporalPlainYearMonth.regulateISOYearMonth()
        DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putDataProperty(realm.getContext(), record, JSTemporalPlainDate.YEAR, dol.getOrDefault(result, JSTemporalPlainDate.YEAR, 0L));
        JSObjectUtil.putDataProperty(realm.getContext(), record, JSTemporalPlainDate.MONTH, dol.getOrDefault(result, JSTemporalPlainDate.MONTH, 0L));
        JSObjectUtil.putDataProperty(realm.getContext(), record, "referenceISODay", 1);
        return record;
    }

    // 12.1.41
    public static DynamicObject isoMonthDayFromFields(DynamicObject fields, DynamicObject options, JSRealm realm, IsObjectNode isObject,
                                                      DynamicObjectLibrary dol, JSToBooleanNode toBoolean,
                                                      JSToStringNode toString, JSStringToNumberNode stringToNumber,
                                                      JSIdenticalNode identicalNode) {
        assert isObject.executeBoolean(fields);
        String overflow = TemporalUtil.toTemporalOverflow(options, dol, isObject, toBoolean, toString);
        fields = fields;    // TODO: Call JSTemporalPlainMonthDay.toTemporalMonthDayFields()
        Object month = dol.getOrDefault(fields, JSTemporalPlainDate.MONTH, null);
        Object monthCode = dol.getOrDefault(fields, JSTemporalPlainDate.MONTH_CODE, null);
        Object year = dol.getOrDefault(fields, JSTemporalPlainDate.YEAR, null);
        if (month != null && monthCode == null && year == null) {
            throw Errors.createTypeError("A year or a month code should be present.");
        }
        month = resolveISOMonth(fields, dol, stringToNumber, identicalNode);
        Object day = dol.getOrDefault(fields, JSTemporalPlainDate.DAY, null);
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
        JSObjectUtil.putDataProperty(realm.getContext(), record, JSTemporalPlainDate.MONTH, dol.getOrDefault(result, JSTemporalPlainDate.MONTH, 0L));
        JSObjectUtil.putDataProperty(realm.getContext(), record, JSTemporalPlainDate.DAY, dol.getOrDefault(result, JSTemporalPlainDate.DAY, 0L));
        JSObjectUtil.putDataProperty(realm.getContext(), record, "referenceISOYear", referenceISOYear);
        return record;
    }

    // 12.1.42
    public static long isoYear(DynamicObject dateOrDateTime, JSRealm realm, IsObjectNode isObject,
                               DynamicObjectLibrary dol, JSToBooleanNode toBoolean, JSToStringNode toString,
                               IsConstructorNode isConstructor, JSFunctionCallNode callNode,
                               JSToIntegerAsLongNode toInt) {
        if (!isObject.executeBoolean(dateOrDateTime) || !dol.containsKey(dateOrDateTime, JSTemporalPlainDate.YEAR)) {
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(
                    dateOrDateTime, null, null, realm, isObject, dol, toBoolean, toString, isConstructor,
                    callNode
            );
            return date.getYear();
        }
        return toInt.executeLong(dol.getOrDefault(dateOrDateTime, JSTemporalPlainDate.YEAR, 0L));
    }

    // 12.1.43
    public static long isoMonth(DynamicObject dateOrDateTime, JSRealm realm, IsObjectNode isObject,
                               DynamicObjectLibrary dol, JSToBooleanNode toBoolean, JSToStringNode toString,
                               IsConstructorNode isConstructor, JSFunctionCallNode callNode,
                               JSToIntegerAsLongNode toInt) {
        if (!isObject.executeBoolean(dateOrDateTime) || !dol.containsKey(dateOrDateTime, JSTemporalPlainDate.MONTH)) {
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(
                    dateOrDateTime, null, null, realm, isObject, dol, toBoolean, toString, isConstructor,
                    callNode
            );
            return date.getMonth();
        }
        return toInt.executeLong(dol.getOrDefault(dateOrDateTime, JSTemporalPlainDate.MONTH, 0L));
    }

    // 12.1.44
    public static String isoMonthCode(DynamicObject dateOrDateTime, JSRealm realm, IsObjectNode isObject,
                                DynamicObjectLibrary dol, JSToBooleanNode toBoolean, JSToStringNode toString,
                                IsConstructorNode isConstructor, JSFunctionCallNode callNode,
                                JSToIntegerAsLongNode toInt) {
        long month = isoMonth(dateOrDateTime, realm, isObject, dol, toBoolean, toString, isConstructor, callNode, toInt);
        String monthCode = String.format("%1$2d", month).replace(" ", "0");
        return "M".concat(monthCode);
    }

    // 12.1.45
    public static long isoDay(DynamicObject dateOrDateTime, JSRealm realm, IsObjectNode isObject,
                                DynamicObjectLibrary dol, JSToBooleanNode toBoolean, JSToStringNode toString,
                                IsConstructorNode isConstructor, JSFunctionCallNode callNode,
                                JSToIntegerAsLongNode toInt) {
        if (!isObject.executeBoolean(dateOrDateTime) || !dol.containsKey(dateOrDateTime, JSTemporalPlainDate.MONTH)) {
            JSTemporalPlainDateObject date = (JSTemporalPlainDateObject) JSTemporalPlainDate.toTemporalDate(
                    dateOrDateTime, null, null, realm, isObject, dol, toBoolean, toString, isConstructor,
                    callNode
            );
            return date.getDay();
        }
        return toInt.executeLong(dol.getOrDefault(dateOrDateTime, JSTemporalPlainDate.DAY, 0L));
    }
}
