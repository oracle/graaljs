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

import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayCalendarGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayEqualsNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayGetterNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayToPlainDateNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayToStringNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainMonthDayPrototypeBuiltinsFactory.JSTemporalPlainMonthDayWithNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.IsPartialTemporalObjectNode;
import com.oracle.truffle.js.nodes.temporal.TemporalCalendarDateFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.nodes.temporal.TemporalMonthDayFromFieldsNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalCalendarIdentifierNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalMonthDayNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDay;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDayObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.ShowCalendar;

public class TemporalPlainMonthDayPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainMonthDayPrototypeBuiltins.TemporalPlainMonthDayPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainMonthDayPrototypeBuiltins();

    protected TemporalPlainMonthDayPrototypeBuiltins() {
        super(JSTemporalPlainMonthDay.PROTOTYPE_NAME, TemporalPlainMonthDayPrototype.class);
    }

    public enum TemporalPlainMonthDayPrototype implements BuiltinEnum<TemporalPlainMonthDayPrototype> {
        // getters
        calendarId(0),
        monthCode(0),
        day(0),

        // methods
        with(1),
        equals(1),
        toPlainDate(1),
        toString(0),
        toLocaleString(0),
        toJSON(0),
        valueOf(0);

        private final int length;

        TemporalPlainMonthDayPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return EnumSet.of(calendarId, monthCode, day).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainMonthDayPrototype builtinEnum) {
        switch (builtinEnum) {
            case calendarId:
                return JSTemporalPlainMonthDayCalendarGetterNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case monthCode:
            case day:
                return JSTemporalPlainMonthDayGetterNodeGen.create(context, builtin, builtinEnum, args().withThis().createArgumentNodes(context));

            case with:
                return JSTemporalPlainMonthDayWithNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case equals:
                return JSTemporalPlainMonthDayEqualsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toPlainDate:
                return JSTemporalPlainMonthDayToPlainDateNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toString:
                return JSTemporalPlainMonthDayToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
            case toJSON:
                return JSTemporalPlainMonthDayToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return UnsupportedValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainMonthDayCalendarGetterNode extends JSBuiltinNode {

        protected JSTemporalPlainMonthDayCalendarGetterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString calendarId(JSTemporalPlainMonthDayObject monthDay,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier) {
            return toCalendarIdentifier.executeString(monthDay.getCalendar());
        }

        @Specialization(guards = "!isJSTemporalMonthDay(monthDay)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object monthDay) {
            throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
        }
    }

    public abstract static class JSTemporalPlainMonthDayGetterNode extends JSBuiltinNode {

        protected final TemporalPlainMonthDayPrototype property;

        protected JSTemporalPlainMonthDayGetterNode(JSContext context, JSBuiltin builtin, TemporalPlainMonthDayPrototype property) {
            super(context, builtin);
            this.property = property;
        }

        @Specialization
        protected Object monthDayGetter(JSTemporalPlainMonthDayObject plainMD) {
            switch (property) {
                case day:
                    return plainMD.getDay();
                case monthCode:
                    return TemporalUtil.buildISOMonthCode(plainMD.getMonth());
            }
            throw Errors.shouldNotReachHere();
        }

        @Specialization(guards = "!isJSTemporalMonthDay(thisObj)")
        protected static Object invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
        }
    }

    // 4.3.20
    public abstract static class JSTemporalPlainMonthDayToString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainMonthDayToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toString(JSTemporalPlainMonthDayObject md, Object optParam,
                        @Cached TruffleString.EqualNode equalNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            JSDynamicObject options = getOptionsObject(optParam, this, errorBranch, optionUndefined);
            ShowCalendar showCalendar = TemporalUtil.toShowCalendarOption(options, getOptionNode, equalNode);
            return JSTemporalPlainMonthDay.temporalMonthDayToString(md, showCalendar);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalMonthDay(thisObj)")
        protected static TruffleString invalidReceiver(Object thisObj, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
        }
    }

    public abstract static class JSTemporalPlainMonthDayToLocaleString extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainMonthDayToLocaleString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public TruffleString toLocaleString(JSTemporalPlainMonthDayObject thisMonthDay) {
            return JSTemporalPlainMonthDay.temporalMonthDayToString(thisMonthDay, ShowCalendar.AUTO);
        }

        @Specialization(guards = "!isJSTemporalMonthDay(thisObj)")
        protected static TruffleString invalidReceiver(@SuppressWarnings("unused") Object thisObj) {
            throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
        }
    }

    public abstract static class JSTemporalPlainMonthDayToPlainDateNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainMonthDayToPlainDateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalPlainDateObject toPlainDate(JSTemporalPlainMonthDayObject monthDay, Object item,
                        @Cached TemporalCalendarDateFromFieldsNode dateFromFieldsNode,
                        @Cached InlinedBranchProfile errorBranch) {
            if (!isObject(item)) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
            }
            TruffleString calendar = monthDay.getCalendar();
            List<TruffleString> receiverFieldNames = TemporalUtil.listDMC;
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), monthDay, receiverFieldNames, TemporalUtil.listEmpty);
            List<TruffleString> inputFieldNames = TemporalUtil.listY;
            JSDynamicObject inputFields = TemporalUtil.prepareTemporalFields(getContext(), TemporalUtil.toJSDynamicObject(item, this, errorBranch), inputFieldNames, TemporalUtil.listEmpty);
            JSDynamicObject mergedFields = TemporalUtil.calendarMergeFields(getContext(), calendar, fields, inputFields);
            List<TruffleString> mergedFieldNames = TemporalUtil.listJoinRemoveDuplicates(receiverFieldNames, inputFieldNames);
            mergedFields = TemporalUtil.prepareTemporalFields(getContext(), mergedFields, mergedFieldNames, TemporalUtil.listEmpty);
            return dateFromFieldsNode.execute(calendar, mergedFields, TemporalUtil.Overflow.CONSTRAIN);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalMonthDay(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object item) {
            throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
        }
    }

    public abstract static class JSTemporalPlainMonthDayWithNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainMonthDayWithNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalPlainMonthDayObject with(JSTemporalPlainMonthDayObject temporalDate, Object temporalMonthDayLike, Object options,
                        @Cached IsPartialTemporalObjectNode isPartialTemporalObjectNode,
                        @Cached TemporalMonthDayFromFieldsNode monthDayFromFieldsNode,
                        @Cached TemporalGetOptionNode getOptionNode,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedConditionProfile optionUndefined) {
            if (!isPartialTemporalObjectNode.execute(temporalMonthDayLike)) {
                errorBranch.enter(this);
                throw TemporalErrors.createTypeErrorPartialTemporalObjectExpected();
            }

            TruffleString calendar = temporalDate.getCalendar();

            List<TruffleString> fieldNames = TemporalUtil.listDMMCY;
            JSDynamicObject fields = TemporalUtil.prepareTemporalFields(getContext(), temporalDate, fieldNames, TemporalUtil.listEmpty);
            JSDynamicObject partialDate = TemporalUtil.prepareTemporalFields(getContext(), temporalMonthDayLike, fieldNames, null);
            fields = TemporalUtil.calendarMergeFields(getContext(), calendar, fields, partialDate);
            fields = TemporalUtil.prepareTemporalFields(getContext(), fields, fieldNames, TemporalUtil.listEmpty);
            Object resolvedOptions = getOptionsObject(options, this, errorBranch, optionUndefined);
            TemporalUtil.Overflow overflow = TemporalUtil.getTemporalOverflowOption(resolvedOptions, getOptionNode);
            return monthDayFromFieldsNode.execute(calendar, fields, overflow);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalMonthDay(thisObj)")
        protected static Object invalidReceiver(Object thisObj, Object temporalMonthDayLike, Object optParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
        }
    }

    public abstract static class JSTemporalPlainMonthDayEqualsNode extends JSTemporalBuiltinOperation {

        protected JSTemporalPlainMonthDayEqualsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean equals(JSTemporalPlainMonthDayObject md, Object otherParam,
                        @Cached ToTemporalCalendarIdentifierNode toCalendarIdentifier,
                        @Cached ToTemporalMonthDayNode toTemporalMonthDayNode) {
            JSTemporalPlainMonthDayObject other = toTemporalMonthDayNode.execute(otherParam, Undefined.instance);
            if (md.getMonth() != other.getMonth()) {
                return false;
            }
            if (md.getDay() != other.getDay()) {
                return false;
            }
            if (md.getYear() != other.getYear()) {
                return false;
            }
            return TemporalUtil.calendarEquals(md.getCalendar(), other.getCalendar(), toCalendarIdentifier);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSTemporalMonthDay(thisObj)")
        protected static boolean invalidReceiver(Object thisObj, Object otherParam) {
            throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
        }
    }
}
