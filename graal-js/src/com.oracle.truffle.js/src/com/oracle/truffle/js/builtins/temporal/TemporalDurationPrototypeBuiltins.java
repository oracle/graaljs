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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
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

import java.util.Collections;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationAbsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationAddNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationNegatedNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationRoundNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationSubtractNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationTotalNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationValueOfNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationWithNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimePluralRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalDurationPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalDurationPrototypeBuiltins.TemporalDurationPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalDurationPrototypeBuiltins();

    protected TemporalDurationPrototypeBuiltins() {
        super(JSTemporalDuration.PROTOTYPE_NAME, TemporalDurationPrototype.class);
    }

    public enum TemporalDurationPrototype implements BuiltinEnum<TemporalDurationPrototype> {
        with(1),
        negated(0),
        abs(0),
        add(2),
        subtract(2),
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
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalDurationPrototype builtinEnum) {
        switch (builtinEnum) {
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

    // 7.3.15
    public abstract static class JSTemporalDurationWith extends JSBuiltinNode {

        protected JSTemporalDurationWith(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject with(DynamicObject thisObj, DynamicObject temporalDurationLike,
                        @Cached("create()") IsObjectNode isObjectNode,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("createNew()") JSFunctionCallNode functionCallNode) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            DynamicObject durationLike = JSTemporalDuration.toPartialDuration(temporalDurationLike,
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
            return JSTemporalDuration.createTemporalDurationFromInstance(years, months, weeks, days,
                            hours, minutes, seconds, milliseconds, microseconds, nanoseconds, getContext().getRealm(),
                            functionCallNode);
        }
    }

    // 7.3.16
    public abstract static class JSTemporalDurationNegated extends JSBuiltinNode {

        protected JSTemporalDurationNegated(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject negated(DynamicObject thisObj,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            return JSTemporalDuration.createTemporalDurationFromInstance(
                            -duration.getYears(), -duration.getMonths(), -duration.getWeeks(), -duration.getDays(),
                            -duration.getHours(), -duration.getMinutes(), -duration.getSeconds(), -duration.getMilliseconds(),
                            -duration.getMicroseconds(), -duration.getNanoseconds(), getContext().getRealm(), callNode);
        }
    }

    // 7.3.17
    public abstract static class JSTemporalDurationAbs extends JSBuiltinNode {

        protected JSTemporalDurationAbs(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject abs(DynamicObject thisObj,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            return JSTemporalDuration.createTemporalDurationFromInstance(
                            Math.abs(duration.getYears()), Math.abs(duration.getMonths()), Math.abs(duration.getWeeks()),
                            Math.abs(duration.getDays()), Math.abs(duration.getHours()), Math.abs(duration.getMinutes()),
                            Math.abs(duration.getSeconds()), Math.abs(duration.getMilliseconds()),
                            Math.abs(duration.getMicroseconds()), Math.abs(duration.getNanoseconds()),
                            getContext().getRealm(), callNode);
        }
    }

    // 7.3.18
    public abstract static class JSTemporalDurationAdd extends JSBuiltinNode {

        protected JSTemporalDurationAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject add(DynamicObject thisObj, Object other, Object options,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            JSTemporalPlainDateTimeRecord otherDuration = JSTemporalDuration.toLimitedTemporalDuration(other, Collections.emptySet(),
                            isObject, toString, toInt);
            DynamicObject normalizedOptions = TemporalUtil.getOptionsObject(options, getContext(), isObject);
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(normalizedOptions, getContext());
            JSTemporalPlainDateTimePluralRecord result = JSTemporalDuration.addDuration(duration.getYears(), duration.getMonths(),
                            duration.getWeeks(), duration.getDays(), duration.getHours(), duration.getMinutes(),
                            duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(),
                            duration.getNanoseconds(),
                            otherDuration.getYear(), otherDuration.getMonth(), otherDuration.getWeeks(), otherDuration.getDay(),
                            otherDuration.getHour(), otherDuration.getMinute(), otherDuration.getSecond(),
                            otherDuration.getMillisecond(), otherDuration.getMicrosecond(), otherDuration.getNanosecond(),
                            relativeTo, getContext());
            return JSTemporalDuration.createTemporalDurationFromInstance(
                            result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(), result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(),
                            result.getMicroseconds(), result.getNanoseconds(),
                            getContext().getRealm(), callNode);
        }
    }

    // 7.3.19
    public abstract static class JSTemporalDurationSubtract extends JSBuiltinNode {

        protected JSTemporalDurationSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject subtract(DynamicObject thisObj, Object other, Object options,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            JSTemporalPlainDateTimeRecord otherDuration = JSTemporalDuration.toLimitedTemporalDuration(other, Collections.emptySet(),
                            isObject, toString, toInt);
            DynamicObject normalizedOptions = TemporalUtil.getOptionsObject(options, getContext(), isObject);
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(normalizedOptions, getContext());
            JSTemporalPlainDateTimePluralRecord result = JSTemporalDuration.addDuration(duration.getYears(), duration.getMonths(),
                            duration.getWeeks(), duration.getDays(), duration.getHours(), duration.getMinutes(),
                            duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(),
                            duration.getNanoseconds(),
                            -otherDuration.getYear(), -otherDuration.getMonth(), -otherDuration.getWeeks(), -otherDuration.getDay(),
                            -otherDuration.getHour(), -otherDuration.getMinute(), -otherDuration.getSecond(),
                            -otherDuration.getMillisecond(), -otherDuration.getMicrosecond(), -otherDuration.getNanosecond(),
                            relativeTo, getContext());
            return JSTemporalDuration.createTemporalDurationFromInstance(
                            result.getYears(), result.getMonths(), result.getWeeks(), result.getDays(),
                            result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            getContext().getRealm(), callNode);
        }
    }

    // 7.3.20
    public abstract static class JSTemporalDurationRound extends JSBuiltinNode {

        protected JSTemporalDurationRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject round(DynamicObject thisObj, Object optParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            if (optParam == Undefined.instance) {
                throw Errors.createTypeError("No options given.");
            }
            DynamicObject normalizedOptions = TemporalUtil.getOptionsObject(optParam, getContext(), isObject);
            boolean smallestUnitPresent = true;
            boolean largestUnitPresent = true;
            String smallestUnit = TemporalUtil.toSmallestTemporalDurationUnit(normalizedOptions, Collections.emptySet(), null, toBoolean, toString);
            if (smallestUnit == null) {
                smallestUnitPresent = false;
                smallestUnit = NANOSECONDS;
            }
            String defaultLargestUnit = JSTemporalDuration.defaultTemporalLargestUnit(duration.getYears(),
                            duration.getMonths(), duration.getWeeks(), duration.getDays(), duration.getHours(),
                            duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                            duration.getMicroseconds());
            defaultLargestUnit = TemporalUtil.largerOfTwoTemporalDurationUnits(defaultLargestUnit, smallestUnit);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(normalizedOptions, Collections.emptySet(),
                            null, toBoolean, toString);
            if (largestUnit == null) {
                largestUnitPresent = false;
                largestUnit = defaultLargestUnit;
            } else if ("auto".equals(largestUnit)) {
                largestUnit = defaultLargestUnit;
            }
            if (!smallestUnitPresent && !largestUnitPresent) {
                throw Errors.createRangeError("unit expected");
            }
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(normalizedOptions, HALF_EXPAND, toBoolean, toString);
            double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(normalizedOptions, maximum, false, isObject, toNumber);
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(normalizedOptions, getContext());
            JSTemporalPlainDateTimePluralRecord unbalanceResult = JSTemporalDuration.unbalanceDurationRelative(duration.getYears(),
                            duration.getMonths(), duration.getWeeks(), duration.getDays(), largestUnit, relativeTo,
                            getContext());
            JSTemporalPlainDateTimePluralRecord roundResult = JSTemporalDuration.roundDuration(
                            unbalanceResult.getYears(),
                            unbalanceResult.getMonths(),
                            unbalanceResult.getWeeks(),
                            unbalanceResult.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                            duration.getMicroseconds(), duration.getNanoseconds(), (long) roundingIncrement, smallestUnit,
                            roundingMode, relativeTo, getContext());
            JSTemporalPlainDateTimePluralRecord adjustResult = JSTemporalDuration.adjustRoundedDurationDays(
                            roundResult.getYears(), roundResult.getMonths(), roundResult.getWeeks(), roundResult.getDays(), roundResult.getHours(), roundResult.getMinutes(), roundResult.getSeconds(),
                            roundResult.getMilliseconds(), roundResult.getMicroseconds(), roundResult.getNanoseconds(),
                            (long) roundingIncrement, smallestUnit, roundingMode, relativeTo, getContext());
            JSTemporalPlainDateTimePluralRecord balanceResult = JSTemporalDuration.balanceDurationRelative(
                            adjustResult.getYears(), adjustResult.getMonths(), adjustResult.getWeeks(), adjustResult.getDays(),
                            largestUnit, relativeTo, getContext());
            // TODO: Check if relativeTo has InitializedTemporalZonedDateTime 7.3.22.22
            JSTemporalPlainDateTimePluralRecord result = JSTemporalDuration.balanceDuration(
                            balanceResult.getDays(),
                            adjustResult.getHours(), adjustResult.getMinutes(), adjustResult.getSeconds(),
                            adjustResult.getMilliseconds(), adjustResult.getMicroseconds(), adjustResult.getNanoseconds(),
                            largestUnit, relativeTo);
            return JSTemporalDuration.createTemporalDurationFromInstance(
                            balanceResult.getYears(), balanceResult.getMonths(), balanceResult.getWeeks(),
                            result.getDays(), result.getHours(), result.getMinutes(), result.getSeconds(),
                            result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(),
                            getContext().getRealm(), callNode);
        }
    }

    // 7.3.21
    public abstract static class JSTemporalDurationTotal extends JSBuiltinNode {

        protected JSTemporalDurationTotal(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected long total(DynamicObject thisObj, Object options,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            DynamicObject normalizedOptions = TemporalUtil.getOptionsObject(options, getContext(), isObject);
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(normalizedOptions, getContext());
            String unit = TemporalUtil.toTemporalDurationTotalUnit(normalizedOptions, toBoolean, toString);
            if (unit == null) {
                throw Errors.createRangeError("Unit not defined.");
            }
            JSTemporalPlainDateTimePluralRecord unbalanceResult = JSTemporalDuration.unbalanceDurationRelative(duration.getYears(),
                            duration.getMonths(), duration.getWeeks(), duration.getDays(), unit, relativeTo, getContext());
            DynamicObject intermediate = Undefined.instance;
            // TODO: Check if relative has InitializedTemporalZonedDateTime. If yes
            // intermediate = moveRelativeZonedDateTime()
            JSTemporalPlainDateTimePluralRecord balanceResult = JSTemporalDuration.balanceDuration(
                            unbalanceResult.getDays(),
                            unbalanceResult.getHours(),
                            unbalanceResult.getMinutes(),
                            unbalanceResult.getSeconds(),
                            unbalanceResult.getMilliseconds(),
                            unbalanceResult.getMicroseconds(),
                            unbalanceResult.getNanoseconds(),
                            unit, intermediate);
            JSTemporalPlainDateTimePluralRecord roundResult = JSTemporalDuration.roundDuration(
                            unbalanceResult.getYears(),
                            unbalanceResult.getMonths(),
                            unbalanceResult.getWeeks(),
                            balanceResult.getDays(),
                            balanceResult.getHours(),
                            balanceResult.getMinutes(),
                            balanceResult.getSeconds(),
                            balanceResult.getMilliseconds(),
                            balanceResult.getMicroseconds(),
                            balanceResult.getNanoseconds(),
                            1, unit, "trunc", relativeTo, getContext());
            long whole = 0;
            if (unit.equals(YEARS)) {
                whole = roundResult.getYears();
            }
            if (unit.equals(MONTHS)) {
                whole = roundResult.getMonths();
            }
            if (unit.equals(WEEKS)) {
                whole = roundResult.getWeeks();
            }
            if (unit.equals(DAYS)) {
                whole = roundResult.getDays();
            }
            if (unit.equals(HOURS)) {
                whole = roundResult.getHours();
            }
            if (unit.equals(MINUTES)) {
                whole = roundResult.getMinutes();
            }
            if (unit.equals(SECONDS)) {
                whole = roundResult.getSeconds();
            }
            if (unit.equals(MILLISECONDS)) {
                whole = roundResult.getMilliseconds();
            }
            if (unit.equals(MICROSECONDS)) {
                whole = roundResult.getMicroseconds();
            }
            return whole + (long) roundResult.getRemainder();
        }
    }

    // 7.3.23 & 7.3.24
    public abstract static class JSTemporalDurationToLocaleString extends JSBuiltinNode {

        protected JSTemporalDurationToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(DynamicObject thisObj) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            return JSTemporalDuration.temporalDurationToString(
                            duration.getYears(), duration.getMonths(), duration.getWeeks(), duration.getDays(),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds(),
                            "auto");
        }
    }

    public abstract static class JSTemporalDurationToString extends JSBuiltinNode {

        protected JSTemporalDurationToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(DynamicObject duration, Object opt,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            TemporalUtil.requireTemporalDuration(duration);
            DynamicObject options = TemporalUtil.getOptionsObject(opt, getContext());
            JSTemporalPrecisionRecord precision = TemporalUtil.toDurationSecondsStringPrecision(options);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, "trunc", toBoolean, toString);
            JSTemporalPlainDateTimePluralRecord result = JSTemporalDuration.roundDuration(
                            getLong(duration, YEARS),
                            getLong(duration, MONTHS),
                            getLong(duration, WEEKS),
                            getLong(duration, DAYS),
                            getLong(duration, HOURS),
                            getLong(duration, MINUTES),
                            getLong(duration, SECONDS),
                            getLong(duration, MILLISECONDS),
                            getLong(duration, MICROSECONDS),
                            getLong(duration, MILLISECONDS),
                            (long) precision.getIncrement(), precision.getUnit(), roundingMode, Undefined.instance, getContext());
            return JSTemporalDuration.temporalDurationToString(result.getYears(),
                            result.getMonths(),
                            result.getWeeks(),
                            result.getDays(),
                            result.getHours(),
                            result.getMinutes(),
                            result.getSeconds(),
                            result.getMilliseconds(),
                            result.getMicroseconds(),
                            result.getNanoseconds(),
                            precision.getPrecision());
        }
    }

    // 7.3.26
    public abstract static class JSTemporalDurationValueOf extends JSBuiltinNode {

        protected JSTemporalDurationValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") DynamicObject thisObj) {
            // TODO ?
            throw Errors.createTypeError("Not supported.");
        }
    }
}
