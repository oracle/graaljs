/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import static com.oracle.truffle.js.nodes.JSGuards.isString;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantDoubleNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantStringNode;
import com.oracle.truffle.js.nodes.cast.JSDoubleToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.objects.JSLazyString;

@NodeInfo(shortName = "+")
public abstract class JSAddNode extends JSBinaryNode implements Truncatable {

    @CompilationFinal boolean truncate;

    @Child private JSDoubleToStringNode doubleToStringNode;
    @Child private JSConcatStringsNode concatStringsNode;

    protected JSAddNode(boolean truncate, JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
        this.truncate = truncate;
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right, boolean truncate) {
        if (JSTruffleOptions.UseSuperOperations) {
            if (left instanceof JSConstantIntegerNode) {
                if (right instanceof JSConstantIntegerNode) {
                    int leftValue = ((JSConstantIntegerNode) left).executeInt(null);
                    int rightValue = ((JSConstantIntegerNode) right).executeInt(null);
                    long value = (long) leftValue + (long) rightValue;
                    return JSRuntime.longIsRepresentableAsInt(value) ? JSConstantNode.createInt((int) value) : JSConstantNode.createDouble(value);
                }
                // can't swap operands here, could be string concat
            } else if (right instanceof JSConstantIntegerNode || right instanceof JSConstantDoubleNode) {
                Object rightValue = ((JSConstantNode) right).execute(null);
                return JSAddConstantRightNumberNodeGen.create(left, (Number) rightValue, truncate);
            } else if (left instanceof JSConstantStringNode && right instanceof JSConstantStringNode) {
                return JSConstantNode.createString((String) left.execute(null) + (String) right.execute(null));
            } else if (left instanceof JSConstantIntegerNode || left instanceof JSConstantDoubleNode) {
                Object leftValue = ((JSConstantNode) left).execute(null);
                return JSAddConstantLeftNumberNodeGen.create((Number) leftValue, right, truncate);
            }
        }
        return JSAddNodeGen.create(truncate, left, right);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        return create(left, right, false);
    }

    public static JavaScriptNode createUnoptimized(JavaScriptNode left, JavaScriptNode right, boolean truncate) {
        return JSAddNodeGen.create(truncate, left, right);
    }

    public abstract Object execute(Object a, Object b);

    @Specialization(guards = "truncate")
    protected static int doIntTruncate(int a, int b) {
        return a + b;
    }

    @Specialization(guards = "!truncate", rewriteOn = ArithmeticException.class)
    protected static int doInt(int a, int b) {
        return Math.addExact(a, b);
    }

    @Specialization(guards = "!truncate", rewriteOn = ArithmeticException.class)
    protected static Object doIntOverflow(int a, int b) {
        long result = (long) a + (long) b;
        return doIntOverflowStaticLong(result);
    }

    static Object doIntOverflowStaticLong(long result) {
        if (Integer.MAX_VALUE <= result && result <= Integer.MIN_VALUE) {
            return (int) result;
        } else if (JSRuntime.isSafeInteger(result)) {
            return LargeInteger.valueOf(result);
        } else {
            throw new ArithmeticException();
        }
    }

    @Specialization(guards = "truncate")
    protected static int doIntLargeIntegerTruncate(int a, LargeInteger b) {
        return (int) (a + b.longValue());
    }

    @Specialization(guards = "truncate")
    protected static int doLargeIntegerIntTruncate(LargeInteger a, int b) {
        return (int) (a.longValue() + b);
    }

    @Specialization(guards = "truncate")
    protected static int doLargeIntegerTruncate(LargeInteger a, LargeInteger b) {
        return (int) (a.longValue() + b.longValue());
    }

    @Specialization(guards = "!truncate", rewriteOn = ArithmeticException.class)
    protected static LargeInteger doIntLargeInteger(int a, LargeInteger b) {
        return LargeInteger.valueOf(a).addExact(b);
    }

