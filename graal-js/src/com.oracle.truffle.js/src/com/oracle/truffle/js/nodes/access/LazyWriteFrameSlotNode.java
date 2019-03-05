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

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSFrameUtil;

public abstract class LazyWriteFrameSlotNode extends JavaScriptNode implements WriteNode {
    protected final Object identifier;
    @Child protected JavaScriptNode rhs;

    public LazyWriteFrameSlotNode(Object identifier, JavaScriptNode rhs) {
        this.identifier = identifier;
        this.rhs = rhs;
    }

    public Object getIdentifier() {
        return identifier;
    }

    public static LazyWriteFrameSlotNode create(Object identifier, JavaScriptNode rhs) {
        return new LazyWriteFrameSlotUninitNode(identifier, rhs);
    }

    @Override
    public JavaScriptNode getRhs() {
        return rhs;
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        return executeWrite(frame, rhs.execute(frame));
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class LazyWriteFrameSlotUninitNode extends LazyWriteFrameSlotNode {
        LazyWriteFrameSlotUninitNode(Object identifier, JavaScriptNode rhs) {
            super(identifier, rhs);
        }

        @Override
        public Object executeWrite(VirtualFrame frame, Object value) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            Frame outerFrame = frame;
            for (int frameLevel = 0;; frameLevel++) {
                Frame outerScope = outerFrame;
                List<FrameSlot> parentSlotList = new ArrayList<>();
                for (int scopeLevel = 0;; scopeLevel++) {
                    FrameSlot slot = outerScope.getFrameDescriptor().findFrameSlot(identifier);
                    if (slot != null) {
                        FrameSlot[] parentSlots = parentSlotList.toArray(ScopeFrameNode.EMPTY_FRAME_SLOT_ARRAY);
                        JSWriteFrameSlotNode resolved = JSWriteFrameSlotNode.create(slot, ScopeFrameNode.create(frameLevel, scopeLevel, parentSlots), rhs, JSFrameUtil.hasTemporalDeadZone(slot));
                        return this.replace(resolved).executeWrite(frame, value);
                    }

                    FrameSlot parentSlot = outerScope.getFrameDescriptor().findFrameSlot(ScopeFrameNode.PARENT_SCOPE_IDENTIFIER);
                    if (parentSlot == null) {
                        break;
                    }
                    outerScope = (Frame) FrameUtil.getObjectSafe(outerScope, parentSlot);
                    parentSlotList.add(parentSlot);
                }

                outerFrame = JSArguments.getEnclosingFrame(outerFrame.getArguments());
                if (outerFrame == JSFrameUtil.NULL_MATERIALIZED_FRAME) {
                    break;
                }
            }

            throw new RuntimeException("frame slot not found");
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new LazyWriteFrameSlotUninitNode(identifier, cloneUninitialized(rhs));
        }
    }
}
