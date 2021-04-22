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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDurationObject;
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
                return TemporalDurationFunctionBuiltinsFactory.JSTemporalDurationCompareNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalDurationFrom extends JSBuiltinNode {

        protected JSTemporalDurationFrom(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected Object from(DynamicObject item,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") IsConstructorNode isConstructor,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("createNew()") JSFunctionCallNode callNode,
                        @CachedLibrary("item") DynamicObjectLibrary dol) {
            DynamicObject constructor = getContext().getRealm().getTemporalDurationConstructor();
            if (isObject.executeBoolean(item) && JSTemporalDuration.isJSTemporalDuration(item)) {
                JSTemporalDurationObject duration = (JSTemporalDurationObject) item;
                return JSTemporalDuration.createTemporalDurationFromStatic(constructor, duration.getYears(),
                                duration.getMonths(), duration.getWeeks(), duration.getDays(), duration.getHours(),
                                duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                                duration.getMicroseconds(), duration.getNanoseconds(), isConstructor, callNode);
            }
            return JSTemporalDuration.toTemporalDuration(item, constructor, getContext().getRealm(), isObject,
                            toInt, dol, toString, isConstructor, callNode);
        }
    }

    public abstract static class JSTemporalDurationCompare extends JSBuiltinNode {

        protected JSTemporalDurationCompare(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected int compare(DynamicObject oneParam, DynamicObject twoParam, DynamicObject optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") IsConstructorNode isConstructor,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("createNew()") JSFunctionCallNode callNode,
                        @CachedLibrary("optionsParam") DynamicObjectLibrary dol) {
            try {
                DynamicObject one = (DynamicObject) JSTemporalDuration.toTemporalDuration(oneParam, null, getContext().getRealm(), isObject,
                                toInt, dol, toString, isConstructor, callNode);
                DynamicObject two = (DynamicObject) JSTemporalDuration.toTemporalDuration(twoParam, null, getContext().getRealm(), isObject,
                                toInt, dol, toString, isConstructor, callNode);
                DynamicObject options = TemporalUtil.normalizeOptionsObject(optionsParam, getContext().getRealm(), isObject);
                DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(options, isObject, dol);
                long shift1 = JSTemporalDuration.calculateOffsetShift(relativeTo,
                                dol.getLongOrDefault(one, YEARS, 0),
                                dol.getLongOrDefault(one, MONTHS, 0),
                                dol.getLongOrDefault(one, WEEKS, 0),
                                dol.getLongOrDefault(one, DAYS, 0),
                                dol.getLongOrDefault(one, HOURS, 0),
                                dol.getLongOrDefault(one, MINUTES, 0),
                                dol.getLongOrDefault(one, SECONDS, 0),
                                dol.getLongOrDefault(one, MILLISECONDS, 0),
                                dol.getLongOrDefault(one, MICROSECONDS, 0),
                                dol.getLongOrDefault(one, NANOSECONDS, 0),
                                isObject);
                long shift2 = JSTemporalDuration.calculateOffsetShift(relativeTo,
                                dol.getLongOrDefault(two, YEARS, 0),
                                dol.getLongOrDefault(two, MONTHS, 0),
                                dol.getLongOrDefault(two, WEEKS, 0),
                                dol.getLongOrDefault(two, DAYS, 0),
                                dol.getLongOrDefault(two, HOURS, 0),
                                dol.getLongOrDefault(two, MINUTES, 0),
                                dol.getLongOrDefault(two, SECONDS, 0),
                                dol.getLongOrDefault(two, MILLISECONDS, 0),
                                dol.getLongOrDefault(two, MICROSECONDS, 0),
                                dol.getLongOrDefault(two, NANOSECONDS, 0),
                                isObject);
                long days1, days2;
                if (dol.getLongOrDefault(one, YEARS, 0) != 0 || dol.getLongOrDefault(two, YEARS, 0) != 0 ||
                                dol.getLongOrDefault(one, MONTHS, 0) != 0 || dol.getLongOrDefault(two, MONTHS, 0) != 0 ||
                                dol.getLongOrDefault(one, WEEKS, 0) != 0 || dol.getLongOrDefault(two, WEEKS, 0) != 0) {
                    DynamicObject balanceResult1 = JSTemporalDuration.unbalanceDurationRelative(
                                    dol.getLongOrDefault(one, YEARS, 0),
                                    dol.getLongOrDefault(one, MONTHS, 0),
                                    dol.getLongOrDefault(one, WEEKS, 0),
                                    dol.getLongOrDefault(one, DAYS, 0),
                                    DAYS, relativeTo, dol, getContext().getRealm());
                    DynamicObject balanceResult2 = JSTemporalDuration.unbalanceDurationRelative(
                                    dol.getLongOrDefault(two, YEARS, 0),
                                    dol.getLongOrDefault(two, MONTHS, 0),
                                    dol.getLongOrDefault(two, WEEKS, 0),
                                    dol.getLongOrDefault(two, DAYS, 0),
                                    DAYS, relativeTo, dol, getContext().getRealm());
                    days1 = dol.getLongOrDefault(balanceResult1, DAYS, 0);
                    days2 = dol.getLongOrDefault(balanceResult2, DAYS, 0);
                } else {
                    days1 = dol.getLongOrDefault(one, DAYS, 0);
                    days2 = dol.getLongOrDefault(two, DAYS, 0);
                }
                long ns1 = JSTemporalDuration.totalDurationNanoseconds(days1,
                                dol.getLongOrDefault(one, HOURS, 0),
                                dol.getLongOrDefault(one, MINUTES, 0),
                                dol.getLongOrDefault(one, SECONDS, 0),
                                dol.getLongOrDefault(one, MILLISECONDS, 0),
                                dol.getLongOrDefault(one, MICROSECONDS, 0),
                                dol.getLongOrDefault(one, NANOSECONDS, 0),
                                shift1);
                long ns2 = JSTemporalDuration.totalDurationNanoseconds(days2,
                                dol.getLongOrDefault(two, HOURS, 0),
                                dol.getLongOrDefault(two, MINUTES, 0),
                                dol.getLongOrDefault(two, SECONDS, 0),
                                dol.getLongOrDefault(two, MILLISECONDS, 0),
                                dol.getLongOrDefault(two, MICROSECONDS, 0),
                                dol.getLongOrDefault(two, NANOSECONDS, 0),
                                shift2);
                if (ns1 > ns2) {
                    return 1;
                }
                if (ns1 < ns2) {
                    return -1;
                }
                return 0;
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
