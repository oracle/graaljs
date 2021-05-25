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
package com.oracle.truffle.js.builtins;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTES;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.getLong;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimePluralRecord;
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

    public abstract static class JSTemporalDurationFrom extends JSBuiltinNode {

        protected JSTemporalDurationFrom(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object from(Object item,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("create()") JSToStringNode toString) {
            if (isObject.executeBoolean(item) && JSTemporalDuration.isJSTemporalDuration(item)) {
                JSTemporalDurationObject duration = (JSTemporalDurationObject) item;
                return JSTemporalDuration.create(getContext(), duration.getYears(),
                                duration.getMonths(), duration.getWeeks(), duration.getDays(), duration.getHours(),
                                duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                                duration.getMicroseconds(), duration.getNanoseconds());
            }
            return JSTemporalDuration.toTemporalDuration(item, getContext(), isObject, toInt, toString);
        }
    }

    public abstract static class JSTemporalDurationCompare extends JSBuiltinNode {

        protected JSTemporalDurationCompare(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int compare(Object oneParam, Object twoParam, Object optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("create()") JSToStringNode toString) {
            DynamicObject one = (DynamicObject) JSTemporalDuration.toTemporalDuration(oneParam, getContext(), isObject, toInt, toString);
            DynamicObject two = (DynamicObject) JSTemporalDuration.toTemporalDuration(twoParam, getContext(), isObject, toInt, toString);
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext(), isObject);
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(options, getContext());
            long shift1 = JSTemporalDuration.calculateOffsetShift(relativeTo,
                            getLong(one, YEARS, 0),
                            getLong(one, MONTHS, 0),
                            getLong(one, WEEKS, 0),
                            getLong(one, DAYS, 0),
                            getLong(one, HOURS, 0),
                            getLong(one, MINUTES, 0),
                            getLong(one, SECONDS, 0),
                            getLong(one, MILLISECONDS, 0),
                            getLong(one, MICROSECONDS, 0),
                            getLong(one, NANOSECONDS, 0),
                            isObject);
            long shift2 = JSTemporalDuration.calculateOffsetShift(relativeTo,
                            getLong(two, YEARS, 0),
                            getLong(two, MONTHS, 0),
                            getLong(two, WEEKS, 0),
                            getLong(two, DAYS, 0),
                            getLong(two, HOURS, 0),
                            getLong(two, MINUTES, 0),
                            getLong(two, SECONDS, 0),
                            getLong(two, MILLISECONDS, 0),
                            getLong(two, MICROSECONDS, 0),
                            getLong(two, NANOSECONDS, 0),
                            isObject);
            long days1;
            long days2;
            if (getLong(one, YEARS, 0) != 0 || getLong(two, YEARS, 0) != 0 ||
                            getLong(one, MONTHS, 0) != 0 || getLong(two, MONTHS, 0) != 0 ||
                            getLong(one, WEEKS, 0) != 0 || getLong(two, WEEKS, 0) != 0) {
                JSTemporalPlainDateTimePluralRecord balanceResult1 = JSTemporalDuration.unbalanceDurationRelative(
                                getLong(one, YEARS, 0),
                                getLong(one, MONTHS, 0),
                                getLong(one, WEEKS, 0),
                                getLong(one, DAYS, 0),
                                DAYS, relativeTo, getContext());
                JSTemporalPlainDateTimePluralRecord balanceResult2 = JSTemporalDuration.unbalanceDurationRelative(
                                getLong(two, YEARS, 0),
                                getLong(two, MONTHS, 0),
                                getLong(two, WEEKS, 0),
                                getLong(two, DAYS, 0),
                                DAYS, relativeTo, getContext());
                days1 = balanceResult1.getDays();
                days2 = balanceResult2.getDays();
            } else {
                days1 = getLong(one, DAYS, 0);
                days2 = getLong(two, DAYS, 0);
            }
            long ns1 = JSTemporalDuration.totalDurationNanoseconds(days1,
                            getLong(one, HOURS, 0),
                            getLong(one, MINUTES, 0),
                            getLong(one, SECONDS, 0),
                            getLong(one, MILLISECONDS, 0),
                            getLong(one, MICROSECONDS, 0),
                            getLong(one, NANOSECONDS, 0),
                            shift1);
            long ns2 = JSTemporalDuration.totalDurationNanoseconds(days2,
                            getLong(two, HOURS, 0),
                            getLong(two, MINUTES, 0),
                            getLong(two, SECONDS, 0),
                            getLong(two, MILLISECONDS, 0),
                            getLong(two, MICROSECONDS, 0),
                            getLong(two, NANOSECONDS, 0),
                            shift2);
            if (ns1 > ns2) {
                return 1;
            }
            if (ns1 < ns2) {
                return -1;
            }
            return 0;
        }
    }

}
