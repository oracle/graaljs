/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.temporal;

import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtoi;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Overflow;

/**
 * Implementation of ToTemporalTime() operation.
 */
public abstract class ToTemporalTimeNode extends JavaScriptBaseNode {

    private final ConditionProfile isObjectProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isPlainDateTimeProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isZonedDateTimeProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isPlainTimeProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile errorBranch = BranchProfile.create();

    protected final JSContext ctx;

    protected ToTemporalTimeNode(JSContext context) {
        this.ctx = context;
    }

    public static ToTemporalTimeNode create(JSContext context) {
        return ToTemporalTimeNodeGen.create(context);
    }

    public abstract JSDynamicObject executeDynamicObject(Object value, Overflow overflowParam);

    @Specialization
    protected JSDynamicObject toTemporalTime(Object item, Overflow overflowParam,
                    @Cached("create()") IsObjectNode isObjectNode,
                    @Cached("create()") JSToStringNode toStringNode,
                    @Cached("create(ctx)") GetTemporalCalendarWithISODefaultNode getTemporalCalendarNode) {
        Overflow overflow = overflowParam == null ? Overflow.CONSTRAIN : overflowParam;
        assert overflow == Overflow.CONSTRAIN || overflow == Overflow.REJECT;
        JSTemporalDurationRecord result2 = null;
        if (isObjectProfile.profile(isObjectNode.executeBoolean(item))) {
            JSDynamicObject itemObj = (JSDynamicObject) item;
            if (isPlainTimeProfile.profile(JSTemporalPlainTime.isJSTemporalPlainTime(itemObj))) {
                return itemObj;
            } else if (isZonedDateTimeProfile.profile(TemporalUtil.isTemporalZonedDateTime(itemObj))) {
                JSTemporalZonedDateTimeObject zdt = (JSTemporalZonedDateTimeObject) itemObj;
                JSTemporalInstantObject instant = JSTemporalInstant.create(ctx, getRealm(), zdt.getNanoseconds());
                JSTemporalPlainDateTimeObject plainDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, zdt.getTimeZone(), instant, zdt.getCalendar());
                return JSTemporalPlainTime.create(ctx, plainDateTime.getHour(), plainDateTime.getMinute(),
                                plainDateTime.getSecond(), plainDateTime.getMillisecond(), plainDateTime.getMicrosecond(), plainDateTime.getNanosecond(), errorBranch);
            } else if (isPlainDateTimeProfile.profile(JSTemporalPlainDateTime.isJSTemporalPlainDateTime(itemObj))) {
                JSTemporalPlainDateTimeObject dt = (JSTemporalPlainDateTimeObject) itemObj;
                return JSTemporalPlainTime.create(ctx, dt.getHour(), dt.getMinute(), dt.getSecond(), dt.getMillisecond(), dt.getMicrosecond(), dt.getNanosecond(), errorBranch);
            }
            JSDynamicObject calendar = getTemporalCalendarNode.executeDynamicObject(itemObj);
            if (!toStringNode.executeString(calendar).equals(TemporalConstants.ISO8601)) {
                errorBranch.enter();
                throw TemporalErrors.createRangeErrorTemporalISO8601Expected();
            }
            JSTemporalDateTimeRecord result = TemporalUtil.toTemporalTimeRecord(itemObj);
            result2 = TemporalUtil.regulateTime(result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), overflow);
        } else {
            TruffleString string = toStringNode.executeString(item);
            JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalTimeString(string);
            assert TemporalUtil.isValidTime(result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
            if (result.hasCalendar() && !toStringNode.executeString(result.getCalendar()).equals(TemporalConstants.ISO8601)) {
                throw TemporalErrors.createRangeErrorTemporalISO8601Expected();
            }
            result2 = JSTemporalDurationRecord.create(result);
        }
        return JSTemporalPlainTime.create(ctx, dtoi(result2.getHours()), dtoi(result2.getMinutes()), dtoi(result2.getSeconds()), dtoi(result2.getMilliseconds()), dtoi(result2.getMicroseconds()),
                        dtoi(result2.getNanoseconds()), errorBranch);
    }
}
