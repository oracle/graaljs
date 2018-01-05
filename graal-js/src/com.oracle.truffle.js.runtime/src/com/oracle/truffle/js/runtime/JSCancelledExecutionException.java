/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.nodes.Node;

public class JSCancelledExecutionException extends RuntimeException implements TruffleException {

    private final Node originatingNode;

    private static final long serialVersionUID = 5656896390677153564L;

    public JSCancelledExecutionException(String message, Node originatedBy) {
        super(message);
        this.originatingNode = originatedBy;
    }

    @Override
    public Node getLocation() {
        return originatingNode;
    }

    @Override
    public boolean isCancelled() {
        return true;
    }
}
