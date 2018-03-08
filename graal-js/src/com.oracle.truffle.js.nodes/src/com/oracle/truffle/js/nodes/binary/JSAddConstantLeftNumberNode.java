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
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.JSRuntime;

@NodeInfo(shortName = "+")
public abstract class JSAddConstantLeftNumberNode extends JSUnaryNode implements Truncatable {

    @CompilationFinal boolean truncate;
    private final double leftDouble;
    private final int leftInt;
    protected final boolean isInt;
    protected final boolean isSafeLong;

    public JSAddConstantLeftNumberNode(boolean truncate, Number leftValue) {
        this.truncate = truncate;
        leftDouble = leftValue.doubleValue();
        leftInt = (int) leftValue.longValue(); // avoid narrowing
        isSafeLong = JSRuntime.doubleIsRepresentableAsLong(leftDouble) && JSRuntime.isSafeInteger(leftDouble);
        isInt = leftValue instanceof Integer || JSRuntime.doubleIsRepresentableAsInt(leftDouble);
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
            JSConstantNode constantNode = isInt ? JSConstantIntegerNode.create(leftInt) : JSConstantDoubleNode.create(leftDouble);
            JavaScriptNode node = JSAddNodeGen.create(truncate, constantNode, getOperand());
            transferSourceSectionNoTags(this, constantNode);
            transferSourceSection(this, node);
            return node;
        } else {
            return this;
        }
    }

    public abstract Object execute(Object a);

    public Number getLeftValue() {
        return isInt ? leftInt : leftDouble;
    }

    @Specialization(guards = {"truncate", "isInt || isSafeLong"})
    protected int doIntTruncate(int right) {
        return leftInt + right;
    }

    @Specialization(guards = {"!truncate", "isInt"}, rewriteOn = ArithmeticException.class)
    protected int doInt(int right) {
        return Math.addExact(leftInt, right);
    }

    @Specialization(guards = {"!truncate", "isSafeLong"}, rewriteOn = ArithmeticException.class)
    protected Object doIntOverflow(int right) {
        long result = (long) leftDouble + right;
        return JSAddNode.doIntOverflowStaticLong(result);
    }

    @Specialization
    protected double doDouble(double right) {
        return leftDouble + right;
    }

    @Specialization
    protected CharSequence doNumberString(CharSequence right,
                    @Cached("leftValueToString()") String leftString,
                    @Cached("create()") JSConcatStringsNode createLazyString) {
        return createLazyString.executeCharSequence(leftString, right);
    }

    @Specialization(replaces = {"doInt", "doDouble", "doNumberString"})
    protected Object doPrimitiveConversion(Object right,
                    @Cached("createHintNone()") JSToPrimitiveNode toPrimitiveB,
                    @Cached("create()") JSToNumberNode toNumberB,
                    @Cached("leftValueToString()") String leftString,
                    @Cached("create()") JSConcatStringsNode createLazyString,
                    @Cached("createBinaryProfile()") ConditionProfile profileB) {

        Object primitiveRight = toPrimitiveB.execute(right);

        if (profileB.profile(isString(primitiveRight))) {
            return createLazyString.executeCharSequence(leftString, (CharSequence) primitiveRight);
        } else {
            return leftDouble + JSRuntime.doubleValue(toNumberB.executeNumber(primitiveRight));
        }
    }

    protected String leftValueToString() {
        return JSRuntime.toString(getLeftValue());
    }

    @Override
    public void setTruncate() {
        CompilerAsserts.neverPartOfCompilation();
        if (truncate == false) {
            truncate = true;
            Truncatable.truncate(getOperand());
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSAddConstantLeftNumberNodeGen.create(truncate, getLeftValue(), cloneUninitialized(getOperand()));
    }

    @Override
    public String expressionToString() {
        if (getOperand() != null) {
            return "(" + JSRuntime.numberToString(getLeftValue()) + " + " + Objects.toString(getOperand().expressionToString(), INTERMEDIATE_VALUE) + ")";
        }
        return null;
    }
}
