/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.objects.Dead;

@SuppressWarnings("unused")
public abstract class FrameSlotNode extends JavaScriptNode {

    @CompilationFinal protected FrameSlot frameSlot;

    protected FrameSlotNode(FrameSlot frameSlot) {
        assert frameSlot != null : "Frame slot must not be null";
        this.frameSlot = frameSlot;
    }

    public final FrameSlot getFrameSlot() {
        return frameSlot;
    }

    public final void setFrameSlot(FrameSlot frameSlot) {
        assert frameSlot != null;
        this.frameSlot = frameSlot;
    }

    /**
     * @return the identifier corresponding to this slot
     */
    public final Object getIdentifier() {
        return frameSlot.getIdentifier();
    }

    public abstract ScopeFrameNode getLevelFrameNode();

    protected final boolean getBoolean(Frame frame) {
        return FrameUtil.getBooleanSafe(frame, frameSlot);
    }

    protected final int getInt(Frame frame) {
        return FrameUtil.getIntSafe(frame, frameSlot);
    }

    protected final double getDouble(Frame frame) {
        return FrameUtil.getDoubleSafe(frame, frameSlot);
    }

    protected final Object getObject(Frame frame) {
        return FrameUtil.getObjectSafe(frame, frameSlot);
    }

    protected final long getLong(Frame frame) {
        return FrameUtil.getLongSafe(frame, frameSlot);
    }

    protected final boolean isBoolean(Frame frame) {
        return frame.isBoolean(frameSlot);
    }

    protected final boolean isInt(Frame frame) {
        return frame.isInt(frameSlot);
    }

    protected final boolean isDouble(Frame frame) {
        return frame.isDouble(frameSlot);
    }

    protected final boolean isObject(Frame frame) {
        return frame.isObject(frameSlot);
    }

    protected final boolean isLong(Frame frame) {
        return frame.isLong(frameSlot);
    }

    protected final boolean isBooleanKind(Frame frame) {
        return isKind(FrameSlotKind.Boolean);
    }

    protected final boolean isIntegerKind(Frame frame) {
        return isKind(FrameSlotKind.Int);
    }

    protected final boolean isDoubleKind(Frame frame) {
        return isKind(FrameSlotKind.Double) || intToDouble();
    }

    protected final boolean isLongKind(Frame frame) {
        return isKind(FrameSlotKind.Long) || intToLong();
    }

    protected final boolean ensureObjectKind(Frame frame) {
        if (frameSlot.getKind() != FrameSlotKind.Object) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            frameSlot.setKind(FrameSlotKind.Object);
        }
        return true;
    }

    private boolean isKind(FrameSlotKind kind) {
        return frameSlot.getKind() == kind || initialSetKind(kind);
    }

    private boolean initialSetKind(FrameSlotKind kind) {
        if (frameSlot.getKind() == FrameSlotKind.Illegal) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            frameSlot.setKind(kind);
            return true;
        }
        return false;
    }

    private boolean intToDouble() {
        if (frameSlot.getKind() == FrameSlotKind.Int || frameSlot.getKind() == FrameSlotKind.Long) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            frameSlot.setKind(FrameSlotKind.Double);
            return true;
        }
        return false;
    }

    private boolean intToLong() {
        if (frameSlot.getKind() == FrameSlotKind.Int) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            frameSlot.setKind(FrameSlotKind.Long);
            return true;
        }
        return false;
    }

    protected final FrameSlotKind getKind(Frame frame) {
        return frameSlot.getKind();
    }

    public boolean hasTemporalDeadZone() {
        return false;
    }

    protected final Object checkNotDead(Object value, BranchProfile deadBranch) {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.SLOWPATH_PROBABILITY, value == Dead.instance())) {
            deadBranch.enter();
            throw Errors.createReferenceErrorNotDefined(frameSlot.getIdentifier(), this);
        }
        return value;
    }
}
