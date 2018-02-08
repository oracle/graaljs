/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantBooleanNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

@NodeInfo(shortName = "!")
public abstract class JSNotNode extends JSUnaryNode {

    public static JavaScriptNode create(JavaScriptNode operand) {
        if (JSTruffleOptions.UseSuperOperations && operand instanceof JSNotNode) {
            // optimize "!!operand", but retain conversion to boolean if operand != boolean
            JSNotNode childNode = (JSNotNode) operand;
            JavaScriptNode childOperand = childNode.getOperand();
            if (childOperand.isResultAlwaysOfType(boolean.class)) {
                return childOperand;
            }
        } else if (JSTruffleOptions.UseSuperOperations && operand instanceof JSConstantBooleanNode) {
            boolean value = (boolean) operand.execute(null);
            return JSConstantNode.createBoolean(!value);
        } else if (JSTruffleOptions.UseSuperOperations && operand instanceof JSConstantIntegerNode) {
            // used by minifiers, "!0" is shorter than "true"
            int value = (int) operand.execute(null);
            return JSConstantNode.createBoolean(value == 0); // negating it
        }
        return JSNotNodeGen.create(operand);
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
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (!getOperand().hasSourceSection()) {
            transferSourceSection(this, getOperand());
        }
        return this;
    }

    @Override
    public Object getNodeObject() {
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor();
        descriptor.addProperty("operator", getClass().getAnnotation(NodeInfo.class).shortName());
        return descriptor;
    }

    @Specialization
    protected boolean doBoolean(boolean a) {
        return !a;
    }

    @Specialization
    protected boolean doNonBoolean(Object a,
                    @Cached("create()") JSToBooleanNode toBooleanNode) {
        return !toBooleanNode.executeBoolean(a);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSNotNodeGen.create(cloneUninitialized(getOperand()));
    }
}
