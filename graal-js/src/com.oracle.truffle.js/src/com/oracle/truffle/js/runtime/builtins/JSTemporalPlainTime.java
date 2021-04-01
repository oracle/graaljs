/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.TemporalPlainTimeFunctionBuiltins;
import com.oracle.truffle.js.builtins.TemporalPlainTimePrototypeBuiltins;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
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

public class JSTemporalPlainTime extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
        PrototypeSupplier {

    public static final JSTemporalPlainTime INSTANCE = new JSTemporalPlainTime();

    public static final String CLASS_NAME = "TemporalPlainTime";
    public static final String PROTOTYPE_NAME = "TemporalPlainTime.prototype";

    public static final String HOUR = "hour";
    public static final String MINUTE = "minute";
    public static final String SECOND = "second";
    public static final String MILLISECOND = "millisecond";
    public static final String MICROSECOND = "microsecond";
    public static final String NANOSECOND = "nanosecond";
    private static final String[] PROPERTIES = new String[]{HOUR, MINUTE, SECOND, MILLISECOND, MICROSECOND, NANOSECOND};

    private JSTemporalPlainTime() {
    }

    public static DynamicObject create(JSContext context, long hours, long minutes, long seconds, long milliseconds,
                                       long microseconds, long nanoseconds) {
        if (!validateTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
            throw Errors.createRangeError("Given time outside the range.");
        }
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalTimeFactory();
        DynamicObject obj = factory.initProto(new JSTemporalPlainTimeObject(factory.getShape(realm),
                hours, minutes, seconds, milliseconds, microseconds, nanoseconds
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
        return "Temporal.PlainTime";
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
                    if (JSTemporalPlainTime.isJSTemporalTime(obj)) {
                        JSTemporalPlainTimeObject temporalTime = (JSTemporalPlainTimeObject) obj;
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
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalPlainTimePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, "Temporal.Time");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainTime.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainTimePrototype();
    }

    @Override
    public void fillConstructor(JSRealm realm, DynamicObject constructor) {
        WithFunctionsAndSpecies.super.fillConstructor(realm, constructor);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalPlainTimeFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalTime(Object obj) {
        return obj instanceof JSTemporalPlainTimeObject;
    }

    //region Abstract methods

    // 4.5.1
    public static DynamicObject differenceTime(long h1, long min1, long s1, long ms1, long mus1, long ns1,
                                               long h2, long min2, long s2, long ms2, long mus2, long ns2,
                                               JSRealm realm, DynamicObjectLibrary dol) {
        try {
            long hours = h2 - h1;
            long minutes = min2 - min1;
            long seconds = s2 - s1;
            long milliseconds = ms2 - ms1;
            long microseconds = mus2 - mus1;
            long nanoseconds = ns2 - ns1;
            long sign = JSTemporalDuration.durationSign(0, 0, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
            DynamicObject bt = balanceTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds, realm);
            DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
            JSObjectUtil.putDataProperty(realm.getContext(), record, JSTemporalDuration.DAYS, dol.getLongOrDefault(bt, JSTemporalDuration.DAYS, 0) * sign);
            JSObjectUtil.putDataProperty(realm.getContext(), record, JSTemporalDuration.HOURS, dol.getLongOrDefault(bt, JSTemporalDuration.HOURS, 0) * sign);
            JSObjectUtil.putDataProperty(realm.getContext(), record, JSTemporalDuration.MINUTES, dol.getLongOrDefault(bt, JSTemporalDuration.MINUTES, 0) * sign);
            JSObjectUtil.putDataProperty(realm.getContext(), record, JSTemporalDuration.SECONDS, dol.getLongOrDefault(bt, JSTemporalDuration.SECONDS, 0) * sign);
            JSObjectUtil.putDataProperty(realm.getContext(), record, JSTemporalDuration.MILLISECONDS, dol.getLongOrDefault(bt, JSTemporalDuration.MILLISECONDS, 0) * sign);
            JSObjectUtil.putDataProperty(realm.getContext(), record, JSTemporalDuration.MICROSECONDS, dol.getLongOrDefault(bt, JSTemporalDuration.MICROSECONDS, 0) * sign);
            JSObjectUtil.putDataProperty(realm.getContext(), record, JSTemporalDuration.NANOSECONDS, dol.getLongOrDefault(bt, JSTemporalDuration.NANOSECONDS, 0) * sign);
            return record;
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 4.5.2
    public static Object toTemporalTime(DynamicObject item, DynamicObject varConstructor, String varOverflow,
                                        JSRealm realm, IsObjectNode isObject, DynamicObjectLibrary dol,
                                        JSToIntegerAsLongNode toInt, JSToStringNode toString,
                                        IsConstructorNode isConstructor, JSFunctionCallNode callNode) {
        try {
            DynamicObject constructor = varConstructor == null ? realm.getTemporalPlainTimeConstructor() : varConstructor;
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
                        dol.getLongOrDefault(result, HOUR, 0),
                        dol.getLongOrDefault(result, MINUTE, 0),
                        dol.getLongOrDefault(result, SECOND, 0),
                        dol.getLongOrDefault(result, MILLISECOND, 0),
                        dol.getLongOrDefault(result, MICROSECOND, 0),
                        dol.getLongOrDefault(result, NANOSECOND, 0),
                        overflow, realm
                );
            } else {
                String string = toString.executeString(item);
                result = null;  // TODO: ParseTemporalTimeString
                if (validateTime(
                        dol.getLongOrDefault(result, HOUR, 0),
                        dol.getLongOrDefault(result, MINUTE, 0),
                        dol.getLongOrDefault(result, SECOND, 0),
                        dol.getLongOrDefault(result, MILLISECOND, 0),
                        dol.getLongOrDefault(result, MICROSECOND, 0),
                        dol.getLongOrDefault(result, NANOSECOND, 0)
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
                    dol.getLongOrDefault(result, HOUR, 0),
                    dol.getLongOrDefault(result, MINUTE, 0),
                    dol.getLongOrDefault(result, SECOND, 0),
                    dol.getLongOrDefault(result, MILLISECOND, 0),
                    dol.getLongOrDefault(result, MICROSECOND, 0),
                    dol.getLongOrDefault(result, NANOSECOND, 0),
                    isConstructor, callNode
            );
        } catch (UnexpectedResultException e) {
            throw new RuntimeException(e);
        }
    }

    // 4.5.3
    public static DynamicObject toPartialTime(DynamicObject temporalTimeLike, JSRealm realm, IsObjectNode isObject,
                                              DynamicObjectLibrary dol, JSToIntegerAsLongNode toInt) {
        if (!isObject.executeBoolean(temporalTimeLike)) {
            throw Errors.createTypeError("Temporal.Time like object expected.");
        }
        DynamicObject result = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        boolean any = false;
        for(String property : PROPERTIES) {
            Object value = dol.getOrDefault(temporalTimeLike, property, null);
            if(value != null) {
                any = true;
                value = toInt.executeLong(value);
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

    // 4.5.6
    public static DynamicObject balanceTime(double hours, double minutes, double seconds, double milliseconds,
                                            double microseconds, double nanoseconds, JSRealm realm) {
        if(hours == Double.POSITIVE_INFINITY || hours == Double.NEGATIVE_INFINITY ||
                minutes == Double.POSITIVE_INFINITY || minutes == Double.NEGATIVE_INFINITY ||
                seconds == Double.POSITIVE_INFINITY || seconds == Double.NEGATIVE_INFINITY ||
                milliseconds == Double.POSITIVE_INFINITY || milliseconds == Double.NEGATIVE_INFINITY ||
                microseconds == Double.POSITIVE_INFINITY || microseconds == Double.NEGATIVE_INFINITY ||
                nanoseconds == Double.POSITIVE_INFINITY || nanoseconds == Double.NEGATIVE_INFINITY
        ) {
            throw Errors.createRangeError("Time is infinite");
        }
        microseconds = microseconds + Math.floor(nanoseconds / 1000);
        nanoseconds = TemporalUtil.nonNegativeModulo(nanoseconds, 1000);
        milliseconds = milliseconds + Math.floor(microseconds / 1000);
        microseconds = TemporalUtil.nonNegativeModulo(microseconds, 1000);
        seconds = seconds + Math.floor(milliseconds / 1000);
        milliseconds = TemporalUtil.nonNegativeModulo(milliseconds, 1000);
        minutes = minutes + Math.floor(seconds / 60);
        seconds = TemporalUtil.nonNegativeModulo(seconds, 60);
        hours = hours + Math.floor(minutes / 60);
        minutes = TemporalUtil.nonNegativeModulo(minutes, 60);
        double days = Math.floor(hours / 24);
        hours = TemporalUtil.nonNegativeModulo(hours, 24);
        return createTimeRecord((long)days, (long)hours, (long)minutes, (long)seconds, (long)milliseconds,
                (long)microseconds, (long)nanoseconds, realm);
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
    public static JSTemporalPlainTimeObject createTemporalTimeFromInstance(JSTemporalPlainTimeObject temporalTime,
                                                                           long hour, long minute, long second,
                                                                           long millisecond, long microsecond,
                                                                           long nanosecond, JSRealm realm,
                                                                           JSFunctionCallNode callNode) {
        assert validateTime(hour, minute, second, millisecond, microsecond, nanosecond);
        DynamicObject constructor = realm.getTemporalPlainTimeConstructor();
        Object[] ctorArgs = new Object[] {hour, minute, second, millisecond, microsecond, nanosecond};
        Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, ctorArgs.length);
        System.arraycopy(ctorArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, ctorArgs.length);
        Object result = callNode.executeCall(args);
        return (JSTemporalPlainTimeObject) result;
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
                                              JSToIntegerAsLongNode toInt) {
        assert isObject.executeBoolean(temporalTimeLike);
        DynamicObject result = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        for(String property : PROPERTIES) {
            Object value = dol.getOrDefault(temporalTimeLike, property, null);
            if (value == null) {
                throw Errors.createTypeError(String.format("Property %s should not be undefined.", property));
            }
            value = toInt.executeLong(value);
            JSObjectUtil.putDataProperty(realm.getContext(), result, property, value);
        }
        return result;
    }

    // 4.5.12
    public static String temporalTimeToString(long hour, long minute, long second, long millisecond, long microsecond,
                                              long nanosecond, Object precision) {
        String hourString = String.format("%1$2d", hour).replace(" ", "0");
        String minuteString = String.format("%1$2d", minute).replace(" ", "0");
        String secondString = TemporalUtil.formatSecondsStringPart(second, millisecond, microsecond, nanosecond, precision);
        return String.format("%s:%s:%s", hourString, minuteString, secondString);
    }

    // 4.5.13
    public static int compareTemporalTime(long h1, long min1, long s1, long ms1, long mus1, long ns1,
                                          long h2, long min2, long s2, long ms2, long mus2, long ns2) {
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

    // 4.5.14
    public static DynamicObject addTime(long hour, long minute, long second, long millisecond, long microsecond,
                                        long nanosecond, long hours, long minutes, long seconds, long milliseconds,
                                        long microseconds, long nanoseconds, JSRealm realm) {
        hour = hour + hours;
        minute = minute + minutes;
        second = second + seconds;
        millisecond = millisecond + microseconds;
        microsecond = microsecond + microseconds;
        nanosecond = nanosecond + nanoseconds;
        return balanceTime(hour, minute, second, millisecond, microsecond, nanosecond, realm);
    }

    // 4.5.15
    public static DynamicObject roundTime(long hours, long minutes, long seconds, long milliseconds, long microseconds,
                                          long nanoseconds, double increment, String unit, String roundingMode,
                                          Long dayLengthNs, JSRealm realm) {
        double fractionalSecond = ((double)nanoseconds / 1_000_000_000) + ((double) microseconds / 1_000_000) +
                ((double) milliseconds / 1_000) + seconds;
        double quantity;
        if (unit.equals("day")) {
            dayLengthNs = dayLengthNs == null ? 86_300_000_000_000L : dayLengthNs;
            quantity = ((double) (((((hours * 60 + minutes) * 60 + seconds) * 1000 + milliseconds) * 1000 + microseconds) * 1000 + nanoseconds)) / dayLengthNs;
        } else if (unit.equals("hour")) {
            quantity = (fractionalSecond / 60 + minutes) / 60 + hours;
        } else if (unit.equals("minute")) {
            quantity = fractionalSecond / 60 + minutes;
        } else if (unit.equals("second")) {
            quantity = fractionalSecond;
        } else if (unit.equals("millisecond")) {
            quantity = ((double) nanoseconds / 1_000_000) + ((double) microseconds / 1_000) + milliseconds;
        } else if (unit.equals("microsecond")) {
            quantity = ((double) nanoseconds / 1_000) + microseconds;
        } else {
            assert unit.equals("nanosecond");
            quantity = nanoseconds;
        }
        double result = TemporalUtil.roundNumberToIncrement(quantity, increment, roundingMode);
        if (unit.equals("day")) {
            return createTimeRecord((long)result, 0, 0, 0, 0, 0, 0, realm);
        }
        if (unit.equals("hour")) {
            return balanceTime(result, 0, 0, 0, 0, 0, realm);
        }
        if (unit.equals("minute")) {
            return balanceTime(hours, result, 0, 0, 0, 0, realm);
        }
        if (unit.equals("second")) {
            return balanceTime(hours, minutes, result, 0, 0, 0, realm);
        }
        if (unit.equals("millisecond")) {
            return balanceTime(hours, minutes, seconds, result, 0, 0, realm);
        }
        if (unit.equals("microsecond")) {
            return balanceTime(hours, minutes, seconds, milliseconds, result, 0, realm);
        }
        assert unit.equals("nanosecond");
        return balanceTime(hours, minutes, seconds, milliseconds, microseconds, result, realm);
    }
    //endregion

    private static DynamicObject createTimeRecord(long day, long hour, long minute, long second, long millisecond,
                                                  long microsecond, long nanosecond, JSRealm realm) {
        DynamicObject record = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putDataProperty(realm.getContext(), record, "days", day);
        JSObjectUtil.putDataProperty(realm.getContext(), record, HOUR, hour);
        JSObjectUtil.putDataProperty(realm.getContext(), record, MINUTE, minute);
        JSObjectUtil.putDataProperty(realm.getContext(), record, SECOND, second);
        JSObjectUtil.putDataProperty(realm.getContext(), record, MILLISECOND, millisecond);
        JSObjectUtil.putDataProperty(realm.getContext(), record, MICROSECOND, microsecond);
        JSObjectUtil.putDataProperty(realm.getContext(), record, NANOSECOND, nanosecond);
        return record;
    }
}
