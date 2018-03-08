/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import java.util.Objects;
import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.LargeInteger;

/**
 * 11.7.2 The Signed Right Shift Operator ( >> ), special-cased for the step to be a constant
 * integer value.
 */
@NodeInfo(shortName = ">>")
@NodeField(name = "shiftValue", type = int.class)
public abstract class JSRightShiftConstantNode extends JSUnaryNode {

    public abstract int getShiftValue();

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        assert right instanceof JSConstantIntegerNode;
        int shiftValue = ((JSConstantIntegerNode) right).executeInt(null);
        if (left instanceof JSConstantIntegerNode) {
            int leftValue = ((JSConstantIntegerNode) left).executeInt(null);
            return JSConstantNode.createInt(leftValue >> shiftValue);
        }
        Truncatable.truncate(left);
        return JSRightShiftConstantNodeGen.create(left, shiftValue);
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
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(BinaryExpressionTag.class)) {
            // need to call the generated factory directly to avoid constant optimizations
            JSConstantNode constantNode = JSConstantIntegerNode.create(getShiftValue());
            JavaScriptNode node = JSRightShiftNodeGen.create(getOperand(), constantNode);
            transferSourceSectionNoTags(this, constantNode);
            transferSourceSection(this, node);
            return node;
        } else {
            return this;
        }
    }

    public abstract int executeInt(int a);

    @Specialization
    protected int doInteger(int a) {
        return a >> getShiftValue();
    }

    @Specialization
    protected int doLargeInteger(LargeInteger a) {
        return a.intValue() >> getShiftValue();
    }

    @Specialization(replaces = "doInteger")
    protected int doGeneric(Object a,
                    @Cached("create()") JSToInt32Node leftInt32) {
        return leftInt32.executeInt(a) >> getShiftValue();
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == int.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSRightShiftConstantNodeGen.create(cloneUninitialized(getOperand()), getShiftValue());
    }

    @Override
    public String expressionToString() {
        if (getOperand() != null) {
            return "(" + Objects.toString(getOperand().expressionToString(), INTERMEDIATE_VALUE) + " >> " + getShiftValue() + ")";
        }
        return null;
    }
}
