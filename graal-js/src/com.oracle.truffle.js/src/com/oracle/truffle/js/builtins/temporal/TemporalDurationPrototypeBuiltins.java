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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationAbsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationAddSubNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationNegatedNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationRoundNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationTotalNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationWithNodeGen;
import com.oracle.truffle.js.nodes.cast.JSNumberToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerWithoutRoundingNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.CalendarMethodsRecordLookupNode;
import com.oracle.truffle.js.nodes.temporal.TemporalBalanceDateDurationRelativeNode;
import com.oracle.truffle.js.nodes.temporal.TemporalDurationAddNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.TemporalRoundDurationNode;
import com.oracle.truffle.js.nodes.temporal.TemporalUnbalanceDateDurationRelativeNode;
import com.oracle.truffle.js.nodes.temporal.ToRelativeTemporalObjectNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDurationNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.CalendarMethodsRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.DateDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeZoneMethodsRecord;
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
                return JSTemporalDurationAddSubNodeGen.create(context, builtin, TemporalUtil.ADD, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case subtract:
                return JSTemporalDurationAddSubNodeGen.create(context, builtin, TemporalUtil.SUBTRACT, args().withThis().fixedArgs(2).createArgumentNodes(context));
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
                return UnsupportedValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalDurationGetterNode extends JSBuiltinNode {

        public final TemporalDurationPrototype property;

        public JSTemporalDurationGetterNode(JSContext context, JSBuiltin builtin, TemporalDurationPrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization
        protected Object durationGetter(JSTemporalDurationObject temporalD) {
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
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalDuration(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
    }

    public abstract static class JSTemporalDurationWith extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalDurationObject with(JSTemporalDurationObject duration, Object temporalDurationLike,
                        @Cached JSToIntegerWithoutRoundingNode toInt,
                        @Cached InlinedBranchProfile errorBranch) {
            JSDynamicObject durationLike = TemporalUtil.toPartialDuration(temporalDurationLike,
                            getContext(), isObjectNode, toInt, this, errorBranch);

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
            return JSTemporalDuration.createTemporalDuration(getContext(), getRealm(),
                            years, months, weeks, days,
                            hours, minutes, seconds, milliseconds, microseconds, nanoseconds,
                            this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalDuration(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object temporalDurationLike) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
    }

    public abstract static class JSTemporalDurationNegated extends JSBuiltinNode {

        protected JSTemporalDurationNegated(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalDurationObject negated(JSTemporalDurationObject duration) {
            return JSTemporalDuration.createNegatedTemporalDuration(getContext(), getRealm(), duration);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalDuration(thisObj)")
        protected static Object invalidReceiver(Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
    }

    public abstract static class JSTemporalDurationAbs extends JSBuiltinNode {

        protected JSTemporalDurationAbs(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalDurationObject abs(JSTemporalDurationObject duration,
                        @Cached InlinedBranchProfile errorBranch) {
            return JSTemporalDuration.createTemporalDuration(getContext(), getRealm(),
                            Math.abs(duration.getYears()), Math.abs(duration.getMonths()), Math.abs(duration.getWeeks()),
                            Math.abs(duration.getDays()), Math.abs(duration.getHours()), Math.abs(duration.getMinutes()),
                            Math.abs(duration.getSeconds()), Math.abs(duration.getMilliseconds()),
                            Math.abs(duration.getMicroseconds()), Math.abs(duration.getNanoseconds()),
                            this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalDuration(thisObj)")
        protected static Object invalidReceiver(Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
    }

    public abstract static class JSTemporalDurationAddSubNode extends JSTemporalBuiltinOperation {

        private final int sign;

        protected JSTemporalDurationAddSubNode(JSContext context, JSBuiltin builtin, int sign) {
            super(context, builtin);
            this.sign = sign;
        }

        @Specialization
        protected JSTemporalDurationObject addDurationToOrSubtractDurationFromDuration(JSTemporalDurationObject duration, Object other, Object options,
                        @Cached ToTemporalDurationNode toTemporalDurationNode,
                        @Cached("createDateAdd()") CalendarMethodsRecordLookupNode lookupDateAdd,
                        @Cached("createDateUntil()") CalendarMethodsRecordLookupNode lookupDateUntil,
                        @Cached TemporalDurationAddNode durationAddNode,
                        @Cached ToRelativeTemporalObjectNode toRelativeTemporalObjectNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSTemporalDurationObject otherDuration = toTemporalDurationNode.execute(other);
            JSDynamicObject normalizedOptions = getOptionsObject(options, this, errorBranch, optionUndefined);
            var relativeToRec = toRelativeTemporalObjectNode.execute(normalizedOptions);

            var relativeTo = relativeToRec.relativeTo();
            TimeZoneMethodsRecord timeZoneRec = relativeToRec.timeZoneRec();
            CalendarMethodsRecord calendarRec = relativeToRec.createCalendarMethodsRecord(lookupDateAdd, lookupDateUntil);

            JSTemporalDurationRecord result = durationAddNode.execute(duration.getYears(), duration.getMonths(),
                            duration.getWeeks(), duration.getDays(), duration.getHours(), duration.getMinutes(),
                            duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(),
                            duration.getNanoseconds(),
                            sign * otherDuration.getYears(), sign * otherDuration.getMonths(), sign * otherDuration.getWeeks(), sign * otherDuration.getDays(),
                            sign * otherDuration.getHours(), sign * otherDuration.getMinutes(), sign * otherDuration.getSeconds(),
                            sign * otherDuration.getMilliseconds(), sign * otherDuration.getMicroseconds(), sign * otherDuration.getNanoseconds(),
                            relativeTo, calendarRec, timeZoneRec, null);
            return JSTemporalDuration.createTemporalDuration(getContext(), getRealm(),
                            result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(),
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(), this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalDuration(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object other, Object options) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
    }

    public abstract static class JSTemporalDurationRound extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalDurationObject round(JSTemporalDurationObject duration, Object roundToParam,
                        @Cached JSToNumberNode toNumber,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalDurationAddNode durationAddNode,
                        @Cached InlinedConditionProfile roundToIsTString,
                        @Cached InlinedConditionProfile relativeToIsZonedDateTime,
                        @Cached ToRelativeTemporalObjectNode toRelativeTemporalObjectNode,
                        @Cached TemporalRoundDurationNode roundDurationNode,
                        @Cached("createDateAdd()") CalendarMethodsRecordLookupNode lookupDateAdd,
                        @Cached("createDateUntil()") CalendarMethodsRecordLookupNode lookupDateUntil,
                        @Cached TemporalUnbalanceDateDurationRelativeNode unbalanceDurationRelativeNode,
                        @Cached TemporalBalanceDateDurationRelativeNode balanceDateDurationRelativeNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            if (roundToParam == Undefined.instance) {
                throw TemporalErrors.createTypeErrorOptionsUndefined();
            }
            JSDynamicObject roundTo;
            if (roundToIsTString.profile(this, Strings.isTString(roundToParam))) {
                roundTo = JSOrdinary.createWithNullPrototype(getContext());
                JSRuntime.createDataPropertyOrThrow(roundTo, TemporalConstants.SMALLEST_UNIT, roundToParam);
            } else {
                roundTo = getOptionsObject(roundToParam, this, errorBranch, optionUndefined);
            }
            boolean smallestUnitPresent = true;
            boolean largestUnitPresent = true;
            Unit smallestUnit = toSmallestTemporalUnit(roundTo, TemporalUtil.listEmpty, null, equalNode, getOptionNode, this, errorBranch);
            if (smallestUnit == Unit.EMPTY) {
                smallestUnitPresent = false;
                smallestUnit = Unit.NANOSECOND;
            }
            Unit existingLargestUnit = TemporalUtil.defaultTemporalLargestUnit(duration.getYears(),
                            duration.getMonths(), duration.getWeeks(), duration.getDays(), duration.getHours(),
                            duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                            duration.getMicroseconds());
            Unit defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(existingLargestUnit, smallestUnit);
            Unit largestUnit = toLargestTemporalUnit(roundTo, TemporalUtil.listEmpty, null, null, equalNode, getOptionNode, this, errorBranch);
            if (largestUnit == Unit.EMPTY) {
                largestUnitPresent = false;
                largestUnit = defaultLargestUnit;
            } else if (Unit.AUTO == largestUnit) {
                largestUnit = defaultLargestUnit;
            }
            if (!smallestUnitPresent && !largestUnitPresent) {
                errorBranch.enter(this);
                throw Errors.createRangeError("at least one of smallestUnit or largestUnit is required");
            }
            JSRealm realm = getRealm();
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            RoundingMode roundingMode = toTemporalRoundingMode(roundTo, HALF_EXPAND, equalNode, getOptionNode);
            Double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(roundTo, maximum, false, toNumber);
            var relativeToRec = toRelativeTemporalObjectNode.execute(roundTo);
            JSTemporalZonedDateTimeObject zonedRelativeTo = relativeToRec.zonedRelativeTo();
            JSTemporalPlainDateObject plainRelativeTo = relativeToRec.plainRelativeTo();
            TimeZoneMethodsRecord timeZoneRec = relativeToRec.timeZoneRec();

            boolean roundingGranularityIsNoop = smallestUnit == Unit.NANOSECOND && roundingIncrement == 1;
            boolean calendarUnitsPresent = duration.getYears() != 0 || duration.getMonths() != 0 || duration.getWeeks() != 0;
            boolean hoursToDaysConversionMayOccur = (duration.getDays() != 0 && zonedRelativeTo != null) ||
                            Math.abs(duration.getHours()) >= 24;
            if (roundingGranularityIsNoop && largestUnit == existingLargestUnit && !calendarUnitsPresent && !hoursToDaysConversionMayOccur &&
                            Math.abs(duration.getMinutes()) < 60 && Math.abs(duration.getSeconds()) < 60 &&
                            Math.abs(duration.getMilliseconds()) < 1000 && Math.abs(duration.getMicroseconds()) < 1000 && Math.abs(duration.getNanoseconds()) < 1000) {
                /*
                 * The above conditions mean that the operation will have no effect: the smallest
                 * unit and rounding increment will leave the total duration unchanged, and it can
                 * be determined without calling a calendar or time zone method that no balancing
                 * will take place.
                 */
                return JSTemporalDuration.createTemporalDuration(getContext(), realm,
                                duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                                duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                                duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), this, errorBranch);
            }
            JSTemporalPlainDateTimeObject precalculatedPlainDateTime = null;
            boolean plainDateTimeOrRelativeToWillBeUsed = !roundingGranularityIsNoop ||
                            largestUnit == Unit.YEAR || largestUnit == Unit.MONTH || largestUnit == Unit.WEEK || largestUnit == Unit.DAY ||
                            calendarUnitsPresent || duration.getDays() != 0;
            if (zonedRelativeTo != null && plainDateTimeOrRelativeToWillBeUsed) {
                /*
                 * Note: The above conditions mean that the corresponding Temporal.PlainDateTime or
                 * Temporal.PlainDate for zonedRelativeTo will be used in one of the operations
                 * below.
                 */
                var instant = JSTemporalInstant.create(getContext(), realm, zonedRelativeTo.getNanoseconds());
                precalculatedPlainDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), getRealm(),
                                timeZoneRec, instant, zonedRelativeTo.getCalendar());
                plainRelativeTo = JSTemporalPlainDate.create(getContext(), realm,
                                precalculatedPlainDateTime.getYear(), precalculatedPlainDateTime.getMonth(), precalculatedPlainDateTime.getDay(),
                                zonedRelativeTo.getCalendar(), this, errorBranch);
            }

            CalendarMethodsRecord calendarRec = relativeToRec.createCalendarMethodsRecord(lookupDateAdd, lookupDateUntil);

            JSTemporalDurationRecord unbalanceResult = unbalanceDurationRelativeNode.execute(duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(), largestUnit,
                            plainRelativeTo, calendarRec);
            JSTemporalDurationRecord roundResult = roundDurationNode.execute(
                            unbalanceResult.getYears(), unbalanceResult.getMonths(), unbalanceResult.getWeeks(), unbalanceResult.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                            duration.getMicroseconds(), duration.getNanoseconds(), (long) roundingIncrement, smallestUnit,
                            roundingMode, plainRelativeTo, zonedRelativeTo, calendarRec, timeZoneRec, precalculatedPlainDateTime);
            TimeDurationRecord balanceResult;
            if (relativeToIsZonedDateTime.profile(this, zonedRelativeTo != null)) {
                JSTemporalDurationRecord adjustResult = TemporalUtil.adjustRoundedDurationDays(getContext(), realm, durationAddNode, roundDurationNode,
                                roundResult.getYears(), roundResult.getMonths(), roundResult.getWeeks(), roundResult.getDays(),
                                roundResult.getHours(), roundResult.getMinutes(), roundResult.getSeconds(),
                                roundResult.getMilliseconds(), roundResult.getMicroseconds(), roundResult.getNanoseconds(),
                                (long) roundingIncrement, smallestUnit, roundingMode,
                                zonedRelativeTo, calendarRec, timeZoneRec, precalculatedPlainDateTime);
                balanceResult = TemporalUtil.balanceTimeDurationRelative(adjustResult.getDays(), adjustResult.getHours(), adjustResult.getMinutes(), adjustResult.getSeconds(),
                                adjustResult.getMilliseconds(), adjustResult.getMicroseconds(), adjustResult.getNanoseconds(), largestUnit,
                                zonedRelativeTo, timeZoneRec, precalculatedPlainDateTime, getContext(), realm);
            } else {
                balanceResult = TemporalUtil.balanceTimeDuration(roundResult.getDays(), roundResult.getHours(), roundResult.getMinutes(), roundResult.getSeconds(),
                                roundResult.getMilliseconds(), roundResult.getMicroseconds(), roundResult.getNanoseconds(), largestUnit);
            }
            DateDurationRecord result = balanceDateDurationRelativeNode.execute(
                            roundResult.getYears(), roundResult.getMonths(), roundResult.getWeeks(),
                            balanceResult.days(), largestUnit, smallestUnit, plainRelativeTo, calendarRec);
            return JSTemporalDuration.createTemporalDuration(getContext(), realm,
                            result.years(), result.months(), result.weeks(), result.days(),
                            balanceResult.hours(), balanceResult.minutes(), balanceResult.seconds(),
                            balanceResult.milliseconds(), balanceResult.microseconds(), balanceResult.nanoseconds(), this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalDuration(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object roundToParam) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
    }

    public abstract static class JSTemporalDurationTotal extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationTotal(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected double total(JSTemporalDurationObject duration, Object totalOfParam,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached ToRelativeTemporalObjectNode toRelativeTemporalObjectNode,
                        @Cached("createDateAdd()") CalendarMethodsRecordLookupNode lookupDateAdd,
                        @Cached("createDateUntil()") CalendarMethodsRecordLookupNode lookupDateUntil,
                        @Cached TemporalRoundDurationNode roundDurationNode,
                        @Cached TemporalUnbalanceDateDurationRelativeNode unbalanceDurationRelativeNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            if (totalOfParam == Undefined.instance) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorOptionsUndefined();
            }
            JSRealm realm = getRealm();
            JSDynamicObject totalOf;
            if (Strings.isTString(totalOfParam)) {
                totalOf = JSOrdinary.createWithNullPrototype(getContext());
                JSRuntime.createDataPropertyOrThrow(totalOf, TemporalConstants.UNIT, totalOfParam);
            } else {
                totalOf = getOptionsObject(totalOfParam, this, errorBranch, optionUndefined);
            }
            var relativeToRec = toRelativeTemporalObjectNode.execute(totalOf);
            JSTemporalZonedDateTimeObject zonedRelativeTo = relativeToRec.zonedRelativeTo();
            JSTemporalPlainDateObject plainRelativeTo = relativeToRec.plainRelativeTo();
            TimeZoneMethodsRecord timeZoneRec = relativeToRec.timeZoneRec();

            Unit unit = toTemporalDurationTotalUnit(totalOf, equalNode, getOptionNode);
            JSTemporalPlainDateTimeObject precalculatedPlainDateTime = null;
            boolean plainDateTimeOrRelativeToWillBeUsed = unit == Unit.YEAR || unit == Unit.MONTH || unit == Unit.WEEK || unit == Unit.DAY ||
                            duration.getYears() != 0 || duration.getMonths() != 0 || duration.getWeeks() != 0;
            if (zonedRelativeTo != null && plainDateTimeOrRelativeToWillBeUsed) {
                /*
                 * Note: The above conditions mean that the corresponding Temporal.PlainDateTime or
                 * Temporal.PlainDate for zonedRelativeTo will be used in one of the operations
                 * below.
                 */
                var instant = JSTemporalInstant.create(getContext(), realm, zonedRelativeTo.getNanoseconds());
                precalculatedPlainDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), getRealm(),
                                timeZoneRec, instant, zonedRelativeTo.getCalendar());
                plainRelativeTo = JSTemporalPlainDate.create(getContext(), realm,
                                precalculatedPlainDateTime.getYear(), precalculatedPlainDateTime.getMonth(), precalculatedPlainDateTime.getDay(),
                                zonedRelativeTo.getCalendar(), this, errorBranch);
            }

            CalendarMethodsRecord calendarRec = relativeToRec.createCalendarMethodsRecord(lookupDateAdd, lookupDateUntil);

            JSTemporalDurationRecord unbalanceResult = unbalanceDurationRelativeNode.execute(
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(), unit, plainRelativeTo, calendarRec);
            TimeDurationRecord balanceResult;
            if (zonedRelativeTo != null) {
                JSTemporalZonedDateTimeObject intermediate = TemporalUtil.moveRelativeZonedDateTime(getContext(), realm, zonedRelativeTo, calendarRec, timeZoneRec,
                                dtol(unbalanceResult.getYears()), dtol(unbalanceResult.getMonths()), dtol(unbalanceResult.getWeeks()), 0, precalculatedPlainDateTime);
                balanceResult = TemporalUtil.balancePossiblyInfiniteTimeDurationRelative(unbalanceResult.getDays(),
                                duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                                duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(),
                                unit, intermediate, timeZoneRec, null, getContext(), realm);
            } else {
                balanceResult = TemporalUtil.balancePossiblyInfiniteTimeDuration(unbalanceResult.getDays(),
                                duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                                duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), unit);

            }
            if (balanceResult.isOverflow()) {
                return balanceResult.getOverflow();
            }
            JSTemporalDurationRecord roundResult = roundDurationNode.execute(
                            unbalanceResult.getYears(), unbalanceResult.getMonths(), unbalanceResult.getWeeks(),
                            balanceResult.days(), balanceResult.hours(), balanceResult.minutes(), balanceResult.seconds(),
                            balanceResult.milliseconds(), balanceResult.microseconds(), balanceResult.nanoseconds(),
                            1, unit, RoundingMode.TRUNC,
                            plainRelativeTo, zonedRelativeTo, calendarRec, timeZoneRec, precalculatedPlainDateTime);

            double whole = switch (unit) {
                case YEAR -> roundResult.getYears();
                case MONTH -> roundResult.getMonths();
                case WEEK -> roundResult.getWeeks();
                case DAY -> roundResult.getDays();
                case HOUR -> roundResult.getHours();
                case MINUTE -> roundResult.getMinutes();
                case SECOND -> roundResult.getSeconds();
                case MILLISECOND -> roundResult.getMilliseconds();
                case MICROSECOND -> roundResult.getMicroseconds();
                case NANOSECOND -> roundResult.getNanoseconds();
                default -> throw Errors.shouldNotReachHereUnexpectedValue(unit);
            };
            return whole + roundResult.getRemainder();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalDuration(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object totalOfParam) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
    }

    public abstract static class JSTemporalDurationToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static TruffleString toString(JSTemporalDurationObject duration,
                        @Cached JSNumberToBigIntNode toBigIntNode) {
            return JSTemporalDuration.temporalDurationToString(
                            dtol(duration.getYears()), dtol(duration.getMonths()), dtol(duration.getWeeks()), dtol(duration.getDays()),
                            dtol(duration.getHours()), dtol(duration.getMinutes()), dtol(duration.getSeconds()),
                            dtol(duration.getMilliseconds()), dtol(duration.getMicroseconds()), dtol(duration.getNanoseconds()),
                            AUTO, toBigIntNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalDuration(thisObj)")
        protected static Object invalidReceiver(Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
    }

    public abstract static class JSTemporalDurationToString extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(JSTemporalDurationObject dur, Object opt,
                        @Cached JSNumberToBigIntNode toBigIntNode,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalRoundDurationNode roundDurationNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSDynamicObject options = getOptionsObject(opt, this, errorBranch, optionUndefined);
            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecision(options, toStringNode, getOptionNode, equalNode);
            if (precision.getUnit() == Unit.MINUTE) {
                errorBranch.enter(this);
                throw Errors.createRangeError("unexpected precision minute");
            }
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode, getOptionNode);
            JSTemporalDurationRecord result = roundDurationNode.execute(dur.getYears(), dur.getMonths(), dur.getWeeks(), dur.getDays(),
                            dur.getHours(), dur.getMinutes(), dur.getSeconds(), dur.getMilliseconds(), dur.getMicroseconds(),
                            dur.getNanoseconds(), (long) precision.getIncrement(), precision.getUnit(), roundingMode);
            return JSTemporalDuration.temporalDurationToString(result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(),
                            result.getHours(), result.getMinutes(), result.getSeconds(),
                            result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            precision.getPrecision(), toBigIntNode);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalDuration(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object opt) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
    }
}
