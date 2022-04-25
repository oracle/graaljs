/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOURS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTES;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECONDS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtol;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.getDouble;

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
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
import com.oracle.truffle.js.nodes.cast.JSNumberToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerWithoutRoundingNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.TemporalBalanceDurationRelativeNode;
import com.oracle.truffle.js.nodes.temporal.TemporalDurationAddNode;
import com.oracle.truffle.js.nodes.temporal.TemporalRoundDurationNode;
import com.oracle.truffle.js.nodes.temporal.TemporalUnbalanceDurationRelativeNode;
import com.oracle.truffle.js.nodes.temporal.ToLimitedTemporalDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToRelativeTemporalObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

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
        protected JSDynamicObject with(Object thisObj, Object temporalDurationLike,
                        @Cached("create()") JSToIntegerWithoutRoundingNode toInt) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            JSDynamicObject durationLike = TemporalUtil.toPartialDuration(temporalDurationLike,
                            getContext(), isObjectNode, toInt, errorBranch);

            double years = getDouble(durationLike, YEARS, duration.getYears());
            double months = getDouble(durationLike, MONTHS, duration.getMonths());
            double weeks = getDouble(durationLike, WEEKS, duration.getWeeks());
            double days = getDouble(durationLike, DAYS, duration.getDays());
            double hours = getDouble(durationLike, HOURS, duration.getHours());
            double minutes = getDouble(durationLike, MINUTES, duration.getMinutes());
            double seconds = getDouble(durationLike, SECONDS, duration.getSeconds());
            double milliseconds = getDouble(durationLike, MILLISECONDS, duration.getMilliseconds());
            double microseconds = getDouble(durationLike, MICROSECONDS, duration.getMicroseconds());
            double nanoseconds = getDouble(durationLike, NANOSECONDS, duration.getNanoseconds());
            return JSTemporalDuration.createTemporalDuration(getContext(), years, months, weeks, days,
                            hours, minutes, seconds, milliseconds, microseconds, nanoseconds, errorBranch);
        }
    }

// 7.3.16
    public abstract static class JSTemporalDurationNegated extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationNegated(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject negated(Object thisObj) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            return JSTemporalDuration.createTemporalDuration(getContext(),
                            -duration.getYears(), -duration.getMonths(), -duration.getWeeks(), -duration.getDays(),
                            -duration.getHours(), -duration.getMinutes(), -duration.getSeconds(), -duration.getMilliseconds(),
                            -duration.getMicroseconds(), -duration.getNanoseconds(), errorBranch);
        }
    }

// 7.3.17
    public abstract static class JSTemporalDurationAbs extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationAbs(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject abs(Object thisObj) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            return JSTemporalDuration.createTemporalDuration(getContext(),
                            Math.abs(duration.getYears()), Math.abs(duration.getMonths()), Math.abs(duration.getWeeks()),
                            Math.abs(duration.getDays()), Math.abs(duration.getHours()), Math.abs(duration.getMinutes()),
                            Math.abs(duration.getSeconds()), Math.abs(duration.getMilliseconds()),
                            Math.abs(duration.getMicroseconds()), Math.abs(duration.getNanoseconds()), errorBranch);
        }
    }

// 7.3.18
    public abstract static class JSTemporalDurationAdd extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject add(Object thisObj, Object other, Object options,
                        @Cached("create(getContext())") TemporalDurationAddNode durationAddNode,
                        @Cached("create(getContext())") ToRelativeTemporalObjectNode toRelativeTemporalObjectNode,
                        @Cached("create()") ToLimitedTemporalDurationNode toLimitedTemporalDurationNode) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            JSTemporalDurationRecord otherDuration = toLimitedTemporalDurationNode.executeDynamicObject(other, TemporalUtil.listEmpty);
            JSDynamicObject normalizedOptions = getOptionsObject(options);
            JSDynamicObject relativeTo = toRelativeTemporalObjectNode.execute(normalizedOptions);
            JSTemporalDurationRecord result = durationAddNode.execute(duration.getYears(), duration.getMonths(),
                            duration.getWeeks(), duration.getDays(), duration.getHours(), duration.getMinutes(),
                            duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(),
                            duration.getNanoseconds(),
                            otherDuration.getYears(), otherDuration.getMonths(), otherDuration.getWeeks(), otherDuration.getDays(),
                            otherDuration.getHours(), otherDuration.getMinutes(), otherDuration.getSeconds(),
                            otherDuration.getMilliseconds(), otherDuration.getMicroseconds(), otherDuration.getNanoseconds(),
                            relativeTo);
            return JSTemporalDuration.createTemporalDuration(getContext(),
                            result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(),
                            result.getMicroseconds(), result.getNanoseconds(), errorBranch);
        }
    }

