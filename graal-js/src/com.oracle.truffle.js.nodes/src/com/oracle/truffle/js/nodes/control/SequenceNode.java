/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.js.nodes.JavaScriptNode;

/**
 * Common interface for all block/multi-statement nodes.
 * 
 * Currently used for return statement optimization.
 */
public interface SequenceNode {

    JavaScriptNode[] getStatements();
}
