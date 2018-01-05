/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import java.util.Objects;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;

@NodeChild(value = "operand")
public abstract class JSUnaryNode extends JavaScriptNode {

    public abstract JavaScriptNode getOperand();

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
}
