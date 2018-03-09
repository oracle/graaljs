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

@NodeInfo(shortName = "**")
public abstract class JSExponentiateNode extends JSBinaryNode {

    protected JSExponentiateNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        return JSExponentiateNodeGen.create(left, right);
    }

    public static JSExponentiateNode create() {
        return (JSExponentiateNode) create(null, null);
    }

    public abstract Object execute(Object a, Object b);

    @Specialization
    protected double doDouble(double a, double b) {
        return Math.pow(a, b);
    }

    @Specialization(replaces = "doDouble")
    protected Object doGeneric(Object a, Object b,
                    @Cached("create()") JSExponentiateNode nestedExponentiateNode,
                    @Cached("create()") JSToNumberNode toNumber1Node,
                    @Cached("create()") JSToNumberNode toNumber2Node) {
        return nestedExponentiateNode.execute(toNumber1Node.execute(a), toNumber2Node.execute(b));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Number.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSExponentiateNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
