/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.util;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSException;

public final class TemporalErrors {

    @TruffleBoundary
    public static JSException createTypeErrorOptions() {
        return Errors.createTypeError("Options is not undefined and not an object.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorOptionsUndefined() {
        return Errors.createTypeError("Options object expected");
    }

    @TruffleBoundary
    public static JSException createRangeErrorInvalidPlainDateTime() {
        return Errors.createRangeError("invalid PlainDateTime");
    }

    @TruffleBoundary
    public static JSException createRangeErrorSmallestUnitOutOfRange() {
        return Errors.createRangeError("Smallest unit is out of range.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorFieldsNotAnObject() {
        return Errors.createTypeError("Given fields is not an object.");
    }

    @TruffleBoundary
    public static JSException createRangeErrorRelativeToNotUndefined(String unit) {
        return Errors.createRangeError(String.format("RelativeTo object should be not undefined if unit is %s.", unit));
    }

    @TruffleBoundary
    public static JSException createRangeErrorDisallowedField(String property) {
        return Errors.createRangeError(String.format("Property %s is a disallowed field and not 0.", property));
    }

    @TruffleBoundary
    public static JSException createRangeErrorOptionsNotContained(List<?> values, Object value) {
        return Errors.createRangeError(String.format("Given options value: %s is not contained in values: %s", value, values));
    }

    @TruffleBoundary
    public static JSException createTypeErrorPropertyRequired(String property) {
        return Errors.createTypeError(String.format("Property %s is required.", property));
    }

    @TruffleBoundary
    public static JSException createTypeErrorPropertyNotUndefined(String property) {
        return Errors.createTypeError(String.format("Property %s should not be undefined.", property));
    }

    @TruffleBoundary
    public static JSException createRangeErrorTimeOutsideRange() {
        return Errors.createRangeError("Given Time outside the range.");
    }

    @TruffleBoundary
    public static JSException createRangeErrorDateOutsideRange() {
        return Errors.createRangeError("Given Date outside the range.");
    }

    @TruffleBoundary
    public static JSException createRangeErrorDateTimeOutsideRange() {
        return Errors.createRangeError("Given DateTime outside the range.");
    }

    @TruffleBoundary
    public static JSException createRangeErrorYearMonthOutsideRange() {
        return Errors.createRangeError("Given YearMonth outside the range.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalTimeExpected() {
        return Errors.createTypeError("Temporal.PlainTime expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalDateExpected() {
        return Errors.createTypeError("Temporal.PlainDate expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalDateTimeExpected() {
        return Errors.createTypeError("Temporal.PlainDateTime expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalDurationExpected() {
        return Errors.createTypeError("Temporal.Duration expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalCalendarExpected() {
        return Errors.createTypeError("Temporal.Calendar expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalPlainYearMonthExpected() {
        return Errors.createTypeError("Temporal.PlainYearMonth expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalPlainMonthDayExpected() {
        return Errors.createTypeError("Temporal.PlainMonthDay expected");
    }

    @TruffleBoundary
    public static JSException createRangeErrorTemporalISO8601Expected() {
        return Errors.createRangeError("iso8601 expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalTimePropertyExpected() {
        return Errors.createTypeError("No Temporal.Time property found in given object.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorConstructorExpected() {
        return Errors.createTypeError("Constructor expected");
    }

    @TruffleBoundary
    public static JSException createRangeErrorCalendarNotSupported() {
        return Errors.createRangeError("Given calendar id not supported.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorInvalidDate() {
        return Errors.createTypeError("Invalid date");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalYearNotPresent() {
        return Errors.createTypeError("Year not present.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalDayNotPresent() {
        return Errors.createTypeError("Day not present.");
    }

    @TruffleBoundary
    public static JSException createRangeErrorIdenticalCalendarExpected() {
        return Errors.createRangeError("identical calendar expected");
    }

    @TruffleBoundary
    public static JSException createRangeErrorIdenticalTimeZoneExpected() {
        return Errors.createRangeError("identical timeZone expected");
    }

    @TruffleBoundary
    public static JSException createRangeErrorTemporalMalformedDuration() {
        return Errors.createRangeError("malformed Duration");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalInstantExpected() {
        return Errors.createTypeError("Temporal.Instant expected");
    }

    @TruffleBoundary
    public static JSException createRangeErrorInvalidNanoseconds() {
        return Errors.createRangeError("invalid nanoseconds value");
    }

    @TruffleBoundary
    public static JSException createRangeErrorSmallestUnitExpected() {
        return Errors.createRangeError("smallestUnit expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorUnexpectedCalendar() {
        return Errors.createTypeError("Unexpected calendar property");
    }

    @TruffleBoundary
    public static JSException createTypeErrorUnexpectedTimeZone() {
        return Errors.createTypeError("Unexpected timeZone property");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalTimeZoneExpected() {
        return Errors.createTypeError("Temporal.TimeZone expected");
    }

    @TruffleBoundary
    public static JSException createRangeErrorInvalidTimeZoneString() {
        return Errors.createRangeError("invalid timeZone string");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTemporalZonedDateTimeExpected() {
        return Errors.createTypeError("Temporal.ZonedDateTime expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorObjectExpected() {
        return Errors.createTypeError("Object expected");
    }

    @TruffleBoundary
    public static JSException createRangeErrorUnexpectedUTCDesignator() {
        return Errors.createRangeError("UTCDesignator Z not allowed");
    }
}
