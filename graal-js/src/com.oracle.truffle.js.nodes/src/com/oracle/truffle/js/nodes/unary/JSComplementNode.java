/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;

@NodeInfo(shortName = "~")
public abstract class JSComplementNode extends JSUnaryNode {

    public static JSComplementNode create(JavaScriptNode operand) {
        Truncatable.truncate(operand);
        return JSComplementNodeGen.create(JSToInt32Node.create(operand));
    }

    @Specialization
    protected int doInteger(int a) {
        return ~a;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == int.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSComplementNodeGen.create(cloneUninitialized(getOperand()));
    }
}
