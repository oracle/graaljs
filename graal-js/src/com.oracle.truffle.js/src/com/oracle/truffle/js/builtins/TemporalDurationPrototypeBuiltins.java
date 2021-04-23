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

import java.util.Collections;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationAbsNodeGen;
import com.oracle.truffle.js.builtins.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationAddNodeGen;
import com.oracle.truffle.js.builtins.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationNegatedNodeGen;
import com.oracle.truffle.js.builtins.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationRoundNodeGen;
import com.oracle.truffle.js.builtins.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationSubtractNodeGen;
import com.oracle.truffle.js.builtins.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationTotalNodeGen;
import com.oracle.truffle.js.builtins.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationValueOfNodeGen;
import com.oracle.truffle.js.builtins.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationWithNodeGen;
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
import com.oracle.truffle.js.runtime.builtins.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDurationObject;
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
                return JSTemporalDurationToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
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
                            getContext().getRealm(), isObjectNode, toInt);

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
        protected DynamicObject add(DynamicObject thisObj, DynamicObject other, DynamicObject options,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            DynamicObject otherDuration = JSTemporalDuration.toLimitedTemporalDuration(other, Collections.emptySet(),
                            getContext().getRealm(), isObject, toString, toInt);
            DynamicObject normalizedOptions = TemporalUtil.getOptionsObject(options, getContext().getRealm(), isObject);
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(normalizedOptions, getContext());
            DynamicObject result = JSTemporalDuration.addDuration(duration.getYears(), duration.getMonths(),
                            duration.getWeeks(), duration.getDays(), duration.getHours(), duration.getMinutes(),
                            duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(),
                            duration.getNanoseconds(),
                            getLong(otherDuration, YEARS),
                            getLong(otherDuration, MONTHS),
                            getLong(otherDuration, WEEKS),
                            getLong(otherDuration, DAYS),
                            getLong(otherDuration, HOURS),
                            getLong(otherDuration, MINUTES),
                            getLong(otherDuration, SECONDS),
                            getLong(otherDuration, MILLISECONDS),
                            getLong(otherDuration, MICROSECONDS),
                            getLong(otherDuration, NANOSECONDS),
                            relativeTo, getContext().getRealm());
            return JSTemporalDuration.createTemporalDurationFromInstance(
                            getLong(result, YEARS),
                            getLong(result, MONTHS),
                            getLong(result, WEEKS),
                            getLong(result, DAYS),
                            getLong(result, HOURS),
                            getLong(result, MINUTES),
                            getLong(result, SECONDS),
                            getLong(result, MILLISECONDS),
                            getLong(result, MICROSECONDS),
                            getLong(result, NANOSECONDS),
                            getContext().getRealm(), callNode);
        }
    }

    // 7.3.19
    public abstract static class JSTemporalDurationSubtract extends JSBuiltinNode {

        protected JSTemporalDurationSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject subtract(DynamicObject thisObj, DynamicObject other, DynamicObject options,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            DynamicObject otherDuration = JSTemporalDuration.toLimitedTemporalDuration(other, Collections.emptySet(),
                            getContext().getRealm(), isObject, toString, toInt);
            DynamicObject normalizedOptions = TemporalUtil.getOptionsObject(options, getContext().getRealm(), isObject);
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(normalizedOptions, getContext());
            DynamicObject result = JSTemporalDuration.addDuration(duration.getYears(), duration.getMonths(),
                            duration.getWeeks(), duration.getDays(), duration.getHours(), duration.getMinutes(),
                            duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(),
                            duration.getNanoseconds(),
                            -getLong(otherDuration, YEARS),
                            -getLong(otherDuration, MONTHS),
                            -getLong(otherDuration, WEEKS),
                            -getLong(otherDuration, DAYS),
                            -getLong(otherDuration, HOURS),
                            -getLong(otherDuration, MINUTES),
                            -getLong(otherDuration, SECONDS),
                            -getLong(otherDuration, MILLISECONDS),
                            -getLong(otherDuration, MICROSECONDS),
                            -getLong(otherDuration, NANOSECONDS),
                            relativeTo, getContext().getRealm());
            return JSTemporalDuration.createTemporalDurationFromInstance(
                            getLong(result, YEARS),
                            getLong(result, MONTHS),
                            getLong(result, WEEKS),
                            getLong(result, DAYS),
                            getLong(result, HOURS),
                            getLong(result, MINUTES),
                            getLong(result, SECONDS),
                            getLong(result, MILLISECONDS),
                            getLong(result, MICROSECONDS),
                            getLong(result, NANOSECONDS),
                            getContext().getRealm(), callNode);
        }
    }

    // 7.3.20
    public abstract static class JSTemporalDurationRound extends JSBuiltinNode {

        protected JSTemporalDurationRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject round(DynamicObject thisObj, DynamicObject options,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            if (options == null) {
                throw Errors.createTypeError("No options given.");
            }
            DynamicObject normalizedOptions = TemporalUtil.getOptionsObject(options, getContext().getRealm(), isObject);
            boolean smallestUnitPresent = true;
            boolean largestUnitPresent = true;
            String smallestUnit = TemporalUtil.toSmallestTemporalDurationUnit(options, null,
                            Collections.emptySet(), isObject, toBoolean, toString);
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
                            null, isObject, toBoolean, toString);
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
            String roundingMode = TemporalUtil.toTemporalRoundingMode(normalizedOptions, "nearest",
                            isObject, toBoolean, toString);
            double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options,
                            maximum, false, isObject, toNumber);
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(options, getContext());
            DynamicObject unbalanceResult = JSTemporalDuration.unbalanceDurationRelative(duration.getYears(),
                            duration.getMonths(), duration.getWeeks(), duration.getDays(), largestUnit, relativeTo,
                            getContext().getRealm());
            DynamicObject roundResult = JSTemporalDuration.roundDuration(
                            getLong(unbalanceResult, YEARS, 0),
                            getLong(unbalanceResult, MONTHS, 0),
                            getLong(unbalanceResult, WEEKS, 0),
                            getLong(unbalanceResult, DAYS, 0),
                            duration.getHours(), duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                            duration.getMicroseconds(), duration.getNanoseconds(), (long) roundingIncrement, smallestUnit,
                            roundingMode, relativeTo, getContext().getRealm());
            DynamicObject adjustResult = JSTemporalDuration.adjustRoundedDurationDays(
                            getLong(roundResult, YEARS, 0),
                            getLong(roundResult, MONTHS, 0),
                            getLong(roundResult, WEEKS, 0),
                            getLong(roundResult, DAYS, 0),
                            getLong(roundResult, HOURS, 0),
                            getLong(roundResult, MINUTES, 0),
                            getLong(roundResult, SECONDS, 0),
                            getLong(roundResult, MILLISECONDS, 0),
                            getLong(roundResult, MICROSECONDS, 0),
                            getLong(roundResult, NANOSECONDS, 0),
                            (long) roundingIncrement, smallestUnit, roundingMode, relativeTo, getContext().getRealm());
            DynamicObject balanceResult = JSTemporalDuration.balanceDurationRelative(
                            getLong(adjustResult, YEARS, 0),
                            getLong(adjustResult, MONTHS, 0),
                            getLong(adjustResult, WEEKS, 0),
                            getLong(adjustResult, DAYS, 0),
                            largestUnit, relativeTo, getContext().getRealm());
            // TODO: Check if relativeTo has InitializedTemporalZonedDateTime 7.3.22.22
            DynamicObject result = JSTemporalDuration.balanceDuration(
                            getLong(balanceResult, DAYS, 0),
                            getLong(adjustResult, HOURS, 0),
                            getLong(adjustResult, MINUTES, 0),
                            getLong(adjustResult, SECONDS, 0),
                            getLong(adjustResult, MILLISECONDS, 0),
                            getLong(adjustResult, MICROSECONDS, 0),
                            getLong(adjustResult, NANOSECONDS, 0),
                            largestUnit, relativeTo, getContext().getRealm());
            return JSTemporalDuration.createTemporalDurationFromInstance(
                            getLong(balanceResult, YEARS, 0),
                            getLong(balanceResult, MONTHS, 0),
                            getLong(balanceResult, WEEKS, 0),
                            getLong(result, DAYS, 0),
                            getLong(result, HOURS, 0),
                            getLong(result, MINUTES, 0),
                            getLong(result, SECONDS, 0),
                            getLong(result, MILLISECONDS, 0),
                            getLong(result, MICROSECONDS, 0),
                            getLong(result, NANOSECONDS, 0),
                            getContext().getRealm(), callNode);
        }
    }

    // 7.3.21
    public abstract static class JSTemporalDurationTotal extends JSBuiltinNode {

        protected JSTemporalDurationTotal(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected long total(DynamicObject thisObj, DynamicObject options,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            DynamicObject normalizedOptions = TemporalUtil.getOptionsObject(options, getContext().getRealm(), isObject);
            DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(normalizedOptions, getContext());
            String unit = TemporalUtil.toTemporalDurationTotalUnit(normalizedOptions, isObject, toBoolean, toString);
            if (unit == null) {
                throw Errors.createRangeError("Unit not defined.");
            }
            DynamicObject unbalanceResult = JSTemporalDuration.unbalanceDurationRelative(duration.getYears(),
                            duration.getMonths(), duration.getWeeks(), duration.getDays(), unit, relativeTo,
                            getContext().getRealm());
            DynamicObject intermediate = null;
            // TODO: Check if relative has InitializedTemporalZonedDateTime. If yes
            // intermediate
            // = moveRelativeZonedDateTime()
            DynamicObject balanceResult = JSTemporalDuration.balanceDuration(
                            getLong(unbalanceResult, DAYS, 0),
                            getLong(unbalanceResult, HOURS, 0),
                            getLong(unbalanceResult, MINUTES, 0),
                            getLong(unbalanceResult, SECONDS, 0),
                            getLong(unbalanceResult, MILLISECONDS, 0),
                            getLong(unbalanceResult, MICROSECONDS, 0),
                            getLong(unbalanceResult, NANOSECONDS, 0),
                            unit, intermediate, getContext().getRealm());
            DynamicObject roundResult = JSTemporalDuration.roundDuration(
                            getLong(unbalanceResult, YEARS, 0),
                            getLong(unbalanceResult, MONTHS, 0),
                            getLong(unbalanceResult, WEEKS, 0),
                            getLong(balanceResult, DAYS, 0),
                            getLong(balanceResult, HOURS, 0),
                            getLong(balanceResult, MINUTES, 0),
                            getLong(balanceResult, SECONDS, 0),
                            getLong(balanceResult, MILLISECONDS, 0),
                            getLong(balanceResult, MICROSECONDS, 0),
                            getLong(balanceResult, NANOSECONDS, 0),
                            1, unit, "trunc", relativeTo, getContext().getRealm());
            long whole = 0;
            if (unit.equals(YEARS)) {
                whole = getLong(roundResult, YEARS, 0);
            }
            if (unit.equals(MONTHS)) {
                whole = getLong(roundResult, MONTHS, 0);
            }
            if (unit.equals(WEEKS)) {
                whole = getLong(roundResult, WEEKS, 0);
            }
            if (unit.equals(DAYS)) {
                whole = getLong(roundResult, DAYS, 0);
            }
            if (unit.equals(HOURS)) {
                whole = getLong(roundResult, HOURS, 0);
            }
            if (unit.equals(MINUTES)) {
                whole = getLong(roundResult, MINUTES, 0);
            }
            if (unit.equals(SECONDS)) {
                whole = getLong(roundResult, SECONDS, 0);
            }
            if (unit.equals(MILLISECONDS)) {
                whole = getLong(roundResult, MILLISECONDS, 0);
            }
            if (unit.equals(MICROSECONDS)) {
                whole = getLong(roundResult, MICROSECONDS, 0);
            }
            return whole + getLong(roundResult, "remainder", 0);
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
            return JSTemporalDuration.temporalDurationToString(duration);
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
