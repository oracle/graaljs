/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;

@NodeInfo(shortName = "-")
public abstract class JSSubtractNode extends JSBinaryNode implements Truncatable {

    @CompilationFinal boolean truncate;

    public JSSubtractNode(boolean truncate) {
        this.truncate = truncate;
    }

    public static JSSubtractNode create(JavaScriptNode left, JavaScriptNode right, boolean truncate) {
        return JSSubtractNodeGen.create(truncate, left, right);
    }

    public static JSSubtractNode create(JavaScriptNode left, JavaScriptNode right) {
        return create(left, right, false);
    }

    public abstract Object execute(Object a, Object b);

    @Specialization(rewriteOn = ArithmeticException.class)
    protected int doInt(int a, int b) {
        if (truncate) {
            return a - b;
        } else {
            return Math.subtractExact(a, b);
        }
    }

    @Specialization(replaces = "doInt")
    protected double doDouble(double a, double b) {
        return a - b;
    }

    @Specialization(replaces = {"doDouble"})
    protected Object doGeneric(Object a, Object b,
                    @Cached("create()") JSToNumberNode toNumberA,
                    @Cached("create()") JSToNumberNode toNumberB,
                    @Cached("copyRecursive()") JSSubtractNode subtract) {
        return subtract.execute(toNumberA.execute(a), toNumberB.execute(b));
    }

    public final JSSubtractNode copyRecursive() {
        return create(null, null, truncate);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Number.class;
    }

    @Override
    public void setTruncate() {
        CompilerAsserts.neverPartOfCompilation();
        if (truncate == false) {
            truncate = true;
            Truncatable.truncate(getLeft());
            Truncatable.truncate(getRight());
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSSubtractNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()), truncate);
    }
}
