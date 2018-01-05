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
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSFrameUtil;

public abstract class LazyWriteFrameSlotNode extends JavaScriptNode implements WriteNode {
    protected final Object identifier;
    @Child protected JavaScriptNode rhs;

    public LazyWriteFrameSlotNode(Object identifier, JavaScriptNode rhs) {
        this.identifier = identifier;
        this.rhs = rhs;
    }

    public Object getIdentifier() {
        return identifier;
    }

    public static LazyWriteFrameSlotNode create(Object identifier, JavaScriptNode rhs) {
        return new LazyWriteFrameSlotUninitNode(identifier, rhs);
    }

    @Override
    public JavaScriptNode getRhs() {
        return rhs;
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        return executeWrite(frame, rhs.execute(frame));
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class LazyWriteFrameSlotUninitNode extends LazyWriteFrameSlotNode {
        LazyWriteFrameSlotUninitNode(Object identifier, JavaScriptNode rhs) {
            super(identifier, rhs);
        }

        @Override
        public Object executeWrite(VirtualFrame frame, Object value) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            Frame outerFrame = frame;
            for (int frameLevel = 0;; frameLevel++) {
                Frame outerScope = outerFrame;
                FrameSlot parentSlot = null;
                for (int scopeLevel = 0;; scopeLevel++) {
                    FrameSlot slot = outerScope.getFrameDescriptor().findFrameSlot(identifier);
                    if (slot != null) {
                        JSWriteFrameSlotNode resolved = JSWriteFrameSlotNode.create(slot, LevelScopeFrameNode.create(frameLevel, scopeLevel, parentSlot), rhs, JSFrameUtil.hasTemporalDeadZone(slot));
                        return this.replace(resolved).executeWrite(frame, value);
                    }

                    parentSlot = outerScope.getFrameDescriptor().findFrameSlot(LevelScopeFrameNode.PARENT_SCOPE_IDENTIFIER);
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

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new LazyWriteFrameSlotUninitNode(identifier, cloneUninitialized(rhs));
        }
    }
}
