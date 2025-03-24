/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.intl;

import java.util.List;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.Pair;

public abstract class GetDurationUnitOptionsNode extends JavaScriptBaseNode {
    public static final List<String> LONG_SHORT_NARROW_STYLES = List.of(IntlUtil.LONG, IntlUtil.SHORT, IntlUtil.NARROW);
    public static final List<String> LONG_SHORT_NARROW_NUMERIC_STYLES = List.of(IntlUtil.LONG, IntlUtil.SHORT, IntlUtil.NARROW, IntlUtil.NUMERIC);
    public static final List<String> LONG_SHORT_NARROW_NUMERIC_2DIGIT_STYLES = List.of(IntlUtil.LONG, IntlUtil.SHORT, IntlUtil.NARROW, IntlUtil.NUMERIC, IntlUtil._2_DIGIT);

    private final Unit unit;
    private final String digitalBase;

    @Child GetStringOptionNode getStyleOption;
    @Child GetStringOptionNode getDisplayOption;

    private final BranchProfile errorBranch = BranchProfile.create();

    public enum Unit {
        YEARS(IntlUtil.KEY_YEARS, IntlUtil.KEY_YEARS_DISPLAY),
        MONTHS(IntlUtil.KEY_MONTHS, IntlUtil.KEY_MONTHS_DISPLAY),
        WEEKS(IntlUtil.KEY_WEEKS, IntlUtil.KEY_WEEKS_DISPLAY),
        DAYS(IntlUtil.KEY_DAYS, IntlUtil.KEY_DAYS_DISPLAY),
        HOURS(IntlUtil.KEY_HOURS, IntlUtil.KEY_HOURS_DISPLAY),
        MINUTES(IntlUtil.KEY_MINUTES, IntlUtil.KEY_MINUTES_DISPLAY),
        SECONDS(IntlUtil.KEY_SECONDS, IntlUtil.KEY_SECONDS_DISPLAY),
        MILLISECONDS(IntlUtil.KEY_MILLISECONDS, IntlUtil.KEY_MILLISECONDS_DISPLAY),
        MICROSECONDS(IntlUtil.KEY_MICROSECONDS, IntlUtil.KEY_MICROSECONDS_DISPLAY),
        NANOSECONDS(IntlUtil.KEY_NANOSECONDS, IntlUtil.KEY_NANOSECONDS_DISPLAY);

        final TruffleString styleKey;
        final TruffleString displayKey;

        Unit(TruffleString styleKey, TruffleString displayKey) {
            this.styleKey = styleKey;
            this.displayKey = displayKey;
        }
    }

    protected GetDurationUnitOptionsNode(JSContext context, Unit unit, List<String> styleList, String digitalBase) {
        this.unit = unit;
        this.digitalBase = digitalBase;
        this.getStyleOption = GetStringOptionNode.create(context, unit.styleKey, styleList, null);
        this.getDisplayOption = GetStringOptionNode.create(context, unit.displayKey, GetStringOptionNode.AUTO_ALWAYS_OPTION_VALUES, null);
    }

    public static GetDurationUnitOptionsNode create(JSContext context, Unit unit, List<String> styleList, String digitalBase) {
        return GetDurationUnitOptionsNodeGen.create(context, unit, styleList, digitalBase);
    }

    public abstract Pair<String, String> executeOptions(Object options, String baseStyle, String prevStyle, boolean twoDigitHours);

    @Specialization
    public Pair<String, String> getOptions(Object options, String baseStyle, String prevStyle, boolean twoDigitHours) {
        String style = getStyleOption.executeValue(options);
        String displayDefault = IntlUtil.ALWAYS;
        if (style == null) {
            if (IntlUtil.DIGITAL.equals(baseStyle)) {
                if (unit != Unit.HOURS && unit != Unit.MINUTES && unit != Unit.SECONDS) {
                    displayDefault = IntlUtil.AUTO;
                }
                style = digitalBase;
            } else {
                if (IntlUtil.FRACTIONAL.equals(prevStyle) || IntlUtil.NUMERIC.equals(prevStyle) || IntlUtil._2_DIGIT.equals(prevStyle)) {
                    if (unit != Unit.MINUTES && unit != Unit.SECONDS) {
                        displayDefault = IntlUtil.AUTO;
                    }
                    style = IntlUtil.NUMERIC;
                } else {
                    displayDefault = IntlUtil.AUTO;
                    style = baseStyle;
                }
            }
        }
        if (IntlUtil.NUMERIC.equals(style)) {
            if (unit == Unit.MILLISECONDS || unit == Unit.MICROSECONDS || unit == Unit.NANOSECONDS) {
                style = IntlUtil.FRACTIONAL;
                displayDefault = IntlUtil.AUTO;
            }
        }
        String display = getDisplayOption.executeValue(options);
        if (display == null) {
            display = displayDefault;
        }
        if (IntlUtil.ALWAYS.equals(display) && IntlUtil.FRACTIONAL.equals(style)) {
            errorBranch.enter();
            throw Errors.createRangeErrorInvalidOptions(this);
        }
        if (IntlUtil.FRACTIONAL.equals(prevStyle)) {
            if (!IntlUtil.FRACTIONAL.equals(style)) {
                errorBranch.enter();
                throw Errors.createRangeErrorInvalidOptions(this);
            }
        } else if (IntlUtil.NUMERIC.equals(prevStyle) || IntlUtil._2_DIGIT.equals(prevStyle)) {
            if (!IntlUtil.FRACTIONAL.equals(style) && !IntlUtil.NUMERIC.equals(style) && !IntlUtil._2_DIGIT.equals(style)) {
                errorBranch.enter();
                throw Errors.createRangeErrorInvalidOptions(this);
            }
            if (unit == Unit.MINUTES || unit == Unit.SECONDS) {
                style = IntlUtil._2_DIGIT;
            }
        }
        if (unit == Unit.HOURS && twoDigitHours) {
            style = IntlUtil._2_DIGIT;
        }
        return new Pair<>(style, display);
    }

}
