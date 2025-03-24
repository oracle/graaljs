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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalUtil.dtol;

import java.util.EnumSet;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
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
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationToLocaleStringIntlNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationTotalNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationWithNodeGen;
import com.oracle.truffle.js.nodes.cast.JSNumberToBigIntNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.InitializeDurationFormatNode;
import com.oracle.truffle.js.nodes.temporal.DifferencePlainDateTimeWithRoundingNode;
import com.oracle.truffle.js.nodes.temporal.DifferenceZonedDateTimeNode;
import com.oracle.truffle.js.nodes.temporal.DifferenceZonedDateTimeWithRoundingNode;
import com.oracle.truffle.js.nodes.temporal.GetRoundingIncrementOptionNode;
import com.oracle.truffle.js.nodes.temporal.GetTemporalUnitNode;
import com.oracle.truffle.js.nodes.temporal.TemporalAddDateNode;
import com.oracle.truffle.js.nodes.temporal.TemporalAddZonedDateTimeNode;
import com.oracle.truffle.js.nodes.temporal.TemporalDifferenceDateNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.ToFractionalSecondDigitsNode;
import com.oracle.truffle.js.nodes.temporal.ToRelativeTemporalObjectNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDurationNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDurationRecordNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalPartialDurationRecordNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.intl.JSDurationFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSDurationFormatObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.NormalizedDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.TimeRecord;
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
                return JSTemporalDurationToLocaleStringNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case toLocaleString:
                if (context.isOptionIntl402()) {
                    return JSTemporalDurationToLocaleStringIntlNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
                } else {
                    return JSTemporalDurationToLocaleStringNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
                }
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
                        @Cached ToTemporalPartialDurationRecordNode toTemporalPartialDurationRecord,
                        @Cached InlinedBranchProfile errorBranch) {
            JSTemporalDurationRecord r = toTemporalPartialDurationRecord.execute(temporalDurationLike, JSTemporalDurationRecord.create(duration));
            return JSTemporalDuration.createTemporalDuration(getContext(), getRealm(),
                            r.getYears(), r.getMonths(), r.getWeeks(), r.getDays(),
                            r.getHours(), r.getMinutes(), r.getSeconds(), r.getMilliseconds(), r.getMicroseconds(), r.getNanoseconds(),
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
        protected JSTemporalDurationObject addDurations(JSTemporalDurationObject duration, Object other, Object options,
                        @Cached ToTemporalDurationNode toTemporalDurationNode,
                        @Cached ToRelativeTemporalObjectNode toRelativeTemporalObjectNode,
                        @Cached TemporalAddDateNode addDateNode,
                        @Cached TemporalDifferenceDateNode differenceDateNode,
                        @Cached DifferenceZonedDateTimeNode differenceZonedDateTimeNode,
                        @Cached TemporalAddZonedDateTimeNode addZonedDateTimeNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined,
                        @Cached InlinedBranchProfile relativeToUndefinedBranch,
                        @Cached InlinedBranchProfile relativeToPlainDateBranch,
                        @Cached InlinedBranchProfile relativeToZonedDateTimeBranch) {
            JSTemporalDurationObject otherDuration = toTemporalDurationNode.execute(other);
            JSDynamicObject normalizedOptions = getOptionsObject(options, this, errorBranch, optionUndefined);
            var relativeToRec = toRelativeTemporalObjectNode.execute(normalizedOptions);

            JSTemporalPlainDateObject plainRelativeTo = relativeToRec.plainRelativeTo();
            JSTemporalZonedDateTimeObject zonedRelativeTo = relativeToRec.zonedRelativeTo();

            JSContext ctx = getJSContext();
            JSRealm realm = getRealm();

            double y1 = duration.getYears();
            double mon1 = duration.getMonths();
            double w1 = duration.getWeeks();
            double d1 = duration.getDays();
            double h1 = duration.getHours();
            double min1 = duration.getMinutes();
            double s1 = duration.getSeconds();
            double ms1 = duration.getMilliseconds();
            double mus1 = duration.getMicroseconds();
            double ns1 = duration.getNanoseconds();
            double y2 = sign * otherDuration.getYears();
            double mon2 = sign * otherDuration.getMonths();
            double w2 = sign * otherDuration.getWeeks();
            double d2 = sign * otherDuration.getDays();
            double h2 = sign * otherDuration.getHours();
            double min2 = sign * otherDuration.getMinutes();
            double s2 = sign * otherDuration.getSeconds();
            double ms2 = sign * otherDuration.getMilliseconds();
            double mus2 = sign * otherDuration.getMicroseconds();
            double ns2 = sign * otherDuration.getNanoseconds();

            TemporalUtil.Unit largestUnit1 = TemporalUtil.defaultTemporalLargestUnit(y1, mon1, w1, d1, h1, min1, s1, ms1, mus1);
            TemporalUtil.Unit largestUnit2 = TemporalUtil.defaultTemporalLargestUnit(y2, mon2, w2, d2, h2, min2, s2, ms2, mus2);
            TemporalUtil.Unit largestUnit = TemporalUtil.largerOfTwoTemporalUnits(largestUnit1, largestUnit2);
            BigInt norm1 = TemporalUtil.normalizeTimeDuration(h1, min1, s1, ms1, mus1, ns1);
            BigInt norm2 = TemporalUtil.normalizeTimeDuration(h2, min2, s2, ms2, mus2, ns2);

            if (zonedRelativeTo == null && plainRelativeTo == null) {
                relativeToUndefinedBranch.enter(this);
                if (largestUnit.isCalendarUnit()) {
                    errorBranch.enter(this);
                    throw Errors.createRangeError("Largest unit allowed with no relativeTo is 'days'.");
                }
                BigInt normResult = TemporalUtil.addNormalizedTimeDuration(norm1, norm2);
                normResult = TemporalUtil.add24HourDaysToNormalizedTimeDuration(normResult, d1 + d2);
                TimeDurationRecord result = TemporalUtil.balanceTimeDuration(normResult, largestUnit);
                return JSTemporalDuration.createTemporalDuration(ctx, realm,
                                0, 0, 0, result.days(),
                                result.hours(), result.minutes(), result.seconds(), result.milliseconds(), result.microseconds(), result.nanoseconds(),
                                this, errorBranch);
            } else if (plainRelativeTo != null) {
                relativeToPlainDateBranch.enter(this);
                TruffleString calendar = plainRelativeTo.getCalendar();
                JSTemporalDurationObject dateDuration1 = JSTemporalDuration.createTemporalDuration(ctx, realm, y1, mon1, w1, d1, 0, 0, 0, 0, 0, 0, this, errorBranch);
                JSTemporalDurationObject dateDuration2 = JSTemporalDuration.createTemporalDuration(ctx, realm, y2, mon2, w2, d2, 0, 0, 0, 0, 0, 0, this, errorBranch);

                JSTemporalPlainDateObject intermediate = addDateNode.execute(calendar, plainRelativeTo, dateDuration1, TemporalUtil.Overflow.CONSTRAIN);
                JSTemporalPlainDateObject end = addDateNode.execute(calendar, intermediate, dateDuration2, TemporalUtil.Overflow.CONSTRAIN);

                TemporalUtil.Unit dateLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(TemporalUtil.Unit.DAY, largestUnit);

                JSTemporalDurationObject dateDifference = differenceDateNode.execute(calendar, plainRelativeTo, end, dateLargestUnit);
                BigInt norm1WithDays = TemporalUtil.add24HourDaysToNormalizedTimeDuration(norm1, dateDifference.getDays());
                BigInt normResult = TemporalUtil.addNormalizedTimeDuration(norm1WithDays, norm2);
                TimeDurationRecord result = TemporalUtil.balanceTimeDuration(normResult, largestUnit);
                return JSTemporalDuration.createTemporalDuration(ctx, realm,
                                dateDifference.getYears(), dateDifference.getMonths(), dateDifference.getWeeks(), result.days(),
                                result.hours(), result.minutes(), result.seconds(), result.milliseconds(), result.microseconds(), result.nanoseconds(),
                                this, errorBranch);
            } else {
                assert zonedRelativeTo != null;
                TruffleString calendar = zonedRelativeTo.getCalendar();
                TruffleString timeZone = zonedRelativeTo.getTimeZone();
                relativeToZonedDateTimeBranch.enter(this);
                JSTemporalPlainDateTimeObject startDateTime = null;
                if (largestUnit.isDateUnit()) {
                    var relativeToInstant = JSTemporalInstant.create(ctx, realm, zonedRelativeTo.getNanoseconds());
                    startDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, realm, timeZone, relativeToInstant, calendar);
                }
                BigInt intermediateNs = addZonedDateTimeNode.execute(zonedRelativeTo.getNanoseconds(), timeZone, calendar,
                                y1, mon1, w1, d1, norm1, startDateTime);
                BigInt endNs = addZonedDateTimeNode.execute(intermediateNs, timeZone, calendar,
                                y2, mon2, w2, d2, norm2, null);

                if (largestUnit.isTimeUnit()) {
                    BigInt norm = TemporalUtil.normalizedTimeDurationFromEpochNanosecondsDifference(endNs, zonedRelativeTo.getNanoseconds());
                    TimeDurationRecord result = TemporalUtil.balanceTimeDuration(norm, largestUnit);
                    return JSTemporalDuration.createTemporalDuration(ctx, realm,
                                    0, 0, 0, 0, result.hours(), result.minutes(), result.seconds(), result.milliseconds(), result.microseconds(), result.nanoseconds(),
                                    this, errorBranch);
                } else {
                    NormalizedDurationRecord diffResult = differenceZonedDateTimeNode.execute(
                                    zonedRelativeTo.getNanoseconds(), endNs, timeZone, calendar, largestUnit, startDateTime);
                    TimeDurationRecord timeResult = TemporalUtil.balanceTimeDuration(diffResult.normalizedTimeTotalNanoseconds(), Unit.HOUR);
                    return JSTemporalDuration.createTemporalDuration(ctx, realm,
                                    diffResult.years(), diffResult.months(), diffResult.weeks(), diffResult.days(),
                                    timeResult.hours(), timeResult.minutes(), timeResult.seconds(),
                                    timeResult.milliseconds(), timeResult.microseconds(), timeResult.nanoseconds(),
                                    this, errorBranch);
                }
            }
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

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected JSTemporalDurationObject round(JSTemporalDurationObject duration, Object roundToParam,
                        @Bind Node node,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached InlinedConditionProfile roundToIsTString,
                        @Cached InlinedConditionProfile relativeToIsZonedDateTime,
                        @Cached ToRelativeTemporalObjectNode toRelativeTemporalObjectNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached GetTemporalUnitNode getLargestUnit,
                        @Cached GetTemporalUnitNode getSmallestUnit,
                        @Cached GetRoundingIncrementOptionNode getRoundingIncrementOption,
                        @Cached TemporalAddDateNode addDate,
                        @Cached DifferencePlainDateTimeWithRoundingNode differencePlainDateTimeWithRounding,
                        @Cached DifferenceZonedDateTimeWithRoundingNode differenceZonedDateTimeWithRounding,
                        @Cached TemporalAddZonedDateTimeNode addZonedDateTimeNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            if (roundToParam == Undefined.instance) {
                throw TemporalErrors.createTypeErrorOptionsUndefined();
            }
            JSDynamicObject roundTo;
            JSContext ctx = getContext();
            if (roundToIsTString.profile(node, Strings.isTString(roundToParam))) {
                roundTo = JSOrdinary.createWithNullPrototype(ctx);
                JSRuntime.createDataPropertyOrThrow(roundTo, TemporalConstants.SMALLEST_UNIT, roundToParam);
            } else {
                roundTo = getOptionsObject(roundToParam, node, errorBranch, optionUndefined);
            }
            boolean smallestUnitPresent = true;
            boolean largestUnitPresent = true;
            Unit largestUnit = getLargestUnit.execute(roundTo, TemporalConstants.LARGEST_UNIT, TemporalUtil.unitMappingDateTimeOrAuto, Unit.EMPTY);
            var relativeToRec = toRelativeTemporalObjectNode.execute(roundTo);
            JSTemporalZonedDateTimeObject zonedRelativeTo = relativeToRec.zonedRelativeTo();
            JSTemporalPlainDateObject plainRelativeTo = relativeToRec.plainRelativeTo();
            int roundingIncrement = getRoundingIncrementOption.execute(roundTo);
            RoundingMode roundingMode = toTemporalRoundingMode(roundTo, HALF_EXPAND, equalNode, getOptionNode);
            Unit smallestUnit = getSmallestUnit.execute(roundTo, TemporalConstants.SMALLEST_UNIT, TemporalUtil.unitMappingDateTime, Unit.EMPTY);
            if (smallestUnit == Unit.EMPTY) {
                smallestUnitPresent = false;
                smallestUnit = Unit.NANOSECOND;
            }
            Unit existingLargestUnit = TemporalUtil.defaultTemporalLargestUnit(duration.getYears(),
                            duration.getMonths(), duration.getWeeks(), duration.getDays(), duration.getHours(),
                            duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                            duration.getMicroseconds());
            Unit defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(existingLargestUnit, smallestUnit);
            if (largestUnit == Unit.EMPTY) {
                largestUnitPresent = false;
                largestUnit = defaultLargestUnit;
            } else if (Unit.AUTO == largestUnit) {
                largestUnit = defaultLargestUnit;
            }
            if (!smallestUnitPresent && !largestUnitPresent) {
                errorBranch.enter(node);
                throw Errors.createRangeError("at least one of smallestUnit or largestUnit is required");
            }
            JSRealm realm = getRealm();
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            Integer maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            if (maximum != null) {
                TemporalUtil.validateTemporalRoundingIncrement(roundingIncrement, maximum, false, node, errorBranch);
            }

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
                return JSTemporalDuration.createTemporalDuration(ctx, realm,
                                duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                                duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                                duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(), this, errorBranch);
            }
            JSTemporalPlainDateTimeObject precalculatedPlainDateTime = null;
            boolean plainDateTimeOrRelativeToWillBeUsed = largestUnit.isCalendarUnit() || largestUnit == Unit.DAY || calendarUnitsPresent || duration.getDays() != 0;
            if (zonedRelativeTo != null && plainDateTimeOrRelativeToWillBeUsed) {
                /*
                 * Note: The above conditions mean that the corresponding Temporal.PlainDateTime or
                 * Temporal.PlainDate for zonedRelativeTo will be used in one of the operations
                 * below.
                 */
                var instant = JSTemporalInstant.create(ctx, realm, zonedRelativeTo.getNanoseconds());
                precalculatedPlainDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(ctx, realm,
                                zonedRelativeTo.getTimeZone(), instant, zonedRelativeTo.getCalendar());
                plainRelativeTo = JSTemporalPlainDate.create(ctx, realm,
                                precalculatedPlainDateTime.getYear(), precalculatedPlainDateTime.getMonth(), precalculatedPlainDateTime.getDay(),
                                zonedRelativeTo.getCalendar(), node, errorBranch);
            }

            BigInt norm = TemporalUtil.normalizeTimeDuration(duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());

            JSTemporalDurationRecord roundResult;
            if (relativeToIsZonedDateTime.profile(node, zonedRelativeTo != null)) {
                TruffleString calendar = zonedRelativeTo.getCalendar();
                TruffleString timeZone = zonedRelativeTo.getTimeZone();
                BigInt relativeEpochNs = zonedRelativeTo.getNanoseconds();
                BigInt targetEpochNs = addZonedDateTimeNode.execute(relativeEpochNs, timeZone, calendar,
                                duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                                norm, precalculatedPlainDateTime);

                var roundRecord = differenceZonedDateTimeWithRounding.execute(relativeEpochNs, targetEpochNs, calendar, timeZone,
                                precalculatedPlainDateTime, largestUnit, roundingIncrement, smallestUnit, roundingMode);
                roundResult = roundRecord.duration();
            } else if (relativeToIsZonedDateTime.profile(node, plainRelativeTo != null)) {
                TruffleString calendar = plainRelativeTo.getCalendar();
                TimeRecord targetTime = TemporalUtil.addTime(0, 0, 0, 0, 0, 0, norm, node, errorBranch);
                var dateDuration = JSTemporalDuration.createTemporalDuration(ctx, realm,
                                duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays() + targetTime.days(),
                                0, 0, 0, 0, 0, 0, node, errorBranch);
                JSTemporalPlainDateObject targetDate = addDate.execute(calendar, plainRelativeTo, dateDuration, TemporalUtil.Overflow.CONSTRAIN);
                var roundRecord = differencePlainDateTimeWithRounding.execute(plainRelativeTo, 0, 0, 0, 0, 0, 0,
                                targetDate.getYear(), targetDate.getMonth(), targetDate.getDay(),
                                targetTime.hour(), targetTime.minute(), targetTime.second(), targetTime.millisecond(), targetTime.microsecond(), targetTime.nanosecond(),
                                calendar, largestUnit, roundingIncrement, smallestUnit, roundingMode);
                roundResult = roundRecord.duration();
            } else {
                if (calendarUnitsPresent || largestUnit.isCalendarUnit() || smallestUnit.isCalendarUnit()) {
                    throw Errors.createRangeError("a starting point is required for years, months, or weeks balancing and rounding");
                }
                var roundRecord = TemporalUtil.roundTimeDuration(duration.getDays(), norm, roundingIncrement, smallestUnit, roundingMode);
                BigInt normWithDays = TemporalUtil.add24HourDaysToNormalizedTimeDuration(roundRecord.normalizedDuration().normalizedTimeTotalNanoseconds(), roundRecord.normalizedDuration().days());
                TimeDurationRecord balanceResult = TemporalUtil.balanceTimeDuration(normWithDays, largestUnit);
                roundResult = JSTemporalDurationRecord.createWeeks(0, 0, 0, balanceResult.days(),
                                balanceResult.hours(), balanceResult.minutes(), balanceResult.seconds(),
                                balanceResult.milliseconds(), balanceResult.microseconds(), balanceResult.nanoseconds());
            }
            return JSTemporalDuration.createTemporalDuration(ctx, realm,
                            roundResult.getYears(), roundResult.getMonths(), roundResult.getWeeks(), roundResult.getDays(),
                            roundResult.getHours(), roundResult.getMinutes(), roundResult.getSeconds(),
                            roundResult.getMilliseconds(), roundResult.getMicroseconds(), roundResult.getNanoseconds(), node, errorBranch);
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

        @SuppressWarnings("truffle-static-method")
        @Specialization
        protected final double total(JSTemporalDurationObject duration, Object totalOfParam,
                        @Bind Node node,
                        @Cached ToRelativeTemporalObjectNode toRelativeTemporalObjectNode,
                        @Cached GetTemporalUnitNode getTemporalUnit,
                        @Cached TemporalAddDateNode addDate,
                        @Cached DifferencePlainDateTimeWithRoundingNode differencePlainDateTimeWithRounding,
                        @Cached DifferenceZonedDateTimeWithRoundingNode differenceZonedDateTimeWithRounding,
                        @Cached TemporalAddZonedDateTimeNode addZonedDateTimeNode,
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
                totalOf = getOptionsObject(totalOfParam, node, errorBranch, optionUndefined);
            }
            var relativeToRec = toRelativeTemporalObjectNode.execute(totalOf);
            JSTemporalZonedDateTimeObject zonedRelativeTo = relativeToRec.zonedRelativeTo();
            JSTemporalPlainDateObject plainRelativeTo = relativeToRec.plainRelativeTo();

            Unit unit = getTemporalUnit.execute(totalOf, UNIT, TemporalUtil.unitMappingDateTime, Unit.REQUIRED);
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
                precalculatedPlainDateTime = TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), realm,
                                zonedRelativeTo.getTimeZone(), instant, zonedRelativeTo.getCalendar());
                plainRelativeTo = JSTemporalPlainDate.create(getContext(), realm,
                                precalculatedPlainDateTime.getYear(), precalculatedPlainDateTime.getMonth(), precalculatedPlainDateTime.getDay(),
                                zonedRelativeTo.getCalendar(), node, errorBranch);
            }

            BigInt norm = TemporalUtil.normalizeTimeDuration(duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());

            double total;
            if (zonedRelativeTo != null) {
                TruffleString calendar = zonedRelativeTo.getCalendar();
                TruffleString timeZone = zonedRelativeTo.getTimeZone();
                BigInt relativeEpochNs = zonedRelativeTo.getNanoseconds();
                BigInt targetEpochNs = addZonedDateTimeNode.execute(relativeEpochNs, timeZone, calendar,
                                duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                                norm, precalculatedPlainDateTime);

                var roundRecord = differenceZonedDateTimeWithRounding.execute(relativeEpochNs, targetEpochNs, calendar, timeZone,
                                precalculatedPlainDateTime, unit, 1, unit, RoundingMode.TRUNC);
                total = roundRecord.total();
            } else if (plainRelativeTo != null) {
                TruffleString calendar = plainRelativeTo.getCalendar();
                TimeRecord targetTime = TemporalUtil.addTime(0, 0, 0, 0, 0, 0, norm, node, errorBranch);

                var dateDuration = JSTemporalDuration.createTemporalDuration(getContext(), realm,
                                duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays() + targetTime.days(),
                                0, 0, 0, 0, 0, 0, node, errorBranch);
                JSTemporalPlainDateObject targetDate = addDate.execute(calendar, plainRelativeTo, dateDuration, TemporalUtil.Overflow.CONSTRAIN);
                var roundRecord = differencePlainDateTimeWithRounding.execute(plainRelativeTo, 0, 0, 0, 0, 0, 0,
                                targetDate.getYear(), targetDate.getMonth(), targetDate.getDay(),
                                targetTime.hour(), targetTime.minute(), targetTime.second(), targetTime.millisecond(), targetTime.microsecond(), targetTime.nanosecond(),
                                calendar, unit, 1, unit, RoundingMode.TRUNC);
                total = roundRecord.total();
            } else {
                if (duration.getYears() != 0 || duration.getMonths() != 0 || duration.getWeeks() != 0 || unit.isCalendarUnit()) {
                    throw Errors.createRangeError("a starting point is required for years, months, or weeks total");
                }
                BigInt normWithDays = TemporalUtil.add24HourDaysToNormalizedTimeDuration(norm, duration.getDays());
                var roundRecord = TemporalUtil.roundTimeDuration(0, normWithDays, 1, unit, RoundingMode.TRUNC);
                total = roundRecord.total();

            }
            return total;
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

    public abstract static class JSTemporalDurationToLocaleStringIntl extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationToLocaleStringIntl(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(JSTemporalDurationObject duration, Object locales, Object options,
                        @Cached("create(getContext())") InitializeDurationFormatNode initializeDurationFormatNode,
                        @Cached ToTemporalDurationRecordNode toTemporalDurationRecord,
                        @Cached TruffleString.FromJavaStringNode fromJavaString) {
            JSRealm realm = getRealm();
            JSDynamicObject proto = getContext().getDurationFormatFactory().getPrototype(realm);
            JSDurationFormatObject durationFormat = JSDurationFormat.create(getContext(), realm, proto);
            initializeDurationFormatNode.executeInit(durationFormat, locales, options);
            JSTemporalDurationRecord record = toTemporalDurationRecord.execute(duration);
            return Strings.fromJavaString(fromJavaString, JSDurationFormat.format(durationFormat.getInternalState(), record));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalDuration(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object locales, Object options) {
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
    }

    public abstract static class JSTemporalDurationToString extends JSTemporalBuiltinOperation {

        protected JSTemporalDurationToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(JSTemporalDurationObject duration, Object opt,
                        @Cached ToFractionalSecondDigitsNode toFractionalSecondDigitsNode,
                        @Cached JSNumberToBigIntNode toBigIntNode,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached GetTemporalUnitNode getSmallestUnit,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSDynamicObject options = getOptionsObject(opt, this, errorBranch, optionUndefined);
            int digits = toFractionalSecondDigitsNode.execute(options);
            RoundingMode roundingMode = toTemporalRoundingMode(options, TRUNC, equalNode, getOptionNode);

            Unit smallestUnit = getSmallestUnit.execute(options, TemporalConstants.SMALLEST_UNIT, TemporalUtil.unitMappingTime, Unit.EMPTY);
            if (smallestUnit == Unit.HOUR || smallestUnit == Unit.MINUTE) {
                errorBranch.enter(this);
                throw TemporalErrors.createRangeErrorSmallestUnitOutOfRange();
            }

            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecisionRecord(smallestUnit, digits);

            JSTemporalDurationRecord result;
            if (precision.getUnit() != Unit.NANOSECOND || precision.getIncrement() != 1) {
                BigInt norm = TemporalUtil.normalizeTimeDuration(duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                                duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
                Unit largestUnit = TemporalUtil.defaultTemporalLargestUnit(duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(), duration.getHours(),
                                duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds());
                var roundRecord = TemporalUtil.roundTimeDuration(0, norm, precision.getIncrement(), precision.getUnit(), roundingMode);
                norm = roundRecord.normalizedDuration().normalizedTimeTotalNanoseconds();
                var time = TemporalUtil.balanceTimeDuration(norm, TemporalUtil.largerOfTwoTemporalUnits(largestUnit, Unit.SECOND));
                result = JSTemporalDurationRecord.createWeeks(duration.getYears(), duration.getMonths(), duration.getWeeks(),
                                duration.getDays() + time.days(), time.hours(), time.minutes(), time.seconds(), time.milliseconds(), time.microseconds(), time.nanoseconds());
            } else {
                result = JSTemporalDurationRecord.create(duration);
            }

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
