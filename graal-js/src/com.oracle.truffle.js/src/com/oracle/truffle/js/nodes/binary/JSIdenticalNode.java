/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
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
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

@ImportStatic({JSRuntime.class, JSConfig.class})
@GenerateCached(false)
public abstract class JSIdenticalNode extends JSCompareNode {

    protected static final int STRICT_EQUALITY_COMPARISON = 0;
    protected static final int SAME_VALUE = 1;
    protected static final int SAME_VALUE_ZERO = 2;

    protected JSIdenticalNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    @NeverDefault
    public static JSIdenticalNode createStrictEqualityComparison() {
        return JSStrictEqualNodeGen.create(null, null);
    }

    @NeverDefault
    public static JSIdenticalNode createSameValue() {
        return JSSameValueNodeGen.create(null, null, SAME_VALUE);
    }

    @NeverDefault
    public static JSIdenticalNode createSameValueZero() {
        return JSSameValueNodeGen.create(null, null, SAME_VALUE_ZERO);
    }

    public static JavaScriptNode createUnoptimized(JavaScriptNode left, JavaScriptNode right) {
        return JSStrictEqualNodeGen.create(left, right);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        if (left instanceof JSConstantNullNode) {
            return IsNullNode.create(right, true);
        } else if (right instanceof JSConstantNullNode) {
            return IsNullNode.create(left, false);
        } else if (left instanceof JSConstantStringNode) {
            return IsIdenticalStringNode.create((TruffleString) left.execute(null), right, true);
        } else if (right instanceof JSConstantStringNode) {
            return IsIdenticalStringNode.create((TruffleString) right.execute(null), left, false);
        } else if (left instanceof JSConstantIntegerNode) {
            return IsIdenticalIntegerNode.create((int) left.execute(null), right, true);
        } else if (right instanceof JSConstantIntegerNode) {
            return IsIdenticalIntegerNode.create((int) right.execute(null), left, false);
        } else if (left instanceof JSConstantBooleanNode) {
            return IsIdenticalBooleanNode.create((boolean) left.execute(null), right, true);
        } else if (right instanceof JSConstantBooleanNode) {
            return IsIdenticalBooleanNode.create((boolean) right.execute(null), left, false);
        } else if (left instanceof JSConstantUndefinedNode) {
            return IsIdenticalUndefinedNode.create(right, true);
        } else if (right instanceof JSConstantUndefinedNode) {
            return IsIdenticalUndefinedNode.create(left, false);
        }
        return JSStrictEqualNodeGen.create(left, right);
    }

    public abstract boolean executeBoolean(Object left, Object right);

    @Specialization
    public static boolean doInt(int a, int b) {
        return a == b;
    }

