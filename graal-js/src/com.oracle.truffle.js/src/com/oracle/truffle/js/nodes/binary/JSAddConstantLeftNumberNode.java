/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryOperationTag;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSOverloadedOperatorsObject;

@NodeInfo(shortName = "+")
public abstract class JSAddConstantLeftNumberNode extends JSUnaryNode implements Truncatable {

    @CompilationFinal boolean truncate;
    private final double leftDouble;
    private final int leftInt;
    protected final boolean isInt;
    protected final boolean isSafeLong;

    protected JSAddConstantLeftNumberNode(Number leftValue, JavaScriptNode right, boolean truncate) {
        super(right);
        this.truncate = truncate;
        this.leftDouble = leftValue.doubleValue();
        this.leftInt = (int) leftValue.longValue(); // avoid narrowing
        this.isSafeLong = JSRuntime.doubleIsRepresentableAsLong(leftDouble) && JSRuntime.isSafeInteger(leftDouble);
        this.isInt = leftValue instanceof Integer || JSRuntime.doubleIsRepresentableAsInt(leftDouble);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == BinaryOperationTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(BinaryOperationTag.class)) {
            JSConstantNode constantNode = isInt ? JSConstantNode.createInt(leftInt) : JSConstantNode.createDouble(leftDouble);
            JavaScriptNode node = JSAddNode.createUnoptimized(constantNode, cloneUninitialized(getOperand(), materializedTags), truncate);
            transferSourceSectionAddExpressionTag(this, constantNode);
            transferSourceSectionAndTags(this, node);
            return node;
        } else {
            return this;
        }
    }

    public Number getLeftValue() {
        return isInt ? leftInt : leftDouble;
    }

    @Specialization(guards = {"truncate", "isInt"})
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

    @Specialization(guards = {"isInt"}, rewriteOn = ArithmeticException.class)
    protected SafeInteger doSafeInteger(SafeInteger right) {
        return SafeInteger.valueOf(leftInt).addExact(right);
    }

    @Specialization
    protected double doDouble(double right) {
        return leftDouble + right;
    }

    @Specialization
    protected Object doNumberString(TruffleString right,
                    @Cached("leftValueToString()") @Shared TruffleString leftString,
                    @Cached @Shared JSConcatStringsNode createLazyString) {
        return createLazyString.executeTString(leftString, right);
    }

    @InliningCutoff
    @Specialization
    protected Object doOverloaded(JSOverloadedOperatorsObject right,
                    @Cached("createHintDefault(getOverloadedOperatorName())") JSOverloadedBinaryNode overloadedOperatorNode) {
        return overloadedOperatorNode.execute(getLeftValue(), right);
    }

    protected TruffleString getOverloadedOperatorName() {
        return Strings.SYMBOL_PLUS;
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"!hasOverloadedOperators(right)"}, replaces = {"doInt", "doDouble", "doNumberString"})
    protected Object doPrimitiveConversion(Object right,
                    @Bind Node node,
                    @Cached JSToPrimitiveNode toPrimitiveB,
                    @Cached JSToNumberNode toNumberB,
                    @Cached("leftValueToString()") @Shared TruffleString leftString,
                    @Cached @Shared JSConcatStringsNode createLazyString,
                    @Cached InlinedConditionProfile profileB) {

        Object primitiveRight = toPrimitiveB.executeHintDefault(right);

        if (profileB.profile(node, isString(primitiveRight))) {
            return createLazyString.executeTString(leftString, (TruffleString) primitiveRight);
        } else {
            return leftDouble + JSRuntime.doubleValue(toNumberB.executeNumber(primitiveRight));
        }
    }

    @NeverDefault
    protected TruffleString leftValueToString() {
        return JSRuntime.toString(getLeftValue());
    }

    @Override
    public void setTruncate() {
        CompilerAsserts.neverPartOfCompilation();
        if (!truncate) {
            truncate = true;
            if (isInt) {
                Truncatable.truncate(getOperand());
            }
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSAddConstantLeftNumberNodeGen.create(getLeftValue(), cloneUninitialized(getOperand(), materializedTags), truncate);
    }

    @Override
    public String expressionToString() {
        if (getOperand() != null) {
            return "(" + JSRuntime.numberToString(getLeftValue()) + " + " + Objects.toString(getOperand().expressionToString(), INTERMEDIATE_VALUE) + ")";
        }
        return null;
    }
}
