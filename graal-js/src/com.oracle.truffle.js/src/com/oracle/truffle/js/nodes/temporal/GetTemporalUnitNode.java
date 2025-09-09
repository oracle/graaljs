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

import static java.util.Map.entry;

import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

/**
 * Implementation of GetTemporalUnit() operation.
 */
public abstract class GetTemporalUnitNode extends JavaScriptBaseNode {
    private static final List<?> ALLOWED_STRINGS = List.of(
                    TemporalConstants.AUTO,
                    TemporalConstants.YEAR,
                    TemporalConstants.MONTH,
                    TemporalConstants.WEEK,
                    TemporalConstants.DAY,
                    TemporalConstants.HOUR,
                    TemporalConstants.MINUTE,
                    TemporalConstants.SECOND,
                    TemporalConstants.MILLISECOND,
                    TemporalConstants.MICROSECOND,
                    TemporalConstants.NANOSECOND,
                    TemporalConstants.YEARS,
                    TemporalConstants.MONTHS,
                    TemporalConstants.WEEKS,
                    TemporalConstants.DAYS,
                    TemporalConstants.HOURS,
                    TemporalConstants.MINUTES,
                    TemporalConstants.SECONDS,
                    TemporalConstants.MILLISECONDS,
                    TemporalConstants.MICROSECONDS,
                    TemporalConstants.NANOSECONDS);
    private static final Map<TruffleString, Unit> NAME_TO_UNIT = Map.ofEntries(
                    entry(TemporalConstants.AUTO, Unit.AUTO),
                    entry(TemporalConstants.YEAR, Unit.YEAR),
                    entry(TemporalConstants.MONTH, Unit.MONTH),
                    entry(TemporalConstants.WEEK, Unit.WEEK),
                    entry(TemporalConstants.DAY, Unit.DAY),
                    entry(TemporalConstants.HOUR, Unit.HOUR),
                    entry(TemporalConstants.MINUTE, Unit.MINUTE),
                    entry(TemporalConstants.SECOND, Unit.SECOND),
                    entry(TemporalConstants.MILLISECOND, Unit.MILLISECOND),
                    entry(TemporalConstants.MICROSECOND, Unit.MICROSECOND),
                    entry(TemporalConstants.NANOSECOND, Unit.NANOSECOND),
                    entry(TemporalConstants.YEARS, Unit.YEAR),
                    entry(TemporalConstants.MONTHS, Unit.MONTH),
                    entry(TemporalConstants.WEEKS, Unit.WEEK),
                    entry(TemporalConstants.DAYS, Unit.DAY),
                    entry(TemporalConstants.HOURS, Unit.HOUR),
                    entry(TemporalConstants.MINUTES, Unit.MINUTE),
                    entry(TemporalConstants.SECONDS, Unit.SECOND),
                    entry(TemporalConstants.MILLISECONDS, Unit.MILLISECOND),
                    entry(TemporalConstants.MICROSECONDS, Unit.MICROSECOND),
                    entry(TemporalConstants.NANOSECONDS, Unit.NANOSECOND));

    protected GetTemporalUnitNode() {
    }

    public abstract Unit execute(JSDynamicObject options, TruffleString key, TemporalUtil.Unit defaultValue);

    @Specialization
    final TemporalUtil.Unit getUnit(JSDynamicObject options, TruffleString key, Unit defaultValue,
                    @Cached TemporalGetOptionNode getOptionNode) {
        Object value = getOptionNode.execute(options, key, TemporalUtil.OptionType.STRING, ALLOWED_STRINGS, defaultValue);
        if (value == Unit.REQUIRED) { // part of GetOption()
            throw Errors.createRangeErrorFormat("Property %s is required", this, key);
        }
        if (value == Unit.EMPTY) {
            return Unit.EMPTY;
        }
        return Boundaries.mapGet(NAME_TO_UNIT, (TruffleString) value);
    }

}
