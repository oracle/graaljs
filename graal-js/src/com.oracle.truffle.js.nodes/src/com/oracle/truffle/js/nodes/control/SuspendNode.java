/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

/**
 * Marker interface for suspending nodes, i.e., {@code yield} and {@code await}.
 */
public interface SuspendNode extends ResumableNode {
}
