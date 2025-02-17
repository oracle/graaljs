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

import java.util.OptionalInt;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.FrameDescriptorProvider;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.access.WriteNode;
import com.oracle.truffle.js.nodes.function.BlockScopeNode;
import com.oracle.truffle.js.nodes.module.ReadImportBindingNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

@ExportLibrary(InteropLibrary.class)
public final class ScopeVariables implements TruffleObject {

    public static final TruffleString RECEIVER_MEMBER = Strings.THIS;
    static final int LIMIT = 4;

    final Frame frame;
    final boolean nodeEnter;
    /** BlockScopeNode or RootNode. */
    final Node blockOrRoot;
    final Frame functionFrame;
    private ScopeMembers members;

    /**
     * @param frame Block scope or function frame
     * @param nodeEnter True if we are entering the node
     * @param blockOrRoot BlockScopeNode or FunctionRootNode
     * @param functionFrame Optional function frame not accessible via parent chain
     */
    private ScopeVariables(Frame frame, boolean nodeEnter, Node blockOrRoot, Frame functionFrame) {
        assert isBlockScopeOrRootNode(blockOrRoot);
        this.frame = frame;
        this.nodeEnter = nodeEnter;
        this.blockOrRoot = blockOrRoot;
        this.functionFrame = functionFrame;
    }

    static boolean isBlockScopeOrRootNode(Node blockOrRoot) {
        return blockOrRoot instanceof BlockScopeNode || blockOrRoot instanceof RootNode;
    }

    public static ScopeVariables create(Frame frame, boolean nodeEnter, Node blockOrRoot, Frame functionFrame) {
        return new ScopeVariables(frame, nodeEnter, blockOrRoot, functionFrame);
    }