// 7.3.19
    public abstract static class JSTemporalDurationSubtract extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject subtract(Object thisObj, Object other, Object options,
                        @Cached("create(getContext())") TemporalDurationAddNode durationAddNode,
                        @Cached("create(getContext())") ToRelativeTemporalObjectNode toRelativeTemporalObjectNode,
                        @Cached("create()") ToLimitedTemporalDurationNode toLimitedTemporalDurationNode) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            JSTemporalDurationRecord otherDuration = toLimitedTemporalDurationNode.executeDynamicObject(other, TemporalUtil.listEmpty);
            JSDynamicObject normalizedOptions = getOptionsObject(options);
            JSDynamicObject relativeTo = toRelativeTemporalObjectNode.execute(normalizedOptions);
            JSTemporalDurationRecord result = durationAddNode.execute(duration.getYears(), duration.getMonths(),
                            duration.getWeeks(), duration.getDays(), duration.getHours(), duration.getMinutes(),
                            duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(),
                            duration.getNanoseconds(),
                            -otherDuration.getYears(), -otherDuration.getMonths(), -otherDuration.getWeeks(), -otherDuration.getDays(),
                            -otherDuration.getHours(), -otherDuration.getMinutes(), -otherDuration.getSeconds(),
                            -otherDuration.getMilliseconds(), -otherDuration.getMicroseconds(), -otherDuration.getNanoseconds(),
                            relativeTo);
            return JSTemporalDuration.createTemporalDuration(getContext(),
                            result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(),
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(), errorBranch);
        }
    }

    public abstract static class JSTemporalDurationRound extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject round(Object thisObj, Object roundToParam,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached JSNumberToBigIntNode toBigInt,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext())") TemporalDurationAddNode durationAddNode,
                        @Cached ConditionProfile roundToIsTString,
                        @Cached ConditionProfile realtiveToIsZonedDateTime,
                        @Cached("create(getContext())") ToRelativeTemporalObjectNode toRelativeTemporalObjectNode,
                        @Cached("create(getContext())") TemporalRoundDurationNode roundDurationNode,
                        @Cached("create(getContext())") TemporalUnbalanceDurationRelativeNode unbalanceDurationRelativeNode,
                        @Cached("create(getContext())") TemporalBalanceDurationRelativeNode balanceDurationRelativeNode) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            if (roundToParam == Undefined.instance) {
                throw TemporalErrors.createTypeErrorOptionsUndefined();
            }
            JSDynamicObject roundTo;
            if (roundToIsTString.profile(Strings.isTString(roundToParam))) {
                roundTo = JSOrdinary.createWithNullPrototype(getContext());
                JSRuntime.createDataPropertyOrThrow(roundTo, TemporalConstants.SMALLEST_UNIT, JSRuntime.toStringIsString(roundToParam));
            } else {
                roundTo = getOptionsObject(roundToParam);
            }
            boolean smallestUnitPresent = true;
            boolean largestUnitPresent = true;
            Unit smallestUnit = toSmallestTemporalUnit(roundTo, TemporalUtil.listEmpty, null, equalNode);
            if (smallestUnit == Unit.EMPTY) {
                smallestUnitPresent = false;
                smallestUnit = Unit.NANOSECOND;
            }
            Unit defaultLargestUnit = TemporalUtil.defaultTemporalLargestUnit(duration.getYears(),
                            duration.getMonths(), duration.getWeeks(), duration.getDays(), duration.getHours(),
                            duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                            duration.getMicroseconds());
            defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(defaultLargestUnit, smallestUnit);
            Unit largestUnit = toLargestTemporalUnit(roundTo, TemporalUtil.listEmpty, null, null, equalNode);
            if (largestUnit == Unit.EMPTY) {
                largestUnitPresent = false;
                largestUnit = defaultLargestUnit;
            } else if (Unit.AUTO == largestUnit) {
                largestUnit = defaultLargestUnit;
            }
            if (!smallestUnitPresent && !largestUnitPresent) {
                errorBranch.enter();
                throw Errors.createRangeError("unit expected");
            }
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            RoundingMode roundingMode = toTemporalRoundingMode(roundTo, HALF_EXPAND, equalNode);
            Double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(roundTo, maximum, false, isObjectNode, toNumber);
            JSDynamicObject relativeTo = toRelativeTemporalObjectNode.execute(roundTo);
            JSTemporalDurationRecord unbalanceResult = unbalanceDurationRelativeNode.execute(duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(), largestUnit,
                            relativeTo);
            JSTemporalDurationRecord roundResult = roundDurationNode.execute(
                            unbalanceResult.getYears(), unbalanceResult.getMonths(), unbalanceResult.getWeeks(), unbalanceResult.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                            duration.getMicroseconds(), duration.getNanoseconds(), (long) roundingIncrement, smallestUnit,
                            roundingMode, relativeTo);
            JSTemporalDurationRecord adjustResult = TemporalUtil.adjustRoundedDurationDays(getContext(), namesNode, durationAddNode,
                            roundResult.getYears(), roundResult.getMonths(), roundResult.getWeeks(), roundResult.getDays(), roundResult.getHours(), roundResult.getMinutes(), roundResult.getSeconds(),
                            roundResult.getMilliseconds(), roundResult.getMicroseconds(), roundResult.getNanoseconds(),
                            (long) roundingIncrement, smallestUnit, roundingMode, relativeTo);
            JSTemporalDurationRecord balanceResult = balanceDurationRelativeNode.execute(adjustResult.getYears(), adjustResult.getMonths(), adjustResult.getWeeks(), adjustResult.getDays(),
                            largestUnit, relativeTo);
            if (realtiveToIsZonedDateTime.profile(TemporalUtil.isTemporalZonedDateTime(relativeTo))) {
                relativeTo = TemporalUtil.moveRelativeZonedDateTime(getContext(), relativeTo, dtol(balanceResult.getYears()), dtol(balanceResult.getMonths()), dtol(balanceResult.getWeeks()), 0);
            }
            JSTemporalDurationRecord result = TemporalUtil.balanceDuration(getContext(), namesNode,
                            balanceResult.getDays(), adjustResult.getHours(), adjustResult.getMinutes(), adjustResult.getSeconds(), adjustResult.getMilliseconds(), adjustResult.getMicroseconds(),
                            toBigInt.executeBigInt(adjustResult.getNanoseconds()).bigIntegerValue(), largestUnit, relativeTo);
            return JSTemporalDuration.createTemporalDuration(getContext(),
                            balanceResult.getYears(), balanceResult.getMonths(), balanceResult.getWeeks(),
                            result.getDays(), result.getHours(), result.getMinutes(), result.getSeconds(),
                            result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(), errorBranch);
        }
    }

    // 7.3.21
    public abstract static class JSTemporalDurationTotal extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationTotal(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected double total(Object thisObj, Object totalOfParam,
                        @Cached("createKeys(getContext())") EnumerableOwnPropertyNamesNode namesNode,
                        @Cached JSNumberToBigIntNode toBigIntNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext())") ToRelativeTemporalObjectNode toRelativeTemporalObjectNode,
                        @Cached("create(getContext())") TemporalRoundDurationNode roundDurationNode,
                        @Cached("create(getContext())") TemporalUnbalanceDurationRelativeNode unbalanceDurationRelativeNode) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            if (totalOfParam == Undefined.instance) {
                errorBranch.enter();
                throw TemporalErrors.createTypeErrorOptionsUndefined();
            }
            JSDynamicObject totalOf;
            if (Strings.isTString(totalOfParam)) {
                totalOf = JSOrdinary.createWithNullPrototype(getContext());
                JSRuntime.createDataPropertyOrThrow(totalOf, TemporalConstants.UNIT, JSRuntime.toStringIsString(totalOfParam));
            } else {
                totalOf = getOptionsObject(totalOfParam);
            }
            JSDynamicObject relativeTo = toRelativeTemporalObjectNode.execute(totalOf);
            Unit unit = toTemporalDurationTotalUnit(totalOf, equalNode);
            JSTemporalDurationRecord unbalanceResult = unbalanceDurationRelativeNode.execute(duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(), unit, relativeTo);
            JSDynamicObject intermediate = Undefined.instance;
            if (TemporalUtil.isTemporalZonedDateTime(relativeTo)) {
                intermediate = TemporalUtil.moveRelativeZonedDateTime(getContext(), relativeTo, dtol(unbalanceResult.getYears()), dtol(unbalanceResult.getMonths()), dtol(unbalanceResult.getWeeks()),
                                0);
            }
            JSTemporalDurationRecord balanceResult = TemporalUtil.balanceDuration(getContext(), namesNode, unbalanceResult.getDays(), duration.getHours(), duration.getMinutes(),
                            duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(), toBigIntNode.executeBigInt(duration.getNanoseconds()).bigIntegerValue(), unit, intermediate);
            JSTemporalDurationRecord roundResult = roundDurationNode.execute(unbalanceResult.getYears(), unbalanceResult.getMonths(), unbalanceResult.getWeeks(),
                            balanceResult.getDays(), balanceResult.getHours(), balanceResult.getMinutes(), balanceResult.getSeconds(), balanceResult.getMilliseconds(), balanceResult.getMicroseconds(),
                            balanceResult.getNanoseconds(), 1, unit, RoundingMode.TRUNC, relativeTo);
            double whole = 0;
            if (unit == Unit.YEAR) {
                whole = roundResult.getYears();
            } else if (unit == Unit.MONTH) {
                whole = roundResult.getMonths();
            } else if (unit == Unit.WEEK) {
                whole = roundResult.getWeeks();
            } else if (unit == Unit.DAY) {
                whole = roundResult.getDays();
            } else if (unit == Unit.HOUR) {
                whole = roundResult.getHours();
            } else if (unit == Unit.MINUTE) {
                whole = roundResult.getMinutes();
            } else if (unit == Unit.SECOND) {
                whole = roundResult.getSeconds();
            } else if (unit == Unit.MILLISECOND) {
                whole = roundResult.getMilliseconds();
            } else if (unit == Unit.MICROSECOND) {
                whole = roundResult.getMicroseconds();
            } else {
                assert Unit.NANOSECOND == unit;
                whole = roundResult.getNanoseconds();
            }
            return whole + roundResult.getRemainder();
        }
    }

