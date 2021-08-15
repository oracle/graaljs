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
package com.oracle.truffle.js.nodes.function;

import static com.oracle.truffle.js.nodes.JSNodeUtil.getWrappedNode;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.arguments.AccessArgumentsArrayDirectlyNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;

public class CallApplyArgumentsNode extends JavaScriptNode {
    @Child private JSFunctionCallNode.InvokeNode callNode;

    protected CallApplyArgumentsNode(JSFunctionCallNode callNode) {
        this.callNode = (JSFunctionCallNode.InvokeNode) callNode;
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        Object target = callNode.executeTarget(frame);
        Object function = callNode.executeFunctionWithTarget(frame, target);

        if (function != getRealm().getApplyFunctionObject()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            replaceWithOrdinaryCall();
        }

        return callNode.executeCall(callNode.createArguments(frame, target, function));
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(StandardTags.ExpressionTag.class) || materializedTags.contains(JSTags.FunctionCallTag.class)) {
            replaceWithOrdinaryCall();
            // nodes have been replaced by replaceWithOrdinaryCall, nothing to do
        }
        return this;
    }

    private void replaceWithOrdinaryCall() {
        atomic(() -> {
            for (JavaScriptNode n : callNode.getArgumentNodes()) {
                JavaScriptNode node = getWrappedNode(n);
                if (node instanceof AccessArgumentsArrayDirectlyNode) {
                    ((AccessArgumentsArrayDirectlyNode) node).replaceWithDefaultArguments();
                }
            }
            this.replace(callNode, "not the built-in apply function");
        });
    }

    public static JavaScriptNode create(JSFunctionCallNode callNode) {
        return new CallApplyArgumentsNode(callNode);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(cloneUninitialized(callNode, materializedTags));
    }
}
