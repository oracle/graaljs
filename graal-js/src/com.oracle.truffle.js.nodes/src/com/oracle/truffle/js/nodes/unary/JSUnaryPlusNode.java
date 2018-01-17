/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode.JSToNumberWrapperNode;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags;
import com.oracle.truffle.js.nodes.tags.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.UnaryOperationTag;

@NodeInfo(shortName = "+")
@NodeChild(value = "operand", type = JavaScriptNode.class)
public abstract class JSUnaryPlusNode extends JSToNumberWrapperNode {
    public static JSUnaryPlusNode create(JavaScriptNode operand) {
        return JSUnaryPlusNodeGen.create(operand);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == UnaryOperationTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        NodeObjectDescriptor descriptor = JSSpecificTags.createNodeObjectDescriptor();
        NodeInfo annotation = getClass().getAnnotation(NodeInfo.class);
        descriptor.addProperty("operator", annotation.shortName());
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
