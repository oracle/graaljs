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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDAbsNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDAddNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDAddSaturateNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDAllTrueNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDAndNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDAnyTrueNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDCheckNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDDivNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDEqualNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDExtractLaneNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDFromTIMDBitsNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDFromTIMDNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDGreaterThanNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDGreaterThanOrEqualNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDLessThanNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDLessThanOrEqualNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDLoadNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDMaxNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDMaxNumNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDMinNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDMinNumNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDMulNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDNegNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDNotEqualNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDNotNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDOrNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDReplaceLaneNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDSelectNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDShiftLeftByScalarNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDShiftRightByScalarNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDShuffleNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDSplatNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDSqrtNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDStoreNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDSubNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDSubSaturateNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDSwizzleNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDXorNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDreciprocalApproximationNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltinsFactory.SIMDreciprocalSqrtApproximationNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.binary.JSEqualNode;
import com.oracle.truffle.js.nodes.cast.JSToLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSSIMD;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.SIMDType;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDBool16x8;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDBool32x4;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDBool8x16;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDFloat32x4;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDInt16x8;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDInt32x4;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDInt8x16;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDTypeInt;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDUint16x8;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDUint32x4;
import com.oracle.truffle.js.runtime.builtins.SIMDType.SIMDUint8x16;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class SIMDTypeFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<SIMDTypeFunctionBuiltins.SIMDTypeFunction> {
    public SIMDTypeFunctionBuiltins(String typeName, SIMDType simdContext) {
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
        div(2),
        max(2),
        min(2),
        maxNum(2),
        minNum(2),
        neg(1),
        sqrt(1),
        reciprocalApproximation(1),
        reciprocalSqrtApproximation(1),
        abs(1),
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
        anyTrue(1),
        allTrue(1),
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
            case abs:
                return SIMDAbsNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case add:
                return SIMDAddNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case addSaturate:
                return SIMDAddSaturateNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case allTrue:
                return SIMDAllTrueNode.create(context, builtin, simdContext, args().fixedArgs(1).createArgumentNodes(context));
            case and:
                return SIMDAndNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case anyTrue:
                return SIMDAnyTrueNode.create(context, builtin, simdContext, args().fixedArgs(1).createArgumentNodes(context));
            case check:
                return SIMDCheckNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case div:
                return SIMDDivNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
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
            case max:
                return SIMDMaxNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case maxNum:
                return SIMDMaxNumNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case min:
                return SIMDMinNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case minNum:
                return SIMDMinNumNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
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
            case reciprocalApproximation:
                return SIMDreciprocalApproximationNode.create(context, builtin, simdContext, args().fixedArgs(1).createArgumentNodes(context));
            case reciprocalSqrtApproximation:
                return SIMDreciprocalSqrtApproximationNode.create(context, builtin, simdContext, args().fixedArgs(1).createArgumentNodes(context));
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
            case sqrt:
                return SIMDSqrtNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
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

    private static boolean fromTIMDGuard(SIMDType timd, SIMDType simdContext) {
        if (timd.equals(simdContext)) {
            return false;
        }
        if (simdContext.getFactory().numberOfElements() != timd.getFactory().numberOfElements()) {
            return false;
        }
        if (isBooleanSIMD(timd) || isBooleanSIMD(simdContext)) {
            return false;
        }
        return true;
    }

    private static boolean isBooleanSIMD(SIMDType t) {
        return SIMDType.SIMDTypedBoolean.class.isAssignableFrom(t.getClass());
    }

    @SuppressWarnings("unused")
    private static boolean isIntegerSIMD(SIMDType t) {
        return SIMDType.SIMDTypeInt.class.isAssignableFrom(t.getClass());
    }

    public abstract static class JSBasicSimdOperation extends JSBuiltinNode {

        protected final Class<?> simdElementType;
        protected final int numberOfElements;
        protected final SIMDType simdContext;
        protected final BranchProfile errorBranch = BranchProfile.create();
        protected final ValueProfile typedArrayProfile = ValueProfile.createIdentityProfile();
        @Child private JSToLengthNode toLengthNode = JSToLengthNode.create();
        @Child private JSEqualNode equalNode = JSEqualNode.create();
        @Child private JSToNumberNode toNumberNode = JSToNumberNode.create();
        @Child protected JSToUInt32Node toUInt32Node = JSToUInt32Node.create();

        @Children protected final SIMDCastNode[] castNodes;

        public JSBasicSimdOperation(JSContext context, JSBuiltin builtin, SIMDType simdContext, int numberOfNodes) {
            super(context, builtin);
            this.simdContext = simdContext;
            this.numberOfElements = simdContext == null ? 16 : simdContext.getNumberOfElements();

            if (simdContext != null) {
                this.simdElementType = simdContext.getClass();
            } else {
                this.simdElementType = null;
            }

            castNodes = new SIMDCastNode[numberOfNodes];
            for (int i = 0; i < numberOfNodes; i++) {
                castNodes[i] = SIMDCastNode.create(simdContext);
            }
        }

        public JSBasicSimdOperation(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            this(context, builtin, simdContext, simdContext == null ? 16 : simdContext.getNumberOfElements());
        }

        protected JSEqualNode getEqualNode() {
            if (equalNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                equalNode = insert(JSEqualNode.create());
            }
            return equalNode;
        }

        protected JSToNumberNode getToNumberNode() {
            if (toNumberNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toNumberNode = insert(JSToNumberNode.create());
            }
            return toNumberNode;
        }

        protected JSToUInt32Node getToUInt32Node() {
            if (toUInt32Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toUInt32Node = insert(JSToUInt32Node.create());
            }
            return toUInt32Node;
        }

        protected JSToLengthNode getToLengthNode() {
            if (toLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toLengthNode = insert(JSToLengthNode.create());
            }
            return toLengthNode;
        }

        protected Object cast(int i, Object o) {
            return castNodes[i].execute(o);
        }

        protected static final Object getLane(DynamicObject simd, int lane) {
            Object obj = JSSIMD.simdGetArray(simd, JSSIMD.isJSSIMD(simd));
            Object[] array = (Object[]) obj;
            Object value = array[lane];
            assert value != null;
            return value;
        }

        protected static final void setLane(DynamicObject simd, int lane, Object value) {
            Object obj = JSSIMD.simdGetArray(simd, JSSIMD.isJSSIMD(simd));
            Object[] array = (Object[]) obj;
            array[lane] = value;
        }

        // SameValueZero(x, y)
        protected boolean sameValueZero(Object x, Object y) {
            if (x == Undefined.instance && y == Undefined.instance) {
                return true;
            }
            if (x == Null.instance && y == Null.instance) {
                return true;
            }
            if (JSRuntime.isNumber(x) && JSRuntime.isNumber(y)) {
                double xd = JSRuntime.toDouble((Number) x);
                double yd = JSRuntime.toDouble((Number) y);
                if (JSRuntime.isNaN(x) && JSRuntime.isNaN(y)) {
                    return true;
                }
                if (xd == +0.0 && yd == -0.0) {
                    return true;
                }
                if (xd == -0.0 && yd == +0.0) {
                    return true;
                }
                if (Double.compare(xd, yd) == 0) {
                    return true;
                }
                return false;
            }
            if (JSRuntime.isString(x) && JSRuntime.isString(y)) {
                if (((String) x).equals(y)) {
                    return true;
                }
                return false;
            }
            if (x instanceof Boolean && y instanceof Boolean) {
                if ((boolean) x == (boolean) y) {
                    return true;
                }
                return false;
            }
            if (JSSymbol.isJSSymbol(x) && JSSymbol.isJSSymbol(x)) {
                if (JSSymbol.getSymbolData((DynamicObject) x) == JSSymbol.getSymbolData((DynamicObject) y)) {
                    return true;
                }
                return false;
            }
            if (JSSIMD.isJSSIMD(x) && JSSIMD.isJSSIMD(y)) {
                for (int i = 0; i < JSSIMD.simdTypeGetSIMDType((DynamicObject) x).getFactory().numberOfElements(); i++) {
                    if (!sameValueZero(simdExtractLane((DynamicObject) x, i), simdExtractLane((DynamicObject) y, i))) {
                        return false;
                    }
                }
                return true;
            }
            if (x == y) {
                return true;
            }
            return false;
        }

        // 5.1.1 SIMDCreate( descriptor, vectorElements )
        @ExplodeLoop
        public DynamicObject simdCreate(SIMDType descriptor, List<Object> vectorElements) {
            assert (vectorElements.size() == descriptor.getFactory().numberOfElements());
            assert vectorElements.size() == numberOfElements;

            DynamicObject t = JSSIMD.createSIMD(getContext(), descriptor);
            for (int i = 0; i < numberOfElements; i++) {
                setLane(t, i, cast(i, Boundaries.listGet(vectorElements, i)));
            }

            return t;
        }

        // 5.1.2 SIMDToLane( max, lane )
        public int simdToLane(int max, Object lane) {
            /*
             * Object index = toNumber(frame, lane); int in = toInt32(frame, index);
             */
            Number index = getToNumberNode().executeNumber(lane);
            int in = JSRuntime.toInt32(index);
            if ((!sameValueZero(index, getToLengthNode().executeLong(index))) || in < 0 || in >= max) {
                errorBranch.enter();
                throw Errors.createRangeError("lane out of bounds!");
            }
            return in;
        }

        // 5.1.3 SIMDExtractLane( value, lane )
        public Object simdExtractLane(DynamicObject value, Object lane) {
            assert JSSIMD.isJSSIMD(value);
            int index = simdToLane(JSSIMD.simdTypeGetSIMDType(value).getFactory().numberOfElements(), lane);
            Object res = getLane(value, index);
            return res;
        }

        // 5.1.3 SIMDExtractLane( value, lane )
        public Object simdExtractLane(DynamicObject value, int lane) {
            assert JSSIMD.isJSSIMD(value);
            Object res = getLane(value, lane);
            return res;
        }

        // 5.1.4 SIMDReplaceLane( value, lane, replacement )
        public DynamicObject simdReplaceLane(DynamicObject value, Object lane, Object replacement) {
            SIMDType descriptor = JSSIMD.simdTypeGetSIMDType(value);

            DynamicObject res = JSSIMD.createSIMD(getContext(), descriptor);
            int index = simdToLane(JSSIMD.simdTypeGetSIMDType(value).getNumberOfElements(), lane);
            for (int i = 0; i < descriptor.getNumberOfElements(); i++) {
                setLane(res, i, getLane(value, i));
            }

            setLane(res, index, cast(0, replacement));
            return res;
        }

        // 5.1.5 MaybeFlushDenormal( n, descriptor )
        public Object maybeFlushDenormal(Object n, @SuppressWarnings("unused") SIMDType descriptor) {
            return n;
        }

        // SIMDLoad( dataBlock, descriptor, byteOffset [, length] )
        protected Object simdLoad(byte[] dataBlock, SIMDType descriptor, int byteOffset, int length) {
            if (byteOffset < 0 || byteOffset > dataBlock.length - descriptor.getBytesPerElement() * length) {
                errorBranch.enter();
                throw Errors.createError("assertion");
            }
            List<Object> elements = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                Boundaries.listAdd(elements, descriptor.deserialize(dataBlock, byteOffset + i * descriptor.getBytesPerElement()));
            }
            for (int i = length; i < descriptor.getFactory().numberOfElements(); i++) {
                Boundaries.listAdd(elements, 0);
            }
            return simdCreate(descriptor, elements);
        }

        protected Object simdLoadFromTypedArray(DynamicObject tarray, Object index, SIMDType descriptor) {
            return simdLoadFromTypedArray(tarray, index, descriptor, descriptor.getFactory().numberOfElements());
        }

        // SIMDLoadFromTypedArray( tarray, index, descriptor [, length] )
        protected Object simdLoadFromTypedArray(DynamicObject tarray, Object index, SIMDType descriptor, int length) {

            if (!JSArrayBufferView.isJSArrayBufferView(tarray)) {
                errorBranch.enter();
                throw Errors.createTypeErrorArrayBufferViewExpected();
            }
            DynamicObject arrayBuffer = JSArrayBufferView.getArrayBuffer(tarray);
            if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
                errorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
            }
            byte[] block;
            final boolean isHeapArrayBuffer = JSArrayBuffer.isJSHeapArrayBuffer(arrayBuffer);
            if (isHeapArrayBuffer) {
                block = JSArrayBuffer.getByteArray(arrayBuffer);
            } else {
                assert JSArrayBuffer.isJSDirectArrayBuffer(arrayBuffer) : JSObject.getJSClass(arrayBuffer);
                ByteBuffer byteBuffer = JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
                block = new byte[JSArrayBuffer.getDirectByteLength(arrayBuffer)];
                ((ByteBuffer) byteBuffer.duplicate().clear()).get(block);
            }

            long indx = getToLengthNode().executeLong(index);
            if (!(indx == 0 && index == Null.instance) && !getEqualNode().executeBoolean(index, indx)) {
                errorBranch.enter();
                throw Errors.createRangeError("index");
            }
            int byteLength = JSArrayBufferView.getByteLength(tarray, true, getContext(), typedArrayProfile);
            int elementlength = byteLength / JSArrayBufferView.typedArrayGetLength(tarray);
            long byteindex = indx * elementlength;

            if (byteindex + simdContext.getFactory().bytesPerElement() * length > byteLength || byteindex < 0) {
                errorBranch.enter();
                throw Errors.createRangeError("");
            }

            return simdLoad(block, descriptor, (int) byteindex, length);
        }

        // SIMDStore( dataBlock, descriptor, byteOffset, n [, length] )
        protected void simdStore(byte[] dataBlock, SIMDType descriptor, int byteOffset, DynamicObject n, int length) {
            if (byteOffset < 0 || byteOffset > dataBlock.length - descriptor.getBytesPerElement() * length) {
                errorBranch.enter();
                throw Errors.createError("assertion");
            }
            for (int i = 0; i < length; i++) {
                descriptor.serialize(dataBlock, byteOffset + i * descriptor.getBytesPerElement(), getLane(n, i));
            }
        }

        // SIMDStoreInTypedArray( tarray, index, descriptor, n [, length] )
        protected Object simdStoreInTypedArray(DynamicObject tarray, Object index, SIMDType descriptor, DynamicObject n) {
            int length = descriptor.getNumberOfElements();
            return simdStoreInTypedArray(tarray, index, descriptor, n, length);
        }

        protected Object simdStoreInTypedArray(DynamicObject tarray, Object index, @SuppressWarnings("unused") SIMDType descriptor, DynamicObject n, int length) {
            if (!JSSIMD.isJSSIMD(n)) {
                errorBranch.enter();
                throw Errors.createTypeError("invalid type");
            }

            if (!JSArrayBufferView.isJSArrayBufferView(tarray)) {
                errorBranch.enter();
                throw Errors.createTypeErrorArrayBufferViewExpected();
            }
            DynamicObject arrayBuffer = JSArrayBufferView.getArrayBuffer(tarray);
            if (arrayBuffer == null || arrayBuffer == Undefined.instance) {
                errorBranch.enter();
                throw Errors.createTypeError("TypedArray was null");
            }
            if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBuffer)) {
                errorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
            }
            if (!JSArrayBufferView.isJSArrayBufferView(tarray)) {
                errorBranch.enter();
                throw Errors.createTypeError("not typed array");
            }

            byte[] block;
            final boolean isHeapArrayBuffer = JSArrayBuffer.isJSHeapArrayBuffer(arrayBuffer);
            if (isHeapArrayBuffer) {
                block = JSArrayBuffer.getByteArray(arrayBuffer);
            } else {
                assert JSArrayBuffer.isJSDirectArrayBuffer(arrayBuffer) : JSObject.getJSClass(arrayBuffer);
                ByteBuffer byteBuffer = JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
                block = new byte[JSArrayBuffer.getDirectByteLength(arrayBuffer)];
                ((ByteBuffer) byteBuffer.duplicate().clear()).get(block);
            }

            long indx = getToLengthNode().executeLong(index);
            if (!(indx == 0 && index == Null.instance) && !getEqualNode().executeBoolean(indx, index)) {
                errorBranch.enter();
                throw Errors.createRangeError("invalid index");
            }

            int byteLength = JSArrayBufferView.getByteLength(tarray, true, getContext(), typedArrayProfile);
            int elementlength = byteLength / JSArrayBufferView.typedArrayGetLength(tarray);
            long byteindex = indx * elementlength;

            if (byteindex + simdContext.getFactory().bytesPerElement() * length > byteLength || byteindex < 0) {
                errorBranch.enter();
                throw Errors.createRangeError("");
            }

            simdStore(block, simdContext, (int) byteindex, n, length);
            if (isHeapArrayBuffer) {
                JSArrayBufferView.typedArraySetArray(tarray, block);
            } else {
                ByteBuffer byteBuffer = JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
                ((ByteBuffer) byteBuffer.duplicate().clear()).put(block);
            }
            return n;
        }

        // SIMDReinterpretCast( value, newDescriptor )
        protected Object simdReinterpretCast(Object value, SIMDType newDescriptor) {
            int bytes = newDescriptor.getBytesPerElement() * newDescriptor.getNumberOfElements();
            if (simdContext.getBytesPerElement() * simdContext.getNumberOfElements() != bytes) {
                errorBranch.enter();
                throw Errors.createError("assertion");
            }
            byte[] block = new byte[bytes];
            SIMDType olddesc = JSSIMD.simdTypeGetSIMDType((DynamicObject) value);
            simdStore(block, olddesc, 0, (DynamicObject) value, olddesc.getNumberOfElements());
            return simdLoad(block, newDescriptor, 0, newDescriptor.getFactory().numberOfElements());
        }

        // 5.1.14 SIMDBoolType( descriptor )
        protected SIMDType simdBoolType(SIMDType descriptor) {
            if (descriptor.getFactory().bytesPerElement() * 8 * descriptor.getFactory().numberOfElements() != 128) {
                errorBranch.enter();
                throw Errors.createError("not 128 bits");
            }
            switch (descriptor.getFactory().numberOfElements()) {
                case 4:
                    return SIMDType.BOOL32X4_FACTORY.createSimdType();
                case 8:
                    return SIMDType.BOOL16X8_FACTORY.createSimdType();
                case 16:
                    return SIMDType.BOOL8X16_FACTORY.createSimdType();
            }
            errorBranch.enter();
            throw Errors.createError("should not reach here");
        }

        protected boolean isUnsigned(SIMDType t) {
            return SIMDType.SIMDTypedUInt.class.isAssignableFrom(t.getClass());
        }

        protected boolean isIntegerSIMD(SIMDType t) {
            return SIMDType.SIMDTypeInt.class.isAssignableFrom(t.getClass());
        }

        protected boolean isFloatSIMD(SIMDType t) {
            return SIMDType.SIMDTypedFloat.class.isAssignableFrom(t.getClass());
        }

        // ReciprocalApproximation(n)
        protected float reciprocalApproximation(float n) {
            if (Float.isNaN(n)) {
                return Float.NaN;
            }
            if (new Float(n).equals(new Float(+0.0))) {
                return Float.POSITIVE_INFINITY;
            }
            if (new Float(n).equals(new Float(-0.0))) {
                return Float.NEGATIVE_INFINITY;
            }
            if (n == Float.POSITIVE_INFINITY) {
                return 0;
            }
            if (n == Float.NEGATIVE_INFINITY) {
                return 0;
            }
            return (float) (1.0 / n);
        }

        // ReciprocalSqrtApproximation(n)
        protected float reciprocalSqrtApproximation(float n) {
            if (Float.isNaN(n)) {
                return Float.NaN;
            }
            if (new Float(n).equals(new Float(+0.0))) {
                return Float.POSITIVE_INFINITY;
            }
            if (new Float(n).equals(new Float(-0.0))) {
                return Float.NEGATIVE_INFINITY;
            }
            if (n == Float.POSITIVE_INFINITY) {
                return 0;
            }
            if (n < 0) {
                return Float.NaN;
            }
            return (float) (1.0 / Math.sqrt(n));
        }

        // Saturate( descriptor, x )
        protected long saturate(SIMDTypeInt descriptor, long x) {
            if (x > descriptor.getMax()) {
                return descriptor.getMax();
            }
            if (x < descriptor.getMin()) {
                return descriptor.getMin();
            }
            return (int) x;
        }
    }

    public abstract static class SIMDSplatNode extends JSBasicSimdOperation {
        protected SIMDSplatNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static Object create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDSplatNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @ExplodeLoop
        @Specialization
        protected Object executeSplat(Object n) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);

            Object val = cast(0, n);
            for (int i = 0; i < numberOfElements; i++) {
                setLane(res, i, val);
            }
            return res;
        }
    }

    public abstract static class SIMDCheckNode extends JSBasicSimdOperation {

        protected SIMDCheckNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDCheckNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDCheckNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doCheck(Object a) {
            // SIMDType descriptor = null;
            if (!JSSIMD.isJSSIMD(a) || JSSIMD.simdTypeGetSIMDType((DynamicObject) a) != simdContext) {
                errorBranch.enter();
                throw Errors.createTypeError("Parameter is not a SIMD Object");
            }
            return a;
        }
    }

    public abstract static class SIMDAddNode extends JSBasicSimdOperation {

        protected SIMDAddNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDAddNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDAddNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doAdd(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDUint32x4.class)) {
                doIntAdd(a, b, res);
            } else if (simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDUint16x8.class)) {
                doShortAdd(a, b, res);
            } else if (simdElementType.equals(SIMDInt8x16.class) || simdElementType.equals(SIMDUint8x16.class)) {
                doByteAdd(a, b, res);
            } else if (simdElementType.equals(SIMDFloat32x4.class)) {
                doFloatAdd(a, b, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntAdd(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, ax + bx);
            }
        }

        @ExplodeLoop
        private void doShortAdd(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(rest, i, (int) (short) (ax + bx));
            }
        }

        @ExplodeLoop
        private void doByteAdd(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(rest, i, (int) (byte) (ax + bx));
            }
        }

        @ExplodeLoop
        private void doFloatAdd(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);
                setLane(rest, i, ax + bx);
            }
        }

    }

    public abstract static class SIMDSubNode extends JSBasicSimdOperation {

        protected SIMDSubNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDSubNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDSubNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doSub(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDUint32x4.class)) {
                doIntSub(a, b, res);
            } else if (simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDUint16x8.class)) {
                doShortSub(a, b, res);
            } else if (simdElementType.equals(SIMDInt8x16.class) || simdElementType.equals(SIMDUint8x16.class)) {
                doByteSub(a, b, res);
            } else if (simdElementType.equals(SIMDFloat32x4.class)) {
                doFloatSub(a, b, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntSub(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, ax - bx);
            }
        }

        @ExplodeLoop
        private void doShortSub(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(rest, i, (int) (short) (ax - bx));
            }
        }

        @ExplodeLoop
        private void doByteSub(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(rest, i, (int) (byte) (ax - bx));
            }
        }

        @ExplodeLoop
        private void doFloatSub(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);
                setLane(rest, i, ax - bx);
            }
        }
    }

    public abstract static class SIMDMulNode extends JSBasicSimdOperation {

        protected SIMDMulNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDMulNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDMulNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doMul(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDUint32x4.class)) {
                doIntMul(a, b, res);
            } else if (simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDUint16x8.class)) {
                doShortMul(a, b, res);
            } else if (simdElementType.equals(SIMDInt8x16.class) || simdElementType.equals(SIMDUint8x16.class)) {
                doByteMul(a, b, res);
            } else if (simdElementType.equals(SIMDFloat32x4.class)) {
                doFloatMul(a, b, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntMul(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, ax * bx);
            }
        }

        @ExplodeLoop
        private void doShortMul(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(rest, i, (int) (short) (ax * bx));
            }
        }

        @ExplodeLoop
        private void doByteMul(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(rest, i, (int) (byte) (ax * bx));
            }
        }

        @ExplodeLoop
        private void doFloatMul(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);
                setLane(rest, i, ax * bx);
            }
        }
    }

    public abstract static class SIMDDivNode extends JSBasicSimdOperation {

        protected SIMDDivNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDDivNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDDivNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doDiv(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);
                setLane(res, i, ax / bx);
            }
            return res;
        }

    }

    public abstract static class SIMDMinNode extends JSBasicSimdOperation {

        protected SIMDMinNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDMinNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDMinNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doMin(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);
                setLane(res, i, Math.min(ax, bx));
            }
            return res;
        }
    }

    public abstract static class SIMDMaxNode extends JSBasicSimdOperation {

        protected SIMDMaxNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDMaxNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDMaxNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doMax(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);
                setLane(res, i, Math.max(ax, bx));
            }
            return res;
        }
    }

    public abstract static class SIMDMinNumNode extends JSBasicSimdOperation {

        protected SIMDMinNumNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDMinNumNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDMinNumNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doMinNum(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);

                if (Float.isNaN(ax)) {
                    setLane(res, i, bx);
                } else if (Float.isNaN(bx)) {
                    setLane(res, i, ax);
                } else {
                    setLane(res, i, Math.min(ax, bx));
                }
            }
            return res;
        }
    }

    public abstract static class SIMDMaxNumNode extends JSBasicSimdOperation {

        protected SIMDMaxNumNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDMaxNumNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDMaxNumNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doMaxNum(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);

                if (Float.isNaN(ax)) {
                    setLane(res, i, bx);
                } else if (Float.isNaN(bx)) {
                    setLane(res, i, ax);
                } else {
                    setLane(res, i, Math.max(ax, bx));
                }
            }
            return res;
        }
    }

    public abstract static class SIMDNegNode extends JSBasicSimdOperation {

        protected SIMDNegNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDNegNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDNegNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doNeg(DynamicObject a) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDUint32x4.class)) {
                doIntNeg(a, res);
            } else if (simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDUint16x8.class)) {
                doShortNeg(a, res);
            } else if (simdElementType.equals(SIMDInt8x16.class) || simdElementType.equals(SIMDUint8x16.class)) {
                doByteNeg(a, res);
            } else if (simdElementType.equals(SIMDFloat32x4.class)) {
                doFloatNeg(a, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntNeg(DynamicObject a, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(res, i, -ax);
            }
        }

        @ExplodeLoop
        private void doShortNeg(DynamicObject a, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(rest, i, (int) (short) (-ax));
            }
        }

        @ExplodeLoop
        private void doByteNeg(DynamicObject a, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(rest, i, (int) (byte) (-ax));
            }
        }

        @ExplodeLoop
        private void doFloatNeg(DynamicObject a, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                setLane(rest, i, -ax);
            }
        }
    }

    public abstract static class SIMDSqrtNode extends JSBasicSimdOperation {

        protected SIMDSqrtNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDSqrtNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDSqrtNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doSqrt(DynamicObject a) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                setLane(res, i, (float) Math.sqrt(ax));
            }
            return res;
        }
    }

    public abstract static class SIMDreciprocalApproximationNode extends JSBasicSimdOperation {

        protected SIMDreciprocalApproximationNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDreciprocalApproximationNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDreciprocalApproximationNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doReciApprox(DynamicObject a) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                setLane(res, i, reciprocalApproximation(ax));
            }
            return res;
        }
    }

    public abstract static class SIMDreciprocalSqrtApproximationNode extends JSBasicSimdOperation {

        protected SIMDreciprocalSqrtApproximationNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDreciprocalSqrtApproximationNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDreciprocalSqrtApproximationNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doReciSqrtApprox(DynamicObject a) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                setLane(res, i, reciprocalSqrtApproximation(ax));
            }
            return res;
        }
    }

    public abstract static class SIMDAbsNode extends JSBasicSimdOperation {

        protected SIMDAbsNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDAbsNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDAbsNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doAbs(DynamicObject a) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                setLane(res, i, Math.abs(ax));
            }
            return res;
        }
    }

    public abstract static class SIMDAndNode extends JSBasicSimdOperation {

        protected SIMDAndNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDAndNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDAndNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doAnd(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDUint32x4.class)) {
                doIntAnd(a, b, res);
            } else if (simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDUint16x8.class)) {
                doShortAnd(a, b, res);
            } else if (simdElementType.equals(SIMDInt8x16.class) || simdElementType.equals(SIMDUint8x16.class)) {
                doByteAnd(a, b, res);
            } else if (simdElementType.equals(SIMDBool32x4.class) || simdElementType.equals(SIMDBool16x8.class) || simdElementType.equals(SIMDBool8x16.class)) {
                doBooleanAnd(a, b, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntAnd(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, ax & bx);
            }
        }

        @ExplodeLoop
        private void doShortAnd(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(rest, i, (int) (short) (ax & bx));
            }
        }

        @ExplodeLoop
        private void doByteAnd(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(rest, i, (int) (byte) (ax & bx));
            }
        }

        @ExplodeLoop
        private void doBooleanAnd(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                boolean ax = (boolean) getLane(a, i);
                boolean bx = (boolean) getLane(b, i);
                setLane(rest, i, (ax & bx));
            }
        }
    }

    public abstract static class SIMDXorNode extends JSBasicSimdOperation {

        protected SIMDXorNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDXorNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDXorNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doXor(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDUint32x4.class)) {
                doIntXor(a, b, res);
            } else if (simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDUint16x8.class)) {
                doShortXor(a, b, res);
            } else if (simdElementType.equals(SIMDInt8x16.class) || simdElementType.equals(SIMDUint8x16.class)) {
                doByteXor(a, b, res);
            } else if (simdElementType.equals(SIMDBool32x4.class) || simdElementType.equals(SIMDBool16x8.class) || simdElementType.equals(SIMDBool8x16.class)) {
                doBooleanXor(a, b, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntXor(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, ax ^ bx);
            }
        }

        @ExplodeLoop
        private void doShortXor(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(rest, i, (int) (short) (ax ^ bx));
            }
        }

        @ExplodeLoop
        private void doByteXor(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(rest, i, (int) (byte) (ax ^ bx));
            }
        }

        @ExplodeLoop
        private void doBooleanXor(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                boolean ax = (boolean) getLane(a, i);
                boolean bx = (boolean) getLane(b, i);
                setLane(rest, i, (ax ^ bx));
            }
        }
    }

    public abstract static class SIMDOrNode extends JSBasicSimdOperation {

        protected SIMDOrNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDOrNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDOrNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doOr(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDUint32x4.class)) {
                doIntOr(a, b, res);
            } else if (simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDUint16x8.class)) {
                doShortOr(a, b, res);
            } else if (simdElementType.equals(SIMDInt8x16.class) || simdElementType.equals(SIMDUint8x16.class)) {
                doByteOr(a, b, res);
            } else if (simdElementType.equals(SIMDBool32x4.class) || simdElementType.equals(SIMDBool16x8.class) || simdElementType.equals(SIMDBool8x16.class)) {
                doBooleanOr(a, b, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntOr(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, ax | bx);
            }
        }

        @ExplodeLoop
        private void doShortOr(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(rest, i, (int) (short) (ax | bx));
            }
        }

        @ExplodeLoop
        private void doByteOr(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(rest, i, (int) (byte) (ax | bx));
            }
        }

        @ExplodeLoop
        private void doBooleanOr(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                boolean ax = (boolean) getLane(a, i);
                boolean bx = (boolean) getLane(b, i);
                setLane(rest, i, (ax | bx));
            }
        }
    }

    public abstract static class SIMDNotNode extends JSBasicSimdOperation {

        protected SIMDNotNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDNotNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDNotNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doNot(DynamicObject a) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDUint32x4.class)) {
                doIntNot(a, res);
            } else if (simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDUint16x8.class)) {
                doShortNot(a, res);
            } else if (simdElementType.equals(SIMDInt8x16.class) || simdElementType.equals(SIMDUint8x16.class)) {
                doByteNot(a, res);
            } else if (simdElementType.equals(SIMDBool32x4.class) || simdElementType.equals(SIMDBool16x8.class) || simdElementType.equals(SIMDBool8x16.class)) {
                doBooleanNot(a, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntNot(DynamicObject a, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(res, i, ~ax);
            }
        }

        @ExplodeLoop
        private void doShortNot(DynamicObject a, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(rest, i, (int) (short) (~ax));
            }
        }

        @ExplodeLoop
        private void doByteNot(DynamicObject a, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(rest, i, (int) (byte) (~ax));
            }
        }

        @ExplodeLoop
        private void doBooleanNot(DynamicObject a, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                boolean ax = (boolean) getLane(a, i);
                setLane(rest, i, (!ax));
            }
        }
    }

    public abstract static class SIMDLessThanNode extends JSBasicSimdOperation {

        protected SIMDLessThanNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDLessThanNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDLessThanNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doLessThan(DynamicObject a, DynamicObject b) {
            SIMDType descriptor = simdBoolType(simdContext);
            DynamicObject res = JSSIMD.createSIMD(getContext(), descriptor);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDInt8x16.class)) {
                doIntLessThan(a, b, res);
            } else if (simdElementType.equals(SIMDUint32x4.class)) {
                doUIntLessThan(a, b, res);
            } else if (simdElementType.equals(SIMDUint16x8.class)) {
                doIntLessThan(a, b, res);
            } else if (simdElementType.equals(SIMDUint8x16.class)) {
                doIntLessThan(a, b, res);
            } else if (simdElementType.equals(SIMDFloat32x4.class)) {
                doFloatLessThan(a, b, res);
            } else {
                System.out.println("TypeNotFound");
            }
            return res;
        }

        @ExplodeLoop
        private void doUIntLessThan(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                long ax = Integer.toUnsignedLong((int) getLane(a, i));
                long bx = Integer.toUnsignedLong((int) getLane(b, i));
                setLane(res, i, ax < bx);
            }
        }

        @ExplodeLoop
        private void doIntLessThan(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, ax < bx);
            }
        }

        @ExplodeLoop
        private void doFloatLessThan(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);
                setLane(rest, i, ax < bx);
            }
        }
    }

    public abstract static class SIMDLessThanOrEqualNode extends JSBasicSimdOperation {

        protected SIMDLessThanOrEqualNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDLessThanOrEqualNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDLessThanOrEqualNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doLessThanOrEqual(DynamicObject a, DynamicObject b) {
            SIMDType descriptor = simdBoolType(simdContext);
            DynamicObject res = JSSIMD.createSIMD(getContext(), descriptor);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDInt8x16.class)) {
                doIntLessThanOrEqual(a, b, res);
            } else if (simdElementType.equals(SIMDUint32x4.class) || simdElementType.equals(SIMDUint16x8.class) || simdElementType.equals(SIMDUint8x16.class)) {
                doUIntLessThanOrEqual(a, b, res);
            } else if (simdElementType.equals(SIMDFloat32x4.class)) {
                doFloatLessThanOrEqual(a, b, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doUIntLessThanOrEqual(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                long ax = Integer.toUnsignedLong((int) getLane(a, i));
                long bx = Integer.toUnsignedLong((int) getLane(b, i));
                setLane(res, i, ax <= bx);
            }
        }

        @ExplodeLoop
        private void doIntLessThanOrEqual(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, ax <= bx);
            }
        }

        @ExplodeLoop
        private void doFloatLessThanOrEqual(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);
                setLane(rest, i, ax <= bx);
            }
        }
    }

    public abstract static class SIMDGreaterThanNode extends JSBasicSimdOperation {

        protected SIMDGreaterThanNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDGreaterThanNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDGreaterThanNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doGreaterThan(DynamicObject a, DynamicObject b) {
            SIMDType descriptor = simdBoolType(simdContext);
            DynamicObject res = JSSIMD.createSIMD(getContext(), descriptor);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDInt8x16.class)) {
                doIntGreaterThan(a, b, res);
            } else if (simdElementType.equals(SIMDUint32x4.class) || simdElementType.equals(SIMDUint16x8.class) || simdElementType.equals(SIMDUint8x16.class)) {
                doUIntGreaterThan(a, b, res);
            } else if (simdElementType.equals(SIMDFloat32x4.class)) {
                doFloatGreaterThan(a, b, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doUIntGreaterThan(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                long ax = Integer.toUnsignedLong((int) getLane(a, i));
                long bx = Integer.toUnsignedLong((int) getLane(b, i));
                setLane(res, i, ax > bx);
            }
        }

        @ExplodeLoop
        private void doIntGreaterThan(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, ax > bx);
            }
        }

        @ExplodeLoop
        private void doFloatGreaterThan(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);
                setLane(rest, i, ax > bx);
            }
        }
    }

    public abstract static class SIMDGreaterThanOrEqualNode extends JSBasicSimdOperation {

        protected SIMDGreaterThanOrEqualNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDGreaterThanOrEqualNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDGreaterThanOrEqualNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doGreaterThanOrEqual(DynamicObject a, DynamicObject b) {
            SIMDType descriptor = simdBoolType(simdContext);
            DynamicObject res = JSSIMD.createSIMD(getContext(), descriptor);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDInt8x16.class)) {
                doIntGreaterThanOrEqual(a, b, res);
            } else if (simdElementType.equals(SIMDUint32x4.class) || simdElementType.equals(SIMDUint16x8.class) || simdElementType.equals(SIMDUint8x16.class)) {
                doUIntGreaterThanOrEqual(a, b, res);
            } else if (simdElementType.equals(SIMDFloat32x4.class)) {
                doFloatGreaterThanOrEqual(a, b, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doUIntGreaterThanOrEqual(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                long ax = Integer.toUnsignedLong((int) getLane(a, i));
                long bx = Integer.toUnsignedLong((int) getLane(b, i));
                setLane(res, i, ax >= bx);
            }
        }

        @ExplodeLoop
        private void doIntGreaterThanOrEqual(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, ax >= bx);
            }
        }

        @ExplodeLoop
        private void doFloatGreaterThanOrEqual(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);
                setLane(rest, i, ax >= bx);
            }
        }
    }

    public abstract static class SIMDNotEqualNode extends JSBasicSimdOperation {

        protected SIMDNotEqualNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDNotEqualNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDNotEqualNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doNotEqual(DynamicObject a, DynamicObject b) {
            SIMDType descriptor = simdBoolType(simdContext);
            DynamicObject res = JSSIMD.createSIMD(getContext(), descriptor);
            if (simdElementType.equals(SIMDFloat32x4.class)) {
                doFloatNotEqual(a, b, res);
            } else {
                doIntNotEqual(a, b, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntNotEqual(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, ax != bx);
            }
        }

        @ExplodeLoop
        private void doFloatNotEqual(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);
                setLane(rest, i, ax != bx);
            }
        }
    }

    public abstract static class SIMDEqualNode extends JSBasicSimdOperation {

        protected SIMDEqualNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDEqualNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDEqualNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doEqual(DynamicObject a, DynamicObject b) {
            SIMDType descriptor = simdBoolType(simdContext);
            DynamicObject res = JSSIMD.createSIMD(getContext(), descriptor);
            if (simdElementType.equals(SIMDFloat32x4.class)) {
                doFloatEqual(a, b, res);
            } else {
                doIntEqual(a, b, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntEqual(DynamicObject a, DynamicObject b, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, ax == bx);
            }
        }

        @ExplodeLoop
        private void doFloatEqual(DynamicObject a, DynamicObject b, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                float ax = (float) getLane(a, i);
                float bx = (float) getLane(b, i);
                setLane(rest, i, ax == bx);
            }
        }
    }

    public abstract static class SIMDAnyTrueNode extends JSBasicSimdOperation {

        protected SIMDAnyTrueNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDAnyTrueNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDAnyTrueNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doAnyTrue(DynamicObject a) {
            for (int i = 0; i < numberOfElements; i++) {
                if ((boolean) getLane(a, i) == true) {
                    return true;
                }
            }

            return false;
        }
    }

    public abstract static class SIMDAllTrueNode extends JSBasicSimdOperation {

        protected SIMDAllTrueNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDAllTrueNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDAllTrueNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doAllTrue(DynamicObject a) {
            for (int i = 0; i < numberOfElements; i++) {
                if ((boolean) getLane(a, i) == false) {
                    return false;
                }
            }

            return true;
        }
    }

    public abstract static class SIMDSelectNode extends JSBasicSimdOperation {

        protected SIMDSelectNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDSelectNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDSelectNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doSelect(DynamicObject selector, DynamicObject a, DynamicObject b) {
            if (!JSSIMD.isJSSIMD(a) || !JSSIMD.isJSSIMD(b)) {
                errorBranch.enter();
                throw Errors.createTypeError("invalid argument Type");
            }
            SIMDType selDescriptor = simdBoolType(simdContext);
            if (selDescriptor != JSSIMD.simdTypeGetSIMDType(selector)) {
                errorBranch.enter();
                throw Errors.createTypeError("invalid argument Type");
            }

            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);

            for (int i = 0; i < numberOfElements; i++) {
                if ((boolean) getLane(selector, i) == true) {
                    setLane(res, i, getLane(a, i));
                } else {
                    setLane(res, i, getLane(b, i));
                }
            }

            return res;
        }
    }

    public abstract static class SIMDAddSaturateNode extends JSBasicSimdOperation {

        protected SIMDAddSaturateNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static Object create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDAddSaturateNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doAddSaturate(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);

            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, (int) saturate((SIMDTypeInt) simdContext, ax + bx));
            }

            return res;
        }
    }

    public abstract static class SIMDSubSaturateNode extends JSBasicSimdOperation {

        protected SIMDSubSaturateNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static Object create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDSubSaturateNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doSubSaturate(DynamicObject a, DynamicObject b) {
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);

            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                int bx = (int) getLane(b, i);
                setLane(res, i, (int) saturate((SIMDTypeInt) simdContext, ax - bx));
            }

            return res;
        }
    }

    public abstract static class SIMDShiftLeftByScalarNode extends JSBasicSimdOperation {

        protected SIMDShiftLeftByScalarNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDShiftLeftByScalarNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDShiftLeftByScalarNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doShiftLeft(DynamicObject a, Object bits) {
            long scalar = getToUInt32Node().executeLong(bits);
            long shiftCount = scalar % (simdContext.getFactory().bytesPerElement() * 8);
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            if (simdElementType.equals(SIMDInt32x4.class) || simdElementType.equals(SIMDUint32x4.class)) {
                doIntShiftLeft(a, shiftCount, res);
            } else if (simdElementType.equals(SIMDInt16x8.class) || simdElementType.equals(SIMDUint16x8.class)) {
                doShortShiftLeft(a, shiftCount, res);
            } else if (simdElementType.equals(SIMDInt8x16.class) || simdElementType.equals(SIMDUint8x16.class)) {
                doByteShiftLeft(a, shiftCount, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntShiftLeft(DynamicObject a, long shiftCount, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(res, i, ax << shiftCount);
            }
        }

        @ExplodeLoop
        private void doShortShiftLeft(DynamicObject a, long shiftCount, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(rest, i, (int) (short) (ax << shiftCount));
            }
        }

        @ExplodeLoop
        private void doByteShiftLeft(DynamicObject a, long shiftCount, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(rest, i, (int) (byte) (ax << shiftCount));
            }
        }
    }

    public abstract static class SIMDShiftRightByScalarNode extends JSBasicSimdOperation {

        protected SIMDShiftRightByScalarNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static Object create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDShiftRightByScalarNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        protected Object doShiftRight(DynamicObject a, Object bits) {
            long scalar = getToUInt32Node().executeLong(bits);
            long shiftCount = scalar % (simdContext.getFactory().bytesPerElement() * 8);
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            if (simdElementType.equals(SIMDInt32x4.class)) {
                doIntShiftRight(a, shiftCount, res);
            } else if (simdElementType.equals(SIMDInt16x8.class)) {
                doShortShiftRight(a, shiftCount, res);
            } else if (simdElementType.equals(SIMDInt8x16.class)) {
                doByteShiftRight(a, shiftCount, res);
            } else if (simdElementType.equals(SIMDUint32x4.class)) {
                doUIntShiftRight(a, shiftCount, res);
            } else if (simdElementType.equals(SIMDUint16x8.class)) {
                doUShortShiftRight(a, shiftCount, res);
            } else if (simdElementType.equals(SIMDUint8x16.class)) {
                doUByteShiftRight(a, shiftCount, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntShiftRight(DynamicObject a, long shiftCount, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(res, i, ax >> shiftCount);
            }
        }

        @ExplodeLoop
        private void doShortShiftRight(DynamicObject a, long shiftCount, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(rest, i, (int) (short) (ax >> shiftCount));
            }
        }

        @ExplodeLoop
        private void doByteShiftRight(DynamicObject a, long shiftCount, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(rest, i, (int) (byte) (ax >> shiftCount));
            }
        }

        @ExplodeLoop
        private void doUIntShiftRight(DynamicObject a, long shiftCount, DynamicObject res) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(res, i, ax >>> shiftCount);
            }
        }

        @ExplodeLoop
        private void doUShortShiftRight(DynamicObject a, long shiftCount, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(rest, i, (int) (short) (ax >>> shiftCount));
            }
        }

        @ExplodeLoop
        private void doUByteShiftRight(DynamicObject a, long shiftCount, DynamicObject rest) {
            for (int i = 0; i < numberOfElements; i++) {
                int ax = (int) getLane(a, i);
                setLane(rest, i, (int) (byte) (ax >>> shiftCount));
            }
        }
    }

    public abstract static class SIMDExtractLaneNode extends JSBasicSimdOperation {

        protected SIMDExtractLaneNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDExtractLaneNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDExtractLaneNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization(guards = {"lane < simdContext.getNumberOfElements()", "lane >= 0"})
        protected Object doExtract(DynamicObject a, int lane) {
            if (simdElementType.equals(SIMDUint32x4.class)) {
                return doUIntExtract(a, lane);
            } else if (simdElementType.equals(SIMDUint16x8.class)) {
                return doUShortExtract(a, lane);
            } else if (simdElementType.equals(SIMDUint8x16.class)) {
                return doUByteExtract(a, lane);
            } else if (simdElementType.equals(SIMDFloat32x4.class)) {
                return doFloatExtract(a, lane);
            } else {
                return doGeneralExtract(a, lane);
            }
        }

        @Specialization
        protected Object doExtract(DynamicObject a, Object lane) {
            int index = simdToLane(simdContext.getNumberOfElements(), lane);
            return doExtract(a, index);
        }

        private static Object doUIntExtract(DynamicObject a, int lane) {
            return JSRuntime.toUInt32((int) getLane(a, lane));
        }

        private static Object doUShortExtract(DynamicObject a, int lane) {
            return JSRuntime.toUInt16((int) getLane(a, lane));
        }

        private static Object doUByteExtract(DynamicObject a, int lane) {
            return JSRuntime.toUInt8((int) getLane(a, lane));
        }

        private static Object doFloatExtract(DynamicObject a, int lane) {
            Object number = getLane(a, lane);
            return JSRuntime.doubleValueVirtual((Number) number);
        }

        private static Object doGeneralExtract(DynamicObject a, int lane) {
            return getLane(a, lane);
        }
    }

    public abstract static class SIMDReplaceLaneNode extends JSBasicSimdOperation {

        protected SIMDReplaceLaneNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDReplaceLaneNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDReplaceLaneNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doReplaceLane(DynamicObject a, Object lane, Object replacement) {
            int index = simdToLane(numberOfElements, lane);
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            for (int i = 0; i < numberOfElements; i++) {
                setLane(res, i, getLane(a, i));
            }

            setLane(res, index, cast(0, replacement));
            return res;
        }
    }

    public abstract static class SIMDStoreNode extends JSBasicSimdOperation {

        protected final int length;

        protected SIMDStoreNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
            this.length = simdContext.getNumberOfElements();
        }

        public static SIMDStoreNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, int i, JavaScriptNode[] createArgumentNodes) {
            return SIMDStoreNodeGen.create(context, builtin, simdContext, i, createArgumentNodes);
        }

        public static SIMDStoreNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDStoreNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        public SIMDStoreNode(JSContext context, JSBuiltin builtin, SIMDType simdContext, int length) {
            super(context, builtin, simdContext);
            this.length = length;
        }

        @Specialization
        protected Object doStore(DynamicObject tarray, Object index, DynamicObject simd) {
            if (!JSArrayBufferView.isJSArrayBufferView(tarray)) {
                errorBranch.enter();
                throw Errors.createTypeErrorArrayBufferViewExpected();
            }
            if (!JSSIMD.isJSSIMD(simd)) {
                errorBranch.enter();
                throw Errors.createTypeError("invalid argument Types");
            }

            return simdStoreInTypedArray(tarray, index, simdContext, simd, length);
        }
    }

    public abstract static class SIMDLoadNode extends JSBasicSimdOperation {
        protected final int length;

        protected SIMDLoadNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
            this.length = simdContext.getNumberOfElements();
        }

        public static SIMDLoadNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, int i, JavaScriptNode[] createArgumentNodes) {
            return SIMDLoadNodeGen.create(context, builtin, simdContext, i, createArgumentNodes);
        }

        public static SIMDLoadNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDLoadNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        public SIMDLoadNode(JSContext context, JSBuiltin builtin, SIMDType simdContext, int length) {
            super(context, builtin, simdContext);
            this.length = length;
        }

        @Specialization
        protected Object doLoad(DynamicObject tarray, Object index) {
            return simdLoadFromTypedArray(tarray, index, simdContext, length);
        }
    }

    public abstract static class SIMDSwizzleNode extends JSBasicSimdOperation {

        public SIMDSwizzleNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDSwizzleNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDSwizzleNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doSwizzle(Object[] args) {
            DynamicObject a = (DynamicObject) args[0];
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);

            for (int i = 0; i < numberOfElements; i++) {
                Object lane = 0;
                if (i < args.length - 1) {
                    lane = args[i + 1];
                }
                int index = simdToLane(numberOfElements, lane);
                setLane(res, i, getLane(a, index));
            }

            return res;
        }
    }

    public abstract static class SIMDShuffleNode extends JSBasicSimdOperation {

        protected SIMDShuffleNode(JSContext context, JSBuiltin builtin, SIMDType simdContext) {
            super(context, builtin, simdContext);
        }

        public static SIMDShuffleNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, JavaScriptNode[] createArgumentNodes) {
            return SIMDShuffleNodeGen.create(context, builtin, simdContext, createArgumentNodes);
        }

        @Specialization
        @ExplodeLoop
        protected Object doShuffle(Object[] args) {

            DynamicObject a = (DynamicObject) args[0];
            DynamicObject b = (DynamicObject) args[1];
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);

            for (int i = 0; i < numberOfElements; i++) {
                Object lane = 0;
                if (i < args.length - 2) {
                    lane = args[i + 2];
                }
                int index = simdToLane(numberOfElements * 2, lane);

                if (index >= numberOfElements) {
                    setLane(res, i, getLane(b, index - numberOfElements));
                } else {
                    setLane(res, i, getLane(a, index));
                }
            }

            return res;
        }
    }

    public abstract static class SIMDFromTIMDNode extends JSBasicSimdOperation {
        protected final SIMDType timd;

        protected SIMDFromTIMDNode(JSContext context, JSBuiltin builtin, SIMDType simdContext, SIMDType timd) {
            super(context, builtin, simdContext);
            this.timd = timd;
        }

        public static SIMDFromTIMDNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, SIMDType createSimdType, JavaScriptNode[] createArgumentNodes) {
            return SIMDFromTIMDNodeGen.create(context, builtin, simdContext, createSimdType, createArgumentNodes);
        }

        @Specialization
        protected Object doFrom(DynamicObject a) {

            if (!JSSIMD.isJSSIMD(a)) {
                errorBranch.enter();
                throw Errors.createTypeError("");
            }
            if (!fromTIMDGuard(timd, simdContext)) {
                errorBranch.enter();
                throw Errors.createTypeError("Should be undefined");
            }
            DynamicObject res = JSSIMD.createSIMD(getContext(), simdContext);
            if (timd.getClass().equals(SIMDUint32x4.class)) {
                doUIntFrom(a, res);
            } else if (timd.getClass().equals(SIMDFloat32x4.class)) {
                doFloatFrom(a, res);
            } else {
                doIntFrom(a, res);
            }
            return res;
        }

        @ExplodeLoop
        private void doIntFrom(DynamicObject a, DynamicObject res) {
            if (isIntegerSIMD(simdContext)) {
                SIMDTypeInt intdescriptor = (SIMDTypeInt) simdContext;
                for (int i = 0; i < numberOfElements; i++) {
                    int intElement = (int) getLane(a, i);
                    if (intElement > intdescriptor.getMax() || intElement < intdescriptor.getMin()) {
                        errorBranch.enter();
                        throw Errors.createRangeError("MIN/MAX");
                    }
                }
            }

            for (int i = 0; i < numberOfElements; i++) {
                setLane(res, i, cast(i, (int) getLane(a, i)));
            }
        }

        @ExplodeLoop
        private void doUIntFrom(DynamicObject a, DynamicObject res) {
            if (isIntegerSIMD(simdContext)) {
                SIMDTypeInt intdescriptor = (SIMDTypeInt) simdContext;
                for (int i = 0; i < numberOfElements; i++) {
                    long intElement = Integer.toUnsignedLong((int) getLane(a, i));
                    if (intElement > intdescriptor.getMax() || intElement < intdescriptor.getMin()) {
                        errorBranch.enter();
                        throw Errors.createRangeError("MIN/MAX");
                    }
                }
            }

            for (int i = 0; i < numberOfElements; i++) {
                setLane(res, i, cast(i, Integer.toUnsignedLong((int) getLane(a, i))));
            }
        }

        @ExplodeLoop
        private void doFloatFrom(DynamicObject a, DynamicObject res) {
            if (isIntegerSIMD(simdContext) && isFloatSIMD(timd)) {
                SIMDTypeInt intdescriptor = (SIMDTypeInt) simdContext;
                for (int i = 0; i < numberOfElements; i++) {
                    if (Float.isNaN((float) getLane(a, i))) {
                        errorBranch.enter();
                        throw Errors.createRangeError("NaN");
                    }
                    long intElement = JSRuntime.toInteger((float) getLane(a, i));
                    if (intElement > intdescriptor.getMax() || intElement < intdescriptor.getMin()) {
                        errorBranch.enter();
                        throw Errors.createRangeError("MIN/MAX");
                    }
                }
            }

            if (isIntegerSIMD(simdContext) && isIntegerSIMD(timd)) {
                SIMDTypeInt intdescriptor = (SIMDTypeInt) simdContext;
                for (int i = 0; i < numberOfElements; i++) {
                    long intElement = JSRuntime.toInteger((float) getLane(a, i));
                    if (intElement > intdescriptor.getMax() || intElement < intdescriptor.getMin()) {
                        errorBranch.enter();
                        throw Errors.createRangeError("MIN/MAX");
                    }
                }
            }

            for (int i = 0; i < numberOfElements; i++) {
                setLane(res, i, cast(i, getLane(a, i)));
            }
        }
    }

    public abstract static class SIMDFromTIMDBitsNode extends JSBasicSimdOperation {
        protected final SIMDType timd;

        protected SIMDFromTIMDBitsNode(JSContext context, JSBuiltin builtin, SIMDType simdContext, SIMDType timd) {
            super(context, builtin, simdContext, Math.max(simdContext.getNumberOfElements(), timd.getNumberOfElements()));
            this.timd = timd;
        }

        public static SIMDFromTIMDBitsNode create(JSContext context, JSBuiltin builtin, SIMDType simdContext, SIMDType createSimdType, JavaScriptNode[] createArgumentNodes) {
            return SIMDFromTIMDBitsNodeGen.create(context, builtin, simdContext, createSimdType, createArgumentNodes);
        }

        @Specialization
        protected Object doFromTIMDBits(Object value) {

            if (!JSSIMD.isJSSIMD(value)) {
                errorBranch.enter();
                throw Errors.createTypeError("");
            }
            SIMDType olddesc = JSSIMD.simdTypeGetSIMDType((DynamicObject) value);

            int bytes = simdContext.getBytesPerElement() * numberOfElements;
            if (olddesc.getBytesPerElement() * olddesc.getNumberOfElements() != bytes) {
                errorBranch.enter();
                throw Errors.createError("assertion");
            }
            byte[] block = new byte[bytes];

            simdStore(block, olddesc, 0, (DynamicObject) value, olddesc.getNumberOfElements());

            return simdLoad(block, simdContext, 0, numberOfElements);
        }
    }
}
