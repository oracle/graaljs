package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
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

import java.util.Collections;

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

        @Specialization(limit = "3")
        protected DynamicObject with(DynamicObject thisObj, DynamicObject temporalDurationLike,
                                     @Cached("create()") IsObjectNode isObjectNode,
                                     @CachedLibrary("temporalDurationLike") DynamicObjectLibrary dol,
                                     @Cached("create()") JSToIntegerAsLongNode toInt,
                                     @Cached("createNew()") JSFunctionCallNode functionCallNode) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            DynamicObject durationLike = JSTemporalDuration.toPartialDuration(temporalDurationLike,
                    getContext().getRealm(), isObjectNode, dol, toInt);

            try {
                long years = dol.getLongOrDefault(durationLike, JSTemporalDuration.YEARS, duration.getYears());
                long months = dol.getLongOrDefault(durationLike, JSTemporalDuration.MONTHS, duration.getMonths());
                long weeks = dol.getLongOrDefault(durationLike, JSTemporalDuration.WEEKS, duration.getWeeks());
                long days = dol.getLongOrDefault(durationLike, JSTemporalDuration.DAYS, duration.getDays());
                long hours = dol.getLongOrDefault(durationLike, JSTemporalDuration.HOURS, duration.getHours());
                long minutes = dol.getLongOrDefault(durationLike, JSTemporalDuration.MINUTES, duration.getMinutes());
                long seconds = dol.getLongOrDefault(durationLike, JSTemporalDuration.SECONDS, duration.getSeconds());
                long milliseconds = dol.getLongOrDefault(durationLike, JSTemporalDuration.MILLISECONDS, duration.getMilliseconds());
                long microseconds = dol.getLongOrDefault(durationLike, JSTemporalDuration.MICROSECONDS, duration.getMicroseconds());
                long nanoseconds = dol.getLongOrDefault(durationLike, JSTemporalDuration.NANOSECONDS, duration.getNanoseconds());
                return JSTemporalDuration.createTemporalDurationFromInstance(duration, years, months, weeks, days,
                        hours, minutes, seconds, milliseconds, microseconds, nanoseconds, getContext().getRealm(),
                        functionCallNode);
            } catch (UnexpectedResultException e) {
                throw Errors.createTypeError(e.getMessage());
            }
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
            return JSTemporalDuration.createTemporalDurationFromInstance(duration,
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
            return JSTemporalDuration.createTemporalDurationFromInstance(duration,
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

        @Specialization(limit = "3")
        protected DynamicObject add(DynamicObject thisObj, DynamicObject other, DynamicObject options,
                                    @Cached("create()") IsObjectNode isObject,
                                    @Cached("create()") JSToStringNode toString,
                                    @Cached("create()") JSToIntegerAsLongNode toInt,
                                    @CachedLibrary("other") DynamicObjectLibrary dol,
                                    @Cached("createNew()") JSFunctionCallNode callNode) {
            try {
                JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
                DynamicObject otherDuration = JSTemporalDuration.toLimitedTemporalDuration(other, Collections.emptySet(),
                        getContext().getRealm(), isObject, toString, toInt, dol);
                DynamicObject normalizedOptions = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(normalizedOptions, isObject, dol);
                DynamicObject result = JSTemporalDuration.addDuration(duration.getYears(), duration.getMonths(),
                        duration.getWeeks(), duration.getDays(), duration.getHours(), duration.getMinutes(),
                        duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(),
                        duration.getNanoseconds(),
                        dol.getLongOrDefault(otherDuration, JSTemporalDuration.YEARS, 0L),
                        dol.getLongOrDefault(otherDuration, JSTemporalDuration.MONTHS, 0L),
                        dol.getLongOrDefault(otherDuration, JSTemporalDuration.WEEKS, 0L),
                        dol.getLongOrDefault(otherDuration, JSTemporalDuration.DAYS, 0L),
                        dol.getLongOrDefault(otherDuration, JSTemporalDuration.HOURS, 0L),
                        dol.getLongOrDefault(otherDuration, JSTemporalDuration.MINUTES, 0L),
                        dol.getLongOrDefault(otherDuration, JSTemporalDuration.SECONDS, 0L),
                        dol.getLongOrDefault(otherDuration, JSTemporalDuration.MILLISECONDS, 0L),
                        dol.getLongOrDefault(otherDuration, JSTemporalDuration.MICROSECONDS, 0L),
                        dol.getLongOrDefault(otherDuration, JSTemporalDuration.NANOSECONDS, 0L),
                        relativeTo, getContext().getRealm(), dol);
                return JSTemporalDuration.createTemporalDurationFromInstance(
                        duration,
                        dol.getLongOrDefault(result, JSTemporalDuration.YEARS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.MONTHS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.WEEKS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.DAYS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.HOURS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.MINUTES, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.SECONDS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.MILLISECONDS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.MICROSECONDS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.NANOSECONDS, 0L),
                        getContext().getRealm(), callNode
                );
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 7.3.19
    public abstract static class JSTemporalDurationSubtract extends JSBuiltinNode {

        protected JSTemporalDurationSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected DynamicObject subtract(DynamicObject thisObj, DynamicObject other, DynamicObject options,
                                    @Cached("create()") IsObjectNode isObject,
                                    @Cached("create()") JSToStringNode toString,
                                    @Cached("create()") JSToIntegerAsLongNode toInt,
                                    @CachedLibrary("other") DynamicObjectLibrary dol,
                                    @Cached("createNew()") JSFunctionCallNode callNode) {
            try {
                JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
                DynamicObject otherDuration = JSTemporalDuration.toLimitedTemporalDuration(other, Collections.emptySet(),
                        getContext().getRealm(), isObject, toString, toInt, dol);
                DynamicObject normalizedOptions = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(normalizedOptions, isObject, dol);
                DynamicObject result = JSTemporalDuration.addDuration(duration.getYears(), duration.getMonths(),
                        duration.getWeeks(), duration.getDays(), duration.getHours(), duration.getMinutes(),
                        duration.getSeconds(), duration.getMilliseconds(), duration.getMicroseconds(),
                        duration.getNanoseconds(),
                        -dol.getLongOrDefault(otherDuration, JSTemporalDuration.YEARS, 0L),
                        -dol.getLongOrDefault(otherDuration, JSTemporalDuration.MONTHS, 0L),
                        -dol.getLongOrDefault(otherDuration, JSTemporalDuration.WEEKS, 0L),
                        -dol.getLongOrDefault(otherDuration, JSTemporalDuration.DAYS, 0L),
                        -dol.getLongOrDefault(otherDuration, JSTemporalDuration.HOURS, 0L),
                        -dol.getLongOrDefault(otherDuration, JSTemporalDuration.MINUTES, 0L),
                        -dol.getLongOrDefault(otherDuration, JSTemporalDuration.SECONDS, 0L),
                        -dol.getLongOrDefault(otherDuration, JSTemporalDuration.MILLISECONDS, 0L),
                        -dol.getLongOrDefault(otherDuration, JSTemporalDuration.MICROSECONDS, 0L),
                        -dol.getLongOrDefault(otherDuration, JSTemporalDuration.NANOSECONDS, 0L),
                        relativeTo, getContext().getRealm(), dol);
                return JSTemporalDuration.createTemporalDurationFromInstance(
                        duration,
                        dol.getLongOrDefault(result, JSTemporalDuration.YEARS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.MONTHS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.WEEKS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.DAYS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.HOURS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.MINUTES, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.SECONDS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.MILLISECONDS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.MICROSECONDS, 0L),
                        dol.getLongOrDefault(result, JSTemporalDuration.NANOSECONDS, 0L),
                        getContext().getRealm(), callNode
                );
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 7.3.20
    public abstract static class JSTemporalDurationRound extends JSBuiltinNode {

        protected JSTemporalDurationRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected DynamicObject round(DynamicObject thisObj, DynamicObject options,
                                         @Cached("create()") IsObjectNode isObject,
                                         @Cached("create()") JSToStringNode toString,
                                         @Cached("create()") JSToBooleanNode toBoolean,
                                         @Cached("create()") JSToNumberNode toNumber,
                                         @CachedLibrary("options") DynamicObjectLibrary dol,
                                         @Cached("createNew()") JSFunctionCallNode callNode) {
            try {
                JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
                if (options == null) {
                    throw Errors.createTypeError("No options given.");
                }
                DynamicObject normalizedOptions = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                boolean smallestUnitPresent = true;
                boolean largestUnitPresent = true;
                String smallestUnit = TemporalUtil.toSmallestTemporalDurationUnit(options, null,
                        Collections.emptySet(), dol, isObject, toBoolean, toString);
                if (smallestUnit == null) {
                    smallestUnitPresent = false;
                    smallestUnit = JSTemporalDuration.NANOSECONDS;
                }
                String defaultLargestUnit = JSTemporalDuration.defaultTemporalLargestUnit(duration.getYears(),
                        duration.getMonths(), duration.getWeeks(), duration.getDays(), duration.getHours(),
                        duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                        duration.getMicroseconds(), duration.getNanoseconds());
                defaultLargestUnit = TemporalUtil.largerOfTwoTemporalDurationUnits(defaultLargestUnit, smallestUnit);
                String largestUnit = TemporalUtil.toLargestTemporalUnit(normalizedOptions, Collections.emptySet(),
                        null, dol, isObject, toBoolean, toString);
                if (largestUnit == null) {
                    largestUnitPresent = false;
                    largestUnit = defaultLargestUnit;
                }
                TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
                String roundingMode = TemporalUtil.toTemporalRoundingMode(normalizedOptions, "nearest", dol,
                        isObject, toBoolean, toString);
                Long maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
                double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options,
                        maximum == null ? null : maximum.doubleValue(), false, dol, isObject, toNumber);
                DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(options, isObject, dol);
                DynamicObject unbalanceResult = JSTemporalDuration.unbalanceDurationRelative(duration.getYears(),
                        duration.getMonths(), duration.getWeeks(), duration.getDays(), largestUnit, relativeTo, dol,
                        getContext().getRealm());
                DynamicObject roundResult = JSTemporalDuration.roundDuration(
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.YEARS, 0),
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.MONTHS, 0),
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.WEEKS, 0),
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.DAYS, 0),
                        duration.getHours(), duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                        duration.getMicroseconds(), duration.getNanoseconds(), (long) roundingIncrement, smallestUnit,
                        roundingMode, relativeTo, dol, getContext().getRealm()
                );
                DynamicObject adjustResult = JSTemporalDuration.adjustRoundedDurationDays(
                        dol.getLongOrDefault(roundResult, JSTemporalDuration.YEARS, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalDuration.MONTHS, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalDuration.WEEKS, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalDuration.DAYS, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(roundResult, JSTemporalDuration.NANOSECONDS, 0),
                        (long) roundingIncrement, smallestUnit, roundingMode, relativeTo, dol, getContext().getRealm()
                );
                DynamicObject balanceResult = JSTemporalDuration.balanceDurationRelative(
                        dol.getLongOrDefault(adjustResult, JSTemporalDuration.YEARS, 0),
                        dol.getLongOrDefault(adjustResult, JSTemporalDuration.MONTHS, 0),
                        dol.getLongOrDefault(adjustResult, JSTemporalDuration.WEEKS, 0),
                        dol.getLongOrDefault(adjustResult, JSTemporalDuration.DAYS, 0),
                        largestUnit, relativeTo, dol, getContext().getRealm()
                );
                // TODO: Check if relativeTo has InitializedTemporalZonedDateTime 7.3.22.22
                DynamicObject result = JSTemporalDuration.balanceDuration(
                        dol.getLongOrDefault(balanceResult, JSTemporalDuration.DAYS, 0),
                        dol.getLongOrDefault(adjustResult, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(adjustResult, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(adjustResult, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(adjustResult, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(adjustResult, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(adjustResult, JSTemporalDuration.NANOSECONDS, 0),
                        largestUnit, relativeTo, getContext().getRealm()
                );
                return JSTemporalDuration.createTemporalDurationFromInstance(
                        duration,
                        dol.getLongOrDefault(balanceResult, JSTemporalDuration.YEARS, 0),
                        dol.getLongOrDefault(balanceResult, JSTemporalDuration.MONTHS, 0),
                        dol.getLongOrDefault(balanceResult, JSTemporalDuration.WEEKS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.DAYS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(result, JSTemporalDuration.NANOSECONDS, 0),
                        getContext().getRealm(), callNode
                );
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 7.3.21
    public abstract static class JSTemporalDurationTotal extends JSBuiltinNode {

        protected JSTemporalDurationTotal(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected long total(DynamicObject thisObj, DynamicObject options,
                             @Cached("create()") IsObjectNode isObject,
                             @CachedLibrary("options") DynamicObjectLibrary dol,
                             @Cached("create()") JSToBooleanNode toBoolean,
                             @Cached("create()") JSToStringNode toString) {
            try {
                JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
                DynamicObject normalizedOptions = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(normalizedOptions, isObject, dol);
                String unit = TemporalUtil.toTemporalDurationTotalUnit(normalizedOptions, dol, isObject, toBoolean, toString);
                if(unit == null) {
                    throw Errors.createRangeError("Unit not defined.");
                }
                DynamicObject unbalanceResult = JSTemporalDuration.unbalanceDurationRelative(duration.getYears(),
                        duration.getMonths(), duration.getWeeks(), duration.getDays(), unit, relativeTo, dol,
                        getContext().getRealm());
                DynamicObject intermediate = null;
                // TODO: Check if relative has InitializedTemporalZonedDateTime. If yes intermediate = moveRelativeZonedDateTime()
                DynamicObject balanceResult = JSTemporalDuration.balanceDuration(
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.DAYS, 0),
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.NANOSECONDS, 0),
                        unit, intermediate, getContext().getRealm()
                );
                DynamicObject roundResult = JSTemporalDuration.roundDuration(
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.YEARS, 0),
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.MONTHS, 0),
                        dol.getLongOrDefault(unbalanceResult, JSTemporalDuration.WEEKS, 0),
                        dol.getLongOrDefault(balanceResult, JSTemporalDuration.DAYS, 0),
                        dol.getLongOrDefault(balanceResult, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(balanceResult, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(balanceResult, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(balanceResult, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(balanceResult, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(balanceResult, JSTemporalDuration.NANOSECONDS, 0),
                        1, unit,"trunc", relativeTo, dol, getContext().getRealm()
                );
                long whole = 0;
                if (unit.equals(JSTemporalDuration.YEARS)) {
                    whole = dol.getLongOrDefault(roundResult, JSTemporalDuration.YEARS, 0);
                }
                if (unit.equals(JSTemporalDuration.MONTHS)) {
                    whole = dol.getLongOrDefault(roundResult, JSTemporalDuration.MONTHS, 0);
                }
                if (unit.equals(JSTemporalDuration.WEEKS)) {
                    whole = dol.getLongOrDefault(roundResult, JSTemporalDuration.WEEKS, 0);
                }
                if (unit.equals(JSTemporalDuration.DAYS)) {
                    whole = dol.getLongOrDefault(roundResult, JSTemporalDuration.DAYS, 0);
                }
                if (unit.equals(JSTemporalDuration.HOURS)) {
                    whole = dol.getLongOrDefault(roundResult, JSTemporalDuration.HOURS, 0);
                }
                if (unit.equals(JSTemporalDuration.MINUTES)) {
                    whole = dol.getLongOrDefault(roundResult, JSTemporalDuration.MINUTES, 0);
                }
                if (unit.equals(JSTemporalDuration.SECONDS)) {
                    whole = dol.getLongOrDefault(roundResult, JSTemporalDuration.SECONDS, 0);
                }
                if (unit.equals(JSTemporalDuration.MILLISECONDS)) {
                    whole = dol.getLongOrDefault(roundResult, JSTemporalDuration.MILLISECONDS, 0);
                }
                if (unit.equals(JSTemporalDuration.MICROSECONDS)) {
                    whole = dol.getLongOrDefault(roundResult, JSTemporalDuration.MICROSECONDS, 0);
                }
                return whole + dol.getLongOrDefault(roundResult, "remainder", 0);
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
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
        protected Object valueOf(DynamicObject thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }
}