    public static boolean doDouble(double a, double b, int type) {
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
    public static boolean doBoolean(boolean a, boolean b) {
        return a == b;
    }

    @Specialization
    public static boolean doBigInt(BigInt a, BigInt b) {
        return a.compareTo(b) == 0;
    }

    @Specialization
    public static boolean doBigIntDouble(@SuppressWarnings("unused") BigInt a, @SuppressWarnings("unused") double b) {
        return false;
    }

    @Specialization
    public static boolean doDoubleBigInt(double a, BigInt b) {
        return doBigIntDouble(b, a);
    }

    @Specialization(guards = {"isUndefined(a)"})
    public static boolean doUndefinedA(Object a, Object b) {
        return a == b;
    }

    @Specialization(guards = {"isUndefined(b)"})
    public static boolean doUndefinedB(Object a, Object b) {
        return a == b;
    }

    @Specialization
    public static boolean doJSObjectA(JSObject a, Object b) {
        return a == b;
    }

    @Specialization
    public static boolean doJSObjectB(Object a, JSObject b) {
        return a == b;
    }

    @Specialization(guards = {"isJSNull(a)", "isJSNull(b)"})
    public static boolean doNullNull(@SuppressWarnings("unused") Object a, @SuppressWarnings("unused") Object b) {
        return true;
    }

    @Specialization(guards = {"isJSNull(a)", "!isNullOrUndefined(b)"})
    public static boolean doNullA(@SuppressWarnings("unused") Object a, Object b,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary nullInterop) {
        assert b != Undefined.instance;
        return nullInterop.isNull(b);
    }

    @Specialization(guards = {"!isNullOrUndefined(a)", "isJSNull(b)"})
    public static boolean doNullB(Object a, @SuppressWarnings("unused") Object b,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary nullInterop) {
        assert a != Undefined.instance;
        return nullInterop.isNull(a);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isReferenceEquals(a, b)")
    public static boolean doTruffleStringIdentity(TruffleString a, TruffleString b) {
        return true;
    }

    @Specialization(replaces = "doTruffleStringIdentity")
    public static boolean doTruffleString(TruffleString a, TruffleString b,
                    @Cached TruffleString.EqualNode equalsNode) {
        return equalsNode.execute(a, b, TruffleString.Encoding.UTF_16);
    }

    @Specialization
    public static boolean doSymbol(Symbol a, Symbol b) {
        return a == b;
    }

    @Specialization(guards = {"isBoolean(a) != isBoolean(b)"})
    public static boolean doBooleanNotBoolean(Object a, Object b) {
        assert (a != null) && (b != null);
        return false;
    }

    @Specialization(guards = {"isSymbol(a) != isSymbol(b)"})
    public static boolean doSymbolNotSymbol(Object a, Object b) {
        assert (a != null) && (b != null);
        return false;
    }

    @Specialization(guards = {"isString(a) != isString(b)"})
    public static boolean doStringNotString(Object a, Object b) {
        assert (a != null) && (b != null);
        return false;
    }

    @Specialization
    public static boolean doLong(long a, long b) {
        return a == b;
    }

    @InliningCutoff
    @Specialization(guards = {"isAForeign || isBForeign"}, limit = "InteropLibraryLimit")
    public static boolean doForeignObject(Object a, Object b,
                    @Bind("isForeignObjectOrNumber(a)") boolean isAForeign,
                    @Bind("isForeignObjectOrNumber(b)") boolean isBForeign,
                    @Bind("type()") int type,
                    @CachedLibrary("a") InteropLibrary aInterop,
                    @CachedLibrary("b") InteropLibrary bInterop) {
        if (aInterop.isNumber(a) && bInterop.isNumber(b)) {
            return doForeignNumber(a, b, aInterop, bInterop, isAForeign, isBForeign, type);
        }
        return aInterop.isIdentical(a, b, bInterop) || (aInterop.isNull(a) && bInterop.isNull(b));
    }

    private static boolean doForeignNumber(Object a, Object b, InteropLibrary aInterop, InteropLibrary bInterop, boolean isAForeign, boolean isBForeign, int type) {
        try {
            if (isAForeign != isBForeign) {
                if (a instanceof BigInt) {
                    assert !(b instanceof BigInt) : b;
                    return false;
                } else if (b instanceof BigInt) {
                    assert !(a instanceof BigInt) : a;
                    return false;
                }
            } else {
                assert isAForeign && isBForeign && !(a instanceof BigInt || b instanceof BigInt);
            }
            if (aInterop.fitsInDouble(a) && bInterop.fitsInDouble(b)) {
                return doDouble(aInterop.asDouble(a), bInterop.asDouble(b), type);
            } else if (aInterop.fitsInLong(a) && bInterop.fitsInLong(b)) {
                return aInterop.asLong(a) == bInterop.asLong(b);
            } else if (aInterop.fitsInBigInteger(a) && bInterop.fitsInBigInteger(b)) {
                return BigInt.fromBigInteger(aInterop.asBigInteger(a)).compareTo(BigInt.fromBigInteger(bInterop.asBigInteger(b))) == 0;
            }
        } catch (UnsupportedMessageException e) {
            assert false : e;
        }
        return false;
    }

    @Fallback
    public static boolean doFallback(Object a, Object b) {
        assert !JSRuntime.identical(a, b) : a + " (" + (a == null ? "null" : a.getClass()) + ")" + ", " + b + " (" + (b == null ? "null" : b.getClass()) + ")";
        return false;
    }
}
