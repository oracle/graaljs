/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.nodes.JSGuards.isUndefined;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerWithoutRoundingNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.util.TemporalConstants;

/**
 * Implementation of ToTemporalPartialDurationRecord(temporalDurationLike) operation.
 */
@ImportStatic(TemporalConstants.class)
public abstract class ToTemporalPartialDurationRecordNode extends JavaScriptBaseNode {

    protected ToTemporalPartialDurationRecordNode() {
    }

    public abstract JSTemporalDurationRecord execute(Object temporalDurationLike, JSTemporalDurationRecord defaults);

    @Specialization
    protected final JSTemporalDurationRecord toTemporalPartialDurationRecord(Object temporalDurationLike, JSTemporalDurationRecord defaults,
                    @Cached IsObjectNode isObjectNode,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached JSToIntegerWithoutRoundingNode toIntegerIfIntegral,
                    @Cached("create(DAYS, getJSContext())") PropertyGetNode getDays,
                    @Cached("create(HOURS, getJSContext())") PropertyGetNode getHours,
                    @Cached("create(MICROSECONDS, getJSContext())") PropertyGetNode getMicroseconds,
                    @Cached("create(MILLISECONDS, getJSContext())") PropertyGetNode getMilliseconds,
                    @Cached("create(MINUTES, getJSContext())") PropertyGetNode getMinutes,
                    @Cached("create(MONTHS, getJSContext())") PropertyGetNode getMonths,
                    @Cached("create(NANOSECONDS, getJSContext())") PropertyGetNode getNanoseconds,
                    @Cached("create(SECONDS, getJSContext())") PropertyGetNode getSeconds,
                    @Cached("create(WEEKS, getJSContext())") PropertyGetNode getWeeks,
                    @Cached("create(YEARS, getJSContext())") PropertyGetNode getYears) {
        if (!isObjectNode.executeBoolean(temporalDurationLike)) {
            errorBranch.enter(this);
            throw Errors.createTypeError("Given duration like is not a object.");
        }
        Object daysValue = getDays.getValue(temporalDurationLike);
        double days = isUndefined(daysValue) ? defaults.getDays() : toIntegerIfIntegral.executeDouble(daysValue);
        Object hoursValue = getHours.getValue(temporalDurationLike);
        double hours = isUndefined(hoursValue) ? defaults.getHours() : toIntegerIfIntegral.executeDouble(hoursValue);
        Object microsecondsValue = getMicroseconds.getValue(temporalDurationLike);
        double microseconds = isUndefined(microsecondsValue) ? defaults.getMicroseconds() : toIntegerIfIntegral.executeDouble(microsecondsValue);
        Object millisecondsValue = getMilliseconds.getValue(temporalDurationLike);
        double milliseconds = isUndefined(millisecondsValue) ? defaults.getMilliseconds() : toIntegerIfIntegral.executeDouble(millisecondsValue);
        Object minutesValue = getMinutes.getValue(temporalDurationLike);
        double minutes = isUndefined(minutesValue) ? defaults.getMinutes() : toIntegerIfIntegral.executeDouble(minutesValue);
        Object monthsValue = getMonths.getValue(temporalDurationLike);
        double months = isUndefined(monthsValue) ? defaults.getMonths() : toIntegerIfIntegral.executeDouble(monthsValue);
        Object nanosecondsValue = getNanoseconds.getValue(temporalDurationLike);
        double nanoseconds = isUndefined(nanosecondsValue) ? defaults.getNanoseconds() : toIntegerIfIntegral.executeDouble(nanosecondsValue);
        Object secondsValue = getSeconds.getValue(temporalDurationLike);
        double seconds = isUndefined(secondsValue) ? defaults.getSeconds() : toIntegerIfIntegral.executeDouble(secondsValue);
        Object weeksValue = getWeeks.getValue(temporalDurationLike);
        double weeks = isUndefined(weeksValue) ? defaults.getWeeks() : toIntegerIfIntegral.executeDouble(weeksValue);
        Object yearsValue = getYears.getValue(temporalDurationLike);
        double years = isUndefined(yearsValue) ? defaults.getYears() : toIntegerIfIntegral.executeDouble(yearsValue);
        if (isUndefined(yearsValue) &&
                        isUndefined(monthsValue) &&
                        isUndefined(weeksValue) &&
                        isUndefined(daysValue) &&
                        isUndefined(hoursValue) &&
                        isUndefined(minutesValue) &&
                        isUndefined(secondsValue) &&
                        isUndefined(millisecondsValue) &&
                        isUndefined(microsecondsValue) &&
                        isUndefined(nanosecondsValue)) {
            errorBranch.enter(this);
            throw Errors.createTypeError("Given duration like object has no duration properties.");
        }
        return JSTemporalDurationRecord.createWeeks(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    }
}
