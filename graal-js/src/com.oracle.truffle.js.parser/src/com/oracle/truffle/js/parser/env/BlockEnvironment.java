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
package com.oracle.truffle.js.parser.env;

import java.util.Objects;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.runtime.JSContext;

public class BlockEnvironment extends Environment {
    private final FunctionEnvironment functionEnvironment;
    private final FrameDescriptor blockFrameDescriptor;
    private final FrameSlot parentSlot;
    private final int scopeLevel;
    private final FrameSlot[] parentSlots;

    public BlockEnvironment(Environment parent, NodeFactory factory, JSContext context) {
        super(parent, factory, context);
        this.functionEnvironment = parent.function();
        this.blockFrameDescriptor = factory.createBlockFrameDescriptor();
        this.parentSlot = Objects.requireNonNull(blockFrameDescriptor.findFrameSlot(ScopeFrameNode.PARENT_SCOPE_IDENTIFIER));
        this.scopeLevel = parent.getScopeLevel() + 1;
        this.parentSlots = prepend(parent.getParentSlots(), parentSlot);
        assert parentSlots.length == scopeLevel;
    }

    @Override
    public FunctionEnvironment function() {
        return functionEnvironment;
    }

    @Override
    public FrameSlot findBlockFrameSlot(String name) {
        FrameSlot frameSlot = getBlockFrameDescriptor().findFrameSlot(name);
        return frameSlot;
    }

    @Override
    public FrameDescriptor getBlockFrameDescriptor() {
        return blockFrameDescriptor;
    }

    public FrameSlot getParentSlot() {
        return parentSlot;
    }

    @Override
    public int getScopeLevel() {
        return scopeLevel;
    }

    @Override
    public FrameSlot[] getParentSlots() {
        return parentSlots;
    }

    private static FrameSlot[] prepend(FrameSlot[] srcArray, FrameSlot newSlot) {
        FrameSlot[] newArray = new FrameSlot[srcArray.length + 1];
        newArray[0] = newSlot;
        System.arraycopy(srcArray, 0, newArray, 1, srcArray.length);
        return newArray;
    }
}
