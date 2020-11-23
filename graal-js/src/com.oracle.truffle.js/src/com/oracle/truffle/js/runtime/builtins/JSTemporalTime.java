package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.TemporalTimeFunctionBuiltins;
import com.oracle.truffle.js.builtins.TemporalTimePrototypeBuiltins;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class JSTemporalTime extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
        PrototypeSupplier {

    public static final JSTemporalTime INSTANCE = new JSTemporalTime();

    public static final String CLASS_NAME = "TemporalTime";
    public static final String PROTOTYPE_NAME = "TemporalTime.prototype";

    public static final String HOUR = "hour";
    public static final String MINUTE = "minute";
    public static final String SECOND = "second";
    public static final String MILLISECOND = "millisecond";
    public static final String MICROSECOND = "microsecond";
    public static final String NANOSECOND = "nanosecond";
    private static final String[] PROPERTIES = new String[]{HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND};

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

    public static boolean validateTime(long hours, long minutes, long seconds, long milliseconds, long microseconds,
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

    // 4.5.2
    public static Object toTemporalTime(DynamicObject item, DynamicObject varConstructor, String varOverflow,
                                        JSRealm realm, IsObjectNode isObject, DynamicObjectLibrary dol,
                                        JSToIntegerAsIntNode toInt, JSToStringNode toString,
                                        IsConstructorNode isConstructor, JSFunctionCallNode callNode) {

        DynamicObject constructor = varConstructor == null ? realm.getTemporalTimeConstructor() : varConstructor;
        String overflow = varOverflow == null ? "constraint" : varOverflow;
        assert overflow.equals("constraint") || overflow.equals("reject");
        DynamicObject result;
        if (isObject.executeBoolean(item)) {
            if (isJSTemporalTime(item)) {
                return item;
            }
            // TODO: 4.c Calendar
            result = toTemporalTimeRecord(item, realm, isObject, dol, toInt);
            result = regulateTime(
                    toInt.executeInt(dol.getOrDefault(result, HOUR, 0)),
                    toInt.executeInt(dol.getOrDefault(result, MINUTE, 0)),
                    toInt.executeInt(dol.getOrDefault(result, SECOND, 0)),
                    toInt.executeInt(dol.getOrDefault(result, MILLISECOND, 0)),
                    toInt.executeInt(dol.getOrDefault(result, MICROSECOND, 0)),
                    toInt.executeInt(dol.getOrDefault(result, NANOSECOND, 0)),
                    overflow, realm
            );
        } else {
            String string = toString.executeString(item);
            result = null;  // TODO: ParseTemporalTimeString
            if (validateTime(
                    toInt.executeInt(dol.getOrDefault(result, HOUR, 0)),
                    toInt.executeInt(dol.getOrDefault(result, MINUTE, 0)),
                    toInt.executeInt(dol.getOrDefault(result, SECOND, 0)),
                    toInt.executeInt(dol.getOrDefault(result, MILLISECOND, 0)),
                    toInt.executeInt(dol.getOrDefault(result, MICROSECOND, 0)),
                    toInt.executeInt(dol.getOrDefault(result, NANOSECOND, 0))
            )) {
                throw Errors.createRangeError("Given time outside the range.");
            }
            Object calendar = dol.getOrDefault(result, "calendar", null);
            if(calendar != null && !calendar.equals("iso8601")) {
                throw Errors.createRangeError("Wrong calendar.");
            }
        }
        return createTemporalTimeFromStatic(
                constructor,
                toInt.executeInt(dol.getOrDefault(result, HOUR, 0)),
                toInt.executeInt(dol.getOrDefault(result, MINUTE, 0)),
                toInt.executeInt(dol.getOrDefault(result, SECOND, 0)),
                toInt.executeInt(dol.getOrDefault(result, MILLISECOND, 0)),
                toInt.executeInt(dol.getOrDefault(result, MICROSECOND, 0)),
                toInt.executeInt(dol.getOrDefault(result, NANOSECOND, 0)),
                isConstructor, callNode
        );

    }

    // 4.5.3
    public static DynamicObject toPartialTime(DynamicObject temporalTimeLike, JSRealm realm, IsObjectNode isObject,
                                              DynamicObjectLibrary dol, JSToIntegerAsIntNode toInt) {
        if (!isObject.executeBoolean(temporalTimeLike)) {
            throw Errors.createTypeError("Temporal.Time like object expected.");
        }
        DynamicObject result = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        boolean any = false;
        for(String property : PROPERTIES) {
            Object value = dol.getOrDefault(temporalTimeLike, property, null);
            if(value != null) {
                any = true;
                value = toInt.executeInt(value);
                JSObjectUtil.putDataProperty(realm.getContext(), result, property, value);
            }
        }
        if (!any) {
            throw Errors.createTypeError("No Temporal.Time property found in given object.");
        }
        return result;
    }

    // 4.5.4
    public static DynamicObject regulateTime(long hours, long minutes, long seconds, long milliseconds, long microseconds,
                                             long nanoseconds, String overflow, JSRealm realm) {
        assert overflow.equals("constraint") || overflow.equals("reject");
        if (overflow.equals("constraint")) {
            return constraintTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds, realm);
        } else {
            if (!validateTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
                throw Errors.createRangeError("Given time outside the range.");
            }

            DynamicObject result = JSObjectUtil.createOrdinaryPrototypeObject(realm);
            JSObjectUtil.putDataProperty(realm.getContext(), result, "days", 0);
            JSObjectUtil.putDataProperty(realm.getContext(), result, HOUR, hours);
            JSObjectUtil.putDataProperty(realm.getContext(), result, MINUTE, minutes);
            JSObjectUtil.putDataProperty(realm.getContext(), result, SECOND, seconds);
            JSObjectUtil.putDataProperty(realm.getContext(), result, MILLISECOND, milliseconds);
            JSObjectUtil.putDataProperty(realm.getContext(), result, MICROSECOND, microseconds);
            JSObjectUtil.putDataProperty(realm.getContext(), result, NANOSECOND, nanoseconds);

            return result;
        }
    }

    // 4.5.7
    public static DynamicObject constraintTime(long hours, long minutes, long seconds, long milliseconds,
                                               long microseconds, long nanoseconds, JSRealm realm) {
        hours = TemporalUtil.constraintToRange(hours, 0, 23);
        minutes = TemporalUtil.constraintToRange(minutes, 0, 59);
        seconds = TemporalUtil.constraintToRange(seconds, 0, 59);
        milliseconds = TemporalUtil.constraintToRange(milliseconds, 0, 999);
        microseconds = TemporalUtil.constraintToRange(microseconds, 0, 999);
        nanoseconds = TemporalUtil.constraintToRange(nanoseconds, 0, 999);

        DynamicObject result = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putDataProperty(realm.getContext(), result, HOUR, hours);
        JSObjectUtil.putDataProperty(realm.getContext(), result, MINUTE, minutes);
        JSObjectUtil.putDataProperty(realm.getContext(), result, SECOND, seconds);
        JSObjectUtil.putDataProperty(realm.getContext(), result, MILLISECOND, milliseconds);
        JSObjectUtil.putDataProperty(realm.getContext(), result, MICROSECOND, microseconds);
        JSObjectUtil.putDataProperty(realm.getContext(), result, NANOSECOND, nanoseconds);

        return result;
    }

    // 4.5.9
    public static JSTemporalTimeObject createTemporalTimeFromInstance(JSTemporalTimeObject temporalTime,
                                                                      long hour, long minute, long second,
                                                                      long millisecond, long microsecond,
                                                                      long nanosecond, JSRealm realm,
                                                                      JSFunctionCallNode callNode) {
        assert validateTime(hour, minute, second, millisecond, microsecond, nanosecond);
        DynamicObject constructor = realm.getTemporalTimeConstructor();
        Object[] ctorArgs = new Object[] {hour, minute, second, millisecond, microsecond, nanosecond};
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, ctorArgs.length);
        System.arraycopy(ctorArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, ctorArgs.length);
        Object result = callNode.executeCall(args);
        return (JSTemporalTimeObject) result;
    }

    // 4.5.10
    public static Object createTemporalTimeFromStatic(DynamicObject constructor, long hours, long minutes,
                                                                    long seconds, long milliseconds, long microseconds,
                                                                    long nanoseconds,
                                                                    IsConstructorNode isConstructor,
                                                                    JSFunctionCallNode callNode) {
        assert validateTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        if (!isConstructor.executeBoolean(constructor)) {
            throw Errors.createTypeError("Given constructor is not an constructor.");
        }
        Object[] ctorArgs = new Object[] {hours, minutes, seconds, milliseconds, microseconds, nanoseconds};
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, ctorArgs.length);
        System.arraycopy(ctorArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, ctorArgs.length);
        Object result = callNode.executeCall(args);
        return result;
    }

    // 4.5.11
    public static DynamicObject toTemporalTimeRecord(DynamicObject temporalTimeLike,
                                              JSRealm realm, IsObjectNode isObject, DynamicObjectLibrary dol,
                                              JSToIntegerAsIntNode toInt) {
        assert isObject.executeBoolean(temporalTimeLike);
        DynamicObject result = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        for(String property : PROPERTIES) {
            Object value = dol.getOrDefault(temporalTimeLike, property, null);
            if (value == null) {
                throw Errors.createTypeError(String.format("Property %s should not be undefined.", property));
            }
            value = toInt.executeInt(value);
            JSObjectUtil.putDataProperty(realm.getContext(), result, property, value);
        }
        return result;
    }

    // 4.5.13
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
