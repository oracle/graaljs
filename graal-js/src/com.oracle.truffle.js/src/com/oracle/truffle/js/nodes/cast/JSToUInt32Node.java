/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantBooleanNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantDoubleNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantNullNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantStringNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantUndefinedNode;
import com.oracle.truffle.js.nodes.binary.JSOverloadedBinaryNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32NodeGen.JSToUInt32WrapperNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSOverloadedOperatorsObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

public abstract class JSToUInt32Node extends JavaScriptBaseNode {

    /**
     * Whether this node is used to implement {@code x >>> n}, where {@code n % 32 == 0}. Such an
     * optimization is only valid as long as {@code x} does not overload operators (in which case we
     * would expect either a TypeError or some custom overloaded behavior).
     */
    private final boolean unsignedRightShift;
    /**
     * If {@link #unsignedRightShift} is {@code true}, this value stores the amount {@code n} by
     * which the argument is to be shifted to the right. If we encounter an object with overloaded
     * unsigned right shift, this is the value that we will pass to the overload.
     */
    private final int shiftValue;

    protected JSToUInt32Node(boolean unsignedRightShift, int shiftValue) {
        this.unsignedRightShift = unsignedRightShift;
        this.shiftValue = shiftValue;
    }

    public static JSToUInt32Node create() {
        return JSToUInt32NodeGen.create(false, 0);
    }

    public static JSToUInt32Node create(boolean unsignedRightShift, int shiftValue) {
        return JSToUInt32NodeGen.create(unsignedRightShift, shiftValue);
    }

    public abstract Object execute(Object value);

    public final long executeLong(Object value) {
        return JSRuntime.longValue((Number) execute(value));
    }

    @Specialization(guards = "value >= 0")
    protected int doInteger(int value) {
        return value;
    }

    @Specialization(guards = "value < 0")
    protected SafeInteger doIntegerNegative(int value) {
        return SafeInteger.valueOf(value & 0x0000_0000_FFFF_FFFFL);
    }

    @Specialization
    protected Object doSafeInteger(SafeInteger value) {
        long lValue = value.longValue() & 0x0000_0000_FFFF_FFFFL;
        if (lValue > Integer.MAX_VALUE) {
            return SafeInteger.valueOf(lValue);
        }
        return (int) lValue;
    }

    @Specialization
    protected int doBoolean(boolean value) {
        return doBooleanStatic(value);
    }

