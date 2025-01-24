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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;

/**
 * 11.7.3 The Unsigned Right Shift Operator (>>>).
 */
@NodeInfo(shortName = ">>>")
public abstract class JSUnsignedRightShiftNode extends JSBinaryNode {

    protected JSUnsignedRightShiftNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        Truncatable.truncate(left);
        Truncatable.truncate(right);
        if (JSConfig.UseSuperOperations && right instanceof JSConstantIntegerNode) {
            return JSUnsignedRightShiftConstantNode.create(left, right);
        }
        return JSUnsignedRightShiftNodeGen.create(left, right);
    }

    static JSUnsignedRightShiftNode create() {
        return JSUnsignedRightShiftNodeGen.create(null, null);
    }

    protected final Number executeNumber(Object a, Object b) {
        return (Number) execute(a, b);
    }

    public abstract Object execute(Object a, Object b);

    protected static boolean rvalZero(int b) {
        return (b & 0x1f) == 0;
    }

    @Specialization(guards = {"rvalZero(b)", "a >= 0"})
    protected int doIntegerFast(int a, @SuppressWarnings("unused") int b) {
        return a;
    }

    @Specialization(guards = "a >= 0")
    protected int doInteger(int a, int b) {
        return a >>> b;
    }

    @Specialization(guards = "!rvalZero(b)")
    protected int doIntegerNegative(int a, int b) {
        return a >>> b;
    }

    @Specialization(guards = "rvalZero(b)")
    protected double doDoubleZero(double a, @SuppressWarnings("unused") int b,
                    @Cached @Shared JSToUInt32Node lvalToUint32Node) {
        return lvalToUint32Node.executeLong(a);
    }

    @Specialization(guards = "!rvalZero(b)")
    protected Number doDouble(double a, int b,
                    @Cached @Shared JSToUInt32Node lvalToUint32Node,
                    @Cached @Shared InlinedConditionProfile returnType) {

        long lnum = lvalToUint32Node.executeLong(a);
        int shiftCount = b & 0x1F;
        if (returnType.profile(this, lnum >= Integer.MAX_VALUE || lnum <= Integer.MIN_VALUE)) {
            return (double) (lnum >>> shiftCount);
        }
        return (int) (lnum >>> shiftCount);
    }

    @Specialization
    protected Number doIntDouble(int a, double b,
                    @Cached @Shared JSToUInt32Node lvalToUint32Node,
                    @Cached @Shared JSToUInt32Node rvalToUint32Node,
                    @Cached @Shared InlinedConditionProfile returnType) {

        long lnum = lvalToUint32Node.executeLong(a);
        int shiftCount = (int) rvalToUint32Node.executeLong(b) & 0x1F;
        if (returnType.profile(this, lnum >= Integer.MAX_VALUE || lnum <= Integer.MIN_VALUE)) {
            return (double) (lnum >>> shiftCount);
        }
        return (int) (lnum >>> shiftCount);
    }

    @Specialization
    protected double doDoubleDouble(double a, double b,
                    @Cached @Shared JSToUInt32Node lvalToUint32Node,
                    @Cached @Shared JSToUInt32Node rvalToUint32Node) {
        return (lvalToUint32Node.executeLong(a) >>> ((int) rvalToUint32Node.executeLong(b) & 0x1F));
    }

    @Specialization
    protected Number doBigInt(@SuppressWarnings("unused") BigInt a, @SuppressWarnings("unused") BigInt b) {
        throw Errors.createTypeError("BigInts have no unsigned right shift, use >> instead");
    }

    @InliningCutoff
    @Specialization(guards = {"hasOverloadedOperators(a) || hasOverloadedOperators(b)"})
    protected Object doOverloaded(Object a, Object b,
                    @Cached("createNumeric(getOverloadedOperatorName())") JSOverloadedBinaryNode overloadedOperatorNode) {
        return overloadedOperatorNode.execute(a, b);
    }

    protected TruffleString getOverloadedOperatorName() {
        return Strings.ANGLE_BRACKET_CLOSE_3;
    }

    @Specialization(guards = {"!hasOverloadedOperators(lval)", "!hasOverloadedOperators(rval)", "!isHandled(lval, rval)"})
    protected static Number doGeneric(Object lval, Object rval,
                    @Bind Node node,
                    @Cached JSToNumericNode lvalToNumericNode,
                    @Cached JSToNumericNode rvalToNumericNode,
                    @Cached JSUnsignedRightShiftNode innerShiftNode,
                    @Cached InlinedBranchProfile mixedNumericTypes) {
        Object lnum = lvalToNumericNode.execute(lval);
        Object rnum = rvalToNumericNode.execute(rval);
        ensureBothSameNumericType(lnum, rnum, node, mixedNumericTypes);
        return innerShiftNode.executeNumber(lnum, rnum);
    }

    protected static boolean isHandled(Object lval, Object rval) {
        return (lval instanceof Integer || lval instanceof Double || lval instanceof SafeInteger) && (rval instanceof Integer || rval instanceof Double || rval instanceof SafeInteger);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSUnsignedRightShiftNodeGen.create(cloneUninitialized(getLeft(), materializedTags), cloneUninitialized(getRight(), materializedTags));
    }
}
