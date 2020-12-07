package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public class JSTemporalDuration extends JSNonProxy implements JSConstructorFactory.Default.WithSpecies,
        PrototypeSupplier {

    public static final JSTemporalDuration INSTANCE = new JSTemporalDuration();

    public static final String CLASS_NAME = "TemporalDuration";
    public static final String PROTOTYPE_NAME = "TemporalDuration.prototype";

    private JSTemporalDuration() {
    }

    public static DynamicObject create(JSContext context, long years, long months, long weeks, long days, long hours,
                                       long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        if(!validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds,
                nanoseconds)) {
            throw Errors.createRangeError("Given duration outside range.");
        }
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalDurationFactory();
        DynamicObject obj = factory.initProto(new JSTemporalDurationObject(factory.getShape(realm),
                years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds), realm);
        return context.trackAllocation(obj);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.Duration";
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

        JSObjectUtil.putToStringTag(prototype, "Temporal.Duration");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalDuration.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalDurationPrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    //region Abstract methods
    // 7.5.3
    public static int durationSign(long years, long months, long weeks, long days, long hours, long minutes,
                                   long seconds, long milliseconds, long microseconds, long nanoseconds) {
        if (years < 0) {
            return -1;
        }
        if (years > 0) {
            return 1;
        }
        if (months < 0) {
            return -1;
        }
        if (months > 1) {
            return 1;
        }
        if (weeks < 0) {
            return -1;
        }
        if (weeks > 0) {
            return 1;
        }
        if (days < 0) {
            return -1;
        }
        if (days > 0) {
            return 1;
        }
        if (hours < 0) {
            return -1;
        }
        if (hours > 0) {
            return 1;
        }
        if (minutes < 0) {
            return -1;
        }
        if (minutes > 0) {
            return 1;
        }
        if (seconds < 0) {
            return -1;
        }
        if (seconds > 0) {
            return 1;
        }
        if (milliseconds < 0) {
            return -1;
        }
        if (milliseconds > 0) {
            return 1;
        }
        if (microseconds < 0) {
            return -1;
        }
        if (microseconds > 0) {
            return 1;
        }
        if (nanoseconds < 0) {
            return -1;
        }
        if (nanoseconds > 0) {
            return 1;
        }
        return 0;
    }

    // 7.5.5
    public static boolean validateTemporalDuration(long years, long months, long weeks, long days, long hours,
                                                   long minutes, long seconds, long milliseconds, long microseconds,
                                                   long nanoseconds) {
        int sign = durationSign(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds,
                nanoseconds);
        if (years < 0 && sign > 0) {
            return false;
        }
        if (years > 0 && sign < 0) {
            return false;
        }
        if (months < 0 && sign > 0) {
            return false;
        }
        if (months > 0 && sign < 0) {
            return false;
        }
        if (weeks < 0 && sign > 0) {
            return false;
        }
        if (weeks > 0 && sign < 0) {
            return false;
        }
        if (days < 0 && sign > 0) {
            return false;
        }
        if (days > 0 && sign < 0) {
            return false;
        }
        if (hours < 0 && sign > 0) {
            return false;
        }
        if (hours > 0 && sign < 0) {
            return false;
        }
        if (minutes < 0 && sign > 0) {
            return false;
        }
        if (minutes > 0 && sign < 0) {
            return false;
        }
        if (seconds < 0 && sign > 0) {
            return false;
        }
        if (seconds > 0 && sign < 0) {
            return false;
        }
        if (milliseconds < 0 && sign > 0) {
            return false;
        }
        if (milliseconds > 0 && sign < 0) {
            return false;
        }
        if (microseconds < 0 && sign > 0) {
            return false;
        }
        if (microseconds > 0 && sign < 0) {
            return false;
        }
        if (nanoseconds < 0 && sign > 0) {
            return false;
        }
        if (nanoseconds > 0 && sign < 0) {
            return false;
        }
        return true;
    }
    //endregion
}
