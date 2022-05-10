/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH_CODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDay;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDayObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonth;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalCalendar;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Implementation of ToTemporalMonthDay() operation.
 */
public abstract class ToTemporalMonthDayNode extends JavaScriptBaseNode {

    @Child private PropertyGetNode getMonthNode;
    @Child private PropertyGetNode getMonthCodeNode;
    @Child private PropertyGetNode getYearNode;
    @Child private PropertyGetNode getCalendarNode;
    protected final ConditionProfile isObjectProfile = ConditionProfile.create();
    protected final ConditionProfile setReferenceYear = ConditionProfile.create();
    protected final ConditionProfile returnPlainMonthDay = ConditionProfile.create();
    protected final ConditionProfile getCalendarPath = ConditionProfile.create();
    protected final JSContext ctx;

    protected ToTemporalMonthDayNode(JSContext context) {
        this.ctx = context;
    }

    public static ToTemporalMonthDayNode create(JSContext context) {
        return ToTemporalMonthDayNodeGen.create(context);
    }

    public abstract JSTemporalPlainMonthDayObject executeDynamicObject(Object item, JSDynamicObject optParam);

    @Specialization
    public JSTemporalPlainMonthDayObject toTemporalMonthDay(Object item, JSDynamicObject options,
                    @Cached BranchProfile errorBranch,
                    @Cached("create()") IsObjectNode isObjectNode,
                    @Cached("create()") JSToStringNode toStringNode,
                    @Cached("create(ctx)") ToTemporalCalendarWithISODefaultNode toTemporalCalendarWithISODefaultNode,
                    @Cached("create(ctx)") TemporalMonthDayFromFieldsNode monthDayFromFieldsNode,
                    @Cached("create(ctx)") TemporalCalendarFieldsNode calendarFieldsNode) {
        int referenceISOYear = 1972;
        if (isObjectProfile.profile(isObjectNode.executeBoolean(item))) {
            JSDynamicObject itemObj = (JSDynamicObject) item;
            if (JSTemporalPlainMonthDay.isJSTemporalPlainMonthDay(itemObj)) {
                return (JSTemporalPlainMonthDayObject) itemObj;
            }
            JSDynamicObject calendar = null;
            boolean calendarAbsent = false;
            if (getCalendarPath.profile(
                            JSTemporalPlainDate.isJSTemporalPlainDate(itemObj) || JSTemporalPlainDateTime.isJSTemporalPlainDateTime(itemObj) || JSTemporalPlainTime.isJSTemporalPlainTime(itemObj) ||
                                            JSTemporalPlainYearMonth.isJSTemporalPlainYearMonth(itemObj) || TemporalUtil.isTemporalZonedDateTime(itemObj))) {
                assert itemObj instanceof TemporalCalendar; // basically, that's above line's check,
                calendar = ((TemporalCalendar) itemObj).getCalendar();
                calendarAbsent = false;
            } else {
                Object calendarObj = getCalendar(itemObj);
                calendarAbsent = (calendarObj == Undefined.instance);
                calendar = toTemporalCalendarWithISODefaultNode.executeDynamicObject(calendarObj);
            }
            List<TruffleString> fieldNames = calendarFieldsNode.execute(calendar, TemporalUtil.listDMMCY);
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(ctx, itemObj, fieldNames, TemporalUtil.listEmpty);

            if (getMonthNode == null || getMonthCodeNode == null || getYearNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getMonthNode = insert(PropertyGetNode.create(MONTH, ctx));
                getMonthCodeNode = insert(PropertyGetNode.create(MONTH_CODE, ctx));
                getYearNode = insert(PropertyGetNode.create(YEAR, ctx));
            }
            Object month = getMonthNode.getValue(fields);
            Object monthCode = getMonthCodeNode.getValue(fields);
            Object year = getYearNode.getValue(fields);
            if (setReferenceYear.profile(calendarAbsent && month != Undefined.instance && monthCode == Undefined.instance && year == Undefined.instance)) {
                TemporalUtil.createDataPropertyOrThrow(ctx, fields, YEAR, referenceISOYear);
            }
            return monthDayFromFieldsNode.execute(calendar, fields, options);
        } else {
            TemporalUtil.toTemporalOverflow(options, TemporalGetOptionNode.getUncached());
            TruffleString string = toStringNode.executeString(item);
            JSTemporalDateTimeRecord result = TemporalUtil.parseTemporalMonthDayString(string);
            JSDynamicObject calendar = toTemporalCalendarWithISODefaultNode.executeDynamicObject(result.getCalendar());
            if (returnPlainMonthDay.profile(result.getYear() == Integer.MIN_VALUE)) {
                return JSTemporalPlainMonthDay.create(ctx, result.getMonth(), result.getDay(), calendar, referenceISOYear, errorBranch);
            }
            JSDynamicObject result2 = JSTemporalPlainMonthDay.create(ctx, result.getMonth(), result.getDay(), calendar, referenceISOYear, errorBranch);
            return monthDayFromFieldsNode.execute(calendar, result2, Undefined.instance);
        }
    }

    private Object getCalendar(JSDynamicObject obj) {
        if (getCalendarNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getCalendarNode = insert(PropertyGetNode.create(CALENDAR, ctx));
        }
        return getCalendarNode.getValue(obj);
    }
}
