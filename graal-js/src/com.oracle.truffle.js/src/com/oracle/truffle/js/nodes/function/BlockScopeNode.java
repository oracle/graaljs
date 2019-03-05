/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
