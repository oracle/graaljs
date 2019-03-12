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
        return isKind(frame, FrameSlotKind.Boolean);
    }

    protected final boolean isIntegerKind(Frame frame) {
        return isKind(frame, FrameSlotKind.Int);
    }

    protected final boolean isDoubleKind(Frame frame) {
        return isKind(frame, FrameSlotKind.Double) || intToDouble(frame);
    }

    protected final boolean isLongKind(Frame frame) {
        return isKind(frame, FrameSlotKind.Long) || intToLong(frame);
    }

    protected final boolean ensureObjectKind(Frame frame) {
        if (frame.getFrameDescriptor().getFrameSlotKind(frameSlot) != FrameSlotKind.Object) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            frame.getFrameDescriptor().setFrameSlotKind(frameSlot, FrameSlotKind.Object);
        }
        return true;
    }

    private boolean isKind(Frame frame, FrameSlotKind kind) {
        return frame.getFrameDescriptor().getFrameSlotKind(frameSlot) == kind || initialSetKind(frame, kind);
    }

    private boolean initialSetKind(Frame frame, FrameSlotKind kind) {
        if (frame.getFrameDescriptor().getFrameSlotKind(frameSlot) == FrameSlotKind.Illegal) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            frame.getFrameDescriptor().setFrameSlotKind(frameSlot, kind);
            return true;
        }
        return false;
    }

    private boolean intToDouble(Frame frame) {
        if (frame.getFrameDescriptor().getFrameSlotKind(frameSlot) == FrameSlotKind.Int || frame.getFrameDescriptor().getFrameSlotKind(frameSlot) == FrameSlotKind.Long) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            frame.getFrameDescriptor().setFrameSlotKind(frameSlot, FrameSlotKind.Double);
            return true;
        }
        return false;
    }

    private boolean intToLong(Frame frame) {
        if (frame.getFrameDescriptor().getFrameSlotKind(frameSlot) == FrameSlotKind.Int) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            frame.getFrameDescriptor().setFrameSlotKind(frameSlot, FrameSlotKind.Long);
            return true;
        }
        return false;
    }

    protected final FrameSlotKind getKind(Frame frame) {
        return frame.getFrameDescriptor().getFrameSlotKind(frameSlot);
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
