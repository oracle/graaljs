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

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimeFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltins;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public final class JSTemporalZonedDateTime extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies,
                PrototypeSupplier {

    public static final JSTemporalZonedDateTime INSTANCE = new JSTemporalZonedDateTime();

    public static final String CLASS_NAME = "ZonedDateTime";
    public static final String PROTOTYPE_NAME = "ZonedDateTime.prototype";

    private JSTemporalZonedDateTime() {
    }

    public static DynamicObject create(JSContext context, BigInt nanoseconds, DynamicObject timeZone, DynamicObject calendar) {
        assert TemporalUtil.isValidEpochNanoseconds(nanoseconds);
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getTemporalZonedDateTimeFactory();
        DynamicObject obj = factory.initProto(new JSTemporalZonedDateTimeObject(factory.getShape(realm), nanoseconds, timeZone, calendar), realm);
        return context.trackAllocation(obj);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return "Temporal.ZonedDateTime";
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
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalZonedDateTimePrototypeBuiltins.BUILTINS);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.CALENDAR, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.CALENDAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.TIME_ZONE, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.TIME_ZONE));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.YEAR, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.MONTH, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.MONTH));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.MONTH_CODE, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.MONTH_CODE));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.DAY, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.DAY));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.HOUR, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.HOUR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.MINUTE, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.MINUTE));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.SECOND, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.SECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.MILLISECOND, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.MILLISECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.MICROSECOND, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.MICROSECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.NANOSECOND, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.NANOSECOND));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.EPOCH_SECONDS, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.EPOCH_SECONDS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.EPOCH_MILLISECONDS,
                        realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.EPOCH_MILLISECONDS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.EPOCH_MICROSECONDS,
                        realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.EPOCH_MICROSECONDS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.EPOCH_NANOSECONDS,
                        realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.EPOCH_NANOSECONDS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.DAY_OF_WEEK, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.DAY_OF_WEEK));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.DAY_OF_YEAR, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.DAY_OF_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.WEEK_OF_YEAR, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.WEEK_OF_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.HOURS_IN_DAY, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.HOURS_IN_DAY));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.DAYS_IN_WEEK, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.DAYS_IN_WEEK));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.DAYS_IN_MONTH, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.DAYS_IN_MONTH));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.MONTHS_IN_YEAR, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.MONTHS_IN_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.IN_LEAP_YEAR, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.IN_LEAP_YEAR));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.OFFSET_NANOSECONDS,
                        realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.OFFSET_NANOSECONDS));
        JSObjectUtil.putBuiltinAccessorProperty(prototype, TemporalConstants.OFFSET, realm.lookupAccessor(TemporalZonedDateTimePrototypeBuiltins.BUILTINS, TemporalConstants.OFFSET));

        JSObjectUtil.putToStringTag(prototype, "Temporal.ZonedDateTime");

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSTemporalZonedDateTime.INSTANCE, context);
        return initialShape;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalZonedDateTimePrototype();
    }

    @Override
    public void fillConstructor(JSRealm realm, DynamicObject constructor) {
        WithFunctionsAndSpecies.super.fillConstructor(realm, constructor);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalZonedDateTimeFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalZonedDateTime(Object obj) {
        return obj instanceof JSTemporalZonedDateTimeObject;
    }
}
