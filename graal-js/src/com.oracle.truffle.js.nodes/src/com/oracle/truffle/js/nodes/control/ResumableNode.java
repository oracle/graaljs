/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.objects.Undefined;

public interface ResumableNode {
    Object resume(VirtualFrame frame);

    @SuppressWarnings("deprecation")
    static GeneratorWrapperNode parent(ResumableNode node) {
        Node parent = ((Node) node).getParent();
        if (parent instanceof com.oracle.truffle.api.instrumentation.InstrumentableFactory.WrapperNode) {
            assert ((com.oracle.truffle.api.instrumentation.InstrumentableFactory.WrapperNode) parent).getDelegateNode() == node;
            parent = parent.getParent();
        }
        return (GeneratorWrapperNode) parent;
    }

    default void setState(VirtualFrame frame, Object state) {
        parent(this).setState(frame, state);
    }

    default Object getState(VirtualFrame frame) {
        return parent(this).getState(frame);
    }

    default int getStateAsInt(VirtualFrame frame) {
        return parent(this).getStateAsInt(frame);
    }

    default Object getStateAndReset(VirtualFrame frame) {
        try {
            return getState(frame);
        } finally {
            resetState(frame);
        }
    }

    default int getStateAsIntAndReset(VirtualFrame frame) {
        try {
            return getStateAsInt(frame);
        } finally {
            resetState(frame);
        }
    }

    default void resetState(VirtualFrame frame) {
        setState(frame, Undefined.instance);
    }
}
