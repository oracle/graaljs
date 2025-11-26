/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.BlockNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.FrameDescriptorProvider;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.function.BlockScopeNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Dead;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

@ExportLibrary(InteropLibrary.class)
final class ScopeMembers implements TruffleObject {

    private final Frame frame;
    /** BlockScopeNode or RootNode. */
    private final Node blockOrRoot;
    private final Frame functionFrame;
    private Object[] members;

    ScopeMembers(Frame frame, Node blockOrRoot, Frame functionFrame) {
        assert ScopeVariables.isBlockScopeOrRootNode(blockOrRoot);
        this.frame = frame;
        this.blockOrRoot = blockOrRoot;
        this.functionFrame = functionFrame;
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
            collectMembersWithoutFrame(membersList, blockOrRoot);
        } else {
            // Traverse block scope nodes in order to discover scope members from inner to outer.
            // Once we've reached the local function root node, we add any non-internal
            // non-hoisted slots that we have not seen yet, including `this`.
            // Then, we go through all closure scope frames. These only have root location nodes.

            class SlotVisitor {
                Node descNode = blockOrRoot;
                int parentSlot = -1;
                boolean seenThis;

                public void accept(FrameDescriptor frameDescriptor, int slot, Frame targetFrame) {
                    assert targetFrame.getFrameDescriptor() == frameDescriptor;
                    Object slotName = frameDescriptor.getSlotName(slot);
                    if (ScopeFrameNode.PARENT_SCOPE_IDENTIFIER.equals(slotName)) {
                        parentSlot = slot;
                    } else if (ScopeFrameNode.EVAL_SCOPE_IDENTIFIER.equals(slotName)) {
                        JSDynamicObject evalScope = (JSDynamicObject) targetFrame.getObject(slot);
                        for (Object key : DynamicObject.GetKeyArrayNode.getUncached().execute(evalScope)) {
                            if (key instanceof TruffleString name) {
                                membersList.add(new Key(name, descNode));
                            }
                        }
                    } else if (JSFrameUtil.isThisSlot(frameDescriptor, slot)) {
                        membersList.add(new Key(ScopeVariables.RECEIVER_MEMBER, descNode, slot));
                        seenThis = true;
                    } else if (!JSFrameUtil.isInternal(frameDescriptor, slot)) {
                        assert slotName instanceof TruffleString;
                        if (!isUnsetFrameSlot(targetFrame, slot)) {
                            membersList.add(new Key((TruffleString) slotName, descNode, slot));
                        }
                    }
                }
            }

            SlotVisitor visitor = new SlotVisitor();
            Frame outerFrame = frame;
            if (functionFrame != null) {
                // traverse local frames
                FrameDescriptor rootFrameDescriptor = functionFrame.getFrameDescriptor();
                while (visitor.descNode instanceof BlockScopeNode) {
                    BlockScopeNode block = (BlockScopeNode) visitor.descNode;
                    visitor.parentSlot = -1;

                    if (block instanceof BlockScopeNode.FrameBlockScopeNode) {
                        FrameDescriptor blockFrameDescriptor = ((BlockScopeNode.FrameBlockScopeNode) block).getFrameDescriptor();
                        if (outerFrame.getFrameDescriptor() == blockFrameDescriptor) {
                            for (int i = 0; i < blockFrameDescriptor.getNumberOfSlots(); i++) {
                                visitor.accept(blockFrameDescriptor, i, outerFrame);
                            }
                        }
                    }
                    for (int i = block.getFrameStart(); i < block.getFrameEnd(); i++) {
                        visitor.accept(rootFrameDescriptor, i, functionFrame);
                    }

                    visitor.descNode = JavaScriptNode.findBlockScopeNode(visitor.descNode.getParent());
                    if (visitor.parentSlot >= 0) {
                        Object parent = outerFrame.getObject(visitor.parentSlot);
                        if (parent instanceof Frame) {
                            outerFrame = (Frame) parent;
                            assert outerFrame != JSFrameUtil.NULL_MATERIALIZED_FRAME;
                        } else {
                            break;
                        }
                    }
                }

                assert functionFrame.getFrameDescriptor() == rootFrameDescriptor && visitor.descNode instanceof RootNode;
                for (int slot = 0; slot < rootFrameDescriptor.getNumberOfSlots(); slot++) {
                    // skip hoisted block-scoped slots; only accessible within their block
                    if (JSFrameUtil.isHoistedFromBlock(rootFrameDescriptor, slot)) {
                        continue;
                    }
                    visitor.accept(rootFrameDescriptor, slot, functionFrame);
                }
                if (!visitor.seenThis) {
                    membersList.add(new Key(ScopeVariables.RECEIVER_MEMBER, visitor.descNode));
                }
                outerFrame = JSArguments.getEnclosingFrame(frame.getArguments());
            }

            // traverse non-local frames
            while (outerFrame != JSFrameUtil.NULL_MATERIALIZED_FRAME) {
                visitor.descNode = JSFunction.getFunctionData(JSFrameUtil.getFunctionObject(outerFrame)).getRootNode();
                visitor.seenThis = false;
                for (;;) {
                    visitor.parentSlot = -1;
                    for (int slot = 0; slot < outerFrame.getFrameDescriptor().getNumberOfSlots(); slot++) {
                        visitor.accept(outerFrame.getFrameDescriptor(), slot, outerFrame);
                    }
                    if (visitor.parentSlot >= 0) {
                        Object parent = outerFrame.getObject(visitor.parentSlot);
                        if (parent instanceof Frame) {
                            outerFrame = (Frame) parent;
                            assert outerFrame != JSFrameUtil.NULL_MATERIALIZED_FRAME;
                            continue;
                        }
                    }
                    break;
                }
                if (!visitor.seenThis) {
                    membersList.add(new Key(ScopeVariables.RECEIVER_MEMBER, visitor.descNode));
                }

                outerFrame = JSArguments.getEnclosingFrame(outerFrame.getArguments());
            }
        }
        return membersList.toArray();
    }

    private static void collectMembersWithoutFrame(List<Object> membersList, Node blockOrRootNode) {
        Node descNode = blockOrRootNode;
        while (descNode != null) {
            if (!(descNode instanceof FrameDescriptorProvider)) {
                break;
            }
            FrameDescriptor desc = ((FrameDescriptorProvider) descNode).getFrameDescriptor();
            for (int slot = 0; slot < desc.getNumberOfSlots(); slot++) {
                if (JSFrameUtil.isInternal(desc, slot)) {
                    continue;
                }
                Object slotName = desc.getSlotName(slot);
                membersList.add(new Key((TruffleString) slotName, descNode, slot));
            }

            descNode = JavaScriptNode.findBlockScopeNode(descNode.getParent());
        }
    }

    static boolean isUnsetFrameSlot(Frame frame, int slot) {
        if (frame != null) {
            byte tag = frame.getTag(slot);
            if (tag == FrameSlotKind.Illegal.tag) {
                return true;
            } else if (tag == FrameSlotKind.Object.tag) {
                Object value = frame.getObject(slot);
                if (value == null || value == Dead.instance() || value instanceof Frame) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Representation of a scope member key with an optional source location.
     */
    @ExportLibrary(InteropLibrary.class)
    static final class Key implements TruffleObject {

        private final TruffleString name;
        private final Node blockOrRoot;
        private final int slot;
        private SourceSection sourceLocation;

        Key(TruffleString name, Node blockOrRoot) {
            this(name, blockOrRoot, -1);
        }

        Key(TruffleString name, Node blockOrRoot, int slot) {
            this.name = name;
            this.slot = slot;
            this.blockOrRoot = blockOrRoot;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        boolean isString() {
            return true;
        }

        @ExportMessage
        TruffleString asTruffleString() {
            return name;
        }

        @ExportMessage
        String asString() {
            return Strings.toJavaString(name);
        }

        @Override
        public String toString() {
            return asString();
        }

        @ExportMessage
        @TruffleBoundary
        boolean hasSourceLocation() {
            return getOrFindSourceLocation().isAvailable();
        }

        @ExportMessage
        @TruffleBoundary
        SourceSection getSourceLocation() throws UnsupportedMessageException {
            if (!hasSourceLocation()) {
                throw UnsupportedMessageException.create();
            }
            return sourceLocation;
        }

        private SourceSection getOrFindSourceLocation() {
            CompilerAsserts.neverPartOfCompilation();
            if (sourceLocation == null && blockOrRoot != null) {
                sourceLocation = findSourceLocation();
            }
            if (sourceLocation == null) {
                // unavailable source section
                sourceLocation = JSBuiltin.createSourceSection();
            }
            return sourceLocation;
        }

        private SourceSection findSourceLocation() {
            if (hasSlot()) {
                class DeclarationFinder implements NodeVisitor {
                    JavaScriptNode found;

                    @Override
                    public boolean visit(Node node) {
                        if (node instanceof JavaScriptNode) {
                            if (node instanceof JSWriteFrameSlotNode) {
                                JSWriteFrameSlotNode write = (JSWriteFrameSlotNode) node;
                                if (write.getSlotIndex() == slot && write.hasSourceSection()) {
                                    found = write;
                                    return false;
                                }
                            }
                            return true;
                        } else if (node == blockOrRoot) {
                            return true;
                        } else if (node instanceof BlockNode) {
                            return true;
                        } else if (node instanceof ScopeFrameNode) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }

                DeclarationFinder finder = new DeclarationFinder();
                blockOrRoot.accept(finder);
                if (finder.found != null) {
                    return finder.found.getSourceSection();
                }
            }
            return blockOrRoot.getEncapsulatingSourceSection();
        }

        private boolean hasSlot() {
            return slot >= 0;
        }
    }
}
