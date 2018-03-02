/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;

public abstract class AbstractFunctionArgumentsNode extends JavaScriptBaseNode {

    public abstract int getCount(VirtualFrame frame);

    public abstract Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta);

    protected abstract AbstractFunctionArgumentsNode copyUninitialized();

    @SuppressWarnings("unchecked")
    public static <T extends AbstractFunctionArgumentsNode> T cloneUninitialized(T node) {
        return node == null ? null : (T) node.copyUninitialized();
    }

    public static AbstractFunctionArgumentsNode materializeArgumentsNode(AbstractFunctionArgumentsNode argumentsNode, SourceSection originalSourceSection) {
        AbstractFunctionArgumentsNode materializedArgumentsNode;
        if (argumentsNode instanceof JSFunctionOneConstantArgumentNode) {
            JSConstantNode constantNode = JSConstantNode.create(((JSFunctionOneConstantArgumentNode) argumentsNode).getValue());
            constantNode.setSourceSection(originalSourceSection);
            materializedArgumentsNode = JSFunctionOneArgumentNode.create(constantNode, false);
        } else {
            materializedArgumentsNode = argumentsNode;
        }
        return materializedArgumentsNode;
    }

}
