/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
            transferSourceSectionAddExpressionTag(this, constantNode);
            transferSourceSectionAndTags(this, node);
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
