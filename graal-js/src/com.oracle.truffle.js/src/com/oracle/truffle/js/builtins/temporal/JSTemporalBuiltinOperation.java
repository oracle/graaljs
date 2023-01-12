/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.js.runtime.util.TemporalConstants.LARGEST_UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ROUNDING_MODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SMALLEST_UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.UNIT;

import java.lang.invoke.MethodHandles;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.InlineSupport.StateField;
import com.oracle.truffle.api.dsl.InlineSupport.UnsafeAccessedField;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.TemporalGetOptionNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalCalendarObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainMonthDayObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainYearMonthObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalTimeZoneObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalZonedDateTimeObject;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalTime;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.InlinedProfileBuilder;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Common base class for ALL Temporal Builtin operations.
 */
public abstract class JSTemporalBuiltinOperation extends JSBuiltinNode {

    @CompilationFinal @UnsafeAccessedField private int state;

    private static final StateField STATE_FIELD = StateField.create(MethodHandles.lookup(), "state");

    protected static final InlinedBranchProfile errorBranch;
    protected static final InlinedConditionProfile optionUndefined;

    static {
        var b = new InlinedProfileBuilder(STATE_FIELD);
        errorBranch = b.branchProfile();
        optionUndefined = b.conditionProfile();
    }

    @Child protected IsObjectNode isObjectNode = IsObjectNode.create();

