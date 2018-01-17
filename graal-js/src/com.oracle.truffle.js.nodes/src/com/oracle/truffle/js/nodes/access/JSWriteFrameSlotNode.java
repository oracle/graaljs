/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags;
import com.oracle.truffle.js.nodes.tags.NodeObjectDescriptor;
import com.oracle.truffle.js.runtime.LargeInteger;

public abstract class JSWriteFrameSlotNode extends FrameSlotNode implements WriteNode {
    protected JSWriteFrameSlotNode(FrameSlot frameSlot) {
        super(frameSlot);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == JSSpecificTags.VariableWriteTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        NodeObjectDescriptor descriptor = JSSpecificTags.createNodeObjectDescriptor();
        descriptor.addProperty("name", getIdentifier());
        return descriptor;
    }

    @Override
    public abstract JavaScriptNode getRhs();

    public abstract Object executeWithFrame(Frame frame, Object value);

    public static JSWriteFrameSlotNode create(FrameSlot frameSlot, int frameLevel, int scopeLevel, JavaScriptNode rhs, boolean hasTemporalDeadZone) {
        if (frameLevel == 0 && scopeLevel == 0 && !hasTemporalDeadZone) {
            return JSWriteCurrentFrameSlotNodeGen.create(frameSlot, rhs);
        }
        return create(frameSlot, ScopeFrameNode.create(frameLevel, scopeLevel), rhs, hasTemporalDeadZone);
    }

    public static JSWriteFrameSlotNode create(FrameSlot frameSlot, ScopeFrameNode levelFrameNode, JavaScriptNode rhs, boolean hasTemporalDeadZone) {
        return JSWriteScopeFrameSlotNodeGen.create(frameSlot, levelFrameNode, hasTemporalDeadZone ? TemporalDeadZoneCheckNode.create(frameSlot, levelFrameNode, rhs) : rhs);
    }

    @Override
    public final boolean isResultAlwaysOfType(Class<?> clazz) {
        return getRhs().isResultAlwaysOfType(clazz);
    }
}

@NodeChild(value = "levelFrameNode", type = ScopeFrameNode.class)
@NodeChild(value = "rhs", type = JavaScriptNode.class)
abstract class JSWriteScopeFrameSlotNode extends JSWriteFrameSlotNode {

    protected JSWriteScopeFrameSlotNode(FrameSlot frameSlot) {
        super(frameSlot);
    }

    @Specialization(guards = "isBooleanKind(levelFrame)")
    protected final boolean doBoolean(Frame levelFrame, boolean value) {
        levelFrame.setBoolean(frameSlot, value);
        return value;
    }

    @Specialization(guards = "isIntegerKind(levelFrame)")
    protected final int doInteger(Frame levelFrame, int value) {
        levelFrame.setInt(frameSlot, value);
        return value;
    }

    @Specialization(guards = "isLongKind(levelFrame)")
    protected final int doLargeIntegerInt(Frame levelFrame, int value) {
        levelFrame.setLong(frameSlot, value);
        return value;
    }

    @Specialization(guards = "isLongKind(levelFrame)")
    protected final LargeInteger doLargeInteger(Frame levelFrame, LargeInteger value) {
        levelFrame.setLong(frameSlot, value.longValue());
        return value;
    }

    @Specialization(guards = "isDoubleKind(levelFrame)", replaces = {"doInteger", "doLargeInteger", "doLargeIntegerInt"})
    protected final double doDouble(Frame levelFrame, double value) {
        levelFrame.setDouble(frameSlot, value);
        return value;
    }

    @Specialization(guards = {"ensureObjectKind(levelFrame)"}, replaces = {"doBoolean", "doInteger", "doDouble", "doLargeInteger", "doLargeIntegerInt"})
    protected final Object doObject(Frame levelFrame, Object value) {
        levelFrame.setObject(frameSlot, value);
        return value;
    }

    @Override
    public final Object executeWithFrame(Frame frame, Object value) {
        return executeEvaluated(null, frame, value);
    }

    abstract Object executeEvaluated(VirtualFrame unusedCurrentFrame, Frame levelFrame, Object value);

    @Override
    public final Object executeWrite(VirtualFrame frame, Object value) {
        return executeEvaluated(frame, getLevelFrameNode().executeFrame(frame), value);
    }

    @Override
    public abstract ScopeFrameNode getLevelFrameNode();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSWriteScopeFrameSlotNodeGen.create(frameSlot, NodeUtil.cloneNode(getLevelFrameNode()), cloneUninitialized(getRhs()));
    }
}

@NodeChild(value = "rhs", type = JavaScriptNode.class)
abstract class JSWriteCurrentFrameSlotNode extends JSWriteFrameSlotNode {

    protected JSWriteCurrentFrameSlotNode(FrameSlot frameSlot) {
        super(frameSlot);
    }

    @Specialization(guards = "isBooleanKind(frame)")
    protected final boolean doBoolean(VirtualFrame frame, boolean value) {
        frame.setBoolean(frameSlot, value);
        return value;
    }

    @Specialization(guards = "isIntegerKind(frame)")
    protected final int doInteger(VirtualFrame frame, int value) {
        frame.setInt(frameSlot, value);
        return value;
    }

    @Specialization(guards = "isLongKind(frame)")
    protected final int doLargeIntegerInt(VirtualFrame frame, int value) {
        frame.setLong(frameSlot, value);
        return value;
    }

    @Specialization(guards = "isLongKind(frame)")
    protected final LargeInteger doLargeInteger(VirtualFrame frame, LargeInteger value) {
        frame.setLong(frameSlot, value.longValue());
        return value;
    }

    @Specialization(guards = "isDoubleKind(frame)", replaces = {"doInteger", "doLargeInteger", "doLargeIntegerInt"})
    protected final double doDouble(VirtualFrame frame, double value) {
        frame.setDouble(frameSlot, value);
        return value;
    }

    @Specialization(guards = {"ensureObjectKind(frame)"}, replaces = {"doBoolean", "doInteger", "doDouble", "doLargeInteger", "doLargeIntegerInt"})
    protected final Object doObject(VirtualFrame frame, Object value) {
        frame.setObject(frameSlot, value);
        return value;
    }

    @Override
    public final Object executeWithFrame(Frame frame, Object value) {
        return executeEvaluated((VirtualFrame) frame, value);
    }

    abstract Object executeEvaluated(VirtualFrame frame, Object value);

    @Override
    public final Object executeWrite(VirtualFrame frame, Object value) {
        return executeEvaluated(frame, value);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSWriteCurrentFrameSlotNodeGen.create(frameSlot, cloneUninitialized(getRhs()));
    }

    @Override
    public ScopeFrameNode getLevelFrameNode() {
        return ScopeFrameNode.create(0, 0);
    }
}
