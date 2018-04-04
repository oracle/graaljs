/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import static com.oracle.truffle.js.nodes.JSGuards.isString;

import java.util.Objects;
import java.util.Set;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantDoubleNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.JSRuntime;

@NodeInfo(shortName = "+")
public abstract class JSAddConstantRightNumberNode extends JSUnaryNode implements Truncatable {

    @CompilationFinal boolean truncate;
    private final double rightDouble;
    private final int rightInt;
    protected final boolean isInt;

    protected JSAddConstantRightNumberNode(JavaScriptNode left, Number rightValue, boolean truncate) {
        super(left);
        this.truncate = truncate;
        this.rightDouble = rightValue.doubleValue();
        if (rightValue instanceof Integer || JSRuntime.doubleIsRepresentableAsInt(rightDouble)) {
            this.isInt = true;
            this.rightInt = rightValue.intValue();
        } else {
            this.isInt = false;
            this.rightInt = 0;
        }
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
        return JSTags.createNodeObjectDescriptor("operator", getClass().getAnnotation(NodeInfo.class).shortName());
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(BinaryExpressionTag.class)) {
            JSConstantNode constantNode = isInt ? JSConstantIntegerNode.create(rightInt) : JSConstantDoubleNode.create(rightDouble);
            JavaScriptNode node = JSAddNode.createUnoptimized(getOperand(), constantNode, truncate);
            transferSourceSectionNoTags(this, constantNode);
            transferSourceSection(this, node);
            return node;
        } else {
            return this;
        }
    }

    public abstract Object execute(Object a);

    public Number getRightValue() {
        return isInt ? rightInt : rightDouble;
    }

    @Specialization(guards = {"truncate", "isInt"})
    protected int doIntTruncate(int left) {
        return left + rightInt;
    }

    @Specialization(guards = {"!truncate", "isInt"}, rewriteOn = ArithmeticException.class)
    protected int doInt(int left) {
        return Math.addExact(left, rightInt);
    }

    @Specialization(guards = {"!truncate", "isInt"}, rewriteOn = ArithmeticException.class)
    protected Object doIntOverflow(int left) {
        long result = (long) left + (long) rightInt;
        return JSAddNode.doIntOverflowStaticLong(result);
    }

    @Specialization
    protected double doDouble(double left) {
        return left + rightDouble;
    }

    @Specialization
    protected CharSequence doStringNumber(CharSequence a,
                    @Cached("rightValueToString()") String rightString,
                    @Cached("create()") JSConcatStringsNode createLazyString) {
        return createLazyString.executeCharSequence(a, rightString);
    }

    @Specialization(replaces = {"doInt", "doDouble", "doStringNumber"})
    protected Object doPrimitiveConversion(Object a,
                    @Cached("createHintNone()") JSToPrimitiveNode toPrimitiveA,
                    @Cached("create()") JSToNumberNode toNumberA,
                    @Cached("rightValueToString()") String rightString,
                    @Cached("create()") JSConcatStringsNode createLazyString,
                    @Cached("createBinaryProfile()") ConditionProfile profileA) {

        Object primitiveA = toPrimitiveA.execute(a);

        if (profileA.profile(isString(primitiveA))) {
            return createLazyString.executeCharSequence((CharSequence) primitiveA, rightString);
        } else {
            return JSRuntime.doubleValue(toNumberA.executeNumber(primitiveA)) + rightDouble;
        }
    }

    protected String rightValueToString() {
        return JSRuntime.toString(getRightValue());
    }

    @Override
    public void setTruncate() {
        CompilerAsserts.neverPartOfCompilation();
        if (truncate == false) {
            truncate = true;
            if (isInt) {
                Truncatable.truncate(getOperand());
            }
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSAddConstantRightNumberNodeGen.create(cloneUninitialized(getOperand()), getRightValue(), truncate);
    }

    @Override
    public String expressionToString() {
        if (getOperand() != null) {
            return "(" + Objects.toString(getOperand().expressionToString(), INTERMEDIATE_VALUE) + " + " + JSRuntime.numberToString(getRightValue()) + ")";
        }
        return null;
    }
}
