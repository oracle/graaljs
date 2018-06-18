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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantNullNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantUndefinedNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.unary.JSIsNullOrUndefinedNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

@NodeInfo(shortName = "==")
@ImportStatic({JSRuntime.class, JSInteropUtil.class})
public abstract class JSEqualNode extends JSCompareNode {
    protected static final int MAX_CLASSES = 3;

    @Child protected JSEqualNode equalNode;
    @Child protected JSToPrimitiveNode toPrimitiveNode;

    protected JSEqualNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JSEqualNode create() {
        return JSEqualNodeGen.create(null, null);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        boolean leftIs = left instanceof JSConstantUndefinedNode || left instanceof JSConstantNullNode;
        boolean rightIs = right instanceof JSConstantUndefinedNode || right instanceof JSConstantNullNode;
        if (leftIs) {
            if (rightIs) {
                return JSConstantNode.createBoolean(true);
            } else {
                return JSIsNullOrUndefinedNode.create(right);
            }
        } else if (rightIs) {
            return JSIsNullOrUndefinedNode.create(left);
        }
        return JSEqualNodeGen.create(left, right);
    }

    public abstract boolean executeBoolean(Object left, Object right);

    @Specialization
    protected static boolean doInt(int a, int b) {
        return a == b;
    }

    @Specialization
    protected static boolean doIntBoolean(int a, boolean b) {
        return a == (b ? 1 : 0);
    }

    @Specialization
    protected static boolean doDouble(double a, double b) {
        return a == b;
    }

    @Specialization
    protected static boolean doBigInt(BigInt a, BigInt b) {
        return a.compareTo(b) == 0;
    }

    @Specialization
    protected boolean doDoubleString(double a, String b) {
        return doDouble(a, stringToDouble(b));
    }

    @Specialization
    protected static boolean doDoubleBoolean(double a, boolean b) {
        return doDouble(a, b ? 1.0 : 0.0);
    }

    @Specialization
    protected static boolean doBoolean(boolean a, boolean b) {
        return a == b;
    }

    @Specialization
    protected static boolean doBooleanInt(boolean a, int b) {
        return (a ? 1 : 0) == b;
    }

    @Specialization
    protected static boolean doBooleanDouble(boolean a, double b) {
        return doDouble((a ? 1.0 : 0.0), b);
    }

