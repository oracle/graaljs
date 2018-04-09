/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.objects.JSLazyString;

@SuppressWarnings("unused")
@ImportStatic(JSRuntime.class)
public abstract class JSConcatStringsNode extends JSBinaryNode {

    protected final ConditionProfile leftProfile1 = ConditionProfile.createBinaryProfile();
    protected final ConditionProfile leftProfile2 = ConditionProfile.createBinaryProfile();
    protected final ConditionProfile rightProfile1 = ConditionProfile.createBinaryProfile();
    protected final ConditionProfile rightProfile2 = ConditionProfile.createBinaryProfile();

    protected JSConcatStringsNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JSConcatStringsNode create() {
        return create(null, null);
    }

    public static JSConcatStringsNode create(JavaScriptNode left, JavaScriptNode right) {
        return JSConcatStringsNodeGen.create(left, right);
    }

    public abstract CharSequence executeCharSequence(CharSequence a, CharSequence b);

    @Specialization(guards = "length(left, leftProfile1, leftProfile2) == 0")
    protected static CharSequence doLeftEmpty(CharSequence left, CharSequence right) {
        return right;
    }

    @Specialization(guards = "length(right, rightProfile1, rightProfile2) == 0")
    protected static CharSequence doRightEmpty(CharSequence left, CharSequence right) {
        return left;
    }

    @Specialization(guards = {"concatGuard(left, right)"})
    protected CharSequence doConcat(CharSequence left, CharSequence right,
                    @Cached("createBinaryProfile()") ConditionProfile stringLength,
                    @Cached("createBinaryProfile()") ConditionProfile shortStringAppend) {
        if (JSTruffleOptions.LazyStrings) {
            int leftLength = JSRuntime.length(left, leftProfile1, leftProfile2);
            int rightLength = JSRuntime.length(right, rightProfile1, rightProfile2);
            int resultLength = leftLength + rightLength;
            if (stringLength.profile(resultLength >= JSTruffleOptions.MinLazyStringLength)) {
                if (shortStringAppend.profile(leftLength == 1 || rightLength == 1)) {
                    return JSLazyString.createCheckedShort(left, right, resultLength);
                } else {
                    return JSLazyString.createChecked(left, right, resultLength);
                }
            }
        }
        return stringConcat(left, right);
    }

    @TruffleBoundary
    private static CharSequence stringConcat(CharSequence left, CharSequence right) {
        return left.toString() + right.toString();
    }

    @Specialization(guards = "!concatStringLengthValid(left, right)")
    protected final CharSequence doInvalidLength(CharSequence left, CharSequence right) {
        throw Errors.createRangeErrorInvalidStringLength(this);
    }

    protected boolean concatGuard(CharSequence left, CharSequence right) {
        int leftLength = JSRuntime.length(left, leftProfile1, leftProfile2);
        int rightLength = JSRuntime.length(right, rightProfile1, rightProfile2);
        return leftLength > 0 && rightLength > 0 && (leftLength + rightLength) <= JSTruffleOptions.StringLengthLimit;
    }

    protected boolean concatStringLengthValid(CharSequence left, CharSequence right) {
        return (JSRuntime.length(left, leftProfile1, leftProfile2) + JSRuntime.length(right, rightProfile1, rightProfile2)) <= JSTruffleOptions.StringLengthLimit;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSConcatStringsNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
