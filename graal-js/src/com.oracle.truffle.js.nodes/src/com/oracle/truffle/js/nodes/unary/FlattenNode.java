/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.PropertyReference;

/**
 * Flatten lazy strings.
 */
public abstract class FlattenNode extends JavaScriptBaseNode {
    FlattenNode() {
    }

    public abstract Object execute(Object value);

    @Specialization
    protected static String doLazyString(JSLazyString value) {
        return value.toString();
    }

    @Specialization
    protected static String doLazyString(PropertyReference value) {
        return value.toString();
    }

    @Specialization
    protected static double doLargeInteger(LargeInteger value) {
        return value.doubleValue();
    }

    @Fallback
    protected static Object doOther(Object value) {
        assert !JSRuntime.isLazyString(value);
        return value;
    }

    public static FlattenNode create() {
        return FlattenNodeGen.create();
    }
}
