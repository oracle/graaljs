package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.TemporalTimeFunctionBuiltins;
import com.oracle.truffle.js.builtins.TemporalTimePrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class JSTemporalTime extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
        PrototypeSupplier {

    public static final JSTemporalTime INSTANCE = new JSTemporalTime();

    public static final String CLASS_NAME = "TemporalTime";
    public static final String PROTOTYPE_NAME = "TemporalTime.prototype";

    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private static final String SECOND = "second";
    private static final String MILLISECOND = "millisecond";
    private static final String MICROSECOND = "microsecond";
    private static final String NANOSECOND = "nanosecond";

    private JSTemporalTime() {
    }

    public static DynamicObject create(JSContext context, long hours, long minutes, long seconds, long milliseconds,
                                       long microseconds, long nanoseconds) {
        if (!validateTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
            throw Errors.createRangeError("Given time outside the range.");
        }
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalTimeFactory();
        DynamicObject obj = factory.initProto(new JSTemporalTimeObject(factory.getShape(realm),
                (int) hours, (int) minutes, (int) seconds, (int) milliseconds, (int) microseconds, (int) nanoseconds
        ), realm);
        return context.trackAllocation(obj);
    }

    private static boolean validateTime(long hours, long minutes, long seconds, long milliseconds, long microseconds,
                                        long nanoseconds) {
        if (hours < 0 || hours > 23) {
            return false;
        }
        if (minutes < 0 || minutes > 59) {
            return false;
        }
        if (seconds < 0 || seconds > 59) {
            return false;
        }
        if (milliseconds < 0 || milliseconds > 999) {
            return false;
        }
        if (microseconds < 0 || microseconds > 999) {
            return false;
        }
        if (nanoseconds < 0 || nanoseconds > 999) {
            return false;
        }
        return true;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.Time";
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
                    if (JSTemporalTime.isJSTemporalTime(obj)) {
                        JSTemporalTimeObject temporalTime = (JSTemporalTimeObject) obj;
                        switch (property) {
                            case HOUR:
                                return temporalTime.getHours();
                            case MINUTE:
                                return temporalTime.getMinutes();
                            case SECOND:
                                return temporalTime.getSeconds();
                            case MILLISECOND:
                                return temporalTime.getMilliseconds();
                            case MICROSECOND:
                                return temporalTime.getMicroseconds();
                            case NANOSECOND:
                                return temporalTime.getNanoseconds();
                            default:
                                errorBranch.enter();
                                throw Errors.createTypeErrorTemporalTimeExpected();
                        }
                    } else {
                        errorBranch.enter();
                        throw Errors.createTypeErrorTemporalTimeExpected();
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

        JSObjectUtil.putBuiltinAccessorProperty(prototype, HOUR,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalTimeHour, HOUR), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MINUTE,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalTimeMinute, MINUTE), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, SECOND,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalTimeSecond, SECOND), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MILLISECOND,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalTimeMillisecond, MILLISECOND), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MICROSECOND,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalTimeMicrosecond, MICROSECOND), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, NANOSECOND,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalTimeNanosecond, NANOSECOND), Undefined.instance);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalTimePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, "Temporal.Time");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalTime.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalTimePrototype();
    }

    @Override
    public void fillConstructor(JSRealm realm, DynamicObject constructor) {
        WithFunctionsAndSpecies.super.fillConstructor(realm, constructor);

    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalTimeFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalTime(Object obj) {
        return obj instanceof JSTemporalTimeObject;
    }

    //region Abstract methods
    public static int compareTemporalTime(int h1, int min1, int s1, int ms1, int mus1, int ns1,
                                          int h2, int min2, int s2, int ms2, int mus2, int ns2) {
        if (h1 > h2) {
            return 1;
        }
        if (h1 < h2) {
            return -1;
        }
        if (min1 > min2) {
            return 1;
        }
        if (min1 < min2) {
            return -1;
        }
        if (s1 > s2) {
            return 1;
        }
        if (s1 < s2) {
            return -1;
        }
        if (ms1 > ms2) {
            return 1;
        }
        if (ms1 < ms2) {
            return -1;
        }
        if (mus1 > mus2) {
            return 1;
        }
        if (mus1 < mus2) {
            return -1;
        }
        if (ns1 > ns2) {
            return 1;
        }
        if (ns1 < ns2) {
            return -1;
        }
        return 0;
    }
    //endregion
}
