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
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * Implementation of ToLength (ES6 7.1.15).
 *
 */
public abstract class JSToLengthNode extends JavaScriptBaseNode {

    protected final BranchProfile needNegativeBranch = BranchProfile.create();

    public static JSToLengthNode create() {
        return JSToLengthNodeGen.create();
    }

    public abstract long executeLong(Object value);

    @Specialization
    protected long doInt(int value) {
        if (value < 0) {
            needNegativeBranch.enter();
            return 0;
        }
        return value;
    }

    @Specialization
    protected long doDouble(double value,
                    @Cached("create()") BranchProfile needPositiveInfinityBranch,
                    @Cached("create()") BranchProfile needNaNBranch,
                    @Cached("create()") BranchProfile needSafeBranch) {
        if (Double.isNaN(value)) {
            needNaNBranch.enter();
            return 0;
        }
        if (JSRuntime.isPositiveInfinity(value)) {
            needPositiveInfinityBranch.enter();
            return JSRuntime.MAX_SAFE_INTEGER_LONG;
        }
        return doLong((long) value, needSafeBranch);
    }

    @Specialization(guards = "isUndefined(value)")
    protected long doUndefined(@SuppressWarnings("unused") DynamicObject value) {
        return 0;
    }

    @Specialization
    protected long doObject(Object value,
                    @Cached("create()") JSToNumberNode toNumberNode,
                    @Cached("create()") BranchProfile needSafeBranch) {
        Number result = (Number) toNumberNode.execute(value);
        return doLong(JSRuntime.toInteger(result), needSafeBranch);
    }

    private long doLong(final long value, final BranchProfile needSafeBranch) {
        if (value < 0) {
            needNegativeBranch.enter();
            return 0;
        }
        if (value > JSRuntime.MAX_SAFE_INTEGER_LONG) {
            needSafeBranch.enter();
            return JSRuntime.MAX_SAFE_INTEGER_LONG;
        }
        return value;
    }
}
