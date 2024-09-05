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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalLong;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetInstantForNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetNextOrPreviousTransitionNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetOffsetNanosecondsForNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetOffsetStringForNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetPlainDateTimeForNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetPossibleInstantsForNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetterNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.CreateTimeZoneMethodsRecordNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarSlotValueNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDateTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalInstantNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneIdentifierNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalTimeZoneSlotValueNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZone;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZoneObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Disambiguation;

public class TemporalTimeZonePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalTimeZonePrototypeBuiltins.TemporalTimeZonePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalTimeZonePrototypeBuiltins();

    protected TemporalTimeZonePrototypeBuiltins() {
        super(JSTemporalTimeZone.PROTOTYPE_NAME, TemporalTimeZonePrototype.class);
    }

    public enum TemporalTimeZonePrototype implements BuiltinEnum<TemporalTimeZonePrototype> {
        // getters
        id(0),

        // methods
        equals(1),
        getOffsetNanosecondsFor(1),
        getOffsetStringFor(1),
        getPlainDateTimeFor(1),
        getInstantFor(1),
        getPossibleInstantsFor(1),
        getNextTransition(1),
        getPreviousTransition(1),
        toString(0),
        toJSON(0),
        valueOf(0);

        private final int length;

        TemporalTimeZonePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(id).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalTimeZonePrototype builtinEnum) {
        switch (builtinEnum) {
            case id:
            case toString:
            case toJSON:
                return JSTemporalTimeZoneGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

            case equals:
                return JSTemporalTimeZoneEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case getOffsetNanosecondsFor:
                return JSTemporalTimeZoneGetOffsetNanosecondsForNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case getOffsetStringFor:
                return JSTemporalTimeZoneGetOffsetStringForNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case getPlainDateTimeFor:
                return JSTemporalTimeZoneGetPlainDateTimeForNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case getInstantFor:
                return JSTemporalTimeZoneGetInstantForNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case getPossibleInstantsFor:
                return JSTemporalTimeZoneGetPossibleInstantsForNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case getNextTransition:
                return JSTemporalTimeZoneGetNextOrPreviousTransitionNodeGen.create(context, builtin, true, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case getPreviousTransition:
                return JSTemporalTimeZoneGetNextOrPreviousTransitionNodeGen.create(context, builtin, false, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case valueOf:
                return UnsupportedValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalTimeZoneGetterNode extends JSBuiltinNode {

        protected final TemporalTimeZonePrototype property;

        protected JSTemporalTimeZoneGetterNode(JSContext context, JSBuiltin builtin, TemporalTimeZonePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization
        protected TruffleString id(JSTemporalTimeZoneObject timeZone,
                        @Cached JSToStringNode toStringNode) {
            switch (property) {
                case id:
                case toString:
                    return timeZone.getIdentifier();
                case toJSON:
                    return toStringNode.executeString(timeZone);
                default:
                    throw CompilerDirectives.shouldNotReachHere();
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalTimeZone(thisObj)")
        protected static Object invalidReceiver(Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalTimeZoneExpected();
        }
    }

    public abstract static class JSTemporalTimeZoneEquals extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneEquals(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean equals(JSTemporalTimeZoneObject timeZone, Object timeZoneLike,
                        @Cached ToTemporalTimeZoneSlotValueNode toTimeZoneSlotValue,
                        @Cached ToTemporalTimeZoneIdentifierNode toTimeZoneIdentifier) {
            Object other = toTimeZoneSlotValue.execute(timeZoneLike);
            return TemporalUtil.timeZoneEquals(timeZone, other, toTimeZoneIdentifier);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalTimeZone(thisObj)")
        protected static boolean invalidReceiver(Object thisObj, Object timeZoneLike) {
            throw TemporalErrors.createTypeErrorTemporalTimeZoneExpected();
        }
    }

    public abstract static class JSTemporalTimeZoneGetOffsetNanosecondsFor extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneGetOffsetNanosecondsFor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected double getOffsetNanosecondsFor(JSTemporalTimeZoneObject timeZone, Object instantParam,
                        @Cached ToTemporalInstantNode toTemporalInstantNode) {
            JSTemporalInstantObject instant = toTemporalInstantNode.execute(instantParam);
            if (timeZone.getNanoseconds() != null) {
                return timeZone.getNanoseconds().doubleValue();
            }
            return TemporalUtil.getIANATimeZoneOffsetNanoseconds(instant.getNanoseconds(), timeZone.getIdentifier());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalTimeZone(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object instantParam) {
            throw TemporalErrors.createTypeErrorTemporalTimeZoneExpected();
        }
    }

    public abstract static class JSTemporalTimeZoneGetOffsetStringFor extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneGetOffsetStringFor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString getOffsetStringFor(JSTemporalTimeZoneObject timeZone, Object instantParam,
                        @Cached ToTemporalInstantNode toTemporalInstantNode,
                        @Cached CreateTimeZoneMethodsRecordNode createTimeZoneMethodsRecord) {
            JSDynamicObject instant = toTemporalInstantNode.execute(instantParam);
            var timeZoneRec = createTimeZoneMethodsRecord.executeOnlyGetOffsetNanosecondsFor(timeZone);
            return TemporalUtil.builtinTimeZoneGetOffsetStringFor(getContext(), getRealm(), timeZoneRec, instant);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalTimeZone(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object instantParam) {
            throw TemporalErrors.createTypeErrorTemporalTimeZoneExpected();
        }
    }

    public abstract static class JSTemporalTimeZoneGetPlainDateTimeFor extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneGetPlainDateTimeFor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalPlainDateTimeObject getPlainDateTimeFor(JSTemporalTimeZoneObject timeZone, Object instantParam, Object calendarLike,
                        @Cached("createWithISO8601()") ToTemporalCalendarSlotValueNode toCalendarSlotValue,
                        @Cached ToTemporalInstantNode toTemporalInstantNode,
                        @Cached CreateTimeZoneMethodsRecordNode createTimeZoneMethodsRecord) {
            JSTemporalInstantObject instant = toTemporalInstantNode.execute(instantParam);
            Object calendar = toCalendarSlotValue.execute(calendarLike);
            var timeZoneRec = createTimeZoneMethodsRecord.executeOnlyGetOffsetNanosecondsFor(timeZone);
            return TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), getRealm(), timeZoneRec, instant, calendar);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalTimeZone(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object instantParam, Object calendarLike) {
            throw TemporalErrors.createTypeErrorTemporalTimeZoneExpected();
        }
    }

    public abstract static class JSTemporalTimeZoneGetInstantFor extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneGetInstantFor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalInstantObject getInstantFor(JSTemporalTimeZoneObject timeZone, Object dateTimeParam, Object optionsParam,
                        @Cached ToTemporalDateTimeNode toTemporalDateTime,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined,
                        @Cached CreateTimeZoneMethodsRecordNode createTimeZoneMethodsRecord) {
            JSTemporalPlainDateTimeObject dateTime = toTemporalDateTime.execute(dateTimeParam, Undefined.instance);
            JSDynamicObject options = getOptionsObject(optionsParam, this, errorBranch, optionUndefined);
            Disambiguation disambiguation = TemporalUtil.toTemporalDisambiguation(options, getOptionNode, equalNode);
            var timeZoneRec = createTimeZoneMethodsRecord.executeFull(timeZone);
            return TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), getRealm(), timeZoneRec, dateTime, disambiguation);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalTimeZone(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object dateTimeParam, Object optionsParam) {
            throw TemporalErrors.createTypeErrorTemporalTimeZoneExpected();
        }
    }

    public abstract static class JSTemporalTimeZoneGetPossibleInstantsFor extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneGetPossibleInstantsFor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected JSDynamicObject getPossibleInstantsFor(JSTemporalTimeZoneObject timeZone, Object dateTimeParam,
                        @Cached ToTemporalDateTimeNode toTemporalDateTime) {
            JSTemporalPlainDateTimeObject dateTime = toTemporalDateTime.execute(dateTimeParam, Undefined.instance);
            JSRealm realm = getRealm();
            if (timeZone.getNanoseconds() != null) {
                BigInt epochNanoseconds = TemporalUtil.getUTCEpochNanoseconds(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(),
                                dateTime.getSecond(), dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond());
                Object instant = JSTemporalInstant.create(getContext(), realm, epochNanoseconds.subtract(timeZone.getNanoseconds()));
                List<Object> list = new ArrayList<>();
                list.add(instant);
                return JSRuntime.createArrayFromList(getContext(), realm, list);
            }
            List<BigInt> possibleEpochNanoseconds = TemporalUtil.getIANATimeZoneEpochValue(timeZone.getIdentifier(), dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(),
                            dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond());
            List<Object> possibleInstants = new ArrayList<>();
            for (BigInt epochNanoseconds : possibleEpochNanoseconds) {
                if (!TemporalUtil.isValidEpochNanoseconds(epochNanoseconds)) {
                    throw TemporalErrors.createRangeErrorInvalidNanoseconds();
                }
                JSDynamicObject instant = JSTemporalInstant.create(getContext(), realm, epochNanoseconds);
                possibleInstants.add(instant);
            }
            return JSRuntime.createArrayFromList(getContext(), realm, possibleInstants);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalTimeZone(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object dateTimeParam) {
            throw TemporalErrors.createTypeErrorTemporalTimeZoneExpected();
        }
    }

    public abstract static class JSTemporalTimeZoneGetNextOrPreviousTransition extends JSTemporalBuiltinOperation {

        private final boolean isNext;

        protected JSTemporalTimeZoneGetNextOrPreviousTransition(JSContext context, JSBuiltin builtin, boolean isNext) {
            super(context, builtin);
            this.isNext = isNext;
        }

        @Specialization
        protected Object getTransition(JSTemporalTimeZoneObject timeZone, Object startingPointParam,
                        @Cached ToTemporalInstantNode toTemporalInstantNode) {
            JSTemporalInstantObject startingPoint = toTemporalInstantNode.execute(startingPointParam);
            if (timeZone.getNanoseconds() != null) {
                return Null.instance;
            }
            OptionalLong transition;
            if (isNext) {
                transition = TemporalUtil.getIANATimeZoneNextTransition(startingPoint.getNanoseconds(), timeZone.getIdentifier());
            } else {
                transition = TemporalUtil.getIANATimeZonePreviousTransition(startingPoint.getNanoseconds(), timeZone.getIdentifier());
            }
            if (transition.isEmpty()) {
                return Null.instance;
            }
            // orElse avoids Exception
            return JSTemporalInstant.create(getContext(), getRealm(), BigInt.valueOf(transition.orElse(0)));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalTimeZone(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object startingPointParam) {
            throw TemporalErrors.createTypeErrorTemporalTimeZoneExpected();
        }
    }

}