    private static int doBooleanStatic(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    @Specialization
    protected double doLong(long value) {
        return JSRuntime.toUInt32(value);
    }

    @Specialization(guards = {"!isDoubleLargerThan2e32(value)"})
    protected double doDoubleFitsInt32Negative(double value) {
        return JSRuntime.toUInt32((long) value);
    }

    @Specialization(guards = {"isDoubleLargerThan2e32(value)", "isDoubleRepresentableAsLong(value)"})
    protected double doDoubleRepresentableAsLong(double value) {
        return JSRuntime.toUInt32NoTruncate(value);
    }

    @Specialization(guards = {"isDoubleLargerThan2e32(value)", "!isDoubleRepresentableAsLong(value)"})
    protected double doDouble(double value) {
        return JSRuntime.toUInt32(value);
    }

    @Specialization(guards = "isJSNull(value)")
    protected int doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization(guards = "isUndefined(value)")
    protected int doUndefined(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization
    protected double doString(TruffleString value,
                    @Cached("create()") JSStringToNumberNode stringToNumberNode) {
        return JSRuntime.toUInt32(stringToNumberNode.executeString(value));
    }

    private static double doStringStatic(TruffleString value) {
        return JSRuntime.toUInt32(JSRuntime.doubleValue(JSRuntime.stringToNumber(value)));
    }

    @Specialization
    protected final Number doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value", this);
    }

    @Specialization
    protected int doBigInt(@SuppressWarnings("unused") BigInt value) {
        throw Errors.createTypeErrorCannotConvertBigIntToNumber(this);
    }

    protected boolean isUnsignedRightShift() {
        return unsignedRightShift;
    }

    @Specialization(guards = {"isUnsignedRightShift()"})
    protected Object doOverloadedOperator(JSOverloadedOperatorsObject value,
                    @Cached("createNumeric(getOverloadedOperatorName())") JSOverloadedBinaryNode overloadedOperatorNode) {
        return overloadedOperatorNode.execute(value, shiftValue);
    }

    protected TruffleString getOverloadedOperatorName() {
        return Strings.ANGLE_BRACKET_CLOSE_3;
    }

    @Specialization(guards = {"isJSObject(value)", "!isUnsignedRightShift() || !hasOverloadedOperators(value)"})
    protected double doJSObject(JSDynamicObject value,
                    @Cached("create()") JSToNumberNode toNumberNode) {
        return JSRuntime.toUInt32(toNumberNode.executeNumber(value));
    }

    @TruffleBoundary
    @Specialization(guards = "isForeignObject(object)")
    protected static double doForeignObject(Object object,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitiveNode,
                    @Cached("create()") JSToUInt32Node toUInt32Node) {
        return ((Number) toUInt32Node.execute(toPrimitiveNode.execute(object))).doubleValue();
    }

    public abstract static class JSToUInt32WrapperNode extends JSUnaryNode {
        @Child private JSToUInt32Node toUInt32Node;
        private final boolean unsignedRightShift;
        private final int shiftValue;

        protected JSToUInt32WrapperNode(JavaScriptNode operand, boolean unsignedRightShift, int shiftValue) {
            super(operand);
            this.unsignedRightShift = unsignedRightShift;
            this.shiftValue = shiftValue;
        }

        public static JavaScriptNode create(JavaScriptNode child) {
            return create(child, false, 0);
        }

        public static JavaScriptNode create(JavaScriptNode child, boolean unsignedRightShift, int shiftValue) {
            if (child instanceof JSConstantIntegerNode) {
                int value = ((JSConstantIntegerNode) child).executeInt(null);
                if (value < 0) {
                    long lValue = JSRuntime.toUInt32(value);
                    return JSRuntime.longIsRepresentableAsInt(lValue) ? JSConstantNode.createInt((int) lValue) : JSConstantNode.createDouble(lValue);
                }
                return child;
            } else if (child instanceof JSConstantDoubleNode) {
                double value = ((JSConstantDoubleNode) child).executeDouble(null);
                return JSConstantNode.createDouble(JSRuntime.toUInt32(value));
            } else if (child instanceof JSConstantBooleanNode) {
                boolean value = ((JSConstantBooleanNode) child).executeBoolean(null);
                return JSConstantNode.createInt(doBooleanStatic(value));
            } else if (child instanceof JSConstantUndefinedNode || child instanceof JSConstantNullNode) {
                return JSConstantNode.createInt(0);
            } else if (child instanceof JSConstantStringNode) {
                Object value = child.execute(null);
                return JSConstantNode.createDouble(doStringStatic((TruffleString) value));
            } else if (child instanceof JSToInt32Node) {
                JSToInt32Node toInt32Child = (JSToInt32Node) child;
                if (toInt32Child.isBitwiseOr() && unsignedRightShift) {
                    return JSToUInt32WrapperNodeGen.create(toInt32Child, unsignedRightShift, shiftValue);
                } else {
                    return JSToUInt32WrapperNodeGen.create(toInt32Child.getOperand());
                }
            }
            return JSToUInt32WrapperNodeGen.create(child, unsignedRightShift, shiftValue);
        }

        @Specialization
        protected Object doDefault(Object value) {
            if (toUInt32Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toUInt32Node = insert(JSToUInt32Node.create(unsignedRightShift, shiftValue));
            }
            return toUInt32Node.execute(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return JSToUInt32WrapperNodeGen.create(cloneUninitialized(getOperand(), materializedTags), unsignedRightShift, shiftValue);
        }
    }
}
