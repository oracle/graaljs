/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class ReturnException extends ControlFlowException {

    private static final long serialVersionUID = 4073191346281369231L;

    private final Object result;

    /**
     * Creates the exception with the result of the JavaScript function.
     * 
     * @param result the alternative result
     */
    public ReturnException(Object result) {
        this.result = result;
    }

    /**
     * @return the unexpected result
     */
    public Object getResult() {
        return result;
    }
}
