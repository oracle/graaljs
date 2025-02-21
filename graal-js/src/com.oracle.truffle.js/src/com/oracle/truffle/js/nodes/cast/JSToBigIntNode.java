/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;

/**
 * Implementation of the abstract operation ToBigInt(argument).
 */
@ImportStatic(JSConfig.class)
@GenerateUncached
public abstract class JSToBigIntNode extends JavaScriptBaseNode {

    public abstract BigInt execute(Object value);

    public final BigInt executeBigInteger(Object value) {
        return execute(value);
    }

    @NeverDefault
    public static JSToBigIntNode create() {
        return JSToBigIntNodeGen.create();
    }

    @NeverDefault
    public static JSToBigIntNode getUncached() {
        return JSToBigIntNodeGen.getUncached();
    }

    @Specialization
    protected static BigInt doBigInt(BigInt value) {
        return value;
    }

    @Specialization(guards = {"!isBigInt(value)"})
    protected final BigInt doOther(Object value,
                    @Cached JSToPrimitiveNode toPrimitiveNode,
                    @Cached JSPrimitiveToBigIntNode primitiveToBigInt) {
        return primitiveToBigInt.executeBigInt(this, toPrimitiveNode.executeHintNumber(value));
    }

    /**
     * Implementation of the abstract operation ToBigInt(argument) where the argument has already
     * been converted ToPrimitive.
     */
    @GenerateInline
    @GenerateCached(false)
    @GenerateUncached
    protected abstract static class JSPrimitiveToBigIntNode extends JavaScriptBaseNode {

        public abstract BigInt executeBigInt(Node node, Object value);

        @Specialization
        protected static BigInt doBoolean(boolean value) {
            return value ? BigInt.ONE : BigInt.ZERO;
        }

        @Specialization(guards = "!value.isForeign()")
        protected static BigInt doBigInt(BigInt value) {
            return value;
        }

        @Specialization(guards = "value.isForeign()")
        protected static BigInt doForeignBigInt(Node node, BigInt value) {
            throw Errors.createErrorCannotConvertToBigInt(JSErrorType.TypeError, value, node);
        }

        @Specialization(guards = "isNumber(value) || isNumberLong(value)")
        protected static BigInt doNumber(Node node, Object value) {
            throw Errors.createErrorCannotConvertToBigInt(JSErrorType.TypeError, value, node);
        }

        @Specialization(guards = "isSymbol(value) || isNullOrUndefined(value)")
        protected static BigInt doSymbolNullOrUndefined(Node node, Object value) {
            throw Errors.createErrorCannotConvertToBigInt(JSErrorType.TypeError, value, node);
        }

        @Specialization
        protected static BigInt doString(Node node, TruffleString value) {
            try {
                return Strings.parseBigInt(value);
            } catch (NumberFormatException e) {
                throw Errors.createErrorCannotConvertToBigInt(JSErrorType.SyntaxError, value, node);
            }
        }
    }

    /**
     * Implementation of the ToBigInt conversion performed by the BigInt(argument) where the
     * argument has already been converted ToPrimitive, i.e.:
     *
     * If prim is a Number, return NumberToBigInt(prim), otherwise ToBigInt(prim).
     *
     * @see JSNumberToBigIntNode
     */
    @GenerateInline
    @GenerateCached(false)
    public abstract static class CoercePrimitiveToBigIntNode extends JavaScriptBaseNode {

        public abstract BigInt executeBigInt(Node node, Object value);

        @Specialization
        protected static BigInt doBoolean(boolean value) {
            return value ? BigInt.ONE : BigInt.ZERO;
        }

        @Specialization(guards = "!value.isForeign()")
        protected static BigInt doBigInt(BigInt value) {
            return value;
        }

        @Specialization(guards = "value.isForeign()")
        protected static BigInt doForeignBigInt(BigInt value) {
            return value.clearForeign();
        }

        @Specialization
        protected static BigInt doInteger(int value) {
            return BigInt.valueOf(value);
        }

        @Specialization
        protected static BigInt doSafeInteger(SafeInteger value) {
            return BigInt.valueOf(value.longValue());
        }

        @Specialization
        protected static BigInt doLong(long value) {
            return BigInt.valueOf(value);
        }

        @SuppressWarnings("truffle-inlining")
        @Specialization
        protected static BigInt doDouble(double value,
                        @Cached JSNumberToBigIntNode numberToBigInt) {
            return numberToBigInt.executeBigInt(value);
        }

        @Specialization(guards = "isSymbol(value) || isNullOrUndefined(value)")
        protected static BigInt doSymbolNullOrUndefined(Node node, Object value) {
            throw Errors.createErrorCannotConvertToBigInt(JSErrorType.TypeError, value, node);
        }

        @Specialization
        protected static BigInt doString(Node node, TruffleString value) {
            return JSPrimitiveToBigIntNode.doString(node, value);
        }
    }
}
