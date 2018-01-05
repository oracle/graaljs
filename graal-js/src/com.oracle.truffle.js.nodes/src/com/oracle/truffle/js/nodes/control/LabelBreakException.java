/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

public final class LabelBreakException extends BreakException {

    private static final long serialVersionUID = -91013036379258890L;

    public LabelBreakException(int id) {
        super(id);
    }
}
