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
    @Child private ScopeFrameNode levelFrameNode;
    private final BranchProfile deadBranch = BranchProfile.create();

    private TemporalDeadZoneCheckNode(FrameSlot frameSlot, ScopeFrameNode levelFrameNode, JavaScriptNode child) {
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
    public ScopeFrameNode getLevelFrameNode() {
        return levelFrameNode;
    }

    @Override
    public boolean hasTemporalDeadZone() {
        return true;
    }

    public static TemporalDeadZoneCheckNode create(FrameSlot frameSlot, ScopeFrameNode levelFrameNode, JavaScriptNode rhs) {
        return new TemporalDeadZoneCheckNode(frameSlot, levelFrameNode, rhs);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new TemporalDeadZoneCheckNode(frameSlot, NodeUtil.cloneNode(levelFrameNode), cloneUninitialized(child));
    }
}
