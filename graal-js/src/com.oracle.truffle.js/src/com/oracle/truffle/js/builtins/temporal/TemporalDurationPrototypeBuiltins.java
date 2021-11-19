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
package com.oracle.truffle.js.builtins.temporal;

import static com.oracle.truffle.js.runtime.util.TemporalConstants.AUTO;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAY;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTES;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTH;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEK;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEAR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.getLong;

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationAbsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationAddNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationNegatedNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationRoundNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationSubtractNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationTotalNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationValueOfNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationWithNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltins.JSTemporalBuiltinOperation;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerWithoutRoundingNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalDurationPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalDurationPrototypeBuiltins.TemporalDurationPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalDurationPrototypeBuiltins();

    protected TemporalDurationPrototypeBuiltins() {
        super(JSTemporalDuration.PROTOTYPE_NAME, TemporalDurationPrototype.class);
    }

    public enum TemporalDurationPrototype implements BuiltinEnum<TemporalDurationPrototype> {
        // getters
        years(0),
        months(0),
        weeks(0),
        days(0),
        hours(0),
        minutes(0),
        seconds(0),
        milliseconds(0),
        microseconds(0),
        nanoseconds(0),
        sign(0),
        blank(0),

        // methods
        with(1),
        negated(0),
        abs(0),
        add(1),
        subtract(1),
        round(1),
        total(1),
        toJSON(0),
        toString(0),
        toLocaleString(0),
        valueOf(0);

        private final int length;

        TemporalDurationPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(hours, minutes, seconds, milliseconds, microseconds, nanoseconds, years, months, weeks, days, sign, blank).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalDurationPrototype builtinEnum) {
        switch (builtinEnum) {
            case years:
            case months:
            case weeks:
            case days:
            case hours:
            case minutes:
            case seconds:
            case milliseconds:
            case microseconds:
            case nanoseconds:
            case sign:
            case blank:
                return JSTemporalDurationGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

            case with:
                return JSTemporalDurationWithNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case negated:
                return JSTemporalDurationNegatedNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case abs:
                return JSTemporalDurationAbsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case add:
                return JSTemporalDurationAddNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case subtract:
                return JSTemporalDurationSubtractNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case round:
                return JSTemporalDurationRoundNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case total:
                return JSTemporalDurationTotalNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toJSON:
            case toLocaleString:
                return JSTemporalDurationToLocaleStringNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case toString:
                return JSTemporalDurationToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case valueOf:
                return JSTemporalDurationValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalDurationGetterNode extends JSBuiltinNode {

        public final TemporalDurationPrototype property;

        public JSTemporalDurationGetterNode(JSContext context, JSBuiltin builtin, TemporalDurationPrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization(guards = "isJSTemporalDuration(thisObj)")
        protected Object durationGetter(Object thisObj) {
            JSTemporalDurationObject temporalD = (JSTemporalDurationObject) thisObj;
            switch (property) {
                case hours:
                    return temporalD.getHours();
                case minutes:
                    return temporalD.getMinutes();
                case seconds:
                    return temporalD.getSeconds();
                case milliseconds:
                    return temporalD.getMilliseconds();
                case microseconds:
                    return temporalD.getMicroseconds();
                case nanoseconds:
                    return temporalD.getNanoseconds();
                case years:
                    return temporalD.getYears();
                case months:
                    return temporalD.getMonths();
                case weeks:
                    return temporalD.getWeeks();
                case days:
                    return temporalD.getDays();
                case sign: {
                    return TemporalUtil.durationSign(temporalD.getYears(), temporalD.getMonths(),
                                    temporalD.getWeeks(), temporalD.getDays(),
                                    temporalD.getHours(), temporalD.getMinutes(),
                                    temporalD.getSeconds(), temporalD.getMilliseconds(),
                                    temporalD.getMicroseconds(), temporalD.getNanoseconds());

                }
                case blank: {
                    int sign = TemporalUtil.durationSign(temporalD.getYears(), temporalD.getMonths(),
                                    temporalD.getWeeks(), temporalD.getDays(),
                                    temporalD.getHours(), temporalD.getMinutes(),
                                    temporalD.getSeconds(), temporalD.getMilliseconds(),
                                    temporalD.getMicroseconds(), temporalD.getNanoseconds());
                    return sign == 0;
                }
            }
            CompilerDirectives.transferToInterpreter();
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalDuration(thisObj)")
        protected static int error(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
    }

// 7.3.15
    public abstract static class JSTemporalDurationWith extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject with(Object thisObj, DynamicObject temporalDurationLike,
                        @Cached("create()") JSToIntegerWithoutRoundingNode toInt) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            DynamicObject durationLike = TemporalUtil.toPartialDuration(temporalDurationLike,
                            getContext(), isObjectNode, toInt);

            long years = getLong(durationLike, YEARS, duration.getYears());
            long months = getLong(durationLike, MONTHS, duration.getMonths());
            long weeks = getLong(durationLike, WEEKS, duration.getWeeks());
            long days = getLong(durationLike, DAYS, duration.getDays());
            long hours = getLong(durationLike, HOURS, duration.getHours());
            long minutes = getLong(durationLike, MINUTES, duration.getMinutes());
            long seconds = getLong(durationLike, SECONDS, duration.getSeconds());
            long milliseconds = getLong(durationLike, MILLISECONDS, duration.getMilliseconds());
            long microseconds = getLong(durationLike, MICROSECONDS, duration.getMicroseconds());
            long nanoseconds = getLong(durationLike, NANOSECONDS, duration.getNanoseconds());
            return JSTemporalDuration.create(getContext(), years, months, weeks, days,
                            hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
        }
    }

// 7.3.16
    public abstract static class JSTemporalDurationNegated extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationNegated(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject negated(Object thisObj) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            return JSTemporalDuration.create(getContext(),
                            -duration.getYears(), -duration.getMonths(), -duration.getWeeks(), -duration.getDays(),
                            -duration.getHours(), -duration.getMinutes(), -duration.getSeconds(), -duration.getMilliseconds(),
                            -duration.getMicroseconds(), -duration.getNanoseconds());
        }
    }

// 7.3.17
    public abstract static class JSTemporalDurationAbs extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationAbs(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject abs(Object thisObj) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            return JSTemporalDuration.create(getContext(),
                            Math.abs(duration.getYears()), Math.abs(duration.getMonths()), Math.abs(duration.getWeeks()),
                            Math.abs(duration.getDays()), Math.abs(duration.getHours()), Math.abs(duration.getMinutes()),
                            Math.abs(duration.getSeconds()), Math.abs(duration.getMilliseconds()),
                            Math.abs(duration.getMicroseconds()), Math.abs(duration.getNanoseconds()));
        }
    }

// 7.3.18
    public abstract static class JSTemporalDurationAdd extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject add(Object thisObj, Object other, Object options,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            JSTemporalDurationRecord otherDuration = TemporalUtil.toLimitedTemporalDuration(other, TemporalUtil.listEmpty, isObjectNode, toString);
            DynamicObject normalizedOptions = getOptionsObject(options);
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(getContext(), getRealm(), normalizedOptions);
            JSTemporalDurationRecord result = TemporalUtil.addDuration(getContext(), namesNode, duration.getYears(), duration.getMonths(),
                            duration.getWeeks(), duration.getDays(), duration.getHours(), duration.getMinutes(),
                            duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(),
                            duration.getNanoseconds(),
                            otherDuration.getYears(), otherDuration.getMonths(), otherDuration.getWeeks(), otherDuration.getDays(),
                            otherDuration.getHours(), otherDuration.getMinutes(), otherDuration.getSeconds(),
                            otherDuration.getMilliseconds(), otherDuration.getMicroseconds(), otherDuration.getNanoseconds(),
                            relativeTo);
            return JSTemporalDuration.create(getContext(),
                            result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(),
                            result.getMicroseconds(), result.getNanoseconds());
        }
    }

// 7.3.19
    public abstract static class JSTemporalDurationSubtract extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject subtract(Object thisObj, Object other, Object options,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            JSTemporalDurationRecord otherDuration = TemporalUtil.toLimitedTemporalDuration(other, TemporalUtil.listEmpty, isObjectNode, toString);
            DynamicObject normalizedOptions = getOptionsObject(options);
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(getContext(), getRealm(), normalizedOptions);
            JSTemporalDurationRecord result = TemporalUtil.addDuration(getContext(), namesNode, duration.getYears(), duration.getMonths(),
                            duration.getWeeks(), duration.getDays(), duration.getHours(), duration.getMinutes(),
                            duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(),
                            duration.getNanoseconds(),
                            -otherDuration.getYears(), -otherDuration.getMonths(), -otherDuration.getWeeks(), -otherDuration.getDays(),
                            -otherDuration.getHours(), -otherDuration.getMinutes(), -otherDuration.getSeconds(),
                            -otherDuration.getMilliseconds(), -otherDuration.getMicroseconds(), -otherDuration.getNanoseconds(),
                            relativeTo);
            return JSTemporalDuration.create(getContext(),
                            result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(),
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds());
        }
    }

