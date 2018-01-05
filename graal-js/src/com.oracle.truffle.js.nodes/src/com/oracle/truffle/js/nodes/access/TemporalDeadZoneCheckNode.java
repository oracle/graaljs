/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSFrameUtil;

public final class TemporalDeadZoneCheckNode extends FrameSlotNode {
    @Child private JavaScriptNode child;
    @Child private LevelScopeFrameNode levelFrameNode;
    private final BranchProfile deadBranch = BranchProfile.create();

    private TemporalDeadZoneCheckNode(FrameSlot frameSlot, LevelScopeFrameNode levelFrameNode, JavaScriptNode child) {
        super(frameSlot);
        this.levelFrameNode = levelFrameNode;
        this.child = child;
        assert JSFrameUtil.hasTemporalDeadZone(frameSlot);
    }

    private void checkNotDead(VirtualFrame frame) {
        Frame levelFrame = levelFrameNode.executeFrame(frame);
        if (levelFrame.isObject(frameSlot)) {
            checkNotDead(super.getObject(levelFrame), deadBranch);
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        checkNotDead(frame);
        return child.execute(frame);
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        checkNotDead(frame);
        return child.executeInt(frame);
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        checkNotDead(frame);
        return child.executeDouble(frame);
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        checkNotDead(frame);
        return child.executeBoolean(frame);
    }

    @Override
    public LevelScopeFrameNode getLevelFrameNode() {
        return levelFrameNode;
    }

    @Override
    public boolean hasTemporalDeadZone() {
        return true;
    }

    public static TemporalDeadZoneCheckNode create(FrameSlot frameSlot, int frameLevel, int scopeLevel, JavaScriptNode rhs) {
        return create(frameSlot, LevelScopeFrameNode.create(frameLevel, scopeLevel), rhs);
    }

    public static TemporalDeadZoneCheckNode create(FrameSlot frameSlot, LevelScopeFrameNode levelFrameNode, JavaScriptNode rhs) {
        return new TemporalDeadZoneCheckNode(frameSlot, levelFrameNode, rhs);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new TemporalDeadZoneCheckNode(frameSlot, NodeUtil.cloneNode(levelFrameNode), cloneUninitialized(child));
    }
}
