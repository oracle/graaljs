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
package com.oracle.truffle.js.parser.env;

import java.util.Map;
import java.util.Objects;

import com.oracle.js.parser.ir.Scope;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.js.nodes.JSFrameDescriptor;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.util.DebugCounter;
import com.oracle.truffle.js.runtime.util.InternalSlotId;

public final class BlockEnvironment extends Environment {

    private final JSFrameDescriptor blockFrameDescriptor;
    private final JSFrameSlot parentSlot;
    private final int scopeLevel;
    private final boolean isFunctionBlock;
    private final Scope scope;
    private final JSFrameSlot blockScopeSlot;
    private int frameStart = -1;
    private int frameEnd = -1;

    private static final DebugCounter reifiedScopes = DebugCounter.create("Reified scopes");
    private static final DebugCounter virtualScopes = DebugCounter.create("Virtual scopes");

    public BlockEnvironment(Environment parent, NodeFactory factory, JSContext context, Scope blockScope) {
        super(parent, factory, context);
        this.isFunctionBlock = blockScope != null && (blockScope.isFunctionTopScope() || blockScope.isEvalScope());
        this.scope = blockScope;
        if (isScopeCaptured(blockScope) || !context.getContextOptions().isScopeOptimization()) {
            this.blockFrameDescriptor = factory.createBlockFrameDescriptor();
            this.parentSlot = Objects.requireNonNull(blockFrameDescriptor.findFrameSlot(ScopeFrameNode.PARENT_SCOPE_IDENTIFIER));
            this.scopeLevel = parent.getScopeLevel() + 1;
            this.blockScopeSlot = parent.function().getOrCreateBlockScopeSlot();
            reifiedScopes.inc();
        } else {
            this.blockFrameDescriptor = null;
            this.parentSlot = null;
            this.scopeLevel = parent.getScopeLevel();
            this.blockScopeSlot = parent.getCurrentBlockScopeSlot();
            virtualScopes.inc();
        }
    }

    public static boolean isScopeCaptured(Scope blockScope) {
        return blockScope == null || blockScope.hasClosures() || blockScope.hasNestedEval() || blockScope.isClassHeadScope();
    }

    public BlockEnvironment(Environment parent, NodeFactory factory, JSContext context) {
        this(parent, factory, context, null);
    }

    @Override
    public JSFrameSlot findBlockFrameSlot(Object name) {
        if (!hasScopeFrame()) {
            return null;
        }
        JSFrameSlot slot = getBlockFrameDescriptor().findFrameSlot(slotId(name));
        if (slot != null && JSFrameUtil.isPrivateName(slot)) {
            // Private names are only visible from within the corresponding PrivateEnvironment.
            return null;
        }
        return slot;
    }

    @Override
    public JSFrameSlot findFunctionFrameSlot(Object name) {
        return getFunctionFrameDescriptor().findFrameSlot(slotId(name));
    }

    @Override
    public JSFrameDescriptor getBlockFrameDescriptor() {
        if (blockFrameDescriptor != null) {
            return blockFrameDescriptor;
        } else {
            return getFunctionFrameDescriptor();
        }
    }

    public JSFrameSlot getParentSlot() {
        return parentSlot;
    }

    @Override
    public int getScopeLevel() {
        return scopeLevel;
    }

    @Override
    public boolean hasScopeFrame() {
        return blockFrameDescriptor != null;
    }

    @Override
    public JSFrameSlot getCurrentBlockScopeSlot() {
        return blockScopeSlot;
    }

    public boolean isFunctionBlock() {
        return isFunctionBlock;
    }

    private Object slotId(Object name) {
        assert name instanceof String || name instanceof InternalSlotId : name;
        if (isFunctionBlock) {
            return name;
        }
        return JSFrameDescriptor.scopedIdentifier(name, scope);
    }

    @Override
    public JSFrameSlot declareInternalSlot(Object name) {
        return getBlockFrameDescriptor().findOrAddFrameSlot(slotId(name), 0, FrameSlotKind.Illegal);
    }

    @Override
    public void addFrameSlotFromSymbol(com.oracle.js.parser.ir.Symbol symbol) {
        Object id = slotId(symbol.getName());
        assert (!hasScopeFrame() || !getBlockFrameDescriptor().contains(id)) && !getFunctionFrameDescriptor().contains(id) : symbol;
        if (symbol.isClosedOver() || (scope != null && scope.hasNestedEval()) || !context.getContextOptions().isScopeOptimization()) {
            getBlockFrameDescriptor().findOrAddFrameSlot(id, symbol.getFlags(), FrameSlotKind.Illegal);
        } else {
            JSFrameSlot slot = getFunctionFrameDescriptor().findOrAddFrameSlot(id, symbol.getFlags() | (!isFunctionBlock ? JSFrameUtil.IS_HOISTED_FROM_BLOCK : 0), FrameSlotKind.Illegal);
            updateSlotRange(slot);
        }
    }

    private void updateSlotRange(JSFrameSlot slot) {
        if (slot.getIndex() < frameStart || frameStart == -1) {
            frameStart = slot.getIndex();
        }
        if (slot.getIndex() >= frameEnd || frameEnd == -1) {
            frameEnd = slot.getIndex() + 1;
        }
    }

    public int getStart() {
        return frameStart;
    }

    public int getEnd() {
        return frameEnd;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    @Override
    protected String toStringImpl(Map<String, Integer> state) {
        int currentFrameLevel = state.getOrDefault("frameLevel", 0);
        int currentScopeLevel = state.getOrDefault("scopeLevel", 0);
        state.put("scopeLevel", currentScopeLevel + (hasScopeFrame() ? 1 : 0));
        return "Block(" + currentFrameLevel + ", " + currentScopeLevel + ")" +
                        " size=" + getBlockFrameDescriptor().getSize() + " " + joinElements(getBlockFrameDescriptor().getIdentifiers()) + " " + scope;
    }
}
