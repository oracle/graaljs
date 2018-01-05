/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.nodes.Node;

public class ExitException extends RuntimeException implements TruffleException {

    private static final long serialVersionUID = -1456196298096686373L;
    private final int status;
    private final Node location;

    public ExitException(int status) {
        this(status, null);
    }

    public ExitException(int status, Node location) {
        this.status = status;
        this.location = location;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public Node getLocation() {
        return location;
    }

    @Override
    public boolean isExit() {
        return true;
    }

    @Override
    public int getExitStatus() {
        return getStatus();
    }
}
