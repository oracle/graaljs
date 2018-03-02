/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

@NodeInfo(shortName = "void", cost = NodeCost.NONE)
public abstract class VoidNode extends JSUnaryNode {

    public static JavaScriptNode create(JavaScriptNode operand) {
        if (operand.isResultAlwaysOfType(Undefined.class)) {
            return operand;
        }
        if (operand instanceof JSConstantNode) {
            // e.g. default parameters
            return JSConstantNode.createUndefined();
        }
        return VoidNodeGen.create(operand);
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
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Undefined.class;
    }

    @Specialization
    protected Object doGeneric(@SuppressWarnings("unused") Object operand) {
        return Undefined.instance;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return VoidNodeGen.create(cloneUninitialized(getOperand()));
    }
}
