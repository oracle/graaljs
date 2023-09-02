/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.binary;

import java.util.Objects;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.ResumableNode;
import com.oracle.truffle.js.nodes.control.YieldException;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryOperationTag;

public abstract class JSLogicalNode extends JavaScriptNode implements ResumableNode.WithIntState {

    @Child @Executed protected JavaScriptNode leftNode;
    @Child protected JavaScriptNode rightNode;

    private static final int RESUME_RIGHT = 1;
    private static final int RESUME_UNEXECUTED = 0;

    protected JSLogicalNode(JavaScriptNode left, JavaScriptNode right) {
        this.leftNode = left;
        this.rightNode = right;
    }

    protected abstract Object executeEvaluated(VirtualFrame frame, Object leftValue);

    @Override
    public Object resume(VirtualFrame frame, int stateSlot) {
        int state = getStateAsIntAndReset(frame, stateSlot);
        if (state == RESUME_UNEXECUTED) {
            Object leftValue = leftNode.execute(frame);
            try {
                return executeEvaluated(frame, leftValue);
            } catch (YieldException e) {
                setStateAsInt(frame, stateSlot, RESUME_RIGHT);
                throw e;
            }
        } else {
            assert state == RESUME_RIGHT;
            try {
                return rightNode.execute(frame);
            } catch (YieldException e) {
                setStateAsInt(frame, stateSlot, RESUME_RIGHT);
                throw e;
            }
        }
    }

    public final JavaScriptNode getLeft() {
        return leftNode;
    }

    public final JavaScriptNode getRight() {
        return rightNode;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return getLeft().isResultAlwaysOfType(clazz) && getRight().isResultAlwaysOfType(clazz);
    }

    @Override
    public String expressionToString() {
        if (getLeft() != null && getRight() != null) {
            NodeInfo annotation = getClass().getAnnotation(NodeInfo.class);
            if (annotation != null && !annotation.shortName().isEmpty()) {
                return "(" + Objects.toString(getLeft().expressionToString(), INTERMEDIATE_VALUE) + " " + annotation.shortName() + " " +
                                Objects.toString(getRight().expressionToString(), INTERMEDIATE_VALUE) + ")";
            }
        }
        return null;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == BinaryOperationTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        NodeInfo annotation = getClass().getAnnotation(NodeInfo.class);
        if (annotation != null) {
            String shortName = annotation.shortName();
            if (!shortName.isEmpty()) {
                return JSTags.createNodeObjectDescriptor("operator", annotation.shortName());
            }
        }
        return null;
    }
}
