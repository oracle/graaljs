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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.OFFSET;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.REJECT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;

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
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.ParseISODateTimeResult;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.MatchBehaviour;
import com.oracle.truffle.js.runtime.util.TemporalUtil.OffsetBehaviour;
import com.oracle.truffle.js.runtime.util.TemporalUtil.OffsetOption;

/**
 * Implementation of ToTemporalZonedDateTime() operation.
 */
public abstract class ToTemporalZonedDateTimeNode extends JavaScriptBaseNode {

    protected ToTemporalZonedDateTimeNode() {
    }

    public abstract JSTemporalZonedDateTimeObject execute(Object value, JSDynamicObject options);

    @Specialization
    public JSTemporalZonedDateTimeObject toTemporalZonedDateTime(Object item, JSDynamicObject options,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached InlinedConditionProfile isObjectProfile,
                    @Cached InlinedConditionProfile isZonedDateTimeProfile,
                    @Cached IsObjectNode isObjectNode,
                    @Cached("create(getJSContext())") GetOptionsObjectNode getOptionsObject,
                    @Cached TruffleString.EqualNode equalNode,
                    @Cached TemporalGetOptionNode getOptionNode,
                    @Cached ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifier,
                    @Cached GetTemporalCalendarSlotValueWithISODefaultNode getCalendarSlotValueWithISODefault,
                    @Cached TemporalCalendarFieldsNode calendarFieldsNode,
                    @Cached TemporalCalendarDateFromFieldsNode dateFromFieldsNode) {
        JSTemporalDateTimeRecord result;
        TruffleString offsetString = null;
        TruffleString timeZone;
        TruffleString calendar;
        JSContext ctx = getLanguage().getJSContext();
        JSRealm realm = getRealm();
        OffsetBehaviour offsetBehaviour = OffsetBehaviour.OPTION;
        MatchBehaviour matchBehaviour = MatchBehaviour.MATCH_EXACTLY;
        TemporalUtil.Disambiguation disambiguation;
        OffsetOption offsetOption;
        if (isObjectProfile.profile(this, isObjectNode.executeBoolean(item))) {
            if (isZonedDateTimeProfile.profile(this, TemporalUtil.isTemporalZonedDateTime(item))) {
                return (JSTemporalZonedDateTimeObject) item;
            }

            calendar = getCalendarSlotValueWithISODefault.execute(item);

            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, Boundaries.listEditableCopy(TemporalUtil.listDMMCY));
            addFieldNames(fieldNames);

            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(ctx, item, fieldNames, TemporalUtil.listTimeZone);
            Object timeZoneObj = JSObject.get(fields, TIME_ZONE);
            timeZone = toTimeZoneIdentifier.execute(timeZoneObj);
            Object offsetStringObj = JSObject.get(fields, OFFSET);
            if (offsetStringObj == Undefined.instance) {
                offsetBehaviour = OffsetBehaviour.WALL;
            } else {
                offsetString = (TruffleString) offsetStringObj;
            }
            Object resolvedOptions = getOptionsObject.execute(options);
            disambiguation = TemporalUtil.toTemporalDisambiguation(resolvedOptions, getOptionNode, equalNode);
            offsetOption = TemporalUtil.toTemporalOffset(resolvedOptions, REJECT, getOptionNode, equalNode);
            TemporalUtil.Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            result = TemporalUtil.interpretTemporalDateTimeFields(calendar, fields, overflow, dateFromFieldsNode);
        } else if (item instanceof TruffleString string) {
            ParseISODateTimeResult resultZDT = TemporalUtil.parseTemporalZonedDateTimeString(string);
            result = resultZDT;
            TruffleString timeZoneName = resultZDT.getTimeZoneResult().getName();
            assert timeZoneName != null;
            if (!TemporalUtil.canParseAsTimeZoneNumericUTCOffset(timeZoneName)) {
                timeZoneName = toTimeZoneIdentifier.execute(timeZoneName);
            }
            offsetString = resultZDT.getTimeZoneResult().getOffsetString();
            if (resultZDT.getTimeZoneResult().isZ()) {
                offsetBehaviour = OffsetBehaviour.EXACT;
            } else {
                offsetBehaviour = OffsetBehaviour.WALL;
            }
            timeZone = timeZoneName;
            calendar = result.getCalendar();
            if (calendar == null) {
                calendar = TemporalConstants.ISO8601;
            }
            if (!TemporalUtil.isBuiltinCalendar((TruffleString) calendar)) {
                errorBranch.enter(this);
                throw TemporalErrors.createRangeErrorCalendarNotSupported();
            }
            matchBehaviour = MatchBehaviour.MATCH_MINUTES;
            Object resolvedOptions = getOptionsObject.execute(options);
            disambiguation = TemporalUtil.toTemporalDisambiguation(resolvedOptions, getOptionNode, equalNode);
            offsetOption = TemporalUtil.toTemporalOffset(resolvedOptions, REJECT, getOptionNode, equalNode);
            TemporalUtil.toTemporalOverflow(resolvedOptions, getOptionNode);
        } else {
            errorBranch.enter(this);
            throw Errors.createTypeErrorNotAString(item);
        }
        long offsetNanoseconds = 0;
        if (offsetBehaviour == OffsetBehaviour.OPTION) {
            offsetNanoseconds = TemporalUtil.parseTimeZoneOffsetString(offsetString);
        }
        BigInt epochNanoseconds = TemporalUtil.interpretISODateTimeOffset(ctx, realm, result.getYear(), result.getMonth(), result.getDay(), result.getHour(), result.getMinute(),
                        result.getSecond(), result.getMillisecond(), result.getMicrosecond(), result.getNanosecond(), offsetBehaviour, offsetNanoseconds, timeZone, disambiguation, offsetOption,
                        matchBehaviour);
        return JSTemporalZonedDateTime.create(ctx, realm, epochNanoseconds, timeZone, calendar);
    }

    @TruffleBoundary
    private static void addFieldNames(List<TruffleString> fieldNames) {
        fieldNames.add(TemporalConstants.HOUR);
        fieldNames.add(TemporalConstants.MICROSECOND);
        fieldNames.add(TemporalConstants.MILLISECOND);
        fieldNames.add(TemporalConstants.MINUTE);
        fieldNames.add(TemporalConstants.NANOSECOND);
        fieldNames.add(TemporalConstants.OFFSET);
        fieldNames.add(TemporalConstants.SECOND);
        fieldNames.add(TemporalConstants.TIME_ZONE);
    }

}
