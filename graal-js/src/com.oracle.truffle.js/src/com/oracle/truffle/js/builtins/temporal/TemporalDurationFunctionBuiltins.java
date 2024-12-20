/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.temporal;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.temporal.CalendarMethodsRecordLookupNode;
import com.oracle.truffle.js.nodes.temporal.TemporalAddZonedDateTimeNode;
import com.oracle.truffle.js.nodes.temporal.TemporalUnbalanceDateDurationRelativeNode;
import com.oracle.truffle.js.nodes.temporal.ToRelativeTemporalObjectNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDurationNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.temporal.CalendarMethodsRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalDurationFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalDurationFunctionBuiltins.TemporalDurationFunction> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalDurationFunctionBuiltins();

    protected TemporalDurationFunctionBuiltins() {
        super(JSTemporalDuration.CLASS_NAME, TemporalDurationFunction.class);
    }

    public enum TemporalDurationFunction implements BuiltinEnum<TemporalDurationFunction> {
        from(1),
        compare(2);

        private final int length;

        TemporalDurationFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalDurationFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return TemporalDurationFunctionBuiltinsFactory.JSTemporalDurationFromNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case compare:
                return TemporalDurationFunctionBuiltinsFactory.JSTemporalDurationCompareNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalDurationFrom extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationFrom(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalDurationObject from(Object item,
                        @Cached ToTemporalDurationNode toTemporalDurationNode,
                        @Cached InlinedBranchProfile errorBranch) {
            if (isObject(item) && JSTemporalDuration.isJSTemporalDuration(item)) {
                JSTemporalDurationObject duration = (JSTemporalDurationObject) item;
                return JSTemporalDuration.createTemporalDuration(getContext(), getRealm(),
                                duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                                duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                                duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), this, errorBranch);
            }
            return toTemporalDurationNode.execute(item);
        }
    }

    public abstract static class JSTemporalDurationCompare extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationCompare(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int compare(Object oneParam, Object twoParam, Object optionsParam,
                        @Cached ToTemporalDurationNode toTemporalDurationNode,
                        @Cached("createDateAdd()") CalendarMethodsRecordLookupNode lookupDateAdd,
                        @Cached ToRelativeTemporalObjectNode toRelativeTemporalObjectNode,
                        @Cached TemporalUnbalanceDateDurationRelativeNode unbalanceDurationRelativeNode,
                        @Cached TemporalAddZonedDateTimeNode addZonedDateTimeNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSTemporalDurationObject one = toTemporalDurationNode.execute(oneParam);
            JSTemporalDurationObject two = toTemporalDurationNode.execute(twoParam);
            JSDynamicObject options = getOptionsObject(optionsParam, this, errorBranch, optionUndefined);
            var relativeToRec = toRelativeTemporalObjectNode.execute(options);

            if (one.getYears() == two.getYears() && one.getMonths() == two.getMonths() && one.getWeeks() == two.getWeeks() && one.getDays() == two.getDays() && one.getHours() == two.getHours() &&
                            one.getMinutes() == two.getMinutes() && one.getSeconds() == two.getSeconds() && one.getMilliseconds() == two.getMilliseconds() &&
                            one.getMicroseconds() == two.getMicroseconds() && one.getNanoseconds() == two.getNanoseconds()) {
                return 0;
            }

            JSTemporalZonedDateTimeObject zonedRelativeTo = relativeToRec.zonedRelativeTo();
            JSTemporalPlainDateObject plainRelativeTo = relativeToRec.plainRelativeTo();
            CalendarMethodsRecord calendarRec = relativeToRec.createCalendarMethodsRecord(lookupDateAdd, null);

            boolean calendarUnitsPresent = one.getYears() != 0 || two.getYears() != 0 ||
                            one.getMonths() != 0 || two.getMonths() != 0 ||
                            one.getWeeks() != 0 || two.getWeeks() != 0;

            JSRealm realm = getRealm();
            if (zonedRelativeTo != null && (calendarUnitsPresent || one.getDays() != 0 || two.getDays() != 0)) {
                TruffleString timeZone = zonedRelativeTo.getTimeZone();
                var instant = JSTemporalInstant.create(getContext(), realm, zonedRelativeTo.getNanoseconds());
                JSTemporalPlainDateTimeObject precalculatedPlainDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), realm, timeZone, instant, calendarRec.receiver());

                BigInt norm1 = TemporalUtil.normalizeTimeDuration(one.getHours(), one.getMinutes(), one.getSeconds(), one.getMilliseconds(), one.getMicroseconds(), one.getNanoseconds());
                BigInt norm2 = TemporalUtil.normalizeTimeDuration(two.getHours(), two.getMinutes(), two.getSeconds(), two.getMilliseconds(), two.getMicroseconds(), two.getNanoseconds());
                var after1 = addZonedDateTimeNode.execute(
                                zonedRelativeTo.getNanoseconds(), timeZone, calendarRec,
                                one.getYears(), one.getMonths(), one.getWeeks(), one.getDays(),
                                norm1, precalculatedPlainDateTime);
                var after2 = addZonedDateTimeNode.execute(
                                zonedRelativeTo.getNanoseconds(), timeZone, calendarRec,
                                two.getYears(), two.getMonths(), two.getWeeks(), two.getDays(),
                                norm2, precalculatedPlainDateTime);
                return after1.compareTo(after2);
            }
            double days1;
            double days2;
            if (calendarUnitsPresent) {
                if (plainRelativeTo == null) {
                    throw Errors.createRangeError("A starting point is required for years, months, or weeks comparison");
                }
                days1 = unbalanceDurationRelativeNode.execute(one.getYears(), one.getMonths(), one.getWeeks(), one.getDays(), plainRelativeTo, calendarRec);
                days2 = unbalanceDurationRelativeNode.execute(two.getYears(), two.getMonths(), two.getWeeks(), two.getDays(), plainRelativeTo, calendarRec);
            } else {
                days1 = one.getDays();
                days2 = two.getDays();
            }
            BigInt ns1 = TemporalUtil.totalDurationNanoseconds(days1,
                            one.getHours(), one.getMinutes(), one.getSeconds(),
                            one.getMilliseconds(), one.getMicroseconds(), one.getNanoseconds());
            BigInt ns2 = TemporalUtil.totalDurationNanoseconds(days2,
                            two.getHours(), two.getMinutes(), two.getSeconds(),
                            two.getMilliseconds(), two.getMicroseconds(), two.getNanoseconds());
            return ns1.compareTo(ns2);
        }
    }

}
