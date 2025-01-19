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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.AUTO;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantAddSubNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantRoundNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantToZonedDateTimeISONodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantUntilSinceNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.GetDifferenceSettingsNode;
import com.oracle.truffle.js.nodes.temporal.GetRoundingIncrementOptionNode;
import com.oracle.truffle.js.nodes.temporal.GetTemporalUnitNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.ToFractionalSecondDigitsNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalInstantNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneIdentifierNode;
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
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeDurationRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

public class TemporalInstantPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalInstantPrototypeBuiltins.TemporalInstantPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalInstantPrototypeBuiltins();

    protected TemporalInstantPrototypeBuiltins() {
        super(JSTemporalInstant.PROTOTYPE_NAME, TemporalInstantPrototype.class);
    }

    public enum TemporalInstantPrototype implements BuiltinEnum<TemporalInstantPrototype> {
        // getters
        epochSeconds(0),
        epochMilliseconds(0),
        epochMicroseconds(0),
        epochNanoseconds(0),

        // methods
        add(1),
        subtract(1),
        until(1),
        since(1),
        round(1),
        equals(1),
        toString(0),
        toLocaleString(0),
        toJSON(0),
        valueOf(0),
        toZonedDateTimeISO(1);

        private final int length;

        TemporalInstantPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(epochSeconds, epochMilliseconds, epochMicroseconds, epochNanoseconds).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalInstantPrototype builtinEnum) {
        switch (builtinEnum) {
            case epochSeconds:
            case epochMilliseconds:
            case epochMicroseconds:
            case epochNanoseconds:
                return JSTemporalInstantGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

            case add:
                return JSTemporalInstantAddSubNodeGen.create(context, builtin, TemporalUtil.ADD, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case subtract:
                return JSTemporalInstantAddSubNodeGen.create(context, builtin, TemporalUtil.SUBTRACT, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case until:
                return JSTemporalInstantUntilSinceNodeGen.create(context, builtin, TemporalUtil.UNTIL, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalInstantUntilSinceNodeGen.create(context, builtin, TemporalUtil.SINCE, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case round:
                return JSTemporalInstantRoundNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case equals:
                return JSTemporalInstantEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toString:
                return JSTemporalInstantToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalInstantToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return UnsupportedValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toZonedDateTimeISO:
                return JSTemporalInstantToZonedDateTimeISONodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));

        }
        return null;
    }

    public abstract static class JSTemporalInstantGetterNode extends JSBuiltinNode {

        protected final TemporalInstantPrototype property;

        protected JSTemporalInstantGetterNode(JSContext context, JSBuiltin builtin, TemporalInstantPrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @TruffleBoundary
        @Specialization
        protected Object instantGetter(JSTemporalInstantObject instant) {
            BigInt ns = instant.getNanoseconds();
            switch (property) {
                // roundTowardsZero is a no-op for BigIntegers
                case epochSeconds:
                    return ns.divide(TemporalUtil.BI_NS_PER_SECOND).doubleValue();
                case epochMilliseconds:
                    return ns.divide(TemporalUtil.BI_NS_PER_MS).doubleValue();
                case epochMicroseconds:
                    return ns.divide(TemporalUtil.BI_1000);
                case epochNanoseconds:
                    return instant.getNanoseconds();
            }
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalInstant(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
    }

    public abstract static class JSTemporalInstantAddSubNode extends JSTemporalBuiltinOperation {

        private final int sign;

        protected JSTemporalInstantAddSubNode(JSContext context, JSBuiltin builtin, int sign) {
            super(context, builtin);
            this.sign = sign;
        }

        @Specialization
        protected JSTemporalInstantObject addDurationToOrSubtractDurationFromInstant(JSTemporalInstantObject instant, Object temporalDurationLike,
                        @Cached ToTemporalDurationNode toTemporalDurationNode,
                        @Cached InlinedBranchProfile errorBranch) {
            JSTemporalDurationObject duration = toTemporalDurationNode.execute(temporalDurationLike);
            if (duration.getDays() != 0 || duration.getMonths() != 0 || duration.getWeeks() != 0 || duration.getYears() != 0) {
                errorBranch.enter(this);
                throw Errors.createRangeError("Temporal.Instant does not support adding or subtracting a Duration with non-zero days, months, weeks, or years.");
            }

            BigInt ns = TemporalUtil.addInstant(instant.getNanoseconds(), sign * duration.getHours(), sign * duration.getMinutes(), sign * duration.getSeconds(),
                            sign * duration.getMilliseconds(), sign * duration.getMicroseconds(), sign * duration.getNanoseconds());
            return JSTemporalInstant.create(getContext(), getRealm(), ns);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalInstant(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object temporalDurationLike) {
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
    }

    public abstract static class JSTemporalInstantUntilSinceNode extends JSTemporalBuiltinOperation {

        private final int sign;

        protected JSTemporalInstantUntilSinceNode(JSContext context, JSBuiltin builtin, int sign) {
            super(context, builtin);
            this.sign = sign;
        }

        @Specialization
        protected JSTemporalDurationObject differenceTemporalInstant(JSTemporalInstantObject instant, Object otherObj, Object options,
                        @Cached ToTemporalInstantNode toTemporalInstantNode,
                        @Cached GetDifferenceSettingsNode getDifferenceSettings,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSTemporalInstantObject other = toTemporalInstantNode.execute(otherObj);
            JSDynamicObject resolvedOptions = getOptionsObject(options, this, errorBranch, optionUndefined);
            var settings = getDifferenceSettings.execute(sign, resolvedOptions, TemporalUtil.unitMappingTimeOrAuto, TemporalUtil.unitMappingTime, Unit.NANOSECOND, Unit.SECOND);

            var diffRecord = TemporalUtil.differenceInstant(instant.getNanoseconds(), other.getNanoseconds(),
                            settings.roundingIncrement(), settings.smallestUnit(), settings.roundingMode());
            BigInt norm = diffRecord.normalizedTimeDuration();
            TimeDurationRecord result = TemporalUtil.balanceTimeDuration(norm, settings.largestUnit());
            JSRealm realm = getRealm();
            return JSTemporalDuration.createTemporalDuration(getContext(), realm, 0, 0, 0, 0,
                            sign * result.hours(), sign * result.minutes(), sign * result.seconds(), sign * result.milliseconds(), sign * result.microseconds(), sign * result.nanoseconds(),
                            this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalInstant(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object otherObj, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
    }

    public abstract static class JSTemporalInstantRound extends JSTemporalBuiltinOperation {

        protected JSTemporalInstantRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalInstantObject round(JSTemporalInstantObject instant, Object roundToParam,
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
            long maximum;
            if (Unit.HOUR == smallestUnit) {
                maximum = TemporalUtil.HOURS_PER_DAY;
            } else if (Unit.MINUTE == smallestUnit) {
                maximum = TemporalUtil.MINUTES_PER_HOUR * TemporalUtil.HOURS_PER_DAY;
            } else if (Unit.SECOND == smallestUnit) {
                maximum = TemporalUtil.SECONDS_PER_MINUTE * TemporalUtil.MINUTES_PER_HOUR * TemporalUtil.HOURS_PER_DAY;
            } else if (Unit.MILLISECOND == smallestUnit) {
                maximum = TemporalUtil.MS_PER_DAY;
            } else if (Unit.MICROSECOND == smallestUnit) {
                maximum = TemporalUtil.MS_PER_DAY * 1000;
            } else {
                assert Unit.NANOSECOND == smallestUnit;
                maximum = TemporalUtil.NS_PER_DAY_LONG;
            }
            TemporalUtil.validateTemporalRoundingIncrement(roundingIncrement, maximum, true, this, errorBranch);
            BigInt roundedNs = TemporalUtil.roundTemporalInstant(instant.getNanoseconds(), roundingIncrement, smallestUnit, roundingMode);
            return JSTemporalInstant.create(getContext(), getRealm(), roundedNs);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalInstant(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object roundToParam) {
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
    }

    public abstract static class JSTemporalInstantEquals extends JSTemporalBuiltinOperation {

        protected JSTemporalInstantEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean equals(JSTemporalInstantObject instant, Object otherObj,
                        @Cached ToTemporalInstantNode toTemporalInstantNode) {
            JSTemporalInstantObject other = toTemporalInstantNode.execute(otherObj);
            return instant.getNanoseconds().compareTo(other.getNanoseconds()) == 0;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalInstant(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object otherObj) {
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
    }

    @ImportStatic(TemporalConstants.class)
    public abstract static class JSTemporalInstantToString extends JSTemporalBuiltinOperation {

        protected JSTemporalInstantToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(JSTemporalInstantObject instant, Object optionsParam,
                        @Cached ToFractionalSecondDigitsNode toFractionalSecondDigits,
                        @Cached ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifier,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached GetTemporalUnitNode getSmallestUnit,
                        @Cached("create(TIME_ZONE, getContext())") PropertyGetNode getTimeZone,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSDynamicObject options = getOptionsObject(optionsParam, this, errorBranch, optionUndefined);
            int digits = toFractionalSecondDigits.execute(options);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode, getOptionNode);

            Unit smallestUnit = getSmallestUnit.execute(options, TemporalConstants.SMALLEST_UNIT, TemporalUtil.unitMappingTime, Unit.EMPTY);
            if (smallestUnit == Unit.HOUR) {
                errorBranch.enter(this);
                throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
            }

            Object timeZone = getTimeZone.getValue(options);
            if (timeZone != Undefined.instance) {
                timeZone = toTimeZoneIdentifier.execute(timeZone);
            }

            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecisionRecord(smallestUnit, digits);
            BigInt roundedNs = TemporalUtil.roundTemporalInstant(instant.getNanoseconds(), precision.getIncrement(), precision.getUnit(), roundingMode);

            JSRealm realm = getRealm();
            var roundedInstant = JSTemporalInstant.create(getContext(), realm, roundedNs);
            return TemporalUtil.temporalInstantToString(roundedInstant, timeZone, precision.getPrecision());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalInstant(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
    }

    public abstract static class JSTemporalInstantToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalInstantToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected TruffleString toLocaleString(JSTemporalInstantObject instant) {
            return TemporalUtil.temporalInstantToString(instant, Undefined.instance, AUTO);
        }

        @Specialization(guards = "!isJSTemporalInstant(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
    }

    public abstract static class JSTemporalInstantToZonedDateTimeISONode extends JSTemporalBuiltinOperation {

        protected JSTemporalInstantToZonedDateTimeISONode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalZonedDateTimeObject toZonedDateTimeISO(JSTemporalInstantObject instant, Object timeZoneParam,
                        @Cached ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifier) {
            TruffleString timeZone = toTimeZoneIdentifier.execute(timeZoneParam);
            return JSTemporalZonedDateTime.create(getContext(), getRealm(), instant.getNanoseconds(), timeZone, TemporalConstants.ISO8601);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalInstant(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object item) {
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
    }

}
