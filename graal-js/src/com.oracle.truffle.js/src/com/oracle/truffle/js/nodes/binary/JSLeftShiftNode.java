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
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.Strings;

@NodeInfo(shortName = "<<")
public abstract class JSLeftShiftNode extends JSBinaryNode {

    protected JSLeftShiftNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        Truncatable.truncate(left);
        Truncatable.truncate(right);
        if (JSConfig.UseSuperOperations && right instanceof JSConstantIntegerNode) {
            return JSLeftShiftConstantNode.create(left, right);
        }
        return JSLeftShiftNodeGen.create(left, right);
    }

    public abstract Object executeObject(Object a, Object b);

    @Specialization
    protected int doInteger(int a, int b) {
        return a << b;
    }

    @Specialization(guards = "!largerThan2e32(b)")
    protected int doIntegerDouble(int a, double b) {
        return a << (int) ((long) b);
    }

    @Specialization
    protected Object doDouble(double a, double b,
                    @Cached @Shared JSLeftShiftNode leftShift,
                    @Cached JSToInt32Node leftInt32,
                    @Cached JSToUInt32Node rightUInt32) {

        return leftShift.executeObject(leftInt32.executeInt(a), rightUInt32.execute(b));
    }

    @Specialization
    protected BigInt doBigInt(BigInt a, BigInt b) {
        if (a.signum() == 0) {
            return BigInt.ZERO;
        }
        if (b.compareTo(BigInt.MAX_INT) < 0) {
            if (b.compareTo(BigInt.MIN_INT) > 0) {
                try {
                    return a.shiftLeft(b.intValue());
                } catch (ArithmeticException ae) {
                    throw Errors.createRangeErrorBigIntMaxSizeExceeded();
                }
            } else {
                return a.signum() < 0 ? BigInt.NEGATIVE_ONE : BigInt.ZERO;
            }
        } else {
            throw Errors.createRangeErrorBigIntMaxSizeExceeded();
        }
    }

    @InliningCutoff
    @Specialization(guards = {"hasOverloadedOperators(a) || hasOverloadedOperators(b)"})
    protected Object doOverloaded(Object a, Object b,
                    @Cached("createNumeric(getOverloadedOperatorName())") JSOverloadedBinaryNode overloadedOperatorNode) {
        return overloadedOperatorNode.execute(a, b);
    }

    protected TruffleString getOverloadedOperatorName() {
        return Strings.ANGLE_BRACKET_OPEN_2;
    }

    @Specialization(guards = {"!hasOverloadedOperators(a)", "!hasOverloadedOperators(b)"}, replaces = {"doInteger", "doIntegerDouble", "doDouble", "doBigInt"})
    protected static Object doGeneric(Object a, Object b,
                    @Bind Node node,
                    @Cached @Shared JSLeftShiftNode leftShift,
                    @Cached JSToNumericNode leftToNumeric,
                    @Cached JSToNumericNode rightToNumeric,
                    @Cached InlinedBranchProfile mixedNumericTypes) {
        Object operandA = leftToNumeric.execute(a);
        Object operandB = rightToNumeric.execute(b);
        ensureBothSameNumericType(operandA, operandB, node, mixedNumericTypes);
        return leftShift.executeObject(operandA, operandB);
    }

    @NeverDefault
    public static JSLeftShiftNode create() {
        return JSLeftShiftNodeGen.create(null, null);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSLeftShiftNodeGen.create(cloneUninitialized(getLeft(), materializedTags), cloneUninitialized(getRight(), materializedTags));
    }
}
