package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public class JSTemporalPlainDate extends JSNonProxy implements JSConstructorFactory.Default.WithSpecies,
        PrototypeSupplier{

    public static final JSTemporalPlainDate INSTANCE = new JSTemporalPlainDate();

    public static final String CLASS_NAME = "TemporalPlainDate";
    public static final String PROTOTYPE_NAME = "TemporalPlainDate.prototype";

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
}
