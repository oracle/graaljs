/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;

@NodeInfo(shortName = "/")
public abstract class JSDivideNode extends JSBinaryNode {

    protected JSDivideNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JSDivideNode create(JavaScriptNode left, JavaScriptNode right) {
        return JSDivideNodeGen.create(left, right);
    }

    public static JSDivideNode create() {
        return create(null, null);
    }

    public abstract Object execute(Object a, Object b);

    // otherwise, explicitly check for cornercase
    protected static boolean isCornercase(int a, int b) {
        return a != 0 && !(b == -1 && a == Integer.MIN_VALUE);
    }

    // when b is positive, the result will fit int (if without remainder)
    @Specialization(rewriteOn = ArithmeticException.class, guards = "b > 0")
    protected int doInt1(int a, int b) {
        if (a % b == 0) {
            return a / b;
        }
        throw new ArithmeticException();
    }

    // otherwise, ensure a > 0 (this also excludes two cornercases):
    // when a == 0, result would be NegativeZero
    // when a == Integer.MIN_VALUE && b == -1, result does not fit into int
    @Specialization(rewriteOn = ArithmeticException.class, guards = "a > 0")
    protected int doInt2(int a, int b) {
        return doInt1(a, b);
    }

    @Specialization(rewriteOn = ArithmeticException.class, guards = "isCornercase(a, b)")
    protected int doInt3(int a, int b) {
        return doInt1(a, b);
    }

    @Specialization(replaces = {"doInt1", "doInt2", "doInt3"})
    protected double doDouble(double a, double b) {
        return a / b;
    }

    @Specialization(replaces = "doDouble")
    protected Object doGeneric(Object a, Object b,
                    @Cached("create()") JSDivideNode nestedDivideNode,
                    @Cached("create()") JSToNumberNode toNumber1Node,
                    @Cached("create()") JSToNumberNode toNumber2Node) {
        return nestedDivideNode.execute(toNumber1Node.execute(a), toNumber2Node.execute(b));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Number.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSDivideNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
