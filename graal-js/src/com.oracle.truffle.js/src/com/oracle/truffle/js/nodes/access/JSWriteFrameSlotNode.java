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

import java.util.Set;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.SafeInteger;

public abstract class JSWriteFrameSlotNode extends FrameSlotNode.WithDescriptor implements WriteNode {
    protected JSWriteFrameSlotNode(JSFrameSlot frameSlot) {
        super(frameSlot);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == WriteVariableTag.class || tag == StandardTags.WriteVariableTag.class) {
            return !JSFrameUtil.isInternal(frameSlot);
        } else if (tag == JSTags.InputNodeTag.class) {
            return !JSFrameUtil.isInternal(frameSlot);
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        String name = JSFrameUtil.getPublicName(getIdentifier());
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor("name", name);
        descriptor.addProperty(StandardTags.WriteVariableTag.NAME, name);
        return descriptor;
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(WriteVariableTag.class) || materializedTags.contains(StandardTags.WriteVariableTag.class)) {
            if (getRhs() != null && !getRhs().hasSourceSection() && this.hasSourceSection()) {
                transferSourceSectionAddExpressionTag(this, getRhs());
            }
        }
        return this;
    }

    @Override
    public abstract JavaScriptNode getRhs();

    public abstract Object executeWithFrame(Frame frame, Object value);

    public static JSWriteFrameSlotNode create(JSFrameSlot frameSlot, JavaScriptNode rhs, boolean hasTemporalDeadZone) {
        if (!hasTemporalDeadZone) {
            return JSWriteCurrentFrameSlotNodeGen.create(frameSlot, rhs);
        }
        return create(frameSlot, ScopeFrameNode.createCurrent(), rhs, hasTemporalDeadZone);
    }

    public static JSWriteFrameSlotNode create(JSFrameSlot frameSlot, ScopeFrameNode scopeFrameNode, JavaScriptNode rhs, boolean hasTemporalDeadZone) {
        if (!hasTemporalDeadZone && scopeFrameNode == ScopeFrameNode.createCurrent()) {
            return JSWriteCurrentFrameSlotNodeGen.create(frameSlot, rhs);
        }
        return JSWriteScopeFrameSlotNodeGen.create(frameSlot, scopeFrameNode, hasTemporalDeadZone ? TemporalDeadZoneCheckNode.create(frameSlot, scopeFrameNode, rhs) : rhs);
    }

    @Override
    public final boolean isResultAlwaysOfType(Class<?> clazz) {
        return getRhs().isResultAlwaysOfType(clazz);
    }
}

abstract class JSWriteScopeFrameSlotNode extends JSWriteFrameSlotNode {
    @Child @Executed ScopeFrameNode scopeFrameNode;
    @Child @Executed JavaScriptNode rhsNode;

    protected JSWriteScopeFrameSlotNode(JSFrameSlot frameSlot, ScopeFrameNode scopeFrameNode, JavaScriptNode rhsNode) {
        super(frameSlot);
        this.scopeFrameNode = scopeFrameNode;
        this.rhsNode = rhsNode;
    }

    @Specialization(guards = "isBooleanKind(levelFrame)")
    protected final boolean doBoolean(Frame levelFrame, boolean value) {
        levelFrame.setBoolean(slot, value);
        return value;
    }

    @Specialization(guards = "(isIntegerKind(frame, kind) || isLongKind(frame, kind)) || isDoubleKind(frame, kind)")
    protected final int doInteger(Frame frame, int value,
                    @Bind("getFrameDescriptor(frame).getSlotKind(slot)") FrameSlotKind kind) {
        if (isIntegerKind(frame, kind)) {
            frame.setInt(slot, value);
        } else if (isLongKind(frame, kind)) {
            frame.setLong(slot, value);
        } else if (isDoubleKind(frame, kind)) {
            frame.setDouble(slot, value);
        }
        return value;
    }

    @Specialization(guards = "isLongKind(levelFrame)")
    protected final SafeInteger doSafeInteger(Frame levelFrame, SafeInteger value) {
        levelFrame.setLong(slot, value.longValue());
        return value;
    }

    @Specialization
    protected final long doLong(Frame levelFrame, long value) {
        ensureObjectKind(levelFrame);
        levelFrame.setObject(slot, value);
        return value;
    }

    @Specialization(guards = "isDoubleKind(levelFrame)", replaces = {"doInteger", "doSafeInteger"})
    protected final double doDouble(Frame levelFrame, double value) {
        levelFrame.setDouble(slot, value);
        return value;
    }

    @Specialization(replaces = {"doBoolean", "doInteger", "doDouble", "doSafeInteger", "doLong"})
    protected final Object doObject(Frame levelFrame, Object value) {
        ensureObjectKind(levelFrame);
        levelFrame.setObject(slot, value);
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
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSWriteScopeFrameSlotNodeGen.create(frameSlot, getLevelFrameNode(), cloneUninitialized(getRhs(), materializedTags));
    }
}

abstract class JSWriteCurrentFrameSlotNode extends JSWriteFrameSlotNode {
    @Child @Executed JavaScriptNode rhsNode;

    protected JSWriteCurrentFrameSlotNode(JSFrameSlot frameSlot, JavaScriptNode rhsNode) {
        super(frameSlot);
        this.rhsNode = rhsNode;
    }

    @Specialization(guards = "isBooleanKind(frame)")
    protected final boolean doBoolean(VirtualFrame frame, boolean value) {
        frame.setBoolean(slot, value);
        return value;
    }

    @Specialization(guards = "(isIntegerKind(frame, kind) || isLongKind(frame, kind)) || isDoubleKind(frame, kind)")
    protected final int doInteger(VirtualFrame frame, int value,
                    @Bind("getFrameDescriptor(frame).getSlotKind(slot)") FrameSlotKind kind) {
        if (isIntegerKind(frame, kind)) {
            frame.setInt(slot, value);
        } else if (isLongKind(frame, kind)) {
            frame.setLong(slot, value);
        } else if (isDoubleKind(frame, kind)) {
            frame.setDouble(slot, value);
        }
        return value;
    }

    @Specialization(guards = "isLongKind(frame)")
    protected final SafeInteger doSafeInteger(VirtualFrame frame, SafeInteger value) {
        frame.setLong(slot, value.longValue());
        return value;
    }

    @Specialization
    protected final long doLong(VirtualFrame frame, long value) {
        ensureObjectKind(frame);
        frame.setObject(slot, value);
        return value;
    }

    @Specialization(guards = "isDoubleKind(frame)", replaces = {"doInteger", "doSafeInteger"})
    protected final double doDouble(VirtualFrame frame, double value) {
        frame.setDouble(slot, value);
        return value;
    }

    @Specialization(replaces = {"doBoolean", "doInteger", "doDouble", "doSafeInteger", "doLong"})
    protected final Object doObject(VirtualFrame frame, Object value) {
        ensureObjectKind(frame);
        frame.setObject(slot, value);
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
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSWriteCurrentFrameSlotNodeGen.create(frameSlot, cloneUninitialized(getRhs(), materializedTags));
    }

    @Override
    public ScopeFrameNode getLevelFrameNode() {
        return ScopeFrameNode.createCurrent();
    }
}
