package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JSTemporalPlainDate extends JSNonProxy implements JSConstructorFactory.Default.WithSpecies,
        PrototypeSupplier{

    public static final JSTemporalPlainDate INSTANCE = new JSTemporalPlainDate();

    public static final String CLASS_NAME = "TemporalPlainDate";
    public static final String PROTOTYPE_NAME = "TemporalPlainDate.prototype";

    public static final String YEAR = "year";
    public static final String MONTH_CODE = "monthCode";
    public static final String MONTH = "month";
    public static final String DAY = "day";

    private JSTemporalPlainDate() {
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.PlainDate";
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);

        JSObjectUtil.putToStringTag(prototype, "Temporal.PlainDate");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainDate.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainDatePrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static boolean isJSTemporalPlainDate(Object obj) {
        return obj instanceof JSTemporalPlainDateObject;
    }

    public static DynamicObject createTemporalDate(JSContext context, long y, long m, long d) {
        rejectDate(y, m, d);
        if(!dateTimeWithinLimits(y, m, d, 12, 0, 0, 0, 0, 0)) {
            throw Errors.createRangeError("Date is not within range.");
        }
        // TODO: Check if calendar is not an object.
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalPlainDateFactory();
        DynamicObject object = factory.initProto(new JSTemporalPlainDateObject(factory.getShape(realm),
                (int) y, (int)m, (int)d
        ), realm);
        return context.trackAllocation(object);
    }

    private static void rejectDate(long year, long month, long day) {
        if (!validateDate(year, month, day)) {
            throw Errors.createRangeError("Given date outside the range.");
        }
    }

    private static boolean validateDate(long year, long month, long day) {
        if (month < 1 || month > 12) {
            return false;
        }
        long daysInMonth = daysInMonth(year, month);
        if (day < 1 || day > daysInMonth) {
            return false;
        }
        return true;
    }

    private static int daysInMonth(long year, long month) {
        assert month >= 1;
        assert month <= 12;
        if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {
            return 31;
        }
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        }
        if (isLeapYear(year)) {
            return 29;
        }
        return 28;
    }

    private static boolean isLeapYear(long year) {
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

    private static boolean dateTimeWithinLimits(long year, long month, long day, long hour, long minute, long second,
                                                long millisecond, long microsecond, long nanosecond) {
        long ns = getEpochFromParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);
        if((ns / 100_000_000_000_000L) <= -864_000L - 864L) {
            return false;
        } else if ((ns / 100_000_000_000_000L) >= 864_000L + 864L) {
            return false;
        }
        return true;
    }

    private static long getEpochFromParts(long year, long month, long day, long hour, long minute, long second,
                                          long millisecond, long microsecond, long nanosecond) {
        assert month >= 1 && month <= 12;
        assert day >= 1 && day <= daysInMonth(year, month);
        assert JSTemporalPlainTime.validateTime(hour, minute, second, millisecond, microsecond, nanosecond);
        double date = JSDate.makeDay(year, month, day);
        double time = JSDate.makeTime(hour, minute, second, millisecond);
        double ms = JSDate.makeDate(date, time);
        assert isFinite(ms);
        return (long) ((ms * 1_000_000) + (microsecond * 1_000) + nanosecond);
    }

    private static boolean isFinite(double d) {
        return !(Double.isNaN(d) || Double.isInfinite(d));
    }

    // 3.5.3
    public static Object createTemporalDateFromStatic(DynamicObject constructor, long isoYear, long isoMonth, long isoDay,
                                                      DynamicObject calendar, IsConstructorNode isConstructor,
                                                      JSFunctionCallNode callNode) {
        assert validateISODate(isoYear, isoMonth, isoDay);
        if (!isConstructor.executeBoolean(constructor)) {
            throw Errors.createTypeError("Given constructor is not an constructor.");
        }
        Object[] ctorArgs = new Object[]{isoYear, isoMonth, isoDay, calendar};
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, ctorArgs.length);
        System.arraycopy(ctorArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, ctorArgs.length);
        Object result = callNode.executeCall(args);
        return result;
    }

    // 3.5.4
    public static Object toTemporalDate(DynamicObject item, DynamicObject constructor, DynamicObject options,
                                               JSRealm realm, IsObjectNode isObject, DynamicObjectLibrary dol,
                                               JSToBooleanNode toBoolean, JSToStringNode toString,
                                               IsConstructorNode isConstructor, JSFunctionCallNode callNode) {
        try {
            constructor = constructor != null ? constructor : realm.getTemporalPlainDateConstructor();
            options = options != null ? options : JSObjectUtil.createOrdinaryPrototypeObject(realm);
            if (isObject.executeBoolean(item)) {
                if (isJSTemporalPlainDate(item)) {
                    return item;
                }
                DynamicObject calendar = null;  // TODO: Call JSTemporalCalendar.getOptionalTemporalCalendar()
                Set<String> fieldNames = null; // TODO: Call JSTemporalCalendar.calendarFields()
                DynamicObject fields = toTemporalDateFields(item, fieldNames, realm, isObject, dol);
                return null;    // TODO: Call JSTemporalCalendar.dateFromFields()
            }
            String overflows = TemporalUtil.toTemporalOverflow(options, dol, isObject, toBoolean, toString);
            String string = toString.executeString(item);
            DynamicObject result = null;    // TODO: Call TemporalUtil.parseTemporalDateString
            if (!validateISODate(dol.getLongOrDefault(result, YEAR, 0),
                    dol.getLongOrDefault(result, MONTH, 0), dol.getLongOrDefault(result, DAY, 0))) {
                throw Errors.createRangeError("Given date is not valid.");
            }
            DynamicObject calendar = null;  // TODO: Call JSTemporalCalendar.toOptionalTemporalCalendar()
            result = regulateISODate(dol.getLongOrDefault(result, YEAR, 0),
                    dol.getLongOrDefault(result, MONTH, 0), dol.getLongOrDefault(result, DAY, 0),
                    overflows, realm);
            return createTemporalDateFromStatic(constructor, dol.getLongOrDefault(result, YEAR, 0),
                    dol.getLongOrDefault(result, MONTH, 0), dol.getLongOrDefault(result, DAY, 0),
                    calendar, isConstructor, callNode);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 3.5.6
    public static DynamicObject toTemporalDateFields(DynamicObject temporalDateLike, Set<String> fieldNames, JSRealm realm, IsObjectNode isObject, DynamicObjectLibrary dol) {
        return TemporalUtil.prepareTemporalFields(temporalDateLike, fieldNames, Collections.emptySet(), realm, isObject, dol);
    }

    // 3.5.7
    public static DynamicObject regulateISODate(long year, long month, long day, String overflow, JSRealm realm) {
        assert overflow.equals("constrain") || overflow.equals("reject");
        if (overflow.equals("reject")) {
            rejectISODate(year, month, day);
            return toRecord(year, month, day, realm);
        }
        if (overflow.equals("constrain")) {
            return constrainISODate(year, month, day, realm);
        }
        throw new RuntimeException("This should never have happened.");
    }

    // 3.5.8
    public static boolean validateISODate(long year, long month, long day) {
        if (month < 1 || month > 12) {
            return false;
        }
        long daysInMonth = 31;   // TODO: Call JSTemporalCalendar.isoDaysInMonth();
        if (day < 1 || day > daysInMonth) {
            return false;
        }
        return true;
    }

    // 3.5.9
    public static void rejectISODate(long year, long month, long day) {
        if (!validateISODate(year, month, day)) {
            throw Errors.createRangeError("Given date is not valid.");
        }
    }

    // 3.5.10
    public static DynamicObject constrainISODate(long year, long month, long day, JSRealm realm) {
        month = TemporalUtil.constraintToRange(month, 1, 12);
        day = TemporalUtil.constraintToRange(day, 1, 31);   // TODO: Replace 31 with call JSTemporalCalendar.isoDaysInMonth
        return toRecord(year, month, day, realm);
    }

    private static DynamicObject toRecord(long year, long month, long day, JSRealm realm) {
        DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putDataProperty(realm.getContext(), record, YEAR, year);
        JSObjectUtil.putDataProperty(realm.getContext(), record, MONTH, month);
        JSObjectUtil.putDataProperty(realm.getContext(), record, DAY, day);
        return record;
    }
}