    /**
     * Assure that we fix the current node and enter flag.
     */
    @ExportMessage
    boolean accepts(@Cached(value = "this.blockOrRoot", adopt = false) Node cachedNode,
                    @Cached(value = "this.nodeEnter", neverDefault = false) boolean cachedNodeEnter) {
        return this.blockOrRoot == cachedNode && this.nodeEnter == cachedNodeEnter;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    boolean isScope() {
        return true;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    Class<? extends TruffleLanguage<?>> getLanguage() {
        return JavaScriptLanguage.class;
    }

    @ExportMessage
    @TruffleBoundary
    boolean hasScopeParent() {
        if (blockOrRoot instanceof BlockScopeNode) {
            BlockScopeNode blockScopeNode = (BlockScopeNode) blockOrRoot;
            Node parentBlock;
            while ((parentBlock = JavaScriptNode.findBlockScopeNode(blockScopeNode.getParent())) != null) {
                if (frame == null) {
                    return true;
                }
                if (blockScopeNode instanceof BlockScopeNode.FrameBlockScopeNode) {
                    if (blockScopeNode.isFunctionBlock()) {
                        if (parentBlock instanceof BlockScopeNode) {
                            blockScopeNode = (BlockScopeNode) parentBlock;
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                if (parentBlock instanceof BlockScopeNode) {
                    return true;
                } else if (parentBlock instanceof RootNode && functionFrame != null) {
                    return true;
                }
                break;
            }
        } else {
            assert blockOrRoot instanceof RootNode;
            if (frame != null) {
                // For closures, we don't have any outer block nodes, only the RootNode.
                if (ScopeFrameNode.isBlockScopeFrame(frame)) {
                    if (getParentFrame() != null) {
                        return true;
                    }
                }
            }
        }
        if (frame != null) {
            Frame parentFrame = JSFrameUtil.getParentFrame(frame);
            return parentFrame != null && parentFrame != JSFrameUtil.NULL_MATERIALIZED_FRAME;
        }
        return false;
    }

    @ExportMessage
    @TruffleBoundary
    Object getScopeParent() throws UnsupportedMessageException {
        if (blockOrRoot instanceof BlockScopeNode) {
            BlockScopeNode blockScopeNode = (BlockScopeNode) blockOrRoot;
            Frame enclosingFrame = frame;
            Node parentBlock;
            while ((parentBlock = JavaScriptNode.findBlockScopeNode(blockScopeNode.getParent())) != null) {
                if (frame == null) {
                    return new ScopeVariables(null, true, parentBlock, null);
                }

                if (blockScopeNode instanceof BlockScopeNode.FrameBlockScopeNode) {
                    enclosingFrame = getParentFrame();
                    if (blockScopeNode.isFunctionBlock()) {
                        if (parentBlock instanceof BlockScopeNode) {
                            blockScopeNode = (BlockScopeNode) parentBlock;
                            assert enclosingFrame != null;
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                if (parentBlock instanceof BlockScopeNode) {
                    return new ScopeVariables(enclosingFrame, true, parentBlock, functionFrame);
                } else if (parentBlock instanceof RootNode && functionFrame != null) {
                    return new ScopeVariables(functionFrame, true, parentBlock, functionFrame);
                }
                break;
            }
        } else {
            assert blockOrRoot instanceof RootNode;
            if (frame != null) {
                // For closures, we don't have any outer block nodes, only the RootNode.
                if (ScopeFrameNode.isBlockScopeFrame(frame)) {
                    Frame parentBlockScope = getParentFrame();
                    if (parentBlockScope != null) {
                        return new ScopeVariables(parentBlockScope, true, blockOrRoot, null);
                    }
                }
            }
        }
        if (frame != null) {
            Frame parentFrame = JSFrameUtil.getParentFrame(frame);
            if (parentFrame != null && parentFrame != JSFrameUtil.NULL_MATERIALIZED_FRAME) {
                RootNode rootNode = ((RootCallTarget) JSFunction.getCallTarget(JSFrameUtil.getFunctionObject(parentFrame))).getRootNode();
                return new ScopeVariables(parentFrame, true, rootNode, null);
            }
        }
        throw UnsupportedMessageException.create();
    }

    @TruffleBoundary
    private Frame getParentFrame() {
        OptionalInt parentSlot = JSFrameUtil.findOptionalFrameSlotIndex(frame.getFrameDescriptor(), ScopeFrameNode.PARENT_SCOPE_IDENTIFIER);
        if (parentSlot.isPresent()) {
            Object parent = frame.getObject(parentSlot.getAsInt());
            if (parent instanceof Frame) {
                return (Frame) parent;
            }
        }
        return null;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        ScopeMembers m = this.members;
        if (m == null) {
            m = new ScopeMembers(frame, blockOrRoot, functionFrame);
            this.members = m;
        }
        return m;
    }

    @ExportMessage
    static final class IsMemberReadable {

        @Specialization(guards = {"cachedMember.equals(member)"}, limit = "LIMIT")
        static boolean doCached(ScopeVariables receiver, String member,
                        @Cached("member") @SuppressWarnings("unused") String cachedMember,
                        // We cache the member existence for fast-path access
                        @Cached("doGeneric(receiver, member)") boolean cachedResult) {
            assert cachedResult == doGeneric(receiver, member);
            return cachedResult;
        }

        @Specialization(replaces = "doCached")
        @TruffleBoundary
        static boolean doGeneric(ScopeVariables receiver, String member) {
            return hasSlot(member, receiver);
        }
    }

    @ExportMessage
    static final class IsMemberModifiable {

        @Specialization(guards = {"cachedMember.equals(member)"}, limit = "LIMIT")
        static boolean doCached(ScopeVariables receiver, String member,
                        @Cached("member") @SuppressWarnings("unused") String cachedMember,
                        // We cache the member existence for fast-path access
                        @Cached("doGeneric(receiver, member)") boolean cachedResult) {
            assert cachedResult == doGeneric(receiver, member);
            return cachedResult;
        }

        @Specialization(replaces = "doCached")
        @TruffleBoundary
        static boolean doGeneric(ScopeVariables receiver, String member) {
            ResolvedSlot slot = findSlot(member, receiver);
            return slot != null && slot.isModifiable();
        }
    }

    @ExportMessage
    static final class ReadMember {

        @Specialization(guards = {"cachedMember.equals(member)"}, limit = "LIMIT")
        static Object doCached(ScopeVariables receiver, @SuppressWarnings("unused") String member,
                        @Cached("member") String cachedMember,
                        // We cache the member's read node for fast-path access
                        @Cached(value = "findSlot(member, receiver)") ResolvedSlot resolvedSlot,
                        @Cached(value = "findReadNode(resolvedSlot)") JavaScriptNode readNode) throws UnknownIdentifierException {
            return doRead(receiver, cachedMember, readNode, resolvedSlot);
        }

        @Specialization(replaces = "doCached")
        @TruffleBoundary
        static Object doGeneric(ScopeVariables receiver, String member) throws UnknownIdentifierException {
            ResolvedSlot resolvedSlot = findSlot(member, receiver);
            JavaScriptNode readNode = findReadNode(resolvedSlot);
            return doRead(receiver, member, readNode, resolvedSlot);
        }

        private static Object doRead(ScopeVariables receiver, String member, JavaScriptNode readNode, ResolvedSlot resolvedSlot) throws UnknownIdentifierException {
            if (readNode == null) {
                throw UnknownIdentifierException.create(member);
            }
            Frame frame = resolvedSlot.isFunctionFrame() ? receiver.functionFrame : receiver.frame;
            if (frame == null) {
                return Undefined.instance;
            } else {
                return readNode.execute((VirtualFrame) frame);
            }
        }
    }

    @ExportMessage
    static final class WriteMember {

        @Specialization(guards = {"cachedMember.equals(member)"}, limit = "LIMIT")
        static void doCached(ScopeVariables receiver, @SuppressWarnings("unused") String member, Object value,
                        @Cached("member") String cachedMember,
                        // We cache the member's write node for fast-path access
                        @Cached(value = "findSlot(member, receiver)") ResolvedSlot resolvedSlot,
                        @Cached(value = "findWriteNode(resolvedSlot)") WriteNode writeNode) throws UnknownIdentifierException {
            doWrite(receiver, cachedMember, value, writeNode, resolvedSlot);
        }

        @Specialization(replaces = "doCached")
        @TruffleBoundary
        static void doGeneric(ScopeVariables receiver, String member, Object value) throws UnknownIdentifierException {
            ResolvedSlot resolvedSlot = findSlot(member, receiver);
            WriteNode writeNode = findWriteNode(resolvedSlot);
            doWrite(receiver, member, value, writeNode, resolvedSlot);
        }

        private static void doWrite(ScopeVariables receiver, String member, Object value, WriteNode writeNode, ResolvedSlot resolvedSlot) throws UnknownIdentifierException {
            if (writeNode == null) {
                throw UnknownIdentifierException.create(member);
            }
            Frame frame = resolvedSlot.isFunctionFrame() ? receiver.functionFrame : receiver.frame;
            if (frame == null) {
                throw UnknownIdentifierException.create(member);
            }
            writeNode.executeWrite((VirtualFrame) frame, value);
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isMemberInsertable(@SuppressWarnings("unused") String member) {
        return false;
    }

    @ExportMessage
    @TruffleBoundary
    boolean hasSourceLocation() {
        return blockOrRoot.getEncapsulatingSourceSection() != null;
    }

    @ExportMessage
    @TruffleBoundary
    SourceSection getSourceLocation() throws UnsupportedMessageException {
        Node sourceSectionProvider = blockOrRoot;
        if (sourceSectionProvider instanceof BlockScopeNode && ((BlockScopeNode) sourceSectionProvider).isFunctionBlock()) {
            sourceSectionProvider = sourceSectionProvider.getRootNode();
        }
        SourceSection sourceLocation = sourceSectionProvider.getEncapsulatingSourceSection();
        if (sourceLocation == null) {
            throw UnsupportedMessageException.create();
        }
        return sourceLocation;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    @TruffleBoundary
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        RootNode root;
        if (blockOrRoot instanceof BlockScopeNode) {
            if (((BlockScopeNode) blockOrRoot).isFunctionBlock()) {
                root = blockOrRoot.getRootNode();
            } else {
                return "block";
            }
        } else {
            root = (RootNode) blockOrRoot;
        }
        String name = root.getName();
        return (name == null) ? "" : name;
    }

    static class ResolvedSlot {
        final int slot;
        final int frameLevel;
        final int scopeLevel;
        final FrameDescriptor descriptor;

        ResolvedSlot(int slot, int frameLevel, int scopeLevel, FrameDescriptor descriptor) {
            this.slot = slot;
            this.frameLevel = frameLevel;
            this.scopeLevel = scopeLevel;
            this.descriptor = descriptor;
        }

        ResolvedSlot() {
            this(-1, -1, -1, null);
        }

        JavaScriptNode createReadNode() {
            if (!hasSlot()) {
                return JSConstantNode.createUndefined();
            }
            ScopeFrameNode scopeFrameNode = createScopeFrameNode();
            return JSReadFrameSlotNode.create(JSFrameSlot.fromIndexedFrameSlot(descriptor, slot), scopeFrameNode, JSFrameUtil.hasTemporalDeadZone(descriptor, slot));
        }

        WriteNode createWriteNode() {
            if (!hasSlot()) {
                return null;
            }
            ScopeFrameNode scopeFrameNode = createScopeFrameNode();
            return JSWriteFrameSlotNode.create(JSFrameSlot.fromIndexedFrameSlot(descriptor, slot), scopeFrameNode, null, JSFrameUtil.hasTemporalDeadZone(descriptor, slot));
        }

        ScopeFrameNode createScopeFrameNode() {
            if (isFunctionFrame()) {
                return ScopeFrameNode.createCurrent();
            }
            return ScopeFrameNode.create(frameLevel, scopeLevel, null);
        }

        boolean isModifiable() {
            return hasSlot() && !JSFrameUtil.isConst(descriptor, slot) && !JSFrameUtil.isThisSlot(descriptor, slot) && !JSFrameUtil.isImportBinding(descriptor, slot);
        }

        boolean hasSlot() {
            return slot >= 0;
        }

        boolean isFunctionFrame() {
            return scopeLevel < 0;
        }

        @Override
        public String toString() {
            if (hasSlot()) {
                return getClass().getSimpleName() + "(" + String.valueOf(descriptor.getSlotName(slot)) + ", #" + slot + ", " + frameLevel + "/" + scopeLevel + ")";
            }
            return super.toString();
        }
    }

    static class DynamicScopeResolvedSlot extends ResolvedSlot {
        final Object key;

        DynamicScopeResolvedSlot(Object key, int slot, int frameLevel, int scopeLevel, FrameDescriptor descriptor) {
            super(slot, frameLevel, scopeLevel, descriptor);
            this.key = key;
        }

        @Override
        JavaScriptNode createReadNode() {
            JavaScriptNode readDynamicScope = super.createReadNode();

            class EvalRead extends JavaScriptNode {
                @Child JavaScriptNode getDynamicScope = readDynamicScope;
                @Child DynamicObjectLibrary objectLibrary;

                @Override
                public Object execute(VirtualFrame frame) {
                    JSDynamicObject scope = (JSDynamicObject) getDynamicScope.execute(frame);
                    if (!JSRuntime.isObject(scope)) {
                        return Undefined.instance;
                    }
                    DynamicObjectLibrary lib = objectLibrary;
                    if (lib == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        if (getParent() != null) {
                            lib = insert(DynamicObjectLibrary.getFactory().createDispatched(JSConfig.PropertyCacheLimit));
                        } else {
                            lib = DynamicObjectLibrary.getUncached();
                        }
                        objectLibrary = lib;
                    }
                    return Properties.getOrDefault(lib, scope, key, Undefined.instance);
                }
            }
            return new EvalRead();
        }

        @Override
        WriteNode createWriteNode() {
            JavaScriptNode readDynamicScope = super.createReadNode();

            class EvalWrite extends JavaScriptNode implements WriteNode {
                @Child JavaScriptNode getDynamicScope = readDynamicScope;
                @Child DynamicObjectLibrary objectLibrary;

                @Override
                public Object execute(VirtualFrame frame) {
                    throw CompilerDirectives.shouldNotReachHere();
                }

                @Override
                public void executeWrite(VirtualFrame frame, Object value) {
                    JSDynamicObject scope = (JSDynamicObject) getDynamicScope.execute(frame);
                    if (!JSRuntime.isObject(scope)) {
                        return;
                    }
                    DynamicObjectLibrary lib = objectLibrary;
                    if (lib == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        if (getParent() != null) {
                            lib = insert(DynamicObjectLibrary.getFactory().createDispatched(JSConfig.PropertyCacheLimit));
                        } else {
                            lib = DynamicObjectLibrary.getUncached();
                        }
                        objectLibrary = lib;
                    }
                    lib.putIfPresent(scope, key, value);
                }

                @Override
                public JavaScriptNode getRhs() {
                    return null;
                }
            }
            return new EvalWrite();
        }
    }

    static class ResolvedThisSlot extends ResolvedSlot {
        ResolvedThisSlot(int slot, int frameLevel, int scopeLevel, FrameDescriptor descriptor) {
            super(slot, frameLevel, scopeLevel, descriptor);
        }

        ResolvedThisSlot() {
        }

        @Override
        JavaScriptNode createReadNode() {
            return new ReadThisNode(hasSlot() ? super.createReadNode() : null);
        }
    }

    static ResolvedSlot findSlot(String memberString, ScopeVariables receiver) {
        CompilerAsserts.neverPartOfCompilation();
        TruffleString member = Strings.fromJavaString(memberString);
        if (receiver.frame == null) {
            return findSlotWithoutFrame(member, receiver.blockOrRoot);
        }

        class SlotVisitor {
            Node descNode = receiver.blockOrRoot;
            int parentSlot = -1;
            int frameLevel = 0;
            int scopeLevel = 0;

            public ResolvedSlot accept(FrameDescriptor frameDescriptor, int slot, Frame targetFrame) {
                assert targetFrame.getFrameDescriptor() == frameDescriptor;
                int effectiveScopeLevel = scopeLevel;
                if (targetFrame == receiver.functionFrame) {
                    assert receiver.functionFrame.getFrameDescriptor() == frameDescriptor;
                    effectiveScopeLevel = -1;
                }
                Object slotName = frameDescriptor.getSlotName(slot);
                if (ScopeFrameNode.PARENT_SCOPE_IDENTIFIER.equals(slotName)) {
                    parentSlot = slot;
                } else if (ScopeFrameNode.EVAL_SCOPE_IDENTIFIER.equals(slotName)) {
                    JSDynamicObject evalScope = (JSDynamicObject) targetFrame.getObject(slot);
                    if (JSRuntime.isObject(evalScope)) {
                        if (DynamicObjectLibrary.getUncached().containsKey(evalScope, member)) {
                            return new DynamicScopeResolvedSlot(member, slot, frameLevel, effectiveScopeLevel, frameDescriptor);
                        }
                    }
                } else if (JSFrameUtil.isThisSlot(frameDescriptor, slot) && ScopeVariables.RECEIVER_MEMBER.equals(member)) {
                    return new ResolvedThisSlot(slot, frameLevel, effectiveScopeLevel, frameDescriptor);
                } else if (!JSFrameUtil.isInternal(frameDescriptor, slot) && member.equals(slotName)) {
                    if (JSFrameUtil.isImportBinding(frameDescriptor, slot)) {
                        return new ResolvedImportSlot(slot, frameLevel, effectiveScopeLevel, frameDescriptor);
                    }
                    return new ResolvedSlot(slot, frameLevel, effectiveScopeLevel, frameDescriptor);
                }
                return null; // continue
            }
        }

        SlotVisitor visitor = new SlotVisitor();
        Frame outerFrame = receiver.frame;
        if (receiver.functionFrame != null) {
            // traverse local frames
            FrameDescriptor rootFrameDescriptor = receiver.functionFrame.getFrameDescriptor();
            while (visitor.descNode instanceof BlockScopeNode) {
                BlockScopeNode block = (BlockScopeNode) visitor.descNode;
                visitor.parentSlot = -1;

                if (block instanceof BlockScopeNode.FrameBlockScopeNode) {
                    FrameDescriptor blockFrameDescriptor = ((BlockScopeNode.FrameBlockScopeNode) block).getFrameDescriptor();
                    // Note: If we are just entering a block scope, the frame is not available yet.
                    assert outerFrame.getFrameDescriptor() == blockFrameDescriptor || block == receiver.blockOrRoot;
                    if (outerFrame.getFrameDescriptor() == blockFrameDescriptor) {
                        for (int i = 0; i < blockFrameDescriptor.getNumberOfSlots(); i++) {
                            ResolvedSlot resolvedSlot = visitor.accept(blockFrameDescriptor, i, outerFrame);
                            if (resolvedSlot != null) {
                                return resolvedSlot;
                            }
                        }
                    }
                }
                for (int i = block.getFrameStart(); i < block.getFrameEnd(); i++) {
                    ResolvedSlot resolvedSlot = visitor.accept(rootFrameDescriptor, i, receiver.functionFrame);
                    if (resolvedSlot != null) {
                        return resolvedSlot;
                    }
                }

                visitor.descNode = JavaScriptNode.findBlockScopeNode(visitor.descNode.getParent());
                if (visitor.parentSlot >= 0) {
                    Object parent = outerFrame.getObject(visitor.parentSlot);
                    if (parent instanceof Frame) {
                        outerFrame = (Frame) parent;
                        assert outerFrame != JSFrameUtil.NULL_MATERIALIZED_FRAME;
                        visitor.scopeLevel++;
                    } else {
                        break;
                    }
                }
            }

            assert receiver.functionFrame.getFrameDescriptor() == rootFrameDescriptor && visitor.frameLevel == 0;
            visitor.scopeLevel = -1;
            for (int slot = 0; slot < rootFrameDescriptor.getNumberOfSlots(); slot++) {
                // skip hoisted block-scoped slots; only accessible within their block
                if (JSFrameUtil.isHoistedFromBlock(rootFrameDescriptor, slot)) {
                    continue;
                }
                ResolvedSlot resolvedSlot = visitor.accept(rootFrameDescriptor, slot, receiver.functionFrame);
                if (resolvedSlot != null) {
                    return resolvedSlot;
                }
            }
            outerFrame = JSArguments.getEnclosingFrame(receiver.frame.getArguments());
            visitor.frameLevel = 1;
        }

        // traverse non-local frames
        while (outerFrame != JSFrameUtil.NULL_MATERIALIZED_FRAME) {
            visitor.descNode = JSFunction.getFunctionData(JSFrameUtil.getFunctionObject(outerFrame)).getRootNode();
            visitor.scopeLevel = 0;
            for (;;) {
                visitor.parentSlot = -1;
                for (int slot = 0; slot < outerFrame.getFrameDescriptor().getNumberOfSlots(); slot++) {
                    ResolvedSlot resolvedSlot = visitor.accept(outerFrame.getFrameDescriptor(), slot, outerFrame);
                    if (resolvedSlot != null) {
                        return resolvedSlot;
                    }
                }
                if (visitor.parentSlot >= 0) {
                    Object parent = outerFrame.getObject(visitor.parentSlot);
                    if (parent instanceof Frame) {
                        outerFrame = (Frame) parent;
                        assert outerFrame != JSFrameUtil.NULL_MATERIALIZED_FRAME;
                        visitor.scopeLevel++;
                        continue;
                    }
                }
                break;
            }

            outerFrame = JSArguments.getEnclosingFrame(outerFrame.getArguments());
            visitor.frameLevel++;
        }

        // If it can't be resolved, we still provide a best-effort 'this' member.
        if (receiver.frame != null && ScopeVariables.RECEIVER_MEMBER.equals(member)) {
            return new ResolvedThisSlot();
        }

        return null; // Not found
    }

    private static ResolvedSlot findSlotWithoutFrame(TruffleString member, Node blockOrRootNode) {
        CompilerAsserts.neverPartOfCompilation();
        Node descNode = blockOrRootNode;
        while (descNode != null) {
            if (!(descNode instanceof FrameDescriptorProvider)) {
                break;
            }
            FrameDescriptor desc = ((FrameDescriptorProvider) descNode).getFrameDescriptor();
            OptionalInt slot = JSFrameUtil.findOptionalFrameSlotIndex(desc, member);
            if (slot.isPresent()) {
                if (JSFrameUtil.isInternal(desc, slot.getAsInt())) {
                    return null;
                }
                return new ResolvedSlot();
            }

            descNode = JavaScriptNode.findBlockScopeNode(descNode.getParent());
        }
        return null;
    }

    static boolean hasSlot(String member, ScopeVariables receiver) {
        return findSlot(member, receiver) != null;
    }

    static JavaScriptNode findReadNode(ResolvedSlot slot) {
        if (slot != null) {
            return slot.createReadNode();
        } else {
            return null;
        }
    }

    static WriteNode findWriteNode(ResolvedSlot slot) {
        if (slot != null && slot.isModifiable()) {
            return slot.createWriteNode();
        } else {
            return null;
        }
    }

    static Object thisFromFunctionOrArguments(Object[] args) {
        // this can be either undefined or not populated yet
        // => try to avoid returning undefined in the latter case
        Object function = JSArguments.getFunctionObject(args);
        if (function instanceof JSFunctionObject jsFunction) {
            return isArrowFunctionWithThisCaptured(jsFunction) ? JSFunction.getLexicalThis(jsFunction) : thisFromArguments(args);
        }
        return Undefined.instance;
    }

    static Object thisFromArguments(Object[] args) {
        Object thisObject = JSArguments.getThisObject(args);
        Object function = JSArguments.getFunctionObject(args);
        if (function instanceof JSFunctionObject jsFunction && !JSFunction.isStrict(jsFunction)) {
            JSRealm realm = JavaScriptLanguage.getCurrentJSRealm();
            if (thisObject == Undefined.instance || thisObject == Null.instance) {
                thisObject = realm.getGlobalObject();
            } else {
                thisObject = JSRuntime.toObject(thisObject);
            }
        }
        return thisObject;
    }

    private static boolean isArrowFunctionWithThisCaptured(JSFunctionObject function) {
        return !JSFunction.isConstructor(function) && JSFunction.isClassPrototypeInitialized(function);
    }

    static final class ReadThisNode extends JavaScriptNode {
        @Child JavaScriptNode readThis;

        ReadThisNode(JavaScriptNode readThis) {
            this.readThis = readThis;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (readThis == null) {
                return thisFromArguments(frame.getArguments());
            }
            Object thisValue = readThis.execute(frame);
            if (thisValue == Undefined.instance) {
                return thisFromFunctionOrArguments(frame.getArguments());
            }
            return thisValue;
        }
    }

    static class ResolvedImportSlot extends ResolvedSlot {
        ResolvedImportSlot(int slot, int frameLevel, int scopeLevel, FrameDescriptor descriptor) {
            super(slot, frameLevel, scopeLevel, descriptor);
        }

        @Override
        JavaScriptNode createReadNode() {
            if (!hasSlot()) {
                return JSConstantNode.createUndefined();
            }
            return ReadImportBindingNode.create(super.createReadNode());
        }
    }
}
