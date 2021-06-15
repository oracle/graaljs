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

import java.math.BigInteger;
import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalZonedDateTimePrototypeBuiltinsFactory.JSTemporalZonedDateTimeValueOfNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPrecisionRecord;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalZonedDateTimePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalZonedDateTimePrototypeBuiltins.TemporalZonedDateTimePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalZonedDateTimePrototypeBuiltins();

    protected TemporalZonedDateTimePrototypeBuiltins() {
        super(JSTemporalZonedDateTime.PROTOTYPE_NAME, TemporalZonedDateTimePrototype.class);
    }

    public enum TemporalZonedDateTimePrototype implements BuiltinEnum<TemporalZonedDateTimePrototype> {
        // getters
        calendar(0),
        // FIXME and many more

        // methods
        toString(0),
        toLocaleString(0),
        toJSON(0),
        valueOf(0);
        // FIXME and many more

        private final int length;

        TemporalZonedDateTimePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(calendar).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalZonedDateTimePrototype builtinEnum) {
        switch (builtinEnum) {
            case calendar:
                return JSTemporalZonedDateTimeGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

            case toString:
                return JSTemporalZonedDateTimeToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalZonedDateTimeToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSTemporalZonedDateTimeValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));

        }
        return null;
    }

    public abstract static class JSTemporalZonedDateTimeGetterNode extends JSBuiltinNode {

        public final TemporalZonedDateTimePrototype property;

        public JSTemporalZonedDateTimeGetterNode(JSContext context, JSBuiltin builtin, TemporalZonedDateTimePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization(guards = "isJSTemporalZonedDateTime(thisObj)")
        protected BigInt ZonedDateTimeGetter(Object thisObj) {
            JSTemporalZonedDateTimeObject ZonedDateTime = (JSTemporalZonedDateTimeObject) thisObj;
            BigInteger ns = ZonedDateTime.getNanoseconds().bigIntegerValue();
            switch (property) {
                case calendar:
                    return TemporalUtil.roundTowardsZero(ns.divide(BigInteger.valueOf(1_000_000_000L)));

            }
            CompilerDirectives.transferToInterpreter();
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalZonedDateTime(thisObj)")
        protected static int error(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
    }

    public abstract static class JSTemporalZonedDateTimeToString extends JSBuiltinNode {

        protected JSTemporalZonedDateTimeToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(Object thisObj, Object optionsParam,
                        @Cached("create()") JSToStringNode toString,
                        @Cached("create()") JSToBooleanNode toBoolean) {
            DynamicObject zonedDateTime = TemporalUtil.requireTemporalTimeZone(thisObj);
            DynamicObject options = TemporalUtil.getOptionsObject(getContext(), optionsParam);
            JSTemporalPrecisionRecord precision = TemporalUtil.toSecondsStringPrecision(options, toBoolean, toString);
            String roundingMode = TemporalUtil.toTemporalRoundingMode(options, TemporalConstants.TRUNC, toBoolean, toString);
            String showCalendar = TemporalUtil.toShowCalendarOption(options);
            String showTimeZone = TemporalUtil.toShowTimeZoneNameOption(options, toBoolean, toString);
            String showOffset = TemporalUtil.toShowOffsetOption(options, toBoolean, toString);
            return TemporalUtil.temporalZonedDateTimeToString(getContext(), zonedDateTime, precision.getPrecision(), showCalendar, showTimeZone, showOffset, precision.getIncrement(),
                            precision.getUnit(), roundingMode);
        }
    }

    public abstract static class JSTemporalZonedDateTimeToLocaleString extends JSBuiltinNode {

        protected JSTemporalZonedDateTimeToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toLocaleString(Object thisObj) {
            DynamicObject zonedDateTime = TemporalUtil.requireTemporalTimeZone(thisObj);
            return TemporalUtil.temporalZonedDateTimeToString(getContext(), zonedDateTime, AUTO, AUTO, AUTO, AUTO);
        }
    }

    public abstract static class JSTemporalZonedDateTimeValueOf extends JSBuiltinNode {

        protected JSTemporalZonedDateTimeValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }

}
