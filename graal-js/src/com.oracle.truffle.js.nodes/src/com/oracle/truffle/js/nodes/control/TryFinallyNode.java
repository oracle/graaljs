/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * 12.14 The try Statement.
 */
@NodeInfo(shortName = "try-finally")
public class TryFinallyNode extends StatementNode implements ResumableNode {

    @Child private JavaScriptNode tryBlock;
    @Child private JavaScriptNode finallyBlock;

    TryFinallyNode(JavaScriptNode tryBlock, JavaScriptNode finallyBlock) {
        this.tryBlock = tryBlock;
        this.finallyBlock = finallyBlock;
    }

    public static JavaScriptNode create(JavaScriptNode tryBlock, JavaScriptNode finallyBlock) {
        return new TryFinallyNode(tryBlock, finallyBlock);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(tryBlock), cloneUninitialized(finallyBlock));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result;
        try {
            result = tryBlock.execute(frame);
        } finally {
            finallyBlock.execute(frame);
        }
        return result;
    }

    @Override
    public Object resume(VirtualFrame frame) {
        Object state = getStateAndReset(frame);
        if (state == Undefined.instance) {
            Object result;
            boolean yieldedInTry = false;
            Throwable ex = null;
            try {
                try {
                    result = tryBlock.execute(frame);
                } catch (YieldException e) {
                    yieldedInTry = true;
                    throw e;
                } catch (Throwable e) {
                    ex = e;
                    throw e;
                }
            } finally {
                if (!yieldedInTry) {
                    try {
                        finallyBlock.execute(frame);
                    } catch (YieldException e) {
                        setState(frame, ex);
                        throw e;
                    }
                }
            }
            return result;
        } else {
            try {
                finallyBlock.execute(frame);
            } catch (YieldException e) {
                setState(frame, state);
                throw e;
            }
            if (state != null) {
                rethrow((Throwable) state);
            }
            return EMPTY;
        }
    }

    @TruffleBoundary
    private static void rethrow(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        } else if (throwable instanceof Error) {
            throw (Error) throwable;
        } else {
            throw new RuntimeException(throwable);
        }
    }
}
