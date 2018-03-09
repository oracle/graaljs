/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.*;
import com.oracle.truffle.js.runtime.*;

/**
 * Sub-nodes of this type always return an integer. This can be exploited by not having to insert a
 * {@link JSToIntegerNode}.
 */
public abstract class JSBinaryIntegerShiftNode extends JSBinaryNode {

    protected JSBinaryIntegerShiftNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    protected static boolean largerThan2e32(double d) {
        return Math.abs(d) >= JSRuntime.TWO32;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == int.class;
    }
}
