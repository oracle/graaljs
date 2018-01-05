/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.js.nodes.*;
import com.oracle.truffle.js.nodes.cast.*;
import com.oracle.truffle.js.nodes.function.*;
import com.oracle.truffle.js.runtime.*;

public abstract class ImulNode extends JSBuiltinNode {
    ImulNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    public static ImulNode create(JSContext context, JSBuiltin builtin, JavaScriptNode[] arguments) {
        return ImulNodeGen.create(context, builtin, createCast(arguments));
    }

    protected static JavaScriptNode[] createCast(JavaScriptNode[] argumentNodes) {
        for (int i = 0; i < argumentNodes.length; i++) {
            argumentNodes[i] = JSToInt32Node.create(argumentNodes[i]);
        }
        return argumentNodes;
    }

    @Specialization
    protected static int imul(int a, int b) {
        return a * b;
    }
}
