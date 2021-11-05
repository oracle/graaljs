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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.SafeInteger;

@ImportStatic(FrameSlotKind.class)
public abstract class JSReadFrameSlotNode extends FrameSlotNode implements RepeatableNode, ReadNode {
    JSReadFrameSlotNode(JSFrameSlot slot) {
        super(slot);
    }

    public static JSReadFrameSlotNode create(JSFrameSlot slot, boolean hasTemporalDeadZone) {
        return create(slot, ScopeFrameNode.createCurrent(), hasTemporalDeadZone);
    }

    public static JSReadFrameSlotNode create(JSFrameSlot slot, ScopeFrameNode levelFrameNode, boolean hasTemporalDeadZone) {
        if (!hasTemporalDeadZone && levelFrameNode == ScopeFrameNode.createCurrent()) {
            return JSReadCurrentFrameSlotNodeGen.create(slot);
        }
        if (hasTemporalDeadZone) {
            return JSReadScopeFrameSlotWithTDZNodeGen.create(slot, levelFrameNode);
        } else {
            return JSReadScopeFrameSlotNodeGen.create(slot, levelFrameNode);
        }
    }

    public static JSReadFrameSlotNode create(JSFrameSlot slot) {
        if (JSFrameUtil.hasTemporalDeadZone(slot)) {
            return JSReadScopeFrameSlotWithTDZNodeGen.create(slot, ScopeFrameNode.createCurrent());
        } else {
            return JSReadCurrentFrameSlotNodeGen.create(slot);
        }
    }

    public static JSReadFrameSlotNode create(FrameDescriptor desc, int slotIndex) {
        JSFrameSlot slotInfo = JSFrameSlot.fromIndexedFrameSlot(desc, slotIndex);
        if (JSFrameUtil.hasTemporalDeadZone(desc, slotIndex)) {
            return JSReadScopeFrameSlotWithTDZNodeGen.create(slotInfo, ScopeFrameNode.createCurrent());
        } else {
            return JSReadCurrentFrameSlotNodeGen.create(slotInfo);
        }
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if ((tag == ReadVariableTag.class || tag == StandardTags.ReadVariableTag.class)) {
            if (JSFrameUtil.isInternal(frameSlot)) {
                // Reads to "<this>" are instrumentable
                return JSFrameUtil.isThisSlot(frameSlot);
            }
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        String name = JSFrameUtil.getPublicName(getIdentifier());
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor("name", name);
        descriptor.addProperty(StandardTags.ReadVariableTag.NAME, name);
        return descriptor;
    }

    @Override
    public String expressionToString() {
        Object ident = getIdentifier();
        if (ident instanceof String) {
            return (String) ident;
        }
        return null;
    }
}

abstract class JSReadScopeFrameSlotNode extends JSReadFrameSlotNode {
    @Child @Executed ScopeFrameNode scopeFrameNode;

    JSReadScopeFrameSlotNode(JSFrameSlot slot, ScopeFrameNode scopeFrameNode) {
        super(slot);
        this.scopeFrameNode = scopeFrameNode;
    }

    @Specialization(guards = "levelFrame.isBoolean(frameSlot.getIndex())")
    protected final boolean doBoolean(Frame levelFrame) {
        return super.getBoolean(levelFrame);
    }

    @Specialization(guards = "levelFrame.isInt(frameSlot.getIndex())")
    protected final int doInt(Frame levelFrame) {
        return super.getInt(levelFrame);
    }

    @Specialization(guards = "levelFrame.isDouble(frameSlot.getIndex()) || levelFrame.isInt(frameSlot.getIndex())")
    protected final double doDouble(Frame levelFrame) {
        if (levelFrame.isInt(frameSlot.getIndex())) {
            return super.getInt(levelFrame);
        } else {
            return super.getDouble(levelFrame);
        }
    }

    @Specialization(guards = {"levelFrame.isObject(frameSlot.getIndex())", "!hasTemporalDeadZone()"})
    protected final Object doObject(Frame levelFrame) {
        return super.getObject(levelFrame);
    }

    @Specialization(guards = "levelFrame.isLong(frameSlot.getIndex())")
    protected final SafeInteger doSafeInteger(Frame levelFrame) {
        return SafeInteger.valueOf(super.getLong(levelFrame));
    }

    @Override
    public ScopeFrameNode getLevelFrameNode() {
        return scopeFrameNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSReadScopeFrameSlotNodeGen.create(frameSlot, getLevelFrameNode());
    }
}

abstract class JSReadScopeFrameSlotWithTDZNode extends JSReadScopeFrameSlotNode {
    JSReadScopeFrameSlotWithTDZNode(JSFrameSlot slot, ScopeFrameNode scopeFrameNode) {
        super(slot, scopeFrameNode);
    }

    @Override
    public boolean hasTemporalDeadZone() {
        return true;
    }

    @Specialization(guards = "levelFrame.isObject(frameSlot.getIndex())")
    protected final Object doObjectTDZ(Frame levelFrame,
                    @Cached("create()") BranchProfile deadBranch) {
        return checkNotDead(super.getObject(levelFrame), deadBranch);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSReadScopeFrameSlotWithTDZNodeGen.create(frameSlot, getLevelFrameNode());
    }
}

abstract class JSReadCurrentFrameSlotNode extends JSReadFrameSlotNode {
    JSReadCurrentFrameSlotNode(JSFrameSlot slot) {
        super(slot);
    }

    @Specialization(guards = "frame.isBoolean(frameSlot.getIndex())")
    protected final boolean doBoolean(VirtualFrame frame) {
        return super.getBoolean(frame);
    }

    @Specialization(guards = "frame.isInt(frameSlot.getIndex())")
    protected final int doInt(VirtualFrame frame) {
        return super.getInt(frame);
    }

    @Specialization(guards = "frame.isDouble(frameSlot.getIndex()) || frame.isInt(frameSlot.getIndex())")
    protected final double doDouble(VirtualFrame frame) {
        if (frame.isInt(frameSlot.getIndex())) {
            return super.getInt(frame);
        } else {
            return super.getDouble(frame);
        }
    }

    @Specialization(guards = "frame.isObject(frameSlot.getIndex())")
    protected final Object doObject(VirtualFrame frame) {
        return super.getObject(frame);
    }

    @Specialization(guards = "frame.isLong(frameSlot.getIndex())")
    protected final SafeInteger doSafeInteger(VirtualFrame frame) {
        return SafeInteger.valueOf(super.getLong(frame));
    }

    @Override
    public ScopeFrameNode getLevelFrameNode() {
        return ScopeFrameNode.createCurrent();
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSReadCurrentFrameSlotNodeGen.create(frameSlot);
    }
}
