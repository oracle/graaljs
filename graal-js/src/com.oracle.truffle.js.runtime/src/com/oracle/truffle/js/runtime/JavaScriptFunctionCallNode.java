/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.nodes.Node;

/**
 * An interface implemented by nodes that represent JavaScript function call.
 */
public interface JavaScriptFunctionCallNode {

    /**
     * Returns the target of the call.
     * 
     * @return target of the call.
     */
    Node getTarget();

}
