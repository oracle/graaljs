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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.cast.JSToNumericNodeGen.JSToNumericWrapperNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;

public abstract class JSToNumericNode extends JSUnaryNode {

    @Child private JSToNumberNode toNumberNode;
    @Child private JSToPrimitiveNode toPrimitiveNode;

    public abstract Object execute(Object value);

    protected JSToNumericNode(JavaScriptNode operand) {
        super(operand);
    }

    public final Object executeObject(Object value) {
        return execute(value);
    }

    public static JSToNumericNode create() {
        return JSToNumericNodeGen.create(null);
    }

    public static JavaScriptNode create(JavaScriptNode child) {
        if (child.isResultAlwaysOfType(Number.class)) {
            return child;
        }
        if (child instanceof JSConstantNode) {
            Object constantOperand = ((JSConstantNode) child).getValue();
            if (constantOperand != null && !(constantOperand instanceof Symbol) && JSRuntime.isJSPrimitive(constantOperand)) {
                if (constantOperand instanceof BigInt) {
                    return JSConstantNode.createBigInt((BigInt) constantOperand);
                } else {
                    return JSConstantNode.createInt(JSRuntime.toInt32(constantOperand));
                }
            }
        }
        return JSToNumericNodeGen.create(child);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(getOperand()));
    }

    @Specialization
    protected Object doBigInt(BigInt value) {
        return value;
    }

    @Specialization(guards = "isJSBigInt(value)")
    protected Object doJSBigInt(Object value) {
        return toPrimitive(value);
    }

    @Specialization(guards = "!isJSBigInt(value)")
    protected Object doOther(Object value) {
        Object primValue = toPrimitive(value);
        if (JSRuntime.isBigInt(primValue)) {
            return primValue;
        }
        return toNumber(primValue);
    }

    private Number toNumber(Object value) {
        if (toNumberNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toNumberNode = insert(JSToNumberNode.create());
        }
        return toNumberNode.executeNumber(value);
    }

    private Object toPrimitive(Object value) {
        if (toPrimitiveNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toPrimitiveNode = insert(JSToPrimitiveNode.createHintNumber());
        }
        return toPrimitiveNode.execute(value);
    }

    public abstract static class JSToNumericWrapperNode extends JSUnaryNode {

        @Child private JSToNumericNode toNumericNode;

        protected JSToNumericWrapperNode(JavaScriptNode operand) {
            super(operand);
        }

        public static JavaScriptNode create(JavaScriptNode child) {
            if (child.isResultAlwaysOfType(Number.class) || child.isResultAlwaysOfType(int.class) || child.isResultAlwaysOfType(double.class)) {
                return child;
            }
            return JSToNumericWrapperNodeGen.create(child);
        }

        @Specialization
        protected Object doDefault(Object value) {
            if (toNumericNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toNumericNode = insert(JSToNumericNode.create());
            }
            return toNumericNode.executeObject(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return create(cloneUninitialized(getOperand()));
        }

        @Override
        public String expressionToString() {
            return getOperand().expressionToString();
        }
    }
}
