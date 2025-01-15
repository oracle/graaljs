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
package com.oracle.truffle.js.nodes.temporal;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.intl.GetOptionsObjectNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of ToTemporalDateTime() operation.
 */
public abstract class ToTemporalDateTimeNode extends JavaScriptBaseNode {

    protected ToTemporalDateTimeNode() {
    }

    public abstract JSTemporalPlainDateTimeObject execute(Object value, JSDynamicObject options);

    @Specialization
    public JSTemporalPlainDateTimeObject toTemporalDateTime(Object item, JSDynamicObject options,
                    @Cached InlinedConditionProfile isObjectProfile,
                    @Cached InlinedConditionProfile isPlainDateTimeProfile,
                    @Cached InlinedConditionProfile isZonedDateTimeProfile,
                    @Cached InlinedConditionProfile isPlainDateProfile,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached IsObjectNode isObjectNode,
                    @Cached("create(getJSContext())") GetOptionsObjectNode getOptionsNode,
                    @Cached GetTemporalCalendarSlotValueWithISODefaultNode getTemporalCalendarNode,
                    @Cached TemporalGetOptionNode getOptionNode,
                    @Cached TemporalCalendarDateFromFieldsNode dateFromFieldsNode) {
        JSTemporalDateTimeRecord result;
        TruffleString calendar;
        JSContext ctx = getLanguage().getJSContext();
        JSRealm realm = getRealm();
        if (isObjectProfile.profile(this, isObjectNode.executeBoolean(item))) {
            if (isPlainDateTimeProfile.profile(this, item instanceof JSTemporalPlainDateTimeObject)) {
                Object resolvedOptions = getOptionsNode.execute(options);
                TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
                return (JSTemporalPlainDateTimeObject) item;
            } else if (isZonedDateTimeProfile.profile(this, TemporalUtil.isTemporalZonedDateTime(item))) {
                var zdt = (JSTemporalZonedDateTimeObject) item;
                var instant = JSTemporalInstant.create(ctx, realm, zdt.getNanoseconds());
                JSTemporalPlainDateTimeObject isoDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, realm, zdt.getTimeZone(), instant, zdt.getCalendar());
                Object resolvedOptions = getOptionsNode.execute(options);
                TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
                return isoDateTime;
            } else if (isPlainDateProfile.profile(this, item instanceof JSTemporalPlainDateObject)) {
                Object resolvedOptions = getOptionsNode.execute(options);
                TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
                var date = (JSTemporalPlainDateObject) item;
                return JSTemporalPlainDateTime.create(ctx, realm, date.getYear(), date.getMonth(), date.getDay(), 0, 0, 0, 0, 0, 0, date.getCalendar(), this, errorBranch);
            }
            calendar = getTemporalCalendarNode.execute(item);

            List<TruffleString> fieldNames = Boundaries.listEditableCopy(TemporalUtil.listDMMCY);
            addFieldNames(fieldNames);

            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(ctx, item, fieldNames, TemporalUtil.listEmpty);
            Object resolvedOptions = getOptionsNode.execute(options);
            TemporalUtil.Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, overflow, dateFromFieldsNode);
        } else if (item instanceof TruffleString string) {
            result = TemporalUtil.parseTemporalDateTimeString(string);
            assert TemporalUtil.isValidISODate(result.getYear(), result.getMonth(), result.getDay());
            assert TemporalUtil.isValidTime(result.getHour(), result.getMinute(), result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond());
            calendar = result.getCalendar();
            if (calendar == null) {
                calendar = TemporalConstants.ISO8601;
            }
            if (!TemporalUtil.isBuiltinCalendar((TruffleString) calendar)) {
                errorBranch.enter(this);
                throw TemporalErrors.createRangeErrorCalendarNotSupported();
            }
            Object resolvedOptions = getOptionsNode.execute(options);
            TemporalUtil.toTemporalOverflow(resolvedOptions, getOptionNode);
        } else {
            errorBranch.enter(this);
            throw Errors.createTypeErrorNotAString(item);
        }
        return JSTemporalPlainDateTime.create(ctx, realm,
                        result.getYear(), result.getMonth(), result.getDay(),
                        result.getHour(), result.getMinute(), result.getSecond(),
                        result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), calendar, this, errorBranch);
    }

    @TruffleBoundary
    private static void addFieldNames(List<TruffleString> fieldNames) {
        fieldNames.add(TemporalConstants.HOUR);
        fieldNames.add(TemporalConstants.MICROSECOND);
        fieldNames.add(TemporalConstants.MILLISECOND);
        fieldNames.add(TemporalConstants.MINUTE);
        fieldNames.add(TemporalConstants.NANOSECOND);
        fieldNames.add(TemporalConstants.SECOND);
    }

}
