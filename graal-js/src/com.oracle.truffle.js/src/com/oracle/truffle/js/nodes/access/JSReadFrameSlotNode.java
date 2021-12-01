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

    protected JSReadFrameSlotNode(int slot, Object identifier) {
        super(slot, identifier);
    }

    public static JSReadFrameSlotNode create(JSFrameSlot slot, boolean hasTemporalDeadZone) {
        return create(slot, ScopeFrameNode.createCurrent(), hasTemporalDeadZone);
    }

    public static JSReadFrameSlotNode create(JSFrameSlot slot, ScopeFrameNode levelFrameNode, boolean hasTemporalDeadZone) {
        assert !hasTemporalDeadZone || JSFrameUtil.hasTemporalDeadZone(slot);
        return create(slot.getIndex(), slot.getIdentifier(), levelFrameNode, hasTemporalDeadZone);
    }

    static JSReadFrameSlotNode create(int slotIndex, Object identifier, ScopeFrameNode levelFrameNode, boolean hasTemporalDeadZone) {
        if (!hasTemporalDeadZone && levelFrameNode == ScopeFrameNode.createCurrent()) {
            return JSReadCurrentFrameSlotNodeGen.create(slotIndex, identifier);
        }
        if (hasTemporalDeadZone) {
            return JSReadScopeFrameSlotWithTDZNodeGen.create(slotIndex, identifier, levelFrameNode);
        } else {
            return JSReadScopeFrameSlotNodeGen.create(slotIndex, identifier, levelFrameNode);
        }
    }

    public static JSReadFrameSlotNode create(JSFrameSlot slot) {
        if (JSFrameUtil.hasTemporalDeadZone(slot)) {
            return JSReadScopeFrameSlotWithTDZNodeGen.create(slot.getIndex(), slot.getIdentifier(), ScopeFrameNode.createCurrent());
        } else {
            return JSReadCurrentFrameSlotNodeGen.create(slot.getIndex(), slot.getIdentifier());
        }
    }

    public static JSReadFrameSlotNode create(FrameDescriptor desc, int slotIndex) {
        return create(JSFrameSlot.fromIndexedFrameSlot(desc, slotIndex));
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if ((tag == ReadVariableTag.class || tag == StandardTags.ReadVariableTag.class)) {
            if (JSFrameUtil.isInternalIdentifier(getIdentifier())) {
                // Reads to "<this>" are instrumentable
                return JSFrameUtil.isThisSlotIdentifier(getIdentifier());
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

    JSReadScopeFrameSlotNode(int slot, Object identifier, ScopeFrameNode scopeFrameNode) {
        super(slot, identifier);
        this.scopeFrameNode = scopeFrameNode;
    }

    @Specialization(guards = "levelFrame.isBoolean(slot)")
    protected final boolean doBoolean(Frame levelFrame) {
        return levelFrame.getBoolean(slot);
    }

    @Specialization(guards = "levelFrame.isInt(slot)")
    protected final int doInt(Frame levelFrame) {
        return levelFrame.getInt(slot);
    }

    @Specialization(guards = "levelFrame.isDouble(slot) || levelFrame.isInt(slot)")
    protected final double doDouble(Frame levelFrame) {
        if (levelFrame.isInt(slot)) {
            return levelFrame.getInt(slot);
        } else {
            return levelFrame.getDouble(slot);
        }
    }

    @Specialization(guards = {"levelFrame.isObject(slot)", "!hasTemporalDeadZone()"})
    protected final Object doObject(Frame levelFrame) {
        return levelFrame.getObject(slot);
    }

    @Specialization(guards = "levelFrame.isLong(slot)")
    protected final SafeInteger doSafeInteger(Frame levelFrame) {
        return SafeInteger.valueOf(levelFrame.getLong(slot));
    }

    @Override
    public ScopeFrameNode getLevelFrameNode() {
        return scopeFrameNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSReadScopeFrameSlotNodeGen.create(getSlotIndex(), getIdentifier(), getLevelFrameNode());
    }
}

abstract class JSReadScopeFrameSlotWithTDZNode extends JSReadScopeFrameSlotNode {

    JSReadScopeFrameSlotWithTDZNode(int slot, Object identifier, ScopeFrameNode scopeFrameNode) {
        super(slot, identifier, scopeFrameNode);
    }

    @Override
    public boolean hasTemporalDeadZone() {
        return true;
    }

    @Specialization(guards = "levelFrame.isObject(slot)")
    protected final Object doObjectTDZ(Frame levelFrame,
                    @Cached("create()") BranchProfile deadBranch) {
        return checkNotDead(levelFrame.getObject(slot), deadBranch);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSReadScopeFrameSlotWithTDZNodeGen.create(getSlotIndex(), getIdentifier(), getLevelFrameNode());
    }
}

abstract class JSReadCurrentFrameSlotNode extends JSReadFrameSlotNode {

    JSReadCurrentFrameSlotNode(int slot, Object identifier) {
        super(slot, identifier);
    }

    @Specialization(guards = "frame.isBoolean(slot)")
    protected final boolean doBoolean(VirtualFrame frame) {
        return frame.getBoolean(slot);
    }

    @Specialization(guards = "frame.isInt(slot)")
    protected final int doInt(VirtualFrame frame) {
        return frame.getInt(slot);
    }

    @Specialization(guards = "frame.isDouble(slot) || frame.isInt(slot)")
    protected final double doDouble(VirtualFrame frame) {
        if (frame.isInt(slot)) {
            return frame.getInt(slot);
        } else {
            return frame.getDouble(slot);
        }
    }

    @Specialization(guards = "frame.isObject(slot)")
    protected final Object doObject(VirtualFrame frame) {
        return frame.getObject(slot);
    }

    @Specialization(guards = "frame.isLong(slot)")
    protected final SafeInteger doSafeInteger(VirtualFrame frame) {
        return SafeInteger.valueOf(frame.getLong(slot));
    }

    @Override
    public ScopeFrameNode getLevelFrameNode() {
        return ScopeFrameNode.createCurrent();
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSReadCurrentFrameSlotNodeGen.create(getSlotIndex(), getIdentifier());
    }
}
