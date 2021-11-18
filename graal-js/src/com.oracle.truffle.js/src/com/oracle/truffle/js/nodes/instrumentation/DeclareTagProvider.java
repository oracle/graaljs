/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.instrumentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.BlockScopeNode.FrameBlockScopeNode;
import com.oracle.truffle.js.nodes.function.FunctionBodyNode;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class DeclareTagProvider {

    public static JavaScriptNode createMaterializedFunctionBodyNode(JavaScriptNode body, SourceSection sourceSection, FrameDescriptor frameDescriptor) {
        return new MaterializedFunctionBodyNode(body, sourceSection, frameDescriptor);
    }

    public static JavaScriptNode createMaterializedBlockNode(JavaScriptNode block, int blockScopeSlot, FrameDescriptor frameDescriptor, int parentSlot, SourceSection sourceSection,
                    boolean functionBlock, boolean functionFrame) {
        return new MaterializedFrameBlockScopeNode(block, blockScopeSlot, frameDescriptor, parentSlot, sourceSection, functionBlock, functionFrame);
    }

    public static boolean isMaterializedFrameProvider(JavaScriptNode node) {
        return node instanceof MaterializedFrameBlockScopeNode || node instanceof MaterializedFunctionBodyNode;
    }

    public static NodeObjectDescriptor createDeclareNodeObject(Object name, Object type) {
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor();
        descriptor.addProperty(JSTags.DeclareTag.NAME, name);
        descriptor.addProperty(JSTags.DeclareTag.TYPE, type);
        return descriptor;
    }

    private DeclareTagProvider() {
    }

    private static JavaScriptNode[] initDeclarations(FrameDescriptor frameDescriptor, SourceSection sourceSection) {
        assert sourceSection != null;
        if (frameDescriptor != null) {
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < frameDescriptor.getNumberOfSlots(); i++) {
                if (!JSFrameUtil.isInternal(frameDescriptor, i) && !JSFrameUtil.isHoistable(frameDescriptor, i)) {
                    slots.add(i);
                }
            }
            JavaScriptNode[] declarations = new JavaScriptNode[slots.size()];
            for (int i = 0; i < slots.size(); i++) {
                declarations[i] = new DeclareProviderNode(frameDescriptor, slots.get(i));
                declarations[i].setSourceSection(sourceSection);
            }
            return declarations;
        } else {
            return new JavaScriptNode[0];
        }
    }

    private static class MaterializedFrameBlockScopeNode extends FrameBlockScopeNode {

        @Children private JavaScriptNode[] declarations;

        protected MaterializedFrameBlockScopeNode(JavaScriptNode block, int blockScopeSlot, FrameDescriptor frameDescriptor, int parentSlot, SourceSection sourceSection,
                        boolean functionBlock, boolean captureFunctionFrame) {
            super(block, blockScopeSlot, frameDescriptor, parentSlot, functionBlock, captureFunctionFrame);
            this.declarations = initDeclarations(frameDescriptor, sourceSection);
        }

        @ExplodeLoop
        private void executeDeclarations(VirtualFrame frame) {
            for (JavaScriptNode declaration : declarations) {
                declaration.execute(frame);
            }
        }

        @Override
        public Object execute(VirtualFrame frame) {
            executeDeclarations(frame);
            return super.execute(frame);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            executeDeclarations(frame);
            super.executeVoid(frame);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new MaterializedFrameBlockScopeNode(cloneUninitialized(block, materializedTags),
                            blockScopeSlot, frameDescriptor, parentSlot, getSourceSection(), functionBlock, captureFunctionFrame);
        }
    }

    private static class MaterializedFunctionBodyNode extends FunctionBodyNode {

        @Children private JavaScriptNode[] declarations;

        private final FrameDescriptor frameDescriptor;

        protected MaterializedFunctionBodyNode(JavaScriptNode body, SourceSection sourceSection, FrameDescriptor frameDescriptor) {
            super(body);
            this.frameDescriptor = frameDescriptor;
            this.declarations = initDeclarations(frameDescriptor, sourceSection);
        }

        @ExplodeLoop
        @Override
        public Object execute(VirtualFrame frame) {
            for (JavaScriptNode declaration : declarations) {
                declaration.execute(frame);
            }
            return super.execute(frame);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new MaterializedFunctionBodyNode(cloneUninitialized(getBody(), materializedTags), getSourceSection(), frameDescriptor);
        }
    }

    private static class DeclareProviderNode extends JavaScriptNode {

        private final FrameDescriptor frameDescriptor;
        private final int slotIndex;

        DeclareProviderNode(FrameDescriptor frameDescriptor, int slotIndex) {
            this.frameDescriptor = frameDescriptor;
            this.slotIndex = slotIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // Ignored
            return Undefined.instance;
        }

        @Override
        public boolean hasTag(Class<? extends Tag> tag) {
            if (tag == JSTags.DeclareTag.class) {
                return true;
            } else {
                return super.hasTag(tag);
            }
        }

        @Override
        public boolean isInstrumentable() {
            return true;
        }

        @Override
        public Object getNodeObject() {
            String type;
            if (JSFrameUtil.isConst(frameDescriptor, slotIndex)) {
                type = "const";
            } else if (JSFrameUtil.isLet(frameDescriptor, slotIndex)) {
                type = "let";
            } else {
                type = "var";
            }
            return createDeclareNodeObject(frameDescriptor.getSlotName(slotIndex), type);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DeclareProviderNode(frameDescriptor, slotIndex);
        }
    }
}
