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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.ISO8601;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayFunctionBuiltins;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltins;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarIdentifierNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.ShowCalendar;

public class JSTemporalPlainMonthDay extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final JSTemporalPlainMonthDay INSTANCE = new JSTemporalPlainMonthDay();

    public static final TruffleString CLASS_NAME = Strings.constant("PlainMonthDay");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("PlainMonthDay.prototype");
    public static final TruffleString TO_STRING_TAG = Strings.constant("Temporal.PlainMonthDay");

    public static JSTemporalPlainMonthDayObject create(JSContext context, JSRealm realm, int isoMonth, int isoDay, TruffleString calendar, int referenceISOYear,
                    Node node, InlinedBranchProfile errorBranch) {
        return create(context, realm, INSTANCE.getIntrinsicDefaultProto(realm), isoMonth, isoDay, calendar, referenceISOYear, node, errorBranch);
    }

    @InliningCutoff
    public static JSTemporalPlainMonthDayObject create(JSContext context, JSRealm realm, JSDynamicObject proto, int isoMonth, int isoDay, TruffleString calendar, int referenceISOYear,
                    Node node, InlinedBranchProfile errorBranch) {
        if (!TemporalUtil.isoDateWithinLimits(referenceISOYear, isoMonth, isoDay)) {
            errorBranch.enter(node);
            throw TemporalErrors.createRangeErrorMonthDayOutsideRange();
        }
        JSObjectFactory factory = context.getTemporalPlainMonthDayFactory();
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSTemporalPlainMonthDayObject(shape, proto, isoMonth, isoDay, calendar, referenceISOYear), realm, proto);
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
        JSObjectUtil.putAccessorsFromContainer(realm, prototype, TemporalPlainMonthDayPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, TemporalPlainMonthDayPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, TO_STRING_TAG);
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, JSTemporalPlainMonthDay.INSTANCE, context);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getTemporalPlainMonthDayPrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, TemporalPlainMonthDayFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSTemporalPlainMonthDay(Object obj) {
        return obj instanceof JSTemporalPlainMonthDayObject;
    }

    @TruffleBoundary
    public static TruffleString temporalMonthDayToString(JSTemporalPlainMonthDayObject md, ShowCalendar showCalendar) {
        TruffleString monthString = TemporalUtil.toZeroPaddedDecimalString(md.getMonth(), 2);
        TruffleString dayString = TemporalUtil.toZeroPaddedDecimalString(md.getDay(), 2);

        TruffleString calendarID = ToTemporalCalendarIdentifierNode.getUncached().executeString(md.getCalendar());
        TruffleString result = Strings.format("%s-%s", monthString, dayString);
        if (showCalendar == ShowCalendar.ALWAYS || showCalendar == ShowCalendar.CRITICAL || !ISO8601.equals(calendarID)) {
            TruffleString year = TemporalUtil.padISOYear(md.getYear());
            result = Strings.format("%s-%s", year, result);
        }
        TruffleString calendarString = TemporalUtil.formatCalendarAnnotation(calendarID, showCalendar);
        if (calendarString.isEmpty()) {
            return result;
        } else {
            return Strings.concat(result, calendarString);
        }
    }
}
