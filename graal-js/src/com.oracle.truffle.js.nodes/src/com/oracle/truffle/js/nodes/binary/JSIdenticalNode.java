/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantBooleanNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantNullNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantStringNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantUndefinedNode;
import com.oracle.truffle.js.nodes.unary.IsIdenticalBooleanNode;
import com.oracle.truffle.js.nodes.unary.IsIdenticalIntegerNode;
import com.oracle.truffle.js.nodes.unary.IsIdenticalStringNode;
import com.oracle.truffle.js.nodes.unary.IsIdenticalUndefinedNode;
import com.oracle.truffle.js.nodes.unary.IsNullNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;

@NodeInfo(shortName = "===")
@ImportStatic(JSRuntime.class)
@NodeField(name = "type", type = int.class)
public abstract class JSIdenticalNode extends JSCompareNode {
    protected static final int MAX_CLASSES = 3;

    protected static final int STRICT_EQUALITY_COMPARISON = 0;
    protected static final int SAME_VALUE = 1;
    protected static final int SAME_VALUE_ZERO = 2;

    public static JSIdenticalNode createStrictEqualityComparison() {
        return JSIdenticalNodeGen.create(null, null, STRICT_EQUALITY_COMPARISON);
    }

    public static JSIdenticalNode createSameValue() {
        return JSIdenticalNodeGen.create(null, null, SAME_VALUE);
    }

    public static JSIdenticalNode createSameValue(JavaScriptNode left, JavaScriptNode right) {
        return JSIdenticalNodeGen.create(left, right, SAME_VALUE);
    }

    public static JSIdenticalNode createSameValueZero() {
        return JSIdenticalNodeGen.create(null, null, SAME_VALUE_ZERO);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        if (left instanceof JSConstantNullNode) {
            return IsNullNode.create(right);
        } else if (right instanceof JSConstantNullNode) {
            return IsNullNode.create(left);
        } else if (left instanceof JSConstantStringNode) {
            return IsIdenticalStringNode.create((String) left.execute(null), right);
        } else if (right instanceof JSConstantStringNode) {
            return IsIdenticalStringNode.create((String) right.execute(null), left);
        } else if (left instanceof JSConstantIntegerNode) {
            return IsIdenticalIntegerNode.create((int) left.execute(null), right);
        } else if (right instanceof JSConstantIntegerNode) {
            return IsIdenticalIntegerNode.create((int) right.execute(null), left);
        } else if (left instanceof JSConstantBooleanNode) {
            return IsIdenticalBooleanNode.create((boolean) left.execute(null), right);
        } else if (right instanceof JSConstantBooleanNode) {
            return IsIdenticalBooleanNode.create((boolean) right.execute(null), left);
        } else if (left instanceof JSConstantUndefinedNode) {
            return IsIdenticalUndefinedNode.create(right);
        } else if (right instanceof JSConstantUndefinedNode) {
            return IsIdenticalUndefinedNode.create(left);
        }
        return JSIdenticalNodeGen.create(left, right, STRICT_EQUALITY_COMPARISON);
    }

    public abstract boolean executeBoolean(Object left, Object right);

    protected abstract int getType();

    @Specialization
    protected static boolean doInt(int a, int b) {
        return a == b;
    }

    @Specialization
    protected boolean doDouble(double a, double b) {
        if (getType() == STRICT_EQUALITY_COMPARISON) {
            return a == b;
        } else if (getType() == SAME_VALUE) {
            if (Double.isNaN(a)) {
                return Double.isNaN(b);
            }
            if (a == 0 && b == 0) {
                return JSRuntime.isNegativeZero(a) == JSRuntime.isNegativeZero(b);
            }
            return a == b;
        } else {
            assert getType() == SAME_VALUE_ZERO;
            if (Double.isNaN(a)) {
                return Double.isNaN(b);
            }
            return a == b;
        }
    }

    @Specialization
    protected static boolean doBoolean(boolean a, boolean b) {
        return a == b;
    }

