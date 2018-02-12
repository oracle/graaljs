/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSFrameUtil;

public abstract class LazyReadFrameSlotNode extends JavaScriptNode implements ReadNode {
    protected final Object identifier;

    public LazyReadFrameSlotNode(Object identifier) {
        this.identifier = identifier;
    }

    public Object getIdentifier() {
        return identifier;
    }

    public static LazyReadFrameSlotNode create(Object identifier) {
        return new LazyReadFrameSlotUninitNode(identifier);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return copy();
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class LazyReadFrameSlotUninitNode extends LazyReadFrameSlotNode {
        LazyReadFrameSlotUninitNode(Object identifier) {
            super(identifier);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            Frame outerFrame = frame;
            for (int frameLevel = 0;; frameLevel++) {
                Frame outerScope = outerFrame;
                FrameSlot parentSlot = null;
                for (int scopeLevel = 0;; scopeLevel++) {
                    FrameSlot slot = outerScope.getFrameDescriptor().findFrameSlot(identifier);
                    if (slot != null) {
                        JSReadFrameSlotNode resolved = JSReadFrameSlotNode.create(slot, ScopeFrameNode.create(frameLevel, scopeLevel, parentSlot), JSFrameUtil.hasTemporalDeadZone(slot));
                        return this.replace(resolved).execute(frame);
                    }

                    parentSlot = outerScope.getFrameDescriptor().findFrameSlot(ScopeFrameNode.PARENT_SCOPE_IDENTIFIER);
                    if (parentSlot == null) {
                        break;
                    }
                    outerScope = (Frame) FrameUtil.getObjectSafe(outerScope, parentSlot);
                }

                outerFrame = JSArguments.getEnclosingFrame(outerFrame.getArguments());
                if (outerFrame == JSFrameUtil.NULL_MATERIALIZED_FRAME) {
                    break;
                }
            }

            throw new RuntimeException("frame slot not found");
        }
    }
}
