/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.temporal;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantFunctionBuiltinsFactory.JSTemporalInstantCompareNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantFunctionBuiltinsFactory.JSTemporalInstantFromEpochNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalInstantFunctionBuiltinsFactory.JSTemporalInstantFromNodeGen;
import com.oracle.truffle.js.nodes.cast.JSNumberToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.temporal.ToTemporalInstantNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstant;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalInstantObject;
import com.oracle.truffle.js.runtime.util.TemporalErrors;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalInstantFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalInstantFunctionBuiltins.TemporalInstantFunction> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalInstantFunctionBuiltins();

    protected TemporalInstantFunctionBuiltins() {
        super(JSTemporalInstant.CLASS_NAME, TemporalInstantFunction.class);
    }

    public enum TemporalInstantFunction implements BuiltinEnum<TemporalInstantFunction> {
        from(1),
        fromEpochMilliseconds(1),
        fromEpochNanoseconds(1),
        compare(2);

        private final int length;

        TemporalInstantFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalInstantFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return JSTemporalInstantFromNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case fromEpochMilliseconds:
                return JSTemporalInstantFromEpochNodeGen.create(context, builtin, TemporalUtil.BI_NS_PER_MS, true, args().fixedArgs(1).createArgumentNodes(context));
            case fromEpochNanoseconds:
                return JSTemporalInstantFromEpochNodeGen.create(context, builtin, BigInt.ONE, false, args().fixedArgs(1).createArgumentNodes(context));
            case compare:
                return JSTemporalInstantCompareNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalInstantFromNode extends JSBuiltinNode {

        public JSTemporalInstantFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSTemporalInstantObject from(Object item,
                        @Cached ToTemporalInstantNode toTemporalInstantNode) {
            if (TemporalUtil.isTemporalInstant(item)) {
                return JSTemporalInstant.create(getContext(), getRealm(), ((JSTemporalInstantObject) item).getNanoseconds());
            }
            return toTemporalInstantNode.execute(item);
        }

    }

    public abstract static class JSTemporalInstantFromEpochNode extends JSBuiltinNode {

        private final BigInt factor;
        private final boolean numberToBigIntConversion;

        public JSTemporalInstantFromEpochNode(JSContext context, JSBuiltin builtin, BigInt factor, boolean numberToBigIntConversion) {
            super(context, builtin);
            this.factor = factor;
            this.numberToBigIntConversion = numberToBigIntConversion;
        }

        @Specialization
        protected JSTemporalInstantObject from(Object epochParam,
                        @Cached JSToNumberNode toNumberNode,
                        @Cached JSNumberToBigIntNode numberToBigIntNode) {
            BigInt epochNanoseconds = null;
            if (numberToBigIntConversion) {
                Number epochConverted = toNumberNode.executeNumber(epochParam);
                epochNanoseconds = numberToBigIntNode.executeBigInt(epochConverted);
            } else {
                epochNanoseconds = JSRuntime.toBigInt(epochParam);
            }
            epochNanoseconds = epochNanoseconds.multiply(factor);
            if (!TemporalUtil.isValidEpochNanoseconds(epochNanoseconds)) {
                throw TemporalErrors.createRangeErrorInvalidNanoseconds();
            }
            return JSTemporalInstant.create(getContext(), getRealm(), epochNanoseconds);
        }

    }

    public abstract static class JSTemporalInstantCompareNode extends JSBuiltinNode {

        public JSTemporalInstantCompareNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int compare(Object obj1, Object obj2,
                        @Cached ToTemporalInstantNode toTemporalInstantNode) {
            JSTemporalInstantObject one = toTemporalInstantNode.execute(obj1);
            JSTemporalInstantObject two = toTemporalInstantNode.execute(obj2);
            return TemporalUtil.compareEpochNanoseconds(one.getNanoseconds(), two.getNanoseconds());
        }
    }

}
