/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class MathOperation extends JSBuiltinNode {

    public MathOperation(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Child private JSToDoubleNode toDoubleNode;

    protected final double toDouble(Object target) {
        if (toDoubleNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toDoubleNode = insert(JSToDoubleNode.create());
        }
        return toDoubleNode.executeDouble(target);
    }
}
