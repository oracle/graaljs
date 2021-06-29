/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantNullNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantUndefinedNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.unary.JSIsNullOrUndefinedNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Record;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.Null;

@NodeInfo(shortName = "==")
@ImportStatic({JSRuntime.class, JSInteropUtil.class, JSConfig.class})
public abstract class JSEqualNode extends JSCompareNode {
    protected static final int MAX_CLASSES = 3;

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
                return JSIsNullOrUndefinedNode.createFromEquals(left, right);
            }
        } else if (rightIs) {
            return JSIsNullOrUndefinedNode.createFromEquals(left, right);
        }
        return JSEqualNodeGen.create(left, right);
    }

    public static JavaScriptNode createUnoptimized(JavaScriptNode left, JavaScriptNode right) {
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

    @Specialization(guards = {"isNullOrUndefined(a)"})
    protected static boolean doLeftNullOrUndefined(@SuppressWarnings("unused") Object a, Object b,
                    @Shared("bInterop") @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary bInterop) {
        return isNullish(b, bInterop);
    }

    @Specialization(guards = {"isNullOrUndefined(b)"})
    protected static boolean doRightNullOrUndefined(Object a, @SuppressWarnings("unused") Object b,
                    @Shared("aInterop") @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary aInterop) {
        return isNullish(a, aInterop);
    }

    @Specialization(guards = {"hasOverloadedOperators(a) || hasOverloadedOperators(b)"})
    protected boolean doOverloaded(Object a, Object b,
                    @Cached("createHintNone(getOverloadedOperatorName())") JSOverloadedBinaryNode overloadedOperatorNode,
                    @Cached("create()") JSToBooleanNode toBooleanNode) {
        if (a == b) {
            return true;
        } else {
            return toBooleanNode.executeBoolean(overloadedOperatorNode.execute(a, b));
        }
    }

    protected String getOverloadedOperatorName() {
        return "==";
    }

    @Specialization(guards = {"isObject(a)", "!isObject(b)", "!hasOverloadedOperators(a)"})
    protected boolean doJSObject(DynamicObject a, Object b,
                    @Shared("bInterop") @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary bInterop,
                    @Shared("toPrimitive") @Cached("createHintNone()") JSToPrimitiveNode toPrimitiveNode,
                    @Shared("equal") @Cached JSEqualNode nestedEqualNode) {
        if (isNullish(b, bInterop)) {
            return false;
        }
        return nestedEqualNode.executeBoolean(toPrimitiveNode.execute(a), b);
    }

    @Specialization(guards = {"!isObject(a)", "isObject(b)", "!hasOverloadedOperators(b)"})
    protected boolean doJSObject(Object a, DynamicObject b,
                    @Shared("aInterop") @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary aInterop,
                    @Shared("toPrimitive") @Cached("createHintNone()") JSToPrimitiveNode toPrimitiveNode,
                    @Shared("equal") @Cached JSEqualNode nestedEqualNode) {
        if (isNullish(a, aInterop)) {
            return false;
        }
        return nestedEqualNode.executeBoolean(a, toPrimitiveNode.execute(b));
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
    @Specialization(guards = {"!isNullOrUndefined(a)", "isJSDynamicObject(a)", "isJSDynamicObject(b)"})
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

    @Specialization(guards = "isForeignObject(a) || isForeignObject(b)")
    protected boolean doForeign(Object a, Object b,
                    @Shared("aInterop") @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary aInterop,
                    @Shared("bInterop") @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary bInterop,
                    @Shared("equal") @Cached JSEqualNode nestedEqualNode) {
        assert (a != null) && (b != null);
        final Object defaultValue = null;
        Object primLeft;
        if (JSGuards.isForeignObject(a)) {
            primLeft = JSInteropUtil.toPrimitiveOrDefault(a, defaultValue, aInterop, this);
        } else {
            primLeft = JSGuards.isNullOrUndefined(a) ? Null.instance : a;
        }
        Object primRight;
        if (JSGuards.isForeignObject(b)) {
            primRight = JSInteropUtil.toPrimitiveOrDefault(b, defaultValue, bInterop, this);
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
            return nestedEqualNode.executeBoolean(primLeft, primRight);
        }
    }

    @Specialization(guards = {"a != null", "b != null", "cachedClassA != null", "cachedClassB != null", "a.getClass() == cachedClassA", "b.getClass() == cachedClassB"}, limit = "MAX_CLASSES")
    protected static boolean doNumberCached(Object a, Object b,
                    @Cached("getJavaNumberClass(a)") Class<?> cachedClassA,
                    @Cached("getJavaNumberClass(b)") Class<?> cachedClassB) {
        return doNumber((Number) cachedClassA.cast(a), (Number) cachedClassB.cast(b));
    }

    @Specialization(guards = {"isJavaNumber(a)", "isJavaNumber(b)"}, replaces = "doNumberCached")
    protected static boolean doNumber(Number a, Number b) {
        return doDouble(JSRuntime.doubleValue(a), JSRuntime.doubleValue(b));
    }

    @Specialization(guards = {"isJavaNumber(a)"})
    protected boolean doStringNumber(Object a, String b) {
        return doDoubleString(JSRuntime.doubleValue((Number) a), b);
    }

    @Specialization(guards = {"isJavaNumber(b)"})
    protected boolean doStringNumber(String a, Object b) {
        return doStringDouble(a, JSRuntime.doubleValue((Number) b));
    }

    @Specialization
    protected static boolean doRecord(Record a, Record b,
                                      @Cached("createStrictEqualityComparison()") @Shared("strictEquality") JSIdenticalNode strictEqualityNode) {
        return strictEqualityNode.executeBoolean(a, b);
    }

    @Specialization
    protected static boolean doTuple(Tuple a, Tuple b,
                                     @Cached("createStrictEqualityComparison()") @Shared("strictEquality") JSIdenticalNode strictEqualityNode) {
        return strictEqualityNode.executeBoolean(a, b);
    }

    @Fallback
    protected static boolean doFallback(Object a, Object b) {
        return JSRuntime.equal(a, b);
    }

    protected static boolean isNullish(Object value, InteropLibrary interop) {
        return JSRuntime.isNullOrUndefined(value) || interop.isNull(value);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSEqualNodeGen.create(cloneUninitialized(getLeft(), materializedTags), cloneUninitialized(getRight(), materializedTags));
    }
}
