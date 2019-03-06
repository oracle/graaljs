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
package com.oracle.truffle.js.nodes.access;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSFrameUtil;

public abstract class ScopeFrameNode extends JavaScriptBaseNode {
    public static final Object PARENT_SCOPE_IDENTIFIER = "<parent>";
    public static final FrameSlot[] EMPTY_FRAME_SLOT_ARRAY = {};

    public static ScopeFrameNode create(int frameLevel) {
        return create(frameLevel, 0, EMPTY_FRAME_SLOT_ARRAY);
    }

    public static ScopeFrameNode create(int frameLevel, int scopeLevel, FrameSlot[] parentSlots) {
        assert scopeLevel == parentSlots.length;
        if (frameLevel == 0) {
            if (scopeLevel == 0) {
                return new CurrentFrameNode();
            }
            return new EnclosingScopeFrameNode(scopeLevel, parentSlots);
        } else if (scopeLevel == 0) {
            if (frameLevel == 1) {
                return new EnclosingFunctionFrameNodeLevel1();
            }
            return new EnclosingFunctionFrameNode(frameLevel);
        }
        return new EnclosingFunctionScopeFrameNode(frameLevel, scopeLevel, parentSlots);
    }

    public static boolean isBlockScopeFrame(Frame frame) {
        List<? extends FrameSlot> slots = frame.getFrameDescriptor().getSlots();
        return !slots.isEmpty() && slots.get(0).getIdentifier() == PARENT_SCOPE_IDENTIFIER;
    }

    public abstract Frame executeFrame(Frame frame);

    @NodeInfo(cost = NodeCost.NONE)
    private static final class CurrentFrameNode extends ScopeFrameNode {
        @Override
        public Frame executeFrame(Frame frame) {
            return frame;
        }
    }

    private static final class EnclosingScopeFrameNode extends ScopeFrameNode {
        private final int scopeLevel;
        @CompilationFinal(dimensions = 1) private final FrameSlot[] parentSlots;

        EnclosingScopeFrameNode(int scopeLevel, FrameSlot[] parentSlots) {
            assert scopeLevel >= 1;
            this.scopeLevel = scopeLevel;
            this.parentSlots = parentSlots;
        }

        @Override
        @ExplodeLoop
        public Frame executeFrame(Frame frame) {
            Frame retFrame = frame;
            for (int i = 0; i < scopeLevel; i++) {
                retFrame = JSFrameUtil.castMaterializedFrame(FrameUtil.getObjectSafe(retFrame, parentSlots[i]));
            }
            return retFrame;
        }
    }

    private static final class EnclosingFunctionScopeFrameNode extends ScopeFrameNode {
        private final int frameLevel;
        private final int scopeLevel;
        @CompilationFinal(dimensions = 1) private final FrameSlot[] parentSlots;

        EnclosingFunctionScopeFrameNode(int frameLevel, int scopeLevel, FrameSlot[] parentSlots) {
            this.frameLevel = frameLevel;
            this.scopeLevel = scopeLevel;
            this.parentSlots = parentSlots;
        }

        @Override
        @ExplodeLoop
        public Frame executeFrame(Frame frame) {
            Frame retFrame = frame;
            for (int i = 0; i < frameLevel; i++) {
                retFrame = JSFrameUtil.castMaterializedFrame(JSArguments.getEnclosingFrame(retFrame.getArguments()));
            }
            for (int i = 0; i < scopeLevel; i++) {
                retFrame = JSFrameUtil.castMaterializedFrame(FrameUtil.getObjectSafe(retFrame, parentSlots[i]));
            }
            return retFrame;
        }
    }

    private static final class EnclosingFunctionFrameNodeLevel1 extends ScopeFrameNode {
        @Override
        public Frame executeFrame(Frame frame) {
            return JSFrameUtil.castMaterializedFrame(JSArguments.getEnclosingFrame(frame.getArguments()));
        }
    }

    private static final class EnclosingFunctionFrameNode extends ScopeFrameNode {
        private final int frameLevel;

        EnclosingFunctionFrameNode(int frameLevel) {
            assert frameLevel >= 1;
            this.frameLevel = frameLevel;
        }

        @Override
        @ExplodeLoop
        public Frame executeFrame(Frame frame) {
            MaterializedFrame retFrame = JSArguments.getEnclosingFrame(frame.getArguments());
            for (int i = 1; i < frameLevel; i++) {
                retFrame = JSArguments.getEnclosingFrame(retFrame.getArguments());
            }
            return retFrame;
        }
    }
}
