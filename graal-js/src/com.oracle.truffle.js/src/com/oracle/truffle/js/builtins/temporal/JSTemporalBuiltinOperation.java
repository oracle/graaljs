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

import static com.oracle.truffle.js.runtime.util.TemporalConstants.AUTO;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.LARGEST_UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.ROUNDING_MODE;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.SMALLEST_UNIT;
import static com.oracle.truffle.js.runtime.util.TemporalConstants.UNIT;

import java.util.List;

import com.oracle.truffle.api.nodes.Node;
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
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

/**
 * Common base class for ALL Temporal Builtin operations.
 */
public abstract class JSTemporalBuiltinOperation extends JSBuiltinNode {

    @Child protected IsObjectNode isObjectNode = IsObjectNode.create();

    public JSTemporalBuiltinOperation(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    protected JSDynamicObject getOptionsObject(Object options, Node node, InlinedBranchProfile errorBranch, InlinedConditionProfile optionUndefined) {
        if (optionUndefined.profile(node, options == Undefined.instance)) {
            return JSOrdinary.createWithNullPrototype(getContext());
        }
        if (isObject(options)) {
            return TemporalUtil.toJSDynamicObject(options, node, errorBranch);
        }
        errorBranch.enter(node);
        throw TemporalErrors.createTypeErrorOptions();
    }

    protected boolean isObject(Object obj) {
        return isObjectNode.executeBoolean(obj);
    }

    protected static TemporalUtil.Unit toLargestTemporalUnit(JSDynamicObject normalizedOptions, List<TruffleString> disallowedUnits, TruffleString fallback, TemporalUtil.Unit autoValue,
                    TruffleString.EqualNode equalNode, TemporalGetOptionNode getOptionNode, Node node, InlinedBranchProfile errorBranch) {
        assert fallback == null || (!disallowedUnits.contains(fallback) && !disallowedUnits.contains(AUTO));
        TruffleString largestUnit = (TruffleString) getOptionNode.execute(normalizedOptions, LARGEST_UNIT, TemporalUtil.OptionType.STRING, TemporalUtil.listAllDateTimeAuto, fallback);
        if (largestUnit != null && largestUnit.equals(AUTO) && autoValue != null) {
            return autoValue;
        }
        if (largestUnit != null) {
            TruffleString singular = Boundaries.mapGet(TemporalUtil.pluralToSingular, largestUnit);
            if (singular != null) {
                largestUnit = singular;
            }
        }
        if (largestUnit != null && Boundaries.listContains(disallowedUnits, largestUnit)) {
            errorBranch.enter(node);
            throw Errors.createRangeError("Largest unit is not allowed.");
        }
        return TemporalUtil.toUnit(largestUnit, equalNode);
    }

    protected static TemporalUtil.Unit toSmallestTemporalUnit(JSDynamicObject normalizedOptions, List<TruffleString> disallowedUnits, TruffleString fallback,
                    TruffleString.EqualNode equalNode, TemporalGetOptionNode getOptionNode, Node node, InlinedBranchProfile errorBranch) {
        TruffleString smallestUnit = (TruffleString) getOptionNode.execute(normalizedOptions, SMALLEST_UNIT, TemporalUtil.OptionType.STRING, TemporalUtil.listAllDateTime, fallback);
        if (smallestUnit != null) {
            TruffleString singular = Boundaries.mapGet(TemporalUtil.pluralToSingular, smallestUnit);
            if (singular != null) {
                smallestUnit = singular;
            }
        }
        if (smallestUnit != null && Boundaries.listContains(disallowedUnits, smallestUnit)) {
            errorBranch.enter(node);
            throw Errors.createRangeError("Smallest unit not allowed.");
        }
        return TemporalUtil.toUnit(smallestUnit, equalNode);
    }

    protected static TemporalUtil.Unit toTemporalDurationTotalUnit(JSDynamicObject normalizedOptions, TruffleString.EqualNode equalNode, TemporalGetOptionNode getOptionNode) {
        TruffleString unit = (TruffleString) getOptionNode.execute(normalizedOptions, UNIT, TemporalUtil.OptionType.STRING, TemporalUtil.listAllDateTime, null);
        if (unit != null) {
            TruffleString singular = Boundaries.mapGet(TemporalUtil.pluralToSingular, unit);
            if (singular != null) {
                unit = singular;
            }
        }
        return TemporalUtil.toUnit(unit, equalNode);
    }

    protected static TemporalUtil.RoundingMode toTemporalRoundingMode(JSDynamicObject options, TruffleString fallback,
                    TruffleString.EqualNode equalNode, TemporalGetOptionNode getOptionNode) {
        return TemporalUtil.toRoundingMode((TruffleString) getOptionNode.execute(options, ROUNDING_MODE, TemporalUtil.OptionType.STRING, TemporalUtil.listRoundingMode, fallback), equalNode);
    }
}
