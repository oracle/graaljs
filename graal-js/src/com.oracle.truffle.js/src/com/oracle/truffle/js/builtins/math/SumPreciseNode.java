/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.RequireObjectCoercibleNode;
import com.oracle.truffle.js.nodes.cast.IsNumberNode;
import com.oracle.truffle.js.nodes.cast.JSNumberToDoubleNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.external.XSum.SmallAccumulator;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;

public abstract class SumPreciseNode extends MathOperation {

    public SumPreciseNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization
    protected double sumPrecise(Object items,
                    @Cached RequireObjectCoercibleNode requireObjectCoercible,
                    @Cached(inline = true) GetIteratorNode getIteratorNode,
                    @Cached IteratorStepNode iteratorStepNode,
                    @Cached IteratorValueNode iteratorValueNode,
                    @Cached("create(getContext())") IteratorCloseNode iteratorCloseNode,
                    @Cached IsNumberNode isNumberNode,
                    @Cached JSNumberToDoubleNode toDoubleNode,
                    @Cached InlinedBranchProfile errorBranch) {
        requireObjectCoercible.executeVoid(items);
        IteratorRecord iter = getIteratorNode.execute(this, items);

        boolean negativeZero = true;
        SmallAccumulator acc = new SmallAccumulator();
        try {
            while (true) {
                Object next = iteratorStepNode.execute(iter);
                if (next == Boolean.FALSE) {
                    break;
                }
                Object nextValue = iteratorValueNode.execute(next);
                if (isNumberNode.execute(this, nextValue)) {
                    double doubleValue = toDoubleNode.execute(this, nextValue);
                    if (!JSRuntime.isNegativeZero(doubleValue)) {
                        negativeZero = false;
                        acc.add(doubleValue);
                    }
                } else {
                    errorBranch.enter(this);
                    throw Errors.createTypeErrorNotANumber(nextValue);
                }
            }
            return negativeZero ? -0d : acc.round();
        } catch (AbstractTruffleException ex) {
            errorBranch.enter(this);
            iteratorCloseNode.executeAbrupt(iter.getIterator());
            throw ex;
        }
    }

}
