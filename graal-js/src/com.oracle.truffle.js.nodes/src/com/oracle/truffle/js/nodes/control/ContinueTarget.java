/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

public class ContinueTarget extends BreakTarget {

    private static final ContinueTarget DEFAULT_LOOP_CONTINUE_TARGET = new ContinueTarget(null, 0, DirectBreakException.instance, ContinueException.instance);
    private final ContinueException continueException;

    protected ContinueTarget(Object label, int id, BreakException breakException, ContinueException continueException) {
        super(label, id, breakException);
        this.continueException = continueException;
    }

    public final ContinueException getContinueException() {
        return continueException;
    }

    public static ContinueTarget forLoop(Object label, int id) {
        return new ContinueTarget(label, id, DirectBreakException.instance, new ContinueException(id));
    }

    public static ContinueTarget forUnlabeledLoop() {
        return DEFAULT_LOOP_CONTINUE_TARGET;
    }
}
