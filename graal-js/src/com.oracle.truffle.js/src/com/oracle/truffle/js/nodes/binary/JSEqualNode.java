/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IsPrimitiveNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantNullNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantUndefinedNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.LongToBigIntNode;
import com.oracle.truffle.js.nodes.unary.JSIsNullOrUndefinedNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * IsLooselyEqual(x, y) aka {@code ==} operator.
 *
 * @see JSIdenticalNode
 */
@NodeInfo(shortName = "==")
@ImportStatic({JSRuntime.class, JSInteropUtil.class, JSConfig.class})
public abstract class JSEqualNode extends JSCompareNode {

    protected JSEqualNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    @NeverDefault
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
    protected static boolean doLong(long a, long b) {
        return a == b;
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
    protected static boolean doDoubleString(double a, TruffleString b,
                    @Shared @Cached JSStringToNumberNode stringToDouble) {
        return doDouble(a, stringToDouble.execute(b));
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
    protected static boolean doBooleanString(boolean a, TruffleString b,
                    @Shared @Cached JSStringToNumberNode stringToDouble) {
        return doBooleanDouble(a, stringToDouble.execute(b));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isReferenceEquals(a, b)")
    protected static boolean doStringIdentity(TruffleString a, TruffleString b) {
        return true;
    }

    @Specialization(replaces = "doStringIdentity")
    protected static boolean doString(TruffleString a, TruffleString b,
                    @Cached TruffleString.EqualNode equalsNode) {
        return Strings.equals(equalsNode, a, b);
    }

    @Specialization
    protected static boolean doStringDouble(TruffleString a, double b,
                    @Shared @Cached JSStringToNumberNode stringToDouble) {
        return doDouble(stringToDouble.execute(a), b);
    }

    @Specialization
    protected static boolean doStringBoolean(TruffleString a, boolean b,
                    @Shared @Cached JSStringToNumberNode stringToDouble) {
        return doDoubleBoolean(stringToDouble.execute(a), b);
    }

    @Specialization
    protected static boolean doStringBigInt(TruffleString a, BigInt b) {
        BigInt aBigInt = JSRuntime.stringToBigInt(a);
        return (aBigInt != null) ? aBigInt.compareTo(b) == 0 : false;
    }

    @Specialization
    protected static boolean doBigIntString(BigInt a, TruffleString b) {
        return doStringBigInt(b, a);
    }

    @Specialization
    protected static boolean doBooleanBigInt(boolean a, BigInt b) {
        return doBigInt(a ? BigInt.ONE : BigInt.ZERO, b);
    }

    @Specialization
    protected static boolean doBigIntBoolean(BigInt a, boolean b) {
        return doBooleanBigInt(b, a);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isNullOrUndefined(a)", "isNullOrUndefined(b)"})
    protected static boolean doBothNullOrUndefined(Object a, Object b) {
        return true;
    }

    @Specialization(guards = {"isNullOrUndefined(a)"})
    protected static boolean doLeftNullOrUndefined(@SuppressWarnings("unused") Object a, Object b,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary bInterop) {
        return isNullish(b, bInterop);
    }

    @Specialization(guards = {"isNullOrUndefined(b)"})
    protected static boolean doRightNullOrUndefined(Object a, @SuppressWarnings("unused") Object b,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary aInterop) {
        return isNullish(a, aInterop);
    }

    @InliningCutoff
    @Specialization(guards = {"hasOverloadedOperators(a) || hasOverloadedOperators(b)"})
    protected static boolean doOverloaded(Object a, Object b,
                    @Bind Node node,
                    @Cached("createHintDefault(getOverloadedOperatorName())") JSOverloadedBinaryNode overloadedOperatorNode,
                    @Cached(inline = true) JSToBooleanNode toBooleanNode) {
        if (a == b) {
            return true;
        } else {
            return toBooleanNode.executeBoolean(node, overloadedOperatorNode.execute(a, b));
        }
    }

    protected static TruffleString getOverloadedOperatorName() {
        return Strings.SYMBOL_EQUALS_EQUALS;
    }

    // null-or-undefined check on one argument suffices
    @Specialization(guards = {"!hasOverloadedOperators(a)", "!hasOverloadedOperators(b)"})
    protected static boolean doJSObject(JSObject a, JSDynamicObject b) {
        return a == b;
    }

    // null-or-undefined check on one argument suffices
    @Specialization(guards = {"!hasOverloadedOperators(a)", "!hasOverloadedOperators(b)"})
    protected static boolean doJSObject(JSDynamicObject a, JSObject b) {
        return a == b;
    }

    @InliningCutoff
    @Specialization(guards = {"!hasOverloadedOperators(a)", "isPrimitiveNode.executeBoolean(b)"})
    protected static boolean doJSObjectVsPrimitive(JSObject a, Object b,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary bInterop,
                    @Shared @Cached("createHintDefault()") JSToPrimitiveNode toPrimitiveNode,
                    @Shared @Cached @SuppressWarnings("unused") IsPrimitiveNode isPrimitiveNode,
                    @Shared @Cached JSEqualNode nestedEqualNode) {
        if (isNullish(b, bInterop)) {
            return false;
        }
        return nestedEqualNode.executeBoolean(toPrimitiveNode.execute(a), b);
    }

    @InliningCutoff
    @Specialization(guards = {"!hasOverloadedOperators(b)", "isPrimitiveNode.executeBoolean(a)"})
    protected static boolean doJSObjectVsPrimitive(Object a, JSObject b,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary aInterop,
                    @Shared @Cached("createHintDefault()") JSToPrimitiveNode toPrimitiveNode,
                    @Shared @Cached @SuppressWarnings("unused") IsPrimitiveNode isPrimitiveNode,
                    @Shared @Cached JSEqualNode nestedEqualNode) {
        if (isNullish(a, aInterop)) {
            return false;
        }
        return nestedEqualNode.executeBoolean(a, toPrimitiveNode.execute(b));
    }

    @Specialization
    protected static boolean doBigIntAndInt(BigInt a, int b) {
        return a.compareValueTo(b) == 0;
    }

    @Specialization
    protected static boolean doBigIntAndNumber(BigInt a, double b) {
        if (Double.isNaN(b)) {
            return false;
        }
        return a.compareValueTo(b) == 0;
    }

    @Specialization
    protected static boolean doIntAndBigInt(int a, BigInt b) {
        return b.compareValueTo(a) == 0;
    }

    @Specialization
    protected static boolean doNumberAndBigInt(double a, BigInt b) {
        return doBigIntAndNumber(b, a);
    }

    @Specialization
    protected static boolean doSymbol(Symbol a, Symbol b) {
        return a == b;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSymbol(b)", "!isObjectNode.executeBoolean(b)"})
    protected static boolean doSymbolNotSymbol(Symbol a, Object b,
                    @Shared @Cached IsObjectNode isObjectNode) {
        return false;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSymbol(a)", "!isObjectNode.executeBoolean(a)"})
    protected static boolean doSymbolNotSymbol(Object a, Symbol b,
                    @Shared @Cached IsObjectNode isObjectNode) {
        return false;
    }

    @InliningCutoff
    @Specialization(guards = "isAForeign || isBForeign")
    protected final boolean doForeign(Object a, Object b,
                    @Bind("isForeignObjectOrNumber(a)") boolean isAForeign,
                    @Bind("isForeignObjectOrNumber(b)") boolean isBForeign,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary aInterop,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary bInterop,
                    @Shared @Cached("createHintDefault()") JSToPrimitiveNode toPrimitiveNode,
                    @Shared @Cached IsPrimitiveNode isPrimitiveNode,
                    @Shared @Cached JSEqualNode nestedEqualNode,
                    @Cached LongToBigIntNode longToBigIntA,
                    @Cached LongToBigIntNode longToBigIntB) {
        assert a != null && b != null;
        // If at least one is nullish => both need to be nullish to be equal
        if (isNullish(a, aInterop)) {
            return isNullish(b, bInterop);
        } else if (isNullish(b, bInterop)) {
            assert !isNullish(a, bInterop);
            return false;
        }
        boolean isAPrimitive = isPrimitiveNode.executeBoolean(a);
        boolean isBPrimitive = isPrimitiveNode.executeBoolean(b);
        if (!isAPrimitive && !isBPrimitive) {
            // If both are of type Object, don't attempt ToPrimitive conversion.
            return aInterop.isIdentical(a, b, bInterop);
        }
        // If one of them is primitive, we attempt to convert the other one ToPrimitive.
        // Foreign primitive values always have to be converted to JS primitive values.
        Object primA = !isAPrimitive || isAForeign ? toPrimitiveNode.execute(a) : a;
        Object primB = !isBPrimitive || isBForeign ? toPrimitiveNode.execute(b) : b;
        // Now that both are primitive values, we can compare them using normal JS semantics.
        assert !JSGuards.isForeignObject(primA) && !JSGuards.isForeignObject(primB);
        primA = longToBigIntA.execute(this, primA);
        primB = longToBigIntB.execute(this, primB);
        return nestedEqualNode.executeBoolean(primA, primB);
    }

    @Fallback
    protected static boolean doFallback(Object a, Object b) {
        assert !JSRuntime.equal(a, b) : a + " (" + (a == null ? "null" : a.getClass()) + ")" + ", " + b + " (" + (b == null ? "null" : b.getClass()) + ")";
        return false;
    }

    protected static boolean isNullish(Object value, InteropLibrary interop) {
        return JSRuntime.isNullOrUndefined(value) || interop.isNull(value);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSEqualNodeGen.create(cloneUninitialized(getLeft(), materializedTags), cloneUninitialized(getRight(), materializedTags));
    }
}
