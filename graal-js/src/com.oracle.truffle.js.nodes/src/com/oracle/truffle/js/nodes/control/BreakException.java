/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.nodes.ControlFlowException;

public abstract class BreakException extends ControlFlowException {

    private static final long serialVersionUID = -91013036379258890L;

    private final int id;

    public BreakException(int id) {
        this.id = id;
    }

    public final boolean matchTarget(int targetId) {
        return id == targetId;
    }

    public final boolean matchTarget(BreakTarget target) {
        return this == target.getBreakException();
    }
}
