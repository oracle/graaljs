/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.BlockNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptNode;

import java.util.Set;

public final class ExprBlockNode extends AbstractBlockNode implements SequenceNode {
    ExprBlockNode(JavaScriptNode[] statements) {
        super(statements);
    }

    public static JavaScriptNode createExprBlock(JavaScriptNode[] statements) {
        return filterStatements(statements, true);
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return block.executeBoolean(frame, BlockNode.NO_ARGUMENT);
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        return block.executeInt(frame, BlockNode.NO_ARGUMENT);
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return block.executeDouble(frame, BlockNode.NO_ARGUMENT);
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame, JavaScriptNode node, int index, int argument) throws UnexpectedResultException {
        return node.executeBoolean(frame);
    }

    @Override
    public int executeInt(VirtualFrame frame, JavaScriptNode node, int index, int argument) throws UnexpectedResultException {
        return node.executeInt(frame);
    }

    @Override
    public double executeDouble(VirtualFrame frame, JavaScriptNode node, int index, int argument) throws UnexpectedResultException {
        return node.executeDouble(frame);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new ExprBlockNode(cloneUninitialized(getStatements(), materializedTags));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return getStatements()[getStatements().length - 1].isResultAlwaysOfType(clazz);
    }
}
