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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.AUTO;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;

import java.math.BigInteger;
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
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantToZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantUntilSinceNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.SnapshotOwnPropertiesNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.TemporalRoundDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarSlotValueNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalInstantNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneSlotValueNode;
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
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
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
        toZonedDateTime(1),
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
                return JSTemporalInstantUntilSinceNodeGen.create(context, builtin, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case since:
                return JSTemporalInstantUntilSinceNodeGen.create(context, builtin, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
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
            case toZonedDateTime:
                return JSTemporalInstantToZonedDateTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
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
            BigInteger ns = instant.getNanoseconds().bigIntegerValue();
            switch (property) {
                // roundTowardsZero is a no-op for BigIntegers
                case epochSeconds:
                    return ns.divide(TemporalUtil.BI_10_POW_9).doubleValue();
                case epochMilliseconds:
                    return ns.divide(TemporalUtil.BI_10_POW_6).doubleValue();
                case epochMicroseconds:
                    return new BigInt(ns.divide(TemporalUtil.BI_1000));
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

        private final boolean isUntil;

        protected JSTemporalInstantUntilSinceNode(JSContext context, JSBuiltin builtin, boolean isUntil) {
            super(context, builtin);
            this.isUntil = isUntil;
        }

        @Specialization
        protected JSTemporalDurationObject differenceTemporalInstant(JSTemporalInstantObject instant, Object otherObj, Object options,
                        @Cached SnapshotOwnPropertiesNode snapshotOwnProperties,
                        @Cached JSToNumberNode toNumber,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached ToTemporalInstantNode toTemporalInstantNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached TemporalRoundDurationNode roundDurationNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSTemporalInstantObject other = toTemporalInstantNode.execute(otherObj);
            JSDynamicObject resolvedOptions = snapshotOwnProperties.snapshot(getOptionsObject(options, this, errorBranch, optionUndefined), Null.instance);
            Unit smallestUnit = toSmallestTemporalUnit(resolvedOptions, TemporalUtil.listYMWD, NANOSECOND, equalNode, getOptionNode, this, errorBranch);
            Unit defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(Unit.SECOND, smallestUnit);
            Unit largestUnit = toLargestTemporalUnit(resolvedOptions, TemporalUtil.listYMWD, AUTO, defaultLargestUnit, equalNode, getOptionNode, this, errorBranch);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            RoundingMode roundingMode = toTemporalRoundingMode(resolvedOptions, TRUNC, equalNode, getOptionNode);
            Double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(resolvedOptions, maximum, false, toNumber);

            BigInt one = isUntil ? instant.getNanoseconds() : other.getNanoseconds();
            BigInt two = isUntil ? other.getNanoseconds() : instant.getNanoseconds();

            TimeDurationRecord result = TemporalUtil.differenceInstant(one, two, roundingIncrement, smallestUnit, largestUnit, roundingMode, roundDurationNode);
            JSRealm realm = getRealm();
            return JSTemporalDuration.createTemporalDuration(getContext(), realm, 0, 0, 0, 0,
                            result.hours(), result.minutes(), result.seconds(), result.milliseconds(), result.microseconds(), result.nanoseconds(), this, errorBranch);
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
                        @Cached JSToNumberNode toNumber,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
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
            Unit smallestUnit = toSmallestTemporalUnit(roundTo, TemporalUtil.listYMWD, null, equalNode, getOptionNode, this, errorBranch);
            if (smallestUnit == Unit.EMPTY) {
                errorBranch.enter(this);
                throw TemporalErrors.createRangeErrorSmallestUnitExpected();
            }
            RoundingMode roundingMode = toTemporalRoundingMode(roundTo, HALF_EXPAND, equalNode, getOptionNode);
            double maximum;
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
                maximum = TemporalUtil.NS_PER_DAY;
            }
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(roundTo, maximum, true, toNumber);
            BigInt roundedNs = TemporalUtil.roundTemporalInstant(instant.getNanoseconds(), (long) roundingIncrement, smallestUnit, roundingMode);
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

    public abstract static class JSTemporalInstantToString extends JSTemporalBuiltinOperation {

        protected JSTemporalInstantToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(JSTemporalInstantObject instant, Object optionsParam,
                        @Cached ToTemporalTimeZoneSlotValueNode toTimeZoneSlotValue,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSDynamicObject options = getOptionsObject(optionsParam, this, errorBranch, optionUndefined);
            Object timeZoneRaw = JSObject.get(options, TIME_ZONE);
            Object timeZone = Undefined.instance;
            if (timeZoneRaw != Undefined.instance) {
                timeZone = toTimeZoneSlotValue.execute(timeZoneRaw);
            }
            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecision(options, toStringNode, getOptionNode, equalNode);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode, getOptionNode);
            BigInt ns = instant.getNanoseconds();
            BigInt roundedNs = TemporalUtil.roundTemporalInstant(ns, (long) precision.getIncrement(), precision.getUnit(), roundingMode);
            JSRealm realm = getRealm();
            var roundedInstant = JSTemporalInstant.create(getContext(), realm, roundedNs);
            return TemporalUtil.temporalInstantToString(getContext(), realm, roundedInstant, timeZone, precision.getPrecision());
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
            return TemporalUtil.temporalInstantToString(getContext(), getRealm(), instant, Undefined.instance, AUTO);
        }

        @Specialization(guards = "!isJSTemporalInstant(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
    }

    @ImportStatic(TemporalConstants.class)
    public abstract static class JSTemporalInstantToZonedDateTimeNode extends JSTemporalBuiltinOperation {

        protected JSTemporalInstantToZonedDateTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalZonedDateTimeObject toZonedDateTime(JSTemporalInstantObject instant, Object item,
                        @Cached("create(CALENDAR, getJSContext())") PropertyGetNode getCalendar,
                        @Cached("create(TIME_ZONE, getJSContext())") PropertyGetNode getTimeZone,
                        @Cached ToTemporalCalendarSlotValueNode toCalendarSlot,
                        @Cached ToTemporalTimeZoneSlotValueNode toTimeZoneSlot,
                        @Cached InlinedBranchProfile errorBranch) {
            if (!isObject(item)) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorNotAnObject(item);
            }
            Object calendarLike = getCalendar.getValue(item);
            if (calendarLike == Undefined.instance) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorTemporalCalendarExpected();
            }
            Object calendar = toCalendarSlot.execute(calendarLike);
            Object temporalTimeZoneLike = getTimeZone.getValue(item);
            if (temporalTimeZoneLike == Undefined.instance) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorTemporalTimeZoneExpected();
            }
            Object timeZone = toTimeZoneSlot.execute(temporalTimeZoneLike);
            return JSTemporalZonedDateTime.create(getContext(), getRealm(), instant.getNanoseconds(), timeZone, calendar);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalInstant(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object item) {
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
    }

    public abstract static class JSTemporalInstantToZonedDateTimeISONode extends JSTemporalBuiltinOperation {

        protected JSTemporalInstantToZonedDateTimeISONode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalZonedDateTimeObject toZonedDateTimeISO(JSTemporalInstantObject instant, Object itemParam,
                        @Cached ToTemporalTimeZoneSlotValueNode toTimeZoneSlotValue,
                        @Cached InlinedBranchProfile errorBranch) {
            Object item = itemParam;
            if (isObject(item)) {
                JSDynamicObject itemObj = TemporalUtil.toJSDynamicObject(item, this, errorBranch);
                Object timeZoneProperty = JSObject.get(itemObj, TIME_ZONE);
                if (timeZoneProperty != Undefined.instance) {
                    item = timeZoneProperty;
                }
            }
            Object timeZone = toTimeZoneSlotValue.execute(item);
            JSRealm realm = getRealm();
            JSDynamicObject calendar = TemporalUtil.getISO8601Calendar(getContext(), realm);
            return JSTemporalZonedDateTime.create(getContext(), realm, instant.getNanoseconds(), timeZone, calendar);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalInstant(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object item) {
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
    }

}
