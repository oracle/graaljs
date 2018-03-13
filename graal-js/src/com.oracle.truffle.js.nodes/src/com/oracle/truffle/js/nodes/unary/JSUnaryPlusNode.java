/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode.JSToNumberWrapperNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;

@NodeInfo(shortName = "+")
public abstract class JSUnaryPlusNode extends JSToNumberWrapperNode {

    protected JSUnaryPlusNode(JavaScriptNode operand) {
        super(operand);
    }

    public static JSUnaryPlusNode create(JavaScriptNode operand) {
        return JSUnaryPlusNodeGen.create(operand);
    }

    @Specialization
    @Override
    protected Object doDefault(Object value) {
        return super.doDefault(value);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == UnaryExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor();
        descriptor.addProperty("operator", getClass().getAnnotation(NodeInfo.class).shortName());
        return descriptor;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSUnaryPlusNodeGen.create(cloneUninitialized(getOperand()));
    }

    @Override
    public String expressionToString() {
        return "(" + "+" + getOperand().expressionToString() + ")";
    }
}
