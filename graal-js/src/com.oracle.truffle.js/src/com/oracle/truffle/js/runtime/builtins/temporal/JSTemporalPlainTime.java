/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimeFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltins;
import com.oracle.truffle.js.nodes.cast.JSToIntegerWithTruncationNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public final class JSTemporalPlainTime extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final JSTemporalPlainTime INSTANCE = new JSTemporalPlainTime();

    public static final TruffleString CLASS_NAME = Strings.constant("PlainTime");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("PlainTime.prototype");
    public static final TruffleString TO_STRING_TAG = Strings.constant("Temporal.PlainTime");

    private JSTemporalPlainTime() {
    }

    public static JSTemporalPlainTimeObject create(JSContext context, JSRealm realm,
                    int hours, int minutes, int seconds, int milliseconds, int microseconds, int nanoseconds,
                    Node node, InlinedBranchProfile errorBranch) {
        return create(context, realm, INSTANCE.getIntrinsicDefaultProto(realm),
                        hours, minutes, seconds, milliseconds, microseconds, nanoseconds,
                        node, errorBranch);
    }

    @InliningCutoff
    public static JSTemporalPlainTimeObject create(JSContext context, JSRealm realm, JSDynamicObject proto,
                    int hours, int minutes, int seconds, int milliseconds, int microseconds, int nanoseconds,
                    Node node, InlinedBranchProfile errorBranch) {
        if (!TemporalUtil.isValidTime(hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
            errorBranch.enter(node);
            throw TemporalErrors.createRangeErrorTimeOutsideRange();
        }
        TruffleString calendar = TemporalConstants.ISO8601;
        JSObjectFactory factory = context.getTemporalPlainTimeFactory();
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSTemporalPlainTimeObject(shape, proto, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, calendar), realm, proto);
        return factory.trackAllocation(newObj);
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject constructor) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(prototype, constructor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalPlainTimePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putAccessorsFromContainer(realm, prototype, TemporalPlainTimePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, TO_STRING_TAG);
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainTime.INSTANCE, context);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainTimePrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalPlainTimeFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalPlainTime(Object obj) {
        return obj instanceof JSTemporalPlainTimeObject;
    }

    // region Abstract methods

    // 4.5.3
    public static JSDynamicObject toPartialTime(Object temporalTimeLike, JSToIntegerWithTruncationNode toIntegerWithTruncation, JSContext ctx) {
        JSRealm realm = JSRealm.get(null);
        JSDynamicObject result = JSOrdinary.create(ctx, realm);
        boolean any = false;
        for (TruffleString property : TemporalUtil.TIME_LIKE_PROPERTIES) {
            Object value = JSRuntime.get(temporalTimeLike, property);
            if (value != Undefined.instance) {
                any = true;
                value = toIntegerWithTruncation.executeDouble(value);
                JSObjectUtil.putDataProperty(result, property, value);
            }
        }
        if (!any) {
            throw TemporalErrors.createTypeErrorTemporalTimePropertyExpected();
        }
        return result;
    }

    // 4.5.12
    @TruffleBoundary
    public static TruffleString temporalTimeToString(long hour, long minute, long second, long millisecond, long microsecond,
                    long nanosecond, Object precision) {
        TruffleString hourString = TemporalUtil.toZeroPaddedDecimalString(hour, 2);
        TruffleString minuteString = TemporalUtil.toZeroPaddedDecimalString(minute, 2);
        TruffleString secondString = TemporalUtil.formatSecondsStringPart(second, millisecond, microsecond, nanosecond, precision);
        return Strings.format("%s:%s%s", hourString, minuteString, secondString);
    }

    // endregion
}
