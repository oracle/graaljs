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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.DAYS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HALF_EXPAND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.HOUR;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MICROSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MILLISECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MINUTE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.MONTHS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.NANOSECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SECOND;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TRUNC;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.WEEKS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.YEARS;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.TIME_ZONE;

import java.math.BigInteger;
import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantAddNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantRoundNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantSubtractNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantToZonedDateTimeISONodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantToZonedDateTimeNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantUntilSinceNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantPrototypeBuiltinsFactory.JSTemporalInstantValueOfNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

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
        until(2),
        since(2),
        round(1),
        equals(1),
        toString(1),
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
                return JSTemporalInstantAddNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case subtract:
                return JSTemporalInstantSubtractNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
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
                return JSTemporalInstantValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toZonedDateTime:
                return JSTemporalInstantToZonedDateTimeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toZonedDateTimeISO:
                return JSTemporalInstantToZonedDateTimeISONodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));

        }
        return null;
    }

    public abstract static class JSTemporalInstantGetterNode extends JSBuiltinNode {

        public final TemporalInstantPrototype property;

        public JSTemporalInstantGetterNode(JSContext context, JSBuiltin builtin, TemporalInstantPrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization(guards = "isJSTemporalInstant(thisObj)")
        protected BigInt instantGetter(Object thisObj) {
            JSTemporalInstantObject instant = (JSTemporalInstantObject) thisObj;
            BigInteger ns = instant.getNanoseconds().bigIntegerValue();
            switch (property) {
                case epochSeconds:
                    return TemporalUtil.roundTowardsZero(ns.divide(BigInteger.valueOf(1_000_000_000L)));
                case epochMilliseconds:
                    return TemporalUtil.roundTowardsZero(ns.divide(BigInteger.valueOf(1_000_000L)));
                case epochMicroseconds:
                    return TemporalUtil.roundTowardsZero(ns.divide(BigInteger.valueOf(1_000L)));
                case epochNanoseconds:
                    return instant.getNanoseconds();
            }
            CompilerDirectives.transferToInterpreter();
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "isJSTemporalInstant(thisObj)")
        protected static int error(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
    }

    // 4.3.10
    public abstract static class JSTemporalInstantAdd extends JSBuiltinNode {

        protected JSTemporalInstantAdd(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject add(DynamicObject thisObj, Object temporalDurationLike,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            JSTemporalInstantObject instant = TemporalUtil.requireTemporalInstant(thisObj);
            JSTemporalDurationRecord duration = TemporalUtil.toLimitedTemporalDuration(temporalDurationLike, TemporalUtil.toSet(YEARS, MONTHS, WEEKS, DAYS), isObject, toString, toInt);
            BigInt ns = TemporalUtil.addInstant(instant.getNanoseconds(), duration.getHours(), duration.getMinutes(), duration.getSeconds(),
                            duration.getMilliseconds(), duration.getMicroseconds(), duration.getNanoseconds());
            return TemporalUtil.createTemporalInstant(ns, getContext());
        }
    }

    public abstract static class JSTemporalInstantSubtract extends JSBuiltinNode {

        protected JSTemporalInstantSubtract(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject subtract(DynamicObject thisObj, Object temporalDurationLike,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToIntegerAsLongNode toInt) {
            JSTemporalInstantObject instant = TemporalUtil.requireTemporalInstant(thisObj);
            JSTemporalDurationRecord duration = TemporalUtil.toLimitedTemporalDuration(temporalDurationLike, TemporalUtil.toSet(YEARS, MONTHS, WEEKS, DAYS), isObject, toString, toInt);
            BigInt ns = TemporalUtil.addInstant(instant.getNanoseconds(), -duration.getHours(), -duration.getMinutes(), -duration.getSeconds(),
                            -duration.getMilliseconds(), -duration.getMicroseconds(), -duration.getNanoseconds());
            return TemporalUtil.createTemporalInstant(ns, getContext());
        }
    }

    public abstract static class JSTemporalInstantUntilSinceNode extends JSBuiltinNode {

        private final boolean isUntil;

        protected JSTemporalInstantUntilSinceNode(JSContext context, JSBuiltin builtin, boolean isUntil) {
            super(context, builtin);
            this.isUntil = isUntil;
        }

        @Specialization
        public DynamicObject untilOrSince(Object thisObj, Object otherObj, Object optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber) {
            JSTemporalInstantObject instant = TemporalUtil.requireTemporalInstant(thisObj);
            JSTemporalInstantObject other = (JSTemporalInstantObject) TemporalUtil.toTemporalInstant(otherObj, getContext());
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext());
            String smallestUnit = TemporalUtil.toSmallestTemporalUnit(options, TemporalUtil.setYMWD, NANOSECOND, toBoolean, toString);
            String defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(SECOND, smallestUnit);
            String largestUnit = TemporalUtil.toLargestTemporalUnit(options, TemporalUtil.setYMWD, AUTO, defaultLargestUnit, toBoolean, toString);
            TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, TRUNC, toBoolean, toString);
            Double maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
            Double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, maximum, false, isObject, toNumber);

            BigInt one = isUntil ? instant.getNanoseconds() : other.getNanoseconds();
            BigInt two = isUntil ? other.getNanoseconds() : instant.getNanoseconds();

            long roundedNs = TemporalUtil.differenceInstant(one, two, roundingIncrement, smallestUnit, roundingMode);
            JSTemporalDurationRecord result = TemporalUtil.balanceDuration(0, 0, 0, 0, 0, 0, roundedNs, largestUnit);
            return JSTemporalDuration.createTemporalDuration(0, 0, 0, 0, result.getHours(), result.getMinutes(), result.getSeconds(),
                            result.getMilliseconds(), result.getMicroseconds(), result.getNanoseconds(), getContext());
        }
    }

    public abstract static class JSTemporalInstantRound extends JSBuiltinNode {

        protected JSTemporalInstantRound(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public DynamicObject round(Object thisObj, Object optionsParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToNumberNode toNumber) {
            JSTemporalInstantObject instant = TemporalUtil.requireTemporalInstant(thisObj);
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext());
            String smallestUnit = TemporalUtil.toSmallestTemporalUnit(options, TemporalUtil.setYMWD, null, toBoolean, toString);
            if (smallestUnit == null) {
                throw TemporalErrors.createRangeErrorSmallestUnitExpected();
            }
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, HALF_EXPAND, toBoolean, toString);
            double maximum;
            if (HOUR.equals(smallestUnit)) {
                maximum = 24;
            } else if (MINUTE.equals(smallestUnit)) {
                maximum = 1440;
            } else if (SECOND.equals(smallestUnit)) {
                maximum = 86400;
            } else if (MILLISECOND.equals(smallestUnit)) {
                maximum = 8.64 * 10_000_000;
            } else if (MICROSECOND.equals(smallestUnit)) {
                maximum = 8.64 * 10_000_000_000d;
            } else {
                assert NANOSECOND.equals(smallestUnit);
                maximum = 8.64 * 10_000_000_000_000d;
            }
            double roundingIncrement = TemporalUtil.toTemporalRoundingIncrement(options, maximum, true, isObject, toNumber);
            long ns = instant.getNanoseconds().longValue();
            long roundedNs = (long) TemporalUtil.roundTemporalInstant(ns, roundingIncrement, smallestUnit, roundingMode);
            return TemporalUtil.createTemporalInstant(new BigInt(BigInteger.valueOf(roundedNs)), getContext());
        }
    }

    public abstract static class JSTemporalInstantEquals extends JSBuiltinNode {

        protected JSTemporalInstantEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public boolean equals(Object thisObj, Object otherObj) {
            JSTemporalInstantObject instant = TemporalUtil.requireTemporalInstant(thisObj);
            JSTemporalInstantObject other = (JSTemporalInstantObject) TemporalUtil.toTemporalInstant(otherObj, getContext());
            if (instant.getNanoseconds().equals(other.getNanoseconds())) {
                return false;
            }
            return true;
        }
    }

    public abstract static class JSTemporalInstantToString extends JSBuiltinNode {

        protected JSTemporalInstantToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(Object thisObj, Object optionsParam,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean) {
            JSTemporalInstantObject instant = TemporalUtil.requireTemporalInstant(thisObj);
            DynamicObject options = TemporalUtil.getOptionsObject(optionsParam, getContext());
            Object timeZoneRaw = JSObject.get(options, TIME_ZONE);
            DynamicObject timeZone = Undefined.instance;
            if (timeZoneRaw != Undefined.instance) {
                timeZone = TemporalUtil.toTemporalTimeZone(timeZoneRaw, getContext());
            }
            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecision(options, toBoolean, toString);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, TRUNC, toBoolean, toString);
            BigInt ns = instant.getNanoseconds();
            double roundedNs = TemporalUtil.roundTemporalInstant(ns.longValue(), precision.getIncrement(), precision.getUnit(), roundingMode);
            DynamicObject roundedInstant = TemporalUtil.createTemporalInstant((long) roundedNs, getContext());
            return TemporalUtil.temporalInstantToString(roundedInstant, timeZone, precision.getPrecision(), getContext());
        }
    }

    public abstract static class JSTemporalInstantToLocaleString extends JSBuiltinNode {

        protected JSTemporalInstantToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization
        public String toLocaleString(Object thisObj) {
            JSTemporalInstantObject instant = TemporalUtil.requireTemporalInstant(thisObj);
            return TemporalUtil.temporalInstantToString(instant, Undefined.instance, AUTO, getContext());
        }
    }

    public abstract static class JSTemporalInstantValueOf extends JSBuiltinNode {

        protected JSTemporalInstantValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }

    public abstract static class JSTemporalInstantToZonedDateTimeNode extends JSBuiltinNode {

        protected JSTemporalInstantToZonedDateTimeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @SuppressWarnings("unused")
        protected Object toZonedDateTime(Object thisObj, Object item) {
            return Undefined.instance; // TODO
        }
    }

    public abstract static class JSTemporalInstantToZonedDateTimeISONode extends JSBuiltinNode {

        protected JSTemporalInstantToZonedDateTimeISONode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @SuppressWarnings("unused")
        protected Object toZonedDateTimeISO(Object thisObj, Object item) {
            return Undefined.instance; // TODO
        }
    }

}
