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
package com.oracle.truffle.js.nodes.binary;

import static com.oracle.truffle.api.CompilerDirectives.SLOWPATH_PROBABILITY;
import static com.oracle.truffle.api.CompilerDirectives.injectBranchProbability;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSLazyString;

public abstract class JSConcatStringsNode extends JavaScriptBaseNode {

    protected final int stringLengthLimit;

    protected JSConcatStringsNode(int stringLengthLimit) {
        this.stringLengthLimit = stringLengthLimit;
    }

    public static JSConcatStringsNode create(int stringLengthLimit) {
        return JSConcatStringsNodeGen.create(stringLengthLimit);
    }

    public static JSConcatStringsNode create() {
        return create(JavaScriptLanguage.getCurrentLanguage().getJSContext().getStringLengthLimit());
    }

    public abstract CharSequence executeCharSequence(CharSequence a, CharSequence b);

    @Specialization(guards = "isEmptyString(left)")
    protected static CharSequence doLeftEmpty(@SuppressWarnings("unused") CharSequence left, CharSequence right) {
        return right;
    }

    @Specialization(guards = "isEmptyString(right)")
    protected static CharSequence doRightEmpty(CharSequence left, @SuppressWarnings("unused") CharSequence right) {
        return left;
    }

    @Specialization(guards = {"!isEmptyString(left)", "!isEmptyString(right)"})
    protected final CharSequence doConcat(CharSequence left, CharSequence right,
                    @Cached("createBinaryProfile()") ConditionProfile leftIsString,
                    @Cached("createBinaryProfile()") ConditionProfile leftIsLazyString,
                    @Cached("createBinaryProfile()") ConditionProfile leftIsFlat,
                    @Cached("createBinaryProfile()") ConditionProfile rightIsString,
                    @Cached("createBinaryProfile()") ConditionProfile rightIsLazyString,
                    @Cached("createBinaryProfile()") ConditionProfile rightIsFlat,
                    @Cached("createBinaryProfile()") ConditionProfile stringLength,
                    @Cached("createBinaryProfile()") ConditionProfile shortStringAppend,
                    @Cached BranchProfile errorBranch) {
        if (JSConfig.LazyStrings) {
            int leftLength = JSRuntime.length(left, leftIsString, leftIsLazyString);
            int rightLength = JSRuntime.length(right, rightIsString, rightIsLazyString);
            int resultLength = leftLength + rightLength;
            validateStringLength(resultLength, errorBranch);
            if (stringLength.profile(resultLength >= JSConfig.MinLazyStringLength)) {
                if (shortStringAppend.profile(leftLength == 1 || rightLength == 1)) {
                    JSLazyString result = JSLazyString.concatToLeafMaybe(left, right, resultLength);
                    if (result != null) {
                        return result;
                    }
                }
                return JSLazyString.createChecked(left, right, resultLength);
            }
        }
        String leftString = toString(left, leftIsString, leftIsLazyString, leftIsFlat);
        String rightString = toString(right, rightIsString, rightIsLazyString, rightIsFlat);
        if (!JSConfig.LazyStrings) {
            validateStringLength(leftString.length() + rightString.length(), errorBranch);
        }
        return Boundaries.stringConcat(leftString, rightString);
    }

    private void validateStringLength(int resultLength, BranchProfile errorBranch) {
        if (injectBranchProbability(SLOWPATH_PROBABILITY, resultLength < 0 || resultLength > stringLengthLimit)) {
            errorBranch.enter();
            throw Errors.createRangeErrorInvalidStringLength(this);
        }
    }

    private static String toString(CharSequence cs, ConditionProfile stringProfile, ConditionProfile lazyStringProfile, ConditionProfile flatProfile) {
        if (stringProfile.profile(cs instanceof String)) {
            return ((String) cs);
        } else if (lazyStringProfile.profile(cs instanceof JSLazyString)) {
            return ((JSLazyString) cs).toString(flatProfile);
        } else {
            return Boundaries.charSequenceToString(cs);
        }
    }

    protected static boolean isEmptyString(CharSequence s) {
        return s instanceof String && ((String) s).isEmpty();
    }

}