    public JSTemporalBuiltinOperation(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    protected JSTemporalPlainDateObject requireTemporalDate(Object obj) {
        if (!(obj instanceof JSTemporalPlainDateObject)) {
            errorBranch.enter(this);
            throw TemporalErrors.createTypeErrorTemporalDateExpected();
        }
        return (JSTemporalPlainDateObject) obj;
    }

    protected TemporalTime requireTemporalTime(Object obj) {
        if (!(obj instanceof TemporalTime)) {
            errorBranch.enter(this);
            throw TemporalErrors.createTypeErrorTemporalTimeExpected();
        }
        return (TemporalTime) obj;
    }

    protected JSTemporalPlainDateTimeObject requireTemporalDateTime(Object obj) {
        if (!(obj instanceof JSTemporalPlainDateTimeObject)) {
            errorBranch.enter(this);
            throw TemporalErrors.createTypeErrorTemporalDateTimeExpected();
        }
        return (JSTemporalPlainDateTimeObject) obj;
    }

    protected JSTemporalPlainMonthDayObject requireTemporalMonthDay(Object obj) {
        if (!(obj instanceof JSTemporalPlainMonthDayObject)) {
            errorBranch.enter(this);
            throw TemporalErrors.createTypeErrorTemporalPlainMonthDayExpected();
        }
        return (JSTemporalPlainMonthDayObject) obj;
    }

    protected JSTemporalPlainYearMonthObject requireTemporalYearMonth(Object obj) {
        if (!(obj instanceof JSTemporalPlainYearMonthObject)) {
            errorBranch.enter(this);
            throw TemporalErrors.createTypeErrorTemporalPlainYearMonthExpected();
        }
        return (JSTemporalPlainYearMonthObject) obj;
    }

    protected JSTemporalInstantObject requireTemporalInstant(Object obj) {
        if (!(obj instanceof JSTemporalInstantObject)) {
            errorBranch.enter(this);
            throw TemporalErrors.createTypeErrorTemporalInstantExpected();
        }
        return (JSTemporalInstantObject) obj;
    }

    protected JSTemporalZonedDateTimeObject requireTemporalZonedDateTime(Object obj) {
        if (!(obj instanceof JSTemporalZonedDateTimeObject)) {
            errorBranch.enter(this);
            throw TemporalErrors.createTypeErrorTemporalZonedDateTimeExpected();
        }
        return (JSTemporalZonedDateTimeObject) obj;
    }

    protected JSTemporalCalendarObject requireTemporalCalendar(Object obj) {
        if (!(obj instanceof JSTemporalCalendarObject)) {
            errorBranch.enter(this);
            throw TemporalErrors.createTypeErrorTemporalCalendarExpected();
        }
        return (JSTemporalCalendarObject) obj;
    }

    protected JSTemporalDurationObject requireTemporalDuration(Object obj) {
        if (!(obj instanceof JSTemporalDurationObject)) {
            errorBranch.enter(this);
            throw TemporalErrors.createTypeErrorTemporalDurationExpected();
        }
        return (JSTemporalDurationObject) obj;
    }

    protected JSTemporalTimeZoneObject requireTemporalTimeZone(Object obj) {
        if (!(obj instanceof JSTemporalTimeZoneObject)) {
            errorBranch.enter(this);
            throw TemporalErrors.createTypeErrorTemporalTimeZoneExpected();
        }
        return (JSTemporalTimeZoneObject) obj;
    }

    protected JSDynamicObject getOptionsObject(Object options) {
        if (optionUndefined.profile(this, options == Undefined.instance)) {
            return JSOrdinary.createWithNullPrototype(getContext());
        }
        if (isObject(options)) {
            return TemporalUtil.toJSDynamicObject(options, this, errorBranch);
        }
        errorBranch.enter(this);
        throw TemporalErrors.createTypeErrorOptions();
    }

    protected boolean isObject(Object obj) {
        return isObjectNode.executeBoolean(obj);
    }

    protected TemporalUtil.Unit toLargestTemporalUnit(JSDynamicObject normalizedOptions, List<TruffleString> disallowedUnits, TruffleString fallback, TemporalUtil.Unit autoValue,
                    TruffleString.EqualNode equalNode, TemporalGetOptionNode getOptionNode) {
        assert fallback == null || (!disallowedUnits.contains(fallback) && !disallowedUnits.contains(AUTO));
        TruffleString largestUnit = (TruffleString) getOptionNode.execute(normalizedOptions, LARGEST_UNIT, TemporalUtil.OptionType.STRING, TemporalUtil.listAllDateTimeAuto, fallback);
        if (largestUnit != null && largestUnit.equals(AUTO) && autoValue != null) {
            return autoValue;
        }
        if (largestUnit != null && Boundaries.setContains(TemporalUtil.pluralUnits, largestUnit)) {
            largestUnit = Boundaries.mapGet(TemporalUtil.pluralToSingular, largestUnit);
        }
        if (largestUnit != null && Boundaries.listContains(disallowedUnits, largestUnit)) {
            errorBranch.enter(this);
            throw Errors.createRangeError("Largest unit is not allowed.");
        }
        return TemporalUtil.toUnit(largestUnit, equalNode);
    }

    protected TemporalUtil.Unit toSmallestTemporalUnit(JSDynamicObject normalizedOptions, List<TruffleString> disallowedUnits, TruffleString fallback,
                    TruffleString.EqualNode equalNode, TemporalGetOptionNode getOptionNode) {
        TruffleString smallestUnit = (TruffleString) getOptionNode.execute(normalizedOptions, SMALLEST_UNIT, TemporalUtil.OptionType.STRING, TemporalUtil.listAllDateTime, fallback);
        if (smallestUnit != null && Boundaries.setContains(TemporalUtil.pluralUnits, smallestUnit)) {
            smallestUnit = Boundaries.mapGet(TemporalUtil.pluralToSingular, smallestUnit);
        }
        if (smallestUnit != null && Boundaries.listContains(disallowedUnits, smallestUnit)) {
            errorBranch.enter(this);
            throw Errors.createRangeError("Smallest unit not allowed.");
        }
        return TemporalUtil.toUnit(smallestUnit, equalNode);
    }

    protected TemporalUtil.Unit toTemporalDurationTotalUnit(JSDynamicObject normalizedOptions, TruffleString.EqualNode equalNode, TemporalGetOptionNode getOptionNode) {
        TruffleString unit = (TruffleString) getOptionNode.execute(normalizedOptions, UNIT, TemporalUtil.OptionType.STRING, TemporalUtil.listAllDateTime, null);
        if (unit != null && Boundaries.setContains(TemporalUtil.pluralUnits, unit)) {
            unit = Boundaries.mapGet(TemporalUtil.pluralToSingular, unit);
        }
        return TemporalUtil.toUnit(unit, equalNode);
    }

    protected TemporalUtil.RoundingMode toTemporalRoundingMode(JSDynamicObject options, TruffleString fallback,
                    TruffleString.EqualNode equalNode, TemporalGetOptionNode getOptionNode) {
        return TemporalUtil.toRoundingMode((TruffleString) getOptionNode.execute(options, ROUNDING_MODE, TemporalUtil.OptionType.STRING, TemporalUtil.listRoundingMode, fallback), equalNode);
    }
}
