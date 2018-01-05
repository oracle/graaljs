/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;

public abstract class IterationScopeNode extends JavaScriptNode {

    public static IterationScopeNode create(FrameDescriptor frameDescriptor, JSReadFrameSlotNode[] reads, JSWriteFrameSlotNode[] writes) {
        return new FrameIterationScopeNode(frameDescriptor, reads, writes);
    }

    @Override
    public abstract VirtualFrame execute(VirtualFrame frame);

    public abstract void executeCopy(VirtualFrame toFrame, VirtualFrame fromFrame);

    public static final class FrameIterationScopeNode extends IterationScopeNode {
        private final FrameDescriptor frameDescriptor;
        @Children private final JSReadFrameSlotNode[] reads;
        @Children private final JSWriteFrameSlotNode[] writes;

        public FrameIterationScopeNode(FrameDescriptor frameDescriptor, JSReadFrameSlotNode[] reads, JSWriteFrameSlotNode[] writes) {
            this.frameDescriptor = frameDescriptor;
            this.reads = reads;
            this.writes = writes;
            assert reads.length == writes.length;
        }

        @Override
        public VirtualFrame execute(VirtualFrame frame) {
            VirtualFrame nextFrame = Truffle.getRuntime().createVirtualFrame(frame.getArguments(), frameDescriptor);
            executeCopy(nextFrame, frame);
            return nextFrame;
        }

        @ExplodeLoop
        @Override
        public void executeCopy(VirtualFrame nextFrame, VirtualFrame frame) {
            for (int i = 0; i < reads.length; i++) {
                writes[i].executeWithFrame(nextFrame, reads[i].execute(frame));
            }
        }

        public FrameDescriptor getFrameDescriptor() {
            return frameDescriptor;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new FrameIterationScopeNode(frameDescriptor, cloneUninitialized(reads), cloneUninitialized(writes));
        }
    }
}