    @Specialization
    protected boolean doBooleanString(boolean a, String b) {
        return doBooleanDouble(a, stringToDouble(b));
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
    protected boolean doStringDouble(String a, double b) {
        return doDouble(stringToDouble(a), b);
    }

    @Specialization
    protected boolean doStringBoolean(String a, boolean b) {
        return doDoubleBoolean(stringToDouble(a), b);
    }

    @Specialization
    protected boolean doStringBigInt(String a, BigInt b) {
        BigInt aBigInt = JSRuntime.stringToBigInt(a);
        return (aBigInt != null) ? aBigInt.compareTo(b) == 0 : false;
    }

    @Specialization
    protected boolean doBigIntString(BigInt a, String b) {
        return doStringBigInt(b, a);
    }

    @Specialization
    protected boolean doBooleanBigInt(boolean a, BigInt b) {
        return doBigInt(a ? BigInt.ONE : BigInt.ZERO, b);
    }

    @Specialization
    protected boolean doBigIntBoolean(BigInt a, boolean b) {
        return doBooleanBigInt(b, a);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isNullOrUndefined(a)", "isNullOrUndefined(b)"})
    protected static boolean doBothNullOrUndefined(Object a, Object b) {
        return true;
    }

    @Specialization(guards = {"isNullOrUndefined(a)"}, replaces = {"doBothNullOrUndefined"})
    protected static boolean doLeftNullOrUndefined(@SuppressWarnings("unused") Object a, Object b) {
        return JSGuards.isNullOrUndefined(b);
    }

    @Specialization(guards = {"isNullOrUndefined(b)"}, replaces = {"doBothNullOrUndefined"})
    protected static boolean doRightNullOrUndefined(Object a, @SuppressWarnings("unused") Object b) {
        return JSGuards.isNullOrUndefined(a);
    }

    @Specialization(guards = {"isObject(a)", "!isObject(b)"})
    protected boolean doJSObject(DynamicObject a, Object b) {
        if (JSGuards.isNullOrUndefined(b)) {
            return false;
        }
        return getEqualNode().executeBoolean(getToPrimitiveNode().execute(a), b);
    }

    @Specialization(guards = {"!isObject(a)", "isObject(b)"})
    protected boolean doJSObject(Object a, DynamicObject b) {
        if (JSGuards.isNullOrUndefined(a)) {
            return false;
        }
        return getEqualNode().executeBoolean(a, getToPrimitiveNode().execute(b));
    }

    @Specialization
    protected boolean doBigIntAndInt(BigInt a, int b) {
        return a.compareTo(BigInt.valueOf(b)) == 0;
    }

    @Specialization
    protected boolean doBigIntAndNumber(BigInt a, double b) {
        if (Double.isNaN(b)) {
            return false;
        }
        return a.compareValueTo(b) == 0;
    }

    @Specialization
    protected boolean doIntAndBigInt(int a, BigInt b) {
        return b.compareTo(BigInt.valueOf(a)) == 0;
    }

    @Specialization
    protected boolean doNumberAndBigInt(double a, BigInt b) {
        return doBigIntAndNumber(b, a);
    }

    // null-or-undefined check on one element suffices
    @Specialization(guards = {"!isNullOrUndefined(a)", "isJSType(a)", "isJSType(b)"})
    protected static boolean doJSObject(DynamicObject a, DynamicObject b) {
        return a == b;
    }

    @Specialization
    protected static boolean doSymbol(Symbol a, Symbol b) {
        return a == b;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSymbol(b)", "!isObject(b)"})
    protected static boolean doSymbolNotSymbol(Symbol a, Object b) {
        return false;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSymbol(a)", "!isObject(a)"})
    protected static boolean doSymbolNotSymbol(Object a, Symbol b) {
        return false;
    }

    @Specialization(guards = "oneIsForeign(a,b)")
    protected boolean doForeign(Object a, Object b,
                    @Cached("createIsNull()") Node isNull,
                    @Cached("createIsBoxed()") Node isBoxed,
                    @Cached("createUnbox()") Node unbox) {
        assert a != null & b != null;
        final Object defaultValue = null;
        Object primLeft;
        if (JSGuards.isForeignObject(a)) {
            primLeft = JSInteropNodeUtil.toPrimitiveOrDefault((TruffleObject) a, defaultValue, isNull, isBoxed, unbox);
        } else {
            primLeft = JSGuards.isNullOrUndefined(a) ? Null.instance : a;
        }
        Object primRight;
        if (JSGuards.isForeignObject(b)) {
            primRight = JSInteropNodeUtil.toPrimitiveOrDefault((TruffleObject) b, defaultValue, isNull, isBoxed, unbox);
        } else {
            primRight = JSGuards.isNullOrUndefined(b) ? Null.instance : b;
        }

        if (primLeft == Null.instance || primRight == Null.instance) {
            // at least one is nullish => both need to be for equality
            return primLeft == primRight;
        } else if (primLeft == defaultValue || primRight == defaultValue) {
            // if both are foreign objects and not null and not boxed, use Java equals
            if (primLeft == defaultValue && primRight == defaultValue) {
                return Boundaries.equals(a, b);
            } else {
                return false; // cannot be equal
            }
        } else {
            assert !JSGuards.isForeignObject(primLeft) && !JSGuards.isForeignObject(primRight);
            return getEqualNode().executeBoolean(primLeft, primRight);
        }
    }

    @Specialization(guards = {"a != null", "b != null", "cachedClassA != null", "cachedClassB != null", "a.getClass() == cachedClassA", "b.getClass() == cachedClassB"}, limit = "MAX_CLASSES")
    protected static boolean doNumberCached(Object a, Object b,
                    @Cached("getJavaNumberClass(a)") Class<?> cachedClassA,
                    @Cached("getJavaNumberClass(b)") Class<?> cachedClassB) {
        return doDouble(JSRuntime.doubleValue((Number) cachedClassA.cast(a)), JSRuntime.doubleValue((Number) cachedClassB.cast(b)));
    }

    @Specialization(guards = {"isJavaNumber(a)", "isJavaNumber(b)"}, replaces = "doNumberCached")
    protected static boolean doNumber(Object a, Object b) {
        return doDouble(JSRuntime.doubleValue((Number) a), JSRuntime.doubleValue((Number) b));
    }

    @Specialization(guards = {"isJavaNumber(a)"})
    protected boolean doStringNumber(Object a, String b) {
        return doDoubleString(JSRuntime.doubleValue((Number) a), b);
    }

    @Specialization(guards = {"isJavaNumber(b)"})
    protected boolean doStringNumber(String a, Object b) {
        return doStringDouble(a, JSRuntime.doubleValue((Number) b));
    }

    @Specialization(guards = {"a != null", "cachedClassA != null", "a.getClass() == cachedClassA"}, limit = "MAX_CLASSES")
    protected static boolean doJavaObjectA(Object a, Object b, //
                    @Cached("getJavaObjectClass(a)") @SuppressWarnings("unused") Class<?> cachedClassA) {
        return doJavaGeneric(a, b);
    }

    @Specialization(guards = {"b != null", "cachedClassB != null", "b.getClass() == cachedClassB"}, limit = "MAX_CLASSES")
    protected static boolean doJavaObjectB(Object a, Object b, //
                    @Cached("getJavaObjectClass(b)") @SuppressWarnings("unused") Class<?> cachedClassB) {
        return doJavaGeneric(a, b);
    }

    @Specialization(guards = {"isJavaObject(a) || isJavaObject(b)"}, replaces = {"doJavaObjectA", "doJavaObjectB"})
    protected static boolean doJavaGeneric(Object a, Object b) {
        assert JSRuntime.isJavaObject(a) || JSRuntime.isJavaObject(b);
        return a == b;
    }

    @Fallback
    protected static boolean doFallback(Object a, Object b) {
        return JSRuntime.equal(a, b);
    }

    protected static boolean oneIsForeign(Object a, Object b) {
        return JSGuards.isForeignObject(a) || JSGuards.isForeignObject(b);
    }

    private JSEqualNode getEqualNode() {
        if (equalNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            equalNode = insert(JSEqualNode.create());
        }
        return equalNode;
    }

    private JSToPrimitiveNode getToPrimitiveNode() {
        if (toPrimitiveNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toPrimitiveNode = insert(JSToPrimitiveNode.createHintNone());
        }
        return toPrimitiveNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSEqualNodeGen.create(cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
