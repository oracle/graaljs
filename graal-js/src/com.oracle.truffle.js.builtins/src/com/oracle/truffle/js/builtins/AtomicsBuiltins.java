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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.IntBinaryOperator;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsCompareExchangeNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsComputeNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsIsLockFreeNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsLoadNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsStoreNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsWaitNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsWakeNodeGen;
import com.oracle.truffle.js.builtins.helper.SharedMemorySync;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToIntegerSpecialNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSAgentWaiterList.JSAgentWaiterListEntry;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain Atomics}.
 */
public final class AtomicsBuiltins extends JSBuiltinsContainer.SwitchEnum<AtomicsBuiltins.Atomics> {
    protected AtomicsBuiltins() {
        super(JSRealm.ATOMICS_CLASS_NAME, Atomics.class);
    }

    public enum Atomics implements BuiltinEnum<Atomics> {
        compareExchange(4),
        load(2),
        store(3),
        add(3),
        sub(3),
        and(3),
        or(3),
        xor(3),
        exchange(3),
        wake(3),
        wait(4),
        isLockFree(1);

        private final int length;

        Atomics(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Atomics builtinEnum) {
        assert context.getEcmaScriptVersion() >= 8;
        switch (builtinEnum) {
            case compareExchange:
                return AtomicsCompareExchangeNodeGen.create(context, builtin, args().fixedArgs(4).createArgumentNodes(context));
            case load:
                return AtomicsLoadNodeGen.create(context, builtin, args().fixedArgs(4).createArgumentNodes(context));
            case store:
                return AtomicsStoreNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case add:
                return AtomicsComputeNodeGen.create(context, builtin, (a, b) -> a + b, args().fixedArgs(3).createArgumentNodes(context));
            case sub:
                return AtomicsComputeNodeGen.create(context, builtin, (a, b) -> a - b, args().fixedArgs(3).createArgumentNodes(context));
            case and:
                return AtomicsComputeNodeGen.create(context, builtin, (a, b) -> a & b, args().fixedArgs(3).createArgumentNodes(context));
            case or:
                return AtomicsComputeNodeGen.create(context, builtin, (a, b) -> a | b, args().fixedArgs(3).createArgumentNodes(context));
            case xor:
                return AtomicsComputeNodeGen.create(context, builtin, (a, b) -> a ^ b, args().fixedArgs(3).createArgumentNodes(context));
            case exchange:
                return AtomicsComputeNodeGen.create(context, builtin, (a, b) -> b, args().fixedArgs(3).createArgumentNodes(context));
            case wake:
                return AtomicsWakeNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case wait:
                return AtomicsWaitNodeGen.create(context, builtin, args().fixedArgs(4).createArgumentNodes(context));
            case isLockFree:
                return AtomicsIsLockFreeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class AtomicsOperationNode extends JSBuiltinNode {

        public AtomicsOperationNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        public static boolean isSharedBufferView(Object object) {
            return JSObject.isJSObject(object) && isSharedBufferView((DynamicObject) object);
        }

        public static boolean isSharedBufferView(DynamicObject object) {
            return JSArrayBufferView.isJSArrayBufferView(object) && JSSharedArrayBuffer.isJSSharedArrayBuffer(JSArrayBufferView.getArrayBuffer(object, JSArrayBufferView.isJSArrayBufferView(object)));
        }

        public static boolean isInt8SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object, isSharedBufferView(object)) instanceof TypedArray.DirectInt8Array;
        }

        public static boolean isUint8SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object, isSharedBufferView(object)) instanceof TypedArray.DirectUint8Array;
        }

