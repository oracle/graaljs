/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.math;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.cast.JSNumberToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;

@GenerateCached(false)
public abstract class MinMaxNode extends JSBuiltinNode {

    public MinMaxNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    protected abstract int minOrMaxInt(int a, int b);

    protected abstract double minOrMaxDouble(double a, double b);

    @Specialization(guards = "args.length == 1")
    protected static Object do1(Object[] args,
                    @Cached @Shared JSToNumberNode toNumber0Node) {
        return toNumber0Node.execute(args[0]);
    }

    @Specialization(guards = {"args.length == 2"})
    protected final Object do2(Object[] args,
                    @Cached @Shared InlinedBranchProfile isIntBranch,
                    @Cached @Shared JSNumberToDoubleNode numberToDouble,
                    @Cached @Shared JSToNumberNode toNumber0Node,
                    @Cached @Shared JSToNumberNode toNumber1Node) {
        Object a0 = args[0];
        Object a1 = args[1];
        if (a0 instanceof Integer i0 && a1 instanceof Integer i1) {
            isIntBranch.enter(this);
            return minOrMaxInt(i0, i1);
        }
        Number n0 = toNumber0Node.executeNumber(a0);
        Number n1 = toNumber1Node.executeNumber(a1);
        if (n0 instanceof Integer i0 && n1 instanceof Integer i1) {
            isIntBranch.enter(this);
            return minOrMaxInt(i0, i1);
        } else {
            // Implicit branch profile
            double d0 = numberToDouble.execute(this, n0);
            double d1 = numberToDouble.execute(this, n1);
            return minOrMaxDouble(d0, d1);
        }
    }

    @Specialization(guards = {"args.length == 3"})
    protected Object do3(Object[] args,
                    @Cached @Shared InlinedBranchProfile isIntBranch,
                    @Cached @Shared JSNumberToDoubleNode numberToDouble,
                    @Cached @Shared JSToNumberNode toNumber0Node,
                    @Cached @Shared JSToNumberNode toNumber1Node,
                    @Cached @Shared JSToNumberNode toNumber2Node) {
        Object a0 = args[0];
        Object a1 = args[1];
        Object a2 = args[2];
        if (a0 instanceof Integer i0 && a1 instanceof Integer i1 && a2 instanceof Integer i2) {
            isIntBranch.enter(this);
            return minOrMaxInt(minOrMaxInt(i0, i1), i2);
        }
        Number n0 = toNumber0Node.executeNumber(a0);
        Number n1 = toNumber1Node.executeNumber(a1);
        Number n2 = toNumber2Node.executeNumber(a2);
        if (n0 instanceof Integer i0 && n1 instanceof Integer i1 && n2 instanceof Integer i2) {
            isIntBranch.enter(this);
            return minOrMaxInt(minOrMaxInt(i0, i1), i2);
        } else {
            // Implicit branch profile
            double d0 = numberToDouble.execute(this, n0);
            double d1 = numberToDouble.execute(this, n1);
            double d2 = numberToDouble.execute(this, n2);
            double result = minOrMaxDouble(d0, d1);
            return minOrMaxDouble(result, d2);
        }
    }

    @Specialization(guards = "args.length >= 4")
    protected Object do4OrMore(Object[] args,
                    @Cached @Shared InlinedBranchProfile isIntBranch,
                    @Cached @Shared JSToNumberNode toNumber0Node,
                    @Cached @Shared JSToNumberNode toNumber1Node,
                    @Cached @Shared JSToNumberNode toNumber2Node,
                    @Cached @Shared JSNumberToDoubleNode numberToDouble) {
        Object a0 = args[0];
        Object n0 = toNumber0Node.execute(a0);
        double dresult;
        int i = 1;
        whileInt: if (n0 instanceof Integer i0) {
            isIntBranch.enter(this);
            int iresult = i0;
            for (; i < args.length; i++) {
                Object ai = args[i];
                Object ni = toNumber1Node.execute(ai);
                if (ni instanceof Integer ii) {
                    iresult = minOrMaxInt(iresult, ii);
                } else {
                    dresult = iresult;
                    break whileInt;
                }
            }
            return iresult;
        } else {
            // Implicit branch profile
            dresult = numberToDouble.execute(this, n0);
        }
        for (; i < args.length; i++) {
            Object ai = args[i];
            Object ni = toNumber2Node.executeNumber(ai);
            double di = numberToDouble.execute(this, ni);
            dresult = minOrMaxDouble(dresult, di);
        }
        return dresult;
    }
}
