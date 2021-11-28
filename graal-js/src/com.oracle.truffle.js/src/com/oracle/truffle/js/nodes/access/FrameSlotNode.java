/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.objects.Dead;

public abstract class FrameSlotNode extends JavaScriptNode {

    protected final FrameSlot frameSlot;

    protected FrameSlotNode(FrameSlot frameSlot) {
        assert frameSlot != null : "Frame slot must not be null";
        this.frameSlot = frameSlot;
    }

    public final FrameSlot getFrameSlot() {
        return frameSlot;
    }

    /**
     * @return the identifier corresponding to this slot
     */
    public final Object getIdentifier() {
        return frameSlot.getIdentifier();
    }

    public abstract ScopeFrameNode getLevelFrameNode();

    protected final boolean getBoolean(Frame frame) {
        return frame.getBoolean(frameSlot);
    }

    protected final int getInt(Frame frame) {
        return frame.getInt(frameSlot);
    }

    protected final double getDouble(Frame frame) {
        return frame.getDouble(frameSlot);
    }

    protected final Object getObject(Frame frame) {
        return frame.getObject(frameSlot);
    }

    protected final long getLong(Frame frame) {
        return frame.getLong(frameSlot);
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

    public abstract static class WithDescriptor extends FrameSlotNode {
        protected final FrameDescriptor frameDescriptor;

        protected WithDescriptor(FrameSlot frameSlot, FrameDescriptor frameDescriptor) {
            super(frameSlot);
            this.frameDescriptor = frameDescriptor;
            assert frameDescriptor != null;
        }

        protected final boolean isBooleanKind(Frame frame) {
            return isOrSetKind(frame, FrameSlotKind.Boolean);
        }

        protected final boolean isIntegerKind(Frame frame) {
            return isOrSetKind(frame, FrameSlotKind.Int);
        }

        protected final boolean isDoubleKind(Frame frame) {
            return isOrSetKind(frame, FrameSlotKind.Double);
        }

        protected final boolean isLongKind(Frame frame) {
            return isOrSetKind(frame, FrameSlotKind.Long);
        }

        protected final boolean isIntegerKind(Frame frame, FrameSlotKind currentKind) {
            return isOrSetKind(frame, currentKind, FrameSlotKind.Int);
        }

        protected final boolean isDoubleKind(Frame frame, FrameSlotKind currentKind) {
            return isOrSetKind(frame, currentKind, FrameSlotKind.Double);
        }

        protected final boolean isLongKind(Frame frame, FrameSlotKind currentKind) {
            return isOrSetKind(frame, currentKind, FrameSlotKind.Long);
        }

        protected final void ensureObjectKind(Frame frame) {
            assert frameDescriptor == frame.getFrameDescriptor();
            if (frameDescriptor.getFrameSlotKind(frameSlot) != FrameSlotKind.Object) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                frameDescriptor.setFrameSlotKind(frameSlot, FrameSlotKind.Object);
            }
        }

        private boolean isOrSetKind(Frame frame, FrameSlotKind targetKind) {
            FrameSlotKind currentKind = frameDescriptor.getFrameSlotKind(frameSlot);
            return isOrSetKind(frame, currentKind, targetKind);
        }

        private boolean isOrSetKind(Frame frame, FrameSlotKind currentKind, FrameSlotKind targetKind) {
            assert frameDescriptor == frame.getFrameDescriptor();
            if (currentKind == targetKind) {
                return true;
            } else if (currentKind == FrameSlotKind.Illegal) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                frameDescriptor.setFrameSlotKind(frameSlot, targetKind);
                return true;
            } else {
                if (targetKind == FrameSlotKind.Double) {
                    if (currentKind == FrameSlotKind.Int || currentKind == FrameSlotKind.Long) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        frameDescriptor.setFrameSlotKind(frameSlot, FrameSlotKind.Double);
                        return true;
                    }
                } else if (targetKind == FrameSlotKind.Long) {
                    if (currentKind == FrameSlotKind.Int) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        frameDescriptor.setFrameSlotKind(frameSlot, FrameSlotKind.Long);
                        return true;
                    }
                }
                return false;
            }
        }
    }
}
