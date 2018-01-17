/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags;
import com.oracle.truffle.js.nodes.tags.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.UnaryOperationTag;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

@NodeInfo(shortName = "-")
public abstract class JSUnaryMinusNode extends JSUnaryNode {

    public static JavaScriptNode create(JavaScriptNode operand) {
        if (JSTruffleOptions.UseSuperOperations && operand instanceof JSConstantIntegerNode) {
            int value = ((JSConstantIntegerNode) operand).executeInt(null);
            if (value == 0) {
                return JSConstantNode.createDouble(-0.0); // negative zero
            } else {
                return JSConstantNode.createInt(-value);
            }
        }
        return JSUnaryMinusNodeGen.create(operand);
    }

    protected static JSUnaryMinusNode create() {
        return JSUnaryMinusNodeGen.create(null);
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

    public abstract Object execute(Object value);

    @Specialization(guards = "isInt(a)")
    protected static int doInt(int a) {
        return -a;
    }

    protected static boolean isInt(int a) {
        return a > Integer.MIN_VALUE && a != 0;
    }

    @Specialization
    protected static double doDouble(double a) {
        return -a;
    }

    @Specialization
    protected static Object doGeneric(Object a,
                    @Cached("create()") JSToNumberNode toNumberNode,
                    @Cached("create()") JSUnaryMinusNode recursiveUnaryMinus) {
        Object value = toNumberNode.execute(a);
        return recursiveUnaryMinus.execute(value);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Number.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSUnaryMinusNodeGen.create(cloneUninitialized(getOperand()));
    }
}
