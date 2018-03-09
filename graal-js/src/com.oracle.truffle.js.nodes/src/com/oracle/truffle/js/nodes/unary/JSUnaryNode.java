/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import java.util.Objects;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;

@GenerateWrapper
public abstract class JSUnaryNode extends JavaScriptNode {
    @Child @Executed protected JavaScriptNode operandNode;

    protected JSUnaryNode(JavaScriptNode operand) {
        this.operandNode = operand;
    }

    /**
     * For instrumentation {@link #createWrapper wrapper}.
     */
    protected JSUnaryNode() {
    }

    public final JavaScriptNode getOperand() {
        return operandNode;
    }

    public abstract Object execute(VirtualFrame frame, Object operandValue);

    @Override
    public String expressionToString() {
        if (getOperand() != null) {
            NodeInfo annotation = getClass().getAnnotation(NodeInfo.class);
            if (annotation != null) {
                String shortName = annotation.shortName();
                if (!shortName.isEmpty()) {
                    return "(" + shortName + (shortName.length() == 1 ? "" : " ") + Objects.toString(getOperand().expressionToString(), INTERMEDIATE_VALUE) + ")";
                }
            }
        }
        return null;
    }

    @Override
    public Object getNodeObject() {
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor();
        NodeInfo annotation = getClass().getAnnotation(NodeInfo.class);
        assert annotation != null;
        descriptor.addProperty("operator", annotation.shortName());
        return descriptor;
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new JSUnaryNodeWrapper(this, probe);
    }
}
