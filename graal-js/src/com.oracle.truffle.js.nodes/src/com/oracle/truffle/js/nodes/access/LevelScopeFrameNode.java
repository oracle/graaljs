/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.js.nodes.*;
import com.oracle.truffle.js.runtime.*;
import com.oracle.truffle.js.runtime.objects.*;

public abstract class LevelScopeFrameNode extends JavaScriptBaseNode {
    public static final FrameDescriptor SCOPE_FRAME_DESCRIPTOR;
    public static final FrameSlot PARENT_SCOPE_SLOT;
    public static final Object PARENT_SCOPE_IDENTIFIER = "<parent>";
    static {
        SCOPE_FRAME_DESCRIPTOR = new FrameDescriptor(Undefined.instance);
        PARENT_SCOPE_SLOT = SCOPE_FRAME_DESCRIPTOR.addFrameSlot(PARENT_SCOPE_IDENTIFIER, FrameSlotKind.Object);
    }

    public static LevelScopeFrameNode create(int frameLevel, int scopeLevel) {
        return create(frameLevel, scopeLevel, PARENT_SCOPE_SLOT);
    }

    public static LevelScopeFrameNode create(int frameLevel, int scopeLevel, FrameSlot parentSlot) {
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

    public static boolean isBlockScopeFrame(Frame frame) {
        CompilerAsserts.neverPartOfCompilation("do not check FrameDescriptor in compiled code");
        return frame.getFrameDescriptor().getSlots().contains(PARENT_SCOPE_SLOT);
    }

    public abstract Frame executeFrame(Frame frame);

    @NodeInfo(cost = NodeCost.NONE)
    private static final class CurrentFrameNode extends LevelScopeFrameNode {
        @Override
        public Frame executeFrame(Frame frame) {
            return frame;
        }
    }

    private static final class EnclosingScopeFrameNode extends LevelScopeFrameNode {
        private final int scopeLevel;
        private final FrameSlot parentSlot;
        private final ValueProfile frameClassProfile = ValueProfile.createClassProfile();

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
                retFrame = (Frame) frameClassProfile.profile(FrameUtil.getObjectSafe(retFrame, parentSlot));
            }
            return retFrame;
        }
    }

    private static final class EnclosingFunctionScopeFrameNode extends LevelScopeFrameNode {
        private final int frameLevel;
        private final int scopeLevel;
        private final FrameSlot parentSlot;
        private final ValueProfile frameClassProfile = ValueProfile.createClassProfile();

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
                retFrame = JSArguments.getEnclosingFrame(retFrame.getArguments());
            }
            for (int i = 0; i < scopeLevel; i++) {
                retFrame = (Frame) frameClassProfile.profile(FrameUtil.getObjectSafe(retFrame, parentSlot));
            }
            return retFrame;
        }
    }

    private static final class EnclosingFunctionFrameNodeLevel1 extends LevelScopeFrameNode {
        @Override
        public Frame executeFrame(Frame frame) {
            return JSArguments.getEnclosingFrame(frame.getArguments());
        }
    }

    private static final class EnclosingFunctionFrameNode extends LevelScopeFrameNode {
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

    public static final class CallerFrameNode extends LevelScopeFrameNode {
        private final int frameLevel;

        CallerFrameNode(int frameLevel) {
            assert frameLevel >= 1;
            this.frameLevel = frameLevel;
        }

        @Override
        @ExplodeLoop
        public Frame executeFrame(Frame frame) {
            Frame callerFrame = Truffle.getRuntime().getCallerFrame().getFrame(FrameAccess.READ_ONLY);
            MaterializedFrame retFrame = JSArguments.getEnclosingFrame(callerFrame.getArguments());
            for (int i = 1; i < frameLevel; i++) {
                retFrame = JSArguments.getEnclosingFrame(retFrame.getArguments());
            }
            return retFrame;
        }
    }
}
