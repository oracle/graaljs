/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import java.util.Objects;
import java.util.Set;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags;
import com.oracle.truffle.js.nodes.tags.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.BinaryOperationTag;

@NodeChildren({@NodeChild("left"), @NodeChild("right")})
public abstract class JSBinaryNode extends JavaScriptNode {

    protected abstract JavaScriptNode getLeft();

    protected abstract JavaScriptNode getRight();

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
        if (tag == BinaryOperationTag.class) {
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

}
