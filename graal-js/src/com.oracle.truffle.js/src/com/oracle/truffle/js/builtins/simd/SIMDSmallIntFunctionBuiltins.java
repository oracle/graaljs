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
package com.oracle.truffle.js.builtins.simd;

import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDAddNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDAddSaturateNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDAndNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDCheckNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDEqualNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDExtractLaneNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDFromTIMDBitsNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDFromTIMDNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDGreaterThanNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDGreaterThanOrEqualNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDLessThanNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDLessThanOrEqualNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDLoadNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDMulNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDNegNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDNotEqualNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDNotNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDOrNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDReplaceLaneNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDSelectNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDShiftLeftByScalarNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDShiftRightByScalarNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDShuffleNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDSplatNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDStoreNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDSubNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDSubSaturateNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDSwizzleNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDXorNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.SIMDType;

public class SIMDSmallIntFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<SIMDSmallIntFunctionBuiltins.SIMDTypeFunction> {
    public SIMDSmallIntFunctionBuiltins(String typeName, SIMDType simdContext) {
        super(typeName, SIMDTypeFunction.class);
        this.simdContext = simdContext;
    }

    protected final SIMDType simdContext;

    public enum SIMDTypeFunction implements BuiltinEnum<SIMDTypeFunction> {
        splat(1),
        check(1),
        add(2),
        sub(2),
        mul(2),
        neg(1),
        and(2),
        xor(2),
        or(2),
        not(2),
        lessThan(2),
        lessThanOrEqual(2),
        greaterThan(2),
        greaterThanOrEqual(2),
        equal(2),
        notEqual(2),
        select(3),
        addSaturate(2),
        subSaturate(2),
        shiftLeftByScalar(2),
        shiftRightByScalar(2),
        extractLane(2),
        replaceLane(3),
        store(3),
        store1(3),
        store2(3),
        store3(3),
        load(2),
        load1(2),
        load2(2),
        load3(2),
        fromInt32x4Bits(1),
        fromUint32x4Bits(1),
        fromInt16x8Bits(1),
        fromUint16x8Bits(1),
        fromInt8x16Bits(1),
        fromUint8x16Bits(1),
        fromFloat32x4Bits(1),
        fromInt32x4(1),
        fromUint32x4(1),
        fromInt16x8(1),
        fromUint16x8(1),
        fromInt8x16(1),
        fromUint8x16(1),
        fromFloat32x4(1),
        swizzle(1),
        shuffle(2);

        private final int length;

        SIMDTypeFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, SIMDTypeFunction builtinEnum) {
        switch (builtinEnum) {
            case add:
                return SIMDAddNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case addSaturate:
                return SIMDAddSaturateNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case and:
                return SIMDAndNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case check:
                return SIMDCheckNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case equal:
                return SIMDEqualNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case extractLane:
                return SIMDExtractLaneNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case fromInt32x4Bits:
                return SIMDFromTIMDBitsNode.create(context, builtin, simdContext, SIMDType.INT32X4_FACTORY.createSimdType(), args().fixedArgs(1).createArgumentNodes(context));
            case fromUint32x4Bits:
                return SIMDFromTIMDBitsNode.create(context, builtin, simdContext, SIMDType.UINT32X4_FACTORY.createSimdType(), args().fixedArgs(1).createArgumentNodes(context));
            case fromInt16x8Bits:
                return SIMDFromTIMDBitsNode.create(context, builtin, simdContext, SIMDType.INT16X8_FACTORY.createSimdType(), args().fixedArgs(1).createArgumentNodes(context));
            case fromUint16x8Bits:
                return SIMDFromTIMDBitsNode.create(context, builtin, simdContext, SIMDType.UINT16X8_FACTORY.createSimdType(), args().fixedArgs(1).createArgumentNodes(context));
            case fromInt8x16Bits:
                return SIMDFromTIMDBitsNode.create(context, builtin, simdContext, SIMDType.INT8X16_FACTORY.createSimdType(), args().fixedArgs(1).createArgumentNodes(context));
            case fromUint8x16Bits:
                return SIMDFromTIMDBitsNode.create(context, builtin, simdContext, SIMDType.UINT8X16_FACTORY.createSimdType(), args().fixedArgs(1).createArgumentNodes(context));
            case fromFloat32x4Bits:
                return SIMDFromTIMDBitsNode.create(context, builtin, simdContext, SIMDType.FLOAT32X4_FACTORY.createSimdType(), args().fixedArgs(1).createArgumentNodes(context));
            case fromInt32x4:
                return SIMDFromTIMDNode.create(context, builtin, simdContext, SIMDType.INT32X4_FACTORY.createSimdType(), args().fixedArgs(2).createArgumentNodes(context));
            case fromUint32x4:
                return SIMDFromTIMDNode.create(context, builtin, simdContext, SIMDType.UINT32X4_FACTORY.createSimdType(), args().fixedArgs(2).createArgumentNodes(context));
            case fromInt16x8:
                return SIMDFromTIMDNode.create(context, builtin, simdContext, SIMDType.INT16X8_FACTORY.createSimdType(), args().fixedArgs(2).createArgumentNodes(context));
            case fromUint16x8:
                return SIMDFromTIMDNode.create(context, builtin, simdContext, SIMDType.UINT16X8_FACTORY.createSimdType(), args().fixedArgs(2).createArgumentNodes(context));
            case fromInt8x16:
                return SIMDFromTIMDNode.create(context, builtin, simdContext, SIMDType.INT8X16_FACTORY.createSimdType(), args().fixedArgs(2).createArgumentNodes(context));
            case fromUint8x16:
                return SIMDFromTIMDNode.create(context, builtin, simdContext, SIMDType.UINT8X16_FACTORY.createSimdType(), args().fixedArgs(2).createArgumentNodes(context));
            case fromFloat32x4:
                return SIMDFromTIMDNode.create(context, builtin, simdContext, SIMDType.FLOAT32X4_FACTORY.createSimdType(), args().fixedArgs(2).createArgumentNodes(context));
            case greaterThan:
                return SIMDGreaterThanNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case greaterThanOrEqual:
                return SIMDGreaterThanOrEqualNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case lessThan:
                return SIMDLessThanNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case lessThanOrEqual:
                return SIMDLessThanOrEqualNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case load:
                return SIMDLoadNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case load1:
                return SIMDLoadNode.create(context, builtin, simdContext, 1, args().fixedArgs(2).createArgumentNodes(context));
            case load2:
                return SIMDLoadNode.create(context, builtin, simdContext, 2, args().fixedArgs(2).createArgumentNodes(context));
            case load3:
                return SIMDLoadNode.create(context, builtin, simdContext, 3, args().fixedArgs(2).createArgumentNodes(context));
            case mul:
                return SIMDMulNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case neg:
                return SIMDNegNode.create(context, builtin, simdContext, args().fixedArgs(1).createArgumentNodes(context));
            case not:
                return SIMDNotNode.create(context, builtin, simdContext, args().fixedArgs(1).createArgumentNodes(context));
            case notEqual:
                return SIMDNotEqualNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case or:
                return SIMDOrNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case replaceLane:
                return SIMDReplaceLaneNode.create(context, builtin, simdContext, args().fixedArgs(3).createArgumentNodes(context));
            case select:
                return SIMDSelectNode.create(context, builtin, simdContext, args().fixedArgs(3).createArgumentNodes(context));
            case shiftLeftByScalar:
                return SIMDShiftLeftByScalarNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case shiftRightByScalar:
                return SIMDShiftRightByScalarNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case shuffle:
                return SIMDShuffleNode.create(context, builtin, simdContext, args().varArgs().createArgumentNodes(context));
            case splat:
                return SIMDSplatNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case store:
                return SIMDStoreNode.create(context, builtin, simdContext, args().fixedArgs(3).createArgumentNodes(context));
            case store1:
                return SIMDStoreNode.create(context, builtin, simdContext, 1, args().fixedArgs(3).createArgumentNodes(context));
            case store2:
                return SIMDStoreNode.create(context, builtin, simdContext, 2, args().fixedArgs(3).createArgumentNodes(context));
            case store3:
                return SIMDStoreNode.create(context, builtin, simdContext, 3, args().fixedArgs(3).createArgumentNodes(context));
            case sub:
                return SIMDSubNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case subSaturate:
                return SIMDSubSaturateNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case swizzle:
                return SIMDSwizzleNode.create(context, builtin, simdContext, args().varArgs().createArgumentNodes(context));
            case xor:
                return SIMDXorNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }
}
