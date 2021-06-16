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
package com.oracle.truffle.js.runtime.builtins.temporal;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimeFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltins;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public final class JSTemporalPlainTime extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalPlainTime INSTANCE = new JSTemporalPlainTime();

    public static final String CLASS_NAME = "PlainTime";
    public static final String PROTOTYPE_NAME = "PlainTime.prototype";

    private JSTemporalPlainTime() {
    }

    public static DynamicObject create(JSContext context, long hours, long minutes, long seconds, long milliseconds,
                    long microseconds, long nanoseconds) {
        if (!TemporalUtil.isValidTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
            throw TemporalErrors.createRangeErrorTimeOutsideRange();
        }
        DynamicObject calendar = TemporalUtil.getISO8601Calendar(context);
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalPlainTimeFactory();
        DynamicObject obj = factory.initProto(new JSTemporalPlainTimeObject(factory.getShape(realm),
                        hours, minutes, seconds, milliseconds, microseconds, nanoseconds, calendar), realm);
        return context.trackAllocation(obj);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.PlainTime";
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
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalPlainTimePrototypeBuiltins.BUILTINS);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, CALENDAR, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, CALENDAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, HOUR, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, HOUR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MINUTE, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, MINUTE));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, SECOND, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, SECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MILLISECOND, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, MILLISECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, MICROSECOND, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, MICROSECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, NANOSECOND, realm.lookupAccessor(TemporalPlainTimePrototypeBuiltins.BUILTINS, NANOSECOND));

        JSObjectUtil.putToStringTag(prototype, "Temporal.PlainTime");

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

    public static boolean isJSTemporalPlainTime(Object obj) {
        return obj instanceof JSTemporalPlainTimeObject;
    }

    // region Abstract methods

    // 4.5.2
    public static DynamicObject toTemporalTime(Object item, String overflowParam, JSContext ctx, IsObjectNode isObject, JSToStringNode toString) {
        String overflow = overflowParam == null ? TemporalConstants.CONSTRAIN : overflowParam;
        assert overflow.equals(TemporalConstants.CONSTRAIN) || overflow.equals(TemporalConstants.REJECT);
        JSTemporalDurationRecord result2 = null;
        if (isObject.executeBoolean(item)) {
            if (isJSTemporalPlainTime(item)) {
                return (DynamicObject) item;
            } else if (TemporalUtil.isTemporalZonedDateTime(item)) {
                JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) item;
                JSTemporalInstantObject instant = TemporalUtil.createTemporalInstant(ctx, zdt.getNanoseconds());
                JSTemporalPlainDateTimeObject plainDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, zdt.getTimeZone(), instant, zdt.getCalendar());
                return TemporalUtil.createTemporalTime(ctx, plainDateTime.getHours(), plainDateTime.getMinutes(),
                                plainDateTime.getSeconds(), plainDateTime.getMilliseconds(), plainDateTime.getMicroseconds(), plainDateTime.getNanoseconds());
            } else if (JSTemporalPlainDateTime.isJSTemporalPlainDateTime(item)) {
                TemporalDateTime dt = (TemporalDateTime) item;
                return TemporalUtil.createTemporalTime(ctx, dt.getHours(), dt.getMinutes(), dt.getSeconds(), dt.getMilliseconds(), dt.getMicroseconds(), dt.getNanoseconds());
            }
            DynamicObject calendar = TemporalUtil.getTemporalCalendarWithISODefault(ctx, item);
            if (!JSRuntime.toString(calendar).equals(TemporalConstants.ISO8601)) {
                throw TemporalErrors.createTypeErrorTemporalISO8601Expected();
            }
            JSTemporalDateTimeRecord result = TemporalUtil.toTemporalTimeRecord((DynamicObject) item);
            result2 = TemporalUtil.regulateTime(
                            result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                            result.getMicrosecond(), result.getNanosecond(),
                            overflow);
        } else {
            String string = toString.executeString(item);
            JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalTimeString(ctx, string);
            assert TemporalUtil.isValidTime(
                            result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(),
                            result.getMicrosecond(), result.getNanosecond());
            if (result.hasCalendar() && !JSRuntime.toString(result.getCalendar()).equals(TemporalConstants.ISO8601)) {
                throw TemporalErrors.createTypeErrorTemporalISO8601Expected();
            }
            result2 = JSTemporalDurationRecord.create(result);
        }
        return create(ctx, result2.getHours(), result2.getMinutes(), result2.getSeconds(), result2.getMilliseconds(),
                        result2.getMicroseconds(), result2.getNanoseconds());
    }

    // 4.5.3
    public static DynamicObject toPartialTime(DynamicObject temporalTimeLike, IsObjectNode isObject, JSToIntegerAsLongNode toInt, JSContext ctx) {
        if (!isObject.executeBoolean(temporalTimeLike)) {
            throw TemporalErrors.createTypeErrorTemporalTimeExpected();
        }
        DynamicObject result = JSOrdinary.create(ctx);
        boolean any = false;
        for (String property : TemporalUtil.TIME_LIKE_PROPERTIES) {
            Object value = JSObject.get(temporalTimeLike, property);
            if (value != Undefined.instance) {
                any = true;
                value = toInt.executeLong(value);
                JSObjectUtil.putDataProperty(ctx, result, property, value);
            }
        }
        if (!any) {
            throw TemporalErrors.createTypeErrorTemporalTimePropertyExpected();
        }
        return result;
    }

    // 4.5.12
    @TruffleBoundary
    public static String temporalTimeToString(long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond, Object precision) {
        String hourString = String.format("%1$2d", hour).replace(" ", "0");
        String minuteString = String.format("%1$2d", minute).replace(" ", "0");
        String secondString = TemporalUtil.formatSecondsStringPart(second, millisecond, microsecond, nanosecond, precision);
        return String.format("%s:%s%s", hourString, minuteString, secondString);
    }

    // endregion
}
