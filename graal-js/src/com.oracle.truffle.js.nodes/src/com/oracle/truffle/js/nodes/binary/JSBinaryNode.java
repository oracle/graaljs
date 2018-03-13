/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import java.util.Objects;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;

public abstract class JSBinaryNode extends JavaScriptNode {
    @Child @Executed protected JavaScriptNode leftNode;
    @Child @Executed protected JavaScriptNode rightNode;

    protected JSBinaryNode(JavaScriptNode left, JavaScriptNode right) {
        this.leftNode = left;
        this.rightNode = right;
    }

    public final JavaScriptNode getLeft() {
        return leftNode;
    }

    public final JavaScriptNode getRight() {
        return rightNode;
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
        if (tag == BinaryExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor();
        NodeInfo annotation = getClass().getAnnotation(NodeInfo.class);
        descriptor.addProperty("operator", annotation.shortName());
        return descriptor;
    }

}
