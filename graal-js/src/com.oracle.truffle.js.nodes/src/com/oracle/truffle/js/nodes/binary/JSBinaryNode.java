/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import java.util.Objects;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;

@NodeChildren({@NodeChild("left"), @NodeChild("right")})
public abstract class JSBinaryNode extends JavaScriptNode {

    public abstract JavaScriptNode getLeft();

    public abstract JavaScriptNode getRight();

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
}
