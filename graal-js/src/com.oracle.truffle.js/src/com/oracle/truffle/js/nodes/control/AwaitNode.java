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
package com.oracle.truffle.js.nodes.control;

import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.instrumentation.JSMaterializedInvokeTargetableNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.runtime.JSContext;

public class AwaitNode extends AbstractAwaitNode implements ResumableNode.WithIntState {

    @Child private JSTargetableNode materializedInputNode;

    protected AwaitNode(JSContext context, int stateSlot, JavaScriptNode expression, JSReadFrameSlotNode readAsyncContextNode, JSReadFrameSlotNode readAsyncResultNode,
                    JSTargetableNode materializedInputNode) {
        super(context, stateSlot, expression, readAsyncContextNode, readAsyncResultNode);
        this.materializedInputNode = materializedInputNode;
    }

    public static JavaScriptNode create(JSContext context, int stateSlot, JavaScriptNode expression, JSReadFrameSlotNode readAsyncContextNode, JSReadFrameSlotNode readAsyncResultNode) {
        return new AwaitNode(context, stateSlot, expression, readAsyncContextNode, readAsyncResultNode, null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        int index = getStateAsInt(frame, stateSlot);
        if (index == 0) {
            Object value = expression.execute(frame);
            setStateAsInt(frame, stateSlot, 1);
            return suspendAwait(frame, value);
        } else {
            setStateAsInt(frame, stateSlot, 0);
            return resumeAwait(frame);
        }
    }

    @Override
    protected void echoInput(VirtualFrame frame, Object value) {
        if (materializedInputNode != null) {
            materializedInputNode.executeWithTarget(frame, value);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        JavaScriptNode expressionCopy = cloneUninitialized(expression, materializedTags);
        JSReadFrameSlotNode asyncResultCopy = cloneUninitialized(readAsyncResultNode, materializedTags);
        JSReadFrameSlotNode asyncContextCopy = cloneUninitialized(readAsyncContextNode, materializedTags);
        return create(context, stateSlot, expressionCopy, asyncContextCopy, asyncResultCopy);
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializationNeeded() && materializedTags.contains(JSTags.ControlFlowBranchTag.class)) {
            JSTargetableNode materializedInput = JSMaterializedInvokeTargetableNode.EchoTargetValueNode.create();
            AwaitNode materialized = new AwaitNode(context, stateSlot, cloneUninitialized(expression, materializedTags),
                            cloneUninitialized(readAsyncContextNode, materializedTags),
                            cloneUninitialized(readAsyncResultNode, materializedTags), materializedInput);
            transferSourceSectionAndTags(this, materialized);
            return materialized;
        }
        return this;
    }

    private boolean materializationNeeded() {
        return this.materializedInputNode == null;
    }
}
