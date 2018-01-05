/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes;

/**
 * Marker interface that declares that the node can be repeatedly executed without side effects.
 * It's primarily used in the Parser to find out if a left hand side assignment must be factored out
 * to local variable.
 */
public interface RepeatableNode {
}
