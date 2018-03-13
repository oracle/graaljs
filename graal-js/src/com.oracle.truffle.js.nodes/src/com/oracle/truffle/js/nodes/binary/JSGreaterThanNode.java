/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.JSToStringOrNumberNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.LargeInteger;

@NodeInfo(shortName = ">")
public abstract class JSGreaterThanNode extends JSCompareNode {

    protected JSGreaterThanNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JSGreaterThanNode create(JavaScriptNode left, JavaScriptNode right) {
        return JSGreaterThanNodeGen.create(left, right);
    }

    public static JSGreaterThanNode create() {
        return JSGreaterThanNodeGen.create(null, null);
    }

    public abstract boolean executeBoolean(Object a, Object b);

    @Specialization
    protected boolean doInt(int a, int b) {
        return a > b;
    }

    @Specialization
    protected boolean doLargeInteger(int a, LargeInteger b) {
        return a > b.longValue();
    }

    @Specialization
    protected boolean doLargeInteger(LargeInteger a, int b) {
        return a.longValue() > b;
    }

    @Specialization
    protected boolean doLargeInteger(LargeInteger a, LargeInteger b) {
        return a.longValue() > b.longValue();
    }

    @Specialization
    protected boolean doDouble(double a, double b) {
        return a > b;
    }

    @Specialization
    protected boolean doString(String a, String b) {
        return Boundaries.stringCompareTo(a, b) > 0;
    }

    @Specialization
    protected boolean doStringDouble(String a, double b) {
        return doDouble(stringToDouble(a), b);
    }

    @Specialization
    protected boolean doDoubleString(double a, String b) {
        return doDouble(a, stringToDouble(b));
    }

    @Specialization(guards = {"isJavaNumber(a)", "isJavaNumber(b)"})
    protected boolean doJavaNumber(Object a, Object b) {
        return doDouble(JSRuntime.doubleValue((Number) a), JSRuntime.doubleValue((Number) b));
    }

    @Specialization(replaces = {"doInt", "doDouble", "doString", "doStringDouble", "doDoubleString", "doJavaNumber"})
    protected boolean doGeneric(Object a, Object b,
                    @Cached("create()") JSToStringOrNumberNode toStringOrNumber1,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitive1,
                    @Cached("create()") JSToStringOrNumberNode toStringOrNumber2,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitive2,
                    @Cached("create()") JSGreaterThanNode greaterThanNode) {
        return greaterThanNode.executeBoolean(toStringOrNumber1.execute(toPrimitive1.execute(a)), toStringOrNumber2.execute(toPrimitive2.execute(b)));
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSGreaterThanNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
