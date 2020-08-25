/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.interop;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.FrameDescriptorProvider;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.objects.Dead;

@ExportLibrary(InteropLibrary.class)
final class ScopeMembers implements TruffleObject {

    private final Frame frame;
    /** FrameBlockScopeNode or RootNode. */
    private final Node blockOrRoot;
    private Object[] members;

    ScopeMembers(Frame frame, /* FrameBlockScopeNode or RootNode */ Node blockOrRoot) {
        this.frame = frame;
        this.blockOrRoot = blockOrRoot;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    Object readArrayElement(long index) throws InvalidArrayIndexException {
        Object[] allMembers = getAllMembers();
        if (0 <= index && index < allMembers.length) {
            return allMembers[(int) index];
        } else {
            throw InvalidArrayIndexException.create(index);
        }
    }

    @ExportMessage
    long getArraySize() {
        return getAllMembers().length;
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
        return 0 <= index && index < getAllMembers().length;
    }

    private Object[] getAllMembers() {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.SLOWPATH_PROBABILITY, members == null)) {
            members = collectAllMembers();
        }
        return members;
    }

    @TruffleBoundary
    private Object[] collectAllMembers() {
        List<Object> membersList = new ArrayList<>();
        if (frame == null) {
            Node descNode = blockOrRoot;
            while (descNode != null) {
                if (!(descNode instanceof FrameDescriptorProvider)) {
                    break;
                }
                FrameDescriptor desc = ((FrameDescriptorProvider) descNode).getFrameDescriptor();
                for (FrameSlot slot : desc.getSlots()) {
                    if (JSFrameUtil.isInternal(slot)) {
                        continue;
                    }
                    membersList.add(new Key(slot.getIdentifier().toString(), descNode));
                }

                descNode = JavaScriptNode.findBlockScopeNode(descNode.getParent());
            }
        } else {
            Node descNode = blockOrRoot;
            Frame outerFrame = frame;
            for (;;) { // frameLevel
                Frame outerScope = outerFrame;
                for (;;) { // scopeLevel
                    for (FrameSlot slot : outerScope.getFrameDescriptor().getSlots()) {
                        if (JSFrameUtil.isInternal(slot)) {
                            continue;
                        }
                        if (isUnsetFrameSlot(outerScope, slot)) {
                            continue;
                        }
                        membersList.add(new Key(slot.getIdentifier().toString(), descNode));
                    }

                    FrameSlot parentSlot = outerScope.getFrameDescriptor().findFrameSlot(ScopeFrameNode.PARENT_SCOPE_IDENTIFIER);
                    if (parentSlot == null) {
                        membersList.add(new Key(ScopeVariables.RECEIVER_MEMBER, descNode));
                        break;
                    }
                    outerScope = (Frame) FrameUtil.getObjectSafe(outerScope, parentSlot);

                    if (descNode != null) {
                        descNode = JavaScriptNode.findBlockScopeNode(descNode.getParent());
                    }
                }

                outerFrame = JSArguments.getEnclosingFrame(outerFrame.getArguments());
                if (outerFrame == JSFrameUtil.NULL_MATERIALIZED_FRAME) {
                    break;
                }
            }
        }
        return membersList.toArray();
    }

    static boolean isUnsetFrameSlot(Frame frame, FrameSlot slot) {
        if (frame != null && frame.isObject(slot)) {
            Object value = FrameUtil.getObjectSafe(frame, slot);
            if (value == null || value == Dead.instance() || value instanceof Frame) {
                return true;
            }
        }
        return false;
    }

    /**
     * Representation of a scope member key with an optional source location.
     */
    @ExportLibrary(InteropLibrary.class)
    static final class Key implements TruffleObject {

        private final String name;
        private final Node blockOrRoot;
        private SourceSection sourceLocation;

        Key(String name, Node blockOrRoot) {
            this.name = name;
            this.blockOrRoot = blockOrRoot;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        boolean isString() {
            return true;
        }

        @ExportMessage
        String asString() {
            return name;
        }

        @ExportMessage
        @TruffleBoundary
        boolean hasSourceLocation() {
            return getSourceLocationImpl().isAvailable();
        }

        @ExportMessage
        @TruffleBoundary
        SourceSection getSourceLocation() throws UnsupportedMessageException {
            if (!hasSourceLocation()) {
                throw UnsupportedMessageException.create();
            }
            return getSourceLocationImpl();
        }

        private SourceSection getSourceLocationImpl() {
            if (sourceLocation == null && blockOrRoot != null) {
                sourceLocation = blockOrRoot.getEncapsulatingSourceSection();
            }
            if (sourceLocation == null) {
                // unavailable source section
                sourceLocation = JSBuiltin.createSourceSection();
            }
            return sourceLocation;
        }
    }
}
