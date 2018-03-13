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
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;

@NodeInfo(shortName = "%")
public abstract class JSModuloNode extends JSBinaryNode {

    protected JSModuloNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JSModuloNode create(JavaScriptNode left, JavaScriptNode right) {
        return JSModuloNodeGen.create(left, right);
    }

    public static JSModuloNode create() {
        return create(null, null);
    }

    public abstract Object execute(Object a, Object b);

    static boolean isPowOf2(int b) {
        return (b > 0) && (b & (b - 1)) == 0;
    }

    @Specialization(rewriteOn = ArithmeticException.class, guards = "isPowOf2(b)")
    protected int doIntPow2(int a, int b,
                    @Cached("create()") BranchProfile negativeBranch,
                    @Cached("create()") BranchProfile negativeZeroBranch) {
        int mask = b - 1;
        int result;
        if (a < 0) {
            negativeBranch.enter();
            result = -(-a & mask);
            if (result == 0) {
                negativeZeroBranch.enter();
                throw new ArithmeticException();
            }
        } else {
            result = a & mask;
        }
        return result;
    }

    @Specialization(rewriteOn = ArithmeticException.class, guards = "!isPowOf2(b)")
    protected int doInt(int a, int b,
                    @Cached("create()") BranchProfile specialBranch) {
        int result = a % b;
        if (result == 0) {
            specialBranch.enter();
            if (a < 0) {
                throw new ArithmeticException();
            }
        }
        return result;
    }

    @Specialization
    protected double doDouble(double a, double b) {
        return a % b;
    }

    @Specialization(replaces = {"doInt", "doDouble"})
    protected Object doGeneric(Object a, Object b,
                    @Cached("create()") JSModuloNode nestedModuloNode,
                    @Cached("create()") JSToNumberNode toNumber1Node,
                    @Cached("create()") JSToNumberNode toNumber2Node) {
        return nestedModuloNode.execute(toNumber1Node.execute(a), toNumber2Node.execute(b));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Number.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSModuloNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