// 7.3.20
    public abstract static class JSTemporalDurationRound extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject round(Object thisObj, Object roundToParam,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            if (roundToParam == Undefined.instance) {
                throw TemporalErrors.createTypeErrorOptionsUndefined();
            }
            DynamicObject roundTo;
            if (JSRuntime.isString(roundToParam)) {
                roundTo = JSOrdinary.createWithNullPrototype(getContext());
                JSRuntime.createDataPropertyOrThrow(roundTo, TemporalConstants.SMALLEST_UNIT, JSRuntime.toStringIsString(roundToParam));
            } else {
                roundTo = getOptionsObject(roundToParam);
            }
            boolean smallestUnitPresent = true;
            boolean largestUnitPresent = true;
            String smallestUnit = toSmallestTemporalUnit(roundTo, TemporalUtil.listEmpty, null);
            if (smallestUnit == null) {
                smallestUnitPresent = false;
                smallestUnit = NANOSECOND;
            }
            String defaultLargestUnit = TemporalUtil.defaultTemporalLargestUnit(duration.getYears(),
                            duration.getMonths(), duration.getWeeks(), duration.getDays(), duration.getHours(),
                            duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                            duration.getMicroseconds());
            defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(defaultLargestUnit, smallestUnit);
            String largestUnit = toLargestTemporalUnit(roundTo, TemporalUtil.listEmpty, null, null);
            if (largestUnit == null) {
                largestUnitPresent = false;
                largestUnit = defaultLargestUnit;
            } else if (AUTO.equals(largestUnit)) {
                largestUnit = defaultLargestUnit;
            }
            if (!smallestUnitPresent && !largestUnitPresent) {
                throw Errors.createRangeError("unit expected");
            }
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            String roundingMode = toTemporalRoundingMode(roundTo, HALF_EXPAND);
            Double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(roundTo, maximum, false, isObjectNode, toNumber);
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(getContext(), getRealm(), roundTo);
            JSTemporalDurationRecord unbalanceResult = TemporalUtil.unbalanceDurationRelative(getContext(), getRealm(),
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(), largestUnit,
                            relativeTo);
            JSTemporalDurationRecord roundResult = TemporalUtil.roundDuration(getContext(), namesNode,
                            unbalanceResult.getYears(), unbalanceResult.getMonths(), unbalanceResult.getWeeks(), unbalanceResult.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                            duration.getMicroseconds(), duration.getNanoseconds(), (long) roundingIncrement, smallestUnit,
                            roundingMode, relativeTo);
            JSTemporalDurationRecord adjustResult = TemporalUtil.adjustRoundedDurationDays(getContext(), namesNode,
                            roundResult.getYears(), roundResult.getMonths(), roundResult.getWeeks(), roundResult.getDays(), roundResult.getHours(), roundResult.getMinutes(), roundResult.getSeconds(),
                            roundResult.getMilliseconds(), roundResult.getMicroseconds(), roundResult.getNanoseconds(),
                            (long) roundingIncrement, smallestUnit, roundingMode, relativeTo);
            JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDurationRelative(
                            getContext(), getRealm(), adjustResult.getYears(), adjustResult.getMonths(), adjustResult.getWeeks(),
                            adjustResult.getDays(), largestUnit, relativeTo);
            if (TemporalUtil.isTemporalZonedDateTime(relativeTo)) {
                relativeTo = TemporalUtil.moveRelativeZonedDateTime(getContext(), relativeTo, balanceResult.getYears(), balanceResult.getMonths(), balanceResult.getWeeks(), 0);
            }
            JSTemporalDurationRecord result = TemporalUtil.balanceDuration(getContext(), namesNode,
                            balanceResult.getDays(), adjustResult.getHours(), adjustResult.getMinutes(), adjustResult.getSeconds(), adjustResult.getMilliseconds(), adjustResult.getMicroseconds(),
                            adjustResult.getNanoseconds(), largestUnit, relativeTo);
            return JSTemporalDuration.create(getContext(),
                            balanceResult.getYears(), balanceResult.getMonths(), balanceResult.getWeeks(),
                            result.getDays(), result.getHours(), result.getMinutes(), result.getSeconds(),
                            result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds());
        }
    }

    // 7.3.21
    public abstract static class JSTemporalDurationTotal extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationTotal(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected long total(Object thisObj, Object totalOfParam,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            if (totalOfParam == Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorOptionsUndefined();
            }
            DynamicObject totalOf;
            if (JSRuntime.isString(totalOfParam)) {
                totalOf = JSOrdinary.createWithNullPrototype(getContext());
                JSRuntime.createDataPropertyOrThrow(totalOf, TemporalConstants.SMALLEST_UNIT, JSRuntime.toStringIsString(totalOfParam));
            } else {
                totalOf = getOptionsObject(totalOfParam);
            }
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(getContext(), getRealm(), totalOf);
            String unit = toTemporalDurationTotalUnit(totalOf);
            JSTemporalDurationRecord unbalanceResult = TemporalUtil.unbalanceDurationRelative(getContext(), getRealm(),
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(), unit, relativeTo);
            DynamicObject intermediate = Undefined.instance;
            if (TemporalUtil.isTemporalZonedDateTime(relativeTo)) {
                intermediate = TemporalUtil.moveRelativeZonedDateTime(getContext(), relativeTo, unbalanceResult.getYears(), unbalanceResult.getMonths(), unbalanceResult.getWeeks(), 0);
            }
            JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDuration(getContext(), namesNode, unbalanceResult.getDays(), duration.getHours(), duration.getMinutes(),
                            duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), unit, intermediate);
            JSTemporalDurationRecord roundResult = TemporalUtil.roundDuration(getContext(), namesNode, unbalanceResult.getYears(), unbalanceResult.getMonths(), unbalanceResult.getWeeks(),
                            balanceResult.getDays(), balanceResult.getHours(), balanceResult.getMinutes(), balanceResult.getSeconds(), balanceResult.getMilliseconds(), balanceResult.getMicroseconds(),
                            balanceResult.getNanoseconds(), 1, unit, TRUNC, relativeTo);
            long whole = 0;
            if (unit.equals(YEAR)) {
                whole = roundResult.getYears();
            } else if (unit.equals(MONTH)) {
                whole = roundResult.getMonths();
            } else if (unit.equals(WEEK)) {
                whole = roundResult.getWeeks();
            } else if (unit.equals(DAY)) {
                whole = roundResult.getDays();
            } else if (unit.equals(HOUR)) {
                whole = roundResult.getHours();
            } else if (unit.equals(MINUTE)) {
                whole = roundResult.getMinutes();
            } else if (unit.equals(SECOND)) {
                whole = roundResult.getSeconds();
            } else if (unit.equals(MILLISECOND)) {
                whole = roundResult.getMilliseconds();
            } else if (unit.equals(MICROSECOND)) {
                whole = roundResult.getMicroseconds();
            } else {
                assert NANOSECOND.equals(unit);
                whole = roundResult.getNanoseconds();
            }
            return whole + (long) roundResult.getRemainder();
        }
    }

