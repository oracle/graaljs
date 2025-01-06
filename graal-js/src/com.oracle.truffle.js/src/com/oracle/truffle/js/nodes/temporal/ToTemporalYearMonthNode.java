/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.intl.GetOptionsObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.builtins.temporal.CalendarMethodsRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonth;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonthObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of ToTemporalYearMonth() operation.
 */
public abstract class ToTemporalYearMonthNode extends JavaScriptBaseNode {

    protected ToTemporalYearMonthNode() {
    }

    public abstract JSTemporalPlainYearMonthObject execute(Object value, Object options);

    @Specialization
    public JSTemporalPlainYearMonthObject toTemporalYearMonth(Object item, Object optionsParam,
                    @Cached InlinedConditionProfile isObjectProfile,
                    @Cached("create(getJSContext())") GetOptionsObjectNode getOptionsObject,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached IsObjectNode isObjectNode,
                    @Cached TemporalGetOptionNode getOptionNode,
                    @Cached GetTemporalCalendarSlotValueWithISODefaultNode getCalendarSlotValueWithISODefault,
                    @Cached("createFields()") CalendarMethodsRecordLookupNode lookupFields,
                    @Cached("createYearMonthFromFields()") CalendarMethodsRecordLookupNode lookupYearMonthFromFields,
                    @Cached TemporalYearMonthFromFieldsNode yearMonthFromFieldsNode,
                    @Cached TemporalCalendarFieldsNode calendarFieldsNode) {
        Object options = optionsParam;
        if (isObjectProfile.profile(this, isObjectNode.executeBoolean(item))) {
            if (item instanceof JSTemporalPlainYearMonthObject yearMonth) {
                Object resolvedOptions = getOptionsObject.execute(options);
                TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
                return JSTemporalPlainYearMonth.create(getJSContext(), getRealm(), yearMonth.getYear(), yearMonth.getMonth(), yearMonth.getCalendar(), yearMonth.getDay(), this, errorBranch);
            }
            Object calendar = getCalendarSlotValueWithISODefault.execute(item);
            Object fieldsMethod = lookupFields.execute(calendar);
            Object yearMonthFromFieldsMethod = lookupYearMonthFromFields.execute(calendar);
            CalendarMethodsRecord calendarRec = CalendarMethodsRecord.forFieldsAndYearMonthFromFields(calendar, fieldsMethod, yearMonthFromFieldsMethod);
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendarRec, TemporalUtil.listMMCY);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getLanguage().getJSContext(), item, fieldNames, TemporalUtil.listEmpty);
            Object resolvedOptions = getOptionsObject.execute(options);
            return yearMonthFromFieldsNode.execute(calendarRec, fields, resolvedOptions);
        } else if (item instanceof TruffleString string) {
            JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalYearMonthString(string);
            TruffleString calendar = result.getCalendar();
            if (calendar == null) {
                calendar = TemporalConstants.ISO8601;
            }
            if (!TemporalUtil.isBuiltinCalendar(calendar)) {
                errorBranch.enter(this);
                throw TemporalErrors.createRangeErrorCalendarNotSupported();
            }
            Object resolvedOptions = getOptionsObject.execute(options);
            TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            JSDynamicObject result2 = JSTemporalPlainYearMonth.create(getLanguage().getJSContext(), getRealm(),
                            result.getYear(), result.getMonth(), calendar, result.getDay(), this, errorBranch);
            Object yearMonthFromFieldsMethod = lookupYearMonthFromFields.execute(calendar);
            CalendarMethodsRecord calendarRec = CalendarMethodsRecord.forYearMonthFromFields(calendar, yearMonthFromFieldsMethod);
            return yearMonthFromFieldsNode.execute(calendarRec, result2, Undefined.instance);
        } else {
            errorBranch.enter(this);
            throw Errors.createTypeErrorNotAString(item);
        }
    }
}
