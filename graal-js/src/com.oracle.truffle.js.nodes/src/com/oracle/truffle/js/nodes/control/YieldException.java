/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class YieldException extends ControlFlowException {
    private static final long serialVersionUID = 3168046581744128272L;

    public static final YieldException YIELD_NULL = new YieldException(null);
    public static final YieldException AWAIT_NULL = new YieldException(null);

    private final Object result;

    public YieldException(Object result) {
        this.result = result;
    }

    public Object getResult() {
        return result;
    }

    public boolean isYield() {
        return this != AWAIT_NULL;
    }

    public boolean isAwait() {
        return this == AWAIT_NULL;
    }
}
