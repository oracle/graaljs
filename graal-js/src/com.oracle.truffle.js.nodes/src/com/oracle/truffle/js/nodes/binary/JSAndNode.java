/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.js.nodes.*;

@NodeInfo(shortName = "&&")
public class JSAndNode extends JSLogicalNode {

    public JSAndNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JSAndNode create(JavaScriptNode left, JavaScriptNode right) {
        return new JSAndNode(left, right);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object leftValue = getLeft().execute(frame);
        boolean leftAsBoolean = toBoolean(leftValue);
        if (canShortCircuit.profile(!leftAsBoolean)) {
            return leftValue;
        } else {
            Object rightValue = getRight().execute(frame);
            return rightValue;
        }
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return getLeft().isResultAlwaysOfType(clazz) && getRight().isResultAlwaysOfType(clazz);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new JSAndNode(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
