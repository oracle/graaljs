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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.CALENDAR;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.intl.GetOptionsObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendarHolder;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDay;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDayObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonth;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of ToTemporalMonthDay() operation.
 */
public abstract class ToTemporalMonthDayNode extends JavaScriptBaseNode {

    @Child private PropertyGetNode getCalendarNode;

    protected ToTemporalMonthDayNode() {
    }

    public abstract JSTemporalPlainMonthDayObject execute(Object item, Object optParam);

    @Specialization
    public JSTemporalPlainMonthDayObject iso8601CalendarProfile(Object item, Object options,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached InlinedConditionProfile isObjectProfile,
                    @Cached InlinedConditionProfile iso8601CalendarProfile,
                    @Cached InlinedConditionProfile getCalendarPath,
                    @Cached IsObjectNode isObjectNode,
                    @Cached("create(getJSContext())") GetOptionsObjectNode getOptionsObject,
                    @Cached("createWithISO8601()") ToTemporalCalendarSlotValueNode toCalendarSlotValue,
                    @Cached TemporalGetOptionNode temporalGetOptionNode,
                    @Cached TemporalMonthDayFromFieldsNode monthDayFromFieldsNode,
                    @Cached TruffleString.ToJavaStringNode toJavaString,
                    @Cached TruffleString.FromJavaStringNode fromJavaString,
                    @Cached TruffleString.EqualNode stringEqual) {
        JSContext ctx = getLanguage().getJSContext();
        JSRealm realm = getRealm();
        if (isObjectProfile.profile(this, isObjectNode.executeBoolean(item))) {
            JSDynamicObject itemObj = (JSDynamicObject) item;
            if (JSTemporalPlainMonthDay.isJSTemporalPlainMonthDay(itemObj)) {
                Object resolvedOptions = getOptionsObject.execute(options);
                TemporalUtil.getTemporalOverflowOption(resolvedOptions, temporalGetOptionNode);
                JSTemporalPlainMonthDayObject pmd = (JSTemporalPlainMonthDayObject) itemObj;
                return JSTemporalPlainMonthDay.create(ctx, realm,
                                pmd.getMonth(), pmd.getDay(), pmd.getCalendar(), pmd.getYear(), this, errorBranch);
            }
            // GetTemporalCalendarIdentifierWithISODefault
            TruffleString calendar;
            if (getCalendarPath.profile(this, JSTemporalPlainDate.isJSTemporalPlainDate(itemObj) ||
                            JSTemporalPlainDateTime.isJSTemporalPlainDateTime(itemObj) ||
                            JSTemporalPlainTime.isJSTemporalPlainTime(itemObj) ||
                            JSTemporalPlainYearMonth.isJSTemporalPlainYearMonth(itemObj) ||
                            TemporalUtil.isTemporalZonedDateTime(itemObj))) {
                calendar = ((JSTemporalCalendarHolder) itemObj).getCalendar();
            } else {
                Object calendarLike = getCalendar(itemObj);
                if (calendarLike == Undefined.instance) {
                    calendar = TemporalConstants.ISO8601;
                } else {
                    calendar = toCalendarSlotValue.execute(calendarLike);
                }
            }

            List<TruffleString> fieldNames = TemporalUtil.listDMMCY;
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(ctx, itemObj, fieldNames, TemporalUtil.listEmpty);
            Object resolvedOptions = getOptionsObject.execute(options);
            TemporalUtil.Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, temporalGetOptionNode);
            return monthDayFromFieldsNode.execute(calendar, fields, overflow);
        } else if (item instanceof TruffleString string) {
            JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalMonthDayString(string);
            TruffleString calendar = result.getCalendar();
            if (calendar == null) {
                calendar = TemporalConstants.ISO8601;
            } else {
                String calendarJLS = toJavaString.execute(calendar);
                calendar = Strings.fromJavaString(IntlUtil.canonicalizeCalendar(calendarJLS));
            }
            Object resolvedOptions = getOptionsObject.execute(options);
            TemporalUtil.Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, temporalGetOptionNode);
            if (iso8601CalendarProfile.profile(this, Strings.equals(stringEqual, calendar, TemporalConstants.ISO8601))) {
                int referenceISOYear = 1972;
                return JSTemporalPlainMonthDay.create(ctx, realm, result.getMonth(), result.getDay(), calendar, referenceISOYear, this, errorBranch);
            }
            JSDynamicObject result2 = JSTemporalPlainMonthDay.create(ctx, realm, result.getMonth(), result.getDay(), calendar, result.getYear(), this, errorBranch);
            return monthDayFromFieldsNode.execute(calendar, result2, overflow);
        } else {
            errorBranch.enter(this);
            throw Errors.createTypeErrorNotAString(item);
        }
    }

    private Object getCalendar(JSDynamicObject obj) {
        if (getCalendarNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getCalendarNode = insert(PropertyGetNode.create(CALENDAR, getLanguage().getJSContext()));
        }
        return getCalendarNode.getValue(obj);
    }
}
