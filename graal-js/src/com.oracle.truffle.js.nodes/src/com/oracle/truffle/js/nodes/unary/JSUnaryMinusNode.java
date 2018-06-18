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
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

@NodeInfo(shortName = "-")
public abstract class JSUnaryMinusNode extends JSUnaryNode {

    protected JSUnaryMinusNode(JavaScriptNode operand) {
        super(operand);
    }

    public static JavaScriptNode create(JavaScriptNode operand) {
        if (JSTruffleOptions.UseSuperOperations && operand instanceof JSConstantIntegerNode) {
            int value = ((JSConstantIntegerNode) operand).executeInt(null);
            if (value == 0) {
                return JSConstantNode.createDouble(-0.0); // negative zero
            } else {
                return JSConstantNode.createInt(-value);
            }
        }
        return JSUnaryMinusNodeGen.create(operand);
    }

    protected static JSUnaryMinusNode create() {
        return JSUnaryMinusNodeGen.create(null);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == UnaryExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("operator", getClass().getAnnotation(NodeInfo.class).shortName());
    }

    public abstract Object execute(Object value);

    @Specialization(guards = "isInt(a)")
    protected static int doInt(int a) {
        return -a;
    }

    protected static boolean isInt(int a) {
        return a > Integer.MIN_VALUE && a != 0;
    }

    @Specialization
    protected static double doDouble(double a) {
        return -a;
    }

    @Specialization
    protected static BigInt doBigInt(BigInt a) {
        return a.negate();
    }

    @Specialization
    protected static Object doGeneric(Object a,
                    @Cached("create()") JSToNumericNode toNumericNode,
                    @Cached("create()") JSUnaryMinusNode recursiveUnaryMinus) {
        Object value = toNumericNode.execute(a);
        return recursiveUnaryMinus.execute(value);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Number.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSUnaryMinusNodeGen.create(cloneUninitialized(getOperand()));
    }
}
