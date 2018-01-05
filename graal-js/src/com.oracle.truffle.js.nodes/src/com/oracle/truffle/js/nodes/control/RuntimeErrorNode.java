/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;

public final class RuntimeErrorNode extends StatementNode {

    private final JSErrorType errorType;
    private final String message;

    RuntimeErrorNode(JSErrorType errorType, String message) {
        this.errorType = errorType;
        this.message = message;
    }

    public static RuntimeErrorNode create(JSErrorType errorType, String message) {
        return new RuntimeErrorNode(errorType, message);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw createException();
    }

    @TruffleBoundary
    private JSException createException() {
        return JSException.create(errorType, message, this);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(errorType, message);
    }
}
