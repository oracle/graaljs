/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.NodeInterface;

/**
 * A node that provides a FrameDescriptor for a function or block scope.
 */
public interface FrameDescriptorProvider extends NodeInterface {
    FrameDescriptor getFrameDescriptor();
}