// 7.3.23 & 7.3.24
    public abstract static class JSTemporalDurationToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(Object thisObj,
                        @Cached JSNumberToBigIntNode toBigIntNode) {
            JSTemporalDurationObject duration = requireTemporalDuration(thisObj);
            return JSTemporalDuration.temporalDurationToString(
                            dtol(duration.getYears()), dtol(duration.getMonths()), dtol(duration.getWeeks()), dtol(duration.getDays()),
                            dtol(duration.getHours()), dtol(duration.getMinutes()), dtol(duration.getSeconds()),
                            dtol(duration.getMilliseconds()), dtol(duration.getMicroseconds()), dtol(duration.getNanoseconds()),
                            AUTO, toBigIntNode);
        }
    }

    public abstract static class JSTemporalDurationToString extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(Object duration, Object opt,
                        @Cached JSNumberToBigIntNode toBigIntNode,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached("create(getContext())") TemporalRoundDurationNode roundDurationNode) {
            JSTemporalDurationObject dur = requireTemporalDuration(duration);
            JSDynamicObject options = getOptionsObject(opt);
            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecision(options, toStringNode, getOptionNode(), equalNode);
            if (precision.getUnit() == Unit.MINUTE) {
                errorBranch.enter();
                throw Errors.createRangeError("unexpected precision minute");
            }
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode);
            JSTemporalDurationRecord result = roundDurationNode.execute(dur.getYears(), dur.getMonths(), dur.getWeeks(), dur.getDays(),
                            dur.getHours(), dur.getMinutes(), dur.getSeconds(), dur.getMilliseconds(), dur.getMicroseconds(),
                            dur.getNanoseconds(), (long) precision.getIncrement(), precision.getUnit(), roundingMode, Undefined.instance);
            return JSTemporalDuration.temporalDurationToString(result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(),
                            result.getHours(), result.getMinutes(), result.getSeconds(),
                            result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            precision.getPrecision(), toBigIntNode);
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
