/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.array.dyn.ConstantObjectArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;

public class AccessRestArgumentsNode extends AccessIndexedArgumentNode {
    private final JSContext context;
    private final int trailingArgCount;

    AccessRestArgumentsNode(JSContext context, int paramIndex, int trailingArgCount) {
        super(paramIndex);
        this.context = context;
        this.trailingArgCount = trailingArgCount;
    }

    public static AccessRestArgumentsNode create(JSContext context, int paramIndex) {
        return new AccessRestArgumentsNode(context, paramIndex, 0);
    }

    public static AccessRestArgumentsNode create(JSContext context, int paramIndex, int trailingArgCount) {
        return new AccessRestArgumentsNode(context, paramIndex, trailingArgCount);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] jsArguments = frame.getArguments();
        int restLength = JSArguments.getUserArgumentCount(jsArguments) - index - trailingArgCount;
        if (profile(restLength > 0)) {
            return JSArray.create(context, ConstantObjectArray.createConstantObjectArray(), JSArguments.extractUserArguments(jsArguments, index, trailingArgCount), restLength);
        } else {
            return JSArray.createEmptyZeroLength(context);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new AccessRestArgumentsNode(context, index, trailingArgCount);
    }
}
