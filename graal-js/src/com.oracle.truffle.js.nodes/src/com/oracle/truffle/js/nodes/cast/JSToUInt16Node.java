/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * Implementation of ToUInt16.
 *
 */
public abstract class JSToUInt16Node extends JavaScriptBaseNode {

    public static JSToUInt16Node create() {
        return JSToUInt16NodeGen.create();
    }

    public abstract int executeInt(Object value);

    @Specialization
    protected int doInt(int value) {
        return JSRuntime.toUInt16(value);
    }

    @Specialization
    protected int doDouble(double value,
                    @Cached("create()") BranchProfile needPositiveInfinityBranch) {
        if (JSRuntime.isPositiveInfinity(value)) {
            needPositiveInfinityBranch.enter();
            return 0;
        }
        return JSRuntime.toUInt16((long) value);
    }

    @Specialization
    protected int doGeneric(Object value,
                    @Cached("create()") JSToNumberNode toNumberNode) {
        return JSRuntime.toUInt16(toNumberNode.executeNumber(value));
    }
}
