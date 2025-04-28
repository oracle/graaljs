/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtoi;

import java.util.EnumSet;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeAddSubNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeRoundNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeUntilSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainTimePrototypeBuiltinsFactory.JSTemporalPlainTimeWithNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerWithTruncationNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.GetDifferenceSettingsNode;
import com.oracle.truffle.js.nodes.temporal.GetRoundingIncrementOptionNode;
import com.oracle.truffle.js.nodes.temporal.GetTemporalUnitNode;
import com.oracle.truffle.js.nodes.temporal.IsPartialTemporalObjectNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.ToFractionalSecondDigitsNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Overflow;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

public class TemporalPlainTimePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainTimePrototypeBuiltins.TemporalPlainTimePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainTimePrototypeBuiltins();

    protected TemporalPlainTimePrototypeBuiltins() {
        super(JSTemporalPlainTime.PROTOTYPE_NAME, TemporalPlainTimePrototype.class);
    }

    public enum TemporalPlainTimePrototype implements BuiltinEnum<TemporalPlainTimePrototype> {
        // getters
        calendar(0),
        hour(0),
        minute(0),
        second(0),
        millisecond(0),
        microsecond(0),
        nanosecond(0),

        // methods
        add(1),
        subtract(1),
        with(1),
        until(1),
        since(1),
        round(1),
        equals(1),
        toString(0),
        toLocaleString(0),
        toJSON(0),
        valueOf(0);

        private final int length;

        TemporalPlainTimePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(calendar, hour, minute, second, millisecond, microsecond, nanosecond).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainTimePrototype builtinEnum) {
        switch (builtinEnum) {
            case calendar:
            case hour:
            case minute:
            case second:
            case millisecond:
            case microsecond:
            case nanosecond:
                return JSTemporalPlainTimeGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));
            case add:
                return JSTemporalPlainTimeAddSubNodeGen.create(context, builtin, TemporalUtil.ADD, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case subtract:
                return JSTemporalPlainTimeAddSubNodeGen.create(context, builtin, TemporalUtil.SUBTRACT, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case with:
                return JSTemporalPlainTimeWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case until:
                return JSTemporalPlainTimeUntilSinceNodeGen.create(context, builtin, TemporalUtil.UNTIL, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalPlainTimeUntilSinceNodeGen.create(context, builtin, TemporalUtil.SINCE, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case round:
                return JSTemporalPlainTimeRoundNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case equals:
                return JSTemporalPlainTimeEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toString:
                return JSTemporalPlainTimeToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalPlainTimeToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return UnsupportedValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainTimeGetterNode extends JSBuiltinNode {

        public final TemporalPlainTimePrototype property;

        public JSTemporalPlainTimeGetterNode(JSContext context, JSBuiltin builtin, TemporalPlainTimePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization
        protected final Object timeGetter(JSTemporalPlainTimeObject temporalTime) {
            switch (property) {
                case calendar:
                    return temporalTime.getCalendar();
                case hour:
                    return temporalTime.getHour();
                case minute:
                    return temporalTime.getMinute();
                case second:
                    return temporalTime.getSecond();
                case millisecond:
                    return temporalTime.getMillisecond();
                case microsecond:
                    return temporalTime.getMicrosecond();
                case nanosecond:
                    return temporalTime.getNanosecond();
            }
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalPlainTime(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainTimeAddSubNode extends JSTemporalBuiltinOperation {

        private final int sign;

        protected JSTemporalPlainTimeAddSubNode(JSContext context, JSBuiltin builtin, int sign) {
            super(context, builtin);
            this.sign = sign;
        }

        @Specialization
        public JSTemporalPlainTimeObject addDurationToOrSubtractDurationFromPlainTime(JSTemporalPlainTimeObject temporalTime, Object temporalDurationLike,
                        @Cached ToTemporalDurationNode toTemporalDurationNode,
                        @Cached InlinedBranchProfile errorBranch) {
            JSTemporalDurationObject duration = toTemporalDurationNode.execute(temporalDurationLike);

            TimeRecord result = TemporalUtil.addTimeDouble(
                            temporalTime.getHour(), temporalTime.getMinute(), temporalTime.getSecond(),
                            temporalTime.getMillisecond(), temporalTime.getMicrosecond(), temporalTime.getNanosecond(),
                            sign * duration.getHours(), sign * duration.getMinutes(), sign * duration.getSeconds(),
                            sign * duration.getMilliseconds(), sign * duration.getMicroseconds(), sign * duration.getNanoseconds());
            assert TemporalUtil.isValidTime(result.hour(), result.minute(), result.second(), result.millisecond(), result.microsecond(), result.nanosecond());
            return JSTemporalPlainTime.create(getContext(), getRealm(),
                            result.hour(), result.minute(), result.second(), result.millisecond(), result.microsecond(), result.nanosecond(),
                            this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainTime(thisObj)")
        protected static JSTemporalPlainTimeObject invalidReceiver(Object thisObj, Object temporalDurationLike) {
            throw TemporalErrors.createTypeErrorTemporalPlainTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainTimeWith extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalPlainTimeObject with(JSTemporalPlainTimeObject temporalTime, Object temporalTimeLike, Object options,
                        @Cached IsPartialTemporalObjectNode isPartialTemporalObjectNode,
                        @Cached JSToIntegerWithTruncationNode toIntegerWithTruncation,
                        @Cached JSToIntegerAsIntNode toInt,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            if (!isPartialTemporalObjectNode.execute(temporalTimeLike)) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorPartialTemporalObjectExpected();
            }

            JSDynamicObject partialTime = JSTemporalPlainTime.toPartialTime(temporalTimeLike, toIntegerWithTruncation, getContext());
            int hour;
            int minute;
            int second;
            int millisecond;
            int microsecond;
            int nanosecond;
            Object tempValue = JSObject.get(partialTime, HOUR);
            if (tempValue != Undefined.instance) {
                hour = toInt.executeInt(tempValue);
            } else {
                hour = temporalTime.getHour();
            }
            tempValue = JSObject.get(partialTime, MINUTE);
            if (tempValue != Undefined.instance) {
                minute = toInt.executeInt(tempValue);
            } else {
                minute = temporalTime.getMinute();
            }
            tempValue = JSObject.get(partialTime, SECOND);
            if (tempValue != Undefined.instance) {
                second = toInt.executeInt(tempValue);
            } else {
                second = temporalTime.getSecond();
            }
            tempValue = JSObject.get(partialTime, MILLISECOND);
            if (tempValue != Undefined.instance) {
                millisecond = toInt.executeInt(tempValue);
            } else {
                millisecond = temporalTime.getMillisecond();
            }
            tempValue = JSObject.get(partialTime, MICROSECOND);
            if (tempValue != Undefined.instance) {
                microsecond = toInt.executeInt(tempValue);
            } else {
                microsecond = temporalTime.getMicrosecond();
            }
            tempValue = JSObject.get(partialTime, NANOSECOND);
            if (tempValue != Undefined.instance) {
                nanosecond = toInt.executeInt(tempValue);
            } else {
                nanosecond = temporalTime.getNanosecond();
            }
            JSDynamicObject resolvedOptions = getOptionsObject(options, this, errorBranch, optionUndefined);
            Overflow overflow = TemporalUtil.toTemporalOverflow(resolvedOptions, getOptionNode);
            JSTemporalDurationRecord result = TemporalUtil.regulateTime(hour, minute, second, millisecond, microsecond, nanosecond, overflow);
            return JSTemporalPlainTime.create(getContext(), getRealm(),
                            dtoi(result.getHours()), dtoi(result.getMinutes()), dtoi(result.getSeconds()), dtoi(result.getMilliseconds()), dtoi(result.getMicroseconds()),
                            dtoi(result.getNanoseconds()), this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainTime(thisObj)")
        protected static JSTemporalPlainTimeObject invalidReceiver(Object thisObj, Object temporalTimeLike, Object options) {
            throw TemporalErrors.createTypeErrorTemporalPlainTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainTimeUntilSinceNode extends JSTemporalBuiltinOperation {

        private final int sign;

        protected JSTemporalPlainTimeUntilSinceNode(JSContext context, JSBuiltin builtin, int sign) {
            super(context, builtin);
            this.sign = sign;
        }

        @Specialization
        public JSTemporalDurationObject differenceTemporalPlainTime(JSTemporalPlainTimeObject temporalTime, Object otherObj, Object options,
                        @Cached ToTemporalTimeNode toTemporalTime,
                        @Cached GetDifferenceSettingsNode getDifferenceSettings,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSTemporalPlainTimeObject other = toTemporalTime.execute(otherObj, Undefined.instance);
            JSDynamicObject resolvedOptions = getOptionsObject(options, this, errorBranch, optionUndefined);

            var settings = getDifferenceSettings.execute(sign, resolvedOptions, TemporalUtil.unitMappingTimeOrAuto, TemporalUtil.unitMappingTime, Unit.NANOSECOND, Unit.HOUR);

            BigInt norm = TemporalUtil.differenceTime(
                            temporalTime.getHour(), temporalTime.getMinute(), temporalTime.getSecond(), temporalTime.getMillisecond(), temporalTime.getMicrosecond(),
                            temporalTime.getNanosecond(),
                            other.getHour(), other.getMinute(), other.getSecond(), other.getMillisecond(), other.getMicrosecond(), other.getNanosecond());

            if (settings.smallestUnit() != Unit.NANOSECOND || settings.roundingIncrement() != 1) {
                var roundRecord = TemporalUtil.roundTimeDuration(0, norm, settings.roundingIncrement(), settings.smallestUnit(), settings.roundingMode());
                norm = roundRecord.normalizedDuration().normalizedTimeTotalNanoseconds();
            }

            JSRealm realm = getRealm();
            TimeDurationRecord result = TemporalUtil.balanceTimeDuration(norm, settings.largestUnit());
            return JSTemporalDuration.createTemporalDuration(getContext(), realm, 0, 0, 0, 0,
                            sign * result.hours(), sign * result.minutes(), sign * result.seconds(),
                            sign * result.milliseconds(), sign * result.microseconds(), sign * result.nanoseconds(), this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainTime(thisObj)")
        protected static JSTemporalDurationObject invalidReceiver(Object thisObj, Object otherObj, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainTimeExpected();
        }
    }

    public abstract static class JSTemporalPlainTimeRound extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalPlainTimeObject round(JSTemporalPlainTimeObject temporalTime, Object roundToParam,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached GetTemporalUnitNode getSmallestUnit,
                        @Cached GetRoundingIncrementOptionNode getRoundingIncrementOption,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            if (roundToParam == Undefined.instance) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorOptionsUndefined();
            }
            JSDynamicObject roundTo;
            if (Strings.isTString(roundToParam)) {
                roundTo = JSOrdinary.createWithNullPrototype(getContext());
                JSRuntime.createDataPropertyOrThrow(roundTo, TemporalConstants.SMALLEST_UNIT, roundToParam);
            } else {
                roundTo = getOptionsObject(roundToParam, this, errorBranch, optionUndefined);
            }
            int roundingIncrement = getRoundingIncrementOption.execute(roundTo);
            RoundingMode roundingMode = toTemporalRoundingMode(roundTo, HALF_EXPAND, equalNode, getOptionNode);
            Unit smallestUnit = getSmallestUnit.execute(roundTo, TemporalConstants.SMALLEST_UNIT, TemporalUtil.unitMappingTime, Unit.REQUIRED);
            int maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            TemporalUtil.validateTemporalRoundingIncrement(roundingIncrement, maximum, false, this, errorBranch);
            TimeRecord result = TemporalUtil.roundTime(temporalTime.getHour(), temporalTime.getMinute(),
                            temporalTime.getSecond(), temporalTime.getMillisecond(), temporalTime.getMicrosecond(),
                            temporalTime.getNanosecond(), roundingIncrement, smallestUnit, roundingMode);
            return JSTemporalPlainTime.create(getContext(), getRealm(),
                            result.hour(), result.minute(), result.second(), result.millisecond(), result.microsecond(), result.nanosecond(),
                            this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainTime(thisObj)")
        protected static JSTemporalPlainTimeObject invalidReceiver(Object thisObj, Object roundToParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainTimeExpected();
        }
    }

    // 4.3.16
    public abstract static class JSTemporalPlainTimeEquals extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean equalsOtherObj(JSTemporalPlainTimeObject thisTime, JSTemporalPlainTimeObject otherObj) {
            return equalsIntl(thisTime, otherObj);
        }

        @Specialization(guards = "!isJSTemporalPlainTime(other)")
        protected boolean equalsGeneric(JSTemporalPlainTimeObject thisTime, Object other,
                        @Cached ToTemporalTimeNode toTemporalTime) {
            JSTemporalPlainTimeObject otherTime = toTemporalTime.execute(other, Undefined.instance);
            return equalsIntl(thisTime, otherTime);
        }

        private static boolean equalsIntl(JSTemporalPlainTimeObject thisTime, JSTemporalPlainTimeObject otherTime) {
            if (thisTime.getHour() != otherTime.getHour()) {
                return false;
            }
            if (thisTime.getMinute() != otherTime.getMinute()) {
                return false;
            }
            if (thisTime.getSecond() != otherTime.getSecond()) {
                return false;
            }
            if (thisTime.getMillisecond() != otherTime.getMillisecond()) {
                return false;
            }
            if (thisTime.getMicrosecond() != otherTime.getMicrosecond()) {
                return false;
            }
            if (thisTime.getNanosecond() != otherTime.getNanosecond()) {
                return false;
            }
            return true;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainTime(thisObj)")
        protected static boolean invalidReceiver(Object thisObj, Object otherObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainTimeExpected();
        }
    }

    // 4.3.20
    public abstract static class JSTemporalPlainTimeToString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final TruffleString toString(JSTemporalPlainTimeObject temporalTime, Object optionsParam,
                        @Cached ToFractionalSecondDigitsNode toFractionalSecondDigitsNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached GetTemporalUnitNode getSmallestUnit,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSDynamicObject options = getOptionsObject(optionsParam, this, errorBranch, optionUndefined);
            int digits = toFractionalSecondDigitsNode.execute(options);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TemporalConstants.TRUNC, equalNode, getOptionNode);

            Unit smallestUnit = getSmallestUnit.execute(options, TemporalConstants.SMALLEST_UNIT, TemporalUtil.unitMappingTime, Unit.EMPTY);
            if (smallestUnit == Unit.HOUR) {
                errorBranch.enter(this);
                throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
            }

            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecisionRecord(smallestUnit, digits);

            TimeRecord roundResult = TemporalUtil.roundTime(temporalTime.getHour(), temporalTime.getMinute(), temporalTime.getSecond(),
                            temporalTime.getMillisecond(), temporalTime.getMicrosecond(), temporalTime.getNanosecond(),
                            precision.getIncrement(), precision.getUnit(), roundingMode);
            return JSTemporalPlainTime.temporalTimeToString(
                            roundResult.hour(), roundResult.minute(), roundResult.second(), roundResult.millisecond(), roundResult.microsecond(), roundResult.nanosecond(),
                            precision.getPrecision());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainTime(thisObj)")
        protected static TruffleString invalidReceiver(Object thisObj, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainTimeExpected();
        }
    }

    // 4.3.21
    // 4.3.22
    public abstract static class JSTemporalPlainTimeToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainTimeToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static TruffleString toLocaleString(JSTemporalPlainTimeObject time) {
            return JSTemporalPlainTime.temporalTimeToString(
                            time.getHour(), time.getMinute(), time.getSecond(),
                            time.getMillisecond(), time.getMicrosecond(), time.getNanosecond(),
                            TemporalConstants.AUTO);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalPlainTime(thisObj)")
        protected static TruffleString invalidReceiver(Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainTimeExpected();
        }
    }
}
