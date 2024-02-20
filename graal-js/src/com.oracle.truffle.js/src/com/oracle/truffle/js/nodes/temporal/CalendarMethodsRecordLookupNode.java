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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;

/**
 * Implementation of CalendarMethodsRecordLookup() operation.
 */
public abstract class CalendarMethodsRecordLookupNode extends JavaScriptBaseNode {
    public enum Key {
        DATE_ADD(TemporalConstants.DATE_ADD),
        DATE_FROM_FIELDS(TemporalConstants.DATE_FROM_FIELDS),
        DATE_UNTIL(TemporalConstants.DATE_UNTIL),
        DAY(TemporalConstants.DAY),
        FIELDS(TemporalConstants.FIELDS),
        MERGE_FIELDS(TemporalConstants.MERGE_FIELDS),
        MONTH_DAY_FROM_FIELDS(TemporalConstants.MONTH_DAY_FROM_FIELDS),
        YEAR_MONTH_FROM_FIELDS(TemporalConstants.YEAR_MONTH_FROM_FIELDS),
        YEAR(TemporalConstants.YEAR),
        MONTH(TemporalConstants.MONTH),
        MONTH_CODE(TemporalConstants.MONTH_CODE),
        DAY_OF_WEEK(TemporalConstants.DAY_OF_WEEK),
        DAY_OF_YEAR(TemporalConstants.DAY_OF_YEAR),
        WEEK_OF_YEAR(TemporalConstants.WEEK_OF_YEAR),
        DAYS_IN_WEEK(TemporalConstants.DAYS_IN_WEEK),
        DAYS_IN_MONTH(TemporalConstants.DAYS_IN_MONTH),
        DAYS_IN_YEAR(TemporalConstants.DAYS_IN_YEAR),
        MONTHS_IN_YEAR(TemporalConstants.MONTHS_IN_YEAR),
        IN_LEAP_YEAR(TemporalConstants.IN_LEAP_YEAR);

        private final TruffleString propertyKey;

        Key(TruffleString propertyKey) {
            this.propertyKey = propertyKey;
        }

        TruffleString getPropertyKey() {
            return propertyKey;
        }
    }

    protected final Key key;

    protected CalendarMethodsRecordLookupNode(Key key) {
        this.key = key;
    }

    public Key getKey() {
        return key;
    }

    @NeverDefault
    public static CalendarMethodsRecordLookupNode createDateAdd() {
        return create(Key.DATE_ADD);
    }

    @NeverDefault
    public static CalendarMethodsRecordLookupNode createDateFromFields() {
        return create(Key.DATE_FROM_FIELDS);
    }

    @NeverDefault
    public static CalendarMethodsRecordLookupNode createDateUntil() {
        return create(Key.DATE_UNTIL);
    }

    @NeverDefault
    public static CalendarMethodsRecordLookupNode createDay() {
        return create(Key.DAY);
    }

    @NeverDefault
    public static CalendarMethodsRecordLookupNode createFields() {
        return create(Key.FIELDS);
    }

    @NeverDefault
    public static CalendarMethodsRecordLookupNode createMergeFields() {
        return create(Key.MERGE_FIELDS);
    }

    @NeverDefault
    public static CalendarMethodsRecordLookupNode createMonthDayFromFields() {
        return create(Key.MONTH_DAY_FROM_FIELDS);
    }

    @NeverDefault
    public static CalendarMethodsRecordLookupNode createYearMonthFromFields() {
        return create(Key.YEAR_MONTH_FROM_FIELDS);
    }

    @NeverDefault
    public static CalendarMethodsRecordLookupNode create(Key key) {
        return CalendarMethodsRecordLookupNodeGen.create(key);
    }

    public abstract Object execute(Object receiver);

    @Specialization
    protected Object lookup(@SuppressWarnings("unused") TruffleString receiver) {
        JSRealm realm = getRealm();
        return switch (key) {
            case DATE_ADD -> realm.getTemporalCalendarDateAddFunctionObject();
            case DATE_FROM_FIELDS -> realm.getTemporalCalendarDateFromFieldsFunctionObject();
            case DATE_UNTIL -> realm.getTemporalCalendarDateUntilFunctionObject();
            case DAY -> realm.getTemporalCalendarDayFunctionObject();
            case FIELDS -> realm.getTemporalCalendarFieldsFunctionObject();
            case MERGE_FIELDS -> realm.getTemporalCalendarMergeFieldsFunctionObject();
            case MONTH_DAY_FROM_FIELDS -> realm.getTemporalCalendarMonthDayFromFieldsFunctionObject();
            case YEAR_MONTH_FROM_FIELDS -> realm.getTemporalCalendarYearMonthFromFieldsFunctionObject();
            case YEAR -> realm.getTemporalCalendarYearFunctionObject();
            case MONTH -> realm.getTemporalCalendarMonthFunctionObject();
            case MONTH_CODE -> realm.getTemporalCalendarMonthCodeFunctionObject();
            case DAY_OF_WEEK -> realm.getTemporalCalendarDayOfWeekFunctionObject();
            case DAY_OF_YEAR -> realm.getTemporalCalendarDayOfYearFunctionObject();
            case WEEK_OF_YEAR -> realm.getTemporalCalendarWeekOfYearFunctionObject();
            case DAYS_IN_WEEK -> realm.getTemporalCalendarDaysInWeekFunctionObject();
            case DAYS_IN_MONTH -> realm.getTemporalCalendarDaysInMonthFunctionObject();
            case DAYS_IN_YEAR -> realm.getTemporalCalendarDaysInYearFunctionObject();
            case MONTHS_IN_YEAR -> realm.getTemporalCalendarMonthsInYearFunctionObject();
            case IN_LEAP_YEAR -> realm.getTemporalCalendarInLeapYearFunctionObject();
            default -> throw CompilerDirectives.shouldNotReachHere(key.name());
        };
    }

    @Specialization(guards = "!isString(receiver)")
    protected Object lookup(Object receiver,
                    @Cached("create(getJSContext(), key.getPropertyKey())") GetMethodNode getMethod,
                    @Cached InlinedBranchProfile errorBranch) {
        Object method = getMethod.executeWithTarget(receiver);
        if (method == Undefined.instance) {
            errorBranch.enter(this);
            throw Errors.createTypeErrorCallableExpected();
        }
        return method;
    }

}
