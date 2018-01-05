/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
            NodeUtil.forEachChild(callNode.argumentsNode, node -> {
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
