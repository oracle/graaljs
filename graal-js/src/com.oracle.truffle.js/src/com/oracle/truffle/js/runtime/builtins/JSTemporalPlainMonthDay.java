package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public class JSTemporalPlainMonthDay extends JSNonProxy implements JSConstructorFactory.Default.WithSpecies,
        PrototypeSupplier {

    public static final JSTemporalPlainMonthDay INSTANCE = new JSTemporalPlainMonthDay();

    public static final String CLASS_NAME = "TemporalPlainMonthDay";
    public static final String PROTOTYPE_NAME = "TemporalPlainMonthDay.prototype";

    public static DynamicObject create(JSContext context, long isoMonth, long isoDay, JSTemporalCalendarObject calendar,
                                       long referenceISOYear) {
        if (!JSTemporalPlainDate.validateISODate(referenceISOYear, isoMonth, isoDay)) {
            throw Errors.createRangeError("Not a valid date.");
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

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, constructor);

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
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static boolean isJSTemporalPlainMonthDay(Object obj) {
        return obj instanceof JSTemporalPlainMonthDayObject;
    }
}
