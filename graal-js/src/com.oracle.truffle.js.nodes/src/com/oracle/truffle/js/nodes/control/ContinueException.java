/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class ContinueException extends ControlFlowException {

    private static final long serialVersionUID = 5329687983726237188L;
    static final ContinueException instance = new ContinueException();

    private final int id;

    public ContinueException() {
        this.id = 0;
    }

    public ContinueException(int id) {
        this.id = id;
    }

    public boolean matchTarget(int targetId) {
        return id == targetId;
    }

    public boolean matchTarget(ContinueTarget target) {
        return this == target.getContinueException();
    }
}
