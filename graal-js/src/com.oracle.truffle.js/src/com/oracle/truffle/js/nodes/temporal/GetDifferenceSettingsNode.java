/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.temporal;

import java.util.EnumSet;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.temporal.JSTemporalBuiltinOperation;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.RoundingMode;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;
import com.oracle.truffle.js.runtime.util.TemporalUtil.UnitGroup;

/**
 * Implementation of GetDifferenceSettings() operation.
 */
public abstract class GetDifferenceSettingsNode extends JavaScriptBaseNode {

    protected GetDifferenceSettingsNode() {
    }

    /**
     * Record returned by the GetDifferenceSettings abstract operation.
     */
    public record GetDifferenceSettingsResult(
                    TemporalUtil.Unit smallestUnit,
                    TemporalUtil.Unit largestUnit,
                    TemporalUtil.RoundingMode roundingMode,
                    int roundingIncrement) {
    }

    public abstract GetDifferenceSettingsResult execute(int operation, JSDynamicObject options, UnitGroup unitGroup, EnumSet<Unit> disallowedUnits,
                    TemporalUtil.Unit fallbackSmallestUnit, TemporalUtil.Unit smallestLargestDefaultUnit);

    @Specialization
    final GetDifferenceSettingsResult getDifferenceSettings(int operation, JSDynamicObject resolvedOptions, UnitGroup unitGroup, EnumSet<Unit> disallowedUnits,
                    Unit fallbackSmallestUnit, Unit smallestLargestDefaultUnit,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached TemporalGetOptionNode getOptionNode,
                    @Cached TruffleString.EqualNode equalNode,
                    @Cached GetTemporalUnitNode getLargestUnit,
                    @Cached GetRoundingIncrementOptionNode getRoundingIncrementOption,
                    @Cached GetTemporalUnitNode getSmallestUnit) {
        Unit largestUnit = getLargestUnit.execute(resolvedOptions, TemporalConstants.LARGEST_UNIT, Unit.EMPTY);
        int roundingIncrement = getRoundingIncrementOption.execute(resolvedOptions);
        RoundingMode roundingMode = JSTemporalBuiltinOperation.toTemporalRoundingMode(resolvedOptions, TemporalConstants.TRUNC, equalNode, getOptionNode);
        Unit smallestUnit = getSmallestUnit.execute(resolvedOptions, TemporalConstants.SMALLEST_UNIT, Unit.EMPTY);
        TemporalUtil.validateTemporalUnitValue(largestUnit, unitGroup, Unit.AUTO, this, errorBranch);
        if (largestUnit == Unit.EMPTY) {
            largestUnit = Unit.AUTO;
        }
        if (disallowedUnits != null && disallowedUnits.contains(largestUnit)) {
            errorBranch.enter(this);
            throw TemporalErrors.createRangeErrorDisallowedUnit(this, largestUnit);
        }
        if (operation == TemporalUtil.SINCE) {
            roundingMode = TemporalUtil.negateTemporalRoundingMode(roundingMode);
        }
        TemporalUtil.validateTemporalUnitValue(smallestUnit, unitGroup, null, this, errorBranch);
        if (smallestUnit == Unit.EMPTY) {
            smallestUnit = fallbackSmallestUnit;
        }
        if (disallowedUnits != null && disallowedUnits.contains(smallestUnit)) {
            errorBranch.enter(this);
            throw TemporalErrors.createRangeErrorDisallowedUnit(this, smallestUnit);
        }
        Unit defaultLargestUnit = TemporalUtil.largerOfTwoTemporalUnits(smallestLargestDefaultUnit, smallestUnit);
        if (largestUnit == Unit.AUTO) {
            largestUnit = defaultLargestUnit;
        }
        TemporalUtil.validateTemporalUnitRange(largestUnit, smallestUnit);
        Integer maximum = TemporalUtil.maximumTemporalDurationRoundingIncrement(smallestUnit);
        if (maximum != null) {
            TemporalUtil.validateTemporalRoundingIncrement(roundingIncrement, maximum, false, this, errorBranch);
        }
        return new GetDifferenceSettingsResult(smallestUnit, largestUnit, roundingMode, roundingIncrement);
    }
}
