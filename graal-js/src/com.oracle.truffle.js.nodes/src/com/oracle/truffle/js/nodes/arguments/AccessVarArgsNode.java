/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.arguments;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSArguments;

public class AccessVarArgsNode extends AccessIndexedArgumentNode {
    private static final int UNINITIALIZED = -2;
    private static final int UNSTABLE = -1;
    /** Expected/profiled stable user argument count or {@link #UNSTABLE}. */
    @CompilationFinal private int userArgumentCount;

    AccessVarArgsNode(int paramIndex) {
        super(paramIndex);
        this.userArgumentCount = UNINITIALIZED;
    }

    public static AccessVarArgsNode create(int paramIndex) {
        return new AccessVarArgsNode(paramIndex);
    }

    @Override
    public final Object[] execute(VirtualFrame frame) {
        return executeObjectArray(frame);
    }

    @Override
    public final Object[] executeObjectArray(VirtualFrame frame) {
        Object[] arguments = frame.getArguments();
        int currentUserArgumentCount = JSArguments.getUserArgumentCount(arguments);

        if (profile(index >= currentUserArgumentCount)) {
            return JSArguments.EMPTY_ARGUMENTS_ARRAY;
        } else {

            int constantUserArgumentCount = userArgumentCount;

            if (constantUserArgumentCount == UNINITIALIZED) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                constantUserArgumentCount = currentUserArgumentCount;
                userArgumentCount = currentUserArgumentCount;
            }

            if (constantUserArgumentCount == UNSTABLE) {
                return getArgumentsArrayWithoutExplosion(arguments, currentUserArgumentCount);
            }

            if (constantUserArgumentCount != currentUserArgumentCount) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                constantUserArgumentCount = currentUserArgumentCount;
                userArgumentCount = UNSTABLE;
            }

            return getArgumentsArray(arguments, constantUserArgumentCount);
        }
    }

    @ExplodeLoop
    private Object[] getArgumentsArray(Object[] arguments, int constantUserArgumentCount) {
        int length = constantUserArgumentCount - index;
        Object[] varArgs = new Object[length];
        for (int i = 0; i < length; i++) {
            varArgs[i] = JSArguments.getUserArgument(arguments, i + index);
        }
        return varArgs;
    }

    private Object[] getArgumentsArrayWithoutExplosion(Object[] arguments, int currentUserArgumentCount) {
        int length = currentUserArgumentCount - index;
        Object[] varArgs = new Object[length];
        for (int i = 0; i < length; i++) {
            varArgs[i] = JSArguments.getUserArgument(arguments, i + index);
        }
        return varArgs;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new AccessVarArgsNode(index);
    }
}
