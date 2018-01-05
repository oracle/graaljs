/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.runtime.JSRuntime;

@NodeInfo(shortName = "*")
public abstract class JSMultiplyNode extends JSBinaryNode {

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        if (left instanceof JSConstantIntegerNode && right instanceof JSConstantIntegerNode) {
            int leftValue = ((JSConstantIntegerNode) left).executeInt(null);
            int rightValue = ((JSConstantIntegerNode) right).executeInt(null);
            long result = (long) leftValue * (long) rightValue;
            if (result == 0 && (leftValue < 0 || rightValue < 0)) {
                return JSConstantNode.createDouble(-0.0);
            } else if (JSRuntime.longIsRepresentableAsInt(result)) {
                return JSConstantNode.createInt((int) result);
            } else {
                return JSConstantNode.createDouble(result);
            }
        }
        return JSMultiplyNodeGen.create(left, right);
    }

    public static JSMultiplyNode create() {
        return (JSMultiplyNode) create(null, null);
    }

    public abstract Object execute(Object a, Object b);

    @Specialization(guards = "b > 0", rewriteOn = ArithmeticException.class)
    protected int doIntBLargerZero(int a, int b) {
        return Math.multiplyExact(a, b);
    }

    @Specialization(guards = "a > 0", rewriteOn = ArithmeticException.class)
    protected int doIntALargerZero(int a, int b) {
        return Math.multiplyExact(a, b);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    protected int doInt(int a, int b, //
                    @Cached("create()") BranchProfile resultZeroBranch) {
        int result = Math.multiplyExact(a, b);
        if (result == 0) {
            resultZeroBranch.enter();
            if (a < 0 || b < 0) {
                throw new ArithmeticException("could be -0");
            }
        }
        return result;
    }

    @Specialization
    protected double doDouble(double a, double b) {
        return a * b;
    }

    @Specialization(replaces = "doDouble")
    protected Object doGeneric(Object a, Object b,
                    @Cached("create()") JSMultiplyNode nestedMultiplyNode,
                    @Cached("create()") JSToNumberNode toNumber1Node,
                    @Cached("create()") JSToNumberNode toNumber2Node) {
        return nestedMultiplyNode.execute(toNumber1Node.execute(a), toNumber2Node.execute(b));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Number.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSMultiplyNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
