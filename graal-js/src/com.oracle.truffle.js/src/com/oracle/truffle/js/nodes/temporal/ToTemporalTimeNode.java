/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.intl.GetOptionsObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Overflow;

/**
 * Implementation of ToTemporalTime() operation.
 */
public abstract class ToTemporalTimeNode extends JavaScriptBaseNode {

    protected ToTemporalTimeNode() {
    }

    public abstract JSTemporalPlainTimeObject execute(Object item, Object options);

    @Specialization
    protected JSTemporalPlainTimeObject toTemporalTime(Object item, Object options,
                    @Cached IsObjectNode isObjectNode,
                    @Cached("create(getJSContext())") GetOptionsObjectNode getOptionsObjectNode,
                    @Cached TemporalGetOptionNode getOptionNode,
                    @Cached JSToStringNode toStringNode,
                    @Cached InlinedConditionProfile isObjectProfile,
                    @Cached InlinedConditionProfile isPlainDateTimeProfile,
                    @Cached InlinedConditionProfile isZonedDateTimeProfile,
                    @Cached InlinedConditionProfile isPlainTimeProfile,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached CreateTimeZoneMethodsRecordNode createTimeZoneMethodsRecord) {
        assert options != null;
        JSContext ctx = getJSContext();
        JSRealm realm = getRealm();
        JSTemporalDurationRecord result2;
        if (isObjectProfile.profile(this, isObjectNode.executeBoolean(item))) {
            if (isPlainTimeProfile.profile(this, JSTemporalPlainTime.isJSTemporalPlainTime(item))) {
                Object resolvedOptions = getOptionsObjectNode.execute(options);
                TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
                JSTemporalPlainTimeObject plainTime = (JSTemporalPlainTimeObject) item;
                return JSTemporalPlainTime.create(ctx, realm,
                                plainTime.getHour(), plainTime.getMinute(), plainTime.getSecond(),
                                plainTime.getMillisecond(), plainTime.getMicrosecond(), plainTime.getNanosecond(),
                                this, errorBranch);
            } else if (isPlainDateTimeProfile.profile(this, JSTemporalPlainDateTime.isJSTemporalPlainDateTime(item))) {
                Object resolvedOptions = getOptionsObjectNode.execute(options);
                TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
                JSTemporalPlainDateTimeObject plainDateTime = (JSTemporalPlainDateTimeObject) item;
                return JSTemporalPlainTime.create(ctx, realm,
                                plainDateTime.getHour(), plainDateTime.getMinute(), plainDateTime.getSecond(),
                                plainDateTime.getMillisecond(), plainDateTime.getMicrosecond(), plainDateTime.getNanosecond(),
                                this, errorBranch);
            } else if (isZonedDateTimeProfile.profile(this, TemporalUtil.isTemporalZonedDateTime(item))) {
                JSTemporalZonedDateTimeObject zonedDateTime = (JSTemporalZonedDateTimeObject) item;
                JSTemporalDateTimeRecord isoDateTime = TemporalUtil.getISODateTimeFor((TruffleString) zonedDateTime.getTimeZone(), zonedDateTime.getNanoseconds());
                Object resolvedOptions = getOptionsObjectNode.execute(options);
                TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
                return JSTemporalPlainTime.create(ctx, realm,
                                isoDateTime.getHour(), isoDateTime.getMinute(), isoDateTime.getSecond(),
                                isoDateTime.getMillisecond(), isoDateTime.getMicrosecond(), isoDateTime.getNanosecond(),
                                this, errorBranch);
            }
            JSTemporalDateTimeRecord result = TemporalUtil.toTemporalTimeRecord(item);
            Object resolvedOptions = getOptionsObjectNode.execute(options);
            Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            result2 = TemporalUtil.regulateTime(result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), overflow);
        } else if (item instanceof TruffleString string) {
            JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalTimeString(string);
            Object resolvedOptions = getOptionsObjectNode.execute(options);
            TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            result2 = JSTemporalDurationRecord.create(result);
        } else {
            errorBranch.enter(this);
            throw Errors.createTypeErrorNotAString(item);
        }
        return JSTemporalPlainTime.create(ctx, realm,
                        dtoi(result2.getHours()), dtoi(result2.getMinutes()), dtoi(result2.getSeconds()),
                        dtoi(result2.getMilliseconds()), dtoi(result2.getMicroseconds()), dtoi(result2.getNanoseconds()), this, errorBranch);
    }
}
