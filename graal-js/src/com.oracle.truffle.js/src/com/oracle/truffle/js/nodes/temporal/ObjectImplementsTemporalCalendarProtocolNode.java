/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.HasPropertyCacheNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendarObject;
import com.oracle.truffle.js.runtime.util.TemporalConstants;

/**
 * Implementation of ObjectImplementsTemporalCalendarProtocol() operation.
 */
@ImportStatic({TemporalConstants.class, Strings.class})
public abstract class ObjectImplementsTemporalCalendarProtocolNode extends JavaScriptBaseNode {

    protected ObjectImplementsTemporalCalendarProtocolNode() {
    }

    @NeverDefault
    public static ObjectImplementsTemporalCalendarProtocolNode create() {
        return ObjectImplementsTemporalCalendarProtocolNodeGen.create();
    }

    public abstract boolean execute(Object object);

    @Specialization
    public boolean doCalendar(@SuppressWarnings("unused") JSTemporalCalendarObject object) {
        return true;
    }

    @Specialization(guards = "!isJSTemporalCalendar(object)")
    public boolean doNonCalendar(Object object,
                    @Cached("create(DATE_ADD, getJSContext())") HasPropertyCacheNode hasDateAdd,
                    @Cached("create(DATE_FROM_FIELDS, getJSContext())") HasPropertyCacheNode hasDateFromFields,
                    @Cached("create(DATE_UNTIL, getJSContext())") HasPropertyCacheNode hasDateUntil,
                    @Cached("create(DAY, getJSContext())") HasPropertyCacheNode hasDay,
                    @Cached("create(DAY_OF_WEEK, getJSContext())") HasPropertyCacheNode hasDayOfWeek,
                    @Cached("create(DAY_OF_YEAR, getJSContext())") HasPropertyCacheNode hasDayOfYear,
                    @Cached("create(DAYS_IN_MONTH, getJSContext())") HasPropertyCacheNode hasDaysInMonth,
                    @Cached("create(DAYS_IN_WEEK, getJSContext())") HasPropertyCacheNode hasDaysInWeek,
                    @Cached("create(DAYS_IN_YEAR, getJSContext())") HasPropertyCacheNode hasDaysInYear,
                    @Cached("create(FIELDS, getJSContext())") HasPropertyCacheNode hasFields,
                    @Cached("create(ID_PROPERTY_NAME, getJSContext())") HasPropertyCacheNode hasID,
                    @Cached("create(IN_LEAP_YEAR, getJSContext())") HasPropertyCacheNode hasInLeapYear,
                    @Cached("create(MERGE_FIELDS, getJSContext())") HasPropertyCacheNode hasMergeFields,
                    @Cached("create(MONTH, getJSContext())") HasPropertyCacheNode hasMonth,
                    @Cached("create(MONTH_CODE, getJSContext())") HasPropertyCacheNode hasMonthCode,
                    @Cached("create(MONTH_DAY_FROM_FIELDS, getJSContext())") HasPropertyCacheNode hasMonthDayFromFields,
                    @Cached("create(MONTHS_IN_YEAR, getJSContext())") HasPropertyCacheNode hasMonthsInYear,
                    @Cached("create(WEEK_OF_YEAR, getJSContext())") HasPropertyCacheNode hasWeekOfYear,
                    @Cached("create(YEAR, getJSContext())") HasPropertyCacheNode hasYear,
                    @Cached("create(YEAR_MONTH_FROM_FIELDS, getJSContext())") HasPropertyCacheNode hasYearMonthFromFields,
                    @Cached("create(YEAR_OF_WEEK, getJSContext())") HasPropertyCacheNode hasYearOfWeek) {
        return hasDateAdd.hasProperty(object) && hasDateFromFields.hasProperty(object) && hasDateUntil.hasProperty(object) && hasDay.hasProperty(object) && hasDayOfWeek.hasProperty(object) &&
                        hasDayOfYear.hasProperty(object) && hasDaysInMonth.hasProperty(object) && hasDaysInWeek.hasProperty(object) && hasDaysInYear.hasProperty(object) &&
                        hasFields.hasProperty(object) && hasID.hasProperty(object) && hasInLeapYear.hasProperty(object) && hasMergeFields.hasProperty(object) && hasMonth.hasProperty(object) &&
                        hasMonthCode.hasProperty(object) && hasMonthDayFromFields.hasProperty(object) && hasMonthsInYear.hasProperty(object) && hasWeekOfYear.hasProperty(object) &&
                        hasYear.hasProperty(object) && hasYearMonthFromFields.hasProperty(object) && hasYearOfWeek.hasProperty(object);
    }

}
