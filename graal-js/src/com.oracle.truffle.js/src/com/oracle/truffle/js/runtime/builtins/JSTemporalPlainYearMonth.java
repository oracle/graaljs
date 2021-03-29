package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public class JSTemporalPlainYearMonth extends JSNonProxy implements JSConstructorFactory.Default.WithSpecies,
        PrototypeSupplier{

    public static final JSTemporalPlainYearMonth INSTANCE = new JSTemporalPlainYearMonth();

    public static final String CLASS_NAME = "TemporalPlainYearMonth";
    public static final String PROTOTYPE_NAME = "TemporalPlainYearMonth.prototype";

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

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, constructor);

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
        return obj instanceof JSTemporalPlainYearMonth;
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
