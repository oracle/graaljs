package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.TemporalDurationPrototypeBuiltins;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

import java.util.Set;

public class JSTemporalDuration extends JSNonProxy implements JSConstructorFactory.Default.WithSpecies,
        PrototypeSupplier {

    public static final JSTemporalDuration INSTANCE = new JSTemporalDuration();

    public static final String CLASS_NAME = "TemporalDuration";
    public static final String PROTOTYPE_NAME = "TemporalDuration.prototype";

    public static final String YEARS = "years";
    public static final String MONTHS = "months";
    public static final String WEEKS = "weeks";
    public static final String DAYS = "days";
    public static final String HOURS = "hours";
    public static final String MINUTES = "minutes";
    public static final String SECONDS = "seconds";
    public static final String MILLISECONDS = "milliseconds";
    public static final String MICROSECONDS = "microseconds";
    public static final String NANOSECONDS = "nanoseconds";
    public static final String SIGN = "sign";
    public static final String BLANK = "blank";

    public static final String[] PROPERTIES = new String[]{
            DAYS, HOURS, MICROSECONDS, MILLISECONDS, MINUTES, MONTHS, NANOSECONDS, SECONDS, WEEKS, YEARS
    };

    private JSTemporalDuration() {
    }

    public static DynamicObject create(JSContext context, long years, long months, long weeks, long days, long hours,
                                       long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        if (!validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds,
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

    private static DynamicObject createGetterFunction(JSRealm realm, BuiltinFunctionKey functionKey, String property) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(functionKey, (c) -> {
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(c.getLanguage(), null, null) {
                private final BranchProfile errorBranch = BranchProfile.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object obj = frame.getArguments()[0];
                    if (JSTemporalDuration.isJSTemporalDuration(obj)) {
                        JSTemporalDurationObject temporalDuration = (JSTemporalDurationObject) obj;
                        switch (property) {
                            case YEARS:
                                return temporalDuration.getYears();
                            case MONTHS:
                                return temporalDuration.getMonths();
                            case WEEKS:
                                return temporalDuration.getWeeks();
                            case DAYS:
                                return temporalDuration.getDays();
                            case HOURS:
                                return temporalDuration.getHours();
                            case MINUTES:
                                return temporalDuration.getMinutes();
                            case SECONDS:
                                return temporalDuration.getSeconds();
                            case MILLISECONDS:
                                return temporalDuration.getMilliseconds();
                            case MICROSECONDS:
                                return temporalDuration.getMicroseconds();
                            case NANOSECONDS:
                                return temporalDuration.getNanoseconds();
                            default:
                                errorBranch.enter();
                                throw Errors.createTypeErrorTemporalDurationExpected();
                        }
                    } else {
                        errorBranch.enter();
                        throw Errors.createTypeErrorTemporalDurationExpected();
                    }
                }
            });
            return JSFunctionData.createCallOnly(c, callTarget, 0, "get " + property);
        });
        DynamicObject getter = JSFunction.create(realm, getterData);
        return getter;
    }

    private static DynamicObject createGetSignFunction(JSRealm realm) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(
                BuiltinFunctionKey.TemporalDurationSign, (c) -> {
                    CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(c.getLanguage(), null, null) {
                        private final BranchProfile errorBranch = BranchProfile.create();

                        @Override
                        public Object execute(VirtualFrame frame) {
                            Object obj = frame.getArguments()[0];
                            if (JSTemporalDuration.isJSTemporalDuration(obj)) {
                                JSTemporalDurationObject temporalDuration = (JSTemporalDurationObject) obj;
                                return durationSign(temporalDuration.getYears(), temporalDuration.getMonths(),
                                        temporalDuration.getWeeks(), temporalDuration.getDays(),
                                        temporalDuration.getHours(), temporalDuration.getMinutes(),
                                        temporalDuration.getSeconds(), temporalDuration.getMilliseconds(),
                                        temporalDuration.getMicroseconds(), temporalDuration.getNanoseconds());
                            } else {
                                errorBranch.enter();
                                throw Errors.createTypeErrorTemporalDurationExpected();
                            }
                        }
                    });
                    return JSFunctionData.createCallOnly(c, callTarget, 0, "get sign");
                });
        DynamicObject getter = JSFunction.create(realm, getterData);
        return getter;
    }

    private static DynamicObject createGetBlankFunction(JSRealm realm) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(
                BuiltinFunctionKey.TemporalDurationBlank, (c) -> {
                    CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(c.getLanguage(), null, null) {
                        private final BranchProfile errorBranch = BranchProfile.create();

                        @Override
                        public Object execute(VirtualFrame frame) {
                            Object obj = frame.getArguments()[0];
                            if (JSTemporalDuration.isJSTemporalDuration(obj)) {
                                JSTemporalDurationObject temporalDuration = (JSTemporalDurationObject) obj;
                                int sign = durationSign(temporalDuration.getYears(), temporalDuration.getMonths(),
                                        temporalDuration.getWeeks(), temporalDuration.getDays(),
                                        temporalDuration.getHours(), temporalDuration.getMinutes(),
                                        temporalDuration.getSeconds(), temporalDuration.getMilliseconds(),
                                        temporalDuration.getMicroseconds(), temporalDuration.getNanoseconds());
                                return sign == 0;
                            } else {
                                errorBranch.enter();
                                throw Errors.createTypeErrorTemporalDurationExpected();
                            }
                        }
                    });
                    return JSFunctionData.createCallOnly(c, callTarget, 0, "get blank");
                });
        DynamicObject getter = JSFunction.create(realm, getterData);
        return getter;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, constructor);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, YEARS,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalDurationYears, YEARS), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MONTHS,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalDurationMonths, MONTHS), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, WEEKS,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalDurationWeeks, WEEKS), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, DAYS,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalDurationDays, DAYS), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, HOURS,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalDurationHours, HOURS), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MINUTES,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalDurationMinutes, MINUTES), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, SECONDS,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalDurationSeconds, SECONDS), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MILLISECONDS,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalDurationMilliseconds, MILLISECONDS), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MICROSECONDS,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalDurationMicroseconds, MICROSECONDS), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, NANOSECONDS,
                createGetterFunction(realm, BuiltinFunctionKey.TemporalDurationNanoseconds, NANOSECONDS), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, SIGN,
                createGetSignFunction(realm), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, BLANK,
                createGetBlankFunction(realm), Undefined.instance);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalDurationPrototypeBuiltins.BUILTINS);
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

    public static boolean isJSTemporalDuration(Object obj) {
        return obj instanceof JSTemporalDurationObject;
    }

    //region Abstract methods
    // 7.5.2
    public static DynamicObject toTemporalDurationRecord(DynamicObject temporalDurationLike, JSRealm realm,
                                                         IsObjectNode isObject, JSToIntegerAsLongNode toInt,
                                                         DynamicObjectLibrary dol) {
        assert isObject.executeBoolean(temporalDurationLike);
        if (isJSTemporalDuration(temporalDurationLike)) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) temporalDurationLike;
            DynamicObject result = JSObjectUtil.createOrdinaryPrototypeObject(realm);
            JSObjectUtil.putDataProperty(realm.getContext(), result, YEARS, duration.getYears());
            JSObjectUtil.putDataProperty(realm.getContext(), result, MONTHS, duration.getMonths());
            JSObjectUtil.putDataProperty(realm.getContext(), result, WEEKS, duration.getWeeks());
            JSObjectUtil.putDataProperty(realm.getContext(), result, DAYS, duration.getDays());
            JSObjectUtil.putDataProperty(realm.getContext(), result, HOURS, duration.getHours());
            JSObjectUtil.putDataProperty(realm.getContext(), result, MINUTES, duration.getMinutes());
            JSObjectUtil.putDataProperty(realm.getContext(), result, SECONDS, duration.getSeconds());
            JSObjectUtil.putDataProperty(realm.getContext(), result, MICROSECONDS, duration.getMicroseconds());
            JSObjectUtil.putDataProperty(realm.getContext(), result, MILLISECONDS, duration.getMilliseconds());
            JSObjectUtil.putDataProperty(realm.getContext(), result, NANOSECONDS, duration.getNanoseconds());
        }
        DynamicObject result = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        boolean any = false;
        for (String property : PROPERTIES) {
            Object val = dol.getOrDefault(temporalDurationLike, property, Null.instance);
            if (!val.equals(Null.instance)) {
                any = true;
            }
            val = toInt.executeLong(val);
            JSObjectUtil.putDataProperty(realm.getContext(), result, property, val);
        }
        if (!any) {
            throw Errors.createTypeError("Given duration like object has no duration properties.");
        }
        return result;
    }

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

    // 7.5.6
    public static String defaultTemporalLargestUnit(long years, long months, long weeks, long days, long hours,
                                                    long minutes, long seconds, long milliseconds, long microseconds,
                                                    long nanoseconds) {
        if (years != 0) {
            return YEARS;
        }
        if (months != 0) {
            return MONTHS;
        }
        if (weeks != 0) {
            return WEEKS;
        }
        if (days != 0) {
            return DAYS;
        }
        if (hours != 0) {
            return HOURS;
        }
        if (minutes != 0) {
            return MINUTES;
        }
        if (seconds != 0) {
            return SECONDS;
        }
        if (milliseconds != 0) {
            return MILLISECONDS;
        }
        if (microseconds != 0) {
            return MICROSECONDS;
        }
        return NANOSECONDS;
    }

    // 7.5.7
    public static DynamicObject toPartialDuration(DynamicObject temporalDurationLike, JSRealm realm,
                                                  IsObjectNode isObjectNode, DynamicObjectLibrary dol,
                                                  JSToIntegerAsLongNode toInt) {
        if (!isObjectNode.executeBoolean(temporalDurationLike)) {
            throw Errors.createTypeError("Given duration like is not a object.");
        }
        DynamicObject result = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        boolean any = false;
        for (String property : PROPERTIES) {
            Object value = dol.getOrDefault(temporalDurationLike, property, null);
            if (value != null) {
                any = true;
                JSObjectUtil.putDataProperty(realm.getContext(), result, property, toInt.executeLong(value));
            }
        }
        if (!any) {
            throw Errors.createTypeError("Given duration like object has no duration properties.");
        }
        return result;
    }

    // 7.5.8
    public static DynamicObject createTemporalDuration(long years, long months, long weeks, long days, long hours,
                                                       long minutes, long seconds, long milliseconds, long microseconds,
                                                       long nanoseconds, JSRealm realm) {
        if(!validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
            throw Errors.createRangeError("Duration not valid.");
        }
        DynamicObject obj = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putDataProperty(realm.getContext(), obj, YEARS, years);
        JSObjectUtil.putDataProperty(realm.getContext(), obj, MONTHS, months);
        JSObjectUtil.putDataProperty(realm.getContext(), obj, WEEKS, weeks);
        JSObjectUtil.putDataProperty(realm.getContext(), obj, DAYS, days);
        JSObjectUtil.putDataProperty(realm.getContext(), obj, HOURS, hours);
        JSObjectUtil.putDataProperty(realm.getContext(), obj, MINUTES, minutes);
        JSObjectUtil.putDataProperty(realm.getContext(), obj, SECONDS, seconds);
        JSObjectUtil.putDataProperty(realm.getContext(), obj, MICROSECONDS, microseconds);
        JSObjectUtil.putDataProperty(realm.getContext(), obj, MILLISECONDS, milliseconds);
        JSObjectUtil.putDataProperty(realm.getContext(), obj, NANOSECONDS, nanoseconds);
        return obj;
    }

    // 7.5.9
    public static JSTemporalDurationObject createTemporalDurationFromInstance(JSTemporalDurationObject duration,
                                                                              long years, long months, long weeks, long days,
                                                                              long hours, long minutes, long seconds,
                                                                              long milliseconds, long microseconds,
                                                                              long nanoseconds, JSRealm realm,
                                                                              JSFunctionCallNode callNode) {
        assert validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        DynamicObject constructor = realm.getTemporalDurationConstructor();
        Object[] ctorArgs = new Object[]{years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds};
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, ctorArgs.length);
        System.arraycopy(ctorArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, ctorArgs.length);
        Object result = callNode.executeCall(args);
        return (JSTemporalDurationObject) result;
    }

    // 7.5.12
    public static long totalDurationNanoseconds(long days, long hours, long minutes, long seconds, long milliseconds,
                                                long microseconds, long nanoseconds, long offsetShift) {
        long ns = nanoseconds;
        if (days != 0) {
            ns -= offsetShift;
        }
        long h = hours + days * 24;
        long min = minutes + h * 60;
        long s = seconds + min * 60;
        long ms = milliseconds + s * 1000;
        long mus = microseconds + ms * 1000;
        return  ns + mus * 1000;
    }

    // 7.5.13
    public static DynamicObject balanceDuration(long days, long hours, long minutes, long seconds, long milliseconds,
                                                long microseconds, long nanoseconds, String largestUnit,
                                                DynamicObject relativeTo, JSRealm realm) {
        long ns;
        if(relativeTo != null) {    // TODO: Check if is temporal zoned date time
            ns = nanoseconds;
        } else {
            ns = totalDurationNanoseconds(days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
        }
        long d;
        if (largestUnit.equals(YEARS) || largestUnit.equals(MONTHS) || largestUnit.equals(WEEKS) || largestUnit.equals(DAYS)) {
            d = days;
        } else {
            d = 0;
        }
        long h = 0, min = 0, s = 0, ms = 0, mus = 0;
        long sign = ns < 0 ? -1 : 1;
        ns = Math.abs(ns);
        if (largestUnit.equals(YEARS) || largestUnit.equals(MONTHS) || largestUnit.equals(WEEKS) ||
                largestUnit.equals(DAYS) || largestUnit.equals(HOURS)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
            ms = Math.floorDiv(mus, 1000);
            mus = mus % 1000;
            s = Math.floorDiv(ms, 1000);
            ms = ms % 1000;
            min = Math.floorDiv(s, 60);
            s = s % 60;
            h = Math.floorDiv(min, 60);
            min = min % 60;
        } else if (largestUnit.equals(MINUTES)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
            ms = Math.floorDiv(mus, 1000);
            mus = mus % 1000;
            s = Math.floorDiv(ms, 1000);
            ms = ms % 1000;
            min = Math.floorDiv(s, 60);
            s = s % 60;
        } else if (largestUnit.equals(SECONDS)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
            ms = Math.floorDiv(mus, 1000);
            mus = mus % 1000;
            s = Math.floorDiv(ms, 1000);
            ms = ms % 1000;
        } else if (largestUnit.equals(MILLISECONDS)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
            ms = Math.floorDiv(mus, 1000);
            mus = mus % 1000;
        } else if (largestUnit.equals(MICROSECONDS)) {
            mus = Math.floorDiv(ns, 1000);
            ns = ns % 1000;
        } else {
            assert largestUnit.equals(NANOSECONDS);
        }
        DynamicObject result = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putDataProperty(realm.getContext(), result, DAYS, d);
        JSObjectUtil.putDataProperty(realm.getContext(), result, HOURS, h * sign);
        JSObjectUtil.putDataProperty(realm.getContext(), result, MINUTES, min * sign);
        JSObjectUtil.putDataProperty(realm.getContext(), result, SECONDS, s * sign);
        JSObjectUtil.putDataProperty(realm.getContext(), result, MILLISECONDS, ms * sign);
        JSObjectUtil.putDataProperty(realm.getContext(), result, MICROSECONDS, mus * sign);
        JSObjectUtil.putDataProperty(realm.getContext(), result, NANOSECONDS, ns * sign);
        return result;
    }

    // 7.5.14
    public static DynamicObject unbalanceDurationRelative(long years, long months, long weeks, long days,
                                                          String largestUnit, DynamicObject relativeTo,
                                                          DynamicObjectLibrary dol, JSRealm realm) {
        try {
            if(largestUnit.equals(YEARS) || (years == 0 && months == 0 && weeks == 0 && days == 0)) {
                DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
                JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, years);
                JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, months);
                JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, weeks);
                JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, days);
            }
            long sign = JSTemporalDuration.durationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
            assert sign != 0;
            DynamicObject oneYear = JSTemporalDuration.createTemporalDuration(sign, 0, 0, 0, 0, 0, 0, 0, 0, 0, realm);
            DynamicObject oneMonth = JSTemporalDuration.createTemporalDuration(0, sign, 0, 0, 0, 0, 0, 0, 0, 0, realm);
            DynamicObject oneWeek = JSTemporalDuration.createTemporalDuration(0, 0, sign, 0, 0, 0, 0, 0, 0, 0, realm);
            DynamicObject oneDay = JSTemporalDuration.createTemporalDuration(0, 0, 0, sign, 0, 0, 0, 0, 0, 0, realm);
            DynamicObject calendar = null;
            if (relativeTo != null) {
                assert JSObject.hasProperty(relativeTo, "calendar");
                calendar = (DynamicObject) JSObject.get(relativeTo, "calendar");
            }
            if (largestUnit.equals(MONTHS)) {
                if (calendar == null) {
                    throw Errors.createRangeError("No calender provided.");
                }
                // TODO: Get function dateAdd
                // TODO: Get function dateUntil
                DynamicObject untilOptions = JSObjectUtil.createOrdinaryPrototypeObject(realm);
                JSObjectUtil.putDataProperty(realm.getContext(), untilOptions, "largestUnit", MONTHS);
                while (Math.abs(years) > 0) {
                    DynamicObject newRelativeTo = null; // TODO: Call dateAdd
                    DynamicObject untilResult = null; // TODO: call dateUntil
                    long oneYearMonths = dol.getLongOrDefault(untilResult, MONTHS, 0);
                    relativeTo = newRelativeTo;
                    years = years - sign;
                    months = months + oneYearMonths;
                }
            } else if(largestUnit.equals(WEEKS)) {
                if(calendar == null) {
                    throw Errors.createRangeError("Calendar should be not undefined.");
                }
                while (Math.abs(years) > 0) {
                    DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneYear, dol, realm);
                    relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                    long oneYearDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                    years = years - sign;
                    days = days + oneYearDays;
                }
                while (Math.abs(months) > 0) {
                    DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, dol, realm);
                    relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                    long oneMonthDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                    months = months - sign;
                    days = days + oneMonthDays;
                }
            } else {
                if (years != 0 || months != 0 || days != 0) {
                    if (calendar == null) {
                        throw Errors.createRangeError("Calendar should be not undefined.");
                    }
                    while (Math.abs(years) > 0) {
                        DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneYear, dol, realm);
                        relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                        long oneYearDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                        years = years - sign;
                        days = days + oneYearDays;
                    }
                    while (Math.abs(months) > 0) {
                        DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, dol, realm);
                        relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                        long oneMonthDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                        months = months - sign;
                        days = days + oneMonthDays;
                    }
                    while (Math.abs(weeks) > 0) {
                        DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, dol, realm);
                        relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                        long oneWeekDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                        weeks = weeks - sign;
                        days = days + oneWeekDays;
                    }
                }
            }
            DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
            JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, years);
            JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, months);
            JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, weeks);
            JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, days);
            return record;
        } catch (UnexpectedResultException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 7.5.15
    public static DynamicObject balanceDurationRelative(long years, long months, long weeks, long days, String largestUnit, DynamicObject relativeTo, DynamicObjectLibrary dol, JSRealm realm) {
        try {
            if ((!largestUnit.equals(YEARS) && !largestUnit.equals(MONTHS) && !largestUnit.equals(WEEKS)) || (years == 0 && months == 0 && weeks == 0 && days == 0)) {
                DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
                JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, years);
                JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, months);
                JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, weeks);
                JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, days);
            }
            long sign = durationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);
            assert sign != 0;
            DynamicObject oneYear = createTemporalDuration(sign, 0, 0, 0, 0, 0, 0, 0, 0, 0, realm);
            DynamicObject oneMonth = createTemporalDuration(0, sign, 0, 0, 0, 0, 0, 0, 0, 0, realm);
            DynamicObject oneWeek = createTemporalDuration(0, 0, sign, 0, 0, 0, 0, 0, 0, 0, realm);
            if (relativeTo == null) {
                throw Errors.createRangeError("RelativeTo should not be null.");
            }
            assert dol.containsKey(relativeTo, "calendar");
            DynamicObject calendar = (DynamicObject) dol.getOrDefault(relativeTo, "calendar", null);
            if (largestUnit.equals(YEARS)) {
                DynamicObject dateAdd = null;   // TODO: Get method dateAdd
                DynamicObject addOptions = JSObjectUtil.createOrdinaryPrototypeObject(realm);
                DynamicObject dateUntil = null; // TODO: Get method dateUntil
                DynamicObject untilOptions = JSObjectUtil.createOrdinaryPrototypeObject(realm);
                JSObjectUtil.putDataProperty(realm.getContext(), untilOptions, "largestUnit", MONTHS);
                DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, dol, realm);
                relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                long oneYearDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                while (Math.abs(days) >= Math.abs(oneYearDays)) {
                    days = days - oneYearDays;
                    years = years + sign;
                    moveResult = moveRelativeDate(calendar, relativeTo, oneYear, dol, realm);
                    relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                    oneYearDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                }
                moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, dol, realm);
                relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                long oneMonthDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                while (Math.abs(days) >= Math.abs(oneMonthDays)) {
                    days = days - oneMonthDays;
                    months = months + sign;
                    moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, dol, realm);
                    relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                    oneMonthDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                }
                DynamicObject newRelativeTo = null; // TODO: Call dateAdd
                DynamicObject untilResult = null;   // TODO: Call dateUntil
                long oneYearMonths = dol.getLongOrDefault(untilResult, MONTHS, 0);
                while (Math.abs(months) >= Math.abs((oneYearMonths))) {
                    months = months - oneYearMonths;
                    years = years + sign;
                    relativeTo = newRelativeTo;
                    newRelativeTo = null;   // TODO: Call dateAdd
                    untilResult = null;     // TODO: Call dateUntil
                    oneYearMonths = dol.getLongOrDefault(untilResult, MONTHS, 0);
                }
            } else if (largestUnit.equals(MONTHS)) {
                DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, dol, realm);
                relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                long oneMonthDays = dol.getLongOrDefault(moveResult, DAYS, null);
                while (Math.abs(days) >= Math.abs(oneMonthDays)) {
                    days = days - oneMonthDays;
                    months = months + sign;
                    moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, dol, realm);
                    relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                    oneMonthDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                }
            } else {
                assert largestUnit.equals(WEEKS);
                DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, dol, realm);
                relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                long oneWeekDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                while (Math.abs(days) >= Math.abs(oneWeekDays)) {
                    days = days - oneWeekDays;
                    weeks = weeks + sign;
                    moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, dol, realm);
                    relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                    oneWeekDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                }
            }
            DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
            JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, years);
            JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, months);
            JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, weeks);
            JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, days);
            return record;
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 7.5.16
    public static DynamicObject addDuration(long y1, long mon1, long w1, long d1, long h1, long min1, long s1, long ms1, long mus1, long ns1,
                                            long y2, long mon2, long w2, long d2, long h2, long min2, long s2, long ms2, long mus2, long ns2,
                                            DynamicObject relativeTo, JSRealm realm, DynamicObjectLibrary dol) {
        try {
            String largestUnit1 = defaultTemporalLargestUnit(y1, mon1, w1, d1, h1, min1, s1, ms1, mus1, ns1);
            String largestUnit2 = defaultTemporalLargestUnit(y2, mon2, w2, d2, h2, min2, s2, ms2, mus2, ns2);
            String largestUnit = TemporalUtil.largerOfTwoTemporalDurationUnits(largestUnit1, largestUnit2);
            long years = 0, months = 0, weeks = 0, days = 0, hours = 0, minutes = 0, seconds = 0, milliseconds = 0, microseconds = 0, nanoseconds = 0;
            if (relativeTo == null) {
                if (largestUnit.equals(YEARS) || largestUnit.equals(MONTHS) || largestUnit.equals(WEEKS)) {
                    throw Errors.createRangeError("Largest unit allowed with no relative is 'days'.");
                }
                DynamicObject result = balanceDuration(d1 + d2, h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2,
                        ns1 + ns2, largestUnit, null, realm);
                years = 0;
                months = 0;
                weeks = 0;
                days = dol.getLongOrDefault(result, DAYS, 0L);
                hours = dol.getLongOrDefault(result, HOURS, 0L);
                minutes = dol.getLongOrDefault(result, MINUTES, 0L);
                seconds = dol.getLongOrDefault(result, SECONDS, 0L);
                milliseconds = dol.getLongOrDefault(result, MILLISECONDS, 0L);
                microseconds = dol.getLongOrDefault(result, MICROSECONDS, 0L);
                nanoseconds = dol.getLongOrDefault(result, NANOSECONDS, 0L);
            } else if (JSTemporalPlainDate.isJSTemporalPlainDate(relativeTo)) {
                // TODO: Get calendar
                DynamicObject datePart = JSTemporalPlainDate.createTemporalDate(realm.getContext(),
                        dol.getLongOrDefault(relativeTo, "ISOYear", 0L),
                        dol.getLongOrDefault(relativeTo, "ISOMonth", 0L),
                        dol.getLongOrDefault(relativeTo, "ISODay", 0L));
                DynamicObject dateDuration1 = JSTemporalDuration.createTemporalDuration(y1, mon1, w1, d1, 0, 0, 0, 0, 0, 0, realm);
                DynamicObject dateDuration2 = JSTemporalDuration.createTemporalDuration(y2, mon2, w2, d2, 0, 0, 0, 0, 0, 0, realm);
                // TODO: Get dateAdd function of calendar
                // TODO: Call dateAdd function
                // TODO: Call dateAdd function
                String dateLargestUnit = TemporalUtil.largerOfTwoTemporalDurationUnits("days", largestUnit);
                // TODO: Get dateUntil function of calendar
                DynamicObject differenceOptions = JSObjectUtil.createOrdinaryPrototypeObject(realm);
                JSObjectUtil.putDataProperty(realm.getContext(), differenceOptions, "largestUnit", dateLargestUnit);
                DynamicObject dateDifference = null; // TODO: Call dateUntil function
                DynamicObject result = JSTemporalDuration.balanceDuration(dol.getLongOrDefault(dateDifference, DAYS, 0L),
                        h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2, ns1 + ns2, largestUnit, null, realm);
                years = dol.getLongOrDefault(dateDifference, YEARS, 0L);
                months = dol.getLongOrDefault(dateDifference, MONTHS, 0L);
                weeks = dol.getLongOrDefault(dateDifference, WEEKS, 0L);
                days = dol.getLongOrDefault(result, DAYS, 0L);
                hours = dol.getLongOrDefault(result, HOURS, 0L);
                minutes = dol.getLongOrDefault(result, MINUTES, 0L);
                seconds = dol.getLongOrDefault(result, SECONDS, 0L);
                milliseconds = dol.getLongOrDefault(result, MILLISECONDS, 0L);
                microseconds = dol.getLongOrDefault(result, MICROSECONDS, 0L);
                nanoseconds = dol.getLongOrDefault(result, NANOSECONDS, 0L);
            } else {
                // TODO: Handle ZonedDateTime
            }
            if(!validateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds,
                    microseconds, nanoseconds)) {
                throw Errors.createRangeError("Duration out of range!");
            }
            DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
            JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, years);
            JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, months);
            JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, weeks);
            JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, days);
            JSObjectUtil.putDataProperty(realm.getContext(), record, HOURS, hours);
            JSObjectUtil.putDataProperty(realm.getContext(), record, MINUTES, minutes);
            JSObjectUtil.putDataProperty(realm.getContext(), record, SECONDS, seconds);
            JSObjectUtil.putDataProperty(realm.getContext(), record, MICROSECONDS, microseconds);
            JSObjectUtil.putDataProperty(realm.getContext(), record, MILLISECONDS, milliseconds);
            JSObjectUtil.putDataProperty(realm.getContext(), record, NANOSECONDS, nanoseconds);
            return record;
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 7.5.17
    public static long daysUntil(DynamicObject earlier, DynamicObject later, DynamicObjectLibrary dol) {
        try {
            assert dol.containsKey(earlier, "ISOYear") && dol.containsKey(later, "ISOYear") &&
                    dol.containsKey(earlier, "ISOMonth") && dol.containsKey(later, "ISOMonth") &&
                    dol.containsKey(earlier, "ISODay") && dol.containsKey(later, "ISODay");
            DynamicObject difference = null;    // TODO: Call differenceDate.
            return dol.getLongOrDefault(difference, DAYS, 0);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 7.5.18
    public static DynamicObject moveRelativeDate(DynamicObject calendar, DynamicObject relativeTo, DynamicObject duration,
                                                 DynamicObjectLibrary dol, JSRealm realm) {
        DynamicObject options = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        DynamicObject later = null; // TODO: Invoke dateAdd on calendar
        long days = daysUntil(relativeTo, later, dol);
        DynamicObject dateTime = null; // TODO: Call createTemporalDateTime
        DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putDataProperty(realm.getContext(), record, "relativeTo", dateTime);
        JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, days);
        return record;
    }

    // 7.5.20
    public static DynamicObject roundDuration(long years, long months, long weeks, long days, long hours, long minutes,
                                              long seconds, long milliseconds, long microseconds, long nanoseconds,
                                              long increment, String unit, String roundingMode, DynamicObject relativeTo,
                                              DynamicObjectLibrary dol, JSRealm realm) {
        try {
            if ((unit.equals(YEARS) || unit.equals(MONTHS) || unit.equals(WEEKS)) && relativeTo == null) {
                throw Errors.createRangeError(String.format("RelativeTo object should be not undefined if unit is %s.", unit));
            }
            DynamicObject zonedRelativeTo = null;
            DynamicObject calendar = null;
            DynamicObject options = null;
            double fractionalSeconds = 0;
            if (relativeTo != null) {
                // TODO: Check if relativeTo has InitializedTemporalZonedDateTime
                calendar = (DynamicObject) dol.getOrDefault(relativeTo, "calendar", null);
                // TODO: Get dateAdd method from calendar
                options = JSObjectUtil.createOrdinaryPrototypeObject(realm);
            }
            if (unit.equals(YEARS) || unit.equals(MONTHS) || unit.equals(WEEKS) || unit.equals(DAYS)) {
                nanoseconds = totalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
                DynamicObject intermediate = null;
                if (zonedRelativeTo != null) {
                    // TODO: intermediate = moveRelativeZonedDateTime
                }
                DynamicObject result = null; // TODO: Call nanosecondsToDays
                days = days + dol.getLongOrDefault(result, DAYS, 0) +
                        (dol.getLongOrDefault(result, NANOSECONDS, 0) /
                                Math.abs(dol.getLongOrDefault(result, "dayLength", 1)));
                hours = 0; minutes = 0; seconds = 0; milliseconds = 0; microseconds = 0; nanoseconds = 0;
            } else {
                fractionalSeconds = (nanoseconds * 0.000_000_000_1) + (microseconds * 0.000_000_1) + (milliseconds * 0.000_1);
            }
            double remainder = 0;
            if (unit.equals(YEARS)) {
                DynamicObject yearsDuration = createTemporalDuration(years, 0, 0, 0, 0, 0, 0, 0, 0, 0, realm);
                DynamicObject yearsLater = null; // TODO: Call dateAdd
                DynamicObject yearsMonthsWeeks = createTemporalDuration(years, months, weeks, 0, 0, 0, 0, 0, 0, 0, realm);
                DynamicObject yearsMonthsWeeksLater = null; // TODO: Call dateAdd
                long monthsWeeksInDays = daysUntil(yearsLater, yearsMonthsWeeksLater, dol);
                relativeTo = yearsLater;
                days = days + monthsWeeksInDays;
                long sign = TemporalUtil.sign(days);
                if (sign == 0) {
                    sign = 1;
                }
                DynamicObject oneYear = createTemporalDuration(sign, 0, 0, 0, 0, 0, 0, 0, 0, 0, realm);
                DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneYear, dol, realm);
                relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                long oneYearDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                while (Math.abs(days) >= Math.abs(oneYearDays)) {
                    years = years + sign;
                    days = days - oneYearDays;
                    moveResult = moveRelativeDate(calendar, relativeTo, oneYear, dol, realm);
                    relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                    oneYearDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                }
                double fractionalYears = years + ((double) days / Math.abs(oneYearDays));
                years = (long) TemporalUtil.roundNumberToIncrement(fractionalYears, increment, roundingMode);
                remainder = fractionalYears - years;
                months = 0; weeks = 0; years = 0;
            } else if (unit.equals(MONTHS)) {
                DynamicObject yearsMonths = createTemporalDuration(years, months, 0, 0, 0, 0, 0, 0, 0, 0, realm);
                DynamicObject yearsMonthsLater = null; // TODO: Call dateAdd
                DynamicObject yearsMonthsWeeks = createTemporalDuration(years, months, weeks, 0, 0, 0, 0, 0, 0, 0, realm);
                DynamicObject yearsMonthsWeeksLater = null; // TODO: Call dateAdd
                long weeksInDays = daysUntil(yearsMonthsLater, yearsMonthsWeeksLater, dol);
                relativeTo = yearsMonthsLater;
                days = days + weeksInDays;
                long sign = TemporalUtil.sign(days);
                if (sign == 0) {
                    sign = 1;
                }
                DynamicObject oneMonth = createTemporalDuration(0, sign, 0, 0, 0, 0, 0, 0, 0, 0, realm);
                DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, dol, realm);
                relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                long oneMonthDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                while (Math.abs(days) >= Math.abs(oneMonthDays)) {
                    months = months + sign;
                    days = days - oneMonthDays;
                    moveResult = moveRelativeDate(calendar, relativeTo, oneMonth, dol, realm);
                    relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                    oneMonthDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                }
                double fractionalMonths = months + ((double)days / Math.abs(oneMonthDays));
                months = (long) TemporalUtil.roundNumberToIncrement(fractionalMonths, increment, roundingMode);
                remainder = fractionalMonths - months;
                weeks = 0; days = 0;
            } else if (unit.equals(WEEKS)) {
                long sign = TemporalUtil.sign(days);
                if (sign == 0) {
                    sign = 1;
                }
                DynamicObject oneWeek = createTemporalDuration(0, 0, sign, 0, 0, 0, 0, 0, 0, 0, realm);
                DynamicObject moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, dol, realm);
                relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                long oneWeekDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                while (Math.abs(days) >= Math.abs(oneWeekDays)) {
                    weeks = weeks - sign;
                    days = days - oneWeekDays;
                    moveResult = moveRelativeDate(calendar, relativeTo, oneWeek, dol, realm);
                    relativeTo = (DynamicObject) dol.getOrDefault(moveResult, "relativeTo", null);
                    oneWeekDays = dol.getLongOrDefault(moveResult, DAYS, 0);
                }
                double fractionalWeeks = weeks + ((double) days / Math.abs(oneWeekDays));
                weeks = (long) TemporalUtil.roundNumberToIncrement(fractionalWeeks, increment, roundingMode);
                remainder = fractionalWeeks - weeks;
                days = 0;
            } else if (unit.equals(DAYS)) {
                double fractionalDays = days;
                days = (long) TemporalUtil.roundNumberToIncrement(fractionalDays, increment, roundingMode);
                remainder = fractionalDays - days;
            } else if (unit.equals(HOURS)) {
                double fractionalHours = (((fractionalSeconds / 60) + minutes) / 60) + hours;
                hours = (long) TemporalUtil.roundNumberToIncrement(fractionalHours, increment, roundingMode);
                remainder = fractionalHours - hours;
                minutes = 0; seconds = 0; milliseconds = 0; microseconds = 0; nanoseconds = 0;
            } else if (unit.equals(MINUTES)) {
                double fractionalMinutes = (fractionalSeconds / 60) + minutes;
                minutes = (long) TemporalUtil.roundNumberToIncrement(fractionalMinutes, increment, roundingMode);
                remainder = fractionalMinutes - minutes;
                seconds = 0; milliseconds = 0; microseconds = 0; nanoseconds = 0;
            } else if (unit.equals(SECONDS)) {
                seconds = (long) TemporalUtil.roundNumberToIncrement(fractionalSeconds, increment, roundingMode);
                remainder = fractionalSeconds - seconds;
                milliseconds = 0; microseconds = 0; nanoseconds = 0;
            } else if (unit.equals(MILLISECONDS)) {
                double fractionalMilliseconds = (nanoseconds * 0.000_000_1) + (microseconds * 0.000_1) + milliseconds;
                milliseconds = (long) TemporalUtil.roundNumberToIncrement(fractionalMilliseconds, increment, roundingMode);
                remainder = fractionalMilliseconds - milliseconds;
                microseconds = 0; nanoseconds = 0;
            } else if (unit.equals(MICROSECONDS)) {
                double fractionalMicroseconds = (nanoseconds * 0.000_1) + microseconds;
                microseconds = (long) TemporalUtil.roundNumberToIncrement(fractionalMicroseconds, increment, roundingMode);
                remainder = fractionalMicroseconds - microseconds;
                nanoseconds = 0;
            } else {
                assert unit.equals(NANOSECONDS);
                remainder = nanoseconds;
                nanoseconds = (long) TemporalUtil.roundNumberToIncrement(nanoseconds, increment, roundingMode);
                remainder = remainder - nanoseconds;
            }

            DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
            JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, years);
            JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, months);
            JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, weeks);
            JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, days);
            JSObjectUtil.putDataProperty(realm.getContext(), record, HOURS, hours);
            JSObjectUtil.putDataProperty(realm.getContext(), record, MINUTES, minutes);
            JSObjectUtil.putDataProperty(realm.getContext(), record, SECONDS, seconds);
            JSObjectUtil.putDataProperty(realm.getContext(), record, MILLISECONDS, milliseconds);
            JSObjectUtil.putDataProperty(realm.getContext(), record, MICROSECONDS, microseconds);
            JSObjectUtil.putDataProperty(realm.getContext(), record, NANOSECONDS, nanoseconds);
            JSObjectUtil.putDataProperty(realm.getContext(), record, "remainder", remainder);
            return record;
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 7.5.21
    public static DynamicObject adjustRoundedDurationDays(long years, long months, long weeks, long days, long hours,
                                                          long minutes, long seconds, long milliseconds, long microseconds,
                                                          long nanoseconds, long increment, String unit,
                                                          String roundingMode, DynamicObject relativeTo,
                                                          DynamicObjectLibrary dol, JSRealm realm) {
        try {
            if (unit.equals(YEARS) || unit.equals(MONTHS) || unit.equals(WEEKS) || unit.equals(DAYS) || // TODO: CHeck for InitializedTemporalZonedDateTime internal slot
                    (unit.equals(NANOSECONDS) && increment == 1)) {
                DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
                JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, years);
                JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, months);
                JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, weeks);
                JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, days);
                JSObjectUtil.putDataProperty(realm.getContext(), record, HOURS, hours);
                JSObjectUtil.putDataProperty(realm.getContext(), record, MINUTES, minutes);
                JSObjectUtil.putDataProperty(realm.getContext(), record, SECONDS, seconds);
                JSObjectUtil.putDataProperty(realm.getContext(), record, MILLISECONDS, milliseconds);
                JSObjectUtil.putDataProperty(realm.getContext(), record, MICROSECONDS, microseconds);
                JSObjectUtil.putDataProperty(realm.getContext(), record, NANOSECONDS, nanoseconds);
                return record;
            }
            long timeRemainderNs = totalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0);
            long direction = TemporalUtil.sign(timeRemainderNs);
            long dayStart = 0; // TODO: Call addZonedDateTime
            long dayEnd = 0; // TODO: Call addZonedDateTime
            long dayLengthNs = dayEnd - dayStart;
            if ((timeRemainderNs - dayLengthNs) * direction < 0) {
                DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
                JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, years);
                JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, months);
                JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, weeks);
                JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, days);
                JSObjectUtil.putDataProperty(realm.getContext(), record, HOURS, hours);
                JSObjectUtil.putDataProperty(realm.getContext(), record, MINUTES, minutes);
                JSObjectUtil.putDataProperty(realm.getContext(), record, SECONDS, seconds);
                JSObjectUtil.putDataProperty(realm.getContext(), record, MILLISECONDS, milliseconds);
                JSObjectUtil.putDataProperty(realm.getContext(), record, MICROSECONDS, microseconds);
                JSObjectUtil.putDataProperty(realm.getContext(), record, NANOSECONDS, nanoseconds);
                return record;
            }
            timeRemainderNs = 0; // TODO: Call roundTemporalInstant
            DynamicObject adjustedDateDuration = addDuration(years, months, weeks, days, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, direction, 0, 0, 0, 0, 0, 0,
                    relativeTo, realm, dol);
            DynamicObject adjustedTimeDuration = balanceDuration(0, 0, 0, 0, 0, 0, timeRemainderNs, "hours", null, realm);
            DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
            JSObjectUtil.putDataProperty(realm.getContext(), record, YEARS, dol.getLongOrDefault(adjustedDateDuration, YEARS, 0));
            JSObjectUtil.putDataProperty(realm.getContext(), record, MONTHS, dol.getLongOrDefault(adjustedDateDuration, MONTHS, 0));
            JSObjectUtil.putDataProperty(realm.getContext(), record, WEEKS, dol.getLongOrDefault(adjustedDateDuration, WEEKS, 0));
            JSObjectUtil.putDataProperty(realm.getContext(), record, DAYS, dol.getLongOrDefault(adjustedDateDuration, DAYS, 0));
            JSObjectUtil.putDataProperty(realm.getContext(), record, HOURS, dol.getLongOrDefault(adjustedTimeDuration, HOURS, 0));
            JSObjectUtil.putDataProperty(realm.getContext(), record, MINUTES, dol.getLongOrDefault(adjustedTimeDuration, MINUTES, 0));
            JSObjectUtil.putDataProperty(realm.getContext(), record, SECONDS, dol.getLongOrDefault(adjustedTimeDuration, SECONDS, 0));
            JSObjectUtil.putDataProperty(realm.getContext(), record, MILLISECONDS, dol.getLongOrDefault(adjustedTimeDuration, MILLISECONDS, 0));
            JSObjectUtil.putDataProperty(realm.getContext(), record, MICROSECONDS, dol.getLongOrDefault(adjustedTimeDuration, MICROSECONDS, 0));
            JSObjectUtil.putDataProperty(realm.getContext(), record, NANOSECONDS, dol.getLongOrDefault(adjustedTimeDuration, NANOSECONDS, 0));
            return record;
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 7.5.22
    public static DynamicObject toLimitedTemporalDuration(DynamicObject temporalDurationLike,
                                                          Set<String> disallowedFields, JSRealm realm,
                                                          IsObjectNode isObject, JSToStringNode toString,
                                                          JSToIntegerAsLongNode toInt, DynamicObjectLibrary dol) {
        DynamicObject duration;
        if (!isObject.executeBoolean(temporalDurationLike)) {
            String str = toString.executeString(temporalDurationLike);
            duration = null; // TODO: parse duration of string
        } else {
            duration = toTemporalDurationRecord(temporalDurationLike, realm, isObject, toInt, dol);
        }
        try {
            if (!validateTemporalDuration(
                    dol.getLongOrDefault(duration, YEARS, 0L),
                    dol.getLongOrDefault(duration, MONTHS, 0L),
                    dol.getLongOrDefault(duration, WEEKS, 0L),
                    dol.getLongOrDefault(duration, DAYS, 0L),
                    dol.getLongOrDefault(duration, HOURS, 0L),
                    dol.getLongOrDefault(duration, MINUTES, 0L),
                    dol.getLongOrDefault(duration, SECONDS, 0L),
                    dol.getLongOrDefault(duration, MILLISECONDS, 0L),
                    dol.getLongOrDefault(duration, MICROSECONDS, 0L),
                    dol.getLongOrDefault(duration, NANOSECONDS, 0L)
            )) {
                throw Errors.createRangeError("Given duration outside range.");
            }

            for (String property : PROPERTIES) {
                long value = dol.getLongOrDefault(duration, property, 0L);
                if (value > 0 && disallowedFields.contains(property)) {
                    throw Errors.createRangeError(
                            String.format("Property %s is a disallowed field and not 0.", property));
                }
            }
            return duration;
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 7.5.23
    public static String temporalDurationToString(JSTemporalDurationObject duration) {
        long years = duration.getYears();
        long months = duration.getMonths();
        long weeks = duration.getWeeks();
        long days = duration.getDays();
        long hours = duration.getHours();
        long minutes = duration.getMinutes();
        long seconds = duration.getSeconds();
        long milliseconds = duration.getMilliseconds();
        long microseconds = duration.getMicroseconds();
        long nanoseconds = duration.getNanoseconds();
        int sign = durationSign(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        microseconds += nanoseconds / 1000;
        nanoseconds = nanoseconds % 1000;
        milliseconds += microseconds / 1000;
        microseconds = microseconds % 1000;
        seconds += milliseconds / 1000;
        milliseconds = milliseconds % 1000;
        if (years == 0 && months == 0 && weeks == 0 && days == 0 && hours == 0 && minutes == 0 && seconds == 0
                && milliseconds == 0 && microseconds == 0 && nanoseconds == 0) {
            return "PT0S";
        }
        StringBuilder datePart = new StringBuilder();
        if (years != 0) {
            datePart.append(Math.abs(years));
            datePart.append("Y");
        }
        if (months != 0) {
            datePart.append(Math.abs(months));
            datePart.append("M");
        }
        if (weeks != 0) {
            datePart.append(Math.abs(weeks));
            datePart.append("W");
        }
        if (days != 0) {
            datePart.append(Math.abs(days));
            datePart.append("D");
        }
        StringBuilder timePart = new StringBuilder();
        if (hours != 0) {
            timePart.append(Math.abs(hours));
            timePart.append("H");
        }
        if (minutes != 0) {
            timePart.append(Math.abs(minutes));
            timePart.append("M");
        }
        if (seconds != 0 || milliseconds != 0 || microseconds != 0 || nanoseconds != 0) {
            String nanosecondPart = "", microsecondPart = "", millisecondPart = "";
            if (nanoseconds != 0) {
                nanosecondPart = String.format("%1$3d", Math.abs(nanoseconds)).replace(" ", "0");
                microsecondPart = "000";
                millisecondPart = "000";
            }
            if (microseconds != 0) {
                microsecondPart = String.format("%1$3d", Math.abs(microseconds)).replace(" ", "0");
                millisecondPart = "000";
            }
            if (milliseconds != 0) {
                millisecondPart = String.format("%1$3d", Math.abs(milliseconds)).replace(" ", "0");
            }
            String decimalPart = millisecondPart + microsecondPart + nanosecondPart;
            String secondsPart = String.format("%d", Math.abs(seconds));
            if (!decimalPart.equals("")) {
                secondsPart += "." + decimalPart;
            }
            timePart.append(secondsPart);
            timePart.append("S");
        }
        String signPart = sign < 0 ? "-" : "";
        StringBuilder result = new StringBuilder();
        result.append(signPart).append("P").append(datePart);
        if (!timePart.toString().equals("")) {
            result.append("T").append(timePart);
        }
        return result.toString();
    }
    //endregion
}
