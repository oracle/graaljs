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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.BigIntFunctionBuiltinsFactory.JSBigIntAsIntNNodeGen;
import com.oracle.truffle.js.builtins.BigIntFunctionBuiltinsFactory.JSBigIntAsUintNNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;

/**
 * Contains builtins for {@linkplain JSBigInt} function (constructor).
 */
public final class BigIntFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<BigIntFunctionBuiltins.BigIntFunction> {
    protected BigIntFunctionBuiltins() {
        super(JSBigInt.CLASS_NAME, BigIntFunction.class);
    }

    public enum BigIntFunction implements BuiltinEnum<BigIntFunction> {
        asUintN(2),
        asIntN(2);

        private final int length;

        BigIntFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            return 9;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, BigIntFunction builtinEnum) {
        switch (builtinEnum) {
            case asUintN:
                return JSBigIntAsUintNNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case asIntN:
                return JSBigIntAsIntNNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSBigIntAsUintNNode extends JSBuiltinNode {

        public JSBigIntAsUintNNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected BigInt doIt(Object bitsObj, Object bigIntObj,
                        @Cached("create()") JSToIndexNode toIndexNode,
                        @Cached("create()") JSToBigIntNode toBigIntNode) {

            long bits = toIndexNode.executeLong(bitsObj);
            BigInt bigint = toBigIntNode.executeBigInteger(bigIntObj);
            if (bits > JSRuntime.MAX_BIG_INT_EXPONENT) {
                if (bigint.signum() >= 0) {
                    return bigint;
                } else {
                    throw Errors.createRangeErrorBigIntMaxSizeExceeded();
                }
            } else {
                return bigint.mod((BigInt.TWO).pow((int) bits));
            }
        }
    }

    public abstract static class JSBigIntAsIntNNode extends JSBuiltinNode {

        public JSBigIntAsIntNNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected BigInt doIt(Object bitsObj, Object bigIntObj,
                        @Cached("create()") JSToIndexNode toIndexNode,
                        @Cached("create()") JSToBigIntNode toBigIntNode) {

            long bits = toIndexNode.executeLong(bitsObj);
            BigInt bigint = toBigIntNode.executeBigInteger(bigIntObj);
            if (bits > JSRuntime.MAX_BIG_INT_EXPONENT) {
                return bigint;
            }
            BigInt twoPowBits = BigInt.TWO.pow((int) bits);
            BigInt mod = bigint.mod(twoPowBits);
            if (bits > 0) {
                if (mod.compareTo(BigInt.TWO.pow((int) bits - 1)) >= 0) {
                    return mod.subtract(twoPowBits);
                } else {
                    return mod;
                }
            } else {
                return BigInt.ZERO;
            }

        }
    }
}
