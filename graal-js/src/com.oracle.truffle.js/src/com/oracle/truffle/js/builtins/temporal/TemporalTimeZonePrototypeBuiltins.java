/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalLong;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDatePrototypeBuiltins.JSTemporalBuiltinOperation;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetInstantForNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetNextOrPreviousTransitionNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetOffsetNanosecondsForNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetOffsetStringForNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetPlainDateTimeForNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetPossibleInstantsForNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneToJSONNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalTimeZonePrototypeBuiltinsFactory.JSTemporalTimeZoneValueOfNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarWithISODefaultNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalDateTimeNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalInstantNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainTime;
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
                return JSTemporalTimeZoneGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

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
            case toString:
                return JSTemporalTimeZoneToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toJSON:
                return JSTemporalTimeZoneToJSONNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return JSTemporalTimeZoneValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSTemporalTimeZoneGetterNode extends JSBuiltinNode {

        public final TemporalTimeZonePrototype property;

        public JSTemporalTimeZoneGetterNode(JSContext context, JSBuiltin builtin, TemporalTimeZonePrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isJSTemporalTimeZone(thisObj)")
        protected TruffleString timeZoneGetter(Object thisObj,
                        @Cached JSToStringNode toStringNode) {
            JSTemporalTimeZoneObject timeZone = (JSTemporalTimeZoneObject) thisObj;
            switch (property) {
                case id:
                    return toStringNode.executeString(timeZone);
            }
            CompilerDirectives.transferToInterpreter();
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalTimeZone(thisObj)")
        protected static int error(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalTimeZoneExpected();
        }

        @CompilerDirectives.TruffleBoundary
        public static JSTemporalTimeZoneObject javaTimeZoneToTimeZone(Object thisObj, InteropLibrary interop, JSContext ctx) {
            try {
                java.time.ZoneId zone = interop.asTimeZone(thisObj);
                TruffleString identifier = TruffleString.fromJavaStringUncached(zone.getId(), TruffleString.Encoding.UTF_32);

                return JSTemporalTimeZone.create(ctx, null, identifier);
            } catch (UnsupportedMessageException e) {
                return null;
            }
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSTemporalTimeZoneToString extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(Object thisObj,
                                         @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            return requireTemporalTimeZone(thisObj, interop, getContext()).getIdentifier();
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSTemporalTimeZoneToJSON extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneToJSON(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toJSON(Object thisObj,
                        @Cached("create()") JSToStringNode toString,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            JSTemporalTimeZoneObject timeZone = requireTemporalTimeZone(thisObj, interop, getContext());
            return toString.executeString(timeZone);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSTemporalTimeZoneValueOf extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneValueOf(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("Not supported.");
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSTemporalTimeZoneGetOffsetNanosecondsFor extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneGetOffsetNanosecondsFor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected double getOffsetNanosecondsFor(Object thisObj, Object instantParam,
                        @Cached("create(getContext())") ToTemporalInstantNode toTemporalInstantNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            JSTemporalTimeZoneObject timeZone = requireTemporalTimeZone(thisObj, interop, getContext());
            JSTemporalInstantObject instant = toTemporalInstantNode.execute(instantParam);
            if (timeZone.getNanoseconds() != null) {
                return timeZone.getNanoseconds().doubleValue();
            }
            return TemporalUtil.getIANATimeZoneOffsetNanoseconds(instant.getNanoseconds(), timeZone.getIdentifier());
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSTemporalTimeZoneGetOffsetStringFor extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneGetOffsetStringFor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString getOffsetStringFor(Object thisObj, Object instantParam,
                        @Cached("create(getContext())") ToTemporalInstantNode toTemporalInstantNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            JSTemporalTimeZoneObject timeZone = requireTemporalTimeZone(thisObj, interop, getContext());
            JSDynamicObject instant = toTemporalInstantNode.execute(instantParam);
            return TemporalUtil.builtinTimeZoneGetOffsetStringFor(timeZone, instant);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSTemporalTimeZoneGetPlainDateTimeFor extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneGetPlainDateTimeFor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject getPlainDateTimeFor(Object thisObj, Object instantParam, Object calendarLike,
                        @Cached("create(getContext())") ToTemporalCalendarWithISODefaultNode toTemporalCalendarWithISODefaultNode,
                        @Cached("create(getContext())") ToTemporalInstantNode toTemporalInstantNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            JSTemporalTimeZoneObject timeZone = requireTemporalTimeZone(thisObj, interop, getContext());
            JSDynamicObject instant = toTemporalInstantNode.execute(instantParam);
            JSDynamicObject calendar = toTemporalCalendarWithISODefaultNode.executeDynamicObject(calendarLike);
            return TemporalUtil.builtinTimeZoneGetPlainDateTimeFor(getContext(), timeZone, instant, calendar);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSTemporalTimeZoneGetInstantFor extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneGetInstantFor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject getInstantFor(Object thisObj, Object dateTimeParam, Object optionsParam,
                        @Cached("create(getContext())") ToTemporalDateTimeNode toTemporalDateTime,
                        @Cached TruffleString.EqualNode equalNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            JSTemporalTimeZoneObject timeZone = requireTemporalTimeZone(thisObj, interop, getContext());
            JSTemporalPlainDateTimeObject dateTime = (JSTemporalPlainDateTimeObject) toTemporalDateTime.executeDynamicObject(dateTimeParam, Undefined.instance);
            JSDynamicObject options = getOptionsObject(optionsParam);
            Disambiguation disambiguation = TemporalUtil.toTemporalDisambiguation(options, getOptionNode(), equalNode);
            return TemporalUtil.builtinTimeZoneGetInstantFor(getContext(), timeZone, dateTime, disambiguation);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSTemporalTimeZoneGetPossibleInstantsFor extends JSTemporalBuiltinOperation {

        protected JSTemporalTimeZoneGetPossibleInstantsFor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected JSDynamicObject getPossibleInstantsFor(Object thisObj, Object dateTimeParam,
                        @Cached("create(getContext())") ToTemporalDateTimeNode toTemporalDateTime,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            JSTemporalTimeZoneObject timeZone = requireTemporalTimeZone(thisObj, interop, getContext());
            JSTemporalPlainDateTimeObject dateTime = (JSTemporalPlainDateTimeObject) toTemporalDateTime.executeDynamicObject(dateTimeParam, Undefined.instance);
            JSRealm realm = getRealm();
            if (timeZone.getNanoseconds() != null) {
                BigInteger epochNanoseconds = TemporalUtil.getEpochFromISOParts(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(),
                                dateTime.getSecond(), dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond());
                Object instant = JSTemporalInstant.create(getContext(), realm, new BigInt(epochNanoseconds.subtract(timeZone.getNanoseconds().bigIntegerValue())));
                List<Object> list = new ArrayList<>();
                list.add(instant);
                return JSRuntime.createArrayFromList(getContext(), realm, list);
            }
            List<BigInt> possibleEpochNanoseconds = TemporalUtil.getIANATimeZoneEpochValue(timeZone.getIdentifier(), dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(),
                            dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), dateTime.getMicrosecond(), dateTime.getNanosecond());
            List<Object> possibleInstants = new ArrayList<>();
            for (BigInt epochNanoseconds : possibleEpochNanoseconds) {
                JSDynamicObject instant = JSTemporalInstant.create(getContext(), realm, epochNanoseconds);
                possibleInstants.add(instant);
            }
            return JSRuntime.createArrayFromList(getContext(), realm, possibleInstants);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSTemporalTimeZoneGetNextOrPreviousTransition extends JSTemporalBuiltinOperation {

        private final boolean isNext;

        protected JSTemporalTimeZoneGetNextOrPreviousTransition(JSContext context, JSBuiltin builtin, boolean isNext) {
            super(context, builtin);
            this.isNext = isNext;
        }

        @Specialization
        protected JSDynamicObject getTransition(Object thisObj, Object startingPointParam,
                        @Cached("create(getContext())") ToTemporalInstantNode toTemporalInstantNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            JSTemporalTimeZoneObject timeZone = requireTemporalTimeZone(thisObj, interop, getContext());
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
    }

}
