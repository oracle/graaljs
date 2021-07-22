/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * Converts value to array index according to ES5 15.4 Array Objects.
 */
@ImportStatic({JSConfig.class, JSRuntime.class})
public abstract class ToArrayIndexNode extends JavaScriptBaseNode {
    protected final boolean convertToPropertyKey;
    protected final boolean convertStringToIndex;

    public abstract Object execute(Object value);

    public abstract long executeLong(Object operand) throws UnexpectedResultException;

    @SuppressWarnings("static-method")
    public final boolean isResultArrayIndex(Object result) {
        return result instanceof Long;
    }

    protected ToArrayIndexNode(boolean convertToPropertyKey, boolean convertStringToIndex) {
        this.convertToPropertyKey = convertToPropertyKey;
        this.convertStringToIndex = convertStringToIndex;
    }

    public static ToArrayIndexNode create() {
        return ToArrayIndexNodeGen.create(true, true);
    }

    public static ToArrayIndexNode createNoToPropertyKey() {
        return ToArrayIndexNodeGen.create(false, true);
    }

    public static ToArrayIndexNode createNoStringToIndex() {
        return ToArrayIndexNodeGen.create(true, false);
    }

    @Specialization(guards = "isIntArrayIndex(value)")
    protected static long doInteger(int value) {
        return value;
    }

    @Specialization(guards = "isLongArrayIndex(value)")
    protected static long doLong(long value) {
        return JSRuntime.castArrayIndex(value);
    }

    protected static boolean doubleIsIntIndex(double d) {
        return JSRuntime.doubleIsRepresentableAsInt(d) && d >= 0;
    }

    @Specialization(guards = "doubleIsIntIndex(value)")
    protected static long doDoubleAsIntIndex(double value) {
        return (long) value;
    }

    protected static boolean doubleIsUintIndex(double d) {
        return JSRuntime.doubleIsRepresentableAsUnsignedInt(d, true) && d >= 0 && d < 0xffff_ffffL;
    }

    @Specialization(guards = "doubleIsUintIndex(value)", replaces = "doDoubleAsIntIndex")
    protected static long doDoubleAsUintIndex(double value) {
        return JSRuntime.castArrayIndex(value);
    }

    @Specialization
    protected static Symbol doSymbol(Symbol value) {
        return value;
    }

    @Specialization(guards = "isBigIntArrayIndex(value)")
    protected static long doBigInt(BigInt value) {
        return value.longValue();
    }

    @Specialization(guards = {"convertStringToIndex", "arrayIndexLengthInRange(index)"})
    protected static Object convertFromString(String index,
                    @Cached ConditionProfile startsWithDigitBranch,
                    @Cached BranchProfile isArrayIndexBranch) {
        if (startsWithDigitBranch.profile(JSRuntime.isAsciiDigit(index.charAt(0)))) {
            long longValue = JSRuntime.parseArrayIndexRaw(index);
            if (JSRuntime.isArrayIndex(longValue)) {
                isArrayIndexBranch.enter();
                return JSRuntime.castArrayIndex(longValue);
            }
        }
        return index;
    }

    @Specialization(guards = {"!convertStringToIndex || !arrayIndexLengthInRange(index)"})
    protected static Object convertFromStringNotInRange(String index) {
        return index;
    }

    protected static boolean notArrayIndex(Object o) {
        return !((o instanceof Integer && JSGuards.isIntArrayIndex((int) o)) ||
                        (o instanceof Double && doubleIsUintIndex((double) o)) ||
                        (o instanceof Long && JSGuards.isLongArrayIndex((long) o)) ||
                        (o instanceof BigInt && JSGuards.isBigIntArrayIndex((BigInt) o)) ||
                        o instanceof String ||
                        o instanceof Symbol);
    }

    @Specialization(guards = {"notArrayIndex(value)", "index >= 0"}, limit = "InteropLibraryLimit")
    protected static long doInteropArrayIndex(@SuppressWarnings("unused") Object value,
                    @CachedLibrary("value") @SuppressWarnings("unused") InteropLibrary interop,
                    @Bind("toArrayIndex(value, interop)") long index) {
        return index;
    }

    @Specialization(guards = {"notArrayIndex(value)", "toArrayIndex(value, interop) < 0"}, limit = "InteropLibraryLimit")
    protected final Object doNonArrayIndex(Object value,
                    @CachedLibrary("value") @SuppressWarnings("unused") InteropLibrary interop,
                    @Cached JSToPropertyKeyNode toPropertyKey,
                    @Cached ConditionProfile arrayIndexLengthInRangeBranch,
                    @Cached ConditionProfile startsWithDigitBranch,
                    @Cached BranchProfile isArrayIndexBranch) {
        CompilerAsserts.partialEvaluationConstant(convertToPropertyKey);
        if (convertToPropertyKey) {
            Object propertyKey = toPropertyKey.execute(value);
            if (convertStringToIndex && propertyKey instanceof String) {
                String stringKey = (String) propertyKey;
                if (arrayIndexLengthInRangeBranch.profile(JSRuntime.arrayIndexLengthInRange(stringKey))) {
                    return convertFromString(stringKey, startsWithDigitBranch, isArrayIndexBranch);
                }
            }
            return propertyKey;
        } else {
            return value;
        }
    }

    static long toArrayIndex(Object value, InteropLibrary interop) {
        if (interop.fitsInLong(value)) {
            try {
                long index = interop.asLong(value);
                if (JSRuntime.isArrayIndex(index)) {
                    return JSRuntime.castArrayIndex(index);
                }
            } catch (UnsupportedMessageException iex) {
            }
        }
        return JSRuntime.INVALID_ARRAY_INDEX;
    }
}