// 7.3.23 & 7.3.24
    public abstract static class JSTemporalDurationToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(Object thisObj) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            return JSTemporalDuration.temporalDurationToString(
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(),
                            AUTO);
        }
    }

    public abstract static class JSTemporalDurationToString extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(Object duration, Object opt,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode) {
            JSTemporalDurationObject dur = requireTemporalDuration(duration);
            DynamicObject options = getOptionsObject(opt);
            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecision(options, getOptionNode());
            if (precision.getUnit().equals(MINUTE)) {
                errorBranch.enter();
                throw Errors.createRangeError("unexpected precision minute");
            }
            String roundingMode = toTemporalRoundingMode(options, TRUNC);
            JSTemporalDurationRecord result = TemporalUtil.roundDuration(getContext(), namesNode,
                            dur.getYears(), dur.getMonths(), dur.getWeeks(), dur.getDays(), dur.getHours(), dur.getMinutes(), dur.getSeconds(), dur.getMilliseconds(), dur.getMicroseconds(),
                            dur.getNanoseconds(), (long) precision.getIncrement(), precision.getUnit(), roundingMode, Undefined.instance);
            return JSTemporalDuration.temporalDurationToString(result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(),
                            result.getHours(), result.getMinutes(), result.getSeconds(),
                            result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            precision.getPrecision());
        }
    }

// 7.3.26
    public abstract static class JSTemporalDurationValueOf extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }
}
