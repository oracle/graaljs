/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.objects.Undefined;

public interface ResumableNode {

    @SuppressWarnings("unused")
    default Object resume(VirtualFrame frame, int stateSlot) {
        throw CompilerDirectives.shouldNotReachHere();
    }

    static JavaScriptNode createResumableNode(ResumableNode node, int stateSlot) {
        assert !(node instanceof SuspendNode) : node;
        JavaScriptNode original = (JavaScriptNode) node;
        JavaScriptNode wrappedNode = GeneratorWrapperNode.createWrapper(original, stateSlot);
        JavaScriptNode.transferSourceSectionAndTags(original, wrappedNode);
        return wrappedNode;
    }

    default void resetState(VirtualFrame frame, int stateSlot) {
        frame.setObject(stateSlot, Undefined.instance);
    }

    default FrameSlotKind getStateSlotKind() {
        return FrameSlotKind.Illegal;
    }

    interface WithObjectState extends ResumableNode {
        default void setState(VirtualFrame frame, int stateSlot, Object state) {
            assert frame.getFrameDescriptor().getSlotKind(stateSlot) == FrameSlotKind.Object;
            frame.setObject(stateSlot, state);
        }

        default Object getState(VirtualFrame frame, int stateSlot) {
            assert frame.getFrameDescriptor().getSlotKind(stateSlot) == FrameSlotKind.Object;
            return frame.getObject(stateSlot);
        }

        default Object getStateAndReset(VirtualFrame frame, int stateSlot) {
            Object state = getState(frame, stateSlot);
            resetState(frame, stateSlot);
            return state;
        }

        @Override
        default FrameSlotKind getStateSlotKind() {
            return FrameSlotKind.Object;
        }
    }

    interface WithIntState extends ResumableNode {
        default void setStateAsInt(VirtualFrame frame, int stateSlot, int state) {
            assert frame.getFrameDescriptor().getSlotKind(stateSlot) == FrameSlotKind.Int;
            frame.setInt(stateSlot, state);
        }

        default int getStateAsInt(VirtualFrame frame, int stateSlot) {
            assert frame.getFrameDescriptor().getSlotKind(stateSlot) == FrameSlotKind.Int;
            if (frame.isInt(stateSlot)) {
                return frame.getInt(stateSlot);
            } else {
                assert frame.isObject(stateSlot) && frame.getObject(stateSlot) == Undefined.instance;
                return 0;
            }
        }

        default int getStateAsIntAndReset(VirtualFrame frame, int stateSlot) {
            int state = getStateAsInt(frame, stateSlot);
            resetState(frame, stateSlot);
            return state;
        }

        @Override
        default FrameSlotKind getStateSlotKind() {
            return FrameSlotKind.Int;
        }
    }
}
