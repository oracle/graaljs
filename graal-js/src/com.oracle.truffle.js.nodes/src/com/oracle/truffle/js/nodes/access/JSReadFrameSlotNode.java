/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.runtime.LargeInteger;

@ImportStatic(FrameSlotKind.class)
public abstract class JSReadFrameSlotNode extends FrameSlotNode implements RepeatableNode, ReadNode {
    JSReadFrameSlotNode(FrameSlot slot) {
        super(slot);
    }

    public static JSReadFrameSlotNode create(FrameSlot slot, ScopeFrameNode levelFrameNode, boolean hasTemporalDeadZone) {
        if (hasTemporalDeadZone) {
            return JSReadScopeFrameSlotWithTDZNodeGen.create(slot, levelFrameNode);
        } else {
            return JSReadScopeFrameSlotNodeGen.create(slot, levelFrameNode);
        }
    }

    public static JSReadFrameSlotNode create(FrameSlot slot, int frameLevel, int scopeLevel, boolean hasTemporalDeadZone) {
        if (frameLevel == 0 && scopeLevel == 0 && !hasTemporalDeadZone) {
            return JSReadCurrentFrameSlotNodeGen.create(slot);
        }
        return create(slot, ScopeFrameNode.create(frameLevel, scopeLevel), hasTemporalDeadZone);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == ReadVariableExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("name", getIdentifier());
    }

    public static JSReadFrameSlotNode create(FrameSlot slot, int frameLevel, int scopeLevel, FrameSlot parentSlot, boolean hasTemporalDeadZone) {
        return create(slot, ScopeFrameNode.create(frameLevel, scopeLevel, parentSlot), hasTemporalDeadZone);
    }

    @Override
    public String expressionToString() {
        if (frameSlot.getIdentifier() instanceof String) {
            return (String) frameSlot.getIdentifier();
        }
        return null;
    }
}

abstract class JSReadScopeFrameSlotNode extends JSReadFrameSlotNode {
    @Child @Executed ScopeFrameNode scopeFrameNode;

    JSReadScopeFrameSlotNode(FrameSlot slot, ScopeFrameNode scopeFrameNode) {
        super(slot);
        this.scopeFrameNode = scopeFrameNode;
    }

    @Specialization(guards = "levelFrame.isBoolean(frameSlot)")
    protected final boolean doBoolean(Frame levelFrame) {
        return super.getBoolean(levelFrame);
    }

    @Specialization(guards = "levelFrame.isInt(frameSlot)")
    protected final int doInt(Frame levelFrame) {
        return super.getInt(levelFrame);
    }

    @Specialization(guards = "levelFrame.isDouble(frameSlot)")
    protected final double doDouble(Frame levelFrame) {
        return super.getDouble(levelFrame);
    }

    @Specialization(guards = {"levelFrame.isObject(frameSlot)", "!hasTemporalDeadZone()"})
    protected final Object doObject(Frame levelFrame) {
        return super.getObject(levelFrame);
    }

    @Specialization(guards = "levelFrame.isLong(frameSlot)")
    protected final LargeInteger doLargeInteger(Frame levelFrame) {
        return LargeInteger.valueOf(super.getLong(levelFrame));
    }

    @Override
    public ScopeFrameNode getLevelFrameNode() {
        return scopeFrameNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSReadScopeFrameSlotNodeGen.create(frameSlot, NodeUtil.cloneNode(getLevelFrameNode()));
    }
}

abstract class JSReadScopeFrameSlotWithTDZNode extends JSReadScopeFrameSlotNode {
    JSReadScopeFrameSlotWithTDZNode(FrameSlot slot, ScopeFrameNode scopeFrameNode) {
        super(slot, scopeFrameNode);
    }

    @Override
    public boolean hasTemporalDeadZone() {
        return true;
    }

    @Specialization(guards = "levelFrame.isObject(frameSlot)")
    protected final Object doObjectTDZ(Frame levelFrame,
                    @Cached("create()") BranchProfile deadBranch) {
        return checkNotDead(super.getObject(levelFrame), deadBranch);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSReadScopeFrameSlotWithTDZNodeGen.create(frameSlot, NodeUtil.cloneNode(getLevelFrameNode()));
    }
}

abstract class JSReadCurrentFrameSlotNode extends JSReadFrameSlotNode {
    JSReadCurrentFrameSlotNode(FrameSlot slot) {
        super(slot);
    }

    @Specialization(guards = "frame.isBoolean(frameSlot)")
    protected final boolean doBoolean(VirtualFrame frame) {
        return super.getBoolean(frame);
    }

    @Specialization(guards = "frame.isInt(frameSlot)")
    protected final int doInt(VirtualFrame frame) {
        return super.getInt(frame);
    }

    @Specialization(guards = "frame.isDouble(frameSlot)")
    protected final double doDouble(VirtualFrame frame) {
        return super.getDouble(frame);
    }

    @Specialization(guards = "frame.isObject(frameSlot)")
    protected final Object doObject(VirtualFrame frame) {
        return super.getObject(frame);
    }

    @Specialization(guards = "frame.isLong(frameSlot)")
    protected final LargeInteger doLargeInteger(VirtualFrame frame) {
        return LargeInteger.valueOf(super.getLong(frame));
    }

    @Override
    public ScopeFrameNode getLevelFrameNode() {
        return ScopeFrameNode.create(0, 0);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSReadCurrentFrameSlotNodeGen.create(frameSlot);
    }
}
