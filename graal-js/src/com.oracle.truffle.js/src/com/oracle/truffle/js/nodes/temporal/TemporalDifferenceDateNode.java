/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateObject;
import com.oracle.truffle.js.runtime.util.TemporalConstants;
import com.oracle.truffle.js.runtime.util.TemporalUtil;
import com.oracle.truffle.js.runtime.util.TemporalUtil.Unit;

/**
 * Implementation of the Temporal DifferenceDate operation.
 */
@ImportStatic(TemporalConstants.class)
public abstract class TemporalDifferenceDateNode extends JavaScriptBaseNode {

    protected TemporalDifferenceDateNode() {
    }

    public abstract JSTemporalDurationObject execute(
                    TruffleString calendar, JSTemporalPlainDateObject one, JSTemporalPlainDateObject two,
                    Unit largestUnit, Object untilOptions);

    @Specialization
    final JSTemporalDurationObject differenceDate(TruffleString calendar, JSTemporalPlainDateObject one, JSTemporalPlainDateObject two,
                    Unit largestUnit, Object untilOptions,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached("createCall()") JSFunctionCallNode callDateUntilNode) {
        JSContext ctx = getJSContext();
        JSRealm realm = getRealm();

        if (one.getYear() == two.getYear() && one.getMonth() == two.getMonth() && one.getDay() == two.getDay()) {
            return JSTemporalDuration.createTemporalDuration(ctx, realm, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, this, errorBranch);
        } else if (largestUnit == Unit.DAY) {
            double days = TemporalUtil.daysUntil(one, two);
            return JSTemporalDuration.createTemporalDuration(ctx, realm, 0, 0, 0, days, 0, 0, 0, 0, 0, 0, this, errorBranch);
        } else {
            return TemporalUtil.calendarDateUntil(ctx, realm, calendar, one, two, largestUnit, this, errorBranch);
        }
    }
}
