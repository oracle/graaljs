/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanUnaryNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.runtime.JSInterruptedExecutionException;

abstract class AbstractRepeatingNode extends JavaScriptNode implements RepeatingNode, ResumableNode {

    @Child protected JavaScriptNode conditionNode;
    @Child protected JavaScriptNode bodyNode;

    AbstractRepeatingNode(JavaScriptNode condition, JavaScriptNode body) {
        this.conditionNode = condition == null ? null : JSToBooleanUnaryNode.create(condition);
        this.bodyNode = body;
    }

    protected final boolean executeCondition(VirtualFrame frame) {
        return StatementNode.executeConditionAsBoolean(frame, conditionNode);
    }

    protected final void executeBody(VirtualFrame frame) {
        bodyNode.executeVoid(frame);
        if (CompilerDirectives.inInterpreter()) {
            checkThreadInterrupted();
        }
    }

    private void checkThreadInterrupted() {
        CompilerAsserts.neverPartOfCompilation("do not check thread interruption from compiled code");
        if (Thread.interrupted()) {
            throw new JSInterruptedExecutionException("Thread was interrupted.", this);
        }
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        return executeRepeating(frame);
    }

    @Override
    public final boolean executeBoolean(VirtualFrame frame) {
        return executeRepeating(frame);
    }

    @Override
    public final Object executeRepeatingWithValue(VirtualFrame frame) {
        return RepeatingNode.super.executeRepeatingWithValue(frame);
    }

    protected boolean materializationNeeded() {
        // If we are using tagged nodes, this node is already materialized.
        return !JSNodeUtil.isTaggedNode(bodyNode);
    }

    @Override
    public abstract AbstractRepeatingNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags);

    protected final JavaScriptNode materializeBody(Set<Class<? extends Tag>> materializedTags) {
        if (JSNodeUtil.isTaggedNode(bodyNode)) {
            return bodyNode;
        }
        JavaScriptNode newBody = JSTaggedExecutionNode.createFor(bodyNode, JSTags.ControlFlowBlockTag.class, materializedTags);
        if (newBody == bodyNode) {
            newBody = cloneUninitialized(bodyNode, materializedTags);
        }
        return newBody;
    }

    protected final JavaScriptNode materializeCondition(Set<Class<? extends Tag>> materializedTags) {
        if (conditionNode == null || JSNodeUtil.isTaggedNode(conditionNode)) {
            return conditionNode;
        }
        JavaScriptNode newCondition = JSTaggedExecutionNode.createForInput(conditionNode, JSTags.ControlFlowBranchTag.class,
                        JSTags.createNodeObjectDescriptor("type", JSTags.ControlFlowBranchTag.Type.Condition.name()), materializedTags);
        if (newCondition == conditionNode) {
            newCondition = cloneUninitialized(conditionNode, materializedTags);
        }
        return newCondition;
    }
}
