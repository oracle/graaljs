/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.arguments;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class AccessIndexedArgumentNode extends JavaScriptNode implements RepeatableNode {
    protected final int index;
    @CompilationFinal private boolean wasTrue;
    @CompilationFinal private boolean wasFalse;

    AccessIndexedArgumentNode(int index) {
        this.index = index;
    }

    public static JavaScriptNode create(int paramIndex) {
        return new AccessIndexedArgumentNode(paramIndex);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] jsArguments = frame.getArguments();
        if (profile(index < JSArguments.getUserArgumentCount(jsArguments))) {
            Object userArg = JSArguments.getUserArgument(jsArguments, index);
            assert userArg != null; // contract: js function arguments cannot be null
            return userArg;
        } else {
            return Undefined.instance;
        }
    }

    // intentional code duplication from ConditionProfile.Binary.
    // this is THE most impacting ConditionProfile on Node.js workloads (Footprint).
    protected final boolean profile(boolean value) {
        if (value) {
            if (!wasTrue) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                wasTrue = true;
            }
            return true;
        } else {
            if (!wasFalse) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                wasFalse = true;
            }
            return false;
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(index);
    }

    public int getIndex() {
        return index;
    }
}
