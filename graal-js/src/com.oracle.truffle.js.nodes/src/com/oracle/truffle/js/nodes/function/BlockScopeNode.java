/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.js.nodes.FrameDescriptorProvider;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.control.ResumableNode;
import com.oracle.truffle.js.nodes.control.YieldException;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class BlockScopeNode extends JavaScriptNode implements ResumableNode, RepeatingNode {
    @Child protected JavaScriptNode block;

    protected BlockScopeNode(JavaScriptNode block) {
        this.block = block;
    }

    public static BlockScopeNode create(FrameDescriptor frameDescriptor, FrameSlot parentSlot, JavaScriptNode block) {
        return new FrameBlockScopeNode(block, frameDescriptor, parentSlot);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return block.execute(appendScopeFrame(frame));
        } finally {
            exitScope(frame);
        }
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        try {
            block.executeVoid(appendScopeFrame(frame));
        } finally {
            exitScope(frame);
        }
    }

    public abstract VirtualFrame appendScopeFrame(VirtualFrame frame);

    public abstract void exitScope(VirtualFrame frame);

    @Override
    public boolean executeRepeating(VirtualFrame frame) {
        try {
            return ((RepeatingNode) block).executeRepeating(appendScopeFrame(frame));
        } finally {
            exitScope(frame);
        }
    }

    public JavaScriptNode getBlock() {
        return block;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return block.isResultAlwaysOfType(clazz);
    }

    public static final class FrameBlockScopeNode extends BlockScopeNode implements FrameDescriptorProvider {
        private final FrameDescriptor frameDescriptor;
        private final FrameSlot parentSlot;

        protected FrameBlockScopeNode(JavaScriptNode block, FrameDescriptor frameDescriptor, FrameSlot parentSlot) {
            super(block);
            this.frameDescriptor = frameDescriptor;
            this.parentSlot = parentSlot;
        }

        @Override
        public VirtualFrame appendScopeFrame(VirtualFrame frame) {
            VirtualFrame scopeFrame = Truffle.getRuntime().createVirtualFrame(frame.getArguments(), frameDescriptor);
            scopeFrame.setObject(parentSlot, frame.materialize());
            return scopeFrame;
        }

        @Override
        public void exitScope(VirtualFrame frame) {
            assert CompilerDirectives.inCompiledCode() || ScopeFrameNode.isBlockScopeFrame(frame);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return block.execute(appendScopeFrame(frame));
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            block.executeVoid(appendScopeFrame(frame));
        }

        @Override
        public FrameDescriptor getFrameDescriptor() {
            return frameDescriptor;
        }

        @Override
        public Object resume(VirtualFrame frame) {
            Object savedScopeFrame = getStateAndReset(frame);
            // Always materialize the frame here to avoid having to duplicate the block code.
            MaterializedFrame scopeFrame = savedScopeFrame == Undefined.instance ? appendScopeFrame(frame).materialize() : JSFrameUtil.castMaterializedFrame(savedScopeFrame);
            try {
                return block.execute(scopeFrame);
            } catch (YieldException e) {
                setState(frame, scopeFrame);
                throw e;
            }
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new FrameBlockScopeNode(cloneUninitialized(block), frameDescriptor, parentSlot);
        }
    }
}
