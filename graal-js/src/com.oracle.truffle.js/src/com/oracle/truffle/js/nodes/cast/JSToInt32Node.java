/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.binary.JSOverloadedBinaryNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryOperationTag;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Record;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.builtins.JSOverloadedOperatorsObject;

import java.util.Set;

/**
 * This node implements the behavior of 9.5 ToInt32. Not to confuse with 9.4 ToInteger, etc.
 *
 */
public abstract class JSToInt32Node extends JSUnaryNode {

    /**
     * Whether this node is used to implement {@code x | 0}. This optimization is valid insofar as
     * {@code x} does not overload operators.
     */
    protected final boolean bitwiseOr;

    protected JSToInt32Node(JavaScriptNode operand, boolean bitwiseOr) {
        super(operand);
        this.bitwiseOr = bitwiseOr;
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        return executeInt(frame);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == UnaryOperationTag.class) {
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
    public abstract int executeInt(VirtualFrame frame);

    public abstract int executeInt(Object operand);

    public static JavaScriptNode create(JavaScriptNode child) {
        return create(child, false);
    }

    public static JavaScriptNode create(JavaScriptNode child, boolean bitwiseOr) {
        if (child != null) {
            if (child.isResultAlwaysOfType(int.class)) {
                return child;
            }
            Truncatable.truncate(child);
            if (child instanceof JSConstantNode) {
                Object constantOperand = ((JSConstantNode) child).getValue();
                if (constantOperand != null && !(constantOperand instanceof Symbol) && JSRuntime.isJSPrimitive(constantOperand)) {
                    return JSConstantNode.createInt(JSRuntime.toInt32(constantOperand));
                }
            }
        }
        return JSToInt32NodeGen.create(child, bitwiseOr);
    }

    public static JSToInt32Node create() {
        return JSToInt32NodeGen.create(null, false);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return !bitwiseOr && clazz == int.class;
    }

    @Specialization
    protected int doInteger(int value) {
        return value;
    }

    @Specialization
    protected int doSafeInteger(SafeInteger value) {
        return value.intValue();
    }

    @Specialization
    protected int doBoolean(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    @Specialization(guards = "isLongRepresentableAsInt32(value)")
    protected int doLong(long value) {
        return (int) value;
    }

    @Specialization(guards = "!isDoubleLargerThan2e32(value)")
    protected int doDoubleFitsInt(double value) {
        return (int) (long) value;
    }

    @Specialization(guards = {"isDoubleLargerThan2e32(value)", "isDoubleRepresentableAsLong(value)", "isDoubleSafeInteger(value)"})
    protected int doDoubleRepresentableAsSafeInteger(double value) {
        assert !Double.isFinite(value) || value % 1 == 0;

        assert !Double.isNaN(value);
        assert !JSRuntime.isNegativeZero(value);

        return (int) (long) value;
    }

    @Specialization(guards = {"isDoubleLargerThan2e32(value)", "isDoubleRepresentableAsLong(value)"}, replaces = "doDoubleRepresentableAsSafeInteger")
    protected int doDoubleRepresentableAsLong(double value) {
        assert !Double.isFinite(value) || value % 1 == 0;
        return JSRuntime.toInt32NoTruncate(value);
    }

    @Specialization(guards = {"isDoubleLargerThan2e32(value)", "!isDoubleRepresentableAsLong(value)"})
    protected int doDouble(double value) {
        return JSRuntime.toInt32(value);
    }

    @Specialization(guards = "isUndefined(value)")
    protected int doUndefined(@SuppressWarnings("unused") Object value) {
        return 0; // toNumber() returns NaN, but toInteger() converts that
    }

    @Specialization(guards = "isJSNull(value)")
    protected int doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization
    protected int doString(String value,
                    @Cached("create()") JSStringToNumberNode stringToNumberNode) {
        return doubleToInt32(stringToNumberNode.executeString(value));
    }

    @Specialization
    protected final int doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value", this);
    }

    @Specialization
    protected int doBigInt(@SuppressWarnings("unused") BigInt value) {
        throw Errors.createTypeErrorCannotConvertBigIntToNumber(this);
    }

    public boolean isBitwiseOr() {
        return bitwiseOr;
    }

    @Specialization(guards = {"isBitwiseOr()"})
    protected Object doOverloadedOperator(JSOverloadedOperatorsObject value,
                    @Cached("createNumeric(getOverloadedOperatorName())") JSOverloadedBinaryNode overloadedOperatorNode) {
        return overloadedOperatorNode.execute(value, 0);
    }

    protected String getOverloadedOperatorName() {
        return "|";
    }

    @Specialization
    protected int doRecord(@SuppressWarnings("unused") Record value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Record value", this);
    }

    @Specialization
    protected int doTuple(@SuppressWarnings("unused") Tuple value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Tuple value", this);
    }

    @Specialization(guards = {"isJSObject(value)", "!isBitwiseOr() || !hasOverloadedOperators(value)"})
    protected int doJSObject(DynamicObject value,
                    @Cached("create()") JSToDoubleNode toDoubleNode) {
        return doubleToInt32(toDoubleNode.executeDouble(value));
    }

    private static int doubleToInt32(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d) || d == 0) {
            return 0;
        }
        return JSRuntime.toInt32(d);
    }

    @Specialization(guards = "isForeignObject(object)")
    protected static int doForeignObject(Object object,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitiveNode,
                    @Cached("create()") JSToInt32Node toInt32Node) {
        return toInt32Node.executeInt(toPrimitiveNode.execute(object));
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSToInt32NodeGen.create(cloneUninitialized(getOperand(), materializedTags), bitwiseOr);
    }
}