    @Specialization(guards = "!truncate", rewriteOn = ArithmeticException.class)
    protected static LargeInteger doLargeIntegerInt(LargeInteger a, int b) {
        return a.addExact(LargeInteger.valueOf(b));
    }

    @Specialization(guards = "!truncate", rewriteOn = ArithmeticException.class)
    protected static LargeInteger doLargeInteger(LargeInteger a, LargeInteger b) {
        return a.addExact(b);
    }

    @Specialization
    protected static double doDouble(double a, double b) {
        return a + b;
    }

    @Specialization
    protected CharSequence doString(CharSequence a, CharSequence b) {
        return getConcatStringsNode().executeCharSequence(a, b);
    }

    @Specialization
    protected CharSequence doStringInt(CharSequence a, int b) {
        return JSLazyString.createLazyInt(a, b);
    }

    @Specialization
    protected CharSequence doIntString(int a, CharSequence b) {
        return JSLazyString.createLazyInt(a, b);
    }

    @Specialization(guards = "isNumber(b)")
    protected CharSequence doStringNumber(CharSequence a, Object b) {
        return getConcatStringsNode().executeCharSequence(a, getDoubleToStringNode().executeString(b));
    }

    @Specialization(guards = "isNumber(a)")
    protected CharSequence doNumberString(Object a, CharSequence b) {
        return getConcatStringsNode().executeCharSequence(getDoubleToStringNode().executeString(a), b);
    }

    @Specialization(replaces = {"doInt", "doIntOverflow", "doIntTruncate", "doLargeInteger", "doIntLargeInteger", "doLargeIntegerInt", "doLargeIntegerTruncate", "doIntLargeIntegerTruncate",
                    "doLargeIntegerIntTruncate", "doDouble", "doString", "doStringInt", "doIntString", "doStringNumber", "doNumberString"})
    protected Object doPrimitiveConversion(Object a, Object b,
                    @Cached("createHintNone()") JSToPrimitiveNode toPrimitiveA,
                    @Cached("createHintNone()") JSToPrimitiveNode toPrimitiveB,
                    @Cached("create()") JSToNumberNode toNumberA,
                    @Cached("create()") JSToNumberNode toNumberB,
                    @Cached("create()") JSToStringNode toStringA,
                    @Cached("create()") JSToStringNode toStringB,
                    @Cached("createBinaryProfile()") ConditionProfile profileA,
                    @Cached("createBinaryProfile()") ConditionProfile profileB,
                    @Cached("copyRecursive()") JSAddNode add) {

        Object primitiveA = toPrimitiveA.execute(a);
        Object primitiveB = toPrimitiveB.execute(b);

        Object castA;
        Object castB;
        if (profileA.profile(isString(primitiveA))) {
            castA = primitiveA;
            castB = toStringB.executeString(primitiveB);
        } else if (profileB.profile(isString(primitiveB))) {
            castA = toStringA.executeString(primitiveA);
            castB = primitiveB;
        } else {
            castA = toNumberA.execute(primitiveA);
            castB = toNumberB.execute(primitiveB);
        }
        return add.execute(castA, castB);
    }

    public final JSAddNode copyRecursive() {
        return (JSAddNode) create(null, null, truncate);
    }

    @Override
    public void setTruncate() {
        CompilerAsserts.neverPartOfCompilation();
        if (truncate == false) {
            truncate = true;
        }
    }

    protected JSDoubleToStringNode getDoubleToStringNode() {
        if (doubleToStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            doubleToStringNode = insert(JSDoubleToStringNode.create());
        }
        return doubleToStringNode;
    }

    protected JSConcatStringsNode getConcatStringsNode() {
        if (concatStringsNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            concatStringsNode = insert(JSConcatStringsNode.create());
        }
        return concatStringsNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSAddNodeGen.createUnoptimized(cloneUninitialized(getLeft()), cloneUninitialized(getRight()), truncate);
    }
}
