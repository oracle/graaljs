/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * Implementation of the abstract operation ToIndex(value) (ES7 7.1.17).
 */
public abstract class JSToIndexNode extends JavaScriptBaseNode {

    final BranchProfile negativeIndexBranch = BranchProfile.create();

    public static JSToIndexNode create() {
        return JSToIndexNodeGen.create();
    }

    public abstract long executeLong(Object value);

    @Specialization(guards = "isUndefined(value)")
    protected long doUndefined(@SuppressWarnings("unused") DynamicObject value) {
        return 0;
    }

    @Specialization
    protected long doInt(int value) {
        if (value < 0) {
            negativeIndexBranch.enter();
            throw Errors.createRangeError("index is negative");
        }
        return value;
    }

    @Specialization
    protected long doDouble(double value,
                    @Cached("create()") BranchProfile tooLargeIndexBranch) {
        long integerIndex = (long) value;
        if (integerIndex < 0) {
            negativeIndexBranch.enter();
            throw Errors.createRangeError("index is negative");
        }
        if (integerIndex <= JSRuntime.MAX_SAFE_INTEGER_LONG) {
            return integerIndex;
        } else {
            tooLargeIndexBranch.enter();
            throw Errors.createRangeError("index is too large");
        }
    }

    @Specialization
    protected static long doObject(Object value,
                    @Cached("create()") JSToNumberNode toNumberNode,
                    @Cached("create()") JSToIndexNode recursiveToIndexNode) {
        Number number = (Number) toNumberNode.execute(value);
        assert number instanceof Integer || number instanceof Double;
        return recursiveToIndexNode.executeLong(number);
    }
}
