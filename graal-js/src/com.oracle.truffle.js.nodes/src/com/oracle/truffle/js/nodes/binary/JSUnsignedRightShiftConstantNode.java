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
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node.JSToUInt32WrapperNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.LargeInteger;

/**
 * 11.7.3 The Unsigned Right Shift Operator (>>>).
 */
@NodeInfo(shortName = ">>>")
@NodeField(name = "shiftValue", type = int.class)
@NodeField(name = "rightValue", type = int.class)
public abstract class JSUnsignedRightShiftConstantNode extends JSUnaryNode {

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        assert right instanceof JSConstantIntegerNode;
        int rightValue = ((JSConstantIntegerNode) right).executeInt(null);
        int shiftValue = rightValue & 0x1F;
        if (shiftValue == 0) {
            return JSToUInt32WrapperNode.create(left);
        }
        if (left instanceof JSConstantIntegerNode) {
            int leftValue = ((JSConstantIntegerNode) left).executeInt(null);
            return JSConstantNode.createInt(leftValue >>> shiftValue);
        }
        return JSUnsignedRightShiftConstantNodeGen.create(left, shiftValue, rightValue);
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
            JSConstantNode constantNode = JSConstantIntegerNode.create(getRightValue());
            JavaScriptNode node = JSUnsignedRightShiftNodeGen.create(getOperand(), constantNode);
            transferSourceSection(this, constantNode);
            transferSourceSection(this, node);
            return node;
        } else {
            return this;
        }
    }

    protected abstract int getShiftValue();

    protected abstract int getRightValue();

    @Specialization
    protected int doInteger(int a) {
        assert getShiftValue() > 0;
        return a >>> getShiftValue();
    }

    @Specialization
    protected int doLargeInteger(LargeInteger a) {
        assert getShiftValue() > 0;
        return a.intValue() >>> getShiftValue();
    }

    @Specialization
    protected int doDouble(double a,
                    @Cached("create()") JSToUInt32Node toUInt32Node) {
        return (int) (toUInt32Node.executeLong(a) >>> getShiftValue());
    }

    @Specialization(guards = "!isHandled(lval)")
    protected Object doGeneric(Object lval,
                    @Cached("create()") JSToUInt32Node toUInt32Node) {
        long lnum = toUInt32Node.executeLong(lval);
        if (lnum >= Integer.MAX_VALUE || lnum <= Integer.MIN_VALUE) {
            return (double) (lnum >>> getShiftValue());
        }
        return (int) (lnum >>> getShiftValue());
    }

    protected static boolean isHandled(Object lval) {
        return lval instanceof Integer || lval instanceof Double;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Number.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSUnsignedRightShiftConstantNodeGen.create(cloneUninitialized(getOperand()), getShiftValue(), getRightValue());
    }

    @Override
    public String expressionToString() {
        if (getOperand() != null) {
            return "(" + Objects.toString(getOperand().expressionToString(), INTERMEDIATE_VALUE) + " >>> " + getShiftValue() + ")";
        }
        return null;
    }
}
