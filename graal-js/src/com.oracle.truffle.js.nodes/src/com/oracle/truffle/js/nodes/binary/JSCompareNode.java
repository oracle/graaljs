/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNode.JSStringToNumberWithTrimNode;

public abstract class JSCompareNode extends JSBinaryNode {

    @Child private JSStringToNumberWithTrimNode stringToNumberNode;

    @Override
    public final Object execute(VirtualFrame frame) {
        return Boolean.valueOf(executeBoolean(frame));
    }

    @Override
    public abstract boolean executeBoolean(VirtualFrame frame);

    protected double stringToDouble(String value) {
        if (stringToNumberNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stringToNumberNode = insert(JSStringToNumberWithTrimNode.create());
        }
        return stringToNumberNode.executeString(value);
    }

    @Override
    public final boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }
}
