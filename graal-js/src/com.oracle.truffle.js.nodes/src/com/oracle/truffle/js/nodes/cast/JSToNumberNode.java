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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNode.JSStringToNumberWithTrimNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNodeGen.JSToNumberWrapperNodeGen;
import com.oracle.truffle.js.nodes.interop.JSUnboxOrGetNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * This implements ECMA 9.3 ToNumber.
 *
 */
public abstract class JSToNumberNode extends JavaScriptBaseNode {

    @Child private JSToNumberNode toNumberNode;
    @Child private JSStringToNumberWithTrimNode stringToNumberNode;

    public abstract Object execute(Object value);

    public final Number executeNumber(Object value) {
        return (Number) execute(value);
    }

    public static JSToNumberNode create() {
        return JSToNumberNodeGen.create();
    }

    @Specialization
    protected static int doInteger(int value) {
        return value;
    }

    @Specialization
    protected static int doBoolean(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    @Specialization
    protected static double doDouble(double value) {
        return value;
    }

    @Specialization
    protected static void doBigInt(@SuppressWarnings("unused") BigInt value) {
        throw Errors.createTypeErrorCanNotMixBigIntWithOtherTypes();
    }

    @Specialization(guards = "isJSNull(value)")
    protected static int doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization(guards = "isUndefined(value)")
    protected static double doUndefined(@SuppressWarnings("unused") Object value) {
        return Double.NaN;
    }

    @Specialization(rewriteOn = SlowPathException.class)
    protected int doStringInt(String value) throws SlowPathException {
        double doubleValue = stringToNumber(value);
        if (!JSRuntime.doubleIsRepresentableAsInt(doubleValue)) {
            throw new SlowPathException();
        }
        return (int) doubleValue;
    }

    @Specialization
    protected double doStringDouble(String value) {
        return stringToNumber(value);
    }

    @Specialization(guards = "isJSObject(value)")
    protected Number doJSObject(DynamicObject value,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitiveNode) {
        return toNumber(toPrimitiveNode.execute(value));
    }

    @Specialization
    protected final Number doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value", this);
    }

    @Specialization(guards = "isForeignObject(object)")
    protected Number doCrossLanguageToDouble(TruffleObject object,
                    @Cached("create()") JSUnboxOrGetNode unboxOrGetNode) {
        return toNumber(unboxOrGetNode.executeWithTarget(object));
    }

    @Specialization(guards = "isJavaNumber(value)")
    protected static double doJavaObject(Object value) {
        return JSRuntime.doubleValue((Number) value);
    }

    private Number toNumber(Object value) {
        if (toNumberNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toNumberNode = insert(JSToNumberNode.create());
        }
        return toNumberNode.executeNumber(value);
    }

    private double stringToNumber(String value) {
        if (stringToNumberNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stringToNumberNode = insert(JSStringToNumberWithTrimNode.create());
        }
        return stringToNumberNode.executeString(value);
    }

    public abstract static class JSToNumberWrapperNode extends JSUnaryNode {

        @Child private JSToNumberNode toNumberNode;

        protected JSToNumberWrapperNode(JavaScriptNode operand) {
            super(operand);
        }

        public static JavaScriptNode create(JavaScriptNode child) {
            if (child.isResultAlwaysOfType(Number.class) || child.isResultAlwaysOfType(int.class) || child.isResultAlwaysOfType(double.class)) {
                return child;
            }
            return JSToNumberWrapperNodeGen.create(child);
        }

        @Specialization
        protected Object doDefault(Object value) {
            if (toNumberNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toNumberNode = insert(JSToNumberNode.create());
            }
            return toNumberNode.executeNumber(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return create(cloneUninitialized(getOperand()));
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return super.isResultAlwaysOfType(Number.class);
        }

        @Override
        public String expressionToString() {
            return getOperand().expressionToString();
        }
    }
}