        public static boolean isInt16SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object, isSharedBufferView(object)) instanceof TypedArray.DirectInt16Array;
        }

        public static boolean isUint16SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object, isSharedBufferView(object)) instanceof TypedArray.DirectUint16Array;
        }

        public static boolean isInt32SharedBufferView(Object object) {
            return JSObject.isJSObject(object) && isInt32SharedBufferView((DynamicObject) object);
        }

        public static boolean isInt32SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object, isSharedBufferView(object)) instanceof TypedArray.DirectInt32Array;
        }

        public static boolean isUint32SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object, isSharedBufferView(object)) instanceof TypedArray.DirectUint32Array;
        }

        public static boolean isNotIntSharedBufferView(DynamicObject object) {
            TypedArray typed = JSArrayBufferView.typedArrayGetArrayType(object);
            return isSharedBufferView(object) &&
                            (typed instanceof TypedArray.DirectUint8ClampedArray || typed instanceof TypedArray.DirectFloat32Array || typed instanceof TypedArray.DirectFloat64Array);
        }

        /* ES8 24.4.1.2 ValidateAtomicAccess */
        protected static int validateAtomicAccess(DynamicObject target, long convertedIndex, Object originalIndex) {
            int length = JSArrayBufferView.typedArrayGetLength(target);
            assert convertedIndex >= 0;
            if (convertedIndex >= length) {
                throw createRangeErrorSharedArray(originalIndex);
            }
            return (int) convertedIndex;
        }

        public static ByteBuffer getBuffer(DynamicObject thisObj, boolean condition) {
            return JSArrayBufferView.typedArrayGetByteBuffer(thisObj, condition).duplicate();
        }

        public static ByteBuffer getBuffer(DynamicObject thisObj) {
            return getBuffer(thisObj, JSArrayBuffer.isJSDirectArrayBuffer(thisObj));
        }

        protected static boolean inboundFast(DynamicObject target, int index) {
            TypedArray array = JSArrayBufferView.typedArrayGetArrayType(target);
            return array.isInBoundsFast(target, index);
        }

        @TruffleBoundary
        protected final JSException createTypeErrorNonSharedArray() {
            return Errors.createTypeError("Cannot execute on non-shared array", this);
        }

        @TruffleBoundary
        protected static final JSException createRangeErrorSharedArray(Object idx) {
            return Errors.createRangeError("Range error with index : " + idx);
        }
    }

    /**
     * 6.3.3 Atomics.compareExchange(typedArray, index, expectedValue, replacementValue).
     */
    public abstract static class AtomicsCompareExchangeNode extends AtomicsOperationNode {

        public AtomicsCompareExchangeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected int doCASInt8(DynamicObject target, int index, int expected, int replacement, boolean sign) {
            return SharedMemorySync.atomicFetchOrGetByte(getContext(), target, index, (byte) expected, replacement, sign);
        }

        protected int doCASInt16(DynamicObject target, int index, int expected, int replacement, boolean sign) {
            return SharedMemorySync.atomicFetchOrGetShort(getContext(), target, index, expected, replacement, sign);
        }

        protected Object doCASUint32(DynamicObject target, int index, Object expected, Object replacement) {
            return SharedMemorySync.atomicFetchOrGetUnsigned(getContext(), target, index, expected, replacement);
        }

        protected int doCASInt(DynamicObject target, int index, int expected, int replacement) {
            return SharedMemorySync.atomicFetchOrGetInt(getContext(), target, index, expected, replacement);
        }

        protected long doCASLong(DynamicObject target, int index, long expected, long replacement) {
            return SharedMemorySync.atomicFetchOrGetLong(getContext(), target, index, expected, replacement);
        }

        @Specialization(guards = {"isInt8SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doInt8ArrayByte(DynamicObject target, int index, int expected, int replacement) {
            return doCASInt8(target, index, expected, replacement, true);
        }

        @Specialization(guards = {"isUint8SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doUint8ArrayByte(DynamicObject target, int index, int expected, int replacement) {
            return doCASInt8(target, index, expected, replacement, false);
        }

        @Specialization(guards = {"isInt16SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doInt16ArrayByte(DynamicObject target, int index, int expected, int replacement) {
            return doCASInt16(target, index, expected, replacement, true);
        }

        @Specialization(guards = {"isUint16SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doUint16ArrayByte(DynamicObject target, int index, int expected, int replacement) {
            return doCASInt16(target, index, expected, replacement, false);
        }

        @Specialization(guards = {"isUint32SharedBufferView(target)", "inboundFast(target,index)"})
        protected Object doUint32ArrayByte(DynamicObject target, int index, Object expected, Object replacement) {
            return doCASUint32(target, index, expected, replacement);
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doInt32ArrayByte(DynamicObject target, int index, byte expected, byte replacement) {
            return doCASInt(target, index, expected, replacement);
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doInt32ArrayInt(DynamicObject target, int index, int expected, int replacement) {
            return doCASInt(target, index, expected, replacement);
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)", "inboundFast(target,index)"})
        protected long doInt32ArrayLong(DynamicObject target, int index, long expected, long replacement) {
            return doCASLong(target, index, expected, replacement);
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doInt32ArrayObj(DynamicObject target, int index, Object expected, Object replacement,
                        @Cached("create()") JSToIntegerSpecialNode toIntegerNode) {
            return doCASInt(target, index, (int) toIntegerNode.executeLong(expected), (int) toIntegerNode.executeLong(replacement));
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)"})
        protected int doInt32ArrayByteObjIdx(DynamicObject target, Object index,
                        byte expected, byte replacement,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASInt(target, intIndex, expected, replacement);
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)"})
        protected int doInt32ArrayIntObjIdx(DynamicObject target, Object index, int expected, int replacement,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASInt(target, intIndex, expected, replacement);
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)"})
        protected long doInt32ArrayLongObjIdx(DynamicObject target, Object index,
                        long expected, long replacement,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASLong(target, intIndex, expected, replacement);
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)"})
        protected int doInt32ArrayObjObjIdx(DynamicObject target, Object index,
                        Object expected, Object replacement,
                        @Cached("create()") JSToIndexNode toIndexNode,
                        @Cached("create()") JSToIntegerSpecialNode toIntegerNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASInt(target, intIndex, (int) toIntegerNode.executeLong(expected), (int) toIntegerNode.executeLong(replacement));
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object expectedValue, Object replacementValue,
                        @Cached("create()") JSToIndexNode toIndexNode,
                        @Cached("create()") JSToIntegerSpecialNode toIntegerNode) {
            if (isSharedBufferView(maybeTarget)) {
                DynamicObject target = (DynamicObject) maybeTarget;
                if (!isNotIntSharedBufferView(target)) {
                    int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
                    int expected = (int) toIntegerNode.executeLong(expectedValue);
                    int replacement = (int) toIntegerNode.executeLong(replacementValue);

                    if (isInt8SharedBufferView(target)) {
                        return doCASInt8(target, intIndex, expected, replacement, true);
                    } else if (isUint8SharedBufferView(target)) {
                        return doCASInt8(target, intIndex, expected, replacement, false);
                    } else if (isInt16SharedBufferView(target)) {
                        return doCASInt16(target, intIndex, expected, replacement, true);
                    } else if (isUint16SharedBufferView(target)) {
                        return doCASInt16(target, intIndex, expected, replacement, false);
                    } else if (isInt32SharedBufferView(target)) {
                        return doCASInt(target, intIndex, expected, replacement);
                    } else if (isUint32SharedBufferView(target)) {
                        return doCASUint32(target, intIndex, expected, replacement);
                    } else {
                        throw Errors.shouldNotReachHere();
                    }
                }
            }
            throw createTypeErrorNonSharedArray();
        }
    }

    /**
     * 6.3.9 Atomics.load(typedArray, index).
     */
    public abstract static class AtomicsLoadNode extends AtomicsOperationNode {

        public AtomicsLoadNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        public abstract Object executeWithBufferAndIndex(VirtualFrame frame, Object target, Object index);

        @Specialization(guards = {"isInt8SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doInt8ArrayObj(DynamicObject target, int index) {
            return SharedMemorySync.doVolatileGet(target, index);
        }

        @Specialization(guards = {"isUint8SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doUint8ArrayObj(DynamicObject target, int index) {
            return SharedMemorySync.doVolatileGet(target, index) & 0xFF;
        }

        @Specialization(guards = {"isInt16SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doInt16ArrayObj(DynamicObject target, int index) {
            return SharedMemorySync.doVolatileGet(target, index);
        }

        @Specialization(guards = {"isUint16SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doUint16ArrayObj(DynamicObject target, int index) {
            return SharedMemorySync.doVolatileGet(target, index) & 0xFFFF;
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)", "inboundFast(target,index)"})
        protected Object doInt32ArrayObj(DynamicObject target, int index) {
            return SharedMemorySync.doVolatileGet(target, index);
        }

        @Specialization(guards = {"isUint32SharedBufferView(target)", "inboundFast(target,index)"})
        protected Object doUint32ArrayObj(DynamicObject target, int index) {
            return SharedMemorySync.doVolatileGet(target, index) & 0xFFFFFFFFL;
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)"})
        protected Object doInt32ArrayObjObjIdx(DynamicObject target, Object index,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return SharedMemorySync.doVolatileGet(target, intIndex);
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            if (isSharedBufferView(maybeTarget)) {
                DynamicObject target = (DynamicObject) maybeTarget;
                if (!isNotIntSharedBufferView(target)) {
                    int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
                    if (isInt8SharedBufferView(target)) {
                        return SharedMemorySync.doVolatileGet(target, intIndex);
                    } else if (isUint8SharedBufferView(target)) {
                        return SharedMemorySync.doVolatileGet(target, intIndex) & 0xFF;
                    } else if (isInt16SharedBufferView(target)) {
                        return SharedMemorySync.doVolatileGet(target, intIndex);
                    } else if (isUint16SharedBufferView(target)) {
                        return SharedMemorySync.doVolatileGet(target, intIndex) & 0xFFFF;
                    } else if (isInt32SharedBufferView(target)) {
                        return SharedMemorySync.doVolatileGet(target, intIndex);
                    } else if (isUint32SharedBufferView(target)) {
                        return SharedMemorySync.doVolatileGet(target, intIndex) & 0xFFFFFFFFL;
                    } else {
                        throw Errors.shouldNotReachHere();
                    }
                }
            }
            throw createTypeErrorNonSharedArray();
        }

    }

    /**
     * 6.3.11 Atomics.store(typedArray, index, value).
     */
    public abstract static class AtomicsStoreNode extends AtomicsOperationNode {

        public AtomicsStoreNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isInt8SharedBufferView(target)||isUint8SharedBufferView(target)",
                        "inboundFast(target,index)"})
        protected Object doIntArrayObj(DynamicObject target, int index, int value) {
            SharedMemorySync.doVolatilePut(target, index, value);
            return value;
        }

        @Specialization(guards = {"isInt8SharedBufferView(target)||isUint8SharedBufferView(target)",
                        "inboundFast(target,index)"})
        protected Object doIntArrayObj(DynamicObject target, int index, double value) {
            SharedMemorySync.doVolatilePut(target, index, (int) value);
            return JSRuntime.toInteger(value);
        }

        @Specialization(guards = {"isInt16SharedBufferView(target)||isUint16SharedBufferView(target)",
                        "inboundFast(target,index)"})
        protected Object doInt16ArrayObj(DynamicObject target, int index, int value) {
            SharedMemorySync.doVolatilePut(target, index, (short) value);
            return value;
        }

        @Specialization(guards = {"isInt16SharedBufferView(target)||isUint16SharedBufferView(target)",
                        "inboundFast(target,index)"})
        protected Object doInt16ArrayObj(DynamicObject target, int index, double value) {
            SharedMemorySync.doVolatilePut(target, index, (short) value);
            return JSRuntime.toInteger(value);
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)||isUint32SharedBufferView(target)",
                        "inboundFast(target,index)"})
        protected Object doInt32ArrayObj(DynamicObject target, int index, int value) {
            SharedMemorySync.doVolatilePut(target, index, value);
            return value;
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)||isUint32SharedBufferView(target)",
                        "inboundFast(target,index)"})
        protected Object doInt32ArrayObj(DynamicObject target, int index, double value) {
            SharedMemorySync.doVolatilePut(target, index, (int) JSRuntime.toInteger(value));
            return JSRuntime.toInteger(value);
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)"})
        protected Object doInt32ArrayObjObjIdx(DynamicObject target, Object index, int value,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            SharedMemorySync.doVolatilePut(target, intIndex, value);
            return value;
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object value,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            if (isSharedBufferView(maybeTarget)) {
                DynamicObject target = (DynamicObject) maybeTarget;
                if (!isNotIntSharedBufferView(target)) {
                    int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

                    if (isInt8SharedBufferView(target) || isUint8SharedBufferView(target)) {
                        SharedMemorySync.doVolatilePut(target, intIndex, (int) JSRuntime.toInteger(value));
                    } else if (isInt16SharedBufferView(target) || isUint16SharedBufferView(target)) {
                        SharedMemorySync.doVolatilePut(target, intIndex, (short) JSRuntime.toInteger(value));
                    } else if (isInt32SharedBufferView(target) || isUint32SharedBufferView(target)) {
                        SharedMemorySync.doVolatilePut(target, intIndex, (int) JSRuntime.toInteger(value));
                    } else {
                        throw Errors.shouldNotReachHere();
                    }
                    return JSRuntime.toInteger(value);
                }
            }
            throw createTypeErrorNonSharedArray();
        }
    }

    /**
     * Other atomic operations.
     */
    public abstract static class AtomicsComputeNode extends AtomicsOperationNode {
        private final IntBinaryOperator operator;

        public AtomicsComputeNode(JSContext context, JSBuiltin builtin, IntBinaryOperator operator) {
            super(context, builtin);
            this.operator = operator;
        }

        private int atomicDoInt(DynamicObject target, int index, int value) {
            int initial;
            int result;
            do {
                initial = SharedMemorySync.doVolatileGet(target, index);
                result = operator.applyAsInt(initial, value);
            } while (!SharedMemorySync.compareAndSwapInt(getContext(), target, index, initial, result));
            return initial;
        }

        @Specialization(guards = {"isInt8SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doInt8ArrayObj(DynamicObject target, int index, int value) {
            return (byte) atomicDoInt(target, index, value);
        }

        @Specialization(guards = {"isUint8SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doUint8ArrayObj(DynamicObject target, int index, int value) {
            return ((byte) (atomicDoInt(target, index, value))) & 0xFF;
        }

        @Specialization(guards = {"isInt16SharedBufferView(target)", "inboundFast(target,index)"})
        protected Object doInt16ArrayObj(DynamicObject target, int index, int value) {
            return atomicDoInt(target, index, value);
        }

        @Specialization(guards = {"isUint16SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doUint16ArrayObj(DynamicObject target, int index, int value) {
            return atomicDoInt(target, index, value) & 0xFFFF;
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)", "inboundFast(target,index)"})
        protected int doInt32ArrayObj(DynamicObject target, int index, int value) {
            return atomicDoInt(target, index, value);
        }

        @Specialization(guards = {"isUint32SharedBufferView(target)", "inboundFast(target,index)"})
        protected Object doUint32ArrayObj(DynamicObject target, int index, int value) {
            return atomicDoInt(target, index, value) & 0xFFFFFFFFL;
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)"})
        protected Object doInt32ArrayObjObjIdx(DynamicObject target, Object index, int value,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return atomicDoInt(target, intIndex, value);
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object value,
                        @Cached("create()") JSToIndexNode toIndexNode,
                        @Cached("create()") JSToIntegerSpecialNode toIntegerNode) {
            if (isSharedBufferView(maybeTarget)) {
                DynamicObject target = (DynamicObject) maybeTarget;
                if (!isNotIntSharedBufferView(target)) {
                    int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
                    int intValue = (int) toIntegerNode.executeLong(value);
                    if (isInt8SharedBufferView(target)) {
                        return (int) (byte) atomicDoInt(target, intIndex, intValue);
                    } else if (isUint8SharedBufferView(target)) {
                        return ((byte) atomicDoInt(target, intIndex, intValue)) & 0xFF;
                    } else if (isInt16SharedBufferView(target)) {
                        return atomicDoInt(target, intIndex, intValue);
                    } else if (isUint16SharedBufferView(target)) {
                        return (atomicDoInt(target, intIndex, intValue)) & 0xFFFF;
                    } else if (isInt32SharedBufferView(target)) {
                        return atomicDoInt(target, intIndex, intValue);
                    } else if (isUint32SharedBufferView(target)) {
                        return (atomicDoInt(target, intIndex, intValue)) & 0xFFFFFFFFL;
                    } else {
                        throw Errors.shouldNotReachHere();
                    }
                }
            }
            throw createTypeErrorNonSharedArray();
        }
    }

    /**
     * Thread Wake-up.
     */
    public abstract static class AtomicsWakeNode extends AtomicsOperationNode {

        public AtomicsWakeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object count,
                        @Cached("create()") JSToIndexNode toIndexNode,
                        @Cached("create()") JSToInt32Node toInt32Node) {
            if (isSharedBufferView(maybeTarget) && isInt32SharedBufferView(maybeTarget)) {
                DynamicObject target = (DynamicObject) maybeTarget;
                int i = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

                int c = Integer.MAX_VALUE;
                if (count != Undefined.instance) {
                    int tmp = toInt32Node.executeInt(count);
                    c = Integer.max(tmp, 0);
                }
                JSAgentWaiterListEntry wl = SharedMemorySync.getWaiterList(getContext(), target, i);
                int n = 0;
                SharedMemorySync.enterCriticalSection(getContext(), wl);
                List<Integer> waiters = SharedMemorySync.removeWaiters(getContext(), wl, c);
                while (n < waiters.size()) {
                    SharedMemorySync.wakeWaiter(getContext(), Boundaries.listGet(waiters, n++), wl);
                }
                SharedMemorySync.leaveCriticalSection(getContext(), wl);
                return n;
            }
            throw createTypeErrorNonSharedArray();
        }
    }

    /**
     * Thread Sleep.
     */
    public abstract static class AtomicsWaitNode extends AtomicsOperationNode {

        private static final String OK = "ok";
        private static final String NOT_EQUAL = "not-equal";
        private static final String TIMED_OUT = "timed-out";

        public AtomicsWaitNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected AtomicsLoadNode createHelperNode() {
            return AtomicsLoadNodeGen.create(getContext(), getBuiltin(), args().fixedArgs(4).createArgumentNodes(getContext()));
        }

        @Specialization
        protected Object doGeneric(VirtualFrame frame, Object maybeTarget, Object index, Object value, Object timeout,
                        @Cached("create()") JSToIndexNode toIndexNode,
                        @Cached("create()") JSToNumberNode timeToInt32Node,
                        @Cached("createHelperNode()") AtomicsLoadNode loadNode) {
            if (isSharedBufferView(maybeTarget) && isInt32SharedBufferView(maybeTarget)) {
                DynamicObject target = (DynamicObject) maybeTarget;
                int i = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

                int v = JSRuntime.toInt32(value);
                int t = Integer.MAX_VALUE;
                Number tmp = timeToInt32Node.executeNumber(timeout);
                if (!JSRuntime.isNaN(tmp)) {
                    t = Integer.max(tmp.intValue(), 0);
                }

                if (!SharedMemorySync.agentCanSuspend(getContext())) {
                    throw createTypeErrorNonSharedArray();
                }
                JSAgentWaiterListEntry wl = SharedMemorySync.getWaiterList(getContext(), target, i);
                SharedMemorySync.enterCriticalSection(getContext(), wl);
                Object w = loadNode.executeWithBufferAndIndex(frame, maybeTarget, i);
                if (!(w instanceof Integer) || (int) w != v) {
                    SharedMemorySync.leaveCriticalSection(getContext(), wl);
                    return NOT_EQUAL;
                }
                int id = getContext().getJSAgent().getSignifier();
                SharedMemorySync.addWaiter(getContext(), wl, id);
                if (t < 0) {
                    return TIMED_OUT;
                }
                boolean awoken = SharedMemorySync.suspendAgent(getContext(), wl, id, t);
                SharedMemorySync.removeWaiter(getContext(), wl, id);
                SharedMemorySync.leaveCriticalSection(getContext(), wl);
                if (awoken) {
                    return OK;
                } else {
                    return TIMED_OUT;
                }
            }
            throw createTypeErrorNonSharedArray();
        }
    }

    /**
     * Lock-free regions checking.
     */
    public abstract static class AtomicsIsLockFreeNode extends AtomicsOperationNode {

        // For now, we assume that any platform is lock free on Graal.js
        private static final boolean AR_IsLockFree1 = true;
        private static final boolean AR_IsLockFree2 = true;

        public AtomicsIsLockFreeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doGeneric(Object size,
                        @Cached("create()") JSToInt32Node toInt32Node) {
            int n = toInt32Node.executeInt(size);
            if (n == 1) {
                return AR_IsLockFree1;
            } else if (n == 2) {
                return AR_IsLockFree2;
            } else if (n == 4) {
                return true;
            }
            return false;
        }
    }
}
