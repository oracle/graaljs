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

import java.util.Objects;
import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;

@NodeInfo(shortName = "^")
public abstract class JSBitwiseXorConstantNode extends JSUnaryNode {

    protected final boolean isInt;
    protected final int rightIntValue;
    protected final BigInt rightBigIntValue;

    protected JSBitwiseXorConstantNode(JavaScriptNode left, Object rightValue) {
        super(left);
        if (rightValue instanceof BigInt) {
            this.isInt = false;
            this.rightIntValue = 0;
            this.rightBigIntValue = (BigInt) rightValue;
        } else {
            this.isInt = true;
            this.rightIntValue = (int) rightValue;
            this.rightBigIntValue = null;
        }
    }

    public static JavaScriptNode create(JavaScriptNode left, Object rightValue) {
        return JSBitwiseXorConstantNodeGen.create(left, rightValue);
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
            JSConstantNode constantNode = JSConstantNode.create(isInt ? rightIntValue : rightBigIntValue);
            JavaScriptNode node = JSBitwiseXorNodeGen.create(getOperand(), constantNode);
            transferSourceSectionNoTags(this, constantNode);
            transferSourceSection(this, node);
            return node;
        } else {
            return this;
        }
    }

    public abstract Object executeObject(Object a);

    @Specialization(guards = "isInt")
    protected int doInteger(int a) {
        return a ^ rightIntValue;
    }

    @Specialization(guards = "isInt")
    protected int doDouble(double a, @Cached("create()") JSToInt32Node leftInt32) {
        return doInteger(leftInt32.executeInt(a));
    }

    @Specialization(guards = "!isInt")
    protected void doIntegerThrows(@SuppressWarnings("unused") int a) {
        throw Errors.createTypeErrorCanNotMixBigIntWithOtherTypes();
    }

    @Specialization(guards = "!isInt")
    protected void doDoubleThrows(@SuppressWarnings("unused") double a) {
        throw Errors.createTypeErrorCanNotMixBigIntWithOtherTypes();
    }

    @Specialization(guards = "isInt")
    protected void doBigIntThrows(@SuppressWarnings("unused") BigInt a) {
        throw Errors.createTypeErrorCanNotMixBigIntWithOtherTypes();
    }

    @Specialization(guards = "!isInt")
    protected BigInt doBigInt(BigInt a) {
        return a.xor(rightBigIntValue);
    }

    @Specialization(replaces = {"doInteger", "doDouble", "doBigIntThrows"}, guards = "isInt")
    protected Object doGeneric(Object a,
                    @Cached("create()") JSToNumericNode toNumeric,
                    @Cached("createBinaryProfile()") ConditionProfile profileIsBigInt,
                    @Cached("copyUninitialized()") JavaScriptNode innerXorNode) {
        Object numericA = toNumeric.execute(a);
        if (profileIsBigInt.profile(JSRuntime.isBigInt(numericA))) {
            throw Errors.createTypeErrorCanNotMixBigIntWithOtherTypes();
        } else {
            return ((JSBitwiseXorConstantNode) innerXorNode).executeObject(numericA);
        }
    }

    @Specialization(replaces = {"doIntegerThrows", "doDoubleThrows", "doBigInt"}, guards = "!isInt")
    protected BigInt doGenericBigIntCase(Object a,
                    @Cached("create()") JSToNumericNode toNumeric,
                    @Cached("createBinaryProfile()") ConditionProfile profileIsBigInt) {
        Object numericA = toNumeric.execute(a);
        if (profileIsBigInt.profile(JSRuntime.isBigInt(numericA))) {
            return doBigInt((BigInt) numericA);
        } else {
            throw Errors.createTypeErrorCanNotMixBigIntWithOtherTypes();
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSBitwiseXorConstantNodeGen.create(cloneUninitialized(getOperand()), isInt ? rightIntValue : rightBigIntValue);
    }

    @Override
    public String expressionToString() {
        if (getOperand() != null) {
            return "(" + Objects.toString(getOperand().expressionToString(), INTERMEDIATE_VALUE) + " ^ " + (isInt ? rightIntValue : rightBigIntValue) + ")";
        }
        return null;
    }
}
