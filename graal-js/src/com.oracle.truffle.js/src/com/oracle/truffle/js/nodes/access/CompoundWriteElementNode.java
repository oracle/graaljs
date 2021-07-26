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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;

import java.util.Set;

public class CompoundWriteElementNode extends WriteElementNode {
    @Child private JSWriteFrameSlotNode writeIndexNode;

    public static CompoundWriteElementNode create(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSWriteFrameSlotNode writeIndexNode, JSContext context,
                    boolean isStrict) {
        return create(targetNode, indexNode, valueNode, writeIndexNode, context, isStrict, false);
    }

    private static CompoundWriteElementNode create(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSWriteFrameSlotNode writeIndexNode, JSContext context,
                    boolean isStrict, boolean writeOwn) {
        return new CompoundWriteElementNode(targetNode, indexNode, valueNode, writeIndexNode, context, isStrict, writeOwn);
    }

    protected CompoundWriteElementNode(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSWriteFrameSlotNode writeIndexNode, JSContext context, boolean isStrict,
                    boolean writeOwn) {
        super(targetNode, indexNode, valueNode, context, isStrict, writeOwn);
        this.writeIndexNode = writeIndexNode;
    }

    @Override
    protected Object executeWithTargetAndIndex(VirtualFrame frame, Object target, Object index, Object receiver) {
        requireObjectCoercible(target, index);
        return super.executeWithTargetAndIndex(frame, target, writeIndex(frame, toArrayIndex(index)), receiver);
    }

    @Override
    protected Object executeWithTargetAndIndex(VirtualFrame frame, Object target, int index, Object receiver) {
        requireObjectCoercible(target, index);
        return super.executeWithTargetAndIndex(frame, target, writeIndex(frame, index), receiver);
    }

    @Override
    protected int executeWithTargetAndIndexInt(VirtualFrame frame, Object target, Object index, Object receiver) throws UnexpectedResultException {
        requireObjectCoercible(target, index);
        return super.executeWithTargetAndIndexInt(frame, target, writeIndex(frame, toArrayIndex(index)), receiver);
    }

    @Override
    protected int executeWithTargetAndIndexInt(VirtualFrame frame, Object target, int index, Object receiver) throws UnexpectedResultException {
        requireObjectCoercible(target, index);
        return super.executeWithTargetAndIndexInt(frame, target, writeIndex(frame, index), receiver);
    }

    @Override
    protected double executeWithTargetAndIndexDouble(VirtualFrame frame, Object target, Object index, Object receiver) throws UnexpectedResultException {
        requireObjectCoercible(target, index);
        return super.executeWithTargetAndIndexDouble(frame, target, writeIndex(frame, toArrayIndex(index)), receiver);
    }

    @Override
    protected double executeWithTargetAndIndexDouble(VirtualFrame frame, Object target, int index, Object receiver) throws UnexpectedResultException {
        requireObjectCoercible(target, index);
        return super.executeWithTargetAndIndexDouble(frame, target, writeIndex(frame, index), receiver);
    }

    private Object writeIndex(VirtualFrame frame, Object index) {
        if (writeIndexNode != null) {
            writeIndexNode.executeWrite(frame, index);
        }
        return index;
    }

    private int writeIndex(VirtualFrame frame, int index) {
        if (writeIndexNode != null) {
            writeIndexNode.executeWrite(frame, index);
        }
        return index;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(cloneUninitialized(targetNode, materializedTags),
                        cloneUninitialized(indexNode, materializedTags),
                        cloneUninitialized(valueNode, materializedTags),
                        cloneUninitialized(writeIndexNode, materializedTags),
                        getContext(), isStrict(), writeOwn());
    }

    @Override
    protected WriteElementNode createMaterialized(JavaScriptNode newTarget, JavaScriptNode newIndex, JavaScriptNode newValue) {
        return CompoundWriteElementNode.create(newTarget, newIndex, newValue, writeIndexNode, getContext(), isStrict(), writeOwn());
    }
}
