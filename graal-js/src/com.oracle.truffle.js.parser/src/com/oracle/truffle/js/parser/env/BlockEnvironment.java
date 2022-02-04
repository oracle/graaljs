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

import com.oracle.truffle.js.nodes.JSFrameDescriptor;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;

public final class BlockEnvironment extends Environment {

    private final JSFrameDescriptor blockFrameDescriptor;
    private final JSFrameSlot parentSlot;
    private final int scopeLevel;
    private final boolean isFunctionBlock;

    public BlockEnvironment(Environment parent, NodeFactory factory, JSContext context, boolean isFunctionBlock) {
        super(parent, factory, context);
        this.blockFrameDescriptor = factory.createBlockFrameDescriptor();
        this.parentSlot = Objects.requireNonNull(blockFrameDescriptor.findFrameSlot(ScopeFrameNode.PARENT_SCOPE_IDENTIFIER));
        this.scopeLevel = parent.getScopeLevel() + 1;
        this.isFunctionBlock = isFunctionBlock;
        parent.function().getOrCreateBlockScopeSlot();
    }

    public BlockEnvironment(Environment parent, NodeFactory factory, JSContext context) {
        this(parent, factory, context, false);
    }

    @Override
    public JSFrameSlot findBlockFrameSlot(Object name) {
        JSFrameSlot slot = getBlockFrameDescriptor().findFrameSlot(name);
        if (slot != null && JSFrameUtil.isPrivateName(slot)) {
            // Private names are only visible from within the corresponding PrivateEnvironment.
            return null;
        }
        return slot;
    }

    @Override
    public JSFrameDescriptor getBlockFrameDescriptor() {
        return blockFrameDescriptor;
    }

    public JSFrameSlot getParentSlot() {
        return parentSlot;
    }

    @Override
    public int getScopeLevel() {
        return scopeLevel;
    }

    @Override
    public JSFrameSlot getCurrentBlockScopeSlot() {
        return function().getBlockScopeSlot();
    }

    public boolean isFunctionBlock() {
        return isFunctionBlock;
    }

    @Override
    protected String toStringImpl(Map<String, Integer> state) {
        int currentFrameLevel = state.getOrDefault("frameLevel", 0);
        int currentScopeLevel = state.getOrDefault("scopeLevel", 0);
        state.put("scopeLevel", currentScopeLevel + 1);
        return "Block(" + currentFrameLevel + ", " + currentScopeLevel + ")" +
                        " size=" + getBlockFrameDescriptor().getSize() + " " + joinElements(getBlockFrameDescriptor().getIdentifiers());
    }
}
