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
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.arguments.AccessArgumentsArrayDirectlyNode;
import com.oracle.truffle.js.runtime.JSContext;

public class CallApplyArgumentsNode extends JavaScriptNode {
    private final JSContext context;
    @Child private JSFunctionCallNode.InvokeNode callNode;

    protected CallApplyArgumentsNode(JSContext context, JSFunctionCallNode callNode) {
        this.context = context;
        this.callNode = (JSFunctionCallNode.InvokeNode) callNode;
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        Object target = callNode.executeTarget(frame);
        Object function = callNode.executeFunctionWithTarget(frame, target);

        if (function != context.getRealm().getApplyFunctionObject()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            replaceWithOrdinaryCall();
        }

        return callNode.executeCall(callNode.createArguments(frame, target, function));
    }

    private void replaceWithOrdinaryCall() {
        atomic(() -> {
            NodeUtil.forEachChild(callNode.getArgumentsNode(), node -> {
                if (node instanceof AccessArgumentsArrayDirectlyNode) {
                    ((AccessArgumentsArrayDirectlyNode) node).replaceWithDefaultArguments();
                }
                return true;
            });
            this.replace(callNode, "not the built-in apply function");
        });
    }

    public static JavaScriptNode create(JSContext context, JSFunctionCallNode callNode) {
        return new CallApplyArgumentsNode(context, callNode);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(context, cloneUninitialized(callNode));
    }
}
