/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.js.nodes.*;
import com.oracle.truffle.js.runtime.objects.*;

@NodeInfo(shortName = "||")
public class JSOrNode extends JSLogicalNode {

    public JSOrNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JSOrNode create(JavaScriptNode left, JavaScriptNode right) {
        return new JSOrNode(left, right);
    }

    public static JSOrNode createNotUndefinedOr(JavaScriptNode left, JavaScriptNode right) {
        return new NotUndefinedOrNode(left, right);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object leftValue = getLeft().execute(frame);
        boolean leftAsBoolean = toBoolean(leftValue);
        if (canShortCircuit.profile(leftAsBoolean)) {
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
        return new JSOrNode(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }

    public static class NotUndefinedOrNode extends JSOrNode {
        public NotUndefinedOrNode(JavaScriptNode left, JavaScriptNode right) {
            super(left, right);
        }

        @Override
        protected boolean toBoolean(Object operand) {
            return operand != Undefined.instance;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new NotUndefinedOrNode(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
        }
    }
}