    @Specialization
    protected static boolean doObject(DynamicObject a, DynamicObject b) {
        assert a != null; // should have been transformed to Null.instance
        assert b != null; // should have been transformed to Null.instance
        return a == b;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isReferenceEquals(a, b)")
    protected static boolean doStringIdentity(String a, String b) {
        return true;
    }

    @Specialization(replaces = "doStringIdentity")
    protected static boolean doString(String a, String b) {
        return a.equals(b);
    }

    @Specialization
    protected static boolean doSymbol(Symbol a, Symbol b) {
        return a == b;
    }

    @Specialization(guards = {"isBoolean(a) != isBoolean(b)"})
    protected static boolean doBooleanNotBoolean(Object a, Object b) {
        assert (a != null) && (b != null);
        return false;
    }

    @Specialization(guards = {"isSymbol(a) != isSymbol(b)"})
    protected static boolean doSymbolNotSymbol(Object a, Object b) {
        assert (a != null) && (b != null);
        return false;
    }

    @Specialization(guards = {"isJSNull(a) != isJSNull(b)"})
    protected static boolean doNullNotNull(Object a, Object b) {
        assert (a != null) && (b != null);
        return false;
    }

    @Specialization(guards = {"isUndefined(a) != isUndefined(b)"})
    protected static boolean doUndefinedNotUndefined(Object a, Object b) {
        assert (a != null) && (b != null);
        return false;
    }

    protected static boolean isNonObjectType(Class<?> clazz) {
        return Number.class.isAssignableFrom(clazz) || JSRuntime.isStringClass(clazz);
    }

    protected static boolean differentNonObjectTypes(Class<?> classA, Class<?> classB) {
        return Number.class.isAssignableFrom(classA) != Number.class.isAssignableFrom(classB) || JSRuntime.isStringClass(classA) != JSRuntime.isStringClass(classB);
    }

    /**
     * lhs and rhs are of different types. This specialization is used only for type classes that
     * are wider than a single type (to be more specific, numbers and strings, currently).
     */
    @SuppressWarnings("unused")
    @Specialization(guards = {"a.getClass() == cachedClassA", "b.getClass() == cachedClassB", "isNonObjectType(cachedClassA) || isNonObjectType(cachedClassB)",
                    "differentNonObjectTypes(cachedClassA, cachedClassB)"}, limit = "MAX_CLASSES")
    protected static boolean doDifferentTypesCached(Object a, Object b, //
                    @Cached("a.getClass()") Class<?> cachedClassA, //
                    @Cached("b.getClass()") Class<?> cachedClassB) {
        return false;
    }

    @Specialization(guards = {"isJavaNumber(a) != isJavaNumber(b)"}, replaces = "doDifferentTypesCached")
    protected static boolean doNumberNotNumber(Object a, Object b) {
        assert (a != null) && (b != null);
        return false;
    }

    @Specialization(guards = {"isString(a) != isString(b)"}, replaces = "doDifferentTypesCached")
    protected static boolean doStringNotString(Object a, Object b) {
        assert (a != null) && (b != null);
        return false;
    }

    @Specialization(guards = {"cachedClassA != null", "cachedClassB != null", "a.getClass() == cachedClassA",
                    "b.getClass() == cachedClassB"}, limit = "MAX_CLASSES")
    protected boolean doNumberCached(Object a, Object b, //
                    @Cached("getJavaNumberClass(a)") Class<?> cachedClassA, //
                    @Cached("getJavaNumberClass(b)") Class<?> cachedClassB) {
        return doDouble(JSRuntime.doubleValue((Number) cachedClassA.cast(a)), JSRuntime.doubleValue((Number) cachedClassB.cast(b)));
    }

    @Specialization(guards = {"isJavaNumber(a)", "isJavaNumber(b)"}, replaces = "doNumberCached")
    protected boolean doNumber(Object a, Object b) {
        return doDouble(JSRuntime.doubleValue((Number) a), JSRuntime.doubleValue((Number) b));
    }

    @Specialization(guards = {"cachedClassA != null", "a.getClass() == cachedClassA"}, limit = "MAX_CLASSES")
    protected static boolean doJavaObjectA(Object a, Object b, //
                    @Cached("getJavaObjectClass(a)") @SuppressWarnings("unused") Class<?> cachedClassA) {
        return doJavaGeneric(a, b);
    }

    @Specialization(guards = {"cachedClassB != null", "b.getClass() == cachedClassB"}, limit = "MAX_CLASSES")
    protected static boolean doJavaObjectB(Object a, Object b, //
                    @Cached("getJavaObjectClass(b)") @SuppressWarnings("unused") Class<?> cachedClassB) {
        return doJavaGeneric(a, b);
    }

    @Specialization(guards = {"isJavaObject(a) || isJavaObject(b)"}, replaces = {"doJavaObjectA", "doJavaObjectB"})
    protected static boolean doJavaGeneric(Object a, Object b) {
        assert (a != null) && (b != null);
        assert JSRuntime.isJavaObject(a) || JSRuntime.isJavaObject(b);
        return a == b;
    }

    @Specialization(guards = {"isForeignObject(a)", "isForeignObject(b)"})
    protected static boolean doTruffleObject(TruffleObject a, TruffleObject b) {
        // case is covered by doFallback, this is to avoid GR-4768
        return JSRuntime.identical(a, b);
    }

    @Fallback
    protected static boolean doFallback(Object a, Object b) {
        return JSRuntime.identical(a, b);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSIdenticalNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()), getType());
    }
}
