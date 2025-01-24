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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Strings;

@NodeInfo(shortName = "-")
public abstract class JSSubtractNode extends JSBinaryNode implements Truncatable {

    @CompilationFinal boolean truncate;

    protected JSSubtractNode(boolean truncate, JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
        this.truncate = truncate;
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right, boolean truncate) {
        return JSSubtractNodeGen.create(truncate, left, right);
    }

    public static JavaScriptNode create(JavaScriptNode left, JavaScriptNode right) {
        return create(left, right, false);
    }

    public abstract Object execute(Object a, Object b);

    @Specialization(rewriteOn = ArithmeticException.class)
    protected int doInt(int a, int b) {
        if (truncate) {
            return a - b;
        } else {
            return Math.subtractExact(a, b);
        }
    }

    @Specialization(replaces = "doInt")
    protected double doDouble(double a, double b) {
        return a - b;
    }

    @Specialization()
    protected BigInt doBigInt(BigInt a, BigInt b) {
        return a.subtract(b);
    }

    @InliningCutoff
    @Specialization(guards = {"hasOverloadedOperators(a) || hasOverloadedOperators(b)"})
    protected Object doOverloaded(Object a, Object b,
                    @Cached("createNumeric(getOverloadedOperatorName())") JSOverloadedBinaryNode overloadedOperatorNode) {
        return overloadedOperatorNode.execute(a, b);
    }

    protected TruffleString getOverloadedOperatorName() {
        return Strings.SYMBOL_MINUS;
    }

    @Specialization(guards = {"!hasOverloadedOperators(a)", "!hasOverloadedOperators(b)"}, replaces = {"doDouble", "doBigInt"})
    protected static Object doGeneric(Object a, Object b,
                    @Bind Node node,
                    @Cached JSToNumericNode toNumericA,
                    @Cached JSToNumericNode toNumericB,
                    @Cached("copyRecursive()") JavaScriptNode subtract,
                    @Cached InlinedBranchProfile mixedNumericTypes) {

        Object castA = toNumericA.execute(a);
        Object castB = toNumericB.execute(b);
        ensureBothSameNumericType(castA, castB, node, mixedNumericTypes);
        return ((JSSubtractNode) subtract).execute(castA, castB);
    }

    public final JavaScriptNode copyRecursive() {
        return create(null, null, truncate);
    }

    @Override
    public void setTruncate() {
        CompilerAsserts.neverPartOfCompilation();
        if (!truncate) {
            truncate = true;
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSSubtractNodeGen.create(cloneUninitialized(getLeft(), materializedTags), cloneUninitialized(getRight(), materializedTags), truncate);
    }
}
