/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class ScopeFrameNode extends JavaScriptBaseNode {
    public static final FrameDescriptor SCOPE_FRAME_DESCRIPTOR;
    public static final FrameSlot PARENT_SCOPE_SLOT;
    public static final Object PARENT_SCOPE_IDENTIFIER = "<parent>";
    static {
        SCOPE_FRAME_DESCRIPTOR = new FrameDescriptor(Undefined.instance);
        PARENT_SCOPE_SLOT = SCOPE_FRAME_DESCRIPTOR.addFrameSlot(PARENT_SCOPE_IDENTIFIER, FrameSlotKind.Object);
    }

    public static ScopeFrameNode create(int frameLevel, int scopeLevel) {
        return create(frameLevel, scopeLevel, PARENT_SCOPE_SLOT);
    }

    public static ScopeFrameNode create(int frameLevel, int scopeLevel, FrameSlot parentSlot) {
        if (frameLevel == 0) {
            if (scopeLevel == 0) {
                return new CurrentFrameNode();
            }
            return new EnclosingScopeFrameNode(scopeLevel, parentSlot);
        } else if (scopeLevel == 0) {
            if (frameLevel == 1) {
                return new EnclosingFunctionFrameNodeLevel1();
            }
            return new EnclosingFunctionFrameNode(frameLevel);
        }
        return new EnclosingFunctionScopeFrameNode(frameLevel, scopeLevel, parentSlot);
    }

    public static ScopeFrameNode createGlobalScope(JSContext context) {
        return new GlobalScopeFrameNode(context);
    }

    public static boolean isBlockScopeFrame(Frame frame) {
        CompilerAsserts.neverPartOfCompilation("do not check FrameDescriptor in compiled code");
        return frame.getFrameDescriptor().getSlots().contains(PARENT_SCOPE_SLOT);
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
        private final FrameSlot parentSlot;

        EnclosingScopeFrameNode(int scopeLevel, FrameSlot parentSlot) {
            assert scopeLevel >= 1;
            this.scopeLevel = scopeLevel;
            this.parentSlot = parentSlot;
        }

        @Override
        @ExplodeLoop
        public Frame executeFrame(Frame frame) {
            Frame retFrame = frame;
            for (int i = 0; i < scopeLevel; i++) {
                retFrame = JSFrameUtil.castMaterializedFrame(FrameUtil.getObjectSafe(retFrame, parentSlot));
            }
            return retFrame;
        }
    }

    private static final class EnclosingFunctionScopeFrameNode extends ScopeFrameNode {
        private final int frameLevel;
        private final int scopeLevel;
        private final FrameSlot parentSlot;

        EnclosingFunctionScopeFrameNode(int frameLevel, int scopeLevel, FrameSlot parentSlot) {
            this.frameLevel = frameLevel;
            this.scopeLevel = scopeLevel;
            this.parentSlot = parentSlot;
        }

        @Override
        @ExplodeLoop
        public Frame executeFrame(Frame frame) {
            Frame retFrame = frame;
            for (int i = 0; i < frameLevel; i++) {
                retFrame = JSFrameUtil.castMaterializedFrame(JSArguments.getEnclosingFrame(retFrame.getArguments()));
            }
            for (int i = 0; i < scopeLevel; i++) {
                retFrame = JSFrameUtil.castMaterializedFrame(FrameUtil.getObjectSafe(retFrame, parentSlot));
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

    private static final class GlobalScopeFrameNode extends ScopeFrameNode {
        private final JSContext context;

        GlobalScopeFrameNode(JSContext context) {
            this.context = context;
        }

        @Override
        public Frame executeFrame(Frame frame) {
            return context.getRealm().getGlobalScope();
        }
    }
}
