/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Calendar Methods Record.
 *
 * Field values may be null in case they have not been read, and therefore must not be used.
 * Conversely, fields that have been read must not be null. The receiver must never be null.
 */
public record CalendarMethodsRecord(
                /**
                 * A String or Object. The calendar object, or a string indicating a built-in time
                 * zone.
                 */
                Object receiver,
                /**
                 * A function object or undefined. The calendar's dateAdd method. For a built-in
                 * calendar this is always %Temporal.Calendar.prototype.dateAdd%.
                 */
                Object dateAdd,
                /**
                 * A function object or undefined. The calendar's dateFromFields method. For a
                 * built-in calendar this is always %Temporal.Calendar.prototype.dateFromFields%.
                 */
                Object dateFromFields,
                /**
                 * A function object or undefined. The calendar's dateUntil method. For a built-in
                 * calendar this is always %Temporal.Calendar.prototype.dateUntil%.
                 */
                Object dateUntil,
                /**
                 * A function object or undefined. The calendar's day method. For a built-in
                 * calendar this is always %Temporal.Calendar.prototype.day%.
                 */
                Object day,
                /**
                 * A function object or undefined. The calendar's fields method. For a built-in
                 * calendar this is always %Temporal.Calendar.prototype.fields%.
                 */
                Object fields,
                /**
                 * A function object or undefined. The calendar's mergeFields method. For a built-in
                 * calendar this is always %Temporal.Calendar.prototype.mergeFields%.
                 */
                Object mergeFields,
                /**
                 * A function object or undefined. The calendar's monthDayFromFields method. For a
                 * built-in calendar this is always
                 * %Temporal.Calendar.prototype.monthDayFromFields%.
                 */
                Object monthDayFromFields,
                /**
                 * A function object or undefined. The calendar's yearMonthFromFields method. For a
                 * built-in calendar this is always
                 * %Temporal.Calendar.prototype.yearMonthFromFields%.
                 */
                Object yearMonthFromFields) {

    public static CalendarMethodsRecord forDateAdd(Object receiver, Object dateAdd) {
        return new CalendarMethodsRecord(receiver, dateAdd, null, null, null, null, null, null, null);
    }

    public static CalendarMethodsRecord forMonthDayFromFieldsMethod(Object receiver, Object monthDayFromFields) {
        return new CalendarMethodsRecord(receiver, null, null, null, null, null, null, monthDayFromFields, null);
    }

    public static CalendarMethodsRecord forYearMonthFromFields(Object receiver, Object yearMonthFromFields) {
        return new CalendarMethodsRecord(receiver, null, null, null, null, null, null, null, yearMonthFromFields);
    }

    public static CalendarMethodsRecord forDateAddDateUntil(Object receiver, Object dateAdd, Object dateUntil) {
        return new CalendarMethodsRecord(receiver, dateAdd, null, dateUntil, null, null, null, null, null);
    }

    public static CalendarMethodsRecord forDateFromFieldsAndFields(Object receiver, Object dateFromFields, Object fields) {
        return new CalendarMethodsRecord(receiver, null, dateFromFields, null, null, fields, null, null, null);
    }

    public static CalendarMethodsRecord forDateFromFieldsAndFieldsAndMergeFields(Object receiver, Object dateFromFields, Object fields, Object mergeFields) {
        return new CalendarMethodsRecord(receiver, null, dateFromFields, null, null, fields, mergeFields, null, null);
    }

    public static CalendarMethodsRecord forFieldsAndMonthDayFromFields(Object receiver, Object fields, Object monthDayFromFields) {
        return new CalendarMethodsRecord(receiver, null, null, null, null, fields, null, monthDayFromFields, null);
    }

    public static CalendarMethodsRecord forFieldsAndMergeFieldsAndMonthDayFromFields(Object receiver, Object fields, Object mergeFields, Object monthDayFromFields) {
        return new CalendarMethodsRecord(receiver, null, null, null, null, fields, mergeFields, monthDayFromFields, null);
    }

    public static CalendarMethodsRecord forFieldsAndMergeFieldsAndYearMonthFromFields(Object receiver, Object fields, Object mergeFields, Object yearMonthFromFields) {
        return new CalendarMethodsRecord(receiver, null, null, null, null, fields, mergeFields, null, yearMonthFromFields);
    }

    public static CalendarMethodsRecord forFieldsAndYearMonthFromFields(Object receiver, Object fields, Object yearMonthFromFields) {
        return new CalendarMethodsRecord(receiver, null, null, null, null, fields, null, null, yearMonthFromFields);
    }

}
