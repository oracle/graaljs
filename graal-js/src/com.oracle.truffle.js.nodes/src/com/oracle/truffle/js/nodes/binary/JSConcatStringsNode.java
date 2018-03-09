/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
    protected static CharSequence doInvalidLength(CharSequence left, CharSequence right) {
        throw Errors.createRangeErrorInvalidStringLength();
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
