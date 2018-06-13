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

import java.util.Set;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;
import com.oracle.truffle.js.runtime.LargeInteger;

public abstract class JSWriteFrameSlotNode extends FrameSlotNode implements WriteNode {
    protected JSWriteFrameSlotNode(FrameSlot frameSlot) {
        super(frameSlot);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == WriteVariableExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("name", getIdentifier());
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(WriteVariableExpressionTag.class)) {
            if (!getRhs().hasSourceSection() && this.hasSourceSection()) {
                transferSourceSectionNoTags(this, getRhs());
            }
        }
        return this;
    }

    @Override
    public abstract JavaScriptNode getRhs();

    public abstract Object executeWithFrame(Frame frame, Object value);

    public static JSWriteFrameSlotNode create(FrameSlot frameSlot, int frameLevel, int scopeLevel, FrameSlot[] parentSlots, JavaScriptNode rhs, boolean hasTemporalDeadZone) {
        if (frameLevel == 0 && scopeLevel == 0 && !hasTemporalDeadZone) {
            return JSWriteCurrentFrameSlotNodeGen.create(frameSlot, rhs);
        }
        return create(frameSlot, ScopeFrameNode.create(frameLevel, scopeLevel, parentSlots), rhs, hasTemporalDeadZone);
    }

    public static JSWriteFrameSlotNode create(FrameSlot frameSlot, ScopeFrameNode levelFrameNode, JavaScriptNode rhs, boolean hasTemporalDeadZone) {
        return JSWriteScopeFrameSlotNodeGen.create(frameSlot, levelFrameNode, hasTemporalDeadZone ? TemporalDeadZoneCheckNode.create(frameSlot, levelFrameNode, rhs) : rhs);
    }

    @Override
    public final boolean isResultAlwaysOfType(Class<?> clazz) {
        return getRhs().isResultAlwaysOfType(clazz);
    }
}

abstract class JSWriteScopeFrameSlotNode extends JSWriteFrameSlotNode {
    @Child @Executed ScopeFrameNode scopeFrameNode;
    @Child @Executed JavaScriptNode rhsNode;

    protected JSWriteScopeFrameSlotNode(FrameSlot frameSlot, ScopeFrameNode scopeFrameNode, JavaScriptNode rhsNode) {
        super(frameSlot);
        this.scopeFrameNode = scopeFrameNode;
        this.rhsNode = rhsNode;
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
    public ScopeFrameNode getLevelFrameNode() {
        return scopeFrameNode;
    }

    @Override
    public JavaScriptNode getRhs() {
        return rhsNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSWriteScopeFrameSlotNodeGen.create(frameSlot, NodeUtil.cloneNode(getLevelFrameNode()), cloneUninitialized(getRhs()));
    }
}

abstract class JSWriteCurrentFrameSlotNode extends JSWriteFrameSlotNode {
    @Child @Executed JavaScriptNode rhsNode;

    protected JSWriteCurrentFrameSlotNode(FrameSlot frameSlot, JavaScriptNode rhsNode) {
        super(frameSlot);
        this.rhsNode = rhsNode;
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
    public JavaScriptNode getRhs() {
        return rhsNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSWriteCurrentFrameSlotNodeGen.create(frameSlot, cloneUninitialized(getRhs()));
    }

    @Override
    public ScopeFrameNode getLevelFrameNode() {
        return ScopeFrameNode.create(0);
    }
}
