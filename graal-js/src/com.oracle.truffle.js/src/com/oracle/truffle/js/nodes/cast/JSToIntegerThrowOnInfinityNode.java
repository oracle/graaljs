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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * This implements ToIntegerThrowOnInfinity (via Temporal proposal).
 */
@ImportStatic(JSGuards.class)
public abstract class JSToIntegerThrowOnInfinityNode extends JavaScriptBaseNode {

    private final BranchProfile errorBranch = BranchProfile.create();
    private final BranchProfile isIntProfile = BranchProfile.create();
    private final BranchProfile isLongProfile = BranchProfile.create();
    private final BranchProfile isDoubleProfile = BranchProfile.create();

    public abstract Object execute(Object value);

    /**
     * The node by definition should return a double value (that has integer properties), but in
     * some cases we know that the result needs to fit into an integer. Methods like
     * https://tc39.es/proposal-temporal/#sec-temporal-isodatetimewithinlimits or
     * https://tc39.es/proposal-temporal/#sec-temporal-isoyearmonthwithinlimits ensure that days and
     * months are in valid ranges, and years up to about 273,000 years.
     */
    public final int executeIntOrThrow(Object value) {
        Number n = (Number) execute(value);

        if (n instanceof Integer) {
            isIntProfile.enter();
            return ((Integer) n).intValue();
        } else if (n instanceof Long) {
            isLongProfile.enter();
            long l = ((Long) n).longValue();
            if (l < Integer.MIN_VALUE || Integer.MAX_VALUE < l) {
                errorBranch.enter();
                throw Errors.createRangeError("value out of range");
            }
            return (int) l;
        } else {
            isDoubleProfile.enter();
            double d = JSRuntime.doubleValue(n);
            if (d < Integer.MIN_VALUE || Integer.MAX_VALUE < d) {
                errorBranch.enter();
                throw Errors.createRangeError("value out of range");
            }
            return (int) d;
        }
    }

    public final double executeDouble(Object value) {
        return JSRuntime.doubleValue((Number) execute(value));
    }

    @NeverDefault
    public static JSToIntegerThrowOnInfinityNode create() {
        return JSToIntegerThrowOnInfinityNodeGen.create();
    }

    @Specialization
    protected static int doInteger(int value) {
        return value;
    }

    @Specialization
    protected static long doLong(long value) {
        return value;
    }

    @Specialization
    protected static int doBoolean(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    @Specialization
    protected static SafeInteger doSafeInteger(SafeInteger value) {
        return value;
    }

    @Specialization
    protected double doDouble(double value) {
        if (Double.isInfinite(value)) {
            errorBranch.enter();
            throw Errors.createRangeError("infinity cannot be converted to integer");
        }
        if (Double.isNaN(value)) {
            errorBranch.enter();
            throw Errors.createRangeError("NaN cannot be converted to integer");
        }
        return JSRuntime.truncateDouble(value);
    }

    @Specialization(guards = "isJSNull(value)")
    protected static int doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization(guards = "isUndefined(value)")
    protected static int doUndefined(@SuppressWarnings("unused") Object value) {
        throw Errors.createRangeError("undefined cannot be converted to integer");
    }

    @Specialization
    protected final Number doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value", this);
    }

    @Specialization
    protected final Number doBigInt(@SuppressWarnings("unused") BigInt value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a BigInt value", this);
    }

    @Specialization
    protected Number doString(TruffleString value,
                    @Shared @Cached JSToIntegerThrowOnInfinityNode toIntOrInf,
                    @Cached JSStringToNumberNode stringToNumberNode) {
        return (Number) toIntOrInf.execute(stringToNumberNode.execute(value));
    }

    @Specialization(guards = "isJSObject(value) || isForeignObject(value)")
    protected Number doJSOrForeignObject(Object value,
                    @Shared @Cached JSToIntegerThrowOnInfinityNode toIntOrInf,
                    @Cached JSToNumberNode toNumberNode) {
        return (Number) toIntOrInf.execute(toNumberNode.executeNumber(value));
    }
}
