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
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
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
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;

@NodeInfo(shortName = "===")
@ImportStatic(JSRuntime.class)
public abstract class JSIdenticalNode extends JSCompareNode {
    protected static final int MAX_CLASSES = 3;

    protected static final int STRICT_EQUALITY_COMPARISON = 0;
    protected static final int SAME_VALUE = 1;
    protected static final int SAME_VALUE_ZERO = 2;

    protected final int type;

    protected JSIdenticalNode(JavaScriptNode left, JavaScriptNode right, int type) {
        super(left, right);
        this.type = type;
    }

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

    @Specialization
    protected static boolean doInt(int a, int b) {
        return a == b;
    }

    @Specialization
    protected boolean doDouble(double a, double b) {
        if (type == STRICT_EQUALITY_COMPARISON) {
            return a == b;
        } else if (type == SAME_VALUE) {
            if (Double.isNaN(a)) {
                return Double.isNaN(b);
            }
            if (a == 0 && b == 0) {
                return JSRuntime.isNegativeZero(a) == JSRuntime.isNegativeZero(b);
            }
            return a == b;
        } else {
            assert type == SAME_VALUE_ZERO;
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
    protected static boolean doBigInt(BigInt a, BigInt b) {
        return a.compareTo(b) == 0;
    }

    @Specialization
    protected static boolean doBigIntDouble(@SuppressWarnings("unused") BigInt a, @SuppressWarnings("unused") double b) {
        return false;
    }

    @Specialization
    protected static boolean doDoubleBigInt(double a, BigInt b) {
        return doBigIntDouble(b, a);
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

    @Specialization(guards = {"isTruffleJavaObject(a)", "isTruffleJavaObject(b)"})
    protected static boolean doTruffleJavaObjects(TruffleObject a, TruffleObject b) {
        TruffleLanguage.Env env = AbstractJavaScriptLanguage.getCurrentEnv();
        return env.asHostObject(a) == env.asHostObject(b);
    }

    @Fallback
    protected static boolean doFallback(Object a, Object b) {
        return JSRuntime.identical(a, b);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSIdenticalNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()), type);
    }
}
